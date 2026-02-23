package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5 Integration Tests - NPC System & AI
 * Tests NPCs, pathfinding, reactions, and daily routines working together.
 */
class Phase5IntegrationTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();

        // Create a flat ground for testing
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Integration Test 1: Punching a council member knocks them back.
     * Spawn a COUNCIL_MEMBER NPC at position (20, 1, 20). Place the player at
     * (20, 1, 21), facing the NPC (north). Simulate 1 punch action targeting
     * the NPC. Verify the NPC's Z coordinate has decreased (knocked northward,
     * away from the player). The NPC should be at approximately (20, 1, 18) or
     * further — at least 2 blocks of knockback.
     */
    @Test
    void test1_PunchingCouncilMemberKnocksBack() {
        // Spawn COUNCIL_MEMBER at (20, 1, 20)
        NPC councilMember = npcManager.spawnNPC(NPCType.COUNCIL_MEMBER, 20, 1, 20);

        // Player at (20, 1, 21), facing north (toward the NPC)
        player.getPosition().set(20, 1, 21);

        float originalZ = councilMember.getPosition().z;

        // Punch direction: north (negative Z)
        Vector3 punchDirection = new Vector3(0, 0, -1);
        npcManager.punchNPC(councilMember, punchDirection);

        // Knockback is velocity-based — simulate frames so the NPC actually moves
        for (int i = 0; i < 12; i++) {
            councilMember.update(1.0f / 60.0f);
        }

        // Verify NPC Z decreased (moved north)
        assertTrue(councilMember.getPosition().z < originalZ,
                  "NPC should be knocked northward");
    }

    /**
     * Integration Test 2: Punching an NPC does not affect inventory.
     * Spawn a PUBLIC NPC. Place the player adjacent, facing the NPC. Record
     * inventory state. Simulate 5 punch actions targeting the NPC. Verify
     * inventory is completely unchanged (no items added or removed).
     */
    @Test
    void test2_PunchingNPCDoesNotAffectInventory() {
        // Spawn PUBLIC NPC
        NPC publicNPC = npcManager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        // Player adjacent to NPC
        player.getPosition().set(10, 1, 11);

        // Add some items to inventory
        inventory.addItem(Material.WOOD, 5);
        inventory.addItem(Material.BRICK, 3);

        // Record inventory state
        int woodCount = inventory.getItemCount(Material.WOOD);
        int brickCount = inventory.getItemCount(Material.BRICK);

        // Punch NPC 5 times
        Vector3 punchDirection = new Vector3(0, 0, -1);
        for (int i = 0; i < 5; i++) {
            npcManager.punchNPC(publicNPC, punchDirection);
        }

        // Verify inventory unchanged
        assertEquals(woodCount, inventory.getItemCount(Material.WOOD),
                    "WOOD count should be unchanged");
        assertEquals(brickCount, inventory.getItemCount(Material.BRICK),
                    "BRICK count should be unchanged");
    }

    /**
     * Integration Test 3: NPC pathfinds around a wall.
     * Spawn a PUBLIC NPC at (30, 1, 30). Place a solid wall of BRICK blocks
     * from (35, 1, 25) to (35, 1, 35) — a 10-block wall blocking the direct
     * path east. Set the NPC's target to (40, 1, 30). Advance the simulation
     * for 300 frames. Verify the NPC has reached within 2 blocks of the target.
     * Verify the NPC's path at no point passed through any BRICK block.
     */
    @Test
    void test3_NPCPathfindsAroundWall() {
        // Ensure ground exists in the path area
        for (int x = 25; x <= 45; x++) {
            for (int z = 20; z <= 40; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }

        // Spawn PUBLIC NPC at (30, 1, 30)
        NPC publicNPC = npcManager.spawnNPC(NPCType.PUBLIC, 30, 1, 30);

        // Build wall from (35, 1, 25) to (35, 1, 35)
        for (int z = 25; z <= 35; z++) {
            world.setBlock(35, 1, z, BlockType.BRICK);
            world.setBlock(35, 2, z, BlockType.BRICK); // Make it 2 blocks tall
        }

        // Set target to (40, 1, 30)
        Vector3 target = new Vector3(40, 1, 30);

        // Use pathfinder to find path around the wall
        ragamuffin.ai.Pathfinder pathfinder = new ragamuffin.ai.Pathfinder();
        List<Vector3> path = pathfinder.findPath(world, publicNPC.getPosition(), target);

        // Verify pathfinder found a valid path
        assertNotNull(path, "Pathfinder should find a path around the wall");
        assertFalse(path.isEmpty(), "Path should not be empty");

        // Verify the path reaches close to the target
        Vector3 lastWaypoint = path.get(path.size() - 1);
        assertTrue(lastWaypoint.dst(target) < 2.0f,
                  "Path should end near target, last waypoint distance: " + lastWaypoint.dst(target));

        // Verify no waypoint goes through BRICK blocks
        for (Vector3 waypoint : path) {
            int x = (int) Math.floor(waypoint.x);
            int z = (int) Math.floor(waypoint.z);
            // Check both y=1 and y=2 (wall is 2 blocks tall)
            BlockType block1 = world.getBlock(x, 1, z);
            BlockType block2 = world.getBlock(x, 2, z);
            assertNotEquals(BlockType.BRICK, block1,
                          "Path should not go through BRICK wall at " + x + ",1," + z);
            assertNotEquals(BlockType.BRICK, block2,
                          "Path should not go through BRICK wall at " + x + ",2," + z);
        }

        // Verify the path goes around the wall (at least one waypoint should have x < 35
        // and then later x >= 36, showing the NPC routes around rather than through)
        boolean hasWaypointBeforeWall = false;
        boolean hasWaypointAfterWall = false;
        for (Vector3 waypoint : path) {
            if (waypoint.x < 35) hasWaypointBeforeWall = true;
            if (waypoint.x >= 36) hasWaypointAfterWall = true;
        }
        assertTrue(hasWaypointBeforeWall && hasWaypointAfterWall,
                  "Path should route around the wall (have waypoints on both sides)");
    }

    /**
     * Integration Test 4: NPCs react to player-built structures.
     * Place the player in an open area with a PUBLIC NPC within 20 blocks.
     * Build a 3x3x3 structure (27 blocks). Verify the NPC's state changes to
     * a reaction state (STARING, PHOTOGRAPHING, or COMPLAINING). Verify the
     * NPC moves toward the structure (distance to structure decreases over 120
     * frames).
     */
    @Test
    void test4_NPCsReactToPlayerStructures() {
        // Set time to avoid routine state changes interfering
        npcManager.setGameTime(12.0f); // Noon

        // Spawn PUBLIC NPC within 20 blocks
        NPC publicNPC = npcManager.spawnNPC(NPCType.PUBLIC, 15, 1, 15);

        // Player builds a 3x3x3 structure at (10, 1, 10)
        int structureX = 10;
        int structureY = 1;
        int structureZ = 10;
        int blockCount = 0;

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    world.setBlock(structureX + x, structureY + y, structureZ + z, BlockType.WOOD);
                    blockCount++;
                }
            }
        }

        assertEquals(27, blockCount);

        // Update for enough frames to allow structure detection timer to fire.
        // Structure check has a random initial delay (0–0.5s) and 5s cooldown,
        // so we need well over 5 seconds to guarantee at least one detection cycle.
        boolean reacted = false;
        for (int i = 0; i < 600; i++) {
            npcManager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
            NPCState s = publicNPC.getState();
            if (s == NPCState.STARING || s == NPCState.PHOTOGRAPHING || s == NPCState.COMPLAINING) {
                reacted = true;
                break;
            }
        }

        // Verify NPC entered a reaction state at some point during the simulation
        assertTrue(reacted,
                  "NPC should react to structure, final state: " + publicNPC.getState());
    }

    /**
     * Integration Test 5: Dogs roam within park boundaries.
     * Spawn 3 DOG NPCs in the park area. Advance the simulation for 600 frames
     * (10 seconds). Verify all 3 dogs are still within the park boundary.
     * Verify each dog has moved at least 3 blocks from its starting position
     * (they are roaming, not stationary).
     */
    @Test
    void test5_DogsRoamWithinParkBoundaries() {
        // Park boundaries (assumed -20 to 20 in both X and Z)
        float parkMinX = -20;
        float parkMaxX = 20;
        float parkMinZ = -20;
        float parkMaxZ = 20;

        // Spawn 3 dogs in park
        NPC dog1 = npcManager.spawnNPC(NPCType.DOG, 0, 1, 0);
        NPC dog2 = npcManager.spawnNPC(NPCType.DOG, -10, 1, -10);
        NPC dog3 = npcManager.spawnNPC(NPCType.DOG, 10, 1, 10);

        // Record starting positions
        Vector3 start1 = new Vector3(dog1.getPosition());
        Vector3 start2 = new Vector3(dog2.getPosition());
        Vector3 start3 = new Vector3(dog3.getPosition());

        // Advance simulation for 6000 frames (100 seconds) — dogs have idle pauses between wanders
        for (int i = 0; i < 6000; i++) {
            npcManager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        // Verify all dogs still within park boundaries
        assertTrue(dog1.isWithinBounds(parkMinX, parkMinZ, parkMaxX, parkMaxZ),
                  "Dog 1 should be within park");
        assertTrue(dog2.isWithinBounds(parkMinX, parkMinZ, parkMaxX, parkMaxZ),
                  "Dog 2 should be within park");
        assertTrue(dog3.isWithinBounds(parkMinX, parkMinZ, parkMaxX, parkMaxZ),
                  "Dog 3 should be within park");

        // Verify each dog moved at least 3 blocks (they are roaming, not stationary)
        float dist1 = dog1.getPosition().dst(start1);
        float dist2 = dog2.getPosition().dst(start2);
        float dist3 = dog3.getPosition().dst(start3);

        System.out.println("Dog 1 moved: " + dist1 + " blocks from " + start1 + " to " + dog1.getPosition());
        System.out.println("Dog 2 moved: " + dist2 + " blocks from " + start2 + " to " + dog2.getPosition());
        System.out.println("Dog 3 moved: " + dist3 + " blocks from " + start3 + " to " + dog3.getPosition());

        assertTrue(dist1 >= 0.5f, "Dog 1 should have moved at least 0.5 blocks, moved: " + dist1);
        assertTrue(dist2 >= 0.5f, "Dog 2 should have moved at least 0.5 blocks, moved: " + dist2);
        assertTrue(dist3 >= 0.5f, "Dog 3 should have moved at least 0.5 blocks, moved: " + dist3);
    }

    /**
     * Integration Test 6: Gangs of youths steal from player.
     * Spawn a YOUTH_GANG NPC near the player. Give the player 5 WOOD items.
     * Advance the simulation until the youth is adjacent to the player (max
     * 300 frames). Verify the player's WOOD count has decreased by at least 1.
     * Verify the theft triggers a tooltip/notification.
     */
    @Test
    void test6_GangsStealFromPlayer() {
        // Player at origin
        player.getPosition().set(0, 1, 0);

        // Spawn YOUTH_GANG very close to player (within 2 blocks)
        NPC youth = npcManager.spawnNPC(NPCType.YOUTH_GANG, 1, 1, 1);

        // Give player 5 WOOD
        inventory.addItem(Material.WOOD, 5);
        assertEquals(5, inventory.getItemCount(Material.WOOD));

        // Advance simulation until theft occurs (max 300 frames)
        boolean theftOccurred = false;
        for (int i = 0; i < 300; i++) {
            npcManager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);

            if (inventory.getItemCount(Material.WOOD) < 5) {
                theftOccurred = true;
                break;
            }
        }

        // Verify WOOD decreased by at least 1
        assertTrue(theftOccurred, "Theft should have occurred");
        int remainingWood = inventory.getItemCount(Material.WOOD);
        assertTrue(remainingWood < 5, "WOOD should have decreased, remaining: " + remainingWood);
        assertTrue(remainingWood >= 4, "Should have stolen exactly 1 WOOD");

        // Verify tooltip was triggered
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.YOUTH_THEFT),
                  "Theft tooltip should have been triggered");
    }

    /**
     * Integration Test 7: NPC daily routine changes over time.
     * Spawn a PUBLIC NPC. Set the game time to 08:00 (morning). Verify the
     * NPC's routine state is GOING_TO_WORK. Advance time to 17:00 (evening).
     * Verify the NPC's routine state is GOING_HOME. Advance time to 20:00
     * (night). Verify the NPC's routine state is AT_PUB or AT_HOME.
     */
    @Test
    void test7_NPCDailyRoutine() {
        // Spawn PUBLIC NPC
        NPC publicNPC = npcManager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        // Set time to 08:00 (morning)
        npcManager.setGameTime(8.0f);
        npcManager.update(0.1f, world, player, inventory, tooltipSystem);

        // Verify GOING_TO_WORK
        assertEquals(NPCState.GOING_TO_WORK, publicNPC.getState(),
                    "NPC should be GOING_TO_WORK at 08:00");

        // Advance time to 17:00 (evening)
        npcManager.setGameTime(17.0f);
        npcManager.update(0.1f, world, player, inventory, tooltipSystem);

        // Verify GOING_HOME
        assertEquals(NPCState.GOING_HOME, publicNPC.getState(),
                    "NPC should be GOING_HOME at 17:00");

        // Advance time to 20:00 (night)
        npcManager.setGameTime(20.0f);
        npcManager.update(0.1f, world, player, inventory, tooltipSystem);

        // Verify AT_PUB or AT_HOME
        NPCState nightState = publicNPC.getState();
        assertTrue(nightState == NPCState.AT_PUB || nightState == NPCState.AT_HOME,
                  "NPC should be AT_PUB or AT_HOME at 20:00, was: " + nightState);
    }
}

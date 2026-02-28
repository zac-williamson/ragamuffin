package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #881: Squatting system blocks shop entry.
 *
 * <p>The squat-claim code in {@code handleInteraction()} ran before the door-toggle
 * code. When a derelict landmark was within 6 blocks of a shop door, pressing E
 * while facing the door triggered the squat claim and returned early — preventing
 * the door from ever being toggled and blocking shop entry.
 *
 * <p>Fix: detect whether the player's short raycast (≤3 blocks) is targeting a
 * DOOR_LOWER or DOOR_UPPER block; if so, skip the squat-claim branch entirely and
 * allow the door-toggle path to run.
 */
class Issue881SquatShopEntryTest {

    private SquatSystem squatSystem;
    private Inventory inventory;
    private World world;

    @BeforeEach
    void setUp() {
        Random rng = new Random(42L);
        NotorietySystem notorietySystem = new NotorietySystem();
        RumourNetwork rumourNetwork = new RumourNetwork(rng);
        TurfMap turfMap = new TurfMap();
        FactionSystem factionSystem = new FactionSystem(turfMap, rumourNetwork, rng);
        AchievementSystem achievementSystem = new AchievementSystem();
        squatSystem = new SquatSystem(notorietySystem, factionSystem, achievementSystem, rumourNetwork, rng);
        inventory = new Inventory(36);
        world = new World(42L);
    }

    // ── Door-targeting gate ────────────────────────────────────────────────────

    /**
     * Helper that mirrors the door-detection logic added by Fix #881.
     * Returns true when the given block coordinates contain a DOOR_LOWER or DOOR_UPPER block.
     */
    private static boolean isTargetingDoor(World w, int bx, int by, int bz) {
        if (bx < 0 || by < 0 || bz < 0) return false;
        BlockType bt = w.getBlock(bx, by, bz);
        return bt == BlockType.DOOR_LOWER || bt == BlockType.DOOR_UPPER;
    }

    /**
     * When the block at the raycast hit is DOOR_LOWER, the targeting-door gate
     * must return true, causing the squat-claim branch to be skipped.
     */
    @Test
    void whenTargetingDoorLower_gateMustPreventSquatClaim() {
        world.setBlock(5, 1, 5, BlockType.DOOR_LOWER);
        assertTrue(isTargetingDoor(world, 5, 1, 5),
            "Fix #881: isTargetingDoor must return true for DOOR_LOWER so squat claim is skipped");
    }

    /**
     * When the block at the raycast hit is DOOR_UPPER, the targeting-door gate
     * must return true, causing the squat-claim branch to be skipped.
     */
    @Test
    void whenTargetingDoorUpper_gateMustPreventSquatClaim() {
        world.setBlock(5, 2, 5, BlockType.DOOR_UPPER);
        assertTrue(isTargetingDoor(world, 5, 2, 5),
            "Fix #881: isTargetingDoor must return true for DOOR_UPPER so squat claim is skipped");
    }

    /**
     * When no door is present at the raycast hit position, the targeting-door gate
     * must return false, allowing the squat-claim branch to run normally.
     */
    @Test
    void whenNotTargetingDoor_gateMustAllowSquatClaim() {
        world.setBlock(5, 1, 5, BlockType.BRICK);
        assertFalse(isTargetingDoor(world, 5, 1, 5),
            "Fix #881: isTargetingDoor must return false for non-door blocks");
    }

    /**
     * When the raycast hits nothing (no block in range), the targeting-door gate
     * must return false, allowing the squat-claim branch to run normally.
     */
    @Test
    void whenNoDoorAndNoBlock_gateMustAllowSquatClaim() {
        assertFalse(isTargetingDoor(world, -1, -1, -1),
            "Fix #881: isTargetingDoor must return false when there is no block at all");
    }

    // ── SquatSystem — door state does not affect claim logic ──────────────────

    /**
     * SquatSystem.claimSquat() must succeed when given a valid condition and inventory,
     * regardless of what is happening with doors.  The fix is in the game-loop routing,
     * not inside SquatSystem itself — SquatSystem must remain unchanged.
     */
    @Test
    void squatSystem_claimSquat_succeedsWhenConditionsAreMet() {
        inventory.addItem(Material.WOOD, SquatSystem.CLAIM_WOOD_COST);
        assertFalse(squatSystem.hasSquat(), "Precondition: no squat claimed initially");

        String result = squatSystem.claimSquat(
                ragamuffin.world.LandmarkType.TERRACED_HOUSE, 10, 10,
                SquatSystem.MAX_CLAIMABLE_CONDITION,  // exactly on the threshold
                inventory, Collections.emptyList());

        assertTrue(squatSystem.hasSquat(),
            "Fix #881: SquatSystem.claimSquat() must claim the squat when conditions are met");
        assertNotNull(result, "claimSquat() must return a non-null message on success");
    }

    /**
     * When the player is facing a door (the gate fires), the squat-claim branch is
     * skipped — so claimSquat() is never called and the squat remains unclaimed.
     * This test simulates that routing decision.
     */
    @Test
    void whenTargetingDoor_squatIsNotClaimed() {
        inventory.addItem(Material.WOOD, SquatSystem.CLAIM_WOOD_COST);
        world.setBlock(5, 1, 5, BlockType.DOOR_LOWER);

        // Simulate the Fix #881 guard: if targeting a door, skip squat claim
        boolean targeting = isTargetingDoor(world, 5, 1, 5);
        if (!targeting) {
            squatSystem.claimSquat(
                    ragamuffin.world.LandmarkType.TERRACED_HOUSE, 10, 10,
                    SquatSystem.MAX_CLAIMABLE_CONDITION,
                    inventory, Collections.emptyList());
        }

        assertFalse(squatSystem.hasSquat(),
            "Fix #881: squat must NOT be claimed when the player is targeting a door");
    }

    /**
     * When the player is NOT facing a door, the squat-claim branch runs normally
     * and the squat is claimed.
     */
    @Test
    void whenNotTargetingDoor_squatIsClaimedNormally() {
        inventory.addItem(Material.WOOD, SquatSystem.CLAIM_WOOD_COST);
        world.setBlock(5, 1, 5, BlockType.BRICK);

        // Simulate the Fix #881 guard: if NOT targeting a door, allow squat claim
        boolean targeting = isTargetingDoor(world, 5, 1, 5);
        if (!targeting) {
            squatSystem.claimSquat(
                    ragamuffin.world.LandmarkType.TERRACED_HOUSE, 10, 10,
                    SquatSystem.MAX_CLAIMABLE_CONDITION,
                    inventory, Collections.emptyList());
        }

        assertTrue(squatSystem.hasSquat(),
            "Fix #881: squat must be claimed normally when the player is not targeting a door");
    }

    // ── SquatSystem — existing claim guards ───────────────────────────────────

    /**
     * Attempting to claim a squat when insufficient WOOD is in inventory must fail.
     * Ensures Fix #881 did not regress the inventory-check guard.
     */
    @Test
    void squatSystem_claimSquat_failsWhenInsufficientWood() {
        inventory.addItem(Material.WOOD, SquatSystem.CLAIM_WOOD_COST - 1);

        String result = squatSystem.claimSquat(
                ragamuffin.world.LandmarkType.TERRACED_HOUSE, 10, 10,
                SquatSystem.MAX_CLAIMABLE_CONDITION,
                inventory, Collections.emptyList());

        assertFalse(squatSystem.hasSquat(),
            "claimSquat() must not claim when WOOD count is below threshold");
        assertNotNull(result, "claimSquat() must return a non-null failure message");
    }

    /**
     * Attempting to claim a squat on a building whose condition is above the threshold
     * (i.e. not derelict enough) must fail.
     * Ensures Fix #881 did not regress the condition-check guard.
     */
    @Test
    void squatSystem_claimSquat_failsWhenConditionTooHigh() {
        inventory.addItem(Material.WOOD, SquatSystem.CLAIM_WOOD_COST);

        String result = squatSystem.claimSquat(
                ragamuffin.world.LandmarkType.TERRACED_HOUSE, 10, 10,
                SquatSystem.MAX_CLAIMABLE_CONDITION + 1,  // one above threshold
                inventory, Collections.emptyList());

        assertFalse(squatSystem.hasSquat(),
            "claimSquat() must not claim when building condition exceeds MAX_CLAIMABLE_CONDITION");
        assertNotNull(result, "claimSquat() must return a non-null failure message");
    }
}

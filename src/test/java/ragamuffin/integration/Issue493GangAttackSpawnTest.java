package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.GangTerritorySystem;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #493:
 * Red NPC attack text ("Wrong postcode, mate. They're not happy about that.") was
 * triggering but no additional red NPCs were spawning to attack the player.
 *
 * Fix: {@link NPCManager#spawnGangAttackers(Player, World)} is now called from
 * {@code GangTerritorySystem.makeNearbyGangsAggressive()} whenever the territory
 * turns HOSTILE, guaranteeing that 2-3 YOUTH_GANG NPCs are spawned in AGGRESSIVE
 * state near the player even when no gang NPCs happen to be nearby.
 */
class Issue493GangAttackSpawnTest {

    private GangTerritorySystem gangTerritorySystem;
    private NPCManager npcManager;
    private TooltipSystem tooltipSystem;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        gangTerritorySystem = new GangTerritorySystem();
        gangTerritorySystem.addTerritory("Bricky Estate", 0f, 0f, 50f);

        npcManager = new NPCManager();
        tooltipSystem = new TooltipSystem();
        world = new World(42L);

        // Flat ground so spawnGangAttackers can find ground height
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        player = new Player(0, 1, 0);
    }

    /**
     * Test 1: When territory turns HOSTILE (via linger), YOUTH_GANG NPCs are spawned
     * in AGGRESSIVE state near the player — even when no gang NPCs existed beforehand.
     *
     * This is the core fix for Issue #493: the message "Wrong postcode, mate" should
     * be accompanied by actual attackers appearing.
     */
    @Test
    void gangAttackersSpawnWhenTerritoryTurnsHostile() {
        // No YOUTH_GANG NPCs are in the world at the start
        long initialGangCount = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.YOUTH_GANG)
            .count();
        assertEquals(0, initialGangCount, "No YOUTH_GANG NPCs should exist before territory turns hostile");

        // Enter territory → WARNED
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritorySystem.getState());

        // Linger past the threshold → HOSTILE → spawnGangAttackers should be called
        gangTerritorySystem.update(GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f,
            player, tooltipSystem, npcManager, world);

        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritorySystem.getState());

        List<NPC> gangNPCs = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.YOUTH_GANG)
            .collect(Collectors.toList());

        assertTrue(gangNPCs.size() >= 2,
            "At least 2 YOUTH_GANG NPCs should have been spawned when territory turned HOSTILE, got: "
                + gangNPCs.size());
    }

    /**
     * Test 2: Spawned gang attackers are in AGGRESSIVE state (not WANDERING),
     * so they immediately pursue the player rather than wandering aimlessly.
     */
    @Test
    void spawnedGangAttackersAreAggressive() {
        // Enter and linger to HOSTILE
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        gangTerritorySystem.update(GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f,
            player, tooltipSystem, npcManager, world);

        List<NPC> gangNPCs = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.YOUTH_GANG)
            .collect(Collectors.toList());

        assertFalse(gangNPCs.isEmpty(), "Gang NPCs should have been spawned");

        long aggressiveCount = gangNPCs.stream()
            .filter(n -> n.getState() == NPCState.AGGRESSIVE)
            .count();

        assertTrue(aggressiveCount >= 2,
            "Spawned YOUTH_GANG NPCs must be in AGGRESSIVE state, but only "
                + aggressiveCount + " of " + gangNPCs.size() + " were aggressive");
    }

    /**
     * Test 3: Attacking a gang member inside a territory also spawns gang attackers
     * (via onPlayerAttacksGang → makeNearbyGangsAggressive).
     */
    @Test
    void gangAttackersSpawnWhenPlayerAttacksGang() {
        // Enter territory → WARNED
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritorySystem.getState());

        long beforeCount = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.YOUTH_GANG)
            .count();

        // Player attacks a gang member — immediate escalation
        gangTerritorySystem.onPlayerAttacksGang(tooltipSystem, npcManager, player, world);

        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritorySystem.getState());

        long afterCount = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.YOUTH_GANG)
            .count();

        assertTrue(afterCount >= beforeCount + 2,
            "At least 2 YOUTH_GANG NPCs should be spawned when player attacks a gang member. "
                + "Before: " + beforeCount + ", After: " + afterCount);
    }

    /**
     * Test 4: Spawned attackers are close enough to reach the player (within 20 blocks).
     * Verifies they are spawned in a sensible proximity, not far across the map.
     */
    @Test
    void spawnedGangAttackersAreNearPlayer() {
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        gangTerritorySystem.update(GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f,
            player, tooltipSystem, npcManager, world);

        List<NPC> gangNPCs = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.YOUTH_GANG)
            .collect(Collectors.toList());

        assertFalse(gangNPCs.isEmpty(), "Gang NPCs should have been spawned");

        for (NPC npc : gangNPCs) {
            float dist = npc.getPosition().dst(player.getPosition());
            assertTrue(dist <= 20.0f,
                "Spawned gang NPC should be within 20 blocks of the player, but was " + dist + " blocks away");
        }
    }
}

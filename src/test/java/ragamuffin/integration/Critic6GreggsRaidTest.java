package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.core.GreggsRaidSystem;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Critic 6 Integration Tests — Greggs Raid System
 *
 * Verifies that:
 * - GreggsRaidSystem starts with level NONE and zero blocks broken
 * - Breaking the first Greggs block advances level to ALERT and shows the raid-alert tooltip
 * - Reaching the escalation threshold shows the escalation tooltip and mobilises police
 * - isPoliceAlerted and isEscalated reflect the correct state at each stage
 * - reset() returns the system to its initial state
 * - Arrest triggers a reset of the Greggs raid state
 * - NPCManager.alertPoliceToGreggRaid spawns at least one aggressive police NPC
 */
class Critic6GreggsRaidTest {

    private GreggsRaidSystem greggsRaid;
    private TooltipSystem tooltipSystem;
    private NPCManager npcManager;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        greggsRaid = new GreggsRaidSystem();
        tooltipSystem = new TooltipSystem();
        npcManager = new NPCManager();
        player = new Player(0, 1, 0);
        world = new World(99999L);
    }

    /**
     * Test 1: Initial state is NONE with zero blocks broken.
     */
    @Test
    void test1_InitialStateIsNone() {
        assertEquals(GreggsRaidSystem.RaidLevel.NONE, greggsRaid.getLevel(),
            "Raid level should start at NONE");
        assertEquals(0, greggsRaid.getBlocksBroken(),
            "No blocks broken initially");
        assertFalse(greggsRaid.isPoliceAlerted(), "Police should not be alerted initially");
        assertFalse(greggsRaid.isEscalated(), "Should not be escalated initially");
    }

    /**
     * Test 2: Breaking the first Greggs block advances level to ALERT.
     */
    @Test
    void test2_FirstBlockAdvancesToAlert() {
        greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);

        assertEquals(GreggsRaidSystem.RaidLevel.ALERT, greggsRaid.getLevel(),
            "Level should be ALERT after one block broken");
        assertEquals(1, greggsRaid.getBlocksBroken(), "One block should be recorded");
        assertTrue(greggsRaid.isPoliceAlerted(), "Police should be alerted at ALERT level");
        assertFalse(greggsRaid.isEscalated(), "Should not be escalated at ALERT level");
    }

    /**
     * Test 3: First block broken triggers the GREGGS_RAID_ALERT tooltip (once).
     */
    @Test
    void test3_FirstBlockTriggersAlertTooltip() {
        greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        tooltipSystem.update(0.01f);

        assertTrue(tooltipSystem.isActive(), "Tooltip should be active after first block");
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GREGGS_RAID_ALERT),
            "GREGGS_RAID_ALERT should have been triggered");
    }

    /**
     * Test 4: Subsequent blocks before threshold do not re-trigger the alert tooltip.
     */
    @Test
    void test4_AlertTooltipNotDuplicatedOnSecondBlock() {
        greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        tooltipSystem.update(10.0f); // Expire the first tooltip
        greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        tooltipSystem.update(0.01f);

        // The second block shouldn't push another alert tooltip (threshold not yet reached)
        // queue should be empty after the first tooltip expired
        assertEquals(0, tooltipSystem.getQueueSize(),
            "No additional tooltip should be queued for the second block before escalation");
    }

    /**
     * Test 5: Reaching the threshold advances level to ESCALATED and triggers escalation tooltip.
     */
    @Test
    void test5_ThresholdAdvancesToEscalated() {
        int threshold = GreggsRaidSystem.RAID_ESCALATION_THRESHOLD;
        for (int i = 0; i < threshold; i++) {
            greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        }

        assertEquals(GreggsRaidSystem.RaidLevel.ESCALATED, greggsRaid.getLevel(),
            "Level should be ESCALATED after threshold blocks broken");
        assertTrue(greggsRaid.isEscalated(), "isEscalated should return true");
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GREGGS_RAID_ESCALATION),
            "GREGGS_RAID_ESCALATION tooltip should have been triggered");
    }

    /**
     * Test 6: Blocks beyond threshold do not re-escalate (idempotent).
     */
    @Test
    void test6_BeyondThresholdStaysEscalated() {
        int threshold = GreggsRaidSystem.RAID_ESCALATION_THRESHOLD;
        for (int i = 0; i < threshold + 5; i++) {
            greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        }
        assertEquals(GreggsRaidSystem.RaidLevel.ESCALATED, greggsRaid.getLevel(),
            "Level stays at ESCALATED once reached");
        assertEquals(threshold + 5, greggsRaid.getBlocksBroken(),
            "Block count should be cumulative");
    }

    /**
     * Test 7: reset() returns system to initial NONE state.
     */
    @Test
    void test7_ResetClearsState() {
        int threshold = GreggsRaidSystem.RAID_ESCALATION_THRESHOLD;
        for (int i = 0; i < threshold; i++) {
            greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        }
        assertTrue(greggsRaid.isEscalated(), "Should be escalated before reset");

        greggsRaid.reset();

        assertEquals(GreggsRaidSystem.RaidLevel.NONE, greggsRaid.getLevel(),
            "Level should be NONE after reset");
        assertEquals(0, greggsRaid.getBlocksBroken(), "Block count should be 0 after reset");
        assertFalse(greggsRaid.isPoliceAlerted(), "Police should not be alerted after reset");
        assertFalse(greggsRaid.isEscalated(), "Should not be escalated after reset");
    }

    /**
     * Test 8: After reset the raid can restart fresh (first block triggers ALERT again).
     */
    @Test
    void test8_CanRestartAfterReset() {
        // First raid
        greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        greggsRaid.reset();

        // Second raid — fresh TooltipSystem (old one has GREGGS_RAID_ALERT as shown)
        tooltipSystem = new TooltipSystem();
        greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);

        assertEquals(GreggsRaidSystem.RaidLevel.ALERT, greggsRaid.getLevel(),
            "Should return to ALERT on restart");
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GREGGS_RAID_ALERT),
            "Alert tooltip should trigger again after reset");
    }

    /**
     * Test 9: NPCManager.alertPoliceToGreggRaid spawns at least one police NPC.
     */
    @Test
    void test9_AlertPoliceSpawnsPoliceNPC() {
        long policeBefore = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        npcManager.alertPoliceToGreggRaid(player, world);

        long policeAfter = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        assertTrue(policeAfter > policeBefore,
            "alertPoliceToGreggRaid should spawn at least one police NPC");
    }

    /**
     * Test 10: Newly spawned police NPC is in AGGRESSIVE state after alert.
     */
    @Test
    void test10_AlertedPoliceIsAggressive() {
        npcManager.alertPoliceToGreggRaid(player, world);

        boolean hasAggressive = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .anyMatch(n -> n.getState() == NPCState.AGGRESSIVE);

        assertTrue(hasAggressive,
            "At least one police NPC should be AGGRESSIVE after alertPoliceToGreggRaid");
    }

    /**
     * Test 11: GREGGS_RAID_ALERT and GREGGS_RAID_ESCALATION have non-empty messages.
     */
    @Test
    void test11_TooltipMessagesAreNonEmpty() {
        assertFalse(TooltipTrigger.GREGGS_RAID_ALERT.getMessage().isEmpty(),
            "GREGGS_RAID_ALERT message should not be empty");
        assertFalse(TooltipTrigger.GREGGS_RAID_ESCALATION.getMessage().isEmpty(),
            "GREGGS_RAID_ESCALATION message should not be empty");
    }

    /**
     * Test 12: Full raid pipeline — first block, escalation, police spawned, messages shown.
     */
    @Test
    void test12_FullRaidPipeline() {
        // Step 1: first block — police alerted
        greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        tooltipSystem.update(0.01f);
        assertEquals(GreggsRaidSystem.RaidLevel.ALERT, greggsRaid.getLevel());
        assertTrue(tooltipSystem.isActive(), "Alert tooltip should be showing");

        // Step 2: break remaining blocks to escalate
        int remaining = GreggsRaidSystem.RAID_ESCALATION_THRESHOLD - 1;
        for (int i = 0; i < remaining; i++) {
            greggsRaid.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
        }
        assertEquals(GreggsRaidSystem.RaidLevel.ESCALATED, greggsRaid.getLevel());

        // Step 3: verify police were mobilised
        boolean hasAggressive = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .anyMatch(n -> n.getState() == NPCState.AGGRESSIVE);
        assertTrue(hasAggressive, "Police should be aggressive after escalation");
    }
}

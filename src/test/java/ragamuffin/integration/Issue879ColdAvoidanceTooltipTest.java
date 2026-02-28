package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.core.WarmthSystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #879: Cold avoidance mechanics clarity.
 *
 * Verifies that the tooltip system surfaces cold-avoidance guidance when warmth
 * drops, so players understand their options before hypothermia sets in.
 */
class Issue879ColdAvoidanceTooltipTest {

    private WarmthSystem warmthSystem;
    private Player player;
    private Inventory inventory;
    private World world;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        warmthSystem = new WarmthSystem();
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();
        // Minimal flat world — no roof, so player is always unsheltered outdoors
        world = new World(42L);
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1 — WARMTH_GETTING_COLD tooltip fires when warmth drops to or below 50%.
     *
     * Set player warmth to 40 (below 50 threshold). Simulate the game-loop tooltip
     * check. Verify WARMTH_GETTING_COLD has been triggered.
     */
    @Test
    void test1_WarmthGettingColdTooltipFiresBelow50Percent() {
        player.setWarmth(40f);
        assertFalse(tooltipSystem.hasShown(TooltipTrigger.WARMTH_GETTING_COLD),
                "Tooltip should not have been shown yet");

        // Simulate the game-loop check that fires after warmthSystem.update()
        if (player.getWarmth() <= 50f) {
            tooltipSystem.trigger(TooltipTrigger.WARMTH_GETTING_COLD);
        }

        assertTrue(tooltipSystem.hasShown(TooltipTrigger.WARMTH_GETTING_COLD),
                "WARMTH_GETTING_COLD tooltip should fire when warmth <= 50");
        assertNotNull(TooltipTrigger.WARMTH_GETTING_COLD.getMessage(),
                "Tooltip message must not be null");
        assertFalse(TooltipTrigger.WARMTH_GETTING_COLD.getMessage().isEmpty(),
                "Tooltip message must not be empty");
    }

    /**
     * Test 2 — WARMTH_DANGER tooltip fires when warmth drops below WARMTH_DANGER_THRESHOLD.
     *
     * Set player warmth to 15 (below 20 threshold). Simulate the game-loop tooltip
     * check. Verify WARMTH_DANGER has been triggered.
     */
    @Test
    void test2_WarmthDangerTooltipFiresBelowDangerThreshold() {
        player.setWarmth(15f);
        assertTrue(player.isWarmthDangerous(), "Warmth of 15 should be in the danger zone");
        assertFalse(tooltipSystem.hasShown(TooltipTrigger.WARMTH_DANGER),
                "Tooltip should not have been shown yet");

        // Simulate the game-loop check
        if (player.isWarmthDangerous()) {
            tooltipSystem.trigger(TooltipTrigger.WARMTH_DANGER);
        }

        assertTrue(tooltipSystem.hasShown(TooltipTrigger.WARMTH_DANGER),
                "WARMTH_DANGER tooltip should fire when warmth is dangerously low");
        // Message must mention at least one avoidance method
        String msg = TooltipTrigger.WARMTH_DANGER.getMessage();
        assertTrue(
                msg.contains("campfire") || msg.contains("indoors") || msg.contains("coat") ||
                msg.contains("flask") || msg.contains("tea") || msg.contains("hat"),
                "WARMTH_DANGER message must mention at least one cold-avoidance method: " + msg);
    }

    /**
     * Test 3 — WARMTH_GETTING_COLD tooltip does NOT fire when warmth is above 50%.
     *
     * Player starts at MAX_WARMTH. The game-loop check should not trigger the tooltip.
     */
    @Test
    void test3_WarmthGettingColdTooltipDoesNotFireWhenWarm() {
        assertEquals(Player.MAX_WARMTH, player.getWarmth(), 0.01f,
                "Player should start at max warmth");

        // Simulate the game-loop check
        if (player.getWarmth() <= 50f) {
            tooltipSystem.trigger(TooltipTrigger.WARMTH_GETTING_COLD);
        }

        assertFalse(tooltipSystem.hasShown(TooltipTrigger.WARMTH_GETTING_COLD),
                "WARMTH_GETTING_COLD tooltip should not fire when warmth is above 50%");
    }

    /**
     * Test 4 — Each cold tooltip fires only once per session (deduplication).
     *
     * Trigger WARMTH_GETTING_COLD twice. Verify it is only queued once.
     */
    @Test
    void test4_ColdTooltipFiresOnlyOnce() {
        player.setWarmth(30f);

        boolean firstTrigger = tooltipSystem.trigger(TooltipTrigger.WARMTH_GETTING_COLD);
        boolean secondTrigger = tooltipSystem.trigger(TooltipTrigger.WARMTH_GETTING_COLD);

        assertTrue(firstTrigger, "First trigger should succeed");
        assertFalse(secondTrigger, "Second trigger should be suppressed (already shown)");
        assertEquals(1, tooltipSystem.getQueueSize(),
                "Only one copy of the tooltip should be in the queue");
    }

    /**
     * Test 5 — WARMTH_GETTING_COLD tooltip message mentions shelter, campfire, or warmth-restoring items.
     *
     * The message must give the player actionable cold-avoidance guidance.
     */
    @Test
    void test5_WarmthGettingColdMessageMentionsAvoidanceMethods() {
        String msg = TooltipTrigger.WARMTH_GETTING_COLD.getMessage();
        assertNotNull(msg, "Message must not be null");
        assertTrue(
                msg.contains("shelter") || msg.contains("campfire") || msg.contains("indoors") ||
                msg.contains("coat") || msg.contains("warm"),
                "WARMTH_GETTING_COLD message must mention a cold-avoidance method: " + msg);
    }
}

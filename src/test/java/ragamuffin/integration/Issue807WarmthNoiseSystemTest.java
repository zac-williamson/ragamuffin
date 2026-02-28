package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.NoiseSystem;
import ragamuffin.core.WarmthSystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #807: WarmthSystem and NoiseSystem wired into the game loop.
 *
 * Verifies the exact scenarios specified in SPEC.md:
 * 1. Warmth drains in rain outdoors
 * 2. Hypothermia causes damage
 * 3. Coat halves warmth drain
 * 4. NPC hears player block-break
 * 5. Noise decays to baseline after spike
 */
class Issue807WarmthNoiseSystemTest {

    private WarmthSystem warmthSystem;
    private NoiseSystem noiseSystem;
    private Player player;
    private Inventory inventory;
    private World world;

    @BeforeEach
    void setUp() {
        warmthSystem = new WarmthSystem();
        noiseSystem = new NoiseSystem();
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        // Minimal flat world with no roof (player is always unsheltered outdoors)
        world = new World(42L);
        // Ground blocks so player is on solid terrain
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1 — Warmth drains in rain outdoors.
     *
     * Set weather to OVERCAST (not raining, but should have some drain),
     * or RAIN (raining). Per SPEC: verify player.getWarmth() decreases over 120 seconds.
     */
    @Test
    void test1_WarmthDrainsInRainOutdoors() {
        // Use RAIN weather — has a positive warmth drain rate outdoors
        Weather weather = Weather.RAIN;
        float initialWarmth = player.getWarmth();
        assertEquals(Player.MAX_WARMTH, initialWarmth, 0.01f,
                "Player should start at max warmth");

        // Advance 120 simulated seconds in small steps (1s each)
        for (int i = 0; i < 120; i++) {
            warmthSystem.update(player, weather, world, 1.0f, false, inventory);
        }

        assertTrue(player.getWarmth() < initialWarmth,
                "Warmth should have decreased after 120 seconds of rain outdoors");
    }

    /**
     * Test 2 — Hypothermia causes damage when warmth < WARMTH_DANGER_THRESHOLD.
     *
     * Set player warmth to 10 (below threshold of 20). Call warmthSystem.update once
     * with delta=1.0. Verify health decreases by approximately WARMTH_DAMAGE_PER_SECOND.
     */
    @Test
    void test2_HypothermiaCausesDamage() {
        player.setWarmth(10f);
        assertTrue(player.isWarmthDangerous(), "Warmth of 10 should be in danger zone");

        float healthBefore = player.getHealth();
        warmthSystem.update(player, Weather.FROST, world, 1.0f, false, inventory);

        float healthAfter = player.getHealth();
        float actualDamage = healthBefore - healthAfter;

        assertTrue(actualDamage > 0, "Player should take damage from hypothermia");
        assertEquals(Player.WARMTH_DAMAGE_PER_SECOND, actualDamage, 0.1f,
                "Hypothermia damage should be approximately WARMTH_DAMAGE_PER_SECOND per second");
    }

    /**
     * Test 3 — Coat halves warmth drain.
     *
     * Compare warmth drain over 10 seconds with vs without a coat in FROST weather.
     * Coat drain must be ≤ 55% of the no-coat drain (50% reduction ± 5% tolerance).
     */
    @Test
    void test3_CoatHalvesWarmthDrain() {
        // Measure drain WITHOUT coat
        Player playerNoCoat = new Player(0, 1, 0);
        playerNoCoat.setWarmth(Player.MAX_WARMTH);
        Inventory invNoCoat = new Inventory(36);
        for (int i = 0; i < 10; i++) {
            warmthSystem.update(playerNoCoat, Weather.FROST, world, 1.0f, false, invNoCoat);
        }
        float drainWithout = Player.MAX_WARMTH - playerNoCoat.getWarmth();

        // Measure drain WITH coat
        Player playerWithCoat = new Player(0, 1, 0);
        playerWithCoat.setWarmth(Player.MAX_WARMTH);
        Inventory invWithCoat = new Inventory(36);
        invWithCoat.addItem(Material.COAT, 1);
        for (int i = 0; i < 10; i++) {
            warmthSystem.update(playerWithCoat, Weather.FROST, world, 1.0f, false, invWithCoat);
        }
        float drainWith = Player.MAX_WARMTH - playerWithCoat.getWarmth();

        assertTrue(drainWithout > 0, "There should be warmth drain without a coat in FROST");
        assertTrue(drainWith >= 0, "Coat drain must be non-negative");

        // With coat drain should be ≤ 55% of without-coat drain (50% ± 5% tolerance)
        float ratio = drainWith / drainWithout;
        assertTrue(ratio <= 0.55f,
                String.format("Coat drain ratio %.3f should be ≤ 0.55 (50%% reduction ± 5%%)", ratio));
    }

    /**
     * Test 4 — NPC hears player block-break.
     *
     * Place a police NPC 8 blocks from the player. Fire noiseSystem.spikeBlockBreak()
     * and player.setNoiseLevel(). Verify hearing range ≥ 8f and police NPC transitions
     * to ALERTED on next NPCManager update tick.
     */
    @Test
    void test4_NPCHearsPlayerBlockBreak() {
        // Set up world with ground
        World testWorld = new World(99L);
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                testWorld.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        Player testPlayer = new Player(0f, 1f, 0f);
        NPCManager npcManager = new NPCManager();
        TooltipSystem tooltipSystem = new TooltipSystem();
        Inventory inv = new Inventory(36);

        // Place police NPC 8 blocks away
        NPC policeNPC = new NPC(8f, 1f, 0f, NPCType.POLICE);
        policeNPC.setState(NPCState.PATROLLING);
        npcManager.getNPCs().add(policeNPC);

        // Spike block-break noise
        noiseSystem.spikeBlockBreak();
        testPlayer.setNoiseLevel(noiseSystem.getNoiseLevel());

        // Verify hearing range is ≥ 8 blocks
        float hearingRange = NoiseSystem.getHearingRange(testPlayer.getNoiseLevel());
        assertTrue(hearingRange >= 8f,
                String.format("Hearing range %.2f should be >= 8 blocks after block break", hearingRange));

        // Run one NPCManager update tick — police should hear the player and become ALERTED
        npcManager.update(1f / 60f, testWorld, testPlayer, inv, tooltipSystem);

        assertEquals(NPCState.ALERTED, policeNPC.getState(),
                "Police NPC should transition to ALERTED after hearing a nearby block break");
    }

    /**
     * Test 5 — Noise decays to baseline after spike.
     *
     * Fire noiseSystem.spikeBlockBreak(). Advance 3.0 simulated seconds via noiseSystem.update()
     * with isMoving=false. Verify noise is within 0.1 of NOISE_STILL (0.05).
     */
    @Test
    void test5_NoiseDecaysToBaselineAfterSpike() {
        noiseSystem.spikeBlockBreak();
        assertEquals(NoiseSystem.NOISE_BLOCK_BREAK, noiseSystem.getNoiseLevel(), 0.01f,
                "Noise should immediately spike to NOISE_BLOCK_BREAK after block break");

        // Advance 3.0 simulated seconds in small steps
        float elapsed = 0f;
        float step = 1f / 60f;
        while (elapsed < 3.0f) {
            noiseSystem.update(step, false, false);
            elapsed += step;
        }

        float finalNoise = noiseSystem.getNoiseLevel();
        assertEquals(NoiseSystem.NOISE_STILL, finalNoise, 0.1f,
                String.format("Noise %.4f should decay to within 0.1 of NOISE_STILL (%.2f) after 3 seconds",
                        finalNoise, NoiseSystem.NOISE_STILL));
    }
}

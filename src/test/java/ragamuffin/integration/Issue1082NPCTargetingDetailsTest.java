package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockBreaker;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.GameHUD;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;
import ragamuffin.core.NPCHitDetector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1082: Display NPC details when targeting them.
 *
 * <p>Verifies that the GameHUD correctly stores and exposes the targeted NPC,
 * that the NPC detail panel logic works end-to-end, and that state descriptions
 * are human-readable.
 */
class Issue1082NPCTargetingDetailsTest {

    private static final float PUNCH_REACH = 5.0f;
    private static final Vector3 PLAYER_EYE = new Vector3(0f, 1.8f, 0f);

    private World world;
    private BlockBreaker blockBreaker;
    private NPCManager npcManager;
    private Player player;
    private GameHUD gameHUD;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        blockBreaker = new BlockBreaker();
        npcManager = new NPCManager();
        player = new Player(0, 1, 0);
        gameHUD = new GameHUD(player);

        // Flat pavement so nothing obstructs the NPC ray
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 10; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: setTargetNPC / getTargetNPC roundtrip
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Setting a targeted NPC on the HUD and reading it back must return the same object.
     */
    @Test
    void test1_setAndGetTargetNPCRoundtrip() {
        NPC npc = npcManager.spawnNPC(NPCType.POLICE, 0f, 1f, 3f);
        assertNull(gameHUD.getTargetNPC(), "Precondition: targetNPC must start null");

        gameHUD.setTargetNPC(npc);
        assertSame(npc, gameHUD.getTargetNPC(),
                "getTargetNPC() must return the exact NPC that was set");

        gameHUD.setTargetNPC(null);
        assertNull(gameHUD.getTargetNPC(), "Clearing targetNPC with null must work");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: NPC in punch reach populates targetNPC on HUD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When an NPC is directly in front of the player and within reach, the
     * NPCHitDetector must find it, and the HUD can display its details.
     */
    @Test
    void test2_NPCInReachIsDetectedAndStoredOnHUD() {
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, 0f, 1f, 3f);

        Vector3 dir = new Vector3(0f, 0f, 1f); // looking toward +Z
        NPC detected = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, dir, PUNCH_REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNotNull(detected, "NPC at Z=3 must be detected within reach of 5.0");

        // Simulate the HUD update that happens in renderUI()
        gameHUD.setTargetNPC(detected);
        assertNotNull(gameHUD.getTargetNPC(), "HUD must store the detected NPC");
        assertEquals(NPCType.PUBLIC, gameHUD.getTargetNPC().getType(),
                "Stored NPC type must match the spawned NPC");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: NPC out of reach results in null targetNPC on HUD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * An NPC beyond PUNCH_REACH (5 blocks) must not be detected; HUD clears.
     */
    @Test
    void test3_NPCBeyondReachClearsTargetNPC() {
        npcManager.spawnNPC(NPCType.PUBLIC, 0f, 1f, 10f); // Z=10, beyond reach of 5

        Vector3 dir = new Vector3(0f, 0f, 1f);
        NPC detected = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, dir, PUNCH_REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNull(detected, "NPC at Z=10 must NOT be detected with reach=5.0");

        // Simulate HUD update: no NPC → clear
        gameHUD.setTargetNPC(detected); // null
        assertNull(gameHUD.getTargetNPC(), "HUD targetNPC must be null when no NPC is in reach");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Named NPC name is exposed correctly
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A named NPC (e.g. "Terry") must report its unique name via isNamed()/getName().
     * The HUD detail panel uses this to display the name, not the type enum.
     */
    @Test
    void test4_NamedNPCNameIsAvailableForDetailPanel() {
        NPC terry = npcManager.spawnNamedNPC(NPCType.LANDLORD, "Terry", 0f, 1f, 3f);
        assertTrue(terry.isNamed(), "Terry must be considered a named NPC");
        assertEquals("Terry", terry.getName(), "Named NPC must return its unique name");

        gameHUD.setTargetNPC(terry);
        NPC stored = gameHUD.getTargetNPC();
        assertNotNull(stored);
        assertTrue(stored.isNamed(), "Stored NPC must still be named");
        assertEquals("Terry", stored.getName(), "Name must survive HUD storage");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: NPC health is accessible for the health bar
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The health bar in the NPC detail panel needs getHealth() and getType().getMaxHealth().
     * Verify fresh NPC is at full health and that the ratio is 1.0.
     */
    @Test
    void test5_NPCHealthRatioIsCorrectForHealthBar() {
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 0f, 1f, 3f);
        float maxHP = police.getType().getMaxHealth();
        float curHP = police.getHealth();

        assertTrue(maxHP > 0f, "Max HP must be positive");
        assertEquals(maxHP, curHP, 0.001f, "Fresh NPC must be at full health");

        float hpPct = curHP / maxHP;
        assertEquals(1.0f, hpPct, 0.001f, "Full-health NPC must yield HP ratio of 1.0");

        // Damage the NPC to confirm the ratio drops
        police.takeDamage(10f);
        float damagedPct = police.getHealth() / maxHP;
        assertTrue(damagedPct < 1.0f, "Damaged NPC HP ratio must be below 1.0");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: State descriptions are human-readable (not raw enum names)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verify that key NPC states produce readable strings (no underscores,
     * not all-caps) via the GameHUD.describeNPCState() helper.
     */
    @Test
    void test6_StateDescriptionsAreHumanReadable() {
        assertReadable(NPCState.IDLE);
        assertReadable(NPCState.WANDERING);
        assertReadable(NPCState.PATROLLING);
        assertReadable(NPCState.AGGRESSIVE);
        assertReadable(NPCState.FLEEING);
        assertReadable(NPCState.KNOCKED_OUT);
        assertReadable(NPCState.CHASING_PLAYER);
    }

    /**
     * Assert that the description for a state does not look like a raw enum name
     * (all uppercase with underscores).
     */
    private void assertReadable(NPCState state) {
        String desc = GameHUD.describeNPCState(state);
        assertNotNull(desc, "Description for " + state + " must not be null");
        assertFalse(desc.isEmpty(), "Description for " + state + " must not be empty");

        // A human-readable description will not be ALL_CAPS_WITH_UNDERSCORES
        boolean isAllCapsUnderscored = desc.equals(desc.toUpperCase())
                && desc.contains("_");
        assertFalse(isAllCapsUnderscored,
                "Description for " + state + " must be human-readable, not raw enum: '" + desc + "'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 7: Targeting a block (not NPC) does not set targetNPC
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When the player aims at a block and there is no NPC in reach, the HUD's
     * targetNPC should remain null (simulating the renderUI() logic).
     */
    @Test
    void test7_TargetingBlockDoesNotSetTargetNPC() {
        // Place a wall block directly ahead; no NPC is spawned
        world.setBlock(0, 1, 3, BlockType.BRICK);

        Vector3 dir = new Vector3(0f, 0f, 1f);
        NPC detected = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, dir, PUNCH_REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNull(detected, "No NPC must be detected when only a block is present");

        // Simulate the else-branch in renderUI()
        if (detected != null) {
            gameHUD.setTargetNPC(detected);
        } else {
            gameHUD.setTargetNPC(null);
        }

        assertNull(gameHUD.getTargetNPC(),
                "HUD targetNPC must be null when the player is targeting a block, not an NPC");
    }
}

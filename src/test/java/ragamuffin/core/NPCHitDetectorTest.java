package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockBreaker;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NPCHitDetector} — verifies the widened hit cone introduced
 * by Fix #242.
 *
 * <p>Geometry for the main cone tests: player eye at (0, 1.8, 0), NPC at (0, 1, 2).
 * The camera initially points straight along +Z towards the NPC. We then rotate the
 * look direction by ±20° and ±40° in the XZ-plane to test the hit/miss boundary.
 *
 * <p>cos(20°) ≈ 0.940 &gt; {@value NPCHitDetector#HIT_CONE_DOT} → hit
 * <br>cos(40°) ≈ 0.766 &lt; {@value NPCHitDetector#HIT_CONE_DOT} → miss
 */
class NPCHitDetectorTest {

    /** Player eye position (camera origin). */
    private static final Vector3 PLAYER_EYE = new Vector3(0f, 1.8f, 0f);

    /** NPC placed 2 blocks in front of the player, at foot level. */
    private static final float NPC_X = 0f;
    private static final float NPC_Y = 1f;
    private static final float NPC_Z = 2f;

    /** Maximum punch reach used in all tests. */
    private static final float REACH = 3.5f;

    private World world;
    private BlockBreaker blockBreaker;
    private NPCManager npcManager;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        blockBreaker = new BlockBreaker();
        npcManager = new NPCManager();

        // Flat ground so nothing obstructs the ray by default
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 10; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
    }

    // -------------------------------------------------------------------------
    // isInHitCone — pure-math tests, no world/NPC needed
    // -------------------------------------------------------------------------

    /**
     * Aiming exactly at the NPC centre (dot = 1.0) must return true.
     */
    @Test
    void test_isInHitCone_exactCentre_hits() {
        // Direction vector pointing directly at NPC centre from PLAYER_EYE
        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        float dx = NPC_X - PLAYER_EYE.x;
        float dy = npcCentreY - PLAYER_EYE.y;
        float dz = NPC_Z - PLAYER_EYE.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float invDist = 1f / dist;

        Vector3 direction = new Vector3(dx * invDist, dy * invDist, dz * invDist);

        assertTrue(NPCHitDetector.isInHitCone(dx, dy, dz, invDist, direction),
                "Exact centre aim must be within the hit cone");
    }

    /**
     * Aiming 20° to the right (XZ-plane) — cos(20°) ≈ 0.940 > 0.9 — must hit.
     */
    @Test
    void test_isInHitCone_20degreesOff_hits() {
        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        float dx = NPC_X - PLAYER_EYE.x;
        float dy = npcCentreY - PLAYER_EYE.y;
        float dz = NPC_Z - PLAYER_EYE.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float invDist = 1f / dist;

        // Camera look direction rotated 20° right in the XZ plane
        double angle = Math.toRadians(20.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        assertTrue(NPCHitDetector.isInHitCone(dx, dy, dz, invDist, direction),
                "20° off-centre aim must be within the hit cone (cos 20° ≈ 0.940 > 0.9)");
    }

    /**
     * Aiming 20° to the left — same as right by symmetry — must hit.
     */
    @Test
    void test_isInHitCone_20degreesLeft_hits() {
        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        float dx = NPC_X - PLAYER_EYE.x;
        float dy = npcCentreY - PLAYER_EYE.y;
        float dz = NPC_Z - PLAYER_EYE.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float invDist = 1f / dist;

        double angle = Math.toRadians(-20.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        assertTrue(NPCHitDetector.isInHitCone(dx, dy, dz, invDist, direction),
                "20° left off-centre aim must be within the hit cone");
    }

    /**
     * Aiming 40° to the right — cos(40°) ≈ 0.766 < 0.9 — must miss.
     */
    @Test
    void test_isInHitCone_40degreesOff_misses() {
        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        float dx = NPC_X - PLAYER_EYE.x;
        float dy = npcCentreY - PLAYER_EYE.y;
        float dz = NPC_Z - PLAYER_EYE.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float invDist = 1f / dist;

        double angle = Math.toRadians(40.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        assertFalse(NPCHitDetector.isInHitCone(dx, dy, dz, invDist, direction),
                "40° off-centre aim must be outside the hit cone (cos 40° ≈ 0.766 < 0.9)");
    }

    /**
     * Aiming 40° to the left — must miss.
     */
    @Test
    void test_isInHitCone_40degreesLeft_misses() {
        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        float dx = NPC_X - PLAYER_EYE.x;
        float dy = npcCentreY - PLAYER_EYE.y;
        float dz = NPC_Z - PLAYER_EYE.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float invDist = 1f / dist;

        double angle = Math.toRadians(-40.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        assertFalse(NPCHitDetector.isInHitCone(dx, dy, dz, invDist, direction),
                "40° left off-centre aim must be outside the hit cone");
    }

    // -------------------------------------------------------------------------
    // findNPCInReach — end-to-end tests with NPC and World
    // -------------------------------------------------------------------------

    /**
     * Player aims directly at the NPC (2 blocks away) — the NPC should be found.
     */
    @Test
    void test_findNPCInReach_directAim_findsNPC() {
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);
        assertNotNull(npc);

        // Direction pointing directly from PLAYER_EYE to NPC centre
        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        Vector3 toNPC = new Vector3(
                NPC_X - PLAYER_EYE.x,
                npcCentreY - PLAYER_EYE.y,
                NPC_Z - PLAYER_EYE.z).nor();

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, toNPC, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNotNull(result, "Direct aim at NPC should find it");
        assertEquals(npc, result, "Should return the exact NPC that was spawned");
    }

    /**
     * Player aims 20° off to the right — within the widened cone — NPC should be found.
     *
     * <p>This test would have FAILED with the old 0.985f threshold (cos 20° ≈ 0.940 &lt; 0.985).
     */
    @Test
    void test_findNPCInReach_20degreesOff_findsNPC() {
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);
        assertNotNull(npc);

        double angle = Math.toRadians(20.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, direction, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNotNull(result,
                "NPC should be found when aiming 20° off-centre " +
                "(Fix #242: old 0.985 threshold would have missed this)");
    }

    /**
     * Player aims 20° off to the left — within the widened cone — NPC should be found.
     */
    @Test
    void test_findNPCInReach_20degreesLeft_findsNPC() {
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);
        assertNotNull(npc);

        double angle = Math.toRadians(-20.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, direction, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNotNull(result, "NPC should be found when aiming 20° left off-centre");
    }

    /**
     * Player aims 40° off — outside the hit cone — NPC should NOT be found.
     */
    @Test
    void test_findNPCInReach_40degreesOff_returnsNull() {
        npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);

        double angle = Math.toRadians(40.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, direction, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNull(result, "NPC should NOT be found when aiming 40° off-centre");
    }

    /**
     * Player aims 40° to the left — outside the hit cone — NPC should NOT be found.
     */
    @Test
    void test_findNPCInReach_40degreesLeft_returnsNull() {
        npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);

        double angle = Math.toRadians(-40.0);
        Vector3 direction = new Vector3(
                (float) Math.sin(angle), 0f, (float) Math.cos(angle));

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, direction, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNull(result, "NPC should NOT be found when aiming 40° left off-centre");
    }

    /**
     * Regression: NPC behind a solid wall must NOT be found even when aimed at.
     *
     * <p>A STONE block is placed at (0, 1, 1) — between player (at z=0) and NPC (at z=2).
     * The block is closer than the NPC so blockDistance &lt; npc distance, preventing the punch.
     */
    @Test
    void test_findNPCInReach_npcBehindWall_returnsNull() {
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);
        assertNotNull(npc);

        // Place a solid wall between the player eye and the NPC
        world.setBlock(0, 1, 1, BlockType.STONE);

        // Aim directly at the NPC
        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        Vector3 toNPC = new Vector3(
                NPC_X - PLAYER_EYE.x,
                npcCentreY - PLAYER_EYE.y,
                NPC_Z - PLAYER_EYE.z).nor();

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, toNPC, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNull(result,
                "NPC behind a solid wall must not be reachable (block-distance guard regression)");
    }

    /**
     * Dead NPCs cannot be punched — findNPCInReach should skip them.
     */
    @Test
    void test_findNPCInReach_deadNPC_returnsNull() {
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);
        assertNotNull(npc);

        // Kill the NPC directly
        while (npc.isAlive()) {
            npcManager.punchNPC(npc, new Vector3(0, 0, 1));
        }
        assertFalse(npc.isAlive(), "NPC should be dead before the hit-detection test");

        float npcCentreY = NPC_Y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
        Vector3 toNPC = new Vector3(
                NPC_X - PLAYER_EYE.x,
                npcCentreY - PLAYER_EYE.y,
                NPC_Z - PLAYER_EYE.z).nor();

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, toNPC, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNull(result, "Dead NPC must not be returned by findNPCInReach");
    }

    /**
     * NPC beyond punch reach (3.5 blocks) must not be found even with direct aim.
     */
    @Test
    void test_findNPCInReach_npcTooFar_returnsNull() {
        // Place NPC 5 blocks away (beyond REACH = 3.5)
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, 0f, 1f, 5f);
        assertNotNull(npc);

        // Aim directly along +Z
        Vector3 direction = new Vector3(0f, 0f, 1f);

        NPC result = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, direction, REACH,
                npcManager.getNPCs(), blockBreaker, world);

        assertNull(result, "NPC beyond punch reach must not be found");
    }

    /**
     * Verify the HIT_CONE_DOT constant is 0.9f as required by Fix #242.
     */
    @Test
    void test_hitConeDotConstant_is0point9() {
        assertEquals(0.9f, NPCHitDetector.HIT_CONE_DOT, 1e-6f,
                "HIT_CONE_DOT must be 0.9f (≈26° half-angle) per Fix #242");
    }
}

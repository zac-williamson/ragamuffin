package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1202 — Northfield Karaoke Night:
 * Wetherspoons Friday Special, Crowd Reaction &amp; the Mic Drop Hustle.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>Successful karaoke — GREAT performance: 3/3 hits → Notoriety +5,
 *       5 COIN tip, LOCAL_EVENT rumour, KARAOKE_KING achievement.</li>
 *   <li>TERRIBLE performance — glassing: 0/3 hits → Notoriety +2,
 *       ≥2 NPCs JEERING, pint glass spawned, LOCAL_EVENT rumour with "bottled".</li>
 *   <li>Bev blocks high-notoriety player: Notoriety ≥ 50 → REFUSED_HIGH_NOTORIETY,
 *       Bev speech contains "not touching my mic".</li>
 *   <li>Fuse box sabotage — unwitnessed: SCREWDRIVER in inventory, no NPCs
 *       within 4 blocks → PA cut, Notoriety unchanged.</li>
 *   <li>Karaoke inactive outside Friday night: Saturday 00:01 →
 *       isKaraokeActive() == false, Bev says "not on tonight, love".</li>
 * </ol>
 */
class Issue1202KaraokeTest {

    private KaraokeSystem karaoke;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;
    private WetherspoonsSystem wetherspoonsSystem;
    private Inventory inventory;
    private List<NPC> pubNpcs;

    @BeforeEach
    void setUp() {
        karaoke = new KaraokeSystem(new Random(42));
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();
        rumourNetwork = new RumourNetwork();
        wetherspoonsSystem = new WetherspoonsSystem(new Random(42));

        karaoke.setNotorietySystem(notorietySystem);
        karaoke.setCriminalRecord(criminalRecord);
        karaoke.setAchievementSystem(achievementSystem);
        karaoke.setRumourNetwork(rumourNetwork);
        karaoke.setWetherspoonsSystem(wetherspoonsSystem);

        karaoke.forceSpawnBev();

        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 20);

        // Populate the pub with 6 PUBLIC NPCs
        pubNpcs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            pubNpcs.add(new NPC(NPCType.PUBLIC, i * 2f, 0f, 5f));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Successful karaoke — GREAT performance
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Set TimeSystem to Friday 20:30. Player queues at KARAOKE_BOOTH_PROP.
     * Mock queue wait (0 turns). Perform with 3/3 hits.
     * Verify:
     * - NotorietySystem.getNotoriety() increased by 5
     * - player inventory COIN increased by 5 (Bev's tip)
     * - at least one LOCAL_EVENT rumour in RumourNetwork
     * - AchievementSystem has KARAOKE_KING unlocked
     */
    @Test
    void greatPerformance_notorietyTipRumourAchievement() {
        int dayOfWeek = KaraokeSystem.FRIDAY_INDEX; // Friday
        float hour = 20.5f; // 20:30

        // Verify karaoke is active on Friday 20:30
        assertTrue(karaoke.isKaraokeActive(hour, dayOfWeek),
                "Karaoke should be active on Friday 20:30");

        // Queue for stage (player has low notoriety)
        KaraokeSystem.QueueResult queueResult =
                karaoke.tryQueueForStage(notorietySystem.getNotoriety(), hour, dayOfWeek);
        assertEquals(KaraokeSystem.QueueResult.QUEUED, queueResult,
                "Should be able to queue at low notoriety");

        // Force queue turns = 0 so player is called immediately
        karaoke.setQueueTurnsRemainingForTesting(0);
        assertTrue(karaoke.advanceQueue(), "Player should be called immediately with 0 turns");

        // Begin performance
        String song = karaoke.beginPerformance();
        assertNotNull(song, "A song should be selected");
        assertTrue(karaoke.isPaActive(), "PA should be active during performance");

        int notorietyBefore = notorietySystem.getNotoriety();
        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Resolve performance with 3/3 hits
        KaraokeSystem.PerformanceResult result =
                karaoke.resolvePerformance(3, pubNpcs, inventory, 10f, 10f);

        assertEquals(KaraokeSystem.PerformanceResult.GREAT, result,
                "3/3 hits should yield GREAT");
        assertEquals(notorietyBefore + KaraokeSystem.NOTORIETY_GREAT,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by 5 on GREAT");
        assertEquals(coinsBefore + KaraokeSystem.BEV_TIP_GREAT,
                inventory.getItemCount(Material.COIN),
                "Player should receive 5 COIN tip from Bev on GREAT");

        // At least one NPC should be celebrating
        boolean anyCelebrating = pubNpcs.stream()
                .anyMatch(npc -> npc.getState() == NPCState.CELEBRATING);
        assertTrue(anyCelebrating, "At least one NPC should be CELEBRATING on GREAT");

        // KARAOKE_KING achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.KARAOKE_KING),
                "KARAOKE_KING achievement should be unlocked on GREAT");

        // Wetherspoons round-buying spike
        assertTrue(wetherspoonsSystem.isKaraokeRoundBuyingActive(),
                "Wetherspoons round-buying spike should be triggered on GREAT");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: TERRIBLE performance — glassing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Set to Friday 21:00. Player queues. Perform with 0/3 hits.
     * Verify:
     * - NotorietySystem increased by 2
     * - at least 2 NPCs have state NPCState.JEERING
     * - pint glass projectile spawned (isPintGlassSpawned() == true)
     * - RumourNetwork has a LOCAL_EVENT rumour containing "bottled"
     * - BOTTLED_IT achievement unlocked
     */
    @Test
    void terriblePerformance_jeersGlassingRumourAchievement() {
        int dayOfWeek = KaraokeSystem.FRIDAY_INDEX;
        float hour = 21.0f;

        // Queue and begin
        KaraokeSystem.QueueResult queueResult =
                karaoke.tryQueueForStage(notorietySystem.getNotoriety(), hour, dayOfWeek);
        assertEquals(KaraokeSystem.QueueResult.QUEUED, queueResult);

        karaoke.setQueueTurnsRemainingForTesting(0);
        karaoke.advanceQueue();
        karaoke.beginPerformance();

        int notorietyBefore = notorietySystem.getNotoriety();

        // Perform with 0 hits
        KaraokeSystem.PerformanceResult result =
                karaoke.resolvePerformance(0, pubNpcs, inventory, 10f, 10f);

        assertEquals(KaraokeSystem.PerformanceResult.TERRIBLE, result,
                "0/3 hits should yield TERRIBLE");
        assertEquals(notorietyBefore + KaraokeSystem.NOTORIETY_TERRIBLE,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by 2 on TERRIBLE");

        // At least 2 NPCs jeering
        long jeeringCount = pubNpcs.stream()
                .filter(npc -> npc.getState() == NPCState.JEERING)
                .count();
        assertTrue(jeeringCount >= 2,
                "At least 2 NPCs should be JEERING on TERRIBLE (got " + jeeringCount + ")");

        // Pint glass projectile spawned
        assertTrue(karaoke.isPintGlassSpawned(),
                "A pint glass projectile should be spawned on TERRIBLE");

        // BOTTLED_IT achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.BOTTLED_IT),
                "BOTTLED_IT achievement should be unlocked on TERRIBLE");

        // LOCAL_EVENT rumour containing "bottled"
        boolean hasBottledRumour = rumourNetwork.getAllRumours().stream()
                .anyMatch(r -> r.getType() == RumourType.LOCAL_EVENT
                        && r.getText().toLowerCase().contains("bottled"));
        assertTrue(hasBottledRumour,
                "RumourNetwork should have a LOCAL_EVENT rumour containing 'bottled'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Bev blocks high-notoriety player
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Set player Notoriety to 55. Set to Friday 20:00.
     * Player presses E on KARAOKE_BOOTH_PROP.
     * Verify:
     * - tryQueueForStage returns REFUSED_HIGH_NOTORIETY
     * - Bev's speech text contains "not touching my mic"
     */
    @Test
    void bevRefusesHighNotorietyPlayer() {
        int dayOfWeek = KaraokeSystem.FRIDAY_INDEX;
        float hour = 20.0f;

        // Set notoriety to 55 (above threshold of 50)
        notorietySystem.addNotoriety(55, null);
        assertTrue(notorietySystem.getNotoriety() >= KaraokeSystem.BEV_REFUSE_NOTORIETY,
                "Notoriety should be at or above Bev's refusal threshold");

        KaraokeSystem.QueueResult result =
                karaoke.tryQueueForStage(notorietySystem.getNotoriety(), hour, dayOfWeek);

        assertEquals(KaraokeSystem.QueueResult.REFUSED_HIGH_NOTORIETY, result,
                "Bev should refuse player with Notoriety ≥ 50");

        NPC bev = karaoke.getBev();
        assertNotNull(bev, "Bev should be spawned");
        String speech = bev.getSpeechText();
        assertNotNull(speech, "Bev should have speech text");
        assertTrue(speech.toLowerCase().contains("not touching my mic"),
                "Bev's speech should contain 'not touching my mic' but was: " + speech);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Fuse box sabotage — unwitnessed
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player holds SCREWDRIVER. No NPCs within 4 blocks.
     * Player interacts with FUSE_BOX_PROP.
     * Verify:
     * - KaraokeSystem.isPaActive() == false
     * - NotorietySystem unchanged (unwitnessed)
     */
    @Test
    void fuseBoxSabotage_unwitnessed_paCutNoNotoriety() {
        // Give player a screwdriver
        inventory.addItem(Material.SCREWDRIVER, 1);

        // All NPCs are 100 blocks away (far outside witness radius of 4)
        List<NPC> farNpcs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            farNpcs.add(new NPC(NPCType.PUBLIC, 100f + i, 0f, 100f + i));
        }

        // PA starts active
        assertTrue(karaoke.isPaActive(), "PA should be active initially");

        int notorietyBefore = notorietySystem.getNotoriety();
        float playerX = 5f, playerZ = 5f;

        KaraokeSystem.SabotageResult result =
                karaoke.interactFuseBox(inventory, farNpcs, playerX, playerZ);

        assertEquals(KaraokeSystem.SabotageResult.CUT_UNWITNESSED, result,
                "Sabotage should be unwitnessed with NPCs 100 blocks away");
        assertFalse(karaoke.isPaActive(),
                "PA should be cut after fuse box sabotage");
        assertEquals(notorietyBefore, notorietySystem.getNotoriety(),
                "Notoriety should be unchanged for unwitnessed sabotage");

        // CRIMINAL_DAMAGE should be recorded regardless
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.CRIMINAL_DAMAGE) > 0,
                "CRIMINAL_DAMAGE should be recorded in CriminalRecord");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Karaoke inactive outside Friday night
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Set to Saturday 00:01 (dayOfWeek = 5, hour = 0.017).
     * Player presses E on KARAOKE_BOOTH_PROP.
     * Verify:
     * - KaraokeSystem.isKaraokeActive() == false
     * - tryQueueForStage returns KARAOKE_INACTIVE
     * - Bev's speech text contains "not on tonight, love"
     */
    @Test
    void karaokeInactiveOutsideFridayNight() {
        int saturdayIndex = 5; // Saturday (0=Monday)
        float hour = 0.017f;  // 00:01

        // Verify inactive on Saturday 00:01
        assertFalse(karaoke.isKaraokeActive(hour, saturdayIndex),
                "Karaoke should NOT be active on Saturday 00:01");

        KaraokeSystem.QueueResult result =
                karaoke.tryQueueForStage(notorietySystem.getNotoriety(), hour, saturdayIndex);

        assertEquals(KaraokeSystem.QueueResult.KARAOKE_INACTIVE, result,
                "Queue attempt should return KARAOKE_INACTIVE outside Friday night");

        NPC bev = karaoke.getBev();
        assertNotNull(bev, "Bev should exist");
        String speech = bev.getSpeechText();
        assertNotNull(speech, "Bev should have speech text");
        assertTrue(speech.toLowerCase().contains("not on tonight"),
                "Bev's speech should contain 'not on tonight' but was: " + speech);

        // Also verify Thursday 20:00 is inactive
        int thursdayIndex = 3;
        assertFalse(karaoke.isKaraokeActive(20.0f, thursdayIndex),
                "Karaoke should NOT be active on Thursday 20:00");

        // And Friday 20:00 IS active
        assertTrue(karaoke.isKaraokeActive(20.0f, KaraokeSystem.FRIDAY_INDEX),
                "Karaoke SHOULD be active on Friday 20:00");

        // And Friday 23:00 is NOT active (boundary)
        assertFalse(karaoke.isKaraokeActive(23.0f, KaraokeSystem.FRIDAY_INDEX),
                "Karaoke should NOT be active at Friday 23:00 (exclusive end)");
    }
}

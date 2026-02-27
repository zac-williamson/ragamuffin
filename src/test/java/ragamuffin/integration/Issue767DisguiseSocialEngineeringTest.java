package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.DisguiseSystem;
import ragamuffin.core.DisguiseSystem.BluffResult;
import ragamuffin.core.DisguiseSystem.CoverStatus;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #767 — Disguise &amp; Social Engineering System.
 *
 * <p>10 exact scenarios:
 * <ol>
 *   <li>Loot clothing from knocked-out NPC</li>
 *   <li>Equip disguise and verify faction access</li>
 *   <li>Cover decays under scrutiny</li>
 *   <li>Scrutiny survival: stand still → cover survives</li>
 *   <li>Scrutiny failure: run during scrutiny → cover immediately blown</li>
 *   <li>Crime penalty: commit crime while disguised → cover decays</li>
 *   <li>Heist integration: police uniform delays alarm response</li>
 *   <li>Greggs apron absurdity: transparent within 3 blocks</li>
 *   <li>Achievement trigger: UNDERCOVER unlocked on successful bluff</li>
 *   <li>Full lifecycle stress test: loot → equip → infiltrate → complete without scrutiny → INCOGNITO</li>
 * </ol>
 */
class Issue767DisguiseSocialEngineeringTest {

    private DisguiseSystem disguiseSystem;
    private Inventory inventory;
    private Player player;
    private AchievementSystem achievementSystem;
    private List<NPC> emptyNpcs;

    @BeforeEach
    void setUp() {
        disguiseSystem = new DisguiseSystem(new Random(42L));
        inventory = new Inventory(36);
        player = new Player(10f, 1f, 10f);
        achievementSystem = new AchievementSystem();
        disguiseSystem.setAchievementSystem(achievementSystem);
        emptyNpcs = new ArrayList<>();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 1: Loot clothing from knocked-out NPC
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: A knocked-out POLICE NPC yields a POLICE_UNIFORM when looted.
     * A knocked-out COUNCIL_BUILDER yields a COUNCIL_JACKET.
     * A knocked-out STREET_LAD yields a STREET_LADS_HOODIE.
     */
    @Test
    void lootClothingFromKnockedOutNPC() {
        // Create a knocked-out police NPC (not alive)
        NPC policeNpc = new NPC(NPCType.POLICE, 10f, 1f, 11f);
        policeNpc.setState(NPCState.KNOCKED_OUT);
        policeNpc.takeDamage(policeNpc.getHealth()); // kill/knock out

        // Loot it
        Material looted = disguiseSystem.lootDisguise(policeNpc, inventory);

        assertEquals(Material.POLICE_UNIFORM, looted,
            "Looting a knocked-out POLICE NPC should yield POLICE_UNIFORM");
        assertEquals(1, inventory.getItemCount(Material.POLICE_UNIFORM),
            "Inventory should contain exactly 1 POLICE_UNIFORM after looting");

        // Council builder yields COUNCIL_JACKET
        NPC builderNpc = new NPC(NPCType.COUNCIL_BUILDER, 10f, 1f, 12f);
        builderNpc.takeDamage(builderNpc.getHealth());
        Inventory inv2 = new Inventory(36);
        Material looted2 = disguiseSystem.lootDisguise(builderNpc, inv2);
        assertEquals(Material.COUNCIL_JACKET, looted2,
            "Looting a knocked-out COUNCIL_BUILDER NPC should yield COUNCIL_JACKET");

        // Street Lad yields STREET_LADS_HOODIE
        NPC streetLad = new NPC(NPCType.STREET_LAD, 10f, 1f, 13f);
        streetLad.takeDamage(streetLad.getHealth());
        Inventory inv3 = new Inventory(36);
        Material looted3 = disguiseSystem.lootDisguise(streetLad, inv3);
        assertEquals(Material.STREET_LADS_HOODIE, looted3,
            "Looting a knocked-out STREET_LAD NPC should yield STREET_LADS_HOODIE");

        // Living NPC yields nothing
        NPC aliveNpc = new NPC(NPCType.POLICE, 10f, 1f, 14f);
        Inventory inv4 = new Inventory(36);
        Material lootedFromAlive = disguiseSystem.lootDisguise(aliveNpc, inv4);
        assertNull(lootedFromAlive, "Looting a living NPC should return null");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 2: Equip disguise and verify faction access
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Equipping a POLICE_UNIFORM grants police access.
     * Equipping a COUNCIL_JACKET grants council access.
     * Equipping a MARCHETTI_TRACKSUIT grants Marchetti access.
     * Equipping a STREET_LADS_HOODIE grants Street Lads access.
     * Cover starts at 100 and HUD shows GREEN.
     */
    @Test
    void equipDisguiseGrantsFactionAccess() {
        // Equip POLICE_UNIFORM
        inventory.addItem(Material.POLICE_UNIFORM, 1);
        boolean equipped = disguiseSystem.equipDisguise(Material.POLICE_UNIFORM, inventory);

        assertTrue(equipped, "Should successfully equip POLICE_UNIFORM from inventory");
        assertTrue(disguiseSystem.isDisguised(), "Player should be disguised");
        assertTrue(disguiseSystem.hasPoliceAccess(), "POLICE_UNIFORM should grant police access");
        assertFalse(disguiseSystem.hasCouncilAccess(), "POLICE_UNIFORM should NOT grant council access");
        assertEquals(DisguiseSystem.MAX_COVER_INTEGRITY, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Cover integrity should start at 100");
        assertEquals(CoverStatus.GREEN, disguiseSystem.getCoverStatus(),
            "HUD should show GREEN when integrity is 100");
        assertEquals(Material.POLICE_UNIFORM, disguiseSystem.getActiveDisguise(),
            "Active disguise should be POLICE_UNIFORM");

        // Switch to COUNCIL_JACKET
        disguiseSystem.removeDisguise();
        inventory.addItem(Material.COUNCIL_JACKET, 1);
        disguiseSystem.equipDisguise(Material.COUNCIL_JACKET, inventory);
        assertTrue(disguiseSystem.hasCouncilAccess(), "COUNCIL_JACKET should grant council access");
        assertFalse(disguiseSystem.hasPoliceAccess(), "COUNCIL_JACKET should NOT grant police access");

        // Switch to MARCHETTI_TRACKSUIT
        disguiseSystem.removeDisguise();
        inventory.addItem(Material.MARCHETTI_TRACKSUIT, 1);
        disguiseSystem.equipDisguise(Material.MARCHETTI_TRACKSUIT, inventory);
        assertTrue(disguiseSystem.hasMarchettiAccess(), "MARCHETTI_TRACKSUIT should grant Marchetti access");

        // Switch to STREET_LADS_HOODIE
        disguiseSystem.removeDisguise();
        inventory.addItem(Material.STREET_LADS_HOODIE, 1);
        disguiseSystem.equipDisguise(Material.STREET_LADS_HOODIE, inventory);
        assertTrue(disguiseSystem.hasStreetLadsAccess(), "STREET_LADS_HOODIE should grant Street Lads access");

        // Cannot equip without inventory item
        disguiseSystem.removeDisguise();
        Inventory emptyInv = new Inventory(36);
        boolean failedEquip = disguiseSystem.equipDisguise(Material.HI_VIS_VEST, emptyInv);
        assertFalse(failedEquip, "Should not equip disguise not in inventory");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 3: Cover decays under scrutiny
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: When a suspicious NPC enters scrutiny range while the player is
     * wearing a POLICE_UNIFORM, the NPC transitions to SCRUTINISING state. Cover
     * integrity decays at 5 per second while under scrutiny. After 20 seconds of
     * sustained scrutiny, integrity drops to 0 and cover is blown.
     */
    @Test
    void coverDecaysUnderScrutiny() {
        // Equip police uniform
        inventory.addItem(Material.POLICE_UNIFORM, 1);
        disguiseSystem.equipDisguise(Material.POLICE_UNIFORM, inventory);

        // Place a council member within scrutiny range — they scrutinise police uniforms
        NPC councilMember = new NPC(NPCType.COUNCIL_MEMBER, 10f, 1f, 14f); // 4 blocks away
        List<NPC> npcs = new ArrayList<>();
        npcs.add(councilMember);

        // First update — NPC should enter SCRUTINISING
        disguiseSystem.update(0.1f, player, npcs, 0f);
        assertEquals(NPCState.SCRUTINISING, councilMember.getState(),
            "Council member should enter SCRUTINISING when player wears POLICE_UNIFORM nearby");

        // Integrity should now be decaying at SCRUTINY_DECAY_RATE per second
        float initialIntegrity = disguiseSystem.getCoverIntegrity();
        // Simulate 10 seconds of scrutiny (with player standing still, speed=0)
        for (int i = 0; i < 100; i++) {
            disguiseSystem.update(0.1f, player, npcs, 0f);
        }
        // After 10 seconds: 100 - (5 * 10) = 50
        assertTrue(disguiseSystem.getCoverIntegrity() < initialIntegrity,
            "Cover integrity should have decayed during scrutiny");
        assertTrue(disguiseSystem.getCoverIntegrity() <= 55f,
            "Cover integrity should have decayed significantly (expected ~50 after 10s)");

        // HUD should now show AMBER
        assertEquals(CoverStatus.AMBER, disguiseSystem.getCoverStatus(),
            "HUD should show AMBER when integrity is around 50");

        // Simulate another 10+ seconds to fully blow cover
        for (int i = 0; i < 110; i++) {
            if (disguiseSystem.isCoverBlown()) break;
            disguiseSystem.update(0.1f, player, npcs, 0f);
        }
        assertTrue(disguiseSystem.isCoverBlown(),
            "Cover should be blown after sustained scrutiny depletes integrity to 0");
        assertEquals(0f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Cover integrity should be exactly 0 when blown");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 4: Scrutiny survival — stand still → cover survives
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: When a suspicious NPC begins scrutinising (3-second window),
     * if the player stands still (speed=0) for the full 3 seconds, the NPC
     * returns to WANDERING state and cover integrity is preserved (no full decay).
     */
    @Test
    void scrutinySurvival_standStill() {
        // Equip MARCHETTI_TRACKSUIT — police scrutinise this
        inventory.addItem(Material.MARCHETTI_TRACKSUIT, 1);
        disguiseSystem.equipDisguise(Material.MARCHETTI_TRACKSUIT, inventory);

        NPC policeNpc = new NPC(NPCType.POLICE, 10f, 1f, 14f); // 4 blocks away
        List<NPC> npcs = new ArrayList<>();
        npcs.add(policeNpc);

        // Trigger scrutiny
        disguiseSystem.update(0.1f, player, npcs, 0f);
        assertEquals(NPCState.SCRUTINISING, policeNpc.getState(),
            "Police should scrutinise a Marchetti tracksuit");

        // Simulate player standing still (speed=0) for exactly 3 seconds
        for (int i = 0; i < 30; i++) {
            disguiseSystem.update(0.1f, player, npcs, 0f);
        }

        // NPC should return to WANDERING after scrutiny duration expires
        assertEquals(NPCState.WANDERING, policeNpc.getState(),
            "NPC should return to WANDERING after scrutiny timer expires without player running");

        // Cover should not be fully blown — some decay may have occurred but not 100%
        assertFalse(disguiseSystem.isCoverBlown(),
            "Cover should NOT be blown when player stands still during scrutiny");
        assertTrue(disguiseSystem.getCoverIntegrity() > 0f,
            "Cover integrity should remain > 0 after standing still through scrutiny");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 5: Scrutiny failure — run during scrutiny → cover immediately blown
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: If the player runs (speed >= 2.5 blocks/sec) while being
     * scrutinised, cover is immediately blown and the NPC transitions to AGGRESSIVE.
     */
    @Test
    void scrutinyFailure_runDuringScrutiny_blowsCover() {
        // Equip STREET_LADS_HOODIE — police scrutinise this
        inventory.addItem(Material.STREET_LADS_HOODIE, 1);
        disguiseSystem.equipDisguise(Material.STREET_LADS_HOODIE, inventory);

        NPC policeNpc = new NPC(NPCType.POLICE, 10f, 1f, 14f); // 4 blocks away
        List<NPC> npcs = new ArrayList<>();
        npcs.add(policeNpc);

        // Trigger scrutiny (player standing still initially)
        disguiseSystem.update(0.1f, player, npcs, 0f);
        assertEquals(NPCState.SCRUTINISING, policeNpc.getState(),
            "Police should begin scrutinising the Street Lads hoodie");
        assertFalse(disguiseSystem.isCoverBlown(), "Cover should not be blown immediately");

        // Now player starts running (speed = 3.5 blocks/sec > RUN_SPEED_THRESHOLD)
        disguiseSystem.update(0.1f, player, npcs, 3.5f);

        // Cover should be immediately blown
        assertTrue(disguiseSystem.isCoverBlown(),
            "Cover should be immediately blown when player runs during scrutiny");
        assertEquals(NPCState.AGGRESSIVE, policeNpc.getState(),
            "Scrutinising NPC should transition to AGGRESSIVE when cover is blown by running");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 6: Crime penalty — commit crime while disguised → cover decays
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6: Calling notifyCrime() while disguised applies a -20 penalty to
     * cover integrity. Three crimes blow cover completely (100 - 3×20 = 40... but
     * the 5th crime from 40 drops to 20, and eventually hits 0). After enough crimes,
     * cover is blown. A single crime in the middle range does not immediately blow cover.
     */
    @Test
    void crimePenalty_coverDecaysOnCrime() {
        inventory.addItem(Material.POLICE_UNIFORM, 1);
        disguiseSystem.equipDisguise(Material.POLICE_UNIFORM, inventory);

        assertEquals(100f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Cover should start at 100");

        // First crime: integrity drops to 80
        disguiseSystem.notifyCrime(player, emptyNpcs);
        assertEquals(80f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "First crime: integrity should drop to 80");
        assertFalse(disguiseSystem.isCoverBlown(), "Cover should not be blown after first crime");

        // Second crime: integrity drops to 60
        disguiseSystem.notifyCrime(player, emptyNpcs);
        assertEquals(60f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Second crime: integrity should drop to 60");

        // Third crime: 40
        disguiseSystem.notifyCrime(player, emptyNpcs);
        assertEquals(40f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Third crime: integrity should drop to 40");

        // HUD should be AMBER now
        assertEquals(CoverStatus.AMBER, disguiseSystem.getCoverStatus(),
            "Cover status should be AMBER at 40 integrity");

        // Fourth crime: 20 — RED zone
        disguiseSystem.notifyCrime(player, emptyNpcs);
        assertEquals(20f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Fourth crime: integrity should drop to 20");
        assertEquals(CoverStatus.RED, disguiseSystem.getCoverStatus(),
            "Cover status should be RED at 20 integrity (below 25)");

        // Fifth crime: 0 — cover blown
        disguiseSystem.notifyCrime(player, emptyNpcs);
        assertTrue(disguiseSystem.isCoverBlown(),
            "Fifth crime should blow cover (integrity reaches 0)");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 7: Heist integration — police uniform delays alarm response
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 7: When the player wears a POLICE_UNIFORM (cover intact),
     * {@link DisguiseSystem#isWearingPoliceUniform()} returns true and
     * {@link DisguiseSystem#POLICE_DISGUISE_ALARM_DELAY} is 30 seconds.
     * After cover is blown, isWearingPoliceUniform() returns false.
     */
    @Test
    void heistIntegration_policeUniformDelaysAlarmResponse() {
        inventory.addItem(Material.POLICE_UNIFORM, 1);
        disguiseSystem.equipDisguise(Material.POLICE_UNIFORM, inventory);

        assertTrue(disguiseSystem.isWearingPoliceUniform(),
            "isWearingPoliceUniform() should return true when POLICE_UNIFORM is equipped");
        assertEquals(30f, DisguiseSystem.POLICE_DISGUISE_ALARM_DELAY, 0.01f,
            "POLICE_DISGUISE_ALARM_DELAY should be 30 seconds");

        // Blow cover
        disguiseSystem.blowCover("Test blow", player, emptyNpcs);
        assertFalse(disguiseSystem.isWearingPoliceUniform(),
            "isWearingPoliceUniform() should return false after cover is blown");

        // Equipping a different disguise should not give police access
        disguiseSystem.removeDisguise();
        inventory.addItem(Material.GREGGS_APRON, 1);
        DisguiseSystem freshSystem = new DisguiseSystem(new Random(1L));
        Inventory freshInv = new Inventory(36);
        freshInv.addItem(Material.GREGGS_APRON, 1);
        freshSystem.equipDisguise(Material.GREGGS_APRON, freshInv);
        assertFalse(freshSystem.isWearingPoliceUniform(),
            "GREGGS_APRON should not grant police access");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 8: Greggs apron absurdity — transparent within 3 blocks
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 8: The GREGGS_APRON is immediately transparent (cover blown) when
     * any NPC comes within 3 blocks. Beyond 3 blocks it works normally. The
     * OBVIOUS_IN_HINDSIGHT achievement is unlocked when this happens.
     */
    @Test
    void greggsApron_immediatelyTransparentWithin3Blocks() {
        inventory.addItem(Material.GREGGS_APRON, 1);
        DisguiseSystem system = new DisguiseSystem(new Random(99L));
        AchievementSystem ach = new AchievementSystem();
        system.setAchievementSystem(ach);
        system.equipDisguise(Material.GREGGS_APRON, inventory);

        assertTrue(system.isDisguised(), "Player should be disguised with Greggs Apron");

        // NPC at 5 blocks away — should NOT blow cover
        NPC farNpc = new NPC(NPCType.PUBLIC, 10f, 1f, 15f); // 5 blocks in Z
        List<NPC> npcs = new ArrayList<>();
        npcs.add(farNpc);
        system.update(0.1f, player, npcs, 0f);
        assertFalse(system.isCoverBlown(), "Greggs Apron should work fine at 5 blocks distance");

        // Move NPC within 3 blocks (2 blocks away)
        NPC closeNpc = new NPC(NPCType.PUBLIC, 10f, 1f, 11.5f); // 1.5 blocks in Z
        List<NPC> closeNpcs = new ArrayList<>();
        closeNpcs.add(closeNpc);
        system.update(0.1f, player, closeNpcs, 0f);

        assertTrue(system.isCoverBlown(),
            "GREGGS_APRON should be immediately transparent (cover blown) within 3 blocks");
        assertTrue(ach.isUnlocked(AchievementType.OBVIOUS_IN_HINDSIGHT),
            "OBVIOUS_IN_HINDSIGHT achievement should be unlocked when Greggs Apron fails up close");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 9: Achievement trigger — UNDERCOVER on successful bluff
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 9: Successfully calling {@link DisguiseSystem#attemptBluff} while
     * disguised unlocks the UNDERCOVER achievement. With a RUMOUR_NOTE prop, success
     * chance is 90%. Without it, 60%. A failed bluff blows cover and returns FAILED.
     */
    @Test
    void achievementTrigger_undercoverOnSuccessfulBluff() {
        // Use a seeded Random that gives success on first call
        // BLUFF_PROP_SUCCESS = 0.90 — seed 42 with nextFloat() < 0.9 should succeed
        DisguiseSystem system = new DisguiseSystem(new Random(42L) {
            @Override
            public float nextFloat() {
                return 0.5f; // 0.5 < 0.9 = success with prop; 0.5 < 0.6 = success without prop
            }
        });
        AchievementSystem ach = new AchievementSystem();
        system.setAchievementSystem(ach);

        Inventory inv = new Inventory(36);
        inv.addItem(Material.POLICE_UNIFORM, 1);
        inv.addItem(Material.RUMOUR_NOTE, 1);
        system.equipDisguise(Material.POLICE_UNIFORM, inv);

        // Bluff with RUMOUR_NOTE prop
        BluffResult result = system.attemptBluff(inv, player, emptyNpcs);
        assertEquals(BluffResult.SUCCESS, result,
            "Bluff should succeed when random value (0.5) is below 0.90 (prop success rate)");
        assertTrue(ach.isUnlocked(AchievementType.UNDERCOVER),
            "UNDERCOVER achievement should be unlocked on successful bluff");

        // Bluff with no disguise returns NO_DISGUISE
        DisguiseSystem noDisguise = new DisguiseSystem(new Random(1L));
        BluffResult noDisguiseResult = noDisguise.attemptBluff(inv, player, emptyNpcs);
        assertEquals(BluffResult.NO_DISGUISE, noDisguiseResult,
            "Bluff should return NO_DISGUISE when player is not wearing one");

        // Blown cover returns COVER_BLOWN
        DisguiseSystem blownSystem = new DisguiseSystem(new Random(1L));
        Inventory blownInv = new Inventory(36);
        blownInv.addItem(Material.COUNCIL_JACKET, 1);
        blownSystem.equipDisguise(Material.COUNCIL_JACKET, blownInv);
        blownSystem.blowCover("Test", player, emptyNpcs);
        BluffResult blownResult = blownSystem.attemptBluff(blownInv, player, emptyNpcs);
        assertEquals(BluffResult.COVER_BLOWN, blownResult,
            "Bluff should return COVER_BLOWN when cover is already blown");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 10: Full lifecycle stress test
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 10: Full lifecycle —
     * <ol>
     *   <li>Loot a POLICE_UNIFORM from knocked-out police NPC</li>
     *   <li>Equip it — cover starts at 100, status GREEN</li>
     *   <li>Update 5 seconds with no nearby NPCs — cover stays at 100</li>
     *   <li>Update with a distant NPC (outside range) — no scrutiny</li>
     *   <li>Complete infiltration successfully (notifyInfiltrationComplete with integrity > 50)</li>
     *   <li>INCOGNITO achievement unlocked (never scrutinised)</li>
     *   <li>METHOD_ACTOR achievement unlocked (integrity > 50 on complete)</li>
     *   <li>Disguise is removed after completion</li>
     * </ol>
     */
    @Test
    void fullLifecycleStressTest() {
        // Step 1: Loot police uniform from knocked-out NPC
        NPC knockedOutPolice = new NPC(NPCType.POLICE, 10f, 1f, 11f);
        knockedOutPolice.takeDamage(knockedOutPolice.getHealth()); // knock out
        Material looted = disguiseSystem.lootDisguise(knockedOutPolice, inventory);
        assertEquals(Material.POLICE_UNIFORM, looted,
            "Step 1: Should loot POLICE_UNIFORM from knocked-out police");
        assertEquals(1, inventory.getItemCount(Material.POLICE_UNIFORM),
            "Inventory should have 1 POLICE_UNIFORM");

        // Step 2: Equip — cover starts at 100
        boolean equipped = disguiseSystem.equipDisguise(Material.POLICE_UNIFORM, inventory);
        assertTrue(equipped, "Step 2: Should equip POLICE_UNIFORM");
        assertEquals(100f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Step 2: Cover should start at 100");
        assertEquals(CoverStatus.GREEN, disguiseSystem.getCoverStatus(),
            "Step 2: HUD should show GREEN");

        // Step 3: 5 seconds with no NPCs — cover stays at 100
        for (int i = 0; i < 50; i++) {
            disguiseSystem.update(0.1f, player, emptyNpcs, 0f);
        }
        assertEquals(100f, disguiseSystem.getCoverIntegrity(), 0.01f,
            "Step 3: Cover should remain at 100 with no nearby NPCs");
        assertFalse(disguiseSystem.isUnderScrutiny(),
            "Step 3: Player should not be under scrutiny with no NPCs nearby");

        // Step 4: NPC far away (20 blocks) — outside scrutiny range (6 blocks) — no scrutiny
        NPC farNpc = new NPC(NPCType.COUNCIL_MEMBER, 10f, 1f, 30f); // 20 blocks away
        List<NPC> distantNpcs = new ArrayList<>();
        distantNpcs.add(farNpc);
        disguiseSystem.update(0.1f, player, distantNpcs, 0f);
        assertNotEquals(NPCState.SCRUTINISING, farNpc.getState(),
            "Step 4: Distant NPC (20 blocks) should NOT scrutinise");
        assertFalse(disguiseSystem.isUnderScrutiny(),
            "Step 4: Player should not be under scrutiny from distant NPC");

        // Step 5: Complete infiltration with cover integrity > 50
        float currentIntegrity = disguiseSystem.getCoverIntegrity();
        assertTrue(currentIntegrity >= 50f,
            "Step 5: Integrity should be >= 50 before completion");
        disguiseSystem.notifyInfiltrationComplete(currentIntegrity, player, emptyNpcs);

        // Step 6 & 7: INCOGNITO and METHOD_ACTOR achievements should be unlocked
        assertTrue(achievementSystem.isUnlocked(AchievementType.INCOGNITO),
            "Step 6: INCOGNITO should be unlocked (never scrutinised during session)");
        assertTrue(achievementSystem.isUnlocked(AchievementType.METHOD_ACTOR),
            "Step 7: METHOD_ACTOR should be unlocked (integrity >= 50 on complete)");

        // Step 8: Disguise should be removed after completion
        assertNull(disguiseSystem.getActiveDisguise(),
            "Step 8: Disguise should be removed after notifyInfiltrationComplete");
        assertFalse(disguiseSystem.isDisguised(),
            "Step 8: Player should no longer be disguised after completion");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Additional: isDisguiseMaterial covers all 6 new materials
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void allSixNewMaterialsAreDisguises() {
        assertTrue(DisguiseSystem.isDisguiseMaterial(Material.POLICE_UNIFORM));
        assertTrue(DisguiseSystem.isDisguiseMaterial(Material.COUNCIL_JACKET));
        assertTrue(DisguiseSystem.isDisguiseMaterial(Material.MARCHETTI_TRACKSUIT));
        assertTrue(DisguiseSystem.isDisguiseMaterial(Material.STREET_LADS_HOODIE));
        assertTrue(DisguiseSystem.isDisguiseMaterial(Material.HI_VIS_VEST));
        assertTrue(DisguiseSystem.isDisguiseMaterial(Material.GREGGS_APRON));

        // Non-disguise materials should return false
        assertFalse(DisguiseSystem.isDisguiseMaterial(Material.WOOD));
        assertFalse(DisguiseSystem.isDisguiseMaterial(Material.BALACLAVA));
        assertFalse(DisguiseSystem.isDisguiseMaterial(Material.POLICE_UNIFORM.equals(Material.COIN)
            ? Material.COIN : Material.COIN));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Additional: 5 new achievements are correctly defined
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void fiveNewAchievementsExist() {
        // All 5 new achievements should be instant (target = 1)
        assertTrue(AchievementType.UNDERCOVER.isInstant(), "UNDERCOVER should be instant");
        assertTrue(AchievementType.METHOD_ACTOR.isInstant(), "METHOD_ACTOR should be instant");
        assertTrue(AchievementType.TURNCOAT.isInstant(), "TURNCOAT should be instant");
        assertTrue(AchievementType.OBVIOUS_IN_HINDSIGHT.isInstant(), "OBVIOUS_IN_HINDSIGHT should be instant");
        assertTrue(AchievementType.INCOGNITO.isInstant(), "INCOGNITO should be instant");

        // Verify they're not initially unlocked
        assertFalse(achievementSystem.isUnlocked(AchievementType.UNDERCOVER));
        assertFalse(achievementSystem.isUnlocked(AchievementType.METHOD_ACTOR));
        assertFalse(achievementSystem.isUnlocked(AchievementType.TURNCOAT));
        assertFalse(achievementSystem.isUnlocked(AchievementType.OBVIOUS_IN_HINDSIGHT));
        assertFalse(achievementSystem.isUnlocked(AchievementType.INCOGNITO));
    }
}

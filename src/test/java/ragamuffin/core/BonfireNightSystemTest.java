package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

import ragamuffin.core.BonfireNightSystem.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BonfireNightSystem} — Issue #1365: Northfield Bonfire Night.
 *
 * <p>Tests:
 * <ol>
 *   <li>Event day detection: day 308, nearest Saturday, wrong day.</li>
 *   <li>THUNDERSTORM cancels event; clean day opens event.</li>
 *   <li>GUY_PROP placement: wrong time, missing ingredients, wrong location, success.</li>
 *   <li>Donation tick: coin added to inventory, PENNY_FOR_THE_GUY achievement.</li>
 *   <li>YOUTH_GANG kick: PARTY_POOPER achievement, guyDestroyed state.</li>
 *   <li>Darren buy: success, wrong time, hostile, insufficient funds.</li>
 *   <li>Holdall steal: stealth too low, success seeds rumour, Darren hostile.</li>
 *   <li>Firework launch: success, misfire, wrong time, missing item.</li>
 *   <li>Firework police sightings: first is warning, second records offence.</li>
 *   <li>PYRO_NIGHT achievement: 3 launches no offence → awarded; 2 launches → not awarded.</li>
 *   <li>Mortar Option A (early launch): Notoriety +5, SABOTEUR achievement.</li>
 *   <li>Mortar Option B (banger plant): schedules catastrophe at 20:00.</li>
 *   <li>Banger catastrophe fires at 20:00: CRIMINAL_DAMAGE recorded, notoriety +8, rumour seeded.</li>
 *   <li>Warden detection radius: full before 21:00, halved after.</li>
 *   <li>Bonfire spawn/embers events fire at correct hours.</li>
 *   <li>NPC spawning: event NPCs and Darren.</li>
 *   <li>New enum values exist: BONFIRE_WARDEN, EVENT_COMPERE, FIREWORK_THEFT.</li>
 * </ol>
 */
class BonfireNightSystemTest {

    private BonfireNightSystem system;
    private Inventory inventory;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private StreetSkillSystem streetSkillSystem;

    @BeforeEach
    void setUp() {
        system = new BonfireNightSystem(new Random(42L));
        inventory = new Inventory(36);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem();
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(7L));
        streetSkillSystem = new StreetSkillSystem(new Random(13L));

        system.setNotorietySystem(notorietySystem);
        system.setWantedSystem(wantedSystem);
        system.setCriminalRecord(criminalRecord);
        system.setRumourNetwork(rumourNetwork);
        system.setStreetSkillSystem(streetSkillSystem);
    }

    // ── 1. Event day detection ────────────────────────────────────────────────

    @Test
    void isBonfireNight_day308_returnsTrue() {
        assertTrue(system.isBonfireNight(308, 1)); // any day-of-week, exact match
    }

    @Test
    void isBonfireNight_nearestSaturday_within3Days_returnsTrue() {
        // Saturday = dayOfWeek 5
        // day 311 = 3 days after 308
        assertTrue(system.isBonfireNight(311, 5));
    }

    @Test
    void isBonfireNight_saturday4DaysAway_returnsFalse() {
        // 4 days away exceeds ±3
        assertFalse(system.isBonfireNight(312, 5));
    }

    @Test
    void isBonfireNight_wrongDayNotSaturday_returnsFalse() {
        assertFalse(system.isBonfireNight(305, 2)); // not day 308, not Saturday
    }

    // ── 2. Event lifecycle ────────────────────────────────────────────────────

    @Test
    void openEvent_thunderstorm_cancelsEvent() {
        EventType result = system.openEvent(true);
        assertEquals(EventType.THUNDERSTORM_CANCELS, result);
        assertFalse(system.isEventActive());
    }

    @Test
    void openEvent_goodWeather_activatesEvent() {
        EventType result = system.openEvent(false);
        assertNull(result);
        assertTrue(system.isEventActive());
    }

    @Test
    void closeEvent_deactivates() {
        system.openEvent(false);
        EventType result = system.closeEvent();
        assertEquals(EventType.EVENT_CLOSED, result);
        assertFalse(system.isEventActive());
    }

    // ── 3. GUY_PROP placement ─────────────────────────────────────────────────

    @Test
    void placeGuy_eventNotActive_returnsEventNotActive() {
        assertEquals(GuyPlacementResult.EVENT_NOT_ACTIVE,
                system.placeGuy(inventory, 18.0f, true));
    }

    @Test
    void placeGuy_wrongTime_returnsWrongTime() {
        system.openEvent(false);
        // 20:00 is outside the 17:00–19:30 window
        assertEquals(GuyPlacementResult.WRONG_TIME,
                system.placeGuy(inventory, 20.0f, true));
    }

    @Test
    void placeGuy_wrongLocation_returnsWrongLocation() {
        system.openEvent(false);
        assertEquals(GuyPlacementResult.WRONG_LOCATION,
                system.placeGuy(inventory, 18.0f, false));
    }

    @Test
    void placeGuy_missingIngredients_returnsMissingIngredients() {
        system.openEvent(false);
        // No items in inventory
        assertEquals(GuyPlacementResult.MISSING_INGREDIENTS,
                system.placeGuy(inventory, 18.0f, true));
    }

    @Test
    void placeGuy_allIngredients_inPark_correctTime_returnsPlaced() {
        system.openEvent(false);
        inventory.addItem(Material.NEWSPAPER, 1);
        inventory.addItem(Material.OLD_CLOTHES, 1);
        inventory.addItem(Material.WOOLLY_HAT, 1);

        GuyPlacementResult result = system.placeGuy(inventory, 18.0f, true);
        assertEquals(GuyPlacementResult.PLACED, result);
        assertTrue(system.isGuyPlaced());
        // Ingredients consumed
        assertFalse(inventory.hasItem(Material.NEWSPAPER));
        assertFalse(inventory.hasItem(Material.OLD_CLOTHES));
        assertFalse(inventory.hasItem(Material.WOOLLY_HAT));
    }

    // ── 4. Donation tick ──────────────────────────────────────────────────────

    @Test
    void update_guyPlaced_donationTickAddsCoin() {
        system.openEvent(false);
        system.setBonfireSpawnedForTesting(true);
        system.setGuyPlacedForTesting(true);

        int before = inventory.getItemCount(Material.COIN);
        // Advance 2.0 minutes — should trigger one donation
        system.update(2.0f, 19.0f, inventory, null,
                type -> achievements.unlock(type));

        assertEquals(before + BonfireNightSystem.GUY_DONATION_AMOUNT,
                inventory.getItemCount(Material.COIN));
    }

    @Test
    void update_guyPlaced_pennyForTheGuyAchievementAwarded() {
        system.openEvent(false);
        system.setBonfireSpawnedForTesting(true);
        system.setGuyPlacedForTesting(true);

        assertFalse(achievements.isUnlocked(AchievementType.PENNY_FOR_THE_GUY));
        system.update(2.0f, 19.0f, inventory, null,
                type -> achievements.unlock(type));
        assertTrue(achievements.isUnlocked(AchievementType.PENNY_FOR_THE_GUY));
    }

    // ── 5. YOUTH_GANG kick ────────────────────────────────────────────────────

    @Test
    void update_youthGangKick_partyPoooperAchievementAndGuyDestroyed() {
        // Use a seeded RNG that always triggers the kick (0.20f chance per minute)
        BonfireNightSystem alwaysKick = new BonfireNightSystem(new Random(0L) {
            @Override
            public float nextFloat() { return 0.01f; } // always < 0.20f
        });
        alwaysKick.setNotorietySystem(notorietySystem);
        alwaysKick.openEvent(false);
        alwaysKick.setBonfireSpawnedForTesting(true);
        alwaysKick.setGuyPlacedForTesting(true);

        boolean[] partyPoooperFired = {false};
        // Give enough time for the kick check (1.0f minutes per check)
        EventType result = alwaysKick.update(1.0f, 19.0f, inventory, null,
                type -> { if (type == AchievementType.PARTY_POOPER) partyPoooperFired[0] = true; });

        assertEquals(EventType.GUY_KICKED_OVER, result);
        assertTrue(alwaysKick.isGuyDestroyed());
        assertTrue(partyPoooperFired[0]);
    }

    // ── 6. Darren buy ─────────────────────────────────────────────────────────

    @Test
    void buyFromDarren_eventNotActive_returnsEventNotActive() {
        assertEquals(DarrenBuyResult.EVENT_NOT_ACTIVE,
                system.buyFromDarren(Material.ROCKET_FIREWORK, 4, inventory, 18.0f));
    }

    @Test
    void buyFromDarren_tooEarly_darrenNotPresent() {
        system.openEvent(false);
        // Before 17:00
        assertEquals(DarrenBuyResult.DARREN_NOT_PRESENT,
                system.buyFromDarren(Material.ROCKET_FIREWORK, 4, inventory, 16.0f));
    }

    @Test
    void buyFromDarren_hostile_returnsDarrenHostile() {
        system.openEvent(false);
        system.setDarrenHostileForTesting(true);
        assertEquals(DarrenBuyResult.DARREN_HOSTILE,
                system.buyFromDarren(Material.ROCKET_FIREWORK, 4, inventory, 18.0f));
    }

    @Test
    void buyFromDarren_insufficientFunds() {
        system.openEvent(false);
        // No COIN in inventory
        assertEquals(DarrenBuyResult.INSUFFICIENT_FUNDS,
                system.buyFromDarren(Material.ROCKET_FIREWORK, 4, inventory, 18.0f));
    }

    @Test
    void buyFromDarren_success_addsFIreworkToInventory() {
        system.openEvent(false);
        inventory.addItem(Material.COIN, 10);

        DarrenBuyResult result =
                system.buyFromDarren(Material.ROCKET_FIREWORK, 4, inventory, 18.0f);

        assertEquals(DarrenBuyResult.PURCHASED, result);
        assertTrue(inventory.hasItem(Material.ROCKET_FIREWORK));
        assertEquals(6, inventory.getItemCount(Material.COIN)); // 10 - 4
    }

    // ── 7. Holdall steal ──────────────────────────────────────────────────────

    @Test
    void stealHoldall_stealthTooLow_returnsStealthTooLow() {
        system.openEvent(false);
        // streetSkillSystem starts at NOVICE (level 0) for STEALTH
        assertEquals(HoldallStealResult.STEALTH_TOO_LOW,
                system.stealHoldall(inventory, 18.0f, null, null));
    }

    @Test
    void stealHoldall_alreadyHostile_returnsDarrenHostile() {
        system.openEvent(false);
        system.setDarrenHostileForTesting(true);
        assertEquals(HoldallStealResult.DARREN_HOSTILE,
                system.stealHoldall(inventory, 18.0f, null, null));
    }

    @Test
    void stealHoldall_darrenNotPresent_returnsNotPresent() {
        system.openEvent(false);
        // Before 17:00 spawn time
        assertEquals(HoldallStealResult.DARREN_NOT_PRESENT,
                system.stealHoldall(inventory, 16.0f, null, null));
    }

    @Test
    void stealHoldall_sufficientStealth_addsCoinAndSeedsRumour() {
        system.openEvent(false);
        // Wire a skill system that reports STEALTH tier 2
        StreetSkillSystem highStealth = new StreetSkillSystem(new Random(0L)) {
            @Override
            public int getTierLevel(Skill skill) {
                return skill == Skill.STEALTH ? 2 : 0;
            }
        };
        system.setStreetSkillSystem(highStealth);

        NPC witness = new NPC(NPCType.PUBLIC, 0f, 0f, 0f);
        HoldallStealResult result =
                system.stealHoldall(inventory, 18.0f, witness, null);

        assertEquals(HoldallStealResult.STOLEN, result);
        assertTrue(system.isHoldallStolen());
        assertTrue(system.isDarrenHostile());
        assertEquals(BonfireNightSystem.HOLDALL_FENCE_VALUE,
                inventory.getItemCount(Material.COIN));
        // Rumour seeded
        assertFalse(rumourNetwork.getAllRumours().isEmpty());
        assertTrue(rumourNetwork.getAllRumours().stream()
                .anyMatch(r -> r.getType() == RumourType.FIREWORK_THEFT));
    }

    // ── 8. Firework launch ────────────────────────────────────────────────────

    @Test
    void launchFirework_eventNotActive_returnsEventNotActive() {
        assertEquals(FireworkLaunchResult.EVENT_NOT_ACTIVE,
                system.launchFirework(Material.ROMAN_CANDLE, inventory, 19.0f, false));
    }

    @Test
    void launchFirework_wrongTime_returnsWrongTime() {
        system.openEvent(false);
        // 16:00 is before event window 18:00
        assertEquals(FireworkLaunchResult.WRONG_TIME,
                system.launchFirework(Material.ROMAN_CANDLE, inventory, 16.0f, false));
    }

    @Test
    void launchFirework_notInInventory_returnsNotInInventory() {
        system.openEvent(false);
        assertEquals(FireworkLaunchResult.NOT_IN_INVENTORY,
                system.launchFirework(Material.ROMAN_CANDLE, inventory, 19.0f, false));
    }

    @Test
    void launchFirework_success_incrementsCount_consumesItem() {
        // Use a system that never misfires
        BonfireNightSystem noMisfire = new BonfireNightSystem(new Random(0L) {
            @Override
            public float nextFloat() { return 0.99f; } // always > misfire chance
        });
        noMisfire.openEvent(false);
        inventory.addItem(Material.ROMAN_CANDLE, 1);

        FireworkLaunchResult result =
                noMisfire.launchFirework(Material.ROMAN_CANDLE, inventory, 19.0f, false);

        assertEquals(FireworkLaunchResult.LAUNCHED, result);
        assertEquals(1, noMisfire.getFireworksLaunched());
        assertFalse(inventory.hasItem(Material.ROMAN_CANDLE));
    }

    @Test
    void launchFirework_misfire_doesNotIncrementCount() {
        // Use a system that always misfires
        BonfireNightSystem alwaysMisfire = new BonfireNightSystem(new Random(0L) {
            @Override
            public float nextFloat() { return 0.0f; } // always < misfire chance
        });
        alwaysMisfire.openEvent(false);
        inventory.addItem(Material.ROMAN_CANDLE, 1);

        FireworkLaunchResult result =
                alwaysMisfire.launchFirework(Material.ROMAN_CANDLE, inventory, 19.0f, false);

        assertEquals(FireworkLaunchResult.MISFIRE, result);
        assertEquals(0, alwaysMisfire.getFireworksLaunched());
    }

    @Test
    void getMisfireChance_rain_doublesChance() {
        float dry = system.getMisfireChance(Material.ROCKET_FIREWORK, false);
        float rainy = system.getMisfireChance(Material.ROCKET_FIREWORK, true);
        assertEquals(dry * BonfireNightSystem.MISFIRE_RAIN_MULTIPLIER, rainy, 0.001f);
    }

    // ── 9. Police sightings ───────────────────────────────────────────────────

    @Test
    void fireworkPoliceSighting_firstSighting_warningOnly() {
        system.openEvent(false);
        boolean offence = system.recordFireworkPoliceSighting(0f, 0f, 0f, null);
        assertFalse(offence);
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.FIREWORK_OFFENCE));
    }

    @Test
    void fireworkPoliceSighting_secondSighting_recordsOffence() {
        system.openEvent(false);
        system.recordFireworkPoliceSighting(0f, 0f, 0f, null);
        boolean offence = system.recordFireworkPoliceSighting(0f, 0f, 0f,
                type -> achievements.unlock(type));
        assertTrue(offence);
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.FIREWORK_OFFENCE) >= 1);
    }

    // ── 10. PYRO_NIGHT achievement ────────────────────────────────────────────

    @Test
    void checkPyroNight_3LaunchesNoOffence_awarded() {
        system.openEvent(false);
        system.setFireworksLaunchedForTesting(3);

        boolean awarded = system.checkPyroNight(type -> achievements.unlock(type));
        assertTrue(awarded);
        assertTrue(achievements.isUnlocked(AchievementType.PYRO_NIGHT));
    }

    @Test
    void checkPyroNight_3LaunchesWithOffence_notAwarded() {
        system.openEvent(false);
        system.setFireworksLaunchedForTesting(3);
        system.setFireworkPoliceSightingsForTesting(2);

        boolean awarded = system.checkPyroNight(type -> achievements.unlock(type));
        assertFalse(awarded);
    }

    @Test
    void checkPyroNight_2Launches_notAwarded() {
        system.openEvent(false);
        system.setFireworksLaunchedForTesting(2);

        boolean awarded = system.checkPyroNight(type -> achievements.unlock(type));
        assertFalse(awarded);
    }

    // ── 11. Mortar Option A — early launch ────────────────────────────────────

    @Test
    void interactWithMortar_eventNotActive_returnsEventNotActive() {
        assertEquals(MortarInteractResult.EVENT_NOT_ACTIVE,
                system.interactWithMortar(inventory, 19.8f, null, null));
    }

    @Test
    void interactWithMortar_beforeMortarActive_returnsMortarNotActive() {
        system.openEvent(false);
        assertEquals(MortarInteractResult.MORTAR_NOT_ACTIVE,
                system.interactWithMortar(inventory, 19.0f, null, null));
    }

    @Test
    void interactWithMortar_optionA_earlyLaunch_notorietyAndSaboteur() {
        system.openEvent(false);
        int notorietyBefore = notorietySystem.getNotoriety();

        MortarInteractResult result = system.interactWithMortar(
                inventory, 19.8f, null, type -> achievements.unlock(type));

        assertEquals(MortarInteractResult.EARLY_LAUNCH, result);
        assertTrue(notorietySystem.getNotoriety()
                >= notorietyBefore + BonfireNightSystem.EARLY_LAUNCH_NOTORIETY);
        assertTrue(achievements.isUnlocked(AchievementType.SABOTEUR));
    }

    @Test
    void interactWithMortar_alreadyTriggered_returnsAlreadyTriggered() {
        system.openEvent(false);
        system.setMortarTriggeredForTesting(true);
        assertEquals(MortarInteractResult.ALREADY_TRIGGERED,
                system.interactWithMortar(inventory, 19.8f, null, null));
    }

    // ── 12. Mortar Option B — banger plant ────────────────────────────────────

    @Test
    void interactWithMortar_optionB_bangerInInventory_plantsBanger() {
        system.openEvent(false);
        inventory.addItem(Material.BANGER_FIREWORK, 1);

        MortarInteractResult result = system.interactWithMortar(
                inventory, 19.8f, null, type -> achievements.unlock(type));

        assertEquals(MortarInteractResult.BANGER_PLANTED, result);
        assertTrue(system.isBangerPlanted());
        assertFalse(inventory.hasItem(Material.BANGER_FIREWORK)); // consumed
    }

    // ── 13. Banger catastrophe ────────────────────────────────────────────────

    @Test
    void update_bangerPlanted_at2000_triggersCatastrophe() {
        system.openEvent(false);
        system.setBonfireSpawnedForTesting(true);
        system.setBangerPlantedForTesting(true);

        NPC witness = new NPC(NPCType.PUBLIC, 0f, 0f, 0f);
        EventType result = system.update(1.0f, 20.0f, inventory, witness,
                type -> achievements.unlock(type));

        assertEquals(EventType.BANGER_CATASTROPHE, result);
        assertTrue(system.isBangerCatastropheFired());
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.CRIMINAL_DAMAGE) >= 1);
        assertTrue(notorietySystem.getNotoriety()
                >= BonfireNightSystem.BANGER_SABOTAGE_NOTORIETY);
        // Rumour seeded
        assertTrue(rumourNetwork.getAllRumours().stream()
                .anyMatch(r -> r.getType() == RumourType.FIREWORK_PRANK));
        // SABOTEUR achievement awarded
        assertTrue(achievements.isUnlocked(AchievementType.SABOTEUR));
    }

    // ── 14. Warden detection radius ───────────────────────────────────────────

    @Test
    void getWardenDetectionRadius_before2100_fullRadius() {
        assertEquals(BonfireNightSystem.WARDEN_DETECTION_RADIUS,
                system.getWardenDetectionRadius(20.0f), 0.001f);
    }

    @Test
    void getWardenDetectionRadius_after2100_halvedRadius() {
        assertEquals(BonfireNightSystem.WARDEN_DISTRACTED_RADIUS,
                system.getWardenDetectionRadius(21.0f), 0.001f);
    }

    // ── 15. Bonfire spawn / embers events ─────────────────────────────────────

    @Test
    void update_at1830_bonfireSpawnEvent() {
        system.openEvent(false);
        EventType result = system.update(0.1f, 18.5f, inventory, null, null);
        assertEquals(EventType.BONFIRE_SPAWNED, result);
        assertTrue(system.isBonfireSpawned());
    }

    @Test
    void update_at2200_bonfireEmbersEvent() {
        system.openEvent(false);
        system.setBonfireSpawnedForTesting(true);
        EventType result = system.update(0.1f, 22.0f, inventory, null, null);
        assertEquals(EventType.BONFIRE_EMBERS, result);
        assertTrue(system.isBonfireEmbers());
    }

    // ── 16. NPC spawning ──────────────────────────────────────────────────────

    @Test
    void spawnEventNPCs_returnsGaryAndKeith() {
        system.openEvent(false);
        List<NPC> npcs = system.spawnEventNPCs();
        assertEquals(2, npcs.size());
        assertTrue(npcs.stream().anyMatch(n -> n.getType() == NPCType.BONFIRE_WARDEN));
        assertTrue(npcs.stream().anyMatch(n -> n.getType() == NPCType.EVENT_COMPERE));
    }

    @Test
    void spawnEventNPCs_eventNotActive_returnsEmpty() {
        List<NPC> npcs = system.spawnEventNPCs();
        assertTrue(npcs.isEmpty());
    }

    @Test
    void spawnDarren_afterSpawnHour_returnsDarrenNPC() {
        system.openEvent(false);
        NPC darren = system.spawnDarren(17.0f);
        assertNotNull(darren);
        assertEquals(NPCType.FIREWORK_DEALER_NPC, darren.getType());
    }

    @Test
    void spawnDarren_tooEarly_returnsNull() {
        system.openEvent(false);
        assertNull(system.spawnDarren(16.9f));
    }

    // ── 17. Enum values exist ─────────────────────────────────────────────────

    @Test
    void bonfireWardenNPCType_exists() {
        assertNotNull(NPCType.BONFIRE_WARDEN);
    }

    @Test
    void eventCompereNPCType_exists() {
        assertNotNull(NPCType.EVENT_COMPERE);
    }

    @Test
    void fireworkTheftRumourType_exists() {
        assertNotNull(RumourType.FIREWORK_THEFT);
    }

    @Test
    void fireworkOffenceCrimeType_exists() {
        assertNotNull(CriminalRecord.CrimeType.FIREWORK_OFFENCE);
    }

    @Test
    void achievementTypes_exist() {
        assertNotNull(AchievementType.PENNY_FOR_THE_GUY);
        assertNotNull(AchievementType.PARTY_POOPER);
        assertNotNull(AchievementType.SABOTEUR);
        assertNotNull(AchievementType.PYRO_NIGHT);
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Random;

/**
 * Implements the Northfield Street Chugger mechanics for
 * {@code NPCType.CHUGGER} and {@code NPCType.CHUGGER_LEADER} (Tracy).
 *
 * <p>Features:
 * <ul>
 *   <li><b>Patrol</b>: CHUGGER NPCs patrol a 6-block radius outside the charity shop
 *       Mon–Sat 10:00–17:00. During rain, patrol radius shrinks to 2 blocks and no
 *       approaches are made.</li>
 *   <li><b>Interception</b>: Chuggger enters {@code ACCOSTING} state and approaches
 *       the player for up to {@value #INTERCEPT_TIMEOUT_SECONDS} seconds.</li>
 *   <li><b>Donation</b>: Player donates 2 COIN → Notoriety −1, {@code CHUGGER_GOODWILL}
 *       achievement, {@code GRATEFUL_CHUGGER} rumour seeded.</li>
 *   <li><b>Direct Debit</b>: Player signs up → {@code STANDING_ORDER} achievement,
 *       {@code DIRECT_DEBIT_ACTIVE} flag set. 1 COIN deducted on each of the next 3
 *       midnight ticks. Cancellable by returning to Tracy at the stand.</li>
 *   <li><b>Punch</b>: Chugger enters {@code FLEEING} state → Notoriety +8, WantedSystem
 *       +1 star, {@code CLIPBOARD_RAGE} achievement, {@code ASSAULT} crime recorded.</li>
 *   <li><b>Dodge</b>: Road-crossing, sprinting, or wearing COUNCIL_JACKET counts as a
 *       dodge. Three unique-method dodges → {@code CHUGGER_DODGER} achievement.</li>
 *   <li><b>Fake Tabard Scam</b>: Equip {@code CHARITY_TABARD} + {@code CHARITY_CLIPBOARD}
 *       to redirect real CHUGGERs and collect 1 COIN per NPC interaction (60% accept).
 *       Fraud detected on 2nd suspicious collection near Tracy or POLICE → Notoriety +6,
 *       WantedSystem +1, {@code CHARITY_FRAUD} crime, {@code DIRECT_DEBIT_HUSTLE}
 *       achievement, {@code CHARITY_FRAUD_RUMOUR} seeded.</li>
 *   <li><b>Tracy Hire</b>: Player can be hired by Tracy as a fake chugger; hitting the
 *       quota earns a 3 COIN reward.</li>
 * </ul>
 *
 * <p>Integrations: NotorietySystem, WantedSystem, CriminalRecord, DisguiseSystem,
 * FactionSystem (STREET_LADS warn of Tracy), StreetSkillSystem, NewspaperSystem,
 * RumourNetwork (DIRECT_DEBIT, GRATEFUL_CHUGGER, CHARITY_FRAUD_RUMOUR).
 *
 * <p>Issue #1299.
 */
public class ChuggerSystem {

    // ── Operating hours ──────────────────────────────────────────────────────

    /** Hour chuggers start patrol (10:00). */
    public static final float OPEN_HOUR = 10.0f;

    /** Hour chuggers end patrol (17:00). */
    public static final float CLOSE_HOUR = 17.0f;

    // ── Day-of-week constants (dayCount % 7, Mon=0 … Sun=6) ─────────────────

    /** Sunday index — no chuggers on Sunday. */
    private static final int SUNDAY = 6;

    // ── Patrol constants ─────────────────────────────────────────────────────

    /** Normal patrol radius in blocks. */
    public static final float PATROL_RADIUS_NORMAL = 6.0f;

    /** Patrol radius during rain, in blocks. */
    public static final float PATROL_RADIUS_RAIN = 2.0f;

    /** Maximum seconds a chugger will remain in ACCOSTING state before giving up. */
    public static final float INTERCEPT_TIMEOUT_SECONDS = 12.0f;

    // ── Donation constants ───────────────────────────────────────────────────

    /** COIN cost of a standard donation. */
    public static final int DONATION_COIN_COST = 2;

    /** Notoriety reduction on donation. */
    public static final int DONATION_NOTORIETY_REDUCTION = 1;

    // ── Direct debit constants ───────────────────────────────────────────────

    /** COIN deducted per day for the direct debit over 3 days. */
    public static final int DIRECT_DEBIT_DAILY_COST = 1;

    /** Number of days the direct debit runs for. */
    public static final int DIRECT_DEBIT_DAYS = 3;

    // ── Punch constants ──────────────────────────────────────────────────────

    /** Notoriety gain on punching a chugger. */
    public static final int PUNCH_NOTORIETY_GAIN = 8;

    /** WantedSystem stars added on punching a chugger. */
    public static final int PUNCH_WANTED_STARS = 1;

    // ── Fake tabard scam constants ───────────────────────────────────────────

    /** Probability (0–1) that an NPC accepts a fake donation. */
    public static final float FAKE_DONATION_ACCEPT_CHANCE = 0.60f;

    /** COIN collected per fake donation interaction. */
    public static final int FAKE_DONATION_COIN = 1;

    /** Number of suspicious collections that trigger fraud detection. */
    public static final int FRAUD_DETECTION_THRESHOLD = 2;

    /** Notoriety gain on fraud detection. */
    public static final int FRAUD_NOTORIETY_GAIN = 6;

    /** WantedSystem stars on fraud detection. */
    public static final int FRAUD_WANTED_STARS = 1;

    // ── Tracy hire constants ─────────────────────────────────────────────────

    /** COIN reward for hitting Tracy's donation quota as a hired fake chugger. */
    public static final int TRACY_QUOTA_REWARD = 3;

    /** Number of fake donations needed to hit Tracy's quota. */
    public static final int TRACY_QUOTA_TARGET = 5;

    // ── Dodge tracking ───────────────────────────────────────────────────────

    /** Number of unique dodge methods needed for CHUGGER_DODGER achievement. */
    public static final int DODGE_COUNT_FOR_ACHIEVEMENT = 3;

    // ── State ────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Remaining seconds until the active interception times out. */
    private float interceptTimer;

    /** Whether a chugger is currently accosting the player. */
    private boolean playerBeingAccosted;

    /** Whether the direct debit is currently active. */
    private boolean directDebitActive;

    /** Days remaining on the direct debit drain (0 = not active / fully paid). */
    private int directDebitDaysRemaining;

    /** Day count when the last midnight direct-debit tick was processed. */
    private int lastDirectDebitDay;

    /** Number of suspicious fake-donation interactions this session. */
    private int suspiciousFakeCollections;

    /** Whether the fake-tabard fraud has been detected (one-shot). */
    private boolean fraudDetected;

    /** Number of fake donations collected this session. */
    private int fakeDonationsCollected;

    /** Whether the player is currently hired as a fake chugger by Tracy. */
    private boolean hiredByTracy;

    /** Fake-donation count toward Tracy's quota while hired. */
    private int tracyQuotaProgress;

    /** Bitmask of dodge methods used: bit 0 = road-cross, bit 1 = sprint, bit 2 = council_jacket. */
    private int dodgeMethodsMask;

    /** Number of unique dodge methods used. */
    private int dodgeCount;

    /** Whether CHUGGER_DODGER achievement has been awarded. */
    private boolean dodgerAchievementAwarded;

    // ── Injected dependencies (all optional — null-checked before use) ────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private StreetSkillSystem streetSkillSystem;
    private NewspaperSystem newspaperSystem;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ChuggerSystem() {
        this(new Random());
    }

    public ChuggerSystem(Random rng) {
        this.rng = rng;
    }

    // ── Dependency injection setters ──────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setStreetSkillSystem(StreetSkillSystem streetSkillSystem) {
        this.streetSkillSystem = streetSkillSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update timers. Call once per frame.
     *
     * @param delta  seconds since last frame
     * @param time   current time system
     * @param chugger the CHUGGER NPC currently accosting the player (may be null)
     */
    public void update(float delta, TimeSystem time, NPC chugger) {
        // Tick interception timeout
        if (playerBeingAccosted && interceptTimer > 0f) {
            interceptTimer -= delta;
            if (interceptTimer <= 0f) {
                playerBeingAccosted = false;
                interceptTimer = 0f;
                if (chugger != null && chugger.getState() == NPCState.ACCOSTING) {
                    chugger.setState(NPCState.PATROL);
                }
            }
        }
    }

    // ── Operating hours / patrol checks ──────────────────────────────────────

    /**
     * Returns whether chugggers are currently on patrol (Mon–Sat 10:00–17:00).
     *
     * @param time current time system
     * @return true if chuggers should be active
     */
    public boolean isPatrolActive(TimeSystem time) {
        float hour = time.getTime();
        if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
            return false;
        }
        // Sunday = day % 7 == 6 → no patrol
        int dow = time.getDayCount() % 7;
        return dow != SUNDAY;
    }

    /**
     * Returns the effective patrol radius given the current weather.
     *
     * @param weather the current weather state (may be null — treated as CLEAR)
     * @return patrol radius in blocks
     */
    public float getPatrolRadius(Weather weather) {
        if (weather != null && weather.isRaining()) {
            return PATROL_RADIUS_RAIN;
        }
        return PATROL_RADIUS_NORMAL;
    }

    /**
     * Returns whether chugggers should approach the player given the current weather.
     * During rain, no approaches are made (even if patrol is active).
     *
     * @param weather the current weather state
     * @return true if approaches are allowed
     */
    public boolean canApproach(Weather weather) {
        return weather == null || !weather.isRaining();
    }

    // ── Interception ──────────────────────────────────────────────────────────

    /**
     * Called when a CHUGGER NPC starts accosting the player.
     *
     * @param chugger the chugger NPC
     */
    public void startIntercept(NPC chugger) {
        playerBeingAccosted = true;
        interceptTimer = INTERCEPT_TIMEOUT_SECONDS;
        if (chugger != null) {
            chugger.setState(NPCState.ACCOSTING);
        }
    }

    /** Returns whether the player is currently being accosted by a chugger. */
    public boolean isPlayerBeingAccosted() {
        return playerBeingAccosted;
    }

    // ── Player interaction outcomes ───────────────────────────────────────────

    /**
     * Result of a player interaction with a chugger.
     */
    public enum InteractionResult {
        /** Player donated 2 COIN; Notoriety reduced by 1. */
        DONATED,
        /** Player signed up for direct debit (3-day, 1 COIN/day). */
        SIGNED_UP_DIRECT_DEBIT,
        /** Player punched the chugger; enters FLEEING, Notoriety +8, Wanted +1. */
        PUNCHED,
        /** Player dodged via road-crossing. */
        DODGED_ROAD,
        /** Player dodged via sprint. */
        DODGED_SPRINT,
        /** Player dodged via COUNCIL_JACKET disguise. */
        DODGED_JACKET,
        /** Player cannot afford the donation. */
        INSUFFICIENT_FUNDS,
        /** Chugger is not currently accosting the player. */
        NOT_ACCOSTING,
    }

    /**
     * Player chooses to donate to the chugger.
     *
     * @param inventory    player inventory
     * @param chugger      the CHUGGER NPC (for rumour seeding)
     * @return interaction result
     */
    public InteractionResult donate(Inventory inventory, NPC chugger) {
        if (!playerBeingAccosted) {
            return InteractionResult.NOT_ACCOSTING;
        }
        if (inventory.getItemCount(Material.COIN) < DONATION_COIN_COST) {
            return InteractionResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, DONATION_COIN_COST);
        playerBeingAccosted = false;
        interceptTimer = 0f;
        if (chugger != null) {
            chugger.setState(NPCState.WANDERING);
        }

        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(DONATION_NOTORIETY_REDUCTION, null);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CHUGGER_GOODWILL);
        }
        if (rumourNetwork != null && chugger != null) {
            rumourNetwork.addRumour(chugger, new Rumour(RumourType.GRATEFUL_CHUGGER,
                    "One of the chuggers outside the charity shop was saying someone actually donated — proper nice of 'em."));
        }
        return InteractionResult.DONATED;
    }

    /**
     * Player signs up for a direct debit with the chugger.
     *
     * @param inventory player inventory
     * @param chugger   the CHUGGER NPC (for rumour seeding)
     * @param time      current time (for seeding day reference)
     * @return interaction result
     */
    public InteractionResult signUpDirectDebit(Inventory inventory, NPC chugger, TimeSystem time) {
        if (!playerBeingAccosted) {
            return InteractionResult.NOT_ACCOSTING;
        }
        playerBeingAccosted = false;
        interceptTimer = 0f;
        if (chugger != null) {
            chugger.setState(NPCState.WANDERING);
        }

        directDebitActive = true;
        directDebitDaysRemaining = DIRECT_DEBIT_DAYS;
        lastDirectDebitDay = time.getDayCount();

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.STANDING_ORDER);
        }
        if (rumourNetwork != null && chugger != null) {
            rumourNetwork.addRumour(chugger, new Rumour(RumourType.DIRECT_DEBIT,
                    "Heard someone signed up for a direct debit outside the charity shop — still paying it now."));
        }
        return InteractionResult.SIGNED_UP_DIRECT_DEBIT;
    }

    /**
     * Player punches the chugger.
     *
     * @param chugger the CHUGGER NPC
     * @return interaction result
     */
    public InteractionResult punchChugger(NPC chugger) {
        if (chugger != null) {
            chugger.setState(NPCState.FLEEING);
        }
        playerBeingAccosted = false;
        interceptTimer = 0f;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(PUNCH_NOTORIETY_GAIN, null);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(PUNCH_WANTED_STARS, 0f, 0f, 0f, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.ASSAULT);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CLIPBOARD_RAGE);
        }
        return InteractionResult.PUNCHED;
    }

    /**
     * Player dodges the chugger by crossing the road.
     *
     * @return interaction result
     */
    public InteractionResult dodgeRoadCross() {
        playerBeingAccosted = false;
        interceptTimer = 0f;
        recordDodge(0);
        return InteractionResult.DODGED_ROAD;
    }

    /**
     * Player dodges the chugger by sprinting.
     *
     * @return interaction result
     */
    public InteractionResult dodgeSprint() {
        playerBeingAccosted = false;
        interceptTimer = 0f;
        recordDodge(1);
        return InteractionResult.DODGED_SPRINT;
    }

    /**
     * Player dodges the chugger by wearing COUNCIL_JACKET.
     *
     * @param inventory player inventory
     * @return interaction result — DODGED_JACKET if equipped, NOT_ACCOSTING if not accosting,
     *         or DODGED_JACKET even if inventory doesn't have the item (caller should verify)
     */
    public InteractionResult dodgeWithCouncilJacket(Inventory inventory) {
        if (!playerBeingAccosted) {
            return InteractionResult.NOT_ACCOSTING;
        }
        playerBeingAccosted = false;
        interceptTimer = 0f;
        recordDodge(2);
        return InteractionResult.DODGED_JACKET;
    }

    // ── Midnight direct-debit drain ───────────────────────────────────────────

    /**
     * Called on each midnight tick. Deducts 1 COIN if the direct debit is active.
     *
     * @param inventory   player inventory
     * @param currentDay  the current day count
     * @return number of COIN deducted (1 if active and day advanced, else 0)
     */
    public int onMidnightTick(Inventory inventory, int currentDay) {
        if (!directDebitActive || directDebitDaysRemaining <= 0) {
            return 0;
        }
        if (currentDay <= lastDirectDebitDay) {
            return 0; // Already processed today
        }
        lastDirectDebitDay = currentDay;
        directDebitDaysRemaining--;
        int deduct = Math.min(DIRECT_DEBIT_DAILY_COST, inventory.getItemCount(Material.COIN));
        if (deduct > 0) {
            inventory.removeItem(Material.COIN, deduct);
        }
        if (directDebitDaysRemaining <= 0) {
            directDebitActive = false;
        }
        return deduct;
    }

    /**
     * Cancel the active direct debit (player returns to Tracy at the stand).
     */
    public void cancelDirectDebit() {
        directDebitActive = false;
        directDebitDaysRemaining = 0;
    }

    // ── Fake tabard scam ──────────────────────────────────────────────────────

    /**
     * Result of attempting a fake-donation interaction while wearing CHARITY_TABARD +
     * CHARITY_CLIPBOARD.
     */
    public enum FakeCollectionResult {
        /** NPC accepted the fake donation; player receives 1 COIN. */
        ACCEPTED,
        /** NPC rejected the fake donation. */
        REJECTED,
        /** Fraud detected — Notoriety +6, Wanted +1, crime recorded. */
        FRAUD_DETECTED,
        /** Player not wearing required disguise items. */
        NOT_DISGUISED,
    }

    /**
     * Player attempts a fake charity collection while wearing CHARITY_TABARD and
     * CHARITY_CLIPBOARD, near a real CHUGGER NPC.
     *
     * @param inventory       player inventory
     * @param nearTracy       true if the CHUGGER_LEADER (Tracy) is within detection range
     * @param nearPolice      true if a POLICE NPC is within detection range
     * @param tracy           Tracy NPC (for rumour seeding; may be null)
     * @return the collection result
     */
    public FakeCollectionResult attemptFakeCollection(
            Inventory inventory,
            boolean nearTracy,
            boolean nearPolice,
            NPC tracy) {

        if (!inventory.hasItem(Material.CHARITY_TABARD) || !inventory.hasItem(Material.CHARITY_CLIPBOARD)) {
            return FakeCollectionResult.NOT_DISGUISED;
        }

        // Check fraud detection first
        if (!fraudDetected && (nearTracy || nearPolice)) {
            suspiciousFakeCollections++;
            if (suspiciousFakeCollections >= FRAUD_DETECTION_THRESHOLD) {
                return detectFraud(tracy);
            }
        }

        // 60% NPC accept chance
        if (rng.nextFloat() < FAKE_DONATION_ACCEPT_CHANCE) {
            inventory.addItem(Material.COIN, FAKE_DONATION_COIN);
            fakeDonationsCollected++;
            if (hiredByTracy) {
                tracyQuotaProgress++;
            }
            return FakeCollectionResult.ACCEPTED;
        }
        return FakeCollectionResult.REJECTED;
    }

    private FakeCollectionResult detectFraud(NPC tracy) {
        fraudDetected = true;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FRAUD_NOTORIETY_GAIN, null);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(FRAUD_WANTED_STARS, 0f, 0f, 0f, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.CHARITY_FRAUD);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DIRECT_DEBIT_HUSTLE);
        }
        if (rumourNetwork != null && tracy != null) {
            rumourNetwork.addRumour(tracy, new Rumour(RumourType.CHARITY_FRAUD_RUMOUR,
                    "Someone's been doing a fake charity collection on the high street — wearing a tabard and everything."));
        }
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline("FAKE CHUGGER EXPOSED ON NORTHFIELD HIGH STREET");
        }
        return FakeCollectionResult.FRAUD_DETECTED;
    }

    // ── Tracy hire ────────────────────────────────────────────────────────────

    /**
     * Result of interacting with Tracy (CHUGGER_LEADER) at the clipboard stand.
     */
    public enum TracyInteractionResult {
        /** Tracy hands the player a CHARITY_TABARD; player is now hired. */
        HIRED_AS_FAKE_CHUGGER,
        /** Player has already been hired; no change. */
        ALREADY_HIRED,
        /** Tracy is not present (outside operating hours). */
        TRACY_NOT_PRESENT,
        /** Direct debit cancelled. */
        DIRECT_DEBIT_CANCELLED,
        /** Quota hit; player receives 3 COIN reward. */
        QUOTA_REWARD_PAID,
    }

    /**
     * Player presses E on Tracy (CHUGGER_LEADER) at the CHARITY_CLIPBOARD_STAND_PROP.
     *
     * <p>Effects depend on state:
     * <ul>
     *   <li>If the direct debit is active → cancel it.</li>
     *   <li>If hired and quota met → pay reward.</li>
     *   <li>Otherwise → hire the player and give a CHARITY_TABARD.</li>
     * </ul>
     *
     * @param inventory player inventory
     * @param time      current time (for opening-hours check)
     * @return the result of the interaction
     */
    public TracyInteractionResult interactWithTracy(Inventory inventory, TimeSystem time) {
        if (!isPatrolActive(time)) {
            return TracyInteractionResult.TRACY_NOT_PRESENT;
        }

        // Cancel direct debit if active
        if (directDebitActive) {
            cancelDirectDebit();
            return TracyInteractionResult.DIRECT_DEBIT_CANCELLED;
        }

        // Pay quota reward if target reached
        if (hiredByTracy && tracyQuotaProgress >= TRACY_QUOTA_TARGET) {
            inventory.addItem(Material.COIN, TRACY_QUOTA_REWARD);
            hiredByTracy = false;
            tracyQuotaProgress = 0;
            return TracyInteractionResult.QUOTA_REWARD_PAID;
        }

        // Hire the player
        if (hiredByTracy) {
            return TracyInteractionResult.ALREADY_HIRED;
        }
        hiredByTracy = true;
        tracyQuotaProgress = 0;
        inventory.addItem(Material.CHARITY_TABARD, 1);
        return TracyInteractionResult.HIRED_AS_FAKE_CHUGGER;
    }

    // ── Dodge recording ───────────────────────────────────────────────────────

    /**
     * Record a dodge method and check for CHUGGER_DODGER achievement.
     *
     * @param bit bit position for this method (0=road, 1=sprint, 2=jacket)
     */
    private void recordDodge(int bit) {
        if ((dodgeMethodsMask & (1 << bit)) == 0) {
            dodgeMethodsMask |= (1 << bit);
            dodgeCount++;
        }
        if (!dodgerAchievementAwarded && dodgeCount >= DODGE_COUNT_FOR_ACHIEVEMENT) {
            dodgerAchievementAwarded = true;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CHUGGER_DODGER);
            }
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns whether the direct debit is currently active. */
    public boolean isDirectDebitActive() {
        return directDebitActive;
    }

    /** Returns the number of direct debit days remaining. */
    public int getDirectDebitDaysRemaining() {
        return directDebitDaysRemaining;
    }

    /** Returns whether the fake-tabard fraud has been detected. */
    public boolean isFraudDetected() {
        return fraudDetected;
    }

    /** Returns the number of fake donations collected this session. */
    public int getFakeDonationsCollected() {
        return fakeDonationsCollected;
    }

    /** Returns whether the player is currently hired by Tracy. */
    public boolean isHiredByTracy() {
        return hiredByTracy;
    }

    /** Returns the player's current progress toward Tracy's quota. */
    public int getTracyQuotaProgress() {
        return tracyQuotaProgress;
    }

    /** Returns the number of unique dodge methods used. */
    public int getDodgeCount() {
        return dodgeCount;
    }

    /** Returns the number of suspicious fake-collection events. */
    public int getSuspiciousFakeCollections() {
        return suspiciousFakeCollections;
    }

    /** Returns the remaining intercept timer in seconds. */
    public float getInterceptTimer() {
        return interceptTimer;
    }
}

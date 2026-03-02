package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1224: Northfield Cybernet Internet Café — Online Marketplace Hustle,
 * Fake Document Printing &amp; the Phishing Scam.
 *
 * <p>Cybernet is open daily 09:00–23:00. Sessions cost 1 COIN for 5 in-game
 * minutes of terminal access. Features:
 * <ul>
 *   <li>FlipIt Online Marketplace — list items bought cheap, sell for markup.</li>
 *   <li>Fake Document Printing — back-room only; FORGED_UC_LETTER, FAKE_REFERENCE_LETTER.</li>
 *   <li>Phishing Scam — passive 3–8 COIN per session.</li>
 *   <li>Remote UC Sign-On — satisfies JobCentre weekly sign-on.</li>
 *   <li>Pirate Radio Remote Scheduling — schedule broadcasts ahead of time.</li>
 *   <li>Burner Phone Top-Up — 1 COIN adds 5 units to BURNER_PHONE.</li>
 * </ul>
 *
 * <p>NPCs: Asif ({@code INTERNET_CAFE_OWNER}, daytime),
 * Hamza ({@code INTERNET_CAFE_ASSISTANT}, 18:00–23:00).
 */
public class InternetCafeSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour at which Cybernet opens (inclusive). */
    public static final float OPEN_HOUR  = 9.0f;
    /** Hour at which Cybernet closes (exclusive). */
    public static final float CLOSE_HOUR = 23.0f;

    /** Hour at which Hamza's evening shift begins. */
    public static final float HAMZA_START_HOUR = 18.0f;

    // ── Session constants ──────────────────────────────────────────────────────

    /** Cost of one terminal session (COIN). */
    public static final int SESSION_COST = 1;
    /** Duration of one terminal session in in-game minutes. */
    public static final float SESSION_DURATION_MINUTES = 5.0f;

    // ── FlipIt marketplace ─────────────────────────────────────────────────────

    /** Maximum active FlipIt listings at once. */
    public static final int MAX_LISTINGS = 3;
    /** Minimum sale multiplier applied to source price. */
    public static final float FLIPIT_MIN_MULTIPLIER = 1.5f;
    /** Maximum sale multiplier applied to source price. */
    public static final float FLIPIT_MAX_MULTIPLIER = 3.0f;
    /** FlipIt platform commission rate (10%). */
    public static final float FLIPIT_COMMISSION = 0.10f;
    /** Minimum in-game hours before a listing can sell. */
    public static final float FLIPIT_MIN_HOURS = 1.0f;
    /** Maximum in-game hours before a listing can sell. */
    public static final float FLIPIT_MAX_HOURS = 5.0f;
    /** Notoriety threshold above which dodgy listings risk COMPUTER_FRAUD. */
    public static final int COMPUTER_FRAUD_NOTORIETY_THRESHOLD = 40;
    /** Police proximity distance (blocks) that triggers COMPUTER_FRAUD check. */
    public static final float COMPUTER_FRAUD_POLICE_RANGE = 10.0f;
    /** Source price assumed for FlipIt items (COIN). */
    public static final int DEFAULT_SOURCE_PRICE = 2;
    /** TRADING XP awarded per completed FlipIt sale. */
    public static final int TRADING_XP_PER_SALE = 1;

    // ── Phishing scam ──────────────────────────────────────────────────────────

    /** Minimum COIN earned per phishing session. */
    public static final int PHISHING_MIN_COIN = 3;
    /** Maximum COIN earned per phishing session. */
    public static final int PHISHING_MAX_COIN = 8;
    /** TRADING XP required for +2 COIN phishing bonus. */
    public static final int PHISHING_TRADING_XP_BONUS_THRESHOLD = 5;
    /** Bonus COIN for TRADING XP ≥ threshold. */
    public static final int PHISHING_TRADING_XP_BONUS = 2;
    /** Notoriety threshold above which CYBER_FRAUD risk applies. */
    public static final int CYBER_FRAUD_NOTORIETY_THRESHOLD = 50;
    /** Probability of CYBER_FRAUD detection per phishing session (10%). */
    public static final float CYBER_FRAUD_CHANCE = 0.10f;

    // ── Fake document printing ─────────────────────────────────────────────────

    /** Notoriety penalty when caught printing in plain sight (+8). */
    public static final int PRINTING_CAUGHT_NOTORIETY = 8;
    /** WantedSystem stars added when Asif spots plain-sight printing (+1). */
    public static final int PRINTING_CAUGHT_WANTED_STARS = 1;
    /** Hamza detection chance for plain-sight printing (25%). */
    public static final float HAMZA_DETECTION_CHANCE = 0.25f;

    // ── Notoriety refusal threshold ────────────────────────────────────────────

    /** Notoriety at or above which Asif refuses service. */
    public static final int SERVICE_REFUSED_NOTORIETY = 80;

    // ── Burner phone ───────────────────────────────────────────────────────────

    /** COIN cost to top up BURNER_PHONE at the PREPAID_CARD_READER_PROP. */
    public static final int BURNER_TOP_UP_COST = 1;
    /** Units added per top-up. */
    public static final int BURNER_TOP_UP_UNITS = 5;

    // ── Enums ──────────────────────────────────────────────────────────────────

    /** Result of attempting to start a terminal session. */
    public enum SessionResult {
        SESSION_STARTED,
        INSUFFICIENT_FUNDS,
        CAFE_CLOSED,
        SERVICE_REFUSED
    }

    /** Status of a FlipIt listing. */
    public enum ListingStatus {
        PENDING,
        SOLD,
        FLAGGED
    }

    /** Result of listing an item on FlipIt. */
    public enum ListResult {
        LISTED,
        LISTED_RISKY,
        MAX_LISTINGS_REACHED,
        NO_SESSION
    }

    /** Document types that can be printed in the back room. */
    public enum DocumentType {
        FORGED_UC_LETTER(Material.FORGED_UC_LETTER),
        FAKE_REFERENCE_LETTER(Material.FAKE_REFERENCE_LETTER),
        FORGED_TV_LICENCE(Material.FORGED_TV_LICENCE);

        private final Material material;

        DocumentType(Material material) {
            this.material = material;
        }

        public Material getMaterial() {
            return material;
        }
    }

    /** Result of a document print attempt. */
    public enum PrintResult {
        PRINTED,
        MISSING_MATERIALS,
        NO_BACK_ROOM_ACCESS,
        CAUGHT_BY_ASIF,
        CAUGHT_BY_HAMZA
    }

    /** Result of a back-room bribe attempt with Hamza. */
    public enum BribeResult {
        ACCESS_GRANTED,
        NO_ENERGY_DRINK,
        HAMZA_NOT_PRESENT
    }

    /** Result of running a phishing session. */
    public enum PhishingResult {
        SUCCESS,
        CYBER_FRAUD_DETECTED,
        NO_SESSION
    }

    /** Result of attempting to top up a burner phone. */
    public enum BurnerTopUpResult {
        SUCCESS,
        NO_BURNER_PHONE,
        INSUFFICIENT_FUNDS
    }

    /** Result of a remote UC sign-on. */
    public enum SignOnResult {
        SIGNED_ON,
        NO_SESSION,
        NO_ACTIVE_CLAIM
    }

    /** Result of scheduling a pirate radio broadcast. */
    public enum ScheduleResult {
        SCHEDULED,
        NO_SESSION,
        NO_TRANSMITTER
    }

    // ── Inner class: FlipIt listing ────────────────────────────────────────────

    /** Represents a single FlipIt marketplace listing. */
    public static class FlipItListing {
        private final Material item;
        private final int sourcePrice;
        private final float listPriceMultiplier;
        private final float hoursToSell;
        private float hoursElapsed;
        private ListingStatus status;
        private boolean dodgy;

        public FlipItListing(Material item, int sourcePrice, float listPriceMultiplier,
                             float hoursToSell, boolean dodgy) {
            this.item               = item;
            this.sourcePrice        = sourcePrice;
            this.listPriceMultiplier = listPriceMultiplier;
            this.hoursToSell        = hoursToSell;
            this.hoursElapsed       = 0f;
            this.status             = ListingStatus.PENDING;
            this.dodgy              = dodgy;
        }

        /** Returns the list price before commission. */
        public int getRawListPrice() {
            return (int) (sourcePrice * listPriceMultiplier);
        }

        /** Returns the net proceeds after 10% commission. */
        public int getNetProceeds() {
            return (int) Math.floor(getRawListPrice() * (1.0 - FLIPIT_COMMISSION));
        }

        public Material getItem() { return item; }
        public int getSourcePrice() { return sourcePrice; }
        public float getHoursToSell() { return hoursToSell; }
        public float getHoursElapsed() { return hoursElapsed; }
        public ListingStatus getStatus() { return status; }
        public boolean isDodgy() { return dodgy; }

        public void advanceTime(float hours) { hoursElapsed += hours; }
        public void setStatus(ListingStatus status) { this.status = status; }

        /** Returns true if the listing has elapsed enough time to be sold. */
        public boolean isReadyToSell() {
            return status == ListingStatus.PENDING && hoursElapsed >= hoursToSell;
        }
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    private boolean sessionActive;
    private float sessionElapsedMinutes;

    private boolean backRoomAccess;
    private int burnerPhoneUnits;

    private final List<FlipItListing> listings = new ArrayList<>();
    private int totalSalesCompleted;

    /** Last in-game hour at which a pirate radio broadcast was scheduled. */
    private float scheduledBroadcastHour = -1f;
    private boolean broadcastScheduled;

    // ── Injected systems ───────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;
    private NoiseSystem noiseSystem;
    private RumourNetwork rumourNetwork;
    private StreetSkillSystem streetSkillSystem;
    private PirateRadioSystem pirateRadioSystem;
    private NPC asif;
    private NPC hamza;

    // ── Constructor ────────────────────────────────────────────────────────────

    public InternetCafeSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem s)     { this.notorietySystem = s; }
    public void setCriminalRecord(CriminalRecord r)        { this.criminalRecord = r; }
    public void setAchievementSystem(AchievementSystem s)  { this.achievementSystem = s; }
    public void setWantedSystem(WantedSystem s)            { this.wantedSystem = s; }
    public void setNoiseSystem(NoiseSystem s)              { this.noiseSystem = s; }
    public void setRumourNetwork(RumourNetwork n)          { this.rumourNetwork = n; }
    public void setStreetSkillSystem(StreetSkillSystem s)  { this.streetSkillSystem = s; }
    public void setPirateRadioSystem(PirateRadioSystem s)  { this.pirateRadioSystem = s; }
    public void setAsif(NPC npc)                           { this.asif = npc; }
    public void setHamza(NPC npc)                          { this.hamza = npc; }

    // ── Opening hours ──────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if Cybernet is open at the given hour.
     *
     * @param hour current in-game hour (0–23.99)
     * @return true if OPEN_HOUR ≤ hour &lt; CLOSE_HOUR
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns {@code true} if Hamza is on shift (18:00–23:00).
     *
     * @param hour current in-game hour
     */
    public boolean isHamzaOnShift(float hour) {
        return hour >= HAMZA_START_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns {@code true} if Asif refuses service due to high notoriety.
     *
     * @param notoriety current notoriety value
     */
    public boolean isServiceRefused(int notoriety) {
        return notoriety >= SERVICE_REFUSED_NOTORIETY;
    }

    // ── Terminal session ───────────────────────────────────────────────────────

    /**
     * Attempts to start a 5-minute terminal session by deducting 1 COIN.
     *
     * @param inventory   player inventory
     * @param timeSystem  current time (used to check opening hours)
     * @return session result enum
     */
    public SessionResult startSession(Inventory inventory, TimeSystem timeSystem) {
        float hour = timeSystem.getHours();
        if (!isOpen(hour)) {
            return SessionResult.CAFE_CLOSED;
        }
        if (notorietySystem != null && isServiceRefused(notorietySystem.getNotoriety())) {
            return SessionResult.SERVICE_REFUSED;
        }
        if (inventory.getItemCount(Material.COIN) < SESSION_COST) {
            return SessionResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, SESSION_COST);
        sessionActive = true;
        sessionElapsedMinutes = 0f;
        return SessionResult.SESSION_STARTED;
    }

    /**
     * Advances the session timer by {@code deltaMinutes} in-game minutes.
     * Ends the session if the 5-minute limit is reached.
     *
     * @param deltaMinutes elapsed in-game minutes
     */
    public void tickSession(float deltaMinutes) {
        if (!sessionActive) return;
        sessionElapsedMinutes += deltaMinutes;
        if (sessionElapsedMinutes >= SESSION_DURATION_MINUTES) {
            sessionActive = false;
        }
    }

    /**
     * Returns the remaining session minutes. Returns 0 if already expired (and
     * also marks session inactive).
     *
     * @param elapsed             how many in-game minutes have elapsed since session start
     * @param sessionLengthMinutes the length of the session in minutes
     */
    public float sessionRemainingMinutes(float elapsed, float sessionLengthMinutes) {
        float remaining = sessionLengthMinutes - elapsed;
        if (remaining <= 0f) {
            sessionActive = false;
            return 0f;
        }
        return remaining;
    }

    public boolean isSessionActive() { return sessionActive; }

    /** Force-ends the active session (for testing / timeout UI). */
    public void endSession() { sessionActive = false; }

    // ── FlipIt marketplace ─────────────────────────────────────────────────────

    /**
     * Lists an item from inventory on FlipIt.
     *
     * <p>Removes the item from inventory and creates a pending listing.
     * Dodgy items (STOLEN_PHONE, COUNTERFEIT_NOTE) are flagged at risk.
     *
     * @param item         the material to list
     * @param inventory    player inventory
     * @param policeNearby true if a POLICE NPC is within COMPUTER_FRAUD_POLICE_RANGE
     * @return listing result
     */
    public ListResult listItem(Material item, Inventory inventory, boolean policeNearby) {
        if (!sessionActive) return ListResult.NO_SESSION;
        if (listings.size() >= MAX_LISTINGS) return ListResult.MAX_LISTINGS_REACHED;
        if (inventory.getItemCount(item) < 1) return ListResult.NO_SESSION;

        inventory.removeItem(item, 1);

        boolean dodgy = (item == Material.STOLEN_PHONE || item == Material.COUNTERFEIT_NOTE);
        float multiplier = FLIPIT_MIN_MULTIPLIER
                + random.nextFloat() * (FLIPIT_MAX_MULTIPLIER - FLIPIT_MIN_MULTIPLIER);
        float hoursToSell = FLIPIT_MIN_HOURS
                + random.nextFloat() * (FLIPIT_MAX_HOURS - FLIPIT_MIN_HOURS);

        FlipItListing listing = new FlipItListing(item, DEFAULT_SOURCE_PRICE, multiplier,
                hoursToSell, dodgy);
        listings.add(listing);

        // Check for COMPUTER_FRAUD risk
        if (dodgy && policeNearby && notorietySystem != null
                && notorietySystem.getNotoriety() >= COMPUTER_FRAUD_NOTORIETY_THRESHOLD) {
            listing.setStatus(ListingStatus.FLAGGED);
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.COMPUTER_FRAUD);
            }
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                        new Rumour(RumourType.GANG_ACTIVITY,
                                "Police flagged dodgy listings at Cybernet."));
            }
            return ListResult.LISTED_RISKY;
        }

        return ListResult.LISTED;
    }

    /**
     * Advances all PENDING listings by {@code hours} in-game hours.
     * Listings that reach their sell time are marked SOLD.
     *
     * @param hours elapsed in-game hours
     */
    public void advanceListingTime(float hours) {
        for (FlipItListing listing : listings) {
            if (listing.getStatus() == ListingStatus.PENDING) {
                listing.advanceTime(hours);
                if (listing.isReadyToSell()) {
                    listing.setStatus(ListingStatus.SOLD);
                    if (rumourNetwork != null) {
                        rumourNetwork.addRumour(null,
                                new Rumour(RumourType.LOCAL_EVENT,
                                        "Some lad on FlipIt's been flogging dodgy gear out of Cybernet."));
                    }
                }
            }
        }
    }

    /**
     * Collects proceeds from all SOLD listings and pays them to the player.
     * Removes collected listings, awards XP and achievements.
     *
     * @param inventory player inventory
     * @return total COIN awarded
     */
    public int collectSaleProceeds(Inventory inventory) {
        int total = 0;
        List<FlipItListing> toRemove = new ArrayList<>();
        for (FlipItListing listing : listings) {
            if (listing.getStatus() == ListingStatus.SOLD) {
                int proceeds = listing.getNetProceeds();
                inventory.addItem(Material.COIN, proceeds);
                total += proceeds;
                toRemove.add(listing);
                totalSalesCompleted++;

                // Award TRADING XP
                if (streetSkillSystem != null) {
                    streetSkillSystem.awardXP(StreetSkillSystem.Skill.TRADING, TRADING_XP_PER_SALE);
                }

                // Achievements
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.DIGITAL_HUSTLER);
                    if (totalSalesCompleted >= 10) {
                        achievementSystem.unlock(AchievementType.POWERSELLER);
                    }
                }
            }
        }
        listings.removeAll(toRemove);
        return total;
    }

    public List<FlipItListing> getListings() { return listings; }
    public int getTotalSalesCompleted() { return totalSalesCompleted; }

    // ── Back-room access ───────────────────────────────────────────────────────

    /**
     * Attempts to bribe Hamza to unlock the back room.
     *
     * @param inventory  player inventory (must contain ENERGY_DRINK)
     * @param currentHour current in-game hour
     * @return bribe result
     */
    public BribeResult bribeHamza(Inventory inventory, float currentHour) {
        if (!isHamzaOnShift(currentHour)) return BribeResult.HAMZA_NOT_PRESENT;
        if (inventory.getItemCount(Material.ENERGY_DRINK) < 1) return BribeResult.NO_ENERGY_DRINK;
        inventory.removeItem(Material.ENERGY_DRINK, 1);
        backRoomAccess = true;
        if (hamza != null) {
            hamza.setSpeechText("Don't tell my uncle.", 5f);
        }
        return BribeResult.ACCESS_GRANTED;
    }

    /** Grants back-room access (e.g. via LOCKPICK). */
    public void grantBackRoomAccess() { backRoomAccess = true; }

    public boolean hasBackRoomAccess() { return backRoomAccess; }

    // ── Document printing ──────────────────────────────────────────────────────

    /**
     * Attempts to print a forged document at the back-room PRINTER_PROP.
     *
     * <p>Requires BLANK_PAPER + PRINTER_INK. Printing outside the back room
     * in Asif's sight triggers an ALERT.
     *
     * @param type           document type to print
     * @param inventory      player inventory
     * @param inBackRoom     true if the player is in the back room
     * @param asifWatching   true if Asif has line-of-sight to the printer
     * @param hamzaWatching  true if Hamza is watching (25% detection)
     * @return print result
     */
    public PrintResult printDocument(DocumentType type, Inventory inventory,
                                     boolean inBackRoom, boolean asifWatching,
                                     boolean hamzaWatching) {
        // Check printing in plain sight
        if (!inBackRoom && asifWatching) {
            if (asif != null) {
                asif.setState(NPCState.AGGRESSIVE);
                asif.setSpeechText("Oi — what are you printing?", 5f);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(PRINTING_CAUGHT_WANTED_STARS,
                        0f, 0f, 0f, null);
            }
            if (noiseSystem != null) {
                noiseSystem.addNoise(1.0f);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.DOCUMENT_FRAUD);
            }
            return PrintResult.CAUGHT_BY_ASIF;
        }

        if (!inBackRoom && hamzaWatching && random.nextFloat() < HAMZA_DETECTION_CHANCE) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.DOCUMENT_FRAUD);
            }
            return PrintResult.CAUGHT_BY_HAMZA;
        }

        if (!inBackRoom && !asifWatching) {
            // Allow printing in main room if Asif is not watching
        } else if (!inBackRoom) {
            // Asif is watching - already handled above
        }

        // Check materials
        if (inventory.getItemCount(Material.BLANK_PAPER) < 1
                || inventory.getItemCount(Material.PRINTER_INK) < 1) {
            return PrintResult.MISSING_MATERIALS;
        }

        inventory.removeItem(Material.BLANK_PAPER, 1);
        inventory.removeItem(Material.PRINTER_INK, 1);
        inventory.addItem(type.getMaterial(), 1);

        // Emit printer noise
        if (noiseSystem != null) {
            noiseSystem.addNoise(1.0f);
        }

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.DOCUMENT_FRAUD);
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.FORGER);
        }

        return PrintResult.PRINTED;
    }

    /**
     * Convenience overload — prints a document only requiring back-room check.
     * Asif and Hamza watching defaults to {@code false}.
     *
     * @param type      document type
     * @param inventory player inventory
     * @return print result
     */
    public PrintResult printDocument(DocumentType type, Inventory inventory) {
        return printDocument(type, inventory, backRoomAccess, false, false);
    }

    // ── Phishing scam ──────────────────────────────────────────────────────────

    /**
     * Runs a phishing scam session and awards COIN to the player.
     *
     * @param inventory  player inventory
     * @param tradingXP  current TRADING XP (for bonus calculation)
     * @return total COIN earned (also added to inventory)
     */
    public int runPhishingSession(Inventory inventory, int tradingXP) {
        if (!sessionActive) return 0;

        int base = PHISHING_MIN_COIN
                + random.nextInt(PHISHING_MAX_COIN - PHISHING_MIN_COIN + 1);
        int bonus = (tradingXP >= PHISHING_TRADING_XP_BONUS_THRESHOLD)
                ? PHISHING_TRADING_XP_BONUS : 0;
        int total = base + bonus;

        inventory.addItem(Material.COIN, total);

        // CYBER_FRAUD risk
        if (notorietySystem != null
                && notorietySystem.getNotoriety() >= CYBER_FRAUD_NOTORIETY_THRESHOLD
                && random.nextFloat() < CYBER_FRAUD_CHANCE) {
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.CYBER_FRAUD);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
            }
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.NIGERIAN_PRINCE);
        }

        return total;
    }

    /**
     * Runs a phishing session returning only the earned amount (no inventory side-effects).
     * Used for unit testing.
     *
     * @param randomSeed seed for deterministic testing
     * @param tradingXP  current TRADING XP
     * @return coin amount in [PHISHING_MIN_COIN, PHISHING_MAX_COIN + PHISHING_TRADING_XP_BONUS]
     */
    public int runPhishingSession(long randomSeed, int tradingXP) {
        Random r = new Random(randomSeed);
        int base = PHISHING_MIN_COIN + r.nextInt(PHISHING_MAX_COIN - PHISHING_MIN_COIN + 1);
        int bonus = (tradingXP >= PHISHING_TRADING_XP_BONUS_THRESHOLD)
                ? PHISHING_TRADING_XP_BONUS : 0;
        return base + bonus;
    }

    // ── Remote UC sign-on ──────────────────────────────────────────────────────

    /**
     * Signs on to Universal Credit remotely via the terminal, satisfying the
     * weekly JobCentre requirement without a physical visit.
     *
     * @param record     the player's JobCentre record
     * @param timeSystem current time system (provides current day)
     * @return sign-on result
     */
    public SignOnResult signOnRemotely(JobCentreRecord record, TimeSystem timeSystem) {
        if (!sessionActive) return SignOnResult.NO_SESSION;
        if (record == null) return SignOnResult.NO_ACTIVE_CLAIM;
        record.recordSignOn(timeSystem.getDayCount());
        return SignOnResult.SIGNED_ON;
    }

    // ── Pirate radio scheduling ────────────────────────────────────────────────

    /**
     * Schedules a pirate radio broadcast {@code hoursFromNow} in-game hours ahead.
     *
     * @param pirateRadioSystem the pirate radio system to schedule for
     * @param hoursFromNow      hours ahead to schedule (1–4)
     * @param timeSystem        current time system
     * @return schedule result
     */
    public ScheduleResult scheduleRadioBroadcast(PirateRadioSystem pirateRadioSystem,
                                                  float hoursFromNow,
                                                  TimeSystem timeSystem) {
        if (!sessionActive) return ScheduleResult.NO_SESSION;
        if (pirateRadioSystem == null) return ScheduleResult.NO_TRANSMITTER;
        if (!pirateRadioSystem.isTransmitterPlaced()) return ScheduleResult.NO_TRANSMITTER;

        scheduledBroadcastHour = timeSystem.getHours() + hoursFromNow;
        broadcastScheduled = true;
        this.pirateRadioSystem = pirateRadioSystem;
        return ScheduleResult.SCHEDULED;
    }

    /**
     * Called each update tick; fires scheduled broadcast when the scheduled hour arrives.
     *
     * @param currentHour current in-game hour
     */
    public void updateScheduledBroadcast(float currentHour) {
        if (broadcastScheduled && pirateRadioSystem != null
                && currentHour >= scheduledBroadcastHour) {
            pirateRadioSystem.startBroadcast();
            broadcastScheduled = false;
            scheduledBroadcastHour = -1f;
        }
    }

    public boolean isBroadcastScheduled() { return broadcastScheduled; }
    public float getScheduledBroadcastHour() { return scheduledBroadcastHour; }

    // ── Burner phone top-up ────────────────────────────────────────────────────

    /**
     * Tops up the player's BURNER_PHONE by {@code BURNER_TOP_UP_UNITS} units
     * for 1 COIN at the PREPAID_CARD_READER_PROP.
     *
     * @param inventory player inventory
     * @return top-up result
     */
    public BurnerTopUpResult topUpBurnerPhone(Inventory inventory) {
        if (inventory.getItemCount(Material.BURNER_PHONE) < 1) {
            return BurnerTopUpResult.NO_BURNER_PHONE;
        }
        if (inventory.getItemCount(Material.COIN) < BURNER_TOP_UP_COST) {
            return BurnerTopUpResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, BURNER_TOP_UP_COST);
        burnerPhoneUnits += BURNER_TOP_UP_UNITS;
        return BurnerTopUpResult.SUCCESS;
    }

    /**
     * Consumes one unit from the burner phone (e.g. for anonymous tip-off).
     *
     * @return true if a unit was consumed; false if no units remain
     */
    public boolean consumeBurnerUnit() {
        if (burnerPhoneUnits <= 0) return false;
        burnerPhoneUnits--;
        return true;
    }

    public int getBurnerPhoneUnits() { return burnerPhoneUnits; }

    // ── Getters ────────────────────────────────────────────────────────────────

    public NPC getAsif()  { return asif; }
    public NPC getHamza() { return hamza; }
}

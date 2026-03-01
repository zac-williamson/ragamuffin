package ragamuffin.core;

import ragamuffin.ai.GangTerritorySystem;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1075: Khan's Off-Licence — After-Hours Booze, Marchetti Muscle &amp; the Loyalty Card Hustle.
 *
 * <p>Khan's Off-Licence ({@code LandmarkType.OFF_LICENCE}) is the Marchetti Crew's primary turf
 * landmark. Imran Khan runs the counter; Declan enforces the door in the evenings.
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Regular hours: 08:00–23:00 daily.</li>
 *   <li>After-hours back door (23:00–02:00): Marchetti Respect ≥ {@link #BACK_DOOR_RESPECT_MIN},
 *       items at 2× price.</li>
 * </ul>
 *
 * <h3>Stock</h3>
 * <ul>
 *   <li>{@link Material#CAN_OF_LAGER} — 2 COIN</li>
 *   <li>{@link Material#CHEAP_SPIRITS} — 3 COIN</li>
 *   <li>{@link Material#CIGARETTE} — 1 COIN</li>
 *   <li>{@link Material#TOBACCO_POUCH} — 2 COIN</li>
 *   <li>{@link Material#SCRATCH_CARD} — 1 COIN</li>
 *   <li>{@link Material#ENERGY_DRINK} — 2 COIN</li>
 *   <li>{@link Material#NEWSPAPER} — 1 COIN</li>
 *   <li>{@link Material#LIGHTER} — 1 COIN</li>
 *   <li>{@link Material#PLASTIC_BAG} — 0 COIN (free with purchase)</li>
 * </ul>
 *
 * <h3>Loyalty Card</h3>
 * <ul>
 *   <li>5 stamps → free {@link Material#CAN_OF_LAGER}</li>
 *   <li>10 stamps → free {@link Material#CHEAP_SPIRITS}</li>
 *   <li>20 lifetime stamps → {@link AchievementType#CORNER_SHOP_REGULAR}</li>
 * </ul>
 *
 * <h3>Shoplifting Detection</h3>
 * <ul>
 *   <li>Imran within {@link #IMRAN_DETECT_RANGE}: 70% base detection.</li>
 *   <li>Declan in exit sight-line: +20%.</li>
 *   <li>CCTV active: Imran effective detection raised.</li>
 *   <li>BALACLAVA worn: immediate detection.</li>
 *   <li>Third detection: permanent ban.</li>
 * </ul>
 *
 * <h3>Turf War</h3>
 * <ul>
 *   <li>Street Lads hold → Declan replaced, Marchetti discount reversed.</li>
 *   <li>Council hold → COUNCIL_NOTICE prop, full price for all.</li>
 * </ul>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link FactionSystem} — Marchetti Crew respect gates discounts, envelope, back-door.</li>
 *   <li>{@link GangTerritorySystem} — Primary Marchetti turf node.</li>
 *   <li>{@link NotorietySystem} — Shoplifting triggers Notoriety +3.</li>
 *   <li>{@link CriminalRecord} — THEFT on shoplifting detection.</li>
 *   <li>{@link RumourNetwork} — seeds GANG_ACTIVITY, ANTISOCIAL_BEHAVIOUR, QUEUE_JUMP, BIG_WIN_AT_BOOKIES.</li>
 *   <li>{@link AchievementSystem} — 5 new achievements.</li>
 *   <li>{@link CornerShopSystem} — price war enforcement.</li>
 * </ul>
 */
public class OffLicenceSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the shop opens for regular business. */
    public static final float OPEN_HOUR = 8.0f;

    /** Hour the shop closes for regular business. */
    public static final float CLOSE_HOUR = 23.0f;

    /** Start of after-hours back-door window. */
    public static final float BACK_DOOR_START_HOUR = 23.0f;

    /** End of after-hours back-door window (wraps past midnight). */
    public static final float BACK_DOOR_END_HOUR = 2.0f;

    // ── Stock prices ──────────────────────────────────────────────────────────

    /** Base price of a Can of Lager. */
    public static final int PRICE_CAN_OF_LAGER = 2;

    /** Base price of Cheap Spirits. */
    public static final int PRICE_CHEAP_SPIRITS = 3;

    /** Base price of a Cigarette. */
    public static final int PRICE_CIGARETTE = 1;

    /** Base price of a Tobacco Pouch. */
    public static final int PRICE_TOBACCO_POUCH = 2;

    /** Base price of a Scratch Card. */
    public static final int PRICE_SCRATCH_CARD = 1;

    /** Base price of an Energy Drink. */
    public static final int PRICE_ENERGY_DRINK = 2;

    /** Base price of a Newspaper. */
    public static final int PRICE_NEWSPAPER = 1;

    /** Base price of a Lighter. */
    public static final int PRICE_LIGHTER = 1;

    /** Price of a Plastic Bag (free). */
    public static final int PRICE_PLASTIC_BAG = 0;

    /** After-hours price multiplier (back-door knock). */
    public static final int AFTER_HOURS_PRICE_MULTIPLIER = 2;

    // ── Faction / access thresholds ───────────────────────────────────────────

    /** Marchetti Crew respect required for 10% discount. */
    public static final int MARCHETTI_DISCOUNT_RESPECT = 40;

    /** Marchetti Crew respect required for envelope mission trigger. */
    public static final int MARCHETTI_ENVELOPE_RESPECT = 70;

    /** Marchetti Crew respect required for after-hours back-door entry. */
    public static final int BACK_DOOR_RESPECT_MIN = 60;

    /** Notoriety threshold above which Imran refuses service. */
    public static final int NOTORIETY_REFUSAL_THRESHOLD = 80;

    /** Notoriety tier at which Declan blocks entry (unless Marchetti respect met). */
    public static final int DECLAN_BLOCK_NOTORIETY_TIER = 3;

    /** Marchetti respect required to bypass Declan's Tier-3 block. */
    public static final int DECLAN_BYPASS_RESPECT = 50;

    /** Seconds Declan waits before turning hostile if player loiters without buying. */
    public static final float DECLAN_LOITER_HOSTILE_SECONDS = 15.0f;

    /** Declan start hour. */
    public static final float DECLAN_START_HOUR = 17.0f;

    /** Declan end hour. */
    public static final float DECLAN_END_HOUR = 23.0f;

    /** Marchetti crew inside peak start hour. */
    public static final float CREW_PEAK_START = 18.0f;

    /** Marchetti crew inside peak end hour. */
    public static final float CREW_PEAK_END = 22.0f;

    // ── Loyalty card ─────────────────────────────────────────────────────────

    /** Stamps required for free lager reward. */
    public static final int LOYALTY_FREE_LAGER_STAMPS = 5;

    /** Stamps required for free spirits reward. */
    public static final int LOYALTY_FREE_SPIRITS_STAMPS = 10;

    /** Lifetime stamps required for CORNER_SHOP_REGULAR achievement. */
    public static final int LOYALTY_ACHIEVEMENT_STAMPS = 20;

    // ── Shoplifting detection ─────────────────────────────────────────────────

    /** Imran detection range in blocks. */
    public static final float IMRAN_DETECT_RANGE = 4.0f;

    /** Base probability of detection when Imran is within range. */
    public static final float SHOPLIFT_IMRAN_BASE_PROB = 0.70f;

    /** Additional detection probability when Declan is in exit sight-line. */
    public static final float SHOPLIFT_DECLAN_BONUS = 0.20f;

    /** Detection probability reduction when CCTV is broken. */
    public static final float SHOPLIFT_CCTV_BROKEN_REDUCTION = 0.40f;

    /** Detection number of times before permanent ban. */
    public static final int SHOPLIFT_BAN_THRESHOLD = 3;

    /** Notoriety gained on shoplifting detection. */
    public static final int SHOPLIFT_NOTORIETY_GAIN = 3;

    // ── Random events ─────────────────────────────────────────────────────────

    /** In-game seconds between random event checks (2 in-game hours). */
    public static final float EVENT_INTERVAL_SECONDS = 120.0f;

    /** Day-of-week index for Friday (0=Monday by convention). */
    public static final int DAY_FRIDAY = 4;

    /** Day-of-week index for Saturday. */
    public static final int DAY_SATURDAY = 5;

    /** Day-of-week index for Sunday. */
    public static final int DAY_SUNDAY = 6;

    /** Hour at which the Marchetti meeting starts on Sundays. */
    public static final float MARCHETTI_MEETING_HOUR = 21.0f;

    /** Duration (in-game seconds) of the Marchetti meeting. */
    public static final float MARCHETTI_MEETING_DURATION = 20.0f * 60.0f;

    /** Distance (blocks) within which player overhears the Marchetti meeting. */
    public static final float MARCHETTI_MEETING_OVERHEAR_RANGE = 3.0f;

    /** Notoriety gained from queue-jumping. */
    public static final int QUEUE_JUMP_NOTORIETY = 1;

    /** Notoriety gained from selling alcohol to underage NPC. */
    public static final int UNDERAGE_SALE_NOTORIETY = 2;

    // ── Price war ─────────────────────────────────────────────────────────────

    /** In-game seconds until Declan responds to price undercutting (~2 in-game hours). */
    public static final float PRICE_WAR_RESPONSE_SECONDS = 120.0f;

    // ── Turf war ─────────────────────────────────────────────────────────────

    /** Territory holder enum for OFF_LICENCE node. */
    public enum TurfHolder {
        MARCHETTI,
        STREET_LADS,
        COUNCIL,
        NEUTRAL
    }

    // ── Entry check result ────────────────────────────────────────────────────

    /** Result of Declan's entry check. */
    public enum EntryCheckResult {
        /** Player is allowed to enter. */
        ALLOWED,
        /** Declan blocks entry. */
        BLOCKED
    }

    // ── Purchase result ───────────────────────────────────────────────────────

    /** Result of a purchase attempt. */
    public enum PurchaseResult {
        SUCCESS,
        CLOSED,
        INSUFFICIENT_FUNDS,
        SERVICE_REFUSED,
        NOT_IN_STOCK
    }

    // ── Back door result ──────────────────────────────────────────────────────

    /** Result of knocking the back door. */
    public enum BackDoorResult {
        /** Imran answers; items available at 2× price. */
        OPEN,
        /** Not in the after-hours window. */
        WRONG_TIME,
        /** Insufficient Marchetti respect. */
        INSUFFICIENT_RESPECT,
        /** Player is drunk; Imran refuses. */
        DRUNK_REFUSED
    }

    // ── MenuItem ──────────────────────────────────────────────────────────────

    /** A single stock item with its base price. */
    public static class MenuItem {
        public final Material material;
        public final int price;

        public MenuItem(Material material, int price) {
            this.material = material;
            this.price    = price;
        }
    }

    /** Full stock. */
    public static final MenuItem[] STOCK = {
        new MenuItem(Material.CAN_OF_LAGER,    PRICE_CAN_OF_LAGER),
        new MenuItem(Material.CHEAP_SPIRITS,   PRICE_CHEAP_SPIRITS),
        new MenuItem(Material.CIGARETTE,       PRICE_CIGARETTE),
        new MenuItem(Material.TOBACCO_POUCH,   PRICE_TOBACCO_POUCH),
        new MenuItem(Material.SCRATCH_CARD,    PRICE_SCRATCH_CARD),
        new MenuItem(Material.ENERGY_DRINK,    PRICE_ENERGY_DRINK),
        new MenuItem(Material.NEWSPAPER,       PRICE_NEWSPAPER),
        new MenuItem(Material.LIGHTER,         PRICE_LIGHTER),
        new MenuItem(Material.PLASTIC_BAG,     PRICE_PLASTIC_BAG),
    };

    // ── State ─────────────────────────────────────────────────────────────────

    /** Imran Khan NPC reference (set by world/NPC manager). */
    private NPC imran;

    /** Declan NPC reference (set by world/NPC manager). */
    private NPC declan;

    /** Marchetti crew NPCs present during peak hours. */
    private final List<NPC> marchettCrew = new ArrayList<>();

    /** Current turf holder of the OFF_LICENCE node. */
    private TurfHolder turfHolder = TurfHolder.MARCHETTI;

    /** Total lifetime loyalty stamps earned. */
    private int lifetimeLoyaltyStamps = 0;

    /** Accumulated stamps since last reward. */
    private int currentStamps = 0;

    /** Queued free item from loyalty reward; null if no reward pending. */
    private Material pendingFreeItem = null;

    /** Number of times shoplifting has been detected. */
    private int shopliftDetections = 0;

    /** Whether the player is permanently banned. */
    private boolean permanentlyBanned = false;

    /** Whether CCTV is active (default true; can be broken by player). */
    private boolean cctvActive = true;

    /** Number of back-door knocks (for BACK_DOOR_BOY achievement). */
    private int backDoorKnocks = 0;

    /** Number of Marchetti envelopes accepted (for MARCHETTI_ERRAND_BOY). */
    private int marchettEnvelopesAccepted = 0;

    /** Whether the envelope was already handed out today. */
    private boolean envelopeHandedToday = false;

    /** Timer for random events. */
    private float eventTimer = 0f;

    /** Timer for price-war enforcement response. */
    private float priceWarTimer = -1f;

    /** Whether price war is currently active. */
    private boolean priceWarActive = false;

    /** Timer tracking how long player has loitered near Declan without buying. */
    private float declanLoiterTimer = 0f;

    /** Whether the FIVE_FINGER_DISCOUNT achievement has been unlocked. */
    private boolean achievementFiveFingerDiscount = false;

    /** Whether the BACK_DOOR_BOY achievement has been unlocked. */
    private boolean achievementBackDoorBoy = false;

    /** Whether the MARCHETTI_ERRAND_BOY achievement has been unlocked. */
    private boolean achievementMarchettiErrandBoy = false;

    /** Whether the UNDERAGE_ENABLER achievement has been unlocked. */
    private boolean achievementUnderageEnabler = false;

    /** Whether the CORNER_SHOP_REGULAR achievement has been unlocked. */
    private boolean achievementCornerShopRegular = false;

    /** Whether a Marchetti meeting is currently happening. */
    private boolean marchettMeetingActive = false;

    /** How much time remains in the Marchetti meeting (seconds). */
    private float marchettMeetingTimer = 0f;

    private final Random random;

    // ── Injected systems ──────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private CornerShopSystem cornerShopSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public OffLicenceSystem() {
        this(new Random());
    }

    public OffLicenceSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ──────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) { this.notorietySystem = n; }
    public void setFactionSystem(FactionSystem f)     { this.factionSystem = f; }
    public void setRumourNetwork(RumourNetwork r)     { this.rumourNetwork = r; }
    public void setCriminalRecord(CriminalRecord c)   { this.criminalRecord = c; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }
    public void setCornerShopSystem(CornerShopSystem cs)  { this.cornerShopSystem = cs; }

    // ── NPC setters ───────────────────────────────────────────────────────────

    public void setImran(NPC imran) { this.imran = imran; }
    public void setDeclan(NPC declan) { this.declan = declan; }
    public void addMarchettiCrew(NPC npc) { marchettCrew.add(npc); }
    public void clearMarchettiCrew() { marchettCrew.clear(); }

    // ── Turf state ────────────────────────────────────────────────────────────

    public void setTurfHolder(TurfHolder holder) { this.turfHolder = holder; }
    public TurfHolder getTurfHolder() { return turfHolder; }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns true if the shop is open at the given hour.
     *
     * @param hour in-game hour (0–24)
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if Declan is on duty at the given hour.
     */
    public boolean isDeclanOnDuty(float hour) {
        return hour >= DECLAN_START_HOUR && hour < DECLAN_END_HOUR;
    }

    /**
     * Returns true if the after-hours back door is accessible.
     *
     * @param marchettRespect the player's Marchetti Crew respect (0–100)
     * @param hour            current in-game hour
     */
    public boolean isBackDoorAccessible(int marchettRespect, float hour) {
        if (marchettRespect < BACK_DOOR_RESPECT_MIN) return false;
        // Window: 23:00–24:00 or 00:00–02:00
        return hour >= BACK_DOOR_START_HOUR || hour < BACK_DOOR_END_HOUR;
    }

    // ── Entry check ───────────────────────────────────────────────────────────

    /**
     * Check whether Declan allows the player to enter the shop.
     *
     * <p>Returns {@link EntryCheckResult#BLOCKED} when:
     * <ul>
     *   <li>The player's notoriety tier is ≥ {@link #DECLAN_BLOCK_NOTORIETY_TIER}, AND</li>
     *   <li>The player's Marchetti respect is &lt; {@link #DECLAN_BYPASS_RESPECT}.</li>
     * </ul>
     *
     * @param notoriety      current player notoriety (raw value, not tier)
     * @param marchettRespect player's Marchetti Crew respect
     */
    public EntryCheckResult checkDeclanEntry(int notoriety, int marchettRespect) {
        int tier = computeNotorietyTier(notoriety);
        if (tier >= DECLAN_BLOCK_NOTORIETY_TIER && marchettRespect < DECLAN_BYPASS_RESPECT) {
            return EntryCheckResult.BLOCKED;
        }
        return EntryCheckResult.ALLOWED;
    }

    /** Compute notoriety tier from raw notoriety value, mirroring {@link NotorietySystem} thresholds. */
    private static int computeNotorietyTier(int notoriety) {
        if (notoriety >= NotorietySystem.TIER_5_THRESHOLD) return 5;
        if (notoriety >= NotorietySystem.TIER_4_THRESHOLD) return 4;
        if (notoriety >= NotorietySystem.TIER_3_THRESHOLD) return 3;
        if (notoriety >= NotorietySystem.TIER_2_THRESHOLD) return 2;
        if (notoriety >= NotorietySystem.TIER_1_THRESHOLD) return 1;
        return 0;
    }

    // ── Stock lookup ──────────────────────────────────────────────────────────

    /**
     * Find a stock entry by material type.
     *
     * @param material the item to look up
     * @return the {@link MenuItem} or {@code null} if not sold here
     */
    public MenuItem findMenuItem(Material material) {
        for (MenuItem item : STOCK) {
            if (item.material == material) return item;
        }
        return null;
    }

    // ── Purchase ──────────────────────────────────────────────────────────────

    /**
     * Attempt to buy an item from Imran.
     *
     * @param material         item to buy
     * @param playerInventory  player's inventory (COIN is consumed)
     * @param currentHour      current in-game hour
     * @param marchettRespect  player's Marchetti Crew respect (for discount)
     * @param playerNotoriety  player's raw notoriety
     * @param callback         achievement callback
     * @return purchase result
     */
    public PurchaseResult buyItem(Material material,
                                   Inventory playerInventory,
                                   float currentHour,
                                   int marchettRespect,
                                   int playerNotoriety,
                                   NotorietySystem.AchievementCallback callback) {
        if (!isOpen(currentHour)) return PurchaseResult.CLOSED;
        if (permanentlyBanned) return PurchaseResult.SERVICE_REFUSED;
        if (playerNotoriety >= NOTORIETY_REFUSAL_THRESHOLD) return PurchaseResult.SERVICE_REFUSED;

        MenuItem item = findMenuItem(material);
        if (item == null) return PurchaseResult.NOT_IN_STOCK;

        int price = item.price;
        // Marchetti discount
        if (turfHolder == TurfHolder.MARCHETTI && marchettRespect >= MARCHETTI_DISCOUNT_RESPECT) {
            price = Math.max(0, (int)(price * 0.9f));
        } else if (turfHolder == TurfHolder.STREET_LADS) {
            // Street Lads hold: Street Lads respect (passed as marchettRespect param for simplicity) grants discount
            // Marchetti players pay full price
            // No discount for Marchetti respect holders
            price = item.price;
        }

        // Free item (PLASTIC_BAG) always costs 0
        if (item.price == 0) price = 0;

        if (playerInventory.getItemCount(Material.COIN) < price) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        if (price > 0) {
            playerInventory.removeItem(Material.COIN, price);
        }
        playerInventory.addItem(material, 1);

        // Stamp loyalty card (exclude free items)
        if (item.price > 0) {
            stampLoyaltyCard(playerInventory, callback);
        }

        // Free plastic bag with every purchase
        if (material != Material.PLASTIC_BAG) {
            playerInventory.addItem(Material.PLASTIC_BAG, 1);
        }

        return PurchaseResult.SUCCESS;
    }

    // ── Loyalty card ─────────────────────────────────────────────────────────

    /**
     * Record one loyalty stamp and check for rewards.
     */
    private void stampLoyaltyCard(Inventory playerInventory,
                                   NotorietySystem.AchievementCallback callback) {
        currentStamps++;
        lifetimeLoyaltyStamps++;

        // Reward at 5 stamps
        if (currentStamps == LOYALTY_FREE_LAGER_STAMPS) {
            pendingFreeItem = Material.CAN_OF_LAGER;
        }
        // Reward at 10 stamps
        if (currentStamps == LOYALTY_FREE_SPIRITS_STAMPS) {
            pendingFreeItem = Material.CHEAP_SPIRITS;
        }

        // Achievement at 20 lifetime stamps
        if (lifetimeLoyaltyStamps >= LOYALTY_ACHIEVEMENT_STAMPS
                && !achievementCornerShopRegular) {
            achievementCornerShopRegular = true;
            award(AchievementType.CORNER_SHOP_REGULAR, callback);
        }
    }

    /**
     * Poll for a pending free item reward.
     *
     * <p>Returns the reward material (and adds it to inventory) if available,
     * otherwise returns {@code null}.
     *
     * @param playerInventory player's inventory to add the item into
     * @return the free item granted, or {@code null} if none pending
     */
    public Material pollFreeItem(Inventory playerInventory) {
        if (pendingFreeItem == null) return null;
        Material reward = pendingFreeItem;
        pendingFreeItem = null;
        playerInventory.addItem(reward, 1);
        return reward;
    }

    /**
     * Returns the current number of loyalty stamps (since last reward tier).
     */
    public int getLoyaltyStamps() {
        return currentStamps;
    }

    /**
     * Returns the total lifetime loyalty stamps.
     */
    public int getLifetimeLoyaltyStamps() {
        return lifetimeLoyaltyStamps;
    }

    // ── Shoplifting ───────────────────────────────────────────────────────────

    /**
     * Determine whether shoplifting is detected.
     *
     * @param imranInRange whether Imran is within {@link #IMRAN_DETECT_RANGE}
     * @param balaclavaWorn whether the player is wearing a BALACLAVA (always detected)
     * @param cctvWorking   whether CCTV is still operational
     * @return true if detected
     */
    public boolean detectShoplift(boolean imranInRange, boolean balaclavaWorn, boolean cctvWorking) {
        if (balaclavaWorn) return true;

        float prob = 0f;
        if (imranInRange) {
            prob += SHOPLIFT_IMRAN_BASE_PROB;
            if (!cctvWorking) {
                prob -= SHOPLIFT_CCTV_BROKEN_REDUCTION;
            }
        }
        if (declan != null && declan.isAlive()) {
            prob += SHOPLIFT_DECLAN_BONUS;
        }
        prob = Math.max(0f, Math.min(1f, prob));
        return random.nextFloat() < prob;
    }

    /**
     * Detect shoplifting using the current system state (Imran/Declan NPC positions vs player).
     *
     * @param player   the player
     * @param imranNpc Imran NPC (may be null)
     * @param declanNpc Declan NPC (may be null)
     * @return true if detected
     */
    public boolean checkShopliftDetection(Player player, NPC imranNpc, NPC declanNpc) {
        boolean balaclavaWorn = false; // Simplified: caller sets directly via detectShoplift overload
        boolean imranInRange = false;
        if (imranNpc != null && imranNpc.isAlive()) {
            float dx = player.getPosition().x - imranNpc.getPosition().x;
            float dz = player.getPosition().z - imranNpc.getPosition().z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            imranInRange = dist <= IMRAN_DETECT_RANGE;
        }
        boolean declanVisible = declanNpc != null && declanNpc.isAlive();
        float prob = 0f;
        if (imranInRange) {
            prob += SHOPLIFT_IMRAN_BASE_PROB;
            if (!cctvActive) {
                prob -= SHOPLIFT_CCTV_BROKEN_REDUCTION;
            }
        }
        if (declanVisible) {
            prob += SHOPLIFT_DECLAN_BONUS;
        }
        prob = Math.max(0f, Math.min(1f, prob));
        return random.nextFloat() < prob;
    }

    /**
     * Trigger consequences of shoplifting detection.
     *
     * @param callback achievement callback
     */
    public void onShopliftDetected(NotorietySystem.AchievementCallback callback) {
        shopliftDetections++;
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SHOPLIFT_NOTORIETY_GAIN, callback);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.THEFT);
        }
        if (imran != null) {
            imran.setSpeechText("Oi! Put that back!", 3f);
        }
        if (declan != null && declan.isAlive()) {
            declan.setState(NPCState.CHASING);
        }
        if (shopliftDetections >= SHOPLIFT_BAN_THRESHOLD) {
            permanentlyBanned = true;
            if (imran != null) {
                imran.setSpeechText("Don't come back here.", 5f);
            }
        }
    }

    /**
     * Called when player successfully shoplifts without detection.
     *
     * @param callback achievement callback
     */
    public void onShopliftUndetected(NotorietySystem.AchievementCallback callback) {
        if (!achievementFiveFingerDiscount) {
            achievementFiveFingerDiscount = true;
            award(AchievementType.FIVE_FINGER_DISCOUNT, callback);
        }
    }

    // ── After-hours back door knock ───────────────────────────────────────────

    /**
     * Attempt to use the after-hours back door.
     *
     * @param marchettRespect  player's Marchetti respect
     * @param currentHour      current in-game hour
     * @param playerIsDrunk    whether the player has the DRUNK status effect
     * @param playerInventory  player's inventory
     * @param callback         achievement callback
     * @return result of the knock
     */
    public BackDoorResult knockBackDoor(int marchettRespect,
                                         float currentHour,
                                         boolean playerIsDrunk,
                                         Inventory playerInventory,
                                         NotorietySystem.AchievementCallback callback) {
        if (!isBackDoorAccessible(marchettRespect, currentHour)) {
            if (marchettRespect < BACK_DOOR_RESPECT_MIN) {
                return BackDoorResult.INSUFFICIENT_RESPECT;
            }
            return BackDoorResult.WRONG_TIME;
        }
        if (playerIsDrunk) {
            if (imran != null) {
                imran.setSpeechText("Come back when you're sober, yeah?", 4f);
            }
            return BackDoorResult.DRUNK_REFUSED;
        }
        backDoorKnocks++;
        if (imran != null) {
            imran.setSpeechText("Quick, round the back. Two minutes.", 4f);
        }
        if (backDoorKnocks >= 3 && !achievementBackDoorBoy) {
            achievementBackDoorBoy = true;
            award(AchievementType.BACK_DOOR_BOY, callback);
        }
        return BackDoorResult.OPEN;
    }

    /**
     * Get the effective price of an item during after-hours back-door session.
     */
    public int getAfterHoursPrice(Material material) {
        MenuItem item = findMenuItem(material);
        if (item == null) return -1;
        return item.price * AFTER_HOURS_PRICE_MULTIPLIER;
    }

    // ── Marchetti envelope ────────────────────────────────────────────────────

    /**
     * Attempt to accept a Marchetti envelope from Imran.
     *
     * @param currentHour     current in-game hour
     * @param marchettRespect  player's Marchetti respect
     * @param hasPendingMission whether the player already has an active faction mission
     * @param callback         achievement callback
     * @return true if envelope was accepted
     */
    public boolean tryAcceptEnvelope(float currentHour,
                                      int marchettRespect,
                                      boolean hasPendingMission,
                                      NotorietySystem.AchievementCallback callback) {
        if (currentHour < 20.0f || marchettRespect < MARCHETTI_ENVELOPE_RESPECT) return false;
        if (envelopeHandedToday) {
            if (imran != null) imran.setSpeechText("Later, yeah?", 3f);
            return false;
        }
        if (hasPendingMission) {
            if (imran != null) imran.setSpeechText("Sort the other business first, yeah?", 4f);
            return false;
        }
        envelopeHandedToday = true;
        marchettEnvelopesAccepted++;
        if (imran != null) {
            imran.setSpeechText("Take this. Don't open it here.", 4f);
        }
        if (marchettEnvelopesAccepted >= 5 && !achievementMarchettiErrandBoy) {
            achievementMarchettiErrandBoy = true;
            award(AchievementType.MARCHETTI_ERRAND_BOY, callback);
        }
        return true;
    }

    // ── Underage sale ─────────────────────────────────────────────────────────

    /**
     * Player hands alcohol to a SCHOOL_KID NPC.
     *
     * @param callback achievement callback
     */
    public void onUnderageSale(List<NPC> npcs, NotorietySystem.AchievementCallback callback) {
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(UNDERAGE_SALE_NOTORIETY, callback);
        }
        if (rumourNetwork != null && npcs != null) {
            for (NPC npc : npcs) {
                if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.ANTISOCIAL_BEHAVIOUR,
                            "Someone sold booze to a kid outside Khan's."));
                    break;
                }
            }
        }
        if (!achievementUnderageEnabler) {
            achievementUnderageEnabler = true;
            award(AchievementType.UNDERAGE_ENABLER, callback);
        }
    }

    // ── Price war ─────────────────────────────────────────────────────────────

    /**
     * Signal that the player's corner shop is undercutting Khan's lager price.
     * Declan will visit within {@link #PRICE_WAR_RESPONSE_SECONDS}.
     */
    public void onLagerPriceUndercut() {
        if (!priceWarActive) {
            priceWarActive = true;
            priceWarTimer = 0f;
        }
    }

    // ── CCTV ─────────────────────────────────────────────────────────────────

    /**
     * Break the CCTV camera (requires 2 hits externally, caller responsibility).
     */
    public void breakCctv() {
        cctvActive = false;
    }

    public boolean isCctvActive() { return cctvActive; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isPermanentlyBanned() { return permanentlyBanned; }
    public int getShopliftDetections() { return shopliftDetections; }
    public int getBackDoorKnocks() { return backDoorKnocks; }
    public int getMarchettiEnvelopesAccepted() { return marchettEnvelopesAccepted; }
    public NPC getImran() { return imran; }
    public NPC getDeclan() { return declan; }
    public boolean isPriceWarActive() { return priceWarActive; }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Main per-frame update.
     *
     * @param delta            seconds since last frame
     * @param currentHour      current in-game hour (0–24)
     * @param dayOfWeek        day of week (0=Monday … 6=Sunday)
     * @param npcs             all NPCs in the scene
     * @param player           the player
     * @param playerInventory  the player's inventory
     * @param callback         achievement callback
     */
    public void update(float delta,
                       float currentHour,
                       int dayOfWeek,
                       List<NPC> npcs,
                       Player player,
                       Inventory playerInventory,
                       NotorietySystem.AchievementCallback callback) {

        // Reset envelope flag on new day (simplified: reset when hour < 1)
        if (currentHour < 1.0f && envelopeHandedToday) {
            envelopeHandedToday = false;
        }

        // Turf war: reassign Declan and discount rules
        updateTurfWar(npcs);

        // Price war timer
        if (priceWarActive && priceWarTimer >= 0f) {
            priceWarTimer += delta;
            if (priceWarTimer >= PRICE_WAR_RESPONSE_SECONDS) {
                triggerPriceWarEnforcer(npcs);
                priceWarTimer = -1f;
            }
        }

        // Declan loiter check
        if (declan != null && declan.isAlive() && isDeclanOnDuty(currentHour) && player != null) {
            float dx = player.getPosition().x - declan.getPosition().x;
            float dz = player.getPosition().z - declan.getPosition().z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist <= 2.0f) {
                declanLoiterTimer += delta;
                if (declanLoiterTimer >= DECLAN_LOITER_HOSTILE_SECONDS) {
                    declan.setSpeechText("You gonna buy something or what?", 4f);
                    declan.setState(NPCState.HOSTILE);
                }
            } else {
                declanLoiterTimer = 0f;
                if (declan.getState() == NPCState.HOSTILE) {
                    declan.setState(NPCState.IDLE);
                }
            }
        }

        // Marchetti meeting on Sundays at 21:00
        if (dayOfWeek == DAY_SUNDAY && currentHour >= MARCHETTI_MEETING_HOUR
                && !marchettMeetingActive && isOpen(currentHour)) {
            marchettMeetingActive = true;
            marchettMeetingTimer = MARCHETTI_MEETING_DURATION;
        }
        if (marchettMeetingActive) {
            marchettMeetingTimer -= delta;
            if (marchettMeetingTimer <= 0f) {
                marchettMeetingActive = false;
            } else if (player != null && rumourNetwork != null) {
                // Check if player is within overhear range of Marchetti crew
                for (NPC crew : marchettCrew) {
                    if (!crew.isAlive()) continue;
                    float dx = player.getPosition().x - crew.getPosition().x;
                    float dz = player.getPosition().z - crew.getPosition().z;
                    float dist = (float) Math.sqrt(dx * dx + dz * dz);
                    if (dist <= MARCHETTI_MEETING_OVERHEAR_RANGE) {
                        rumourNetwork.addRumour(crew, new Rumour(RumourType.GANG_ACTIVITY,
                                "The Marchetti boys were having a meeting at Khan's."));
                        break;
                    }
                }
            }
        }

        // Random events
        eventTimer += delta;
        if (eventTimer >= EVENT_INTERVAL_SECONDS && isOpen(currentHour)) {
            eventTimer = 0f;
            triggerRandomEvent(currentHour, dayOfWeek, npcs, player, playerInventory, callback);
        }
    }

    // ── Turf war helpers ──────────────────────────────────────────────────────

    private void updateTurfWar(List<NPC> npcs) {
        // Turf war changes are handled externally via setTurfHolder(); this hook
        // can react to holder changes for NPC speech/state if needed.
    }

    /**
     * Apply turf war NPC changes for the given holder.
     * Called by external systems (e.g. GangTerritorySystem integration) when the turf node changes.
     *
     * @param holder  new holder
     * @param npcs    NPC list to mutate
     */
    public void applyTurfWar(TurfHolder holder, List<NPC> npcs) {
        this.turfHolder = holder;
        if (holder == TurfHolder.STREET_LADS) {
            // Declan (Marchetti enforcer) leaves; Street Lads NPC takes door
            if (declan != null && declan.isAlive()) {
                declan.setState(NPCState.FLEEING);
            }
            declan = null; // Door is now held by a Street Lads NPC (spawned externally)
        } else if (holder == TurfHolder.COUNCIL) {
            // Council hold: full price for all; no Marchetti Declan
            if (declan != null && declan.isAlive()) {
                declan.setState(NPCState.FLEEING);
            }
            declan = null;
        }
    }

    // ── Price war helper ──────────────────────────────────────────────────────

    private void triggerPriceWarEnforcer(List<NPC> npcs) {
        // Seed GANG_ACTIVITY rumour town-wide
        if (rumourNetwork != null && npcs != null) {
            for (NPC npc : npcs) {
                if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.GANG_ACTIVITY,
                            "The Marchetti boys smashed up a stall that was undercutting Khan's."));
                    break;
                }
            }
        }
        // Spawn/send Declan (or a fresh YOUTH_GANG NPC) to the player's corner shop
        // (Caller is responsible for positioning; we expose a flag)
        priceWarActive = false;
    }

    // ── Random events ─────────────────────────────────────────────────────────

    private void triggerRandomEvent(float currentHour,
                                     int dayOfWeek,
                                     List<NPC> npcs,
                                     Player player,
                                     Inventory playerInventory,
                                     NotorietySystem.AchievementCallback callback) {
        int roll = random.nextInt(5);
        switch (roll) {
            case 0: triggerQueueEvent(currentHour, dayOfWeek, npcs, callback); break;
            case 1: triggerUnderageAttemptEvent(npcs, callback);              break;
            case 2: triggerScratchCardEvent(npcs);                             break;
            case 3: triggerChangeArgumentEvent(npcs);                          break;
            case 4: // Marchetti meeting handled in update loop                 break;
            default: break;
        }
    }

    private void triggerQueueEvent(float currentHour, int dayOfWeek,
                                    List<NPC> npcs,
                                    NotorietySystem.AchievementCallback callback) {
        // Queue at door: Fri/Sat 18:00–19:00
        boolean isQueueTime = (dayOfWeek == DAY_FRIDAY || dayOfWeek == DAY_SATURDAY)
                && currentHour >= 18.0f && currentHour < 19.0f;
        if (!isQueueTime) return;
        // Seed QUEUE_JUMP rumour if player acts (simplified: seed proactively)
        if (rumourNetwork != null && npcs != null) {
            for (NPC npc : npcs) {
                if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.QUEUE_JUMP,
                            "Someone queue-jumped at Khan's on a Friday night."));
                    break;
                }
            }
        }
    }

    private void triggerUnderageAttemptEvent(List<NPC> npcs,
                                              NotorietySystem.AchievementCallback callback) {
        // SCHOOL_KID tries to buy lager; Imran refuses
        if (imran != null) {
            imran.setSpeechText("Come back when you're older, mate.", 4f);
        }
    }

    private void triggerScratchCardEvent(List<NPC> npcs) {
        // PENSIONER wins 10 COIN on a scratch card
        if (rumourNetwork != null && npcs != null) {
            for (NPC npc : npcs) {
                if (npc.isAlive() && npc.getType() == NPCType.PENSIONER) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.BIG_WIN_AT_BOOKIES,
                            "Old dear won a tenner on a scratch card at Khan's."));
                    break;
                }
            }
        }
    }

    private void triggerChangeArgumentEvent(List<NPC> npcs) {
        // Two PUBLIC NPCs argue; Declan intervenes after 10s
        if (declan != null && declan.isAlive()) {
            declan.setSpeechText("Alright, settle down.", 4f);
        }
    }

    // ── Achievement helper ────────────────────────────────────────────────────

    private void award(AchievementType type, NotorietySystem.AchievementCallback callback) {
        if (achievementSystem != null) {
            achievementSystem.unlock(type);
        }
        if (callback != null) {
            callback.award(type);
        }
    }
}

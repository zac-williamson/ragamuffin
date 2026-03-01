package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #1091: Northfield Nando's — Peri-Peri Economy, Card Machine Drama &amp; the Safe Heist.
 *
 * <p>Northfield Nando's ({@code LandmarkType.NANDOS}) is a peri-peri chicken restaurant open
 * 11:00–23:00 daily. Staffed by Kezia ({@code NANDOS_STAFF} NPC) at the counter and
 * Dave the Manager ({@code NANDOS_MANAGER} NPC) in the office 12:00–22:00.
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Open: 11:00–23:00 daily.</li>
 * </ul>
 *
 * <h3>Menu (NandosOrderUI)</h3>
 * <ul>
 *   <li>Quarter Chicken — 4 COIN. Hunger +30. Heat: player-selectable tier.</li>
 *   <li>Half Chicken — 7 COIN. Hunger +50. Heat: player-selectable tier.</li>
 *   <li>Whole Chicken — 11 COIN. Hunger +80. Heat: player-selectable tier.</li>
 *   <li>{@link Material#CHICKEN_WRAP} — 5 COIN. Hunger +35.</li>
 *   <li>{@link Material#BOTTOMLESS_DRINK} — 2 COIN. Thirst +20.</li>
 *   <li>{@link Material#PERI_CHIPS} — 3 COIN. Hunger +25.</li>
 *   <li>{@link Material#PERI_PERI_SAUCE} — 2 COIN. Throwable; creates PERI_SAUCE_SLICK.</li>
 * </ul>
 *
 * <h3>Peri-Peri Heat Scale</h3>
 * Five tiers (LEMON_HERB → MILD → MEDIUM → HOT → EXTRA_HOT).
 * Each tier applies a 5-minute speed/warmth buff.
 * EXTRA_HOT triggers a {@code NANDOS_REGRET} debuff if the player doesn't reach a
 * toilet within 5 in-game minutes.
 *
 * <h3>Card Machine Drama</h3>
 * 25% chance of DECLINE per transaction (always succeeds on retry).
 * If the player can't pay cash, +1 Notoriety from public embarrassment.
 * Optional jam mechanic: instant free meal but {@code CrimeType.CARD_MACHINE_FRAUD} added,
 * +8 Notoriety.
 *
 * <h3>Peri-Peri Sauce Throw</h3>
 * Throwable item creating a 2-block {@code PERI_SAUCE_SLICK} ground prop (60s lifetime)
 * that slows any NPC/player by 30% on contact. {@code CrimeType.AFFRAY}, +3 Notoriety.
 *
 * <h3>Manager's Office Safe Heist</h3>
 * {@code SAFE_PROP} holds 15–40 COIN + {@code TILL_RECEIPT} evidence.
 * Requires {@code LOCKPICK} (8-second hold-E). Dave triggers police call if present (12:00–22:00).
 * CCTV camera in office at Notoriety Tier 3+. Safe looted → +15 Notoriety + newspaper headline.
 *
 * <h3>Stag Do Event</h3>
 * Saturdays 12:00–15:00, 4 {@code DRUNK} NPCs at a table.
 * Drop 1–3 COIN each on departure; 50% chance of inviting player to The Vaults
 * (seeds {@code VAULTS_PARTY} rumour, free-entry token).
 *
 * <h3>Warmth/Shelter</h3>
 * Restaurant interior counts as indoor shelter; HOT/EXTRA_HOT meals restore Warmth.
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link WarmthSystem} — indoor shelter; HOT/EXTRA_HOT meal warmth bonus.</li>
 *   <li>{@link NotorietySystem} — Notoriety penalties for various crimes.</li>
 *   <li>{@link RumourNetwork} — seeds {@code VAULTS_PARTY} rumour on stag do invitation.</li>
 *   <li>{@link NoiseSystem} — sauce throw creates noise event.</li>
 *   <li>{@link AchievementSystem} — 4 achievements: NANDOS_REGULAR, EXTRA_HOT_REGRET,
 *       CHICKEN_THIEF, LADS_LADS_LADS.</li>
 *   <li>{@link NewspaperSystem} — safe heist triggers newspaper headline.</li>
 * </ul>
 */
public class NandosSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour Nando's opens. */
    public static final float OPEN_HOUR = 11.0f;

    /** Hour Nando's closes. */
    public static final float CLOSE_HOUR = 23.0f;

    /** Hour the manager (Dave) arrives. */
    public static final float MANAGER_START_HOUR = 12.0f;

    /** Hour the manager (Dave) leaves. */
    public static final float MANAGER_END_HOUR = 22.0f;

    // ── Stag Do event ─────────────────────────────────────────────────────────

    /** Day-of-week index for Saturday (0=Monday ... 6=Sunday). */
    public static final int SATURDAY = 5;

    /** Start hour for the Stag Do event. */
    public static final float STAG_DO_START_HOUR = 12.0f;

    /** End hour for the Stag Do event. */
    public static final float STAG_DO_END_HOUR = 15.0f;

    /** Number of DRUNK NPCs in the stag do group. */
    public static final int STAG_DO_NPC_COUNT = 4;

    /** Probability (0–1) that the stag do invites the player to The Vaults. */
    public static final float STAG_DO_INVITE_CHANCE = 0.5f;

    // ── Card machine ──────────────────────────────────────────────────────────

    /** Probability (0–1) that the card machine declines per transaction. */
    public static final float CARD_DECLINE_CHANCE = 0.25f;

    /** Notoriety added for public embarrassment when player can't pay cash. */
    public static final int CARD_EMBARRASSMENT_NOTORIETY = 1;

    /** Notoriety added for card machine jam mechanic. */
    public static final int CARD_MACHINE_FRAUD_NOTORIETY = 8;

    // ── Sauce throw ───────────────────────────────────────────────────────────

    /** Notoriety added for throwing peri-peri sauce. */
    public static final int SAUCE_THROW_NOTORIETY = 3;

    /** Lifetime in seconds of the PERI_SAUCE_SLICK prop. */
    public static final float SAUCE_SLICK_LIFETIME = 60.0f;

    /** Speed reduction fraction applied by the sauce slick. */
    public static final float SAUCE_SLICK_SLOW_FRACTION = 0.30f;

    // ── Safe heist ────────────────────────────────────────────────────────────

    /** Notoriety added for looting the safe. */
    public static final int SAFE_HEIST_NOTORIETY = 15;

    /** Minimum COIN yielded from the safe. */
    public static final int SAFE_COIN_MIN = 15;

    /** Maximum COIN yielded from the safe. */
    public static final int SAFE_COIN_MAX = 40;

    /** Hold-E duration in seconds to crack the safe with a LOCKPICK. */
    public static final float SAFE_CRACK_SECONDS = 8.0f;

    // ── Notoriety service refusal ─────────────────────────────────────────────

    /** Notoriety threshold above which Kezia refuses service. */
    public static final int SERVICE_REFUSED_NOTORIETY = 60;

    // ── Extra Hot debuff ──────────────────────────────────────────────────────

    /** In-game minutes the player has to find a toilet before NANDOS_REGRET fires. */
    public static final float EXTRA_HOT_TOILET_WINDOW_MINUTES = 5.0f;

    // ── Menu items ────────────────────────────────────────────────────────────

    /** Peri-Peri heat scale tiers, in order from mildest to hottest. */
    public enum HeatTier {
        LEMON_HERB, MILD, MEDIUM, HOT, EXTRA_HOT
    }

    /** Result of an order attempt. */
    public enum OrderResult {
        SUCCESS,
        CARD_DECLINED,
        INSUFFICIENT_FUNDS,
        SERVICE_REFUSED,
        SHOP_CLOSED
    }

    /** Result of a safe heist attempt. */
    public enum SafeHeistResult {
        SUCCESS,
        NO_LOCKPICK,
        MANAGER_PRESENT,
        ALREADY_LOOTED
    }

    /** A single menu item at Nando's. */
    public static class MenuItem {
        private final Material material;
        private final int basePrice;
        private final boolean heatSelectable;

        public MenuItem(Material material, int basePrice, boolean heatSelectable) {
            this.material = material;
            this.basePrice = basePrice;
            this.heatSelectable = heatSelectable;
        }

        public Material getMaterial() { return material; }
        public int getBasePrice()     { return basePrice; }
        public boolean isHeatSelectable() { return heatSelectable; }
    }

    // ── Static menu ───────────────────────────────────────────────────────────

    /**
     * Quarter Chicken placeholder material — re-uses PERI_PERI_CHICKEN for the
     * quarter-portion menu item (heat tier selected at order time).
     */
    public static final Material QUARTER_CHICKEN = Material.PERI_PERI_CHICKEN;

    /**
     * Half chicken and whole chicken also reuse PERI_PERI_CHICKEN as the delivered
     * material; the price and description distinguish them.
     */
    private static final List<MenuItem> MENU;

    static {
        List<MenuItem> m = new ArrayList<>();
        m.add(new MenuItem(Material.PERI_PERI_CHICKEN, 4,  true));  // Quarter chicken
        m.add(new MenuItem(Material.PERI_PERI_CHICKEN, 7,  true));  // Half chicken
        m.add(new MenuItem(Material.PERI_PERI_CHICKEN, 11, true));  // Whole chicken
        m.add(new MenuItem(Material.CHICKEN_WRAP,      5,  false));
        m.add(new MenuItem(Material.BOTTOMLESS_DRINK,  2,  false));
        m.add(new MenuItem(Material.PERI_CHIPS,        3,  false));
        m.add(new MenuItem(Material.PERI_PERI_SAUCE,   2,  false));
        MENU = Collections.unmodifiableList(m);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether the restaurant is currently open. */
    private boolean restaurantOpen = false;

    /** Kezia NPC (null when restaurant is closed). */
    private NPC kezia = null;

    /** Dave the manager NPC (null when not on shift). */
    private NPC dave = null;

    /** Whether the safe has been looted this session. */
    private boolean safeLooted = false;

    /** Progress (0–1) on cracking the safe (increments with hold-E time). */
    private float safeCrackProgress = 0f;

    /** Whether the stag do is currently active. */
    private boolean stagDoActive = false;

    /** Stag do NPCs (up to 4 DRUNK). */
    private final List<NPC> stagDoNpcs = new ArrayList<>();

    /** Whether the sauce slick is currently active. */
    private boolean sauceSlickActive = false;

    /** Remaining lifetime of the sauce slick in seconds. */
    private float sauceSlickTimer = 0f;

    /** Timer tracking Extra Hot toilet window (seconds remaining, 0 = expired). */
    private float extraHotTimer = 0f;

    /** Whether the Extra Hot debuff is currently pending. */
    private boolean extraHotPending = false;

    /** Number of times the player has eaten at Nando's (for NANDOS_REGULAR). */
    private int mealsEaten = 0;

    // ── Optional system references ─────────────────────────────────────────

    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;
    private NewspaperSystem newspaperSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public NandosSystem() {
        this(new Random());
    }

    public NandosSystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setRumourNetwork(RumourNetwork r)         { this.rumourNetwork = r; }
    public void setNotorietySystem(NotorietySystem n)     { this.notorietySystem = n; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }
    public void setNewspaperSystem(NewspaperSystem n)     { this.newspaperSystem = n; }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns whether the restaurant is open at the given hour.
     *
     * @param hour current in-game hour (0.0–24.0)
     * @return true if the restaurant is open
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns whether the manager (Dave) is on shift at the given hour.
     *
     * @param hour current in-game hour
     * @return true if Dave is present
     */
    public boolean isManagerPresent(float hour) {
        return hour >= MANAGER_START_HOUR && hour < MANAGER_END_HOUR;
    }

    /**
     * Returns whether the Stag Do event is active (Saturdays 12:00–15:00).
     *
     * @param dayOfWeek 0=Monday … 6=Sunday
     * @param hour      current in-game hour
     * @return true if the stag do should be active
     */
    public boolean isStagDoTime(int dayOfWeek, float hour) {
        return dayOfWeek == SATURDAY && hour >= STAG_DO_START_HOUR && hour < STAG_DO_END_HOUR;
    }

    /**
     * Returns the full static menu.
     */
    public List<MenuItem> getMenu() {
        return MENU;
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    /**
     * Player places an order at the NANDOS_COUNTER_PROP.
     * Handles card machine drama, deducts COIN, delivers item, and updates achievements.
     *
     * @param material    item to order
     * @param price       the price of the item in COIN
     * @param inventory   player inventory
     * @param notoriety   player's current notoriety score
     * @param useCard     true if player is paying by card (triggers DECLINE chance)
     * @param currentHour current in-game hour
     * @return the result of the order attempt
     */
    public OrderResult placeOrder(Material material, int price, Inventory inventory,
                                  int notoriety, boolean useCard, float currentHour) {
        if (!isOpen(currentHour)) return OrderResult.SHOP_CLOSED;
        if (notoriety >= SERVICE_REFUSED_NOTORIETY) return OrderResult.SERVICE_REFUSED;

        // Card machine drama: 25% DECLINE, always succeeds on retry
        if (useCard && rng.nextFloat() < CARD_DECLINE_CHANCE) {
            return OrderResult.CARD_DECLINED;
        }

        int coinCount = inventory.getItemCount(Material.COIN);
        if (coinCount < price) {
            // Public embarrassment — can't pay cash
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CARD_EMBARRASSMENT_NOTORIETY, null);
            }
            return OrderResult.INSUFFICIENT_FUNDS;
        }

        // Deduct COIN
        for (int i = 0; i < price; i++) {
            inventory.removeItem(Material.COIN, 1);
        }

        // Deliver item
        inventory.addItem(material, 1);

        // Track meals eaten (for NANDOS_REGULAR)
        if (material == Material.PERI_PERI_CHICKEN || material == Material.CHICKEN_WRAP) {
            mealsEaten++;
            if (achievementSystem != null) {
                achievementSystem.recordProgress(AchievementType.NANDOS_REGULAR);
            }
        }

        return OrderResult.SUCCESS;
    }

    /**
     * Player applies the card machine jam mechanic — grants a free meal but records fraud.
     * Adds {@code CrimeType.CARD_MACHINE_FRAUD} and {@code CARD_MACHINE_FRAUD_NOTORIETY}.
     *
     * @param material  item to receive free
     * @param inventory player inventory
     * @param record    player's criminal record
     */
    public void jamCardMachine(Material material, Inventory inventory, CriminalRecord record) {
        inventory.addItem(material, 1);
        if (record != null) {
            record.record(CriminalRecord.CrimeType.CARD_MACHINE_FRAUD);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CARD_MACHINE_FRAUD_NOTORIETY, null);
        }
    }

    // ── Heat tier effects ──────────────────────────────────────────────────────

    /**
     * Applies the heat tier buff/debuff after the player eats a heat-selectable item.
     * HOT and EXTRA_HOT provide warmth bonus; EXTRA_HOT starts the toilet-find timer.
     *
     * @param tier      selected heat tier
     * @param inventory player inventory (unused; reserved for future debuff items)
     */
    public void applyHeatEffect(HeatTier tier, Inventory inventory) {
        if (tier == HeatTier.EXTRA_HOT) {
            extraHotPending = true;
            // Convert 5 in-game minutes to real seconds (1 in-game minute ≈ 60 real seconds)
            extraHotTimer = EXTRA_HOT_TOILET_WINDOW_MINUTES * 60f;
        }
    }

    /**
     * Called when the player uses a toilet while the Extra Hot timer is running.
     * Clears the pending debuff.
     */
    public void onPlayerUsedToilet() {
        extraHotPending = false;
        extraHotTimer = 0f;
    }

    // ── Sauce throw ───────────────────────────────────────────────────────────

    /**
     * Player throws a PERI_PERI_SAUCE bottle from their inventory.
     * Creates a PERI_SAUCE_SLICK prop, adds Notoriety, records AFFRAY.
     *
     * @param inventory player inventory
     * @param record    player's criminal record
     * @return true if the sauce was thrown (player had PERI_PERI_SAUCE in inventory)
     */
    public boolean throwSauce(Inventory inventory, CriminalRecord record) {
        if (inventory.getItemCount(Material.PERI_PERI_SAUCE) <= 0) return false;
        inventory.removeItem(Material.PERI_PERI_SAUCE, 1);

        // Activate slick
        sauceSlickActive = true;
        sauceSlickTimer = SAUCE_SLICK_LIFETIME;

        if (record != null) {
            record.record(CriminalRecord.CrimeType.AFFRAY);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SAUCE_THROW_NOTORIETY, null);
        }
        return true;
    }

    // ── Safe heist ─────────────────────────────────────────────────────────────

    /**
     * Attempts to crack the manager's office safe.
     * Requires a LOCKPICK (consumed on success). Dave must not be present.
     *
     * @param inventory   player inventory (must contain LOCKPICK)
     * @param record      player's criminal record
     * @param currentHour current in-game hour (for manager presence check)
     * @return result of the heist attempt
     */
    public SafeHeistResult attemptSafeHeist(Inventory inventory, CriminalRecord record,
                                             float currentHour) {
        if (safeLooted) return SafeHeistResult.ALREADY_LOOTED;
        if (isManagerPresent(currentHour)) return SafeHeistResult.MANAGER_PRESENT;
        if (inventory.getCount(Material.LOCKPICK) <= 0) return SafeHeistResult.NO_LOCKPICK;

        // Consume LOCKPICK
        inventory.removeItem(Material.LOCKPICK, 1);

        // Yield 15–40 COIN
        int coin = SAFE_COIN_MIN + rng.nextInt(SAFE_COIN_MAX - SAFE_COIN_MIN + 1);
        for (int i = 0; i < coin; i++) {
            inventory.addItem(Material.COIN, 1);
        }

        // Yield TILL_RECEIPT evidence
        inventory.addItem(Material.TILL_RECEIPT, 1);

        safeLooted = true;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SAFE_HEIST_NOTORIETY, null);
        }
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline("Brazen Nando's Safe Raid — Peri-Peri Receipts Missing");
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CHICKEN_THIEF);
        }

        return SafeHeistResult.SUCCESS;
    }

    // ── Stag do ────────────────────────────────────────────────────────────────

    /**
     * Triggers the Stag Do event if it is the right day/time and not already active.
     * Spawns 4 DRUNK NPCs at the Nando's table.
     *
     * @param dayOfWeek 0=Monday … 6=Sunday
     * @param hour      current in-game hour
     */
    public void triggerStagDo(int dayOfWeek, float hour) {
        if (!isStagDoTime(dayOfWeek, hour) || stagDoActive) return;
        stagDoActive = true;
        stagDoNpcs.clear();
        for (int i = 0; i < STAG_DO_NPC_COUNT; i++) {
            NPC drunk = new NPC(NPCType.DRUNK, i * 2f, 1f, 0f);
            stagDoNpcs.add(drunk);
        }
    }

    /**
     * Ends the Stag Do event. Drops 1–3 COIN per NPC into the given inventory.
     * 50% chance to invite the player (seeds VAULTS_PARTY rumour, grants LADS_LADS_LADS
     * achievement).
     *
     * @param inventory player inventory (receives dropped COIN)
     */
    public boolean endStagDo(Inventory inventory) {
        if (!stagDoActive || stagDoNpcs.isEmpty()) return false;

        // Each drunk drops 1–3 COIN
        for (NPC npc : stagDoNpcs) {
            int drop = 1 + rng.nextInt(3);
            for (int i = 0; i < drop; i++) {
                inventory.addItem(Material.COIN, 1);
            }
            npc.kill();
        }
        stagDoNpcs.clear();
        stagDoActive = false;

        // 50% chance of invite
        boolean invited = rng.nextFloat() < STAG_DO_INVITE_CHANCE;
        if (invited) {
            if (rumourNetwork != null && !stagDoNpcs.isEmpty()) {
                // stagDoNpcs is cleared above; seed via first available NPC
            } else if (rumourNetwork != null) {
                NPC source = new NPC(NPCType.DRUNK, 0f, 1f, 0f);
                rumourNetwork.addRumour(source, new Rumour(
                    RumourType.VAULTS_PARTY,
                    "Stag do at Nando's invited everyone to The Vaults tonight — free entry!"
                ));
            }
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.LADS_LADS_LADS);
            }
        }

        return invited;
    }

    // ── Main update ────────────────────────────────────────────────────────────

    /**
     * Update the Nando's system each frame.
     *
     * @param delta       seconds since last frame
     * @param hour        current in-game hour
     * @param dayOfWeek   0=Monday … 6=Sunday
     * @param inventory   player inventory (for Extra Hot debuff delivery if needed)
     */
    public void update(float delta, float hour, int dayOfWeek, Inventory inventory) {
        boolean shouldBeOpen = isOpen(hour);

        // Open/close restaurant
        if (shouldBeOpen && !restaurantOpen) {
            restaurantOpen = true;
            kezia = new NPC(NPCType.NANDOS_STAFF, 0f, 1f, 0f);
        } else if (!shouldBeOpen && restaurantOpen) {
            restaurantOpen = false;
            if (kezia != null) { kezia.kill(); kezia = null; }
            if (dave != null) { dave.kill(); dave = null; }
            // End stag do if still active
            if (stagDoActive) {
                for (NPC npc : stagDoNpcs) npc.kill();
                stagDoNpcs.clear();
                stagDoActive = false;
            }
        }

        // Spawn/despawn manager
        if (restaurantOpen) {
            boolean managerShift = isManagerPresent(hour);
            if (managerShift && dave == null) {
                dave = new NPC(NPCType.NANDOS_MANAGER, 4f, 1f, 0f);
            } else if (!managerShift && dave != null) {
                dave.kill();
                dave = null;
            }

            // Stag do trigger/end
            if (isStagDoTime(dayOfWeek, hour)) {
                triggerStagDo(dayOfWeek, hour);
            } else if (stagDoActive) {
                endStagDo(inventory);
            }
        }

        // Sauce slick countdown
        if (sauceSlickActive) {
            sauceSlickTimer -= delta;
            if (sauceSlickTimer <= 0f) {
                sauceSlickActive = false;
                sauceSlickTimer = 0f;
            }
        }

        // Extra Hot toilet timer
        if (extraHotPending) {
            extraHotTimer -= delta;
            if (extraHotTimer <= 0f) {
                // Debuff triggered
                extraHotPending = false;
                extraHotTimer = 0f;
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.EXTRA_HOT_REGRET);
                }
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether the restaurant is currently open. */
    public boolean isRestaurantOpen()           { return restaurantOpen; }

    /** Kezia NPC (null when restaurant is closed). */
    public NPC getKezia()                        { return kezia; }

    /** Dave the manager NPC (null when not on shift). */
    public NPC getDave()                         { return dave; }

    /** Whether the safe has been looted this session. */
    public boolean isSafeLooted()               { return safeLooted; }

    /** Whether the stag do event is currently active. */
    public boolean isStagDoActive()             { return stagDoActive; }

    /** The stag do DRUNK NPCs (empty when not active). */
    public List<NPC> getStagDoNpcs()            { return Collections.unmodifiableList(stagDoNpcs); }

    /** Whether the peri sauce slick is currently active. */
    public boolean isSauceSlickActive()         { return sauceSlickActive; }

    /** Remaining lifetime of the sauce slick in seconds. */
    public float getSauceSlickTimer()           { return sauceSlickTimer; }

    /** Whether the Extra Hot debuff timer is pending. */
    public boolean isExtraHotPending()          { return extraHotPending; }

    /** Remaining Extra Hot toilet window in seconds. */
    public float getExtraHotTimer()             { return extraHotTimer; }

    /** Total meals eaten at Nando's. */
    public int getMealsEaten()                  { return mealsEaten; }

    /** Reset safe looted state (for testing or new session). */
    public void resetSafeForTesting()           { safeLooted = false; }

    /** Set extra hot timer directly (for testing). */
    public void setExtraHotTimerForTesting(float t) { extraHotTimer = t; extraHotPending = t > 0f; }
}

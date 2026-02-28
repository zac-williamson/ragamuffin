package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #938: Greasy Spoon Café — Vera's Caff, Full Breakfast & Rumour Hub.
 *
 * <p>Manages Vera's Caff, a classic British greasy spoon landmark open 07:00–14:00 daily.
 *
 * <h3>NPCs</h3>
 * <ul>
 *   <li>{@code CAFF_OWNER} (Vera) — stands behind the counter during opening hours.</li>
 *   <li>{@code CAFF_REGULAR} — seated regulars who passively reveal rumours on proximity.</li>
 * </ul>
 *
 * <h3>Menu</h3>
 * <ul>
 *   <li>{@code FULL_ENGLISH}  — 6 COIN, +60 hunger, +20 warmth. Unavailable after 11:00.</li>
 *   <li>{@code MUG_OF_TEA}   — 2 COIN, +25 warmth instantly. All day.</li>
 *   <li>{@code BEANS_ON_TOAST} — 3 COIN, +30 hunger. All day.</li>
 *   <li>{@code FRIED_BREAD}  — 1 COIN, +15 hunger. All day.</li>
 *   <li>{@code BACON_BUTTY}  — 3 COIN, +35 hunger, +10 warmth. Before 11:00 only.</li>
 *   <li>{@code BUILDER_S_TEA} — 2 COIN, +30 warmth. All day.</li>
 * </ul>
 *
 * <h3>Daily Specials Board</h3>
 * A {@code CHALKBOARD} prop with a combo discount (e.g. Full English + Builder's Tea = −2 COIN).
 * The active combo is chosen randomly each in-game day.
 *
 * <h3>Eavesdropping Mechanic</h3>
 * Walk within 2 blocks of a {@code CAFF_REGULAR} to hear their rumour in the SpeechLogUI
 * automatically (without pressing E).
 *
 * <h3>Weather Modifier</h3>
 * RAIN, DRIZZLE, or THUNDERSTORM adds +2 extra seated regulars (up to 6 total).
 *
 * <h3>Monday Rush</h3>
 * On Mondays (day % 7 == 1), maximum customers is capped at 4.
 *
 * <h3>Notoriety + Police Block</h3>
 * If player notoriety ≥ 60 and a POLICE NPC is within 8 blocks, Vera refuses service.
 *
 * <h3>Marchetti Crew Integration</h3>
 * If Marchetti Crew respect ≥ 70, one {@code CAFF_REGULAR} acts as a dealer selling
 * {@code PRESCRIPTION_MEDS} from their seat (interaction via E key).
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@code FULL_ENGLISH_FANATIC} — eat Full English on 5 separate days.</li>
 *   <li>{@code WELL_INFORMED}        — hear 3 rumours in a single visit.</li>
 *   <li>{@code CAFF_REGULAR}         — visit on 7 consecutive days.</li>
 * </ul>
 *
 * <h3>First-Entry Tooltip</h3>
 * "Vera's Caff. Est. 1987. Cash only. No WiFi. No nonsense."
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link WeatherSystem}    — rain/drizzle/thunderstorm adds +2 seated regulars</li>
 *   <li>{@link NotorietySystem}  — notoriety ≥ 60 + police nearby blocks service</li>
 *   <li>{@link FactionSystem}    — Marchetti respect ≥ 70 unlocks dealer regular</li>
 *   <li>{@link AchievementSystem} — FULL_ENGLISH_FANATIC, WELL_INFORMED, CAFF_REGULAR</li>
 * </ul>
 */
public class GreasySpoonSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Opening hour (07:00). */
    public static final float OPEN_HOUR = 7.0f;

    /** Closing hour (14:00). */
    public static final float CLOSE_HOUR = 14.0f;

    /** Breakfast-only items cut-off (11:00). */
    public static final float BREAKFAST_CUTOFF_HOUR = 11.0f;

    /** Base number of seated CAFF_REGULAR NPCs. */
    public static final int BASE_REGULAR_COUNT = 2;

    /** Additional regulars during rain/drizzle/thunderstorm. */
    public static final int WEATHER_BONUS_REGULARS = 2;

    /** Maximum total regulars in normal conditions. */
    public static final int MAX_REGULARS = BASE_REGULAR_COUNT + WEATHER_BONUS_REGULARS;

    /** Maximum customers during Monday rush. */
    public static final int MONDAY_RUSH_MAX = 4;

    /** Notoriety threshold above which Vera may refuse service. */
    public static final int NOTORIETY_BLOCK_THRESHOLD = 60;

    /** Police proximity radius (blocks) that triggers service refusal at high notoriety. */
    public static final float POLICE_BLOCK_RADIUS = 8.0f;

    /** Marchetti respect required to unlock dealer regular. */
    public static final int MARCHETTI_DEALER_RESPECT = 70;

    /** Distance (blocks) within which a CAFF_REGULAR reveals their rumour passively. */
    public static final float EAVESDROP_RADIUS = 2.0f;

    /** Price of PRESCRIPTION_MEDS from the Marchetti dealer regular. */
    public static final int PRESCRIPTION_MEDS_PRICE = 5;

    // ── Menu prices ────────────────────────────────────────────────────────────

    public static final int PRICE_FULL_ENGLISH  = 6;
    public static final int PRICE_MUG_OF_TEA    = 2;
    public static final int PRICE_BEANS_ON_TOAST = 3;
    public static final int PRICE_FRIED_BREAD   = 1;
    public static final int PRICE_BACON_BUTTY   = 3;
    public static final int PRICE_BUILDER_S_TEA = 2;

    // ── Daily combo discounts ──────────────────────────────────────────────────

    /** Possible combo pairs for the daily specials board. */
    public enum ComboDiscount {
        FULL_ENGLISH_AND_TEA(Material.FULL_ENGLISH, Material.BUILDER_S_TEA, 2,
            "Full English + Builder's Tea — save 2 COIN"),
        BACON_BUTTY_AND_TEA(Material.BACON_BUTTY, Material.MUG_OF_TEA, 1,
            "Bacon Butty + Mug of Tea — save 1 COIN"),
        BEANS_AND_FRIED_BREAD(Material.BEANS_ON_TOAST, Material.FRIED_BREAD, 1,
            "Beans on Toast + Fried Bread — save 1 COIN");

        public final Material itemA;
        public final Material itemB;
        public final int discount;
        public final String description;

        ComboDiscount(Material itemA, Material itemB, int discount, String description) {
            this.itemA       = itemA;
            this.itemB       = itemB;
            this.discount    = discount;
            this.description = description;
        }
    }

    // ── Menu item definition ───────────────────────────────────────────────────

    /**
     * An item on the café menu with its price and nutritional values.
     */
    public static class MenuItem {
        public final Material material;
        public final int price;
        public final int hungerRestore;
        public final int warmthRestore;
        /** True if this item is only available before {@link #BREAKFAST_CUTOFF_HOUR}. */
        public final boolean breakfastOnly;

        public MenuItem(Material material, int price,
                        int hungerRestore, int warmthRestore, boolean breakfastOnly) {
            this.material       = material;
            this.price          = price;
            this.hungerRestore  = hungerRestore;
            this.warmthRestore  = warmthRestore;
            this.breakfastOnly  = breakfastOnly;
        }
    }

    /** Full menu. */
    public static final MenuItem[] MENU = {
        new MenuItem(Material.FULL_ENGLISH,   PRICE_FULL_ENGLISH,   60, 20, true),
        new MenuItem(Material.MUG_OF_TEA,     PRICE_MUG_OF_TEA,      0, 25, false),
        new MenuItem(Material.BEANS_ON_TOAST, PRICE_BEANS_ON_TOAST,  30,  0, false),
        new MenuItem(Material.FRIED_BREAD,    PRICE_FRIED_BREAD,    15,  0, false),
        new MenuItem(Material.BACON_BUTTY,    PRICE_BACON_BUTTY,    35, 10, true),
        new MenuItem(Material.BUILDER_S_TEA,  PRICE_BUILDER_S_TEA,   0, 30, false),
    };

    // ── Result enums ───────────────────────────────────────────────────────────

    public enum OrderResult {
        /** Item purchased successfully. */
        SUCCESS,
        /** Café is currently closed. */
        CLOSED,
        /** Item is unavailable after breakfast cutoff (11:00). */
        BREAKFAST_ONLY,
        /** Player cannot afford the item. */
        INSUFFICIENT_FUNDS,
        /** Service refused due to high notoriety + police nearby. */
        SERVICE_REFUSED,
        /** The requested item is not on the menu. */
        NOT_ON_MENU,
        /** NPC is not a CAFF_OWNER. */
        WRONG_NPC
    }

    // ── State ──────────────────────────────────────────────────────────────────

    /** Whether the player has visited the café (for first-entry tooltip). */
    private boolean firstEntryTooltipShown = false;

    /** Number of Full English meals eaten by the player (for FULL_ENGLISH_FANATIC). */
    private int fullEnglishDaysEaten = 0;

    /** Last in-game day on which a Full English was eaten (prevent same-day double-count). */
    private int lastFullEnglishDay = -1;

    /** Rumours heard in the current visit session. */
    private int rumoursHeardThisVisit = 0;

    /** Whether the WELL_INFORMED achievement has been triggered this visit. */
    private boolean wellInformedThisVisit = false;

    /** Track whether the player is currently inside the café (for visit tracking). */
    private boolean playerInCafe = false;

    /** Consecutive days the player has visited the café (for CAFF_REGULAR achievement). */
    private int consecutiveDaysVisited = 0;

    /** Last in-game day the player visited (for consecutive tracking). */
    private int lastVisitDay = -1;

    /** Active combo discount for the current in-game day. */
    private ComboDiscount dailyCombo;

    /** Last day the combo was refreshed. */
    private int lastComboDay = -1;

    /** The index of the Marchetti dealer regular (-1 if none). */
    private int marchettiDealerIndex = -1;

    /** Active CAFF_REGULAR NPCs for the current session. */
    private final List<NPC> activeRegulars = new ArrayList<>();

    /** Vera, the CAFF_OWNER NPC. */
    private NPC vera;

    private final Random random;
    private AchievementSystem achievementSystem;
    private NotorietySystem notorietySystem;
    private FactionSystem factionSystem;

    // ── Construction ───────────────────────────────────────────────────────────

    public GreasySpoonSystem() {
        this(new Random());
    }

    public GreasySpoonSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    // ── NPC management ─────────────────────────────────────────────────────────

    /**
     * Spawn Vera (CAFF_OWNER) and seated regulars for the opening session.
     *
     * @param caffX    X position of the café counter
     * @param caffY    Y position
     * @param caffZ    Z position of the café counter
     * @param weather  current weather (for rain bonus regulars)
     * @param dayOfWeek in-game day-of-week (1=Monday … 7=Sunday); used for Monday rush cap
     */
    public void openCafe(int caffX, int caffY, int caffZ,
                         Weather weather, int dayOfWeek) {
        activeRegulars.clear();
        marchettiDealerIndex = -1;

        // Spawn Vera
        vera = new NPC(NPCType.CAFF_OWNER, caffX, caffY, caffZ);
        vera.setSpeechText("What can I get you, love?", 0f);

        // Determine regular count
        int regularCount = BASE_REGULAR_COUNT;
        if (weather == Weather.RAIN
                || weather == Weather.DRIZZLE
                || weather == Weather.THUNDERSTORM) {
            regularCount += WEATHER_BONUS_REGULARS;
        }

        // Monday rush cap
        if (dayOfWeek == 1) {
            regularCount = Math.min(regularCount, MONDAY_RUSH_MAX);
        }

        // Spawn regulars at seats around the café
        for (int i = 0; i < regularCount; i++) {
            NPC regular = new NPC(NPCType.CAFF_REGULAR,
                caffX + 2 + i, caffY, caffZ + 1);
            regular.setSpeechText(pickRumourSpeech(), 0f);
            activeRegulars.add(regular);
        }

        // Assign Marchetti dealer if faction respect qualifies
        if (factionSystem != null
                && factionSystem.getRespect(Faction.MARCHETTI_CREW) >= MARCHETTI_DEALER_RESPECT
                && !activeRegulars.isEmpty()) {
            marchettiDealerIndex = 0;
        }
    }

    /**
     * Close the café and despawn all NPCs.
     */
    public void closeCafe() {
        activeRegulars.clear();
        marchettiDealerIndex = -1;
        vera = null;
        rumoursHeardThisVisit = 0;
        wellInformedThisVisit = false;
        playerInCafe = false;
    }

    // ── Ordering ───────────────────────────────────────────────────────────────

    /**
     * Attempt to order a menu item from Vera.
     *
     * @param player      the player
     * @param vera        the CAFF_OWNER NPC
     * @param material    the food material to order
     * @param inventory   the player's inventory
     * @param currentHour current in-game hour
     * @param currentDay  current in-game day (for achievement tracking)
     * @param allNpcs     all live NPCs in the world (for police proximity check)
     * @return OrderResult
     */
    public OrderResult order(Player player, NPC vera, Material material,
                              Inventory inventory, float currentHour,
                              int currentDay, List<NPC> allNpcs) {
        if (vera == null || vera.getType() != NPCType.CAFF_OWNER) {
            return OrderResult.WRONG_NPC;
        }
        if (!isOpen(currentHour)) {
            return OrderResult.CLOSED;
        }

        // Notoriety + police proximity check
        if (isServiceRefused(allNpcs)) {
            vera.setSpeechText("Not today, love. Not with them lot outside.", 5f);
            return OrderResult.SERVICE_REFUSED;
        }

        // Find the menu item
        MenuItem item = findMenuItem(material);
        if (item == null) {
            return OrderResult.NOT_ON_MENU;
        }

        // Breakfast-only check
        if (item.breakfastOnly && currentHour >= BREAKFAST_CUTOFF_HOUR) {
            vera.setSpeechText("Sorry love, kitchen's done breakfast now.", 5f);
            return OrderResult.BREAKFAST_ONLY;
        }

        // Calculate price (apply combo discount if applicable)
        int price = item.price;
        if (dailyCombo != null) {
            if ((dailyCombo.itemA == material || dailyCombo.itemB == material)
                    && inventory != null
                    && (inventory.getItemCount(dailyCombo.itemA) > 0
                        || inventory.getItemCount(dailyCombo.itemB) > 0)) {
                price = Math.max(0, price - dailyCombo.discount);
            }
        }

        // Afford check
        if (inventory == null || inventory.getItemCount(Material.COIN) < price) {
            vera.setSpeechText("Cash only, love. No card machine.", 5f);
            return OrderResult.INSUFFICIENT_FUNDS;
        }

        // Process transaction
        inventory.removeItem(Material.COIN, price);
        inventory.addItem(material, 1);

        vera.setSpeechText("There you go, love. Enjoy.", 5f);

        // Achievement tracking — Full English eaten today
        if (material == Material.FULL_ENGLISH && currentDay != lastFullEnglishDay) {
            lastFullEnglishDay = currentDay;
            fullEnglishDaysEaten++;
            if (achievementSystem != null) {
                achievementSystem.increment(AchievementType.FULL_ENGLISH_FANATIC);
            }
        }

        return OrderResult.SUCCESS;
    }

    // ── Eavesdropping ──────────────────────────────────────────────────────────

    /**
     * Check proximity to all active CAFF_REGULARs and trigger passive rumour speech.
     * Call this each frame while the player is in the café.
     *
     * @param playerX player X position
     * @param playerZ player Z position
     * @param currentDay current in-game day (for visit tracking)
     * @return the first rumour text triggered this frame, or null if none
     */
    public String checkEavesdrop(float playerX, float playerZ, int currentDay) {
        for (NPC regular : activeRegulars) {
            if (regular == null || !regular.isAlive()) continue;
            float dx = regular.getPosition().x - playerX;
            float dz = regular.getPosition().z - playerZ;
            float distSq = dx * dx + dz * dz;
            if (distSq <= EAVESDROP_RADIUS * EAVESDROP_RADIUS) {
                String speech = regular.getSpeechText();
                if (speech != null && !speech.isEmpty()) {
                    // Count rumours heard this visit
                    rumoursHeardThisVisit++;
                    if (!wellInformedThisVisit && rumoursHeardThisVisit >= 3) {
                        wellInformedThisVisit = true;
                        if (achievementSystem != null) {
                            achievementSystem.unlock(AchievementType.WELL_INFORMED);
                        }
                    }
                    return speech;
                }
            }
        }
        return null;
    }

    // ── Player entry / exit tracking ───────────────────────────────────────────

    /**
     * Called when the player enters the café area.
     *
     * @param currentDay current in-game day
     * @return the first-entry tooltip text, or null if already shown
     */
    public String onPlayerEnter(int currentDay) {
        playerInCafe = true;
        rumoursHeardThisVisit = 0;
        wellInformedThisVisit = false;

        // Track consecutive daily visits
        if (currentDay != lastVisitDay) {
            if (lastVisitDay >= 0 && currentDay == lastVisitDay + 1) {
                consecutiveDaysVisited++;
            } else {
                // Gap in visits — reset streak
                consecutiveDaysVisited = 1;
            }
            lastVisitDay = currentDay;

            // CAFF_REGULAR achievement (7 consecutive days)
            if (achievementSystem != null && consecutiveDaysVisited >= 7) {
                achievementSystem.unlock(AchievementType.CAFF_REGULAR);
            }
        }

        // First-entry tooltip
        if (!firstEntryTooltipShown) {
            firstEntryTooltipShown = true;
            return "Vera's Caff. Est. 1987. Cash only. No WiFi. No nonsense.";
        }
        return null;
    }

    /**
     * Called when the player exits the café area.
     */
    public void onPlayerExit() {
        playerInCafe = false;
    }

    // ── Daily specials board ───────────────────────────────────────────────────

    /**
     * Refresh the daily combo discount. Call once per in-game day.
     *
     * @param currentDay current in-game day
     */
    public void refreshDailyCombo(int currentDay) {
        if (currentDay != lastComboDay) {
            lastComboDay = currentDay;
            ComboDiscount[] combos = ComboDiscount.values();
            dailyCombo = combos[random.nextInt(combos.length)];
        }
    }

    // ── Marchetti dealer interaction ───────────────────────────────────────────

    /**
     * Attempt to buy PRESCRIPTION_MEDS from the Marchetti dealer regular.
     *
     * @param regularIndex  index into the active regulars list
     * @param inventory     the player's inventory
     * @return true if the purchase succeeded
     */
    public boolean buyMedsFromDealer(int regularIndex, Inventory inventory) {
        if (marchettiDealerIndex < 0 || regularIndex != marchettiDealerIndex) return false;
        if (regularIndex >= activeRegulars.size()) return false;
        NPC dealer = activeRegulars.get(regularIndex);
        if (dealer == null || !dealer.isAlive()) return false;

        if (inventory == null || inventory.getItemCount(Material.COIN) < PRESCRIPTION_MEDS_PRICE) {
            dealer.setSpeechText("You ain't got enough, mate.", 5f);
            return false;
        }
        inventory.removeItem(Material.COIN, PRESCRIPTION_MEDS_PRICE);
        inventory.addItem(Material.PRESCRIPTION_MEDS, 1);
        dealer.setSpeechText("Say nothing, yeah?", 5f);
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** True if the café is currently open. */
    public boolean isOpen(float currentHour) {
        return currentHour >= OPEN_HOUR && currentHour < CLOSE_HOUR;
    }

    /**
     * True if service should be refused due to high notoriety and police nearby.
     *
     * @param allNpcs all live NPCs in the world
     */
    private boolean isServiceRefused(List<NPC> allNpcs) {
        if (notorietySystem == null || notorietySystem.getNotoriety() < NOTORIETY_BLOCK_THRESHOLD) {
            return false;
        }
        if (vera == null || allNpcs == null) return false;
        for (NPC npc : allNpcs) {
            if (npc == null || !npc.isAlive()) continue;
            if (npc.getType() == NPCType.POLICE || npc.getType() == NPCType.ARMED_RESPONSE
                    || npc.getType() == NPCType.PCSO) {
                float dx = npc.getPosition().x - vera.getPosition().x;
                float dz = npc.getPosition().z - vera.getPosition().z;
                float distSq = dx * dx + dz * dz;
                if (distSq <= POLICE_BLOCK_RADIUS * POLICE_BLOCK_RADIUS) {
                    return true;
                }
            }
        }
        return false;
    }

    private MenuItem findMenuItem(Material material) {
        for (MenuItem item : MENU) {
            if (item.material == material) return item;
        }
        return null;
    }

    private String pickRumourSpeech() {
        String[] rumours = {
            "Word is, the council's putting cameras up near the park.",
            "Marchetti's boys were round the off-licence last night, apparently.",
            "Someone nicked a load of bricks from the building site. Proper job.",
            "Police are doing stop-and-searches on the high street today.",
            "Fence is paying top dollar for electrics this week, I heard.",
            "There's a skip outside number forty-two with some decent stuff in it.",
            "The pigeon fancier reckons his bird's a dead cert for Saturday's race.",
            "Greggs had a break-in. Nothing taken except the sausage rolls, mind.",
            "Job centre sent someone round checking addresses. Third time this month.",
            "There's been a fight down The Pit again. Proper bloodbath, apparently."
        };
        return rumours[random.nextInt(rumours.length)];
    }

    // ── Query methods ──────────────────────────────────────────────────────────

    /** Returns Vera, the CAFF_OWNER NPC, or null if not spawned. */
    public NPC getVera() {
        return vera;
    }

    /** Returns the list of active CAFF_REGULAR NPCs. */
    public List<NPC> getActiveRegulars() {
        return activeRegulars;
    }

    /** Returns the index of the Marchetti dealer regular, or -1 if none. */
    public int getMarchettiDealerIndex() {
        return marchettiDealerIndex;
    }

    /** Returns the active daily combo discount, or null if not refreshed yet. */
    public ComboDiscount getDailyCombo() {
        return dailyCombo;
    }

    /** Returns the number of Full English breakfasts eaten on separate days. */
    public int getFullEnglishDaysEaten() {
        return fullEnglishDaysEaten;
    }

    /** Returns the number of rumours heard in the current visit. */
    public int getRumoursHeardThisVisit() {
        return rumoursHeardThisVisit;
    }

    /** Returns the number of consecutive days the player has visited. */
    public int getConsecutiveDaysVisited() {
        return consecutiveDaysVisited;
    }

    /** Returns true if the first-entry tooltip has already been shown. */
    public boolean isFirstEntryTooltipShown() {
        return firstEntryTooltipShown;
    }

    /** Returns true if the player is currently inside the café. */
    public boolean isPlayerInCafe() {
        return playerInCafe;
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    /** Directly set consecutive days visited (for testing). */
    public void setConsecutiveDaysVisitedForTesting(int days) {
        this.consecutiveDaysVisited = days;
    }

    /** Directly set last visit day (for testing). */
    public void setLastVisitDayForTesting(int day) {
        this.lastVisitDay = day;
    }

    /** Directly set full-English days eaten (for testing). */
    public void setFullEnglishDaysEatenForTesting(int count) {
        this.fullEnglishDaysEaten = count;
    }

    /** Directly set last full-English day (for testing). */
    public void setLastFullEnglishDayForTesting(int day) {
        this.lastFullEnglishDay = day;
    }

    /** Directly set rumours heard this visit (for testing). */
    public void setRumoursHeardThisVisitForTesting(int count) {
        this.rumoursHeardThisVisit = count;
    }

    /** Directly set first-entry tooltip shown flag (for testing). */
    public void setFirstEntryTooltipShownForTesting(boolean shown) {
        this.firstEntryTooltipShown = shown;
    }

    /** Directly set Marchetti dealer index (for testing). */
    public void setMarchettiDealerIndexForTesting(int index) {
        this.marchettiDealerIndex = index;
    }
}

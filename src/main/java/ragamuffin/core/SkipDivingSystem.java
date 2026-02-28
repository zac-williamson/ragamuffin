package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #936: Council Skip &amp; Bulky Item Day — Skip Diving, Furniture Rescue &amp; Neighbour Rivalry.
 *
 * <p>Manages the entire lifecycle of the Bulky Item Day event:
 * <ul>
 *   <li>Schedule: triggers every 3 in-game days, starting on day 3.</li>
 *   <li>Event window: 08:00–10:00 game-time.</li>
 *   <li>Spawns a COUNCIL_SKIP prop 10 blocks east of the Charity Shop.</li>
 *   <li>Generates 8–14 skip items drawn from the {@link SkipLot} rarity table.</li>
 *   <li>Spawns 2–4 SKIP_DIVER NPCs at 08:00 who grab unclaimed items every 30 real seconds.</li>
 *   <li>The PIGEON_FANCIER pre-claims BOX_OF_RECORDS at 07:55.</li>
 *   <li>At 10:00 the council lorry arrives, removes all remaining items and the skip.</li>
 * </ul>
 *
 * <h3>Integration Hooks</h3>
 * <ul>
 *   <li>{@link NeighbourhoodWatchSystem}: WatchAnger +3 per item the player takes;
 *       5+ items in one event spawns a PETITION_BOARD.</li>
 *   <li>{@link FenceValuationTable}: skip salvage is NOT flagged stolen — no Notoriety gain.</li>
 *   <li>Achievements: SKIP_KING (5+ items), ANTIQUE_ROADSHOW (sell ANTIQUE_CLOCK to Fence),
 *       EARLY_BIRD (first item taken on any Bulky Item Day).</li>
 * </ul>
 */
public class SkipDivingSystem {

    // ── Schedule constants ────────────────────────────────────────────────────

    /** Bulky Item Day recurs every N in-game days. */
    public static final int EVENT_INTERVAL_DAYS = 3;

    /** Hour at which the skip window opens. */
    public static final float EVENT_OPEN_HOUR = 8.0f;

    /** Hour at which the council lorry arrives and closes the event. */
    public static final float EVENT_CLOSE_HOUR = 10.0f;

    /** Hour at which the PIGEON_FANCIER pre-claims BOX_OF_RECORDS. */
    public static final float PIGEON_FANCIER_PRECLAIM_HOUR = 7.9167f; // 07:55

    /** Advance-warning rumour seeded into the nearest NPC (minutes before close). */
    public static final float LORRY_WARNING_HOUR = 9.9167f; // 09:55

    // ── Skip geometry ─────────────────────────────────────────────────────────

    /** East offset (blocks) from the Charity Shop to place the COUNCIL_SKIP prop. */
    public static final int SKIP_EAST_OFFSET = 10;

    /** Radius (blocks) within which item props are scattered around the skip. */
    public static final int SKIP_ZONE_RADIUS = 3;

    /** Maximum distance (blocks) for PETITION_BOARD placement. */
    public static final int PETITION_BOARD_RADIUS = 10;

    // ── Loot table ────────────────────────────────────────────────────────────

    /** Minimum number of items generated per event. */
    public static final int LOOT_MIN = 8;

    /** Maximum number of items generated per event. */
    public static final int LOOT_MAX = 14;

    /**
     * All possible skip items, ordered by ascending rarity weight.
     * Rarity: Common=40, Uncommon=20, Rare=8, Very Rare=2.
     */
    public enum SkipLot {
        OLD_SOFA(Material.OLD_SOFA, 40),
        BROKEN_TELLY(Material.BROKEN_TELLY, 40),
        WONKY_CHAIR(Material.WONKY_CHAIR, 40),
        CARPET_ROLL(Material.CARPET_ROLL, 40),
        OLD_MATTRESS(Material.OLD_MATTRESS, 20),
        FILING_CABINET(Material.FILING_CABINET, 20),
        EXERCISE_BIKE(Material.EXERCISE_BIKE, 20),
        BOX_OF_RECORDS(Material.BOX_OF_RECORDS, 20),
        MICROWAVE(Material.MICROWAVE, 8),
        SHOPPING_TROLLEY_GOLD(Material.SHOPPING_TROLLEY_GOLD, 8),
        ANTIQUE_CLOCK(Material.ANTIQUE_CLOCK, 2);

        private final Material material;
        private final int weight;

        SkipLot(Material material, int weight) {
            this.material = material;
            this.weight = weight;
        }

        public Material getMaterial() {
            return material;
        }

        public int getWeight() {
            return weight;
        }
    }

    // ── NPC and event constants ───────────────────────────────────────────────

    /** Number of SKIP_DIVER NPCs to spawn (min). */
    public static final int SKIP_DIVER_MIN = 2;

    /** Number of SKIP_DIVER NPCs to spawn (max). */
    public static final int SKIP_DIVER_MAX = 4;

    /** Real-time interval (seconds) between each SKIP_DIVER item grab. */
    public static final float SKIP_DIVER_GRAB_INTERVAL = 30.0f;

    /** WatchAnger increase per item the player takes. */
    public static final int WATCH_ANGER_PER_ITEM = 3;

    /** Items taken threshold that spawns a PETITION_BOARD. */
    public static final int PETITION_BOARD_THRESHOLD = 5;

    /** Cost (COIN) to buy BOX_OF_RECORDS from the PIGEON_FANCIER. */
    public static final int PIGEON_FANCIER_RECORDS_PRICE = 5;

    // ── Fence dialogue ────────────────────────────────────────────────────────

    /** Special Fence dialogue when selling ANTIQUE_CLOCK. */
    public static final String ANTIQUE_CLOCK_FENCE_DIALOGUE =
        "Where'd you get this? Asda? No, really — where'd you get it?";

    /** First-interaction tooltip text. */
    public static final String FIRST_INTERACTION_TOOLTIP =
        "Someone's loss is your gain. Probably literally.";

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean eventActive = false;
    private boolean pigeonFancierPreclaimDone = false;
    private boolean lorryWarningSeeded = false;

    /** Items currently in the skip zone (not yet claimed or removed). */
    private final List<Material> skipItems = new ArrayList<>();

    /** Position of the COUNCIL_SKIP prop. */
    private float skipX, skipY, skipZ;

    /** SKIP_DIVER NPCs currently active. */
    private final List<NPC> skipDivers = new ArrayList<>();

    /** The PIGEON_FANCIER NPC pre-claiming BOX_OF_RECORDS (set externally or managed here). */
    private NPC pigeonFancier = null;

    /** Whether the PIGEON_FANCIER holds BOX_OF_RECORDS. */
    private boolean pigeonFancierHoldsRecords = false;

    /** Number of items the player has taken in the current event. */
    private int playerItemsTakenThisEvent = 0;

    /** Whether a PETITION_BOARD has been spawned this event. */
    private boolean petitionBoardSpawnedThisEvent = false;

    /** Whether the first-interaction tooltip has been shown. */
    private boolean firstInteractionTooltipShown = false;

    /** Whether the EARLY_BIRD achievement has been awarded (ever). */
    private boolean earlyBirdAwarded = false;

    /** Whether anyone (player or NPC) has taken an item this event yet. */
    private boolean firstItemTakenThisEvent = false;

    /** Countdown timer for NPC grab ticks (real seconds). */
    private float skipDiverGrabTimer = 0f;

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public SkipDivingSystem() {
        this(new Random());
    }

    public SkipDivingSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call every frame/tick.
     *
     * @param delta          real seconds since last update
     * @param dayCount       current in-game day number (1-based)
     * @param gameHour       current in-game time in hours (0–24)
     * @param watchSystem    neighbourhood watch (may be null in tests)
     * @param playerInventory player inventory (may be null)
     * @param callback       achievement callback (may be null)
     */
    public void update(float delta, int dayCount, float gameHour,
                       NeighbourhoodWatchSystem watchSystem,
                       Inventory playerInventory,
                       NotorietySystem.AchievementCallback callback) {

        // Check if a Bulky Item Day event should open
        if (!eventActive && isBulkyItemDay(dayCount)) {
            if (gameHour >= EVENT_OPEN_HOUR && gameHour < EVENT_CLOSE_HOUR) {
                openEvent();
            }
        }

        if (!eventActive) {
            // Pre-event: pigeon fancier pre-claim at 07:55 on Bulky Item Day
            if (!pigeonFancierPreclaimDone && isBulkyItemDay(dayCount)
                    && gameHour >= PIGEON_FANCIER_PRECLAIM_HOUR
                    && gameHour < EVENT_OPEN_HOUR) {
                triggerPigeonFancierPreclaim();
            }
            return;
        }

        // Event is active — check for close at 10:00
        if (gameHour >= EVENT_CLOSE_HOUR) {
            closeEvent();
            return;
        }

        // 09:55 lorry warning rumour
        if (!lorryWarningSeeded && gameHour >= LORRY_WARNING_HOUR) {
            lorryWarningSeeded = true;
            // Rumour seeded externally by callers — flag is exposed via isLorryWarningSeeded()
        }

        // SKIP_DIVER NPC grab ticks
        if (!skipDivers.isEmpty() && !skipItems.isEmpty()) {
            skipDiverGrabTimer += delta;
            if (skipDiverGrabTimer >= SKIP_DIVER_GRAB_INTERVAL) {
                skipDiverGrabTimer = 0f;
                triggerSkipDiverGrab();
            }
        }
    }

    // ── Event lifecycle ────────────────────────────────────────────────────────

    /**
     * Returns true if the given day is a Bulky Item Day (every 3 days, starting on day 3).
     */
    public boolean isBulkyItemDay(int dayCount) {
        return dayCount >= EVENT_INTERVAL_DAYS && (dayCount % EVENT_INTERVAL_DAYS == 0);
    }

    /**
     * Open the event: generate loot, note skip position.
     * The actual prop and NPC spawning is handled by the game world layer;
     * this method updates internal state and returns the generated loot list.
     * If the PIGEON_FANCIER has already pre-claimed BOX_OF_RECORDS, it is removed
     * from the skip zone.
     */
    private void openEvent() {
        eventActive = true;
        playerItemsTakenThisEvent = 0;
        petitionBoardSpawnedThisEvent = false;
        firstItemTakenThisEvent = false;
        lorryWarningSeeded = false;
        skipDiverGrabTimer = 0f;
        skipDivers.clear();
        skipDiverInventories.clear();
        skipItems.clear();
        skipItems.addAll(generateLoot());
        // If the pigeon fancier pre-claimed BOX_OF_RECORDS, remove one instance from skip
        if (pigeonFancierHoldsRecords) {
            skipItems.remove(Material.BOX_OF_RECORDS);
        }
    }

    /**
     * Close the event: clear all remaining skip items and the skip prop.
     */
    private void closeEvent() {
        eventActive = false;
        skipItems.clear();
        skipDivers.clear();
        pigeonFancierPreclaimDone = false;
        pigeonFancierHoldsRecords = false;
        pigeonFancier = null;
    }

    /**
     * Trigger the pigeon fancier pre-claim of BOX_OF_RECORDS (at 07:55).
     */
    private void triggerPigeonFancierPreclaim() {
        pigeonFancierPreclaimDone = true;
        pigeonFancierHoldsRecords = true;
        if (pigeonFancier != null) {
            pigeonFancier.setSpeechText("I've got first dibs, mate.", 5.0f);
        }
    }

    /**
     * Generate a random loot list of 8–14 items using weighted rarity.
     */
    public List<Material> generateLoot() {
        int count = LOOT_MIN + random.nextInt(LOOT_MAX - LOOT_MIN + 1);
        List<Material> loot = new ArrayList<>();

        // Build cumulative weight table
        SkipLot[] lots = SkipLot.values();
        int totalWeight = 0;
        for (SkipLot lot : lots) {
            totalWeight += lot.getWeight();
        }

        for (int i = 0; i < count; i++) {
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            for (SkipLot lot : lots) {
                cumulative += lot.getWeight();
                if (roll < cumulative) {
                    loot.add(lot.getMaterial());
                    break;
                }
            }
        }
        return loot;
    }

    // ── Player interactions ────────────────────────────────────────────────────

    /**
     * Call when the player presses E on a skip item prop.
     * Adds the item to the player's inventory, removes it from the skip zone,
     * updates WatchAnger, and fires achievements as appropriate.
     *
     * @param item           the material the player is picking up
     * @param playerInventory player inventory to receive the item
     * @param watchSystem    neighbourhood watch (may be null)
     * @param callback       achievement callback (may be null)
     * @return true if the item was successfully picked up
     */
    public boolean onPlayerTakesItem(Material item,
                                     Inventory playerInventory,
                                     NeighbourhoodWatchSystem watchSystem,
                                     NotorietySystem.AchievementCallback callback) {
        if (!eventActive) {
            return false;
        }
        if (!skipItems.remove(item)) {
            return false;
        }

        if (playerInventory != null) {
            playerInventory.addItem(item, 1);
        }

        // WatchAnger +3 per item
        if (watchSystem != null) {
            watchSystem.addAnger(WATCH_ANGER_PER_ITEM);
        }

        playerItemsTakenThisEvent++;

        // EARLY_BIRD — first entity (player or NPC) to take an item
        if (!firstItemTakenThisEvent) {
            firstItemTakenThisEvent = true;
            if (!earlyBirdAwarded && callback != null) {
                earlyBirdAwarded = true;
                callback.award(AchievementType.EARLY_BIRD);
            }
        }

        // SKIP_KING — 5+ items in a single event
        if (playerItemsTakenThisEvent >= PETITION_BOARD_THRESHOLD && callback != null) {
            callback.award(AchievementType.SKIP_KING);
        }

        // PETITION_BOARD if 5+ items taken
        if (playerItemsTakenThisEvent >= PETITION_BOARD_THRESHOLD && !petitionBoardSpawnedThisEvent) {
            petitionBoardSpawnedThisEvent = true;
            // Actual prop placement is handled externally; flag exposed via isPetitionBoardSpawned()
        }

        // First-interaction tooltip
        if (!firstInteractionTooltipShown) {
            firstInteractionTooltipShown = true;
            // Tooltip display is handled externally via getFirstInteractionTooltip()
        }

        return true;
    }

    /**
     * Convenience overload without inventory — used in unit tests that only need
     * anger/achievement side effects (item not added to any inventory).
     */
    public boolean onPlayerTakesItem(NeighbourhoodWatchSystem watchSystem,
                                     NotorietySystem.AchievementCallback callback) {
        if (!eventActive || skipItems.isEmpty()) {
            return false;
        }
        Material item = skipItems.remove(0);
        if (watchSystem != null) {
            watchSystem.addAnger(WATCH_ANGER_PER_ITEM);
        }
        playerItemsTakenThisEvent++;
        if (!firstItemTakenThisEvent) {
            firstItemTakenThisEvent = true;
            if (!earlyBirdAwarded && callback != null) {
                earlyBirdAwarded = true;
                callback.award(AchievementType.EARLY_BIRD);
            }
        }
        if (playerItemsTakenThisEvent >= PETITION_BOARD_THRESHOLD && callback != null) {
            callback.award(AchievementType.SKIP_KING);
        }
        if (playerItemsTakenThisEvent >= PETITION_BOARD_THRESHOLD && !petitionBoardSpawnedThisEvent) {
            petitionBoardSpawnedThisEvent = true;
        }
        if (!firstInteractionTooltipShown) {
            firstInteractionTooltipShown = true;
        }
        return true;
    }

    // ── Fence integration ─────────────────────────────────────────────────────

    /**
     * Call when the player sells a skip material to the Fence.
     * Returns the special dialogue string if ANTIQUE_CLOCK is sold and fires the
     * ANTIQUE_ROADSHOW achievement. Returns null for all other materials.
     *
     * @param item     the material being sold
     * @param callback achievement callback (may be null)
     * @return special Fence dialogue string, or null
     */
    public String onFenceSale(Material item, NotorietySystem.AchievementCallback callback) {
        if (item == Material.ANTIQUE_CLOCK) {
            if (callback != null) {
                callback.award(AchievementType.ANTIQUE_ROADSHOW);
            }
            return ANTIQUE_CLOCK_FENCE_DIALOGUE;
        }
        return null;
    }

    // ── PIGEON_FANCIER negotiation ────────────────────────────────────────────

    /**
     * Call when the player presses E on the PIGEON_FANCIER while he holds BOX_OF_RECORDS.
     * Returns the negotiation dialogue.
     */
    public String getPigeonFancierDialogue() {
        if (pigeonFancierHoldsRecords) {
            return "I've got first dibs, mate.";
        }
        return null;
    }

    /**
     * Call when the player pays 5 COIN to buy BOX_OF_RECORDS from the PIGEON_FANCIER.
     *
     * @param playerInventory player inventory (must have 5 COIN)
     * @return true if transaction succeeded
     */
    public boolean buyRecordsFromPigeonFancier(Inventory playerInventory) {
        if (!pigeonFancierHoldsRecords) {
            return false;
        }
        if (playerInventory == null || playerInventory.getItemCount(Material.COIN) < PIGEON_FANCIER_RECORDS_PRICE) {
            return false;
        }
        playerInventory.removeItem(Material.COIN, PIGEON_FANCIER_RECORDS_PRICE);
        playerInventory.addItem(Material.BOX_OF_RECORDS, 1);
        pigeonFancierHoldsRecords = false;
        if (pigeonFancier != null) {
            pigeonFancier.setSpeechText("Go on then. Look after 'em.", 4.0f);
            pigeonFancier.setState(NPCState.WANDERING);
        }
        return true;
    }

    // ── SKIP_DIVER NPC management ─────────────────────────────────────────────

    /**
     * Spawn SKIP_DIVER NPCs at the skip position. Called externally at event open (08:00).
     *
     * @param count number of divers to spawn (2–4)
     * @return list of spawned SKIP_DIVER NPCs
     */
    public List<NPC> spawnSkipDivers(int count) {
        List<NPC> spawned = new ArrayList<>();
        int safeCount = Math.max(SKIP_DIVER_MIN, Math.min(SKIP_DIVER_MAX, count));
        String[] speeches = {
            "Bags I the sofa.",
            "You were quick.",
            "Don't even think about that telly."
        };
        for (int i = 0; i < safeCount; i++) {
            float offsetX = (random.nextFloat() - 0.5f) * SKIP_ZONE_RADIUS * 2f;
            float offsetZ = (random.nextFloat() - 0.5f) * SKIP_ZONE_RADIUS * 2f;
            NPC diver = new NPC(NPCType.SKIP_DIVER, skipX + offsetX, skipY, skipZ + offsetZ);
            diver.setState(NPCState.WANDERING);
            diver.setSpeechText(speeches[i % speeches.length], 4.0f);
            spawned.add(diver);
            skipDivers.add(diver);
        }
        return spawned;
    }

    /**
     * Trigger a grab by the first available SKIP_DIVER NPC (called every 30 real seconds).
     * Removes one unclaimed item from the skip zone and puts it into the diver's "possession"
     * (tracked internally by removing from skipItems list).
     *
     * @return the material grabbed, or null if no items or divers available
     */
    private Material triggerSkipDiverGrab() {
        if (skipItems.isEmpty() || skipDivers.isEmpty()) {
            return null;
        }
        // Pick a random available diver
        NPC diver = skipDivers.get(random.nextInt(skipDivers.size()));
        // Pick a random item
        int idx = random.nextInt(skipItems.size());
        Material grabbed = skipItems.remove(idx);

        // Track item in the diver's "inventory" via a side list
        skipDiverInventories.computeIfAbsent(diver, k -> new ArrayList<>()).add(grabbed);

        if (!firstItemTakenThisEvent) {
            firstItemTakenThisEvent = true;
        }
        return grabbed;
    }

    /** Simple map from diver NPC to items they've grabbed (for test verification). */
    private final java.util.Map<NPC, List<Material>> skipDiverInventories = new java.util.HashMap<>();

    /**
     * Returns the list of items grabbed by the given SKIP_DIVER NPC.
     */
    public List<Material> getSkipDiverInventory(NPC diver) {
        return skipDiverInventories.getOrDefault(diver, Collections.emptyList());
    }

    /**
     * Forcibly trigger a single skip-diver grab tick (for testing).
     * Returns the grabbed material or null.
     */
    public Material triggerSkipDiverGrabForTesting() {
        return triggerSkipDiverGrab();
    }

    // ── Event management helpers (for tests / world layer) ────────────────────

    /**
     * Manually open the event with a specified loot list (for testing).
     */
    public void openEventForTesting(List<Material> loot) {
        eventActive = true;
        playerItemsTakenThisEvent = 0;
        petitionBoardSpawnedThisEvent = false;
        firstItemTakenThisEvent = false;
        lorryWarningSeeded = false;
        skipDiverGrabTimer = 0f;
        skipDivers.clear();
        skipDiverInventories.clear();
        skipItems.clear();
        skipItems.addAll(loot);
    }

    /**
     * Manually open the event, generating loot randomly (for testing).
     */
    public void openEventForTesting() {
        openEventForTesting(generateLoot());
    }

    /**
     * Set the skip prop world position (called by the world layer on event open).
     */
    public void setSkipPosition(float x, float y, float z) {
        this.skipX = x;
        this.skipY = y;
        this.skipZ = z;
    }

    /**
     * Set the PIGEON_FANCIER NPC reference (called by NPCManager on day 3+).
     */
    public void setPigeonFancier(NPC pigeonFancier) {
        this.pigeonFancier = pigeonFancier;
    }

    /**
     * Force the pigeon fancier pre-claim state (for testing).
     */
    public void triggerPigeonFancierPreclaimForTesting() {
        triggerPigeonFancierPreclaim();
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    /** Returns true if a Bulky Item Day event is currently active. */
    public boolean isEventActive() {
        return eventActive;
    }

    /** Returns the current list of unclaimed items in the skip zone. */
    public List<Material> getSkipItems() {
        return Collections.unmodifiableList(skipItems);
    }

    /** Returns the number of items the player has taken in the current event. */
    public int getPlayerItemsTakenThisEvent() {
        return playerItemsTakenThisEvent;
    }

    /** Returns true if a PETITION_BOARD has been spawned this event. */
    public boolean isPetitionBoardSpawned() {
        return petitionBoardSpawnedThisEvent;
    }

    /** Returns true if the lorry warning rumour should be seeded (09:55). */
    public boolean isLorryWarningSeeded() {
        return lorryWarningSeeded;
    }

    /** Returns true if the PIGEON_FANCIER has pre-claimed BOX_OF_RECORDS. */
    public boolean isPigeonFancierHoldingRecords() {
        return pigeonFancierHoldsRecords;
    }

    /** Returns the list of active SKIP_DIVER NPCs. */
    public List<NPC> getSkipDivers() {
        return Collections.unmodifiableList(skipDivers);
    }

    /** Returns the first-interaction tooltip text (to be shown by the UI layer). */
    public String getFirstInteractionTooltip() {
        return FIRST_INTERACTION_TOOLTIP;
    }

    /** Returns true if the first-interaction tooltip has been shown. */
    public boolean isFirstInteractionTooltipShown() {
        return firstInteractionTooltipShown;
    }

    /** Returns the skip prop X coordinate. */
    public float getSkipX() { return skipX; }

    /** Returns the skip prop Y coordinate. */
    public float getSkipY() { return skipY; }

    /** Returns the skip prop Z coordinate. */
    public float getSkipZ() { return skipZ; }
}

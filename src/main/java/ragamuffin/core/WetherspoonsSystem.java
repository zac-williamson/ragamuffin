package ragamuffin.core;

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
 * Issue #971: The Rusty Anchor Wetherspoons — Cheap Pints, Thursday Curry Club &amp;
 * 7am Breakfast Crowd.
 *
 * <p>Brings The Rusty Anchor (WETHERSPOONS landmark) to life as a fully interactive
 * pub experience distinct from The Ragamuffin Arms.
 *
 * <h3>NPCs</h3>
 * <ul>
 *   <li><b>Gary the Barman</b> ({@link NPCType#BARMAN}) — behind the counter; world-weary.</li>
 *   <li>Two {@link NPCType#BOUNCER} NPCs at the entrance — block Notoriety Tier 4+ only.</li>
 *   <li>7am Breakfast Crowd (07:00–09:00): 2–4 {@link NPCType#COUNCIL_BUILDER} + 1–2
 *       {@link NPCType#PENSIONER} NPCs seated.</li>
 *   <li>Thursday Curry Club (17:00–21:00): 4–6 extra {@link NPCType#PENSIONER} /
 *       {@link NPCType#PUBLIC} NPCs.</li>
 * </ul>
 *
 * <h3>Menu (all cheaper than the Arms)</h3>
 * <ul>
 *   <li>{@code PINT}              — 2 COIN. Warmth +10, Alertness −5.</li>
 *   <li>{@code CHEAP_SPIRITS}     — 1 COIN. Warmth +5, Alertness −10, 20% DRUNK chance.</li>
 *   <li>{@code FULL_ENGLISH}      — 3 COIN. Hunger −60, Warmth +15. 07:00–11:30 only.</li>
 *   <li>{@code BEANS_ON_TOAST}    — 2 COIN. Hunger −40. All day.</li>
 *   <li>{@code CURRY_CLUB_SPECIAL}— 4 COIN. Hunger −80, Warmth +20. Thursdays 17:00–21:00.</li>
 * </ul>
 *
 * <h3>App-at-the-Table Mechanic</h3>
 * Press E on any {@code PUB_TABLE_PROP} to order at the table. If Notoriety is Tier 3+
 * and Gary would refuse service from the bar, table ordering still succeeds. After 10
 * in-game seconds Gary delivers the item and spots the player — refusing all further
 * table orders that visit.
 *
 * <h3>Atmospheric Events</h3>
 * One random event per in-game hour (1 of 5):
 * DOMESTIC_ARGUMENT, SLEEPING_DRUNK, QUIZ_NIGHT (Wednesdays only), FOOTBALL_ON_TELLY,
 * KICKED_OUT.
 *
 * <h3>Bouncer Threshold</h3>
 * Blocks Notoriety Tier 4+ (more lenient than The Ragamuffin Arms' Tier 3+).
 * {@link CriminalRecord.CrimeType#ASSAULT_IN_PUB} results in a permanent ban.
 *
 * <h3>Gary's Rumours</h3>
 * Same buy-a-drink-first system as The Arms barman, but Gary's pool is weighted toward
 * {@link RumourType#LOCAL_EVENT} and {@link RumourType#NEIGHBOURHOOD}, with fewer
 * {@link RumourType#GANG_ACTIVITY} rumours.
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link WarmthSystem}        — Rusty Anchor counts as shelter (handled externally).</li>
 *   <li>{@link WeatherSystem}       — Rain adds +2 extra patron NPCs.</li>
 *   <li>{@link NotorietySystem}     — Bouncer blocks Tier 4+.</li>
 *   <li>{@link CriminalRecord}      — ASSAULT_IN_PUB = permanent ban.</li>
 *   <li>{@link FactionSystem}       — Neutral territory; all factions coexist.</li>
 *   <li>{@link AchievementSystem}   — CURRY_CLUB, SEVEN_AM_PINT, WETHERSPOONS_REGULAR,
 *                                     APP_AT_THE_TABLE, SLEEPING_DRUNK_PICKPOCKET.</li>
 * </ul>
 */
public class WetherspoonsSystem {

    // ── Constants ──────────────────────────────────────────────────────────────

    /** Opening hour (07:00). */
    public static final float OPEN_HOUR = 7.0f;

    /** Closing hour (midnight / 00:00 = 24.0 for modular arithmetic). */
    public static final float CLOSE_HOUR = 24.0f;

    /** Breakfast cutoff: FULL_ENGLISH unavailable after this hour. */
    public static final float BREAKFAST_CUTOFF_HOUR = 11.5f;

    /** Curry Club start hour (17:00). */
    public static final float CURRY_CLUB_START_HOUR = 17.0f;

    /** Curry Club end hour (21:00). */
    public static final float CURRY_CLUB_END_HOUR = 21.0f;

    /** Breakfast crowd start (07:00). */
    public static final float BREAKFAST_CROWD_START_HOUR = 7.0f;

    /** Breakfast crowd end (09:00). */
    public static final float BREAKFAST_CROWD_END_HOUR = 9.0f;

    /** Day-of-week index for Thursday (0=Monday). */
    public static final int THURSDAY_INDEX = 3;

    /** Day-of-week index for Wednesday (0=Monday). */
    public static final int WEDNESDAY_INDEX = 2;

    /** Notoriety score at which the bouncer blocks entry (Tier 4 = 750). */
    public static final int BOUNCER_BLOCK_NOTORIETY = NotorietySystem.TIER_4_THRESHOLD;

    /** Minimum extra Curry Club NPCs. */
    public static final int CURRY_CLUB_NPC_MIN = 4;

    /** Maximum extra Curry Club NPCs. */
    public static final int CURRY_CLUB_NPC_MAX = 6;

    /** Minimum breakfast crowd builder NPCs. */
    public static final int BREAKFAST_BUILDER_MIN = 2;

    /** Maximum breakfast crowd builder NPCs. */
    public static final int BREAKFAST_BUILDER_MAX = 4;

    /** Minimum breakfast crowd pensioner NPCs. */
    public static final int BREAKFAST_PENSIONER_MIN = 1;

    /** Maximum breakfast crowd pensioner NPCs. */
    public static final int BREAKFAST_PENSIONER_MAX = 2;

    /** Extra patron NPCs added during rain/drizzle/thunderstorm. */
    public static final int WEATHER_BONUS_PATRONS = 2;

    /** In-game seconds after table order before Gary delivers. */
    public static final float TABLE_ORDER_DELIVERY_SECONDS = 10.0f;

    /** Radius within which the sleeping DRUNK NPC wakes if the player approaches. */
    public static final float SLEEPING_DRUNK_WAKE_RADIUS = 1.0f;

    /** Radius within which the player can pickpocket the sleeping DRUNK. */
    public static final float SLEEPING_DRUNK_PICKPOCKET_RADIUS = 0.8f;

    /** Minimum COIN pickpocketed from sleeping drunk. */
    public static final int PICKPOCKET_COIN_MIN = 1;

    /** Maximum COIN pickpocketed from sleeping drunk. */
    public static final int PICKPOCKET_COIN_MAX = 3;

    /** Chance of getting DRUNK state when ordering CHEAP_SPIRITS (20%). */
    public static final float CHEAP_SPIRITS_DRUNK_CHANCE = 0.20f;

    /** Total drinks purchased threshold for WETHERSPOONS_REGULAR achievement. */
    public static final int REGULAR_DRINKS_THRESHOLD = 10;

    /** Probability of Football on the Telly event on evenings outside quiz night (20%). */
    public static final float FOOTBALL_EVENT_PROBABILITY = 0.20f;

    // ── Menu prices ────────────────────────────────────────────────────────────

    public static final int PRICE_PINT              = 2;
    public static final int PRICE_CHEAP_SPIRITS     = 1;
    public static final int PRICE_FULL_ENGLISH      = 3;
    public static final int PRICE_BEANS_ON_TOAST    = 2;
    public static final int PRICE_CURRY_CLUB_SPECIAL = 4;

    // ── Atmospheric events ─────────────────────────────────────────────────────

    /** The five atmospheric event types. */
    public enum AtmosphericEvent {
        DOMESTIC_ARGUMENT,
        SLEEPING_DRUNK,
        QUIZ_NIGHT,
        FOOTBALL_ON_TELLY,
        KICKED_OUT
    }

    // ── System state ──────────────────────────────────────────────────────────

    /** Idle — no active transaction or special state. */
    public enum State {
        IDLE,
        SERVING,
        TABLE_ORDER_PENDING,
        TABLE_ORDER_DELIVERED,
        CLOSED
    }

    // ── Order result ──────────────────────────────────────────────────────────

    public enum OrderResult {
        SUCCESS,
        CLOSED,
        BREAKFAST_ONLY,
        CURRY_CLUB_ONLY,
        INSUFFICIENT_FUNDS,
        SERVICE_REFUSED,
        NOT_ON_MENU,
        WRONG_NPC,
        BARRED
    }

    // ── Table order result ─────────────────────────────────────────────────────

    public enum TableOrderResult {
        PENDING,
        DELIVERED,
        TABLE_ORDER_REFUSED,
        INSUFFICIENT_FUNDS,
        NOT_ON_MENU,
        CLOSED
    }

    // ── Bouncer result ─────────────────────────────────────────────────────────

    public enum BouncerResult {
        ALLOWED,
        BLOCKED_NOTORIETY,
        BLOCKED_ASSAULT_RECORD
    }

    // ── MenuItem inner class ──────────────────────────────────────────────────

    /** An item on the Rusty Anchor menu. */
    public static class MenuItem {
        public final Material material;
        public final int price;
        public final int hungerRestore;
        public final int warmthRestore;
        /** True if only available 07:00–11:30. */
        public final boolean breakfastOnly;
        /** True if only available Thursdays 17:00–21:00. */
        public final boolean curryClubOnly;

        public MenuItem(Material material, int price,
                        int hungerRestore, int warmthRestore,
                        boolean breakfastOnly, boolean curryClubOnly) {
            this.material       = material;
            this.price          = price;
            this.hungerRestore  = hungerRestore;
            this.warmthRestore  = warmthRestore;
            this.breakfastOnly  = breakfastOnly;
            this.curryClubOnly  = curryClubOnly;
        }
    }

    /** Full menu. */
    public static final MenuItem[] MENU = {
        new MenuItem(Material.PINT,               PRICE_PINT,               0,   10, false, false),
        new MenuItem(Material.CHEAP_SPIRITS,      PRICE_CHEAP_SPIRITS,      0,    5, false, false),
        new MenuItem(Material.FULL_ENGLISH,       PRICE_FULL_ENGLISH,      60,   15, true,  false),
        new MenuItem(Material.BEANS_ON_TOAST,     PRICE_BEANS_ON_TOAST,    40,    0, false, false),
        new MenuItem(Material.CURRY_CLUB_SPECIAL, PRICE_CURRY_CLUB_SPECIAL, 80,  20, false, true),
    };

    // ── State ──────────────────────────────────────────────────────────────────

    private State state = State.IDLE;

    /** Gary the Barman NPC. */
    private NPC gary;

    /** Bouncer NPCs at the entrance (2). */
    private final List<NPC> bouncers = new ArrayList<>();

    /** Breakfast crowd NPCs (07:00–09:00). */
    private final List<NPC> breakfastNpcs = new ArrayList<>();

    /** Curry Club NPCs (Thursdays 17:00–21:00). */
    private final List<NPC> curryClubNpcs = new ArrayList<>();

    /** Weather bonus patron NPCs. */
    private final List<NPC> weatherPatrons = new ArrayList<>();

    /** Whether breakfast crowd is currently spawned. */
    private boolean breakfastCrowdActive = false;

    /** Whether curry club crowd is currently spawned. */
    private boolean curryClubActive = false;

    /** Whether weather patrons are currently spawned. */
    private boolean weatherPatronsActive = false;

    /** Whether the player has bought a drink this visit (for Gary's rumours). */
    private boolean playerBoughtDrinkThisVisit = false;

    /** Gary's rumour buffer — populated 1 per in-game hour. */
    private final List<String> garyRumourBuffer = new ArrayList<>();

    /** Timer tracking in-game hours for rumour accumulation. */
    private float lastRumourHour = -1f;

    /** Whether the player has already been refused table orders this visit. */
    private boolean tableOrdersRefusedThisVisit = false;

    /** Whether a table order is currently pending delivery. */
    private boolean tableOrderPending = false;

    /** The material ordered at the table (pending delivery). */
    private Material tableOrderMaterial = null;

    /** Timer counting in-game seconds until table order delivery. */
    private float tableOrderTimer = 0f;

    /** Total drinks purchased at the Rusty Anchor (for WETHERSPOONS_REGULAR). */
    private int totalDrinksPurchased = 0;

    /** Last in-game hour an atmospheric event fired. */
    private float lastAtmosphericEventHour = -1f;

    /** Currently sleeping drunk NPC (for SLEEPING_DRUNK event). */
    private NPC sleepingDrunk = null;

    /** Whether the first-entry tooltip has been shown. */
    private boolean firstEntryTooltipShown = false;

    /** Whether the player is currently inside the pub. */
    private boolean playerInPub = false;

    /** Whether ASSAULT_IN_PUB permanent ban is active. */
    private boolean permanentlyBanned = false;

    /** Current active atmospheric event. */
    private AtmosphericEvent currentEvent = null;

    /** Pub coordinates for NPC spawning. */
    private float pubX = 0f, pubY = 1f, pubZ = 0f;

    // ── Optional system references ─────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;

    private final Random random;

    // ── Construction ───────────────────────────────────────────────────────────

    public WetherspoonsSystem() {
        this(new Random());
    }

    public WetherspoonsSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    /** Set the pub position for NPC placement. */
    public void setPubPosition(float x, float y, float z) {
        this.pubX = x;
        this.pubY = y;
        this.pubZ = z;
    }

    // ── Main update ────────────────────────────────────────────────────────────

    /**
     * Update the system once per frame.
     *
     * @param delta      real-time seconds since last frame
     * @param gameHour   current in-game hour (0.0–24.0)
     * @param dayOfWeek  current day-of-week index (0=Monday … 6=Sunday)
     * @param weather    current weather
     */
    public void update(float delta, float gameHour, int dayOfWeek, Weather weather) {
        float normHour = gameHour % 24.0f;

        // Update state based on hours
        if (isOpen(normHour)) {
            state = State.IDLE;

            // Spawn Gary if not present
            if (gary == null) {
                gary = new NPC(NPCType.BARMAN, pubX + 1, pubY, pubZ);
                gary.setName("Gary");
                gary.setSpeechText("What can I get you, mate?", 0f);
            }

            // Spawn bouncers if not present
            if (bouncers.isEmpty()) {
                bouncers.add(new NPC(NPCType.BOUNCER, pubX - 1, pubY, pubZ - 2));
                bouncers.add(new NPC(NPCType.BOUNCER, pubX + 1, pubY, pubZ - 2));
            }

            // Manage breakfast crowd
            boolean breakfastTime = isBreakfastCrowdTime(normHour);
            if (breakfastTime && !breakfastCrowdActive) {
                spawnBreakfastCrowd();
            } else if (!breakfastTime && breakfastCrowdActive) {
                despawnBreakfastCrowd();
            }

            // Manage curry club crowd
            boolean curryClubTime = isCurryClubTime(normHour, dayOfWeek);
            if (curryClubTime && !curryClubActive) {
                spawnCurryClubCrowd();
            } else if (!curryClubTime && curryClubActive) {
                despawnCurryClubCrowd();
            }

            // Manage weather bonus patrons
            boolean rainBonus = (weather == Weather.RAIN
                    || weather == Weather.DRIZZLE
                    || weather == Weather.THUNDERSTORM);
            if (rainBonus && !weatherPatronsActive) {
                spawnWeatherPatrons();
            } else if (!rainBonus && weatherPatronsActive) {
                despawnWeatherPatrons();
            }

            // Accumulate Gary rumours (one per in-game hour)
            updateGaryRumours(normHour);

            // Atmospheric events (one per hour)
            updateAtmosphericEvents(normHour, dayOfWeek);

            // Table order delivery timer
            if (tableOrderPending) {
                tableOrderTimer += delta;
            }

        } else {
            // Closed
            state = State.CLOSED;
            closePub();
        }
    }

    // ── Opening/closing ────────────────────────────────────────────────────────

    private void closePub() {
        if (gary != null) {
            gary = null;
        }
        bouncers.clear();
        despawnBreakfastCrowd();
        despawnCurryClubCrowd();
        despawnWeatherPatrons();
        playerBoughtDrinkThisVisit = false;
        tableOrdersRefusedThisVisit = false;
        tableOrderPending = false;
        tableOrderMaterial = null;
        tableOrderTimer = 0f;
        sleepingDrunk = null;
        currentEvent = null;
        playerInPub = false;
    }

    // ── Bouncer check ──────────────────────────────────────────────────────────

    /**
     * Check whether the bouncer allows the player entry.
     *
     * @return ALLOWED, BLOCKED_NOTORIETY, or BLOCKED_ASSAULT_RECORD
     */
    public BouncerResult checkBouncer() {
        // Permanent ban (ASSAULT_IN_PUB)
        if (permanentlyBanned) {
            return BouncerResult.BLOCKED_ASSAULT_RECORD;
        }
        if (criminalRecord != null
                && criminalRecord.getCount(CriminalRecord.CrimeType.ASSAULT_IN_PUB) > 0) {
            permanentlyBanned = true;
            if (!bouncers.isEmpty()) {
                bouncers.get(0).setSpeechText("Not in here, mate. You're barred.", 5f);
            }
            return BouncerResult.BLOCKED_ASSAULT_RECORD;
        }

        // Notoriety Tier 4+
        if (notorietySystem != null
                && notorietySystem.getNotoriety() >= BOUNCER_BLOCK_NOTORIETY) {
            if (!bouncers.isEmpty()) {
                bouncers.get(0).setSpeechText("Not tonight, mate.", 5f);
            }
            return BouncerResult.BLOCKED_NOTORIETY;
        }

        return BouncerResult.ALLOWED;
    }

    // ── Ordering from Gary ─────────────────────────────────────────────────────

    /**
     * Attempt to order an item from Gary at the bar.
     *
     * @param npc         the NPC the player pressed E on (must be BARMAN)
     * @param material    the requested item
     * @param inventory   the player's inventory
     * @param gameHour    current in-game hour
     * @param dayOfWeek   current day of week (0=Monday)
     * @return OrderResult
     */
    public OrderResult order(NPC npc, Material material, Inventory inventory,
                             float gameHour, int dayOfWeek) {
        if (npc == null || npc.getType() != NPCType.BARMAN) {
            return OrderResult.WRONG_NPC;
        }

        float normHour = gameHour % 24.0f;

        if (!isOpen(normHour)) {
            return OrderResult.CLOSED;
        }

        if (permanentlyBanned
                || (criminalRecord != null
                    && criminalRecord.getCount(CriminalRecord.CrimeType.ASSAULT_IN_PUB) > 0)) {
            permanentlyBanned = true;
            npc.setSpeechText("Not in here, mate. You're barred.", 5f);
            return OrderResult.BARRED;
        }

        MenuItem item = findMenuItem(material);
        if (item == null) {
            return OrderResult.NOT_ON_MENU;
        }

        // Breakfast-only check
        if (item.breakfastOnly && normHour >= BREAKFAST_CUTOFF_HOUR) {
            npc.setSpeechText("Kitchen's done breakfast now, mate.", 5f);
            return OrderResult.BREAKFAST_ONLY;
        }

        // Curry Club check
        if (item.curryClubOnly && !isCurryClubTime(normHour, dayOfWeek)) {
            npc.setSpeechText("Curry club's Thursdays, mate. Come back Thursday.", 5f);
            return OrderResult.CURRY_CLUB_ONLY;
        }

        // Afford check
        if (inventory == null || inventory.getItemCount(Material.COIN) < item.price) {
            npc.setSpeechText("You ain't got enough, mate.", 5f);
            return OrderResult.INSUFFICIENT_FUNDS;
        }

        // Process transaction
        state = State.SERVING;
        inventory.removeItem(Material.COIN, item.price);
        inventory.addItem(material, 1);
        npc.setSpeechText("There you go, mate.", 5f);

        playerBoughtDrinkThisVisit = true;
        trackDrinkPurchase(material, normHour, dayOfWeek);

        state = State.IDLE;
        return OrderResult.SUCCESS;
    }

    // ── App-at-the-Table Mechanic ──────────────────────────────────────────────

    /**
     * Player presses E on a PUB_TABLE_PROP to initiate a table order.
     * If a table order is already pending, this delivers it.
     *
     * @param material     the item to order (or null to deliver pending order)
     * @param inventory    the player's inventory
     * @param gameHour     current in-game hour
     * @param dayOfWeek    current day of week
     * @return TableOrderResult
     */
    public TableOrderResult interactWithTable(Material material, Inventory inventory,
                                               float gameHour, int dayOfWeek) {
        float normHour = gameHour % 24.0f;

        if (!isOpen(normHour)) {
            return TableOrderResult.CLOSED;
        }

        // Deliver a pending order if enough time has passed
        if (tableOrderPending && tableOrderMaterial != null) {
            if (tableOrderTimer >= TABLE_ORDER_DELIVERY_SECONDS) {
                return deliverTableOrder(inventory);
            }
            // Still waiting for delivery
            return TableOrderResult.PENDING;
        }

        // Check if Gary has refused table orders this visit
        if (tableOrdersRefusedThisVisit) {
            return TableOrderResult.TABLE_ORDER_REFUSED;
        }

        // Start a new table order
        if (material == null) {
            return TableOrderResult.NOT_ON_MENU;
        }

        MenuItem item = findMenuItem(material);
        if (item == null) {
            return TableOrderResult.NOT_ON_MENU;
        }

        // Breakfast-only check
        if (item.breakfastOnly && normHour >= BREAKFAST_CUTOFF_HOUR) {
            return TableOrderResult.NOT_ON_MENU;
        }

        // Curry Club check
        if (item.curryClubOnly && !isCurryClubTime(normHour, dayOfWeek)) {
            return TableOrderResult.NOT_ON_MENU;
        }

        // Afford check (deduct at order time)
        if (inventory == null || inventory.getItemCount(Material.COIN) < item.price) {
            return TableOrderResult.INSUFFICIENT_FUNDS;
        }

        // Begin pending order — deduct coin now
        inventory.removeItem(Material.COIN, item.price);
        tableOrderMaterial = material;
        tableOrderPending = true;
        tableOrderTimer = 0f;
        state = State.TABLE_ORDER_PENDING;

        return TableOrderResult.PENDING;
    }

    /** Deliver the pending table order and mark Gary as having spotted the player. */
    private TableOrderResult deliverTableOrder(Inventory inventory) {
        Material mat = tableOrderMaterial;
        tableOrderPending = false;
        tableOrderMaterial = null;
        tableOrderTimer = 0f;
        tableOrdersRefusedThisVisit = true; // Gary spots player; refuses further table orders

        if (inventory != null && mat != null) {
            inventory.addItem(mat, 1);
        }

        if (gary != null) {
            gary.setSpeechText("Oi — you've got some nerve!", 5f);
        }

        playerBoughtDrinkThisVisit = true;
        trackDrinkPurchase(mat, -1f, -1); // hour/day unknown at delivery time; skip time checks

        state = State.TABLE_ORDER_DELIVERED;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.APP_AT_THE_TABLE);
        }

        return TableOrderResult.DELIVERED;
    }

    // ── Gary's Rumours ─────────────────────────────────────────────────────────

    /**
     * Player presses E on Gary after already having bought a drink.
     * Returns a rumour string if one is available, or a refusal.
     *
     * @return a rumour string, or a "buy something first" message if none available
     */
    public String hearRumourFromGary() {
        if (!playerBoughtDrinkThisVisit) {
            if (gary != null) {
                gary.setSpeechText("Buy something and I'll have a chat.", 5f);
            }
            return "Buy something and I'll have a chat.";
        }

        if (garyRumourBuffer.isEmpty()) {
            if (gary != null) {
                gary.setSpeechText("Nothing to report, mate. Quiet night.", 5f);
            }
            return "Nothing to report, mate. Quiet night.";
        }

        String rumour = garyRumourBuffer.remove(0);
        playerBoughtDrinkThisVisit = false; // must buy again for next rumour
        if (gary != null) {
            gary.setSpeechText(rumour, 6f);
        }

        // Seed into RumourNetwork
        if (rumourNetwork != null && gary != null) {
            RumourType type = pickGaryRumourType();
            rumourNetwork.addRumour(gary, new Rumour(type, rumour));
        }

        return rumour;
    }

    // ── Sleeping drunk pickpocket ──────────────────────────────────────────────

    /**
     * Attempt to pickpocket the sleeping drunk NPC.
     *
     * @param playerX   player X position
     * @param playerZ   player Z position
     * @param inventory player's inventory
     * @return true if the pickpocket succeeded
     */
    public boolean pickpocketSleepingDrunk(float playerX, float playerZ, Inventory inventory) {
        if (sleepingDrunk == null || !sleepingDrunk.isAlive()) return false;

        float dx = sleepingDrunk.getPosition().x - playerX;
        float dz = sleepingDrunk.getPosition().z - playerZ;
        float distSq = dx * dx + dz * dz;

        if (distSq > SLEEPING_DRUNK_PICKPOCKET_RADIUS * SLEEPING_DRUNK_PICKPOCKET_RADIUS) {
            return false;
        }

        int coins = PICKPOCKET_COIN_MIN
                + random.nextInt(PICKPOCKET_COIN_MAX - PICKPOCKET_COIN_MIN + 1);
        if (inventory != null) {
            inventory.addItem(Material.COIN, coins);
        }

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SLEEPING_DRUNK_PICKPOCKET);
        }

        return true;
    }

    /**
     * Check if the sleeping drunk should wake up due to player proximity.
     *
     * @param playerX player X position
     * @param playerZ player Z position
     * @return true if the drunk woke up
     */
    public boolean checkSleepingDrunkWakes(float playerX, float playerZ) {
        if (sleepingDrunk == null || !sleepingDrunk.isAlive()) return false;

        float dx = sleepingDrunk.getPosition().x - playerX;
        float dz = sleepingDrunk.getPosition().z - playerZ;
        float distSq = dx * dx + dz * dz;

        if (distSq <= SLEEPING_DRUNK_WAKE_RADIUS * SLEEPING_DRUNK_WAKE_RADIUS) {
            sleepingDrunk.setState(NPCState.WANDERING);
            sleepingDrunk = null;
            return true;
        }

        return false;
    }

    // ── Player entry / exit ────────────────────────────────────────────────────

    /**
     * Called when the player enters the pub.
     *
     * @return first-entry tooltip text, or null if already shown
     */
    public String onPlayerEnter() {
        playerInPub = true;
        playerBoughtDrinkThisVisit = false;
        tableOrdersRefusedThisVisit = false;
        tableOrderPending = false;
        tableOrderMaterial = null;
        tableOrderTimer = 0f;

        if (!firstEntryTooltipShown) {
            firstEntryTooltipShown = true;
            return "The Rusty Anchor. Same as everywhere else. Different carpet.";
        }
        return null;
    }

    /** Called when the player exits the pub. */
    public void onPlayerExit() {
        playerInPub = false;
        playerBoughtDrinkThisVisit = false;
        tableOrdersRefusedThisVisit = false;
        tableOrderPending = false;
        tableOrderMaterial = null;
        tableOrderTimer = 0f;
        sleepingDrunk = null;
    }

    // ── Atmospheric Events ─────────────────────────────────────────────────────

    /** Manually trigger a specific atmospheric event (for testing). */
    public void triggerAtmosphericEvent(AtmosphericEvent event) {
        currentEvent = event;
        executeEvent(event, 0, -1);
    }

    private void updateAtmosphericEvents(float normHour, int dayOfWeek) {
        int currentHourInt = (int) normHour;
        if (currentHourInt != (int) lastAtmosphericEventHour) {
            lastAtmosphericEventHour = currentHourInt;
            AtmosphericEvent event = pickAtmosphericEvent(normHour, dayOfWeek);
            if (event != null) {
                currentEvent = event;
                executeEvent(event, normHour, dayOfWeek);
            }
        }
    }

    private AtmosphericEvent pickAtmosphericEvent(float normHour, int dayOfWeek) {
        // Quiz Night: Wednesdays 19:00–22:00 only
        if (dayOfWeek == WEDNESDAY_INDEX && normHour >= 19.0f && normHour < 22.0f) {
            if (random.nextFloat() < 0.5f) {
                return AtmosphericEvent.QUIZ_NIGHT;
            }
        }

        // Football on the Telly: 20% chance evenings outside quiz night
        if (normHour >= 19.0f && normHour < 22.0f
                && !(dayOfWeek == WEDNESDAY_INDEX)) {
            if (random.nextFloat() < FOOTBALL_EVENT_PROBABILITY) {
                return AtmosphericEvent.FOOTBALL_ON_TELLY;
            }
        }

        // Otherwise pick randomly from remaining events
        AtmosphericEvent[] pool = {
            AtmosphericEvent.DOMESTIC_ARGUMENT,
            AtmosphericEvent.SLEEPING_DRUNK,
            AtmosphericEvent.KICKED_OUT
        };
        return pool[random.nextInt(pool.length)];
    }

    private void executeEvent(AtmosphericEvent event, float normHour, int dayOfWeek) {
        switch (event) {
            case DOMESTIC_ARGUMENT:
                // Two PUBLIC NPCs begin arguing
                NPC arguer1 = new NPC(NPCType.PUBLIC, pubX + 2, pubY, pubZ + 2);
                NPC arguer2 = new NPC(NPCType.PUBLIC, pubX + 3, pubY, pubZ + 2);
                arguer1.setSpeechText("I can't believe you said that!", 8f);
                arguer2.setSpeechText("Don't start with me!", 8f);
                break;

            case SLEEPING_DRUNK:
                // Spawn a DRUNK NPC slumped at a table
                sleepingDrunk = new NPC(NPCType.DRUNK, pubX + 4, pubY, pubZ + 3);
                sleepingDrunk.setState(NPCState.IDLE);
                sleepingDrunk.setSpeechText("Zzzz...", 0f);
                break;

            case QUIZ_NIGHT:
                // Seed LOCAL_EVENT rumour for quiz night
                if (rumourNetwork != null && gary != null) {
                    rumourNetwork.addRumour(gary,
                        new Rumour(RumourType.LOCAL_EVENT,
                            "Quiz night at the Rusty Anchor tonight — few quid prize if you're sharp."));
                }
                break;

            case FOOTBALL_ON_TELLY:
                // TV activates — nearby NPCs face it
                if (gary != null) {
                    gary.setSpeechText("Come on!", 5f);
                }
                break;

            case KICKED_OUT:
                // BOUNCER ejects a DRUNK NPC; seed TROUBLE_AT_PUB rumour
                NPC drunk = new NPC(NPCType.DRUNK, pubX + 1, pubY, pubZ);
                drunk.setState(NPCState.WANDERING);
                if (rumourNetwork != null && gary != null) {
                    rumourNetwork.addRumour(gary,
                        new Rumour(RumourType.TROUBLE_AT_PUB,
                            "Someone got kicked out of the Rusty Anchor — made a right scene."));
                }
                break;

            default:
                break;
        }
    }

    // ── NPC spawn/despawn helpers ──────────────────────────────────────────────

    private void spawnBreakfastCrowd() {
        breakfastCrowdActive = true;
        int builders = BREAKFAST_BUILDER_MIN
                + random.nextInt(BREAKFAST_BUILDER_MAX - BREAKFAST_BUILDER_MIN + 1);
        int pensioners = BREAKFAST_PENSIONER_MIN
                + random.nextInt(BREAKFAST_PENSIONER_MAX - BREAKFAST_PENSIONER_MIN + 1);

        for (int i = 0; i < builders; i++) {
            NPC n = new NPC(NPCType.COUNCIL_BUILDER, pubX + 2 + i, pubY, pubZ + 2);
            n.setState(NPCState.IDLE);
            breakfastNpcs.add(n);
        }
        for (int i = 0; i < pensioners; i++) {
            NPC n = new NPC(NPCType.PENSIONER, pubX + 2 + builders + i, pubY, pubZ + 2);
            n.setState(NPCState.IDLE);
            breakfastNpcs.add(n);
        }
    }

    private void despawnBreakfastCrowd() {
        breakfastNpcs.clear();
        breakfastCrowdActive = false;
    }

    private void spawnCurryClubCrowd() {
        curryClubActive = true;
        int count = CURRY_CLUB_NPC_MIN
                + random.nextInt(CURRY_CLUB_NPC_MAX - CURRY_CLUB_NPC_MIN + 1);

        for (int i = 0; i < count; i++) {
            NPCType type = (random.nextBoolean()) ? NPCType.PENSIONER : NPCType.PUBLIC;
            NPC n = new NPC(type, pubX + 2 + (i % 4), pubY, pubZ + 3 + (i / 4));
            n.setState(NPCState.IDLE);
            curryClubNpcs.add(n);
        }

        // Update chalkboard speech
        if (gary != null) {
            gary.setSpeechText("Curry Club TONIGHT — £4. Get in.", 0f);
        }

        // Seed LOCAL_EVENT rumour
        if (rumourNetwork != null && gary != null) {
            rumourNetwork.addRumour(gary,
                new Rumour(RumourType.LOCAL_EVENT,
                    "Rusty Anchor's doing the curry club again — four quid, can't go wrong."));
        }
    }

    private void despawnCurryClubCrowd() {
        curryClubNpcs.clear();
        curryClubActive = false;
    }

    private void spawnWeatherPatrons() {
        weatherPatronsActive = true;
        for (int i = 0; i < WEATHER_BONUS_PATRONS; i++) {
            NPC n = new NPC(NPCType.PUBLIC, pubX + 6 + i, pubY, pubZ + 2);
            n.setState(NPCState.IDLE);
            weatherPatrons.add(n);
        }
    }

    private void despawnWeatherPatrons() {
        weatherPatrons.clear();
        weatherPatronsActive = false;
    }

    // ── Rumour accumulation ────────────────────────────────────────────────────

    private void updateGaryRumours(float normHour) {
        int currentHourInt = (int) normHour;
        if (currentHourInt != (int) lastRumourHour) {
            lastRumourHour = currentHourInt;
            garyRumourBuffer.add(pickGaryRumourText());
        }
    }

    private String pickGaryRumourText() {
        String[] rumours = {
            "Heard there's a planning meeting about them flats on Thursday.",
            "Someone's been running a car boot near the industrial estate, apparently.",
            "Bloke in the corner reckons he's got a van full of flat-screen TVs. Cash only.",
            "Council's talking about closing the car park again. Same every year.",
            "Old girl on Maple Street finally sold up. Gone to her daughter's.",
            "There's a collection going round for the youth centre roof.",
            "Lads from the estate won five-a-side last weekend. First time in years.",
            "Someone's been doing wheelies outside the school. PCSO's livid.",
            "Heard the off-licence is changing hands. New family coming in.",
            "Market's moving to Saturdays apparently. Council's idea, obviously.",
        };
        return rumours[random.nextInt(rumours.length)];
    }

    private RumourType pickGaryRumourType() {
        // Gary's pool: weighted toward LOCAL_EVENT and NEIGHBOURHOOD
        // 40% LOCAL_EVENT, 40% NEIGHBOURHOOD, 20% GANG_ACTIVITY
        float roll = random.nextFloat();
        if (roll < 0.40f) return RumourType.LOCAL_EVENT;
        if (roll < 0.80f) return RumourType.NEIGHBOURHOOD;
        return RumourType.GANG_ACTIVITY;
    }

    // ── Achievement tracking ───────────────────────────────────────────────────

    private void trackDrinkPurchase(Material material, float gameHour, int dayOfWeek) {
        if (material == null) return;

        // Count drinks (PINT and CHEAP_SPIRITS only, not food)
        if (material == Material.PINT || material == Material.CHEAP_SPIRITS) {
            totalDrinksPurchased++;

            // SEVEN_AM_PINT: buy a PINT before 08:00
            if (material == Material.PINT && gameHour >= 0f && gameHour < 8.0f) {
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.SEVEN_AM_PINT);
                }
            }

            // WETHERSPOONS_REGULAR: 10 drinks total
            if (achievementSystem != null) {
                achievementSystem.increment(AchievementType.WETHERSPOONS_REGULAR);
            }
        }

        // CURRY_CLUB: eat Curry Club Special on a Thursday
        if (material == Material.CURRY_CLUB_SPECIAL && dayOfWeek == THURSDAY_INDEX) {
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CURRY_CLUB);
            }
        }
    }

    // ── Menu helpers ───────────────────────────────────────────────────────────

    /** Find a menu item by material, or null if not on the menu. */
    public MenuItem findMenuItem(Material material) {
        for (MenuItem item : MENU) {
            if (item.material == material) return item;
        }
        return null;
    }

    /**
     * Returns which menu items are currently available given the time / day.
     *
     * @param gameHour  current in-game hour
     * @param dayOfWeek current day of week (0=Monday)
     * @return list of available MenuItem
     */
    public List<MenuItem> getAvailableMenu(float gameHour, int dayOfWeek) {
        float normHour = gameHour % 24.0f;
        List<MenuItem> available = new ArrayList<>();
        for (MenuItem item : MENU) {
            if (item.breakfastOnly && normHour >= BREAKFAST_CUTOFF_HOUR) continue;
            if (item.curryClubOnly && !isCurryClubTime(normHour, dayOfWeek)) continue;
            available.add(item);
        }
        return available;
    }

    // ── Predicates ─────────────────────────────────────────────────────────────

    /** True if the pub is currently open. */
    public boolean isOpen(float gameHour) {
        float normHour = gameHour % 24.0f;
        return normHour >= OPEN_HOUR;
    }

    /** True if it is currently Curry Club time (Thursday 17:00–21:00). */
    public boolean isCurryClubTime(float gameHour, int dayOfWeek) {
        float normHour = gameHour % 24.0f;
        return dayOfWeek == THURSDAY_INDEX
                && normHour >= CURRY_CLUB_START_HOUR
                && normHour < CURRY_CLUB_END_HOUR;
    }

    /** True if it is currently breakfast crowd time (07:00–09:00). */
    public boolean isBreakfastCrowdTime(float gameHour) {
        float normHour = gameHour % 24.0f;
        return normHour >= BREAKFAST_CROWD_START_HOUR && normHour < BREAKFAST_CROWD_END_HOUR;
    }

    // ── Query methods ──────────────────────────────────────────────────────────

    /** Returns Gary the Barman NPC, or null if not spawned. */
    public NPC getGary() { return gary; }

    /** Returns the bouncer NPCs. */
    public List<NPC> getBouncers() { return bouncers; }

    /** Returns the breakfast crowd NPCs. */
    public List<NPC> getBreakfastNpcs() { return breakfastNpcs; }

    /** Returns the curry club crowd NPCs. */
    public List<NPC> getCurryClubNpcs() { return curryClubNpcs; }

    /** Returns the weather bonus patron NPCs. */
    public List<NPC> getWeatherPatrons() { return weatherPatrons; }

    /** Returns the sleeping drunk NPC, or null if none. */
    public NPC getSleepingDrunk() { return sleepingDrunk; }

    /** Returns the current system state. */
    public State getState() { return state; }

    /** Returns the current active atmospheric event, or null. */
    public AtmosphericEvent getCurrentEvent() { return currentEvent; }

    /** Returns true if Gary's rumour buffer is non-empty. */
    public boolean hasRumours() { return !garyRumourBuffer.isEmpty(); }

    /** Returns the number of rumours in Gary's buffer. */
    public int getRumourBufferSize() { return garyRumourBuffer.size(); }

    /** Returns the total number of drinks purchased at the Rusty Anchor. */
    public int getTotalDrinksPurchased() { return totalDrinksPurchased; }

    /** Returns true if the curry club is currently active. */
    public boolean isCurryClubActive() { return curryClubActive; }

    /** Returns true if the breakfast crowd is currently active. */
    public boolean isBreakfastCrowdActive() { return breakfastCrowdActive; }

    /** Returns true if the player has been permanently banned. */
    public boolean isPermanentlyBanned() { return permanentlyBanned; }

    /** Returns true if the player bought a drink this visit. */
    public boolean hasPlayerBoughtDrinkThisVisit() { return playerBoughtDrinkThisVisit; }

    /** Returns true if Gary has refused table orders this visit. */
    public boolean isTableOrdersRefusedThisVisit() { return tableOrdersRefusedThisVisit; }

    /** Returns the table order delivery timer (seconds). */
    public float getTableOrderTimer() { return tableOrderTimer; }

    /** Returns true if a table order is pending. */
    public boolean isTableOrderPending() { return tableOrderPending; }

    /** Returns the first-entry tooltip shown flag. */
    public boolean isFirstEntryTooltipShown() { return firstEntryTooltipShown; }

    // ── Test helpers ───────────────────────────────────────────────────────────

    /** Directly seed a rumour into Gary's buffer (for testing). */
    public void addRumourToBufferForTesting(String rumour) {
        garyRumourBuffer.add(rumour);
    }

    /** Directly set total drinks purchased (for testing). */
    public void setTotalDrinksPurchasedForTesting(int count) {
        this.totalDrinksPurchased = count;
    }

    /** Directly set the table order timer (for testing). */
    public void setTableOrderTimerForTesting(float seconds) {
        this.tableOrderTimer = seconds;
    }

    /** Directly set the permanently banned flag (for testing). */
    public void setPermanentlyBannedForTesting(boolean banned) {
        this.permanentlyBanned = banned;
    }

    /** Directly set the playerBoughtDrinkThisVisit flag (for testing). */
    public void setPlayerBoughtDrinkThisVisitForTesting(boolean bought) {
        this.playerBoughtDrinkThisVisit = bought;
    }

    /** Directly set the first entry tooltip shown (for testing). */
    public void setFirstEntryTooltipShownForTesting(boolean shown) {
        this.firstEntryTooltipShown = shown;
    }

    /** Directly place a sleeping drunk NPC (for testing). */
    public void setSleepingDrunkForTesting(NPC drunk) {
        this.sleepingDrunk = drunk;
    }

    /** Directly set curry club NPCs (for testing). */
    public void setCurryClubActiveForTesting(boolean active) {
        this.curryClubActive = active;
    }
}

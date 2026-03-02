package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Issue #1102: Northfield Indoor Market — Stall Rental, Knock-Offs &amp; the
 * Trading Standards Raid.
 *
 * <p>A covered market hall open Tuesday, Friday and Saturday 08:00–16:00.
 * Players can rent a stall from Ray the Market Manager, place items for sale,
 * pickpocket in the crowds, and survive (or flee) the periodic Trading Standards
 * raid that clears out contraband.
 *
 * <h3>Market Hours</h3>
 * <ul>
 *   <li>Open: Tue/Fri/Sat 08:00–16:00 only.</li>
 *   <li>{@link #MARKET_SHUTTER_PROP} blocks entry outside these hours.</li>
 *   <li>Destroying the shutter: +8 Notoriety, CRIMINAL_DAMAGE offence.</li>
 * </ul>
 *
 * <h3>Traders</h3>
 * <ul>
 *   <li><b>Dave</b> — second-hand electronics (incl. STOLEN_PHONE).</li>
 *   <li><b>Sheila</b> — clothes and knock-offs.</li>
 *   <li><b>Mo</b> — DVDs and counterfeit goods; flees before raids.</li>
 *   <li><b>Brenda</b> — hot food + TEA_URN_PROP; free tea during rain.</li>
 * </ul>
 *
 * <h3>Stall Rental</h3>
 * <ul>
 *   <li>3 COIN / market day, from Ray (MARKET_MANAGER).</li>
 *   <li>Refused if player Notoriety Tier ≥ 4.</li>
 *   <li>Up to {@link #MAX_STALL_ITEMS} items for sale.</li>
 *   <li>MARKET_PUNTER NPCs browse every {@link #STALL_BROWSE_INTERVAL_MINS} in-game minutes.</li>
 * </ul>
 *
 * <h3>Trading Standards Raid</h3>
 * <ul>
 *   <li>{@link #RAID_DAILY_CHANCE} per market day, between 10:00–15:00.</li>
 *   <li>2 TRADING_STANDARDS NPCs walk each stall.</li>
 *   <li>Contraband confiscated; +20 Notoriety, +1 wanted star, TRADING_STANDARDS_RAID crime.</li>
 * </ul>
 */
public class IndoorMarketSystem {

    // ── Public constants ──────────────────────────────────────────────────────

    /** Market opening hour. */
    public static final float OPEN_HOUR = 8.0f;

    /** Market closing hour. */
    public static final float CLOSE_HOUR = 16.0f;

    /**
     * Days on which the market is open: 0=Mon … 5=Sat, 6=Sun.
     * Tue=1, Fri=4, Sat=5 (using {@code (dayCount - 1) % 7}).
     */
    public static final int[] MARKET_DAYS = {1, 4, 5};  // Tue=1, Fri=4, Sat=5

    /** Cost in COIN to rent a stall for one market day. */
    public static final int STALL_RENT_COST = 3;

    /** In-game minutes between punter browse cycles. */
    public static final float STALL_BROWSE_INTERVAL_MINS = 2.0f;

    /** Price ≤ 150% market value → punter always buys. */
    public static final float PUNTER_BUY_THRESHOLD_FAIR = 1.5f;

    /** Price ≤ 200% market value → punter may buy at reduced chance. */
    public static final float PUNTER_BUY_THRESHOLD_HIGH = 2.0f;

    /** Chance of purchase when price is 151–200% of market value. */
    public static final float PUNTER_BUY_CHANCE_HIGH_PRICE = 0.40f;

    /** Maximum items the player may place in their rented stall. */
    public static final int MAX_STALL_ITEMS = 6;

    /** Minimum punters spawned on a market day. */
    public static final int PUNTER_CROWD_MIN = 8;

    /** Maximum punters spawned on a market day. */
    public static final int PUNTER_CROWD_MAX = 12;

    /** Base pickpocket success probability. */
    public static final float PICKPOCKET_BASE_SUCCESS = 0.55f;

    /** Notoriety tier ≥ 3 penalty to pickpocket chance. */
    public static final float PICKPOCKET_NOTORIETY_PENALTY = 0.20f;

    /** Minimum nearby NPCs required for crowd cover on a pickpocket attempt. */
    public static final int PICKPOCKET_CROWD_MIN = 3;

    /** Daily probability of a Trading Standards raid occurring. */
    public static final float RAID_DAILY_CHANCE = 0.15f;

    /** Earliest in-game hour a raid can begin. */
    public static final float RAID_WINDOW_START = 10.0f;

    /** Latest in-game hour a raid can begin. */
    public static final float RAID_WINDOW_END = 15.0f;

    /** Frames the player has to vacate their stall before arrest during a raid. */
    public static final int RAID_ESCAPE_FRAMES = 60;

    /** Duration of a raid in in-game minutes. */
    public static final int RAID_DURATION_MINS = 10;

    /** Fraction by which crowd size is reduced during rain. */
    public static final float RAIN_CROWD_REDUCTION = 0.30f;

    /** Dave's barter acceptance percentage (haggler). */
    public static final int DAVE_BARTER_ACCEPT_PCT = 60;

    /** Sheila's barter acceptance percentage (generous). */
    public static final int SHEILA_BARTER_ACCEPT_PCT = 90;

    /** Mo's barter acceptance percentage (nervous). */
    public static final int MO_BARTER_ACCEPT_PCT = 50;

    // ── In-game-minute conversion ─────────────────────────────────────────────

    /** Real-seconds per in-game minute (default time speed: 1 real-sec = 6 in-game minutes). */
    private static final float SECONDS_PER_GAME_MINUTE = 1.0f / 6.0f;

    // ── Shutter state ─────────────────────────────────────────────────────────

    /** Whether the market shutters are currently up (impassable). */
    private boolean shutterDown = true;

    // ── Player stall state ────────────────────────────────────────────────────

    /** Whether the player currently has a rented stall. */
    private boolean playerStallActive = false;

    /** Items the player has placed for sale: list of [material, quantity, price]. */
    private final List<StallItem> playerStallItems = new ArrayList<>();

    /** In-game hour when the player's stall rental expires. */
    private float stallRentExpiry = 0f;

    /** COIN earned by the player from stall sales today. */
    private int stallSalesToday = 0;

    /** Total completed rentals where the player sold ≥ 1 item. */
    private int stallRentalsCompleted = 0;

    /** Timer (real seconds) since last punter browse cycle. */
    private float browseTimer = 0f;

    /** COIN earned from stall sales this Saturday (for SATURDAY_MARKET_KING). */
    private int saturdaySales = 0;

    // ── Raid state ────────────────────────────────────────────────────────────

    /** Whether a Trading Standards raid is currently active. */
    private boolean raidActive = false;

    /** In-game minute at which the current raid started. */
    private int raidStartMinute = 0;

    /** Whether a raid has already been triggered today. */
    private boolean raidTriggeredToday = false;

    /** Frame counter for the raid escape window. */
    private int raidEscapeFrames = 0;

    /** Whether the player escaped a raid with contraband (for LEGS_IT achievement). */
    private boolean escapedRaidWithContraband = false;

    /** NPCs spawned as Trading Standards officers for the current raid. */
    private final List<NPC> tradingStandardsNpcs = new ArrayList<>();

    // ── Pickpocket tracking ───────────────────────────────────────────────────

    /** Number of successful pickpockets on punters this market day. */
    private int pickpocketsTodayCount = 0;

    /** Tracks coin carried by each spawned punter NPC (NPC identity → coin). */
    private final Map<NPC, Integer> punterCoinMap = new IdentityHashMap<>();

    // ── Achievement tracking ──────────────────────────────────────────────────

    private boolean achievementMarketRegular = false;
    private boolean achievementSovereignTrading = false;
    private boolean achievementLegsIt = false;
    private boolean achievementCrowdWorker = false;
    private boolean achievementSaturdayMarketKing = false;

    // ── Crowd (spawned punters) ───────────────────────────────────────────────

    /** Punter NPCs spawned for the current market day. */
    private final List<NPC> spawnedPunters = new ArrayList<>();

    /** Whether the crowd has been spawned for today. */
    private boolean crowdSpawnedToday = false;

    /** Last day index for which the crowd was managed. */
    private int lastDayIndex = -1;

    // ── Injected systems ──────────────────────────────────────────────────────

    private final TimeSystem timeSystem;
    private final RumourNetwork rumourNetwork;
    private final FenceSystem fenceSystem;
    private final StreetEconomySystem streetEconomySystem;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;

    // ── Random ────────────────────────────────────────────────────────────────

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public IndoorMarketSystem(TimeSystem timeSystem,
                              RumourNetwork rumourNetwork,
                              FenceSystem fenceSystem,
                              StreetEconomySystem streetEconomySystem,
                              NotorietySystem notorietySystem,
                              WantedSystem wantedSystem,
                              CriminalRecord criminalRecord,
                              Random random) {
        this.timeSystem = timeSystem;
        this.rumourNetwork = rumourNetwork;
        this.fenceSystem = fenceSystem;
        this.streetEconomySystem = streetEconomySystem;
        this.notorietySystem = notorietySystem;
        this.wantedSystem = wantedSystem;
        this.criminalRecord = criminalRecord;
        this.random = random;
    }

    /** Convenience constructor for testing. */
    public IndoorMarketSystem(TimeSystem timeSystem, Random random) {
        this(timeSystem, null, null, null, null, null, null, random);
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem ns) { this.notorietySystem = ns; }
    public void setWantedSystem(WantedSystem ws) { this.wantedSystem = ws; }
    public void setCriminalRecord(CriminalRecord cr) { this.criminalRecord = cr; }

    // ── Market hours ──────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the market is open right now (correct day AND within
     * opening hours).
     */
    public boolean isMarketDay() {
        return isMarketDay(timeSystem);
    }

    /**
     * Returns {@code true} if the given time system reports a market day and the
     * time is within {@link #OPEN_HOUR}–{@link #CLOSE_HOUR}.
     */
    public boolean isMarketDay(TimeSystem ts) {
        float hour = ts.getTime();
        if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) return false;
        int dayOfWeek = (ts.getDayCount() - 1) % 7;  // 0=Mon … 6=Sun
        for (int d : MARKET_DAYS) {
            if (d == dayOfWeek) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the shutter is currently blocking entry (market closed).
     */
    public boolean isShutterDown() {
        return shutterDown;
    }

    // ── Crowd spawning ────────────────────────────────────────────────────────

    /**
     * Spawn the crowd of {@link NPCType#MARKET_PUNTER} NPCs for a market day.
     * During rain the crowd is reduced by {@link #RAIN_CROWD_REDUCTION}.
     *
     * @param weatherSystem the current weather system (may be {@code null})
     * @return list of spawned punter NPCs (may be empty if not a market day)
     */
    public List<NPC> spawnCrowd(WeatherSystem weatherSystem) {
        spawnedPunters.clear();
        if (!isMarketDay()) return spawnedPunters;

        boolean isRaining = weatherSystem != null && weatherSystem.getCurrentWeather() == Weather.RAIN;
        int baseCount = PUNTER_CROWD_MIN + random.nextInt(PUNTER_CROWD_MAX - PUNTER_CROWD_MIN + 1);
        if (isRaining) {
            baseCount = (int) Math.floor(baseCount * (1f - RAIN_CROWD_REDUCTION));
        }

        punterCoinMap.clear();
        for (int i = 0; i < baseCount; i++) {
            NPC punter = new NPC(NPCType.MARKET_PUNTER, 0f, 0f, 0f);
            punter.setState(NPCState.WANDERING);
            // Give each punter 1–5 COIN (tracked separately as NPC has no coin field)
            punterCoinMap.put(punter, 1 + random.nextInt(5));
            spawnedPunters.add(punter);
        }
        crowdSpawnedToday = true;
        return spawnedPunters;
    }

    /** Returns the currently spawned punter NPCs. */
    public List<NPC> getSpawnedPunters() {
        return spawnedPunters;
    }

    // ── Stall rental ──────────────────────────────────────────────────────────

    /**
     * Attempt to rent a stall from Ray the Market Manager.
     *
     * @param playerInventory  player's inventory (3 COIN deducted on success)
     * @param notorietyTier    current player notoriety tier
     * @return {@code true} if rental succeeded
     */
    public boolean rentStall(Inventory playerInventory, int notorietyTier) {
        if (!isMarketDay()) return false;
        if (playerStallActive) return false;
        if (notorietyTier >= 4) return false;
        if (playerInventory == null) return false;
        if (playerInventory.getItemCount(Material.COIN) < STALL_RENT_COST) return false;

        playerInventory.removeItem(Material.COIN, STALL_RENT_COST);
        playerStallActive = true;
        playerStallItems.clear();
        stallSalesToday = 0;

        // Rental expires at CLOSE_HOUR
        stallRentExpiry = CLOSE_HOUR;
        return true;
    }

    /**
     * Place an item in the player's rented stall for sale.
     *
     * @param material     the item to sell
     * @param quantity     number of units
     * @param askingPrice  price per unit in COIN
     * @return {@code true} if placed
     */
    public boolean placeItemInStall(Material material, int quantity, int askingPrice) {
        if (!playerStallActive) return false;
        if (playerStallItems.size() >= MAX_STALL_ITEMS) return false;
        if (material == null || quantity <= 0 || askingPrice <= 0) return false;
        playerStallItems.add(new StallItem(material, quantity, askingPrice));
        return true;
    }

    /**
     * Whether the player's stall contains contraband (counterfeit or stolen items).
     */
    public boolean stallHasContraband() {
        for (StallItem item : playerStallItems) {
            if (isContraband(item.material)) return true;
        }
        return false;
    }

    private static boolean isContraband(Material m) {
        return m == Material.COUNTERFEIT_WATCH
                || m == Material.STOLEN_PHONE
                || m == Material.KNOCK_OFF_PERFUME
                || m == Material.KNOCKOFF_DESIGNER_TSHIRT
                || m == Material.KNOCK_OFF_TRACKSUIT;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the market system each frame.
     *
     * @param delta               seconds since last frame
     * @param npcs                all NPCs in the world
     * @param player              the player entity
     * @param playerInventory     player's inventory
     * @param achievementCallback for unlocking achievements
     */
    public void update(float delta,
                       List<NPC> npcs,
                       Player player,
                       Inventory playerInventory,
                       NotorietySystem.AchievementCallback achievementCallback) {

        boolean marketOpen = isMarketDay();
        shutterDown = !marketOpen;

        // ── Day roll-over reset ────────────────────────────────────────────────
        int currentDay = timeSystem.getDayCount();
        if (currentDay != lastDayIndex) {
            lastDayIndex = currentDay;
            raidTriggeredToday = false;
            crowdSpawnedToday = false;
            pickpocketsTodayCount = 0;
            saturdaySales = 0;
            // Reset stall on day change if rental has expired
            if (playerStallActive && timeSystem.getTime() < OPEN_HOUR) {
                finaliseStallForDay(achievementCallback);
            }
        }

        if (!marketOpen) {
            // Despawn punters when market closes
            if (!spawnedPunters.isEmpty()) {
                spawnedPunters.clear();
                crowdSpawnedToday = false;
            }
            return;
        }

        // ── Raid scheduling ────────────────────────────────────────────────────
        float hour = timeSystem.getTime();
        if (!raidTriggeredToday && hour >= RAID_WINDOW_START && hour < RAID_WINDOW_END) {
            if (random.nextFloat() < (RAID_DAILY_CHANCE * delta)) {
                triggerRaid(timeSystem);
            }
        }

        // ── Active raid processing ─────────────────────────────────────────────
        if (raidActive) {
            processRaid(player, playerInventory, npcs, achievementCallback);
        }

        // ── Stall browse cycle ─────────────────────────────────────────────────
        if (playerStallActive && !playerStallItems.isEmpty()) {
            browseTimer += delta;
            float browseIntervalSeconds = STALL_BROWSE_INTERVAL_MINS * 60f * SECONDS_PER_GAME_MINUTE;
            if (browseTimer >= browseIntervalSeconds) {
                browseTimer = 0f;
                runPunterBrowse(npcs, playerInventory, achievementCallback);
            }
        }
    }

    // ── Raid ──────────────────────────────────────────────────────────────────

    /**
     * Manually trigger a Trading Standards raid (also used by integration tests).
     *
     * @param ts time system (used to record raid start minute)
     */
    public void triggerRaid(TimeSystem ts) {
        if (raidActive) return;
        raidActive = true;
        raidTriggeredToday = true;
        raidStartMinute = ts.getHours() * 60 + ts.getMinutes();
        raidEscapeFrames = 0;
        escapedRaidWithContraband = false;

        // Spawn 2 Trading Standards officers
        tradingStandardsNpcs.clear();
        for (int i = 0; i < 2; i++) {
            NPC officer = new NPC(NPCType.TRADING_STANDARDS, 0f, 0f, 0f);
            officer.setState(NPCState.WANDERING);
            tradingStandardsNpcs.add(officer);
        }

        // Seed MARKET_RAID rumour to nearby market NPCs
        if (rumourNetwork != null) {
            for (NPC punter : spawnedPunters) {
                if (punter.isAlive()) {
                    rumourNetwork.addRumour(punter, new Rumour(RumourType.MARKET_RAID, "Trading Standards are in the market!"));
                    break; // Seed to first available punter
                }
            }
        }
    }

    private void processRaid(Player player, Inventory playerInventory,
                              List<NPC> npcs,
                              NotorietySystem.AchievementCallback achievementCallback) {
        raidEscapeFrames++;

        // Make Mo flee
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.isAlive() && npc.getType() == NPCType.MARKET_TRADER) {
                    // Mo is identifiable in real wiring; here we set all traders to FLEEING
                    // (integration: specific Mo NPC would be tracked separately)
                    if (npc.getName() != null && npc.getName().equals("Mo")) {
                        npc.setState(NPCState.FLEEING);
                    }
                }
            }
        }

        // Check if player stall has contraband once officers have had time to reach it
        if (raidEscapeFrames >= RAID_ESCAPE_FRAMES && playerStallActive) {
            boolean playerAdjacent = isPlayerAdjacentToStall(player);
            boolean hasContraband = stallHasContraband();

            if (hasContraband) {
                if (playerAdjacent) {
                    // Immediate consequences: confiscate and penalise
                    applyRaidConsequences(playerInventory, achievementCallback);
                } else {
                    // Player escaped — give LEGS_IT achievement
                    escapedRaidWithContraband = true;
                    if (!achievementLegsIt) {
                        achievementLegsIt = true;
                        if (achievementCallback != null) {
                            achievementCallback.award(AchievementType.LEGS_IT);
                        }
                    }
                    // Still confiscate the items (officers reached stall)
                    confiscateStallContraband();
                }
            }

            // End raid after RAID_DURATION_MINS
            float currentMinute = timeSystem.getHours() * 60 + timeSystem.getMinutes();
            if (currentMinute - raidStartMinute >= RAID_DURATION_MINS) {
                endRaid();
            }
        }
    }

    private void applyRaidConsequences(Inventory playerInventory,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        confiscateStallContraband();

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(20, achievementCallback);
        }
        if (wantedSystem != null && playerInventory != null) {
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.TRADING_STANDARDS_RAID);
        }
    }

    private void confiscateStallContraband() {
        playerStallItems.removeIf(item -> isContraband(item.material));
    }

    private void endRaid() {
        raidActive = false;
        tradingStandardsNpcs.clear();
    }

    private boolean isPlayerAdjacentToStall(Player player) {
        if (player == null) return false;
        // "Adjacent" means within 2 blocks of the stall
        // In a real implementation the stall position would be tracked;
        // here we use a flag set by the integration test.
        return playerAdjacentToStallForTest;
    }

    // ── Pickpocket ────────────────────────────────────────────────────────────

    /**
     * Attempt to pickpocket a {@link NPCType#MARKET_PUNTER} NPC.
     *
     * @param player          the player
     * @param target          the target punter NPC
     * @param nearbyNpcCount  number of other NPCs within 4 blocks (crowd cover)
     * @param playerInventory player inventory (COIN added on success)
     * @param notorietyTier   player's current notoriety tier
     * @param pickpocketSkill player's STEALTH/PICKPOCKET skill tier level (0+)
     * @param achievementCallback for achievements
     * @return {@code true} if pickpocket succeeded
     */
    public boolean tryPickpocket(Player player, NPC target, int nearbyNpcCount,
                                  Inventory playerInventory, int notorietyTier,
                                  int pickpocketSkill,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        // Require minimum crowd cover
        if (nearbyNpcCount < PICKPOCKET_CROWD_MIN) return false;
        if (target == null || !target.isAlive()) return false;

        float chance = PICKPOCKET_BASE_SUCCESS;
        chance += pickpocketSkill * 0.10f;
        if (notorietyTier >= 3) {
            chance -= PICKPOCKET_NOTORIETY_PENALTY;
        }
        chance = Math.max(0.05f, Math.min(0.95f, chance));

        if (random.nextFloat() < chance) {
            // Success — transfer 1–3 COIN
            int stolen = 1 + random.nextInt(3);
            int available = punterCoinMap.getOrDefault(target, 3);
            stolen = Math.min(stolen, Math.max(1, available));
            punterCoinMap.put(target, Math.max(0, available - stolen));
            if (playerInventory != null) {
                playerInventory.addItem(Material.COIN, stolen);
            }
            target.setState(NPCState.WANDERING);  // Doesn't notice

            pickpocketsTodayCount++;
            if (pickpocketsTodayCount >= 5 && !achievementCrowdWorker) {
                achievementCrowdWorker = true;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.CROWD_WORKER);
                }
            }
            return true;
        } else {
            // Failure — punter shouts
            target.setState(NPCState.WITNESS);  // NPC reacts as witness/alarmed
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(5, achievementCallback);
            }
            return false;
        }
    }

    /**
     * Convenience overload without skill/notoriety (uses base chance only).
     */
    public boolean tryPickpocket(Player player, NPC target, int nearbyNpcCount,
                                  Inventory playerInventory,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        return tryPickpocket(player, target, nearbyNpcCount, playerInventory, 0, 0, achievementCallback);
    }

    // ── Barter ────────────────────────────────────────────────────────────────

    /**
     * Attempt a barter with a trader NPC.
     *
     * <p>The trader accepts if the combined market value of offered items is ≥ 80%
     * of the target item's value, subject to the trader's personality accept rate.
     *
     * @param traderType  the type of trader NPC (MARKET_TRADER)
     * @param traderName  "Dave", "Sheila", "Mo", or "Brenda"
     * @param offerValue  combined market value of items being offered
     * @param targetValue market value of the item being requested
     * @return {@code true} if the barter was accepted
     */
    public boolean tryBarter(NPCType traderType, String traderName, int offerValue, int targetValue) {
        if ("Brenda".equalsIgnoreCase(traderName)) return false;  // Never barters
        if ("Mo".equalsIgnoreCase(traderName) && offerValue == 0) return false;

        // Value threshold: offer must be ≥ 80% of target
        if (offerValue < targetValue * 0.80f) return false;

        // Personality accept rate
        int acceptPct;
        if ("Dave".equalsIgnoreCase(traderName)) {
            acceptPct = DAVE_BARTER_ACCEPT_PCT;
        } else if ("Sheila".equalsIgnoreCase(traderName)) {
            acceptPct = SHEILA_BARTER_ACCEPT_PCT;
        } else if ("Mo".equalsIgnoreCase(traderName)) {
            acceptPct = MO_BARTER_ACCEPT_PCT;
        } else {
            acceptPct = 50;
        }

        return random.nextInt(100) < acceptPct;
    }

    // ── Punter buy logic ──────────────────────────────────────────────────────

    /**
     * Determine whether a punter will buy an item at the given price.
     *
     * @param price       the asking price in COIN
     * @param marketValue the item's baseline market value in COIN
     * @return {@code true} if the punter buys
     */
    public boolean shouldPunterBuy(int price, int marketValue) {
        if (marketValue <= 0) return false;
        float ratio = (float) price / marketValue;
        if (ratio <= PUNTER_BUY_THRESHOLD_FAIR) return true;
        if (ratio <= PUNTER_BUY_THRESHOLD_HIGH) return random.nextFloat() < PUNTER_BUY_CHANCE_HIGH_PRICE;
        return false;
    }

    // ── Punter browse ─────────────────────────────────────────────────────────

    private void runPunterBrowse(List<NPC> npcs, Inventory playerInventory,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (playerStallItems.isEmpty()) return;

        // Pick a random item from the stall
        StallItem item = playerStallItems.get(random.nextInt(playerStallItems.size()));

        // Determine market value (use FenceSystem if available, else 1 COIN default)
        int marketValue = 1;
        if (fenceSystem != null) {
            int fenceVal = fenceSystem.getSellPrice(item.material);
            if (fenceVal > 0) marketValue = fenceVal;
        }

        if (shouldPunterBuy(item.price, marketValue)) {
            // Sale!
            int revenue = item.price;
            item.quantity--;
            if (item.quantity <= 0) {
                playerStallItems.remove(item);
            }
            if (playerInventory != null) {
                playerInventory.addItem(Material.COIN, revenue);
            }
            stallSalesToday += revenue;

            // Saturday tracking
            int dayOfWeek = (timeSystem.getDayCount() - 1) % 7;
            if (dayOfWeek == 5) {  // Saturday
                saturdaySales += revenue;
                if (saturdaySales >= 20 && !achievementSaturdayMarketKing) {
                    achievementSaturdayMarketKing = true;
                    if (achievementCallback != null) {
                        achievementCallback.award(AchievementType.SATURDAY_MARKET_KING);
                    }
                }
            }

            // SOVEREIGN_TRADING — sold COUNTERFEIT_WATCH for 5+ COIN
            if (item.material == Material.COUNTERFEIT_WATCH && revenue >= 5
                    && !achievementSovereignTrading) {
                achievementSovereignTrading = true;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.SOVEREIGN_TRADING);
                }
            }
        }
    }

    // ── Brenda / free tea ─────────────────────────────────────────────────────

    /**
     * Interact with Brenda's tea urn. Returns a {@link Material#MUG_OF_TEA} for free
     * during rain, otherwise costs 1 COIN.
     *
     * @param playerInventory player inventory
     * @param weatherSystem   current weather system (may be null)
     * @return {@code true} if tea was dispensed
     */
    public boolean interactWithTeaUrn(Inventory playerInventory, WeatherSystem weatherSystem) {
        if (!isMarketDay()) return false;
        if (playerInventory == null) return false;

        boolean isRaining = weatherSystem != null
                && weatherSystem.getCurrentWeather() == Weather.RAIN;
        if (isRaining) {
            // Free tea during rain
            playerInventory.addItem(Material.MUG_OF_TEA, 1);
            return true;
        } else {
            // 1 COIN normally
            if (playerInventory.getItemCount(Material.COIN) >= 1) {
                playerInventory.removeItem(Material.COIN, 1);
                playerInventory.addItem(Material.MUG_OF_TEA, 1);
                return true;
            }
        }
        return false;
    }

    // ── Stall finalisation ────────────────────────────────────────────────────

    private void finaliseStallForDay(NotorietySystem.AchievementCallback achievementCallback) {
        if (stallSalesToday > 0) {
            stallRentalsCompleted++;
            if (stallRentalsCompleted >= 5 && !achievementMarketRegular) {
                achievementMarketRegular = true;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.MARKET_REGULAR);
                }
            }
        }
        playerStallActive = false;
        playerStallItems.clear();
        stallSalesToday = 0;
    }

    // ── Test hooks ────────────────────────────────────────────────────────────

    /**
     * Flag used by integration tests to simulate player adjacency to stall.
     * In full game wiring this would be computed from player 3D position.
     */
    private boolean playerAdjacentToStallForTest = false;

    /** Set player-adjacent-to-stall flag for testing. */
    public void setPlayerAdjacentToStall(boolean adjacent) {
        this.playerAdjacentToStallForTest = adjacent;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether the player currently has a rented stall. */
    public boolean isPlayerStallActive() { return playerStallActive; }

    /** Items currently in the player's stall. */
    public List<StallItem> getPlayerStallItems() { return playerStallItems; }

    /** Whether a raid is currently active. */
    public boolean isRaidActive() { return raidActive; }

    /** In-game minute the current raid started. */
    public int getRaidStartMinute() { return raidStartMinute; }

    /** Stall rentals completed where at least 1 item was sold. */
    public int getStallRentalsCompleted() { return stallRentalsCompleted; }

    /** Number of successful pickpockets on punters today. */
    public int getPickpocketsTodayCount() { return pickpocketsTodayCount; }

    /** NPCs currently acting as Trading Standards officers. */
    public List<NPC> getTradingStandardsNpcs() { return tradingStandardsNpcs; }

    /** Stall sales earned today. */
    public int getStallSalesToday() { return stallSalesToday; }

    // ── Inner data class ──────────────────────────────────────────────────────

    /** Represents a single item slot in the player's rented stall. */
    public static class StallItem {
        public final Material material;
        public int quantity;
        public final int price;

        public StallItem(Material material, int quantity, int price) {
            this.material = material;
            this.quantity = quantity;
            this.price = price;
        }
    }
}

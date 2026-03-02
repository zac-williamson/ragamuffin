package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1098: Northfield Summer Fete — Tombola, Cake Stall &amp; the Great Raffle Fix.
 *
 * <p>Brings a one-day annual summer fete to Northfield Park on the first Saturday of July,
 * 10:00–16:00. Five interactive stalls, criminal mechanics (cake theft, raffle fix,
 * bric-a-brac theft), weather cancellation, post-fete patron surge, and ice cream van arrival.
 *
 * <h3>Schedule</h3>
 * <ul>
 *   <li>09:30 — Props spawned; 2–3 VOLUNTEER_NPC NPCs appear; weather cancellation check.</li>
 *   <li>10:00 — Fete opens; VICAR_NPC opening speech; 4–6 PENSIONER + 2–3 SCHOOL_KID spawn.</li>
 *   <li>13:00 — Cake stall restocks; IceCreamVanSystem van arrives.</li>
 *   <li>15:00 — Raffle draw.</li>
 *   <li>16:00 — Fete closes; props despawn after 10 in-game minutes; patrons surge Wetherspoons.</li>
 * </ul>
 *
 * <h3>Criminal Mechanics</h3>
 * <ul>
 *   <li><b>Cake theft</b>: steal from unattended stall → Notoriety +5 / PETTY_THEFT;
 *       VICAR witnessing adds Wanted +1.</li>
 *   <li><b>Raffle fix</b>: hold E for 3s with RIGGED_BARREL and no volunteer within 8 blocks;
 *       interrupted → Notoriety +8, ejection, Wanted +1.</li>
 *   <li><b>Bric-a-brac theft</b>: rummaging without volunteer present → PETTY_THEFT +3.</li>
 * </ul>
 *
 * <h3>Weather Cancellation</h3>
 * THUNDERSTORM or HEAVY_RAIN at 09:30 cancels the fete. DRIZZLE does not cancel.
 */
public class FeteSystem {

    // ── Day-of-week constants (dayCount % 7) ──────────────────────────────────
    private static final int SATURDAY = 5;

    // ── Schedule constants ────────────────────────────────────────────────────

    /** Hour at which props are spawned and weather check occurs. */
    public static final float SETUP_HOUR          = 9.5f;   // 09:30
    /** Hour at which the fete opens. */
    public static final float OPEN_HOUR           = 10.0f;
    /** Hour at which the cake stall restocks and ice cream van arrives. */
    public static final float RESTOCK_HOUR        = 13.0f;
    /** Hour at which the raffle draw occurs. */
    public static final float RAFFLE_DRAW_HOUR    = 15.0f;
    /** Hour at which the fete closes. */
    public static final float CLOSE_HOUR          = 16.0f;
    /** Minutes (in-game) after close before props despawn (10 minutes = 10/60 hours). */
    public static final float PROP_DESPAWN_DELAY  = 10.0f / 60.0f;

    // ── Tombola prize probabilities ───────────────────────────────────────────

    /** Probability of BISCUIT prize from tombola. */
    public static final float TOMBOLA_BISCUIT_CHANCE     = 0.40f;
    /** Probability of BOTTLE_OF_WINE prize from tombola (cumulative: 0.60). */
    public static final float TOMBOLA_WINE_CHANCE        = 0.20f;
    /** Probability of KETTLE prize from tombola (cumulative: 0.75). */
    public static final float TOMBOLA_KETTLE_CHANCE      = 0.15f;
    /** Probability of CUDDLY_TOY prize from tombola (cumulative: 0.85). */
    public static final float TOMBOLA_TOY_CHANCE         = 0.10f;
    /** Probability of no prize (cumulative: 1.00). */
    public static final float TOMBOLA_NO_PRIZE_CHANCE    = 0.15f;

    // ── Hook-a-Duck constants ─────────────────────────────────────────────────

    /** Win probability for Hook-a-Duck. */
    public static final float HOOK_A_DUCK_WIN_CHANCE = 0.30f;

    /** Number of consecutive wins before volunteer suspicion check. */
    public static final int HOOK_A_DUCK_SUSPICION_STREAK = 3;

    /** Probability that a VOLUNTEER bans the player after a suspicious streak. */
    public static final float HOOK_A_DUCK_BAN_CHANCE = 0.50f;

    // ── Cake stall constants ──────────────────────────────────────────────────

    /** Initial stock (units per cake type) at the cake stall. */
    public static final int CAKE_STALL_INITIAL_STOCK = 3;

    // ── Theft / notoriety constants ───────────────────────────────────────────

    /** Notoriety gained from cake theft. */
    public static final int CAKE_THEFT_NOTORIETY     = 5;
    /** Notoriety gained from bric-a-brac theft (rummaging alone). */
    public static final int BRIC_A_BRAC_THEFT_NOTORIETY = 3;
    /** Notoriety gained from a successful raffle fix. */
    public static final int RAFFLE_FIX_NOTORIETY     = 5;
    /** Notoriety gained from an interrupted raffle fix (caught mid-swap). */
    public static final int RAFFLE_FIX_CAUGHT_NOTORIETY = 8;

    // ── Raffle fix constants ──────────────────────────────────────────────────

    /** Seconds of holding E required to fix the raffle. */
    public static final float RAFFLE_FIX_DURATION = 3.0f;
    /** Radius within which a VOLUNTEER blocks the raffle fix. */
    public static final float RAFFLE_FIX_VOLUNTEER_RADIUS = 8.0f;
    /** Radius within which a VOLUNTEER witnesses cake theft. */
    public static final float CAKE_THEFT_WITNESS_RADIUS = 6.0f;
    /** Radius within which a VOLUNTEER counts as supervising bric-a-brac rummage. */
    public static final float BRIC_A_BRAC_VOLUNTEER_RADIUS = 5.0f;

    // ── Post-fete patron surge ────────────────────────────────────────────────

    /** Extra patrons added to Wetherspoons at fete close. */
    public static final int WETHERSPOONS_PATRON_SURGE = 4;

    // ── Raffle ticket constants ───────────────────────────────────────────────

    /** Maximum raffle tickets purchasable per player. */
    public static final int MAX_RAFFLE_TICKETS = 5;

    // ── NPC counts ────────────────────────────────────────────────────────────

    /** Minimum PENSIONER NPCs at open. */
    public static final int PENSIONER_MIN = 4;
    /** Maximum PENSIONER NPCs at open. */
    public static final int PENSIONER_MAX = 6;
    /** Minimum SCHOOL_KID NPCs at open. */
    public static final int SCHOOL_KID_MIN = 2;
    /** Maximum SCHOOL_KID NPCs at open. */
    public static final int SCHOOL_KID_MAX = 3;
    /** Minimum VOLUNTEER NPCs at setup. */
    public static final int VOLUNTEER_MIN = 2;
    /** Maximum VOLUNTEER NPCs at setup. */
    public static final int VOLUNTEER_MAX = 3;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the fete is currently active (props spawned, stalls open). */
    private boolean feteActive = false;
    /** Whether the fete was cancelled due to bad weather. */
    private boolean feteCancelled = false;
    /** Whether the fete props have been spawned for today. */
    private boolean propsSpawned = false;
    /** Whether the 09:30 setup phase has been processed today. */
    private boolean setupProcessed = false;
    /** Whether the 10:00 open phase has been processed today. */
    private boolean openProcessed = false;
    /** Whether the 13:00 restock phase has been processed today. */
    private boolean restockProcessed = false;
    /** Whether the raffle has been drawn. */
    private boolean raffleDrawn = false;
    /** Whether the raffle was rigged by the player. */
    private boolean raffleRigged = false;
    /** Whether the 16:00 close phase has been processed. */
    private boolean closeProcessed = false;
    /** Whether the player has been ejected from the fete (cannot interact with props). */
    private boolean playerEjected = false;

    /** Last day count processed (to detect new days). */
    private int lastProcessedDay = -1;

    /** Cake stall stock: index 0=VICTORIA_SPONGE, 1=SCONE, 2=CUPCAKE, 3=JAM_AND_CREAM. */
    private final int[] cakeStock = new int[4];
    private static final Material[] CAKE_TYPES = {
        Material.VICTORIA_SPONGE, Material.SCONE, Material.CUPCAKE, Material.JAM_AND_CREAM
    };
    private static final int[] CAKE_PRICES = { 2, 1, 1, 1 };
    private static final int[] CAKE_HUNGER = { 30, 15, 10, 12 };

    /** Number of raffle tickets the player has purchased. */
    private int playerRaffleTickets = 0;

    /** Active fete prop positions (for cleanup). */
    private final List<PropPosition> feteProps = new ArrayList<>();
    /** Active fete NPC references. */
    private final List<NPC> feteNpcs = new ArrayList<>();
    /** The VICAR_NPC for this fete. */
    private NPC vicarNpc = null;
    /** The VOLUNTEER_NPC list for this fete. */
    private final List<NPC> volunteerNpcs = new ArrayList<>();

    /** Hook-a-Duck consecutive win streak. */
    private int hookADuckStreak = 0;
    /** Whether the player is banned from Hook-a-Duck for today. */
    private boolean hookADuckBanned = false;

    /** Timer for raffle fix hold (seconds). */
    private float raffleFixTimer = 0f;
    /** Whether the player is currently holding E on the raffle stall. */
    private boolean raffleFixInProgress = false;
    /** Whether the player has a RIGGED_BARREL in hand for fixing. */
    private boolean riggedBarrelPresent = false;

    /** Timer tracking time after close (for prop despawn). */
    private float closeTimer = 0f;

    /** Number of fetes attended (across in-game years, for BRITISH_INSTITUTION). */
    private int fetesAttended = 0;

    /** Park landmark position (set by caller). */
    private float parkX = 80f;
    private float parkZ = 80f;
    private float parkWidth = 40f;
    private float parkDepth = 40f;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final TimeSystem timeSystem;
    private final World world;
    private final RumourNetwork rumourNetwork;
    private final NotorietySystem notorietySystem;
    private final WantedSystem wantedSystem;
    private final ChurchSystem churchSystem;
    private final Inventory playerInventory;
    private final Random random;

    private WetherspoonsSystem wetherspoonsSystem;
    private IceCreamVanSystem iceCreamVanSystem;
    private AchievementSystem achievementSystem;
    private CriminalRecord criminalRecord;

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /**
     * Callback for awarding achievements.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    public FeteSystem(TimeSystem timeSystem, World world,
                      RumourNetwork rumourNetwork, NotorietySystem notorietySystem,
                      WantedSystem wantedSystem, ChurchSystem churchSystem,
                      Inventory playerInventory, Random random) {
        this.timeSystem      = timeSystem;
        this.world           = world;
        this.rumourNetwork   = rumourNetwork;
        this.notorietySystem = notorietySystem;
        this.wantedSystem    = wantedSystem;
        this.churchSystem    = churchSystem;
        this.playerInventory = playerInventory;
        this.random          = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

    public void setWetherspoonsSystem(WetherspoonsSystem s) { this.wetherspoonsSystem = s; }
    public void setIceCreamVanSystem(IceCreamVanSystem s)   { this.iceCreamVanSystem  = s; }
    public void setAchievementSystem(AchievementSystem s)   { this.achievementSystem  = s; }
    public void setCriminalRecord(CriminalRecord r)         { this.criminalRecord     = r; }

    /** Set the park bounds so props are spawned in the right location. */
    public void setParkBounds(float x, float z, float width, float depth) {
        this.parkX = x; this.parkZ = z;
        this.parkWidth = width; this.parkDepth = depth;
    }

    // ── Main update ────────────────────────────────────────────────────────────

    /**
     * Update the fete system once per frame.
     *
     * @param delta      real-time seconds since last frame
     * @param ts         the TimeSystem (same reference stored in field, passed for API parity)
     * @param weather    current weather
     * @param allNpcs    all live NPCs in the world
     * @param playerX    player world X position
     * @param playerY    player world Y position
     * @param playerZ    player world Z position
     * @param achCallback callback for awarding achievements (may be null)
     */
    public void update(float delta, TimeSystem ts, Weather weather,
                       List<NPC> allNpcs,
                       float playerX, float playerY, float playerZ,
                       AchievementCallback achCallback) {
        int dayCount    = ts.getDayCount();
        float hour      = ts.getTime();
        int dayOfMonth  = ts.getDayOfMonth();
        int month       = ts.getMonth(); // 0=Jan, 6=July

        // Reset daily state on new day
        if (dayCount != lastProcessedDay) {
            resetDailyState();
            lastProcessedDay = dayCount;
        }

        // Only proceed on the first Saturday of July
        if (!isFeteDay(dayOfMonth, dayCount, month)) {
            return;
        }

        // ── 09:30: Setup & weather check ─────────────────────────────────────
        if (!setupProcessed && hour >= SETUP_HOUR) {
            setupProcessed = true;
            onSetup(weather, allNpcs, achCallback);
        }

        if (feteCancelled) return;

        // ── 10:00: Fete opens ─────────────────────────────────────────────────
        if (!openProcessed && hour >= OPEN_HOUR) {
            openProcessed = true;
            onOpen(allNpcs, achCallback);
        }

        // ── 13:00: Cake restock + ice cream van ──────────────────────────────
        if (!restockProcessed && hour >= RESTOCK_HOUR) {
            restockProcessed = true;
            onRestock(allNpcs);
        }

        // ── 15:00: Raffle draw ────────────────────────────────────────────────
        if (!raffleDrawn && hour >= RAFFLE_DRAW_HOUR && feteActive) {
            raffleDrawn = true;
            conductRaffleDraw(allNpcs, achCallback);
        }

        // ── Raffle fix progress ───────────────────────────────────────────────
        if (raffleFixInProgress && !raffleDrawn) {
            updateRaffleFixProgress(delta, allNpcs, playerX, playerY, playerZ, achCallback);
        }

        // ── 16:00: Fete closes ────────────────────────────────────────────────
        if (!closeProcessed && hour >= CLOSE_HOUR && feteActive) {
            closeProcessed = true;
            onClose(allNpcs, achCallback);
        }

        // ── Post-close prop despawn (after 10 in-game minutes) ────────────────
        if (closeProcessed && feteActive && propsSpawned) {
            // in-game minutes: 1 real second = 6 in-game minutes (timeSpeed=0.1 h/s)
            // We track real delta here since we use actual clock delta
            closeTimer += delta;
            float inGameMinutes = closeTimer * ts.getTimeSpeed() * 60f;
            if (inGameMinutes >= 10f) {
                despawnProps();
                feteActive = false;
                seedRumour("Lovely turnout at the fete today. Mrs. Patterson's lemon drizzle was robbed, apparently.");
            }
        }
    }

    // ── Schedule handlers ──────────────────────────────────────────────────────

    private void onSetup(Weather weather, List<NPC> allNpcs, AchievementCallback achCallback) {
        // Weather cancellation
        if (weather == Weather.THUNDERSTORM || weather == Weather.RAIN) {
            feteCancelled = true;
            seedRumour("Fete's been called off — typical.");
            return;
        }

        // Spawn props within park
        spawnProps();

        // Spawn 2–3 VOLUNTEER_NPC
        int volCount = VOLUNTEER_MIN + random.nextInt(VOLUNTEER_MAX - VOLUNTEER_MIN + 1);
        for (int i = 0; i < volCount; i++) {
            NPC vol = new NPC(NPCType.FOOD_BANK_VOLUNTEER,
                    parkX + 5f + i * 3f, 0f, parkZ + 5f);
            vol.setName("Margaret");
            vol.setState(NPCState.IDLE);
            volunteerNpcs.add(vol);
            feteNpcs.add(vol);
            if (allNpcs != null) allNpcs.add(vol);
        }

        feteActive = true;
        propsSpawned = true;
    }

    private void onOpen(List<NPC> allNpcs, AchievementCallback achCallback) {
        if (!feteActive) return;

        // Spawn VICAR_NPC
        vicarNpc = new NPC(NPCType.VICAR, "Reverend Dave",
                parkX + 10f, 0f, parkZ + 8f);
        vicarNpc.setState(NPCState.IDLE);
        vicarNpc.setSpeechText("Welcome to the 29th Annual Northfield Summer Fete! Mind the wasps.", 12f);
        feteNpcs.add(vicarNpc);
        if (allNpcs != null) allNpcs.add(vicarNpc);

        // Spawn 4–6 PENSIONER
        int pensionersCount = PENSIONER_MIN + random.nextInt(PENSIONER_MAX - PENSIONER_MIN + 1);
        for (int i = 0; i < pensionersCount; i++) {
            NPC p = new NPC(NPCType.PENSIONER,
                    parkX + 3f + (i % 4) * 3f, 0f, parkZ + 3f + (i / 4) * 3f);
            p.setState(NPCState.WANDERING);
            feteNpcs.add(p);
            if (allNpcs != null) allNpcs.add(p);
        }

        // Spawn 2–3 SCHOOL_KID
        int kidCount = SCHOOL_KID_MIN + random.nextInt(SCHOOL_KID_MAX - SCHOOL_KID_MIN + 1);
        for (int i = 0; i < kidCount; i++) {
            NPC kid = new NPC(NPCType.SCHOOL_KID,
                    parkX + 15f + i * 2f, 0f, parkZ + 12f);
            kid.setState(NPCState.WANDERING);
            feteNpcs.add(kid);
            if (allNpcs != null) allNpcs.add(kid);
        }

        // Track attendance
        fetesAttended++;
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.BRITISH_INSTITUTION);
        }
    }

    private void onRestock(List<NPC> allNpcs) {
        // Restock cake stall
        for (int i = 0; i < cakeStock.length; i++) {
            cakeStock[i] = CAKE_STALL_INITIAL_STOCK;
        }

        // Ice cream van arrives at park
        if (iceCreamVanSystem != null && iceCreamVanSystem.isVanAvailable()) {
            iceCreamVanSystem.sendToPark();
            // Spawn 2–4 SCHOOL_KID near park entrance
            int queueSize = 2 + random.nextInt(3);
            for (int i = 0; i < queueSize; i++) {
                NPC kid = new NPC(NPCType.SCHOOL_KID,
                        parkX + i * 1.5f, 0f, parkZ - 2f);
                kid.setState(NPCState.IDLE);
                feteNpcs.add(kid);
                if (allNpcs != null) allNpcs.add(kid);
            }
        }
    }

    private void conductRaffleDraw(List<NPC> allNpcs, AchievementCallback achCallback) {
        if (vicarNpc != null) {
            vicarNpc.setSpeechText("And the winner of the grand raffle is... number " + (1 + random.nextInt(99)) + "!", 10f);
        }

        // Determine if player wins
        boolean playerWins = (playerRaffleTickets > 0) && (raffleRigged || random.nextFloat() < playerWinChance());

        if (playerWins) {
            int prize = raffleRigged ? 0 : random.nextInt(3);
            if (prize == 0) {
                // 1st prize
                playerInventory.addItem(Material.CAN_OF_LAGER, 1);
                playerInventory.addItem(Material.COIN, 10);
            } else if (prize == 1) {
                // 2nd prize
                playerInventory.addItem(Material.CUDDLY_TOY, 1);
            } else {
                // 3rd prize
                playerInventory.addItem(Material.BISCUIT, 1);
            }
        }

        seedRumour("The raffle draw at the fete — big crowd, lots of excitement. Someone won a lovely bottle of wine.");

        // FETE_CHAMPION check
        checkFeteChampion(achCallback);
    }

    private void onClose(List<NPC> allNpcs, AchievementCallback achCallback) {
        // Post-fete: Wetherspoons patron surge
        if (wetherspoonsSystem != null) {
            wetherspoonsSystem.addPatrons(WETHERSPOONS_PATRON_SURGE);
        }

        // NPC dispersal
        for (NPC npc : feteNpcs) {
            if (npc.isAlive()) {
                npc.setState(NPCState.WANDERING);
            }
        }

        closeTimer = 0f;
    }

    // ── Tombola mechanic ───────────────────────────────────────────────────────

    /**
     * Player plays the tombola (press E + 1 COIN).
     *
     * @param inventory player's inventory
     * @return the prize material, or null if no prize / insufficient funds
     */
    public Material playTombola(Inventory inventory) {
        if (!feteActive || playerEjected) return null;
        if (inventory.getItemCount(Material.COIN) < 1) return null;
        inventory.removeItem(Material.COIN, 1);

        float roll = random.nextFloat();
        Material prize = null;
        if (roll < TOMBOLA_BISCUIT_CHANCE) {
            prize = Material.BISCUIT;
        } else if (roll < TOMBOLA_BISCUIT_CHANCE + TOMBOLA_WINE_CHANCE) {
            prize = Material.CAN_OF_LAGER;
        } else if (roll < TOMBOLA_BISCUIT_CHANCE + TOMBOLA_WINE_CHANCE + TOMBOLA_KETTLE_CHANCE) {
            prize = Material.KETTLE;
        } else if (roll < TOMBOLA_BISCUIT_CHANCE + TOMBOLA_WINE_CHANCE + TOMBOLA_KETTLE_CHANCE + TOMBOLA_TOY_CHANCE) {
            prize = Material.CUDDLY_TOY;
        }
        // else: no prize (15%)

        if (prize != null) {
            inventory.addItem(prize, 1);
            checkFeteChampion(null);
        }
        return prize;
    }

    // ── Cake stall mechanics ──────────────────────────────────────────────────

    /**
     * Player buys a cake from the stall.
     *
     * @param cakeType   one of VICTORIA_SPONGE, SCONE, CUPCAKE, JAM_AND_CREAM
     * @param inventory  player's inventory
     * @return true if purchase succeeded
     */
    public boolean buyCake(Material cakeType, Inventory inventory) {
        if (!feteActive || playerEjected) return false;
        int idx = cakeTypeIndex(cakeType);
        if (idx < 0 || cakeStock[idx] <= 0) return false;
        int price = CAKE_PRICES[idx];
        if (inventory.getItemCount(Material.COIN) < price) return false;

        inventory.removeItem(Material.COIN, price);
        inventory.addItem(cakeType, 1);
        cakeStock[idx]--;
        return true;
    }

    /**
     * Player attempts to steal a cake from the stall.
     *
     * @param inventory    player's inventory
     * @param allNpcs      all live NPCs (for witness check)
     * @param playerX      player world X
     * @param playerY      player world Y
     * @param playerZ      player world Z
     * @param achCallback  achievement callback
     * @return the stolen cake Material, or null if blocked / no stock
     */
    public Material stealCake(Inventory inventory, List<NPC> allNpcs,
                               float playerX, float playerY, float playerZ,
                               AchievementCallback achCallback) {
        if (!feteActive || playerEjected) return null;

        // Check if VICAR or VOLUNTEER is within witness range
        boolean witnessed = isNpcTypeNearby(allNpcs, NPCType.VICAR, playerX, playerZ, CAKE_THEFT_WITNESS_RADIUS)
                || isNpcTypeNearby(allNpcs, NPCType.FOOD_BANK_VOLUNTEER, playerX, playerZ, CAKE_THEFT_WITNESS_RADIUS);

        if (witnessed) {
            // VICAR witnesses — call police
            if (vicarNpc != null) {
                vicarNpc.setSpeechText("OI! That's for charity!", 8f);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, playerX, playerY, playerZ,
                        achCallback != null ? type -> { if (achievementSystem != null) achievementSystem.unlock(type); } : null);
            }
            return null;
        }

        // Find a cake to steal
        for (int i = 0; i < cakeStock.length; i++) {
            if (cakeStock[i] > 0) {
                Material cake = CAKE_TYPES[i];
                inventory.addItem(cake, 1);
                cakeStock[i]--;

                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(CAKE_THEFT_NOTORIETY,
                            achCallback != null ? t -> { if (achievementSystem != null) achievementSystem.unlock(t); } : null);
                }
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.THEFT);
                }

                if (achCallback != null) achCallback.award(AchievementType.CAKE_THIEF);
                if (achievementSystem != null) achievementSystem.unlock(AchievementType.CAKE_THIEF);

                return cake;
            }
        }
        return null;
    }

    // ── Bric-a-brac rummage ────────────────────────────────────────────────────

    private static final Material[] BRIC_A_BRAC_LOOT = {
        Material.COAT, Material.TEXTBOOK, Material.HYMN_BOOK,
        Material.ORNAMENT, Material.TOASTER, Material.RETRO_CASSETTE
    };

    /**
     * Player rummages in the bric-a-brac table.
     *
     * @param inventory    player's inventory
     * @param allNpcs      all live NPCs
     * @param playerX      player world X
     * @param playerZ      player world Z
     * @param achCallback  achievement callback
     * @return the item found, or null
     */
    public Material rummage(Inventory inventory, List<NPC> allNpcs,
                             float playerX, float playerZ,
                             AchievementCallback achCallback) {
        if (!feteActive || playerEjected) return null;

        boolean supervised = isNpcTypeNearby(allNpcs, NPCType.FOOD_BANK_VOLUNTEER,
                playerX, playerZ, BRIC_A_BRAC_VOLUNTEER_RADIUS);

        Material found = BRIC_A_BRAC_LOOT[random.nextInt(BRIC_A_BRAC_LOOT.length)];
        inventory.addItem(found, 1);

        if (!supervised) {
            // Treat as theft
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(BRIC_A_BRAC_THEFT_NOTORIETY,
                        achCallback != null ? t -> { if (achievementSystem != null) achievementSystem.unlock(t); } : null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.THEFT);
            }
        }
        return found;
    }

    // ── Raffle ticket purchase ─────────────────────────────────────────────────

    /**
     * Player buys a raffle ticket (1 COIN each, max 5).
     *
     * @param inventory player's inventory
     * @return true if ticket purchased
     */
    public boolean buyRaffleTicket(Inventory inventory) {
        if (!feteActive || playerEjected) return false;
        if (playerRaffleTickets >= MAX_RAFFLE_TICKETS) return false;
        if (inventory.getItemCount(Material.COIN) < 1) return false;

        inventory.removeItem(Material.COIN, 1);
        inventory.addItem(Material.RAFFLE_TICKET, 1);
        playerRaffleTickets++;
        return true;
    }

    // ── Raffle fix mechanic ────────────────────────────────────────────────────

    /**
     * Player begins holding E on the raffle stall (starts fix progress).
     * Must have RIGGED_BARREL in inventory.
     *
     * @param inventory  player inventory
     * @param allNpcs    all live NPCs
     * @param playerX    player world X
     * @param playerZ    player world Z
     * @return true if fix attempt has started
     */
    public boolean beginRaffleFix(Inventory inventory, List<NPC> allNpcs,
                                   float playerX, float playerZ) {
        if (!feteActive || playerEjected || raffleDrawn) return false;
        if (inventory.getItemCount(Material.RIGGED_BARREL) < 1) return false;

        // Check no volunteer nearby
        if (isNpcTypeNearby(allNpcs, NPCType.FOOD_BANK_VOLUNTEER,
                playerX, playerZ, RAFFLE_FIX_VOLUNTEER_RADIUS)) {
            return false;
        }

        raffleFixInProgress = true;
        riggedBarrelPresent = true;
        raffleFixTimer = 0f;
        return true;
    }

    /** Cancel the raffle fix attempt (player released E). */
    public void cancelRaffleFix() {
        raffleFixInProgress = false;
        raffleFixTimer = 0f;
    }

    private void updateRaffleFixProgress(float delta, List<NPC> allNpcs,
                                          float playerX, float playerY, float playerZ,
                                          AchievementCallback achCallback) {
        // Check if a volunteer has walked within range
        if (isNpcTypeNearby(allNpcs, NPCType.FOOD_BANK_VOLUNTEER,
                playerX, playerZ, RAFFLE_FIX_VOLUNTEER_RADIUS)) {
            // Caught mid-swap
            raffleFixInProgress = false;
            raffleRigged = false;
            riggedBarrelPresent = false;
            playerEjected = true;

            if (notorietySystem != null) {
                notorietySystem.addNotoriety(RAFFLE_FIX_CAUGHT_NOTORIETY,
                        achCallback != null ? t -> { if (achievementSystem != null) achievementSystem.unlock(t); } : null);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, playerX, playerY, playerZ,
                        achCallback != null ? type -> { if (achievementSystem != null) achievementSystem.unlock(type); } : null);
            }
            return;
        }

        raffleFixTimer += delta;

        if (raffleFixTimer >= RAFFLE_FIX_DURATION) {
            // Fix complete!
            raffleFixInProgress = false;
            raffleRigged = true;
            riggedBarrelPresent = false;

            // Consume RIGGED_BARREL
            if (playerInventory != null) {
                playerInventory.removeItem(Material.RIGGED_BARREL, 1);
            }

            if (notorietySystem != null) {
                notorietySystem.addNotoriety(RAFFLE_FIX_NOTORIETY,
                        achCallback != null ? t -> { if (achievementSystem != null) achievementSystem.unlock(t); } : null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.BINGO_CHEATING);
            }
            if (achCallback != null) achCallback.award(AchievementType.RIGGED);
            if (achievementSystem != null) achievementSystem.unlock(AchievementType.RIGGED);
        }
    }

    // ── Hook-a-Duck ────────────────────────────────────────────────────────────

    /**
     * Player plays Hook-a-Duck (press E + 1 COIN).
     *
     * @param inventory    player inventory
     * @param allNpcs      all live NPCs (for volunteer suspicion check)
     * @param playerX      player world X
     * @param playerZ      player world Z
     * @return CUDDLY_TOY if won, null if lost / banned
     */
    public Material playHookADuck(Inventory inventory, List<NPC> allNpcs,
                                   float playerX, float playerZ) {
        if (!feteActive || playerEjected || hookADuckBanned) return null;
        if (inventory.getItemCount(Material.COIN) < 1) return null;
        inventory.removeItem(Material.COIN, 1);

        boolean won = random.nextFloat() < HOOK_A_DUCK_WIN_CHANCE;
        if (won) {
            inventory.addItem(Material.CUDDLY_TOY, 1);
            hookADuckStreak++;

            // Suspicion check after 3 consecutive wins
            if (hookADuckStreak >= HOOK_A_DUCK_SUSPICION_STREAK) {
                if (isNpcTypeNearby(allNpcs, NPCType.FOOD_BANK_VOLUNTEER,
                        playerX, playerZ, 5f)) {
                    if (random.nextFloat() < HOOK_A_DUCK_BAN_CHANCE) {
                        hookADuckBanned = true;
                    }
                }
                hookADuckStreak = 0;
            }
            checkFeteChampion(null);
            return Material.CUDDLY_TOY;
        } else {
            hookADuckStreak = 0;
            return null;
        }
    }

    // ── Prop management ────────────────────────────────────────────────────────

    private void spawnProps() {
        feteProps.clear();
        float cx = parkX + parkWidth / 2f;
        float cz = parkZ + parkDepth / 2f;

        PropPosition tombola  = new PropPosition(cx - 8f, 0f, cz - 5f, PropType.TOMBOLA_STALL_PROP,     0f);
        PropPosition cake     = new PropPosition(cx - 4f, 0f, cz - 5f, PropType.CAKE_STALL_PROP,        0f);
        PropPosition bric     = new PropPosition(cx,      0f, cz - 5f, PropType.BRIC_A_BRAC_PROP,       0f);
        PropPosition raffle   = new PropPosition(cx + 5f, 0f, cz - 5f, PropType.RAFFLE_TICKET_STALL_PROP, 0f);
        PropPosition duck     = new PropPosition(cx + 10f, 0f, cz - 5f, PropType.HOOK_A_DUCK_PROP,      0f);

        feteProps.add(tombola);
        feteProps.add(cake);
        feteProps.add(bric);
        feteProps.add(raffle);
        feteProps.add(duck);

        if (world != null) {
            for (PropPosition p : feteProps) {
                world.addPropPosition(p);
            }
        }

        // Initialise cake stock
        for (int i = 0; i < cakeStock.length; i++) {
            cakeStock[i] = CAKE_STALL_INITIAL_STOCK;
        }
    }

    private void despawnProps() {
        if (world != null) {
            List<PropPosition> allProps = world.getPropPositions();
            allProps.removeAll(feteProps);
        }
        feteProps.clear();
        propsSpawned = false;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean isFeteDay(int dayOfMonth, int dayCount, int month) {
        if (month != 6) return false; // July = 6
        if (dayCount % 7 != SATURDAY) return false;
        return dayOfMonth >= 1 && dayOfMonth <= 7;
    }

    private boolean isNpcTypeNearby(List<NPC> allNpcs, NPCType type,
                                     float px, float pz, float radius) {
        if (allNpcs == null) return false;
        for (NPC npc : allNpcs) {
            if (npc == null || !npc.isAlive()) continue;
            if (npc.getType() != type) continue;
            float dx = npc.getPosition().x - px;
            float dz = npc.getPosition().z - pz;
            if (dx * dx + dz * dz <= radius * radius) return true;
        }
        return false;
    }

    private float playerWinChance() {
        if (playerRaffleTickets <= 0) return 0f;
        // Approx: tickets sold at fete ~50 (rough estimate)
        return (float) playerRaffleTickets / 50f;
    }

    private int cakeTypeIndex(Material m) {
        for (int i = 0; i < CAKE_TYPES.length; i++) {
            if (CAKE_TYPES[i] == m) return i;
        }
        return -1;
    }

    private void seedRumour(String text) {
        if (rumourNetwork == null) return;
        NPC source = vicarNpc != null ? vicarNpc
                : (!volunteerNpcs.isEmpty() ? volunteerNpcs.get(0) : null);
        if (source == null) return;
        rumourNetwork.addRumour(source, new Rumour(RumourType.LOCAL_EVENT, text));
    }

    private boolean tomblonaWon = false;
    private boolean hookADuckWon = false;
    private boolean raffleWon = false;

    private void checkFeteChampion(AchievementCallback achCallback) {
        // A simplified check — in practice the game loop would call this
        if (tomblonaWon && hookADuckWon && raffleWon) {
            if (achievementSystem != null) achievementSystem.unlock(AchievementType.FETE_CHAMPION);
            if (achCallback != null) achCallback.award(AchievementType.FETE_CHAMPION);
        }
    }

    private void resetDailyState() {
        feteActive       = false;
        feteCancelled    = false;
        propsSpawned     = false;
        setupProcessed   = false;
        openProcessed    = false;
        restockProcessed = false;
        raffleDrawn      = false;
        raffleRigged     = false;
        closeProcessed   = false;
        playerEjected    = false;
        raffleFixInProgress = false;
        raffleFixTimer   = 0f;
        riggedBarrelPresent = false;
        hookADuckStreak  = 0;
        hookADuckBanned  = false;
        playerRaffleTickets = 0;
        closeTimer       = 0f;
        tomblonaWon      = false;
        hookADuckWon     = false;
        raffleWon        = false;
        volunteerNpcs.clear();
        feteNpcs.clear();
        vicarNpc         = null;
        feteProps.clear();
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    /** Returns true if the fete is currently active. */
    public boolean isFeteActive() { return feteActive; }

    /** Returns true if the fete was cancelled due to weather. */
    public boolean isFeteCancelled() { return feteCancelled; }

    /** Returns true if the raffle has been drawn. */
    public boolean getRaffleDrawn() { return raffleDrawn; }

    /** Returns true if the raffle draw was rigged by the player. */
    public boolean isRaffleRigged() { return raffleRigged; }

    /** Returns true if the player has been ejected from the fete. */
    public boolean isPlayerEjected() { return playerEjected; }

    /** Returns the current cake stall stock for the given type. */
    public int getCakeStock(Material cakeType) {
        int idx = cakeTypeIndex(cakeType);
        return idx >= 0 ? cakeStock[idx] : 0;
    }

    /** Returns the VICAR_NPC for this fete, or null. */
    public NPC getVicarNpc() { return vicarNpc; }

    /** Returns the VOLUNTEER NPCs for this fete. */
    public List<NPC> getVolunteerNpcs() { return volunteerNpcs; }

    /** Returns all fete NPC references. */
    public List<NPC> getFeteNpcs() { return feteNpcs; }

    /** Returns the fete prop positions. */
    public List<PropPosition> getFeteProps() { return feteProps; }

    /** Returns the player's raffle ticket count. */
    public int getPlayerRaffleTickets() { return playerRaffleTickets; }

    /** Returns the number of fetes attended (for BRITISH_INSTITUTION). */
    public int getFetesAttended() { return fetesAttended; }

    /** Returns the raffle fix progress timer (seconds). */
    public float getRaffleFixTimer() { return raffleFixTimer; }

    /** Returns whether the raffle fix is currently in progress. */
    public boolean isRaffleFixInProgress() { return raffleFixInProgress; }

    // ── Test helpers ───────────────────────────────────────────────────────────

    /** Force-set fete active state (for testing). */
    public void setFeteActiveForTesting(boolean active) { this.feteActive = active; }

    /** Force-set fete cancelled state (for testing). */
    public void setFeteCancelledForTesting(boolean cancelled) { this.feteCancelled = cancelled; }

    /** Force-set raffle drawn state (for testing). */
    public void setRaffleDrawnForTesting(boolean drawn) { this.raffleDrawn = drawn; }

    /** Force-set raffle rigged state (for testing). */
    public void setRaffleRiggedForTesting(boolean rigged) { this.raffleRigged = rigged; }

    /** Force-set player ejected state (for testing). */
    public void setPlayerEjectedForTesting(boolean ejected) { this.playerEjected = ejected; }

    /** Force-set player raffle tickets (for testing). */
    public void setPlayerRaffleTicketsForTesting(int count) { this.playerRaffleTickets = count; }

    /** Force-set setupProcessed (for testing — bypass setup logic). */
    public void setSetupProcessedForTesting(boolean processed) { this.setupProcessed = processed; }

    /** Force-set openProcessed (for testing). */
    public void setOpenProcessedForTesting(boolean processed) { this.openProcessed = processed; }

    /** Force-set props spawned (for testing). */
    public void setPropsSpawnedForTesting(boolean spawned) { this.propsSpawned = spawned; }

    /** Directly spawn props for testing (without time checks). */
    public void spawnPropsForTesting() {
        spawnProps();
        feteActive = true;
        propsSpawned = true;
    }

    /** Directly add a VICAR NPC for testing. */
    public void setVicarNpcForTesting(NPC vicar) { this.vicarNpc = vicar; }

    /** Directly add a VOLUNTEER NPC for testing. */
    public void addVolunteerNpcForTesting(NPC volunteer) {
        volunteerNpcs.add(volunteer);
        feteNpcs.add(volunteer);
    }

    /** Force-set the last processed day (for testing). */
    public void setLastProcessedDayForTesting(int day) { this.lastProcessedDay = day; }

    /** Set cake stock directly (for testing). */
    public void setCakeStockForTesting(int index, int count) {
        if (index >= 0 && index < cakeStock.length) cakeStock[index] = count;
    }

    /** Force-set close processed (for testing). */
    public void setCloseProcessedForTesting(boolean closed) { this.closeProcessed = closed; }

    /** Force-set restock processed (for testing). */
    public void setRestockProcessedForTesting(boolean processed) { this.restockProcessed = processed; }
}

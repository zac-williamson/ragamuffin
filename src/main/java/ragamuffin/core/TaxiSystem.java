package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #967: Northfield Taxi Rank — A1 Taxis, Dodgy Minicabs &amp; Cash-Only Night Rides.
 *
 * <p>Brings the existing {@link LandmarkType#TAXI_RANK} (displayed as "A1 Taxis") to life
 * with two distinct services:
 *
 * <h3>A1 Taxis — Mick the Dispatcher</h3>
 * <ul>
 *   <li>Mick ({@link NPCType#SHOPKEEPER}) staffs the kiosk {@link #MICK_OPEN_HOUR}–{@link #MICK_CLOSE_HOUR} (08:00–02:00).</li>
 *   <li>Three destinations matching bus stop positions: Park, Industrial Estate, High Street return.</li>
 *   <li>Day fares: Park {@link #FARE_PARK_DAY}, Industrial Estate {@link #FARE_INDUSTRIAL_DAY}, High Street {@link #FARE_HIGH_STREET_DAY}.</li>
 *   <li>Night fares (22:00–06:00): Park {@link #FARE_PARK_NIGHT}, Industrial Estate {@link #FARE_INDUSTRIAL_NIGHT}, High Street {@link #FARE_HIGH_STREET_NIGHT}.</li>
 *   <li>Player presses E on Mick to open destination menu; fare deducted, fast-travel to destination.</li>
 *   <li>Mick refuses at Notoriety {@link #REFUSAL_TIER} ("I know your face."); Tier 3 hesitates but accepts.</li>
 *   <li>Mick accumulates 1 rumour/hour from passing NPCs; player can ask "Hear anything?" free of charge.</li>
 *   <li>If player's CriminalRecord contains {@link CriminalRecord.CrimeType#FARE_EVASION}, Mick says "Cash upfront".</li>
 * </ul>
 *
 * <h3>TAXI_PASS</h3>
 * <ul>
 *   <li>5-journey prepaid card; buy from Mick for {@link #TAXI_PASS_PRICE} COIN.</li>
 *   <li>Each journey decrements ride counter; when exhausted, falls back to coin payment.</li>
 * </ul>
 *
 * <h3>Dave's Minicab (Late-Night Alternative)</h3>
 * <ul>
 *   <li>Beat-up hatchback parked near the pub {@link #DAVE_OPEN_HOUR}–{@link #DAVE_CLOSE_HOUR} (22:00–04:00).</li>
 *   <li>Driver uses {@link NPCType#MINICAB_DRIVER}.</li>
 *   <li>1 COIN cheaper than A1 Taxis but has risks:
 *       {@link #DAVE_DETOUR_CHANCE} chance of detour (drops player 20 blocks off target);
 *       {@link #DAVE_PACKAGE_CHANCE} chance of receiving a {@link Material#DODGY_PACKAGE} item
 *       ({@link NPCType#UNDERCOVER_POLICE} spawns on arrival with 20% chance).</li>
 *   <li>Refuses BALACLAVA-wearing players.</li>
 *   <li>Tooltip on first detour: "Never ask what's in the bag."</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#DODGY_PACKAGE} — received a dodgy package from Dave.</li>
 *   <li>{@link AchievementType#LAST_FARE} — took Dave's minicab after 02:00.</li>
 *   <li>{@link AchievementType#REGULAR_CUSTOMER} — used A1 Taxis 5 times.</li>
 * </ul>
 */
public class TaxiSystem {

    // ── Service hours ─────────────────────────────────────────────────────────

    /** Hour Mick opens (08:00). */
    public static final float MICK_OPEN_HOUR = 8.0f;

    /** Hour Mick closes (02:00 next day, stored as 26.0 for wrap-around check). */
    public static final float MICK_CLOSE_HOUR = 26.0f;  // 02:00 next day

    /** Hour Dave's minicab appears (22:00). */
    public static final float DAVE_OPEN_HOUR = 22.0f;

    /** Hour Dave's minicab disappears (04:00). */
    public static final float DAVE_CLOSE_HOUR = 4.0f;

    /** Hour at which "night" fares apply (22:00). */
    public static final float NIGHT_FARE_START = 22.0f;

    /** Hour at which day fares resume (06:00). */
    public static final float NIGHT_FARE_END = 6.0f;

    // ── Destination indices ───────────────────────────────────────────────────

    /** Destination: Park. */
    public static final int DEST_PARK = 0;

    /** Destination: Industrial Estate. */
    public static final int DEST_INDUSTRIAL = 1;

    /** Destination: High Street return. */
    public static final int DEST_HIGH_STREET = 2;

    /** Total number of destinations. */
    public static final int NUM_DESTINATIONS = 3;

    // ── A1 Taxis day fares ────────────────────────────────────────────────────

    /** Day fare to the Park. */
    public static final int FARE_PARK_DAY = 4;

    /** Day fare to the Industrial Estate. */
    public static final int FARE_INDUSTRIAL_DAY = 6;

    /** Day fare — High Street return. */
    public static final int FARE_HIGH_STREET_DAY = 3;

    // ── A1 Taxis night fares ──────────────────────────────────────────────────

    /** Night fare to the Park. */
    public static final int FARE_PARK_NIGHT = 7;

    /** Night fare to the Industrial Estate. */
    public static final int FARE_INDUSTRIAL_NIGHT = 10;

    /** Night fare — High Street return. */
    public static final int FARE_HIGH_STREET_NIGHT = 5;

    // ── TAXI_PASS ─────────────────────────────────────────────────────────────

    /** Cost to buy a TAXI_PASS from Mick. */
    public static final int TAXI_PASS_PRICE = 18;

    /** Number of journeys on a TAXI_PASS. */
    public static final int TAXI_PASS_JOURNEYS = 5;

    // ── Notoriety thresholds ──────────────────────────────────────────────────

    /** Notoriety tier at which Mick refuses service outright. */
    public static final int REFUSAL_TIER = 5;

    /** Notoriety tier at which Mick hesitates but still accepts. */
    public static final int HESITATION_TIER = 3;

    // ── Dave's minicab risk constants ─────────────────────────────────────────

    /** Chance (0–1) that Dave takes a detour, dropping player 20 blocks off target. */
    public static final float DAVE_DETOUR_CHANCE = 0.30f;

    /** Chance (0–1) that Dave slips the player a DODGY_PACKAGE. */
    public static final float DAVE_PACKAGE_CHANCE = 0.15f;

    /** Chance (0–1) that UNDERCOVER_POLICE spawns at destination if player holds DODGY_PACKAGE. */
    public static final float UNDERCOVER_POLICE_CHANCE = 0.20f;

    /** How far off-target Dave drops the player when a detour occurs (blocks). */
    public static final float DETOUR_OFFSET_BLOCKS = 20.0f;

    /** Dave's fare discount vs. A1 Taxis (1 COIN cheaper per journey). */
    public static final int DAVE_DISCOUNT = 1;

    // ── Rumour accumulation ───────────────────────────────────────────────────

    /** In-game hours between each rumour Mick picks up from passing NPCs. */
    public static final float RUMOUR_ACCUMULATE_HOURS = 1.0f;

    /** Maximum number of rumours Mick holds at once. */
    public static final int MAX_MICK_RUMOURS = 5;

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String MICK_GREETING = "A1 Taxis — where d'you need?";
    public static final String MICK_FARE_EVASION = "Cash upfront, mate. I know your record.";
    public static final String MICK_HESITATION = "...You sure? Alright, but you're paying now.";
    public static final String MICK_REFUSAL = "I know your face. Not getting in my cab.";
    public static final String MICK_NO_RUMOURS = "Quiet night. Not heard much.";
    public static final String MICK_CLOSED = "We're not running right now. Try again later.";
    public static final String DAVE_GREETING = "Hop in, mate. Cash only, yeah?";
    public static final String DAVE_DETOUR = "I know a shortcut.";
    public static final String DAVE_PACKAGE = "Don't ask what's in the bag.";
    public static final String DAVE_BALACLAVA_REFUSAL = "No masks. I've got a policy.";
    public static final String DAVE_CLOSED = "No cabs this time of night. Try A1 Taxis.";

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** The Mick dispatcher NPC (null if not spawned). */
    private NPC mickNpc = null;

    /** The Dave minicab driver NPC (null if not active). */
    private NPC daveNpc = null;

    /** Rides remaining on the player's TAXI_PASS (0 = no pass). */
    private int taxiPassRidesRemaining = 0;

    /** Accumulated rumours Mick has heard. */
    private final List<String> mickRumours = new ArrayList<>();

    /** Timer tracking in-game hours since last rumour accumulation. */
    private float rumourTimer = 0f;

    /** Total A1 Taxis journeys taken by the player (for REGULAR_CUSTOMER achievement). */
    private int totalMickJourneys = 0;

    /** Whether the first detour tooltip has been shown this session. */
    private boolean firstDetourTooltipShown = false;

    /** Last destination the player was taken to (for tracking). */
    private int lastDestination = -1;

    // ── Optional system references ────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;

    // ── Construction ──────────────────────────────────────────────────────────

    public TaxiSystem() {
        this(new Random());
    }

    public TaxiSystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) {
        this.notorietySystem = n;
    }

    public void setCriminalRecord(CriminalRecord c) {
        this.criminalRecord = c;
    }

    public void setAchievementSystem(AchievementSystem a) {
        this.achievementSystem = a;
    }

    public void setRumourNetwork(RumourNetwork r) {
        this.rumourNetwork = r;
    }

    // ── Main update ───────────────────────────────────────────────────────────

    /**
     * Update the taxi system each frame.
     *
     * @param delta        seconds since last frame (real-time)
     * @param hour         current in-game hour (0.0–24.0)
     * @param nearbyNpcs   nearby NPCs who may pass rumours to Mick
     */
    public void update(float delta, float hour, List<NPC> nearbyNpcs) {
        float normHour = normaliseHour(hour);

        // Spawn/despawn Mick
        if (isMickOpen(normHour)) {
            if (mickNpc == null) {
                mickNpc = new NPC(NPCType.SHOPKEEPER, 0f, 1f, 0f);
                mickNpc.setSpeechText(MICK_GREETING, 3f);
            }
        } else {
            if (mickNpc != null) {
                mickNpc.kill();
                mickNpc = null;
            }
        }

        // Spawn/despawn Dave
        if (isDaveOpen(normHour)) {
            if (daveNpc == null) {
                daveNpc = new NPC(NPCType.MINICAB_DRIVER, 5f, 1f, 5f);
                daveNpc.setSpeechText(DAVE_GREETING, 3f);
            }
        } else {
            if (daveNpc != null) {
                daveNpc.kill();
                daveNpc = null;
            }
        }

        // Accumulate rumours for Mick
        if (mickNpc != null && nearbyNpcs != null && !nearbyNpcs.isEmpty()) {
            rumourTimer += delta * (1f / 360f); // convert real seconds to in-game hours (6 min/real-sec)
            if (rumourTimer >= RUMOUR_ACCUMULATE_HOURS) {
                rumourTimer = 0f;
                accumulateRumour(nearbyNpcs);
            }
        }
    }

    /** Accumulate one rumour from nearby NPCs passing the rank. */
    private void accumulateRumour(List<NPC> nearbyNpcs) {
        if (mickRumours.size() >= MAX_MICK_RUMOURS) return;
        // Generic rumour lines Mick collects from fares
        String[] rumourPool = {
            "Heard there's something going on down the industrial estate tonight.",
            "Bloke I took earlier said the park's been busy.",
            "Word is the pub's got something happening this week.",
            "Some lads were talking about a job near the high street.",
            "Council's been sniffing around, apparently.",
            "Saw a dodgy van parked near the estate. Not mine, before you ask.",
            "Someone told me the charity shop's been shifting more than clothes.",
        };
        mickRumours.add(rumourPool[rng.nextInt(rumourPool.length)]);
    }

    // ── A1 Taxis — player interaction ─────────────────────────────────────────

    /**
     * Result of interacting with Mick's kiosk.
     */
    public enum MickResult {
        /** Kiosk is closed (outside 08:00–02:00). */
        CLOSED,
        /** Mick refuses — player is Tier 5 notoriety. */
        REFUSED,
        /** Mick hesitates but accepts — player is Tier 3 notoriety. */
        HESITATED_BUT_ACCEPTED,
        /** Player has FARE_EVASION on record — Mick demands cash upfront (no TAXI_PASS). */
        CASH_UPFRONT,
        /** Insufficient funds for the journey. */
        INSUFFICIENT_FUNDS,
        /** Journey successful — player fast-travelled to destination. */
        SUCCESS,
        /** TAXI_PASS used for this journey. */
        SUCCESS_PASS
    }

    /**
     * Player presses E on Mick — requests a taxi to the specified destination.
     *
     * @param hour            current in-game hour (0–24)
     * @param destination     destination index ({@link #DEST_PARK}, etc.)
     * @param playerInventory player's inventory
     * @return result of the interaction
     */
    public MickResult requestRide(float hour, int destination, Inventory playerInventory) {
        float normHour = normaliseHour(hour);

        if (!isMickOpen(normHour)) {
            if (mickNpc != null) mickNpc.setSpeechText(MICK_CLOSED, 4f);
            return MickResult.CLOSED;
        }

        // Notoriety check
        if (notorietySystem != null) {
            int tier = notorietySystem.getTier();
            if (tier >= REFUSAL_TIER) {
                if (mickNpc != null) mickNpc.setSpeechText(MICK_REFUSAL, 5f);
                return MickResult.REFUSED;
            }
        }

        boolean hasFareEvasion = criminalRecord != null
                && criminalRecord.getCount(CriminalRecord.CrimeType.FARE_EVASION) > 0;

        // Check for TAXI_PASS (not usable if fare evasion and cash-upfront mode)
        boolean usePass = false;
        if (!hasFareEvasion && taxiPassRidesRemaining > 0
                && playerInventory != null
                && playerInventory.getItemCount(Material.TAXI_PASS) > 0) {
            usePass = true;
        }

        int fare = computeFare(normHour, destination);

        if (hasFareEvasion && mickNpc != null) {
            mickNpc.setSpeechText(MICK_FARE_EVASION, 4f);
        }

        if (!usePass) {
            if (playerInventory == null || playerInventory.getItemCount(Material.COIN) < fare) {
                return MickResult.INSUFFICIENT_FUNDS;
            }
            playerInventory.removeItem(Material.COIN, fare);
        } else {
            // Decrement pass rides
            taxiPassRidesRemaining--;
            if (taxiPassRidesRemaining <= 0) {
                taxiPassRidesRemaining = 0;
                // Remove the exhausted pass from inventory
                if (playerInventory != null) {
                    playerInventory.removeItem(Material.TAXI_PASS, 1);
                }
            }
        }

        // Hesitation dialogue
        if (notorietySystem != null && notorietySystem.getTier() >= HESITATION_TIER) {
            if (mickNpc != null) mickNpc.setSpeechText(MICK_HESITATION, 4f);
        }

        lastDestination = destination;
        totalMickJourneys++;

        // REGULAR_CUSTOMER achievement
        if (achievementSystem != null && totalMickJourneys >= AchievementType.REGULAR_CUSTOMER.getProgressTarget()) {
            achievementSystem.unlock(AchievementType.REGULAR_CUSTOMER);
        }

        if (hasFareEvasion) {
            return MickResult.CASH_UPFRONT;
        }
        return usePass ? MickResult.SUCCESS_PASS : MickResult.SUCCESS;
    }

    /**
     * Player asks Mick "Hear anything?" — returns a rumour string or empty if none.
     * Free of charge; removes the rumour from Mick's queue.
     *
     * @return rumour text, or {@link #MICK_NO_RUMOURS} if nothing to share
     */
    public String askMickForRumour() {
        if (mickRumours.isEmpty()) {
            return MICK_NO_RUMOURS;
        }
        return mickRumours.remove(0);
    }

    // ── TAXI_PASS purchase ────────────────────────────────────────────────────

    /**
     * Result of purchasing a TAXI_PASS from Mick.
     */
    public enum PassPurchaseResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        KIOSK_CLOSED
    }

    /**
     * Player buys a TAXI_PASS from Mick.
     *
     * @param hour            current in-game hour
     * @param playerInventory player's inventory
     * @return result of the purchase
     */
    public PassPurchaseResult buyTaxiPass(float hour, Inventory playerInventory) {
        float normHour = normaliseHour(hour);
        if (!isMickOpen(normHour)) return PassPurchaseResult.KIOSK_CLOSED;
        if (playerInventory == null) return PassPurchaseResult.INSUFFICIENT_FUNDS;
        if (playerInventory.getItemCount(Material.COIN) < TAXI_PASS_PRICE) {
            return PassPurchaseResult.INSUFFICIENT_FUNDS;
        }
        playerInventory.removeItem(Material.COIN, TAXI_PASS_PRICE);
        playerInventory.addItem(Material.TAXI_PASS, 1);
        taxiPassRidesRemaining = TAXI_PASS_JOURNEYS;
        return PassPurchaseResult.SUCCESS;
    }

    // ── Dave's minicab ────────────────────────────────────────────────────────

    /**
     * Result of requesting Dave's minicab.
     */
    public enum DaveResult {
        /** Dave is not operating (outside 22:00–04:00). */
        CLOSED,
        /** Dave refuses BALACLAVA wearers. */
        BALACLAVA_REFUSED,
        /** Insufficient funds. */
        INSUFFICIENT_FUNDS,
        /** Normal successful journey. */
        SUCCESS,
        /** Journey completed but player was detoured 20 blocks off target. */
        DETOURED,
        /** Journey completed and player received a DODGY_PACKAGE. */
        SUCCESS_WITH_PACKAGE,
        /** Journey completed with detour AND a dodgy package. */
        DETOURED_WITH_PACKAGE
    }

    /**
     * Player requests Dave's minicab.
     *
     * @param hour            current in-game hour
     * @param destination     destination index
     * @param playerInventory player's inventory
     * @return result of the journey
     */
    public DaveResult requestDaveRide(float hour, int destination, Inventory playerInventory) {
        float normHour = normaliseHour(hour);

        if (!isDaveOpen(normHour)) {
            if (daveNpc != null) daveNpc.setSpeechText(DAVE_CLOSED, 4f);
            return DaveResult.CLOSED;
        }

        // Refuse BALACLAVA wearers
        if (playerInventory != null && playerInventory.getItemCount(Material.BALACLAVA) > 0) {
            if (daveNpc != null) daveNpc.setSpeechText(DAVE_BALACLAVA_REFUSAL, 4f);
            return DaveResult.BALACLAVA_REFUSED;
        }

        int fare = computeFare(normHour, destination) - DAVE_DISCOUNT;
        fare = Math.max(1, fare); // minimum 1 COIN

        if (playerInventory == null || playerInventory.getItemCount(Material.COIN) < fare) {
            return DaveResult.INSUFFICIENT_FUNDS;
        }
        playerInventory.removeItem(Material.COIN, fare);

        // LAST_FARE achievement — Dave operating past 02:00
        if ((normHour >= 2.0f && normHour < DAVE_CLOSE_HOUR) && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.LAST_FARE);
        }

        lastDestination = destination;

        boolean detoured = rng.nextFloat() < DAVE_DETOUR_CHANCE;
        boolean packageReceived = rng.nextFloat() < DAVE_PACKAGE_CHANCE;

        if (detoured && daveNpc != null) {
            daveNpc.setSpeechText(DAVE_DETOUR, 4f);
        }
        if (packageReceived) {
            if (playerInventory != null) {
                playerInventory.addItem(Material.DODGY_PACKAGE, 1);
            }
            if (daveNpc != null) daveNpc.setSpeechText(DAVE_PACKAGE, 4f);
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.DODGY_PACKAGE);
            }
        }

        if (detoured && packageReceived) return DaveResult.DETOURED_WITH_PACKAGE;
        if (detoured) return DaveResult.DETOURED;
        if (packageReceived) return DaveResult.SUCCESS_WITH_PACKAGE;
        return DaveResult.SUCCESS;
    }

    // ── Fare calculation ──────────────────────────────────────────────────────

    /**
     * Compute the A1 Taxis fare for the given destination and hour.
     *
     * @param normHour   normalised in-game hour (0–24)
     * @param destination destination index
     * @return fare in COIN (minimum 1)
     */
    public int computeFare(float normHour, int destination) {
        boolean night = isNightHour(normHour);
        switch (destination) {
            case DEST_PARK:
                return night ? FARE_PARK_NIGHT : FARE_PARK_DAY;
            case DEST_INDUSTRIAL:
                return night ? FARE_INDUSTRIAL_NIGHT : FARE_INDUSTRIAL_DAY;
            case DEST_HIGH_STREET:
                return night ? FARE_HIGH_STREET_NIGHT : FARE_HIGH_STREET_DAY;
            default:
                return night ? FARE_PARK_NIGHT : FARE_PARK_DAY;
        }
    }

    // ── TAXI_PASS — manual activation ─────────────────────────────────────────

    /**
     * Activate a TAXI_PASS from the player's inventory (called when player uses the item).
     *
     * @param playerInventory the player's inventory
     * @return true if pass was activated
     */
    public boolean activateTaxiPass(Inventory playerInventory) {
        if (playerInventory == null) return false;
        if (playerInventory.getItemCount(Material.TAXI_PASS) < 1) return false;
        taxiPassRidesRemaining = TAXI_PASS_JOURNEYS;
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Whether Mick's kiosk is open at the given hour.
     * Mick works 08:00–02:00 (wraps past midnight).
     */
    public boolean isMickOpen(float normHour) {
        // 08:00 to midnight: normHour >= 8.0
        // midnight to 02:00: normHour < 2.0
        return normHour >= MICK_OPEN_HOUR || normHour < 2.0f;
    }

    /**
     * Whether Dave's minicab is available at the given hour.
     * Dave operates 22:00–04:00 (wraps past midnight).
     */
    public boolean isDaveOpen(float normHour) {
        return normHour >= DAVE_OPEN_HOUR || normHour < DAVE_CLOSE_HOUR;
    }

    private boolean isNightHour(float normHour) {
        return normHour >= NIGHT_FARE_START || normHour < NIGHT_FARE_END;
    }

    private float normaliseHour(float hour) {
        return hour % 24.0f;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /**
     * Force-spawn Mick for testing.
     */
    public void forceSpawnMick() {
        if (mickNpc == null) {
            mickNpc = new NPC(NPCType.SHOPKEEPER, 0f, 1f, 0f);
        }
    }

    /**
     * Force-spawn Dave for testing.
     */
    public void forceSpawnDave() {
        if (daveNpc == null) {
            daveNpc = new NPC(NPCType.MINICAB_DRIVER, 5f, 1f, 5f);
        }
    }

    /**
     * Inject a rumour into Mick's queue (for testing).
     */
    public void addRumourForTesting(String rumour) {
        if (mickRumours.size() < MAX_MICK_RUMOURS) {
            mickRumours.add(rumour);
        }
    }

    /**
     * Set TAXI_PASS rides remaining (for testing).
     */
    public void setTaxiPassRidesForTesting(int rides) {
        this.taxiPassRidesRemaining = rides;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** The Mick dispatcher NPC (may be null if kiosk closed). */
    public NPC getMickNpc() {
        return mickNpc;
    }

    /** The Dave minicab driver NPC (may be null if not operating). */
    public NPC getDaveNpc() {
        return daveNpc;
    }

    /** Rides remaining on the active TAXI_PASS (0 = no pass). */
    public int getTaxiPassRidesRemaining() {
        return taxiPassRidesRemaining;
    }

    /** Number of rumours Mick is currently holding. */
    public int getMickRumourCount() {
        return mickRumours.size();
    }

    /** Total A1 Taxis journeys taken by the player. */
    public int getTotalMickJourneys() {
        return totalMickJourneys;
    }

    /** Last destination taken (destination index, or -1 if none yet). */
    public int getLastDestination() {
        return lastDestination;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1276: Northfield Minicab Office — Big Terry's Cabs, Cash-in-Hand Runs
 * &amp; the Unlicensed Driver Hustle.
 *
 * <p>Big Terry's Cabs is the unlicensed minicab office on the Northfield high street.
 * It offers cheaper fares than A1 Taxis with no night surcharge, but with greater
 * crime-system integration.
 *
 * <h3>Terry the Dispatcher</h3>
 * <ul>
 *   <li>Terry ({@link NPCType#MINICAB_DISPATCHER}) staffs the hatch
 *       Mon–Sat {@link #TERRY_OPEN_HOUR_WEEKDAY}–{@link #TERRY_CLOSE_HOUR_WEEKDAY} (07:00–02:00),
 *       Sun {@link #TERRY_OPEN_HOUR_SUNDAY}–{@link #TERRY_CLOSE_HOUR_SUNDAY} (10:00–23:00).</li>
 *   <li>Three destinations: Park (3 COIN), High Street (2 COIN), Industrial Estate (4 COIN).</li>
 *   <li>5% cheaper than A1 Taxis day rates. No night surcharge.</li>
 *   <li>Refuses Notoriety Tier 4+ ("I run a legitimate business.").</li>
 *   <li>Tier 3 accepted with flavour: "Don't cause me bother."</li>
 *   <li>{@link Material#TL_COUNCIL_PLATE} bypasses notoriety refusal.</li>
 * </ul>
 *
 * <h3>Cash-in-Hand Delivery Hustle</h3>
 * <ul>
 *   <li>Terry tasks player with delivering a {@link Material#DODGY_PACKAGE} within
 *       {@link #DELIVERY_TIME_LIMIT_SECONDS} in-game seconds.</li>
 *   <li>Reward: {@link #DELIVERY_COIN_REWARD} COIN + Marchetti Respect +1.</li>
 *   <li>{@link NPCType#UNDERCOVER_POLICE} has {@link #UNDERCOVER_POLICE_CHANCE} chance of
 *       stopping player mid-delivery. Refusing inspection: +2 Wanted stars.
 *       Consenting and failing: {@link CriminalRecord.CrimeType#POSSESSION_OF_STOLEN_GOODS}.</li>
 *   <li>Three successful deliveries unlock {@link AchievementType#CASH_IN_HAND}.</li>
 * </ul>
 *
 * <h3>Touting Hustle</h3>
 * <ul>
 *   <li>Player touts for fares from passing NPCs; earns {@link #TOUT_COIN_REWARD} COIN each.</li>
 *   <li>Without {@link Material#TL_COUNCIL_PLATE}: +1 Wanted star after every
 *       {@link #TOUTS_PER_WANTED_STAR} unlicensed touts.</li>
 *   <li>Five touts without plate spawns {@link NPCType#TRAFFIC_WARDEN} and issues
 *       {@link Material#FIXED_PENALTY_NOTICE} (15 COIN fine).</li>
 *   <li>Wearing {@link Material#HI_VIS_VEST} raises the traffic warden spawn threshold
 *       from {@link #TOUTS_BEFORE_WARDEN} to {@link #TOUTS_BEFORE_WARDEN_HI_VIS}.</li>
 * </ul>
 *
 * <h3>Ambient Radio Chatter</h3>
 * <ul>
 *   <li>Every {@link #RADIO_CHATTER_INTERVAL_SECONDS} real seconds, a flavour line from
 *       {@link #RADIO_CHATTER} cycles as a HUD subtitle.</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#CASH_IN_HAND} — 3 delivery runs without being stopped.</li>
 *   <li>{@link AchievementType#UNLICENSED_OPERATOR} — 10 touting jobs without TL_COUNCIL_PLATE.</li>
 *   <li>{@link AchievementType#BIG_TERRYS_REGULAR} — 5 paid rides with Big Terry's Cabs.</li>
 * </ul>
 */
public class MinicabSystem {

    // ── Service hours ──────────────────────────────────────────────────────────

    /** Hour Terry opens on weekdays (Mon–Sat): 07:00. */
    public static final float TERRY_OPEN_HOUR_WEEKDAY = 7.0f;

    /** Hour Terry closes on weekdays (02:00 next day). */
    public static final float TERRY_CLOSE_HOUR_WEEKDAY = 2.0f;

    /** Hour Terry opens on Sundays: 10:00. */
    public static final float TERRY_OPEN_HOUR_SUNDAY = 10.0f;

    /** Hour Terry closes on Sundays: 23:00. */
    public static final float TERRY_CLOSE_HOUR_SUNDAY = 23.0f;

    // ── Destination constants ──────────────────────────────────────────────────

    /** Destination index: Park. */
    public static final int DEST_PARK = 0;

    /** Destination index: High Street. */
    public static final int DEST_HIGH_STREET = 1;

    /** Destination index: Industrial Estate. */
    public static final int DEST_INDUSTRIAL = 2;

    /** Total number of destinations offered by Big Terry's. */
    public static final int NUM_DESTINATIONS = 3;

    // ── Fares (no night surcharge) ────────────────────────────────────────────

    /** Fare to the Park. */
    public static final int FARE_PARK = 3;

    /** Fare to the High Street. */
    public static final int FARE_HIGH_STREET = 2;

    /** Fare to the Industrial Estate. */
    public static final int FARE_INDUSTRIAL = 4;

    // ── Notoriety thresholds ──────────────────────────────────────────────────

    /** Tier at which Terry refuses outright. */
    public static final int REFUSAL_TIER = 4;

    /** Tier at which Terry accepts with flavour warning. */
    public static final int HESITATION_TIER = 3;

    // ── Delivery hustle constants ─────────────────────────────────────────────

    /** In-game seconds the player has to complete a delivery. */
    public static final float DELIVERY_TIME_LIMIT_SECONDS = 90.0f;

    /** Coin reward for a successful delivery. */
    public static final int DELIVERY_COIN_REWARD = 8;

    /** Marchetti Respect awarded per successful delivery. */
    public static final int DELIVERY_MARCHETTI_RESPECT = 1;

    /** Chance of UNDERCOVER_POLICE stop during delivery. */
    public static final float UNDERCOVER_POLICE_CHANCE = 0.15f;

    /** Wanted stars added if player refuses undercover inspection. */
    public static final int REFUSE_INSPECTION_WANTED_STARS = 2;

    /** Number of successful deliveries to unlock CASH_IN_HAND achievement. */
    public static final int DELIVERIES_FOR_CASH_IN_HAND = 3;

    // ── Touting hustle constants ──────────────────────────────────────────────

    /** Coin reward per successful tout. */
    public static final int TOUT_COIN_REWARD = 3;

    /** Unlicensed touts before +1 Wanted star. */
    public static final int TOUTS_PER_WANTED_STAR = 3;

    /** Unlicensed touts before TRAFFIC_WARDEN spawns (without HI_VIS_VEST). */
    public static final int TOUTS_BEFORE_WARDEN = 5;

    /** Unlicensed touts before TRAFFIC_WARDEN spawns when wearing HI_VIS_VEST. */
    public static final int TOUTS_BEFORE_WARDEN_HI_VIS = 8;

    /** Coin fine for the FIXED_PENALTY_NOTICE. */
    public static final int FIXED_PENALTY_COIN = 15;

    /** Number of unlicensed touting jobs to unlock UNLICENSED_OPERATOR achievement. */
    public static final int TOUTS_FOR_UNLICENSED_OPERATOR = 10;

    // ── Radio chatter ─────────────────────────────────────────────────────────

    /** Real-time seconds between radio chatter lines. */
    public static final float RADIO_CHATTER_INTERVAL_SECONDS = 90.0f;

    /** Ambient radio chatter lines cycling as HUD subtitles. */
    public static final String[] RADIO_CHATTER = {
        "Charlie, you there? Pick-up on the High Street, two minutes.",
        "Don't forget, cash only. I don't care what they say about cards.",
        "Jez, stay off the estate tonight — Old Bill's got a van parked up.",
        "Anyone know why Kenny's not answering? ...Kenny? KENNY.",
        "We are a professional operation. Tell the punters I said so.",
        "Don't give receipts. I keep saying this.",
        "If the council rings again, I'm in a meeting. I'm always in a meeting.",
        "Big Terry's Cabs — reliable, affordable, no questions asked."
    };

    // ── Distances ─────────────────────────────────────────────────────────────

    /** Distance (blocks) within A1 Taxis rank that triggers TURF_WAR rumour. */
    public static final float TURF_WAR_RANGE = 10.0f;

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String TERRY_GREETING     = "Big Terry's Cabs — where d'you need, love?";
    public static final String TERRY_REFUSAL      = "I run a legitimate business.";
    public static final String TERRY_HESITATION   = "Don't cause me bother.";
    public static final String TERRY_COUNCIL_PLATE = "Licensed and everything. Jump in.";
    public static final String TERRY_CLOSED       = "We're closed, mate.";
    public static final String TERRY_NO_MONEY     = "You're short, love. Come back when you've got it.";
    public static final String TERRY_DELIVERY_OFFER = "Got a little job for you, if you're up for it.";
    public static final String TERRY_DELIVERY_RETURN = "Nice one. Here's your cut.";
    public static final String UNDERCOVER_STOP    = "What's in the bag, son?";
    public static final String LOCAL_GOSSIP_TEXT  = "Terry's boys'll take you anywhere, no questions asked.";

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Terry the dispatcher NPC (null if not spawned). */
    private NPC terryNpc = null;

    /** Driver NPCs (Kenny and Jez). */
    private final List<NPC> driverNpcs = new ArrayList<>();

    /** Total paid rides taken with Big Terry's (for BIG_TERRYS_REGULAR achievement). */
    private int totalRides = 0;

    /** Successful deliveries in current session (for CASH_IN_HAND achievement). */
    private int sessionDeliveries = 0;

    /** Total unlicensed touting jobs completed (for UNLICENSED_OPERATOR achievement). */
    private int totalUnlicensedTouts = 0;

    /** Total touts (with or without plate) in current touting session. */
    private int sessionTouts = 0;

    /** Whether a TRAFFIC_WARDEN has been spawned for the current session. */
    private NPC trafficWardenNpc = null;

    /** Timer (real seconds) for radio chatter. */
    private float radioChatterTimer = 0f;

    /** Index of the next radio chatter line to display. */
    private int radioChatterIndex = 0;

    /** Last radio chatter line shown (for testing). */
    private String lastRadioChatterLine = null;

    /** Last destination the player was taken to. */
    private int lastDestination = -1;

    // ── Optional system references ─────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;

    // ── Construction ──────────────────────────────────────────────────────────

    public MinicabSystem() {
        this(new Random());
    }

    public MinicabSystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ──────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) { this.notorietySystem = n; }
    public void setWantedSystem(WantedSystem w)        { this.wantedSystem = w; }
    public void setCriminalRecord(CriminalRecord c)    { this.criminalRecord = c; }
    public void setAchievementSystem(AchievementSystem a) { this.achievementSystem = a; }
    public void setFactionSystem(FactionSystem f)      { this.factionSystem = f; }
    public void setRumourNetwork(RumourNetwork r)      { this.rumourNetwork = r; }

    // ── Main update ───────────────────────────────────────────────────────────

    /**
     * Update the minicab system each frame.
     *
     * @param delta    seconds since last frame (real-time)
     * @param hour     current in-game hour (0.0–24.0)
     * @param isSunday true if the current in-game day is Sunday
     */
    public void update(float delta, float hour, boolean isSunday) {
        float normHour = normaliseHour(hour);
        boolean open = isTerryOpen(normHour, isSunday);

        // Spawn/despawn Terry
        if (open) {
            if (terryNpc == null) {
                terryNpc = new NPC(NPCType.MINICAB_DISPATCHER, 0f, 1f, 0f);
                terryNpc.setSpeechText(TERRY_GREETING, 3f);
            }
            // Spawn up to 2 drivers if not present
            while (driverNpcs.size() < 2) {
                NPC driver = new NPC(NPCType.MINICAB_DRIVER,
                        (driverNpcs.size() + 1) * 3f, 1f, -2f);
                driverNpcs.add(driver);
            }
        } else {
            if (terryNpc != null) {
                terryNpc.kill();
                terryNpc = null;
            }
            for (NPC driver : driverNpcs) {
                driver.kill();
            }
            driverNpcs.clear();
        }

        // Radio chatter timer
        if (open) {
            radioChatterTimer += delta;
            if (radioChatterTimer >= RADIO_CHATTER_INTERVAL_SECONDS) {
                radioChatterTimer = 0f;
                lastRadioChatterLine = RADIO_CHATTER[radioChatterIndex % RADIO_CHATTER.length];
                radioChatterIndex++;
            }
        }
    }

    // ── Service availability ───────────────────────────────────────────────────

    /**
     * Whether Terry is open at the given hour on the given day type.
     *
     * @param normHour normalised hour (0–24)
     * @param isSunday true if the current day is Sunday
     */
    public boolean isTerryOpen(float normHour, boolean isSunday) {
        if (isSunday) {
            // Sunday: 10:00–23:00 (no wrap-around)
            return normHour >= TERRY_OPEN_HOUR_SUNDAY && normHour < TERRY_CLOSE_HOUR_SUNDAY;
        } else {
            // Weekday: 07:00–02:00 (wraps past midnight)
            return normHour >= TERRY_OPEN_HOUR_WEEKDAY || normHour < TERRY_CLOSE_HOUR_WEEKDAY;
        }
    }

    /**
     * Whether the service is available to the given player (considering notoriety
     * and whether the office is open).
     *
     * @param hour     current in-game hour
     * @param isSunday true if Sunday
     * @param inventory player's inventory (checked for TL_COUNCIL_PLATE)
     * @return true if Terry will serve the player
     */
    public boolean isServiceAvailable(float hour, boolean isSunday, Inventory inventory) {
        float normHour = normaliseHour(hour);
        if (!isTerryOpen(normHour, isSunday)) return false;

        // TL_COUNCIL_PLATE bypasses notoriety check
        if (inventory != null && inventory.getItemCount(Material.TL_COUNCIL_PLATE) > 0) {
            return true;
        }

        if (notorietySystem != null) {
            return notorietySystem.getTier() < REFUSAL_TIER;
        }
        return true;
    }

    // ── Fare calculation ──────────────────────────────────────────────────────

    /**
     * Compute the fare for a given destination. No night surcharge.
     *
     * @param destination destination index ({@link #DEST_PARK}, etc.)
     * @return fare in COIN
     */
    public int computeFare(int destination) {
        switch (destination) {
            case DEST_PARK:         return FARE_PARK;
            case DEST_HIGH_STREET:  return FARE_HIGH_STREET;
            case DEST_INDUSTRIAL:   return FARE_INDUSTRIAL;
            default:                return FARE_PARK;
        }
    }

    // ── Ride request ─────────────────────────────────────────────────────────

    /**
     * Result of requesting a ride from Terry.
     */
    public enum RideResult {
        /** Office is closed. */
        CLOSED,
        /** Terry refuses — Notoriety Tier 4+ and no TL_COUNCIL_PLATE. */
        REFUSED,
        /** Terry hesitates but accepts — Notoriety Tier 3. */
        HESITATED_BUT_ACCEPTED,
        /** Player accepted with TL_COUNCIL_PLATE despite high notoriety. */
        ACCEPTED_WITH_PLATE,
        /** Insufficient funds for the journey. */
        INSUFFICIENT_FUNDS,
        /** Successful journey. */
        SUCCESS
    }

    /**
     * Player presses E on Terry to request a ride.
     *
     * @param hour        current in-game hour
     * @param isSunday    true if the current day is Sunday
     * @param destination destination index
     * @param inventory   player's inventory
     * @return result of the interaction
     */
    public RideResult requestRide(float hour, boolean isSunday,
                                  int destination, Inventory inventory) {
        float normHour = normaliseHour(hour);

        if (!isTerryOpen(normHour, isSunday)) {
            if (terryNpc != null) terryNpc.setSpeechText(TERRY_CLOSED, 4f);
            return RideResult.CLOSED;
        }

        boolean hasCouncilPlate = inventory != null
                && inventory.getItemCount(Material.TL_COUNCIL_PLATE) > 0;

        // Notoriety check (council plate bypasses)
        if (!hasCouncilPlate && notorietySystem != null) {
            int tier = notorietySystem.getTier();
            if (tier >= REFUSAL_TIER) {
                if (terryNpc != null) terryNpc.setSpeechText(TERRY_REFUSAL, 5f);
                return RideResult.REFUSED;
            }
        }

        int fare = computeFare(destination);
        if (inventory == null || inventory.getItemCount(Material.COIN) < fare) {
            if (terryNpc != null) terryNpc.setSpeechText(TERRY_NO_MONEY, 4f);
            return RideResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, fare);
        lastDestination = destination;
        totalRides++;

        // Council plate dialogue
        if (hasCouncilPlate && notorietySystem != null
                && notorietySystem.getTier() >= REFUSAL_TIER) {
            if (terryNpc != null) terryNpc.setSpeechText(TERRY_COUNCIL_PLATE, 4f);
        } else if (!hasCouncilPlate && notorietySystem != null
                && notorietySystem.getTier() >= HESITATION_TIER) {
            if (terryNpc != null) terryNpc.setSpeechText(TERRY_HESITATION, 4f);
        } else {
            if (terryNpc != null) terryNpc.setSpeechText(TERRY_GREETING, 3f);
        }

        // Seed LOCAL_GOSSIP rumour on first ride
        if (totalRides == 1 && rumourNetwork != null && terryNpc != null) {
            rumourNetwork.addRumour(terryNpc, new Rumour(RumourType.LOCAL_EVENT, LOCAL_GOSSIP_TEXT));
        }

        // BIG_TERRYS_REGULAR achievement
        if (achievementSystem != null
                && totalRides >= AchievementType.BIG_TERRYS_REGULAR.getProgressTarget()) {
            achievementSystem.unlock(AchievementType.BIG_TERRYS_REGULAR);
        }

        if (hasCouncilPlate && notorietySystem != null
                && notorietySystem.getTier() >= REFUSAL_TIER) {
            return RideResult.ACCEPTED_WITH_PLATE;
        }
        if (!hasCouncilPlate && notorietySystem != null
                && notorietySystem.getTier() >= HESITATION_TIER) {
            return RideResult.HESITATED_BUT_ACCEPTED;
        }
        return RideResult.SUCCESS;
    }

    // ── Delivery hustle ───────────────────────────────────────────────────────

    /**
     * Result of a delivery hustle interaction.
     */
    public enum DeliveryResult {
        /** Delivery completed successfully; rewards issued. */
        SUCCESS,
        /** Delivery completed but undercover police stopped the player mid-delivery
         *  and the player refused inspection (+2 Wanted stars). */
        STOPPED_BY_POLICE_REFUSED,
        /** Delivery failed; the DODGY_PACKAGE was flagged and confiscated
         *  (POSSESSION_OF_STOLEN_GOODS recorded). */
        STOPPED_BY_POLICE_CONFISCATED,
        /** Time limit exceeded; no reward. */
        TIMED_OUT,
        /** Insufficient DODGY_PACKAGE in inventory. */
        NO_PACKAGE
    }

    /**
     * Player accepts a delivery job from Terry and completes it within the time limit.
     * Call this after the player has accepted the DODGY_PACKAGE and reached the target
     * address within {@link #DELIVERY_TIME_LIMIT_SECONDS} in-game seconds.
     *
     * @param deliveryTimeElapsed real seconds taken to complete the delivery
     * @param inventory           player's inventory
     * @return result of the delivery
     */
    public DeliveryResult completeDelivery(float deliveryTimeElapsed, Inventory inventory) {
        if (inventory == null || inventory.getItemCount(Material.DODGY_PACKAGE) < 1) {
            return DeliveryResult.NO_PACKAGE;
        }
        if (deliveryTimeElapsed > DELIVERY_TIME_LIMIT_SECONDS) {
            return DeliveryResult.TIMED_OUT;
        }

        // Remove the package
        inventory.removeItem(Material.DODGY_PACKAGE, 1);

        // Check for UNDERCOVER_POLICE stop
        if (rng.nextFloat() < UNDERCOVER_POLICE_CHANCE) {
            // Police stop: player must decide to refuse or consent
            // By default (for this method): treat as refused inspection
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(REFUSE_INSPECTION_WANTED_STARS,
                        0f, 0f, 0f, null);
            }
            return DeliveryResult.STOPPED_BY_POLICE_REFUSED;
        }

        // Successful delivery
        inventory.addItem(Material.COIN, DELIVERY_COIN_REWARD);
        sessionDeliveries++;

        // Marchetti Respect +1
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW,
                    DELIVERY_MARCHETTI_RESPECT);
        }

        // CASH_IN_HAND achievement after 3 successful deliveries
        if (achievementSystem != null
                && sessionDeliveries >= DELIVERIES_FOR_CASH_IN_HAND) {
            achievementSystem.unlock(AchievementType.CASH_IN_HAND);
        }

        if (terryNpc != null) terryNpc.setSpeechText(TERRY_DELIVERY_RETURN, 4f);
        return DeliveryResult.SUCCESS;
    }

    /**
     * Variant of {@link #completeDelivery} where the player explicitly consents
     * to an undercover police inspection and the package is flagged.
     *
     * @param inventory player's inventory
     * @return STOPPED_BY_POLICE_CONFISCATED
     */
    public DeliveryResult deliveryConsentedAndFailed(Inventory inventory) {
        if (inventory != null) {
            inventory.removeItem(Material.DODGY_PACKAGE, 1);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.POSSESSION_OF_STOLEN_GOODS);
        }
        return DeliveryResult.STOPPED_BY_POLICE_CONFISCATED;
    }

    /**
     * Player starts a delivery run: Terry gives the player a DODGY_PACKAGE.
     *
     * @param inventory player's inventory
     * @return true if the package was issued
     */
    public boolean startDelivery(Inventory inventory) {
        if (inventory == null) return false;
        inventory.addItem(Material.DODGY_PACKAGE, 1);
        if (terryNpc != null) terryNpc.setSpeechText(TERRY_DELIVERY_OFFER, 4f);
        return true;
    }

    // ── Touting hustle ────────────────────────────────────────────────────────

    /**
     * Result of a touting action.
     */
    public enum ToutResult {
        /** Tout successful; COIN awarded. */
        SUCCESS,
        /** Tout resulted in a Wanted star (after 3 unlicensed touts without plate). */
        SUCCESS_WANTED_STAR,
        /** TRAFFIC_WARDEN spawned and FIXED_PENALTY_NOTICE issued. */
        WARDEN_SPAWNED,
        /** No passing NPCs available to tout. */
        NO_TARGET
    }

    /**
     * Player touts for a fare from a passing NPC.
     *
     * @param inventory      player's inventory
     * @param nearbyNpcs     NPCs near the player to tout at
     * @param playerX        player X position (for turf-war range check)
     * @param playerZ        player Z position (for turf-war range check)
     * @param a1TaxisRankX   X position of A1 Taxis rank (for turf-war check)
     * @param a1TaxisRankZ   Z position of A1 Taxis rank (for turf-war check)
     * @return result of the touting action
     */
    public ToutResult tout(Inventory inventory, List<NPC> nearbyNpcs,
                           float playerX, float playerZ,
                           float a1TaxisRankX, float a1TaxisRankZ) {
        if (nearbyNpcs == null || nearbyNpcs.isEmpty()) {
            return ToutResult.NO_TARGET;
        }

        // Find a PUBLIC or WORKER NPC to tout at
        NPC target = null;
        for (NPC npc : nearbyNpcs) {
            if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.COMMUTER) {
                target = npc;
                break;
            }
        }
        if (target == null) return ToutResult.NO_TARGET;

        boolean hasCouncilPlate = inventory != null
                && inventory.getItemCount(Material.TL_COUNCIL_PLATE) > 0;
        boolean hasHiVis = inventory != null
                && inventory.getItemCount(Material.HI_VIS_VEST) > 0;

        // Award coin
        if (inventory != null) {
            inventory.addItem(Material.COIN, TOUT_COIN_REWARD);
        }

        sessionTouts++;

        // Check turf-war proximity to A1 Taxis rank
        float dx = playerX - a1TaxisRankX;
        float dz = playerZ - a1TaxisRankZ;
        float distSq = dx * dx + dz * dz;
        if (distSq <= TURF_WAR_RANGE * TURF_WAR_RANGE && rumourNetwork != null) {
            rumourNetwork.addRumour(target,
                    new Rumour(RumourType.TURF_WAR, "Big Terry's boys are nicking A1's trade."));
        }

        if (!hasCouncilPlate) {
            totalUnlicensedTouts++;

            // Record unlicensed touting
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.UNLICENSED_TOUTING);
            }

            // +1 Wanted star per TOUTS_PER_WANTED_STAR unlicensed touts
            if (totalUnlicensedTouts % TOUTS_PER_WANTED_STAR == 0) {
                if (wantedSystem != null) {
                    wantedSystem.addWantedStars(1, playerX, 0f, playerZ, null);
                }
            }

            // UNLICENSED_OPERATOR achievement at 10 unlicensed touts
            if (achievementSystem != null
                    && totalUnlicensedTouts >= TOUTS_FOR_UNLICENSED_OPERATOR) {
                achievementSystem.unlock(AchievementType.UNLICENSED_OPERATOR);
            }

            // Check traffic warden threshold
            int wardenThreshold = hasHiVis ? TOUTS_BEFORE_WARDEN_HI_VIS : TOUTS_BEFORE_WARDEN;
            if (sessionTouts >= wardenThreshold && trafficWardenNpc == null) {
                // Spawn TRAFFIC_WARDEN
                trafficWardenNpc = new NPC(NPCType.TRAFFIC_WARDEN, playerX + 3f, 1f, playerZ);
                // Issue FIXED_PENALTY_NOTICE
                if (inventory != null) {
                    inventory.addItem(Material.FIXED_PENALTY_NOTICE, 1);
                    inventory.removeItem(Material.COIN,
                            Math.min(FIXED_PENALTY_COIN, inventory.getItemCount(Material.COIN)));
                }
                return ToutResult.WARDEN_SPAWNED;
            }

            // Return star result if one was just added
            if (totalUnlicensedTouts % TOUTS_PER_WANTED_STAR == 0) {
                return ToutResult.SUCCESS_WANTED_STAR;
            }
        }

        return ToutResult.SUCCESS;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float normaliseHour(float hour) {
        return hour % 24.0f;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force-spawn Terry for testing. */
    public void forceSpawnTerry() {
        if (terryNpc == null) {
            terryNpc = new NPC(NPCType.MINICAB_DISPATCHER, 0f, 1f, 0f);
        }
    }

    /** Set session deliveries count (for testing). */
    public void setSessionDeliveriesForTesting(int count) {
        this.sessionDeliveries = count;
    }

    /** Set session touts count (for testing). */
    public void setSessionToutsForTesting(int count) {
        this.sessionTouts = count;
    }

    /** Set total unlicensed touts (for testing). */
    public void setTotalUnlicensedToutsForTesting(int count) {
        this.totalUnlicensedTouts = count;
    }

    /** Reset the traffic warden NPC (for testing). */
    public void clearTrafficWardenForTesting() {
        this.trafficWardenNpc = null;
    }

    /** Advance radio chatter timer by a fixed amount (for testing). */
    public void advanceRadioChatterTimer(float seconds) {
        radioChatterTimer += seconds;
        if (radioChatterTimer >= RADIO_CHATTER_INTERVAL_SECONDS) {
            radioChatterTimer -= RADIO_CHATTER_INTERVAL_SECONDS;
            lastRadioChatterLine = RADIO_CHATTER[radioChatterIndex % RADIO_CHATTER.length];
            radioChatterIndex++;
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Terry the dispatcher NPC (may be null if office is closed). */
    public NPC getTerryNpc() { return terryNpc; }

    /** List of active driver NPCs. */
    public List<NPC> getDriverNpcs() { return driverNpcs; }

    /** Active TRAFFIC_WARDEN NPC (null if none spawned). */
    public NPC getTrafficWardenNpc() { return trafficWardenNpc; }

    /** Total paid rides taken with Big Terry's Cabs. */
    public int getTotalRides() { return totalRides; }

    /** Successful deliveries in current session. */
    public int getSessionDeliveries() { return sessionDeliveries; }

    /** Total unlicensed touting jobs completed. */
    public int getTotalUnlicensedTouts() { return totalUnlicensedTouts; }

    /** Touts in current session. */
    public int getSessionTouts() { return sessionTouts; }

    /** Last radio chatter line displayed (null if none yet). */
    public String getLastRadioChatterLine() { return lastRadioChatterLine; }

    /** Radio chatter cycle index. */
    public int getRadioChatterIndex() { return radioChatterIndex; }

    /** Last destination taken (index, or -1 if none). */
    public int getLastDestination() { return lastDestination; }
}

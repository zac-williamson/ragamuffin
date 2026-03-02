package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.Random;

/**
 * Issue #1159: Northfield Angel Nails &amp; Beauty — Gossip Economy, WAG Culture &amp;
 * the Acrylic Hustle.
 *
 * <p>Angel Nails &amp; Beauty ({@link ragamuffin.world.LandmarkType#NAIL_SALON}) is an
 * 8×6×3 RENDER_PINK shopfront run by Trang ({@link ragamuffin.entity.NPCType#NAIL_TECH})
 * and her junior Kim.  It is the gossip hub of Northfield, a light disguise mechanic,
 * and — once the player earns sufficient Street Rep — a minor fence for stolen goods.
 *
 * <h3>Opening Hours</h3>
 * <ul>
 *   <li>Mon–Sat 09:00–19:00.</li>
 *   <li>Closed Sundays.</li>
 * </ul>
 *
 * <h3>Services</h3>
 * <ul>
 *   <li>Manicure — {@link #PRICE_MANICURE} COIN; −1 Notoriety display.</li>
 *   <li>Gel Manicure — {@link #PRICE_GEL} COIN; +2 CHA for 12 in-game hours.</li>
 *   <li>Pedicure — {@link #PRICE_PEDICURE} COIN; flavour only.</li>
 *   <li>Full Set Acrylics — {@link #PRICE_ACRYLICS} COIN; {@code ACRYLICS_DONE} flag;
 *       {@code DISGUISE_ACRYLIC} modifier.</li>
 * </ul>
 *
 * <h3>Gossip Economy</h3>
 * Player exchanges intel with seated CLIENT NPCs at the waiting bench.
 * Seeds {@link RumourType#SALON_GOSSIP} each exchange.  Trang seeds one SALON_GOSSIP
 * rumour daily at 09:00; at Street Rep ≥ 20 also seeds {@link RumourType#CONTRABAND_SHIPMENT}
 * once per 3 game days when a relevant faction event is active.
 *
 * <h3>Back-Door Fence</h3>
 * At Street Rep ≥ {@link #BACK_ROOM_REP_THRESHOLD}, Trang's cousin drops a package
 * once per game day ({@link #FENCE_DROP_CHANCE} chance, 10:00–16:00 window).
 * Contents: {@link Material#NAIL_POLISH} ×6, {@link Material#STOLEN_JEWELLERY},
 * or {@link Material#COUNTERFEIT_PERFUME}.
 *
 * <h3>WAG Saturday</h3>
 * Sat 10:00–12:00: services cost 1.5× (rounded up).  If Marchetti Respect ≥
 * {@link #STACEY_MARCHETTI_RESPECT_THRESHOLD}, Stacey Marchetti appears and seeds
 * a Marchetti faction rumour.
 *
 * <h3>Nail Polish Theft</h3>
 * Base catch chance {@link #THEFT_CATCH_CHANCE_BASE}; rises to
 * {@link #THEFT_CATCH_CHANCE_KIM} when Kim is watching.
 * Caught: 2-day ban, Notoriety +{@link #THEFT_NOTORIETY_PENALTY},
 * {@link ragamuffin.core.CriminalRecord.CrimeType#SHOPLIFTING}.
 *
 * <h3>Closing-Time Hustle</h3>
 * At 18:45 Kim leaves a cash envelope on the desk for 2 minutes.
 * Stealing it yields {@link #ENVELOPE_COIN} COIN,
 * Notoriety +{@link #ENVELOPE_NOTORIETY_PENALTY},
 * {@link ragamuffin.core.CriminalRecord.CrimeType#THEFT}.
 */
public class NailSalonSystem {

    // ── Day-of-week constants (dayCount % 7; 0=Mon … 5=Sat, 6=Sun) ──────────
    public static final int MONDAY   = 0;
    public static final int SATURDAY = 5;
    public static final int SUNDAY   = 6;

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Opening hour Mon–Sat. */
    public static final float OPEN_HOUR  = 9.0f;
    /** Closing hour Mon–Sat. */
    public static final float CLOSE_HOUR = 19.0f;

    // ── WAG Saturday window ───────────────────────────────────────────────────

    /** WAG appointment window start (Saturday only). */
    public static final float WAG_WINDOW_START = 10.0f;
    /** WAG appointment window end (Saturday only). */
    public static final float WAG_WINDOW_END   = 12.0f;

    // ── Service prices (base, weekday) ────────────────────────────────────────

    /** Base price for a standard manicure. */
    public static final int PRICE_MANICURE  = 2;
    /** Base price for a gel manicure. */
    public static final int PRICE_GEL       = 5;
    /** Base price for a pedicure. */
    public static final int PRICE_PEDICURE  = 3;
    /** Base price for a full set of acrylics. */
    public static final int PRICE_ACRYLICS  = 8;

    /**
     * WAG Saturday price multiplier applied to all services during
     * {@link #WAG_WINDOW_START}–{@link #WAG_WINDOW_END} on Saturday.
     * Rounded up to the nearest integer.
     */
    public static final float WAG_PRICE_MULTIPLIER = 1.5f;

    // ── Street Rep thresholds ─────────────────────────────────────────────────

    /** Minimum Street Rep to access the back-room fence. */
    public static final int BACK_ROOM_REP_THRESHOLD  = 30;
    /** Minimum Street Rep for Trang to share tips/CONTRABAND rumours. */
    public static final int TRANG_INTEL_REP_THRESHOLD = 20;
    /** Minimum Street Rep for Trang to issue a NAIL_SALON_VOUCHER. */
    public static final int VOUCHER_REP_THRESHOLD    = 50;

    // ── Faction thresholds ────────────────────────────────────────────────────

    /** Marchetti Respect required for Stacey Marchetti to appear on WAG Saturday. */
    public static final int STACEY_MARCHETTI_RESPECT_THRESHOLD = 40;

    // ── WantedSystem refusal ──────────────────────────────────────────────────

    /** Wanted stars at or above which Trang refuses entry. */
    public static final int REFUSE_WANTED_STARS = 3;

    // ── Nail polish theft ─────────────────────────────────────────────────────

    /** Catch chance (0–1) when Kim is NOT watching. */
    public static final float THEFT_CATCH_CHANCE_BASE = 0.25f;
    /** Catch chance (0–1) when Kim IS watching. */
    public static final float THEFT_CATCH_CHANCE_KIM  = 0.60f;
    /** Notoriety added when caught stealing nail polish. */
    public static final int THEFT_NOTORIETY_PENALTY   = 2;
    /** Duration of the ban (in-game days) when caught. */
    public static final int THEFT_BAN_DAYS            = 2;

    // ── Closing-time envelope ─────────────────────────────────────────────────

    /** Hour at which Kim leaves the cash envelope on the desk. */
    public static final float ENVELOPE_HOUR         = 18.75f; // 18:45
    /** Duration (in-game minutes) the envelope remains on the desk. */
    public static final float ENVELOPE_WINDOW_MINUTES = 2.0f;
    /** COIN value of the cash envelope. */
    public static final int ENVELOPE_COIN           = 15;
    /** Notoriety penalty for stealing the envelope. */
    public static final int ENVELOPE_NOTORIETY_PENALTY = 4;

    // ── Gel manicure buff ─────────────────────────────────────────────────────

    /** CHA bonus granted by a gel manicure (applied externally). */
    public static final int GEL_CHA_BONUS = 2;
    /** Duration of the gel CHA buff in in-game hours. */
    public static final float GEL_BUFF_HOURS = 12.0f;

    // ── Manicure notoriety display reduction ──────────────────────────────────

    /** Notoriety display reduction from a manicure or gel service. */
    public static final int MANICURE_NOTORIETY_REDUCTION = 1;

    // ── Disguise: acrylics modifier ───────────────────────────────────────────

    /** Recognition reduction with PENSIONER/WAG NPCs from DISGUISE_ACRYLIC. */
    public static final int ACRYLIC_RECOGNITION_REDUCTION_FRIENDLY = 10;
    /** Recognition reduction with STREET_LAD NPCs (negative = increases suspicion). */
    public static final int ACRYLIC_RECOGNITION_REDUCTION_LAD = -5;

    // ── Back-door fence ───────────────────────────────────────────────────────

    /** Daily probability (0–1) that Trang's cousin drops a package. */
    public static final float FENCE_DROP_CHANCE = 0.40f;
    /** Earliest hour the courier can arrive (inclusive). */
    public static final float FENCE_DROP_HOUR_MIN = 10.0f;
    /** Latest hour the courier can arrive (exclusive). */
    public static final float FENCE_DROP_HOUR_MAX = 16.0f;

    /** Coin value of COUNTERFEIT_PERFUME when fenced. */
    public static final int COUNTERFEIT_PERFUME_FENCE_VALUE = 4;
    /** Min coin value of STOLEN_JEWELLERY when fenced. */
    public static final int STOLEN_JEWELLERY_FENCE_MIN = 8;
    /** Max coin value of STOLEN_JEWELLERY when fenced. */
    public static final int STOLEN_JEWELLERY_FENCE_MAX = 15;
    /** Quantity of NAIL_POLISH in a bulk drop. */
    public static final int NAIL_POLISH_BULK_COUNT = 6;

    // ── Visit tracking ────────────────────────────────────────────────────────

    /** Number of salon visits required for the SALON_REGULAR achievement. */
    public static final int SALON_REGULAR_VISIT_COUNT = 5;
    /** Number of gossip exchanges required for the GOSSIP_QUEEN achievement. */
    public static final int GOSSIP_QUEEN_EXCHANGE_COUNT = 10;

    // ── NeighbourhoodSystem ───────────────────────────────────────────────────

    /** Neighbourhood vibes threshold below which the salon boards up its window. */
    public static final int BOARD_UP_VIBES_THRESHOLD = 30;
    /** Client count reduced to this when neighbourhood vibes are too low. */
    public static final int LOW_VIBES_CLIENT_COUNT = 1;

    // ── Weather ───────────────────────────────────────────────────────────────

    /** Extra CLIENT NPCs added during RAIN or DRIZZLE. */
    public static final int RAIN_EXTRA_CLIENT_COUNT = 1;

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────

    /** Total visits to the salon by the player (for SALON_REGULAR achievement). */
    private int totalVisits = 0;

    /** Total gossip exchanges completed (for GOSSIP_QUEEN achievement). */
    private int totalGossipExchanges = 0;

    /** Whether the player currently has the ACRYLICS_DONE flag. */
    private boolean acrylicsDone = false;

    /** Remaining in-game minutes for the gel CHA buff. */
    private float gelBuffRemaining = 0f;

    /** Whether a courier drop is available in the SUPPLY_CABINET_PROP today. */
    private boolean hasCourierDroppedToday = false;

    /** Whether the cash envelope is currently on the desk. */
    private boolean envelopePending = false;

    /** Remaining in-game minutes the envelope stays on the desk. */
    private float envelopeTimeRemaining = 0f;

    /** Whether the player is currently banned from the salon. */
    private boolean bannedFromSalon = false;

    /** Remaining in-game days on the current ban (0 = not banned). */
    private float banDaysRemaining = 0f;

    /** Random instance for probabilistic mechanics. */
    private final Random random;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public NailSalonSystem() {
        this.random = new Random();
    }

    public NailSalonSystem(Random random) {
        this.random = random;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the salon is open at the given hour and day.
     *
     * @param hour      current in-game hour (0.0–24.0)
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if open for business
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == SUNDAY) return false;
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WAG Saturday window
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if we are within the WAG appointment window (Sat 10:00–12:00).
     *
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if WAG Saturday window is active
     */
    public boolean isWagSaturdayWindow(float hour, int dayOfWeek) {
        return dayOfWeek == SATURDAY && hour >= WAG_WINDOW_START && hour < WAG_WINDOW_END;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Service types & pricing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The services available at Angel Nails &amp; Beauty.
     */
    public enum ServiceType {
        MANICURE(PRICE_MANICURE),
        GEL(PRICE_GEL),
        PEDICURE(PRICE_PEDICURE),
        ACRYLICS(PRICE_ACRYLICS);

        private final int basePrice;

        ServiceType(int basePrice) {
            this.basePrice = basePrice;
        }

        public int getBasePrice() {
            return basePrice;
        }
    }

    /**
     * Result of a service purchase attempt.
     */
    public enum ServiceResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        SHOP_CLOSED,
        REFUSED_BANNED,
        REFUSED_WANTED,
        INSUFFICIENT_REP
    }

    /**
     * Calculate the actual price for a service, accounting for WAG Saturday premium.
     *
     * @param service    the service type
     * @param isSaturday whether today is Saturday AND within the WAG window
     * @return COIN cost (rounded up for Saturday premium)
     */
    public int getServicePrice(ServiceType service, boolean isSaturday) {
        int base = service.getBasePrice();
        if (isSaturday) {
            return (int) Math.ceil(base * WAG_PRICE_MULTIPLIER);
        }
        return base;
    }

    /**
     * Attempt to purchase a nail service.
     *
     * <p>On SUCCESS:
     * <ol>
     *   <li>Deducts COIN from inventory.</li>
     *   <li>Applies buff/flag to player.</li>
     *   <li>Reduces Notoriety display for manicure/gel.</li>
     *   <li>Awards achievements.</li>
     * </ol>
     *
     * @param service         the requested service
     * @param inventory       the player's inventory
     * @param hour            current in-game hour
     * @param dayOfWeek       0=Mon … 5=Sat, 6=Sun
     * @param wantedStars     player's current wanted stars
     * @param notorietySystem for display notoriety reduction
     * @param achievementCb   callback for awarding achievements (may be null)
     * @return result of the attempt
     */
    public ServiceResult purchaseService(ServiceType service,
                                         Inventory inventory,
                                         float hour,
                                         int dayOfWeek,
                                         int wantedStars,
                                         NotorietySystem notorietySystem,
                                         NotorietySystem.AchievementCallback achievementCb) {
        if (!isOpen(hour, dayOfWeek)) {
            return ServiceResult.SHOP_CLOSED;
        }
        if (bannedFromSalon) {
            return ServiceResult.REFUSED_BANNED;
        }
        if (wantedStars >= REFUSE_WANTED_STARS) {
            return ServiceResult.REFUSED_WANTED;
        }

        boolean isSatWindow = isWagSaturdayWindow(hour, dayOfWeek);
        int cost = getServicePrice(service, isSatWindow);

        if (!inventory.hasItem(Material.COIN, cost)) {
            return ServiceResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, cost);

        // Apply service effect
        switch (service) {
            case MANICURE:
                applyManicureEffect(notorietySystem, achievementCb);
                break;
            case GEL:
                applyManicureEffect(notorietySystem, achievementCb);
                gelBuffRemaining = GEL_BUFF_HOURS * 60f; // convert to in-game minutes
                break;
            case PEDICURE:
                // Flavour only
                break;
            case ACRYLICS:
                acrylicsDone = true;
                if (achievementCb != null) {
                    achievementCb.award(AchievementType.FULL_SET);
                }
                break;
        }

        totalVisits++;
        if (achievementCb != null && totalVisits >= SALON_REGULAR_VISIT_COUNT) {
            achievementCb.award(AchievementType.SALON_REGULAR);
        }

        return ServiceResult.SUCCESS;
    }

    private void applyManicureEffect(NotorietySystem notorietySystem,
                                      NotorietySystem.AchievementCallback achievementCb) {
        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(MANICURE_NOTORIETY_REDUCTION, achievementCb);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gossip exchange
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Perform a gossip exchange with a seated CLIENT NPC.
     * Player provides intel; client returns a rumour; SALON_GOSSIP is seeded.
     *
     * @param clientNpc     the CLIENT NPC at the adjacent nail station
     * @param rumourNetwork the game's rumour network (may be null)
     * @param achievementCb callback for GOSSIP_QUEEN achievement (may be null)
     * @return true if the exchange succeeded
     */
    public boolean performGossipExchange(NPC clientNpc,
                                          RumourNetwork rumourNetwork,
                                          NotorietySystem.AchievementCallback achievementCb) {
        if (clientNpc == null) return false;

        // Seed a SALON_GOSSIP rumour into the network
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(clientNpc,
                    new Rumour(RumourType.SALON_GOSSIP,
                            "Tracy from the nail salon was saying things — the usual. You know how it is."));
        }

        totalGossipExchanges++;
        if (achievementCb != null && totalGossipExchanges >= GOSSIP_QUEEN_EXCHANGE_COUNT) {
            achievementCb.award(AchievementType.GOSSIP_QUEEN);
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back-door fence
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the back room is accessible to the player.
     *
     * @param streetRep player's current Street Reputation points
     * @return true if accessible (either by rep or lockpick not checked here)
     */
    public boolean isBackRoomAccessible(int streetRep) {
        return streetRep >= BACK_ROOM_REP_THRESHOLD;
    }

    /**
     * Called when the game day advances: rolls for a courier drop and resets
     * daily state.
     *
     * @param hour current in-game hour (0–23; roll only triggers inside 10–16)
     */
    public void onHourTick(int hour) {
        // Reset courier flag at midnight
        if (hour == 0) {
            hasCourierDroppedToday = false;
        }
        // Roll for courier drop during the fence window
        if (!hasCourierDroppedToday && hour >= (int) FENCE_DROP_HOUR_MIN && hour < (int) FENCE_DROP_HOUR_MAX) {
            if (random.nextFloat() < FENCE_DROP_CHANCE) {
                hasCourierDroppedToday = true;
            }
        }
        // Spawn envelope at 18:45
        if (hour == 18 && !envelopePending) {
            // 18:45 is checked in update() — flag it here for simplicity at 18:00
            // Precise timing handled by envelopeTimeRemaining countdown
        }
    }

    /**
     * Collect the supply cabinet contents (back-door fence).
     * Adds one of: NAIL_POLISH ×6, STOLEN_JEWELLERY, or COUNTERFEIT_PERFUME.
     *
     * @param inventory   the player's inventory
     * @param streetRep   player's Street Rep
     * @return true if a drop was available and collected
     */
    public boolean collectFenceDrop(Inventory inventory, int streetRep) {
        if (!isBackRoomAccessible(streetRep)) return false;
        if (!hasCourierDroppedToday) return false;
        hasCourierDroppedToday = false;

        // Distribute: 50% NAIL_POLISH bulk, 25% STOLEN_JEWELLERY, 25% COUNTERFEIT_PERFUME
        int roll = random.nextInt(4);
        if (roll < 2) {
            inventory.addItem(Material.NAIL_POLISH, NAIL_POLISH_BULK_COUNT);
        } else if (roll == 2) {
            inventory.addItem(Material.STOLEN_JEWELLERY, 1);
        } else {
            inventory.addItem(Material.COUNTERFEIT_PERFUME, 1);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Nail polish theft
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the catch probability for stealing from the COLOUR_WALL_PROP.
     *
     * @param kimWatching whether Kim NPC currently has line-of-sight to the player
     * @return catch chance (0.0–1.0)
     */
    public float getNailPolishTheftCatchChance(boolean kimWatching) {
        return kimWatching ? THEFT_CATCH_CHANCE_KIM : THEFT_CATCH_CHANCE_BASE;
    }

    /**
     * Attempt to steal one NAIL_POLISH from the COLOUR_WALL_PROP.
     *
     * @param inventory      the player's inventory
     * @param kimWatching    whether Kim has line-of-sight
     * @param notorietySystem for applying Notoriety penalty on catch (may be null)
     * @param criminalRecord  for recording SHOPLIFTING on catch (may be null)
     * @param achievementCb  for FIVE_FINGER_DISCOUNT_DELUXE on success (may be null)
     * @return {@code true} if the theft succeeded (uncaught); {@code false} if caught
     */
    public boolean attemptNailPolishTheft(Inventory inventory,
                                           boolean kimWatching,
                                           NotorietySystem notorietySystem,
                                           CriminalRecord criminalRecord,
                                           NotorietySystem.AchievementCallback achievementCb) {
        float catchChance = getNailPolishTheftCatchChance(kimWatching);
        boolean caught = random.nextFloat() < catchChance;

        if (caught) {
            // Ban the player for 2 days
            bannedFromSalon = true;
            banDaysRemaining = THEFT_BAN_DAYS;
            // Notoriety penalty
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(THEFT_NOTORIETY_PENALTY, achievementCb);
            }
            // Criminal record
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.SHOPLIFTING);
            }
            return false;
        }

        // Uncaught — add to inventory
        inventory.addItem(Material.NAIL_POLISH, 1);
        if (achievementCb != null) {
            achievementCb.award(AchievementType.FIVE_FINGER_DISCOUNT_DELUXE);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Closing-time envelope
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Spawn the cash envelope on the reception desk (called when in-game time
     * reaches 18:45).
     */
    public void spawnEnvelope() {
        if (!envelopePending) {
            envelopePending = true;
            envelopeTimeRemaining = ENVELOPE_WINDOW_MINUTES;
        }
    }

    /**
     * Steal the cash envelope from the desk.
     *
     * @param inventory      the player's inventory (receives COIN)
     * @param notorietySystem for applying Notoriety penalty (may be null)
     * @param criminalRecord  for recording THEFT (may be null)
     * @param achievementCb  achievement callback (may be null)
     * @return true if the envelope was present and stolen
     */
    public boolean stealEnvelope(Inventory inventory,
                                  NotorietySystem notorietySystem,
                                  CriminalRecord criminalRecord,
                                  NotorietySystem.AchievementCallback achievementCb) {
        if (!envelopePending) return false;
        envelopePending = false;
        envelopeTimeRemaining = 0f;

        inventory.addItem(Material.COIN, ENVELOPE_COIN);

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(ENVELOPE_NOTORIETY_PENALTY, achievementCb);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.THEFT);
        }
        // Permanent ban unless bribed
        bannedFromSalon = true;
        banDaysRemaining = Integer.MAX_VALUE; // permanent until bribed
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WAG Saturday — Stacey Marchetti
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt to receive Marchetti faction intel from Stacey during a WAG Saturday.
     * Stacey appears when Marchetti Respect ≥ {@link #STACEY_MARCHETTI_RESPECT_THRESHOLD}.
     *
     * @param hour            current in-game hour
     * @param dayOfWeek       0=Mon … 5=Sat, 6=Sun
     * @param marchettiRespect player's Marchetti faction Respect
     * @param rumourNetwork   for seeding a Marchetti faction rumour (may be null)
     * @param staceyNpc       the WAG NPC representing Stacey (may be null)
     * @param achievementCb   for STACEY_INTEL achievement (may be null)
     * @return true if Stacey appeared and shared intel
     */
    public boolean triggerStaceyIntel(float hour,
                                       int dayOfWeek,
                                       int marchettiRespect,
                                       RumourNetwork rumourNetwork,
                                       NPC staceyNpc,
                                       NotorietySystem.AchievementCallback achievementCb) {
        if (!isWagSaturdayWindow(hour, dayOfWeek)) return false;
        if (marchettiRespect < STACEY_MARCHETTI_RESPECT_THRESHOLD) return false;

        // Seed a Marchetti faction rumour
        if (rumourNetwork != null && staceyNpc != null) {
            rumourNetwork.addRumour(staceyNpc,
                    new Rumour(RumourType.CONTRABAND_SHIPMENT,
                            "Stacey Marchetti let slip something — something coming through the back. Big."));
        }

        if (achievementCb != null) {
            achievementCb.award(AchievementType.STACEY_INTEL);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client count
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the number of CLIENT NPCs present at any given moment, based on
     * weather and neighbourhood vibes.
     *
     * <p>Base count is 2; rain adds {@link #RAIN_EXTRA_CLIENT_COUNT};
     * low vibes caps at {@link #LOW_VIBES_CLIENT_COUNT}.
     *
     * @param weather        current weather
     * @param neighbourVibes neighbourhood vibes (0–100)
     * @return NPC client count
     */
    public int getClientCount(Weather weather, int neighbourVibes) {
        if (neighbourVibes < BOARD_UP_VIBES_THRESHOLD) {
            return LOW_VIBES_CLIENT_COUNT;
        }
        int base = 2;
        if (weather == Weather.RAIN || weather == Weather.DRIZZLE) {
            base += RAIN_EXTRA_CLIENT_COUNT;
        }
        return base;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-frame update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update timed buffs and state each frame.
     *
     * @param delta      seconds since last frame (real time)
     * @param timeSystem used to convert real seconds to in-game minutes
     */
    public void update(float delta, TimeSystem timeSystem) {
        float inGameMinutesPerRealSecond = timeSystem.getTimeSpeed() * 60f;
        float inGameMinutes = delta * inGameMinutesPerRealSecond;

        // Gel buff countdown
        if (gelBuffRemaining > 0f) {
            gelBuffRemaining -= inGameMinutes;
            if (gelBuffRemaining < 0f) gelBuffRemaining = 0f;
        }

        // Envelope window countdown
        if (envelopePending && envelopeTimeRemaining > 0f) {
            envelopeTimeRemaining -= inGameMinutes;
            if (envelopeTimeRemaining <= 0f) {
                envelopeTimeRemaining = 0f;
                envelopePending = false; // Kim pocketed the envelope
            }
        }

        // Ban countdown (1 in-game day = 24 in-game hours = 1440 in-game minutes)
        if (bannedFromSalon && banDaysRemaining > 0f && banDaysRemaining != Integer.MAX_VALUE) {
            float inGameDays = inGameMinutes / (24f * 60f);
            banDaysRemaining -= inGameDays;
            if (banDaysRemaining <= 0f) {
                banDaysRemaining = 0f;
                bannedFromSalon = false;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State queries
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if the gel CHA buff is currently active. */
    public boolean isGelBuffActive() {
        return gelBuffRemaining > 0f;
    }

    /** Returns remaining in-game minutes for the gel buff. */
    public float getGelBuffRemaining() {
        return gelBuffRemaining;
    }

    /** Returns true if the player has the ACRYLICS_DONE flag. */
    public boolean isAcrylicsDone() {
        return acrylicsDone;
    }

    /** Returns true if a courier drop is available today in the SUPPLY_CABINET_PROP. */
    public boolean hasCourierDrop() {
        return hasCourierDroppedToday;
    }

    /** Returns true if the cash envelope is currently on the reception desk. */
    public boolean isEnvelopePending() {
        return envelopePending;
    }

    /** Returns true if the player is currently banned from the salon. */
    public boolean isBannedFromSalon() {
        return bannedFromSalon;
    }

    /** Returns total salon visits by the player. */
    public int getTotalVisits() {
        return totalVisits;
    }

    /** Returns total gossip exchanges completed. */
    public int getTotalGossipExchanges() {
        return totalGossipExchanges;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Testing helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Directly set the total visit count (for testing). */
    public void setTotalVisitsForTesting(int count) {
        this.totalVisits = count;
    }

    /** Directly set the total gossip exchange count (for testing). */
    public void setTotalGossipExchangesForTesting(int count) {
        this.totalGossipExchanges = count;
    }

    /** Directly set the gel buff remaining in-game minutes (for testing). */
    public void setGelBuffForTesting(float minutes) {
        this.gelBuffRemaining = minutes;
    }

    /** Directly set the courier drop flag (for testing). */
    public void setCourierDropForTesting(boolean hasDrop) {
        this.hasCourierDroppedToday = hasDrop;
    }

    /** Directly set the banned state and duration (for testing). */
    public void setBannedForTesting(boolean banned, float days) {
        this.bannedFromSalon = banned;
        this.banDaysRemaining = days;
    }

    /** Directly set the envelope pending state (for testing). */
    public void setEnvelopePendingForTesting(boolean pending) {
        this.envelopePending = pending;
        if (pending) {
            envelopeTimeRemaining = ENVELOPE_WINDOW_MINUTES;
        }
    }

    /** Directly set the acrylics done flag (for testing). */
    public void setAcrylicsDoneForTesting(boolean done) {
        this.acrylicsDone = done;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1489: Northfield Hospice Sponsored Walk — Brenda's Route, the Pledge Fraud
 * &amp; the Cone Heist.
 *
 * <p>Every year on day 10 (a Sunday), Brenda ({@code NPCType.WALK_ORGANISER}) sets up a
 * 20-waypoint charity sponsored walk around Northfield. She assembles outside the Community
 * Centre at 08:30, hands out {@code Material.SPONSOR_FORM} clipboards, and sends 8–12
 * {@code NPCType.SPONSORED_WALKER} participants around the route, which is marked by 20
 * {@code PropType.ROUTE_CONE_PROP} orange cones at 20-block intervals. The walk runs
 * 09:00–10:30. A {@code PropType.PRIZE_ENVELOPE_PROP} at the finish line contains
 * {@code Material.CHARITY_RAFFLE_TICKET} + 3 COIN for the first finisher.
 *
 * <h3>Schedule</h3>
 * <ul>
 *   <li>08:30 — Brenda spawns at Community Centre; registration opens.</li>
 *   <li>09:00 — Walk starts; player timer begins on crossing waypoint 1.</li>
 *   <li>10:30 — Event closes; SPONSORED_WALKER NPCs despawn.</li>
 * </ul>
 *
 * <h3>Mechanic 1 — Registration &amp; Sponsorship (08:30–09:00)</h3>
 * Player gets {@code SPONSOR_FORM} free from Brenda (press E). Can collect pledges from
 * up to {@code MAX_SPONSORS} = 6 NPCs: PENSIONER 90%/2 COIN, PUBLIC 60%/1 COIN, CHUGGER
 * always refuses. Total pledgeable = {@code MAX_PLEDGE_COIN} = 14 COIN.
 *
 * <h3>Mechanic 2 — The Walk (09:00–10:30)</h3>
 * Player must pass within {@code WAYPOINT_RADIUS} = 3.0f blocks of all 20
 * {@code ROUTE_CONE_PROP} waypoints in order. Walk does NOT cancel in heavy rain.
 * Finish earns pledge payout + {@code WALK_HERO} rumour + {@code WALKED_THE_WALK} achievement.
 *
 * <h3>Mechanic 3 — Cone Theft &amp; Sabotage</h3>
 * Each {@code ROUTE_CONE_PROP} takes 2 hits to drop a {@code TRAFFIC_CONE} (fence value 1 COIN).
 * Remove 5+ cones → walk abandoned, {@code WALK_CANCELLED} rumour seeded (Vibes −3),
 * WantedLevel +1.
 *
 * <h3>Mechanic 4 — Pledge Fraud</h3>
 * Collecting from 3+ sponsors without finishing triggers {@code CHARITY_FRAUD} crime,
 * Notoriety +3, and Brenda's 60-second pursuit. Escaping beyond 40 blocks seeds
 * {@code BRENDA_CONNED} rumour + {@code DODGED_BRENDA} achievement.
 * Collecting pledges without returning = {@code CHARITY_MUGGER} achievement.
 */
public class SponsoredWalkSystem {

    // ── Schedule constants ───────────────────────────────────────────────────

    /** Day-of-cycle on which the first walk event falls. */
    public static final int FIRST_EVENT_DAY = 10;

    /** Number of days between events. */
    public static final int WALK_INTERVAL_DAYS = 28;

    /** Hour at which Brenda spawns and registration opens. */
    public static final float REGISTRATION_OPEN_HOUR = 8.5f;   // 08:30

    /** Hour at which the walk starts. */
    public static final float WALK_START_HOUR = 9.0f;

    /** Hour at which the event closes and walkers despawn. */
    public static final float WALK_END_HOUR = 10.5f;           // 10:30

    // ── Walk mechanics constants ─────────────────────────────────────────────

    /** Maximum number of sponsor NPCs a player can solicit. */
    public static final int MAX_SPONSORS = 6;

    /** Maximum total pledge COIN. */
    public static final int MAX_PLEDGE_COIN = 14;

    /** Number of waypoints (ROUTE_CONE_PROP) on the route. */
    public static final int WAYPOINT_COUNT = 20;

    /** Block radius within which a player counts as having passed a waypoint. */
    public static final float WAYPOINT_RADIUS = 3.0f;

    /** Distance between waypoints (in blocks). */
    public static final int WAYPOINT_INTERVAL_BLOCKS = 20;

    /** Walk speed of SPONSORED_WALKER NPCs (blocks/second). */
    public static final float NPC_WALK_SPEED = 2.5f;

    // ── Cone constants ────────────────────────────────────────────────────────

    /** Hits required to destroy a ROUTE_CONE_PROP. */
    public static final int CONE_HP = 2;

    /** Number of cones that must be removed to abandon the walk. */
    public static final int CONE_ABANDON_THRESHOLD = 5;

    /** Fence value of a TRAFFIC_CONE (in COIN). */
    public static final int CONE_FENCE_VALUE = 1;

    /** Seconds a SPONSORED_WALKER is confused when their next waypoint cone is missing. */
    public static final float WALKER_CONFUSED_SECONDS = 10.0f;

    // ── Prize constants ───────────────────────────────────────────────────────

    /** COIN in the PRIZE_ENVELOPE_PROP awarded to the first finisher. */
    public static final int PRIZE_ENVELOPE_COIN = 3;

    // ── Pursuit / fraud constants ─────────────────────────────────────────────

    /** Block radius beyond which the player has escaped Brenda's pursuit. */
    public static final float BRENDA_PURSUIT_RADIUS = 40.0f;

    /** Seconds Brenda pursues the player after fraud before timeout. */
    public static final float BRENDA_PURSUIT_SECONDS = 60.0f;

    /** Notoriety added when pledge fraud is detected. */
    public static final int PLEDGE_FRAUD_NOTORIETY = 3;

    /** Notoriety added when Brenda's clipboard is pickpocketed. */
    public static final int CLIPBOARD_THEFT_NOTORIETY = 4;

    /** COIN in Brenda's clipboard when pickpocketed. */
    public static final int CLIPBOARD_COIN = 6;

    // ── Pledge probability constants ──────────────────────────────────────────

    /** Probability (0–1) that a PENSIONER pledges. */
    public static final float PENSIONER_PLEDGE_CHANCE = 0.90f;

    /** COIN pledged by a PENSIONER. */
    public static final int PENSIONER_PLEDGE_AMOUNT = 2;

    /** Probability (0–1) that a PUBLIC NPC pledges. */
    public static final float PUBLIC_PLEDGE_CHANCE = 0.60f;

    /** COIN pledged by a PUBLIC NPC. */
    public static final int PUBLIC_PLEDGE_AMOUNT = 1;

    // ── State ────────────────────────────────────────────────────────────────

    private final Random random;

    /** True once registration has opened this cycle. */
    private boolean registrationOpen = false;

    /** True once the walk has started (09:00 gun). */
    private boolean walkStarted = false;

    /** True once the event has been abandoned (5+ cones removed). */
    private boolean walkAbandoned = false;

    /** True if Brenda has been spawned this cycle. */
    private boolean brendaSpawned = false;

    /** True if the player has received their SPONSOR_FORM this cycle. */
    private boolean playerRegistered = false;

    /** Accumulated pledge total on the player's sponsor form. */
    private int pledgeTotal = 0;

    /** Number of NPCs already solicited this event. */
    private int sponsorSolicitations = 0;

    /** Number of cones removed this event. */
    private int conesRemoved = 0;

    /** True if the first-finisher prize has already been claimed. */
    private boolean prizeEnvelopeClaimed = false;

    /** True if Brenda is currently in pursuit mode after fraud. */
    private boolean brendaPursuit = false;

    /** Elapsed seconds of Brenda's pursuit (capped at BRENDA_PURSUIT_SECONDS). */
    private float pursuitTimer = 0f;

    /** True once DODGED_BRENDA has been awarded this cycle (prevent double-award). */
    private boolean dodgedBrendaAwarded = false;

    /** True once CHARITY_MUGGER has been awarded this cycle. */
    private boolean charityMuggerAwarded = false;

    /** True once the walk has been officially completed by the player. */
    private boolean walkCompleted = false;

    /** True once the WALK_HERO rumour has been seeded this cycle. */
    private boolean walkHeroSeeded = false;

    /** True once the WALK_CANCELLED rumour has been seeded this cycle. */
    private boolean walkCancelledSeeded = false;

    /** The day count on which the current event is running. */
    private int currentEventDayCount = -1;

    /** Brenda NPC reference (for state changes and distance checks). */
    private NPC brenda = null;

    /** Managed NPC list (Brenda + spawned walkers). */
    private final List<NPC> eventNpcs = new ArrayList<>();

    // ── Constructor ──────────────────────────────────────────────────────────

    /** Create the system with the default random source. */
    public SponsoredWalkSystem() {
        this(new Random());
    }

    /** Create the system with a seeded random source (for tests). */
    public SponsoredWalkSystem(Random random) {
        this.random = random;
    }

    // ── Public query methods ──────────────────────────────────────────────────

    /**
     * Returns true if today is a walk event day and the event window is open.
     *
     * @param dayCount  in-game day count (1-based)
     * @param hour      current in-game hour
     */
    public boolean isEventDay(int dayCount, float hour) {
        if (dayCount < FIRST_EVENT_DAY) return false;
        int offset = (dayCount - FIRST_EVENT_DAY) % WALK_INTERVAL_DAYS;
        if (offset != 0) return false;
        return hour >= REGISTRATION_OPEN_HOUR && hour <= WALK_END_HOUR;
    }

    /** Returns true if registration is currently open. */
    public boolean isRegistrationOpen() {
        return registrationOpen && !walkStarted && !walkAbandoned;
    }

    /** Returns true if the walk is in progress. */
    public boolean isWalkActive() {
        return walkStarted && !walkAbandoned;
    }

    /** Returns true if the event is currently active (registration or walk in progress). */
    public boolean isActive() {
        return (registrationOpen || walkStarted) && !walkAbandoned;
    }

    /** Returns true if the walk has been abandoned (5+ cones removed). */
    public boolean isAbandoned() {
        return walkAbandoned;
    }

    /** Returns true if the player has received their SPONSOR_FORM. */
    public boolean isPlayerRegistered() {
        return playerRegistered;
    }

    /** Returns the accumulated pledge total on the player's form. */
    public int getPledgeTotal() {
        return pledgeTotal;
    }

    /** Returns the number of NPCs already solicited this event. */
    public int getSponsorSolicitations() {
        return sponsorSolicitations;
    }

    /** Returns the number of cones removed this event. */
    public int getConesRemoved() {
        return conesRemoved;
    }

    /** Returns true if Brenda is currently in pursuit mode. */
    public boolean isBrendaInPursuit() {
        return brendaPursuit;
    }

    /** Returns the list of NPCs managed by this system (Brenda + walkers). */
    public List<NPC> getEventNpcs() {
        return eventNpcs;
    }

    /** Returns Brenda's NPC instance (may be null before spawn). */
    public NPC getBrenda() {
        return brenda;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Call every game tick.
     *
     * @param delta              seconds elapsed since last frame
     * @param timeSystem         current time/date source
     * @param npcs               global NPC list (for spawning / despawning)
     * @param rumourNetwork      rumour network (may be null in tests)
     * @param notorietySystem    notoriety system (may be null in tests)
     * @param wantedSystem       wanted system (may be null in tests)
     * @param neighbourhoodSystem neighbourhood system (may be null in tests)
     * @param playerX            player world X position (for pursuit check)
     * @param playerZ            player world Z position (for pursuit check)
     * @param achievement        achievement callback (may be null in tests)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       List<NPC> npcs,
                       RumourNetwork rumourNetwork,
                       NotorietySystem notorietySystem,
                       WantedSystem wantedSystem,
                       NeighbourhoodSystem neighbourhoodSystem,
                       float playerX,
                       float playerZ,
                       AchievementCallback achievement) {

        int dayCount = timeSystem.getDayCount();
        float hour = timeSystem.getTime();

        boolean onEventDay = isOnEventCycle(dayCount, hour);

        if (!onEventDay) {
            if (currentEventDayCount >= 0 && currentEventDayCount != dayCount) {
                resetForNewCycle(npcs);
            }
            return;
        }

        if (currentEventDayCount != dayCount) {
            currentEventDayCount = dayCount;
        }

        // ── Registration opens at 08:30 ───────────────────────────────────────
        if (!registrationOpen && hour >= REGISTRATION_OPEN_HOUR) {
            registrationOpen = true;
            spawnBrenda(npcs);
        }

        // ── Walk starts at 09:00 ───────────────────────────────────────────────
        if (registrationOpen && !walkStarted && !walkAbandoned && hour >= WALK_START_HOUR) {
            walkStarted = true;
        }

        // ── Pursuit timer (Brenda chasing player after fraud) ────────────────
        if (brendaPursuit && !dodgedBrendaAwarded) {
            pursuitTimer += delta;

            // Check if player has escaped (distance > BRENDA_PURSUIT_RADIUS)
            if (brenda != null) {
                float dx = playerX - brenda.getPosition().x;
                float dz = playerZ - brenda.getPosition().z;
                float distSq = dx * dx + dz * dz;
                if (distSq > BRENDA_PURSUIT_RADIUS * BRENDA_PURSUIT_RADIUS) {
                    // Player escaped
                    brendaPursuit = false;
                    dodgedBrendaAwarded = true;
                    if (rumourNetwork != null) {
                        seedRumour(rumourNetwork, npcs, RumourType.BRENDA_CONNED,
                                "Someone did Brenda from the Hospice Walk out of her pledges. Proper wrong that.");
                    }
                    if (achievement != null) {
                        achievement.award(AchievementType.DODGED_BRENDA);
                    }
                }
            }

            // Pursuit timeout
            if (pursuitTimer >= BRENDA_PURSUIT_SECONDS && !dodgedBrendaAwarded) {
                brendaPursuit = false;
            }
        }

        // ── CHARITY_MUGGER check: sponsors collected, walk never completed ────
        if (!charityMuggerAwarded && sponsorSolicitations >= 3 && pledgeTotal > 0
                && !walkCompleted && walkAbandoned) {
            charityMuggerAwarded = true;
            if (achievement != null) {
                achievement.award(AchievementType.CHARITY_MUGGER);
            }
        }

        // ── Event closes at 10:30 ─────────────────────────────────────────────
        if (walkStarted && hour >= WALK_END_HOUR) {
            closeEvent(npcs);
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Player presses E on Brenda to receive a SPONSOR_FORM (free).
     *
     * @param inventory player inventory
     * @return {@link RegistrationResult} indicating outcome
     */
    public RegistrationResult register(Inventory inventory) {
        if (!registrationOpen || walkStarted || walkAbandoned) {
            return RegistrationResult.NOT_OPEN;
        }
        if (playerRegistered) {
            return RegistrationResult.ALREADY_REGISTERED;
        }

        inventory.addItem(Material.SPONSOR_FORM, 1);
        playerRegistered = true;
        return RegistrationResult.SUCCESS;
    }

    /** Result of a registration attempt. */
    public enum RegistrationResult {
        /** Registration successful. SPONSOR_FORM added to inventory. */
        SUCCESS,
        /** Registration not open (wrong time or walk abandoned). */
        NOT_OPEN,
        /** Player already registered. */
        ALREADY_REGISTERED
    }

    // ── Sponsor solicitation ──────────────────────────────────────────────────

    /**
     * Approach an NPC with the sponsor form to collect a pledge.
     *
     * @param npc       the NPC being approached
     * @param inventory player inventory (must contain SPONSOR_FORM)
     * @return {@link PledgeResult} indicating outcome
     */
    public PledgeResult solicitPledge(NPC npc, Inventory inventory) {
        if (!playerRegistered) {
            return PledgeResult.NOT_REGISTERED;
        }
        if (!inventory.hasItem(Material.SPONSOR_FORM)) {
            return PledgeResult.NO_FORM;
        }
        if (sponsorSolicitations >= MAX_SPONSORS) {
            return PledgeResult.MAX_REACHED;
        }
        if (pledgeTotal >= MAX_PLEDGE_COIN) {
            return PledgeResult.MAX_COIN_REACHED;
        }

        NPCType type = npc.getType();

        if (type == NPCType.CHUGGER) {
            return PledgeResult.REFUSED;
        }

        float roll = random.nextFloat();
        int pledge = 0;

        if (type == NPCType.PENSIONER) {
            if (roll < PENSIONER_PLEDGE_CHANCE) {
                pledge = PENSIONER_PLEDGE_AMOUNT;
            }
        } else if (type == NPCType.PUBLIC) {
            if (roll < PUBLIC_PLEDGE_CHANCE) {
                pledge = PUBLIC_PLEDGE_AMOUNT;
            }
        } else {
            // Other NPC types: 60% / 1 COIN (same as PUBLIC)
            if (roll < PUBLIC_PLEDGE_CHANCE) {
                pledge = PUBLIC_PLEDGE_AMOUNT;
            }
        }

        sponsorSolicitations++;

        if (pledge <= 0) {
            return PledgeResult.DECLINED;
        }

        pledgeTotal = Math.min(pledgeTotal + pledge, MAX_PLEDGE_COIN);
        return PledgeResult.SUCCESS;
    }

    /** Result of a sponsor solicitation attempt. */
    public enum PledgeResult {
        /** Pledge recorded successfully. */
        SUCCESS,
        /** NPC declined (probability roll failed). */
        DECLINED,
        /** NPC refuses (CHUGGER). */
        REFUSED,
        /** Player is not registered. */
        NOT_REGISTERED,
        /** Player does not have SPONSOR_FORM. */
        NO_FORM,
        /** Maximum NPC solicitations reached. */
        MAX_REACHED,
        /** Maximum pledge coin reached. */
        MAX_COIN_REACHED
    }

    // ── Walk completion ───────────────────────────────────────────────────────

    /**
     * Called when the player returns to Brenda after completing all 20 waypoints.
     *
     * <p>Seeds {@code WALK_HERO} rumour, pays out pledge total as COIN,
     * awards first-finisher prize if not yet claimed, and grants
     * {@code WALKED_THE_WALK} achievement.
     *
     * @param inventory          player inventory
     * @param rumourNetwork      rumour network (may be null)
     * @param notorietySystem    notoriety system (may be null)
     * @param neighbourhoodSystem neighbourhood system for Vibes (may be null)
     * @param npcs               global NPC list (for rumour seeding)
     * @param achievement        achievement callback (may be null)
     * @return {@link WalkFinishResult} indicating outcome
     */
    public WalkFinishResult finishWalk(Inventory inventory,
                                       RumourNetwork rumourNetwork,
                                       NotorietySystem notorietySystem,
                                       NeighbourhoodSystem neighbourhoodSystem,
                                       List<NPC> npcs,
                                       AchievementCallback achievement) {
        if (!playerRegistered) {
            return WalkFinishResult.NOT_REGISTERED;
        }
        if (walkAbandoned) {
            return WalkFinishResult.WALK_ABANDONED;
        }

        walkCompleted = true;

        // Pay out pledge total
        if (pledgeTotal > 0) {
            inventory.addItem(Material.COIN, pledgeTotal);
        }

        // First finisher prize
        boolean firstFinisher = !prizeEnvelopeClaimed;
        if (firstFinisher) {
            prizeEnvelopeClaimed = true;
            inventory.addItem(Material.CHARITY_RAFFLE_TICKET, 1);
            inventory.addItem(Material.COIN, PRIZE_ENVELOPE_COIN);
        }

        // Seed WALK_HERO rumour and add Vibes +2
        if (!walkHeroSeeded) {
            walkHeroSeeded = true;
            if (rumourNetwork != null && npcs != null) {
                seedRumour(rumourNetwork, npcs, RumourType.WALK_HERO,
                        "Saw someone actually finish the whole sponsored walk. Fair play.");
            }
            if (neighbourhoodSystem != null) {
                neighbourhoodSystem.addVibes(2);
            }
        }

        // Award WALKED_THE_WALK achievement
        if (achievement != null) {
            achievement.award(AchievementType.WALKED_THE_WALK);
        }

        return firstFinisher ? WalkFinishResult.FIRST_FINISHER : WalkFinishResult.FINISHED;
    }

    /** Result of completing the walk. */
    public enum WalkFinishResult {
        /** Finished first — raffle ticket + bonus coin + pledges awarded. */
        FIRST_FINISHER,
        /** Finished but not first — pledges awarded. */
        FINISHED,
        /** Player not registered. */
        NOT_REGISTERED,
        /** Walk was abandoned (cones removed). */
        WALK_ABANDONED
    }

    // ── Pledge collection (fraud path) ────────────────────────────────────────

    /**
     * Called when the player attempts to collect pledge money from Brenda after the walk.
     *
     * <p>If {@code walkCompleted} is true: pays out pledges legitimately.
     * If {@code walkCompleted} is false and 3+ sponsors were collected:
     * records {@code CHARITY_FRAUD}, adds Notoriety +3, puts Brenda into pursuit.
     *
     * @param inventory          player inventory
     * @param walkCompleted      whether the player legitimately finished all waypoints
     * @param criminalRecord     criminal record
     * @param notorietySystem    notoriety system
     * @param wantedSystem       wanted system (may be null)
     * @param rumourNetwork      rumour network (may be null)
     * @param npcs               global NPC list
     * @param achievement        achievement callback (may be null)
     * @return {@link CollectPledgesResult} indicating outcome
     */
    public CollectPledgesResult collectPledges(Inventory inventory,
                                               boolean walkCompleted,
                                               CriminalRecord criminalRecord,
                                               NotorietySystem notorietySystem,
                                               WantedSystem wantedSystem,
                                               RumourNetwork rumourNetwork,
                                               List<NPC> npcs,
                                               AchievementCallback achievement) {
        if (!inventory.hasItem(Material.SPONSOR_FORM)) {
            return CollectPledgesResult.NO_FORM;
        }

        if (!walkCompleted && sponsorSolicitations >= 3) {
            // Fraud detected
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.CHARITY_FRAUD);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(PLEDGE_FRAUD_NOTORIETY, null);
            }
            // Brenda enters pursuit mode
            if (brenda != null) {
                brenda.setState(NPCState.ANGRY);
            }
            brendaPursuit = true;
            pursuitTimer = 0f;

            return CollectPledgesResult.FRAUD_DETECTED;
        }

        if (walkCompleted && pledgeTotal > 0) {
            inventory.addItem(Material.COIN, pledgeTotal);
        }
        inventory.removeItem(Material.SPONSOR_FORM, 1);
        return CollectPledgesResult.SUCCESS;
    }

    /** Result of a pledge collection attempt. */
    public enum CollectPledgesResult {
        /** Pledges paid out successfully. */
        SUCCESS,
        /** Fraud detected (collected 3+ pledges without finishing). */
        FRAUD_DETECTED,
        /** Player does not have SPONSOR_FORM. */
        NO_FORM
    }

    // ── Cone removal ──────────────────────────────────────────────────────────

    /**
     * Player punches a ROUTE_CONE_PROP twice to remove it and gain a TRAFFIC_CONE.
     *
     * <p>If {@code CONE_ABANDON_THRESHOLD} or more cones have been removed,
     * the walk is officially abandoned: {@code WALK_CANCELLED} rumour is seeded,
     * Vibes −3, WantedLevel +1.
     *
     * @param inventory           player inventory (receives TRAFFIC_CONE)
     * @param rumourNetwork       rumour network (may be null)
     * @param wantedSystem        wanted system (may be null)
     * @param neighbourhoodSystem neighbourhood system for Vibes (may be null)
     * @param npcs                global NPC list (for rumour seeding)
     * @return {@link ConeRemovalResult} indicating outcome
     */
    public ConeRemovalResult removeCone(Inventory inventory,
                                        RumourNetwork rumourNetwork,
                                        WantedSystem wantedSystem,
                                        NeighbourhoodSystem neighbourhoodSystem,
                                        List<NPC> npcs) {
        if (walkAbandoned) {
            return ConeRemovalResult.WALK_ALREADY_ABANDONED;
        }

        conesRemoved++;
        inventory.addItem(Material.TRAFFIC_CONE, 1);

        if (conesRemoved >= CONE_ABANDON_THRESHOLD) {
            walkAbandoned = true;
            walkStarted = false;
            registrationOpen = false;

            // Seed WALK_CANCELLED rumour
            if (!walkCancelledSeeded) {
                walkCancelledSeeded = true;
                if (rumourNetwork != null && npcs != null) {
                    seedRumour(rumourNetwork, npcs, RumourType.WALK_CANCELLED,
                            "Sponsored walk got abandoned — someone nicked all the cones.");
                }
            }

            // Vibes −3
            if (neighbourhoodSystem != null) {
                neighbourhoodSystem.addVibes(-3);
            }

            // WantedLevel +1
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
            }

            return ConeRemovalResult.WALK_ABANDONED;
        }

        return ConeRemovalResult.CONE_REMOVED;
    }

    /** Result of removing a ROUTE_CONE_PROP. */
    public enum ConeRemovalResult {
        /** Cone removed successfully; TRAFFIC_CONE added to inventory. */
        CONE_REMOVED,
        /** 5+ cones removed — walk is now officially abandoned. */
        WALK_ABANDONED,
        /** Walk was already abandoned before this cone was removed. */
        WALK_ALREADY_ABANDONED
    }

    // ── Clipboard pickpocket ──────────────────────────────────────────────────

    /**
     * Player pickpockets Brenda's clipboard (press E behind her).
     *
     * <p>Yields {@code SPONSOR_FORM} + {@code CLIPBOARD_COIN} = 6 COIN.
     * Records {@code THEFT_FROM_PERSON}, adds Notoriety +4, and if spotted
     * calls police (WantedLevel +1).
     *
     * @param inventory       player inventory
     * @param criminalRecord  criminal record
     * @param notorietySystem notoriety system
     * @param wantedSystem    wanted system (may be null)
     * @param witnessed       true if Brenda or a witness spotted the theft
     * @return {@link PickpocketResult}
     */
    public PickpocketResult pickpocketClipboard(Inventory inventory,
                                                CriminalRecord criminalRecord,
                                                NotorietySystem notorietySystem,
                                                WantedSystem wantedSystem,
                                                boolean witnessed) {
        inventory.addItem(Material.SPONSOR_FORM, 1);
        inventory.addItem(Material.COIN, CLIPBOARD_COIN);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.THEFT_FROM_PERSON);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CLIPBOARD_THEFT_NOTORIETY, null);
        }
        if (witnessed && wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
            if (brenda != null) {
                brenda.setState(NPCState.ANGRY);
            }
        }

        return PickpocketResult.SUCCESS;
    }

    /** Result of a pickpocket attempt on Brenda. */
    public enum PickpocketResult {
        /** Pickpocket succeeded. */
        SUCCESS,
        /** Caught in the act. */
        CAUGHT
    }

    // ── Waypoint check ────────────────────────────────────────────────────────

    /**
     * Check whether the player has passed the next expected waypoint.
     *
     * @param waypointsCompleted number of waypoints already completed (0-based)
     * @return {@link WaypointStatus}
     */
    public WaypointStatus checkWaypointCompletion(int waypointsCompleted) {
        if (waypointsCompleted >= WAYPOINT_COUNT) {
            return WaypointStatus.ALL_COMPLETE;
        }
        return WaypointStatus.IN_PROGRESS;
    }

    /** Status of the player's waypoint progress. */
    public enum WaypointStatus {
        /** All 20 waypoints completed. */
        ALL_COMPLETE,
        /** Some waypoints remain. */
        IN_PROGRESS
    }

    // ── Achievement callback interface ────────────────────────────────────────

    /** Callback interface for awarding achievements, matching the project convention. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isOnEventCycle(int dayCount, float hour) {
        if (dayCount < FIRST_EVENT_DAY) return false;
        int offset = (dayCount - FIRST_EVENT_DAY) % WALK_INTERVAL_DAYS;
        if (offset != 0) return false;
        return hour >= REGISTRATION_OPEN_HOUR && hour <= WALK_END_HOUR;
    }

    private void spawnBrenda(List<NPC> npcs) {
        brenda = new NPC(NPCType.WALK_ORGANISER, 0f, 0f, 0f);
        eventNpcs.add(brenda);
        if (npcs != null) npcs.add(brenda);
        brendaSpawned = true;
    }

    private void seedRumour(RumourNetwork rumourNetwork, List<NPC> npcs, RumourType type, String text) {
        NPC seed = null;
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.PENSIONER) {
                    seed = npc;
                    break;
                }
            }
            if (seed == null && !npcs.isEmpty()) {
                seed = npcs.get(0);
            }
        }
        if (seed == null && brenda != null) {
            seed = brenda;
        }
        if (seed != null) {
            rumourNetwork.addRumour(seed, new Rumour(type, text));
        }
    }

    private void closeEvent(List<NPC> npcs) {
        if (npcs != null) {
            npcs.removeAll(eventNpcs);
        }
        eventNpcs.clear();
        brenda = null;
        walkStarted = false;
        registrationOpen = false;
    }

    private void resetForNewCycle(List<NPC> npcs) {
        if (npcs != null) {
            npcs.removeAll(eventNpcs);
        }
        eventNpcs.clear();
        brenda = null;
        registrationOpen = false;
        walkStarted = false;
        walkAbandoned = false;
        brendaSpawned = false;
        playerRegistered = false;
        pledgeTotal = 0;
        sponsorSolicitations = 0;
        conesRemoved = 0;
        prizeEnvelopeClaimed = false;
        brendaPursuit = false;
        pursuitTimer = 0f;
        dodgedBrendaAwarded = false;
        charityMuggerAwarded = false;
        walkCompleted = false;
        walkHeroSeeded = false;
        walkCancelledSeeded = false;
        currentEventDayCount = -1;
    }
}

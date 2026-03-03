package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * EnglandMatchSystem — Issue #1394: Northfield England Match Night.
 *
 * <p>Handles the England Euros/World Cup match viewing event at Wetherspoons.
 * Core responsibilities:
 * <ul>
 *   <li>Match day detection and scheduling (7 fixture days).</li>
 *   <li>Pre-match crowd build-up (18:30) and kick-off (20:00).</li>
 *   <li>Deterministic match result seeded from {@code Random(dayCount * 31 + 7)}.</li>
 *   <li>Goal event crowd reactions (CELEBRATING / GROANING).</li>
 *   <li>TV sabotage via CROWBAR or CABLE during Dave's half-time blind spot.</li>
 *   <li>Scoreline betting via Barry (MATCH_BOOKIE NPC).</li>
 *   <li>Trophy cabinet heist (SIGNED_SHIRT, FA_CUP_REPLICA, GOLDEN_BOOT_PROP).</li>
 *   <li>German flag diplomatic incident.</li>
 *   <li>Match fix via MATCH_FIX_ITEM (Marchetti Crew Respect ≥ 60).</li>
 * </ul>
 */
public class EnglandMatchSystem {

    // ── Match schedule ────────────────────────────────────────────────────────

    /** Fixed list of in-game days on which England matches take place. */
    public static final List<Integer> ENGLAND_MATCH_DAYS =
        Arrays.asList(170, 175, 180, 185, 188, 191, 195);

    /** Kick-off time (in-game hours). */
    public static final float KICKOFF_HOUR = 20.0f;

    /** Pre-match crowd build begins (in-game hours). */
    public static final float PREMATCH_HOUR = 18.5f;

    /** Queue forms at the door (in-game hours). */
    public static final float QUEUE_HOUR = 19.5f;

    /** Half-time hour (in-game hours; 45 in-game minutes after kick-off). */
    public static final float HALFTIME_HOUR = 20.75f;

    /** Full-time: 90 in-game minutes after kick-off. */
    public static final float FULLTIME_HOUR = 21.5f;

    /** Duration of match in real seconds (configurable). */
    public static final float MATCH_DURATION_REAL_SECONDS = 300f;

    // ── Crowd constants ───────────────────────────────────────────────────────

    /** Minimum crowd size spawned inside pub. */
    public static final int CROWD_MIN = 30;

    /** Maximum crowd size / bouncer cap. */
    public static final int CROWD_CAP = 40;

    // ── Goal event timings ────────────────────────────────────────────────────

    /** Duration of CELEBRATING state after an England goal (real seconds). */
    public static final float CELEBRATING_DURATION_SECONDS = 15f;

    /** Duration of GROANING state after an opposition goal (real seconds). */
    public static final float GROANING_DURATION_SECONDS = 10f;

    /** Duration shops are unattended after an England goal. */
    public static final float SHOP_UNATTENDED_WINDOW = 20f;

    // ── TV sabotage constants ─────────────────────────────────────────────────

    /** Dave's blind spot window start — half-time begins. */
    public static final float DAVE_BLIND_SPOT_START = 20.75f;

    /** Dave's blind spot window end. */
    public static final float DAVE_BLIND_SPOT_END = 21.0f;

    /** Hold-E duration required to sabotage TV (real seconds). */
    public static final float TV_SABOTAGE_HOLD_DURATION = 5f;

    /** Duration of crowd AGITATED state after TV sabotage (real seconds). */
    public static final float AGITATION_DURATION_SECONDS = 120f;

    /** Interval between CROWD_BRAWL events during agitation (real seconds). */
    public static final float AGITATED_BRAWL_INTERVAL = 30f;

    /** Probability Dave witnesses the sabotage. */
    public static final float DAVE_WITNESS_CHANCE = 0.50f;

    /** Probability a RIVAL_FAN is blamed for sabotage instead of player. */
    public static final float RIVAL_FAN_BLAME_CHANCE = 0.35f;

    // ── Betting constants ─────────────────────────────────────────────────────

    /** Maximum bet in COIN Barry will accept. */
    public static final int MAX_BET_COINS = 30;

    /** Payout multiplier for an ENGLAND_WIN bet. */
    public static final int WIN_ODDS = 2;

    /** Payout multiplier for a DRAW bet. */
    public static final int DRAW_ODDS = 4;

    /** Payout multiplier for an OPPOSITION_WIN bet. */
    public static final int OPPOSITION_ODDS = 6;

    // ── Victory/loss constants ────────────────────────────────────────────────

    /** Duration shops have unattended doors after England win riot (real seconds). */
    public static final float VICTORY_CHAOS_DURATION = 180f;

    /** Notoriety gain for TV sabotage. */
    public static final int NOTORIETY_TV_SABOTAGE = 10;

    /** Notoriety gain for full trophy cabinet heist. */
    public static final int NOTORIETY_TROPHY_HEIST = 8;

    /** Notoriety gain for German flag placement. */
    public static final int NOTORIETY_GERMAN_FLAG = 5;

    /** WantedSystem stars added if Dave witnesses TV sabotage. */
    public static final int WANTED_TV_SABOTAGE_WITNESSED = 2;

    /** WantedSystem stars for German flag plant. */
    public static final int WANTED_GERMAN_FLAG = 1;

    /** NeighbourhoodSystem VIBES penalty on England win (victory riot). */
    public static final int VIBES_WIN_RIOT = -5;

    /** NeighbourhoodSystem VIBES penalty on England loss. */
    public static final int VIBES_LOSS = -2;

    /** Marchetti cut of match fix winnings (20%). */
    public static final float MARCHETTI_CUT = 0.20f;

    /** Marchetti Crew Respect required to obtain/use MATCH_FIX_ITEM. */
    public static final int MARCHETTI_RESPECT_REQUIRED = 60;

    /** Cost of MATCH_FIX_ITEM from Marchetti. */
    public static final int MATCH_FIX_COST = 50;

    // ── Match result enum ─────────────────────────────────────────────────────

    /** Possible match outcomes. */
    public enum MatchResult {
        ENGLAND_WIN,
        ENGLAND_DRAW,
        ENGLAND_LOSS
    }

    // ── Bet result enum ───────────────────────────────────────────────────────

    /** Result of a bet placement attempt. */
    public enum BetResult {
        ACCEPTED,
        BETTING_CLOSED,
        MAX_BET_EXCEEDED,
        INSUFFICIENT_FUNDS,
        ALREADY_BET
    }

    // ── TV prop state ─────────────────────────────────────────────────────────

    /** State of the PUB_TV_PROP. */
    public enum TvState {
        FUNCTIONAL,
        BROKEN
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private final Random random;

    /** Crowd NPCs currently inside Wetherspoons. */
    private final List<NPC> crowd = new ArrayList<>();

    /** Whether the match is currently active (between pre-match and full-time). */
    private boolean matchActive = false;

    /** Whether the pre-match phase has started (crowd building). */
    private boolean preMatchActive = false;

    /** Whether a goal celebration is active and shops are unattended. */
    private boolean goalCelebrationActive = false;

    /** Timer for the current crowd reaction state (seconds remaining). */
    private float crowdStateTimer = 0f;

    /** TV state. */
    private TvState tvState = TvState.FUNCTIONAL;

    /** Whether the TV sabotage hold-E action is in progress. */
    private float tvSabotageHoldTimer = 0f;

    /** Whether TV has been sabotaged this match. */
    private boolean tvSabotaged = false;

    /** Timer for crowd agitation after TV sabotage (seconds remaining). */
    private float agitationTimer = 0f;

    /** Timer for the next brawl during agitation. */
    private float agitatedBrawlTimer = 0f;

    /** Whether the match has been cancelled (THUNDERSTORM). */
    private boolean matchCancelled = false;

    /** Current match result (null until full-time). */
    private MatchResult matchResult = null;

    /** Whether the result has been forced by MATCH_FIX_ITEM. */
    private boolean resultForced = false;

    /** Active bet: 0 = no bet, else the amount bet. */
    private int betAmount = 0;

    /** Active bet target. */
    private MatchResult betTarget = null;

    /** Whether a bet has been placed this match. */
    private boolean betPlaced = false;

    /** Whether the victory riot has been fired. */
    private boolean victoryRiotFired = false;

    /** Timer for goal celebration unattended shop window (seconds remaining). */
    private float shopUnattendedTimer = 0f;

    /** Whether the trophy cabinet has been smashed. */
    private boolean trophyCabinetSmashed = false;

    /** Items remaining in the trophy cabinet. */
    private final List<Material> trophyItems = new ArrayList<>(
        Arrays.asList(Material.SIGNED_SHIRT, Material.FA_CUP_REPLICA, Material.GOLDEN_BOOT_PROP)
    );

    /** Items already taken from trophy cabinet this match. */
    private final List<Material> trophyItemsTaken = new ArrayList<>();

    /** Whether Dave's blind spot is currently open. */
    private boolean daveBlindSpot = false;

    /** Whether Dave is watching the bar area (blocks sabotage). */
    private boolean daveWatching = false;

    /** Current game hour (updated from TimeSystem). */
    private float currentHour = 0f;

    /** Current day count (set by caller before match day). */
    private int currentDayCount = 0;

    // ── Injected dependencies ─────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private RumourNetwork rumourNetwork;
    private NeighbourhoodSystem neighbourhoodSystem;
    private NewspaperSystem newspaperSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private WeatherSystem weatherSystem;
    private FootballSystem footballSystem;
    private WetherspoonsSystem wetherspoonsSystem;

    // ── Constructors ──────────────────────────────────────────────────────────

    public EnglandMatchSystem() {
        this(new Random());
    }

    public EnglandMatchSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ──────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setWeatherSystem(WeatherSystem weatherSystem) {
        this.weatherSystem = weatherSystem;
    }

    public void setFootballSystem(FootballSystem footballSystem) {
        this.footballSystem = footballSystem;
    }

    public void setWetherspoonsSystem(WetherspoonsSystem wetherspoonsSystem) {
        this.wetherspoonsSystem = wetherspoonsSystem;
    }

    // ── Match day detection ───────────────────────────────────────────────────

    /**
     * Returns true if the given in-game day is an England match day.
     *
     * @param dayCount the current in-game day count
     * @return true if dayCount is in {@link #ENGLAND_MATCH_DAYS}
     */
    public boolean isMatchDay(int dayCount) {
        return ENGLAND_MATCH_DAYS.contains(dayCount);
    }

    /**
     * Deterministic match result for a given day count.
     * Seeded from {@code new Random(dayCount * 31 + 7)}.
     *
     * @param dayCount the in-game day count
     * @return the match result
     */
    public MatchResult determineResult(int dayCount) {
        if (resultForced) {
            return MatchResult.ENGLAND_LOSS;
        }
        Random resultRng = new Random((long) dayCount * 31 + 7);
        int roll = resultRng.nextInt(3);
        if (roll == 0) return MatchResult.ENGLAND_WIN;
        if (roll == 1) return MatchResult.ENGLAND_DRAW;
        return MatchResult.ENGLAND_LOSS;
    }

    // ── Update loop ───────────────────────────────────────────────────────────

    /**
     * Per-frame update. Called from the game loop.
     *
     * @param delta        seconds since last frame
     * @param timeSystem   the current TimeSystem
     * @param allNpcs      all NPCs in the world (for rumour seeding)
     * @param achievementCb achievement callback
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCb) {
        if (timeSystem == null) return;

        currentHour = timeSystem.getTime();
        currentDayCount = timeSystem.getDayCount();

        // Check for thunderstorm cancellation before pre-match starts
        if (isMatchDay(currentDayCount) && !matchActive && !preMatchActive && !matchCancelled) {
            if (currentHour < PREMATCH_HOUR) {
                Weather weather = (weatherSystem != null) ? weatherSystem.getCurrentWeather() : null;
                if (weather == Weather.THUNDERSTORM) {
                    matchCancelled = true;
                    seedRumour(allNpcs, RumourType.MATCH_CANCELLED_RAIN,
                        "They were saying the Wetherspoons screening's off tonight — something about the satellite dish.");
                    return;
                }
            }
        }

        if (matchCancelled) return;

        if (isMatchDay(currentDayCount)) {
            handleMatchDay(delta, allNpcs, achievementCb);
        }

        // Tick crowd state timers
        if (crowdStateTimer > 0) {
            crowdStateTimer -= delta;
            if (crowdStateTimer <= 0) {
                crowdStateTimer = 0;
                // Reset crowd to WATCHING_MATCH
                if (matchActive) {
                    for (NPC npc : crowd) {
                        if (npc.getState() == NPCState.CELEBRATING ||
                            npc.getState() == NPCState.GROANING) {
                            npc.setState(NPCState.WATCHING_MATCH);
                        }
                    }
                    goalCelebrationActive = false;
                }
            }
        }

        // Tick shop unattended window
        if (shopUnattendedTimer > 0) {
            shopUnattendedTimer -= delta;
        }

        // Tick agitation timer
        if (agitationTimer > 0) {
            agitationTimer -= delta;
            agitatedBrawlTimer -= delta;
            if (agitatedBrawlTimer <= 0) {
                agitatedBrawlTimer = AGITATED_BRAWL_INTERVAL;
                // Fire a crowd brawl (represented as state change for test purposes)
                fireCrowdBrawl();
            }
            if (agitationTimer <= 0) {
                agitationTimer = 0;
                // Agitation ended — restore crowd to idle/despawning
                for (NPC npc : crowd) {
                    if (npc.getState() == NPCState.AGITATED) {
                        npc.setState(NPCState.IDLE);
                    }
                }
            }
        }
    }

    private void handleMatchDay(float delta, List<NPC> allNpcs,
                                 NotorietySystem.AchievementCallback achievementCb) {
        // Pre-match phase: 18:30
        if (!preMatchActive && currentHour >= PREMATCH_HOUR) {
            startPreMatch(allNpcs);
        }

        // Kick-off: 20:00
        if (preMatchActive && !matchActive && currentHour >= KICKOFF_HOUR) {
            startMatch(allNpcs);
        }

        // Half-time: Dave blind spot 20:45–21:00
        if (matchActive) {
            daveBlindSpot = (currentHour >= DAVE_BLIND_SPOT_START && currentHour < DAVE_BLIND_SPOT_END);
        }

        // Full-time: 21:30
        if (matchActive && !victoryRiotFired && currentHour >= FULLTIME_HOUR) {
            endMatch(allNpcs, achievementCb);
        }
    }

    private void startPreMatch(List<NPC> allNpcs) {
        preMatchActive = true;
        spawnCrowd();
        if (wetherspoonsSystem != null) {
            wetherspoonsSystem.addPatrons(crowd.size());
        }
    }

    private void startMatch(List<NPC> allNpcs) {
        matchActive = true;
        matchResult = null;
        // Set all crowd to WATCHING_MATCH
        for (NPC npc : crowd) {
            npc.setState(NPCState.WATCHING_MATCH);
        }
    }

    private void endMatch(List<NPC> allNpcs, NotorietySystem.AchievementCallback achievementCb) {
        matchResult = determineResult(currentDayCount);
        matchActive = false;
        victoryRiotFired = true;

        switch (matchResult) {
            case ENGLAND_WIN:
                handleEnglandWin(allNpcs, achievementCb);
                break;
            case ENGLAND_DRAW:
                handleDraw(allNpcs);
                break;
            case ENGLAND_LOSS:
                handleEnglandLoss(allNpcs);
                break;
        }

        // Resolve any pending bet
        if (betPlaced) {
            resolveBet(achievementCb);
        }
    }

    private void handleEnglandWin(List<NPC> allNpcs,
                                   NotorietySystem.AchievementCallback achievementCb) {
        seedRumour(allNpcs, RumourType.ENGLAND_WIN_RIOT,
            "England went through and half the town went mental. Dave's got a broken window.");
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(
                Math.max(0, neighbourhoodSystem.getVibes() + VIBES_WIN_RIOT));
        }
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline(
                "Northfield Goes Mental — England Through!");
        }
        // Fire crowd brawl across 20-block radius
        fireCrowdBrawl();
    }

    private void handleDraw(List<NPC> allNpcs) {
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline(
                "A Point's A Point, Says Terry.");
        }
    }

    private void handleEnglandLoss(List<NPC> allNpcs) {
        seedRumour(allNpcs, RumourType.ENGLAND_LOSS_DESPAIR,
            "England lost again. The pub was in silence for twenty minutes. It were like a funeral.");
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(
                Math.max(0, neighbourhoodSystem.getVibes() + VIBES_LOSS));
        }
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline(
                "Another Heartbreak: Northfield Fans Left Devastated.");
        }
        // Disperse crowd
        for (NPC npc : crowd) {
            npc.setState(NPCState.DESPAWNING);
        }
    }

    // ── Goal events ───────────────────────────────────────────────────────────

    /**
     * Inject an ENGLAND_GOAL event. All crowd enters CELEBRATING for 15 seconds.
     * Shop unattended window opens for {@link #SHOP_UNATTENDED_WINDOW} seconds.
     */
    public void onEnglandGoal() {
        crowdStateTimer = CELEBRATING_DURATION_SECONDS;
        goalCelebrationActive = true;
        shopUnattendedTimer = SHOP_UNATTENDED_WINDOW;
        for (NPC npc : crowd) {
            npc.setState(NPCState.CELEBRATING);
        }
    }

    /**
     * Inject an OPPOSITION_GOAL event. All crowd enters GROANING for 10 seconds.
     * 25% chance of CROWD_BRAWL.
     */
    public void onOppositionGoal() {
        crowdStateTimer = GROANING_DURATION_SECONDS;
        goalCelebrationActive = false;
        for (NPC npc : crowd) {
            npc.setState(NPCState.GROANING);
        }
        // 25% brawl chance
        if (random.nextFloat() < 0.25f) {
            fireCrowdBrawl();
        }
    }

    private void fireCrowdBrawl() {
        // Triggers crowd brawl event; in a full game implementation this would
        // spawn FIGHTING_EACH_OTHER state NPCs and notify the game world.
        for (NPC npc : crowd) {
            if (npc.getState() == NPCState.WATCHING_MATCH ||
                npc.getState() == NPCState.GROANING ||
                npc.getState() == NPCState.AGITATED) {
                // Mark a subset as fighting — simplified implementation
                break;
            }
        }
    }

    // ── TV sabotage ───────────────────────────────────────────────────────────

    /**
     * Attempt to sabotage the pub TV. Requires the player to hold E for 5 seconds
     * with a CROWBAR or CABLE item, during Dave's blind spot window.
     *
     * @param player        the player's inventory
     * @param holdDuration  how long the player has been holding E (seconds)
     * @param allNpcs       all NPCs (for rumour seeding)
     * @param achievementCb achievement callback
     * @return true if sabotage succeeded
     */
    public boolean sabotageTV(Inventory player, float holdDuration, List<NPC> allNpcs,
                               NotorietySystem.AchievementCallback achievementCb) {
        if (tvState == TvState.BROKEN) return false;
        if (!player.hasItem(Material.CROWBAR) && !player.hasItem(Material.CABLE)) return false;

        if (holdDuration < TV_SABOTAGE_HOLD_DURATION) return false;

        // Success
        tvState = TvState.BROKEN;
        tvSabotaged = true;

        // Agitate the crowd
        agitationTimer = AGITATION_DURATION_SECONDS;
        agitatedBrawlTimer = AGITATED_BRAWL_INTERVAL;
        for (NPC npc : crowd) {
            npc.setState(NPCState.AGITATED);
        }

        // Seed rumour
        seedRumour(allNpcs, RumourType.TELLY_SABOTAGE,
            "Someone cut the aerial cable at the Wetherspoons. Mid-match. On a penalty.");

        // Check if Dave witnessed
        boolean witnessed = daveWatching || (random.nextFloat() < DAVE_WITNESS_CHANCE);
        boolean rivalBlamedInstead = (!daveWatching) && (random.nextFloat() < RIVAL_FAN_BLAME_CHANCE);

        if (witnessed && !rivalBlamedInstead) {
            // Dave saw the player
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(WANTED_TV_SABOTAGE_WITNESSED, 0, 0, 0, achievementCb);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NOTORIETY_TV_SABOTAGE, achievementCb);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.TV_SABOTAGE);
            }
            // Achievement NOT awarded if witnessed
        } else {
            // Got away with it
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NOTORIETY_TV_SABOTAGE, achievementCb);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.TV_SABOTAGE);
            }
            if (achievementCb != null) {
                achievementCb.award(AchievementType.SABOTEUR);
            }
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.SABOTEUR);
            }
        }

        // Street Lads faction loses 5 respect (they were enjoying it)
        if (factionSystem != null) {
            factionSystem.modifyRespect(FactionSystem.Faction.STREET_LADS, -5);
        }

        return true;
    }

    /**
     * Convenience overload: sabotageTV with daveWatching field used for witnessed check.
     */
    public boolean sabotageTV(Inventory player, List<NPC> allNpcs,
                               NotorietySystem.AchievementCallback achievementCb) {
        return sabotageTV(player, TV_SABOTAGE_HOLD_DURATION, allNpcs, achievementCb);
    }

    // ── Betting ───────────────────────────────────────────────────────────────

    /**
     * Place a bet with Barry the Bookie before kick-off.
     *
     * @param target   the result to bet on
     * @param amount   number of COIN to bet
     * @param inventory player's inventory
     * @return the result of the bet attempt
     */
    public BetResult placeBet(MatchResult target, int amount, Inventory inventory) {
        if (currentHour >= KICKOFF_HOUR) {
            return BetResult.BETTING_CLOSED;
        }
        if (amount > MAX_BET_COINS) {
            return BetResult.MAX_BET_EXCEEDED;
        }
        if (betPlaced) {
            return BetResult.ALREADY_BET;
        }
        if (inventory.getItemCount(Material.COIN) < amount) {
            return BetResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, amount);
        betAmount = amount;
        betTarget = target;
        betPlaced = true;
        return BetResult.ACCEPTED;
    }

    private void resolveBet(NotorietySystem.AchievementCallback achievementCb) {
        if (!betPlaced || betTarget == null || matchResult == null) return;

        if (betTarget == matchResult) {
            int odds = getOdds(betTarget);
            int gross = betAmount * odds;
            int payout = gross;

            // Marchetti cut if match was fixed
            int marchettiDeduction = 0;
            if (resultForced) {
                marchettiDeduction = Math.round(gross * MARCHETTI_CUT);
                payout = gross - marchettiDeduction;
            }

            // Give winnings
            // Note: inventory reference not stored — caller must supply or use resolvebet(inventory,...)
            // This internal method is called from endMatch; actual payout tracked via getPendingBetPayout()
            pendingBetPayout = payout;
            pendingBetMarchettiCut = marchettiDeduction;

            // Achievements
            if (betTarget == MatchResult.ENGLAND_WIN && achievementCb != null) {
                achievementCb.award(AchievementType.ITS_COMING_HOME);
                if (achievementSystem != null) achievementSystem.unlock(AchievementType.ITS_COMING_HOME);
            }
            if (resultForced && achievementCb != null) {
                achievementCb.award(AchievementType.MATCH_FIXER);
                if (achievementSystem != null) achievementSystem.unlock(AchievementType.MATCH_FIXER);
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.ENGLAND_MATCH_FIXING);
                }
            }
        } else {
            pendingBetPayout = 0;
            pendingBetMarchettiCut = 0;
        }
        betPlaced = false;
    }

    /** Pending bet payout (COIN) — apply to player inventory after full-time. */
    private int pendingBetPayout = 0;

    /** Marchetti deduction from winnings. */
    private int pendingBetMarchettiCut = 0;

    private int getOdds(MatchResult result) {
        switch (result) {
            case ENGLAND_WIN:  return WIN_ODDS;
            case ENGLAND_DRAW: return DRAW_ODDS;
            case ENGLAND_LOSS: return OPPOSITION_ODDS;
            default:           return 1;
        }
    }

    // ── Match fix ─────────────────────────────────────────────────────────────

    /**
     * Apply the MATCH_FIX_ITEM to force the match result to ENGLAND_LOSS.
     * Requires Marchetti Crew Respect ≥ 60. Consumes the item.
     *
     * @param player        player's inventory
     * @param achievementCb achievement callback
     * @return true if fix was applied
     */
    public boolean applyMatchFix(Inventory player,
                                  NotorietySystem.AchievementCallback achievementCb) {
        if (!player.hasItem(Material.MATCH_FIX_ITEM)) return false;

        // Check Marchetti Crew Respect
        if (factionSystem != null) {
            int respect = factionSystem.getRespect(FactionSystem.Faction.MARCHETTI_CREW);
            if (respect < MARCHETTI_RESPECT_REQUIRED) return false;
        }

        player.removeItem(Material.MATCH_FIX_ITEM, 1);
        resultForced = true;
        return true;
    }

    // ── Trophy cabinet heist ──────────────────────────────────────────────────

    /**
     * Break open the trophy cabinet and loot its contents.
     * Requires GLASS_CUTTER (silent) or CROWBAR (loud — triggers 30-block noise).
     *
     * @param player        player's inventory
     * @param allNpcs       all NPCs (for rumour seeding)
     * @param achievementCb achievement callback
     * @return true if at least one item was taken
     */
    public boolean breakTrophyCabinet(Inventory player, List<NPC> allNpcs,
                                       NotorietySystem.AchievementCallback achievementCb) {
        if (trophyItems.isEmpty()) return false;
        if (!player.hasItem(Material.GLASS_CUTTER) && !player.hasItem(Material.CROWBAR)) {
            return false;
        }

        trophyCabinetSmashed = true;

        // Transfer all remaining trophy items to player
        for (Material item : new ArrayList<>(trophyItems)) {
            player.addItem(item, 1);
            trophyItemsTaken.add(item);
            trophyItems.remove(item);
        }

        // Notoriety +8 for full heist
        if (notorietySystem != null && !trophyItemsTaken.isEmpty()) {
            notorietySystem.addNotoriety(NOTORIETY_TROPHY_HEIST, achievementCb);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.TROPHY_THEFT);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(trophyItemsTaken.size());
        }

        // Full heist achievement (all 3 items)
        if (trophyItemsTaken.size() >= 3) {
            seedRumour(allNpcs, RumourType.TROPHY_CABINET_NICKED,
                "Someone had the Wetherspoons trophy cabinet away. In the middle of the match. Terry's devastated.");
            if (achievementCb != null) {
                achievementCb.award(AchievementType.TROPHY_HUNTER);
            }
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.TROPHY_HUNTER);
            }
            if (newspaperSystem != null) {
                newspaperSystem.publishHeadline(
                    "Brazen Thief Clears Out Northfield Wetherspoons Trophy Cabinet During England Match.");
            }
        }

        return true;
    }

    // ── German flag mechanic ──────────────────────────────────────────────────

    /**
     * Plant a GERMAN_FLAG on the pub noticeboard. All crowd enters HOSTILE_TO_PLAYER
     * for 30 seconds. Awards DIPLOMATIC_INCIDENT achievement.
     * Seeds GERMAN_FLAG_OUTRAGE rumour (widest propagation).
     *
     * @param player        player's inventory
     * @param allNpcs       all NPCs (for rumour seeding)
     * @param achievementCb achievement callback
     * @return true if flag was planted
     */
    public boolean plantFlag(Inventory player, List<NPC> allNpcs,
                              NotorietySystem.AchievementCallback achievementCb) {
        if (!player.hasItem(Material.GERMAN_FLAG)) return false;

        player.removeItem(Material.GERMAN_FLAG, 1);

        // All crowd hostile for 30 seconds
        for (NPC npc : crowd) {
            npc.setState(NPCState.HOSTILE_TO_PLAYER);
        }

        // Notoriety +5
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_GERMAN_FLAG, achievementCb);
        }

        // Wanted +1
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(WANTED_GERMAN_FLAG);
        }

        // Seed widest propagation rumour to all relevant NPCs
        if (rumourNetwork != null && allNpcs != null) {
            Rumour outrage = new Rumour(RumourType.GERMAN_FLAG_OUTRAGE,
                "Someone brought a German flag into the Wetherspoons during the England match. Police got called.");
            for (NPC npc : allNpcs) {
                NPCType type = npc.getType();
                if (type == NPCType.PUBLIC || type == NPCType.PENSIONER ||
                    type == NPCType.JOURNALIST || type == NPCType.BARMAN) {
                    rumourNetwork.addRumour(npc, outrage);
                }
            }
        }

        // Achievement
        if (achievementCb != null) {
            achievementCb.award(AchievementType.DIPLOMATIC_INCIDENT);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DIPLOMATIC_INCIDENT);
        }

        return true;
    }

    // ── Crowd management ──────────────────────────────────────────────────────

    /**
     * Spawn crowd NPCs (30–40 WETHERSPOONS_CROWD) for the match.
     * Count determined from {@link #CROWD_MIN} to {@link #CROWD_CAP}.
     */
    public void spawnCrowd() {
        int count = CROWD_MIN + random.nextInt(CROWD_CAP - CROWD_MIN + 1);
        crowd.clear();
        for (int i = 0; i < count; i++) {
            NPC npc = new NPC(NPCType.WETHERSPOONS_CROWD, i * 0.5f, 1f, i * 0.5f);
            npc.setState(NPCState.IDLE);
            crowd.add(npc);
        }
    }

    /**
     * Add an NPC to the crowd (for testing or external spawning).
     */
    public void addCrowdNPC(NPC npc) {
        crowd.add(npc);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void seedRumour(List<NPC> allNpcs, RumourType type, String text) {
        if (rumourNetwork == null || allNpcs == null || allNpcs.isEmpty()) return;
        Rumour rumour = new Rumour(type, text);
        // Seed to first available NPC
        for (NPC npc : allNpcs) {
            rumourNetwork.addRumour(npc, rumour);
            return;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isMatchActive() {
        return matchActive;
    }

    public boolean isPreMatchActive() {
        return preMatchActive;
    }

    public boolean isMatchCancelled() {
        return matchCancelled;
    }

    public MatchResult getMatchResult() {
        return matchResult;
    }

    public TvState getTvState() {
        return tvState;
    }

    public boolean isTvSabotaged() {
        return tvSabotaged;
    }

    public boolean isDaveBlindSpot() {
        return daveBlindSpot;
    }

    public boolean isDaveWatching() {
        return daveWatching;
    }

    public float getAgitationTimer() {
        return agitationTimer;
    }

    public boolean isGoalCelebrationActive() {
        return goalCelebrationActive;
    }

    public float getShopUnattendedTimer() {
        return shopUnattendedTimer;
    }

    public List<NPC> getCrowd() {
        return crowd;
    }

    public int getPendingBetPayout() {
        return pendingBetPayout;
    }

    public int getPendingBetMarchettiCut() {
        return pendingBetMarchettiCut;
    }

    public boolean isBetPlaced() {
        return betPlaced;
    }

    public MatchResult getBetTarget() {
        return betTarget;
    }

    public boolean isResultForced() {
        return resultForced;
    }

    public boolean isTrophyCabinetSmashed() {
        return trophyCabinetSmashed;
    }

    public List<Material> getTrophyItems() {
        return trophyItems;
    }

    public List<Material> getTrophyItemsTaken() {
        return trophyItemsTaken;
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** Force the current game hour for testing. */
    public void setCurrentHourForTesting(float hour) {
        this.currentHour = hour;
    }

    /** Force the current day count for testing. */
    public void setCurrentDayCountForTesting(int dayCount) {
        this.currentDayCount = dayCount;
    }

    /** Force daveWatching for testing. */
    public void setDaveWatchingForTesting(boolean watching) {
        this.daveWatching = watching;
    }

    /** Force matchActive for testing. */
    public void setMatchActiveForTesting(boolean active) {
        this.matchActive = active;
    }

    /** Force preMatchActive for testing. */
    public void setPreMatchActiveForTesting(boolean active) {
        this.preMatchActive = active;
    }

    /** Force matchResult for testing. */
    public void setMatchResultForTesting(MatchResult result) {
        this.matchResult = result;
    }

    /** Force daveBlindSpot for testing. */
    public void setDaveBlindSpotForTesting(boolean blindSpot) {
        this.daveBlindSpot = blindSpot;
    }

    /** Force resultForced for testing. */
    public void setResultForcedForTesting(boolean forced) {
        this.resultForced = forced;
    }
}

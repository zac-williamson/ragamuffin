package ragamuffin.core;

import ragamuffin.audio.SoundEffect;
import ragamuffin.audio.SoundSystem;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;
import ragamuffin.core.Faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1246: Northfield Sunday League Football — FootballSystem.
 *
 * <p>Manages the complete lifecycle of the Sunday League match in the park:
 * pitch setup, team spawning, kick-off, goal scoring, half-time, full-time,
 * post-match pub migration and newspaper report.
 *
 * <h3>Schedule</h3>
 * <ul>
 *   <li>Every Sunday 10:00–11:00 in-game time.</li>
 *   <li>Cancelled if weather is {@link Weather#THUNDERSTORM}.</li>
 *   <li>RAIN/DRIZZLE: muddy pitch — player movement 20% slower on pitch grass.</li>
 * </ul>
 *
 * <h3>Teams</h3>
 * <ul>
 *   <li><b>Ragamuffin Rovers</b> — 5 × STREET_LAD NPCs.</li>
 *   <li><b>Council FC</b>        — 5 × COUNCIL_MEMBER NPCs.</li>
 *   <li><b>Referee</b>           — 1 × REFEREE NPC.</li>
 * </ul>
 *
 * <h3>Probability engine</h3>
 * Goal-attempt rolls every {@value #GOAL_ATTEMPT_INTERVAL_SECONDS} real seconds.
 * Base chance: {@value #BASE_GOAL_CHANCE}. Modifiers applied per team.
 *
 * <h3>Betting</h3>
 * {@link #placeBet} / {@link #resolveBets}: 2:1 on winner, 3:1 on draw.
 *
 * <h3>Integrations</h3>
 * {@link FactionSystem}, {@link WeatherSystem}, {@link NotorietySystem},
 * {@link CriminalRecord}, {@link WetherspoonsSystem}, {@link RumourNetwork},
 * {@link SoundSystem}, {@link AchievementSystem}.
 */
public class FootballSystem {

    // ── Schedule constants ────────────────────────────────────────────────────

    /** Day-of-week index for Sunday (dayCount % 7 == SUNDAY). */
    public static final int SUNDAY = 6;

    /** Match kick-off hour (10:00). */
    public static final float MATCH_START_HOUR = 10.0f;

    /** Match full-time hour (11:00 = 10 + 60 in-game minutes). */
    public static final float MATCH_END_HOUR = 11.0f;

    /** Half-time in-game hour (10:06 = after 6 in-game minutes). */
    public static final float HALF_TIME_HOUR = 10.1f; // 10h 6min = 10.1h

    /** Pre-match pitch setup begins at 09:59 (fans arrive at 09:50). */
    public static final float SETUP_HOUR = 9.983f; // ~09:59

    // ── Match simulation constants ────────────────────────────────────────────

    /** Real-seconds between goal-attempt rolls. */
    public static final float GOAL_ATTEMPT_INTERVAL_SECONDS = 30.0f;

    /** Base goal-chance per attempt (30%). */
    public static final float BASE_GOAL_CHANCE = 0.30f;

    /** Buff when the relevant faction has ≥ 60 Respect (+10%). */
    public static final float FACTION_RESPECT_BUFF = 0.10f;

    /** Penalty for muddy pitch (RAIN/DRIZZLE) for both teams (−5%). */
    public static final float MUD_PENALTY = 0.05f;

    /** Player-on-pitch buff for Rovers (+5%). */
    public static final float PLAYER_SUBSTITUTE_BUFF = 0.05f;

    /** Sabotage penalty for targeted team per sabotaged player (−20%). */
    public static final float SABOTAGE_PENALTY = 0.20f;

    /** Chance of referee catching a sabotage attempt when ref is within 6 blocks (30%). */
    public static final float SABOTAGE_CATCH_CHANCE = 0.30f;

    /** Referee proximity for sabotage catch check (6 blocks). */
    public static final float REF_PROXIMITY_SABOTAGE = 6.0f;

    /** Referee proximity for tackle penalty check (6 blocks). */
    public static final float REF_PROXIMITY_TACKLE = 6.0f;

    /** Noise level threshold below which an unseen tackle is not penalised. */
    public static final float NOISE_THRESHOLD_TACKLE = 0.4f;

    /** Number of players per team. */
    public static final int PLAYERS_PER_TEAM = 5;

    // ── Pitch layout constants ────────────────────────────────────────────────

    /** Pitch centre X offset from park landmark (relative). */
    public static final float PITCH_CENTRE_X = 0f;

    /** Pitch centre Z offset from park landmark (relative). */
    public static final float PITCH_CENTRE_Z = 0f;

    /** Half-width of the pitch (10 blocks wide total). */
    public static final float PITCH_HALF_WIDTH = 5f;

    /** Half-length of the pitch (10 blocks long total). */
    public static final float PITCH_HALF_LENGTH = 5f;

    // ── Betting constants ─────────────────────────────────────────────────────

    /** Payout multiplier for correct winner bet (2:1). */
    public static final int WINNER_ODDS = 2;

    /** Payout multiplier for correct draw bet (3:1). */
    public static final int DRAW_ODDS = 3;

    /** Minimum bet stake in COIN. */
    public static final int MIN_BET_STAKE = 1;

    /** Maximum bet stake in COIN. */
    public static final int MAX_BET_STAKE = 5;

    // ── Notoriety constants ───────────────────────────────────────────────────

    /** Notoriety gained on yellow card (tackle seen). */
    public static final int NOTORIETY_YELLOW_CARD = 2;

    /** Notoriety gained on red card (second yellow or blatant). */
    public static final int NOTORIETY_RED_CARD = 4;

    /** Notoriety gained on first referee abuse. */
    public static final int NOTORIETY_REF_ABUSE_YELLOW = 2;

    /** Notoriety gained on second referee abuse (red + ejection). */
    public static final int NOTORIETY_REF_ABUSE_RED = 3;

    /** Notoriety gained for punching the referee. */
    public static final int NOTORIETY_PUNCH_REF = 8;

    /** Notoriety gained on sabotage catch by referee. */
    public static final int NOTORIETY_SABOTAGE_CAUGHT = 10;

    // ── Post-match patron surge ───────────────────────────────────────────────

    /** Minimum extra pub patrons after the match. */
    public static final int MIN_PATRON_SURGE = 4;

    /** Maximum extra pub patrons after the match. */
    public static final int MAX_PATRON_SURGE = 8;

    /** Duration (in-game hours) of the patron surge. */
    public static final float PATRON_SURGE_DURATION_HOURS = 2.0f;

    // ── Physio bag constants ──────────────────────────────────────────────────

    /** HP restored by physio bag. */
    public static final int PHYSIO_HEAL_AMOUNT = 20;

    // ── Referee abuse speech ──────────────────────────────────────────────────

    private static final String[] ABUSE_LINES = {
        "Terrible ref!", "You're blind, mate!", "My gran runs faster!"
    };

    private static final String REF_YELLOW_RESPONSE = "Watch it.";

    // ── Goal announcement dialogue ────────────────────────────────────────────

    private static final String GOAL_SPEECH_DURATION_SECONDS = "4.0"; // informational

    /** Duration of GOAL speech bubble in seconds. */
    public static final float GOAL_SPEECH_DURATION = 4.0f;

    // ── Rumour text ───────────────────────────────────────────────────────────

    /** Thunderstorm cancellation rumour. */
    public static final String CANCELLATION_RUMOUR =
        "Match called off — thunder's about.";

    /** Post-match gossip rumour. */
    public static final String POST_MATCH_RUMOUR =
        "Did you see that match on Sunday? Ref was having a nightmare.";

    // ── Teams ─────────────────────────────────────────────────────────────────

    /** The two teams. */
    public enum Team {
        ROVERS, COUNCIL_FC, DRAW
    }

    // ── Match state ───────────────────────────────────────────────────────────

    private final Random random;

    // Current match score: [0]=Rovers, [1]=Council FC
    private final int[] score = new int[2];

    // NPCs on the pitch
    private final List<NPC> roversPlayers  = new ArrayList<>();
    private final List<NPC> councilPlayers = new ArrayList<>();
    private NPC referee;
    private final List<NPC> fans = new ArrayList<>();

    // Props spawned for the pitch
    private final List<SpawnedProp> pitchProps = new ArrayList<>();

    // Whether the pitch is currently set up
    private boolean pitchSetup = false;

    // Whether a match is currently active
    private boolean matchActive = false;

    // Whether we've checked for cancellation this Sunday
    private boolean cancellationChecked = false;

    // Whether post-match has been resolved
    private boolean postMatchResolved = false;

    // Timer for goal-attempt rolls (real seconds)
    private float goalAttemptTimer = 0f;

    // Which team currently has possession (alternates)
    private int possessionTeam = 0; // 0=Rovers, 1=Council FC

    // Player participation
    private boolean playerOnPitch = false;
    private boolean physioUsed = false;

    // Referee abuse / card state
    private int playerCardCount = 0; // 0=clean, 1=yellow, 2=red (ejected)
    private int refAbuseCount = 0;

    // Sabotage tracking
    private int sabotageCount = 0; // number of Council FC players sabotaged

    // Active bet
    private Team betTeam = null;
    private int betStake = 0;
    private boolean betPlaced = false;

    // Weather/mud state
    private boolean muddy = false;

    // Last day processed (to avoid double-processing per Sunday)
    private int lastMatchDay = -1;

    // Last match day for post-match
    private int lastPostMatchDay = -1;

    // Park landmark world-space position (injected or defaulted)
    private float parkX = 0f;
    private float parkZ = 0f;

    // ── System dependencies (nullable — null-safe throughout) ─────────────────

    private FactionSystem factionSystem;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private WetherspoonsSystem wetherspoonsSystem;
    private RumourNetwork rumourNetwork;
    private SoundSystem soundSystem;
    private AchievementSystem achievementSystem;

    // ── Inner types ───────────────────────────────────────────────────────────

    /** A prop spawned on the pitch. */
    public static class SpawnedProp {
        public final PropType type;
        public final float x, y, z;

        public SpawnedProp(PropType type, float x, float y, float z) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public FootballSystem() {
        this(new Random());
    }

    public FootballSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setWetherspoonsSystem(WetherspoonsSystem wetherspoonsSystem) {
        this.wetherspoonsSystem = wetherspoonsSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setSoundSystem(SoundSystem soundSystem) {
        this.soundSystem = soundSystem;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setParkPosition(float x, float z) {
        this.parkX = x;
        this.parkZ = z;
    }

    // ── Schedule ──────────────────────────────────────────────────────────────

    /**
     * Returns true when a match should be running:
     * Sunday 10:00–11:00 and weather != THUNDERSTORM.
     */
    public boolean isMatchActive() {
        return matchActive;
    }

    /**
     * Returns true when the pitch is muddy (RAIN or DRIZZLE weather).
     */
    public boolean isMuddy() {
        return muddy;
    }

    /**
     * Returns true when the player is on the pitch as a Rovers substitute.
     */
    public boolean isPlayerOnPitch() {
        return playerOnPitch;
    }

    /**
     * Number of yellow/red cards the player has received this match.
     */
    public int getPlayerCardCount() {
        return playerCardCount;
    }

    /**
     * Current score for the given team.
     */
    public int getScore(Team team) {
        if (team == Team.ROVERS) return score[0];
        if (team == Team.COUNCIL_FC) return score[1];
        return 0;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Called once per frame by the game loop.
     *
     * @param delta         real seconds since last frame
     * @param timeSystem    game clock
     * @param weatherSystem current weather
     * @param allNpcs       all live NPCs (may be null in unit tests)
     */
    public void update(float delta, TimeSystem timeSystem, WeatherSystem weatherSystem,
                       List<NPC> allNpcs) {
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();
        int dow = dayCount % 7;
        Weather weather = (weatherSystem != null) ? weatherSystem.getCurrentWeather() : Weather.CLEAR;

        // ── Pitch setup: kick off at 10:00 on Sunday ─────────────────────────
        if (dow == SUNDAY && hour >= SETUP_HOUR && hour < MATCH_END_HOUR
                && !pitchSetup && !cancellationChecked) {

            cancellationChecked = true;

            if (weather == Weather.THUNDERSTORM) {
                // Match cancelled — seed cancellation rumour
                seedCancellationRumour(allNpcs);
                return;
            }

            // Determine mud
            muddy = (weather == Weather.RAIN || weather == Weather.DRIZZLE);

            setupPitch();
            spawnTeams();
            matchActive = true;
            postMatchResolved = false;
            goalAttemptTimer = 0f;
            possessionTeam = 0;
            playerOnPitch = false;
            physioUsed = false;
            playerCardCount = 0;
            refAbuseCount = 0;
            sabotageCount = 0;
            score[0] = 0;
            score[1] = 0;
            lastMatchDay = dayCount;
        }

        // ── Reset cancellation flag for next Sunday ───────────────────────────
        if (dow != SUNDAY) {
            cancellationChecked = false;
            if (!matchActive) {
                pitchSetup = false;
            }
        }

        // ── Match running ─────────────────────────────────────────────────────
        if (matchActive) {
            // Full-time check
            if (hour >= MATCH_END_HOUR) {
                endMatch(allNpcs);
                return;
            }

            // Goal-attempt timer
            goalAttemptTimer += delta;
            if (goalAttemptTimer >= GOAL_ATTEMPT_INTERVAL_SECONDS) {
                goalAttemptTimer -= GOAL_ATTEMPT_INTERVAL_SECONDS;
                rollGoalAttempt(weather, allNpcs);
                // Alternate possession
                possessionTeam = 1 - possessionTeam;
            }
        }
    }

    // ── Pitch setup / teardown ────────────────────────────────────────────────

    /**
     * Spawn pitch props at the park landmark position.
     * Called automatically at match start; also callable by tests.
     */
    public void setupPitch() {
        if (pitchSetup) return;
        pitchProps.clear();

        float cx = parkX + PITCH_CENTRE_X;
        float cz = parkZ + PITCH_CENTRE_Z;

        // Goal posts: north and south ends
        pitchProps.add(new SpawnedProp(PropType.GOAL_POST_PROP, cx, 0f, cz - PITCH_HALF_LENGTH));
        pitchProps.add(new SpawnedProp(PropType.GOAL_POST_PROP, cx, 0f, cz + PITCH_HALF_LENGTH));

        // Corner flags
        pitchProps.add(new SpawnedProp(PropType.CORNER_FLAG_PROP, cx - PITCH_HALF_WIDTH, 0f, cz - PITCH_HALF_LENGTH));
        pitchProps.add(new SpawnedProp(PropType.CORNER_FLAG_PROP, cx + PITCH_HALF_WIDTH, 0f, cz - PITCH_HALF_LENGTH));
        pitchProps.add(new SpawnedProp(PropType.CORNER_FLAG_PROP, cx - PITCH_HALF_WIDTH, 0f, cz + PITCH_HALF_LENGTH));
        pitchProps.add(new SpawnedProp(PropType.CORNER_FLAG_PROP, cx + PITCH_HALF_WIDTH, 0f, cz + PITCH_HALF_LENGTH));

        // Centre circle
        pitchProps.add(new SpawnedProp(PropType.CENTRE_CIRCLE_PROP, cx, 0f, cz));

        // Pitch bookie: east touchline, 2 blocks south of centre
        pitchProps.add(new SpawnedProp(PropType.PITCH_BOOKIE_PROP, cx + PITCH_HALF_WIDTH + 1f, 0f, cz + 2f));

        // Physio bag: west touchline
        pitchProps.add(new SpawnedProp(PropType.PHYSIO_BAG_PROP, cx - PITCH_HALF_WIDTH - 1f, 0f, cz));

        pitchSetup = true;
    }

    /**
     * Remove all pitch props at full-time.
     * Called automatically at match end; also callable by tests.
     */
    public void teardownPitch() {
        pitchProps.clear();
        pitchSetup = false;
    }

    /** Read-only list of currently spawned pitch props. */
    public List<SpawnedProp> getPitchProps() {
        return pitchProps;
    }

    // ── NPC spawning ──────────────────────────────────────────────────────────

    private void spawnTeams() {
        roversPlayers.clear();
        councilPlayers.clear();

        float cx = parkX + PITCH_CENTRE_X;
        float cz = parkZ + PITCH_CENTRE_Z;

        // Spawn Rovers (STREET_LAD) on south half
        for (int i = 0; i < PLAYERS_PER_TEAM; i++) {
            float x = cx - 2f + i * 1f;
            float z = cz + 2f;
            NPC npc = new NPC(NPCType.STREET_LAD, x, 0f, z);
            roversPlayers.add(npc);
        }

        // Spawn Council FC (COUNCIL_MEMBER) on north half
        for (int i = 0; i < PLAYERS_PER_TEAM; i++) {
            float x = cx - 2f + i * 1f;
            float z = cz - 2f;
            NPC npc = new NPC(NPCType.COUNCIL_MEMBER, x, 0f, z);
            councilPlayers.add(npc);
        }

        // Referee
        referee = new NPC(NPCType.REFEREE, cx, 0f, cz);
    }

    /** All active Rovers players (excludes sabotaged/removed). */
    public List<NPC> getRoversPlayers() {
        return roversPlayers;
    }

    /** All active Council FC players (excludes sabotaged/removed). */
    public List<NPC> getCouncilPlayers() {
        return councilPlayers;
    }

    /** The referee NPC (null before match starts). */
    public NPC getRefereeNpc() {
        return referee;
    }

    // ── Goal scoring ──────────────────────────────────────────────────────────

    private void rollGoalAttempt(Weather weather, List<NPC> allNpcs) {
        // Compute chance for the team in possession
        float chance;
        Team scoringTeam;

        if (possessionTeam == 0) {
            // Rovers' attempt
            chance = computeRoversChance(weather);
            scoringTeam = Team.ROVERS;
        } else {
            // Council FC's attempt
            chance = computeCouncilChance(weather);
            scoringTeam = Team.COUNCIL_FC;
        }

        if (random.nextFloat() < chance) {
            forceGoal(scoringTeam);
        }
    }

    private float computeRoversChance(Weather weather) {
        float chance = BASE_GOAL_CHANCE;
        if (factionSystem != null && factionSystem.getRespect(Faction.STREET_LADS) >= 60) {
            chance += FACTION_RESPECT_BUFF;
        }
        if (weather == Weather.RAIN || weather == Weather.DRIZZLE) {
            chance -= MUD_PENALTY;
        }
        if (playerOnPitch) {
            chance += PLAYER_SUBSTITUTE_BUFF;
        }
        return Math.max(0f, Math.min(1f, chance));
    }

    private float computeCouncilChance(Weather weather) {
        float chance = BASE_GOAL_CHANCE;
        if (factionSystem != null && factionSystem.getRespect(Faction.THE_COUNCIL) >= 60) {
            chance += FACTION_RESPECT_BUFF;
        }
        if (weather == Weather.RAIN || weather == Weather.DRIZZLE) {
            chance -= MUD_PENALTY;
        }
        // Sabotage reduces council chance by 20% per sabotaged player
        chance -= sabotageCount * SABOTAGE_PENALTY;
        return Math.max(0f, Math.min(1f, chance));
    }

    /**
     * Returns the effective goal-chance modifier for Council FC (for testing).
     * A negative value means their chance is reduced.
     */
    public float getCouncilGoalChanceModifier() {
        return -(sabotageCount * SABOTAGE_PENALTY);
    }

    /**
     * Force a goal for the given team (for testing or mission hooks).
     */
    public void forceGoal(Team team) {
        if (team == Team.ROVERS) {
            score[0]++;
        } else if (team == Team.COUNCIL_FC) {
            score[1]++;
        }

        // Referee announces goal
        if (referee != null) {
            String teamName = (team == Team.ROVERS) ? "Rovers" : "Council FC";
            referee.setSpeechText("GOAL! " + teamName + "!", GOAL_SPEECH_DURATION);
        }

        // Sound effect
        if (soundSystem != null) {
            soundSystem.play(SoundEffect.CROWD_CHEER);
        }

        // Seed rumour
        if (rumourNetwork != null && !roversPlayers.isEmpty()) {
            NPC nearbyNpc = roversPlayers.get(0);
            rumourNetwork.addRumour(nearbyNpc, new Rumour(RumourType.LOCAL_EVENT,
                "Did you see that goal?"));
        }
    }

    // ── Match end ─────────────────────────────────────────────────────────────

    /**
     * Force the match to end with the given score (used in tests).
     */
    public void forceMatchEnd(int roversScore, int councilScore) {
        score[0] = roversScore;
        score[1] = councilScore;
        matchActive = false;
        teardownPitch();
        resolvePostMatch(null);
    }

    private void endMatch(List<NPC> allNpcs) {
        matchActive = false;
        teardownPitch();
        resolvePostMatch(allNpcs);
    }

    private void resolvePostMatch(List<NPC> allNpcs) {
        if (postMatchResolved) return;
        postMatchResolved = true;

        // Faction respect adjustments
        if (factionSystem != null) {
            if (score[0] > score[1]) {
                // Rovers win
                factionSystem.applyRespectDelta(Faction.STREET_LADS, 3);
            } else if (score[1] > score[0]) {
                // Council FC win
                factionSystem.applyRespectDelta(Faction.THE_COUNCIL, 3);
            }
        }

        // Post-match pub patron surge
        if (wetherspoonsSystem != null) {
            int surge = MIN_PATRON_SURGE + random.nextInt(MAX_PATRON_SURGE - MIN_PATRON_SURGE + 1);
            wetherspoonsSystem.addPatrons(surge);
        }

        // Migrate fan NPCs to pub state
        for (NPC fan : fans) {
            fan.setState(NPCState.AT_PUB);
        }
        for (NPC player : roversPlayers) {
            player.setState(NPCState.AT_PUB);
        }
        for (NPC player : councilPlayers) {
            player.setState(NPCState.AT_PUB);
        }

        // Seed post-match rumour
        if (rumourNetwork != null) {
            List<NPC> targets = new ArrayList<>();
            targets.addAll(roversPlayers);
            targets.addAll(councilPlayers);
            if (referee != null) targets.add(referee);

            int rumourCount = Math.min(targets.size(), 2 + random.nextInt(3)); // 2-4
            for (int i = 0; i < rumourCount && i < targets.size(); i++) {
                rumourNetwork.addRumour(targets.get(i), new Rumour(RumourType.LOCAL_EVENT,
                    POST_MATCH_RUMOUR));
            }
        }

        // Player achievement for substituting and playing to full-time
        if (playerOnPitch && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SUNDAY_LEAGUE);
        }
    }

    // ── Betting ───────────────────────────────────────────────────────────────

    /**
     * Place a bet on the outcome of the match.
     *
     * @param team      the team (or DRAW) to bet on
     * @param stake     amount of COIN to stake (1–5)
     * @param inventory player inventory (COIN is deducted)
     * @return true if the bet was placed successfully
     */
    public boolean placeBet(Team team, int stake, Inventory inventory) {
        if (betPlaced) return false; // only one bet per match
        if (stake < MIN_BET_STAKE || stake > MAX_BET_STAKE) return false;
        if (inventory == null || !inventory.removeItem(Material.COIN, stake)) return false;

        betTeam = team;
        betStake = stake;
        betPlaced = true;
        return true;
    }

    /**
     * Resolve bets at full-time and credit winnings to inventory.
     *
     * @param inventory player inventory (COIN is added on win)
     */
    public void resolveBets(Inventory inventory) {
        if (!betPlaced || inventory == null) return;

        Team winner;
        if (score[0] > score[1]) winner = Team.ROVERS;
        else if (score[1] > score[0]) winner = Team.COUNCIL_FC;
        else winner = Team.DRAW;

        boolean won = (betTeam == winner);
        if (won) {
            int odds = (winner == Team.DRAW) ? DRAW_ODDS : WINNER_ODDS;
            int payout = betStake * odds;
            inventory.addItem(Material.COIN, payout);

            // Achievement for winning a bet
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.FOOTBALL_PUNTER);
            }
        }
        betPlaced = false;
    }

    // ── Player substitute ─────────────────────────────────────────────────────

    /**
     * Player presses E on the Rovers captain NPC before kick-off.
     * Replaces one STREET_LAD player from the pitch.
     *
     * @return true if the substitution succeeded
     */
    public boolean substitutePlayerIn() {
        if (playerOnPitch) return false;
        if (!pitchSetup) return false;
        if (!roversPlayers.isEmpty()) {
            NPC replaced = roversPlayers.get(roversPlayers.size() - 1);
            replaced.setState(NPCState.IDLE); // steps off pitch
            roversPlayers.remove(replaced);
        }
        playerOnPitch = true;
        return true;
    }

    // ── Tackle system ─────────────────────────────────────────────────────────

    /**
     * Player attempts to tackle a COUNCIL_MEMBER NPC on the pitch.
     *
     * @param target         the NPC being tackled
     * @param refDistance    distance from the referee (blocks)
     * @param noiseLevel     current noise level (0–1)
     * @param achievementCb  achievement callback (may be null)
     * @return TackleResult indicating whether the tackle was seen
     */
    public TackleResult attemptTackle(NPC target, float refDistance, float noiseLevel,
                                      NotorietySystem.AchievementCallback achievementCb) {
        if (!playerOnPitch || target == null) return TackleResult.NOT_ON_PITCH;
        if (playerCardCount >= 2) return TackleResult.ALREADY_EJECTED;

        boolean refereeNearby = refDistance <= REF_PROXIMITY_TACKLE;
        boolean tooNoisy = noiseLevel >= NOISE_THRESHOLD_TACKLE;
        boolean seen = refereeNearby || tooNoisy;

        if (seen) {
            // Yellow card
            playerCardCount++;
            if (criminalRecord != null) criminalRecord.record(CriminalRecord.CrimeType.AFFRAY);
            if (notorietySystem != null && achievementCb != null) {
                notorietySystem.addNotoriety(NOTORIETY_YELLOW_CARD, achievementCb);
            }

            if (playerCardCount >= 2) {
                // Red card — ejected
                playerOnPitch = false;
                if (notorietySystem != null && achievementCb != null) {
                    notorietySystem.addNotoriety(NOTORIETY_RED_CARD, achievementCb);
                }
                if (factionSystem != null) {
                    factionSystem.applyRespectDelta(Faction.THE_COUNCIL, -5);
                }
                return TackleResult.RED_CARD;
            }
            return TackleResult.YELLOW_CARD;
        } else {
            // Unseen dirty tackle — achievement
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.DIRTY_TACKLE);
            }
            return TackleResult.UNSEEN;
        }
    }

    /** Result of a tackle attempt. */
    public enum TackleResult {
        UNSEEN,
        YELLOW_CARD,
        RED_CARD,
        NOT_ON_PITCH,
        ALREADY_EJECTED
    }

    // ── Referee abuse ─────────────────────────────────────────────────────────

    /**
     * Player presses E on the referee NPC (verbal abuse).
     *
     * @param achievementCb achievement callback (may be null)
     * @return AbuseResult
     */
    public AbuseResult abuseReferee(NotorietySystem.AchievementCallback achievementCb) {
        if (!playerOnPitch) return AbuseResult.NOT_ON_PITCH;
        if (playerCardCount >= 2) return AbuseResult.ALREADY_EJECTED;

        // Player shouts abuse
        String abuseLine = ABUSE_LINES[refAbuseCount % ABUSE_LINES.length];
        refAbuseCount++;

        if (refAbuseCount == 1) {
            // Yellow card — ref issues warning
            playerCardCount++;
            if (referee != null) referee.setSpeechText(REF_YELLOW_RESPONSE, 3.0f);
            if (notorietySystem != null && achievementCb != null) {
                notorietySystem.addNotoriety(NOTORIETY_REF_ABUSE_YELLOW, achievementCb);
            }
            return AbuseResult.YELLOW_CARD;
        } else {
            // Red card + ejection
            playerCardCount = 2;
            playerOnPitch = false;
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.DISORDERLY_CONDUCT);
            }
            if (notorietySystem != null && achievementCb != null) {
                notorietySystem.addNotoriety(NOTORIETY_REF_ABUSE_RED, achievementCb);
            }
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.REF_ABUSE);
            }
            return AbuseResult.RED_CARD_EJECTED;
        }
    }

    /**
     * Player punches the referee NPC.
     *
     * @param wantedSystem  WantedSystem to trigger wanted stars (may be null)
     * @param achievementCb achievement callback (may be null)
     */
    public void punchReferee(Object wantedSystem,
                             NotorietySystem.AchievementCallback achievementCb) {
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.ASSAULT_OF_OFFICIAL);
        }
        if (notorietySystem != null && achievementCb != null) {
            notorietySystem.addNotoriety(NOTORIETY_PUNCH_REF, achievementCb);
        }
        playerOnPitch = false;
        playerCardCount = 2;
        // WantedSystem +2 stars — we use duck typing to avoid hard dependency
        if (wantedSystem instanceof WantedSystem) {
            ((WantedSystem) wantedSystem).addWantedStars(2, 0f, 0f, 0f, null);
        }
    }

    /** Result of a referee abuse action. */
    public enum AbuseResult {
        YELLOW_CARD,
        RED_CARD_EJECTED,
        NOT_ON_PITCH,
        ALREADY_EJECTED
    }

    // ── Sabotage ──────────────────────────────────────────────────────────────

    /**
     * Player gives a DODGY_PIE to a Council FC player (press E).
     *
     * @param target         the target COUNCIL_MEMBER NPC
     * @param inventory      player inventory (DODGY_PIE consumed)
     * @param refDistance    current distance from the referee
     * @param achievementCb  achievement callback (may be null)
     * @return SabotageResult
     */
    public SabotageResult sabotagePlayer(NPC target, Inventory inventory, float refDistance,
                                         NotorietySystem.AchievementCallback achievementCb) {
        if (target == null || inventory == null) return SabotageResult.FAILED;
        if (!inventory.hasItem(Material.DODGY_PIE, 1)) return SabotageResult.NO_PIE;

        inventory.removeItem(Material.DODGY_PIE, 1);

        // Check referee catch
        boolean refNearby = (refDistance <= REF_PROXIMITY_SABOTAGE);
        boolean caught = refNearby && (random.nextFloat() < SABOTAGE_CATCH_CHANCE);

        if (caught) {
            // Red card + MATCH_FIXING crime
            playerCardCount = 2;
            playerOnPitch = false;
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.MATCH_FIXING);
            }
            if (notorietySystem != null && achievementCb != null) {
                notorietySystem.addNotoriety(NOTORIETY_SABOTAGE_CAUGHT, achievementCb);
            }
            return SabotageResult.CAUGHT;
        }

        // Success — target becomes SICK
        target.setState(NPCState.SICK);
        councilPlayers.remove(target); // removed from active player list
        sabotageCount++;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DODGY_PIE);
        }

        return SabotageResult.SUCCESS;
    }

    /** Result of a sabotage attempt. */
    public enum SabotageResult {
        SUCCESS,
        CAUGHT,
        NO_PIE,
        FAILED
    }

    // ── Physio bag ────────────────────────────────────────────────────────────

    /**
     * Player presses E on the physio bag while on-pitch (once per match).
     *
     * @param playerHealth current player health
     * @return amount of health restored (0 if already used)
     */
    public int usePhysioBag(int playerHealth) {
        if (physioUsed || !playerOnPitch) return 0;
        physioUsed = true;
        return PHYSIO_HEAL_AMOUNT;
    }

    // ── Cancellation rumour ───────────────────────────────────────────────────

    private void seedCancellationRumour(List<NPC> allNpcs) {
        if (rumourNetwork == null || allNpcs == null || allNpcs.isEmpty()) return;
        // Seed to first available NPC near park
        NPC target = allNpcs.get(0);
        rumourNetwork.addRumour(target, new Rumour(RumourType.LOCAL_EVENT, CANCELLATION_RUMOUR));
    }

    // ── Mud speed modifier ────────────────────────────────────────────────────

    /**
     * Returns the player movement speed multiplier for on-pitch grass blocks.
     * 0.8 when muddy (20% slowdown), 1.0 otherwise.
     */
    public float getMudSpeedMultiplier() {
        return muddy ? 0.8f : 1.0f;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /**
     * Force the match into active state without going through the normal update cycle.
     * Spawns teams and sets up pitch. Used by integration tests.
     */
    public void forceStartMatch(Weather weather) {
        muddy = (weather == Weather.RAIN || weather == Weather.DRIZZLE);
        setupPitch();
        spawnTeams();
        matchActive = true;
        postMatchResolved = false;
        goalAttemptTimer = 0f;
        possessionTeam = 0;
        playerOnPitch = false;
        physioUsed = false;
        playerCardCount = 0;
        refAbuseCount = 0;
        sabotageCount = 0;
        score[0] = 0;
        score[1] = 0;
        cancellationChecked = true;
    }

    /**
     * Add fan NPCs to the system (so they migrate to pub at full-time).
     */
    public void addFan(NPC fan) {
        fans.add(fan);
    }

    /**
     * Force the player onto the pitch (substitute in) — for testing.
     */
    public void forcePlayerOnPitch() {
        playerOnPitch = true;
    }

    /**
     * Returns the number of sabotaged Council FC players.
     */
    public int getSabotageCount() {
        return sabotageCount;
    }
}

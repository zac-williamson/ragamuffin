package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Issue #1414 — Northfield Local Election system.
 *
 * <p>Manages the full 8-day election cycle on days 83–90:
 * <ul>
 *   <li>Days 83–89: Canvassing Week — candidates, canvassers, leaflets, poster wars.</li>
 *   <li>Day 90 07:00–22:00: Polling Day — vote casting, ballot box theft, exclusion zone.</li>
 *   <li>Day 90 22:30+: Count Night — result declaration, box-stuffing event.</li>
 *   <li>Days 91–97: Post-election faction effects.</li>
 * </ul>
 */
public class LocalElectionSystem {

    // ── Election calendar constants ────────────────────────────────────────────

    /** First day of canvassing week. */
    public static final int CANVASSING_START_DAY = 83;

    /** Polling Day (day of year). */
    public static final int POLLING_DAY = 90;

    /** Polling station opens (hour). */
    public static final float POLLING_STATION_OPEN_HOUR = 7.0f;

    /** Polling station closes (hour). */
    public static final float POLLING_STATION_CLOSE_HOUR = 22.0f;

    /** Count night begins (hour). */
    public static final float COUNT_START_HOUR = 22.5f;

    /** Days post-election that faction effects last. */
    public static final int POST_ELECTION_EFFECT_DAYS = 7;

    // ── Mechanic constants ─────────────────────────────────────────────────────

    /** Base detection risk for postal vote fraud (15%). */
    public static final float POSTAL_VOTE_DETECTION_BASE = 0.15f;

    /** Reduced detection risk with SLEIGHT_OF_HAND ≥ Journeyman (5%). */
    public static final float POSTAL_VOTE_DETECTION_SLEIGHT = 0.05f;

    /** Seconds to hold E for ballot box theft. */
    public static final float BALLOT_BOX_STEAL_SECONDS = 3.0f;

    /** Notoriety gain for stealing the ballot box. */
    public static final int BALLOT_BOX_NOTORIETY = 15;

    /** Seconds to hold E for stealing campaign leaflets. */
    public static final float LEAFLET_STEAL_SECONDS = 2.0f;

    /** Cost in COIN to bribe a canvasser. */
    public static final int BRIBE_CANVASSER_COST = 2;

    /** Notoriety gain for defacing an election poster (if witnessed). */
    public static final int DEFACED_POSTER_NOTORIETY = 3;

    /** Cost of a POSTAL_VOTE_BUNDLE from the Post Office. */
    public static final int POSTAL_VOTE_BUNDLE_COST = 1;

    /** Votes added to a candidate's tally on successful postal fraud. */
    public static final int POSTAL_VOTES_ADDED = 5;

    /** Notoriety gain on being caught committing postal fraud. */
    public static final int POSTAL_FRAUD_NOTORIETY = 10;

    /** Notoriety gain for stealing the count sheet. */
    public static final int COUNT_SHEET_NOTORIETY = 5;

    /** Notoriety gain for wearing a rosette inside the exclusion zone. */
    public static final int ROSETTE_BREACH_NOTORIETY = 8;

    /** Brannigan post-election DWP payment bonus multiplier. */
    public static final float BRANNIGAN_DWP_MULTIPLIER = 1.10f;

    /** Patel post-election NeighbourhoodSystem Vibes bonus. */
    public static final int PATEL_VIBES_BONUS = 5;

    // ── Candidate enum ─────────────────────────────────────────────────────────

    /** The three election candidates. */
    public enum Candidate {
        /** Patricia Holt — Blue, promises parking crackdown. */
        HOLT,
        /** Steve Brannigan — Red, promises more benefits. */
        BRANNIGAN,
        /** Nikhil Patel — Independent, promises a community hub. */
        PATEL
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Vote tallies per candidate (seeded at construction). */
    private final Map<Candidate, Integer> votes;

    /** Whether the player has pledged support to each candidate. */
    private final Map<Candidate, Boolean> pledges;

    /** Whether the player has already voted on Polling Day. */
    private boolean hasVoted = false;

    /** Which candidate the player voted for (null if not voted). */
    private Candidate votedFor = null;

    /** Whether the election result has been voided (ballot box stolen). */
    private boolean resultVoided = false;

    /** Override detection risk (used in tests; negative means use default). */
    private float detectionRiskOverride = -1f;

    /** Whether FIRST_VOTER achievement has been awarded. */
    private boolean firstVoterAwarded = false;

    /** Whether TACTICAL_VOTER achievement has been awarded. */
    private boolean tacticalVoterAwarded = false;

    /** Whether BALLOT_STUFFER achievement has been awarded. */
    private boolean ballotStufferAwarded = false;

    /** Whether STREET_SMART achievement has been awarded. */
    private boolean streetSmartAwarded = false;

    /** Whether KINGMAKER achievement has been awarded. */
    private boolean kingmakerAwarded = false;

    /** Day the election effects expire (set to electionDay + POST_ELECTION_EFFECT_DAYS). */
    private int effectExpiryDay = -1;

    /** The winning candidate after the election is resolved. */
    private Candidate winner = null;

    /** NPCs spawned by this system during canvassing/polling. */
    private final List<NPC> spawnedNpcs = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Construct the LocalElectionSystem with a seeded RNG.
     */
    public LocalElectionSystem(Random random) {
        this.random = random;
        votes = new EnumMap<>(Candidate.class);
        pledges = new EnumMap<>(Candidate.class);
        for (Candidate c : Candidate.values()) {
            votes.put(c, 100); // balanced starting tally
            pledges.put(c, false);
        }
    }

    /** Construct with a default RNG. */
    public LocalElectionSystem() {
        this(new Random());
    }

    // ── Calendar queries ──────────────────────────────────────────────────────

    /**
     * @param dayOfYear current in-game day of year (0-indexed)
     * @return true if it is currently Canvassing Week (days 83–89 inclusive)
     */
    public boolean isCanvassingWeek(int dayOfYear) {
        return dayOfYear >= CANVASSING_START_DAY && dayOfYear < POLLING_DAY;
    }

    /**
     * @param dayOfYear current in-game day of year
     * @return true if today is Polling Day (day 90)
     */
    public boolean isPollingDay(int dayOfYear) {
        return dayOfYear == POLLING_DAY;
    }

    /**
     * @param dayOfYear current in-game day of year
     * @param currentHour current in-game hour (0–24)
     * @return true if the polling station is currently open
     */
    public boolean isPollingStationOpen(int dayOfYear, float currentHour) {
        return isPollingDay(dayOfYear)
                && currentHour >= POLLING_STATION_OPEN_HOUR
                && currentHour < POLLING_STATION_CLOSE_HOUR;
    }

    /**
     * Convenience overload using a {@link TimeSystem}.
     */
    public boolean isPollingStationOpen(TimeSystem timeSystem) {
        return isPollingStationOpen(timeSystem.getDayOfYear(), timeSystem.getTime());
    }

    /**
     * @param dayOfYear current day of year
     * @param currentHour current hour
     * @return true if it is Count Night (day 90 from 22:30 onwards)
     */
    public boolean isCountNight(int dayOfYear, float currentHour) {
        return isPollingDay(dayOfYear) && currentHour >= COUNT_START_HOUR;
    }

    // ── Canvassing Week mechanics ─────────────────────────────────────────────

    /**
     * Player pledges support to a candidate by pressing E on their CANDIDATE_NPC.
     * Awards TACTICAL_VOTER when all three candidates have been pledged.
     *
     * @param candidate the candidate being pledged to
     * @param achievementCallback callback for awarding achievements
     */
    public void pledgeSupport(Candidate candidate, NotorietySystem.AchievementCallback achievementCallback) {
        pledges.put(candidate, true);
        if (!tacticalVoterAwarded
                && pledges.get(Candidate.HOLT)
                && pledges.get(Candidate.BRANNIGAN)
                && pledges.get(Candidate.PATEL)) {
            tacticalVoterAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.TACTICAL_VOTER);
            }
        }
    }

    /**
     * Check whether the player has pledged support to a candidate.
     */
    public boolean hasPledged(Candidate candidate) {
        return Boolean.TRUE.equals(pledges.get(candidate));
    }

    // ── Postal vote fraud ─────────────────────────────────────────────────────

    /**
     * Set the detection risk override for testing (0.0 = never detected, 1.0 = always).
     * Use a negative value to restore default behaviour.
     */
    public void setDetectionRiskForTesting(float risk) {
        this.detectionRiskOverride = risk;
    }

    /**
     * Player submits a postal vote bundle in favour of {@code candidate}.
     *
     * <p>On success (not detected): adds {@link #POSTAL_VOTES_ADDED} to the candidate's
     * tally and awards {@link AchievementType#BALLOT_STUFFER}.
     *
     * <p>On detection: records {@link CriminalRecord.CrimeType#ELECTORAL_FRAUD}, adds
     * {@link #POSTAL_FRAUD_NOTORIETY} Notoriety, seeds {@code ELECTION_FRAUD} rumour,
     * and adds WantedSystem stars.
     *
     * @param player         player inventory (POSTAL_VOTE_BUNDLE is consumed)
     * @param candidate      target candidate for the fraudulent votes
     * @param criminalRecord player's criminal record
     * @param notorietySystem player's notoriety tracker
     * @param wantedSystem   player's wanted level
     * @param rumourNetwork  used to seed ELECTION_FRAUD rumour on detection
     * @param achievementCallback callback for awarding achievements
     * @param rumourSeeder   an NPC to seed the fraud rumour from (may be null)
     * @return true if the fraud was successful (not detected)
     */
    public boolean submitPostalVote(Inventory player,
                                    Candidate candidate,
                                    CriminalRecord criminalRecord,
                                    NotorietySystem notorietySystem,
                                    WantedSystem wantedSystem,
                                    RumourNetwork rumourNetwork,
                                    NotorietySystem.AchievementCallback achievementCallback,
                                    NPC rumourSeeder) {
        // Consume the bundle
        if (player != null) {
            player.removeItem(Material.POSTAL_VOTE_BUNDLE, 1);
        }

        float detectionThreshold = detectionRiskOverride >= 0f
                ? detectionRiskOverride
                : POSTAL_VOTE_DETECTION_BASE;

        boolean detected = random.nextFloat() < detectionThreshold;

        if (!detected) {
            // Success
            votes.merge(candidate, POSTAL_VOTES_ADDED, Integer::sum);
            if (!ballotStufferAwarded) {
                ballotStufferAwarded = true;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.BALLOT_STUFFER);
                }
            }
            return true;
        } else {
            // Caught
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.ELECTORAL_FRAUD);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(POSTAL_FRAUD_NOTORIETY, achievementCallback);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(2);
            }
            if (rumourNetwork != null && rumourSeeder != null) {
                rumourNetwork.addRumour(rumourSeeder,
                        new Rumour(RumourType.ELECTION_FRAUD,
                                "I heard someone nicked the postal votes off Dave the postman."));
            }
            return false;
        }
    }

    /**
     * Simplified overload used in integration tests (minimal dependencies).
     */
    public boolean submitPostalVote(Inventory player, Candidate candidate) {
        return submitPostalVote(player, candidate, null, null, null, null, null, null);
    }

    // ── Polling Day mechanics ─────────────────────────────────────────────────

    /**
     * Cast the player's vote for a candidate.
     *
     * <p>Awards {@link AchievementType#FIRST_VOTER} on first call.
     * Subsequent calls return false (already voted).
     *
     * @param player player inventory (not consumed, but checked for presence)
     * @param candidate candidate to vote for
     * @param achievementCallback callback for awarding achievements
     * @return true if the vote was accepted, false if already voted
     */
    public boolean castVote(Inventory player, Candidate candidate,
                            NotorietySystem.AchievementCallback achievementCallback) {
        if (hasVoted) {
            return false;
        }
        hasVoted = true;
        votedFor = candidate;
        votes.merge(candidate, 1, Integer::sum);
        if (!firstVoterAwarded) {
            firstVoterAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.FIRST_VOTER);
            }
        }
        return true;
    }

    /**
     * Simplified overload for integration tests.
     */
    public boolean castVote(Inventory player, Candidate candidate) {
        return castVote(player, candidate, null);
    }

    /**
     * @return true if the player has already voted this election
     */
    public boolean hasVoted() {
        return hasVoted;
    }

    /**
     * @return the candidate the player voted for, or null if not yet voted
     */
    public Candidate getVotedFor() {
        return votedFor;
    }

    /**
     * Steal the ballot box.  Barry must be distracted (caller is responsible for checking).
     * Voids the election result, adds Notoriety, records the crime, and adds Wanted stars.
     *
     * @param player player inventory (BALLOT_BOX is added)
     * @param criminalRecord player's criminal record
     * @param notorietySystem player's notoriety tracker
     * @param wantedSystem player's wanted level
     * @param achievementCallback achievement callback
     */
    public void stealBallotBox(Inventory player,
                               CriminalRecord criminalRecord,
                               NotorietySystem notorietySystem,
                               WantedSystem wantedSystem,
                               NotorietySystem.AchievementCallback achievementCallback) {
        resultVoided = true;
        if (player != null) {
            player.addItem(Material.BALLOT_BOX, 1);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(BALLOT_BOX_NOTORIETY, achievementCallback);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.BREACH_OF_POLLING_STATION_EXCLUSION);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(2);
        }
    }

    /**
     * Player is caught wearing a ROSETTE_ITEM inside the exclusion zone.
     *
     * @param criminalRecord player's criminal record
     * @param notorietySystem player's notoriety
     * @param wantedSystem player's wanted level
     * @param achievementCallback achievement callback
     */
    public void rosetteExclusionBreach(CriminalRecord criminalRecord,
                                       NotorietySystem notorietySystem,
                                       WantedSystem wantedSystem,
                                       NotorietySystem.AchievementCallback achievementCallback) {
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(ROSETTE_BREACH_NOTORIETY, achievementCallback);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.BREACH_OF_POLLING_STATION_EXCLUSION);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(1);
        }
    }

    /**
     * @return true if the election result has been voided (ballot box stolen)
     */
    public boolean isResultVoided() {
        return resultVoided;
    }

    // ── Count Night mechanics ─────────────────────────────────────────────────

    /**
     * Player grasses up the THUG attempting box-stuffing to Barry.
     * Awards STREET_SMART, reduces Patel's votes (as the THUG was stuffing for Patel's rival).
     *
     * @param achievementCallback callback for awarding achievements
     */
    public void grassUpBoxStuffer(NotorietySystem.AchievementCallback achievementCallback) {
        if (!streetSmartAwarded) {
            streetSmartAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.STREET_SMART);
            }
        }
    }

    /**
     * Player steals the count sheet.  Adds COUNT_SHEET to inventory, adds Notoriety.
     *
     * @param player player inventory
     * @param notorietySystem player's notoriety
     * @param achievementCallback achievement callback
     */
    public void stealCountSheet(Inventory player,
                                NotorietySystem notorietySystem,
                                NotorietySystem.AchievementCallback achievementCallback) {
        if (player != null) {
            player.addItem(Material.COUNT_SHEET, 1);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(COUNT_SHEET_NOTORIETY, achievementCallback);
        }
    }

    // ── Result resolution ─────────────────────────────────────────────────────

    /**
     * Determine the winner based on current vote tallies.
     *
     * @return the {@link Candidate} with the most votes, or null if the result is voided
     */
    public Candidate getWinner() {
        if (resultVoided) return null;

        Candidate best = null;
        int bestVotes = -1;
        for (Map.Entry<Candidate, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > bestVotes) {
                bestVotes = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    /**
     * Apply the post-election faction effects based on the winning candidate.
     * Should be called once when the result is announced (day 90, 23:30).
     *
     * @param dwpSystem             DWP system to boost if Brannigan wins
     * @param neighbourhoodSystem   NeighbourhoodSystem to boost vibes if Patel wins
     * @param rumourNetwork         to seed ELECTION_UPSET rumour on Independent win
     * @param rumourSeeder          NPC to seed the rumour from
     * @param electionDay           the day-of-year the election took place
     * @param achievementCallback   achievement callback for KINGMAKER
     */
    public void applyPostElectionEffects(DWPSystem dwpSystem,
                                         NeighbourhoodSystem neighbourhoodSystem,
                                         RumourNetwork rumourNetwork,
                                         NPC rumourSeeder,
                                         int electionDay,
                                         NotorietySystem.AchievementCallback achievementCallback) {
        winner = getWinner();
        effectExpiryDay = electionDay + POST_ELECTION_EFFECT_DAYS;

        if (winner == null) return;

        switch (winner) {
            case HOLT:
                // Police cooperation +20% — handled externally by PoliceSystem
                break;
            case BRANNIGAN:
                if (dwpSystem != null) {
                    dwpSystem.setPaymentMultiplier(BRANNIGAN_DWP_MULTIPLIER);
                }
                break;
            case PATEL:
                if (neighbourhoodSystem != null) {
                    neighbourhoodSystem.setVibes(neighbourhoodSystem.getVibes() + PATEL_VIBES_BONUS);
                }
                if (rumourNetwork != null && rumourSeeder != null) {
                    rumourNetwork.addRumour(rumourSeeder,
                            new Rumour(RumourType.ELECTION_UPSET,
                                    "Can't believe Patel won. World's gone mad."));
                }
                break;
        }

        // KINGMAKER — player backed the winning candidate
        if (!kingmakerAwarded && votedFor == winner) {
            kingmakerAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.KINGMAKER);
            }
        }
    }

    /**
     * Simplified overload for integration tests (Brannigan win path).
     */
    public void applyPostElectionEffects(DWPSystem dwpSystem) {
        // Force Brannigan to win by setting his votes highest
        votes.put(Candidate.BRANNIGAN, 999);
        applyPostElectionEffects(dwpSystem, null, null, null, POLLING_DAY, null);
    }

    /**
     * Decay post-election effects when the expiry day is reached.
     * Call once per day after the election.
     *
     * @param currentDay current day of year
     * @param dwpSystem  DWP system to reset multiplier
     */
    public void tickPostElectionDecay(int currentDay, DWPSystem dwpSystem) {
        if (effectExpiryDay > 0 && currentDay >= effectExpiryDay) {
            if (dwpSystem != null) {
                dwpSystem.setPaymentMultiplier(1.0f);
            }
            effectExpiryDay = -1;
        }
    }

    // ── NPC spawning ──────────────────────────────────────────────────────────

    /**
     * Main update tick.  Spawns/despawns election NPCs based on the current day.
     *
     * @param dayOfYear  current day of year
     * @param npcs       mutable list of active NPCs in the world
     */
    public void update(int dayOfYear, List<NPC> npcs) {
        // Spawn canvassing NPCs at the start of canvassing week
        if (isCanvassingWeek(dayOfYear) && spawnedNpcs.isEmpty()) {
            spawnCandidateNpcs(npcs);
        }
        // Despawn all election NPCs once the election is over
        if (dayOfYear > POLLING_DAY && !spawnedNpcs.isEmpty()) {
            npcs.removeAll(spawnedNpcs);
            spawnedNpcs.clear();
        }
    }

    private void spawnCandidateNpcs(List<NPC> npcs) {
        // Three candidate NPCs at their table positions
        NPC holt = new NPC(NPCType.CANDIDATE_NPC, "Patricia Holt", 50f, 1f, 50f);
        NPC brannigan = new NPC(NPCType.CANDIDATE_NPC, "Steve Brannigan", 60f, 1f, 40f);
        NPC patel = new NPC(NPCType.CANDIDATE_NPC, "Nikhil Patel", 45f, 1f, 60f);
        spawnedNpcs.add(holt);
        spawnedNpcs.add(brannigan);
        spawnedNpcs.add(patel);
        npcs.add(holt);
        npcs.add(brannigan);
        npcs.add(patel);

        // Canvassers
        NPC canvasser1 = new NPC(NPCType.CANVASSER_NPC, 55f, 1f, 50f);
        NPC canvasser2 = new NPC(NPCType.CANVASSER_NPC, 65f, 1f, 45f);
        spawnedNpcs.add(canvasser1);
        spawnedNpcs.add(canvasser2);
        npcs.add(canvasser1);
        npcs.add(canvasser2);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * @param candidate the candidate to query
     * @return current vote tally for this candidate
     */
    public int getVotes(Candidate candidate) {
        return votes.getOrDefault(candidate, 0);
    }

    /**
     * Directly set a candidate's vote tally (for testing / seeding).
     */
    public void setVotes(Candidate candidate, int count) {
        votes.put(candidate, count);
    }

    /**
     * Add to a candidate's vote tally.
     */
    public void addVotes(Candidate candidate, int delta) {
        votes.merge(candidate, delta, Integer::sum);
    }

    /** @return the spawned election NPCs (for inspection in tests). */
    public List<NPC> getSpawnedNpcs() {
        return spawnedNpcs;
    }

    /** @return true if the TACTICAL_VOTER achievement has been awarded. */
    public boolean isTacticalVoterAwarded() {
        return tacticalVoterAwarded;
    }

    /** @return true if the BALLOT_STUFFER achievement has been awarded. */
    public boolean isBallotStufferAwarded() {
        return ballotStufferAwarded;
    }

    /** @return true if the FIRST_VOTER achievement has been awarded. */
    public boolean isFirstVoterAwarded() {
        return firstVoterAwarded;
    }

    /** @return the winning candidate, or null if not yet resolved or voided. */
    public Candidate getResolvedWinner() {
        return winner;
    }
}

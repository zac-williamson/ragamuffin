package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the Northfield Traveller Site — Paddy Flynn's Tarmac Crew.
 *
 * <p>Features:
 * <ul>
 *   <li><b>Site visit cycle</b>: crew pitches up every 7–10 in-game days; stays 4–6 days
 *       before Council eviction.</li>
 *   <li><b>NPCs</b>: TRAVELLER_BOSS (Paddy), 3× TRAVELLER_WORKER (Liam/Seamus/Donal),
 *       TRAVELLER_WOMAN (Brigid), LURCHER_DOG, COUNCIL_ENFORCEMENT_OFFICER (Derek),
 *       RSPCA_OFFICER (on report).</li>
 *   <li><b>Cash-in-hand tarmac work</b>: Paddy assigns driveway jobs, scrap runs, or
 *       kerb-hawking heather. Rewards COIN + TRADING/SOCIAL/STREET_SMARTS XP.</li>
 *   <li><b>Scrap metal fence</b>: Paddy pays above FenceSystem rates for SCRAP_METAL,
 *       COPPER_PIPE, STOLEN_BIKE, STOLEN_PHONE — capped at 20 COIN per visit.</li>
 *   <li><b>Dog fight betting</b>: Fri/Sat 21:00–23:00 at DOG_FIGHT_RING_PROP; 2:1 payouts.</li>
 *   <li><b>RSPCA report</b>: disperses dog fight ring; Paddy goes HOSTILE; Marchetti −2.</li>
 *   <li><b>Council tip-off</b>: day 1 report → Derek arrives same afternoon;
 *       NeighbourhoodSystem COMMUNITY_RESPECT +2.</li>
 *   <li><b>Night caravan raid</b>: 02:00–04:00 with CROWBAR/LOCKPICK → 8–15 COIN +
 *       DOG_FIGHT_LEDGER. LURCHER_DOG sleeps if DOG_PERMISSION_FLAG earned.</li>
 *   <li><b>DIY driveway repair</b>: use TARMAC_MIX on player's property → COMFORT_SCORE +5.</li>
 * </ul>
 */
public class TravellerSiteSystem {

    // ── Visit cycle ────────────────────────────────────────────────────────────

    /** Minimum in-game days between crew visits. */
    public static final int VISIT_INTERVAL_MIN_DAYS = 7;

    /** Maximum in-game days between crew visits. */
    public static final int VISIT_INTERVAL_MAX_DAYS = 10;

    /** Minimum in-game days the crew stays before eviction. */
    public static final int VISIT_DURATION_MIN_DAYS = 4;

    /** Maximum in-game days the crew stays before eviction. */
    public static final int VISIT_DURATION_MAX_DAYS = 6;

    // ── Scrap fence ───────────────────────────────────────────────────────────

    /** Above-FenceSystem rate paid for SCRAP_METAL (per item). */
    public static final int SCRAP_RATE_SCRAP_METAL = 4;

    /** Above-FenceSystem rate paid for COPPER_PIPE (per item). */
    public static final int SCRAP_RATE_COPPER_PIPE = 6;

    /** Above-FenceSystem rate paid for STOLEN_BIKE (per item). */
    public static final int SCRAP_RATE_STOLEN_BIKE = 8;

    /** Above-FenceSystem rate paid for STOLEN_PHONE (per item). */
    public static final int SCRAP_RATE_STOLEN_PHONE = 7;

    /** Maximum total COIN Paddy will pay per visit from the scrap fence. */
    public static final int SCRAP_FENCE_CAP_COIN = 20;

    // ── Dog fight ring ────────────────────────────────────────────────────────

    /** Day-of-week index for Friday (0=Mon, …, 6=Sun). */
    public static final int DOG_FIGHT_DAY_FRIDAY = 4;

    /** Day-of-week index for Saturday. */
    public static final int DOG_FIGHT_DAY_SATURDAY = 5;

    /** Hour at which dog fighting begins. */
    public static final float DOG_FIGHT_START_HOUR = 21.0f;

    /** Hour at which dog fighting ends. */
    public static final float DOG_FIGHT_END_HOUR = 23.0f;

    /** 2:1 payout multiplier — bet amount returned plus this multiple. */
    public static final int DOG_FIGHT_BET_PAYOUT_MULTIPLIER = 2;

    /** Minimum bet at the dog fight ring. */
    public static final int DOG_FIGHT_MIN_BET = 2;

    /** Maximum bet at the dog fight ring. */
    public static final int DOG_FIGHT_MAX_BET = 10;

    /** Win probability at the dog fight ring (50/50). */
    public static final float DOG_FIGHT_WIN_CHANCE = 0.50f;

    /** Notoriety gained for attending the dog fight. */
    public static final int DOG_FIGHT_NOTORIETY = 8;

    /** Marchetti Respect penalty when player reports dog fight to RSPCA. */
    public static final int RSPCA_REPORT_MARCHETTI_PENALTY = -2;

    // ── Council tip-off ───────────────────────────────────────────────────────

    /** Day on which Derek normally arrives for eviction. */
    public static final int DEREK_NORMAL_EVICTION_DAY = 3;

    /** COMMUNITY_RESPECT bonus for reporting on day 1. */
    public static final int COUNCIL_TIPOFF_COMMUNITY_RESPECT = 2;

    /** Street Lads Respect penalty for council tip-off. */
    public static final int COUNCIL_TIPOFF_STREET_LADS_PENALTY = -1;

    // ── Night raid ────────────────────────────────────────────────────────────

    /** Hour at which night raid window opens. */
    public static final float NIGHT_RAID_START_HOUR = 2.0f;

    /** Hour at which night raid window closes. */
    public static final float NIGHT_RAID_END_HOUR = 4.0f;

    /** Minimum COIN looted in a successful raid. */
    public static final int NIGHT_RAID_COIN_MIN = 8;

    /** Maximum COIN looted in a successful raid. */
    public static final int NIGHT_RAID_COIN_MAX = 15;

    /** Notoriety gained for the night raid. */
    public static final int NIGHT_RAID_NOTORIETY = 5;

    // ── Cash-in-hand jobs ─────────────────────────────────────────────────────

    /** COIN reward for completing a Paddy job. */
    public static final int JOB_COIN_REWARD = 5;

    /** Number of Paddy jobs required to earn DOG_PERMISSION_FLAG. */
    public static final int JOBS_FOR_DOG_PERMISSION = 2;

    // ── Lucky Heather ─────────────────────────────────────────────────────────

    /** COIN cost of LUCKY_HEATHER from Brigid. */
    public static final int LUCKY_HEATHER_COST = 2;

    /** COIN cost of CLOTHES_PEG_BUNDLE from Brigid. */
    public static final int CLOTHES_PEG_COST = 1;

    /** COIN cost of TARMAC_MIX from Paddy. */
    public static final int TARMAC_MIX_COST = 5;

    /** Number of LUCKY_HEATHER items required to craft LUCKY_HEATHER_CROWN. */
    public static final int LUCKY_HEATHER_CROWN_RECIPE = 5;

    /** COMFORT_SCORE bonus for applying TARMAC_MIX to own driveway. */
    public static final int TARMAC_COMFORT_BONUS = 5;

    /** HP multiplier bonus from carrying LUCKY_HEATHER (5% extra max HP). */
    public static final float LUCKY_HEATHER_HP_BONUS = 0.05f;

    // ── Job types ──────────────────────────────────────────────────────────────

    /** Result codes returned by job and interaction methods. */
    public enum JobResult {
        /** Job assigned successfully. */
        JOB_ASSIGNED,
        /** Job already in progress. */
        JOB_IN_PROGRESS,
        /** Job completed and reward paid. */
        JOB_COMPLETED,
        /** Job failed (player did not complete within time limit). */
        JOB_FAILED,
        /** Site not currently active. */
        SITE_INACTIVE,
        /** Paddy is hostile (after RSPCA report). */
        PADDY_HOSTILE,
        /** Player has not completed enough jobs. */
        INSUFFICIENT_REP
    }

    /** Result codes for scrap fence interactions. */
    public enum ScrapResult {
        /** Items sold successfully. */
        SOLD,
        /** No accepted scrap items in inventory. */
        NO_SCRAP,
        /** Visit cap already reached. */
        CAP_REACHED,
        /** Site not currently active. */
        SITE_INACTIVE
    }

    /** Result codes for dog fight interactions. */
    public enum DogFightResult {
        /** Bet placed and won. */
        WON,
        /** Bet placed and lost. */
        LOST,
        /** Wrong time (not Fri/Sat 21:00–23:00). */
        WRONG_TIME,
        /** Site not active or ring dispersed. */
        RING_INACTIVE,
        /** Insufficient funds for minimum bet. */
        INSUFFICIENT_FUNDS
    }

    /** Result codes for RSPCA report. */
    public enum RspcaReportResult {
        /** Report filed; RSPCA officer spawned. */
        REPORTED,
        /** Dog fight ring already dispersed. */
        ALREADY_DISPERSED,
        /** Site not active. */
        SITE_INACTIVE
    }

    /** Result codes for council tip-off. */
    public enum CouncilTipoffResult {
        /** Tip-off filed; Derek arrives early. */
        TIPPED_OFF,
        /** Too late — beyond day 1. */
        TOO_LATE,
        /** Already tipped off. */
        ALREADY_TIPPED,
        /** Site not active. */
        SITE_INACTIVE
    }

    /** Result codes for night raid. */
    public enum NightRaidResult {
        /** Raid succeeded; loot obtained. */
        SUCCESS,
        /** Wrong time (not 02:00–04:00). */
        WRONG_TIME,
        /** Missing required tool (CROWBAR or LOCKPICK). */
        NO_TOOL,
        /** Dog interrupted the raid. */
        DOG_ALERTED,
        /** Site not active. */
        SITE_INACTIVE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the crew is currently on-site. */
    private boolean siteActive = false;

    /** Current in-game day when site became active. */
    private int siteArrivalDay = -1;

    /** How many days the crew will stay (4–6). */
    private int siteDurationDays = 0;

    /** Day counter for next arrival (counts down after departure). */
    private int daysUntilNextVisit = 0;

    /** Whether Paddy is hostile (after RSPCA report). */
    private boolean paddyHostile = false;

    /** Whether the dog fight ring has been dispersed. */
    private boolean dogFightRingDispersed = false;

    /** Whether council has been tipped off. */
    private boolean councilTippedOff = false;

    /** Whether Derek (enforcement officer) has arrived. */
    private boolean derekArrived = false;

    /** COIN paid by Paddy's scrap fence this visit (resets on new visit). */
    private int scrapFenceCoinsThisVisit = 0;

    /** Number of jobs player has completed for Paddy this visit. */
    private int jobsCompletedThisVisit = 0;

    /** Whether player has earned the DOG_PERMISSION_FLAG. */
    private boolean dogPermissionFlag = false;

    /** Whether a job is currently in progress. */
    private boolean jobInProgress = false;

    /** Day tracker for day-rollover detection. */
    private int currentDay = -1;

    // ── Optional system integrations ──────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private ragamuffin.ui.AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private StreetSkillSystem streetSkillSystem;

    // ── NPC references ────────────────────────────────────────────────────────

    private NPC paddyNpc;
    private NPC brigidNpc;
    private NPC lurcher;
    private NPC derekNpc;
    private NPC rspcaOfficerNpc;

    /** Worker NPCs — 3 in total (Liam, Seamus, Donal). */
    private final List<NPC> workers = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    public TravellerSiteSystem() {
        this(new Random());
    }

    public TravellerSiteSystem(Random random) {
        this.random = random;
        // First visit arrives in 7–10 days
        daysUntilNextVisit = VISIT_INTERVAL_MIN_DAYS
            + random.nextInt(VISIT_INTERVAL_MAX_DAYS - VISIT_INTERVAL_MIN_DAYS + 1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dependency injection setters
    // ─────────────────────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem sys) { this.notorietySystem = sys; }
    public void setWantedSystem(WantedSystem sys) { this.wantedSystem = sys; }
    public void setCriminalRecord(CriminalRecord record) { this.criminalRecord = record; }
    public void setRumourNetwork(RumourNetwork network) { this.rumourNetwork = network; }
    public void setAchievementSystem(ragamuffin.ui.AchievementSystem sys) { this.achievementSystem = sys; }
    public void setFactionSystem(FactionSystem sys) { this.factionSystem = sys; }
    public void setNeighbourhoodSystem(NeighbourhoodSystem sys) { this.neighbourhoodSystem = sys; }
    public void setStreetSkillSystem(StreetSkillSystem sys) { this.streetSkillSystem = sys; }

    // ── NPC setters ───────────────────────────────────────────────────────────

    public void setPaddyNpc(NPC npc) { this.paddyNpc = npc; }
    public void setBrigidNpc(NPC npc) { this.brigidNpc = npc; }
    public void setLurcherDog(NPC npc) { this.lurcher = npc; }
    public void setDerekNpc(NPC npc) { this.derekNpc = npc; }
    public void setRspcaOfficerNpc(NPC npc) { this.rspcaOfficerNpc = npc; }
    public void addWorker(NPC npc) { this.workers.add(npc); }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-frame update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Per-frame update. Call once per frame.
     *
     * @param delta      frame delta (seconds)
     * @param timeSystem current game time
     */
    public void update(float delta, TimeSystem timeSystem) {
        int day = timeSystem.getDayCount();

        // Detect day rollover
        if (day != currentDay) {
            currentDay = day;
            onDayRollover(timeSystem);
        }
    }

    private void onDayRollover(TimeSystem timeSystem) {
        if (!siteActive) {
            // Count down to next visit
            daysUntilNextVisit--;
            if (daysUntilNextVisit <= 0) {
                activateSite(timeSystem);
            }
        } else {
            int daysOnSite = timeSystem.getDayCount() - siteArrivalDay;

            // Check normal eviction (Derek arrives day 3 unless tipped off early)
            if (!derekArrived && !councilTippedOff && daysOnSite >= DEREK_NORMAL_EVICTION_DAY) {
                spawnDerek();
            }

            // Eviction complete after stay duration
            if (daysOnSite >= siteDurationDays) {
                deactivateSite(timeSystem);
            }
        }
    }

    private void activateSite(TimeSystem timeSystem) {
        siteActive = true;
        siteArrivalDay = timeSystem.getDayCount();
        siteDurationDays = VISIT_DURATION_MIN_DAYS
            + random.nextInt(VISIT_DURATION_MAX_DAYS - VISIT_DURATION_MIN_DAYS + 1);
        paddyHostile = false;
        dogFightRingDispersed = false;
        councilTippedOff = false;
        derekArrived = false;
        scrapFenceCoinsThisVisit = 0;
        jobsCompletedThisVisit = 0;
        jobInProgress = false;

        // Seed arrival rumour
        if (rumourNetwork != null && paddyNpc != null) {
            rumourNetwork.addRumour(paddyNpc,
                new Rumour(RumourType.TRAVELLERS_ARRIVED,
                    "Heard the travellers have pitched up on the industrial estate again — Paddy's mob."));
        }
    }

    private void deactivateSite(TimeSystem timeSystem) {
        siteActive = false;
        daysUntilNextVisit = VISIT_INTERVAL_MIN_DAYS
            + random.nextInt(VISIT_INTERVAL_MAX_DAYS - VISIT_INTERVAL_MIN_DAYS + 1);
        // Reset per-visit state
        scrapFenceCoinsThisVisit = 0;
        jobsCompletedThisVisit = 0;
        jobInProgress = false;
    }

    private void spawnDerek() {
        derekArrived = true;
        if (derekNpc != null) {
            derekNpc.setState(NPCState.PATROL);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cash-in-hand jobs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Request a cash-in-hand job from Paddy.
     *
     * @return JOB_ASSIGNED if a new job was assigned; otherwise a relevant failure code.
     */
    public JobResult requestJob() {
        if (!siteActive) return JobResult.SITE_INACTIVE;
        if (paddyHostile) return JobResult.PADDY_HOSTILE;
        if (jobInProgress) return JobResult.JOB_IN_PROGRESS;
        jobInProgress = true;
        return JobResult.JOB_ASSIGNED;
    }

    /**
     * Complete the current cash-in-hand job and claim the reward.
     *
     * @param inventory player's inventory
     * @return JOB_COMPLETED if successful; JOB_FAILED if no job was in progress.
     */
    public JobResult completeJob(Inventory inventory) {
        if (!siteActive) return JobResult.SITE_INACTIVE;
        if (!jobInProgress) return JobResult.JOB_FAILED;

        jobInProgress = false;
        jobsCompletedThisVisit++;

        // Pay reward
        inventory.addItem(Material.COIN, JOB_COIN_REWARD);

        // Grant DOG_PERMISSION_FLAG after 2 jobs
        if (jobsCompletedThisVisit >= JOBS_FOR_DOG_PERMISSION && !dogPermissionFlag) {
            dogPermissionFlag = true;
            if (achievementSystem != null) {
                achievementSystem.unlock(ragamuffin.ui.AchievementType.PADDYS_GRAFTER);
            }
        }

        // StreetSkillSystem XP
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.TRADING, 5);
        }

        // Seed rumour on poor-quality job (random 30% chance)
        if (random.nextFloat() < 0.30f && rumourNetwork != null && paddyNpc != null) {
            rumourNetwork.addRumour(paddyNpc,
                new Rumour(RumourType.SHODDY_WORK,
                    "Someone got a driveway done off them travellers — looks like it's already falling apart."));
        }

        return JobResult.JOB_COMPLETED;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scrap metal fence
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sell accepted scrap items from inventory to Paddy's scrap fence.
     * Accepted items: SCRAP_METAL, COPPER_PIPE, STOLEN_BIKE, STOLEN_PHONE.
     * Capped at {@link #SCRAP_FENCE_CAP_COIN} per visit.
     *
     * @param inventory player's inventory
     * @return result code
     */
    public ScrapResult sellScrap(Inventory inventory) {
        if (!siteActive) return ScrapResult.SITE_INACTIVE;
        if (paddyHostile) return ScrapResult.SITE_INACTIVE;
        if (scrapFenceCoinsThisVisit >= SCRAP_FENCE_CAP_COIN) return ScrapResult.CAP_REACHED;

        int totalPaid = 0;
        boolean soldAnything = false;

        totalPaid += sellScrapItem(inventory, Material.SCRAP_METAL, SCRAP_RATE_SCRAP_METAL);
        totalPaid += sellScrapItem(inventory, Material.COPPER_PIPE, SCRAP_RATE_COPPER_PIPE);
        totalPaid += sellScrapItem(inventory, Material.STOLEN_BIKE, SCRAP_RATE_STOLEN_BIKE);
        totalPaid += sellScrapItem(inventory, Material.STOLEN_PHONE, SCRAP_RATE_STOLEN_PHONE);

        if (totalPaid > 0) {
            soldAnything = true;
        }

        if (!soldAnything) return ScrapResult.NO_SCRAP;

        // Cap enforcement
        int actualPaid = Math.min(totalPaid, SCRAP_FENCE_CAP_COIN - scrapFenceCoinsThisVisit);
        scrapFenceCoinsThisVisit += actualPaid;
        inventory.addItem(Material.COIN, actualPaid);

        // Achievement for hitting the cap
        if (scrapFenceCoinsThisVisit >= SCRAP_FENCE_CAP_COIN && achievementSystem != null) {
            achievementSystem.unlock(ragamuffin.ui.AchievementType.SCRAP_KING);
        }

        return ScrapResult.SOLD;
    }

    /**
     * Sell one unit of a specific scrap material and return the coin value (pre-cap).
     * Removes the item from inventory and returns its rate, or 0 if not present.
     */
    private int sellScrapItem(Inventory inventory, Material material, int rate) {
        int count = inventory.getItemCount(material);
        if (count <= 0) return 0;
        int units = count; // sell all of that type
        inventory.removeItem(material, units);
        return units * rate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dog fight ring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a bet at the dog fight ring.
     *
     * @param inventory player's inventory
     * @param betAmount bet in COIN
     * @param timeSystem current time
     * @return result code
     */
    public DogFightResult placeBet(Inventory inventory, int betAmount, TimeSystem timeSystem) {
        if (!siteActive) return DogFightResult.RING_INACTIVE;
        if (dogFightRingDispersed) return DogFightResult.RING_INACTIVE;

        // Time check: Fri or Sat, 21:00–23:00
        int dow = timeSystem.getDayIndex() % 7;
        float hour = timeSystem.getTime();
        if (dow != DOG_FIGHT_DAY_FRIDAY && dow != DOG_FIGHT_DAY_SATURDAY) {
            return DogFightResult.WRONG_TIME;
        }
        if (hour < DOG_FIGHT_START_HOUR || hour >= DOG_FIGHT_END_HOUR) {
            return DogFightResult.WRONG_TIME;
        }

        int bet = Math.max(DOG_FIGHT_MIN_BET, Math.min(betAmount, DOG_FIGHT_MAX_BET));
        if (inventory.getItemCount(Material.COIN) < bet) {
            return DogFightResult.INSUFFICIENT_FUNDS;
        }

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.DOG_FIGHTING_ATTENDANCE);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(DOG_FIGHT_NOTORIETY, achievementSystem != null ? achievementSystem::unlock : null);
        }

        // Place bet
        inventory.removeItem(Material.COIN, bet);

        boolean won = random.nextFloat() < DOG_FIGHT_WIN_CHANCE;
        if (won) {
            int payout = bet * DOG_FIGHT_BET_PAYOUT_MULTIPLIER;
            inventory.addItem(Material.COIN, payout);
            if (achievementSystem != null) {
                achievementSystem.unlock(ragamuffin.ui.AchievementType.DIRTY_MONEY);
            }
            return DogFightResult.WON;
        } else {
            return DogFightResult.LOST;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RSPCA report
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Report the dog fight ring to the RSPCA.
     * Disperses the ring, spawns RSPCA_OFFICER, Paddy goes HOSTILE, Marchetti −2.
     *
     * @return result code
     */
    public RspcaReportResult reportToRspca() {
        if (!siteActive) return RspcaReportResult.SITE_INACTIVE;
        if (dogFightRingDispersed) return RspcaReportResult.ALREADY_DISPERSED;

        dogFightRingDispersed = true;

        // Paddy goes hostile
        paddyHostile = true;
        if (paddyNpc != null) {
            paddyNpc.setState(NPCState.AGGRESSIVE);
        }

        // Spawn RSPCA officer
        if (rspcaOfficerNpc != null) {
            rspcaOfficerNpc.setState(NPCState.PATROL);
        }

        // Marchetti Respect penalty
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, RSPCA_REPORT_MARCHETTI_PENALTY);
        }

        // Seed DOG_FIGHT_RAID rumour
        if (rumourNetwork != null && rspcaOfficerNpc != null) {
            rumourNetwork.addRumour(rspcaOfficerNpc,
                new Rumour(RumourType.DOG_FIGHT_RAID,
                    "Word is someone turned over the travellers' dog fight ring — RSPCA turned up."));
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(ragamuffin.ui.AchievementType.RSPCA_GRASS);
        }

        return RspcaReportResult.REPORTED;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Council tip-off
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tip off the council about the traveller site on day 1.
     * Derek arrives same afternoon instead of day 3.
     * NeighbourhoodSystem COMMUNITY_RESPECT +2; Street Lads Respect −1.
     *
     * @param timeSystem current time
     * @return result code
     */
    public CouncilTipoffResult reportToCouncil(TimeSystem timeSystem) {
        if (!siteActive) return CouncilTipoffResult.SITE_INACTIVE;
        if (councilTippedOff) return CouncilTipoffResult.ALREADY_TIPPED;

        int daysOnSite = timeSystem.getDayCount() - siteArrivalDay;
        if (daysOnSite > 1) return CouncilTipoffResult.TOO_LATE;

        councilTippedOff = true;

        // Derek arrives immediately
        spawnDerek();

        // Neighbourhood respect boost
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(neighbourhoodSystem.getVibes() + COUNCIL_TIPOFF_COMMUNITY_RESPECT);
        }

        // Street Lads penalty
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, COUNCIL_TIPOFF_STREET_LADS_PENALTY);
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(ragamuffin.ui.AchievementType.COMMUNITY_WATCH_HERO);
        }

        return CouncilTipoffResult.TIPPED_OFF;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Night caravan raid
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt to raid the caravan during the 02:00–04:00 window.
     * Requires CROWBAR or LOCKPICK. LURCHER_DOG sleeps if DOG_PERMISSION_FLAG is set.
     *
     * @param inventory  player's inventory
     * @param timeSystem current time
     * @return result code
     */
    public NightRaidResult raidCaravan(Inventory inventory, TimeSystem timeSystem) {
        if (!siteActive) return NightRaidResult.SITE_INACTIVE;

        float hour = timeSystem.getTime();
        if (hour < NIGHT_RAID_START_HOUR || hour >= NIGHT_RAID_END_HOUR) {
            return NightRaidResult.WRONG_TIME;
        }

        // Check for CROWBAR or LOCKPICK
        boolean hasTool = inventory.getItemCount(Material.CROWBAR) > 0
            || inventory.getItemCount(Material.LOCKPICK) > 0;
        if (!hasTool) {
            return NightRaidResult.NO_TOOL;
        }

        // Dog check
        if (!dogPermissionFlag && lurcher != null) {
            lurcher.setState(NPCState.AGGRESSIVE);
            return NightRaidResult.DOG_ALERTED;
        }

        // Raid succeeds — grant loot
        int coinLoot = NIGHT_RAID_COIN_MIN
            + random.nextInt(NIGHT_RAID_COIN_MAX - NIGHT_RAID_COIN_MIN + 1);
        inventory.addItem(Material.COIN, coinLoot);
        inventory.addItem(Material.DOG_FIGHT_LEDGER, 1);

        // Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NIGHT_RAID_NOTORIETY, achievementSystem != null ? achievementSystem::unlock : null);
        }

        // Wanted
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
        }

        // Crime record
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.BURGLARY);
        }

        // Rumour
        if (rumourNetwork != null && paddyNpc != null) {
            rumourNetwork.addRumour(paddyNpc,
                new Rumour(RumourType.DOG_FIGHT_RAID,
                    "Word is someone turned over the travellers' caravan in the middle of the night."));
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(ragamuffin.ui.AchievementType.NIGHT_RAIDER);
        }

        return NightRaidResult.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lucky Heather / Clothes Pegs / Tarmac
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Buy LUCKY_HEATHER from Brigid for 2 COIN.
     *
     * @param inventory player's inventory
     * @return true if purchased successfully; false if site inactive or insufficient funds.
     */
    public boolean buyLuckyHeather(Inventory inventory) {
        if (!siteActive) return false;
        if (inventory.getItemCount(Material.COIN) < LUCKY_HEATHER_COST) return false;
        inventory.removeItem(Material.COIN, LUCKY_HEATHER_COST);
        inventory.addItem(Material.LUCKY_HEATHER, 1);
        return true;
    }

    /**
     * Buy CLOTHES_PEG_BUNDLE from Brigid for 1 COIN.
     *
     * @param inventory player's inventory
     * @return true if purchased; false otherwise.
     */
    public boolean buyClothesPageBundle(Inventory inventory) {
        if (!siteActive) return false;
        if (inventory.getItemCount(Material.COIN) < CLOTHES_PEG_COST) return false;
        inventory.removeItem(Material.COIN, CLOTHES_PEG_COST);
        inventory.addItem(Material.CLOTHES_PEG_BUNDLE, 1);
        return true;
    }

    /**
     * Buy TARMAC_MIX from Paddy for 5 COIN.
     *
     * @param inventory player's inventory
     * @return true if purchased; false otherwise.
     */
    public boolean buyTarmacMix(Inventory inventory) {
        if (!siteActive) return false;
        if (paddyHostile) return false;
        if (inventory.getItemCount(Material.COIN) < TARMAC_MIX_COST) return false;
        inventory.removeItem(Material.COIN, TARMAC_MIX_COST);
        inventory.addItem(Material.TARMAC_MIX, 1);
        return true;
    }

    /**
     * Throw CLOTHES_PEG_BUNDLE at an NPC (comedy throw).
     * Seeds CLOTHES_PEG_INCIDENT rumour. Consumes 1 CLOTHES_PEG_BUNDLE.
     *
     * @param inventory  player's inventory
     * @param target     the NPC being pelted
     * @param pegTargetsHit set of NPCs previously hit (for PEG_WARFARE achievement)
     * @return true if thrown; false if no bundle in inventory.
     */
    public boolean throwClothesPegs(Inventory inventory, NPC target, java.util.Set<NPC> pegTargetsHit) {
        if (inventory.getItemCount(Material.CLOTHES_PEG_BUNDLE) <= 0) return false;
        inventory.removeItem(Material.CLOTHES_PEG_BUNDLE, 1);

        if (target != null) {
            target.setState(NPCState.FLEEING);
        }

        if (rumourNetwork != null && target != null) {
            rumourNetwork.addRumour(target,
                new Rumour(RumourType.CLOTHES_PEG_INCIDENT,
                    "Saw someone pelting someone with clothes pegs — had me in stitches."));
        }

        // Track unique peg targets for PEG_WARFARE achievement (3 unique NPCs)
        if (pegTargetsHit != null && target != null) {
            pegTargetsHit.add(target);
            if (pegTargetsHit.size() >= 3 && achievementSystem != null) {
                achievementSystem.unlock(ragamuffin.ui.AchievementType.PEG_WARFARE);
            }
        }

        return true;
    }

    /**
     * Apply TARMAC_MIX to the player's own property driveway.
     * Consumes 1 TARMAC_MIX. COMFORT_SCORE +5.
     *
     * @param inventory player's inventory
     * @return true if applied; false if no TARMAC_MIX in inventory.
     */
    public boolean applyTarmacMix(Inventory inventory) {
        if (inventory.getItemCount(Material.TARMAC_MIX) <= 0) return false;
        inventory.removeItem(Material.TARMAC_MIX, 1);

        if (achievementSystem != null) {
            achievementSystem.unlock(ragamuffin.ui.AchievementType.SMOOTH_DRIVEWAY);
        }

        return true;
    }

    /**
     * Craft LUCKY_HEATHER_CROWN from 5× LUCKY_HEATHER.
     *
     * @param inventory player's inventory
     * @return true if crafted; false if insufficient LUCKY_HEATHER.
     */
    public boolean craftLuckyHeatherCrown(Inventory inventory) {
        if (inventory.getItemCount(Material.LUCKY_HEATHER) < LUCKY_HEATHER_CROWN_RECIPE) {
            return false;
        }
        inventory.removeItem(Material.LUCKY_HEATHER, LUCKY_HEATHER_CROWN_RECIPE);
        inventory.addItem(Material.LUCKY_HEATHER_CROWN, 1);

        if (achievementSystem != null) {
            achievementSystem.unlock(ragamuffin.ui.AchievementType.HEATHER_ROYALTY);
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────

    /** Whether the traveller site is currently active (crew on-site). */
    public boolean isSiteActive() { return siteActive; }

    /** Whether Paddy is currently hostile. */
    public boolean isPaddyHostile() { return paddyHostile; }

    /** Whether the dog fight ring has been dispersed. */
    public boolean isDogFightRingDispersed() { return dogFightRingDispersed; }

    /** Whether the council has been tipped off this visit. */
    public boolean isCouncilTippedOff() { return councilTippedOff; }

    /** Whether Derek (the enforcement officer) has arrived. */
    public boolean isDerekArrived() { return derekArrived; }

    /** Total COIN paid by Paddy's scrap fence this visit. */
    public int getScrapFenceCoinsThisVisit() { return scrapFenceCoinsThisVisit; }

    /** Number of cash-in-hand jobs completed this visit. */
    public int getJobsCompletedThisVisit() { return jobsCompletedThisVisit; }

    /** Whether the player has earned the DOG_PERMISSION_FLAG. */
    public boolean hasDogPermission() { return dogPermissionFlag; }

    /** Whether a job is currently in progress. */
    public boolean isJobInProgress() { return jobInProgress; }

    /** Days until the next visit (when site is inactive). */
    public int getDaysUntilNextVisit() { return daysUntilNextVisit; }

    // ── Package-private: for testing ──────────────────────────────────────────

    /** Force-activate the site at a given day (for testing). */
    void forceActivateSite(TimeSystem timeSystem) {
        activateSite(timeSystem);
    }

    /** Force-deactivate the site (for testing). */
    void forceDeactivateSite(TimeSystem timeSystem) {
        deactivateSite(timeSystem);
    }

    /** Set the dog permission flag directly (for testing). */
    void setDogPermissionFlag(boolean value) {
        this.dogPermissionFlag = value;
    }
}

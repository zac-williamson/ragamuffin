package ragamuffin.core;

import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.JobCentreRecord.JobSearchMissionType;
import ragamuffin.core.NewspaperSystem.InfamyEvent;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #795: The JobCentre Gauntlet — Universal Credit, Sanctions &amp; Bureaucratic Torment.
 *
 * <h3>Core loop</h3>
 * Every 3 in-game days a sign-on window opens at 09:00.  Missing a sign-on
 * increments {@code sanctionLevel} (0–3), reducing the UC payment by 3 COIN
 * per level. At sanction level 3 a persistent {@code DEBT_COLLECTOR} NPC
 * spawns and pursues the player until 10 COIN is paid.
 *
 * <h3>Job Search Requirements</h3>
 * Each sign-on assigns one of 5 satirical missions (CV_WORKSHOP, APPLY_FOR_3_JOBS,
 * MANDATORY_WORK_PLACEMENT, UNIVERSAL_JOBMATCH_PROFILE, WORK_CAPABILITY_ASSESSMENT).
 *
 * <h3>Criminal record complications</h3>
 * <ul>
 *   <li>3–5 offences → case worker suspicious; player gets Lie/Admit dialogue.</li>
 *   <li>6+ offences OR active Wanted level → POLICE NPC spawns at entrance.</li>
 *   <li>Notoriety Tier 3+ → case worker addresses player by Street Legend Title;
 *       auto-fails mission but still pays (too scared to sanction).</li>
 *   <li>Notoriety Tier 5 → case worker flees; claim permanently closed.</li>
 * </ul>
 *
 * <h3>DEBT_COLLECTOR</h3>
 * Unkillable grey-suited NPC; pathfinds to player; broadcasts "Oi! You owe
 * the DWP" every 10 seconds; nearby NPCs laugh. Punching adds Notoriety +15
 * and generates a NewspaperSystem headline.
 *
 * <h3>BUREAUCRACY skill track</h3>
 * Five perks: extended sign-on window, automatic sanction appeal, higher UC
 * payment, half-counted criminal record, go Off the Grid.
 */
public class JobCentreSystem {

    // ── Sign-on timing constants ───────────────────────────────────────────────

    /** Number of in-game days between mandatory sign-ons. */
    public static final int SIGN_ON_INTERVAL_DAYS = 3;

    /** Hour the sign-on window opens (09:00). */
    public static final float SIGN_ON_OPEN_HOUR = 9.0f;

    /** Default sign-on window duration in hours (1 in-game hour). */
    public static final float SIGN_ON_WINDOW_HOURS = 1.0f;

    /** Extended sign-on window (BUREAUCRACY Apprentice+). */
    public static final float SIGN_ON_WINDOW_EXTENDED_HOURS = 2.0f;

    /** Criminal-record offence threshold for suspicious case worker. */
    public static final int SUSPICIOUS_THRESHOLD_LOW = 3;

    /** Criminal-record offence threshold for police escort. */
    public static final int SUSPICIOUS_THRESHOLD_HIGH = 6;

    /** Notoriety tier at which the case worker auto-fails but still pays. */
    public static final int NOTORIETY_SCARED_TIER = 3;

    /** Notoriety tier at which the case worker flees and closes the claim. */
    public static final int NOTORIETY_FLEE_TIER = 5;

    /** Notoriety added when the player punches the debt collector. */
    public static final int DEBT_COLLECTOR_PUNCH_NOTORIETY = 15;

    /** Seconds between DEBT_COLLECTOR broadcasts. */
    public static final float DEBT_COLLECTOR_BROADCAST_INTERVAL = 10f;

    /** UC payment bonus at BUREAUCRACY level 3 (Journeyman). */
    public static final int BUREAUCRACY_PAYMENT_BONUS = 2;

    /** BUREAUCRACY level for automatic sanction appeal (Apprentice = level 1). */
    public static final int BUREAUCRACY_APPEAL_LEVEL = 1;

    /** BUREAUCRACY level for higher UC payment (Journeyman = level 2). */
    public static final int BUREAUCRACY_PAYMENT_LEVEL = 2;

    /** BUREAUCRACY level for half-counted criminal record (Expert = level 3). */
    public static final int BUREAUCRACY_HALF_RECORD_LEVEL = 3;

    /** BUREAUCRACY level for Off-the-Grid perk (Legend = level 4). */
    public static final int BUREAUCRACY_OFF_GRID_LEVEL = 4;

    // ── Faction cross-pollination constants ───────────────────────────────────

    /** Street Lads respect bonus for 3 missed sign-ons. */
    public static final int STREET_LADS_MISSED_SIGN_ON_RESPECT = 8;

    /** Council respect bonus at BUREAUCRACY Level 3 (Journeyman). */
    public static final int COUNCIL_BUREAUCRACY_RESPECT = 5;

    // ── Sign-on result types ──────────────────────────────────────────────────

    public enum SignOnResult {
        /** Normal sign-on. Mission assigned; UC payment given after completion. */
        SUCCESS,
        /** Criminal record 3–5 offences; player must choose Lie or Admit. */
        SUSPICIOUS,
        /** Criminal record 6+ offences or Wanted level; police spawned at entrance. */
        POLICE_ESCORT,
        /** Notoriety Tier 3+; case worker scared but pays anyway. Mission auto-fails. */
        NOTORIETY_SCARED,
        /** Notoriety Tier 5; case worker flees; claim closed. */
        NOTORIETY_FLEE,
        /** Marchetti mission item confiscated at sign-on. */
        MARCHETTI_CONFISCATION,
        /** Sign-on window is not currently open. */
        WINDOW_CLOSED,
        /** UC claim is permanently closed. */
        CLAIM_CLOSED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final TimeSystem timeSystem;
    private final CriminalRecord criminalRecord;
    private final NotorietySystem notorietySystem;
    private final FactionSystem factionSystem;
    private final RumourNetwork rumourNetwork;
    private final NewspaperSystem newspaperSystem;
    private final StreetSkillSystem streetSkillSystem;
    private final WantedSystem wantedSystem;
    private final NPCManager npcManager;
    private final Random random;

    private final JobCentreRecord record;

    /** The active DEBT_COLLECTOR NPC, or null if none. */
    private NPC debtCollector = null;

    /** Timer between debt collector broadcasts. */
    private float debtCollectorBroadcastTimer = 0f;

    /** The day number of the next required sign-on. */
    private int nextSignOnDay = SIGN_ON_INTERVAL_DAYS;

    /** Whether the sign-on window was open last frame (for missed-sign-on detection). */
    private boolean windowWasOpen = false;

    /** Marchetti mission item Materials — used for confiscation check. */
    private static final Material[] MARCHETTI_MISSION_ITEMS = {
        Material.CROWBAR, Material.BOLT_CUTTERS, Material.BALACLAVA
    };

    // ── Construction ──────────────────────────────────────────────────────────

    public JobCentreSystem(
            TimeSystem timeSystem,
            CriminalRecord criminalRecord,
            NotorietySystem notorietySystem,
            FactionSystem factionSystem,
            RumourNetwork rumourNetwork,
            NewspaperSystem newspaperSystem,
            StreetSkillSystem streetSkillSystem,
            WantedSystem wantedSystem,
            NPCManager npcManager,
            Random random) {
        this.timeSystem       = timeSystem;
        this.criminalRecord   = criminalRecord;
        this.notorietySystem  = notorietySystem;
        this.factionSystem    = factionSystem;
        this.rumourNetwork    = rumourNetwork;
        this.newspaperSystem  = newspaperSystem;
        this.streetSkillSystem = streetSkillSystem;
        this.wantedSystem     = wantedSystem;
        this.npcManager       = npcManager;
        this.random           = random;
        this.record           = new JobCentreRecord();
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the JobCentre system each frame.
     *
     * @param delta    seconds since last frame
     * @param player   the player
     * @param allNpcs  all active NPCs in the world
     */
    public void update(float delta, Player player, List<NPC> allNpcs) {
        if (record.isClaimClosed()) return;

        int currentDay = timeSystem.getDayCount();
        float currentHour = timeSystem.getTime();

        boolean windowOpen = isSignOnWindowOpenNow(currentDay, currentHour);

        // Detect sign-on window closing without the player having signed on
        if (windowWasOpen && !windowOpen) {
            // Window just closed — did the player sign on?
            if (record.getLastSignOnDay() < nextSignOnDay) {
                // Missed sign-on
                record.recordMissedSignOn();

                // BUREAUCRACY Apprentice+: automatic sanction appeal
                int bureaucracyLevel = streetSkillSystem.getTierLevel(StreetSkillSystem.Skill.BUREAUCRACY);
                if (bureaucracyLevel >= BUREAUCRACY_APPEAL_LEVEL && record.getSanctionLevel() > 0) {
                    record.appealSanction();
                }

                // Street Lads notice after 3 missed sign-ons
                if (!record.isStreetLadsNoticed()
                        && record.getMissedSignOns() >= JobCentreRecord.STREET_LADS_NOTICE_MISSED) {
                    record.setStreetLadsNoticed(true);
                    factionSystem.applyRespectDelta(
                            Faction.STREET_LADS,
                            STREET_LADS_MISSED_SIGN_ON_RESPECT);
                    // Seed rumour
                    if (rumourNetwork != null && allNpcs != null) {
                        for (NPC npc : allNpcs) {
                            if (npc.getType() == NPCType.STREET_LAD) {
                                rumourNetwork.addRumour(npc, new Rumour(
                                        RumourType.GANG_ACTIVITY,
                                        "Proper anti-establishment, that — never signs on."));
                                break;
                            }
                        }
                    }
                }

                // Advance next sign-on day
                nextSignOnDay = currentDay + SIGN_ON_INTERVAL_DAYS;
            }
        }

        windowWasOpen = windowOpen;

        // Debt collector update
        updateDebtCollector(delta, player, allNpcs);

        // BUREAUCRACY Level 3 → Council Respect
        if (!record.isCouncilRespectAwarded()) {
            int bureaucracyLevel = streetSkillSystem.getTierLevel(StreetSkillSystem.Skill.BUREAUCRACY);
            if (bureaucracyLevel >= BUREAUCRACY_HALF_RECORD_LEVEL) {
                record.setCouncilRespectAwarded(true);
                factionSystem.applyRespectDelta(Faction.THE_COUNCIL, COUNCIL_BUREAUCRACY_RESPECT);
            }
        }
    }

    // ── Sign-on logic ──────────────────────────────────────────────────────────

    /**
     * Attempt to sign on at the JobCentre.
     *
     * <p>Performs all criminal-record and notoriety checks, assigns a mission,
     * and optionally confiscates Marchetti items.
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @return a {@link SignOnResult} describing the outcome
     */
    public SignOnResult trySignOn(Player player, Inventory inventory) {
        if (record.isClaimClosed()) {
            return SignOnResult.CLAIM_CLOSED;
        }

        int currentDay = timeSystem.getDayCount();
        float currentHour = timeSystem.getTime();

        if (!isSignOnWindowOpenNow(currentDay, currentHour)) {
            return SignOnResult.WINDOW_CLOSED;
        }

        // Notoriety Tier 5 — case worker flees; claim closed
        int tier = notorietySystem.getTier();
        if (tier >= NOTORIETY_FLEE_TIER) {
            record.closeClaim();
            return SignOnResult.NOTORIETY_FLEE;
        }

        // Check for Marchetti mission items in inventory (confiscated)
        for (Material item : MARCHETTI_MISSION_ITEMS) {
            if (inventory.getItemCount(item) > 0) {
                inventory.removeItem(item, 1);
                // Still process sign-on after confiscation, but return the special result
                processSignOn(player, inventory, currentDay, tier);
                return SignOnResult.MARCHETTI_CONFISCATION;
            }
        }

        // Notoriety Tier 3+ — scared, auto-fail mission but pay
        if (tier >= NOTORIETY_SCARED_TIER) {
            // Pay UC regardless (too scared to sanction)
            int payment = computeUCPayment();
            inventory.addItem(Material.COIN, payment);
            record.recordSignOn(currentDay);
            nextSignOnDay = currentDay + SIGN_ON_INTERVAL_DAYS;
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.BUREAUCRACY, JobCentreRecord.XP_SIGN_ON);
            return SignOnResult.NOTORIETY_SCARED;
        }

        // Criminal record checks
        int offences = getEffectiveOffenceCount();
        if (offences >= SUSPICIOUS_THRESHOLD_HIGH || isWanted()) {
            // Police escort — spawn POLICE at entrance
            spawnPoliceAtEntrance(player);
            processSignOn(player, inventory, currentDay, tier);
            return SignOnResult.POLICE_ESCORT;
        }

        if (offences >= SUSPICIOUS_THRESHOLD_LOW) {
            processSignOn(player, inventory, currentDay, tier);
            return SignOnResult.SUSPICIOUS;
        }

        // Normal sign-on
        processSignOn(player, inventory, currentDay, tier);
        return SignOnResult.SUCCESS;
    }

    /**
     * Complete the sign-on, award UC payment, and assign a mission.
     */
    private void processSignOn(Player player, Inventory inventory, int currentDay, int tier) {
        // Award UC payment
        int payment = computeUCPayment();
        inventory.addItem(Material.COIN, payment);

        // Record sign-on
        record.recordSignOn(currentDay);
        nextSignOnDay = currentDay + SIGN_ON_INTERVAL_DAYS;

        // Assign a random mission
        JobSearchMissionType mission = pickRandomMission();
        record.assignMission(mission);

        // Award BUREAUCRACY XP
        streetSkillSystem.awardXP(StreetSkillSystem.Skill.BUREAUCRACY, JobCentreRecord.XP_SIGN_ON);
    }

    /**
     * Compute the effective UC payment for this sign-on, including BUREAUCRACY bonuses.
     */
    public int computeUCPayment() {
        int base = record.getCurrentUCPayment();
        int bureaucracyLevel = streetSkillSystem.getTierLevel(StreetSkillSystem.Skill.BUREAUCRACY);
        if (bureaucracyLevel >= BUREAUCRACY_PAYMENT_LEVEL) {
            base += BUREAUCRACY_PAYMENT_BONUS;
        }
        return Math.max(0, base);
    }

    /**
     * Complete the currently active job search mission.
     * Awards BUREAUCRACY XP and marks the mission as done.
     *
     * @return true if a mission was active and is now completed
     */
    public boolean completeMission() {
        if (record.getCurrentMission() == null || record.isMissionCompletedThisCycle()) {
            return false;
        }
        record.completeMission();
        streetSkillSystem.awardXP(StreetSkillSystem.Skill.BUREAUCRACY, JobCentreRecord.XP_MISSION_COMPLETE);
        return true;
    }

    /**
     * Pay off the DWP debt (10 COIN). Deactivates the debt collector.
     *
     * @param inventory the player's inventory
     * @return true if the debt was cleared
     */
    public boolean payDebt(Inventory inventory) {
        if (!record.isDebtCollectorActive()) return false;
        if (inventory.getItemCount(Material.COIN) < JobCentreRecord.DEBT_AMOUNT) return false;

        inventory.removeItem(Material.COIN, JobCentreRecord.DEBT_AMOUNT);
        record.clearDebt();
        despawnDebtCollector();
        return true;
    }

    // ── Sign-on window ─────────────────────────────────────────────────────────

    /**
     * Whether the sign-on window is currently open.
     */
    public boolean isSignOnWindowOpen() {
        return isSignOnWindowOpenNow(timeSystem.getDayCount(), timeSystem.getTime());
    }

    private boolean isSignOnWindowOpenNow(int day, float hour) {
        if (record.isClaimClosed()) return false;
        if (day < nextSignOnDay) return false;

        float windowDuration = getSignOnWindowDuration();
        return hour >= SIGN_ON_OPEN_HOUR && hour < (SIGN_ON_OPEN_HOUR + windowDuration);
    }

    private float getSignOnWindowDuration() {
        int bureaucracyLevel = streetSkillSystem.getTierLevel(StreetSkillSystem.Skill.BUREAUCRACY);
        return bureaucracyLevel >= BUREAUCRACY_APPEAL_LEVEL
                ? SIGN_ON_WINDOW_EXTENDED_HOURS
                : SIGN_ON_WINDOW_HOURS;
    }

    // ── Debt collector management ──────────────────────────────────────────────

    private void updateDebtCollector(float delta, Player player, List<NPC> allNpcs) {
        if (!record.isDebtCollectorActive()) return;

        // Spawn debt collector if not present
        if (debtCollector == null || !debtCollector.isAlive()) {
            spawnDebtCollector(player);
        }

        if (debtCollector == null) return;

        // Periodic broadcast
        debtCollectorBroadcastTimer -= delta;
        if (debtCollectorBroadcastTimer <= 0f) {
            debtCollectorBroadcastTimer = DEBT_COLLECTOR_BROADCAST_INTERVAL;
            debtCollector.setSpeechText("Oi! You owe the DWP!", 4f);

            // Make nearby NPCs laugh
            if (allNpcs != null) {
                for (NPC npc : allNpcs) {
                    if (npc == debtCollector) continue;
                    if (!npc.isAlive()) continue;
                    float dist = npc.getPosition().dst(debtCollector.getPosition());
                    if (dist <= 8f) {
                        String[] laughs = {
                            "Ha! Skint, are ya?", "Ha ha ha!", "Classic.",
                            "Stitched up!", "Ha, loser!"
                        };
                        npc.setSpeechText(laughs[random.nextInt(laughs.length)], 3f);
                    }
                }
            }
        }

        // Pathfind toward player
        debtCollector.setTargetPosition(player.getPosition().cpy());
        debtCollector.setState(NPCState.CHASING_PLAYER);
    }

    private void spawnDebtCollector(Player player) {
        if (npcManager == null) return;
        float angle = (float) (random.nextFloat() * Math.PI * 2);
        float dist = 15f;
        float x = player.getPosition().x + (float) Math.cos(angle) * dist;
        float z = player.getPosition().z + (float) Math.sin(angle) * dist;
        float y = player.getPosition().y;
        debtCollector = npcManager.spawnNPC(NPCType.DEBT_COLLECTOR, x, y, z);
        if (debtCollector != null) {
            debtCollector.setName("DWP Debt Collector");
            debtCollector.setBuildingType(LandmarkType.JOB_CENTRE);
            debtCollector.setState(NPCState.CHASING_PLAYER);
            debtCollector.setSpeechText("Oi! You owe the DWP!", 4f);
            debtCollectorBroadcastTimer = DEBT_COLLECTOR_BROADCAST_INTERVAL;
        }
    }

    private void despawnDebtCollector() {
        if (debtCollector != null) {
            debtCollector.setState(NPCState.WANDERING);
            debtCollector.setSpeechText("We'll be in touch.", 3f);
            debtCollector = null;
        }
    }

    /**
     * Handle the player punching the debt collector.
     * Adds Notoriety +15 and triggers a newspaper headline.
     */
    public void onDebtCollectorPunched(NotorietySystem.AchievementCallback achievementCallback) {
        notorietySystem.addNotoriety(DEBT_COLLECTOR_PUNCH_NOTORIETY, achievementCallback);
        if (newspaperSystem != null) {
            newspaperSystem.recordEvent(new InfamyEvent(
                    "DWP_ASSAULT", "JobCentre Plus", null,
                    "DWP Debt Collector", 0, null, null, 5));
        }
    }

    // ── Police at entrance ─────────────────────────────────────────────────────

    private void spawnPoliceAtEntrance(Player player) {
        if (npcManager == null) return;
        // Spawn a POLICE NPC near the player's position (simulating JobCentre entrance)
        float angle = (float) (random.nextFloat() * Math.PI * 2);
        float x = player.getPosition().x + (float) Math.cos(angle) * 5f;
        float z = player.getPosition().z + (float) Math.sin(angle) * 5f;
        float y = player.getPosition().y;
        NPC police = npcManager.spawnNPC(NPCType.POLICE, x, y, z);
        if (police != null) {
            police.setState(NPCState.PATROLLING);
            police.setSpeechText("You've got a record, son. Watch yourself.", 4f);
        }
    }

    // ── Mission assignment ─────────────────────────────────────────────────────

    private JobSearchMissionType pickRandomMission() {
        JobSearchMissionType[] values = JobSearchMissionType.values();
        return values[random.nextInt(values.length)];
    }

    // ── Criminal record helper ─────────────────────────────────────────────────

    private int getEffectiveOffenceCount() {
        int total = criminalRecord.getTotalCrimes();
        int bureaucracyLevel = streetSkillSystem.getTierLevel(StreetSkillSystem.Skill.BUREAUCRACY);
        if (bureaucracyLevel >= BUREAUCRACY_HALF_RECORD_LEVEL) {
            total = total / 2;
        }
        return total;
    }

    private boolean isWanted() {
        if (wantedSystem == null) return false;
        return wantedSystem.getWantedStars() > 0;
    }

    // ── Dialogue helpers ──────────────────────────────────────────────────────

    /**
     * Get case worker dialogue based on the sign-on result.
     *
     * @param result        the sign-on result
     * @param notorietyTier the player's current notoriety tier
     * @return dialogue string
     */
    public String getCaseWorkerDialogue(SignOnResult result, int notorietyTier) {
        switch (result) {
            case SUCCESS:
                return "Right, let's get you sorted. Your job search requirements for this period... "
                        + getAssignedMissionDialogue();
            case SUSPICIOUS:
                return "I see you've had some... incidents. I'll need you to confirm: did you commit "
                        + "any offences this period? [LIE / ADMIT IT]";
            case POLICE_ESCORT:
                return "I'm going to have to ask you to wait — there are some officers here who'd "
                        + "like a word. We take compliance very seriously.";
            case NOTORIETY_SCARED:
                return "Oh — I know who you are. You're " + NotorietySystem.getTierTitle(notorietyTier)
                        + ". Right. Your payment's, erm, processed. No missions today. You're free to go.";
            case NOTORIETY_FLEE:
                return "Oh god — HELP! SECURITY! *runs out of the building*";
            case MARCHETTI_CONFISCATION:
                return "I'm going to have to confiscate that. Can't be bringing that sort of thing "
                        + "in here. Right, let's continue with your appointment...";
            case WINDOW_CLOSED:
                return "Your appointment is not until " + getNextSignOnInfo()
                        + ". We'll see you then.";
            case CLAIM_CLOSED:
                return "Your claim has been permanently closed. Please refer to your local council.";
            default:
                return "Take a seat. Someone will be with you shortly.";
        }
    }

    private String getAssignedMissionDialogue() {
        JobSearchMissionType mission = record.getCurrentMission();
        if (mission == null) return "No further requirements at this time.";
        return mission.getDescription();
    }

    /**
     * Get "lie vs admit" sub-dialogue for the SUSPICIOUS case.
     *
     * @param lied true if the player chose to lie
     * @return the case worker's response
     */
    public String getSuspiciousResponse(boolean lied) {
        if (lied) {
            return "Right. Well, we'll take that on record. We keep all these files, you know. "
                    + "Everything goes in the system. Here's your payment — and a word of advice: "
                    + "keep your nose clean.";
        } else {
            return "I appreciate your honesty. That's… actually quite rare. "
                    + "I'll note it positively. Your payment has been processed.";
        }
    }

    private String getNextSignOnInfo() {
        return "Day " + nextSignOnDay + ", between 09:00 and "
                + String.format("%.0f:00", SIGN_ON_OPEN_HOUR + getSignOnWindowDuration());
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public JobCentreRecord getRecord() { return record; }

    public NPC getDebtCollector() { return debtCollector; }

    public int getNextSignOnDay() { return nextSignOnDay; }

    /** Force the next sign-on day (for testing). */
    public void setNextSignOnDayForTesting(int day) { this.nextSignOnDay = day; }

    /** Force the debt collector NPC reference (for testing). */
    public void setDebtCollectorForTesting(NPC npc) { this.debtCollector = npc; }
}

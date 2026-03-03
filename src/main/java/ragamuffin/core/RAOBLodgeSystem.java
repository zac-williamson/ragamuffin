package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1443: Northfield Buffaloes Lodge No. 347 — The Initiation, the
 * Old Boys' Network &amp; the Lodge Safe Heist.
 *
 * <h3>Access Control</h3>
 * <ul>
 *   <li>Big Bernard ({@link ragamuffin.entity.NPCType#RAOB_DOORMAN}) guards
 *       {@link ragamuffin.world.PropType#LODGE_DOOR_PROP}.</li>
 *   <li>Player with {@link Material#BUFFALO_MEMBERSHIP_CARD} passes freely.</li>
 *   <li>Without card: 40 % bluff success if Notoriety &lt; 20 (BLUFF_SUCCESS_CHANCE);
 *       on failure or force-entry, {@link CrimeType#LODGE_TRESPASS} is recorded.</li>
 * </ul>
 *
 * <h3>Sponsorship Hustle</h3>
 * <ul>
 *   <li>Four {@link NPCType#RAOB_LODGE_MEMBER} NPCs each grant one
 *       {@link Material#SPONSORSHIP_FORM} after a unique favour.</li>
 *   <li>Any two forms are required to begin initiation.</li>
 * </ul>
 *
 * <h3>Initiation Ceremony</h3>
 * <ul>
 *   <li>Thursdays 20:00 at {@link ragamuffin.world.PropType#INITIATION_ALTAR_PROP}.</li>
 *   <li>Requires 2× {@link Material#SPONSORSHIP_FORM} + {@value #INITIATION_COIN_COST} COIN.</li>
 *   <li>Uses {@link BattleBarMiniGame} (MEDIUM difficulty).</li>
 *   <li>Success: grants {@link Material#BUFFALO_MEMBERSHIP_CARD}, {@link Material#BUFFALO_FEZ},
 *       sets {@code playerIsMember = true}, awards {@link AchievementType#BUFFALO_SOLDIER}.</li>
 * </ul>
 *
 * <h3>Lodge Bar</h3>
 * <ul>
 *   <li>1 COIN pints; rumour sink; secret handshake (+{@value #HANDSHAKE_RESPECT_PER_NPC}
 *       COUNCIL respect per NPC per day) gated on membership card.</li>
 *   <li>Blackmail: use {@link Material#KOMPROMAT_LEDGER} on any NPC — 20 % chance NPC
 *       calls police, recording {@link CrimeType#LODGE_BLACKMAIL}.</li>
 * </ul>
 *
 * <h3>Lodge Safe Heist</h3>
 * <ul>
 *   <li><b>Silent:</b> eavesdrop Ron's combination 20:00–20:30; {@value #EAVESDROP_DETECTION_CHANCE}
 *       detection chance. Grants {@link #SAFE_COMBO_KNOWN} flag.</li>
 *   <li><b>Forced:</b> {@value #SAFE_FORCED_HITS} hits, {@value #SAFE_FORCED_NOISE} noise,
 *       {@value #SAFE_FORCED_DETECTION_CHANCE} detection chance.</li>
 *   <li>Safe contents: {@link Material#KOMPROMAT_LEDGER}, {@link Material#LODGE_CHARTER_DOCUMENT},
 *       {@link Material#REGALIA_SET}, 30–50 COIN.</li>
 *   <li>Success awards {@link AchievementType#SAFE_CRACKER} and {@link AchievementType#GRUBBY_LEVERAGE}
 *       (if KOMPROMAT_LEDGER looted).</li>
 * </ul>
 *
 * <h3>Old Boys' Network</h3>
 * <ul>
 *   <li>Housing queue skip, magistrates case dismissed, planning fast-track, bookies hot tip —
 *       all gated on {@link Material#BUFFALO_MEMBERSHIP_CARD} plus coin/favour cost.</li>
 * </ul>
 */
public class RAOBLodgeSystem {

    // ── Access control ────────────────────────────────────────────────────────

    /** Probability (0–1) that a bluff succeeds when Notoriety &lt; BLUFF_NOTORIETY_MAX. */
    public static final float BLUFF_SUCCESS_CHANCE = 0.40f;

    /** Maximum Notoriety at which the player may attempt a bluff. */
    public static final int BLUFF_NOTORIETY_MAX = 20;

    /** Notoriety gain when LODGE_TRESPASS crime is recorded. */
    public static final int TRESPASS_NOTORIETY = 8;

    // ── Initiation ────────────────────────────────────────────────────────────

    /** Number of SPONSORSHIP_FORMs required to begin initiation. */
    public static final int INITIATION_FORMS_REQUIRED = 2;

    /** COIN cost for the initiation ceremony. */
    public static final int INITIATION_COIN_COST = 10;

    /** In-game hour the initiation ceremony begins. */
    public static final float INITIATION_HOUR = 20.0f;

    /** Day-of-week index for Thursday (0 = Monday … 6 = Sunday). */
    public static final int INITIATION_DAY = 3; // Thursday

    // ── Lodge bar ─────────────────────────────────────────────────────────────

    /** COIN cost for a pint at the lodge bar. */
    public static final int PINT_COIN_COST = 1;

    /** Council respect granted per NPC per day via secret handshake. */
    public static final int HANDSHAKE_RESPECT_PER_NPC = 3;

    /** Probability (0–1) that an NPC calls police when blackmailed with KOMPROMAT_LEDGER. */
    public static final float BLACKMAIL_POLICE_CHANCE = 0.20f;

    // ── Lodge Safe Heist — eavesdrop ──────────────────────────────────────────

    /** In-game hour Ron recites the safe combination. */
    public static final float EAVESDROP_START_HOUR = 20.0f;

    /** In-game hour the eavesdrop window closes. */
    public static final float EAVESDROP_END_HOUR = 20.5f;

    /** Detection chance (0–1) during eavesdrop. */
    public static final float EAVESDROP_DETECTION_CHANCE = 0.15f;

    // ── Lodge Safe Heist — forced entry ──────────────────────────────────────

    /** Number of hits required to force the safe open. */
    public static final int SAFE_FORCED_HITS = 12;

    /** Noise level emitted when forcing the safe. */
    public static final float SAFE_FORCED_NOISE = 7.0f;

    /** Detection chance (0–1) when forcing the safe. */
    public static final float SAFE_FORCED_DETECTION_CHANCE = 0.70f;

    /** Notoriety gain on LODGE_BURGLARY crime. */
    public static final int BURGLARY_NOTORIETY = 5;

    /** Minimum COIN yield from safe. */
    public static final int SAFE_COIN_MIN = 30;

    /** Maximum COIN yield from safe (exclusive). */
    public static final int SAFE_COIN_MAX = 51;

    // ── Old Boys' Network ─────────────────────────────────────────────────────

    /** COIN cost for a housing-queue skip (Brian). */
    public static final int HOUSING_SKIP_COST = 15;

    /** COIN cost for a magistrates case dismissal (Sandra). */
    public static final int CASE_DISMISSED_COST = 20;

    /** COIN cost for a planning fast-track (Reg). */
    public static final int PLANNING_FAST_TRACK_COST = 10;

    /** COIN cost for the bookies weekly hot tip (Terry). */
    public static final int HOT_TIP_COST = 5;

    // ── NPC names ─────────────────────────────────────────────────────────────

    public static final String RON_NAME               = "Ron";
    public static final String BRIAN_NAME             = "Brian";
    public static final String TERRY_NAME             = "Terry";
    public static final String COUNCILLOR_WALSH_NAME  = "Councillor Walsh";

    // ── Result enums ─────────────────────────────────────────────────────────

    /** Result of an access check at the lodge door. */
    public enum AccessResult {
        /** Player holds a BUFFALO_MEMBERSHIP_CARD — entry granted immediately. */
        MEMBER_ENTRY,
        /** Bluff succeeded — Big Bernard was fooled. */
        BLUFF_SUCCESS,
        /** Bluff failed — Big Bernard turned the player away. */
        BLUFF_FAILED,
        /** Player forced entry — LODGE_TRESPASS crime recorded. */
        FORCED_ENTRY,
        /** No doorman NPC present at the door prop. */
        NO_DOORMAN
    }

    /** Result of attempting the initiation ceremony. */
    public enum InitiationResult {
        /** Ceremony cannot start — wrong day/time. */
        WRONG_TIME,
        /** Player lacks sufficient SPONSORSHIP_FORMs. */
        INSUFFICIENT_FORMS,
        /** Player cannot afford the coin cost. */
        INSUFFICIENT_COIN,
        /** Player is already a member. */
        ALREADY_MEMBER,
        /** Initiation mini-game failed. */
        MINI_GAME_FAILED,
        /** Initiation succeeded — BUFFALO_MEMBERSHIP_CARD granted. */
        SUCCESS
    }

    /** Result of a pint purchase at the lodge bar. */
    public enum PintResult {
        /** Pint served. */
        SUCCESS,
        /** Player cannot afford a pint. */
        INSUFFICIENT_COIN,
        /** Player is not a member and the bar is members-only. */
        NOT_A_MEMBER
    }

    /** Result of an eavesdrop attempt on Ron's combination. */
    public enum EavesdropResult {
        /** Outside the eavesdrop window. */
        WRONG_TIME,
        /** Ron not present. */
        RON_ABSENT,
        /** Player was detected. */
        DETECTED,
        /** Combination learned successfully. */
        SUCCESS
    }

    /** Result of a forced or silent safe crack. */
    public enum SafeResult {
        /** Safe already cracked this session. */
        ALREADY_LOOTED,
        /** Silent open: combo not known — eavesdrop first. */
        COMBO_UNKNOWN,
        /** Forced entry: detected — LODGE_BURGLARY crime recorded, loot denied. */
        DETECTED,
        /** Safe opened and looted successfully. */
        SUCCESS
    }

    /** Result of a blackmail attempt with KOMPROMAT_LEDGER. */
    public enum BlackmailResult {
        /** Player does not hold a KOMPROMAT_LEDGER. */
        NO_LEDGER,
        /** No target NPC nearby. */
        NO_TARGET,
        /** NPC called the police — LODGE_BLACKMAIL crime recorded. */
        POLICE_CALLED,
        /** NPC capitulated — GRUBBY_LEVERAGE achievement awarded. */
        SUCCESS
    }

    /** Result of an Old Boys' Network favour request. */
    public enum FavourResult {
        /** Player is not a member. */
        NOT_A_MEMBER,
        /** Player cannot afford the favour. */
        INSUFFICIENT_COIN,
        /** No suitable member NPC nearby. */
        NPC_ABSENT,
        /** Favour already used this session. */
        ALREADY_USED,
        /** Favour granted. */
        SUCCESS
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** True once the player has been initiated into the lodge. */
    private boolean playerIsMember;

    /** True if the player has learned Ron's safe combination via eavesdropping. */
    private boolean safeComboKnown;

    /** True once the safe has been looted this session. */
    private boolean safeLooted;

    /** Number of SPONSORSHIP_FORMs obtained from each named NPC (tracked by name). */
    private boolean ronFormGiven;
    private boolean brianFormGiven;
    private boolean terryFormGiven;
    private boolean walshFormGiven;

    /** Day index on which the last handshake respect bonus was applied. */
    private int lastHandshakeDay;

    /** True if the housing-queue skip has been used this session. */
    private boolean housingSkipUsed;

    /** True if the magistrates case dismissal has been used this session. */
    private boolean caseDismissedUsed;

    /** True if the planning fast-track has been used this session. */
    private boolean planningFastTrackUsed;

    /** True if the bookies hot tip has been used this session. */
    private boolean hotTipUsed;

    /** Accumulated forced-entry hits on the safe (0 → SAFE_FORCED_HITS). */
    private int safeHitsApplied;

    /** Cached reference to the NotorietySystem (set via setter). */
    private NotorietySystem notorietySystem;

    /** Cached reference to the CriminalRecord (set via setter). */
    private CriminalRecord criminalRecord;

    /** Cached reference to the RumourNetwork (set via setter). */
    private RumourNetwork rumourNetwork;

    /** Cached reference to the FactionSystem (set via setter). */
    private FactionSystem factionSystem;

    // ── Constants exposed for test access ─────────────────────────────────────

    /** Flag value returned by {@link #isSafeComboKnown()} when eavesdrop succeeded. */
    public static final boolean SAFE_COMBO_KNOWN = true;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public RAOBLodgeSystem(Random random) {
        this.random = random;
        this.playerIsMember = false;
        this.safeComboKnown = false;
        this.safeLooted = false;
        this.safeHitsApplied = 0;
        this.lastHandshakeDay = -1;
        this.housingSkipUsed = false;
        this.caseDismissedUsed = false;
        this.planningFastTrackUsed = false;
        this.hotTipUsed = false;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem ns)   { this.notorietySystem = ns; }
    public void setCriminalRecord(CriminalRecord cr)     { this.criminalRecord  = cr; }
    public void setRumourNetwork(RumourNetwork rn)       { this.rumourNetwork   = rn; }
    public void setFactionSystem(FactionSystem fs)       { this.factionSystem   = fs; }

    // ── State accessors ───────────────────────────────────────────────────────

    public boolean isPlayerMember()     { return playerIsMember; }
    public boolean isSafeComboKnown()   { return safeComboKnown; }
    public boolean isSafeLooted()       { return safeLooted; }
    public int     getSafeHitsApplied() { return safeHitsApplied; }
    public boolean isRonFormGiven()     { return ronFormGiven; }
    public boolean isBrianFormGiven()   { return brianFormGiven; }
    public boolean isTerryFormGiven()   { return terryFormGiven; }
    public boolean isWalshFormGiven()   { return walshFormGiven; }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-frame update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called every game frame. Handles time-gated events (handshake respect,
     * session resets at midnight).
     *
     * @param delta       seconds since last frame
     * @param timeSystem  current game time
     * @param npcs        all active NPCs
     * @param achievementCallback callback for awarding achievements (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       List<NPC> npcs,
                       NotorietySystem.AchievementCallback achievementCallback) {
        if (timeSystem == null) return;

        // Daily session reset — use getDayCount() as a day-change sentinel
        int currentDay = timeSystem.getDayCount();
        if (currentDay != lastHandshakeDay) {
            // Handshake respect bonus applied once per day for each RAOB_LODGE_MEMBER NPC
            if (playerIsMember && factionSystem != null && npcs != null) {
                int lodgeMembersNearby = countNPCsOfType(npcs, NPCType.RAOB_LODGE_MEMBER);
                if (lodgeMembersNearby > 0) {
                    factionSystem.applyRespectDelta(Faction.THE_COUNCIL,
                            lodgeMembersNearby * HANDSHAKE_RESPECT_PER_NPC);
                }
            }
            lastHandshakeDay = currentDay;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Access control — Big Bernard at LODGE_DOOR_PROP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt to enter the lodge. Checks membership card, bluff, or force-entry.
     *
     * @param inventory     player inventory
     * @param doormanNearby true if Big Bernard (RAOB_DOORMAN) is at the door
     * @param forceEntry    true if the player is attempting to push past the doorman
     * @param witnessed     true if another NPC witnessed force-entry
     * @param achievementCallback callback (may be null)
     * @return {@link AccessResult}
     */
    public AccessResult attemptEntry(Inventory inventory,
                                     boolean doormanNearby,
                                     boolean forceEntry,
                                     boolean witnessed,
                                     NotorietySystem.AchievementCallback achievementCallback) {
        // Member always enters freely
        if (inventory.hasItem(Material.BUFFALO_MEMBERSHIP_CARD)) {
            return AccessResult.MEMBER_ENTRY;
        }

        if (!doormanNearby) {
            // No doorman — lodge is unmanned, player slips in
            return AccessResult.NO_DOORMAN;
        }

        if (forceEntry) {
            recordTrespass(witnessed, achievementCallback);
            return AccessResult.FORCED_ENTRY;
        }

        // Bluff attempt
        int notoriety = notorietySystem != null ? notorietySystem.getNotoriety() : 0;
        if (notoriety < BLUFF_NOTORIETY_MAX && random.nextFloat() < BLUFF_SUCCESS_CHANCE) {
            return AccessResult.BLUFF_SUCCESS;
        }

        return AccessResult.BLUFF_FAILED;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sponsorship — obtaining SPONSORSHIP_FORMs from named NPCs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ask Ron for a sponsorship form (favour: buy him a pint — already done via
     * {@link #buyPint}).  Ron hands over his form once the player has bought him
     * a pint (pass {@code pintBought = true}).
     *
     * @param inventory  player inventory
     * @param npcNearby  true if Ron is nearby
     * @param pintBought true if the player has already bought Ron a pint
     * @return true if the form was granted
     */
    public boolean requestRonSponsorshipForm(Inventory inventory,
                                              boolean npcNearby,
                                              boolean pintBought) {
        if (!npcNearby || ronFormGiven) return false;
        if (!pintBought) return false;
        ronFormGiven = true;
        inventory.addItem(Material.SPONSORSHIP_FORM, 1);
        return true;
    }

    /**
     * Ask Brian (Housing Officer) for a sponsorship form (favour: bring him a
     * {@link Material#BOX_OF_CHOCOLATES}).
     *
     * @param inventory player inventory
     * @param npcNearby true if Brian is nearby
     * @return true if the form was granted
     */
    public boolean requestBrianSponsorshipForm(Inventory inventory, boolean npcNearby) {
        if (!npcNearby || brianFormGiven) return false;
        if (!inventory.hasItem(Material.BOX_OF_CHOCOLATES)) return false;
        inventory.removeItem(Material.BOX_OF_CHOCOLATES, 1);
        brianFormGiven = true;
        inventory.addItem(Material.SPONSORSHIP_FORM, 1);
        return true;
    }

    /**
     * Ask Terry (bookmaker) for a sponsorship form (favour: show evidence of a
     * 10+ COIN winning bet slip — player must hold a {@link Material#BET_SLIP}).
     *
     * @param inventory  player inventory
     * @param npcNearby  true if Terry is nearby
     * @param betSlipHeld true if the player holds a BET_SLIP in inventory
     * @return true if the form was granted
     */
    public boolean requestTerrySponsorshipForm(Inventory inventory,
                                                boolean npcNearby,
                                                boolean betSlipHeld) {
        if (!npcNearby || terryFormGiven) return false;
        if (!betSlipHeld || !inventory.hasItem(Material.BET_SLIP)) return false;
        terryFormGiven = true;
        inventory.addItem(Material.SPONSORSHIP_FORM, 1);
        return true;
    }

    /**
     * Ask Councillor Walsh for a sponsorship form (favour: pay 5 COIN "donation").
     *
     * @param inventory player inventory
     * @param npcNearby true if Councillor Walsh is nearby
     * @return true if the form was granted
     */
    public boolean requestWalshSponsorshipForm(Inventory inventory, boolean npcNearby) {
        if (!npcNearby || walshFormGiven) return false;
        if (inventory.getItemCount(Material.COIN) < HOT_TIP_COST) return false;
        inventory.removeItem(Material.COIN, HOT_TIP_COST);
        walshFormGiven = true;
        inventory.addItem(Material.SPONSORSHIP_FORM, 1);
        // Seed initiation rumour
        seedRumour(null, RumourType.RAOB_INITIATION,
                "Word is there's going to be an initiation at the Buffalo lodge.");
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initiation ceremony
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt the initiation ceremony at the INITIATION_ALTAR_PROP.
     * Must be Thursday at 20:00, player needs 2× SPONSORSHIP_FORM and 10 COIN.
     * Uses a MEDIUM BattleBarMiniGame — the bar is simulated to resolved immediately
     * for testability (pass {@code miniGameSuccess} directly from the caller).
     *
     * @param inventory          player inventory
     * @param timeSystem         current game time
     * @param altarNearby        true if player is at the INITIATION_ALTAR_PROP
     * @param miniGameSuccess    result of the BattleBarMiniGame (caller resolves)
     * @param achievementCallback callback for achievements (may be null)
     * @return {@link InitiationResult}
     */
    public InitiationResult attemptInitiation(Inventory inventory,
                                               TimeSystem timeSystem,
                                               boolean altarNearby,
                                               boolean miniGameSuccess,
                                               NotorietySystem.AchievementCallback achievementCallback) {
        if (playerIsMember) return InitiationResult.ALREADY_MEMBER;

        // Time gate: Thursday (dayCount % 7 == 3) at 20:00
        if (timeSystem != null) {
            int dow = timeSystem.getDayCount() % 7;
            int hour = timeSystem.getHours();
            if (dow != INITIATION_DAY || hour < (int) INITIATION_HOUR || hour >= (int) (INITIATION_HOUR + 1f)) {
                return InitiationResult.WRONG_TIME;
            }
        }

        if (inventory.getItemCount(Material.SPONSORSHIP_FORM) < INITIATION_FORMS_REQUIRED) {
            return InitiationResult.INSUFFICIENT_FORMS;
        }

        if (inventory.getItemCount(Material.COIN) < INITIATION_COIN_COST) {
            return InitiationResult.INSUFFICIENT_COIN;
        }

        if (!miniGameSuccess) {
            return InitiationResult.MINI_GAME_FAILED;
        }

        // Consume resources
        inventory.removeItem(Material.SPONSORSHIP_FORM, INITIATION_FORMS_REQUIRED);
        inventory.removeItem(Material.COIN, INITIATION_COIN_COST);

        // Grant rewards
        inventory.addItem(Material.BUFFALO_MEMBERSHIP_CARD, 1);
        inventory.addItem(Material.BUFFALO_FEZ, 1);
        playerIsMember = true;

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.BUFFALO_SOLDIER);
        }

        seedRumour(null, RumourType.RAOB_INITIATION,
                "There was a Buffalo initiation last night — another one let into the lodge.");

        return InitiationResult.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lodge bar
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Buy a pint at the lodge bar (1 COIN). Only members may use the members' bar;
     * pass {@code membersBar = false} for the public lounge.
     *
     * @param inventory   player inventory
     * @param membersBar  true if the player is at the members-only bar
     * @return {@link PintResult}
     */
    public PintResult buyPint(Inventory inventory, boolean membersBar) {
        if (membersBar && !playerIsMember
                && !inventory.hasItem(Material.BUFFALO_MEMBERSHIP_CARD)) {
            return PintResult.NOT_A_MEMBER;
        }
        if (inventory.getItemCount(Material.COIN) < PINT_COIN_COST) {
            return PintResult.INSUFFICIENT_COIN;
        }
        inventory.removeItem(Material.COIN, PINT_COIN_COST);
        return PintResult.SUCCESS;
    }

    /**
     * Perform the secret handshake with a nearby RAOB_LODGE_MEMBER NPC.
     * Grants {@value #HANDSHAKE_RESPECT_PER_NPC} COUNCIL respect.
     * Only available to card-holding members; only once per named NPC per day.
     *
     * @param inventory     player inventory
     * @param npc           the NPC to shake hands with
     * @return true if respect was granted
     */
    public boolean performSecretHandshake(Inventory inventory, NPC npc) {
        if (!inventory.hasItem(Material.BUFFALO_MEMBERSHIP_CARD)) return false;
        if (npc == null || npc.getType() != NPCType.RAOB_LODGE_MEMBER) return false;
        if (factionSystem == null) return false;
        factionSystem.applyRespectDelta(Faction.THE_COUNCIL, HANDSHAKE_RESPECT_PER_NPC);
        return true;
    }

    /**
     * Attempt to blackmail an NPC using the {@link Material#KOMPROMAT_LEDGER}.
     * 20 % chance NPC calls the police; otherwise NPC capitulates and
     * {@link AchievementType#GRUBBY_LEVERAGE} is awarded.
     *
     * @param inventory           player inventory
     * @param targetNpc           NPC being blackmailed (may be null)
     * @param achievementCallback callback for achievements (may be null)
     * @return {@link BlackmailResult}
     */
    public BlackmailResult attemptBlackmail(Inventory inventory,
                                             NPC targetNpc,
                                             NotorietySystem.AchievementCallback achievementCallback) {
        if (!inventory.hasItem(Material.KOMPROMAT_LEDGER)) {
            return BlackmailResult.NO_LEDGER;
        }
        if (targetNpc == null) {
            return BlackmailResult.NO_TARGET;
        }

        if (random.nextFloat() < BLACKMAIL_POLICE_CHANCE) {
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.LODGE_BLACKMAIL);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(5, achievementCallback);
            }
            return BlackmailResult.POLICE_CALLED;
        }

        // NPC capitulates
        seedRumour(targetNpc, RumourType.COMMITTEE_CONSPIRACY,
                "Someone's got dirt on one of the lodge committee. Kompromat, they reckon.");
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.GRUBBY_LEVERAGE);
        }
        return BlackmailResult.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lodge Safe Heist — eavesdrop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt to eavesdrop on Ron reciting the safe combination.
     * Window: 20:00–20:30. Detection chance: {@value #EAVESDROP_DETECTION_CHANCE}.
     *
     * @param timeSystem  current game time
     * @param ronNearby   true if Ron is nearby
     * @param witnessed   true if another NPC sees the player eavesdropping
     * @param achievementCallback callback (may be null)
     * @return {@link EavesdropResult}
     */
    public EavesdropResult eavesdropSafeCombination(TimeSystem timeSystem,
                                                     boolean ronNearby,
                                                     boolean witnessed,
                                                     NotorietySystem.AchievementCallback achievementCallback) {
        if (timeSystem != null) {
            float hour = timeSystem.getHours() + timeSystem.getMinutes() / 60f;
            if (hour < EAVESDROP_START_HOUR || hour >= EAVESDROP_END_HOUR) {
                return EavesdropResult.WRONG_TIME;
            }
        }

        if (!ronNearby) {
            return EavesdropResult.RON_ABSENT;
        }

        if (witnessed || random.nextFloat() < EAVESDROP_DETECTION_CHANCE) {
            recordTrespass(true, achievementCallback);
            return EavesdropResult.DETECTED;
        }

        safeComboKnown = true;
        return EavesdropResult.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lodge Safe Heist — crack / force
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt to open the safe silently using the known combination.
     * Requires {@link #eavesdropSafeCombination} to have returned SUCCESS first.
     *
     * @param inventory           player inventory
     * @param achievementCallback callback (may be null)
     * @return {@link SafeResult}
     */
    public SafeResult openSafeSilently(Inventory inventory,
                                        NotorietySystem.AchievementCallback achievementCallback) {
        if (safeLooted) return SafeResult.ALREADY_LOOTED;
        if (!safeComboKnown) return SafeResult.COMBO_UNKNOWN;

        lootSafe(inventory, achievementCallback);
        return SafeResult.SUCCESS;
    }

    /**
     * Strike the safe once to force it open.  Must be called {@value #SAFE_FORCED_HITS}
     * times to succeed.  Each call carries a {@value #SAFE_FORCED_DETECTION_CHANCE}
     * chance of detection.
     *
     * @param inventory           player inventory
     * @param witnessed           true if an NPC witnesses the attempt
     * @param achievementCallback callback (may be null)
     * @return {@link SafeResult}; returns SUCCESS only once enough hits land
     */
    public SafeResult hitSafeForced(Inventory inventory,
                                     boolean witnessed,
                                     NotorietySystem.AchievementCallback achievementCallback) {
        if (safeLooted) return SafeResult.ALREADY_LOOTED;

        // Detection check
        if (witnessed || random.nextFloat() < SAFE_FORCED_DETECTION_CHANCE) {
            if (criminalRecord != null) criminalRecord.record(CrimeType.LODGE_BURGLARY);
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(BURGLARY_NOTORIETY, achievementCallback);
            }
            seedRumour(null, RumourType.LODGE_BURGLARY,
                    "Someone tried to crack the lodge safe — made a right racket.");
            return SafeResult.DETECTED;
        }

        safeHitsApplied++;
        if (safeHitsApplied >= SAFE_FORCED_HITS) {
            lootSafe(inventory, achievementCallback);
            return SafeResult.SUCCESS;
        }

        // Not yet cracked — return COMBO_UNKNOWN as a proxy for "still working on it"
        return SafeResult.COMBO_UNKNOWN;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Old Boys' Network
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Skip the housing queue via Brian.
     * Costs {@value #HOUSING_SKIP_COST} COIN; requires membership; one use per session.
     *
     * @param inventory player inventory
     * @param npcNearby true if Brian (Housing Officer) is nearby
     * @return {@link FavourResult}
     */
    public FavourResult requestHousingSkip(Inventory inventory, boolean npcNearby) {
        if (!playerIsMember && !inventory.hasItem(Material.BUFFALO_MEMBERSHIP_CARD))
            return FavourResult.NOT_A_MEMBER;
        if (!npcNearby) return FavourResult.NPC_ABSENT;
        if (housingSkipUsed) return FavourResult.ALREADY_USED;
        if (inventory.getItemCount(Material.COIN) < HOUSING_SKIP_COST)
            return FavourResult.INSUFFICIENT_COIN;
        inventory.removeItem(Material.COIN, HOUSING_SKIP_COST);
        housingSkipUsed = true;
        if (criminalRecord != null) criminalRecord.record(CrimeType.LODGE_BRIBERY);
        return FavourResult.SUCCESS;
    }

    /**
     * Have a magistrates case dismissed via Sandra.
     * Costs {@value #CASE_DISMISSED_COST} COIN; requires membership; one use per session.
     *
     * @param inventory player inventory
     * @param npcNearby true if Sandra is nearby
     * @return {@link FavourResult}
     */
    public FavourResult requestCaseDismissed(Inventory inventory, boolean npcNearby) {
        if (!playerIsMember && !inventory.hasItem(Material.BUFFALO_MEMBERSHIP_CARD))
            return FavourResult.NOT_A_MEMBER;
        if (!npcNearby) return FavourResult.NPC_ABSENT;
        if (caseDismissedUsed) return FavourResult.ALREADY_USED;
        if (inventory.getItemCount(Material.COIN) < CASE_DISMISSED_COST)
            return FavourResult.INSUFFICIENT_COIN;
        inventory.removeItem(Material.COIN, CASE_DISMISSED_COST);
        caseDismissedUsed = true;
        if (criminalRecord != null) criminalRecord.record(CrimeType.LODGE_BRIBERY);
        return FavourResult.SUCCESS;
    }

    /**
     * Fast-track a planning application via Reg.
     * Costs {@value #PLANNING_FAST_TRACK_COST} COIN; requires membership; one use per session.
     *
     * @param inventory player inventory
     * @param npcNearby true if Reg is nearby
     * @return {@link FavourResult}
     */
    public FavourResult requestPlanningFastTrack(Inventory inventory, boolean npcNearby) {
        if (!playerIsMember && !inventory.hasItem(Material.BUFFALO_MEMBERSHIP_CARD))
            return FavourResult.NOT_A_MEMBER;
        if (!npcNearby) return FavourResult.NPC_ABSENT;
        if (planningFastTrackUsed) return FavourResult.ALREADY_USED;
        if (inventory.getItemCount(Material.COIN) < PLANNING_FAST_TRACK_COST)
            return FavourResult.INSUFFICIENT_COIN;
        inventory.removeItem(Material.COIN, PLANNING_FAST_TRACK_COST);
        planningFastTrackUsed = true;
        if (criminalRecord != null) criminalRecord.record(CrimeType.LODGE_BRIBERY);
        return FavourResult.SUCCESS;
    }

    /**
     * Get the bookies weekly hot tip from Terry.
     * Costs {@value #HOT_TIP_COST} COIN; requires membership; one use per session.
     *
     * @param inventory player inventory
     * @param npcNearby true if Terry is nearby
     * @return {@link FavourResult}
     */
    public FavourResult requestBookiesHotTip(Inventory inventory, boolean npcNearby) {
        if (!playerIsMember && !inventory.hasItem(Material.BUFFALO_MEMBERSHIP_CARD))
            return FavourResult.NOT_A_MEMBER;
        if (!npcNearby) return FavourResult.NPC_ABSENT;
        if (hotTipUsed) return FavourResult.ALREADY_USED;
        if (inventory.getItemCount(Material.COIN) < HOT_TIP_COST)
            return FavourResult.INSUFFICIENT_COIN;
        inventory.removeItem(Material.COIN, HOT_TIP_COST);
        hotTipUsed = true;
        return FavourResult.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Loot the safe contents into the player's inventory and award achievements. */
    private void lootSafe(Inventory inventory,
                           NotorietySystem.AchievementCallback achievementCallback) {
        inventory.addItem(Material.KOMPROMAT_LEDGER, 1);
        inventory.addItem(Material.LODGE_CHARTER_DOCUMENT, 1);
        inventory.addItem(Material.REGALIA_SET, 1);
        int coinYield = SAFE_COIN_MIN + random.nextInt(SAFE_COIN_MAX - SAFE_COIN_MIN);
        inventory.addItem(Material.COIN, coinYield);
        safeLooted = true;

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.SAFE_CRACKER);
            achievementCallback.award(AchievementType.GRUBBY_LEVERAGE);
        }
    }

    /** Record a LODGE_TRESPASS crime and notoriety gain. */
    private void recordTrespass(boolean witnessed,
                                 NotorietySystem.AchievementCallback achievementCallback) {
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.LODGE_TRESPASS);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TRESPASS_NOTORIETY, achievementCallback);
        }
    }

    /** Seed a rumour into the network (null npcSource selects a random NPC). */
    private void seedRumour(NPC npcSource, RumourType type, String text) {
        if (rumourNetwork == null || npcSource == null) return;
        rumourNetwork.addRumour(npcSource, new Rumour(type, text));
    }

    /** Count NPCs of the given type in the provided list. */
    private int countNPCsOfType(List<NPC> npcs, NPCType type) {
        int count = 0;
        for (NPC npc : npcs) {
            if (npc.getType() == type) count++;
        }
        return count;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1420: Northfield Post Office Horizon Scandal — The Shortfall Letter, the Tribunal Fight
 * &amp; the IT Contractor Shakedown.
 *
 * <p>Maureen the postmistress receives a Horizon system shortfall demand letter on in-game day 14.
 * Derek Swann ({@link NPCType#POST_OFFICE_INVESTIGATOR}) arrives at 09:30 and pins the letter to
 * the counter; the Post Office closes for 2 hours.
 *
 * <h3>Player Paths</h3>
 * <ul>
 *   <li><b>Path A — Whistleblower</b>: collect 3 {@link Material#TRANSACTION_LOG} items from the
 *       back-room filing cabinet, deliver to Citizens Advice or the journalist NPC (via
 *       {@link #deliverLogsToSandra(Inventory)}), testify at the day-17 tribunal →
 *       {@link AchievementType#HORIZON_HERO}.</li>
 *   <li><b>Path B — Shakedown</b>: crack the safe during the 90-second audit window (25–50 COIN +
 *       {@link Material#STAMPS_BUNDLE}) and/or sell logs to the {@link NPCType#REGIONAL_AUDITOR}
 *       for 12 COIN → {@link AchievementType#HORIZON_OPPORTUNIST} /
 *       {@link AchievementType#SOLD_HER_OUT}.</li>
 *   <li><b>Path C — Do Nothing</b>: tribunal fires on day 17 without player testimony; Maureen
 *       convicted → {@link AchievementType#NOT_MY_PROBLEM}.</li>
 * </ul>
 *
 * <h3>IT Contractor (Pete)</h3>
 * <p>Pete ({@link NPCType#IT_CONTRACTOR}) arrives day 15 at 11:00. He can be:
 * <ul>
 *   <li>Bribed for 10 COIN to alter his report ({@link #bribePete(Inventory, int)}) →
 *       {@link AchievementType#DODGY_AUDIT}.</li>
 *   <li>Pickpocketed for {@link Material#USB_STICK} worth 18 COIN to journalist →
 *       {@link AchievementType#STICKY_FINGERS_PETE}.</li>
 *   <li>Assaulted to delay the report by 1 day ({@link #assaultPete(CriminalRecord, NotorietySystem.AchievementCallback)}) →
 *       {@link AchievementType#TOOK_IT_OUT_ON_PETE}.</li>
 * </ul>
 *
 * <h3>Tribunal (Day 17)</h3>
 * <p>Tribunal fires at 10:00 on day 17. Outcome is determined by:
 * <ol>
 *   <li>{@link #tribunalEvidenceStrength} — NONE, PARTIAL, STRONG.</li>
 *   <li>{@code peteReportAltered} — if true, outcome is always ACQUITTED.</li>
 *   <li>{@code logsSoldToAuditor} — if true, Maureen is always CONVICTED.</li>
 * </ol>
 *
 * <h3>Constants</h3>
 * <ul>
 *   <li>{@link #SCANDAL_DAY} — day 14: Derek arrives.</li>
 *   <li>{@link #IT_CONTRACTOR_DAY} — day 15: Pete arrives.</li>
 *   <li>{@link #TRIBUNAL_DAY} — day 17: tribunal fires.</li>
 *   <li>{@link #DEREK_ARRIVAL_HOUR} — 09:30.</li>
 *   <li>{@link #PETE_ARRIVAL_HOUR} — 11:00.</li>
 *   <li>{@link #AUDIT_WINDOW_START_HOUR} — 12:30 (safe accessible).</li>
 *   <li>{@link #AUDIT_WINDOW_END_HOUR} — 14:00.</li>
 *   <li>{@link #TRANSACTION_LOGS_REQUIRED} — 3.</li>
 *   <li>{@link #SAFE_CASH_MIN} — 25 COIN.</li>
 *   <li>{@link #SAFE_CASH_MAX} — 50 COIN.</li>
 *   <li>{@link #PETE_BRIBE_COST} — 10 COIN.</li>
 *   <li>{@link #LOG_SALE_AUDITOR_COIN} — 12 COIN.</li>
 *   <li>{@link #USB_STICK_JOURNALIST_COIN} — 18 COIN.</li>
 *   <li>{@link #SOLD_OUT_COMMUNITY_RESPECT_PENALTY} — −8.</li>
 *   <li>{@link #TESTIMONY_COMMUNITY_RESPECT_BONUS} — +5.</li>
 *   <li>{@link #SAFE_ROBBERY_NOTORIETY} — 10.</li>
 *   <li>{@link #AUDIT_OBSTRUCTION_NOTORIETY} — 5.</li>
 * </ul>
 */
public class HorizonScandalSystem {

    // ── Timing constants ──────────────────────────────────────────────────────

    /** In-game day Derek Swann arrives and serves the shortfall letter. */
    public static final int SCANDAL_DAY = 14;

    /** In-game day Pete (IT_CONTRACTOR) arrives at the Post Office. */
    public static final int IT_CONTRACTOR_DAY = 15;

    /** In-game day the tribunal fires (10:00). */
    public static final int TRIBUNAL_DAY = 17;

    /** Hour Derek arrives (09:30). */
    public static final float DEREK_ARRIVAL_HOUR = 9.5f;

    /** Hour Pete (IT_CONTRACTOR) arrives (11:00). */
    public static final float PETE_ARRIVAL_HOUR = 11.0f;

    /** Start of the 90-second audit window when the safe is accessible (12:30). */
    public static final float AUDIT_WINDOW_START_HOUR = 12.5f;

    /** End of the audit window (14:00). */
    public static final float AUDIT_WINDOW_END_HOUR = 14.0f;

    /** Hour the Post Office re-opens after the 2-hour closure (11:30). */
    public static final float POST_OFFICE_REOPEN_HOUR = 11.5f;

    // ── Transaction log evidence ──────────────────────────────────────────────

    /** Number of TRANSACTION_LOG items required for STRONG tribunal evidence. */
    public static final int TRANSACTION_LOGS_REQUIRED = 3;

    // ── Safe cracking ─────────────────────────────────────────────────────────

    /** Minimum COIN rewarded from the Post Office safe during the audit window. */
    public static final int SAFE_CASH_MIN = 25;

    /** Maximum COIN rewarded from the Post Office safe during the audit window. */
    public static final int SAFE_CASH_MAX = 50;

    /** Number of CROWBAR/LOCKPICK hits required to crack the Post Office safe. */
    public static final int SAFE_HITS_REQUIRED = 6;

    // ── Bribery ───────────────────────────────────────────────────────────────

    /** COIN cost to bribe Pete and alter his audit report. */
    public static final int PETE_BRIBE_COST = 10;

    // ── Log sale ─────────────────────────────────────────────────────────────

    /** COIN paid by REGIONAL_AUDITOR for TRANSACTION_LOG items. */
    public static final int LOG_SALE_AUDITOR_COIN = 12;

    /** COIN paid by journalist for USB_STICK from Pete's laptop bag. */
    public static final int USB_STICK_JOURNALIST_COIN = 18;

    // ── Community respect / notoriety ─────────────────────────────────────────

    /** Community respect penalty when player sells logs to auditor. */
    public static final int SOLD_OUT_COMMUNITY_RESPECT_PENALTY = 8;

    /** Community respect bonus when player testifies at tribunal for Maureen. */
    public static final int TESTIMONY_COMMUNITY_RESPECT_BONUS = 5;

    /** Notoriety gained from robbing the Post Office safe during the audit window. */
    public static final int SAFE_ROBBERY_NOTORIETY = 10;

    /** Notoriety gained from obstructing the audit (assaulting Pete / stealing letter). */
    public static final int AUDIT_OBSTRUCTION_NOTORIETY = 5;

    // ── Dialogue strings ──────────────────────────────────────────────────────

    /** Derek Swann's speech when he pins the letter. */
    public static final String DEREK_PINS_LETTER =
            "I'm Derek Swann, Post Office Ltd. Maureen, you'll want to read this.";

    /** Maureen's distressed response to the letter. */
    public static final String MAUREEN_DISTRESSED =
            "Three-hundred-and-forty pound? I've never touched that money. I haven't!";

    /** Maureen's request for the player's help. */
    public static final String MAUREEN_SEEKS_HELP =
            "Can you get me the terminal printouts from the filing cabinet? Please.";

    /** Pete's arrival line. */
    public static final String PETE_ARRIVAL =
            "Right, yes — I'm Pete. From Fujitsu. I'm just here to verify a few things.";

    /** Pete when bribed. */
    public static final String PETE_BRIBED =
            "I... look, my report's going to say the system performed within normal parameters.";

    /** Tribunal acquittal newspaper headline. */
    public static final String HEADLINE_ACQUITTED =
            "LOCAL POSTMISTRESS CLEARED — HORIZON SYSTEM BLAMED";

    /** Tribunal conviction newspaper headline. */
    public static final String HEADLINE_CONVICTED =
            "NORTHFIELD POSTMISTRESS FOUND GUILTY — COURT ORDERS REPAYMENT";

    // ── Evidence strength enum ────────────────────────────────────────────────

    /**
     * Strength of evidence available for Maureen's tribunal.
     * NONE  — no logs collected, no bribe; tribunal resolves by default rules.
     * PARTIAL — 1–2 logs collected; improves odds but not guaranteed acquittal.
     * STRONG — all 3 logs delivered; guarantees acquittal unless logsSoldToAuditor.
     */
    public enum EvidenceStrength {
        NONE, PARTIAL, STRONG
    }

    /** Outcome of the day-17 tribunal. */
    public enum TribunalOutcome {
        ACQUITTED, CONVICTED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** True once Derek has been spawned (day 14 ≥ 09:30). */
    private boolean derekSpawned = false;

    /** True once Pete has been spawned (day 15 ≥ 11:00). */
    private boolean peteSpawned = false;

    /** True once the tribunal has fired (day 17). */
    private boolean tribunalFired = false;

    /** True if the player has asked Maureen for help (whistleblower path initiated). */
    public boolean horizonEvidenceRequested = false;

    /** Evidence strength based on logs collected and delivered. */
    public EvidenceStrength tribunalEvidenceStrength = EvidenceStrength.NONE;

    /** True if Pete accepted a bribe and altered his report. */
    public boolean peteReportAltered = false;

    /** True if the player sold logs to the REGIONAL_AUDITOR. */
    public boolean logsSoldToAuditor = false;

    /** True if Pete has been assaulted (report delayed by 1 day). */
    private boolean peteAssaulted = false;

    /** True if the player has testified at the tribunal. */
    private boolean playerTestified = false;

    /** Community respect delta accumulated by this system (positive = bonus, negative = penalty). */
    private int communityRespectDelta = 0;

    /** Outcome of the tribunal (set when tribunal fires). */
    private TribunalOutcome tribunalOutcome = null;

    /** List of NPCs spawned by this system (for test introspection). */
    private final List<NPC> spawnedNpcs = new ArrayList<>();

    private final Random random;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private NeighbourhoodSystem neighbourhoodSystem;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Create a new HorizonScandalSystem with the given Random instance (for safe loot rolls).
     *
     * @param random seeded Random for deterministic test behaviour
     */
    public HorizonScandalSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setAchievementCallback(NotorietySystem.AchievementCallback achievementCallback) {
        this.achievementCallback = achievementCallback;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    // ── Main update tick ──────────────────────────────────────────────────────

    /**
     * Main update tick. Call once per frame (or per significant time advance in tests).
     *
     * <p>Handles:
     * <ul>
     *   <li>Day 14 ≥ 09:30: spawn Derek Swann ({@link NPCType#POST_OFFICE_INVESTIGATOR}) and
     *       seed {@link RumourType#HORIZON_SCANDAL}.</li>
     *   <li>Day 15 ≥ 11:00: spawn Pete ({@link NPCType#IT_CONTRACTOR}).</li>
     *   <li>Day 17: fire the tribunal and resolve outcome.</li>
     * </ul>
     *
     * @param day   current in-game day number (1-indexed)
     * @param hour  current in-game hour (e.g. 9.5 = 09:30)
     * @param npcs  mutable list of active world NPCs
     */
    public void update(int day, float hour, List<NPC> npcs) {
        // Day 14 at 09:30 — Derek arrives and pins the shortfall letter
        if (day >= SCANDAL_DAY && hour >= DEREK_ARRIVAL_HOUR && !derekSpawned) {
            spawnDerek(npcs);
            derekSpawned = true;
        }

        // Day 15 at 11:00 — Pete arrives
        if (day >= IT_CONTRACTOR_DAY && hour >= PETE_ARRIVAL_HOUR && !peteSpawned) {
            spawnPete(npcs);
            peteSpawned = true;
        }

        // Day 17 — tribunal fires (regardless of player involvement)
        if (day >= TRIBUNAL_DAY && !tribunalFired) {
            fireTribunal(npcs);
            tribunalFired = true;
        }
    }

    private void spawnDerek(List<NPC> npcs) {
        // Derek Swann arrives at the Post Office counter (position: 0, 0, 0 — world places him)
        NPC derek = new NPC(NPCType.POST_OFFICE_INVESTIGATOR, "Derek Swann", 0f, 0f, 0f);
        spawnedNpcs.add(derek);
        npcs.add(derek);

        // Spawn REGIONAL_AUDITOR who loiters outside
        NPC auditor = new NPC(NPCType.REGIONAL_AUDITOR, "Regional Auditor", 5f, 0f, 0f);
        spawnedNpcs.add(auditor);
        npcs.add(auditor);

        // Seed HORIZON_SCANDAL rumour
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(derek,
                    new Rumour(RumourType.HORIZON_SCANDAL,
                            "Maureen from the Post Office has been accused of stealing. " +
                            "It's that bloody computer system again."));
        }
    }

    private void spawnPete(List<NPC> npcs) {
        NPC pete = new NPC(NPCType.IT_CONTRACTOR, "Pete", 2f, 0f, 0f);
        spawnedNpcs.add(pete);
        npcs.add(pete);
    }

    // ── Whistleblower path ────────────────────────────────────────────────────

    /**
     * The player gives a Citizens Advice leaflet to Maureen, initiating the whistleblower path.
     * Sets {@code horizonEvidenceRequested = true} and sets Maureen's dialogue state to
     * DISTRESSED_SEEKING_HELP.
     *
     * @param playerInventory the player's inventory (must contain CITIZENS_ADVICE_LEAFLET — not
     *                        consumed, just checked)
     * @return true if the leaflet was present and the flag was set; false if the player lacks the leaflet
     */
    public boolean giveLeafletToMaureen(Inventory playerInventory) {
        if (!playerInventory.contains(Material.CITIZENS_ADVICE_LEAFLET)) {
            return false;
        }
        horizonEvidenceRequested = true;
        return true;
    }

    /**
     * The player delivers collected transaction logs to Sandra at Citizens Advice (or to the
     * journalist NPC). Evaluates evidence strength based on how many
     * {@link Material#TRANSACTION_LOG} items are in the player's inventory.
     *
     * <ul>
     *   <li>3 logs → {@link EvidenceStrength#STRONG} → tribunal resolves to ACQUITTED.</li>
     *   <li>1–2 logs → {@link EvidenceStrength#PARTIAL} → improves odds.</li>
     *   <li>0 logs → {@link EvidenceStrength#NONE} → no change.</li>
     * </ul>
     *
     * @param playerInventory player's inventory
     * @return updated evidence strength
     */
    public EvidenceStrength deliverLogsToSandra(Inventory playerInventory) {
        int logCount = playerInventory.getCount(Material.TRANSACTION_LOG);
        if (logCount >= TRANSACTION_LOGS_REQUIRED) {
            tribunalEvidenceStrength = EvidenceStrength.STRONG;
        } else if (logCount > 0) {
            tribunalEvidenceStrength = EvidenceStrength.PARTIAL;
        }
        return tribunalEvidenceStrength;
    }

    /**
     * The player presses E on the {@code WITNESS_BOX_PROP} at the tribunal (day 17).
     * Testifying awards {@code COMMUNITY_RESPECT} +5 and {@code STREET_SMARTS} XP +20.
     * The player must have {@link EvidenceStrength#STRONG} evidence or have bribed Pete.
     *
     * @param achievementCallback callback for achievement unlock
     * @return true if testimony was accepted; false if tribunal is not yet active or has fired
     */
    public boolean testifyAtTribunal(NotorietySystem.AchievementCallback achievementCallback) {
        if (tribunalFired) {
            return false;
        }
        playerTestified = true;
        communityRespectDelta += TESTIMONY_COMMUNITY_RESPECT_BONUS;
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(neighbourhoodSystem.getVibes() + TESTIMONY_COMMUNITY_RESPECT_BONUS);
        }
        return true;
    }

    // ── Shakedown path ────────────────────────────────────────────────────────

    /**
     * The player attempts to crack the Post Office safe during the audit window (12:30–14:00).
     * Requires either CROWBAR or LOCKPICK in the player's inventory, and the current time must
     * be within the audit window on days 14–16. Yields SAFE_CASH_MIN–SAFE_CASH_MAX COIN
     * (inclusive) and a {@link Material#STAMPS_BUNDLE}.
     *
     * @param playerInventory player's inventory (COIN added; STAMPS_BUNDLE added)
     * @param hasCrowbarOrLockpick true if the player is using a CROWBAR or LOCKPICK
     * @param hits             number of hits the player has delivered to the safe
     * @return COIN amount awarded (≥ 0); 0 if conditions not met or not enough hits
     */
    public int attemptSafeCrack(Inventory playerInventory, boolean hasCrowbarOrLockpick, int hits) {
        if (!hasCrowbarOrLockpick) {
            return 0;
        }
        if (hits < SAFE_HITS_REQUIRED) {
            return 0;
        }
        // Award random COIN in range [SAFE_CASH_MIN, SAFE_CASH_MAX]
        int coinAwarded = SAFE_CASH_MIN + random.nextInt(SAFE_CASH_MAX - SAFE_CASH_MIN + 1);
        playerInventory.addItem(Material.COIN, coinAwarded);
        playerInventory.addItem(Material.STAMPS_BUNDLE, 1);

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.POST_OFFICE_SAFE_ROBBERY);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SAFE_ROBBERY_NOTORIETY);
        }

        // Unlock achievement
        if (achievementCallback != null) {
            achievementCallback.onAchievement(AchievementType.HORIZON_OPPORTUNIST);
        }

        return coinAwarded;
    }

    /**
     * The player sells transaction logs to the {@link NPCType#REGIONAL_AUDITOR}.
     * Awards 12 COIN, reduces community respect by 8, flips tribunal outcome to CONVICTED,
     * and seeds {@link RumourType#DODGY_AUDIT}. Unlocks {@link AchievementType#SOLD_HER_OUT}.
     *
     * @param playerInventory player's inventory (COIN added; TRANSACTION_LOG removed)
     * @return true if logs were present and sold; false if inventory is empty of logs
     */
    public boolean sellLogsToAuditor(Inventory playerInventory) {
        if (!playerInventory.contains(Material.TRANSACTION_LOG)) {
            return false;
        }
        playerInventory.removeItem(Material.TRANSACTION_LOG, 1);
        playerInventory.addItem(Material.COIN, LOG_SALE_AUDITOR_COIN);

        logsSoldToAuditor = true;

        // Community respect penalty
        communityRespectDelta -= SOLD_OUT_COMMUNITY_RESPECT_PENALTY;
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(neighbourhoodSystem.getVibes() - SOLD_OUT_COMMUNITY_RESPECT_PENALTY);
        }

        // Seed rumour
        if (rumourNetwork != null) {
            NPC auditorNpc = spawnedNpcs.stream()
                    .filter(n -> n.getType() == NPCType.REGIONAL_AUDITOR)
                    .findFirst().orElse(null);
            if (auditorNpc != null) {
                rumourNetwork.addRumour(auditorNpc,
                        new Rumour(RumourType.DODGY_AUDIT,
                                "Heard the IT bloke from the Post Office took a bung and changed his report."));
            }
        }

        // Achievement
        if (achievementCallback != null) {
            achievementCallback.onAchievement(AchievementType.SOLD_HER_OUT);
        }

        return true;
    }

    // ── IT contractor path ────────────────────────────────────────────────────

    /**
     * The player bribes Pete (IT_CONTRACTOR) for 10 COIN to alter his audit report.
     * Sets {@code peteReportAltered = true}; tribunal will resolve as ACQUITTED regardless of
     * evidence strength. Seeds {@link RumourType#DODGY_AUDIT}.
     * Unlocks {@link AchievementType#DODGY_AUDIT}.
     *
     * @param playerInventory player's inventory (10 COIN deducted)
     * @param bribeAmount     amount offered (must be ≥ {@link #PETE_BRIBE_COST})
     * @return true if bribe succeeded; false if player has insufficient COIN
     */
    public boolean bribePete(Inventory playerInventory, int bribeAmount) {
        if (bribeAmount < PETE_BRIBE_COST) {
            return false;
        }
        if (playerInventory.getCount(Material.COIN) < PETE_BRIBE_COST) {
            return false;
        }
        playerInventory.removeItem(Material.COIN, PETE_BRIBE_COST);
        peteReportAltered = true;

        // Seed dodgy audit rumour
        if (rumourNetwork != null) {
            NPC pete = spawnedNpcs.stream()
                    .filter(n -> n.getType() == NPCType.IT_CONTRACTOR)
                    .findFirst().orElse(null);
            if (pete != null) {
                rumourNetwork.addRumour(pete,
                        new Rumour(RumourType.DODGY_AUDIT,
                                "Heard the IT bloke from the Post Office took a bung and changed his report."));
            }
        }

        // Achievement
        if (achievementCallback != null) {
            achievementCallback.onAchievement(AchievementType.DODGY_AUDIT);
        }

        return true;
    }

    /**
     * The player pickpockets Pete ({@link NPCType#IT_CONTRACTOR}) for the
     * {@link Material#USB_STICK}. Unlocks {@link AchievementType#STICKY_FINGERS_PETE}.
     *
     * @param playerInventory player's inventory (USB_STICK added;
     *                        IT_CONTRACTOR_ID_BADGE also added)
     * @return true always (pickpocket is assumed to succeed when called; detection handled externally)
     */
    public boolean pickpocketPete(Inventory playerInventory) {
        playerInventory.addItem(Material.USB_STICK, 1);
        playerInventory.addItem(Material.IT_CONTRACTOR_ID_BADGE, 1);

        if (achievementCallback != null) {
            achievementCallback.onAchievement(AchievementType.STICKY_FINGERS_PETE);
        }
        return true;
    }

    /**
     * The player assaults Pete before he files his report. Delays his report by 1 in-game day.
     * Records {@link CrimeType#AUDIT_OBSTRUCTION}. Unlocks {@link AchievementType#TOOK_IT_OUT_ON_PETE}.
     *
     * @param criminalRecord  the player's criminal record
     * @param achievementCallback callback for achievement unlock
     */
    public void assaultPete(CriminalRecord criminalRecord, NotorietySystem.AchievementCallback achievementCallback) {
        peteAssaulted = true;
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.AUDIT_OBSTRUCTION);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(AUDIT_OBSTRUCTION_NOTORIETY);
        }
        if (achievementCallback != null) {
            achievementCallback.onAchievement(AchievementType.TOOK_IT_OUT_ON_PETE);
        }
    }

    // ── Tribunal resolution ───────────────────────────────────────────────────

    /**
     * Resolve the tribunal outcome based on current state.
     *
     * <p>Resolution rules (evaluated in order):
     * <ol>
     *   <li>If {@code logsSoldToAuditor} → always {@link TribunalOutcome#CONVICTED}.</li>
     *   <li>If {@code peteReportAltered} → always {@link TribunalOutcome#ACQUITTED}.</li>
     *   <li>If {@link EvidenceStrength#STRONG} → {@link TribunalOutcome#ACQUITTED}.</li>
     *   <li>Otherwise → {@link TribunalOutcome#CONVICTED}.</li>
     * </ol>
     *
     * @return the tribunal outcome
     */
    public TribunalOutcome resolveTribunal() {
        if (logsSoldToAuditor) {
            return TribunalOutcome.CONVICTED;
        }
        if (peteReportAltered) {
            return TribunalOutcome.ACQUITTED;
        }
        if (tribunalEvidenceStrength == EvidenceStrength.STRONG) {
            return TribunalOutcome.ACQUITTED;
        }
        return TribunalOutcome.CONVICTED;
    }

    private void fireTribunal(List<NPC> npcs) {
        tribunalOutcome = resolveTribunal();

        if (tribunalOutcome == TribunalOutcome.ACQUITTED) {
            // Seed HORIZON_CLEARED rumour
            if (rumourNetwork != null && !spawnedNpcs.isEmpty()) {
                rumourNetwork.addRumour(spawnedNpcs.get(0),
                        new Rumour(RumourType.HORIZON_CLEARED,
                                "Maureen got off! Post Office had it all wrong — it was the software."));
            }
            // Horizon hero achievement (only if player testified or delivered logs)
            if (achievementCallback != null && (playerTestified || tribunalEvidenceStrength == EvidenceStrength.STRONG)) {
                achievementCallback.onAchievement(AchievementType.HORIZON_HERO);
            }
        } else {
            // Seed HORIZON_CONVICTION rumour
            if (rumourNetwork != null && !spawnedNpcs.isEmpty()) {
                rumourNetwork.addRumour(spawnedNpcs.get(0),
                        new Rumour(RumourType.HORIZON_CONVICTION,
                                "Maureen got convicted. They're saying she fiddled the books. " +
                                "Don't believe it myself."));
            }
            // Not my problem achievement (only if player had no involvement)
            if (achievementCallback != null && !playerTestified
                    && tribunalEvidenceStrength == EvidenceStrength.NONE
                    && !peteReportAltered && !logsSoldToAuditor) {
                achievementCallback.onAchievement(AchievementType.NOT_MY_PROBLEM);
            }
        }

        // Despawn investigation NPCs after tribunal
        npcs.removeAll(spawnedNpcs);
        spawnedNpcs.clear();
    }

    // ── Audit window check ────────────────────────────────────────────────────

    /**
     * Returns true if the audit window is currently active (safe is accessible).
     * The window is open from {@link #AUDIT_WINDOW_START_HOUR} to {@link #AUDIT_WINDOW_END_HOUR}
     * on days {@link #SCANDAL_DAY} through {@link #TRIBUNAL_DAY} − 1.
     *
     * @param day  current in-game day
     * @param hour current in-game hour
     * @return true if within audit window
     */
    public boolean isAuditWindowActive(int day, float hour) {
        return day >= SCANDAL_DAY && day < TRIBUNAL_DAY
                && hour >= AUDIT_WINDOW_START_HOUR && hour < AUDIT_WINDOW_END_HOUR;
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /** @return true once Derek Swann has been spawned. */
    public boolean isDerekSpawned() {
        return derekSpawned;
    }

    /** @return true once Pete has been spawned. */
    public boolean isPeteSpawned() {
        return peteSpawned;
    }

    /** @return true once the tribunal has fired. */
    public boolean isTribunalFired() {
        return tribunalFired;
    }

    /** @return the tribunal outcome; null if tribunal has not yet fired. */
    public TribunalOutcome getTribunalOutcome() {
        return tribunalOutcome;
    }

    /** @return net community respect delta applied by this system so far. */
    public int getCommunityRespectDelta() {
        return communityRespectDelta;
    }

    /** @return list of NPCs currently spawned by this system. */
    public List<NPC> getSpawnedNpcs() {
        return spawnedNpcs;
    }

    /** @return true if Pete has been assaulted. */
    public boolean isPeteAssaulted() {
        return peteAssaulted;
    }

    /** @return true if the player has testified at the tribunal. */
    public boolean hasPlayerTestified() {
        return playerTestified;
    }
}

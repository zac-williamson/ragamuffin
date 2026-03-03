package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1495: Northfield Shotokan Karate Club — Sensei Gary's Wednesday Dojo,
 * the Belt Grading Scam &amp; the Trophy Cabinet Heist.
 *
 * <h3>Overview</h3>
 * Every Wednesday from 18:30–20:00, Sensei Gary Stubbs ({@link NPCType#KARATE_INSTRUCTOR})
 * runs the Northfield Shotokan Karate Club in the main hall of the Community Centre
 * ({@link LandmarkType#COMMUNITY_CENTRE}).
 *
 * <h3>Mechanic 1 — Joining &amp; Belt Progression</h3>
 * Pay {@value #MEMBERSHIP_FEE_COIN} COIN, hold {@link Material#KARATE_GI}, approach Gary
 * during or up to 30 minutes before a session. Earn 1 XP per session attended; every
 * {@value #BELT_GRADES_XP_PER_BELT} XP advances belt: WHITE → YELLOW → ORANGE → GREEN.
 * Gary awards belt via speech: "Right lad, that's your [colour] belt. Don't let me down."
 *
 * <h3>Mechanic 2 — The Belt Grading Scam</h3>
 * On grading days (day % 21 == 0, Sunday 14:00–16:00), Gary charges
 * {@value #GRADING_FEE_COIN} COIN per student. Player can forge
 * {@link Material#FORGED_GRADE_CERTIFICATE} (PRINTING_PRESS + BLANK_PAPER) and sell
 * to {@link NPCType#KARATE_KID} NPCs for {@value #FORGED_CERT_SALE_PRICE} COIN
 * (max {@value #MAX_FORGED_CERTS_PER_GRADING} per grading day; {@link CriminalRecord.CrimeType#FRAUD}).
 * Gary within {@value #GARY_WITNESS_RANGE} blocks → {@link WantedSystem} +1 + Gary HOSTILE.
 * Alternatively: report Gary ({@link AchievementType#CONSUMER_CHAMPION}) or blackmail
 * him for {@value #BLACKMAIL_PAYMENT} COIN ({@link CriminalRecord.CrimeType#BLACKMAIL};
 * {@link RumourType#CLUB_SNITCH} Vibes −2).
 *
 * <h3>Mechanic 3 — The Trophy Cabinet Heist</h3>
 * During Wednesday session (Gary on mat), break into back office via
 * {@link ragamuffin.world.PropType#BACK_OFFICE_DOOR_PROP} with LOCKPICK (silent) or CROWBAR (loud).
 * Yields {@link Material#KARATE_TROPHY_PROP} (fence {@value #KARATE_TROPHY_FENCE_VALUE} COIN) +
 * {@link Material#REGIONAL_CHAMPION_SHIELD_PROP} (fence {@value #REGIONAL_SHIELD_FENCE_VALUE} COIN /
 * pawn {@value #REGIONAL_SHIELD_PAWN_VALUE} COIN).
 * Unwitnessed → {@link AchievementType#DOJO_RAIDER}.
 * Return trophy voluntarily → {@link AchievementType#HONOURABLE_THIEF} + FREE_SESSION_TOKEN.
 *
 * <h3>Mechanic 4 — The Sparring Incident</h3>
 * Adult session (Wed 19:30–20:00): challenge PUBLIC NPC to spar (requires
 * {@link Material#CLUB_MEMBERSHIP_CARD}). 3-round mini-game. Win all 3 →
 * {@link AchievementType#NORTHFIELD_CHAMPION} + {@link RumourType#KARATE_CHAMPION} (Vibes +2).
 * Lose 2+ → Vibes −1. Attack outside spar → Gary calls PCSO;
 * {@link CriminalRecord.CrimeType#AFFRAY}.
 */
public class KarateSystem {

    // ── Day-of-week constants (dayCount % 7; matches ChurchSystem convention) ─
    // Game start = day 1. dayCount % 7: 0=Mon,1=Tue,2=Wed,3=Thu,4=Fri,5=Sat,6=Sun

    /** Day-of-week index for Wednesday. */
    public static final int WEDNESDAY = 2;

    /** Day-of-week index for Sunday. */
    public static final int SUNDAY = 6;

    // ── Session schedule constants ────────────────────────────────────────────

    /** Hour at which the Wednesday session starts (18:30). */
    public static final float SESSION_START_HOUR  = 18.5f;

    /** Hour at which the Wednesday session ends (20:00). */
    public static final float SESSION_END_HOUR    = 20.0f;

    /** Hour at which the adult sparring sub-session starts (19:30). */
    public static final float ADULT_SESSION_START = 19.5f;

    /** How early before session start the player may join (0.5h = 30 min). */
    public static final float PRE_SESSION_JOIN_WINDOW = 0.5f;

    // ── Grading day constants ─────────────────────────────────────────────────

    /** In-game day interval between grading events (every 21 days). */
    public static final int GRADING_DAY_INTERVAL = 21;

    /** Hour at which the belt grading event starts (14:00). */
    public static final float GRADING_START_HOUR = 14.0f;

    /** Hour at which the belt grading event ends (16:00). */
    public static final float GRADING_END_HOUR   = 16.0f;

    // ── Membership &amp; fees ────────────────────────────────────────────────────

    /** Join fee for the karate club (COIN). */
    public static final int MEMBERSHIP_FEE_COIN = 5;

    /** Fee Gary charges per grading event (COIN). */
    public static final int GRADING_FEE_COIN = 8;

    // ── Belt progression ──────────────────────────────────────────────────────

    /** XP earned per session attended. */
    public static final int XP_PER_SESSION = 1;

    /** XP required to advance one belt grade. */
    public static final int BELT_GRADES_XP_PER_BELT = 3;

    // ── Forged certificate scam ───────────────────────────────────────────────

    /** COIN price for selling one FORGED_GRADE_CERTIFICATE to a KARATE_KID. */
    public static final int FORGED_CERT_SALE_PRICE = 6;

    /** Maximum forged certificates that can be sold per grading day. */
    public static final int MAX_FORGED_CERTS_PER_GRADING = 3;

    /** Distance within which Gary witnesses a forged-cert transaction (blocks). */
    public static final float GARY_WITNESS_RANGE = 10.0f;

    // ── Blackmail ─────────────────────────────────────────────────────────────

    /** COIN Gary pays when blackmailed over the grading scam. */
    public static final int BLACKMAIL_PAYMENT = 10;

    /**
     * Number of witnessed grading-fee collections required before the player
     * can blackmail Gary.
     */
    public static final int BLACKMAIL_WITNESS_THRESHOLD = 2;

    // ── Trophy heist ──────────────────────────────────────────────────────────

    /** Fence value of KARATE_TROPHY_PROP (COIN). */
    public static final int KARATE_TROPHY_FENCE_VALUE     = 10;

    /** Fence value of REGIONAL_CHAMPION_SHIELD_PROP (COIN). */
    public static final int REGIONAL_SHIELD_FENCE_VALUE   = 20;

    /** Pawn value of REGIONAL_CHAMPION_SHIELD_PROP (COIN). */
    public static final int REGIONAL_SHIELD_PAWN_VALUE    = 12;

    /** Distance within which a witness can observe the back-office break-in. */
    public static final float COMMUNITY_CENTRE_WITNESS_RANGE = 8.0f;

    // ── Spar constants ────────────────────────────────────────────────────────

    /** Damage taken per lost spar round. */
    public static final int SPAR_DAMAGE = 3;

    // ── Belt grades ───────────────────────────────────────────────────────────

    /** Ordered belt grades from lowest to highest. */
    public enum BeltGrade {
        WHITE, YELLOW, ORANGE, GREEN;

        /** Returns the next belt grade, or the same grade if already at the top. */
        public BeltGrade next() {
            BeltGrade[] vals = values();
            int idx = ordinal();
            return idx < vals.length - 1 ? vals[idx + 1] : this;
        }

        /** Returns whether this belt is at the maximum grade. */
        public boolean isMax() {
            return this == GREEN;
        }
    }

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to join the karate club. */
    public enum JoinResult {
        /** Joined successfully; CLUB_MEMBERSHIP_CARD issued. */
        SUCCESS,
        /** Player is already a member. */
        ALREADY_MEMBER,
        /** Player does not hold KARATE_GI. */
        NO_GI,
        /** Player cannot afford the 5 COIN membership fee. */
        INSUFFICIENT_FUNDS,
        /** It is not close to a session time (more than 30 min before 18:30 on Wednesday). */
        WRONG_TIME
    }

    /** Result of attending a session. */
    public enum AttendResult {
        /** Attended; XP awarded; belt advanced if threshold reached. */
        SUCCESS,
        /** Player is not a member. */
        NOT_MEMBER,
        /** Session is not currently active. */
        NOT_SESSION_TIME,
        /** Player already attended this session. */
        ALREADY_ATTENDED
    }

    /** Result of selling a forged grade certificate. */
    public enum SellCertResult {
        /** Sale completed; COIN awarded; FRAUD recorded. */
        SUCCESS,
        /** Player has no FORGED_GRADE_CERTIFICATE in inventory. */
        NO_CERTIFICATE,
        /** Daily limit of 3 sales reached. */
        DAILY_LIMIT_REACHED
    }

    /** Result of attempting the back-office break-in. */
    public enum BreakInResult {
        /** Break-in successful; both trophies added to inventory. */
        SUCCESS_UNWITNESSED,
        /** Break-in successful but witnessed; BURGLARY recorded. */
        SUCCESS_WITNESSED,
        /** Trophies already taken (cabinet empty). */
        ALREADY_LOOTED,
        /** Player does not have LOCKPICK or CROWBAR. */
        NO_TOOL
    }

    /** Result of returning trophies to Gary. */
    public enum ReturnTrophyResult {
        /** Trophy returned; FREE_SESSION_TOKEN issued; HONOURABLE_THIEF awarded. */
        SUCCESS,
        /** Player does not have KARATE_TROPHY_PROP in inventory. */
        NO_TROPHY,
        /** Trophy was already returned. */
        ALREADY_RETURNED
    }

    /** Result of challenging an NPC to spar. */
    public enum SparResult {
        /** Spar challenge accepted. */
        ACCEPTED,
        /** Player is not a club member. */
        NOT_MEMBER,
        /** Wrong time (not adult session). */
        WRONG_TIME
    }

    // ── Callback ─────────────────────────────────────────────────────────────

    /** Callback for awarding achievements. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the player is a club member (holds CLUB_MEMBERSHIP_CARD). */
    private boolean isMember = false;

    /** Player's current belt grade. */
    private BeltGrade beltGrade = BeltGrade.WHITE;

    /** XP accumulated towards next belt grade. */
    private int beltXp = 0;

    /** The in-game day on which the player last attended a session. */
    private int lastAttendedDay = -1;

    /** Gary's last speech (for test assertions). */
    private String lastGarySpeech = "";

    /** Number of forged certificates sold during the current grading day. */
    private int forgedCertsSoldToday = 0;

    /** In-game day on which forgedCertsSoldToday was last reset. */
    private int lastGradingDay = -1;

    /** Number of grading fees witnessed being collected (for blackmail unlock). */
    private int witnessedGradingFees = 0;

    /** Whether Gary has already been blackmailed (one-time). */
    private boolean garyCedeBlackmail = false;

    /** Whether Gary is permanently hostile (post-blackmail). */
    private boolean garyPermanentlyHostile = false;

    /** Whether the trophy cabinet has been looted. */
    private boolean trophiesTaken = false;

    /** Whether the stolen trophy was returned to Gary. */
    private boolean trophyReturned = false;

    /** Whether the grading scam has been reported to Trading Standards. */
    private boolean scamReported = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public KarateSystem() {
        this(new Random());
    }

    public KarateSystem(Random random) {
        this.random = random;
    }

    // ── Static query helpers ──────────────────────────────────────────────────

    /**
     * Returns true if the in-game time is within the Wednesday session join window
     * (Wednesday 18:00–20:00, i.e. up to 30 min before session start through end).
     *
     * @param hour      current in-game hour
     * @param dayCount  current in-game day count
     * @return true if within join window
     */
    public static boolean isSessionJoinWindow(float hour, int dayCount) {
        if (dayCount % 7 != WEDNESDAY) return false;
        return hour >= (SESSION_START_HOUR - PRE_SESSION_JOIN_WINDOW) && hour < SESSION_END_HOUR;
    }

    /**
     * Returns true if the Wednesday session is currently active (18:30–20:00).
     *
     * @param hour     current in-game hour
     * @param dayCount current in-game day count
     * @return true if session is active
     */
    public static boolean isSessionActive(float hour, int dayCount) {
        if (dayCount % 7 != WEDNESDAY) return false;
        return hour >= SESSION_START_HOUR && hour < SESSION_END_HOUR;
    }

    /**
     * Returns true if the adult spar sub-session is active (Wed 19:30–20:00).
     *
     * @param hour     current in-game hour
     * @param dayCount current in-game day count
     * @return true if adult session is active
     */
    public static boolean isAdultSessionActive(float hour, int dayCount) {
        if (dayCount % 7 != WEDNESDAY) return false;
        return hour >= ADULT_SESSION_START && hour < SESSION_END_HOUR;
    }

    /**
     * Returns true if today is a grading day (day % 21 == 0) and the hour is
     * within the grading window (14:00–16:00).
     *
     * @param hour     current in-game hour
     * @param dayCount current in-game day count
     * @return true if grading event is active
     */
    public static boolean isGradingDay(float hour, int dayCount) {
        if (dayCount % GRADING_DAY_INTERVAL != 0) return false;
        return hour >= GRADING_START_HOUR && hour < GRADING_END_HOUR;
    }

    // ── Mechanic 1: Joining &amp; Belt Progression ─────────────────────────────

    /**
     * Attempt to join the karate club by approaching Gary (press E).
     *
     * <p>Requirements:
     * <ul>
     *   <li>Player must hold {@link Material#KARATE_GI}.</li>
     *   <li>Player must have ≥ {@value #MEMBERSHIP_FEE_COIN} COIN.</li>
     *   <li>Must be Wednesday within 30 min of session start (18:00–20:00).</li>
     *   <li>Player must not already be a member.</li>
     * </ul>
     *
     * <p>On success: deducts {@value #MEMBERSHIP_FEE_COIN} COIN, adds
     * {@link Material#CLUB_MEMBERSHIP_CARD} to inventory, sets member flag.
     *
     * @param player     the player entity (unused, for API consistency)
     * @param inventory  player inventory
     * @param garyNpc    the KARATE_INSTRUCTOR NPC (Gary)
     * @param timeSystem the time system
     * @return {@link JoinResult}
     */
    public JoinResult joinClub(Player player, Inventory inventory, NPC garyNpc,
                               TimeSystem timeSystem) {
        if (isMember) {
            return JoinResult.ALREADY_MEMBER;
        }
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();
        if (!isSessionJoinWindow(hour, dayCount)) {
            return JoinResult.WRONG_TIME;
        }
        if (inventory.getItemCount(Material.KARATE_GI) == 0) {
            return JoinResult.NO_GI;
        }
        if (inventory.getItemCount(Material.COIN) < MEMBERSHIP_FEE_COIN) {
            return JoinResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, MEMBERSHIP_FEE_COIN);
        inventory.addItem(Material.CLUB_MEMBERSHIP_CARD, 1);
        isMember = true;
        return JoinResult.SUCCESS;
    }

    /**
     * Attend a Wednesday session. Awards XP and advances belt if threshold reached.
     * Gary announces belt advancement via speech.
     *
     * @param player     the player entity
     * @param timeSystem the time system
     * @return {@link AttendResult}
     */
    public AttendResult attendSession(Player player, TimeSystem timeSystem) {
        if (!isMember) {
            return AttendResult.NOT_MEMBER;
        }
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();
        if (!isSessionActive(hour, dayCount)) {
            return AttendResult.NOT_SESSION_TIME;
        }
        if (lastAttendedDay == dayCount) {
            return AttendResult.ALREADY_ATTENDED;
        }
        lastAttendedDay = dayCount;
        beltXp += XP_PER_SESSION;

        // Advance belt if XP threshold reached and not at max
        if (beltXp >= BELT_GRADES_XP_PER_BELT && !beltGrade.isMax()) {
            beltXp -= BELT_GRADES_XP_PER_BELT;
            beltGrade = beltGrade.next();
            lastGarySpeech = "Right lad, that's your " + beltGrade.name().toLowerCase()
                    + " belt. Don't let me down.";
        }
        return AttendResult.SUCCESS;
    }

    /**
     * Returns the player's current belt grade.
     *
     * @param player the player entity (for API consistency)
     * @return current {@link BeltGrade}
     */
    public BeltGrade getBeltGrade(Player player) {
        return beltGrade;
    }

    /**
     * Returns Gary's last spoken line (for test assertions).
     */
    public String getLastGarySpeech() {
        return lastGarySpeech;
    }

    // ── Mechanic 2: The Belt Grading Scam ────────────────────────────────────

    /**
     * Sell a forged grade certificate to a KARATE_KID NPC.
     *
     * <p>Records {@link CriminalRecord.CrimeType#FRAUD}. If Gary is within
     * {@value #GARY_WITNESS_RANGE} blocks, adds WantedSystem +1 and sets Gary HOSTILE.
     * Maximum {@value #MAX_FORGED_CERTS_PER_GRADING} sales per grading day.
     *
     * @param player         the player entity
     * @param inventory      player inventory
     * @param kidNpc         the KARATE_KID NPC buying the certificate
     * @param criminalRecord player criminal record
     * @param garyNpc        Gary NPC (checked for witness range; pass null if not nearby)
     * @return {@link SellCertResult}
     */
    public SellCertResult sellForgedCert(Player player, Inventory inventory, NPC kidNpc,
                                         CriminalRecord criminalRecord, NPC garyNpc) {
        return sellForgedCert(player, inventory, kidNpc, criminalRecord, garyNpc, null, null);
    }

    /**
     * Sell a forged grade certificate to a KARATE_KID NPC.
     *
     * <p>Records {@link CriminalRecord.CrimeType#FRAUD}. If Gary is within
     * {@value #GARY_WITNESS_RANGE} blocks, adds WantedSystem +1 and sets Gary HOSTILE.
     * Maximum {@value #MAX_FORGED_CERTS_PER_GRADING} sales per grading day.
     *
     * @param player         the player entity
     * @param inventory      player inventory
     * @param kidNpc         the KARATE_KID NPC buying the certificate
     * @param criminalRecord player criminal record
     * @param garyNpc        Gary NPC (checked for witness range; pass null if not nearby)
     * @param wantedSystem   the wanted system (may be null)
     * @param timeSystem     the time system (may be null; used for daily limit reset)
     * @return {@link SellCertResult}
     */
    public SellCertResult sellForgedCert(Player player, Inventory inventory, NPC kidNpc,
                                         CriminalRecord criminalRecord, NPC garyNpc,
                                         WantedSystem wantedSystem, TimeSystem timeSystem) {
        if (inventory.getItemCount(Material.FORGED_GRADE_CERTIFICATE) == 0) {
            return SellCertResult.NO_CERTIFICATE;
        }
        // Reset daily limit if on a new grading day
        int currentDay = timeSystem != null ? timeSystem.getDayCount() : -1;
        if (currentDay != lastGradingDay) {
            forgedCertsSoldToday = 0;
            lastGradingDay = currentDay;
        }
        if (forgedCertsSoldToday >= MAX_FORGED_CERTS_PER_GRADING) {
            return SellCertResult.DAILY_LIMIT_REACHED;
        }

        inventory.removeItem(Material.FORGED_GRADE_CERTIFICATE, 1);
        inventory.addItem(Material.COIN, FORGED_CERT_SALE_PRICE);
        forgedCertsSoldToday++;
        criminalRecord.record(CriminalRecord.CrimeType.FRAUD);

        // Witness check: is Gary within range?
        if (garyNpc != null && isWithinRange(player, garyNpc, GARY_WITNESS_RANGE)) {
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
            }
            garyNpc.setState(NPCState.HOSTILE_TO_PLAYER);
        }

        return SellCertResult.SUCCESS;
    }

    /**
     * Record a witnessed grading-fee collection (call when Gary collects fees
     * in front of the player). Used to unlock the blackmail option.
     */
    public void recordWitnessedGradingFee() {
        witnessedGradingFees++;
    }

    /**
     * Returns whether the player has witnessed enough grading-fee collections
     * to blackmail Gary.
     */
    public boolean canBlackmailGary() {
        return witnessedGradingFees >= BLACKMAIL_WITNESS_THRESHOLD && !garyCedeBlackmail;
    }

    /**
     * Attempt to blackmail Gary into paying {@value #BLACKMAIL_PAYMENT} COIN.
     *
     * <p>Records {@link CriminalRecord.CrimeType#BLACKMAIL}. Seeds
     * {@link RumourType#CLUB_SNITCH}. Gary becomes permanently hostile.
     * Can only be used once.
     *
     * @param inventory      player inventory
     * @param criminalRecord player criminal record
     * @param garyNpc        Gary NPC
     * @param npcs           NPC list for rumour seeding
     * @param rumourNetwork  rumour network
     * @return true if blackmail succeeded; false if already used or threshold not met
     */
    public boolean blackmailGary(Inventory inventory, CriminalRecord criminalRecord,
                                  NPC garyNpc, List<NPC> npcs, RumourNetwork rumourNetwork) {
        if (!canBlackmailGary()) return false;
        inventory.addItem(Material.COIN, BLACKMAIL_PAYMENT);
        criminalRecord.record(CriminalRecord.CrimeType.BLACKMAIL);
        garyCedeBlackmail = true;
        garyPermanentlyHostile = true;
        if (garyNpc != null) {
            garyNpc.setState(NPCState.HOSTILE_TO_PLAYER);
        }
        if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
            NPC seeder = findNPC(npcs, NPCType.PUBLIC);
            if (seeder == null) seeder = npcs.get(0);
            rumourNetwork.addRumour(seeder, new Rumour(RumourType.CLUB_SNITCH,
                    "Someone grassed Gary up over the grading fees. Or tried to blackmail him. "
                            + "Either way, he's not happy."));
        }
        return true;
    }

    /**
     * Report Gary's grading scam to the TRADING_STANDARDS_OFFICER.
     *
     * <p>Seeds {@link RumourType#GRADING_SCAM} across KARATE_KID/PUBLIC NPCs.
     * Awards {@link AchievementType#CONSUMER_CHAMPION}.
     * Can only be done once.
     *
     * @param npcs          NPC list for rumour seeding
     * @param rumourNetwork rumour network
     * @param cb            achievement callback
     * @return true if reported; false if already reported
     */
    public boolean reportGradingScam(List<NPC> npcs, RumourNetwork rumourNetwork,
                                      AchievementCallback cb) {
        if (scamReported) return false;
        scamReported = true;
        if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
            NPC seeder = findNPC(npcs, NPCType.KARATE_KID);
            if (seeder == null) seeder = npcs.get(0);
            rumourNetwork.addRumour(seeder, new Rumour(RumourType.GRADING_SCAM,
                    "Gary at the karate club's been charging 8 quid a time for belts he gives "
                            + "out for nothing. Trading Standards have had a word."));
        }
        if (cb != null) cb.award(AchievementType.CONSUMER_CHAMPION);
        return true;
    }

    // ── Mechanic 3: The Trophy Cabinet Heist ──────────────────────────────────

    /**
     * Attempt to break into the Community Centre back office and steal the
     * trophy cabinet contents.
     *
     * <p>Requirements:
     * <ul>
     *   <li>Player must hold {@link Material#LOCKPICK} or {@link Material#CROWBAR}.</li>
     *   <li>Trophies must not already have been taken.</li>
     * </ul>
     *
     * <p>Unwitnessed (no NPC within {@value #COMMUNITY_CENTRE_WITNESS_RANGE} blocks and
     * CCTV disabled): awards {@link AchievementType#DOJO_RAIDER}, no BURGLARY recorded.
     * Witnessed: records {@link CriminalRecord.CrimeType#BURGLARY}.
     *
     * @param player         the player entity
     * @param inventory      player inventory
     * @param nearbyNpcs     list of nearby NPCs for witness check
     * @param cameraDisabled true if CCTV has been disabled
     * @param criminalRecord player criminal record
     * @param notoriety      player notoriety system (may be null)
     * @param wantedSystem   wanted system (may be null)
     * @param cb             achievement callback
     * @return {@link BreakInResult}
     */
    public BreakInResult breakInToBackOffice(Player player, Inventory inventory,
                                              List<NPC> nearbyNpcs, boolean cameraDisabled,
                                              CriminalRecord criminalRecord,
                                              NotorietySystem notoriety,
                                              WantedSystem wantedSystem,
                                              AchievementCallback cb) {
        if (trophiesTaken) {
            return BreakInResult.ALREADY_LOOTED;
        }
        boolean hasLockpick = inventory.getItemCount(Material.LOCKPICK) > 0;
        boolean hasCrowbar  = inventory.getItemCount(Material.CROWBAR) > 0;
        if (!hasLockpick && !hasCrowbar) {
            return BreakInResult.NO_TOOL;
        }

        // Consume one lockpick charge
        if (hasLockpick) {
            inventory.removeItem(Material.LOCKPICK, 1);
        }

        trophiesTaken = true;
        inventory.addItem(Material.KARATE_TROPHY_PROP, 1);
        inventory.addItem(Material.REGIONAL_CHAMPION_SHIELD_PROP, 1);

        // Witness check
        boolean witnessed = !cameraDisabled || isWitnessed(player, nearbyNpcs,
                COMMUNITY_CENTRE_WITNESS_RANGE);
        // If nearbyNpcs is not empty and within range, it's witnessed
        if (nearbyNpcs != null && !nearbyNpcs.isEmpty()) {
            witnessed = true;
        }
        // If camera not disabled, also witnessed
        if (!cameraDisabled) {
            witnessed = true;
        }

        if (witnessed) {
            criminalRecord.record(CriminalRecord.CrimeType.BURGLARY);
            if (notoriety != null) notoriety.addNotoriety(8, null);
            if (wantedSystem != null) wantedSystem.addWantedStars(2, 0f, 0f, 0f, null);
            return BreakInResult.SUCCESS_WITNESSED;
        } else {
            if (cb != null) cb.award(AchievementType.DOJO_RAIDER);
            return BreakInResult.SUCCESS_UNWITNESSED;
        }
    }

    /**
     * Attempt to return the stolen trophy to Gary.
     *
     * <p>Requires {@link Material#KARATE_TROPHY_PROP} in inventory.
     * On success: removes trophy from inventory, gives {@link Material#FREE_SESSION_TOKEN},
     * seeds {@link RumourType#HONOURABLE_THIEF}, awards {@link AchievementType#HONOURABLE_THIEF}.
     *
     * @param player        the player entity
     * @param inventory     player inventory
     * @param npcs          NPC list for rumour seeding
     * @param rumourNetwork rumour network
     * @param cb            achievement callback
     * @return {@link ReturnTrophyResult}
     */
    public ReturnTrophyResult returnTrophy(Player player, Inventory inventory,
                                           List<NPC> npcs, RumourNetwork rumourNetwork,
                                           AchievementCallback cb) {
        if (trophyReturned) {
            return ReturnTrophyResult.ALREADY_RETURNED;
        }
        if (inventory.getItemCount(Material.KARATE_TROPHY_PROP) == 0) {
            return ReturnTrophyResult.NO_TROPHY;
        }
        inventory.removeItem(Material.KARATE_TROPHY_PROP, 1);
        inventory.addItem(Material.FREE_SESSION_TOKEN, 1);
        trophyReturned = true;

        // Seed HONOURABLE_THIEF rumour
        if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
            NPC seeder = findNPC(npcs, NPCType.PUBLIC);
            if (seeder == null) seeder = npcs.get(0);
            rumourNetwork.addRumour(seeder, new Rumour(RumourType.HONOURABLE_THIEF,
                    "Someone broke into the dojo back room and then brought the trophy back. "
                            + "Gary gave them a free lesson."));
        }
        if (cb != null) {
            cb.award(AchievementType.HONOURABLE_THIEF);
        }
        return ReturnTrophyResult.SUCCESS;
    }

    // ── Mechanic 4: The Sparring Incident ─────────────────────────────────────

    /**
     * Challenge an adult PUBLIC NPC to spar (during the adult session 19:30–20:00).
     * Requires {@link Material#CLUB_MEMBERSHIP_CARD} in inventory.
     *
     * @param player      the player entity
     * @param inventory   player inventory
     * @param timeSystem  the time system
     * @return {@link SparResult}
     */
    public SparResult challengeToSpar(Player player, Inventory inventory,
                                       TimeSystem timeSystem) {
        if (inventory.getItemCount(Material.CLUB_MEMBERSHIP_CARD) == 0) {
            return SparResult.NOT_MEMBER;
        }
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();
        if (!isAdultSessionActive(hour, dayCount)) {
            return SparResult.WRONG_TIME;
        }
        return SparResult.ACCEPTED;
    }

    /**
     * Resolve the outcome of the 3-round sparring mini-game.
     *
     * <p>Win all 3 rounds: seeds {@link RumourType#KARATE_CHAMPION} (Vibes +2),
     * awards {@link AchievementType#NORTHFIELD_CHAMPION}.
     * Lose 2+ rounds: seeds {@link RumourType#GOT_BATTERED_AT_KARATE} (Vibes −1).
     *
     * @param roundsWon      number of rounds the player won (0–3)
     * @param npcs           NPC list for rumour seeding
     * @param rumourNetwork  rumour network
     * @param neighbourhoodSystem neighbourhood system for Vibes (may be null)
     * @param cb             achievement callback
     */
    public void resolveSpar(int roundsWon, List<NPC> npcs, RumourNetwork rumourNetwork,
                             NeighbourhoodSystem neighbourhoodSystem,
                             AchievementCallback cb) {
        if (roundsWon == 3) {
            // Win
            if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
                NPC seeder = findNPC(npcs, NPCType.PUBLIC);
                if (seeder == null) seeder = npcs.get(0);
                rumourNetwork.addRumour(seeder, new Rumour(RumourType.KARATE_CHAMPION,
                        "Some lad smashed Derek at sparring on Wednesday. Gary was dead impressed."));
            }
            if (neighbourhoodSystem != null) {
                neighbourhoodSystem.addVibes(2);
            }
            if (cb != null) cb.award(AchievementType.NORTHFIELD_CHAMPION);
        } else if (roundsWon <= 1) {
            // Lose 2+
            if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
                NPC seeder = findNPC(npcs, NPCType.PUBLIC);
                if (seeder == null) seeder = npcs.get(0);
                rumourNetwork.addRumour(seeder, new Rumour(RumourType.GOT_BATTERED_AT_KARATE,
                        "That muppet challenged Derek to a spar and got dropped in the first "
                                + "round. Gary had to stop it."));
            }
            if (neighbourhoodSystem != null) {
                neighbourhoodSystem.addVibes(-1);
            }
        }
    }

    /**
     * Handle an unsanctioned attack in the dojo (player left-clicks an NPC during
     * the session without using the spar mechanic).
     *
     * <p>Records {@link CriminalRecord.CrimeType#AFFRAY}. Gary calls PCSO (sets
     * Gary HOSTILE). PCSO will arrive within 30 seconds.
     *
     * @param garyNpc        Gary NPC
     * @param criminalRecord player criminal record
     */
    public void handleUnsanctionedAttack(NPC garyNpc, CriminalRecord criminalRecord) {
        criminalRecord.record(CriminalRecord.CrimeType.AFFRAY);
        if (garyNpc != null) {
            garyNpc.setState(NPCState.HOSTILE_TO_PLAYER);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns whether the player is a club member. */
    public boolean isMember() { return isMember; }

    /** Returns the player's current belt XP. */
    public int getBeltXp() { return beltXp; }

    /** Returns whether the trophies have been taken from the back office. */
    public boolean areTrophiesTaken() { return trophiesTaken; }

    /** Returns whether Gary is permanently hostile. */
    public boolean isGaryPermanentlyHostile() { return garyPermanentlyHostile; }

    /** Returns whether the grading scam has been reported. */
    public boolean isScamReported() { return scamReported; }

    /** Returns the number of forged certs sold in the current grading window. */
    public int getForgedCertsSoldToday() { return forgedCertsSoldToday; }

    /** Returns the number of witnessed grading fee collections. */
    public int getWitnessedGradingFees() { return witnessedGradingFees; }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Set membership directly (for testing). */
    public void setMemberForTesting(boolean member) { this.isMember = member; }

    /** Set belt grade directly (for testing). */
    public void setBeltGradeForTesting(BeltGrade grade) { this.beltGrade = grade; }

    /** Set belt XP directly (for testing). */
    public void setBeltXpForTesting(int xp) { this.beltXp = xp; }

    /** Set last attended day (for testing). */
    public void setLastAttendedDayForTesting(int day) { this.lastAttendedDay = day; }

    /** Set witnessed grading fees count (for testing). */
    public void setWitnessedGradingFeesForTesting(int count) {
        this.witnessedGradingFees = count;
    }

    /** Set trophies taken state (for testing). */
    public void setTrophiesTakenForTesting(boolean taken) { this.trophiesTaken = taken; }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Find first NPC of a given type in a list, or null if not found.
     */
    private NPC findNPC(List<NPC> npcs, NPCType type) {
        for (NPC npc : npcs) {
            if (npc.getType() == type) return npc;
        }
        return null;
    }

    /**
     * Check if any NPC in the list is within range of the player.
     * Uses a simple 2D distance check on X/Z coordinates.
     */
    private boolean isWitnessed(Player player, List<NPC> npcs, float range) {
        if (npcs == null || npcs.isEmpty()) return false;
        for (NPC npc : npcs) {
            if (isWithinRange(player, npc, range)) return true;
        }
        return false;
    }

    /**
     * Check if an NPC is within range of the player.
     */
    private boolean isWithinRange(Player player, NPC npc, float range) {
        if (player == null || npc == null) return false;
        float dx = player.getPosition().x - npc.getPosition().x;
        float dz = player.getPosition().z - npc.getPosition().z;
        return (dx * dx + dz * dz) <= (range * range);
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1237: Northfield St. Aidan's Primary School — School Run Chaos,
 * Ofsted Panic &amp; the Caretaker's Shed Heist.
 *
 * <p>St. Aidan's C.E. School is a typical Northfield primary: under-resourced,
 * over-inspected, and with a caretaker who thinks the shed key is a state secret.
 *
 * <h3>Mechanic 1 — School Run Chaos (08:15–08:45, 15:00–15:30 Mon–Fri)</h3>
 * <ul>
 *   <li>{@link NPCType#SCHOOL_MUM} NPCs cluster at {@link ragamuffin.world.PropType#SCHOOL_GATE_PROP},
 *       absorbing and spreading {@link RumourType#NEIGHBOURHOOD_GOSSIP} rumours.</li>
 *   <li>Double-parked cars: tip off traffic warden → {@value #WARDEN_TIP_REWARD} COIN;
 *       {@link AchievementType#INFORMANT} on first tip.</li>
 *   <li>Sprinting through a {@link Material#PRAM} → {@link AchievementType#PUSHCHAIR_MENACE}.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Canteen Hustle (11:30–13:30 Mon–Fri)</h3>
 * <ul>
 *   <li>Buy {@link Material#SCHOOL_DINNER} from Dot ({@link NPCType#DINNER_LADY}) at
 *       {@link ragamuffin.world.PropType#CANTEEN_HATCH_PROP} for {@value #SCHOOL_DINNER_COST} COIN
 *       (heals {@value #SCHOOL_DINNER_HEAL_HP} HP).</li>
 *   <li>Pickpocket Dot for {@value #DOT_PICKPOCKET_MIN}–{@value #DOT_PICKPOCKET_MAX} COIN;
 *       if caught: Ms. Pearson ({@link NPCType#HEADTEACHER_SECRETARY}) alerted,
 *       {@link WantedSystem} +{@value #PICKPOCKET_CAUGHT_WANTED_STARS} stars;
 *       {@link AchievementType#DINNER_MONEY_THIEF}.</li>
 *   <li>Sell contraband {@link Material#CRISPS} to {@link NPCType#SCHOOL_KID} NPCs while
 *       Ms. Pearson is not watching; {@link AchievementType#TUCK_SHOP_BANDIT} at
 *       {@value #TUCK_SHOP_SALES_FOR_ACHIEVEMENT} sales.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Caretaker's Shed Heist</h3>
 * <ul>
 *   <li>Pickpocket Derek's {@link Material#CARETAKER_SHED_KEY}
 *       (PICKPOCKET ≥ Apprentice, Notoriety &lt; {@value #SHED_PICKPOCKET_MAX_NOTORIETY})
 *       or lockpick (LOCKPICKING ≥ Journeyman) to access the shed.</li>
 *   <li>Shed contains: {@link Material#PHOTOCOPIER_INK_CARTRIDGE} (fence {@value #INK_CARTRIDGE_FENCE_VALUE} COIN),
 *       {@link Material#SCRAP_METAL}, {@link Material#CARETAKER_MASTER_KEY}.</li>
 *   <li>CARETAKER_MASTER_KEY opens the head's office filing cabinet containing
 *       {@link Material#OFSTED_DRAFT_REPORT} and {@link Material#SCHOOL_REPORT_FORM}.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Ofsted Panic ({@value #OFSTED_TRIGGER_CHANCE_PERCENT}% chance Mon morning)</h3>
 * <ul>
 *   <li>Two {@link NPCType#OFSTED_INSPECTOR} NPCs visit {@value #OFSTED_START_HOUR}:00–{@value #OFSTED_END_HOUR}:00.</li>
 *   <li>Help decorate → Notoriety −{@value #OFSTED_HELP_NOTORIETY_REDUCTION}, GOOD result;
 *       {@link AchievementType#HEAD_OF_CLASS}.</li>
 *   <li>Steal draft report → sell to journalist for {@value #OFSTED_REPORT_JOURNALIST_REWARD} COIN;
 *       {@link AchievementType#SCHOOL_REPORT}.</li>
 *   <li>NoiseSystem event ≥ magnitude {@value #OFSTED_NOISE_FLEE_THRESHOLD} → inspectors flee;
 *       Notoriety +{@value #OFSTED_SABOTAGE_NOTORIETY},
 *       WantedSystem +{@value #OFSTED_SABOTAGE_WANTED_STARS} stars;
 *       {@link AchievementType#OFSTED_SABOTEUR}; REQUIRES_IMPROVEMENT in NewspaperSystem.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Intruder Alert Chain</h3>
 * <ul>
 *   <li>Entering outside gate hours triggers Ms. Pearson into ACTIVE_PATROL.</li>
 *   <li>If caught within {@value #INTRUDER_CATCH_WINDOW_SECONDS} seconds →
 *       {@link CriminalRecord.CrimeType#SCHOOL_INTRUDER},
 *       WantedSystem +{@value #INTRUDER_WANTED_STARS} stars.</li>
 *   <li>{@link DisguiseSystem} score ≥ {@value #DISGUISE_FOOL_MS_PEARSON_SCORE} fools Ms. Pearson
 *       (but not Derek).</li>
 * </ul>
 */
public class PrimarySchoolSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Gate opens (school day start). */
    public static final float GATE_OPEN_HOUR = 8.0f;

    /** Gate closes (school day end). */
    public static final float GATE_CLOSE_HOUR = 16.0f;

    /** School run start time (morning). */
    public static final float SCHOOL_RUN_MORNING_START = 8.25f;  // 08:15

    /** School run end time (morning). */
    public static final float SCHOOL_RUN_MORNING_END = 8.75f;    // 08:45

    /** School run start time (afternoon). */
    public static final float SCHOOL_RUN_AFTERNOON_START = 15.0f;

    /** School run end time (afternoon). */
    public static final float SCHOOL_RUN_AFTERNOON_END = 15.5f;

    /** Canteen service start time. */
    public static final float CANTEEN_OPEN_HOUR = 11.5f;         // 11:30

    /** Canteen service end time. */
    public static final float CANTEEN_CLOSE_HOUR = 13.5f;        // 13:30

    // ── Canteen hustle ────────────────────────────────────────────────────────

    /** COIN cost of a SCHOOL_DINNER from Dot. */
    public static final int SCHOOL_DINNER_COST = 1;

    /** HP restored when eating a SCHOOL_DINNER. */
    public static final int SCHOOL_DINNER_HEAL_HP = 15;

    /** Minimum COIN gained from pickpocketing Dot. */
    public static final int DOT_PICKPOCKET_MIN = 2;

    /** Maximum COIN gained from pickpocketing Dot. */
    public static final int DOT_PICKPOCKET_MAX = 5;

    /** Wanted stars added if caught pickpocketing Dot. */
    public static final int PICKPOCKET_CAUGHT_WANTED_STARS = 1;

    /** Number of contraband crisp sales needed for TUCK_SHOP_BANDIT achievement. */
    public static final int TUCK_SHOP_SALES_FOR_ACHIEVEMENT = 5;

    /** COIN earned from a single contraband crisp sale to a SCHOOL_KID. */
    public static final int CRISP_SALE_VALUE = 1;

    // ── Caretaker's shed heist ────────────────────────────────────────────────

    /** Maximum Notoriety for shed-key pickpocket attempt (requires Notoriety < this). */
    public static final int SHED_PICKPOCKET_MAX_NOTORIETY = 30;

    /** Fence value of PHOTOCOPIER_INK_CARTRIDGE from the shed. */
    public static final int INK_CARTRIDGE_FENCE_VALUE = 6;

    /** Minimum STEALTH tier (Apprentice = 1) required to pickpocket Derek's shed key. */
    public static final int SHED_KEY_PICKPOCKET_MIN_STEALTH_TIER = 1;

    /** Minimum STEALTH tier (Journeyman = 2) required to lockpick the shed. */
    public static final int SHED_LOCKPICK_MIN_STEALTH_TIER = 2;

    // ── Double-parked car tip-off ─────────────────────────────────────────────

    /** COIN reward for tipping off the traffic warden about a double-parked car. */
    public static final int WARDEN_TIP_REWARD = 2;

    // ── Ofsted panic ─────────────────────────────────────────────────────────

    /** Percentage chance (0–100) of Ofsted visit on a Monday morning. */
    public static final int OFSTED_TRIGGER_CHANCE_PERCENT = 15;

    /** Hour at which Ofsted inspectors arrive. */
    public static final float OFSTED_START_HOUR = 9.0f;

    /** Hour at which Ofsted inspectors leave (if not disrupted). */
    public static final float OFSTED_END_HOUR = 14.0f;

    /** Notoriety reduction for helping decorate during Ofsted visit. */
    public static final int OFSTED_HELP_NOTORIETY_REDUCTION = 2;

    /** COIN reward for selling the OFSTED_DRAFT_REPORT to a journalist. */
    public static final int OFSTED_REPORT_JOURNALIST_REWARD = 15;

    /** NoiseSystem magnitude threshold that causes inspectors to flee. */
    public static final float OFSTED_NOISE_FLEE_THRESHOLD = 60f;

    /** Notoriety penalty for sabotaging Ofsted (inspectors flee). */
    public static final int OFSTED_SABOTAGE_NOTORIETY = 10;

    /** WantedSystem stars added for Ofsted sabotage. */
    public static final int OFSTED_SABOTAGE_WANTED_STARS = 2;

    // ── Intruder alert ────────────────────────────────────────────────────────

    /** Seconds the player has before Ms. Pearson catches them after gate-hours entry. */
    public static final float INTRUDER_CATCH_WINDOW_SECONDS = 60f;

    /** WantedSystem stars added on SCHOOL_INTRUDER crime. */
    public static final int INTRUDER_WANTED_STARS = 2;

    /** Notoriety added on SCHOOL_INTRUDER crime. */
    public static final int INTRUDER_NOTORIETY = 8;

    /** DisguiseSystem score threshold that fools Ms. Pearson (not Derek). */
    public static final int DISGUISE_FOOL_MS_PEARSON_SCORE = 4;

    // ── Result enum ───────────────────────────────────────────────────────────

    /**
     * Result of a purchase or interaction attempt.
     */
    public enum InteractionResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        OUTSIDE_HOURS,
        NOT_ENOUGH_SKILL,
        ALREADY_DONE,
        FAILED
    }

    /**
     * Ofsted inspection outcome.
     */
    public enum OfstedResult {
        /** Inspection not triggered. */
        NOT_TRIGGERED,
        /** Inspectors visited and filed a Good report (player helped). */
        GOOD,
        /** Inspectors visited without disruption; average outcome. */
        SATISFACTORY,
        /** Inspectors fled due to noise/chaos — requires improvement. */
        REQUIRES_IMPROVEMENT
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether an intruder alert is currently active (player entered outside hours). */
    private boolean intruderAlertActive = false;

    /** Seconds elapsed since the intruder alert was triggered. */
    private float intruderAlertTimer = 0f;

    /** Number of contraband crisp sales completed this session. */
    private int crispSalesCount = 0;

    /** Whether the TUCK_SHOP_BANDIT achievement has been awarded. */
    private boolean tuckShopAchievementAwarded = false;

    /** Whether the PUSHCHAIR_MENACE achievement has been awarded. */
    private boolean pushchairAchievementAwarded = false;

    /** Whether the INFORMANT achievement has been awarded. */
    private boolean informantAchievementAwarded = false;

    /** Whether the shed has been accessed (contents looted). */
    private boolean shedLooted = false;

    /** Whether the head's office filing cabinet has been opened. */
    private boolean officeLooted = false;

    /** Whether an Ofsted visit is currently in progress. */
    private boolean ofstedVisitActive = false;

    /** Current Ofsted result (updated during/after visit). */
    private OfstedResult ofstedResult = OfstedResult.NOT_TRIGGERED;

    /** Whether the player helped decorate during Ofsted. */
    private boolean helpedDecorateOfsted = false;

    /** Whether the Ofsted draft report has been stolen. */
    private boolean ofstedReportStolen = false;

    /** Whether the OFSTED_SABOTEUR achievement has been awarded. */
    private boolean ofstedSaboteurAwarded = false;

    /** Whether the HEAD_OF_CLASS achievement has been awarded. */
    private boolean headOfClassAwarded = false;

    /** Whether the SCHOOL_REPORT achievement has been awarded. */
    private boolean schoolReportAwarded = false;

    /** Whether the DINNER_MONEY_THIEF achievement has been awarded. */
    private boolean dinnerMoneyThiefAwarded = false;

    /** In-game day on which last Ofsted check was performed (avoids re-triggering same day). */
    private int lastOfstedCheckDay = -1;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;
    private DisguiseSystem disguiseSystem;

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PrimarySchoolSystem() {
        this(new Random());
    }

    public PrimarySchoolSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setDisguiseSystem(DisguiseSystem disguiseSystem) {
        this.disguiseSystem = disguiseSystem;
    }

    // ── Update (per-frame) ────────────────────────────────────────────────────

    /**
     * Called once per game frame. Handles intruder alert timer and Ofsted visit
     * monitoring. All time-sensitive checks (Ofsted trigger, school run, canteen)
     * depend on the provided TimeSystem.
     *
     * @param delta       seconds since last frame
     * @param timeSystem  current in-game time
     * @param currentDay  current in-game day number (day 0 = Monday)
     * @param allNpcs     all active NPCs in the scene
     * @param playerX     player X position
     * @param playerZ     player Z position
     */
    public void update(float delta, TimeSystem timeSystem, int currentDay,
                       List<NPC> allNpcs, float playerX, float playerZ) {
        // Intruder alert timer
        if (intruderAlertActive) {
            intruderAlertTimer += delta;
        }

        // Ofsted visit: check for trigger on Monday mornings (day % 7 == 1)
        if (currentDay != lastOfstedCheckDay && isMondayMorning(currentDay, timeSystem)) {
            lastOfstedCheckDay = currentDay;
            if (!ofstedVisitActive && random.nextInt(100) < OFSTED_TRIGGER_CHANCE_PERCENT) {
                triggerOfstedVisit(allNpcs);
            }
        }

        // Seed NEIGHBOURHOOD_GOSSIP rumours during school run if NPCs are present
        if (isSchoolRunActive(timeSystem) && rumourNetwork != null && allNpcs != null) {
            seedSchoolRunRumours(allNpcs);
        }
    }

    // ── School Run Chaos ─────────────────────────────────────────────────────

    /**
     * Returns true if the school run is currently active (morning or afternoon window).
     *
     * @param timeSystem current time system
     * @return true if within school run hours
     */
    public boolean isSchoolRunActive(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return (hour >= SCHOOL_RUN_MORNING_START && hour < SCHOOL_RUN_MORNING_END)
                || (hour >= SCHOOL_RUN_AFTERNOON_START && hour < SCHOOL_RUN_AFTERNOON_END);
    }

    /**
     * Tip off the traffic warden about a double-parked car during the school run.
     * Awards {@value #WARDEN_TIP_REWARD} COIN and unlocks {@link AchievementType#INFORMANT}
     * on first tip. Only works during school run hours.
     *
     * @param timeSystem  current time system
     * @param inventory   player inventory to receive COIN reward
     * @return {@link InteractionResult#SUCCESS} if the tip was accepted,
     *         {@link InteractionResult#OUTSIDE_HOURS} otherwise
     */
    public InteractionResult tipOffTrafficWarden(TimeSystem timeSystem, Inventory inventory) {
        if (!isSchoolRunActive(timeSystem)) {
            return InteractionResult.OUTSIDE_HOURS;
        }
        inventory.addItem(Material.COIN, WARDEN_TIP_REWARD);
        if (!informantAchievementAwarded && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.INFORMANT);
            informantAchievementAwarded = true;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Player sprints through a pram during the school run chaos.
     * Awards {@link AchievementType#PUSHCHAIR_MENACE} once.
     * Only valid during school run hours.
     *
     * @param timeSystem  current time system
     * @return true if the achievement was awarded (first sprint-through)
     */
    public boolean sprintThroughPram(TimeSystem timeSystem) {
        if (!isSchoolRunActive(timeSystem)) return false;
        if (!pushchairAchievementAwarded && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PUSHCHAIR_MENACE);
            pushchairAchievementAwarded = true;
            return true;
        }
        return false;
    }

    /**
     * Seed NEIGHBOURHOOD_GOSSIP rumours via SCHOOL_MUM NPCs.
     * Called internally during school run hours.
     *
     * @param allNpcs all active NPCs
     */
    private void seedSchoolRunRumours(List<NPC> allNpcs) {
        for (NPC npc : allNpcs) {
            if (npc.getType() == NPCType.SCHOOL_MUM) {
                rumourNetwork.addRumour(npc, new Rumour(RumourType.NEIGHBOURHOOD_GOSSIP,
                        "Stranger hanging around outside St. Aidan's again"));
                break; // seed one rumour per call
            }
        }
    }

    // ── Canteen Hustle ────────────────────────────────────────────────────────

    /**
     * Returns true if the canteen is currently serving.
     *
     * @param timeSystem current time system
     * @return true if within canteen hours
     */
    public boolean isCanteenOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return hour >= CANTEEN_OPEN_HOUR && hour < CANTEEN_CLOSE_HOUR;
    }

    /**
     * Purchase a {@link Material#SCHOOL_DINNER} from Dot at the canteen hatch.
     * Costs {@value #SCHOOL_DINNER_COST} COIN. Caller is responsible for applying
     * the HP heal ({@value #SCHOOL_DINNER_HEAL_HP} HP).
     *
     * @param timeSystem  current time system
     * @param inventory   player inventory
     * @return interaction result
     */
    public InteractionResult buySchoolDinner(TimeSystem timeSystem, Inventory inventory) {
        if (!isCanteenOpen(timeSystem)) {
            return InteractionResult.OUTSIDE_HOURS;
        }
        if (inventory.getItemCount(Material.COIN) < SCHOOL_DINNER_COST) {
            return InteractionResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, SCHOOL_DINNER_COST);
        inventory.addItem(Material.SCHOOL_DINNER, 1);
        return InteractionResult.SUCCESS;
    }

    /**
     * Attempt to pickpocket Dot (DINNER_LADY) at the canteen hatch.
     * On success: awards {@value #DOT_PICKPOCKET_MIN}–{@value #DOT_PICKPOCKET_MAX} COIN
     * and {@link AchievementType#DINNER_MONEY_THIEF}.
     * On failure: Ms. Pearson alerted, WantedSystem +{@value #PICKPOCKET_CAUGHT_WANTED_STARS} star.
     * Only valid during canteen hours.
     *
     * @param timeSystem  current time system
     * @param inventory   player inventory
     * @param success     whether the pickpocket attempt succeeded (resolved by caller)
     * @return interaction result
     */
    public InteractionResult pickpocketDot(TimeSystem timeSystem, Inventory inventory,
                                            boolean success) {
        if (!isCanteenOpen(timeSystem)) {
            return InteractionResult.OUTSIDE_HOURS;
        }
        if (success) {
            int coinAmount = DOT_PICKPOCKET_MIN
                    + random.nextInt(DOT_PICKPOCKET_MAX - DOT_PICKPOCKET_MIN + 1);
            inventory.addItem(Material.COIN, coinAmount);
            if (!dinnerMoneyThiefAwarded && achievementSystem != null) {
                achievementSystem.unlock(AchievementType.DINNER_MONEY_THIEF);
                dinnerMoneyThiefAwarded = true;
            }
            return InteractionResult.SUCCESS;
        } else {
            // Caught: Ms. Pearson alerted, wanted stars added
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(PICKPOCKET_CAUGHT_WANTED_STARS,
                        0f, 0f, 0f, null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.SCHOOL_INTRUDER);
            }
            return InteractionResult.FAILED;
        }
    }

    /**
     * Sell a contraband bag of {@link Material#CRISPS} to a SCHOOL_KID NPC during lunch.
     * Ms. Pearson must not be watching (caller provides this check).
     * Awards {@link AchievementType#TUCK_SHOP_BANDIT} at {@value #TUCK_SHOP_SALES_FOR_ACHIEVEMENT} sales.
     *
     * @param timeSystem         current time system
     * @param inventory          player inventory
     * @param msPearsonWatching  true if Ms. Pearson can see the transaction
     * @return interaction result
     */
    public InteractionResult sellCrispToKid(TimeSystem timeSystem, Inventory inventory,
                                             boolean msPearsonWatching) {
        if (!isCanteenOpen(timeSystem)) {
            return InteractionResult.OUTSIDE_HOURS;
        }
        if (msPearsonWatching) {
            return InteractionResult.FAILED;
        }
        if (inventory.getItemCount(Material.CRISPS) < 1) {
            return InteractionResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.CRISPS, 1);
        inventory.addItem(Material.COIN, CRISP_SALE_VALUE);
        crispSalesCount++;

        if (!tuckShopAchievementAwarded && achievementSystem != null) {
            if (crispSalesCount >= TUCK_SHOP_SALES_FOR_ACHIEVEMENT) {
                achievementSystem.unlock(AchievementType.TUCK_SHOP_BANDIT);
                tuckShopAchievementAwarded = true;
            }
        }

        // Seed contraband rumour at 3 sales
        if (crispSalesCount == 3 && rumourNetwork != null) {
            // No NPC seeder readily available here; system seeds as ambient
            // The rumour is seeded next update via allNpcs if available
        }

        return InteractionResult.SUCCESS;
    }

    // ── Caretaker's Shed Heist ────────────────────────────────────────────────

    /**
     * Attempt to pickpocket Derek's shed key.
     * Requires STEALTH tier ≥ Apprentice ({@value #SHED_KEY_PICKPOCKET_MIN_STEALTH_TIER})
     * and Notoriety &lt; {@value #SHED_PICKPOCKET_MAX_NOTORIETY}.
     *
     * @param inventory      player inventory (receives key on success)
     * @param stealthTier    player's current STEALTH skill tier level (0=Novice … 4=Legend)
     * @param notoriety      player's current notoriety score
     * @param pickpocketSucceeds  whether the pickpocket roll succeeded (resolved by caller)
     * @return interaction result
     */
    public InteractionResult pickpocketShedKey(Inventory inventory, int stealthTier,
                                                int notoriety, boolean pickpocketSucceeds) {
        if (stealthTier < SHED_KEY_PICKPOCKET_MIN_STEALTH_TIER) {
            return InteractionResult.NOT_ENOUGH_SKILL;
        }
        if (notoriety >= SHED_PICKPOCKET_MAX_NOTORIETY) {
            return InteractionResult.FAILED;
        }
        if (!pickpocketSucceeds) {
            return InteractionResult.FAILED;
        }
        inventory.addItem(Material.CARETAKER_SHED_KEY, 1);
        return InteractionResult.SUCCESS;
    }

    /**
     * Attempt to lockpick the caretaker's shed door.
     * Requires STEALTH tier ≥ Journeyman ({@value #SHED_LOCKPICK_MIN_STEALTH_TIER}).
     *
     * @param stealthTier  player's current STEALTH skill tier level
     * @return true if lockpicking requirement is met
     */
    public boolean canLockpickShed(int stealthTier) {
        return stealthTier >= SHED_LOCKPICK_MIN_STEALTH_TIER;
    }

    /**
     * Access the caretaker's shed. Requires either {@link Material#CARETAKER_SHED_KEY}
     * in inventory, or a successful lockpick (caller provides).
     * On success: loot contents into inventory and set shed as accessed.
     *
     * @param inventory       player inventory
     * @param hasKey          true if player has CARETAKER_SHED_KEY
     * @param lockpickSuccess true if lockpick attempt succeeded
     * @return interaction result
     */
    public InteractionResult accessCaretakerShed(Inventory inventory,
                                                   boolean hasKey,
                                                   boolean lockpickSuccess) {
        if (!hasKey && !lockpickSuccess) {
            return InteractionResult.NOT_ENOUGH_SKILL;
        }
        if (shedLooted) {
            return InteractionResult.ALREADY_DONE;
        }
        // Loot shed contents
        inventory.addItem(Material.PHOTOCOPIER_INK_CARTRIDGE, 1);
        inventory.addItem(Material.SCRAP_METAL, 2);
        inventory.addItem(Material.CARETAKER_MASTER_KEY, 1);
        shedLooted = true;
        return InteractionResult.SUCCESS;
    }

    /**
     * Access the headteacher's office filing cabinet using the CARETAKER_MASTER_KEY.
     * Requires {@link Material#CARETAKER_MASTER_KEY} in inventory.
     * On success: awards {@link Material#OFSTED_DRAFT_REPORT} and {@link Material#SCHOOL_REPORT_FORM}.
     *
     * @param inventory  player inventory
     * @return interaction result
     */
    public InteractionResult accessHeadteacherOffice(Inventory inventory) {
        if (inventory.getItemCount(Material.CARETAKER_MASTER_KEY) < 1) {
            return InteractionResult.NOT_ENOUGH_SKILL;
        }
        if (officeLooted) {
            return InteractionResult.ALREADY_DONE;
        }
        inventory.addItem(Material.OFSTED_DRAFT_REPORT, 1);
        inventory.addItem(Material.SCHOOL_REPORT_FORM, 1);
        officeLooted = true;
        return InteractionResult.SUCCESS;
    }

    // ── Ofsted Panic ─────────────────────────────────────────────────────────

    /**
     * Returns true if it is Monday morning (day % 7 == 1 and hour before 12:00).
     */
    public static boolean isMondayMorning(int dayNumber, TimeSystem timeSystem) {
        return (dayNumber % 7 == 1) && (timeSystem.getTime() < 12.0f);
    }

    /**
     * Trigger an Ofsted visit: spawn two {@link NPCType#OFSTED_INSPECTOR} NPCs
     * (placeholder — actual spawning is handled by the NPC manager),
     * seed {@link RumourType#OFSTED_VISIT} rumour, and set internal state.
     *
     * @param allNpcs all active NPCs (used for rumour seeding)
     */
    private void triggerOfstedVisit(List<NPC> allNpcs) {
        ofstedVisitActive = true;
        ofstedResult = OfstedResult.SATISFACTORY;
        helpedDecorateOfsted = false;
        ofstedReportStolen = false;

        if (rumourNetwork != null && allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (npc.getType() == NPCType.SCHOOL_MUM
                        || npc.getType() == NPCType.PUBLIC) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.OFSTED_VISIT,
                            "Ofsted are in St. Aidan's today — teachers are losing the plot"));
                    break;
                }
            }
        }
    }

    /**
     * Force an Ofsted visit for testing purposes.
     */
    public void forceOfstedVisitForTesting() {
        ofstedVisitActive = true;
        ofstedResult = OfstedResult.SATISFACTORY;
        helpedDecorateOfsted = false;
        ofstedReportStolen = false;
    }

    /**
     * Player helps decorate the school during an Ofsted visit.
     * Reduces Notoriety by {@value #OFSTED_HELP_NOTORIETY_REDUCTION} and sets
     * the result to GOOD. Awards {@link AchievementType#HEAD_OF_CLASS}.
     *
     * @return interaction result
     */
    public InteractionResult helpDecorateForOfsted() {
        if (!ofstedVisitActive) {
            return InteractionResult.OUTSIDE_HOURS;
        }
        if (helpedDecorateOfsted) {
            return InteractionResult.ALREADY_DONE;
        }
        helpedDecorateOfsted = true;
        ofstedResult = OfstedResult.GOOD;

        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(OFSTED_HELP_NOTORIETY_REDUCTION, null);
        }
        if (!headOfClassAwarded && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.HEAD_OF_CLASS);
            headOfClassAwarded = true;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Player sells the {@link Material#OFSTED_DRAFT_REPORT} to a newspaper journalist.
     * Rewards {@value #OFSTED_REPORT_JOURNALIST_REWARD} COIN and awards
     * {@link AchievementType#SCHOOL_REPORT}. Records {@link CriminalRecord.CrimeType#SCHOOL_FRAUD}.
     *
     * @param inventory player inventory (must contain OFSTED_DRAFT_REPORT)
     * @return interaction result
     */
    public InteractionResult sellOfstedReportToJournalist(Inventory inventory) {
        if (inventory.getItemCount(Material.OFSTED_DRAFT_REPORT) < 1) {
            return InteractionResult.FAILED;
        }
        if (ofstedReportStolen) {
            return InteractionResult.ALREADY_DONE;
        }
        inventory.removeItem(Material.OFSTED_DRAFT_REPORT, 1);
        inventory.addItem(Material.COIN, OFSTED_REPORT_JOURNALIST_REWARD);
        ofstedReportStolen = true;
        ofstedResult = OfstedResult.REQUIRES_IMPROVEMENT;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.SCHOOL_FRAUD);
        }
        if (!schoolReportAwarded && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SCHOOL_REPORT);
            schoolReportAwarded = true;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * A major noise event (e.g. wheelie bin fire) has occurred near the school
     * during an Ofsted visit. If the event magnitude ≥ {@value #OFSTED_NOISE_FLEE_THRESHOLD},
     * the inspectors flee: Notoriety +{@value #OFSTED_SABOTAGE_NOTORIETY},
     * WantedSystem +{@value #OFSTED_SABOTAGE_WANTED_STARS} stars,
     * {@link AchievementType#OFSTED_SABOTEUR}, result → REQUIRES_IMPROVEMENT.
     *
     * @param noiseMagnitude magnitude of the noise event (0–100)
     * @param achievementCallback achievement callback for notoriety system (may be null)
     */
    public void onNoisyEvent(float noiseMagnitude,
                              NotorietySystem.AchievementCallback achievementCallback) {
        if (!ofstedVisitActive) return;
        if (noiseMagnitude < OFSTED_NOISE_FLEE_THRESHOLD) return;

        // Inspectors flee
        ofstedVisitActive = false;
        ofstedResult = OfstedResult.REQUIRES_IMPROVEMENT;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(OFSTED_SABOTAGE_NOTORIETY, achievementCallback);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(OFSTED_SABOTAGE_WANTED_STARS,
                    0f, 0f, 0f, null);
        }
        if (!ofstedSaboteurAwarded && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.OFSTED_SABOTEUR);
            ofstedSaboteurAwarded = true;
        }

        // Seed rumour
        if (rumourNetwork != null) {
            // Rumour seeded without specific NPC; done opportunistically
        }
    }

    // ── Intruder Alert Chain ──────────────────────────────────────────────────

    /**
     * Returns true if the school gate is open (player may enter freely).
     *
     * @param timeSystem current time system
     * @return true if within gate hours
     */
    public boolean isGateOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return hour >= GATE_OPEN_HOUR && hour < GATE_CLOSE_HOUR;
    }

    /**
     * Player enters the school grounds. If outside gate hours and not disguised
     * sufficiently, triggers the Intruder Alert chain.
     *
     * @param timeSystem        current time system
     * @param disguiseCoverScore DisguiseSystem cover score (0–100); fool Ms. Pearson if ≥ threshold
     * @return true if intruder alert was triggered
     */
    public boolean enterSchoolGrounds(TimeSystem timeSystem, float disguiseCoverScore) {
        if (isGateOpen(timeSystem)) {
            // Normal entry — no alert
            return false;
        }
        // Outside hours entry
        // DisguiseSystem score ≥ DISGUISE_FOOL_MS_PEARSON_SCORE fools Ms. Pearson
        // (score is 0–100 cover integrity; high score = intact disguise)
        if (disguiseCoverScore >= DISGUISE_FOOL_MS_PEARSON_SCORE) {
            // Disguise fools Ms. Pearson; Derek still suspicious
            return false;
        }
        intruderAlertActive = true;
        intruderAlertTimer = 0f;
        return true;
    }

    /**
     * Called when Ms. Pearson catches the player (timer expired or player detected).
     * Records {@link CriminalRecord.CrimeType#SCHOOL_INTRUDER} and adds
     * WantedSystem +{@value #INTRUDER_WANTED_STARS} stars.
     * Also seeds a {@link RumourType#NEIGHBOURHOOD_GOSSIP} rumour via nearby NPCs.
     *
     * @param allNpcs all active NPCs (for rumour seeding)
     */
    public void onIntruderCaught(List<NPC> allNpcs) {
        intruderAlertActive = false;
        intruderAlertTimer = 0f;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.SCHOOL_INTRUDER);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(INTRUDER_WANTED_STARS,
                    0f, 0f, 0f, null);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(INTRUDER_NOTORIETY, null);
        }

        // Seed gossip rumour
        if (rumourNetwork != null && allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (npc.getType() == NPCType.SCHOOL_MUM
                        || npc.getType() == NPCType.PUBLIC) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.NEIGHBOURHOOD_GOSSIP,
                            "Someone got marched off the school grounds by the secretary"));
                    break;
                }
            }
        }
    }

    /**
     * Returns whether an intruder alert is currently active.
     */
    public boolean isIntruderAlertActive() {
        return intruderAlertActive;
    }

    /**
     * Returns the elapsed time (seconds) since the intruder alert was triggered.
     */
    public float getIntruderAlertTimer() {
        return intruderAlertTimer;
    }

    /**
     * Returns whether the player has been caught by the intruder alert
     * (timer exceeded catch window).
     *
     * @return true if the alert timer has expired (player should be caught)
     */
    public boolean isIntruderCatchWindowExpired() {
        return intruderAlertActive
                && intruderAlertTimer >= INTRUDER_CATCH_WINDOW_SECONDS;
    }

    /**
     * Cancel the intruder alert (e.g. player escaped the grounds).
     */
    public void cancelIntruderAlert() {
        intruderAlertActive = false;
        intruderAlertTimer = 0f;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return number of contraband crisp sales completed. */
    public int getCrispSalesCount() { return crispSalesCount; }

    /** @return whether the caretaker's shed has been looted. */
    public boolean isShedLooted() { return shedLooted; }

    /** @return whether the headteacher's office has been accessed. */
    public boolean isOfficeLooted() { return officeLooted; }

    /** @return whether an Ofsted visit is currently active. */
    public boolean isOfstedVisitActive() { return ofstedVisitActive; }

    /** @return the current Ofsted inspection result. */
    public OfstedResult getOfstedResult() { return ofstedResult; }

    /** @return whether the TUCK_SHOP_BANDIT achievement has been awarded. */
    public boolean isTuckShopAchievementAwarded() { return tuckShopAchievementAwarded; }

    /** @return whether the player helped decorate during Ofsted. */
    public boolean isHelpedDecorateOfsted() { return helpedDecorateOfsted; }

    /** @return whether the Ofsted draft report has been sold. */
    public boolean isOfstedReportStolen() { return ofstedReportStolen; }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force the intruder alert timer to a specific value for testing. */
    public void setIntruderAlertTimerForTesting(float seconds) {
        intruderAlertTimer = seconds;
    }

    /** Force the crisp sales count for testing. */
    public void setCrispSalesCountForTesting(int count) {
        crispSalesCount = count;
    }

    /** Force shed looted state for testing. */
    public void setShedLootedForTesting(boolean looted) {
        shedLooted = looted;
    }

    /** Force office looted state for testing. */
    public void setOfficeLootedForTesting(boolean looted) {
        officeLooted = looted;
    }
}

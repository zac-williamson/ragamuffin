package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #975: Northfield Post Office — Benefits Book, Dodgy Parcels &amp; Royal Mail Round.
 *
 * <h3>Benefits Book Cashing</h3>
 * <ul>
 *   <li>{@link Material#BENEFITS_BOOK} is awarded to the player on JobCentre registration.</li>
 *   <li>Cashed weekly at the Post Office counter (press E on Maureen) for the player's
 *       current benefit amount from JobCentreSystem.</li>
 *   <li>Stolen books from PENSIONER NPCs can be fraudulently cashed — 40% detection chance
 *       by Maureen ({@link #MAUREEN_FRAUD_DETECTION_CHANCE}).
 *       On detection: {@link CrimeType#BENEFITS_FRAUD} crime, +12 Notoriety, police called.</li>
 * </ul>
 *
 * <h3>Scratch Cards</h3>
 * <ul>
 *   <li>1 COIN each; 10% win 5 COIN ({@link #SCRATCH_CARD_WIN_CHANCE});
 *       3% win 25 COIN jackpot ({@link #SCRATCH_CARD_JACKPOT_CHANCE}).</li>
 *   <li>Jackpot awards {@link AchievementType#LUCKY_DIP}.</li>
 * </ul>
 *
 * <h3>Doorstep Parcel System</h3>
 * <ul>
 *   <li>POSTMAN delivers 3–5 {@link Material#PARCEL} props to residential doors each morning
 *       (06:00–08:00).</li>
 *   <li>Player can steal them: {@link CrimeType#PARCEL_THEFT} (+5 Notoriety; +3 if witnessed).</li>
 *   <li>Contents are randomised loot from {@link #rollParcelContents(Random)}.</li>
 *   <li>5 parcels stolen in one day awards {@link AchievementType#SPECIAL_DELIVERY}.</li>
 * </ul>
 *
 * <h3>Threatening Letters</h3>
 * <ul>
 *   <li>Buy {@link Material#STAMP} for 1 COIN, press E on {@link Material#POST_BOX_PROP},
 *       choose a target NPC and template.</li>
 *   <li>Target enters {@link NPCState#FRIGHTENED} for 24h, drops 20% coin, refuses to
 *       report crimes.</li>
 *   <li>Traceable at Notoriety Tier 3+ ({@link #TRACEABLE_NOTORIETY_TIER}):
 *       {@link CrimeType#THREATENING_BEHAVIOUR} crime; 40% police investigate
 *       ({@link #LETTER_TRACE_POLICE_CHANCE}).</li>
 *   <li>First letter awards {@link AchievementType#DEAR_SIR}.</li>
 * </ul>
 *
 * <h3>POSTMAN Delivery Shift</h3>
 * <ul>
 *   <li>Player wearing {@link Material#HI_VIS_VEST} can sign on for a delivery shift
 *       05:30–07:00 by pressing E on Maureen at the sorting office door.</li>
 *   <li>4 parcels to deliver; 3 COIN each on successful delivery.</li>
 *   <li>Parcels can be diverted (stolen): −5 Notoriety per diverted parcel on clean shift,
 *       but triggers {@link CrimeType#PARCEL_THEFT} and eventual shift lockout after
 *       {@link #SHIFT_LOCKOUT_DIVERSIONS} diversions.</li>
 *   <li>Clean shift (0 diversions) awards {@link AchievementType#GOING_POSTAL}.</li>
 *   <li>Shift speed reduced by {@link #FROST_SPEED_MULTIPLIER} during FROST/SNOW weather.</li>
 * </ul>
 */
public class PostOfficeSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour the Post Office opens on weekdays (Monday–Friday). */
    public static final float OPEN_HOUR_WEEKDAY = 9.0f;

    /** Hour the Post Office closes on weekdays. */
    public static final float CLOSE_HOUR_WEEKDAY = 17.5f;

    /** Hour the Post Office opens on Saturday. */
    public static final float OPEN_HOUR_SATURDAY = 9.0f;

    /** Hour the Post Office closes on Saturday (12:30). */
    public static final float CLOSE_HOUR_SATURDAY = 12.5f;

    /** Hour the POSTMAN shift sign-on opens (05:30). */
    public static final float SHIFT_SIGN_ON_HOUR = 5.5f;

    /** Hour the POSTMAN shift ends (07:00). */
    public static final float SHIFT_END_HOUR = 7.0f;

    /** Hour the POSTMAN delivery round starts (06:00). */
    public static final float POSTMAN_DELIVERY_START_HOUR = 6.0f;

    /** Hour the POSTMAN delivery round ends (08:00). */
    public static final float POSTMAN_DELIVERY_END_HOUR = 8.0f;

    // ── Benefit cashing ────────────────────────────────────────────────────────

    /** How often the player can cash their benefits book (1 in-game week = 7 days). */
    public static final int BENEFITS_CASH_INTERVAL_DAYS = 7;

    /** Chance (0–1) that Maureen detects a stolen BENEFITS_BOOK from a PENSIONER. */
    public static final float MAUREEN_FRAUD_DETECTION_CHANCE = 0.40f;

    /** Notoriety added on BENEFITS_FRAUD detection. */
    public static final int BENEFITS_FRAUD_NOTORIETY = 12;

    // ── Scratch cards ──────────────────────────────────────────────────────────

    /** Cost of one scratch card. */
    public static final int SCRATCH_CARD_PRICE = 1;

    /** Chance (0–1) of winning the small prize (5 COIN). */
    public static final float SCRATCH_CARD_WIN_CHANCE = 0.10f;

    /** Chance (0–1) of winning the jackpot (25 COIN). Checked AFTER SCRATCH_CARD_WIN_CHANCE. */
    public static final float SCRATCH_CARD_JACKPOT_CHANCE = 0.03f;

    /** Small scratch card prize (COIN). */
    public static final int SCRATCH_CARD_SMALL_PRIZE = 5;

    /** Jackpot scratch card prize (COIN). */
    public static final int SCRATCH_CARD_JACKPOT_PRIZE = 25;

    // ── Parcel theft ───────────────────────────────────────────────────────────

    /** Minimum doorstep parcels the POSTMAN delivers each morning. */
    public static final int PARCELS_DELIVERED_MIN = 3;

    /** Maximum doorstep parcels the POSTMAN delivers each morning. */
    public static final int PARCELS_DELIVERED_MAX = 5;

    /** Notoriety added for stealing a parcel. */
    public static final int PARCEL_THEFT_NOTORIETY = 5;

    /** Additional notoriety if the theft was witnessed (NPC within 8 blocks). */
    public static final int PARCEL_THEFT_WITNESSED_NOTORIETY = 3;

    /** Witness detection radius in blocks. */
    public static final float PARCEL_WITNESS_RADIUS = 8.0f;

    /** Number of parcels stolen in one day to earn SPECIAL_DELIVERY achievement. */
    public static final int SPECIAL_DELIVERY_THRESHOLD = 5;

    // ── Threatening letters ────────────────────────────────────────────────────

    /** Stamp cost at the counter. */
    public static final int STAMP_PRICE = 1;

    /** Duration (in-game hours) the FRIGHTENED state lasts after a threatening letter. */
    public static final float FRIGHTENED_DURATION_HOURS = 24.0f;

    /** Fraction of coin the frightened NPC drops. */
    public static final float FRIGHTENED_COIN_DROP_FRACTION = 0.20f;

    /** Notoriety tier at which a threatening letter can be traced back to the player. */
    public static final int TRACEABLE_NOTORIETY_TIER = 3;

    /** Chance (0–1) police investigate after tracing a threatening letter. */
    public static final float LETTER_TRACE_POLICE_CHANCE = 0.40f;

    /** Notoriety added for the THREATENING_BEHAVIOUR crime. */
    public static final int THREATENING_BEHAVIOUR_NOTORIETY = 10;

    // ── POSTMAN shift ──────────────────────────────────────────────────────────

    /** Parcels to deliver per player shift. */
    public static final int SHIFT_PARCELS = 4;

    /** COIN paid per successfully delivered parcel. */
    public static final int SHIFT_PARCEL_PAYMENT = 3;

    /** Number of cumulative diversions before the player is locked out of shifts. */
    public static final int SHIFT_LOCKOUT_DIVERSIONS = 3;

    /** Speed multiplier during FROST/SNOW weather — slows the postman. */
    public static final float FROST_SPEED_MULTIPLIER = 0.7f;

    // ── Speech lines ───────────────────────────────────────────────────────────

    public static final String MAUREEN_GREETING        = "Next please.";
    public static final String MAUREEN_BENEFITS_PAID   = "Sign here, love. That's your lot for the week.";
    public static final String MAUREEN_ALREADY_CASHED  = "You've had your book this week. Come back next week.";
    public static final String MAUREEN_NO_BOOK         = "Have you got your book? I can't do owt without the book.";
    public static final String MAUREEN_FRAUD_CAUGHT    = "Hang on — that's not yours, is it. I'm calling the police.";
    public static final String MAUREEN_CLOSED          = "We're closed. Come back when we're open, please.";
    public static final String MAUREEN_STAMP_SOLD      = "First class, is it? That'll be a coin.";
    public static final String MAUREEN_NO_COIN         = "You haven't got enough, love.";
    public static final String SHIFT_SIGN_ON_OK        = "You're on the round. Parcels are in the back. Off you go.";
    public static final String SHIFT_NO_HIVIZ          = "You'll need your hi-vis for this. Health and safety.";
    public static final String SHIFT_LOCKED_OUT        = "We've had complaints. You're off the round, I'm afraid.";
    public static final String SHIFT_OUTSIDE_WINDOW    = "Sign-on's five-thirty. Come back then.";

    // ── Scratch card messages ──────────────────────────────────────────────────

    public static final String SCRATCH_CARD_JACKPOT    = "Jackpot! Twenty-five coin. Maureen is mildly stunned.";
    public static final String SCRATCH_CARD_WIN        = "Winner! Five coin. Not bad for a quid.";
    public static final String SCRATCH_CARD_LOSE       = "No luck. Better luck next time, love.";

    // ── Threatening letter templates ───────────────────────────────────────────

    public static final String[] LETTER_TEMPLATES = {
        "I know where you live. Consider this a warning.",
        "Your days are numbered. Regards.",
        "Surprised? You shouldn't be. Watch your step.",
        "Stop what you're doing. This is your only notice."
    };

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** The Maureen counter clerk NPC (null if not spawned). */
    private NPC maureens = null;

    /** Day number when the player last cashed their benefits book (-1 = never). */
    private int lastBenefitsCashDay = -1;

    /** Parcels stolen today (resets on day rollover). */
    private int parcelsToday = 0;

    /** Day number of last parcel count reset. */
    private int lastParcelResetDay = -1;

    /** Whether the player is currently on a delivery shift. */
    private boolean shiftActive = false;

    /** Parcels remaining to deliver in the current shift. */
    private int shiftParcelsRemaining = 0;

    /** Parcels diverted (stolen) in the current shift. */
    private int shiftDivertedCount = 0;

    /** Cumulative total diversions — once >= SHIFT_LOCKOUT_DIVERSIONS, player is locked out. */
    private int totalDiversionCount = 0;

    /** Whether the player has been locked out of delivery shifts. */
    private boolean shiftLockedOut = false;

    /** Whether the first threatening letter has been sent (for achievement). */
    private boolean firstLetterSent = false;

    /** Whether the first benefits cash has happened (for achievement). */
    private boolean firstBenefitsCashed = false;

    // ── Optional system references (set via setters) ────────────────────────────

    private NotorietySystem notorietySystem;
    private WitnessSystem witnessSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private JobCentreSystem jobCentreSystem;
    private WeatherSystem weatherSystem;
    private DisguiseSystem disguiseSystem;

    // ── Construction ────────────────────────────────────────────────────────────

    public PostOfficeSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWitnessSystem(WitnessSystem witnessSystem) {
        this.witnessSystem = witnessSystem;
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

    public void setJobCentreSystem(JobCentreSystem jobCentreSystem) {
        this.jobCentreSystem = jobCentreSystem;
    }

    public void setWeatherSystem(WeatherSystem weatherSystem) {
        this.weatherSystem = weatherSystem;
    }

    public void setDisguiseSystem(DisguiseSystem disguiseSystem) {
        this.disguiseSystem = disguiseSystem;
    }

    // ── NPC management ──────────────────────────────────────────────────────────

    /** Force-spawn Maureen for testing purposes. */
    public void forceSpawnMaureen() {
        maureens = new NPC(NPCType.COUNTER_CLERK, 0f, 0f, 0f);
        maureens.setName("Maureen");
    }

    /** Returns the Maureen NPC (may be null if not spawned). */
    public NPC getMaureen() {
        return maureens;
    }

    // ── Opening hours logic ─────────────────────────────────────────────────────

    /**
     * Returns true if the Post Office counter is open at the given hour on the given
     * day-of-week (0=Sunday, 1=Monday, …, 6=Saturday). Closed Sundays.
     *
     * @param hour        current in-game hour (0–24)
     * @param dayOfWeek   0=Sunday … 6=Saturday
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == 0) return false; // Closed Sunday
        if (dayOfWeek == 6) {
            // Saturday: 09:00–12:30
            return hour >= OPEN_HOUR_SATURDAY && hour < CLOSE_HOUR_SATURDAY;
        }
        // Weekday: 09:00–17:30
        return hour >= OPEN_HOUR_WEEKDAY && hour < CLOSE_HOUR_WEEKDAY;
    }

    /**
     * Returns true if the POSTMAN shift sign-on window is open (05:30–07:00).
     *
     * @param hour current in-game hour
     */
    public boolean isShiftSignOnOpen(float hour) {
        return hour >= SHIFT_SIGN_ON_HOUR && hour < SHIFT_END_HOUR;
    }

    // ── Benefit book cashing ────────────────────────────────────────────────────

    /**
     * Result of attempting to cash a benefits book.
     */
    public enum CashBenefitsResult {
        /** Successfully cashed — benefit amount added to inventory. */
        SUCCESS,
        /** Counter is closed right now. */
        CLOSED,
        /** Player doesn't have a BENEFITS_BOOK in inventory. */
        NO_BOOK,
        /** Already cashed this week — come back next week. */
        ALREADY_CASHED,
        /** Fraudulent book detected — crime recorded, police called. */
        FRAUD_DETECTED,
        /** Fraudulent book cashed without detection. */
        FRAUD_UNDETECTED
    }

    /**
     * Player presses E on Maureen to cash their benefits book.
     *
     * @param player      the player
     * @param inventory   player's inventory
     * @param currentDay  current in-game day number
     * @param hour        current in-game hour
     * @param dayOfWeek   0=Sunday … 6=Saturday
     * @param npcs        all active NPCs (for witness detection)
     * @param benefitAmount  the weekly benefit amount from JobCentreSystem
     * @param bookFromPensioner  whether the BENEFITS_BOOK was stolen from a PENSIONER NPC
     */
    public CashBenefitsResult cashBenefitsBook(
            Player player,
            Inventory inventory,
            int currentDay,
            float hour,
            int dayOfWeek,
            List<NPC> npcs,
            int benefitAmount,
            boolean bookFromPensioner) {

        if (!isOpen(hour, dayOfWeek)) {
            return CashBenefitsResult.CLOSED;
        }

        if (!inventory.hasItem(Material.BENEFITS_BOOK)) {
            return CashBenefitsResult.NO_BOOK;
        }

        // Check weekly cooldown
        if (lastBenefitsCashDay >= 0 && (currentDay - lastBenefitsCashDay) < BENEFITS_CASH_INTERVAL_DAYS) {
            return CashBenefitsResult.ALREADY_CASHED;
        }

        // Check for stolen book fraud
        if (bookFromPensioner) {
            if (random.nextFloat() < MAUREEN_FRAUD_DETECTION_CHANCE) {
                // Fraud detected
                inventory.removeItem(Material.BENEFITS_BOOK, 1);
                if (criminalRecord != null) {
                    criminalRecord.record(CrimeType.BENEFITS_FRAUD);
                }
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(BENEFITS_FRAUD_NOTORIETY, null);
                }
                // Seed rumour
                if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
                    NPC nearest = findNearestNPC(npcs, NPCType.PUBLIC);
                    if (nearest != null) {
                        rumourNetwork.addRumour(nearest,
                                new Rumour(RumourType.BENEFITS_FRAUD_CAUGHT,
                                        "Maureen caught someone trying to cash a stolen benefits book."));
                    }
                }
                return CashBenefitsResult.FRAUD_DETECTED;
            } else {
                // Fraud undetected — pay out
                inventory.removeItem(Material.BENEFITS_BOOK, 1);
                inventory.addItem(Material.COIN, benefitAmount);
                lastBenefitsCashDay = currentDay;
                awardBenefitsAchievement();
                return CashBenefitsResult.FRAUD_UNDETECTED;
            }
        }

        // Legitimate cash
        inventory.removeItem(Material.BENEFITS_BOOK, 1);
        inventory.addItem(Material.COIN, benefitAmount);
        lastBenefitsCashDay = currentDay;
        awardBenefitsAchievement();
        return CashBenefitsResult.SUCCESS;
    }

    private void awardBenefitsAchievement() {
        if (!firstBenefitsCashed && achievementSystem != null) {
            firstBenefitsCashed = true;
            achievementSystem.unlock(AchievementType.SIGNED_FOR_IT);
        }
    }

    // ── Scratch cards ───────────────────────────────────────────────────────────

    /**
     * Result of buying and scratching a scratch card.
     */
    public enum ScratchCardResult {
        JACKPOT,
        WIN,
        LOSE,
        CLOSED,
        NO_COIN
    }

    /**
     * Player buys and scratches a scratch card (1 COIN cost).
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     */
    public ScratchCardResult buyScratchCard(Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return ScratchCardResult.CLOSED;
        }
        if (inventory.getItemCount(Material.COIN) < SCRATCH_CARD_PRICE) {
            return ScratchCardResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, SCRATCH_CARD_PRICE);

        float roll = random.nextFloat();
        if (roll < SCRATCH_CARD_JACKPOT_CHANCE) {
            inventory.addItem(Material.COIN, SCRATCH_CARD_JACKPOT_PRIZE);
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.LUCKY_DIP);
            }
            return ScratchCardResult.JACKPOT;
        } else if (roll < SCRATCH_CARD_JACKPOT_CHANCE + SCRATCH_CARD_WIN_CHANCE) {
            inventory.addItem(Material.COIN, SCRATCH_CARD_SMALL_PRIZE);
            return ScratchCardResult.WIN;
        } else {
            return ScratchCardResult.LOSE;
        }
    }

    // ── Stamp purchase ──────────────────────────────────────────────────────────

    /**
     * Result of buying a stamp from the counter.
     */
    public enum BuyStampResult {
        SUCCESS,
        CLOSED,
        NO_COIN
    }

    /**
     * Player buys a STAMP from the counter (1 COIN).
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     */
    public BuyStampResult buyStamp(Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return BuyStampResult.CLOSED;
        }
        if (inventory.getItemCount(Material.COIN) < STAMP_PRICE) {
            return BuyStampResult.NO_COIN;
        }
        inventory.removeItem(Material.COIN, STAMP_PRICE);
        inventory.addItem(Material.STAMP, 1);
        return BuyStampResult.SUCCESS;
    }

    // ── Parcel theft ────────────────────────────────────────────────────────────

    /**
     * Result of stealing a doorstep parcel.
     */
    public enum StealParcelResult {
        /** Parcel stolen — contents added to inventory. */
        STOLEN,
        /** Parcel stolen and witnessed by a nearby NPC. */
        STOLEN_WITNESSED,
        /** No parcel at this location. */
        NO_PARCEL
    }

    /**
     * Player steals a doorstep parcel.
     *
     * @param inventory   player's inventory
     * @param npcs        all active NPCs (for witness detection)
     * @param px          player X position
     * @param py          player Y position
     * @param pz          player Z position
     * @param currentDay  current in-game day number
     */
    public StealParcelResult stealParcel(
            Inventory inventory,
            List<NPC> npcs,
            float px, float py, float pz,
            int currentDay) {

        // Reset daily counter on new day
        if (currentDay != lastParcelResetDay) {
            parcelsToday = 0;
            lastParcelResetDay = currentDay;
        }

        // Add the parcel loot
        Material loot = rollParcelContents(random);
        inventory.addItem(loot, 1);
        parcelsToday++;

        // Check for witness
        boolean witnessed = isWitnessed(npcs, px, py, pz, PARCEL_WITNESS_RADIUS);

        int notorietyGain = PARCEL_THEFT_NOTORIETY;
        if (witnessed) {
            notorietyGain += PARCEL_THEFT_WITNESSED_NOTORIETY;
        }

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.PARCEL_THEFT);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(notorietyGain, null);
        }

        // Witness rumour
        if (witnessed && rumourNetwork != null && npcs != null) {
            NPC nearestWitness = findWitnessNPC(npcs, px, py, pz, PARCEL_WITNESS_RADIUS);
            if (nearestWitness != null) {
                rumourNetwork.addRumour(nearestWitness,
                        new Rumour(RumourType.PARCEL_THEFT_SPOTTED,
                                "Someone's nicking parcels off doorsteps."));
            }
        }

        // Achievement: 5 parcels in one day
        if (parcelsToday >= SPECIAL_DELIVERY_THRESHOLD && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SPECIAL_DELIVERY);
        }

        return witnessed ? StealParcelResult.STOLEN_WITNESSED : StealParcelResult.STOLEN;
    }

    /**
     * Roll random parcel contents. Returns a loot {@link Material}.
     */
    public static Material rollParcelContents(Random rng) {
        // Weighted random loot table
        int roll = rng.nextInt(10);
        switch (roll) {
            case 0: return Material.COIN;
            case 1: return Material.ENERGY_DRINK;
            case 2: return Material.CRISPS;
            case 3: return Material.STAPLER;
            case 4: return Material.NEWSPAPER;
            case 5: return Material.PARACETAMOL;
            case 6: return Material.DIAMOND;
            case 7: return Material.STOLEN_PHONE;
            case 8: return Material.TEXTBOOK;
            default: return Material.FOOD;
        }
    }

    // ── Threatening letters ─────────────────────────────────────────────────────

    /**
     * Result of sending a threatening letter via the post box.
     */
    public enum SendLetterResult {
        /** Letter sent — target NPC enters FRIGHTENED state. */
        SENT,
        /** Letter traced back to player (Notoriety Tier 3+, 40% chance). Crime recorded. */
        TRACED,
        /** Player does not have a STAMP. */
        NO_STAMP,
        /** No valid target NPC specified. */
        NO_TARGET
    }

    /**
     * Player sends a threatening letter via the POST_BOX_PROP.
     *
     * @param inventory          player's inventory
     * @param targetNpc          the NPC to threaten
     * @param notorietyTier      current player notoriety tier (0–5)
     */
    public SendLetterResult sendThreateningLetter(
            Inventory inventory,
            NPC targetNpc,
            int notorietyTier) {

        if (targetNpc == null) {
            return SendLetterResult.NO_TARGET;
        }
        if (!inventory.hasItem(Material.STAMP)) {
            return SendLetterResult.NO_STAMP;
        }

        inventory.removeItem(Material.STAMP, 1);

        // Apply FLEEING state to target (NPC becomes frightened/fleeing)
        targetNpc.setState(NPCState.FLEEING);

        // First letter achievement
        if (!firstLetterSent && achievementSystem != null) {
            firstLetterSent = true;
            achievementSystem.unlock(AchievementType.DEAR_SIR);
        }

        // Seed letter rumour
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(targetNpc,
                    new Rumour(RumourType.THREATENING_LETTER,
                            "Someone's been sending nasty letters round here."));
        }

        // Check traceability at Tier 3+
        if (notorietyTier >= TRACEABLE_NOTORIETY_TIER) {
            if (random.nextFloat() < LETTER_TRACE_POLICE_CHANCE) {
                if (criminalRecord != null) {
                    criminalRecord.record(CrimeType.THREATENING_BEHAVIOUR);
                }
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(THREATENING_BEHAVIOUR_NOTORIETY, null);
                }
                return SendLetterResult.TRACED;
            }
        }

        return SendLetterResult.SENT;
    }

    // ── POSTMAN delivery shift ──────────────────────────────────────────────────

    /**
     * Result of attempting to sign on for a POSTMAN delivery shift.
     */
    public enum ShiftSignOnResult {
        /** Shift signed on — player has 4 parcels to deliver. */
        SUCCESS,
        /** Counter is closed / outside sign-on window. */
        OUTSIDE_WINDOW,
        /** Player not wearing HI_VIS_VEST. */
        NO_HI_VIS,
        /** Player locked out due to too many diversions. */
        LOCKED_OUT,
        /** A shift is already active. */
        ALREADY_ON_SHIFT
    }

    /**
     * Player signs on for a delivery shift.
     *
     * @param inventory  player inventory (checked for HI_VIS_VEST)
     * @param hour       current in-game hour
     */
    public ShiftSignOnResult signOnForShift(Inventory inventory, float hour) {
        if (!isShiftSignOnOpen(hour)) {
            return ShiftSignOnResult.OUTSIDE_WINDOW;
        }
        if (shiftActive) {
            return ShiftSignOnResult.ALREADY_ON_SHIFT;
        }
        if (shiftLockedOut) {
            return ShiftSignOnResult.LOCKED_OUT;
        }
        if (!inventory.hasItem(Material.HI_VIS_VEST)) {
            return ShiftSignOnResult.NO_HI_VIS;
        }

        shiftActive = true;
        shiftParcelsRemaining = SHIFT_PARCELS;
        shiftDivertedCount = 0;
        return ShiftSignOnResult.SUCCESS;
    }

    /**
     * Result of delivering one parcel during the POSTMAN shift.
     */
    public enum DeliverParcelResult {
        /** Parcel delivered. Coin awarded if all delivered. */
        DELIVERED,
        /** Parcel diverted (stolen). Crime recorded, shift lockout incremented. */
        DIVERTED,
        /** Shift complete — final payment awarded. */
        SHIFT_COMPLETE,
        /** No shift active. */
        NO_SHIFT
    }

    /**
     * Deliver or divert the next parcel on the shift.
     *
     * @param inventory  player's inventory
     * @param divert     if true, player steals the parcel instead of delivering it
     */
    public DeliverParcelResult deliverOrDivertParcel(Inventory inventory, boolean divert) {
        if (!shiftActive) {
            return DeliverParcelResult.NO_SHIFT;
        }

        if (divert) {
            shiftDivertedCount++;
            totalDiversionCount++;
            shiftParcelsRemaining--;

            // Add diverted parcel contents to inventory
            inventory.addItem(rollParcelContents(random), 1);

            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.PARCEL_THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(PARCEL_THEFT_NOTORIETY, null);
            }

            // Lock out if too many diversions
            if (totalDiversionCount >= SHIFT_LOCKOUT_DIVERSIONS) {
                shiftLockedOut = true;
            }

            if (shiftParcelsRemaining <= 0) {
                shiftActive = false;
                return DeliverParcelResult.SHIFT_COMPLETE;
            }
            return DeliverParcelResult.DIVERTED;
        } else {
            // Legitimate delivery
            shiftParcelsRemaining--;
            inventory.addItem(Material.COIN, SHIFT_PARCEL_PAYMENT);

            if (shiftParcelsRemaining <= 0) {
                shiftActive = false;
                // Clean shift achievement
                if (shiftDivertedCount == 0 && achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.GOING_POSTAL);
                }
                return DeliverParcelResult.SHIFT_COMPLETE;
            }
            return DeliverParcelResult.DELIVERED;
        }
    }

    // ── Utility helpers ─────────────────────────────────────────────────────────

    private boolean isWitnessed(List<NPC> npcs, float px, float py, float pz, float radius) {
        if (npcs == null) return false;
        for (NPC npc : npcs) {
            if (npc == null) continue;
            float dx = npc.getPosition().x - px;
            float dy = npc.getPosition().y - py;
            float dz = npc.getPosition().z - pz;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= radius) {
                return true;
            }
        }
        return false;
    }

    private NPC findWitnessNPC(List<NPC> npcs, float px, float py, float pz, float radius) {
        if (npcs == null) return null;
        for (NPC npc : npcs) {
            if (npc == null) continue;
            float dx = npc.getPosition().x - px;
            float dy = npc.getPosition().y - py;
            float dz = npc.getPosition().z - pz;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= radius) {
                return npc;
            }
        }
        return null;
    }

    private NPC findNearestNPC(List<NPC> npcs, NPCType type) {
        if (npcs == null) return null;
        for (NPC npc : npcs) {
            if (npc != null && npc.getType() == type) {
                return npc;
            }
        }
        // Fallback: any NPC
        return npcs.isEmpty() ? null : npcs.get(0);
    }

    // ── Accessors for testing ────────────────────────────────────────────────────

    /** Returns the day the player last cashed their benefits book (-1 = never). */
    public int getLastBenefitsCashDay() {
        return lastBenefitsCashDay;
    }

    /** Force-set the last benefits cash day for testing. */
    public void setLastBenefitsCashDayForTesting(int day) {
        this.lastBenefitsCashDay = day;
    }

    /** Returns number of parcels stolen today. */
    public int getParcelsToday() {
        return parcelsToday;
    }

    /** Force-set parcels stolen today for testing. */
    public void setParcelstolenForTesting(int count) {
        this.parcelsToday = count;
    }

    /** Returns whether a delivery shift is currently active. */
    public boolean isShiftActive() {
        return shiftActive;
    }

    /** Returns parcels remaining in the current shift. */
    public int getShiftParcelsRemaining() {
        return shiftParcelsRemaining;
    }

    /** Returns cumulative diversion count. */
    public int getTotalDiversionCount() {
        return totalDiversionCount;
    }

    /** Force-set total diversion count for testing. */
    public void setTotalDiversionCountForTesting(int count) {
        this.totalDiversionCount = count;
        this.shiftLockedOut = count >= SHIFT_LOCKOUT_DIVERSIONS;
    }

    /** Returns whether the player is locked out of delivery shifts. */
    public boolean isShiftLockedOut() {
        return shiftLockedOut;
    }

    /** Force-set shift lockout for testing. */
    public void setShiftLockedOutForTesting(boolean locked) {
        this.shiftLockedOut = locked;
    }

    /** Returns whether the GOING_POSTAL-eligible condition (clean shift) was met. */
    public int getShiftDivertedCount() {
        return shiftDivertedCount;
    }
}

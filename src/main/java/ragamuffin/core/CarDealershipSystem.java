package ragamuffin.core;

import ragamuffin.ai.CarManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarCondition;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1227: Northfield Wheelwright Motors — Dodgy Car Lot, Clock &amp; Swap
 * &amp; the VIN Plate Swap.
 *
 * <p>Wayne's sun-bleached used car lot on the edge of the industrial estate,
 * hemmed in by the scrapyard and the hand car wash. A 20×14-block tarmac
 * forecourt with 12 CAR_PROP bays, a PORTACABIN_PROP at the rear,
 * CAR_LOT_SIGN_PROP, and BUNTING_PROP.
 *
 * <h3>Core mechanics</h3>
 * <ul>
 *   <li><b>Buying</b> — inspect cars via E; haggle with Wayne
 *       (≥80% for MINT/TIDY, ≥60% for ROUGH/BANGER). Finance option
 *       (10 COIN deposit, 5 COIN/day for 10 days). Awards WHEELER_DEALER,
 *       ON_THE_NEVER_NEVER.</li>
 *   <li><b>Selling / Part-Ex</b> — Wayne offers 50–70% of retail. Stolen cars
 *       require V5C_PROP or FAKE_V5C. Without docs: police called, Notoriety +10,
 *       HANDLING_STOLEN_GOODS. With FAKE_V5C: DOCUMENT_FRAUD, Notoriety +5,
 *       CLEAN_TITLE achievement.</li>
 *   <li><b>Clocking</b> — bribe Bez (5 COIN) + MILEAGE_CORRECTOR_PROP to set
 *       ROUGH/BANGER → TIDY (+15 COIN resale). CONSUMER_FRAUD if caught by
 *       TRADING_STANDARDS NPC within 20 blocks. Awards DODGY_MILEAGE.</li>
 *   <li><b>VIN Plate Swap</b> — SCREWDRIVER + donor car from Scrapyard. Car
 *       unregistered for 3 days; then 10% daily ANPR check by POLICE NPC.
 *       Awards VIN_SWAP.</li>
 *   <li><b>Repossession</b> — 3 missed finance payments → REPO_MAN takes car.
 *       Awards REPOSSESSED.</li>
 *   <li><b>Wayne's Banter</b> — 12 speech lines, 45-second cycle.</li>
 * </ul>
 *
 * <h3>Opening hours</h3>
 * Mon–Sat 09:00–18:00. Closed Sunday.
 *
 * <h3>Achievements</h3>
 * WHEELER_DEALER, ON_THE_NEVER_NEVER, CLEAN_TITLE, DODGY_MILEAGE,
 * VIN_SWAP, REPOSSESSED.
 */
public class CarDealershipSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the lot opens Mon–Sat. */
    public static final float OPEN_HOUR = 9.0f;

    /** Hour the lot closes (exclusive). */
    public static final float CLOSE_HOUR = 18.0f;

    /** Hour Bez stops working on cars (moves inside portacabin). */
    public static final float BEZ_FINISH_HOUR = 16.0f;

    /** Hour Wayne retreats to portacabin. */
    public static final float WAYNE_PORTACABIN_HOUR = 17.0f;

    // ── Pricing constants ─────────────────────────────────────────────────────

    /** Minimum asking price for a forecourt car (COIN). */
    public static final int MIN_ASKING_PRICE = 10;

    /** Maximum asking price for a forecourt car (COIN). */
    public static final int MAX_ASKING_PRICE = 80;

    /** Minimum resale ratio Wayne pays (50% of retail). */
    public static final float RESALE_RATIO_MIN = 0.50f;

    /** Maximum resale ratio Wayne pays (70% of retail). */
    public static final float RESALE_RATIO_MAX = 0.70f;

    /** Minimum haggle ratio for MINT/TIDY cars (80% of asking). */
    public static final float HAGGLE_THRESHOLD_GOOD = 0.80f;

    /** Minimum haggle ratio for ROUGH/BANGER cars (60% of asking). */
    public static final float HAGGLE_THRESHOLD_ROUGH = 0.60f;

    /** Finance deposit amount (COIN). */
    public static final int FINANCE_DEPOSIT = 10;

    /** Finance daily payment (COIN per in-game day). */
    public static final int FINANCE_DAILY = 5;

    /** Number of finance payment days. */
    public static final int FINANCE_DAYS = 10;

    /** Clocking resale bonus (COIN added to car's price). */
    public static final int CLOCK_PRICE_BONUS = 15;

    /** Bribe cost to clock a car with Bez (COIN). */
    public static final int BEZ_CLOCK_BRIBE = 5;

    /** Notoriety penalty for selling a stolen car without documents. */
    public static final int STOLEN_NO_DOCS_NOTORIETY = 10;

    /** Notoriety penalty for selling with FAKE_V5C. */
    public static final int STOLEN_FAKE_DOCS_NOTORIETY = 5;

    /** Notoriety penalty for selling a clocked car (caught by Trading Standards). */
    public static final int CONSUMER_FRAUD_NOTORIETY = 8;

    /** Notoriety penalty for VIN swap. */
    public static final int VIN_SWAP_NOTORIETY = 3;

    /** Notoriety penalty for ANPR detection (added to WantedSystem). */
    public static final int ANPR_WANTED_STARS = 1;

    /** Range (blocks) within which TRADING_STANDARDS NPC triggers fraud detection. */
    public static final float TRADING_STANDARDS_RANGE = 20.0f;

    /** Daily probability (0–1) that a patrolling POLICE NPC runs an ANPR check. */
    public static final float ANPR_DAILY_CHANCE = 0.10f;

    /** In-game days before ANPR starts flagging a VIN-swapped car. */
    public static final int VIN_GRACE_DAYS = 3;

    /** Wayne's banter cycle interval (seconds). */
    public static final float BANTER_CYCLE_SECONDS = 45.0f;

    // ── Wayne's banter lines ──────────────────────────────────────────────────

    /** 12 flavour speech lines cycled every 45 seconds. */
    public static final String[] WAYNE_BANTER = {
        "Every car on that lot drives sweet as a nut, mate.",
        "That one's only had one previous owner. A nun. Honest.",
        "I'm losing money at that price. Killing myself here.",
        "Mileage? Oh, that's a genuine reading, that is.",
        "Don't listen to Bez. He's still learning.",
        "You want it valeted? Bez'll give it a quick once-over.",
        "Look, I don't ask questions, and neither should you.",
        "That one came in Saturday. Gone by Tuesday at that price.",
        "MOT runs out when? That's your problem, son.",
        "Full service history. Well, most of it.",
        "Lovely motor, that. My uncle had one. He loved it.",
        "Cash? Oh, we can talk about cash."
    };

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Index into WAYNE_BANTER for the current speech line. */
    private int banterIndex = 0;

    /** Seconds elapsed since last banter line. */
    private float banterTimer = 0f;

    /** Whether the WHEELER_DEALER achievement has been awarded this session. */
    private boolean wheelerDealerAwarded = false;

    /** Whether the ON_THE_NEVER_NEVER achievement has been awarded this session. */
    private boolean onTheNeverNeverAwarded = false;

    /** Whether the CLEAN_TITLE achievement has been awarded this session. */
    private boolean cleanTitleAwarded = false;

    /** Whether the DODGY_MILEAGE achievement has been awarded this session. */
    private boolean dodgyMileageAwarded = false;

    /** Whether the VIN_SWAP achievement has been awarded this session. */
    private boolean vinSwapAwarded = false;

    /** Whether the REPOSSESSED achievement has been awarded this session. */
    private boolean repossessedAwarded = false;

    /** Outstanding finance payment days remaining (0 = paid off or no finance). */
    private int financePaymentsRemaining = 0;

    /** Number of consecutive missed finance payments. */
    private int missedPayments = 0;

    /** The car currently under finance (null if no active finance). */
    private Car financedCar = null;

    /** Optional reference to NotorietySystem for Notoriety changes. */
    private NotorietySystem notorietySystem;

    /** Optional reference to RumourNetwork for seeding rumours. */
    private RumourNetwork rumourNetwork;

    /** Optional reference to CriminalRecord for recording crimes. */
    private CriminalRecord criminalRecord;

    /** Optional reference to CarManager for managing player-owned cars. */
    private CarManager carManager;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of inspecting a car on the forecourt. */
    public enum CarInspectResult {
        /** Inspection successful; car details available. */
        SUCCESS,
        /** No car at the given location. */
        NO_CAR_PRESENT,
        /** Lot is closed. */
        LOT_CLOSED
    }

    /** Result of a haggling attempt. */
    public enum HaggleResult {
        /** Wayne accepted the offer. */
        ACCEPTED,
        /** Wayne rejected the offer ("You're 'avin' a bubble."). */
        REJECTED,
        /** Lot is closed. */
        LOT_CLOSED,
        /** Player has insufficient funds. */
        INSUFFICIENT_FUNDS
    }

    /** Result of a car purchase on finance. */
    public enum FinanceResult {
        /** Finance approved; deposit taken. */
        APPROVED,
        /** Lot is closed. */
        LOT_CLOSED,
        /** Player has insufficient funds for deposit. */
        INSUFFICIENT_FUNDS_DEPOSIT,
        /** Finance already active. */
        FINANCE_ALREADY_ACTIVE
    }

    /** Result of selling a car to Wayne. */
    public enum SellResult {
        /** Sale completed successfully. */
        SOLD,
        /** Sale refused — lot is closed. */
        LOT_CLOSED,
        /** Car is stolen and player has no V5C or FAKE_V5C — police called. */
        POLICE_CALLED,
        /** Car is stolen; player had FAKE_V5C — sold but DOCUMENT_FRAUD recorded. */
        SOLD_WITH_FAKE_DOCS,
        /** No car to sell. */
        NO_CAR
    }

    /** Result of clocking a car with Bez. */
    public enum ClockResult {
        /** Odometer successfully clocked. */
        SUCCESS,
        /** Car is already TIDY or MINT — clocking not applicable. */
        NOT_APPLICABLE,
        /** Player has no MILEAGE_CORRECTOR_PROP. */
        NO_CORRECTOR,
        /** Player has insufficient funds for bribe. */
        INSUFFICIENT_FUNDS,
        /** Bez is unavailable (outside 09:00–16:00 or FROST before 10:00). */
        BEZ_UNAVAILABLE
    }

    /** Result of a VIN plate swap. */
    public enum VinSwapResult {
        /** VIN successfully swapped. */
        SUCCESS,
        /** No SCREWDRIVER in inventory. */
        NO_SCREWDRIVER,
        /** No donor car available at the Scrapyard. */
        NO_DONOR_CAR,
        /** Car is already VIN-swapped. */
        ALREADY_SWAPPED
    }

    /** Result of a finance payment. */
    public enum PaymentResult {
        /** Payment accepted; finance updated. */
        PAID,
        /** Finance already paid off. */
        NO_ACTIVE_FINANCE,
        /** Insufficient funds. */
        INSUFFICIENT_FUNDS
    }

    /** Result of an ANPR check on a car. */
    public enum AnprResult {
        /** Car is clean — no action. */
        CLEAN,
        /** Car flagged; Wanted +1 star. */
        FLAGGED,
        /** Car is within VIN grace period — not flagged yet. */
        GRACE_PERIOD
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Create a new CarDealershipSystem.
     *
     * @param random seeded Random for deterministic car generation and tests
     */
    public CarDealershipSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    /** Inject optional NotorietySystem reference. */
    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    /** Inject optional RumourNetwork reference. */
    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    /** Inject optional CriminalRecord reference. */
    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    /** Inject optional CarManager reference. */
    public void setCarManager(CarManager carManager) {
        this.carManager = carManager;
    }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns true if Wheelwright Motors is open at the given hour on the given day.
     * Open Mon–Sat (dayOfWeek 1=Mon … 6=Sat; 0=Sun). Closed Sunday.
     * THUNDERSTORM closes the lot 2 hours early.
     *
     * @param hour        current in-game hour (0.0–23.99)
     * @param dayOfWeek   0=Sunday, 1=Monday … 6=Saturday
     * @param thunderstorm true if current weather is thunderstorm
     */
    public boolean isOpen(float hour, int dayOfWeek, boolean thunderstorm) {
        if (dayOfWeek == 0) return false; // Closed Sunday
        float closeHour = thunderstorm ? CLOSE_HOUR - 2.0f : CLOSE_HOUR;
        return hour >= OPEN_HOUR && hour < closeHour;
    }

    /**
     * Convenience overload — no thunderstorm.
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        return isOpen(hour, dayOfWeek, false);
    }

    /**
     * Returns true if Bez is available for clocking at the given hour and weather.
     * Bez works 09:00–16:00. During FROST he is unavailable until 10:00.
     *
     * @param hour  current in-game hour
     * @param frost true if current weather is FROST
     */
    public boolean isBezAvailable(float hour, boolean frost) {
        float bezStartHour = frost ? 10.0f : OPEN_HOUR;
        return hour >= bezStartHour && hour < BEZ_FINISH_HOUR;
    }

    // ── Car inspection ────────────────────────────────────────────────────────

    /**
     * Inspect a car on the forecourt.
     * Returns a {@link CarInspectResult} indicating success or failure.
     * On success the car's condition and a generated mileage are accessible
     * via the car entity itself.
     *
     * @param car        the car prop being inspected
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     * @return inspection result
     */
    public CarInspectResult inspectCar(Car car, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) return CarInspectResult.LOT_CLOSED;
        if (car == null) return CarInspectResult.NO_CAR_PRESENT;
        return CarInspectResult.SUCCESS;
    }

    // ── Haggling ─────────────────────────────────────────────────────────────

    /**
     * Attempt to buy a car at the given offer price.
     * Wayne accepts if:
     * <ul>
     *   <li>MINT/TIDY: offer ≥ 80% of asking price.</li>
     *   <li>ROUGH/BANGER: offer ≥ 60% of asking price.</li>
     * </ul>
     * On first successful haggle below asking price, awards WHEELER_DEALER.
     *
     * @param askingPrice  the price Wayne is asking (COIN)
     * @param offer        the price the player offers (COIN)
     * @param condition    the car's condition rating
     * @param inventory    the player's inventory (COIN deducted on acceptance)
     * @param hour         current in-game hour
     * @param dayOfWeek    0=Sunday … 6=Saturday
     * @param cb           achievement callback
     * @return haggle result
     */
    public HaggleResult haggle(int askingPrice, int offer, CarCondition condition,
                               Inventory inventory, float hour, int dayOfWeek,
                               NotorietySystem.AchievementCallback cb) {
        if (!isOpen(hour, dayOfWeek)) return HaggleResult.LOT_CLOSED;
        if (inventory.getItemCount(Material.COIN) < offer) return HaggleResult.INSUFFICIENT_FUNDS;

        float threshold = (condition == CarCondition.MINT || condition == CarCondition.TIDY)
                ? HAGGLE_THRESHOLD_GOOD
                : HAGGLE_THRESHOLD_ROUGH;

        float ratio = (float) offer / (float) askingPrice;
        if (ratio < threshold) {
            return HaggleResult.REJECTED;
        }

        // Accept: deduct coin
        inventory.removeItem(Material.COIN, offer);

        // Award WHEELER_DEALER on first haggle below asking price
        if (offer < askingPrice && !wheelerDealerAwarded) {
            wheelerDealerAwarded = true;
            if (cb != null) cb.award(AchievementType.WHEELER_DEALER);
        }

        return HaggleResult.ACCEPTED;
    }

    // ── Finance ───────────────────────────────────────────────────────────────

    /**
     * Start a finance agreement for a car.
     * Player pays a 10 COIN deposit; 5 COIN/day for 10 days is then due.
     * Awards ON_THE_NEVER_NEVER on first finance purchase.
     *
     * @param car        the car being financed
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     * @param cb         achievement callback
     * @return finance result
     */
    public FinanceResult startFinance(Car car, Inventory inventory, float hour,
                                     int dayOfWeek, NotorietySystem.AchievementCallback cb) {
        if (!isOpen(hour, dayOfWeek)) return FinanceResult.LOT_CLOSED;
        if (financedCar != null) return FinanceResult.FINANCE_ALREADY_ACTIVE;
        if (inventory.getItemCount(Material.COIN) < FINANCE_DEPOSIT) {
            return FinanceResult.INSUFFICIENT_FUNDS_DEPOSIT;
        }

        inventory.removeItem(Material.COIN, FINANCE_DEPOSIT);
        financedCar = car;
        financePaymentsRemaining = FINANCE_DAYS;
        missedPayments = 0;

        if (!onTheNeverNeverAwarded) {
            onTheNeverNeverAwarded = true;
            if (cb != null) cb.award(AchievementType.ON_THE_NEVER_NEVER);
        }

        return FinanceResult.APPROVED;
    }

    /**
     * Process a daily finance payment attempt.
     *
     * @param inventory  player's inventory
     * @return payment result
     */
    public PaymentResult makeFinancePayment(Inventory inventory) {
        if (financedCar == null || financePaymentsRemaining <= 0) {
            return PaymentResult.NO_ACTIVE_FINANCE;
        }
        if (inventory.getItemCount(Material.COIN) < FINANCE_DAILY) {
            return PaymentResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, FINANCE_DAILY);
        financePaymentsRemaining--;
        missedPayments = 0;
        if (financePaymentsRemaining <= 0) {
            financedCar = null;
        }
        return PaymentResult.PAID;
    }

    /**
     * Called each in-game day when a finance payment is missed.
     * Tracks missed payment count and handles repossession logic:
     * <ul>
     *   <li>Miss 1: spawn REPO_MAN NPC; one-day grace.</li>
     *   <li>Miss 2: REPO_MAN appears again; player can pay/scarper/bribe.</li>
     *   <li>Miss 3: car removed from CarManager overnight; REPOSSESSED awarded.</li>
     * </ul>
     *
     * @param cb achievement callback
     * @return the number of missed payments after this miss
     */
    public int recordMissedPayment(NotorietySystem.AchievementCallback cb) {
        if (financedCar == null) return 0;
        missedPayments++;

        if (missedPayments >= 3) {
            // Repossess the car
            if (carManager != null) {
                carManager.getCars().remove(financedCar);
            }
            financedCar = null;
            financePaymentsRemaining = 0;
            missedPayments = 0;

            if (!repossessedAwarded) {
                repossessedAwarded = true;
                if (cb != null) cb.award(AchievementType.REPOSSESSED);
            }
        }
        return missedPayments;
    }

    // ── Selling ───────────────────────────────────────────────────────────────

    /**
     * Sell a car to Wayne.
     * <ul>
     *   <li>Normal car: Wayne pays 50–70% of retail (based on asking price).</li>
     *   <li>Stolen car, no docs: police called; Notoriety +10;
     *       HANDLING_STOLEN_GOODS recorded.</li>
     *   <li>Stolen car, FAKE_V5C: sold; DOCUMENT_FRAUD recorded; Notoriety +5;
     *       CLEAN_TITLE achievement.</li>
     *   <li>Stolen car, V5C_PROP: sold cleanly (40% of retail).</li>
     * </ul>
     *
     * @param car          the car being sold
     * @param retailPrice  the car's retail asking price (COIN)
     * @param inventory    player's inventory (COIN added; docs consumed)
     * @param hour         current in-game hour
     * @param dayOfWeek    0=Sunday … 6=Saturday
     * @param cb           achievement callback
     * @return sell result
     */
    public SellResult sellCar(Car car, int retailPrice, Inventory inventory,
                              float hour, int dayOfWeek,
                              NotorietySystem.AchievementCallback cb) {
        if (car == null) return SellResult.NO_CAR;
        if (!isOpen(hour, dayOfWeek)) return SellResult.LOT_CLOSED;

        if (car.isStolen()) {
            boolean hasV5C = inventory.hasItem(Material.V5C_PROP);
            boolean hasFakeV5C = inventory.hasItem(Material.FAKE_V5C);

            if (!hasV5C && !hasFakeV5C) {
                // Police called
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(STOLEN_NO_DOCS_NOTORIETY, cb);
                }
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.HANDLING_STOLEN_GOODS);
                }
                if (rumourNetwork != null) {
                    rumourNetwork.addRumour(null, new Rumour(RumourType.DODGY_DEAL, "Heard someone tried to flog a bent motor round Wheelwright's."));
                }
                return SellResult.POLICE_CALLED;
            }

            if (hasFakeV5C) {
                inventory.removeItem(Material.FAKE_V5C, 1);
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(STOLEN_FAKE_DOCS_NOTORIETY, cb);
                }
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.DOCUMENT_FRAUD);
                }
                int payment = Math.round(retailPrice * 0.40f);
                inventory.addItem(Material.COIN, payment);
                if (!cleanTitleAwarded) {
                    cleanTitleAwarded = true;
                    if (cb != null) cb.award(AchievementType.CLEAN_TITLE);
                }
                seedSaleRumour();
                return SellResult.SOLD_WITH_FAKE_DOCS;
            }

            // Has legitimate V5C — sold at 40% of retail
            inventory.removeItem(Material.V5C_PROP, 1);
            int payment = Math.round(retailPrice * 0.40f);
            inventory.addItem(Material.COIN, payment);
            seedSaleRumour();
            return SellResult.SOLD;
        }

        // Normal sale: 50–70% of retail
        float ratio = RESALE_RATIO_MIN + random.nextFloat() * (RESALE_RATIO_MAX - RESALE_RATIO_MIN);
        int payment = Math.round(retailPrice * ratio);
        inventory.addItem(Material.COIN, payment);
        if (carManager != null) {
            carManager.getCars().remove(car);
        }
        seedSaleRumour();
        return SellResult.SOLD;
    }

    // ── Clocking ──────────────────────────────────────────────────────────────

    /**
     * Clock a car's odometer with Bez's help.
     * Requires: car condition ROUGH or BANGER; MILEAGE_CORRECTOR_PROP in inventory;
     * 5 COIN bribe; Bez available.
     * On success: condition set to TIDY; car flagged as clocked.
     *
     * @param car          the car to clock
     * @param inventory    player's inventory (MILEAGE_CORRECTOR consumed; 5 COIN deducted)
     * @param hour         current in-game hour
     * @param frost        true if weather is FROST
     * @return clock result
     */
    public ClockResult clockOdometer(Car car, Inventory inventory,
                                     float hour, boolean frost) {
        if (car == null) return ClockResult.NOT_APPLICABLE;
        CarCondition cond = car.getCondition();
        if (cond == CarCondition.MINT || cond == CarCondition.TIDY) {
            return ClockResult.NOT_APPLICABLE;
        }
        if (!isBezAvailable(hour, frost)) return ClockResult.BEZ_UNAVAILABLE;
        if (!inventory.hasItem(Material.MILEAGE_CORRECTOR)) return ClockResult.NO_CORRECTOR;
        if (inventory.getItemCount(Material.COIN) < BEZ_CLOCK_BRIBE) {
            return ClockResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.MILEAGE_CORRECTOR, 1);
        inventory.removeItem(Material.COIN, BEZ_CLOCK_BRIBE);
        car.setCondition(CarCondition.TIDY);
        car.setClocked(true);
        return ClockResult.SUCCESS;
    }

    /**
     * Called when a clocked car is sold to a civilian via StreetEconomySystem.
     * If tradingStandardsNearby: records CONSUMER_FRAUD; Notoriety +8;
     * awards DODGY_MILEAGE.
     *
     * @param car                    the clocked car being sold
     * @param tradingStandardsNearby true if a TRADING_STANDARDS NPC is within 20 blocks
     * @param cb                     achievement callback
     */
    public void onClockedCarSoldToCivilian(Car car, boolean tradingStandardsNearby,
                                           NotorietySystem.AchievementCallback cb) {
        if (car == null || !car.isClocked()) return;
        if (tradingStandardsNearby) {
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CONSUMER_FRAUD_NOTORIETY, cb);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.CONSUMER_FRAUD);
            }
            if (!dodgyMileageAwarded) {
                dodgyMileageAwarded = true;
                if (cb != null) cb.award(AchievementType.DODGY_MILEAGE);
            }
        }
    }

    // ── VIN plate swap ────────────────────────────────────────────────────────

    /**
     * Swap VIN plates between the player's car and a donor car from the Scrapyard.
     * Requires: SCREWDRIVER in inventory; donor car available.
     * On success: car.vinSwapped = true; car.vinSwapDaysElapsed = 0;
     * Notoriety +3; VIN_SWAP achievement awarded (once).
     *
     * @param car        the player's car to re-plate
     * @param donorCar   the donor car from the Scrapyard (may be null if none available)
     * @param inventory  player's inventory (SCREWDRIVER not consumed)
     * @param cb         achievement callback
     * @return VIN swap result
     */
    public VinSwapResult vinSwap(Car car, Car donorCar, Inventory inventory,
                                 NotorietySystem.AchievementCallback cb) {
        if (car == null) return VinSwapResult.NO_DONOR_CAR;
        if (car.isVinSwapped()) return VinSwapResult.ALREADY_SWAPPED;
        if (!inventory.hasItem(Material.SCREWDRIVER)) return VinSwapResult.NO_SCREWDRIVER;
        if (donorCar == null) return VinSwapResult.NO_DONOR_CAR;

        car.setVinSwapped(true);
        // Donor car VIN is now "used" — mark it as VIN-swapped too (scrapped)
        donorCar.setVinSwapped(true);

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(VIN_SWAP_NOTORIETY, cb);
        }
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(null, new Rumour(RumourType.DODGY_DEAL, "Word is someone's been swapping plates round Pearce's yard."));
        }

        if (!vinSwapAwarded) {
            vinSwapAwarded = true;
            if (cb != null) cb.award(AchievementType.VIN_SWAP);
        }

        return VinSwapResult.SUCCESS;
    }

    // ── ANPR check ────────────────────────────────────────────────────────────

    /**
     * Run an ANPR check on a car driven or parked by the player.
     * Called once per in-game day by the game loop.
     * <ul>
     *   <li>If car is clean (not VIN-swapped): CLEAN.</li>
     *   <li>If VIN-swapped within grace period: GRACE_PERIOD.</li>
     *   <li>After grace: 10% random chance → FLAGGED (WantedSystem +1 star).</li>
     * </ul>
     *
     * @param car          the car to check
     * @param randomValue  a pre-generated float in [0.0, 1.0) for deterministic tests
     * @return ANPR result
     */
    public AnprResult anprCheck(Car car, float randomValue) {
        if (car == null || !car.isVinSwapped()) return AnprResult.CLEAN;
        if (car.getVinSwapDaysElapsed() < VIN_GRACE_DAYS) return AnprResult.GRACE_PERIOD;
        if (randomValue < ANPR_DAILY_CHANCE) {
            return AnprResult.FLAGGED;
        }
        return AnprResult.CLEAN;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update for Wayne's banter cycle and VIN grace period tracking.
     * Call once per frame from the main game loop.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        banterTimer += delta;
        if (banterTimer >= BANTER_CYCLE_SECONDS) {
            banterTimer = 0f;
            banterIndex = (banterIndex + 1) % WAYNE_BANTER.length;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the current Wayne banter line (cycles every 45 seconds).
     */
    public String getCurrentBanter() {
        return WAYNE_BANTER[banterIndex];
    }

    /**
     * Returns the computed resale offer Wayne would give for a car at a given
     * retail price (50–70% range, deterministic based on this system's Random).
     *
     * @param retailPrice car retail price in COIN
     */
    public int getResaleOffer(int retailPrice) {
        float ratio = RESALE_RATIO_MIN + random.nextFloat() * (RESALE_RATIO_MAX - RESALE_RATIO_MIN);
        return Math.max(1, Math.round(retailPrice * ratio));
    }

    /**
     * Returns the number of finance payments remaining (0 if no active finance).
     */
    public int getFinancePaymentsRemaining() {
        return financePaymentsRemaining;
    }

    /**
     * Returns the number of consecutive missed finance payments.
     */
    public int getMissedPayments() {
        return missedPayments;
    }

    /**
     * Returns the car currently under finance, or null if no active finance.
     */
    public Car getFinancedCar() {
        return financedCar;
    }

    /**
     * Seeds a sale rumour into the RumourNetwork (if set).
     */
    private void seedSaleRumour() {
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(null, new Rumour(RumourType.DODGY_DEAL,
                "Heard someone flogged a motor round Wheelwright's — dodgy as."));
        }
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /**
     * For testing: override whether WHEELER_DEALER achievement has been awarded.
     */
    public void setWheelerDealerAwardedForTesting(boolean awarded) {
        this.wheelerDealerAwarded = awarded;
    }

    /**
     * For testing: set the number of finance payments remaining.
     */
    public void setFinancePaymentsRemainingForTesting(int remaining) {
        this.financePaymentsRemaining = remaining;
    }

    /**
     * For testing: set the financed car directly.
     */
    public void setFinancedCarForTesting(Car car) {
        this.financedCar = car;
    }

    /**
     * For testing: set missed payment count directly.
     */
    public void setMissedPaymentsForTesting(int count) {
        this.missedPayments = count;
    }

    /**
     * For testing: get the current banter index.
     */
    public int getBanterIndex() {
        return banterIndex;
    }

    /**
     * For testing: get the banter timer value.
     */
    public float getBanterTimer() {
        return banterTimer;
    }
}

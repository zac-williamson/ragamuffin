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
 * Implements the Bert's Tyres &amp; MOT garage mechanics for LandmarkType.BERTS_GARAGE.
 *
 * <p>Features:
 * <ul>
 *   <li><b>MOT Request</b>: Press E on Bert (DODGY_MECHANIC), costs 2 COIN. Outcome driven
 *       by Bert's corruption score (0–100):
 *       <ul>
 *         <li>Low (0–39): genuine pass/fail based on RNG.</li>
 *         <li>Mid (40–69): DELIBERATE_FAIL — Bert fabricates faults; player can call his bluff
 *             with STREET_LADS Respect ≥ 40 (70% success chance).</li>
 *         <li>High (70–100): PASS_BRIBE — requires BROWN_ENVELOPE; cert flagged for DVSA
 *             invalidation on next raid; records VEHICLE_FRAUD crime.</li>
 *       </ul>
 *   </li>
 *   <li><b>Stolen Tyre Trade</b>: Search TYRE_STACK_PROP while Bert is distracted → 0–2
 *       STOLEN_TYRE; sell to Bert for 8 COIN each (max 4/day). Achievement: PART_WORN_PROFITEER.</li>
 *   <li><b>Catalytic Converter Hustle</b>: Equip CROWBAR, hold E on parked car for 8 seconds
 *       → CATALYTIC_CONVERTER; Notoriety +8 unwitnessed / +16 witnessed + WantedSystem +2;
 *       sell to Bert for 35 COIN or scrapyard for 28 COIN. Achievement: CONVERTER_KING.</li>
 *   <li><b>Inspection Pit Loot</b>: Open INSPECTION_HATCH_PROP while Bert is distracted (20s
 *       window). Bert checks pit every 45s — caught = WantedSystem +1. Achievement: PIT_STOP.</li>
 *   <li><b>Garage Phone Distraction</b>: Press E on GARAGE_PHONE_PROP once per hour → Kyle
 *       fetches Bert → BERT_DISTRACTED for 20 seconds. Achievement: PHONE_A_FRIEND.</li>
 *   <li><b>DVSA Raid</b>: Every 7 in-game days at 10:00, inspector arrives and invalidates
 *       bribe certs. Player can tip Bert off (Respect ≥ 30) for +5 Respect &amp;
 *       BERT_WARNED rumour. Achievement: TIP_OFF.</li>
 * </ul>
 *
 * <p>Issue #1291.
 */
public class MOTSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour Bert opens (08:00). */
    public static final float OPEN_HOUR = 8.0f;

    /** Hour Bert closes (17:00). */
    public static final float CLOSE_HOUR = 17.0f;

    // ── MOT Request constants ─────────────────────────────────────────────────

    /** COIN cost for an MOT request. */
    public static final int MOT_REQUEST_COST = 2;

    /** Corruption score threshold below which low-corruption outcomes apply. */
    public static final int CORRUPTION_LOW_MAX = 40;

    /** Corruption score threshold below which mid-corruption outcomes apply. */
    public static final int CORRUPTION_MID_MAX = 70;

    /** Chance (0–1) that a low-corruption MOT is a genuine pass. */
    public static final float GENUINE_PASS_CHANCE = 0.60f;

    /** Chance (0–1) that calling Bert's bluff succeeds. */
    public static final float BLUFF_SUCCESS_CHANCE = 0.70f;

    /** Minimum STREET_LADS Respect needed to call Bert's bluff. */
    public static final int BLUFF_MIN_RESPECT = 40;

    /** Minimum STREET_LADS Respect needed to tip Bert off about a DVSA raid. */
    public static final int TIP_OFF_MIN_RESPECT = 30;

    /** STREET_LADS Respect bonus for tipping Bert off. */
    public static final int TIP_OFF_RESPECT_BONUS = 5;

    /** Validity period (in-game days) for a genuine MOT certificate. */
    public static final int MOT_CERTIFICATE_VALIDITY_DAYS = 12;

    // ── Stolen tyres ──────────────────────────────────────────────────────────

    /** COIN earned per STOLEN_TYRE sold to Bert. */
    public static final int STOLEN_TYRE_BERT_PRICE = 8;

    /** COIN earned per STOLEN_TYRE sold to pawn shop. */
    public static final int STOLEN_TYRE_PAWN_PRICE = 5;

    /** Maximum STOLEN_TYRE transactions Bert will accept per in-game day. */
    public static final int STOLEN_TYRE_DAILY_LIMIT = 4;

    /** Maximum tyres player can steal from a TYRE_STACK_PROP per search. */
    public static final int TYRE_STACK_MAX_YIELD = 2;

    // ── Catalytic converter constants ─────────────────────────────────────────

    /** Seconds to hold E on a parked car to strip the catalytic converter. */
    public static final float CATALYTIC_STRIP_DURATION = 8.0f;

    /** COIN earned per CATALYTIC_CONVERTER sold to Bert. */
    public static final int CATALYTIC_CONVERTER_BERT_PRICE = 35;

    /** COIN earned per CATALYTIC_CONVERTER sold to scrapyard. */
    public static final int CATALYTIC_CONVERTER_SCRAPYARD_PRICE = 28;

    /** COIN earned per CATALYTIC_CONVERTER sold to MARCHETTI_CREW (bulk deal). */
    public static final int CATALYTIC_CONVERTER_MARCHETTI_PRICE = 40;

    /** Notoriety gained for unwitnessed catalytic converter theft. */
    public static final int NOTORIETY_CATALYTIC_UNWITNESSED = 8;

    /** Notoriety gained for witnessed catalytic converter theft. */
    public static final int NOTORIETY_CATALYTIC_WITNESSED = 16;

    /** WantedSystem stars added for witnessed catalytic theft. */
    public static final int WANTED_CATALYTIC_WITNESSED = 2;

    /** How many catalytic thefts trigger a newspaper headline. */
    public static final int CATALYTIC_THEFT_HEADLINE_THRESHOLD = 3;

    // ── Inspection pit constants ───────────────────────────────────────────────

    /** Seconds between Bert's pit-check sweeps. */
    public static final float PIT_CHECK_INTERVAL = 45.0f;

    /** WantedSystem stars added if caught in the inspection pit. */
    public static final int WANTED_CAUGHT_IN_PIT = 1;

    // ── Garage phone / distraction constants ──────────────────────────────────

    /** Duration (seconds) of BERT_DISTRACTED state after phone call. */
    public static final float BERT_DISTRACTED_DURATION = 20.0f;

    /** Cooldown (seconds) between phone distraction uses (1 in-game hour = 3600s). */
    public static final float PHONE_COOLDOWN_DURATION = 3600.0f;

    // ── DVSA Raid constants ────────────────────────────────────────────────────

    /** In-game day interval between DVSA raids. */
    public static final int DVSA_RAID_INTERVAL_DAYS = 7;

    /** Hour at which the DVSA inspector arrives. */
    public static final float DVSA_RAID_HOUR = 10.0f;

    /** Notoriety added when DVSA invalidates a PASS_BRIBE certificate. */
    public static final int NOTORIETY_CERT_INVALIDATED = 12;

    /** WantedSystem stars added when DVSA invalidates a PASS_BRIBE certificate. */
    public static final int WANTED_CERT_INVALIDATED = 2;

    // ── Outcomes / results ────────────────────────────────────────────────────

    /**
     * Possible outcomes of an MOT request.
     */
    public enum MOTOutcome {
        /** Valid MOT certificate issued (genuine pass). */
        GENUINE_PASS,
        /** MOT refused — genuine faults found (low corruption). */
        GENUINE_FAIL,
        /** Bert fabricated faults; player can call bluff if STREET_LADS Respect ≥ 40. */
        DELIBERATE_FAIL,
        /** Bribe accepted — cert issued but flagged for DVSA invalidation. */
        PASS_BRIBE,
        /** Player cannot afford the MOT fee. */
        INSUFFICIENT_FUNDS,
        /** Bert is not present (outside opening hours or garage closed). */
        BERT_NOT_PRESENT,
        /** Player has no BROWN_ENVELOPE for a high-corruption scenario. */
        NO_BROWN_ENVELOPE,
    }

    /**
     * Result of a bluff attempt (calling out Bert's deliberate fail).
     */
    public enum BluffResult {
        /** Bluff succeeded — Bert issues a genuine cert. */
        SUCCESS,
        /** Bluff failed — Bert keeps the fail sheet and STREET_LADS Respect −5. */
        FAILURE,
        /** Player lacks the required STREET_LADS Respect. */
        INSUFFICIENT_RESPECT,
        /** No active deliberate fail to bluff. */
        NO_ACTIVE_FAIL,
    }

    /**
     * Result of attempting to sell a STOLEN_TYRE to Bert.
     */
    public enum TyreSaleResult {
        SUCCESS,
        DAILY_LIMIT_REACHED,
        NO_TYRES_IN_INVENTORY,
        BERT_NOT_PRESENT,
    }

    /**
     * Result of looting the TYRE_STACK_PROP while Bert is distracted.
     */
    public enum TyreSearchResult {
        /** Found 1–2 STOLEN_TYRE items. */
        FOUND,
        /** Found nothing this time. */
        EMPTY,
        /** Bert is not distracted — too risky. */
        BERT_WATCHING,
    }

    /**
     * Result of opening the inspection pit hatch.
     */
    public enum PitLootResult {
        SUCCESS,
        BERT_WATCHING,
        ALREADY_LOOTED,
    }

    /**
     * Result of selling a CATALYTIC_CONVERTER to Bert.
     */
    public enum ConverterSaleResult {
        SUCCESS,
        NO_CONVERTER,
        BERT_NOT_PRESENT,
    }

    /**
     * Result of activating the garage phone distraction.
     */
    public enum PhoneResult {
        SUCCESS,
        ON_COOLDOWN,
        BERT_ALREADY_DISTRACTED,
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Bert's corruption score (0–100). Higher = more likely to offer bribes/deliberate fails. */
    private final int bertCorruptionScore;

    /** Number of stolen tyre transactions completed today. */
    private int stolenTyreDayCount;

    /** In-game day index when stolenTyreDayCount was last reset. */
    private int stolenTyreDayIndex;

    /** Seconds remaining on the BERT_DISTRACTED state (0 = not distracted). */
    private float bertDistractedTimer;

    /** Seconds until the phone distraction can be used again. */
    private float phoneCooldownTimer;

    /** Seconds until Bert's next pit check sweep. */
    private float pitCheckTimer;

    /** In-game day index of the last DVSA raid. */
    private int lastDVSARaidDay;

    /** Whether the DVSA raid for the current cycle has been triggered this hour. */
    private boolean raidTriggeredThisCycle;

    /** How many catalytic converters the player has stripped this session. */
    private int catalyticConvertersStolen;

    /** Whether the inspection pit has been looted since Bert last checked. */
    private boolean pitLooted;

    /** Whether the current pending MOT outcome is a DELIBERATE_FAIL (awaiting bluff). */
    private boolean pendingDeliberateFail;

    /** Whether a PASS_BRIBE cert is currently active (can be invalidated by DVSA). */
    private boolean bribeCertActive;

    // ── Injected dependencies (all optional — null-checked before use) ─────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private CriminalRecord criminalRecord;
    private NewspaperSystem newspaperSystem;
    private AchievementSystem achievementSystem;

    // ── Constructors ──────────────────────────────────────────────────────────

    public MOTSystem() {
        this(new Random());
    }

    public MOTSystem(Random rng) {
        this.rng = rng;
        // Seed corruption score: tends toward the middle (30–80) with noise
        this.bertCorruptionScore = 20 + rng.nextInt(71); // 20–90 range
        this.pitCheckTimer = PIT_CHECK_INTERVAL;
        this.lastDVSARaidDay = -DVSA_RAID_INTERVAL_DAYS; // allow raid from day 0
    }

    // ── Dependency injection setters ──────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update timers and trigger periodic events. Call once per frame.
     *
     * @param delta      seconds since last frame
     * @param time       current time system (for opening hours and raid scheduling)
     * @param bert       the DODGY_MECHANIC NPC, or null if not spawned
     */
    public void update(float delta, TimeSystem time, NPC bert) {
        // Tick distraction timer
        if (bertDistractedTimer > 0f) {
            bertDistractedTimer = Math.max(0f, bertDistractedTimer - delta);
        }

        // Tick phone cooldown
        if (phoneCooldownTimer > 0f) {
            phoneCooldownTimer = Math.max(0f, phoneCooldownTimer - delta);
        }

        // Tick pit check
        if (pitCheckTimer > 0f) {
            pitCheckTimer -= delta;
        } else {
            pitCheckTimer = PIT_CHECK_INTERVAL;
            pitLooted = false; // Bert checked — pit is cleared
        }

        // Daily stolen tyre count reset
        int currentDay = time.getDayCount();
        if (currentDay != stolenTyreDayIndex) {
            stolenTyreDayIndex = currentDay;
            stolenTyreDayCount = 0;
        }

        // DVSA raid scheduling — every 7 days at 10:00
        float currentHour = time.getTime();
        if ((currentDay - lastDVSARaidDay) >= DVSA_RAID_INTERVAL_DAYS
                && currentHour >= DVSA_RAID_HOUR
                && currentHour < DVSA_RAID_HOUR + 1.0f
                && !raidTriggeredThisCycle) {
            raidTriggeredThisCycle = true;
            lastDVSARaidDay = currentDay;
            triggerDVSARaidInternal(bert);
        }

        // Reset raid trigger flag when we leave the raid hour window
        if (currentHour < DVSA_RAID_HOUR || currentHour >= DVSA_RAID_HOUR + 1.0f) {
            raidTriggeredThisCycle = false;
        }
    }

    // ── MOT Request ───────────────────────────────────────────────────────────

    /**
     * The player presses E on Bert to request an MOT.
     *
     * <p>Costs {@value #MOT_REQUEST_COST} COIN. Outcome depends on Bert's corruption score.
     * On PASS_BRIBE, deducts a BROWN_ENVELOPE from inventory and records VEHICLE_FRAUD.
     *
     * @param inventory  player inventory
     * @param time       current time (for opening-hours check)
     * @return the MOT outcome
     */
    public MOTOutcome requestMOT(Inventory inventory, TimeSystem time) {
        float hour = time.getTime();
        if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
            return MOTOutcome.BERT_NOT_PRESENT;
        }
        if (inventory.getItemCount(Material.COIN) < MOT_REQUEST_COST) {
            return MOTOutcome.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, MOT_REQUEST_COST);

        if (bertCorruptionScore < CORRUPTION_LOW_MAX) {
            // Genuine outcome
            if (rng.nextFloat() < GENUINE_PASS_CHANCE) {
                inventory.addItem(Material.MOT_CERTIFICATE, 1);
                return MOTOutcome.GENUINE_PASS;
            } else {
                inventory.addItem(Material.FAIL_SHEET, 1);
                return MOTOutcome.GENUINE_FAIL;
            }
        } else if (bertCorruptionScore < CORRUPTION_MID_MAX) {
            // Bert fabricates faults
            inventory.addItem(Material.FAIL_SHEET, 1);
            pendingDeliberateFail = true;
            if (rumourNetwork != null) {
                // Seed MOT_SCAM rumour anonymously (no specific NPC, use null-safe call)
            }
            return MOTOutcome.DELIBERATE_FAIL;
        } else {
            // High corruption — bribe path
            if (inventory.getItemCount(Material.BROWN_ENVELOPE) < 1) {
                // No bribe material — fall back to deliberate fail
                inventory.addItem(Material.FAIL_SHEET, 1);
                pendingDeliberateFail = true;
                return MOTOutcome.DELIBERATE_FAIL;
            }
            inventory.removeItem(Material.BROWN_ENVELOPE, 1);
            inventory.addItem(Material.MOT_CERTIFICATE, 1);
            inventory.addItem(Material.INSPECTION_STICKER, 1);
            bribeCertActive = true;

            // Record crime
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.VEHICLE_FRAUD);
            }

            // Seed UNROADWORTHY rumour (no NPC carrier — system-level seeding, skip)
            // Achievement
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.BROWN_ENVELOPE_TEST);
            }

            return MOTOutcome.PASS_BRIBE;
        }
    }

    // ── Bluff mechanic ────────────────────────────────────────────────────────

    /**
     * The player calls out Bert's deliberate fail.
     *
     * <p>Requires a pending DELIBERATE_FAIL and STREET_LADS Respect ≥ {@value #BLUFF_MIN_RESPECT}.
     * Success chance is {@value #BLUFF_SUCCESS_CHANCE}. On success, FAIL_SHEET is replaced with
     * MOT_CERTIFICATE. On failure, STREET_LADS Respect is reduced by 5.
     *
     * @param inventory  player inventory
     * @return the bluff result
     */
    public BluffResult callBluff(Inventory inventory) {
        if (!pendingDeliberateFail) {
            return BluffResult.NO_ACTIVE_FAIL;
        }

        int streetLadsRespect = factionSystem != null
                ? factionSystem.getRespect(Faction.STREET_LADS)
                : 0;
        if (streetLadsRespect < BLUFF_MIN_RESPECT) {
            return BluffResult.INSUFFICIENT_RESPECT;
        }

        pendingDeliberateFail = false;

        if (rng.nextFloat() < BLUFF_SUCCESS_CHANCE) {
            // Remove fail sheet, issue genuine cert
            inventory.removeItem(Material.FAIL_SHEET, 1);
            inventory.addItem(Material.MOT_CERTIFICATE, 1);
            return BluffResult.SUCCESS;
        } else {
            // Bert keeps the fail; player loses Respect
            if (factionSystem != null) {
                factionSystem.applyRespectDelta(Faction.STREET_LADS, -5);
            }
            return BluffResult.FAILURE;
        }
    }

    // ── Stolen tyre mechanics ─────────────────────────────────────────────────

    /**
     * The player searches the TYRE_STACK_PROP while Bert is distracted.
     *
     * <p>Yields 0–{@value #TYRE_STACK_MAX_YIELD} STOLEN_TYRE items.
     *
     * @param inventory  player inventory
     * @return search result
     */
    public TyreSearchResult searchTyreStack(Inventory inventory) {
        if (!isBertDistracted()) {
            return TyreSearchResult.BERT_WATCHING;
        }
        int found = rng.nextInt(TYRE_STACK_MAX_YIELD + 1); // 0–2
        if (found == 0) {
            return TyreSearchResult.EMPTY;
        }
        inventory.addItem(Material.STOLEN_TYRE, found);
        return TyreSearchResult.FOUND;
    }

    /**
     * The player sells STOLEN_TYRE(s) to Bert.
     *
     * <p>Pays {@value #STOLEN_TYRE_BERT_PRICE} COIN each; max
     * {@value #STOLEN_TYRE_DAILY_LIMIT} per day.
     *
     * @param inventory   player inventory
     * @param count       number of tyres to sell
     * @param time        current time (for opening-hours check)
     * @param bertNpc     the DODGY_MECHANIC NPC (used for rumour seeding), may be null
     * @return sale result
     */
    public TyreSaleResult sellStolenTyres(Inventory inventory, int count,
                                           TimeSystem time, NPC bertNpc) {
        float hour = time.getTime();
        if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
            return TyreSaleResult.BERT_NOT_PRESENT;
        }
        if (inventory.getItemCount(Material.STOLEN_TYRE) < count) {
            return TyreSaleResult.NO_TYRES_IN_INVENTORY;
        }
        if (stolenTyreDayCount >= STOLEN_TYRE_DAILY_LIMIT) {
            return TyreSaleResult.DAILY_LIMIT_REACHED;
        }

        int canSell = Math.min(count, STOLEN_TYRE_DAILY_LIMIT - stolenTyreDayCount);
        inventory.removeItem(Material.STOLEN_TYRE, canSell);
        inventory.addItem(Material.COIN, canSell * STOLEN_TYRE_BERT_PRICE);
        stolenTyreDayCount += canSell;

        // Seed rumour on first sale
        if (stolenTyreDayCount == canSell && rumourNetwork != null && bertNpc != null) {
            rumourNetwork.addRumour(bertNpc,
                    new Rumour(RumourType.STOLEN_GOODS_TRADE,
                            "Bert's been buying part-worn tyres off the back of a lorry again."));
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PART_WORN_PROFITEER);
        }

        return TyreSaleResult.SUCCESS;
    }

    // ── Catalytic converter hustle ─────────────────────────────────────────────

    /**
     * Called when the player successfully strips a catalytic converter from a parked car.
     *
     * <p>Awards the CATALYTIC_CONVERTER item, applies Notoriety and Wanted consequences,
     * records CATALYTIC_THEFT crime, and (after 3+ thefts) triggers newspaper headline and
     * CATALYTIC_THEFT_SPREE rumour.
     *
     * @param inventory   player inventory
     * @param witnessed   true if an NPC witnessed the theft
     * @param playerX     player position X (for WantedSystem LKP)
     * @param playerY     player position Y
     * @param playerZ     player position Z
     * @param bertNpc     DODGY_MECHANIC NPC for rumour carrier, may be null
     */
    public void onCatalyticConverterStripped(Inventory inventory, boolean witnessed,
                                              float playerX, float playerY, float playerZ,
                                              NPC bertNpc) {
        inventory.addItem(Material.CATALYTIC_CONVERTER, 1);
        catalyticConvertersStolen++;

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.CATALYTIC_THEFT);
        }

        // Notoriety
        final NotorietySystem.AchievementCallback notorietyCallback =
                achievementSystem != null ? achievementSystem::unlock : null;

        if (notorietySystem != null) {
            int notorietyGain = witnessed
                    ? NOTORIETY_CATALYTIC_WITNESSED
                    : NOTORIETY_CATALYTIC_UNWITNESSED;
            notorietySystem.addNotoriety(notorietyGain, notorietyCallback);
        }

        // Wanted stars for witnessed theft
        if (witnessed && wantedSystem != null) {
            final WantedSystem.AchievementCallback wantedCallback =
                    achievementSystem != null ? achievementSystem::unlock : null;
            wantedSystem.addWantedStars(WANTED_CATALYTIC_WITNESSED,
                    playerX, playerY, playerZ, wantedCallback);
        }

        // Headline + rumour after threshold
        if (catalyticConvertersStolen >= CATALYTIC_THEFT_HEADLINE_THRESHOLD) {
            if (newspaperSystem != null) {
                newspaperSystem.recordEvent(new NewspaperSystem.InfamyEvent(
                        "CATALYTIC_THEFT",
                        "Bert's Tyres & MOT",
                        Material.CATALYTIC_CONVERTER,
                        null,
                        wantedSystem != null ? wantedSystem.getWantedStars() : 0,
                        "LEG_IT",
                        null,
                        4
                ));
            }
            if (rumourNetwork != null && bertNpc != null
                    && catalyticConvertersStolen == CATALYTIC_THEFT_HEADLINE_THRESHOLD) {
                rumourNetwork.addRumour(bertNpc,
                        new Rumour(RumourType.CATALYTIC_THEFT_SPREE,
                                "Someone's been doing catalytic converters round here — "
                                + "three cars in a week."));
            }
        }

        // Achievement (progress-based, target = 3)
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.CONVERTER_KING);
        }
    }

    /**
     * The player sells a CATALYTIC_CONVERTER to Bert.
     *
     * @param inventory  player inventory
     * @param time       current time (for opening-hours check)
     * @return sale result
     */
    public ConverterSaleResult sellCatalyticConverter(Inventory inventory, TimeSystem time) {
        float hour = time.getTime();
        if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
            return ConverterSaleResult.BERT_NOT_PRESENT;
        }
        if (inventory.getItemCount(Material.CATALYTIC_CONVERTER) < 1) {
            return ConverterSaleResult.NO_CONVERTER;
        }
        inventory.removeItem(Material.CATALYTIC_CONVERTER, 1);
        inventory.addItem(Material.COIN, CATALYTIC_CONVERTER_BERT_PRICE);
        return ConverterSaleResult.SUCCESS;
    }

    // ── Inspection pit loot ────────────────────────────────────────────────────

    /**
     * The player opens the inspection pit hatch while Bert is distracted.
     *
     * <p>Yields a random assortment of pit loot (CAR_BATTERY, CATALYTIC_CONVERTER,
     * SCRAP_METAL, MOTOR_OIL).
     *
     * @param inventory  player inventory
     * @return loot result
     */
    public PitLootResult lootInspectionPit(Inventory inventory) {
        if (!isBertDistracted()) {
            return PitLootResult.BERT_WATCHING;
        }
        if (pitLooted) {
            return PitLootResult.ALREADY_LOOTED;
        }

        pitLooted = true;

        // Random loot selection from pit contents
        Material[] pitLoot = {
            Material.CAR_BATTERY,
            Material.CATALYTIC_CONVERTER,
            Material.SCRAP_METAL,
            Material.MOTOR_OIL
        };
        // Award 1–3 random items
        int lootCount = 1 + rng.nextInt(3);
        for (int i = 0; i < lootCount; i++) {
            inventory.addItem(pitLoot[rng.nextInt(pitLoot.length)], 1);
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PIT_STOP);
        }

        return PitLootResult.SUCCESS;
    }

    /**
     * Called by the game system when Bert catches the player in the inspection pit.
     *
     * <p>Adds {@value #WANTED_CAUGHT_IN_PIT} Wanted star.
     *
     * @param playerX     player X position
     * @param playerY     player Y position
     * @param playerZ     player Z position
     */
    public void onCaughtInPit(float playerX, float playerY, float playerZ) {
        if (wantedSystem != null) {
            final WantedSystem.AchievementCallback wantedCallback =
                    achievementSystem != null ? achievementSystem::unlock : null;
            wantedSystem.addWantedStars(WANTED_CAUGHT_IN_PIT,
                    playerX, playerY, playerZ, wantedCallback);
        }
    }

    // ── Garage phone distraction ───────────────────────────────────────────────

    /**
     * The player presses E on the GARAGE_PHONE_PROP to distract Bert via Kyle.
     *
     * <p>Sets Bert to BERT_DISTRACTED for {@value #BERT_DISTRACTED_DURATION} seconds.
     * Can only be used once per {@value #PHONE_COOLDOWN_DURATION} seconds.
     *
     * @return phone result
     */
    public PhoneResult triggerGaragePhone() {
        if (phoneCooldownTimer > 0f) {
            return PhoneResult.ON_COOLDOWN;
        }
        if (isBertDistracted()) {
            return PhoneResult.BERT_ALREADY_DISTRACTED;
        }
        bertDistractedTimer = BERT_DISTRACTED_DURATION;
        phoneCooldownTimer = PHONE_COOLDOWN_DURATION;

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PHONE_A_FRIEND);
        }

        return PhoneResult.SUCCESS;
    }

    // ── DVSA Raid ─────────────────────────────────────────────────────────────

    /**
     * The player tips Bert off about an incoming DVSA raid.
     *
     * <p>Requires STREET_LADS Respect ≥ {@value #TIP_OFF_MIN_RESPECT}.
     * Cancels bribeCertActive flag, grants +{@value #TIP_OFF_RESPECT_BONUS} Respect,
     * seeds BERT_WARNED rumour, unlocks TIP_OFF achievement.
     *
     * @param bertNpc  the DODGY_MECHANIC NPC for rumour seeding, may be null
     * @return true if tip-off was accepted
     */
    public boolean tipOffBert(NPC bertNpc) {
        int streetLadsRespect = factionSystem != null
                ? factionSystem.getRespect(Faction.STREET_LADS)
                : 0;
        if (streetLadsRespect < TIP_OFF_MIN_RESPECT) {
            return false;
        }

        // Bert hides the certs — raid won't invalidate them
        bribeCertActive = false;

        // Reward
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, TIP_OFF_RESPECT_BONUS);
        }

        // Seed BERT_WARNED rumour
        if (rumourNetwork != null && bertNpc != null) {
            rumourNetwork.addRumour(bertNpc,
                    new Rumour(RumourType.BERT_WARNED,
                            "Someone tipped Bert off about the DVSA — "
                            + "he had an hour to hide the dodgy certs."));
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.TIP_OFF);
        }

        return true;
    }

    /**
     * Manually trigger a DVSA raid (also called internally by the update loop).
     *
     * <p>If a PASS_BRIBE cert is active, invalidates it: records VEHICLE_FRAUD,
     * adds Notoriety and Wanted stars.
     *
     * @param bertNpc  the DODGY_MECHANIC NPC for rumour seeding, may be null
     */
    public void triggerDVSARaid(NPC bertNpc) {
        lastDVSARaidDay += DVSA_RAID_INTERVAL_DAYS; // prevent double-trigger
        triggerDVSARaidInternal(bertNpc);
    }

    private void triggerDVSARaidInternal(NPC bertNpc) {
        if (!bribeCertActive) {
            // Nothing to invalidate
            return;
        }

        bribeCertActive = false;

        // Record additional crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.VEHICLE_FRAUD);
        }

        final NotorietySystem.AchievementCallback notorietyCallback =
                achievementSystem != null ? achievementSystem::unlock : null;
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_CERT_INVALIDATED, notorietyCallback);
        }

        final WantedSystem.AchievementCallback wantedCallback =
                achievementSystem != null ? achievementSystem::unlock : null;
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(WANTED_CERT_INVALIDATED, 0f, 0f, 0f, wantedCallback);
        }

        // Seed DVSA_RAID rumour
        if (rumourNetwork != null && bertNpc != null) {
            rumourNetwork.addRumour(bertNpc,
                    new Rumour(RumourType.DVSA_RAID,
                            "DVSA were round at Bert's — found certs for cars that shouldn't have passed."));
        }
    }

    // ── State queries ─────────────────────────────────────────────────────────

    /** @return true if Bert is currently distracted (phone call active). */
    public boolean isBertDistracted() {
        return bertDistractedTimer > 0f;
    }

    /** @return remaining seconds of distraction, or 0 if not distracted. */
    public float getBertDistractedTimer() {
        return bertDistractedTimer;
    }

    /** @return remaining seconds until the garage phone can be used again. */
    public float getPhoneCooldownTimer() {
        return phoneCooldownTimer;
    }

    /** @return whether a PASS_BRIBE certificate is currently active. */
    public boolean isBribeCertActive() {
        return bribeCertActive;
    }

    /** @return Bert's corruption score (0–100). */
    public int getBertCorruptionScore() {
        return bertCorruptionScore;
    }

    /** @return number of stolen tyre transactions completed today. */
    public int getStolenTyreDayCount() {
        return stolenTyreDayCount;
    }

    /** @return total catalytic converters stripped this session. */
    public int getCatalyticConvertersStolen() {
        return catalyticConvertersStolen;
    }

    /** @return true if there is a pending deliberate fail awaiting a bluff call. */
    public boolean isPendingDeliberateFail() {
        return pendingDeliberateFail;
    }

    /** @return in-game day index of the last DVSA raid. */
    public int getLastDVSARaidDay() {
        return lastDVSARaidDay;
    }

    /**
     * Force-set Bert's distracted timer (for testing).
     *
     * @param seconds seconds of distraction remaining
     */
    public void setBertDistractedTimerForTesting(float seconds) {
        this.bertDistractedTimer = seconds;
    }

    /**
     * Force-set the phone cooldown timer (for testing).
     *
     * @param seconds seconds remaining on cooldown
     */
    public void setPhoneCooldownTimerForTesting(float seconds) {
        this.phoneCooldownTimer = seconds;
    }

    /**
     * Force-set the bribe cert active flag (for testing).
     *
     * @param active true if a PASS_BRIBE cert is active
     */
    public void setBribeCertActiveForTesting(boolean active) {
        this.bribeCertActive = active;
    }

    /**
     * Force-set the catalytic converters stolen count (for testing).
     *
     * @param count number of converters
     */
    public void setCatalyticConvertersStolenForTesting(int count) {
        this.catalyticConvertersStolen = count;
    }

    /**
     * Force-set the stolen tyre day count (for testing).
     *
     * @param count number of tyres sold today
     */
    public void setStolenTyreDayCountForTesting(int count) {
        this.stolenTyreDayCount = count;
    }

    /**
     * Force-set the pending deliberate fail flag (for testing).
     *
     * @param pending true if there is an active deliberate fail
     */
    public void setPendingDeliberateFailForTesting(boolean pending) {
        this.pendingDeliberateFail = pending;
    }
}

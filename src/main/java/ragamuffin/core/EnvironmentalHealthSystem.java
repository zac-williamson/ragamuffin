package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Issue #1196: Northfield Environmental Health Officer — Surprise Inspections,
 * Hygiene Ratings &amp; the Sticker Forgery Racket.
 *
 * <p>Janet, the Council Environmental Health Officer, makes one surprise visit per
 * in-game day (Mon–Fri 09:30–15:30) to a rotating food venue. Each venue tracks a
 * {@code condition} score (0–100) that maps to a 1–5 star hygiene rating.
 *
 * <h3>Integrated systems:</h3>
 * <ul>
 *   <li>{@link SkipDivingSystem} — {@code hasRatsAtVenue()} penalty</li>
 *   <li>{@link NotorietySystem} — failed bribe +3 notoriety; sabotage +5</li>
 *   <li>{@link WantedSystem} — failed bribe +1 star; assault +3 stars; forgery +2 stars</li>
 *   <li>{@link CriminalRecord} — BRIBERY, CRIMINAL_DAMAGE, FRAUD, ASSAULT_ON_OFFICIAL</li>
 *   <li>{@link RumourNetwork} — FOOD_HYGIENE and COUNCIL_ENFORCEMENT rumours</li>
 *   <li>{@link NewspaperSystem} — closure headlines</li>
 *   <li>{@link NeighbourhoodSystem} — closure −3 vibes; re-open +2 vibes; clean kitchen +1 vibes</li>
 *   <li>{@link FactionSystem} — Marchetti Crew −5 respect on venue closure</li>
 *   <li>{@link StreetSkillSystem} — INFLUENCE bribe modifier; GRAFTING for kitchen cleaning</li>
 *   <li>{@link AchievementSystem} — GREASY_PALM, FIVE_STAR_FRAUDSTER, CLEAN_KITCHEN, PUBLIC_HEALTH_HERO</li>
 * </ul>
 */
public class EnvironmentalHealthSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Starting condition for each venue (maps to 4-star rating). */
    public static final int INITIAL_CONDITION = 65;

    /** Maximum condition score. */
    public static final int MAX_CONDITION = 100;

    /** Passive weekly condition degradation. */
    public static final int WEEKLY_DEGRADATION = 5;

    /** Condition improvement from kitchen-cleaning interaction. */
    public static final int KITCHEN_CLEAN_BOOST = 10;

    /** Condition penalty from crowbar sabotage. */
    public static final int SABOTAGE_PENALTY = 25;

    /** Condition variance applied during inspection (±). */
    public static final int INSPECTION_VARIANCE = 10;

    /** Rat infestation condition penalty during inspection. */
    public static final int RAT_PENALTY = 15;

    /** Condition floor when bribe succeeds (minimum 3-star outcome). */
    public static final int BRIBE_SUCCESS_CONDITION_FLOOR = 80;

    /** Condition forced on spite inspection after failed bribe. */
    public static final int BRIBE_FAILURE_SPITE_CONDITION = 10;

    /** Coin cost for bribe attempt. */
    public static final int BRIBE_COST = 5;

    /** Base bribe success probability (40%). */
    public static final float BRIBE_BASE_SUCCESS = 0.40f;

    /** Bribe success bonus per INFLUENCE tier level. */
    public static final float BRIBE_INFLUENCE_BONUS = 0.10f;

    /** Bribe success penalty at Notoriety ≥ 30. */
    public static final float BRIBE_NOTORIETY_30_PENALTY = 0.20f;

    /** Bribe success penalty at Notoriety ≥ 50. */
    public static final float BRIBE_NOTORIETY_50_PENALTY = 0.30f;

    /** Notoriety threshold for first bribe penalty. */
    public static final int BRIBE_NOTORIETY_THRESHOLD_LOW = 30;

    /** Notoriety threshold for second bribe penalty. */
    public static final int BRIBE_NOTORIETY_THRESHOLD_HIGH = 50;

    /** Notoriety gain on failed bribe. */
    public static final int BRIBE_FAIL_NOTORIETY_GAIN = 3;

    /** Notoriety gain on kitchen sabotage. */
    public static final int SABOTAGE_NOTORIETY_GAIN = 5;

    /** Wanted stars added on failed bribe. */
    public static final int BRIBE_FAIL_WANTED_STARS = 1;

    /** Wanted stars added on Janet assault. */
    public static final int ASSAULT_WANTED_STARS = 3;

    /** Wanted stars added on forgery detection. */
    public static final int FORGERY_WANTED_STARS = 2;

    /** Notoriety gain on failed bribe. */
    public static final int BRIBE_FAIL_NOTORIETY = 3;

    /** Neighbourhood vibes delta on venue closure. */
    public static final int CLOSURE_VIBES_DELTA = -3;

    /** Neighbourhood vibes delta when venue re-opens after notice. */
    public static final int REOPEN_VIBES_DELTA = 2;

    /** Neighbourhood vibes delta on kitchen cleaning. */
    public static final int KITCHEN_CLEAN_VIBES_DELTA = 1;

    /** Faction respect delta for Marchetti on venue closure in their turf. */
    public static final int MARCHETTI_CLOSURE_RESPECT_DELTA = -5;

    /** Sale price for a FORGED_FIVE_STAR_STICKER. */
    public static final int FORGED_STICKER_PRICE = 8;

    /** Minimum bribe success notoriety count for bribe tracking. */
    public static final int FIRST_BRIBE_ATTEMPT = 1;

    /** Janet's working day start hour. */
    public static final float JANET_START_HOUR = 9.0f;

    /** Janet's working day end hour. */
    public static final float JANET_END_HOUR = 17.0f;

    /** Earliest inspection hour. */
    public static final float INSPECTION_EARLIEST_HOUR = 9.5f;

    /** Latest inspection hour. */
    public static final float INSPECTION_LATEST_HOUR = 15.5f;

    // ── Food venues inspected ─────────────────────────────────────────────────

    /** Ordered list of food venues Janet may inspect (cycling by last-inspection age). */
    public static final LandmarkType[] FOOD_VENUES = {
        LandmarkType.GREASY_SPOON_CAFE,
        LandmarkType.CHIPPY,
        LandmarkType.KEBAB_VAN,
        LandmarkType.CHINESE_TAKEAWAY,
        LandmarkType.NANDOS,
        LandmarkType.BISTA_VILLAGE
    };

    // ── Venue state ───────────────────────────────────────────────────────────

    /** Condition score per venue (0–100). */
    private final Map<LandmarkType, Integer> venueCondition = new EnumMap<>(LandmarkType.class);

    /** Whether a forged sticker has been placed at each venue. */
    private final Map<LandmarkType, Boolean> forgedStickerPlaced = new EnumMap<>(LandmarkType.class);

    /** Day of last inspection per venue (for rotation weighting). */
    private final Map<LandmarkType, Integer> lastInspectionDay = new EnumMap<>(LandmarkType.class);

    /** Whether a CLOSURE_NOTICE_PROP is currently placed at each venue. */
    private final Map<LandmarkType, Boolean> closureNoticePlaced = new EnumMap<>(LandmarkType.class);

    /** Whether an IMPROVEMENT_NOTICE_PROP is currently placed at each venue. */
    private final Map<LandmarkType, Boolean> improvementNoticePlaced = new EnumMap<>(LandmarkType.class);

    // ── Inspection state ──────────────────────────────────────────────────────

    /** Result of last runInspection call (1–5 stars). */
    private int lastRating = 0;

    /** Whether Janet accepted a bribe in the current inspection cycle. */
    private boolean bribed = false;

    /** Whether forgery was detected in the last inspection. */
    private boolean forgeryDetected = false;

    /** How many bribe attempts the player has made in this session. */
    private int bribesAttempted = 0;

    /**
     * Venue targeted by the next tip-off inspection (set via SUGGESTION_BOX_PROP).
     * Null if no tip-off is pending.
     */
    private LandmarkType tipOffTarget = null;

    /** Day the last tip-off was filed (to enforce one-per-week limit). */
    private int lastTipOffDay = -7;

    // ── Injected systems ──────────────────────────────────────────────────────

    private SkipDivingSystem skipDivingSystem;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private NewspaperSystem newspaperSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private FactionSystem factionSystem;
    private StreetSkillSystem streetSkillSystem;
    private AchievementSystem achievementSystem;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EnvironmentalHealthSystem() {
        for (LandmarkType venue : FOOD_VENUES) {
            venueCondition.put(venue, INITIAL_CONDITION);
            forgedStickerPlaced.put(venue, false);
            lastInspectionDay.put(venue, -1);
            closureNoticePlaced.put(venue, false);
            improvementNoticePlaced.put(venue, false);
        }
    }

    // ── Dependency injection ─────────────────────────────────────────────────

    public void setSkipDivingSystem(SkipDivingSystem skipDivingSystem) {
        this.skipDivingSystem = skipDivingSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setStreetSkillSystem(StreetSkillSystem streetSkillSystem) {
        this.streetSkillSystem = streetSkillSystem;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    // ── Venue condition management ────────────────────────────────────────────

    /**
     * Returns the current condition score (0–100) for the given venue.
     */
    public int getCondition(LandmarkType venue) {
        return venueCondition.getOrDefault(venue, INITIAL_CONDITION);
    }

    /**
     * Sets the condition score for the given venue, clamped to [0, MAX_CONDITION].
     */
    public void setCondition(LandmarkType venue, int condition) {
        venueCondition.put(venue, Math.max(0, Math.min(MAX_CONDITION, condition)));
    }

    /**
     * Applies the passive weekly degradation to all food venues.
     * Call once per in-game week from the game's time system.
     */
    public void applyWeeklyDegradation() {
        for (LandmarkType venue : FOOD_VENUES) {
            int current = getCondition(venue);
            setCondition(venue, current - WEEKLY_DEGRADATION);
        }
    }

    /**
     * Raises the venue condition by {@link #KITCHEN_CLEAN_BOOST}, capped at
     * {@link #MAX_CONDITION}. Requires GRAFTING ≥ Apprentice.
     *
     * @param venue    the venue being cleaned
     * @param oldRating the rating before cleaning (to detect CLEAN_KITCHEN achievement)
     * @return the new condition after cleaning
     */
    public int cleanKitchen(LandmarkType venue, int oldRating) {
        int current = getCondition(venue);
        int newCondition = Math.min(MAX_CONDITION, current + KITCHEN_CLEAN_BOOST);
        setCondition(venue, newCondition);

        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(neighbourhoodSystem.getVibes() + KITCHEN_CLEAN_VIBES_DELTA);
        }

        // CLEAN_KITCHEN: venue raised from rating 2 to 5 in a single cycle
        int newRating = conditionToRating(newCondition);
        if (oldRating <= 2 && newRating == 5 && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CLEAN_KITCHEN);
        }

        return newCondition;
    }

    /**
     * Sabotages the venue kitchen (crowbar), reducing condition by
     * {@link #SABOTAGE_PENALTY}. Records CRIMINAL_DAMAGE. Adds notoriety +5.
     *
     * @param venue              the venue being sabotaged
     * @param achievementCallback callback for any achievements triggered
     */
    public void sabotageKitchen(LandmarkType venue, NotorietySystem.AchievementCallback achievementCallback) {
        int current = getCondition(venue);
        setCondition(venue, current - SABOTAGE_PENALTY);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SABOTAGE_NOTORIETY_GAIN, achievementCallback);
        }
    }

    // ── Rating calculation ────────────────────────────────────────────────────

    /**
     * Converts a condition score (0–100) to a hygiene star rating (1–5).
     *
     * <pre>
     * 80–100 → 5 stars
     * 60–79  → 4 stars
     * 40–59  → 3 stars
     * 20–39  → 2 stars
     *  0–19  → 1 star
     * </pre>
     */
    public static int conditionToRating(int condition) {
        if (condition >= 80) return 5;
        if (condition >= 60) return 4;
        if (condition >= 40) return 3;
        if (condition >= 20) return 2;
        return 1;
    }

    // ── Bribery ───────────────────────────────────────────────────────────────

    /**
     * Attempt to bribe Janet when the player presses E on her during an inspection.
     * Player must have ≥ 5 COIN in inventory.
     *
     * @param playerInventory    player's inventory
     * @param notoriety          player's current notoriety score
     * @param influenceTierLevel player's INFLUENCE skill tier level (0=Novice … 4=Legend)
     * @param random             seeded RNG for determinism
     * @param achievementCallback callback for achievement unlocks
     * @return BRIBE_SUCCESS, BRIBE_FAILURE, or BRIBE_INSUFFICIENT_FUNDS
     */
    public BribeResult attemptBribe(Inventory playerInventory, int notoriety,
                                    int influenceTierLevel, Random random,
                                    NotorietySystem.AchievementCallback achievementCallback) {
        if (!playerInventory.hasItem(Material.COIN, BRIBE_COST)) {
            return BribeResult.BRIBE_INSUFFICIENT_FUNDS;
        }

        bribesAttempted++;

        float successChance = BRIBE_BASE_SUCCESS + influenceTierLevel * BRIBE_INFLUENCE_BONUS;
        if (notoriety >= BRIBE_NOTORIETY_THRESHOLD_HIGH) {
            successChance -= BRIBE_NOTORIETY_50_PENALTY;
        } else if (notoriety >= BRIBE_NOTORIETY_THRESHOLD_LOW) {
            successChance -= BRIBE_NOTORIETY_30_PENALTY;
        }
        successChance = Math.max(0f, Math.min(1f, successChance));

        float roll = random.nextFloat();
        if (roll < successChance) {
            // Success
            playerInventory.removeItem(Material.COIN, BRIBE_COST);
            bribed = true;

            // GREASY_PALM: first attempt bribe success
            if (bribesAttempted == FIRST_BRIBE_ATTEMPT && achievementSystem != null) {
                achievementSystem.unlock(AchievementType.GREASY_PALM);
            }
            return BribeResult.BRIBE_SUCCESS;
        } else {
            // Failure
            bribed = false;
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.BRIBERY);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(BRIBE_FAIL_NOTORIETY, achievementCallback);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(BRIBE_FAIL_WANTED_STARS, 0f, 0f, 0f, achievementCallback);
            }
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                    new Rumour(RumourType.COUNCIL_ENFORCEMENT,
                        "Council environmental health have been round — something's going on."));
            }
            return BribeResult.BRIBE_FAILURE;
        }
    }

    /** Result of a bribe attempt. */
    public enum BribeResult {
        BRIBE_SUCCESS,
        BRIBE_FAILURE,
        BRIBE_INSUFFICIENT_FUNDS
    }

    // ── Inspection ────────────────────────────────────────────────────────────

    /**
     * Runs a food hygiene inspection for the given venue. This is the core mechanic:
     *
     * <ol>
     *   <li>Roll condition ± {@link #INSPECTION_VARIANCE}.</li>
     *   <li>Apply rat penalty ({@link #RAT_PENALTY}) if {@code SkipDivingSystem.hasRatsAtVenue()}.</li>
     *   <li>If bribe succeeded, floor effective condition at {@link #BRIBE_SUCCESS_CONDITION_FLOOR}.</li>
     *   <li>Determine star rating (1–5).</li>
     *   <li>Check for forged sticker — if placed and real condition still low, detect forgery.</li>
     *   <li>Update HYGIENE_RATING_PROP state.</li>
     *   <li>Issue notices and seed rumours as appropriate.</li>
     * </ol>
     *
     * @param venue        the landmark being inspected
     * @param playerPresent whether the player is inside the venue
     * @param random       seeded RNG (variance roll)
     * @return the star rating (1–5)
     */
    public int runInspection(LandmarkType venue, boolean playerPresent, Random random) {
        int baseCondition = getCondition(venue);

        // 1. Roll variance
        int variance = random.nextInt(INSPECTION_VARIANCE * 2 + 1) - INSPECTION_VARIANCE;
        int effectiveCondition = baseCondition + variance;

        // 2. Rat infestation penalty
        boolean ratsPresent = skipDivingSystem != null && skipDivingSystem.hasRatsAtVenue(venue);
        if (ratsPresent) {
            effectiveCondition -= RAT_PENALTY;
        }

        // 3. Bribe: floor condition
        if (bribed) {
            effectiveCondition = Math.max(effectiveCondition, BRIBE_SUCCESS_CONDITION_FLOOR);
            bribed = false; // consumed
        }

        effectiveCondition = Math.max(0, Math.min(MAX_CONDITION, effectiveCondition));

        // 4. Check forged sticker
        boolean hasForgedSticker = forgedStickerPlaced.getOrDefault(venue, false);
        if (hasForgedSticker) {
            // Forgery detected if real condition would give < 4 stars
            int realRating = conditionToRating(effectiveCondition);
            if (realRating < 4) {
                forgeryDetected = true;
                forgedStickerPlaced.put(venue, false);
                if (criminalRecord != null) {
                    criminalRecord.record(CrimeType.FRAUD);
                }
                if (wantedSystem != null) {
                    wantedSystem.addWantedStars(FORGERY_WANTED_STARS, 0f, 0f, 0f, null);
                }
                if (rumourNetwork != null) {
                    rumourNetwork.addRumour(null,
                        new Rumour(RumourType.FOOD_HYGIENE,
                            getVenueName(venue) + " busted using a forged hygiene sticker — " +
                            "Environmental Health are fuming."));
                }
                // Issue closure immediately
                closureNoticePlaced.put(venue, true);
                improvementNoticePlaced.put(venue, false);
                applyClosureEffects(venue);
                lastInspectionDay.put(venue, 0);
                lastRating = 1;
                return 1;
            } else {
                // Forgery not detected — real condition is clean enough
                forgeryDetected = false;

                // FIVE_STAR_FRAUDSTER: sticker "fooled" Janet once
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.FIVE_STAR_FRAUDSTER);
                }
                forgedStickerPlaced.put(venue, false);
            }
        } else {
            forgeryDetected = false;
        }

        // 5. Determine rating
        int rating = conditionToRating(effectiveCondition);

        // 6. Apply notices and rumours
        closureNoticePlaced.put(venue, false);
        improvementNoticePlaced.put(venue, false);

        if (rating <= 2) {
            improvementNoticePlaced.put(venue, true);
            if (rumourNetwork != null) {
                rumourNetwork.addRumour(null,
                    new Rumour(RumourType.FOOD_HYGIENE,
                        getVenueName(venue) + " only got " + rating + " star" +
                        (rating == 1 ? "" : "s") + " — I'm never eating there again."));
            }
        }

        if (rating == 1) {
            closureNoticePlaced.put(venue, true);
            applyClosureEffects(venue);
        }

        // PUBLIC_HEALTH_HERO: tip-off of rat-infested venue led to closure
        if (ratsPresent && tipOffTarget == venue && rating == 1 && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PUBLIC_HEALTH_HERO);
        }

        // Clear tip-off target after use
        if (tipOffTarget == venue) {
            tipOffTarget = null;
        }

        lastInspectionDay.put(venue, 0);
        lastRating = rating;
        return rating;
    }

    /** Applies side-effects of a venue closure (notice rating = 1). */
    private void applyClosureEffects(LandmarkType venue) {
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(neighbourhoodSystem.getVibes() + CLOSURE_VIBES_DELTA);
        }
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline(getVenueName(venue).toUpperCase() +
                " SHUT IN HYGIENE HORROR");
        }
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, MARCHETTI_CLOSURE_RESPECT_DELTA);
        }
    }

    // ── Tip-off mechanic ──────────────────────────────────────────────────────

    /**
     * Posts an ANONYMOUS_NOTE to the SUGGESTION_BOX_PROP at the Council Office to
     * force Janet's next inspection to target the given venue.
     * Enforces a one-tip-per-in-game-week limit.
     *
     * @param target         the venue to tip off about
     * @param playerInventory the player's inventory (must contain ANONYMOUS_NOTE)
     * @param currentDay     current in-game day number
     * @return true if the tip-off was accepted
     */
    public boolean postTipOff(LandmarkType target, Inventory playerInventory, int currentDay) {
        if (!playerInventory.hasItem(Material.ANONYMOUS_NOTE, 1)) {
            return false;
        }
        if (currentDay - lastTipOffDay < 7) {
            // One tip-off per in-game week
            return false;
        }
        playerInventory.removeItem(Material.ANONYMOUS_NOTE, 1);
        tipOffTarget = target;
        lastTipOffDay = currentDay;
        return true;
    }

    /**
     * Returns the venue Janet should inspect next (tip-off target if set, otherwise
     * selects by longest-time-since-last-inspection weighting).
     *
     * @param currentDay current in-game day
     * @param random     seeded RNG for random selection
     * @return the selected venue LandmarkType
     */
    public LandmarkType selectNextInspectionVenue(int currentDay, Random random) {
        if (tipOffTarget != null) {
            return tipOffTarget;
        }
        // Select the venue with the oldest last inspection (longest gap)
        LandmarkType oldest = FOOD_VENUES[0];
        int maxGap = currentDay - lastInspectionDay.getOrDefault(FOOD_VENUES[0], -1);
        for (LandmarkType venue : FOOD_VENUES) {
            int gap = currentDay - lastInspectionDay.getOrDefault(venue, -1);
            if (gap > maxGap) {
                maxGap = gap;
                oldest = venue;
            }
        }
        return oldest;
    }

    // ── Sticker forgery ───────────────────────────────────────────────────────

    /**
     * Marks a forged five-star sticker as placed at the given venue.
     * Called when the player sells a FORGED_FIVE_STAR_STICKER to a venue owner.
     * The venue's condition is NOT changed; only the displayed prop is replaced.
     *
     * @param venue the venue receiving the forged sticker
     */
    public void placeForgedSticker(LandmarkType venue) {
        forgedStickerPlaced.put(venue, true);
    }

    /** Returns whether a forged sticker is currently placed at the given venue. */
    public boolean hasForgedSticker(LandmarkType venue) {
        return forgedStickerPlaced.getOrDefault(venue, false);
    }

    // ── Assault on Janet ─────────────────────────────────────────────────────

    /**
     * Called when the player assaults Janet. Seeds COUNCIL_ENFORCEMENT rumour,
     * records ASSAULT_ON_OFFICIAL crime, adds Wanted +3.
     *
     * @param achievementCallback callback for any achievement unlocks
     */
    public void onJanetAssaulted(NotorietySystem.AchievementCallback achievementCallback) {
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.ASSAULT_ON_OFFICIAL);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(ASSAULT_WANTED_STARS, 0f, 0f, 0f, achievementCallback);
        }
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(null,
                new Rumour(RumourType.COUNCIL_ENFORCEMENT,
                    "Someone attacked the environmental health woman — absolutely shocking."));
        }
    }

    // ── State queries ─────────────────────────────────────────────────────────

    /** Returns true if the bribe was accepted in the most recent inspection cycle. */
    public boolean isBribed() {
        return bribed;
    }

    /** Returns true if forgery was detected in the most recent inspection. */
    public boolean isForgeryDetected() {
        return forgeryDetected;
    }

    /** Returns the star rating (1–5) from the most recent inspection. */
    public int getLastRating() {
        return lastRating;
    }

    /** Returns the pending tip-off target venue, or null if none. */
    public LandmarkType getTipOffTarget() {
        return tipOffTarget;
    }

    /** Returns the day the last tip-off was filed. */
    public int getLastTipOffDay() {
        return lastTipOffDay;
    }

    /** Returns whether a CLOSURE_NOTICE_PROP is placed at the given venue. */
    public boolean hasClosureNotice(LandmarkType venue) {
        return closureNoticePlaced.getOrDefault(venue, false);
    }

    /** Returns whether an IMPROVEMENT_NOTICE_PROP is placed at the given venue. */
    public boolean hasImprovementNotice(LandmarkType venue) {
        return improvementNoticePlaced.getOrDefault(venue, false);
    }

    /**
     * Forces Janet's next inspection to target the given venue (used in tests
     * to advance state without going through the tip-off UI).
     */
    public void forceNextInspectionTarget(LandmarkType venue) {
        this.tipOffTarget = venue;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the display name of a food venue. */
    public static String getVenueName(LandmarkType venue) {
        switch (venue) {
            case GREASY_SPOON_CAFE:   return "Pete's Café";
            case CHIPPY:              return "Tony's Chip Shop";
            case KEBAB_VAN:           return "Ali's Kebab Van";
            case CHINESE_TAKEAWAY:    return "Golden Palace";
            case NANDOS:              return "Nando's";
            case BISTA_VILLAGE:       return "Balti Village";
            default:                  return venue.name();
        }
    }

    /**
     * Convenience: get a venue's display name for a closure headline.
     * Delegates to {@link #getVenueName(LandmarkType)}.
     */
    public static String getClosureHeadline(LandmarkType venue) {
        return getVenueName(venue).toUpperCase() + " SHUT IN HYGIENE HORROR";
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Issue #769: Dynamic NPC Needs &amp; Black Market Economy — 'The Street Hustle'
 *
 * <p>A living street economy where every NPC has personal needs (HUNGRY, COLD, BORED,
 * BROKE, SCARED, DESPERATE) that accumulate over time and can be satisfied by the
 * player for coin.
 *
 * <h3>Key Mechanics</h3>
 * <ul>
 *   <li><b>NPC Needs</b> — per-NPC need scores (0–100) per {@link NeedType}, accumulating
 *       each frame at a rate determined by type, weather, and active market events.</li>
 *   <li><b>Floating Prices</b> — base prices for tradeable goods driven by supply/demand.
 *       {@link MarketEvent}s spike prices for specific commodities.</li>
 *   <li><b>Street Dealing</b> — player presses E near an NPC whose highest need &gt; 50
 *       to get a contextual deal prompt with haggling.</li>
 *   <li><b>Haggling</b> — player can offer below market price; NPC accepts if desperate
 *       (need &gt; 75) or rejects and walks away.</li>
 *   <li><b>Protection Rackets</b> — businesses (GREGGS, OFF_LICENCE, etc.) pay passive
 *       income each game-minute when the player runs a racket on them. Rival factions
 *       can undercut rackets if their Respect drops below the threshold.</li>
 *   <li><b>Market Events</b> — 6 disruption events that spike prices and NPC behaviour.</li>
 * </ul>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link FactionSystem} — trade loyalty: faction respect &ge; FRIENDLY_THRESHOLD
 *       gives a 20% price bonus to deals with aligned NPCs.</li>
 *   <li>{@link RumourNetwork} — trade rumours seeded by significant deals.</li>
 *   <li>{@link DisguiseSystem} — GREGGS_APRON/COUNCIL_JACKET give price bonuses in-uniform.</li>
 *   <li>{@link WitnessSystem} — criminal record entry for handling STOLEN_PHONE or
 *       COUNTERFEIT_NOTE when a POLICE NPC is nearby.</li>
 *   <li>{@link WeatherSystem} — COLD_SNAP / FROST spikes COLD need accumulation rate.</li>
 *   <li>{@link NotorietySystem} — racket penalties when notoriety attracts rival factions.</li>
 * </ul>
 */
public class StreetEconomySystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Need score threshold above which an NPC displays a deal prompt. */
    public static final float DEAL_PROMPT_THRESHOLD = 50f;

    /** Need score above which an NPC will accept a haggled (below-market) offer. */
    public static final float DESPERATE_HAGGLE_THRESHOLD = 75f;

    /** Maximum need score per type. */
    public static final float MAX_NEED_SCORE = 100f;

    /** Minimum need score. */
    public static final float MIN_NEED_SCORE = 0f;

    /** Number of needs that must exceed {@link #DESPERATE_HAGGLE_THRESHOLD} to trigger
     *  the compound DESPERATE state. */
    public static final int DESPERATE_COMPOUND_TRIGGER = 3;

    /** Number of simultaneously desperate NPCs that triggers a MARCHETTI_SHIPMENT rumour. */
    public static final int MARCHETTI_RUMOUR_TRIGGER_COUNT = 5;

    /** Base passive income per game-minute per protection racket. */
    public static final int RACKET_PASSIVE_INCOME_COIN = 3;

    /** Faction respect threshold below which a rival faction undercuts a racket. */
    public static final int RACKET_UNDERCUT_RESPECT_THRESHOLD = 40;

    /** Distance in blocks within which the player can interact with an NPC for a deal. */
    public static final float DEAL_INTERACTION_RANGE = 3f;

    /** Minimum haggle ratio (0–1) relative to market price that an NPC will accept
     *  when DESPERATE (need &gt; 75). */
    public static final float DESPERATE_MIN_HAGGLE_RATIO = 0.6f;

    /** Faction trade loyalty bonus when faction Respect &ge; FRIENDLY_THRESHOLD. */
    public static final float FACTION_LOYALTY_PRICE_BONUS = 0.20f;

    /** DisguiseSystem price bonus when wearing GREGGS_APRON in a deal (in-uniform). */
    public static final float DISGUISE_PRICE_BONUS = 0.15f;

    /** Coin penalty to player's racket income per point of Notoriety above Tier 2. */
    public static final float NOTORIETY_RACKET_PENALTY_PER_TIER = 0.10f;

    /** Seconds between racket passive income ticks. */
    public static final float RACKET_INCOME_INTERVAL = 60f;

    /** How many dodgy item types the player must deal in a single session to earn
     *  DODGY_AS_THEY_COME (stolen goods, counterfeit notes, prescription meds). */
    public static final int DODGY_ITEMS_REQUIRED = 3;

    // ── Base prices (in COIN) ─────────────────────────────────────────────────

    private static final Map<Material, Integer> BASE_PRICES = new HashMap<>();

    static {
        BASE_PRICES.put(Material.GREGGS_PASTRY,      2);
        BASE_PRICES.put(Material.CAN_OF_LAGER,       3);
        BASE_PRICES.put(Material.CIGARETTE,          2);
        BASE_PRICES.put(Material.WOOLLY_HAT_ECONOMY, 4);
        BASE_PRICES.put(Material.SLEEPING_BAG,       8);
        BASE_PRICES.put(Material.STOLEN_PHONE,       15);
        BASE_PRICES.put(Material.PRESCRIPTION_MEDS,  12);
        BASE_PRICES.put(Material.COUNTERFEIT_NOTE,   6);
        BASE_PRICES.put(Material.TOBACCO_POUCH,      3);
        BASE_PRICES.put(Material.NEWSPAPER,          1);
        BASE_PRICES.put(Material.SAUSAGE_ROLL,       2);
        BASE_PRICES.put(Material.DODGY_PASTY,        2);
        BASE_PRICES.put(Material.PINT,               4);
        BASE_PRICES.put(Material.WOOLLY_HAT,         4);
        BASE_PRICES.put(Material.COAT,               6);
        BASE_PRICES.put(Material.FLASK_OF_TEA,       3);
        BASE_PRICES.put(Material.SCRATCH_CARD,       2);
        BASE_PRICES.put(Material.ENERGY_DRINK,       2);
    }

    // ── Which goods satisfy which needs ──────────────────────────────────────

    private static final Map<NeedType, Material[]> SATISFIERS = new EnumMap<>(NeedType.class);

    static {
        SATISFIERS.put(NeedType.HUNGRY, new Material[]{
            Material.GREGGS_PASTRY, Material.SAUSAGE_ROLL, Material.DODGY_PASTY,
            Material.CHIPS, Material.KEBAB
        });
        SATISFIERS.put(NeedType.COLD, new Material[]{
            Material.WOOLLY_HAT_ECONOMY, Material.WOOLLY_HAT, Material.COAT, Material.FLASK_OF_TEA
        });
        SATISFIERS.put(NeedType.BORED, new Material[]{
            Material.CAN_OF_LAGER, Material.CIGARETTE, Material.NEWSPAPER, Material.SCRATCH_CARD
        });
        SATISFIERS.put(NeedType.BROKE, new Material[]{
            Material.COIN, Material.COUNTERFEIT_NOTE, Material.STOLEN_PHONE
        });
        SATISFIERS.put(NeedType.SCARED, new Material[]{
            Material.SLEEPING_BAG, Material.CAN_OF_LAGER
        });
        SATISFIERS.put(NeedType.DESPERATE, new Material[]{
            Material.PRESCRIPTION_MEDS, Material.TOBACCO_POUCH
        });
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Per-NPC need scores: npcNeeds.get(npc).get(type) → 0–100 score. */
    private final Map<NPC, EnumMap<NeedType, Float>> npcNeeds = new HashMap<>();

    /** Active market event, or null. */
    private MarketEvent activeEvent = null;

    /** Remaining duration of the active event (seconds). */
    private float eventTimer = 0f;

    /** Businesses on which the player has an active protection racket. */
    private final List<LandmarkType> racketBusinesses = new ArrayList<>();

    /** Timer for racket income ticks. */
    private float racketTimer = 0f;

    /** Total coins earned from rackets (cumulative). */
    private int totalRacketIncome = 0;

    /** Whether BENEFIT_DAY has been active and the broke-need-zero effect applied. */
    private boolean benefitDayActive = false;

    /** Set of dodgy material types handled in this session (for DODGY_AS_THEY_COME). */
    private final java.util.Set<Material> dodgyItemsHandled = new java.util.HashSet<>();

    /** Whether DODGY_AS_THEY_COME has been awarded. */
    private boolean dodgyAchievementAwarded = false;

    /** Whether CORNERED_THE_MARKET has been awarded. */
    private boolean corneredMarketAwarded = false;

    /** How many units of any single commodity the player has sold in one session. */
    private final Map<Material, Integer> sessionSales = new HashMap<>();

    /** Threshold for CORNERED_THE_MARKET achievement (units of same item). */
    private static final int CORNERED_MARKET_THRESHOLD = 10;

    /** Whether the LOAN_SHARK achievement has been awarded. */
    private boolean loanSharkAwarded = false;

    /** Whether COLD_SNAP_CAPITALIST has been awarded. */
    private boolean coldSnapCapitalistAwarded = false;

    /** Whether BENEFIT_FRAUD has been awarded. */
    private boolean benefitFraudAwarded = false;

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public StreetEconomySystem() {
        this(new Random());
    }

    public StreetEconomySystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update needs accumulation, market event timers, and racket income.
     *
     * @param delta      seconds since last frame
     * @param npcs       all NPCs in the world
     * @param player     the player
     * @param weather    current weather state (may be null)
     * @param notorietyTier current notoriety tier (0–5)
     * @param playerInventory player's inventory for racket income drops
     * @param rumourNetwork  for seeding MARCHETTI_SHIPMENT rumours (may be null)
     * @param achievementCallback for unlocking achievements (may be null)
     */
    public void update(float delta, List<NPC> npcs, Player player, Weather weather,
                       int notorietyTier, Inventory playerInventory,
                       RumourNetwork rumourNetwork,
                       NotorietySystem.AchievementCallback achievementCallback) {

        // Update active market event timer
        if (activeEvent != null) {
            eventTimer -= delta;
            if (eventTimer <= 0f) {
                activeEvent = null;
                benefitDayActive = false;
            }
        }

        // Accumulate needs for each NPC
        int desperateCount = 0;
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            EnumMap<NeedType, Float> needs = getNeedsForNpc(npc);
            accumulateNeeds(needs, delta, weather);
            updateDesperate(needs, delta);
            if (needs.get(NeedType.DESPERATE) >= DEAL_PROMPT_THRESHOLD) {
                desperateCount++;
            }
        }

        // BENEFIT_DAY: zero-out BROKE need for all NPCs
        if (activeEvent == MarketEvent.BENEFIT_DAY && !benefitDayActive) {
            benefitDayActive = true;
            for (NPC npc : npcs) {
                if (!npc.isAlive()) continue;
                EnumMap<NeedType, Float> needs = getNeedsForNpc(npc);
                needs.put(NeedType.BROKE, MIN_NEED_SCORE);
            }
        }

        // MARCHETTI_SHIPMENT rumour: 5+ desperate NPCs
        if (desperateCount >= MARCHETTI_RUMOUR_TRIGGER_COUNT && rumourNetwork != null) {
            seedMarchettiRumour(npcs, rumourNetwork);
        }

        // Racket passive income tick
        if (!racketBusinesses.isEmpty()) {
            racketTimer += delta;
            if (racketTimer >= RACKET_INCOME_INTERVAL) {
                racketTimer = 0f;
                int income = computeRacketIncome(notorietyTier);
                if (playerInventory != null && income > 0) {
                    playerInventory.addItem(Material.COIN, income);
                    totalRacketIncome += income;
                }
            }
        }
    }

    // ── Need accumulation ─────────────────────────────────────────────────────

    private void accumulateNeeds(EnumMap<NeedType, Float> needs, float delta, Weather weather) {
        for (NeedType type : NeedType.values()) {
            if (type == NeedType.DESPERATE) continue; // handled separately
            float rate = type.getBaseAccumulationRate();

            // Weather modifier
            if (type == NeedType.COLD && weather != null &&
                    (weather == Weather.COLD_SNAP || weather == Weather.FROST)) {
                rate *= 3.0f;
            }

            // Market event modifier
            if (activeEvent != null) {
                rate = applyEventAccumulation(type, rate, activeEvent);
            }

            float current = needs.getOrDefault(type, 0f);
            needs.put(type, Math.min(MAX_NEED_SCORE, current + rate * delta));
        }
    }

    private float applyEventAccumulation(NeedType type, float rate, MarketEvent event) {
        switch (event) {
            case GREGGS_STRIKE:
                if (type == NeedType.HUNGRY) return rate * 2.5f;
                break;
            case LAGER_SHORTAGE:
                if (type == NeedType.BORED) return rate * 2.0f;
                break;
            case COLD_SNAP:
                if (type == NeedType.COLD) return rate * 3.0f;
                break;
            case BENEFIT_DAY:
                if (type == NeedType.BROKE) return 0f; // handled on activation
                break;
            case MARCHETTI_SHIPMENT:
                if (type == NeedType.DESPERATE) return rate * 2.0f;
                break;
            default:
                break;
        }
        return rate;
    }

    private void updateDesperate(EnumMap<NeedType, Float> needs, float delta) {
        // Count how many non-DESPERATE needs exceed the compound trigger threshold
        int highNeeds = 0;
        for (NeedType type : NeedType.values()) {
            if (type == NeedType.DESPERATE) continue;
            if (needs.getOrDefault(type, 0f) >= DESPERATE_HAGGLE_THRESHOLD) {
                highNeeds++;
            }
        }

        float desperateRate = highNeeds >= DESPERATE_COMPOUND_TRIGGER
            ? NeedType.DESPERATE.getBaseAccumulationRate() : -0.5f; // decay if not triggered

        // Market event spike
        if (activeEvent == MarketEvent.MARCHETTI_SHIPMENT) {
            if (highNeeds >= DESPERATE_COMPOUND_TRIGGER) {
                desperateRate *= 2.0f;
            }
        }

        float current = needs.getOrDefault(NeedType.DESPERATE, 0f);
        needs.put(NeedType.DESPERATE, Math.max(MIN_NEED_SCORE,
            Math.min(MAX_NEED_SCORE, current + desperateRate * delta)));
    }

    // ── Street Dealing ────────────────────────────────────────────────────────

    /**
     * Result of a deal attempt.
     */
    public enum DealResult {
        /** Deal completed; player paid and NPC's need satisfied. */
        SUCCESS,
        /** NPC has no significant need right now. */
        NO_NEED,
        /** Player doesn't have the required item. */
        MISSING_ITEM,
        /** NPC rejected the haggled price. */
        HAGGLE_REJECTED,
        /** Player and NPC are too far apart. */
        OUT_OF_RANGE,
        /** The item is flagged as stolen/dodgy and a police NPC is nearby. */
        POLICE_NEARBY_DODGY
    }

    /**
     * Attempt a street deal with the given NPC.
     *
     * <p>The player offers {@code item} at {@code offeredPrice} COIN. If the NPC's
     * corresponding need exceeds {@link #DEAL_PROMPT_THRESHOLD} and the offered price
     * is at or above the effective market price (or at/above
     * {@link #DESPERATE_MIN_HAGGLE_RATIO} × market price when the NPC is desperate),
     * the deal succeeds.
     *
     * @param npc              the NPC to deal with
     * @param item             material the player is offering
     * @param offeredPrice     coins the player asks for
     * @param playerInventory  player's inventory
     * @param player           the player
     * @param allNpcs          all NPCs (for police-nearby check)
     * @param factionRespect   faction respect for the NPC's aligned faction (0–100); -1 if N/A
     * @param wearingDisguise  true if the player is wearing a relevant disguise
     * @param notorietyTier    current notoriety tier (0–5)
     * @param achievementCallback for unlocking achievements (may be null)
     * @return deal result
     */
    public DealResult attemptDeal(
            NPC npc,
            Material item,
            int offeredPrice,
            Inventory playerInventory,
            Player player,
            List<NPC> allNpcs,
            int factionRespect,
            boolean wearingDisguise,
            int notorietyTier,
            NotorietySystem.AchievementCallback achievementCallback) {

        // Range check
        float dist = npc.getPosition().dst(player.getPosition());
        if (dist > DEAL_INTERACTION_RANGE) {
            return DealResult.OUT_OF_RANGE;
        }

        // Check if player has the item
        if (playerInventory.getItemCount(item) < 1) {
            return DealResult.MISSING_ITEM;
        }

        // Dodgy items: police nearby check
        if (isDodgyItem(item) && isPoliceNearby(player, allNpcs)) {
            return DealResult.POLICE_NEARBY_DODGY;
        }

        // Find the NPC's highest relevant need for this item
        NeedType satisfiedNeed = findSatisfiedNeed(item, npc);
        if (satisfiedNeed == null) {
            return DealResult.NO_NEED;
        }

        float needScore = getNeedScore(npc, satisfiedNeed);
        if (needScore < DEAL_PROMPT_THRESHOLD) {
            return DealResult.NO_NEED;
        }

        // Compute effective market price with modifiers
        int marketPrice = getEffectivePrice(item, factionRespect, wearingDisguise,
            activeEvent, notorietyTier);

        // Haggling check
        boolean desperate = needScore >= DESPERATE_HAGGLE_THRESHOLD;
        int minAcceptablePrice = desperate
            ? (int) Math.floor(marketPrice * DESPERATE_MIN_HAGGLE_RATIO)
            : marketPrice;

        if (offeredPrice < minAcceptablePrice) {
            npc.setSpeechText("Do I look like a charity mate?", 3f);
            return DealResult.HAGGLE_REJECTED;
        }

        // Execute deal: remove item, give coins
        playerInventory.removeItem(item, 1);
        playerInventory.addItem(Material.COIN, offeredPrice);

        // Satisfy need
        getNeedsForNpc(npc).put(satisfiedNeed, MIN_NEED_SCORE);

        // NPC speech
        npc.setSpeechText(satisfiedNeed.getNpcSpeech().replace(".", "! Cheers."), 4f);
        npc.setState(NPCState.IDLE);

        // Track sales for CORNERED_THE_MARKET
        sessionSales.merge(item, 1, Integer::sum);
        if (!corneredMarketAwarded && sessionSales.getOrDefault(item, 0) >= CORNERED_MARKET_THRESHOLD) {
            corneredMarketAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CORNERED_THE_MARKET);
            }
        }

        // ENTREPRENEUR: first ever successful deal
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.ENTREPRENEUR);
        }

        // Track dodgy items for DODGY_AS_THEY_COME
        if (isDodgyItem(item)) {
            dodgyItemsHandled.add(item);
            checkDodgyAchievement(achievementCallback);
        }

        // COLD_SNAP_CAPITALIST: sold warm item at double-market price during cold snap
        if (!coldSnapCapitalistAwarded
                && activeEvent == MarketEvent.COLD_SNAP
                && (item == Material.WOOLLY_HAT_ECONOMY || item == Material.WOOLLY_HAT || item == Material.COAT)
                && offeredPrice >= marketPrice * 2) {
            coldSnapCapitalistAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.COLD_SNAP_CAPITALIST);
            }
        }

        // BENEFIT_FRAUD: exploited benefit day
        if (!benefitFraudAwarded && activeEvent == MarketEvent.BENEFIT_DAY
                && item == Material.COUNTERFEIT_NOTE) {
            benefitFraudAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BENEFIT_FRAUD);
            }
        }

        return DealResult.SUCCESS;
    }

    /**
     * Offer a loan to a BROKE NPC. If interest &gt; 0, LOAN_SHARK achievement is triggered.
     *
     * @param npc              target NPC
     * @param loanAmount       coins loaned
     * @param interestAmount   coins interest charged (0 for a free loan)
     * @param playerInventory  player's inventory
     * @param player           the player
     * @param achievementCallback for achievements (may be null)
     * @return true if loan was given
     */
    public boolean offerLoan(NPC npc, int loanAmount, int interestAmount,
                             Inventory playerInventory, Player player,
                             NotorietySystem.AchievementCallback achievementCallback) {
        float dist = npc.getPosition().dst(player.getPosition());
        if (dist > DEAL_INTERACTION_RANGE) return false;
        if (playerInventory.getItemCount(Material.COIN) < loanAmount) return false;

        float brokeScore = getNeedScore(npc, NeedType.BROKE);
        if (brokeScore < DEAL_PROMPT_THRESHOLD) return false;

        playerInventory.removeItem(Material.COIN, loanAmount);
        getNeedsForNpc(npc).put(NeedType.BROKE, MIN_NEED_SCORE);
        npc.setSpeechText("Cheers mate, you're a lifesaver.", 4f);

        if (interestAmount > 0 && !loanSharkAwarded) {
            loanSharkAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.LOAN_SHARK);
            }
        }
        return true;
    }

    // ── Protection Rackets ────────────────────────────────────────────────────

    /**
     * Start a protection racket on the given business.
     *
     * @param business         landmark type of the business
     * @param factionRespects  map of faction to current respect (for rival undercut check)
     * @return true if racket was started (false if already active or conditions not met)
     */
    public boolean startRacket(LandmarkType business,
                               Map<Faction, Integer> factionRespects) {
        if (racketBusinesses.contains(business)) return false;

        // Check rival faction undercut
        if (factionRespects != null) {
            for (Map.Entry<Faction, Integer> entry : factionRespects.entrySet()) {
                if (entry.getValue() < RACKET_UNDERCUT_RESPECT_THRESHOLD) {
                    // Rival faction will undercut
                    return false;
                }
            }
        }

        racketBusinesses.add(business);
        return true;
    }

    /**
     * Stop a protection racket on the given business.
     */
    public boolean stopRacket(LandmarkType business) {
        return racketBusinesses.remove(business);
    }

    /**
     * Returns the list of businesses under a protection racket.
     */
    public List<LandmarkType> getRacketBusinesses() {
        return java.util.Collections.unmodifiableList(racketBusinesses);
    }

    /**
     * Compute racket income after applying notoriety penalties.
     */
    private int computeRacketIncome(int notorietyTier) {
        if (racketBusinesses.isEmpty()) return 0;
        float penalty = notorietyTier > 2
            ? (notorietyTier - 2) * NOTORIETY_RACKET_PENALTY_PER_TIER
            : 0f;
        float multiplier = Math.max(0f, 1f - penalty);
        return Math.round(racketBusinesses.size() * RACKET_PASSIVE_INCOME_COIN * multiplier);
    }

    // ── Market Events ─────────────────────────────────────────────────────────

    /**
     * Trigger a market disruption event. Replaces any active event.
     *
     * @param event            the event to trigger
     * @param allNpcs          all NPCs (for speech bubbles)
     * @param rumourNetwork    for seeding event rumours (may be null)
     */
    public void triggerMarketEvent(MarketEvent event, List<NPC> allNpcs,
                                   RumourNetwork rumourNetwork) {
        this.activeEvent = event;
        this.eventTimer = event.getDurationSeconds();
        this.benefitDayActive = false;

        // Seed rumour about event
        if (rumourNetwork != null && allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (!npc.isAlive()) continue;
                if (npc.getType() == NPCType.BARMAN || npc.getType() == NPCType.PUBLIC) {
                    rumourNetwork.addRumour(npc,
                        new Rumour(RumourType.LOOT_TIP, event.getDescription()));
                    break; // seed once
                }
            }
        }

        // NPC speech reaction
        if (allNpcs != null) {
            int reacted = 0;
            for (NPC npc : allNpcs) {
                if (!npc.isAlive() || reacted >= 3) break;
                if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.BARMAN) {
                    npc.setSpeechText(event.getDescription(), 5f);
                    reacted++;
                }
            }
        }
    }

    /**
     * Force-end the current market event (for testing).
     */
    public void endMarketEvent() {
        this.activeEvent = null;
        this.eventTimer = 0f;
        this.benefitDayActive = false;
    }

    // ── Price computation ─────────────────────────────────────────────────────

    /**
     * Get the effective market price for an item, incorporating all modifiers.
     *
     * @param item             material to price
     * @param factionRespect   faction respect (0–100); -1 = no faction bonus
     * @param wearingDisguise  true if player wears a relevant disguise
     * @param event            active market event (may be null)
     * @param notorietyTier    current notoriety tier (0–5)
     * @return effective price in COIN (minimum 1)
     */
    public int getEffectivePrice(Material item, int factionRespect, boolean wearingDisguise,
                                 MarketEvent event, int notorietyTier) {
        int base = BASE_PRICES.getOrDefault(item, 1);
        float price = base;

        // Market event price modifier
        if (event != null && event.affects(item)) {
            price *= event.getPriceMultiplier();
        }

        // Faction loyalty bonus
        if (factionRespect >= FactionSystem.FRIENDLY_THRESHOLD) {
            price *= (1f + FACTION_LOYALTY_PRICE_BONUS);
        }

        // Disguise bonus
        if (wearingDisguise) {
            price *= (1f + DISGUISE_PRICE_BONUS);
        }

        return Math.max(1, Math.round(price));
    }

    /**
     * Get the base price for an item (without modifiers).
     */
    public int getBasePrice(Material item) {
        return BASE_PRICES.getOrDefault(item, 1);
    }

    // ── Need accessors ────────────────────────────────────────────────────────

    /**
     * Get the need score for a specific NPC and need type.
     *
     * @return need score 0–100
     */
    public float getNeedScore(NPC npc, NeedType type) {
        return getNeedsForNpc(npc).getOrDefault(type, 0f);
    }

    /**
     * Get the highest need type for the given NPC.
     * Returns null if all needs are below the deal prompt threshold.
     */
    public NeedType getHighestNeed(NPC npc) {
        EnumMap<NeedType, Float> needs = getNeedsForNpc(npc);
        NeedType highest = null;
        float highestScore = DEAL_PROMPT_THRESHOLD;
        for (Map.Entry<NeedType, Float> entry : needs.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                highest = entry.getKey();
            }
        }
        return highest;
    }

    /**
     * Directly set a need score for an NPC (for testing / event injection).
     */
    public void setNeedScore(NPC npc, NeedType type, float score) {
        getNeedsForNpc(npc).put(type, Math.max(MIN_NEED_SCORE, Math.min(MAX_NEED_SCORE, score)));
    }

    /**
     * Spike the SCARED need for all NPCs within range of a crime location.
     *
     * @param crimeX  X coordinate of the crime
     * @param crimeZ  Z coordinate of the crime
     * @param range   radius in blocks
     * @param spike   amount to add to SCARED need
     * @param allNpcs all NPCs
     */
    public void onCrimeEvent(float crimeX, float crimeZ, float range, float spike,
                             List<NPC> allNpcs) {
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            float dx = npc.getPosition().x - crimeX;
            float dz = npc.getPosition().z - crimeZ;
            if (Math.sqrt(dx * dx + dz * dz) <= range) {
                float current = getNeedScore(npc, NeedType.SCARED);
                setNeedScore(npc, NeedType.SCARED, current + spike);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EnumMap<NeedType, Float> getNeedsForNpc(NPC npc) {
        return npcNeeds.computeIfAbsent(npc, k -> {
            EnumMap<NeedType, Float> m = new EnumMap<>(NeedType.class);
            for (NeedType t : NeedType.values()) {
                m.put(t, 0f);
            }
            return m;
        });
    }

    /**
     * Find which NeedType is satisfied by the given material for the given NPC.
     * Returns the need with the highest score that this item satisfies, or null.
     */
    private NeedType findSatisfiedNeed(Material item, NPC npc) {
        NeedType best = null;
        float bestScore = DEAL_PROMPT_THRESHOLD;
        for (Map.Entry<NeedType, Material[]> entry : SATISFIERS.entrySet()) {
            for (Material m : entry.getValue()) {
                if (m == item) {
                    float score = getNeedScore(npc, entry.getKey());
                    if (score > bestScore) {
                        bestScore = score;
                        best = entry.getKey();
                    }
                    break;
                }
            }
        }
        return best;
    }

    /** Returns true if the item is a dodgy/stolen goods type. */
    private boolean isDodgyItem(Material item) {
        return item == Material.STOLEN_PHONE
            || item == Material.COUNTERFEIT_NOTE
            || item == Material.PRESCRIPTION_MEDS;
    }

    /** Returns true if a POLICE NPC is within 6 blocks of the player. */
    private boolean isPoliceNearby(Player player, List<NPC> allNpcs) {
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.POLICE || npc.getType() == NPCType.ARMED_RESPONSE
                    || npc.getType() == NPCType.PCSO) {
                if (npc.getPosition().dst(player.getPosition()) <= 6f) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkDodgyAchievement(NotorietySystem.AchievementCallback achievementCallback) {
        if (!dodgyAchievementAwarded && achievementCallback != null) {
            boolean hasStolen = dodgyItemsHandled.contains(Material.STOLEN_PHONE);
            boolean hasCounterfeit = dodgyItemsHandled.contains(Material.COUNTERFEIT_NOTE);
            boolean hasMeds = dodgyItemsHandled.contains(Material.PRESCRIPTION_MEDS);
            if (hasStolen && hasCounterfeit && hasMeds) {
                dodgyAchievementAwarded = true;
                achievementCallback.award(AchievementType.DODGY_AS_THEY_COME);
            }
        }
    }

    private void seedMarchettiRumour(List<NPC> allNpcs, RumourNetwork rumourNetwork) {
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.BARMAN) {
                rumourNetwork.addRumour(npc,
                    new Rumour(RumourType.GANG_ACTIVITY,
                        "Word is Marchetti's got a shipment in. Half the street's gagging for it."));
                return;
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Active market event, or null. */
    public MarketEvent getActiveEvent() {
        return activeEvent;
    }

    /** Remaining event duration in seconds. */
    public float getEventTimer() {
        return eventTimer;
    }

    /** Whether the given market event is currently active. */
    public boolean isEventActive(MarketEvent event) {
        return activeEvent == event;
    }

    /** Total coins earned from protection rackets. */
    public int getTotalRacketIncome() {
        return totalRacketIncome;
    }

    /** Session sales count for the given item (for CORNERED_THE_MARKET tracking). */
    public int getSessionSales(Material item) {
        return sessionSales.getOrDefault(item, 0);
    }

    /**
     * Returns a map of all need scores for the given NPC (read-only view).
     */
    public Map<NeedType, Float> getAllNeeds(NPC npc) {
        return java.util.Collections.unmodifiableMap(getNeedsForNpc(npc));
    }

    /**
     * Returns true if the given item satisfies any need for the given NPC above
     * the deal prompt threshold.
     */
    public boolean hasRelevantNeed(NPC npc, Material item) {
        return findSatisfiedNeed(item, npc) != null;
    }
}

package ragamuffin.core;

/**
 * Issue #769: Dynamic NPC Needs — the six need states that every NPC accumulates over time.
 *
 * <p>Each NPC has a need score (0–100) per type. Scores accumulate based on time,
 * weather, faction events, and market disruptions. The player can satisfy needs
 * for coin via contextual E-key prompts.
 *
 * <ul>
 *   <li><b>HUNGRY</b>   — rises over time; spikes on GREGGS_STRIKE event</li>
 *   <li><b>COLD</b>     — rises in COLD_SNAP / FROST weather; satisfied by WOOLLY_HAT_ECONOMY</li>
 *   <li><b>BORED</b>    — rises slowly; satisfied by CAN_OF_LAGER or NEWSPAPER</li>
 *   <li><b>BROKE</b>    — accumulates; player can loan money (LOAN_SHARK mechanic)</li>
 *   <li><b>SCARED</b>   — spikes near crime events; satisfied by SLEEPING_BAG or reassurance</li>
 *   <li><b>DESPERATE</b>— compound state; rises when 3+ other needs are high; satisfied by PRESCRIPTION_MEDS</li>
 * </ul>
 */
public enum NeedType {

    /**
     * HUNGRY: satisfied by GREGGS_PASTRY, SAUSAGE_ROLL, CHIPS, KEBAB, or similar food items.
     * Accumulation rate: 1.0 per game-minute normally; 2.5 per game-minute during GREGGS_STRIKE.
     */
    HUNGRY(
        "Hungry",
        "Could murder a pasty right now.",
        1.0f,
        5  // coin reward when satisfied
    ),

    /**
     * COLD: satisfied by WOOLLY_HAT_ECONOMY, COAT, or FLASK_OF_TEA.
     * Accumulation rate: 0.5 per game-minute normally; 3.0 per game-minute during COLD_SNAP.
     */
    COLD(
        "Cold",
        "Absolutely freezing, mate.",
        0.5f,
        4
    ),

    /**
     * BORED: satisfied by CAN_OF_LAGER, NEWSPAPER, or a CIGARETTE.
     * Accumulation rate: 0.8 per game-minute.
     * Spikes on BENEFIT_DAY (NPCs with coin but nothing to do).
     */
    BORED(
        "Bored",
        "Nothing to do round here, is there.",
        0.8f,
        3
    ),

    /**
     * BROKE: satisfied by gifting COIN or COUNTERFEIT_NOTE.
     * Player can offer a loan (triggers LOAN_SHARK mechanic if interest is charged).
     * Accumulation rate: 0.3 per game-minute; faster after BENEFIT_DAY ends.
     */
    BROKE(
        "Broke",
        "Haven't got a penny to my name.",
        0.3f,
        2
    ),

    /**
     * SCARED: spikes when a crime event occurs nearby; satisfied by SLEEPING_BAG.
     * Accumulation rate: spikes to 50 on nearby crime; decays slowly (0.2/game-minute).
     */
    SCARED(
        "Scared",
        "It's not safe round here.",
        0.2f,
        6
    ),

    /**
     * DESPERATE: compound state — rises when 3+ other needs exceed 60.
     * Satisfied exclusively by PRESCRIPTION_MEDS or TOBACCO_POUCH.
     * Accumulation rate: 2.0 per game-minute when compound condition is active.
     * Triggers MARCHETTI_SHIPMENT rumour when 5+ NPCs are desperate simultaneously.
     */
    DESPERATE(
        "Desperate",
        "I need something. Anything. Please.",
        2.0f,
        10
    );

    private final String displayName;
    private final String npcSpeech;
    /** Base accumulation rate in need-points per real second (game time). */
    private final float baseAccumulationRate;
    /** Base coin reward when the player satisfies this need. */
    private final int baseCoinReward;

    NeedType(String displayName, String npcSpeech, float baseAccumulationRate, int baseCoinReward) {
        this.displayName = displayName;
        this.npcSpeech = npcSpeech;
        this.baseAccumulationRate = baseAccumulationRate;
        this.baseCoinReward = baseCoinReward;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNpcSpeech() {
        return npcSpeech;
    }

    public float getBaseAccumulationRate() {
        return baseAccumulationRate;
    }

    public int getBaseCoinReward() {
        return baseCoinReward;
    }
}

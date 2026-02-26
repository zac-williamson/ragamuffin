package ragamuffin.world;

/**
 * Defines the types of unique non-block-based 3D props that can be placed in the world.
 *
 * Props are decorative objects rendered as simple 3D geometric models rather than
 * voxel blocks. They add visual variety to the environment and help make the British
 * town feel more realistic and lived-in.
 *
 * Issue #669: Add unique non-block-based 3D models to the world.
 */
public enum PropType {

    /** A classic red British telephone box. */
    PHONE_BOX,

    /** A bright red cylindrical Royal Mail post box. */
    POST_BOX,

    /** A wooden park bench with metal legs. */
    PARK_BENCH,

    /** A covered bus shelter with glass panels and a metal frame. */
    BUS_SHELTER,

    /** A bollard — short concrete post used to control traffic. */
    BOLLARD,

    /** A street lamp / lamppost with a curving arm and light. */
    STREET_LAMP,

    /** A litter bin / rubbish bin. */
    LITTER_BIN,

    /** A market stall with a coloured canvas awning. */
    MARKET_STALL,

    /** A park picnic table. */
    PICNIC_TABLE,

    /** A bicycle rack. */
    BIKE_RACK,

    /** A shopping trolley, abandoned or in a rack. */
    SHOPPING_TROLLEY,

    /** A park statue / monument on a plinth. */
    STATUE,

    // ── Pub props (Phase 8b / Issue #696) ──────────────────────────────────

    /** A dartboard mounted on a pub wall. */
    DARTBOARD,

    /** A pub bar stool. */
    BAR_STOOL,

    /** A fruit machine (slot machine) — interactive, costs 1 coin to play. */
    FRUIT_MACHINE,

    /** A pub table. */
    PUB_TABLE,

    /** A pub chair. */
    PUB_CHAIR,

    // ── Faction turf props (Phase 8d / Issue #702) ─────────────────────────

    /**
     * A spray-paint graffiti tag on a wall — faction-coloured, appears/disappears
     * as turf shifts between Marchetti Crew, Street Lads, and The Council.
     */
    GRAFFITI_TAG,

    // ── Heist props (Phase O / Issue #704) ─────────────────────────────────────

    /**
     * A red wall-mounted alarm box on heistable buildings. While active, any block
     * break within 8 blocks spikes noise to 1.0 and flags the player as wanted.
     * Silenced by interacting (E) with BOLT_CUTTERS (1 second). Reactivates after
     * 3 in-game minutes.
     */
    ALARM_BOX,

    /**
     * A heavy metal safe inside the jeweller and off-licence back rooms. Cannot be
     * punched open. Requires CROWBAR + 8 seconds of hold-E interaction while
     * undetected. Yields diamonds or coin.
     */
    SAFE,

    /**
     * A wall-mounted CCTV camera. If the player walks within its 6-block frontal
     * cone (45°) without a BALACLAVA at night, they gain +1 criminal record per
     * second and a GANG_ACTIVITY rumour is seeded.
     */
    CCTV,

    // ── Issue #714: Player Squat system ─────────────────────────────────────

    /**
     * A bed inside the squat. Provides +10 Vibe and is required to host lodgers.
     * Also enables 2× energy recovery when sleeping inside at night (Vibe ≥ 40).
     */
    BED,

    /**
     * A dartboard mounted on the squat wall. Provides +7 Vibe.
     * (Named SQUAT_DARTBOARD to distinguish from the pub DARTBOARD prop.)
     */
    SQUAT_DARTBOARD,

    /**
     * A workbench inside the squat. Provides 0 Vibe but unlocks advanced crafting recipes:
     * BARRICADE, LOCKPICK, and FAKE_ID.
     */
    WORKBENCH,

    // ── Issue #716: Underground Music Scene ─────────────────────────────────

    /**
     * A large speaker stack — rave equipment prop. Increases rave capacity by 5
     * and income by +1 COIN/attendee/minute. Provides +5 Vibe to the squat.
     */
    SPEAKER_STACK,

    /**
     * A mirror ball — rave equipment prop. Increases rave capacity by 3 and
     * provides +3 Vibe to the squat.
     */
    DISCO_BALL,

    /**
     * DJ decks (turntables + mixer) — rave equipment prop. Enables recruiting a
     * STREET_LAD NPC as resident DJ, which doubles rave income. Provides +8 Vibe.
     */
    DJ_DECKS,

    /**
     * A WANTED poster — appears on walls near the squat at Notoriety Tier 5.
     * Replaced by FAN_POSTER once the player reaches MC Rank 5.
     */
    WANTED_POSTER,

    /**
     * A fan poster of the MC (player) — replaces WANTED_POSTERs near the squat
     * upon reaching MC Rank 5, and attracts the permanent hype-man NPC.
     */
    FAN_POSTER,
}

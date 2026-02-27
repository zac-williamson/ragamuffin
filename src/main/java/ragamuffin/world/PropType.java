package ragamuffin.world;

import ragamuffin.building.Material;

/**
 * Defines the types of unique non-block-based 3D props that can be placed in the world.
 *
 * Props are decorative objects rendered as simple 3D geometric models rather than
 * voxel blocks. They add visual variety to the environment and help make the British
 * town feel more realistic and lived-in.
 *
 * Issue #669: Add unique non-block-based 3D models to the world.
 * Issue #719: Props now have collision (AABB) and are destructible into materials.
 */
public enum PropType {

    /** A classic red British telephone box. */
    PHONE_BOX(0.90f, 2.60f, 0.90f, 8, Material.SCRAP_METAL),

    /** A bright red cylindrical Royal Mail post box. */
    POST_BOX(0.50f, 1.00f, 0.50f, 5, Material.SCRAP_METAL),

    /** A wooden park bench with metal legs. */
    PARK_BENCH(1.40f, 1.00f, 0.60f, 3, Material.WOOD),

    /** A covered bus shelter with glass panels and a metal frame. */
    BUS_SHELTER(2.60f, 2.35f, 1.20f, 8, Material.GLASS),

    /** A bollard — short concrete post used to control traffic. */
    BOLLARD(0.28f, 1.05f, 0.28f, 5, Material.CONCRETE),

    /** A street lamp / lamppost with a curving arm and light. */
    STREET_LAMP(0.28f, 4.20f, 0.28f, 5, Material.SCRAP_METAL),

    /** A litter bin / rubbish bin. */
    LITTER_BIN(0.46f, 1.32f, 0.46f, 3, Material.SCRAP_METAL),

    /** A market stall with a coloured canvas awning. */
    MARKET_STALL(1.70f, 2.22f, 1.20f, 5, Material.WOOD),

    /** A park picnic table. */
    PICNIC_TABLE(1.60f, 0.84f, 1.60f, 3, Material.WOOD),

    /** A bicycle rack. */
    BIKE_RACK(0.92f, 0.96f, 0.12f, 5, Material.SCRAP_METAL),

    /** A shopping trolley, abandoned or in a rack. */
    SHOPPING_TROLLEY(0.80f, 1.00f, 0.80f, 3, Material.SCRAP_METAL),

    /** A park statue / monument on a plinth. */
    STATUE(1.20f, 2.54f, 1.20f, 8, Material.STONE),

    // ── Pub props (Phase 8b / Issue #696) ──────────────────────────────────

    /** A dartboard mounted on a pub wall. */
    DARTBOARD(0.50f, 0.50f, 0.10f, 2, Material.WOOD),

    /** A pub bar stool. */
    BAR_STOOL(0.40f, 0.80f, 0.40f, 3, Material.WOOD),

    /** A fruit machine (slot machine) — interactive, costs 1 coin to play. */
    FRUIT_MACHINE(0.60f, 1.80f, 0.50f, 8, Material.SCRAP_METAL),

    /** A pub table. */
    PUB_TABLE(1.00f, 0.80f, 0.70f, 3, Material.WOOD),

    /** A pub chair. */
    PUB_CHAIR(0.50f, 0.90f, 0.50f, 3, Material.WOOD),

    // ── Faction turf props (Phase 8d / Issue #702) ─────────────────────────

    /**
     * A spray-paint graffiti tag on a wall — faction-coloured, appears/disappears
     * as turf shifts between Marchetti Crew, Street Lads, and The Council.
     */
    GRAFFITI_TAG(0.80f, 0.60f, 0.02f, 1, null),

    // ── Heist props (Phase O / Issue #704) ─────────────────────────────────────

    /**
     * A red wall-mounted alarm box on heistable buildings. While active, any block
     * break within 8 blocks spikes noise to 1.0 and flags the player as wanted.
     * Silenced by interacting (E) with BOLT_CUTTERS (1 second). Reactivates after
     * 3 in-game minutes.
     */
    ALARM_BOX(0.30f, 0.30f, 0.15f, 3, Material.SCRAP_METAL),

    /**
     * A heavy metal safe inside the jeweller and off-licence back rooms. Cannot be
     * punched open. Requires CROWBAR + 8 seconds of hold-E interaction while
     * undetected. Yields diamonds or coin.
     */
    SAFE(0.80f, 1.00f, 0.70f, 20, Material.IRON),

    /**
     * A wall-mounted CCTV camera. If the player walks within its 6-block frontal
     * cone (45°) without a BALACLAVA at night, they gain +1 criminal record per
     * second and a GANG_ACTIVITY rumour is seeded.
     */
    CCTV(0.25f, 0.20f, 0.30f, 3, Material.SCRAP_METAL),

    // ── Issue #714: Player Squat system ─────────────────────────────────────

    /**
     * A bed inside the squat. Provides +10 Vibe and is required to host lodgers.
     * Also enables 2× energy recovery when sleeping inside at night (Vibe ≥ 40).
     */
    BED(1.00f, 0.60f, 2.00f, 5, Material.WOOD),

    /**
     * A dartboard mounted on the squat wall. Provides +7 Vibe.
     * (Named SQUAT_DARTBOARD to distinguish from the pub DARTBOARD prop.)
     */
    SQUAT_DARTBOARD(0.50f, 0.50f, 0.10f, 2, Material.WOOD),

    /**
     * A workbench inside the squat. Provides 0 Vibe but unlocks advanced crafting recipes:
     * BARRICADE, LOCKPICK, and FAKE_ID.
     */
    WORKBENCH(1.20f, 0.90f, 0.70f, 8, Material.WOOD),

    // ── Issue #716: Underground Music Scene ─────────────────────────────────

    /**
     * A large speaker stack — rave equipment prop. Increases rave capacity by 5
     * and income by +1 COIN/attendee/minute. Provides +5 Vibe to the squat.
     */
    SPEAKER_STACK(0.80f, 1.80f, 0.60f, 5, Material.SCRAP_METAL),

    /**
     * A mirror ball — rave equipment prop. Increases rave capacity by 3 and
     * provides +3 Vibe to the squat.
     */
    DISCO_BALL(0.30f, 0.30f, 0.30f, 2, Material.GLASS),

    /**
     * DJ decks (turntables + mixer) — rave equipment prop. Enables recruiting a
     * STREET_LAD NPC as resident DJ, which doubles rave income. Provides +8 Vibe.
     */
    DJ_DECKS(1.20f, 0.50f, 0.70f, 5, Material.SCRAP_METAL),

    /**
     * A WANTED poster — appears on walls near the squat at Notoriety Tier 5.
     * Replaced by FAN_POSTER once the player reaches MC Rank 5.
     */
    WANTED_POSTER(0.50f, 0.70f, 0.02f, 1, null),

    /**
     * A fan poster of the MC (player) — replaces WANTED_POSTERs near the squat
     * upon reaching MC Rank 5, and attracts the permanent hype-man NPC.
     */
    FAN_POSTER(0.50f, 0.70f, 0.02f, 1, null);

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #719: Collision and destructibility data
    // ─────────────────────────────────────────────────────────────────────────

    /** Width of the prop's collision bounding box (X axis). */
    private final float collisionWidth;
    /** Height of the prop's collision bounding box (Y axis). */
    private final float collisionHeight;
    /** Depth of the prop's collision bounding box (Z axis). */
    private final float collisionDepth;
    /** Number of punches required to destroy this prop. */
    private final int hitsToBreak;
    /**
     * Material dropped when this prop is destroyed, or {@code null} if nothing
     * is dropped (e.g. flat posters/graffiti that just disappear).
     */
    private final Material materialDrop;

    PropType(float collisionWidth, float collisionHeight, float collisionDepth,
             int hitsToBreak, Material materialDrop) {
        this.collisionWidth  = collisionWidth;
        this.collisionHeight = collisionHeight;
        this.collisionDepth  = collisionDepth;
        this.hitsToBreak     = hitsToBreak;
        this.materialDrop    = materialDrop;
    }

    /**
     * Width of the AABB used for collision detection (X axis, centred on the
     * prop's world position).
     */
    public float getCollisionWidth()  { return collisionWidth; }

    /**
     * Height of the AABB used for collision detection (Y axis, measured from
     * the prop's base Y position upward).
     */
    public float getCollisionHeight() { return collisionHeight; }

    /**
     * Depth of the AABB used for collision detection (Z axis, centred on the
     * prop's world position).
     */
    public float getCollisionDepth()  { return collisionDepth; }

    /**
     * Number of bare-fist punches required to destroy this prop.
     */
    public int getHitsToBreak()       { return hitsToBreak; }

    /**
     * Material dropped when this prop is destroyed, or {@code null} if nothing
     * is dropped.
     */
    public Material getMaterialDrop() { return materialDrop; }
}

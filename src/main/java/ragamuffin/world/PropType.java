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
    FAN_POSTER(0.50f, 0.70f, 0.02f, 1, null),

    // ── Issue #721: Small 3D objects on shop shelves ─────────────────────────

    /**
     * A small tin can — merchandise on a shop shelf.
     * Yields SCRAP_METAL when broken.
     */
    SHELF_CAN(0.14f, 0.20f, 0.14f, 1, Material.SCRAP_METAL),

    /**
     * A small glass bottle — merchandise on a shop shelf (off-licence, Greggs, etc.).
     * Yields GLASS when broken.
     */
    SHELF_BOTTLE(0.10f, 0.28f, 0.10f, 1, Material.GLASS),

    /**
     * A small cardboard box — merchandise on a shop shelf.
     * Yields nothing (too small) when broken.
     */
    SHELF_BOX(0.18f, 0.16f, 0.14f, 1, null),

    // ── Issue #795: The JobCentre Gauntlet ────────────────────────────────────

    /**
     * A laminated DWP motivational poster on the JobCentre wall.
     * "YOUR JOB IS OUT THERE" / "WORK SETS YOU FREE" etc.
     * Destroyed by 1 punch; yields nothing. Standing in front for 3 seconds
     * completes the CV_WORKSHOP job search mission.
     */
    MOTIVATIONAL_POSTER(0.60f, 0.80f, 0.02f, 1, null),

    /**
     * A community notice board covered in job listings and council leaflets.
     * Interact (E) to trigger the UNIVERSAL_JOBMATCH_PROFILE mission dialogue.
     */
    COMMUNITY_NOTICE_BOARD(0.80f, 1.20f, 0.10f, 3, Material.WOOD),

    /**
     * A DWP official letter — prop item dropped or placed on the desk.
     * Interacting reads the current sanction level / UC payment amount.
     * Can be carried in inventory as DWP_LETTER material.
     */
    DWP_LETTER(0.20f, 0.02f, 0.28f, 1, null),

    /**
     * Litter on the floor — generated during a Mandatory Work Placement mission
     * to simulate picking up rubbish. Player must "break" 3 LITTER props
     * within 30 seconds to complete the mission.
     */
    LITTER(0.30f, 0.05f, 0.30f, 1, null),

    // ── Issue #797: The Neighbourhood Watch ───────────────────────────────────

    /**
     * A petition board — spawned on a pavement at WatchAnger Tier 2 (Petitions).
     * Displays a community petition against the player's behaviour.
     * Can be removed by crafting and using a NEIGHBOURHOOD_NEWSLETTER (−8 Anger).
     * Destroyed by 3 punches; yields nothing.
     */
    PETITION_BOARD(0.80f, 1.40f, 0.10f, 3, null),

    // ── Issue #799: The Corner Shop Economy ────────────────────────────────────

    /**
     * A hand-painted shop sign above the door of the player's corner shop.
     * Placed automatically when the player claims and opens their shop.
     * Destroyed by 5 punches; yields WOOD when broken.
     */
    SHOP_SIGN(1.20f, 0.50f, 0.10f, 5, Material.WOOD),

    /**
     * An official-looking council inspection notice taped to the shop door.
     * Spawned when shop Heat reaches 30 (Inspection Notice threshold).
     * Interacting (E) dismisses it but raises Heat by +5 if ignored for one day.
     * Destroyed by 1 punch; yields nothing.
     */
    INSPECTION_NOTICE(0.30f, 0.40f, 0.02f, 1, null),

    /**
     * A business rates demand notice from the council.
     * Spawns when daily revenue exceeds 50 coins.
     * Ignoring it triggers a subsidised competitor stall from the Council.
     * Destroyed by 1 punch; yields nothing.
     */
    BUSINESS_RATES_NOTICE(0.30f, 0.40f, 0.02f, 1, null),

    // ── Issue #908 / #909: Bookies Horse Racing System ───────────────────────

    /**
     * A wall-mounted TV screen inside the bookies, showing live race results and
     * race schedules. Player presses E to open BettingUI and place bets.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    TV_SCREEN(0.80f, 0.50f, 0.10f, 5, Material.SCRAP_METAL),

    // ── Issue #801: The Underground Fight Night ────────────────────────────────

    /**
     * A chalkboard BOOKIE_BOARD showing current odds and championship ladder standings.
     * Found inside the Pit next to the BOOKIE_NPC.
     * Interact (E) to view odds; destroyed by 3 punches, yields WOOD.
     */
    BOOKIE_BOARD(0.80f, 1.20f, 0.10f, 3, Material.WOOD),

    /**
     * A rope-prop forming the perimeter of the fighting ring.
     * Six of these form the 6×6 square boundary inside the Pit.
     * Destroyed by 1 punch; yields nothing.
     */
    FIGHT_RING_ROPE(1.00f, 0.60f, 0.10f, 1, null),

    /**
     * A hanging lantern providing atmospheric lighting inside the Pit.
     * Emits a warm orange glow. Destroyed by 2 punches; yields SCRAP_METAL.
     */
    LANTERN(0.20f, 0.40f, 0.20f, 2, Material.SCRAP_METAL),

    // ── Issue #914: Allotment System ──────────────────────────────────────────

    /**
     * A council repossession notice — posted on the allotment gate post after
     * 3 consecutive fallow days. The player has 1 in-game day to plant a seed
     * before the plot is repossessed. Destroyed by 1 punch; yields nothing.
     */
    REPOSSESSION_NOTICE(0.30f, 0.40f, 0.02f, 1, null),

    // ── Issue #922: Skate Park System ─────────────────────────────────────────

    /**
     * A council closure notice — a bright yellow warning sign placed on the skate
     * park perimeter wall during a Park Closure Attempt. Player can tear it down
     * by pressing E (granting Street Lads Respect +10, Council Respect −5) or
     * bribe the COUNCIL_MEMBER NPC for 8 COIN. Destroyed by 1 punch; yields nothing.
     */
    CLOSURE_NOTICE(0.30f, 0.50f, 0.02f, 1, null),

    // ── Issue #924: Launderette System ────────────────────────────────────────

    /**
     * A front-loading washing machine inside the Spotless Launderette.
     * Interact (E) while holding 2 COIN to start a 90-second wash cycle.
     * On completion the player receives CLEAN_CLOTHES. If wearing a BLOODY_HOODIE
     * or STOLEN_JACKET, the wash also deducts 2 Notoriety and clears the
     * COVERED_IN_BLOOD debuff. Destroyed by 5 punches; yields SCRAP_METAL.
     */
    WASHING_MACHINE(0.70f, 1.00f, 0.65f, 5, Material.SCRAP_METAL),

    /**
     * A changing cubicle curtain-prop inside the launderette.
     * Interact (E) while holding CLEAN_CLOTHES to equip them and gain the
     * FRESHLY_LAUNDERED buff (−20% NPC recognition chance for 3 in-game minutes).
     * Destroyed by 2 punches; yields nothing.
     */
    CHANGING_CUBICLE(0.90f, 2.00f, 0.90f, 2, null),

    /**
     * A hand-painted CLOSED sign hung on the launderette door outside opening hours
     * (22:00–07:00). Attempting to enter while this sign is visible triggers
     * ANGER_VISIBLE_CRIME in the NeighbourhoodWatchSystem. Destroyed by 1 punch;
     * yields nothing.
     */
    CLOSED_SIGN(0.40f, 0.60f, 0.02f, 1, null),

    // ── Issue #926: Tony's Chip Shop ──────────────────────────────────────────

    /**
     * The serving counter at Tony's Chip Shop (CHIPPY landmark).
     * Interact (E) to open the {@code ChippyOrderUI} and purchase food items:
     * CHIPS, BATTERED_SAUSAGE, CHIP_BUTTY, MUSHY_PEAS, PICKLED_EGG, FISH_SUPPER,
     * SALT_AND_VINEGAR_PACKET, and BOTTLE_OF_WATER. Only available during opening
     * hours 11:00–00:00. Destroyed by 8 punches; yields COUNTER material.
     */
    CHIPPY_COUNTER(1.40f, 1.10f, 0.80f, 8, Material.COUNTER),

    // ── Issue #928: Public Library System ──────────────────────────────────────

    /**
     * A library bookshelf stocked with books inside Northfield Library.
     * Interact (E) to start a 60-second reading session that awards +15 XP to a
     * StreetSkill based on the book's content:
     * DIY_MANUAL → CONSTRUCTION, NEGOTIATION_BOOK → TRADING,
     * STREET_LAW_PAMPHLET → STREETWISE, Gardening section → HORTICULTURE.
     * Maximum 3 sessions per in-game day. Destroyed by 5 punches; yields WOOD.
     */
    BOOKSHELF(0.90f, 1.80f, 0.30f, 5, Material.WOOD),

    /**
     * A public internet terminal inside Northfield Library.
     * Interact (E) to access the library internet: scout fence prices (+10 coins price
     * preview), check criminal record summary, or pre-register for JobCentre jobs.
     * Available only during library opening hours. Destroyed by 8 punches; yields SCRAP_METAL.
     */
    INTERNET_TERMINAL(0.60f, 1.50f, 0.60f, 8, Material.SCRAP_METAL),

    /**
     * A reading chair inside Northfield Library.
     * Sitting in a reading chair (press E) doubles the library's Notoriety decay
     * bonus — police give the player a pass if Notoriety &lt; 60 while seated.
     * Destroyed by 3 punches; yields WOOD.
     */
    READING_CHAIR(0.70f, 0.90f, 0.70f, 3, Material.WOOD),

    /**
     * A newspaper stand inside Northfield Library.
     * Interact (E) to collect the free daily newspaper (equivalent to today's
     * NewspaperSystem edition), revealing any active MarketEvent.
     * One free copy per in-game day. Destroyed by 2 punches; yields nothing.
     */
    NEWSPAPER_STAND(0.80f, 1.20f, 0.30f, 2, null),

    // ── Issue #930: Charity Shop System ──────────────────────────────────────

    /**
     * A mystery bag prop on the charity shop counter — a tatty carrier bag
     * stapled shut with unknown contents inside.
     * Interact (E) to purchase for 2 COIN; yields a weighted random item.
     * Maximum 3 mystery bags per day (reduced to 1 for Notoriety Tier 3+ players).
     * Destroyed by 1 punch; yields nothing (it's already empty inside).
     */
    MYSTERY_BAG(0.20f, 0.30f, 0.15f, 1, null),

    // ── Issue #934: Pigeon Racing System ─────────────────────────────────────

    /**
     * Pigeon loft — a handbuilt wooden structure housing up to 3 racing pigeons.
     * Crafted from 8 WOOD + 2 PLANKS; placed on the squat roof or allotment.
     * Interact (E) to open the PigeonLoftUI: train pigeons, enter races, check morale.
     * Frost weather damages the loft (−10 condition per frost period via WeatherSystem).
     * Destroyed by 8 punches; yields WOOD.
     */
    PIGEON_LOFT(1.50f, 1.20f, 1.00f, 8, Material.WOOD),

    // ── Issue #940: Wheelie Bin Fire System ──────────────────────────────────

    /**
     * A dark grey wheelie bin prop. Solid (1×1×1.5 blocks). Found outside terraced
     * houses and in clusters at the industrial estate and off-licence. Can be ignited
     * with a PETROL_CAN or by a YOUTH_GANG NPC at night.
     * Destroyed by 8 punches (HARD material); yields SCRAP_METAL.
     */
    WHEELIE_BIN(1.00f, 1.50f, 1.00f, 8, Material.SCRAP_METAL),

    /**
     * A burning wheelie bin. Replaces WHEELIE_BIN when ignited. Non-solid (fire
     * hazard cosmetic only). Emits flickering orange point light and smoke particles.
     * Provides campfire-level warmth within 5 blocks. Cannot be punched; interact
     * with FIRE_EXTINGUISHER (E) to extinguish early.
     */
    BURNING_BIN(1.00f, 1.50f, 1.00f, 0, null),

    /**
     * A burnt-out wheelie bin husk. Replaces BURNING_BIN after extinguishing or
     * natural burnout. Cosmetic ruin prop; no light. Destroyed by 1 punch; drops
     * 1 SCRAP_METAL.
     */
    BURNT_BIN(1.00f, 1.50f, 1.00f, 1, Material.SCRAP_METAL);

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

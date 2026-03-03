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
    BURNT_BIN(1.00f, 1.50f, 1.00f, 1, Material.SCRAP_METAL),

    // ── Issue #948: Hand Car Wash ─────────────────────────────────────────────

    /**
     * A wall-mounted hose reel with a trigger nozzle. Interaction point for car
     * wash shifts. Press E within 2 blocks to start a shift (08:00–18:00).
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    HOSE_PROP(1.20f, 1.50f, 0.80f, 5, Material.SCRAP_METAL),

    /**
     * A plastic bucket filled with soapy water. Decorative; confirms the forecourt
     * is operational. Destroyed by 2 punches; yields nothing (plastic bucket).
     */
    BUCKET_PROP(0.60f, 0.50f, 0.60f, 2, null),

    /**
     * A metal lockbox containing today's car wash takings (3–9 COIN, randomised
     * per in-game day). Press E to rob it (requires CROWBAR if Notoriety below
     * Tier 2; no tool required at Tier 2+). Destroyed by 8 punches; yields IRON.
     */
    CASH_BOX_PROP(0.40f, 0.50f, 0.30f, 8, Material.IRON),

    // ── Issue #950: Northfield Leisure Centre ─────────────────────────────────

    /**
     * A leisure centre vending machine stocked with ENERGY_DRINK (2 COIN),
     * CHOCOLATE_BAR (2 COIN), and WATER_BOTTLE (1 COIN).
     * Press E to open the vending menu. Destroyed by 8 punches; yields SCRAP_METAL.
     */
    VENDING_MACHINE_PROP(0.70f, 1.80f, 0.50f, 8, Material.SCRAP_METAL),

    /**
     * The leisure centre changing room cubicle area.
     * Player within 5 blocks triggers changing room rumour gossip seeded into
     * the RumourNetwork every 60 real seconds. Non-solid (cosmetic zone marker).
     * Destroyed by 3 punches; yields nothing.
     */
    CHANGING_ROOM_PROP(1.20f, 2.10f, 0.10f, 3, null),

    /**
     * A broken sauna unit — has been Out of Order since 2009.
     * Interact (E) to read the out-of-order notice.
     * Always non-functional; triggers TYPICAL achievement on first interaction.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    SAUNA_PROP(1.80f, 2.20f, 1.80f, 5, Material.SCRAP_METAL),

    /**
     * The fire exit door of the leisure centre.
     * Press E to attempt a silent sneak-in. Silent if no NPC is within 8 blocks;
     * otherwise caught: +8 Notoriety, +1 wanted star. Awards TIGHT_FISTED
     * achievement on first successful sneak-in.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    FIRE_EXIT_PROP(0.20f, 2.10f, 1.00f, 5, Material.SCRAP_METAL),

    // ── Issue #954: Northfield Bingo Hall ─────────────────────────────────────

    /**
     * Bingo caller's podium — a wooden lectern at the front of the bingo hall.
     * The CALLER NPC stands here and announces numbers during sessions.
     * Destroyed by 5 punches; yields WOOD.
     */
    BINGO_CALLER_PODIUM_PROP(0.60f, 1.20f, 0.60f, 5, Material.WOOD),

    /**
     * Bingo prize board — a wall-mounted board showing the current top prize.
     * TRADING Tier 2 perk: press E to peek at the next 3 called numbers.
     * Destroyed by 3 punches; yields nothing.
     */
    PRIZE_BOARD_PROP(0.10f, 1.00f, 0.80f, 3, null),

    /**
     * Bingo hall refreshment counter — sells TEA_CUP and BISCUIT at 1 COIN each.
     * Press E while within 2 blocks to buy refreshments during a session.
     * Destroyed by 5 punches; yields WOOD.
     */
    REFRESHMENT_COUNTER_PROP(1.20f, 1.00f, 0.60f, 5, Material.WOOD),

    /**
     * Bingo trophy prop — placed in the player's nearest squat on FULL HOUSE win.
     * A small golden cup with "LUCKY STARS" engraved on the plinth.
     * Destroyed by 3 punches; yields BINGO_TROPHY material.
     */
    BINGO_TROPHY_PROP(0.30f, 0.40f, 0.30f, 3, Material.BINGO_TROPHY),

    // ── Issue #965: Northfield Snooker Hall ───────────────────────────────────

    /**
     * Snooker table prop — a full-size 4×2 block green baize snooker table with
     * wooden surround. Collidable; player cannot walk through it.
     * Destroyed by 10 punches; yields WOOD×2.
     * Used for the snooker mini-game triggered by pressing E while adjacent.
     */
    SNOOKER_TABLE_PROP(4.00f, 0.90f, 2.00f, 10, Material.WOOD),

    // ── Issue #971: The Rusty Anchor Wetherspoons ──────────────────────────────

    /**
     * Newspaper rack at The Rusty Anchor — free to read.
     * Player interaction picks up 1 NEWSPAPER (if none in inventory).
     */
    NEWSPAPER_RACK_PROP(0.40f, 1.20f, 0.20f, 3, Material.NEWSPAPER),

    /**
     * Television set on the wall — activates for Football on the Telly atmospheric event.
     * Seated PUBLIC NPCs face it when active.
     */
    TELEVISION_PROP(0.80f, 0.60f, 0.15f, 3, Material.SCRAP_METAL),

    // ── Issue #977: Northfield Amusement Arcade ───────────────────────────────

    /**
     * Arcade machine — an upright video-game cabinet (genre: FIGHTER, RACER, or SHOOTER).
     * Costs TWOPENCE to play. Player can tamper with SCREWDRIVER for free plays.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    ARCADE_MACHINE_PROP(0.70f, 1.90f, 0.60f, 8, Material.SCRAP_METAL),

    /**
     * Penny falls machine — a 2p coin cascade gambling machine.
     * Press E to spend 1 TWOPENCE per push; press F to shove (25% tilt chance).
     * Each machine has a seeded jackpot threshold (50–200 pushes) paying 30 TWOPENCE.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    PENNY_FALLS_PROP(0.80f, 1.60f, 0.80f, 8, Material.SCRAP_METAL),

    /**
     * Claw machine — a prize-grabber cabinet with plush toy prizes.
     * Costs 2 TWOPENCE per attempt; 3-second timed steering.
     * Two prizes per machine; restocked daily by Kevin.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    CLAW_MACHINE_PROP(1.00f, 2.00f, 1.00f, 8, Material.SCRAP_METAL),

    /**
     * Change machine — converts 1 COIN → 5 TWOPENCE tokens.
     * Press E to exchange. Player with SCREWDRIVER can extract COIN (THEFT crime).
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    CHANGE_MACHINE_PROP(0.60f, 1.80f, 0.60f, 8, Material.SCRAP_METAL),

    /**
     * Redemption counter — staffed by Kevin; exchanges accumulated TWOPENCE for prizes.
     * Token exchanges: 10→PLUSH_TOY, 20→ARCADE_TOKEN, 50→WOOLLY_HAT_ECONOMY, 100→SCRATCH_CARD×3.
     * Destroyed by 5 punches; yields WOOD.
     */
    REDEMPTION_COUNTER_PROP(1.20f, 1.00f, 0.60f, 5, Material.WOOD),

    // ── Issue #981: Council Estate — Tower Block, Lifts & Stair-Climbing Misery ─

    /**
     * Lift prop — a 2×2×3 metal box with a door face in the council flat lobby.
     * Press E to call the lift or select a destination floor while inside.
     * Managed by CouncilFlatsSystem.
     */
    LIFT_PROP(2.00f, 3.00f, 2.00f, 20, Material.SCRAP_METAL),

    /**
     * Satellite dish — mounted on the roof of council tower blocks.
     * Press E to attempt dish interference (50% chance to reduce PirateRadioSystem
     * signal by 30% for 5 in-game minutes). Adds +1 Notoriety regardless.
     */
    SATELLITE_DISH_PROP(1.00f, 0.80f, 1.00f, 5, Material.SCRAP_METAL),

    /**
     * Graffiti wall prop — flavour graffiti painted on stairwell concrete walls.
     * Spawns in stairwells with 10% probability per floor. Not the player's own tag.
     */
    GRAFFITI_WALL_PROP(0.80f, 0.60f, 0.02f, 1, null),

    // ── Issue #985: Northfield Police Station ─────────────────────────────────

    /**
     * Property bag cupboard — a locked metal cupboard in the custody suite.
     * Holds all items confiscated from the player during arrest processing.
     * Items are returned to inventory on bail payment or morning release.
     * Accessible only with a CUSTODY_KEY_CARD via the evidence locker,
     * or automatically on standard release.
     */
    PROPERTY_BAG_CUPBOARD_PROP(0.70f, 1.80f, 0.50f, 15, Material.SCRAP_METAL),

    /**
     * Fingerprint pad — an ink pad on the custody suite processing desk.
     * Player is placed at this prop during the fingerprinting interaction (3 seconds).
     * Triggers the BANG_TO_RIGHTS achievement on first use.
     */
    FINGERPRINT_PAD_PROP(0.40f, 0.05f, 0.30f, 3, null),

    /**
     * Cell door — a heavy barred door on each custody cell.
     * Impassable until a release condition is met (bail, morning, or lockpick).
     * Can be picked with a LOCKPICK (5-second interaction); adds ESCAPE_FROM_CUSTODY
     * to CriminalRecord and triggers WantedSystem +3 stars on success.
     */
    CELL_DOOR_PROP(0.10f, 2.20f, 1.00f, 25, Material.SCRAP_METAL),

    /**
     * Cell telephone — a wall-mounted telephone inside each custody cell.
     * Press E to make a single phone call (one per arrest): reduces bail by 5 COIN.
     * Awards the ONE_PHONE_CALL achievement on first use.
     */
    TELEPHONE_PROP(0.20f, 0.25f, 0.15f, 3, Material.SCRAP_METAL),

    /**
     * Keypad — electronic door entry panel beside the evidence locker.
     * Requires a valid CUSTODY_KEY_CARD (press E while holding one).
     * 3-second interaction; opens the evidence locker door for 30 seconds.
     * Triggers EVIDENCE_TAMPERING crime + WantedSystem +2 stars if a
     * DETECTIVE or DETENTION_OFFICER is within 8 blocks.
     */
    KEYPAD_PROP(0.15f, 0.20f, 0.08f, 5, null),

    /**
     * Evidence shelf — a steel shelving unit inside the evidence locker.
     * Press E to loot: returns all of the player's own confiscated items and
     * may yield a random STOLEN_PHONE or DRUGS_EVIDENCE bonus.
     * Looting while witnessed triggers EVIDENCE_TAMPERING.
     */
    EVIDENCE_SHELF_PROP(0.60f, 2.00f, 0.40f, 10, Material.SCRAP_METAL),

    /**
     * Waiting bench — a fixed wooden bench in the police station reception.
     * Purely decorative; NPCs (WAITING_PATIENT type) may sit on it.
     */
    WAITING_BENCH_PROP(1.80f, 0.50f, 0.60f, 5, Material.WOOD),

    /**
     * Interview table — a scarred laminate table in the interview room.
     * A tape recorder prop sits on top. Interacting (E) plays flavour dialogue.
     * Cannot be removed by the player.
     */
    INTERVIEW_TABLE_PROP(1.60f, 0.80f, 0.80f, 8, Material.WOOD),

    // ── Issue #998: Northfield Aldi Supermarket ────────────────────────────────

    /**
     * Aldi checkout counter — staffed by Bev (SHOP_ASSISTANT).
     * Press E to pay for basket contents; each item's price is deducted from
     * COIN. During yellow-sticker hour (19:00–21:00) all prices are 0 COIN.
     * Destroyed by 8 punches; yields COUNTER material.
     */
    CHECKOUT_PROP(1.40f, 1.10f, 0.80f, 8, Material.COUNTER),

    /**
     * Aldi self-checkout terminal near the exit.
     * Press E to attempt unmanned checkout: 40% chance of "Unexpected item in
     * bagging area" alert (empties basket, alerts Bev and Dave); 60% silent
     * success (items move to inventory at full price).
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    SELF_CHECKOUT_PROP(0.70f, 1.60f, 0.60f, 8, Material.SCRAP_METAL),

    // ── Issue #1002: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Fuel pump dispenser on the BP petrol station forecourt.
     * Press E (while holding empty PETROL_CAN or empty-handed) to fill up for 3 COIN.
     * Leaving the forecourt without paying sets the pump-and-walk flag and triggers
     * theft mechanics (+5 Notoriety, PETROL_THEFT crime, cashier CHASING).
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    FUEL_PUMP_PROP(0.60f, 1.80f, 0.60f, 8, Material.SCRAP_METAL),

    /**
     * Wall-mounted CCTV camera on the kiosk exterior facing the forecourt.
     * If active (not destroyed), crimes on the forecourt (pump theft, assault) are
     * logged via {@code WitnessSystem.recordCCTVEvent}, incrementing cctvHeatLevel
     * by 10 per event. At heat ≥ 50, a POLICE NPC spawns 60s later.
     * Destroyed by 2 hits (FRAGILE); prevents CCTV logging when broken. +3 Notoriety.
     */
    CCTV_PROP(0.25f, 0.20f, 0.30f, 2, Material.SCRAP_METAL),

    // ── Issue #1004: Northfield Community Centre ───────────────────────────────

    /**
     * Community noticeboard — press E to browse 3–5 weekly rotating notices drawn
     * from {@code RumourNetwork} and {@code BuildingQuestRegistry}.
     * Reading counts rumours as heard. Faction graffiti overlay reflects territory scores.
     * If {@code WantedSystem.getWantedStars() >= 2}, the board displays a WANTED notice.
     * Destroyed by 3 punches; yields WOOD.
     */
    NOTICEBOARD_PROP(0.10f, 1.20f, 0.05f, 3, Material.WOOD),

    /**
     * Boxing bag — press E during boxing club hours (Mon/Wed/Fri 18:00–21:00) to train.
     * Each 30-second session awards +1 {@code StreetSkillSystem.Skill.BOXING} point.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    BOXING_BAG_PROP(0.40f, 1.20f, 0.40f, 8, Material.SCRAP_METAL),

    /**
     * Boxing ring — press E at BOXING skill ≥ 2 to start a sparring session with a
     * STREET_LAD NPC. Win: +5 COIN and +3 STREET_LADS Faction Respect.
     * Destroyed by 12 punches; yields WOOD.
     */
    BOXING_RING_PROP(4.00f, 0.10f, 4.00f, 12, Material.WOOD),

    /**
     * Donation box — drag items from inventory to donate. Donating 3+ items on a
     * Saturday: −2 Notoriety, +5 FOOD_BANK Respect. Tooltip: "You're not all bad."
     * Destroyed by 5 punches; yields CARDBOARD.
     */

    // ── Issue #1008: St. Mary's Church ────────────────────────────────────────

    /**
     * Pulpit — wooden lectern where Reverend Dave stands during services.
     * Press E during service to receive a blessing (Notoriety −2).
     * Destroyed by 6 punches; yields WOOD.
     */
    PULPIT_PROP(0.80f, 1.20f, 0.80f, 6, Material.WOOD),

    /**
     * Church pew — long bench where the congregation sits.
     * Player can sit (press E) for +1 Warmth/min bonus during service.
     * Destroyed by 5 punches; yields WOOD.
     */
    PEW_PROP(2.00f, 0.80f, 0.50f, 5, Material.WOOD),

    /**
     * Collection plate — circulated 10:30–11:00 Sunday.
     * Press E to donate (Notoriety −1/COIN, max −3) or steal (Notoriety +10, +1 wanted star if witnessed).
     * Destroyed by 1 punch; yields SCRAP_METAL.
     */
    COLLECTION_PLATE_PROP(0.30f, 0.05f, 0.30f, 1, Material.SCRAP_METAL),

    /**
     * Confession booth — wooden confessional; press E to eavesdrop.
     * Hear one random rumour from RumourNetwork.
     * Destroyed by 6 punches; yields WOOD.
     */
    CONFESSION_BOOTH_PROP(1.00f, 2.00f, 1.00f, 6, Material.WOOD),

    /**
     * Bell rope — hanging rope in the bell tower; press E to ring the bell.
     * Causes NoiseSystem noise level 5. All NPCs within 40 blocks → INVESTIGATING.
     * Night ringing (23:00–06:00): +5 Notoriety, ANTISOCIAL_BEHAVIOUR, +1 wanted star.
     * Destroyed by 2 punches; yields CLOTH.
     */
    BELL_ROPE_PROP(0.10f, 2.00f, 0.10f, 2, Material.CLOTH),

    // ── Issue #1012: Skin Deep Tattoos ────────────────────────────────────────
    /** Reclining tattoo chair — Kev works on the player here. */
    TATTOO_CHAIR_PROP(0.80f, 1.00f, 1.80f, 5, Material.SCRAP_METAL),
    /** Wall-mounted flash sheet showing tattoo designs. */
    FLASH_SHEET_PROP(0.05f, 0.80f, 0.60f, 1, null),
    /** Tattoo station workbench with inks and equipment. */
    TATTOO_STATION_PROP(1.00f, 1.00f, 0.60f, 4, Material.NEEDLE),
    /**
     * MIRROR_PROP — wall-mounted mirror. Player can press E near this to attempt
     * a prison tattoo DIY mechanic (requires NEEDLE + INK_BOTTLE).
     * Destroyed by 2 hits; yields GLASS.
     */
    MIRROR_PROP(0.80f, 1.00f, 0.05f, 2, Material.GLASS),
    /**
     * CCTV_CAMERA_PROP — a wall-mounted CCTV camera covering the tattoo parlour
     * display area. If unobstructed and within 8 blocks of a theft event, witnesses
     * the crime (+1 wanted star, THEFT CriminalRecord entry).
     * Destroyed by 2 hits (FRAGILE); yields SCRAP_METAL.
     */
    CCTV_CAMERA_PROP(0.25f, 0.20f, 0.30f, 2, Material.SCRAP_METAL),

    // ── Issue #1110: Skin Deep Tattoos — neon sign ────────────────────────────
    /**
     * TATTOO_SIGN_PROP — neon shop sign mounted outside Skin Deep Tattoos.
     * Visible at night; glows pink/purple. Destroyed by 3 hits; yields SCRAP_METAL.
     * Interacting (E) causes Kev to shout "Oi, that's me sign!"
     */
    TATTOO_SIGN_PROP(0.40f, 0.30f, 0.10f, 3, Material.SCRAP_METAL),

    // ── Issue #1020: Northfield Sporting & Social Club ────────────────────────

    /**
     * DARTBOARD_PROP — wall-mounted bristle dartboard at the social club.
     * Press E to start a darts mini-game (301 countdown, double-out required).
     * NPC challenge mode: challenge a MEMBER for a 5 COIN pot.
     * Destroyed by 3 punches; yields WOOD.
     */
    DARTBOARD_PROP(0.50f, 0.50f, 0.10f, 3, Material.WOOD),

    /**
     * NOTICE_BOARD_PROP — cork notice board in the social club entrance.
     * Displays upcoming quiz night, AGM dates, and club rules.
     * Press E to read. Destroyed by 2 punches; yields WOOD.
     */
    NOTICE_BOARD_PROP(0.80f, 1.00f, 0.10f, 2, Material.WOOD),

    /**
     * MEMBERSHIP_DESK_PROP — Derek's membership desk near the entrance.
     * Press E to apply for membership (requires STREET_LADS Respect ≥ 40 for TEMP,
     * ≥ 60 for FULL, or a guest invite from a MEMBER).
     * Destroyed by 5 punches; yields WOOD.
     */
    MEMBERSHIP_DESK_PROP(1.20f, 0.90f, 0.60f, 5, Material.WOOD),

    /**
     * CLUB_DOOR_PROP — the reinforced front door of the social club.
     * Closed 23:00–11:00. Friday/Saturday: PubLockInSystem lock-in from 23:00.
     * Blocking entry when the player is Wanted Tier 2+ (Keith informs the door).
     * Destroyed by 8 punches; yields WOOD.
     */
    CLUB_DOOR_PROP(1.00f, 2.20f, 0.20f, 8, Material.WOOD),

    /**
     * TROPHY_CABINET_PROP — glass-fronted trophy cabinet in the main room.
     * Contains darts and snooker trophies. Decorative; press E for flavour text.
     * Destroyed by 3 punches; yields GLASS.
     */
    TROPHY_CABINET_PROP(1.20f, 1.80f, 0.40f, 3, Material.GLASS),

    // ── Issue #1022: Northfield GP Surgery ────────────────────────────────────

    /**
     * RECEPTION_DESK_PROP — Brenda's reception desk at the surgery entrance.
     * Press E to book an appointment (1–3 days ahead) or check walk-in availability.
     * Destroyed by 8 punches; yields WOOD.
     */
    RECEPTION_DESK_PROP(1.40f, 1.10f, 0.60f, 8, Material.WOOD),

    /**
     * WAITING_CHAIR_PROP — plastic NHS waiting-room chair (NHS blue).
     * Sitting here (press E) fast-forwards wait time at 10×.
     * Destroyed by 3 punches; yields PLASTIC.
     */
    WAITING_CHAIR_PROP(0.50f, 0.90f, 0.50f, 3, Material.PLASTIC),

    /**
     * EXAMINATION_TABLE_PROP — padded examination table in the consultation room.
     * Decorative; press E for flavour text from Dr. Nair.
     * Destroyed by 6 punches; yields SCRAP_METAL.
     */
    EXAMINATION_TABLE_PROP(1.80f, 0.80f, 0.60f, 6, Material.SCRAP_METAL),

    /**
     * DOCTOR_DESK_PROP — Dr. Nair's desk; press E to begin a consultation.
     * Requires a valid appointment or walk-in window.
     * Destroyed by 5 punches; yields WOOD.
     */
    DOCTOR_DESK_PROP(1.20f, 0.80f, 0.60f, 5, Material.WOOD),

    /**
     * MEDICINE_CABINET_PROP — wall-mounted cabinet in the treatment room.
     * Lockpick required; 4 hits to open; yields PRESCRIPTION_MEDS + ANTIDEPRESSANTS.
     * Triggers Wanted Tier 2, Notoriety +10.
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    MEDICINE_CABINET_PROP(0.60f, 1.40f, 0.30f, 4, Material.SCRAP_METAL),

    /**
     * DRUG_SAFE_PROP — heavy floor safe in the dispensary.
     * Crowbar required; 12 hits; yields PRESCRIPTION_MEDS ×8.
     * Triggers Wanted Tier 3, NoiseSystem alarm, Notoriety +20.
     * Destroyed by 12 punches; yields SCRAP_METAL.
     */
    DRUG_SAFE_PROP(0.50f, 0.80f, 0.40f, 12, Material.SCRAP_METAL),

    /**
     * PHARMACY_HATCH_PROP — sliding window at the dispensary counter.
     * Press E once per in-game week to collect a repeat prescription.
     * Abuse detection (>1/week) → Notoriety +5 and 2-week block.
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    PHARMACY_HATCH_PROP(0.80f, 0.50f, 0.10f, 4, Material.SCRAP_METAL),

    /**
     * LEAFLET_RACK_PROP — wall-mounted NHS leaflet rack in the waiting area.
     * Press E to receive a random collectible LEAFLET item.
     * Destroyed by 2 punches; yields CARDBOARD.
     */
    LEAFLET_RACK_PROP(0.30f, 1.20f, 0.30f, 2, Material.CARDBOARD),

    // ── Issue #1225: Northfield Fast Cash Finance ─────────────────────────────

    /**
     * LOAN_DESK_PROP — Barry's garish counter at Fast Cash Finance.
     * Press E to interact with LOAN_MANAGER NPC (Barry) to apply for or repay a loan.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    LOAN_DESK_PROP(1.20f, 0.90f, 0.60f, 5, Material.SCRAP_METAL),

    // ── Issue #1418: Northfield QuickFix Loans ───────────────────────────────

    /**
     * CASH_DRAWER_PROP — Darren's back-office cash drawer at QuickFix Loans.
     * Hold E for 5 seconds with CROWBAR during Darren's lunch (12:30–13:00) to
     * loot 30–50 COIN. Destroyed by 3 hits; yields SCRAP_METAL.
     */
    CASH_DRAWER_PROP(0.60f, 0.30f, 0.40f, 3, Material.SCRAP_METAL),

    // ── Issue #1026: Northfield Scrapyard ────────────────────────────────────

    /**
     * WEIGH_BRIDGE_PROP — flat metal weighing platform at the scrapyard entrance.
     * Press E to open the sell menu. Destroyed by 8 hits; yields SCRAP_METAL.
     */
    WEIGH_BRIDGE_PROP(2.0f, 0.2f, 2.0f, 8, Material.SCRAP_METAL),

    /**
     * CRUSHER_PROP — large hydraulic industrial press in the south yard.
     * Press E to destroy items permanently (evidence destruction).
     * Running it generates NoiseSystem level 8. Destroyed by 12 hits; yields SCRAP_METAL.
     */
    CRUSHER_PROP(2.0f, 2.5f, 2.0f, 12, Material.SCRAP_METAL),

    /**
     * YARD_OFFICE_PROP — small portacabin near the scrapyard entrance.
     * Gary Pearce works inside 09:00–17:00. Contains CASH_BOX_PROP (12 COIN; requires LOCKPICK).
     * Destroyed by 6 hits; yields WOOD.
     */
    YARD_OFFICE_PROP(3.0f, 2.5f, 4.0f, 6, Material.WOOD),

    /**
     * SCRAP_PILE_PROP — heap of twisted scrap metal scattered around the yard.
     * Each pile yields SCRAP_METAL ×1–3 on 4 hits with any tool. Respawn after 1 in-game day.
     * Destroyed by 4 hits; yields SCRAP_METAL.
     */
    SCRAP_PILE_PROP(1.5f, 1.0f, 1.5f, 4, Material.SCRAP_METAL),

    /**
     * COPPER_BALE_PROP — compressed bale of copper in the locked compound.
     * Yields COPPER_BALE ×1 when broken. Destroyed by 6 hits; yields COPPER_BALE.
     */
    COPPER_BALE_PROP(1.2f, 1.0f, 0.8f, 6, Material.COPPER_BALE),

    /**
     * DOG_KENNEL_PROP — Tyson the Rottweiler's night base.
     * Tyson is unleashed 20:00–07:00. Destroyed by 3 hits; yields WOOD.
     */
    DOG_KENNEL_PROP(1.2f, 1.0f, 1.0f, 3, Material.WOOD),

    /**
     * COPPER_PIPE_PROP — exposed copper pipe fitting on an industrial wall.
     * Remove with SCREWDRIVER (5 seconds) to yield COPPER_WIRE ×2.
     * Destroyed by 5 hits; yields SCRAP_METAL.
     */
    COPPER_PIPE_PROP(0.3f, 0.3f, 0.6f, 5, Material.SCRAP_METAL),

    // ── Issue #1028: Northfield Cash Converters ───────────────────────────────

    /**
     * COUNTER_PROP — the Cash Converters service counter.
     * Dean stands behind it during opening hours (09:00–17:30 Mon–Sat).
     * Press E to open the CashConvertersUI for buying/selling electronics.
     * Destroyed by 8 punches; yields COUNTER material.
     */
    COUNTER_PROP(1.40f, 1.10f, 0.80f, 8, Material.COUNTER),

    /**
     * DISPLAY_CASE_PROP — a locked glass display case showing high-value stock.
     * Smash (2 hits, fragile glass) to grab contents without payment.
     * Triggers WitnessSystem THEFT crime and +8 Notoriety.
     * Destroyed by 2 hits; yields GLASS + 1 random GAMES_CONSOLE / LAPTOP / TABLET.
     */
    DISPLAY_CASE_PROP(1.20f, 1.20f, 0.50f, 2, Material.GLASS),

    /**
     * BACK_ROOM_DOOR_PROP — reinforced door to the Cash Converters back room.
     * Accessible only with LOCKPICK (5-second interact) or during shift work.
     * Back room contains SAFE_PROP with day's takings.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    BACK_ROOM_DOOR_PROP(1.00f, 2.20f, 0.20f, 8, Material.SCRAP_METAL),

    // ── Issue #1286: Northfield Cash Converters (additional props) ─────────────

    /**
     * CASH_CONVERTER_COUNTER_PROP — the main Cash Converters shop counter.
     * Dean stands behind it during opening hours (09:00–17:30 Mon–Sat).
     * Press E to open the sell/buy UI. Crowbar for 5 seconds after hours to break in.
     * Destroyed by 8 hits; yields WOOD.
     */
    CASH_CONVERTER_COUNTER_PROP(1.5f, 1.1f, 0.6f, 8, Material.WOOD),

    // ── Issue #1030: Al-Noor Mosque ────────────────────────────────────────────

    /**
     * Prayer mat — a small woven mat laid on the mosque floor.
     * WORSHIPPER NPCs kneel on these during the five daily prayers and Friday Jumu'ah.
     * Player may use one (press E) during prayer for +1 Community Respect per minute.
     * Destroyed by 2 punches; yields CLOTH.
     */
    PRAYER_MAT_PROP(0.60f, 0.02f, 0.90f, 2, Material.CLOTH),

    /**
     * Mihrab — the ornate prayer niche in the mosque's qibla wall, indicating the
     * direction of Mecca. The Imam leads prayers here. Player must be within 6 blocks
     * of this prop for the full Friday Jumu'ah attendance bonus.
     * Destroyed by 8 punches; yields STONE.
     */
    MIHRAB_PROP(1.00f, 2.40f, 0.30f, 8, Material.STONE),

    /**
     * Shoe rack — a wooden rack beside the mosque entrance where shoes are left.
     * Player must interact (E) to remove shoes before entering; sanctuary mechanic
     * and Jumu'ah attendance reward both require shoes removed.
     * Destroyed by 3 punches; yields WOOD.
     */
    SHOE_RACK_PROP(1.00f, 1.00f, 0.30f, 3, Material.WOOD),

    /**
     * Ablution tap — wall-mounted tap in the wudu room for ritual washing.
     * Press E to use: +3 Warmth, removes DIRTY status. Always accessible.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    ABLUTION_TAP_PROP(0.20f, 0.50f, 0.20f, 5, Material.SCRAP_METAL),

    /**
     * Outdoor tap — exterior tap on the mosque wall, accessible at all times.
     * Press E to drink: +10 Thirst. No conditions required.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    OUTDOOR_TAP_PROP(0.20f, 0.40f, 0.20f, 5, Material.SCRAP_METAL),

    /**
     * Takings box — a wooden collection box for mosque donations near the entrance.
     * Donate COIN (press E while holding): +5 Community Respect per 2 COIN donated.
     * Rob it (punch to break): yields 5–15 COIN but triggers COMMUNITY_OUTRAGE rumour,
     * +3 Notoriety, −20 Community Respect, THEFT_FROM_PLACE_OF_WORSHIP crime, and
     * permanent sanctuary revocation. Destroyed by 5 punches; yields WOOD.
     */
    TAKINGS_BOX_PROP(0.30f, 0.50f, 0.30f, 5, Material.WOOD),

    /**
     * Fold table — a portable folding table set up at Maghrib during Ramadan for the
     * Iftar meal. Carries DATE_FRUIT×3, FLATBREAD×1, SOUP_CUP×1 (once per session).
     * Seeds IFTAR_TONIGHT rumour to nearby NPCs when placed.
     * Destroyed by 3 punches; yields WOOD.
     */
    FOLD_TABLE_PROP(1.80f, 0.90f, 0.90f, 3, Material.WOOD),

    // ── Issue #1037: Northfield Indoor Market ─────────────────────────────────

    /**
     * Market Shutter — a rolling metal shutter across the indoor market entrance.
     * Impassable state when market is closed (outside Tue/Fri/Sat 08:00–16:00).
     * Raised state during market hours; player can walk through.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    MARKET_SHUTTER_PROP(3.00f, 2.50f, 0.20f, 8, Material.SCRAP_METAL),

    /**
     * Tea Urn — Brenda's large stainless-steel tea urn at her hot food stall.
     * Press E to receive a MUG_OF_TEA (1 COIN; free during rain).
     * Brenda says: "It's Baltic out there, love." during rain.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    TEA_URN_PROP(0.40f, 0.70f, 0.40f, 5, Material.SCRAP_METAL),

    // ── Issue #1039: Northfield Barber ────────────────────────────────────────

    /**
     * Barber Chair — the padded hydraulic chair where the customer sits for a cut.
     * Two placed inside Kosta's Barbers. Press E while adjacent to book a cut.
     * Destroyed by 6 punches; yields SCRAP_METAL.
     */
    BARBER_CHAIR_PROP(0.70f, 1.20f, 0.70f, 6, Material.SCRAP_METAL),

    /**
     * Barber Pole — the iconic red-and-white rotating striped pole mounted outside.
     * Decorative; signals the shop is open when it rotates.
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    BARBER_POLE_PROP(0.20f, 1.80f, 0.20f, 3, Material.SCRAP_METAL),

    /**
     * Waiting Bench — a wooden bench for customers awaiting their turn.
     * Seats up to 3 NPC customers. Destroyed by 3 punches; yields WOOD.
     */
    BARBER_WAITING_BENCH_PROP(1.40f, 1.00f, 0.60f, 3, Material.WOOD),

    // ── Issue #1041: Northfield Argos ─────────────────────────────────────────

    /**
     * Argos Catalogue — the iconic laminated catalogue on a fixed pedestal.
     * Press E to browse the 12-item catalogue, write a slip, and hand it to
     * the clerk. Destroyed by 5 punches; yields SCRAP_METAL.
     * Multiple units are placed around the shop floor.
     */
    ARGOS_CATALOGUE_PROP(0.50f, 1.10f, 0.50f, 5, Material.SCRAP_METAL),

    /**
     * Argos Returns Desk — the dedicated returns counter at the back of the shop.
     * Press E to initiate a returns transaction with the ARGOS_CLERK.
     * Success rate: 60% own purchase, 30% others', 5% stolen goods.
     * Stolen return: 30% chance clerk notices → ARGOS_MANAGER spawns.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    ARGOS_RETURNS_DESK_PROP(1.20f, 1.00f, 0.60f, 8, Material.SCRAP_METAL),

    /**
     * Argos Plastic Seat — a moulded orange plastic seat bolted to the floor.
     * 3–5 placed in the waiting area. Sitting in one speeds up the order wait
     * timer and restores 1 Warmth/minute. NPCs seated here gossip (LOCAL_EVENT).
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    ARGOS_PLASTIC_SEAT_PROP(0.60f, 0.90f, 0.60f, 4, Material.SCRAP_METAL),

    /**
     * Argos Collection Counter — the main service counter where clerks retrieve
     * orders and call out collection numbers. Press E when adjacent to hand in
     * a slip or collect a completed order.
     * Destroyed by 6 punches; yields SCRAP_METAL.
     */
    ARGOS_COUNTER_PROP(2.00f, 1.00f, 0.80f, 6, Material.SCRAP_METAL),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1043: Northfield Fire Station props
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BAY_DOOR — rollup garage door for the fire engine bay.
     * Double-width. Opens/closes via FireStationSystem state.
     * Destroyed by 10 punches; yields SCRAP_METAL.
     */
    BAY_DOOR(2.40f, 2.60f, 0.20f, 10, Material.SCRAP_METAL),

    /**
     * FIRE_ENGINE_PROP — the red fire engine parked in Bay 1.
     * Press E to attempt the Engine Heist (FireStationSystem handles interaction).
     * Not destructible by normal means; yields null.
     */
    FIRE_ENGINE_PROP(3.00f, 2.00f, 7.00f, 99, null),

    /**
     * HOSE_REEL — wall-mounted hose reel in the station.
     * Press E to pick up FIRE_EXTINGUISHER (theft if FIREFIGHTER within 6 blocks).
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    HOSE_REEL(0.60f, 1.00f, 0.40f, 4, Material.SCRAP_METAL),

    /**
     * LOCKER — personal locker in the crew room.
     * Press E to search; 50% chance yields FIREFIGHTER_HELMET.
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    LOCKER(0.60f, 1.80f, 0.50f, 4, Material.SCRAP_METAL),

    /**
     * WATCH_ROOM — elevated glass booth where the Watch Commander is stationed.
     * Non-interactive prop (scene dressing). Destroyed by 6 punches; yields GLASS.
     */
    WATCH_ROOM(2.40f, 2.00f, 2.40f, 6, Material.GLASS),

    /**
     * FIRE_POLE — connects upper crew quarters to the appliance bay.
     * Scene dressing / traversal prop. Destroyed by 5 punches; yields SCRAP_METAL.
     */
    FIRE_POLE(0.30f, 3.00f, 0.30f, 5, Material.SCRAP_METAL),

    /**
     * RECEPTION_DESK (fire station) — front-desk area inside the station entrance.
     * Press E to attempt interaction. Destroyed by 8 punches; yields WOOD.
     * (Separate from RECEPTION_DESK_PROP used by the GP Surgery.)
     */
    FIRE_STATION_RECEPTION_DESK(1.40f, 1.10f, 0.60f, 8, Material.WOOD),

    // Issue #1053: Northfield Ladbrokes — BettingShopSystem props
    /**
     * FOBT Terminal — interactive Fixed-Odds Betting Terminal in the corner of Ladbrokes.
     * Press E to play virtual roulette. Drops SCRAP_METAL when broken.
     */
    FOBT_TERMINAL(0.70f, 1.60f, 0.50f, 8, Material.SCRAP_METAL),

    /**
     * Racing Post prop — the folded Racing Post on the betting shop counter.
     * Press E to read today's horse form guide; grants +5% payout bonus on next horse bet.
     */
    RACING_POST_PROP(0.40f, 0.05f, 0.30f, 2, Material.RACING_POST),

    // Issue #1055: Northfield War Memorial — StatueSystem
    /**
     * Placard Prop — sign board held or placed by protestors at the war memorial.
     * Player can pick it up for the PLACARD_PINCHER achievement.
     * Drops: Material.PLACARD (fenceable for 2 COIN).
     */
    PLACARD_PROP(0.30f, 1.20f, 0.05f, 2, Material.PLACARD),

    // ── Issue #1063: Northfield Social Club ───────────────────────────────────

    /** Performance stage at the Social Club — Diamond Dave performs here on Saturday nights. */
    STAGE_PROP(3.00f, 0.50f, 2.00f, 6, Material.WOOD),

    /** Dominoes table — PENSIONER and COUNCIL_BUILDER NPCs gather here Tue/Thu 19:00–22:00. */
    DOMINOES_TABLE_PROP(1.20f, 0.80f, 0.70f, 4, Material.WOOD),

    /** Club noticeboard — displays committee notices, raffle results, and event schedules. */
    CLUB_NOTICEBOARD_PROP(0.10f, 1.20f, 0.05f, 3, Material.WOOD),

    /** Bar hatch — the serving counter at the Social Club bar. */
    BAR_HATCH_PROP(1.50f, 1.00f, 0.10f, 5, Material.WOOD),

    // ── Issue #1073: Northfield Cemetery ──────────────────────────────────────

    /**
     * Grave plot — a diggable 1×1 dirt mound at each headstone location in the
     * cemetery. 8 SPADE hits convert it to {@code OPEN_GRAVE_PROP} and yield a
     * loot roll. Rain extends this to 10 hits.
     */
    GRAVE_PLOT_PROP(0.80f, 0.20f, 0.80f, 8, null),

    /**
     * Open grave — the disturbed state of a {@code GRAVE_PLOT_PROP} after the
     * player has fully dug it out. No further loot; cosmetic open-pit appearance.
     */
    OPEN_GRAVE_PROP(0.80f, 0.20f, 0.80f, 99, null),

    /**
     * Groundskeeper's shed — a small 2×2 wooden shed near the cemetery north wall.
     * Contains a SPADE item. Breakable when Vernon is absent; breaking it while
     * Vernon is present triggers a police call.
     */
    GROUNDSKEEPER_SHED_PROP(2.00f, 2.00f, 2.00f, 8, Material.SPADE),

    /**
     * Lych gate — the decorative entrance gate at the cemetery south face.
     * Cosmetic only; always passable by the player.
     */
    LYCH_GATE_PROP(2.00f, 2.50f, 0.30f, 6, Material.WOOD),

    /**
     * Fresh flowers — dropped by MOURNER NPCs after a funeral procession ends.
     * Despawns after 2 in-game days. Not sellable.
     */
    FRESH_FLOWERS_PROP(0.30f, 0.20f, 0.30f, 1, null),

    // ── Issue #1077: Northfield Chinese Takeaway — Golden Palace ─────────────

    /**
     * Chinese lantern — a red-and-gold decorative lantern above the Golden Palace door.
     * Always lit (emits ambient warmth glow). Decorative only.
     * Destroyed by 2 punches; yields SCRAP_METAL.
     */
    CHINESE_LANTERN_PROP(0.30f, 0.60f, 0.30f, 2, Material.SCRAP_METAL),

    /**
     * Menu board — a laminated picture menu mounted on the wall behind the counter.
     * Press E to read the full menu (tooltip). Decorative; cannot be removed.
     * Destroyed by 5 punches; yields WOOD.
     */
    MENU_BOARD_PROP(0.80f, 1.20f, 0.05f, 5, Material.WOOD),

    /**
     * Serving hatch — the partition separating kitchen from front-of-house.
     * Cosmetic only; impassable. Mr. Chen passes food through it.
     * Destroyed by 8 punches; yields WOOD.
     */
    SERVING_HATCH_PROP(1.60f, 1.20f, 0.15f, 8, Material.WOOD),

    /**
     * Kebab flyer — a paper flyer dropped by the rival DELIVERY_DRIVER NPC during late-night events.
     * Picking it up reveals Sultan's Kebab on the map if undiscovered.
     * Destroyed by 1 punch; yields nothing.
     */
    KEBAB_FLYER_PROP(0.20f, 0.01f, 0.30f, 1, null),

    // ── Issue #1081: Northfield Pet Shop & Vet — Paws 'n' Claws ─────────────

    /**
     * Fish Tank — a large aquarium in Paws 'n' Claws filled with tropical fish.
     * Ambient gurgling noise source. Press E to view fish (flavour text only).
     * Destroyed by 4 punches; yields GLASS.
     */
    FISH_TANK_PROP(1.20f, 0.80f, 0.50f, 4, ragamuffin.building.Material.GLASS),

    /**
     * Animal Cage — stacked wire cages housing rabbits and guinea pigs.
     * Press E to view animals (flavour text). Not breakable in normal play.
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    ANIMAL_CAGE_PROP(0.60f, 0.80f, 0.60f, 3, ragamuffin.building.Material.SCRAP_METAL),

    /**
     * Bird Perch — a tall T-shaped perch stand in the budgie corner of Paws 'n' Claws.
     * Cosmetic; press E for ambient budgie chatter speech line.
     * Destroyed by 2 punches; yields WOOD.
     */
    BIRD_PERCH_PROP(0.30f, 1.60f, 0.30f, 2, ragamuffin.building.Material.WOOD),

    /**
     * Dog Kennel — the pen behind the counter at Paws 'n' Claws.
     * Press E to browse dogs for purchase (opens the dog purchase menu).
     * Interaction triggers PetShopSystem.browseKennel(). Not destroyable.
     */
    PET_SHOP_KENNEL_PROP(1.20f, 1.00f, 1.20f, 999, null),

    /**
     * Consulting Table — the examination table in Northfield Vets.
     * Press E (with dog companion present or RACING_PIGEON in inventory)
     * to request a vet consultation. Not destroyable in normal play.
     * Destroyed by 5 punches; yields WOOD.
     */
    CONSULTING_TABLE_PROP(1.20f, 0.90f, 0.60f, 5, ragamuffin.building.Material.WOOD),

    /**
     * Handwritten Sign — the faded hand-lettered sign outside Paws 'n' Claws.
     * Reads: "Puppies for sale — no time wasters".
     * Cosmetic prop; press E to read full text.
     * Destroyed by 1 punch; yields nothing.
     */
    HANDWRITTEN_SIGN_PROP(0.10f, 0.60f, 0.40f, 1, null),

    // ── Issue #1207: Big Terry's Cabs ─────────────────────────────────────────

    /**
     * Dispatcher hatch — the hatch-window counter Big Terry sits behind.
     * Press E to open the cab-booking menu. Impassable. Destroyed by 6 punches; yields WOOD.
     */
    DISPATCHER_HATCH_PROP(1.20f, 1.00f, 0.80f, 6, Material.WOOD),

    // ── Issue #1091: Northfield Nando's ─────────────────────────────────────

    /**
     * Nando's order counter — the main service counter staffed by Kezia (NANDOS_STAFF).
     * Press E to open NandosOrderUI. Impassable. Destroyed by 8 punches; yields WOOD.
     */
    NANDOS_COUNTER_PROP(2.00f, 1.20f, 0.60f, 8, Material.WOOD),

    /**
     * Nando's restaurant table — a standard 4-seat dining table.
     * Used by NPC customers and the Stag Do group (4 DRUNK NPCs on Saturdays).
     * Destroyed by 5 punches; yields WOOD.
     */
    NANDOS_TABLE_PROP(1.20f, 0.90f, 0.80f, 5, Material.WOOD),

    /**
     * Card machine — the portable card payment terminal at the Nando's counter.
     * 25% chance of DECLINE per transaction; always succeeds on retry.
     * Press E to use jam mechanic (free meal, adds CrimeType.CARD_MACHINE_FRAUD, +8 Notoriety).
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    CARD_MACHINE_PROP(0.15f, 0.25f, 0.10f, 4, Material.SCRAP_METAL),

    /**
     * Hot sauce rack — wall-mounted rack of peri-peri sauce bottles near the counter.
     * Press E to take one PERI_PERI_SAUCE (1 free per visit, NANDOS_STAFF must not be watching).
     * Destroyed by 3 punches; yields nothing.
     */
    HOT_SAUCE_RACK_PROP(0.40f, 0.60f, 0.10f, 3, null),

    /**
     * Peri-Peri sauce slick — a temporary ground prop created when PERI_PERI_SAUCE is thrown.
     * Slows any NPC or player passing over it by 30%. Lifetime: 60 seconds.
     * Destroyed by 1 punch; yields nothing.
     */
    PERI_SAUCE_SLICK(2.00f, 0.05f, 2.00f, 1, null),

    /**
     * Nando's exterior sign — the red illuminated signage above the restaurant door.
     * Decorative; confirms the NANDOS landmark to players.
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    NANDOS_SIGN_PROP(2.00f, 0.60f, 0.10f, 3, Material.SCRAP_METAL),

    /**
     * Nando's manager's office safe — holds 15–40 COIN + TILL_RECEIPT evidence.
     * Requires LOCKPICK (8-second hold-E) to crack.
     * If Dave (NANDOS_MANAGER) is present (12:00–22:00), police are called on interaction.
     * CCTV camera activates at Notoriety Tier 3+. Safe looted: +15 Notoriety + newspaper headline.
     * Destroyed by 12 punches; yields SCRAP_METAL.
     */
    SAFE_PROP(0.50f, 0.80f, 0.40f, 12, Material.SCRAP_METAL),

    // ── Issue #1094: Northfield By-Election ──────────────────────────────────

    /**
     * Blue election poster — Conservative Party. Attached to walls and fences.
     * Tear down with 1 punch → CrimeType.CRIMINAL_DAMAGE, −2 Tory votes.
     * Spray with GraffitiSystem → becomes DEFACED_POSTER_PROP.
     * 3+ destructions triggers a NewspaperSystem headline.
     * Destroyed by 1 punch; yields nothing.
     */
    ELECTION_POSTER_BLUE_PROP(0.60f, 0.80f, 0.05f, 1, null),

    /**
     * Red election poster — Labour Party. Attached to walls and fences.
     * Tear down with 1 punch → CrimeType.CRIMINAL_DAMAGE, −2 Labour votes.
     * Spray with GraffitiSystem → becomes DEFACED_POSTER_PROP.
     * 3+ destructions triggers a NewspaperSystem headline.
     * Destroyed by 1 punch; yields nothing.
     */
    ELECTION_POSTER_RED_PROP(0.60f, 0.80f, 0.05f, 1, null),

    /**
     * Independent candidate election poster — appears if the player registers
     * as Independent via the NOMINATION_FORM. Placed near polling station.
     * Destroyed by 1 punch; yields nothing.
     */
    ELECTION_POSTER_INDEPENDENT_PROP(0.60f, 0.80f, 0.05f, 1, null),

    /**
     * Canvassing table — set up by PARTY_VOLUNTEER during canvassing hours.
     * Press E to volunteer and receive 10 CAMPAIGN_LEAFLET items.
     * Destroyed by 5 punches; yields WOOD.
     */
    CANVASSING_TABLE_PROP(1.20f, 0.90f, 0.60f, 5, Material.WOOD),

    /**
     * Polling station booth — the official voting point.
     * Press E while holding a NOMINATION_FORM to register as Independent candidate.
     * Present during the 5 campaign days only. Destroyed by 8 punches; yields WOOD.
     */
    POLLING_STATION_PROP(1.60f, 2.20f, 0.40f, 8, Material.WOOD),

    /**
     * Ballot box — sealed transparent box containing cast ballots.
     * Hold E for 8 seconds after 18:00 on polling day to steal it.
     * Requires Notoriety ≤ 30 or COUNCIL_JACKET disguise.
     * Grants BALLOT_BOX item, voids election result.
     * Destroyed by 1 punch after theft; yields nothing.
     */
    BALLOT_BOX_PROP(0.40f, 0.60f, 0.40f, 1, null),

    /**
     * Returning Officer's podium — where the RETURNING_OFFICER NPC stands.
     * Press E to speak with the Returning Officer about the Independent candidacy.
     * Present during campaign period. Destroyed by 5 punches; yields WOOD.
     */
    RETURNING_OFFICER_PODIUM_PROP(0.80f, 1.20f, 0.60f, 5, Material.WOOD),

    /**
     * Defaced poster — result of spraying an ELECTION_POSTER_BLUE_PROP or
     * ELECTION_POSTER_RED_PROP with GraffitiSystem.
     * A poster covered in graffiti and slogans.
     * Destroyed by 1 punch; yields nothing.
     */
    DEFACED_POSTER_PROP(0.60f, 0.80f, 0.05f, 1, null),

    // ── Issue #1096: Sunday League Football ──────────────────────────────────

    /**
     * Goal Post — a pair of metal posts with a crossbar.
     * Placed at each end of the pitch by FootballSystem at 09:55.
     * Removed at full-time. Yields SCRAP_METAL if destroyed (5 hits).
     */
    GOAL_POST_PROP(1.0f, 3.0f, 0.2f, 5, Material.SCRAP_METAL),

    /**
     * Corner Flag — a lightweight plastic flag on a flexible post.
     * Placed at each corner of the pitch. Yields nothing if destroyed (1 hit).
     */
    CORNER_FLAG_PROP(0.15f, 1.5f, 0.15f, 1, null),

    /**
     * Centre Circle Marker — a painted centre-circle marker on the pitch.
     * Purely decorative; no collision. Destroyed by 1 punch; yields nothing.
     */
    CENTRE_CIRCLE_PROP(0.5f, 0.05f, 0.5f, 1, null),

    /**
     * Pitch Bookie — an illegal pitch-side betting table run by a STREET_LAD.
     * Player bets 1–5 COIN on Rovers / Draw / Council FC at 2:1 / 3:1 odds.
     * Yields COIN if destroyed (3 hits).
     */
    PITCH_BOOKIE_PROP(1.0f, 1.0f, 0.5f, 3, Material.COIN),

    /**
     * Physio Bag — a sports bag left on the touchline.
     * Yields PARACETAMOL if looted (press E) or destroyed (2 hits).
     */
    PHYSIO_BAG_PROP(0.4f, 0.3f, 0.4f, 2, Material.PARACETAMOL),

    // ── Issue #1098: Northfield Summer Fete ──────────────────────────────────

    /**
     * Tombola Stall — a rotating drum of numbered tickets manned by VOLUNTEER_NPC.
     * Press E + 1 COIN for a random prize. Breaks into 2× WOOD (3 hits).
     */
    TOMBOLA_STALL_PROP(1.5f, 1.2f, 0.8f, 3, Material.WOOD),

    /**
     * Cake Stall — trestle table of home-baked cakes manned by VICAR_NPC.
     * Press E to buy or steal. Breaks into 2× WOOD (3 hits).
     */
    CAKE_STALL_PROP(1.5f, 1.2f, 0.8f, 3, Material.WOOD),

    /**
     * Bric-a-Brac Table — an unmanned table piled with random junk.
     * Press E to rummage. Breaks into 1× WOOD (2 hits).
     */
    BRIC_A_BRAC_PROP(2.0f, 1.0f, 0.6f, 2, Material.WOOD),

    /**
     * Raffle Ticket Stall — press E + 1 COIN per ticket (max 5).
     * Grand draw at 15:00. Hold E for 3s with RIGGED_BARREL to fix draw.
     * Breaks into 1× WOOD (3 hits).
     */
    RAFFLE_TICKET_STALL_PROP(1.2f, 1.2f, 0.8f, 3, Material.WOOD),

    /**
     * Hook-a-Duck — children's game for SCHOOL_KID NPCs.
     * Press E + 1 COIN → 30% chance CUDDLY_TOY win. Made from PLASTIC (4 hits).
     */
    HOOK_A_DUCK_PROP(1.5f, 0.8f, 1.0f, 4, Material.PLASTIC),

    // ── Issue #1100: Northfield Council Flats — Kendrick House ───────────────

    /**
     * Letterbox bank — a row of metal communal letterboxes in the Kendrick House lobby.
     * Spawns 1–3 parcels daily at 08:00. Hold E (1–3 seconds) to steal a parcel.
     * Witnessed theft: police called (Wanted +1), PLAYER_SPOTTED rumour, Notoriety +3.
     * Stolen parcels fence at 60% face value.
     * Integrated with PostOfficeSystem for parcel deliveries.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    LETTERBOX_BANK_PROP(1.20f, 1.60f, 0.20f, 5, Material.SCRAP_METAL),

    /**
     * Flat door — a metal fire-door on each flat in Kendrick House.
     * Can be knocked on (E) to trigger gossip with resident PENSIONER/PUBLIC NPC.
     * Can be lockpicked (LOCKPICK + 5 seconds) to enter when resident absent.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    FLAT_DOOR_PROP(1.00f, 2.20f, 0.15f, 8, Material.SCRAP_METAL),

    /**
     * Communal noticeboard in the Kendrick House lobby/stairwell.
     * Press E to read current notices seeded from RumourNetwork and NewspaperSystem.
     * On housing inspection days, the inspection notice appears here.
     * Destroyed by 3 punches; yields WOOD.
     */
    COMMUNAL_NOTICEBOARD_PROP(0.80f, 1.20f, 0.10f, 3, Material.WOOD),

    // ── Issue #1108: Northfield Sporting & Social Club ────────────────────────

    /**
     * CARD_TABLE_PROP — green baize card table in the back room of the social club.
     * Press E to start a pontoon game (bet 1–10 COIN per hand).
     * Accessible only with MARCHETTI_CREW Respect ≥ 60 or a LOCKPICK.
     * Destroyed by 4 punches; yields WOOD.
     */
    CARD_TABLE_PROP(1.20f, 0.80f, 0.70f, 4, Material.WOOD),

    /**
     * LOCKED_DOOR_PROP — a reinforced interior door to the back room of the social club.
     * Requires MARCHETTI_CREW Respect ≥ 60 or a LOCKPICK to open.
     * Destroyed by 8 punches; yields WOOD.
     */
    LOCKED_DOOR_PROP(1.00f, 2.20f, 0.15f, 8, Material.WOOD),

    /**
     * EVIDENCE_PROP — a sealed envelope containing PROTECTION_LETTER documents.
     * Found in Derek's back-room desk; picking up auto-adds PROTECTION_LETTER to inventory.
     * Non-destructible by design (E to take).
     */
    EVIDENCE_PROP(0.30f, 0.20f, 0.20f, 99, null),

    // ── Issue #1192: Additional Sporting & Social Club props ──────────────────

    /**
     * DARTS_TROPHY_PROP — a scratched silver trophy on the bar shelf above the
     * dartboard. Awarded to the Thursday Darts League winner. Press E to read
     * engraved names. Destroyed by 3 punches; yields SCRAP_METAL.
     */
    DARTS_TROPHY_PROP(0.30f, 0.40f, 0.20f, 3, Material.SCRAP_METAL),

    /**
     * AGM_ACCOUNTS_PROP — a ring-bound booklet of club accounts placed on the
     * committee table during the first-Sunday-of-month AGM. Press E to inspect
     * as Treasurer; embezzlement options displayed if player holds CLUB_MEMBERSHIP_CARD
     * and is standing for Treasurer. Destroyed by 2 punches; yields nothing (paper).
     */
    AGM_ACCOUNTS_PROP(0.25f, 0.30f, 0.15f, 2, null),

    /**
     * BARSTOOL_PROP — a worn vinyl-topped barstool at the Members' Bar. Named
     * regulars (Arthur, Derek, Brenda) are seated here. Press E to start a
     * conversation and receive seeded rumours. Destroyed by 3 punches; yields WOOD.
     */
    BARSTOOL_PROP(0.40f, 0.70f, 0.40f, 3, Material.WOOD),

    // ── Issue #1114: Meredith & Sons Funeral Parlour ──────────────────────────

    /**
     * FUNERAL_PARLOUR_SIGN_PROP — gold-lettered black fascia sign mounted outside
     * Meredith &amp; Sons Funeral Directors. Decorative; press E for "Meredith &amp; Sons
     * Funeral Directors — Discretion Guaranteed."
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    FUNERAL_PARLOUR_SIGN_PROP(1.20f, 0.40f, 0.10f, 3, Material.SCRAP_METAL),

    /**
     * CASKET_PROP — an ornate wooden casket on a viewing table in the viewing room.
     * Contains 3–5 personal effects from the effects table.
     * Press E to open; taking items counts as THEFT_FROM_PERSON (+6 Notoriety, −15 Community Respect).
     * Resets every 3 in-game days with new occupant name and effects.
     * Destroyed by 10 punches; yields WOOD.
     */
    CASKET_PROP(1.80f, 0.70f, 0.70f, 10, Material.WOOD),

    /**
     * VELVET_CURTAIN_PROP — heavy velvet curtain doorway separating the viewing room
     * from the main parlour. Entering the viewing room unescorted counts as TRESPASS.
     * Non-destructible; press E to pass through.
     */
    VELVET_CURTAIN_PROP(1.00f, 2.20f, 0.10f, 99, null),

    /**
     * HEARSE_PROP — a black hearse parked outside 09:00–17:00 weekdays.
     * At STREET_LADS Respect ≥ 50, player can "borrow" it via CarDrivingSystem.
     * Top speed 0.6× normal; police ignore for 30 in-game seconds before pursuing.
     * Destroyed by 15 punches; yields SCRAP_METAL.
     */
    HEARSE_PROP(2.00f, 1.60f, 4.60f, 15, Material.SCRAP_METAL),

    /**
     * HEADSTONE_PROP — a carved stone grave marker in the cemetery.
     * Press E to read a procedurally generated name and date of the deceased.
     * If the player has a matching WILL_LOCATION rumour, a special prompt appears
     * and the GRAVEYARD_SHIFT achievement is unlocked.
     * Destroyed by 8 punches; yields STONE.
     */
    HEADSTONE_PROP(0.60f, 0.80f, 0.20f, 8, Material.STONE),

    /**
     * FLOWER_STAND_PROP — Gerald's retail display near the parlour entrance.
     * Features FUNERAL_FLOWERS, CONDOLENCES_CARD, and MEMORIAL_CANDLE for sale.
     * Destroyed by 3 punches; yields WOOD.
     */
    FLOWER_STAND_PROP(0.80f, 1.20f, 0.50f, 3, Material.WOOD),

    /**
     * LOCKBOX_PROP — a small metal cash box hidden under the floorboards of the
     * deceased's TERRACED_HOUSE. Contains BISCUIT_TIN_SAVINGS (15–35 COIN) and a
     * PROPERTY_DEED. Accessible after receiving a WILL_LOCATION rumour from Dawn.
     * Non-destructible; press E to take.
     */
    LOCKBOX_PROP(0.30f, 0.20f, 0.25f, 99, null),

    /**
     * HIDING_SPOT_PROP — a floorboard trap-door prop in a TERRACED_HOUSE.
     * Press E to reveal the LOCKBOX_PROP underneath.
     * Non-destructible; decorative until revealed.
     */
    HIDING_SPOT_PROP(0.80f, 0.10f, 0.80f, 99, null),

    // ── Issue #1116: Northfield Pharmacy — Day & Night Chemist ───────────────

    /**
     * PHARMACY_SHELF_PROP — rotating display stand of own-brand vitamins and OTC items.
     * Press E (while Shop Assistant &gt; 4 blocks away) to shoplift a random OTC item.
     * 30% base chance Janet notices (+10% per notoriety tier).
     * Destroyed by 3 punches; yields WOOD.
     */
    PHARMACY_SHELF_PROP(0.60f, 1.50f, 0.60f, 3, Material.WOOD),

    /**
     * STOREROOM_DOOR_PROP — locked wooden door at the rear of the pharmacy.
     * Lockpick (4 hits) or CROWBAR (2 hits) to force open.
     * Breaking in: Notoriety +15, WantedSystem Tier 2, NoiseSystem +30.
     * Resets every 3 in-game days.
     * Destroyed by 4 punches; yields WOOD.
     */
    STOREROOM_DOOR_PROP(0.10f, 2.00f, 1.00f, 4, Material.WOOD),

    /**
     * PHARMACY_COUNTER_PROP — OTC sales counter at the front of the shop.
     * Press E to open the over-the-counter purchase menu.
     * Destroyed by 5 punches; yields WOOD.
     */
    PHARMACY_COUNTER_PROP(1.50f, 1.00f, 0.50f, 5, Material.WOOD),

    /**
     * PHARMACY_SIGN_PROP — illuminated exterior sign reading "Day &amp; Night Chemist".
     * Decorative; flavour tooltip: "Open until 10. Theoretically."
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    PHARMACY_SIGN_PROP(1.20f, 0.40f, 0.10f, 3, Material.SCRAP_METAL),

    // ── Issue #1122: Sun Kissed Studio ────────────────────────────────────────

    /**
     * TANNING_BED_PROP — a horizontal UV tanning unit with a hinged lid.
     * Press E to purchase a sunbed session from Tracey.
     * Tooltip: "The Northfield Riviera."
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    TANNING_BED_PROP(2.00f, 0.80f, 1.00f, 4, Material.SCRAP_METAL),

    /**
     * MASSAGE_TABLE_PROP — a padded fold-out massage table in the back room.
     * Press E near Jade or Tanya to book a massage.
     * Tooltip: "Surprisingly professional."
     * Destroyed by 3 punches; yields WOOD.
     */
    MASSAGE_TABLE_PROP(1.80f, 0.70f, 0.80f, 3, Material.WOOD),

    /**
     * SALON_RECEPTION_DESK_PROP — Tracey's front-of-house desk with a price menu board.
     * Press E to view the service menu.
     * Tooltip: "Cash only. No cheques. No questions."
     * Destroyed by 5 punches; yields WOOD.
     */
    SALON_RECEPTION_DESK_PROP(1.50f, 1.00f, 0.60f, 5, Material.WOOD),

    /**
     * LAUNDRY_BAG_PROP — a heavy-duty laundry bag kept behind the reception desk.
     * Contains the MARCHETTI_LEDGER when the back-room safe has been raided.
     * Tooltip: "Not towels."
     * Destroyed by 2 punches; yields BROWN_ENVELOPE.
     */
    LAUNDRY_BAG_PROP(0.80f, 0.60f, 0.60f, 2, Material.BROWN_ENVELOPE),

    // ── Issue #1128: Northfield NHS Walk-In Centre ─────────────────────────────

    /**
     * Triage reception desk — TRIAGE_NURSE Brenda's station at the entrance.
     * Player interacts to join the triage queue. Displays estimated wait time.
     * Destruction yields SCRAP_METAL; 5 hits.
     */
    TRIAGE_DESK_PROP(1.60f, 1.00f, 0.60f, 5, Material.SCRAP_METAL),

    /**
     * NHS treatment cubicle — curtained bay where Dr. Okafor treats patients.
     * Player must be adjacent for emergency healing. 4 cubicles total.
     * Indestructible (hitPoints 99).
     */
    TREATMENT_CUBICLE_PROP(2.00f, 2.20f, 1.80f, 99, null),

    /**
     * Controlled drugs safe — reinforced safe in the medicine room.
     * Requires CROWBAR. Yields TRAMADOL ×1–2, DIAZEPAM ×1.
     * Opening adds MEDICINE_THEFT + CONTROLLED_DRUG_TRAFFICKING to CriminalRecord,
     * +12 Notoriety, +2 WantedSystem stars. 20 hits; yields IRON on destruction.
     */
    CONTROLLED_DRUGS_SAFE_PROP(0.80f, 1.00f, 0.70f, 20, Material.IRON),

    /**
     * Blood pressure machine — self-service kiosk in the waiting room.
     * Player interaction gives a random BP reading. High reading triggers
     * a temporary resting heart-rate buff (reduces sprint fatigue).
     * 3 hits; yields SCRAP_METAL.
     */
    BLOOD_PRESSURE_MACHINE_PROP(0.50f, 1.40f, 0.50f, 3, Material.SCRAP_METAL),

    /**
     * Ambulance bay — docking station for the NHS ambulance outside the centre.
     * PARAMEDIC NPCs (Andy and Sue) spawn here. Interacting when player is
     * gravely injured triggers immediate transport to treatment.
     * Indestructible (hitPoints 99).
     */
    AMBULANCE_BAY_PROP(3.00f, 1.80f, 6.00f, 99, null),

    /**
     * NHS sign — large illuminated sign above the main entrance.
     * 3 hits; yields SCRAP_METAL. Destroying adds +2 Notoriety (vandalism).
     */
    NHS_SIGN_PROP(2.00f, 0.50f, 0.20f, 3, Material.SCRAP_METAL),

    /**
     * Cigarette bin — metal bin outside the entrance for cigarette ends.
     * Contains CIGARETTE ×1–3 with 40% probability. 2 hits; yields SCRAP_METAL.
     */
    CIGARETTE_BIN_PROP(0.30f, 0.90f, 0.30f, 2, Material.SCRAP_METAL),

    /**
     * Walk-in medicine room door — locked door separating waiting room from
     * the medicine room. Requires LOCKPICK to open. Opening triggers Brenda's
     * alarm if she is within 10 blocks. 8 hits; yields WOOD.
     */
    WALK_IN_MEDICINE_ROOM_DOOR_PROP(1.00f, 2.20f, 0.15f, 8, Material.WOOD),

    /**
     * Queue board — electronic display showing current queue length and wait times.
     * Player can inspect to see their position. 3 hits; yields SCRAP_METAL.
     */
    QUEUE_BOARD_PROP(1.00f, 1.40f, 0.15f, 3, Material.SCRAP_METAL),

    /**
     * X-ray viewer — wall-mounted light box in a treatment cubicle.
     * Decorative; can be interacted with for a humorous message about
     * "someone's suspicious left knee".
     * 3 hits; yields SCRAP_METAL.
     */
    X_RAY_VIEWER_PROP(0.60f, 0.80f, 0.15f, 3, Material.SCRAP_METAL),

    /**
     * Sharps bin — yellow disposal container for used syringes.
     * Contains UNUSED_SYRINGE ×1 with 30% probability. 2 hits; yields PLASTIC.
     */
    SHARPS_BIN_PROP(0.35f, 0.80f, 0.35f, 2, Material.PLASTIC),

    // ── Issue #1130: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Till — cash register counter in the BP kiosk. Press E with CROWBAR to rob
     * (yields 8–18 COIN; ARMED_ROBBERY record; Notoriety +15; police response 3 min).
     * Wayne panic button cuts response to 30 seconds at Wanted Tier ≥ 2.
     * 6 hits to destroy; yields SCRAP_METAL.
     */
    TILL_PROP(0.70f, 1.20f, 0.50f, 6, Material.SCRAP_METAL),

    /**
     * Lottery / scratch-card display — rotating rack near the till.
     * Press E to buy a SCRATCH_CARD for 1 COIN. 3 hits; yields CARDBOARD.
     */
    LOTTERY_DISPLAY_PROP(0.50f, 1.40f, 0.30f, 3, Material.CARDBOARD),

    /**
     * Microwave — counter-top unit that heats MICROWAVE_PASTY items.
     * Press E with a raw pasty to produce a heated MICROWAVE_PASTY.
     * After 21:00 adds 30% food-poisoning risk. 4 hits; yields SCRAP_METAL.
     */
    MICROWAVE_PROP(0.55f, 0.40f, 0.45f, 4, Material.SCRAP_METAL),

    /**
     * Energy drink fridge — illuminated glass-door fridge stocked with ENERGY_DRINK.
     * Press E to purchase for 1 COIN. 5 hits; yields GLASS.
     */
    ENERGY_DRINK_FRIDGE_PROP(0.70f, 1.80f, 0.70f, 5, Material.GLASS),

    /**
     * Confectionery shelf — wall-mounted shelf stocked with CHOCOLATE_BAR and CRISPS.
     * Press E to browse and purchase. 3 hits; yields WOOD.
     */
    CONFECTIONERY_SHELF_PROP(1.20f, 1.00f, 0.25f, 3, Material.WOOD),

    /**
     * Cigarette cabinet — locked glass cabinet behind the counter.
     * Breaking it (3 hits) yields CIGARETTE_CARTON ×2–3; adds NoiseSystem +25.
     * 3 hits to destroy; yields GLASS.
     */
    CIGARETTE_CABINET_PROP(0.90f, 0.80f, 0.20f, 3, Material.GLASS),

    /**
     * Air pump — forecourt air pump for vehicle tyres.
     * Interacting while crouched at night adds a SIPHONING cover state.
     * 3 hits; yields SCRAP_METAL.
     */
    AIR_PUMP_PROP(0.30f, 1.40f, 0.30f, 3, Material.SCRAP_METAL),

    /**
     * Squeegee bucket — windscreen-cleaning station on the forecourt.
     * Decorative; can be used as a distraction prop (splash on NPC).
     * 2 hits; yields SCRAP_METAL (bucket).
     */
    SQUEEGEE_BUCKET_PROP(0.35f, 0.60f, 0.35f, 2, Material.SCRAP_METAL),

    /**
     * Stockroom shelf — shelving unit in the back stockroom.
     * Searching (press E) yields random items: NEWSPAPER, CHOCOLATE_BAR, CRISPS,
     * or the CASH_POUCH_PROP (5% chance). 4 hits; yields WOOD.
     */
    STOCKROOM_SHELF_PROP(1.00f, 2.00f, 0.30f, 4, Material.WOOD),

    /**
     * Cash pouch — vinyl money pouch kept in the stockroom.
     * Taking it yields 5–12 COIN; records THEFT CriminalRecord; Notoriety +5.
     * 1 hit; yields COIN (equivalent to its contents).
     */
    CASH_POUCH_PROP(0.20f, 0.10f, 0.30f, 1, Material.COIN),

    /**
     * Car wash token machine — coin-operated dispenser on the forecourt exterior.
     * Insert 3 COIN (press E) to receive a CAR_WASH_TOKEN. 5 hits; yields SCRAP_METAL.
     */
    CAR_WASH_TOKEN_MACHINE_PROP(0.50f, 1.20f, 0.40f, 5, Material.SCRAP_METAL),

    /**
     * Panic button — wall-mounted emergency button behind the till counter.
     * Activated by Wayne at Wanted Tier ≥ 2; cuts police response from 3 min to 30 s.
     * Destroying it (2 hits) prevents Wayne from triggering fast police response.
     * 2 hits; yields SCRAP_METAL.
     */
    PANIC_BUTTON_PROP(0.15f, 0.15f, 0.10f, 2, Material.SCRAP_METAL),

    // ── Issue #1132: Northfield Dog Grooming Parlour — Pawfect Cuts ──────────

    /**
     * Grooming table — a padded stainless-steel grooming bench where dogs are
     * brushed, dried, and trimmed. The primary interactive prop in Pawfect Cuts.
     * Press E while standing adjacent to interact (requires DOG companion present).
     * 5 hits; yields SCRAP_METAL.
     */
    GROOMING_TABLE_PROP(1.20f, 0.90f, 0.60f, 5, Material.SCRAP_METAL),

    /**
     * Dog bath tub — a raised stainless-steel bathing tub used for washing dogs.
     * Required for Basic Wash and Medicated Bath services at Pawfect Cuts.
     * 6 hits; yields SCRAP_METAL.
     */
    DOG_BATH_TUB_PROP(1.00f, 0.70f, 0.60f, 6, Material.SCRAP_METAL),

    /**
     * Grooming tool rack — a wall-mounted display rack holding scissors, clippers,
     * brushes, and other grooming equipment. Decorative in the parlour.
     * 3 hits; yields SCRAP_METAL.
     */
    GROOMING_TOOL_RACK_PROP(0.90f, 1.40f, 0.20f, 3, Material.SCRAP_METAL),

    /**
     * Pet treat display — a revolving counter-top display of DOG_TREAT items.
     * Player can purchase DOG_TREATs (1 COIN each) by pressing E.
     * 3 hits; yields WOOD.
     */
    PET_TREAT_DISPLAY_PROP(0.50f, 1.20f, 0.50f, 3, Material.WOOD),

    /**
     * Waiting area bench — a padded bench in the Pawfect Cuts waiting area.
     * DOG_OWNER NPCs sit here with their dogs before appointments.
     * 3 hits; yields WOOD.
     */
    WAITING_AREA_BENCH_PROP(1.60f, 0.80f, 0.50f, 3, Material.WOOD),

    /**
     * Dog show trophy cabinet — a glass-fronted display cabinet in Pawfect Cuts
     * holding past Northfield Dog Show trophies and rosettes.
     * Contains a DOG_SHOW_ROSETTE lootable item (unlocked after winning the show).
     * 5 hits; yields GLASS.
     */
    DOG_SHOW_TROPHY_CABINET_PROP(0.80f, 1.80f, 0.40f, 5, Material.GLASS),

    // ── Issue #1134: Patel's Newsagent ────────────────────────────────────────

    /**
     * Newsagent counter — the main service counter in Patel's News.
     * Press E to open the retail purchase menu (NEWSPAPER, SCRATCH_CARD, PENNY_SWEETS,
     * CHOCOLATE_BAR, CRISPS, ENERGY_DRINK, TOBACCO_POUCH, LOTTERY_TICKET, BIRTHDAY_CARD,
     * LOCAL_MAP). 5 hits to destroy; yields WOOD.
     */
    NEWSAGENT_COUNTER_PROP(1.80f, 1.10f, 0.60f, 5, Material.WOOD),

    /**
     * Newsagent notice board — a cork board near the door with local ads and a
     * paper-round sign-up sheet. Press E between 05:30 and 07:00 to begin the
     * paper round delivery job. 3 hits to destroy; yields WOOD.
     */
    NEWSAGENT_NOTICE_BOARD_PROP(0.80f, 1.20f, 0.10f, 3, Material.WOOD),

    /**
     * Magazine rack — a floor-standing wire display rack near the counter.
     * Press E to browse RACING_FORM, DIY_MONTHLY, PUZZLE_BOOK; top-shelf
     * DODGY_MAGAZINE requires Notoriety ≥ 30. Shoplifting is detected by Raj.
     * 4 hits to destroy; yields SCRAP_METAL.
     */
    MAGAZINE_RACK_PROP(0.60f, 1.50f, 0.35f, 4, Material.SCRAP_METAL),

    /**
     * Newspaper bundle — a bundled stack of today's Daily Ragamuffin editions,
     * placed outside the shop door from 06:00. Interact to buy a NEWSPAPER (1 COIN)
     * or to grab one for free (SHOPLIFTING + Notoriety +1 if Raj spots you).
     * 2 hits to destroy; yields CARDBOARD.
     */
    NEWSPAPER_BUNDLE_PROP(0.60f, 0.50f, 0.40f, 2, Material.CARDBOARD),

    /**
     * Sweet counter — a glass counter-top display filled with jars of penny sweets.
     * Press E to buy PENNY_SWEETS (1 COIN). 4 hits to destroy; yields GLASS.
     */
    SWEET_COUNTER_PROP(1.20f, 1.00f, 0.50f, 4, Material.GLASS),

    /**
     * Letterbox — a residential door letterbox used as a paper-round delivery stop.
     * Eight LETTERBOX_PROP instances are placed across the nearby residential streets.
     * Press E while carrying NEWSPAPER to deliver (increments paper-round progress).
     * Non-destructible (attached to door); 0 hits; yields null.
     */
    LETTERBOX_PROP(0.30f, 0.15f, 0.10f, 0, null),

    /**
     * Stockroom door — the locked door to the Patel's News stockroom.
     * Requires NEWSAGENT_KEY or a LOCKPICK to open. Contains CASH_BOX_PROP
     * (8–14 COIN) and stock shelves. 6 hits to break down; yields WOOD.
     */
    STOCKROOM_DOOR_PROP(0.15f, 2.00f, 0.90f, 6, Material.WOOD),

    // ── Issue #1136: The Vaults Nightclub ─────────────────────────────────────

    /**
     * Nightclub Queue Prop — the velvet-rope barrier outside The Vaults.
     * Players press E to join the queue. Big Dave (BOUNCER) is stationed here.
     * Non-destructible; 0 hits; yields null.
     */
    NIGHTCLUB_QUEUE_PROP(0.10f, 1.00f, 0.10f, 0, null),

    /**
     * Nightclub Bar Prop — the sticky-topped bar counter inside The Vaults.
     * Press E to buy drinks: CAN_OF_LAGER (3C), DOUBLE_VODKA (5C), WATER_BOTTLE (1C).
     * Stacey (BARMAID) is stationed here. 5 hits to demolish; yields WOOD.
     */
    NIGHTCLUB_BAR_PROP(2.00f, 1.10f, 0.60f, 5, Material.WOOD),

    /**
     * Nightclub Speaker Prop — a bass-heavy speaker stack on the dancefloor.
     * Proximity (≤5 blocks) triggers DANCING state in the player.
     * Emits high-radius noise (85 dB equivalent) during opening hours.
     * 5 hits to destroy; yields SCRAP_METAL.
     */
    NIGHTCLUB_SPEAKER_PROP(0.60f, 1.50f, 0.60f, 5, Material.SCRAP_METAL),

    /**
     * Nightclub Toilet Prop — a cramped toilet stall at The Vaults.
     * Wayne (DRUG_DEALER_NPC) lurks here 23:00–02:00. DRUNK NPCs are pickpocketable.
     * 3 hits to break; yields SCRAP_METAL.
     */
    NIGHTCLUB_TOILET_PROP(0.90f, 2.00f, 0.90f, 3, Material.SCRAP_METAL),

    /**
     * Nightclub VIP Table Prop — a booth in the VIP area of The Vaults.
     * Accessible at FactionSystem respect ≥ 20 (FRIENDLY) or after 3 club visits.
     * MARCHETTI_CREW NPCs share GANG_ACTIVITY rumours freely here.
     * 4 hits to destroy; yields WOOD.
     */
    NIGHTCLUB_VIP_TABLE_PROP(1.20f, 0.80f, 0.80f, 4, Material.WOOD),

    /**
     * Nightclub Office Door Prop — the locked manager's office door at The Vaults.
     * Requires NIGHTCLUB_MASTER_KEY or 3 LOCKPICK attempts to open.
     * If player caught inside without permission: Big Dave called, 10-min ban.
     * 8 hits to break down; yields WOOD.
     */
    NIGHTCLUB_OFFICE_DOOR_PROP(0.15f, 2.20f, 1.00f, 8, Material.WOOD),

    /**
     * Nightclub Safe Prop — the combination safe in the manager's office.
     * Contains 20–40 COIN + random loot (COUNTERFEIT_NOTE, STOLEN_PHONE, GOLD_CHAIN).
     * Failed lockpick (≥2 attempts) triggers alarm: Big Dave + WantedSystem tier +1.
     * Successful loot: +6 Notoriety, THEFT record, LOCAL_EVENT rumour, newspaper headline.
     * 12 hits to force open; yields SCRAP_METAL.
     */
    NIGHTCLUB_SAFE_PROP(0.50f, 0.80f, 0.40f, 12, Material.SCRAP_METAL),

    /**
     * Nightclub Mirror Ball Prop — a rotating disco mirror ball above the dancefloor.
     * Decorative; identical visual function to DISCO_BALL in RaveSystem.
     * Non-destructible; 0 hits; yields null.
     */
    NIGHTCLUB_MIRROR_BALL_PROP(0.40f, 0.40f, 0.40f, 0, null),

    // ── Issue #1280: Northfield Nightclub — The Vaults ────────────────────────

    /**
     * STROBE_LIGHT_PROP — ceiling-mounted strobe light above the dancefloor at The Vaults.
     * Decorative; contributes to atmosphere. Non-destructible; 0 hits; yields null.
     */
    STROBE_LIGHT_PROP(0.30f, 0.30f, 0.30f, 0, null),

    /**
     * BOUNCER_BOOTH_PROP — Big Dave's entry booth at the front door of The Vaults.
     * Press E to attempt entry (checked by NightclubSystem.canEnter).
     * 6 hits to break; yields SCRAP_METAL.
     */
    BOUNCER_BOOTH_PROP(0.80f, 1.20f, 0.50f, 6, Material.SCRAP_METAL),

    /**
     * VELVET_ROPE_PROP — the queue barrier at The Vaults front door.
     * Marks the queue area; non-destructible; 0 hits; yields null.
     */
    VELVET_ROPE_PROP(0.10f, 1.00f, 0.10f, 0, null),

    /**
     * PRIVATE_BOOTH_PROP — a VIP seating booth in The Vaults.
     * Press E (with VIP access or MARCHETTI Respect >= 50) to sit.
     * Seeds GANG_ACTIVITY rumour; Tracker mission triggers at Respect >= 50.
     * 4 hits to break; yields WOOD.
     */
    PRIVATE_BOOTH_PROP(1.20f, 1.00f, 0.80f, 4, Material.WOOD),

    /**
     * FIRE_EXIT_DOOR_PROP — the back-alley fire exit at The Vaults.
     * Press E to attempt smuggling exit; 30% alarm trigger chance.
     * 5 hits to break; yields SCRAP_METAL.
     */
    FIRE_EXIT_DOOR_PROP(0.20f, 2.10f, 1.00f, 5, Material.SCRAP_METAL),

    // ── Issue #1142: Northfield RAOB Lodge ────────────────────────────────────

    /**
     * Lodge Bar Prop — the Lodge's private bar counter.
     * Press E to buy drinks: Worthington's Bitter at 1 COIN each.
     * Available only during session hours (Tue/Thu 19:00–23:00, Sat 12:00–23:00).
     * 8 hits to break; yields WOOD.
     */
    LODGE_BAR_PROP(3.0f, 1.2f, 0.6f, 8, Material.WOOD),

    /**
     * Lodge Altar Prop — the ceremonial focal point of the Lodge Room.
     * Press E to begin the initiation ceremony (requires 2 SPONSORSHIP_FORMs + 5 COIN).
     * Ceremony lasts 60 in-game seconds while Norman reads from the RITUAL_BOOK_PROP.
     * 6 hits to break; yields WOOD.
     */
    LODGE_ALTAR_PROP(1.5f, 1.8f, 1.5f, 6, Material.WOOD),

    /**
     * Lodge Safe Prop — Keith's combination safe in the back room.
     * Contains 30–50 COIN + LODGE_CHARTER_DOCUMENT + REGALIA_SET.
     * Detection: 15% if Big Bernard is in distraction window (20:00–20:30), 70% outside.
     * 12 hits to force open; yields SCRAP_METAL.
     */
    LODGE_SAFE_PROP(0.8f, 1.2f, 0.8f, 12, Material.SCRAP_METAL),

    // ── Issue #1349: Northfield RAOB Buffalo Lodge No. 1247 ───────────────────

    /**
     * Buffalo Lodge Plaque — the brass plaque beside the Lodge entrance door.
     * Press E to read Lodge history. Indestructible (health=99).
     */
    BUFFALO_LODGE_PLAQUE(0.4f, 0.3f, 0.05f, 99, Material.SCRAP_METAL),

    /**
     * Initiation Altar Prop — the ceremonial altar used for the RAOBLodgeSystem initiation.
     * Press E (with sponsor trust + COIN) to begin BattleBarMiniGame initiation sequence.
     * 6 hits to break; yields WOOD.
     */
    INITIATION_ALTAR_PROP(1.6f, 1.9f, 1.6f, 6, Material.WOOD),

    /**
     * Lodge Door Prop — the heavy oak entrance door to the Lodge.
     * Opens for RAOB_MEMBER players; requires LODGE_DOORMAN approval otherwise.
     * 10 hits to force open; yields WOOD.
     */
    LODGE_DOOR_PROP(1.0f, 2.4f, 0.12f, 10, Material.WOOD),

    /**
     * Ceremonial Candle Prop — a tall wax candle on a brass stand in the Lodge room.
     * Decorative; can be knocked over by player action (noise level 2).
     * 2 hits to break; yields nothing (WOOD stub).
     */
    CEREMONIAL_CANDLE_PROP(0.08f, 0.60f, 0.08f, 2, Material.WOOD),

    /**
     * Regalia Room Door Prop — the locked door to the Regalia Room.
     * Requires PRIMO membership tier to open legitimately, or 4 LOCKPICK attempts.
     * 8 hits to break down; yields WOOD.
     */
    REGALIA_ROOM_DOOR_PROP(1.0f, 2.5f, 0.1f, 8, Material.WOOD),

    /**
     * Ritual Book Prop — the Lodge's ceremonial ritual book, read by Norman during initiations.
     * Press E (as PRIMO+) to read Lodge lore.
     * 2 hits to break; yields PLANKS (the wooden lectern).
     */
    RITUAL_BOOK_PROP(0.2f, 0.35f, 0.15f, 2, Material.PLANKS),

    // ── Issue #1144: Northfield Probation Office ──────────────────────────────

    /**
     * Probation Desk — Karen's sign-on check-in desk in the case officer's office.
     * Press E to begin the sign-on dialogue tree with Karen.
     * 8 hits to break; yields WOOD.
     */
    PROBATION_DESK_PROP(1.40f, 0.80f, 0.70f, 8, Material.WOOD),

    /**
     * Case File Cabinet — a grey metal filing cabinet in Karen's office.
     * Accessible only 13:00–14:00 (Karen's lunch break; Debbie distracted).
     * Contains 1–3 CASE_FILE_DOCUMENT items. CCTV_CAMERA_PROP records access.
     * 5 hits to break; yields SCRAP_METAL.
     */
    CASE_FILE_CABINET_PROP(0.50f, 1.20f, 0.40f, 5, Material.SCRAP_METAL),

    /**
     * Community Service Station — the task assignment board in the park.
     * Gary (COMMUNITY_SERVICE_SUPERVISOR) stands nearby.
     * Press E to receive community service task assignment and sign in/out.
     * 3 hits to break; yields WOOD.
     */
    COMMUNITY_SERVICE_STATION_PROP(0.80f, 1.60f, 0.20f, 3, Material.WOOD),

    // ── Issue #1146: Mick's MOT & Tyre Centre ────────────────────────────────

    /**
     * MOT Ramp Prop — the hydraulic ramp in Bay 1 used for MOT testing.
     * Press E to access the MOT menu: Official MOT (Terry present only,
     * Mon/Wed/Fri 09:00–12:00, 5 COIN), Dodgy MOT (15 COIN, instant pass,
     * 25% Terry walk-in risk), Advisory Repairs (3–8 COIN, requires SCRAP_METAL/TYRE).
     * 12 hits to break; yields SCRAP_METAL.
     */
    MOT_RAMP_PROP(3.0f, 1.2f, 6.0f, 12, Material.SCRAP_METAL),

    /**
     * Repair Ramp Prop — the second hydraulic ramp in Bay 2 used for chop-shop work.
     * Press E to access the Chop Shop menu: "Chop for parts" yields 2–4 SCRAP_METAL
     * + 1 TYRE + 1 CAR_PART. "Cut and shut" fuses two cars.
     * 12 hits to break; yields SCRAP_METAL.
     */
    REPAIR_RAMP_PROP(3.0f, 1.2f, 6.0f, 12, Material.SCRAP_METAL),

    /**
     * Tyre Stack Prop — a stack of part-worn tyres leaning against the garage wall.
     * Press E to buy: 1 TYRE for 2 COIN each (up to 4 per visit).
     * 4 hits to break; yields TYRE.
     */
    TYRE_STACK_PROP(1.0f, 1.5f, 0.5f, 4, Material.TYRE),

    /**
     * Car For Sale Prop — one of three bangers on the forecourt (roadworthiness 25–55).
     * Press E to inspect: shows price (20–40 COIN) and visible defects.
     * 8 hits to break; yields SCRAP_METAL.
     */
    CAR_FOR_SALE_PROP(2.0f, 1.5f, 4.5f, 8, Material.SCRAP_METAL),

    /**
     * Garage Office Prop — the tiny office at the back of the garage.
     * Contains a pegboard with car keys and the CASH_TIN_PROP.
     * 6 hits to break; yields WOOD.
     */
    GARAGE_OFFICE_PROP(2.5f, 2.5f, 2.5f, 6, Material.WOOD),

    /**
     * Cash Tin Prop — Mick's petty cash tin on the office desk.
     * Contains 20–40 COIN. Open silently with LOCKPICK or noisily with CROWBAR
     * (NoiseSystem +6, seeds CRIME_SPOTTED rumour if witnessed).
     * 3 hits to break; yields SCRAP_METAL.
     */
    CASH_TIN_PROP(0.3f, 0.2f, 0.2f, 3, Material.SCRAP_METAL),

    // ── Issue #1148: Northfield Council Estate Lock-Up Garages ───────────────

    /**
     * Garage Door Prop — the rolling shutter door on each council lock-up garage.
     * Locked by default; opened via LOCKPICK (silent, 70%) or CROWBAR (noisy, 100%).
     * Cannot be destroyed; yields no material on hit. 999 hits effectively indestructible.
     */
    GARAGE_DOOR_PROP(2.5f, 2.2f, 0.15f, 999, null),

    /**
     * Garage Shelf Prop — heavy metal shelving unit inside Garage 7 (player rental).
     * Stores up to 30 item stacks as persistent world storage.
     * 5 hits to break; yields SCRAP_METAL.
     */
    GARAGE_SHELF_PROP(1.8f, 2.0f, 0.4f, 5, Material.SCRAP_METAL),

    /**
     * Amplifier Prop — a guitar amplifier in Garage 1 (band rehearsal).
     * Produces NoiseSystem spike when powered. 4 hits to break; yields CABLE.
     */
    AMPLIFIER_PROP(0.55f, 0.60f, 0.45f, 4, Material.CABLE),

    /**
     * Drum Kit Prop — full drum kit in Garage 1 (band rehearsal).
     * Player can interact (E) to join rehearsal if 3 previous sessions watched.
     * 6 hits to break; yields DRUM_COMPONENT.
     */
    DRUM_KIT_PROP(1.20f, 1.10f, 1.00f, 6, Material.DRUM_COMPONENT),

    /**
     * Scales Prop — drug dealer's precision scales in Garage 3 (Marchetti drug den).
     * Interacting with E triggers StreetEconomySystem deal UI.
     * 3 hits to break; yields SCRAP_METAL.
     */
    SCALES_PROP(0.25f, 0.15f, 0.20f, 3, Material.SCRAP_METAL),

    /**
     * Pigeon Coop Prop — a wooden pigeon loft in Garage 8 (pigeon fancier).
     * Interacting triggers PigeonRacingSystem UI. 8 hits to break; yields WOOD.
     */
    PIGEON_COOP_PROP(1.80f, 1.60f, 0.90f, 8, Material.WOOD),

    // ── Issue #1151: Northfield Sporting & Social Club ────────────────────────

    /**
     * CHALK_SCOREBOARD_PROP — a wall-mounted chalk scoreboard beside the dartboard.
     * Displays current 501 scores for the active darts game.
     * Press E to view; non-interactive during matches. 3 hits to break; yields WOOD.
     */
    CHALK_SCOREBOARD_PROP(0.80f, 1.20f, 0.05f, 3, Material.WOOD),

    /**
     * BAR_PUMP_PROP — a row of hand-pull beer pumps behind the members' bar.
     * Press E to order a drink (BITTER, MILD, or LAGER_TOP) if holding COIN.
     * Members get half-price; non-members cannot order.
     * 6 hits to break; yields SCRAP_METAL.
     */
    BAR_PUMP_PROP(0.60f, 1.20f, 0.30f, 6, Material.SCRAP_METAL),

    /**
     * CIGARETTE_MACHINE_PROP — a wall-mounted cigarette vending machine near the entrance.
     * Press E to buy a pack for 3 COIN. Not tied to any system mechanic; flavour prop.
     * 4 hits to break; yields SCRAP_METAL.
     */
    CIGARETTE_MACHINE_PROP(0.50f, 1.40f, 0.30f, 4, Material.SCRAP_METAL),

    /**
     * CLUB_SIGN_PROP — the external sign above the front door:
     * "Northfield Sporting &amp; Social Club — Members Only".
     * Press E for flavour text. 2 hits to break; yields WOOD.
     */
    CLUB_SIGN_PROP(1.50f, 0.50f, 0.10f, 2, Material.WOOD),

    /**
     * PROTECTION_ENVELOPE_PROP — a brown envelope on the back-room table containing 20 COIN.
     * Appears Mon 19:55; collected by MARCHETTI_ENFORCER at 20:00.
     * Stealing it (E during 19:55–20:00) triggers Tommy ambush + Wanted +3.
     * 1 hit to break; yields COIN (20).
     */
    PROTECTION_ENVELOPE_PROP(0.20f, 0.05f, 0.12f, 1, Material.COIN),

    // ── Issue #1153: Northfield Community Centre ──────────────────────────────

    /**
     * MAT_PROP — a rolled foam exercise mat stacked at the hall wall.
     * Present during aerobics sessions. 2 hits to break; yields nothing.
     */
    MAT_PROP(0.60f, 0.15f, 0.18f, 2, null),

    /**
     * STACKING_CHAIR_PROP — a blue plastic stacking chair used for support-group circles.
     * 2 hits to break; yields WOOD.
     */
    STACKING_CHAIR_PROP(0.45f, 0.90f, 0.45f, 2, Material.WOOD),

    /**
     * BISCUIT_TIN_PROP — a decorative tin of assorted biscuits on the refreshments table.
     * Press E to steal contents (awards BISCUIT_BANDIT if in a support-group session).
     * 1 hit to break; yields nothing.
     */
    BISCUIT_TIN_PROP(0.22f, 0.16f, 0.22f, 1, null),

    /**
     * BOUNCY_CASTLE_PROP — an inflatable bouncy castle for the Toddler Playgroup (Tue/Thu).
     * Decorative; deflates at session end. 5 hits to break; yields nothing.
     */
    BOUNCY_CASTLE_PROP(2.40f, 1.80f, 2.40f, 5, null),

    /**
     * PHOTOCOPIER_PROP — an office photocopier in the community centre back corridor.
     * Press E with GRANT_APPLICATION_FORM to create FORGED_GRANT_APPLICATION.
     * Generates HIGH noise (level 3.5, 15-block radius via NoiseSystem).
     * 6 hits to break; yields SCRAP_METAL.
     */
    PHOTOCOPIER_PROP(0.70f, 1.10f, 0.60f, 6, Material.SCRAP_METAL),

    /**
     * SERVING_TABLE_PROP — a folding trestle table used for curry night food service.
     * 3 hits to break; yields WOOD.
     */
    SERVING_TABLE_PROP(1.80f, 0.85f, 0.70f, 3, Material.WOOD),

    /**
     * POOL_TABLE_PROP — a full-size pool table unlocked by the legitimate grant application.
     * Interacting (E) with COIN starts a game. 8 hits to break; yields WOOD.
     */
    POOL_TABLE_PROP(2.20f, 0.85f, 1.20f, 8, Material.WOOD),

    /**
     * FILING_CABINET_PROP — a grey metal filing cabinet in the community centre back corridor.
     * Press E with LOCKPICK to open (70% success) and steal GRANT_APPLICATION_FORM.
     * 5 hits to break; yields SCRAP_METAL.
     */
    FILING_CABINET_PROP(0.55f, 1.30f, 0.50f, 5, Material.SCRAP_METAL),

    // ── Issue #1157: Northfield Tesco Express ─────────────────────────────────

    /**
     * MEAL_DEAL_COUNTER_PROP — waist-high display counter at the Tesco Express entrance.
     * Press E to trigger the meal deal selection UI. 5 hits; yields WOOD.
     */
    MEAL_DEAL_COUNTER_PROP(1.20f, 0.90f, 0.60f, 5, Material.WOOD),

    /**
     * WINE_CHILLER_PROP — glass-fronted refrigerated wine display unit.
     * Press E to browse alcohol items: TESCO_FINEST_WINE × 3, TESCO_OWN_BRAND_VODKA × 2.
     * LOCKED after 22:00 (Challenge 25 policy). 8 hits to break; yields GLASS.
     */
    WINE_CHILLER_PROP(0.80f, 1.80f, 0.40f, 8, Material.GLASS),

    // ── Issue #1159: Northfield Angel Nails & Beauty ─────────────────────────

    /**
     * A nail station — a small table with UV lamp and tools at Angel Nails & Beauty.
     * Press E when vacant to purchase a nail service. 4 placed in the salon interior.
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    NAIL_STATION_PROP(0.80f, 1.00f, 0.50f, 3, Material.SCRAP_METAL),

    /**
     * A UV nail dryer — small countertop device placed next to each nail station.
     * Decorative; destroyed by 2 punches; yields SCRAP_METAL.
     */
    NAIL_DRYER_PROP(0.30f, 0.25f, 0.25f, 2, Material.SCRAP_METAL),

    /**
     * A colour wall display holding 12 bottles of NAIL_POLISH.
     * Press E to attempt theft (25% base catch chance; 60% if Kim is watching).
     * Destroyed by 3 punches; yields NAIL_POLISH ×1.
     */
    COLOUR_WALL_PROP(1.20f, 1.60f, 0.15f, 3, Material.NAIL_POLISH),

    /**
     * Exterior signage for Angel Nails & Beauty — a pink illuminated sign above the shopfront.
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    NAIL_SALON_SIGN_PROP(1.60f, 0.50f, 0.15f, 3, Material.SCRAP_METAL),

    // ── Issue #1161: Northfield Poundstretcher ────────────────────────────────

    /**
     * A Poundstretcher shelf crate — a waist-high display crate holding own-brand items.
     * Press E to attempt shoplifting. Placed along the shop walls.
     * Destroyed by 4 punches; yields WOOD.
     */
    CRATE_PROP(0.60f, 0.80f, 0.60f, 4, Material.WOOD),

    /**
     * A delivery pallet — a wooden pallet loaded with stock boxes from the delivery lorry.
     * Spawned at the loading bay at 08:00 daily; despawns when brought inside.
     * Player can loot one item (press E) within the 2-minute window.
     * Destroyed by 5 punches; yields WOOD.
     */
    PALLET_PROP(1.20f, 0.30f, 0.80f, 5, Material.WOOD),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1163: NHS Dentist props
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * RECEPTION_DESK_DENTAL_PROP — Deborah's front desk at the NHS Dental Practice.
     * Press E during opening hours (Mon–Fri 08:30–17:00, excl. lunch 13:00–14:00)
     * to register as an NHS patient or check appointment status.
     * Destroyed by 8 punches; yields WOOD.
     */
    RECEPTION_DESK_DENTAL_PROP(1.40f, 1.10f, 0.60f, 8, Material.WOOD),

    /**
     * DENTAL_CHAIR_PROP — Dr. Rashid's reclining dental chair in the treatment room.
     * Press E when an appointment is due to start treatment (Filling / Root Canal / Extraction).
     * Destroyed by 6 punches; yields SCRAP_METAL.
     */
    DENTAL_CHAIR_PROP(0.70f, 1.20f, 1.80f, 6, Material.SCRAP_METAL),

    /**
     * IMPROVISED_DENTAL_CHAIR_PROP — Mirek's council-flat dental setup: a reclining
     * car seat bolted to the floor. Press E to receive black-market dental treatment
     * (5 COIN; 60 % success, 20 % INFECTION side effect, 40 % BOTCHED_JOB).
     * Destroyed by 4 punches; yields SCRAP_METAL.
     */
    IMPROVISED_DENTAL_CHAIR_PROP(0.65f, 1.10f, 1.60f, 4, Material.SCRAP_METAL),

    /**
     * FILING_CABINET_PROP (dental practice) — grey metal filing cabinet in the
     * NHS Dental Practice back office. Lock-pick to obtain WAITING_LIST_LETTER.
     * Notoriety catch chance: 40 % if Deborah present; 60 % at Notoriety Tier ≥ 1.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    FILING_CABINET_DENTAL_PROP(0.55f, 1.30f, 0.50f, 5, Material.SCRAP_METAL),

    /**
     * DENTAL_SIGN_PROP — exterior NHS blue sign for the Northfield Dental Practice.
     * Read-only flavour prop; press E to read: "NHS Dental Practice — Currently NOT
     * TAKING NEW NHS PATIENTS." Destroyed by 3 punches; yields SCRAP_METAL.
     */
    DENTAL_SIGN_PROP(0.40f, 0.80f, 0.10f, 3, Material.SCRAP_METAL),

    /**
     * TOOTHPASTE_DISPLAY_PROP — a wire rack of toothpaste and mouthwash near the
     * reception. Press E to browse; TOOTHBRUSH available for 1 COIN.
     * Destroyed by 2 punches; yields WOOD.
     */
    TOOTHPASTE_DISPLAY_PROP(0.40f, 1.20f, 0.30f, 2, Material.WOOD),

    // ── Issue #1165: Northfield Match Day ────────────────────────────────────

    /**
     * COACH_PROP — a full-size coach at the STADIUM_COACH_PARK.
     * 4 coach bays; indestructible (0 hits to break = never destroyed).
     * Provides CROWD_WARMTH shelter on cold match days.
     * Width 8 blocks, height 3 blocks, depth 3 blocks.
     */
    COACH_PROP(8.0f, 3.0f, 3.0f, 0, null),

    /**
     * MATCH_DAY_STALL_PROP — a fold-out table where FOOTBALL_TOUT stands.
     * Destroyed by 4 punches; yields WOOD.
     * Press E to interact with the tout's wares.
     */
    MATCH_DAY_STALL_PROP(1.20f, 0.80f, 0.60f, 4, Material.WOOD),

    // ── Issue #1167: Northfield Amateur Boxing Club ───────────────────────

    /**
     * SPEED_BAG_PROP — wall-mounted speed bag at Tommy's Gym.
     * Press E to train dexterity (max 3/day). Each session reduces incoming
     * damage by 1 per SPEED_BAG_CHALK item consumed during sparring.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    SPEED_BAG_PROP(0.30f, 0.30f, 0.30f, 5, Material.SCRAP_METAL),

    /**
     * BET_TABLE_PROP — Wayne's betting table in the back room.
     * Active only during underground white-collar circuit nights
     * (alternate Saturdays 22:00–02:00). Press E to place bets or
     * accept bout-fixing bribe offers.
     * Destroyed by 4 punches; yields WOOD.
     */
    BET_TABLE_PROP(1.20f, 0.80f, 0.60f, 4, Material.WOOD),

    /**
     * PADLOCK_PROP — padlock on Derek's terraced-house door (trophy quest).
     * Lockpick to enter. Press E to attempt lockpick.
     * Destroyed by 3 punches; yields SCRAP_METAL.
     */
    PADLOCK_PROP(0.20f, 0.25f, 0.10f, 3, Material.SCRAP_METAL),

    /**
     * NOTICE_BOARD_PROP (boxing club) — cork notice board inside Tommy's Gym.
     * Displays clues for the trophy quest and Friday Night Fight sign-up sheet.
     * Press E to read / sign up. Destroyed by 3 punches; yields WOOD.
     */
    BOXING_NOTICE_BOARD_PROP(0.80f, 1.00f, 0.10f, 3, Material.WOOD),

    /**
     * BOXING_TROPHY_CABINET_PROP — glass-fronted trophy cabinet in Tommy's Gym.
     * Contains the 1987 ABA trophy (or empty slot if stolen by Derek).
     * Destroyed by 5 punches; yields GLASS.
     */
    BOXING_TROPHY_CABINET_PROP(1.20f, 1.80f, 0.40f, 5, Material.GLASS),

    // ── Issue #1175: Northfield Argos ─────────────────────────────────────────

    /**
     * COLLECTION_BOARD_PROP — illuminated number display board at the Argos counter.
     * Flashes the player's number when their order is ready.
     * Press E on the counter when number is called to collect.
     */
    COLLECTION_BOARD_PROP(1.20f, 0.80f, 0.15f, 5, Material.SCRAP_METAL),

    /**
     * QUEUE_BARRIER_PROP — retractable belt barrier defining the collection queue.
     * Marks the queue lane; collides with player to enforce queue discipline.
     */
    QUEUE_BARRIER_PROP(0.10f, 1.00f, 0.10f, 2, Material.SCRAP_METAL),

    // ── Issue #1177: Northfield Sunday Car Park Market ────────────────────────

    /**
     * MARKET_STALL_PROP — folding table with tarpaulin awning used by Sunday market
     * traders. Player can set up their own stall on any empty table (up to 6 items).
     */
    MARKET_STALL_PROP(1.50f, 1.00f, 0.80f, 4, Material.WOOD),

    /**
     * MARKET_VAN_PROP — Transit-style van parked at the edge of the Co-op car park.
     * Each trader has one. Boot briefly unattended during 07:15–07:30 setup window.
     * Interact to attempt the Van Unload Heist (steal 1–3 items).
     */
    MARKET_VAN_PROP(4.50f, 1.80f, 2.00f, 0, null),

    /**
     * COUNCIL_VAN — yellow council enforcement van. Patrols the market every
     * 4 in-game minutes. Spawns COUNCIL_OFFICER when it arrives at the market.
     */
    COUNCIL_VAN(4.00f, 1.80f, 2.00f, 0, null),

    // ── Issue #1183: Northfield Household Waste Recycling Centre ─────────────

    /**
     * GENERAL_WASTE_SKIP — large open-top skip for general household waste.
     * Player presses E to dispose items (no return). May yield RAGS or CHARITY_BAG.
     */
    GENERAL_WASTE_SKIP(2.50f, 1.20f, 1.20f, 20, null),

    /**
     * RECYCLING_SKIP — colour-coded skip for glass, paper, and cans.
     * Player presses E to dispose recyclables. May yield GLASS_BOTTLE ×1–3.
     */
    RECYCLING_SKIP(2.50f, 1.20f, 1.20f, 20, null),

    /**
     * WEEE_SKIP — Waste Electrical and Electronic Equipment skip.
     * Player presses E to search (3 seconds). Yields CIRCUIT_BOARD, COPPER_WIRE,
     * OLD_PHONE, BROKEN_KETTLE, or RETRO_CONSOLE. Dave suspicious if player lingers
     * more than 10 seconds. Richer loot during night runs.
     */
    WEEE_SKIP(2.50f, 1.20f, 1.20f, 20, null),

    /**
     * METAL_SKIP — skip for scrap metal and ferrous waste.
     * Player presses E to search (3 seconds). Yields SCRAP_METAL ×1–2, rarely COPPER_PIPE.
     * Dave suspicious if player lingers more than 10 seconds.
     */
    METAL_SKIP(2.50f, 1.20f, 1.20f, 20, null),

    /**
     * REUSE_SHELF_PROP — the Reuse Corner free shelf where items are left for others.
     * Contents seeded daily by Random(dayNumber). Max 3 items taken per visit.
     * Player can also deposit items. Deposit 3+ items → REUSE_HERO achievement.
     */
    REUSE_SHELF_PROP(1.20f, 1.00f, 0.40f, 5, null),

    // ── Issue #1190: Information Broker ──────────────────────────────────────

    /**
     * BEAD_CURTAIN_PROP — a strip curtain of plastic beads hanging in a doorway.
     * Marks the entrance to Kenny Doyle's back-room booth in The Feathers.
     * Passable (no collision blocking the player); provides visual division.
     * Destroyed by 1 punch; yields nothing.
     */
    BEAD_CURTAIN_PROP(0.90f, 2.00f, 0.05f, 1, null),

    /**
     * STASH_CRATE_PROP — a wooden crate spawned at a real world position by
     * InformationBrokerSystem when a {@code STASH_SITE} IntelligenceLot is applied.
     * Contains faction-appropriate loot. Destroyed by 3 punches; yields {@link Material#WOOD}.
     */
    STASH_CRATE_PROP(0.60f, 0.60f, 0.60f, 3, Material.WOOD),

    // ── Issue #1196: Environmental Health Officer ─────────────────────────────

    /**
     * HYGIENE_RATING_PROP — a Food Hygiene Rating Scheme sticker mounted near a
     * food venue's front door. Displays current rating (1–5 stars). Breakable in
     * 1 punch, drops HYGIENE_STICKER. Updated by EnvironmentalHealthSystem after
     * each inspection.
     */
    HYGIENE_RATING_PROP(0.20f, 0.30f, 0.02f, 1, Material.HYGIENE_STICKER),

    /**
     * IMPROVEMENT_NOTICE_PROP — an official improvement notice placed by Janet on
     * the venue counter after a rating ≤ 2 inspection. Venue NPC refuses to serve
     * player while this is present (up to 1 in-game day).
     */
    IMPROVEMENT_NOTICE_PROP(0.30f, 0.04f, 0.21f, 1, Material.IMPROVEMENT_NOTICE),

    /**
     * CLOSURE_NOTICE_PROP — a red closure notice placed by Janet after a rating 1
     * inspection. Venue is force-closed for 1 in-game day.
     */
    CLOSURE_NOTICE_PROP(0.30f, 0.04f, 0.21f, 1, null),

    /**
     * SUGGESTION_BOX_PROP — a wooden suggestion box at the Council Office.
     * Player can post ANONYMOUS_NOTE (E interaction) to force Janet's next
     * inspection to target a specific venue. One tip-off per in-game week.
     */
    SUGGESTION_BOX_PROP(0.25f, 0.30f, 0.25f, 5, null),

    /**
     * KITCHEN_PROP — a commercial kitchen interior fixture present in food venues.
     * E-hold (5 s, GRAFTING ≥ Apprentice) raises venue condition +10.
     * Crowbar sabotage variant: condition −25, CRIMINAL_DAMAGE CrimeType.
     */
    KITCHEN_PROP(1.20f, 1.00f, 0.80f, 10, null),

    // ── Issue #1198: Northfield Traffic Warden ────────────────────────────────

    /**
     * Double-yellow line road marking — painted on the road surface along the high street.
     * Any car parked here while Clive is on patrol will immediately receive a PCN
     * (zero grace period). Destroyed by CROWBAR (CRIMINAL_DAMAGE CrimeType); +8 Notoriety.
     */
    DOUBLE_YELLOW_PROP(0.50f, 0.05f, 2.00f, 10, null),

    /**
     * Single-yellow line road marking — 2-hour max parking restriction (SIDE_STREET_ZONE).
     * Car parked here for &gt; 120 in-game minutes will receive a PCN from Clive.
     */
    SINGLE_YELLOW_PROP(0.50f, 0.05f, 2.00f, 8, null),

    /**
     * Pay-and-Display ticket machine — standalone yellow metal column at COUNCIL_CAR_PARK.
     * Press E to buy a PAY_AND_DISPLAY_TICKET (1 COIN per hour, max 4 hours).
     * Vandalism with CROWBAR: disables machine for 1 in-game day, seeds VANDALISM rumour,
     * +8 Notoriety, +20 NoiseSystem level. Sets broken-machine flag (+20% appeal success).
     * Yields SCRAP_METAL when destroyed.
     */
    PAY_AND_DISPLAY_MACHINE_PROP(0.40f, 1.60f, 0.40f, 12, Material.SCRAP_METAL),

    /**
     * Wheel clamp — a bright yellow Denver boot applied to a car's wheel by Clive
     * after a second-circuit violation. Car.isClamped() == true while present.
     * Removed when the fine (12 COIN) is paid; drops WHEEL_CLAMP material.
     */
    WHEEL_CLAMP_PROP(0.60f, 0.40f, 0.40f, 15, Material.WHEEL_CLAMP),

    /**
     * Appeal desk — counter prop at the COUNCIL_OFFICE where the player submits
     * a PCN appeal. Press E (must hold PENALTY_CHARGE_NOTICE + BLANK_PAPER).
     * Staffed by COUNCIL_RECEPTIONIST Brenda. Appeal takes 2 in-game days.
     */
    APPEAL_DESK_PROP(1.20f, 0.90f, 0.70f, 8, Material.SCRAP_METAL),

    // ── Issue #1202: Karaoke Night ─────────────────────────────────────────

    /**
     * KARAOKE_BOOTH_PROP — DJ/karaoke booth at the back of Wetherspoons.
     * Active Fridays 20:00–23:00. Press E to queue for a song (player must have
     * Notoriety &lt; 50). Bev the compère manages the queue.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    KARAOKE_BOOTH_PROP(1.20f, 1.80f, 0.80f, 5, Material.SCRAP_METAL),

    /**
     * FUSE_BOX_PROP — electrical fuse box in the back hallway of Wetherspoons.
     * Interact while holding SCREWDRIVER to kill the PA system during a rival's
     * performance. Notoriety +3 if witnessed. Destroyed by 5 punches; yields
     * SCRAP_METAL.
     */
    FUSE_BOX_PROP(0.40f, 0.60f, 0.20f, 5, Material.SCRAP_METAL),

    /**
     * MICROPHONE_PROP — world prop version of the karaoke microphone on its stand.
     * Grab while Bev is distracted (10-second window after each song). Yields
     * MICROPHONE item. Destroyed by 2 punches; yields MICROPHONE material.
     */
    MICROPHONE_PROP(0.20f, 1.20f, 0.20f, 2, Material.MICROPHONE),

    // ── Issue #1205: Northfield DVSA Test Centre ──────────────────────────────

    /**
     * THEORY_TERMINAL_PROP — a beige desktop PC on a particleboard desk.
     * Interact (E) to start a theory test (costs 2 COIN, 10 questions from pool of 30).
     * Each question is multiple-choice (1–4 input); ≥8/10 correct = pass.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    THEORY_TERMINAL_PROP(0.70f, 1.10f, 0.50f, 5, Material.SCRAP_METAL),

    /**
     * EXAMINER_DESK_PROP — Sandra's examination desk at the DVSA Test Centre.
     * Interact (E) to speak with Sandra: book a practical test, attempt a bribe,
     * or check test status. Destroyed by 8 punches; yields WOOD.
     */
    EXAMINER_DESK_PROP(1.40f, 0.80f, 0.60f, 8, Material.WOOD),

    /**
     * INSTRUCTOR_CAR_PROP — Keith's dual-control Ford Fiesta parked outside the
     * test centre. Interact (E) to start a driving lesson with Keith (3 COIN/session).
     * Not driveable by the player independently. Destroyed by 10 punches; yields
     * SCRAP_METAL. Exempt from TrafficWarden PCNs.
     */
    INSTRUCTOR_CAR_PROP(2.50f, 1.50f, 4.50f, 10, Material.SCRAP_METAL),

    /**
     * MOT_BACK_OFFICE_FILING_CABINET_PROP — a grey metal filing cabinet in the back office of the MOT centre.
     * LOCKPICK required; 3 hits to open; yields PENDING_TEST_RESULT_ITEM (forged cert).
     * 30% chance Sandra flags it as fraud each use: WantedSystem +2 stars, FRAUD crime.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    MOT_BACK_OFFICE_FILING_CABINET_PROP(0.50f, 1.30f, 0.40f, 5, Material.SCRAP_METAL),

    // ── Issue #1209: Citizens Advice Bureau ───────────────────────────────────

    /**
     * CONSULTATION_DESK_PROP — Margaret or Brian's desk in the Citizens Advice
     * Bureau. Press E (with volunteer seated) to open the consultation topic menu.
     * Destroyed by 6 punches; yields WOOD.
     */
    CONSULTATION_DESK_PROP(1.40f, 0.80f, 0.60f, 6, Material.WOOD),

    /**
     * LOW_PARTITION_PROP — a waist-high room divider separating the waiting area
     * from the consultation desks. Walkable over (no collision above 0.9f).
     * Decorative; yields WOOD on destruction (4 punches).
     */
    LOW_PARTITION_PROP(2.00f, 0.90f, 0.10f, 4, Material.WOOD),

    /**
     * CAB_REFERRAL_FORM_PROP — a paper pile on the consultation desk. Interact (E)
     * to pick up a CAB_REFERRAL_FORM item. Used in the Eviction Dodger side-quest.
     * Yields CAB_REFERRAL_FORM on destruction (1 punch).
     */
    CAB_REFERRAL_FORM_PROP(0.30f, 0.05f, 0.40f, 1, Material.CAB_REFERRAL_FORM),

    // ── Issue #1216: Northfield Driving Instructor ─────────────────────────────

    /**
     * DRIVING_SCHOOL_DESK_PROP — Dave's booking counter in the BSM driving school
     * above the newsagent. Press E to book lessons (15 COIN/lesson, or 60 COIN for
     * 5-lesson block course). When Dave is on a lesson and Notoriety Tier ≥ 2,
     * pressing E instead offers access to the filing cabinet forged-certificate hustle.
     * Destroyed by 8 punches; yields WOOD.
     */
    DRIVING_SCHOOL_DESK_PROP(1.40f, 0.80f, 0.60f, 8, Material.WOOD),

    /**
     * THEORY_TEST_POSTER_PROP — a laminated highway-code poster on the wall.
     * Flavour only; readable (press E) to see a mock question. No game effect.
     * Destroyed by 2 punches; yields nothing.
     */
    THEORY_TEST_POSTER_PROP(0.60f, 0.90f, 0.02f, 2, null),

    /**
     * DRIVING_SCHOOL_FILING_CABINET_PROP — a grey metal filing cabinet in Dave's
     * back office containing forged pass certificates. Costs 20 COIN to access
     * (accessible only when Dave is on a lesson and player has Notoriety Tier ≥ 2).
     * 35% detection chance at DVSA Test Centre per use.
     * Destroyed by 5 punches; yields SCRAP_METAL.
     */
    DRIVING_SCHOOL_FILING_CABINET_PROP(0.55f, 1.30f, 0.50f, 5, Material.SCRAP_METAL),

    /**
     * DAVE_INSTRUCTOR_CAR_PROP — Dave's dual-control Vauxhall Corsa with L-plates,
     * parked outside the DRIVING_SCHOOL overnight. Stealable 21:00–06:00 using
     * SCREWDRIVER (10s hotwire) or CAR_KEY_COPY (instant). Dual-control flag caps
     * speed at 60% without a FULL_DRIVING_LICENCE. Destroyed by 10 punches;
     * yields SCRAP_METAL.
     */
    DAVE_INSTRUCTOR_CAR_PROP(2.50f, 1.50f, 4.50f, 10, Material.SCRAP_METAL),

    // ── Issue #1218: Northfield Claims Management Company ─────────────────────

    /**
     * GOLD_SIGN_PROP — the brass and gold-effect signage outside Compensation Kings,
     * reading "COMPENSATION KINGS — NO WIN, NO FEE*". Flavour prop; interaction (E)
     * triggers Gary's intake spiel. Cannot be destroyed (hitsToBreak = 99 = very tough).
     */
    GOLD_SIGN_PROP(1.20f, 0.50f, 0.05f, 99, null),

    /**
     * PAMPHLET_RACK_PROP — a wire rack of glossy leaflets by the front door of
     * Compensation Kings. Press E to take a pamphlet (flavour text: "Have you been
     * injured in an accident that wasn't your fault?"). No game effect; yields null.
     * Destroyed by 2 punches.
     */
    PAMPHLET_RACK_PROP(0.30f, 1.20f, 0.20f, 2, null),

    /**
     * LOOSE_PAVING_PROP — a cracked and raised section of pavement on the high street,
     * near the park, or outside Iceland. Press E adjacent to this prop to stage a
     * SLIP_AND_FALL accident via ClaimsManagementSystem.stageAccident(). The prop
     * remains in the world after use. Destroyed by 5 punches; yields STONE.
     */
    LOOSE_PAVING_PROP(0.80f, 0.10f, 0.80f, 5, Material.STONE),

    // ── Issue #1224: Northfield Cybernet Internet Café ────────────────────────

    /**
     * PRINTER_PROP — a battered inkjet printer on the back-room desk at Cybernet.
     * Press E (requires back-room access + BLANK_PAPER + PRINTER_INK) to print
     * forged documents. Printing in main room in Asif's sight triggers ALERT.
     * Noise level 1 within 4 blocks. Destroyed by 3 punches; yields SCRAP_METAL.
     */
    PRINTER_PROP(0.50f, 0.40f, 0.40f, 3, Material.SCRAP_METAL),

    /**
     * PREPAID_CARD_READER_PROP — a chunky card reader near Cybernet's front counter.
     * Press E with BURNER_PHONE in inventory and 1 COIN to add 5 units to the phone.
     * Cannot be destroyed (bolted to counter; hitsToBreak = 99).
     */
    PREPAID_CARD_READER_PROP(0.30f, 0.25f, 0.20f, 99, null),

    /**
     * STASH_BOX_PROP — a battered cardboard box in Cybernet's back room containing
     * random loot: 3–8 COIN + USB_DRIVE + occasional STOLEN_PHONE.
     * One-time loot per in-game day; resets at 09:00. Destroyed by 2 punches; yields null.
     */
    STASH_BOX_PROP(0.50f, 0.35f, 0.40f, 2, null),

    // ── Issue #1227: Wheelwright Motors — Dodgy Car Lot ──────────────────────

    /**
     * CAR_LOT_SIGN_PROP — large billboard-style sign on the front wall of Wheelwright
     * Motors reading "Wheelwright Motors — No Reasonable Offer Refused". 2.0×1.2×0.15
     * AABB. Destroyed by 6 punches (sturdy); yields SCRAP_METAL.
     */
    CAR_LOT_SIGN_PROP(2.00f, 1.20f, 0.15f, 6, Material.SCRAP_METAL),

    /**
     * PORTACABIN_PROP — Wayne's site office at the rear of the forecourt. Contains a
     * kettle, laminator, and DESK_PROP. 3.5×2.5×4.0 AABB. Wayne retreats here after
     * 17:00 and during RAIN. Destroyed by 20 punches; yields WOOD.
     */
    PORTACABIN_PROP(3.50f, 2.50f, 4.00f, 20, Material.WOOD),

    /**
     * BUNTING_PROP — decorative string of triangular flags stretched across the Wheelwright
     * Motors forecourt. Visual only (no collision). Destroyed by 1 punch; yields null.
     */
    BUNTING_PROP(8.00f, 0.20f, 0.20f, 1, null),

    /**
     * PLAYER_CAR_BAY_PROP — a painted tarmac bay marker near the player's squat where
     * purchased cars are parked. Not collidable. Removed when repossessed car is taken.
     * Yields null on destruction.
     */
    PLAYER_CAR_BAY_PROP(2.00f, 0.05f, 4.00f, 1, null),

    /**
     * MECHANIC_PROP — Bez's workstation on the Wheelwright Motors forecourt: an oil-stained
     * workbench with tools, a trolley jack, and a car bonnet propped open. 2.0×1.2×2.0 AABB.
     * Destroyed by 8 punches; yields SCRAP_METAL.
     */
    MECHANIC_PROP(2.00f, 1.20f, 2.00f, 8, Material.SCRAP_METAL),

    // ── Issue #1231: Northfield ASBO System ───────────────────────────────────

    /**
     * ASBO Exclusion Sign — an official council-issued cordon sign placed at the
     * boundary of a landmark designated as an exclusion zone under an active ASBO.
     * Marks 2–4 randomly selected landmarks (WETHERSPOONS, PUB, OFF_LICENCE,
     * SKATE_PARK, FRIED_CHICKEN_SHOP, GREYHOUND_TRACK, NIGHTCLUB, BETTING_SHOP).
     * The player triggers a 5-second countdown if they enter a marked zone.
     * Non-destructible (council property); 0 hits; yields null.
     * Removed automatically when the ASBO expires or is overturned.
     */
    ASBO_EXCLUSION_SIGN_PROP(0.60f, 1.40f, 0.10f, 0, null),

    // ── Issue #1237: Northfield St. Aidan's Primary School ───────────────────

    /**
     * School Gate — iron gate at the main entrance; closed outside 08:00–16:00.
     * Entering when closed triggers Intruder Alert chain via PrimarySchoolSystem.
     * Break: 0 hits (cannot be broken). Dimension: wide single-road gate.
     */
    SCHOOL_GATE_PROP(2.00f, 1.80f, 0.15f, 0, null),

    /**
     * Canteen Hatch — the serving counter where Dot (DINNER_LADY) sells SCHOOL_DINNER.
     * Active 11:30–13:30 Mon–Fri. Press E to buy or attempt pickpocket.
     */
    CANTEEN_HATCH_PROP(1.40f, 1.00f, 0.80f, 0, null),

    /**
     * Caretaker's Shed — Derek's storage shed in the playground corner.
     * Locked with padlock; requires CARETAKER_SHED_KEY or LOCKPICKING ≥ Journeyman.
     * Contains PHOTOCOPIER_INK_CARTRIDGE, SCRAP_METAL, CARETAKER_MASTER_KEY.
     */
    CARETAKER_SHED_PROP(2.50f, 2.20f, 2.00f, 0, null),

    /**
     * Headteacher's Office Door — solid wood door requiring CARETAKER_MASTER_KEY.
     * Behind it: filing cabinet with OFSTED_DRAFT_REPORT and SCHOOL_REPORT_FORM.
     */
    HEADTEACHER_OFFICE_DOOR_PROP(1.00f, 2.10f, 0.10f, 0, null),

    /**
     * Ofsted Notice — pinned to the school entrance during inspection days.
     * Interacting gives player option to help decorate (Notoriety −2) or steal draft.
     * Disappears when inspection ends.
     */
    OFSTED_NOTICE_PROP(0.60f, 0.90f, 0.05f, 0, null),

    // ── Issue #1243: Northfield Bert's Tyres & MOT ───────────────────────────

    /**
     * Inspection Pit — the below-floor vehicle inspection bay at Bert's garage.
     * Player can crouch into it to loot CATALYTIC_CONVERTER, CAR_BATTERY items
     * when Bert is distracted. Bert returns to check the pit every 45 seconds.
     */
    INSPECTION_PIT_PROP(3.00f, 1.20f, 6.00f, 0, null),

    /**
     * Inspection Hatch — the hinged steel floor hatch giving access to the pit.
     * Press E to open/close. Stays open for 20 seconds then auto-closes.
     * Opening while Bert is watching adds +5 to his suspicion counter.
     */
    INSPECTION_HATCH_PROP(1.20f, 0.10f, 1.20f, 6, null),

    /**
     * Parts Shelf — industrial metal shelving holding CAR_BATTERY, spare parts,
     * and occasionally CATALYTIC_CONVERTER. Press E to search while Bert is
     * distracted (BERT_DISTRACTED state). Yields 1–3 items.
     */
    PARTS_SHELF_PROP(2.40f, 2.00f, 0.50f, 6, Material.CAR_BATTERY),

    /**
     * Garage Phone — the wall-mounted phone at Bert's desk. Press E to trigger
     * a fake call that puts Bert into BERT_DISTRACTED state for 20 seconds while
     * Kyle fetches him. Can only be used once per in-game hour.
     */
    GARAGE_PHONE_PROP(0.30f, 0.20f, 0.15f, 3, null),

    // ── Issue #1252: Northfield TV Licensing ──────────────────────────────────

    /**
     * Detector Van — white Transit-style van with a dish aerial on the roof.
     * Spawns on the street outside the industrial estate every Sunday 14:00–16:00.
     * Purely atmospheric — the van is always empty (reflecting the real-world myth).
     * Player interaction (E) triggers tooltip: "The van appears to have nobody in it."
     * and unlocks the MYTH_BUSTER achievement.
     */
    DETECTOR_VAN_PROP(4.50f, 2.20f, 2.00f, 20, Material.SCRAP_METAL),

    // ── Issue #952: Clucky's Fried Chicken ───────────────────────────────────

    /**
     * Industrial deep fryer at Clucky's. Press E to sabotage (Notoriety +15, ARSON crime).
     * Destroyed by 6 hits; yields SCRAP_METAL. Generates NoiseSystem level 8 on smash.
     */
    FRYER_PROP(0.70f, 1.20f, 0.70f, 6, Material.SCRAP_METAL),

    /**
     * Cheap plastic table at Clucky's seating area.
     * Destroyed by 2 hits; yields nothing. Seats the YOUTH_GANG NPC group.
     */
    PLASTIC_TABLE_PROP(0.90f, 0.75f, 0.90f, 2, null),

    /**
     * Cheap plastic chair at Clucky's seating area.
     * Destroyed by 1 hit; yields nothing.
     */
    PLASTIC_CHAIR_PROP(0.50f, 0.85f, 0.50f, 1, null),

    /**
     * Rolling security grille on the Clucky's shopfront.
     * Drops at 02:00 (closing time). Cannot be destroyed by the player.
     * Acts as a solid collision barrier when closed.
     */
    SECURITY_GRILLE_PROP(8.00f, 4.00f, 0.10f, 0, null),

    /**
     * Clucky's Fried Chicken illuminated fascia sign above the entrance.
     * Destroyed by 4 hits; yields nothing. High visibility — seeding a rumour
     * that "the chicken sign is smashed" if witnessed.
     */
    CLUCKYS_SIGN_PROP(4.00f, 0.60f, 0.20f, 4, null),

    // ── Issue #1257: Northfield Rag-and-Bone Man ──────────────────────────────

    /**
     * Rag-and-Bone Stop — a kerbside chalk marking at one of Barry's 6 route stops.
     * Barry parks his van here for 90 seconds; up to 3 PUBLIC NPCs queue to sell JUNK_ITEM.
     * Player can also press E to sell scrap. Cannot be broken.
     */
    RAGBONE_STOP(1.00f, 0.05f, 1.00f, 0, null),

    /**
     * Rag-and-Bone Van — Barry's battered flatbed Transit. Drives between RAGBONE_STOPs.
     * Destroyed by 20 hits; yields SCRAP_METAL. Player can slash tyres at night with PENKNIFE
     * (interacting 02:00–06:00) to trigger the Rival Route Sabotage hustle.
     * Repairable by the player using RUBBER_TYRE for 10 COIN.
     */
    RAG_AND_BONE_VAN(4.50f, 2.20f, 2.00f, 20, Material.SCRAP_METAL),

    /**
     * Garden Ornament — a gnome, flamingo, or similar prop placed in a front garden.
     * Stolen from residential areas at night (CriminalRecord THEFT, WitnessSystem).
     * Sellable to Barry next morning for 2 COIN, or fenced for 1 COIN.
     * Destroyed by 3 hits; yields nothing.
     */
    GARDEN_ORNAMENT(0.40f, 0.60f, 0.40f, 3, null),

    // ── Issue #1259: Northfield Pub Quiz Night ────────────────────────────────

    /**
     * Derek's quiz podium — a tall wooden lectern at the front of the quiz area.
     * Player presses E to register for the quiz (costs 1 COIN, open 19:30–20:00).
     * Yields QUIZ_SHEET on registration. Cannot be destroyed (hitsToBreak = 0).
     */
    QUIZ_PODIUM_PROP(0.60f, 1.20f, 0.50f, 0, null),

    // ── Issue #1263: Northfield Illegal Street Racing ─────────────────────────

    /**
     * RACING_CONE_PROP — a traffic cone used to mark the start/finish line and
     * course boundaries of the ring road sprint. Placed by Shane on meet nights.
     * Can be kicked aside (hitsToBreak = 1, drops null). Cannot block vehicles.
     */
    RACING_CONE_PROP(0.30f, 0.70f, 0.30f, 1, null),

    /**
     * RACE_FINISH_BANNER_PROP — a cloth banner strung across the ring road
     * marking the finish line of the illegal sprint. Shane deploys it at 23:15.
     * Torn down automatically on police shutdown (or by player).
     * hitsToBreak = 2, no material drop.
     */
    RACE_FINISH_BANNER_PROP(8.00f, 0.20f, 0.20f, 2, null),

    // ── Issue #1273: Northfield Fly-Tipping Ring ──────────────────────────────

    /**
     * FLY_TIP_PILE_PROP — a heap of illegally dumped waste (black bags, broken furniture,
     * rubble bags) spawned when the player fly-tips a load on wasteland, canal bank,
     * or back alley. Persists for 2 in-game days, draining NeighbourhoodSystem Vibes −1/day.
     * Cleared after 60 s if player pays the FIXED_PENALTY_NOTICE fine, or naturally decays.
     * Player can destroy it manually (4 hits). No material drop (it just disappears).
     * Within 10 blocks of a food venue: adds RAT_PENALTY to EnvironmentalHealthSystem inspection.
     */
    FLY_TIP_PILE_PROP(1.80f, 1.20f, 1.80f, 4, null),

    /**
     * CLEARANCE_JOB_BOARD_PROP — a community noticeboard outside the Pound Shop and
     * Community Centre showing 0–3 available waste-clearance jobs. Player presses E
     * to accept a job (House Clearance, Garden Rubbish, or Builder's Rubble).
     * Cannot be destroyed (hitsToBreak = 0).
     */
    CLEARANCE_JOB_BOARD_PROP(0.60f, 1.40f, 0.10f, 0, null),

    // ── Issue #1278: Northfield Travelling Fairground ─────────────────────────

    /**
     * DODGEMS_RIDE_PROP — the dodgems arena: low-sided ring of bumper cars.
     * Player interacts with E to start a 2-COIN session (screen-shake, Notoriety risk near police).
     * 6×6×0.5 collision floor; cannot be destroyed (hitsToBreak = 0).
     */
    DODGEMS_RIDE_PROP(6.00f, 0.50f, 6.00f, 0, null),

    /**
     * WALTZER_RIDE_PROP — the spinning waltzer ride platform.
     * Player interacts with E to ride for 2 COIN (DIZZY debuff). Tip Wayne 1 COIN to spin faster.
     * 5×5×2 rotating platform; cannot be destroyed.
     */
    WALTZER_RIDE_PROP(5.00f, 2.00f, 5.00f, 0, null),

    /**
     * STRONGMAN_PROP — the high-striker strength-test tower with a bell at the top.
     * Player uses BattleBarMiniGame (MEDIUM difficulty) to attempt the strike.
     * Win = FAIRGROUND_PRIZE. Staffed by Big Lenny or a FAIRGROUND_WORKER.
     * 0.6×4.0×0.6; hitsToBreak = 8 (yields SCRAP_METAL if destroyed).
     */
    STRONGMAN_PROP(0.60f, 4.00f, 0.60f, 8, Material.SCRAP_METAL),

    /**
     * CANDY_FLOSS_STALL_PROP — Donna's candy floss and toffee apple stall.
     * Player interacts with E to buy CANDY_FLOSS (2 COIN) or TOFFEE_APPLE (2 COIN).
     * 1.8×2.0×1.0; hitsToBreak = 3 (yields WOOD).
     */
    CANDY_FLOSS_STALL_PROP(1.80f, 2.00f, 1.00f, 3, Material.WOOD),

    /**
     * RING_TOSS_STALL_PROP — Shaz's ring toss stall. Always rigged (15% real success vs stated 25%).
     * Player pays 1 COIN per attempt. Win = FAIRGROUND_PRIZE.
     * 1.8×2.0×1.0; hitsToBreak = 3 (yields WOOD).
     */
    RING_TOSS_STALL_PROP(1.80f, 2.00f, 1.00f, 3, Material.WOOD),

    /**
     * DIESEL_GENERATOR_PROP — the unattended generator powering the fairground lights.
     * Strippable for SCRAP_METAL on Sunday 23:00+ when fairground is closed.
     * 1.5×1.2×0.8; hitsToBreak = 10 (yields SCRAP_METAL).
     */
    DIESEL_GENERATOR_PROP(1.50f, 1.20f, 0.80f, 10, Material.SCRAP_METAL),

    // ── Issue #1282: Northfield Day & Night Chemist ───────────────────────────

    /**
     * DISPENSARY_COUNTER_PROP — Janet's main counter where prescriptions are dispensed.
     * Player presses E to submit PRESCRIPTION_SLIP; 10s dispense timer begins.
     * 2.0×1.1×0.8; indestructible (hitsToBreak = 0).
     */
    DISPENSARY_COUNTER_PROP(2.00f, 1.10f, 0.80f, 0, null),

    /**
     * PRESCRIPTION_RACK_PROP — the rack holding bagged prescriptions awaiting collection.
     * Player presses E to look for their name; forged prescription attempt made here.
     * 0.8×1.8×0.4; hitsToBreak = 3 (yields WOOD).
     */
    PRESCRIPTION_RACK_PROP(0.80f, 1.80f, 0.40f, 3, Material.WOOD),

    /**
     * METHADONE_FRIDGE_PROP — refrigerated unit storing the day's methadone doses.
     * Accessible 13:30–14:30; steal attempt: 40% success, WantedSystem +3, ROBBERY charge.
     * 0.8×1.6×0.7; hitsToBreak = 8 (yields SCRAP_METAL).
     */
    METHADONE_FRIDGE_PROP(0.80f, 1.60f, 0.70f, 8, Material.SCRAP_METAL),

    /**
     * CHEMIST_CROSS_PROP — the green illuminated cross sign above the entrance.
     * Decorative; indicates pharmacy is open.
     * 0.8×1.0×0.1; hitsToBreak = 2 (yields GLASS).
     */
    CHEMIST_CROSS_PROP(0.80f, 1.00f, 0.10f, 2, Material.GLASS),

    // ── Issue #1299: Northfield Street Chuggers ──────────────────────────────

    /**
     * CHARITY_CLIPBOARD_STAND_PROP — a fold-out table with a charity banner outside the
     * charity shop on Northfield High Street. Used as the spawn anchor for CHUGGER_LEADER
     * (Tracy) and patrol origin for CHUGGER NPCs (Mon–Sat 10:00–17:00).
     * Player can press E to interact with Tracy (hire as fake chugger, cancel direct debit).
     * Dropped item: CHARITY_CLIPBOARD (represents the leftover paperwork).
     * 1.50×0.90×0.60; hitsToBreak = 4 (yields CHARITY_CLIPBOARD).
     */
    CHARITY_CLIPBOARD_STAND_PROP(1.50f, 0.90f, 0.60f, 4, Material.CHARITY_CLIPBOARD),

    // ── Issue #1303: Northfield Dave's Carpets ────────────────────────────────

    /**
     * CLOSING_DOWN_BANNER_PROP — the sun-bleached banner draped across Dave's shopfront:
     * "CLOSING DOWN — EVERYTHING MUST GO!!"
     * Decorative; clicking it records a closing-down claim observation.
     * 3.00×1.00×0.10; hitsToBreak = 2 (yields CARPET_OFFCUT).
     */
    CLOSING_DOWN_BANNER_PROP(3.00f, 1.00f, 0.10f, 2, Material.CARPET_OFFCUT),

    /**
     * CARPET_ROLL_PROP — a large roll of carpet in the stockroom.
     * Lootable when Kev is distracted; yields CARPET_OFFCUT ×2–4 (random).
     * 2.00×0.60×0.60; hitsToBreak = 3 (yields CARPET_OFFCUT).
     */
    CARPET_ROLL_PROP(2.00f, 0.60f, 0.60f, 3, Material.CARPET_OFFCUT),

    /**
     * SOFA_PROP — a second-hand sofa on the shop floor.
     * Becomes a SOFA item in inventory after purchase; or can be physically pushed
     * using SACK_TRUCK_PROP mechanics.
     * 2.00×0.90×0.90; hitsToBreak = 8 (yields SOFA).
     */
    SOFA_PROP(2.00f, 0.90f, 0.90f, 8, Material.SOFA),

    /**
     * SACK_TRUCK_PROP — a fold-flat trolley in the stockroom.
     * Interacting (E) adds a SACK_TRUCK to inventory. Lootable when Kev is distracted.
     * 0.50×1.40×0.40; hitsToBreak = 4 (yields SACK_TRUCK).
     */
    SACK_TRUCK_PROP(0.50f, 1.40f, 0.40f, 4, Material.SACK_TRUCK),

    /**
     * TRADING_STANDARDS_WARNING — the formal warning notice issued by Sandra after Dave's
     * claim is reported. Placed on the shopfront by Sandra; causes Dave to enter DEFLATED state.
     * 0.60×0.40×0.05; hitsToBreak = 1 (yields null — just disappears).
     */
    TRADING_STANDARDS_WARNING(0.60f, 0.40f, 0.05f, 1, null),

    // ── Issue #1306: Northfield Traveller Site ────────────────────────────────

    /**
     * CARAVAN_PROP — Paddy's static caravan, the main site structure.
     * 4.50×2.80×2.00; hitsToBreak = 12 (yields SCRAP_METAL).
     * Player presses E to attempt night raid (02:00–04:00 only, requires CROWBAR or LOCKPICK).
     * LURCHER_DOG wakes if player lacks DOG_PERMISSION_FLAG.
     */
    CARAVAN_PROP(4.50f, 2.80f, 2.00f, 12, Material.SCRAP_METAL),

    /**
     * SCRAP_PILE_PROP — a mound of scrap metal and salvaged parts near the caravan.
     * 1.80×1.20×1.80; hitsToBreak = 4 (yields SCRAP_METAL).
     * Player presses E to search (4 seconds): yields SCRAP_METAL ×1–3, rarely COPPER_PIPE.
     * NOTE: A SCRAP_PILE_PROP already exists in the scrapyard. This one is traveller-specific
     * and can yield STOLEN_BIKE (5% chance).
     */
    TRAVELLER_SCRAP_PILE_PROP(1.80f, 1.20f, 1.80f, 4, Material.SCRAP_METAL),

    /**
     * TARMAC_DRUM_PROP — barrel of cold-lay tarmac beside the caravan.
     * 0.60×1.00×0.60; hitsToBreak = 5 (yields TARMAC_MIX).
     * Player presses E to purchase TARMAC_MIX for 5 COIN when Paddy is present.
     */
    TARMAC_DRUM_PROP(0.60f, 1.00f, 0.60f, 5, Material.TARMAC_MIX),

    /**
     * DOG_FIGHT_RING_PROP — makeshift ring in a corner of the site.
     * 2.40×0.60×2.40; hitsToBreak = 3 (yields SCRAP_METAL).
     * Active Fri/Sat 21:00–23:00. Player presses E to place bets (2:1 payout).
     * Dispersed by RSPCA_OFFICER when dog fight is reported.
     */
    DOG_FIGHT_RING_PROP(2.40f, 0.60f, 2.40f, 3, Material.SCRAP_METAL),

    /**
     * ENFORCEMENT_NOTICE_PROP — council enforcement notice posted by Derek.
     * 0.60×0.40×0.05; hitsToBreak = 1 (yields null — just disappears).
     * Placed on the CARAVAN_PROP when eviction is triggered.
     */
    ENFORCEMENT_NOTICE_PROP(0.60f, 0.40f, 0.05f, 1, null),

    /**
     * DOG_CHAIN_PROP — heavy chain anchoring LURCHER_DOG to the caravan.
     * 0.30×0.20×1.20; hitsToBreak = 6 (yields SCRAP_METAL).
     * Breaking it frees the dog but triggers LURCHER_DOG aggression.
     */
    DOG_CHAIN_PROP(0.30f, 0.20f, 1.20f, 6, Material.SCRAP_METAL),

    // ── Issue #1315: Prison Van Escape — The Paddy Wagon Hustle ───────────────

    /**
     * POLICE_VAN_PROP — the transit vehicle used to transport arrested players.
     * 6.0×2.2×2.2; indestructible during transit (hitsToBreak = Integer.MAX_VALUE).
     * Yields SCRAP_METAL when destroyed after despawn (not modelled in prop stats).
     */
    POLICE_VAN_PROP(6.0f, 2.2f, 2.2f, Integer.MAX_VALUE, null),

    /**
     * VAN_CELL_DOOR_PROP — rear cage door with a padlock; the lockpick and brute-force target.
     * 1.0×2.0×0.1; hitsToBreak = 6 (yields SCRAP_METAL).
     */
    VAN_CELL_DOOR_PROP(1.0f, 2.0f, 0.1f, 6, Material.SCRAP_METAL),

    /**
     * VAN_BENCH_PROP — metal bench along the left wall; can be ripped loose for brute force.
     * 1.8×0.5×0.4; hitsToBreak = 3 (yields VAN_BENCH item).
     */
    VAN_BENCH_PROP(1.8f, 0.5f, 0.4f, 3, Material.VAN_BENCH),

    /**
     * VAN_VENT_PROP — small roof vent; kick open with ≥ 30% strength skill.
     * 0.4×0.4×0.1; hitsToBreak = 2 (yields nothing).
     */
    VAN_VENT_PROP(0.4f, 0.4f, 0.1f, 2, null),

    // ── Issue #1317: Northfield Bonfire Night ─────────────────────────────────

    /**
     * BONFIRE_PROP — a communal park bonfire built from pallets and timber.
     * 2.0×2.0×2.0; non-solid (cosmetic fire hazard). Emits warmth with double radius
     * compared to a campfire (10 blocks). Flickering orange light. Extinguishable by
     * FIRE_ENGINE response. Cannot be punched; yields nothing.
     */
    BONFIRE_PROP(2.0f, 2.0f, 2.0f, 0, null),

    /**
     * COLD_EMBERS_PROP — the burnt-out remains of the park bonfire after it dies down.
     * Replaces BONFIRE_PROP after the event ends or when suppressed by FIRE_ENGINE.
     * 2.0×0.3×2.0; purely cosmetic. Destroyed by 1 punch; yields SCRAP_METAL.
     */
    COLD_EMBERS_PROP(2.0f, 0.3f, 2.0f, 1, Material.SCRAP_METAL),

    /**
     * GUY_PROP — a penny-for-the-guy effigy crafted by the player from
     * NEWSPAPER + OLD_CLOTHES + HAT. Placed in the park for donations.
     * 0.6×1.8×0.4; destroyed by 3 punches (kicked over by YOUTH_GANG); yields nothing.
     */
    GUY_PROP(0.6f, 1.8f, 0.4f, 3, null),

    /**
     * FIREWORK_MORTAR_PROP — the official display launch tube at the Tesco car park.
     * 0.4×0.6×0.4; indestructible during the event. Interactable (E) to trigger
     * early launch (sabotage) or to plant a BANGER_FIREWORK inside.
     * Yields SCRAP_METAL after the event ends.
     */
    FIREWORK_MORTAR_PROP(0.4f, 0.6f, 0.4f, Integer.MAX_VALUE, Material.SCRAP_METAL),

    // ── Issue #1381: Northfield Halloween ─────────────────────────────────────

    /**
     * JACK_O_LANTERN_PROP — a carved pumpkin placed by the player from CARVED_PUMPKIN.
     * 0.4×0.4×0.4m; emits orange ambient glow via LightingSystem (+1 Neighbourhood VIBES).
     * Placed by right-clicking with CARVED_PUMPKIN.
     */
    JACK_O_LANTERN_PROP(0.4f, 0.4f, 0.4f, 3, Material.PUMPKIN_INNARDS),

    /**
     * EGGED_DOOR_PROP — a splattered egg decal applied to a door or car after being
     * hit with RAW_EGG. 0.1×0.8×0.8m; purely cosmetic, triggers NeighbourhoodSystem
     * VIBES loss and NoiseSystem event.
     */
    EGGED_DOOR_PROP(0.1f, 0.8f, 0.8f, 1, null),

    /**
     * ARGOS_MOTION_SENSOR_PROP — Dave's security motion sensor that arms at 17:30
     * on Halloween night. 0.2×0.2×0.2m; indestructible during event. Egging Dave's
     * car while armed triggers level-15 noise and Dave spawns angry.
     */
    ARGOS_MOTION_SENSOR_PROP(0.2f, 0.2f, 0.2f, Integer.MAX_VALUE, null),

    // ── Issue #1319: NatWest Cashpoint — The Dodgy ATM ───────────────────────

    /**
     * CASHPOINT_PROP — the lone NatWest cashpoint on the High Street.
     * 0.6×1.8×0.4m; highly durable (indestructible under normal conditions,
     * requires CROWBAR or ANGLE_GRINDER when out-of-service).
     * <ul>
     *   <li>Press E to interact (withdraw COIN, attach skimmer, or crack open
     *       when the machine is out of service).</li>
     *   <li>When cracked open with CROWBAR: 4s hold, noise 3.0, yields
     *       80–150 COIN + ENGINEER_ACCESS_CARD.</li>
     *   <li>When cracked open with ANGLE_GRINDER: 1.5s hold, noise 7.5,
     *       attracts POLICE within 20 blocks.</li>
     *   <li>Drops SCRAP_METAL when fully destroyed (indestructible during
     *       normal operation; only becomes destructible in OUT_OF_SERVICE mode).</li>
     * </ul>
     */
    CASHPOINT_PROP(0.6f, 1.8f, 0.4f, Integer.MAX_VALUE, Material.SCRAP_METAL),

    // ── Issue #1333: Northfield Employment System ─────────────────────────────

    /**
     * JOB_VACANCY_BOARD_PROP — a small corkboard outside each employer with
     * vacancy cards pinned to it. Press E to collect a JOB_APPLICATION_FORM.
     * Indestructible under normal conditions.
     */
    JOB_VACANCY_BOARD_PROP(0.6f, 0.9f, 0.1f, Integer.MAX_VALUE, null),

    /**
     * STAFF_CLOCK_IN_PROP — a wall-mounted punch-card / electronic clock-in
     * terminal inside each employer. Press E to clock in at shift start and
     * clock out at shift end.
     */
    STAFF_CLOCK_IN_PROP(0.3f, 0.5f, 0.1f, Integer.MAX_VALUE, null),

    /**
     * STOCK_CRATE_PROP — a wooden pallet or plastic crate of stock inside
     * the employer. Player must press E on this every 2 in-game minutes
     * during a shift to log productivity. Missing 3 tasks = SKIVING warning.
     */
    STOCK_CRATE_PROP(0.8f, 0.8f, 0.8f, 5, Material.CARDBOARD),

    // ── Issue #1335: Northfield Cycle Centre — Dave's Bikes ───────────────────

    /**
     * BIKE_RACK_PROP — a freestanding Sheffield-style metal bike rack found
     * outside the cycle shop, park entrance, and community centre. Player can
     * press E to park and lock their bike (requires BIKE_LOCK in inventory).
     * Indestructible; dropping a bike here makes it available as a
     * LOCKED_BIKE_PROP for other NPCs and the player to interact with.
     */
    BIKE_RACK_PROP(1.2f, 1.0f, 0.5f, Integer.MAX_VALUE, null),

    /**
     * LOCKED_BIKE_PROP — a bicycle secured to a rack or railing with a lock.
     * Three lock tiers determine hits to break:
     * <ul>
     *   <li>Basic lock: 3 hits (CROWBAR) / 1.5 s hold (ANGLE_GRINDER)</li>
     *   <li>Standard lock: 5 hits (CROWBAR) / 3 s hold (ANGLE_GRINDER)</li>
     *   <li>Heavy-duty lock: 8 hits (CROWBAR) / 5 s hold (ANGLE_GRINDER)</li>
     * </ul>
     * On successful unlock: drops STOLEN_BIKE, seeds BIKE_THEFT_RING rumour,
     * records BIKE_THEFT in CriminalRecord, adds WantedSystem star if witnessed.
     * Indestructible by normal means; only the lock can be cut.
     */
    LOCKED_BIKE_PROP(0.6f, 1.2f, 0.3f, Integer.MAX_VALUE, Material.STOLEN_BIKE),

    /**
     * JUST_EAT_DELIVERY_BOARD_PROP — a bright orange cork notice board outside
     * Dave's Cycle Centre listing active delivery jobs from the KebabVan and
     * Chippy. Press E to accept a delivery (requires DELIVERY_BAG in inventory).
     * Shows job destination, food type, time limit, and payout tier.
     * Indestructible.
     */
    JUST_EAT_DELIVERY_BOARD_PROP(0.6f, 0.9f, 0.1f, Integer.MAX_VALUE, null),

    // ── Issue #1337: Northfield Police Station — The Nick ─────────────────────

    /**
     * ENQUIRY_COUNTER_PROP — the public-side enquiry counter in the front lobby.
     * Staffed by DESK_SERGEANT Geoff (08:00–20:00) or CUSTODY_SERGEANT (20:00–08:00).
     * Press E to interact: voluntary surrender, information requests, or bribery.
     * Indestructible.
     */
    ENQUIRY_COUNTER_PROP(1.80f, 1.10f, 0.60f, Integer.MAX_VALUE, null),

    /**
     * CUSTODY_DOOR_PROP — the heavy reinforced door between the public lobby and
     * the custody suite. Normally locked; opened by DESK_SERGEANT on voluntary
     * surrender or by the player using a POLICE_KEY_CARD. Can be broken open
     * (12 hits with CROWBAR) but triggers a station-wide alert.
     * Drops SCRAP_METAL when destroyed.
     */
    CUSTODY_DOOR_PROP(1.00f, 2.20f, 0.20f, 12, Material.SCRAP_METAL),

    /**
     * EVIDENCE_LOCKER_PROP — a floor-to-ceiling metal cage in the evidence storage
     * room. Contains 3–6 confiscated items. Opened via three routes: POLICE_KEY_CARD,
     * ROPE_AND_HOOK (back window), or FIRE_ALARM_PROP distraction.
     * Breaking it (10 hits with CROWBAR) records EVIDENCE_TAMPERING.
     * Drops SCRAP_METAL when destroyed.
     */
    EVIDENCE_LOCKER_PROP(1.40f, 2.10f, 0.80f, 10, Material.SCRAP_METAL),

    /**
     * POLICE_GARAGE_PROP — the vehicle impound garage door on the side of the
     * station. Impounded vehicles are stored behind this door. Can be opened
     * by paying 20 COIN + presenting a DRIVING_LICENCE at the enquiry counter
     * (during 08:00–18:00), or broken open with a CROWBAR at night (8 hits).
     * Breaking it records VEHICLE_RECOVERY_OFFENCE.
     * Drops SCRAP_METAL when destroyed.
     */
    POLICE_GARAGE_PROP(2.00f, 2.40f, 0.20f, 8, Material.SCRAP_METAL),

    /**
     * FIRE_ALARM_PROP — a wall-mounted red break-glass fire alarm unit inside the
     * station corridor. Pressing E triggers a station evacuation for 90 seconds,
     * clearing all NPCs from the evidence room. One-time use per game session.
     * Records FALSE_ALARM in CriminalRecord if the player is caught near it.
     * Drops SCRAP_METAL when destroyed (4 hits).
     */
    FIRE_ALARM_PROP(0.30f, 0.30f, 0.12f, 4, Material.SCRAP_METAL),

    /**
     * BACK_WINDOW_PROP — a small frosted-glass window on the rear wall of the
     * station, one floor up. Accessible via ROPE_AND_HOOK from the back alley.
     * Broken silently with GLASS_CUTTER (1 hit, zero noise) or noisily with 3
     * punches (+50 noise). Provides entry to the evidence room without triggering
     * the custody door alarm. Drops GLASS when destroyed.
     */

    // ── Issue #1339: Council Enforcement Day ──────────────────────────────────

    /**
     * COUNCIL_NOTICE_PROP — an official council enforcement notice pinned to the
     * community centre noticeboard the evening before a Council Enforcement Day
     * (at 19:00 on days 13, 27, 41…). The notice warns residents of the next
     * day's multi-agency sweep. Pressing E reads the notice and seeds the
     * ENFORCEMENT_SWEEP rumour if not already seeded. Awards FOREWARNED
     * achievement on first interaction. Removed at 08:00 on the sweep day.
     * Indestructible (pinned notice).
     */
    COUNCIL_NOTICE_PROP(0.60f, 0.40f, 0.02f, Integer.MAX_VALUE, null),

    // ── Issue #1347: Northfield Remembrance Sunday ────────────────────────────

    /**
     * WREATH_PROP — a large poppy wreath laid at the base of the war memorial
     * (STATUE prop in the park) by the VICAR at 11:02 on Remembrance Sunday.
     * Stealing it (press E after 11:30) records MEMORIAL_VANDALISM in CriminalRecord,
     * adds Notoriety +10 and WantedSystem +2. Fenceable at PawnShop for 8–12 COIN.
     * Despawns naturally at 23:59 on Remembrance Sunday.
     * Drops POPPY (×3–5) when destroyed.
     */
    WREATH_PROP(0.60f, 0.30f, 0.60f, 3, Material.POPPY),

    // ── Issue #1353: Northfield Amateur Dramatics Society ─────────────────────

    /**
     * AUDITION_NOTICE_PROP — a paper notice pinned on the community centre noticeboard
     * advertising open auditions for the NAODS production of Blood Brothers. Pressing E
     * on Tuesdays triggers the BattleBarMiniGame audition sequence (3 cues). Available
     * every Tuesday until the last Saturday production of the month.
     * Indestructible (pinned notice).
     */
    AUDITION_NOTICE_PROP(0.50f, 0.40f, 0.02f, Integer.MAX_VALUE, null),

    /**
     * STAGE_MARK_PROP — a taped X on the community centre stage floor marking actor
     * positions during Blood Brothers rehearsals. Pressing E while standing on one
     * grants STEALTH XP (stage presence). 8–12 marks placed during rehearsal setup.
     * Indestructible (tape on floor).
     */
    STAGE_MARK_PROP(0.40f, 0.05f, 0.40f, Integer.MAX_VALUE, null),

    /**
     * COSTUME_CUPBOARD_PROP — a tall wooden wardrobe in the community centre back room
     * storing 3–5 STAGE_COSTUME items. Lockpickable (requires LOCKPICK) during the
     * Tuesday 13:00–17:00 heist window while Patricia is at the GP Surgery.
     * Awards BEST_IN_SHOW achievement on successful heist.
     * Drops STAGE_COSTUME on destroy.
     */
    COSTUME_CUPBOARD_PROP(0.80f, 1.80f, 0.50f, 8, Material.STAGE_COSTUME),

    /**
     * TICKET_BOOTH_PROP — a small wooden booth at the community centre entrance where
     * NAODS members sell opening night tickets (2 COIN each, 20 total). Player can
     * attempt to tout (resell at 4 COIN, risk enforcement) or present forged tickets
     * (30% caught). Present on the last Saturday of the month from 18:30.
     * Indestructible (built-in booth).
     */
    TICKET_BOOTH_PROP(0.80f, 1.20f, 0.80f, Integer.MAX_VALUE, null),

    /**
     * TICKET_CASH_BOX_PROP — a metal cash box behind the TICKET_BOOTH_PROP containing
     * 40 COIN from ticket sales. Stealable as part of Mario's sabotage options.
     * 6 hits to break open; drops COIN if destroyed.
     */
    TICKET_CASH_BOX_PROP(0.30f, 0.20f, 0.20f, 6, Material.COIN),

    /**
     * PROP_GUN_PROP — a locked cabinet on the community centre stage storing the
     * PROP_GUN used in the Blood Brothers production. Can be unlocked (press E) or
     * smashed open (6 hits). Mario's sabotage option: swap PROP_GUN with AIRGUN.
     * Drops PROP_GUN on destroy.
     */
    PROP_GUN_PROP(0.50f, 0.80f, 0.30f, 6, Material.PROP_GUN),

    // ── Issue #1357: Northfield Charity Fun Run ────────────────────────────────

    /**
     * FUN_RUN_CHECKPOINT_PROP — a numbered orange cone/flag waypoint marker on the
     * charity fun run course. Eight placed around the route. Player must pass through
     * all 8 in order to complete the run legitimately; skipping any counts as
     * course-cutting. Indestructible during the event. Drops nothing.
     */
    FUN_RUN_CHECKPOINT_PROP(0.40f, 1.20f, 0.40f, 999, null),

    /**
     * START_FINISH_ARCH_PROP — the inflatable start/finish arch at the Northfield
     * Community Centre car park. Janet (FUN_RUN_MARSHAL) stands beside it.
     * Press E to register (2 COIN). Cross it to start/finish the run timer.
     * Indestructible during the event. Drops nothing.
     */
    START_FINISH_ARCH_PROP(2.00f, 2.50f, 0.30f, 999, null),

    /**
     * WATER_STATION_PROP — a trestle table with paper cups of water on the course.
     * Press E to take WATER_CUP (+5 Hunger). Tip it over (press E + sprint) for chaos
     * (Vibes −1 if witnessed). Dropped by chaos: 0–3 WATER_CUP items scatter nearby.
     * Drops WATER_CUP on destroy.
     */
    WATER_STATION_PROP(1.00f, 0.90f, 0.50f, 4, Material.WATER_CUP),

    // ── Issue #1359: Northfield HMRC Tax Investigation ────────────────────────

    /**
     * HMRC_NOTICE_BOARD_PROP — an official HMRC notice board mounted near the player's
     * address after a TAX_DEMAND_LETTER is issued. Displays the outstanding tax demand
     * amount and deadline. Press E to read the full demand notice. Cannot be destroyed
     * (hitsToBreak: 999). Removed automatically when demand is settled or 7 days expire.
     * Drops nothing.
     */
    HMRC_NOTICE_BOARD_PROP(0.60f, 1.20f, 0.10f, 999, null),

    /**
     * BACK_WINDOW_PROP — a ground-floor rear window available as an escape route during
     * the dawn raid (day 5, 06:30). Press E to climb out and flee the property, awarding
     * DAWN_RAID_SURVIVOR achievement. Destructible by player (hitsToBreak: 2, GLASS).
     * Drops GLASS on destroy.
     */
    BACK_WINDOW_PROP(0.80f, 1.00f, 0.10f, 2, Material.GLASS),

    // ── Issue #1361: Northfield St. Margaret's Church Hall Jumble Sale ─────────

    /**
     * JUMBLE_TABLE_PROP — a long trestle table laden with bric-a-brac.
     * Present in St. Margaret's Church Hall from 08:00 Saturday. Interactable
     * for browsing (press E). First player/NPC to reach one within 5 s of 09:00
     * opening gets the EARLY_BIRD 30% discount. Indestructible (hitsToBreak: 999).
     * Drops nothing.
     */
    JUMBLE_TABLE_PROP(2.00f, 0.90f, 0.60f, 999, null),

    /**
     * DONATION_BOX_PROP — the donation intake point managed by Reverend Dave during
     * 08:00–09:00. Press E to donate items or start a volunteer sort shift. Items
     * donated here appear in the charity shop stock next Monday. Indestructible.
     * Drops nothing.
     */
    DONATION_BOX_PROP(0.80f, 1.00f, 0.80f, 999, null),

    /**
     * MYSTERY_BOX_PROP — one of three sealed auction boxes spawned near Reverend Dave
     * at 11:00. Contains random loot (junk/useful/SCORE tier). Player can plant a
     * BAIT_ITEM before 11:00. Revealed and distributed at 12:00 auction. Indestructible
     * by normal means (hitsToBreak: 999). Drops its contained MYSTERY_BOX material on
     * auction win.
     */
    MYSTERY_BOX_PROP(0.60f, 0.60f, 0.60f, 999, Material.MYSTERY_BOX),

    // ── Issue #1363: Northfield Sunday Car Boot Sale ───────────────────────────

    /**
     * BOOT_SALE_TABLE_PROP — a folding trestle table loaded with boot sale
     * junk. Spawned per BOOT_SALE_VENDOR pitch in the council car park on
     * Sundays 06:00–12:00. Player can browse items by pressing E. Takes 5
     * hits to overturn (drops its current stock items). Width/depth: 1.0f×0.5f.
     */
    BOOT_SALE_TABLE_PROP(1.0f, 0.80f, 0.50f, 5, null),

    /**
     * CAR_BOOT_PROP — open car boot (hatchback rear section) serving as
     * vendor storage display at the car boot sale. Decorative collision zone;
     * 8 hits to break. Drops VHS_TAPE on destruction.
     */
    CAR_BOOT_PROP(1.80f, 1.20f, 0.80f, 8, Material.VHS_TAPE),

    /**
     * PITCH_MARKER_PROP — painted kerb stone or chalked square marking a
     * vendor pitch slot in the car park. Indestructible (hitsToBreak: 999).
     * No material drop. Used by CarBootSaleSystem to track pitch assignments.
     */
    PITCH_MARKER_PROP(0.30f, 0.10f, 0.30f, 999, null),

    /**
     * BOOT_SALE_SIGN_PROP — A4 laminated sign reading "NORTHFIELD CAR BOOT
     * SALE — EVERY SUNDAY 6AM". Stapled to the car park entrance fence post.
     * 3 hits to remove. Drops null (sign tears). Press E to read event details.
     */
    BOOT_SALE_SIGN_PROP(0.20f, 0.50f, 0.05f, 3, null),

    // ── Issue #1367: Northfield Speed Awareness Course ────────────────────────

    /**
     * SPEED_CAMERA_PROP — a pole-mounted Gatso speed camera on the high street.
     * 0.40×2.50×0.40m; 10 hits to destroy; drops nothing (council property).
     * Active 24/7. Triggers SPEEDING_NOTICE when player drives above threshold.
     */
    SPEED_CAMERA_PROP(0.40f, 2.50f, 0.40f, 10, null),

    /**
     * SPEED_AWARENESS_BOARD_PROP — wall-mounted presentation board at the front
     * of Community Centre Room B. 2.0×1.5×0.1m; 3 hits to remove; drops nothing.
     * Player must be within 10 blocks during the course to count as attending.
     */
    SPEED_AWARENESS_BOARD_PROP(2.0f, 1.5f, 0.1f, 3, null),

    // ── Issue #1369: Northfield New Year's Eve ────────────────────────────────

    /**
     * NYE_STAGE_PROP — temporary event stage erected at the park bandstand on
     * New Year's Eve. 3.0×1.0×3.0m; survives the night (indestructible, 999 hits).
     * Spawned at 20:00 on day 365; EVENT_COMPERE Darren uses it for the countdown.
     */
    NYE_STAGE_PROP(3.0f, 1.0f, 3.0f, 999, null),

    /**
     * FIREWORK_ROCKET_PROP — decorative launch tube placed at the park edge.
     * 0.3×1.5×0.3m; 1 hit to remove; auto-launches at midnight triggering a
     * ParticleSystem burst + SoundEffect.FIREWORK_BANG.
     */
    FIREWORK_ROCKET_PROP(0.3f, 1.5f, 0.3f, 1, null),

    /**
     * FRONT_DOOR_PROP — the front door of a residential terraced house.
     * 1.0×2.0×0.1m; indestructible. Player presses E while holding COAL to
     * attempt a First Footing knock between 00:01–01:30 on day 1 of the new year.
     */
    FRONT_DOOR_PROP(1.0f, 2.0f, 0.1f, 999, null),

    /**
     * LANDLORD_HOUSE_PROP — Terry's house, the destination for helping Big Terry
     * home after the NYE lock-in. 3.0×3.0×3.0m; indestructible. Player must walk
     * within 5 blocks to complete the "help home" interaction.
     */
    LANDLORD_HOUSE_PROP(3.0f, 3.0f, 3.0f, 999, null),

    // ── Issue #1371: Northfield Christmas Market ──────────────────────────────

    /**
     * XMAS_MARKET_CHALET_PROP — a wooden market chalet (stall booth) used by
     * all six Christmas Market vendors near the war memorial. 2.0×2.5×1.5m;
     * indestructible during event. Drops WOOD when broken after event ends.
     */
    XMAS_MARKET_CHALET_PROP(2.0f, 2.5f, 1.5f, 999, Material.WOOD),

    /**
     * SANTA_GROTTO_PROP — a decorated grotto booth where SANTA_CLAUS (Terry)
     * receives SCHOOL_KID visitors. Hosts the GROTTO_TIN. 3.0×3.0×2.0m;
     * indestructible during event.
     */
    SANTA_GROTTO_PROP(3.0f, 3.0f, 2.0f, 999, null),

    /**
     * CAROL_SONG_BOARD_PROP — a board with printed carol lyrics where
     * CAROL_SINGER NPCs gather 17:00–19:00. 0.5×1.5×0.1m; fragile.
     */
    CAROL_SONG_BOARD_PROP(0.5f, 1.5f, 0.1f, 3, null),

    /**
     * RAFFLE_TICKET_DRUM_PROP — Margaret's charity raffle drum. Can be swapped
     * by the player (FENCE ≥ Journeyman) to guarantee winning ticket.
     * 0.6×0.8×0.6m; moderate durability.
     */
    RAFFLE_TICKET_DRUM_PROP(0.6f, 0.8f, 0.6f, 5, null),

    /**
     * GROTTO_TIN — the collection tin placed inside Santa's Grotto that
     * accumulates 1 COIN per SCHOOL_KID visitor (max 20/day). Stealable when
     * Terry is distracted. 0.2×0.3×0.2m; fragile.
     */
    GROTTO_TIN(0.2f, 0.3f, 0.2f, 2, Material.COIN),

    // ── Issue #1373: Northfield Local Council Elections ───────────────────────

    /**
     * LEAFLET_PILE_PROP — stack of campaign leaflets outside shops/estates during
     * canvassing week (days 83–89). Interact to collect CAMPAIGN_LEAFLET or (holding
     * PERMANENT_MARKER) to sabotage rival piles. 0.4×0.2×0.3m; fragile.
     */
    LEAFLET_PILE_PROP(0.4f, 0.2f, 0.3f, 2, Material.CAMPAIGN_LEAFLET),

    /**
     * CANDIDATE_TABLE_PROP — folding table staffed by a CANDIDATE_NPC during
     * canvassing week. Press E to pledge support or volunteer a leafleting shift.
     * Decorated with CAMPAIGN_LEAFLET stacks and CANDIDATE_MUG. 1.2×0.8×0.6m; sturdy.
     */
    CANDIDATE_TABLE_PROP(1.2f, 0.8f, 0.6f, 8, null),

    /**
     * VOTE_COUNT_TABLE_PROP — trestle table at the town hall used by returning
     * officer's staff to tally ballot papers on Count Night (22:30, day 90).
     * Interact during RECOUNT_DEMAND event to steal COUNT_SHEET. 1.8×0.9×0.6m; sturdy.
     */
    VOTE_COUNT_TABLE_PROP(1.8f, 0.9f, 0.6f, 8, Material.COUNT_SHEET),

    // ── Issue #1381: Northfield Bank Holiday Street Party ─────────────────────

    /**
     * TRESTLE_TABLE_PROP — decorated trestle table loaded with party food (SAUSAGE_ROLL,
     * CRISP_PACKET, WARM_LAGER). Knocked over by Tyke the dog if uncaught.
     * 1.8×0.8×0.6m; sturdy.
     */
    TRESTLE_TABLE_PROP(1.8f, 0.8f, 0.6f, 8, null),

    /**
     * DISPOSABLE_BBQ_PROP — three-state prop: UNLIT (12:00–12:30), LIT (burning normally),
     * OUT_OF_CONTROL (after petrol or random chance from 14:00). Interact to light/cook.
     * 0.4×0.1×0.4m; fragile.
     */
    DISPOSABLE_BBQ_PROP(0.4f, 0.1f, 0.4f, 2, null),

    /**
     * RAFFLE_DRUM_PROP — Brenda's prize raffle drum, operated 18:00–18:10.
     * Press E to buy a RAFFLE_TICKET or rig draw with STICKY_FINGERS tier 2.
     * 0.5×0.5×0.5m; sturdy.
     */
    RAFFLE_DRUM_PROP(0.5f, 0.5f, 0.5f, 8, null),

    /**
     * PRIZE_BOX_PROP — the prize box containing raffle winnings. Stealable by
     * the player; triggers THEFT + LOCAL_SCANDAL rumour.
     * 0.5×0.4×0.4m; fragile.
     */
    PRIZE_BOX_PROP(0.5f, 0.4f, 0.4f, 3, null),

    /**
     * TARPAULIN_PROP — improvised overhead shelter spawned when weather is DRIZZLE.
     * Provides basic rain cover over the trestle tables.
     * 3.0×0.1×2.0m; fragile.
     */
    TARPAULIN_PROP(3.0f, 0.1f, 2.0f, 2, null),

    // ── Issue #1383: Northfield Boxing Day Sales ──────────────────────────────

    /**
     * SALE_QUEUE_BARRIER_PROP — retractable crowd-control barrier outside Iceland.
     * Spawned at 05:30 on Boxing Day; defines the queue lane.
     * 1.2×0.9×0.1m; sturdy.
     */
    SALE_QUEUE_BARRIER_PROP(1.2f, 0.9f, 0.1f, 8, null),

    /**
     * WAYNE_VAN_PROP — Wayne's white Transit van parked outside Iceland.
     * Active 06:30–12:00 on Boxing Day. Player can buy, rob (CROWBAR), or undercut Wayne.
     * 4.0×2.0×2.0m; very sturdy.
     */
    WAYNE_VAN_PROP(4.0f, 2.0f, 2.0f, 20, null),

    /**
     * SALE_SIGN_PROP — bright red A-board sign: "BOXING DAY SALE — UP TO 50% OFF".
     * Spawned outside ICELAND, POUND_SHOP, and CHARITY_SHOP at 06:00. Despawns at 18:00.
     * 0.6×1.2×0.1m; fragile.
     */
    SALE_SIGN_PROP(0.6f, 1.2f, 0.1f, 2, null),

    // ── Issue #1386: Northfield St George's Day ───────────────────────────────

    /**
     * MORRIS_DANCE_AREA_PROP — a rope-off circle in the park where Morris Dancers perform.
     * Active 11:00–15:00 on St George's Day (dayCount % 365 == 113).
     * 4.0×0.1×4.0m; indestructible.
     */
    MORRIS_DANCE_AREA_PROP(4.0f, 0.1f, 4.0f, 999, null),

    /**
     * ST_GEORGE_MENU_PROP — laminated A4 "St George's Day Special" menu above the Wetherspoons bar.
     * Present all day on St George's Day. Not interactable directly.
     * 0.3×0.4×0.05m; fragile.
     */
    ST_GEORGE_MENU_PROP(0.3f, 0.4f, 0.05f, 1, null),

    /**
     * BAR_STOOL_PROP — tall wooden bar stool in Wetherspoons. Player can climb it (press E)
     * to reach the ST_GEORGE_FLAG_PROP mounted above the bar. Sturdy.
     * 0.4×0.8×0.4m; 5 hits.
     */
    BAR_STOOL_PROP(0.4f, 0.8f, 0.4f, 5, Material.WOOD),

    /**
     * DRAINPIPE_PROP — cast-iron drainpipe running up the back of the Wetherspoons building.
     * Hold E for 3 seconds to climb to the roof. Present year-round; only meaningful on
     * St George's Day. 0.3×4.0×0.3m; sturdy.
     */
    DRAINPIPE_PROP(0.3f, 4.0f, 0.3f, 10, null),

    /**
     * ROOF_FLAG_MOUNT_PROP — flag mount on the roof of Wetherspoons holding the ROOF_FLAG_PROP.
     * Press E to take the flag (CCTV must be disabled). 0.2×0.5×0.2m; sturdy.
     */
    ROOF_FLAG_MOUNT_PROP(0.2f, 0.5f, 0.2f, 8, null),

    // ── Issue #1394: England Match Night ─────────────────────────────────────

    /**
     * PUB_TV_PROP — large wall-mounted screen inside Wetherspoons showing the England match.
     * Sabotable via CROWBAR or CABLE item during Dave's blind spot (half-time 20:45–21:00).
     * State: FUNCTIONAL / BROKEN. 1.8×1.2×0.15m; sturdy.
     */
    PUB_TV_PROP(1.8f, 1.2f, 0.15f, 10, null),

    // ── Issue #1396: Northfield Royal Mail Strike ─────────────────────────────

    /**
     * OVERFLOW_DEPOT_PROP — locked building prop behind the sorting office.
     * Contains 20 PARCEL items during the strike. Requires CROWBAR (3 uses) or
     * BOLT_CUTTER (1 use) to open. Drops nothing on destruction.
     * 3.0×2.5×3.0m; very sturdy (20 hits).
     */
    OVERFLOW_DEPOT_PROP(3.0f, 2.5f, 3.0f, 20, null),

    /**
     * COURIER_VAN_PROP — battered Transit van parked outside the corner shop.
     * Interactable 09:00–18:00 on strike days to start unofficial courier mode.
     * 4.5×1.8×2.0m; indestructible for gameplay purposes.
     */
    COURIER_VAN_PROP(4.5f, 1.8f, 2.0f, 99, null),

    // ── Issue #1398: Northfield Window Cleaner ────────────────────────────────

    /**
     * LADDER_PROP — Terry's aluminium extension ladder leaned against a house wall.
     * Climbable by the player (burglary route to upstairs windows). Present for 90 in-game
     * seconds at each property. Removed when Terry moves on.
     * 0.6×4.0×0.2m; 99 hits (indestructible — Terry takes it with him).
     */
    LADDER_PROP(0.6f, 4.0f, 0.2f, 99, null),

    /**
     * WINDOW_CLEANING_VAN_PROP — Terry's white Transit van parked on the street near his route.
     * Contains BUCKET_AND_CHAMOIS (stealable). Indestructible for gameplay purposes.
     * 4.5×1.8×2.0m.
     */
    WINDOW_CLEANING_VAN_PROP(4.5f, 1.8f, 2.0f, 99, null),

    // ── Issue #1402: Northfield Severn Trent Road Dig ─────────────────────────

    /**
     * ROAD_TRENCH_PROP — Open trench cut across Northfield Road by Severn Trent.
     * 4 blocks wide, 2 blocks deep. Blocks pedestrian and vehicle movement.
     * Present for the duration of the dig event (5 in-game days from Monday 08:00).
     * 4.0×2.0×4.0m; indestructible (99 hits) — only removed when dig ends.
     */
    ROAD_TRENCH_PROP(4.0f, 2.0f, 4.0f, 99, null),

    /**
     * ORANGE_BARRIER_PROP — Plastic traffic barrier (12 placed around trench perimeter).
     * Kickable: hold E for 1 second to remove. Can be re-placed by player.
     * 1.2×1.0×0.5m; 1 hit to kick over (not destroyed — just displaced).
     */
    ORANGE_BARRIER_PROP(1.2f, 1.0f, 0.5f, 1, null),

    /**
     * TEMP_TRAFFIC_LIGHT_PROP — Temporary traffic signal on a 90-second cycle.
     * Can be sabotaged (BattleBarMiniGame EASY) to stick both lights on green.
     * 0.3×2.5×0.3m; 8 hits to destroy; drops null (it's council property).
     */
    TEMP_TRAFFIC_LIGHT_PROP(0.3f, 2.5f, 0.3f, 8, null),

    /**
     * WELFARE_CABIN_PROP — Site welfare unit (portable cabin) on the pavement.
     * Contains loot (THERMOS always, others probabilistic). Locked 08:00–22:00 (E shows "Locked").
     * Night raid (22:00–05:00): press E to loot — CrimeType.THEFT_FROM_WORKSITE, Notoriety +3.
     * 6.0×2.8×2.4m; indestructible (99 hits).
     */
    WELFARE_CABIN_PROP(6.0f, 2.8f, 2.4f, 99, null),

    /**
     * BURIED_STASH_PROP — Hidden under one of the trench blocks.
     * Location revealed by CONTRACTOR_GOODWILL ≥ 40 or BRIBE_HIGH.
     * Contains 5–10 COIN + MYSTERY_OBJECT. Press E to loot.
     * 0.5×0.5×0.5m; 1 hit (fragile mud cover); drops MYSTERY_OBJECT.
     */
    BURIED_STASH_PROP(0.5f, 0.5f, 0.5f, 1, Material.MYSTERY_OBJECT),

    // ── Issue #1406: Northfield Dodgy Roofer ──────────────────────────────────

    /**
     * ROOFER_VAN_PROP — Kenny's white transit van parked outside target properties.
     * Unattended while Kenny is on a doorstep or up a ladder (BUSY state).
     * CROWBAR + 3-second hold to break in — yields loot table (BUCKET_OF_SEALANT always,
     * SCAFFOLDING_SPANNER 50%, INVOICE_PAD 35%, CASH_ENVELOPE 20%, ROOF_SLATE_BAG 15%).
     * CrimeType.VEHICLE_BREAK_IN + Notoriety +6 on successful break-in.
     * 5.5×2.2×2.0m; indestructible (99 hits); drops null.
     */
    ROOFER_VAN_PROP(5.5f, 2.2f, 2.0f, 99, null),

    /**
     * INVOICE_PROP — Forged follow-up invoice left at a recently-worked house door.
     * Created via Mechanic 5 (INVOICE_PAD required). 30% acceptance → 8 COIN.
     * Triggers CrimeType.FRAUD + Notoriety +5 on use.
     * 0.3×0.4×0.05m; 1 hit (paper); drops INVOICE_PAD (partial use).
     */
    INVOICE_PROP(0.3f, 0.4f, 0.05f, 1, Material.INVOICE_PAD),

    /**
     * TRADING_STANDARDS_OFFICE_PROP — Front desk prop for the Trading Standards office.
     * Player can report Kenny here (Notoriety &lt; 25) for Notoriety −3 + PUBLIC_SPIRITED achievement.
     * Also used for CIVIC_MINDED + SNITCH rumour tip-off mechanic.
     * 1.2×1.0×0.6m; indestructible (99 hits); drops null.
     */
    TRADING_STANDARDS_OFFICE_PROP(1.2f, 1.0f, 0.6f, 99, null),

    // ── Issue #1416: Northfield Mobile Speed Camera Van ───────────────────────

    /**
     * SPEED_CAMERA_VAN_PROP — Sharon's mobile speed camera GATSO van.
     * Parked near Northfield school weekdays 08:00–09:30 and 15:30–17:00.
     * Hold E for 4 seconds while Sharon is distracted to steal SPEED_CAMERA_SD_CARD.
     * Can be disabled via GraffitiSystem (lens fogging), CROWBAR (tyre slash), or LIGHTER (arson).
     * 5.5×2.2×2.0m; indestructible (99 hits); drops null.
     */
    SPEED_CAMERA_VAN_PROP(5.5f, 2.2f, 2.0f, 99, null),

    /**
     * HANDWRITTEN_WARNING_SIGN_PROP — a player-crafted cardboard warning sign placed on the road.
     * Crafted from MARKER_PEN + CARDBOARD. Warns drivers automatically for 10 in-game minutes.
     * 0.4×0.6×0.05m; 1 hit (fragile card); drops HANDWRITTEN_WARNING_SIGN.
     */
    HANDWRITTEN_WARNING_SIGN_PROP(0.4f, 0.6f, 0.05f, 1, ragamuffin.building.Material.HANDWRITTEN_WARNING_SIGN),

    /**
     * TABLOID_RACK_PROP — a tabloid newspaper rack near Sharon's van.
     * Player interacts (E) to distract Sharon for 25 in-game seconds.
     * 0.3×1.2×0.3m; indestructible (99 hits); drops null.
     */
    TABLOID_RACK_PROP(0.3f, 1.2f, 0.3f, 99, null),

    // ── Issue #1420: Northfield Post Office Horizon Scandal ──────────────────

    /**
     * SHORTFALL_LETTER_PROP — A4 demand letter pinned to the Post Office counter.
     * Readable via E-interaction: displays "Post Office Ltd demands repayment of £340 shortfall
     * recorded by Horizon system. Failure to comply within 14 days may result in prosecution."
     * Stealing it (hold-E) records {@code AUDIT_OBSTRUCTION} crime.
     * Dims: 0.3 × 0.02 × 0.4; indestructible; drops {@link ragamuffin.building.Material#SHORTFALL_LETTER}.
     */
    SHORTFALL_LETTER_PROP(0.3f, 0.02f, 0.4f, 99, Material.SHORTFALL_LETTER),

    /**
     * TRANSACTION_LOG_PROP — stack of Horizon terminal printouts in the Post Office back-room
     * filing cabinet. Contains 3 collectible {@link ragamuffin.building.Material#TRANSACTION_LOG}
     * units. Accessible with Maureen's trust (COMMUNITY_RESPECT ≥ 20) or by LOCKPICK/CROWBAR
     * break-in while Derek is absent.
     * Dims: 0.4 × 0.05 × 0.3; breaks in 1 hit; drops TRANSACTION_LOG.
     */
    TRANSACTION_LOG_PROP(0.4f, 0.05f, 0.3f, 1, Material.TRANSACTION_LOG),

    /**
     * WITNESS_BOX_PROP — the witness stand in MagistratesCourtSystem where the player
     * testifies on day 17. Press E within 5 blocks to deliver testimony.
     * Dims: 1.0 × 1.2 × 1.0; indestructible; drops null.
     */
    WITNESS_BOX_PROP(1.0f, 1.2f, 1.0f, 99, null),

    // ── Issue #1422: Northfield Charity Sponsored Walk ────────────────────────

    /**
     * ROUTE_CONE_PROP — orange traffic cone, 20 placed along the walk route at 20-block intervals.
     * Removable by player (2 punches). Becomes {@link ragamuffin.building.Material#TRAFFIC_CONE}
     * Material in inventory.
     * Dims: 0.3 × 0.6 × 0.3.
     */
    ROUTE_CONE_PROP(0.3f, 0.6f, 0.3f, 2, ragamuffin.building.Material.TRAFFIC_CONE),

    /**
     * PRIZE_ENVELOPE_PROP — brown envelope on trestle table outside the Community Centre.
     * Contains 15–25 COIN + {@link ragamuffin.building.Material#CHARITY_RAFFLE_TICKET}.
     * Dims: 0.2 × 0.02 × 0.12. Indestructible (grabbed via E interact).
     */
    PRIZE_ENVELOPE_PROP(0.2f, 0.02f, 0.12f, 99, null),

    // ── Issue #1424: Northfield Doorstep Energy Tout ──────────────────────────

    /**
     * TOUT_CLIPBOARD_PROP — Craig's PowerSave UK branded clipboard with resident contact list.
     * Placed on Craig's person while he does his round; drops on the ground if he is
     * knocked out or when the round ends.
     * <ul>
     *   <li>Pickpocket (Stealth ≥ 1) or assault Craig to steal.</li>
     *   <li>Fence for 12 COIN, or use to run Craig's round door-to-door (up to 6 addresses).</li>
     *   <li>3+ doorstep knocks records {@code DOORSTEP_FRAUD}; triggers Dave ({@link ragamuffin.entity.NPCType#TOUT_ENFORCER}) spawn.</li>
     * </ul>
     * Dims: 0.3 × 0.02 × 0.4; indestructible; drops {@link ragamuffin.building.Material#TOUT_CLIPBOARD}.
     */
    TOUT_CLIPBOARD_PROP(0.3f, 0.02f, 0.4f, 99, ragamuffin.building.Material.TOUT_CLIPBOARD),

    /**
     * SMART_METER_KIT_PROP — crate of PowerSave UK smart meter install equipment in Craig's van.
     * Accessible by breaking the van's GLASS window (2 hits) with a NoiseSystem spike.
     * <ul>
     *   <li>Fence for 18 COIN, install in squat for fake prestige, or trade to
     *       {@link ragamuffin.entity.NPCType#PIGEON_FANCIER} for 10 COIN + {@code FANCIER_FAVOUR} flag.</li>
     *   <li>Stealing it records {@code VEHICLE_BREAK_IN} crime.</li>
     * </ul>
     * Dims: 0.6 × 0.5 × 0.6; 2 hits to break (window); drops {@link ragamuffin.building.Material#SMART_METER_KIT}.
     */
    SMART_METER_KIT_PROP(0.6f, 0.5f, 0.6f, 2, ragamuffin.building.Material.SMART_METER_KIT),

    /**
     * ENERGY_VAN_PROP — white PowerSave UK transit van, parked on the terrace street during Craig's round.
     * Target of the Path C smart meter sabotage mechanic.
     * <ul>
     *   <li>Glass window takes 2 hits to smash; breaking it triggers a NoiseSystem spike (+25).</li>
     *   <li>Contains {@link #SMART_METER_KIT_PROP} accessible after window is broken.</li>
     *   <li>Parked only during Craig's active round; despawns when Craig's round ends or Craig is removed.</li>
     * </ul>
     * Dims: 2.0 × 1.8 × 4.5; 8 hits (body) / 2 hits (window); drops {@code SCRAP_METAL}.
     */
    ENERGY_VAN_PROP(2.0f, 1.8f, 4.5f, 8, ragamuffin.building.Material.SCRAP_METAL),

    // ── Issue #1426: Northfield Neighbourhood WhatsApp Group ─────────────────

    /**
     * WHATSAPP_FEED_TERMINAL_PROP — Alternative access terminal at the library or internet café.
     * Same E-interaction as {@code COMMUNITY_NOTICE_BOARD} but indoors.
     * Dims: 0.5 × 1.0 × 0.4; 4 hits to break; drops WOOD.
     */
    WHATSAPP_FEED_TERMINAL_PROP(0.5f, 1.0f, 0.4f, 4, ragamuffin.building.Material.WOOD),

    // ── Issue #1428: Northfield Council CCTV Audit ───────────────────────────

    /**
     * CCTV_MONITOR_PROP — stack of four small monitors in Keith's portacabin.
     * Press E to view live feeds (reveals all live camera IDs). Achievement: BACK_ROOM_ACCESS.
     * Dims: 0.8 × 0.6 × 0.4; 3 hits to break; drops SCRAP_METAL.
     */
    CCTV_MONITOR_PROP(0.8f, 0.6f, 0.4f, 3, ragamuffin.building.Material.SCRAP_METAL),

    /**
     * CCTV_TAPE_UNFILED_PROP — glowing VHS tape on Keith's desk.
     * 5-minute retrieval window after a taped crime. Press E to steal.
     * Dims: 0.15 × 0.10 × 0.20; 1 hit to break; drops CCTV_FOOTAGE.
     */
    CCTV_TAPE_UNFILED_PROP(0.15f, 0.10f, 0.20f, 1, ragamuffin.building.Material.CCTV_FOOTAGE),

    /**
     * CCTV_PORTACABIN_PROP — Keith's base behind the JobCentre. Lockable.
     * Dims: 4.0 × 3.0 × 6.0; 8 hits to break; drops WOOD.
     */
    CCTV_PORTACABIN_PROP(4.0f, 3.0f, 6.0f, 8, ragamuffin.building.Material.WOOD),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1433: Northfield Easter Weekend
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * EASTER_EGG_PROP — A hidden foil egg placed by Brenda during the council egg hunt.
     * Dims: 0.3 × 0.2 × 0.3; 1 hit to break; drops FOIL_EASTER_EGG.
     * Player collects by pressing E within 1 block.
     */
    EASTER_EGG_PROP(0.3f, 0.2f, 0.3f, 1, ragamuffin.building.Material.FOIL_EASTER_EGG),

    /**
     * EASTER_BANNER_PROP — Northfield Easter Weekend decorative banner strung across streets.
     * Dims: 3.0 × 0.5 × 0.1; 2 hits to break; drops WOOD.
     */
    EASTER_BANNER_PROP(3.0f, 0.5f, 0.1f, 2, ragamuffin.building.Material.WOOD),

    /**
     * MOTORBIKE_PROP — A parked charity-parade motorbike.
     * Dims: 2.0 × 1.2 × 0.8; 5 hits to break; drops BIKER_JACKET (via EasterSystem logic).
     * STEALTH ≥ 2 required for 3-second E-hold steal.
     */
    MOTORBIKE_PROP(2.0f, 1.2f, 0.8f, 5, ragamuffin.building.Material.BIKER_JACKET),

    /**
     * CHARITY_BUCKET_PROP — Easter Egg Run charity collection bucket at park entrance.
     * Dims: 0.4 × 0.5 × 0.4; 1 hit to break; drops CHARITY_BUCKET_EASTER.
     * Donate or steal — theft triggers Notoriety +8, Wanted +1, all BIKER_NPCs HOSTILE.
     */
    CHARITY_BUCKET_PROP(0.4f, 0.5f, 0.4f, 1, ragamuffin.building.Material.CHARITY_BUCKET_EASTER),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1435: Northfield Community Speedwatch
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TRIPOD_SPEED_GUN_PROP — Keith's calibrated speed gun on an aluminium tripod stand.
     * Dims: 1.0 × 1.5 × 1.0; 2 hits to destroy (aluminium frame); drops SCRAP_METAL.
     * Spawned by CommunitySpeedwatchSystem at SPEEDWATCH_POSITION during active sessions.
     * Can be stolen by holding E for 4 seconds while Keith is distracted.
     * Theft: CrimeType THEFT, Notoriety +6, WantedSystem +1, session ends immediately.
     */
    TRIPOD_SPEED_GUN_PROP(1.0f, 1.5f, 1.0f, 2, ragamuffin.building.Material.SCRAP_METAL),

    /**
     * SPEEDWATCH_SIGN_PROP — Roadside A-board: "COMMUNITY SPEEDWATCH IN OPERATION. SLOW DOWN."
     * Dims: 0.5 × 1.2 × 0.4; 1 hit to destroy; drops WOOD.
     * Graffiti-able (GraffitiSystem): Notoriety +3, CrimeType CRIMINAL_DAMAGE.
     * Keith re-erects the sign next session with "AND RESPECT COMMUNITY VOLUNTEERS."
     */
    SPEEDWATCH_SIGN_PROP(0.5f, 1.2f, 0.4f, 1, ragamuffin.building.Material.WOOD),

    // ── Issue #1439: Welcome sign ─────────────────────────────────────────────

    /**
     * WELCOME_SIGN — a large physical town welcome sign placed at the main entrance
     * to the park, reading "Welcome to Northfield". Two wooden posts support a wide
     * green sign panel. Dims: 4.0 × 2.0 × 0.3 m; hardness 5; drops WOOD.
     */
    WELCOME_SIGN(4.0f, 2.0f, 0.3f, 5, ragamuffin.building.Material.WOOD),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1445: Northfield Salvation Army Citadel
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SHELTER_BED_PROP — a folding single-frame camp bed with a thin mattress and
     * scratchy blanket, set up in the Salvation Army Citadel night shelter.
     * Spawned by SalvationArmyCitadelSystem (Fri–Sat 20:00–08:00); 4 beds maximum.
     * Player can sleep in an unoccupied bed (+20 Warmth, +20 HP over 60 seconds).
     * Dims: 0.8 × 0.5 × 2.0; indestructible (hitsToBreak: 999); drops nothing.
     */
    SHELTER_BED_PROP(0.8f, 0.5f, 2.0f, 999, null);

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

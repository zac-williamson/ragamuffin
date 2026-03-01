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
    DONATION_BOX_PROP(0.60f, 0.80f, 0.60f, 5, Material.CARDBOARD),

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
    COPPER_PIPE_PROP(0.3f, 0.3f, 0.6f, 5, Material.SCRAP_METAL);

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

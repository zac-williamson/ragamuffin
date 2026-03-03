package ragamuffin.building;

import com.badlogic.gdx.graphics.Color;

/**
 * Material types that can be collected from breaking blocks.
 * These are items in the player's inventory.
 */
public enum Material {
    WOOD("Wood"),
    BRICK("Brick"),
    GLASS("Glass"),
    STONE("Stone"),
    DIAMOND("Diamond"),
    COMPUTER("Computer"),
    OFFICE_CHAIR("Office Chair"),
    STAPLER("Stapler"),
    GRASS_TURF("Grass Turf"),
    DIRT("Dirt"),
    PAVEMENT_SLAB("Pavement Slab"),
    ROAD_ASPHALT("Road Asphalt"),
    PLANKS("Planks"),
    SHELTER_WALL("Shelter Wall"),
    SHELTER_FLOOR("Shelter Floor"),
    SHELTER_ROOF("Shelter Roof"),
    BRICK_WALL("Brick Wall"),
    WINDOW("Window"),
    SAUSAGE_ROLL("Sausage Roll"),
    STEAK_BAKE("Steak Bake"),
    CARDBOARD("Cardboard"),
    IMPROVISED_TOOL("Improvised Tool"),
    STONE_TOOL("Stone Tool"),
    CHIPS("Chips"),
    KEBAB("Kebab"),
    ENERGY_DRINK("Energy Drink"),
    CRISPS("Crisps"),
    TIN_OF_BEANS("Tin of Beans"),
    CONCRETE("Concrete"),
    ROOF_TILE("Roof Tile"),
    TARMAC("Tarmac"),
    SCRAP_METAL("Scrap Metal"),
    RENDER("Render"),
    RENDER_CREAM("Cream Render"),
    RENDER_PINK("Pink Render"),
    SLATE("Slate"),
    PEBBLEDASH("Pebbledash"),
    DOOR("Door"),
    LINOLEUM("Linoleum"),
    LINO_GREEN("Green Lino"),
    YELLOW_BRICK("Yellow Brick"),
    TILE("Tile"),
    TILE_BLACK("Black Tile"),
    COUNTER("Counter"),
    SHELF("Shelf"),
    TABLE("Table"),
    CARPET("Carpet"),
    FENCE("Fence"),
    SIGN("Sign"),
    SIGN_RED("Red Sign"),
    SIGN_BLUE("Blue Sign"),
    SIGN_GREEN("Green Sign"),
    SIGN_YELLOW("Yellow Sign"),
    GARDEN_WALL("Garden Wall"),
    BOOKSHELF("Bookshelf"),
    METAL_RED("Red Metal"),
    PINT("Pint"),
    PERI_PERI_CHICKEN("Peri-Peri Chicken"),
    SCRATCH_CARD("Scratch Card"),
    NEWSPAPER("Newspaper"),
    WASHING_POWDER("Washing Powder"),
    PARACETAMOL("Paracetamol"),
    TEXTBOOK("Textbook"),
    HYMN_BOOK("Hymn Book"),
    PETROL_CAN("Petrol Can"),
    HAIR_CLIPPERS("Hair Clippers"),
    HAIR_CLIPPERS_BROKEN("Broken Hair Clippers"),  // Issue #944: exhausted after 3 DIY uses
    NAIL_POLISH("Nail Polish"),
    BROKEN_PHONE("Broken Phone"),
    DODGY_DVD("Dodgy DVD"),
    FIRE_EXTINGUISHER("Fire Extinguisher"),
    PLYWOOD("Plywood"),
    PIPE("Pipe"),
    CARDBOARD_BOX("Cardboard Box"),
    ANTIDEPRESSANTS("Antidepressants"),
    STAIRS("Stairs"),
    LADDER("Ladder"),
    HALF_BLOCK("Half Block"),
    SHILLING("Shilling"),
    PENNY("Penny"),
    HIGH_VIS_JACKET("High-Vis Jacket"),
    CROWBAR("Crowbar"),
    ANGLE_GRINDER("Angle Grinder"),
    BALACLAVA("Balaclava"),
    BOLT_CUTTERS("Bolt Cutters"),
    DODGY_PASTY("Dodgy Pasty"),
    FOOD("Food"),  // generic currency used by the Fence to pay for stolen goods
    COAL("Coal"),        // Mined from underground coal ore
    IRON("Iron"),        // Mined from underground iron ore seams
    FLINT("Flint"),      // Knapped flint from deep stone, useful for improvised tools
    COIN("Coin"),        // Currency dropped by hostile NPCs; used to buy drinks at the pub
    COAT("Coat"),              // Clothing: reduces warmth drain outdoors
    UMBRELLA("Umbrella"),      // Clothing: blocks wetness accumulation in rain
    WOOLLY_HAT("Woolly Hat"),  // Clothing: reduces warmth drain from cold/frost
    FLASK_OF_TEA("Flask of Tea"), // Consumable: restores warmth instantly

    // ── Heist tools & loot (Phase O / Issue #704) ──────────────────────────────

    /** Crafted from DIAMOND×1 + WOOD×1. Removes GLASS silently in 1 hit (zero noise). */
    GLASS_CUTTER("Glass Cutter"),

    /** Crafted from WOOD×4 + LEAVES×2. Deployable ladder on vertical surface for 60 seconds. */
    ROPE_LADDER("Rope Ladder"),

    /** Leaf bundle — drops from LEAVES blocks, used in ROPE_LADDER recipe. */
    LEAVES("Leaves"),

    /** Jewellery loot from jeweller safe. */
    GOLD_RING("Gold Ring"),

    /** Document loot from JobCentre. */
    COUNCIL_ID("Council ID"),

    /** Food loot from Greggs raid. */
    PASTY("Pasty"),

    // ── Issue #712: Slumlord property system ──────────────────────────────────

    /** Proof of property ownership — issued by the ESTATE_AGENT on purchase. */
    DEED("Deed"),

    /** Paint tin — used to repair buildings and raise their Condition score. */
    PAINT_TIN("Paint Tin"),

    /** Eviction notice — removes a THUG NPC from a player-owned building. */
    EVICTION_NOTICE("Eviction Notice"),

    // ── Issue #714: Player Squat system ──────────────────────────────────────

    /** Barricade block material — crafted from 2 WOOD + 1 BRICK. Absorbs 3 hits. */
    BARRICADE("Barricade"),

    /** Lockpick — crafted at squat WORKBENCH. Reduces safe-crack time by 3 seconds. */
    LOCKPICK("Lockpick"),

    /** Fake ID — crafted at squat WORKBENCH. Removes 1 criminal record offence at police station. */
    FAKE_ID("Fake ID"),

    // ── Issue #716: Underground Music Scene ──────────────────────────────────

    /**
     * Microphone — required to initiate an MC Battle against a faction champion.
     * Crafted from 1 SCRAP_METAL + 1 WIRE (PIPE). Held item; consumed on battle start.
     */
    MICROPHONE("Microphone"),

    /**
     * Rave Flyer — used to announce an illegal rave at the player's squat.
     * Crafted from 2 PAPER (NEWSPAPER) + 1 PAINT_TIN. Single use; triggers RAVE_ANNOUNCEMENT rumour.
     */
    FLYER("Flyer"),

    // ── Issue #720: Craftable 3D prop items ──────────────────────────────────

    /**
     * Crafted bed prop — place inside the squat to provide +10 Vibe and enable
     * lodger hosting. Crafted from 4 WOOD + 2 PLANKS.
     */
    PROP_BED("Bed"),

    /**
     * Crafted workbench prop — place inside the squat to unlock advanced crafting
     * recipes (BARRICADE, LOCKPICK, FAKE_ID). Crafted from 6 PLANKS + 2 SCRAP_METAL.
     */
    PROP_WORKBENCH("Workbench"),

    /**
     * Crafted dartboard prop — place inside the squat for +7 Vibe.
     * Crafted from 3 WOOD + 1 SCRAP_METAL.
     */
    PROP_DARTBOARD("Dartboard"),

    /**
     * Crafted speaker stack prop — rave equipment, +5 capacity and +1 COIN/attendee/min.
     * Crafted from 4 SCRAP_METAL + 2 WOOD.
     */
    PROP_SPEAKER_STACK("Speaker Stack"),

    /**
     * Crafted disco ball prop — rave equipment, +3 rave capacity.
     * Crafted from 2 GLASS + 1 SCRAP_METAL.
     */
    PROP_DISCO_BALL("Disco Ball"),

    /**
     * Crafted DJ decks prop — rave equipment, enables DJ recruitment (doubles income).
     * Crafted from 3 SCRAP_METAL + 2 WIRE (PIPE).
     */
    PROP_DJ_DECKS("DJ Decks"),

    // ── Issue #765: Witness & Evidence System ────────────────────────────────

    /**
     * Rumour note — crafted from 1 COIN + a barman interaction (buying a drink).
     * Used to tip off police (press E on POLICE NPC while holding it): clears one
     * of the player's own CriminalRecord entries but seeds a BETRAYAL rumour that
     * turns the tipped faction hostile.
     */
    RUMOUR_NOTE("Rumour Note"),

    /**
     * CCTV Tape — a stealable prop found in office blocks and off-licences.
     * If a crime happens nearby and the tape is not stolen within 3 in-game minutes,
     * police automatically gain a WITNESSED_CRIME entry. Stolen tapes can be sold to
     * the fence for 15 COIN.
     */
    CCTV_TAPE("CCTV Tape"),

    // ── Issue #767: Disguise & Social Engineering System ─────────────────────

    /**
     * Police uniform — looted from a knocked-out POLICE NPC.
     * Wearing it stops police chasing the player (until cover is blown).
     */
    POLICE_UNIFORM("Police Uniform"),

    /**
     * Council jacket — looted from a knocked-out COUNCIL_BUILDER or COUNCIL_MEMBER NPC.
     * Wearing it lets the player demolish structures without triggering builders.
     */
    COUNCIL_JACKET("Council Jacket"),

    /**
     * Marchetti tracksuit — looted from a knocked-out MARCHETTI_CREW NPC.
     * Wearing it grants safe passage through Marchetti-controlled territory.
     */
    MARCHETTI_TRACKSUIT("Marchetti Tracksuit"),

    /**
     * Street Lads hoodie — looted from a knocked-out STREET_LADS NPC.
     * Wearing it grants safe passage through Street Lads territory.
     */
    STREET_LADS_HOODIE("Street Lads Hoodie"),

    /**
     * Hi-vis vest disguise — looted from a knocked-out worker NPC.
     * Wearing it grants access to construction sites and service areas.
     */
    HI_VIS_VEST("Hi-Vis Vest"),

    /**
     * Greggs apron — looted from a knocked-out Greggs worker.
     * Comedy gag: works perfectly beyond 3 blocks, immediately transparent within 3 blocks.
     */
    GREGGS_APRON("Greggs Apron"),

    // ── Issue #769: Dynamic NPC Needs & Black Market Economy ─────────────────

    /**
     * Greggs pastry — sold or given to hungry NPCs to satisfy HUNGRY need.
     * Spikes in price during GREGGS_STRIKE market event.
     */
    GREGGS_PASTRY("Greggs Pastry"),

    /**
     * Can of lager — sold to NPCs to satisfy BORED/DESPERATE needs.
     * Spikes in price during LAGER_SHORTAGE market event.
     */
    CAN_OF_LAGER("Can of Lager"),

    /**
     * Plastic Bag — given free with any purchase at Khan's Off-Licence.
     * Provides +1 inventory slot while carried.
     */
    PLASTIC_BAG("Plastic Bag"),

    /**
     * Cigarette — sold to NPCs to satisfy DESPERATE need.
     * Affected by COUNCIL_CRACKDOWN event (restricted supply).
     */
    CIGARETTE("Cigarette"),

    /**
     * Woolly hat — sold to cold NPCs to satisfy COLD need.
     * High demand during COLD_SNAP market event.
     */
    WOOLLY_HAT_ECONOMY("Woolly Hat (Economy)"),

    /**
     * Sleeping bag — sold to desperate NPCs (SCARED/DESPERATE).
     * Available at black market; not sold in shops.
     */
    SLEEPING_BAG("Sleeping Bag"),

    /**
     * Stolen phone — fenced item; handling adds WITNESSED_CRIMES to criminal record.
     * Source of passive income; WitnessSystem penalises holding stolen goods.
     */
    STOLEN_PHONE("Stolen Phone"),

    /**
     * Prescription meds — sold to DESPERATE NPCs for high coin.
     * Handling attracts police attention at Notoriety Tier 2+.
     */
    PRESCRIPTION_MEDS("Prescription Meds"),

    /**
     * Counterfeit note — acts as currency multiplier (×2 COIN value) but
     * triggers WitnessSystem criminal record entry if a POLICE NPC is nearby.
     */
    COUNTERFEIT_NOTE("Counterfeit Note"),

    /**
     * Tobacco pouch — sold to DESPERATE NPCs; less scrutinised than CIGARETTE.
     * Unaffected by COUNCIL_CRACKDOWN event.
     */
    TOBACCO_POUCH("Tobacco Pouch"),

    // ── Issue #781: Graffiti & Territorial Marking ────────────────────────────

    /**
     * Empty spray can — drops from breaking shelving props in the industrial estate.
     * Can be re-crafted with a PAINT_PIGMENT into a fresh SPRAY_CAN.
     */
    SPRAY_CAN_EMPTY("Empty Spray Can"),

    /**
     * Filled spray can — crafted from SPRAY_CAN_EMPTY + any PAINT_PIGMENT.
     * Has 20 uses; press T to tag a block face. Becomes SPRAY_CAN_EMPTY when exhausted.
     */
    SPRAY_CAN("Spray Can"),

    /**
     * Red paint pigment — drops from art-supply/hardware props.
     * Crafted with SPRAY_CAN_EMPTY to make a red SPRAY_CAN.
     */
    PAINT_PIGMENT_RED("Red Paint Pigment"),

    /**
     * Blue paint pigment — drops from art-supply/hardware props.
     */
    PAINT_PIGMENT_BLUE("Blue Paint Pigment"),

    /**
     * Gold paint pigment — drops from art-supply/hardware props.
     * Required exclusively for CROWN_TAG (Marchetti Crew faction style).
     */
    PAINT_PIGMENT_GOLD("Gold Paint Pigment"),

    /**
     * White paint pigment — drops from art-supply/hardware props.
     */
    PAINT_PIGMENT_WHITE("White Paint Pigment"),

    /**
     * Grey paint pigment — drops from art-supply/hardware props.
     */
    PAINT_PIGMENT_GREY("Grey Paint Pigment"),

    /**
     * Newspaper — already exists; referenced here as black market rumour source.
     * Used by NPCs in BORED state; seeded by RUMOUR_NETWORK with LOOT_TIP.
     * NOTE: NEWSPAPER already exists above — this comment is for documentation only.
     */

    // ── Issue #783: Pirate FM — Underground Radio Station ─────────────────────

    /**
     * Wire — crafted from 1 COIN + 1 WOOD.
     * Used as a component for MICROPHONE, TRANSMITTER block, and DJ equipment.
     * Tradeable commodity in the StreetEconomySystem.
     */
    WIRE("Wire"),

    /**
     * Broadcast Tape — crafted from 1 NEWSPAPER + 1 COIN.
     * Right-click on a TRANSMITTER to pre-record a show. Tape auto-broadcasts
     * the chosen action every 30 in-game seconds at half effectiveness while unattended.
     */
    BROADCAST_TAPE("Broadcast Tape"),

    /**
     * Transmitter (item) — crafted from 2 WIRE + 1 COMPUTER + 1 WOOD.
     * Place it in the world (must be indoors with 3+ block roof) to place a
     * TRANSMITTER block. Once placed, interact with it and press B while holding
     * a MICROPHONE to broadcast.
     */
    TRANSMITTER_ITEM("Transmitter"),

    // ── Issue #785: The Dodgy Market Stall ────────────────────────────────────

    /**
     * Stall Frame — crafted from 4 WOOD.
     * Place on any PAVEMENT or ROAD block to create a market stall.
     * Press E to open the Stall Management UI.
     */
    STALL_FRAME("Stall Frame"),

    /**
     * Stall Awning — crafted from 2 WOOD + 1 PLANKS.
     * Attach to a STALL_FRAME to provide weather protection.
     * Prevents RAIN from closing the stall and destroying stock.
     */
    STALL_AWNING("Stall Awning"),

    /**
     * Market Licence — purchased from the Council NPC for 20 COIN.
     * Prevents POLICE from fining the stall and MARKET_INSPECTOR penalties.
     */
    MARKET_LICENCE("Market Licence"),

    /**
     * Knock-Off Perfume — dodgy item sold at the stall.
     * High demand, moderate price, raises Notoriety slightly if caught.
     */
    KNOCK_OFF_PERFUME("Knock-Off Perfume"),

    // ── Issue #787: Street Skills & Character Progression ──────────────────────

    /**
     * Skeleton Key — crafted from 3 WIRE + 1 BRICK.
     * Opens any locked door once; consumed on use.
     */
    SKELETON_KEY("Skeleton Key"),

    /**
     * Bitter Greens — LEAVES block eaten as emergency food.
     * Requires GRAFTING Journeyman tier or above to consume.
     * Restores 20 hunger; slightly unpleasant (no energy bonus).
     */
    BITTER_GREENS("Bitter Greens"),

    // ── Issue #797: The Neighbourhood Watch ───────────────────────────────────

    /**
     * Neighbourhood Newsletter — crafted from 2 NEWSPAPER + 1 COIN.
     * Use near a PETITION_BOARD to remove it and reduce WatchAnger by 8.
     * Community diplomacy. Fragile diplomacy, but diplomacy nonetheless.
     */
    NEIGHBOURHOOD_NEWSLETTER("Neighbourhood Newsletter"),

    /**
     * Peace Offering — crafted from 1 SAUSAGE_ROLL + 1 COIN.
     * Use on a WATCH_MEMBER NPC to convert them from aggressive patrol to
     * neutral patrol mode and reduce WatchAnger by 5.
     * A sausage roll has resolved more conflicts than you'd think.
     */
    PEACE_OFFERING("Peace Offering"),

    // ── Issue #799: The Corner Shop Economy ───────────────────────────────────

    /**
     * Shop Key — used to claim any charity shop, off-licence, or derelict unit.
     * Alternatively, pressing E on a derelict door (Condition ≤ 49) claims it directly.
     */
    SHOP_KEY("Shop Key"),

    /**
     * Cider — sold at the corner shop for faction Street Lads Respect.
     * Selling at fair price gives Street Lads Respect +2–5/day.
     */
    CIDER("Cider"),

    /**
     * Tobacco — sold at the corner shop. Grey-market item.
     * Selling at fair price gives Street Lads Respect +2–5/day.
     */
    TOBACCO("Tobacco"),

    // ── Issue #801: The Underground Fight Night ────────────────────────────────

    /**
     * Fight Card — a paper flyer granting entry to the Pit.
     * Dropped by YOUTH NPCs near the industrial estate.
     * Alternatively, Notoriety ≥ 10 is sufficient for entry.
     */
    FIGHT_CARD("Fight Card"),

    /**
     * Championship Belt — held by the rank-1 fighter on the Championship Ladder.
     * Wearable cosmetic; grants +5 Notoriety/day while worn.
     */
    CHAMPIONSHIP_BELT("Championship Belt"),

    /**
     * Mouth Guard — reduces stamina loss from opponent hits by 25%.
     * Craftable from RUBBER.
     */
    MOUTH_GUARD("Mouth Guard"),

    /**
     * Rubber — raw material used to craft a MOUTH_GUARD.
     */
    RUBBER("Rubber"),

    // ── Issue #901: Portal to Bista Village ───────────────────────────────────

    /**
     * Bista Village Portal — craftable one-use item.
     * Crafted from DIAMOND×2 + STONE×4 + WOOD×2.
     * When used (right-click while held), teleports the player to Bista Village —
     * a historically accurate recreation of the Gurkha settlement outside Aldershot.
     * The portal is the only means of accessing this location.
     * Single-use: consumed on activation. A return portal stone appears in Bista Village.
     */
    BISTA_VILLAGE_PORTAL("Bista Village Portal"),

    // ── Issue #906: Busking System ────────────────────────────────────────────

    /**
     * Bucket Drum — craftable street percussion instrument.
     * Crafted from 1 SCRAP_METAL + 1 PLANKS.
     * Held item; equip in hotbar and press E near PAVEMENT/ROAD to start a busk session.
     * Tooltip on first pickup: "Bucket drum. The percussion instrument of the dispossessed."
     */
    BUCKET_DRUM("Bucket Drum"),

    // ── Issue #908: Bookies Horse Racing System ────────────────────────────────

    /**
     * Bet Slip — single-use paper item added to inventory when a bet is placed at the bookies.
     * Removed from inventory when the race resolves (win or loss).
     * Payout = stake × odds numerator on win.
     */
    BET_SLIP("Bet Slip"),

    // ── Issue #914: Allotment System ──────────────────────────────────────────

    /** Potato seed — plantable in a DIRT plot block. Grows into POTATO in 15 in-game minutes. */
    POTATO_SEED("Potato Seed"),

    /** Carrot seed — plantable in a DIRT plot block. Grows into CARROT in 10 in-game minutes. */
    CARROT_SEED("Carrot Seed"),

    /** Cabbage seed — plantable in a DIRT plot block. Grows into CABBAGE in 20 in-game minutes. */
    CABBAGE_SEED("Cabbage Seed"),

    /** Sunflower seed — plantable in a DIRT plot block. Grows into SUNFLOWER in 8 in-game minutes. */
    SUNFLOWER_SEED("Sunflower Seed"),

    /** Harvested potato — satisfies NeedType.HUNGRY. Yield 2–4 per POTATO crop. */
    POTATO("Potato"),

    /** Harvested carrot — satisfies NeedType.HUNGRY. Yield 2–3 per CARROT crop. */
    CARROT("Carrot"),

    /** Harvested cabbage — satisfies NeedType.HUNGRY. Yield 1–2 per CABBAGE crop. */
    CABBAGE("Cabbage"),

    /** Harvested sunflower — trade-only (3 coins each at stalls/PLOT_NEIGHBOUR). Not edible. */
    SUNFLOWER("Sunflower"),

    /** Plot Deed — proof of allotment plot ownership. Issued on claiming a plot; removed on repossession. */
    PLOT_DEED("Plot Deed"),

    // ── Issue #918: Bus Stop & Public Transport System ─────────────────────────

    /**
     * Bus Pass — craftable from 3 COIN + 1 NEWSPAPER.
     * Provides 7-day unlimited travel on The Number 47 bus.
     * Avoids the base 2-COIN fare and bypasses ticket inspector checks.
     * Tooltip on first pickup: "Seven days of unlimited travel. The bus still won't come on time."
     */
    BUS_PASS("Bus Pass"),

    /**
     * Inspector Badge — looted from a beaten TICKET_INSPECTOR NPC.
     * Acts as a disguise: while held, TICKET_INSPECTOR NPCs do not check the player's
     * ticket. Tradeable at the fence for 8 COIN.
     */
    INSPECTOR_BADGE("Inspector Badge"),

    // ── Issue #922: Skate Park System ─────────────────────────────────────────

    /**
     * Skateboard — craftable from 2 WOOD + 1 PLANKS.
     * Not required to perform tricks, but holding one in the hotbar gives +15%
     * trick score multiplier when skating in the SKATE_PARK.
     * Tooltip on first craft: "Technically it's a weapon too."
     */
    SKATEBOARD("Skateboard"),

    // ── Issue #924: Launderette System ────────────────────────────────────────

    /**
     * Clean Clothes — received after a completed 90-second wash cycle at the
     * WASHING_MACHINE prop. Using the CHANGING_CUBICLE prop after collection
     * equips them and grants the FRESHLY_LAUNDERED buff (−20% NPC recognition
     * chance for 3 in-game minutes).
     */
    CLEAN_CLOTHES("Clean Clothes"),

    /**
     * Bloody Hoodie — drops from the player after losing more than 30 HP in a
     * single fight. Washing it at the launderette deducts 2 Notoriety and clears
     * the COVERED_IN_BLOOD debuff.
     */
    BLOODY_HOODIE("Bloody Hoodie"),

    /**
     * Stolen Jacket — sold by the FENCE NPC during a SUSPICIOUS_LOAD random event
     * at the launderette (5 COIN). Washing it deducts 2 Notoriety and clears the
     * COVERED_IN_BLOOD debuff, same as the BLOODY_HOODIE.
     * Earns the LAUNDERING achievement on purchase.
     */
    STOLEN_JACKET("Stolen Jacket"),

    /**
     * Cloth — a piece of rough fabric. Dropped by BELL_ROPE_PROP on destruction.
     * Used as a crafting component and wearable disguise component.
     * Tooltip: "It's just a bit of cloth. Could be useful."
     */
    CLOTH("Cloth"),

    // ── Issue #926: Chippy System — Tony's Chip Shop ──────────────────────────

    /**
     * Battered Sausage — purchased from CHIPPY_COUNTER for 2 COIN.
     * Restores +30 hunger and +10 energy. Comes wrapped in WHITE_PAPER.
     */
    BATTERED_SAUSAGE("Battered Sausage"),

    /**
     * Chip Butty — purchased from CHIPPY_COUNTER for 3 COIN.
     * Requires BREAD in the player's inventory to unlock on the menu.
     * Restores +50 hunger. Comes wrapped in WHITE_PAPER.
     */
    CHIP_BUTTY("Chip Butty"),

    /**
     * Mushy Peas — purchased from CHIPPY_COUNTER for 1 COIN.
     * Restores +15 hunger and provides +5 cold relief. Comes wrapped in WHITE_PAPER.
     */
    MUSHY_PEAS("Mushy Peas"),

    /**
     * Pickled Egg — purchased from CHIPPY_COUNTER for 1 COIN.
     * Restores +10 hunger but has a 20% chance of inflicting FOOD_POISONING
     * (80% movement speed for 60 seconds). Comes wrapped in WHITE_PAPER.
     */
    PICKLED_EGG("Pickled Egg"),

    /**
     * Fish Supper — premium item purchased from CHIPPY_COUNTER for 4 COIN.
     * Only available on 2 out of every 3 in-game days. Restores +60 hunger.
     * Comes wrapped in WHITE_PAPER. Buying 5 triggers CHIPPY_REGULAR newspaper infamy.
     */
    FISH_SUPPER("Fish Supper"),

    /**
     * Salt and Vinegar Packet — condiment sold at CHIPPY_COUNTER for 1 COIN.
     * Combine with CHIPS in inventory to produce CHIPS_SEASONED (+50 hunger).
     * Earns the SALT_AND_VINEGAR achievement on first use.
     */
    SALT_AND_VINEGAR_PACKET("Salt &amp; Vinegar Packet"),

    /**
     * Chips (Seasoned) — result of combining CHIPS + SALT_AND_VINEGAR_PACKET.
     * Restores +50 hunger (compared to plain CHIPS' +40).
     */
    CHIPS_SEASONED("Chips (Seasoned)"),

    /**
     * Bottle of Water — purchased from CHIPPY_COUNTER for 1 COIN.
     * Restores +20 thirst. Non-food item, does not trigger FOOD_POISONING.
     */
    BOTTLE_OF_WATER("Bottle of Water"),

    /**
     * White Paper — junk wrapping material. Every chippy item is delivered
     * inside this. Can be dropped or discarded; no gameplay function.
     */
    WHITE_PAPER("White Paper"),

    // ── Issue #928: Public Library System ──────────────────────────────────────

    /**
     * DIY Manual — borrowed from the BOOKSHELF prop in the library's home improvement
     * section. Reading it during a 60-second session awards +15 CONSTRUCTION XP.
     * Can be carried in inventory; auto-returns at next library visit.
     */
    DIY_MANUAL("DIY Manual"),

    /**
     * Negotiation Book — borrowed from the BOOKSHELF prop in the library's business
     * and self-help section. Reading it during a 60-second session awards +15 TRADING XP.
     * Can be carried in inventory; auto-returns at next library visit.
     */
    NEGOTIATION_BOOK("Negotiation Book"),

    /**
     * Street Law Pamphlet — borrowed from the BOOKSHELF prop in the library's legal
     * reference section. Reading it during a 60-second session awards +15 STREETWISE XP
     * and grants Street Lads Respect +5 via FactionSystem.
     * Can be carried in inventory; auto-returns at next library visit.
     */
    STREET_LAW_PAMPHLET("Street Law Pamphlet"),

    // ── Issue #932: Ice Cream Van System ─────────────────────────────────────

    /**
     * 99 Flake — classic British ice cream cone with a chocolate flake.
     * Purchased from the ice cream van for 2 COIN (3 during HEATWAVE).
     * Satisfies HUNGRY −20. Tooltip on first purchase:
     * "Nothing says British summer like overpriced ice cream from a van that might fail its MOT."
     */
    ICE_CREAM_99("99 Flake"),

    /**
     * Screwball — spiral ice lolly with a bubblegum ball at the bottom.
     * Purchased from the ice cream van for 1 COIN (2 during HEATWAVE).
     * Satisfies HUNGRY −10. Has a 30% chance of also giving the player a BUBBLEGUM item.
     */
    SCREWBALL("Screwball"),

    /**
     * Fab Lolly — strawberry, chocolate and hundreds-and-thousands lolly.
     * Purchased from the ice cream van for 2 COIN (3 during HEATWAVE).
     * Satisfies HUNGRY −15. Interaction: "You eat the strawberry bit first, don't you."
     */
    FAB_LOLLY("Fab Lolly"),

    /**
     * Choc Ice — chocolate-coated vanilla ice cream bar.
     * Purchased from the ice cream van for 3 COIN (4 during HEATWAVE).
     * Satisfies HUNGRY −25 and grants SWEET_TOOTH buff (+5 energy for 60 seconds).
     */
    CHOC_ICE("Choc Ice"),

    /**
     * Oyster Card Lolly — rare blue lolly shaped like an Oyster card.
     * Purchased from the ice cream van for 5 COIN (6 during HEATWAVE).
     * Single-use: when redeemed at a bus stop, grants a free ride on the Number 47.
     */
    OYSTER_CARD_LOLLY("Oyster Card Lolly"),

    /**
     * Bubblegum — chewy pink bubblegum ball found at the bottom of a SCREWBALL (30% chance).
     * No gameplay effect; collectible novelty item.
     */
    BUBBLEGUM("Bubblegum"),

    // ── Issue #1069: Ice Cream Van System (Dave's Ices — Mr. Whippy) ─────────

    /**
     * Ninety-Nine Flake — classic Mr. Whippy cone with a chocolate flake.
     * Purchased from Dave's Ices van for 2 COIN (3 during HEATWAVE/SUNNY).
     * Satisfies HUNGRY −20. Tooltip on first purchase:
     * "Nothing says British summer like overpriced ice cream from a van that might fail its MOT."
     */
    NINETY_NINE_FLAKE("99 Flake"),

    /**
     * Lolly — a basic ice lolly from Dave's van.
     * Purchased for 1 COIN. Satisfies HUNGRY −10.
     * Tooltip: "Tastes like childhood and traffic fumes."
     */
    LOLLY("Lolly"),

    /**
     * Wafer Tub — a small tub of soft-serve ice cream with a wafer.
     * Purchased for 2 COIN. Satisfies HUNGRY −15.
     * Tooltip: "The spoon is barely a spoon."
     */
    WAFER_TUBS("Wafer Tub"),

    /**
     * Banana Split — a banana-flavoured ice lolly split down the middle.
     * Purchased for 2 COIN. Satisfies HUNGRY −15.
     * Tooltip: "No actual banana was harmed in the making of this."
     */
    BANANA_SPLIT("Banana Split"),

    /**
     * Flake 99 with Sauce — 99 Flake ice cream with strawberry sauce drizzled on top.
     * Purchased for 3 COIN. Satisfies HUNGRY −25 and grants SWEET_TOOTH buff.
     * Tooltip: "You asked for sauce. You got sauce. No regrets."
     */
    FLAKE_99_WITH_SAUCE("Flake 99 with Sauce"),

    // ── Issue #934: Pigeon Racing System ─────────────────────────────────────

    /**
     * Racing Pigeon — the player's homing pigeon, acquired from PIGEON_FANCIER NPC,
     * caught as a wild BIRD NPC, or earned as a community reward.
     * Stored in the PIGEON_LOFT prop; not carried in inventory during a race.
     * Tooltip on first acquisition: "A proper racing pigeon. Treat it right."
     */
    RACING_PIGEON("Racing Pigeon"),

    /**
     * Bread Crust — pigeon feed used to train the racing pigeon.
     * Each crust increases the pigeon's training level (0–10, capped).
     * Dropped by PENSIONER NPCs (10% chance) or crafted from CARDBOARD + COIN.
     * Tooltip on first pickup: "Stale bread. The pigeon's not fussed."
     */
    BREAD_CRUST("Bread Crust"),

    /**
     * Pigeon Trophy — awarded on winning the Northfield Derby (NORTHFIELD_DERBY achievement).
     * Decorative; can be placed on a surface in the squat for +5 Vibe.
     * Tooltip: "First place. The bird earned it more than you did."
     */
    PIGEON_TROPHY("Pigeon Trophy"),

    // ── Issue #936: Council Skip & Bulky Item Day ─────────────────────────────

    /**
     * Old Sofa — common skip loot. Yields 3 WOOD when broken.
     * Fence value: 3 COIN. Not stolen; no Notoriety gain.
     */
    OLD_SOFA("Old Sofa"),

    /**
     * Broken Telly — common skip loot. Yields 1 SCRAP_METAL + 1 COMPUTER when broken.
     * Fence value: 4 COIN. Not stolen; no Notoriety gain.
     */
    BROKEN_TELLY("Broken Telly"),

    /**
     * Wonky Chair — common skip loot. Yields 2 WOOD when broken.
     * Fence value: 2 COIN. Not stolen; no Notoriety gain.
     */
    WONKY_CHAIR("Wonky Chair"),

    /**
     * Carpet Roll — common skip loot. Wearable as WOOLLY_HAT_ECONOMY proxy.
     * Fence value: 3 COIN. Not stolen; no Notoriety gain.
     */
    CARPET_ROLL("Carpet Roll"),

    /**
     * Old Mattress — uncommon skip loot. Restores 20 warmth when slept on in squat.
     * Crafting ingredient: OLD_MATTRESS + SLEEPING_BAG → LUXURY_BED (+15 Vibe).
     * Fence value: 5 COIN. Not stolen; no Notoriety gain.
     */
    OLD_MATTRESS("Old Mattress"),

    /**
     * Filing Cabinet — uncommon skip loot. Yields 1 COIN + random DWP_LETTER when broken.
     * Fence value: 6 COIN. Not stolen; no Notoriety gain.
     */
    FILING_CABINET("Filing Cabinet"),

    /**
     * Exercise Bike — uncommon skip loot.
     * Crafting ingredient: EXERCISE_BIKE + SCRAP_METAL → IMPROVISED_TOOL (25 uses).
     * Fence value: 7 COIN. Not stolen; no Notoriety gain.
     */
    EXERCISE_BIKE("Exercise Bike"),

    /**
     * Box of Records — uncommon skip loot. Gives +10 MC Rank XP.
     * Pre-claimed by the PIGEON_FANCIER at 07:55 on Bulky Item Day.
     * Fence value: 8 COIN. Not stolen; no Notoriety gain.
     */
    BOX_OF_RECORDS("Box of Records"),

    /**
     * Microwave — rare skip loot. When placed in squat, enables
     * GREGGS_PASTRY → HOT_PASTRY conversion at the workbench (+15 hunger).
     * Fence value: 10 COIN. Not stolen; no Notoriety gain.
     */
    MICROWAVE("Microwave"),

    /**
     * Shopping Trolley (Gold) — rare skip loot. Special prop: carries 4× inventory
     * slots as a mobile chest.
     * Fence value: 12 COIN. Not stolen; no Notoriety gain.
     */
    SHOPPING_TROLLEY_GOLD("Golden Shopping Trolley"),

    /**
     * Antique Clock — very rare skip loot.
     * Tooltip: "Probably worth something. Probably nicked."
     * Triggers special Fence dialogue and ANTIQUE_ROADSHOW achievement on sale.
     * Fence value: 20 COIN. Not stolen; no Notoriety gain.
     */
    ANTIQUE_CLOCK("Antique Clock"),

    /**
     * Hot Pastry — result of microwaving GREGGS_PASTRY in a squat with a MICROWAVE.
     * Restores +15 hunger compared to the cold version.
     */
    HOT_PASTRY("Hot Pastry"),

    /**
     * Luxury Bed — crafted from OLD_MATTRESS + SLEEPING_BAG.
     * Squat furnishing: +15 Vibe. A step above the PROP_BED.
     */
    LUXURY_BED("Luxury Bed"),

    /**
     * DWP Letter — drops from a FILING_CABINET when broken.
     * Flavour item; no gameplay function beyond being delightfully grim.
     */
    DWP_LETTER("DWP Letter"),

    // ── Issue #938: Greasy Spoon Café ─────────────────────────────────────────

    /**
     * Full English — the centrepiece of Vera's Caff menu.
     * Cost: 6 COIN. Restores +60 hunger, +20 warmth. Unavailable after 11:00.
     * Counts toward FULL_ENGLISH_FANATIC achievement on consumption.
     */
    FULL_ENGLISH("Full English"),

    /**
     * Mug of Tea — builder's staple sold at Vera's Caff.
     * Cost: 2 COIN. Restores +25 warmth instantly. Available all day.
     */
    MUG_OF_TEA("Mug of Tea"),

    /**
     * Beans on Toast — a simple caff staple.
     * Cost: 3 COIN. Restores +30 hunger. Available all day.
     */
    BEANS_ON_TOAST("Beans on Toast"),

    /**
     * Fried Bread — greasy side item from Vera's Caff.
     * Cost: 1 COIN. Restores +15 hunger. Available all day.
     */
    FRIED_BREAD("Fried Bread"),

    /**
     * Bacon Butty — the breakfast classic, available before 11:00 only.
     * Cost: 3 COIN. Restores +35 hunger, +10 warmth.
     */
    BACON_BUTTY("Bacon Butty"),

    /**
     * Builder's Tea — a large mug of strong tea.
     * Cost: 2 COIN. Restores +30 warmth. Available all day.
     */
    BUILDER_S_TEA("Builder's Tea"),

    /**
     * Chalkboard — a prop used for the daily specials board at Vera's Caff.
     * Displays the day's combo discount. Not an inventory item; placed as a prop.
     */
    CHALKBOARD("Chalkboard"),

    // ── Issue #948: Hand Car Wash ─────────────────────────────────────────────

    /**
     * Squeegee — a window-cleaning squeegee on a telescopic pole.
     * Spawns in the SHED_PROP at the Sparkle Hand Car Wash; also sold by the
     * charity shop. Equipping it while on a car wash shift gives +1 bonus COIN
     * per in-game minute (showing initiative).
     */
    SQUEEGEE("Squeegee"),

    // ── Issue #950: Northfield Leisure Centre ─────────────────────────────────

    /**
     * Chocolate Bar — sold in the leisure centre vending machine for 2 COIN.
     * Restores +20 hunger. Also satisfies HUNGRY need for NPCs.
     */
    CHOCOLATE_BAR("Chocolate Bar"),

    /**
     * Water Bottle — sold in the leisure centre vending machine for 1 COIN.
     * Restores +10 hunger and +5 warmth. Satisfies THIRSTY need for NPCs.
     */
    WATER_BOTTLE("Water Bottle"),

    /**
     * Swim Trunks — pool-side attire. Required to use the swimming session.
     * Sold by the receptionist for 3 COIN or found in the charity shop.
     * Equipping reduces the costume check at the leisure centre.
     */
    SWIM_TRUNKS("Swim Trunks"),

    // ── Issue #952: Clucky's Fried Chicken ───────────────────────────────────

    /**
     * Chicken Wings — purchased from Clucky's for 2 COIN.
     * Restores +35 hunger. Can be shared (Wing Tax mechanic).
     * Tooltip on first purchase: "Still got the bone in. How it should be."
     */
    CHICKEN_WINGS("Chicken Wings"),

    /**
     * Chicken Box — purchased from Clucky's for 4 COIN.
     * Restores +50 hunger. Target of the Wing Tax steal mechanic (40% chance).
     * Tooltip on first purchase: "Four pieces, chips, and a suspicious sauce."
     */
    CHICKEN_BOX("Chicken Box"),

    /**
     * Chips and Gravy — purchased from Clucky's for 1 COIN after 20:00 only.
     * Restores +30 hunger. Late-night special; unavailable before 20:00.
     * Tooltip on first purchase: "Chips and gravy. The north approves."
     */
    CHIPS_AND_GRAVY("Chips and Gravy"),

    /**
     * Flat Cola — purchased from Clucky's for 1 COIN.
     * Restores +5 energy. Noticeably flat.
     * Tooltip on first purchase: "It was fizzy when it left the factory. Probably."
     */
    FLAT_COLA("Flat Cola"),

    /**
     * Chicken Bone — litter prop spawned outside Clucky's every 15 in-game minutes (cap 8).
     * Depositing in a LITTER_BIN_PROP reduces Notoriety by 1 (cap −5/day).
     * Tooltip: "Someone's dinner. Now the pavement's problem."
     */
    CHICKEN_BONE("Chicken Bone"),

    /**
     * Chicken Box (Empty) — litter prop spawned outside Clucky's every 15 in-game minutes (cap 8).
     * Depositing in a LITTER_BIN_PROP reduces Notoriety by 1 (cap −5/day).
     * Tooltip: "Grease-stained and sad. Much like the high street."
     */
    EMPTY_CHICKEN_BOX("Empty Chicken Box"),

    // ── Issue #954: Northfield Bingo Hall ─────────────────────────────────────

    /**
     * Rigged Bingo Card — a homemade bingo card with pre-dabbed numbers.
     * Crafted from 1 BET_SLIP + 1 COIN + 2 WOOD. When used on entry to the bingo hall,
     * silently replaces the fair card (needs only 3 more numbers to complete).
     * 5% cheat-detection chance per number called. If caught: ejection, Notoriety +8.
     * Tooltip on use: "It's only cheating if you get caught."
     * Tooltip on caught: "You absolute melt."
     */
    RIGGED_BINGO_CARD("Rigged Bingo Card"),

    /**
     * Tea Cup — sold at the bingo hall refreshment counter for 1 COIN.
     * A small cup of lukewarm tea. Restores +10 warmth.
     * Tooltip: "Milky. Slightly too hot. Exactly right."
     */
    TEA_CUP("Tea Cup"),

    /**
     * Biscuit — sold at the bingo hall refreshment counter for 1 COIN.
     * A single digestive in a paper wrapper. Restores +5 hunger.
     * Tooltip: "One biscuit. The economy, mate."
     */
    BISCUIT("Biscuit"),

    /**
     * Bingo Trophy — awarded on FULL HOUSE win at Lucky Stars Bingo.
     * Placed as a BINGO_TROPHY_PROP in the player's nearest squat.
     * Tooltip: "You are the luckiest person in Northfield. Statistically."
     */
    BINGO_TROPHY("Bingo Trophy"),

    // ── Issue #961: Cash4Gold Pawn Shop ───────────────────────────────────────

    /**
     * Guitar — sellable or pledgeable at the pawn shop.
     * Found in skips, squats, or looted from busker NPCs.
     * Pawn shop base value: 18 COIN.
     * Tooltip: "Six strings, three chords, zero prospects."
     */
    GUITAR("Guitar"),

    // ── Issue #963: Northfield Canal ─────────────────────────────────────────

    /**
     * Fishing Rod — crafted from WOOD×1 + SCRAP_METAL×1 + CARDBOARD×1.
     * Has 5 durability; breaks on a failed reel, dropping SCRAP_METAL×1.
     * Used at canal bank (press E while holding) to enter fishing state machine.
     * Tooltip: "The optimism of a fishing rod is one of Britain's great traditions."
     */
    FISHING_ROD("Fishing Rod"),

    /**
     * Fish: Roach — common canal catch (40% chance). Yields +8 hunger raw
     * (20% food-poisoning chance, −10 HP over 30s) or +25 hunger when cooked
     * at a campfire (no risk). Sellable to Barge Baz for 1 COIN.
     * Tooltip: "A roach. Technically edible."
     */
    FISH_ROACH("Roach"),

    /**
     * Fish: Bream — uncommon canal catch (25% chance). Same hunger/cooking stats
     * as FISH_ROACH. Sellable to Barge Baz for 3 COIN.
     * Tooltip: "A bream. Smells like canal. Tastes like canal."
     */
    FISH_BREAM("Bream"),

    /**
     * Fish: Perch — uncommon canal catch (15% chance). Same hunger/cooking stats
     * as FISH_ROACH. Not bought by Barge Baz (too common to bother).
     * Tooltip: "A perch. Still alive. Sort of."
     */
    FISH_PERCH("Perch"),

    /**
     * Fish: Pike — rare canal catch (10% chance). Same hunger/cooking stats as
     * other fish. Sellable to Barge Baz for 5 COIN. Seeds a SHOP_NEWS rumour
     * on catch: "Someone pulled a proper pike out of the canal..."
     * Tooltip: "A pike. Aggressive, territorial, and somehow a metaphor."
     */
    FISH_PIKE("Pike"),

    /**
     * Rope — crafting material sold by Barge Baz (3 COIN).
     * Used in advanced crafting recipes (e.g. canal-related structures).
     * Tooltip: "Rope. Not a noose. Don't be dramatic."
     */
    ROPE("Rope"),

    // ── Issue #965: Northfield Snooker Hall ───────────────────────────────────

    /**
     * Cue — a snooker cue used as a melee weapon (2 damage per hit) and for playing.
     * Craftable: WOOD×2. Breaks after 3 hits in combat, dropping WOOD×1.
     * Tooltip: "A snooker cue. Two damage, breaks in three hits. Not ideal."
     */
    CUE("Snooker Cue"),

    /**
     * Chalk Cube — a small blue cube of snooker chalk. Cosmetic item.
     * Sold by Dennis the Proprietor for 1 COIN.
     * Tooltip: "Chalk. Technically for cue tips. Technically."
     */
    CHALK_CUBE("Chalk Cube"),

    // ── Issue #967: Northfield Taxi Rank ──────────────────────────────────────

    /**
     * Taxi Pass — a prepaid 5-journey card purchased from Mick at A1 Taxis for 18 COIN,
     * or crafted from 1 NEWSPAPER + 3 COIN (counterfeit version adds +1 Notoriety).
     * Each journey decrements the ride counter; when exhausted, falls back to coin payment.
     * Tooltip: "Five journeys. Cash upfront was non-negotiable."
     */
    TAXI_PASS("Taxi Pass"),

    /**
     * Dodgy Package — an unmarked package received from Dave's Minicab with 15% chance.
     * Contents unknown. If UNDERCOVER_POLICE is nearby on arrival, they investigate.
     * Tooltip: "Never ask what's in the bag."
     */
    DODGY_PACKAGE("Dodgy Package"),

    // ── Issue #1207: Big Terry's Cabs ─────────────────────────────────────────

    /**
     * Fare Receipt — issued by Big Terry's Cabs when the player is overcharged (10% chance).
     * Redeem at the DISPATCHER_HATCH_PROP for a 1 COIN refund.
     * Filing a complaint awards CUSTOMER_SERVICE achievement.
     * Tooltip: "Terry's overcharged you by 1 COIN. He claims it's a 'fuel surcharge'."
     */
    FARE_RECEIPT("Fare Receipt"),

    // ── Issue #969: Northfield Cemetery ───────────────────────────────────────

    /**
     * Spade — a gravedigger's tool. Used to dig GRAVE_PLOT_PROP blocks in the cemetery.
     * 8 uses before it breaks. Craftable: 2 WOOD + 1 SCRAP_METAL.
     * Also found in the groundskeeper's shed.
     */
    SPADE("Spade"),

    /**
     * Wedding Ring — loot from a disturbed grave.
     * Fenceable to FENCE NPC or PAWN_SHOP for 8 COIN.
     * Tooltip: "Someone wore this their whole life."
     */
    WEDDING_RING("Wedding Ring"),

    /**
     * Pocket Watch — loot from a disturbed grave.
     * Fenceable to FENCE NPC or PAWN_SHOP for 12 COIN.
     * Tooltip: "Stopped at some point. Hasn't moved since."
     */
    POCKET_WATCH("Pocket Watch"),

    /**
     * Condolence Card — a social item.
     * Giving one to a MOURNER NPC after the funeral raises Community Respect +1.
     * Not sellable.
     */
    CONDOLENCE_CARD("Condolence Card"),

    /**
     * Old Photograph — flavour item found in graves.
     * Not sellable. Examining it displays: "Someone loved this person."
     */
    OLD_PHOTOGRAPH("Old Photograph"),

    /**
     * Old Coin — a tarnished Victorian coin found in graves.
     * Fenceable to FENCE NPC for 8 COIN.
     * Tooltip: "Worn smooth. Probably worth something to the right person."
     */
    OLD_COIN("Old Coin"),

    // ── Issue #971: The Rusty Anchor Wetherspoons ──────────────────────────────

    /**
     * Cheap Spirits — the cheapest drink at The Rusty Anchor (Wetherspoons).
     * Cost: 1 COIN. Warmth +5, Alertness -10, 20% chance of DRUNK state.
     */
    CHEAP_SPIRITS("Cheap Spirits"),

    /**
     * Curry Club Special — Thursday evening only (17:00–21:00) at The Rusty Anchor.
     * Cost: 4 COIN. Hunger -80, Warmth +20.
     * Eating it on a Thursday awards the CURRY_CLUB achievement.
     */
    CURRY_CLUB_SPECIAL("Curry Club Special"),

    // ── Issue #973: Northfield GP Surgery ─────────────────────────────────────

    /**
     * Prescription — issued by Dr. Kapoor after consultation.
     * Redeemed at the PHARMACY_HATCH_PROP within 2 in-game days for the actual medicine.
     * Tooltip: "Take to the pharmacy. Don't lose it."
     */
    PRESCRIPTION("Prescription"),

    /**
     * Antibiotics — heals 30 HP and clears all active stat debuffs.
     * Dispensed for MODERATE_ILLNESS diagnosis (any stat 20–39%).
     * Tooltip: "Finish the course."
     */
    ANTIBIOTICS("Antibiotics"),

    /**
     * Strong Meds — heals 60 HP; restores warmth to 80.
     * Dispensed for SERIOUS_CONDITION diagnosis (any stat < 20%).
     * IMPORTANT: drinking a PINT within 10 in-game minutes drops Warmth by 20.
     * Tooltip: "Don't mix these with alcohol."
     */
    STRONG_MEDS("Strong Meds"),

    /**
     * Sick Note — presented to the CASE_WORKER at the JobCentre (press E while holding).
     * Grants: sanction exemption for that week's job search + 50% benefit uplift for 7 in-game days.
     * Tooltip: "Signed off. Doctor's orders."
     */
    SICK_NOTE("Sick Note"),

    /**
     * Blank Prescription Form — found in the doctor's room waste bin (30% chance).
     * Used for the prescription fraud scam at the PHARMACY_HATCH_PROP.
     * Tooltip: "Someone's letterhead. Technically. Don't."
     */
    BLANK_PRESCRIPTION_FORM("Blank Prescription Form"),

    /**
     * Neon Leaflet — flavour item collected from the LEAFLET_STAND_PROP in the GP Surgery.
     * Unsellable. Random flavour texts: "Know Your Chlamydia", "5-A-Day: An Aspiration",
     * "Feeling Low? You're Not Alone (But We're Very Busy)".
     * Tooltip varies by type.
     */
    NEON_LEAFLET("Neon Leaflet"),

    // ── Issue #1116: Northfield Pharmacy — Day & Night Chemist ───────────────

    /**
     * Cold and Flu Sachet — OTC remedy sold at the pharmacy counter.
     * Clears COLD_SNAP warmth debuff; Warmth +10.
     * Stock sells out within 2 in-game hours during COLD_SNAP/FROST.
     * Tooltip: "Just add hot water and self-pity."
     */
    COLD_AND_FLU_SACHET("Cold & Flu Sachet"),

    /**
     * Antiseptic Cream — OTC first-aid item sold at the pharmacy counter.
     * Stops bleed-out from fight wounds; health regen +5 for 60 seconds.
     * Tooltip: "Stings a bit. Good."
     */
    ANTISEPTIC_CREAM("Antiseptic Cream"),

    /**
     * Vitamin C Tablets — OTC supplement sold at the pharmacy counter.
     * Reduces illness probability by 20% for 1 in-game day.
     * Tooltip: "One a day. Not the whole packet."
     */
    VITAMIN_C_TABLETS("Vitamin C Tablets"),

    /**
     * Hair Dye — OTC cosmetic sold at the pharmacy counter.
     * Applied via DisguiseSystem: reduces NPC recognition by 30% for 1 in-game day.
     * Tooltip: "A new you. For twenty minutes."
     */
    HAIR_DYE("Hair Dye"),

    /**
     * Reading Glasses — flavour item sold at the pharmacy counter.
     * Unsellable. No mechanical effect.
     * Tooltip: "You don't need these. But you feel wiser."
     */
    READING_GLASSES("Reading Glasses"),

    // ── Issue #975: Northfield Post Office ────────────────────────────────────

    /**
     * Benefits Book — issued to the player upon registration at the JobCentre.
     * Cashed weekly at the Post Office counter for the player's current benefit amount.
     * Stolen books from PENSIONER NPCs can be fraudulently cashed (40% detection by Maureen).
     * Tooltip: "Sign here. Every week. No exceptions."
     */
    BENEFITS_BOOK("Benefits Book"),

    /**
     * Parcel — a doorstep delivery item placed by the POSTMAN each morning (06:00–08:00).
     * Contents are randomised loot when opened.
     * Stealing one adds PARCEL_THEFT to the criminal record (+5 Notoriety; +3 if witnessed).
     * Tooltip: "Addressed to someone else. Obviously."
     */
    PARCEL("Parcel"),

    /**
     * Stamp — purchased from the Post Office counter for 1 COIN.
     * Required to send a THREATENING_LETTER via a POST_BOX_PROP.
     * Tooltip: "First class. Because you mean business."
     */
    STAMP("Stamp"),

    /**
     * Post Box Prop — a red Royal Mail post box on the high street.
     * Press E while holding a STAMP to compose and send a threatening letter to a target NPC.
     * Target enters FRIGHTENED state for 24h, drops 20% coin, and refuses to report crimes.
     * Traceable at Notoriety Tier 3+, triggering THREATENING_BEHAVIOUR crime.
     * Tooltip: "Post early for guaranteed delivery."
     */
    POST_BOX_PROP("Post Box"),

    // ── Issue #977: Northfield Amusement Arcade ───────────────────────────────

    /**
     * Twopence — a 2p coin used in arcade machines.
     * Obtained from the change machine (5 per 1 COIN) or found on the arcade floor.
     * Currency for PENNY_FALLS_PROP and CLAW_MACHINE_PROP.
     * Tooltip: "Practically worthless. Until it isn't."
     */
    TWOPENCE("2p Coin"),

    /**
     * Plush Toy — a cheap stuffed animal won from the claw machine.
     * Sellable to the fence (3 COIN) or donated to the charity shop (2 COIN goodwill).
     * Tooltip: "Against all odds."
     */
    PLUSH_TOY("Plush Toy"),

    /**
     * Arcade Token — a redemption prize exchanged for 20 TWOPENCE at the redemption counter.
     * Can be traded at the Fence for 2 COIN. No other legitimate use.
     * Tooltip: "You can buy anything with enough of these. Theoretically."
     */
    ARCADE_TOKEN("Arcade Token"),

    /**
     * Screwdriver — a flathead screwdriver crafted from 1 SCRAP_METAL + 1 PIPE.
     * Used to tamper with arcade machines when Kevin is not watching (>6 blocks away or facing away).
     * Machine tampering actions: lower penny-falls threshold, unlock free plays, extract coin.
     * Tooltip: "For fixing things. And breaking things. Mostly breaking things."
     */
    SCREWDRIVER("Screwdriver"),

    // ── Issue #1309: Northfield Ace Amusements Arcade ────────────────────────

    /**
     * Stuffed Animal — a plush toy won from the claw machine at Ace Amusements.
     * Sellable to the fence (3 COIN) or donated to the charity shop (2 COIN goodwill).
     * Tooltip: "Against all odds."
     */
    STUFFED_ANIMAL("Stuffed Animal"),

    /**
     * Prize Ticket — a paper prize ticket dispensed by the arcade shooter machine.
     * Accumulated and exchanged at the redemption counter for prizes.
     * 5 tickets per win from the Arcade Shooter mini-game.
     * Tooltip: "One day these will add up to something."
     */
    PRIZE_TICKET("Prize Ticket"),

    /**
     * Plastic Trophy — a gaudy plastic award from the redemption counter (40 tickets).
     * Purely decorative. Sell to the fence for 1 COIN or donate to charity shop.
     * Tooltip: "Third place at something you'll never quite remember."
     */
    PLASTIC_TROPHY("Plastic Trophy"),

    /**
     * Arcade Champion Badge — the top redemption counter prize (100 tickets).
     * Grants the holder tamper immunity: Kevin will not catch the player tampering
     * while they wear this badge. Shows as a wearable item in the hotbar.
     * Tooltip: "Kevin himself pressed this. He wasn't happy about it."
     */
    ARCADE_CHAMPION_BADGE("Arcade Champion Badge"),

    /**
     * Builder's Overalls — worn by BUILDER NPCs and sold at the market.
     * When worn, reduces Kevin's detection radius at Ace Amusements from 6 to 3 blocks,
     * making machine-tampering easier to get away with.
     * Tooltip: "Blend in. Act busy. Carry a clipboard."
     */
    BUILDER_OVERALLS("Builder's Overalls"),

    // ── Issue #979: Fix My Phone — Phone Repair Shop ─────────────────────────

    /**
     * SIM Card — a network SIM card for cloning stolen phones.
     * Found at the NEWSAGENT PHONE_DISPLAY_PROP or looted from POSTMAN / DELIVERY_DRIVER
     * NPCs at 20% drop chance. Required (with 1 STOLEN_PHONE + 2 COIN) for back-room
     * phone cloning at Fix My Phone. Satisfies NeedType.BORED at base price 2 COIN.
     * Tooltip: "You don't want to know whose number that is."
     */
    SIM_CARD("SIM Card"),

    /**
     * Cloned Phone — a phone cloned from a STOLEN_PHONE + SIM_CARD in Tariq's back room.
     * Single-use: press E to extract 1 rumour (LOOT_TIP or GANG_ACTIVITY) from the
     * RumourNetwork, then the phone becomes a BURNED_PHONE. Alternatively, plant it on an
     * NPC for passive surveillance (3 rumours over 3 in-game hours); burns automatically.
     * Fence value: 5 COIN. CriminalRecord: adds THEFT on creation.
     * Tooltip: "Tariq said he didn't want to know. He definitely wants to know."
     */
    CLONED_PHONE("Cloned Phone"),

    /**
     * Burned Phone — a used-up CLONED_PHONE after rumour extraction or surveillance timeout.
     * Worthless except as Fence junk (1 COIN).
     * Tooltip: "That's the last of its secrets."
     */
    BURNED_PHONE("Burned Phone"),

    /**
     * Screen Protector — a cosmetic 1 COIN item upsold by Tariq on each phone repair.
     * Adds +1 durability flavour charge to the repaired phone. Donatable to the charity
     * shop for 1 COIN goodwill. Tradeable to desperate BORED NPCs.
     * Tooltip: "It'll probably scratch anyway."
     */
    SCREEN_PROTECTOR("Screen Protector"),

    // ── Issue #983: Northfield Dog Track ─────────────────────────────────────

    /**
     * Cold Pastry — a stale, unidentifiable pastry from a dodgy van.
     * Crafting ingredient: COLD_PASTRY + SUSPICIOUS_MEAT → DODGY_PIE.
     * Tooltip: "Still in the bag. Nobody's asking questions."
     */
    COLD_PASTRY("Cold Pastry"),

    /**
     * Suspicious Meat — an unlabelled lump of protein of indeterminate origin.
     * Crafting ingredient: COLD_PASTRY + SUSPICIOUS_MEAT → DODGY_PIE.
     * Tooltip: "Don't look at it. Don't smell it. Just use it."
     */
    SUSPICIOUS_MEAT("Suspicious Meat"),

    /**
     * Dodgy Pie — crafted from COLD_PASTRY + SUSPICIOUS_MEAT.
     * When slipped to a greyhound (press E while holding, near kennel),
     * reduces that dog's speed by 30% for the next race.
     * The KENNEL_HAND must not be watching (>8 blocks away or facing away).
     * If caught: RACE_FIXING added to CriminalRecord, Notoriety +10.
     * Tooltip: "A pie with a purpose. A terrible, illegal purpose."
     */
    DODGY_PIE("Dodgy Pie"),

    /**
     * Greyhound — a stolen racing greyhound liberated from the kennel at night.
     * Requires LOCKPICK to access the kennel block. Stolen via kennel heist.
     * Fenceable at the Pawn Shop for 25–40 COIN.
     * Adds ANIMAL_THEFT to CriminalRecord; Notoriety +10 if witnessed.
     * Tooltip: "Fast dog. Faster sentence."
     */
    GREYHOUND("Greyhound"),

    /**
     * Race Card — the official programme for tonight's greyhound racing.
     * Satisfies NeedType.BORED for NPCs (StreetEconomySystem satisfier).
     * Sold by the TOTE_CLERK for 1 COIN. Gives odds information for the evening.
     * Tooltip: "Tonight's runners. Six dogs, one dream, zero guarantees."
     */
    RACE_CARD("Race Card"),

    // ── Issue #985: Northfield Police Station ─────────────────────────────────

    /**
     * Custody Key Card — opens the evidence locker keypad at Northfield Police Station.
     * Dropped by the DETENTION_OFFICER (5% chance on defeat), or crafted from
     * BLANK_CARD + INSPECTOR_BADGE (crafting recipe unlocked at squat WORKBENCH).
     * Single-use per entry attempt.
     * Tooltip: "This opens something official. Something you're not meant to open."
     */
    CUSTODY_KEY_CARD("Custody Key Card"),

    /**
     * Blank Card — a plain white plastic card, blank on both sides.
     * Crafting ingredient: 2 COIN → BLANK_CARD (at WORKBENCH).
     * Used with INSPECTOR_BADGE to craft CUSTODY_KEY_CARD.
     */
    BLANK_CARD("Blank Card"),

    /**
     * Bail Receipt — issued by the DUTY_SERGEANT on bail payment.
     * Proof of release; reduces bail by 20% on the next arrest if still held in inventory.
     * Tooltip: "Paid up. Doesn't mean you're innocent. Just means you could afford it."
     */
    BAIL_RECEIPT("Bail Receipt"),

    /**
     * Police Jacket — looted from a defeated DETENTION_OFFICER.
     * DisguiseSystem: wearing it allows free passage through the police station
     * reception and into the custody suite. Fails at the cell block door if
     * Notoriety ≥ 30 (DETENTION_OFFICER recognises the player on close inspection).
     * Tooltip: "It fits, surprisingly. Try not to look too shifty."
     */
    POLICE_JACKET("Police Jacket"),

    /**
     * Drugs Evidence — contraband seized from suspects, stored in the evidence locker.
     * Lootable from EVIDENCE_SHELF_PROP during the evidence locker heist.
     * NeedType.DESPERATE satisfier worth 20 COIN on the street.
     * Holding it adds +5 Notoriety if spotted by a police NPC.
     * Tooltip: "Confiscated goods. Technically still evidence."
     */
    DRUGS_EVIDENCE("Drugs Evidence"),

    // ── Issue #1000: Northfield Fire Station ──────────────────────────────────

    /**
     * Firefighter Helmet — bright yellow safety helmet looted from a station locker.
     * Provides fire protection (halves burn damage) and 50% disguise effectiveness
     * when worn — NPCs are less likely to notice the player in a fire station context.
     * Theft detected with 60% probability if a FIREFIGHTER is within 6 blocks → +1 wanted star.
     * Tooltip: "Big yellow lid. Either you're a hero or you're very committed to a bit."
     */
    FIREFIGHTER_HELMET("Firefighter Helmet"),

    /**
     * Fire Axe — a full-size fire axe from the station equipment locker.
     * Breaks BRICK or WOOD blocks in 4 hits (standard is 8 and 5 respectively).
     * Deals 15 weapon damage in melee combat.
     * Theft detected with 60% probability if a FIREFIGHTER is within 6 blocks → +1 wanted star.
     * Tooltip: "They use it to open doors. You're not using it to open doors."
     */
    FIRE_AXE("Fire Axe"),

    /**
     * Hose Reel — a coiled fire hose from the station equipment locker.
     * Single-use item: press E near a BURNING_BIN to extinguish it, or aim at an NPC
     * to knock them back 5 blocks (cancels NPC aggression state).
     * Theft detected with 60% probability if a FIREFIGHTER is within 6 blocks → +1 wanted star.
     * Tooltip: "Heavy. Wet. One use. Make it count."
     */
    HOSE_REEL("Hose Reel"),

    // ── Issue #1002: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Disposable Lighter — a cheap plastic lighter sold at the BP kiosk for 2 COIN.
     * Single-use ignition item: press E on a {@link ragamuffin.world.PropType#WHEELIE_BIN}
     * to ignite it via {@code WheeliBinFireSystem.ignite()} without consuming a PETROL_CAN.
     * Consumed on use. Cannot be refilled.
     * Tooltip: "Stolen from by the till, probably."
     */
    DISPOSABLE_LIGHTER("Disposable Lighter"),

    // ── Issue #1004: Northfield Community Centre ──────────────────────────────

    /**
     * Vinyl Record — "Still in its sleeve. Contains something by a band you've vaguely heard of."
     * Sold at the Bring &amp; Buy Sale for 3 COIN. Can be fenced for coin.
     */
    VINYL_RECORD("Vinyl Record"),

    /**
     * Thermos Flask — consumable; drink once for +20 Warmth. "Lukewarm at best. You're grateful."
     * Sold at the Bring &amp; Buy Sale for 2 COIN.
     */
    THERMOS_FLASK("Thermos Flask"),

    /**
     * Jigsaw Puzzle Box — "Five hundred pieces. At least two are missing."
     * Satisfies {@code NeedType.BORED} for 15 score when used. Sold at Bring &amp; Buy for 1 COIN.
     */
    JIGSAW_PUZZLE_BOX("Jigsaw Puzzle Box"),

    /**
     * Knitting Needles — "Could be used for knitting. Or other things."
     * Weapon: 3 damage. Also a crafting component. Sold at Bring &amp; Buy for 1 COIN.
     */
    KNITTING_NEEDLES("Knitting Needles"),

    // ── Issue #1008: St. Mary's Church ──────────────────────────────────────

    /**
     * Soup Cup — "Vegetable soup in a polystyrene cup. Hot, beige, comforting."
     * Given free by Reverend Dave at the soup kitchen (Mon/Thu 12:00–14:00).
     * Restores +25 hunger, +5 Warmth on consume. Sell value: 0 COIN.
     */
    SOUP_CUP("Soup Cup"),

    /**
     * Candle — "A white wax candle. Burns for 5 in-game minutes, providing light radius 3."
     * Crafting component; also usable as a throwable fire-starter (sets WOOD blocks within 1 block alight).
     * Found at jumble sales; sell value: 1 COIN.
     */
    CANDLE("Candle"),

    // ── Issue #1331: Power Cut System ─────────────────────────────────────────

    /**
     * Torch — "A rag-wrapped metal torch. Burns for 20 in-game minutes, 5-block light radius."
     * Crafted from SCRAP_METAL × 1 + CLOTH × 1.
     * Provides warm yellow light; essential indoors during a power cut.
     */
    TORCH("Torch"),

    /**
     * Tablecloth — "Beige, floral. Smells of moth balls."
     * Wearable as a disguise component: +1 Disguise tier for 5 minutes if draped over player (press E to wear).
     * Found at jumble sales; sell value: 1 COIN.
     */
    TABLECLOTH("Tablecloth"),

    // ── Issue #1012: Skin Deep Tattoos ────────────────────────────────────────
    /** Sewing needle — used for prison-tattoo DIY at a MIRROR_PROP. */
    NEEDLE("Needle"),
    /** Ink bottle — combined with NEEDLE for prison tattoo. */
    INK_BOTTLE("Ink Bottle"),
    /** Tattoo gun — unlocked by Kev after 3 visits; weapon (3 dmg) and fence item (12–18 COIN). */
    TATTOO_GUN("Tattoo Gun"),

    // ── Issue #1110: Skin Deep Tattoos — new craft items ─────────────────────
    /**
     * Prison Tattoo Kit — crafted from NEEDLE + INK_BOTTLE.
     * Single-use DIY tattoo: applies PRISON_INK buff free, costs 10 HP,
     * 20% chance of INFECTED_WOUND debuff. Unlocks HARD_AS_NAILS achievement.
     */
    PRISON_TATTOO_KIT("Prison Tattoo Kit"),

    /**
     * Tattoo Voucher — given by Kev as reward for tipping him off about Spider.
     * Redeemable for one free flash tattoo at Skin Deep Tattoos.
     */
    TATTOO_VOUCHER("Tattoo Voucher"),

    // ── Issue #1014: Northfield Newsagent ────────────────────────────────────

    /**
     * Lottery Ticket — "Lucky Dip. Numbers: 4, 7, 12, 23, 41, 9."
     * Purchased from Patel's News for 1 COIN. Resolved at 20:00 daily:
     * jackpot 100 COIN (0.5%), match-3 5 COIN (8%). Forfeited if not claimed within 24 hours.
     */
    LOTTERY_TICKET("Lottery Ticket"),

    /**
     * Chewing Gum — "Spearmint. Still in the packet."
     * Reduces Notoriety detection timer by 5 seconds while chewing (idle animation).
     * Purchased from Patel's News for 1 COIN.
     */
    CHEWING_GUM("Chewing Gum"),

    /**
     * Phone Credit Voucher — "£5 top-up. Valid at all major networks."
     * Satisfies NPC NEED_PHONE_CREDIT; tradeable on the street for 4 COIN.
     * Purchased from Patel's News for 3 COIN.
     */
    PHONE_CREDIT_VOUCHER("Phone Credit Voucher"),

    /**
     * Paper Satchel — "A canvas delivery bag with 'Patel's News' stencilled on it."
     * Container (capacity 10 NEWSPAPERs); quest item for the paper round job.
     * Awarded by Patel on accepting the paper round shift.
     */
    PAPER_SATCHEL("Paper Satchel"),

    /**
     * Birthday Card — "A card with a picture of balloons. Generic but sincere."
     * Giftable to an NPC target for +1 Faction Respect.
     * Purchased from Patel's News for 1 COIN.
     */
    BIRTHDAY_CARD("Birthday Card"),

    /**
     * Plastic — generic plastic material dropped by prop destruction.
     * Dropped by LOTTERY_TERMINAL_PROP on destruction.
     */
    PLASTIC("Plastic"),

    /**
     * Lighter — a reusable pocket lighter sold at Patel's News for 2 COIN.
     * Valid ignition source for WheeliBinFireSystem and CampfireSystem.
     * Fence value: 1 COIN. Tooltip: "Flicks first time, every time. Usually."
     */
    LIGHTER("Lighter"),

    // ── Issue #1016: Northfield Canal ─────────────────────────────────────────

    /**
     * Canal Fish — generic fish caught from the Northfield Canal.
     * Eat for +10 hunger. Sellable at Fence for 2 COIN.
     * Tooltip: "Smells like canal. Tastes like canal. You're still eating it."
     */
    CANAL_FISH("Canal Fish"),

    /**
     * Dinghy — an inflatable rubber dinghy.
     * Equip and press F on a WATER block to float across. 10 uses.
     * Tooltip: "Not seaworthy. Barely canal-worthy."
     */
    DINGHY("Dinghy"),

    /**
     * Boot — a single wellington boot, fished out of the canal.
     * Flavour item; unsellable. Tooltip: "Someone had a worse day than you."
     */
    BOOT("Boot"),

    /**
     * Stolen Wallet — a wallet fished out of the canal.
     * Contains 2–6 COIN. +5 Notoriety when picked up.
     * Tooltip: "The money's still in it. Mostly."
     */
    STOLEN_WALLET("Stolen Wallet"),

    /**
     * String — crafting ingredient for a FISHING_ROD.
     * Craftable: 2 WOOD + 1 STRING_ITEM; sold at corner shop.
     * Tooltip: "A length of string. Every crisis starts with a length of string."
     */
    STRING_ITEM("String"),

    /**
     * Mooring Notice — council notice served after the player squats the western
     * narrowboat for 3 in-game days. Player can tear it down (−10 Council Respect)
     * or comply (lose boat, receive 5 COIN compensation).
     * Tooltip: "Notice to Vacate. The Council remains grateful for your cooperation."
     */
    MOORING_NOTICE("Mooring Notice"),

    /**
     * Birdwatching Tip — a rumour item from Terry the Twitcher at the canal.
     * Contains neighbourhood gossip. Not sellable; auto-converts to a NEIGHBOURHOOD rumour.
     * Tooltip: "Terry saw something. Terry always sees something."
     */
    BIRDWATCHING_TIP("Birdwatching Tip"),

    /**
     * Camping Lantern — a small lantern for the narrowboat interior.
     * One of the small items that improves the narrowboat Warmth bonus to +12/min.
     * Tooltip: "Warm glow. Slightly suspicious smell."
     */
    CAMPING_LANTERN("Camping Lantern"),

    /**
     * Camping Stove — a portable gas stove for the narrowboat interior.
     * One of the small items that improves the narrowboat Warmth bonus to +12/min.
     * Tooltip: "Hot food. Technically cooking. Technically a kitchen."
     */
    CAMPING_STOVE("Camping Stove"),

    // ── Issue #1018: Northfield Poundstretcher ────────────────────────────────

    /**
     * Bleach — bottle of own-brand bleach, 1 COIN.
     * Sold at POUND_SHOP. Ingredient for IMPROVISED_PEPPER_SPRAY craft.
     * Tooltip: "Multipurpose. Cleaning and intimidation."
     */
    BLEACH("Bleach"),

    /**
     * Gaffer Tape — a roll of heavy-duty gaffer tape, 2 COIN.
     * Sold at POUND_SHOP. Used in MAKESHIFT_ARMOUR and DUCT_TAPE_RESTRAINT crafts.
     * Tooltip: "Fixes everything. Literally."
     */
    GAFFER_TAPE("Gaffer Tape"),

    /**
     * Knock-Off Tracksuit — a suspiciously cheap branded tracksuit, 3 COIN.
     * Sold at POUND_SHOP. Functions as a disguise: −15% NPC recognition.
     * Tooltip: "Adiads. Close enough."
     */
    KNOCK_OFF_TRACKSUIT("Knock-Off Tracksuit"),

    /**
     * Baked Beans Tin — a large catering tin of beans, 1 COIN.
     * Sold at POUND_SHOP. Restores hunger. Small item.
     * Tooltip: "Heinz? No. But close."
     */
    BAKED_BEANS_TIN("Baked Beans Tin"),

    /**
     * Cable Ties — a pack of 100 cable ties, 2 COIN.
     * Sold at POUND_SHOP. Used in DUCT_TAPE_RESTRAINT craft and HeistSystem guard-binding.
     * Tooltip: "Useful. Very useful."
     */
    CABLE_TIES("Cable Ties"),

    /**
     * Washing Up Liquid — a bottle of supermarket-own washing-up liquid, 1 COIN.
     * Sold at POUND_SHOP. Ingredient for SLIPPERY_FLOOR_TRAP craft.
     * Tooltip: "Lemon fresh. Very slippery."
     */
    WASHING_UP_LIQUID("Washing Up Liquid"),

    /**
     * Mop — a floor mop, 2 COIN.
     * Sold at POUND_SHOP. Ingredient for SLIPPERY_FLOOR_TRAP craft.
     * Tooltip: "Cleans up messes. Makes new ones."
     */
    MOP("Mop"),

    /**
     * Padlock — a basic combination padlock, 2 COIN.
     * Sold at POUND_SHOP. Used to secure squat doors; tracked by SquatFurnishingTracker.
     * Tooltip: "Not exactly a safe. But it's something."
     */
    PADLOCK("Padlock"),

    /**
     * Mystery Box — an unmarked cardboard box from the bargain bin.
     * Contains a random loot item. Seeds a LOOT_TIP rumour when opened.
     * Tooltip: "Could be anything. Probably isn't great."
     */

    /**
     * Slippery Floor Trap — crafted trap: WASHING_UP_LIQUID×1 + MOP×1.
     * Placed on floor; NPCs and player who walk over it are temporarily slowed.
     * Tooltip: "Health and Safety nightmare."
     */
    SLIPPERY_FLOOR_TRAP("Slippery Floor Trap"),

    /**
     * Makeshift Armour — crafted from GAFFER_TAPE×2 + CLOTH×3.
     * Reduces incoming damage by 20%. Very ugly.
     * Tooltip: "It'll hold. Probably."
     */
    MAKESHIFT_ARMOUR("Makeshift Armour"),

    /**
     * Duct Tape Restraint — crafted from GAFFER_TAPE×1 + CABLE_TIES×1.
     * Used in HeistSystem to bind guards silently.
     * Tooltip: "Practical. Uncomfortable."
     */
    DUCT_TAPE_RESTRAINT("Duct Tape Restraint"),

    /**
     * Improvised Pepper Spray — crafted from BLEACH×1 + CANDLE×1.
     * Single-use; temporarily blinds NPCs in a short cone.
     * Tooltip: "Do NOT get this in your eyes."
     */
    IMPROVISED_PEPPER_SPRAY("Improvised Pepper Spray"),

    /**
     * Makeshift Torch — crafted from CLOTH×1 + CANDLE×1.
     * Provides light in dark areas; burns out after 120 seconds.
     * Tooltip: "Medieval. Effective."
     */
    MAKESHIFT_TORCH("Makeshift Torch"),

    // ── Issue #1020: Northfield Sporting & Social Club ────────────────────────

    /**
     * Bitter — a proper pint of bitter, served by Keith at the social club bar.
     * Members: 1 COIN. Guests: 2 COIN. Restores 8 hunger, slight warmth boost.
     * Tooltip: "Not Flash. Not Fancy. Quite Good."
     */
    BITTER("Bitter"),

    /**
     * Mild — a pint of mild ale, cheapest drink on the bar.
     * Members: 1 COIN. Guests: 2 COIN. Restores 6 hunger.
     * Tooltip: "Old man's drink. You've earned it."
     */
    MILD("Mild"),

    /**
     * Lager Top — a pint of lager with a splash of lemonade.
     * Members: 1 COIN. Guests: 2 COIN. Restores 7 hunger.
     * Keith's look of disdain is free of charge.
     * Tooltip: "Keith disapproves, but serves it anyway."
     */
    LAGER_TOP("Lager Top"),

    /**
     * Members Card — laminated membership card for the Northfield Sporting &amp; Social Club.
     * Three tiers: GUEST (day pass), TEMP (1-day), FULL (permanent).
     * Required for member pricing and AGM voting rights.
     * Tooltip: "Membership has its privileges. Mostly cheaper beer."
     */
    MEMBERS_CARD("Members Card"),

    /**
     * Dodgy Ledger — Derek's ledger from the back office, recording protection payments
     * to the Marchetti Crew. Can be handed to the police for Notoriety −10
     * and MARCHETTI_CREW Respect −20. Unlocks the GRASS achievement.
     * Tooltip: "Numbers that shouldn't exist. Names that shouldn't be written down."
     */
    DODGY_LEDGER("Dodgy Ledger"),

    /**
     * Quiz Sheet — the answer sheet for Thursday Quiz Night.
     * Distributed by Derek at 20:00. Filled in during the quiz; handed back at the end.
     * Tooltip: "Capital of Peru? Lima. Everyone always forgets Lima."
     */
    QUIZ_SHEET("Quiz Sheet"),

    // ── Issue #1259: Northfield Pub Quiz Night ────────────────────────────────

    /**
     * Quiz Prize Envelope — handed to the winning team by Derek at 22:00.
     * Contains 10 COIN. Triggers QUIZ_CHAMPION achievement and QUIZ_CHAMPION_RUMOUR.
     * Tooltip: "Derek sealed it himself. You can tell from the saliva."
     */
    QUIZ_PRIZE_ENVELOPE("Quiz Prize Envelope"),

    // ── Issue #1024: Sultan's Kebab ───────────────────────────────────────────

    /**
     * Doner Kebab — purchased from KEBAB_COUNTER_PROP for 4 COIN.
     * Restores 50 HUNGER, 15 WARMTH. The classic.
     */
    DONER_KEBAB("Doner Kebab"),

    /**
     * Shish Kebab — purchased from KEBAB_COUNTER_PROP for 5 COIN.
     * Restores 45 HUNGER, 10 WARMTH. A touch classier.
     */
    SHISH_KEBAB("Shish Kebab"),

    /**
     * Chips in Pitta — purchased from KEBAB_COUNTER_PROP for 2 COIN.
     * Restores 25 HUNGER. Available after 22:00 only.
     */
    CHIPS_IN_PITTA("Chips in Pitta"),

    /**
     * Garlic Bread Slice — purchased from KEBAB_COUNTER_PROP for 1 COIN.
     * Restores 10 HUNGER. Cheap filler.
     */
    GARLIC_BREAD_SLICE("Garlic Bread"),

    /**
     * Doner Meat — raw. Stolen from WALK_IN_FRIDGE_PROP.
     * Cook on campfire for COOKED_DONER_MEAT.
     */
    DONER_MEAT("Doner Meat"),

    /**
     * Cooked Doner Meat — cooked DONER_MEAT on campfire.
     * +65 HUNGER, +20 WARMTH. Best meal in the game.
     */
    COOKED_DONER_MEAT("Cooked Doner"),

    /**
     * Pitta Bread — stolen from WALK_IN_FRIDGE_PROP.
     * +5 HUNGER. Barely worth it. But here you are.
     */
    PITTA_BREAD("Pitta Bread"),

    /**
     * Tool Kit — used to fix the CHILLI_SAUCE_DISPENSER_PROP during the Chilli Sauce Incident.
     * Also used for general repairs. Found in BUILDERS_MERCHANT or crafted.
     */
    TOOL_KIT("Tool Kit"),

    // ── Issue #1118: Sultan's Kebab House ────────────────────────────────────

    /**
     * Mixed Kebab — purchased from KEBAB_COUNTER_PROP for 5 COIN.
     * Hunger +75, Warmth +15. Premium item; only 3 available per night.
     */
    MIXED_KEBAB("Mixed Kebab"),

    /**
     * Garlic Bread — purchased from KEBAB_COUNTER_PROP for 1 COIN.
     * Hunger +20, Warmth +5. Combo discount: any kebab + GARLIC_BREAD = −1 COIN total.
     */
    GARLIC_BREAD("Garlic Bread"),

    /**
     * Meat Card — laminated loyalty stamp card from Sultan's Kebab.
     * Accumulates 1 stamp per in-game day. 5 stamps = free DONER_KEBAB;
     * 10 stamps = free MIXED_KEBAB + Mehmet calls you by name.
     * Forgeable at squat WORKBENCH with NEWSPAPER + COIN.
     */
    MEAT_CARD("Meat Card"),

    /**
     * Card School Invite — rare drop (5% chance) from STREET_LAD NPCs on interaction.
     * Grants access to the back-room card school at Sultan's Kebab without needing Street Rep ≥ 30.
     */
    CARD_SCHOOL_INVITE("Card School Invite"),

    // ── Issue #1026: Northfield Scrapyard ────────────────────────────────────

    /**
     * Copper Wire — stripped from streetlights (3 hits, 22:00–05:00) or industrial
     * COPPER_PIPE_PROP fittings (SCREWDRIVER, 5 seconds). Sells for 4 COIN at the
     * scrapyard weigh-bridge. Gary refuses it if Notoriety ≥ 50.
     */
    COPPER_WIRE("Copper Wire"),

    /**
     * Lead Flashing — harvested from St. Mary's Church roof with BOLT_CUTTERS (8 seconds).
     * Sells for 3 COIN at the scrapyard weigh-bridge. If VICAR is within 15 blocks when
     * harvested, triggers CrimeType.METAL_THEFT (+2 Notoriety).
     */
    LEAD_FLASHING("Lead Flashing"),

    /**
     * Copper Bale — a compressed bale of copper found in the scrapyard locked compound.
     * Too hot to sell openly; Gary refuses it. Sells for 15 COIN via FENCE only.
     */
    COPPER_BALE("Copper Bale"),

    // ── Issue #1028: Northfield Cash Converters ───────────────────────────────

    /**
     * DVD — a second-hand film or game DVD in a plastic case.
     * Sold by Dean at Cash Converters (base price 2 COIN).
     * Prices halved during SCHOOLHOLIDAYS market event.
     * Tooltip: "Region 2. Slight scratches. Plays fine, probably."
     */
    DVD("DVD"),

    /**
     * Bluetooth Speaker — a portable wireless speaker, used or boxed.
     * Sold at Cash Converters (base price 6 COIN).
     * Tooltip: "Connects to everything. Sounds like nothing."
     */
    BLUETOOTH_SPEAKER("Bluetooth Speaker"),

    /**
     * Tablet — a second-hand touchscreen tablet.
     * High-value electronics item checked for serial number by Dean.
     * Base price 15 COIN at Cash Converters; 30% of that via Dave the Middleman.
     * Tooltip: "Previous owner's wallpaper is still on it."
     */
    TABLET("Tablet"),

    /**
     * Games Console — a second-hand gaming console (generic).
     * High-value electronics; serial number checked by Dean.
     * Price ×1.8 during CONSOLE_DROP market event.
     * Base price 20 COIN; Dave pays 30%.
     * Tooltip: "No controllers included. Obviously."
     */
    GAMES_CONSOLE("Games Console"),

    /**
     * Laptop — a second-hand laptop computer.
     * High-value electronics; serial number checked by Dean.
     * Base price 18 COIN at Cash Converters; Dave pays 30%.
     * Tooltip: "Needs a good clean. And a new keyboard. And probably a battery."
     */
    LAPTOP("Laptop"),

    /**
     * Wiped Phone — a STOLEN_PHONE with its IMEI scrubbed via the Fix My Phone
     * cloning pipeline. Bypasses Dean's serial number check at Cash Converters.
     * Base price same as STOLEN_PHONE (8 COIN); no HANDLING_STOLEN_GOODS crime risk.
     * Tooltip: "Totally clean. Totally."
     */
    WIPED_PHONE("Wiped Phone"),

    // ── Issue #1030: Al-Noor Mosque ────────────────────────────────────────────

    /**
     * Date Fruit — dried dates served at the Iftar table during Ramadan.
     * Restores +15 Hunger. Given as part of the Iftar meal (DATE_FRUIT×3, FLATBREAD×1, SOUP_CUP×1).
     * Tooltip: "Sweet, sticky, and surprisingly filling."
     */
    DATE_FRUIT("Dates"),

    /**
     * Flatbread — unleavened bread served at the Iftar table or given by the Imam to hungry visitors.
     * Restores +20 Hunger.
     * Tooltip: "Warm flatbread, freshly made."
     */
    FLATBREAD("Flatbread"),

    // ── Issue #1033: St. Aidan's C.E. Primary School ─────────────────────────

    /**
     * School Dinner — bought from the DINNER_LADY for 1 COIN (11:30–13:30 Mon–Fri).
     * Restores +25 Hunger and +10 Health.
     * Tooltip: "It's turkey twizzlers and chips. You've had worse."
     */
    SCHOOL_DINNER("School Dinner"),

    /**
     * Vaping Supplies — sold by truant SCHOOL_KID for 2 COIN.
     * Useable anywhere; reduces Warmth −5 but seeds STREET_LADS respect rumour (+3).
     * Tooltip: "Not great for you."
     */
    VAPING_SUPPLIES("Vaping Supplies"),

    /**
     * Copied Homework — sold by truant SCHOOL_KID for 2 COIN.
     * Use at Library internet terminal for +20 STREETWISE XP (one-time per session).
     * Tooltip: "Someone else's hard work. Your gain."
     */
    COPIED_HOMEWORK("Copied Homework"),

    /**
     * Teacher Reference Letter — looted from the headteacher's safe.
     * Use at JobCentre to unlock a 1-day cleaning job regardless of criminal record.
     * Tooltip: "Forged but convincing. Mrs Fowler's signature is surprisingly easy to copy."
     */
    TEACHER_REFERENCE_LETTER("Teacher Reference Letter"),

    /**
     * Mophead — looted from the caretaker's shed.
     * Use on graffiti to clean it (same mechanic as BUCKET_OF_WATER).
     * Tooltip: "A well-used mophead. Still works though."
     */
    MOPHEAD("Mophead"),

    /**
     * Second-Hand Bike — looted from the caretaker's shed.
     * Rideable item; sell at scrapyard for 8 COIN.
     * Tooltip: "Three gears and a dodgy brake. Should be fine."
     */
    SECOND_HAND_BIKE("Second-Hand Bike"),

    /**
     * Petty Cash Box — looted from the headteacher's safe.
     * Auto-converts to 5–12 COIN on pickup.
     * Tooltip: "Heavy for a small box. Must be a good week."
     */
    PETTY_CASH_BOX("Petty Cash Box"),

    // ── Issue #1237: Northfield St. Aidan's Primary School ───────────────────

    /**
     * Caretaker's Shed Key — pickpocketed from Derek (CARETAKER) with PICKPOCKET ≥ Apprentice
     * and Notoriety &lt; 30, or obtained via LOCKPICKING ≥ Journeyman on the shed padlock.
     * Use at CARETAKER_SHED_PROP to access shed contents.
     * Tooltip: "A chunky key on a faded 'SHED' fob. Derek's pride and joy."
     */
    CARETAKER_SHED_KEY("Caretaker's Shed Key"),

    /**
     * Caretaker Master Key — looted from Derek's shed; opens the head's office filing cabinet.
     * Fence value: 4 COIN. Tooltip: "Opens everything in the school. Derek doesn't know it's gone."
     */
    CARETAKER_MASTER_KEY("Caretaker Master Key"),

    /**
     * Ofsted Draft Report — looted from headteacher's office filing cabinet using CARETAKER_MASTER_KEY.
     * Sell to a newspaper journalist NPC for 15 COIN; triggers OFSTED_PANIC chain.
     * Tooltip: "Requires Improvement. Three pages of polite devastation."
     */
    OFSTED_DRAFT_REPORT("Ofsted Draft Report"),

    /**
     * School Report Form — blank official form from the headteacher's office.
     * Use at Cybernet with COPIED_HOMEWORK to forge a FORGED_SCHOOL_REPORT.
     * Tooltip: "Officially blank. Unofficially, full of possibilities."
     */
    SCHOOL_REPORT_FORM("School Report Form"),

    /**
     * Forged School Report — crafted from SCHOOL_REPORT_FORM at Cybernet.
     * Sell to a SCHOOL_MUM NPC for 3 COIN; risks SCHOOL_FRAUD CrimeType if witnessed.
     * Tooltip: "A*s across the board. Gary has never been prouder."
     */
    FORGED_SCHOOL_REPORT("Forged School Report"),

    /**
     * Photocopier Ink Cartridge — looted from Derek's shed.
     * Fence value: 6 COIN. Tooltip: "Worth more than you'd think. Offices are desperate for these."
     */
    PHOTOCOPIER_INK_CARTRIDGE("Photocopier Ink Cartridge"),

    /**
     * Pram — prop item associated with SCHOOL_MUM NPCs during the school run.
     * Sprinting through it earns the PUSHCHAIR_MENACE achievement.
     * Tooltip: "A buggy loaded with half-eaten rice cakes and a sleeping toddler."
     */
    PRAM("Pram"),

    // ── Issue #1035: Northfield Nando's ───────────────────────────────────────

    /**
     * Lemon &amp; Herb Chicken — mildest Nando's menu item (heat level 0).
     * Costs 4 COIN. Restores +30 Hunger.
     * Tooltip: "Safe choice. Nobody's judging. Well, a bit."
     */
    LEMON_HERB_CHICKEN("Lemon & Herb Chicken"),

    /**
     * Mild Chicken — heat level 1 Nando's menu item.
     * Costs 4 COIN. Restores +30 Hunger.
     * Tooltip: "Mild. A gentle warmth. Nothing to worry about."
     */
    MILD_CHICKEN("Mild Chicken"),

    /**
     * Medium Chicken — heat level 2 Nando's menu item.
     * Costs 5 COIN. Restores +35 Hunger.
     * Tooltip: "Medium. You're committed now."
     */
    MEDIUM_CHICKEN("Medium Chicken"),

    /**
     * Hot Chicken — heat level 3 Nando's menu item. Causes brief screen tint (2s).
     * Costs 5 COIN. Restores +35 Hunger.
     * Tooltip: "Hot. Your face is going red. It's fine."
     */
    HOT_CHICKEN("Hot Chicken"),

    /**
     * Extra Hot Chicken — heat level 4 Nando's menu item. Causes 3s screen-shake;
     * seeds NANDOS_LEGEND rumour town-wide.
     * Costs 6 COIN. Restores +40 Hunger.
     * Tooltip: "Extra Hot. Respect. Or stupidity. Hard to tell."
     */
    EXTRA_HOT_CHICKEN("Extra Hot Chicken"),

    /**
     * Chicken Wing Platter — entry-level Nando's order that stamps the loyalty card.
     * Costs 3 COIN. Restores +20 Hunger. Each purchase adds 1 stamp to NANDOS_LOYALTY_CARD.
     * Tooltip: "Six wings. Not nearly enough."
     */
    CHICKEN_WING_PLATTER("Chicken Wing Platter"),

    /**
     * Extra Hot Sauce — bottled peri-peri sauce pocketable from PERI_PERI_SAUCE_RACK.
     * Consumable: +2 Health, −5 Notoriety. Throwable: NPC enters FLEEING for 10s (+2 Notoriety).
     * Sellable to Fence for 3 COIN. Unlocked in menu by asking manager.
     * Tooltip: "Handle with care. And maybe gloves."
     */
    EXTRA_HOT_SAUCE("Extra Hot Sauce"),

    /**
     * Nando's Loyalty Card — a stamp-card item from the LOYALTY_CARD_STAND.
     * Gains 1 stamp per CHICKEN_WING_PLATTER purchased. Auto-converts to
     * NANDOS_FREE_MEAL_VOUCHER at 10 stamps. Player can hold up to 5 cards.
     * Tooltip: "Eight stamps in. Two to go. Or are there."
     */
    NANDOS_LOYALTY_CARD("Nando's Loyalty Card"),

    /**
     * Nando's Free Meal Voucher — auto-generated when NANDOS_LOYALTY_CARD reaches 10 stamps.
     * Redeemable at NANDOS_COUNTER_PROP for one EXTRA_HOT_CHICKEN free of charge.
     * Alternatively sell via StreetEconomySystem for 4 COIN to an NPC with HUNGRY > 60.
     * Tooltip: "Worth four coins to the right person. Or a free meal."
     */
    NANDOS_FREE_MEAL_VOUCHER("Nando's Free Meal Voucher"),

    /**
     * Nando's Apron — lootable from NANDOS_STAFF NPC.
     * Wearing it gives free access to the kitchen area (staff-only zone).
     * Integrates with DisguiseSystem: kitchen access bypasses service block.
     * Tooltip: "Black polo. Red apron. You look the part. Mostly."
     */
    NANDOS_APRON("Nando's Apron"),

    // ── Issue #1037: Northfield Indoor Market ─────────────────────────────────

    /**
     * Counterfeit Watch — sold at Mo's stall for 3 COIN.
     * Fenceable via FenceSystem for 5 COIN (50% markup).
     * Triggers Trading Standards confiscation if in player stall during a raid.
     */
    COUNTERFEIT_WATCH("Counterfeit Watch"),

    /**
     * Knockoff Designer T-Shirt — sold at Sheila's stall for 1 COIN.
     * Fenceable via FenceSystem at 50% markup.
     * DisguiseSystem: reduces NPC recognition by 15% (looks like a punter).
     */
    KNOCKOFF_DESIGNER_TSHIRT("Knockoff Designer T-Shirt"),

    /**
     * Tracksuit Bottoms — sold at Sheila's stall for 2 COIN.
     * DisguiseSystem: reduces NPC recognition by 15%.
     */
    TRACKSUIT_BOTTOMS("Tracksuit Bottoms"),

    /**
     * Football Shirt — sold at Sheila's stall for 2 COIN.
     * No special effects beyond being cheap clothes.
     */
    FOOTBALL_SHIRT("Football Shirt"),

    /**
     * Dodgy Charger — sold at Dave's stall for 2 COIN.
     * 20% chance of spawning ELECTRICAL_FIRE prop when placed in a building.
     */
    DODGY_CHARGER("Dodgy Charger"),

    /**
     * Old Telly — sold at Dave's stall for 3 COIN.
     * A battered old CRT television. Large, heavy, and nearly worthless.
     */
    OLD_TELLY("Old Telly"),

    /**
     * Extension Lead — sold at Dave's stall for 1 COIN.
     * Cheap electrical item. No special function.
     */
    EXTENSION_LEAD("Extension Lead"),

    /**
     * Jam Doughnut — sold at Brenda's stall for 1 COIN.
     * Restores +20 Hunger. Sugary and entirely adequate.
     */
    JAM_DOUGHNUT("Jam Doughnut"),

    // ── Issue #1041: Northfield Argos ─────────────────────────────────────────

    /**
     * Argos Pencil — the tiny stubby pencil provided free at Argos for writing slips.
     * Stealing 5 triggers TRESPASS in CriminalRecord and unlocks PENCIL_THIEF achievement.
     * Tooltip on first pickup: "It's just a pencil. A very specific, very Argos pencil."
     */
    ARGOS_PENCIL("Argos Pencil"),

    /**
     * Argos Order Slip — the completed paper slip written at the catalogue desk.
     * Holds a 4-digit item number. Hand to ARGOS_CLERK to receive a collection number.
     * Single use; consumed when order is placed.
     */
    ARGOS_ORDER_SLIP("Argos Order Slip"),

    /**
     * Argos Collection Ticket — issued by ARGOS_CLERK after a slip is accepted.
     * Holds the collection number. Press E on ARGOS_COUNTER_PROP to collect order
     * when called. Wait time: 60–120 real seconds.
     */
    ARGOS_COLLECTION_TICKET("Argos Collection Ticket"),

    /**
     * Folding Chair (Argos) — catalogue item #1234.
     * A lightweight folding chair from the Argos furniture range.
     * Restores 5 Warmth when sat in (placed as prop).
     */
    FOLDING_CHAIR("Folding Chair"),

    /**
     * Kettle (Argos) — catalogue item #2156.
     * A standard electric kettle. Can be placed in a squat to brew tea (+10 Warmth).
     * Craftable into MAKESHIFT_WEAPON (3 hits, 8 damage) with SCRAP_METAL.
     */
    KETTLE("Kettle"),

    /**
     * Toaster (Argos) — catalogue item #3421.
     * A two-slice toaster. Placed at squat, generates TOAST (food, +15 Hunger) once per hour.
     */
    TOASTER("Toaster"),

    /**
     * Gold Chain (Argos) — catalogue item #4567.
     * A chunky gold-effect chain necklace. Wearable cosmetic; +5 Notoriety display while worn.
     * Sellable at pawn shop for 4 COIN.
     */
    GOLD_CHAIN("Gold Chain"),

    /**
     * Portable Radio (Argos) — catalogue item #5102.
     * A battery-powered portable radio. Placed at squat, satisfies BORED need for 3 nearby NPCs.
     * Required component for PIRATE_FM transmitter build.
     */
    PORTABLE_RADIO("Portable Radio"),

    /**
     * Air Fryer (Argos) — catalogue item #7801.
     * The modern British kitchen staple. Placed at squat, generates HOT_FOOD (food, +25 Hunger)
     * once per 30 in-game minutes.
     */
    AIR_FRYER("Air Fryer"),

    /**
     * Duvet (Argos) — catalogue item #8045.
     * A 10.5 tog duvet. Placed at squat bed, grants WARM_SLEEP buff: +15 Warmth on wake.
     */
    DUVET("Duvet"),

    /**
     * Electric Heater (Argos) — catalogue item #8921.
     * A plug-in convector heater. Placed at squat, increases ambient Warmth rate by +2/min.
     */
    ELECTRIC_HEATER("Electric Heater"),

    /**
     * Alarm Clock (Argos) — catalogue item #9012.
     * A battery alarm clock. Press E to set wake time; player auto-wakes at that hour.
     */
    ALARM_CLOCK("Alarm Clock"),

    /**
     * Kids Bike (Argos) — catalogue item #9411.
     * A small child's bicycle. Can be resold at market stall for 3 COIN or fenced for 2 COIN.
     */
    KIDS_BIKE("Kids Bike"),

    /**
     * Layby Receipt — document proving a layby deposit has been paid.
     * Holds item number, deposit amount, and remaining instalments.
     * Losing this causes inability to claim the reserved item.
     */
    LAYBY_RECEIPT("Layby Receipt"),

    /**
     * Marchetti Package — the item collected at Argos using slip #9999.
     * Delivered to a Marchetti faction contact for 10 COIN + 5 respect.
     * Police nearby adds SUSPICIOUS_PACKAGE suspicion to CriminalRecord.
     * Tooltip: "Don't open it. Seriously."
     */
    MARCHETTI_PACKAGE("Marchetti Package"),

    // ── Issue #1045: Northfield Council Flats ────────────────────────────────

    /**
     * Parcel Delivery — a parcel collected from the Post Office for a FLAT_RESIDENT
     * side-quest. Delivering it earns 3 COIN. Single use; consumed on delivery.
     */
    PARCEL_DELIVERY("Parcel Delivery"),

    /**
     * Master Key — earned by completing Donna's eviction quest chain (tower 1).
     * Opens any FLAT_DOOR_PROP in tower 1 without knocking.
     * Tooltip: "Opens every door on the estate. Don't lose it."
     */
    MASTER_KEY("Master Key"),

    // ── Issue #1047: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Petrol Can (Full) — an empty {@link #PETROL_CAN} filled at a forecourt pump for 5 COIN.
     * Required by {@link ragamuffin.core.WheeliBinFireSystem} to ignite wheelie bins.
     * Non-stackable (max 1 per slot). Movement speed −5% while carrying.
     * Tooltip: "Full to the brim. Handle with care."
     */
    PETROL_CAN_FULL("Petrol Can (Full)"),

    // ── Issue #1049: Northfield Chippy — Tony's Chip Shop ─────────────────────

    /**
     * Bread — a plain white loaf. Required in inventory to unlock CHIP_BUTTY on Tony's menu.
     * Also sold at the corner shop and Tesco Express.
     */
    BREAD("Bread"),

    /**
     * Cold Chips — leftover CHIPS obtained by breaking into the chippy after closing time.
     * Restores only +10 hunger. Tooltip: "They're cold. And a bit sad. But they're yours."
     */
    COLD_CHIPS("Cold Chips"),

    /**
     * Cooking Oil — looted from the chippy's lard bucket prop after closing.
     * Crafting input: COOKING_OIL → FIRE_STARTER (combine with NEWSPAPER) or
     * MOLOTOV (combine with BOTTLE_OF_WATER + CLOTH).
     */
    COOKING_OIL("Cooking Oil"),

    /**
     * Fire Starter — crafted from COOKING_OIL + NEWSPAPER.
     * Ignites a campfire or wheelie bin in one use. Consumed on use.
     */
    FIRE_STARTER("Fire Starter"),

    /**
     * Molotov Cocktail — crafted from COOKING_OIL + BOTTLE_OF_WATER + CLOTH.
     * Throwable incendiary. Records {@link ragamuffin.core.CriminalRecord.CrimeType#ARSON}
     * on use. Earns LARD_ALCHEMIST achievement when first crafted.
     */
    MOLOTOV("Molotov Cocktail"),

    /**
     * Lard Bucket — a prop item obtained from the chippy fryer after break-in.
     * Converted in inventory to COOKING_OIL (×3) when examined (press E while holding).
     * Tooltip: "A bucket of old lard. Tony won't miss it. Probably."
     */
    LARD_BUCKET("Lard Bucket"),

    // Issue #1053: Northfield Ladbrokes — BettingShopSystem
    /**
     * Betting Slip (Blank) — taken from Derek's float tray during after-hours break-in.
     * Fenceable for 4 COIN. Tooltip: "Old Derek's float? Nice."
     */
    BETTING_SLIP_BLANK("Betting Slip (Blank)"),

    /**
     * Racing Post — today's horse form guide from the counter prop.
     * Reading grants +5% payout bonus on next horse bet (once per day).
     * Fenceable for 1 COIN.
     */
    RACING_POST("Racing Post"),

    // Issue #1055: Northfield War Memorial — StatueSystem
    /**
     * Bread Crumbs — crafted from 1 × BREAD. Placing within 1 block of the war memorial
     * attracts 8 pigeons simultaneously, enabling wild pigeon capture.
     * Achievement: STATUE_SNACK.
     */
    BREAD_CRUMBS("Bread Crumbs"),

    /**
     * Chain — crafted from 3 × METAL_SCRAP. Alternative to ROPE for statue toppling.
     * Fenceable for 3 COIN.
     */
    CHAIN("Chain"),

    /**
     * Firework — crafted from 1 × WIRE + 1 × COIN. Used during Bonfire Night at the statue.
     * 5% misfire chance ignites nearby LEAVES/WOOD blocks and triggers FireStationSystem.
     */
    FIREWORK("Firework"),

    /**
     * Time Capsule — loot from toppling the war memorial statue.
     * Flavour text: "Northfield District Council, 1953. Contents: a copy of the Birmingham Post and a Woodbine."
     * Fenceable for 12 COIN.
     */
    TIME_CAPSULE("Time Capsule"),

    /**
     * Poppy — seasonal loot at the Remembrance Sunday event at the statue.
     * 3–5 available from the poppy wreath prop. Fenceable for 1 COIN each.
     */
    POPPY("Poppy"),

    /**
     * Cleaning Supplies — dropped by the COUNCIL_CLEANER NPC when they scrub the statue.
     * Fenceable for 2 COIN equivalent or used as a crafting ingredient.
     */
    CLEANING_SUPPLIES("Cleaning Supplies"),

    /**
     * Placard — held by protestors at the war memorial. Player can pick it up.
     * Achievement: PLACARD_PINCHER. Fenceable for 2 COIN.
     */
    PLACARD("Placard"),

    // ── Issue #1063: Northfield Social Club ───────────────────────────────────

    /**
     * Membership Card — Northfield Social Club. Est. 1963. No football colours.
     * Grants permanent bar access and eligibility for committee elections.
     */
    MEMBERSHIP_CARD("Membership Card"),

    /**
     * Pack of Nuts — dry roasted. Half of them are skin.
     * Available at the Social Club bar for members. 1 COIN.
     */
    PACK_OF_NUTS("Pack of Nuts"),

    /**
     * Pork Scratchings — technically food.
     * Available at the Social Club bar for members. 1 COIN.
     */
    PORK_SCRATCHINGS("Pork Scratchings"),

    /**
     * Raffle Ticket — you've probably not won.
     * Purchased from the Social Club raffle draw for 1 COIN each (up to 3).
     */

    /**
     * Meat Raffle Prize — a frozen chicken. Still in the bag.
     * Win from the Social Club raffle draw. Worth 4 COIN at the fence.
     */
    MEAT_RAFFLE_PRIZE("Meat Raffle Prize"),

    // ── Issue #1071: Northfield Fast Cash Finance ─────────────────────────
    /**
     * Loan Leaflet — a garish fluorescent flyer picked up from the LEAFLET_RACK_PROP
     * inside Fast Cash Finance. Readable item: tooltip reads
     * "Representative example: borrow £10 for a week. Repay £15. TOTAL: despair."
     * Crafting substitute for CARDBOARD in paper-based recipes.
     * Fenceable for 0 COIN (worthless) but useful as crafting material.
     */
    LOAN_LEAFLET("Loan Leaflet"),

    /**
     * Threatening Letter — sent by Fast Cash Finance (Barry) on first missed repayment.
     * Dropped into the player's letterbox prop. Readable: "Dear Sir/Madam, this is your
     * FINAL NOTICE before we take further action." Has no gameplay value but raises
     * Notoriety +1 when found in inventory during a police stop-and-search.
     */
    THREATENING_LETTER("Threatening Letter"),

    /**
     * Debt Advice Letter — issued by CitizensAdviceSystem in exchange for LOAN_LEAFLET.
     * Use at Fast Cash Finance counter before signing a new loan to apply a 20% interest
     * reduction on that loan. One-time use; consumed on loan application.
     * Tooltip: "Know your rights. You have some. Not many, but some."
     */
    DEBT_ADVICE_LETTER("Debt Advice Letter"),

    // ── Issue #1077: Northfield Chinese Takeaway — Golden Palace ─────────────

    /** Prawn Crackers — 1 COIN. Hunger −10. Also a litter prop near the Golden Palace 22:00–00:00. */
    PRAWN_CRACKERS("Prawn Crackers"),

    /** Spring Rolls — 2 COIN. Hunger −20. Two served; shareable with NPC. */
    SPRING_ROLLS("Spring Rolls"),

    /** Chicken Chow Mein — 4 COIN. Hunger −50, Warmth +10. Post-pub favourite. */
    CHICKEN_CHOW_MEIN("Chicken Chow Mein"),

    /** Egg Fried Rice — 2 COIN. Hunger −30. */
    EGG_FRIED_RICE("Egg Fried Rice"),

    /** Sweet and Sour Chicken — 4 COIN. Hunger −50. */
    SWEET_AND_SOUR_CHICKEN("Sweet and Sour Chicken"),

    /** Crispy Duck — 6 COIN. Hunger −60, Warmth +15. Requires 20–40s wait. No refund if order cancelled. */
    CRISPY_DUCK("Crispy Duck"),

    /** Fortune Cookie — 1 COIN. On use: shows a random British fortune string. No hunger gain. */
    FORTUNE_COOKIE("Fortune Cookie"),

    /** Takeaway Bag — given free with any order ≥ 3 COIN. Inventory slot container. */
    TAKEAWAY_BAG("Takeaway Bag"),

    // ── Issue #1079: Northfield Magistrates' Court ─────────────────────────

    /**
     * Court Summons — issued to the player after arrest for a non-minor offence.
     * Schedules a court appearance 3 in-game days from issue date.
     * Tooltip: "Northfield Magistrates' Court. Room 2. Do not be late."
     */
    COURT_SUMMONS("Court Summons"),

    /**
     * Community Service Slip — issued as part of a Community Service sentence.
     * Present this at JobCentreSystem, FoodBankSystem, or AllotmentSystem to
     * begin a work shift. Each completed 10-minute shift grants Notoriety −5.
     * Skipping a shift: Notoriety +8 and an immediate warrant.
     * Tooltip: "Report to your assigned service point by 09:00. Or else."
     */
    COMMUNITY_SERVICE_SLIP("Community Service Slip"),

    /**
     * Forged Document — obtained from FenceSystem.
     * When presented at the magistrates' court hearing, swaps a serious charge
     * for a minor public order offence.
     * Adds PERVERTING_COURSE_OF_JUSTICE to CriminalRecord if caught (15% detection chance).
     * Tooltip: "Almost real. Probably good enough. Definitely illegal."
     */
    FORGED_DOCUMENT("Forged Document"),

    // ── Issue #1081: Northfield Pet Shop & Vet — Paws 'n' Claws ─────────────

    /**
     * Dog Treats — purchasable from Bev at Paws 'n' Claws (1 COIN per pack).
     * Reduces dog companion HUNGER by 40. Also satisfies NPC HUNGRY need (≥ 50)
     * if an adjacent NPC is hungry (emergency give). One use per pack.
     * Tooltip: "Bisto-flavour — apparently. The dog loves them."
     */
    DOG_TREATS("Dog Treats"),

    /**
     * Dog Lead — purchasable from Bev (2 COIN). Equippable leash item.
     * When equipped: police treat dog as "controlled" and do not cite the player
     * for dangerous dog (avoids Notoriety +3 and DANGEROUS_DOG criminal record entry).
     * Tooltip: "Keep him close or you'll regret it near the station."
     */
    DOG_LEAD("Dog Lead"),

    /**
     * Dog Vet Record — issued by Dr. Patel after a dog vaccination (8 COIN).
     * Allows dog companion entry to BOOT_SALE with +5% loot value bonus.
     * Required for BEST_IN_SHOW achievement.
     * Tooltip: "Official. A4. Laminated. Dr. Patel's handwriting is terrible."
     */
    DOG_VET_RECORD("Dog Vet Record"),

    /**
     * Dog Sedative — looted from the vet medicine cabinet.
     * One-use throwable item: when thrown at any NPC, reduces their speed by 50%
     * for 30 in-game seconds. Adding this to inventory from the medicine cabinet
     * raid triggers Notoriety +12 and Wanted Tier 2.
     * Tooltip: "Not for human consumption. Not that you're considering it."
     */
    DOG_SEDATIVE("Dog Sedative"),

    /**
     * Budgie — purchasable from Bev at Paws 'n' Claws (3 COIN).
     * Placed in squat via SquatSystem: adds +5 Vibe. Does not follow player.
     * Tooltip: "His name is Gary. He knows three words. One of them is rude."
     */
    BUDGIE("Budgie"),

    /**
     * Goldfish — purchasable from Bev at Paws 'n' Claws (1 COIN).
     * Placed in squat: adds +2 Vibe. Dies after 3 in-game days if not "fed"
     * (press E on squat's fish prop once per day). When goldfish dies, Vibe −2.
     * Tooltip: "His name is Gary. The other Gary."
     */
    GOLDFISH("Goldfish"),

    // ── Issue #1085: Northfield Internet Café — Cybernet ─────────────────────

    /**
     * Blank Paper — a ream of plain A4 paper.
     * Purchasable from Asif at Cybernet (1 COIN per 5 sheets), or looted from
     * the Post Office stationery shelf (3 COIN value, minor theft).
     * Required for printing FORGED_DOCUMENT, FAKE_ID, and COUNTERFEIT_NOTE
     * at the Cybernet printer.
     */
    BLANK_PAPER("Blank Paper"),

    /**
     * Cloned Phone Data — a copy of a phone's identity extracted at Cybernet.
     * Produced by the "Clone phone details" terminal action (3 COIN + STOLEN_PHONE).
     * One-use; enables PhoneRepairSystem.unlockWithClonedData() to unlock a phone
     * without needing Tariq's service.
     */
    CLONED_PHONE_DATA("Cloned Phone Data"),

    /**
     * Mining Rig Component — hardware from a destroyed MINING_RIG_PROP.
     * Drops when a MINING_RIG_PROP (back room of Cybernet) is destroyed.
     * Sellable at ScrapyardSystem for 4 COIN per unit, or at FenceSystem for
     * 7 COIN ("dodgy GPUs"). 3 units total in the back room.
     */
    MINING_RIG_COMPONENT("Mining Rig Component"),

    /**
     * Coin Roll — a paper roll of 5 COIN (currency bundle).
     * Drops from the CASH_BOX_PROP in Cybernet's back room alongside loose coin.
     * Tradeable or usable directly as 5 COIN value.
     */
    COIN_ROLL("Coin Roll"),

    /**
     * Cybernet Membership Card — a loyalty card from Asif's internet café.
     * Purchasable from Asif for 5 COIN. Grants a 20% discount on all terminal
     * time (session cost reduced) for 7 in-game days.
     * Tooltip: "Five quid for a card that saves you 20p. Asif's no mug."
     */
    CYBERNET_MEMBERSHIP_CARD("Cybernet Membership Card"),

    // ── Issue #1224: Northfield Cybernet Internet Café — Online Marketplace ──

    /**
     * Printer Ink — a cartridge of black ink for the PRINTER_PROP at Cybernet.
     * Purchased from PoundShopSystem or NewsagentSystem (2 COIN).
     * One cartridge = one document printed. Required for FORGED_UC_LETTER and
     * FAKE_REFERENCE_LETTER; also needed for FORGED_TV_LICENCE.
     */
    PRINTER_INK("Printer Ink"),

    /**
     * Forged UC Letter — a forged Universal Credit letter printed at Cybernet's
     * back-room PRINTER_PROP (1 BLANK_PAPER + 1 PRINTER_INK).
     * Bypasses the DWPSystem 3-day waiting period when presented at JobCentreSystem.
     * 20% detection chance; triggers DOCUMENT_FRAUD on CriminalRecord if caught.
     */
    FORGED_UC_LETTER("Forged UC Letter"),

    /**
     * Fake Reference Letter — a forged employment reference printed at Cybernet's
     * back-room PRINTER_PROP (1 BLANK_PAPER + 1 PRINTER_INK).
     * Presented at TempAgencySystem to qualify for higher-paid shifts without
     * prior work history. Triggers DOCUMENT_FRAUD on CriminalRecord if detected.
     */
    FAKE_REFERENCE_LETTER("Fake Reference Letter"),

    // ── Issue #1087: The Vaults Nightclub ─────────────────────────────────────

    /**
     * Alcopop — a sugary bottled drink sold at The Vaults bar for 1 COIN.
     * DrunkenessLevel +1 on consumption.
     */
    ALCOPOP("Alcopop"),

    /**
     * Double Vodka — strong spirit sold at The Vaults bar for 1 COIN.
     * DrunkenessLevel +2 on consumption (great value).
     */
    DOUBLE_VODKA("Double Vodka"),

    /**
     * Pills — sold by The Dealer in the club toilets (4 COIN ×2).
     * Effect: DrunkenessLevel −1, movement speed +20% for 5 in-game minutes,
     * then DrunkenessLevel +2 crash.
     * Purchase triggers 10% undercover bust check.
     */
    PILLS("Pills"),

    /**
     * Faction Pass — grants free VIP entry to The Vaults, skipping the queue.
     * Obtainable by completing a Marchetti mission outside the club.
     * Big Dave tips his chin when you show it.
     */
    FACTION_PASS("Faction Pass"),

    /**
     * Phone Tracker Item — given by Tony Marchetti for the tracker side mission.
     * One-use; must be pickpocketed into a named STREET_LADS NPC's jacket.
     * On success: MARCHETTI_CREW Respect +15, 10 COIN reward.
     */
    PHONE_TRACKER_ITEM("Phone Tracker"),

    /**
     * Lost Wallet — drops from a random NPC at closing time chaos (02:45).
     * Contains 3–8 COIN (revealed on E). Can be returned to the police station
     * for Notoriety −2, or kept for the cash.
     */
    LOST_WALLET("Lost Wallet"),

    /**
     * Cigarettes — sold from the CIGARETTE_MACHINE_PROP (5× for 2 COIN).
     * Smoking one (E while selected) reduces DrunkenessLevel −1
     * but adds Notoriety +1 (anti-social behaviour).
     */
    CIGARETTES("Cigarettes"),

    /**
     * Club Wristband — issued on entry to The Vaults.
     * Required to re-enter the same night without paying again.
     * Consumed on exit if not re-entering.
     */
    CLUB_WRISTBAND("Club Wristband"),

    // ── Issue #1089: Northfield BP Petrol Station — 24-Hour Forecourt ──────────

    /**
     * Forecourt Pasty — a sausage-roll-shaped mystery parcel from the PASTY_WARMER_PROP.
     * Costs 1 COIN. Satisfies HUNGRY −30; 20% chance of PASTY_REGRET debuff
     * (stomach pain: −5 speed for 90s). Tooltip: "It's been there since Tuesday."
     */
    FORECOURT_PASTY("Forecourt Pasty"),

    /**
     * Motor Oil — a litre of 10W-40 from the BP kiosk. Costs 3 COIN.
     * Crafting ingredient: PETROL_CAN + MOTOR_OIL + RAG → MOLOTOV_COCKTAIL.
     * Also used for vehicle maintenance in CarDrivingSystem.
     */
    MOTOR_OIL("Motor Oil"),

    /**
     * Rag — a dirty cloth, sold cheaply from the BP kiosk for 1 COIN.
     * Crafting ingredient: PETROL_CAN + MOTOR_OIL + RAG → MOLOTOV_COCKTAIL.
     */
    RAG("Rag"),

    /**
     * Molotov Cocktail (BP variant) — crafted from PETROL_CAN + MOTOR_OIL + RAG.
     * Throwable incendiary (right-click while selected): ignites a 3×3 fire area
     * for 10 seconds, triggers {@code WheeliBinFireSystem.onFireStarted()} at impact.
     * Records {@link ragamuffin.core.CriminalRecord.CrimeType#ARSON} on throw.
     * Notoriety +5 per throw. Cannot be used indoors.
     * Tooltip: "Handle with extreme care. And run."
     */
    MOLOTOV_COCKTAIL("Molotov Cocktail (Petrol)"),

    /**
     * Siphon Hose — sold at the Builders Merchant. Used with Stealth Tier 2+ to siphon
     * petrol from parked cars into an empty PETROL_CAN. Takes 10 real seconds.
     * Generates noise LEVEL_1; caught by PCSO within 10 blocks (+2 Notoriety, CrimeType.THEFT).
     * Tooltip: "Don't ask where it's been."
     */
    SIPHON_HOSE("Siphon Hose"),

    // ── Issue #1091: Northfield Nando's ────────────────────────────────────────

    /**
     * Chicken Wrap — Nando's menu item. 5 COIN. Restores +35 Hunger.
     * Available all day during opening hours.
     * Tooltip: "Flatbread. Chicken. Slaw. It's a wrap."
     */
    CHICKEN_WRAP("Chicken Wrap"),

    /**
     * Bottomless Drink — Nando's refillable soft drink. 2 COIN.
     * Restores +20 Thirst per refill. Counts as one item on the order.
     * Tooltip: "Bottomless. Unlike your dignity."
     */
    BOTTOMLESS_DRINK("Bottomless Drink"),

    /**
     * Peri-Peri Sauce — a bottle of house peri-peri sauce from the HOT_SAUCE_RACK_PROP.
     * Throwable: creates a 2-block PERI_SAUCE_SLICK ground prop (60s lifetime)
     * that slows any NPC/player by 30% on contact.
     * Records CrimeType.AFFRAY and adds +3 Notoriety when thrown.
     * Tooltip: "Handle with care. It's sticky and it stings."
     */
    PERI_PERI_SAUCE("Peri-Peri Sauce"),

    /**
     * Peri Chips — Nando's seasoned chips. 3 COIN. Restores +25 Hunger.
     * Tooltip: "Just chips. But better chips."
     */
    PERI_CHIPS("Peri Chips"),

    /**
     * Till Receipt — a printed receipt from the Nando's CARD_MACHINE_PROP.
     * Evidence item found in the SAFE_PROP alongside stolen coin.
     * Can be handed to police (tips off money laundering route, −10 Notoriety)
     * or sold to Marchetti via FactionSystem for 3 COIN.
     * Tooltip: "Order #47. Paid by card. Name: D. MARCHETTI."
     */
    TILL_RECEIPT("Till Receipt"),

    // ── Issue #1094: Northfield By-Election ──────────────────────────────────

    /**
     * Campaign Leaflet — paper flyer handed out by the PARTY_VOLUNTEER at the
     * CANVASSING_TABLE_PROP (receive 10 per visit).
     * Deliver to a residential DOOR_PROP for +1 vote to the corresponding party.
     * Can be crafted with TOBACCO into a ROLLIE (achievement: POLITICAL_SMOKER).
     * Tooltip: "Technically you're supposed to deliver these, not smoke them."
     */

    /**
     * Nomination Form — issued by the RETURNING_OFFICER NPC to the player when
     * Community Respect ≥ 40 and Notoriety ≤ 20.
     * Submit at the POLLING_STATION_PROP to register as Independent candidate
     * (starts at 20 votes).
     * Tooltip: "Your name, printed above the fold. Local democracy in action."
     */
    NOMINATION_FORM("Nomination Form"),

    /**
     * Ballot Box — the sealed ballot box at the BALLOT_BOX_PROP.
     * Obtained by holding E for 8 seconds on polling day after 18:00
     * (requires Notoriety ≤ 30 or COUNCIL_JACKET disguise).
     * Voids the election result. Sellable to the Fence for 15 COIN.
     * Records CrimeType.ELECTION_INTERFERENCE; +25 Notoriety, +2 Wanted stars.
     * Tooltip: "Heavy. Smells of old felt-tips. Don't drop it."
     */
    BALLOT_BOX("Ballot Box"),

    /**
     * Councillor Badge — awarded to the Independent candidate on winning the by-election.
     * Grants Community Respect +30, Notoriety −15, and the PEOPLES_CHAMPION achievement.
     * Wearable cosmetic; increases NPC cooperation chance by 15% while equipped.
     * Tooltip: "Cllr. [Your Name]. It's laminated and everything."
     */
    COUNCILLOR_BADGE("Councillor Badge"),

    /**
     * Rollie — crafted from 1 CAMPAIGN_LEAFLET + 1 TOBACCO.
     * A hand-rolled cigarette made from a campaign pamphlet.
     * Restores +5 energy. Earns the POLITICAL_SMOKER achievement on first craft.
     * Tooltip: "You've rolled a Tory manifesto into a cigarette. Somehow fitting."
     */
    ROLLIE("Rollie"),

    // ── Issue #1096: Sunday League Football ──────────────────────────────────

    /**
     * Football — a standard five-a-side match ball.
     * Craftable: 1 LEATHER + 1 RUBBER. Also found in skips.
     * Can be placed as a prop or kicked by walking into it.
     * Required for the FootballSystem match; spawned by FootballSystem at kick-off.
     * Tooltip: "They say football is the beautiful game. Not on a Sunday in Northfield."
     */
    FOOTBALL("Football"),

    /**
     * Referee Whistle — dropped by the REFEREE NPC (10% chance on defeat).
     * Blowing it (press E while holding) causes all NPCs within 10 blocks to
     * pause briefly.
     * Tooltip: "One blast stops play. Two blasts gets you glassed."
     */
    REFEREE_WHISTLE("Referee Whistle"),

    // ── Issue #1098: Northfield Summer Fete ──────────────────────────────────

    /**
     * Victoria Sponge — 2 COIN at the cake stall. Hunger +30.
     * Can be gifted to an NPC for Friendly status.
     * Tooltip: "Technically two sponges. The jam is the star."
     */
    VICTORIA_SPONGE("Victoria Sponge"),

    /**
     * Scone — 1 COIN at the cake stall. Hunger +15.
     * Tooltip: "Best eaten with cream and jam. In that order."
     */
    SCONE("Scone"),

    /**
     * Cupcake — 1 COIN at the cake stall. Hunger +10.
     * Tooltip: "Small. Iced. Suspiciously neat."
     */
    CUPCAKE("Cupcake"),

    /**
     * Jam and Cream — 1 COIN at the cake stall. Hunger +12.
     * The topping for scones. Also sold separately.
     * Tooltip: "A small pot of cream and jam. No scone included. Typical."
     */
    JAM_AND_CREAM("Jam and Cream"),

    /**
     * Cuddly Toy — tombola or Hook-a-Duck prize. Fence buy: 1 COIN.
     * Gift to a child NPC for +10 Street Rep.
     * Tooltip: "A stuffed bear with one eye. He's been to better fetes."
     */
    CUDDLY_TOY("Cuddly Toy"),

    /**
     * Ornament — bric-a-brac rummage find. Sell value: 2 COIN; fence buy: 1 COIN.
     * Tooltip: "A ceramic horse. Hideous. Someone's gran loved it."
     */
    ORNAMENT("Ornament"),

    /**
     * Retro Cassette — bric-a-brac rummage find. No value; flavour item.
     * Tooltip: "Still plays. Nobody owns a cassette player."
     */
    RETRO_CASSETTE("Retro Cassette"),

    /**
     * Rigged Barrel — crafted from 1 WOOD + 1 RAFFLE_TICKET.
     * Hold E on RAFFLE_TICKET_STALL_PROP for 3 seconds (no volunteer within 8 blocks)
     * to swap the raffle draw and guarantee 1st prize.
     * Single use. Triggers CrimeType.FRAUD on successful swap.
     * Tooltip: "Looks like a barrel. Isn't quite a barrel."
     */

    // ── Issue #1100: Northfield Council Flats — Kendrick House ───────────────

    /**
     * Housing Priority Letter — issued by Derek (COUNCIL_MEMBER) after passing a
     * clean housing inspection at Kendrick House. Required with 50 COIN at the
     * council office to claim a flat via {@link ragamuffin.core.PropertySystem}.
     * Fence value: 15 COIN at the council office counter.
     * Tooltip: "Congratulations. You're on the list. Don't hold your breath."
     */
    HOUSING_PRIORITY_LETTER("Housing Priority Letter"),

    /**
     * Stolen Parcel — a parcel lifted from the LETTERBOX_BANK_PROP in Kendrick House.
     * Fence value: 60% of base value (varies 3–8 COIN). Marked as stolen.
     * Possession triggers HANDLING_STOLEN_GOODS if police search the player.
     * Tooltip: "Someone's Amazon order. Their loss."
     */
    STOLEN_PARCEL("Stolen Parcel"),

    /**
     * Stolen Goods — generic contraband label applied to items pilfered from flats.
     * Seized by Derek during the housing inspection if found near the player.
     * Fence value: 4 COIN. Triggers PARCEL_THEFT crime on discovery.
     * Tooltip: "Didn't come from a shop. Well, it did. But not yours."
     */
    STOLEN_GOODS("Stolen Goods"),

    /**
     * Suit Jacket — a smart jacket worn as a disguise.
     * When worn as active disguise ({@link ragamuffin.core.DisguiseSystem}),
     * Derek (COUNCIL_MEMBER) skips the player's floor during the housing inspection.
     * Tooltip: "Looks like you've got your life together. You don't, but it helps."
     */
    SUIT_JACKET("Suit Jacket"),

    // ── Issue #1104: Northfield Community Centre ──────────────────────────────

    /**
     * Council Minutes — stolen document from the Northfield Council Budget Meeting.
     * Required for the Marchetti Crew mission at {@link ragamuffin.world.LandmarkType#COMMUNITY_CENTRE}.
     * Steal from {@code FOLDING_TABLE_PROP} inside the locked meeting room on Tuesday 10:00–12:00.
     * Return to Marchetti contact at the pub for 15 COIN reward.
     * Tooltip: "Twelve pages of bureaucratic nothing — and someone wants it very badly."
     */
    COUNCIL_MINUTES("Council Minutes"),

    /**
     * Cake Slice — a slice of homemade cake from the Sunday Cake Bake-Off stall.
     * Cost: 1 COIN. Effects: {@link ragamuffin.core.HealingSystem} +8 HP,
     * {@link ragamuffin.core.WarmthSystem} +5. Satisfies hunger.
     * Tooltip: "Victoria sponge. Proper job. Someone's nan made it."
     */
    CAKE_SLICE("Cake Slice"),

    // ── Issue #1106: Angel Nails & Beauty ────────────────────────────────────

    /**
     * Gel Polish — bottle of professional gel nail polish from the salon shelf.
     * Fence value: 3 COIN. Pawn value: 1 COIN.
     * Tooltip: "Could be a nice present. Or just nicked."
     */
    GEL_POLISH("Gel Polish"),

    // ── Issue #1108: Northfield Sporting & Social Club ────────────────────────

    /**
     * Member Invite — a guest pass to the Northfield Sporting &amp; Social Club.
     * Obtained from a MEMBER NPC at STREET_LADS Respect ≥ 30.
     * Allows Guest-level entry without paying. One-use.
     */
    MEMBER_INVITE("Member Invite"),

    /**
     * Cheat Sheet — a crib sheet of quiz answers.
     * Found in the library or crafted from NEWSPAPER + PEN.
     * Used during Quiz Night (press F during a question): +2 points, 40% catch chance.
     */
    CHEAT_SHEET("Cheat Sheet"),

    /**
     * Protection Letter — documentary evidence of Derek's protection payments.
     * Found in Derek's back-room desk (requires LOCKPICK to access).
     * Can be used to report to police, blackmail Derek, or give to a journalist.
     */
    PROTECTION_LETTER("Protection Letter"),

    /**
     * Protection Photo — a phone photo of the Marchetti handover in the car park.
     * Taken by pressing F within 4 blocks of the Sunday 19:00 exchange.
     * Functions like PROTECTION_LETTER for expose paths.
     */
    PROTECTION_PHOTO("Protection Photo"),

    // ── Issue #1112: The Raj Mahal — Friday Night Curry ──────────────────────

    /**
     * Chicken Tikka Masala — 6 COIN. Hunger −70, Warmth +15. Grants FULL_STOMACH buff.
     * Tooltip: "The national dish. Probably."
     */
    CHICKEN_TIKKA_MASALA("Chicken Tikka Masala"),

    /**
     * Lamb Balti — 7 COIN. Hunger −80, Warmth +20. Grants FULL_STOMACH buff.
     * Tooltip: "Hot enough to start a fire. Be warned."
     */
    LAMB_BALTI("Lamb Balti"),

    /**
     * Saag Aloo — 3 COIN. Hunger −40, Warmth +5. Grants FULL_STOMACH buff.
     * Tooltip: "Spinach and potato. Somehow greater than the sum of its parts."
     */
    SAAG_ALOO("Saag Aloo"),

    /**
     * Naan Bread — 2 COIN. Hunger −25. Grants FULL_STOMACH buff.
     * Tooltip: "Tear, dip, repeat."
     */
    NAAN_BREAD("Naan Bread"),

    /**
     * Poppadoms — 1 COIN. Hunger −10. Grants FULL_STOMACH buff.
     * Tooltip: "The beginning of something beautiful."
     */
    POPPADOMS("Poppadoms"),

    /**
     * Mango Lassi — 2 COIN. Hunger −15, Warmth +5. Grants FULL_STOMACH buff.
     * Tooltip: "Sweet, cool, and deeply civilised."
     */
    MANGO_LASSI("Mango Lassi"),

    /**
     * BYO Lager Corkage — 1 COIN fee for bringing a CAN_OF_LAGER into the Raj Mahal.
     * Paid to Sanjay. Avoidable at Street Lads Respect ≥ 50.
     * Tooltip: "The price of liberty."
     */
    BYO_LAGER_CORKAGE("BYO Corkage Receipt"),

    /**
     * Folded Note — a sealed envelope passed between Marchetti lieutenants in the
     * Raj Mahal back room on Thursday evenings. Pickpocketable from dining lieutenants.
     * Can be delivered as a mission item or sold to the fence.
     * Tooltip: "Don't open it. Actually — definitely open it."
     */
    FOLDED_NOTE("Folded Note"),

    // ── Issue #1114: Meredith & Sons Funeral Parlour ──────────────────────────

    /**
     * Funeral Flowers — sold by Gerald; can be placed on HEADSTONE props in the cemetery
     * for +2 Community Respect.
     * Tooltip: "They were loved. Probably."
     */
    FUNERAL_FLOWERS("Funeral Flowers"),

    /**
     * Condolences Card — sold by Gerald; give to a BEREAVED NPC for +3 Community Respect.
     * Tooltip: "The right words, printed by someone else."
     */
    CONDOLENCES_CARD("Condolences Card"),

    /**
     * Memorial Candle — sold by Gerald; can be placed at the CASKET prop.
     * Tooltip: "Smells better than the back room."
     */
    MEMORIAL_CANDLE("Memorial Candle"),

    /**
     * Ornament Vase — a personal effect from the casket or deceased's home.
     * Value: 5 COIN at PawnShop; Gerald buys for 2 COIN (50% of PawnShop).
     * Tooltip: "It meant something. To someone."
     */
    ORNAMENT_VASE("Ornament Vase"),

    /**
     * War Medal — personal effect; library trades for TRADING XP.
     * Value: 8 COIN at PawnShop; Gerald buys for 4 COIN.
     * Tooltip: "Earned the hard way. Lost the easy way."
     */
    WAR_MEDAL("War Medal"),

    /**
     * Spare Dentures — cursed personal effect from the casket.
     * Value: 1 COIN at PawnShop. Selling triggers tooltip "You absolute ghoul."
     * Tooltip: "Someone's going to be eating soup tonight."
     */
    SPARE_DENTURES("Spare Dentures"),

    /**
     * Biscuit Tin Savings — a battered tin used as a cash box.
     * Opening in inventory yields 15–35 COIN.
     * Tooltip: "The working class pension fund."
     */
    BISCUIT_TIN_SAVINGS("Biscuit Tin Savings"),

    /**
     * Property Deed — document from the inheritance heist lockbox.
     * Sellable to the estate agent (Baxter's) for 50 COIN.
     * Tooltip: "Someone's whole life on one sheet of paper."
     */
    PROPERTY_DEED("Property Deed"),

    // ── Issue #1122: Sun Kissed Studio — Marchetti front materials ────────────

    /**
     * Marchetti Ledger — a leather-bound accounts book from the back-room safe
     * at Sun Kissed Studio. Contains records of all Marchetti Crew cash drops.
     * Can be delivered to the police station to trigger a 24-hour raid closure,
     * but marks the player as GRASS (−40 Marchetti Respect).
     * Tooltip: "Names, dates, and amounts. The kind of book that gets people killed."
     */
    MARCHETTI_LEDGER("Marchetti Ledger"),

    /**
     * Brown Envelope — an unlabelled cash envelope used for Marchetti Crew drops.
     * Dropped at Sun Kissed Studio at 11:00 and 18:00 daily.
     * Player can intercept (−20 Marchetti Respect, +15–25 COIN) or deliver
     * to Tracey (+3 Respect, +5 COIN).
     * Tooltip: "The lifeblood of Northfield's informal economy."
     */
    BROWN_ENVELOPE("Brown Envelope"),

    // ── Issue #1124: Salvation Army Citadel ───────────────────────────────────

    /**
     * Brass Instrument — shared with BuskingSystem; required to join the Saturday Brass Band.
     * Earns +1 COIN/min and −1 Notoriety per 2 minutes during band session (max −6/session).
     * Tooltip: "The trombone of the downtrodden. Also useful for busking."
     */
    BRASS_INSTRUMENT("Brass Instrument"),

    /**
     * Salvation Army Uniform — stolen from UNIFORM_HOOK_PROP in the Citadel back room.
     * Grants DisguiseSystem −25% recognition while worn.
     * Stripped automatically on a Notoriety spike ≥ 10.
     * Tooltip: "The Lord's armour. Dubiously acquired."
     */
    SALVATION_ARMY_UNIFORM("Salvation Army Uniform"),

    /**
     * Collection Ledger — records donations and expenditure at the Citadel.
     * Can be sold at the Boot Sale for 5 COIN or used as evidence of charity fraud.
     * Tooltip: "Columns of need and generosity, in biro."
     */
    COLLECTION_LEDGER("Collection Ledger"),

    /**
     * Soup Bowl — issued during meal service (12:00–14:00, 18:00–19:30); not portable on its own.
     * Restores +25 Hunger and removes COLD debuff when filled with hot soup.
     * Tooltip: "Thick vegetable soup. There is no shame in this."
     */
    SOUP_BOWL("Soup Bowl"),

    // ── Issue #1126: Northfield Household Waste Recycling Centre ─────────────

    /**
     * Junk Bag — a black bin bag of miscellaneous rubbish. Used in fly-tip jobs at the
     * Recycling Centre (carry to GENERAL_WASTE_BAY_PROP for payment). Also a decorative
     * prop in the general waste bay.
     * Tooltip: "Someone's problem, now yours."
     */
    JUNK_BAG("Junk Bag"),

    /**
     * Circuit Board — electronic component smashed from EWASTE_SKIP_PROP at the Tip.
     * Crafting material for PirateRadioSystem TRANSMITTER upgrades.
     * Sells for 2 COIN at the Scrapyard.
     * Tooltip: "The guts of someone's old PC."
     */
    CIRCUIT_BOARD("Circuit Board"),

    /**
     * Working Laptop — rare find from the Reuse shelf at the Tip. Fenceable at the
     * Pawn Shop for 10 COIN. Selling adds HANDLING_STOLEN_GOODS to CriminalRecord
     * (suspicious resale). Flagged NOT_STOLEN so no Notoriety on standard fence.
     * Tooltip: "Someone chucked this. Their loss."
     */
    WORKING_LAPTOP("Working Laptop"),

    // ── Issue #1128: Northfield NHS Walk-In Centre ─────────────────────────────

    /**
     * Bandage — sterile dressing from the Walk-In Centre medicine cabinet.
     * Heals 5 HP when used. Fenceable for 2 COIN.
     * Tooltip: "Clean, sterile, and technically yours now."
     */
    BANDAGE("Bandage"),

    /**
     * Morphine Ampoule — rare controlled drug, 15% chance in MEDICINE_CABINET_PROP.
     * Heals 30 HP instantly. Fenceable for 10–12 COIN.
     * Triggers MEDICINE_THEFT CriminalRecord and +8 Notoriety on raid.
     * Tooltip: "Single-use. Handle with care."
     */
    MORPHINE_AMPOULE("Morphine Ampoule"),

    /**
     * Tramadol — controlled opioid from CONTROLLED_DRUGS_SAFE_PROP (requires CROWBAR).
     * Heals 15 HP. Fenceable for 6–10 COIN. Triggers CONTROLLED_DRUG_TRAFFICKING
     * CriminalRecord if 3+ units fenced in one session.
     * Tooltip: "Prescription only. Technically."
     */
    TRAMADOL("Tramadol"),

    /**
     * Diazepam — controlled benzodiazepine from CONTROLLED_DRUGS_SAFE_PROP.
     * Reduces fear buff. Fenceable for 6–8 COIN.
     * Tooltip: "Takes the edge off. All of it."
     */
    DIAZEPAM("Diazepam"),

    /**
     * Unused Syringe — found in SHARPS_BIN_PROP or dropped by medical staff.
     * Used in PirateRadioSystem transmitter recipe (adds signal clarity buff).
     * Fenceable for 1 COIN or tradeable at black market.
     * Tooltip: "Still sealed. Mostly."
     */
    UNUSED_SYRINGE("Unused Syringe"),

    /**
     * Discharge Letter — issued by Dr. Okafor after emergency treatment.
     * Accepted by JobCentreSystem as a sick note (same weight as GP_SICK_NOTE).
     * Accepted by LaunderetteSystem as a fraud alibi for 2 COIN.
     * Tooltip: "Official NHS paper. Worth surprisingly little."
     */
    DISCHARGE_LETTER("Discharge Letter"),

    /**
     * Prescription Form — pickpocketable from PENSIONER/PUBLIC NPCs in the
     * Walk-In Centre waiting room (15% chance). Can be used at the pharmacy
     * to obtain prescription items. Triggers PRESCRIPTION_FRAUD if caught.
     * Tooltip: "Someone else's problem, briefly."
     */
    PRESCRIPTION_FORM("Prescription Form"),

    // ── Issue #1130: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Microwave Pasty — heated in the MICROWAVE_PROP at the BP kiosk for 2 COIN.
     * Restores +25 Hunger. After 21:00 there is a 30% chance of food poisoning
     * debuff: −10 HP over 30s. Tooltip: "Technically a meal. Technically."
     */
    MICROWAVE_PASTY("Microwave Pasty"),

    /**
     * Car Wash Token — purchased from CAR_WASH_TOKEN_MACHINE_PROP for 3 COIN.
     * Redeemed at the automated car wash bay. Can be sold to the Fence for 2 COIN.
     * Tooltip: "Good for one wash. Car not included."
     */
    CAR_WASH_TOKEN("Car Wash Token"),

    /**
     * Cigarette Carton — looted from CIGARETTE_CABINET_PROP after 3 hits.
     * Contains 20 cigarettes. Fenceable for 6 COIN per carton.
     * Tooltip: "Twenty reasons your lungs hate you."
     */
    CIGARETTE_CARTON("Cigarette Carton"),

    /**
     * Map — bought from the BP kiosk for 2 COIN.
     * Using it (press E while selected) reveals 3 nearby undiscovered landmarks
     * on the world map. Single use; consumed on use.
     * Tooltip: "Northfield A–Z. Still relevant, apparently."
     */
    MAP("Map"),

    // ── Issue #1132: Northfield Dog Grooming Parlour — Pawfect Cuts ──────────

    /**
     * Dog Treat — a small bone-shaped biscuit treat sold at Pawfect Cuts
     * from PET_TREAT_DISPLAY_PROP (1 COIN each).
     * Feeding your dog a treat increases Bond Level by +1 (max +5 per day).
     * Also used to bribe the DOG_SHOW_TROPHY_CABINET_PROP guard dog at the scrapyard.
     * Tooltip: "A biscuit for a good boy. Or a bad boy. He doesn't judge."
     */
    DOG_TREAT("Dog Treat"),

    /**
     * Dog Show Rosette — awarded to the winner of the Northfield Dog Show
     * held fortnightly in the park. Three tiers: 3rd Place (yellow), 2nd Place (blue),
     * 1st Place (red). Carrying the red rosette seeds a LOCAL_EVENT rumour.
     * Can be sold to the pawn shop for 5 COIN (1st Place) / 2 COIN (other tiers).
     * Tooltip: "Northfield Dog Show, 1st Place. Tracey looked very proud."
     */
    DOG_SHOW_ROSETTE("Dog Show Rosette"),

    /**
     * Flea Powder — a craftable item (1 SOAP + 1 COIN) that cures the
     * FLEA_INFESTATION debuff. Applied to the dog (press E while selected near dog)
     * or used on the squat bed (press E at BED prop) to remove the sleep penalty.
     * Tooltip: "Smells awful. The dog agrees."
     */
    FLEA_POWDER("Flea Powder"),

    /**
     * Scissors — general-purpose scissors carried by Tracey at Pawfect Cuts.
     * Can be looted if Tracey is knocked out. Used in the Nail Clipping service.
     * As a weapon: deals 3 damage per hit (counts as MELEE).
     * Tooltip: "Don't run with these."
     */
    SCISSORS("Scissors"),

    /**
     * Dog Grooming Voucher — issued by Tracey after a Full Groom service.
     * Redeemable for 50% off the next grooming service of equal or lesser value.
     * Expires after 14 in-game days (one fortnight).
     * Can be sold to other DOG_OWNER NPCs for 3 COIN.
     * Tooltip: "Compliments of Pawfect Cuts. Tracey's handwriting is very neat."
     */
    DOG_GROOMING_VOUCHER("Dog Grooming Voucher"),

    /**
     * Unlicensed Dog — a dog of dubious provenance sold by the DOG_DEALER on
     * Tuesday evenings. Costs 6 COIN; resellable to DOG_OWNER NPCs for 10 COIN
     * (4 COIN profit). Carrying one adds +2 Notoriety if inspected by police.
     * Cannot be adopted as a companion (already "owned" — it's complicated).
     * Tooltip: "No papers. Don't ask. She's lovely though."
     */
    UNLICENSED_DOG("Unlicensed Dog"),

    // ── Issue #1134: Patel's Newsagent ────────────────────────────────────────

    /**
     * Penny Sweets — a paper bag of assorted sweets from Patel's sweet counter.
     * Costs 1 COIN. Satisfies HUNGRY need (−15). Tooltip: "Refreshers, cola bottles,
     * a stray foam shrimp. The works."
     */
    PENNY_SWEETS("Penny Sweets"),

    /**
     * Local Map — a laminated A4 street map of Northfield sold at Patel's.
     * Costs 2 COIN. Reveals fog of war in a 50-block radius of the player's current
     * position on use. Tooltip: "Surprisingly accurate. Raj drew it himself."
     */
    LOCAL_MAP("Local Map"),

    /**
     * Racing Form — a specialist horse-racing magazine sold at Patel's magazine rack.
     * Costs 2 COIN. Grants +15% bet accuracy in HorseRacingSystem for 1 in-game day.
     * Tooltip: "Horse racing tips inside. Raj swears by the Tuesday edition."
     */
    RACING_FORM("Racing Form"),

    /**
     * DIY Monthly — a home-improvement magazine sold at Patel's magazine rack.
     * Costs 2 COIN. Required as a crafting ingredient for PAPIER_MACHE_BRICK.
     * Tooltip: "Twelve pages on grouting. Essential reading."
     */
    DIY_MONTHLY("DIY Monthly"),

    /**
     * Puzzle Book — a crossword and word-search compilation from Patel's rack.
     * Costs 2 COIN. Satisfies BORED need (−20) on use.
     * Tooltip: "A 2,000-word sudoku grid on page 47. You're going to need a pen."
     */
    PUZZLE_BOOK("Puzzle Book"),

    /**
     * Dodgy Magazine — an adult-interest publication kept behind the counter at
     * Patel's News. Only sold to players with Notoriety ≥ 30. Costs 3 COIN.
     * Satisfies BORED need (−25). Shoplifting one adds a SHOPLIFTING CriminalRecord
     * entry, +2 Notoriety, and a 7-day BANNED_FROM_PATEL flag.
     * Tooltip: "Top shelf. Raj keeps them behind the Sporting Life."
     */
    DODGY_MAGAZINE("Dodgy Magazine"),

    /**
     * Newsagent Key — the key to the Patel's News stockroom. Obtained from Raj at
     * STREET_LADS Respect ≥ 40 as part of bulk-tobacco deal or can be pilfered.
     * Unlocks STOCKROOM_DOOR_PROP to access the CASH_BOX_PROP (8–14 COIN robbery).
     * Robbery = THEFT record + Notoriety +4.
     * Tooltip: "A Yale key on a paper-clip fob. Don't lose it."
     */
    NEWSAGENT_KEY("Newsagent Key"),

    /**
     * Newsagent Apron — a blue tabard worn by Raj Patel behind the counter.
     * Dropped if Raj is knocked unconscious. Equipping it reduces police recognition
     * by 10% (same effect as SHOPKEEPER disguise).
     * Tooltip: "Raj's apron. It says 'Patel's News — Est. 1991'."
     */
    NEWSAGENT_APRON("Newsagent Apron"),

    /**
     * Papier Mache Brick — a craftable placeable block made from NEWSPAPER + DIY_MONTHLY.
     * Requires 2 punches to break. Looks like a brick but crumbles on close inspection.
     * Tooltip: "Looks convincing from a distance. Very much from a distance."
     */
    PAPIER_MACHE_BRICK("Papier Mache Brick"),

    // ── Issue #1136: The Vaults Nightclub ─────────────────────────────────────

    /**
     * Smart Shirt — a collared dress shirt worn for nights out.
     * Bypasses Big Dave's reputation door check at The Vaults. Slight warmth penalty
     * (not warm outdoors). Obtainable from the charity shop or crafted.
     * Tooltip: "You scrub up alright, to be fair."
     */
    SMART_SHIRT("Smart Shirt"),

    /**
     * Nightclub Master Key — the manager's master key to The Vaults' back office.
     * Opens NIGHTCLUB_OFFICE_DOOR_PROP; one-use (consumed on door open).
     * Looted from Terry (NIGHTCLUB_MANAGER) or hidden in the VIP area.
     * Tooltip: "A chunky Yale key with a 'T' fob. Opens something important."
     */
    NIGHTCLUB_MASTER_KEY("Nightclub Master Key"),

    // ── Issue #1325: Northfield Nightclub — The Vaults ────────────────────────

    /**
     * Cheap Lager — sold at The Vaults bar for 2 COIN.
     * On consumption: +5 HP, sets DrunkLevel to TIPSY (or higher if already tipsy).
     * Tooltip: "It's flat, it's warm, it's two quid. Welcome to The Vaults."
     */
    CHEAP_LAGER("Cheap Lager"),

    /**
     * Stethoscope — medical instrument repurposed for safe-cracking.
     * Required for the NIGHTCLUB_SAFE_PROP heist (hold E 6 seconds).
     * Fenceable for 8 COIN.
     * Tooltip: "Not borrowed from the NHS. Definitely not."
     */
    STETHOSCOPE("Stethoscope"),

    // ── Issue #1138: Northfield Iceland ───────────────────────────────────────

    /**
     * Frozen Pizza — Iceland own-brand frozen pizza. Part of the three-for-a-fiver party food deal.
     * Satisfies HUNGRY −30 when eaten. Sellable to the Fence for 1 COIN.
     * Tooltip: "Cheese and tomato. Or possibly tomato and cheese. Hard to say."
     */
    FROZEN_PIZZA("Frozen Pizza"),

    /**
     * Prawn Ring — centrepiece of every British party buffet since 1987.
     * Key item in the self-checkout scam: placing on the belt distracts Kevin (SECURITY_GUARD)
     * for 30 seconds, reducing detection chance to 0%.
     * Part of the three-for-a-fiver deal. Satisfies HUNGRY −20.
     * Tooltip: "The gateway drug to a prawn cocktail addiction."
     */
    PRAWN_RING("Prawn Ring"),

    /**
     * Chicken Nuggets — Iceland own-brand nuggets. Part of the three-for-a-fiver party food deal.
     * Satisfies HUNGRY −25. Tooltip: "Forty-seven nuggets for a fiver. Don't ask what's in them."
     */
    CHICKEN_NUGGETS("Chicken Nuggets"),

    /**
     * Iceland Prawn Cocktail — luxury party starter in a plastic tray.
     * High-end item; not part of the basic deal but sold at the counter.
     * Satisfies HUNGRY −35. Tooltip: "Posh. By Iceland standards."
     */
    ICELAND_PRAWN_COCKTAIL("Iceland Prawn Cocktail"),

    /**
     * Frozen Turkey — a whole frozen bird from the Iceland stockroom.
     * 6 are locked in the stockroom Dec 1–24. Stealing all 6 earns the
     * GREAT_TURKEY_HEIST achievement and a NewspaperSystem front page.
     * Satisfies HUNGRY −80 when cooked. Heavy item: -10% player speed while held.
     * Tooltip: "Bernard Matthews would be appalled. Or proud. It's ambiguous."
     */
    FROZEN_TURKEY("Frozen Turkey"),

    /**
     * Christmas Envelope — a paper envelope containing the customer's Christmas Club savings.
     * Handed out by Debbie (ICELAND_MANAGER) in December.
     * Contents: 20–35 COIN. Stealing it from another customer seeds LOCAL_SCANDAL rumour.
     * Tooltip: "Someone's been saving all year for this."
     */
    CHRISTMAS_ENVELOPE("Christmas Envelope"),

    /**
     * Christmas Club Cash Box — the strongbox behind the Iceland counter where
     * Debbie stores all the Christmas Club envelopes.
     * Stealing it earns CHRISTMAS_CLUB_VILLAIN achievement and seeds LOCAL_SCANDAL.
     * Contains 20–35 COIN per enrolled customer.
     * Tooltip: "Heavier than it looks. Also heavier on your conscience. Probably."
     */
    CHRISTMAS_CLUB_CASH_BOX("Christmas Club Cash Box"),

    /**
     * Iceland Staff Key — Debbie's master key to the Iceland stockroom.
     * Opens the STOCKROOM_DOOR_PROP; not consumed on use (can be used multiple times).
     * Looted from Debbie (ICELAND_MANAGER) or found in the stockroom office.
     * Tooltip: "A chunky brass key. 'Iceland Staff Only' stamped on the fob."
     */
    ICELAND_STAFF_KEY("Iceland Staff Key"),

    /**
     * Fake Receipt — a forged till receipt for unpaid self-checkout items.
     * Crafted from 1 NEWSPAPER + 1 COIN at the squat WORKBENCH.
     * Used at the self-checkout: 60% chance Sharon accepts it; if Kevin is distracted
     * by a PRAWN_RING, acceptance chance rises to 100%.
     * Tooltip: "Looks almost right. Squint a bit."
     */
    FAKE_RECEIPT("Fake Receipt"),

    // ── Issue #1142: Northfield RAOB Lodge — Buffaloes, Secret Handshakes & Old Boys' Network ──

    /**
     * Membership Card — issued to INITIATES and BROTHER_BUFFALOs of Northfield Buffaloes Lodge No. 347.
     * Required to pass RAOB_DOORMAN (Big Bernard) entry check.
     * Fence value: 3 COIN.
     * Tooltip: "The Northfield Buffaloes Lodge No. 347. It smells of Worthington's."
     */
    RAOB_MEMBERSHIP_CARD("RAOB Membership Card"),

    /**
     * Sponsorship Form — signed by a Lodge Member vouching for a prospective initiate.
     * Two forms required (from two different RAOB_MEMBER NPCs) to begin initiation.
     * Tooltip: "A Lodge sponsorship form. Reg's signature looks suspiciously legible."
     */
    SPONSORSHIP_FORM("Sponsorship Form"),

    /**
     * Lodge Charter Document — the founding document of Northfield Buffaloes Lodge No. 347.
     * Looted from the LODGE_SAFE_PROP. Used for blackmail of any RAOB_MEMBER NPC.
     * Fence value: 20 COIN.
     * Tooltip: "The Lodge charter. Someone's been claiming expenses for 'ceremonial refreshments' since 1974."
     */
    LODGE_CHARTER_DOCUMENT("Lodge Charter Document"),

    /**
     * Regalia Set — full ceremonial regalia: apron, collar, jewel, and sash.
     * Looted from LODGE_SAFE_PROP or the Regalia Room (PRIMO tier).
     * Wearable as DisguiseSystem tier: −30% COUNCIL_MEMBER suspicion.
     * Fence value: 8 COIN.
     * Tooltip: "Full ceremonial Buffalo regalia. Somehow, no one questions you."
     */
    REGALIA_SET("Regalia Set"),

    /**
     * Ceremonial Mallet — the Lodge's ritual mallet used by the Grand Primo.
     * Looted from the Regalia Room (PRIMO tier). Weapon: 1.5× melee damage multiplier.
     * Fence value: 5 COIN.
     * Tooltip: "Solid oak. Inscribed: 'For Ceremonial Use Only'. Nobody's checking."
     */
    CEREMONIAL_MALLET("Ceremonial Mallet"),

    /**
     * Premium Lager Crate — a case of premium continental lager.
     * Bribe gift for Housing Officer Brian (RAOB_MEMBER) in exchange for a HOUSING_PRIORITY_LETTER.
     * Fence value: 4 COIN.
     * Tooltip: "A crate of something European and expensive. Brian will love this."
     */
    PREMIUM_LAGER_CRATE("Premium Lager Crate"),

    /**
     * Box of Chocolates — a luxury assortment box.
     * Bribe gift for Magistrate Clerk Sandra (RAOB_MEMBER) in exchange for a CASE_DISMISSED_FORM.
     * Fence value: 2 COIN.
     * Tooltip: "Continental selection. Sandra's weakness. Everyone's weakness, really."
     */
    BOX_OF_CHOCOLATES("Box of Chocolates"),

    /**
     * Planning Permission — a forged or expedited planning approval document.
     * Obtained from Planning Inspector Reg (RAOB_MEMBER) via a 10 COIN briefcase bribe.
     * Skips property Notoriety check in PropertySystem.
     * Tooltip: "Signed, stamped, and suspiciously prompt."
     */
    PLANNING_PERMISSION("Planning Permission"),

    /**
     * Case Dismissed Form — an official-looking form cancelling a pending fine.
     * Obtained from Magistrate Clerk Sandra (RAOB_MEMBER) via a BOX_OF_CHOCOLATES bribe.
     * Removes one pending CriminalRecord fine from the player's record.
     * Tooltip: "Case dismissed. No reason given. Best not to ask."
     */
    CASE_DISMISSED_FORM("Case Dismissed Form"),

    /**
     * Racing Tip — an inside tip from a Lodge Member.
     * Obtained from Bookmaker Terry (RAOB_MEMBER) in exchange for evidence of a ≥10 COIN
     * greyhound or horse racing win. Activates BOOKIES_MULTIPLIER flag doubling next bet payout.
     * Tooltip: "Dog four in the 8:30. Terry says it's a cert. Terry says that every time."
     */
    RACING_TIP("Racing Tip"),

    // ── Issue #1349: Northfield RAOB Buffalo Lodge No. 1247 ───────────────────

    /**
     * Buffalo Membership Card — issued on completing the RAOBLodgeSystem initiation.
     * Grants RAOB_MEMBER player flag; required for Lodge entry and secret handshake.
     * Tooltip: "Northfield Buffaloes Lodge No. 1247. It smells of warm lager and ambition."
     */
    BUFFALO_MEMBERSHIP_CARD("Buffalo Membership Card"),

    /**
     * Buffalo Fez — the ceremonial crimson fez worn by Lodge members.
     * DisguiseSystem: blends into Grand Ceremony without triggering LODGE_TRESPASS.
     * Backfires if player Wanted ≥ 2 outside the Lodge (suspicion +1).
     * Tooltip: "A small red fez with a gold tassel. Dignified, in its own way."
     */
    BUFFALO_FEZ("Buffalo Fez"),

    /**
     * Kompromat Ledger — Ron's handwritten ledger of Lodge members' indiscretions.
     * Looted from LODGE_SAFE_PROP during the Lodge Safe Heist.
     * Can be sold to CitizensAdviceSystem or PoliceStationSystem for 30 COIN +
     * Notoriety −5 + newspaper headline.
     * Tooltip: "Decades of secrets. Councillor Walsh features heavily."
     */
    KOMPROMAT_LEDGER("Kompromat Ledger"),

    /**
     * Buffalo Token — ceremonial coin used as Lodge currency and goodwill gifts.
     * Two obtained from the LODGE_SAFE_PROP heist; also given by RAOB_PRIMO_REGENT
     * during the secret handshake (+5 COIN goodwill value).
     * Tooltip: "A brass Buffalo token. Redeemable for one pint of Worthington's."
     */
    BUFFALO_TOKEN("Buffalo Token"),

    /**
     * Ceremonial Cane — Ron's ornate walking cane, symbol of the Primo Regent office.
     * Looted or gifted at PRIMO_REGENT achievement tier. Weapon: 1.3× melee damage.
     * Tooltip: "Ebony shaft, silver buffalo head. Surprisingly heavy."
     */
    CEREMONIAL_CANE("Ceremonial Cane"),

    // ── Issue #1351: Northfield QuickFix Loans ────────────────────────────────

    /**
     * Final Demand Letter — a red-bordered official letter left by Terry (BAILIFF_NPC)
     * on the door of the player's squat when they are absent during a bailiff visit.
     * Three final demand letters trigger PropertySystem eviction.
     * Plays SoundEffect.PAPER_RUSTLE on pickup.
     * Tooltip: "FINAL DEMAND. Pay within 48 hours or face possession proceedings."
     */
    FINAL_DEMAND_LETTER("Final Demand Letter"),

    /**
     * Forged ID — a convincing-but-fake identity document crafted from
     * STOLEN_PHONE + PRINTER_PAPER + INK_BOTTLE via CraftingSystem.
     * Used at QuickFix Loans counter for a no-liability loan (70% pass, 30% catch).
     * Consumed on use (whether caught or not).
     * Tooltip: "It'll probably work. Probably."
     */
    FORGED_ID("Forged ID"),

    // ── Issue #1144: Northfield Probation Office ──────────────────────────────

    /**
     * Sign-On Letter — appointment letter from the Probation Service delivered via
     * PostOfficeSystem. Triggers sign-on obligation at the Probation Office within
     * 3 in-game days. Tooltip: "Please report to the Probation Office within 3 days.
     * Failure to attend will be considered a breach."
     */
    SIGN_ON_LETTER("Sign-On Letter"),

    /**
     * Electronic Tag — ankle-worn GPS tag fitted by Karen (PROBATION_OFFICER).
     * Enforces a 21:00–07:00 curfew: player must be indoors. Outdoor detection triggers
     * WantedSystem +2 and RECALL_TO_CUSTODY. Can be cut with WIRE_CUTTERS.
     * Tooltip: "You're tethered. Metaphorically and literally."
     */
    ELECTRONIC_TAG("Electronic Tag"),

    /**
     * Fake Signal Chip — signal-spoofing device craftable from BROKEN_PHONE + WIRE at a
     * WORKBENCH. Gives a 2-hour window in which the tag reports the player as home even while
     * outdoors. 30% chance Karen notices the discrepancy at next sign-on.
     * Tooltip: "Two hours of freedom. Probably."
     */
    FAKE_SIGNAL_CHIP("Fake Signal Chip"),

    /**
     * Community Service Vest — hi-vis vest issued at sign-on for community service tasks.
     * DisguiseSystem: −15% PCSO/POLICE suspicion, +20% FENCE/DEALER suspicion.
     * Tooltip: "You're giving something back. Whether you want to or not."
     */
    COMMUNITY_SERVICE_VEST("Community Service Vest"),

    /**
     * Case File Document — a client case file stolen from Karen's CASE_FILE_CABINET_PROP.
     * Fenceable at FenceSystem for 5 COIN each, or use for blackmail.
     * CCTV records the theft.
     * Tooltip: "Someone's whole life of bad decisions. In a manila folder."
     */
    CASE_FILE_DOCUMENT("Case File Document"),

    /**
     * Wire Cutters — heavy-duty cutting tool available from ScrapyardSystem.
     * Cuts the ELECTRONIC_TAG (sets ABSCONDED flag, Notoriety +10).
     * Also used for general wire-cutting tasks.
     * Tooltip: "For cutting wire. Or, technically, other things."
     */
    WIRE_CUTTERS("Wire Cutters"),

    /**
     * Tin of Paint — a tin of community service paint used for bench-painting tasks.
     * Press E at a PARK_BENCH while holding this to register 1 community service hour.
     * Tooltip: "British Racing Green. For benches, apparently."
     */
    TIN_OF_PAINT("Tin of Paint"),

    // ── Issue #1146: Mick's MOT & Tyre Centre ────────────────────────────────

    /**
     * Tyre — a car tyre removed during Advisory Repairs or chopped from a scrapped car.
     * Used as a repair component at REPAIR_RAMP_PROP (+15–20 roadworthiness).
     * Fenceable for 3 COIN. Tooltip: "One careful owner. One careless one."
     */
    TYRE("Tyre"),

    /**
     * Car Part — a generic salvage component yielded by the chop-shop in Bay 2.
     * Fenceable at FenceSystem for 8–12 COIN.
     * Tooltip: "Could be anything. Probably wasn't legal to remove it."
     */
    CAR_PART("Car Part"),

    /**
     * Blank Logbook — a clean V5C document sold by Mick at MARCHETTI_CREW Respect ≥ 50.
     * Required for the Car Ringing service (clears stolen flag for 25 COIN + this item).
     * Tooltip: "An empty history. Brand new past. The DVLA would disagree."
     */
    BLANK_LOGBOOK("Blank Logbook"),

    /**
     * MOT Certificate — issued after a Dodgy MOT at MOT_RAMP_PROP.
     * Clears the UNROADWORTHY flag; 25% Terry walk-in detection risk.
     * Adds FRAUDULENT_MOT to CriminalRecord if detected.
     * Tooltip: "Valid for 12 months. Or until the wheels fall off."
     */
    MOT_CERT("MOT Certificate"),

    // ── Issue #1148: Northfield Council Estate Lock-Up Garages ───────────────

    /**
     * Bric-a-Brac — miscellaneous junk cleared from the hoarder's garage (Garage 2).
     * Fenceable at FenceSystem for 2–5 COIN. Tooltip: "Someone's treasure. Mostly junk."
     */
    BRIC_A_BRAC("Bric-a-Brac"),

    /**
     * Vintage Record — rare vinyl found in the hoarder's clearance (Garage 2).
     * Fenceable at FenceSystem for 8–15 COIN. Tooltip: "Still plays if you know someone with a deck."
     */
    VINTAGE_RECORD("Vintage Record"),

    /**
     * Drum Component — a piece of drum kit found in the band rehearsal garage (Garage 1).
     * Used in crafting; Tooltip: "Keeps a beat. Annoys the neighbours."
     */
    DRUM_COMPONENT("Drum Component"),

    /**
     * Cable — audio/speaker cable from the band garage (Garage 1) or DIY garage (Garage 4).
     * Fenceable for 2 COIN. Tooltip: "Metres of it. Tangled, obviously."
     */
    CABLE("Cable"),

    /**
     * Burner Phone — a cheap unregistered mobile from the Marchetti drug den (Garage 3).
     * Triggers police interest if found on player during stop-and-search.
     * Fenceable for 5 COIN. Tooltip: "Pay-as-you-go. No questions asked. One careful owner."
     */
    BURNER_PHONE("Burner Phone"),

    /**
     * Pigeon Feed Bag — a large bag of grain from the pigeon fancier's garage (Garage 8).
     * Can be used to train racing pigeons (PigeonRacingSystem integration).
     * Tooltip: "Thirty kilos of corn. For the birds. Literally."
     */
    PIGEON_FEED_BAG("Pigeon Feed Bag"),

    /**
     * Garage Key 7 — the key to player-rentable Garage 7, obtained from Dave the Caretaker
     * for 5 COIN/week. Grants access and allows use of GARAGE_SHELF_PROP.
     * Tooltip: "It's yours. For now. Keep up the payments."
     */
    GARAGE_KEY_7("Garage Key 7"),

    // ── Issue #1151: Northfield Sporting & Social Club ────────────────────────

    /**
     * Club Membership Card — sold by Ron (SOCIAL_CLUB_STEWARD) for 5 COIN.
     * Grants access to the main bar, members' pricing on drinks, and all activities.
     * Required for darts challenges, quiz entry, and back-room pontoon.
     * Tooltip: "Membership has its privileges. Mostly cheaper beer."
     */
    CLUB_MEMBERSHIP_CARD("Club Membership Card"),

    /**
     * Darts Set — a set of steel-tip darts, awarded when beating Brian in a 501 match.
     * Equipped to hotbar; improves darts accuracy by +1 tier when used at DARTBOARD_PROP.
     * Tooltip: "Tungsten tips. Proper flights. You know what you're doing."
     */
    DARTS_SET("Darts Set"),

    /**
     * Quiz Answer Sheet — Maureen's master answer sheet for Thursday Quiz Night.
     * Can be stolen from NOTICE_BOARD_PROP at 19:00–19:30 (before Maureen collects it).
     * Using it during the quiz guarantees correct answers but has 30% chance of detection.
     * Tooltip: "Capital of Peru? Lima. Everyone always forgets Lima."
     */
    QUIZ_ANSWER_SHEET("Quiz Answer Sheet"),

    // ── Issue #1153: Northfield Community Centre ──────────────────────────────

    /**
     * Aerobics Pass — issued by Denise (COMMUNITY_CENTRE_MANAGER) for 1 COIN.
     * Grants entry to Sandra's aerobics sessions. Expires after one session.
     * Tooltip: "Sandra says bring a towel. She's not joking."
     */
    AEROBICS_PASS("Aerobics Pass"),

    /**
     * CAB Referral Letter — issued by Derek (CAB_VOLUNTEER) at the Tuesday drop-in.
     * Halves the FoodBank waiting time when presented at the food bank counter.
     * Tooltip: "Derek's signed it. That actually means something."
     */
    CAB_REFERRAL_LETTER("CAB Referral Letter"),

    /**
     * Character Reference Letter — issued by Derek (CAB_VOLUNTEER) at the Tuesday drop-in.
     * Reduces MagistratesCourtSystem sentence by one tier when presented in court.
     * Tooltip: "Northfield Community Centre confirms that you have been known to attend things."
     */
    CHARACTER_REFERENCE_LETTER("Character Reference Letter"),

    /**
     * Grant Application Form — stolen from the FILING_CABINET_PROP in the back corridor.
     * Used at the PHOTOCOPIER_PROP to create a FORGED_GRANT_APPLICATION.
     * Can also be legitimately submitted with Denise's help (trust ≥ 30).
     * Tooltip: "Section 4b is surprisingly philosophical."
     */
    GRANT_APPLICATION_FORM("Grant Application Form"),

    /**
     * Forged Grant Application — a doctored copy of the GRANT_APPLICATION_FORM.
     * Posted at POST_BOX_PROP. After 3 in-game days, yields GRANT_CHEQUE (30 COIN).
     * 25% catch rate (×2 at Notoriety Tier 3+); if caught: OBTAINING_MONEY_BY_DECEPTION charge.
     * Tooltip: "Community Pool Table — Equipment &amp; Installation. You've definitely planned this."
     */
    FORGED_GRANT_APPLICATION("Forged Grant Application"),

    /**
     * Grant Cheque — a council cheque for 30 COIN, received after a successful grant.
     * Cashed at the Post Office or bank. Awards GRANT_GRABBER on legitimate use, or
     * triggers prosecution if the forgery is discovered first.
     * Tooltip: "Pay to the bearer: thirty pounds. For a pool table. Community benefit."
     */
    GRANT_CHEQUE("Grant Cheque"),

    /**
     * Counterfeit Flyer — a fake community event flyer used to mislead NPCs.
     * Can be pinned to NOTICE_BOARD_PROP to redirect NPCs away from a heist area.
     * Tooltip: "Free bingo night. Definitely real. Don't tell anyone."
     */
    COUNTERFEIT_FLYER("Counterfeit Flyer"),

    /**
     * Curry and Rice — a portion of community curry served at Saturday night curry night.
     * Costs 2 COIN from CURRY_COOK NPC. Restores +12 Warmth and +15 Hunger.
     * Tooltip: "Best meal in Northfield. Don't argue."
     */
    CURRY_AND_RICE("Curry and Rice"),

    /**
     * Naan — a side of naan bread from the Saturday curry night.
     * Restores +5 Hunger. Free with TINNED_GOODS donation.
     * Tooltip: "Freshly made. You can tell."
     */
    NAAN("Naan"),

    /**
     * Samosa — two vegetable samosas from the community curry night starters.
     * Restores +4 Hunger, +2 Warmth.
     * Tooltip: "Go on. They're only small."
     */
    SAMOSA("Samosa"),

    // ── Issue #1155: Northfield NHS Dentist ──────────────────────────────────

    /**
     * Fizzy Drink — a can of fizzy pop, sold at corner shops and vending machines for 1 COIN.
     * Restores +5 Energy, +5 Hunger. Adds +12 sugar damage to toothache system.
     * Tooltip: "Lovely going down. Your teeth disagree."
     */
    FIZZY_DRINK("Fizzy Drink"),

    /**
     * Haribo — a bag of gummy sweets from the newsagent or pound shop for 1 COIN.
     * Restores +8 Hunger. Adds +15 sugar damage to toothache system.
     * Tooltip: "Kids love them. Dentists love them more."
     */
    HARIBO("Haribo"),

    /**
     * Toothbrush — sold at the pharmacy for 1 COIN.
     * Reduces sugar damage by −25 when used (press E).
     * Tooltip: "Your dentist wants you to use this twice a day. You won't."
     */
    TOOTHBRUSH("Toothbrush"),

    /**
     * Dental Appointment Letter — issued by Deborah after NHS registration.
     * Required to attend an appointment with Dr. Rashid.
     * Tooltip: "Northfield Dental Practice. Please arrive 10 minutes early."
     */
    DENTAL_APPOINTMENT_LETTER("Dental Appointment Letter"),

    /**
     * Waiting List Letter — stolen from the back-office filing cabinet at the dental practice.
     * Can be photocopied at the Community Centre to create a FORGED_WAITING_LIST_LETTER.
     * Tooltip: "Someone's entire place in the queue. In a manila folder."
     */
    WAITING_LIST_LETTER("Waiting List Letter"),

    /**
     * Forged Waiting List Letter — a photocopied WAITING_LIST_LETTER from the Community Centre.
     * Present to Deborah to halve remaining NHS wait time. 25% catch chance at Notoriety ≥ 30.
     * Tooltip: "Looks official. Feels criminal. Welcome to Northfield."
     */
    FORGED_WAITING_LIST_LETTER("Forged Waiting List Letter"),

    // ── Issue #1159: Northfield Angel Nails & Beauty ─────────────────────────

    /**
     * Stolen Jewellery — fenceable item from Trang's cousin drop or other heist loot.
     * Worth 8–15 COIN at FenceSystem; Notoriety +3 if caught with it.
     */
    STOLEN_JEWELLERY("Stolen Jewellery"),

    /**
     * Gel Set — salon supply consumed by a gel manicure service.
     * Trang restocks daily. Yields nothing when broken.
     */
    GEL_SET("Gel Set"),

    /**
     * Counterfeit Perfume — fenceable item dropped via Trang's cousin.
     * Worth 4 COIN at FenceSystem; low risk, no WitnessSystem flag.
     */
    COUNTERFEIT_PERFUME("Counterfeit Perfume"),

    /**
     * Nail Salon Voucher — given by Trang at Street Rep ≥ 50.
     * Redeemable for one free manicure; single use.
     */
    NAIL_SALON_VOUCHER("Nail Salon Voucher"),

    // ── Issue #1157: Northfield Tesco Express ─────────────────────────────────

    /**
     * Tesco Sandwich — meal deal component. Hunger +20.
     * Description: "Egg mayo. The egg is grey." Costs 2 COIN alone.
     */
    TESCO_SANDWICH("Tesco Sandwich"),

    /**
     * Tesco Pasta Pot — alternative sandwich slot in meal deal. Hunger +25.
     * Description: "Room temperature pasta. Technically food."
     */
    TESCO_PASTA_POT("Tesco Pasta Pot"),

    /**
     * Tesco Orange Juice — meal deal drink slot. Thirst −15.
     */
    TESCO_ORANGE_JUICE("Tesco Orange Juice"),

    /**
     * Tesco Meal Deal Bag — completed meal deal containing sandwich + drink + snack.
     * Opens into 3 separate items when consumed.
     */
    TESCO_MEAL_DEAL_BAG("Tesco Meal Deal Bag"),

    /**
     * Clubcard — Tesco loyalty card. Reduces meal deal price to 2 COIN.
     * Grants +1 Clubcard point per COIN spent. Unstackable.
     */
    CLUBCARD("Clubcard"),

    /**
     * Clubcard Voucher — 1 COIN discount on next purchase. Single-use.
     * Obtained when Clubcard points reach 10.
     */
    CLUBCARD_VOUCHER("Clubcard Voucher"),

    /**
     * Tesco Finest Wine — pricey item on top shelf. Cost 4 COIN.
     * Drunkenness +3. Description: "On offer this week. It wasn't."
     */
    TESCO_FINEST_WINE("Tesco Finest Wine"),

    /**
     * Tesco Own Brand Vodka — 2 COIN. Drunkenness +4.
     * HealingSystem: cures minor cuts (antiseptic grade).
     */
    TESCO_OWN_BRAND_VODKA("Tesco Own Brand Vodka"),

    /**
     * Ready Meal — 2 COIN. Requires MICROWAVE to eat (Hunger +30 when cooked, −5 if eaten cold).
     * Description: "For one."
     */
    READY_MEAL("Ready Meal"),

    /**
     * Clubcard Statement — letter in player's squat mailbox: points total, vouchers available.
     * Spawns every 7 in-game days if player has a Clubcard.
     */
    CLUBCARD_STATEMENT("Clubcard Statement"),

    // ── Issue #1161: Northfield Poundstretcher ────────────────────────────────

    /**
     * Own-Brand Crisps — Poundstretcher own-label crisps. 1 COIN.
     * Hunger +10. Tooltip: "Not Walkers. But close enough, isn't it?"
     */
    OWN_BRAND_CRISPS("Own-Brand Crisps"),

    /**
     * Own-Brand Cola — Poundstretcher own-label cola. 1 COIN.
     * Thirst +15. Tooltip: "Tastes like cola. Sort of."
     */
    OWN_BRAND_COLA("Own-Brand Cola"),

    /**
     * Bargain Bucket Crisps — a large bag of mixed-flavour own-brand crisps. 1 COIN.
     * Hunger +20. Tooltip: "An entire bag for a pound. Nobody's winning here."
     */
    BARGAIN_BUCKET_CRISPS("Bargain Bucket Crisps"),

    /**
     * Wholesale Spirits — a case of unlabelled spirit bottles from the delivery pallet.
     * 10% chance to appear in the daily delivery. Fence value: 6 COIN.
     * Tooltip: "No label. No questions. Probably fine."
     */
    WHOLESALE_SPIRITS("Wholesale Spirits"),

    // ── Issue #1165: Northfield Match Day ────────────────────────────────────
    /** Counterfeit match ticket — buy from FOOTBALL_TOUT for 2 COIN, sell to HOME_FAN for 4 COIN.
     * 30% catch chance → Notoriety +4, TOUT_SCAM crime. */
    COUNTERFEIT_TICKET("Counterfeit Ticket"),

    /** Knock-off football scarf — wearable as disguise; while equipped AWAY_FAN NPCs are non-hostile.
     * Craft from 2× FABRIC_SCRAP or buy from FOOTBALL_TOUT for 1 COIN. */
    KNOCKOFF_SCARF("Knockoff Scarf"),

    /** Match-day programme — picked from HOME_FAN/AWAY_FAN pockets; fence value 2 COIN. */
    MATCH_PROGRAMME("Match Programme"),

    /** Fan's wallet — yields 3–8 COIN when looted via pickpocket from a HOME_FAN or AWAY_FAN. */
    WALLET_FAN("Fan's Wallet"),

    /** Fabric scrap — drops from CHARITY_SHOP crates and LAUNDERETTE lost-property.
     * Used to craft KNOCKOFF_SCARF (2× FABRIC_SCRAP = 1 scarf). */
    FABRIC_SCRAP("Fabric Scrap"),

    // ── Issue #1167: Northfield Amateur Boxing Club ───────────────────────

    /** Gym membership card — 5 COIN/week from Tommy. Required after first training session.
     * Allows unlimited bag training (up to 3/day) and entry to Friday Night Fights. */
    GYM_MEMBERSHIP_CARD("Gym Membership Card"),

    /** Fight entry form — signed up Mon–Thu at BOXING_NOTICE_BOARD_PROP for Friday Night Fights.
     * Requires BOXING skill ≥ 3 and a valid GYM_MEMBERSHIP_CARD. */
    FIGHT_ENTRY_FORM("Fight Entry Form"),

    /** Boxing gloves — required for sparring and fight bouts.
     * Purchased from Tommy for 3 COIN or found in the gym locker area. */
    BOXING_GLOVES("Boxing Gloves"),

    /** Loaded glove — BOXING_GLOVES + SCRAP_METAL crafted item.
     * 40% chance of being caught on pat-down → ejection + FIGHT_FIXING crime. */
    LOADED_GLOVE("Loaded Glove"),

    /** Fight purse — cash prize from a bout win. 8 COIN for Friday Night Fights;
     * 30 COIN for the underground white-collar circuit. */
    FIGHT_PURSE("Fight Purse"),

    /** Protein bar — consolation prize from Tommy after a Friday Night Fight loss.
     * Restores 10 health. Tooltip: "It's not about the result, son." */
    PROTEIN_BAR("Protein Bar"),

    /** Speed-bag chalk — consumed during sparring to reduce incoming damage by 1 per piece.
     * Found in gym supply cupboard or bought from Tommy for 1 COIN. */
    SPEED_BAG_CHALK("Speed Bag Chalk"),

    /** ABA Trophy 1987 — the stolen trophy from Tommy's display cabinet.
     * Quest item: retrieve from Derek's house → return to Tommy → LEGACY_OF_THE_RING. */
    ABA_TROPHY("ABA Trophy 1987"),

    // ── Issue #1171: Northfield TV Licence ────────────────────────────────────

    /**
     * TV Licence Letter — buff envelope from the BBC Licensing Authority.
     * Issued every 7 in-game days to unlicensed properties. Status escalates
     * UNLICENSED → WARNED → FINAL_NOTICE → SUMMONED.
     */
    TV_LICENCE_LETTER("TV Licence Letter"),

    /**
     * TV Licence Certificate — proof of payment (12 COIN at LETTERBOX_PROP).
     * Frameable on wall; grants +2 Community Respect. Valid for 28 in-game days.
     */
    TV_LICENCE_CERTIFICATE("TV Licence Certificate"),

    /**
     * Forged TV Licence — craftable at StreetRep ≥ 40 using BLANK_PAPER + PRINTER_INK
     * via PRINTER_PROP. Sellable to PUBLIC/PENSIONER NPCs for 8 COIN each.
     * 20% chance each sale reports it, seeding FORGED_DOCUMENT criminal record
     * and wanted level. Three sales triggers a NewspaperSystem headline.
     */
    FORGED_TV_LICENCE("Forged TV Licence"),

    // ── Issue #1173: Northfield Balti House ──────────────────────────────────

    /**
     * Vegetable Balti — 5 COIN. Mohammed's wife's recipe; cheapest option.
     * Hunger +40, Warmth +15. Clears TOOTHACHE_DEBUFF on consumption.
     */
    VEGETABLE_BALTI("Vegetable Balti"),

    /**
     * Mango Chutney — free with any meal. Tiny plastic pot.
     * No direct effect; accompanies seated meal.
     */
    MANGO_CHUTNEY("Mango Chutney"),

    /**
     * Balti Box — takeaway container; stackable; satisfies HUNGER when consumed.
     * Holds one portion. Carry-out variant of seated meal.
     */
    BALTI_BOX("Balti Box"),

    /**
     * Naan Bag — paper bag of naans; 2 uses. Stolen from kitchen during Naan Heist.
     * Hunger +20 per use. Fence value: 1 COIN each.
     */
    NAAN_BAG("Naan Bag"),

    /**
     * Balti Catering Tin — large stolen catering tin from Mumtaz kitchen.
     * No direct use; fence value 8 COIN at PawnShopSystem.
     */
    BALTI_CATERING_TIN("Balti Catering Tin"),

    /**
     * Fake Curry Powder — craftable from 2 CHALK + 1 TURMERIC.
     * Sell to Mohammed as a "bulk spice deal" for 6 COIN.
     * 30% chance next day: hygiene violation + NewspaperSystem headline.
     */
    FAKE_CURRY_POWDER("Fake Curry Powder"),

    /**
     * Restaurant Receipt — proof of purchase from Mumtaz Baltis.
     * Used as alibi in WitnessSystem. Given on successful payment.
     */
    RESTAURANT_RECEIPT("Restaurant Receipt"),

    // ── Issue #1175: Northfield Argos ────────────────────────────────────────

    /**
     * Argos Slip — numbered collection slip written at the catalogue counter.
     * Consumed when the player (or NPC) collects the item at the counter.
     */
    ARGOS_SLIP("Argos Slip"),

    /**
     * Argos Receipt — proof of purchase issued by the Argos counter.
     * Used for legitimate returns or as the base for FORGED_RECEIPT.
     */
    ARGOS_RECEIPT("Argos Receipt"),

    /**
     * Forged Receipt — a fake Argos receipt craftable from BLANK_RECEIPT + BIRO + STOLEN_PHONE.
     * Used for returns fraud at RETURNS_DESK_PROP; 40% detection chance.
     */
    FORGED_RECEIPT("Forged Receipt"),

    /**
     * Blank Receipt — thermal paper found in bins or lootable from the back office.
     * Required ingredient for crafting a FORGED_RECEIPT.
     */
    BLANK_RECEIPT("Blank Receipt"),

    /**
     * Catalogue Pencil — tiny Argos pencil for writing slips.
     * Can be used as a improvised weapon (1 damage). Value: 0 COIN.
     * Picking one up unlocks the TINY_PENCIL achievement.
     */
    CATALOGUE_PENCIL("Catalogue Pencil"),

    // ── Issue #1181: Northfield Chugger Blitz ─────────────────────────────────

    /**
     * Charity Tabard — bright yellow tabard worn by charity fundraisers.
     * Craftable from FABRIC_SCRAP×1 + MARKER_PEN×1.
     * Equipping redirects CHUGGER NPCs to target other NPCs instead of the player.
     * Equip to collect fake donations (1 COIN each per NPC interaction).
     * Fraud detection triggers on 2nd suspicious NPC contact.
     * Tooltip: "The official disguise of the man who wants your direct debit."
     */
    CHARITY_TABARD("Charity Tabard"),

    /**
     * Charity Clipboard — the fundraiser's essential tool.
     * Looted from CHUGGER NPCs or craftable indirectly via the CHARITY_TABARD bundle.
     * Required to collect fake donations when wearing CHARITY_TABARD.
     * Can be stolen by YOUTH_GANG NPCs (sends chugger into DISTRESSED state).
     * Tooltip: "Contains: a pen, a sign-up form, and broken dreams."
     */
    CHARITY_CLIPBOARD("Charity Clipboard"),

    /**
     * Marker Pen — a thick permanent marker used for signs and crafting.
     * Found in community centres, offices, and charity shops.
     * Used in the CHARITY_TABARD crafting recipe.
     * Tooltip: "Smells funny. Writes well."
     */
    MARKER_PEN("Marker Pen"),

    // ── Issue #1183: Northfield Household Waste Recycling Centre ─────────────

    /**
     * Old Phone — discarded mobile handset looted from the WEEE skip.
     * Can be taken to PhoneRepairSystem (Fix My Phone) for a 2 COIN repair commission.
     * Tooltip: "Still got stuff on it, probably."
     */
    OLD_PHONE("Old Phone"),

    /**
     * Broken Kettle — a defunct electric kettle from the WEEE skip.
     * Comedic item. Crafting ingredient: BROKEN_KETTLE + COPPER_WIRE = IMPROVISED_TASER.
     * Tooltip: "It rattles. Something's definitely loose."
     */
    BROKEN_KETTLE("Broken Kettle"),

    /**
     * Retro Console — a vintage games console looted from the WEEE skip.
     * High-value item. Sellable to FenceSystem for 8 COIN.
     * Achievement TIP_TREASURE triggered on first find.
     * Tooltip: "Someone's mum threw this away."
     */
    RETRO_CONSOLE("Retro Console"),

    /**
     * Curtains — a pair of old curtains from the Reuse Corner.
     * Tooltip: "Brown. Floral. Indestructible."
     */
    CURTAINS("Curtains"),

    /**
     * Board Game — a battered board game from the Reuse Corner.
     * Tooltip: "Probably missing half the pieces."
     */

    /**
     * Broken Lamp — a cracked floor lamp from the Reuse Corner.
     * Tooltip: "Flickering, but technically works."
     */
    BROKEN_LAMP("Broken Lamp"),

    /**
     * Old Bike Wheel — a bicycle wheel with a bent rim from the Reuse Corner.
     * Tooltip: "Oval, really. More of a conceptual wheel."
     */
    OLD_BIKE_WHEEL("Old Bike Wheel"),

    /**
     * Coat Hanger Bundle — a tangled wad of wire coat hangers.
     * From the Reuse Corner. Tooltip: "Reproduces in wardrobe conditions."
     */
    COAT_HANGER_BUNDLE("Coat Hanger Bundle"),

    /**
     * Charity Book — a donated paperback from the Reuse Corner.
     * Tooltip: "Someone's holiday read. Agatha Christie, obviously."
     */
    CHARITY_BOOK("Charity Book"),

    /**
     * Hardcore Permit — official council document permitting use of the hardcore
     * bay at the HWRC. Obtained by queuing at the council office (30 in-game minutes)
     * or forged via COUNCIL_LETTERHEAD + MARKER_PEN. Forged version has 15% detection
     * risk by Dave (FORGED_COUNCIL_DOCUMENT crime, Notoriety +6).
     * Tooltip: "Laminated. Surprisingly official-looking."
     */
    HARDCORE_PERMIT("Hardcore Permit"),

    /**
     * Council Letterhead — official-looking blank council headed paper.
     * Found in council offices or stolen from COUNCIL_OFFICER NPCs.
     * Crafting ingredient: COUNCIL_LETTERHEAD + MARKER_PEN = HARDCORE_PERMIT (forged).
     * Tooltip: "Northfield Metropolitan Borough Council. Est. 1974."
     */
    COUNCIL_LETTERHEAD("Council Letterhead"),

    /**
     * Improvised Taser — a jury-rigged stun device.
     * Crafted from BROKEN_KETTLE + COPPER_WIRE. Single use; stuns target for 3 seconds.
     * Tooltip: "Please don't ask how it works."
     */
    IMPROVISED_TASER("Improvised Taser"),

    /**
     * Builders Rubble — broken bricks, concrete chunks, and plaster debris.
     * Clearly trade waste at the HWRC; triggers Dave's suspicion check.
     * Tooltip: "Heavy, awkward, and definitely trade waste."
     */
    BUILDERS_RUBBLE("Builders Rubble"),

    /**
     * Asbestos Sheet — old corrugated asbestos roofing panel.
     * Clearly trade waste at the HWRC; triggers Dave's suspicion check.
     * Tooltip: "Do not break. Seriously."
     */
    ASBESTOS_SHEET("Asbestos Sheet"),

    /**
     * Glass Bottle — an empty glass bottle from the recycling skip.
     * Deposited or found when searching RECYCLING_SKIP. Stackable 1–3.
     * Tooltip: "Washed out. Probably."
     */
    GLASS_BOTTLE("Glass Bottle"),

    /**
     * Rags — old clothing fragments from the general waste skip.
     * Tooltip: "Soft. Smells of someone else's life."
     */
    RAGS("Rags"),

    /**
     * Charity Bag — an unopened charity donation bag from the general waste skip.
     * May contain random low-value items.
     * Tooltip: "Someone never put it out."
     */
    CHARITY_BAG("Charity Bag"),

    // ── Issue #1186: Northfield Probation Office ──────────────────────────────

    /**
     * Ankle Tag — an electronic monitoring device fitted by the Probation Officer
     * on Enhanced Orders. Appears in the HUD while active.
     * Curfew: 21:00–07:00, must remain within 20 blocks of the player's squat home.
     * Breach (leaving curfew zone): WantedSystem +1 star, CURFEW_BREACH criminal record.
     * Cutting the tag (FenceSystem, rep ≥ 40, 15 COIN): WantedSystem +3 stars, TAG_TAMPER record.
     * Tooltip: "You can hear it beeping if you listen carefully."
     */
    ANKLE_TAG("Ankle Tag"),

    /**
     * Sign-In Form (Prop) — the official sign-in register at the probation office.
     * Used internally to track fortnightly sign-in appointments.
     * The player interacts with the PROBATION_RECEPTIONIST NPC to sign in.
     * Tooltip: "A4, ring-bound, smells of despair and Tipp-Ex."
     */
    SIGN_IN_FORM_PROP("Sign-In Form"),

    // ── Issue #1188: Northfield DWP Home Visit ────────────────────────────

    /**
     * DWP Letter Prop — placed at the squat door when suspicion score reaches 60+.
     * Readable via E: "You are required to attend a compliance interview or be
     * available for a home visit. Failure to cooperate may result in your claim
     * being suspended."
     * HoverTooltip: "A brown envelope. Never good news."
     */
    DWP_LETTER_PROP("DWP Letter (Compliance Notice)"),

    /**
     * Appeal Letter Prop — crafted from NEWSPAPER (paper) + MARKER_PEN.
     * Required to initiate an appeal against a DWP sanction.
     * Press E on the JOB_CENTRE_CLERK while holding this to start the 2-week appeal.
     * Tooltip: "A letter of appeal. Probably won't work. Worth a try."
     */
    APPEAL_LETTER_PROP("DWP Appeal Letter"),

    /**
     * Cash-in-Hand Ledger — a handwritten record of undeclared earnings.
     * Found in skip (SkipDivingSystem, 5% chance) or bought from FenceSystem.
     * If found by Brenda during a home visit evidence search: instant CRIMINAL_REFERRAL.
     * Tooltip: "Don't let Brenda see this."
     */
    CASH_IN_HAND_LEDGER("Cash-in-Hand Ledger"),

    // ── Issue #1190: Information Broker ──────────────────────────────────────

    /**
     * Paper — a blank sheet of writing paper.
     * Crafting component for RUMOUR_NOTE (with PEN).
     * Found at the post office counter or newsagent.
     */
    PAPER("Paper"),

    /**
     * Pen — a biro.
     * Crafting component for RUMOUR_NOTE (with PAPER).
     * Found at the post office counter or newsagent.
     */
    PEN("Pen"),

    // ── Issue #1196: Environmental Health Officer ─────────────────────────────

    /**
     * Hygiene Sticker — peeled from a HYGIENE_RATING_PROP (1 punch) or collected.
     * Base material for forging a FORGED_FIVE_STAR_STICKER at the PHOTOCOPIER_PROP.
     */
    HYGIENE_STICKER("Hygiene Sticker"),

    /**
     * Forged Five-Star Sticker — crafted from HYGIENE_STICKER + BLANK_PAPER at
     * PHOTOCOPIER_PROP. Sell to a venue owner with rating ≤ 3 for 8 COIN.
     * On Janet's follow-up inspection, forgery is detected → FRAUD crime, Wanted +2.
     */
    FORGED_FIVE_STAR_STICKER("Forged Five-Star Sticker"),

    /**
     * Improvement Notice — issued by Janet on a rating ≤ 2 inspection.
     * Fence value 1 COIN; or hand to venue owner for +5 Respect with them.
     */
    IMPROVEMENT_NOTICE("Improvement Notice"),

    /**
     * Anonymous Note — craftable from BLANK_PAPER alone (fold and post).
     * Post to SUGGESTION_BOX_PROP at Council Office to force Janet's next
     * inspection to target a specific venue. One tip-off per in-game week.
     */
    ANONYMOUS_NOTE("Anonymous Note"),

    // ── Issue #1198: Northfield Traffic Warden ────────────────────────────────

    /**
     * Pay-and-Display Ticket — bought from PAY_AND_DISPLAY_MACHINE_PROP (1 COIN/hr, max 4 hrs).
     * Player must press E on their parked car to place it on the dashboard.
     * Valid ticket prevents Clive from issuing a PCN at COUNCIL_CAR_PARK.
     */
    PAY_AND_DISPLAY_TICKET("Pay-and-Display Ticket"),

    /**
     * Penalty Charge Notice — issued by Clive (TRAFFIC_WARDEN) on a parking violation.
     * Fine: 3 COIN if paid within 2 in-game days; 6 COIN standard; 12 COIN if clamped.
     * Can be appealed at APPEAL_DESK_PROP at the COUNCIL_OFFICE.
     * Recorded as PARKING_OFFENCE in CriminalRecord.
     */
    PENALTY_CHARGE_NOTICE("Penalty Charge Notice"),

    /**
     * Forged Parking Ticket — crafted at PHOTOCOPIER_PROP (GRAFTING ≥ Apprentice):
     * PAY_AND_DISPLAY_TICKET + BLANK_PAPER → FORGED_PARKING_TICKET.
     * Place on another car: 60% chance Clive detects forgery (FRAUD + Notoriety +6 + PCN).
     * Sell to COMMUTER or CAFF_REGULAR NPCs for 1 COIN each.
     * 5 sales in one day awards TICKET_TOUT achievement.
     */
    FORGED_PARKING_TICKET("Forged Parking Ticket"),

    /**
     * Wheel Clamp — dropped when the clamp is removed from a clamped car.
     * Fence at scrapyard for 2 COIN (stolen clamp).
     * Applied by Clive on a second-circuit repeat violation.
     */
    WHEEL_CLAMP("Wheel Clamp"),

    /**
     * Clive's Terminal — dropped if Clive is knocked out.
     * Sell at pawn shop for 5 COIN.
     * One-use: interact (E) with a PCN-marked car while holding CLIVE_TERMINAL
     * to void the PCN (80% success chance).
     */
    CLIVE_TERMINAL("Clive's Terminal"),

    // ── Issue #1205: Northfield DVSA Test Centre ──────────────────────────────

    /**
     * Driving Licence — a pink UK driving licence (DVLA format).
     * Awarded on passing the practical test at the DVSA Test Centre, or obtained via
     * the PENDING_TEST_RESULT_ITEM forgery route (30% fraud detection chance per use).
     * Disables the unlicensed-driving penalty in NotorietySystem and unlocks DRIVING
     * rank 4+ cap in StreetSkillSystem. Permanent item; not consumed on use.
     * Tooltip: "Full UK driving licence. Sandra was reluctant but the Highway Code was clear."
     */

    /**
     * Theory Pass Certificate — a laminated DVSA theory pass certificate.
     * Awarded on passing the theory test (≥8/10 questions correct) at the
     * THEORY_TERMINAL_PROP. Required to book a practical test with Sandra.
     * Single-use prerequisite; not consumed but checked on practical test booking.
     * Tooltip: "Passed the theory. The examiner looked almost impressed."
     */
    THEORY_PASS_CERTIFICATE("Theory Pass Certificate"),

    /**
     * Pending Test Result — a manila envelope from the FILING_CABINET_PROP.
     * Contains a forged driving test pass result. Acts as a DRIVING_LICENCE substitute
     * but has a 30% chance each use that Sandra detects it as fraudulent:
     * WantedSystem +2 stars, FRAUD recorded in CriminalRecord.
     * Tooltip: "Someone's pending test result. Probably won't stand up to scrutiny."
     */
    PENDING_TEST_RESULT_ITEM("Pending Test Result"),

    // ── Issue #1209: Citizens Advice Bureau ───────────────────────────────────

    /**
     * BENEFIT_APPEAL_LETTER — an official CAB appeal letter produced by Margaret.
     * Used at the DWPSystem hatch to appeal a benefit sanction (70% base success
     * chance). Also grants +20% appeal success bonus to TrafficWardenSystem.submitAppeal()
     * when held alongside a PENALTY_CHARGE_NOTICE.
     * Tooltip: "A letter from the Citizens Advice. Might actually work."
     */
    BENEFIT_APPEAL_LETTER("Benefit Appeal Letter"),

    /**
     * DEBT_MANAGEMENT_LETTER — Margaret's debt management plan letter.
     * Reduces StreetEconomySystem debt flag. Seeds LOCAL_EVENT rumour.
     * Tooltip: "Managed debt plan. At least someone's keeping track."
     */
    DEBT_MANAGEMENT_LETTER("Debt Management Letter"),

    /**
     * EVICTION_LETTER — an official eviction notice obtained from PropertySystem.
     * Must be presented at the CAB consultation desk to trigger Margaret's
     * eviction-dispute mechanic (extends squat eviction deadline by 7 days).
     * Tooltip: "The landlord's solicitors have been busy."
     */
    EVICTION_LETTER("Eviction Letter"),

    /**
     * BAILIFF_DISPUTE_LETTER — produced by Margaret after a BAILIFF_VISIT
     * consultation. Suspends PropertySystem bailiff debt-recovery for 3 in-game days.
     * Tooltip: "Formally disputes the bailiff's authority. For now."
     */
    BAILIFF_DISPUTE_LETTER("Bailiff Dispute Letter"),

    /**
     * GRIEVANCE_LETTER — employment dispute letter produced by Margaret after an
     * EMPLOYMENT_RIGHTS consultation. Redeems 2 COIN refund from StreetEconomySystem
     * if the player was underpaid on a runner job.
     * Tooltip: "Rights in writing. Might get you your money back."
     */
    GRIEVANCE_LETTER("Grievance Letter"),

    /**
     * FORGED_BENEFIT_LETTER — an illicit forgery produced by Brian.
     * Presented at the DWPSystem hatch to falsely claim a sanction was resolved:
     * yields 5 COIN fraudulent payment. 25% detection chance per use; detected →
     * CrimeType.BENEFIT_FRAUD recorded, WantedSystem +1 star, Brian suspended 7 days.
     * Tooltip: "Looks almost official. Almost."
     */
    FORGED_BENEFIT_LETTER("Forged Benefit Letter"),

    /**
     * FORGED_LANDLORD_REFERENCE — an illicit forgery produced by Brian.
     * Used at PropertySystem.applyForRental() to bypass the credit check and skip
     * the 2-COIN deposit. 15% detection chance per use; detected → immediate eviction,
     * NotorietySystem +5.
     * Tooltip: "Your reference from 'Mr Patel'. He doesn't exist."
     */
    FORGED_LANDLORD_REFERENCE("Forged Landlord Reference"),

    /**
     * FORGED_COURT_SUMMONS — an illicit forgery produced by Brian.
     * Delivered to a PUBLIC NPC to frighten them into NPCState.FLEEING for 60 seconds.
     * No individual detection risk (NPC can't verify), but NeighbourhoodWatchSystem
     * anger +8 if a witness NPC is present.
     * Tooltip: "A fake summons with someone's name on it. Nasty."
     */
    FORGED_COURT_SUMMONS("Forged Court Summons"),

    /**
     * CAB_REFERRAL_FORM — a referral form from the Citizens Advice desk prop.
     * Used in the Eviction Dodger waiting-room side-quest: hand it to the WAITING_CLIENT
     * NPC to complete the quest (reward: 2 COIN).
     * Tooltip: "A CAB referral form. Someone needs this more than you."
     */
    CAB_REFERRAL_FORM("CAB Referral Form"),

    /**
     * DEBT_STATEMENT_ITEM — produced by combining BLANK_PAPER with a DEBT_SPIRAL
     * WAITING_CLIENT NPC's verbal statement in the Debt Chronicler side-quest.
     * Hand it to Margaret to complete the quest (reward: LOCALS Respect +8, rumour seeded).
     * Tooltip: "A written account of someone's debt situation."
     */
    DEBT_STATEMENT_ITEM("Debt Statement"),

    // ── Issue #1216: Northfield Driving Instructor ─────────────────────────────

    /**
     * FORGED_PASS_CERTIFICATE — a forged DVSA pass certificate obtained from the
     * DRIVING_SCHOOL_FILING_CABINET_PROP while Dave is on a lesson. Costs 20 COIN.
     * Accepted at the DVSA Test Centre EXAMINER_DESK_PROP as a substitute for a
     * FULL_DRIVING_LICENCE, but carries a 35% detection risk per use — on detection:
     * CrimeType.DOCUMENT_FRAUD recorded, WantedSystem +1 star.
     * Tooltip: "A very convincing pass certificate. Convincing enough, hopefully."
     */
    FORGED_PASS_CERTIFICATE("Forged Pass Certificate"),

    /**
     * CAR_KEY_COPY — a duplicate ignition key cut for Dave's dual-control Corsa.
     * Allows instant hotwire of DAVE_INSTRUCTOR_CAR_PROP (skips the 10-second
     * SCREWDRIVER mini-game). Obtainable from FenceSystem.
     * Tooltip: "A rough-cut car key. Could be for anything. Probably isn't."
     */
    CAR_KEY_COPY("Car Key Copy"),

    /**
     * FULL_DRIVING_LICENCE — a full UK driving licence awarded after completing
     * 5 lessons with Dave at the DRIVING_SCHOOL. Disables the dual-control speed cap
     * in CarDrivingSystem. Unlocks DRIVING XP tier 4 cap in StreetSkillSystem.
     * Also unlocks the "Delivery Driver" job in DWPSystem (+2 UC/day).
     * Tooltip: "Full UK driving licence. Dave was almost proud."
     */
    FULL_DRIVING_LICENCE("Full Driving Licence"),

    // ── Issue #1218: Northfield Claims Management Company ─────────────────────

    /**
     * NECK_BRACE — a foam cervical collar worn as part of a whiplash claim.
     * Applying this when filing a WHIPLASH_CLAIM or SLIP_AND_FALL multiplies
     * the payout by ×1.5 in ClaimsManagementSystem.
     * Sellable at FenceSystem for 1 COIN.
     * Tooltip: "Officially issued by Gary. Medically dubious."
     */
    NECK_BRACE("Neck Brace"),

    /**
     * CLAIM_REFERENCE_SLIP — a crumpled receipt-style slip issued by Gary when
     * a claim is filed at Compensation Kings. Acts as proof of a pending claim in
     * ClaimsManagementSystem. Worthless if sold to the Fence.
     * Tooltip: "Keep this safe. Or don't. Gary's got a copy."
     */
    CLAIM_REFERENCE_SLIP("Claim Reference Slip"),

    // ── Issue #1227: Wheelwright Motors — Dodgy Car Lot ──────────────────────

    /**
     * MILEAGE_CORRECTOR — a handheld OBD-style device sold by Mo at the Indoor Market
     * (knock-offs, 8 COIN). Used together with a 5-COIN bribe to Bez to clock a ROUGH or
     * BANGER car's odometer up to TIDY condition at Wheelwright Motors.
     * Tooltip: "Mo says it fell off a lorry. You believe him."
     */
    MILEAGE_CORRECTOR("Mileage Corrector"),

    /**
     * FAKE_V5C — a forged vehicle registration document (V5C logbook) printed at
     * Cybernet's back-room PRINTER_PROP (requires BLANK_PAPER + PRINTER_INK, 3 COIN fee).
     * Used to sell a stolen car to Wayne without triggering a police call.
     * Selling with FAKE_V5C records DOCUMENT_FRAUD in CriminalRecord; Notoriety +5.
     * Tooltip: "Looks official. Probably won't stand up in court."
     */
    FAKE_V5C("Fake V5C"),

    /**
     * V5C_PROP — a legitimate vehicle registration document (V5C logbook) that comes with
     * a lawfully acquired car. Prevents HANDLING_STOLEN_GOODS charge when selling a car to
     * Wayne. If the car is stolen, Wayne still asks for it but the V5C clears the transaction.
     * Tooltip: "Keep it in the glovebox. Or don't. Your call."
     */
    V5C_PROP("V5C Document"),

    // ── Issue #1229: Northfield Handy Builders — Trade Counter, Builder's Credit & Copper Pipe Hustle ──

    /**
     * CEMENT_BAG — a 25kg bag of general-purpose cement. Purchased at Handy Builders
     * (3 COIN) or found in the yard skip. Used in MORTAR crafting.
     * Tooltip: "A 25kg bag of general-purpose cement. Your back won't thank you."
     */
    CEMENT_BAG("Cement Bag"),

    /**
     * SAND_BAG — builder's sand in a paper sack. Purchased at Handy Builders (2 COIN)
     * or found in the yard. Used with CEMENT_BAG to make MORTAR.
     * Tooltip: "Builder's sand. Gets everywhere."
     */
    SAND_BAG("Sand Bag"),

    /**
     * WIRE_REEL — a full reel of electrical cable. Purchased at Handy Builders (4 COIN,
     * daily limit 10). Contains copper wire; strips via CraftingSystem into 3 x COPPER_WIRE
     * and 1 x SCRAP_METAL (requires SCREWDRIVER).
     * Tooltip: "A full reel of electrical cable. Contains copper wire."
     */
    WIRE_REEL("Wire Reel"),

    /**
     * COPPER_PIPE — 15mm copper plumbing pipe. Purchased at Handy Builders (5 COIN,
     * daily limit 10). Can be sold at Scrapyard for 4 COIN or stripped via CRUSHER_PROP
     * into COPPER_BALE worth 8 COIN — the core of the Copper Pipe Hustle.
     * Tooltip: "15mm copper plumbing pipe. Hot commodity in certain circles."
     */
    COPPER_PIPE("Copper Pipe"),

    /**
     * MORTAR — mixed mortar ready to apply. Crafted from CEMENT_BAG + SAND_BAG + BUCKET
     * (yields 3 units). Used in SquatFurnishingTracker to plaster BRICK wall faces;
     * increases lodger rent tolerance by +2 COIN/night per face.
     * Tooltip: "Mixed mortar, ready to apply. Give it a stir."
     */
    MORTAR("Mortar"),

    /**
     * TRADE_ACCOUNT_CARD — a legitimate Handy Builders trade account card issued by Terry.
     * Grants a 20-COIN credit line repayable within 3 in-game days.
     * Tooltip: "Your Handy Builders trade account card. Terry's name on the back."
     */
    TRADE_ACCOUNT_CARD("Trade Account Card"),

    /**
     * FAKE_TRADE_ACCOUNT_CARD — a forged Handy Builders trade card printed at Cybernet
     * (BLANK_PAPER + PRINTER_INK). Provides a false-name 20-COIN credit line.
     * Donna runs ID checks on Saturdays (1-in-3 chance); if caught: Notoriety +6,
     * IDENTITY_FRAUD on CriminalRecord, card confiscated.
     * Tooltip: "A convincing-looking Handy Builders trade card. The name on it is almost plausible."
     */
    FAKE_TRADE_ACCOUNT_CARD("Fake Trade Account Card"),

    // ── Issue #1231: Northfield ASBO System ──────────────────────────────────

    /**
     * ASBO_LETTER — an official Anti-Social Behaviour Order notification letter
     * delivered to the player's letterbox by the MagistratesCourtSystem within
     * 1 in-game day of asboPressure reaching 30. The player must open it to
     * acknowledge ASBO proceedings. Ignoring it for 3 in-game days results in
     * the order being granted in absentia.
     * Tooltip: "An official-looking letter. Mum would be devastated."
     */
    ASBO_LETTER("ASBO Letter"),

    /**
     * ASBO_ORDER_DOCUMENT — the formal Anti-Social Behaviour Order document issued
     * by Northfield Magistrates' Court after proceedings complete. Carried in the
     * player's inventory as proof of the active order. Contains listed exclusion zones
     * and the 28-day expiry. Can be shown to Margaret at CitizensAdviceSystem to
     * begin an appeal (35% success rate → ASBO revoked).
     * Tooltip: "The order. Laminated, naturally. Trevor looked very pleased with himself."
     */
    ASBO_ORDER_DOCUMENT("ASBO Order Document"),

    /**
     * Issue #1235: PORK_CHOPS — prize from the Northfield Sporting &amp; Social Club
     * Friday meat raffle. A tray of pork loin chops, still in the butcher's wrap.
     * Worth 3 COIN at the fence. Tooltip: "Winner winner, pork chop dinner."
     */
    PORK_CHOPS("Pork Chops"),

    /**
     * Issue #1235: CHICKEN_LEGS — prize from the Northfield Sporting &amp; Social Club
     * Friday meat raffle. A bag of chicken drumsticks, slightly freezer-burned.
     * Worth 2 COIN at the fence.
     */
    CHICKEN_LEGS("Chicken Legs"),

    /**
     * Issue #1235: SAUSAGES — prize from the Northfield Sporting &amp; Social Club
     * Friday meat raffle. A ring of pork sausages from the local butcher.
     * Worth 2 COIN at the fence.
     */
    SAUSAGES("Sausages"),

    /**
     * Issue #1235: BACK_ROOM_KEY — brass Yale key to the back-room pontoon at the
     * Northfield Sporting &amp; Social Club. Can be fenced for 5 COIN from dodgy contacts.
     * Grants access without requiring Marchetti Crew Respect ≥ 60.
     * Tooltip: "Someone left this behind. Their loss."
     */
    BACK_ROOM_KEY("Back Room Key"),

    /**
     * Issue #1235: INCRIMINATING_DOCUMENT — evidence of Councillor Hicks's corrupt
     * planning application. Sourced from InformationBroker or Scrapyard.
     * Present at the AGM to expose him: Notoriety −15, Hicks flees, newspaper headline.
     * Tooltip: "Council planning application with someone else's signature on it."
     */
    INCRIMINATING_DOCUMENT("Incriminating Document"),

    // ── Issue #1240: Northfield NHS Blood Donation Session ────────────────────

    /**
     * BLOOD_BAG — a sealed NHS blood donation bag.
     * Dropped from BLOOD_FRIDGE_PROP in the mobile unit van. Fence value 10 COIN.
     * Tooltip: "Still warm. The label says 'O+'. Handle with care."
     */
    BLOOD_BAG("Blood Bag"),

    /**
     * DONOR_QUESTIONNAIRE — the pre-donation eligibility form on the clipboard.
     * Pick up from DONOR_QUESTIONNAIRE_PROP. Used at PHOTOCOPIER_PROP to craft
     * FORGED_DONOR_QUESTIONNAIRE. Tooltip: "A checklist of awkward questions."
     */
    DONOR_QUESTIONNAIRE("Donor Questionnaire"),

    /**
     * FORGED_DONOR_QUESTIONNAIRE — a photocopied, altered eligibility form.
     * Crafted from DONOR_QUESTIONNAIRE + BLANK_FORM at PHOTOCOPIER_PROP.
     * Present to Brenda when ineligible: 70% success if Tyler not watching.
     * Tooltip: "All the right boxes are ticked. You ticked them yourself."
     */
    FORGED_DONOR_QUESTIONNAIRE("Forged Donor Questionnaire"),

    /**
     * ORANGE_SQUASH — the complimentary drink given after donation.
     * Heals +5 HP. Awarded automatically after legitimate donation.
     * Tooltip: "Warm, slightly flat. Still appreciated."
     */
    ORANGE_SQUASH("Orange Squash"),

    /**
     * Issue #1311: DIGESTIVE_BISCUIT — an NHS digestive biscuit from the biscuit table.
     * Restores 5% health when consumed. Stacks to 8.
     * Tooltip: "An NHS biscuit. You earned this. Probably."
     */
    DIGESTIVE_BISCUIT("Digestive Biscuit"),

    /**
     * Issue #1311: ORANGE_JUICE — a small cup of orange juice from the biscuit table.
     * Restores 10% health when consumed. Single use.
     * Tooltip: "A small cup. It counts."
     */
    ORANGE_JUICE("Orange Juice"),

    // ── Issue #1243: Northfield Bert's Tyres & MOT ───────────────────────────

    /**
     * MOT_CERTIFICATE — an official DVLA MOT certificate issued by Bert's Tyres &amp; MOT.
     * Valid for 12 in-game days. Bribed certificates are marked invalid after a DVSA raid.
     * Tooltip: "Roadworthy. Allegedly."
     */
    MOT_CERTIFICATE("MOT Certificate"),

    /**
     * FAIL_SHEET — the official DVSA failure notice Bert prints when he fails a car,
     * genuine or fabricated. Lists the invented fault.
     * Tooltip: "A list of problems. Some of them real."
     */
    FAIL_SHEET("MOT Failure Sheet"),

    /**
     * STOLEN_TYRE — a part-worn tyre of dubious provenance. Bert pays 8 COIN each.
     * Can be acquired from TYRE_STACK_PROP at unattended garages.
     * Tooltip: "Part-worn. Partly legal."
     */
    STOLEN_TYRE("Stolen Tyre"),

    /**
     * CATALYTIC_CONVERTER — stripped from a parked car. Fence value: 35 COIN.
     * Very high notoriety if witnessed. Tooltip: "Worth more than the car."
     */
    CATALYTIC_CONVERTER("Catalytic Converter"),

    /**
     * CAR_BATTERY — pulled from a vehicle or bought from PARTS_SHELF_PROP.
     * Fence value: 20 COIN. Also used in crafting recipes.
     * Tooltip: "Still got a bit of charge in it."
     */
    CAR_BATTERY("Car Battery"),

    /**
     * INSPECTION_STICKER — a windscreen sticker showing pass date and expiry.
     * Automatically attached to the player's vehicle on PASS or PASS_BRIBE outcome.
     * Tooltip: "Stick it on the windscreen and hope for the best."
     */
    INSPECTION_STICKER("MOT Inspection Sticker"),

    // ── Issue #1251: Northfield Street Chuggers ───────────────────────────────

    /**
     * CHARITY_WRISTBAND — cosmetic wristband received when donating to a chugger.
     * While worn, lowers Notoriety by 1 (signals prosocial behaviour to NPCs).
     */
    CHARITY_WRISTBAND("Charity Wristband"),

    /**
     * DONOR_LIST — a list of donor details harvested via skimmed sign-ups.
     * Sellable to the FenceSystem for 8 COIN each; max 3 per day.
     * Tooltip: "Someone who deals in mailing lists will pay good money for this."
     */
    DONOR_LIST("Donor List"),

    // ── Issue #1252: Northfield TV Licensing ──────────────────────────────────

    /**
     * TV_ENFORCEMENT_NOTICE — escalated letter left by Derek when the player invokes
     * rights (no warrant) or fails to answer the door. More serious than TV_LICENCE_LETTER.
     * Tooltip: "A formal enforcement notice from TV Licensing. Ignore at your peril."
     */
    TV_ENFORCEMENT_NOTICE("TV Enforcement Notice"),

    /**
     * TV_LICENCE — a genuine BBC TV Licence purchased at the Post Office for 5 COIN.
     * Prevents enforcement letters and Derek's visits. Expires after 52 in-game weeks.
     * Also required as a non-consumed ingredient to craft FAKE_TV_LICENCE.
     * Tooltip: "Congratulations. You are now a law-abiding television viewer."
     */
    TV_LICENCE("TV Licence"),

    /**
     * FAKE_TV_LICENCE — forged TV Licence crafted from TV_LICENCE + PRINTER_PAPER + PRINTER_PROP.
     * Sellable to PUBLIC/PENSIONER NPCs for 3 COIN, or fenced for 2 COIN.
     * Showing to Derek: accepted (50%) if suspicion &lt; 2; always rejected if suspicion ≥ 2.
     * Rejection adds CriminalRecord.FORGED_DOCUMENT and Notoriety increase.
     */
    FAKE_TV_LICENCE("Fake TV Licence"),

    /**
     * PRINTER_PAPER — blank A4 paper used as ingredient for printing fake documents.
     * Available at the internet café or stationers. Required for FAKE_TV_LICENCE recipe.
     */
    PRINTER_PAPER("Printer Paper"),

    // ── Issue #1257: Northfield Rag-and-Bone Man ──────────────────────────────

    /**
     * JUNK_ITEM — generic household junk (old iron, broken kettles, etc.).
     * Sells to Barry for 1 COIN; fence value 0 COIN. Carried by PUBLIC NPCs at RAGBONE_STOPs.
     * Tooltip: "Barry'll give you a penny for that."
     */
    JUNK_ITEM("Junk"),

    /**
     * GARDEN_ORNAMENT (material) — dropped by GARDEN_ORNAMENT prop or taken from NPC bags.
     * Sells to Barry for 2 COIN. No questions asked.
     * Tooltip: "Could've come from anywhere, couldn't it."
     */
    GARDEN_ORNAMENT("Garden Ornament"),

    /**
     * FORGED_LICENCE — a forged BARRY_LICENCE_STATUS document crafted at the InternetCafeSystem
     * for 5 COIN. Prevents COUNCIL_ENFORCEMENT impounding Barry's van for the rest of the day.
     * Tooltip: "Looks official enough. Probably."
     */
    FORGED_LICENCE("Forged Licence"),

    /**
     * RUBBER_TYRE — a replacement tyre for Barry's van after it has been slashed.
     * Acquired from ScrapyardSystem or PropType.SCRAP_PILE. Required for BARRY_S_MATE achievement.
     * Repairs the van when used on RAG_AND_BONE_VAN prop; rewards 10 COIN from Barry.
     * Tooltip: "Just the right size for a flatbed Transit."
     */
    RUBBER_TYRE("Rubber Tyre"),

    /**
     * PENKNIFE — a small folding knife used to slash Barry's tyres (02:00–06:00).
     * Single-use for the sabotage action; otherwise a weak melee weapon (2 damage).
     * Tooltip: "Don't do anything daft with it."
     */
    PENKNIFE("Penknife"),

    // ── Issue #1263: Northfield Illegal Street Racing ─────────────────────────

    /**
     * NITROUS_CANISTER — a small nitrous oxide canister purchased from
     * CarDealershipSystem (8 COIN). Press Space during a race for a 3-second
     * speed burst. Single use per race. Reduces target's speed by 25% if the
     * nitrous line is loosened with a SCREWDRIVER (sabotage hustle).
     * Tooltip: "One shot. Make it count."
     */
    NITROUS_CANISTER("Nitrous Canister"),

    /**
     * RACING_TROPHY — awarded to the 1st-place finisher of a ring road sprint.
     * A cheap plastic trophy engraved 'Northfield Ring Road Champion'.
     * Can be fenced at StreetEconomySystem for 3 COIN, or kept for bragging rights.
     * Tooltip: "You beat a load of boy racers. Outstanding."
     */
    RACING_TROPHY("Racing Trophy"),

    // ── Issue #1265: Northfield Loan Shark — Big Mick's Doorstep Lending ─────

    /**
     * LOAN_AGREEMENT — a crumpled, barely-legible two-page contract issued by
     * Big Mick when the player borrows money. Contains the loan amount, ruinous
     * APR, and a "security clause" written in tiny print.
     * Given to the player on borrow(); surrendered on full repayment.
     * Tooltip: "The small print says he can take your stuff. He wasn't joking."
     */
    LOAN_AGREEMENT("Loan Agreement"),

    /**
     * DEBT_LEDGER — Big Mick's handwritten accounts book, kept in a locked desk
     * at the Loan Shark Office. Lists every outstanding debt with names, amounts,
     * and collection notes. Extremely incriminating.
     * Sellable to the Marchetti Crew contact for 30 COIN (clears player debt).
     * Tooltip: "Names, numbers, the lot. Half the estate is in here."
     */
    DEBT_LEDGER("Debt Ledger"),

    // ── Issue #1267: Northfield Prepayment Meter ───────────────────────────────

    /**
     * METER_KEY — a prepayment electricity key (PAYG top-up token).
     * Blank by default; loaded at CornerShopSystem or NewsagentSystem with
     * 10 or 22 credit units. Insert at ELECTRIC_METER_PROP to top up.
     * Key becomes blank again after insertion (reusable).
     * Tooltip: "A tenner on the leccy. You're living the dream."
     */
    METER_KEY("Meter Key"),

    /**
     * EMERGENCY_CREDIT_NOTICE — a paper slip placed in the player's inventory
     * when the prepayment meter auto-activates 1.5 units of emergency credit
     * between 23:00–06:00. Reminds the player that 1.5 units of debt will be
     * deducted from the next top-up.
     * Tooltip: "Emergency credit. You've got til morning."
     */
    EMERGENCY_CREDIT_NOTICE("Emergency Credit Notice"),

    // ── Issue #1269: Northfield BT Phone Box ──────────────────────────────────

    /**
     * PHONE_BOX_KEY — a BT engineer's master key.
     * Rare drop from UTILITY_WORKER NPC (3% pickpocket chance) or ScrapyardSystem.
     * Lets the player lock the coin slot (no other NPC can use the box for 1 in-game day)
     * or unlock a broken box without SCRAP_METAL. Also reveals the SCRAWLED_NUMBER sticker.
     */
    PHONE_BOX_KEY("BT Engineer Key"),

    /**
     * SCRAWLED_NUMBER — a scrap of paper with a shady contact number.
     * Found inside the phone box on first use (or revealed by PHONE_BOX_KEY).
     * Required to make the Marchetti dead-drop call from the phone box.
     */
    SCRAWLED_NUMBER("Scrawled Number"),

    /**
     * PHONE_CARD — a legacy BT phonecard.
     * Acts as currency for calls from the phone box instead of COIN (card consumed on use).
     * Found in skip diving or charity shop loot pools.
     */
    PHONE_CARD("Phone Card"),

    // ── Issue #1271: Northfield Tattoo Parlour ────────────────────────────────

    /**
     * Biro Pen — a standard ballpoint pen. Used as an ingredient for the Jailhouse
     * Special tattoo option at the tattoo parlour. Also found in stationery loot pools.
     */
    BIRO_PEN("Biro Pen"),

    /**
     * Tattoo Gun Kit — a makeshift tattoo device cobbled together from scrap.
     * Crafted from 3 SCRAP_METAL + 1 BROKEN_PHONE.
     * Allows the player to run unlicensed tattoo sessions at Daz's station
     * during his lunch break (13:00–14:00).
     */
    TATTOO_GUN_KIT("Tattoo Gun Kit"),

    // ── Issue #1273: Northfield Fly-Tipping Ring ──────────────────────────────

    /**
     * GARDEN_WASTE_BAG — a green council sack filled with garden trimmings, leaves,
     * and grass clippings. Collected from garden-rubbish clearance jobs.
     * Loaded into the fly-tip load (max 10 items); pay: 3–6 COIN per job.
     */
    GARDEN_WASTE_BAG("Garden Waste Bag"),

    /**
     * RUBBLE_SACK — a heavy builder's rubble bag, half-full of concrete and plaster.
     * Collected from builder's-rubble clearance jobs at TERRACED_HOUSE build sites.
     * Loaded into the fly-tip load; pay: 8 COIN per job (3 bags per job).
     */
    RUBBLE_SACK("Rubble Sack"),

    /**
     * FIXED_PENALTY_NOTICE — a council Fixed Penalty Notice issued by COUNCIL_VAN_OFFICER
     * Gary after a fly-tipping confrontation. Costs {@code FIXED_PENALTY_COIN} (15 COIN)
     * to pay off via CitizensAdviceSystem. Clears the active fly-tip pile after 60 s
     * and reduces Notoriety by 1.
     */
    FIXED_PENALTY_NOTICE("Fixed Penalty Notice"),

    // ── Issue #1276: Northfield Minicab Office — Big Terry's Cabs ─────────────

    /**
     * TL_COUNCIL_PLATE — a taxi licence council plate. Probably real.
     * Carried by the player to bypass Big Terry's notoriety refusal:
     * "Licensed and everything. Jump in."
     * Also raises the TRAFFIC_WARDEN touting threshold from 5 to 8 unlicensed touts,
     * and suppresses WantedSystem star accumulation from touting.
     */
    TL_COUNCIL_PLATE("Taxi Licence Council Plate. Probably real."),

    // ── Issue #1278: Northfield Travelling Fairground ─────────────────────────

    /**
     * Candy Floss — purchased from CANDY_FLOSS_STALL_PROP for 2 COIN.
     * Satisfies HUNGRY −8. Bulk-bag 5× into FAIRGROUND_CANDYFLOSS_BAG for resale hustle.
     */
    CANDY_FLOSS("Candy Floss"),

    /**
     * Toffee Apple — purchased from CANDY_FLOSS_STALL_PROP for 2 COIN.
     * Satisfies HUNGRY −10.
     */
    TOFFEE_APPLE("Toffee Apple"),

    /**
     * Fairground Prize — won at STRONGMAN_PROP or RING_TOSS_STALL_PROP.
     * Can be fenced for 3–5 COIN. A garish stuffed toy.
     */
    FAIRGROUND_PRIZE("Fairground Prize"),

    /**
     * Fairground Candy Floss Bag — crafted from 5× CANDY_FLOSS.
     * Resale hustle: sell to NPCs near the fairground for 12 COIN (cost 10 = 2 COIN profit).
     */
    FAIRGROUND_CANDYFLOSS_BAG("Bag of Candy Floss"),

    /**
     * Fairground Ticket — purchased from Big Lenny (FAIRGROUND_BOSS) for 1 COIN per ride.
     * Required to access Dodgems, Waltzers, or Strongman. One use.
     */
    FAIRGROUND_TICKET("Fairground Ticket"),

    // ── Issue #1282: Northfield Day & Night Chemist ───────────────────────────

    /**
     * CALPOL — children's paracetamol suspension. Sold OTC.
     * Heals 3 HP. "It's not just for kids."
     */
    CALPOL("Calpol"),

    /**
     * IBUPROFEN — sold OTC at the chemist.
     * Heals 5 HP, reduces PAIN debuff.
     */
    IBUPROFEN("Ibuprofen"),

    /**
     * PLASTERS — sold OTC, 5 HP heal on use.
     * "Box of twenty. Should last you the week. Maybe."
     */
    PLASTERS("Plasters"),

    /**
     * CONDOMS — sold OTC. Causes NPC laugh reaction when seen in inventory.
     * "For medicinal purposes."
     */
    CONDOMS("Condoms"),

    /**
     * NUROFEN_PLUS — contains codeine. Sold OTC but triggers DEPENDENCY debuff
     * after 5 doses in 24 hours.
     * "For the pain. All of it."
     */
    NUROFEN_PLUS("Nurofen Plus"),

    /**
     * FORGED_PRESCRIPTION — a forged prescription slip.
     * 60% chance of passing Janet's check. Fail = PRESCRIPTION_FRAUD crime logged.
     */
    FORGED_PRESCRIPTION("Forged Prescription"),

    /**
     * STOLEN_METHADONE — taken from the methadone fridge or a METHADONE_CLIENT.
     * WantedSystem +3 stars, ROBBERY charge on acquisition.
     * FenceSystem value: 12 COIN.
     */
    STOLEN_METHADONE("Stolen Methadone"),

    /**
     * WHITE_COAT — wearable disguise, reduces suspicion in the chemist by 60%.
     * Found in STAFF_DOOR area or looted from PHARMACIST.
     */
    WHITE_COAT("White Coat"),

    // ── Issue #1301: Northfield Big Issue Vendor ──────────────────────────────

    /**
     * BIG_ISSUE_MAG — a copy of The Big Issue sold by Gary Milligan outside Greggs.
     * Costs 3 COIN from Gary. Reading it (hold E, 3 seconds) seeds a LOCAL_EVENT
     * rumour and grants STREET_SMARTS XP +1. Can be donated to CharityShopSystem.
     */
    BIG_ISSUE_MAG("Big Issue Magazine"),

    /**
     * COUNTERFEIT_BIG_ISSUE — a fake copy printed at InternetCafeSystem for 1 COIN
     * (yields 3 copies). Sells at the same 30% NPC buy rate as the genuine article.
     * 30% per-sale detection risk; 3 detected sales → MARKET_INSPECTOR spawn
     * and MAGAZINE_SCAM rumour seeded.
     */
    COUNTERFEIT_BIG_ISSUE("Counterfeit Big Issue"),

    /**
     * CANVAS_BAG — Gary's worn canvas shoulder bag, containing 3–6 COIN plus
     * 5× BIG_ISSUE_MAG. Can be pickpocketed (55% detection base; −20% with
     * STREET_SMARTS ≥ 5).
     */
    CANVAS_BAG("Canvas Bag"),

    // ── Issue #1303: Northfield Dave's Carpets ────────────────────────────────

    /**
     * CARPET_OFFCUT — a remnant piece of carpet from Dave's stockroom.
     * Purchased for 4 COIN or looted (×2–4) from a CARPET_ROLL_PROP when Kev is distracted.
     * Placed as flooring in the squat grants a warmth bonus (+2 Vibe/block via SquatFurnishingTracker).
     */
    CARPET_OFFCUT("Carpet Offcut"),

    /**
     * SOFA — a bulky second-hand sofa purchased from Dave for 12 COIN.
     * Can be transported to a squat/property using a SACK_TRUCK.
     * Grants +20 comfort (Vibe) via SquatFurnishingTracker on delivery.
     * There is a 45% witness-calls-police risk while transporting it.
     */
    SOFA("Sofa"),

    /**
     * CLOSING_DOWN_FLYER — crafted from NEWSPAPER + MARKER_PEN.
     * Hand to NPCs to refer them to Dave for +1 COIN commission each.
     * Also used as a prop when setting up the player's own fake closing-down pitch.
     */
    CLOSING_DOWN_FLYER("Closing-Down Flyer"),

    /**
     * SACK_TRUCK — a fold-flat trolley purchased for 6 COIN from Dave or looted from
     * the SACK_TRUCK_PROP in the stockroom. Required to transport a SOFA.
     * Looting from stockroom requires Kev to be distracted.
     */
    SACK_TRUCK("Sack Truck"),

    /**
     * CARPET_SAMPLE_BOOK — a large ring-binder of carpet samples taken from the shop floor.
     * Can be sold to FenceSystem for 3 COIN or used as a prop in the player's fake pitch.
     */
    CARPET_SAMPLE_BOOK("Carpet Sample Book"),

    // ── Issue #1306: Northfield Traveller Site ────────────────────────────────

    /**
     * LUCKY_HEATHER — sprig of heather sold by Brigid (TRAVELLER_WOMAN).
     * Costs 2 COIN. Placebo effect: +5% max HP for the current visit.
     * Can be crafted into LUCKY_HEATHER_CROWN (5×).
     */
    LUCKY_HEATHER("Lucky Heather"),

    /**
     * CLOTHES_PEG_BUNDLE — bundle of wooden clothes pegs sold by Brigid for 1 COIN.
     * Comedy throw item: throw at NPCs to trigger a brief STARTLED state and laughter
     * rumour (CLOTHES_PEG_INCIDENT). No damage; harmless but memorable.
     */
    CLOTHES_PEG_BUNDLE("Clothes Peg Bundle"),

    /**
     * TARMAC_MIX — bag of cold-lay tarmac mix purchased from Paddy (5 COIN).
     * Use on player's own property driveway to apply DIY repair: COMFORT_SCORE +5.
     * Required for SMOOTH_DRIVEWAY achievement.
     */
    TARMAC_MIX("Tarmac Mix"),

    /**
     * DOG_FIGHT_LEDGER — evidence notebook found in CARAVAN_PROP during night raid.
     * Contains records of dog fights and bets. Can be reported to RSPCA (triggers
     * RSPCA_OFFICER spawn and dispersal of dog fight ring; Paddy goes HOSTILE,
     * Marchetti Respect −2). Also usable as evidence with POLICE for WantedSystem
     * reduction.
     */
    DOG_FIGHT_LEDGER("Dog Fight Ledger"),

    /**
     * LUCKY_HEATHER_CROWN — wreath crafted from 5× LUCKY_HEATHER.
     * Equippable headgear: +10% max HP, Notoriety −2 (looks harmless/eccentric).
     * Unlocks HEATHER_ROYALTY achievement.
     */
    LUCKY_HEATHER_CROWN("Lucky Heather Crown"),

    /**
     * STOLEN_BIKE — a stolen bicycle recovered from the Northfield area.
     * Accepted by TRAVELLER_BOSS at above-FenceSystem rates (8 COIN).
     * Also accepted by FenceSystem at standard rate (5 COIN).
     */
    STOLEN_BIKE("Stolen Bike"),

    // ── Issue #1315: Prison Van Escape — The Paddy Wagon Hustle ───────────────

    /**
     * VAN_BENCH — ripped-off metal bench from inside the police van.
     * Improvised melee weapon; 3 durability; deals 4 damage per hit. Stacks to 1.
     * Tooltip: "Cold, metal, and surprisingly motivating."
     * Dropped by VAN_BENCH_PROP when destroyed during PrisonVanSystem escape.
     */
    VAN_BENCH("Van Bench"),

    // ── Issue #1317: Northfield Bonfire Night ─────────────────────────────────

    /**
     * ROCKET_FIREWORK — a larger aerial firework purchased from Darren (FIREWORK_DEALER_NPC)
     * behind the off-licence. Produces loud bang and colour burst. Noise level 9.0.
     * Misfire chance: 10% (20% in rain). Triggers FIREWORK_OFFENCE on second police
     * sighting. Cost: 4 COIN.
     */
    ROCKET_FIREWORK("Rocket Firework"),

    /**
     * BANGER_FIREWORK — a small explosive banger. Very loud (noise level 8.0).
     * Misfire chance: 20% (40% in rain). Can be planted in FIREWORK_MORTAR_PROP
     * for catastrophic sabotage (Notoriety +8, CRIMINAL_DAMAGE, FIRE_ENGINE response).
     * Cost: 2 COIN.
     */
    BANGER_FIREWORK("Banger Firework"),

    /**
     * ROMAN_CANDLE — a multi-shot firework that fires coloured stars.
     * Noise level 6.0. Misfire chance: 5% (10% in rain). Produces sustained light show.
     * Cost: 3 COIN.
     */
    ROMAN_CANDLE("Roman Candle"),

    /**
     * OLD_CLOTHES — a bundle of worn clothing; one of the crafting ingredients for
     * the GUY_PROP effigy (NEWSPAPER + OLD_CLOTHES + HAT).
     * Looted from launderette dryers or charity shop rejects.
     * Fence value: 1 COIN.
     */
    OLD_CLOTHES("Old Clothes"),

    // ── Issue #1319: NatWest Cashpoint — The Dodgy ATM ───────────────────────

    /**
     * STOLEN_PIN_NOTE — a scrap of paper jotted with a victim's PIN number,
     * obtained by shoulder-surfing at the CASHPOINT_PROP.
     * Combined with VICTIM_BANK_CARD enables fraudulent withdrawal (30–80 COIN)
     * between 22:00–05:00. Fenceable at 5 COIN.
     * Tooltip: "Four digits. Someone's whole financial life."
     */
    STOLEN_PIN_NOTE("Stolen PIN Note"),

    /**
     * VICTIM_BANK_CARD — the bank card of an NPC who was pickpocketed after
     * successful shoulder-surfing at the cashpoint.
     * Combined with STOLEN_PIN_NOTE enables fraudulent withdrawal (30–80 COIN)
     * between 22:00–05:00. Single-use; destroyed after use.
     * Tooltip: "Not yours. Obviously."
     */
    VICTIM_BANK_CARD("Victim Bank Card"),

    /**
     * CARD_SKIMMER_DEVICE — an illegal card skimmer sold by Kenny (MONEY_MULE)
     * for 25 COIN on Fri/Sat 20:00–23:00.
     * Player attaches it to CASHPOINT_PROP; each visiting NPC has 60% chance to
     * yield CLONED_CARD_DATA. Active 2 in-game hours or until police within 3 blocks.
     * Tooltip: "A neat little device. Criminally neat."
     */
    CARD_SKIMMER_DEVICE("Card Skimmer Device"),

    /**
     * CLONED_CARD_DATA — captured magnetic strip data from an NPC's card,
     * harvested by the CARD_SKIMMER_DEVICE attached to CASHPOINT_PROP.
     * Fenceable at 15 COIN each.
     * Tooltip: "Data. Lots of it. None of it yours."
     */
    CLONED_CARD_DATA("Cloned Card Data"),

    /**
     * STUFFED_ENVELOPE — a thick envelope of cash given by Kenny (MONEY_MULE)
     * for a money-mule run. Carry it 30 blocks south within 3 minutes for 15 COIN.
     * Police stop while carrying triggers MONEY_LAUNDERING on CriminalRecord.
     * Tooltip: "Heavy for an envelope. Don't open it."
     */
    STUFFED_ENVELOPE("Stuffed Envelope"),

    /**
     * ENGINEER_ACCESS_CARD — an engineer's access card found inside a cracked-open
     * CASHPOINT_PROP (out-of-service machine). Fenceable at 20 COIN.
     * Tooltip: "Someone important lost this. Lucky you found it."
     */
    ENGINEER_ACCESS_CARD("Engineer Access Card"),

    // ── Issue #1333: Northfield Employment System ─────────────────────────────

    /**
     * JOB_APPLICATION_FORM — collected from JOB_VACANCY_BOARD_PROP outside each
     * employer. Must be presented to the interview NPC during business hours to
     * begin the 3-question interview mini-game.
     * Tooltip: "Fill it in. Don't mention the convictions."
     */
    JOB_APPLICATION_FORM("Job Application Form"),

    /**
     * STAFF_ID_BADGE — issued on hire. Player must wear it (carry in inventory)
     * to clock in to shifts at STAFF_CLOCK_IN_PROP. Removed on dismissal or
     * voluntary resignation. Cannot be sold or fenced.
     * Tooltip: "You're one of us now. Don't embarrass the brand."
     */
    STAFF_ID_BADGE("Staff ID Badge"),

    /**
     * FORGED_REFERENCE — craftable at LibrarySystem (BLANK_PAPER ×1 +
     * PHOTOCOPIER_INK_CARTRIDGE ×1, FORGERY ≥ Apprentice). Presented during
     * interview to add +2 to outcome score. Detection risk: 30% base (−10% per
     * FORGERY level above Apprentice). If detected: FRAUD CriminalRecord entry,
     * Notoriety +5, permanent employer blacklist.
     * Tooltip: "Glowing references from people who definitely exist."
     */
    FORGED_REFERENCE("Forged Reference"),

    // ── Issue #1335: Northfield Cycle Centre — Dave's Bikes ───────────────────

    /**
     * BIKE_REPAIR_KIT — a compact kit containing tyre levers, patches, and a
     * small pump. Purchased from Dave's Cycle Centre or crafted (RUBBER_PATCH +
     * SCRAP_METAL). Used to fix punctures while riding; removes the PUNCTURE
     * debuff in 5 seconds. Tooltip: "Every cyclist's best mate."
     */
    BIKE_REPAIR_KIT("Bike Repair Kit"),

    /**
     * BIKE_LOCK — a D-lock or chain lock used to secure a bike to a
     * BIKE_RACK_PROP or lamp post. Three tiers: basic (3 hits to cut),
     * standard (5 hits), heavy-duty (8 hits). Purchased from Dave's Cycle Centre.
     * Tooltip: "Only as secure as the post it's locked to."
     */
    BIKE_LOCK("Bike Lock"),

    /**
     * BIKE_LIGHT_FRONT — a white LED front light that clips to the handlebars.
     * Required after dark to avoid a CYCLING_OFFENCE stop by PCSO.
     * Purchased from Dave's Cycle Centre. Battery lasts 3 in-game nights.
     * Tooltip: "See and be seen. Or don't. Up to you."
     */
    BIKE_LIGHT_FRONT("Bike Light (Front)"),

    /**
     * BIKE_LIGHT_REAR — a red flashing rear light. Required after dark alongside
     * BIKE_LIGHT_FRONT to avoid a CYCLING_OFFENCE stop. Purchased from Dave's
     * Cycle Centre. Tooltip: "The blink of 'don't kill me'."
     */
    BIKE_LIGHT_REAR("Bike Light (Rear)"),

    /**
     * BIKE_HELMET — a foam-and-plastic cycling helmet. Not legally required but
     * reduces collision damage by 50% when mounted. Triggers CYCLE_TO_WORK
     * achievement when worn to an employer. Tooltip: "Stylist? No. Alive? Hopefully."
     */
    BIKE_HELMET("Bike Helmet"),

    /**
     * DELIVERY_BAG — a large insulated courier bag in JustEat orange.
     * Required to accept delivery missions from JUST_EAT_DELIVERY_BOARD_PROP.
     * Holds up to 4 food orders simultaneously. Purchased from Dave's Cycle Centre.
     * Tooltip: "The unmistakable bag of the gig economy."
     */
    DELIVERY_BAG("Delivery Bag"),

    /**
     * COLD_DELIVERY_BAG — a delivery bag where the insulation has failed.
     * Orders delivered in this bag count as cold (reduced payout: 2 COIN instead
     * of 4 COIN). Crafted by using DELIVERY_BAG 20+ times without repair.
     * Tooltip: "It's cold. The food is cold. Everything is cold."
     */
    COLD_DELIVERY_BAG("Cold Delivery Bag"),

    // ── Issue #1337: Northfield Police Station — The Nick ─────────────────────

    /**
     * POLICE_KEY_CARD — a laminated access card belonging to a DESK_SERGEANT or
     * CUSTODY_SERGEANT. Required for the key-card route into the evidence locker.
     * Obtained by pickpocketing (STEALTH Expert+) or purchasing from the Fence
     * (30 COIN, Marchetti Respect ≥ 40). Single-use; destroyed on entry.
     * Tooltip: "B Division, Northfield. Keep on person at all times."
     */
    POLICE_KEY_CARD("Police Key Card"),

    /**
     * ROPE_AND_HOOK — a grappling rope used for the back-window route into the
     * evidence locker. Crafted from ROPE_LADDER + SCRAP_METAL (3 hits at
     * WORKBENCH). Allows the player to enter the evidence locker via the small
     * first-floor window on the rear of the station. Single-use.
     * Tooltip: "Hooks on anything. Whether you should is another matter."
     */
    ROPE_AND_HOOK("Rope and Hook"),

    /**
     * DRIVING_LICENCE — a photocard driving licence. Required (with 20 COIN) to
     * reclaim an impounded vehicle from the police garage during opening hours
     * (08:00–18:00). Also accepted by CarDrivingSystem as proof of entitlement.
     * Obtainable from the DVLA office (by quest) or forged at the PHOTOCOPIER_PROP
     * (GRAFTING ≥ Apprentice; 15% detection chance at the enquiry counter).
     * Tooltip: "Full UK licence. Photo may or may not resemble you."
     */
    DRIVING_LICENCE("Driving Licence"),

    // ── Issue #1341: Northfield Residents' Association Meeting ─────────────────

    /**
     * NOISE_ABATEMENT_LETTER — an official letter from the council confirming that
     * a noise abatement order has been issued against the player's property.
     * Used in ResidentsAssociationSystem to dismiss a noise complaint from the
     * meeting agenda (E on Kevin with this in inventory counts as official proof).
     * Obtainable from EnvironmentalHealthSystem after 3+ NOISE_COMPLAINT CriminalRecord
     * entries, or crafted using COUNCIL_LETTERHEAD + MARKER_PEN (forgery).
     * Tooltip: "If you're this loud, it's getting reported."
     */
    NOISE_ABATEMENT_LETTER("Noise Abatement Letter"),

    /**
     * PETTY_CASH_TIN — Pauline's lockable tin containing the Residents' Association's
     * petty cash float (20 COIN). Can be pickpocketed from Pauline during the meeting
     * (STEALTH vs Pauline's OBSERVANT) for 20 COIN + Notoriety +5, or redirected to
     * the player via a legitimate budget motion. Tooltip: "Association funds. Hands off."
     */
    PETTY_CASH_TIN("Petty Cash Tin"),

    /**
     * MYSTERY_HAMPER — the monthly raffle's first prize: a wicker hamper of
     * assorted local produce. Won by the player at 70% chance if a RIGGED_BARREL
     * is used; otherwise 25% base chance. Worth 8 COIN at the pawn shop.
     * Awards COMMUNITY_SCROUNGER achievement on first win.
     * Tooltip: "Contains several things from the Co-op and one mystery item."
     */
    MYSTERY_HAMPER("Mystery Hamper"),

    /**
     * GARDEN_VOUCHER — the monthly raffle's second prize: a 5 COIN voucher for
     * Northfield Garden Centre. Won by non-first-place raffle draw.
     * Worth face value (5 COIN) at the garden centre.
     * Tooltip: "Valid for one calendar year. No cash alternative."
     */
    GARDEN_VOUCHER("Garden Centre Voucher"),

    /**
     * RIGGED_BARREL — a specially weighted raffle barrel drum that tips the odds
     * in the player's favour (70% win chance vs 25% base) when substituted for
     * the meeting's standard barrel. Must be placed unwitnessed (STEALTH ≥ 2)
     * before the raffle begins (Agenda slot 4). Awards RIGGED_RAFFLE achievement.
     * Tooltip: "Weighted for results. Very community-minded."
     */
    RIGGED_BARREL("Rigged Raffle Barrel"),

    // ── Issue #1343: Northfield Christmas Lights Switch-On ────────────────────

    /**
     * MULLED_WINE — a warm spiced red wine served at the Christmas lights event
     * stalls. Consuming it restores +25 Warmth. Purchasable for 2 COIN from any
     * MULLED_WINE_STALL_PROP, or taken free from the unattended stall after 18:00.
     * Tooltip: "Hot, sweet, and dangerously festive."
     */
    MULLED_WINE("Mulled Wine"),

    /**
     * WAYNE_STUBBS_AUTOGRAPH — a signed selfie from CELEBRITY_NPC Wayne Stubbs,
     * ex-Big Brother contestant. Given freely by Wayne when the player interacts
     * with him (if Notoriety &lt; 3). Fenceable for 3 COIN.
     * Tooltip: "Series 7. He came eighth."
     */
    WAYNE_STUBBS_AUTOGRAPH("Wayne Stubbs Autograph"),

    /**
     * CELEBRITY_WALLET — Wayne Stubbs's wallet, containing 8–12 COIN. Obtained
     * by successfully pickpocketing the CELEBRITY_NPC (STEALTH ≥ 2 required).
     * Awards CELEBRITY_MUGGER achievement.
     * Tooltip: "Full of crisp twenties and a Greggs loyalty card."
     */
    CELEBRITY_WALLET("Celebrity Wallet"),

    /**
     * FREEBIE_WRISTBAND — a cloth wristband handed out by event staff at the
     * Christmas lights switch-on. Gives the player free access to the mulled wine
     * stall once. Dropped by SECURITY_GUARD NPCs on defeat.
     * Tooltip: "VIP access. Mostly meaningless."
     */
    FREEBIE_WRISTBAND("Freebie Wristband"),

    /**
     * CHRISTMAS_LIGHTS_BULB — a replacement festive bulb salvaged from the
     * SWITCH_BOX_PROP or dropped when the lights fail. Used as craft ingredient
     * or fenceable for 1 COIN each.
     * Tooltip: "Needs a firm hand and the right fuse."
     */
    CHRISTMAS_LIGHTS_BULB("Christmas Lights Bulb"),

    // ── Issue #1353: Northfield Amateur Dramatics Society ─────────────────────

    /**
     * STAGE_COSTUME — a theatrical costume from the NAODS production of Blood Brothers.
     * Gives −2 police suspicion while worn (costume = 'not a criminal'). Flips to +1
     * suspicion if player is Wanted ≥ 2. Non-cast wearers are spotted by NAODS_MEMBER
     * NPCs with a 40% chance, triggering a confrontation. 3–5 costumes are stored in the
     * lockpickable COSTUME_CUPBOARD_PROP. Sellable at charity shop for 3 COIN or via
     * fence for 8 COIN.
     * Tooltip: "You could be anyone in this. That's rather the point."
     */
    STAGE_COSTUME("Stage Costume"),

    /**
     * REHEARSAL_SCHEDULE — a printed rehearsal timetable issued to cast members by
     * Patricia (DRAMA_DIRECTOR). Sets the NAODS_MEMBER flag on the player and confirms
     * rehearsal nights (Wed/Thu 19:00–22:00). Leaving a rehearsal early removes the
     * player from the cast.
     * Tooltip: "Wednesday and Thursday. Don't be late."
     */
    REHEARSAL_SCHEDULE("Rehearsal Schedule"),

    /**
     * FORGED_TICKET — a forged NAODS opening night ticket craftable from
     * PRINTER_PAPER + INK_BOTTLE. Admits entry to the production at a saving of 2 COIN.
     * 30% chance of being caught at the TICKET_BOOTH_PROP, recording TICKET_FRAUD.
     * Tooltip: "Printed on a home printer. It'll probably work."
     */
    FORGED_TICKET("Forged Ticket"),

    /**
     * PROP_GUN — a realistic-looking stage prop revolver used in the Blood Brothers
     * production. Can be swapped with AIRGUN as part of Mario's sabotage plan, causing
     * chaos on stage. Stored in the PROP_GUN_PROP cabinet.
     * Tooltip: "It's not real. Probably best not to wave it around, though."
     */
    PROP_GUN("Prop Gun"),

    // ── Issue #1357: Northfield Charity Fun Run ────────────────────────────────

    /**
     * RACE_NUMBER_BIB — an official race number bib issued by Janet the FUN_RUN_MARSHAL
     * on registration (2 COIN entry fee). Required to participate in the charity fun run.
     * Tooltip: "Your official race number. Pin it on and try not to embarrass yourself."
     */
    RACE_NUMBER_BIB("Race Number Bib"),

    /**
     * SPONSOR_SHEET — a clipboard sponsor sheet issued with the race number bib.
     * Player can solicit pledges from NPCs before the race (PENSIONER: 80% chance,
     * CHUGGER: refuses). Presenting post-finish pays up to 20 COIN.
     * Fraud path: presenting without finishing caught if Notoriety ≥ 5.
     * Tooltip: "Get as many signatures as you can before the race."
     */
    SPONSOR_SHEET("Sponsor Sheet"),

    /**
     * WINNERS_MEDAL — awarded to the player for finishing the fun run in under
     * 25 in-game minutes. A cheap gold-coloured medal on a ribbon.
     * Tooltip: "First place. You may feel smug about this."
     */
    WINNERS_MEDAL("Winner's Medal"),

    /**
     * WATER_CUP — a paper cup of water from a fun run water station.
     * Consuming it restores +5 Hunger. Can also be tipped over for chaos.
     * Tooltip: "A paper cup of lukewarm water. Essential fuel."
     */
    WATER_CUP("Water Cup"),

    // ── Issue #1359: Northfield HMRC Tax Investigation ────────────────────────

    /**
     * TAX_DEMAND_LETTER — an official HMRC demand notice served by Sandra Watts
     * (HMRC_INSPECTOR). Amount is 30% of untaxed earnings, capped at 80 COIN.
     * Player must pay within 2 in-game days or face distraint (bailiff Derek).
     * Can be challenged via CitizensAdvice for 40% reduction.
     * Tooltip: "HMRC demands payment. Ignoring this will not make it go away."
     */
    TAX_DEMAND_LETTER("Tax Demand Letter"),

    /**
     * CLEAN_BILL_OF_HEALTH — an HMRC clearance letter issued after the player
     * pays their full tax demand. Grants 7 in-game days of immunity from Sandra.
     * Tooltip: "All clear from HMRC. For now."
     */
    CLEAN_BILL_OF_HEALTH("Clean Bill of Health"),

    /**
     * CASH_BRIBE_ENVELOPE — a brown envelope stuffed with coin used to bribe
     * Sandra (HMRC_INSPECTOR). 60% success chance; on success seeds BENT_OFFICIAL
     * rumour. On failure logs BRIBERY_OF_PUBLIC_OFFICIAL crime and adds Wanted +2.
     * Crafted from COIN (10) + BROWN_ENVELOPE (already in inventory if available).
     * Tooltip: "A discreet envelope. Best offered quietly."
     */
    CASH_BRIBE_ENVELOPE("Cash Bribe Envelope"),

    // ── Issue #1361: Northfield St. Margaret's Church Hall Jumble Sale ─────────

    /**
     * JAM_JAR — Margaret's homemade jam sold at St. Margaret's jumble sale till.
     * Costs 2 COIN. Consuming restores 20 hunger. Can be thrown at a PENSIONER
     * crowd as a distraction, causing STARTLED state and a 15-second unattended
     * stall window. Stealing without paying: PETTY_THEFT + Margaret HOSTILE.
     * Tooltip: "Homemade jam. Goes well on toast. Or as a projectile."
     */
    JAM_JAR("Homemade Jam"),

    /**
     * MYSTERY_BOX — sealed auction container used in the noon Mystery Box Auction
     * at St. Margaret's. Three spawn at 11:00 near Reverend Dave. Contains random
     * loot: junk / useful / SCORE tier (DIAMOND, WAR_MEDAL, STOLEN_PHONE).
     * Cannot be sold before opening; opens on auction win.
     */
    MYSTERY_BOX("Mystery Box"),

    /**
     * BAIT_ITEM — planted inside a MYSTERY_BOX_PROP before 11:00 using a STEALTH
     * approach (requires Notoriety < 30) to inflate NPC bids on that box by ×1.5,
     * driving it out of NPC budget and letting the player win the other cheaply.
     * Tooltip: "Looks tempting. That's the point."
     */
    BAIT_ITEM("Planted Bait"),

    /**
     * VALUABLE_DONATION — high-value item skimmed from donation bags during the
     * volunteer sort shift (08:00–09:00). Worth 3× normal fence value. If Reverend
     * Dave catches the player pocketing one (15% risk): CAUGHT_NICKING_DONATIONS
     * crime + Notoriety +8 + 7-day volunteer ban.
     */
    VALUABLE_DONATION("Valuable Donation"),

    /**
     * JUMBLE_FIND — random low-value item yielded from sorting donation bags during
     * the volunteer shift. 40% chance per sort action. No crime risk; honest perk
     * of volunteering.
     */
    JUMBLE_FIND("Jumble Find"),

    // ── Issue #1363: Northfield Sunday Car Boot Sale ───────────────────────────

    /**
     * VHS_TAPE — chunky black videocassette sold by BOOT_SALE_VENDOR at car boot
     * sale. Base value 1 COIN. Can be traded at CharityShopSystem or PawnShopSystem.
     * 5% gem-find bonus applies if BETAMAX_PLAYER is in inventory (curiosity premium).
     * Tooltip: "VHS tape. Someone taped over Coronation Street with this."
     */
    VHS_TAPE("VHS Tape"),

    /**
     * CROCKERY_SET — mismatched plates and cups bundled in a carrier bag.
     * Sold by BOOT_SALE_VENDOR. Base value 2 COIN. Breaks if dropped (fragile).
     * Tooltip: "Six plates, four cups. None of them match."
     */
    CROCKERY_SET("Crockery Set"),

    /**
     * BOARD_GAME — battered board game with missing pieces. Sold by BOOT_SALE_VENDOR.
     * Base value 1 COIN. Can be gifted to PENSIONER NPCs for +2 Neighbourhood Vibes.
     * Tooltip: "Cluedo. One of the murder weapons is missing. Suspicious."
     */
    BOARD_GAME("Board Game"),

    /**
     * BETAMAX_PLAYER — obsolete video cassette player found in car boot stock.
     * Sold by BOOT_SALE_VENDOR. Base value 3 COIN. 5% gem-find chance on purchase
     * (player finds hidden cash inside unit). Tooltip: "Betamax. Ahead of its time.
     * Behind everything else."
     */
    BETAMAX_PLAYER("Betamax Player"),

    /**
     * FONDUE_SET — 1970s cheese fondue set complete with long forks. Sold by
     * BOOT_SALE_VENDOR. Base value 2 COIN. 5% gem-find chance (a COIN wedged
     * in the base). Can be used as a melee weapon (+1 attack with cheese fork).
     * Tooltip: "Retro dinner party kit. The forks are suspiciously sharp."
     */
    FONDUE_SET("Fondue Set"),

    /**
     * GARDEN_GNOME — ceramic garden gnome with chipped hat. Sold by BOOT_SALE_VENDOR.
     * Base value 2 COIN. Can be placed as a PROP in the world (decoration).
     * Throwing it deals 4 damage. Tooltip: "He's seen things you wouldn't believe."
     */
    GARDEN_GNOME("Garden Gnome"),

    /**
     * CASSETTE_PLAYER — portable cassette player with foam headphones. Sold by
     * BOOT_SALE_VENDOR. Base value 2 COIN. Equipping it gives player a subtle
     * speed +5% (motivational music effect). Tooltip: "Press play. It still works."
     */
    CASSETTE_PLAYER("Cassette Player"),

    // ── Issue #1367: Northfield Speed Awareness Course ────────────────────────

    /** Section 172 Notice of Intended Prosecution from a speed camera or Clive.
     * Issued to inventory when the player drives above SPEED_LIMIT_THRESHOLD.
     * Player has 2 in-game days to pay fine, book course, or face COURT_SUMMONS. */
    SPEEDING_NOTICE("Speeding Notice"),

    // ── Issue #1369: Northfield New Year's Eve ────────────────────────────────

    /** Small purse dropped by Sharon (DRUNK NPC) near the park entrance on NYE.
     * Contains 4–8 COIN. Returning to Sharon: Respect +3, HONEST_FINDER achievement.
     * Keeping it: THEFT_FROM_PERSON CriminalRecord, Notoriety +4. */
    PURSE("Purse"),

    // ── Issue #1371: Northfield Christmas Market ──────────────────────────────

    /** Grilled bratwurst sausage sold by Dietmar at the Christmas Market.
     * Consuming heals +10 HP. Available 10:00–20:00 on market days (335–356). */
    BRATWURST("Bratwurst"),

    /** Small festive trinket (novelty decoration, snow globe, etc.) sold by Linda.
     * Giving one to a PENSIONER NPC: Community Respect +2. */
    CHRISTMAS_TRINKET("Christmas Trinket"),

    /** Traditional candy cane; sold at various stalls and in Santa's Grotto.
     * Small sugar rush (+2 HP, minor energy boost). */
    CANDY_CANE("Candy Cane"),

    /** Numbered paper raffle ticket sold by Margaret at the charity stall.
     * Resolved at 19:30; 10% chance of winning a CHRISTMAS_HAMPER. */
    RAFFLE_TICKET("Raffle Ticket"),

    /** Wicker hamper filled with festive goods — the raffle jackpot prize.
     * High barter value; sellable to FENCE for 12 COIN. */
    CHRISTMAS_HAMPER("Christmas Hamper"),

    /** Counterfeit designer scarf sold by Colin (DODGY_TRADER).
     * Triggers Trading Standards mechanic when in inventory during Janet's inspection. */
    FAKE_DESIGNER_SCARF("Fake Designer Scarf"),

    /** Clip-on "I'M SANTA" badge pickpocketed from Terry in costume.
     * Unlocks MUGGED_FATHER_CHRISTMAS achievement. Tradeable novelty item. */
    SANTA_BADGE("Santa Badge"),

    // ── Issue #1373: Northfield Local Council Elections ───────────────────────

    /** Campaign rosette worn by canvassers; equipping it near POLLING_STATION_PROP
     * enables heckling mechanics on Polling Day. */
    ROSETTE_ITEM("Campaign Rosette"),

    /** Bundle of postal ballots intercepted from POSTMAN_NPC (08:00–11:00, days 83–89).
     * Fill in 1–5 ballots at home for +5 votes per ballot to chosen candidate. */
    POSTAL_VOTE_BUNDLE("Postal Vote Bundle"),

    /** Single campaign leaflet dropped from LEAFLET_PILE_PROP or volunteered door-to-door.
     * Sabotage rival piles using PERMANENT_MARKER. */
    CAMPAIGN_LEAFLET("Campaign Leaflet"),

    /** Stolen tally sheet from Count Night (22:30, day 90). Can be fenced for 8 COIN. */
    COUNT_SHEET("Count Sheet"),

    /** Novelty souvenir mug bearing a candidate's face. Tradeable keepsake. */
    CANDIDATE_MUG("Candidate Mug"),

    /** Broad-tipped permanent marker used to deface rival LEAFLET_PILE_PROP. */
    PERMANENT_MARKER("Permanent Marker"),

    // ── Issue #1381: Northfield Bank Holiday Street Party ─────────────────────

    /** Warm flat lager from Brenda's coolbox. +10 hunger, +5 energy, −5 inhibition.
     * Lowers assault detection threshold by 1 tier for 5 in-game minutes.
     * Tooltip: "It's flat. It's warm. It's British." */
    WARM_LAGER("Warm Lager"),

    /** Crisp packet from the trestle table — empty after eating.
     * 20% food-poisoning chance. Tooltip: "Ready Salted. The safe choice." */
    CRISP_PACKET("Crisp Packet"),

    /** Raw sausage for the disposable BBQ; cookable for COOKED_SAUSAGE.
     * Tooltip: "Pink in the middle is fine. Probably." */
    RAW_SAUSAGE("Raw Sausage"),

    /** Cooked sausage from the BBQ. +25 hunger.
     * Tooltip: "Slightly charred. The British standard." */
    COOKED_SAUSAGE("Cooked Sausage"),

    /** Raffle prize — holiday food selection box. +30 hunger (eat individually). */
    SELECTION_BOX("Selection Box"),

    /** Raffle prize — novelty towel; wearable disguise modifier (−2 recognition for 1 in-game day). */
    NOVELTY_TOWEL("Novelty Towel"),

    /** Raffle prize — bottle of Lidl Merlot. Drinkable; fenceable for 3 COIN.
     * Tooltip: "Lidl Merlot. It's a special occasion." */
    BOTTLE_OF_WINE("Bottle of Wine"),

    /** Raffle prize — toolkit; functions as SCREWDRIVER + SPANNER combined. */
    TOOLKIT("Toolkit"),

    /** Raffle rarest prize (5%). Tooltip: "Northfield's 2014 Bank Holiday. Someone still has it." */
    COMMEMORATIVE_MUG("Commemorative Mug"),

    // ── Issue #1381: Northfield Halloween ─────────────────────────────────────

    /** Throwable item — used to egg doors, cars, and NPCs during Halloween.
     * Produces EGGED_DOOR_PROP decal and triggers NoiseSystem events. */
    RAW_EGG("Raw Egg"),

    /** Sweet — given by houses with high halloweenMoodScore during trick-or-treat. */
    MINI_CHOCOLATE_BAR("Mini Chocolate Bar"),

    /** Container — player uses this to participate in trick-or-treat route.
     * Filled with MINI_CHOCOLATE_BAR and CRISP_PACKET items from houses. */
    TRICK_OR_TREAT_BAG("Trick-or-Treat Bag"),

    /** Wearable costume — grants disguiseBonus 20 on Halloween night, 5 off-season. */
    WITCH_COSTUME("Witch Costume"),

    /** Wearable costume — grants disguiseBonus 20 on Halloween night, 5 off-season. */
    GHOST_SHEET("Ghost Sheet"),

    /** Wearable costume — grants disguiseBonus 20 on Halloween night, 5 off-season.
     * Conditional bonus suppresses SCHOOL_KID reactions. */
    SKELETON_SUIT("Skeleton Suit"),

    /** Wearable costume — grants disguiseBonus 20 on Halloween night, 5 off-season. */
    PUMPKIN_HEAD_MASK("Pumpkin Head Mask"),

    /** Seasonal produce — available at shops around Halloween.
     * Carve with KNIFE to produce CARVED_PUMPKIN + PUMPKIN_INNARDS. */
    RAW_PUMPKIN("Raw Pumpkin"),

    /** Crafted item — carved RAW_PUMPKIN becomes a JACK_O_LANTERN_PROP when placed. */
    CARVED_PUMPKIN("Carved Pumpkin"),

    /** By-product of carving RAW_PUMPKIN. Can be used as allotment compost. */
    PUMPKIN_INNARDS("Pumpkin Innards"),

    /** Wearable costume accessory — part of the witch costume set. */
    POINTY_HAT("Pointy Hat"),

    /** Wearable costume accessory — part of the witch/vampire costume set. */
    BLACK_CAPE("Black Cape"),

    // ── Issue #1383: Northfield Boxing Day Sales ──────────────────────────────

    /**
     * Frozen Prawn Ring — "A ring of frozen prawns. A Boxing Day staple."
     * Iceland sale item; 50% off during rush window (06:00–07:30). */
    FROZEN_PRAWN_RING("Frozen Prawn Ring"),

    /**
     * Party Food Platter — "Assorted party snacks on a plastic tray."
     * Iceland sale item; 50% off during rush window. */
    PARTY_FOOD_PLATTER("Party Food Platter"),

    /**
     * Luxury Biscuit Tin — "A large tin of assorted biscuits. Someone will eat all the nice ones first."
     * Iceland sale item, limited stock (8 units); scalped by Wayne outside. */
    LUXURY_BISCUIT_TIN("Luxury Biscuit Tin"),

    /**
     * George Foreman Grill — "The one everyone wants. There's only one left."
     * Iceland sale item, strictly 1 unit; triggers SALE_DISPUTE fight if two NPCs reach it.
     * Wayne scalps it for 15 COIN. Selling one to queue NPCs for 14 COIN awards SALE_SHARK. */
    GEORGE_FOREMAN_GRILL("George Foreman Grill"),

    /**
     * Bread Maker — "Not what anyone queued for. Still, it's something."
     * Charity shop Boxing Day stock; actually useful — flip at PawnShop for 2 COIN. */
    BREAD_MAKER("Bread Maker"),

    /**
     * Vinyl Record Box — "A cardboard box of mixed records. Mostly rubbish."
     * Charity shop Boxing Day stock; 5% chance contains a GENUINE_FIRST_PRESSING. */
    VINYL_RECORD_BOX("Vinyl Record Box"),

    /**
     * Genuine First Pressing — "Rare. 8 COIN at PawnShop. Brenda has no idea."
     * Hidden inside VINYL_RECORD_BOX (5% chance). Awards CHARITY_SHOP_TREASURE. */
    GENUINE_FIRST_PRESSING("Genuine First Pressing"),

    /**
     * HDMI Cable — "Found near the DVR. Suspiciously tangled."
     * Looted near Iceland DVR; connection to the CCTV Blackout incident. */
    HDMI_CABLE("HDMI Cable"),

    // ── Issue #1386: Northfield St George's Day ───────────────────────────────

    /**
     * DOOM_BAR_PINT — "Proper English ale. Goes down dangerously easy."
     * Sold by Terry at Wetherspoons during the St George's Day lock-in (23 April).
     * 3 pints in one session → DRUNK_STATE. Price: 3 COIN per pint. */
    DOOM_BAR_PINT("Doom Bar Pint"),

    /**
     * BANANA_SKIN — "A banana peel. Classic. Still funny, apparently."
     * Used to sabotage Morris Dancers on St George's Day. Triggers NoiseSystem level 7.
     * Can be found on the floor near the park on St George's Day. */
    BANANA_SKIN("Banana Skin"),

    /**
     * ENGLAND_SHIRT_PROP — "Three lions on the shirt. Ketchup on the collar."
     * Sold at PoundShop for 3 COIN. Keep condition ≥ 85 and stay sober to win
     * the Best England Shirt competition at 15:00. */
    ENGLAND_SHIRT_PROP("England Shirt"),

    /**
     * MORRIS_STICK_PROP — "A painted stick used in traditional Morris Dancing.
     * If you've got one, you've almost certainly nicked it."
     * Carried by MORRIS_DANCER NPCs (11:00–15:00 in the Park). Stealable;
     * triggers all 6 dancers to pursue the player. */
    MORRIS_STICK_PROP("Morris Stick"),

    /**
     * ST_GEORGE_FLAG_PROP — "England's finest. Hanging above the bar at Wetherspoons."
     * Obtainable by climbing BAR_STOOL_PROP and pressing E on the flag.
     * Results in ejection + 3-day ban + TOOK_THE_FLAG achievement.
     * Fenceable to Mirek for 5 COIN. */
    ST_GEORGE_FLAG_PROP("St George Flag"),

    /**
     * ROOF_FLAG_PROP — "Bigger flag. Harder to get. More suspicious to carry."
     * Obtainable via DRAINPIPE_PROP in the back alley (3-second hold-E).
     * Requires CCTV disabled. Fenceable to Mirek for 12 COIN.
     * Awards OFF_THE_ROOF achievement. */
    ROOF_FLAG_PROP("Roof Flag"),

    // ── Issue #1388: Northfield Dog Show ─────────────────────────────────────

    /**
     * DOG_BRUSH — "A slicker brush. Mostly used on the Staffy. Occasionally on the carpet."
     * Buy from CORNER_SHOP for 2 COIN, or occasionally from CHARITY_SHOP.
     * Required to set the GROOMED flag (+15 show score). Bond ≥ 10 required to use. */
    DOG_BRUSH("Dog Brush"),

    /**
     * VICTORIA_SPONGE_SLICE — "Light, fluffy, Margaret's finest. Competition-grade jam."
     * Sold at WI_STALL_PROP by WI_VOLUNTEER_NPC Margaret during the Dog Show.
     * Price: 2 COIN. Hunger +35, Warmth +5. */
    VICTORIA_SPONGE_SLICE("Victoria Sponge Slice"),

    /**
     * BEST_IN_SHOW_ROSETTE_PROP — "Red, white and royal blue. 'Best in Show — Northfield 20XX.' Clive signed it."
     * Awarded to 1st place competitor, or stealable from JUDGES_TABLE_PROP during
     * the 14:00–14:15 unattended window. Fenceable: FENCE 10 COIN, PAWN_SHOP 7 COIN. */
    BEST_IN_SHOW_ROSETTE_PROP("Best in Show Rosette"),

    /**
     * RESERVE_ROSETTE_PROP — "Blue rosette. Almost the best. Close, but no Bonio."
     * Awarded to 2nd place competitor. Fenceable: FENCE 5 COIN. */
    RESERVE_ROSETTE_PROP("Reserve Rosette"),

    /**
     * THIRD_PLACE_ROSETTE_PROP — "Yellow rosette. Bronze of dog shows."
     * Awarded to 3rd place competitor. Fenceable: FENCE 2 COIN. */
    THIRD_PLACE_ROSETTE_PROP("Third Place Rosette"),

    /**
     * SHOW_SCHEDULE_FLYER — "Northfield Annual Dog Show. All breeds welcome. Clive judging. Bring a brush."
     * Readable prop; no mechanical effect. Distributed around Northfield on show day. */
    SHOW_SCHEDULE_FLYER("Show Schedule Flyer"),

    /**
     * CUP_OF_TEA — "Hot, strong, restorative. Margaret's best brew."
     * Sold at WI_STALL_PROP for 1 COIN. Warmth +20. */
    CUP_OF_TEA("Cup of Tea"),

    // ── Issue #1394: England Match Night ─────────────────────────────────────

    /**
     * ENGLAND_SHIRT — "Three lions. Polyester. Optimism sold separately."
     * Wearable (chest slot). Sold at Newsagent for 8 COIN.
     * Provides crowd-blend disguise during England match (Terry detection −90%). */
    ENGLAND_SHIRT("England Shirt"),

    /**
     * MATCH_FIX_ITEM — "A sealed envelope. The less you know, the better. Marchetti sends his regards."
     * Obtained from Marchetti Crew at Respect ≥ 60 for 50 COIN. Single-use.
     * Forces ENGLAND_LOSS result when used before 20:00 on a match day. */
    MATCH_FIX_ITEM("Match Fix Envelope"),

    /**
     * GERMAN_FLAG — "Black, red, gold. Bring it to a Wetherspoons on match night. See what happens."
     * Craftable (2× FABRIC_SCRAP + RED_DYE) or found in Charity Shop.
     * Planting on pub noticeboard triggers HOSTILE_TO_PLAYER state in all crowd for 30s. */
    GERMAN_FLAG("German Flag"),

    /**
     * SIGNED_SHIRT — "Signed by someone. Authenticity certificate missing. Terry says it's genuine."
     * Lootable from TROPHY_CABINET_PROP inside Wetherspoons. Fenceable: 25 COIN. */
    SIGNED_SHIRT("Signed Shirt"),

    /**
     * FA_CUP_REPLICA — "A plastic FA Cup. Gold-painted. Terry won it at the fair in 1997. He insists it's real."
     * Lootable from TROPHY_CABINET_PROP. Fenceable: 20 COIN. */
    FA_CUP_REPLICA("FA Cup Replica"),

    /**
     * GOLDEN_BOOT_PROP — "A spray-painted boot on a plinth. 'Top Scorer 1991.' No name. Dave won't say whose."
     * Lootable from TROPHY_CABINET_PROP. Fenceable: 30 COIN. Also pawnable: 18 COIN. */
    GOLDEN_BOOT_PROP("Golden Boot"),

    // ── Issue #1394: Northfield Primary School Sports Day ─────────────────────

    /**
     * BOURBONS — "Tube of Bourbons from Dot's stall. 50p. Worth every penny."
     * Sold at refreshment stall for 1 COIN during Sports Day. Heals 2 HP. */
    BOURBONS("Bourbons"),

    /**
     * SQUASH_CARTON — "Orange squash in tiny carton. Straw pre-attached. Nostalgic."
     * Sold at refreshment stall for 1 COIN during Sports Day. Heals 1 HP; cures THIRST. */
    SQUASH_CARTON("Squash Carton"),

    /**
     * SPORTS_DAY_ROSETTE — "1st Place. St. Aidan's Sports Day. Derek's handwriting."
     * Lootable from PRIZE_TABLE_PROP (3× available). Fenceable: 2 COIN. Keepable as trophy. */
    SPORTS_DAY_ROSETTE("Sports Day Rosette"),

    /**
     * FIZZY_DRINK_CAN — "Full, unopened. Could be thrown. Probably shouldn't be."
     * Throwable item. Throwing at WASP_NEST_PROP disperses wasps faster (3 NPCs stung, Wanted +1). */
    FIZZY_DRINK_CAN("Fizzy Drink Can"),

    // ── Issue #1396: Northfield Royal Mail Strike ─────────────────────────────

    /**
     * TEABAG — "Proper Yorkshire. Found in the kitchen cupboard. Kettle-ready."
     * Ingredient for crafting MUG_OF_TEA (KETTLE_PROP + TEABAG). Found in kitchen cupboard props. */
    TEABAG("Teabag"),

    /**
     * SCAB_PARCEL_BAG — "Royal Mail sack. Four deliveries inside. Shame in every one."
     * Temporary item given during the scab shift; removed on shift end or abandonment. */
    SCAB_PARCEL_BAG("Scab Parcel Bag"),

    /**
     * HANDWRITTEN_PARCEL — "Brown paper, string, biro address. Pensioner special."
     * Unofficial courier parcel given by a frustrated PENSIONER or PUBLIC NPC. Single-use delivery item. */
    HANDWRITTEN_PARCEL("Handwritten Parcel"),

    /**
     * UNION_HERO_BADGE — "TUC solidarity pin badge. Reduces PCSO suspicion by 1 star."
     * Wearable reward for completing the full solidarity path on day 3. */
    UNION_HERO_BADGE("Union Hero Badge"),

    /**
     * BOLT_CUTTER — "Heavy-duty. One snip and you're in. Single use."
     * Tool — bypasses locked doors/props in 1 use (vs. 3 CROWBAR uses). Fenceable. */
    BOLT_CUTTER("Bolt Cutter"),

    // ── Issue #1398: Northfield Window Cleaner ────────────────────────────────

    /**
     * BUCKET_AND_CHAMOIS — "A yellow bucket, a squeegee, and more chamois than you need.
     * Terry's livelihood in material form."
     * Required to offer rival window cleaning. Buy from corner shop (8 COIN) or steal Terry's. */
    BUCKET_AND_CHAMOIS("Bucket and Chamois"),

    /**
     * TERRY_DEBT_NOTE — "Payment refused. Property: 14 Acacia Ave. Amount: 2 COIN. Date noted."
     * Given to player after reporting a defaulter to Terry. Redeemable for 1 COIN + gossip. */
    TERRY_DEBT_NOTE("Terry's Debt Note"),

    // ── Issue #1400: Northfield Residents' Parking Permit Racket ─────────────

    /**
     * PARKING_PERMIT — "Zone S Residents' Parking Permit. Valid 168 hours. Do not bend."
     * Sold by Brenda (COUNCIL_CLERK) at the COUNCIL_OFFICE_KIOSK for 4 COIN.
     * Valid 168 in-game hours; prevents Barry from clamping the player's car. */
    PARKING_PERMIT("Parking Permit"),

    /**
     * FORGED_PARKING_PERMIT — "Zone S Residents' Parking Permit. Printed in Comic Sans."
     * Crafted from STOLEN_PRINTER_INK + CARDBOARD (or at internet café).
     * 20% detection chance when Barry inspects → CrimeType.DOCUMENT_FRAUD, Notoriety +5. */
    FORGED_PARKING_PERMIT("Forged Parking Permit"),

    /**
     * WHEEL_CLAMP_KEY — "Barry's release key. Feels wrong to have it."
     * Used to attempt clamp removal without paying 10 COIN; failure = Notoriety +6, WantedStar +1. */
    WHEEL_CLAMP_KEY("Wheel Clamp Key"),

    /**
     * STOLEN_PRINTER_INK — "HP DeskJet cartridge. Still in the sealed box. Almost certainly nicked."
     * Crafting ingredient for FORGED_PARKING_PERMIT. Found in office blocks or bought from fence. */
    STOLEN_PRINTER_INK("Stolen Printer Ink"),

    // ── Issue #1402: Northfield Severn Trent Road Dig ─────────────────────────

    /**
     * THERMOS — "Battered red thermos. Still warm. Smells of builder's tea."
     * Always present in welfare cabin loot. Restores +8 warmth when consumed.
     * Can be handed to CONTRACTOR_STEVE or CONTRACTOR_PHIL to raise CONTRACTOR_GOODWILL (+20 each). */
    THERMOS("Thermos"),

    /**
     * SITE_RADIO — "Roberts radio covered in plaster dust. Tuned to Radio 2."
     * 60% chance in welfare cabin loot. Fenceable for 4 COIN.
     * Also usable to listen to pirate radio (PirateRadioSystem). */
    SITE_RADIO("Site Radio"),

    /**
     * CONTRACTOR_CLIPBOARD — "Severn Trent job pack. Mostly blank variation-order forms."
     * 40% chance in welfare cabin loot. Key item for council billing scam (Mechanic 4).
     * Also buyable at InternetCafe for 2 COIN. */
    CONTRACTOR_CLIPBOARD("Contractor Clipboard"),

    /**
     * HARD_HAT — "White hard hat. 'SEVERN TRENT' stencilled on the back in marker pen."
     * Part of contractor disguise (with HI_VIS_JACKET). Bought at corner shop for 2 COIN. */
    HARD_HAT("Hard Hat"),

    /**
     * MYSTERY_OBJECT — "You're not sure what this is. Looks old. Possibly valuable."
     * Found in BURIED_STASH_PROP under the trench. Fenceable for 8 COIN. */
    MYSTERY_OBJECT("Mystery Object"),

    // ── Issue #1404: Northfield Community Litter Pick ─────────────────────────

    /**
     * LITTER_PICKER_STICK — "Bright orange reacher-grabber. Smells of council storage."
     * Handed out by Janet (LITTER_PICK_COORDINATOR) at the start of the event.
     * Required to collect LITTER_PROP items (press E). Cannot be sold or fenced. */
    LITTER_PICKER_STICK("Litter Picker Stick"),

    /**
     * COUNCIL_RUBBISH_BAG — "Large black sack with 'NORTHFIELD TIDY STREETS' printed on the side."
     * Handed out by Janet alongside LITTER_PICKER_STICK. Tracks collected litter count (0–30+).
     * Return to Janet with ≥ 8 items for Notoriety −5 + GOOD_CITIZEN buff.
     * If bag contains CRACK_PIPE: Janet screams, police called, Notoriety +8. */
    COUNCIL_RUBBISH_BAG("Council Rubbish Bag"),

    /**
     * CRACK_PIPE — "Scorched glass pipe. Nobody's going to think you found it on the floor."
     * Found in HIDDEN_STASH_PROP (10% chance). Triggers DRUG_POSSESSION CrimeType if player
     * is searched or if returned in bag to Janet. Can be fenced for 2 COIN.
     * Possession triggers JANETS_WORST_DAY achievement when returned in bag. */
    CRACK_PIPE("Crack Pipe"),

    // ── Issue #1406: Northfield Dodgy Roofer ──────────────────────────────────

    /**
     * BUCKET_OF_SEALANT — "Grey sealant compound. Could fix guttering. Probably won't."
     * Always present in ROOFER_VAN_PROP loot. Required item for Mechanic 3 (rival cold-calling).
     * Buy from corner shop or steal from Kenny's van. */
    BUCKET_OF_SEALANT("Bucket of Sealant"),

    /**
     * LATEX_GLOVES — "Box of disposable gloves. Unbranded, obviously."
     * Required alongside BUCKET_OF_SEALANT for Mechanic 3 (rival cold-calling round).
     * Buy from corner shop. */
    LATEX_GLOVES("Latex Gloves"),

    /**
     * SCAFFOLDING_SPANNER — "Heavy steel podger. 50% chance in Kenny's van loot."
     * Fenceable for 5 COIN. Can also be used as a weapon (counts as IMPROVISED_TOOL). */
    SCAFFOLDING_SPANNER("Scaffolding Spanner"),

    /**
     * INVOICE_PAD — "Printed invoice forms. 'KR Roofing Solutions' header. Looks almost legit."
     * 35% chance in Kenny's van loot. Required for Mechanic 5 (invoice fraud).
     * Used to forge follow-up invoices at recently-worked houses. */
    INVOICE_PAD("Invoice Pad"),

    /**
     * CASH_ENVELOPE — "Thick envelope. You can feel the notes inside."
     * 20% chance in Kenny's van loot. Contains 10–20 COIN on use.
     * Triggers CrimeType.VEHICLE_BREAK_IN if found without prior crime recording. */
    CASH_ENVELOPE("Cash Envelope"),

    /**
     * ROOF_SLATE_BAG — "Canvas bag of second-hand roofing slates. Heavy."
     * 15% chance in Kenny's van loot. Fenceable for 8 COIN. */
    ROOF_SLATE_BAG("Roof Slate Bag"),

    // ── Issue #1408: Northfield Catalogue Man ─────────────────────────────────

    /** Knockoff novelty from Barry's catalogue bag — trinkets (ornaments, keyrings, etc.). Fence value 2–3 COIN. */
    CATALOGUE_TRINKET("Catalogue Trinket"),

    /** Knockoff tool from Barry's catalogue bag — screwdrivers, tape measures, etc. Fence value 3–5 COIN. */
    CATALOGUE_TOOL("Catalogue Tool"),

    /** Knockoff textile from Barry's catalogue bag — tea towels, blankets, etc. Fence value 2–4 COIN. */
    CATALOGUE_TEXTILE("Catalogue Textile"),

    /** Receipt proving a household owes Barry money. Tip off the Loan Shark for 3 COIN finder's fee. */
    CATALOGUE_RECEIPT("Catalogue Receipt"),

    /**
     * Counterfeit product sample from Barry's deliveries.
     * Obtained after witnessing 5 undetected deliveries.
     * Three paths: report to Trading Standards, blackmail Barry, or craft a KNOCKOFF_CATALOGUE.
     */
    CATALOGUE_SAMPLE("Catalogue Sample"),

    /**
     * Rival knockoff catalogue — crafted at the InternetCafe.
     * Run a rival round: 35% accept rate, 4 COIN/sale.
     * Monthly Trading Standards check (last Friday 14:00) has 20% catch chance.
     */
    KNOCKOFF_CATALOGUE("Knockoff Catalogue"),

    // ── Issue #1416: Northfield Mobile Speed Camera Van ───────────────────────

    /**
     * SPEED_CAMERA_SD_CARD — the SD card from Sharon's GATSO speed camera.
     * Stolen by holding E on SPEED_CAMERA_VAN_PROP for 4 seconds while Sharon is distracted.
     * Sell to FenceSystem (20 COIN), a SPEEDING_DRIVER_NPC (15 COIN + GRATEFUL_DRIVER rumour),
     * or a journalist via phone box (30 COIN, triggers police patrol increase).
     * Achievement: CANDID_CAMERA after 3 steals.
     */
    SPEED_CAMERA_SD_CARD("Speed Camera SD Card"),

    /**
     * SPEEDING_FINE_NOTICE — an official speeding fine notice photographed by the GATSO.
     * Produced when a car is flashed. Sellable or usable as evidence.
     */
    SPEEDING_FINE_NOTICE("Speeding Fine Notice"),

    /**
     * HANDWRITTEN_WARNING_SIGN — a cardboard sign warning drivers of the speed camera ahead.
     * Crafted from MARKER_PEN + CARDBOARD. Placed as HANDWRITTEN_WARNING_SIGN_PROP on the road.
     * Automatically warns approaching SPEEDING_DRIVER_NPCs for 10 in-game minutes.
     */
    HANDWRITTEN_WARNING_SIGN("Handwritten Warning Sign"),

    // ── Issue #1420: Northfield Post Office Horizon Scandal ──────────────────

    /**
     * CITIZENS_ADVICE_LEAFLET — Citizens Advice Bureau leaflet given to Maureen to initiate
     * the whistleblower path. "Know your rights. Post Office employees are not alone."
     * Stack size 1. Not fenceable.
     */
    CITIZENS_ADVICE_LEAFLET("Citizens Advice Leaflet"),

    /**
     * SHORTFALL_LETTER — the official Post Office Ltd demand notice served to Maureen on day 14.
     * "Post Office Ltd demand notice. Horizon deficit: £340." Stack size 1. Not fenceable.
     * Exists as both inventory item and as SHORTFALL_LETTER_PROP pinned to the counter.
     */
    SHORTFALL_LETTER("Shortfall Letter"),

    /**
     * TRANSACTION_LOG — Horizon terminal printout from the Post Office back-room filing cabinet.
     * "Horizon terminal printout. Columns of numbers, none of which add up." Stack size 3.
     * Fence value 4 COIN (to REGIONAL_AUDITOR only: 12 COIN).
     * 3 units required for full tribunal evidence ({@code tribunalEvidenceStrength == STRONG}).
     */
    TRANSACTION_LOG("Transaction Log"),

    /**
     * STAMPS_BUNDLE — bundle of first-class stamps found in the Post Office safe.
     * Obtained during the 90-second audit window safe crack (25–50 COIN + stamps bundle).
     * Stack size 5. Fenceable at 2 COIN per unit; sellable at corner shop for 1 COIN each.
     */
    STAMPS_BUNDLE("Stamps Bundle"),

    /**
     * IT_CONTRACTOR_ID_BADGE — Pete's work lanyard ID badge. Pickpocketable from IT_CONTRACTOR NPC.
     * "Pete's ID. Photo makes him look like he's confessing." Stack size 1. Fence value 6 COIN.
     */
    IT_CONTRACTOR_ID_BADGE("IT Contractor ID Badge"),

    /**
     * USB_STICK — USB drive from Pete's laptop bag. Pickpocketable from IT_CONTRACTOR NPC.
     * "USB drive from Pete's laptop bag. Contains 4GB of Horizon audit logs and 2GB of
     * questionable music." Stack size 1. Journalist trade value 18 COIN; also tradeable
     * to Maureen (free) to strengthen her tribunal case.
     */
    USB_STICK("USB Stick"),

    // ── Issue #1422: Northfield Charity Sponsored Walk ────────────────────────

    /**
     * SPONSOR_FORM — "Official Northfield Hospice Sponsored Walk form. Six names, six promises."
     * Stack size 1. Not fenceable.
     */
    SPONSOR_FORM("Sponsor Form"),

    /**
     * TRAFFIC_CONE — "Orange traffic cone. Surprisingly heavy. Surprisingly tempting."
     * Stack size 4. Fence value 1 COIN. Obtained by punching ROUTE_CONE_PROP twice.
     */
    TRAFFIC_CONE("Traffic Cone"),

    /**
     * CHARITY_RAFFLE_TICKET — "Raffle ticket No. 47. First prize: a hamper from Iceland."
     * Stack size 1. No fence value; tradeable to SPONSORED_WALKER NPCs for 1 COIN goodwill.
     * Obtained alongside COIN when grabbing the PRIZE_ENVELOPE_PROP.
     */
    CHARITY_RAFFLE_TICKET("Charity Raffle Ticket"),

    // ── Issue #1424: Northfield Doorstep Energy Tout ─────────────────────────

    /**
     * TOUT_CLIPBOARD — Craig's PowerSave UK branded clipboard with resident meter-read list.
     * "A clipboard with 'PowerSave UK — Meter Reading Division' printed on it. The badge looks
     * fake. The uniform looks fake. The smile is definitely fake."
     * <ul>
     *   <li>Fence value: 12 COIN.</li>
     *   <li>Can be used to run Craig's own round door-to-door (up to 6 addresses, 8 COIN each).</li>
     *   <li>3+ doorstep knocks with this item records {@code DOORSTEP_FRAUD} crime.</li>
     * </ul>
     * Stack size 1. Obtained by pickpocketing {@link ragamuffin.entity.NPCType#ENERGY_TOUT}
     * (Stealth ≥ 1) or assaulting him; or dropped when {@link ragamuffin.world.PropType#TOUT_CLIPBOARD_PROP}
     * is destroyed.
     */
    TOUT_CLIPBOARD("Tout's Clipboard"),

    /**
     * SMART_METER_KIT — Boxed PowerSave UK smart meter installation kit.
     * "Still in the shrink-wrap. 'Not for resale' sticker partially removed."
     * <ul>
     *   <li>Fence value: 18 COIN.</li>
     *   <li>Install in squat for fake prestige (+5 neighbourhood vibe decoration score).</li>
     *   <li>Trade to {@link ragamuffin.entity.NPCType#PIGEON_FANCIER} for 10 COIN + {@code FANCIER_FAVOUR} flag.</li>
     * </ul>
     * Stack size 1. Obtained by breaking into {@link ragamuffin.world.PropType#ENERGY_VAN_PROP}
     * via the GLASS window (2 hits, NoiseSystem +25).
     */
    SMART_METER_KIT("Smart Meter Kit"),

    /**
     * CRAIG_WITNESS_STATEMENT — Handwritten account of Craig's door-to-door scam.
     * "Three pages of careful notes. Names, times, addresses. The Citizens Advice woman
     * looked at it and said 'oh, this is Craig again'."
     * <ul>
     *   <li>Crafted from {@code SCRAP_PAPER} + {@code BIRO_PEN} after tailing Craig for 3+ entries.</li>
     *   <li>Deliver to CitizensAdvice to trigger Trading Standards shutdown of Craig.</li>
     *   <li>Reward: residents get partial refunds, neighbourhood vibes +6, Notoriety −5.</li>
     * </ul>
     * Stack size 1.
     */
    CRAIG_WITNESS_STATEMENT("Craig's Witness Statement"),

    /**
     * DAVE_DEBT_LIST — A scrawled list of names and addresses in Dave's handwriting.
     * "Several crossed out. Several circled. 'Outstanding: £40' written next to your address."
     * <ul>
     *   <li>Fence value: 3 COIN.</li>
     *   <li>Dropped by {@link ragamuffin.entity.NPCType#TOUT_ENFORCER} (Dave) on defeat alongside BURNER_PHONE.</li>
     *   <li>Can be sold to the Loan Shark NPC for 6 COIN.</li>
     * </ul>
     * Stack size 1.
     */
    DAVE_DEBT_LIST("Dave's Debt List"),

    /**
     * REPLACEMENT_CLIPBOARD — Blank clipboard purchased from the pound shop.
     * "Could be used for anything. Currently earmarked for door-to-door fraud."
     * <ul>
     *   <li>Craftable from WOOD + SCRAP_PAPER at workbench.</li>
     *   <li>Functions identically to {@code TOUT_CLIPBOARD} for running the door-to-door round.</li>
     *   <li>No fence value (too generic).</li>
     * </ul>
     * Stack size 1.
     */
    REPLACEMENT_CLIPBOARD("Replacement Clipboard"),

    /**
     * FANCIER_FAVOUR — A small handwritten IOU from the Pigeon Fancier.
     * "'One favour owed — Reg' on a folded piece of paper torn from a Racing Post."
     * <ul>
     *   <li>Obtained by trading {@code SMART_METER_KIT} to {@link ragamuffin.entity.NPCType#PIGEON_FANCIER}
     *       for 10 COIN + this item.</li>
     *   <li>Sets the {@code FANCIER_FAVOUR} player flag — Reg will vouch for the player at the
     *       pigeon racing club or provide a tip-off on an upcoming race.</li>
     * </ul>
     * Stack size 1.
     */
    FANCIER_FAVOUR("Fancier's Favour"),

    // ── Issue #1426: Northfield Neighbourhood WhatsApp Group ─────────────────

    /**
     * STRAY_CAT — "Whiskers. Very friendly. Slightly damp."
     * <ul>
     *   <li>Obtained by pressing E on {@link ragamuffin.entity.NPCType#LOST_CAT_NPC} (Whiskers).</li>
     *   <li>Stack size 1. Fence value 0. Tradeable.</li>
     *   <li>Deliverable to owner's door for 3 COIN reward via {@code WhatsAppGroupSystem.returnCatToOwner()}.</li>
     *   <li>Sellable to {@link ragamuffin.entity.NPCType#PET_SHOP_OWNER} for 5 COIN.</li>
     * </ul>
     */
    STRAY_CAT("Stray Cat"),

    /**
     * CAT_RANSOM_NOTE — "Anon note: 'Your cat is safe. Leave 1 COIN on the step.'"
     * <ul>
     *   <li>Crafted from {@code SCRAP_PAPER} + {@code BIRO_PEN}.</li>
     *   <li>Stack size 1. Not fenceable.</li>
     *   <li>Used by the EXTORTION ransom path in {@code WhatsAppGroupSystem.ransomCat()}.</li>
     * </ul>
     */
    CAT_RANSOM_NOTE("Cat Ransom Note"),

    // ── Issue #1428: Northfield Council CCTV Audit ───────────────────────────

    /**
     * CCTV_FOOTAGE — "VHS tape. Keith's handwriting on the label. Best destroyed."
     * <ul>
     *   <li>Generated as evidence when player commits a crime within range of a live CCTV camera.</li>
     *   <li>Stack size 1. Fence value 4 COIN. Flagged stolen.</li>
     *   <li>Destroy at CAMPFIRE_PROP or WHEELIE_BIN_FIRE_PROP to clear evidence retroactively.</li>
     * </ul>
     */
    CCTV_FOOTAGE("CCTV Footage"),

    /**
     * DUMMY_CCTV_CAMERA — "A hollow plastic camera with a dead battery inside. Classic."
     * <ul>
     *   <li>Obtained by removing a dummy camera with SCREWDRIVER + REPAIR ≥ 1.</li>
     *   <li>Stack size 1. Fence value 2 COIN.</li>
     *   <li>Can be re-installed as a decoy to make nearby NPCs nervous/flee.</li>
     * </ul>
     */
    DUMMY_CCTV_CAMERA("Dummy CCTV Camera"),

    /**
     * MEAL_DEAL_ITEM — "Sandwich, crisps, drink. The ultimate council bribe."
     * <ul>
     *   <li>Crafted: SANDWICH + CRISPS + FIZZY_DRINK.</li>
     *   <li>Stack size 1. Not fenceable.</li>
     *   <li>Used to bribe Keith (CCTV_OPERATOR) with 80% success for 15-minute camera blindspot.</li>
     * </ul>
     */
    MEAL_DEAL_ITEM("Meal Deal"),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1433: Northfield Easter Weekend
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * HOT_CROSS_BUN — Fresh from Greggs on Good Friday morning.
     * <ul>
     *   <li>Obtained from the Greggs queue on day 91 (Good Friday), 08:00–10:00.</li>
     *   <li>Heals 5 HP when consumed. Stack size 12.</li>
     * </ul>
     */
    HOT_CROSS_BUN("Hot Cross Bun"),

    /**
     * CHOCOLATE_EGG — Filched from the Easter Bunny's pocket.
     * <ul>
     *   <li>Obtained by pickpocketing {@link ragamuffin.entity.NPCType#EASTER_BUNNY_NPC} (×3 per lift).</li>
     *   <li>Heals 3 HP. Fenceable for 2 COIN each.</li>
     * </ul>
     */
    CHOCOLATE_EGG("Chocolate Egg"),

    /**
     * FOIL_EASTER_EGG — A shiny foil-wrapped egg from the council egg hunt.
     * <ul>
     *   <li>Collected by pressing E within 1 block of an {@link ragamuffin.world.PropType#EASTER_EGG_PROP}.</li>
     *   <li>Not consumable; counts toward {@link ragamuffin.ui.AchievementType#EASTER_EGG_BARON}.</li>
     * </ul>
     */
    FOIL_EASTER_EGG("Foil Easter Egg"),

    /**
     * EASTER_BASKET — Wicker basket for egg collection.
     * <ul>
     *   <li>Crafted item; increases egg-carrying capacity during the hunt.</li>
     * </ul>
     */
    EASTER_BASKET("Easter Basket"),

    /**
     * BIKER_JACKET — Heavy leather jacket lifted from a parked motorbike.
     * <ul>
     *   <li>Requires STEALTH ≥ 2 and 3-second E-hold on {@link ragamuffin.world.PropType#MOTORBIKE_PROP}.</li>
     *   <li>Provides 2 armour. Fenceable for 15 COIN.</li>
     * </ul>
     */
    BIKER_JACKET("Biker Jacket"),

    /**
     * CHARITY_BUCKET_EASTER — The collection bucket at the park entrance during the motorbike parade.
     * <ul>
     *   <li>Stealing triggers Notoriety +8, Wanted +1, and all {@link ragamuffin.entity.NPCType#BIKER_NPC} become HOSTILE.</li>
     *   <li>Donating grants StreetRep +3 and BIKER respect +5.</li>
     * </ul>
     */
    CHARITY_BUCKET_EASTER("Charity Bucket"),

    /**
     * BIKERS_VOUCHER — A complimentary voucher handed out by bikers after a generous donation.
     * <ul>
     *   <li>Redeemable at KebabVan for a free kebab.</li>
     * </ul>
     */
    BIKERS_VOUCHER("Bikers' Voucher"),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1435: Northfield Community Speedwatch
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SPEED_GUN — A calibrated Stalker II speed gun. Technically police-grade.
     * Keith bought his off eBay.
     * Stolen from TRIPOD_SPEED_GUN_PROP by holding E for 4 seconds while Keith is distracted.
     * Fence value: 12 COIN (FenceSystem). Sell to SPEEDING_DRIVER_NPC: 8 COIN.
     * Alt use: hold E to measure passing car speeds for 60 seconds (flavour dialogue).
     * Achievement: COMMUNITY_POLICING_ENDS_HERE on theft.
     */
    SPEED_GUN("Speed Gun"),

    /**
     * SPEEDWATCH_CLIPBOARD — Laminated sheet, 3 columns: TIME, PLATE, SPEED.
     * Keith's handwriting is surprisingly neat.
     * Given to player on joining as a volunteer (E on Keith with HI_VIS_VEST, Notoriety ≤ 25).
     * Used to record passing vehicles during session (E within 5 seconds of vehicle passing).
     * Can be abused outside session to send FAKE_SPEEDWATCH_LETTER items.
     */
    SPEEDWATCH_CLIPBOARD("Speedwatch Clipboard"),

    /**
     * SPEEDWATCH_WARNING_LETTER — A politely worded letter explaining that you were observed
     * exceeding the speed limit. It is not a fine. It is, Keith notes, just as effective.
     * Delivered 2 in-game days after detection by CommunitySpeedwatchSystem.
     * Receiving 3 letters triggers NotorietySystem community reputation −2 and LOCAL_GOSSIP rumour.
     */
    SPEEDWATCH_WARNING_LETTER("Speedwatch Warning Letter"),

    /**
     * SPEEDWATCH_LANYARD — Honorary Community Speedwatch Member. Keith laminated it himself.
     * It says 'DO NOT DUPLICATE'.
     * Awarded by Keith when player recovers the stolen clipboard from the confrontation driver.
     * When worn: grants "Volunteer" title in StreetReputation display (StreetSkillSystem).
     */
    SPEEDWATCH_LANYARD("Speedwatch Lanyard"),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1447: Northfield Street Hustler — Danny's Three-Card Monte
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * RIGGED_CARD_DECK — "A bent deck with a marked queen. Property of Danny Doyle. Do not touch."
     * <ul>
     *   <li>Pickpocketable from {@link ragamuffin.entity.NPCType#STREET_HUSTLER_NPC} (PICKPOCKET ≥ Apprentice, 25% success).</li>
     *   <li>Stored at LOCK_UP_GARAGE_PROP 02:00–05:00.</li>
     *   <li>Deploy own game via E on pavement ({@link ragamuffin.world.PropType#MAKESHIFT_TABLE_PROP}).</li>
     *   <li>Fence value: 3 COIN. Cannot be sold at CharityShopSystem.</li>
     * </ul>
     */
    RIGGED_CARD_DECK("Rigged Card Deck"),

    /**
     * LOOKOUT_CUT — "Folded notes, quickly counted. Don't ask where they came from."
     * <ul>
     *   <li>Awarded to the player per 10 in-game minutes while acting as lookout for Danny.</li>
     *   <li>4 COIN value; auto-converts to COIN on pickup; not a persistent item.</li>
     * </ul>
     */
    LOOKOUT_CUT("Lookout Cut"),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1449: Northfield Mobile Library — Keith's Van
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * LIBRARY_CARD — "Northfield Mobile Library Card. Issued by Keith. Keep it safe."
     * <ul>
     *   <li>Given free by MOBILE_LIBRARIAN on first visit to the van.</li>
     *   <li>Required to borrow books from the MobileLibraryUI.</li>
     *   <li>Confiscated on LIBRARY_THEFT detection; restored after paying 5 COIN goodwill.</li>
     * </ul>
     */
    LIBRARY_CARD("Library Card"),

    /**
     * RARE_BOOK — "A slim volume. Vellum cover, no price sticker. Almost certainly shouldn't be here."
     * <ul>
     *   <li>Stolen from RARE_BOOK_SHELF_PROP at van rear (3s hold-E while Keith distracted).</li>
     *   <li>Fence value: 18 COIN pawn / 12 COIN fence.</li>
     *   <li>Awards OVERDUE_FOREVER achievement.</li>
     * </ul>
     */
    RARE_BOOK("Rare Book"),

    /**
     * SELF_HELP_PAPERBACK — "Believe in Yourself! (Second Edition). Slightly water-damaged."
     * <ul>
     *   <li>Borrowable from mobile library; must be returned within 7 in-game days.</li>
     *   <li>Reading from inventory: Notoriety −1/day for 1 day.</li>
     * </ul>
     */
    SELF_HELP_PAPERBACK("Self-Help Paperback"),

    /**
     * LOCAL_HISTORY_BOOK — "Northfield Through the Ages, Vol. 2. Foxed pages. Smells of a library."
     * <ul>
     *   <li>Borrowable from mobile library; must be returned within 7 in-game days.</li>
     *   <li>Reading from inventory: reveals 2 random rumours from the RumourNetwork.</li>
     * </ul>
     */
    LOCAL_HISTORY_BOOK("Local History Book"),

    /**
     * CRIME_FICTION — "The Long Arm. Dog-eared thriller. 'From the library of K. Butterworth'."
     * <ul>
     *   <li>Borrowable from mobile library; must be returned within 7 in-game days.</li>
     *   <li>Reading from inventory: PICKPOCKET XP +1.</li>
     * </ul>
     */
    CRIME_FICTION("Crime Fiction"),

    /**
     * JOB_SKILLS_GUIDE — "Get That Job! Interview tips inside. Spine cracked."
     * <ul>
     *   <li>Borrowable from mobile library; must be returned within 7 in-game days.</li>
     *   <li>Reading from inventory: interview success +5% for 2 in-game days.</li>
     * </ul>
     */
    JOB_SKILLS_GUIDE("Job Skills Guide"),

    /**
     * GARDENING_ENCYCLOPEDIA — "The Complete Garden (1987 edition). Heavy. Smells of potting compost."
     * <ul>
     *   <li>Borrowable from mobile library; must be returned within 7 in-game days.</li>
     *   <li>Reading from inventory: allotment yield ×1.2 for 1 growth cycle.</li>
     * </ul>
     */
    GARDENING_ENCYCLOPEDIA("Gardening Encyclopedia"),

    /**
     * COOKBOOK — "Cooking on a Budget. Well-thumbed. Gravy stain on page 47."
     * <ul>
     *   <li>Borrowable from mobile library; must be returned within 7 in-game days.</li>
     *   <li>Reading from inventory: MUG_OF_TEA costs 1 fewer COIN for 1 in-game day.</li>
     * </ul>
     */
    COOKBOOK("Cookbook"),

    /**
     * LIBRARY_VOUCHER — "One free book loan. Signed 'Keith'. Slightly crumpled."
     * <ul>
     *   <li>Given by MOBILE_LIBRARIAN when Save the Library petition reaches 20 signatures.</li>
     *   <li>Redeemable for one free book loan, waiving any overdue fine.</li>
     * </ul>
     */
    LIBRARY_VOUCHER("Library Voucher"),

    /**
     * PETITION_BOARD — "Save Our Mobile Library! Sign here."
     * <ul>
     *   <li>Appears from day 14 near the mobile library van.</li>
     *   <li>Player can sign (E); counts as 3 signatures if Notoriety &lt; 20.</li>
     *   <li>Steal the board (7 HARD hits): service cut 5 days, Notoriety +3, Vibes −5.</li>
     * </ul>
     */
    PETITION_BOARD("Petition Board"),

    // ── Issue #1459: Northfield Church Hall Jumble Sale ───────────────────────

    /**
     * JUMBLE_ENTRY_TICKET — paper entry ticket from Dot for 1 COIN.
     * Required to enter the Community Centre during jumble sale (09:00–13:00).
     */
    JUMBLE_ENTRY_TICKET("Jumble Sale Entry Ticket"),

    /**
     * JUMBLE_ORNAMENT — china ornament from the premium table (table 3).
     * Fence value: 3 COIN. Item base price: 2 COIN.
     */
    JUMBLE_ORNAMENT("Ornament"),

    /**
     * JUMBLE_CLOCK — old mantle clock from the premium table.
     * Fence value: 5 COIN. Item base price: 3 COIN.
     */
    JUMBLE_CLOCK("Old Clock"),

    /**
     * JUMBLE_BOOK_LOT — bundle of paperbacks from the premium table.
     * Fence value: 2 COIN. Item base price: 1 COIN.
     */
    JUMBLE_BOOK_LOT("Book Bundle"),

    /**
     * JUMBLE_CASSETTE — cassette tape from the tat tables (13–15).
     * Fence value: 0 COIN. Item base price: 1 COIN.
     */
    JUMBLE_CASSETTE("Cassette Tape"),

    /**
     * JUMBLE_VHS_TAPE — VHS tape from the tat tables.
     * Fence value: 1 COIN. Item base price: 1 COIN.
     */
    JUMBLE_VHS_TAPE("VHS Tape"),

    /**
     * JUMBLE_COAT — second-hand coat from the tat tables.
     * Confers +15 warmth when equipped; reduces volunteer detection 20%.
     * Item base price: 2 COIN.
     */
    JUMBLE_COAT("Second-Hand Coat"),

    /**
     * JUMBLE_RECEIPT_PROP — handwritten receipt from Dot's bring-and-buy table.
     * Valid proof of legitimate purchase; clears the STOLEN flag from a donated item.
     */
    JUMBLE_RECEIPT_PROP("Jumble Receipt"),

    // ── Issue #1461: Northfield Street Preacher ───────────────────────────────

    /**
     * MEGAPHONE — Brother Gary's hand-held megaphone.
     * Stolen by pickpocket (25% base, PICKPOCKET Apprentice required) or taken freely
     * during the 12:30–13:00 thermos break window. Fence value: 8 COIN.
     * When used by the player, broadcasts noise at 20-block radius for NPC distraction.
     */
    MEGAPHONE("Megaphone"),

    /**
     * BLESSED_WATER_BOTTLE — small bottle of "holy water" sold by Brother Gary
     * for 3 COIN as a scam (0 HP restore, no real effect).
     * Can be used as evidence at the COUNCIL_OFFICE to trigger the Trading Standards sting.
     * Holding it as evidence earns a 5 COIN whistleblower reward.
     */
    BLESSED_WATER_BOTTLE("Blessed Water Bottle"),

    // ── Issue #1469: Northfield Second-Hand Record Shop ───────────────────────

    /**
     * RARE_PRESSING — a genuine rare vinyl pressing from a RECORD_SHELF_PROP (12% crate-dig chance).
     * Sell price: 6 COIN to Clive; 12 COIN to Trevor if it matches his genre.
     * Broadcast on TRANSMITTER_PROP draws +6 LISTENER NPCs and grants Vibes +2 town-wide.
     * Fence value: 4 COIN.
     */
    RARE_PRESSING("Rare Pressing"),

    /**
     * FAKE_RARE_LABEL — a forged rare-pressing label, crafted from PRINTER_PAPER + INK_BOTTLE
     * at the InternetCafe. Apply to a standard VINYL_RECORD to pass it off as a RARE_PRESSING.
     * Clive catches it 35% of the time (Notoriety +6, CrimeType.FRAUD, banned for the day).
     * Trevor catches it 50% of the time (HOSTILE, TRADING_SCAM rumour, skips 3 Wednesdays).
     * Successfully fool Clive 3 times → CLIVE_KNOWS_NOTHING achievement.
     */
    FAKE_RARE_LABEL("Fake Rare Label"),

    // ── Issue #1471: Northfield Closing-Down Sale — Dave's Everything Must Go ──

    /**
     * KNOCKOFF_ELECTRONIC — a generic knockoff electronic item (hairdryer, extension
     * lead, phone charger, telly, laptop, or kettle) sold at Dave's perpetual
     * closing-down sale at "70% off" prices that are actually above RRP.
     * Fence value: 1–4 COIN. Can be used as a component in crafting recipes
     * (e.g. 3× KNOCKOFF_ELECTRONIC → 1 DISTRACTION_DEVICE at a workbench).
     * The KNOCKOFF_LAPTOP variant can be traded to INTERNET_CAFE_OWNER for
     * INTERNET_CAFE_DISCOUNT_CARD (2 free internet sessions).
     */
    KNOCKOFF_ELECTRONIC("Knockoff Electronic"),

    /**
     * BRAND_NAME_TELLY — the one item in Dave's life that is actually worth
     * something: a genuine brand-name television accidentally stocked in the
     * liquidation delivery. Fence value: 20 COIN. Can be placed as a decorative
     * prop in a player-owned squat, raising Squat Comfort by 5.
     */
    BRAND_NAME_TELLY("Brand Name Telly"),

    // ── Issue #1475: Northfield Rare Bird Alert ───────────────────────────────

    /**
     * BINOCULARS — 8x zoom binoculars, purchased from CANAL_TWITCHER (Terry) for 8 COIN,
     * or found in a SKIP_PROP during the parakeet event (15% chance).
     * Required to confirm the parakeet sighting and offer guided tours.
     * Tooltip: "8x zoom. Smells of a stranger's face."
     */
    BINOCULARS("Binoculars"),

    /**
     * BIRD_PHOTO — a photograph of the ring-necked parakeet taken at PARAKEET_TREE_PROP.
     * Produced by holding E for 3 seconds with BINOCULARS at the tree.
     * Sell to JOURNALIST for 12 COIN (triggers newspaper headline) or to Terry for 5 COIN
     * + free BIRDWATCHING_TIP items thereafter. Achievement: NATURE_CORRESPONDENT on first sale.
     * Tooltip: "Bright green, screaming. Perfect shot."
     */
    BIRD_PHOTO("Bird Photo"),

    /**
     * FAKE_BIRD_PHOTO — a photocopied forgery of a BIRD_PHOTO, made at the photocopier.
     * 30% detection risk (CrimeType.FRAUD). Achievement: PRESS_FABRICATOR on first successful use.
     * Tooltip: "Definitely a parakeet. Ignore the staple marks."
     */
    FAKE_BIRD_PHOTO("Fake Bird Photo"),

    /**
     * BIRD_GUIDE_BOOK — a small pocket bird-identification guide carried by TWITCHER NPCs.
     * Dropped on pickpocket; fenceable for 3 COIN.
     * Tooltip: "Collins Bird Guide, 2004 edition. Page 47 is stuck together."
     */
    BIRD_GUIDE_BOOK("Bird Guide Book"),

    /**
     * FAKE_BIRDWATCHING_TIP — a forged birdwatching note, crafted from BIRDWATCHING_TIP + MARKER_PEN.
     * Sells for 3 COIN (same as real tip) but the deceived twitcher returns hostile after 60 seconds,
     * shouting "That was completely wrong! I could've missed it!" Seeds DODGY_TIPSTER rumour.
     * Achievement: GOOSE_CHASE on first successful fake tip sale.
     * Tooltip: "Completely wrong location. Confidently written."
     */
    FAKE_BIRDWATCHING_TIP("Fake Birdwatching Tip"),

    // ── Issue #1477: Northfield Warm Hub ──────────────────────────────────────

    /**
     * HUB_TEA — a free hot drink dispensed by Shirley at the warm hub.
     * Obtained by pressing E on WARM_HUB_VOLUNTEER (WARM_HUB_TABLE_PROP) once per 120 seconds.
     * Restores +25 warmth and +8 hunger. Can be used to stage a fake medical emergency
     * on a WARM_HUB_VISITOR (hold E for 2s, 40% detection chance).
     * Tooltip: "Free tea from Shirley. Tastes of powdered milk and kindness."
     */
    HUB_TEA("Hub Tea"),

    /**
     * HUB_BISCUIT — a rich tea biscuit from Shirley's tin at the warm hub.
     * Player may take 1 free per visit. Taking 2+ triggers Shirley (Notoriety +1 each).
     * Stealing the whole tin (4s unobserved hold) yields 3–6 biscuits + DONATIONS_TIN.
     * Tooltip: "Rich tea. Shirley made you take it."
     */
    HUB_BISCUIT("Hub Biscuit"),

    /**
     * DONATIONS_TIN — the empty tin left after the biscuits are stolen, or yielded
     * when the DONATIONS_TIN_PROP is robbed. Also produced when the biscuit tin is
     * looted (3–6 HUB_BISCUIT items + 1 DONATIONS_TIN).
     * Fenceable for 1 COIN. Tooltip: "Empty tin. Someone got there first."
     */
    DONATIONS_TIN("Donations Tin"),

    /**
     * FAKE_DONATIONS_FORM — crafted from BLANK_PRESCRIPTION_FORM + PEN.
     * Shown to Shirley at the warm hub to trick her into handing over the contents of
     * the DONATIONS_TIN_PROP (no crime recorded, Notoriety +3).
     * Achievement: SHIRL_S_BEEN_HAD on first successful use.
     * Tooltip: "Looks official. Shirley won't know the difference."
     */
    FAKE_DONATIONS_FORM("Fake Donations Form"),

    /**
     * HOMEMADE_CAKE_SLICE — a slice of homemade cake given by Shirley as a reward
     * for escorting all lingering rough sleepers out of the warm hub within 90 seconds
     * of closure. Restores +20 hunger and +10 warmth.
     * Achievement: SHIRLEY_S_FAVOURITE on receipt.
     * Tooltip: "Victoria sponge. Shirley says it's her mum's recipe."
     */
    HOMEMADE_CAKE_SLICE("Homemade Cake Slice"),

    // ── Issue #1479: Northfield Public Defibrillator ───────────────────────────

    /**
     * DEFIBRILLATOR_UNIT — the portable defibrillator from the community cabinet.
     * Retrieved by entering the correct code (1984) on DEFIBRILLATOR_CABINET_PROP.
     * Used on a CARDIAC_VICTIM (6-second hold): Notoriety −10, NeighbourhoodVibes +5.
     * If cable is missing: use time ×3, 25% chance of total failure.
     * Tooltip: "Northfield Community Defibrillator. Phil registered it. Phil moved to Stoke."
     */
    DEFIBRILLATOR_UNIT("Defibrillator Unit"),

    /**
     * COPPER_CABLE — the internal copper wiring from the defibrillator cabinet.
     * Yields 3 per cabinet opening. Scrap value 4 COIN each at the scrapyard.
     * Can be crafted into REWIRED_EXTENSION_LEAD (car boot value 8 COIN).
     * Taking it adds CrimeType.CABINET_THEFT and Notoriety +5.
     * If cable is missing when a cardiac event fires, defibrillator use time triples
     * and there is a 25% chance of total failure.
     * Tooltip: "Copper cable. Technically it belongs to the defibrillator."
     */
    COPPER_CABLE("Copper Cable"),

    /**
     * CPR_TRAINING_FLYER — crafted from BLANK_PAPER + MARKER_PEN.
     * Pin to community centre notice board to schedule a CPR training session.
     * The following evening 3–6 CPR_STUDENT NPCs will arrive and pay 5 COIN each.
     * Tooltip: "Learn CPR! Community Centre, 7pm. Bring £5. Cert included."
     */
    CPR_TRAINING_FLYER("CPR Training Flyer"),

    /**
     * CPR_CERTIFICATE — awarded to each CPR_STUDENT who completes the session.
     * Player receives one per student on successful course completion.
     * Achievement: COMMUNITY_FIRST_AIDER on first completion.
     * Tooltip: "Certified First Aider (CPR). Signed by the instructor."
     */
    CPR_CERTIFICATE("CPR Certificate"),

    /**
     * REWIRED_EXTENSION_LEAD — crafted from COPPER_CABLE.
     * Looks legitimate; sells for 8 COIN at car boot sales.
     * If inspected, reveals it was wired from a public defibrillator cabinet.
     * Tooltip: "Extension lead. Very well-made, actually."
     */
    REWIRED_EXTENSION_LEAD("Rewired Extension Lead");

    private final String displayName;

    Material(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the icon color(s) for this material in the inventory UI.
     * Returns an array of 1 or 2 colors. A single color fills the whole slot;
     * two colors split the slot diagonally to represent materials with distinct
     * top/side appearance (e.g. grass has green top, brown side).
     */
    public Color[] getIconColors() {
        switch (this) {
            // Block materials — colors match BlockType
            case WOOD:           return c(0.72f, 0.52f, 0.28f);   // Warm pine
            case BRICK:          return c(0.72f, 0.28f, 0.18f);   // Terracotta red
            case BRICK_WALL:     return c(0.72f, 0.28f, 0.18f);
            case GLASS:          return c(0.55f, 0.78f, 0.92f, 0.7f); // Light blue
            case WINDOW:         return c(0.55f, 0.78f, 0.92f, 0.7f);
            case STONE:          return c(0.52f, 0.52f, 0.50f);   // Cool grey
            case GRASS_TURF:     return cs(0.28f, 0.68f, 0.18f,   // Green top
                                           0.45f, 0.35f, 0.20f);  // Brown side
            case DIRT:           return c(0.55f, 0.38f, 0.18f);   // Warm brown
            case PAVEMENT_SLAB:  return c(0.68f, 0.68f, 0.65f);   // Light grey
            case ROAD_ASPHALT:   return c(0.25f, 0.25f, 0.27f);   // Dark asphalt
            case PLANKS:         return c(0.72f, 0.52f, 0.28f);   // Same as wood
            case CARDBOARD:      return c(0.75f, 0.62f, 0.38f);   // Tan
            case CARDBOARD_BOX:  return c(0.75f, 0.62f, 0.38f);
            case CONCRETE:       return c(0.62f, 0.62f, 0.58f);   // Cool grey
            case ROOF_TILE:      return c(0.60f, 0.18f, 0.12f);   // Deep terracotta
            case TARMAC:         return c(0.18f, 0.18f, 0.18f);   // Very dark
            case SCRAP_METAL:    return c(0.48f, 0.52f, 0.55f);   // Blue-grey steel
            case RENDER:         return c(0.92f, 0.90f, 0.86f);   // White render
            case RENDER_CREAM:   return c(0.88f, 0.82f, 0.62f);   // Cream
            case RENDER_PINK:    return c(0.88f, 0.65f, 0.70f);   // Pink
            case SLATE:          return c(0.30f, 0.32f, 0.38f);   // Dark blue-grey
            case PEBBLEDASH:     return c(0.72f, 0.70f, 0.62f);   // Sandy
            case DOOR:           return c(0.42f, 0.25f, 0.10f);   // Dark oak
            case LINOLEUM:       return c(0.58f, 0.52f, 0.42f);   // Beige
            case LINO_GREEN:     return c(0.35f, 0.52f, 0.30f);   // Hospital green
            case YELLOW_BRICK:   return c(0.82f, 0.72f, 0.35f);   // London stock
            case TILE:           return c(0.92f, 0.92f, 0.90f);   // White tile
            case TILE_BLACK:     return c(0.12f, 0.12f, 0.12f);   // Black tile
            case COUNTER:        return c(0.62f, 0.52f, 0.38f);   // Laminate
            case SHELF:          return c(0.52f, 0.38f, 0.22f);   // Plywood
            case TABLE:          return c(0.48f, 0.32f, 0.18f);   // Dark stained
            case CARPET:         return c(0.52f, 0.18f, 0.18f);   // Burgundy
            case FENCE:          return c(0.18f, 0.18f, 0.20f);   // Iron black
            case SIGN:           return c(0.95f, 0.95f, 0.92f);
            case SIGN_RED:       return c(0.88f, 0.10f, 0.10f);
            case SIGN_BLUE:      return c(0.10f, 0.18f, 0.68f);
            case SIGN_GREEN:     return c(0.10f, 0.58f, 0.18f);
            case SIGN_YELLOW:    return c(0.92f, 0.82f, 0.12f);
            case GARDEN_WALL:    return c(0.58f, 0.50f, 0.42f);   // Sandstone
            case BOOKSHELF:      return c(0.38f, 0.28f, 0.12f);   // Dark walnut
            case METAL_RED:      return c(0.78f, 0.12f, 0.08f);   // Pillar box red
            case SHELTER_WALL:   return c(0.52f, 0.52f, 0.50f);
            case SHELTER_FLOOR:  return c(0.68f, 0.68f, 0.65f);
            case SHELTER_ROOF:   return c(0.48f, 0.52f, 0.55f);
            case PLYWOOD:        return c(0.80f, 0.68f, 0.40f);   // Light plywood
            case PIPE:           return c(0.55f, 0.55f, 0.60f);   // Metal pipe
            case STAIRS:         return cs(0.70f, 0.55f, 0.28f,   // Light tread top
                                           0.60f, 0.45f, 0.22f);  // Darker riser side
            case LADDER:         return cs(0.58f, 0.40f, 0.18f,   // Rung front
                                           0.45f, 0.30f, 0.12f);  // Darker rail
            case HALF_BLOCK:     return cs(0.68f, 0.68f, 0.65f,   // Lighter slab top
                                           0.62f, 0.62f, 0.58f);  // Concrete side

            // Currency
            case SHILLING:       return cs(0.78f, 0.72f, 0.35f,   // Silver-gold shilling
                                           0.60f, 0.55f, 0.22f);
            case PENNY:          return cs(0.72f, 0.38f, 0.15f,   // Copper penny
                                           0.55f, 0.28f, 0.08f);

            // Diamond — cyan/white sparkle
            case DIAMOND:        return cs(0.65f, 0.95f, 1.00f,
                                           0.40f, 0.85f, 0.90f);

            // Office items
            case COMPUTER:       return c(0.22f, 0.22f, 0.28f);   // Dark grey monitor
            case OFFICE_CHAIR:   return c(0.15f, 0.15f, 0.15f);   // Black chair
            case STAPLER:        return c(0.20f, 0.20f, 0.60f);   // Blue stapler

            // Food items
            case SAUSAGE_ROLL:   return c(0.80f, 0.55f, 0.20f);   // Golden pastry
            case STEAK_BAKE:     return c(0.75f, 0.42f, 0.15f);   // Darker pastry
            case CHIPS:          return c(0.90f, 0.78f, 0.30f);   // Golden chips
            case KEBAB:          return c(0.72f, 0.32f, 0.15f);   // Meat brown
            case CRISPS:         return c(0.88f, 0.78f, 0.20f);   // Yellow packet
            case TIN_OF_BEANS:   return cs(0.85f, 0.18f, 0.18f,   // Red tin
                                            0.92f, 0.60f, 0.15f); // Orange beans
            case ENERGY_DRINK:   return c(0.15f, 0.85f, 0.30f);   // Bright green can
            case PINT:           return c(0.78f, 0.62f, 0.18f);   // Golden ale
            case PERI_PERI_CHICKEN: return c(0.85f, 0.38f, 0.12f); // Spicy orange

            // Shop goods
            case SCRATCH_CARD:   return c(0.88f, 0.82f, 0.15f);   // Gold card
            case NEWSPAPER:      return c(0.88f, 0.84f, 0.72f);   // Newsprint grey
            case WASHING_POWDER: return c(0.70f, 0.78f, 0.95f);   // Blue box
            case PARACETAMOL:    return c(0.95f, 0.95f, 0.95f);   // White packet
            case TEXTBOOK:       return c(0.20f, 0.38f, 0.68f);   // Blue textbook
            case HYMN_BOOK:      return c(0.18f, 0.18f, 0.52f);   // Dark blue
            case PETROL_CAN:     return c(0.82f, 0.30f, 0.15f);   // Red can
            case PETROL_CAN_FULL: return c(0.82f, 0.30f, 0.15f);  // Red can with yellow stripe
            // Issue #1049: Chippy
            case BREAD:          return c(0.92f, 0.88f, 0.72f);  // Pale cream loaf
            case COLD_CHIPS:     return c(0.78f, 0.70f, 0.28f);  // Dull yellow, cold
            case COOKING_OIL:    return cs(0.92f, 0.82f, 0.30f,  // Golden oil
                                          0.70f, 0.60f, 0.20f);  // Darker base
            case FIRE_STARTER:   return c(0.90f, 0.40f, 0.10f);  // Orange-red
            case MOLOTOV:        return cs(0.55f, 0.78f, 0.95f,  // Glass bottle blue
                                          0.90f, 0.40f, 0.10f);  // Rag flame
            case LARD_BUCKET:    return c(0.88f, 0.85f, 0.72f);  // Pale greasy white

            // Issue #1053: Ladbrokes BettingShopSystem
            case BETTING_SLIP_BLANK: return c(0.95f, 0.92f, 0.80f); // Off-white paper slip
            case RACING_POST:        return c(0.88f, 0.18f, 0.18f); // Classic Racing Post red

            // Issue #1055: Northfield War Memorial — StatueSystem
            case BREAD_CRUMBS:       return c(0.88f, 0.75f, 0.50f); // Sandy crumb colour
            case ROPE:               return c(0.65f, 0.48f, 0.28f); // Natural hemp brown
            case CHAIN:              return c(0.60f, 0.60f, 0.65f); // Steel grey
            case FIREWORK:           return cs(0.95f, 0.18f, 0.10f, // Red firework body
                                               0.95f, 0.80f, 0.10f); // Gold sparkle
            case TIME_CAPSULE:       return c(0.55f, 0.45f, 0.25f); // Aged bronze tin
            case POPPY:              return cs(0.90f, 0.10f, 0.10f, // Poppy red
                                               0.08f, 0.08f, 0.08f); // Black centre
            case CLEANING_SUPPLIES:  return c(0.35f, 0.65f, 0.90f); // Council-issue blue bottle
            case PLACARD:            return c(0.92f, 0.88f, 0.72f); // Cardboard/pale sign
            case MEMBERSHIP_CARD:    return c(0.18f, 0.30f, 0.62f); // Club blue card
            case PACK_OF_NUTS:       return c(0.75f, 0.55f, 0.18f); // Golden-yellow packet
            case PORK_SCRATCHINGS:   return c(0.72f, 0.45f, 0.18f); // Warm brown/crispy
            case RAFFLE_TICKET:      return c(0.88f, 0.20f, 0.20f); // Red raffle stub
            case MEAT_RAFFLE_PRIZE:  return c(0.88f, 0.88f, 0.92f); // Frozen/white chicken bag
            case LOAN_LEAFLET:       return c(0.98f, 0.85f, 0.10f); // Fluorescent yellow flyer
            case THREATENING_LETTER: return c(0.92f, 0.92f, 0.92f); // Pale grey demand letter
            case DEBT_ADVICE_LETTER: return c(0.75f, 0.88f, 0.72f); // Calm green CAB leaflet
            case HAIR_CLIPPERS:         return c(0.35f, 0.35f, 0.38f);   // Silver-grey
            case HAIR_CLIPPERS_BROKEN:  return c(0.20f, 0.20f, 0.22f);   // Dark grey, broken
            case NAIL_POLISH:    return c(0.92f, 0.18f, 0.55f);   // Hot pink
            case BROKEN_PHONE:   return c(0.18f, 0.18f, 0.22f);   // Dark screen
            case DODGY_DVD:      return c(0.62f, 0.62f, 0.72f);   // Silver disc
            case FIRE_EXTINGUISHER: return c(0.82f, 0.12f, 0.10f); // Red cylinder
            case ANTIDEPRESSANTS: return c(0.85f, 0.85f, 0.92f);  // White/pale blue

            // Tools
            case IMPROVISED_TOOL: return cs(0.55f, 0.38f, 0.18f,  // Wood handle
                                             0.52f, 0.52f, 0.50f); // Stone head
            case STONE_TOOL:     return cs(0.52f, 0.52f, 0.50f,   // Stone head
                                            0.45f, 0.35f, 0.20f); // Wood handle

            // Fence stock items
            case HIGH_VIS_JACKET: return c(0.95f, 0.75f, 0.05f);  // Bright yellow
            case CROWBAR:         return c(0.25f, 0.25f, 0.30f);  // Dark steel
            case ANGLE_GRINDER:   return c(0.70f, 0.10f, 0.10f);  // Red power tool body
            case BALACLAVA:       return c(0.10f, 0.10f, 0.10f);  // Black
            case BOLT_CUTTERS:    return cs(0.30f, 0.30f, 0.35f,  // Dark metal handles
                                            0.55f, 0.50f, 0.15f); // Orange grip
            case DODGY_PASTY:     return c(0.78f, 0.52f, 0.22f);  // Golden pastry
            case FOOD:            return c(0.88f, 0.62f, 0.18f);  // Generic food orange

            // Underground minerals
            case COAL:            return c(0.15f, 0.15f, 0.15f);  // Black coal chunk
            case IRON:            return cs(0.55f, 0.40f, 0.28f,  // Rusty brown ore
                                            0.35f, 0.35f, 0.38f); // Dark grey metal
            case FLINT:           return c(0.20f, 0.22f, 0.25f);  // Very dark blue-grey flint
            case COIN:            return cs(0.85f, 0.72f, 0.20f,  // Gold coin face
                                            0.65f, 0.52f, 0.10f); // Darker gold edge

            // Clothing items (Issue #698)
            case COAT:            return c(0.22f, 0.32f, 0.48f);  // Dark blue coat
            case UMBRELLA:        return cs(0.18f, 0.28f, 0.65f,  // Blue canopy
                                            0.35f, 0.28f, 0.18f); // Brown handle
            case WOOLLY_HAT:      return c(0.72f, 0.18f, 0.18f);  // Red woolly hat
            case FLASK_OF_TEA:    return cs(0.62f, 0.52f, 0.38f,  // Metal flask body
                                            0.88f, 0.72f, 0.32f); // Tea-coloured cap

            // Heist tools & loot (Issue #704)
            case GLASS_CUTTER:   return cs(0.65f, 0.95f, 1.00f,  // Diamond tip cyan
                                            0.58f, 0.40f, 0.18f); // Wood handle
            case ROPE_LADDER:    return cs(0.58f, 0.40f, 0.18f,  // Rope brown
                                            0.45f, 0.30f, 0.12f); // Darker rope
            case GOLD_RING:      return cs(0.95f, 0.80f, 0.15f,  // Gold ring face
                                            0.75f, 0.60f, 0.08f); // Darker gold edge
            case COUNCIL_ID:     return c(0.20f, 0.38f, 0.68f);  // Official blue card
            case PASTY:          return c(0.82f, 0.60f, 0.25f);  // Greggs golden pastry
            case LEAVES:         return c(0.28f, 0.62f, 0.18f);  // Green leaf bundle

            // Slumlord items (Issue #712)
            case DEED:           return cs(0.92f, 0.88f, 0.62f,  // Parchment front
                                            0.72f, 0.65f, 0.35f); // Darker back
            case PAINT_TIN:      return cs(0.82f, 0.18f, 0.18f,  // Red tin
                                            0.62f, 0.62f, 0.65f); // Grey lid
            case EVICTION_NOTICE: return c(0.88f, 0.12f, 0.12f); // Bold red paper

            // Squat system items (Issue #714)
            case BARRICADE:      return cs(0.45f, 0.30f, 0.15f,  // Rough planks
                                            0.35f, 0.22f, 0.10f); // Darker wood
            case LOCKPICK:       return cs(0.55f, 0.55f, 0.60f,  // Silver metal
                                            0.40f, 0.40f, 0.45f); // Darker grip
            case FAKE_ID:        return c(0.20f, 0.38f, 0.68f);  // Official-looking blue card

            // Underground music scene (Issue #716)
            case MICROPHONE:     return cs(0.30f, 0.30f, 0.35f,  // Dark metal body
                                            0.18f, 0.18f, 0.20f); // Darker grille
            case FLYER:          return cs(0.92f, 0.10f, 0.55f,  // Hot pink flyer
                                            0.88f, 0.82f, 0.15f); // Yellow accent

            // Witness & Evidence items (Issue #765)
            case RUMOUR_NOTE:        return cs(0.88f, 0.84f, 0.72f,  // Newsprint/paper
                                               0.55f, 0.38f, 0.18f); // Brown ink
            case CCTV_TAPE:          return cs(0.15f, 0.15f, 0.18f,  // Black cassette body
                                               0.85f, 0.72f, 0.20f); // Gold label

            // Disguise items (Issue #767)
            case POLICE_UNIFORM:     return cs(0.10f, 0.18f, 0.55f,  // Police navy
                                               0.08f, 0.12f, 0.40f); // Darker navy
            case COUNCIL_JACKET:     return cs(0.95f, 0.80f, 0.05f,  // Council hi-vis yellow
                                               0.15f, 0.15f, 0.15f); // Dark logo
            case MARCHETTI_TRACKSUIT: return cs(0.80f, 0.10f, 0.10f, // Marchetti red
                                                0.55f, 0.05f, 0.05f); // Darker red stripe
            case STREET_LADS_HOODIE: return cs(0.10f, 0.55f, 0.10f,  // Street Lads green
                                               0.06f, 0.35f, 0.06f); // Darker green
            case HI_VIS_VEST:        return cs(0.95f, 0.78f, 0.02f,  // Bright yellow
                                               0.88f, 0.88f, 0.88f); // Silver reflective strip
            case GREGGS_APRON:       return cs(0.82f, 0.10f, 0.15f,  // Greggs red
                                               0.15f, 0.30f, 0.70f); // Blue Greggs logo

            // Issue #769: Black Market Economy items
            case GREGGS_PASTRY:      return c(0.88f, 0.68f, 0.22f);  // Golden pastry
            case PLASTIC_BAG:        return c(0.85f, 0.92f, 0.95f);  // Translucent white bag
            case CAN_OF_LAGER:       return cs(0.88f, 0.75f, 0.10f,  // Gold lager label
                                               0.55f, 0.55f, 0.60f); // Silver can
            case CIGARETTE:          return cs(0.95f, 0.95f, 0.90f,  // White paper
                                               0.78f, 0.35f, 0.15f); // Orange filter
            case WOOLLY_HAT_ECONOMY: return c(0.62f, 0.18f, 0.62f);  // Purple woolly hat
            case SLEEPING_BAG:       return cs(0.22f, 0.45f, 0.68f,  // Blue outer
                                               0.65f, 0.65f, 0.68f); // Grey inner
            case STOLEN_PHONE:       return cs(0.12f, 0.12f, 0.15f,  // Dark screen
                                               0.85f, 0.35f, 0.12f); // Orange stolen indicator
            case PRESCRIPTION_MEDS:  return cs(0.88f, 0.88f, 0.95f,  // White packet
                                               0.25f, 0.42f, 0.78f); // Blue cross
            case COUNTERFEIT_NOTE:   return cs(0.55f, 0.72f, 0.35f,  // Fake green note
                                               0.35f, 0.52f, 0.20f); // Darker fake green
            case TOBACCO_POUCH:      return cs(0.52f, 0.35f, 0.18f,  // Brown leather
                                               0.35f, 0.22f, 0.08f); // Dark brown

            // Issue #783: Pirate FM materials
            case WIRE:              return cs(0.55f, 0.55f, 0.60f,  // Silver wire
                                              0.38f, 0.38f, 0.42f); // Darker insulation
            case BROADCAST_TAPE:    return cs(0.15f, 0.15f, 0.18f,  // Black cassette body
                                              0.88f, 0.12f, 0.12f); // Red label
            case TRANSMITTER_ITEM:  return cs(0.20f, 0.20f, 0.25f,  // Dark metal chassis
                                              0.88f, 0.50f, 0.05f); // Orange antenna light

            // Issue #781: Graffiti materials
            case SPRAY_CAN_EMPTY:   return c(0.65f, 0.65f, 0.65f);  // Grey empty can
            case SPRAY_CAN:         return cs(0.85f, 0.18f, 0.18f,  // Red cap
                                              0.72f, 0.72f, 0.72f); // Silver body
            case PAINT_PIGMENT_RED:  return c(0.88f, 0.12f, 0.12f); // Vivid red
            case PAINT_PIGMENT_BLUE: return c(0.12f, 0.22f, 0.88f); // Vivid blue
            case PAINT_PIGMENT_GOLD: return c(0.88f, 0.72f, 0.10f); // Gold
            case PAINT_PIGMENT_WHITE:return c(0.95f, 0.95f, 0.95f); // White
            case PAINT_PIGMENT_GREY: return c(0.55f, 0.55f, 0.58f); // Mid grey

            // Craftable 3D props (Issue #720)
            case PROP_BED:           return cs(0.55f, 0.38f, 0.22f,  // Wooden frame
                                               0.88f, 0.88f, 0.88f); // White pillow
            case PROP_WORKBENCH:     return cs(0.62f, 0.45f, 0.22f,  // Plywood top
                                               0.40f, 0.30f, 0.18f); // Darker legs
            case PROP_DARTBOARD:     return cs(0.10f, 0.10f, 0.12f,  // Dark board
                                               0.88f, 0.18f, 0.18f); // Red ring
            case PROP_SPEAKER_STACK: return cs(0.15f, 0.15f, 0.18f,  // Black cabinet
                                               0.30f, 0.30f, 0.35f); // Grey grille
            case PROP_DISCO_BALL:    return cs(0.85f, 0.90f, 0.95f,  // Mirrored silver
                                               0.65f, 0.72f, 0.80f); // Darker reflection
            case PROP_DJ_DECKS:      return cs(0.15f, 0.15f, 0.18f,  // Black chassis
                                               0.55f, 0.55f, 0.60f); // Silver controls

            // Issue #785: The Dodgy Market Stall
            case STALL_FRAME:       return cs(0.55f, 0.38f, 0.18f,  // Wooden frame brown
                                              0.40f, 0.28f, 0.12f); // Darker wood
            case STALL_AWNING:      return cs(0.88f, 0.18f, 0.18f,  // Red-and-white stripe
                                              0.95f, 0.92f, 0.88f); // White stripe
            case MARKET_LICENCE:    return cs(0.88f, 0.82f, 0.45f,  // Official cream paper
                                              0.25f, 0.42f, 0.78f); // Blue council stamp
            case KNOCK_OFF_PERFUME: return cs(0.72f, 0.55f, 0.78f,  // Purple bottle
                                              0.88f, 0.82f, 0.55f); // Gold cap

            // Issue #797: Neighbourhood Watch materials
            case NEIGHBOURHOOD_NEWSLETTER: return cs(0.88f, 0.84f, 0.72f,  // Newsprint grey
                                                     0.25f, 0.55f, 0.25f); // Green community tint
            case PEACE_OFFERING:    return cs(0.80f, 0.55f, 0.20f,  // Sausage roll gold
                                              0.85f, 0.72f, 0.20f); // Coin gold

            // Issue #799: Corner Shop Economy materials
            case SHOP_KEY:          return cs(0.75f, 0.65f, 0.10f,  // Brass key
                                              0.55f, 0.45f, 0.08f); // Darker brass
            case CIDER:             return cs(0.88f, 0.72f, 0.18f,  // Golden cider
                                              0.72f, 0.55f, 0.12f); // Darker gold
            case TOBACCO:           return cs(0.52f, 0.35f, 0.18f,  // Brown tobacco
                                              0.38f, 0.22f, 0.08f); // Dark brown

            // Issue #801: Underground Fight Night materials
            case FIGHT_CARD:        return cs(0.92f, 0.88f, 0.30f,  // Yellow paper flyer
                                              0.78f, 0.18f, 0.18f); // Red print
            case CHAMPIONSHIP_BELT: return cs(0.95f, 0.82f, 0.15f,  // Gold belt
                                              0.78f, 0.60f, 0.08f); // Darker gold
            case MOUTH_GUARD:       return cs(0.22f, 0.68f, 0.88f,  // Blue mouthguard
                                              0.18f, 0.55f, 0.72f); // Darker blue
            case RUBBER:            return c(0.18f, 0.18f, 0.18f);  // Black rubber

            // Issue #901: Bista Village Portal
            case BISTA_VILLAGE_PORTAL: return cs(0.55f, 0.10f, 0.75f,  // Purple/violet portal shimmer
                                                  0.85f, 0.70f, 0.20f); // Gold runes

            // Issue #906: Busking System
            case BUCKET_DRUM:    return cs(0.20f, 0.20f, 0.22f,  // Dark grey bucket body
                                           0.48f, 0.52f, 0.55f); // Blue-grey metal rim

            // Issue #908: Bookies Horse Racing System
            case BET_SLIP:       return cs(0.92f, 0.88f, 0.72f,  // Cream/off-white paper
                                           0.22f, 0.55f, 0.22f); // Green bookies print

            // Issue #914: Allotment System
            case POTATO_SEED:    return c(0.72f, 0.52f, 0.22f);  // Brown seed
            case CARROT_SEED:    return c(0.72f, 0.52f, 0.22f);  // Brown seed
            case CABBAGE_SEED:   return c(0.68f, 0.52f, 0.22f);  // Brown seed
            case SUNFLOWER_SEED: return c(0.78f, 0.62f, 0.18f);  // Yellow-brown seed
            case POTATO:         return cs(0.72f, 0.60f, 0.28f,  // Potato skin beige
                                           0.60f, 0.50f, 0.20f); // Darker side
            case CARROT:         return cs(0.92f, 0.50f, 0.10f,  // Bright orange
                                           0.75f, 0.38f, 0.05f); // Darker orange
            case CABBAGE:        return cs(0.30f, 0.62f, 0.20f,  // Leafy green
                                           0.22f, 0.50f, 0.15f); // Darker green
            case SUNFLOWER:      return cs(0.95f, 0.82f, 0.12f,  // Yellow petals
                                           0.55f, 0.35f, 0.08f); // Brown centre
            case PLOT_DEED:      return cs(0.88f, 0.82f, 0.55f,  // Parchment
                                           0.25f, 0.55f, 0.25f); // Green council stamp

            // Issue #918: Bus Stop & Public Transport System
            case BUS_PASS:       return cs(0.10f, 0.38f, 0.72f,  // TfL-style blue card
                                           0.88f, 0.75f, 0.10f); // Yellow accent stripe
            case INSPECTOR_BADGE: return cs(0.82f, 0.72f, 0.15f, // Gold badge face
                                            0.62f, 0.52f, 0.08f); // Darker gold edge

            // Issue #922: Skate Park System
            case SKATEBOARD:     return cs(0.35f, 0.22f, 0.08f,  // Dark maple deck
                                           0.20f, 0.20f, 0.22f); // Black trucks

            // Issue #924: Launderette System
            case CLEAN_CLOTHES:  return cs(0.92f, 0.95f, 1.00f,  // White-blue fresh fabric
                                           0.75f, 0.82f, 0.92f); // Pale blue fold
            case BLOODY_HOODIE:  return cs(0.15f, 0.10f, 0.10f,  // Dark hoodie body
                                           0.72f, 0.08f, 0.08f); // Blood red stain
            case STOLEN_JACKET:  return cs(0.18f, 0.25f, 0.18f,  // Dark olive jacket
                                           0.35f, 0.20f, 0.10f); // Brown lining

            // Issue #926: Chippy System
            case BATTERED_SAUSAGE: return cs(0.72f, 0.50f, 0.18f,  // Battered coating golden
                                              0.58f, 0.22f, 0.10f); // Sausage brown
            case CHIP_BUTTY:     return cs(0.92f, 0.88f, 0.72f,  // White bread
                                            0.90f, 0.78f, 0.30f); // Golden chips
            case MUSHY_PEAS:     return c(0.38f, 0.62f, 0.25f);  // Muted green
            case PICKLED_EGG:    return cs(0.95f, 0.95f, 0.88f,  // Pale egg white
                                            0.62f, 0.58f, 0.35f); // Vinegar brown
            case FISH_SUPPER:    return cs(0.80f, 0.65f, 0.25f,  // Golden batter
                                            0.88f, 0.78f, 0.30f); // Chip yellow
            case SALT_AND_VINEGAR_PACKET: return cs(0.20f, 0.22f, 0.72f, // Blue packet
                                                     0.92f, 0.88f, 0.10f); // Yellow text
            case CHIPS_SEASONED: return cs(0.90f, 0.78f, 0.30f,  // Golden chips
                                            0.72f, 0.62f, 0.20f); // Darker seasoned
            case BOTTLE_OF_WATER: return cs(0.62f, 0.82f, 0.95f, // Clear blue water
                                             0.88f, 0.95f, 1.00f); // Lighter highlight
            case WHITE_PAPER:    return c(0.96f, 0.96f, 0.94f);  // Off-white paper

            // Issue #928: Public Library System
            case DIY_MANUAL:     return cs(0.78f, 0.52f, 0.22f,  // Orange/ochre cover
                                           0.62f, 0.38f, 0.10f); // Darker spine
            case NEGOTIATION_BOOK: return cs(0.18f, 0.42f, 0.72f, // Blue business cover
                                             0.12f, 0.28f, 0.55f); // Darker spine
            case STREET_LAW_PAMPHLET: return cs(0.88f, 0.18f, 0.18f, // Red pamphlet cover
                                                0.65f, 0.10f, 0.10f); // Darker red

            // Issue #932: Ice Cream Van System
            case NINETY_NINE_FLAKE: return cs(0.98f, 0.95f, 0.88f, // Cream cone
                                           0.42f, 0.28f, 0.10f); // Chocolate flake
            case LOLLY:          return c(0.90f, 0.25f, 0.45f);  // Pink lolly
            case WAFER_TUBS:     return cs(0.98f, 0.95f, 0.88f, // Cream top
                                           0.92f, 0.82f, 0.60f); // Wafer side
            case BANANA_SPLIT:   return c(0.95f, 0.88f, 0.20f);  // Banana yellow
            case FLAKE_99_WITH_SAUCE: return cs(0.98f, 0.95f, 0.88f, // Cream
                                               0.92f, 0.18f, 0.28f); // Strawberry sauce
            case ICE_CREAM_99:   return cs(0.98f, 0.95f, 0.88f,  // Cream cone
                                           0.72f, 0.52f, 0.28f); // Wafer brown
            case SCREWBALL:      return c(0.92f, 0.55f, 0.78f);  // Pink spiral
            case FAB_LOLLY:      return cs(0.92f, 0.18f, 0.28f,  // Strawberry red top
                                           0.88f, 0.72f, 0.18f); // Yellow bottom
            case CHOC_ICE:       return cs(0.28f, 0.18f, 0.10f,  // Dark chocolate coat
                                           0.98f, 0.95f, 0.88f); // Cream inside
            case OYSTER_CARD_LOLLY: return c(0.12f, 0.42f, 0.72f); // Oyster card blue
            case BUBBLEGUM:      return c(0.95f, 0.40f, 0.70f);  // Bubblegum pink

            // Issue #934: Pigeon Racing System
            case RACING_PIGEON:  return cs(0.62f, 0.60f, 0.65f,  // Grey-blue pigeon
                                           0.92f, 0.75f, 0.55f); // Orange ring on foot
            case BREAD_CRUST:    return c(0.82f, 0.70f, 0.42f);  // Stale crust tan
            case PIGEON_TROPHY:  return cs(0.88f, 0.72f, 0.10f,  // Gold trophy
                                           0.75f, 0.58f, 0.05f); // Darker gold base

            // Issue #936: Council Skip & Bulky Item Day
            case OLD_SOFA:           return cs(0.55f, 0.35f, 0.20f,  // Brown fabric
                                               0.40f, 0.25f, 0.12f); // Darker wood legs
            case BROKEN_TELLY:       return cs(0.15f, 0.15f, 0.18f,  // Black plastic
                                               0.35f, 0.35f, 0.40f); // Grey screen
            case WONKY_CHAIR:        return cs(0.50f, 0.32f, 0.15f,  // Wood brown
                                               0.38f, 0.22f, 0.08f); // Darker legs
            case CARPET_ROLL:        return cs(0.52f, 0.18f, 0.18f,  // Burgundy carpet
                                               0.38f, 0.12f, 0.12f); // Darker roll
            case OLD_MATTRESS:       return cs(0.78f, 0.72f, 0.58f,  // Cream/beige ticking
                                               0.60f, 0.52f, 0.38f); // Stained yellow
            case FILING_CABINET:     return cs(0.55f, 0.58f, 0.62f,  // Grey metal
                                               0.40f, 0.42f, 0.48f); // Darker metal
            case EXERCISE_BIKE:      return cs(0.18f, 0.18f, 0.20f,  // Black frame
                                               0.55f, 0.55f, 0.60f); // Silver parts
            case BOX_OF_RECORDS:     return cs(0.38f, 0.28f, 0.18f,  // Cardboard box
                                               0.15f, 0.15f, 0.20f); // Dark vinyl records
            case MICROWAVE:          return cs(0.72f, 0.72f, 0.75f,  // Silver/white plastic
                                               0.20f, 0.20f, 0.22f); // Dark glass door
            case SHOPPING_TROLLEY_GOLD: return cs(0.92f, 0.75f, 0.15f, // Gold chrome frame
                                                   0.72f, 0.55f, 0.05f); // Darker gold
            case ANTIQUE_CLOCK:      return cs(0.55f, 0.40f, 0.15f,  // Dark mahogany
                                               0.88f, 0.72f, 0.15f); // Gold clock face
            case HOT_PASTRY:         return c(0.92f, 0.65f, 0.22f);  // Golden pastry
            case LUXURY_BED:         return cs(0.42f, 0.28f, 0.12f,  // Dark frame
                                               0.88f, 0.72f, 0.55f); // Cream mattress
            case DWP_LETTER:         return cs(0.88f, 0.84f, 0.72f,  // Cream paper
                                               0.18f, 0.28f, 0.65f); // Blue official text

            // Issue #938: Greasy Spoon Café
            case FULL_ENGLISH:       return cs(0.92f, 0.72f, 0.22f,  // Golden yolk
                                               0.72f, 0.22f, 0.12f); // Red tomato/beans
            case MUG_OF_TEA:         return cs(0.62f, 0.40f, 0.22f,  // Tan tea colour
                                               0.88f, 0.88f, 0.85f); // White mug
            case BEANS_ON_TOAST:     return cs(0.80f, 0.35f, 0.15f,  // Orange bean sauce
                                               0.82f, 0.65f, 0.38f); // Toast brown
            case FRIED_BREAD:        return cs(0.82f, 0.68f, 0.30f,  // Fried golden
                                               0.68f, 0.52f, 0.20f); // Darker crust
            case BACON_BUTTY:        return cs(0.80f, 0.28f, 0.18f,  // Bacon pink-red
                                               0.92f, 0.82f, 0.60f); // Buttered roll
            case BUILDER_S_TEA:      return cs(0.55f, 0.32f, 0.15f,  // Dark strong tea
                                               0.88f, 0.88f, 0.85f); // White mug
            case CHALKBOARD:         return cs(0.18f, 0.18f, 0.20f,  // Dark slate
                                               0.88f, 0.88f, 0.72f); // Chalk text cream

            // Issue #948: Hand Car Wash
            case SQUEEGEE:           return cs(0.20f, 0.50f, 0.85f,  // Blue rubber blade
                                               0.55f, 0.40f, 0.18f); // Brown wood handle

            // Issue #950: Northfield Leisure Centre
            case CHOCOLATE_BAR:      return cs(0.35f, 0.18f, 0.08f,  // Dark chocolate
                                               0.88f, 0.72f, 0.35f); // Gold foil wrapper
            case WATER_BOTTLE:       return cs(0.55f, 0.80f, 0.95f,  // Light blue bottle
                                               0.90f, 0.90f, 0.90f); // White label
            case SWIM_TRUNKS:        return cs(0.12f, 0.28f, 0.75f,  // Swimming blue
                                               0.85f, 0.85f, 0.85f); // White trim
            case CHICKEN_WINGS:      return cs(0.82f, 0.52f, 0.18f,  // Golden fried
                                               0.90f, 0.72f, 0.35f); // Spiced orange
            case CHICKEN_BOX:        return cs(0.82f, 0.52f, 0.18f,  // Golden fried
                                               0.88f, 0.35f, 0.10f); // Red box
            case CHIPS_AND_GRAVY:    return cs(0.88f, 0.72f, 0.30f,  // Chips yellow
                                               0.45f, 0.28f, 0.08f); // Dark gravy
            case FLAT_COLA:          return cs(0.20f, 0.08f, 0.04f,  // Very dark cola
                                               0.65f, 0.65f, 0.65f); // Grey can
            case CHICKEN_BONE:       return c(0.90f, 0.85f, 0.75f);  // Off-white bone
            case EMPTY_CHICKEN_BOX:  return cs(0.88f, 0.35f, 0.10f,  // Red box
                                               0.72f, 0.52f, 0.28f); // Grease-stained

            // Issue #961: Cash4Gold Pawn Shop
            case GUITAR:             return cs(0.62f, 0.38f, 0.12f,  // Warm wood body
                                               0.38f, 0.22f, 0.06f); // Darker neck

            // Issue #963: Northfield Canal
            case FISHING_ROD:        return cs(0.55f, 0.40f, 0.18f,  // Brown bamboo rod
                                               0.48f, 0.52f, 0.55f); // Grey metal reel
            case FISH_ROACH:         return cs(0.55f, 0.65f, 0.55f,  // Grey-green fish body
                                               0.80f, 0.72f, 0.45f); // Silver-gold belly
            case FISH_BREAM:         return cs(0.62f, 0.68f, 0.58f,  // Olive-grey body
                                               0.88f, 0.82f, 0.58f); // Golden-cream belly
            case FISH_PERCH:         return cs(0.28f, 0.55f, 0.28f,  // Green striped body
                                               0.92f, 0.38f, 0.18f); // Orange-red fins
            case FISH_PIKE:          return cs(0.35f, 0.52f, 0.32f,  // Dark olive-green body
                                               0.65f, 0.72f, 0.45f); // Yellow-green belly

            // Issue #965: Northfield Snooker Hall
            case CUE:                return cs(0.75f, 0.55f, 0.22f,  // Light ash wood shaft
                                               0.48f, 0.30f, 0.08f); // Darker butt end
            case CHALK_CUBE:         return c(0.28f, 0.45f, 0.72f);  // Chalk blue

            // Issue #967: Northfield Taxi Rank
            case TAXI_PASS:          return cs(0.08f, 0.55f, 0.20f,  // Taxi green card
                                               0.88f, 0.75f, 0.10f); // Yellow accent stripe
            case DODGY_PACKAGE:      return cs(0.48f, 0.38f, 0.25f,  // Brown plain packaging
                                               0.22f, 0.18f, 0.12f); // Dark tape / string

            // Issue #969: Northfield Cemetery
            case SPADE:              return cs(0.45f, 0.30f, 0.10f,  // Brown wooden handle
                                               0.55f, 0.55f, 0.60f); // Metal blade
            case WEDDING_RING:       return cs(0.95f, 0.82f, 0.20f,  // Gold band
                                               0.98f, 0.90f, 0.55f); // Lighter highlight
            case POCKET_WATCH:       return cs(0.78f, 0.68f, 0.40f,  // Tarnished gold case
                                               0.35f, 0.35f, 0.38f); // Dark face
            case CONDOLENCE_CARD:    return cs(0.92f, 0.90f, 0.88f,  // Cream card
                                               0.60f, 0.45f, 0.55f); // Mauve floral accent
            case OLD_PHOTOGRAPH:     return cs(0.85f, 0.78f, 0.65f,  // Sepia tones
                                               0.55f, 0.45f, 0.35f); // Dark border
            case OLD_COIN:           return cs(0.60f, 0.52f, 0.28f,  // Tarnished bronze face
                                               0.45f, 0.38f, 0.18f); // Dark aged rim

            // Issue #971: The Rusty Anchor Wetherspoons
            case CHEAP_SPIRITS:      return cs(0.88f, 0.78f, 0.30f,  // Amber spirits
                                               0.35f, 0.22f, 0.08f); // Dark bottle
            case CURRY_CLUB_SPECIAL: return cs(0.90f, 0.55f, 0.10f,  // Curry orange
                                               0.75f, 0.38f, 0.05f); // Rich sauce

            // Issue #973: Northfield GP Surgery
            case PRESCRIPTION:          return cs(0.88f, 0.88f, 0.95f,  // White paper slip
                                                   0.28f, 0.55f, 0.82f); // NHS blue stamp
            case ANTIBIOTICS:           return c(0.88f, 0.72f, 0.18f);   // Yellow capsule
            case STRONG_MEDS:           return cs(0.78f, 0.22f, 0.22f,   // Red pill/packet
                                                   0.55f, 0.08f, 0.08f); // Dark red warning
            case SICK_NOTE:             return cs(0.92f, 0.92f, 0.88f,   // Cream official paper
                                                   0.28f, 0.55f, 0.82f); // NHS blue header
            case BLANK_PRESCRIPTION_FORM: return cs(0.95f, 0.95f, 0.90f, // Pale cream form
                                                   0.70f, 0.70f, 0.65f); // Grey ruled lines
            case NEON_LEAFLET:          return cs(0.20f, 0.82f, 0.45f,   // Bright green NHS leaflet
                                                   0.10f, 0.62f, 0.28f); // Darker green accent

            // Issue #1116: Northfield Pharmacy
            case COLD_AND_FLU_SACHET:  return cs(0.12f, 0.55f, 0.82f,   // Blue sachet packet
                                                   0.08f, 0.38f, 0.60f); // Darker blue
            case ANTISEPTIC_CREAM:     return cs(0.92f, 0.92f, 0.88f,   // White cream tube
                                                   0.72f, 0.72f, 0.68f); // Grey cap
            case VITAMIN_C_TABLETS:    return cs(0.98f, 0.70f, 0.10f,   // Orange tablet pot
                                                   0.82f, 0.50f, 0.05f); // Darker orange
            case HAIR_DYE:             return cs(0.72f, 0.18f, 0.62f,   // Purple dye box
                                                   0.50f, 0.08f, 0.42f); // Darker purple
            case READING_GLASSES:      return cs(0.15f, 0.15f, 0.18f,   // Dark frame
                                                   0.70f, 0.85f, 0.92f); // Lens tint

            // Issue #975: Northfield Post Office
            case BENEFITS_BOOK:         return cs(0.12f, 0.38f, 0.70f,   // Royal blue DWP booklet
                                                   0.92f, 0.88f, 0.80f); // Cream page interior
            case PARCEL:                return cs(0.62f, 0.48f, 0.28f,   // Brown cardboard
                                                   0.22f, 0.18f, 0.12f); // Dark tape
            case STAMP:                 return cs(0.92f, 0.20f, 0.18f,   // Royal Mail red
                                                   0.88f, 0.88f, 0.85f); // White stamp face
            case POST_BOX_PROP:         return c(0.90f, 0.15f, 0.12f);   // Pillar box red

            // Issue #977: Northfield Amusement Arcade
            case TWOPENCE:              return cs(0.82f, 0.68f, 0.18f,   // Copper coin face
                                                   0.65f, 0.50f, 0.12f); // Darker edge
            case PLUSH_TOY:             return cs(0.92f, 0.42f, 0.55f,   // Garish pink toy
                                                   0.72f, 0.22f, 0.35f); // Darker shadow
            case ARCADE_TOKEN:          return cs(0.75f, 0.62f, 0.20f,   // Gold token face
                                                   0.22f, 0.22f, 0.22f); // Dark edge
            case SCREWDRIVER:           return cs(0.48f, 0.48f, 0.52f,   // Metal blade
                                                   0.55f, 0.35f, 0.12f); // Wooden handle

            // Issue #1000: Northfield Fire Station
            case FIREFIGHTER_HELMET:    return c(0.95f, 0.80f, 0.10f);   // Bright yellow helmet
            case FIRE_AXE:              return cs(0.78f, 0.12f, 0.08f,   // Red axe head
                                                   0.45f, 0.28f, 0.12f); // Dark wood handle
            case HOSE_REEL:             return cs(0.88f, 0.12f, 0.08f,   // Red reel drum
                                                   0.55f, 0.55f, 0.60f); // Grey hose
            case DISPOSABLE_LIGHTER:    return cs(0.95f, 0.20f, 0.10f,   // Red lighter body
                                                   0.92f, 0.88f, 0.20f); // Yellow flame top

            // Issue #1004: Northfield Community Centre
            case VINYL_RECORD:          return c(0.10f, 0.10f, 0.10f);   // Black vinyl
            case THERMOS_FLASK:         return cs(0.62f, 0.18f, 0.12f,   // Red flask body
                                                   0.80f, 0.80f, 0.80f); // Silver lid
            case JIGSAW_PUZZLE_BOX:     return cs(0.20f, 0.45f, 0.75f,   // Blue box side
                                                   0.85f, 0.75f, 0.30f); // Gold lid
            case KNITTING_NEEDLES:      return cs(0.78f, 0.12f, 0.12f,   // Red needles
                                                   0.88f, 0.82f, 0.50f); // Yellow yarn

            // Issue #1012: Skin Deep Tattoos
            case NEEDLE:                return cs(0.80f, 0.80f, 0.82f,   // Silver needle
                                                   0.10f, 0.10f, 0.10f); // Dark tip
            case INK_BOTTLE:            return cs(0.10f, 0.10f, 0.38f,   // Dark ink body
                                                   0.22f, 0.22f, 0.60f); // Blue ink cap
            case TATTOO_GUN:            return cs(0.22f, 0.22f, 0.25f,   // Gunmetal body
                                                   0.80f, 0.80f, 0.80f); // Chrome highlight

            // Issue #1110: Skin Deep Tattoos — new craft items
            case PRISON_TATTOO_KIT:     return cs(0.35f, 0.28f, 0.20f,   // Brown cloth wrap
                                                   0.10f, 0.10f, 0.38f); // Dark ink stain
            case TATTOO_VOUCHER:        return cs(0.95f, 0.92f, 0.75f,   // Cream card stock
                                                   0.12f, 0.10f, 0.30f); // Dark ink logo

            // Issue #1014: Northfield Newsagent
            case LOTTERY_TICKET:        return cs(0.20f, 0.55f, 0.20f,   // Green ticket body
                                                   0.88f, 0.82f, 0.15f); // Gold strip
            case CHEWING_GUM:           return c(0.65f, 0.90f, 0.55f);   // Spearmint green packet
            case PHONE_CREDIT_VOUCHER:  return cs(0.20f, 0.35f, 0.75f,   // Blue voucher
                                                   0.88f, 0.88f, 0.88f); // Grey back
            case PAPER_SATCHEL:         return cs(0.62f, 0.48f, 0.28f,   // Canvas brown
                                                   0.45f, 0.32f, 0.15f); // Darker strap
            case BIRTHDAY_CARD:         return cs(0.88f, 0.20f, 0.55f,   // Pink card front
                                                   0.95f, 0.92f, 0.85f); // Cream inside
            case PLASTIC:               return c(0.72f, 0.80f, 0.88f);   // Light blue-grey plastic
            case LIGHTER:               return cs(0.22f, 0.22f, 0.25f,   // Dark chrome body
                                                   0.92f, 0.78f, 0.15f); // Gold flame top

            // Issue #1016: Northfield Canal
            case CANAL_FISH:            return cs(0.45f, 0.62f, 0.52f,   // Dull green-grey fish
                                                   0.30f, 0.42f, 0.38f); // Darker belly
            case DINGHY:                return cs(0.90f, 0.22f, 0.12f,   // Red inflatable rubber
                                                   0.88f, 0.88f, 0.88f); // Grey interior
            case BOOT:                  return cs(0.25f, 0.22f, 0.20f,   // Dark rubber boot
                                                   0.38f, 0.35f, 0.30f); // Lighter sole
            case STOLEN_WALLET:         return cs(0.35f, 0.22f, 0.10f,   // Brown leather
                                                   0.18f, 0.12f, 0.05f); // Dark interior
            case STRING_ITEM:           return cs(0.78f, 0.68f, 0.45f,   // Natural twine
                                                   0.58f, 0.48f, 0.28f); // Shadow
            case MOORING_NOTICE:        return cs(0.92f, 0.90f, 0.82f,   // Cream council paper
                                                   0.18f, 0.38f, 0.65f); // Blue council header
            case BIRDWATCHING_TIP:      return cs(0.88f, 0.88f, 0.80f,   // Cream note paper
                                                   0.35f, 0.55f, 0.28f); // Green nature accent
            case CAMPING_LANTERN:       return cs(0.88f, 0.72f, 0.15f,   // Warm yellow glow
                                                   0.35f, 0.35f, 0.38f); // Dark metal frame
            case CAMPING_STOVE:         return cs(0.35f, 0.35f, 0.38f,   // Dark metal body
                                                   0.88f, 0.30f, 0.10f); // Orange flame

            // Issue #1018: Northfield Poundstretcher
            case BLEACH:                return c(0.90f, 0.95f, 0.98f);   // White plastic bottle
            case GAFFER_TAPE:           return c(0.22f, 0.22f, 0.22f);   // Black tape roll
            case KNOCK_OFF_TRACKSUIT:   return cs(0.18f, 0.35f, 0.78f,   // Cheap blue top
                                                   0.22f, 0.22f, 0.22f); // Dark trouser stripe
            case BAKED_BEANS_TIN:       return cs(0.82f, 0.18f, 0.12f,   // Red tin body
                                                   0.95f, 0.65f, 0.18f); // Orange bean fill
            case CABLE_TIES:            return c(0.28f, 0.72f, 0.92f);   // Blue cable tie
            case WASHING_UP_LIQUID:     return c(0.78f, 0.90f, 0.40f);   // Lime green bottle
            case MOP:                   return cs(0.85f, 0.78f, 0.55f,   // Pale mop head
                                                  0.55f, 0.38f, 0.22f);  // Brown handle
            case CLOTH:                 return c(0.72f, 0.62f, 0.52f);   // Off-white rag
            case CANDLE:                return cs(0.95f, 0.92f, 0.72f,   // Cream wax body
                                                  0.98f, 0.78f, 0.20f);  // Yellow flame
            case TORCH:                 return cs(0.55f, 0.38f, 0.22f,   // Dark metal handle
                                                  0.98f, 0.72f, 0.18f);  // Warm orange flame
            case PADLOCK:               return c(0.52f, 0.52f, 0.58f);   // Metal grey padlock
            case MYSTERY_BOX:           return cs(0.72f, 0.58f, 0.32f,   // Cardboard brown box
                                                  0.88f, 0.20f, 0.12f);  // Red question mark
            case SLIPPERY_FLOOR_TRAP:   return c(0.55f, 0.88f, 0.92f);   // Wet floor blue
            case MAKESHIFT_ARMOUR:      return cs(0.22f, 0.22f, 0.22f,   // Gaffer tape black
                                                  0.72f, 0.62f, 0.52f);  // Cloth beige
            case DUCT_TAPE_RESTRAINT:   return cs(0.22f, 0.22f, 0.22f,   // Dark tape
                                                  0.28f, 0.72f, 0.92f);  // Blue cable tie
            case IMPROVISED_PEPPER_SPRAY: return c(0.92f, 0.88f, 0.25f); // Yellow warning
            case MAKESHIFT_TORCH:       return cs(0.55f, 0.38f, 0.22f,   // Dark wood handle
                                                  0.98f, 0.68f, 0.15f);  // Orange flame

            // Issue #1020: Northfield Sporting & Social Club
            case BITTER:                return cs(0.72f, 0.42f, 0.08f,   // Amber bitter
                                                  0.90f, 0.90f, 0.88f); // Cream head
            case MILD:                  return cs(0.48f, 0.28f, 0.08f,   // Dark mild
                                                  0.90f, 0.90f, 0.88f); // Cream head
            case LAGER_TOP:             return cs(0.88f, 0.80f, 0.20f,   // Pale lager
                                                  0.90f, 0.90f, 0.88f); // Cream lemonade top
            case MEMBERS_CARD:          return cs(0.18f, 0.35f, 0.18f,   // Dark green card
                                                  0.92f, 0.88f, 0.70f); // Cream text
            case DODGY_LEDGER:          return cs(0.25f, 0.18f, 0.12f,   // Dark cover
                                                  0.92f, 0.88f, 0.70f); // Cream page
            case QUIZ_SHEET:            return cs(0.95f, 0.92f, 0.82f,   // Off-white paper
                                                  0.15f, 0.15f, 0.55f); // Blue ink

            // Issue #1024: Sultan's Kebab
            case DONER_KEBAB:           return cs(0.72f, 0.48f, 0.15f,   // Toasted wrap
                                                  0.88f, 0.55f, 0.12f); // Doner meat
            case SHISH_KEBAB:           return cs(0.72f, 0.28f, 0.12f,   // Grilled meat
                                                  0.85f, 0.65f, 0.20f); // Golden skewer
            case CHIPS_IN_PITTA:        return cs(0.88f, 0.72f, 0.25f,   // Golden chips
                                                  0.92f, 0.88f, 0.72f); // Pitta bread
            case GARLIC_BREAD_SLICE:    return cs(0.88f, 0.82f, 0.45f,   // Toasted bread
                                                  0.65f, 0.58f, 0.22f); // Butter/garlic
            case DONER_MEAT:            return cs(0.62f, 0.32f, 0.18f,   // Raw meat
                                                  0.48f, 0.22f, 0.12f); // Dark meat
            case COOKED_DONER_MEAT:     return cs(0.72f, 0.42f, 0.12f,   // Cooked meat
                                                  0.55f, 0.30f, 0.08f); // Dark crust
            case PITTA_BREAD:           return c(0.92f, 0.88f, 0.72f);   // Cream pitta
            case TOOL_KIT:              return cs(0.62f, 0.42f, 0.22f,   // Brown case
                                                  0.72f, 0.52f, 0.12f); // Metal clasp

            // Issue #1026: Northfield Scrapyard
            case COPPER_WIRE:           return cs(0.72f, 0.45f, 0.12f,   // Copper orange
                                                  0.85f, 0.60f, 0.20f); // Bright copper
            case LEAD_FLASHING:         return cs(0.55f, 0.55f, 0.58f,   // Lead grey
                                                  0.45f, 0.45f, 0.48f); // Dark lead
            case COPPER_BALE:           return cs(0.68f, 0.40f, 0.10f,   // Compressed copper
                                                  0.58f, 0.32f, 0.08f); // Dark bale

            // Issue #1028: Northfield Cash Converters
            case DVD:                   return cs(0.62f, 0.62f, 0.72f,   // Silver disc
                                                  0.10f, 0.22f, 0.68f); // Blue case
            case BLUETOOTH_SPEAKER:     return cs(0.15f, 0.15f, 0.18f,   // Black body
                                                  0.55f, 0.55f, 0.60f); // Grey grille
            case TABLET:                return cs(0.12f, 0.12f, 0.15f,   // Dark screen
                                                  0.45f, 0.45f, 0.48f); // Aluminium back
            case GAMES_CONSOLE:         return cs(0.22f, 0.22f, 0.25f,   // Dark grey unit
                                                  0.55f, 0.15f, 0.15f); // Red accent
            case LAPTOP:                return cs(0.18f, 0.18f, 0.22f,   // Dark lid
                                                  0.62f, 0.62f, 0.62f); // Grey keyboard
            case WIPED_PHONE:           return cs(0.12f, 0.12f, 0.15f,   // Dark screen
                                                  0.25f, 0.72f, 0.25f); // Green "clean" indicator

            // Issue #1081: Pet Shop items
            case DOG_TREATS:    return c(0.85f, 0.62f, 0.18f);  // Biscuit brown
            case DOG_LEAD:      return c(0.52f, 0.28f, 0.10f);  // Leather brown
            case DOG_VET_RECORD: return cs(0.92f, 0.88f, 0.72f, // Paper cream
                                           0.25f, 0.62f, 0.25f); // Green stamp
            case DOG_SEDATIVE:  return c(0.72f, 0.18f, 0.72f);  // Purple syringe
            case BUDGIE:        return cs(0.20f, 0.72f, 0.25f,  // Green feathers
                                          0.95f, 0.85f, 0.15f); // Yellow head
            case GOLDFISH:      return cs(0.95f, 0.50f, 0.12f,  // Orange fish body
                                          0.20f, 0.55f, 0.88f); // Blue water

            // Issue #1112: The Raj Mahal
            case CHICKEN_TIKKA_MASALA: return cs(0.90f, 0.45f, 0.10f,  // Deep orange sauce
                                                  0.82f, 0.62f, 0.18f); // Golden chicken
            case LAMB_BALTI:           return cs(0.72f, 0.32f, 0.08f,  // Dark curry
                                                  0.82f, 0.45f, 0.12f); // Lamb brown
            case SAAG_ALOO:            return cs(0.25f, 0.55f, 0.18f,  // Spinach green
                                                  0.88f, 0.80f, 0.25f); // Potato yellow
            case NAAN_BREAD:           return c(0.92f, 0.85f, 0.62f);  // Toasted dough
            case POPPADOMS:            return c(0.88f, 0.75f, 0.30f);  // Crisp golden
            case MANGO_LASSI:          return cs(0.95f, 0.72f, 0.20f,  // Mango orange
                                                  0.98f, 0.95f, 0.80f); // Cream yoghurt
            case BYO_LAGER_CORKAGE:    return c(0.85f, 0.78f, 0.42f);  // Paper receipt
            case FOLDED_NOTE:          return cs(0.92f, 0.88f, 0.70f,  // Cream paper
                                                  0.40f, 0.35f, 0.28f); // Brown envelope

            // Issue #1114: Funeral Parlour items
            case FUNERAL_FLOWERS:      return cs(0.90f, 0.20f, 0.30f,  // Deep red petals
                                                  0.25f, 0.60f, 0.20f); // Green stem
            case CONDOLENCES_CARD:     return cs(0.92f, 0.88f, 0.82f,  // Cream card
                                                  0.55f, 0.45f, 0.60f); // Lavender border
            case MEMORIAL_CANDLE:      return cs(0.95f, 0.90f, 0.70f,  // Cream wax
                                                  0.98f, 0.85f, 0.20f); // Flame yellow
            case ORNAMENT_VASE:        return cs(0.60f, 0.50f, 0.35f,  // Earthy brown
                                                  0.45f, 0.35f, 0.22f); // Dark base
            case WAR_MEDAL:            return cs(0.85f, 0.72f, 0.20f,  // Gold medal
                                                  0.60f, 0.15f, 0.15f); // Red ribbon
            case SPARE_DENTURES:       return cs(0.95f, 0.92f, 0.88f,  // Off-white plastic
                                                  0.88f, 0.82f, 0.72f); // Yellowed
            case BISCUIT_TIN_SAVINGS:  return cs(0.72f, 0.55f, 0.30f,  // Tin brown
                                                  0.85f, 0.70f, 0.40f); // Warm gold
            case PROPERTY_DEED:        return cs(0.88f, 0.85f, 0.72f,  // Parchment
                                                  0.50f, 0.40f, 0.20f); // Ink brown

            // Issue #1124: Salvation Army Citadel
            case BRASS_INSTRUMENT:        return cs(0.85f, 0.72f, 0.20f,  // Brass gold
                                                    0.70f, 0.55f, 0.10f); // Darker brass
            case SALVATION_ARMY_UNIFORM:  return cs(0.12f, 0.20f, 0.60f,  // Navy blue jacket
                                                    0.88f, 0.20f, 0.18f); // Red trim
            case COLLECTION_LEDGER:       return cs(0.15f, 0.35f, 0.15f,  // Dark green cover
                                                    0.88f, 0.84f, 0.72f); // Cream pages
            case SOUP_BOWL:               return cs(0.80f, 0.55f, 0.30f,  // Terracotta bowl
                                                    0.90f, 0.68f, 0.28f); // Warm soup

            // Issue #1126: Recycling Centre
            case JUNK_BAG:                return cs(0.12f, 0.12f, 0.12f,  // Black bin bag
                                                    0.25f, 0.25f, 0.25f); // Dark grey tie
            case CIRCUIT_BOARD:           return cs(0.18f, 0.45f, 0.18f,  // PCB green
                                                    0.82f, 0.72f, 0.18f); // Gold traces
            case WORKING_LAPTOP:          return cs(0.22f, 0.22f, 0.22f,  // Dark grey lid
                                                    0.35f, 0.35f, 0.35f); // Lighter base

            // Issue #1128: Northfield NHS Walk-In Centre
            case BANDAGE:                 return c(0.95f, 0.95f, 0.95f);   // White dressing
            case MORPHINE_AMPOULE:        return cs(0.75f, 0.88f, 0.95f,   // Glass blue tint
                                                    0.90f, 0.95f, 0.98f); // Clear glass
            case TRAMADOL:                return c(0.80f, 0.78f, 0.95f);   // Pale purple capsule
            case DIAZEPAM:                return c(0.65f, 0.85f, 0.70f);   // Pale green tablet
            case UNUSED_SYRINGE:          return cs(0.92f, 0.92f, 0.92f,   // Clear barrel
                                                    0.90f, 0.15f, 0.15f); // Red plunger
            case DISCHARGE_LETTER:        return c(0.95f, 0.98f, 0.92f);   // Off-white NHS paper
            case PRESCRIPTION_FORM:       return c(0.85f, 0.92f, 0.98f);   // Light blue form

            // Issue #1130: Northfield BP Petrol Station
            case MICROWAVE_PASTY:         return cs(0.88f, 0.62f, 0.22f,  // Golden pasty
                                                    0.65f, 0.40f, 0.15f); // Darker crust
            case CAR_WASH_TOKEN:          return cs(0.30f, 0.58f, 0.82f,  // Blue token
                                                    0.20f, 0.42f, 0.65f); // Dark rim
            case CIGARETTE_CARTON:        return cs(0.90f, 0.15f, 0.15f,  // Red carton
                                                    0.95f, 0.90f, 0.85f); // White band
            case MAP:                     return cs(0.88f, 0.82f, 0.62f,  // Parchment fold
                                                    0.35f, 0.55f, 0.35f); // Green roads

            // Issue #1132: Dog Grooming Parlour
            case DOG_TREAT:               return cs(0.85f, 0.65f, 0.35f,  // Biscuit brown
                                                    0.70f, 0.50f, 0.25f); // Darker brown
            case DOG_SHOW_ROSETTE:        return cs(0.90f, 0.10f, 0.10f,  // Red ribbon
                                                    1.00f, 0.85f, 0.00f); // Gold centre
            case FLEA_POWDER:             return cs(0.90f, 0.90f, 0.90f,  // White powder
                                                    0.70f, 0.70f, 0.75f); // Lavender tint
            case SCISSORS:                return cs(0.70f, 0.70f, 0.72f,  // Steel grey
                                                    0.50f, 0.50f, 0.55f); // Dark handle
            case DOG_GROOMING_VOUCHER:    return cs(0.95f, 0.80f, 0.60f,  // Cream card
                                                    0.20f, 0.55f, 0.90f); // Blue text
            case UNLICENSED_DOG:          return cs(0.55f, 0.40f, 0.28f,  // Brown dog fur
                                                    0.30f, 0.20f, 0.12f); // Dark markings

            // Issue #1134: Patel's Newsagent
            case PENNY_SWEETS:            return cs(0.92f, 0.30f, 0.55f,  // Pink paper bag
                                                    0.80f, 0.20f, 0.45f); // Darker base
            case LOCAL_MAP:               return cs(0.85f, 0.88f, 0.95f,  // Light blue paper
                                                    0.60f, 0.62f, 0.70f); // Grey fold lines
            case RACING_FORM:             return cs(0.88f, 0.82f, 0.35f,  // Yellow cover
                                                    0.65f, 0.60f, 0.18f); // Darker spine
            case DIY_MONTHLY:             return cs(0.20f, 0.55f, 0.25f,  // Green magazine
                                                    0.12f, 0.38f, 0.15f); // Darker spine
            case PUZZLE_BOOK:             return cs(0.75f, 0.75f, 0.90f,  // Pale purple
                                                    0.55f, 0.55f, 0.72f); // Darker spine
            case DODGY_MAGAZINE:          return cs(0.30f, 0.30f, 0.30f,  // Brown wrapper
                                                    0.20f, 0.20f, 0.20f); // Dark edges
            case NEWSAGENT_KEY:           return cs(0.78f, 0.72f, 0.35f,  // Brass key
                                                    0.55f, 0.50f, 0.20f); // Darker shaft
            case NEWSAGENT_APRON:         return cs(0.15f, 0.35f, 0.72f,  // Blue tabard
                                                    0.10f, 0.25f, 0.55f); // Darker strap
            case PAPIER_MACHE_BRICK:      return cs(0.78f, 0.50f, 0.38f,  // Pinkish brick
                                                    0.62f, 0.38f, 0.28f); // Mortar grey

            // Issue #1136: The Vaults Nightclub
            case SMART_SHIRT:             return cs(0.88f, 0.88f, 0.95f,  // White/pale shirt
                                                    0.60f, 0.60f, 0.72f); // Collar shadow
            case NIGHTCLUB_MASTER_KEY:    return cs(0.60f, 0.55f, 0.30f,  // Brass key
                                                    0.38f, 0.35f, 0.18f); // Dark shaft

            // Issue #1138: Northfield Iceland
            case FROZEN_PIZZA:            return cs(0.55f, 0.78f, 0.92f,  // Iceland blue box
                                                    0.88f, 0.22f, 0.15f); // Red tomato sauce hint
            case PRAWN_RING:              return cs(0.90f, 0.55f, 0.35f,  // Pink prawn
                                                    0.75f, 0.82f, 0.45f); // Parsley garnish
            case CHICKEN_NUGGETS:         return cs(0.88f, 0.68f, 0.28f,  // Golden nugget batter
                                                    0.55f, 0.78f, 0.92f); // Iceland blue tray
            case ICELAND_PRAWN_COCKTAIL:  return cs(0.85f, 0.35f, 0.40f,  // Prawn pink
                                                    0.92f, 0.85f, 0.75f); // Cream sauce
            case FROZEN_TURKEY:           return cs(0.88f, 0.78f, 0.65f,  // Pale turkey flesh
                                                    0.55f, 0.78f, 0.92f); // Iceland blue bag
            case CHRISTMAS_ENVELOPE:      return cs(0.88f, 0.12f, 0.12f,  // Red Christmas envelope
                                                    0.92f, 0.82f, 0.18f); // Gold trim
            case CHRISTMAS_CLUB_CASH_BOX: return cs(0.62f, 0.42f, 0.22f,  // Brown lockbox
                                                    0.88f, 0.12f, 0.12f); // Red lid stripe
            case ICELAND_STAFF_KEY:       return cs(0.55f, 0.78f, 0.92f,  // Iceland blue fob
                                                    0.78f, 0.72f, 0.35f); // Brass key
            case FAKE_RECEIPT:            return cs(0.95f, 0.95f, 0.90f,  // Off-white thermal paper
                                                    0.70f, 0.70f, 0.65f); // Grey text hint

            // Issue #1142: Northfield RAOB Lodge
            case RAOB_MEMBERSHIP_CARD:    return cs(0.18f, 0.30f, 0.62f,  // Lodge blue card
                                                    0.10f, 0.18f, 0.45f); // Dark border
            case SPONSORSHIP_FORM:        return cs(0.92f, 0.88f, 0.72f,  // Cream paper
                                                    0.50f, 0.40f, 0.20f); // Ink brown
            case LODGE_CHARTER_DOCUMENT:  return cs(0.85f, 0.75f, 0.55f,  // Aged parchment
                                                    0.62f, 0.48f, 0.28f); // Darker edge
            case REGALIA_SET:             return cs(0.55f, 0.15f, 0.65f,  // Purple ceremonial
                                                    0.85f, 0.72f, 0.20f); // Gold trim
            case CEREMONIAL_MALLET:       return cs(0.55f, 0.38f, 0.18f,  // Oak handle
                                                    0.45f, 0.30f, 0.12f); // Darker head
            case PREMIUM_LAGER_CRATE:     return cs(0.88f, 0.80f, 0.18f,  // Gold label lager
                                                    0.62f, 0.55f, 0.12f); // Darker crate
            case BOX_OF_CHOCOLATES:       return cs(0.35f, 0.15f, 0.08f,  // Dark chocolate box
                                                    0.90f, 0.72f, 0.28f); // Gold ribbon
            case PLANNING_PERMISSION:     return cs(0.88f, 0.84f, 0.72f,  // Official cream paper
                                                    0.10f, 0.30f, 0.62f); // Blue council stamp
            case CASE_DISMISSED_FORM:     return cs(0.92f, 0.92f, 0.90f,  // White court paper
                                                    0.18f, 0.55f, 0.22f); // Green court stamp
            case RACING_TIP:              return cs(0.92f, 0.88f, 0.72f,  // Cream slip
                                                    0.88f, 0.18f, 0.18f); // Red racing ink

            // Issue #1349: Northfield RAOB Buffalo Lodge No. 1247
            case BUFFALO_MEMBERSHIP_CARD: return cs(0.72f, 0.08f, 0.08f,  // Buffalo crimson card
                                                    0.88f, 0.75f, 0.15f); // Gold lettering
            case BUFFALO_FEZ:             return cs(0.72f, 0.08f, 0.08f,  // Crimson fez body
                                                    0.88f, 0.75f, 0.15f); // Gold tassel
            case KOMPROMAT_LEDGER:        return cs(0.12f, 0.12f, 0.18f,  // Black leatherette
                                                    0.62f, 0.48f, 0.28f); // Aged page edges
            case BUFFALO_TOKEN:           return cs(0.78f, 0.62f, 0.22f,  // Brass token
                                                    0.55f, 0.42f, 0.10f); // Darker relief
            case CEREMONIAL_CANE:         return cs(0.10f, 0.08f, 0.08f,  // Ebony shaft
                                                    0.82f, 0.78f, 0.72f); // Silver buffalo head

            // Issue #1351: Northfield QuickFix Loans
            case FINAL_DEMAND_LETTER:     return cs(0.88f, 0.12f, 0.08f,  // Red-bordered paper
                                                    0.95f, 0.90f, 0.80f); // White body
            case FORGED_ID:               return cs(0.88f, 0.85f, 0.82f,  // Off-white card
                                                    0.28f, 0.42f, 0.68f); // Fake blue detail

            // Issue #1144: Northfield Probation Office
            case SIGN_ON_LETTER:          return cs(0.92f, 0.92f, 0.88f,  // White paper
                                                    0.18f, 0.38f, 0.72f); // Blue official stamp
            case ELECTRONIC_TAG:          return cs(0.12f, 0.12f, 0.18f,  // Black device
                                                    0.22f, 0.68f, 0.22f); // Green LED light
            case FAKE_SIGNAL_CHIP:        return cs(0.30f, 0.30f, 0.38f,  // Circuit grey
                                                    0.18f, 0.55f, 0.78f); // Blue chip trace
            case COMMUNITY_SERVICE_VEST:  return cs(0.90f, 0.82f, 0.05f,  // Hi-vis yellow
                                                    0.88f, 0.45f, 0.05f); // Orange trim
            case CASE_FILE_DOCUMENT:      return cs(0.82f, 0.68f, 0.42f,  // Manila folder
                                                    0.20f, 0.20f, 0.22f); // Dark text
            case WIRE_CUTTERS:            return cs(0.45f, 0.45f, 0.50f,  // Steel grey
                                                    0.22f, 0.18f, 0.12f); // Dark handle
            case TIN_OF_PAINT:            return cs(0.18f, 0.50f, 0.22f,  // British Racing Green
                                                    0.65f, 0.60f, 0.55f); // Tin silver

            // Issue #1146: Mick's MOT & Tyre Centre
            case TYRE:                    return cs(0.18f, 0.18f, 0.18f,  // Rubber black
                                                    0.42f, 0.42f, 0.45f); // Steel rim
            case CAR_PART:                return cs(0.48f, 0.52f, 0.55f,  // Oily steel
                                                    0.30f, 0.28f, 0.25f); // Grime shadow
            case BLANK_LOGBOOK:           return cs(0.88f, 0.88f, 0.85f,  // Official cream
                                                    0.10f, 0.30f, 0.60f); // DVLA blue stamp
            case MOT_CERT:                return cs(0.92f, 0.92f, 0.88f,  // White certificate
                                                    0.12f, 0.55f, 0.22f); // Green PASS stamp

            // Issue #1148: Northfield Council Estate Lock-Up Garages
            case BRIC_A_BRAC:             return cs(0.70f, 0.55f, 0.38f,  // Dusty tan
                                                    0.45f, 0.35f, 0.25f); // Brown shadow
            case VINTAGE_RECORD:          return cs(0.10f, 0.10f, 0.12f,  // Black vinyl
                                                    0.55f, 0.52f, 0.50f); // Label centre
            case DRUM_COMPONENT:          return cs(0.72f, 0.20f, 0.15f,  // Red drum shell
                                                    0.88f, 0.88f, 0.85f); // Chrome rim
            case CABLE:                   return c(0.15f, 0.15f, 0.18f);  // Black cable
            case BURNER_PHONE:            return cs(0.22f, 0.22f, 0.25f,  // Dark plastic
                                                    0.65f, 0.65f, 0.62f); // Screen grey
            case PIGEON_FEED_BAG:         return cs(0.85f, 0.80f, 0.60f,  // Hessian beige
                                                    0.45f, 0.38f, 0.20f); // Shadow brown
            case GARAGE_KEY_7:            return cs(0.78f, 0.65f, 0.20f,  // Brass key
                                                    0.55f, 0.45f, 0.10f); // Darker brass

            // Issue #1151: Northfield Sporting & Social Club
            case CLUB_MEMBERSHIP_CARD:    return cs(0.10f, 0.30f, 0.70f,  // Club blue card
                                                    0.05f, 0.15f, 0.45f); // Dark navy
            case DARTS_SET:               return cs(0.50f, 0.50f, 0.52f,  // Steel grey
                                                    0.25f, 0.25f, 0.28f); // Dark metal
            case QUIZ_ANSWER_SHEET:       return cs(0.95f, 0.95f, 0.88f,  // Off-white paper
                                                    0.20f, 0.20f, 0.60f); // Blue ink lines

            // Issue #1153: Northfield Community Centre
            case AEROBICS_PASS:           return cs(0.88f, 0.22f, 0.55f,  // Hot pink pass
                                                    0.98f, 0.90f, 0.95f); // Light background
            case CAB_REFERRAL_LETTER:     return cs(0.92f, 0.92f, 0.88f,  // White paper
                                                    0.10f, 0.30f, 0.68f); // CAB blue stamp
            case CHARACTER_REFERENCE_LETTER: return cs(0.92f, 0.92f, 0.88f, // White paper
                                                    0.22f, 0.50f, 0.22f); // Green approving stamp
            case GRANT_APPLICATION_FORM:  return cs(0.90f, 0.88f, 0.78f,  // Cream council paper
                                                    0.10f, 0.28f, 0.65f); // Blue council header
            case FORGED_GRANT_APPLICATION: return cs(0.88f, 0.82f, 0.68f, // Slightly off cream
                                                    0.38f, 0.22f, 0.65f); // Suspicious purple hue
            case GRANT_CHEQUE:            return cs(0.92f, 0.92f, 0.82f,  // Cheque cream
                                                    0.10f, 0.48f, 0.22f); // Green bank stamp
            case COUNTERFEIT_FLYER:       return cs(0.98f, 0.95f, 0.75f,  // Yellow flyer
                                                    0.88f, 0.20f, 0.20f); // Red headline text
            case CURRY_AND_RICE:          return cs(0.82f, 0.55f, 0.12f,  // Golden curry sauce
                                                    0.92f, 0.88f, 0.72f); // White rice
            case NAAN:                    return c(0.88f, 0.78f, 0.55f);  // Warm bread tan
            case SAMOSA:                  return cs(0.78f, 0.62f, 0.30f,  // Pastry gold
                                                    0.55f, 0.72f, 0.22f); // Pea green filling hint

            // Issue #1155: Northfield NHS Dentist
            case FIZZY_DRINK:             return cs(0.88f, 0.10f, 0.10f,  // Red can
                                                    0.98f, 0.95f, 0.88f); // White highlight
            case HARIBO:                  return cs(0.98f, 0.85f, 0.10f,  // Bright yellow bag
                                                    0.92f, 0.25f, 0.25f); // Red logo band
            case TOOTHBRUSH:              return cs(0.20f, 0.65f, 0.90f,  // NHS blue handle
                                                    0.95f, 0.95f, 0.95f); // White bristles
            case DENTAL_APPOINTMENT_LETTER: return cs(0.90f, 0.92f, 0.98f, // NHS letter paper
                                                    0.28f, 0.55f, 0.82f); // NHS blue header
            case WAITING_LIST_LETTER:     return cs(0.92f, 0.92f, 0.88f,  // White paper
                                                    0.28f, 0.55f, 0.82f); // NHS blue stamp
            case FORGED_WAITING_LIST_LETTER: return cs(0.88f, 0.88f, 0.80f, // Slightly off-white
                                                    0.42f, 0.28f, 0.70f); // Suspicious purple hue

            // Issue #1157: Northfield Tesco Express
            case TESCO_SANDWICH:         return cs(0.90f, 0.85f, 0.70f,  // Bread beige
                                                   0.60f, 0.80f, 0.30f); // Lettuce green
            case TESCO_PASTA_POT:        return c(0.95f, 0.90f, 0.70f);  // Pasta yellow
            case TESCO_ORANGE_JUICE:     return c(1.00f, 0.60f, 0.10f);  // Orange
            case TESCO_MEAL_DEAL_BAG:    return cs(0.10f, 0.40f, 0.20f,  // Tesco dark green
                                                   1.00f, 0.80f, 0.00f); // Tesco gold
            case CLUBCARD:               return cs(0.10f, 0.40f, 0.20f,  // Tesco green
                                                   0.80f, 0.80f, 0.80f); // Silver card
            case CLUBCARD_VOUCHER:       return cs(0.10f, 0.40f, 0.20f,  // Green
                                                   0.95f, 0.95f, 0.80f); // Yellow voucher
            case TESCO_FINEST_WINE:      return cs(0.45f, 0.10f, 0.20f,  // Dark red wine
                                                   0.85f, 0.75f, 0.30f); // Gold label
            case TESCO_OWN_BRAND_VODKA:  return c(0.88f, 0.92f, 0.98f);  // Clear/blue
            case READY_MEAL:             return cs(0.30f, 0.25f, 0.20f,  // Dark tray
                                                   0.80f, 0.60f, 0.40f); // Food orange
            case CLUBCARD_STATEMENT:     return cs(0.90f, 0.92f, 0.88f,  // White letter
                                                   0.10f, 0.40f, 0.20f); // Tesco green header

            // Issue #1161: Northfield Poundstretcher
            case OWN_BRAND_CRISPS:       return c(0.88f, 0.72f, 0.22f);  // Golden yellow bag
            case OWN_BRAND_COLA:         return c(0.20f, 0.15f, 0.55f);  // Dark blue can
            case BARGAIN_BUCKET_CRISPS:  return c(0.75f, 0.55f, 0.18f);  // Brown value bag
            case WHOLESALE_SPIRITS:      return c(0.60f, 0.75f, 0.90f);  // Clear-blue glass

            // Issue #1165: Northfield Match Day
            case COUNTERFEIT_TICKET:     return cs(0.92f, 0.88f, 0.72f,  // Cream ticket paper
                                                   0.18f, 0.35f, 0.72f); // Blue club colour
            case KNOCKOFF_SCARF:         return cs(0.18f, 0.35f, 0.72f,  // Blue (home colours)
                                                   0.88f, 0.72f, 0.18f); // Gold stripe
            case MATCH_PROGRAMME:        return cs(0.20f, 0.45f, 0.80f,  // Club blue cover
                                                   0.92f, 0.88f, 0.72f); // White/cream pages
            case WALLET_FAN:             return cs(0.28f, 0.18f, 0.10f,  // Dark leather
                                                   0.72f, 0.60f, 0.20f); // Gold trim
            case FABRIC_SCRAP:           return c(0.72f, 0.65f, 0.58f);  // Grey fabric

            // Issue #1167: Northfield Amateur Boxing Club
            case GYM_MEMBERSHIP_CARD:   return c(0.20f, 0.55f, 0.30f);  // Green card
            case FIGHT_ENTRY_FORM:      return c(0.88f, 0.88f, 0.75f);  // Cream form paper
            case BOXING_GLOVES:         return cs(0.80f, 0.10f, 0.10f,  // Red glove leather
                                                  0.22f, 0.08f, 0.08f); // Dark trim
            case LOADED_GLOVE:          return cs(0.55f, 0.55f, 0.58f,  // Grey metal weight
                                                  0.80f, 0.10f, 0.10f); // Red leather outer
            case FIGHT_PURSE:           return cs(0.82f, 0.68f, 0.15f,  // Gold coin
                                                  0.28f, 0.20f, 0.08f); // Dark envelope
            case PROTEIN_BAR:           return cs(0.38f, 0.25f, 0.08f,  // Brown wrapper
                                                  0.92f, 0.75f, 0.15f); // Gold branding
            case SPEED_BAG_CHALK:       return c(0.90f, 0.90f, 0.88f);  // White chalk dust
            case ABA_TROPHY:            return cs(0.82f, 0.68f, 0.15f,  // Gold trophy body
                                                  0.62f, 0.48f, 0.10f); // Dark gold base

            // Issue #1171: Northfield TV Licence
            case TV_LICENCE_LETTER:     return cs(0.88f, 0.88f, 0.75f,  // Cream envelope
                                                  0.72f, 0.20f, 0.10f); // Red BBC stripe
            case TV_LICENCE_CERTIFICATE: return cs(0.92f, 0.88f, 0.75f, // Official cream paper
                                                   0.20f, 0.50f, 0.80f); // Blue official seal
            case FORGED_TV_LICENCE:     return cs(0.82f, 0.78f, 0.65f,  // Fake cream paper
                                                  0.55f, 0.15f, 0.10f); // Suspicious red marks

            // Issue #1173: Northfield Balti House
            case VEGETABLE_BALTI:       return cs(0.40f, 0.62f, 0.22f,  // Green curry sauce
                                                  0.72f, 0.52f, 0.18f); // Steel balti bowl
            case MANGO_CHUTNEY:         return c(0.95f, 0.72f, 0.15f);  // Orange-gold chutney
            case BALTI_BOX:             return cs(0.82f, 0.72f, 0.55f,  // Cardboard box
                                                  0.20f, 0.60f, 0.25f); // Green logo
            case NAAN_BAG:              return cs(0.92f, 0.88f, 0.72f,  // Toasted dough colour
                                                  0.70f, 0.60f, 0.45f); // Brown bag
            case BALTI_CATERING_TIN:    return cs(0.75f, 0.78f, 0.80f,  // Brushed steel
                                                  0.50f, 0.52f, 0.55f); // Dark steel rim
            case FAKE_CURRY_POWDER:     return cs(0.95f, 0.85f, 0.12f,  // Bright yellow
                                                  0.88f, 0.70f, 0.08f); // Turmeric shade
            case RESTAURANT_RECEIPT:    return cs(0.95f, 0.95f, 0.90f,  // White till paper
                                                  0.35f, 0.35f, 0.35f); // Grey printed text

            // Issue #1175: Northfield Argos
            case ARGOS_SLIP:            return cs(0.85f, 0.88f, 0.95f,  // Light blue slip paper
                                                  0.30f, 0.30f, 0.70f); // Dark blue printed number
            case ARGOS_RECEIPT:         return cs(0.95f, 0.93f, 0.88f,  // Cream thermal paper
                                                  0.20f, 0.45f, 0.75f); // Argos blue logo
            case FORGED_RECEIPT:        return cs(0.90f, 0.88f, 0.82f,  // Off-white paper
                                                  0.55f, 0.40f, 0.20f); // Brown ink (dodgy)
            case BLANK_RECEIPT:         return c(0.96f, 0.96f, 0.94f);  // Plain thermal paper
            case CATALOGUE_PENCIL:      return cs(0.95f, 0.80f, 0.10f,  // Yellow pencil body
                                                  0.50f, 0.30f, 0.10f); // Brown wood tip

            // Issue #1181: Northfield Chugger Blitz
            case CHARITY_TABARD:        return cs(0.95f, 0.80f, 0.05f,  // Bright yellow tabard
                                                  0.15f, 0.55f, 0.25f); // Green charity logo
            case CHARITY_CLIPBOARD:     return cs(0.72f, 0.52f, 0.28f,  // Brown clipboard board
                                                  0.95f, 0.95f, 0.92f); // White paper
            case MARKER_PEN:            return cs(0.10f, 0.10f, 0.10f,  // Black marker body
                                                  0.85f, 0.20f, 0.20f); // Red cap

            // Issue #1186: Northfield Probation Office
            case ANKLE_TAG:             return cs(0.12f, 0.12f, 0.18f,  // Black plastic casing
                                                  0.22f, 0.68f, 0.22f); // Green LED indicator
            case SIGN_IN_FORM_PROP:     return cs(0.95f, 0.95f, 0.92f,  // White A4 paper
                                                  0.20f, 0.40f, 0.75f); // Blue official ink

            // Issue #1188: Northfield DWP Home Visit
            case DWP_LETTER_PROP:       return cs(0.78f, 0.62f, 0.28f,  // Brown envelope
                                                  0.22f, 0.35f, 0.68f); // Blue official stamp
            case APPEAL_LETTER_PROP:    return cs(0.95f, 0.95f, 0.92f,  // White paper
                                                  0.18f, 0.45f, 0.22f); // Green "Appeal" text
            case CASH_IN_HAND_LEDGER:   return cs(0.32f, 0.22f, 0.12f,  // Brown notebook
                                                  0.88f, 0.72f, 0.18f); // Gold lettering

            // Issue #1229: Northfield Handy Builders
            case CEMENT_BAG:            return cs(0.72f, 0.68f, 0.55f,  // Sandy beige bag
                                                  0.55f, 0.50f, 0.38f); // Darker stripe
            case SAND_BAG:              return cs(0.82f, 0.72f, 0.40f,  // Sandy yellow bag
                                                  0.65f, 0.55f, 0.28f); // Darker base
            case WIRE_REEL:             return cs(0.15f, 0.15f, 0.18f,  // Dark reel body
                                                  0.72f, 0.45f, 0.12f); // Orange cable
            case COPPER_PIPE:           return c(0.72f, 0.40f, 0.18f);  // Copper orange
            case MORTAR:                return cs(0.72f, 0.68f, 0.60f,  // Grey mortar
                                                  0.55f, 0.52f, 0.45f); // Darker mix
            case TRADE_ACCOUNT_CARD:    return cs(0.15f, 0.45f, 0.25f,  // Green trade card
                                                  0.95f, 0.92f, 0.80f); // Cream text
            case FAKE_TRADE_ACCOUNT_CARD: return cs(0.15f, 0.35f, 0.20f, // Slightly off green
                                                  0.85f, 0.80f, 0.62f); // Slightly off cream
            case ASBO_LETTER:           return cs(0.95f, 0.92f, 0.80f,  // Cream envelope
                                                  0.20f, 0.30f, 0.80f); // Blue official stripe
            case ASBO_ORDER_DOCUMENT:   return cs(0.88f, 0.85f, 0.75f,  // Off-white laminate
                                                  0.20f, 0.30f, 0.80f); // Blue council stripe

            // Issue #1235: Northfield Sporting & Social Club
            case PORK_CHOPS:            return c(0.78f, 0.38f, 0.30f);  // Raw pork pink
            case CHICKEN_LEGS:          return c(0.88f, 0.72f, 0.42f);  // Pale chicken yellow
            case SAUSAGES:              return c(0.70f, 0.32f, 0.22f);  // Dark sausage brown
            case BACK_ROOM_KEY:         return c(0.75f, 0.65f, 0.30f);  // Brass yellow
            case INCRIMINATING_DOCUMENT: return cs(0.92f, 0.90f, 0.78f, // Cream paper
                                                   0.60f, 0.10f, 0.10f); // Red stamp

            // Issue #1240: Northfield NHS Blood Donation Session
            case BLOOD_BAG:             return cs(0.75f, 0.08f, 0.08f,  // Blood red bag
                                                  0.88f, 0.88f, 0.92f); // Clear plastic
            case DONOR_QUESTIONNAIRE:   return cs(0.95f, 0.95f, 0.92f,  // White form
                                                  0.20f, 0.50f, 0.20f); // NHS green header
            case FORGED_DONOR_QUESTIONNAIRE: return cs(0.92f, 0.92f, 0.88f, // Slightly off-white
                                                  0.20f, 0.50f, 0.20f); // NHS green header
            case ORANGE_SQUASH:         return c(0.95f, 0.55f, 0.10f);  // Orange squash colour
            case DIGESTIVE_BISCUIT:     return c(0.82f, 0.68f, 0.42f);  // Pale biscuit tan
            case ORANGE_JUICE:          return c(0.98f, 0.60f, 0.10f);  // Bright orange juice

            // Issue #1243: Northfield Bert's Tyres & MOT
            case MOT_CERTIFICATE:       return cs(0.95f, 0.95f, 0.85f,  // Off-white document
                                                  0.10f, 0.45f, 0.10f); // Green DVLA stripe
            case FAIL_SHEET:            return cs(0.98f, 0.90f, 0.85f,  // Pink failure notice
                                                  0.75f, 0.15f, 0.10f); // Red header
            case BROWN_ENVELOPE:        return c(0.62f, 0.45f, 0.22f);  // Brown envelope
            case STOLEN_TYRE:           return c(0.15f, 0.15f, 0.15f);  // Rubber black
            case CATALYTIC_CONVERTER:   return c(0.65f, 0.65f, 0.60f);  // Silvery grey metal
            case CAR_BATTERY:           return cs(0.12f, 0.12f, 0.14f,  // Black casing
                                                  0.80f, 0.20f, 0.10f); // Red terminal
            case INSPECTION_STICKER:    return c(0.20f, 0.65f, 0.20f);  // Green pass sticker

            // Issue #1257: Northfield Rag-and-Bone Man
            case JUNK_ITEM:             return c(0.45f, 0.40f, 0.35f);  // Rusty brown junk
            case GARDEN_ORNAMENT:       return c(0.75f, 0.45f, 0.30f);  // Terracotta gnome
            case FORGED_LICENCE:        return cs(0.95f, 0.95f, 0.85f,  // Off-white paper
                                                  0.20f, 0.55f, 0.20f); // Green licence border
            case RUBBER_TYRE:           return c(0.12f, 0.12f, 0.12f);  // Black rubber
            case PENKNIFE:              return cs(0.60f, 0.60f, 0.55f,  // Steel blade
                                                  0.35f, 0.22f, 0.12f); // Brown handle

            // Issue #1263: Northfield Illegal Street Racing
            case NITROUS_CANISTER:      return cs(0.85f, 0.85f, 0.90f,  // Silver canister
                                                  0.20f, 0.65f, 0.20f); // Green label
            case RACING_TROPHY:         return cs(0.90f, 0.75f, 0.20f,  // Gold trophy
                                                  0.60f, 0.45f, 0.15f); // Dark gold base

            // Issue #1265: Northfield Loan Shark — Big Mick's Doorstep Lending
            case LOAN_AGREEMENT:        return cs(0.95f, 0.92f, 0.80f,  // Yellowed paper
                                                  0.70f, 0.15f, 0.10f); // Red "AGREEMENT" stamp
            case DEBT_LEDGER:           return cs(0.20f, 0.20f, 0.25f,  // Dark navy cover
                                                  0.85f, 0.75f, 0.30f); // Gold lettering

            // Issue #1273: Northfield Fly-Tipping Ring
            case GARDEN_WASTE_BAG:      return cs(0.25f, 0.55f, 0.20f,  // Council green bag
                                                  0.35f, 0.45f, 0.25f); // Dark green detail
            case RUBBLE_SACK:           return cs(0.55f, 0.48f, 0.38f,  // Dusty concrete grey
                                                  0.40f, 0.35f, 0.28f); // Shadow base
            case FIXED_PENALTY_NOTICE:  return cs(0.95f, 0.92f, 0.75f,  // Yellow council notice
                                                  0.70f, 0.08f, 0.08f); // Red "PENALTY" stamp

            // Issue #1276: Northfield Minicab Office — Big Terry's Cabs
            case TL_COUNCIL_PLATE:      return cs(0.85f, 0.82f, 0.20f,  // Yellow licence plate
                                                  0.15f, 0.15f, 0.15f); // Black text/border

            // Issue #1282: Northfield Day & Night Chemist
            case CALPOL:                return c(0.90f, 0.40f, 0.60f);  // Pink children's medicine
            case IBUPROFEN:             return c(0.85f, 0.30f, 0.20f);  // Red/orange packaging
            case PLASTERS:              return cs(0.90f, 0.72f, 0.58f,  // Skin tone strip
                                                  0.75f, 0.20f, 0.20f); // Red cross
            case CONDOMS:               return c(0.20f, 0.60f, 0.25f);  // Green foil packet
            case NUROFEN_PLUS:          return cs(0.85f, 0.25f, 0.25f,  // Red packaging
                                                  0.95f, 0.95f, 0.95f); // White stripe
            case FORGED_PRESCRIPTION:   return cs(0.95f, 0.95f, 0.88f,  // Off-white paper
                                                  0.40f, 0.60f, 0.85f); // Blue NHS stripe
            case STOLEN_METHADONE:      return c(0.40f, 0.72f, 0.40f);  // Green liquid bottle
            case WHITE_COAT:            return c(0.96f, 0.96f, 0.96f);  // Clinical white

            // Issue #1301: Northfield Big Issue Vendor
            case BIG_ISSUE_MAG:         return cs(0.20f, 0.45f, 0.80f,  // Blue Big Issue cover
                                                  0.95f, 0.95f, 0.95f); // White title text
            case COUNTERFEIT_BIG_ISSUE: return cs(0.25f, 0.38f, 0.62f,  // Faded blue cover
                                                  0.75f, 0.75f, 0.75f); // Grey washed-out text
            case CANVAS_BAG:            return cs(0.55f, 0.48f, 0.30f,  // Worn khaki canvas
                                                  0.40f, 0.35f, 0.20f); // Dark strap

            // Issue #1306: Northfield Traveller Site
            case LUCKY_HEATHER:         return cs(0.72f, 0.30f, 0.65f,  // Purple heather
                                                  0.50f, 0.75f, 0.30f); // Green stem
            case CLOTHES_PEG_BUNDLE:    return cs(0.72f, 0.50f, 0.22f,  // Pale wood peg
                                                  0.55f, 0.35f, 0.12f); // Dark wood grain
            case TARMAC_MIX:            return cs(0.20f, 0.20f, 0.20f,  // Black tarmac
                                                  0.45f, 0.45f, 0.45f); // Grey aggregate
            case DOG_FIGHT_LEDGER:      return cs(0.20f, 0.25f, 0.35f,  // Dark navy cover
                                                  0.82f, 0.12f, 0.10f); // Red "PRIVATE" label
            case LUCKY_HEATHER_CROWN:   return cs(0.72f, 0.30f, 0.65f,  // Purple heather
                                                  0.80f, 0.75f, 0.20f); // Gold crown wire
            case STOLEN_BIKE:           return cs(0.20f, 0.40f, 0.70f,  // Blue frame
                                                  0.18f, 0.18f, 0.18f); // Black tyres

            // Issue #1317: Northfield Bonfire Night
            case ROCKET_FIREWORK:       return cs(0.90f, 0.15f, 0.10f,  // Red card tube
                                                  0.95f, 0.85f, 0.10f); // Gold fuse
            case BANGER_FIREWORK:       return cs(0.15f, 0.15f, 0.15f,  // Black wrapping
                                                  0.85f, 0.18f, 0.10f); // Red label
            case ROMAN_CANDLE:          return cs(0.55f, 0.20f, 0.70f,  // Purple tube
                                                  0.95f, 0.75f, 0.10f); // Gold stars
            case OLD_CLOTHES:           return cs(0.45f, 0.38f, 0.30f,  // Worn beige/brown
                                                  0.35f, 0.28f, 0.22f); // Darker folds

            // Issue #1335: Northfield Cycle Centre — Dave's Bikes
            case BIKE_REPAIR_KIT:       return cs(0.85f, 0.35f, 0.10f,  // Orange pouch
                                                  0.20f, 0.20f, 0.20f); // Black zip
            case BIKE_LOCK:             return cs(0.18f, 0.18f, 0.18f,  // Black D-lock
                                                  0.65f, 0.65f, 0.65f); // Silver bar
            case BIKE_LIGHT_FRONT:      return cs(0.92f, 0.90f, 0.82f,  // White LED lens
                                                  0.20f, 0.20f, 0.20f); // Black body
            case BIKE_LIGHT_REAR:       return cs(0.90f, 0.10f, 0.10f,  // Red LED lens
                                                  0.20f, 0.20f, 0.20f); // Black body
            case BIKE_HELMET:           return cs(0.15f, 0.65f, 0.90f,  // Blue foam shell
                                                  0.85f, 0.85f, 0.85f); // White strap
            case DELIVERY_BAG:          return cs(0.95f, 0.45f, 0.05f,  // JustEat orange
                                                  0.10f, 0.10f, 0.10f); // Black logo
            case COLD_DELIVERY_BAG:     return cs(0.60f, 0.30f, 0.05f,  // Faded orange
                                                  0.35f, 0.35f, 0.35f); // Grey worn seam

            // Issue #1337: Northfield Police Station — The Nick
            case POLICE_KEY_CARD:       return cs(0.10f, 0.25f, 0.60f,  // Police blue card
                                                  0.85f, 0.85f, 0.85f); // Silver chip
            case ROPE_AND_HOOK:         return cs(0.55f, 0.38f, 0.20f,  // Brown rope
                                                  0.50f, 0.50f, 0.50f); // Grey hook
            case DRIVING_LICENCE:       return cs(0.18f, 0.40f, 0.70f,  // DVLA blue card
                                                  0.90f, 0.85f, 0.75f); // Cream face

            // Issue #1341: Residents' Association
            case NOISE_ABATEMENT_LETTER: return cs(0.90f, 0.90f, 0.80f,  // Cream paper
                                                   0.20f, 0.40f, 0.70f); // Council blue text
            case PETTY_CASH_TIN:        return cs(0.70f, 0.45f, 0.10f,  // Tan metal tin
                                                  0.30f, 0.65f, 0.30f); // Green lid
            case MYSTERY_HAMPER:        return cs(0.70f, 0.50f, 0.25f,  // Wicker brown
                                                  0.85f, 0.20f, 0.20f); // Red bow
            case GARDEN_VOUCHER:        return cs(0.30f, 0.65f, 0.30f,  // Garden green
                                                  0.95f, 0.90f, 0.75f); // Cream voucher
            case RIGGED_BARREL:         return cs(0.45f, 0.28f, 0.12f,  // Dark wood barrel
                                                  0.60f, 0.60f, 0.60f); // Metal hoop

            // Issue #1343: Northfield Christmas Lights Switch-On
            case MULLED_WINE:           return cs(0.55f, 0.05f, 0.10f,  // Deep red wine
                                                  0.70f, 0.40f, 0.15f); // Spiced amber
            case WAYNE_STUBBS_AUTOGRAPH: return cs(0.95f, 0.95f, 0.85f, // Cream paper
                                                  0.20f, 0.40f, 0.80f); // Blue ink
            case CELEBRITY_WALLET:      return cs(0.15f, 0.10f, 0.08f,  // Black leather
                                                  0.85f, 0.75f, 0.20f); // Gold trim
            case FREEBIE_WRISTBAND:     return cs(0.20f, 0.60f, 0.25f,  // Christmas green
                                                  0.90f, 0.20f, 0.20f); // Red stripe
            case CHRISTMAS_LIGHTS_BULB: return cs(0.95f, 0.80f, 0.10f,  // Warm yellow glow
                                                  0.85f, 0.25f, 0.10f); // Red base

            // Issue #1353: Northfield Amateur Dramatics Society
            case STAGE_COSTUME:         return cs(0.75f, 0.20f, 0.55f,  // Theatrical purple
                                                  0.95f, 0.90f, 0.80f); // Cream lace trim
            case REHEARSAL_SCHEDULE:    return cs(0.92f, 0.92f, 0.88f,  // Off-white paper
                                                  0.20f, 0.35f, 0.60f); // Blue ink header
            case FORGED_TICKET:         return cs(0.90f, 0.85f, 0.70f,  // Cream ticket card
                                                  0.80f, 0.15f, 0.20f); // Red border
            case PROP_GUN:              return cs(0.18f, 0.18f, 0.18f,  // Black prop body
                                                  0.65f, 0.60f, 0.55f); // Silver barrel

            // Issue #1357: Northfield Charity Fun Run
            case RACE_NUMBER_BIB:       return cs(0.92f, 0.92f, 0.88f,  // White bib
                                                  0.80f, 0.15f, 0.20f); // Red number
            case SPONSOR_SHEET:         return cs(0.94f, 0.94f, 0.90f,  // Cream paper
                                                  0.20f, 0.40f, 0.70f); // Blue text
            case WINNERS_MEDAL:         return cs(0.85f, 0.70f, 0.10f,  // Gold medal
                                                  0.80f, 0.20f, 0.20f); // Red ribbon
            case WATER_CUP:             return cs(0.90f, 0.90f, 0.90f,  // White paper cup
                                                  0.30f, 0.55f, 0.85f); // Blue branding

            // Issue #1359: Northfield HMRC Tax Investigation
            case TAX_DEMAND_LETTER:     return cs(0.95f, 0.95f, 0.85f,  // Cream HMRC paper
                                                  0.80f, 0.10f, 0.10f); // Red HMRC logo
            case CLEAN_BILL_OF_HEALTH:  return cs(0.90f, 0.95f, 0.90f,  // Light green clearance
                                                  0.20f, 0.60f, 0.20f); // Green tick/border
            case CASH_BRIBE_ENVELOPE:   return cs(0.60f, 0.45f, 0.25f,  // Brown envelope
                                                  0.85f, 0.70f, 0.10f); // Gold coin hint

            // Issue #1381: Northfield Bank Holiday Street Party
            case WARM_LAGER:        return c(0.88f, 0.78f, 0.30f); // Flat golden lager
            case CRISP_PACKET:      return c(0.92f, 0.85f, 0.20f); // Yellow crisp packet
            case RAW_SAUSAGE:       return c(0.90f, 0.60f, 0.55f); // Pink raw meat
            case COOKED_SAUSAGE:    return c(0.65f, 0.35f, 0.12f); // Charred brown
            case SELECTION_BOX:     return cs(0.90f, 0.10f, 0.10f, // Red box
                                              0.95f, 0.75f, 0.10f); // Gold ribbon
            case NOVELTY_TOWEL:     return cs(0.20f, 0.55f, 0.82f, // Blue towel
                                              0.95f, 0.92f, 0.88f); // White stripe
            case BOTTLE_OF_WINE:    return c(0.52f, 0.18f, 0.38f); // Dark Merlot red
            case TOOLKIT:           return c(0.35f, 0.35f, 0.40f); // Steel grey
            case COMMEMORATIVE_MUG: return cs(0.25f, 0.45f, 0.72f, // Council blue
                                              0.92f, 0.88f, 0.72f); // Cream interior

            // Issue #1381: Northfield Halloween
            case RAW_EGG:           return c(0.95f, 0.92f, 0.80f); // Off-white egg
            case MINI_CHOCOLATE_BAR: return c(0.35f, 0.18f, 0.08f); // Dark chocolate
            case TRICK_OR_TREAT_BAG: return c(1.00f, 0.55f, 0.00f); // Orange bag
            case WITCH_COSTUME:     return c(0.10f, 0.08f, 0.12f); // Near black
            case GHOST_SHEET:       return c(0.95f, 0.95f, 0.95f); // White sheet
            case SKELETON_SUIT:     return cs(0.92f, 0.92f, 0.88f, // Bone white
                                              0.12f, 0.12f, 0.12f); // Black details
            case PUMPKIN_HEAD_MASK: return c(0.95f, 0.50f, 0.05f); // Jack-o-lantern orange
            case RAW_PUMPKIN:       return c(1.00f, 0.50f, 0.05f); // Pumpkin orange
            case CARVED_PUMPKIN:    return c(0.88f, 0.45f, 0.05f); // Darker carved pumpkin
            case PUMPKIN_INNARDS:   return c(0.88f, 0.65f, 0.10f); // Orange-yellow pulp
            case POINTY_HAT:        return c(0.10f, 0.08f, 0.12f); // Black witch hat
            case BLACK_CAPE:        return c(0.08f, 0.08f, 0.10f); // Deep black cape

            // Issue #1383: Northfield Boxing Day Sales
            case FROZEN_PRAWN_RING:      return c(0.88f, 0.62f, 0.55f); // Pale pink/peach
            case PARTY_FOOD_PLATTER:     return cs(0.92f, 0.88f, 0.78f, // Cream platter
                                                   0.88f, 0.22f, 0.18f); // Red food items
            case LUXURY_BISCUIT_TIN:     return cs(0.72f, 0.12f, 0.12f, // Deep red tin
                                                   0.85f, 0.72f, 0.10f); // Gold embossing
            case GEORGE_FOREMAN_GRILL:   return cs(0.20f, 0.22f, 0.25f, // Dark grey grill body
                                                   0.65f, 0.65f, 0.68f); // Silver grill plate
            case BREAD_MAKER:            return c(0.72f, 0.70f, 0.65f);  // Off-white plastic
            case VINYL_RECORD_BOX:       return cs(0.75f, 0.62f, 0.38f, // Brown cardboard
                                                   0.10f, 0.10f, 0.10f); // Black record peek
            case GENUINE_FIRST_PRESSING: return cs(0.10f, 0.10f, 0.10f, // Black vinyl
                                                   0.82f, 0.72f, 0.10f); // Gold label
            case HDMI_CABLE:             return c(0.18f, 0.18f, 0.20f);  // Dark grey/black cable

            // Issue #1400: Northfield Residents' Parking Permit Racket
            case PARKING_PERMIT:         return cs(0.88f, 0.92f, 0.70f, // Pale green permit
                                                   0.18f, 0.35f, 0.18f); // Dark green text
            case FORGED_PARKING_PERMIT:  return cs(0.88f, 0.92f, 0.70f, // Same pale green
                                                   0.85f, 0.55f, 0.10f); // Suspicious orange text
            case WHEEL_CLAMP_KEY:        return cs(0.15f, 0.15f, 0.18f, // Dark metal
                                                   0.72f, 0.62f, 0.18f); // Brass key tip
            case STOLEN_PRINTER_INK:     return cs(0.18f, 0.18f, 0.22f, // Black cartridge body
                                                   0.55f, 0.15f, 0.62f); // Purple HP logo accent

            // Issue #1402: Northfield Severn Trent Road Dig
            case THERMOS:               return cs(0.72f, 0.15f, 0.10f, // Red thermos body
                                                  0.85f, 0.82f, 0.75f); // Silver cup cap
            case SITE_RADIO:            return cs(0.15f, 0.15f, 0.18f, // Plaster-dusty dark grey
                                                  0.88f, 0.75f, 0.25f); // Tuner dial gold
            case CONTRACTOR_CLIPBOARD:  return cs(0.72f, 0.55f, 0.30f, // Brown clipboard board
                                                  0.92f, 0.92f, 0.88f); // White paper
            case HARD_HAT:              return c(0.95f, 0.95f, 0.92f);  // White hard hat
            case MYSTERY_OBJECT:        return cs(0.45f, 0.38f, 0.28f, // Earthy brown (dirty)
                                                  0.62f, 0.55f, 0.40f); // Lighter soil highlight

            // Issue #1404: Northfield Community Litter Pick
            case LITTER_PICKER_STICK:   return cs(0.92f, 0.48f, 0.05f, // Hi-vis orange shaft
                                                   0.75f, 0.72f, 0.68f); // Silver grabber tip
            case COUNCIL_RUBBISH_BAG:   return cs(0.12f, 0.12f, 0.14f, // Black bag body
                                                   0.85f, 0.88f, 0.92f); // White printed text
            case CRACK_PIPE:            return cs(0.78f, 0.88f, 0.92f, // Pale glass tube
                                                   0.62f, 0.35f, 0.15f); // Amber scorch marks

            // Issue #1406: Northfield Dodgy Roofer
            case BUCKET_OF_SEALANT:     return cs(0.50f, 0.50f, 0.52f, // Grey bucket body
                                                   0.72f, 0.70f, 0.65f); // Silver lid
            case LATEX_GLOVES:          return cs(0.92f, 0.88f, 0.75f, // Off-white rubber glove
                                                   0.75f, 0.72f, 0.60f); // Darker cuff band
            case SCAFFOLDING_SPANNER:   return cs(0.45f, 0.45f, 0.48f, // Steel grey body
                                                   0.62f, 0.58f, 0.52f); // Worn metal highlight
            case INVOICE_PAD:           return cs(0.92f, 0.92f, 0.88f, // White paper
                                                   0.72f, 0.55f, 0.30f); // Brown cardboard back
            case CASH_ENVELOPE:         return cs(0.88f, 0.82f, 0.62f, // Manila envelope
                                                   0.25f, 0.55f, 0.25f); // Green cash visible
            case ROOF_SLATE_BAG:        return cs(0.28f, 0.28f, 0.30f, // Dark slate grey
                                                   0.48f, 0.42f, 0.38f); // Worn canvas bag

            // Issue #1416: Northfield Mobile Speed Camera Van
            case SPEED_CAMERA_SD_CARD:  return c(0.20f, 0.20f, 0.22f);  // Dark plastic card
            case SPEEDING_FINE_NOTICE:  return cs(0.95f, 0.90f, 0.70f, // Cream paper
                                                   0.15f, 0.25f, 0.60f); // Blue police heading
            case HANDWRITTEN_WARNING_SIGN: return cs(0.75f, 0.62f, 0.38f, // Cardboard
                                                     0.05f, 0.05f, 0.05f); // Black marker text

            // Issue #1420: Northfield Post Office Horizon Scandal
            case CITIZENS_ADVICE_LEAFLET: return cs(0.10f, 0.45f, 0.20f, // Green CAB branding
                                                    0.92f, 0.92f, 0.88f); // White leaflet paper
            case SHORTFALL_LETTER:        return cs(0.95f, 0.90f, 0.70f, // Cream paper
                                                    0.15f, 0.25f, 0.55f); // Blue Post Office heading
            case TRANSACTION_LOG:         return cs(0.92f, 0.92f, 0.88f, // White paper
                                                    0.25f, 0.25f, 0.25f); // Grey dot-matrix text
            case STAMPS_BUNDLE:           return c(0.85f, 0.15f, 0.15f);  // Royal Mail red
            case IT_CONTRACTOR_ID_BADGE:  return cs(0.85f, 0.88f, 0.92f, // White card
                                                    0.15f, 0.40f, 0.75f); // Blue lanyard stripe
            case USB_STICK:               return c(0.20f, 0.22f, 0.25f);  // Dark plastic

            // Issue #1424: Northfield Doorstep Energy Tout
            case TOUT_CLIPBOARD:          return cs(0.88f, 0.92f, 0.88f, // White clipboard
                                                    0.05f, 0.45f, 0.15f); // PowerSave UK green brand
            case SMART_METER_KIT:         return cs(0.20f, 0.22f, 0.25f, // Dark box
                                                    0.05f, 0.45f, 0.15f); // Green PowerSave logo
            case CRAIG_WITNESS_STATEMENT: return cs(0.92f, 0.92f, 0.88f, // White paper
                                                    0.25f, 0.55f, 0.25f); // Green pen notes
            case DAVE_DEBT_LIST:          return cs(0.92f, 0.90f, 0.80f, // Yellowed paper
                                                    0.10f, 0.10f, 0.10f); // Black biro scrawl
            case REPLACEMENT_CLIPBOARD:   return c(0.78f, 0.65f, 0.40f);  // Bare wood clipboard
            case FANCIER_FAVOUR:          return cs(0.88f, 0.82f, 0.62f, // Racing Post paper
                                                    0.55f, 0.28f, 0.10f); // Brown ink handwriting

            // Issue #1426: Northfield Neighbourhood WhatsApp Group
            case STRAY_CAT:               return cs(0.70f, 0.55f, 0.35f, // Tabby orange-brown
                                                    0.25f, 0.20f, 0.15f); // Darker stripe
            case CAT_RANSOM_NOTE:         return cs(0.92f, 0.90f, 0.80f, // Cream paper
                                                    0.10f, 0.10f, 0.10f); // Black biro scrawl

            // Issue #1428: Northfield Council CCTV Audit
            case CCTV_FOOTAGE:            return cs(0.10f, 0.10f, 0.12f, // Black VHS body
                                                    0.60f, 0.55f, 0.20f); // Keith's biro label
            case DUMMY_CCTV_CAMERA:       return cs(0.20f, 0.20f, 0.22f, // Dark grey plastic
                                                    0.80f, 0.10f, 0.10f); // Blinking red LED
            case MEAL_DEAL_ITEM:          return cs(0.90f, 0.85f, 0.70f, // Sandwich wrapper
                                                    0.20f, 0.55f, 0.25f); // Green crisps packet

            // Issue #1433: Northfield Easter Weekend
            case HOT_CROSS_BUN:           return cs(0.75f, 0.50f, 0.20f, // Baked bun
                                                    0.60f, 0.30f, 0.10f); // Cross icing
            case CHOCOLATE_EGG:           return cs(0.35f, 0.20f, 0.05f, // Dark chocolate
                                                    0.80f, 0.60f, 0.20f); // Foil wrap
            case FOIL_EASTER_EGG:         return cs(0.90f, 0.80f, 0.10f, // Bright gold foil
                                                    0.20f, 0.80f, 0.30f); // Green ribbon
            case EASTER_BASKET:           return cs(0.75f, 0.55f, 0.20f, // Wicker
                                                    0.20f, 0.65f, 0.25f); // Grass filler
            case BIKER_JACKET:            return c(0.10f, 0.10f, 0.10f);  // Black leather
            case CHARITY_BUCKET_EASTER:   return cs(0.20f, 0.45f, 0.80f, // Blue bucket
                                                    0.95f, 0.85f, 0.10f); // Yellow label
            case BIKERS_VOUCHER:          return cs(0.90f, 0.90f, 0.90f, // White slip
                                                    0.10f, 0.10f, 0.50f); // Blue print

            // Issue #1435: Northfield Community Speedwatch
            case SPEED_GUN:               return cs(0.25f, 0.25f, 0.28f, // Dark grey body
                                                    0.60f, 0.60f, 0.65f); // Silver barrel
            case SPEEDWATCH_CLIPBOARD:    return cs(0.88f, 0.92f, 0.88f, // White laminated sheet
                                                    0.85f, 0.72f, 0.20f); // Yellow hi-vis trim
            case SPEEDWATCH_WARNING_LETTER: return cs(0.95f, 0.92f, 0.88f, // Cream paper
                                                    0.10f, 0.25f, 0.55f); // Blue police-style heading
            case SPEEDWATCH_LANYARD:      return cs(0.85f, 0.72f, 0.20f, // Yellow hi-vis lanyard
                                                    0.88f, 0.92f, 0.88f); // White ID card

            // Issue #1447: Northfield Street Hustler
            case RIGGED_CARD_DECK:        return cs(0.10f, 0.10f, 0.12f, // Black card back
                                                    0.85f, 0.10f, 0.10f); // Red marked queen
            case LOOKOUT_CUT:             return cs(0.72f, 0.62f, 0.10f, // Folded notes
                                                    0.60f, 0.50f, 0.08f); // Darker crease

            // Issue #1449: Northfield Mobile Library
            case LIBRARY_CARD:            return cs(0.15f, 0.42f, 0.72f, // Blue card body
                                                    0.88f, 0.92f, 0.88f); // White text area
            case RARE_BOOK:               return cs(0.45f, 0.18f, 0.08f, // Dark vellum cover
                                                    0.72f, 0.62f, 0.38f); // Gold spine lettering
            case SELF_HELP_PAPERBACK:     return cs(0.92f, 0.75f, 0.28f, // Bright yellow cover
                                                    0.88f, 0.92f, 0.88f); // White pages
            case LOCAL_HISTORY_BOOK:      return cs(0.38f, 0.25f, 0.12f, // Brown hardback cover
                                                    0.88f, 0.92f, 0.88f); // White pages
            case CRIME_FICTION:           return cs(0.12f, 0.12f, 0.15f, // Dark thriller cover
                                                    0.82f, 0.15f, 0.12f); // Red title text
            case JOB_SKILLS_GUIDE:        return cs(0.15f, 0.55f, 0.28f, // Green cover
                                                    0.88f, 0.92f, 0.88f); // White pages
            case GARDENING_ENCYCLOPEDIA:  return cs(0.22f, 0.55f, 0.18f, // Green cover
                                                    0.62f, 0.42f, 0.18f); // Brown spine
            case COOKBOOK:                return cs(0.88f, 0.38f, 0.15f, // Orange cover
                                                    0.88f, 0.92f, 0.88f); // White pages
            case LIBRARY_VOUCHER:         return cs(0.88f, 0.85f, 0.72f, // Cream paper
                                                    0.15f, 0.42f, 0.72f); // Blue stamp
            case PETITION_BOARD:          return cs(0.72f, 0.55f, 0.30f, // Brown clipboard
                                                    0.88f, 0.92f, 0.88f); // White paper

            // Issue #1461: Northfield Street Preacher
            case MEGAPHONE:               return cs(0.88f, 0.72f, 0.10f, // Yellow megaphone body
                                                    0.48f, 0.48f, 0.52f); // Grey handle
            case BLESSED_WATER_BOTTLE:    return cs(0.62f, 0.82f, 0.92f, // Light blue bottle
                                                    0.88f, 0.92f, 0.88f); // White label

            // Issue #1469: Northfield Second-Hand Record Shop
            case RARE_PRESSING:           return cs(0.10f, 0.10f, 0.10f, // Black vinyl
                                                    0.88f, 0.72f, 0.10f); // Gold label
            case FAKE_RARE_LABEL:         return cs(0.88f, 0.72f, 0.10f, // Gold foil
                                                    0.88f, 0.92f, 0.88f); // White paper backing

            // Issue #1471: Northfield Closing-Down Sale
            case KNOCKOFF_ELECTRONIC:     return cs(0.35f, 0.35f, 0.40f, // Grey plastic casing
                                                    0.88f, 0.15f, 0.12f); // Red "SALE" sticker
            case BRAND_NAME_TELLY:        return cs(0.12f, 0.12f, 0.15f, // Black TV body
                                                    0.55f, 0.78f, 0.92f); // Blue screen glow

            // Issue #1475: Northfield Rare Bird Alert
            case BINOCULARS:              return cs(0.22f, 0.22f, 0.25f, // Dark metal body
                                                    0.55f, 0.78f, 0.92f); // Blue lens
            case BIRD_PHOTO:              return cs(0.95f, 0.95f, 0.90f, // White photo paper
                                                    0.28f, 0.68f, 0.18f); // Green parakeet
            case FAKE_BIRD_PHOTO:         return cs(0.90f, 0.90f, 0.85f, // Slightly grey photocopy
                                                    0.42f, 0.42f, 0.42f); // Grey smudge
            case BIRD_GUIDE_BOOK:         return cs(0.20f, 0.48f, 0.20f, // Green cover
                                                    0.95f, 0.92f, 0.82f); // Cream pages
            case FAKE_BIRDWATCHING_TIP:   return cs(0.85f, 0.85f, 0.75f, // Slightly off-white paper
                                                    0.65f, 0.12f, 0.12f); // Red marker scrawl

            // Issue #1477: Northfield Warm Hub
            case HUB_TEA:                 return cs(0.88f, 0.68f, 0.40f, // Milky tea brown
                                                    0.92f, 0.92f, 0.90f); // White mug
            case HUB_BISCUIT:             return c(0.92f, 0.82f, 0.65f);  // Pale biscuit beige
            case DONATIONS_TIN:           return cs(0.55f, 0.62f, 0.68f, // Tin grey-blue
                                                    0.30f, 0.30f, 0.32f); // Dark empty interior
            case FAKE_DONATIONS_FORM:     return cs(0.95f, 0.95f, 0.90f, // White paper
                                                    0.12f, 0.22f, 0.68f); // Blue official stamp
            case HOMEMADE_CAKE_SLICE:     return cs(0.95f, 0.90f, 0.80f, // Cream sponge
                                                    0.85f, 0.32f, 0.32f); // Pink jam layer

            default:             return c(0.5f, 0.5f, 0.5f);
        }
    }

    /** Make a single-color icon (opaque). */
    private static Color[] c(float r, float g, float b) {
        return new Color[]{new Color(r, g, b, 1f)};
    }

    /** Make a single-color icon with alpha. */
    private static Color[] c(float r, float g, float b, float a) {
        return new Color[]{new Color(r, g, b, a)};
    }

    /** Make a two-color icon (top-left and bottom-right halves). */
    private static Color[] cs(float r1, float g1, float b1, float r2, float g2, float b2) {
        return new Color[]{new Color(r1, g1, b1, 1f), new Color(r2, g2, b2, 1f)};
    }

    /**
     * Returns true if this material is a block/construction material (drawn as a
     * coloured square in the inventory). Returns false for tools, food, and shop goods,
     * which use custom shapes for better visual clarity.
     */
    public boolean isBlockItem() {
        switch (this) {
            // Tools
            case IMPROVISED_TOOL:
            case STONE_TOOL:
            // Food & drink
            case SAUSAGE_ROLL:
            case STEAK_BAKE:
            case CHIPS:
            case KEBAB:
            case ENERGY_DRINK:
            case CRISPS:
            case TIN_OF_BEANS:
            case PINT:
            case PERI_PERI_CHICKEN:
            // Shop goods & other non-block items
            case SCRATCH_CARD:
            case NEWSPAPER:
            case WASHING_POWDER:
            case PARACETAMOL:
            case TEXTBOOK:
            case HYMN_BOOK:
            case PETROL_CAN:
            case PETROL_CAN_FULL:
            case BREAD:
            case COLD_CHIPS:
            case COOKING_OIL:
            case FIRE_STARTER:
            case MOLOTOV:
            case LARD_BUCKET:
            case HAIR_CLIPPERS:
            case HAIR_CLIPPERS_BROKEN:
            case NAIL_POLISH:
            case BROKEN_PHONE:
            case DODGY_DVD:
            case FIRE_EXTINGUISHER:
            case ANTIDEPRESSANTS:
            // Office non-block items
            case COMPUTER:
            case OFFICE_CHAIR:
            case STAPLER:
            // Diamond is a gem, not a block
            case DIAMOND:
            // Currency coins
            case SHILLING:
            case PENNY:
            case COIN:
            // Fence stock items
            case HIGH_VIS_JACKET:
            case CROWBAR:
            case ANGLE_GRINDER:
            case BALACLAVA:
            case BOLT_CUTTERS:
            case DODGY_PASTY:
            case FOOD:
            // Clothing items (Issue #698)
            case COAT:
            case UMBRELLA:
            case WOOLLY_HAT:
            case FLASK_OF_TEA:
            // Heist tools & loot (Issue #704)
            case GLASS_CUTTER:
            case ROPE_LADDER:
            case GOLD_RING:
            case COUNCIL_ID:
            case PASTY:
            case LEAVES:
            // Slumlord items (Issue #712)
            case DEED:
            case PAINT_TIN:
            case EVICTION_NOTICE:
            // Squat system items (Issue #714)
            case LOCKPICK:
            case FAKE_ID:
            // Underground music scene (Issue #716)
            case MICROPHONE:
            case FLYER:
            // Craftable 3D props (Issue #720)
            case PROP_BED:
            case PROP_WORKBENCH:
            case PROP_DARTBOARD:
            case PROP_SPEAKER_STACK:
            case PROP_DISCO_BALL:
            case PROP_DJ_DECKS:
            // Witness & Evidence items (Issue #765)
            case RUMOUR_NOTE:
            case CCTV_TAPE:
            // Disguise items (Issue #767)
            case POLICE_UNIFORM:
            case COUNCIL_JACKET:
            case MARCHETTI_TRACKSUIT:
            case STREET_LADS_HOODIE:
            case HI_VIS_VEST:
            case GREGGS_APRON:
            // Issue #769: Black Market Economy items
            case PLASTIC_BAG:
            case GREGGS_PASTRY:
            case CAN_OF_LAGER:
            case CIGARETTE:
            case WOOLLY_HAT_ECONOMY:
            case SLEEPING_BAG:
            case STOLEN_PHONE:
            case PRESCRIPTION_MEDS:
            case COUNTERFEIT_NOTE:
            case TOBACCO_POUCH:
            // Issue #781: Graffiti materials (not block items)
            case SPRAY_CAN_EMPTY:
            case SPRAY_CAN:
            case PAINT_PIGMENT_RED:
            case PAINT_PIGMENT_BLUE:
            case PAINT_PIGMENT_GOLD:
            case PAINT_PIGMENT_WHITE:
            case PAINT_PIGMENT_GREY:
            // Issue #783: Pirate FM materials (not block items)
            case WIRE:
            case BROADCAST_TAPE:
            case TRANSMITTER_ITEM:
            // Issue #785: Dodgy Market Stall materials (not block items)
            case STALL_FRAME:
            case STALL_AWNING:
            case MARKET_LICENCE:
            case KNOCK_OFF_PERFUME:
            // Issue #797: Neighbourhood Watch materials (not block items)
            case NEIGHBOURHOOD_NEWSLETTER:
            case PEACE_OFFERING:
            // Issue #799: Corner Shop Economy materials (not block items)
            case SHOP_KEY:
            case CIDER:
            case TOBACCO:
            // Issue #801: Underground Fight Night materials (not block items)
            case FIGHT_CARD:
            case CHAMPIONSHIP_BELT:
            case MOUTH_GUARD:
            case RUBBER:
            // Issue #901: Bista Village Portal (not a block item)
            case BISTA_VILLAGE_PORTAL:
            // Issue #906: Busking System (not a block item)
            case BUCKET_DRUM:
            // Issue #908: Bookies Horse Racing System (not a block item)
            case BET_SLIP:
            // Issue #914: Allotment System (not block items)
            case POTATO_SEED:
            case CARROT_SEED:
            case CABBAGE_SEED:
            case SUNFLOWER_SEED:
            case POTATO:
            case CARROT:
            case CABBAGE:
            case SUNFLOWER:
            case PLOT_DEED:
            // Issue #918: Bus System (not block items)
            case BUS_PASS:
            case INSPECTOR_BADGE:
            // Issue #922: Skate Park System (not a block item)
            case SKATEBOARD:
            // Issue #924: Launderette System (not block items)
            case CLEAN_CLOTHES:
            case BLOODY_HOODIE:
            case STOLEN_JACKET:
            // Issue #926: Chippy System (not block items)
            case BATTERED_SAUSAGE:
            case CHIP_BUTTY:
            case MUSHY_PEAS:
            case PICKLED_EGG:
            case FISH_SUPPER:
            case SALT_AND_VINEGAR_PACKET:
            case CHIPS_SEASONED:
            case BOTTLE_OF_WATER:
            case WHITE_PAPER:
            // Issue #928: Public Library System (not block items)
            case DIY_MANUAL:
            case NEGOTIATION_BOOK:
            case STREET_LAW_PAMPHLET:
            // Issue #932: Ice Cream Van System (not block items)
            case ICE_CREAM_99:
            case SCREWBALL:
            case FAB_LOLLY:
            case CHOC_ICE:
            case OYSTER_CARD_LOLLY:
            case BUBBLEGUM:
            // Issue #1069: Ice Cream Van System (Dave's Ices — not block items)
            case NINETY_NINE_FLAKE:
            case LOLLY:
            case WAFER_TUBS:
            case BANANA_SPLIT:
            case FLAKE_99_WITH_SAUCE:
            // Issue #934: Pigeon Racing System (not block items)
            case RACING_PIGEON:
            case BREAD_CRUST:
            case PIGEON_TROPHY:
            // Issue #936: Council Skip & Bulky Item Day (not block items)
            case OLD_SOFA:
            case BROKEN_TELLY:
            case WONKY_CHAIR:
            case CARPET_ROLL:
            case OLD_MATTRESS:
            case FILING_CABINET:
            case EXERCISE_BIKE:
            case BOX_OF_RECORDS:
            case MICROWAVE:
            case SHOPPING_TROLLEY_GOLD:
            case ANTIQUE_CLOCK:
            case HOT_PASTRY:
            case LUXURY_BED:
            case DWP_LETTER:
            // Issue #938: Greasy Spoon Café (not block items)
            case FULL_ENGLISH:
            case MUG_OF_TEA:
            case BEANS_ON_TOAST:
            case FRIED_BREAD:
            case BACON_BUTTY:
            case BUILDER_S_TEA:
            case CHALKBOARD:
            // Issue #948: Hand Car Wash (not block items)
            case SQUEEGEE:
            // Issue #950: Northfield Leisure Centre (not block items)
            case CHOCOLATE_BAR:
            case WATER_BOTTLE:
            case SWIM_TRUNKS:
            // Issue #952: Clucky's Fried Chicken (not block items)
            case CHICKEN_WINGS:
            case CHICKEN_BOX:
            case CHIPS_AND_GRAVY:
            case FLAT_COLA:
            case CHICKEN_BONE:
            case EMPTY_CHICKEN_BOX:
            // Issue #961: Cash4Gold Pawn Shop
            case GUITAR:
            // Issue #963: Northfield Canal
            case FISHING_ROD:
            case FISH_ROACH:
            case FISH_BREAM:
            case FISH_PERCH:
            case FISH_PIKE:
            case ROPE:
            // Issue #967: Northfield Taxi Rank (not block items)
            case TAXI_PASS:
            case DODGY_PACKAGE:
            // Issue #969: Northfield Cemetery (not block items)
            case SPADE:
            case WEDDING_RING:
            case POCKET_WATCH:
            case CONDOLENCE_CARD:
            case OLD_PHOTOGRAPH:
            case OLD_COIN:
            // Issue #971: The Rusty Anchor Wetherspoons (not block items)
            case CHEAP_SPIRITS:
            case CURRY_CLUB_SPECIAL:
            // Issue #973: Northfield GP Surgery (not block items)
            case PRESCRIPTION:
            case ANTIBIOTICS:
            case STRONG_MEDS:
            case SICK_NOTE:
            case BLANK_PRESCRIPTION_FORM:
            case NEON_LEAFLET:
            // Issue #1116: Northfield Pharmacy (not block items)
            case COLD_AND_FLU_SACHET:
            case ANTISEPTIC_CREAM:
            case VITAMIN_C_TABLETS:
            case HAIR_DYE:
            case READING_GLASSES:
            // Issue #975: Northfield Post Office (not block items)
            case BENEFITS_BOOK:
            case PARCEL:
            case STAMP:
            case POST_BOX_PROP:
            // Issue #977: Northfield Amusement Arcade (not block items)
            case TWOPENCE:
            case PLUSH_TOY:
            case ARCADE_TOKEN:
            case SCREWDRIVER:
            // Issue #1000: Northfield Fire Station (not block items)
            case FIREFIGHTER_HELMET:
            case FIRE_AXE:
            case HOSE_REEL:
            // Issue #1002: Northfield BP Petrol Station (not block items)
            case DISPOSABLE_LIGHTER:
            // Issue #1014: Northfield Newsagent (not block items)
            case LOTTERY_TICKET:
            case CHEWING_GUM:
            case PHONE_CREDIT_VOUCHER:
            case PAPER_SATCHEL:
            case BIRTHDAY_CARD:
            case LIGHTER:
            // Issue #1016: Northfield Canal (not block items)
            case CANAL_FISH:
            case DINGHY:
            case BOOT:
            case STOLEN_WALLET:
            case STRING_ITEM:
            case MOORING_NOTICE:
            case BIRDWATCHING_TIP:
            case CAMPING_LANTERN:
            case CAMPING_STOVE:
            // Issue #1018: Northfield Poundstretcher — all are inventory items, not blocks
            case BLEACH:
            case GAFFER_TAPE:
            case KNOCK_OFF_TRACKSUIT:
            case BAKED_BEANS_TIN:
            case CABLE_TIES:
            case WASHING_UP_LIQUID:
            case MOP:
            case CLOTH:
            case CANDLE:
            case TORCH:
            case PADLOCK:
            case MYSTERY_BOX:
            case SLIPPERY_FLOOR_TRAP:
            case MAKESHIFT_ARMOUR:
            case DUCT_TAPE_RESTRAINT:
            case IMPROVISED_PEPPER_SPRAY:
            case MAKESHIFT_TORCH:
            // Issue #1020: Northfield Sporting & Social Club — all inventory items, not blocks
            case BITTER:
            case MILD:
            case LAGER_TOP:
            case MEMBERS_CARD:
            case DODGY_LEDGER:
            case QUIZ_SHEET:
            // Issue #1024: Sultan's Kebab — all inventory items, not blocks
            case DONER_KEBAB:
            case SHISH_KEBAB:
            case CHIPS_IN_PITTA:
            case GARLIC_BREAD_SLICE:
            case DONER_MEAT:
            case COOKED_DONER_MEAT:
            case PITTA_BREAD:
            case TOOL_KIT:
            // Issue #1026: Northfield Scrapyard
            case COPPER_WIRE:
            case LEAD_FLASHING:
            case COPPER_BALE:
            // Issue #1028: Northfield Cash Converters
            case DVD:
            case BLUETOOTH_SPEAKER:
            case TABLET:
            case GAMES_CONSOLE:
            case LAPTOP:
            case WIPED_PHONE:
            // Issue #1112: The Raj Mahal — all food/items, not blocks
            case CHICKEN_TIKKA_MASALA:
            case LAMB_BALTI:
            case SAAG_ALOO:
            case NAAN_BREAD:
            case POPPADOMS:
            case MANGO_LASSI:
            case BYO_LAGER_CORKAGE:
            case FOLDED_NOTE:
            // Issue #1114: Funeral Parlour — not block items
            case FUNERAL_FLOWERS:
            case CONDOLENCES_CARD:
            case MEMORIAL_CANDLE:
            case ORNAMENT_VASE:
            case WAR_MEDAL:
            case SPARE_DENTURES:
            case BISCUIT_TIN_SAVINGS:
            case PROPERTY_DEED:
            // Issue #1122: Tanning Salon — not block items
            case MARCHETTI_LEDGER:
            case BROWN_ENVELOPE:
            // Issue #1124: Salvation Army Citadel — not block items
            case BRASS_INSTRUMENT:
            case SALVATION_ARMY_UNIFORM:
            case COLLECTION_LEDGER:
            case SOUP_BOWL:
            // Issue #1126: Recycling Centre — not block items
            case JUNK_BAG:
            case CIRCUIT_BOARD:
            case WORKING_LAPTOP:
            // Issue #1128: NHS Walk-In Centre — not block items
            case BANDAGE:
            case MORPHINE_AMPOULE:
            case TRAMADOL:
            case DIAZEPAM:
            case UNUSED_SYRINGE:
            case DISCHARGE_LETTER:
            case PRESCRIPTION_FORM:
            // Issue #1130: Northfield BP Petrol Station — not block items
            case MICROWAVE_PASTY:
            case CAR_WASH_TOKEN:
            case CIGARETTE_CARTON:
            case MAP:
            // Issue #1132: Dog Grooming Parlour — not block items
            case DOG_TREAT:
            case DOG_SHOW_ROSETTE:
            case FLEA_POWDER:
            case SCISSORS:
            case DOG_GROOMING_VOUCHER:
            case UNLICENSED_DOG:
            // Issue #1134: Patel's Newsagent — not block items
            case PENNY_SWEETS:
            case LOCAL_MAP:
            case RACING_FORM:
            case DIY_MONTHLY:
            case PUZZLE_BOOK:
            case DODGY_MAGAZINE:
            case NEWSAGENT_KEY:
            case NEWSAGENT_APRON:
            case PAPIER_MACHE_BRICK:
            // Issue #1136: The Vaults Nightclub — not block items
            case SMART_SHIRT:
            case NIGHTCLUB_MASTER_KEY:
            // Issue #1142: Northfield RAOB Lodge — not block items
            case RAOB_MEMBERSHIP_CARD:
            case SPONSORSHIP_FORM:
            case LODGE_CHARTER_DOCUMENT:
            case REGALIA_SET:
            case CEREMONIAL_MALLET:
            case PREMIUM_LAGER_CRATE:
            case BOX_OF_CHOCOLATES:
            case PLANNING_PERMISSION:
            case CASE_DISMISSED_FORM:
            case RACING_TIP:
            // Issue #1349: Northfield RAOB Buffalo Lodge No. 1247 — not block items
            case BUFFALO_MEMBERSHIP_CARD:
            case BUFFALO_FEZ:
            case KOMPROMAT_LEDGER:
            case BUFFALO_TOKEN:
            case CEREMONIAL_CANE:
            // Issue #1351: Northfield QuickFix Loans — not block items
            case FINAL_DEMAND_LETTER:
            case FORGED_ID:
            // Issue #1353: Northfield Amateur Dramatics Society — not block items
            case STAGE_COSTUME:
            case REHEARSAL_SCHEDULE:
            case FORGED_TICKET:
            case PROP_GUN:
            // Issue #1357: Northfield Charity Fun Run — not block items
            case RACE_NUMBER_BIB:
            case SPONSOR_SHEET:
            case WINNERS_MEDAL:
            case WATER_CUP:
            // Issue #1359: Northfield HMRC Tax Investigation — not block items
            case TAX_DEMAND_LETTER:
            case CLEAN_BILL_OF_HEALTH:
            case CASH_BRIBE_ENVELOPE:
            // Issue #1144: Northfield Probation Office — not block items
            case SIGN_ON_LETTER:
            case ELECTRONIC_TAG:
            case FAKE_SIGNAL_CHIP:
            case COMMUNITY_SERVICE_VEST:
            case CASE_FILE_DOCUMENT:
            case WIRE_CUTTERS:
            case TIN_OF_PAINT:
            // Issue #1146: Mick's MOT & Tyre Centre — not block items
            case TYRE:
            case CAR_PART:
            case BLANK_LOGBOOK:
            case MOT_CERT:
            // Issue #1148: Northfield Council Estate Lock-Up Garages — not block items
            case BRIC_A_BRAC:
            case VINTAGE_RECORD:
            case DRUM_COMPONENT:
            case CABLE:
            case BURNER_PHONE:
            case PIGEON_FEED_BAG:
            case GARAGE_KEY_7:
            // Issue #1151: Northfield Sporting & Social Club — not block items
            case CLUB_MEMBERSHIP_CARD:
            case DARTS_SET:
            case QUIZ_ANSWER_SHEET:
            // Issue #1155: Northfield NHS Dentist — not block items
            case FIZZY_DRINK:
            case HARIBO:
            case TOOTHBRUSH:
            case DENTAL_APPOINTMENT_LETTER:
            case WAITING_LIST_LETTER:
            case FORGED_WAITING_LIST_LETTER:
            // Issue #1157: Northfield Tesco Express — not block items
            case TESCO_SANDWICH:
            case TESCO_PASTA_POT:
            case TESCO_ORANGE_JUICE:
            case TESCO_MEAL_DEAL_BAG:
            case CLUBCARD:
            case CLUBCARD_VOUCHER:
            case TESCO_FINEST_WINE:
            case TESCO_OWN_BRAND_VODKA:
            case READY_MEAL:
            case CLUBCARD_STATEMENT:
            // Issue #1159: Northfield Angel Nails & Beauty — not block items
            case STOLEN_JEWELLERY:
            case GEL_SET:
            case COUNTERFEIT_PERFUME:
            case NAIL_SALON_VOUCHER:
            // Issue #1161: Northfield Poundstretcher — not block items
            case OWN_BRAND_CRISPS:
            case OWN_BRAND_COLA:
            case BARGAIN_BUCKET_CRISPS:
            case WHOLESALE_SPIRITS:
            // Issue #1173: Northfield Balti House — not block items
            case VEGETABLE_BALTI:
            case MANGO_CHUTNEY:
            case BALTI_BOX:
            case NAAN_BAG:
            case BALTI_CATERING_TIN:
            case FAKE_CURRY_POWDER:
            case RESTAURANT_RECEIPT:
            // Issue #1175: Northfield Argos — not block items
            case ARGOS_SLIP:
            case ARGOS_RECEIPT:
            case FORGED_RECEIPT:
            case BLANK_RECEIPT:
            case CATALOGUE_PENCIL:
            // Issue #1181: Northfield Chugger Blitz
            case CHARITY_TABARD:
            case CHARITY_CLIPBOARD:
            case MARKER_PEN:
            // Issue #1186: Northfield Probation Office — not block items
            case ANKLE_TAG:
            case SIGN_IN_FORM_PROP:
            // Issue #1188: Northfield DWP Home Visit — not block items
            case DWP_LETTER_PROP:
            case APPEAL_LETTER_PROP:
            case CASH_IN_HAND_LEDGER:
            // Issue #1381: Northfield Bank Holiday Street Party — not block items
            case WARM_LAGER:
            case CRISP_PACKET:
            case RAW_SAUSAGE:
            case COOKED_SAUSAGE:
            case SELECTION_BOX:
            case NOVELTY_TOWEL:
            case BOTTLE_OF_WINE:
            case TOOLKIT:
            case COMMEMORATIVE_MUG:
            // Issue #1381: Northfield Halloween — not block items
            case RAW_EGG:
            case MINI_CHOCOLATE_BAR:
            case TRICK_OR_TREAT_BAG:
            case WITCH_COSTUME:
            case GHOST_SHEET:
            case SKELETON_SUIT:
            case PUMPKIN_HEAD_MASK:
            case RAW_PUMPKIN:
            case CARVED_PUMPKIN:
            case PUMPKIN_INNARDS:
            case POINTY_HAT:
            case BLACK_CAPE:
            // Issue #1383: Northfield Boxing Day Sales — not block items
            case FROZEN_PRAWN_RING:
            case PARTY_FOOD_PLATTER:
            case LUXURY_BISCUIT_TIN:
            case GEORGE_FOREMAN_GRILL:
            case BREAD_MAKER:
            case VINYL_RECORD_BOX:
            case GENUINE_FIRST_PRESSING:
            case HDMI_CABLE:
            // Issue #1400: Northfield Residents' Parking Permit Racket — not block items
            case PARKING_PERMIT:
            case FORGED_PARKING_PERMIT:
            case WHEEL_CLAMP_KEY:
            case STOLEN_PRINTER_INK:
            // Issue #1402: Northfield Severn Trent Road Dig — not block items
            case THERMOS:
            case SITE_RADIO:
            case CONTRACTOR_CLIPBOARD:
            case HARD_HAT:
            case MYSTERY_OBJECT:
            // Issue #1404: Northfield Community Litter Pick — not block items
            case LITTER_PICKER_STICK:
            case COUNCIL_RUBBISH_BAG:
            case CRACK_PIPE:
                return false;
            // Issue #1406: Northfield Dodgy Roofer — not block items
            case BUCKET_OF_SEALANT:
            case LATEX_GLOVES:
            case SCAFFOLDING_SPANNER:
            case INVOICE_PAD:
            case CASH_ENVELOPE:
            case ROOF_SLATE_BAG:
                return false;
            // Issue #1416: Northfield Mobile Speed Camera Van — not block items
            case SPEED_CAMERA_SD_CARD:
            case SPEEDING_FINE_NOTICE:
            case HANDWRITTEN_WARNING_SIGN:
                return false;
            // Issue #1420: Northfield Post Office Horizon Scandal — not block items
            case CITIZENS_ADVICE_LEAFLET:
            case SHORTFALL_LETTER:
            case TRANSACTION_LOG:
            case STAMPS_BUNDLE:
            case IT_CONTRACTOR_ID_BADGE:
            case USB_STICK:
                return false;
            // Issue #1424: Northfield Doorstep Energy Tout — not block items
            case TOUT_CLIPBOARD:
            case SMART_METER_KIT:
            case CRAIG_WITNESS_STATEMENT:
            case DAVE_DEBT_LIST:
            case REPLACEMENT_CLIPBOARD:
            case FANCIER_FAVOUR:
                return false;
            default:
                return true;
        }
    }

    /**
     * Returns true if this material is a small item that can be placed on top of
     * a block without grid snapping. Small items use precise float coordinates so
     * they can sit freely on any block surface (e.g. a can on a counter, a book
     * on a shelf). They do not occupy a voxel grid cell.
     */
    public boolean isSmallItem() {
        switch (this) {
            case TIN_OF_BEANS:
            case ENERGY_DRINK:
            case PINT:
            case SAUSAGE_ROLL:
            case STEAK_BAKE:
            case CHIPS:
            case KEBAB:
            case PERI_PERI_CHICKEN:
            case CRISPS:
            case NEWSPAPER:
            case TEXTBOOK:
            case HYMN_BOOK:
            case SCRATCH_CARD:
            case BROKEN_PHONE:
            case DODGY_DVD:
            case STAPLER:
            case PARACETAMOL:
            case ANTIDEPRESSANTS:
            case NAIL_POLISH:
            // Issue #799: Corner Shop Economy
            case CIDER:
            case TOBACCO:
            // Issue #908: Bookies Horse Racing System
            case BET_SLIP:
            // Issue #914: Allotment System — produce sits on surfaces
            case POTATO:
            case CARROT:
            case CABBAGE:
            case SUNFLOWER:
            // Issue #938: Greasy Spoon Café — food items sit on surfaces
            case FULL_ENGLISH:
            case MUG_OF_TEA:
            case BEANS_ON_TOAST:
            case FRIED_BREAD:
            case BACON_BUTTY:
            case BUILDER_S_TEA:
            // Issue #950: Northfield Leisure Centre — vending items sit on surfaces
            case CHOCOLATE_BAR:
            case WATER_BOTTLE:
            // Issue #952: Clucky's Fried Chicken — food items sit on surfaces
            case CHICKEN_WINGS:
            case CHICKEN_BOX:
            case CHIPS_AND_GRAVY:
            case FLAT_COLA:
            case CHICKEN_BONE:
            case EMPTY_CHICKEN_BOX:
            // Issue #971: The Rusty Anchor Wetherspoons — drinks/food sit on surfaces
            case CHEAP_SPIRITS:
            case CURRY_CLUB_SPECIAL:
            // Issue #973: Northfield GP Surgery — medicine/paper items sit on surfaces
            case PRESCRIPTION:
            case ANTIBIOTICS:
            case STRONG_MEDS:
            case SICK_NOTE:
            case BLANK_PRESCRIPTION_FORM:
            case NEON_LEAFLET:
            // Issue #1116: Northfield Pharmacy — OTC items sit on surfaces
            case COLD_AND_FLU_SACHET:
            case ANTISEPTIC_CREAM:
            case VITAMIN_C_TABLETS:
            case HAIR_DYE:
            case READING_GLASSES:
            // Issue #975: Northfield Post Office — paper/parcel items sit on surfaces
            case BENEFITS_BOOK:
            case PARCEL:
            case STAMP:
            // Issue #977: Northfield Amusement Arcade — small items sit on surfaces
            case TWOPENCE:
            case PLUSH_TOY:
            case ARCADE_TOKEN:
            // Issue #1004: Northfield Community Centre — Bring & Buy Sale items
            case VINYL_RECORD:
            case THERMOS_FLASK:
            case JIGSAW_PUZZLE_BOX:
            case KNITTING_NEEDLES:
            // Issue #1012: Skin Deep Tattoos — small craft items sit on surfaces
            case NEEDLE:
            case INK_BOTTLE:
            // Issue #1110: Skin Deep Tattoos — new craft items sit on surfaces
            case PRISON_TATTOO_KIT:
            case TATTOO_VOUCHER:
            // Issue #1018: Northfield Poundstretcher — small shop items sit on surfaces
            case BLEACH:
            case GAFFER_TAPE:
            case BAKED_BEANS_TIN:
            case CABLE_TIES:
            case WASHING_UP_LIQUID:
            case CLOTH:
            case CANDLE:
            case TORCH:
            case PADLOCK:
            case MYSTERY_BOX:
            // Issue #1028: Northfield Cash Converters — small electronics sit on shelves
            case DVD:
            case WIPED_PHONE:
            // Issue #1128: NHS Walk-In Centre — small medical items sit on surfaces
            case BANDAGE:
            case MORPHINE_AMPOULE:
            case TRAMADOL:
            case DIAZEPAM:
            case UNUSED_SYRINGE:
            case DISCHARGE_LETTER:
            case PRESCRIPTION_FORM:
            // Issue #1130: Northfield BP Petrol Station — small items sit on surfaces
            case MICROWAVE_PASTY:
            case CAR_WASH_TOKEN:
            case CIGARETTE_CARTON:
            case MAP:
            // Issue #1132: Dog Grooming Parlour — small items sit on surfaces
            case DOG_TREAT:
            case DOG_SHOW_ROSETTE:
            case FLEA_POWDER:
            case SCISSORS:
            case DOG_GROOMING_VOUCHER:
            case UNLICENSED_DOG:
            // Issue #1134: Patel's Newsagent — small items sit on surfaces
            case PENNY_SWEETS:
            case LOCAL_MAP:
            case RACING_FORM:
            case DIY_MONTHLY:
            case PUZZLE_BOOK:
            case DODGY_MAGAZINE:
            case NEWSAGENT_KEY:
            case NEWSAGENT_APRON:
            case PAPIER_MACHE_BRICK:
            // Issue #1136: The Vaults Nightclub
            case SMART_SHIRT:
            case NIGHTCLUB_MASTER_KEY:
            // Issue #1144: Northfield Probation Office — small items sit on surfaces
            case SIGN_ON_LETTER:
            case CASE_FILE_DOCUMENT:
            case FAKE_SIGNAL_CHIP:
            // Issue #1173: Northfield Balti House — food/small items sit on surfaces
            case VEGETABLE_BALTI:
            case MANGO_CHUTNEY:
            case BALTI_BOX:
            case NAAN_BAG:
            case BALTI_CATERING_TIN:
            case FAKE_CURRY_POWDER:
            case RESTAURANT_RECEIPT:
            // Issue #1175: Northfield Argos — paper items sit on surfaces
            case ARGOS_SLIP:
            case ARGOS_RECEIPT:
            case FORGED_RECEIPT:
            case BLANK_RECEIPT:
            case CATALOGUE_PENCIL:
            // Issue #1181: Northfield Chugger Blitz — small items placed on surfaces
            case CHARITY_CLIPBOARD:
            case MARKER_PEN:
            // Issue #1257: Northfield Rag-and-Bone Man
            case JUNK_ITEM:
            case GARDEN_ORNAMENT:
            case PENKNIFE:
            // Issue #1263: Northfield Illegal Street Racing
            case NITROUS_CANISTER:
            case RACING_TROPHY:
            // Issue #1265: Northfield Loan Shark — Big Mick's Doorstep Lending
            case LOAN_AGREEMENT:
            case DEBT_LEDGER:
            // Issue #1351: Northfield QuickFix Loans
            case FINAL_DEMAND_LETTER:
            case FORGED_ID:
            // Issue #1353: Northfield Amateur Dramatics Society — small items sit on surfaces
            case STAGE_COSTUME:
            case REHEARSAL_SCHEDULE:
            case FORGED_TICKET:
            case PROP_GUN:
            // Issue #1357: Northfield Charity Fun Run — small items sit on surfaces
            case RACE_NUMBER_BIB:
            case SPONSOR_SHEET:
            case WINNERS_MEDAL:
            case WATER_CUP:
            // Issue #1359: Northfield HMRC Tax Investigation — small items sit on surfaces
            case TAX_DEMAND_LETTER:
            case CLEAN_BILL_OF_HEALTH:
            case CASH_BRIBE_ENVELOPE:
            // Issue #1381: Northfield Bank Holiday Street Party — small items sit on surfaces
            case WARM_LAGER:
            case CRISP_PACKET:
            case RAW_SAUSAGE:
            case COOKED_SAUSAGE:
            case SELECTION_BOX:
            case NOVELTY_TOWEL:
            case BOTTLE_OF_WINE:
            case TOOLKIT:
            case COMMEMORATIVE_MUG:
            // Issue #1381: Northfield Halloween — small items sit on surfaces
            case RAW_EGG:
            case MINI_CHOCOLATE_BAR:
            case TRICK_OR_TREAT_BAG:
            case RAW_PUMPKIN:
            case CARVED_PUMPKIN:
            case PUMPKIN_INNARDS:
            // Issue #1416: Northfield Mobile Speed Camera Van — small items sit on surfaces
            case SPEED_CAMERA_SD_CARD:
            case SPEEDING_FINE_NOTICE:
            case HANDWRITTEN_WARNING_SIGN:
            // Issue #1420: Northfield Post Office Horizon Scandal — small items sit on surfaces
            case CITIZENS_ADVICE_LEAFLET:
            case SHORTFALL_LETTER:
            case TRANSACTION_LOG:
            case STAMPS_BUNDLE:
            case IT_CONTRACTOR_ID_BADGE:
            case USB_STICK:
                return true;
            default:
                return false;
        }
    }

    /**
     * Describes the shape style used when rendering this material's icon.
     * Only meaningful when isBlockItem() returns false.
     */
    public enum IconShape {
        /** A pickaxe/tool silhouette (handle + head) */
        TOOL,
        /** A folded rectangle (newspaper/book) */
        FLAT_PAPER,
        /** A tall thin rectangle (bottle/can) */
        BOTTLE,
        /** A wide flat oval/rectangle (food on a surface) */
        FOOD,
        /** A rounded rectangle (card/phone/disc) */
        CARD,
        /** A diamond/gem shape */
        GEM,
        /** A box/cube outline */
        BOX,
        /** A cylinder silhouette (extinguisher/petrol can) */
        CYLINDER
    }

    /**
     * Returns the shape style to use when drawing this item's icon.
     */
    public IconShape getIconShape() {
        switch (this) {
            case IMPROVISED_TOOL:
            case STONE_TOOL:
                return IconShape.TOOL;

            case NEWSPAPER:
            case TEXTBOOK:
            case HYMN_BOOK:
            case PARACETAMOL:
            case WASHING_POWDER:
            case ANTIDEPRESSANTS:
                return IconShape.FLAT_PAPER;

            case ENERGY_DRINK:
            case PINT:
            case NAIL_POLISH:
                return IconShape.BOTTLE;

            case SAUSAGE_ROLL:
            case STEAK_BAKE:
            case CHIPS:
            case KEBAB:
            case PERI_PERI_CHICKEN:
            case CRISPS:
                return IconShape.FOOD;

            case SCRATCH_CARD:
            case BROKEN_PHONE:
            case DODGY_DVD:
            case SHILLING:
            case PENNY:
            case COIN:
                return IconShape.CARD;

            case DIAMOND:
                return IconShape.GEM;

            case TIN_OF_BEANS:
            case COMPUTER:
            case OFFICE_CHAIR:
            case STAPLER:
                return IconShape.BOX;

            case PETROL_CAN:
            case PETROL_CAN_FULL:
            case FIRE_EXTINGUISHER:
            case HAIR_CLIPPERS:
            case HAIR_CLIPPERS_BROKEN:
            case COOKING_OIL:
            case MOLOTOV:
                return IconShape.CYLINDER;
            case LARD_BUCKET:
                return IconShape.BOX;

            case HIGH_VIS_JACKET:
                return IconShape.FLAT_PAPER; // vest-like shape
            case CROWBAR:
            case ANGLE_GRINDER:
            case BOLT_CUTTERS:
                return IconShape.TOOL;
            case BALACLAVA:
                return IconShape.CARD; // mask-like card shape
            case DODGY_PASTY:
                return IconShape.FOOD;
            case FOOD:
                return IconShape.FOOD;
            // Clothing items (Issue #698)
            case COAT:
                return IconShape.FLAT_PAPER; // coat silhouette
            case UMBRELLA:
                return IconShape.TOOL;       // umbrella handle shape
            case WOOLLY_HAT:
                return IconShape.CARD;       // hat shape
            case FLASK_OF_TEA:
                return IconShape.CYLINDER;   // flask cylinder
            // Heist tools & loot (Issue #704)
            case GLASS_CUTTER:
                return IconShape.TOOL;
            case ROPE_LADDER:
                return IconShape.TOOL;
            case GOLD_RING:
                return IconShape.CARD;
            case COUNCIL_ID:
                return IconShape.CARD;
            case PASTY:
                return IconShape.FOOD;
            case LEAVES:
                return IconShape.FLAT_PAPER; // flat leaf bundle
            // Slumlord items (Issue #712)
            case DEED:
                return IconShape.FLAT_PAPER; // legal document
            case PAINT_TIN:
                return IconShape.CYLINDER;   // paint tin
            case EVICTION_NOTICE:
                return IconShape.FLAT_PAPER; // official notice paper
            // Squat system items (Issue #714)
            case LOCKPICK:
                return IconShape.TOOL;       // thin metal pick
            case FAKE_ID:
                return IconShape.CARD;       // ID card

            // Underground music scene (Issue #716)
            case MICROPHONE:
                return IconShape.CYLINDER;   // microphone body
            case FLYER:
                return IconShape.FLAT_PAPER; // paper flyer

            // Craftable 3D props (Issue #720)
            case PROP_BED:
                return IconShape.BOX;        // bed frame box shape
            case PROP_WORKBENCH:
                return IconShape.BOX;        // workbench surface box
            case PROP_DARTBOARD:
                return IconShape.CARD;       // round flat board
            case PROP_SPEAKER_STACK:
                return IconShape.BOX;        // cabinet box shape
            case PROP_DISCO_BALL:
                return IconShape.GEM;        // spherical reflective shape
            case PROP_DJ_DECKS:
                return IconShape.BOX;        // flat console box

            // Witness & Evidence items (Issue #765)
            case RUMOUR_NOTE:
                return IconShape.FLAT_PAPER; // small folded note
            case CCTV_TAPE:
                return IconShape.CARD;       // cassette tape shape

            // Disguise items (Issue #767)
            case POLICE_UNIFORM:
            case COUNCIL_JACKET:
            case MARCHETTI_TRACKSUIT:
            case STREET_LADS_HOODIE:
            case HI_VIS_VEST:
            case GREGGS_APRON:
                return IconShape.FLAT_PAPER; // clothing/vest shape

            // Issue #769: Black Market Economy items
            case GREGGS_PASTRY:
                return IconShape.FOOD;
            case PLASTIC_BAG:
                return IconShape.FLAT_PAPER; // carrier bag
            case CAN_OF_LAGER:
                return IconShape.BOTTLE;
            case CIGARETTE:
                return IconShape.TOOL;       // thin stick shape
            case WOOLLY_HAT_ECONOMY:
                return IconShape.CARD;       // hat shape
            case SLEEPING_BAG:
                return IconShape.BOX;        // rolled bag
            case STOLEN_PHONE:
                return IconShape.CARD;       // phone shape
            case PRESCRIPTION_MEDS:
                return IconShape.FLAT_PAPER; // medicine packet
            case COUNTERFEIT_NOTE:
                return IconShape.FLAT_PAPER; // banknote
            case TOBACCO_POUCH:
                return IconShape.CARD;       // small pouch

            // Issue #781: Graffiti materials
            case SPRAY_CAN_EMPTY:
                return IconShape.CYLINDER;   // empty can
            case SPRAY_CAN:
                return IconShape.CYLINDER;   // filled can
            case PAINT_PIGMENT_RED:
            case PAINT_PIGMENT_BLUE:
            case PAINT_PIGMENT_GOLD:
            case PAINT_PIGMENT_WHITE:
            case PAINT_PIGMENT_GREY:
                return IconShape.BOTTLE;     // pigment bottle

            // Issue #783: Pirate FM materials
            case WIRE:
                return IconShape.TOOL;       // coil of wire shape
            case BROADCAST_TAPE:
                return IconShape.CARD;       // cassette tape shape
            case TRANSMITTER_ITEM:
                return IconShape.BOX;        // transmitter unit box shape

            // Issue #785: Dodgy Market Stall materials
            case STALL_FRAME:
                return IconShape.BOX;        // wooden frame structure
            case STALL_AWNING:
                return IconShape.FLAT_PAPER; // flat fabric canopy
            case MARKET_LICENCE:
                return IconShape.FLAT_PAPER; // official licence paper
            case KNOCK_OFF_PERFUME:
                return IconShape.BOTTLE;     // perfume bottle

            // Issue #797: Neighbourhood Watch materials
            case NEIGHBOURHOOD_NEWSLETTER:
                return IconShape.FLAT_PAPER; // folded community newsletter
            case PEACE_OFFERING:
                return IconShape.FOOD;       // sausage roll + coin bundle

            // Issue #799: Corner Shop Economy materials
            case SHOP_KEY:
                return IconShape.TOOL;       // key shape
            case CIDER:
                return IconShape.BOTTLE;     // cider can/bottle
            case TOBACCO:
                return IconShape.FLAT_PAPER; // tobacco packet

            // Issue #801: Underground Fight Night materials
            case FIGHT_CARD:
                return IconShape.FLAT_PAPER; // paper flyer
            case CHAMPIONSHIP_BELT:
                return IconShape.FLAT_PAPER; // belt / strap
            case MOUTH_GUARD:
                return IconShape.CARD;       // small moulded piece
            case RUBBER:
                return IconShape.BOX;        // rubber block/chunk

            // Issue #901: Bista Village Portal
            case BISTA_VILLAGE_PORTAL:
                return IconShape.GEM;        // glowing portal stone / gem shape

            // Issue #906: Busking System
            case BUCKET_DRUM:
                return IconShape.CYLINDER;   // bucket drum cylinder shape

            // Issue #914: Allotment System
            case POTATO_SEED:
            case CARROT_SEED:
            case CABBAGE_SEED:
            case SUNFLOWER_SEED:
                return IconShape.CARD;       // small seed packet
            case POTATO:
            case CARROT:
            case CABBAGE:
                return IconShape.FOOD;       // vegetable food shape
            case SUNFLOWER:
                return IconShape.FLAT_PAPER; // flat flower shape
            case PLOT_DEED:
                return IconShape.FLAT_PAPER; // official deed document

            // Issue #918: Bus System
            case BUS_PASS:
                return IconShape.CARD;        // travelcard shape
            case INSPECTOR_BADGE:
                return IconShape.CARD;        // badge/card shape

            // Issue #922: Skate Park System
            case SKATEBOARD:
                return IconShape.FLAT_PAPER;  // flat deck/board shape

            // Issue #924: Launderette System
            case CLEAN_CLOTHES:
                return IconShape.FLAT_PAPER;  // folded clean laundry
            case BLOODY_HOODIE:
                return IconShape.FLAT_PAPER;  // hoodie / clothing
            case STOLEN_JACKET:
                return IconShape.FLAT_PAPER;  // jacket / clothing

            // Issue #926: Chippy System
            case BATTERED_SAUSAGE:
            case CHIP_BUTTY:
            case MUSHY_PEAS:
            case PICKLED_EGG:
            case FISH_SUPPER:
            case CHIPS_SEASONED:
                return IconShape.FOOD;        // food item
            case SALT_AND_VINEGAR_PACKET:
                return IconShape.FLAT_PAPER;  // condiment packet
            case BOTTLE_OF_WATER:
                return IconShape.BOTTLE;      // water bottle
            case WHITE_PAPER:
                return IconShape.FLAT_PAPER;  // paper wrapping

            // Issue #928: Public Library System
            case DIY_MANUAL:
                return IconShape.FLAT_PAPER;  // booklet/manual shape
            case NEGOTIATION_BOOK:
                return IconShape.FLAT_PAPER;  // book shape
            case STREET_LAW_PAMPHLET:
                return IconShape.FLAT_PAPER;  // pamphlet/paper shape

            // Issue #932: Ice Cream Van System
            case ICE_CREAM_99:
                return IconShape.FOOD;        // ice cream cone
            case SCREWBALL:
                return IconShape.CYLINDER;    // lolly cylinder shape
            case FAB_LOLLY:
                return IconShape.CYLINDER;    // lolly cylinder shape
            case CHOC_ICE:
                return IconShape.BOX;         // rectangular bar shape
            case OYSTER_CARD_LOLLY:
                return IconShape.CARD;        // card-shaped lolly
            case BUBBLEGUM:
                return IconShape.FOOD;        // small round sweet

            // Issue #1069: Ice Cream Van System (Dave's Ices)
            case NINETY_NINE_FLAKE:
                return IconShape.FOOD;        // ice cream cone
            case LOLLY:
                return IconShape.CYLINDER;    // lolly stick cylinder
            case WAFER_TUBS:
                return IconShape.BOX;         // small tub
            case BANANA_SPLIT:
                return IconShape.CYLINDER;    // lolly shape
            case FLAKE_99_WITH_SAUCE:
                return IconShape.FOOD;        // cone with sauce

            // Issue #934: Pigeon Racing System
            case RACING_PIGEON:
                return IconShape.CARD;        // pigeon shape (compact bird silhouette)
            case BREAD_CRUST:
                return IconShape.FOOD;        // flat bread crust
            case PIGEON_TROPHY:
                return IconShape.CYLINDER;    // trophy cup shape

            // Issue #936: Council Skip & Bulky Item Day
            case OLD_SOFA:
                return IconShape.BOX;         // sofa bulk shape
            case BROKEN_TELLY:
                return IconShape.BOX;         // TV box shape
            case WONKY_CHAIR:
                return IconShape.BOX;         // chair shape
            case CARPET_ROLL:
                return IconShape.CYLINDER;    // rolled-up carpet
            case OLD_MATTRESS:
                return IconShape.FLAT_PAPER;  // flat mattress silhouette
            case FILING_CABINET:
                return IconShape.BOX;         // metal cabinet
            case EXERCISE_BIKE:
                return IconShape.BOX;         // bike frame bulk
            case BOX_OF_RECORDS:
                return IconShape.BOX;         // cardboard box of vinyl
            case MICROWAVE:
                return IconShape.BOX;         // white box appliance
            case SHOPPING_TROLLEY_GOLD:
                return IconShape.BOX;         // trolley frame
            case ANTIQUE_CLOCK:
                return IconShape.CYLINDER;    // mantel clock cylinder
            case HOT_PASTRY:
                return IconShape.FOOD;        // pastry food item
            case LUXURY_BED:
                return IconShape.BOX;         // bed frame box
            case DWP_LETTER:
                return IconShape.FLAT_PAPER;  // official letter

            // Issue #938: Greasy Spoon Café
            case FULL_ENGLISH:
                return IconShape.FOOD;        // full plate of food
            case MUG_OF_TEA:
                return IconShape.BOTTLE;      // mug shape
            case BEANS_ON_TOAST:
                return IconShape.FOOD;        // food on toast
            case FRIED_BREAD:
                return IconShape.FOOD;        // flat fried slice
            case BACON_BUTTY:
                return IconShape.FOOD;        // bread roll with filling
            case BUILDER_S_TEA:
                return IconShape.BOTTLE;      // large mug shape
            case CHALKBOARD:
                return IconShape.FLAT_PAPER;  // flat board

            // Issue #948: Hand Car Wash
            case SQUEEGEE:
                return IconShape.TOOL;        // long-handled squeegee

            // Issue #950: Northfield Leisure Centre
            case CHOCOLATE_BAR:
                return IconShape.FOOD;        // chocolate bar shape
            case WATER_BOTTLE:
                return IconShape.BOTTLE;      // plastic bottle
            case SWIM_TRUNKS:
                return IconShape.FLAT_PAPER;  // flat garment silhouette

            // Issue #952: Clucky's Fried Chicken
            case CHICKEN_WINGS:
                return IconShape.FOOD;        // wings on a plate
            case CHICKEN_BOX:
                return IconShape.BOX;         // takeaway box
            case CHIPS_AND_GRAVY:
                return IconShape.FOOD;        // chips with gravy
            case FLAT_COLA:
                return IconShape.BOTTLE;      // can shape
            case CHICKEN_BONE:
                return IconShape.FOOD;        // gnawed bone
            case EMPTY_CHICKEN_BOX:
                return IconShape.BOX;         // discarded box

            // Issue #961: Cash4Gold Pawn Shop
            case GUITAR:
                return IconShape.TOOL;        // long instrument shape

            // Issue #963: Northfield Canal
            case FISHING_ROD:
                return IconShape.TOOL;        // long rod shape
            case FISH_ROACH:
            case FISH_BREAM:
            case FISH_PERCH:
            case FISH_PIKE:
                return IconShape.FOOD;        // fish food item
            case ROPE:
                return IconShape.TOOL;        // coiled rope / tool shape

            // Issue #965: Northfield Snooker Hall
            case CUE:
                return IconShape.TOOL;        // long cue shaft
            case CHALK_CUBE:
                return IconShape.BOX;         // small cube

            // Issue #967: Northfield Taxi Rank
            case TAXI_PASS:
                return IconShape.CARD;        // prepaid card shape
            case DODGY_PACKAGE:
                return IconShape.BOX;         // plain wrapped box

            // Issue #969: Northfield Cemetery
            case SPADE:
                return IconShape.TOOL;        // long-handled spade
            case WEDDING_RING:
                return IconShape.GEM;         // ring / gem shape
            case POCKET_WATCH:
                return IconShape.CYLINDER;    // round watch case
            case CONDOLENCE_CARD:
                return IconShape.FLAT_PAPER;  // greeting card shape
            case OLD_PHOTOGRAPH:
                return IconShape.FLAT_PAPER;  // flat photo print
            case OLD_COIN:
                return IconShape.CYLINDER;    // round coin disc

            // Issue #971: The Rusty Anchor Wetherspoons
            case CHEAP_SPIRITS:
                return IconShape.BOTTLE;      // small spirits bottle
            case CURRY_CLUB_SPECIAL:
                return IconShape.FOOD;        // curry dish

            // Issue #973: Northfield GP Surgery
            case PRESCRIPTION:
                return IconShape.FLAT_PAPER;  // paper prescription slip
            case ANTIBIOTICS:
                return IconShape.FLAT_PAPER;  // medicine packet
            case STRONG_MEDS:
                return IconShape.FLAT_PAPER;  // medicine packet (red)
            case SICK_NOTE:
                return IconShape.FLAT_PAPER;  // official note paper
            case BLANK_PRESCRIPTION_FORM:
                return IconShape.FLAT_PAPER;  // blank paper form
            case NEON_LEAFLET:
                return IconShape.FLAT_PAPER;  // folded leaflet

            // Issue #1116: Northfield Pharmacy
            case COLD_AND_FLU_SACHET:
                return IconShape.BOX;         // blue sachet packet
            case ANTISEPTIC_CREAM:
                return IconShape.CYLINDER;    // cream tube
            case VITAMIN_C_TABLETS:
                return IconShape.CYLINDER;    // tablet pot
            case HAIR_DYE:
                return IconShape.BOX;         // dye box
            case READING_GLASSES:
                return IconShape.FLAT_PAPER;  // glasses (flat depiction)

            // Issue #975: Northfield Post Office
            case BENEFITS_BOOK:
                return IconShape.FLAT_PAPER;  // blue booklet
            case PARCEL:
                return IconShape.BOX;         // wrapped cardboard box
            case STAMP:
                return IconShape.FLAT_PAPER;  // small red stamp
            case POST_BOX_PROP:
                return IconShape.CYLINDER;    // red post box cylinder

            // Issue #977: Northfield Amusement Arcade
            case TWOPENCE:
                return IconShape.CYLINDER;    // small coin disc
            case PLUSH_TOY:
                return IconShape.BOX;         // stuffed toy shape
            case ARCADE_TOKEN:
                return IconShape.CYLINDER;    // token disc
            case SCREWDRIVER:
                return IconShape.TOOL;        // long screwdriver handle

            // Issue #1000: Northfield Fire Station
            case FIREFIGHTER_HELMET:
                return IconShape.BOX;         // helmet shape — boxy silhouette
            case FIRE_AXE:
                return IconShape.TOOL;        // long-handled axe
            case HOSE_REEL:
                return IconShape.CYLINDER;    // coiled drum shape
            case DISPOSABLE_LIGHTER:
                return IconShape.BOX;         // small rectangular lighter

            // Issue #1004: Northfield Community Centre
            case VINYL_RECORD:
                return IconShape.CYLINDER;    // round record disc
            case THERMOS_FLASK:
                return IconShape.CYLINDER;    // cylindrical flask
            case JIGSAW_PUZZLE_BOX:
                return IconShape.BOX;         // flat box shape
            case KNITTING_NEEDLES:
                return IconShape.TOOL;        // long needle shafts

            // Issue #1012: Skin Deep Tattoos
            case NEEDLE:
                return IconShape.TOOL;        // thin needle shaft
            case INK_BOTTLE:
                return IconShape.CYLINDER;    // small glass bottle
            case TATTOO_GUN:
                return IconShape.TOOL;        // handheld tattoo gun

            // Issue #1110: Skin Deep Tattoos — new craft items
            case PRISON_TATTOO_KIT:
                return IconShape.TOOL;        // improvised kit bundle
            case TATTOO_VOUCHER:
                return IconShape.FLAT_PAPER;  // gift voucher card

            // Issue #1016: Northfield Canal
            case CANAL_FISH:
                return IconShape.FOOD;        // fish silhouette
            case DINGHY:
                return IconShape.BOX;         // inflatable dinghy shape
            case BOOT:
                return IconShape.BOX;         // single boot shape
            case STOLEN_WALLET:
                return IconShape.CARD;        // wallet shape
            case STRING_ITEM:
                return IconShape.TOOL;        // coil of string
            case MOORING_NOTICE:
                return IconShape.FLAT_PAPER;  // council notice paper
            case BIRDWATCHING_TIP:
                return IconShape.FLAT_PAPER;  // folded note
            case CAMPING_LANTERN:
                return IconShape.CYLINDER;    // cylindrical lantern
            case CAMPING_STOVE:
                return IconShape.BOX;         // compact stove unit

            // Issue #1018: Northfield Poundstretcher
            case BLEACH:
                return IconShape.BOTTLE;      // cleaning bottle
            case GAFFER_TAPE:
                return IconShape.CYLINDER;    // tape roll cylinder
            case KNOCK_OFF_TRACKSUIT:
                return IconShape.FLAT_PAPER;  // clothing/garment shape
            case BAKED_BEANS_TIN:
                return IconShape.CYLINDER;    // tin can cylinder
            case CABLE_TIES:
                return IconShape.TOOL;        // bundled ties
            case WASHING_UP_LIQUID:
                return IconShape.BOTTLE;      // squeeze bottle
            case MOP:
                return IconShape.TOOL;        // long-handled mop
            case CLOTH:
                return IconShape.FLAT_PAPER;  // flat fabric square
            case CANDLE:
                return IconShape.CYLINDER;    // candle cylinder
            case TORCH:
                return IconShape.TOOL;        // torch/rod shape
            case PADLOCK:
                return IconShape.BOX;         // padlock body
            case MYSTERY_BOX:
                return IconShape.BOX;         // cardboard box
            case SLIPPERY_FLOOR_TRAP:
                return IconShape.FLAT_PAPER;  // flat trap on floor
            case MAKESHIFT_ARMOUR:
                return IconShape.FLAT_PAPER;  // taped-together vest
            case DUCT_TAPE_RESTRAINT:
                return IconShape.TOOL;        // bound restraint strip
            case IMPROVISED_PEPPER_SPRAY:
                return IconShape.BOTTLE;      // small spray bottle
            case MAKESHIFT_TORCH:
                return IconShape.TOOL;        // torch/handle shape

            // Issue #1020: Northfield Sporting & Social Club
            case BITTER:
            case MILD:
            case LAGER_TOP:
                return IconShape.BOTTLE;      // pint glass shape
            case MEMBERS_CARD:
                return IconShape.CARD;        // membership card
            case DODGY_LEDGER:
                return IconShape.FLAT_PAPER;  // ledger/notebook
            case QUIZ_SHEET:
                return IconShape.FLAT_PAPER;  // answer sheet

            // Issue #1024: Sultan's Kebab
            case DONER_KEBAB:
            case SHISH_KEBAB:
            case CHIPS_IN_PITTA:
            case GARLIC_BREAD_SLICE:
            case COOKED_DONER_MEAT:
                return IconShape.FOOD;        // wrapped kebab / food item
            case DONER_MEAT:
                return IconShape.FOOD;        // raw meat
            case PITTA_BREAD:
                return IconShape.FLAT_PAPER;  // flat bread shape
            case TOOL_KIT:
                return IconShape.BOX;         // toolbox

            // Issue #1026: Northfield Scrapyard
            case COPPER_WIRE:
                return IconShape.TOOL;        // coiled wire
            case LEAD_FLASHING:
                return IconShape.FLAT_PAPER;  // flat sheet of lead
            case COPPER_BALE:
                return IconShape.BOX;         // compressed bale

            // Issue #1028: Northfield Cash Converters
            case DVD:
                return IconShape.CARD;        // disc in case
            case BLUETOOTH_SPEAKER:
                return IconShape.BOX;         // compact speaker box
            case TABLET:
                return IconShape.FLAT_PAPER;  // flat touchscreen
            case GAMES_CONSOLE:
                return IconShape.BOX;         // console unit
            case LAPTOP:
                return IconShape.BOX;         // laptop shape
            case WIPED_PHONE:
                return IconShape.CARD;        // phone shape

            // Issue #1053: Ladbrokes BettingShopSystem
            case BETTING_SLIP_BLANK:
                return IconShape.FLAT_PAPER;  // blank paper betting slip
            case RACING_POST:
                return IconShape.FLAT_PAPER;  // folded newspaper

            // Issue #1055: Northfield War Memorial — StatueSystem
            case BREAD_CRUMBS:
                return IconShape.FOOD;        // scattered crumbs
            case CHAIN:
                return IconShape.TOOL;        // chain links
            case FIREWORK:
                return IconShape.TOOL;        // firework tube
            case TIME_CAPSULE:
                return IconShape.BOX;         // sealed tin
            case POPPY:
                return IconShape.FOOD;        // small flower
            case CLEANING_SUPPLIES:
                return IconShape.BOTTLE;      // cleaning bottle
            case PLACARD:
                return IconShape.FLAT_PAPER;  // placard board

            // Issue #1071: Northfield Fast Cash Finance
            case LOAN_LEAFLET:
                return IconShape.FLAT_PAPER;  // fluorescent flyer
            case THREATENING_LETTER:
                return IconShape.FLAT_PAPER;  // demand letter in envelope
            case DEBT_ADVICE_LETTER:
                return IconShape.FLAT_PAPER;  // CAB advice letter

            // Issue #1081: Pet Shop items
            case DOG_TREATS:
                return IconShape.BOX;         // treat box
            case DOG_LEAD:
                return IconShape.TOOL;        // leather lead
            case DOG_VET_RECORD:
                return IconShape.FLAT_PAPER;  // official record card
            case DOG_SEDATIVE:
                return IconShape.BOTTLE;      // syringe/vial
            case BUDGIE:
                return IconShape.BOX;         // small bird cage shape
            case GOLDFISH:
                return IconShape.BOX;         // bag of water with fish

            // Issue #1128: NHS Walk-In Centre
            case BANDAGE:
                return IconShape.FLAT_PAPER;  // rolled bandage / dressing
            case MORPHINE_AMPOULE:
                return IconShape.BOTTLE;      // glass ampoule vial
            case TRAMADOL:
                return IconShape.FLAT_PAPER;  // blister packet
            case DIAZEPAM:
                return IconShape.FLAT_PAPER;  // tablet packet
            case UNUSED_SYRINGE:
                return IconShape.TOOL;        // syringe barrel + plunger
            case DISCHARGE_LETTER:
                return IconShape.FLAT_PAPER;  // official letter paper
            case PRESCRIPTION_FORM:
                return IconShape.FLAT_PAPER;  // NHS prescription slip

            // Issue #1130: Northfield BP Petrol Station
            case MICROWAVE_PASTY:
                return IconShape.FOOD;        // pasty / savoury parcel
            case CAR_WASH_TOKEN:
                return IconShape.BOX;         // round token
            case CIGARETTE_CARTON:
                return IconShape.BOX;         // rectangular carton
            case MAP:
                return IconShape.FLAT_PAPER;  // folded road map

            // Issue #1132: Dog Grooming Parlour
            case DOG_TREAT:
                return IconShape.FOOD;        // bone-shaped biscuit
            case DOG_SHOW_ROSETTE:
                return IconShape.CARD;        // circular ribbon rosette
            case FLEA_POWDER:
                return IconShape.BOTTLE;      // powder shaker bottle
            case SCISSORS:
                return IconShape.TOOL;        // scissor shape
            case DOG_GROOMING_VOUCHER:
                return IconShape.FLAT_PAPER;  // voucher card
            case UNLICENSED_DOG:
                return IconShape.BOX;         // dog-shaped silhouette

            // Issue #1134: Patel's Newsagent
            case PENNY_SWEETS:
                return IconShape.FOOD;        // small paper bag of sweets
            case LOCAL_MAP:
                return IconShape.FLAT_PAPER;  // folded street map
            case RACING_FORM:
                return IconShape.FLAT_PAPER;  // folded racing magazine
            case DIY_MONTHLY:
                return IconShape.FLAT_PAPER;  // glossy magazine
            case PUZZLE_BOOK:
                return IconShape.FLAT_PAPER;  // A5 book shape
            case DODGY_MAGAZINE:
                return IconShape.FLAT_PAPER;  // magazine in brown wrapper
            case NEWSAGENT_KEY:
                return IconShape.TOOL;        // small Yale key
            case NEWSAGENT_APRON:
                return IconShape.CARD;        // folded tabard
            case PAPIER_MACHE_BRICK:
                return IconShape.BOX;         // brick-shaped lump

            // Issue #1136: The Vaults Nightclub
            case SMART_SHIRT:
                return IconShape.CARD;        // folded shirt
            case NIGHTCLUB_MASTER_KEY:
                return IconShape.TOOL;        // Yale key

            // Issue #1142: Northfield RAOB Lodge
            case RAOB_MEMBERSHIP_CARD:
                return IconShape.CARD;        // lodge membership card
            case SPONSORSHIP_FORM:
                return IconShape.FLAT_PAPER;  // official form
            case LODGE_CHARTER_DOCUMENT:
                return IconShape.FLAT_PAPER;  // aged parchment document
            case REGALIA_SET:
                return IconShape.BOX;         // folded ceremonial regalia
            case CEREMONIAL_MALLET:
                return IconShape.TOOL;        // wooden mallet
            case PREMIUM_LAGER_CRATE:
                return IconShape.BOX;         // wooden crate of lager
            case BOX_OF_CHOCOLATES:
                return IconShape.BOX;         // rectangular chocolate box
            case PLANNING_PERMISSION:
                return IconShape.FLAT_PAPER;  // official stamped document
            case CASE_DISMISSED_FORM:
                return IconShape.FLAT_PAPER;  // court dismissal form
            case RACING_TIP:
                return IconShape.FLAT_PAPER;  // small paper tip slip

            // Issue #1349: Northfield RAOB Buffalo Lodge No. 1247
            case BUFFALO_MEMBERSHIP_CARD:
                return IconShape.CARD;        // crimson Lodge membership card
            case BUFFALO_FEZ:
                return IconShape.BOX;         // small fez hat
            case KOMPROMAT_LEDGER:
                return IconShape.BOX;         // black leatherette ledger
            case BUFFALO_TOKEN:
                return IconShape.CARD;        // brass token coin
            case CEREMONIAL_CANE:
                return IconShape.TOOL;        // ornate walking cane

            // Issue #1144: Northfield Probation Office
            case SIGN_ON_LETTER:
                return IconShape.FLAT_PAPER;  // appointment letter
            case ELECTRONIC_TAG:
                return IconShape.CARD;        // ankle device
            case FAKE_SIGNAL_CHIP:
                return IconShape.CARD;        // chip component
            case COMMUNITY_SERVICE_VEST:
                return IconShape.FLAT_PAPER;  // folded hi-vis vest
            case CASE_FILE_DOCUMENT:
                return IconShape.FLAT_PAPER;  // manila folder contents
            case WIRE_CUTTERS:
                return IconShape.TOOL;        // cutting tool
            case TIN_OF_PAINT:
                return IconShape.CYLINDER;    // paint tin

            // Issue #1146: Mick's MOT & Tyre Centre
            case TYRE:
                return IconShape.CYLINDER;    // round rubber tyre
            case CAR_PART:
                return IconShape.BOX;         // generic salvage part
            case BLANK_LOGBOOK:
                return IconShape.FLAT_PAPER;  // official V5C document
            case MOT_CERT:
                return IconShape.FLAT_PAPER;  // MOT pass certificate

            // Issue #1148: Northfield Council Estate Lock-Up Garages
            case BRIC_A_BRAC:
                return IconShape.BOX;         // assorted junk
            case VINTAGE_RECORD:
                return IconShape.CYLINDER;    // vinyl record disc
            case DRUM_COMPONENT:
                return IconShape.CYLINDER;    // drum shell/hardware piece
            case CABLE:
                return IconShape.TOOL;        // coiled cable
            case BURNER_PHONE:
                return IconShape.CARD;        // flat phone shape
            case PIGEON_FEED_BAG:
                return IconShape.BOX;         // large sack
            case GARAGE_KEY_7:
                return IconShape.TOOL;        // small Yale key

            // Issue #1151: Northfield Sporting & Social Club
            case CLUB_MEMBERSHIP_CARD:
                return IconShape.CARD;        // laminated membership card
            case DARTS_SET:
                return IconShape.TOOL;        // steel-tip darts
            case QUIZ_ANSWER_SHEET:
                return IconShape.FLAT_PAPER;  // answer sheet

            // Issue #1153: Northfield Community Centre
            case AEROBICS_PASS:
                return IconShape.CARD;        // laminated day pass
            case CAB_REFERRAL_LETTER:
                return IconShape.FLAT_PAPER;  // official referral letter
            case CHARACTER_REFERENCE_LETTER:
                return IconShape.FLAT_PAPER;  // character reference form
            case GRANT_APPLICATION_FORM:
                return IconShape.FLAT_PAPER;  // council grant form
            case FORGED_GRANT_APPLICATION:
                return IconShape.FLAT_PAPER;  // forged form
            case GRANT_CHEQUE:
                return IconShape.FLAT_PAPER;  // council cheque
            case COUNTERFEIT_FLYER:
                return IconShape.FLAT_PAPER;  // A5 flyer
            case CURRY_AND_RICE:
                return IconShape.FOOD;        // curry portion in tray
            case NAAN:
                return IconShape.FOOD;        // flatbread
            case SAMOSA:
                return IconShape.FOOD;        // triangular pastry
            case MANGO_LASSI:
                return IconShape.BOTTLE;      // yoghurt drink cup

            // Issue #1155: Northfield NHS Dentist
            case FIZZY_DRINK:
                return IconShape.BOTTLE;      // can of fizzy drink
            case HARIBO:
                return IconShape.BOX;         // bag of gummies
            case TOOTHBRUSH:
                return IconShape.TOOL;        // long-handled brush
            case DENTAL_APPOINTMENT_LETTER:
                return IconShape.FLAT_PAPER;  // NHS appointment letter
            case WAITING_LIST_LETTER:
                return IconShape.FLAT_PAPER;  // official NHS letter
            case FORGED_WAITING_LIST_LETTER:
                return IconShape.FLAT_PAPER;  // forged NHS letter

            // Issue #1159: Northfield Angel Nails & Beauty
            case STOLEN_JEWELLERY:
                return IconShape.BOX;         // small jewellery box
            case GEL_SET:
                return IconShape.BOX;         // small gel pot
            case COUNTERFEIT_PERFUME:
                return IconShape.BOTTLE;      // perfume bottle
            case NAIL_SALON_VOUCHER:
                return IconShape.FLAT_PAPER;  // voucher card

            // Issue #1157: Northfield Tesco Express
            case TESCO_SANDWICH:
                return IconShape.FOOD;        // triangular sandwich pack
            case TESCO_PASTA_POT:
                return IconShape.FOOD;        // pasta pot with lid
            case TESCO_ORANGE_JUICE:
                return IconShape.BOTTLE;      // small OJ carton
            case TESCO_MEAL_DEAL_BAG:
                return IconShape.BOX;         // branded paper bag
            case CLUBCARD:
                return IconShape.CARD;        // loyalty card
            case CLUBCARD_VOUCHER:
                return IconShape.FLAT_PAPER;  // voucher slip
            case TESCO_FINEST_WINE:
                return IconShape.BOTTLE;      // wine bottle
            case TESCO_OWN_BRAND_VODKA:
                return IconShape.BOTTLE;      // vodka bottle
            case READY_MEAL:
                return IconShape.FOOD;        // plastic tray meal
            case CLUBCARD_STATEMENT:
                return IconShape.FLAT_PAPER;  // statement letter

            // Issue #1161: Northfield Poundstretcher
            case OWN_BRAND_CRISPS:
                return IconShape.BOX;         // crisp packet
            case OWN_BRAND_COLA:
                return IconShape.BOTTLE;      // cola can
            case BARGAIN_BUCKET_CRISPS:
                return IconShape.BOX;         // large crisp bag
            case WHOLESALE_SPIRITS:
                return IconShape.BOTTLE;      // unlabelled bottle

            // Issue #1165: Northfield Match Day
            case COUNTERFEIT_TICKET:
                return IconShape.FLAT_PAPER;  // paper ticket
            case KNOCKOFF_SCARF:
                return IconShape.FLAT_PAPER;  // folded scarf
            case MATCH_PROGRAMME:
                return IconShape.FLAT_PAPER;  // programme booklet
            case WALLET_FAN:
                return IconShape.CARD;        // flat wallet
            case FABRIC_SCRAP:
                return IconShape.FLAT_PAPER;  // scrap of fabric

            // Issue #1167: Northfield Amateur Boxing Club
            case GYM_MEMBERSHIP_CARD:
                return IconShape.CARD;        // laminated gym card
            case FIGHT_ENTRY_FORM:
                return IconShape.FLAT_PAPER;  // official sign-up form
            case BOXING_GLOVES:
                return IconShape.BOX;         // pair of gloves
            case LOADED_GLOVE:
                return IconShape.BOX;         // weighted glove
            case FIGHT_PURSE:
                return IconShape.BOX;         // envelope with prize money
            case PROTEIN_BAR:
                return IconShape.FOOD;        // wrapped energy bar
            case SPEED_BAG_CHALK:
                return IconShape.BOX;         // chalk block
            case ABA_TROPHY:
                return IconShape.CYLINDER;    // trophy cup shape

            // Issue #1171: Northfield TV Licence
            case TV_LICENCE_LETTER:
                return IconShape.FLAT_PAPER;  // buff envelope
            case TV_LICENCE_CERTIFICATE:
                return IconShape.FLAT_PAPER;  // official certificate
            case FORGED_TV_LICENCE:
                return IconShape.FLAT_PAPER;  // forged certificate

            // Issue #1173: Northfield Balti House
            case VEGETABLE_BALTI:
                return IconShape.FOOD;        // balti bowl
            case MANGO_CHUTNEY:
                return IconShape.FOOD;        // small condiment pot
            case BALTI_BOX:
                return IconShape.BOX;         // cardboard takeaway box
            case NAAN_BAG:
                return IconShape.FLAT_PAPER;  // paper naan bag
            case BALTI_CATERING_TIN:
                return IconShape.CYLINDER;    // large catering tin
            case FAKE_CURRY_POWDER:
                return IconShape.BOX;         // spice packet
            case RESTAURANT_RECEIPT:
                return IconShape.FLAT_PAPER;  // till receipt

            // Issue #1175: Northfield Argos
            case ARGOS_SLIP:
                return IconShape.FLAT_PAPER;  // collection slip
            case ARGOS_RECEIPT:
                return IconShape.FLAT_PAPER;  // proof of purchase
            case FORGED_RECEIPT:
                return IconShape.FLAT_PAPER;  // dodgy receipt
            case BLANK_RECEIPT:
                return IconShape.FLAT_PAPER;  // thermal paper
            case CATALOGUE_PENCIL:
                return IconShape.CYLINDER;    // tiny pencil

            // Issue #1181: Northfield Chugger Blitz
            case CHARITY_TABARD:
                return IconShape.FLAT_PAPER;  // wearable tabard (folded)
            case CHARITY_CLIPBOARD:
                return IconShape.FLAT_PAPER;  // clipboard with form
            case MARKER_PEN:
                return IconShape.CYLINDER;    // marker pen

            // Issue #1186: Northfield Probation Office
            case ANKLE_TAG:
                return IconShape.BOX;         // black plastic device
            case SIGN_IN_FORM_PROP:
                return IconShape.FLAT_PAPER;  // A4 sign-in register

            // Issue #1188: Northfield DWP Home Visit
            case DWP_LETTER_PROP:
                return IconShape.FLAT_PAPER;  // brown envelope letter
            case APPEAL_LETTER_PROP:
                return IconShape.FLAT_PAPER;  // official appeal letter
            case CASH_IN_HAND_LEDGER:
                return IconShape.BOX;         // small notebook

            // Issue #1190: Information Broker
            case PAPER:
                return IconShape.FLAT_PAPER;  // blank sheet
            case PEN:
                return IconShape.CYLINDER;    // biro pen

            // Issue #1196: Environmental Health Officer
            case HYGIENE_STICKER:
                return IconShape.FLAT_PAPER;  // small sticker
            case FORGED_FIVE_STAR_STICKER:
                return IconShape.FLAT_PAPER;  // forged sticker
            case IMPROVEMENT_NOTICE:
                return IconShape.FLAT_PAPER;  // official notice
            case ANONYMOUS_NOTE:
                return IconShape.FLAT_PAPER;  // folded note

            // Issue #1198: Northfield Traffic Warden
            case PAY_AND_DISPLAY_TICKET:
                return IconShape.FLAT_PAPER;  // small ticket slip
            case PENALTY_CHARGE_NOTICE:
                return IconShape.FLAT_PAPER;  // yellow PCN envelope
            case FORGED_PARKING_TICKET:
                return IconShape.FLAT_PAPER;  // forged ticket slip
            case WHEEL_CLAMP:
                return IconShape.BOX;         // heavy metal device
            case CLIVE_TERMINAL:
                return IconShape.BOX;         // hand-held enforcement terminal

            // Issue #1205: Northfield DVSA Test Centre
            case DRIVING_LICENCE:
                return IconShape.CARD;        // laminated pink licence card
            case THEORY_PASS_CERTIFICATE:
                return IconShape.FLAT_PAPER;  // printed certificate
            case PENDING_TEST_RESULT_ITEM:
                return IconShape.FLAT_PAPER;  // manila envelope

            // Issue #1209: Citizens Advice Bureau
            case BENEFIT_APPEAL_LETTER:
                return IconShape.FLAT_PAPER;  // official CAB letter
            case DEBT_MANAGEMENT_LETTER:
                return IconShape.FLAT_PAPER;  // debt plan letter
            case EVICTION_LETTER:
                return IconShape.FLAT_PAPER;  // eviction notice
            case BAILIFF_DISPUTE_LETTER:
                return IconShape.FLAT_PAPER;  // dispute letter
            case GRIEVANCE_LETTER:
                return IconShape.FLAT_PAPER;  // employment grievance letter
            case FORGED_BENEFIT_LETTER:
                return IconShape.FLAT_PAPER;  // forged official letter
            case FORGED_LANDLORD_REFERENCE:
                return IconShape.FLAT_PAPER;  // forged reference letter
            case FORGED_COURT_SUMMONS:
                return IconShape.FLAT_PAPER;  // forged court document
            case CAB_REFERRAL_FORM:
                return IconShape.FLAT_PAPER;  // referral form
            case DEBT_STATEMENT_ITEM:
                return IconShape.FLAT_PAPER;  // handwritten statement

            // Issue #1216: Northfield Driving Instructor
            case FORGED_PASS_CERTIFICATE:
                return IconShape.FLAT_PAPER;  // forged DVSA pass slip
            case CAR_KEY_COPY:
                return IconShape.TOOL;        // cut key
            case FULL_DRIVING_LICENCE:
                return IconShape.CARD;        // laminated licence card

            // Issue #1218: Northfield Claims Management Company
            case NECK_BRACE:
                return IconShape.BOX;         // foam cervical collar
            case CLAIM_REFERENCE_SLIP:
                return IconShape.FLAT_PAPER;  // crumpled payout receipt

            // Issue #1227: Northfield Wheelwright Motors
            case MILEAGE_CORRECTOR:
                return IconShape.TOOL;        // handheld OBD clocking device
            case FAKE_V5C:
                return IconShape.FLAT_PAPER;  // forged V5C logbook
            case V5C_PROP:
                return IconShape.FLAT_PAPER;  // legitimate V5C logbook

            // Issue #1229: Northfield Handy Builders
            case CEMENT_BAG:
                return IconShape.BOX;         // paper cement bag
            case SAND_BAG:
                return IconShape.BOX;         // fabric sand bag
            case WIRE_REEL:
                return IconShape.CYLINDER;    // cable reel
            case COPPER_PIPE:
                return IconShape.CYLINDER;    // copper tube
            case MORTAR:
                return IconShape.BOX;         // tub of mortar mix
            case TRADE_ACCOUNT_CARD:
                return IconShape.CARD;        // credit/account card
            case FAKE_TRADE_ACCOUNT_CARD:
                return IconShape.CARD;        // forged account card

            // Issue #1231: Northfield ASBO System
            case ASBO_LETTER:
                return IconShape.FLAT_PAPER;  // official court notification letter
            case ASBO_ORDER_DOCUMENT:
                return IconShape.FLAT_PAPER;  // laminated ASBO order document

            // Issue #1235: Northfield Sporting & Social Club
            case PORK_CHOPS:
                return IconShape.BOX;         // butcher-wrapped tray
            case CHICKEN_LEGS:
                return IconShape.BOX;         // bag of drumsticks
            case SAUSAGES:
                return IconShape.CYLINDER;    // ring of sausages
            case BACK_ROOM_KEY:
                return IconShape.FLAT_PAPER;  // small brass Yale key
            case INCRIMINATING_DOCUMENT:
                return IconShape.FLAT_PAPER;  // folded planning document

            // Issue #1240: Northfield NHS Blood Donation Session
            case BLOOD_BAG:
                return IconShape.BOX;         // sealed blood donation bag
            case DONOR_QUESTIONNAIRE:
                return IconShape.FLAT_PAPER;  // clipboard eligibility form
            case FORGED_DONOR_QUESTIONNAIRE:
                return IconShape.FLAT_PAPER;  // photocopied altered form
            case ORANGE_SQUASH:
                return IconShape.CYLINDER;    // plastic cup of squash
            case DIGESTIVE_BISCUIT:
                return IconShape.FLAT_PAPER;  // round biscuit
            case ORANGE_JUICE:
                return IconShape.CYLINDER;    // small plastic cup

            // Issue #1243: Northfield Bert's Tyres & MOT
            case MOT_CERTIFICATE:
                return IconShape.FLAT_PAPER;  // official certificate
            case FAIL_SHEET:
                return IconShape.FLAT_PAPER;  // failure notice form
            case BROWN_ENVELOPE:
                return IconShape.FLAT_PAPER;  // brown envelope
            case STOLEN_TYRE:
                return IconShape.BOX;         // rubber tyre
            case CATALYTIC_CONVERTER:
                return IconShape.CYLINDER;    // cylindrical exhaust part
            case CAR_BATTERY:
                return IconShape.BOX;         // rectangular battery
            case INSPECTION_STICKER:
                return IconShape.FLAT_PAPER;  // windscreen sticker

            // Issue #1257: Northfield Rag-and-Bone Man
            case JUNK_ITEM:
                return IconShape.BOX;         // miscellaneous household junk
            case GARDEN_ORNAMENT:
                return IconShape.BOX;         // ceramic gnome or ornament
            case FORGED_LICENCE:
                return IconShape.FLAT_PAPER;  // forged licence document
            case RUBBER_TYRE:
                return IconShape.BOX;         // rubber tyre
            case PENKNIFE:
                return IconShape.FLAT_PAPER;  // small folding knife

            // Issue #1263: Northfield Illegal Street Racing
            case NITROUS_CANISTER:
                return IconShape.CYLINDER;    // pressurised nitrous canister
            case RACING_TROPHY:
                return IconShape.BOX;         // plastic trophy

            // Issue #1267: Northfield Prepayment Meter
            case METER_KEY:
                return IconShape.FLAT_PAPER;  // small plastic PAYG key
            case EMERGENCY_CREDIT_NOTICE:
                return IconShape.FLAT_PAPER;  // paper emergency credit slip

            // Issue #1269: Northfield BT Phone Box
            case PHONE_BOX_KEY:
                return IconShape.FLAT_PAPER;  // small BT engineer master key
            case SCRAWLED_NUMBER:
                return IconShape.FLAT_PAPER;  // scrap of paper with contact number
            case PHONE_CARD:
                return IconShape.FLAT_PAPER;  // legacy BT phonecard

            // Issue #1273: Northfield Fly-Tipping Ring
            case GARDEN_WASTE_BAG:
                return IconShape.BOX;         // bulging green waste sack
            case RUBBLE_SACK:
                return IconShape.BOX;         // heavy builder's rubble bag
            case FIXED_PENALTY_NOTICE:
                return IconShape.FLAT_PAPER;  // council fixed penalty notice

            // Issue #1276: Northfield Minicab Office — Big Terry's Cabs
            case TL_COUNCIL_PLATE:
                return IconShape.FLAT_PAPER;  // laminated taxi licence plate

            // Issue #1282: Northfield Day & Night Chemist
            case CALPOL:
                return IconShape.BOX;         // children's medicine bottle
            case IBUPROFEN:
                return IconShape.BOX;         // tablet box
            case PLASTERS:
                return IconShape.BOX;         // plaster tin
            case CONDOMS:
                return IconShape.BOX;         // foil packet box
            case NUROFEN_PLUS:
                return IconShape.BOX;         // small box
            case FORGED_PRESCRIPTION:
                return IconShape.FLAT_PAPER;  // prescription slip
            case STOLEN_METHADONE:
                return IconShape.CYLINDER;    // liquid bottle
            case WHITE_COAT:
                return IconShape.BOX;         // folded coat

            // Issue #1301: Northfield Big Issue Vendor
            case BIG_ISSUE_MAG:
                return IconShape.FLAT_PAPER;  // magazine
            case COUNTERFEIT_BIG_ISSUE:
                return IconShape.FLAT_PAPER;  // counterfeit magazine
            case CANVAS_BAG:
                return IconShape.BOX;         // shoulder bag

            // Issue #1303: Northfield Dave's Carpets
            case CARPET_OFFCUT:
                return IconShape.BOX;         // rolled carpet offcut
            case SOFA:
                return IconShape.BOX;         // bulky sofa
            case CLOSING_DOWN_FLYER:
                return IconShape.FLAT_PAPER;  // printed flyer
            case SACK_TRUCK:
                return IconShape.BOX;         // fold-flat trolley
            case CARPET_SAMPLE_BOOK:
                return IconShape.BOX;         // ring-binder of samples

            // Issue #1306: Northfield Traveller Site
            case LUCKY_HEATHER:
                return IconShape.FLAT_PAPER;  // small sprig
            case CLOTHES_PEG_BUNDLE:
                return IconShape.BOX;         // bundle of pegs
            case TARMAC_MIX:
                return IconShape.BOX;         // heavy bag
            case DOG_FIGHT_LEDGER:
                return IconShape.FLAT_PAPER;  // notebook
            case LUCKY_HEATHER_CROWN:
                return IconShape.BOX;         // woven crown
            case STOLEN_BIKE:
                return IconShape.BOX;         // bicycle

            // Issue #1317: Northfield Bonfire Night
            case ROCKET_FIREWORK:
                return IconShape.CYLINDER;    // card tube firework
            case BANGER_FIREWORK:
                return IconShape.CYLINDER;    // small banger
            case ROMAN_CANDLE:
                return IconShape.CYLINDER;    // long tube
            case OLD_CLOTHES:
                return IconShape.BOX;         // bundled clothes

            // Issue #1319: NatWest Cashpoint — The Dodgy ATM
            case STOLEN_PIN_NOTE:
                return IconShape.FLAT_PAPER;  // scrap of paper with PIN
            case VICTIM_BANK_CARD:
                return IconShape.FLAT_PAPER;  // plastic bank card
            case CARD_SKIMMER_DEVICE:
                return IconShape.BOX;         // small electronic device
            case CLONED_CARD_DATA:
                return IconShape.FLAT_PAPER;  // data printout
            case STUFFED_ENVELOPE:
                return IconShape.FLAT_PAPER;  // thick envelope
            case ENGINEER_ACCESS_CARD:
                return IconShape.FLAT_PAPER;  // laminated access card

            // Issue #1337: Northfield Police Station — The Nick
            case POLICE_KEY_CARD:
                return IconShape.FLAT_PAPER;  // laminated key card
            case ROPE_AND_HOOK:
                return IconShape.BOX;         // coiled rope with hook

            // Issue #1335: Northfield Cycle Centre — Dave's Bikes
            case BIKE_REPAIR_KIT:
                return IconShape.BOX;         // compact repair pouch
            case BIKE_LOCK:
                return IconShape.BOX;         // D-lock or chain
            case BIKE_LIGHT_FRONT:
                return IconShape.BOX;         // clip-on LED unit
            case BIKE_LIGHT_REAR:
                return IconShape.BOX;         // clip-on LED unit
            case BIKE_HELMET:
                return IconShape.BOX;         // foam shell helmet
            case DELIVERY_BAG:
                return IconShape.BOX;         // large insulated courier bag
            case COLD_DELIVERY_BAG:
                return IconShape.BOX;         // worn insulated bag

            // Issue #1343: Northfield Christmas Lights Switch-On
            case MULLED_WINE:
                return IconShape.CYLINDER;    // small cup / beaker
            case WAYNE_STUBBS_AUTOGRAPH:
                return IconShape.FLAT_PAPER;  // signed photo
            case CELEBRITY_WALLET:
                return IconShape.FLAT_PAPER;  // leather billfold
            case FREEBIE_WRISTBAND:
                return IconShape.CYLINDER;    // cloth wristband
            case CHRISTMAS_LIGHTS_BULB:
                return IconShape.CYLINDER;    // small bulb

            // Issue #1353: Northfield Amateur Dramatics Society
            case STAGE_COSTUME:
                return IconShape.BOX;         // folded theatrical costume
            case REHEARSAL_SCHEDULE:
                return IconShape.FLAT_PAPER;  // printed timetable sheet
            case FORGED_TICKET:
                return IconShape.FLAT_PAPER;  // home-printed ticket
            case PROP_GUN:
                return IconShape.BOX;         // stage prop revolver

            // Issue #1357: Northfield Charity Fun Run
            case RACE_NUMBER_BIB:
                return IconShape.FLAT_PAPER;  // pinned race number bib
            case SPONSOR_SHEET:
                return IconShape.FLAT_PAPER;  // clipboard sponsor sheet
            case WINNERS_MEDAL:
                return IconShape.CYLINDER;    // medal on a ribbon
            case WATER_CUP:
                return IconShape.CYLINDER;    // paper cup of water

            // Issue #1359: Northfield HMRC Tax Investigation
            case TAX_DEMAND_LETTER:
                return IconShape.FLAT_PAPER;  // official HMRC letter
            case CLEAN_BILL_OF_HEALTH:
                return IconShape.FLAT_PAPER;  // HMRC clearance letter
            case CASH_BRIBE_ENVELOPE:
                return IconShape.FLAT_PAPER;  // brown envelope

            // Issue #1367: Northfield Speed Awareness Course
            case SPEEDING_NOTICE:
                return IconShape.FLAT_PAPER;  // Section 172 NIP letter

            // Issue #1371: Northfield Christmas Market
            case BRATWURST:
                return IconShape.CYLINDER;    // grilled sausage in a bun
            case CHRISTMAS_TRINKET:
                return IconShape.BOX;         // small festive knick-knack
            case CANDY_CANE:
                return IconShape.CYLINDER;    // striped sugar cane
            case RAFFLE_TICKET:
                return IconShape.FLAT_PAPER;  // numbered paper strip
            case CHRISTMAS_HAMPER:
                return IconShape.BOX;         // wicker hamper with ribbon
            case FAKE_DESIGNER_SCARF:
                return IconShape.BOX;         // folded counterfeit scarf
            case SANTA_BADGE:
                return IconShape.FLAT_PAPER;  // clip-on "I'M SANTA" badge

            // Issue #1381: Northfield Bank Holiday Street Party
            case WARM_LAGER:
                return IconShape.CYLINDER;    // can of lager
            case CRISP_PACKET:
                return IconShape.FOOD;        // flat snack packet
            case RAW_SAUSAGE:
                return IconShape.FOOD;        // raw pink sausage
            case COOKED_SAUSAGE:
                return IconShape.FOOD;        // charred sausage
            case SELECTION_BOX:
                return IconShape.BOX;         // holiday snack box
            case NOVELTY_TOWEL:
                return IconShape.BOX;         // folded towel
            case BOTTLE_OF_WINE:
                return IconShape.BOTTLE;      // wine bottle
            case TOOLKIT:
                return IconShape.BOX;         // compact tool set
            case COMMEMORATIVE_MUG:
                return IconShape.CYLINDER;    // ceramic mug

            // Issue #1381: Northfield Halloween
            case RAW_EGG:
                return IconShape.CYLINDER;    // small oval egg
            case MINI_CHOCOLATE_BAR:
                return IconShape.BOX;         // small chocolate bar
            case TRICK_OR_TREAT_BAG:
                return IconShape.BOX;         // orange bag
            case WITCH_COSTUME:
                return IconShape.FLAT_PAPER;  // folded costume
            case GHOST_SHEET:
                return IconShape.FLAT_PAPER;  // white sheet
            case SKELETON_SUIT:
                return IconShape.FLAT_PAPER;  // folded suit
            case PUMPKIN_HEAD_MASK:
                return IconShape.BOX;         // round mask
            case RAW_PUMPKIN:
                return IconShape.BOX;         // round pumpkin
            case CARVED_PUMPKIN:
                return IconShape.BOX;         // carved round pumpkin
            case PUMPKIN_INNARDS:
                return IconShape.FOOD;        // gooey innards
            case POINTY_HAT:
                return IconShape.BOX;         // pointed hat
            case BLACK_CAPE:
                return IconShape.FLAT_PAPER;  // folded cape

            // Issue #1383: Northfield Boxing Day Sales
            case FROZEN_PRAWN_RING:
                return IconShape.FOOD;        // ring of frozen prawns
            case PARTY_FOOD_PLATTER:
                return IconShape.BOX;         // flat plastic platter
            case LUXURY_BISCUIT_TIN:
                return IconShape.BOX;         // round biscuit tin
            case GEORGE_FOREMAN_GRILL:
                return IconShape.BOX;         // compact electric grill
            case BREAD_MAKER:
                return IconShape.BOX;         // boxy kitchen appliance
            case VINYL_RECORD_BOX:
                return IconShape.BOX;         // cardboard box of records
            case GENUINE_FIRST_PRESSING:
                return IconShape.CYLINDER;    // rare vinyl disc
            case HDMI_CABLE:
                return IconShape.TOOL;        // cable / lead shape

            // Issue #1386: Northfield St George's Day
            case DOOM_BAR_PINT:
                return IconShape.CYLINDER;    // tall pint glass
            case BANANA_SKIN:
                return IconShape.FOOD;        // curved banana skin
            case ENGLAND_SHIRT_PROP:
                return IconShape.FLAT_PAPER;  // folded football shirt
            case MORRIS_STICK_PROP:
                return IconShape.TOOL;        // short wooden stick
            case ST_GEORGE_FLAG_PROP:
                return IconShape.FLAT_PAPER;  // folded flag
            case ROOF_FLAG_PROP:
                return IconShape.FLAT_PAPER;  // folded flag

            // Issue #1388: Northfield Dog Show
            case DOG_BRUSH:
                return IconShape.TOOL;        // slicker brush
            case VICTORIA_SPONGE_SLICE:
                return IconShape.FOOD;        // slice of sponge cake
            case BEST_IN_SHOW_ROSETTE_PROP:
                return IconShape.FLAT_PAPER;  // large rosette
            case RESERVE_ROSETTE_PROP:
                return IconShape.FLAT_PAPER;  // medium rosette
            case THIRD_PLACE_ROSETTE_PROP:
                return IconShape.FLAT_PAPER;  // small rosette
            case SHOW_SCHEDULE_FLYER:
                return IconShape.FLAT_PAPER;  // A5 flyer
            case CUP_OF_TEA:
                return IconShape.CYLINDER;    // mug of tea

            // Issue #1396: Northfield Royal Mail Strike
            case TEABAG:
                return IconShape.BOX;         // small square teabag
            case SCAB_PARCEL_BAG:
                return IconShape.BOX;         // Royal Mail sack
            case HANDWRITTEN_PARCEL:
                return IconShape.BOX;         // brown-paper parcel
            case UNION_HERO_BADGE:
                return IconShape.FLAT_PAPER;  // pin badge / flat disc
            case BOLT_CUTTER:
                return IconShape.TOOL;        // heavy-duty cutting tool

            // Issue #1398: Northfield Window Cleaner
            case BUCKET_AND_CHAMOIS:
                return IconShape.BOX;         // yellow bucket with chamois cloth
            case TERRY_DEBT_NOTE:
                return IconShape.FLAT_PAPER;  // handwritten note

            // Issue #1400: Northfield Residents' Parking Permit Racket
            case PARKING_PERMIT:
                return IconShape.FLAT_PAPER;  // laminated permit card
            case FORGED_PARKING_PERMIT:
                return IconShape.FLAT_PAPER;  // dodgy-looking permit card
            case WHEEL_CLAMP_KEY:
                return IconShape.TOOL;        // small release key
            case STOLEN_PRINTER_INK:
                return IconShape.BOX;         // sealed inkjet cartridge box

            // Issue #1402: Northfield Severn Trent Road Dig
            case THERMOS:
                return IconShape.CYLINDER;    // red flask
            case SITE_RADIO:
                return IconShape.BOX;         // portable radio
            case CONTRACTOR_CLIPBOARD:
                return IconShape.FLAT_PAPER;  // clipboard with forms
            case HARD_HAT:
                return IconShape.BOX;         // safety helmet
            case MYSTERY_OBJECT:
                return IconShape.BOX;         // unidentified lump

            // Issue #1404: Northfield Community Litter Pick
            case LITTER_PICKER_STICK:
                return IconShape.TOOL;        // long orange grabber tool
            case COUNCIL_RUBBISH_BAG:
                return IconShape.BOX;         // large black bag
            case CRACK_PIPE:
                return IconShape.CYLINDER;    // glass tube pipe

            // Issue #1406: Northfield Dodgy Roofer
            case BUCKET_OF_SEALANT:
                return IconShape.CYLINDER;    // grey sealant bucket
            case LATEX_GLOVES:
                return IconShape.FLAT_PAPER;  // flat glove box
            case SCAFFOLDING_SPANNER:
                return IconShape.TOOL;        // heavy steel podger
            case INVOICE_PAD:
                return IconShape.FLAT_PAPER;  // pad of invoice forms
            case CASH_ENVELOPE:
                return IconShape.FLAT_PAPER;  // bulging manila envelope
            case ROOF_SLATE_BAG:
                return IconShape.BOX;         // heavy canvas bag of slates

            // Issue #1416: Northfield Mobile Speed Camera Van
            case SPEED_CAMERA_SD_CARD:
                return IconShape.FLAT_PAPER;  // small plastic SD card
            case SPEEDING_FINE_NOTICE:
                return IconShape.FLAT_PAPER;  // official notice document
            case HANDWRITTEN_WARNING_SIGN:
                return IconShape.FLAT_PAPER;  // cardboard sign with marker text

            // Issue #1420: Northfield Post Office Horizon Scandal
            case CITIZENS_ADVICE_LEAFLET:
                return IconShape.FLAT_PAPER;  // tri-fold CAB leaflet
            case SHORTFALL_LETTER:
                return IconShape.FLAT_PAPER;  // A4 demand notice
            case TRANSACTION_LOG:
                return IconShape.FLAT_PAPER;  // dot-matrix printout
            case STAMPS_BUNDLE:
                return IconShape.BOX;         // bundle of first-class stamps
            case IT_CONTRACTOR_ID_BADGE:
                return IconShape.FLAT_PAPER;  // lanyard ID card
            case USB_STICK:
                return IconShape.FLAT_PAPER;  // small USB drive

            // Issue #1424: Northfield Doorstep Energy Tout
            case TOUT_CLIPBOARD:
                return IconShape.FLAT_PAPER;  // branded clipboard
            case SMART_METER_KIT:
                return IconShape.BOX;         // boxed kit
            case CRAIG_WITNESS_STATEMENT:
                return IconShape.FLAT_PAPER;  // handwritten statement
            case DAVE_DEBT_LIST:
                return IconShape.FLAT_PAPER;  // scrawled list
            case REPLACEMENT_CLIPBOARD:
                return IconShape.FLAT_PAPER;  // blank clipboard
            case FANCIER_FAVOUR:
                return IconShape.FLAT_PAPER;  // folded IOU note

            // Issue #1428: Northfield Council CCTV Audit
            case CCTV_FOOTAGE:
                return IconShape.BOX;         // VHS cassette
            case DUMMY_CCTV_CAMERA:
                return IconShape.BOX;         // hollow plastic camera unit
            case MEAL_DEAL_ITEM:
                return IconShape.BOX;         // bundled bag with sandwich, crisps, drink

            // Issue #1433: Northfield Easter Weekend
            case HOT_CROSS_BUN:
                return IconShape.FOOD;        // baked bun
            case CHOCOLATE_EGG:
                return IconShape.FOOD;        // foil-wrapped egg
            case FOIL_EASTER_EGG:
                return IconShape.FOOD;        // shiny collectible egg
            case EASTER_BASKET:
                return IconShape.BOX;         // wicker basket
            case BIKER_JACKET:
                return IconShape.FLAT_PAPER;  // flat jacket icon
            case CHARITY_BUCKET_EASTER:
                return IconShape.CYLINDER;    // collection bucket
            case BIKERS_VOUCHER:
                return IconShape.FLAT_PAPER;  // paper voucher

            // Issue #1447: Northfield Street Hustler
            case RIGGED_CARD_DECK:
                return IconShape.FLAT_PAPER;  // deck of cards
            case LOOKOUT_CUT:
                return IconShape.FLAT_PAPER;  // folded notes

            // Issue #1449: Northfield Mobile Library
            case LIBRARY_CARD:
                return IconShape.FLAT_PAPER;  // laminated card
            case RARE_BOOK:
                return IconShape.BOX;         // slim hardback volume
            case SELF_HELP_PAPERBACK:
                return IconShape.FLAT_PAPER;  // paperback book
            case LOCAL_HISTORY_BOOK:
                return IconShape.FLAT_PAPER;  // hardback local history
            case CRIME_FICTION:
                return IconShape.FLAT_PAPER;  // dog-eared thriller
            case JOB_SKILLS_GUIDE:
                return IconShape.FLAT_PAPER;  // slim guide booklet
            case GARDENING_ENCYCLOPEDIA:
                return IconShape.BOX;         // heavy reference tome
            case COOKBOOK:
                return IconShape.FLAT_PAPER;  // paperback cookbook
            case LIBRARY_VOUCHER:
                return IconShape.FLAT_PAPER;  // paper voucher
            case PETITION_BOARD:
                return IconShape.FLAT_PAPER;  // clipboard petition

            // Issue #1477: Northfield Warm Hub
            case HUB_TEA:
                return IconShape.BOTTLE;      // mug of tea
            case HUB_BISCUIT:
                return IconShape.FOOD;        // rich tea biscuit
            case DONATIONS_TIN:
                return IconShape.CYLINDER;    // round tin
            case FAKE_DONATIONS_FORM:
                return IconShape.FLAT_PAPER;  // official-looking form
            case HOMEMADE_CAKE_SLICE:
                return IconShape.FOOD;        // slice of Victoria sponge

            default:
                return IconShape.BOX;
        }
    }
}

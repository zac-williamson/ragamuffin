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

    /**
     * Tablecloth — "Beige, floral. Smells of moth balls."
     * Wearable as a disguise component: +1 Disguise tier for 5 minutes if draped over player (press E to wear).
     * Found at jumble sales; sell value: 1 COIN.
     */
    TABLECLOTH("Tablecloth");

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
            case ROPE:               return cs(0.72f, 0.60f, 0.35f,  // Natural hemp tan
                                               0.55f, 0.42f, 0.20f); // Darker twisted strand

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
            case FIRE_EXTINGUISHER:
            case HAIR_CLIPPERS:
            case HAIR_CLIPPERS_BROKEN:
                return IconShape.CYLINDER;

            case HIGH_VIS_JACKET:
                return IconShape.FLAT_PAPER; // vest-like shape
            case CROWBAR:
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

            default:
                return IconShape.BOX;
        }
    }
}

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
    TOBACCO("Tobacco");

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
            case HAIR_CLIPPERS:  return c(0.35f, 0.35f, 0.38f);   // Silver-grey
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

            default:
                return IconShape.BOX;
        }
    }
}

package ragamuffin.world;

/**
 * Types of landmarks in the British town.
 */
public enum LandmarkType {
    PARK,
    GREGGS,
    OFF_LICENCE,
    CHARITY_SHOP,
    JEWELLER,
    OFFICE_BUILDING,
    JOB_CENTRE,
    INDUSTRIAL_ESTATE,
    BOOKIES,
    TERRACED_HOUSE,
    KEBAB_SHOP,
    LAUNDERETTE,
    TESCO_EXPRESS,
    PUB,
    PAWN_SHOP,
    BUILDERS_MERCHANT,
    WAREHOUSE,
    CHIPPY,
    NEWSAGENT,
    GP_SURGERY,
    PRIMARY_SCHOOL,
    COMMUNITY_CENTRE,
    CHURCH,
    TAXI_RANK,
    CAR_WASH,
    COUNCIL_FLATS,
    PETROL_STATION,
    NANDOS,
    BARBER,
    NAIL_SALON,
    WETHERSPOONS,
    CORNER_SHOP,
    BETTING_SHOP,
    PHONE_REPAIR,
    CASH_CONVERTER,
    LIBRARY,
    FIRE_STATION,
    ALLOTMENTS,
    CANAL,
    SKATE_PARK,
    CEMETERY,
    UNDERGROUND_BUNKER,
    SEWER_TUNNEL,
    LEISURE_CENTRE,
    MOSQUE,
    ESTATE_AGENT,
    SUPERMARKET,
    POLICE_STATION,
    FOOD_BANK,
    STATUE,

    // ── Issue #714: Player Squat system ─────────────────────────────────────
    /** A derelict building claimed by the player as a personal squat. */
    SQUAT,

    // ── Issue #724: Subterranean secrets ────────────────────────────────────
    /** A hidden chamber off the sewer network, concealing stashed contraband. */
    SEWER_STASH_ROOM,
    /** A lower level of the civil defence bunker, deeper underground. */
    BUNKER_LOWER_LEVEL,
    /** A concealed basement beneath a shop on the high street — black market goods. */
    BLACK_MARKET_BASEMENT,

    // ── Issue #789: Boot Sale — Emergent Underground Auction Economy ─────────
    /** Clandestine daily boot sale venue on wasteland at the south-east corner. */
    BOOT_SALE,

    // ── Issue #801: The Underground Fight Night ────────────────────────────────
    /**
     * The Pit — a concealed basement fight venue beneath the industrial estate.
     * A 12×8×4 STONE basement with DIRT floor, LANTERN props, a central rope ring,
     * and spectators. Entry requires Notoriety ≥ 10 or a FIGHT_CARD flyer.
     */
    THE_PIT,

    // ── Issue #901: Portal to Bista Village ───────────────────────────────────
    /**
     * Bista Village — a historically accurate recreation of the Nepali Gurkha
     * settlement established near Aldershot following the Gurkha Justice Campaign
     * of 2009. The village features traditional Nepali architecture: stone-and-mortar
     * homes, a community chautara (stone resting platform), prayer flags, terraced
     * gardens, and a small dhara (stone water spout).
     * Accessible only via the craftable BISTA_VILLAGE_PORTAL item.
     * A return portal stone spawns at the village entrance on arrival.
     */
    BISTA_VILLAGE,

    // ── Issue #916: Late-Night Kebab Van ─────────────────────────────────────
    /**
     * The late-night kebab van — a mobile food vendor that spawns near the pub
     * between 22:00 and 02:00. Not a permanent landmark; used as a position tag
     * for spawn logic and police patrol awareness.
     */
    KEBAB_VAN,

    // ── Issue #918: Bus Stop & Public Transport System ─────────────────────────

    /**
     * Bus Stop — High Street near Greggs.
     * The Number 47's first scheduled stop on the town route.
     */
    BUS_STOP_HIGH_STREET,

    /**
     * Bus Stop — Park near the pond.
     * The Number 47's second scheduled stop; serves the park area.
     */
    BUS_STOP_PARK,

    /**
     * Bus Stop — Industrial Estate near JobCentre.
     * The Number 47's third and final scheduled stop on the town route.
     */
    BUS_STOP_INDUSTRIAL,

    // ── Issue #938: Greasy Spoon Café ────────────────────────────────────────

    /**
     * Vera's Caff — a classic British greasy spoon café on the high street.
     * Open 07:00–14:00 daily. Run by Vera (CAFF_OWNER NPC).
     * Features a daily specials CHALKBOARD prop with combo discounts.
     * Seated CAFF_REGULAR NPCs passively reveal rumours on player proximity (2 blocks).
     * Weather modifier: rain/drizzle/thunderstorm adds +2 seated regulars.
     * Monday rush: max 4 customers. Notoriety ≥ 60 + police nearby blocks service.
     */
    GREASY_SPOON_CAFE,

    // ── Issue #944: High Street Barber ────────────────────────────────────────

    /**
     * Ali's Barber Shop — a Turkish barber on the high street, between the
     * off-licence and the charity shop. Open 09:00–18:00 Monday–Saturday.
     * Run by Ali (BARBER NPC). Features BARBER_CHAIR, BARBER_POLE (outside),
     * and MIRROR props. Ali accumulates neighbourhood rumours and shares one free
     * per visit.
     */
    BARBER_SHOP,

    // ── Issue #948: Hand Car Wash ─────────────────────────────────────────────

    /**
     * Sparkle Hand Car Wash — a cash-in-hand hand car wash near the industrial
     * estate. Staffed by two WORKER NPCs and managed by a CAR_WASH_BOSS.
     * Open 08:00–18:00. Player can work shifts for coin, launder notoriety, or
     * rob the CASH_BOX_PROP. A HOSE_PROP and BUCKET_PROP sit on a 6×4 PAVEMENT
     * forecourt; a SHED_PROP at the back houses a SQUEEGEE.
     */
    HAND_CAR_WASH,

    // ── Issue #952: Clucky's Fried Chicken ───────────────────────────────────

    /**
     * Clucky's Fried Chicken — a late-night fried chicken shop on the high street.
     * Open 10:00–02:00. Staffed by Devraj (DEVRAJ NPC) behind a counter.
     * Menu: CHICKEN_WINGS (2 coin), CHICKEN_BOX (4 coin), CHIPS_AND_GRAVY (1 coin,
     * after 20:00 only), FLAT_COLA (1 coin).
     * Youth/YOUTH_GANG NPCs congregate outside 18:00–02:00 (up to 6).
     * Wing Tax mechanic: on player exit with food, a youth may demand a wing.
     * Litter props spawn every 15 in-game minutes (cap 8); deposit in LITTER_BIN_PROP
     * for −1 Notoriety per piece. Street brawl: 15%/hour chance 22:00–01:00.
     */
    FRIED_CHICKEN_SHOP;

    /**
     * Returns the display name shown on the building's sign.
     * Returns null for landmarks that don't have named signage (e.g. houses, park).
     */
    public String getDisplayName() {
        switch (this) {
            case GREGGS:            return "Greggs";
            case OFF_LICENCE:       return "Khan's Off-Licence";
            case CHARITY_SHOP:      return "Hearts & Minds Charity";
            case JEWELLER:          return "Andre's Diamonds";
            case OFFICE_BUILDING:   return "Meridian House";
            case JOB_CENTRE:        return "Jobcentre Plus";
            case BOOKIES:           return "Coral Betting";
            case KEBAB_SHOP:        return "Sultan's Kebab";
            case LAUNDERETTE:       return "Spotless Launderette";
            case TESCO_EXPRESS:     return "Tesco Express";
            case PUB:               return "The Ragamuffin Arms";
            case PAWN_SHOP:         return "Cash4Gold Pawnbrokers";
            case BUILDERS_MERCHANT: return "Handy Builders";
            case CHIPPY:            return "Tony's Chip Shop";
            case NEWSAGENT:         return "Patel's News";
            case GP_SURGERY:        return "Northfield Surgery";
            case PRIMARY_SCHOOL:    return "St. Aidan's C.E. School";
            case COMMUNITY_CENTRE:  return "Northfield Community Ctr";
            case CHURCH:            return "St. Mary's Church";
            case TAXI_RANK:         return "A1 Taxis";
            case CAR_WASH:          return "Sparkle Car Wash";
            case PETROL_STATION:    return "BP Petrol Station";
            case NANDOS:            return "Nando's";
            case BARBER:            return "Kev's Barber Shop";
            case NAIL_SALON:        return "Angel Nails & Beauty";
            case WETHERSPOONS:      return "The Rusty Anchor";
            case CORNER_SHOP:       return "Happy Shopper";
            case BETTING_SHOP:      return "Ladbrokes";
            case PHONE_REPAIR:      return "Fix My Phone";
            case CASH_CONVERTER:    return "Cash Converters";
            case LIBRARY:           return "Northfield Library";
            case FIRE_STATION:      return "Northfield Fire Station";
            case UNDERGROUND_BUNKER: return "Civil Defence Bunker";
            case SEWER_TUNNEL:      return "Sewer System";
            case SEWER_STASH_ROOM:  return "Hidden Chamber";
            case BUNKER_LOWER_LEVEL: return "Bunker Lower Level";
            case BLACK_MARKET_BASEMENT: return "The Basement";
            case LEISURE_CENTRE:    return "Northfield Leisure Centre";
            case MOSQUE:            return "Al-Noor Mosque";
            case ESTATE_AGENT:      return "Baxter's Estate Agents";
            case SUPERMARKET:       return "Aldi";
            case POLICE_STATION:    return "Northfield Police Station";
            case FOOD_BANK:         return "Northfield Food Bank";
            case THE_PIT:           return "The Pit";
            case BISTA_VILLAGE:     return "Bista Village";
            case KEBAB_VAN:              return "Ali's Kebab Van";
            case BUS_STOP_HIGH_STREET:  return "Bus Stop – High Street";
            case BUS_STOP_PARK:         return "Bus Stop – Park";
            case BUS_STOP_INDUSTRIAL:   return "Bus Stop – Industrial Estate";
            case GREASY_SPOON_CAFE:     return "Vera's Caff";
            case BARBER_SHOP:           return "Ali's Barber Shop";
            case HAND_CAR_WASH:         return "Sparkle Hand Car Wash";
            case FRIED_CHICKEN_SHOP:    return "Clucky's Fried Chicken";
            default:                    return null; // No sign for parks, houses, etc.
        }
    }
}

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
    THE_PIT;

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
            default:                return null; // No sign for parks, houses, etc.
        }
    }
}

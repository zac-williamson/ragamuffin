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
    FRIED_CHICKEN_SHOP,

    // ── Issue #954: Northfield Bingo Hall ─────────────────────────────────────

    /**
     * Lucky Stars Bingo — a single-storey brick bingo hall on the high street.
     * Sessions run Tuesdays and Thursdays 14:00–17:00 (doors open 13:30).
     * Staffed by the CALLER NPC at the podium. Entry costs 2 COIN.
     * Interior features BENCH_PROP seating, BINGO_CALLER_PODIUM_PROP, PRIZE_BOARD_PROP,
     * and a REFRESHMENT_COUNTER_PROP selling TEA_CUP and BISCUIT (1 COIN each).
     * 4–8 PENSIONER NPCs attend each session. Rigged card mechanic available.
     * Warm shelter: +5 Warmth/min while seated.
     */
    BINGO_HALL,

    // ── Issue #965: Northfield Snooker Hall ───────────────────────────────────

    /**
     * Cue Zone — a dingy upstairs snooker hall above a row of shops on the high street.
     * Open 10:00–23:00 daily. Managed by Dennis the Proprietor (SHOPKEEPER NPC).
     * Features four SNOOKER_TABLE_PROP tables available for 2 COIN rental.
     * Frank the Hustler (SNOOKER_HUSTLER NPC) wanders 12:00–22:00.
     * One-Armed Carl (PUBLIC NPC) available at STREET_LADS Respect ≥ 75.
     * Back-room card game (pontoon) accessible at MARCHETTI_CREW Respect ≥ 60.
     */
    SNOOKER_HALL,

    // ── Issue #975: Northfield Post Office ────────────────────────────────────

    /**
     * Northfield Post Office — a red-brick Royal Mail branch on the high street.
     * Open Mon–Fri 09:00–17:30, Sat 09:00–12:30.
     * Staffed by Maureen the COUNTER_CLERK. Serves as the focal point for the
     * PostOfficeSystem: benefit book cashing, scratch cards, stamp purchases,
     * and POSTMAN shift sign-on (05:30–07:00).
     * A POST_BOX_PROP stands outside for sending threatening letters.
     */
    POST_OFFICE,

    // ── Issue #977: Northfield Amusement Arcade ───────────────────────────────

    /**
     * Ace Amusements — a garish amusement arcade on the high street, squeezed
     * between the charity shop and the bookies. Open 10:00–22:00 daily.
     * A 10×8-block interior with CARPET (red), YELLOW_BRICK walls and GLASS frontage.
     * Features ARCADE_MACHINE_PROP, PENNY_FALLS_PROP, CLAW_MACHINE_PROP,
     * CHANGE_MACHINE_PROP, and REDEMPTION_COUNTER_PROP.
     * Staffed by Kevin the ARCADE_ATTENDANT; 2–4 ARCADE_KID NPCs after school.
     */
    ARCADE,

    // ── Issue #983: Northfield Dog Track ─────────────────────────────────────

    /**
     * Northfield Dog Track — a floodlit greyhound racing venue on the industrial
     * fringe of Northfield. Open evenings (18:00–23:00) and Saturday afternoons
     * (13:00–19:00). The Marchetti Crew run the track.
     *
     * <p>Six named greyhounds race every 8 in-game minutes. Fixed-odds bets
     * (1.5×–8×) are placed at the Tote window (TOTE_CLERK NPC). The track is
     * guarded by a SECURITY_GUARD on a 90-second patrol; a KENNEL_HAND tends
     * the dogs in the kennel block.
     *
     * <p>Entry is refused at Marchetti Crew Respect &lt; 30. At Respect &ge; 60,
     * the TOTE_CLERK provides one insider tip per session. Rain reduces attendance
     * (fewer TRACK_PUNTER NPCs); fog dims the scoreboard display.
     *
     * <p>Illegal activities: bribe the KENNEL_HAND (10 COIN, guarantees a dog
     * loses); slip a DODGY_PIE to a dog (reduces speed 30%); night kennel heist
     * with LOCKPICK to steal a GREYHOUND item and fence it at the Pawn Shop
     * for 25–40 COIN.
     *
     * <p>Integrates with: FactionSystem (Marchetti), BettingUI, RumourNetwork,
     * NotorietySystem, NoiseSystem, WeatherSystem, CriminalRecord, PawnShopSystem,
     * StreetEconomySystem (RACE_CARD as BORED satisfier).
     */
    GREYHOUND_TRACK,

    /**
     * Issue #1012: Skin Deep Tattoos — tattoo parlour on the high street.
     * Run by Kev (TATTOOIST), open Tue–Sat 11:00–18:00.
     * Features TATTOO_CHAIR_PROP, FLASH_SHEET_PROP, TATTOO_STATION_PROP.
     */
    TATTOO_PARLOUR,

    // ── Issue #1026: Northfield Scrapyard ────────────────────────────────────

    /**
     * Pearce &amp; Sons Metal Merchants — a scrapyard on the industrial estate.
     * Managed by SCRAPYARD_OWNER Gary and SCRAPYARD_WORKER Kyle.
     * Open 09:00–17:00. GUARD_DOG Tyson patrols 20:00–07:00.
     * Features WEIGH_BRIDGE_PROP, CRUSHER_PROP, YARD_OFFICE_PROP.
     * Locked compound (LOCKED_GATE_PROP) contains COPPER_BALE_PROP.
     * Buys scrap by weight; refuses copper if player Notoriety ≥ 50.
     */
    SCRAPYARD,

    // ── Issue #1037: Northfield Indoor Market ─────────────────────────────────

    /**
     * Northfield Indoor Market — a covered market hall open Tue/Fri/Sat 08:00–16:00.
     * A quintessential British working-class institution: rows of stalls selling
     * dodgy DVDs, second-hand clothes, counterfeit goods, knock-off perfume, and
     * a greasy tea urn. Players can rent a stall, browse for cheap gear, pickpocket
     * in the crowds, and survive the chaos of a Trading Standards raid.
     *
     * <p>Building: low single-storey brick rectangle (16×24×4 blocks), open-plan
     * interior with 8 stall units ({@code MARKET_STALL_PROP}) in two rows of four.
     * When closed, {@code MARKET_SHUTTER_PROP} is in impassable state.
     *
     * <p>Staffed by Dave (electronics), Sheila (clothes), Mo (knock-offs), Brenda
     * (hot food / tea urn), and Ray the Market Manager near the entrance.
     *
     * <p>Integrates with: IndoorMarketSystem, FenceSystem, StreetEconomySystem,
     * WeatherSystem, DisguiseSystem, NoiseSystem, RumourNetwork.
     */
    INDOOR_MARKET,

    // ── Issue #1039: Northfield Barber ────────────────────────────────────────

    /**
     * Kosta's Barbers — a Turkish-run barbershop on the high street.
     * Run by Kosta (BARBER_OWNER, Mon–Sat 09:00–18:00) and apprentice Wayne
     * (BARBER_APPRENTICE, Tue–Fri 12:00–18:00). A BARBER_POLE_PROP stands
     * outside; two BARBER_CHAIR_PROPs inside; a WAITING_BENCH_PROP seats up to 3.
     * Haircuts change the player's HairstyleType and grant timed recognition reduction.
     * Integrates with WantedSystem (refused ≥2 stars), NotorietySystem (−5 display),
     * DisguiseSystem (passive modifier), FactionSystem (free trim at Marchetti ≥75),
     * NeighbourhoodSystem (boards up when vibes &lt;30), RumourNetwork (LOCAL_EVENT
     * per cut), NewspaperSystem (hairstyle in crime descriptions).
     */
    BARBERS,

    // ── Issue #1041: Northfield Argos ─────────────────────────────────────────

    /**
     * Argos — the iconic British catalogue shop on the high street.
     * Open Mon–Sat 09:00–17:30, Sun 10:00–16:00.
     * Staffed by ARGOS_CLERK and ARGOS_MANAGER. Customers browse catalogues,
     * write slips at ARGOS_CATALOGUE props, and collect orders at the counter.
     * Features layby debt system, returns desk fraud mechanic, pencil theft,
     * Marchetti dead-drop (item 9999), and random chaos events.
     * Integrates with WantedSystem (refused ≥2 stars), FactionSystem (Marchetti
     * dead-drop at Respect ≥50), CriminalRecord (TRESPASS for pencil theft),
     * NotorietySystem, RumourNetwork (LOCAL_EVENT from seated shoppers),
     * NewspaperSystem (SYSTEM_DOWN headlines).
     */
    ARGOS,

    // ── Issue #1067: Northfield Poundstretcher ────────────────────────────────

    /**
     * Northfield Poundstretcher — own-brand bargain retailer on the high street.
     * Run by Sharon (POUND_SHOP_MANAGER). Open Mon–Sat 08:30–18:30, Sun 10:00–16:00.
     * Compact 8×6-block floor plan: SHOPKEEPER_COUNTER_PROP at rear, shelf CRATE_PROPs
     * along two walls. Entrance on south face.
     */
    POUND_SHOP,

    // ── Issue #1069: Northfield Ice Cream Van ─────────────────────────────────

    /**
     * Dave's Ices — a battered Mr. Whippy ice cream van that cruises Northfield's
     * residential streets and park on warm days (SUNNY/OVERCAST, 12:00–19:30).
     * Operated by ICE_CREAM_MAN NPC (Dave). Sells 99 Flakes, lollies, choc ices,
     * and wafer tubs through the main hatch. At Street Rep ≥ 40, the side-hatch fence
     * opens (55% of base value, no Notoriety penalty).
     * Mobile; not a permanent map fixture. Used as a position tag for spawn logic,
     * police patrol awareness, and faction (Marchetti) territory interactions.
     */
    ICE_CREAM_VAN,

    // ── Issue #1071: Northfield Fast Cash Finance ─────────────────────────
    /**
     * Fast Cash Finance — a garish payday loan shop on the Northfield parade,
     * sandwiched between the charity shop and the bookies.
     * Run by Barry (LOAN_MANAGER NPC). Open Mon–Fri 09:00–18:00, Sat 10:00–16:00.
     * Interior: LOAN_DESK_PROP, LEAFLET_RACK_PROP, CCTV_CAMERA_PROP.
     */
    PAYDAY_LOAN_SHOP,

    // ── Issue #1077: Northfield Chinese Takeaway — Golden Palace ─────────────

    /**
     * Golden Palace — a narrow Chinese takeaway shopfront (6×10×4 blocks) squeezed
     * between the Wetherspoons side-alley and the payday loan shop.
     * Run by Mr. Chen ({@code SHOPKEEPER} NPC). Open daily 16:00–23:30.
     * Interior: red CARPET, {@code MENU_BOARD_PROP} behind the counter,
     * {@code SERVING_HATCH_PROP} separating kitchen from front-of-house,
     * {@code TELEPHONE_PROP} on the counter, 4 {@code WAITING_BENCH_PROP} seats.
     * A {@code CHINESE_LANTERN_PROP} hangs above the door (always lit).
     * Integrates with ChineseTakeawaySystem, WarmthSystem, WeatherSystem,
     * SquatSystem, RumourNetwork, NotorietySystem, NoiseSystem, StreetEconomySystem,
     * NPCManager, WantedSystem, AchievementSystem.
     */
    CHINESE_TAKEAWAY,

    // ── Issue #1079: Northfield Magistrates' Court ─────────────────────────

    /**
     * Northfield Magistrates' Court — a squat, authoritative 1960s civic building
     * on the edge of the town centre. Presided over by Magistrate Sandra Pemberton.
     * Open Mon–Fri 09:00–17:00.
     *
     * <p>The MagistratesCourtSystem runs hearings here: charges are read, pleas entered,
     * evidence checked via WitnessSystem, and sentences delivered.
     * Outcomes range from Conditional Caution to Custodial (24h lock-out).
     *
     * <p>NPCs: MAGISTRATE (Sandra Pemberton), CPS_PROSECUTOR (Martin Gale),
     * DUTY_SOLICITOR (Donna), COURT_USHER (Trevor).
     *
     * <p>Integrates with MagistratesCourtSystem, ArrestSystem, WitnessSystem,
     * FenceSystem, JobCentreSystem, FoodBankSystem, AllotmentSystem,
     * NotorietySystem, CriminalRecord, AchievementSystem, RumourNetwork.
     */
    MAGISTRATES_COURT,

    // ── Issue #1081: Northfield Pet Shop & Vet — Paws 'n' Claws ─────────────

    /**
     * Paws 'n' Claws — a narrow 6×10×4 brick shopfront on the high street parade.
     *
     * <p>Interior: FISH_TANK_PROP (ambient gurgling), two ANIMAL_CAGE_PROP rows
     * (rabbits / guinea pigs), a BIRD_PERCH_PROP (budgies), DOG_KENNEL_PROP
     * behind the counter. Outside: faded HANDWRITTEN_SIGN_PROP.
     *
     * <p>Open Mon–Sat 09:00–17:30. Staffed by Bev (PET_SHOP_OWNER NPC).
     *
     * <p>Integrates with PetShopSystem, FactionSystem, NotorietySystem,
     * SquatSystem, StreetEconomySystem, NeighbourhoodSystem, WeatherSystem,
     * RumourNetwork, CriminalRecord, AchievementSystem, BootSaleSystem.
     */
    PET_SHOP,

    /**
     * Northfield Vets — adjoining Paws 'n' Claws via a shared corridor.
     *
     * <p>A 6×8×3 practice: CONSULTING_TABLE_PROP, MEDICINE_CABINET_PROP
     * (lockpickable), WAITING_BENCH_PROP (3 seats).
     *
     * <p>Open Mon–Fri 08:30–18:00, Sat 09:00–13:00. Staffed by Dr. Patel (VET NPC).
     * 2–3 waiting PUBLIC NPCs in the waiting room.
     *
     * <p>Integrates with PetShopSystem, PigeonRacingSystem, NotorietySystem,
     * WantedSystem, WitnessSystem, CriminalRecord, RumourNetwork, AchievementSystem.
     */
    VET_SURGERY,

    // ── Issue #1085: Northfield Internet Café — Cybernet ─────────────────────

    /**
     * Cybernet — a narrow 8×10×3 brick shopfront on the high street, between
     * the Off-Licence and Poundstretcher.
     *
     * <p>Interior: six {@code INTERNET_TERMINAL} props in two rows of three,
     * a repurposed counter (Asif behind it), a {@code VENDING_MACHINE_PROP}
     * (instant noodles, energy drinks — 1 COIN each), a {@code PRINTER_PROP},
     * and a BACK_ROOM behind a locked {@code DOOR_PROP}. Outside: a neon
     * OPEN/CLOSED sign.
     *
     * <p>Open daily 09:00–23:00. Run by Asif ({@code INTERNET_CAFE_OWNER} NPC);
     * nephew Hamza ({@code INTERNET_CAFE_ASSISTANT} NPC) covers 18:00–23:00.
     *
     * <p>Integrates with InternetCafeSystem, FenceSystem, PirateRadioSystem,
     * PhoneRepairSystem, JobCentreSystem, FactionSystem, NotorietySystem,
     * WantedSystem, CriminalRecord, DisguiseSystem, NeighbourhoodSystem,
     * WeatherSystem, NoiseSystem, WitnessSystem, RumourNetwork, AchievementSystem.
     */
    INTERNET_CAFE,

    /**
     * The Vaults — converted railway arch nightclub at the edge of the high
     * street near the Wetherspoons. Open Thu–Sun 22:00–03:00. Entry: 3 COIN
     * (Big Dave the BOUNCER on the door). Main floor: dancefloor, bar, DJ booth.
     * Lower level: VIP booths and fire exit.
     *
     * <p>Integrates with NightclubSystem, TaxiSystem, KebabVanSystem, RaveSystem,
     * MCBattleSystem, StreetSkillSystem, FactionSystem, NotorietySystem, WantedSystem,
     * CriminalRecord, DisguiseSystem, RumourNetwork, NeighbourhoodSystem,
     * WeatherSystem, NoiseSystem, NeighbourhoodWatchSystem, WitnessSystem,
     * AchievementSystem.
     */
    NIGHTCLUB;

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
            case BINGO_HALL:            return "Lucky Stars Bingo";
            case SNOOKER_HALL:          return "Cue Zone";
            case POST_OFFICE:           return "Northfield Post Office";
            case ARCADE:                return "Ace Amusements";
            case GREYHOUND_TRACK:       return "Northfield Dog Track";
            case TATTOO_PARLOUR:        return "Skin Deep Tattoos";
            case SCRAPYARD:             return "Pearce & Sons Metal Merchants";
            case INDOOR_MARKET:         return "Northfield Indoor Market";
            case BARBERS:               return "Kosta's Barbers";
            case ARGOS:                 return "Argos";
            case POUND_SHOP:            return "Poundstretcher";
            case ICE_CREAM_VAN:         return "Dave's Ices";
            case PAYDAY_LOAN_SHOP:      return "Fast Cash Finance";
            case CHINESE_TAKEAWAY:      return "Golden Palace";
            case MAGISTRATES_COURT:     return "Northfield Magistrates' Court";
            case PET_SHOP:              return "Paws 'n' Claws";
            case VET_SURGERY:           return "Northfield Vets";
            case INTERNET_CAFE:         return "Cybernet";
            case NIGHTCLUB:             return "The Vaults";
            default:                    return null; // No sign for parks, houses, etc.
        }
    }
}

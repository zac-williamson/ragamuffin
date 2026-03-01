package ragamuffin.core;

/**
 * Types of rumours that NPCs carry and spread through the rumour network.
 * Each type represents a category of gossip about the state of the world.
 */
public enum RumourType {

    /** "Someone's been causing bother near [landmark]" — generated when player commits a crime near a landmark. */
    PLAYER_SPOTTED,

    /** "I heard there's good stuff inside [landmark]" — generated at world-gen time for office/jeweller. */
    LOOT_TIP,

    /** "The [gang name] boys are running [territory name] now" — generated when gang territory reaches HOSTILE. */
    GANG_ACTIVITY,

    /** "You might want to talk to someone at [landmark]" — generated when a quest becomes available. */
    QUEST_LEAD,

    /** "The council's sending someone round about those buildings" — generated when demolition threshold crossed. */
    COUNCIL_NOTICE,

    /** "Heard it's going to [weather] later — dress warm / bring an umbrella" — seeded hourly by TimeSystem, accumulated by the barman. */
    WEATHER_TIP,

    /** "There's a rave on at [location] tonight — should be mental" — seeded when the player uses a FLYER at their squat. Draws nearby NPCs toward the squat as rave attendees. */
    RAVE_ANNOUNCEMENT,

    /** "Saw someone doing [crime] near [location]" — seeded when an NPC witnesses a crime and flees. Causes police NPCs who receive it to investigate the crime location. */
    WITNESS_SIGHTING,

    /** "Someone grassed on [faction/person]" — seeded when the player tips off police with a RUMOUR_NOTE. Turns the tipped faction hostile toward the player. */
    BETRAYAL,

    /** "Developers are moving in — prices are going up round here" — seeded when a Luxury Flat is built by The Council's gentrification wave. */
    GENTRIFICATION,

    /** "There's a new shop on the high street — bit dodgy if you ask me" — seeded when the player's corner shop opens or hits a revenue milestone. */
    SHOP_NEWS,

    /** "Heard the filth got a tip-off about someone causing bother" — seeded by WantedSystem when a witness reports a crime to police. */
    POLICE_TIP,

    /** "Someone just cleaned up at the bookies on a 33/1 shot" — seeded by HorseRacingSystem on a 33/1 win. Draws nearby NPCs toward the bookies for 60 seconds. */
    BIG_WIN_AT_BOOKIES,

    // ── Issue #916: Late-Night Kebab Van ─────────────────────────────────────

    /** "Someone half-inched a kebab from the van last night — they used a tin of beans" — seeded by KebabVanSystem when the player steals food via the TIN_OF_BEANS distraction. */
    THEFT,

    // ── Issue #926: Chippy System ─────────────────────────────────────────────

    /** "Someone jumped the queue outside Tony's — reckons they're above waiting" — seeded by ChippySystem when the player queue-jumps during the post-pub queue (23:00–01:00). */
    QUEUE_JUMP,

    /** "Someone punched Biscuit outside Tony's — Tony's furious" — seeded town-wide by ChippySystem when the player punches the STRAY_CAT NPC. */
    CAT_PUNCH,

    // ── Issue #928: Public Library System ─────────────────────────────────────

    /** "Heard someone got chucked out of the library — librarian's on the warpath" — seeded by LibrarySystem when the player is ejected by the LIBRARIAN NPC after repeat shushing. */
    LIBRARY_BAN,

    // ── Issue #934: Pigeon Racing System ─────────────────────────────────────

    /** "Race day tomorrow — lofts are out in Northfield. Should be a good one." — seeded by PigeonRacingSystem the evening before a race day, or when a race is postponed due to bad weather (+1 day reschedule). */
    PIGEON_RACE_DAY,

    /** "Heard someone's bird won the Northfield Derby yesterday — brought home the trophy an' all." — seeded by PigeonRacingSystem on a NORTHFIELD_DERBY win. Spreads via PIGEON_FANCIER NPCs. */
    PIGEON_VICTORY,

    // ── Issue #942: Food Bank System ─────────────────────────────────────────

    /** "Someone's been doing proper good at the food bank — donated again this week, apparently" — seeded by FoodBankSystem when the player donates surplus food or materials. Seeds into the barman's rumour buffer. */
    COMMUNITY_WIN,

    // ── Issue #950: Northfield Leisure Centre ─────────────────────────────────

    /** Leisure centre changing room gossip — one of 8 pool-specific lines seeded every 60s
     * when the player is within 5 blocks of a CHANGING_ROOM_PROP.
     * Spreads via the regular NPC rumour network. */
    CHANGING_ROOM_GOSSIP,

    // ── Issue #952: Clucky's Fried Chicken ───────────────────────────────────

    /** "There's a scrap kicking off outside Clucky's — someone's getting battered"
     * — seeded by FriedChickenShopSystem when 2–3 YOUTH_GANG NPCs enter FIGHTING_EACH_OTHER
     * state outside the shop. Triggers NOISE_EVENT and police patrol awareness. */
    FIGHT_NEARBY,

    // ── Issue #954: Northfield Bingo Hall ─────────────────────────────────────

    /** "Someone just had a full house at Lucky Stars — walked away with fifteen coin!"
     * — seeded by BingoSystem when the player wins a FULL HOUSE.
     * Spreads via nearby NPCs; draws curious PUBLIC NPCs toward the bingo hall. */
    WINNER,

    // ── Issue #963: Northfield Canal ──────────────────────────────────────────

    /** "Someone chucked something in the canal — right in front of me, an' all"
     * — seeded by CanalSystem when the player disposes of evidence in the water.
     * If a POLICE/WITNESS NPC is nearby the rumour records LITTERING. */
    LITTER,

    // ── Issue #969: Northfield Cemetery ──────────────────────────────────────

    /** "Did you see the funeral today? Poor sod."
     * — seeded by CemeterySystem after each funeral procession ends.
     * Spreads via nearby NPCs within 30 blocks of the cemetery.
     * Marks the freshly covered grave plot as DISTURBED (diggable in 4 hits). */
    FRESH_GRAVE,

    // ── Issue #971: The Rusty Anchor Wetherspoons ──────────────────────────────

    /** "There's something on tonight down [location] — worth a look" — seeded by
     * WetherspoonsSystem on quiz night start and Thursday curry club.
     * Gary at the Rusty Anchor seeds this type more frequently than GANG_ACTIVITY. */
    LOCAL_EVENT,

    /** "Round here, there's been a bit of bother with [topic]" — neighbourhood
     * gossip seeded by Gary (BARMAN) at The Rusty Anchor.
     * More common from Gary than GANG_ACTIVITY rumours. */
    NEIGHBOURHOOD,

    /** "Someone got proper kicked out of the pub last night — all hell broke loose"
     * — seeded by WetherspoonsSystem when the KICKED_OUT atmospheric event fires.
     * Spreads via NPCs within 30 blocks of The Rusty Anchor. */
    TROUBLE_AT_PUB,

    // ── Issue #975: Northfield Post Office ────────────────────────────────────

    /** "Heard someone's been nicking parcels off doorsteps — broad daylight an' all"
     * — seeded by PostOfficeSystem on PARCEL_THEFT when a witness is present.
     * Spreads via nearby NPCs; increases police patrol awareness in residential areas. */
    PARCEL_THEFT_SPOTTED,

    /** "Maureen at the post office caught someone trying to cash a stolen benefits book.
     * Phoned the police herself, she did."
     * — seeded by PostOfficeSystem on BENEFITS_FRAUD detection.
     * Spreads via PENSIONER NPCs who recognise the fraud pattern. */
    BENEFITS_FRAUD_CAUGHT,

    /** "Someone round here's been sending nasty letters. Police are asking questions."
     * — seeded by PostOfficeSystem when a threatening letter is traced at Tier 3+.
     * Spreads town-wide; increases WATCH_MEMBER vigilance. */
    THREATENING_LETTER,

    // ── Issue #981: Council Estate ────────────────────────────────────────────

    /** "Someone's been hammering on doors up on the estate at all hours."
     * — seeded by CouncilFlatsSystem when the player knocks on a resident door 3+
     * times after 22:00. Triggers NeighbourhoodWatchSystem +5 anger. */
    NOISE_COMPLAINT,

    // ── Issue #983: Northfield Dog Track ─────────────────────────────────────

    /** "Heard a dog went missing from the track kennels last night — Marchetti's not happy."
     * — seeded by GreyhoundRacingSystem when the player successfully steals a GREYHOUND.
     * Spreads via TRACK_PUNTER and KENNEL_HAND NPCs; increases SECURITY_GUARD patrols for
     * one session. Raises Marchetti Crew hostility by -5 Respect. */
    DOG_HEIST,

    /** "Someone fixed the dogs down the track — third trap never had a chance."
     * — seeded by GreyhoundRacingSystem when a race fix (DODGY_PIE or bribery) is witnessed.
     * Spreads via TRACK_PUNTER NPCs; triggers SECURITY_GUARD alert state.
     * If it reaches police: RACE_FIXING added to criminal record. */
    RACE_FIXED,

    // ── Issue #985: Northfield Police Station ─────────────────────────────────

    /** "Someone grassed to the station — pointed the finger at [faction]."
     * — seeded by PoliceStationSystem when the player uses the Tip Off menu.
     * NPCs carrying this rumour use it against the player: "I heard you grassed 'em up."
     * Turns the named faction hostile toward the player. */
    GRASSED_UP,

    /** "Someone broke out of the nick — lockpicked the cell door and legged it."
     * — seeded by PoliceStationSystem on a successful cell breakout.
     * Positive reputation with STREET_LADS (+5 Respect).
     * Spreads via PUBLIC NPCs near the police station. */
    GREAT_ESCAPE_RUMOUR,

    // ── Issue #998: Northfield Aldi Supermarket ────────────────────────────────

    /** "Someone knocked a trolley over down Aldi — Dave had to go and sort it out."
     * — seeded by SupermarketSystem when the player punches a SHOPPING_TROLLEY prop.
     * Adds +5 Notoriety; diverts SECURITY_GUARD Dave for 15 seconds. */
    VANDALISM,

    /** "Someone reckons there's a gold trolley down Aldi car park come closing."
     * — seeded by SupermarketSystem the night before the golden trolley spawns.
     * Spreads to 2 nearby NPCs via RumourNetwork. */
    URBAN_LEGEND,

    /** "Someone just legged it out of Aldi with a full basket — Dave went after 'em."
     * — seeded by SupermarketSystem when the player punches Dave (SECURITY_GUARD).
     * Spreads to 3 nearby NPCs; triggers WantedSystem stars and Notoriety gain. */
    ASSAULT,

    // ── Issue #1000: Northfield Fire Station ─────────────────────────────────

    /** "Someone only went and nicked the actual fire engine from the station — full flashing lights and all."
     * — seeded by FireStationSystem when the player completes the Engine Heist.
     * Spreads to 5 nearby NPCs via RumourNetwork. Raises FIREFIGHTER suspicion permanently.
     * Police NPCs who receive it immediately raise WantedSystem by +1 star. */
    MAJOR_THEFT,

    // ── Issue #1022: Northfield GP Surgery ────────────────────────────────────

    /** "Doctor Nair reckons half the street's got stress-related conditions — not surprised, to be honest."
     * — seeded weekly by GPSurgerySystem from Dr. Nair.
     * Spreads via GP_PATIENT and PUBLIC NPCs near the surgery.
     * Soft narrative flavour; no gameplay effect beyond atmosphere. */
    LOCAL_HEALTH,

    // ── Issue #1030: Al-Noor Mosque ────────────────────────────────────────────

    /** "Someone robbed the mosque collection box. Absolute disgrace."
     * — seeded by MosqueSystem when the player destroys the TAKINGS_BOX_PROP.
     * Spreads to all NPCs within 50 blocks. Raises hostility toward the player.
     * Contributes to LOWEST_OF_THE_LOW achievement unlock. */
    COMMUNITY_OUTRAGE,

    /** "The mosque is doing an Iftar tonight — free food for everyone, come along."
     * — seeded by MosqueSystem at Maghrib during Ramadan when the FOLD_TABLE_PROP is placed.
     * Spreads to nearby NPCs; draws WORSHIPPER and PUBLIC NPCs toward the mosque. */
    IFTAR_TONIGHT;
}

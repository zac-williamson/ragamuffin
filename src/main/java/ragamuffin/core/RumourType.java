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
    LITTER;
}

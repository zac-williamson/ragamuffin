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
    CAT_PUNCH;
}

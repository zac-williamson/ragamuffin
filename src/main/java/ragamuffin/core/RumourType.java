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
    GENTRIFICATION;
}

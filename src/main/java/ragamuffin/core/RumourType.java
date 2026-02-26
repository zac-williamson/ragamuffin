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
    COUNCIL_NOTICE;
}

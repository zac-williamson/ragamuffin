package ragamuffin.entity;

/**
 * States for NPC behavior state machines.
 */
public enum NPCState {
    IDLE,           // Standing still
    WANDERING,      // Random wandering
    GOING_TO_WORK,   // Daily routine: heading to work
    GOING_HOME,      // Daily routine: heading home
    AT_PUB,          // Daily routine: at the pub
    AT_HOME,         // Daily routine: at home
    GOING_TO_SCHOOL, // Daily routine: school kid heading to school
    AT_SCHOOL,       // Daily routine: school kid attending school
    LEAVING_SCHOOL,  // Daily routine: school kid leaving at end of day
    STARING,        // Reacting to player structure
    PHOTOGRAPHING,  // Taking photos of player structure
    COMPLAINING,    // Complaining about player structure
    STEALING,       // Youth gang stealing from player
    PATROLLING,     // Police patrolling
    SUSPICIOUS,     // Police heard player but hasn't seen them — investigating
    WARNING,        // Police issuing warning to player
    AGGRESSIVE,     // Police escalated state
    ARRESTING,      // Police arresting player
    DEMOLISHING,    // Council builder demolishing structure
    KNOCKED_BACK,   // Council builder knocked back (Phase 7)
    FLEEING,        // Civilian fleeing from notorious player
    KNOCKED_OUT,    // NPC defeated/incapacitated in combat
    ATTACKING,      // NPC performing an attack animation
    WAVING,         // NPC waving at the player
    DANCING,        // NPC doing a dance animation
    POINTING,       // NPC pointing at something
    SHELTERING,      // Issue #698: NPC sheltering under awning/indoors due to rain/thunderstorm
    ATTACKING_PLAYER, // Issue #702: Faction NPC actively attacking the player (hostile faction)
    FOLLOWING,          // Issue #706: NPC following the player (e.g. heist accomplice)
    FOLLOWING_PLAYER,   // Issue #709: Permanent accomplice following the player
    WITNESS,            // Issue #765: NPC witnessed a crime — fleeing toward landmark, will report to police
    REPORTING_TO_POLICE, // Issue #765: NPC reporting to police after witnessing a crime
    SCRUTINISING,       // Issue #767: NPC is suspicious of player's disguise — freeze-stares for 3 seconds
    CHASING_PLAYER,     // Issue #771: Police actively chasing player (hot pursuit)
    SEARCHING,          // Issue #771: Police searching area after losing line of sight
    HIDING,             // Issue #771: (unused by police) Player-initiated hide state marker on NPC prop

    // Issue #916: Late-Night Kebab Van
    QUEUING             // NPC is standing in line at the kebab van waiting to be served
}

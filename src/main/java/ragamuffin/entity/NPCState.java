package ragamuffin.entity;

/**
 * States for NPC behavior state machines.
 */
public enum NPCState {
    IDLE,           // Standing still
    WANDERING,      // Random wandering
    GOING_TO_WORK,  // Daily routine: heading to work
    GOING_HOME,     // Daily routine: heading home
    AT_PUB,         // Daily routine: at the pub
    AT_HOME,        // Daily routine: at home
    STARING,        // Reacting to player structure
    PHOTOGRAPHING,  // Taking photos of player structure
    COMPLAINING,    // Complaining about player structure
    STEALING,       // Youth gang stealing from player
    PATROLLING,     // Police patrolling
    WARNING,        // Police issuing warning to player
    AGGRESSIVE,     // Police escalated state
    ARRESTING,      // Police arresting player
    DEMOLISHING,    // Council builder demolishing structure
    KNOCKED_BACK,   // Council builder knocked back (Phase 7)
    FLEEING         // Civilian fleeing from notorious player
}

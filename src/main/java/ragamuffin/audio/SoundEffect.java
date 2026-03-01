package ragamuffin.audio;

/**
 * Sound effects available in the game.
 */
public enum SoundEffect {
    // Block sounds
    BLOCK_BREAK_WOOD,
    BLOCK_BREAK_STONE,
    BLOCK_BREAK_GLASS,
    BLOCK_PUNCH,
    BLOCK_PLACE,

    // Movement sounds
    FOOTSTEP_PAVEMENT,
    FOOTSTEP_GRASS,

    // UI sounds
    UI_CLICK,
    UI_OPEN,
    UI_CLOSE,
    INVENTORY_PICKUP,

    // NPC/Combat sounds
    NPC_HIT,
    PLAYER_DODGE,

    // Ambient/Environmental
    AMBIENT_PARK,
    AMBIENT_STREET,

    // Special events
    POLICE_SIREN,
    TOOLTIP,

    // Item use sounds
    ITEM_EAT,
    ITEM_USE,
    MUNCH,     // Large munching sound for crisp consumption
    PIRATE_RADIO_MUSIC,  // Looping lo-fi pirate radio jingle while broadcasting

    // Issue #932: Ice Cream Van System
    ICE_CREAM_JINGLE,  // Tinny ice cream van jingle; plays at each van stop

    // Issue #981: Council Estate
    LIFT_CREAK  // Creaking lift cable SFX; played while the lift is travelling between floors
}

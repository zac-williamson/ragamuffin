package ragamuffin.entity;

/**
 * Types of NPCs in the game, with combat stats.
 */
public enum NPCType {
    PUBLIC(20f, 0f, 0f, false),            // Members of the public - passive
    DOG(15f, 5f, 2.0f, false),             // Dogs - bite if provoked
    YOUTH_GANG(30f, 8f, 1.5f, true),       // Gangs - aggressive, steal and punch
    COUNCIL_MEMBER(25f, 0f, 0f, false),    // Bureaucratic - passive
    POLICE(50f, 10f, 1.0f, true),          // Police - tough, hit hard
    COUNCIL_BUILDER(40f, 5f, 2.0f, false), // Builders - defensive only
    SHOPKEEPER(20f, 0f, 0f, false),        // Stand near shops, comment on player
    POSTMAN(20f, 0f, 0f, false),           // Walk routes between buildings
    JOGGER(20f, 0f, 0f, false),            // Run through the park
    DRUNK(15f, 3f, 3.0f, false),           // Stumble around at night, mildly aggressive
    BUSKER(20f, 0f, 0f, false),            // Stand on high street, play music
    DELIVERY_DRIVER(20f, 0f, 0f, false),   // Amazon/JustEat driver rushing about
    PENSIONER(10f, 0f, 0f, false),          // Slow elderly person, complains a lot
    SCHOOL_KID(15f, 2f, 3.0f, false),      // Noisy school kids in groups
    STREET_PREACHER(20f, 0f, 0f, false),   // Named NPC: preacher with distinctive robes
    LOLLIPOP_LADY(20f, 0f, 0f, false),    // Named NPC: crossing patrol warden with hi-vis
    FENCE(25f, 0f, 0f, false);            // Black market trader — operates at charity shop / industrial estate

    private final float maxHealth;
    private final float attackDamage;   // Damage per hit to player
    private final float attackCooldown; // Seconds between attacks
    private final boolean hostile;      // Will actively seek and attack player

    NPCType(float maxHealth, float attackDamage, float attackCooldown, boolean hostile) {
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.attackCooldown = attackCooldown;
        this.hostile = hostile;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public boolean isHostile() {
        return hostile;
    }
}

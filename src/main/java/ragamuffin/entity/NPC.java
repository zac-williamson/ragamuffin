package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;

/**
 * Non-player character with AI behavior.
 * NPCs can wander, react to structures, and interact with the player.
 */
public class NPC {
    public static final float WIDTH = 0.6f;
    public static final float HEIGHT = 1.8f;
    public static final float DEPTH = 0.6f;
    public static final float MOVE_SPEED = 2.0f; // Slower than player
    public static final float DOG_SPEED = 5.0f; // Dogs move faster when roaming

    private final NPCType type;
    private final Vector3 position;
    private final Vector3 velocity;
    private final AABB aabb;
    private NPCState state;
    private Vector3 targetPosition;
    private String speechText;
    private float speechTimer;
    private int currentPathIndex;
    private java.util.List<Vector3> path;
    private float facingAngle; // degrees, 0 = +Z, 90 = +X
    private float animTime;    // accumulated animation time for walk cycle
    private float health;      // NPC health points
    private float attackCooldown; // time until NPC can attack again
    private boolean alive;

    public NPC(NPCType type, float x, float y, float z) {
        this.type = type;
        this.position = new Vector3(x, y, z);
        this.velocity = new Vector3();
        this.aabb = new AABB(position, WIDTH, HEIGHT, DEPTH);
        this.state = NPCState.IDLE;
        this.targetPosition = null;
        this.speechText = null;
        this.speechTimer = 0;
        this.currentPathIndex = 0;
        this.path = null;
        this.facingAngle = 0f;
        this.animTime = 0f;
        this.health = type.getMaxHealth();
        this.attackCooldown = 0f;
        this.alive = true;
    }

    public NPCType getType() {
        return type;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public AABB getAABB() {
        return aabb;
    }

    public NPCState getState() {
        return state;
    }

    public void setState(NPCState newState) {
        this.state = newState;
    }

    public Vector3 getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Vector3 target) {
        this.targetPosition = target;
    }

    public String getSpeechText() {
        return speechText;
    }

    public void setSpeechText(String text, float duration) {
        this.speechText = text;
        this.speechTimer = duration;
    }

    public boolean isSpeaking() {
        return speechTimer > 0 && speechText != null;
    }

    public java.util.List<Vector3> getPath() {
        return path;
    }

    public void setPath(java.util.List<Vector3> newPath) {
        this.path = newPath;
        this.currentPathIndex = 0;
    }

    public int getCurrentPathIndex() {
        return currentPathIndex;
    }

    public void advancePathIndex() {
        if (path != null && currentPathIndex < path.size() - 1) {
            currentPathIndex++;
        }
    }

    /**
     * Update NPC state and behavior.
     */
    public void update(float delta) {
        // Update speech timer
        if (speechTimer > 0) {
            speechTimer -= delta;
            if (speechTimer <= 0) {
                speechText = null;
            }
        }

        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }

        // Update facing angle from velocity
        float hSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (hSpeedSq > 0.001f) {
            facingAngle = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
            animTime += delta;
        }

        // Update position with velocity
        position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
    }

    public float getFacingAngle() {
        return facingAngle;
    }

    public void setFacingAngle(float angle) {
        this.facingAngle = angle;
    }

    public float getAnimTime() {
        return animTime;
    }

    /**
     * Move the NPC in the given direction.
     */
    public void move(float dx, float dy, float dz, float delta) {
        velocity.set(dx, dy, dz).nor().scl(MOVE_SPEED * delta);
        position.add(velocity);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
        // Update facing angle based on horizontal movement
        if (dx != 0 || dz != 0) {
            facingAngle = (float) Math.toDegrees(Math.atan2(dx, dz));
        }
    }

    /**
     * Set velocity directly.
     */
    public void setVelocity(float x, float y, float z) {
        velocity.set(x, y, z);
    }

    /**
     * Apply knockback to the NPC.
     */
    public void applyKnockback(Vector3 direction, float force) {
        Vector3 knockback = direction.cpy().nor().scl(force);
        position.add(knockback);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
    }

    /**
     * Check if NPC is within a certain distance of a position.
     */
    public boolean isNear(Vector3 pos, float distance) {
        return position.dst(pos) <= distance;
    }

    /**
     * Check if NPC is within a bounding box (for park boundaries, etc.).
     */
    public boolean isWithinBounds(float minX, float minZ, float maxX, float maxZ) {
        return position.x >= minX && position.x <= maxX &&
               position.z >= minZ && position.z <= maxZ;
    }

    // Combat methods

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public boolean isAlive() {
        return alive;
    }

    /**
     * Apply damage to this NPC. Returns true if the NPC died.
     */
    public boolean takeDamage(float amount) {
        health -= amount;
        if (health <= 0) {
            health = 0;
            alive = false;
            return true;
        }
        return false;
    }

    /**
     * Check if this NPC can attack (cooldown expired).
     */
    public boolean canAttack() {
        return attackCooldown <= 0 && alive;
    }

    /**
     * Reset attack cooldown after attacking.
     */
    public void resetAttackCooldown() {
        attackCooldown = type.getAttackCooldown();
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }
}

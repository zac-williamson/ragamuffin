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

        // Update position with velocity
        position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
    }

    /**
     * Move the NPC in the given direction.
     */
    public void move(float dx, float dy, float dz, float delta) {
        velocity.set(dx, dy, dz).nor().scl(MOVE_SPEED * delta);
        position.add(velocity);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
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
}

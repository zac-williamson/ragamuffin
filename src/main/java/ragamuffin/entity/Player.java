package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.core.StreetReputation;
import ragamuffin.world.BlockType;
import ragamuffin.world.Chunk;

/**
 * The player entity with position, movement, and collision.
 */
public class Player {
    public static final float WIDTH = 0.6f;
    public static final float HEIGHT = 1.8f;
    public static final float DEPTH = 0.6f;
    public static final float EYE_HEIGHT = 1.62f; // Eye level for camera
    public static final float MOVE_SPEED = 12.0f;
    public static final float SPRINT_SPEED = 20.0f;
    public static final float GRAVITY = 9.8f; // Gravity acceleration (m/s^2)
    public static final float JUMP_VELOCITY = 6.0f; // Initial upward velocity when jumping

    // Dodge/roll constants
    public static final float DODGE_SPEED = 25.0f;   // Burst speed during dodge
    public static final float DODGE_DURATION = 0.3f;  // How long the dodge lasts (seconds)
    public static final float DODGE_COOLDOWN = 1.0f;  // Cooldown between dodges (seconds)
    public static final float DODGE_ENERGY_COST = 15f; // Energy cost per dodge

    // Phase 8: Survival stats
    public static final float MAX_HEALTH = 100f;
    public static final float MAX_HUNGER = 100f;
    public static final float MAX_ENERGY = 100f;
    public static final float HUNGER_DRAIN_PER_MINUTE = 2f; // 50 minutes to starve
    public static final float ENERGY_DRAIN_PER_ACTION = 1f;
    public static final float ENERGY_RECOVERY_PER_SECOND = 5f; // 20 seconds to full recovery

    private final Vector3 position;
    private final Vector3 velocity;
    private final AABB aabb;

    private float health;
    private float hunger;
    private float energy;
    private boolean isDead;
    private float verticalVelocity; // Separate vertical velocity for gravity
    private float fallStartY; // Y position when the player started falling
    private boolean isFalling; // Whether the player is currently falling

    // Dodge state
    private boolean isDodging;
    private float dodgeTimer;       // Time remaining in current dodge
    private float dodgeCooldownTimer; // Time until dodge is available again
    private float dodgeDirX;        // Dodge direction X
    private float dodgeDirZ;        // Dodge direction Z

    // Damage flash
    private float damageFlashTimer; // > 0 when recently damaged
    private static final float DAMAGE_FLASH_DURATION = 0.35f;

    // Street reputation
    private StreetReputation streetReputation;

    public Player(float x, float y, float z) {
        this.position = new Vector3(x, y, z);
        this.velocity = new Vector3();
        this.aabb = new AABB(position, WIDTH, HEIGHT, DEPTH);
        this.health = MAX_HEALTH;
        this.hunger = MAX_HUNGER;
        this.energy = MAX_ENERGY;
        this.isDead = false;
        this.verticalVelocity = 0f;
        this.fallStartY = y;
        this.isFalling = false;
        this.isDodging = false;
        this.dodgeTimer = 0f;
        this.dodgeCooldownTimer = 0f;
        this.dodgeDirX = 0f;
        this.dodgeDirZ = 0f;
        this.damageFlashTimer = 0f;
        this.streetReputation = new StreetReputation();
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

    /**
     * Move the player in the given direction.
     * @param dx X direction
     * @param dy Y direction
     * @param dz Z direction
     * @param delta Delta time in seconds
     */
    public void move(float dx, float dy, float dz, float delta) {
        velocity.set(dx, dy, dz).nor().scl(MOVE_SPEED * delta);
        position.add(velocity);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
    }

    /**
     * Update player position and apply velocity.
     */
    public void update(float delta) {
        position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
    }

    /**
     * Set velocity for next frame.
     */
    public void setVelocity(float x, float y, float z) {
        velocity.set(x, y, z);
    }

    /**
     * Check for collision against a chunk and resolve if needed.
     * Returns true if collision occurred.
     */
    public boolean checkCollision(Chunk chunk) {
        // Get the range of blocks the AABB might intersect
        int minX = (int) Math.floor(aabb.getMinX()) - chunk.getChunkX() * Chunk.SIZE;
        int maxX = (int) Math.ceil(aabb.getMaxX()) - chunk.getChunkX() * Chunk.SIZE;
        int minY = (int) Math.floor(aabb.getMinY());
        int maxY = (int) Math.ceil(aabb.getMaxY());
        int minZ = (int) Math.floor(aabb.getMinZ()) - chunk.getChunkZ() * Chunk.SIZE;
        int maxZ = (int) Math.ceil(aabb.getMaxZ()) - chunk.getChunkZ() * Chunk.SIZE;

        boolean collided = false;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = chunk.getBlock(x, y, z);
                    if (block.isSolid()) {
                        AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                        if (aabb.intersects(blockBox)) {
                            collided = true;
                        }
                    }
                }
            }
        }
        return collided;
    }

    /**
     * Attempt to move with collision detection against a chunk.
     * Returns the actual distance moved.
     */
    public Vector3 moveWithCollision(float dx, float dy, float dz, float delta, Chunk chunk) {
        Vector3 desiredMove = new Vector3(dx, dy, dz).nor().scl(MOVE_SPEED * delta);
        Vector3 originalPos = new Vector3(position);

        // Try full movement
        position.add(desiredMove);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);

        if (checkCollision(chunk)) {
            // Collision - revert and try sliding
            position.set(originalPos);

            // Try X only
            position.add(desiredMove.x, 0, 0);
            aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
            if (checkCollision(chunk)) {
                position.set(originalPos);
            }

            // Try Z only
            Vector3 zOnlyPos = new Vector3(position);
            position.add(0, 0, desiredMove.z);
            aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
            if (checkCollision(chunk)) {
                position.set(zOnlyPos);
            }

            aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
        }

        return new Vector3(position).sub(originalPos);
    }

    // Phase 8: Health/Hunger/Energy management

    public float getHealth() {
        return health;
    }

    public float getHunger() {
        return hunger;
    }

    public float getEnergy() {
        return energy;
    }

    public boolean isDead() {
        return isDead;
    }

    /**
     * Apply damage to the player.
     */
    public void damage(float amount) {
        health = Math.max(0, health - amount);
        if (health <= 0) {
            isDead = true;
        }
        damageFlashTimer = DAMAGE_FLASH_DURATION;
    }

    /**
     * Tick the damage flash timer. Call once per frame.
     */
    public void updateFlash(float delta) {
        if (damageFlashTimer > 0) {
            damageFlashTimer = Math.max(0, damageFlashTimer - delta);
        }
    }

    /**
     * Get damage flash intensity (0.0 = no flash, 1.0 = full flash).
     */
    public float getDamageFlashIntensity() {
        return damageFlashTimer / DAMAGE_FLASH_DURATION;
    }

    /**
     * Heal the player.
     */
    public void heal(float amount) {
        health = Math.min(MAX_HEALTH, health + amount);
    }

    /**
     * Decrease hunger over time (called every frame).
     */
    public void updateHunger(float deltaSeconds) {
        hunger = Math.max(0, hunger - (HUNGER_DRAIN_PER_MINUTE / 60f) * deltaSeconds);
    }

    /**
     * Feed the player to restore hunger.
     */
    public void eat(float amount) {
        hunger = Math.min(MAX_HUNGER, hunger + amount);
    }

    /**
     * Consume energy for an action (punching, placing, etc.).
     */
    public void consumeEnergy(float amount) {
        energy = Math.max(0, energy - amount);
    }

    /**
     * Recover energy over time when not performing actions.
     */
    public void recoverEnergy(float deltaSeconds) {
        energy = Math.min(MAX_ENERGY, energy + ENERGY_RECOVERY_PER_SECOND * deltaSeconds);
    }

    /**
     * Restore energy by a fixed amount (for consumables).
     */
    public void restoreEnergy(float amount) {
        energy = Math.min(MAX_ENERGY, energy + amount);
    }

    /**
     * Revive the player (reset dead state).
     */
    public void revive() {
        isDead = false;
    }

    /**
     * Set health directly (for testing).
     */
    public void setHealth(float health) {
        this.health = Math.max(0, Math.min(MAX_HEALTH, health));
        if (this.health <= 0) {
            isDead = true;
        } else {
            isDead = false;
        }
    }

    /**
     * Set hunger directly (for testing).
     */
    public void setHunger(float hunger) {
        this.hunger = Math.max(0, Math.min(MAX_HUNGER, hunger));
    }

    /**
     * Set energy directly (for testing).
     */
    public void setEnergy(float energy) {
        this.energy = Math.max(0, Math.min(MAX_ENERGY, energy));
    }

    /**
     * Get vertical velocity (for gravity).
     */
    public float getVerticalVelocity() {
        return verticalVelocity;
    }

    /**
     * Set vertical velocity (for gravity/testing).
     */
    public void setVerticalVelocity(float verticalVelocity) {
        this.verticalVelocity = verticalVelocity;
    }

    /**
     * Apply gravity to vertical velocity.
     */
    public void applyGravity(float delta) {
        verticalVelocity -= GRAVITY * delta;
    }

    /**
     * Reset vertical velocity (when landing on ground).
     */
    public void resetVerticalVelocity() {
        verticalVelocity = 0f;
    }

    /**
     * Jump if on ground (sets upward velocity).
     */
    public void jump() {
        verticalVelocity = JUMP_VELOCITY;
    }

    /**
     * Start tracking a fall from the current Y position.
     */
    public void startFalling() {
        if (!isFalling) {
            fallStartY = position.y;
            isFalling = true;
        }
    }

    /**
     * Called when the player lands. Returns fall damage based on distance fallen.
     * Falls greater than 10 blocks deal 10 damage per block over 10.
     */
    public float landAndGetFallDamage() {
        if (!isFalling) return 0f;
        isFalling = false;

        float fallDistance = fallStartY - position.y;
        if (fallDistance > 10f) {
            float damageBlocks = fallDistance - 10f;
            return damageBlocks * 10f; // 10 HP per block over the threshold
        }
        return 0f;
    }

    public boolean isFalling() {
        return isFalling;
    }

    // ========== Dodge/Roll System ==========

    /**
     * Attempt to start a dodge roll in the given direction.
     * Returns true if the dodge was initiated.
     * @param dirX horizontal X direction (normalised)
     * @param dirZ horizontal Z direction (normalised)
     */
    public boolean dodge(float dirX, float dirZ) {
        if (isDodging || dodgeCooldownTimer > 0 || energy < DODGE_ENERGY_COST) {
            return false;
        }
        // Need a movement direction
        float len = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 0.01f) {
            return false;
        }
        isDodging = true;
        dodgeTimer = DODGE_DURATION;
        dodgeCooldownTimer = DODGE_COOLDOWN;
        dodgeDirX = dirX / len;
        dodgeDirZ = dirZ / len;
        consumeEnergy(DODGE_ENERGY_COST);
        return true;
    }

    /**
     * Update dodge timers each frame.
     */
    public void updateDodge(float delta) {
        if (isDodging) {
            dodgeTimer -= delta;
            if (dodgeTimer <= 0) {
                isDodging = false;
                dodgeTimer = 0f;
            }
        }
        if (dodgeCooldownTimer > 0) {
            dodgeCooldownTimer -= delta;
            if (dodgeCooldownTimer < 0) {
                dodgeCooldownTimer = 0f;
            }
        }
    }

    /**
     * Whether the player is currently mid-dodge (invincible).
     */
    public boolean isDodging() {
        return isDodging;
    }

    /**
     * Get the dodge movement direction X.
     */
    public float getDodgeDirX() {
        return dodgeDirX;
    }

    /**
     * Get the dodge movement direction Z.
     */
    public float getDodgeDirZ() {
        return dodgeDirZ;
    }

    /**
     * Whether the dodge cooldown has expired and a new dodge is available.
     */
    public boolean canDodge() {
        return !isDodging && dodgeCooldownTimer <= 0 && energy >= DODGE_ENERGY_COST;
    }

    /**
     * Get remaining dodge cooldown (for UI).
     */
    public float getDodgeCooldownTimer() {
        return dodgeCooldownTimer;
    }

    /**
     * Get the player's street reputation.
     */
    public StreetReputation getStreetReputation() {
        return streetReputation;
    }
}

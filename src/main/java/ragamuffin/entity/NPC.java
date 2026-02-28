package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Material;
import ragamuffin.core.Rumour;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;

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
    private NPCModelVariant modelVariant; // Physical appearance variant for 3D model rendering
    private String name; // Optional unique name (null = anonymous NPC)
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
        this(type, null, x, y, z);
    }

    public NPC(NPCType type, String name, float x, float y, float z) {
        this.type = type;
        this.name = name;
        this.modelVariant = NPCModelVariant.DEFAULT;
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
        // Stagger initial blink timer so nearby NPCs don't all blink simultaneously.
        // Use a deterministic offset based on position so it's consistent across frames.
        this.blinkTimer = (Math.abs(x * 7.3f + z * 3.1f) % blinkInterval);
        // Issue #765: Deterministic bravery in [0,1] based on position hash.
        this.bravery = Math.abs((x * 13.7f + z * 9.3f) % 1.0f);
    }

    public NPCType getType() {
        return type;
    }

    /**
     * Returns this NPC's unique name, or null if anonymous.
     */
    public String getName() {
        return name;
    }

    /**
     * Set a unique name for this NPC.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns true if this NPC has a unique name assigned.
     */
    public boolean isNamed() {
        return name != null && !name.isEmpty();
    }

    /**
     * Returns the physical appearance variant used when rendering this NPC's 3D model.
     * Defaults to {@link NPCModelVariant#DEFAULT} on construction.
     */
    public NPCModelVariant getModelVariant() {
        return modelVariant;
    }

    /**
     * Set the physical appearance variant for this NPC's 3D model.
     * The renderer reads this each frame to apply the appropriate scale factors.
     */
    public void setModelVariant(NPCModelVariant variant) {
        this.modelVariant = (variant != null) ? variant : NPCModelVariant.DEFAULT;
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
        if (path != null) {
            currentPathIndex++;
        }
    }

    /**
     * Update NPC state and behavior.
     */
    public void update(float delta) {
        updateTimers(delta);

        // Update position with velocity
        position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta);
        aabb.setPosition(position, WIDTH, HEIGHT, DEPTH);
    }

    /**
     * Advance only the speech timer by delta seconds.
     * Called in the PAUSED branch so speech bubbles count down while the game is paused
     * without advancing attack cooldowns, blink cycles, or animation timers (Fix #423).
     */
    public void tickSpeechOnly(float delta) {
        if (speechTimer > 0) {
            speechTimer -= delta;
            if (speechTimer <= 0) {
                speechText = null;
                shopMenuOpen = false;
                selectedShopItem = 1;
            }
        }
    }

    /**
     * Update timers, facing angle, and animation — but NOT position.
     * Used by NPCManager which handles movement with world collision separately.
     */
    public void updateTimers(float delta) {
        // Update speech timer
        if (speechTimer > 0) {
            speechTimer -= delta;
            if (speechTimer <= 0) {
                speechText = null;
                // Shop menu expires with the speech bubble so a player who walks
                // away does not find the menu still open on return.
                shopMenuOpen = false;
                selectedShopItem = 1;
            }
        }

        // Update attack cooldown
        if (attackCooldown > 0) {
            attackCooldown -= delta;
        }

        // Update pickpocket cooldown (Issue #709)
        if (pickpocketCooldown > 0) {
            pickpocketCooldown -= delta;
        }

        // Update facing angle from velocity
        // atan2(vx, vz) gives the angle from +Z axis toward +X axis
        // The model's face is now at +Z local, so yaw 0 = face toward +Z
        float hSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (hSpeedSq > 0.001f) {
            facingAngle = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
            animTime += delta;
            idleTime = 0f;
        } else {
            idleTime += delta;
        }

        // Update blink cycle — NPCs blink periodically
        if (state != NPCState.KNOCKED_OUT) {
            blinkTimer += delta;
            if (!blinking && blinkTimer >= blinkInterval) {
                blinking = true;
                blinkTimer = 0f;
            } else if (blinking && blinkTimer >= blinkDuration) {
                blinking = false;
                blinkTimer = 0f;
            }
        } else {
            // Knocked-out NPCs have eyes closed
            blinking = true;
        }
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
     * Returns how long (in seconds) the NPC has been continuously stationary.
     * Resets to zero whenever the NPC starts moving. Used for idle animations.
     */
    public float getIdleTime() {
        return idleTime;
    }

    /**
     * Move the NPC in the given direction.
     */
    public void move(float dx, float dy, float dz, float delta) {
        velocity.set(dx, dy, dz).nor().scl(MOVE_SPEED);
        position.add(velocity.x * delta, velocity.y * delta, velocity.z * delta);
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
     * Apply knockback to the NPC as a velocity impulse rather than
     * teleporting through terrain. The knockback force is applied as
     * a large velocity that gets collision-checked each frame.
     */
    public void applyKnockback(Vector3 direction, float force) {
        Vector3 kb = direction.cpy().nor().scl(force * 6f); // moderate velocity impulse
        velocity.set(kb.x, 2f, kb.z); // slight upward pop
        knockbackTimer = 0.2f; // maintain knockback velocity for 0.2 seconds
    }

    private float knockbackTimer = 0f;
    private float knockedOutTimer = 0f; // how long NPC has been in KNOCKED_OUT state
    private float stuckTimer = 0f;     // time spent stuck against obstacle
    private Vector3 lastPosition = null; // position last frame for stuck detection
    private float idleTime = 0f;       // accumulated time the NPC has been stationary (for idle animations)

    // Blink animation state
    private float blinkTimer = 0f;       // counts up; resets after each blink cycle
    private float blinkInterval = 3.5f;  // seconds between blinks (varies per NPC)
    private float blinkDuration = 0.12f; // seconds the eyes stay shut
    private boolean blinking = false;    // true while eyes are shut

    // Stolen items — tracks what this NPC has stolen from the player so it can be recovered
    private final List<Material> stolenItems = new ArrayList<>();

    // Rumour network — buffer of gossip this NPC carries
    private final List<Rumour> rumours = new ArrayList<>();

    // Index into barman's rumour buffer for cycling through rumours when the player talks to them
    private int barmanRumourIndex = 0;

    // Building association — set for static quest NPCs stationed inside labelled buildings
    private LandmarkType buildingType = null;

    // Pickpocket cooldown — set after a successful pickpocket; prevents re-picking the same NPC
    private float pickpocketCooldown = 0f; // counts down to 0

    // Shop interaction state — true when the player has opened the shop menu (first E-press)
    // and a purchase is awaited on the next E-press.
    private boolean shopMenuOpen = false;

    // Issue #765: Witness bravery — 0.0 (cowardly) to 1.0 (very brave).
    // Determines success chance when the player tries to bribe this NPC to stay quiet.
    // Bravery > 0.7 = NPC resists most bribes. Bravery < 0.3 = NPC accepts easily.
    // Initialised deterministically from position so it's stable across updates.
    private float bravery;

    // Issue #765: Witness report timer — when this NPC is in WITNESS state, counts
    // down from WITNESS_REPORT_DELAY to 0, after which the NPC reports to police.
    private float witnessReportTimer = 0f;

    // The item index (1-3) the player has highlighted in the shop menu.
    // 1 = sausage roll, 2 = energy drink, 3 = crisps.
    // Defaults to 1 when the menu opens. Only meaningful while shopMenuOpen is true.
    private int selectedShopItem = 1;

    public boolean isKnockedBack() {
        return knockbackTimer > 0f;
    }

    public float getKnockedOutTimer() {
        return knockedOutTimer;
    }

    /**
     * Advance the KNOCKED_OUT timer by delta seconds.
     * Separated from {@link #updateTimers(float)} so that NPCManager can tick it
     * independently (both during the normal update loop and in the PAUSED branch via
     * {@code tickRecoveryTimers}) without double-advancing other per-NPC timers.
     */
    public void tickKnockedOutTimer(float delta) {
        if (state == NPCState.KNOCKED_OUT) {
            knockedOutTimer += delta;
        }
    }

    public void updateKnockback(float delta) {
        if (knockbackTimer > 0f) {
            knockbackTimer -= delta;
            if (knockbackTimer <= 0f) {
                knockbackTimer = 0f;
                velocity.set(0, 0, 0);
            }
        }
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

    /**
     * Update stuck detection — call after collision-resolved movement.
     * Returns true if NPC has been stuck for a significant time.
     */
    public boolean updateStuckDetection(float delta) {
        float hSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (hSpeedSq < 0.001f) {
            // Not trying to move — not stuck
            stuckTimer = 0f;
            lastPosition = null;
            return false;
        }

        if (lastPosition == null) {
            lastPosition = position.cpy();
            stuckTimer = 0f;
            return false;
        }

        // Check if we've barely moved despite having velocity.
        // Compare actual movement to 10% of the expected movement this frame;
        // this avoids false positives at high frame rates where per-frame
        // displacement is much smaller than the 0.1-block absolute threshold.
        float distMoved = position.dst2(lastPosition); // squared distance
        float minExpectedMoveSq = hSpeedSq * delta * delta * 0.1f; // 10% of (speed * delta)^2
        if (distMoved < minExpectedMoveSq) {
            stuckTimer += delta;
        } else {
            stuckTimer = 0f;
        }
        lastPosition.set(position);

        return stuckTimer > 0.3f; // stuck for more than 0.3 seconds
    }

    public void resetStuckTimer() {
        stuckTimer = 0f;
        lastPosition = null;
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
     * Revive this NPC after being knocked out.
     * Restores health to half maximum, resets the knocked-out timer,
     * and transitions the NPC back to WANDERING so it resumes normal behaviour.
     * Called by NPCManager.tickRecoveryTimers() when the recovery duration expires.
     */
    public void revive() {
        alive = true;
        health = type.getMaxHealth() * 0.5f;
        knockedOutTimer = 0f;
        state = NPCState.WANDERING;
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

    /**
     * Derive the facial expression from the NPC's current behavioral state.
     * This is used by the renderer to select the correct face geometry.
     */
    public FacialExpression getFacialExpression() {
        switch (state) {
            case AGGRESSIVE:
            case ARRESTING:
            case DEMOLISHING:
            case WARNING:
            case ATTACKING:
                return FacialExpression.ANGRY;
            case FLEEING:
            case WITNESS:
            case REPORTING_TO_POLICE:
                return FacialExpression.SCARED;
            case AT_PUB:
            case AT_HOME:
            case WAVING:
            case DANCING:
                return FacialExpression.HAPPY;
            case STARING:
            case PHOTOGRAPHING:
            case POINTING:
                return FacialExpression.SURPRISED;
            case COMPLAINING:
            case STEALING:
                return FacialExpression.DISGUSTED;
            default:
                return FacialExpression.NEUTRAL;
        }
    }

    /**
     * Returns the current value of the blink timer (counts up between blink cycles).
     * Used in tests to verify the blink cycle does not advance while paused (Fix #423).
     */
    public float getBlinkTimer() {
        return blinkTimer;
    }

    /**
     * Returns true while the NPC's eyes are shut (mid-blink).
     * Used by the renderer to substitute a closed-eye face.
     */
    public boolean isBlinking() {
        return blinking;
    }

    /**
     * Returns true when the NPC's mouth should appear open — i.e. while speaking.
     * The renderer uses this to animate the mouth for speech.
     */
    public boolean isMouthOpen() {
        return isSpeaking();
    }

    /**
     * Get the building type this NPC is associated with, or null if not a building NPC.
     */
    public LandmarkType getBuildingType() {
        return buildingType;
    }

    /**
     * Associate this NPC with a labelled building (marks it as a static quest NPC).
     */
    public void setBuildingType(LandmarkType buildingType) {
        this.buildingType = buildingType;
    }

    /**
     * Returns a unique shop identifier for this shopkeeper based on the linked shop.
     * For a shopkeeper linked to GREGGS this returns "SHOPKEEPER_GREGGS",
     * for one linked to OFF_LICENCE it returns "SHOPKEEPER_OFF_LICENCE", etc.
     * Returns null if this NPC is not linked to a shop (no buildingType set).
     *
     * Issue #874: Shopkeepers have unique identification that corresponds to
     * which shop they are linked to.
     */
    public String getShopId() {
        if (buildingType == null) {
            return null;
        }
        return "SHOPKEEPER_" + buildingType.name();
    }

    /**
     * Returns true when the shop menu is currently open for this shopkeeper
     * (the player has pressed E once and is now being shown available items).
     */
    public boolean isShopMenuOpen() {
        return shopMenuOpen;
    }

    /**
     * Open or close the shop menu for this shopkeeper NPC.
     * Opening the menu resets the selection to item 1 (sausage roll).
     */
    public void setShopMenuOpen(boolean open) {
        this.shopMenuOpen = open;
        if (open) {
            selectedShopItem = 1;
        }
    }

    /**
     * Returns the currently highlighted shop item index (1-3).
     * 1 = sausage roll, 2 = energy drink, 3 = crisps.
     * Only meaningful while the shop menu is open.
     */
    public int getSelectedShopItem() {
        return selectedShopItem;
    }

    /**
     * Set the highlighted shop item index (1-3).
     * Clamped to the valid range [1, 3].
     */
    public void setSelectedShopItem(int index) {
        if (index >= 1 && index <= 3) {
            this.selectedShopItem = index;
        }
    }

    /**
     * Record an item stolen by this NPC so the player can recover it.
     */
    public void addStolenItem(Material material) {
        stolenItems.add(material);
    }

    /**
     * Return and clear all items stolen by this NPC.
     */
    public List<Material> claimStolenItems() {
        List<Material> items = new ArrayList<>(stolenItems);
        stolenItems.clear();
        return items;
    }

    /**
     * How many items this NPC has stolen.
     */
    public int getStolenItemCount() {
        return stolenItems.size();
    }

    // ── Rumour network ──────────────────────────────────────────────────────────

    // ── Pickpocket cooldown (Issue #709) ────────────────────────────────────────

    /**
     * Returns true if this NPC is currently on pickpocket cooldown
     * (cannot be successfully pickpocketed again yet).
     */
    public boolean isPickpocketCooldown() {
        return pickpocketCooldown > 0f;
    }

    /**
     * Set the pickpocket cooldown timer in seconds.
     */
    public void setPickpocketCooldown(float seconds) {
        this.pickpocketCooldown = seconds;
    }

    /**
     * Returns the remaining pickpocket cooldown in seconds.
     */
    public float getPickpocketCooldown() {
        return pickpocketCooldown;
    }

    // ── Rumour network ──────────────────────────────────────────────────────────

    /**
     * Direct access to this NPC's rumour buffer (mutable).
     * Used by RumourNetwork to add/remove rumours.
     */
    public List<Rumour> getRumours() {
        return rumours;
    }

    /**
     * Returns the top rumour (most recently added) this NPC holds, or null if empty.
     */
    public Rumour getTopRumour() {
        if (rumours.isEmpty()) return null;
        return rumours.get(rumours.size() - 1);
    }

    // ── Issue #765: Witness system ──────────────────────────────────────────────

    /**
     * Returns this NPC's bravery value in [0,1].
     * Used by the witness bribery mechanic to determine success chance.
     * High bravery means the NPC is harder to bribe into silence.
     */
    public float getBravery() {
        return bravery;
    }

    /**
     * Set this NPC's bravery value (clamped to [0,1]).
     */
    public void setBravery(float bravery) {
        this.bravery = Math.max(0f, Math.min(1f, bravery));
    }

    /**
     * Returns the remaining witness report timer in seconds.
     * When &gt; 0 the NPC has witnessed a crime and will report to police when it reaches 0.
     */
    public float getWitnessReportTimer() {
        return witnessReportTimer;
    }

    /**
     * Set the witness report timer (seconds until the NPC reports to police).
     */
    public void setWitnessReportTimer(float seconds) {
        this.witnessReportTimer = seconds;
    }

    /**
     * Tick the witness report timer by delta seconds.
     * Returns true if the timer just expired (NPC should now report to police).
     */
    public boolean tickWitnessTimer(float delta) {
        if (witnessReportTimer <= 0f) return false;
        witnessReportTimer -= delta;
        if (witnessReportTimer <= 0f) {
            witnessReportTimer = 0f;
            return true;
        }
        return false;
    }

    /**
     * Returns the barman's current rumour to share with the player,
     * cycling through all accumulated rumours on each call.
     * Returns null if the barman has no rumours.
     */
    public Rumour cycleBarmanRumour() {
        if (rumours.isEmpty()) return null;
        Rumour r = rumours.get(barmanRumourIndex % rumours.size());
        barmanRumourIndex = (barmanRumourIndex + 1) % rumours.size();
        return r;
    }
}

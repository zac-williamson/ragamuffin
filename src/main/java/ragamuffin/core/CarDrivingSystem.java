package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.ai.CarManager;
import ragamuffin.entity.Car;
import ragamuffin.entity.Player;
import ragamuffin.world.World;

/**
 * Issue #773 — Car interaction and driving mechanics.
 *
 * Handles entering and exiting parked/moving cars, as well as player-controlled
 * driving input.  The player presses E (interact) while near a car to enter it.
 * While driving, WASD controls the vehicle: W accelerates, S brakes/reverses,
 * A/D steer.  ESC or E again exits the car, placing the player beside it.
 *
 * Driving physics are delegated to {@link Car#updatePlayerDriven}.
 */
public class CarDrivingSystem {

    /** How close the player must be (centre-to-centre, blocks) to enter a car. */
    public static final float ENTER_RANGE = 3.0f;

    /** Offset applied when the player exits a car (to avoid spawning inside it). */
    public static final float EXIT_OFFSET = 2.5f;

    /** The car the player is currently driving, or null if on foot. */
    private Car currentCar = null;

    /** Whether the player is currently inside a car. */
    private boolean inCar = false;

    /** Last message generated (e.g. "You get in the car."). */
    private String lastMessage = null;

    private final CarManager carManager;
    private World world;

    public CarDrivingSystem(CarManager carManager) {
        this.carManager = carManager;
    }

    /** Provide the world reference for block collision checks when driving. */
    public void setWorld(World world) {
        this.world = world;
    }

    // ── Entry ─────────────────────────────────────────────────────────────────

    /**
     * Attempt to enter the nearest car within {@link #ENTER_RANGE}.
     * Does nothing and returns false if no car is within range or the player
     * is already driving.
     *
     * Issue #991: Behaviour depends on whether the target car is parked or moving.
     * <ul>
     *   <li><b>Parked car</b> — no driver present; the player steals it silently
     *       and immediately takes control.</li>
     *   <li><b>Moving car</b> — an NPC driver is inside; interacting causes the
     *       driver to get out (the car stops) and the player takes the wheel.</li>
     * </ul>
     *
     * @param player the player entity
     * @return true if the player successfully entered a car
     */
    public boolean tryEnterCar(Player player) {
        if (inCar) {
            return false; // already driving
        }

        Car nearest = findNearestCar(player.getPosition());
        if (nearest == null) {
            return false;
        }

        if (nearest.isParked()) {
            // Parked car — no driver, steal it directly (Issue #991)
            nearest.setParked(false); // un-park so it can be driven
            nearest.setDrivenByPlayer(true);
            currentCar = nearest;
            inCar = true;
            lastMessage = "You get in the parked car. WASD to drive, E or ESC to exit.";
        } else {
            // Moving car — driver gets out first, then player takes over (Issue #991)
            nearest.setStopped(true); // bring the car to a halt
            nearest.setDrivenByPlayer(true);
            currentCar = nearest;
            inCar = true;
            lastMessage = "The driver gets out. You take the wheel. WASD to drive, E or ESC to exit.";
        }
        return true;
    }

    /**
     * Exit the current car, placing the player beside it.
     *
     * @param player the player entity
     * @return true if the player successfully exited
     */
    public boolean exitCar(Player player) {
        if (!inCar || currentCar == null) {
            return false;
        }

        // Place player to the side of the car so they don't clip into it
        Vector3 carPos = currentCar.getPosition();
        // Step the player out perpendicular to the car's heading
        double rad = Math.toRadians(currentCar.getHeading());
        // Perpendicular: rotate heading 90° to the right
        float perpX = (float)  Math.cos(rad);
        float perpZ = (float) -Math.sin(rad);
        float exitX = carPos.x + perpX * EXIT_OFFSET;
        float exitZ = carPos.z + perpZ * EXIT_OFFSET;
        player.teleport(exitX, carPos.y, exitZ);

        currentCar.setDrivenByPlayer(false);
        currentCar = null;
        inCar = false;
        lastMessage = "You get out of the car.";
        return true;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update driving each frame when the player is in a car.
     * Moves the player to match the car position.
     *
     * @param delta      seconds since last frame
     * @param player     the player entity
     * @param accelerate W key held
     * @param braking    S key held
     * @param turnLeft   A key held
     * @param turnRight  D key held
     */
    public void update(float delta, Player player,
                       boolean accelerate, boolean braking,
                       boolean turnLeft, boolean turnRight) {
        if (!inCar || currentCar == null) {
            return;
        }

        currentCar.updatePlayerDriven(delta, accelerate, braking, turnLeft, turnRight);

        // Issue #804: bounce the player-driven car off solid blocks
        if (world != null && world.checkAABBCollision(currentCar.getAABB())) {
            currentCar.bounceOffBlock();
        }

        // Snap the player to the car's position (seat position — slightly above car base)
        Vector3 carPos = currentCar.getPosition();
        player.teleport(carPos.x, carPos.y, carPos.z);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Find the nearest car to {@code playerPos} within {@link #ENTER_RANGE}.
     * Returns null if no car is close enough or all nearby cars are already
     * player-driven.
     */
    private Car findNearestCar(Vector3 playerPos) {
        Car nearest = null;
        float nearestDist2 = ENTER_RANGE * ENTER_RANGE;

        for (Car car : carManager.getCars()) {
            if (car.isDrivenByPlayer()) continue; // already occupied
            Vector3 carPos = car.getPosition();
            float dx = playerPos.x - carPos.x;
            float dz = playerPos.z - carPos.z;
            float dist2 = dx * dx + dz * dz;
            if (dist2 < nearestDist2) {
                nearestDist2 = dist2;
                nearest = car;
            }
        }
        return nearest;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether the player is currently inside a car. */
    public boolean isInCar() {
        return inCar;
    }

    /** The car the player is currently driving, or null. */
    public Car getCurrentCar() {
        return currentCar;
    }

    /**
     * Consume and return the last feedback message (e.g. "You get in the car.").
     * Returns null if no new message since the last poll.
     */
    public String pollLastMessage() {
        String msg = lastMessage;
        lastMessage = null;
        return msg;
    }
}

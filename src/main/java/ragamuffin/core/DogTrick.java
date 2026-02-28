package ragamuffin.core;

/**
 * Tricks that the companion dog (Staffordshire Bull Terrier) can learn.
 *
 * <p>Each trick requires a minimum Bond Level before it can be taught.
 * The required bond thresholds are defined as constants in {@link DogCompanionSystem}.
 *
 * <p>Issue #946: Status Dog — Staffy Companion, Intimidation &amp; Park Walks.
 */
public enum DogTrick {

    /**
     * SIT — the dog performs a sit animation (NPCState.SITTING, 2 seconds).
     * Requires Bond ≥ 10.
     */
    SIT,

    /**
     * STAY — the dog holds its current position for 10 seconds, ignoring player movement.
     * Useful for leaving the dog as a distraction.
     * Requires Bond ≥ 25.
     */
    STAY,

    /**
     * FETCH — the dog runs to the nearest dropped SmallItem within 8 blocks and returns it.
     * The item is added to the player's inventory on return.
     * Requires Bond ≥ 40.
     */
    FETCH,

    /**
     * GUARD — the dog sits at its current position and growls at any NPC within 3 blocks,
     * causing them to enter FLEEING or AVOIDING state for 30 seconds.
     * Does not deal damage.
     * Requires Bond ≥ 60.
     */
    GUARD
}

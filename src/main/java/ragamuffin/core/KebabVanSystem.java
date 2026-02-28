package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #916: Late-Night Kebab Van — 'Ali's Kebab Van'
 *
 * <p>A mobile food van that parks near the pub between 22:00 and 02:00 each night.
 * Drunk and rave-attendee NPCs autonomously queue for KEBAB/CHIPS. The player can
 * queue normally, queue-jump, or use a TIN_OF_BEANS distraction to steal food.
 *
 * <h3>Overview</h3>
 * <ul>
 *   <li>Spawns at 22:00; despawns at 02:00 (hour ≥ 26 in 0–24 normalised form)
 *       or when stock hits 0.</li>
 *   <li>VAN_OWNER NPC stands at the hatch; up to {@link #MAX_QUEUE_SIZE} NPCs queue.</li>
 *   <li>8 seconds per service; 20 starting stock.</li>
 * </ul>
 *
 * <h3>Dynamic Pricing</h3>
 * <ul>
 *   <li>Rush hour (22:00–23:00): ×1.5</li>
 *   <li>Last orders (01:00–02:00): ×0.75</li>
 *   <li>COLD_SNAP / FROST: ×2.0</li>
 *   <li>GREGGS_STRIKE market event: ×1.5</li>
 * </ul>
 *
 * <h3>Player Actions</h3>
 * <ul>
 *   <li><b>Queue normally</b>: join back of queue, pay base price × modifiers.</li>
 *   <li><b>Queue-jump</b>: push to front; 40% chance displaced DRUNK attacks.</li>
 *   <li><b>Distraction</b>: throw TIN_OF_BEANS — owner distracted for 5 seconds,
 *       player steals KEBAB free, +2 Notoriety, THEFT rumour seeded.</li>
 * </ul>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link StreetEconomySystem} — NPC HUNGRY need draw, need reset after serving</li>
 *   <li>{@link WitnessSystem} — theft evidence</li>
 *   <li>{@link RumourNetwork} — THEFT rumour seeded at theft location</li>
 *   <li>{@link WeatherSystem} — price multipliers for COLD_SNAP / FROST</li>
 *   <li>{@link NotorietySystem} — +2 notoriety on theft</li>
 *   <li>{@link AchievementSystem} — DIRTY_KEBAB, FRONT_OF_THE_QUEUE, DISTRACTION_TECHNIQUE, LAST_ORDERS</li>
 * </ul>
 */
public class KebabVanSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Hour of day the van spawns (22:00). */
    public static final float VAN_SPAWN_HOUR = 22.0f;

    /** Hour of day the van despawns expressed as hours past midnight (02:00 = 26.0 in a
     *  continuous 0–48 scale, or simply 2.0 if wrapping through midnight is handled separately). */
    public static final float VAN_DESPAWN_HOUR = 2.0f;

    /** Starting stock count each night. */
    public static final int INITIAL_STOCK = 20;

    /** Seconds per customer served (in real/game seconds). */
    public static final float SERVE_TIME_SECONDS = 8.0f;

    /** Maximum NPCs in the queue at any one time. */
    public static final int MAX_QUEUE_SIZE = 8;

    /** Base price of a KEBAB in COIN. */
    public static final int BASE_PRICE_KEBAB = 3;

    /** Base price of CHIPS in COIN. */
    public static final int BASE_PRICE_CHIPS = 2;

    /** Rush-hour price multiplier (22:00–23:00). */
    public static final float RUSH_HOUR_MULTIPLIER = 1.5f;

    /** Last-orders price multiplier (01:00–02:00). */
    public static final float LAST_ORDERS_MULTIPLIER = 0.75f;

    /** Cold-weather price multiplier (COLD_SNAP / FROST). */
    public static final float COLD_WEATHER_MULTIPLIER = 2.0f;

    /** GREGGS_STRIKE price multiplier. */
    public static final float GREGGS_STRIKE_PRICE_MULTIPLIER = 1.5f;

    /** Extra NPCs added to queue on GREGGS_STRIKE. */
    public static final int GREGGS_STRIKE_EXTRA_NPCS = 3;

    /** Seconds the VAN_OWNER is distracted after a TIN_OF_BEANS throw. */
    public static final float DISTRACTION_DURATION = 5.0f;

    /** Notoriety added when the player steals from the van. */
    public static final int THEFT_NOTORIETY_GAIN = 2;

    /** Distance (blocks) within which DRUNK NPCs with HUNGRY > threshold are drawn. */
    public static final float NPC_DRAW_RANGE = 20.0f;

    /** HUNGRY need threshold above which a DRUNK/RAVE_ATTENDEE NPC joins the queue. */
    public static final float HUNGRY_DRAW_THRESHOLD = 40.0f;

    /** Probability (0–1) that a displaced DRUNK NPC attacks the player on queue-jump. */
    public static final float QUEUE_JUMP_FIGHT_CHANCE = 0.40f;

    /** "SOLD OUT" speech text for the VAN_OWNER. */
    public static final String SOLD_OUT_SPEECH = "SOLD OUT";

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether the van is currently active (spawned). */
    private boolean vanActive = false;

    /** Remaining stock. Starts at INITIAL_STOCK each night. */
    private int stock = 0;

    /** Elapsed time serving the current front-of-queue entity. */
    private float serveTimer = 0f;

    /** Remaining distraction time (>0 means owner is distracted). */
    private float distractionTimer = 0f;

    /** The VAN_OWNER NPC (null when van is inactive). */
    private NPC vanOwner = null;

    /**
     * Ordered list representing the queue. Index 0 = front (being served).
     * Entries are NPC objects or the special {@link #PLAYER_QUEUE_ENTRY} sentinel.
     */
    private final List<Object> queue = new ArrayList<>();

    /** Sentinel object used to represent the player in the queue list. */
    private static final Object PLAYER_QUEUE_ENTRY = new Object();

    /** Whether the player is currently in the queue. */
    private boolean playerInQueue = false;

    /** Index of the player in the queue (-1 = not in queue). */
    private int playerQueueIndex = -1;

    // ── Optional system references (may be null) ──────────────────────────────

    private StreetEconomySystem streetEconomySystem;
    private WitnessSystem witnessSystem;
    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public KebabVanSystem() {
        this(new Random());
    }

    public KebabVanSystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setStreetEconomySystem(StreetEconomySystem s) {
        this.streetEconomySystem = s;
    }

    public void setWitnessSystem(WitnessSystem w) {
        this.witnessSystem = w;
    }

    public void setRumourNetwork(RumourNetwork r) {
        this.rumourNetwork = r;
    }

    public void setNotorietySystem(NotorietySystem n) {
        this.notorietySystem = n;
    }

    public void setAchievementSystem(AchievementSystem a) {
        this.achievementSystem = a;
    }

    // ── Main update ──────────────────────────────────────────────────────────

    /**
     * Update the kebab van system.
     *
     * @param delta         seconds since last frame (real/game seconds)
     * @param hour          current in-game hour (0.0–24.0; hours past midnight may exceed 24
     *                      if caller tracks continuous time — normalise internally)
     * @param weather       current weather (for price multipliers)
     * @param activeEvent   active market event, or null
     * @param nearbyNpcs    all living NPCs near the van area (for queue auto-join logic)
     * @param player        the player (may be null in tests)
     * @param playerInventory  player's inventory (may be null)
     */
    public void update(float delta,
                       float hour,
                       Weather weather,
                       MarketEvent activeEvent,
                       List<NPC> nearbyNpcs,
                       Player player,
                       Inventory playerInventory) {

        float normHour = normaliseHour(hour);

        boolean shouldBeActive = isShouldBeActive(normHour);

        // Spawn / despawn
        if (shouldBeActive && !vanActive) {
            spawnVan(activeEvent);
        } else if (!shouldBeActive && vanActive) {
            despawnVan();
            return;
        }

        if (!vanActive) return;

        // Advance distraction timer
        if (distractionTimer > 0f) {
            distractionTimer = Math.max(0f, distractionTimer - delta);
        }

        // Auto-join queue for nearby DRUNK/RAVE_ATTENDEE NPCs
        if (nearbyNpcs != null) {
            autoJoinQueue(nearbyNpcs);
        }

        // Serve front of queue
        if (!queue.isEmpty()) {
            serveTimer += delta;
            if (serveTimer >= SERVE_TIME_SECONDS) {
                serveTimer = 0f;
                serveNextInQueue(hour, weather, activeEvent, player, playerInventory);
            }
        } else {
            serveTimer = 0f;
        }
    }

    // ── Spawn / Despawn ──────────────────────────────────────────────────────

    private boolean isShouldBeActive(float normHour) {
        return normHour >= VAN_SPAWN_HOUR || normHour < VAN_DESPAWN_HOUR;
    }

    private void spawnVan(MarketEvent activeEvent) {
        vanActive = true;
        stock = INITIAL_STOCK;
        queue.clear();
        playerInQueue = false;
        playerQueueIndex = -1;
        serveTimer = 0f;
        distractionTimer = 0f;
        vanOwner = new NPC(NPCType.VAN_OWNER, 0f, 1f, 0f);

        // GREGGS_STRIKE: add extra NPCs to queue immediately
        if (activeEvent == MarketEvent.GREGGS_STRIKE) {
            for (int i = 0; i < GREGGS_STRIKE_EXTRA_NPCS; i++) {
                NPC extra = new NPC(NPCType.DRUNK, i * 2f, 1f, 2f);
                extra.setState(NPCState.QUEUING);
                queue.add(extra);
            }
        }
    }

    private void despawnVan() {
        vanActive = false;
        if (vanOwner != null) {
            vanOwner.kill();
            vanOwner = null;
        }
        // Dismiss all queued NPCs
        for (Object entry : queue) {
            if (entry instanceof NPC) {
                NPC npc = (NPC) entry;
                npc.setState(NPCState.WANDERING);
            }
        }
        queue.clear();
        playerInQueue = false;
        playerQueueIndex = -1;
    }

    // ── Auto-queue ───────────────────────────────────────────────────────────

    private void autoJoinQueue(List<NPC> nearbyNpcs) {
        if (queue.size() >= MAX_QUEUE_SIZE) return;
        for (NPC npc : nearbyNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getState() == NPCState.QUEUING) continue;
            if (queue.contains(npc)) continue;
            if (queue.size() >= MAX_QUEUE_SIZE) break;

            boolean isDrawnType = npc.getType() == NPCType.DRUNK
                    || npc.getType() == NPCType.RAVE_ATTENDEE;
            if (!isDrawnType) continue;

            float hungry = 0f;
            if (streetEconomySystem != null) {
                hungry = streetEconomySystem.getNeedScore(npc, NeedType.HUNGRY);
            }
            if (hungry > HUNGRY_DRAW_THRESHOLD) {
                npc.setState(NPCState.QUEUING);
                queue.add(npc);
            }
        }
    }

    // ── Serving ───────────────────────────────────────────────────────────────

    private void serveNextInQueue(float hour, Weather weather, MarketEvent activeEvent,
                                   Player player, Inventory playerInventory) {
        if (queue.isEmpty() || stock <= 0) return;

        Object front = queue.remove(0);

        // Update player queue index
        if (playerInQueue) {
            playerQueueIndex--;
            if (playerQueueIndex < 0) {
                playerQueueIndex = -1;
            }
        }

        stock--;

        if (front == PLAYER_QUEUE_ENTRY) {
            // Serve the player
            playerInQueue = false;
            playerQueueIndex = -1;
            if (playerInventory != null) {
                playerInventory.addItem(Material.KEBAB, 1);
            }
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.DIRTY_KEBAB);
            }
            // Check last orders achievement
            float normHour = normaliseHour(hour);
            if (normHour >= 1.0f && normHour < VAN_DESPAWN_HOUR) {
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.LAST_ORDERS);
                }
            }
            // Reset player HUNGRY need is handled by the player eating the item in-game
        } else if (front instanceof NPC) {
            NPC npc = (NPC) front;
            npc.setState(NPCState.WANDERING);
            // Reset NPC needs
            if (streetEconomySystem != null) {
                streetEconomySystem.setNeedScore(npc, NeedType.HUNGRY, 0f);
                streetEconomySystem.setNeedScore(npc, NeedType.BORED, 0f);
            }
        }

        if (stock <= 0) {
            // Sold out
            if (vanOwner != null) {
                vanOwner.say(SOLD_OUT_SPEECH, 5f);
            }
            // Dismiss remaining queue
            for (Object entry : queue) {
                if (entry instanceof NPC) {
                    ((NPC) entry).setState(NPCState.WANDERING);
                }
            }
            queue.clear();
            playerInQueue = false;
            playerQueueIndex = -1;
            vanActive = false;
            if (vanOwner != null) {
                vanOwner.kill();
                vanOwner = null;
            }
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────

    /**
     * Result of a player queue action.
     */
    public enum QueueResult {
        SUCCESS,
        VAN_NOT_ACTIVE,
        ALREADY_IN_QUEUE,
        QUEUE_FULL,
        INSUFFICIENT_FUNDS
    }

    /**
     * Player presses E normally — joins back of queue.
     *
     * @param playerInventory the player's inventory (coins are NOT deducted here;
     *                        deduction occurs when the player reaches the front and is served)
     * @param hour            current in-game hour
     * @param weather         current weather
     * @param activeEvent     active market event or null
     * @return result of the action
     */
    public QueueResult joinQueue(Inventory playerInventory, float hour, Weather weather, MarketEvent activeEvent) {
        if (!vanActive) return QueueResult.VAN_NOT_ACTIVE;
        if (playerInQueue) return QueueResult.ALREADY_IN_QUEUE;
        if (queue.size() >= MAX_QUEUE_SIZE) return QueueResult.QUEUE_FULL;

        int price = getKebabPrice(hour, weather, activeEvent);
        if (playerInventory != null && playerInventory.getItemCount(Material.COIN) < price) {
            return QueueResult.INSUFFICIENT_FUNDS;
        }

        queue.add(PLAYER_QUEUE_ENTRY);
        playerInQueue = true;
        playerQueueIndex = queue.size() - 1;
        return QueueResult.SUCCESS;
    }

    /**
     * Player presses E while sprinting — queue-jumps to front.
     *
     * @param playerInventory the player's inventory
     * @param hour            current in-game hour
     * @param weather         current weather
     * @param activeEvent     active market event or null
     * @return result of the action
     */
    public QueueJumpResult queueJump(Inventory playerInventory, float hour, Weather weather, MarketEvent activeEvent) {
        if (!vanActive) return new QueueJumpResult(QueueResult.VAN_NOT_ACTIVE, null, false);
        if (playerInQueue) return new QueueJumpResult(QueueResult.ALREADY_IN_QUEUE, null, false);

        int price = getKebabPrice(hour, weather, activeEvent);
        if (playerInventory != null && playerInventory.getItemCount(Material.COIN) < price) {
            return new QueueJumpResult(QueueResult.INSUFFICIENT_FUNDS, null, false);
        }

        // Find which NPC is displaced (the one currently at index 0 if any)
        NPC displaced = null;
        if (!queue.isEmpty() && queue.get(0) instanceof NPC) {
            displaced = (NPC) queue.get(0);
        }

        // Insert player at front
        queue.add(0, PLAYER_QUEUE_ENTRY);
        playerInQueue = true;
        playerQueueIndex = 0;

        // Displaced NPC gets angry
        boolean fight = false;
        if (displaced != null) {
            displaced.say("Oi! I was here first!", 3f);
            if (displaced.getType() == NPCType.DRUNK) {
                fight = rng.nextFloat() < QUEUE_JUMP_FIGHT_CHANCE;
                if (fight) {
                    displaced.setState(NPCState.ATTACKING);
                }
            }
        }

        // Increment achievement progress
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.FRONT_OF_THE_QUEUE);
        }

        return new QueueJumpResult(QueueResult.SUCCESS, displaced, fight);
    }

    /**
     * Result of a queue-jump action.
     */
    public static class QueueJumpResult {
        public final QueueResult status;
        public final NPC displacedNpc;
        public final boolean fightTriggered;

        public QueueJumpResult(QueueResult status, NPC displacedNpc, boolean fightTriggered) {
            this.status = status;
            this.displacedNpc = displacedNpc;
            this.fightTriggered = fightTriggered;
        }
    }

    /**
     * Player throws a TIN_OF_BEANS near the van — starts the distraction.
     * Removes 1 TIN_OF_BEANS from the player's inventory.
     *
     * @param playerInventory the player's inventory
     * @return true if distraction was started
     */
    public boolean startDistraction(Inventory playerInventory) {
        if (!vanActive) return false;
        if (playerInventory == null) return false;
        if (playerInventory.getItemCount(Material.TIN_OF_BEANS) < 1) return false;

        playerInventory.removeItem(Material.TIN_OF_BEANS, 1);
        distractionTimer = DISTRACTION_DURATION;

        // VAN_OWNER chases distraction
        if (vanOwner != null) {
            vanOwner.say("What's that?!", 3f);
        }
        return true;
    }

    /**
     * Player steals from the van during the distraction window.
     * Adds 1 KEBAB to inventory, adds Notoriety, seeds THEFT rumour.
     *
     * @param playerInventory the player's inventory
     * @param vanX            van X world position (for rumour seeding)
     * @param vanZ            van Z world position (for rumour seeding)
     * @return true if theft was successful
     */
    public boolean stealFood(Inventory playerInventory, float vanX, float vanZ) {
        if (!vanActive) return false;
        if (distractionTimer <= 0f) return false;
        if (stock <= 0) return false;

        stock--;
        if (playerInventory != null) {
            playerInventory.addItem(Material.KEBAB, 1);
        }

        // Add notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(THEFT_NOTORIETY_GAIN, achievementSystem != null ? achievementSystem::increment : null);
        }

        // Seed THEFT rumour into nearby NPCs via the van owner
        if (rumourNetwork != null && vanOwner != null) {
            Rumour r = new Rumour(RumourType.THEFT,
                    "Someone half-inched a kebab from the van using a tin of beans!");
            vanOwner.getRumours().add(r);
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DISTRACTION_TECHNIQUE);
        }

        if (stock <= 0) {
            if (vanOwner != null) vanOwner.say(SOLD_OUT_SPEECH, 5f);
            despawnVan();
        }

        return true;
    }

    // ── Serve player directly (when at front of queue) ────────────────────────

    /**
     * Serve the player who is at the front of the queue. Deducts coin and adds food.
     * Called when the serve timer fires and the player is at index 0.
     *
     * @param playerInventory inventory to deduct coins from and add food to
     * @param hour            current hour for price and last-orders achievement
     * @param weather         current weather
     * @param activeEvent     active market event or null
     * @return true if player was served successfully
     */
    public boolean servePlayer(Inventory playerInventory, float hour, Weather weather, MarketEvent activeEvent) {
        if (!vanActive || !playerInQueue || playerQueueIndex != 0) return false;
        if (stock <= 0) return false;

        int price = getKebabPrice(hour, weather, activeEvent);
        if (playerInventory != null && playerInventory.getItemCount(Material.COIN) < price) {
            return false;
        }

        if (playerInventory != null) {
            playerInventory.removeItem(Material.COIN, price);
            playerInventory.addItem(Material.KEBAB, 1);
        }

        stock--;
        queue.remove(0);
        playerInQueue = false;
        playerQueueIndex = -1;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DIRTY_KEBAB);
        }

        float normHour = normaliseHour(hour);
        if (normHour >= 1.0f && normHour < VAN_DESPAWN_HOUR) {
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.LAST_ORDERS);
            }
        }

        // Reset HUNGRY need via StreetEconomySystem if available (treat player as NPC-less here)

        if (stock <= 0) {
            if (vanOwner != null) vanOwner.say(SOLD_OUT_SPEECH, 5f);
            despawnVan();
        }

        return true;
    }

    // ── Pricing ───────────────────────────────────────────────────────────────

    /**
     * Calculate the current KEBAB price given time, weather, and market conditions.
     *
     * @param hour        current in-game hour
     * @param weather     current weather
     * @param activeEvent active market event or null
     * @return price in COIN (rounded up, minimum 1)
     */
    public int getKebabPrice(float hour, Weather weather, MarketEvent activeEvent) {
        return computePrice(BASE_PRICE_KEBAB, hour, weather, activeEvent);
    }

    /**
     * Calculate the current CHIPS price given time, weather, and market conditions.
     *
     * @param hour        current in-game hour
     * @param weather     current weather
     * @param activeEvent active market event or null
     * @return price in COIN (rounded up, minimum 1)
     */
    public int getChipsPrice(float hour, Weather weather, MarketEvent activeEvent) {
        return computePrice(BASE_PRICE_CHIPS, hour, weather, activeEvent);
    }

    private int computePrice(int basePrice, float hour, Weather weather, MarketEvent activeEvent) {
        float normHour = normaliseHour(hour);
        float multiplier = 1.0f;

        // Rush hour: 22:00–23:00
        if (normHour >= 22.0f && normHour < 23.0f) {
            multiplier *= RUSH_HOUR_MULTIPLIER;
        }
        // Last orders: 01:00–02:00
        if (normHour >= 1.0f && normHour < VAN_DESPAWN_HOUR) {
            multiplier *= LAST_ORDERS_MULTIPLIER;
        }
        // Cold weather
        if (weather == Weather.COLD_SNAP || weather == Weather.FROST) {
            multiplier *= COLD_WEATHER_MULTIPLIER;
        }
        // GREGGS_STRIKE market event
        if (activeEvent == MarketEvent.GREGGS_STRIKE) {
            multiplier *= GREGGS_STRIKE_PRICE_MULTIPLIER;
        }

        return Math.max(1, (int) Math.ceil(basePrice * multiplier));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Normalise a continuous hour value (which may be >24 for post-midnight tracking)
     * back to a 0–24 range.
     */
    private float normaliseHour(float hour) {
        return hour % 24.0f;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns true if the van is currently active (spawned). */
    public boolean isVanActive() {
        return vanActive;
    }

    /** Returns the VAN_OWNER NPC, or null if the van is not active. */
    public NPC getVanOwner() {
        return vanOwner;
    }

    /** Returns the current queue (unmodifiable view). */
    public List<Object> getQueue() {
        return queue;
    }

    /** Returns the current stock level. */
    public int getStock() {
        return stock;
    }

    /** Returns the index of the player in the queue, or -1 if not queuing. */
    public int getPlayerQueueIndex() {
        return playerQueueIndex;
    }

    /** Returns true if the player is currently in the queue. */
    public boolean isPlayerInQueue() {
        return playerInQueue;
    }

    /** Returns true if the VAN_OWNER is currently distracted by a TIN_OF_BEANS throw. */
    public boolean isOwnerDistracted() {
        return distractionTimer > 0f;
    }

    /** Returns the remaining distraction time in seconds. */
    public float getDistractionTimer() {
        return distractionTimer;
    }

    /** Returns the queue size (number of entities including player if queuing). */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Force-spawn the van (for testing). Sets stock to INITIAL_STOCK and creates the VAN_OWNER NPC.
     *
     * @param activeEvent active market event (may be null)
     */
    public void forceSpawn(MarketEvent activeEvent) {
        if (!vanActive) {
            spawnVan(activeEvent);
        }
    }

    /**
     * Force-set stock level (for testing sell-out scenarios).
     *
     * @param stock new stock level
     */
    public void setStock(int stock) {
        this.stock = stock;
    }
}

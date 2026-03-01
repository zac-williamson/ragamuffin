package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #998: Northfield Aldi — Reduced to Clear: Supermarket Shopping,
 * Shoplifting &amp; the Yellow Sticker Rush.
 *
 * <h3>Legitimate Shopping</h3>
 * <ul>
 *   <li>Press E on {@link PropType#SHELF_CAN} → adds {@link Material#TIN_OF_BEANS} to basket (1 COIN)</li>
 *   <li>Press E on {@link PropType#SHELF_BOTTLE} → adds {@link Material#WATER_BOTTLE} to basket (1 COIN)</li>
 *   <li>Press E on {@link PropType#SHELF_BOX} → adds {@link Material#BISCUIT} to basket (1 COIN)</li>
 *   <li>Press E on {@link PropType#CHECKOUT_PROP} to pay; items move to inventory, COIN deducted.</li>
 * </ul>
 *
 * <h3>Yellow-Sticker Hour (19:00–21:00)</h3>
 * <ul>
 *   <li>All prices drop to 0 COIN.</li>
 *   <li>First exploit awards {@link AchievementType#YELLOW_STICKER_LEGEND}.</li>
 * </ul>
 *
 * <h3>Shoplifting</h3>
 * <ul>
 *   <li>Leaving the store boundary with unpaid basket items flags them as stolen.</li>
 *   <li>Dave (SECURITY_GUARD) has a 1-in-4 chance/second of challenging the player
 *       when within 5 blocks and basket is non-empty.</li>
 *   <li>Blind spots: two back-corner SHELF_BOX positions → 0% detection chance.</li>
 *   <li>First successful blind-spot lift awards {@link AchievementType#BLIND_SPOT_ARTIST}.</li>
 * </ul>
 *
 * <h3>Trolley Distraction</h3>
 * <ul>
 *   <li>Punching a SHOPPING_TROLLEY prop diverts Dave for 15 seconds (+5 Notoriety).</li>
 * </ul>
 *
 * <h3>Self-Checkout</h3>
 * <ul>
 *   <li>40% fail → "unexpected item in bagging area"; alerts Bev and Dave.</li>
 *   <li>60% success → silent checkout at full price.</li>
 * </ul>
 *
 * <h3>The Golden Trolley</h3>
 * <ul>
 *   <li>Spawns in car park 03:00–05:00, once per playthrough.</li>
 *   <li>Press E: yields 20 COIN + {@link AchievementType#GOLDEN_TROLLEY}.</li>
 *   <li>The night before, seeds a {@link RumourType#URBAN_LEGEND} to 2 nearby NPCs.</li>
 * </ul>
 */
public class SupermarketSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour the supermarket opens (daily). */
    public static final float OPEN_HOUR = 8.0f;

    /** Hour the supermarket closes (daily). */
    public static final float CLOSE_HOUR = 22.0f;

    /** Sunday opening hour (reduced capacity). */
    public static final float SUNDAY_OPEN_HOUR = 10.0f;

    /** Sunday closing hour (reduced capacity). */
    public static final float SUNDAY_CLOSE_HOUR = 16.0f;

    // ── Yellow-sticker hour ────────────────────────────────────────────────────

    /** Hour yellow-sticker pricing begins. */
    public static final float YELLOW_STICKER_START = 19.0f;

    /** Hour yellow-sticker pricing ends. */
    public static final float YELLOW_STICKER_END = 21.0f;

    // ── Golden trolley window ──────────────────────────────────────────────────

    /** Hour the golden trolley spawns in the car park. */
    public static final float GOLDEN_TROLLEY_SPAWN_HOUR = 3.0f;

    /** Hour the golden trolley despawns. */
    public static final float GOLDEN_TROLLEY_DESPAWN_HOUR = 5.0f;

    /** COIN reward for interacting with the golden trolley. */
    public static final int GOLDEN_TROLLEY_REWARD = 20;

    // ── Security / shoplifting ─────────────────────────────────────────────────

    /** Blocks within which Dave challenges a player carrying an unpaid basket. */
    public static final float DAVE_CHALLENGE_RADIUS = 5.0f;

    /** Blocks within which Dave scans for the player. */
    public static final float DAVE_SCAN_RADIUS = 8.0f;

    /** Probability per second that Dave challenges the player (1-in-4). */
    public static final float DAVE_CHALLENGE_CHANCE_PER_SECOND = 0.25f;

    /** Duration (seconds) Dave investigates a knocked-over trolley. */
    public static final float DAVE_INVESTIGATE_DURATION = 15.0f;

    /** Duration (seconds) Dave pursues the player after "leg it". */
    public static final float DAVE_PURSUIT_DURATION = 20.0f;

    /** Speed (blocks/s) at which Dave chases the player. */
    public static final float DAVE_PURSUIT_SPEED = 6.0f;

    /** Maximum radius from the store entrance Dave will chase. */
    public static final float DAVE_CHASE_RADIUS = 30.0f;

    /** Notoriety added when the player knocks over a trolley. */
    public static final int TROLLEY_KNOCK_NOTORIETY = 5;

    /** Notoriety added when the player legs it from Dave. */
    public static final int LEG_IT_NOTORIETY = 15;

    /** Notoriety added when the player assaults Dave. */
    public static final int ASSAULT_DAVE_NOTORIETY = 25;

    /** Wanted stars added when the player legs it. */
    public static final int LEG_IT_WANTED_STARS = 1;

    /** Wanted stars added when the player assaults Dave. */
    public static final int ASSAULT_DAVE_WANTED_STARS = 2;

    /** Number of nearby NPCs that receive the ASSAULT rumour. */
    public static final int ASSAULT_RUMOUR_NPC_COUNT = 3;

    // ── Self-checkout ──────────────────────────────────────────────────────────

    /** Probability of self-checkout failure (0.4 = 40%). */
    public static final float SELF_CHECKOUT_FAIL_CHANCE = 0.40f;

    // ── Item prices ────────────────────────────────────────────────────────────

    /** Base price for SHELF_CAN items (TIN_OF_BEANS). */
    public static final int PRICE_SHELF_CAN = 1;

    /** Base price for SHELF_BOTTLE items (WATER_BOTTLE). */
    public static final int PRICE_SHELF_BOTTLE = 1;

    /** Base price for SHELF_BOX items (BISCUIT). */
    public static final int PRICE_SHELF_BOX = 1;

    // ── Blind-spot positions (local store coordinates) ─────────────────────────

    /**
     * X coordinate of the first blind-spot SHELF_BOX (back-left corner).
     * Relative to the store's north-west corner.
     */
    public static final float BLIND_SPOT_1_X = 1.0f;

    /** Z coordinate of the first blind-spot SHELF_BOX. */
    public static final float BLIND_SPOT_1_Z = 1.0f;

    /** X coordinate of the second blind-spot SHELF_BOX (back-right corner). */
    public static final float BLIND_SPOT_2_X = 2.0f;

    /** Z coordinate of the second blind-spot SHELF_BOX. */
    public static final float BLIND_SPOT_2_Z = 1.0f;

    /** Radius within which a position is considered "in the blind spot". */
    public static final float BLIND_SPOT_RADIUS = 1.5f;

    // ── Speech lines ───────────────────────────────────────────────────────────

    public static final String BEV_GREETING         = "That's your lot, love.";
    public static final String BEV_WANTS_BAG        = "Do you want a bag?";
    public static final String BEV_ALERTED          = "Oi — did that go through?";
    public static final String BEV_CALLING_DAVE     = "I'm calling Dave.";
    public static final String BEV_NO_COIN          = "You haven't got enough, love.";
    public static final String BEV_CLOSED           = "We're closed, bab.";

    public static final String DAVE_CHALLENGE       = "Oi mate, did you pay for that?";
    public static final String DAVE_PATROL          = "Move along. Nothing to see here.";
    public static final String DAVE_INVESTIGATING   = "Alright, alright, I'm coming...";

    public static final String SELF_CHECKOUT_FAIL   = "Unexpected item in bagging area.";
    public static final String SELF_CHECKOUT_OK     = "Thank you for shopping at Aldi.";

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** Bev the SHOP_ASSISTANT NPC (null if not spawned). */
    private NPC bev = null;

    /** Dave the SECURITY_GUARD NPC (null if not spawned). */
    private NPC dave = null;

    /** Items currently in the virtual basket (not yet paid for). */
    private final List<Material> basket = new ArrayList<>();

    /** Whether the golden trolley has been spawned this playthrough. */
    private boolean goldenTrolleySpawnedThisPlaythrough = false;

    /** Whether the golden trolley is currently present in the car park. */
    private boolean goldenTrolleyPresent = false;

    /** Whether the golden-trolley rumour has been seeded for the upcoming spawn. */
    private boolean goldenTrolleyRumourSeeded = false;

    /** Whether the player has ever exploited yellow-sticker hour. */
    private boolean yellowStickerLegendAwarded = false;

    /** Whether the player has ever performed a successful blind-spot lift. */
    private boolean blindSpotArtistAwarded = false;

    /** Whether the golden-trolley achievement has been awarded. */
    private boolean goldenTrolleyAchievementAwarded = false;

    /** Seconds remaining while Dave is investigating a knocked-over trolley (0 = not investigating). */
    private float daveInvestigatingTimer = 0f;

    /** Seconds remaining in Dave's pursuit of the player (0 = not pursuing). */
    private float davePursuitTimer = 0f;

    /** Whether Dave is currently in pursuit mode. */
    private boolean daveInPursuit = false;

    /** Accumulated time since last detection check (for per-second chance). */
    private float detectionAccumulator = 0f;

    // ── Optional system references ─────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private WeatherSystem weatherSystem;
    private NeighbourhoodSystem neighbourhoodSystem;

    // ── Construction ────────────────────────────────────────────────────────────

    public SupermarketSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setWeatherSystem(WeatherSystem weatherSystem) {
        this.weatherSystem = weatherSystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    // ── NPC management ──────────────────────────────────────────────────────────

    /** Force-spawn Bev and Dave for testing purposes. */
    public void forceSpawnNpcs() {
        bev  = new NPC(NPCType.SHOP_ASSISTANT, 0f, 0f, 0f);
        bev.setName("Bev");
        dave = new NPC(NPCType.SECURITY_GUARD, 3f, 0f, 3f);
        dave.setName("Dave");
    }

    /** Returns the Bev NPC (may be null if not spawned). */
    public NPC getBev() { return bev; }

    /** Returns the Dave NPC (may be null if not spawned). */
    public NPC getDave() { return dave; }

    // ── Opening hours ───────────────────────────────────────────────────────────

    /**
     * Returns true if the supermarket is open at the given hour on the given day.
     *
     * @param hour      current in-game hour (0–24)
     * @param dayOfWeek 0=Sunday … 6=Saturday
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == 0) {
            return hour >= SUNDAY_OPEN_HOUR && hour < SUNDAY_CLOSE_HOUR;
        }
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    // ── Yellow-sticker hour ─────────────────────────────────────────────────────

    /**
     * Returns true if yellow-sticker hour is currently active (19:00–21:00).
     *
     * @param hour current in-game hour
     */
    public boolean isYellowStickerHour(float hour) {
        return hour >= YELLOW_STICKER_START && hour < YELLOW_STICKER_END;
    }

    /**
     * Returns the price for an item given the current hour.
     * During yellow-sticker hour all prices are 0 COIN.
     *
     * @param basePrise  normal price in COIN
     * @param hour       current in-game hour
     * @return 0 during yellow-sticker hour, otherwise {@code basePrise}
     */
    public int getItemPrice(int basePrise, float hour) {
        return isYellowStickerHour(hour) ? 0 : basePrise;
    }

    // ── Basket management ───────────────────────────────────────────────────────

    /**
     * Player presses E on a shelf prop. Adds the corresponding item to the basket.
     *
     * @param propType prop the player interacted with
     * @return the item added, or null if the prop type is not a shelf item
     */
    public Material pickUpShelfItem(PropType propType) {
        Material item = shelfPropToItem(propType);
        if (item != null) {
            basket.add(item);
        }
        return item;
    }

    /** Returns the basket contents (unmodifiable view). */
    public List<Material> getBasket() {
        return java.util.Collections.unmodifiableList(basket);
    }

    /** Returns the total price of all items in the basket at the given hour. */
    public int getBasketTotal(float hour) {
        int total = 0;
        for (Material item : basket) {
            total += getItemPrice(basePriceFor(item), hour);
        }
        return total;
    }

    /** True if the basket is non-empty. */
    public boolean hasUnpaidItems() {
        return !basket.isEmpty();
    }

    /** Clears the basket without paying. */
    public void clearBasket() {
        basket.clear();
    }

    // ── Checkout ────────────────────────────────────────────────────────────────

    /**
     * Result of pressing E on the checkout or self-checkout.
     */
    public enum CheckoutResult {
        /** Checkout succeeded; items moved to inventory, COIN deducted. */
        SUCCESS,
        /** Player did not have enough COIN. */
        NO_COIN,
        /** Basket was empty — nothing to buy. */
        EMPTY_BASKET,
        /** Store is closed. */
        CLOSED,
        /** Self-checkout failed: "unexpected item in bagging area". */
        SELF_CHECKOUT_FAIL
    }

    /**
     * Player presses E on the CHECKOUT_PROP (staffed by Bev).
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     * @return the checkout result
     */
    public CheckoutResult checkoutAtCounter(Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return CheckoutResult.CLOSED;
        }
        if (basket.isEmpty()) {
            return CheckoutResult.EMPTY_BASKET;
        }

        int total = getBasketTotal(hour);
        if (inventory.getItemCount(Material.COIN) < total) {
            return CheckoutResult.NO_COIN;
        }

        // Deduct coin and move items to inventory
        if (total > 0) {
            inventory.removeItem(Material.COIN, total);
        }
        for (Material item : basket) {
            inventory.addItem(item, 1);
        }

        // Award yellow-sticker achievement on first exploit
        if (isYellowStickerHour(hour) && !yellowStickerLegendAwarded) {
            yellowStickerLegendAwarded = true;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.YELLOW_STICKER_LEGEND);
            }
        }

        basket.clear();
        return CheckoutResult.SUCCESS;
    }

    /**
     * Player presses E on the SELF_CHECKOUT_PROP.
     * 40% chance of failure (alerts Bev and Dave); 60% silent success.
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     * @return the checkout result
     */
    public CheckoutResult selfCheckout(Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return CheckoutResult.CLOSED;
        }
        if (basket.isEmpty()) {
            return CheckoutResult.EMPTY_BASKET;
        }

        if (random.nextFloat() < SELF_CHECKOUT_FAIL_CHANCE) {
            // Fail — alert Bev and Dave
            basket.clear();
            if (dave != null) {
                dave.setState(NPCState.SUSPICIOUS);
            }
            return CheckoutResult.SELF_CHECKOUT_FAIL;
        }

        // Success — pay full price silently
        int total = getBasketTotal(hour);
        if (inventory.getItemCount(Material.COIN) < total) {
            return CheckoutResult.NO_COIN;
        }
        if (total > 0) {
            inventory.removeItem(Material.COIN, total);
        }
        for (Material item : basket) {
            inventory.addItem(item, 1);
        }
        basket.clear();
        return CheckoutResult.SUCCESS;
    }

    // ── Blind-spot detection ────────────────────────────────────────────────────

    /**
     * Returns true if the position (x, z) is within a blind-spot created by the
     * two back-corner SHELF_BOX props. Dave has 0% detection chance here.
     *
     * @param x world X position
     * @param z world Z position
     */
    public boolean isInBlindSpot(float x, float z) {
        float dx1 = x - BLIND_SPOT_1_X;
        float dz1 = z - BLIND_SPOT_1_Z;
        if (dx1 * dx1 + dz1 * dz1 <= BLIND_SPOT_RADIUS * BLIND_SPOT_RADIUS) {
            return true;
        }
        float dx2 = x - BLIND_SPOT_2_X;
        float dz2 = z - BLIND_SPOT_2_Z;
        return dx2 * dx2 + dz2 * dz2 <= BLIND_SPOT_RADIUS * BLIND_SPOT_RADIUS;
    }

    // ── Shoplifting detection ────────────────────────────────────────────────────

    /**
     * Per-frame update for Dave's detection logic.
     * Called every frame from the main {@link #update} method.
     *
     * @param delta       seconds since last frame
     * @param playerX     player X position
     * @param playerY     player Y position
     * @param playerZ     player Z position
     * @param npcs        all active NPCs
     * @return true if Dave issues a challenge this frame
     */
    public boolean updateDetection(float delta, float playerX, float playerY, float playerZ,
                                   List<NPC> npcs) {
        if (basket.isEmpty()) return false;
        if (dave == null) return false;
        if (daveInvestigatingTimer > 0f) return false; // Dave is distracted
        if (daveInPursuit) return false; // Already pursuing

        // Check if player is in a blind spot
        if (isInBlindSpot(playerX, playerZ)) return false;

        // Check distance between Dave and player
        float dist = distanceBetween(dave.getPosition().x, dave.getPosition().z, playerX, playerZ);
        if (dist > DAVE_CHALLENGE_RADIUS) return false;

        // Accumulate time for per-second chance
        detectionAccumulator += delta;
        if (detectionAccumulator >= 1.0f) {
            detectionAccumulator -= 1.0f;
            return random.nextFloat() < DAVE_CHALLENGE_CHANCE_PER_SECOND;
        }
        return false;
    }

    // ── Player responses to Dave's challenge ────────────────────────────────────

    /**
     * Player chooses to comply — pays the basket total at full price.
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     */
    public void complyWithDave(Inventory inventory, float hour) {
        int total = getBasketTotal(hour);
        if (inventory.getItemCount(Material.COIN) >= total && total > 0) {
            inventory.removeItem(Material.COIN, total);
        }
        for (Material item : basket) {
            inventory.addItem(item, 1);
        }
        basket.clear();
    }

    /**
     * Player chooses to leg it from Dave.
     *
     * @param playerX    player X position
     * @param playerY    player Y position
     * @param playerZ    player Z position
     * @param npcs       nearby NPCs (used for rumour seeding)
     */
    public void legItFromDave(float playerX, float playerY, float playerZ, List<NPC> npcs) {
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(LEG_IT_WANTED_STARS, playerX, playerY, playerZ, null);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(LEG_IT_NOTORIETY, null);
        }
        daveInPursuit = true;
        davePursuitTimer = DAVE_PURSUIT_DURATION;
        if (dave != null) {
            dave.setState(NPCState.CHASING_PLAYER);
        }
    }

    /**
     * Player punches Dave (assault).
     *
     * @param playerX    player X position
     * @param playerY    player Y position
     * @param playerZ    player Z position
     * @param npcs       nearby NPCs (for rumour seeding)
     */
    public void assaultDave(float playerX, float playerY, float playerZ, List<NPC> npcs) {
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(ASSAULT_DAVE_WANTED_STARS, playerX, playerY, playerZ, null);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(ASSAULT_DAVE_NOTORIETY, null);
        }
        // Seed ASSAULT rumour to nearby NPCs
        if (rumourNetwork != null && npcs != null) {
            int seeded = 0;
            for (NPC npc : npcs) {
                if (seeded >= ASSAULT_RUMOUR_NPC_COUNT) break;
                if (npc.getType() != NPCType.SECURITY_GUARD && npc.getType() != NPCType.SHOP_ASSISTANT) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.ASSAULT,
                            "Someone just legged it out of Aldi with a full basket — Dave went after 'em."));
                    seeded++;
                }
            }
        }
    }

    // ── Trolley distraction ─────────────────────────────────────────────────────

    /**
     * Player punches a SHOPPING_TROLLEY prop. Dave is diverted for 15 seconds.
     *
     * @param playerX player X position
     * @param playerZ player Z position
     * @param npcs    nearby NPCs (for VANDALISM rumour)
     */
    public void knockOverTrolley(float playerX, float playerZ, List<NPC> npcs) {
        daveInvestigatingTimer = DAVE_INVESTIGATE_DURATION;
        if (dave != null) {
            dave.setState(NPCState.SUSPICIOUS);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TROLLEY_KNOCK_NOTORIETY, null);
        }
        // Seed VANDALISM rumour to 1 nearby NPC
        if (rumourNetwork != null && npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() != NPCType.SECURITY_GUARD) {
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.VANDALISM,
                            "Someone knocked a trolley over down Aldi — Dave had to go and sort it out."));
                    break;
                }
            }
        }
    }

    // ── Blind-spot lift ─────────────────────────────────────────────────────────

    /**
     * Called when the player successfully lifts an item from a blind-spot position
     * without Dave detecting them. Awards BLIND_SPOT_ARTIST on first success.
     */
    public void onBlindSpotLiftSuccess() {
        if (!blindSpotArtistAwarded && achievementSystem != null) {
            blindSpotArtistAwarded = true;
            achievementSystem.unlock(AchievementType.BLIND_SPOT_ARTIST);
        }
    }

    // ── Golden trolley ──────────────────────────────────────────────────────────

    /** Returns true if the golden trolley is currently present in the car park. */
    public boolean isGoldenTrolleyPresent() {
        return goldenTrolleyPresent;
    }

    /**
     * Player presses E on the SHOPPING_TROLLEY_GOLD prop.
     *
     * @param inventory player's inventory
     */
    public void interactWithGoldenTrolley(Inventory inventory) {
        if (!goldenTrolleyPresent) return;
        inventory.addItem(Material.COIN, GOLDEN_TROLLEY_REWARD);
        goldenTrolleyPresent = false;
        if (!goldenTrolleyAchievementAwarded && achievementSystem != null) {
            goldenTrolleyAchievementAwarded = true;
            achievementSystem.unlock(AchievementType.GOLDEN_TROLLEY);
        }
    }

    // ── Main update loop ────────────────────────────────────────────────────────

    /**
     * Per-frame update. Manages golden trolley spawn/despawn, Dave's patrol and
     * investigation timers, and NeighbourhoodSystem vibe contribution.
     *
     * @param delta      seconds since last frame
     * @param timeSystem current game time
     * @param npcs       all active NPCs (for rumour seeding)
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> npcs) {
        float hour      = timeSystem.getTime();
        int   dayOfWeek = timeSystem.getDayCount() % 7;

        // Neighbourhood vibe: +1 Vibe/min while open (local economic anchor)
        // Handled externally via NeighbourhoodSystem.recalculateVibes; we just
        // expose isOpen() so the game loop can query us.

        // ── Golden trolley rumour (seed evening before the window) ─────────────
        // Seed once, the night before (20:00–22:00 the day prior)
        if (!goldenTrolleySpawnedThisPlaythrough && !goldenTrolleyRumourSeeded
                && hour >= 20.0f && hour < 22.0f && npcs != null && !npcs.isEmpty()) {
            goldenTrolleyRumourSeeded = true;
            if (rumourNetwork != null) {
                int seeded = 0;
                for (NPC npc : npcs) {
                    if (seeded >= 2) break;
                    rumourNetwork.addRumour(npc, new Rumour(RumourType.URBAN_LEGEND,
                            "Someone reckons there's a gold trolley down Aldi car park come closing."));
                    seeded++;
                }
            }
        }

        // ── Golden trolley spawn ───────────────────────────────────────────────
        if (!goldenTrolleySpawnedThisPlaythrough && !goldenTrolleyPresent
                && hour >= GOLDEN_TROLLEY_SPAWN_HOUR && hour < GOLDEN_TROLLEY_DESPAWN_HOUR) {
            goldenTrolleyPresent = true;
            goldenTrolleySpawnedThisPlaythrough = true;
        }

        // ── Golden trolley despawn ─────────────────────────────────────────────
        if (goldenTrolleyPresent && hour >= GOLDEN_TROLLEY_DESPAWN_HOUR) {
            goldenTrolleyPresent = false;
        }

        // ── Dave investigation timer ───────────────────────────────────────────
        if (daveInvestigatingTimer > 0f) {
            daveInvestigatingTimer -= delta;
            if (daveInvestigatingTimer <= 0f) {
                daveInvestigatingTimer = 0f;
                if (dave != null) {
                    dave.setState(NPCState.IDLE);
                }
            }
        }

        // ── Dave pursuit timer ─────────────────────────────────────────────────
        if (daveInPursuit && davePursuitTimer > 0f) {
            davePursuitTimer -= delta;
            if (davePursuitTimer <= 0f) {
                davePursuitTimer = 0f;
                daveInPursuit = false;
                if (dave != null) {
                    dave.setState(NPCState.IDLE);
                }
            }
        }
    }

    // ── Getters for test access ─────────────────────────────────────────────────

    /** True if Dave is currently investigating a knocked-over trolley. */
    public boolean isDaveInvestigating() {
        return daveInvestigatingTimer > 0f;
    }

    /** Remaining investigation time in seconds. */
    public float getDaveInvestigatingTimer() {
        return daveInvestigatingTimer;
    }

    /** True if Dave is currently in pursuit of the player. */
    public boolean isDaveInPursuit() {
        return daveInPursuit;
    }

    /** True if the golden trolley has been spawned this playthrough. */
    public boolean isGoldenTrolleySpawnedThisPlaythrough() {
        return goldenTrolleySpawnedThisPlaythrough;
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    /** Maps a shelf prop type to the corresponding inventory item. */
    private Material shelfPropToItem(PropType propType) {
        switch (propType) {
            case SHELF_CAN:    return Material.TIN_OF_BEANS;
            case SHELF_BOTTLE: return Material.WATER_BOTTLE;
            case SHELF_BOX:    return Material.BISCUIT;
            default:           return null;
        }
    }

    /** Returns the base price for a given item. */
    private int basePriceFor(Material item) {
        if (item == Material.TIN_OF_BEANS)  return PRICE_SHELF_CAN;
        if (item == Material.WATER_BOTTLE)  return PRICE_SHELF_BOTTLE;
        if (item == Material.BISCUIT)       return PRICE_SHELF_BOX;
        return 1;
    }

    /** Finds the nearest NPC of a given type within the NPC list. */
    private NPC findNearestNPC(List<NPC> npcs, NPCType type) {
        NPC nearest = null;
        float minDist = Float.MAX_VALUE;
        for (NPC npc : npcs) {
            if (npc.getType() == type) {
                float px = npc.getPosition().x;
                float pz = npc.getPosition().z;
                float d = px * px + pz * pz;
                if (d < minDist) {
                    minDist = d;
                    nearest = npc;
                }
            }
        }
        return nearest;
    }

    /** Euclidean 2-D distance helper. */
    private float distanceBetween(float x1, float z1, float x2, float z2) {
        float dx = x1 - x2;
        float dz = z1 - z2;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}

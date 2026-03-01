package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1069: Add Northfield Ice Cream Van — Mr. Whippy, the Jingle Economy
 * &amp; the Marchetti Turf War.
 *
 * <p>Dave drives his battered 'Dave's Ices' Whippy van around Northfield's
 * residential streets and park on warm days, selling 99 Flakes, lollies and
 * choc ices — and through a side-hatch, fencing stolen goods at a markdown.
 *
 * <h3>Operating Hours &amp; Weather</h3>
 * <ul>
 *   <li>The van only operates in good weather (SUNNY or OVERCAST), 12:00–19:30.</li>
 *   <li>Rain, frost, cold snap, thunderstorm → van stays in depot.</li>
 * </ul>
 *
 * <h3>Queue Mechanics</h3>
 * <ul>
 *   <li>NPCs with BORED or HUNGRY needs autonomously queue (up to {@link #MAX_QUEUE_SIZE}).</li>
 *   <li>JINGLE_AMBUSH: van refuses service when player is Wanted (wanted stars ≥ 1).</li>
 * </ul>
 *
 * <h3>Side-Hatch Fence</h3>
 * <ul>
 *   <li>At Street Rep ≥ 40, Dave opens the side-hatch fence at 55% of base value,
 *       no Notoriety penalty.</li>
 * </ul>
 *
 * <h3>Key Events</h3>
 * <ul>
 *   <li><b>JINGLE_AMBUSH</b>: van refuses service when player is Wanted.</li>
 *   <li><b>MARCHETTI_DRIVE_BY</b>: Marchetti enforcers on bicycles harass at low respect;
 *       firebomb van at Marchetti Respect &lt; 20.</li>
 *   <li><b>QUEUE_JUMP</b>: Chav NPC pushes in — player can confront or pay.</li>
 *   <li><b>KIDS_MOB</b>: school dismissal swarms the queue.</li>
 *   <li><b>STOLEN_VAN</b>: player with Driving ≥ 3 can steal van overnight from
 *       industrial estate depot.</li>
 * </ul>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link WeatherSystem} — van only operates in SUNNY/OVERCAST weather.</li>
 *   <li>{@link TimeSystem} — operating window 12:00–19:30.</li>
 *   <li>{@link StreetEconomySystem} — NPC HUNGRY/BORED need draw, reset after serving.</li>
 *   <li>{@link FactionSystem} — Marchetti drive-by at low respect; firebomb &lt; 20.</li>
 *   <li>{@link WantedSystem} — JINGLE_AMBUSH when player is Wanted.</li>
 *   <li>{@link NotorietySystem} — van theft notoriety.</li>
 *   <li>{@link AchievementSystem} — 7 achievements.</li>
 *   <li>{@link RumourNetwork} — rumours seeded on key events.</li>
 * </ul>
 */
public class IceCreamVanSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Hour of day the van starts operating (12:00). */
    public static final float VAN_START_HOUR = 12.0f;

    /** Hour of day the van stops operating (19:30). */
    public static final float VAN_END_HOUR = 19.5f;

    /** Starting stock each day. */
    public static final int INITIAL_STOCK = 30;

    /** Seconds per customer served. */
    public static final float SERVE_TIME_SECONDS = 6.0f;

    /** Maximum NPCs in the queue at any one time. */
    public static final int MAX_QUEUE_SIZE = 10;

    /** Base price of a NINETY_NINE_FLAKE in COIN. */
    public static final int BASE_PRICE_99_FLAKE = 2;

    /** Base price of a LOLLY in COIN. */
    public static final int BASE_PRICE_LOLLY = 1;

    /** Base price of a CHOC_ICE in COIN. */
    public static final int BASE_PRICE_CHOC_ICE = 3;

    /** Base price of a WAFER_TUBS in COIN. */
    public static final int BASE_PRICE_WAFER_TUBS = 2;

    /** Base price of a BANANA_SPLIT in COIN. */
    public static final int BASE_PRICE_BANANA_SPLIT = 2;

    /** Base price of a FLAKE_99_WITH_SAUCE in COIN. */
    public static final int BASE_PRICE_FLAKE_99_WITH_SAUCE = 3;

    /** Street Rep threshold above which the side-hatch fence opens. */
    public static final int SIDE_HATCH_STREET_REP_THRESHOLD = 40;

    /** Side-hatch fence multiplier (55% of base value). */
    public static final float SIDE_HATCH_FENCE_MULTIPLIER = 0.55f;

    /** Marchetti Respect threshold below which a drive-by is triggered. */
    public static final int MARCHETTI_DRIVEBY_RESPECT_THRESHOLD = 50;

    /** Marchetti Respect threshold below which the van is firebombed. */
    public static final int MARCHETTI_FIREBOMB_RESPECT_THRESHOLD = 20;

    /** HUNGRY need threshold above which an NPC joins the queue. */
    public static final float HUNGRY_DRAW_THRESHOLD = 35.0f;

    /** BORED need threshold above which an NPC joins the queue. */
    public static final float BORED_DRAW_THRESHOLD = 40.0f;

    /** Probability (0–1) of Marchetti drive-by occurring per van activation. */
    public static final float DRIVEBY_PROBABILITY = 0.25f;

    /** Probability (0–1) of a KIDS_MOB event occurring when van is active. */
    public static final float KIDS_MOB_PROBABILITY = 0.15f;

    /** Number of SCHOOL_KID NPCs added in a KIDS_MOB event. */
    public static final int KIDS_MOB_COUNT = 6;

    /** Notoriety gained on van theft. */
    public static final int VAN_THEFT_NOTORIETY = 5;

    /** Minimum driving skill level required to steal the van. */
    public static final int VAN_THEFT_DRIVING_SKILL_REQUIRED = 3;

    /** Depot (industrial estate) hours during which van can be stolen (overnight). */
    public static final float VAN_DEPOT_START_HOUR = 20.0f; // After 20:00
    public static final float VAN_DEPOT_END_HOUR = 11.0f;   // Before 11:00

    /** Speech: van refuses service to Wanted player. */
    public static final String JINGLE_AMBUSH_SPEECH = "Oi — I know your face. On your bike, mate.";

    /** Speech: Dave's standard greeting. */
    public static final String DAVE_GREETING_SPEECH = "What's your fancy?";

    /** Speech: sold out. */
    public static final String SOLD_OUT_SPEECH = "Sorry love, sold out.";

    /** Speech: side hatch opens. */
    public static final String SIDE_HATCH_SPEECH = "Alright, come round the side. Keep it quiet.";

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Whether the van is currently active (on the road). */
    private boolean vanActive = false;

    /** Whether the van has been stolen by the player. */
    private boolean vanStolen = false;

    /** Whether the van has been firebombed by Marchetti. */
    private boolean vanFirebombed = false;

    /** Remaining stock. */
    private int stock = 0;

    /** Elapsed time serving the current front-of-queue entity. */
    private float serveTimer = 0f;

    /** The ICE_CREAM_MAN NPC (Dave). Null when van is inactive. */
    private NPC dave = null;

    /**
     * Ordered list representing the queue. Index 0 = front (being served).
     * Entries are NPC objects or {@link #PLAYER_QUEUE_ENTRY}.
     */
    private final List<Object> queue = new ArrayList<>();

    /** Sentinel object used to represent the player in the queue. */
    private static final Object PLAYER_QUEUE_ENTRY = new Object();

    /** Whether the player is currently in the queue. */
    private boolean playerInQueue = false;

    /** Index of the player in the queue (-1 = not in queue). */
    private int playerQueueIndex = -1;

    /** Whether the side-hatch fence is currently open. */
    private boolean sideHatchOpen = false;

    /** Whether the current event is a KIDS_MOB. */
    private boolean kidsMobActive = false;

    /** Whether a Marchetti drive-by has occurred this session. */
    private boolean driveby = false;

    // ── Optional system references ────────────────────────────────────────────

    private StreetEconomySystem streetEconomySystem;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;
    private WantedSystem wantedSystem;
    private FactionSystem factionSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public IceCreamVanSystem() {
        this(new Random());
    }

    public IceCreamVanSystem(Random rng) {
        this.rng = rng;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setStreetEconomySystem(StreetEconomySystem s) {
        this.streetEconomySystem = s;
    }

    public void setNotorietySystem(NotorietySystem n) {
        this.notorietySystem = n;
    }

    public void setAchievementSystem(AchievementSystem a) {
        this.achievementSystem = a;
    }

    public void setRumourNetwork(RumourNetwork r) {
        this.rumourNetwork = r;
    }

    public void setWantedSystem(WantedSystem w) {
        this.wantedSystem = w;
    }

    public void setFactionSystem(FactionSystem f) {
        this.factionSystem = f;
    }

    // ── Main update ───────────────────────────────────────────────────────────

    /**
     * Update the ice cream van system.
     *
     * @param delta        seconds since last frame
     * @param hour         current in-game hour (0.0–24.0)
     * @param weather      current weather
     * @param nearbyNpcs   all living NPCs near the van (for queue auto-join)
     * @param player       the player (may be null in tests)
     * @param playerInventory player's inventory (may be null)
     * @param streetRep    player's current street reputation points
     */
    public void update(float delta,
                       float hour,
                       Weather weather,
                       List<NPC> nearbyNpcs,
                       Player player,
                       Inventory playerInventory,
                       int streetRep) {

        boolean shouldBeActive = isShouldBeActive(hour, weather);

        // Spawn / despawn
        if (shouldBeActive && !vanActive && !vanFirebombed && !vanStolen) {
            spawnVan(hour, streetRep);
        } else if (!shouldBeActive && vanActive) {
            despawnVan();
            return;
        }

        if (!vanActive) return;

        // Auto-join queue for nearby HUNGRY/BORED NPCs
        if (nearbyNpcs != null) {
            autoJoinQueue(nearbyNpcs);
        }

        // Serve front of queue
        if (!queue.isEmpty()) {
            serveTimer += delta;
            if (serveTimer >= SERVE_TIME_SECONDS) {
                serveTimer = 0f;
                serveNextInQueue(player, playerInventory, streetRep);
            }
        } else {
            serveTimer = 0f;
        }
    }

    // ── Spawn / Despawn ───────────────────────────────────────────────────────

    private boolean isShouldBeActive(float hour, Weather weather) {
        if (weather != Weather.SUNNY && weather != Weather.OVERCAST) {
            return false;
        }
        float normHour = hour % 24.0f;
        return normHour >= VAN_START_HOUR && normHour < VAN_END_HOUR;
    }

    private void spawnVan(float hour, int streetRep) {
        vanActive = true;
        stock = INITIAL_STOCK;
        queue.clear();
        playerInQueue = false;
        playerQueueIndex = -1;
        serveTimer = 0f;
        kidsMobActive = false;
        driveby = false;
        dave = new NPC(NPCType.ICE_CREAM_MAN, 0f, 1f, 0f);
        dave.say(DAVE_GREETING_SPEECH, 5f);

        // Open side hatch if street rep qualifies
        sideHatchOpen = streetRep >= SIDE_HATCH_STREET_REP_THRESHOLD;
        if (sideHatchOpen && dave != null) {
            dave.say(SIDE_HATCH_SPEECH, 5f);
        }

        // Check for Marchetti drive-by event
        if (factionSystem != null) {
            int marchettiRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);
            if (marchettiRespect < MARCHETTI_DRIVEBY_RESPECT_THRESHOLD
                    && rng.nextFloat() < DRIVEBY_PROBABILITY) {
                driveby = true;
                if (marchettiRespect < MARCHETTI_FIREBOMB_RESPECT_THRESHOLD) {
                    // Van is firebombed immediately
                    vanFirebombed = true;
                    vanActive = false;
                    if (dave != null) {
                        dave.kill();
                        dave = null;
                    }
                    queue.clear();
                    return;
                }
                // Harassment: queue is disrupted; NPCs flee
            }
        }

        // KIDS_MOB event: random chance of school swarm
        if (rng.nextFloat() < KIDS_MOB_PROBABILITY) {
            kidsMobActive = true;
            // Add school kids to queue
            for (int i = 0; i < KIDS_MOB_COUNT && queue.size() < MAX_QUEUE_SIZE; i++) {
                NPC kid = new NPC(NPCType.SCHOOL_KID, i * 1.5f, 1f, 2f);
                kid.setState(NPCState.QUEUING);
                queue.add(kid);
            }
        }
    }

    private void despawnVan() {
        vanActive = false;
        if (dave != null) {
            dave.kill();
            dave = null;
        }
        for (Object entry : queue) {
            if (entry instanceof NPC) {
                ((NPC) entry).setState(NPCState.WANDERING);
            }
        }
        queue.clear();
        playerInQueue = false;
        playerQueueIndex = -1;
        sideHatchOpen = false;
    }

    // ── Auto-queue ────────────────────────────────────────────────────────────

    private void autoJoinQueue(List<NPC> nearbyNpcs) {
        if (queue.size() >= MAX_QUEUE_SIZE) return;
        for (NPC npc : nearbyNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getState() == NPCState.QUEUING) continue;
            if (queue.contains(npc)) continue;
            if (queue.size() >= MAX_QUEUE_SIZE) break;

            // Drawn types: BORED or HUNGRY NPCs
            boolean drawnByHunger = false;
            boolean drawnByBoredom = false;
            if (streetEconomySystem != null) {
                drawnByHunger = streetEconomySystem.getNeedScore(npc, NeedType.HUNGRY) > HUNGRY_DRAW_THRESHOLD;
                drawnByBoredom = streetEconomySystem.getNeedScore(npc, NeedType.BORED) > BORED_DRAW_THRESHOLD;
            }

            if (drawnByHunger || drawnByBoredom) {
                npc.setState(NPCState.QUEUING);
                queue.add(npc);
            }
        }
    }

    // ── Serving ───────────────────────────────────────────────────────────────

    private void serveNextInQueue(Player player, Inventory playerInventory, int streetRep) {
        if (queue.isEmpty() || stock <= 0) return;

        Object front = queue.remove(0);

        if (playerInQueue) {
            playerQueueIndex--;
            if (playerQueueIndex < 0) playerQueueIndex = -1;
        }

        stock--;

        if (front == PLAYER_QUEUE_ENTRY) {
            // Serve the player
            playerInQueue = false;
            playerQueueIndex = -1;
            if (playerInventory != null) {
                playerInventory.addItem(Material.NINETY_NINE_FLAKE, 1);
            }
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CHEEKY_FLAKE);
            }
        } else if (front instanceof NPC) {
            NPC npc = (NPC) front;
            npc.setState(NPCState.WANDERING);
            if (streetEconomySystem != null) {
                streetEconomySystem.setNeedScore(npc, NeedType.HUNGRY, 0f);
                streetEconomySystem.setNeedScore(npc, NeedType.BORED, 0f);
            }
        }

        if (stock <= 0) {
            if (dave != null) dave.say(SOLD_OUT_SPEECH, 5f);
            for (Object entry : queue) {
                if (entry instanceof NPC) ((NPC) entry).setState(NPCState.WANDERING);
            }
            queue.clear();
            playerInQueue = false;
            playerQueueIndex = -1;
            vanActive = false;
            if (dave != null) {
                dave.kill();
                dave = null;
            }
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────

    /**
     * Result of a player buy/queue action.
     */
    public enum BuyResult {
        SUCCESS,
        VAN_NOT_ACTIVE,
        ALREADY_IN_QUEUE,
        QUEUE_FULL,
        INSUFFICIENT_FUNDS,
        REFUSED_WANTED,
        SOLD_OUT
    }

    /**
     * Player joins the queue to buy a 99 Flake (or other item).
     *
     * <p>JINGLE_AMBUSH: refuses if player is Wanted (wanted stars ≥ 1).
     *
     * @param playerInventory player's inventory
     * @param itemPrice       price of the item being bought
     * @return result of the action
     */
    public BuyResult joinQueue(Inventory playerInventory, int itemPrice) {
        if (!vanActive) return BuyResult.VAN_NOT_ACTIVE;
        if (stock <= 0) return BuyResult.SOLD_OUT;
        if (playerInQueue) return BuyResult.ALREADY_IN_QUEUE;
        if (queue.size() >= MAX_QUEUE_SIZE) return BuyResult.QUEUE_FULL;

        // JINGLE_AMBUSH: refuse Wanted player
        if (wantedSystem != null && wantedSystem.getWantedStars() >= 1) {
            if (dave != null) dave.say(JINGLE_AMBUSH_SPEECH, 4f);
            return BuyResult.REFUSED_WANTED;
        }

        if (playerInventory != null && playerInventory.getItemCount(Material.COIN) < itemPrice) {
            return BuyResult.INSUFFICIENT_FUNDS;
        }

        queue.add(PLAYER_QUEUE_ENTRY);
        playerInQueue = true;
        playerQueueIndex = queue.size() - 1;
        return BuyResult.SUCCESS;
    }

    /**
     * Player buys a specific item directly from Dave (short circuit for tests/interactions).
     * Deducts COIN from inventory, adds item. Refuses if Wanted.
     *
     * @param playerInventory player's inventory
     * @param item            the item to buy
     * @param price           coin cost
     * @return result of the action
     */
    public BuyResult buyItem(Inventory playerInventory, Material item, int price) {
        if (!vanActive) return BuyResult.VAN_NOT_ACTIVE;
        if (stock <= 0) return BuyResult.SOLD_OUT;

        // JINGLE_AMBUSH
        if (wantedSystem != null && wantedSystem.getWantedStars() >= 1) {
            if (dave != null) dave.say(JINGLE_AMBUSH_SPEECH, 4f);
            return BuyResult.REFUSED_WANTED;
        }

        if (playerInventory != null && playerInventory.getItemCount(Material.COIN) < price) {
            return BuyResult.INSUFFICIENT_FUNDS;
        }

        if (playerInventory != null) {
            playerInventory.removeItem(Material.COIN, price);
            playerInventory.addItem(item, 1);
        }

        stock--;

        // Track achievements
        if (achievementSystem != null) {
            if (item == Material.NINETY_NINE_FLAKE || item == Material.FLAKE_99_WITH_SAUCE) {
                achievementSystem.unlock(AchievementType.CHEEKY_FLAKE);
            }
        }

        if (stock <= 0) {
            if (dave != null) dave.say(SOLD_OUT_SPEECH, 5f);
            despawnVan();
        }

        return BuyResult.SUCCESS;
    }

    /**
     * Result of a queue-jump (QUEUE_JUMP event) interaction.
     */
    public enum QueueJumpResult {
        SUCCESS_PAY,          // Player paid the Chav to let them in
        SUCCESS_CONFRONT,     // Player confronted the Chav, who backed down
        FIGHT_TRIGGERED,      // Confrontation resulted in a fight
        VAN_NOT_ACTIVE,
        NO_CHAV_PRESENT
    }

    /**
     * Handle the QUEUE_JUMP event: a Chav pushes in front.
     * Player can confront (chance of fight) or pay (2 COIN).
     *
     * @param playerInventory player's inventory
     * @param confront        true = confront the Chav; false = pay to resolve
     * @param chavNpc         the Chav NPC who pushed in (may be null)
     * @return result of the interaction
     */
    public QueueJumpResult handleQueueJump(Inventory playerInventory, boolean confront, NPC chavNpc) {
        if (!vanActive) return QueueJumpResult.VAN_NOT_ACTIVE;
        if (chavNpc == null) return QueueJumpResult.NO_CHAV_PRESENT;

        if (confront) {
            // 50% chance the Chav backs down; 50% fight
            if (rng.nextFloat() < 0.5f) {
                chavNpc.setState(NPCState.WANDERING);
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.CHAV_CHARMED);
                }
                return QueueJumpResult.SUCCESS_CONFRONT;
            } else {
                chavNpc.setState(NPCState.ATTACKING);
                return QueueJumpResult.FIGHT_TRIGGERED;
            }
        } else {
            // Pay 2 COIN to resolve
            if (playerInventory != null && playerInventory.getItemCount(Material.COIN) >= 2) {
                playerInventory.removeItem(Material.COIN, 2);
                chavNpc.setState(NPCState.WANDERING);
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.CHAV_CHARMED);
                }
                return QueueJumpResult.SUCCESS_PAY;
            } else {
                // Can't pay — Chav attacks
                chavNpc.setState(NPCState.ATTACKING);
                return QueueJumpResult.FIGHT_TRIGGERED;
            }
        }
    }

    /**
     * Result of a Marchetti drive-by defence.
     */
    public enum DriveByResult {
        DEFENDED,         // Player successfully defended the van
        VAN_DAMAGED,      // Van was damaged (drive-by harassed without firebomb)
        VAN_FIREBOMBED,   // Van was firebombed and destroyed
        NOT_ACTIVE,
        NO_DRIVEBY
    }

    /**
     * Player defends Dave's van from a Marchetti drive-by.
     * At Marchetti Respect ≥ 20, van is merely harassed.
     * At Respect &lt; 20, the van is firebombed unless the player intervenes.
     *
     * @param playerDefends   true if the player is actively defending
     * @return result of the event
     */
    public DriveByResult handleMarchettiDriveBy(boolean playerDefends) {
        if (!vanActive) return DriveByResult.NOT_ACTIVE;
        if (!driveby) return DriveByResult.NO_DRIVEBY;

        if (playerDefends) {
            driveby = false;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.MARCHETTI_DEFENDER);
            }
            if (rumourNetwork != null && dave != null) {
                dave.getRumours().add(new Rumour(RumourType.GANG_ACTIVITY,
                        "Some geezer fought off the Marchetti lads at the ice cream van."));
            }
            return DriveByResult.DEFENDED;
        } else {
            driveby = false;
            // Check firebomb threshold
            if (factionSystem != null
                    && factionSystem.getRespect(Faction.MARCHETTI_CREW) < MARCHETTI_FIREBOMB_RESPECT_THRESHOLD) {
                vanFirebombed = true;
                despawnVan();
                return DriveByResult.VAN_FIREBOMBED;
            } else {
                // Harass only: reduce stock as a penalty
                stock = Math.max(0, stock - 5);
                return DriveByResult.VAN_DAMAGED;
            }
        }
    }

    /**
     * Player uses side hatch to fence a stolen item (Street Rep ≥ 40).
     * Returns the coin value given (55% of base value) or 0 if unavailable.
     *
     * @param playerInventory player's inventory
     * @param item            item to fence
     * @param baseValue       base fence value in COIN
     * @return coins received, or 0 if side hatch not open / van not active
     */
    public int fenceItemViaSideHatch(Inventory playerInventory, Material item, int baseValue) {
        if (!vanActive) return 0;
        if (!sideHatchOpen) return 0;
        if (playerInventory == null) return 0;
        if (playerInventory.getItemCount(item) < 1) return 0;

        int payment = Math.max(1, (int) (baseValue * SIDE_HATCH_FENCE_MULTIPLIER));
        playerInventory.removeItem(item, 1);
        if (playerInventory != null) {
            playerInventory.addItem(Material.COIN, payment);
        }

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SIDE_HATCH);
        }

        return payment;
    }

    /**
     * Player attempts to steal the van from the industrial estate depot overnight.
     * Requires Driving ≥ {@link #VAN_THEFT_DRIVING_SKILL_REQUIRED} and the van must
     * be in depot (outside operating hours).
     *
     * @param hour            current in-game hour
     * @param drivingSkillLevel player's driving skill level (0–5)
     * @return true if theft was successful
     */
    public boolean stealVanFromDepot(float hour, int drivingSkillLevel) {
        float normHour = hour % 24.0f;
        boolean inDepotHours = normHour >= VAN_DEPOT_START_HOUR || normHour < VAN_DEPOT_END_HOUR;

        if (!inDepotHours) return false;
        if (vanActive) return false;
        if (vanStolen) return false;
        if (drivingSkillLevel < VAN_THEFT_DRIVING_SKILL_REQUIRED) return false;

        vanStolen = true;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(VAN_THEFT_NOTORIETY,
                    achievementSystem != null ? achievementSystem::increment : null);
        }

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.VAN_HEIST);
        }

        if (rumourNetwork != null) {
            NPC rumourHolder = new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
            rumourHolder.getRumours().add(new Rumour(RumourType.THEFT,
                    "Someone nicked the ice cream van from the depot last night. Playing the jingle and everything."));
        }

        return true;
    }

    /**
     * Handle the KIDS_MOB event award if the player survives it.
     * Call this when the player has successfully purchased despite the mob.
     */
    public void awardKidsMobSurvived() {
        if (achievementSystem != null && kidsMobActive) {
            achievementSystem.unlock(AchievementType.KIDS_MOB_SURVIVED);
        }
    }

    /**
     * Award DAVE_APPROVES achievement (call when Dave serves player without hesitation).
     */
    public void awardDaveApproves() {
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DAVE_APPROVES);
        }
    }

    // ── Pricing ───────────────────────────────────────────────────────────────

    /**
     * Get the price for a given item from the van.
     *
     * @param item    the item to price
     * @param weather current weather (SUNNY may apply markup)
     * @return price in COIN
     */
    public int getItemPrice(Material item, Weather weather) {
        int base;
        switch (item) {
            case NINETY_NINE_FLAKE:    base = BASE_PRICE_99_FLAKE; break;
            case LOLLY:                base = BASE_PRICE_LOLLY; break;
            case CHOC_ICE:             base = BASE_PRICE_CHOC_ICE; break;
            case WAFER_TUBS:           base = BASE_PRICE_WAFER_TUBS; break;
            case BANANA_SPLIT:         base = BASE_PRICE_BANANA_SPLIT; break;
            case FLAKE_99_WITH_SAUCE:  base = BASE_PRICE_FLAKE_99_WITH_SAUCE; break;
            default:                   base = 1; break;
        }
        // SUNNY heatwave uplift: +1 COIN
        if (weather == Weather.SUNNY) {
            base += 1;
        }
        return base;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns true if the van is currently active. */
    public boolean isVanActive() {
        return vanActive;
    }

    /** Returns true if the van has been firebombed. */
    public boolean isVanFirebombed() {
        return vanFirebombed;
    }

    /** Returns true if the van has been stolen. */
    public boolean isVanStolen() {
        return vanStolen;
    }

    /** Returns the ICE_CREAM_MAN NPC (Dave), or null if van is inactive. */
    public NPC getDave() {
        return dave;
    }

    /** Returns the current queue (may contain NPCs and the player sentinel). */
    public List<Object> getQueue() {
        return queue;
    }

    /** Returns current stock level. */
    public int getStock() {
        return stock;
    }

    /** Returns the player's queue index, or -1 if not queuing. */
    public int getPlayerQueueIndex() {
        return playerQueueIndex;
    }

    /** Returns true if the player is currently in the queue. */
    public boolean isPlayerInQueue() {
        return playerInQueue;
    }

    /** Returns true if the side hatch fence is currently open. */
    public boolean isSideHatchOpen() {
        return sideHatchOpen;
    }

    /** Returns true if a KIDS_MOB event is currently active. */
    public boolean isKidsMobActive() {
        return kidsMobActive;
    }

    /** Returns true if a Marchetti drive-by event is pending. */
    public boolean isDriveByPending() {
        return driveby;
    }

    /** Returns the queue size. */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Force-spawn the van for testing.
     *
     * @param streetRep player's street reputation points
     */
    public void forceSpawn(int streetRep) {
        if (!vanActive) {
            vanActive = true;
            stock = INITIAL_STOCK;
            queue.clear();
            playerInQueue = false;
            playerQueueIndex = -1;
            serveTimer = 0f;
            kidsMobActive = false;
            driveby = false;
            dave = new NPC(NPCType.ICE_CREAM_MAN, 0f, 1f, 0f);
            sideHatchOpen = streetRep >= SIDE_HATCH_STREET_REP_THRESHOLD;
        }
    }

    /**
     * Force-set stock level for testing.
     *
     * @param stock new stock level
     */
    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * Reset firebombed and stolen flags (for testing).
     */
    public void resetVanState() {
        vanFirebombed = false;
        vanStolen = false;
    }

    /**
     * Force-set drive-by flag for testing.
     *
     * @param value true to make a drive-by pending
     */
    public void setDriveByForTesting(boolean value) {
        this.driveby = value;
    }

    /**
     * Force-set kids mob active for testing.
     *
     * @param value true to simulate KIDS_MOB active
     */
    public void setKidsMobActiveForTesting(boolean value) {
        this.kidsMobActive = value;
    }
}

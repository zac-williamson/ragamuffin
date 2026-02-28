package ragamuffin.core;

import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Issue #950: Northfield Leisure Centre — Swimming, Bans &amp; Changing Room Gossip.
 *
 * <h3>Core features</h3>
 * <ul>
 *   <li><b>Sharon the Receptionist</b> — charges 3 COIN entry (5 COIN peak 15:00–17:00).
 *       Refuses Notoriety Tier ≥ 3 players: "You're on the system. You know what you did."
 *       COUNCIL_JACKET or BASEBALL_CAP reduces ban recognition by 60% (1-in-10 chance of being
 *       recognised when disguised vs normal 1-in-1 check).</li>
 *   <li><b>Fire exit sneak-in</b> — press E on FIRE_EXIT_PROP. Silent if no NPC within 8 blocks;
 *       caught = +8 Notoriety +1 wanted star. Awards TIGHT_FISTED achievement on first success.</li>
 *   <li><b>Swim session</b> — 3 in-game minutes: +40 Warmth, +15 Health, −5 Notoriety
 *       (capped −10/day).</li>
 *   <li><b>Vending machine</b> — ENERGY_DRINK (2 COIN), CHOCOLATE_BAR (2 COIN),
 *       WATER_BOTTLE (1 COIN).</li>
 *   <li><b>Changing room gossip</b> — 8 leisure-centre-specific lines seeded to RumourNetwork
 *       every 60 real seconds when player is within 5 blocks of CHANGING_ROOM_PROP.</li>
 *   <li><b>Broken sauna</b> — SAUNA_PROP always Out of Order since 2009. First interaction
 *       awards TYPICAL achievement.</li>
 *   <li><b>Disguise recognition reduction</b> — COUNCIL_JACKET or BASEBALL_CAP reduces
 *       banned-list recognition by 60%.</li>
 * </ul>
 *
 * <h3>System integrations</h3>
 * <ul>
 *   <li>TimeSystem — opening hours 07:00–21:00; peak pricing 15:00–17:00</li>
 *   <li>NotorietySystem — ban check (Tier ≥ 3); swim reduces notoriety −5 (cap −10/day)</li>
 *   <li>WarmthSystem — swim adds +40 warmth; leisure centre acts as warm shelter</li>
 *   <li>HealingSystem — swim adds +15 health directly</li>
 *   <li>RumourNetwork — changing room gossip every 60s</li>
 *   <li>StreetEconomySystem — vending items satisfy HUNGRY/THIRSTY needs</li>
 *   <li>NeighbourhoodSystem — +2 Vibes/min when open</li>
 *   <li>WantedSystem — fire exit caught: +1 wanted star</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * TIGHT_FISTED (first sneak-in via fire exit), TYPICAL (broken sauna first interaction).
 */
public class LeisureCentreSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the leisure centre opens. */
    public static final float OPEN_HOUR = 7.0f;

    /** Hour the leisure centre closes. */
    public static final float CLOSE_HOUR = 21.0f;

    /** Start of after-school peak pricing (15:00). */
    public static final float PEAK_START = 15.0f;

    /** End of after-school peak pricing (17:00). */
    public static final float PEAK_END = 17.0f;

    // ── Entry pricing ─────────────────────────────────────────────────────────

    /** Normal entry fee in COIN. */
    public static final int ENTRY_FEE_NORMAL = 3;

    /** Peak entry fee in COIN (after-school 15:00–17:00). */
    public static final int ENTRY_FEE_PEAK = 5;

    /** Notoriety tier at which the receptionist refuses entry. */
    public static final int BAN_TIER_THRESHOLD = 3;

    /** Disguise recognition reduction factor (60% less likely to be recognised). */
    public static final float DISGUISE_RECOGNITION_REDUCTION = 0.60f;

    // ── Fire exit sneak-in ────────────────────────────────────────────────────

    /** Notoriety added when caught sneaking through fire exit. */
    public static final int FIRE_EXIT_CAUGHT_NOTORIETY = 8;

    /** Wanted stars added when caught sneaking through fire exit. */
    public static final int FIRE_EXIT_CAUGHT_WANTED_STARS = 1;

    /** Range within which an NPC detects the player at the fire exit. */
    public static final float FIRE_EXIT_DETECTION_RANGE = 8.0f;

    // ── Swim session ──────────────────────────────────────────────────────────

    /** Duration of a swim session in real seconds (~3 in-game minutes). */
    public static final float SWIM_SESSION_DURATION = 180.0f;

    /** Warmth restored by completing a swim session. */
    public static final float SWIM_WARMTH_BONUS = 40.0f;

    /** Health restored by completing a swim session. */
    public static final float SWIM_HEALTH_BONUS = 15.0f;

    /** Notoriety reduced by completing a swim session. */
    public static final int SWIM_NOTORIETY_REDUCTION = 5;

    /** Maximum notoriety reduction from swimming per in-game day. */
    public static final int SWIM_MAX_DAILY_NOTORIETY_REDUCTION = 10;

    // ── Vending machine ───────────────────────────────────────────────────────

    /** Cost of ENERGY_DRINK from the vending machine. */
    public static final int VENDING_ENERGY_DRINK_COST = 2;

    /** Cost of CHOCOLATE_BAR from the vending machine. */
    public static final int VENDING_CHOCOLATE_BAR_COST = 2;

    /** Cost of WATER_BOTTLE from the vending machine. */
    public static final int VENDING_WATER_BOTTLE_COST = 1;

    // ── Changing room gossip ──────────────────────────────────────────────────

    /** Interval between changing room gossip rumour seeds (real seconds). */
    public static final float CHANGING_ROOM_GOSSIP_INTERVAL = 60.0f;

    /** Range within which the player triggers changing room gossip. */
    public static final float CHANGING_ROOM_TRIGGER_RANGE = 5.0f;

    /** The 8 changing room gossip lines. */
    public static final String[] CHANGING_ROOM_GOSSIP_LINES = {
        "Heard the councillor's been coming in here on his lunch break. Never actually swims though.",
        "Someone left a full Greggs bag in the family changing room. Sausage rolls everywhere.",
        "The deep end's been closed for six months — something to do with the filtration system.",
        "Saw Karen from the estate here. She told me the sauna's been broken since the Olympics.",
        "Lifeguard reckons they found a phone in the pool last Tuesday. Still worked, apparently.",
        "There was a duck in the baby pool last Thursday. Nobody moved it for twenty minutes.",
        "Donna said she saw the deputy mayor doing lengths at half six this morning. In his vest.",
        "Heard the old leisure centre was better — had a wave machine and everything."
    };

    // ── NeighbourhoodSystem integration ──────────────────────────────────────

    /** Vibes gained per real second when the leisure centre is open. */
    public static final float VIBES_PER_SECOND_OPEN = 2.0f / 60.0f; // +2 Vibes/min

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether a swim session is currently in progress. */
    private boolean swimActive = false;

    /** Remaining real seconds in the current swim session. */
    private float swimTimer = 0f;

    /** Total notoriety reduced from swimming today. */
    private int notorietyReducedToday = 0;

    /** The in-game day on which notoriety was last reduced by swimming. */
    private int lastSwimDay = -1;

    /** Timer counting down until the next changing room gossip seed. */
    private float changingRoomGossipTimer = 0f;

    /** Whether the TIGHT_FISTED (fire exit sneak-in) achievement has been awarded. */
    private boolean tightFistedAwarded = false;

    /** Whether the TYPICAL (broken sauna) achievement has been awarded. */
    private boolean typicalAwarded = false;

    /** Index of the next changing room gossip line to seed. */
    private int gossipLineIndex = 0;

    /** Whether the receptionist NPC has been spawned. */
    private boolean receptionistSpawned = false;

    /** The active RECEPTIONIST NPC, or null if not spawned. */
    private NPC receptionist = null;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final Random random;

    // ── Construction ─────────────────────────────────────────────────────────

    public LeisureCentreSystem() {
        this(new Random());
    }

    public LeisureCentreSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the leisure centre system each frame.
     *
     * @param delta              seconds since last frame
     * @param timeSystem         game time (for opening hours, peak check, daily reset)
     * @param player             the player
     * @param npcManager         NPC manager (for spawning RECEPTIONIST)
     * @param notorietySystem    notoriety system (for swim notoriety reduction)
     * @param rumourNetwork      rumour network (for changing room gossip)
     * @param allNpcs            all active NPCs (for fire exit detection, gossip seeding)
     * @param playerNearChangingRoom  true if player is within 5 blocks of a CHANGING_ROOM_PROP
     * @param neighbourhoodSystem neighbourhood system (for Vibes update)
     * @param achievementCallback achievement callback
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Player player,
                       NPCManager npcManager,
                       NotorietySystem notorietySystem,
                       RumourNetwork rumourNetwork,
                       List<NPC> allNpcs,
                       boolean playerNearChangingRoom,
                       NeighbourhoodSystem neighbourhoodSystem,
                       NotorietySystem.AchievementCallback achievementCallback) {

        float hour = (timeSystem != null) ? timeSystem.getTime() : 12.0f;
        int day = (timeSystem != null) ? timeSystem.getDayCount() : 1;
        boolean isOpen = isOpen(hour);

        // Reset daily notoriety reduction
        if (day != lastSwimDay) {
            notorietyReducedToday = 0;
        }

        // Manage RECEPTIONIST NPC
        if (isOpen && !receptionistSpawned) {
            spawnReceptionist(player, npcManager);
        } else if (!isOpen && receptionistSpawned) {
            despawnReceptionist();
        }

        // Advance swim session timer
        if (swimActive) {
            swimTimer -= delta;
            if (swimTimer <= 0f) {
                swimTimer = 0f;
                completeSwimSession(player, notorietySystem, day, achievementCallback);
            }
        }

        // Changing room gossip seeding
        if (playerNearChangingRoom && isOpen) {
            changingRoomGossipTimer -= delta;
            if (changingRoomGossipTimer <= 0f) {
                changingRoomGossipTimer = CHANGING_ROOM_GOSSIP_INTERVAL;
                seedChangingRoomGossip(rumourNetwork, allNpcs);
            }
        } else if (!playerNearChangingRoom) {
            // Reset timer when player leaves changing room area so gossip seeds promptly on return
            if (changingRoomGossipTimer <= 0f) {
                changingRoomGossipTimer = 0f;
            }
        }

        // NeighbourhoodSystem: +2 Vibes/min when open
        if (isOpen && neighbourhoodSystem != null) {
            float vibesDelta = VIBES_PER_SECOND_OPEN * delta * 60f; // scale to per-minute
            // Apply fractional vibes accumulation (NeighbourhoodSystem uses setVibes)
            // We accumulate and apply each full unit
            vibesAccumulator += VIBES_PER_SECOND_OPEN * delta;
            if (vibesAccumulator >= 1.0f) {
                int vibesGain = (int) vibesAccumulator;
                vibesAccumulator -= vibesGain;
                int currentVibes = neighbourhoodSystem.getVibes();
                neighbourhoodSystem.setVibes(Math.min(100, currentVibes + vibesGain));
            }
        }
    }

    /** Accumulated vibes (sub-integer) for NeighbourhoodSystem. */
    private float vibesAccumulator = 0f;

    // ── Entry / reception ─────────────────────────────────────────────────────

    /**
     * Result of attempting to enter the leisure centre via the reception desk.
     */
    public enum EntryResult {
        /** Entry granted; COIN deducted. */
        ENTRY_GRANTED,
        /** Refused — player is on the banned list (Notoriety Tier ≥ 3). */
        REFUSED_BANNED,
        /** Refused — player cannot afford the entry fee. */
        REFUSED_NO_COINS,
        /** Closed — outside opening hours. */
        CLOSED
    }

    /**
     * Called when the player interacts with the RECEPTIONIST NPC.
     * Charges entry fee based on time of day; refuses Notoriety Tier ≥ 3 players.
     * COUNCIL_JACKET or BASEBALL_CAP reduces recognition by 60%.
     *
     * @param hour           current in-game hour
     * @param notorietyTier  player's current notoriety tier (0–5)
     * @param inventory      player inventory (COIN deducted on success)
     * @param hasDisguise    true if player is wearing COUNCIL_JACKET or BASEBALL_CAP
     * @return the entry result
     */
    public EntryResult attemptEntry(float hour, int notorietyTier,
                                     Inventory inventory, boolean hasDisguise) {
        if (!isOpen(hour)) {
            return EntryResult.CLOSED;
        }

        // Ban check: Tier >= 3 is refused (unless disguise fools the receptionist)
        if (notorietyTier >= BAN_TIER_THRESHOLD) {
            if (hasDisguise) {
                // 60% reduction: recognised 40% of the time instead of 100%
                float recognitionChance = 1.0f - DISGUISE_RECOGNITION_REDUCTION; // 0.40
                if (random.nextFloat() < recognitionChance) {
                    if (receptionist != null) {
                        receptionist.setSpeechText("You're on the system. You know what you did.", 4f);
                    }
                    return EntryResult.REFUSED_BANNED;
                }
                // Disguise worked — fall through to fee check
            } else {
                if (receptionist != null) {
                    receptionist.setSpeechText("You're on the system. You know what you did.", 4f);
                }
                return EntryResult.REFUSED_BANNED;
            }
        }

        // Check fee
        int fee = getEntryFee(hour);
        if (inventory == null || inventory.getItemCount(Material.COIN) < fee) {
            if (receptionist != null) {
                receptionist.setSpeechText("That's " + fee + " coin, please.", 3f);
            }
            return EntryResult.REFUSED_NO_COINS;
        }

        inventory.removeItem(Material.COIN, fee);
        if (receptionist != null) {
            receptionist.setSpeechText("There you go, love. Enjoy your swim.", 3f);
        }
        return EntryResult.ENTRY_GRANTED;
    }

    /**
     * Returns the entry fee for the given in-game hour.
     *
     * @param hour current in-game hour
     * @return entry fee in COIN
     */
    public int getEntryFee(float hour) {
        if (hour >= PEAK_START && hour < PEAK_END) {
            return ENTRY_FEE_PEAK;
        }
        return ENTRY_FEE_NORMAL;
    }

    // ── Fire exit sneak-in ────────────────────────────────────────────────────

    /**
     * Result of attempting to sneak in via the fire exit.
     */
    public enum FireExitResult {
        /** Sneak-in succeeded — no NPC within detection range. */
        SUCCESS,
        /** Caught — an NPC was nearby; Notoriety and wanted stars applied by caller. */
        CAUGHT,
        /** Leisure centre is closed — fire exit is locked (silent alarm). */
        CLOSED
    }

    /**
     * Called when the player presses E on the FIRE_EXIT_PROP.
     * Silent if no NPC is within {@link #FIRE_EXIT_DETECTION_RANGE} blocks.
     * Caught = +8 Notoriety +1 wanted star (applied by caller from WantedSystem).
     * Awards TIGHT_FISTED achievement on first successful sneak-in.
     *
     * @param allNpcs             all active NPCs (for detection check)
     * @param playerX             player X world position
     * @param playerZ             player Z world position
     * @param hour                current in-game hour (for open check)
     * @param notorietySystem     notoriety system (for +8 notoriety on catch)
     * @param achievementCallback achievement callback
     * @return fire exit result
     */
    public FireExitResult attemptFireExitSneak(List<NPC> allNpcs,
                                                float playerX, float playerZ,
                                                float hour,
                                                NotorietySystem notorietySystem,
                                                NotorietySystem.AchievementCallback achievementCallback) {
        if (!isOpen(hour)) {
            return FireExitResult.CLOSED;
        }

        boolean detected = isNpcNearby(allNpcs, playerX, playerZ, FIRE_EXIT_DETECTION_RANGE);

        if (detected) {
            // Caught: add notoriety
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(FIRE_EXIT_CAUGHT_NOTORIETY, achievementCallback);
            }
            return FireExitResult.CAUGHT;
        }

        // Success: award achievement on first sneak-in
        if (!tightFistedAwarded && achievementCallback != null) {
            tightFistedAwarded = true;
            achievementCallback.award(AchievementType.TIGHT_FISTED);
        }
        return FireExitResult.SUCCESS;
    }

    // ── Swim session ──────────────────────────────────────────────────────────

    /**
     * Result of starting a swim session.
     */
    public enum SwimResult {
        /** Swim session started. */
        STARTED,
        /** Already swimming. */
        ALREADY_SWIMMING,
        /** Leisure centre is closed. */
        CLOSED,
        /** Player was not admitted (no token). */
        NOT_ADMITTED
    }

    /**
     * Start a swim session. Requires the player to have been admitted (caller's
     * responsibility to check via {@link #attemptEntry}).
     *
     * @param hour current in-game hour
     * @return swim result
     */
    public SwimResult startSwim(float hour) {
        if (!isOpen(hour)) {
            return SwimResult.CLOSED;
        }
        if (swimActive) {
            return SwimResult.ALREADY_SWIMMING;
        }
        swimActive = true;
        swimTimer = SWIM_SESSION_DURATION;
        return SwimResult.STARTED;
    }

    /**
     * Complete the swim session: apply warmth, health, and notoriety bonuses.
     */
    private void completeSwimSession(Player player, NotorietySystem notorietySystem,
                                      int day, NotorietySystem.AchievementCallback achievementCallback) {
        swimActive = false;
        swimTimer = 0f;

        // Apply warmth and health bonuses
        if (player != null) {
            player.restoreWarmth(SWIM_WARMTH_BONUS);
            player.heal(SWIM_HEALTH_BONUS);
        }

        // Reset daily reduction if day changed
        if (day != lastSwimDay) {
            notorietyReducedToday = 0;
            lastSwimDay = day;
        }

        // Apply notoriety reduction (capped at MAX per day)
        if (notorietySystem != null && notorietyReducedToday < SWIM_MAX_DAILY_NOTORIETY_REDUCTION) {
            int reduction = Math.min(SWIM_NOTORIETY_REDUCTION,
                    SWIM_MAX_DAILY_NOTORIETY_REDUCTION - notorietyReducedToday);
            notorietySystem.reduceNotoriety(reduction, achievementCallback);
            notorietyReducedToday += reduction;
            lastSwimDay = day;
        }
    }

    /**
     * Cancel the current swim session (player left the pool area early).
     */
    public void cancelSwim() {
        swimActive = false;
        swimTimer = 0f;
    }

    // ── Vending machine ───────────────────────────────────────────────────────

    /**
     * Items available from the vending machine.
     */
    public enum VendingItem {
        ENERGY_DRINK(Material.ENERGY_DRINK, VENDING_ENERGY_DRINK_COST),
        CHOCOLATE_BAR(Material.CHOCOLATE_BAR, VENDING_CHOCOLATE_BAR_COST),
        WATER_BOTTLE(Material.WATER_BOTTLE, VENDING_WATER_BOTTLE_COST);

        public final Material material;
        public final int cost;

        VendingItem(Material material, int cost) {
            this.material = material;
            this.cost = cost;
        }
    }

    /**
     * Result of buying from the vending machine.
     */
    public enum VendingResult {
        /** Item purchased successfully. */
        SUCCESS,
        /** Not enough coins. */
        INSUFFICIENT_COINS,
        /** Leisure centre is closed. */
        CLOSED
    }

    /**
     * Buy an item from the vending machine.
     *
     * @param hour      current in-game hour
     * @param item      the vending item to buy
     * @param inventory player inventory (COIN deducted; item added)
     * @return vending result
     */
    public VendingResult buyFromVending(float hour, VendingItem item, Inventory inventory) {
        if (!isOpen(hour)) {
            return VendingResult.CLOSED;
        }
        if (inventory == null || inventory.getItemCount(Material.COIN) < item.cost) {
            return VendingResult.INSUFFICIENT_COINS;
        }
        inventory.removeItem(Material.COIN, item.cost);
        inventory.addItem(item.material, 1);
        return VendingResult.SUCCESS;
    }

    // ── Broken sauna ──────────────────────────────────────────────────────────

    /**
     * Called when the player interacts (E) with the SAUNA_PROP.
     * The sauna has been Out of Order since 2009. Awards TYPICAL on first
     * interaction.
     *
     * @param achievementCallback achievement callback
     * @return the flavour text displayed to the player
     */
    public String interactWithSauna(NotorietySystem.AchievementCallback achievementCallback) {
        if (!typicalAwarded && achievementCallback != null) {
            typicalAwarded = true;
            achievementCallback.award(AchievementType.TYPICAL);
        }
        return "OUT OF ORDER. We apologise for any inconvenience. "
                + "Sign dated: 14/08/2009.";
    }

    // ── Changing room gossip ──────────────────────────────────────────────────

    /**
     * Seed the next changing room gossip line into the rumour network.
     * Cycles through all 8 lines.
     *
     * @param rumourNetwork rumour network
     * @param allNpcs       all active NPCs (gossip seeded to a random alive NPC)
     */
    private void seedChangingRoomGossip(RumourNetwork rumourNetwork, List<NPC> allNpcs) {
        if (rumourNetwork == null || allNpcs == null || allNpcs.isEmpty()) return;

        String gossipText = CHANGING_ROOM_GOSSIP_LINES[gossipLineIndex % CHANGING_ROOM_GOSSIP_LINES.length];
        gossipLineIndex++;

        Rumour rumour = new Rumour(RumourType.CHANGING_ROOM_GOSSIP, gossipText);

        // Seed to a random alive NPC (preferably PUBLIC or similar)
        NPC target = null;
        for (NPC npc : allNpcs) {
            if (npc.isAlive() && npc.getType() == NPCType.PUBLIC) {
                target = npc;
                break;
            }
        }
        if (target == null) {
            for (NPC npc : allNpcs) {
                if (npc.isAlive()) {
                    target = npc;
                    break;
                }
            }
        }
        if (target != null) {
            rumourNetwork.addRumour(target, rumour);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the leisure centre is open at the given in-game hour.
     *
     * @param hour in-game hour (0–24)
     * @return true if open
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if the given in-game hour is during the after-school peak.
     *
     * @param hour in-game hour
     * @return true if peak pricing applies
     */
    public boolean isPeakTime(float hour) {
        return hour >= PEAK_START && hour < PEAK_END;
    }

    /**
     * Returns true if any NPC is within {@code range} blocks of (x, z).
     */
    private boolean isNpcNearby(List<NPC> allNpcs, float x, float z, float range) {
        if (allNpcs == null) return false;
        float rangeSq = range * range;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            float dx = npc.getPosition().x - x;
            float dz = npc.getPosition().z - z;
            if (dx * dx + dz * dz <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    // ── RECEPTIONIST NPC management ───────────────────────────────────────────

    private void spawnReceptionist(Player player, NPCManager npcManager) {
        if (receptionistSpawned || npcManager == null || player == null) return;
        receptionistSpawned = true;
        float x = player.getPosition().x + 2f;
        float y = player.getPosition().y;
        float z = player.getPosition().z + 2f;
        receptionist = npcManager.spawnNPC(NPCType.RECEPTIONIST, x, y, z);
        if (receptionist != null) {
            receptionist.setName("Sharon");
            receptionist.setBuildingType(LandmarkType.LEISURE_CENTRE);
            receptionist.setState(NPCState.IDLE);
            receptionist.setSpeechText("Welcome to Northfield Leisure Centre.", 3f);
        }
    }

    private void despawnReceptionist() {
        if (receptionist != null) {
            receptionist.setState(NPCState.FLEEING);
            receptionist = null;
        }
        receptionistSpawned = false;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether a swim session is currently in progress. */
    public boolean isSwimActive() { return swimActive; }

    /** Remaining real seconds in the current swim session (0 if not active). */
    public float getSwimTimer() { return swimTimer; }

    /** Total notoriety reduced from swimming today. */
    public int getNotorietyReducedToday() { return notorietyReducedToday; }

    /** Whether the TIGHT_FISTED (fire exit sneak-in) achievement has been awarded. */
    public boolean isTightFistedAwarded() { return tightFistedAwarded; }

    /** Whether the TYPICAL (broken sauna) achievement has been awarded. */
    public boolean isTypicalAwarded() { return typicalAwarded; }

    /** The active RECEPTIONIST NPC (null if not spawned). */
    public NPC getReceptionist() { return receptionist; }

    /** Whether the receptionist NPC is currently spawned. */
    public boolean isReceptionistSpawned() { return receptionistSpawned; }

    /** The current gossip line index (cycles through 0–7). */
    public int getGossipLineIndex() { return gossipLineIndex; }

    /** The changing room gossip timer (seconds until next seed). */
    public float getChangingRoomGossipTimer() { return changingRoomGossipTimer; }

    // ── Force-set for testing ─────────────────────────────────────────────────

    /** Force-set swim active state (for testing). */
    public void setSwimActiveForTesting(boolean active, float timer) {
        this.swimActive = active;
        this.swimTimer = timer;
    }

    /** Force-set notoriety-reduced-today (for testing). */
    public void setNotorietyReducedTodayForTesting(int value) {
        this.notorietyReducedToday = value;
    }

    /** Force-set last swim day (for testing). */
    public void setLastSwimDayForTesting(int day) {
        this.lastSwimDay = day;
    }

    /** Force-set changing room gossip timer (for testing). */
    public void setChangingRoomGossipTimerForTesting(float seconds) {
        this.changingRoomGossipTimer = seconds;
    }

    /** Force-set tight-fisted awarded (for testing). */
    public void setTightFistedAwardedForTesting(boolean awarded) {
        this.tightFistedAwarded = awarded;
    }

    /** Force-set typical awarded (for testing). */
    public void setTypicalAwardedForTesting(boolean awarded) {
        this.typicalAwarded = awarded;
    }

    /** Force-set gossip line index (for testing). */
    public void setGossipLineIndexForTesting(int index) {
        this.gossipLineIndex = index;
    }

    /** Force-set the receptionist NPC (for testing). */
    public void setReceptionistForTesting(NPC npc) {
        this.receptionist = npc;
        this.receptionistSpawned = npc != null;
    }
}

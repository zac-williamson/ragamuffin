package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.Faction;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1345: Northfield BT Phone Box — The Scrawled Number,
 * Anonymous Tip-Offs &amp; the Marchetti Dead-Drop.
 *
 * <p>Manages both {@link LandmarkType#PHONE_BOX_HIGH_STREET} and
 * {@link LandmarkType#PHONE_BOX_ESTATE}.
 *
 * <h3>High Street Box</h3>
 * <ul>
 *   <li>4-option call menu: Tip-Off, Marchetti Dead-Drop, Prank Call, 999.</li>
 *   <li>Each call costs 1 {@link Material#COIN} or consumes 1 {@link Material#PHONE_CARD}.</li>
 *   <li>Box may be occupied by a PUBLIC/PENSIONER/COMMUTER NPC (20% chance at daytime).</li>
 *   <li>Coin-box raid with {@link Material#PHONE_BOX_KEY}: 2–6 COIN,
 *       {@link CrimeType#PHONE_BOX_VANDALISM}, Notoriety +1, Wanted +1.</li>
 *   <li>Vandalism (8 hits): destroys prop, 1 SCRAP_METAL, adds crime, breaks box 2 days.</li>
 * </ul>
 *
 * <h3>Estate Box</h3>
 * <ul>
 *   <li>Starts broken; repair with 3× SCRAP_METAL (hold E) OR PHONE_BOX_KEY.</li>
 *   <li>Repair awards {@link AchievementType#LAST_PHONE_STANDING}, seeds
 *       {@link RumourType#PHONE_BOX_REPAIR}, +2 Neighbourhood Vibes.</li>
 * </ul>
 */
public class PhoneBoxSystem {

    // ── Box states ────────────────────────────────────────────────────────────

    /** Functional states for a phone box. */
    public enum BoxState {
        /** Box is usable. */
        FUNCTIONAL,
        /** Box has been smashed / vandalized — unusable. */
        BROKEN
    }

    // ── Call types ────────────────────────────────────────────────────────────

    /** The four available call types from the High Street box. */
    public enum CallType {
        TIP_OFF,
        DEAD_DROP,
        PRANK_CALL,
        EMERGENCY_999
    }

    // ── Call results ──────────────────────────────────────────────────────────

    /** Result of a makeCall() attempt. */
    public enum CallResult {
        /** Call completed successfully. */
        SUCCESS,
        /** Box is smashed / not functional. */
        BOX_SMASHED,
        /** Box is occupied by an NPC. */
        BOX_OCCUPIED,
        /** Player has no COIN or PHONE_CARD. */
        NO_PAYMENT,
        /** Dead-drop requires Marchetti Respect ≥ 30. */
        FACTION_LOCKED,
        /** Dead-drop requires SCRAWLED_NUMBER in inventory. */
        MISSING_ITEM
    }

    // ── Prank call targets ────────────────────────────────────────────────────

    /** Targets for a prank call. */
    public enum PrankTarget {
        FIRE_STATION,
        POLICE_STATION,
        PIZZA_DELIVERY
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Minimum hour for daytime NPC occupancy check. */
    public static final float DAYTIME_START_HOUR = 7.0f;

    /** Maximum hour for daytime NPC occupancy check. */
    public static final float DAYTIME_END_HOUR = 22.0f;

    /** Probability that a phone box is occupied when the player approaches (daytime). */
    public static final float OCCUPANCY_CHANCE = 0.20f;

    /** Call cost in COIN. */
    public static final int CALL_COST_COIN = 1;

    /** Minimum coin yield from coin box raid. */
    public static final int COIN_BOX_MIN = 2;

    /** Maximum coin yield from coin box raid. */
    public static final int COIN_BOX_MAX = 6;

    /** Notoriety added for coin box raid or vandalism. */
    public static final int VANDALISM_NOTORIETY = 1;

    /** SCRAP_METAL required to repair the estate box. */
    public static final int REPAIR_SCRAP_METAL_COST = 3;

    /** In-game-minutes delay before police spawn after tip-off. */
    public static final float TIP_OFF_POLICE_DELAY_MINUTES = 5.0f;

    /** In-game-minutes delay before Marchetti runner spawns after dead-drop call. */
    public static final float DEAD_DROP_RUNNER_DELAY_MINUTES = 10.0f;

    /** Marchetti Respect threshold required for dead-drop call. */
    public static final int MARCHETTI_RESPECT_THRESHOLD = 30;

    /** Marchetti Respect awarded after collecting the dead-drop package. */
    public static final int DEAD_DROP_RESPECT_REWARD = 5;

    /** Neighbourhood Vibes added when estate box is repaired. */
    public static final int REPAIR_VIBES_BONUS = 2;

    /** Number of calls before OFF_THE_BOOKS achievement is awarded. */
    public static final int OFF_THE_BOOKS_CALL_THRESHOLD = 10;

    /** In-game days until a broken box is auto-repaired by the council. */
    public static final int COUNCIL_REPAIR_DAYS = 2;

    // ── Marchetti dead-drop loot pool ─────────────────────────────────────────

    /** Items a MARCHETTI_RUNNER may bring as the dead-drop package (roll 0–2). */
    public static final Material[] DEAD_DROP_LOOT_POOL = {
        Material.SCRAP_METAL,
        Material.COIN,
        Material.FAKE_ID
    };

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String MSG_OUT_OF_ORDER   = "Out of order.";
    public static final String MSG_OCCUPIED       = "Someone's on the phone. You'll have to wait.";
    public static final String MSG_NO_COIN        = "You haven't got enough. You need a coin or a phone card.";
    public static final String MSG_WRONG_NUMBER   = "Wrong number, pal.";
    public static final String MSG_NO_SCRAWLED    = "You'd need that number from the box first.";
    public static final String MSG_TIP_OFF_SENT   = "Call made. They'll be there soon.";
    public static final String MSG_DEAD_DROP_SENT = "Package on its way. Wait by the bench.";
    public static final String MSG_SCRAWLED_FOUND = "There's a number scrawled inside the box. You pocket it.";
    public static final String MSG_REPAIRED       = "The estate box hums back to life. Not bad.";
    public static final String MSG_NOT_ENOUGH_SCRAP = "You need 3 scrap metal to fix this.";
    public static final String MSG_BOX_SMASHED    = "The phone box is smashed. Nothing you can do with it.";

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** State of the High Street box. */
    private BoxState highStreetBoxState = BoxState.FUNCTIONAL;

    /** State of the Estate box — starts broken. */
    private BoxState estateBoxState = BoxState.BROKEN;

    /** Day the high street box was broken (for council auto-repair). -1 = not broken. */
    private int highStreetBrokenDay = -1;

    /** Whether the player has already discovered the SCRAWLED_NUMBER in the High Street box. */
    private boolean scrawledNumberDiscovered = false;

    /** Total calls made from either box — progress toward OFF_THE_BOOKS achievement. */
    private int totalCallsMade = 0;

    /** Pending tip-off police dispatch: time remaining in in-game minutes. -1 = none. */
    private float tipOffPendingMinutes = -1f;

    /** Pending dead-drop runner spawn: time remaining in in-game minutes. -1 = none. */
    private float deadDropPendingMinutes = -1f;

    /** Whether the MARCHETTI_RUNNER has been spawned for the current dead-drop. */
    private boolean deadDropRunnerSpawned = false;

    /** Whether the dead-drop package has been collected this session. */
    private boolean deadDropCollected = false;

    /** Whether the DEAD_DROP achievement has been awarded already. */
    private boolean deadDropAchievementAwarded = false;

    // ── Optional system references ────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private NeighbourhoodSystem neighbourhoodSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public PhoneBoxSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    // ── Occupancy check ───────────────────────────────────────────────────────

    /**
     * Returns true if the phone box is currently occupied by a civilian NPC.
     * Occupancy is only possible during daytime hours.
     *
     * @param hour current in-game hour
     */
    public boolean isOccupied(float hour) {
        if (hour < DAYTIME_START_HOUR || hour >= DAYTIME_END_HOUR) {
            return false;
        }
        return random.nextFloat() < OCCUPANCY_CHANCE;
    }

    // ── SCRAWLED_NUMBER discovery ──────────────────────────────────────────────

    /**
     * Called when the player first presses E on the High Street box.
     * If the scrawled number has not yet been discovered, adds it to inventory.
     *
     * @param inventory player inventory
     * @return true if SCRAWLED_NUMBER was newly added
     */
    public boolean discoverScrawledNumber(Inventory inventory) {
        if (scrawledNumberDiscovered) {
            return false;
        }
        scrawledNumberDiscovered = true;
        inventory.addItem(Material.SCRAWLED_NUMBER, 1);
        return true;
    }

    /** Returns whether the SCRAWLED_NUMBER has been discovered yet. */
    public boolean isScrawledNumberDiscovered() {
        return scrawledNumberDiscovered;
    }

    // ── Call mechanic ─────────────────────────────────────────────────────────

    /**
     * Player makes a call from the High Street phone box.
     *
     * @param type          which call to make
     * @param inventory     player inventory (for payment check)
     * @param hour          current in-game hour (for occupancy check)
     * @param callerNpc     nearest NPC to seed rumours from (may be null)
     * @param npcs          all active NPCs (for rumour seeding)
     * @return              result of the call attempt
     */
    public CallResult makeCall(
            CallType type,
            Inventory inventory,
            float hour,
            NPC callerNpc,
            List<NPC> npcs) {

        if (highStreetBoxState == BoxState.BROKEN) {
            return CallResult.BOX_SMASHED;
        }

        if (isOccupied(hour)) {
            return CallResult.BOX_OCCUPIED;
        }

        // Pre-call validation for dead-drop
        if (type == CallType.DEAD_DROP) {
            int marchettiRespect = factionSystem != null
                    ? factionSystem.getRespect(Faction.MARCHETTI_CREW)
                    : 0;
            if (marchettiRespect < MARCHETTI_RESPECT_THRESHOLD) {
                return CallResult.FACTION_LOCKED;
            }
            if (!inventory.hasItem(Material.SCRAWLED_NUMBER)) {
                return CallResult.MISSING_ITEM;
            }
        }

        // Payment: PHONE_CARD takes priority over COIN
        if (inventory.hasItem(Material.PHONE_CARD)) {
            inventory.removeItem(Material.PHONE_CARD, 1);
        } else if (inventory.getItemCount(Material.COIN) >= CALL_COST_COIN) {
            inventory.removeItem(Material.COIN, CALL_COST_COIN);
        } else {
            return CallResult.NO_PAYMENT;
        }

        // Auto-discover scrawled number on first interaction
        discoverScrawledNumber(inventory);

        // Increment call counter
        totalCallsMade++;
        checkOffTheBooksAchievement();

        // Execute the call
        switch (type) {
            case TIP_OFF:
                executeTipOff(callerNpc, npcs);
                break;
            case DEAD_DROP:
                executeDeadDrop(callerNpc, npcs);
                break;
            case PRANK_CALL:
                executePrankCall(callerNpc, npcs);
                break;
            case EMERGENCY_999:
                executeEmergency999(callerNpc, npcs);
                break;
        }

        return CallResult.SUCCESS;
    }

    private void executeTipOff(NPC callerNpc, List<NPC> npcs) {
        // Seed POLICE_TIP_OFF rumour
        if (rumourNetwork != null) {
            NPC seed = callerNpc != null ? callerNpc : findAnyNpc(npcs);
            if (seed != null) {
                rumourNetwork.addRumour(seed,
                        new Rumour(RumourType.POLICE_TIP_OFF,
                                "Someone grassed — police showed up looking for someone."));
            }
        }
        // Schedule police dispatch in 5 in-game minutes
        tipOffPendingMinutes = TIP_OFF_POLICE_DELAY_MINUTES;
    }

    private void executeDeadDrop(NPC callerNpc, List<NPC> npcs) {
        // Seed MARCHETTI_CONTACT rumour
        if (rumourNetwork != null) {
            NPC seed = callerNpc != null ? callerNpc : findAnyNpc(npcs);
            if (seed != null) {
                rumourNetwork.addRumour(seed,
                        new Rumour(RumourType.MARCHETTI_CONTACT,
                                "Someone's been making calls — Marchetti crew were seen nearby."));
            }
        }
        // Schedule runner spawn in 10 in-game minutes
        deadDropPendingMinutes = DEAD_DROP_RUNNER_DELAY_MINUTES;
        deadDropRunnerSpawned = false;
        deadDropCollected = false;
    }

    private void executePrankCall(NPC callerNpc, List<NPC> npcs) {
        // Seeds a LOCAL_EVENT flavour rumour; no achievement
        if (rumourNetwork != null) {
            NPC seed = callerNpc != null ? callerNpc : findAnyNpc(npcs);
            if (seed != null) {
                rumourNetwork.addRumour(seed,
                        new Rumour(RumourType.LOCAL_EVENT,
                                "Someone rang 999 for a pizza. The high street went mad."));
            }
        }
    }

    private void executeEmergency999(NPC callerNpc, List<NPC> npcs) {
        // Legitimate use — no penalty; seeds LOCAL_EVENT if no active emergency
        if (rumourNetwork != null) {
            NPC seed = callerNpc != null ? callerNpc : findAnyNpc(npcs);
            if (seed != null) {
                rumourNetwork.addRumour(seed,
                        new Rumour(RumourType.LOCAL_EVENT,
                                "Someone called 999 from the phone box again."));
            }
        }
    }

    // ── Coin box raid ─────────────────────────────────────────────────────────

    /**
     * Player uses a PHONE_BOX_KEY to raid the coin box.
     *
     * @param inventory player inventory
     * @return number of coins yielded (0 if box is broken or player lacks the key)
     */
    public int raidCoinBox(Inventory inventory) {
        if (highStreetBoxState == BoxState.BROKEN) {
            return 0;
        }
        if (!inventory.hasItem(Material.PHONE_BOX_KEY)) {
            return 0;
        }

        int coins = COIN_BOX_MIN + random.nextInt(COIN_BOX_MAX - COIN_BOX_MIN + 1);
        inventory.addItem(Material.COIN, coins);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.PHONE_BOX_VANDALISM);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(VANDALISM_NOTORIETY, null);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0, 0, 0, null);
        }

        return coins;
    }

    // ── Vandalism (8 hits) ────────────────────────────────────────────────────

    /**
     * Player smashes the High Street phone box (8 hits).
     * Yields 1 SCRAP_METAL, records crime, breaks the box.
     *
     * @param inventory    player inventory to receive SCRAP_METAL
     * @param currentDay   current in-game day (for council repair countdown)
     */
    public void vandaliseBox(Inventory inventory, int currentDay) {
        if (highStreetBoxState == BoxState.BROKEN) {
            return; // Already broken
        }

        highStreetBoxState = BoxState.BROKEN;
        highStreetBrokenDay = currentDay;

        inventory.addItem(Material.SCRAP_METAL, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.PHONE_BOX_VANDALISM);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(VANDALISM_NOTORIETY, null);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0, 0, 0, null);
        }
    }

    // ── Estate box repair ─────────────────────────────────────────────────────

    /**
     * Player attempts to repair the estate phone box.
     * Requires 3× SCRAP_METAL in inventory (or PHONE_BOX_KEY as alternative).
     *
     * @param inventory player inventory
     * @param npcs      nearby NPCs for rumour seeding (may be null)
     * @return true if repair succeeded
     */
    public boolean repairEstateBox(Inventory inventory, List<NPC> npcs) {
        if (estateBoxState == BoxState.FUNCTIONAL) {
            return false; // Already working
        }

        boolean hasKey = inventory.hasItem(Material.PHONE_BOX_KEY);
        boolean hasMaterials = inventory.getItemCount(Material.SCRAP_METAL) >= REPAIR_SCRAP_METAL_COST;

        if (!hasKey && !hasMaterials) {
            return false;
        }

        // Consume materials (key doesn't consume scrap)
        if (!hasKey) {
            inventory.removeItem(Material.SCRAP_METAL, REPAIR_SCRAP_METAL_COST);
        }

        estateBoxState = BoxState.FUNCTIONAL;

        // Award achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.LAST_PHONE_STANDING);
        }

        // Seed rumour
        if (rumourNetwork != null) {
            NPC seed = findAnyNpc(npcs);
            if (seed != null) {
                rumourNetwork.addRumour(seed,
                        new Rumour(RumourType.PHONE_BOX_REPAIR,
                                "That old phone box on the estate — someone actually fixed it."));
            }
        }

        // Neighbourhood Vibes +2
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.setVibes(
                    Math.min(100, neighbourhoodSystem.getVibes() + REPAIR_VIBES_BONUS));
        }

        return true;
    }

    // ── NPC vandalism ─────────────────────────────────────────────────────────

    /**
     * An NPC attempts to vandalise the phone box (e.g., YOUTH_GANG).
     * Only YOUTH_GANG with a random roll < 0.05 will break it.
     *
     * @param npcType NPC type attempting vandalism
     * @return true if the box was broken by this NPC
     */
    public boolean npcVandalise(NPCType npcType) {
        if (npcType != NPCType.YOUTH_GANG) {
            return false;
        }
        if (highStreetBoxState == BoxState.BROKEN) {
            return false; // Already broken
        }
        if (random.nextFloat() < 0.05f) {
            highStreetBoxState = BoxState.BROKEN;
            return true;
        }
        return false;
    }

    // ── Council auto-repair ───────────────────────────────────────────────────

    /**
     * Call this daily to check if the council has repaired a broken box.
     *
     * @param currentDay current in-game day number
     */
    public void tickDailyRepair(int currentDay) {
        if (highStreetBoxState == BoxState.BROKEN && highStreetBrokenDay >= 0) {
            if ((currentDay - highStreetBrokenDay) >= COUNCIL_REPAIR_DAYS) {
                highStreetBoxState = BoxState.FUNCTIONAL;
                highStreetBrokenDay = -1;
            }
        }
    }

    // ── Pending event tick ────────────────────────────────────────────────────

    /**
     * Per-frame update. Ticks pending police tip-off and dead-drop timers.
     * Callers are responsible for actually spawning NPCs based on
     * {@link #isTipOffPoliceReady()} and {@link #isDeadDropRunnerReady()}.
     *
     * @param deltaMinutes elapsed time in in-game minutes this frame
     */
    public void update(float deltaMinutes) {
        if (tipOffPendingMinutes > 0f) {
            tipOffPendingMinutes = Math.max(0f, tipOffPendingMinutes - deltaMinutes);
        }
        if (deadDropPendingMinutes > 0f && !deadDropRunnerSpawned) {
            deadDropPendingMinutes = Math.max(0f, deadDropPendingMinutes - deltaMinutes);
        }
    }

    /**
     * Returns true if the police tip-off delay has elapsed and police should be spawned.
     * Calling this resets the pending flag.
     */
    public boolean isTipOffPoliceReady() {
        if (tipOffPendingMinutes == 0f) {
            tipOffPendingMinutes = -1f;
            return true;
        }
        return false;
    }

    /**
     * Returns true if the dead-drop runner spawn delay has elapsed.
     * Calling this marks the runner as spawned.
     */
    public boolean isDeadDropRunnerReady() {
        if (deadDropPendingMinutes == 0f && !deadDropRunnerSpawned) {
            deadDropRunnerSpawned = true;
            return true;
        }
        return false;
    }

    // ── Dead-drop package collection ──────────────────────────────────────────

    /**
     * Player collects the dead-drop package from the MARCHETTI_RUNNER.
     * Awards {@link AchievementType#DEAD_DROP}, adds +5 Marchetti Respect.
     *
     * @param inventory player inventory to receive loot
     * @return loot item added, or null if already collected
     */
    public Material collectDeadDropPackage(Inventory inventory) {
        if (deadDropCollected) {
            return null;
        }
        deadDropCollected = true;

        // Roll loot: SCRAP_METAL x3, COIN x15, or FAKE_ID
        int roll = random.nextInt(3);
        Material loot;
        int qty;
        switch (roll) {
            case 0:
                loot = Material.SCRAP_METAL;
                qty = 3;
                break;
            case 1:
                loot = Material.COIN;
                qty = 15;
                break;
            default:
                loot = Material.FAKE_ID;
                qty = 1;
                break;
        }
        inventory.addItem(loot, qty);

        // Award DEAD_DROP achievement
        if (!deadDropAchievementAwarded && achievementSystem != null) {
            deadDropAchievementAwarded = true;
            achievementSystem.unlock(AchievementType.DEAD_DROP);
        }

        // Marchetti Respect +5
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, DEAD_DROP_RESPECT_REWARD);
        }

        return loot;
    }

    // ── Achievement helper ────────────────────────────────────────────────────

    private void checkOffTheBooksAchievement() {
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.OFF_THE_BOOKS);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private NPC findAnyNpc(List<NPC> npcs) {
        if (npcs == null || npcs.isEmpty()) return null;
        for (NPC npc : npcs) {
            if (npc != null) return npc;
        }
        return null;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns the current state of the High Street box. */
    public BoxState getHighStreetBoxState() {
        return highStreetBoxState;
    }

    /** Returns the current state of the Estate box. */
    public BoxState getEstateBoxState() {
        return estateBoxState;
    }

    /** Returns total calls made from either box. */
    public int getTotalCallsMade() {
        return totalCallsMade;
    }

    /** Force-set the High Street box state for testing. */
    public void setHighStreetBoxStateForTesting(BoxState state) {
        this.highStreetBoxState = state;
    }

    /** Force-set the Estate box state for testing. */
    public void setEstateBoxStateForTesting(BoxState state) {
        this.estateBoxState = state;
    }

    /** Force-set scrawled number discovered flag for testing. */
    public void setScrawledNumberDiscoveredForTesting(boolean discovered) {
        this.scrawledNumberDiscovered = discovered;
    }

    /** Force-set high street broken day for testing. */
    public void setHighStreetBrokenDayForTesting(int day) {
        this.highStreetBrokenDay = day;
    }

    /** Force-set total calls for testing. */
    public void setTotalCallsMadeForTesting(int count) {
        this.totalCallsMade = count;
    }

    /** Force-set tip-off pending minutes for testing. */
    public void setTipOffPendingMinutesForTesting(float minutes) {
        this.tipOffPendingMinutes = minutes;
    }

    /** Force-set dead-drop pending minutes for testing. */
    public void setDeadDropPendingMinutesForTesting(float minutes) {
        this.deadDropPendingMinutes = minutes;
        this.deadDropRunnerSpawned = false;
    }

    /** Returns whether the dead-drop runner has been spawned. */
    public boolean isDeadDropRunnerSpawned() {
        return deadDropRunnerSpawned;
    }
}

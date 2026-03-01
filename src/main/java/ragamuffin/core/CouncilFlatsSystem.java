package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #1100: Northfield Council Flats — Kendrick House gameplay.
 *
 * <p>Five interlocking mechanics:
 * <ol>
 *   <li><b>Broken Lift</b> — LIFT_PROP is broken 80% of the time. Fix with
 *       SCRAP_METAL + WIRE (5-second hold). Reduces Notoriety −1, seeds LOCAL_EVENT rumour.
 *       Working lift: 5% chance of mid-ride breakdown → 60-second CLAUSTROPHOBIC debuff.</li>
 *   <li><b>Nosy Neighbour Network</b> — Floors 2–4 resident NPCs dispense LOCAL_EVENT
 *       rumours when gossiped with (once per in-game hour). 30% chance seeds
 *       PLAYER_SPOTTED rumour if player committed a crime recently. 16:00–17:00 communal
 *       gathering cross-pollinates rumours. NeighbourhoodWatch anger &gt; 50 causes residents
 *       to call police on wanted players in the stairwell.</li>
 *   <li><b>Housing Inspection Hustle</b> — Derek (COUNCIL_MEMBER) visits every 14 days
 *       10:00–12:00, scanning each floor for contraband. Found with player nearby:
 *       Notoriety +5, POSSESSION crime, Wanted +1. SUIT_JACKET disguise skips player floor.
 *       Clean pass rewards DWP_LETTER (5 COIN) or HOUSING_PRIORITY_LETTER (15 COIN at office).</li>
 *   <li><b>Letterbox Parcel Economy</b> — LETTERBOX_BANK_PROP spawns 1–3 parcels at 08:00.
 *       Steal by hold-E (1–3 seconds). Witnessed: police called (Wanted +1),
 *       PLAYER_SPOTTED rumour, Notoriety +3. Stolen parcels fence at 60% value.</li>
 *   <li><b>Top-Floor Squat Economy</b> — Floor 5 squat sells SCRAP_METAL/WIRE/STOLEN_GOODS
 *       at 50% off. Friday/Saturday 23:00: squat party raises NoiseSystem +30,
 *       NeighbourhoodWatch anger +5, seeds NOISE_COMPLAINT rumour.</li>
 * </ol>
 *
 * <h3>New props</h3>
 * {@code LETTERBOX_BANK_PROP}, {@code FLAT_DOOR_PROP}, {@code COMMUNAL_NOTICEBOARD_PROP}.
 *
 * <h3>New materials</h3>
 * {@code HOUSING_PRIORITY_LETTER}, {@code STOLEN_PARCEL}, {@code STOLEN_GOODS},
 * {@code SUIT_JACKET}.
 *
 * <h3>New achievements</h3>
 * {@code LIFT_ENGINEER}, {@code NOSY_NEIGHBOUR}, {@code INSPECTION_PASSED},
 * {@code PARCEL_PIRATE}.
 */
public class CouncilFlatsSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Probability (0–100) that the lift is broken at any given time. */
    public static final int LIFT_BROKEN_CHANCE = 80;

    /** Duration (real seconds) to hold E to fix the lift. */
    public static final float LIFT_FIX_HOLD_DURATION = 5.0f;

    /** Probability (0–100) that a working lift breaks mid-ride. */
    public static final int LIFT_BREAKDOWN_CHANCE = 5;

    /** Duration (real seconds) of the CLAUSTROPHOBIC movement debuff on breakdown. */
    public static final float CLAUSTROPHOBIC_DEBUFF_DURATION = 60.0f;

    /** Movement speed multiplier applied during the CLAUSTROPHOBIC debuff. */
    public static final float CLAUSTROPHOBIC_SPEED_MULTIPLIER = 0.0f; // trapped = cannot move

    /** Notoriety reduction for fixing the lift. */
    public static final int LIFT_FIX_NOTORIETY_REDUCTION = 1;

    /**
     * In-game hours between resident gossip dispensations (per NPC).
     * Residents will only gossip once per hour.
     */
    public static final float GOSSIP_COOLDOWN_HOURS = 1.0f;

    /**
     * Probability (0–100) that a resident seeds a PLAYER_SPOTTED rumour instead of
     * LOCAL_EVENT when the player has a recent crime on record.
     */
    public static final int PLAYER_SPOTTED_CRIME_CHANCE = 30;

    /** In-game hour that residents begin the communal gathering. */
    public static final float COMMUNAL_GATHER_START = 16.0f;

    /** In-game hour that residents disperse from the communal gathering. */
    public static final float COMMUNAL_GATHER_END = 17.0f;

    /** NeighbourhoodWatch anger threshold above which residents call police. */
    public static final int WATCH_ANGER_POLICE_CALL_THRESHOLD = 50;

    /** In-game day interval between Derek's housing inspections. */
    public static final int INSPECTION_INTERVAL_DAYS = 14;

    /** In-game hour that Derek's inspection begins. */
    public static final float INSPECTION_START_HOUR = 10.0f;

    /** In-game hour that Derek's inspection ends. */
    public static final float INSPECTION_END_HOUR = 12.0f;

    /** Notoriety gain when contraband is found during inspection. */
    public static final int INSPECTION_CONTRABAND_NOTORIETY = 5;

    /** Number of parcels spawned daily at 08:00 (random 1–3). */
    public static final int PARCEL_SPAWN_MIN = 1;
    public static final int PARCEL_SPAWN_MAX = 3;

    /** In-game hour that parcels are spawned each day. */
    public static final float PARCEL_SPAWN_HOUR = 8.0f;

    /** Duration (seconds) of hold-E to steal a parcel (randomised 1–3). */
    public static final float PARCEL_STEAL_MIN_DURATION = 1.0f;
    public static final float PARCEL_STEAL_MAX_DURATION = 3.0f;

    /** Notoriety gain for a witnessed parcel theft. */
    public static final int PARCEL_THEFT_NOTORIETY = 3;

    /** Percentage of a parcel's base value paid by the fence (0–100). */
    public static final int PARCEL_FENCE_VALUE_PERCENT = 60;

    /** Base coin value of a parcel before fence discount (random 3–8). */
    public static final int PARCEL_BASE_VALUE_MIN = 3;
    public static final int PARCEL_BASE_VALUE_MAX = 8;

    /** Price discount at the top-floor squat stall (50% off = 0.5). */
    public static final float SQUAT_DISCOUNT_MULTIPLIER = 0.5f;

    /** In-game hour that the squat party begins. */
    public static final float SQUAT_PARTY_HOUR = 23.0f;

    /** Noise level added to NoiseSystem during a squat party. */
    public static final float SQUAT_PARTY_NOISE = 30.0f;

    /** NeighbourhoodWatch anger added per squat party. */
    public static final int SQUAT_PARTY_WATCH_ANGER = 5;

    /** Number of SCRAP_METAL/WIRE required to fix the lift. */
    public static final int LIFT_FIX_SCRAP_REQUIRED = 1;
    public static final int LIFT_FIX_WIRE_REQUIRED = 1;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the lift is currently broken. */
    private boolean liftBroken;

    /** Progress (0–LIFT_FIX_HOLD_DURATION) towards fixing the lift. */
    private float liftFixProgress;

    /** Whether the player is currently trapped by a mid-ride breakdown. */
    private boolean playerTrapped;

    /** Remaining seconds of the CLAUSTROPHOBIC debuff. */
    private float claustrophobicTimer;

    /** Per-NPC gossip cooldown timers (NPC index → remaining seconds). */
    private final java.util.Map<String, Float> gossipCooldowns = new java.util.HashMap<>();

    /** In-game hour of last gossip (used for cooldown logic). */
    private float lastGossipHour;

    /** Day index of Derek's last inspection. */
    private int lastInspectionDay;

    /** Whether Derek is currently active on his inspection walk. */
    private boolean inspectionActive;

    /** Current floor Derek is inspecting (1–5). */
    private int derekCurrentFloor;

    /** Number of parcels currently available at the letterbox bank. */
    private int parcelsAvailable;

    /** In-game day when parcels were last spawned. */
    private int lastParcelSpawnDay;

    /** Progress (0–duration) towards a parcel steal. */
    private float parcelStealProgress;

    /** Required hold duration for the current parcel steal attempt. */
    private float parcelStealRequiredDuration;

    /** Whether a squat party has fired today. */
    private boolean sqautPartyFiredToday;

    /** Day when squat party last fired. */
    private int lastSquatPartyDay;

    /** Number of parcels stolen (for PARCEL_PIRATE achievement). */
    private int parcelsStolen;

    /** Number of residents gossiped with (for NOSY_NEIGHBOUR achievement). */
    private int residentsGossiped;

    /** Whether LIFT_ENGINEER achievement is already unlocked. */
    private boolean liftEngineerUnlocked;

    /** Whether INSPECTION_PASSED achievement is already unlocked (this run). */
    private boolean inspectionPassedThisRun;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final NotorietySystem notorietySystem;
    private final WantedSystem wantedSystem;
    private final NeighbourhoodWatchSystem neighbourhoodWatchSystem;
    private final NoiseSystem noiseSystem;
    private final RumourNetwork rumourNetwork;
    private final NewspaperSystem newspaperSystem;
    private final AchievementSystem achievementSystem;
    private final CriminalRecord criminalRecord;
    private final DisguiseSystem disguiseSystem;
    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CouncilFlatsSystem(
            NotorietySystem notorietySystem,
            WantedSystem wantedSystem,
            NeighbourhoodWatchSystem neighbourhoodWatchSystem,
            NoiseSystem noiseSystem,
            RumourNetwork rumourNetwork,
            NewspaperSystem newspaperSystem,
            AchievementSystem achievementSystem,
            CriminalRecord criminalRecord,
            DisguiseSystem disguiseSystem,
            Random random) {
        this.notorietySystem          = notorietySystem;
        this.wantedSystem             = wantedSystem;
        this.neighbourhoodWatchSystem = neighbourhoodWatchSystem;
        this.noiseSystem              = noiseSystem;
        this.rumourNetwork            = rumourNetwork;
        this.newspaperSystem          = newspaperSystem;
        this.achievementSystem        = achievementSystem;
        this.criminalRecord           = criminalRecord;
        this.disguiseSystem           = disguiseSystem;
        this.random                   = random;

        // Initialise lift state
        this.liftBroken = random.nextInt(100) < LIFT_BROKEN_CHANCE;
        this.parcelsAvailable = 0;
        this.lastInspectionDay = -INSPECTION_INTERVAL_DAYS; // first inspection fires soon
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update called by the game loop.
     *
     * @param delta        seconds since last frame
     * @param time         current TimeSystem for hour/day queries
     * @param allNpcs      all living NPCs (for rumour seeding and Derek spawning)
     * @param playerX      player world X
     * @param playerZ      player world Z
     * @param playerFloor  which floor (1–5) the player is currently on
     */
    public void update(float delta, TimeSystem time, List<NPC> allNpcs,
                       float playerX, float playerZ, int playerFloor) {
        float currentHour = time.getHour();
        int   currentDay  = time.getDayIndex();
        int   dayOfWeek   = time.getDayOfWeek(); // 0=Mon … 5=Sat, 6=Sun

        // ── Claustrophobic debuff countdown ───────────────────────────────────
        if (playerTrapped && claustrophobicTimer > 0) {
            claustrophobicTimer -= delta;
            if (claustrophobicTimer <= 0) {
                playerTrapped = false;
                claustrophobicTimer = 0;
            }
        }

        // ── Parcel spawning at 08:00 ──────────────────────────────────────────
        if (currentDay != lastParcelSpawnDay && currentHour >= PARCEL_SPAWN_HOUR) {
            lastParcelSpawnDay = currentDay;
            parcelsAvailable = PARCEL_SPAWN_MIN + random.nextInt(PARCEL_SPAWN_MAX - PARCEL_SPAWN_MIN + 1);
            sqautPartyFiredToday = false;
        }

        // ── Housing inspection ────────────────────────────────────────────────
        if (currentDay - lastInspectionDay >= INSPECTION_INTERVAL_DAYS) {
            if (currentHour >= INSPECTION_START_HOUR && currentHour < INSPECTION_END_HOUR) {
                if (!inspectionActive) {
                    startInspection(currentDay, allNpcs);
                }
            }
        }
        if (inspectionActive && currentHour >= INSPECTION_END_HOUR) {
            endInspection(allNpcs);
        }

        // ── Communal gathering rumour cross-pollination 16:00–17:00 ──────────
        if (currentHour >= COMMUNAL_GATHER_START && currentHour < COMMUNAL_GATHER_END) {
            // Seed a LOCAL_EVENT rumour from a resident NPC once per day
            if (currentDay != (int) lastGossipHour) {
                seedCommunalRumour(allNpcs, "The neighbours have gathered downstairs again. Everyone knows everyone's business.");
                lastGossipHour = currentDay;
            }
        }

        // ── Squat party on Friday(5)/Saturday(6) at 23:00 ────────────────────
        if ((dayOfWeek == 5 || dayOfWeek == 6)
                && currentHour >= SQUAT_PARTY_HOUR
                && currentDay != lastSquatPartyDay) {
            lastSquatPartyDay = currentDay;
            triggerSquatParty(allNpcs);
        }

        // ── NeighbourhoodWatch anger → residents call police ──────────────────
        if (neighbourhoodWatchSystem != null
                && neighbourhoodWatchSystem.getWatchAnger() > WATCH_ANGER_POLICE_CALL_THRESHOLD
                && wantedSystem != null
                && wantedSystem.getWantedStars() >= 1) {
            // Handled externally by NPC police-call logic; system just tracks threshold
        }
    }

    // ── Mechanic 1: Broken Lift ───────────────────────────────────────────────

    /**
     * Returns {@code true} if the lift at Kendrick House is currently broken.
     */
    public boolean isLiftBroken() {
        return liftBroken;
    }

    /**
     * Returns {@code true} if the player is currently trapped by a lift breakdown.
     */
    public boolean isPlayerTrapped() {
        return playerTrapped;
    }

    /**
     * Returns the remaining seconds of the CLAUSTROPHOBIC debuff, or 0 if inactive.
     */
    public float getClaustrophobicTimer() {
        return claustrophobicTimer;
    }

    /**
     * Called every frame while the player is holding E at the broken lift with
     * the required materials in their inventory.
     *
     * @param delta     seconds since last frame
     * @param inventory player's inventory (SCRAP_METAL + WIRE consumed on success)
     * @param allNpcs   all living NPCs (for rumour seeding)
     * @return {@code true} if the lift was successfully fixed this frame
     */
    public boolean updateLiftFix(float delta, Inventory inventory, List<NPC> allNpcs) {
        if (!liftBroken) return false;

        if (inventory.getItemCount(Material.SCRAP_METAL) < LIFT_FIX_SCRAP_REQUIRED
                || inventory.getItemCount(Material.WIRE) < LIFT_FIX_WIRE_REQUIRED) {
            liftFixProgress = 0;
            return false;
        }

        liftFixProgress += delta;
        if (liftFixProgress >= LIFT_FIX_HOLD_DURATION) {
            liftFixProgress = 0;
            liftBroken = false;

            inventory.removeItem(Material.SCRAP_METAL, LIFT_FIX_SCRAP_REQUIRED);
            inventory.removeItem(Material.WIRE, LIFT_FIX_WIRE_REQUIRED);

            // Reward
            if (notorietySystem != null) {
                notorietySystem.reduceNotoriety(LIFT_FIX_NOTORIETY_REDUCTION,
                        achievementSystem::unlock);
            }
            seedLocalEventRumour(allNpcs,
                    "Someone fixed the lift at Kendrick House. About bloody time.");

            // Achievement
            if (!liftEngineerUnlocked) {
                liftEngineerUnlocked = true;
                achievementSystem.unlock(AchievementType.LIFT_ENGINEER);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the current lift-fix hold progress (0 to {@link #LIFT_FIX_HOLD_DURATION}).
     */
    public float getLiftFixProgress() {
        return liftFixProgress;
    }

    /**
     * Called when the player rides a working lift. 5% chance the lift breaks mid-ride,
     * trapping the player with a CLAUSTROPHOBIC debuff.
     *
     * @return {@code true} if the lift broke during this ride
     */
    public boolean onRideLift() {
        if (liftBroken) return false;
        if (random.nextInt(100) < LIFT_BREAKDOWN_CHANCE) {
            liftBroken = true;
            playerTrapped = true;
            claustrophobicTimer = CLAUSTROPHOBIC_DEBUFF_DURATION;
            return true;
        }
        return false;
    }

    /**
     * Returns the movement speed multiplier to apply while the player is trapped.
     * Returns 1.0 when not trapped.
     */
    public float getMovementMultiplier() {
        return playerTrapped ? CLAUSTROPHOBIC_SPEED_MULTIPLIER : 1.0f;
    }

    // ── Mechanic 2: Nosy Neighbour Network ───────────────────────────────────

    /**
     * Called when the player gossips with a resident NPC on floors 2–4.
     * Dispenses a LOCAL_EVENT (or PLAYER_SPOTTED) rumour once per in-game hour.
     *
     * @param npc              the resident NPC
     * @param currentHour      current in-game hour
     * @param hasRecentCrime   whether the player committed a crime in the building recently
     * @param allNpcs          all living NPCs
     * @return a dialogue string from the resident, or {@code null} if on cooldown
     */
    public String gossipWithResident(NPC npc, float currentHour, boolean hasRecentCrime,
                                     List<NPC> allNpcs) {
        if (npc == null) return null;
        if (npc.getType() != NPCType.PENSIONER && npc.getType() != NPCType.PUBLIC
                && npc.getType() != NPCType.FLAT_RESIDENT) {
            return null;
        }

        String npcKey = npc.getName() != null ? npc.getName() : npc.getType().name();
        Float cooldown = gossipCooldowns.get(npcKey);
        if (cooldown != null && cooldown > 0) {
            return null; // on cooldown
        }

        // Set hourly cooldown (60 in-game minutes = 1 in-game hour approx)
        gossipCooldowns.put(npcKey, GOSSIP_COOLDOWN_HOURS * 60f);

        // Track for achievement
        residentsGossiped++;
        if (residentsGossiped >= 5) {
            achievementSystem.unlock(AchievementType.NOSY_NEIGHBOUR);
        }

        // Decide rumour type
        if (hasRecentCrime && random.nextInt(100) < PLAYER_SPOTTED_CRIME_CHANCE) {
            seedPlayerSpottedRumour(allNpcs, npc,
                    "I saw someone suspicious in the building earlier. Could've been that troublemaker.");
            return "I've seen you about. I know what goes on round here. Don't think I don't.";
        } else {
            seedLocalEventRumour(allNpcs,
                    "Derek on the third floor hasn't paid his TV licence in three years, apparently.");
            return "You hear about number 14? The council came round again. Always something, isn't it.";
        }
    }

    /**
     * Advance gossip cooldown timers each frame.
     *
     * @param delta seconds since last frame (in real seconds; convert to in-game minutes as needed)
     */
    public void updateGossipCooldowns(float delta) {
        // delta here is in real seconds; approximate 1 real second ≈ some in-game time
        // We use real seconds for simplicity (cooldown in seconds)
        for (String key : new java.util.HashSet<>(gossipCooldowns.keySet())) {
            float remaining = gossipCooldowns.getOrDefault(key, 0f) - delta;
            gossipCooldowns.put(key, Math.max(0, remaining));
        }
    }

    // ── Mechanic 3: Housing Inspection Hustle ────────────────────────────────

    /**
     * Returns {@code true} if Derek's inspection is currently active.
     */
    public boolean isInspectionActive() {
        return inspectionActive;
    }

    /**
     * Returns the floor Derek is currently inspecting (1–5), or 0 if inactive.
     */
    public int getDerekCurrentFloor() {
        return inspectionActive ? derekCurrentFloor : 0;
    }

    /**
     * Called when Derek reaches the player's floor during an inspection.
     * Checks for contraband and applies penalties or rewards.
     *
     * @param playerFloor      which floor the player is on (1–5)
     * @param inventory        player's inventory to check for contraband
     * @param allNpcs          all living NPCs
     * @return a description of the inspection outcome, never null
     */
    public String conductFloorInspection(int playerFloor, Inventory inventory, List<NPC> allNpcs) {
        if (!inspectionActive) return "";
        if (derekCurrentFloor != playerFloor) return "";

        // Check disguise — SUIT_JACKET causes Derek to skip this floor
        if (disguiseSystem != null && disguiseSystem.getActiveDisguise() == Material.SUIT_JACKET) {
            return "Derek glances at you, nods approvingly, and moves on to the next floor.";
        }

        // Check contraband
        if (hasContraband(inventory)) {
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(INSPECTION_CONTRABAND_NOTORIETY,
                        achievementSystem::unlock);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.POSSESSION);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0, 1, 0, achievementSystem::unlock);
            }
            return "Derek spots the contraband. \"Right, I'm going to have to report this.\" "
                    + "Notoriety +5, crime recorded.";
        }

        // Clean pass
        return "";
    }

    /**
     * Called at the end of a clean inspection (Derek finished all floors without finding contraband).
     * Rewards the player with DWP_LETTER or HOUSING_PRIORITY_LETTER.
     *
     * @param inventory player inventory to receive the reward
     * @param allNpcs   all living NPCs (for NewspaperSystem integration)
     * @return reward description
     */
    public String rewardCleanInspection(Inventory inventory, List<NPC> allNpcs) {
        if (!inspectionPassedThisRun) {
            inspectionPassedThisRun = true;
            achievementSystem.unlock(AchievementType.INSPECTION_PASSED);
        }

        // 50% chance of HOUSING_PRIORITY_LETTER, otherwise DWP_LETTER
        if (random.nextBoolean()) {
            inventory.addItem(Material.HOUSING_PRIORITY_LETTER, 1);
            return "Derek hands you a Housing Priority Letter. \"You're a model tenant.\" "
                    + "Fence it at the council office for 15 COIN, or use it to claim a flat.";
        } else {
            inventory.addItem(Material.DWP_LETTER, 1);
            return "Derek leaves a DWP letter. It contains 5 COIN of UC entitlement confirmation.";
        }
    }

    // ── Mechanic 4: Letterbox Parcel Economy ─────────────────────────────────

    /**
     * Returns the number of parcels currently available at the letterbox bank.
     */
    public int getParcelsAvailable() {
        return parcelsAvailable;
    }

    /**
     * Called every frame while the player is holding E at the letterbox bank.
     *
     * @param delta      seconds since last frame
     * @param inventory  player's inventory (STOLEN_PARCEL added on success)
     * @param witnessed  whether a resident NPC is within witness range
     * @param allNpcs    all living NPCs (for rumour seeding)
     * @return {@code true} if a parcel was successfully stolen this frame
     */
    public boolean updateParcelSteal(float delta, Inventory inventory,
                                     boolean witnessed, List<NPC> allNpcs) {
        if (parcelsAvailable <= 0) return false;

        if (parcelStealProgress == 0) {
            // Determine required duration for this steal attempt (1–3 seconds)
            parcelStealRequiredDuration = PARCEL_STEAL_MIN_DURATION
                    + random.nextFloat() * (PARCEL_STEAL_MAX_DURATION - PARCEL_STEAL_MIN_DURATION);
        }

        parcelStealProgress += delta;
        if (parcelStealProgress >= parcelStealRequiredDuration) {
            parcelStealProgress = 0;
            parcelsAvailable--;

            inventory.addItem(Material.STOLEN_PARCEL, 1);
            parcelsStolen++;

            if (witnessed) {
                onWitnessedParcelTheft(allNpcs);
            }

            if (parcelsStolen >= 5) {
                achievementSystem.unlock(AchievementType.PARCEL_PIRATE);
            }

            return true;
        }
        return false;
    }

    /**
     * Cancels a parcel steal in progress (player released E).
     */
    public void cancelParcelSteal() {
        parcelStealProgress = 0;
        parcelStealRequiredDuration = 0;
    }

    /**
     * Returns the current parcel steal hold progress (0 to {@link #parcelStealRequiredDuration}).
     */
    public float getParcelStealProgress() {
        return parcelStealProgress;
    }

    /**
     * Called when a witnessed parcel theft occurs.
     * Applies penalties and seeds rumour.
     */
    private void onWitnessedParcelTheft(List<NPC> allNpcs) {
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(PARCEL_THEFT_NOTORIETY, achievementSystem::unlock);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0, 1, 0, achievementSystem::unlock);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.PARCEL_THEFT);
        }
        seedPlayerSpottedRumour(allNpcs, null,
                "Someone was nicking parcels from the letterbox at Kendrick House. Brazen as you like.");
    }

    /**
     * Calculate the fence value (in COIN) of a STOLEN_PARCEL.
     * Returns 60% of a random base value.
     */
    public int calculateParcelFenceValue() {
        int baseValue = PARCEL_BASE_VALUE_MIN
                + random.nextInt(PARCEL_BASE_VALUE_MAX - PARCEL_BASE_VALUE_MIN + 1);
        return Math.max(1, (int) (baseValue * PARCEL_FENCE_VALUE_PERCENT / 100f));
    }

    // ── Mechanic 5: Top-Floor Squat Economy ──────────────────────────────────

    /**
     * Returns whether a squat party is active this session (Friday/Saturday night).
     */
    public boolean isSquatPartyActive() {
        return sqautPartyFiredToday;
    }

    /**
     * Calculates the discounted price of a squat stall item.
     *
     * @param basePrice  full price in COIN
     * @return discounted price (50% off)
     */
    public int getSquatStallPrice(int basePrice) {
        return Math.max(1, (int) (basePrice * SQUAT_DISCOUNT_MULTIPLIER));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void startInspection(int currentDay, List<NPC> allNpcs) {
        inspectionActive = true;
        derekCurrentFloor = 1;
        inspectionPassedThisRun = false;

        // Publish newspaper notice
        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline("HOUSING INSPECTION TODAY AT KENDRICK HOUSE — DEREK ON THE WARPATH");
        }

        // Seed a rumour
        seedLocalEventRumour(allNpcs,
                "Derek from the council is doing inspections at Kendrick House today. "
                        + "Anyone with anything dodgy best hide it.");
    }

    private void endInspection(List<NPC> allNpcs) {
        if (!inspectionActive) return;
        lastInspectionDay = (int) (lastInspectionDay + INSPECTION_INTERVAL_DAYS); // nudge for next
        // Actually track the real day properly:
        inspectionActive = false;
        derekCurrentFloor = 0;
    }

    private void triggerSquatParty(List<NPC> allNpcs) {
        sqautPartyFiredToday = true;

        if (noiseSystem != null) {
            noiseSystem.addNoise(SQUAT_PARTY_NOISE);
        }
        if (neighbourhoodWatchSystem != null) {
            neighbourhoodWatchSystem.addAnger(SQUAT_PARTY_WATCH_ANGER);
        }
        seedNoiseComplaintRumour(allNpcs,
                "There's a party kicking off at Kendrick House again. The music's shaking the walls.");
    }

    private boolean hasContraband(Inventory inventory) {
        return inventory.getItemCount(Material.STOLEN_GOODS) > 0
                || inventory.getItemCount(Material.COUNTERFEIT_NOTE) > 0
                || inventory.getItemCount(Material.DRUGS_EVIDENCE) > 0
                || inventory.getItemCount(Material.STOLEN_PHONE) > 0
                || inventory.getItemCount(Material.STOLEN_PARCEL) > 0;
    }

    private void seedLocalEventRumour(List<NPC> allNpcs, String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.LOCAL_EVENT, text);
        for (NPC npc : allNpcs) {
            if (npc.isAlive() && (npc.getType() == NPCType.PUBLIC
                    || npc.getType() == NPCType.PENSIONER
                    || npc.getType() == NPCType.FLAT_RESIDENT)) {
                rumourNetwork.addRumour(npc, rumour);
                return;
            }
        }
    }

    private void seedPlayerSpottedRumour(List<NPC> allNpcs, NPC source, String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.PLAYER_SPOTTED, text);
        NPC seeder = source;
        if (seeder == null) {
            for (NPC npc : allNpcs) {
                if (npc.isAlive()) {
                    seeder = npc;
                    break;
                }
            }
        }
        if (seeder != null) {
            rumourNetwork.addRumour(seeder, rumour);
        }
    }

    private void seedCommunalRumour(List<NPC> allNpcs, String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.LOCAL_EVENT, text);
        // Seed to all resident NPCs during the communal gathering
        int count = 0;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.PENSIONER || npc.getType() == NPCType.PUBLIC
                    || npc.getType() == NPCType.FLAT_RESIDENT) {
                rumourNetwork.addRumour(npc, rumour);
                if (++count >= 3) break;
            }
        }
    }

    private void seedNoiseComplaintRumour(List<NPC> allNpcs, String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.NOISE_COMPLAINT, text);
        for (NPC npc : allNpcs) {
            if (npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                return;
            }
        }
    }

    // ── Accessors for inspection day tracking ─────────────────────────────────

    /**
     * Returns the in-game day of Derek's last inspection (for testing).
     */
    public int getLastInspectionDay() {
        return lastInspectionDay;
    }

    /**
     * Directly set the last inspection day (for testing only).
     */
    public void setLastInspectionDayForTesting(int day) {
        this.lastInspectionDay = day;
    }

    /**
     * Returns number of parcels stolen so far (for achievement tracking).
     */
    public int getParcelsStolen() {
        return parcelsStolen;
    }

    /**
     * Returns number of residents gossiped with so far (for achievement tracking).
     */
    public int getResidentsGossiped() {
        return residentsGossiped;
    }

    /**
     * Force the lift to a broken state (for testing).
     */
    public void setLiftBrokenForTesting(boolean broken) {
        this.liftBroken = broken;
        if (!broken) {
            playerTrapped = false;
            claustrophobicTimer = 0;
        }
    }

    /**
     * Force parcel count (for testing).
     */
    public void setParcelsAvailableForTesting(int count) {
        this.parcelsAvailable = count;
    }

    /**
     * Force inspection active state (for testing).
     */
    public void setInspectionActiveForTesting(boolean active, int floor) {
        this.inspectionActive = active;
        this.derekCurrentFloor = floor;
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Three-faction turf-war engine for Phase 8d (Issue #702).
 *
 * <h3>Factions</h3>
 * <ul>
 *   <li>{@link Faction#MARCHETTI_CREW} – organised crime, industrial / off-licence</li>
 *   <li>{@link Faction#STREET_LADS}    – chaotic youths, park / estate</li>
 *   <li>{@link Faction#THE_COUNCIL}    – bureaucrats, town hall / office block</li>
 * </ul>
 *
 * <h3>Respect</h3>
 * Each faction maintains a Respect score (0–100, starting at 50) toward the player.
 * Player actions (hitting NPCs, completing missions, buying rounds, getting arrested,
 * etc.) fire Respect deltas that ripple across all three factions.
 *
 * <h3>Turf</h3>
 * {@link TurfMap} tracks per-block ownership.  When the Respect gap between two factions
 * exceeds 30, 10 % of the weaker faction's blocks are transferred to the stronger one.
 *
 * <h3>Rumours</h3>
 * Turf shifts and Respect threshold crossings seed {@link RumourType#GANG_ACTIVITY}
 * rumours into nearby NPCs so the barman stays informed.
 *
 * <h3>Endgame / Special States</h3>
 * <ul>
 *   <li>Faction Victory: Respect ≥ 90 AND ≥ 60 % turf → faction-specific bonus</li>
 *   <li>Everyone Hates You: all factions below Respect 30 → all hostile, fence prices +50 %</li>
 * </ul>
 */
public class FactionSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    public static final int   STARTING_RESPECT           = 50;
    public static final int   MAX_RESPECT                = 100;
    public static final int   MIN_RESPECT                = 0;

    /** Below this threshold, faction NPCs become hostile on sight. */
    public static final int   HOSTILE_THRESHOLD          = 20;

    /** Above this threshold, faction NPCs are friendly and offer exclusive trade/missions. */
    public static final int   FRIENDLY_THRESHOLD         = 75;

    /** Respect gap that triggers turf transfer. */
    public static final int   TURF_TRANSFER_GAP          = 30;

    /** Respect at or above which + ≥ 60 % turf → faction victory. */
    public static final int   VICTORY_RESPECT            = 90;

    /** Turf fraction required for victory (0–1). */
    public static final float VICTORY_TURF_FRACTION      = 0.60f;

    /** All factions below this simultaneously → "Everyone Hates You". */
    public static final int   EVERYONE_HATES_YOU_THRESHOLD = 30;

    /** How long (seconds) a faction mission runs before expiring. */
    public static final float MISSION_DURATION_SECONDS   = 300f; // 5 in-game minutes

    /** Respect penalty when a mission expires. */
    public static final int   MISSION_FAIL_RESPECT_PENALTY = 10;

    // ── Respect deltas (per action) ───────────────────────────────────────────

    public static final int DELTA_HIT_NPC                = -15;  // Hitting faction NPC
    public static final int DELTA_RIVAL_BUILDING_BREAK   = -10;  // Break block in rival building
    public static final int DELTA_OWNER_BUILDING_BREAK   =   5;  // Owner gains when rival breaks
    public static final int DELTA_MISSION_COMPLETE_SELF  =  20;  // Completing a mission
    public static final int DELTA_MISSION_COMPLETE_RIVAL = -10;  // Rival penalty on mission complete
    public static final int DELTA_BUY_ROUND              =   2;  // All factions: buy a round at pub
    public static final int DELTA_PLACE_GRAFFITI_RIVAL   =  -5;  // Rival loses when graffiti placed
    public static final int DELTA_PLACE_GRAFFITI_SELF    =   8;  // Own faction gains from graffiti
    public static final int DELTA_ARRESTED_NEAR_FACTION  =   3;  // Nearest faction: player arrested

    // ── Fence price multiplier when everyone hates you ────────────────────────
    public static final float EVERYONE_HATES_FENCE_PRICE_MULT = 1.5f;

    // ── State ─────────────────────────────────────────────────────────────────

    private final Map<Faction, Integer> respect;
    private final Map<Faction, FactionMission> activeMissions;
    private final TurfMap turfMap;
    private final RumourNetwork rumourNetwork;
    private final Random random;

    /** Whether the Council Victory endgame state is active. */
    private boolean councilVictoryActive = false;

    /** Whether the "Everyone Hates You" state is active. */
    private boolean everyoneHatesYouActive = false;

    /** Set of faction victories already triggered (so each fires only once). */
    private final java.util.Set<Faction> victoriesTriggered = java.util.EnumSet.noneOf(Faction.class);

    /** Pulse timer per faction for HUD bar pulse effect. */
    private final float[] hudPulseTimer = new float[Faction.values().length];

    /** Last known Respect per faction (for threshold-crossing detection). */
    private final int[] lastRespect = new int[Faction.values().length];

    // ── Constructor ───────────────────────────────────────────────────────────

    /** No-arg constructor for tests and contexts where turf/rumour integration is not needed. */
    public FactionSystem() {
        this(new TurfMap(), new RumourNetwork(new Random()), new Random());
    }

    /** Single-Random constructor for tests. Uses fresh TurfMap and RumourNetwork. */
    public FactionSystem(Random random) {
        this(new TurfMap(), new RumourNetwork(random), random);
    }

    public FactionSystem(TurfMap turfMap, RumourNetwork rumourNetwork) {
        this(turfMap, rumourNetwork, new Random());
    }

    public FactionSystem(TurfMap turfMap, RumourNetwork rumourNetwork, Random random) {
        this.turfMap       = turfMap;
        this.rumourNetwork = rumourNetwork;
        this.random        = random;

        this.respect        = new EnumMap<>(Faction.class);
        this.activeMissions = new EnumMap<>(Faction.class);

        for (Faction f : Faction.values()) {
            respect.put(f, STARTING_RESPECT);
            lastRespect[f.index()] = STARTING_RESPECT;
        }
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame. Updates mission timers, checks turf transfers, detects
     * threshold crossings, and evaluates endgame states.
     *
     * @param delta    seconds since last frame
     * @param player   the player (for COUNCIL_ID award)
     * @param allNpcs  all living NPCs (used to seed rumours)
     */
    public void update(float delta, Player player, List<NPC> allNpcs) {
        // Advance mission timers
        for (Faction f : Faction.values()) {
            FactionMission mission = activeMissions.get(f);
            if (mission != null && mission.isActive()) {
                boolean expired = mission.tick(delta);
                if (expired) {
                    applyRespectDelta(f, -MISSION_FAIL_RESPECT_PENALTY);
                    activeMissions.remove(f);
                }
            }
        }

        // HUD pulse decay
        for (int i = 0; i < hudPulseTimer.length; i++) {
            if (hudPulseTimer[i] > 0f) {
                hudPulseTimer[i] = Math.max(0f, hudPulseTimer[i] - delta);
            }
        }

        // Turf transfer checks: for each pair of factions, if gap > 30 shift 10 %
        Faction[] factions = Faction.values();
        for (int i = 0; i < factions.length; i++) {
            for (int j = i + 1; j < factions.length; j++) {
                Faction a = factions[i];
                Faction b = factions[j];
                int ra = getRespect(a);
                int rb = getRespect(b);
                int gap = Math.abs(ra - rb);
                if (gap > TURF_TRANSFER_GAP) {
                    Faction winner = (ra > rb) ? a : b;
                    Faction loser  = (ra > rb) ? b : a;
                    int transferred = turfMap.transferTurf(loser, winner);
                    if (transferred > 0) {
                        seedTurfShiftRumour(winner, loser, allNpcs);
                    }
                }
            }
        }

        // Detect Respect threshold crossings and seed rumours
        for (Faction f : factions) {
            int current  = getRespect(f);
            int previous = lastRespect[f.index()];
            if (previous < FRIENDLY_THRESHOLD && current >= FRIENDLY_THRESHOLD) {
                seedRumour(f, f.getRumorName() + " are looking for someone reliable", allNpcs);
            } else if (previous > HOSTILE_THRESHOLD && current <= HOSTILE_THRESHOLD) {
                seedRumour(f, "You'd best avoid " + f.getDisplayName() + " territory for a while", allNpcs);
            }
            lastRespect[f.index()] = current;
        }

        // Update faction NPC hostility based on Respect
        updateFactionNpcHostility(allNpcs);

        // Evaluate endgame conditions
        evaluateEndgame(player, allNpcs);
    }

    // ── Respect manipulation ───────────────────────────────────────────────────

    /**
     * Apply a raw Respect delta to one faction (clamped to [0, 100]).
     * Pulses the HUD bar.
     */
    public void applyRespectDelta(Faction faction, int delta) {
        int current = respect.get(faction);
        int newVal  = Math.max(MIN_RESPECT, Math.min(MAX_RESPECT, current + delta));
        respect.put(faction, newVal);
        hudPulseTimer[faction.index()] = 0.5f; // pulse for half a second
    }

    /** Get current Respect for a faction. */
    public int getRespect(Faction faction) {
        return respect.getOrDefault(faction, STARTING_RESPECT);
    }

    /** Force-set Respect (for testing). */
    public void setRespect(Faction faction, int value) {
        respect.put(faction, Math.max(MIN_RESPECT, Math.min(MAX_RESPECT, value)));
        lastRespect[faction.index()] = getRespect(faction);
    }

    // ── Player action hooks ────────────────────────────────────────────────────

    /**
     * Call when the player hits an NPC that belongs to a faction.
     * That faction loses {@link #DELTA_HIT_NPC} Respect and a GANG_ACTIVITY rumour
     * is seeded into nearby NPCs.
     */
    public void onPlayerHitFactionNpc(Faction hitFaction, List<NPC> allNpcs) {
        applyRespectDelta(hitFaction, DELTA_HIT_NPC);
        seedRumour(hitFaction,
                "Someone proper went for a " + hitFaction.getDisplayName() + " lad", allNpcs);
    }

    /**
     * Convenience overload: detects the faction from the NPC's name and fires the hit delta.
     * Does nothing if the NPC does not belong to any faction.
     */
    public void onPlayerHitFactionNpc(NPC hitNpc, List<NPC> allNpcs) {
        Faction faction = detectFactionFromNpc(hitNpc);
        if (faction != null) {
            onPlayerHitFactionNpc(faction, allNpcs);
        }
    }

    /**
     * Call when the player breaks a block associated with a building owned by a faction.
     *
     * @param buildingFaction the faction that owns the building
     * @param allNpcs         living NPCs for rumour seeding
     */
    public void onPlayerBreaksRivalBuilding(Faction buildingFaction, List<NPC> allNpcs) {
        applyRespectDelta(buildingFaction, DELTA_RIVAL_BUILDING_BREAK);
        // The faction whose turf is being damaged gains a little from rival factions
        // (they look tougher by comparison)
        for (Faction rival : buildingFaction.rivals()) {
            applyRespectDelta(rival, DELTA_OWNER_BUILDING_BREAK);
        }
    }

    /**
     * Call when the player completes a mission for a faction.
     * That faction gains {@link #DELTA_MISSION_COMPLETE_SELF}; rivals lose
     * {@link #DELTA_MISSION_COMPLETE_RIVAL}.
     *
     * @param faction   the faction whose mission was completed
     * @param allNpcs   living NPCs for rumour seeding
     */
    public void onMissionCompleted(Faction faction, List<NPC> allNpcs) {
        applyRespectDelta(faction, DELTA_MISSION_COMPLETE_SELF);
        for (Faction rival : faction.rivals()) {
            applyRespectDelta(rival, DELTA_MISSION_COMPLETE_RIVAL);
        }
        FactionMission mission = activeMissions.get(faction);
        String missionTitle = (mission != null) ? mission.getTitle() : "that job";
        seedRumour(faction,
                "Someone sorted out " + missionTitle + " for " + faction.getRumorName(), allNpcs);
        if (mission != null) {
            mission.markCompleted();
            activeMissions.remove(faction);
        }
    }

    /**
     * Call when the player buys a round of drinks at the pub.
     * All factions gain {@link #DELTA_BUY_ROUND} Respect.
     *
     * @param inventory the player's inventory (costs 5 COINs)
     * @return true if the player had enough coins and the round was bought
     */
    public boolean onBuyRound(Inventory inventory) {
        int cost = 5;
        if (inventory.getItemCount(Material.COIN) < cost) {
            return false;
        }
        inventory.removeItem(Material.COIN, cost);
        for (Faction f : Faction.values()) {
            applyRespectDelta(f, DELTA_BUY_ROUND);
        }
        return true;
    }

    /**
     * Call when the player places graffiti in a faction's territory.
     *
     * @param rivalFaction the faction whose turf the graffiti is on
     * @param ownFaction   the faction the graffiti represents (player's allied faction, or null)
     * @param allNpcs      living NPCs for rumour seeding
     */
    public void onGraffitiPlaced(Faction rivalFaction, Faction ownFaction, List<NPC> allNpcs) {
        applyRespectDelta(rivalFaction, DELTA_PLACE_GRAFFITI_RIVAL);
        if (ownFaction != null && ownFaction != rivalFaction) {
            applyRespectDelta(ownFaction, DELTA_PLACE_GRAFFITI_SELF);
        }
    }

    /**
     * Call when the player is arrested near a faction's territory.
     *
     * @param nearestFaction the faction whose territory the player was in
     */
    public void onPlayerArrested(Faction nearestFaction) {
        if (nearestFaction != null) {
            applyRespectDelta(nearestFaction, DELTA_ARRESTED_NEAR_FACTION);
        }
    }

    // ── Mission management ─────────────────────────────────────────────────────

    /**
     * Offer a new mission to the player from a faction's pool.
     * Replaces any existing active mission for that faction.
     *
     * @param faction the faction offering the mission
     * @return the new {@link FactionMission}, or null if none are available
     */
    public FactionMission offerMission(Faction faction) {
        FactionMission mission = buildMission(faction);
        if (mission != null) {
            activeMissions.put(faction, mission);
        }
        return mission;
    }

    /**
     * Retrieve the currently active mission for a faction, or null.
     */
    public FactionMission getActiveMission(Faction faction) {
        return activeMissions.get(faction);
    }

    /**
     * Attempt to complete the active mission for a faction, granting rewards.
     *
     * @param faction   the faction
     * @param inventory the player's inventory (rewards added here)
     * @param allNpcs   living NPCs for rumour seeding
     * @return completion message, or null if no active mission / conditions not met
     */
    public String tryCompleteMission(Faction faction, Inventory inventory, List<NPC> allNpcs) {
        FactionMission mission = activeMissions.get(faction);
        if (mission == null || !mission.isActive()) return null;

        // Reward
        inventory.addItem(Material.COIN, mission.getCoinReward());
        onMissionCompleted(faction, allNpcs);

        return "Job done. " + faction.getDisplayName() + " owe you " +
               mission.getCoinReward() + " coin. Don't spend it all on cider.";
    }

    // ── Endgame / special states ───────────────────────────────────────────────

    /**
     * Returns true if the "Everyone Hates You" state is active
     * (all factions simultaneously below {@link #EVERYONE_HATES_YOU_THRESHOLD}).
     */
    public boolean isEveryoneHatesYouActive() {
        return everyoneHatesYouActive;
    }

    /**
     * Returns the victorious faction if any has reached victory conditions,
     * otherwise null.
     */
    public Faction getVictoriousFaction() {
        for (Faction f : victoriesTriggered) {
            return f; // return first (should only be one)
        }
        return null;
    }

    /** True if Council Victory is active. */
    public boolean isCouncilVictoryActive() {
        return councilVictoryActive;
    }

    // ── HUD pulse ──────────────────────────────────────────────────────────────

    /**
     * Whether the HUD bar for the given faction is currently pulsing.
     */
    public boolean isHudPulsing(Faction faction) {
        return hudPulseTimer[faction.index()] > 0f;
    }

    public float getHudPulseTimer(Faction faction) {
        return hudPulseTimer[faction.index()];
    }

    // ── Fence price modifier ───────────────────────────────────────────────────

    /**
     * Returns the fence buy-price multiplier.  When "Everyone Hates You" is active
     * it returns {@link #EVERYONE_HATES_FENCE_PRICE_MULT}; otherwise 1.0.
     */
    public float getFencePriceMultiplier() {
        return everyoneHatesYouActive ? EVERYONE_HATES_FENCE_PRICE_MULT : 1.0f;
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private void evaluateEndgame(Player player, List<NPC> allNpcs) {
        // Everyone Hates You
        boolean allLow = true;
        for (Faction f : Faction.values()) {
            if (getRespect(f) >= EVERYONE_HATES_YOU_THRESHOLD) {
                allLow = false;
                break;
            }
        }
        boolean wasActive = everyoneHatesYouActive;
        everyoneHatesYouActive = allLow;
        if (allLow && !wasActive) {
            // First activation: seed rumour
            seedGlobalRumour("That one's gone proper feral", allNpcs);
            // Make all faction NPCs hostile
            for (NPC npc : allNpcs) {
                if (npc.isAlive() && isFactionNpc(npc)) {
                    npc.setState(NPCState.ATTACKING_PLAYER);
                }
            }
        }

        // Faction victory conditions
        for (Faction f : Faction.values()) {
            if (victoriesTriggered.contains(f)) continue;
            if (getRespect(f) >= VICTORY_RESPECT &&
                    turfMap.ownershipFraction(f) >= VICTORY_TURF_FRACTION) {
                victoriesTriggered.add(f);
                triggerFactionVictory(f, player, allNpcs);
            }
        }
    }

    private void triggerFactionVictory(Faction faction, Player player, List<NPC> allNpcs) {
        switch (faction) {
            case MARCHETTI_CREW:
                // Off-licence permanent shop, double industrial loot, Council bribed
                seedGlobalRumour("The Marchetti Crew run this town now. Business is booming.", allNpcs);
                break;
            case STREET_LADS:
                // Park permanent safe zone, 2x drug corner coins, youth stop attacking
                seedGlobalRumour("The Street Lads have got the park locked down. No trouble.", allNpcs);
                break;
            case THE_COUNCIL:
                // Auto-demolish structures, police 2x, but player gets COUNCIL_ID
                councilVictoryActive = true;
                seedGlobalRumour("The Council's taken over. Proper sorted. God help us.", allNpcs);
                break;
        }
    }

    private void updateFactionNpcHostility(List<NPC> allNpcs) {
        // Faction NPC hostility is driven by the per-faction Respect level.
        // Lieutenant NPCs are the primary faction-associated NPC type.
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() != NPCType.FACTION_LIEUTENANT) continue;
            // Determine the faction from the NPC's name prefix
            Faction npcFaction = detectFactionFromNpc(npc);
            if (npcFaction == null) continue;
            int rep = getRespect(npcFaction);
            if (rep <= HOSTILE_THRESHOLD &&
                    npc.getState() != NPCState.ATTACKING_PLAYER &&
                    npc.getState() != NPCState.KNOCKED_OUT) {
                npc.setState(NPCState.ATTACKING_PLAYER);
            }
        }
    }

    private boolean isFactionNpc(NPC npc) {
        return npc.getType() == NPCType.FACTION_LIEUTENANT ||
               npc.getType() == NPCType.YOUTH_GANG;
    }

    /**
     * Detect which faction an NPC belongs to based on its name.
     * Convention: name starts with "marchetti_", "streetlads_", or "council_".
     */
    private Faction detectFactionFromNpc(NPC npc) {
        String name = npc.getName();
        if (name == null) return null;
        String lower = name.toLowerCase();
        if (lower.startsWith("marchetti")) return Faction.MARCHETTI_CREW;
        if (lower.startsWith("streetlads") || lower.startsWith("street_lads")) return Faction.STREET_LADS;
        if (lower.startsWith("council")) return Faction.THE_COUNCIL;
        return null;
    }

    private void seedTurfShiftRumour(Faction winner, Faction loser, List<NPC> allNpcs) {
        String text = winner.getRumorName() + " are taking over " + loser.getTerritory() + " now";
        seedRumour(winner, text, allNpcs);
    }

    /**
     * Seed a GANG_ACTIVITY rumour into up to 3 nearby NPCs.
     */
    private void seedRumour(Faction about, String text, List<NPC> allNpcs) {
        Rumour rumour = new Rumour(RumourType.GANG_ACTIVITY, text);
        int seeded = 0;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            rumourNetwork.addRumour(npc, rumour);
            seeded++;
            if (seeded >= 3) break;
        }
    }

    /**
     * Seed a GANG_ACTIVITY rumour into all available NPCs (global events).
     */
    private void seedGlobalRumour(String text, List<NPC> allNpcs) {
        Rumour rumour = new Rumour(RumourType.GANG_ACTIVITY, text);
        int seeded = 0;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            rumourNetwork.addRumour(npc, rumour);
            seeded++;
            if (seeded >= 3) break;
        }
    }

    /**
     * Build a procedural mission for the given faction.
     */
    private FactionMission buildMission(Faction faction) {
        switch (faction) {
            case MARCHETTI_CREW: return buildMarchettiMission();
            case STREET_LADS:    return buildStreetLadsMission();
            case THE_COUNCIL:    return buildCouncilMission();
            default:             return null;
        }
    }

    private FactionMission buildMarchettiMission() {
        int pick = random.nextInt(3);
        switch (pick) {
            case 0:
                return new FactionMission(
                        Faction.MARCHETTI_CREW,
                        FactionMission.MissionType.DELIVERY_RUN,
                        "Delivery Run",
                        "Carry a PETROL_CAN from the industrial estate to the off-licence. " +
                        "Don't dawdle — you've got 3 minutes.",
                        8, 20, 10, MISSION_DURATION_SECONDS);
            case 1:
                return new FactionMission(
                        Faction.MARCHETTI_CREW,
                        FactionMission.MissionType.EVICTION_NOTICE,
                        "Eviction Notice",
                        "Break 10 blocks of the Street Lads' park shelter. " +
                        "Send them a message.",
                        6, 25, 0, MISSION_DURATION_SECONDS);
            default:
                return new FactionMission(
                        Faction.MARCHETTI_CREW,
                        FactionMission.MissionType.QUIET_THE_WITNESS,
                        "Quiet the Witness",
                        "Hit a WITNESS NPC 3 times before they reach the police station. " +
                        "No mess.",
                        10, 30, 0, MISSION_DURATION_SECONDS);
        }
    }

    private FactionMission buildStreetLadsMission() {
        int pick = random.nextInt(3);
        switch (pick) {
            case 0:
                return new FactionMission(
                        Faction.STREET_LADS,
                        FactionMission.MissionType.CORNER_DEFENCE,
                        "Corner Defence",
                        "Block Marchetti NPCs crossing the main intersection for 2 minutes. " +
                        "Show them who owns the corners.",
                        5, 20, 0, MISSION_DURATION_SECONDS);
            case 1:
                return new FactionMission(
                        Faction.STREET_LADS,
                        FactionMission.MissionType.OFFICE_JOB,
                        "Office Job",
                        "Steal 3 COMPUTER items from the office without getting arrested. " +
                        "Nice earner.",
                        8, 25, 0, MISSION_DURATION_SECONDS);
            default:
                return new FactionMission(
                        Faction.STREET_LADS,
                        FactionMission.MissionType.TAG_THE_TURF,
                        "Tag the Turf",
                        "Place 5 graffiti tags on Marchetti walls. Let them know.",
                        6, 20, 0, MISSION_DURATION_SECONDS);
        }
    }

    private FactionMission buildCouncilMission() {
        int pick = random.nextInt(3);
        switch (pick) {
            case 0:
                return new FactionMission(
                        Faction.THE_COUNCIL,
                        FactionMission.MissionType.VOLUNTARY_COMPLIANCE,
                        "Voluntary Compliance",
                        "Demolish any structure you've built with more than 20 blocks " +
                        "to avoid a planning fine. The Council appreciates compliance.",
                        0, 15, 0, MISSION_DURATION_SECONDS);
            case 1:
                return new FactionMission(
                        Faction.THE_COUNCIL,
                        FactionMission.MissionType.REPORT_A_NUISANCE,
                        "Report a Nuisance",
                        "Press E near the Street Lads drug corner. " +
                        "The Council will note your civic responsibility.",
                        0, 20, 0, MISSION_DURATION_SECONDS);
            default:
                return new FactionMission(
                        Faction.THE_COUNCIL,
                        FactionMission.MissionType.CLEAR_THE_ENCAMPMENT,
                        "Clear the Encampment",
                        "Destroy 20 CARDBOARD blocks in the park encampment. " +
                        "The Council thanks you for keeping the area tidy.",
                        10, 25, 0, MISSION_DURATION_SECONDS);
        }
    }

    // ── Accessors (for tests) ──────────────────────────────────────────────────

    public TurfMap getTurfMap() {
        return turfMap;
    }

    public Map<Faction, FactionMission> getActiveMissions() {
        return java.util.Collections.unmodifiableMap(activeMissions);
    }
}

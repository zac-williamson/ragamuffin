package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #767: Disguise &amp; Social Engineering System
 *
 * <p>The player can knock out NPCs, loot their clothing, and wear it to
 * infiltrate restricted areas and faction territories.
 *
 * <h3>Disguises and their faction-specific access</h3>
 * <ul>
 *   <li>{@link Material#POLICE_UNIFORM} — stops police chasing you while cover holds</li>
 *   <li>{@link Material#COUNCIL_JACKET} — demolish structures without triggering council builders</li>
 *   <li>{@link Material#MARCHETTI_TRACKSUIT} — safe passage through Marchetti territory</li>
 *   <li>{@link Material#STREET_LADS_HOODIE} — safe passage through Street Lads territory</li>
 *   <li>{@link Material#HI_VIS_VEST} — access to construction/service areas</li>
 *   <li>{@link Material#GREGGS_APRON} — comedy gag: perfect beyond 3 blocks, immediately
 *       transparent within 3 blocks</li>
 * </ul>
 *
 * <h3>Cover integrity</h3>
 * Cover starts at 100 and decays:
 * <ul>
 *   <li>–5 per second while a suspicious NPC is scrutinising</li>
 *   <li>–20 instant on committing a crime while disguised</li>
 *   <li>–30 instant when running during scrutiny</li>
 *   <li>–100 (blow cover immediately) if sprinting within 3 blocks of any NPC
 *       while wearing GREGGS_APRON</li>
 * </ul>
 * Cover is blown (set to 0) when integrity hits 0.
 *
 * <h3>Scrutiny mechanic</h3>
 * Suspicious NPCs within {@link #SCRUTINY_RANGE} blocks freeze, enter
 * {@link NPCState#SCRUTINISING}, and stare for {@link #SCRUTINY_DURATION} seconds.
 * If the player stands still, the NPC returns to normal. If the player runs, cover
 * is immediately blown.
 *
 * <h3>Bluff mechanic</h3>
 * When pressing E to enter a guarded building, there is a 60 % base success chance
 * (90 % if holding a {@link Material#RUMOUR_NOTE} as a prop).
 *
 * <h3>Heist integration</h3>
 * Wearing a {@link Material#POLICE_UNIFORM} delays the heist alarm response by
 * {@link #POLICE_DISGUISE_ALARM_DELAY} seconds.
 */
public class DisguiseSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Full cover integrity on equip. */
    public static final float MAX_COVER_INTEGRITY = 100f;

    /** Cover integrity decay per second while actively being scrutinised. */
    public static final float SCRUTINY_DECAY_RATE = 5f;

    /** Instant cover integrity penalty for committing a crime while disguised. */
    public static final float CRIME_COVER_PENALTY = 20f;

    /** Instant cover integrity penalty for running during scrutiny. */
    public static final float RUN_DURING_SCRUTINY_PENALTY = 30f;

    /** Distance at which a suspicious NPC will begin scrutinising the player. */
    public static final float SCRUTINY_RANGE = 6f;

    /** Seconds an NPC freezes and stares before deciding the disguise passes. */
    public static final float SCRUTINY_DURATION = 3f;

    /** Base success chance for a bluff at an enemy building entrance (60 %). */
    public static final float BLUFF_BASE_SUCCESS = 0.60f;

    /** Bluff success chance when carrying a RUMOUR_NOTE as a prop (90 %). */
    public static final float BLUFF_PROP_SUCCESS = 0.90f;

    /** Range within which the GREGGS_APRON disguise is immediately transparent (blocks). */
    public static final float GREGGS_APRON_FAIL_RANGE = 3f;

    /**
     * Seconds by which a POLICE_UNIFORM delays alarm response in the heist system.
     * The calling code (HeistSystem integration) reads this constant.
     */
    public static final float POLICE_DISGUISE_ALARM_DELAY = 30f;

    /** Movement speed threshold above which the player is considered "running". */
    public static final float RUN_SPEED_THRESHOLD = 2.5f;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Currently worn disguise material, or null if not disguised. */
    private Material activeDisguise = null;

    /** Current cover integrity (0–100). 0 means cover is blown. */
    private float coverIntegrity = MAX_COVER_INTEGRITY;

    /** Whether cover has been blown this disguise session. */
    private boolean coverBlown = false;

    /** Whether the player is currently being scrutinised by at least one NPC. */
    private boolean underScrutiny = false;

    /** Tracks active scrutiny events (one per NPC currently scrutinising). */
    private final List<ScrutinyEvent> scrutinyEvents = new ArrayList<>();

    /** True if the player was scrutinised at least once during this disguise session. */
    private boolean wasEverScrutinised = false;

    /** True if this is the player's first time entering bluff mode (for UNDERCOVER achievement). */
    private boolean hasCompletedInfiltration = false;

    private final Random random;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;

    // ── Construction ──────────────────────────────────────────────────────────

    public DisguiseSystem() {
        this(new Random());
    }

    public DisguiseSystem(Random random) {
        this.random = random;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    // ── Equip / unequip ───────────────────────────────────────────────────────

    /**
     * Equip a disguise from the player's inventory.
     * Resets cover integrity to 100 and starts the disguise session.
     *
     * @param disguise  the disguise material to wear
     * @param inventory the player's inventory (must contain at least 1 of this material)
     * @return true if the disguise was successfully equipped
     */
    public boolean equipDisguise(Material disguise, Inventory inventory) {
        if (!isDisguiseMaterial(disguise)) return false;
        if (!inventory.hasItem(disguise, 1)) return false;

        activeDisguise = disguise;
        coverIntegrity = MAX_COVER_INTEGRITY;
        coverBlown = false;
        underScrutiny = false;
        scrutinyEvents.clear();
        wasEverScrutinised = false;
        hasCompletedInfiltration = false;
        return true;
    }

    /**
     * Remove the current disguise (voluntarily or when cover is blown).
     * Awards the INCOGNITO achievement if the player was never scrutinised.
     */
    public void removeDisguise() {
        if (activeDisguise == null) return;

        if (!wasEverScrutinised && !coverBlown && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.INCOGNITO);
        }

        activeDisguise = null;
        coverIntegrity = MAX_COVER_INTEGRITY;
        coverBlown = false;
        underScrutiny = false;
        scrutinyEvents.clear();
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the disguise system each frame.
     *
     * @param delta         seconds since last frame
     * @param player        the player
     * @param npcs          all living NPCs
     * @param playerSpeed   current player horizontal speed (blocks/sec)
     */
    public void update(float delta, Player player, List<NPC> npcs, float playerSpeed) {
        if (activeDisguise == null || coverBlown) return;

        // GREGGS_APRON special case: immediately blown within GREGGS_APRON_FAIL_RANGE
        if (activeDisguise == Material.GREGGS_APRON) {
            for (NPC npc : npcs) {
                if (!npc.isAlive()) continue;
                float dist = npc.getPosition().dst(player.getPosition());
                if (dist <= GREGGS_APRON_FAIL_RANGE) {
                    blowCover("That's just Dave in an apron!", player, npcs);
                    if (achievementSystem != null) {
                        achievementSystem.unlock(AchievementType.OBVIOUS_IN_HINDSIGHT);
                    }
                    return;
                }
            }
        }

        // Update scrutiny events
        updateScrutiny(delta, player, npcs, playerSpeed);

        // Decay cover integrity while under scrutiny
        if (underScrutiny) {
            coverIntegrity -= SCRUTINY_DECAY_RATE * delta;
            if (coverIntegrity <= 0f) {
                coverIntegrity = 0f;
                blowCover(getBlownDialogue(), player, npcs);
            }
        }
    }

    private void updateScrutiny(float delta, Player player, List<NPC> npcs, float playerSpeed) {
        boolean isRunning = playerSpeed >= RUN_SPEED_THRESHOLD;

        // Check for new NPCs entering scrutiny range
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getState() == NPCState.SCRUTINISING) continue; // already scrutinising

            float dist = npc.getPosition().dst(player.getPosition());
            if (dist <= SCRUTINY_RANGE && shouldNPCScrutinise(npc)) {
                // Start scrutiny
                npc.setState(NPCState.SCRUTINISING);
                npc.setSpeechText("Oi... 'ang on a minute...", SCRUTINY_DURATION);
                scrutinyEvents.add(new ScrutinyEvent(npc, SCRUTINY_DURATION));
                wasEverScrutinised = true;
            }
        }

        // Update active scrutiny events — collect removals to avoid ConcurrentModificationException
        List<ScrutinyEvent> toRemove = new ArrayList<>();
        boolean shouldBlowCover = false;
        String blowCoverReason = null;

        for (ScrutinyEvent event : scrutinyEvents) {
            if (!event.npc.isAlive()) {
                toRemove.add(event);
                continue;
            }

            // If player is running during scrutiny, blow cover immediately
            if (isRunning && event.npc.getState() == NPCState.SCRUTINISING) {
                coverIntegrity = Math.max(0f, coverIntegrity - RUN_DURING_SCRUTINY_PENALTY);
                shouldBlowCover = true;
                blowCoverReason = "Oi! You're legging it!";
                toRemove.add(event);
                continue;
            }

            // Check if NPC has moved out of range
            float dist = event.npc.getPosition().dst(player.getPosition());
            if (dist > SCRUTINY_RANGE * 1.5f) {
                event.npc.setState(NPCState.WANDERING);
                toRemove.add(event);
                continue;
            }

            // Count down scrutiny timer
            event.timer -= delta;
            if (event.timer <= 0f) {
                // Scrutiny passed — NPC satisfied
                event.npc.setState(NPCState.WANDERING);
                event.npc.setSpeechText("Right, sorry mate...", 2f);
                toRemove.add(event);
            }
        }

        scrutinyEvents.removeAll(toRemove);

        // Blow cover after iteration to avoid ConcurrentModificationException
        if (shouldBlowCover) {
            blowCover(blowCoverReason, player, npcs);
            return;
        }

        underScrutiny = !scrutinyEvents.isEmpty();
    }

    /**
     * Returns whether the given NPC type would scrutinise the player's disguise.
     * Police scrutinise non-police disguises, faction NPCs scrutinise rival disguises, etc.
     */
    private boolean shouldNPCScrutinise(NPC npc) {
        if (activeDisguise == null) return false;
        switch (activeDisguise) {
            case POLICE_UNIFORM:
                // Police won't scrutinise a colleague; council builders might
                return npc.getType() == NPCType.COUNCIL_MEMBER
                        || npc.getType() == NPCType.COUNCIL_BUILDER;
            case COUNCIL_JACKET:
                return npc.getType() == NPCType.POLICE
                        || npc.getType() == NPCType.PCSO;
            case MARCHETTI_TRACKSUIT:
                return npc.getType() == NPCType.POLICE
                        || npc.getType() == NPCType.PCSO
                        || npc.getType() == NPCType.ARMED_RESPONSE;
            case STREET_LADS_HOODIE:
                return npc.getType() == NPCType.POLICE
                        || npc.getType() == NPCType.PCSO;
            case HI_VIS_VEST:
                return npc.getType() == NPCType.POLICE;
            case GREGGS_APRON:
                // Handled separately (range-based), but general NPCs may scrutinise
                return npc.getType() == NPCType.POLICE
                        || npc.getType() == NPCType.COUNCIL_MEMBER;
            // Issue #924: Launderette disguises attract police scrutiny
            case BLOODY_HOODIE:
            case STOLEN_JACKET:
                return npc.getType() == NPCType.POLICE
                        || npc.getType() == NPCType.PCSO;
            default:
                return false;
        }
    }

    // ── Cover blown ────────────────────────────────────────────────────────────

    /**
     * Immediately blow the player's cover, setting integrity to 0 and transitioning
     * scrutinising NPCs to hostile states.
     *
     * @param dialogue speech text to apply to nearby NPCs
     * @param player   the player
     * @param npcs     all living NPCs
     */
    public void blowCover(String dialogue, Player player, List<NPC> npcs) {
        if (coverBlown) return;

        coverBlown = true;
        coverIntegrity = 0f;
        underScrutiny = false;

        // Hostile NPCs react
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            float dist = npc.getPosition().dst(player.getPosition());
            if (dist <= SCRUTINY_RANGE * 2f) {
                if (npc.getState() == NPCState.SCRUTINISING
                        || shouldNPCScrutinise(npc)) {
                    npc.setState(NPCState.AGGRESSIVE);
                    npc.setSpeechText(dialogue, 4f);
                }
            }
        }

        // Clear scrutiny events
        for (ScrutinyEvent event : scrutinyEvents) {
            if (event.npc.isAlive() && event.npc.getState() == NPCState.SCRUTINISING) {
                event.npc.setState(NPCState.AGGRESSIVE);
            }
        }
        scrutinyEvents.clear();

        // Seed a witness rumour about the blown disguise
        if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
            String disguiseName = activeDisguise != null ? activeDisguise.getDisplayName() : "disguise";
            String rumourText = "Someone in a " + disguiseName + " was spotted — turned out to be an impostor";
            NPC nearestNpc = null;
            float nearestDist = Float.MAX_VALUE;
            for (NPC npc : npcs) {
                if (!npc.isAlive()) continue;
                float d = npc.getPosition().dst(player.getPosition());
                if (d < nearestDist) {
                    nearestDist = d;
                    nearestNpc = npc;
                }
            }
            if (nearestNpc != null) {
                rumourNetwork.addRumour(nearestNpc, new Rumour(RumourType.WITNESS_SIGHTING, rumourText));
            }
        }

        // Remove disguise
        activeDisguise = null;
    }

    /**
     * Notify the disguise system that the player has committed a crime while disguised.
     * Applies the {@link #CRIME_COVER_PENALTY} integrity penalty.
     *
     * @param player the player
     * @param npcs   all living NPCs
     */
    public void notifyCrime(Player player, List<NPC> npcs) {
        if (activeDisguise == null || coverBlown) return;

        coverIntegrity -= CRIME_COVER_PENALTY;
        if (coverIntegrity <= 0f) {
            coverIntegrity = 0f;
            blowCover("Oi! You're supposed to be one of us!", player, npcs);
        }
    }

    // ── Bluff mechanic ────────────────────────────────────────────────────────

    /**
     * Result of a bluff attempt.
     */
    public enum BluffResult {
        /** Bluff succeeded — player gains access. */
        SUCCESS,
        /** Bluff failed — cover is blown. */
        FAILED,
        /** No active disguise equipped. */
        NO_DISGUISE,
        /** Cover is already blown. */
        COVER_BLOWN
    }

    /**
     * Attempt to bluff entry into a restricted area.
     * Base success chance is 60 %; holding a {@link Material#RUMOUR_NOTE} raises it to 90 %.
     *
     * @param inventory the player's inventory (checked for RUMOUR_NOTE prop)
     * @param player    the player
     * @param npcs      all living NPCs
     * @return the result of the bluff attempt
     */
    public BluffResult attemptBluff(Inventory inventory, Player player, List<NPC> npcs) {
        if (coverBlown) return BluffResult.COVER_BLOWN;
        if (activeDisguise == null) return BluffResult.NO_DISGUISE;

        boolean hasProp = inventory != null && inventory.hasItem(Material.RUMOUR_NOTE, 1);
        float successChance = hasProp ? BLUFF_PROP_SUCCESS : BLUFF_BASE_SUCCESS;

        if (random.nextFloat() < successChance) {
            // Bluff succeeded
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.UNDERCOVER);
                hasCompletedInfiltration = true;
            }
            return BluffResult.SUCCESS;
        } else {
            // Bluff failed — blow cover
            blowCover("Oi! I don't know you! Who sent you?!", player, npcs);
            return BluffResult.FAILED;
        }
    }

    // ── Method actor tracking ──────────────────────────────────────────────────

    /**
     * Notify that an infiltration has been completed successfully while cover integrity
     * remained above 50 throughout. Awards the METHOD_ACTOR achievement.
     */
    public void notifyInfiltrationComplete(float finalIntegrity, Player player, List<NPC> npcs) {
        if (finalIntegrity >= 50f && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.METHOD_ACTOR);
        }
        // Remove the disguise cleanly
        removeDisguise();
    }

    /**
     * Notify that the player is wearing a rival faction disguise (TURNCOAT achievement).
     *
     * @param rivalDisguise the rival faction's disguise worn
     */
    public void notifyRivalDisguiseWorn(Material rivalDisguise) {
        if (achievementSystem != null && isRivalFactionDisguise(rivalDisguise)) {
            achievementSystem.unlock(AchievementType.TURNCOAT);
        }
    }

    // ── Looting disguise from knocked-out NPC ─────────────────────────────────

    /**
     * Try to loot a disguise from a knocked-out NPC.
     * Returns the {@link Material} that should be added to the player's inventory,
     * or null if the NPC doesn't carry a disguise.
     *
     * @param npc       the knocked-out NPC to loot clothing from
     * @param inventory the player's inventory (receives the item)
     * @return the looted disguise material, or null
     */
    public Material lootDisguise(NPC npc, Inventory inventory) {
        if (npc == null || npc.isAlive()) return null;

        Material disguise = getDisguiseForNPCType(npc.getType());
        if (disguise == null) return null;

        inventory.addItem(disguise, 1);
        return disguise;
    }

    /**
     * Returns the disguise material associated with the given NPC type, or null.
     */
    public static Material getDisguiseForNPCType(NPCType type) {
        switch (type) {
            case POLICE:
            case PCSO:
            case ARMED_RESPONSE:
                return Material.POLICE_UNIFORM;
            case COUNCIL_MEMBER:
            case COUNCIL_BUILDER:
                return Material.COUNCIL_JACKET;
            case FACTION_LIEUTENANT:
                // Faction lieutenants: context-dependent; return MARCHETTI_TRACKSUIT as default
                return Material.MARCHETTI_TRACKSUIT;
            case STREET_LAD:
                return Material.STREET_LADS_HOODIE;
            case LOLLIPOP_LADY:
            case DELIVERY_DRIVER:
                return Material.HI_VIS_VEST;
            case SHOPKEEPER:
                // Greggs shopkeeper
                return Material.GREGGS_APRON;
            default:
                return null;
        }
    }

    // ── Heist integration ────────────────────────────────────────────────────────

    /**
     * Returns true if the player is wearing a POLICE_UNIFORM, which delays alarm
     * response in the heist system by {@link #POLICE_DISGUISE_ALARM_DELAY} seconds.
     */
    public boolean isWearingPoliceUniform() {
        return activeDisguise == Material.POLICE_UNIFORM && !coverBlown;
    }

    // ── Access checks ────────────────────────────────────────────────────────────

    /**
     * Returns true if the player's active disguise grants access to police areas
     * (i.e. police won't chase while cover holds).
     */
    public boolean hasPoliceAccess() {
        return activeDisguise == Material.POLICE_UNIFORM && !coverBlown;
    }

    /**
     * Returns true if the player's active disguise grants council access
     * (won't trigger builders during demolition).
     */
    public boolean hasCouncilAccess() {
        return activeDisguise == Material.COUNCIL_JACKET && !coverBlown;
    }

    /**
     * Returns true if the player's active disguise grants Marchetti territory access.
     */
    public boolean hasMarchettiAccess() {
        return activeDisguise == Material.MARCHETTI_TRACKSUIT && !coverBlown;
    }

    /**
     * Returns true if the player's active disguise grants Street Lads territory access.
     */
    public boolean hasStreetLadsAccess() {
        return activeDisguise == Material.STREET_LADS_HOODIE && !coverBlown;
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    /** Returns the currently worn disguise material, or null if not disguised. */
    public Material getActiveDisguise() {
        return activeDisguise;
    }

    /** Returns the current cover integrity (0–100). */
    public float getCoverIntegrity() {
        return coverIntegrity;
    }

    /** Returns true if the player is currently disguised and cover has not been blown. */
    public boolean isDisguised() {
        return activeDisguise != null && !coverBlown;
    }

    /** Returns true if cover has been blown. */
    public boolean isCoverBlown() {
        return coverBlown;
    }

    /** Returns true if the player is currently under scrutiny from at least one NPC. */
    public boolean isUnderScrutiny() {
        return underScrutiny;
    }

    /**
     * Returns the HUD status for the cover integrity indicator.
     * GREEN = 75–100, AMBER = 25–74, RED = 1–24 (pulses below 25), NONE = not disguised.
     */
    public CoverStatus getCoverStatus() {
        if (activeDisguise == null || coverBlown) return CoverStatus.NONE;
        if (coverIntegrity >= 75f) return CoverStatus.GREEN;
        if (coverIntegrity >= 25f) return CoverStatus.AMBER;
        return CoverStatus.RED;
    }

    /**
     * HUD cover status indicator.
     */
    public enum CoverStatus {
        NONE,   // Not disguised
        GREEN,  // Cover integrity 75–100
        AMBER,  // Cover integrity 25–74
        RED     // Cover integrity 1–24 (pulse animation below 25)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns true if the given material is a wearable disguise.
     */
    public static boolean isDisguiseMaterial(Material material) {
        switch (material) {
            case POLICE_UNIFORM:
            case COUNCIL_JACKET:
            case MARCHETTI_TRACKSUIT:
            case STREET_LADS_HOODIE:
            case HI_VIS_VEST:
            case GREGGS_APRON:
            // Issue #924: Launderette disguises
            case BLOODY_HOODIE:
            case STOLEN_JACKET:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if the given disguise is a rival faction disguise
     * (MARCHETTI_TRACKSUIT or STREET_LADS_HOODIE), for the TURNCOAT achievement.
     */
    private static boolean isRivalFactionDisguise(Material material) {
        return material == Material.MARCHETTI_TRACKSUIT
                || material == Material.STREET_LADS_HOODIE;
    }

    /**
     * Returns faction-appropriate "cover blown" dialogue for the active disguise.
     */
    private String getBlownDialogue() {
        if (activeDisguise == null) return "Oi!";
        switch (activeDisguise) {
            case POLICE_UNIFORM:
                return "Oi! You're not police! Get him!";
            case COUNCIL_JACKET:
                return "You're not from the council! Who are you?!";
            case MARCHETTI_TRACKSUIT:
                return "You're not one of us, bruv. Big mistake.";
            case STREET_LADS_HOODIE:
                return "Nah, I don't recognise you. Wrong endz.";
            case HI_VIS_VEST:
                return "You don't work here! Security!";
            case GREGGS_APRON:
                return "That's just Dave in an apron!";
            // Issue #924: Launderette disguises
            case BLOODY_HOODIE:
                return "Oi! Is that blood?! Get him!";
            case STOLEN_JACKET:
                return "That's a stolen jacket, that! Grab him!";
            default:
                return "Oi! Who are you?!";
        }
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * Tracks a single scrutiny event: an NPC that is currently freeze-staring.
     */
    private static class ScrutinyEvent {
        final NPC npc;
        float timer;

        ScrutinyEvent(NPC npc, float timer) {
            this.npc = npc;
            this.timer = timer;
        }
    }
}

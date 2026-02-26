package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks gang territories in the world and manages player intrusion.
 *
 * Each territory is a named circular area on the map. When the player walks in,
 * the local youth gang gets territorial. They escalate to hostile if the player
 * stays or provokes them.
 *
 * States:
 *   CLEAR    - player is not in any territory
 *   WARNED   - player entered a territory; gang has noticed
 *   HOSTILE  - gang has turned aggressive (player lingered or fought back)
 */
public class GangTerritorySystem {

    /** How long (seconds) the player can linger in a territory before gangs turn hostile. */
    public static final float LINGER_THRESHOLD_SECONDS = 5.0f;

    public enum TerritoryState {
        CLEAR,
        WARNED,
        HOSTILE
    }

    /** A single gang territory: named, centred at (cx, cz), with given radius. */
    public static class Territory {
        public final String name;
        public final float cx;
        public final float cz;
        public final float radius;

        public Territory(String name, float cx, float cz, float radius) {
            this.name = name;
            this.cx = cx;
            this.cz = cz;
            this.radius = radius;
        }

        /** Returns true if (x, z) is within this territory. */
        public boolean contains(float x, float z) {
            float dx = x - cx;
            float dz = z - cz;
            return (dx * dx + dz * dz) <= (radius * radius);
        }
    }

    private final List<Territory> territories;
    private TerritoryState state;
    private Territory currentTerritory; // the territory the player is currently inside, or null
    private float lingerTimer;          // how long player has been inside current territory

    public GangTerritorySystem() {
        this.territories = new ArrayList<>();
        this.state = TerritoryState.CLEAR;
        this.currentTerritory = null;
        this.lingerTimer = 0f;
    }

    /**
     * Register a new named gang territory.
     *
     * @param name   descriptive name (e.g. "Bricky Estate")
     * @param cx     centre X coordinate
     * @param cz     centre Z coordinate
     * @param radius radius of the territory in world units
     */
    public void addTerritory(String name, float cx, float cz, float radius) {
        territories.add(new Territory(name, cx, cz, radius));
    }

    /**
     * Must be called every game tick. Tracks the player's position relative to
     * registered territories and triggers warnings and hostility escalation.
     *
     * @param delta          frame delta in seconds
     * @param player         the player
     * @param tooltipSystem  used to show territory-entry messages
     * @param npcManager     used to make nearby gang NPCs aggressive
     * @param world          passed through to npcManager for pathfinding
     */
    public void update(float delta, Player player, TooltipSystem tooltipSystem,
                       NPCManager npcManager, World world) {
        float px = player.getPosition().x;
        float pz = player.getPosition().z;

        Territory inside = findContaining(px, pz);

        if (inside == null) {
            // Player left all territories — reset to CLEAR (gangs calm down over time, not instantly)
            if (state != TerritoryState.CLEAR) {
                state = TerritoryState.CLEAR;
                currentTerritory = null;
                lingerTimer = 0f;
            }
            return;
        }

        if (currentTerritory == null || !currentTerritory.name.equals(inside.name)) {
            // Entered a new (or different) territory
            currentTerritory = inside;
            lingerTimer = 0f;
            if (state == TerritoryState.CLEAR) {
                state = TerritoryState.WARNED;
                tooltipSystem.trigger(TooltipTrigger.GANG_TERRITORY_ENTERED);
            }
        }

        // Accumulate linger time
        lingerTimer += delta;

        if (state == TerritoryState.WARNED && lingerTimer >= LINGER_THRESHOLD_SECONDS) {
            state = TerritoryState.HOSTILE;
            tooltipSystem.trigger(TooltipTrigger.GANG_TERRITORY_HOSTILE);
            makeNearbyGangsAggressive(player, npcManager, world);
        }
    }

    /**
     * Immediately escalate to HOSTILE — call this when the player attacks a gang member
     * inside a territory.
     */
    public void onPlayerAttacksGang(TooltipSystem tooltipSystem, NPCManager npcManager,
                                    Player player, World world) {
        if (currentTerritory != null && state != TerritoryState.HOSTILE) {
            state = TerritoryState.HOSTILE;
            tooltipSystem.trigger(TooltipTrigger.GANG_TERRITORY_HOSTILE);
            makeNearbyGangsAggressive(player, npcManager, world);
        }
    }

    /**
     * Get current territory state.
     */
    public TerritoryState getState() {
        return state;
    }

    /**
     * Get the territory the player is currently inside, or null if none.
     */
    public Territory getCurrentTerritory() {
        return currentTerritory;
    }

    /**
     * How many territories are registered.
     */
    public int getTerritoryCount() {
        return territories.size();
    }

    /**
     * Returns the first territory containing (x, z), or null if none.
     */
    public Territory findContaining(float x, float z) {
        for (Territory t : territories) {
            if (t.contains(x, z)) {
                return t;
            }
        }
        return null;
    }

    /**
     * How long (seconds) the player has been inside the current territory.
     */
    public float getLingerTimer() {
        return lingerTimer;
    }

    /**
     * Reset all territory state — call on player arrest or respawn.
     */
    public void reset() {
        state = TerritoryState.CLEAR;
        currentTerritory = null;
        lingerTimer = 0f;
    }

    /**
     * Reset the gang linger timer — called when the player activates a BALACLAVA (Fix #687).
     * This gives the player another {@link #LINGER_THRESHOLD_SECONDS} seconds before gangs
     * turn hostile, as if they just entered the territory for the first time.
     * Only has effect when the player is WARNED (not already HOSTILE).
     */
    public void resetLingerTimer() {
        if (state == TerritoryState.WARNED) {
            lingerTimer = 0f;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Set nearby YOUTH_GANG NPCs to AGGRESSIVE and give them something to say.
     *  Also spawns additional attackers so the player always faces a response. */
    private void makeNearbyGangsAggressive(Player player, NPCManager npcManager, World world) {
        Vector3 playerPos = player.getPosition();
        for (NPC npc : npcManager.getNPCs()) {
            if (npc.getType() == NPCType.YOUTH_GANG && npc.isAlive()
                    && npc.getState() != NPCState.AGGRESSIVE) {
                float dist = npc.getPosition().dst(playerPos);
                if (dist < 30.0f) {
                    npc.setState(NPCState.AGGRESSIVE);
                    npc.setSpeechText("Wrong ends, bruv!", 3.0f);
                }
            }
        }
        // Spawn additional attackers so the player always faces a gang response,
        // even if no YOUTH_GANG NPCs happen to be nearby (fixes Issue #493).
        npcManager.spawnGangAttackers(player, world);
    }
}

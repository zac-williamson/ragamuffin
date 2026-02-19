package ragamuffin.core;

import ragamuffin.ai.NPCManager;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.World;

/**
 * Tracks the state of a Greggs raid and manages escalation.
 *
 * The raid progresses through three levels:
 *   NONE       - player hasn't touched Greggs
 *   ALERT      - first block broken, police are suspicious
 *   ESCALATED  - threshold reached, police are mobilised and angry
 *
 * This class encapsulates the raid mechanic that was previously spread
 * across RagamuffinGame so it can be tested in isolation and reused.
 */
public class GreggsRaidSystem {

    /** Number of Greggs blocks that must be broken before the full police response. */
    public static final int RAID_ESCALATION_THRESHOLD = 3;

    /** Raid states — models how the law reacts to sausage roll crime. */
    public enum RaidLevel {
        NONE,
        ALERT,
        ESCALATED
    }

    private int blocksBroken;
    private RaidLevel level;

    public GreggsRaidSystem() {
        this.blocksBroken = 0;
        this.level = RaidLevel.NONE;
    }

    /**
     * Record that the player has broken a Greggs block.
     * Triggers tooltips and, at escalation threshold, mobilises the police.
     *
     * @param tooltipSystem  used to display the raid alert messages
     * @param npcManager     used to alert police when the raid escalates
     * @param player         the player (needed for police spawn position)
     * @param world          the world (needed for police pathfinding)
     */
    public void onGreggBlockBroken(TooltipSystem tooltipSystem,
                                   NPCManager npcManager,
                                   Player player,
                                   World world) {
        blocksBroken++;

        if (blocksBroken == 1 && level == RaidLevel.NONE) {
            level = RaidLevel.ALERT;
            tooltipSystem.trigger(TooltipTrigger.GREGGS_RAID_ALERT);
        }

        if (blocksBroken >= RAID_ESCALATION_THRESHOLD && level != RaidLevel.ESCALATED) {
            level = RaidLevel.ESCALATED;
            tooltipSystem.trigger(TooltipTrigger.GREGGS_RAID_ESCALATION);
            npcManager.alertPoliceToGreggRaid(player, world);
        }
    }

    /**
     * Get the current raid escalation level.
     */
    public RaidLevel getLevel() {
        return level;
    }

    /**
     * Get the total number of Greggs blocks broken in this raid.
     */
    public int getBlocksBroken() {
        return blocksBroken;
    }

    /**
     * Whether the raid has escalated to a full police response.
     */
    public boolean isEscalated() {
        return level == RaidLevel.ESCALATED;
    }

    /**
     * Whether the police have been alerted (any level above NONE).
     */
    public boolean isPoliceAlerted() {
        return level != RaidLevel.NONE;
    }

    /**
     * Reset the raid state — called when the player is arrested or respawns.
     * The police have dealt with the crime; slate wiped clean.
     */
    public void reset() {
        blocksBroken = 0;
        level = RaidLevel.NONE;
    }
}

package ragamuffin.ui;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages tooltips that appear during gameplay.
 * Each tooltip trigger is shown only once per game session.
 */
public class TooltipSystem {
    private final Set<TooltipTrigger> shownTooltips;
    private String currentTooltip;

    public TooltipSystem() {
        this.shownTooltips = new HashSet<>();
        this.currentTooltip = null;
    }

    /**
     * Attempt to trigger a tooltip.
     * @return true if the tooltip was triggered (first time), false if already shown
     */
    public boolean trigger(TooltipTrigger trigger) {
        if (shownTooltips.contains(trigger)) {
            return false; // Already shown
        }

        shownTooltips.add(trigger);
        currentTooltip = trigger.getMessage();
        return true;
    }

    /**
     * Get the currently active tooltip message.
     * @return the tooltip message, or null if no tooltip is active
     */
    public String getCurrentTooltip() {
        return currentTooltip;
    }

    /**
     * Clear the current tooltip.
     */
    public void clearCurrent() {
        currentTooltip = null;
    }

    /**
     * Check if a tooltip has been shown before.
     */
    public boolean hasShown(TooltipTrigger trigger) {
        return shownTooltips.contains(trigger);
    }

    /**
     * Check if there is an active tooltip.
     */
    public boolean isActive() {
        return currentTooltip != null;
    }

    /**
     * Reset all tooltips (for testing or new game).
     */
    public void reset() {
        shownTooltips.clear();
        currentTooltip = null;
    }
}

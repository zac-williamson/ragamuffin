package ragamuffin.ui;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Manages tooltips that appear during gameplay.
 * Each tooltip trigger is shown only once per game session.
 * Tooltips are queued and displayed in order.
 */
public class TooltipSystem {
    private static final float TOOLTIP_DISPLAY_TIME = 3.0f; // 3 seconds = 60 frames minimum

    private final Set<TooltipTrigger> shownTooltips;
    private final Queue<String> tooltipQueue;
    private String currentTooltip;
    private float currentDisplayTime;

    public TooltipSystem() {
        this.shownTooltips = new HashSet<>();
        this.tooltipQueue = new LinkedList<>();
        this.currentTooltip = null;
        this.currentDisplayTime = 0;
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
        tooltipQueue.add(trigger.getMessage());
        return true;
    }

    /**
     * Update the tooltip system (call every frame).
     */
    public void update(float delta) {
        if (currentTooltip != null) {
            currentDisplayTime += delta;
            if (currentDisplayTime >= TOOLTIP_DISPLAY_TIME) {
                // Current tooltip finished displaying, move to next
                currentTooltip = null;
                currentDisplayTime = 0;
            }
        }

        // If no current tooltip and queue has messages, show next
        if (currentTooltip == null && !tooltipQueue.isEmpty()) {
            currentTooltip = tooltipQueue.poll();
            currentDisplayTime = 0;
        }
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
        currentDisplayTime = 0;
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
     * Get the number of tooltips in the queue.
     */
    public int getQueueSize() {
        return tooltipQueue.size();
    }

    /**
     * Get the current display time of the active tooltip.
     */
    public float getCurrentDisplayTime() {
        return currentDisplayTime;
    }

    /**
     * Reset all tooltips (for testing or new game).
     */
    public void reset() {
        shownTooltips.clear();
        tooltipQueue.clear();
        currentTooltip = null;
        currentDisplayTime = 0;
    }
}

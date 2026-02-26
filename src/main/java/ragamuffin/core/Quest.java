package ragamuffin.core;

import ragamuffin.building.Material;
import ragamuffin.world.LandmarkType;

/**
 * Represents a quest offered by a building NPC.
 * Quests are thematically tied to their building and offer emergent storytelling.
 */
public class Quest {

    public enum ObjectiveType {
        COLLECT,     // Gather a specific material
        DELIVER,     // Take an item to a location
        EXPLORE      // Visit a location or landmark
    }

    private final String id;
    private final String giver;          // NPC / building name
    private final String description;   // What the player is asked to do
    private final ObjectiveType type;
    private final Material requiredMaterial; // For COLLECT/DELIVER quests, null otherwise
    private final int requiredCount;         // How many items needed (1 for non-collect)
    private final LandmarkType targetLandmark; // For EXPLORE/DELIVER quests, null otherwise
    private final Material reward;           // Item reward on completion
    private final int rewardCount;
    private boolean completed;
    private boolean active;
    /** For EXPLORE quests: whether the player has visited the target landmark. */
    private boolean locationVisited;

    public Quest(String id, String giver, String description,
                 ObjectiveType type, Material requiredMaterial, int requiredCount,
                 Material reward, int rewardCount) {
        this(id, giver, description, type, requiredMaterial, requiredCount, null, reward, rewardCount);
    }

    public Quest(String id, String giver, String description,
                 ObjectiveType type, Material requiredMaterial, int requiredCount,
                 LandmarkType targetLandmark, Material reward, int rewardCount) {
        this.id = id;
        this.giver = giver;
        this.description = description;
        this.type = type;
        this.requiredMaterial = requiredMaterial;
        this.requiredCount = requiredCount;
        this.targetLandmark = targetLandmark;
        this.reward = reward;
        this.rewardCount = rewardCount;
        this.completed = false;
        this.active = false;
        this.locationVisited = false;
    }

    public String getId() { return id; }
    public String getGiver() { return giver; }
    public String getDescription() { return description; }
    public ObjectiveType getType() { return type; }
    public Material getRequiredMaterial() { return requiredMaterial; }
    public int getRequiredCount() { return requiredCount; }
    public LandmarkType getTargetLandmark() { return targetLandmark; }
    public Material getReward() { return reward; }
    public int getRewardCount() { return rewardCount; }
    public boolean isCompleted() { return completed; }
    public boolean isActive() { return active; }
    public boolean isLocationVisited() { return locationVisited; }

    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Mark the target landmark as visited. Used by EXPLORE quests.
     * Has no effect on other quest types.
     */
    public void markLocationVisited() {
        this.locationVisited = true;
    }

    /**
     * Check if the quest's objective is satisfied.
     * For EXPLORE quests, checks whether the target location has been visited
     * (inventory is not needed and may be null).
     * For COLLECT/DELIVER quests, checks the player's inventory.
     */
    public boolean checkCompletion(ragamuffin.building.Inventory inventory) {
        if (completed) return true;
        switch (type) {
            case COLLECT:
                if (requiredMaterial != null && inventory != null) {
                    return inventory.getItemCount(requiredMaterial) >= requiredCount;
                }
                return false;
            case DELIVER:
                // DELIVER requires the player to have the items AND be at the target location.
                // Completion is triggered externally (location check); here just verify items.
                if (requiredMaterial != null && inventory != null) {
                    return inventory.getItemCount(requiredMaterial) >= requiredCount;
                }
                return false;
            case EXPLORE:
                return locationVisited;
            default:
                return false;
        }
    }

    /**
     * Complete the quest: remove required items (for COLLECT/DELIVER), award reward.
     * For EXPLORE quests, inventory may be null (no items are consumed or rewarded without one).
     * Returns true if completion was successful.
     */
    public boolean complete(ragamuffin.building.Inventory inventory) {
        if (completed) return false;
        switch (type) {
            case COLLECT:
            case DELIVER:
                if (requiredMaterial != null) {
                    if (inventory == null) return false;
                    if (inventory.getItemCount(requiredMaterial) < requiredCount) return false;
                    inventory.removeItem(requiredMaterial, requiredCount);
                }
                break;
            case EXPLORE:
                if (!locationVisited) return false;
                break;
            default:
                break;
        }
        if (reward != null && inventory != null) {
            inventory.addItem(reward, rewardCount);
        }
        completed = true;
        active = false;
        return true;
    }
}

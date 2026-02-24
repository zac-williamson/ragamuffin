package ragamuffin.core;

import ragamuffin.building.Material;

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
    private final Material requiredMaterial; // For COLLECT quests, null otherwise
    private final int requiredCount;         // How many items needed (1 for non-collect)
    private final Material reward;           // Item reward on completion
    private final int rewardCount;
    private boolean completed;
    private boolean active;

    public Quest(String id, String giver, String description,
                 ObjectiveType type, Material requiredMaterial, int requiredCount,
                 Material reward, int rewardCount) {
        this.id = id;
        this.giver = giver;
        this.description = description;
        this.type = type;
        this.requiredMaterial = requiredMaterial;
        this.requiredCount = requiredCount;
        this.reward = reward;
        this.rewardCount = rewardCount;
        this.completed = false;
        this.active = false;
    }

    public String getId() { return id; }
    public String getGiver() { return giver; }
    public String getDescription() { return description; }
    public ObjectiveType getType() { return type; }
    public Material getRequiredMaterial() { return requiredMaterial; }
    public int getRequiredCount() { return requiredCount; }
    public Material getReward() { return reward; }
    public int getRewardCount() { return rewardCount; }
    public boolean isCompleted() { return completed; }
    public boolean isActive() { return active; }

    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Check if the player's inventory satisfies this quest's objective.
     */
    public boolean checkCompletion(ragamuffin.building.Inventory inventory) {
        if (completed) return true;
        if (type == ObjectiveType.COLLECT && requiredMaterial != null) {
            return inventory.getItemCount(requiredMaterial) >= requiredCount;
        }
        return false;
    }

    /**
     * Complete the quest: remove required items, award reward.
     * Returns true if completion was successful.
     */
    public boolean complete(ragamuffin.building.Inventory inventory) {
        if (completed) return false;
        if (type == ObjectiveType.COLLECT && requiredMaterial != null) {
            if (inventory.getItemCount(requiredMaterial) < requiredCount) return false;
            inventory.removeItem(requiredMaterial, requiredCount);
        }
        if (reward != null) {
            inventory.addItem(reward, rewardCount);
        }
        completed = true;
        active = false;
        return true;
    }
}

package ragamuffin.building;

import java.util.HashMap;
import java.util.Map;

/**
 * Player inventory system with slots and item stacking.
 */
public class Inventory {
    private static final int MAX_STACK_SIZE = 99;

    private final int size;
    private final InventorySlot[] slots;

    /** Default inventory size (36 slots — same as the main game inventory). */
    public static final int DEFAULT_SIZE = 36;

    /** Create an inventory with the default size of {@value #DEFAULT_SIZE} slots. */
    public Inventory() {
        this(DEFAULT_SIZE);
    }

    public Inventory(int size) {
        this.size = size;
        this.slots = new InventorySlot[size];
        for (int i = 0; i < size; i++) {
            slots[i] = new InventorySlot();
        }
    }

    /**
     * Add an item to the inventory. Stacks with existing items of the same type.
     * @return true if the item was added successfully
     */
    public boolean addItem(Material material, int count) {
        if (material == null || count <= 0) {
            return false;
        }

        int remaining = count;

        // First, try to stack with existing slots
        for (int i = 0; i < size; i++) {
            if (slots[i].getMaterial() == material) {
                int space = MAX_STACK_SIZE - slots[i].getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, remaining);
                    slots[i].setCount(slots[i].getCount() + toAdd);
                    remaining -= toAdd;
                    if (remaining == 0) {
                        return true;
                    }
                }
            }
        }

        // Then, find empty slots
        for (int i = 0; i < size; i++) {
            if (slots[i].isEmpty()) {
                int toAdd = Math.min(MAX_STACK_SIZE, remaining);
                slots[i].setMaterial(material);
                slots[i].setCount(toAdd);
                remaining -= toAdd;
                if (remaining == 0) {
                    return true;
                }
            }
        }

        // If we still have items remaining, inventory is full
        return remaining == 0;
    }

    /**
     * Remove an item from the inventory.
     * @return true if the full count was removed
     */
    public boolean removeItem(Material material, int count) {
        if (material == null || count <= 0) {
            return false;
        }

        // Check if we have enough
        if (!hasItem(material, count)) {
            return false;
        }

        int remaining = count;

        // Remove from slots
        for (int i = 0; i < size && remaining > 0; i++) {
            if (slots[i].getMaterial() == material) {
                int inSlot = slots[i].getCount();
                if (inSlot <= remaining) {
                    // Clear this slot entirely
                    remaining -= inSlot;
                    slots[i].clear();
                } else {
                    // Partial removal
                    slots[i].setCount(inSlot - remaining);
                    remaining = 0;
                }
            }
        }

        return true;
    }

    /**
     * Check if the inventory has at least one of the given material.
     */
    public boolean hasItem(Material material) {
        return hasItem(material, 1);
    }

    /**
     * Check if the inventory has at least the given count of the material.
     */
    public boolean hasItem(Material material, int count) {
        return getItemCount(material) >= count;
    }

    /**
     * Get the total count of a material in the inventory.
     */
    public int getItemCount(Material material) {
        int total = 0;
        for (int i = 0; i < size; i++) {
            if (slots[i].getMaterial() == material) {
                total += slots[i].getCount();
            }
        }
        return total;
    }

    /**
     * Clear all items from the inventory.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            slots[i].clear();
        }
    }

    /**
     * Find the first slot containing the given material.
     * @return slot index, or -1 if not found
     */
    public int findSlotWithItem(Material material) {
        for (int i = 0; i < size; i++) {
            if (slots[i].getMaterial() == material) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the material in a specific slot.
     * @return the material, or null if the slot is empty
     */
    public Material getItemInSlot(int slot) {
        if (slot < 0 || slot >= size) {
            return null;
        }
        return slots[slot].getMaterial();
    }

    /**
     * Get the count of items in a specific slot.
     */
    public int getCountInSlot(int slot) {
        if (slot < 0 || slot >= size) {
            return 0;
        }
        return slots[slot].getCount();
    }

    /**
     * Get the size of the inventory (number of slots).
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the tool in a specific slot, or null if no tool is stored.
     */
    public Tool getToolInSlot(int slot) {
        if (slot < 0 || slot >= size) {
            return null;
        }
        return slots[slot].getTool();
    }

    /**
     * Set the tool in a specific slot.
     */
    public void setToolInSlot(int slot, Tool tool) {
        if (slot >= 0 && slot < size) {
            slots[slot].setTool(tool);
        }
    }

    /**
     * Swap the contents of two inventory slots.
     */
    public void swapSlots(int slotA, int slotB) {
        if (slotA < 0 || slotA >= size || slotB < 0 || slotB >= size || slotA == slotB) {
            return;
        }
        Material tempMat = slots[slotA].getMaterial();
        int tempCount = slots[slotA].getCount();
        Tool tempTool = slots[slotA].getTool();
        slots[slotA].setMaterial(slots[slotB].getMaterial());
        slots[slotA].setCount(slots[slotB].getCount());
        slots[slotA].setTool(slots[slotB].getTool());
        if (slots[slotA].getMaterial() == null) {
            slots[slotA].clear();
        }
        slots[slotB].setMaterial(tempMat);
        slots[slotB].setCount(tempCount);
        slots[slotB].setTool(tempTool);
        if (slots[slotB].getMaterial() == null) {
            slots[slotB].clear();
        }
    }

    /**
     * Inner class representing a single inventory slot.
     */
    private static class InventorySlot {
        private Material material;
        private int count;
        private Tool tool; // Non-null when holding a tool item

        public InventorySlot() {
            this.material = null;
            this.count = 0;
            this.tool = null;
        }

        public Material getMaterial() {
            return material;
        }

        public void setMaterial(Material material) {
            this.material = material;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Tool getTool() {
            return tool;
        }

        public void setTool(Tool tool) {
            this.tool = tool;
        }

        public boolean isEmpty() {
            return material == null || count == 0;
        }

        public void clear() {
            material = null;
            count = 0;
            tool = null;
        }
    }
}

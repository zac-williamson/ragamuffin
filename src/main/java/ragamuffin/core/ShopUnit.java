package ragamuffin.core;

import ragamuffin.building.Material;
import ragamuffin.entity.NPC;

import java.util.EnumMap;
import java.util.Map;

/**
 * Issue #799: The Corner Shop Economy — ShopUnit data class.
 *
 * <p>Tracks the state of a single player-owned shop unit. Created when the
 * player claims a derelict building (or uses a {@link Material#SHOP_KEY}).
 *
 * <h3>Stock and Pricing</h3>
 * <ul>
 *   <li>{@code stockMap} — maps material to quantity in stock</li>
 *   <li>{@code priceMap} — maps material to player-set asking price (in COIN)</li>
 * </ul>
 *
 * <h3>Finance</h3>
 * <ul>
 *   <li>{@code cashRegister} — coins accumulated since last collection</li>
 *   <li>{@code dailyRevenue} — total coins earned today (resets each in-game day)</li>
 * </ul>
 *
 * <h3>Heat</h3>
 * <ul>
 *   <li>{@code heatLevel} — 0 to 100. Rises from dodgy sales, runner spotted,
 *       newspaper mentions. Thresholds: 30 = Inspection Notice, 60 = Undercover
 *       Police stakeout, 80 = Raid Warning rumour, 100 = Police Raid.</li>
 * </ul>
 *
 * <h3>Runner</h3>
 * <ul>
 *   <li>{@code runnerNpc} — reference to the hired RUNNER NPC, or {@code null} if none.</li>
 * </ul>
 */
public class ShopUnit {

    // ── Stock and pricing ─────────────────────────────────────────────────────

    /** Current stock quantities per material. */
    private final Map<Material, Integer> stockMap = new EnumMap<>(Material.class);

    /** Player-set asking prices per material (in COIN). */
    private final Map<Material, Integer> priceMap = new EnumMap<>(Material.class);

    // ── Finance ───────────────────────────────────────────────────────────────

    /** Coins in the till, ready to be collected by the player. */
    private int cashRegister = 0;

    /** Coins earned today (reset at start of each in-game day). */
    private int dailyRevenue = 0;

    // ── Heat ──────────────────────────────────────────────────────────────────

    /**
     * Shop Heat — 0 (clean) to 100 (raided).
     * Rises from dodgy sales, runner spotted by police, newspaper mentions.
     */
    private int heatLevel = 0;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the shop is currently open for business. */
    private boolean open = false;

    /** Whether the shop has been claimed (one-time setup). */
    private boolean claimed = false;

    /** Reference to the hired runner NPC, or null if none. */
    private NPC runnerNpc = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public ShopUnit() {
    }

    // ── Stock management ──────────────────────────────────────────────────────

    /**
     * Add a quantity of a material to stock.
     *
     * @param material the material to stock
     * @param quantity amount to add (must be > 0)
     */
    public void addStock(Material material, int quantity) {
        if (quantity <= 0) return;
        stockMap.merge(material, quantity, Integer::sum);
    }

    /**
     * Remove a quantity of a material from stock.
     *
     * @param material the material to remove
     * @param quantity amount to remove
     * @return true if sufficient stock was available and removed; false otherwise
     */
    public boolean removeStock(Material material, int quantity) {
        int current = stockMap.getOrDefault(material, 0);
        if (current < quantity) return false;
        int remaining = current - quantity;
        if (remaining == 0) {
            stockMap.remove(material);
        } else {
            stockMap.put(material, remaining);
        }
        return true;
    }

    /**
     * Returns the quantity of the given material in stock.
     */
    public int getStock(Material material) {
        return stockMap.getOrDefault(material, 0);
    }

    /**
     * Returns a read-only view of the stock map.
     */
    public Map<Material, Integer> getStockMap() {
        return java.util.Collections.unmodifiableMap(stockMap);
    }

    // ── Pricing ───────────────────────────────────────────────────────────────

    /**
     * Set the asking price for a material.
     *
     * @param material the material
     * @param price    asking price in COIN (must be ≥ 0)
     */
    public void setPrice(Material material, int price) {
        if (price < 0) price = 0;
        priceMap.put(material, price);
    }

    /**
     * Get the player-set asking price for a material. Returns 0 if not set.
     */
    public int getPrice(Material material) {
        return priceMap.getOrDefault(material, 0);
    }

    /**
     * Returns a read-only view of the price map.
     */
    public Map<Material, Integer> getPriceMap() {
        return java.util.Collections.unmodifiableMap(priceMap);
    }

    // ── Finance ───────────────────────────────────────────────────────────────

    /**
     * Add coins to the cash register (called when a customer buys something).
     */
    public void addCash(int amount) {
        if (amount <= 0) return;
        cashRegister += amount;
        dailyRevenue += amount;
    }

    /**
     * Collect all cash from the register (player picks up the money).
     *
     * @return the amount collected
     */
    public int collectCash() {
        int collected = cashRegister;
        cashRegister = 0;
        return collected;
    }

    /**
     * Reset the daily revenue counter (call at the start of each in-game day).
     */
    public void resetDailyRevenue() {
        dailyRevenue = 0;
    }

    public int getCashRegister() {
        return cashRegister;
    }

    public int getDailyRevenue() {
        return dailyRevenue;
    }

    // ── Heat ──────────────────────────────────────────────────────────────────

    /**
     * Add heat to the shop. Clamped to [0, 100].
     */
    public void addHeat(int amount) {
        heatLevel = Math.min(100, heatLevel + amount);
    }

    /**
     * Reduce heat. Clamped to 0.
     */
    public void reduceHeat(int amount) {
        heatLevel = Math.max(0, heatLevel - amount);
    }

    public int getHeatLevel() {
        return heatLevel;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public NPC getRunnerNpc() {
        return runnerNpc;
    }

    public void setRunnerNpc(NPC runnerNpc) {
        this.runnerNpc = runnerNpc;
    }

    public boolean hasRunner() {
        return runnerNpc != null;
    }
}

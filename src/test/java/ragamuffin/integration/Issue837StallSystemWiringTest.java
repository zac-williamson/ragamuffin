package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.FactionSystem;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.StallSystem;
import ragamuffin.core.StreetEconomySystem;
import ragamuffin.core.TurfMap;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #837 — Wire StallSystem into the game loop.
 *
 * <p>5 scenarios:
 * <ol>
 *   <li>StallSystem instantiation and single PLAYING frame produces no exception</li>
 *   <li>placeStall on PAVEMENT succeeds</li>
 *   <li>placeStall on non-valid surface fails</li>
 *   <li>RAIN without awning closes stall and destroys stock</li>
 *   <li>Unlicensed trading spawns MARKET_INSPECTOR after delay</li>
 * </ol>
 */
class Issue837StallSystemWiringTest {

    private StallSystem stallSystem;
    private Inventory inventory;
    private Player player;
    private List<NPC> npcs;
    private FactionSystem factionSystem;
    private NotorietySystem notorietySystem;
    private StreetEconomySystem streetEconomySystem;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private CriminalRecord criminalRecord;

    @BeforeEach
    void setUp() {
        stallSystem = new StallSystem(new Random(42L));
        inventory = new Inventory(36);
        player = new Player(10f, 1f, 10f);
        npcs = new ArrayList<>();
        TurfMap turfMap = new TurfMap();
        factionSystem = new FactionSystem(turfMap, new ragamuffin.core.RumourNetwork(new Random(42L)));
        notorietySystem = new NotorietySystem();
        streetEconomySystem = new StreetEconomySystem(new Random(42L));
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        criminalRecord = new CriminalRecord();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: StallSystem instantiated and single PLAYING frame is safe
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Construct a StallSystem. Verify the field is non-null. Simulate
     * one frame of update() with no stall placed. Verify no exception is thrown.
     */
    @Test
    void stallSystemInstantiatedAndUpdateIsNoop() {
        assertNotNull(stallSystem, "StallSystem must be non-null after construction");

        // Simulate one PLAYING frame with no stall placed — should be a no-op
        assertDoesNotThrow(() ->
            stallSystem.update(
                0.016f,
                npcs,
                player,
                Weather.CLEAR,
                inventory,
                factionSystem,
                notorietySystem,
                criminalRecord,
                achievementCallback,
                streetEconomySystem
            )
        );

        assertFalse(stallSystem.isStallPlaced(), "Stall should not be placed after a bare update");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: placeStall on PAVEMENT succeeds
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Add a STALL_FRAME to the player's inventory. Call placeStall with
     * groundBlockType "PAVEMENT". Verify it returns true and isStallPlaced() is true.
     */
    @Test
    void placeStallOnPavementSucceeds() {
        inventory.addItem(Material.STALL_FRAME, 1);

        boolean placed = stallSystem.placeStall(5, 1, 5, "PAVEMENT", inventory, null);

        assertTrue(placed, "placeStall on PAVEMENT should return true");
        assertTrue(stallSystem.isStallPlaced(), "isStallPlaced() should be true after successful placement");
        // STALL_FRAME should have been consumed from inventory
        assertEquals(0, inventory.getItemCount(Material.STALL_FRAME),
                "STALL_FRAME should be consumed from inventory on placement");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: placeStall on non-valid surface fails
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Add a STALL_FRAME to inventory. Call placeStall with groundBlockType
     * "GRASS". Verify it returns false and isStallPlaced() is false.
     */
    @Test
    void placeStallOnGrassFails() {
        inventory.addItem(Material.STALL_FRAME, 1);

        boolean placed = stallSystem.placeStall(5, 1, 5, "GRASS", inventory, null);

        assertFalse(placed, "placeStall on GRASS should return false");
        assertFalse(stallSystem.isStallPlaced(), "isStallPlaced() should remain false on invalid placement");
        // STALL_FRAME should NOT have been consumed
        assertEquals(1, inventory.getItemCount(Material.STALL_FRAME),
                "STALL_FRAME should not be consumed when placement fails");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: RAIN without awning closes stall and destroys stock
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Place and open the stall. Add 2 items to stock. Call update with
     * Weather.RAIN (no awning attached). Verify isStallOpen() is false and no stock
     * remains.
     */
    @Test
    void rainWithoutAwningClosesStallAndDestroysStock() {
        inventory.addItem(Material.STALL_FRAME, 1);
        stallSystem.placeStall(5, 1, 5, "PAVEMENT", inventory, null);
        stallSystem.openStall();
        stallSystem.setStockSlot(0, Material.WOOD, 2, 3);
        stallSystem.setStockSlot(1, Material.BRICK, 1, 5);

        assertTrue(stallSystem.isStallOpen(), "Stall should be open before rain update");

        stallSystem.update(
            0.1f,
            npcs,
            player,
            Weather.RAIN,
            inventory,
            factionSystem,
            notorietySystem,
            criminalRecord,
            achievementCallback,
            streetEconomySystem
        );

        assertFalse(stallSystem.isStallOpen(), "RAIN without awning should close the stall");
        assertNull(stallSystem.getStockSlot(0), "Stock slot 0 should be destroyed by rain");
        assertNull(stallSystem.getStockSlot(1), "Stock slot 1 should be destroyed by rain");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Unlicensed trading spawns MARKET_INSPECTOR after delay
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Place and open a stall with no MARKET_LICENCE. Call update with a
     * delta exceeding UNLICENSED_INSPECTOR_DELAY. Verify at least one NPC with type
     * MARKET_INSPECTOR has been spawned into the npcs list.
     */
    @Test
    void unlicensedTradingSpawnsInspectorAfterDelay() {
        inventory.addItem(Material.STALL_FRAME, 1);
        stallSystem.placeStall(5, 1, 5, "PAVEMENT", inventory, null);
        stallSystem.openStall();

        assertFalse(stallSystem.isHasLicence(), "Stall should be unlicensed");

        float bigDelta = StallSystem.UNLICENSED_INSPECTOR_DELAY + 1f;
        stallSystem.update(
            bigDelta,
            npcs,
            player,
            Weather.CLEAR,
            inventory,
            factionSystem,
            notorietySystem,
            criminalRecord,
            achievementCallback,
            streetEconomySystem
        );

        boolean inspectorSpawned = npcs.stream()
                .anyMatch(npc -> npc.getType() == NPCType.MARKET_INSPECTOR);
        assertTrue(inspectorSpawned,
                "A MARKET_INSPECTOR NPC should be spawned after unlicensed trading delay");
        assertTrue(stallSystem.isInspectorActive(),
                "StallSystem should report inspector as active");
    }
}

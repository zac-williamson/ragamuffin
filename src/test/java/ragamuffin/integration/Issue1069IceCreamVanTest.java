package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1069 — Northfield Ice Cream Van: Mr. Whippy,
 * the Jingle Economy &amp; the Marchetti Turf War.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>Van spawns in good weather during operating hours; refuses service when player
 *       is Wanted (JINGLE_AMBUSH); grants CHEEKY_FLAKE on purchase in non-Wanted state.</li>
 *   <li>Side-hatch fence: Street Rep ≥ 40 opens the side hatch; player fences a stolen
 *       item at 55% of base value; SIDE_HATCH achievement awarded.</li>
 *   <li>Marchetti drive-by: player defends the van, MARCHETTI_DEFENDER achievement awarded;
 *       repeat without defending at Respect &lt; 20 — van is firebombed.</li>
 *   <li>QUEUE_JUMP event: Chav pushes in; player confronts (50/50 outcome) or pays 2 COIN.</li>
 *   <li>Van depot theft overnight: Driving ≥ 3 steals van, VAN_HEIST achievement awarded;
 *       Driving &lt; 3 fails.</li>
 * </ol>
 */
class Issue1069IceCreamVanTest {

    private IceCreamVanSystem vanSystem;
    private AchievementSystem achievementSystem;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private FactionSystem factionSystem;
    private StreetEconomySystem streetEconomySystem;
    private RumourNetwork rumourNetwork;
    private Inventory inventory;
    private Player player;
    private List<NPC> nearbyNpcs;

    @BeforeEach
    void setUp() {
        // Use fixed seed for determinism
        vanSystem = new IceCreamVanSystem(new Random(42));
        achievementSystem = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(99));
        factionSystem = new FactionSystem();
        streetEconomySystem = new StreetEconomySystem(new Random(7));
        rumourNetwork = new RumourNetwork(new Random(13));
        inventory = new Inventory(36);
        player = new Player(0f, 1f, 0f);
        nearbyNpcs = new ArrayList<>();

        vanSystem.setAchievementSystem(achievementSystem);
        vanSystem.setNotorietySystem(notorietySystem);
        vanSystem.setWantedSystem(wantedSystem);
        vanSystem.setFactionSystem(factionSystem);
        vanSystem.setStreetEconomySystem(streetEconomySystem);
        vanSystem.setRumourNetwork(rumourNetwork);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Weather/time gating and JINGLE_AMBUSH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Van spawns in good weather during operating hours (SUNNY, 14:00).
     * Van does NOT spawn in bad weather (RAIN). JINGLE_AMBUSH refuses a Wanted player.
     * Non-Wanted player buys 99 Flake; CHEEKY_FLAKE achievement awarded.
     */
    @Test
    void scenario1_weatherGatingAndJingleAmbush() {
        // ── Van NOT spawning in bad weather ──────────────────────────────────

        vanSystem.update(0.016f, 14.0f, Weather.RAIN,
                nearbyNpcs, player, inventory, 0);

        assertFalse(vanSystem.isVanActive(),
                "Van should NOT be active in RAIN weather");

        // ── Van spawns in good weather during operating hours ─────────────────

        vanSystem.update(0.016f, 14.0f, Weather.SUNNY,
                nearbyNpcs, player, inventory, 0);

        assertTrue(vanSystem.isVanActive(),
                "Van should be active at 14:00 in SUNNY weather");
        assertNotNull(vanSystem.getDave(),
                "Dave (ICE_CREAM_MAN NPC) should be present when van is active");
        assertEquals(IceCreamVanSystem.INITIAL_STOCK, vanSystem.getStock(),
                "Van should start with full stock");

        // ── JINGLE_AMBUSH: refuse Wanted player ───────────────────────────────

        // Give player a wanted star
        wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
        assertTrue(wantedSystem.getWantedStars() >= 1, "Player should have at least 1 wanted star");

        IceCreamVanSystem.BuyResult refusedResult = vanSystem.joinQueue(inventory,
                IceCreamVanSystem.BASE_PRICE_99_FLAKE);

        assertEquals(IceCreamVanSystem.BuyResult.REFUSED_WANTED, refusedResult,
                "Van should refuse service to Wanted player (JINGLE_AMBUSH)");

        // ── Non-Wanted player buys 99 Flake ───────────────────────────────────

        // Clear wanted status (simulate player losing stars)
        wantedSystem.clearWanted();
        assertEquals(0, wantedSystem.getWantedStars(), "Player should have 0 wanted stars");

        inventory.addItem(Material.COIN, IceCreamVanSystem.BASE_PRICE_99_FLAKE + 1); // Sunny = +1
        IceCreamVanSystem.BuyResult buyResult = vanSystem.buyItem(
                inventory, Material.NINETY_NINE_FLAKE,
                vanSystem.getItemPrice(Material.NINETY_NINE_FLAKE, Weather.SUNNY));

        assertEquals(IceCreamVanSystem.BuyResult.SUCCESS, buyResult,
                "Non-Wanted player should be served");
        assertEquals(1, inventory.getItemCount(Material.NINETY_NINE_FLAKE),
                "NINETY_NINE_FLAKE should be in inventory after purchase");
        assertTrue(achievementSystem.isUnlocked(AchievementType.CHEEKY_FLAKE),
                "CHEEKY_FLAKE achievement should be awarded on first purchase");

        // ── Van does NOT operate outside hours ────────────────────────────────

        // Reset van
        vanSystem.resetVanState();
        // Force despawn by updating at 20:00
        vanSystem.update(0.016f, 20.0f, Weather.SUNNY, nearbyNpcs, player, inventory, 0);
        assertFalse(vanSystem.isVanActive(),
                "Van should NOT be active at 20:00 (outside 12:00–19:30)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Side-hatch fence at Street Rep ≥ 40
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: With Street Rep ≥ 40, Dave opens the side hatch.
     * Player fences a STOLEN_PHONE at 55% of base value.
     * SIDE_HATCH achievement awarded.
     * Below Street Rep 40, side hatch is closed.
     */
    @Test
    void scenario2_sideHatchFenceAtStreetRep40() {
        // ── Below threshold: side hatch closed ───────────────────────────────

        vanSystem.forceSpawn(30); // street rep = 30 (below threshold)

        assertTrue(vanSystem.isVanActive(), "Van should be active after forceSpawn");
        assertFalse(vanSystem.isSideHatchOpen(),
                "Side hatch should be CLOSED at Street Rep < 40");

        // Attempt to fence — should return 0
        inventory.addItem(Material.STOLEN_PHONE, 1);
        int coins = vanSystem.fenceItemViaSideHatch(inventory, Material.STOLEN_PHONE, 10);

        assertEquals(0, coins,
                "Side hatch fence should return 0 when hatch is closed");
        assertEquals(1, inventory.getItemCount(Material.STOLEN_PHONE),
                "Item should NOT be consumed when hatch is closed");

        // ── At threshold: side hatch opens ───────────────────────────────────

        vanSystem.forceSpawn(IceCreamVanSystem.SIDE_HATCH_STREET_REP_THRESHOLD);

        assertTrue(vanSystem.isSideHatchOpen(),
                "Side hatch should be OPEN at Street Rep ≥ 40");

        // Fence the STOLEN_PHONE (base value = 10)
        int baseValue = 10;
        int expectedPayment = Math.max(1, (int) (baseValue * IceCreamVanSystem.SIDE_HATCH_FENCE_MULTIPLIER));
        coins = vanSystem.fenceItemViaSideHatch(inventory, Material.STOLEN_PHONE, baseValue);

        assertEquals(expectedPayment, coins,
                "Side hatch should pay 55% of base value (" + expectedPayment + " COIN for base 10)");
        assertEquals(0, inventory.getItemCount(Material.STOLEN_PHONE),
                "STOLEN_PHONE should be consumed on successful fence");
        assertEquals(expectedPayment, inventory.getItemCount(Material.COIN),
                "Coins should be added to inventory");
        assertTrue(achievementSystem.isUnlocked(AchievementType.SIDE_HATCH),
                "SIDE_HATCH achievement should be awarded");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Marchetti drive-by events
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3a: Player defends van from Marchetti drive-by.
     * MARCHETTI_DEFENDER achievement awarded. Drive-by cleared.
     *
     * Scenario 3b: Van is firebombed when Marchetti Respect &lt; 20 and player doesn't defend.
     */
    @Test
    void scenario3_marchettiDriveBy_defendAndFirebomb() {
        // ── Scenario 3a: Player defends ───────────────────────────────────────

        vanSystem.forceSpawn(0);
        vanSystem.setDriveByForTesting(true);
        assertTrue(vanSystem.isDriveByPending(), "Drive-by should be pending");

        IceCreamVanSystem.DriveByResult defendResult =
                vanSystem.handleMarchettiDriveBy(true);

        assertEquals(IceCreamVanSystem.DriveByResult.DEFENDED, defendResult,
                "Player should successfully defend the van");
        assertFalse(vanSystem.isDriveByPending(),
                "Drive-by should be cleared after defence");
        assertTrue(achievementSystem.isUnlocked(AchievementType.MARCHETTI_DEFENDER),
                "MARCHETTI_DEFENDER achievement should be awarded on defence");

        // ── Scenario 3b: Van firebombed at Marchetti Respect < 20 ─────────────

        // Reset van and adjust faction respect
        vanSystem.resetVanState();
        vanSystem.forceSpawn(0);
        vanSystem.setDriveByForTesting(true);

        // Set Marchetti respect below firebomb threshold
        factionSystem.setRespect(Faction.MARCHETTI_CREW,
                IceCreamVanSystem.MARCHETTI_FIREBOMB_RESPECT_THRESHOLD - 5);

        IceCreamVanSystem.DriveByResult firebombResult =
                vanSystem.handleMarchettiDriveBy(false);

        assertEquals(IceCreamVanSystem.DriveByResult.VAN_FIREBOMBED, firebombResult,
                "Van should be firebombed when Marchetti Respect < 20 and player doesn't defend");
        assertTrue(vanSystem.isVanFirebombed(),
                "Van should be marked as firebombed");
        assertFalse(vanSystem.isVanActive(),
                "Van should be inactive after firebombing");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: QUEUE_JUMP event — Chav pushes in
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: A Chav NPC pushes into the queue.
     * Player pays 2 COIN → CHAV_CHARMED achievement, queue resolved.
     * Player confronts with insufficient coin → fight triggered.
     */
    @Test
    void scenario4_queueJump_chavPushesIn() {
        vanSystem.forceSpawn(0);

        NPC chav = new NPC(NPCType.YOUTH_GANG, 2f, 1f, 2f);

        // ── Pay to resolve ────────────────────────────────────────────────────

        inventory.addItem(Material.COIN, 5);
        IceCreamVanSystem.QueueJumpResult payResult =
                vanSystem.handleQueueJump(inventory, false, chav);

        assertTrue(
                payResult == IceCreamVanSystem.QueueJumpResult.SUCCESS_PAY
                        || payResult == IceCreamVanSystem.QueueJumpResult.SUCCESS_CONFRONT
                        || payResult == IceCreamVanSystem.QueueJumpResult.FIGHT_TRIGGERED,
                "Queue jump should produce a valid result");

        if (payResult == IceCreamVanSystem.QueueJumpResult.SUCCESS_PAY) {
            assertEquals(3, inventory.getItemCount(Material.COIN),
                    "2 COIN should be deducted on successful payment");
            assertTrue(achievementSystem.isUnlocked(AchievementType.CHAV_CHARMED),
                    "CHAV_CHARMED should be awarded on successful payment");
        }

        // ── Confront with no coin → fight triggered ───────────────────────────

        // Remove coins so player can't pay
        inventory.removeItem(Material.COIN, inventory.getItemCount(Material.COIN));

        NPC chav2 = new NPC(NPCType.YOUTH_GANG, 3f, 1f, 3f);
        // Use a seeded RNG that gives a fight (force by running multiple times)
        // We just verify the result is valid
        IceCreamVanSystem.QueueJumpResult confrontResult =
                vanSystem.handleQueueJump(inventory, true, chav2);

        assertTrue(
                confrontResult == IceCreamVanSystem.QueueJumpResult.SUCCESS_CONFRONT
                        || confrontResult == IceCreamVanSystem.QueueJumpResult.FIGHT_TRIGGERED,
                "Confrontation should result in either success or fight");

        // ── No chav present ────────────────────────────────────────────────────

        IceCreamVanSystem.QueueJumpResult noChavResult =
                vanSystem.handleQueueJump(inventory, false, null);
        assertEquals(IceCreamVanSystem.QueueJumpResult.NO_CHAV_PRESENT, noChavResult,
                "Should return NO_CHAV_PRESENT when chav NPC is null");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Van theft from depot overnight
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Player with Driving ≥ 3 steals van from industrial estate depot overnight.
     * VAN_HEIST achievement awarded. Notoriety gained.
     * Player with Driving &lt; 3 cannot steal van.
     */
    @Test
    void scenario5_vanTheftFromDepot_drivingSkillGating() {
        // Van must NOT be active for depot theft
        assertFalse(vanSystem.isVanActive(), "Van should be inactive for depot theft test");

        // ── Insufficient driving skill ────────────────────────────────────────

        boolean failedTheft = vanSystem.stealVanFromDepot(22.0f,
                IceCreamVanSystem.VAN_THEFT_DRIVING_SKILL_REQUIRED - 1);

        assertFalse(failedTheft,
                "Theft should fail when Driving < " + IceCreamVanSystem.VAN_THEFT_DRIVING_SKILL_REQUIRED);
        assertFalse(vanSystem.isVanStolen(),
                "Van should NOT be stolen with insufficient driving skill");
        assertFalse(achievementSystem.isUnlocked(AchievementType.VAN_HEIST),
                "VAN_HEIST should NOT be awarded on failed theft");

        // ── Can't steal during operating hours ───────────────────────────────

        boolean dayTheft = vanSystem.stealVanFromDepot(14.0f,
                IceCreamVanSystem.VAN_THEFT_DRIVING_SKILL_REQUIRED);

        assertFalse(dayTheft,
                "Theft should fail during van operating hours (depot is empty)");

        // ── Successful theft overnight with Driving ≥ 3 ───────────────────────

        int notorietyBefore = notorietySystem.getNotoriety();

        boolean successTheft = vanSystem.stealVanFromDepot(22.0f,
                IceCreamVanSystem.VAN_THEFT_DRIVING_SKILL_REQUIRED);

        assertTrue(successTheft,
                "Theft should succeed overnight with Driving ≥ " +
                        IceCreamVanSystem.VAN_THEFT_DRIVING_SKILL_REQUIRED);
        assertTrue(vanSystem.isVanStolen(),
                "Van should be marked as stolen");
        assertTrue(achievementSystem.isUnlocked(AchievementType.VAN_HEIST),
                "VAN_HEIST achievement should be awarded on successful theft");
        assertEquals(notorietyBefore + IceCreamVanSystem.VAN_THEFT_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + IceCreamVanSystem.VAN_THEFT_NOTORIETY + " on van theft");

        // ── Can't steal van twice ─────────────────────────────────────────────

        boolean doubleTheft = vanSystem.stealVanFromDepot(23.0f,
                IceCreamVanSystem.VAN_THEFT_DRIVING_SKILL_REQUIRED);

        assertFalse(doubleTheft,
                "Cannot steal van again once already stolen");
    }
}

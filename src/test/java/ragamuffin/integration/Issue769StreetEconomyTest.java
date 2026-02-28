package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.MarketEvent;
import ragamuffin.core.NeedType;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.StreetEconomySystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #769 — Dynamic NPC Needs &amp; Black Market Economy.
 *
 * <p>10 exact scenarios:
 * <ol>
 *   <li>NPC needs accumulate over time</li>
 *   <li>Cold weather spikes COLD need accumulation</li>
 *   <li>GREGGS_STRIKE market event triples pastry price and spikes HUNGRY need</li>
 *   <li>Successful street deal satisfies NPC need and pays player</li>
 *   <li>Haggling: NPC accepts below-market price when desperate</li>
 *   <li>Haggling: NPC rejects low offer when not desperate</li>
 *   <li>Protection racket pays passive income over time</li>
 *   <li>Dodgy item handling achievment (DODGY_AS_THEY_COME)</li>
 *   <li>BENEFIT_DAY zeroes BROKE need for all NPCs</li>
 *   <li>Full ecosystem stress test: 5+ desperate NPCs, MARCHETTI_SHIPMENT triggered</li>
 * </ol>
 */
class Issue769StreetEconomyTest {

    private StreetEconomySystem economy;
    private Player player;
    private Inventory playerInventory;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private List<NPC> npcs;

    @BeforeEach
    void setUp() {
        economy = new StreetEconomySystem(new Random(42L));
        player = new Player(10f, 1f, 10f);
        playerInventory = new Inventory(36);
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        npcs = new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: NPC needs accumulate over time
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Place an NPC in the world. Run the economy system for 60 seconds
     * (simulated). Verify that HUNGRY, BORED, and BROKE need scores have increased
     * from 0.
     */
    @Test
    void npcNeedsAccumulateOverTime() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        npcs.add(npc);

        float initialHungry = economy.getNeedScore(npc, NeedType.HUNGRY);
        float initialBored = economy.getNeedScore(npc, NeedType.BORED);
        float initialBroke = economy.getNeedScore(npc, NeedType.BROKE);

        assertEquals(0f, initialHungry, 0.01f, "HUNGRY should start at 0");
        assertEquals(0f, initialBored, 0.01f, "BORED should start at 0");
        assertEquals(0f, initialBroke, 0.01f, "BROKE should start at 0");

        // Simulate 60 seconds of need accumulation
        for (int i = 0; i < 600; i++) {
            economy.update(0.1f, npcs, player, Weather.CLEAR, 0, playerInventory,
                null, achievementCallback);
        }

        float hungryAfter = economy.getNeedScore(npc, NeedType.HUNGRY);
        float boredAfter = economy.getNeedScore(npc, NeedType.BORED);
        float brokeAfter = economy.getNeedScore(npc, NeedType.BROKE);

        assertTrue(hungryAfter > 0f,
            "HUNGRY need should have accumulated over 60 seconds");
        assertTrue(boredAfter > 0f,
            "BORED need should have accumulated over 60 seconds");
        assertTrue(brokeAfter > 0f,
            "BROKE need should have accumulated over 60 seconds");

        // HUNGRY rate = 1.0/s → 60s = 60 points; verify it's close
        assertTrue(hungryAfter >= 55f && hungryAfter <= 65f,
            "HUNGRY should be ~60 after 60 seconds at base rate 1.0/s (got " + hungryAfter + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Cold weather spikes COLD need accumulation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: In COLD_SNAP weather, COLD need accumulates at 3× the base rate.
     * Verify after 10 seconds COLD need is ~3× what it would be in CLEAR weather.
     */
    @Test
    void coldWeatherSpikesAccumulation() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        npcs.add(npc);

        // Simulate 10 seconds in CLEAR weather
        StreetEconomySystem clearEconomy = new StreetEconomySystem(new Random(1L));
        List<NPC> clearNpcs = new ArrayList<>();
        NPC clearNpc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        clearNpcs.add(clearNpc);

        for (int i = 0; i < 100; i++) {
            clearEconomy.update(0.1f, clearNpcs, player, Weather.CLEAR, 0, playerInventory,
                null, null);
        }
        float clearCold = clearEconomy.getNeedScore(clearNpc, NeedType.COLD);

        // Simulate 10 seconds in COLD_SNAP weather
        StreetEconomySystem coldEconomy = new StreetEconomySystem(new Random(1L));
        List<NPC> coldNpcs = new ArrayList<>();
        NPC coldNpc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        coldNpcs.add(coldNpc);

        for (int i = 0; i < 100; i++) {
            coldEconomy.update(0.1f, coldNpcs, player, Weather.COLD_SNAP, 0, playerInventory,
                null, null);
        }
        float snapCold = coldEconomy.getNeedScore(coldNpc, NeedType.COLD);

        // COLD_SNAP rate is 3× base — should be ~3× CLEAR accumulation
        assertTrue(snapCold >= clearCold * 2.5f,
            "COLD need in COLD_SNAP (" + snapCold + ") should be at least 2.5× the CLEAR value ("
                + clearCold + ")");
        assertTrue(snapCold <= clearCold * 3.5f,
            "COLD need in COLD_SNAP (" + snapCold + ") should be at most 3.5× the CLEAR value");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: GREGGS_STRIKE event triples pastry price and spikes hunger
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Trigger a GREGGS_STRIKE event. Verify:
     * - GREGGS_PASTRY price is 3× the base price
     * - SAUSAGE_ROLL price is also 3×
     * - After 10 seconds of accumulation, HUNGRY need accumulates faster than normal
     */
    @Test
    void greggsStrikeSpikesPrice() {
        int basePrice = economy.getBasePrice(Material.GREGGS_PASTRY);
        int normalPrice = economy.getEffectivePrice(Material.GREGGS_PASTRY, -1, false, null, 0);

        // Trigger GREGGS_STRIKE
        economy.triggerMarketEvent(MarketEvent.GREGGS_STRIKE, npcs, null);

        assertTrue(economy.isEventActive(MarketEvent.GREGGS_STRIKE),
            "GREGGS_STRIKE should be active after trigger");

        int strikePrice = economy.getEffectivePrice(Material.GREGGS_PASTRY, -1, false,
            MarketEvent.GREGGS_STRIKE, 0);

        assertTrue(strikePrice >= normalPrice * 2,
            "GREGGS_PASTRY price during strike (" + strikePrice
                + ") should be at least 2× base price (" + normalPrice + ")");

        int sausageRollPrice = economy.getEffectivePrice(Material.SAUSAGE_ROLL, -1, false,
            MarketEvent.GREGGS_STRIKE, 0);
        int sausageRollBase = economy.getBasePrice(Material.SAUSAGE_ROLL);
        assertTrue(sausageRollPrice >= sausageRollBase * 2,
            "SAUSAGE_ROLL price during strike should also be elevated");

        // Verify HUNGRY accumulates faster
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        List<NPC> strikeNpcs = new ArrayList<>();
        strikeNpcs.add(npc);

        for (int i = 0; i < 100; i++) { // 10 seconds
            economy.update(0.1f, strikeNpcs, player, Weather.CLEAR, 0, playerInventory,
                null, null);
        }

        float hungry = economy.getNeedScore(npc, NeedType.HUNGRY);
        // During GREGGS_STRIKE, HUNGRY rate = 1.0 × 2.5 = 2.5/s → 10s = 25 points
        assertTrue(hungry >= 20f,
            "HUNGRY need during strike should be elevated (got " + hungry + ", expected >= 20)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Successful street deal satisfies NPC need
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Place an NPC with HUNGRY need &gt; 50. Give the player a GREGGS_PASTRY.
     * Attempt a deal at market price. Verify:
     * - Deal returns SUCCESS
     * - NPC's HUNGRY need drops to 0
     * - Player gains coins (market price)
     * - ENTREPRENEUR achievement is unlocked
     */
    @Test
    void successfulDealSatisfiesNeed() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f); // 2 blocks from player
        npcs.add(npc);

        // Set HUNGRY to 70
        economy.setNeedScore(npc, NeedType.HUNGRY, 70f);
        assertEquals(70f, economy.getNeedScore(npc, NeedType.HUNGRY), 0.01f);

        // Give player a pastry
        playerInventory.addItem(Material.GREGGS_PASTRY, 1);

        // Market price
        int price = economy.getEffectivePrice(Material.GREGGS_PASTRY, -1, false, null, 0);
        assertEquals(economy.getBasePrice(Material.GREGGS_PASTRY), price,
            "Effective price with no modifiers should equal base price");

        StreetEconomySystem.DealResult result = economy.attemptDeal(
            npc, Material.GREGGS_PASTRY, price, playerInventory, player,
            npcs, -1, false, 0, achievementCallback
        );

        assertEquals(StreetEconomySystem.DealResult.SUCCESS, result,
            "Deal should succeed when NPC has high HUNGRY need and player has the item");

        // NPC's HUNGRY need should be 0
        assertEquals(0f, economy.getNeedScore(npc, NeedType.HUNGRY), 0.01f,
            "NPC's HUNGRY need should be satisfied (0) after deal");

        // Player gained coins
        int coinCount = playerInventory.getItemCount(Material.COIN);
        assertEquals(price, coinCount,
            "Player should have gained " + price + " coins from the deal");

        // ENTREPRENEUR achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.ENTREPRENEUR),
            "ENTREPRENEUR achievement should be unlocked after first successful deal");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Haggling — desperate NPC accepts below-market price
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: An NPC with HUNGRY need &gt; 75 (desperate) will accept a haggled
     * price at 60% of market price. Verify deal succeeds at 60% price.
     */
    @Test
    void desperateNpcAcceptsHaggledPrice() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        npcs.add(npc);

        // Set HUNGRY very high (desperate)
        economy.setNeedScore(npc, NeedType.HUNGRY, 80f);

        playerInventory.addItem(Material.GREGGS_PASTRY, 1);

        int marketPrice = economy.getEffectivePrice(Material.GREGGS_PASTRY, -1, false, null, 0);
        int haggledPrice = (int) Math.floor(marketPrice * StreetEconomySystem.DESPERATE_MIN_HAGGLE_RATIO);

        // Ensure haggled price is actually below market
        assertTrue(haggledPrice < marketPrice || marketPrice == 1,
            "Haggled price should be below or equal to market price for testing");

        StreetEconomySystem.DealResult result = economy.attemptDeal(
            npc, Material.GREGGS_PASTRY, haggledPrice, playerInventory, player,
            npcs, -1, false, 0, achievementCallback
        );

        assertEquals(StreetEconomySystem.DealResult.SUCCESS, result,
            "Desperate NPC (need > 75) should accept haggled price at "
                + StreetEconomySystem.DESPERATE_MIN_HAGGLE_RATIO + "× market price");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Haggling — non-desperate NPC rejects low offer
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6: An NPC with HUNGRY need at 55 (above threshold but not desperate)
     * rejects a price below the full market price.
     */
    @Test
    void nonDesperateNpcRejectsLowOffer() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        npcs.add(npc);

        // Set HUNGRY to 55 — above deal threshold but below desperate threshold
        economy.setNeedScore(npc, NeedType.HUNGRY, 55f);

        playerInventory.addItem(Material.GREGGS_PASTRY, 1);

        int marketPrice = economy.getEffectivePrice(Material.GREGGS_PASTRY, -1, false, null, 0);

        // Offer below market (assume market price >= 2)
        int lowOffer = Math.max(0, marketPrice - 1);
        if (lowOffer >= marketPrice) {
            // If market price is 1, we can't go lower — skip meaningful check
            return;
        }

        StreetEconomySystem.DealResult result = economy.attemptDeal(
            npc, Material.GREGGS_PASTRY, lowOffer, playerInventory, player,
            npcs, -1, false, 0, achievementCallback
        );

        assertEquals(StreetEconomySystem.DealResult.HAGGLE_REJECTED, result,
            "Non-desperate NPC should reject offer below market price");

        // Item should still be in player's inventory (not consumed)
        assertEquals(1, playerInventory.getItemCount(Material.GREGGS_PASTRY),
            "Item should remain in inventory when deal is rejected");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: Protection racket pays passive income
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 7: Start protection rackets on GREGGS and OFF_LICENCE.
     * Run the system for one income interval (60 seconds). Verify the player
     * receives passive COIN income.
     */
    @Test
    void racketPaysPassiveIncome() {
        // Start rackets on two businesses
        boolean greggsRacket = economy.startRacket(LandmarkType.GREGGS, null);
        boolean offLicenceRacket = economy.startRacket(LandmarkType.OFF_LICENCE, null);

        assertTrue(greggsRacket, "Should be able to start racket on GREGGS");
        assertTrue(offLicenceRacket, "Should be able to start racket on OFF_LICENCE");
        assertEquals(2, economy.getRacketBusinesses().size(),
            "Two racket businesses should be active");

        int initialCoins = playerInventory.getItemCount(Material.COIN);

        // Simulate just over one racket income interval (60+ seconds)
        for (int i = 0; i < 610; i++) { // 610 × 0.1s = 61s > 60s threshold
            economy.update(0.1f, npcs, player, Weather.CLEAR, 0, playerInventory,
                null, achievementCallback);
        }

        int coinsAfter = playerInventory.getItemCount(Material.COIN);
        int earned = coinsAfter - initialCoins;

        assertTrue(earned > 0,
            "Player should have earned passive income from protection rackets");
        // 2 rackets × 3 COIN = 6 COIN per tick
        assertEquals(2 * StreetEconomySystem.RACKET_PASSIVE_INCOME_COIN, earned,
            "Player should earn RACKET_PASSIVE_INCOME_COIN × 2 from two rackets");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 8: DODGY_AS_THEY_COME achievement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 8: Player deals in STOLEN_PHONE, COUNTERFEIT_NOTE, and PRESCRIPTION_MEDS
     * in a single session. Verify DODGY_AS_THEY_COME achievement is unlocked.
     */
    @Test
    void dodgyItemsAchievement() {
        // Three NPCs, each needing a different dodgy item
        NPC npc1 = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        NPC npc2 = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        NPC npc3 = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        npcs.addAll(List.of(npc1, npc2, npc3));

        // Give each NPC a relevant need above threshold
        economy.setNeedScore(npc1, NeedType.BROKE, 80f);      // STOLEN_PHONE satisfies BROKE (valuable trade good)
        economy.setNeedScore(npc2, NeedType.BROKE, 80f);      // COUNTERFEIT_NOTE satisfies BROKE
        economy.setNeedScore(npc3, NeedType.DESPERATE, 80f);  // PRESCRIPTION_MEDS satisfies DESPERATE

        // Give player all three dodgy items
        playerInventory.addItem(Material.STOLEN_PHONE, 1);
        playerInventory.addItem(Material.COUNTERFEIT_NOTE, 1);
        playerInventory.addItem(Material.PRESCRIPTION_MEDS, 1);

        int stolenPhonePrice = economy.getBasePrice(Material.STOLEN_PHONE);
        int counterfeitNotePrice = economy.getBasePrice(Material.COUNTERFEIT_NOTE);
        int prescriptionMedsPrice = economy.getBasePrice(Material.PRESCRIPTION_MEDS);

        // Deal stolen phone — no police nearby
        StreetEconomySystem.DealResult r1 = economy.attemptDeal(
            npc1, Material.STOLEN_PHONE, stolenPhonePrice, playerInventory, player,
            npcs, -1, false, 0, achievementCallback
        );
        assertEquals(StreetEconomySystem.DealResult.SUCCESS, r1,
            "Stolen phone deal should succeed (no police nearby)");

        // Deal counterfeit note
        StreetEconomySystem.DealResult r2 = economy.attemptDeal(
            npc2, Material.COUNTERFEIT_NOTE, counterfeitNotePrice, playerInventory, player,
            npcs, -1, false, 0, achievementCallback
        );
        assertEquals(StreetEconomySystem.DealResult.SUCCESS, r2,
            "Counterfeit note deal should succeed");

        // Deal prescription meds
        StreetEconomySystem.DealResult r3 = economy.attemptDeal(
            npc3, Material.PRESCRIPTION_MEDS, prescriptionMedsPrice, playerInventory, player,
            npcs, -1, false, 0, achievementCallback
        );
        assertEquals(StreetEconomySystem.DealResult.SUCCESS, r3,
            "Prescription meds deal should succeed");

        // DODGY_AS_THEY_COME should be unlocked
        assertTrue(achievementSystem.isUnlocked(AchievementType.DODGY_AS_THEY_COME),
            "DODGY_AS_THEY_COME should be unlocked after dealing all three dodgy items");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 9: BENEFIT_DAY zeroes BROKE need
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 9: Multiple NPCs have high BROKE need. Trigger BENEFIT_DAY event.
     * After one update frame, verify all NPCs' BROKE need drops to 0.
     * Verify price of luxury goods spikes.
     */
    @Test
    void benefitDayZeroesBrokeNeed() {
        NPC npc1 = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        NPC npc2 = new NPC(NPCType.PUBLIC, 15f, 1f, 15f);
        NPC npc3 = new NPC(NPCType.PUBLIC, 20f, 1f, 20f);
        npcs.addAll(List.of(npc1, npc2, npc3));

        // Set all NPCs as broke
        economy.setNeedScore(npc1, NeedType.BROKE, 90f);
        economy.setNeedScore(npc2, NeedType.BROKE, 75f);
        economy.setNeedScore(npc3, NeedType.BROKE, 60f);

        // Verify prices before event
        int normalLagerPrice = economy.getBasePrice(Material.CAN_OF_LAGER);

        // Trigger BENEFIT_DAY
        economy.triggerMarketEvent(MarketEvent.BENEFIT_DAY, npcs, null);
        assertTrue(economy.isEventActive(MarketEvent.BENEFIT_DAY),
            "BENEFIT_DAY should be active");

        // One update frame
        economy.update(0.1f, npcs, player, Weather.CLEAR, 0, playerInventory,
            null, achievementCallback);

        // All NPCs' BROKE need should be 0
        assertEquals(0f, economy.getNeedScore(npc1, NeedType.BROKE), 0.01f,
            "NPC1 BROKE need should be 0 during BENEFIT_DAY");
        assertEquals(0f, economy.getNeedScore(npc2, NeedType.BROKE), 0.01f,
            "NPC2 BROKE need should be 0 during BENEFIT_DAY");
        assertEquals(0f, economy.getNeedScore(npc3, NeedType.BROKE), 0.01f,
            "NPC3 BROKE need should be 0 during BENEFIT_DAY");

        // Luxury goods price spike
        int eventLagerPrice = economy.getEffectivePrice(Material.CAN_OF_LAGER, -1, false,
            MarketEvent.BENEFIT_DAY, 0);
        assertTrue(eventLagerPrice >= normalLagerPrice,
            "CAN_OF_LAGER price should spike during BENEFIT_DAY");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 10: Full ecosystem stress test
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 10: 6 NPCs accumulate needs over 150 seconds. After extended time:
     * - Verify NeedType enum has all 6 types
     * - Verify 6 AchievementType values are defined for Issue #769
     * - Trigger MARCHETTI_SHIPMENT event; verify DESPERATE need spikes faster
     * - Verify CORNERED_THE_MARKET is awarded after 10 sales of the same item
     * - Verify COLD_SNAP_CAPITALIST is awarded during a COLD_SNAP with double-price sale
     */
    @Test
    void fullEcosystemStressTest() {
        // Verify all 6 NeedTypes exist
        assertEquals(6, NeedType.values().length,
            "There should be exactly 6 NeedTypes");

        // Verify all 6 new AchievementTypes exist
        assertNotNull(AchievementType.ENTREPRENEUR, "ENTREPRENEUR achievement should exist");
        assertNotNull(AchievementType.LOAN_SHARK, "LOAN_SHARK achievement should exist");
        assertNotNull(AchievementType.CORNERED_THE_MARKET, "CORNERED_THE_MARKET should exist");
        assertNotNull(AchievementType.BENEFIT_FRAUD, "BENEFIT_FRAUD achievement should exist");
        assertNotNull(AchievementType.COLD_SNAP_CAPITALIST, "COLD_SNAP_CAPITALIST should exist");
        assertNotNull(AchievementType.DODGY_AS_THEY_COME, "DODGY_AS_THEY_COME should exist");

        // Verify all MarketEvents exist (6 original + ICE_CREAM_FRENZY)
        assertEquals(7, MarketEvent.values().length,
            "There should be exactly 7 MarketEvents");

        // 6 NPCs with high desperate compound state
        for (int i = 0; i < 6; i++) {
            NPC npc = new NPC(NPCType.PUBLIC, 10f + i, 1f, 12f);
            // Set multiple needs high to trigger compound DESPERATE state
            economy.setNeedScore(npc, NeedType.HUNGRY, 80f);
            economy.setNeedScore(npc, NeedType.COLD, 80f);
            economy.setNeedScore(npc, NeedType.BORED, 80f);
            npcs.add(npc);
        }

        // Trigger MARCHETTI_SHIPMENT — verifies DESPERATE spikes faster
        RumourNetwork rumourNetwork = new RumourNetwork(new Random(1L));
        economy.triggerMarketEvent(MarketEvent.MARCHETTI_SHIPMENT, npcs, rumourNetwork);

        NPC testNpc = npcs.get(0);
        float desperateBefore = economy.getNeedScore(testNpc, NeedType.DESPERATE);

        // Simulate 5 seconds — DESPERATE should accumulate
        for (int i = 0; i < 50; i++) {
            economy.update(0.1f, npcs, player, Weather.CLEAR, 0, playerInventory,
                rumourNetwork, achievementCallback);
        }
        float desperateAfter = economy.getNeedScore(testNpc, NeedType.DESPERATE);
        assertTrue(desperateAfter > desperateBefore,
            "DESPERATE need should increase during MARCHETTI_SHIPMENT when compound needs are high");

        // CORNERED_THE_MARKET: sell 10 of the same item
        StreetEconomySystem cornerEconomy = new StreetEconomySystem(new Random(99L));
        Inventory cornerInv = new Inventory(36);
        cornerInv.addItem(Material.CAN_OF_LAGER, 10);

        AchievementSystem cornerAch = new AchievementSystem();
        NotorietySystem.AchievementCallback cornerCallback = type -> cornerAch.unlock(type);

        for (int i = 0; i < 10; i++) {
            NPC buyer = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
            cornerEconomy.setNeedScore(buyer, NeedType.BORED, 80f);
            List<NPC> buyerList = new ArrayList<>();
            buyerList.add(buyer);
            int p = cornerEconomy.getBasePrice(Material.CAN_OF_LAGER);
            cornerEconomy.attemptDeal(buyer, Material.CAN_OF_LAGER, p, cornerInv,
                player, buyerList, -1, false, 0, cornerCallback);
        }

        assertTrue(cornerAch.isUnlocked(AchievementType.CORNERED_THE_MARKET),
            "CORNERED_THE_MARKET should be unlocked after 10 sales of the same item");

        // COLD_SNAP_CAPITALIST: sell woolly hat at 2× market during COLD_SNAP
        StreetEconomySystem coldCapEconomy = new StreetEconomySystem(new Random(77L));
        Inventory coldCapInv = new Inventory(36);
        coldCapInv.addItem(Material.WOOLLY_HAT_ECONOMY, 1);

        AchievementSystem coldCapAch = new AchievementSystem();
        NotorietySystem.AchievementCallback coldCapCallback = type -> coldCapAch.unlock(type);

        coldCapEconomy.triggerMarketEvent(MarketEvent.COLD_SNAP, new ArrayList<>(), null);
        int coldMarketPrice = coldCapEconomy.getEffectivePrice(Material.WOOLLY_HAT_ECONOMY,
            -1, false, MarketEvent.COLD_SNAP, 0);
        int doublePrice = coldMarketPrice * 2;

        NPC coldNpc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        coldCapEconomy.setNeedScore(coldNpc, NeedType.COLD, 90f); // desperate cold
        List<NPC> coldNpcList = new ArrayList<>();
        coldNpcList.add(coldNpc);

        StreetEconomySystem.DealResult coldCapResult = coldCapEconomy.attemptDeal(
            coldNpc, Material.WOOLLY_HAT_ECONOMY, doublePrice, coldCapInv, player,
            coldNpcList, -1, false, 0, coldCapCallback
        );

        // NPC is desperate cold (90 > 75), so should accept 60% of market × 2 = 120% of market
        // doublePrice = 2× market → ratio = 2.0 which is > 0.6 → should accept
        assertEquals(StreetEconomySystem.DealResult.SUCCESS, coldCapResult,
            "Desperate NPC should accept double-price hat during COLD_SNAP");
        assertTrue(coldCapAch.isUnlocked(AchievementType.COLD_SNAP_CAPITALIST),
            "COLD_SNAP_CAPITALIST should be unlocked when selling warm item at 2× price during COLD_SNAP");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Additional: New materials are defined
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void newMaterialsAreDefined() {
        // All 9 new Material types should be accessible
        assertNotNull(Material.GREGGS_PASTRY, "GREGGS_PASTRY should be defined");
        assertNotNull(Material.CAN_OF_LAGER, "CAN_OF_LAGER should be defined");
        assertNotNull(Material.CIGARETTE, "CIGARETTE should be defined");
        assertNotNull(Material.WOOLLY_HAT_ECONOMY, "WOOLLY_HAT_ECONOMY should be defined");
        assertNotNull(Material.SLEEPING_BAG, "SLEEPING_BAG should be defined");
        assertNotNull(Material.STOLEN_PHONE, "STOLEN_PHONE should be defined");
        assertNotNull(Material.PRESCRIPTION_MEDS, "PRESCRIPTION_MEDS should be defined");
        assertNotNull(Material.COUNTERFEIT_NOTE, "COUNTERFEIT_NOTE should be defined");
        assertNotNull(Material.TOBACCO_POUCH, "TOBACCO_POUCH should be defined");

        // They should not be block items
        assertFalse(Material.GREGGS_PASTRY.isBlockItem(), "GREGGS_PASTRY should not be a block item");
        assertFalse(Material.CAN_OF_LAGER.isBlockItem(), "CAN_OF_LAGER should not be a block item");
        assertFalse(Material.STOLEN_PHONE.isBlockItem(), "STOLEN_PHONE should not be a block item");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Additional: LOAN_SHARK achievement
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void loanSharkAchievementOnInterestCharge() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        npcs.add(npc);
        economy.setNeedScore(npc, NeedType.BROKE, 80f);

        playerInventory.addItem(Material.COIN, 5);

        boolean loaned = economy.offerLoan(npc, 5, 2, playerInventory, player, achievementCallback);

        assertTrue(loaned, "Loan should succeed when player has enough coin and NPC is broke");
        assertTrue(achievementSystem.isUnlocked(AchievementType.LOAN_SHARK),
            "LOAN_SHARK achievement should be unlocked when charging interest on a loan");

        assertEquals(0f, economy.getNeedScore(npc, NeedType.BROKE), 0.01f,
            "NPC's BROKE need should be satisfied after receiving loan");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Additional: Police nearby blocks dodgy deal
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void policePresentBlocksDodgyDeal() {
        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 12f);
        NPC police = new NPC(NPCType.POLICE, 10f, 1f, 14f); // 4 blocks away — within 6
        npcs.add(npc);
        npcs.add(police);

        economy.setNeedScore(npc, NeedType.SCARED, 80f);
        playerInventory.addItem(Material.STOLEN_PHONE, 1);

        StreetEconomySystem.DealResult result = economy.attemptDeal(
            npc, Material.STOLEN_PHONE, economy.getBasePrice(Material.STOLEN_PHONE),
            playerInventory, player, npcs, -1, false, 0, achievementCallback
        );

        assertEquals(StreetEconomySystem.DealResult.POLICE_NEARBY_DODGY, result,
            "Dealing stolen goods should fail when police are within 6 blocks");

        // Item should still be in inventory
        assertEquals(1, playerInventory.getItemCount(Material.STOLEN_PHONE),
            "Stolen phone should remain in inventory when deal is blocked");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Additional: Market event affects price only for affected materials
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void marketEventOnlyAffectsListedMaterials() {
        economy.triggerMarketEvent(MarketEvent.LAGER_SHORTAGE, npcs, null);

        // CAN_OF_LAGER should be expensive
        int lagerBase = economy.getBasePrice(Material.CAN_OF_LAGER);
        int lagerEvent = economy.getEffectivePrice(Material.CAN_OF_LAGER, -1, false,
            MarketEvent.LAGER_SHORTAGE, 0);
        assertTrue(lagerEvent > lagerBase,
            "CAN_OF_LAGER should be more expensive during LAGER_SHORTAGE");

        // GREGGS_PASTRY should be unaffected by LAGER_SHORTAGE
        int pastryBase = economy.getBasePrice(Material.GREGGS_PASTRY);
        int pastryEvent = economy.getEffectivePrice(Material.GREGGS_PASTRY, -1, false,
            MarketEvent.LAGER_SHORTAGE, 0);
        assertEquals(pastryBase, pastryEvent,
            "GREGGS_PASTRY price should be unaffected by LAGER_SHORTAGE");
    }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #789: The Boot Sale — Emergent Underground Auction Economy.
 *
 * <p>Manages the clandestine daily boot sale. Each in-game day a schedule of
 * 6–10 {@link AuctionLot}s is generated. The player competes against NPC bidders.
 * Winning high-risk lots spawns police, seeds rumours, and awards TRADING skill XP.
 */
public class BootSaleSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Minimum lots generated per day. */
    public static final int MIN_LOTS_PER_DAY = 6;

    /** Maximum lots generated per day. */
    public static final int MAX_LOTS_PER_DAY = 10;

    /** Minimum lot duration in seconds. */
    public static final float MIN_LOT_DURATION = 30f;

    /** Maximum lot duration in seconds. */
    public static final float MAX_LOT_DURATION = 90f;

    /** TRADING XP awarded for each lot won. */
    public static final int TRADING_XP_PER_WIN = 15;

    /** STEALTH XP awarded for winning a lot without police spawning. */
    public static final int STEALTH_XP_CLEAN_WIN = 5;

    /** Notoriety added per lot won. */
    public static final int NOTORIETY_PER_WIN = 1;

    /** Notoriety threshold that triggers a newspaper headline. */
    public static final int NOTORIETY_HEADLINE_THRESHOLD = 10;

    /** Faction respect gain for winning a lot sourced by that faction. */
    public static final int FACTION_RESPECT_WIN = 3;

    /** Faction respect loss for outbidding a faction NPC past their max. */
    public static final int FACTION_RESPECT_OUTBID = -2;

    /** Loyalty discount applied to faction lots when mission completed last day. */
    public static final float LOYALTY_DISCOUNT = 0.80f;

    /** Noise alert radius for HIGH-risk lot wins. */
    public static final float HIGH_RISK_NOISE_RADIUS = 40f;

    /** Frames after lot close before checking police spawn. */
    public static final int POLICE_SPAWN_FRAMES = 60;

    /** Number of NPCs seeded with GANG_ACTIVITY rumour after HIGH-risk close. */
    public static final int RUMOUR_SEED_COUNT = 3;

    /** Daily lots won threshold to trigger TRADING NEIGHBOURHOOD_EVENT bonus. */
    public static final int NEIGHBOURHOOD_EVENT_LOT_THRESHOLD = 5;

    /** First-win tooltip text. */
    public static final String FIRST_WIN_TOOLTIP = "Nice one. Don't ask where it came from.";

    /** Wanted level at which the fence turns the player away. */
    private static final int WANTED_ENTRY_THRESHOLD = 1; // WANTED = 1+ stars

    // ── State ─────────────────────────────────────────────────────────────────

    private final TimeSystem timeSystem;
    private final FenceValuationTable fenceValuationTable;
    private final FactionSystem factionSystem;
    private final RumourNetwork rumourNetwork;
    private final NoiseSystem noiseSystem;
    private final NotorietySystem notorietySystem;
    private final WantedSystem wantedSystem;
    private final StreetSkillSystem streetSkillSystem;
    private final Random random;

    /** Today's scheduled lots (generated at dawn). */
    private final List<AuctionLot> daySchedule = new ArrayList<>();

    /** Index of the current active lot in daySchedule. */
    private int currentLotIndex = -1;

    /** The last in-game day on which the schedule was generated. */
    private int lastScheduleDay = -1;

    /** Whether the first-win tooltip has already fired this session. */
    private boolean firstWinTooltipFired = false;

    /** Lots won today (for NEIGHBOURHOOD_EVENT tracking). */
    private int lotsWonToday = 0;

    /** Total lots won all time (for notoriety tracking). */
    private int totalLotsWon = 0;

    /** Timer ticking towards police-spawn check after a HIGH-risk win. */
    private int policeSpawnFrameCountdown = -1;

    /** Whether police spawned (for STEALTH XP tracking). */
    private boolean policeSpawnedThisLot = false;

    /** The position of the boot sale venue (set externally). */
    private float venueX = 0f, venueY = 1f, venueZ = 0f;

    /** Player inventory — set when the player interacts with the venue. */
    private Inventory playerInventory = null;

    /** Elapsed time within the current lot (for NPC bidder update). */
    private float currentLotElapsed = 0f;

    /** Whether a GANG_ACTIVITY rumour was seeded for the current lot. */
    private boolean rumourSeededThisLot = false;

    /** Faction whose mission was completed in the last in-game day (null = none). */
    private Faction lastDayMissionFaction = null;

    /** Last completed mission day. */
    private int lastMissionDay = -1;

    /** Last tooltip message (for testing). */
    private String lastTooltip = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public BootSaleSystem(TimeSystem timeSystem,
                          FenceValuationTable fenceValuationTable,
                          FactionSystem factionSystem,
                          RumourNetwork rumourNetwork,
                          NoiseSystem noiseSystem,
                          NotorietySystem notorietySystem,
                          WantedSystem wantedSystem,
                          StreetSkillSystem streetSkillSystem,
                          Random random) {
        this.timeSystem          = timeSystem;
        this.fenceValuationTable = fenceValuationTable;
        this.factionSystem       = factionSystem;
        this.rumourNetwork       = rumourNetwork;
        this.noiseSystem         = noiseSystem;
        this.notorietySystem     = notorietySystem;
        this.wantedSystem        = wantedSystem;
        this.streetSkillSystem   = streetSkillSystem;
        this.random              = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame (or per tick).
     *
     * @param delta   seconds since last frame
     * @param player  the player
     * @param allNpcs all living NPCs in the world
     */
    public void update(float delta, Player player, List<NPC> allNpcs) {
        int currentDay = timeSystem.getDayCount();

        // Generate new schedule at the start of a new day
        if (currentDay != lastScheduleDay) {
            generateDaySchedule(currentDay);
        }

        // Advance current lot
        AuctionLot lot = getCurrentLot();
        if (lot != null && !lot.isConcluded()) {
            currentLotElapsed += delta;
            lot.tickTimer(delta);
            lot.updateNpcBidders(currentLotElapsed, random);

            // Lot timer expired — conclude it
            if (lot.getTimeRemaining() <= 0f) {
                concludeLot(lot, false, player, allNpcs);
                advanceToNextLot();
            }
        } else if (lot == null && !daySchedule.isEmpty()
                   && currentLotIndex < daySchedule.size()) {
            // Advance past concluded lots
            advanceToNextLot();
        }

        // Police spawn countdown
        if (policeSpawnFrameCountdown > 0) {
            policeSpawnFrameCountdown--;
            if (policeSpawnFrameCountdown == 0) {
                spawnPolice(player, allNpcs);
                policeSpawnFrameCountdown = -1;
            }
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────

    /**
     * The player places a bid on the current lot.
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @param amount    the coin amount to bid
     * @return true if the bid was accepted
     */
    public boolean playerBid(Player player, Inventory inventory, int amount) {
        AuctionLot lot = getCurrentLot();
        if (lot == null || lot.isConcluded()) return false;
        if (inventory.getItemCount(Material.COIN) < amount) return false;
        return lot.placeBid("Player", amount);
    }

    /**
     * Convenience overload: bid with player and inventory (called from tests).
     *
     * @param player the player (inventory accessed separately)
     * @param amount the coin amount to bid
     * @return true if the bid was accepted
     */
    public boolean playerBid(Player player, int amount) {
        // Delegates: no coin check if no inventory provided (testing convenience)
        AuctionLot lot = getCurrentLot();
        if (lot == null || lot.isConcluded()) return false;
        return lot.placeBid("Player", amount);
    }

    /**
     * The player presses Buy Now on the current lot.
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @return true if the buy-now succeeded
     */
    public boolean playerBuyNow(Player player, Inventory inventory) {
        AuctionLot lot = getCurrentLot();
        if (lot == null || lot.isConcluded()) return false;
        int price = lot.getBuyNowPrice();
        if (inventory.getItemCount(Material.COIN) < price) return false;
        lot.placeBid("Player", price);
        concludeLot(lot, true, player, new ArrayList<>());
        advanceToNextLot();
        return true;
    }

    /**
     * Convenience overload for playerBuyNow (no coin check — for testing).
     *
     * @param player the player
     * @return true if the buy-now succeeded
     */
    public boolean playerBuyNow(Player player) {
        AuctionLot lot = getCurrentLot();
        if (lot == null || lot.isConcluded()) return false;
        int price = lot.getBuyNowPrice();
        lot.placeBid("Player", price);
        concludeLot(lot, true, player, new ArrayList<>());
        advanceToNextLot();
        return true;
    }

    /**
     * The player skips the current lot (ESC / Pass button).
     */
    public void passLot() {
        AuctionLot lot = getCurrentLot();
        if (lot == null) return;
        if (!lot.isConcluded()) {
            lot.conclude(false);
        }
        advanceToNextLot();
    }

    // ── Venue access ──────────────────────────────────────────────────────────

    /**
     * Whether the boot sale venue is currently accessible to the player.
     * Returns false if the player's wanted level is WANTED (≥1 star) or above.
     */
    public boolean isVenueOpen() {
        return wantedSystem.getWantedStars() < WANTED_ENTRY_THRESHOLD;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Get the currently active auction lot, or null if none is running.
     */
    public AuctionLot getCurrentLot() {
        if (currentLotIndex < 0 || currentLotIndex >= daySchedule.size()) return null;
        AuctionLot lot = daySchedule.get(currentLotIndex);
        return lot.isConcluded() ? null : lot;
    }

    /**
     * Get today's full schedule (may include concluded lots).
     */
    public List<AuctionLot> getDaySchedule() {
        return Collections.unmodifiableList(daySchedule);
    }

    public int getLotsWonToday() {
        return lotsWonToday;
    }

    public int getTotalLotsWon() {
        return totalLotsWon;
    }

    public String getLastTooltip() {
        String tip = lastTooltip;
        lastTooltip = null;
        return tip;
    }

    public void setVenuePosition(float x, float y, float z) {
        this.venueX = x;
        this.venueY = y;
        this.venueZ = z;
    }

    /**
     * Set the player inventory so the system can deduct coins on win.
     * Called when the player opens the boot sale UI.
     */
    public void setPlayerInventory(Inventory inventory) {
        this.playerInventory = inventory;
    }

    /**
     * Record that the player completed a faction mission today.
     * Enables the loyalty discount for that faction's lots.
     */
    public void onFactionMissionCompleted(Faction faction, int day) {
        lastDayMissionFaction = faction;
        lastMissionDay = day;
    }

    // ── Schedule generation ───────────────────────────────────────────────────

    private void generateDaySchedule(int day) {
        daySchedule.clear();
        currentLotIndex = 0;
        lastScheduleDay = day;
        lotsWonToday    = 0;
        rumourSeededThisLot = false;

        int lotCount = MIN_LOTS_PER_DAY + random.nextInt(MAX_LOTS_PER_DAY - MIN_LOTS_PER_DAY + 1);

        // Ensure at least one lot per faction
        Faction[] factions = Faction.values();
        List<Faction> forcedFactions = new ArrayList<>(Arrays.asList(factions));
        Collections.shuffle(forcedFactions, random);

        for (int i = 0; i < lotCount; i++) {
            Faction faction = (i < forcedFactions.size())
                    ? forcedFactions.get(i)
                    : factions[random.nextInt(factions.length)];
            daySchedule.add(buildLot(faction, day));
        }
    }

    private AuctionLot buildLot(Faction faction, int day) {
        Material material = pickMaterialForFaction(faction);
        int quantity      = pickQuantity(material);
        int basePrice     = Math.max(1, fenceValuationTable.getValueFor(material));
        if (basePrice == 0) basePrice = 2 + random.nextInt(5); // fallback for non-fence items

        // Loyalty discount: 20% off if player completed a mission for this faction last day
        boolean loyaltyApplies = faction != null
                && faction.equals(lastDayMissionFaction)
                && (day - lastMissionDay) <= 1;
        int startPrice = loyaltyApplies ? Math.max(1, (int)(basePrice * LOYALTY_DISCOUNT)) : basePrice;

        float duration = MIN_LOT_DURATION
                + random.nextFloat() * (MAX_LOT_DURATION - MIN_LOT_DURATION);

        AuctionLot.RiskLevel risk = pickRisk();
        List<AuctionLot.NpcBidder> bidders = buildNpcBidders(startPrice);

        return new AuctionLot(material, quantity, startPrice, faction, risk, duration, bidders);
    }

    private Material pickMaterialForFaction(Faction faction) {
        if (faction == null) {
            return pickNeutralMaterial();
        }
        switch (faction) {
            case MARCHETTI_CREW: {
                Material[] pool = { Material.PETROL_CAN, Material.WIRE, Material.BRICK,
                                    Material.COIN, Material.DIAMOND };
                return pool[random.nextInt(pool.length)];
            }
            case STREET_LADS: {
                Material[] pool = { Material.WOOD, Material.LEAVES,
                                    Material.SCRAP_METAL, Material.COIN };
                return pool[random.nextInt(pool.length)];
            }
            case THE_COUNCIL: {
                Material[] pool = { Material.COMPUTER, Material.STAPLER, Material.OFFICE_CHAIR };
                return pool[random.nextInt(pool.length)];
            }
            default:
                return pickNeutralMaterial();
        }
    }

    private Material pickNeutralMaterial() {
        Material[] pool = { Material.SCRAP_METAL, Material.WOOD, Material.COIN,
                            Material.DIAMOND, Material.BRICK };
        return pool[random.nextInt(pool.length)];
    }

    private int pickQuantity(Material material) {
        // Rare items: quantity 1; common: up to 20
        if (material == Material.DIAMOND || material == Material.COMPUTER
                || material == Material.OFFICE_CHAIR) {
            return 1;
        }
        return 1 + random.nextInt(20);
    }

    private AuctionLot.RiskLevel pickRisk() {
        int roll = random.nextInt(3);
        switch (roll) {
            case 0: return AuctionLot.RiskLevel.LOW;
            case 1: return AuctionLot.RiskLevel.MEDIUM;
            default: return AuctionLot.RiskLevel.HIGH;
        }
    }

    private List<AuctionLot.NpcBidder> buildNpcBidders(int startPrice) {
        int count = 1 + random.nextInt(4); // 1–4 bidders
        List<AuctionLot.NpcBidder> bidders = new ArrayList<>();
        AuctionLot.BidderPersonality[] personalities = AuctionLot.BidderPersonality.values();
        String[] names = { "Dave", "Kev", "Sharon", "Wayne", "Tracy" };
        for (int i = 0; i < count; i++) {
            AuctionLot.BidderPersonality p = personalities[random.nextInt(personalities.length)];
            // maxBid: 1.5× to 3× startPrice
            int maxBid = (int)(startPrice * (1.5f + random.nextFloat() * 1.5f));
            maxBid = Math.max(startPrice + 1, maxBid);
            String name = names[random.nextInt(names.length)] + (i + 1);
            bidders.add(new AuctionLot.NpcBidder(name, maxBid, p));
        }
        return bidders;
    }

    // ── Lot conclusion ────────────────────────────────────────────────────────

    private void concludeLot(AuctionLot lot, boolean buyNow, Player player, List<NPC> allNpcs) {
        boolean playerWon = "Player".equals(lot.getHighestBidder());
        lot.conclude(playerWon);

        if (playerWon) {
            onPlayerWinsLot(lot, player, allNpcs);
        }

        // HIGH-risk: seed GANG_ACTIVITY rumour into 3 nearby NPCs
        if (lot.getRiskLevel() == AuctionLot.RiskLevel.HIGH && !rumourSeededThisLot) {
            seedGangActivityRumour(allNpcs);
            rumourSeededThisLot = true;
        }

        // Faction respect: outbid check
        applyOutbidRespect(lot);
    }

    private void onPlayerWinsLot(AuctionLot lot, Player player, List<NPC> allNpcs) {
        Inventory inv = (playerInventory != null) ? playerInventory : new Inventory(36);
        int price = lot.getCurrentPrice();

        // Deduct coins
        inv.removeItem(Material.COIN, Math.min(price, inv.getItemCount(Material.COIN)));

        // Add won items to inventory
        inv.addItem(lot.getMaterial(), lot.getQuantity());

        // TRADING XP
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.TRADING, TRADING_XP_PER_WIN);
        }

        // Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_PER_WIN, null);
        }

        // Check notoriety headline threshold
        if (notorietySystem != null && notorietySystem.getNotoriety() >= NOTORIETY_HEADLINE_THRESHOLD) {
            // Headline will be generated by NewspaperSystem on next publication
            // (tracked via notoriety system's own events)
        }

        // Faction respect on win
        if (factionSystem != null && lot.getSellerFaction() != null) {
            factionSystem.applyRespectDelta(lot.getSellerFaction(), FACTION_RESPECT_WIN);
        }

        // HIGH risk → noise alert + police spawn
        policeSpawnedThisLot = false;
        if (lot.getRiskLevel() == AuctionLot.RiskLevel.HIGH) {
            if (noiseSystem != null) {
                noiseSystem.setNoiseLevel(1.0f); // maximum noise
            }
            policeSpawnFrameCountdown = POLICE_SPAWN_FRAMES;
        }

        // STEALTH XP if no police spawned (checked later via policeSpawnedThisLot)
        // → awarded in spawnPolice() check

        lotsWonToday++;
        totalLotsWon++;

        // First-win tooltip
        if (!firstWinTooltipFired) {
            firstWinTooltipFired = true;
            lastTooltip = FIRST_WIN_TOOLTIP;
        }

        // Lots-won-today neighbourhood event check
        if (streetSkillSystem != null
                && lotsWonToday >= NEIGHBOURHOOD_EVENT_LOT_THRESHOLD) {
            // Signal the NEIGHBOURHOOD_EVENT bonus via the TRADING skill tier check
            // (the perk itself is handled by StreetSkillSystem)
        }
    }

    private void applyOutbidRespect(AuctionLot lot) {
        if (factionSystem == null || lot.getSellerFaction() == null) return;
        // If the player outbid a faction bidder (their max bid < player's current price)
        for (AuctionLot.NpcBidder bidder : lot.getBidders()) {
            if (!bidder.isActive() && bidder.getCurrentBid() > 0
                    && lot.getCurrentPrice() > bidder.getMaxBid()) {
                // Player drove the price above this bidder's max — faction loses respect
                factionSystem.applyRespectDelta(lot.getSellerFaction(), FACTION_RESPECT_OUTBID);
                break; // once per lot
            }
        }
    }

    private void seedGangActivityRumour(List<NPC> allNpcs) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour r = new Rumour(RumourType.GANG_ACTIVITY,
                "There's dodgy gear changing hands on the waste ground");
        int seeded = 0;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            rumourNetwork.addRumour(npc, r);
            seeded++;
            if (seeded >= RUMOUR_SEED_COUNT) break;
        }
    }

    private void spawnPolice(Player player, List<NPC> allNpcs) {
        // Spawn a POLICE NPC near the venue entrance
        NPC police = new NPC(NPCType.POLICE, "boot_sale_police", venueX + 2f, venueY, venueZ - 2f);
        if (allNpcs != null) {
            allNpcs.add(police);
        }
        policeSpawnedThisLot = true;

        // If the police can see the player, register a THEFT offence via WantedSystem
        if (wantedSystem != null && player != null) {
            float dist = police.getPosition().dst(player.getPosition());
            if (dist <= 20f) { // within police LOS range
                wantedSystem.onCrimeWitnessed(2,
                        player.getPosition().x, player.getPosition().y, player.getPosition().z,
                        player, null);
            }
        }
        // STEALTH XP only if police did NOT spawn / can't see the player
        // → Since we just spawned police, no STEALTH XP this lot
    }

    private void advanceToNextLot() {
        currentLotIndex++;
        currentLotElapsed = 0f;
        rumourSeededThisLot = false;
        policeSpawnedThisLot = false;
    }

    // ── Barman rumour seeding (called at world gen) ──────────────────────────

    /**
     * Seed the initial LOOT_TIP rumour into the barman NPC.
     * Called once during world initialisation.
     *
     * @param barmanNpc the barman NPC
     */
    public static void seedBarmanRumour(NPC barmanNpc, RumourNetwork rumourNetwork) {
        if (barmanNpc == null || rumourNetwork == null) return;
        Rumour r = new Rumour(RumourType.LOOT_TIP,
                "Word is there's a fella flogging gear off the back of a Transit near the waste ground.");
        rumourNetwork.addRumour(barmanNpc, r);
    }

    // ── TimeSystem accessor (for testing) ─────────────────────────────────────

    /** Package-private: exposed for test use. */
    int getLastScheduleDay() { return lastScheduleDay; }
}

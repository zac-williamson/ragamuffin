package ragamuffin.core;

import ragamuffin.building.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Issue #789: A single lot in the boot sale auction.
 *
 * <p>Immutable description of what is being auctioned: the item, the price,
 * the timer, which faction sourced it, and how risky winning it is.
 */
public class AuctionLot {

    // ── Risk levels ───────────────────────────────────────────────────────────

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    // ── NPC bidder personalities ──────────────────────────────────────────────

    public enum BidderPersonality {
        /** Bids immediately at the start of a lot. */
        AGGRESSIVE,
        /** Waits until the final third of the timer before bidding. */
        PATIENT,
        /** Drops out once the price exceeds 1.8× startPrice. */
        CAUTIOUS
    }

    // ── NPC bidder ────────────────────────────────────────────────────────────

    /**
     * A virtual NPC competitor in the auction. Holds a maxBid and a personality
     * that determines when/how it bids.
     */
    public static class NpcBidder {
        private final String name;
        private final int maxBid;
        private final BidderPersonality personality;
        private int currentBid = 0;
        private boolean active = true;

        public NpcBidder(String name, int maxBid, BidderPersonality personality) {
            if (maxBid <= 0) throw new IllegalArgumentException("maxBid must be positive");
            this.name = name;
            this.maxBid = maxBid;
            this.personality = personality;
        }

        public String getName() { return name; }
        public int getMaxBid() { return maxBid; }
        public BidderPersonality getPersonality() { return personality; }
        public int getCurrentBid() { return currentBid; }
        public boolean isActive() { return active; }

        public void placeBid(int amount) {
            if (amount > currentBid) {
                currentBid = amount;
            }
        }

        public void dropOut() {
            active = false;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final Material material;
    private final int quantity;
    private final int startPrice;
    private final int buyNowPrice;
    private final float durationSeconds;
    private final Faction sellerFaction;
    private final RiskLevel riskLevel;
    private final List<NpcBidder> bidders;

    /** Current highest bid (may be by player or an NPC bidder). */
    private int currentPrice;

    /** Name of the current highest bidder, or null if no bids yet. */
    private String highestBidder;

    /** How much time remains on this lot (counts down from durationSeconds). */
    private float timeRemaining;

    /** Whether this lot has concluded (timer expired or bought now). */
    private boolean concluded = false;

    /** Whether the player won this lot. */
    private boolean playerWon = false;

    /** Bid history log — last entries shown in the UI (most recent last). */
    private final List<String> bidHistory = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    public AuctionLot(Material material, int quantity, int startPrice,
                      Faction sellerFaction, RiskLevel riskLevel,
                      float durationSeconds, List<NpcBidder> bidders) {
        if (material == null)   throw new IllegalArgumentException("material must not be null");
        if (quantity <= 0)      throw new IllegalArgumentException("quantity must be positive");
        if (startPrice <= 0)    throw new IllegalArgumentException("startPrice must be positive");
        if (durationSeconds < 30f || durationSeconds > 90f)
            throw new IllegalArgumentException("durationSeconds must be 30–90");
        if (riskLevel == null)  throw new IllegalArgumentException("riskLevel must not be null");

        this.material        = material;
        this.quantity        = quantity;
        this.startPrice      = startPrice;
        this.buyNowPrice     = startPrice * 2;
        this.durationSeconds = durationSeconds;
        this.sellerFaction   = sellerFaction;
        this.riskLevel       = riskLevel;
        this.bidders         = bidders != null ? new ArrayList<>(bidders) : new ArrayList<>();
        this.currentPrice    = startPrice;
        this.timeRemaining   = durationSeconds;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Material getMaterial()         { return material; }
    public int getQuantity()              { return quantity; }
    public int getStartPrice()            { return startPrice; }
    public int getBuyNowPrice()           { return buyNowPrice; }
    public float getDurationSeconds()     { return durationSeconds; }
    public Faction getSellerFaction()     { return sellerFaction; }
    public RiskLevel getRiskLevel()       { return riskLevel; }
    public List<NpcBidder> getBidders()   { return Collections.unmodifiableList(bidders); }
    public int getCurrentPrice()          { return currentPrice; }
    public String getHighestBidder()      { return highestBidder; }
    public float getTimeRemaining()       { return timeRemaining; }
    public boolean isConcluded()          { return concluded; }
    public boolean isPlayerWon()          { return playerWon; }
    public List<String> getBidHistory()   { return Collections.unmodifiableList(bidHistory); }

    // ── Mutation (called by BootSaleSystem) ───────────────────────────────────

    /**
     * Record a bid by the player.
     *
     * @param amount the coin amount the player is bidding
     * @return true if the bid was accepted (higher than current price)
     */
    public boolean placeBid(String bidderName, int amount) {
        if (concluded || amount <= currentPrice) return false;
        currentPrice = amount;
        highestBidder = bidderName;
        bidHistory.add(bidderName + " bid " + amount);
        return true;
    }

    /**
     * Advance NPC bidder logic for one frame.
     * AGGRESSIVE bidders bid within the first 10% of the duration.
     * PATIENT bidders wait until the last 33% of the duration.
     * CAUTIOUS bidders drop out if price > 1.8× startPrice.
     *
     * @param elapsed how many seconds have elapsed so far (= durationSeconds − timeRemaining)
     * @param rand    seeded random for increments
     */
    public void updateNpcBidders(float elapsed, java.util.Random rand) {
        if (concluded) return;

        float fraction = elapsed / durationSeconds; // 0 = start, 1 = end

        for (NpcBidder bidder : bidders) {
            if (!bidder.isActive()) continue;

            // CAUTIOUS: drop out if price too high
            if (bidder.getPersonality() == BidderPersonality.CAUTIOUS) {
                if (currentPrice > startPrice * 1.8f) {
                    bidder.dropOut();
                    continue;
                }
            }

            // Decide whether to bid now
            boolean shouldBid = false;
            switch (bidder.getPersonality()) {
                case AGGRESSIVE:
                    // Bids in the first 25% of time and also when outbid
                    if (fraction <= 0.25f && bidder.getCurrentBid() == 0) {
                        shouldBid = true;
                    } else if (currentPrice > bidder.getCurrentBid() && rand.nextFloat() < 0.3f) {
                        shouldBid = true;
                    }
                    break;
                case PATIENT:
                    // Waits until the last third (fraction >= 0.667)
                    if (fraction >= 0.667f && currentPrice < bidder.getMaxBid()) {
                        if (bidder.getCurrentBid() < currentPrice) {
                            shouldBid = true;
                        }
                    }
                    break;
                case CAUTIOUS:
                    // Bids slowly when below 1.5× start
                    if (currentPrice < startPrice * 1.5f && currentPrice >= bidder.getCurrentBid()
                            && rand.nextFloat() < 0.2f) {
                        shouldBid = true;
                    }
                    break;
            }

            if (shouldBid) {
                int nextBid = currentPrice + 1 + rand.nextInt(3);
                if (nextBid <= bidder.getMaxBid()) {
                    placeBid(bidder.getName(), nextBid);
                    bidder.placeBid(nextBid);
                } else if (bidder.getMaxBid() > currentPrice) {
                    placeBid(bidder.getName(), bidder.getMaxBid());
                    bidder.placeBid(bidder.getMaxBid());
                } else {
                    bidder.dropOut();
                }
            }
        }
    }

    /** Advance the countdown timer. */
    public void tickTimer(float delta) {
        if (!concluded) {
            timeRemaining = Math.max(0f, timeRemaining - delta);
        }
    }

    /** Mark the lot as concluded. */
    public void conclude(boolean playerWon) {
        this.concluded = true;
        this.playerWon = playerWon;
    }
}

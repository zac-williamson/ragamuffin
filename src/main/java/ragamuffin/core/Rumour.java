package ragamuffin.core;

/**
 * A single piece of gossip carried by NPCs in the rumour network.
 * Rumours spread between NPCs when they pass within 4 blocks of each other,
 * and expire after 5 hops.
 */
public class Rumour {

    /** Maximum number of hops before a rumour expires. */
    public static final int MAX_HOPS = 5;

    private final RumourType type;
    private final String text;
    private int hops;

    /**
     * Create a new rumour with hop count 0.
     *
     * @param type the category of rumour
     * @param text the human-readable gossip text
     */
    public Rumour(RumourType type, String text) {
        this.type = type;
        this.text = text;
        this.hops = 0;
    }

    /**
     * Internal constructor used when spreading a rumour (incrementing hops).
     */
    private Rumour(RumourType type, String text, int hops) {
        this.type = type;
        this.text = text;
        this.hops = hops;
    }

    public RumourType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public int getHops() {
        return hops;
    }

    /**
     * Returns true if this rumour has expired (reached MAX_HOPS).
     */
    public boolean isExpired() {
        return hops >= MAX_HOPS;
    }

    /**
     * Increment the hop counter and return this rumour (mutates in place).
     * After this call, call {@link #isExpired()} to check if the rumour should be removed.
     */
    public void incrementHops() {
        hops++;
    }

    /**
     * Create a copy of this rumour with hop count incremented by 1.
     * Used when spreading the rumour to another NPC.
     */
    public Rumour spread() {
        return new Rumour(type, text, hops + 1);
    }

    @Override
    public String toString() {
        return "[" + type + " hops=" + hops + "] " + text;
    }
}

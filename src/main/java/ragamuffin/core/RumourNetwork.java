package ragamuffin.core;

import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the NPC rumour network for Phase 8b.
 *
 * <p>NPCs carry up to 3 rumours. When two NPCs pass within 4 blocks of each
 * other they exchange rumours (at most once per pair per 60 seconds). Each
 * rumour tracks how many hops it has taken; at 5 hops it expires. The BARMAN
 * NPC is a rumour sink that accumulates up to 10 rumours and never expires them.
 * NPCs entering the pub always share with the barman.
 */
public class RumourNetwork {

    /** Maximum number of rumours a regular NPC can hold. */
    public static final int NPC_MAX_RUMOURS = 3;

    /** Maximum number of rumours the barman accumulates. */
    public static final int BARMAN_MAX_RUMOURS = 10;

    /** Distance (in blocks) within which NPCs exchange rumours. */
    public static final float SPREAD_DISTANCE = 4f;

    /** Minimum seconds between rumour exchanges for the same pair of NPCs. */
    public static final float EXCHANGE_COOLDOWN = 60f;

    private final Random random;

    // Tracks the last time each pair of NPCs exchanged rumours.
    // Key: canonical pair id (smaller npc hashCode first).
    private final java.util.Map<Long, Float> exchangeCooldowns = new java.util.HashMap<>();

    public RumourNetwork(Random random) {
        this.random = random;
    }

    /**
     * Update the rumour network for one frame.
     *
     * <p>For each pair of NPCs within {@link #SPREAD_DISTANCE} whose exchange
     * cooldown has elapsed, one NPC shares their most-recent rumour with the other.
     * Expired rumours are pruned from all NPC buffers.
     *
     * @param npcs  all living NPCs in the world
     * @param delta seconds since last update
     */
    public void update(List<NPC> npcs, float delta) {
        // Advance all exchange cooldowns
        List<Long> toRemove = new ArrayList<>();
        for (java.util.Map.Entry<Long, Float> entry : exchangeCooldowns.entrySet()) {
            float remaining = entry.getValue() - delta;
            if (remaining <= 0f) {
                toRemove.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }
        for (Long key : toRemove) {
            exchangeCooldowns.remove(key);
        }

        // Check all pairs
        int n = npcs.size();
        for (int i = 0; i < n; i++) {
            NPC a = npcs.get(i);
            if (!a.isAlive()) continue;
            for (int j = i + 1; j < n; j++) {
                NPC b = npcs.get(j);
                if (!b.isAlive()) continue;

                // Check distance
                if (a.getPosition().dst(b.getPosition()) > SPREAD_DISTANCE) continue;

                // Check cooldown
                long pairKey = pairKey(a, b);
                if (exchangeCooldowns.containsKey(pairKey)) continue;

                // Attempt exchange
                tryExchange(a, b);
                exchangeCooldowns.put(pairKey, EXCHANGE_COOLDOWN);
            }
        }

        // Prune expired rumours from all NPCs
        for (NPC npc : npcs) {
            pruneExpired(npc);
        }
    }

    /**
     * Try to exchange a rumour between two NPCs. One random NPC is the speaker.
     * The barman always receives; regular NPCs receive up to their buffer limit.
     */
    private void tryExchange(NPC speaker, NPC listener) {
        // Randomly pick who speaks
        if (random.nextBoolean()) {
            NPC tmp = speaker;
            speaker = listener;
            listener = tmp;
        }

        // If the barman is involved, always make the non-barman the speaker
        if (listener.getType() == NPCType.BARMAN && speaker.getType() != NPCType.BARMAN) {
            // speaker is already correct
        } else if (speaker.getType() == NPCType.BARMAN && listener.getType() != NPCType.BARMAN) {
            // swap: listener speaks to barman
            NPC tmp = speaker;
            speaker = listener;
            listener = tmp;
        }

        List<Rumour> speakerRumours = speaker.getRumours();
        if (speakerRumours.isEmpty()) return;

        // Pick the speaker's most-recent (last-added) rumour
        Rumour toSpread = speakerRumours.get(speakerRumours.size() - 1);

        // Barman accumulates; regular NPCs respect the buffer limit
        int listenerMax = (listener.getType() == NPCType.BARMAN) ? BARMAN_MAX_RUMOURS : NPC_MAX_RUMOURS;
        List<Rumour> listenerRumours = listener.getRumours();

        // Don't add a duplicate of the same type+text
        for (Rumour existing : listenerRumours) {
            if (existing.getText().equals(toSpread.getText())) return;
        }

        if (listenerRumours.size() >= listenerMax) {
            // Evict oldest (first) rumour to make room (only for regular NPCs)
            if (listener.getType() != NPCType.BARMAN) {
                listenerRumours.remove(0);
            } else {
                return; // barman buffer full
            }
        }

        Rumour spread = toSpread.spread();
        listenerRumours.add(spread);
    }

    /**
     * Make an NPC share all their rumours with the barman (called when entering the pub).
     *
     * @param visitor the NPC entering the pub
     * @param barman  the BARMAN NPC
     */
    public void shareWithBarman(NPC visitor, NPC barman) {
        List<Rumour> barmanRumours = barman.getRumours();
        for (Rumour r : visitor.getRumours()) {
            if (barmanRumours.size() >= BARMAN_MAX_RUMOURS) break;
            // Don't duplicate
            boolean dup = false;
            for (Rumour existing : barmanRumours) {
                if (existing.getText().equals(r.getText())) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                barmanRumours.add(r.spread());
            }
        }
    }

    /**
     * Add a new rumour directly to an NPC's buffer (e.g. world-gen seeding).
     * Evicts oldest if the buffer is full.
     */
    public void addRumour(NPC npc, Rumour rumour) {
        List<Rumour> rumours = npc.getRumours();
        int max = (npc.getType() == NPCType.BARMAN) ? BARMAN_MAX_RUMOURS : NPC_MAX_RUMOURS;
        if (rumours.size() >= max) {
            rumours.remove(0);
        }
        rumours.add(rumour);
    }

    /**
     * Remove expired rumours from an NPC's buffer.
     * The barman's rumours never expire.
     */
    private void pruneExpired(NPC npc) {
        if (npc.getType() == NPCType.BARMAN) return; // barman accumulates forever
        npc.getRumours().removeIf(Rumour::isExpired);
    }

    /**
     * Canonical pair key for two NPCs (order-independent).
     */
    private long pairKey(NPC a, NPC b) {
        int ha = System.identityHashCode(a);
        int hb = System.identityHashCode(b);
        if (ha < hb) {
            return ((long) ha << 32) | (hb & 0xFFFFFFFFL);
        } else {
            return ((long) hb << 32) | (ha & 0xFFFFFFFFL);
        }
    }
}

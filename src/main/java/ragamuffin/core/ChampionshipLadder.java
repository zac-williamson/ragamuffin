package ragamuffin.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Issue #801: The Underground Fight Night — Championship Ladder.
 *
 * <p>A persistent ranked list of up to {@link #LADDER_SIZE} fighters. The fighter at
 * rank 1 (index 0) holds the {@code Material.CHAMPIONSHIP_BELT} and earns a passive
 * +5 Notoriety/day. After each fight the winner climbs one rung and the loser drops
 * one rung.
 *
 * <p>Marchetti's fighter "Vinnie" is always seeded onto the ladder at generation time.
 * If Vinnie reaches rank 1 before the player, the Marchetti faction receives a −15
 * respect penalty to the player (handled by {@code FightNightSystem}).
 */
public class ChampionshipLadder {

    /** Number of ranked positions on the ladder. */
    public static final int LADDER_SIZE = 8;

    /** Name of the Marchetti proxy fighter. */
    public static final String VINNIE_NAME = "Vinnie";

    // ── Entry ─────────────────────────────────────────────────────────────────

    /**
     * A single entry on the championship ladder.
     */
    public static class Entry {

        private final String fighterName;
        private int rank; // 1-based; 1 = champion

        Entry(String fighterName, int rank) {
            this.fighterName = fighterName;
            this.rank = rank;
        }

        /** Fighter's display name (or "Player" for the player). */
        public String getFighterName() {
            return fighterName;
        }

        /** Current rank (1 = champion). */
        public int getRank() {
            return rank;
        }

        @Override
        public String toString() {
            return rank + ". " + fighterName;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Ordered list of ladder entries (index 0 = rank 1 = champion). */
    private final List<Entry> entries = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Create an empty ladder.  Call {@link #initDefault()} to populate with the
     * standard set of named fighters.
     */
    public ChampionshipLadder() {
    }

    /**
     * Populate the ladder with the default 8 fighter names, including Vinnie
     * (Marchetti's proxy) at rank 5 by default.
     */
    public void initDefault() {
        entries.clear();
        String[] names = {
            "Mad Terry", "The Postman", "Gaz Two Fingers", "Big Dave",
            VINNIE_NAME, "Knuckles McGrath", "The Quiet One", "Southpaw Steve"
        };
        for (int i = 0; i < names.length; i++) {
            entries.add(new Entry(names[i], i + 1));
        }
    }

    // ── Ladder manipulation ───────────────────────────────────────────────────

    /**
     * Register a new fighter on the ladder at the bottom rung.
     * If the ladder already contains a fighter with the same name, this is a no-op.
     * If the ladder is full the bottom-ranked current entry is displaced.
     *
     * @param fighterName the fighter's display name
     */
    public void registerFighter(String fighterName) {
        for (Entry e : entries) {
            if (e.getFighterName().equals(fighterName)) {
                return; // Already on the ladder
            }
        }
        int newRank = entries.size() + 1;
        if (entries.size() >= LADDER_SIZE) {
            // Displace last entry
            entries.remove(entries.size() - 1);
            newRank = LADDER_SIZE;
        }
        entries.add(new Entry(fighterName, newRank));
        reindex();
    }

    /**
     * After a fight: winner climbs one rung, loser drops one rung.
     * Clamped to the ladder bounds.
     *
     * @param winnerName display name of the winning fighter
     * @param loserName  display name of the losing fighter
     */
    public void updateAfterFight(String winnerName, String loserName) {
        Entry winner = getEntry(winnerName);
        Entry loser  = getEntry(loserName);

        if (winner != null && winner.rank > 1) {
            // Swap winner with the fighter one rung above
            Entry displaced = getEntryAtRank(winner.rank - 1);
            if (displaced != null && !displaced.getFighterName().equals(loserName)) {
                displaced.rank++;
            }
            winner.rank--;
        }

        if (loser != null && loser.rank < entries.size()) {
            // Swap loser with the fighter one rung below
            Entry displaced = getEntryAtRank(loser.rank + 1);
            if (displaced != null && !displaced.getFighterName().equals(winnerName)) {
                displaced.rank--;
            }
            loser.rank++;
        }

        reindex();
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns all ladder entries sorted by rank (rank 1 first).
     */
    public List<Entry> getEntries() {
        List<Entry> sorted = new ArrayList<>(entries);
        Collections.sort(sorted, (a, b) -> Integer.compare(a.rank, b.rank));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Returns the current rank of a fighter, or -1 if not on the ladder.
     */
    public int getRank(String fighterName) {
        for (Entry e : entries) {
            if (e.getFighterName().equals(fighterName)) {
                return e.rank;
            }
        }
        return -1;
    }

    /**
     * Returns the name of the current rank-1 champion, or null if the ladder is empty.
     */
    public String getChampionName() {
        Entry champ = getEntryAtRank(1);
        return champ == null ? null : champ.getFighterName();
    }

    /**
     * Returns true if the given fighter is at rank 1 (champion).
     */
    public boolean isChampion(String fighterName) {
        return fighterName != null && fighterName.equals(getChampionName());
    }

    /**
     * Returns the number of fighters currently on the ladder.
     */
    public int size() {
        return entries.size();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Entry getEntry(String name) {
        if (name == null) return null;
        for (Entry e : entries) {
            if (e.getFighterName().equals(name)) return e;
        }
        return null;
    }

    private Entry getEntryAtRank(int rank) {
        for (Entry e : entries) {
            if (e.rank == rank) return e;
        }
        return null;
    }

    /** Reindex all entries so ranks are contiguous starting from 1. */
    private void reindex() {
        Collections.sort(entries, (a, b) -> Integer.compare(a.rank, b.rank));
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).rank = i + 1;
        }
    }
}

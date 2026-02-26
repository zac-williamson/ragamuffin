package ragamuffin.core;

import java.util.Random;

/**
 * The fruit machine mini-game in the pub (Phase 8b / Issue #696).
 *
 * <p>Costs 1 COIN to play. Spins three symbol slots with a seeded RNG.
 * <ul>
 *   <li>Three matching symbols (1-in-27): win 9 coins.</li>
 *   <li>Two matching symbols (6-in-27): win 2 coins.</li>
 *   <li>No match: lose the coin.</li>
 * </ul>
 */
public class FruitMachine {

    /** The number of symbol slots. */
    public static final int SLOT_COUNT = 3;

    /** Number of distinct symbols (so chance of triple match = 1/27). */
    public static final int SYMBOL_COUNT = 3;

    /** Coin cost per play. */
    public static final int COST = 1;

    /** Payout for triple match. */
    public static final int WIN_TRIPLE = 9;

    /** Payout for pair match. */
    public static final int WIN_PAIR = 2;

    /** Payout for no match. */
    public static final int WIN_NONE = 0;

    // Emoji-like character symbols used for display
    public static final String[] SYMBOLS = { "@", "#", "$" };

    private final Random random;

    public FruitMachine(Random random) {
        this.random = random;
    }

    /**
     * Result of a single spin.
     */
    public static class SpinResult {
        /** The three symbols that came up. */
        public final int[] slots;
        /** How many coins the player wins (0 = no win, loses their coin). */
        public final int payout;
        /** Human-readable display string. */
        public final String displayText;

        public SpinResult(int[] slots, int payout) {
            this.slots = slots;
            this.payout = payout;
            StringBuilder sb = new StringBuilder("[ ");
            for (int i = 0; i < slots.length; i++) {
                if (i > 0) sb.append("  ");
                sb.append(SYMBOLS[slots[i]]);
            }
            sb.append(" ]");
            if (payout == WIN_TRIPLE) {
                sb.append(" — JACKPOT! +9 coins");
            } else if (payout == WIN_PAIR) {
                sb.append(" — Lucky pair! +2 coins");
            } else {
                sb.append(" — No match. Better luck next time.");
            }
            this.displayText = sb.toString();
        }
    }

    /**
     * Spin the fruit machine and return the result.
     * The caller is responsible for deducting the cost coin and adding payout coins.
     */
    public SpinResult spin() {
        int[] slots = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = random.nextInt(SYMBOL_COUNT);
        }

        int payout;
        if (slots[0] == slots[1] && slots[1] == slots[2]) {
            payout = WIN_TRIPLE;
        } else if (slots[0] == slots[1] || slots[1] == slots[2] || slots[0] == slots[2]) {
            payout = WIN_PAIR;
        } else {
            payout = WIN_NONE;
        }

        return new SpinResult(slots, payout);
    }
}

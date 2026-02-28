package ragamuffin.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #954: A 5×5 bingo card containing unique numbers drawn from 1–90.
 *
 * <p>The centre square (row 2, col 2) is a free square (value 0, always dabbed).
 * The remaining 24 squares contain a random selection of unique numbers from 1–90.
 *
 * <p>A card is "rigged" when created from a {@code RIGGED_BINGO_CARD} item: the
 * constructor pre-dabs 22 of the 24 numbered squares, leaving only 3 remaining.
 */
public class BingoCard {

    /** Size of the bingo grid (5×5). */
    public static final int SIZE = 5;

    /** The centre square index — always a free square. */
    public static final int FREE_CENTRE_ROW = 2;
    public static final int FREE_CENTRE_COL = 2;

    /** Number of squares on a rigged card that are pre-dabbed (leaving 3 undabbed). */
    public static final int RIGGED_PRE_DABBED = 22;

    /** The grid of numbers. 0 = free centre. */
    private final int[][] numbers;

    /** Whether each square has been dabbed. */
    private final boolean[][] dabbed;

    /** Whether this card was created with a RIGGED_BINGO_CARD item. */
    private final boolean rigged;

    /** How many squares have been dabbed (including the free centre). */
    private int dabbedCount;

    // ── Construction ─────────────────────────────────────────────────────────

    /**
     * Create a new fair bingo card with 24 random numbers (1–90) and a free centre.
     *
     * @param random RNG used to shuffle numbers
     */
    public BingoCard(Random random) {
        this(random, false);
    }

    /**
     * Create a bingo card, optionally rigged.
     *
     * @param random RNG used to shuffle numbers
     * @param rigged if true, pre-dabs 22 squares so only 3 more are needed for FULL HOUSE
     */
    public BingoCard(Random random, boolean rigged) {
        this.numbers = new int[SIZE][SIZE];
        this.dabbed  = new boolean[SIZE][SIZE];
        this.rigged  = rigged;

        // Build pool of 1–90 and shuffle
        List<Integer> pool = new ArrayList<>(90);
        for (int i = 1; i <= 90; i++) {
            pool.add(i);
        }
        Collections.shuffle(pool, random);

        // Fill grid; skip centre (free square)
        int poolIndex = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (row == FREE_CENTRE_ROW && col == FREE_CENTRE_COL) {
                    numbers[row][col] = 0; // Free square
                    dabbed[row][col]  = true;
                } else {
                    numbers[row][col] = pool.get(poolIndex++);
                    dabbed[row][col]  = false;
                }
            }
        }

        // Count the free centre
        dabbedCount = 1;

        // If rigged, pre-dab RIGGED_PRE_DABBED squares (leaving 3 undabbed)
        if (rigged) {
            preDabRiggedCard(random);
        }
    }

    /**
     * Pre-dab 22 non-centre squares, leaving exactly 3 undabbed numbered squares.
     */
    private void preDabRiggedCard(Random random) {
        // Collect all non-centre non-dabbed positions
        List<int[]> positions = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (row == FREE_CENTRE_ROW && col == FREE_CENTRE_COL) continue;
                positions.add(new int[]{row, col});
            }
        }
        // Shuffle and dab the first RIGGED_PRE_DABBED
        Collections.shuffle(positions, random);
        int toDab = Math.min(RIGGED_PRE_DABBED, positions.size());
        for (int i = 0; i < toDab; i++) {
            int r = positions.get(i)[0];
            int c = positions.get(i)[1];
            if (!dabbed[r][c]) {
                dabbed[r][c] = true;
                dabbedCount++;
            }
        }
    }

    // ── Dabbing ───────────────────────────────────────────────────────────────

    /**
     * Mark a number as dabbed if it appears on this card.
     *
     * @param number the called number (1–90)
     * @return true if the number was on the card and newly dabbed
     */
    public boolean dab(int number) {
        if (number <= 0) return false;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (numbers[row][col] == number && !dabbed[row][col]) {
                    dabbed[row][col] = true;
                    dabbedCount++;
                    return true;
                }
            }
        }
        return false;
    }

    // ── Win detection ─────────────────────────────────────────────────────────

    /**
     * Returns true if the player has completed at least one full row, column, or diagonal.
     */
    public boolean hasLine() {
        // Rows
        for (int row = 0; row < SIZE; row++) {
            boolean complete = true;
            for (int col = 0; col < SIZE; col++) {
                if (!dabbed[row][col]) { complete = false; break; }
            }
            if (complete) return true;
        }
        // Columns
        for (int col = 0; col < SIZE; col++) {
            boolean complete = true;
            for (int row = 0; row < SIZE; row++) {
                if (!dabbed[row][col]) { complete = false; break; }
            }
            if (complete) return true;
        }
        // Main diagonal
        boolean diag1 = true;
        boolean diag2 = true;
        for (int i = 0; i < SIZE; i++) {
            if (!dabbed[i][i])          diag1 = false;
            if (!dabbed[i][SIZE - 1 - i]) diag2 = false;
        }
        return diag1 || diag2;
    }

    /**
     * Returns true if all 25 squares are dabbed (full house).
     */
    public boolean hasFullHouse() {
        return dabbedCount >= SIZE * SIZE;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the number at the given cell (0 = free centre).
     */
    public int getNumber(int row, int col) {
        return numbers[row][col];
    }

    /**
     * Returns whether the given cell has been dabbed.
     */
    public boolean isDabbed(int row, int col) {
        return dabbed[row][col];
    }

    /**
     * Returns the total number of squares currently dabbed (including free centre).
     */
    public int getDabbedCount() {
        return dabbedCount;
    }

    /**
     * Returns whether this is a rigged card.
     */
    public boolean isRigged() {
        return rigged;
    }

    /**
     * Returns the 5×5 numbers grid (read-only copy).
     */
    public int[][] getNumbers() {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(numbers[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    /**
     * Returns the 5×5 dabbed grid (read-only copy).
     */
    public boolean[][] getDabbed() {
        boolean[][] copy = new boolean[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(dabbed[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    // ── Force-set for testing ─────────────────────────────────────────────────

    /**
     * Force-dab a specific cell (for testing). Does not re-count.
     */
    public void setDabbedForTesting(int row, int col, boolean value) {
        if (dabbed[row][col] != value) {
            dabbed[row][col] = value;
            dabbedCount += value ? 1 : -1;
        }
    }

    /**
     * Force-dab all squares except the specified remaining count (for testing).
     * Useful to set up a card that is one number away from a line or full house.
     *
     * @param leavingUndabbed number of non-centre squares to leave undabbed
     */
    public void forceAlmostComplete(int leavingUndabbed) {
        // Reset all non-centre squares to undabbed
        dabbedCount = 1; // free centre
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (r == FREE_CENTRE_ROW && c == FREE_CENTRE_COL) continue;
                dabbed[r][c] = false;
            }
        }
        // Dab all except the last leavingUndabbed
        int undabbedLeft = leavingUndabbed;
        for (int r = SIZE - 1; r >= 0; r--) {
            for (int c = SIZE - 1; c >= 0; c--) {
                if (r == FREE_CENTRE_ROW && c == FREE_CENTRE_COL) continue;
                if (undabbedLeft > 0) {
                    undabbedLeft--;
                } else {
                    dabbed[r][c] = true;
                    dabbedCount++;
                }
            }
        }
    }

    /**
     * Returns the number at the last undabbed, non-centre square (for testing convenience).
     * Returns -1 if none found.
     */
    public int getFirstUndabbedNumber() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (r == FREE_CENTRE_ROW && c == FREE_CENTRE_COL) continue;
                if (!dabbed[r][c]) return numbers[r][c];
            }
        }
        return -1;
    }
}

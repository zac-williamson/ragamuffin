package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;

/**
 * Issue #1225: Northfield Citizens Advice Bureau — debt advice integration.
 *
 * <p>The Citizens Advice Bureau is a voluntary-sector landmark ({@code CITIZENS_ADVICE})
 * that provides free debt advice to the player. The primary mechanic exposed here is the
 * LOAN_LEAFLET → DEBT_ADVICE_LETTER exchange, which the player can then use at
 * Fast Cash Finance to negotiate a 20% interest reduction on their next loan.
 *
 * <h3>Mechanic</h3>
 * <ol>
 *   <li>Player picks up a {@link Material#LOAN_LEAFLET} from the {@code LEAFLET_RACK_PROP}
 *       inside Fast Cash Finance (once per visit).</li>
 *   <li>Player brings it to the Citizens Advice Bureau and presses E on the
 *       {@code ADVICE_DESK_PROP}.</li>
 *   <li>{@link #exchangeLeafletForAdvice(Inventory)} is called:
 *       <ul>
 *         <li>Removes 1 LOAN_LEAFLET from inventory.</li>
 *         <li>Adds 1 DEBT_ADVICE_LETTER to inventory.</li>
 *         <li>Returns {@link ExchangeResult#SUCCESS}.</li>
 *       </ul></li>
 *   <li>The DEBT_ADVICE_LETTER is consumed by {@link PaydayLoanSystem} on the next
 *       loan application, applying a 20% reduction to the calculated interest amount.</li>
 * </ol>
 *
 * <h3>Opening hours</h3>
 * Mon–Fri 09:00–17:00 (Saturday and Sunday closed).
 */
public class CitizensAdviceSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Opening hour (Monday–Friday). */
    public static final float OPEN_HOUR  = 9.0f;

    /** Closing hour (Monday–Friday). */
    public static final float CLOSE_HOUR = 17.0f;

    /** Interest reduction multiplier applied when DEBT_ADVICE_LETTER is used on a loan. */
    public static final float INTEREST_REDUCTION = 0.20f;

    // ── Result enum ───────────────────────────────────────────────────────────

    /** Result of calling {@link #exchangeLeafletForAdvice(Inventory)}. */
    public enum ExchangeResult {
        /** Successfully exchanged; DEBT_ADVICE_LETTER added to inventory. */
        SUCCESS,
        /** Player does not have a LOAN_LEAFLET. */
        NO_LEAFLET,
        /** Bureau is closed (outside Mon–Fri 09:00–17:00). */
        CLOSED,
        /** Player already has a DEBT_ADVICE_LETTER — no need for another. */
        ALREADY_HAS_LETTER
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the bureau is currently open (set by {@link #update(TimeSystem)}). */
    private boolean open = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    /** Default constructor. */
    public CitizensAdviceSystem() {
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame to keep the open/closed state in sync with in-game time.
     *
     * @param time current in-game time
     */
    public void update(TimeSystem time) {
        int dayOfWeek = time.getDayIndex() % 7; // 0=Mon … 6=Sun
        float hour = time.getHours() + time.getMinutes() / 60f;
        open = dayOfWeek < 5 && hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    // ── Core mechanic ─────────────────────────────────────────────────────────

    /**
     * Attempt to exchange a {@link Material#LOAN_LEAFLET} for a
     * {@link Material#DEBT_ADVICE_LETTER}.
     *
     * @param inventory player inventory
     * @return result indicating success or reason for failure
     */
    public ExchangeResult exchangeLeafletForAdvice(Inventory inventory) {
        if (!open) {
            return ExchangeResult.CLOSED;
        }
        if (inventory.getItemCount(Material.DEBT_ADVICE_LETTER) > 0) {
            return ExchangeResult.ALREADY_HAS_LETTER;
        }
        if (inventory.getItemCount(Material.LOAN_LEAFLET) <= 0) {
            return ExchangeResult.NO_LEAFLET;
        }
        inventory.removeItem(Material.LOAN_LEAFLET, 1);
        inventory.addItem(Material.DEBT_ADVICE_LETTER, 1);
        return ExchangeResult.SUCCESS;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return {@code true} if the bureau is currently open. */
    public boolean isOpen() {
        return open;
    }
}

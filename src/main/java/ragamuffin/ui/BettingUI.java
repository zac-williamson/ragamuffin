package ragamuffin.ui;

import ragamuffin.core.HorseRacingSystem;
import ragamuffin.core.HorseRacingSystem.Race;
import ragamuffin.core.HorseRacingSystem.Horse;

import java.util.List;

/**
 * Issue #908: BettingUI — overlay shown when the player presses E on a TV_SCREEN
 * prop inside the bookies or betting shop.
 *
 * <p>Displays today's races, the horses and odds, the player's active bet,
 * and stake-selection controls. Rendering is intentionally headless-safe:
 * all rendering calls are no-ops without a LibGDX context.
 *
 * <p>Input is handled externally (by the game loop / InputHandler). This class
 * manages UI state only.
 */
public class BettingUI {

    // ── Layout constants ──────────────────────────────────────────────────────

    public static final float PANEL_WIDTH  = 380f;
    public static final float PANEL_HEIGHT = 460f;
    public static final float PADDING      = 12f;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean visible;

    /** Currently viewed race index (0–7). */
    private int selectedRaceIndex;

    /** Currently highlighted horse index (0–5). */
    private int selectedHorseIndex;

    /** Current stake input value. */
    private int currentStake;

    /** Last outcome message to display. */
    private String lastOutcomeMessage;

    // ── Construction ──────────────────────────────────────────────────────────

    public BettingUI() {
        this.visible = false;
        this.selectedRaceIndex = 0;
        this.selectedHorseIndex = 0;
        this.currentStake = 1;
        this.lastOutcomeMessage = null;
    }

    // ── Visibility ────────────────────────────────────────────────────────────

    public boolean isVisible() {
        return visible;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
        lastOutcomeMessage = null;
    }

    public void toggle() {
        if (visible) hide();
        else show();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public int getSelectedRaceIndex() {
        return selectedRaceIndex;
    }

    public void setSelectedRaceIndex(int index) {
        this.selectedRaceIndex = index;
        this.selectedHorseIndex = 0; // reset horse selection when changing race
    }

    public int getSelectedHorseIndex() {
        return selectedHorseIndex;
    }

    public void setSelectedHorseIndex(int index) {
        this.selectedHorseIndex = index;
    }

    /**
     * Navigate to the next race (wraps around).
     *
     * @param totalRaces total number of races today
     */
    public void nextRace(int totalRaces) {
        if (totalRaces <= 0) return;
        selectedRaceIndex = (selectedRaceIndex + 1) % totalRaces;
        selectedHorseIndex = 0;
    }

    /**
     * Navigate to the previous race (wraps around).
     *
     * @param totalRaces total number of races today
     */
    public void prevRace(int totalRaces) {
        if (totalRaces <= 0) return;
        selectedRaceIndex = (selectedRaceIndex + totalRaces - 1) % totalRaces;
        selectedHorseIndex = 0;
    }

    /**
     * Navigate to the next horse (wraps around).
     *
     * @param totalHorses horses in the current race
     */
    public void nextHorse(int totalHorses) {
        if (totalHorses <= 0) return;
        selectedHorseIndex = (selectedHorseIndex + 1) % totalHorses;
    }

    /**
     * Navigate to the previous horse.
     *
     * @param totalHorses horses in the current race
     */
    public void prevHorse(int totalHorses) {
        if (totalHorses <= 0) return;
        selectedHorseIndex = (selectedHorseIndex + totalHorses - 1) % totalHorses;
    }

    // ── Stake ─────────────────────────────────────────────────────────────────

    public int getCurrentStake() {
        return currentStake;
    }

    /**
     * Increase stake by 1 (capped at maxStake).
     */
    public void increaseStake(int maxStake) {
        currentStake = Math.min(currentStake + 1, maxStake);
    }

    /**
     * Decrease stake by 1 (capped at MIN_STAKE).
     */
    public void decreaseStake() {
        currentStake = Math.max(currentStake - 1, HorseRacingSystem.MIN_STAKE);
    }

    /**
     * Set stake directly (clamped to valid range).
     */
    public void setStake(int stake, int maxStake) {
        currentStake = Math.max(HorseRacingSystem.MIN_STAKE, Math.min(stake, maxStake));
    }

    // ── Outcome messages ──────────────────────────────────────────────────────

    public String getLastOutcomeMessage() {
        return lastOutcomeMessage;
    }

    public void setLastOutcomeMessage(String message) {
        this.lastOutcomeMessage = message;
    }

    // ── Summary text helpers (for tests and HUD) ──────────────────────────────

    /**
     * Build a summary string for the currently selected race.
     *
     * @param system the horse racing system
     * @return human-readable summary
     */
    public String buildRaceSummary(HorseRacingSystem system) {
        List<Race> races = system.getTodaysRaces();
        if (races.isEmpty()) return "No races today.";
        if (selectedRaceIndex >= races.size()) return "No such race.";

        Race race = races.get(selectedRaceIndex);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Race %d — %s\n",
                race.getRaceIndex() + 1,
                formatHour(race.getScheduledHour())));

        for (int i = 0; i < race.getHorses().size(); i++) {
            Horse h = race.getHorses().get(i);
            String prefix = (i == selectedHorseIndex) ? "> " : "  ";
            sb.append(String.format("%s%-28s %s\n", prefix, h.getName(), h.getOddsString()));
        }

        if (race.isResolved()) {
            sb.append("\nResult: ").append(race.getWinner().getName());
        }

        HorseRacingSystem.Bet bet = system.getActiveBet();
        if (bet != null && bet.getRaceIndex() == selectedRaceIndex) {
            Horse betHorse = race.getHorses().get(bet.getHorseIndex());
            sb.append(String.format("\n** Your bet: %s @ %s for %d coins **",
                    betHorse.getName(), betHorse.getOddsString(), bet.getStake()));
        }

        return sb.toString();
    }

    private String formatHour(float hour) {
        int h = (int) hour;
        int m = (int) ((hour - h) * 60f);
        return String.format("%02d:%02d", h, m);
    }
}

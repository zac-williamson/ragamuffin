package ragamuffin.core;

import ragamuffin.entity.HairstyleType;

/**
 * Issue #1039: Service menu for Kosta's Barbers.
 *
 * <p>Each cut type defines its price in COIN, the recognition reduction it grants
 * (as a fraction 0–1), the duration of the reduction window in in-game minutes,
 * and the {@link HairstyleType} the player acquires after the cut.
 *
 * <p>Recognition reduction integrates with {@link WantedSystem} (police LOS time
 * extended proportionally) and {@link DisguiseSystem} (passive modifier).
 */
public enum CutType {

    /**
     * Trim — quick tidy-up; no style change.
     * Cost: 2 COIN. Reduction: 0 % (cosmetic only). Duration: n/a.
     * Available from both BARBER_OWNER and BARBER_APPRENTICE.
     */
    TRIM(2, 0.00f, 0, HairstyleType.SHORT, "Short Back & Sides",
            "Just a tidy-up, mate."),

    /**
     * Short Back &amp; Sides — classic British style.
     * Cost: 3 COIN. Reduction: +15 %. Duration: 10 in-game minutes.
     * Results in SHORT hairstyle.
     */
    SHORT_BACK_AND_SIDES(3, 0.15f, 10, HairstyleType.SHORT, "Short Back & Sides",
            "Nice and smart."),

    /**
     * Grade 1 Buzzcut — close to the scalp all over.
     * Cost: 3 COIN. Reduction: +25 %. Duration: 10 in-game minutes.
     * Results in BUZZCUT hairstyle.
     */
    GRADE_1_BUZZCUT(3, 0.25f, 10, HairstyleType.BUZZCUT, "Grade 1 All Over",
            "Hard as nails."),

    /**
     * Fade — tapered sides blending into longer hair on top.
     * Cost: 4 COIN. Reduction: +20 %. Duration: 12 in-game minutes.
     * Results in SHORT hairstyle.
     */
    FADE(4, 0.20f, 12, HairstyleType.SHORT, "Fade",
            "Fresh fade, looking clean."),

    /**
     * Mohawk — distinctive tall central strip; memorable but double-edged.
     * Cost: 4 COIN. Reduction: +0 % (distinctive — no reduction benefit).
     * Results in MOHAWK hairstyle.
     */
    MOHAWK(4, 0.00f, 0, HairstyleType.MOHAWK, "Mohawk",
            "You'll stand out, I'll tell you that."),

    /**
     * Head Shave — entirely bald, maximum anonymity.
     * Cost: 3 COIN. Reduction: +30 %. Duration: 15 in-game minutes.
     * Results in NONE hairstyle (bald).
     */
    HEAD_SHAVE(3, 0.30f, 15, HairstyleType.NONE, "Head Shave",
            "Clean as a whistle, mate.");

    // ── Fields ────────────────────────────────────────────────────────────────

    private final int costCoin;
    private final float recognitionReduction;
    private final int durationMinutes;
    private final HairstyleType resultingHairstyle;
    private final String displayName;
    private final String barberSpeech;

    // ── Constructor ───────────────────────────────────────────────────────────

    CutType(int costCoin, float recognitionReduction, int durationMinutes,
            HairstyleType resultingHairstyle, String displayName, String barberSpeech) {
        this.costCoin             = costCoin;
        this.recognitionReduction = recognitionReduction;
        this.durationMinutes      = durationMinutes;
        this.resultingHairstyle   = resultingHairstyle;
        this.displayName          = displayName;
        this.barberSpeech         = barberSpeech;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Price in COIN for this cut. */
    public int getCostCoin() {
        return costCoin;
    }

    /**
     * Recognition reduction fraction (0–1) applied to police LOS time and
     * NPC threat perception while the window is active.
     */
    public float getRecognitionReduction() {
        return recognitionReduction;
    }

    /** Duration of the recognition reduction window in in-game minutes. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** The {@link HairstyleType} the player will have after this cut. */
    public HairstyleType getResultingHairstyle() {
        return resultingHairstyle;
    }

    /** Short display name shown in the service menu. */
    public String getDisplayName() {
        return displayName;
    }

    /** Dialogue Kosta says after completing this cut. */
    public String getBarberSpeech() {
        return barberSpeech;
    }
}

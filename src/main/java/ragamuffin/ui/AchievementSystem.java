package ragamuffin.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks achievement progress, unlocks achievements, and queues on-screen
 * notifications when an achievement is first earned.
 *
 * <p>Call {@link #increment(AchievementType)} or {@link #unlock(AchievementType)}
 * from game logic to record progress.  Call {@link #update(float)} each frame
 * so notification banners advance their countdown.  Call
 * {@link #getCurrentNotification()} to retrieve the active banner text (null
 * if none), and {@link #renderNotification} is left to the caller (typically
 * {@code RagamuffinGame.renderUI()}).
 */
public class AchievementSystem {

    /** How long each notification banner is shown (seconds). */
    public static final float NOTIFICATION_DURATION = 4.0f;

    /** Banner fade-out window: last N seconds of the display time. */
    public static final float NOTIFICATION_FADE_WINDOW = 1.0f;

    // --- Internal state ---

    /** Progress counter for each achievement type (0 until fully unlocked). */
    private final Map<AchievementType, Integer> progress;

    /** Whether each achievement has been unlocked this session. */
    private final Map<AchievementType, Boolean> unlocked;

    /** Queue of notification messages waiting to be shown. */
    private final List<String> notificationQueue;

    /** Currently displayed notification text (null = none). */
    private String currentNotification;

    /** Remaining display time for the current notification. */
    private float notificationTimer;

    /** Callback triggered when a new notification banner appears (e.g. for sound). */
    private Runnable onNotificationShow;

    // ------------------------------------------------------------------

    public AchievementSystem() {
        progress = new EnumMap<>(AchievementType.class);
        unlocked = new EnumMap<>(AchievementType.class);
        notificationQueue = new ArrayList<>();
        currentNotification = null;
        notificationTimer = 0f;
        onNotificationShow = null;

        for (AchievementType type : AchievementType.values()) {
            progress.put(type, 0);
            unlocked.put(type, false);
        }
    }

    // ------------------------------------------------------------------
    // Progress / unlock API
    // ------------------------------------------------------------------

    /**
     * Increment progress for an achievement by 1.
     * Unlocks the achievement when the target is reached.
     * Has no effect if the achievement is already unlocked.
     */
    public void increment(AchievementType type) {
        if (isUnlocked(type)) return;

        int current = progress.get(type) + 1;
        progress.put(type, current);

        if (current >= type.getProgressTarget()) {
            doUnlock(type);
        }
    }

    /**
     * Unconditionally unlock an achievement (progress-based or instant).
     * Has no effect if the achievement is already unlocked.
     */
    public void unlock(AchievementType type) {
        if (isUnlocked(type)) return;
        progress.put(type, type.getProgressTarget());
        doUnlock(type);
    }

    private void doUnlock(AchievementType type) {
        unlocked.put(type, true);
        String banner = "Achievement unlocked: " + type.getName();
        notificationQueue.add(banner);
    }

    // ------------------------------------------------------------------
    // Query API
    // ------------------------------------------------------------------

    public boolean isUnlocked(AchievementType type) {
        return Boolean.TRUE.equals(unlocked.get(type));
    }

    public int getProgress(AchievementType type) {
        return progress.getOrDefault(type, 0);
    }

    public int getUnlockedCount() {
        int count = 0;
        for (Boolean v : unlocked.values()) {
            if (Boolean.TRUE.equals(v)) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return AchievementType.values().length;
    }

    // ------------------------------------------------------------------
    // Notification tick
    // ------------------------------------------------------------------

    /**
     * Set a callback invoked whenever a new notification banner is shown.
     * Useful for playing a sound effect.
     */
    public void setOnNotificationShow(Runnable callback) {
        this.onNotificationShow = callback;
    }

    /**
     * Advance the notification countdown.  Call once per frame.
     */
    public void update(float delta) {
        if (currentNotification != null) {
            notificationTimer -= delta;
            if (notificationTimer <= 0f) {
                currentNotification = null;
                notificationTimer = 0f;
            }
        }

        if (currentNotification == null && !notificationQueue.isEmpty()) {
            currentNotification = notificationQueue.remove(0);
            notificationTimer = NOTIFICATION_DURATION;
            if (onNotificationShow != null) {
                onNotificationShow.run();
            }
        }
    }

    /**
     * The currently active notification message, or {@code null} if none.
     */
    public String getCurrentNotification() {
        return currentNotification;
    }

    /**
     * Alpha multiplier for the notification banner based on remaining time.
     * Fades out over the last {@link #NOTIFICATION_FADE_WINDOW} seconds.
     */
    public float getNotificationAlpha() {
        if (currentNotification == null) return 0f;
        if (notificationTimer > NOTIFICATION_FADE_WINDOW) return 1f;
        return Math.max(0f, notificationTimer / NOTIFICATION_FADE_WINDOW);
    }

    public float getNotificationTimer() {
        return notificationTimer;
    }

    public boolean isNotificationActive() {
        return currentNotification != null;
    }

    // ------------------------------------------------------------------
    // Reset (new game)
    // ------------------------------------------------------------------

    public void reset() {
        for (AchievementType type : AchievementType.values()) {
            progress.put(type, 0);
            unlocked.put(type, false);
        }
        notificationQueue.clear();
        currentNotification = null;
        notificationTimer = 0f;
    }
}

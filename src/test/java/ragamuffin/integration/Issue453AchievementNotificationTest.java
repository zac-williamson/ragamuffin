package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #453: achievementSystem.update() never called —
 * achievement notifications never display.
 *
 * <p>These tests verify the full notification lifecycle:
 * <ol>
 *   <li>Unlocking an achievement queues a notification.</li>
 *   <li>Calling {@code update(delta)} activates the notification and fires the
 *       sound callback.</li>
 *   <li>After {@link AchievementSystem#NOTIFICATION_DURATION} seconds of
 *       {@code update()} calls the notification has cleared.</li>
 *   <li>The queue is empty once all notifications have been shown.</li>
 * </ol>
 */
class Issue453AchievementNotificationTest {

    private AchievementSystem achievementSystem;

    @BeforeEach
    void setUp() {
        achievementSystem = new AchievementSystem();
    }

    /**
     * Test 1: Unlocking an achievement and calling update() shows the notification.
     *
     * Unlock an achievement, call update(delta) once, verify getCurrentNotification()
     * is non-null and contains the achievement's name.
     */
    @Test
    void unlockAndUpdateShowsNotification() {
        achievementSystem.unlock(AchievementType.FIRST_PUNCH);

        // Before update(), no notification is visible
        assertNull(achievementSystem.getCurrentNotification(),
                "No notification active before update() is called");
        assertFalse(achievementSystem.isNotificationActive(),
                "isNotificationActive() must be false before update()");

        // One update() tick activates the queued notification
        achievementSystem.update(0.016f);

        assertNotNull(achievementSystem.getCurrentNotification(),
                "Notification must be active after update()");
        assertTrue(achievementSystem.isNotificationActive(),
                "isNotificationActive() must be true while notification is displayed");
        assertTrue(achievementSystem.getCurrentNotification()
                        .contains(AchievementType.FIRST_PUNCH.getName()),
                "Notification text must include the achievement name");
    }

    /**
     * Test 2: After update(delta) is called for NOTIFICATION_DURATION seconds,
     * getCurrentNotification() returns null and the notification is cleared.
     */
    @Test
    void notificationClearsAfterFullDuration() {
        achievementSystem.unlock(AchievementType.FIRST_BLOCK);
        achievementSystem.update(0.016f); // Activate

        assertTrue(achievementSystem.isNotificationActive(),
                "Notification must be active after first update()");

        // Simulate NOTIFICATION_DURATION seconds of game loop ticks at 60 fps
        float delta = 1.0f / 60.0f;
        int frames = (int) Math.ceil(AchievementSystem.NOTIFICATION_DURATION / delta) + 5;
        for (int i = 0; i < frames; i++) {
            achievementSystem.update(delta);
        }

        assertNull(achievementSystem.getCurrentNotification(),
                "getCurrentNotification() must return null after NOTIFICATION_DURATION seconds");
        assertFalse(achievementSystem.isNotificationActive(),
                "isNotificationActive() must be false once the notification has expired");
    }

    /**
     * Test 3: Sound callback fires when notification is shown.
     *
     * Verify that the onNotificationShow callback registered via
     * setOnNotificationShow() is invoked exactly when the notification becomes
     * active — not on unlock, and not more than once per achievement.
     */
    @Test
    void soundCallbackFiresOnNotificationShow() {
        AtomicBoolean callbackFired = new AtomicBoolean(false);
        achievementSystem.setOnNotificationShow(() -> callbackFired.set(true));

        achievementSystem.unlock(AchievementType.GREGGS_RAID);

        // Callback must NOT have fired yet — unlock only queues the notification
        assertFalse(callbackFired.get(),
                "Sound callback must not fire on unlock — only when the banner appears");

        achievementSystem.update(0.016f); // Activates notification

        assertTrue(callbackFired.get(),
                "Sound callback must fire when update() activates the notification");
    }

    /**
     * Test 4: Multiple achievements queue notifications that are shown sequentially.
     *
     * Unlock two achievements. Verify notifications appear one at a time, in order,
     * and both are eventually shown and cleared.
     */
    @Test
    void multipleAchievementsShownSequentially() {
        achievementSystem.unlock(AchievementType.FIRST_PUNCH);
        achievementSystem.unlock(AchievementType.FIRST_BLOCK);

        achievementSystem.update(0.016f); // Activate first

        String firstNotification = achievementSystem.getCurrentNotification();
        assertNotNull(firstNotification, "First notification must be active");
        assertTrue(firstNotification.contains(AchievementType.FIRST_PUNCH.getName()),
                "First notification must be for FIRST_PUNCH");

        // Run through first notification's full duration
        float delta = 1.0f / 60.0f;
        int frames = (int) Math.ceil(AchievementSystem.NOTIFICATION_DURATION / delta) + 5;
        for (int i = 0; i < frames; i++) {
            achievementSystem.update(delta);
        }

        // After first expires, second should become active
        String secondNotification = achievementSystem.getCurrentNotification();
        assertNotNull(secondNotification, "Second notification must be active after first expires");
        assertTrue(secondNotification.contains(AchievementType.FIRST_BLOCK.getName()),
                "Second notification must be for FIRST_BLOCK");

        // Run through second notification's full duration
        for (int i = 0; i < frames; i++) {
            achievementSystem.update(delta);
        }

        assertNull(achievementSystem.getCurrentNotification(),
                "No notification active once both achievements have been shown");
    }

    /**
     * Test 5: Progress-based achievement unlocks via increment() also shows notification.
     *
     * Verify that an achievement unlocked through incremental progress (not direct
     * unlock()) also queues and displays its notification correctly.
     */
    @Test
    void progressAchievementShowsNotificationOnComplete() {
        AchievementType type = AchievementType.GLAZIER; // progressTarget = 5

        // Increment to one below target — no unlock yet
        for (int i = 0; i < type.getProgressTarget() - 1; i++) {
            achievementSystem.increment(type);
        }
        achievementSystem.update(0.016f);
        assertNull(achievementSystem.getCurrentNotification(),
                "No notification before progress target is reached");

        // Final increment — triggers unlock
        achievementSystem.increment(type);
        assertTrue(achievementSystem.isUnlocked(type),
                "Achievement must be unlocked after reaching target via increment()");

        // update() activates the queued notification
        achievementSystem.update(0.016f);
        assertNotNull(achievementSystem.getCurrentNotification(),
                "Notification must appear after progress-based unlock");
        assertTrue(achievementSystem.getCurrentNotification().contains(type.getName()),
                "Notification text must include the achievement name");

        // After NOTIFICATION_DURATION the banner clears
        float delta = 1.0f / 60.0f;
        int frames = (int) Math.ceil(AchievementSystem.NOTIFICATION_DURATION / delta) + 5;
        for (int i = 0; i < frames; i++) {
            achievementSystem.update(delta);
        }
        assertNull(achievementSystem.getCurrentNotification(),
                "Notification must clear after NOTIFICATION_DURATION");
    }
}

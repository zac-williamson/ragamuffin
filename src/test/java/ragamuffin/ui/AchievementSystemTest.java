package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AchievementSystemTest {

    private AchievementSystem system;

    @BeforeEach
    void setUp() {
        system = new AchievementSystem();
    }

    @Test
    void unlockQueuesNotification() {
        system.unlock(AchievementType.FIRST_PUNCH);
        // Notification is queued but not yet active until update() is called
        assertNull(system.getCurrentNotification(),
                "Notification should not be active before update()");

        system.update(0.016f);
        assertNotNull(system.getCurrentNotification(),
                "Notification should be active after first update()");
        assertTrue(system.getCurrentNotification().contains(AchievementType.FIRST_PUNCH.getName()),
                "Notification text should contain achievement name");
    }

    @Test
    void notificationClearsAfterDuration() {
        system.unlock(AchievementType.FIRST_PUNCH);
        system.update(0.016f); // Activate notification

        assertNotNull(system.getCurrentNotification(), "Notification must be active");

        // Advance time past NOTIFICATION_DURATION
        float remaining = AchievementSystem.NOTIFICATION_DURATION;
        while (remaining > 0f) {
            float step = 0.1f;
            system.update(step);
            remaining -= step;
        }

        assertNull(system.getCurrentNotification(),
                "Notification must be null after NOTIFICATION_DURATION seconds");
        assertFalse(system.isNotificationActive(),
                "isNotificationActive() must return false after duration expires");
    }

    @Test
    void queueEmptiesAfterAllNotificationsShown() {
        system.unlock(AchievementType.FIRST_PUNCH);
        system.unlock(AchievementType.FIRST_BLOCK);

        // Drive first notification through
        system.update(0.016f); // Activate first
        for (float t = 0f; t < AchievementSystem.NOTIFICATION_DURATION + 0.1f; t += 0.1f) {
            system.update(0.1f);
        }
        // Now first is gone, second may be activated — drive through it too
        system.update(0.016f); // Activate second (if not already)
        for (float t = 0f; t < AchievementSystem.NOTIFICATION_DURATION + 0.1f; t += 0.1f) {
            system.update(0.1f);
        }

        assertNull(system.getCurrentNotification(),
                "No notification active after all queued notifications have shown");
        assertFalse(system.isNotificationActive(), "Queue must be empty");
    }

    @Test
    void multipleAchievementsQueueInOrder() {
        system.unlock(AchievementType.FIRST_PUNCH);
        system.unlock(AchievementType.FIRST_BLOCK);

        // First update activates first queued notification
        system.update(0.016f);
        String first = system.getCurrentNotification();
        assertNotNull(first, "First notification must be active");
        assertTrue(first.contains(AchievementType.FIRST_PUNCH.getName()),
                "First notification must be for FIRST_PUNCH");

        // Expire first notification
        for (float t = 0f; t < AchievementSystem.NOTIFICATION_DURATION + 0.1f; t += 0.1f) {
            system.update(0.1f);
        }

        // Next update activates second notification
        system.update(0.016f);
        String second = system.getCurrentNotification();
        assertNotNull(second, "Second notification must be active after first expires");
        assertTrue(second.contains(AchievementType.FIRST_BLOCK.getName()),
                "Second notification must be for FIRST_BLOCK");
    }

    @Test
    void notificationCallbackFiredOnShow() {
        AtomicInteger callCount = new AtomicInteger(0);
        system.setOnNotificationShow(callCount::incrementAndGet);

        system.unlock(AchievementType.FIRST_PUNCH);
        system.update(0.016f); // Activates notification, should fire callback

        assertEquals(1, callCount.get(),
                "onNotificationShow callback must be called exactly once when notification appears");
    }

    @Test
    void notificationCallbackFiredOncePerAchievement() {
        AtomicInteger callCount = new AtomicInteger(0);
        system.setOnNotificationShow(callCount::incrementAndGet);

        system.unlock(AchievementType.FIRST_PUNCH);
        system.unlock(AchievementType.FIRST_BLOCK);

        // Activate first
        system.update(0.016f);
        assertEquals(1, callCount.get(), "Callback fires once for first achievement");

        // Expire first, activate second
        for (float t = 0f; t < AchievementSystem.NOTIFICATION_DURATION + 0.1f; t += 0.1f) {
            system.update(0.1f);
        }
        assertEquals(2, callCount.get(), "Callback fires again for second achievement");
    }

    @Test
    void getNotificationAlphaFadesNearEnd() {
        system.unlock(AchievementType.FIRST_PUNCH);
        system.update(0.016f); // Activate

        // At full duration, alpha should be 1
        assertEquals(1f, system.getNotificationAlpha(), 0.01f,
                "Alpha must be 1.0 at the start of the notification");

        // Advance to within the fade window
        float advanceTo = AchievementSystem.NOTIFICATION_DURATION - AchievementSystem.NOTIFICATION_FADE_WINDOW / 2f;
        for (float t = 0f; t < advanceTo; t += 0.1f) {
            system.update(0.1f);
        }

        float alpha = system.getNotificationAlpha();
        assertTrue(alpha > 0f && alpha < 1f,
                "Alpha must be between 0 and 1 during the fade window, got: " + alpha);
    }

    @Test
    void noNotificationBeforeUpdate() {
        system.unlock(AchievementType.FIRST_PUNCH);
        assertNull(system.getCurrentNotification(),
                "getCurrentNotification() must be null before any update() call");
        assertFalse(system.isNotificationActive(),
                "isNotificationActive() must be false before any update() call");
    }

    @Test
    void resetClearsNotificationQueue() {
        system.unlock(AchievementType.FIRST_PUNCH);
        system.update(0.016f); // Activate

        system.reset();

        assertNull(system.getCurrentNotification(),
                "getCurrentNotification() must be null after reset()");
        assertFalse(system.isNotificationActive(),
                "isNotificationActive() must be false after reset()");
    }

    @Test
    void marathonManUnlockedAfter1000MetresWalked() {
        // Simulate the distance-accumulation logic: 1000 increments (one per metre)
        // mirrors what RagamuffinGame.updatePlayingSimulation() does.
        AchievementType type = AchievementType.MARATHON_MAN;
        assertFalse(system.isUnlocked(type), "MARATHON_MAN must not be unlocked before 1000 metres");

        for (int metre = 0; metre < 999; metre++) {
            system.increment(type);
        }
        assertFalse(system.isUnlocked(type), "MARATHON_MAN must not be unlocked at 999 metres");

        system.increment(type); // 1000th metre
        assertTrue(system.isUnlocked(type), "MARATHON_MAN must be unlocked after 1000 metres walked");
    }

    @Test
    void unlockSameAchievementTwiceDoesNotQueueTwice() {
        system.unlock(AchievementType.FIRST_PUNCH);
        system.unlock(AchievementType.FIRST_PUNCH); // Second call must be ignored

        system.update(0.016f); // Activate first

        // Expire it
        for (float t = 0f; t < AchievementSystem.NOTIFICATION_DURATION + 0.1f; t += 0.1f) {
            system.update(0.1f);
        }

        assertNull(system.getCurrentNotification(),
                "No second notification — duplicate unlock must be ignored");
    }

    @Test
    void incrementToTargetUnlocksAndQueuesNotification() {
        AchievementType type = AchievementType.BRAWLER; // progressTarget > 1
        int target = type.getProgressTarget();

        for (int i = 0; i < target - 1; i++) {
            system.increment(type);
        }
        // Not yet unlocked
        assertFalse(system.isUnlocked(type), "Achievement must not be unlocked before target");
        system.update(0.016f);
        assertNull(system.getCurrentNotification(), "No notification before target reached");

        // Final increment — triggers unlock
        system.increment(type);
        assertTrue(system.isUnlocked(type), "Achievement must be unlocked at target");
        system.update(0.016f);
        assertNotNull(system.getCurrentNotification(),
                "Notification must be queued when achievement is unlocked via increment");
    }
}

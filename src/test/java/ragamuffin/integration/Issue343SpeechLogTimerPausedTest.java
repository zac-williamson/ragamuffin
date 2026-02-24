package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.SpeechLogUI;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #343:
 * speechLogUI.update() was not called in the PAUSED render path, so speech log
 * entry timers froze while the pause menu was open. Entries that should have
 * expired during the pause remained fully visible until the next update() call
 * after the player resumed.
 *
 * Fix: call {@code speechLogUI.update(npcManager.getNPCs(), delta)} in the PAUSED
 * render block, mirroring the pattern of tooltipSystem (#331), firstPersonArm (#339),
 * and weatherSystem (#341).
 */
class Issue343SpeechLogTimerPausedTest {

    /**
     * Test 1: update() advances timers — repeated calls with a sufficient total delta
     * must expire an entry that should have timed out.
     *
     * Simulates the paused scenario: update() is called each frame with a real-time
     * delta while the game is paused. After the full ENTRY_DURATION + FADE_DURATION
     * has elapsed, the entry must be gone.
     */
    @Test
    void updateAdvancesTimers_entryExpiresAfterFullLifetime() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setSpeechText("Nice day for it!", 3.0f);

        // Add the entry
        log.update(List.of(npc), 0.016f);
        assertEquals(1, log.getEntryCount(), "Entry should be present after first update");

        // Advance past full lifetime in small ticks (simulating paused frames)
        float totalLifetime = SpeechLogUI.ENTRY_DURATION + SpeechLogUI.FADE_DURATION + 0.1f;
        float tick = totalLifetime / 60f;
        for (int i = 0; i < 60; i++) {
            log.update(List.of(npc), tick);
        }

        assertEquals(0, log.getEntryCount(),
                "Entry must expire after its full lifetime elapses via update() calls during pause");
    }

    /**
     * Test 2: Without calling update() (simulating the pre-fix paused state),
     * entry timers do NOT advance and the entry stays visible indefinitely.
     *
     * This documents the exact bug: if update() is never called during pause,
     * a speech entry that should have expired remains on-screen.
     */
    @Test
    void withoutUpdate_entryTimerFreezes_entryRemainsVisible() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.POLICE, 0, 1, 0);
        npc.setSpeechText("Move along!", 3.0f);

        // Add the entry via one update call
        log.update(List.of(npc), 0.016f);
        assertEquals(1, log.getEntryCount(), "Entry present after first update");

        // Deliberately do NOT call update() again — simulates the frozen timer
        // during pause (the pre-fix behaviour)
        // Entry must still be present — timer never advanced
        assertEquals(1, log.getEntryCount(),
                "Entry must still be present if update() is never called (timer frozen — pre-fix bug)");

        // Verify the entry is fully opaque (alpha = 1.0) since no time has passed
        SpeechLogUI.Entry entry = log.getEntries().get(0);
        assertEquals(1.0f, entry.getAlpha(), 0.001f,
                "Alpha must be 1.0 when timer has not advanced at all");
    }

    /**
     * Test 3: Entry alpha decreases as update() drains the timer into the fade window.
     *
     * After ENTRY_DURATION seconds the entry enters the fade window. A further
     * FADE_DURATION/2 seconds should yield an alpha of approximately 0.5.
     */
    @Test
    void updateDrainsFadeWindow_alphaDecreasesCorrectly() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.BUSKER, 0, 1, 0);
        npc.setSpeechText("Wonderwall!", 3.0f);

        // Add entry
        log.update(List.of(npc), 0.016f);

        // Advance to exactly halfway through the fade window
        // Total lifetime = ENTRY_DURATION + FADE_DURATION; alpha starts fading at FADE_DURATION remaining
        float advanceTo = SpeechLogUI.ENTRY_DURATION + SpeechLogUI.FADE_DURATION / 2f;
        log.update(List.of(npc), advanceTo);

        assertEquals(1, log.getEntryCount(), "Entry should still exist mid-fade");
        SpeechLogUI.Entry entry = log.getEntries().get(0);
        assertTrue(entry.getAlpha() < 1.0f,
                "Alpha must be less than 1.0 once entry enters the fade window");
        assertTrue(entry.getAlpha() > 0.0f,
                "Alpha must be greater than 0.0 midway through the fade window");
    }

    /**
     * Test 4: update() with zero delta does NOT advance timers.
     *
     * Passing 0 delta must leave the entry intact and at full alpha — consistent
     * with the WeatherSystem behaviour tested in Issue341WeatherTimerPausedTest.
     */
    @Test
    void updateWithZeroDelta_doesNotAdvanceTimer() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.DRUNK, 0, 1, 0);
        npc.setSpeechText("Hic!", 3.0f);

        log.update(List.of(npc), 0.016f);
        assertEquals(1, log.getEntryCount());

        // Pump 1000 zero-delta updates
        for (int i = 0; i < 1000; i++) {
            log.update(List.of(npc), 0f);
        }

        assertEquals(1, log.getEntryCount(),
                "Zero-delta updates must not expire the entry");
        assertEquals(1.0f, log.getEntries().get(0).getAlpha(), 0.001f,
                "Alpha must remain 1.0 when zero delta is supplied");
    }

    /**
     * Test 5: Entry that would have expired during a long pause IS expired when
     * a single large delta is supplied — mirrors the realistic post-pause scenario
     * where accumulated real time is delivered as one large delta.
     */
    @Test
    void singleLargeDelta_expiresEntryThatFrozeWhilePaused() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.PENSIONER, 0, 1, 0);
        npc.setSpeechText("Back in my day!", 3.0f);

        log.update(List.of(npc), 0.016f);
        assertEquals(1, log.getEntryCount());

        // Supply a single large delta exceeding the full entry lifetime
        float bigDelta = SpeechLogUI.ENTRY_DURATION + SpeechLogUI.FADE_DURATION + 1f;
        log.update(List.of(npc), bigDelta);

        assertEquals(0, log.getEntryCount(),
                "A single large delta must expire the entry — simulates correct behaviour after long pause");
    }
}

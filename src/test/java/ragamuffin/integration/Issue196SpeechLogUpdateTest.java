package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.SpeechLogUI;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #196:
 * SpeechLogUI.update() never called — NPC speech log always empty.
 *
 * Root cause: speechLogUI.update(npcManager.getNPCs(), delta) was never invoked
 * in the game loop. The log scans NPC speech state in update(), so without it
 * the entries deque remained empty forever and render() returned immediately.
 *
 * Fix: call speechLogUI.update() in updatePlaying() immediately after
 * npcManager.update() so that NPC speech set for the current frame is visible
 * to the log scanner.
 */
class Issue196SpeechLogUpdateTest {

    /**
     * Test 1: A speaking NPC populates the speech log when update() is called.
     * Without the fix, update() was never called and entries stayed empty.
     */
    @Test
    void speakingNPC_populatesSpeechLog_whenUpdateIsCalled() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setSpeechText("Lovely weather!", 3.0f);

        // This simulates the per-frame call now placed in updatePlaying()
        log.update(List.of(npc), 0.016f);

        assertEquals(1, log.getEntryCount(),
                "Speech log must have 1 entry after update() is called with a speaking NPC");
        assertEquals("Lovely weather!", log.getEntries().get(0).text);
    }

    /**
     * Test 2: Without calling update(), the log remains empty — confirms the bug
     * that existed before the fix. If update() is never invoked, the log never
     * populates regardless of NPC speech state.
     */
    @Test
    void speechLog_remainsEmpty_withoutUpdate() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.POLICE, 0, 1, 0);
        npc.setSpeechText("Move along!", 3.0f);

        // Deliberately do NOT call log.update() — simulates the pre-fix state
        // where updatePlaying() never called speechLogUI.update()

        assertEquals(0, log.getEntryCount(),
                "Log must be empty if update() is never called — confirming the pre-fix bug");
    }

    /**
     * Test 3: update() must be called after npcManager sets speech for the frame.
     * Calling update() before speech is set yields nothing; after yields an entry.
     * This verifies the ordering requirement: npcManager.update() then speechLogUI.update().
     */
    @Test
    void updateAfterSpeechIsSet_yieldsEntry_updateBeforeDoesNot() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.DRUNK, 0, 1, 0);

        // Call update before NPC has any speech — should log nothing
        log.update(List.of(npc), 0.016f);
        assertEquals(0, log.getEntryCount(),
                "No entry should be added when NPC is not yet speaking");

        // NPC then speaks (simulates npcManager.update() setting speech)
        npc.setSpeechText("Hic...", 3.0f);

        // update() is now called after NPC speech is set — should detect new speech
        log.update(List.of(npc), 0.016f);
        assertEquals(1, log.getEntryCount(),
                "Entry should appear once update() is called after NPC speech is set");
    }

    /**
     * Test 4: Entries expire correctly when update() advances their timers.
     * Without update() being called per-frame, timers never advance and entries
     * would accumulate forever (if the log were ever populated at all).
     */
    @Test
    void entryTimers_advanceCorrectly_overMultipleFrames() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc = new NPC(NPCType.BUSKER, 0, 1, 0);
        npc.setSpeechText("Wonderwall...", 3.0f);

        // First update — entry is added
        log.update(List.of(npc), 0.016f);
        assertEquals(1, log.getEntryCount());

        // Advance time past the full lifetime of the entry
        float totalLifetime = SpeechLogUI.ENTRY_DURATION + SpeechLogUI.FADE_DURATION + 0.5f;
        log.update(List.of(npc), totalLifetime);

        assertEquals(0, log.getEntryCount(),
                "Entry should be removed once its full lifetime has elapsed via update()");
    }

    /**
     * Test 5: Multiple NPCs speaking in the same frame each get a log entry.
     * Confirms that the update() call scans all active NPCs, not just the first.
     */
    @Test
    void multipleNPCsSpeaking_allGetLogEntries() {
        SpeechLogUI log = new SpeechLogUI();
        NPC npc1 = new NPC(NPCType.POLICE, 0, 1, 0);
        NPC npc2 = new NPC(NPCType.YOUTH_GANG, 5, 1, 0);
        NPC npc3 = new NPC(NPCType.PENSIONER, 10, 1, 0);

        npc1.setSpeechText("Stop right there!", 2.0f);
        npc2.setSpeechText("Innit though", 2.0f);
        npc3.setSpeechText("Back in my day...", 2.0f);

        log.update(List.of(npc1, npc2, npc3), 0.016f);

        assertEquals(3, log.getEntryCount(),
                "All three speaking NPCs should have entries in the speech log");
    }
}

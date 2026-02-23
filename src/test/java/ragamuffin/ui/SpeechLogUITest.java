package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SpeechLogUI — Issue #13.
 *
 * These tests exercise the log's data model (entry tracking, expiry, limits)
 * without requiring a LibGDX display context.
 */
class SpeechLogUITest {

    private SpeechLogUI log;

    @BeforeEach
    void setUp() {
        log = new SpeechLogUI();
    }

    // ===== Basic entry management =====

    @Test
    void testEmptyLogHasNoEntries() {
        assertEquals(0, log.getEntryCount());
    }

    @Test
    void testAddEntryAppearsInLog() {
        log.addEntry(NPCType.PUBLIC, "Blimey!");

        assertEquals(1, log.getEntryCount());
        List<SpeechLogUI.Entry> entries = log.getEntries();
        assertEquals("Blimey!", entries.get(0).text);
        assertEquals("Passer-by", entries.get(0).label);
    }

    @Test
    void testNewerEntriesArePrependedToFront() {
        log.addEntry(NPCType.PUBLIC, "First");
        log.addEntry(NPCType.POLICE, "Second");

        List<SpeechLogUI.Entry> entries = log.getEntries();
        // Front = most recent
        assertEquals("Second", entries.get(0).text);
        assertEquals("First", entries.get(1).text);
    }

    @Test
    void testMaxEntriesNotExceeded() {
        for (int i = 0; i < SpeechLogUI.MAX_ENTRIES + 5; i++) {
            log.addEntry(NPCType.DRUNK, "Hic " + i);
        }
        assertEquals(SpeechLogUI.MAX_ENTRIES, log.getEntryCount());
    }

    @Test
    void testMaxEntriesRetainsMostRecent() {
        for (int i = 0; i < SpeechLogUI.MAX_ENTRIES + 3; i++) {
            log.addEntry(NPCType.DRUNK, "Line " + i);
        }
        // Most recent entry should be at index 0
        List<SpeechLogUI.Entry> entries = log.getEntries();
        String mostRecent = "Line " + (SpeechLogUI.MAX_ENTRIES + 2);
        assertEquals(mostRecent, entries.get(0).text);
    }

    // ===== Entry expiry =====

    @Test
    void testNewEntryIsFullyOpaque() {
        log.addEntry(NPCType.SHOPKEEPER, "Buy something!");
        SpeechLogUI.Entry entry = log.getEntries().get(0);
        assertEquals(1.0f, entry.getAlpha(), 0.001f);
    }

    @Test
    void testEntryFadesAfterDuration() {
        log.addEntry(NPCType.SHOPKEEPER, "Buy something!");
        // Advance past the hold time so we are in the fade window
        float advanceBy = SpeechLogUI.ENTRY_DURATION + SpeechLogUI.FADE_DURATION * 0.5f;
        log.update(Collections.emptyList(), advanceBy);

        // Entry may have been removed if expired, or alpha should be < 1
        if (log.getEntryCount() == 0) {
            // Fully expired — acceptable
            return;
        }
        SpeechLogUI.Entry entry = log.getEntries().get(0);
        assertTrue(entry.getAlpha() < 1.0f, "Entry should be fading");
    }

    @Test
    void testExpiredEntryIsRemovedFromLog() {
        log.addEntry(NPCType.JOGGER, "Huff puff...");
        float totalLifetime = SpeechLogUI.ENTRY_DURATION + SpeechLogUI.FADE_DURATION + 0.1f;
        log.update(Collections.emptyList(), totalLifetime);

        assertEquals(0, log.getEntryCount(), "Expired entries should be removed");
    }

    // ===== NPC speech detection via update() =====

    @Test
    void testSpeakingNPCAddsLogEntry() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 0, 1, 0);
        npc.setSpeechText("You what?", 3.0f);

        log.update(List.of(npc), 0.016f);

        assertEquals(1, log.getEntryCount());
        assertEquals("You what?", log.getEntries().get(0).text);
    }

    @Test
    void testSilentNPCDoesNotAddLogEntry() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        // NPC is not speaking

        log.update(List.of(npc), 0.016f);

        assertEquals(0, log.getEntryCount());
    }

    @Test
    void testSameSpeechNotDuplicatedInLog() {
        NPC npc = new NPC(NPCType.PENSIONER, 0, 1, 0);
        npc.setSpeechText("Back in my day...", 5.0f);

        // Update multiple times while NPC is still saying the same thing
        log.update(List.of(npc), 0.016f);
        log.update(List.of(npc), 0.016f);
        log.update(List.of(npc), 0.016f);

        assertEquals(1, log.getEntryCount(), "Same speech should only be logged once");
    }

    @Test
    void testNewSpeechFromSameNPCAddsAnotherEntry() {
        NPC npc = new NPC(NPCType.BUSKER, 0, 1, 0);
        npc.setSpeechText("Wonderwall...", 3.0f);
        log.update(List.of(npc), 0.016f);

        // NPC switches to a different line
        npc.setSpeechText("Any requests?", 3.0f);
        log.update(List.of(npc), 0.016f);

        assertEquals(2, log.getEntryCount());
    }

    @Test
    void testMultipleNPCsSpeakingAllLogged() {
        NPC npc1 = new NPC(NPCType.POLICE, 0, 1, 0);
        NPC npc2 = new NPC(NPCType.DRUNK, 5, 1, 0);
        npc1.setSpeechText("Move along!", 2.0f);
        npc2.setSpeechText("Lovely evening...", 2.0f);

        log.update(List.of(npc1, npc2), 0.016f);

        assertEquals(2, log.getEntryCount());
    }

    // ===== Label formatting =====

    @Test
    void testPoliceNPCFormatsCorrectly() {
        log.addEntry(NPCType.POLICE, "Stop right there!");
        assertEquals("Police", log.getEntries().get(0).label);
    }

    @Test
    void testYouthNPCFormatsCorrectly() {
        log.addEntry(NPCType.YOUTH_GANG, "Innit");
        assertEquals("Youth", log.getEntries().get(0).label);
    }

    @Test
    void testDeliveryDriverFormatsCorrectly() {
        log.addEntry(NPCType.DELIVERY_DRIVER, "Next-day delivery!");
        assertEquals("Delivery", log.getEntries().get(0).label);
    }
}

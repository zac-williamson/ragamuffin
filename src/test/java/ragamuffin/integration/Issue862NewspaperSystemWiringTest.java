package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #862 — Wire NewspaperSystem.update() into the game loop.
 *
 * <p>5 scenarios from SPEC.md:
 * <ol>
 *   <li>NewspaperSystem instantiated and updated in game loop — no exception</li>
 *   <li>Edition published at 18:00 via update()</li>
 *   <li>Filler edition on zero infamy</li>
 *   <li>NPC reacts to high-infamy headline</li>
 *   <li>Heightened Alert triggered on 7+ infamy publication</li>
 * </ol>
 */
class Issue862NewspaperSystemWiringTest {

    private NewspaperSystem newspaperSystem;
    private WantedSystem wantedSystem;
    private NotorietySystem notorietySystem;
    private FenceSystem fenceSystem;
    private RumourNetwork rumourNetwork;
    private FactionSystem factionSystem;
    private StreetEconomySystem streetEconomySystem;
    private CriminalRecord criminalRecord;
    private List<NPC> npcs;
    private NotorietySystem.AchievementCallback noopCallback;

    @BeforeEach
    void setUp() {
        Random rng = new Random(42L);
        newspaperSystem = new NewspaperSystem(rng);
        wantedSystem = new WantedSystem(new Random(42L));
        notorietySystem = new NotorietySystem(new Random(42L));
        fenceSystem = new FenceSystem(new Random(42L));
        rumourNetwork = new RumourNetwork(new Random(42L));
        TurfMap turfMap = new TurfMap();
        factionSystem = new FactionSystem(turfMap, rumourNetwork, new Random(42L));
        streetEconomySystem = new StreetEconomySystem(new Random(42L));
        criminalRecord = new CriminalRecord();
        npcs = new ArrayList<>();
        noopCallback = type -> {};
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: NewspaperSystem instantiated and updated in game loop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Construct a NewspaperSystem. Verify it is non-null.
     * Simulate one PLAYING-state frame via update(). Verify no exception is thrown.
     */
    @Test
    void newspaperSystemInstantiatedAndUpdateIsNoop() {
        assertNotNull(newspaperSystem, "NewspaperSystem must be non-null after construction");

        // Simulate one PLAYING frame at 09:00 on day 1 — well before 18:00, nothing fires
        assertDoesNotThrow(() ->
            newspaperSystem.update(
                0.016f,
                9.0f,
                1,
                notorietySystem,
                wantedSystem,
                rumourNetwork,
                null,
                factionSystem,
                fenceSystem,
                streetEconomySystem,
                criminalRecord,
                npcs,
                noopCallback
            ),
            "update() must not throw on a normal PLAYING-state frame"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Edition published at 18:00
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Record one InfamyEvent (infamyScore=8, actionType="CHASE").
     * Call update(0.1f, 17.85f, 1, ...) — verify no paper published yet.
     * Call update(0.1f, 18.05f, 1, ...) — verify paper is published and headline
     * contains an expected crime-related term.
     */
    @Test
    void editionPublishedAt1800() {
        NewspaperSystem.InfamyEvent chaseEvent = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 8
        );
        newspaperSystem.recordEvent(chaseEvent);

        // Before 18:00 — no edition yet
        newspaperSystem.update(0.1f, 17.85f, 1,
            notorietySystem, wantedSystem, rumourNetwork, null,
            factionSystem, fenceSystem, streetEconomySystem,
            criminalRecord, npcs, noopCallback);

        assertNull(newspaperSystem.getLatestPaper(),
            "No edition should be published before 18:00");

        // At 18:05 — edition fires
        newspaperSystem.update(0.1f, 18.05f, 1,
            notorietySystem, wantedSystem, rumourNetwork, null,
            factionSystem, fenceSystem, streetEconomySystem,
            criminalRecord, npcs, noopCallback);

        assertNotNull(newspaperSystem.getLatestPaper(),
            "An edition should be published at 18:00");

        String headline = newspaperSystem.getLatestPaper().getHeadline();
        assertFalse(headline.equals(NewspaperSystem.PIGEON_FILLER),
            "An 8-infamy event should not produce a filler headline: " + headline);
        // Headline should reference a serious crime
        boolean hasCrimeTerm = headline.contains("WANTED") || headline.contains("FUGITIVE")
            || headline.contains("CHAOS") || headline.contains("CRIMINAL")
            || headline.contains("MASTERMIND") || headline.contains("RAIDER")
            || headline.contains("LOCKDOWN") || headline.contains("CROOK")
            || headline.contains("BRAZEN") || headline.contains("AUDACIOUS")
            || headline.contains("UNTOUCHABLE") || headline.contains("BAFFLED")
            || headline.contains("ESCAPES") || headline.contains("INCIDENT");
        assertTrue(hasCrimeTerm,
            "Headline for 8-infamy chase event should contain a crime-related term: " + headline);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Filler edition on zero infamy
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Construct a NewspaperSystem with no recorded events.
     * Advance time past 18:00 via update(). Verify the headline equals
     * NewspaperSystem.PIGEON_FILLER.
     */
    @Test
    void fillerEditionOnZeroInfamy() {
        // Arm and fire with no events recorded
        newspaperSystem.update(0.1f, 17.85f, 1,
            null, null, null, null, null, null, null, null, npcs, null);
        newspaperSystem.update(0.1f, 18.05f, 1,
            null, null, null, null, null, null, null, null, npcs, null);

        assertNotNull(newspaperSystem.getLatestPaper(),
            "A filler edition should still be published when there are no infamy events");
        assertEquals(NewspaperSystem.PIGEON_FILLER,
            newspaperSystem.getLatestPaper().getHeadline(),
            "No-crime edition must use the PIGEON_FILLER headline");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: NPC reacts to high-infamy headline
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Construct a PUBLIC NPC. Construct a Newspaper with infamyScore=8.
     * Call onNpcReadsNewspaper(npc, paper, null, null). Verify the NPC's speech text
     * contains "Did you see the paper?".
     */
    @Test
    void npcReactsToHighInfamyHeadline() {
        NewspaperSystem.Newspaper paper = new NewspaperSystem.Newspaper(
            "BOROUGH IN CHAOS: CRIMINAL MASTERMIND EVADES FEDS",
            List.of("BRIEF: High Street incident."),
            List.of("FENCE: Items sought."),
            8, 1
        );

        NPC publicNpc = new NPC(NPCType.PUBLIC, 10, 1, 10);
        newspaperSystem.onNpcReadsNewspaper(publicNpc, paper, null, null);

        assertNotNull(publicNpc.getSpeechText(),
            "PUBLIC NPC should have speech text after reading an 8-infamy newspaper");
        assertTrue(publicNpc.getSpeechText().contains("Did you see the paper?"),
            "NPC speech should contain \"Did you see the paper?\": " + publicNpc.getSpeechText());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Heightened Alert triggered on 7+ infamy publication
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Record an InfamyEvent with infamyScore=9. Advance time past 18:00
     * via update() passing wantedSystem. Verify wantedSystem.isHeightenedAlertActive()
     * returns true.
     */
    @Test
    void heightenedAlertTriggeredOnHighInfamyPublication() {
        assertFalse(wantedSystem.isHeightenedAlertActive(),
            "Heightened Alert should not be active before any publication");

        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
            "CHASE", "High Street", null, null, 5, "LEG_IT", null, 9
        );
        newspaperSystem.recordEvent(event);

        // Arm then fire
        newspaperSystem.update(0.1f, 17.85f, 1,
            notorietySystem, wantedSystem, rumourNetwork, null,
            factionSystem, fenceSystem, streetEconomySystem,
            criminalRecord, npcs, noopCallback);
        newspaperSystem.update(0.1f, 18.05f, 1,
            notorietySystem, wantedSystem, rumourNetwork, null,
            factionSystem, fenceSystem, streetEconomySystem,
            criminalRecord, npcs, noopCallback);

        assertTrue(wantedSystem.isHeightenedAlertActive(),
            "Heightened Alert should be active after publishing a 9-infamy edition");
    }
}

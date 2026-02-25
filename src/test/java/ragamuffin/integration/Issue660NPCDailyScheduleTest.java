package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #660 — NPC daily schedules and paths.
 *
 * Verifies that school children follow a realistic daily routine:
 *   08:00–09:00 → GOING_TO_SCHOOL (morning commute)
 *   09:00–15:30 → AT_SCHOOL (school hours)
 *   15:30–17:00 → LEAVING_SCHOOL (end of school day)
 *   other times → WANDERING (evenings/nights: free time)
 */
class Issue660NPCDailyScheduleTest {

    private NPCManager manager;

    @BeforeEach
    void setUp() {
        manager = new NPCManager();
    }

    // -----------------------------------------------------------------------
    // NPCState enum — new states exist
    // -----------------------------------------------------------------------

    @Test
    void goingToSchoolStateExists() {
        // Verify the enum value is present (would fail to compile if missing)
        NPCState state = NPCState.GOING_TO_SCHOOL;
        assertNotNull(state, "GOING_TO_SCHOOL state must exist in NPCState enum");
    }

    @Test
    void atSchoolStateExists() {
        NPCState state = NPCState.AT_SCHOOL;
        assertNotNull(state, "AT_SCHOOL state must exist in NPCState enum");
    }

    @Test
    void leavingSchoolStateExists() {
        NPCState state = NPCState.LEAVING_SCHOOL;
        assertNotNull(state, "LEAVING_SCHOOL state must exist in NPCState enum");
    }

    // -----------------------------------------------------------------------
    // School kid schedule — transitions at correct time bands
    // -----------------------------------------------------------------------

    @Test
    void schoolKidGoesToSchoolDuringMorningCommute() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid, "SCHOOL_KID should spawn successfully");

        // Transition into the morning commute band (08:00–09:00)
        manager.setGameTime(8.0f);

        assertEquals(NPCState.GOING_TO_SCHOOL, kid.getState(),
            "School kid should be GOING_TO_SCHOOL between 08:00 and 09:00");
    }

    @Test
    void schoolKidAtSchoolDuringSchoolHours() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // Transition into school hours (09:00–15:30)
        manager.setGameTime(9.0f);

        assertEquals(NPCState.AT_SCHOOL, kid.getState(),
            "School kid should be AT_SCHOOL between 09:00 and 15:30");
    }

    @Test
    void schoolKidAtSchoolAtMidday() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        manager.setGameTime(12.0f); // Midday

        assertEquals(NPCState.AT_SCHOOL, kid.getState(),
            "School kid should be AT_SCHOOL at midday");
    }

    @Test
    void schoolKidLeavesSchoolAfterThreeThirty() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // Transition into end-of-school band (15:30–17:00)
        manager.setGameTime(15.5f);

        assertEquals(NPCState.LEAVING_SCHOOL, kid.getState(),
            "School kid should be LEAVING_SCHOOL between 15:30 and 17:00");
    }

    @Test
    void schoolKidWandersInEvening() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // First set to a school state
        manager.setGameTime(9.0f); // AT_SCHOOL
        assertEquals(NPCState.AT_SCHOOL, kid.getState());

        // Then transition to evening (after 17:00)
        manager.setGameTime(18.0f);

        assertEquals(NPCState.WANDERING, kid.getState(),
            "School kid should be WANDERING in the evening (after 17:00)");
    }

    @Test
    void schoolKidWandersAtNight() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // Set to school state, then to night
        manager.setGameTime(9.0f); // AT_SCHOOL
        manager.setGameTime(22.0f); // Night

        assertEquals(NPCState.WANDERING, kid.getState(),
            "School kid should be WANDERING at night");
    }

    @Test
    void schoolKidWandersBeforeSchool() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // Early morning before school starts — kid has no schedule yet
        manager.setGameTime(6.0f); // 6 AM, before school

        // Should be in WANDERING (no school band active)
        NPCState state = kid.getState();
        assertTrue(state == NPCState.WANDERING || state == NPCState.IDLE,
            "School kid before school (6 AM) should be WANDERING or IDLE, was: " + state);
    }

    // -----------------------------------------------------------------------
    // Band transitions — no thrashing within same band
    // -----------------------------------------------------------------------

    @Test
    void schoolKidScheduleDoesNotClobberReactionStateWithinSameBand() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // Transition into school band
        manager.setGameTime(9.0f);
        assertEquals(NPCState.AT_SCHOOL, kid.getState());

        // Simulate a reaction state (e.g., being hit)
        kid.setState(NPCState.FLEEING);

        // Repeated calls within the same band must NOT overwrite FLEEING
        manager.setGameTime(10.0f);
        manager.setGameTime(11.5f);
        manager.setGameTime(14.0f);

        assertEquals(NPCState.FLEEING, kid.getState(),
            "Reaction state FLEEING must not be overwritten by same-band setGameTime calls (issue #660)");
    }

    @Test
    void schoolKidReactionStatePreservedAcrossBandTransition() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // Set to a school band, then put into a reaction state
        manager.setGameTime(9.0f);
        kid.setState(NPCState.AGGRESSIVE);

        // Transition to a new band — reaction state must still be preserved
        manager.setGameTime(15.5f); // LEAVING_SCHOOL band
        assertEquals(NPCState.AGGRESSIVE, kid.getState(),
            "Reaction state AGGRESSIVE must be preserved even across school band transitions");
    }

    // -----------------------------------------------------------------------
    // Full daily cycle — end-to-end schedule
    // -----------------------------------------------------------------------

    @Test
    void schoolKidFollowsFullDailySchedule() {
        NPC kid = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        assertNotNull(kid);

        // 1. Morning commute
        manager.setGameTime(8.0f);
        assertEquals(NPCState.GOING_TO_SCHOOL, kid.getState(),
            "08:00 — kid should be GOING_TO_SCHOOL");

        // 2. School hours
        manager.setGameTime(9.0f);
        assertEquals(NPCState.AT_SCHOOL, kid.getState(),
            "09:00 — kid should be AT_SCHOOL");

        // 3. End of school
        manager.setGameTime(15.5f);
        assertEquals(NPCState.LEAVING_SCHOOL, kid.getState(),
            "15:30 — kid should be LEAVING_SCHOOL");

        // 4. Evening free time
        manager.setGameTime(17.0f);
        assertEquals(NPCState.WANDERING, kid.getState(),
            "17:00 — kid should be WANDERING in the evening");
    }

    // -----------------------------------------------------------------------
    // Other NPC types not affected by school schedule
    // -----------------------------------------------------------------------

    @Test
    void publicNPCNotAffectedBySchoolBandChange() {
        NPC pub = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);
        assertNotNull(pub);

        // Set to work hours so PUBLIC is in work state
        manager.setGameTime(8.0f);
        assertEquals(NPCState.GOING_TO_WORK, pub.getState(),
            "PUBLIC NPC should still follow work routine, not school routine");

        // School band transition should not affect PUBLIC
        manager.setGameTime(9.0f);
        assertEquals(NPCState.GOING_TO_WORK, pub.getState(),
            "PUBLIC NPC should remain GOING_TO_WORK — school schedule only applies to SCHOOL_KID");
    }

    @Test
    void multipleSchoolKidsFollowScheduleTogether() {
        NPC kid1 = manager.spawnNPC(NPCType.SCHOOL_KID, 10, 1, 10);
        NPC kid2 = manager.spawnNPC(NPCType.SCHOOL_KID, 15, 1, 15);
        NPC kid3 = manager.spawnNPC(NPCType.SCHOOL_KID, 20, 1, 20);
        assertNotNull(kid1);
        assertNotNull(kid2);
        assertNotNull(kid3);

        manager.setGameTime(9.0f);

        assertEquals(NPCState.AT_SCHOOL, kid1.getState(), "Kid 1 should be AT_SCHOOL");
        assertEquals(NPCState.AT_SCHOOL, kid2.getState(), "Kid 2 should be AT_SCHOOL");
        assertEquals(NPCState.AT_SCHOOL, kid3.getState(), "Kid 3 should be AT_SCHOOL");
    }
}

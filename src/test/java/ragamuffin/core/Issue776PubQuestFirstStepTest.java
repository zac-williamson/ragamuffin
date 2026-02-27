package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Quest.ObjectiveType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #776: Fix quest completion in Ragamuffin Arms — first step
 * unclear or broken.
 *
 * The barman's quest (pub_pint) is an EXPLORE quest targeting the JOB_CENTRE.
 * Previously the only way to progress it was by speaking to an NPC inside the
 * Job Centre — a requirement that wasn't communicated to the player, making the
 * first step feel broken or impossible.
 *
 * The fix (checkPlayerPosition) fires onPlayerEntersLandmark() automatically
 * whenever the player walks into a landmark's bounding box, so the quest
 * progresses naturally the moment the player reaches the Job Centre, with no
 * need to find and speak to a specific NPC.
 */
class Issue776PubQuestFirstStepTest {

    private InteractionSystem interactionSystem;
    private Inventory inventory;
    private World world;

    @BeforeEach
    void setUp() {
        interactionSystem = new InteractionSystem();
        inventory = new Inventory(36);
        world = new World(42L);

        // Register a Job Centre landmark at a known position (10, 0, 10), size 5x3x5
        world.addLandmark(new Landmark(LandmarkType.JOB_CENTRE, 10, 0, 10, 5, 3, 5));
    }

    /**
     * Walking into the Job Centre area (via checkPlayerPosition) must mark the
     * pub quest as visited even if no NPC is spoken to.
     */
    @Test
    void walkingIntoJobCentre_marksBarmanQuestVisited() {
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest, "Barman quest must exist");
        assertEquals(ObjectiveType.EXPLORE, barmanQuest.getType(),
                "Barman quest must be EXPLORE type");
        assertEquals(LandmarkType.JOB_CENTRE, barmanQuest.getTargetLandmark(),
                "Barman quest must target the Job Centre");

        barmanQuest.setActive(true);
        assertFalse(barmanQuest.isLocationVisited(), "Must not be visited yet");

        // Simulate player walking into the Job Centre area (centre of the landmark)
        Vector3 playerPos = new Vector3(12f, 1f, 12f); // inside the 10-14, 0-2, 10-14 box
        interactionSystem.checkPlayerPosition(playerPos, world);

        assertTrue(barmanQuest.isLocationVisited(),
                "Barman quest must be marked visited after player walks into the Job Centre area");
    }

    /**
     * Walking into a different landmark must NOT mark the pub quest as visited.
     */
    @Test
    void walkingIntoWrongLandmark_doesNotMarkBarmanQuestVisited() {
        // Register a Greggs landmark at a different position
        world.addLandmark(new Landmark(LandmarkType.GREGGS, 50, 0, 50, 5, 3, 5));

        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest);
        barmanQuest.setActive(true);

        // Player walks into Greggs, not the Job Centre
        Vector3 playerPos = new Vector3(52f, 1f, 52f); // inside Greggs
        interactionSystem.checkPlayerPosition(playerPos, world);

        assertFalse(barmanQuest.isLocationVisited(),
                "Walking into Greggs must not mark the barman quest (Job Centre) as visited");
    }

    /**
     * checkPlayerPosition with a position outside any landmark must not affect quests.
     */
    @Test
    void walkingOutsideAnyLandmark_doesNotMarkQuestVisited() {
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest);
        barmanQuest.setActive(true);

        // Player is in the middle of the street, not inside any landmark
        Vector3 playerPos = new Vector3(0f, 1f, 0f);
        interactionSystem.checkPlayerPosition(playerPos, world);

        assertFalse(barmanQuest.isLocationVisited(),
                "Being outside any landmark must not mark the quest as visited");
    }

    /**
     * checkPlayerPosition must not mark an inactive quest as visited.
     */
    @Test
    void checkPlayerPosition_doesNotMarkInactiveQuestVisited() {
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest);
        assertFalse(barmanQuest.isActive(), "Quest must not be active yet");

        // Player walks into Job Centre before accepting the quest
        Vector3 playerPos = new Vector3(12f, 1f, 12f);
        interactionSystem.checkPlayerPosition(playerPos, world);

        assertFalse(barmanQuest.isLocationVisited(),
                "Inactive quest must not be marked visited even if player enters the target area");
    }

    /**
     * Full flow: accept pub quest → walk to Job Centre → return to barman → complete.
     * This verifies the full first-step resolution end-to-end.
     */
    @Test
    void fullFlow_pubQuestCompletesAfterWalkingToJobCentre() {
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest);

        // Step 1: accept quest (simulate speaking to barman)
        barmanQuest.setActive(true);
        assertFalse(barmanQuest.checkCompletion(inventory),
                "Quest must not be completable before visiting Job Centre");

        // Step 2: player walks to Job Centre
        Vector3 jobCentrePos = new Vector3(12f, 1f, 12f);
        interactionSystem.checkPlayerPosition(jobCentrePos, world);

        assertTrue(barmanQuest.isLocationVisited(),
                "Quest location must be marked visited after walking to Job Centre");
        assertTrue(barmanQuest.checkCompletion(inventory),
                "Quest must be completable after visiting Job Centre");

        // Step 3: player returns to barman and claims reward
        boolean completed = barmanQuest.complete(inventory);
        assertTrue(completed, "Quest completion must succeed");
        assertTrue(barmanQuest.isCompleted(), "Quest must be marked completed");
        assertFalse(barmanQuest.isActive(), "Quest must no longer be active");
        assertEquals(4, inventory.getItemCount(Material.CRISPS),
                "Player must receive 4 crisps reward");
    }

    /**
     * The quest description must not contain ambiguous wording that leaves the
     * first step unclear.  Specifically it must not say only "go see if he's alright"
     * without giving any hint of what the player needs to do at the Job Centre.
     */
    @Test
    void pubQuestDescription_isNotAmbiguous() {
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest);
        String desc = barmanQuest.getDescription();
        assertNotNull(desc, "Quest description must not be null");
        assertFalse(desc.isBlank(), "Quest description must not be blank");
        // Must reference the job centre in some way
        assertTrue(desc.toLowerCase().contains("job centre") || desc.toLowerCase().contains("jobcentre"),
                "Quest description must mention the job centre so the player knows where to go");
    }
}

package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.core.Quest.ObjectiveType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.world.LandmarkType;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #718: Barman quest doesn't progress when speaking to
 * the Job Centre NPC.
 *
 * The barman quest (pub_pint) is an EXPLORE quest whose target is JOB_CENTRE.
 * When the player speaks to the Job Centre NPC, the quest should be marked
 * as visited so the player can return to the barman to complete it.
 */
class Issue718BarmanQuestJobCentreNPCTest {

    private InteractionSystem interactionSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        interactionSystem = new InteractionSystem();
        inventory = new Inventory(36);
    }

    /**
     * Speaking to the Job Centre NPC should mark the barman's EXPLORE quest
     * (pub_pint) as visited, so the player can complete it by returning to the pub.
     */
    @Test
    void speakingToJobCentreNPC_marksBarmanQuestVisited() {
        // Activate the barman quest (pub_pint: EXPLORE -> JOB_CENTRE)
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest, "Barman quest must exist for PUB landmark");
        assertEquals(ObjectiveType.EXPLORE, barmanQuest.getType(),
                "Barman quest must be an EXPLORE quest");
        assertEquals(LandmarkType.JOB_CENTRE, barmanQuest.getTargetLandmark(),
                "Barman quest must target the JOB_CENTRE");
        barmanQuest.setActive(true);

        assertFalse(barmanQuest.isLocationVisited(),
                "Barman quest location must not be visited yet");

        // Simulate speaking to the Job Centre NPC
        NPC jobCentreNPC = new NPC(NPCType.SHOPKEEPER, 50, 1, 50);
        jobCentreNPC.setBuildingType(LandmarkType.JOB_CENTRE);

        interactionSystem.interactWithNPC(jobCentreNPC, inventory, null, Collections.emptyList());

        assertTrue(barmanQuest.isLocationVisited(),
                "Speaking to the Job Centre NPC must mark the barman quest location as visited");
    }

    /**
     * After visiting the Job Centre (by speaking to its NPC), the barman quest
     * should be completable when the player returns and speaks to the barman.
     */
    @Test
    void afterVisitingJobCentre_barmanQuestIsCompletable() {
        // Activate the barman quest
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest);
        barmanQuest.setActive(true);

        // Player speaks to the Job Centre NPC
        NPC jobCentreNPC = new NPC(NPCType.SHOPKEEPER, 50, 1, 50);
        jobCentreNPC.setBuildingType(LandmarkType.JOB_CENTRE);
        interactionSystem.interactWithNPC(jobCentreNPC, inventory, null, Collections.emptyList());

        // Quest should now be completable
        assertTrue(barmanQuest.checkCompletion(inventory),
                "Barman quest must be completable after visiting Job Centre");

        // Player returns to barman — quest completes and gives reward
        NPC barmanNPC = new NPC(NPCType.SHOPKEEPER, 10, 1, 10);
        barmanNPC.setBuildingType(LandmarkType.PUB);
        String dialogue = interactionSystem.interactWithNPC(barmanNPC, inventory, null, Collections.emptyList());

        assertTrue(barmanQuest.isCompleted(),
                "Barman quest must be completed after returning to the pub");
        assertNotNull(dialogue, "Barman must say something on quest completion");
        // Reward is 4 CRISPS
        assertEquals(4, inventory.getItemCount(ragamuffin.building.Material.CRISPS),
                "Player must receive 4 crisps as the barman quest reward");
    }

    /**
     * Speaking to an NPC at a different building must NOT mark the barman
     * quest as visited.
     */
    @Test
    void speakingToOtherBuildingNPC_doesNotMarkBarmanQuestVisited() {
        Quest barmanQuest = interactionSystem.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(barmanQuest);
        barmanQuest.setActive(true);

        // Player speaks to the Greggs NPC instead
        NPC greggsNPC = new NPC(NPCType.SHOPKEEPER, 30, 1, 30);
        greggsNPC.setBuildingType(LandmarkType.GREGGS);
        interactionSystem.interactWithNPC(greggsNPC, inventory, null, Collections.emptyList());

        assertFalse(barmanQuest.isLocationVisited(),
                "Speaking to the Greggs NPC must not mark the barman quest (JOB_CENTRE) as visited");
    }
}

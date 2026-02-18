package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;

import java.util.List;
import java.util.Random;

/**
 * Phase 11: Handles player interactions including food consumption and NPC dialogue.
 */
public class InteractionSystem {

    private static final float INTERACTION_RANGE = 2.0f;
    private static final Random RANDOM = new Random();

    // NPC dialogue lines
    private static final String[] PUBLIC_DIALOGUE = {
        "Is that... legal?",
        "My council tax pays for this?",
        "I'm calling the council.",
        "Bit rough, innit?",
        "You alright, love?",
        "State of this place.",
        "Have you tried the JobCentre?",
        "Mind yourself, yeah?",
        "This used to be a nice area.",
        "You look like you need a Greggs.",
        "My nan wouldn't stand for this.",
        "Shocking, absolutely shocking.",
        "Are you sleeping rough?",
        "Can't get a GP appointment for weeks.",
        "The bins haven't been collected again."
    };

    private static final String[] POLICE_DIALOGUE_OPTIONS = {
        "Move along.",
        "Evening. What are you up to then?",
        "This is a dispersal zone.",
        "Have you got ID?",
        "Don't make me radio for backup.",
        "Right, what's going on here?"
    };

    private static final String[] COUNCIL_BUILDER_DIALOGUE_OPTIONS = {
        "Planning permission denied.",
        "This is an unauthorised structure.",
        "Health and safety, mate.",
        "Got a notice of demolition here.",
        "We'll have this down by Tuesday."
    };

    private static final String[] YOUTH_GANG_DIALOGUE = {
        "Got any change, bruv?",
        "Nice phone, that.",
        "What you looking at?",
        "This is our patch.",
        "Run your pockets.",
        "You want some, yeah?"
    };

    /**
     * Handle food consumption (right-click with food in hotbar).
     * @return true if food was consumed
     */
    public boolean consumeFood(Material food, Player player, Inventory inventory) {
        if (food == null) {
            return false;
        }

        float hungerRestored = 0;

        if (food == Material.SAUSAGE_ROLL) {
            hungerRestored = 30;
        } else if (food == Material.STEAK_BAKE) {
            hungerRestored = 30;
        } else {
            // Not a food item
            return false;
        }

        // Eat the food
        player.eat(hungerRestored);
        inventory.removeItem(food, 1);
        return true;
    }

    /**
     * Check if a material is food.
     */
    public boolean isFood(Material material) {
        return material == Material.SAUSAGE_ROLL || material == Material.STEAK_BAKE;
    }

    /**
     * Interact with an NPC (E key).
     * @param npc The NPC to interact with
     * @return The dialogue text, or null if no interaction
     */
    public String interactWithNPC(NPC npc) {
        if (npc == null) {
            return null;
        }

        String dialogue = null;

        switch (npc.getType()) {
            case PUBLIC:
                dialogue = PUBLIC_DIALOGUE[RANDOM.nextInt(PUBLIC_DIALOGUE.length)];
                break;
            case POLICE:
                dialogue = POLICE_DIALOGUE_OPTIONS[RANDOM.nextInt(POLICE_DIALOGUE_OPTIONS.length)];
                break;
            case COUNCIL_BUILDER:
                dialogue = COUNCIL_BUILDER_DIALOGUE_OPTIONS[RANDOM.nextInt(COUNCIL_BUILDER_DIALOGUE_OPTIONS.length)];
                break;
            case YOUTH_GANG:
                dialogue = YOUTH_GANG_DIALOGUE[RANDOM.nextInt(YOUTH_GANG_DIALOGUE.length)];
                break;
            case DOG:
                dialogue = "*bark*";
                break;
            default:
                break;
        }

        if (dialogue != null) {
            npc.setSpeechText(dialogue, 3.0f);
        }

        return dialogue;
    }

    /**
     * Find an NPC in interaction range.
     */
    public NPC findNPCInRange(Vector3 playerPos, Vector3 lookDirection, List<NPC> npcs) {
        NPC closest = null;
        float closestDistance = INTERACTION_RANGE;

        for (NPC npc : npcs) {
            // Check if NPC is within interaction range
            float distance = playerPos.dst(npc.getPosition());
            if (distance <= INTERACTION_RANGE) {
                // Check if player is roughly facing the NPC
                Vector3 toNPC = new Vector3(npc.getPosition()).sub(playerPos).nor();
                float dot = lookDirection.dot(toNPC);

                // Player must be facing the NPC (dot > 0.5 means within ~60 degrees)
                if (dot > 0.5f && distance < closestDistance) {
                    closest = npc;
                    closestDistance = distance;
                }
            }
        }

        return closest;
    }

    /**
     * Get random speech for PUBLIC NPCs near player.
     */
    public String getRandomPublicSpeech() {
        return PUBLIC_DIALOGUE[RANDOM.nextInt(PUBLIC_DIALOGUE.length)];
    }
}

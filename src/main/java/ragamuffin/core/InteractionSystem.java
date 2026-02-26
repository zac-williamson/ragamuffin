package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Phase 11: Handles player interactions including food consumption and NPC dialogue.
 * Issue #440: Extended to support quest NPCs in labelled buildings.
 */
public class InteractionSystem {

    public static final float INTERACTION_RANGE = 2.0f;
    private static final Random RANDOM = new Random();

    private final BuildingQuestRegistry questRegistry = new BuildingQuestRegistry();

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

    private static final String[] SHOPKEEPER_DIALOGUE = {
        "Browse all you like, love.",
        "We're closing in ten minutes.",
        "Two for one on crisps.",
        "Got your Clubcard?",
        "Self-service is broken again.",
        "No, we don't do cashback."
    };

    private static final String[] POSTMAN_DIALOGUE = {
        "Another parcel for number 12.",
        "Dog nearly had me leg.",
        "This rain'll ruin these letters.",
        "I'm on my third round.",
        "Sign here, please."
    };

    private static final String[] JOGGER_DIALOGUE = {
        "On your left!",
        "Morning!",
        "Just five more k...",
        "Can't stop, Strava's running!",
        "*panting intensifies*"
    };

    private static final String[] DRUNK_DIALOGUE = {
        "You're my best mate, you are.",
        "I love you, man.",
        "Who moved the pavement?",
        "I'm not drunk, you're drunk.",
        "This town's gone to the dogs.",
        "Buy us a pint?"
    };

    private static final String[] BUSKER_DIALOGUE = {
        "Any requests? Quid each.",
        "I was on X Factor, you know.",
        "Any spare change?",
        "This is me best Oasis.",
        "I take contactless, actually."
    };

    private static final String[] DELIVERY_DRIVER_DIALOGUE = {
        "Where's number 42?",
        "Parcel for... can't read this.",
        "Left it behind the bin.",
        "That's my third missed delivery.",
        "Just leave it with a neighbour."
    };

    private static final String[] PENSIONER_DIALOGUE = {
        "In my day, this was all fields.",
        "These prices are criminal.",
        "Nobody says hello anymore.",
        "Young people today, honestly.",
        "Is this the queue?",
        "I remember proper winters."
    };

    private static final String[] SCHOOL_KID_DIALOGUE = {
        "Have you got games on your phone?",
        "That's well peak!",
        "Can I have a quid?",
        "Bruv, look at the state of him!",
        "Safe, yeah?",
        "Are you someone's dad?"
    };

    private static final String[] COUNCIL_MEMBER_DIALOGUE = {
        "Have you filled in form 27B?",
        "This area is under review.",
        "Budget cuts, I'm afraid.",
        "We'll look into it. Eventually.",
        "Not my department."
    };

    /**
     * Handle food/consumable use (right-click with item in hotbar).
     * @return true if item was consumed
     */
    public boolean consumeFood(Material food, Player player, Inventory inventory) {
        if (food == null) {
            return false;
        }

        lastConsumeMessage = null;
        float hungerRestored = 0;

        if (food == Material.SAUSAGE_ROLL) {
            hungerRestored = 30;
            lastConsumeMessage = "You eat the sausage roll. Warm, flaky, perfect.";
        } else if (food == Material.STEAK_BAKE) {
            hungerRestored = 35;
            lastConsumeMessage = "You wolf down the steak bake. Greggs delivers again.";
        } else if (food == Material.CHIPS) {
            hungerRestored = 25;
            lastConsumeMessage = "You eat the chips. Slightly soggy, but beggars can't be choosers.";
        } else if (food == Material.KEBAB) {
            hungerRestored = 40;
            lastConsumeMessage = "You demolish the kebab. Mysterious meat, no questions asked.";
        } else if (food == Material.CRISPS) {
            hungerRestored = 10;
            lastConsumeMessage = "You crunch through the crisps. Ready salted. Classic.";
        } else if (food == Material.TIN_OF_BEANS) {
            hungerRestored = 35;
            lastConsumeMessage = "You eat the beans cold from the tin. Desperate times.";
        } else if (food == Material.ENERGY_DRINK) {
            hungerRestored = 5;
            player.restoreEnergy(30); // Energy drink restores energy too
            lastConsumeMessage = "You neck the energy drink. Your heart is now vibrating.";
        } else if (food == Material.PINT) {
            hungerRestored = 15;
            player.restoreEnergy(20); // Liquid courage
            lastConsumeMessage = "You down the pint. Dutch courage, British problems.";
        } else if (food == Material.PERI_PERI_CHICKEN) {
            hungerRestored = 45; // Cheeky Nandos is top-tier sustenance
            lastConsumeMessage = "Cheeky Nandos. Even out here, it hits different.";
        } else if (food == Material.PARACETAMOL) {
            hungerRestored = 0;
            player.heal(25); // Direct health restoration
            lastConsumeMessage = "You take two paracetamol. That should help.";
        } else if (food == Material.FIRE_EXTINGUISHER) {
            hungerRestored = 0;
            player.heal(20); // Blast of cold foam soothes the wounds
            lastConsumeMessage = "You blast yourself with foam. Oddly soothing.";
        } else if (food == Material.WASHING_POWDER) {
            hungerRestored = 0;
            player.restoreEnergy(15); // Clean laundry scent is invigorating
            lastConsumeMessage = "You sniff the washing powder. Invigorating.";
        } else if (food == Material.SCRATCH_CARD) {
            hungerRestored = 0;
            inventory.removeItem(food, 1);
            // 1 in 5 chance of winning a diamond
            if (RANDOM.nextInt(5) == 0) {
                inventory.addItem(Material.DIAMOND, 1);
                lastScratchCardWon = true;
                lastConsumeMessage = "You scratch the card. YOU HAVE WON! A diamond falls out.";
            } else {
                lastScratchCardWon = false;
                lastConsumeMessage = "You scratch the card. Not a winner. Gutted.";
            }
            return true; // Already removed, skip the removal below
        } else if (food == Material.ANTIDEPRESSANTS) {
            hungerRestored = 0;
            player.restoreEnergy(20); // Steadies the nerves
            lastConsumeMessage = "You take one. Does anything feel different? Hard to say.";
        } else {
            // Not a consumable item
            return false;
        }

        // Consume the item
        player.eat(hungerRestored);
        inventory.removeItem(food, 1);
        return true;
    }

    /**
     * Whether the last scratch card scratched was a winner.
     * Reset after each scratch card use.
     */
    private boolean lastScratchCardWon = false;

    /**
     * Optional tooltip message set by the last consumeFood() call.
     * May be null if no message was generated.
     */
    private String lastConsumeMessage = null;

    /**
     * Check if the last scratch card was a winner.
     */
    public boolean didLastScratchCardWin() {
        return lastScratchCardWon;
    }

    /**
     * Get the tooltip message set by the last consumeFood() call, if any.
     * Returns null if no message was generated.
     */
    public String getLastConsumeMessage() {
        return lastConsumeMessage;
    }

    /**
     * Check if a material is food or consumable.
     */
    public boolean isFood(Material material) {
        return material == Material.SAUSAGE_ROLL || material == Material.STEAK_BAKE
            || material == Material.CHIPS || material == Material.KEBAB
            || material == Material.CRISPS || material == Material.TIN_OF_BEANS
            || material == Material.ENERGY_DRINK || material == Material.PINT
            || material == Material.PERI_PERI_CHICKEN || material == Material.PARACETAMOL
            || material == Material.FIRE_EXTINGUISHER || material == Material.WASHING_POWDER
            || material == Material.SCRATCH_CARD || material == Material.ANTIDEPRESSANTS;
    }

    /**
     * Interact with an NPC (E key).
     * @param npc The NPC to interact with
     * @return The dialogue text, or null if no interaction
     */
    public String interactWithNPC(NPC npc) {
        return interactWithNPC(npc, null);
    }

    /**
     * Interact with an NPC (E key), with optional inventory for quest completion.
     * If the NPC is a building quest NPC, handles quest offer/progress/completion.
     * If the NPC is a SHOPKEEPER and the player has shillings, offers to sell food.
     * @param npc The NPC to interact with
     * @param inventory Player inventory (may be null for non-quest interactions)
     * @return The dialogue text, or null if no interaction
     */
    public String interactWithNPC(NPC npc, Inventory inventory) {
        if (npc == null) {
            return null;
        }

        // Building quest NPC — check if this NPC has a building association with a quest
        LandmarkType buildingType = npc.getBuildingType();
        if (buildingType != null && questRegistry.hasQuest(buildingType)) {
            String questDialogue = handleQuestInteraction(buildingType, inventory);
            if (questDialogue != null) {
                npc.setSpeechText(questDialogue, 5.0f);
                return questDialogue;
            }
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
            case SHOPKEEPER:
                // Two-step purchase flow (Fix #535, extended in Fix #539):
                // First E-press: open the shop menu listing available items (only if player has currency).
                //   Player can press 1/2/3 to highlight an item; selection defaults to 1.
                // Second E-press (while menu is open): execute the purchase for the selected item.
                if (inventory != null) {
                    int shillingsHeld = inventory.getItemCount(Material.SHILLING);
                    int penniesHeld = inventory.getItemCount(Material.PENNY);
                    if (npc.isShopMenuOpen()) {
                        // Second press — complete the purchase for the selected item
                        npc.setShopMenuOpen(false);
                        dialogue = handleShopkeeperPurchase(npc, inventory);
                        if (dialogue == null) {
                            dialogue = "We don't seem to have what you need right now.";
                        }
                    } else if (shillingsHeld > 0 || penniesHeld > 0) {
                        // First press and player has currency — open the shop menu.
                        // Use a longer duration (10 s) so the player has time to read,
                        // select an item with 1/2/3, and confirm with E. (Fix #551)
                        npc.setShopMenuOpen(true); // also resets selection to 1
                        dialogue = buildShopMenuDialogue(npc, inventory);
                        npc.setSpeechText(dialogue, 10.0f); // set directly so the generic fallback below is skipped
                        return dialogue; // return early to skip the generic 3-second setSpeechText call
                    } else {
                        // No currency — fall through to generic dialogue
                        dialogue = SHOPKEEPER_DIALOGUE[RANDOM.nextInt(SHOPKEEPER_DIALOGUE.length)];
                    }
                } else {
                    dialogue = SHOPKEEPER_DIALOGUE[RANDOM.nextInt(SHOPKEEPER_DIALOGUE.length)];
                }
                break;
            case POSTMAN:
                dialogue = POSTMAN_DIALOGUE[RANDOM.nextInt(POSTMAN_DIALOGUE.length)];
                break;
            case JOGGER:
                dialogue = JOGGER_DIALOGUE[RANDOM.nextInt(JOGGER_DIALOGUE.length)];
                break;
            case DRUNK:
                dialogue = DRUNK_DIALOGUE[RANDOM.nextInt(DRUNK_DIALOGUE.length)];
                break;
            case BUSKER:
                dialogue = BUSKER_DIALOGUE[RANDOM.nextInt(BUSKER_DIALOGUE.length)];
                break;
            case DELIVERY_DRIVER:
                dialogue = DELIVERY_DRIVER_DIALOGUE[RANDOM.nextInt(DELIVERY_DRIVER_DIALOGUE.length)];
                break;
            case PENSIONER:
                dialogue = PENSIONER_DIALOGUE[RANDOM.nextInt(PENSIONER_DIALOGUE.length)];
                break;
            case SCHOOL_KID:
                dialogue = SCHOOL_KID_DIALOGUE[RANDOM.nextInt(SCHOOL_KID_DIALOGUE.length)];
                break;
            case COUNCIL_MEMBER:
                dialogue = COUNCIL_MEMBER_DIALOGUE[RANDOM.nextInt(COUNCIL_MEMBER_DIALOGUE.length)];
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
     * Handle a quest interaction with a building NPC.
     * Returns the dialogue line to show, or null if no special quest handling.
     */
    private String handleQuestInteraction(LandmarkType buildingType, Inventory inventory) {
        Quest quest = questRegistry.getQuest(buildingType);
        if (quest == null) return null;

        if (quest.isCompleted()) {
            return "Thanks again. You did right by me.";
        }

        if (!quest.isActive()) {
            // Offer the quest
            quest.setActive(true);
            return BuildingQuestRegistry.getQuestOfferLine(quest);
        }

        // Quest is active — check if player can complete it.
        // EXPLORE quests don't need an inventory; COLLECT/DELIVER quests do.
        if (quest.checkCompletion(inventory)) {
            boolean success = quest.complete(inventory);
            if (success) {
                lastQuestCompleted = quest;
                return BuildingQuestRegistry.getQuestCompletionLine(quest);
            }
        }

        // Remind the player of the objective
        return BuildingQuestRegistry.getQuestReminderLine(quest, inventory);
    }

    /**
     * Handle a shopkeeper merchant purchase using the player's currency and the
     * item they have selected in the shop menu (Fix #539).
     * Attempts to buy the item at {@code npc.getSelectedShopItem()} (1=sausage roll,
     * 2=energy drink, 3=crisps).  If the player cannot afford the selected item the
     * shopkeeper says so rather than silently switching to a cheaper item.
     * Returns the purchase dialogue, or null if the player has no currency at all.
     */
    private String handleShopkeeperPurchase(NPC npc, Inventory inventory) {
        int shillings = inventory.getItemCount(Material.SHILLING);
        int pennies = inventory.getItemCount(Material.PENNY);

        if (shillings == 0 && pennies == 0) {
            return null;
        }

        int selection = npc.getSelectedShopItem();

        switch (selection) {
            case 1: // Sausage roll — 2 shillings
                buySausageRoll(inventory);
                return lastPurchaseMessage;
            case 2: // Energy drink — 1 shilling
                buyEnergyDrink(inventory);
                return lastPurchaseMessage;
            case 3: // Crisps — 6 pennies
                buyCrisps(inventory);
                return lastPurchaseMessage;
            default:
                return "Got coins, have you? Sausage roll's 2 shillings, energy drink's 1 shilling, crisps are 6 pennies.";
        }
    }

    /**
     * Build the shop menu dialogue string, marking the currently selected item.
     * Called on the first E-press and whenever the player changes selection (Fix #539).
     */
    public String buildShopMenuDialogue(NPC npc, Inventory inventory) {
        int sel = npc.getSelectedShopItem();
        int shillings = inventory.getItemCount(Material.SHILLING);
        int pennies = inventory.getItemCount(Material.PENNY);

        String sausageRoll  = (sel == 1 ? "> " : "  ") + "1. sausage roll (2s)"
            + (shillings >= SAUSAGE_ROLL_COST_SHILLINGS ? "" : " [can't afford]");
        String energyDrink  = (sel == 2 ? "> " : "  ") + "2. energy drink (1s)"
            + (shillings >= ENERGY_DRINK_COST_SHILLINGS ? "" : " [can't afford]");
        String crisps       = (sel == 3 ? "> " : "  ") + "3. crisps (6p)"
            + (pennies >= CRISPS_COST_PENNIES ? "" : " [can't afford]");

        return "What'll it be?\n" + sausageRoll + "\n" + energyDrink + "\n" + crisps
            + "\nPress 1/2/3 to select, E to buy.";
    }

    /**
     * Select a shop item for an open shop menu (Fix #539).
     * Called when the player presses 1, 2, or 3 while the shop menu is open.
     * Updates the NPC's selected item and refreshes the speech bubble to reflect
     * the new selection.
     *
     * @param npc   the shopkeeper NPC whose menu is open
     * @param index item index 1-3 (1=sausage roll, 2=energy drink, 3=crisps)
     * @param inventory player inventory (used to show affordability)
     * @return the updated shop menu dialogue, or null if the menu is not open
     */
    public String selectShopItem(NPC npc, int index, Inventory inventory) {
        if (npc == null || !npc.isShopMenuOpen()) {
            return null;
        }
        npc.setSelectedShopItem(index);
        String dialogue = buildShopMenuDialogue(npc, inventory);
        npc.setSpeechText(dialogue, 10.0f); // Fix #551: give player time to confirm with E
        return dialogue;
    }

    /** The last quest that was completed (for UI feedback). */
    private Quest lastQuestCompleted = null;

    /**
     * Get the last quest completed and clear it.
     */
    public Quest pollLastQuestCompleted() {
        Quest q = lastQuestCompleted;
        lastQuestCompleted = null;
        return q;
    }

    /**
     * Get the quest registry (for tests and UI).
     */
    public BuildingQuestRegistry getQuestRegistry() {
        return questRegistry;
    }

    /**
     * Notify the quest system that the player has entered a landmark.
     * Any active EXPLORE quests targeting this landmark will be marked as visited,
     * making them completable when the player next speaks to the quest giver.
     *
     * @param landmarkType the landmark the player has just entered or reached
     */
    public void onPlayerEntersLandmark(ragamuffin.world.LandmarkType landmarkType) {
        if (landmarkType == null) return;
        for (ragamuffin.world.LandmarkType type : ragamuffin.world.LandmarkType.values()) {
            Quest quest = questRegistry.getQuest(type);
            if (quest == null) continue;
            if (!quest.isActive() || quest.isCompleted()) continue;
            if (quest.getType() == Quest.ObjectiveType.EXPLORE
                    && landmarkType == quest.getTargetLandmark()) {
                quest.markLocationVisited();
            }
        }
    }

    /**
     * Find an NPC in interaction range.
     */
    public NPC findNPCInRange(Vector3 playerPos, Vector3 lookDirection, List<NPC> npcs) {
        NPC closest = null;
        float closestDistance = INTERACTION_RANGE;

        // Project look direction onto horizontal plane so vertical aim does not affect facing check
        Vector3 horizLook = new Vector3(lookDirection.x, 0, lookDirection.z);
        if (horizLook.len2() < 0.0001f) {
            // Player is looking straight up or down — use full direction as fallback
            horizLook.set(lookDirection);
        }
        horizLook.nor();

        for (NPC npc : npcs) {
            // Dead NPCs cannot be interacted with
            if (!npc.isAlive()) continue;

            // Check if NPC is within interaction range
            float distance = playerPos.dst(npc.getPosition());
            if (distance <= INTERACTION_RANGE) {
                // Check if player is roughly facing the NPC (horizontal plane only)
                Vector3 toNPC = new Vector3(npc.getPosition().x - playerPos.x, 0,
                        npc.getPosition().z - playerPos.z);
                if (toNPC.len2() < 0.0001f) {
                    // NPC is directly above/below — treat as facing
                    toNPC.set(horizLook);
                }
                toNPC.nor();
                float dot = horizLook.dot(toNPC);

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

    /**
     * Check if a material has a non-food, non-placeable use action (right-click).
     * These items are not consumed but may restore stats or show a tooltip message.
     */
    public boolean canUseItem(Material material) {
        switch (material) {
            case NEWSPAPER:
            case TEXTBOOK:
            case HYMN_BOOK:
            case HAIR_CLIPPERS:
            case NAIL_POLISH:
            case BROKEN_PHONE:
            case DODGY_DVD:
            case PLYWOOD:
            case PIPE:
            case COMPUTER:
            case OFFICE_CHAIR:
            case STAPLER:
            case PETROL_CAN:
            case DIAMOND:
            case SHILLING:
            case PENNY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Use a non-food, non-placeable item.
     * Reading material (NEWSPAPER, TEXTBOOK, HYMN_BOOK) restores a small amount of energy.
     * Other items show a contextual tooltip message.
     *
     * @return a tooltip message to display, or null if nothing to show
     */
    public String useItem(Material material, Player player, Inventory inventory) {
        if (material == null) {
            return null;
        }

        switch (material) {
            case NEWSPAPER:
                player.restoreEnergy(5);
                inventory.removeItem(material, 1);
                return "You skim the headlines. Grim reading, but oddly soothing.";
            case TEXTBOOK:
                player.restoreEnergy(8);
                inventory.removeItem(material, 1);
                return "Dense prose, no pictures. Your brain hurts slightly less than before.";
            case HYMN_BOOK:
                player.restoreEnergy(5);
                inventory.removeItem(material, 1);
                return "You hum a verse. It doesn't help much, but it's something.";
            case PETROL_CAN:
                return "Smells like trouble. Useful as a crafting ingredient.";
            case DIAMOND:
                return "Jewellers can be a good source of diamond. This one's worth a fortune.";
            case HAIR_CLIPPERS:
                return "A quick trim? Not the time or place.";
            case NAIL_POLISH:
                return "Not really the moment for a manicure.";
            case BROKEN_PHONE:
                return "The screen's cracked. Still, someone might want the parts.";
            case DODGY_DVD:
                return "You don't want to know what's on this. Could be worth something to the right person.";
            case PLYWOOD:
                return "Flat and sturdy. Craft it into planks or use it to build a shelter.";
            case PIPE:
                return "A solid length of pipe. Useful for crafting an improvised tool.";
            case COMPUTER:
                return "A bulky desktop. Someone might pay good money for the components.";
            case OFFICE_CHAIR:
                return "On wheels, but not exactly portable. Leave it.";
            case STAPLER:
                return "Satisfying click. Utterly useless out here.";
            case SHILLING:
                return "Shillings are currency — trade them with shopkeepers for food and goods.";
            case PENNY:
                return "A penny. Twelve pennies make a shilling. Trade them with shopkeepers.";
            default:
                return null;
        }
    }

    // --- Merchant purchase constants ---

    /** Cost of a sausage roll from a shopkeeper (in shillings). */
    public static final int SAUSAGE_ROLL_COST_SHILLINGS = 2;

    /** Cost of an energy drink from a shopkeeper (in shillings). */
    public static final int ENERGY_DRINK_COST_SHILLINGS = 1;

    /** Cost of crisps from a shopkeeper (in pennies). */
    public static final int CRISPS_COST_PENNIES = 6;

    /**
     * Result of a merchant purchase attempt.
     */
    public enum PurchaseResult {
        /** Purchase succeeded — item added to inventory, currency deducted. */
        SUCCESS,
        /** Player has insufficient currency. */
        INSUFFICIENT_FUNDS,
        /** The NPC is not a shopkeeper or does not sell items. */
        NOT_A_MERCHANT
    }

    /** The last purchase result message, set by attemptPurchase(). */
    private String lastPurchaseMessage = null;

    /** Get and clear the last purchase message. */
    public String pollLastPurchaseMessage() {
        String msg = lastPurchaseMessage;
        lastPurchaseMessage = null;
        return msg;
    }

    /**
     * Attempt to buy a sausage roll from a SHOPKEEPER NPC for 2 shillings.
     * On success deducts 2 shillings and adds 1 sausage roll to inventory.
     *
     * @return PurchaseResult indicating outcome
     */
    public PurchaseResult buySausageRoll(Inventory inventory) {
        return purchaseItem(inventory, Material.SHILLING, SAUSAGE_ROLL_COST_SHILLINGS,
            Material.SAUSAGE_ROLL, 1,
            "You hand over 2 shillings. The shopkeeper passes you a sausage roll. Cheers.",
            "You need 2 shillings for a sausage roll. Keep looking.");
    }

    /**
     * Attempt to buy an energy drink from a SHOPKEEPER NPC for 1 shilling.
     * On success deducts 1 shilling and adds 1 energy drink to inventory.
     *
     * @return PurchaseResult indicating outcome
     */
    public PurchaseResult buyEnergyDrink(Inventory inventory) {
        return purchaseItem(inventory, Material.SHILLING, ENERGY_DRINK_COST_SHILLINGS,
            Material.ENERGY_DRINK, 1,
            "You hand over a shilling. The shopkeeper slides you an energy drink.",
            "You need 1 shilling for an energy drink. Find some coins first.");
    }

    /**
     * Attempt to buy crisps from a SHOPKEEPER NPC for 6 pennies.
     * On success deducts 6 pennies and adds 1 crisps to inventory.
     *
     * @return PurchaseResult indicating outcome
     */
    public PurchaseResult buyCrisps(Inventory inventory) {
        return purchaseItem(inventory, Material.PENNY, CRISPS_COST_PENNIES,
            Material.CRISPS, 1,
            "Six pennies for crisps. Not bad. Ready salted, obviously.",
            "You need 6 pennies for a bag of crisps.");
    }

    private PurchaseResult purchaseItem(Inventory inventory,
                                        Material currency, int currencyCost,
                                        Material item, int itemCount,
                                        String successMsg, String failMsg) {
        if (inventory.getItemCount(currency) >= currencyCost) {
            inventory.removeItem(currency, currencyCost);
            inventory.addItem(item, itemCount);
            lastPurchaseMessage = successMsg;
            return PurchaseResult.SUCCESS;
        } else {
            lastPurchaseMessage = failMsg;
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }
    }
}

package ragamuffin.ui;

/**
 * Enumeration of all tooltip triggers in the game.
 * Each trigger is shown only once (first-time only).
 */
public enum TooltipTrigger {
    FIRST_TREE_PUNCH("Punch a tree to get wood"),
    JEWELLER_DIAMOND("Jewellers can be a good source of diamond"),
    YOUTH_THEFT("Oi! That's mine!"),
    FIRST_POLICE_ENCOUNTER("Evening officer. Lovely night for it."),
    FIRST_COUNCIL_ENCOUNTER("Dodge to avoid the attacks of stronger enemies"),
    FIRST_BLOCK_PLACE("That's... structurally ambitious."),
    FIRST_DEATH("Council tax doesn't pay itself. Get up."),
    FIRST_GREGGS("Ah, Greggs. The backbone of British cuisine."),
    HUNGER_LOW("Your stomach growls. Even the pigeons look appetising."),
    FIRST_CRAFT("Crafting with materials of questionable provenance."),
    TOOL_BROKEN("Your tool falls apart. Typical."),
    FIRST_NPC_LOOT("Spoils of war. Or mugging. Same thing round here."),
    CARDBOARD_BOX_SHELTER("Home sweet home. Well, cardboard home. Home-adjacent."),
    GREGGS_RAID_ALERT("Raiding Greggs? Bold. The police take a dim view of sausage roll crime."),
    GREGGS_RAID_ESCALATION("Someone's called it in. Police are on their way to avenge the pastries."),
    GANG_TERRITORY_ENTERED("You've wandered into their patch. They've noticed."),
    GANG_TERRITORY_HOSTILE("Wrong postcode, mate. They're not happy about that."),
    WARMTH_GETTING_COLD("Getting cold. Find shelter or stand near a campfire to warm up."),
    WARMTH_DANGER("Hypothermia setting in. Get indoors, find a campfire, or drink a flask of tea. A coat or woolly hat will slow the drain.");

    private final String message;

    TooltipTrigger(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

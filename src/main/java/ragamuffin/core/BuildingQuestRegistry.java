package ragamuffin.core;

import ragamuffin.building.Material;
import ragamuffin.core.Quest.ObjectiveType;
import ragamuffin.world.LandmarkType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry of quests offered by building NPCs.
 * Each labelled building has a thematically relevant quest.
 * Quests are designed to elevate gameplay and enable emergent storytelling.
 */
public class BuildingQuestRegistry {

    // Maps LandmarkType to a quest template (reset/copied per playthrough).
    private final Map<LandmarkType, Quest> quests = new EnumMap<>(LandmarkType.class);

    public BuildingQuestRegistry() {
        register(LandmarkType.TESCO_EXPRESS,
            new Quest("tesco_stock", "Tesco Express Manager",
                "Our shelves are bare. Bring me 3 tins of beans and I'll make it worth your while.",
                ObjectiveType.COLLECT, Material.TIN_OF_BEANS, 3,
                Material.CRISPS, 5));

        register(LandmarkType.GREGGS,
            new Quest("greggs_flour", "Greggs Baker",
                "We're desperate — pop to Tesco and grab me 2 tins of beans. Baking emergency.",
                ObjectiveType.COLLECT, Material.TIN_OF_BEANS, 2,
                Material.STEAK_BAKE, 3));

        register(LandmarkType.OFF_LICENCE,
            new Quest("offlicence_delivery", "Khan's Off-Licence",
                "My delivery driver's done a runner. Bring me an energy drink from the high street, will you?",
                ObjectiveType.COLLECT, Material.ENERGY_DRINK, 1,
                Material.PINT, 2));

        register(LandmarkType.CHARITY_SHOP,
            new Quest("charity_clothes", "Charity Shop Volunteer",
                "We're desperately short of donations. Bring me some newspaper — anything helps.",
                ObjectiveType.COLLECT, Material.NEWSPAPER, 2,
                Material.TEXTBOOK, 1));

        register(LandmarkType.JEWELLER,
            new Quest("jeweller_diamond", "Andre",
                "Someone nicked a diamond from my display. Bring it back and I'll reward you handsomely.",
                ObjectiveType.COLLECT, Material.DIAMOND, 1,
                Material.SHILLING, 5));

        register(LandmarkType.JOB_CENTRE,
            new Quest("jobcentre_form", "Jobcentre Advisor",
                "I need a textbook to update our training materials. Could you find one?",
                ObjectiveType.COLLECT, Material.TEXTBOOK, 1,
                Material.ANTIDEPRESSANTS, 2));

        register(LandmarkType.BOOKIES,
            new Quest("bookies_scratchcard", "Coral Manager",
                "Punters keep asking for energy drinks. Grab me 3 from the off-licence and I'll sort you.",
                ObjectiveType.COLLECT, Material.ENERGY_DRINK, 3,
                Material.ENERGY_DRINK, 2));

        register(LandmarkType.KEBAB_SHOP,
            new Quest("sultan_kebab", "Sultan",
                "We're out of supplies. Grab me 2 tins of beans from the corner shop — improvising tonight.",
                ObjectiveType.COLLECT, Material.TIN_OF_BEANS, 2,
                Material.KEBAB, 2));

        register(LandmarkType.LAUNDERETTE,
            new Quest("launderette_powder", "Spotless Owner",
                "Pipes are playing up. Pop to the fire station and make sure they know — I need a plumber.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.FIRE_STATION, Material.ANTIDEPRESSANTS, 1));

        register(LandmarkType.PUB,
            new Quest("pub_pint", "Barman",
                "Need someone to check on my mate at the job centre. Just head over there and I'll know you've been.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.JOB_CENTRE, Material.CRISPS, 4));

        register(LandmarkType.PAWN_SHOP,
            new Quest("pawnshop_electronics", "Cash4Gold Manager",
                "Got a punter after a computer. Find me one and I'll cut you in.",
                ObjectiveType.COLLECT, Material.COMPUTER, 1,
                Material.SHILLING, 3));

        register(LandmarkType.CHIPPY,
            new Quest("chippy_chips", "Tony",
                "Oil delivery never showed. Pop to the builders merchant and see if they've got anything.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.BUILDERS_MERCHANT, Material.KEBAB, 1));

        register(LandmarkType.NEWSAGENT,
            new Quest("newsagent_paper", "Patel",
                "My newspaper delivery never showed. Go check the library — might have some there.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.LIBRARY, Material.PENNY, 6));

        register(LandmarkType.GP_SURGERY,
            new Quest("surgery_meds", "Northfield Surgery Receptionist",
                "We need an emergency supply assessment. Can you check what the community centre has?",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.COMMUNITY_CENTRE, Material.ANTIDEPRESSANTS, 3));

        register(LandmarkType.WETHERSPOONS,
            new Quest("spoons_pint", "Rusty Anchor Barman",
                "We've got a VIP punter asking for Nandos. Go check if they're open and come back.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.NANDOS, Material.PERI_PERI_CHICKEN, 1));

        register(LandmarkType.NANDOS,
            new Quest("nandos_chicken", "Nando's Manager",
                "We've had a rush on peri-peri. Bring me one portion and I'll upgrade your loyalty card.",
                ObjectiveType.COLLECT, Material.PERI_PERI_CHICKEN, 1,
                Material.ENERGY_DRINK, 3));

        register(LandmarkType.BARBER,
            new Quest("barber_clippers", "Kev",
                "Left me clippers at the pawn shop — the muppets took them. Go get 'em back.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.PAWN_SHOP, Material.PARACETAMOL, 2));

        register(LandmarkType.NAIL_SALON,
            new Quest("nails_polish", "Angel Nails",
                "I'm waiting on a supplier. Pop to the cash converters and check if they've got anything.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.CASH_CONVERTER, Material.ANTIDEPRESSANTS, 1));

        register(LandmarkType.CORNER_SHOP,
            new Quest("cornershop_crisps", "Happy Shopper Owner",
                "Crisp delivery is late again. Bring me 4 bags and I'll throw in something extra.",
                ObjectiveType.COLLECT, Material.CRISPS, 4,
                Material.TIN_OF_BEANS, 2));

        register(LandmarkType.BETTING_SHOP,
            new Quest("ladbrokes_card", "Ladbrokes Manager",
                "Need a couple of scratch cards for a VIP customer. Sort me out?",
                ObjectiveType.COLLECT, Material.SCRATCH_CARD, 2,
                Material.ENERGY_DRINK, 2));

        register(LandmarkType.PHONE_REPAIR,
            new Quest("phone_repair", "Fix My Phone Tech",
                "I need a broken phone to use for parts. Bring me one and I'll make it worth your time.",
                ObjectiveType.COLLECT, Material.BROKEN_PHONE, 1,
                Material.COMPUTER, 1));

        register(LandmarkType.CASH_CONVERTER,
            new Quest("cashconverter_dvd", "Cash Converters Manager",
                "Customer wants a dodgy DVD — don't ask. Bring one in and I'll pay over the odds.",
                ObjectiveType.COLLECT, Material.DODGY_DVD, 1,
                Material.PENNY, 12));

        register(LandmarkType.LIBRARY,
            new Quest("library_textbook", "Northfield Library",
                "We've had a rash of unreturned textbooks. Track down 2 for us, would you?",
                ObjectiveType.COLLECT, Material.TEXTBOOK, 2,
                Material.NEWSPAPER, 3));

        register(LandmarkType.OFFICE_BUILDING,
            new Quest("office_stapler", "Meridian House Facilities",
                "Someone's nicked the office stapler again. It's not funny. Bring it back.",
                ObjectiveType.COLLECT, Material.STAPLER, 1,
                Material.COMPUTER, 1));

        register(LandmarkType.COMMUNITY_CENTRE,
            new Quest("community_hymns", "Community Centre Coordinator",
                "We're putting on a hymn recital. Could you find us a hymn book?",
                ObjectiveType.COLLECT, Material.HYMN_BOOK, 1,
                Material.TIN_OF_BEANS, 3));

        register(LandmarkType.PRIMARY_SCHOOL,
            new Quest("school_textbook", "St. Aidan's Teacher",
                "Terrible — half our textbooks have gone missing. Bring me 2 and you'll be doing good.",
                ObjectiveType.COLLECT, Material.TEXTBOOK, 2,
                Material.CRISPS, 3));
    }

    private void register(LandmarkType type, Quest quest) {
        quests.put(type, quest);
    }

    /**
     * Get the quest for a given building type.
     * Returns null if no quest is defined for that type.
     */
    public Quest getQuest(LandmarkType type) {
        return quests.get(type);
    }

    /**
     * Check whether a landmark type has an associated quest.
     */
    public boolean hasQuest(LandmarkType type) {
        return quests.containsKey(type);
    }

    /**
     * Get the quest offer line — what the NPC says when the player first interacts.
     */
    public static String getQuestOfferLine(Quest quest) {
        return quest.getDescription();
    }

    /**
     * Get the NPC's response when the quest is already active but not completed.
     */
    public static String getQuestReminderLine(Quest quest) {
        return getQuestReminderLine(quest, null);
    }

    /**
     * Get the NPC's response when the quest is already active but not completed.
     * When an inventory is provided, includes the player's current item count.
     */
    public static String getQuestReminderLine(Quest quest, ragamuffin.building.Inventory inventory) {
        if (quest.getType() == Quest.ObjectiveType.COLLECT && quest.getRequiredMaterial() != null) {
            if (inventory != null) {
                int current = inventory.getItemCount(quest.getRequiredMaterial());
                int remaining = Math.max(0, quest.getRequiredCount() - current);
                return "You've got " + current + " — need " + remaining + " more "
                    + materialName(quest.getRequiredMaterial()) + ". Don't let me down.";
            }
            return "Still waiting on those " + quest.getRequiredCount() + " "
                + materialName(quest.getRequiredMaterial()) + ". Don't let me down.";
        }
        if (quest.getType() == Quest.ObjectiveType.EXPLORE && quest.getTargetLandmark() != null) {
            String name = quest.getTargetLandmark().getDisplayName();
            if (name == null) name = quest.getTargetLandmark().name().toLowerCase().replace('_', ' ');
            return "Not been yet? You need to head over to " + name + ". Off you go.";
        }
        return "You haven't finished the job yet.";
    }

    /**
     * Get the NPC's response when the quest is completed.
     */
    public static String getQuestCompletionLine(Quest quest) {
        if (quest.getReward() != null) {
            return "Cheers — here's your " + materialName(quest.getReward())
                + ". Pleasure doing business.";
        }
        return "Cheers. You're a lifesaver.";
    }

    private static String materialName(Material m) {
        return m.name().toLowerCase().replace('_', ' ');
    }
}

package ragamuffin.ui;

/**
 * All achievements in the game, with humorous names and descriptions that hint
 * at how to unlock them without being too obvious about it.
 *
 * Progress-based achievements include a target count; instant (one-shot)
 * achievements have a target of 1.
 */
public enum AchievementType {

    // --- Arrest / Police ---
    FOUGHT_THE_LAW(
        "I Fought the Law",
        "The law won. Probably. You were arrested.",
        1
    ),
    REPEAT_OFFENDER(
        "Frequent Flyer",
        "The custody sergeant knows you by name now. And your mum's number.",
        5
    ),

    // --- Combat / Punching ---
    FIRST_PUNCH(
        "Diplomatic Incident",
        "Violence is never the answer. Except when it is.",
        1
    ),
    BRAWLER(
        "Professional Disagreer",
        "You've resolved fifty disputes the old-fashioned way.",
        50
    ),
    GANG_AGGRO(
        "Wrong Postcode",
        "Some people take territorial disputes very seriously.",
        1
    ),

    // --- Block Breaking ---
    FIRST_BLOCK(
        "Property Damage",
        "It wasn't yours. That's kind of the point.",
        1
    ),
    LUMBERJACK(
        "Going Green (In Reverse)",
        "You've felled enough trees to concern a small environmental charity.",
        10
    ),
    BRICK_BY_BRICK(
        "Urban Renewal Enthusiast",
        "Demolishing fifty bricks. The council would not approve.",
        50
    ),
    GLAZIER(
        "Pane in the Glass",
        "You've broken enough windows to keep a glazier in business.",
        5
    ),

    // --- Survival / Hunger ---
    STARVING(
        "Austerity",
        "Hunger's just a state of mind. A very unpleasant state of mind.",
        1
    ),
    GREGGS_FAN(
        "The Blessed Sacrament",
        "You've eaten from Greggs. The nation's true cathedral.",
        3
    ),

    // --- Death / Respawn ---
    FIRST_DEATH(
        "Brief Setback",
        "You died. This is fine. Probably.",
        1
    ),
    NINE_LIVES(
        "Cockroach",
        "Nine deaths and still going. The estate can't get rid of you.",
        9
    ),

    // --- Weather ---
    COLD_SNAP_SURVIVOR(
        "Brass Monkey Weather",
        "Survived a cold snap. Barely. Fingers still attached.",
        1
    ),

    // --- Building / Crafting ---
    FIRST_CRAFT(
        "Blue Peter Badge (Denied)",
        "You crafted something. Whether it's useful is another matter.",
        1
    ),
    BUILDER(
        "Grand Designs (Budget)",
        "You've placed fifty blocks. Kevin would be disappointed.",
        50
    ),

    // --- Exploration ---
    PARK_VISITOR(
        "The Green Lung",
        "Visited the park. The pigeons judged you.",
        1
    ),
    JOBCENTRE_VISITOR(
        "Career Development",
        "Visited the JobCentre. Nobody's judging. We're all just... browsing.",
        1
    ),
    GREGGS_RAID(
        "Sausage Roll Insurgency",
        "Raided Greggs. The most British crime imaginable.",
        1
    ),

    // --- NPC Interactions ---
    FIRST_NPC_LOOT(
        "Redistributive Justice",
        "Took something from someone who no longer needed it. Allegedly.",
        1
    ),
    TALKED_TO_DRUNK(
        "Philosopher's Stone",
        "Had a conversation with a drunk. Gained profound wisdom. Probably.",
        1
    ),

    // --- Tools ---
    TOOL_BREAKER(
        "You Had ONE Job",
        "Broke a tool. It's fine. Tools are temporary. Incompetence is eternal.",
        3
    ),

    // --- Shelter ---
    CARDBOARD_CASTLE(
        "Grand Designs (Series B)",
        "Built a cardboard shelter. Kevin McCloud would weep.",
        1
    ),

    // --- Reputation ---
    NOTORIOUS(
        "Local Celebrity",
        "Achieved notorious status. Your face is on a laminated notice at the off-licence.",
        1
    ),

    // --- Distance travelled ---
    MARATHON_MAN(
        "Haven't Got a Bus Pass",
        "Walked the equivalent of a marathon. The 68 wasn't running again.",
        1000
    ),

    // --- Quests ---
    FIRST_QUEST(
        "Community Spirit",
        "Completed your first quest. Helping people? In this economy?",
        1
    ),
    QUEST_MASTER(
        "Odd Job Alan",
        "Completed ten quests. Everyone on the estate knows you can get things done.",
        10
    );

    private final String name;
    private final String description;
    private final int progressTarget; // 1 = instant unlock; >1 = tracked progress

    AchievementType(String name, String description, int progressTarget) {
        this.name = name;
        this.description = description;
        this.progressTarget = progressTarget;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getProgressTarget() {
        return progressTarget;
    }

    /** Convenience: achievements that fire on a single event. */
    public boolean isInstant() {
        return progressTarget == 1;
    }
}

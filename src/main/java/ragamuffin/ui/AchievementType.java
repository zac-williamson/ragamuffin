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
    ),

    // --- Heist (Phase O / Issue #704) ---
    MASTER_CRIMINAL(
        "You Absolute Wrong'un",
        "You absolute wrong'un. Completed a heist. Welcome to the big leagues.",
        1
    ),
    SMOOTH_OPERATOR(
        "In and Out. No Fuss.",
        "Completed a heist without triggering a single alarm. Absolute professional.",
        1
    ),
    FENCE_FRESH(
        "Still Warm, Son",
        "Fenced hot loot within 5 in-game minutes. The fence was impressed. Briefly.",
        1
    ),
    SOLO_JOB(
        "Trust No One",
        "Completed a heist without an accomplice. Lone wolf. Or just paranoid.",
        1
    ),

    // ── Phase 8e: Notoriety (Issue #709) ─────────────────────────────────────

    FIRST_PICKPOCKET(
        "Light Fingers",
        "Your first successful dip. Easy money. For now.",
        1
    ),
    LOCAL_NUISANCE(
        "Bit of a Handful",
        "You've become a Local Nuisance. The neighbourhood watch have your photo.",
        1
    ),
    SURVIVED_ARU(
        "Run That's Still Running",
        "Escaped from three consecutive Armed Response Unit pursuits. Somehow.",
        3
    ),
    RAGAMUFFIN(
        "The Ragamuffin",
        "Maximum notoriety. You are the most wanted person in the postcode. Probably in the borough.",
        1
    ),
    KEEPING_IT_QUIET(
        "Hush Money",
        "Bribed the fence three times to keep your name out of it. Expensive habit.",
        3
    ),
    THE_CREW(
        "Two-Man Job",
        "Completed a heist with an accomplice. Don't get attached.",
        1
    ),

    // ── Issue #712: Slumlord property system ──────────────────────────────────

    FIRST_PROPERTY(
        "Foot on the Ladder",
        "Bought your first property. Thatcher would be proud. Probably.",
        1
    ),
    ARMCHAIR_LANDLORD(
        "Armchair Landlord",
        "You own five properties and haven't lifted a paintbrush in weeks. Nice.",
        1
    ),
    SLUM_CLEARANCE(
        "Slum Clearance",
        "Repaired a building to full condition. The tenants are cautiously optimistic.",
        1
    ),
    THATCHER_WOULDNT(
        "Thatcher Wouldn't",
        "You're not Thatcher. Five properties is enough.",
        1
    ),
    RATES_DODGER(
        "Rates Dodger",
        "Ignored the council rates for a full week. Bold. Reckless. Very on-brand.",
        1
    ),

    // ── Issue #714: Player Squat system ──────────────────────────────────────

    SQUATTER(
        "Home is Where You Hang Your Hoody",
        "Claimed a derelict building as your own. Castle doctrine, British edition.",
        1
    ),
    LEGENDARY_SQUAT(
        "They Don't Make Them Like This in Kensington",
        "Reached Vibe 80+. Your gaff is a local institution. NPCs are talking.",
        1
    ),
    RUNNING_A_HOUSE(
        "You're Basically a Housing Association",
        "Four lodgers simultaneously. Someone's got to manage this place.",
        1
    ),
    BOUNCER_ON_DOOR(
        "Very Professional",
        "Hired the pub bouncer to guard your squat door. That's class, that.",
        1
    ),
    BARRICADED_IN(
        "Castle Doctrine, British Edition",
        "Survived three raids without Vibe dropping below 60. Proper fortified.",
        1
    ),

    // ── Issue #716: Underground Music Scene ──────────────────────────────────

    FIRST_BARS(
        "Spitting",
        "Won your first MC Battle. They weren't expecting that, were they.",
        1
    ),
    GRIME_GOD(
        "The Don",
        "Reached MC Rank 5. You are the grime scene. The grime scene is you.",
        1
    ),
    ILLEGAL_RAVE(
        "Police Don't Like It",
        "Hosted your first illegal rave. Someone's neighbours are furious.",
        1
    ),
    SWERVED_THE_FEDS(
        "Gone Before They Arrived",
        "Dispersed the rave before the police arrived. Not your first rodeo.",
        1
    ),
    BODIED(
        "Completely Bodied",
        "Won all three MC Battles in a single career. Absolute scenes.",
        1
    ),
    DOUBLE_LIFE(
        "By Day, By Night",
        "Reach MC Rank 3 while simultaneously owning a property. Legitimate. Sort of.",
        1
    ),

    // ── Issue #765: Witness & Evidence System ──────────────────────────────────

    CLEAN_GETAWAY(
        "Clean Getaway",
        "Committed a crime and left no evidence behind. Tidy.",
        1
    ),
    LEAVE_NO_TRACE(
        "Leave No Trace",
        "Destroyed or avoided all evidence from five separate crime events. Nothing to see here.",
        5
    ),
    GRASS(
        "Grassing",
        "Tipped off the police. Hidden achievement. Somebody's going to be very unhappy with you.",
        1
    ),
    IN_BROAD_DAYLIGHT(
        "In Broad Daylight",
        "Committed a crime while three or more NPCs were watching. Ballsy. Stupid. Both.",
        1
    ),
    STITCH_UP(
        "Proper Stitch-Up",
        "Used a RUMOUR_NOTE to pin something on someone else. You're going places. Bad places.",
        1
    ),

    // ── Issue #769: Dynamic NPC Needs & Black Market Economy ─────────────────

    ENTREPRENEUR(
        "Entrepreneur of the Year",
        "Completed your first street deal. The Prince's Trust would not be proud.",
        1
    ),
    LOAN_SHARK(
        "Neither a Borrower Nor a Lender Be",
        "Charged interest on a desperate NPC. Keynes would have thoughts.",
        1
    ),
    CORNERED_THE_MARKET(
        "Gordon Gekko of the High Street",
        "Cornered the market on a single commodity. Greed is good, allegedly.",
        1
    ),
    BENEFIT_FRAUD(
        "The System Works for Me",
        "Exploited the benefit day price spike for maximum profit. Entrepreneurial.",
        1
    ),
    COLD_SNAP_CAPITALIST(
        "Price Gouger",
        "Sold woolly hats at double price during a cold snap. Opportunistic.",
        1
    ),
    DODGY_AS_THEY_COME(
        "Dodgy as They Come",
        "Handled stolen goods, counterfeit notes, and prescription meds in a single session. Proper wrong 'un.",
        1
    ),

    // ── Issue #771: Hot Pursuit — Wanted System ──────────────────────────────

    LEG_IT(
        "Leg It",
        "Escaped police by running 80 blocks and breaking line of sight for 60 seconds. Pure bottle.",
        1
    ),
    BENT_COPPER(
        "Bent Copper",
        "Cultivated a corrupt PCSO with three flasks of tea. Everyone has a price.",
        1
    ),
    CLEAN_GETAWAY_PURSUIT(
        "Clean Getaway",
        "Lost the police and let your wanted level decay to zero. Not a trace.",
        1
    ),
    FIVE_STAR_NIGHTMARE(
        "Five Star Nightmare",
        "Reached 5 wanted stars. The entire borough knows your face.",
        1
    ),
    WHEELIE_BIN_HERO(
        "Wheelie Bin Hero",
        "Hid in a wheelie bin while police searched the area. British. Disgusting. Effective.",
        1
    ),
    INNOCENT_FACE(
        "Innocent Face",
        "Used a disguise change to reset the police description mid-pursuit. Butter wouldn't melt.",
        1
    ),

    // ── Issue #767: Disguise & Social Engineering System ──────────────────────

    UNDERCOVER(
        "Undercover",
        "Successfully used a disguise to infiltrate a restricted area. Method acting.",
        1
    ),
    METHOD_ACTOR(
        "Method Actor",
        "Maintained cover integrity above 50 throughout an entire infiltration. Stay in character.",
        1
    ),
    TURNCOAT(
        "Turncoat",
        "Wore a rival faction's colours and survived. Bold. Possibly stupid.",
        1
    ),
    OBVIOUS_IN_HINDSIGHT(
        "Obvious in Hindsight",
        "Had your cover blown within 3 blocks. The Greggs apron didn't fool anyone up close.",
        1
    ),
    INCOGNITO(
        "Incognito",
        "Completed a disguise infiltration without ever being scrutinised. Ghost mode.",
        1
    ),

    // ── Issue #774: The Daily Ragamuffin — Tabloid Newspaper System ───────────

    TABLOID_KINGPIN(
        "Tabloid Kingpin",
        "Planted a lie in the press. A rival's doing the time for your crime.",
        1
    ),
    REGULAR_READER(
        "Regular Reader",
        "Collected The Daily Ragamuffin seven days running. Keeping up with current events.",
        7
    ),
    FRONT_PAGE_VILLAIN(
        "Front Page Villain",
        "Reached an infamy score of 10. You're Britain's most wanted, apparently.",
        1
    ),
    NO_COMMENT(
        "No Comment",
        "Suppressed three damaging stories via buyout. The truth stays buried.",
        3
    ),
    PIGEON_MENACE(
        "Pigeon Menace",
        "Kept your head down for five full in-game days. The pigeons got more column inches than you.",
        5
    ),

    // ── Issue #781: Graffiti & Territorial Marking ────────────────────────────

    WRITER(
        "Writer",
        "Placed your first graffiti tag. Your name on the wall. Respect.",
        1
    ),
    GETTING_UP(
        "Getting Up",
        "Fifty tags placed. You're making your presence known across the estate.",
        50
    ),
    ALL_CITY(
        "All City",
        "Living tags in every zone simultaneously. This whole town is your gallery.",
        1
    ),
    SCRUBBED(
        "Scrubbed",
        "Ten of your tags removed by Council Cleaners. You're famous enough to be a nuisance.",
        10
    ),
    CLEAN_HANDS(
        "Clean Hands",
        "Completed a full in-game day without placing any tags, while holding a spray can. Suspicious restraint.",
        1
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

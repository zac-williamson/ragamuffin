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
    ),

    // ── Issue #783: Pirate FM — Underground Radio Station ────────────────────

    ON_AIR(
        "On Air",
        "Completed your first pirate broadcast. The community has spoken. Loudly.",
        1
    ),
    PIRATE_FM(
        "Pirate FM",
        "Broadcast for ten cumulative in-game minutes. The airwaves belong to the people. Allegedly.",
        10
    ),
    SIGNAL_JAM(
        "Signal Jam",
        "Destroyed a Council Signal Van. They're going to need a bigger budget.",
        1
    ),
    THE_PEOPLE_S_DJ(
        "The People's DJ",
        "Had six Listener NPCs arrive at your transmitter simultaneously. You're famous. Sort of.",
        1
    ),
    ENEMY_OF_THE_STATE(
        "Enemy of the State",
        "Your broadcast made the front page: 'RAGAMUFFIN FM: ENEMY OF THE STATE?' Brilliant.",
        1
    ),
    OFF_AIR(
        "Off Air",
        "Had your transmitter confiscated by a Signal Van. Should've turned it off earlier, mate.",
        1
    ),

    // ── Issue #785: The Dodgy Market Stall ────────────────────────────────────

    MARKET_TRADER(
        "Market Trader",
        "Opened your first dodgy market stall. Del Boy would be proud. Probably.",
        1
    ),
    LICENSED_TO_SELL(
        "Licensed to Sell",
        "Got a Market Licence before the inspector turned up. Technically legal. For once.",
        1
    ),
    BRIBED_THE_INSPECTOR(
        "Bribed the Inspector",
        "Slipped the Market Inspector a tenner to look the other way. Public service, really.",
        1
    ),
    SHUTIT_DOWN(
        "Shut It Down",
        "Had your stall closed by police. All stock confiscated. Back to the day job.",
        1
    ),
    EMPIRE_BUILDER(
        "Empire Builder",
        "Reached 200 COIN in lifetime stall sales. From a trestle table to a street empire.",
        1
    ),
    TURF_VENDOR(
        "Turf Vendor",
        "Set up a stall in faction territory and paid the protection cut. Business is business.",
        1
    ),

    // ── Issue #787: Street Skills & Character Progression ──────────────────────

    FIRST_BLOOD(
        "First Blood",
        "Won your first fight. The blood washes out. Probably.",
        1
    ),
    PROPER_HARD(
        "Proper Hard",
        "Reached BRAWLING Legend. The estate knows not to start on you.",
        1
    ),
    GRAFTER(
        "Grafter",
        "Reached GRAFTING Legend. You could survive on a car park roof for a week.",
        1
    ),
    WHEELERDEALER(
        "Wheeler Dealer",
        "Reached TRADING Legend. Del Boy's got nothing on you.",
        1
    ),
    GHOST(
        "Ghost",
        "Reached STEALTH Legend. You were never here. Nobody saw nothing.",
        1
    ),
    WORDS_ON_THE_STREET(
        "Words on the Street",
        "Reached INFLUENCE Legend. You've got a following. Whether that's good or bad is unclear.",
        1
    ),
    LEGEND_OF_THE_MANOR(
        "Legend of the Manor",
        "Reached SURVIVAL Legend. You've outlasted everyone who said you wouldn't.",
        1
    ),
    PICKPOCKET(
        "Light Fingers, Heavy Pockets",
        "Successfully pick-pocketed an NPC. They never felt a thing. Allegedly.",
        1
    ),
    RALLY_CRY(
        "Rally Cry",
        "Assembled a mob of followers using RALLY. Safety in numbers. Relative safety.",
        1
    ),

    // ── Issue #793: The Living Neighbourhood — Dynamic Gentrification ──────────

    LAST_OF_THE_LOCALS(
        "Last of the Locals",
        "Organised a community meeting and kept the developers out. The estate remembers.",
        1
    ),
    PROPERTY_DEVELOPER(
        "Property Developer",
        "Sold a building to developers. Thirty coins richer, thirty per cent less popular.",
        1
    ),
    DYSTOPIA_NOW(
        "Dystopia Now",
        "The Neighbourhood Vibes hit rock bottom. It was all going so well. Sort of.",
        1
    ),
    COMMUNITY_HERO(
        "Community Hero",
        "Tore down a condemned notice and gave the neighbourhood a fighting chance. Local legend.",
        1
    ),

    // ── Issue #797: The Neighbourhood Watch ───────────────────────────────────

    WATCHED(
        "They've Got Their Eye on You",
        "Triggered the Neighbourhood Watch for the first time. They've made a note.",
        1
    ),
    PETITION_RECEIVED(
        "Sign Here Please",
        "A petition board appeared. Democracy in action. Mostly against you.",
        1
    ),
    GROVELLED(
        "Undignified But Effective",
        "Grovelled successfully. Anger reduced. Dignity: also reduced.",
        1
    ),
    UPRISING_SURVIVED(
        "Against All Odds",
        "Survived a Full Uprising. Every NPC in the postcode wanted a word with you.",
        1
    ),
    PEACEMAKER(
        "Sausage Roll Diplomacy",
        "Converted a Watch Member with a peace offering. The power of pastry.",
        1
    ),
    NEWSLETTER_PUBLISHED(
        "Community Correspondent",
        "Removed a petition board with a neighbourhood newsletter. Local hero. Sort of.",
        1
    ),

    // ── Issue #801: The Underground Fight Night ───────────────────────────────

    FIRST_BLOOD_PIT(
        "First Blood (Pit)",
        "Won your first fight in the Pit. The crowd went wild. Relatively.",
        1
    ),
    CHAMPION_OF_THE_PIT(
        "Champion of the Pit",
        "Reached rank 1 on the Championship Ladder. Nobody messes with you now. In the basement, anyway.",
        1
    ),
    DIRTY_FIGHTER(
        "Dirty Fighter",
        "Won a fight after landing an eye-gouge. The ref didn't see it. There isn't a ref.",
        1
    ),
    CLEANED_OUT_THE_BOOKIE(
        "Cleaned Out the Bookie",
        "Drained the bookie's pot below 20 coins in a single fight night. He looked gutted.",
        1
    ),
    PROMOTED(
        "Fight Promoter",
        "Promoted a fight card via Pirate Radio. The whole estate knew about it by evening.",
        1
    ),
    UNDERCOVER_SPOTTER(
        "Undercover Spotter",
        "Identified and exposed the plain-clothes police officer in the Pit crowd. Good eyes.",
        1
    ),

    // ── Issue #906: Busking System ────────────────────────────────────────────

    STREET_PERFORMER(
        "Street Performer",
        "Started your first busk session. Percussive. Improvised. Very British.",
        1
    ),
    LIVING_WAGE(
        "Living Wage",
        "You've made more busking than a week at the JobCentre. The bucket never lies.",
        1
    ),
    BUCKET_LIST(
        "Bucket List",
        "Earned 20 coins from busking across all sessions. The dispossessed percussionist.",
        1
    ),
    MOVE_ALONG_PLEASE(
        "Move Along Please",
        "Had your bucket drum confiscated by police. Unlicensed street performance. Classic.",
        1
    ),

    // ── Issue #908: Bookies Horse Racing System ───────────────────────────────

    LUCKY_PUNT(
        "Lucky Punt",
        "Won your first bet at the bookies. Don't let it go to your head.",
        1
    ),
    OUTSIDER(
        "Outsider",
        "Won a bet at 10/1 or better. The form book knows nothing.",
        1
    ),
    RANK_OUTSIDER(
        "Rank Outsider",
        "Won at 33/1. The whole estate heard about it. Even the loan shark looked impressed.",
        1
    ),
    LOSING_STREAK(
        "Losing Streak",
        "Lost 50 coins net at the bookies. The horses were definitely trying their best.",
        1
    ),
    DEBT_FREE(
        "Debt Free",
        "Repaid a loan shark debt on time. A rare act of fiscal responsibility in this postcode.",
        1
    ),
    DAILY_PUNTER(
        "Daily Punter",
        "Bet on all 8 races in a single day. Some people have a system. This wasn't it.",
        1
    ),

    // ── Issue #799: The Corner Shop Economy ──────────────────────────────────

    OPEN_FOR_BUSINESS(
        "Open For Business",
        "Claimed your first derelict shop unit. Del Boy's looking worried.",
        1
    ),
    KERCHING(
        "Ker-ching!",
        "Earned 100 coins in a single day at your corner shop. Not bad for a grey market.",
        1
    ),
    PROTECTION_MONEY(
        "Protection Money",
        "Paid Marchetti's crew to look the other way. Business is business.",
        1
    ),
    THE_NEIGHBOURHOOD_SHOP(
        "The Neighbourhood Shop",
        "Achieved Street Lads Respect ≥ 70 — they're defending your stock now.",
        1
    ),
    RAIDED(
        "Raided",
        "Got your shop raided by police. They took everything. Start again.",
        1
    ),
    PRICE_WAR(
        "Price War",
        "Triggered a Marchetti enforcer visit by undercutting their off-licence prices.",
        1
    ),

    // ── Issue #914: Allotment System ──────────────────────────────────────────

    GREEN_FINGERS(
        "Down the Allotment",
        "Harvested your first crop. Get your hands dirty. The earth doesn't care about your problems.",
        1
    ),
    CHAMPION_GROWER(
        "Best in Show",
        "Won the Giant Vegetable Show. The neighbourhood has spoken. Your veg is the best.",
        1
    ),
    SELF_SUFFICIENT(
        "Off the Grid",
        "Harvested 20 crops total. Who needs Greggs when you've got your own patch?",
        20
    ),
    GOOD_NEIGHBOUR(
        "Keep Britain Tidy",
        "Received 3 compliments from plot neighbours without getting a complaint. Diplomatic. Somehow.",
        3
    ),

    // ── Issue #916: Late-Night Kebab Van ──────────────────────────────────────

    DIRTY_KEBAB(
        "After Midnight",
        "Ate a kebab from the late-night van. It's mostly meat. Mostly.",
        1
    ),
    FRONT_OF_THE_QUEUE(
        "Queue Jumper",
        "Queue-jumped three times at the kebab van. It's quicker, not nicer.",
        3
    ),
    DISTRACTION_TECHNIQUE(
        "Tin of Beans Stratagem",
        "Used a tin of beans to steal a kebab. Van owner never saw it coming.",
        1
    ),
    LAST_ORDERS(
        "Last Orders",
        "Bought from the kebab van during the last-orders discount window. Savvy.",
        1
    ),

    // ── Issue #918: Bus Stop & Public Transport System ─────────────────────────

    MISSED_THE_BUS(
        "Missed The Bus",
        "The Number 47 left without you. It was always going to be like this.",
        1
    ),
    FARE_DODGER(
        "Fare Dodger",
        "Boarded without paying. The inspector's somewhere on this bus. Probably.",
        1
    ),
    LAST_NIGHT_BUS(
        "Last Night Bus",
        "Took the Night Bus after 23:00. At least three passengers were unconscious.",
        1
    ),
    COMMUTER_PICKPOCKET(
        "Rush Hour Special",
        "Picked a commuter's pocket on a crowded bus. Professional. Morally questionable.",
        1
    ),

    // ── Issue #920: Pub Lock-In ────────────────────────────────────────────────

    LOCK_IN_REGULAR(
        "After Hours",
        "Attended five lock-ins at The Ragamuffin Arms. Terry knows your usual.",
        5
    ),
    STAYED_BEHIND_THE_BAR(
        "Under the Counter",
        "Hid behind the bar during a police raid. Terry was impressed. Briefly.",
        1
    ),
    QUIZ_NIGHT_CHAMPION(
        "Mastermind (Budget Edition)",
        "Won the Thursday pub quiz. Your specialist subject: being there.",
        1
    ),
    DARTS_HUSTLER(
        "One Hundred and Eighty",
        "Beat an NPC opponent at darts with a coin stake. Treble twenty, son.",
        1
    ),
    PUB_GRASS(
        "Judas",
        "Tipped off the police about the lock-in. Terry will never forgive you.",
        1
    ),
    LOCK_IN_LEGEND(
        "Last Man Standing",
        "Survived ten lock-ins without a DRUNK_AND_DISORDERLY charge. Legendary constitution.",
        10
    ),

    // ── Issue #922: Skate Park System ─────────────────────────────────────────

    KICKFLIP_KING(
        "Kickflip King",
        "Pulled off ten tricks in one session. The lads are genuinely impressed.",
        10
    ),
    COUNCIL_SABOTEUR(
        "Council Saboteur",
        "Torn down three closure notices. The Council has filed a strongly-worded memo about you.",
        3
    ),
    ASBO_MAGNET(
        "ASBO Magnet",
        "Three ASBOs. Your mum stopped answering the phone.",
        3
    ),
    PARK_LEGEND(
        "Park Legend",
        "Pulled a McTwist. The whole estate saw it. Even the council CCTV.",
        1
    ),

    // ── Issue #924: Launderette System ────────────────────────────────────────

    FRESH_START(
        "Fresh Start",
        "Completed your first wash cycle at the Spotless Launderette. Clean slate. Relatively.",
        1
    ),
    SMELLS_LIKE_CLEAN_SPIRIT(
        "Smells Like Clean Spirit",
        "Scrubbed your notoriety three times in the launderette. The machine knows your secrets.",
        3
    ),
    LAUNDERING(
        "Laundering",
        "Bought a stolen jacket from a dodgy bloke in a launderette. No questions asked.",
        1
    ),
    PEACEKEEPER_OF_SUDWORTH(
        "Peacekeeper of Sudworth",
        "Brokered peace during a machine theft dispute. Diplomacy. In a launderette.",
        1
    ),

    // ── Issue #926: Chippy System ──────────────────────────────────────────────

    SALT_AND_VINEGAR(
        "Salt & Vinegar",
        "Seasoned your chips with salt and vinegar. A bold choice. A correct choice.",
        1
    ),
    LAST_ORDERS_CHIPPY(
        "Last Orders",
        "Bought chips from Tony's within 10 minutes of closing. Midnight grease. The best grease.",
        1
    ),
    FED_THE_CAT(
        "Fed the Cat",
        "Fed Biscuit three times. She didn't say thank you but she looked grateful. Sort of.",
        3
    ),
    QUEUE_JUMPER(
        "Queue Jumper",
        "Jumped the post-pub queue at Tony's. Someone's kebab was delayed for this.",
        1
    ),
    CAT_PUNCHER(
        "Cat Puncher",
        "You punched Biscuit. The whole town knows. Tony knows. Even the cat knows.",
        1
    ),
    CHIPPY_REGULAR(
        "Chippy Regular",
        "Bought a fish supper from Tony's five times. Your arteries have filed a formal complaint.",
        5
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

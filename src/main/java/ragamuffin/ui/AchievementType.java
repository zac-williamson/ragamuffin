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

    // ── Issue #928: Public Library System ─────────────────────────────────────

    BOOKWORM(
        "Bookworm",
        "Read three books in the library in a single day. Self-improvement. Sort of.",
        3
    ),
    NIGHT_OWL(
        "Night Owl",
        "Slept rough in the library after closing time. The librarian would be horrified.",
        1
    ),
    SELF_IMPROVEMENT(
        "Self-Improvement",
        "Gained XP from reading in the library five times. The estate should try it.",
        5
    ),
    SHUSHED(
        "Shhhhh!",
        "Got shushed by the librarian. She's faster than she looks.",
        1
    ),
    EJECTED_FROM_LIBRARY(
        "Banned from the Library",
        "Got ejected from the library. The quietest place in the postcode and you couldn't manage it.",
        1
    ),
    FLASK_OF_SYMPATHY(
        "Flask of Sympathy",
        "Survived a cold night by sleeping in the library. The tea-coloured stain on the carpet was already there.",
        1
    ),

    // ── Issue #930: Charity Shop System ──────────────────────────────────────

    CHARITY_SHOP_DIAMOND(
        "Diamond Geezer",
        "Found a diamond in a mystery bag from the charity shop. Against all statistical likelihood.",
        1
    ),
    DIAMOND_DONOR(
        "Big Giver",
        "Donated a diamond to the charity shop. The volunteer almost fainted. It's going straight to the display cabinet.",
        1
    ),
    COMMUNITY_SERVICE(
        "Outstanding Citizen",
        "Made five donations to the charity shop. The volunteer has started leaving your name off the suspect list.",
        5
    ),
    TIGHT_FISTED(
        "Every Penny",
        "Successfully haggled the price down at the charity shop. Even in a charity shop, you couldn't pay full price.",
        1
    ),

    // ── Issue #932: Ice Cream Van System ──────────────────────────────────────

    KING_OF_THE_ROAD(
        "King of the Road",
        "Destroyed the rival ice cream van during a Jingle War. The estate is yours.",
        1
    ),
    MISTER_FROSTY(
        "Mister Frosty",
        "Sold ice cream from the stolen van 10 times. Entrepreneurship, British-style.",
        10
    ),
    CHOC_ICE_COLD(
        "Choc Ice Cold",
        "Bought a Choc Ice during a cold snap. Against all reason. Against all instinct.",
        1
    ),
    FREE_RIDE(
        "Free Ride",
        "Used an Oyster Card Lolly to board the Number 47. The driver was baffled.",
        1
    ),

    // ── Issue #934: Pigeon Racing System ──────────────────────────────────────

    HOME_BIRD(
        "Home Bird",
        "Your pigeon completed its first homing race. It found its way back. Unlike some people.",
        1
    ),
    CHAMPION_OF_THE_LOFT(
        "Champion of the Loft",
        "Won three neighbourhood races with the same pigeon. She's a proper racer now.",
        3
    ),
    NORTHFIELD_DERBY(
        "Northfield Derby",
        "Won the Northfield Derby. A proud day. The pigeon fancier raised an eyebrow of respect.",
        1
    ),
    CAUGHT_IT_YERSELF(
        "Caught It Yerself",
        "Acquired a racing pigeon by catching a wild BIRD NPC in the park. Old-school.",
        1
    ),
    BREAD_WINNER(
        "Bread Winner",
        "Used 10 bread crusts to train your pigeon. Dedication. Frugal, but dedicated.",
        10
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
    ),

    // ── Issue #936: Council Skip & Bulky Item Day ─────────────────────────────

    SKIP_KING(
        "Skip King",
        "Salvaged 5 or more items from a single Bulky Item Day. The street is basically your warehouse now.",
        1
    ),

    ANTIQUE_ROADSHOW(
        "Antique Roadshow",
        "Sold an Antique Clock to the Fence. He had questions. You had answers. Sort of.",
        1
    ),

    EARLY_BIRD(
        "Early Bird",
        "First to take an item from the skip on Bulky Item Day. The early scavenger gets the sofa.",
        1
    ),

    // ── Issue #938: Greasy Spoon Café ─────────────────────────────────────────

    FULL_ENGLISH_FANATIC(
        "Full English Fanatic",
        "Eaten a full English on five separate days. Vera knows your order before you open your mouth.",
        5
    ),
    WELL_INFORMED(
        "Well Informed",
        "Heard three different rumours in a single visit to Vera's Caff. Knowledge is power. Allegedly.",
        3
    ),
    CAFF_REGULAR(
        "Proper Regular",
        "Visited Vera's Caff on seven consecutive days. You've got your own mug now. Figuratively.",
        7
    ),

    // ── Issue #940: Wheelie Bin Fire System ──────────────────────────────────

    PYROMANIAC(
        "Man of the People",
        "Set three wheelie bins alight in a single night. Saturday night entertainment — no subscription required.",
        3
    ),
    FIRE_WARDEN(
        "Community Champion",
        "Extinguished a bin fire with a fire extinguisher before the fire engine arrived. Hero. Or grass. Depending on who's watching.",
        1
    ),

    // ── Issue #942: Food Bank System ──────────────────────────────────────────

    HEARTS_AND_MINDS(
        "Hearts and Minds",
        "Donated to the food bank on five separate days. Margaret is cautiously optimistic about you.",
        5
    ),
    ROUGH_WEEK(
        "Rough Week",
        "Collected an emergency parcel three separate times. No questions asked. That's the point.",
        3
    ),

    // ── Issue #946: Status Dog — Staffy Companion ─────────────────────────────

    MANS_BEST_FRIEND(
        "Man's Best Friend",
        "Adopted the stray Staffy from the park. He's not much, but he's yours. Probably.",
        1
    ),
    GOOD_BOY_GOOD_BOY(
        "Good Boy. Good Boy.",
        "Taught your dog all four tricks. The neighbourhood is cautiously impressed.",
        1
    ),
    DANGEROUS_DOG(
        "Dangerous Dog",
        "Used your dog to intimidate someone five times. The council has a file on you. And the dog.",
        5
    ),

    // ── Issue #948: Hand Car Wash ─────────────────────────────────────────────

    HONEST_DAYS_WORK(
        "Honest Day's Work",
        "Four shifts at the car wash. Your nan would be proud.",
        4
    ),

    SOAPY_BANDIT(
        "Soapy Bandit",
        "Worked for it, then nicked it back. Efficient.",
        1
    ),

    // ── Issue #950: Northfield Leisure Centre ─────────────────────────────────

    TYPICAL(
        "Typical",
        "Found the sauna out of order. It's been like that since 2009. Nobody's surprised.",
        1
    ),

    // ── Issue #952: Clucky's Fried Chicken ────────────────────────────────────

    SEVEN_WINGS(
        "Seven Wings",
        "Ate seven chicken wings in one sitting outside Clucky's. At midnight. Alone.",
        7
    ),

    WING_TAXED(
        "Wing Tax",
        "A youth took your box. You stood there. This is the story of the high street.",
        1
    ),

    STAND_YOUR_GROUND(
        "Stand Your Ground",
        "Refused to hand over your wings and survived the consequences. Barely.",
        1
    ),

    LITTER_HERO(
        "Community Service",
        "Picked up five pieces of chicken litter and binned them. Unpaid. Almost noble.",
        5
    ),

    // ── Issue #963: Northfield Canal ──────────────────────────────────────────

    CANAL_CATCH(
        "Gone Fishing",
        "Caught your first fish from the canal. The water is brown. The fish is not.",
        1
    ),
    TROLLEY_FISHERMAN(
        "Aisle 12",
        "Pulled a shopping trolley out of the canal. Worth more than the fish, honestly.",
        1
    ),
    EVIDENCE_IN_THE_CUT(
        "The Cut Sees Everything",
        "Disposed of evidence in the canal unwitnessed. The pike know. They're not talking.",
        1
    ),
    NIGHT_SWIMMER(
        "Hypothermia Chic",
        "Entered the canal after dark. The PCSO looked on in bewilderment.",
        1
    ),

    // ── Issue #961: Cash4Gold Pawn Shop ───────────────────────────────────────

    SUNDAY_MORNING_REGRET(
        "Sunday Morning Regret",
        "Forfeited a pawn pledge on Day 4. Gary kept it. You knew this would happen.",
        1
    ),
    IN_HOC(
        "In Hoc",
        "Three simultaneous pledges at Cash4Gold. Gary has a poster of you on the wall. Not in a good way.",
        1
    ),
    CASH_IN_HAND(
        "Cash in Hand",
        "Sold ten items to Gary in one session. He's beginning to ask fewer questions.",
        10
    ),

    // ── Issue #954: Northfield Bingo Hall ─────────────────────────────────────

    EYES_DOWN(
        "Eyes Down",
        "Won a full house at Lucky Stars Bingo. No assistance. Just luck and a very large felt-tip.",
        1
    ),

    CHEEKY_DABBER(
        "Cheeky Dabber",
        "Won using a rigged card and didn't get caught. The caller suspects nothing. Probably.",
        1
    ),

    SOLIDARITY(
        "Solidarity",
        "Ejected by a pensioner uprising. You hit one of them. In a bingo hall. On a Tuesday.",
        1
    ),

    // ── Issue #965: Northfield Snooker Hall ───────────────────────────────────

    FIRST_FRAME(
        "First Frame",
        "Played your first frame at Cue Zone. The green baize wept.",
        1
    ),

    SNOOKER_HUSTLER(
        "The Hustler",
        "Successfully hustled Frank. He never saw it coming. You nearly felt bad.",
        1
    ),

    SNOOKER_LEGEND(
        "Cue Sport Legend",
        "Defeated One-Armed Carl at snooker. Nobody is speaking. The chalk dust settles.",
        1
    ),

    BACK_ROOM_WINNER(
        "Back Room Winner",
        "Won the back-room pontoon game at Cue Zone. The Marchetti boys respect a winner.",
        1
    ),

    CHALK_AND_TALK(
        "Chalk and Talk",
        "Bought chalk from Dennis. He looked genuinely pleased. It's his only consistent income.",
        1
    ),

    // ── Issue #967: Northfield Taxi Rank ──────────────────────────────────────

    DODGY_PACKAGE(
        "Don't Ask, Don't Tell",
        "Received a mysterious package from Dave's Minicab. You didn't ask. Smart.",
        1
    ),

    LAST_FARE(
        "Last Fare",
        "Took Dave's minicab after 02:00. He was the only one still running. You didn't want to know why.",
        1
    ),

    REGULAR_CUSTOMER(
        "Regular Customer",
        "Used A1 Taxis five times. Mick knows your usual destinations. He doesn't comment. Much.",
        5
    ),

    // ── Issue #969: Northfield Cemetery ───────────────────────────────────────

    GRAVE_ROBBER(
        "Grave Robber",
        "Successfully dug and looted a grave in Northfield Cemetery. Low point, this.",
        1
    ),

    RESPECTFUL(
        "Paying Your Respects",
        "Attended a full funeral procession without disrupting it. It cost you nothing.",
        1
    ),

    POCKET_FULL_OF_SORROW(
        "Pocket Full of Sorrow",
        "Sold a wedding ring or pocket watch to the pawn shop. Gary didn't ask where you got it.",
        1
    ),

    CEMETERY_NIGHT_OWL(
        "Night Owl (Cemetery)",
        "Entered the cemetery after midnight. What were you looking for? Best not to think about it.",
        1
    ),

    // ── Issue #971: The Rusty Anchor Wetherspoons ──────────────────────────────

    CURRY_CLUB(
        "Curry Club",
        "Ate the Curry Club Special on a Thursday at The Rusty Anchor. Four quid. Can't go wrong.",
        1
    ),

    SEVEN_AM_PINT(
        "Hair of the Dog",
        "Bought a pint at The Rusty Anchor before 08:00. Wetherspoons is the only place that understands you.",
        1
    ),

    WETHERSPOONS_REGULAR(
        "Local",
        "Bought 10 drinks at The Rusty Anchor. Gary knows your face. He doesn't seem happy about it.",
        10
    ),

    APP_AT_THE_TABLE(
        "There's an App for That",
        "Ordered at the table via the Spoons app mechanic. Gary eventually found you. He wasn't pleased.",
        1
    ),

    SLEEPING_DRUNK_PICKPOCKET(
        "Don't Mind If I Do",
        "Pickpocketed the sleeping drunk at The Rusty Anchor. He'll never know. Probably.",
        1
    ),

    // --- Issue #973: Northfield GP Surgery ---
    JUST_PARACETAMOL(
        "Is That It?",
        "Received a Paracetamol prescription from Dr. Kapoor. He gave you paracetamol. Of course he did.",
        1
    ),

    SIGNED_OFF_SICK(
        "Doctor's Orders",
        "Received a sick note and used it at the JobCentre. The DWP were sympathetic. For once.",
        1
    ),

    PRESCRIPTION_FRAUDSTER(
        "Pharmacist's Worst Nightmare",
        "Successfully forged a prescription at the pharmacy hatch. Bold. Very bold.",
        1
    ),

    FULL_WAITING_ROOM(
        "Take a Number",
        "Entered the GP Surgery waiting room when all 5 patient chairs were already occupied.",
        1
    ),

    DONT_MIX_WITH_ALCOHOL(
        "Medical Advice Ignored",
        "Drank a pint within 10 minutes of taking Strong Meds. The leaflet warned you.",
        1
    ),

    // ── Issue #975: Northfield Post Office ────────────────────────────────────

    SIGNED_FOR_IT(
        "Signed For It",
        "Cashed your benefits book at the Post Office for the first time. Every week. Without fail. Allegedly.",
        1
    ),

    LUCKY_DIP(
        "Lucky Dip",
        "Won the 25 COIN jackpot on a scratch card. Maureen looked almost pleased for you.",
        1
    ),

    SPECIAL_DELIVERY(
        "Special Delivery",
        "Stole five parcels from doorsteps in a single morning. Worse than Amazon, and that's saying something.",
        5
    ),

    GOING_POSTAL(
        "Going Postal",
        "Completed a full postman delivery shift without diverting a single parcel. Almost honest work.",
        1
    ),

    DEAR_SIR(
        "Dear Sir or Madam",
        "Sent a threatening letter through the Royal Mail. First class. Because you mean business.",
        1
    ),

    // ── Issue #977: Northfield Amusement Arcade ───────────────────────────────

    PENNY_KING(
        "Penny King",
        "Hit the penny-falls jackpot. Thirty twopences. The arcade has never felt such shame.",
        1
    ),

    AGAINST_ALL_ODDS(
        "Against All Odds",
        "Won the claw machine on your very first attempt. Pure skill. Probably luck. Don't tell Kevin.",
        1
    ),

    TILTED(
        "Tilted",
        "Triggered a machine tilt three times in one visit. Kevin was not impressed. Kevin is never impressed.",
        1
    ),

    SKOOL_SKIVER(
        "Skool Skiver",
        "Spent three hours in the arcade during school hours. The register will show you absent. Allegedly.",
        1
    ),

    // ── Issue #981: Council Estate ────────────────────────────────────────────

    LEGS_OF_STEEL(
        "Legs of Steel",
        "Climbed to the top floor via the stairs while the lift was out of order. The council said they'd fix it last month.",
        1
    ),

    ROOFTOP_DEALER(
        "Rooftop Dealer",
        "Completed 5 item sales to council flat residents in a single day. Entrepreneurial spirit, if nothing else.",
        1
    ),

    URBAN_ARTIST(
        "Urban Artist",
        "Tagged 10 stairwell walls in a single visit. You've left your mark on the estate. The cleaners will be round Thursday.",
        1
    ),

    // ── Issue #983: Northfield Dog Track ─────────────────────────────────────

    PUNTER(
        "Down the Dogs",
        "Placed your first bet at the Northfield Dog Track. Welcome to the most reliable way to lose money in Northfield.",
        1
    ),

    LUCKY_DOG(
        "Lucky Dog",
        "Won a greyhound bet at 8/1 or better. The dog did all the work. You just held a slip.",
        1
    ),

    TRACK_FIXER(
        "Bent as a Nine-Bob Note",
        "Successfully fixed a greyhound race — either by bribing the kennel hand or slipping a dodgy pie to a dog. Marchetti would be proud.",
        1
    ),

    NICKED_THE_GREYHOUND(
        "Nicked the Greyhound",
        "Stole a live greyhound from the kennel and fenced it. You absolute liability. Bless you.",
        1
    ),

    BROKE_THE_TOTE(
        "Broke the Tote",
        "Won three consecutive greyhound races in a single session. The Marchetti Crew have noticed. That might not be good.",
        1
    ),

    // ── Issue #985: Northfield Police Station ─────────────────────────────────

    BANG_TO_RIGHTS(
        "Bang to Rights",
        "Processed through the custody suite for the first time. The fingerprint machine doesn't lie.",
        1
    ),

    ONE_PHONE_CALL(
        "One Phone Call",
        "Used the cell telephone during an arrest. Someone answered. You owe them one.",
        1
    ),

    GREAT_ESCAPE(
        "The Great Escape",
        "Broke out of a police cell with a lockpick. McQueen would approve. The duty sergeant would not.",
        1
    ),

    PROPER_GRASS(
        "Proper Grass",
        "Used the Tip Off menu at Northfield Police Station. In this neighbourhood, snitches do get stitches.",
        1
    ),

    EVIDENCE_LOST(
        "Evidence Lost",
        "Reclaimed your own confiscated items from the police evidence locker. Technically they were yours to begin with.",
        1
    ),

    // ── Issue #789: The Boot Sale — Underground Auction ───────────────────────

    FIRST_LOT(
        "Got Your Eye In",
        "Won your first lot at the boot sale. Don't ask where it came from. Nobody will.",
        1
    ),
    HIGH_ROLLER(
        "High Roller",
        "Won a HIGH-risk lot at the boot sale without police turning up. Beautiful. Absolutely beautiful.",
        1
    ),
    MARKET_KING(
        "King of the Carpark",
        "Won five lots in a single boot sale day. The Marchetti boys are taking notice.",
        1
    ),
    BOOT_SALE_REGULAR(
        "Regular Punter",
        "Won twenty lots in total across all boot sales. You've got a system. Or just no shame.",
        20
    ),

    // ── Issue #702: Three-Faction Turf War ────────────────────────────────────

    FACTION_FRIEND(
        "Useful to Someone",
        "Reached Friendly status with any faction. They might not shoot you first. Might.",
        1
    ),
    FACTION_ENEMY(
        "Wrong Side of the Street",
        "Let all three factions drop to Hostile. The whole town wants a word with you.",
        1
    ),
    MARCHETTI_MANOR(
        "Marchetti Manor",
        "Helped the Marchetti Crew reach faction victory. The off-licence is all yours. Sort of.",
        1
    ),
    STREETS_LIBERATED(
        "Streets Liberated",
        "Helped the Street Lads reach faction victory. The park is free. So are the youths.",
        1
    ),
    COUNCIL_APPROVED(
        "Council Approved",
        "Helped The Council reach faction victory. They tore down your shed. You still got a laminated ID.",
        1
    ),

    // ── Issue #698: Campfire System ────────────────────────────────────────────

    FIRST_CAMPFIRE(
        "Urban Survivor",
        "Lit your first campfire on the street. The warmth is real. The legality is questionable.",
        1
    ),
    CAMPFIRE_COOK(
        "Into the Wild (Northfield Edition)",
        "Survived three cold snaps using only campfires for warmth. Ray Mears would understand.",
        3
    ),

    // ── Issue #773: Car Driving ────────────────────────────────────────────────

    BEHIND_THE_WHEEL(
        "Behind the Wheel",
        "Got into a car and drove it. Technically. The definition of 'drove' is flexible here.",
        1
    ),
    JOYRIDER(
        "Tax, Test, Insurance",
        "Driven 200 blocks in total. The DVLA doesn't know. The PCSO suspects.",
        200
    ),

    // ── Issue #901: Bista Village ──────────────────────────────────────────────

    PORTAL_TOURIST(
        "Mind the Gap",
        "Stepped through the portal to Bista Village for the first time. Didn't even ask where it goes.",
        1
    ),
    RETURN_TRIP(
        "There and Back Again",
        "Made three return trips through the Bista Village portal. You know the way now.",
        3
    ),

    // ── Issue #950: Northfield Leisure Centre (additional) ────────────────────

    REGULAR_SWIMMER(
        "Lane Hogger",
        "Completed five swim sessions at Northfield Leisure Centre. Chlorine is your cologne now.",
        5
    ),
    VENDING_FIEND(
        "All Four Food Groups",
        "Bought all three vending machine items in a single visit to the leisure centre. Nutrition.",
        1
    ),

    // ── Issue #998: Northfield Aldi Supermarket ────────────────────────────────

    YELLOW_STICKER_LEGEND(
        "Yellow Sticker Legend",
        "Exploited yellow-sticker hour at Aldi. Everything's 0 COIN between seven and nine. The pensioners know.",
        1
    ),
    BLIND_SPOT_ARTIST(
        "Blind Spot Artist",
        "Lifted an item from the blind-spot corner without Dave clocking you. Professional. Borderline artistic.",
        1
    ),
    GOLDEN_TROLLEY(
        "Golden Trolley",
        "Found the golden shopping trolley in the car park. Twenty coin. Don't ask where it came from.",
        1
    ),

    // ── Issue #1000: Northfield Fire Station ──────────────────────────────────

    EMERGENCY_SERVICES(
        "Emergency Services",
        "Called in a false alarm to the fire station. They weren't best pleased. Worth it.",
        1
    ),
    GREAT_ENGINE_HEIST(
        "The Great Engine Heist",
        "Stole the actual fire engine. Three wanted stars. Twenty-five notoriety. Completely worth it.",
        1
    ),

    // ── Issue #1008: St. Mary's Church ────────────────────────────────────────

    BLESS_YOU(
        "Bless You",
        "Received your first blessing from Reverend Dave. God forgives freely. The council does not.",
        1
    ),
    COLLECTION_THIEF(
        "Passing the Plate",
        "Stole the collection plate. Either very brave or very wrong. Possibly both.",
        1
    ),
    BELL_RINGER(
        "Ding Dong",
        "Rang the church bell for the first time. Half of Northfield looked up.",
        1
    ),
    SANCTUARY_SEEKER(
        "God's on Your Side",
        "Lost a wanted star by sheltering in St. Mary's Church. The police felt awkward about it.",
        1
    ),
    REGULAR_PARISHIONER(
        "Regular Parishioner",
        "Attended five Sunday services. God is starting to recognise your face.",
        5
    ),

    // ── Issue #1020: Northfield Sporting & Social Club ────────────────────────

    FULL_MEMBER(
        "Card-Carrying Member",
        "Became a full member of the Northfield Sporting & Social Club. Derek shook your hand. Firmly.",
        1
    ),
    QUIZ_CHAMPION(
        "Quiz Champion",
        "Won Thursday Quiz Night with a perfect score. Derek announced it to the whole room. Twice.",
        1
    ),
    DARTS_HUSTLER_CLUB(
        "Darts Hustler",
        "Won 5 COIN off a MEMBER in a darts challenge at the social club. You didn't even look like you were trying.",
        1
    ),
    AGM_TROUBLEMAKER(
        "Point of Order",
        "Successfully moved a motion at the AGM. The committee looked deeply uncomfortable.",
        1
    ),
    COMMITTEE_CONSPIRACY(
        "Follow the Money",
        "Witnessed Derek's protection payment to the Marchetti Crew. Sometimes it's better not to know.",
        1
    ),

    // ── Issue #1022: Northfield GP Surgery ────────────────────────────────────

    HYPOCHONDRIAC(
        "Frequent Flyer (NHS Edition)",
        "Booked and attended five GP appointments. Brenda knows your name now.",
        5
    ),
    OFF_SICK(
        "Doctor's Note",
        "Used a sick note to dodge a JobCentre sanction. Technically legitimate.",
        1
    ),
    SELF_MEDICATING(
        "Taking Matters Into Your Own Hands",
        "Raided the medicine cabinet. You're basically a pharmacist now.",
        1
    ),
    SURGERY_RAIDER(
        "Highly Irregular",
        "Cracked the drug safe. Dr. Nair is absolutely furious.",
        1
    ),
    GOOD_SAMARITAN(
        "Just Doing My Bit",
        "Completed three fetch-prescription quests for patients. Pure of heart.",
        3
    ),
    WAITING_LIST(
        "Still Waiting",
        "Sat in the waiting room for over 30 in-game minutes. You've earned it.",
        1
    ),

    // ── Issue #1026: Northfield Scrapyard ────────────────────────────────────

    WEIGHT_FOR_IT(
        "Weight for It",
        "Sold your first load of scrap at the weigh-bridge. Gary wasn't impressed.",
        1
    ),
    COPPER_THIEF(
        "Sparky",
        "Stripped the cables off a streetlight and flogged them before dawn. Very British.",
        1
    ),
    HEAVY_METAL(
        "Heavy Metal",
        "Sold 20 pieces of scrap metal in a single session. Gary's warming to you.",
        20
    ),
    CLEAN_SLATE(
        "Clean Slate",
        "Used the crusher to destroy three pieces of evidence. No body, no crime.",
        3
    ),
    DOGS_DINNER(
        "Dog's Dinner",
        "Distracted Tyson with a sausage roll and raided the locked compound. Respect.",
        1
    ),

    // ── Issue #1028: Northfield Cash Converters ───────────────────────────────

    DIGITAL_POVERTY(
        "Digital Poverty",
        "Sold a games console to pay the gas bill. No judgement.",
        1
    ),
    CLEAN_IMEI(
        "Clean IMEI",
        "Sold a wiped phone to Dean without him batting an eyelid. Tariq did good work.",
        1
    ),
    NIGHT_SHIFT(
        "Night Shift",
        "Made a sale to Dave the Middleman in the back alley after midnight. Very discreet.",
        1
    ),
    STORE_CREDIT_MILLIONAIRE(
        "Store Credit Millionaire",
        "Earned 50 COIN in total from Cash Converters. Dean knows your face by now.",
        50
    ),
    CCTV_BLIND_SPOT(
        "Blind Spot",
        "Smashed the CCTV before selling hot goods to Dean. Evidence? What evidence?",
        1
    ),

    // ── Issue #1030: Al-Noor Mosque ────────────────────────────────────────────

    IFTAR_GUEST(
        "Breaking Bread",
        "Ate at the Iftar table during Ramadan. You were welcome, and you knew it.",
        1
    ),
    JUMU_AH_REGULAR(
        "Friday Feeling",
        "Attended Friday Jumu'ah three times. The Imam recognised your face.",
        3
    ),
    LOWEST_OF_THE_LOW(
        "Below Rock Bottom",
        "Robbed the mosque collection box. The whole neighbourhood knows. Even the kids.",
        1
    ),
    COMMUNITY_PILLAR(
        "Community Pillar",
        "Donated to the mosque five times. Hassan nodded at you. High praise.",
        5
    ),

    // ── Issue #1033: St. Aidan's C.E. Primary School ─────────────────────────

    SCHOOL_DINNER(
        "Turkey Twizzlers",
        "You bought a school dinner. A highlight, honestly.",
        1
    ),
    SAFE_CRACKER_JR(
        "Petty Cash",
        "You robbed a primary school. This is rock bottom.",
        1
    ),
    NIGHT_SCHOOL(
        "After Hours",
        "You broke into the school after dark. Old habits.",
        1
    ),
    TEACHERS_PET(
        "Model Citizen",
        "Derek shared a rumour with you. He thinks you're alright.",
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

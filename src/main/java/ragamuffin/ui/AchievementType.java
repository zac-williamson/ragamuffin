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

    // ── Issue #1325: Northfield Nightclub — The Vaults ────────────────────────

    FIRST_TIMER(
        "First Night Out",
        "Talked your way past Big Dave and made it into The Vaults. You're practically a local.",
        1
    ),
    LAST_MAN_STANDING(
        "Last Man Standing",
        "Won three dancefloor brawls in a single night. Security are writing a report about you.",
        3
    ),
    CRACKING_THE_VAULTS(
        "Cracking The Vaults",
        "Cracked the manager's safe. Terry's going to have a very bad morning.",
        1
    ),
    BARRED_FOR_LIFE(
        "Barred for Life",
        "Ejected from The Vaults on three separate nights. Big Dave's got a photo of you behind the bar.",
        3
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

    /**
     * Awarded after successfully refusing the Wing Tax 5 times without giving up chicken.
     * (Stare-down or Run result; fight triggered or avoided — either counts as refusal.)
     */
    WING_DEFENDER(
        "Not on My Watch",
        "Refused to hand over your wings five times. The youth moved on. You did not.",
        5
    ),

    /**
     * Awarded after marking up the CHICKEN_BOX price to 6 COIN (NEGOTIATE ≥ 2)
     * for the Late-Night Lockout Hustle on 3 separate occasions.
     */
    LATE_NIGHT_ENTREPRENEUR(
        "Late-Night Entrepreneur",
        "Sold a Chicken Box to a drunk at double the price. Three times. Capitalism.",
        3
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

    // ── Issue #1309: Northfield Ace Amusements Arcade (extended) ─────────────

    PENNY_CASCADE(
        "Penny Cascade",
        "Won the penny-falls timing bar in the green zone. Pure reflex. Kevin watched the whole thing.",
        1
    ),

    CLAW_MASTER(
        "Claw Master",
        "Won the claw machine on the first attempt. The machine was not rigged. Probably.",
        1
    ),

    ARCADE_LEGEND(
        "Arcade Legend",
        "Won the arcade shooter without missing three times. Ten kills, clean. The high score board has your name.",
        1
    ),

    DODGY_ENGINEER(
        "Dodgy Engineer",
        "Tampered with an arcade machine while Kevin wasn't looking. He's not paid enough to catch you.",
        1
    ),

    REDEMPTION_ARC(
        "Redemption Arc",
        "Exchanged prize tickets for the Arcade Champion Badge. Kevin printed it himself. He cried a little.",
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

    // ── Issue #1259: Northfield Pub Quiz Night ────────────────────────────────

    HAT_TRICK_QUIZZER(
        "Hat-Trick Quizzer",
        "Won three consecutive Wednesday Quiz Nights at The Rusty Anchor. Derek is starting to look suspicious.",
        3
    ),
    CHEATS_NEVER_PROSPER(
        "Cheats Never Prosper",
        "Got caught using a cheat sheet by Derek. He held it up in front of everyone. Absolutely humiliating.",
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
    ),

    // ── Issue #1039: Northfield Barber ────────────────────────────────────────

    FRESH_CUT(
        "Fresh Cut",
        "You had a haircut at Kosta's Barbers. Smart.",
        1
    ),
    UNDERCOVER_FADE(
        "Undercover Fade",
        "You got a fade and ducked police recognition. Smooth.",
        1
    ),
    NEW_MAN(
        "New Man",
        "Your new look fooled everyone. Even you.",
        1
    ),
    KOSTA_REGULAR(
        "Kosta's Regular",
        "You've visited Kosta's Barbers five times. He knows your order.",
        5
    ),
    BARBER_QUEUE_JUMPER(
        "Queue Jumper",
        "You jumped the waiting queue at the barber. They were not happy.",
        1
    ),
    FREE_FROM_KOSTA(
        "On the House",
        "Kosta gave you a free trim. Marchetti connections pay off.",
        1
    ),

    // ── Issue #1041: Northfield Argos ─────────────────────────────────────────

    PENCIL_THIEF(
        "Armed and Dangerous",
        "Stole five Argos pencils. You're basically a criminal mastermind.",
        5
    ),
    LAYBY_LIMBO(
        "Still Waiting",
        "Waited ten in-game days for an out-of-stock layby item. It's not coming.",
        1
    ),
    MARCHETTI_DELIVERY(
        "Special Delivery",
        "Collected item 9999 and delivered the Marchetti Package. No questions asked.",
        1
    ),
    ARGOS_CHAOS(
        "Computer Says No",
        "Survived a full SYSTEM_DOWN event. The whole queue was fuming.",
        1
    ),

    // ── Issue #1047: Northfield BP Petrol Station ──────────────────────────────

    FORECOURT_REGULAR(
        "Forecourt Regular",
        "You've bought from the petrol station 10 times. Dave knows your face.",
        10
    ),
    DRIVE_OFF(
        "Drive-Off",
        "Stole a full petrol can without being caught. Classic.",
        1
    ),
    SCRATCH_CARD_WINNER(
        "Jackpot!",
        "Won the 20 COIN scratch card jackpot. Luck of the forecourt.",
        1
    ),
    NIGHT_SHIFT_FRIEND(
        "Night Shift Friend",
        "Baz gave you a free energy drink at 3am. You're his only company.",
        1
    ),
    PASTY_REGRET(
        "That Pasty Has Been There Since Tuesday",
        "Suffered stomach pain from a forecourt pasty. You had it coming.",
        1
    ),

    // ── Issue #1051: Angel Nails & Beauty ─────────────────────────────────────

    TREAT_YOURSELF(
        "Treat Yourself",
        "Got your nails done for the first time. You deserve it.",
        1
    ),
    HIGH_MAINTENANCE(
        "High Maintenance",
        "Tried every service at Angel Nails & Beauty. You're practically a regular.",
        1
    ),
    GEL_ECONOMY(
        "Gel Economy",
        "Completed the Marchetti voucher scam on 3 separate Thursdays. Nails out, money in.",
        3
    ),
    SMUDGED(
        "Smudged",
        "Received the SMUDGED_NAILS debuff during a heatwave. Should've waited for it to dry.",
        1
    ),
    NAIL_BURGLAR(
        "Nail Burglar",
        "Broke into Angel Nails & Beauty after hours. Desperate times.",
        1
    ),

    // ── Issue #1053: Northfield Ladbrokes — BettingShopSystem ─────────────────

    FIRST_FLUTTER(
        "First Flutter",
        "Placed your first bet at the bookies. Derek didn't even blink.",
        1
    ),
    RACING_CERT(
        "Racing Cert",
        "Cashed out a winning bet of 10 COIN or more. Don't get used to it.",
        1
    ),
    FOBT_RAGE(
        "FOBT Rage",
        "Lost 10+ COIN on the Fixed-Odds Betting Terminal in one session. The machine won.",
        1
    ),
    SURE_THING(
        "Sure Thing",
        "Accepted a race fix from the Marchetti Crew and cashed out. Nice and easy.",
        1
    ),
    FOLDED(
        "Folded",
        "Broke into the bookies after hours. Derek's float tray wasn't going to rob itself.",
        1
    ),
    BETTING_SLIP_BLUES(
        "Betting Slip Blues",
        "Held three losing slips simultaneously. All three. You beautiful optimist.",
        1
    ),

    // ── Issue #1055: Northfield War Memorial — StatueSystem ────────────────

    STATUE_SNACK(
        "Statue Snack",
        "Attracted 8 pigeons to the war memorial simultaneously with BREAD_CRUMBS.",
        1
    ),
    COUNCIL_ESTATE(
        "Council Estate",
        "Bribed the council cleaner on 3 separate days. They've seen nuffink.",
        3
    ),
    COME_DOWN(
        "Come Down",
        "Toppled the war memorial statue. Northfield will never forget.",
        1
    ),
    LEST_WE_FORGET(
        "Lest We Forget",
        "Attended the full Remembrance Sunday silence without causing any disturbance.",
        1
    ),
    REMEMBER_REMEMBER(
        "Remember Remember",
        "Triggered a firework misfire that started a fire on Bonfire Night.",
        1
    ),
    PLACARD_PINCHER(
        "Placard Pincher",
        "Picked up a protestor's placard. Whose side are you on?",
        1
    ),

    // ── Issue #1061: Northfield Community Centre ───────────────────────────

    ONE_DAY_AT_A_TIME(
        "One Day at a Time",
        "Attended your first AA meeting at the community centre. You showed up.",
        1
    ),
    SATURDAY_BARGAIN_HUNTER(
        "Saturday Bargain Hunter",
        "Purchased 3 items from a single Bring & Buy Sale session. Happy shopping.",
        1
    ),

    // ── Issue #1069: Ice Cream Van System (Dave's Ices) ───────────────────────
    CHEEKY_FLAKE(
        "Cheeky Flake",
        "Bought a 99 Flake from Dave's van on a warm day. Worth every penny. Probably.",
        1
    ),
    DAVE_APPROVES(
        "Dave Approves",
        "Dave opened the main hatch for you without hesitation. High praise from a man with a van.",
        1
    ),
    SIDE_HATCH(
        "Side Hatch",
        "Dave opened the side hatch. You didn't ask where any of it came from.",
        1
    ),
    CHAV_CHARMED(
        "Chav Charmed",
        "You talked down the queue-jumping Chav without a single punch. Progress.",
        1
    ),
    VAN_HEIST(
        "Van Heist",
        "You nicked Dave's ice cream van from the depot. The jingle is now your problem.",
        1
    ),
    MARCHETTI_DEFENDER(
        "Marchetti Defender",
        "You protected Dave's van from the Marchetti drive-by. Dave owes you an ice cream. He will forget.",
        1
    ),
    KIDS_MOB_SURVIVED(
        "Kids Mob Survived",
        "The school run swarm hit the queue and you made it out with your 99 Flake intact. Respect.",
        1
    ),

    // ── Issue #1065: Fix My Phone ──────────────────────────────────────────────
    SIMSWAPPER(
        "SIM Swapper",
        "Completed your first IMEI wipe. Tariq said he didn't want to know. He definitely wants to know.",
        1
    ),
    PLANTED_IT(
        "Planted It",
        "Planted a cloned phone on an NPC for surveillance. Three hours of intel. Completely illegal.",
        1
    ),
    CRACKED_SCREEN(
        "Cracked Screen",
        "The POLICE_KNOCK event fired while you were holding a stolen phone. You moved faster than you thought.",
        1
    ),
    TARIQ_REGULAR(
        "Tariq's Regular",
        "Used Fix My Phone five times. Tariq has stopped pretending he doesn't know your name.",
        5
    ),
    BURNED(
        "Burned",
        "A cloned phone's surveillance burned out. Three rumours. Zero fingerprints.",
        1
    ),

    // ── Issue #1071: Northfield Fast Cash Finance ─────────────────────────
    IN_DEBT(
        "In Debt",
        "Took your first payday loan. The APR is 1,294%. Representative example.",
        1
    ),
    DEBT_SPIRAL(
        "Debt Spiral",
        "Missed two repayments on a single loan. Barry is not best pleased.",
        1
    ),
    BAILIFF_BRIBED(
        "Grease the Wheels",
        "Bribed the bailiff. Ten coins and a firm handshake. No paperwork.",
        1
    ),
    BAILIFF_ASSAULT(
        "Don't Shoot the Messenger",
        "Attacked the bailiff. Wanted Tier 2 and Barry will never forgive you.",
        1
    ),
    MARCHETTI_MONEY(
        "In Deeper",
        "Had your debt sold to the Marchetti Crew. This is fine. Everything is fine.",
        1
    ),
    HIGH_ROLLER_NOTICE(
        "Word Gets Around",
        "Barry heard you had a good day at the bookies and proactively offered a bigger loan.",
        1
    ),

    // ── Issue #1075: Khan's Off-Licence ───────────────────────────────────────

    CORNER_SHOP_REGULAR(
        "Loyalty Stamped",
        "20 lifetime loyalty stamps at Khan's Off-Licence.",
        20
    ),
    UNDERAGE_ENABLER(
        "Underage Enabler",
        "Handed alcohol to a school kid.",
        1
    ),
    BACK_DOOR_BOY(
        "Back Door Boy",
        "Used the after-hours back-door knock 3 times.",
        3
    ),
    MARCHETTI_ERRAND_BOY(
        "Marchetti Errand Boy",
        "Accepted 5 Marchetti envelopes from Imran.",
        5
    ),
    FIVE_FINGER_DISCOUNT(
        "Five Finger Discount",
        "Shoplifted from Khan's without being detected.",
        1
    ),

    // ── Issue #1079: Northfield Magistrates' Court ────────────────────────────

    FIRST_OFFENCE(
        "First Offence",
        "You stood in the dock for the first time. Sandra Pemberton looked at you like something she'd scraped off her shoe.",
        1
    ),
    NOT_GUILTY(
        "No Comment",
        "You walked out of court without a conviction. Martin Gale looked furious. Sandra looked unconvinced. You're free.",
        1
    ),
    COMMUNITY_SERVICE_HERO(
        "Community Spirit",
        "Completed ten community service shifts without skipping a single one. The parole board is mildly impressed.",
        10
    ),
    BENT_BRIEF(
        "Bent Brief",
        "Bribed Martin Gale to drop a charge. He pocketed it without blinking. Says everything, really.",
        1
    ),
    CONTEMPT_OF_COURT(
        "Contempt of Court",
        "You showed open disrespect in Sandra Pemberton's courtroom. She was not pleased. Nobody has ever been less pleased.",
        1
    ),
    CUSTODIAL(
        "Her Majesty's Guest",
        "Sentenced to a custodial term. Released 24 hours later with nothing but the clothes on your back and a bus fare.",
        1
    ),

    // ── Issue #1077: Northfield Chinese Takeaway — Golden Palace ─────────────

    LATE_NIGHT_REGULAR(
        "Last Orders at the Palace",
        "Ordered from Golden Palace between 23:00 and 00:00. Mr. Chen gave you a look.",
        1
    ),
    FORTUNE_SEEKER(
        "You Will Find Great Fortune",
        "Used ten fortune cookies. The fortunes were increasingly accurate. Unsettlingly so.",
        10
    ),
    PRAWN_CRACKER_PIGEON_FEEDER(
        "The Prawn Cracker Economy",
        "Fed prawn crackers to pigeons three times. The pigeons have formed a committee.",
        3
    ),
    CRISPY_DUCK_CONNOISSEUR(
        "Peking Duck, Northfield-Style",
        "Ordered Crispy Duck five times. Mr. Chen knows your order before you open your mouth.",
        5
    ),
    PHONE_CHAOS(
        "Wrong Number",
        "Answered the Golden Palace phone during the Phone Order Chaos event. You gave a fake address. Classic.",
        1
    ),

    // ── Issue #1081: Northfield Pet Shop & Vet — Paws 'n' Claws ─────────────

    DOG_OWNER(
        "Man's Best Friend",
        "Purchased a dog from Bev at Paws 'n' Claws. He's looking at you like you're the best person in the world. You're not.",
        1
    ),
    BEST_IN_SHOW(
        "Crufts, Eventually",
        "Brought a vaccinated dog with a vet record to the Boot Sale. Bev would be proud.",
        1
    ),
    VET_BILLS(
        "Worth Every Penny",
        "Spent 30 coins or more at Northfield Vets. Dr. Patel has a new conservatory.",
        1
    ),
    DODGY_BREEDER(
        "Pedigree Chums",
        "Completed Bev's Marchetti pedigree theft mission. The less said the better.",
        1
    ),
    DOGNAPPED(
        "No Questions Asked",
        "Captured an ambient dog as your companion without buying it. It seemed to like you. Still theft though.",
        1
    ),

    // ── Issue #1091: Northfield Nando's ─────────────────────────────────────

    NANDOS_REGULAR(
        "Half Chicken, No Chips",
        "Eaten at Nando's five times. Kezia knows your order. She's judging you. Gently.",
        5
    ),
    EXTRA_HOT_REGRET(
        "Why Did I Do That",
        "Suffered the NANDOS_REGRET debuff. You had five minutes to find a toilet. You did not.",
        1
    ),
    CHICKEN_THIEF(
        "The Safe Was Just Sitting There",
        "Looted the Nando's manager's office safe. Dave called the police. Worth it.",
        1
    ),
    LADS_LADS_LADS(
        "On the Lash",
        "Accepted a stag do invitation at Nando's. Free entry to The Vaults. The lads respect you now.",
        1
    ),

    // ── Issue #1094: Northfield By-Election ──────────────────────────────────

    CAMPAIGNER(
        "Knocking on Doors",
        "Delivered campaign leaflets to 10 residential doors. Local democracy in action. Sort of.",
        10
    ),
    POLITICAL_SMOKER(
        "Roll-Up Manifesto",
        "Crafted a Rollie from a campaign leaflet and tobacco. Technically a form of political commentary.",
        1
    ),
    POSTER_BOY(
        "No Platform",
        "Tore down 3 election posters. The returning officer is not impressed. Neither is the Daily Ragamuffin.",
        3
    ),
    PEOPLES_CHAMPION(
        "The People's Choice",
        "Won the Northfield Ward by-election as an Independent candidate. Cllr. You. It's laminated and everything.",
        1
    ),
    DEMOCRACY_THIEF(
        "Carried Away With It",
        "Stole the ballot box on polling day. The election was voided. You are not a hero of the people.",
        1
    ),

    // ── Issue #1096: Sunday League Football ──────────────────────────────────

    SUNDAY_LEAGUE(
        "Lace 'Em Up",
        "Substituted into the Sunday League match and played to full-time. Rovers needed you. Or maybe they didn't.",
        1
    ),
    REF_ABUSE(
        "Are You Blind, Mate?",
        "Shouted abuse at the referee twice in one match. He wasn't impressed. You were ejected. Worth it.",
        1
    ),
    DODGY_PIE(
        "Guaranteed to Put Someone Off",
        "Slipped a Dodgy Pie to an opposition player. Council FC's striker spent half-time in the bushes.",
        1
    ),
    FOOTBALL_PUNTER(
        "Two to One. Easy Money.",
        "Won a pitch-side bet at the Northfield Sunday League. The bookie looked gutted.",
        1
    ),
    DIRTY_TACKLE(
        "Studs Up",
        "Fouled an opposition player without the referee noticing. You're a menace. The touchline loved it.",
        1
    ),

    // ── Issue #1098: Northfield Summer Fete ──────────────────────────────────

    FETE_CHAMPION(
        "Fete Champion",
        "Won the tombola, raffle, and Hook-a-Duck in a single fete. Margaret was delighted. You were smug.",
        1
    ),
    CAKE_THIEF(
        "No Shame",
        "Stole a cake from the fete stall. It wasn't even a good cake. The vicar saw everything.",
        1
    ),
    RIGGED(
        "Fix the Draw",
        "Successfully rigged the raffle barrel at the summer fete. You guaranteed your own win. Shameful.",
        1
    ),
    BRITISH_INSTITUTION(
        "British Institution",
        "Attended 3 annual summer fetes. You've eaten the scones, spun the tombola, lost on the raffle. Bliss.",
        3
    ),

    // ── Issue #1100: Northfield Council Flats — Kendrick House ───────────────

    LIFT_ENGINEER(
        "Going Up? Eventually.",
        "Fixed the broken lift at Kendrick House using Scrap Metal and Wire. Derek from the Council is taking the credit.",
        1
    ),
    NOSY_NEIGHBOUR(
        "Net Curtain Intelligence",
        "Gossiped with 5 different residents on floors 2–4 of Kendrick House. You know everyone's business now.",
        5
    ),
    INSPECTION_PASSED(
        "Passes Inspection",
        "Derek completed his housing inspection and found nothing. Your flat is the cleanest crime scene in Northfield.",
        1
    ),
    PARCEL_PIRATE(
        "Someone's Amazon Order",
        "Stolen 5 parcels from the communal letterbox bank at Kendrick House. You've got a cheek.",
        5
    ),

    // ── Issue #1102: Northfield Indoor Market ─────────────────────────────────

    MARKET_REGULAR(
        "Regular at the Market",
        "Completed 5 stall rentals at the Indoor Market, selling at least one item each time. Ray's starting to know your face.",
        5
    ),
    SOVEREIGN_TRADING(
        "Sovereign Trading",
        "Sold a Counterfeit Watch at the Indoor Market for 5 COIN or more. Lovely bit of bling.",
        1
    ),
    LEGS_IT(
        "Legs It",
        "Escaped a Trading Standards raid with contraband still in your stall inventory. Had it on your toes.",
        1
    ),
    CROWD_WORKER(
        "Crowd Worker",
        "Successfully pickpocketed 5 Market Punters in a single market day. The crowd's your cover.",
        5
    ),
    SATURDAY_MARKET_KING(
        "Saturday Market King",
        "Earned 20 COIN or more from stall sales in a single Saturday market day. The king of Northfield.",
        1
    ),

    // ── Issue #1104: Northfield Community Centre ──────────────────────────────

    BOXING_CHAMP(
        "Float Like a Butterfly",
        "Won 5 sparring sessions at Ray's Boxing Club. The Northfield Amateur Champion. Sort of.",
        5
    ),
    COUNCIL_MOLE(
        "Minutes Away",
        "Successfully eavesdropped on the Council Budget Meeting 3 times. You know where the money isn't going.",
        3
    ),
    JUMBLE_SALE_KING(
        "One Man's Junk",
        "Bought 10 items from the Jumble Sale. Northfield's premier bargain hunter.",
        10
    ),
    CAKE_SABOTEUR(
        "Special Ingredient",
        "Slipped something extra into a Bake-Off competitor's cake without anyone noticing. That's not flour.",
        1
    ),
    BAKE_OFF_CHEAT(
        "Bought-In Bake",
        "Won the Northfield Cake Bake-Off using a shop-bought item. Technically still a winner.",
        1
    ),

    // ── Issue #1110: Skin Deep Tattoos ────────────────────────────────────────

    /** Get 3 tattoos in a single session at Skin Deep Tattoos. */
    LIVING_CANVAS(
        "Living Canvas",
        "Got 3 tattoos in a single session. Kev's done more work on you than your nan ever did.",
        3
    ),

    /** Apply a DIY prison tattoo kit successfully. */
    HARD_AS_NAILS(
        "Hard as Nails",
        "Applied a prison tattoo kit yourself. Needle, ink, and a complete disregard for hygiene.",
        1
    ),

    /** Pay for tattoo removal within 24 in-game hours of getting a HEAVILY_TATTOOED buff. */
    TATTOO_REGRET(
        "What Was I Thinking",
        "Paid to have a tattoo removed within 24 hours. At least you made it quick.",
        1
    ),

    /** Tip off Kev about Spider the rival tattooist. */
    GRASSROOTS_INFORMANT(
        "Eyes on the Street",
        "Told Kev about Spider's cut-price operation. The neighbourhood watches out for its own.",
        1
    ),

    /** Have HEAVILY_TATTOOED buff active while entering the JobCentre. */
    WALKING_ARTFORM(
        "Walking Artform",
        "Turned up to sign-on absolutely covered in ink. The case worker had opinions.",
        1
    ),

    // ── Issue #1116: Northfield Pharmacy — Day & Night Chemist ───────────────

    /** Successfully obtain STRONG_MEDS with a BLANK_PRESCRIPTION_FORM. */
    FORGED_IT(
        "Doctor's Orders",
        "Used a blank prescription form and walked out with the strong stuff. Janet didn't even blink.",
        1
    ),

    /** Pocket an item from a PHARMACY_SHELF_PROP undetected. */
    FIVE_FINGER_PHARMACY(
        "Five Finger Pharmacy",
        "Helped yourself to something off the shelf. Janet was busy. The shelf was not.",
        1
    ),

    /** Buy every OTC product at the pharmacy counter at least once. */
    MEDICINE_CABINET(
        "Medicine Cabinet",
        "Purchased every item Janet sells over the counter. You are very committed to your health.",
        6
    ),

    /** Redeem a PRESCRIPTION on its last valid in-game day. */
    JUST_IN_TIME(
        "Just In Time",
        "Handed in your prescription on the very last day it was valid. Cutting it fine.",
        1
    ),

    /** Take PARACETAMOL a third time within the 6-hour window. */
    OVERDONE_IT(
        "Overdone It",
        "Three paracetamol in six hours. Janet would be appalled. Your stomach agrees with her.",
        1
    ),

    // ── Issue #1122: Sun Kissed Studio ────────────────────────────────────────

    /** Use a sunbed at Sun Kissed Studio and leave with the TANNED buff active. */
    BRONZED(
        "Bronzed",
        "Walked out of Sun Kissed Studio looking like you've been to Marbella. You haven't.",
        1
    ),

    /** Use a sunbed 5 times (accumulate 5 sessions at Sun Kissed Studio). */
    SUN_KISSED(
        "Sun Kissed",
        "Five sessions under the UV tubes. Tracey says you're her best customer. Worrying.",
        5
    ),

    /** Deliver a Marchetti cash drop to Tracey at Sun Kissed Studio. */
    CLEAN_MONEY(
        "Clean Money",
        "Walked a brown envelope through the front door of a tanning salon. Perfectly normal.",
        1
    ),

    /** Deliver the MARCHETTI_LEDGER to the police station. */
    LAUNDERED(
        "Laundered",
        "Handed over the ledger. The Marchetti Crew will have opinions about this.",
        1
    ),

    /** Purchase Special Services at Sun Kissed Studio (Street Rep ≥ 40). */
    SPECIAL_APPOINTMENT(
        "Special Appointment",
        "Booked in for the full treatment. The kind that doesn't go on the price board.",
        1
    ),

    // ── Issue #1130: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Siphon fuel from a parked car between 21:00 and 06:00.
     */
    MIDNIGHT_MECHANIC(
        "Midnight Mechanic",
        "Siphoned petrol from a parked car in the dead of night. Wayne didn't see a thing. Probably.",
        1
    ),

    /**
     * Rob the BP till with a CROWBAR.
     */
    HOLD_UP(
        "This Is a Robbery",
        "Used a crowbar on the BP till. Raj would be disappointed. Wayne was asleep.",
        1
    ),

    /**
     * Throw a MOLOTOV_COCKTAIL crafted from a PETROL_CAN_FULL + FLYER.
     */
    MOLOTOV_MOMENT(
        "Molotov Moment",
        "Threw a homemade incendiary. It went well, by which we mean it went very badly for everyone.",
        1
    ),

    /**
     * Eat a MICROWAVE_PASTY from the BP kiosk after 21:00.
     */
    MICROWAVE_MILLIONAIRE(
        "Midnight Feast",
        "Ate a forecourt pasty after 21:00. Your stomach lodged a formal complaint.",
        1
    ),

    /**
     * Fill a PETROL_CAN at the BP pump 5 times legitimately.
     */
    PETROL_HEAD(
        "Petrol Head",
        "Filled a petrol can at the BP forecourt five times. Raj knows your face by now.",
        5
    ),

    // ── Issue #1132: Northfield Dog Grooming Parlour — Pawfect Cuts ───────────

    /**
     * Win the Northfield Dog Show legitimately (Bond ≥ 70 + GUARD trick + fresh groom).
     * No bribery. No conspiracy. Just a well-groomed dog and a proud moment.
     */
    LEGITIMATE_CHAMPION(
        "Legitimate Champion",
        "Won the Northfield Dog Show the honest way. Bond ≥ 70, GUARD trick, fresh groom. Tracey cried.",
        1
    ),

    /**
     * Bribe the JUDGE_NPC at the Northfield Dog Show (SHOW_RIGGING, +5 Notoriety).
     * The Crufts Conspiracy quest: Winston's secret revealed.
     */
    BENT_JUDGE(
        "Fixed It",
        "Slipped the judge a few quid at the dog show. Winston's owner looked furious. Good.",
        1
    ),

    /**
     * Expose the Crufts Conspiracy via NewspaperSystem headline (−15 Marchetti respect).
     */
    WHISTLEBLOWER(
        "Whistleblower",
        "Exposed the dog show rigging to The Daily Ragamuffin. The Marchetti Crew are not pleased.",
        1
    ),

    /**
     * Dog develops FLEA_INFESTATION debuff after 7 in-game days without grooming.
     */
    FLEA_MARKET(
        "Flea Market",
        "Your dog's developed fleas. The bed's infested. Tracey at Pawfect Cuts will be delighted.",
        1
    ),

    /**
     * Cure the FLEA_INFESTATION debuff using FLEA_POWDER.
     */
    FLEA_REMEDY(
        "Sorted It",
        "Treated the fleas. The powder smells terrible. The dog seems indifferent.",
        1
    ),

    /**
     * Purchase all four grooming services (Basic Wash, Full Groom, Medicated Bath, Nail Clipping).
     */
    PAMPERED_POOCH(
        "Pampered Pooch",
        "Used all four grooming services at Pawfect Cuts. That dog lives better than you do.",
        1
    ),

    /**
     * Buy an UNLICENSED_DOG from the DOG_DEALER and sell it to a DOG_OWNER for profit.
     */
    DOG_FLIPPER(
        "Dog Flipper",
        "Bought an unlicensed dog, sold it on for profit. No paperwork. Just vibes and a lead.",
        1
    ),

    /**
     * Attend the Northfield Dog Show (any placement).
     */
    SHOW_DAY(
        "Show Day",
        "Entered the Northfield Dog Show. The other dogs were impeccably groomed. Yours had a go.",
        1
    ),

    // ── Issue #1134: Patel's Newsagent ────────────────────────────────────────

    /**
     * Awarded when the player wins the scratch-card jackpot (50 COIN) at Patel's News.
     */
    NEWSAGENT_JACKPOT(
        "Rajjackpot",
        "Won the 50 COIN scratch card jackpot at Patel's. Raj looked quietly devastated.",
        1
    ),

    /**
     * Awarded when the player buys 5 or more scratch cards in a single in-game day,
     * gaining the SCRATCH_CARD_ADDICTION debuff.
     */
    SCRATCH_CARD_ADDICTION(
        "One More Can't Hurt",
        "Bought five scratch cards in a single day. Raj said nothing. He didn't have to.",
        1
    ),

    /**
     * Awarded after completing 7 paper rounds before 07:00 on 7 separate days.
     */
    RELIABLE_PAPERBOY(
        "Reliable Paperboy",
        "Completed seven paper rounds on time. People actually expect you now. Terrifying.",
        7
    ),

    /**
     * Awarded when the player wins the weekly lottery draw (1-in-50 chance; 25 COIN).
     */
    LOTTERY_WINNER(
        "It Could Be You",
        "Won the newsagent lottery draw. Twenty-five quid. Raj was unreasonably happy for you.",
        1
    ),

    /**
     * Awarded when Raj gives the player a free chocolate bar after 10+ purchases —
     * the mark of a truly loyal customer.
     */
    RAJS_FAVOURITE(
        "Raj's Favourite",
        "Raj slid you a free Dairy Milk across the counter. You're basically family now.",
        1
    ),

    /**
     * Awarded when the player buys or shoplifts a DODGY_MAGAZINE from the top shelf.
     */
    TOP_SHELF(
        "Top Shelf",
        "Acquired a dodgy magazine from behind the counter. Raj's expression was unreadable.",
        1
    ),

    /**
     * Awarded when the player places a crafted PAPIER_MACHE_BRICK in the world.
     */
    PAPIER_MACHE_ARCHITECT(
        "Papier Mache Architect",
        "Placed a papier mache brick. It looks convincing. It isn't.",
        1
    ),

    /**
     * Awarded on first completed paper round (player signs on and delivers 8 papers before 07:00).
     */
    EARLY_BIRD_PAPERBOY(
        "Early Bird",
        "Completed a paper round before 07:00. You saw the milk float. Norman waved.",
        1
    ),

    // ── Issue #1200: Patel's News (NewsagentSystem) ────────────────────────────

    /**
     * Issue #1200: Awarded when the player completes their first paper round on time
     * (all 10 newspapers delivered to LETTERBOX_PROP targets before 07:00).
     */
    PAPER_ROUND_DONE(
        "Morning Rounds",
        "Delivered all ten papers before seven. Patel nodded. High praise.",
        1
    ),

    /**
     * Issue #1200: Awarded when the player buys or shoplifts a DODGY_MAGAZINE
     * from behind the counter at Patel's News.
     */
    DODGY_MAG_BUYER(
        "Reading Material",
        "Acquired a dodgy magazine. It's for the articles. Obviously.",
        1
    ),

    /**
     * Issue #1200: Awarded when the player receives a BANNED_FROM_PATEL flag
     * (shoplifting detected, bundle theft, or repeated criminal activity in the shop).
     */
    BANNED_FROM_PATEL(
        "Persona Non Patel",
        "You've been banned from Patel's. Even Raj looks disappointed.",
        1
    ),

    /**
     * Issue #1200: Awarded when the player uses NEWSAGENT_KEY to break into Patel's
     * back office at night and raids the CASH_BOX_PROP.
     */
    NEWSAGENT_BURGLAR(
        "After Hours",
        "Raided Patel's cash box in the dead of night. Raj suspects nothing. Probably.",
        1
    ),

    // ── Issue #1136: The Vaults Nightclub ─────────────────────────────────────

    /**
     * Awarded when the player enters The Vaults nightclub for the first time.
     */
    FIRST_TIME_IN(
        "Welcome to The Vaults",
        "First time inside. The floor is sticky. The music is loud. You feel alive.",
        1
    ),

    /**
     * Awarded when the player successfully bribes Big Dave to skip the queue.
     */
    BOUNCER_BRIBED(
        "Slipped Him a Fiver",
        "Bribed the bouncer. Big Dave pocketed it without blinking. Respect.",
        1
    ),

    /**
     * Awarded when the player wins an MCBattle on the nightclub dancefloor.
     */
    DANCEFLOOR_MC(
        "Dancefloor MC",
        "Won a freestyle battle at The Vaults. The crowd went absolutely mental.",
        1
    ),

    /**
     * Awarded when the player successfully pickpockets a DRUNK NPC inside the club.
     */
    NIGHTCLUB_PICKPOCKET(
        "Light Fingers",
        "Lifted a wallet from a DRUNK punter. Dark venue, heavy bass. They never knew.",
        1
    ),

    /**
     * Awarded when the player is ejected from The Vaults at 03:00 on 3 separate nights.
     */
    CLOSING_TIME(
        "Closing Time",
        "Ejected at 03:00 three nights running. The bouncer knows your face now.",
        3
    ),

    /**
     * Awarded when the player gains entry to the VIP area of The Vaults.
     */
    VIP_ACCESS(
        "VIP Treatment",
        "Made it into the VIP area. The seats are slightly less sticky. Slightly.",
        1
    ),

    /**
     * Awarded when the player completes a full night (22:00–03:00) in the club
     * without buying any alcohol.
     */
    SOBER_IN_THE_VAULTS(
        "Designated Driver",
        "Spent a whole night in The Vaults completely sober. You saw everything.",
        1
    ),

    // ── Issue #1138: Northfield Iceland ───────────────────────────────────────

    /**
     * Awarded when the player successfully exploits the three-for-a-fiver deal
     * at the Iceland checkout for the first time.
     */
    THREE_FOR_A_FIVER(
        "Life of Riley",
        "Three items for a fiver. The apex of British consumer civilisation.",
        1
    ),

    /**
     * Awarded when the player successfully uses a PRAWN_RING to distract Kevin
     * (ICELAND_SECURITY) long enough to complete a self-checkout scam undetected.
     */
    PRAWN_RING_BAIT(
        "Crustacean Misdirection",
        "A prawn ring, correctly deployed, is a devastating tactical weapon.",
        1
    ),

    /**
     * Awarded when the player is caught by Kevin attempting the self-checkout scam
     * (unexpected item in the bagging area).
     */
    UNEXPECTED_ITEM(
        "Unexpected Item",
        "In the bagging area. You are the unexpected item. Aren't we all.",
        1
    ),

    /**
     * Awarded when the player steals all 6 FROZEN_TURKEY items from the Iceland
     * stockroom during Dec 1–24. Triggers a NewspaperSystem front page.
     */
    GREAT_TURKEY_HEIST(
        "Great Turkey Heist",
        "Six frozen turkeys. One stockroom. No regrets. Merry Christmas, Northfield.",
        6
    ),

    /**
     * Awarded when the player steals the Iceland Christmas Club Cash Box, depriving
     * multiple customers of their year-long savings. Seeds a LOCAL_SCANDAL rumour.
     */
    CHRISTMAS_CLUB_VILLAIN(
        "Bah Humbug",
        "They saved all year for this. You took it in thirty seconds. You monster.",
        1
    ),

    /**
     * Awarded when the player honestly manages a customer's Christmas Club envelope
     * (returns it without stealing). Raises LOCAL_HERO faction standing.
     */
    CHRISTMAS_CLUB_HONEST(
        "Community Spirit",
        "You handed it back. The whole envelope. All of it. People do this.",
        1
    ),

    /**
     * Awarded when the player successfully passes a FAKE_RECEIPT at the Iceland
     * self-checkout without Kevin noticing.
     */
    RECEIPT_ARTIST(
        "Receipt Artist",
        "The pen is mightier than the till. Marginally.",
        1
    ),

    /**
     * Awarded when the player fences all 6 stolen FROZEN_TURKEY items to the
     * FenceSystem or KebabVanSystem in a single session.
     */
    TURKEY_DISTRIBUTOR(
        "Cold Chain Logistics",
        "Six turkeys, appropriately redistributed. Supply chain management at its finest.",
        1
    ),

    // ── Issue #1142: Northfield RAOB Lodge ────────────────────────────────────

    /**
     * Awarded when the player completes the RAOB initiation ceremony and becomes an INITIATE.
     * Requires 2 SPONSORSHIP_FORMs + 5 COIN + ceremony at LODGE_ALTAR_PROP.
     */
    BOTHER_BUFFALO(
        "Bother Buffalo",
        "You took the oath. You wore the apron. You drank the Worthington's. Welcome, Brother.",
        1
    ),

    /**
     * Awarded when the player successfully calls in all four Lodge Member favours
     * (Brian's housing letter, Sandra's dismissal form, Reg's planning permission,
     * Terry's betting multiplier) in a single Lodge membership.
     */
    OLD_BOYS_NETWORK(
        "Old Boys' Network",
        "Brian sorted the flat. Sandra wiped the fine. Reg stamped the permission. Terry fixed the odds. " +
            "You didn't even buy a round.",
        4
    ),

    /**
     * Awarded when the player successfully loots the LODGE_SAFE_PROP
     * (obtains LODGE_CHARTER_DOCUMENT + REGALIA_SET + COIN).
     */
    SAFE_CRACKER(
        "Combination Plumber",
        "The safe wasn't as secure as Norman thought. In fairness, neither was the Lodge.",
        1
    ),

    /**
     * Awarded when the player successfully blackmails a RAOB_MEMBER using the LODGE_CHARTER_DOCUMENT.
     */
    GRUBBY_LEVERAGE(
        "Grubby Leverage",
        "It wasn't what they knew. It was what you knew. " +
            "And what you knew was in a document that shouldn't exist.",
        1
    ),

    /**
     * Awarded when the player wears the REGALIA_SET (activates DisguiseSystem tier).
     */
    FULL_REGALIA(
        "Full Regalia",
        "Apron, collar, jewel, and sash. You look completely ridiculous. " +
            "Somehow, no one questions you.",
        1
    ),

    /**
     * Awarded when the player reaches PRIMO membership tier (three favours + 20 COIN donation).
     */
    GRAND_PRIMO(
        "Grand Primo",
        "Three favours, twenty coin, and the trust of men who probably shouldn't trust anyone. " +
            "You are, regrettably, a Primo.",
        1
    ),

    /**
     * Awarded when the player uses the CEREMONIAL_MALLET as a weapon in combat
     * (deals 1.5× damage to an NPC).
     */
    CEREMONIAL_VIOLENCE(
        "For Ceremonial Use Only",
        "The inscription said 'ceremonial use only'. " +
            "This was, in a sense, a ceremony.",
        1
    ),

    // ── Issue #1146: Mick's MOT & Tyre Centre ────────────────────────────────

    /**
     * Awarded when the player successfully rings a car at Mick's garage
     * (clears the stolen flag using BLANK_LOGBOOK + 25 COIN).
     */
    RINGER(
        "Ringer",
        "New plates, new log book, new you. The DVLA remains unconvinced.",
        1
    ),

    /**
     * Awarded when the player obtains a Dodgy MOT certificate at MOT_RAMP_PROP.
     */
    DODGY_MOT(
        "Technically Roadworthy",
        "The certificate says PASS. The car says otherwise. Terry has been bribed.",
        1
    ),

    /**
     * Awarded when the player drives a Cut-and-Shut car that subsequently suffers
     * engine failure (20% risk per journey after the fusion).
     */
    DEATH_TRAP(
        "She'll Be Fine",
        "You drove a cut-and-shut. It wasn't fine. You knew this going in.",
        1
    ),

    /**
     * Awarded when the player steals the cash tin from Mick's garage office.
     */
    GARAGE_THIEF(
        "Till Thief",
        "Nicked Mick's petty cash. He had it coming. Probably.",
        1
    ),

    /**
     * Awarded when the player chops a car for parts in Bay 2 (first chop).
     */
    CHOP_SHOP(
        "Parts Is Parts",
        "Reduced a perfectly good car to components. Allegedly. Parts are parts.",
        1
    ),

    /**
     * Awarded when the player drives a car with roadworthiness ≥ 90 and no
     * stolen flag for a full in-game week without being plate-checked.
     */
    CLEAN_DRIVER(
        "Clean Sheet",
        "A whole week. Legal car. Valid MOT. Not even a parking ticket. Who are you?",
        1
    ),

    // ── Issue #1148: Northfield Council Estate Lock-Up Garages ───────────────

    /**
     * Awarded when the player successfully picks the lock on any council garage
     * (LOCKPICK method, 70% success, silent).
     */
    LOCKSMITH(
        "Locksmith",
        "In and out. Silent. Professional. Dave definitely didn't see a thing.",
        1
    ),

    /**
     * Awarded when the player breaks into a council garage with a CROWBAR
     * (100% success, HIGH noise, 25-block radius).
     */
    CROWBAR_JUSTICE(
        "Crowbar Justice",
        "Subtle as a brick. Everyone on the estate heard that. Everyone.",
        1
    ),

    /**
     * Awarded when the UNDERCOVER_POLICE raid completes after the player's tip-off
     * (stash cleared, MARCHETTI_CREW Respect -10).
     */
    INFORMANT(
        "Informant",
        "The raid happened. The stash is gone. Sleep with one eye open.",
        1
    ),

    /**
     * Awarded when the player clears the hoarder's garage (Garage 2) and fences
     * at least 10 items of BRIC_A_BRAC from the clearance quest.
     */
    BRIC_A_BRAC_BANDIT(
        "Bric-a-Brac Bandit",
        "You've emptied someone's life into a holdall and called it a favour.",
        1
    ),

    /**
     * Awarded when the player loots the stolen goods stash in Garage 5
     * (HEAVY_PADLOCK secured; requires BOLT_CUTTERS or 2x CROWBAR attempts).
     */
    STASH_ROBBER(
        "Stash Robber",
        "Someone worked hard to nick all that. You worked harder to nick it from them.",
        1
    ),

    /**
     * Awarded when the player watches 3 rehearsals, takes the doorman role,
     * and joins the band at MC_BATTLE rank >= 2.
     */
    GARAGE_BAND_MEMBER(
        "Garage Band Member",
        "Three rehearsals, one handshake, and you're in. Gigs pay 6 COIN. Don't be late.",
        1
    ),

    /**
     * Awarded when the player rents Garage 7 from Dave the Caretaker and
     * successfully pays for 3 consecutive weeks without eviction.
     */
    LOCK_UP_LANDLORD(
        "Lock-Up Landlord",
        "5 COIN a week. It's not much. But it's yours. For now.",
        1
    ),

    // ── Issue #1151: Northfield Sporting & Social Club ────────────────────────

    /**
     * Awarded when the player beats Brian the resident darts pro in a 501 match.
     * Requires full 501 game completed at DARTBOARD_PROP with a double-out finish.
     */
    TREBLE_TOP(
        "Treble Top",
        "Beat Brian at darts. He's been club champion for eleven years. Not any more.",
        1
    ),

    /**
     * Awarded when the player wins Thursday Quiz Night with the highest score.
     * Must enter (1 COIN), complete all 8 rounds, and finish top of the leaderboard.
     */
    PONTOON_KING(
        "Pontoon King",
        "Cleaned out three Marchetti boys at the back-room card table. Diplomatically unwise. Financially excellent.",
        1
    ),

    /**
     * Awarded when the player steals the protection envelope before Tommy's enforcer arrives.
     * Must grab the envelope from PROTECTION_ENVELOPE_PROP during the 19:55–20:00 window.
     */
    ENVELOPE_THIEF(
        "Envelope Thief",
        "Nicked Tommy Marchetti's protection money. The consequences will be significant.",
        1
    ),

    /**
     * Awarded when the player pays Ron's debt (requires STREET_LADS Respect >= 50)
     * and buys a round for the whole club. A genuine pillar of the community. Almost.
     */
    SOCIAL_PILLAR(
        "Social Pillar",
        "Paid Ron's debt and bought the house a round. You're basically a local celebrity now.",
        1
    ),

    /**
     * Awarded when the player grasses Tommy Marchetti to the police station,
     * triggering the sting operation that gets him arrested.
     */
    GRASSED_UP(
        "Grassed Up",
        "Tipped off the police about Tommy's collection. 40% chance they find out it was you. Good luck.",
        1
    ),

    // ── Issue #1153: Northfield Community Centre ──────────────────────────────

    /**
     * Awarded when the player completes an aerobics session scoring 6+ out of 8 prompts.
     */
    STEP_TOGETHER(
        "Step Together",
        "You kept up with Sandra. Well, mostly. She was too polite to say anything.",
        1
    ),

    /**
     * Awarded when the player successfully posts a FORGED_GRANT_APPLICATION and collects the GRANT_CHEQUE.
     */
    GRANT_GRABBER(
        "Grant Grabber",
        "Thirty coin from the council. For a community pool table. That doesn't exist. Yet.",
        1
    ),

    /**
     * Awarded when the player steals the BISCUIT_TIN_PROP contents during a session.
     */
    BISCUIT_BANDIT(
        "Biscuit Bandit",
        "You nicked the Rich Tea. During a bereavement support group. Shameless.",
        1
    ),

    /**
     * Awarded when the player completes the legitimate grant application with Denise's help.
     */
    HONEST_CITIZEN(
        "Honest Citizen",
        "You did it properly. Filled in the forms. Waited three weeks. Denise is genuinely chuffed.",
        1
    ),

    /**
     * Awarded when the player shares their story at the Thursday NA meeting (first time).
     */
    ANONYMOUS(
        "Anonymous",
        "You shared. They listened. Nobody said a word afterwards. That's how it's supposed to work.",
        1
    ),

    /**
     * Awarded when the player attends 8 aerobics sessions (completing the full 8-week block).
     */
    CLEAN_EIGHT(
        "Clean Eight",
        "Eight sessions without missing one. Sandra saved you a spot at the front.",
        8
    ),

    // ── Issue #1159: Northfield Angel Nails & Beauty ──────────────────────────

    /**
     * Awarded after 5 visits to Angel Nails & Beauty.
     */
    SALON_REGULAR(
        "Regular",
        "You've been in that chair five times. Trang knows your name and your drama.",
        5
    ),

    /**
     * Awarded when the player gets a full set of acrylics.
     */
    FULL_SET(
        "Full Set",
        "Eight COIN and two hours later, you've got talons. Respect.",
        1
    ),

    /**
     * Awarded after seeding 10 SALON_GOSSIP rumours via the waiting bench exchange.
     */
    GOSSIP_QUEEN(
        "Gossip Queen",
        "Ten gossip exchanges at the nail salon. You know everything about everyone.",
        10
    ),

    /**
     * Awarded for stealing nail polish from the COLOUR_WALL_PROP without being caught.
     */
    FIVE_FINGER_DISCOUNT_DELUXE(
        "Five-Finger Discount (Deluxe)",
        "Nicked a bottle of Gel Polish 214 from right under Kim's nose. Bold.",
        1
    ),

    /**
     * Awarded for receiving Marchetti faction intel from Stacey during a WAG Saturday.
     */
    STACEY_INTEL(
        "Overheard at the Nail Bar",
        "Stacey Marchetti said something she probably shouldn't have. You were listening.",
        1
    ),

    // ── Issue #1161: Northfield Poundstretcher ────────────────────────────────

    /**
     * Awarded when the player is banned from Poundstretcher (caught shoplifting).
     */
    EVERY_POUNDS_A_PRISONER(
        "Every Pound's a Prisoner",
        "Banned from a pound shop. Sharon's seen things.",
        1
    ),

    /**
     * Awarded for successfully shoplifting from Poundstretcher without being caught.
     */
    POUNDSTRETCHER_FIVE_FINGER(
        "Five-Finger Discount (Poundstretcher)",
        "Nicked something from the shelf and walked right out. Sharon didn't even blink.",
        1
    ),

    /**
     * Awarded when Sharon catches the player shoplifting (she had line of sight).
     */
    SHARON_KNOWS(
        "Sharon Knows",
        "Sharon had eyes on you. Always did.",
        1
    ),

    /**
     * Awarded for completing the bulk-buy deal (5 COIN for 6 items).
     */
    BULK_BUYER(
        "Bulk Buyer",
        "Five coin for six items. Sharon's contractual obligation. You took it.",
        1
    ),

    /**
     * Awarded for using a Street Lad proxy while banned from Poundstretcher.
     */
    MIDDLE_MAN(
        "Middle Man",
        "Banned but not beaten. You found a workaround.",
        1
    ),

    /**
     * Awarded for looting the delivery pallet before it's brought inside.
     */
    PALLET_PIRATE(
        "Pallet Pirate",
        "Two minutes. One pallet. You were in and out before the driver finished his fag.",
        1
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1163: NHS Dentist achievements
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Awarded after waiting the full 6-in-game-day NHS wait without forging or going private.
     */
    SIX_MONTH_WAIT(
        "Six-Month Wait",
        "You waited the full NHS stretch. Dignity intact. Sort of.",
        1
    ),

    /**
     * Awarded for successfully presenting a FORGED_WAITING_LIST_LETTER to halve your wait.
     */
    QUEUE_JUMPER_DENTAL(
        "Queue Jumper",
        "Forged the waiting-list letter. Deborah didn't notice. This time.",
        1
    ),

    /**
     * Awarded for paying Mirek for dental treatment (any outcome).
     */
    BUDGET_MOLAR(
        "Budget Molar",
        "Five quid. A car seat. Mirek's steady hands. What could go wrong.",
        1
    ),

    /**
     * Awarded when Mirek's treatment makes your toothache worse.
     */
    BOTCHED_JOB(
        "Botched Job",
        "Mirek said \"Is fine. Normal.\" It was neither.",
        1
    ),

    /**
     * Awarded when toothachePoints reach SEVERE_TOOTHACHE level (≥ 85).
     */
    SWEET_TOOTH_CONSEQUENCE(
        "Sweet Tooth Consequence",
        "All those Haribos and Fanta cans. Your dentist tried to warn you. You didn't have one.",
        1
    ),

    /**
     * Awarded for paying 15 COIN for the private next-day slot.
     */
    PRIVATE_PATIENT(
        "Private Patient",
        "Fifteen coin for a next-day slot. At least the magazines are newer.",
        1
    ),

    // ── Issue #1165: Northfield Match Day ────────────────────────────────────
    /**
     * Awarded for successfully pickpocketing 3 fans in one match day.
     */
    MATCH_DAY_PICKPOCKET(
        "Dip in the Derby",
        "Three wallets lifted before the final whistle. Beautiful.",
        3
    ),

    /**
     * Awarded for selling 6 KNOCKOFF_SCARF items in one match day.
     */
    TOUT_MASTER(
        "Scarf Artist",
        "Six knock-off scarves flogged. Supply met demand.",
        6
    ),

    /**
     * Awarded for winning a street brawl on match day.
     */
    MATCH_DAY_WARRIOR(
        "Hooligan in Chief",
        "You took a side and came out on top. Proper result.",
        1
    ),

    /**
     * Awarded for walking through an away-fan mob wearing home-team colours.
     */
    WRONG_COLOURS(
        "Wrong Colours",
        "Three away fans disagree with your fashion choices.",
        1
    ),

    // ── Issue #1167: Northfield Amateur Boxing Club ───────────────────────

    /**
     * Awarded on entering the first Friday Night Fight bout.
     */
    FIRST_BOUT(
        "First Blood",
        "You signed up. Now you've got to get in that ring.",
        1
    ),

    /**
     * Awarded for winning 3 Friday Night Fight bouts.
     */
    AMATEUR_CHAMPION(
        "Local Hero",
        "Three wins at Tommy's. You're the talk of Northfield.",
        3
    ),

    /**
     * Awarded for winning the underground white-collar circuit.
     */
    WHITE_COLLAR_WINNER(
        "Gentleman Brawler",
        "You knocked out a bloke in a polo shirt. Proper classy.",
        1
    ),

    /**
     * Awarded for accepting a bout-fixing bribe from Wayne and winning.
     */
    FIXED_FIGHT(
        "Bent Promoter",
        "Wayne's money, your fists. Worked out, didn't it.",
        1
    ),

    /**
     * Awarded when caught with LOADED_GLOVE during pat-down.
     */
    LOADED_GLOVES(
        "Heavy Handed",
        "A bit of extra metal never hurt anyone. Until it did.",
        1
    ),

    /**
     * Awarded for completing the Tommy's Trophy Quest.
     */
    TOMMY_BOY(
        "Tommy's Boy",
        "You did right by the old man. He won't forget it.",
        1
    ),

    /**
     * Awarded for returning the 1987 ABA trophy and gaining permanent membership.
     */
    LEGACY_OF_THE_RING(
        "Legacy of the Ring",
        "1987. The year Tommy was somebody. Now you are too.",
        1
    ),

    /**
     * Awarded for training at the bag 3 times in a single in-game day.
     */
    GYM_RAT(
        "Gym Rat",
        "Three sessions before teatime. Tommy's seen it all now.",
        1
    ),

    // ── Issue #1171: Northfield TV Licence ────────────────────────────────────

    /**
     * Awarded for paying for a TV licence at the LETTERBOX_PROP.
     */
    HONEST_TELLY(
        "Licence to Watch",
        "You paid your TV licence. The BBC thanks you. Seriously.",
        1
    ),

    /**
     * Awarded for reaching SUMMONED status without ever paying.
     */
    EVADER(
        "Dodger",
        "The van's been round three times. You still haven't paid.",
        1
    ),

    /**
     * Awarded for selling a FORGED_TV_LICENCE to an NPC.
     */
    BOGUS_INSPECTOR(
        "Bogus Inspector",
        "You sold a fake licence. The check's in the post.",
        1
    ),

    /**
     * Awarded for evading detection when the DETECTOR_VAN passes with a lit TV.
     */
    DETECTOR_PROOF(
        "Detector Proof",
        "The van went past. The telly was on. They didn't notice.",
        1
    ),

    /**
     * Awarded for selling 3 FORGED_TV_LICENCE items (the NewspaperSystem headline trigger).
     */
    LOWEST_OF_THE_LOW_TELLY(
        "TV Licence Scammer",
        "Three pensioners. Three fake licences. The Daily Ragamuffin knows your face.",
        3
    ),

    // ── Issue #1181: Northfield Chugger Blitz ─────────────────────────────────

    /**
     * Awarded when the player donates to a CHUGGER NPC (2 COIN, −1 Notoriety).
     * Unlocked on first donation.
     */
    CHUGGER_GOODWILL(
        "Good Samaritan",
        "You gave 2 quid to someone holding a clipboard. They were genuinely grateful. Briefly.",
        1
    ),

    /**
     * Awarded when the player signs up for a Direct Debit via a CHUGGER NPC.
     * Triggers the 3-day 1 COIN/day deduction mechanic.
     */
    STANDING_ORDER(
        "Standing Order",
        "Three days. Three coins. Automatic. You know what you signed.",
        1
    ),

    /**
     * Awarded when the player punches a CHUGGER NPC (aggressive refusal).
     * Triggers FLEEING state, +8 Notoriety, +1 Wanted star.
     */
    CLIPBOARD_RAGE(
        "Clipboard Rage",
        "You punched a charity worker. The clipboard did not survive.",
        1
    ),

    /**
     * Awarded when the player collects fake donations using CHARITY_TABARD + CHARITY_CLIPBOARD.
     * Fraud detected on 2nd suspicious NPC contact.
     */
    DIRECT_DEBIT_HUSTLE(
        "Direct Debit Hustle",
        "Fake tabard. Real clipboard. Almost-real donations. Almost.",
        1
    ),

    /**
     * Awarded when the player successfully dodges a CHUGGER NPC three times in a row
     * using road-crossing, sprint, or disguise mechanics.
     */
    CHUGGER_DODGER(
        "Chugger Dodger",
        "Three times. Three different methods. You are basically a black belt in clipboard avoidance.",
        3
    ),

    // ── Issue #1183: Northfield Household Waste Recycling Centre ─────────────

    /**
     * Issue #1183: Awarded when the player finds a RETRO_CONSOLE in the WEEE skip.
     */
    TIP_TREASURE(
        "Tip Treasure",
        "Someone's mum threw away a perfectly good games console. Your gain.",
        1
    ),

    /**
     * Issue #1183: Awarded when the player deposits 3 or more items to the Reuse Corner
     * in a single visit to the HWRC.
     */
    REUSE_HERO(
        "Reuse Hero",
        "You gave back to the community. In the form of old curtains and a broken lamp.",
        1
    ),

    /**
     * Issue #1183: Awarded on the player's first trade waste visit (paid 15 COIN fee).
     */
    TRADE_WASTE_MOAN(
        "Trade Waste Moan",
        "Fifteen quid? For a bit of rubble? Dave has heard it before, mate.",
        1
    ),

    /**
     * Issue #1183: Awarded when the player bribes Dave (TIP_ATTENDANT) with 5 COIN
     * to use the hardcore bay without a HARDCORE_PERMIT.
     */
    BACKHANDER_AT_THE_TIP(
        "Backhander at the Tip",
        "I didn't see nothing. Five quid says I still don't.",
        1
    ),

    /**
     * Issue #1183: Awarded on the player's first successful night entry to the HWRC
     * via bolt-cutters on the gate chain (18:00–08:00).
     */
    MOONLIGHT_TIP_RUN(
        "Moonlight Tip Run",
        "In, loot the WEEE skip, out before sunrise. Very professional.",
        1
    ),

    /**
     * Issue #1183: Awarded when the player sells a total of 10 CIRCUIT_BOARDs
     * (cumulative across all sessions).
     */
    CIRCUIT_BOARD_MILLIONAIRE(
        "Circuit Board Millionaire",
        "Ten circuit boards sold. Dave would be disgusted. Gary at the scrapyard is delighted.",
        10
    ),

    // ── Issue #1186: Northfield Probation Office ──────────────────────────────

    /**
     * Issue #1186: Awarded when the player completes all fortnightly sign-ins
     * without missing one (entire probation order without breach).
     */
    STRAIGHT_AND_NARROW(
        "Straight and Narrow",
        "You signed in every fortnight. Every single one. Karen is almost impressed.",
        1
    ),

    /**
     * Issue #1186: Awarded when the player completes all 8 hours of community service
     * across the three available postings (park litter, Food Bank sorting, Community
     * Centre painting). Triggered by ProbationSystem.logServiceHours.
     */
    COMMUNITY_SPIRIT(
        "Community Spirit",
        "Eight hours of unpaid labour for the good of Northfield. You absolute legend.",
        1
    ),

    /**
     * Issue #1186: Awarded when the player's probation order is fully discharged
     * (all sign-ins completed or 8-hour community service total reached).
     */
    DONE_MY_TIME(
        "Done My Time",
        "Order discharged. You are, technically, a model citizen. For now.",
        1
    ),

    /**
     * Issue #1186: Awarded when the player pays the Fence to cut the ANKLE_TAG,
     * triggering TAG_TAMPER crime and the +3 star wanted escalation.
     */
    DONT_KNOW_YOU(
        "I Don't Know You",
        "You paid the Fence to cut your tag. The police would like a word. Several words.",
        1
    ),

    // ── Issue #1188: Northfield DWP Home Visit ────────────────────────────

    /**
     * Issue #1188: Awarded when the player claims UC for 10 consecutive in-game
     * weeks while also earning coin from street deals, fence sales, or boot sales.
     */
    BENEFIT_STREET(
        "Benefits Street",
        "Ten weeks claiming UC while earning on the side. The DWP hasn't noticed. Yet.",
        1
    ),

    /**
     * Issue #1188: Awarded the first time the player receives a DWP compliance
     * notice (DWP_LETTER_PROP placed at squat door).
     */
    BROWN_ENVELOPE(
        "Brown Envelope",
        "A brown envelope from the DWP. Never good news. Never.",
        1
    ),

    /**
     * Issue #1188: Awarded when the player successfully bluffs Brenda during
     * a DWP home visit (dice roll passes).
     */
    TALKED_MY_WAY_OUT(
        "Talked My Way Out",
        "You bluffed a DWP compliance officer. Brenda was not entirely convinced. But enough.",
        1
    ),

    /**
     * Issue #1188: Awarded when the player accumulates a CRIMINAL_REFERRAL
     * sanction from the DWP (suspicion ≥ 90 + evidence found).
     */
    BENEFIT_FRAUDSTER(
        "Benefit Fraudster",
        "The DWP have referred you to the Magistrates' Court. You've made the big leagues.",
        1
    ),

    /**
     * Issue #1188: Awarded when the player wins an appeal against a DWP sanction.
     */
    APPEAL_UPHELD(
        "Appeal Upheld",
        "The DWP ruled in your favour. They hate doing that.",
        1
    ),

    /**
     * Issue #1188: Awarded when a NOSY_NEIGHBOUR NPC tips off the DWP,
     * adding +15 suspicion score.
     */
    DWP_TIPOFF(
        "Nosy Neighbour",
        "Someone on your street rang the DWP. You know who it was.",
        1
    ),

    /**
     * Issue #1188: Awarded when the player declares 0 earnings for 5 consecutive
     * fortnightly sign-ons without triggering a home visit.
     */
    NOTHING_TO_DECLARE(
        "Nothing to Declare",
        "Five fortnights. Zero declared earnings. Brenda hasn't knocked. Yet.",
        1
    ),

    // ── Issue #1192: Northfield Sporting & Social Club ─────────────────────────

    /**
     * Issue #1192: Awarded when the player successfully joins the Northfield
     * Sporting &amp; Social Club by paying 5 COIN for a CLUB_MEMBERSHIP_CARD.
     */
    CLUB_MEMBER(
        "Members Only",
        "You're a card-carrying member of the Northfield Sporting & Social Club. Ron gave you a nod.",
        1
    ),

    /**
     * Issue #1192: Awarded when the player wins a side-bet darts match at the
     * Thursday Darts League at the social club.
     * (DARTS_HUSTLER_CLUB already present — this entry has been merged into it.)
     */

    /**
     * Issue #1192: Awarded when the player wins 3+ consecutive hands in the
     * back-room Pontoon session.
     */
    BACK_ROOM_BANKER(
        "Back Room Banker",
        "Three on the spin at Mick's table. He wasn't pleased. Nobody ever is.",
        1
    ),

    /**
     * Issue #1192: Awarded when the player catches Mick the card dealer cheating
     * using STREETWISE &ge; Journeyman skill.
     */
    CAUGHT_THE_CHEAT(
        "Caught the Cheat",
        "Caught Mick slipping an ace from his sleeve. He looked you dead in the eye. Said nothing.",
        1
    ),

    /**
     * Issue #1192: Awarded when the player embezzles club funds as Treasurer
     * at the AGM without being caught.
     */
    COOKING_THE_BOOKS(
        "Cooking the Books",
        "You've been embezzling from the social club's petty cash. In fairness, so was Derek.",
        1
    ),

    /**
     * Issue #1192: Awarded when the player successfully intercepts the Marchetti
     * Crew protection collector, landing 5 hits to drive them off.
     */
    CLUB_PROTECTOR(
        "Club Protector",
        "You saw off the Marchetti collector. Ron shook your hand. It was awkward but sincere.",
        1
    ),

    // ── Issue #1196: Environmental Health Officer ─────────────────────────────

    /**
     * Issue #1196: Awarded when the player successfully bribes Janet on first attempt.
     */
    GREASY_PALM(
        "Greasy Palm",
        "You slipped Janet a fiver. She took it without breaking eye contact. Respect.",
        1
    ),

    /**
     * Issue #1196: Awarded when the player sells a forged sticker that subsequently
     * fools Janet for at least one inspection (real condition passed due to re-cleaning).
     */
    FIVE_STAR_FRAUDSTER(
        "Five Star Fraudster",
        "Your forged sticker fooled the inspector. Briefly. Probably.",
        1
    ),

    /**
     * Issue #1196: Awarded when the player uses the kitchen-cleaning interaction to
     * raise a venue from rating 2 to rating 5 in a single inspection cycle.
     */
    CLEAN_KITCHEN(
        "Clean Kitchen",
        "You scrubbed that kitchen until it sparkled. Janet was almost impressed.",
        1
    ),

    /**
     * Issue #1196: Awarded when the player tips off the Council about a genuinely
     * rat-infested venue (SkipDivingSystem rats active + tip leads to closure).
     */
    PUBLIC_HEALTH_HERO(
        "Public Health Hero",
        "You reported the rats. Nobody will thank you. But you did the right thing.",
        1
    ),

    // ── Issue #1202: Karaoke Night ─────────────────────────────────────────

    /**
     * Issue #1202: Awarded when the player scores 3/3 on the BattleBarMiniGame
     * during a karaoke performance at Wetherspoons (Friday Karaoke Night).
     */
    KARAOKE_KING(
        "Karaoke King",
        "Nailed a karaoke performance at the Spoons. Bev nearly cracked a smile.",
        1
    ),

    /**
     * Issue #1202: Awarded when the player is hit by a thrown pint glass after
     * scoring ≤1/3 in a karaoke performance (TERRIBLE result).
     */
    BOTTLED_IT(
        "Bottled It",
        "Got glassed for a terrible karaoke performance. In fairness, it was bad.",
        1
    ),

    /**
     * Issue #1202: Awarded when the player performs karaoke to a max-capacity pub
     * (all NPC slots filled at Wetherspoons).
     */
    FULL_HOUSE(
        "Full House",
        "Performed to a packed pub. They weren't all there for you, but still.",
        1
    ),

    /**
     * Issue #1202: Awarded when the player steals the MICROPHONE_PROP from the
     * karaoke booth while Bev is distracted during or after a rival's performance.
     */
    MIC_DROP(
        "Mic Drop",
        "Stole the mic mid-show. Bev is absolutely fuming.",
        1
    ),

    // ── Issue #1205: Northfield DVSA Test Centre ──────────────────────────────

    /**
     * Awarded when the player passes the DVSA theory test with ≥8/10 correct.
     */
    THEORY_PASSMARK(
        "First Time Pass",
        "You passed the theory test. Eight out of ten. The Highway Code is surprisingly violent.",
        1
    ),

    /**
     * Awarded when the player obtains a full DRIVING_LICENCE by passing the
     * practical test with Sandra (≤15 faults).
     */
    ROAD_LEGAL(
        "Road Legal",
        "Full UK licence. Sandra looked almost proud. Almost.",
        1
    ),

    /**
     * Awarded when the player successfully bribes Sandra for an instant pass
     * (CHARISMA ≥2 + 15 COIN, 70% success, no witnesses within 6 blocks).
     */
    PALM_GREASED(
        "Cash in Hand",
        "A brown envelope and a firm handshake. Sandra calls it a gift. The law calls it something else.",
        1
    ),

    /**
     * Awarded when the player completes 3 driving lessons with Keith.
     */
    BACKSEAT_DRIVER(
        "Backseat Driver",
        "Three lessons with Keith. He's given up correcting your mirror checks.",
        1
    ),

    /**
     * Awarded when a police NPC is spawned due to the unlicensed-driving penalty
     * (player entered a car without a DRIVING_LICENCE).
     */
    DRIVING_WITHOUT_DUE_CARE(
        "No Licence, No Problem",
        "The police disagree. Twenty percent chance. You hit the twenty percent.",
        1
    ),

    // ── Issue #1209: Citizens Advice Bureau ───────────────────────────────────

    /**
     * Awarded when the player uses the Citizens Advice Bureau for the first time
     * (completes a consultation with Margaret or Brian).
     */
    ADVICE_SEEKER(
        "Know Your Rights",
        "First time at the CAB. Margaret looked genuinely pleased someone came in.",
        1
    ),

    /**
     * Awarded when the player successfully appeals a benefit sanction via the CAB
     * (BENEFIT_SANCTION consultation succeeds and DWPSystem sanction is lifted).
     */
    APPEAL_VICTORY(
        "The System Works",
        "Sanction lifted. Margaret punched the air. You pretended not to notice.",
        1
    ),

    /**
     * Awarded when the player skips the CAB queue via bribe or intimidation.
     */
    CAB_QUEUE_JUMPER(
        "Queue Jumper",
        "Three quid or a shove. Either way, you're next.",
        1
    ),

    /**
     * Awarded when Brian produces a forged letter for the player (first forgery).
     */
    PAPER_TRAIL(
        "Paper Trail",
        "Brian types with two fingers and a guilty conscience.",
        1
    ),

    /**
     * Awarded when the player receives 3 total forged documents from Brian.
     */
    SERIAL_FRAUDSTER(
        "Serial Fraudster",
        "Three forgeries. Brian keeps a copy for his own records. You didn't ask why.",
        1
    ),

    // ── Issue #1216: Northfield Driving Instructor ─────────────────────────────

    /**
     * Awarded when the player completes 5 lessons with Dave and receives a
     * FULL_DRIVING_LICENCE from the DRIVING_SCHOOL (not via DVSA test).
     */
    SHORTCUT_TO_NOWHERE(
        "Why Learn When You Can Forge?",
        "You paid 20 COIN for a certificate that will definitely hold up. Definitely.",
        1
    ),

    /**
     * Awarded when the player sabotages a live driving lesson (beeping horn,
     * cutting in at a junction, or parking to block the route).
     */
    BACK_SEAT_DRIVER(
        "You Absolute Menace",
        "The learner stalled three times and cried. Dave will be filing a report.",
        1
    ),

    /**
     * Awarded when the player steals Dave's dual-control Corsa overnight.
     */
    TAKING_THE_WHEEL(
        "L-Plates and All",
        "Dave's dual-control Corsa. The L-plates didn't survive the first roundabout.",
        1
    ),

    // ── Issue #1218: Northfield Claims Management Company ─────────────────────

    /**
     * Awarded when the player successfully files a fraudulent personal injury
     * claim at Compensation Kings and receives a payout.
     */
    COMPENSATION_NATION(
        "Where There's Blame, There's a Claim",
        "Gary was delighted. The insurance company was not. You were 5 COIN richer.",
        1
    ),

    /**
     * Awarded when an INSURANCE_INVESTIGATOR catches the player sprinting,
     * fighting, or breaking blocks within 20 blocks while a claim is pending,
     * triggering claim invalidation and INSURANCE_FRAUD on the criminal record.
     */
    CAUGHT_IN_THE_ACT(
        "You Absolute Melt",
        "The investigator had photos. Gary has disowned you. The neck brace is in a bin.",
        1
    ),

    /**
     * Awarded when the player files a claim while wearing a NECK_BRACE,
     * increasing the payout multiplier to ×1.5.
     */
    NECK_BRACE_BANDIT(
        "Medically Certified Wrong 'Un",
        "The neck brace cost 0 COIN. Gary's cut was 30%. You still came out ahead.",
        1
    ),

    /**
     * Awarded when the player suffers a genuine unprovoked injury (dog bite or
     * car collision) and Gary waives his cut, paying out in full.
     */
    GENUINE_VICTIM(
        "Actually Hurt",
        "This one wasn't staged. Gary felt bad. He still filed the claim, obviously.",
        1
    ),

    /**
     * Awarded when the player successfully shakes an INSURANCE_INVESTIGATOR by
     * staying 60 blocks away for 5 in-game minutes without being seen.
     */
    SHOOK_THE_TAIL(
        "Ghost Protocol",
        "Gone in five minutes. The investigator went home. Gary was impressed.",
        1
    ),

    // ── Issue #1224: Northfield Cybernet Internet Café ────────────────────────

    /**
     * Awarded on the first successful FlipIt sale at Cybernet's online marketplace.
     * The hustle doesn't sleep, even at 22:58 on a Tuesday.
     */
    DIGITAL_HUSTLER(
        "Buy Low, Sell High, Stay Dodgy",
        "Your first FlipIt sale. Asif nodded approvingly. Sort of.",
        1
    ),

    /**
     * Awarded after completing 10 total FlipIt sales at Cybernet.
     * You've become a one-man grey-market logistics operation.
     */
    POWERSELLER(
        "Feedback: A+++++ Would Scam Again",
        "Ten sales on FlipIt. Hamza's impressed. Asif doesn't want to know.",
        10
    ),

    /**
     * Awarded on the first successful phishing session payout at Cybernet.
     * A fictional Nigerian prince could not be reached for comment.
     */
    NIGERIAN_PRINCE(
        "Congratulations, You Have Been Selected",
        "You ran a phishing scam from Asif's terminal. The coins are real. The guilt is not.",
        1
    ),

    /**
     * Awarded on first successful document forgery at Cybernet's back-room printer.
     * Hamza said "nice one" and then immediately went back to his phone.
     */
    FORGER(
        "Ctrl+P, Ctrl+F Life",
        "Printed your first forged document at Cybernet. Hamza looked the other way.",
        1
    ),

    // ── Issue #1227: Wheelwright Motors — Dodgy Car Lot ──────────────────────

    /**
     * Awarded on first successful haggle below asking price at Wheelwright Motors.
     * Wayne said "Go on then" and shook hands. You both knew it was a win.
     */
    WHEELER_DEALER(
        "No Reasonable Offer Refused",
        "Haggled Wayne down at Wheelwright's. He still made a profit. Probably.",
        1
    ),

    /**
     * Awarded on first finance purchase at Wheelwright Motors.
     * Ten quid a day for ten days. What could possibly go wrong?
     */
    ON_THE_NEVER_NEVER(
        "Never Never Land",
        "Bought a car on Wayne's dodgy hire purchase. The repo man cometh.",
        1
    ),

    /**
     * Awarded when player sells a stolen car using a FAKE_V5C at Wheelwright's.
     * Wayne didn't ask. You didn't tell. Beautiful arrangement, really.
     */
    CLEAN_TITLE(
        "Nothing to See Here, Officer",
        "Sold a stolen car with forged documents. Wayne pocketed the cash. You split.",
        1
    ),

    /**
     * Awarded when player clocks a car with Bez's help and sells it to a civilian.
     * The mileage said 12,000. It had done 140,000. Classic.
     */
    DODGY_MILEAGE(
        "Odometer? More Like Odd-ometer",
        "Clocked a car and flogged it to a punter. Trading Standards were watching.",
        1
    ),

    /**
     * Awarded on first successful VIN plate swap at the Scrapyard.
     * The car is now officially a different car. Legally speaking.
     */
    VIN_SWAP(
        "Identity Crisis",
        "Swapped the VIN plates at Pearce's yard. The ANPR won't know what to think.",
        1
    ),

    /**
     * Awarded when the repo man takes the player's financed car after 3 missed payments.
     * It was gone when you got up. Bez had looked the other way.
     */
    REPOSSESSED(
        "Gone Before Breakfast",
        "The repo man came in the night and took your car back. Should've paid Wayne.",
        1
    ),

    // ── Issue #1237: Northfield St. Aidan's Primary School ───────────────────

    /**
     * Awarded when the player pickpockets Dot (DINNER_LADY) for COIN.
     * Ms. Pearson was watching from the hatch. Dot cried. You didn't hang around.
     */
    DINNER_MONEY_THIEF(
        "Dinner Money Bandit",
        "Pickpocketed the dinner lady. Dot had a good cry. You had a fiver.",
        1
    ),

    /**
     * Awarded when the player sells contraband CRISPS to SCHOOL_KID NPCs 5 times
     * during lunch while Ms. Pearson isn't watching.
     * Northfield's first-ever underground snack racket.
     */
    TUCK_SHOP_BANDIT(
        "Black Market Snacks",
        "Sold crisps to kids five times under Ms. Pearson's nose. You ran a whole operation.",
        5
    ),

    /**
     * Awarded when the player sprints through a PRAM during the school run chaos.
     * The toddler thought it was funny. The mum did not.
     */
    PUSHCHAIR_MENACE(
        "Road Rage Jr.",
        "Sprinted through a pram during the school run. The toddler was fine. The mum less so.",
        1
    ),

    /**
     * Awarded when the player triggers a NoiseSystem event ≥ magnitude 60 (e.g. wheelie
     * bin fire) during an Ofsted inspection, causing the inspectors to flee.
     * The school is now Requires Improvement. Technically your fault.
     */
    OFSTED_SABOTEUR(
        "Special Measures",
        "Caused Ofsted to flee the school in chaos. The report said Requires Improvement.",
        1
    ),

    /**
     * Awarded when the player helps decorate the school during an Ofsted visit
     * (Notoriety −2, positive outcome).
     * You briefly became the most helpful person in Northfield.
     */
    HEAD_OF_CLASS(
        "Model Citizen (Briefly)",
        "Helped decorate the school during Ofsted. They gave it a Good. You got -2 Notoriety.",
        1
    ),

    /**
     * Awarded when the player sells the OFSTED_DRAFT_REPORT to a newspaper journalist.
     * Front page of the Northfield Gazette. Mrs Fowler never forgave you.
     */
    SCHOOL_REPORT(
        "Front Page Exclusive",
        "Sold the Ofsted draft report to a journalist. It was in the Gazette by Tuesday.",
        1
    ),

    // ── Issue #1240: Northfield NHS Blood Donation Session ────────────────────

    /**
     * Awarded on first legitimate blood donation to Brenda (NHS_DONOR_COORDINATOR).
     * You sat in the recliner. You gave blood. You ate a biscuit. Northfield is proud.
     */
    GOOD_CITIZEN(
        "Good Citizen",
        "Donated blood at the NHS mobile unit. Brenda said you were very brave.",
        1
    ),

    /**
     * Awarded when the player completes 3 legitimate donations across separate sessions
     * (84-day cooldown enforced between each).
     * You are a regular. They know your name. They save you the good biscuits.
     */
    REGULAR_DONOR(
        "Regular Donor",
        "Donated blood three times. They've put your name on the biscuit tin.",
        3
    ),

    /**
     * Awarded when the player successfully presents a FORGED_DONOR_QUESTIONNAIRE to Brenda.
     * The form was perfect. Tyler wasn't looking. You got a biscuit anyway.
     */
    FORGED_THEIR_WAY_TO_A_BISCUIT(
        "Forged Their Way to a Biscuit",
        "Submitted a forged questionnaire and got away with it. Worth it for the Hobnob.",
        1
    ),

    /**
     * Awarded when the player donates twice in one session using DisguiseSystem (score ≥ 3).
     * Tyler didn't recognise you. The nurses did. They let it go — you seemed enthusiastic.
     */
    TWICE_THE_HERO(
        "Twice the Hero",
        "Donated blood twice in one session. Tyler was fooled. Brenda had her suspicions.",
        1
    ),

    /**
     * Awarded when the player steals the BISCUIT_TABLE_PROP tin while unobserved.
     * Eight biscuits. Gone. Northfield's most audacious heist.
     */
    BISCUIT_TIN_BANDIT(
        "Biscuit Tin Bandit",
        "Nicked the donation tin when nobody was looking. Eight biscuits. No remorse.",
        1
    ),

    /**
     * Awarded when the player steals 3 or more BLOOD_BAGs from BLOOD_FRIDGE_PROP in one session.
     * The van was raided. The Gazette ran it on the front page. You were long gone.
     */
    PLASMA_KING(
        "Plasma King",
        "Stole three blood bags from the NHS van. The Gazette called it 'audacious'.",
        1
    ),

    // ── Issue #1243: Northfield Bert's Tyres & MOT ───────────────────────────

    /**
     * Awarded when the player uses the callout mechanic (E twice, StreetReputation ≥ 40)
     * and Bert is forced to issue a free pass.
     */
    CALLED_HIS_BLUFF(
        "Called His Bluff",
        "Called out Bert's fake failure. He issued a free pass and said nothing.",
        1
    ),

    /**
     * Awarded when the player passes a BROWN_ENVELOPE to Bert and receives a PASS_BRIBE
     * MOT certificate.
     */
    ENVELOPE_ECONOMY(
        "Envelope Economy",
        "Slipped Bert a brown envelope. Certificate signed, no questions asked.",
        1
    ),

    /**
     * Awarded when the player loots a CATALYTIC_CONVERTER from the INSPECTION_PIT_PROP
     * while Bert is distracted.
     */
    CAT_BURGLAR(
        "Cat Burglar",
        "Nicked a catalytic converter from Bert's pit. Worth thirty-five quid.",
        1
    ),

    /**
     * Awarded when the player is present during a DVSA_INSPECTOR raid and does not
     * interfere, letting the inspector do their job.
     */
    CIVIC_DUTY(
        "Civic Duty",
        "Watched the DVSA raid Bert's garage and did absolutely nothing to stop it.",
        1
    ),

    /**
     * Awarded when the player sells a STOLEN_TYRE to Bert while Bert is himself
     * in possession of another STOLEN_TYRE.
     */
    STOLEN_ON_STOLEN(
        "Stolen on Stolen",
        "Sold a stolen tyre to a man who already had one. Economics of the estate.",
        1
    ),

    /**
     * Awarded when the player's PASS_BRIBE certificate survives a DVSA inspection
     * without being invalidated.
     */
    ROADWORTHY_ISH(
        "Roadworthy-ish",
        "Bert's bribe certificate passed the DVSA check. Technically, you're legal.",
        1
    ),

    // ── Issue #1252: Northfield TV Licensing ──────────────────────────────────

    /**
     * Awarded when the player approaches and interacts with the DETECTOR_VAN_PROP
     * to discover it is unmanned — debunking the great TV Licensing myth.
     */
    MYTH_BUSTER(
        "Myth Buster",
        "The detector van was empty. It always was.",
        1
    ),

    /**
     * Awarded when the player purchases a TV Licence at the Post Office.
     */
    LAW_ABIDING_VIEWER(
        "Law-Abiding Viewer",
        "You paid for your TV Licence. Your mum would be proud.",
        1
    ),

    /**
     * Awarded when the player receives 3 enforcement letters without paying
     * the TV Licence fee.
     */
    LICENCE_EVADER(
        "Licence Evader",
        "Three letters from TV Licensing. You haven't read a single one.",
        1
    ),

    /**
     * Awarded when the player successfully avoids Derek on 5 consecutive visits
     * (TV not visible, successful lie, or biscuit accepted).
     */
    DEREK_S_NEMESIS(
        "Derek's Nemesis",
        "Five times Derek knocked. Five times he left empty-handed.",
        5
    ),

    // ── Issue #1257: Northfield Rag-and-Bone Man ──────────────────────────────

    /**
     * Awarded (instant) when the player repairs Barry's slashed tyres with a RUBBER_TYRE,
     * restoring the Rag-and-Bone round and earning Barry's gratitude (10 COIN reward).
     */
    BARRY_S_MATE(
        "Barry's Mate",
        "Sorted Barry's van out. He owes you one.",
        1
    ),

    /**
     * Progress achievement (×5): awarded for completing the Door-Knock Pre-buy hustle
     * 5 times — knocking on residential doors before Barry arrives and reselling junk to him.
     */
    KNOCKER_BOY(
        "Knocker Boy",
        "Five times you beat Barry to the door.",
        5
    ),

    /**
     * Progress achievement (×10): awarded for completing 10 scrap-selling transactions
     * with Barry across the Horsebox Hustle mechanics (any combination of sellable items).
     */
    HORSEBOX_HUSTLER(
        "Horsebox Hustler",
        "Ten deals done off the back of a flatbed Transit.",
        10
    ),

    // ── Issue #1263: Northfield Illegal Street Racing ─────────────────────────

    /**
     * Awarded (instant) when the player wins a ring road sprint race in 1st place.
     * Triggers on StreetRacingSystem race resolution with player in position 1.
     */
    RING_ROAD_KING(
        "Ring Road King",
        "Won a ring road sprint. Shane gave you a nod. The boy racers were sick.",
        1
    ),

    /**
     * Awarded (instant) when the player enters the race without any car modifications
     * (no CarDealershipSystem upgrades) and finishes in any position.
     * Proves you don't need the fancy gear.
     */
    STOCK_STANDARD(
        "Stock Standard",
        "Raced with no mods. Just the car and your nerve. Fair play.",
        1
    ),

    /**
     * Awarded (instant) on first successful nitrous-line sabotage (SCREWDRIVER on
     * competitor's car) that directly contributes to a race win.
     * The dirty tricks achievement.
     */
    DIRTY_TRICKS(
        "Dirty Tricks",
        "Loosened someone's nitrous line. They didn't finish. You did.",
        1
    ),

    // ── Issue #1265: Northfield Loan Shark — Big Mick's Doorstep Lending ─────

    /**
     * Awarded (instant) when the player takes out their first loan from Big Mick.
     * Triggers on LoanSharkSystem.borrow() with any tier. Welcome to the red.
     */
    IN_THE_RED(
        "In the Red",
        "Borrowed off Big Mick. Everyone does it once. Most do it twice.",
        1
    ),

    /**
     * Awarded (instant) when the player's debt reaches the compounded maximum
     * (2× original loan) and the collector seizes a hotbar item.
     * The point of no return.
     */
    SPIRAL(
        "Spiral",
        "Debt hit the cap. Collector's been round. You knew it would come to this.",
        1
    ),

    /**
     * Awarded (instant) when the player fully repays their loan (principal + all
     * accrued interest) via LoanSharkSystem.repay(). Clean slate — for now.
     */
    CLEARED_UP(
        "Cleared Up",
        "Paid Big Mick back every penny. He's already looking for your replacement.",
        1
    ),

    /**
     * Awarded (instant) when the player completes the Ledger Job hustle: steals the
     * DEBT_LEDGER from Big Mick's desk and sells it to the Marchetti Crew contact.
     * Clears the debt but triggers 2 THUG NPCs for 5 in-game days.
     */
    BIG_MICK_BOUNCED(
        "Big Mick Bounced",
        "Flogged his ledger to the Marchettis. Debt gone. Thugs incoming. Worth it.",
        1
    ),

    // ── Issue #1269: Northfield BT Phone Box ──────────────────────────────────

    /**
     * Awarded (instant) when the player makes their first successful anonymous tip-off
     * from the phone box that leads to a POLICE NPC being dispatched.
     * Triggers POLICE_TIP_OFF rumour. Reduces Wanted stars by 1.
     */
    GRASSIN_UP(
        "Grassin' Up",
        "Rang the Old Bill anonymously. That felt wrong. And right.",
        1
    ),

    /**
     * Awarded (instant) when the player successfully collects their first Marchetti
     * dead-drop delivery from a MARCHETTI_RUNNER NPC.
     * Requires Marchetti Respect >= 30 and SCRAWLED_NUMBER in inventory.
     */
    DEAD_DROP(
        "Dead Drop",
        "Someone left a package. You didn't ask what was in it.",
        1
    ),

    /**
     * Awarded (progress) when the player has made 10 total calls from the phone box.
     * Tracks all call types combined. A regular on the payphone.
     */
    OFF_THE_BOOKS(
        "Off the Books",
        "Ten calls. All anonymous. All from a box nobody else uses.",
        10
    ),

    /**
     * Awarded (instant) when the player repairs the broken estate phone box
     * using 3× SCRAP_METAL. Seeds PHONE_BOX_REPAIR rumour. +2 Neighbourhood Vibes.
     */
    LAST_PHONE_STANDING(
        "Last Phone Standing",
        "Fixed the estate box. You're basically BT Openreach now.",
        1
    ),

    // ── Issue #1271: Northfield Tattoo Parlour ────────────────────────────────

    /** Get first tattoo from Daz's tattoo parlour. */
    FRESH_INK(
        "Fresh Ink",
        "Got your first tattoo from Daz. Looks proper, to be fair.",
        1
    ),

    /** Get all 4 tattoo types (flash, custom, jailhouse, touch-up) in one playthrough. */
    WALKING_CANVAS(
        "Walking Canvas",
        "Four different tattoos. You're basically a gallery at this point.",
        4
    ),

    /** Complete 3 walk-in hustle sessions without getting caught. */
    UNLICENSED_OPERATOR(
        "Unlicensed Operator",
        "Three sessions on Daz's kit. No licence, no problem — so far.",
        3
    ),

    /** Survive the INFECTED_INK debuff — health reaches warning threshold, then cured. */
    INFECTION_SURVIVOR(
        "Infection Survivor",
        "That jailhouse special nearly did you in. Nearly.",
        1
    ),

    /** Use tattoo recognition reduction to evade a WANTED chase. */
    LOOKING_PROPER_DIFFERENT(
        "Looking Proper Different",
        "The ink worked. Police walked right past. You look like a different person.",
        1
    ),

    // --- Issue #1273: Northfield Fly-Tipping Ring ---

    /** Complete the first waste-clearance job. */
    GRIM_REAPER(
        "Grim Reaper",
        "You cleared their rubbish for cash. Doesn't matter where it ends up.",
        1
    ),

    /** Fly-tip 5 loads without getting caught. */
    THE_ENVIRONMENT_THOUGH(
        "The Environment, Though",
        "Five loads, no council. Someone else's problem now.",
        5
    ),

    /** Dispose of 3 loads legitimately at the Recycling Centre. */
    CIVIC_PRIDE(
        "Civic Pride",
        "Three trips to the tip. You're practically a councillor.",
        3
    ),

    /** Trigger the 'Fly-Tipping Crisis Hits Northfield' newspaper headline. */
    HEADLINE_SHAME(
        "Headline Shame",
        "Front page of the Ragamuffin. Mum is not pleased.",
        1
    ),

    /** Burn a load near a wheelie bin fire and have the fire brigade show up. */
    BURN_IT_ALL(
        "Burn It All",
        "Plan B was a bit much. But it worked.",
        1
    ),

    // ── Issue #1276: Northfield Minicab Office — Big Terry's Cabs ─────────────

    /** Use Big Terry's Cabs for 5 paid rides. */
    BIG_TERRYS_REGULAR(
        "Big Terry's Regular",
        "Five rides with Terry. He knows your face and your usual. You don't ask about the other passengers.",
        5
    ),

    // ── Issue #1278: Northfield Travelling Fairground ─────────────────────────

    /** Survive a Dodgems session without drawing police attention. */
    DODGEMS_ACE(
        "Dodgems Ace",
        "Survived a Dodgems session without drawing police attention. Barely.",
        1
    ),

    /** Ride the Waltzers three times in one fairground visit. */
    DIZZY_RASCAL(
        "Dizzy Rascal",
        "Rode the Waltzers three times in one visit. You're still not right.",
        3
    ),

    /** Hit the bell on the Strongman High-Striker. */
    BELLRINGER(
        "Bellringer",
        "Hit the bell on the Strongman High-Striker. Big Lenny looked almost impressed.",
        1
    ),

    /** Complete a cash-in-hand Strongman shift for Big Lenny. */
    FAIRGROUND_WORKER_BADGE(
        "Fairground Worker",
        "Completed a cash-in-hand shift running the Strongman for Big Lenny.",
        1
    ),

    /** Experience every fairground attraction in a single visit. */
    ALL_THE_FUN_OF_THE_FAIR(
        "All the Fun of the Fair",
        "Experienced every attraction at the travelling fairground in a single visit.",
        1
    ),

    // ── Issue #1280: Northfield Nightclub — The Vaults ────────────────────────

    /** Gain entry to The Vaults on 10 separate nights. */
    VAULTS_REGULAR(
        "Vaults Regular",
        "Ten nights in The Vaults. Big Dave nods. That means something.",
        10
    ),

    /** Win a freestyle battle on the dancefloor at The Vaults. */
    DANCE_FLOOR_LEGEND(
        "Dance Floor Legend",
        "Owned the dancefloor at The Vaults. The crowd parted. The strobes agreed.",
        1
    ),

    /** Broker peace between two fighting factions inside The Vaults. */
    PEACEKEEPER(
        "Peacekeeper",
        "Stopped a brawl at The Vaults. For once, nobody got ejected. Almost disappointing.",
        1
    ),

    /** Use the fire exit to smuggle goods out of The Vaults undetected. */
    BACK_DOOR_MERCHANT(
        "Back Door Merchant",
        "Slipped out the fire exit with something you shouldn't have. No alarm. No conscience.",
        1
    ),

    /** Still standing at closing time (02:45) on 3 separate nights. */
    CLOSING_TIME_CHAMPION(
        "Closing Time Champion",
        "Last one standing at 02:45 three nights running. The kebab van knows your order.",
        3
    ),

    // ── Issue #1282: Northfield Day & Night Chemist ───────────────────────────

    /** Acquire any OTC item from the Day & Night Chemist. */
    PROPER_ILL(
        "Proper Ill",
        "You bought something from the chemist. Self-medication is the British way.",
        1
    ),

    /** Reach the DEPENDENCY debuff via Nurofen Plus overconsumption. */
    NUROFEN_NIGHTMARE(
        "Nurofen Nightmare",
        "Five doses of Nurofen Plus in 24 hours. The codeine giveth. The codeine taketh.",
        1
    ),

    /** Complete the drug safe heist — obtain DIAZEPAM from the DRUG_SAFE_PROP. */
    BACK_STREET_PHARMACIST(
        "Back Street Pharmacist",
        "Crowbarred open the drug safe at the chemist. Janet is absolutely fuming.",
        1
    ),

    // ── Issue #1286: Northfield Cash Converters ───────────────────────────────

    /** Sell 3 items to Dean without triggering a serial check failure. */
    DEAN_APPROVED(
        "Dean Approved",
        "Clean as a whistle. Dean almost smiled.",
        3
    ),

    /** Complete 5 transactions with Dave the Middleman. */
    BACK_ALLEY_REGULAR(
        "Back Alley Regular",
        "Dave doesn't even ask what it is anymore.",
        5
    ),

    /** Sell a wiped phone that passes Dean's check. */
    IMEI_ARTISTE(
        "IMEI Artiste",
        "Tariq's finest work. Probably.",
        1
    ),

    /** Smash the display case and take something before Dave shows up. */
    GLASS_CASE_OPPORTUNIST(
        "Glass Case Opportunist",
        "You couldn't wait, could you.",
        1
    ),

    // ── Issue #1289: Northfield Meredith & Sons Funeral Parlour ───────────────

    /** Paid Gerald for a pre-need arrangement. */
    PLAN_AHEAD(
        "Plan Ahead",
        "You've paid Gerald to sort your funeral in advance. He seemed pleased. You felt strange.",
        1
    ),

    /** Looted items from a casket in the viewing room. */
    GRAVE_GOODS_COLLECTOR(
        "Grave Goods Collector",
        "You rifled through someone's effects in the viewing room. They weren't using them.",
        1
    ),

    /** Drove the hearse without crashing or being caught. */
    DEAD_DELIVERY(
        "Dead Delivery",
        "You borrowed Meredith's hearse and returned it in one piece. Technically.",
        1
    ),

    /** Gave a condolences card to a mourner during a funeral procession. */
    COLD_COMFORT(
        "Cold Comfort",
        "You handed a condolences card to a grieving stranger. It probably helped. Probably.",
        1
    ),

    /** Sold a war medal to Gerald (Gold Teeth Sideline). */
    GERALD_S_REGULAR(
        "Gerald's Regular",
        "Gerald pays better than the pawn shop. Best not to ask why.",
        1
    ),

    // ── Issue #1291: Northfield Bert's Tyres & MOT ───────────────────────────

    /**
     * Awarded when the player passes a BROWN_ENVELOPE to Bert and receives a PASS_BRIBE
     * outcome (corruption score ≥ 70). Requires COIN ≥ 2 and BROWN_ENVELOPE in inventory.
     * Triggers UNROADWORTHY rumour. Records VEHICLE_FRAUD in CriminalRecord.
     */
    BROWN_ENVELOPE_TEST(
        "Brown Envelope Test",
        "You bribed Bert. The car definitely doesn't pass. But the certificate says it does.",
        1
    ),

    /**
     * Awarded when the player sells a STOLEN_TYRE to Bert.
     * Triggers STOLEN_GOODS_TRADE rumour on first sale.
     */
    PART_WORN_PROFITEER(
        "Part Worn Profiteer",
        "Part worn. Part nicked. Bert's not fussy either way.",
        1
    ),

    /**
     * Awarded when the player steals 3+ CATALYTIC_CONVERTER items from parked cars.
     * Triggers CATALYTIC_THEFT_SPREE rumour and NewspaperSystem headline.
     */
    CONVERTER_KING(
        "Converter King",
        "Three catalytic converters in a week. The exhaust fumes are someone else's problem.",
        3
    ),

    /**
     * Awarded when the player loots the INSPECTION_PIT_PROP while Bert is distracted.
     * Requires BERT_DISTRACTED state. Records no crime unless caught.
     */
    PIT_STOP(
        "Pit Stop",
        "You had a rummage in Bert's inspection pit. Found some things. Best not say what.",
        1
    ),

    /**
     * Awarded when the player uses the GARAGE_PHONE_PROP to distract Bert via Kyle.
     * Creates a 20-second BERT_DISTRACTED window. Once per in-game hour.
     */
    PHONE_A_FRIEND(
        "Phone a Friend",
        "You rang the garage phone. Kyle answered. Bert followed. Classic.",
        1
    ),

    /**
     * Awarded when the player tips Bert off about an incoming DVSA raid
     * (STREET_LADS Respect ≥ 30). Seeds BERT_WARNED rumour. +5 STREET_LADS Respect.
     */
    TIP_OFF(
        "Tip-Off",
        "You warned Bert about the inspector. He owes you one. He probably won't pay.",
        1
    ),

    // ── Issue #1303: Northfield Dave's Carpets ────────────────────────────────

    /**
     * Awarded when the player earns commission by referring 5 NPCs to Dave's Carpets.
     * Each referral with a CLOSING_DOWN_FLYER yields +1 COIN.
     * Seeds DEAL_BROKER rumour on unlock.
     */
    CARPET_KING(
        "Carpet King",
        "Five suckers walked through Dave's door because of you. He's still closing down.",
        5
    ),

    /**
     * Awarded when the player delivers a SOFA to a squat AND has CARPET_OFFCUT flooring placed.
     * Requires SACK_TRUCK for transport. +20 Vibe via SquatFurnishingTracker.
     */
    INTERIOR_DECORATOR(
        "Interior Decorator",
        "Sofa delivered. Carpet down. The squat has never looked so grim.",
        1
    ),

    /**
     * Awarded when the player loots the CARPET_ROLL_PROP while Kev is distracted.
     * Yields CARPET_OFFCUT x2-4 and SACK_TRUCK. Seeds CARPET_THIEF rumour.
     */
    CARPET_ROLL_HEIST(
        "Five-Finger Discount",
        "Kev had a Twix. You had a carpet roll. Fair trade, really.",
        1
    ),

    /**
     * Awarded when the player earns 8 COIN from their own fake closing-down pitch
     * without Keith (MARKET_INSPECTOR) catching them.
     * Requires CLOSING_DOWN_FLYERs and 4 sales before inspector spawn.
     */
    COPYCAT_DAVE(
        "Copycat Dave",
        "You set up your own closing-down sale. Dave's been doing it three years. You managed four sales.",
        1
    ),

    /**
     * Awarded when the player successfully reports Dave to Sandra (Trading Standards).
     * Dave enters DEFLATED state for 24h; TRADING_STANDARDS_WARNING prop placed.
     * Seeds DAVE_REPORTED rumour.
     */
    REPORTED_DAVE(
        "Reported Dave",
        "You grassed up Dave to Trading Standards. Sandra was not amused. Dave was less so.",
        1
    ),

    /**
     * Awarded when the player interacts with Dave while he is in DEFLATED state
     * (after Sandra's visit). Dave delivers a mournful speech.
     */
    STANDING_CUSTOMER_DAVE(
        "Standing Customer",
        "You visited Dave while he was at rock bottom. He appreciated it. Sort of.",
        1
    ),

    // ── Issue #1306: Northfield Traveller Site ────────────────────────────────

    /**
     * Awarded when the player completes 2 cash-in-hand jobs for Paddy Flynn,
     * unlocking DOG_PERMISSION_FLAG.
     */
    PADDYS_GRAFTER(
        "Paddy's Grafter",
        "Did two jobs for Paddy. You're practically family. Practically.",
        1
    ),

    /**
     * Awarded when the player sells SCRAP_METAL, COPPER_PIPE, STOLEN_BIKE, or STOLEN_PHONE
     * to Paddy's scrap fence and reaches the 20 COIN cap in a single visit.
     */
    SCRAP_KING(
        "Scrap King",
        "Maxed out Paddy's scrap budget in one visit. He's impressed. You should be worried.",
        1
    ),

    /**
     * Awarded when the player reports the dog fight ring to the RSPCA,
     * triggering dispersal of the ring and RSPCA_OFFICER spawn.
     */
    RSPCA_GRASS(
        "RSPCA Grass",
        "You reported Paddy's dog fight to the RSPCA. The dogs are fine. Paddy is not.",
        1
    ),

    /**
     * Awarded when the player tips off the council on day 1, causing Derek
     * to arrive same afternoon instead of day 3.
     * NeighbourhoodSystem COMMUNITY_RESPECT +2.
     */
    COMMUNITY_WATCH_HERO(
        "Community Watch Hero",
        "Got the travellers shifted before they'd even set up properly. The neighbours are delighted.",
        1
    ),

    /**
     * Awarded when the player successfully raids the caravan between 02:00 and 04:00,
     * obtaining at least 8 COIN and the DOG_FIGHT_LEDGER.
     */
    NIGHT_RAIDER(
        "Night Raider",
        "Turned over Paddy's caravan in the small hours. The lurcher didn't even notice.",
        1
    ),

    /**
     * Awarded when the player wins a bet at the DOG_FIGHT_RING_PROP (2:1 payout).
     */
    DIRTY_MONEY(
        "Dirty Money",
        "Won a few quid at Paddy's dog fight. Keep it quiet.",
        1
    ),

    /**
     * Awarded when the player applies TARMAC_MIX to their own property driveway.
     * COMFORT_SCORE +5.
     */
    SMOOTH_DRIVEWAY(
        "Smooth Driveway",
        "Laid your own tarmac. It's not perfect but it's yours.",
        1
    ),

    /**
     * Awarded when the player crafts a LUCKY_HEATHER_CROWN from 5× LUCKY_HEATHER.
     */
    HEATHER_ROYALTY(
        "Heather Royalty",
        "Fashioned yourself a crown of lucky heather. You look ridiculous. You feel invincible.",
        1
    ),

    /**
     * Awarded when the player throws CLOTHES_PEG_BUNDLE at 3 different NPCs.
     * Tracks progress (3 unique peg targets).
     */
    PEG_WARFARE(
        "Peg Warfare",
        "Assaulted three people with clothes pegs. Brigid would be proud.",
        3
    ),

    // ── Issue #1315: Prison Van Escape — The Paddy Wagon Hustle ───────────────

    /**
     * Fires on the first successful escape via brute force or lockpick.
     */
    ESCAPE_ARTIST(
        "Doing a Ronnie Biggs",
        "You escaped police custody. The hard way. Possibly the only way.",
        1
    ),

    /**
     * Fires on the first successful bribed escape.
     */
    BOUGHT_MY_WAY_OUT(
        "Creative Accounting",
        "You bribed your way out of a police van. The British justice system at its finest.",
        1
    ),

    /**
     * Fires on the first successful bluff escape.
     */
    SILVER_TONGUE(
        "I Know My Rights",
        "Fast talk got you out of the back of a police van. Probably shouldn't have worked, but here we are.",
        1
    ),

    /**
     * Fires on the third successful escape (any method). Target: 3.
     */
    REPEAT_ESCAPEE(
        "Professional Liability",
        "Third time escaping police custody. They really should just give up.",
        3
    ),

    // ── Issue #1317: Northfield Bonfire Night ─────────────────────────────────

    /**
     * Fires when the player successfully collects at least 1 COIN donation from a
     * PUBLIC or PENSIONER NPC while the GUY_PROP is placed in the park.
     */
    PENNY_FOR_THE_GUY(
        "Penny for the Guy",
        "Crafted a Guy and charmed the public out of their loose change. Every penny counts.",
        1
    ),

    /**
     * Fires when a YOUTH_GANG NPC kicks over the player's GUY_PROP (destroys it)
     * during the Bonfire Night event.
     */
    PARTY_POOPER(
        "Party Pooper",
        "Your Guy got kicked over by a gang of youths. Classic Northfield.",
        1
    ),

    /**
     * Fires when the player plants a BANGER_FIREWORK in FIREWORK_MORTAR_PROP and
     * triggers the catastrophic misfire (Notoriety +8, CRIMINAL_DAMAGE, FIRE_ENGINE response).
     */
    SABOTEUR(
        "Saboteur",
        "You ruined the Tesco car park display. The compère is not happy.",
        1
    ),

    /**
     * Fires when the player launches at least 3 fireworks (any type) during a single
     * Bonfire Night event without triggering a FIREWORK_OFFENCE on the criminal record.
     */
    PYRO_NIGHT(
        "Pyro Night",
        "Three fireworks launched and not a single arrest. Bonfire Night, Northfield style.",
        3
    ),

    // ── Issue #1319: NatWest Cashpoint — The Dodgy ATM ───────────────────────

    /**
     * Fires after the player successfully withdraws from the CASHPOINT_PROP on
     * 7 different in-game days (resets daily at 00:00). Tracks unique calendar days.
     */
    CASHPOINT_REGULAR(
        "Cashpoint Regular",
        "Seven withdrawals on seven different days. You know this machine better than your own mum.",
        7
    ),

    /**
     * Fires on the player's first successful fraudulent withdrawal using
     * STOLEN_PIN_NOTE + VICTIM_BANK_CARD between 22:00–05:00.
     */
    IDENTITY_THIEF(
        "Identity Thief",
        "You used someone else's card. At 3am. At a Northfield cashpoint. Peak.",
        1
    ),

    /**
     * Fires when the player collects 3 or more CLONED_CARD_DATA items in a single
     * skimmer session (one CARD_SKIMMER_DEVICE attachment).
     */
    SKIMMER_KING(
        "Skimmer King",
        "Three cloned cards from one machine. Someone's very busy this evening.",
        1
    ),

    /**
     * Fires on the player's first successful forced entry into an out-of-service
     * CASHPOINT_PROP (using CROWBAR or ANGLE_GRINDER).
     */
    CASH_AND_CARRY(
        "Cash and Carry",
        "You cracked open a cashpoint. Health and Safety would not approve.",
        1
    ),

    /**
     * Fires after the player completes 5 envelope-drop runs for Kenny (MONEY_MULE).
     */
    MONEY_MULE_RUNNER(
        "Money Mule",
        "Five envelope runs for Kenny. You didn't ask what was in them. Wise.",
        5
    ),

    // ── Issue #1329: Northfield Traffic Warden ────────────────────────────────

    /**
     * Fires when the player sells FORGED_PARKING_TICKET to 5 commuters in a single day.
     */
    TICKET_TOUT(
        "Ticket Tout",
        "Five forged parking tickets flogged in one day. Clive would not be impressed.",
        1
    ),

    /**
     * Fires when the player's PCN appeal at APPEAL_DESK_PROP is upheld.
     */
    APPEAL_SUCCESS(
        "Appeal Upheld",
        "You fought the council and the council lost. Briefly.",
        1
    ),

    /**
     * Fires the first time Clive applies a WHEEL_CLAMP_PROP to the player's vehicle.
     */
    CLAMPED(
        "Clamped",
        "Clive has clamped your motor. That's going to ruin your day.",
        1
    ),

    /**
     * Fires when the player removes a wheel clamp without paying the release fee.
     */
    CLAMP_DODGER(
        "Clamp Dodger",
        "You got the clamp off without paying. Probably won't end there.",
        1
    ),

    /**
     * Fires when the player steals Clive's terminal by knocking him out.
     */
    TERMINAL_THIEF(
        "Terminal Thief",
        "You nicked Clive's terminal. He'll have to fill out a form about that.",
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

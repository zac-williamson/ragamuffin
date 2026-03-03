package ragamuffin.core;

/**
 * Types of rumours that NPCs carry and spread through the rumour network.
 * Each type represents a category of gossip about the state of the world.
 */
public enum RumourType {

    /** "Someone's been causing bother near [landmark]" — generated when player commits a crime near a landmark. */
    PLAYER_SPOTTED,

    /** "I heard there's good stuff inside [landmark]" — generated at world-gen time for office/jeweller. */
    LOOT_TIP,

    /** "The [gang name] boys are running [territory name] now" — generated when gang territory reaches HOSTILE. */
    GANG_ACTIVITY,

    /** "You might want to talk to someone at [landmark]" — generated when a quest becomes available. */
    QUEST_LEAD,

    /** "The council's sending someone round about those buildings" — generated when demolition threshold crossed. */
    COUNCIL_NOTICE,

    /** "Heard it's going to [weather] later — dress warm / bring an umbrella" — seeded hourly by TimeSystem, accumulated by the barman. */
    WEATHER_TIP,

    /** "There's a rave on at [location] tonight — should be mental" — seeded when the player uses a FLYER at their squat. Draws nearby NPCs toward the squat as rave attendees. */
    RAVE_ANNOUNCEMENT,

    /** "Saw someone doing [crime] near [location]" — seeded when an NPC witnesses a crime and flees. Causes police NPCs who receive it to investigate the crime location. */
    WITNESS_SIGHTING,

    /** "Someone grassed on [faction/person]" — seeded when the player tips off police with a RUMOUR_NOTE. Turns the tipped faction hostile toward the player. */
    BETRAYAL,

    /** "Developers are moving in — prices are going up round here" — seeded when a Luxury Flat is built by The Council's gentrification wave. */
    GENTRIFICATION,

    /** "There's a new shop on the high street — bit dodgy if you ask me" — seeded when the player's corner shop opens or hits a revenue milestone. */
    SHOP_NEWS,

    /** "Heard the filth got a tip-off about someone causing bother" — seeded by WantedSystem when a witness reports a crime to police. */
    POLICE_TIP,

    /** "Someone just cleaned up at the bookies on a 33/1 shot" — seeded by HorseRacingSystem on a 33/1 win. Draws nearby NPCs toward the bookies for 60 seconds. */
    BIG_WIN_AT_BOOKIES,

    // ── Issue #916: Late-Night Kebab Van ─────────────────────────────────────

    /** "Someone half-inched a kebab from the van last night — they used a tin of beans" — seeded by KebabVanSystem when the player steals food via the TIN_OF_BEANS distraction. */
    THEFT,

    // ── Issue #926: Chippy System ─────────────────────────────────────────────

    /** "Someone jumped the queue outside Tony's — reckons they're above waiting" — seeded by ChippySystem when the player queue-jumps during the post-pub queue (23:00–01:00). */
    QUEUE_JUMP,

    /** "Someone punched Biscuit outside Tony's — Tony's furious" — seeded town-wide by ChippySystem when the player punches the STRAY_CAT NPC. */
    CAT_PUNCH,

    // ── Issue #928: Public Library System ─────────────────────────────────────

    /** "Heard someone got chucked out of the library — librarian's on the warpath" — seeded by LibrarySystem when the player is ejected by the LIBRARIAN NPC after repeat shushing. */
    LIBRARY_BAN,

    // ── Issue #934: Pigeon Racing System ─────────────────────────────────────

    /** "Race day tomorrow — lofts are out in Northfield. Should be a good one." — seeded by PigeonRacingSystem the evening before a race day, or when a race is postponed due to bad weather (+1 day reschedule). */
    PIGEON_RACE_DAY,

    /** "Heard someone's bird won the Northfield Derby yesterday — brought home the trophy an' all." — seeded by PigeonRacingSystem on a NORTHFIELD_DERBY win. Spreads via PIGEON_FANCIER NPCs. */
    PIGEON_VICTORY,

    // ── Issue #942: Food Bank System ─────────────────────────────────────────

    /** "Someone's been doing proper good at the food bank — donated again this week, apparently" — seeded by FoodBankSystem when the player donates surplus food or materials. Seeds into the barman's rumour buffer. */
    COMMUNITY_WIN,

    // ── Issue #950: Northfield Leisure Centre ─────────────────────────────────

    /** Leisure centre changing room gossip — one of 8 pool-specific lines seeded every 60s
     * when the player is within 5 blocks of a CHANGING_ROOM_PROP.
     * Spreads via the regular NPC rumour network. */
    CHANGING_ROOM_GOSSIP,

    // ── Issue #952: Clucky's Fried Chicken ───────────────────────────────────

    /** "There's a scrap kicking off outside Clucky's — someone's getting battered"
     * — seeded by FriedChickenShopSystem when 2–3 YOUTH_GANG NPCs enter FIGHTING_EACH_OTHER
     * state outside the shop. Triggers NOISE_EVENT and police patrol awareness. */
    FIGHT_NEARBY,

    // ── Issue #954: Northfield Bingo Hall ─────────────────────────────────────

    /** "Someone just had a full house at Lucky Stars — walked away with fifteen coin!"
     * — seeded by BingoSystem when the player wins a FULL HOUSE.
     * Spreads via nearby NPCs; draws curious PUBLIC NPCs toward the bingo hall. */
    WINNER,

    // ── Issue #963: Northfield Canal ──────────────────────────────────────────

    /** "Someone chucked something in the canal — right in front of me, an' all"
     * — seeded by CanalSystem when the player disposes of evidence in the water.
     * If a POLICE/WITNESS NPC is nearby the rumour records LITTERING. */
    LITTER,

    // ── Issue #969: Northfield Cemetery ──────────────────────────────────────

    /** "Did you see the funeral today? Poor sod."
     * — seeded by CemeterySystem after each funeral procession ends.
     * Spreads via nearby NPCs within 30 blocks of the cemetery.
     * Marks the freshly covered grave plot as DISTURBED (diggable in 4 hits). */
    FRESH_GRAVE,

    // ── Issue #1138: Northfield Iceland ───────────────────────────────────────

    /** "Someone nicked the Christmas Club money from the Iceland — all of it. Poor Debbie."
     * — seeded by IcelandSystem when the player steals the CHRISTMAS_CLUB_CASH_BOX
     *   or a CHRISTMAS_ENVELOPE from another customer.
     * Spreads to all PUBLIC and PENSIONER NPCs within 40 blocks of the Iceland.
     * Triggers CHRISTMAS_CLUB_VILLAIN achievement unlock. Raises police patrol awareness. */
    LOCAL_SCANDAL,

    // ── Issue #971: The Rusty Anchor Wetherspoons ──────────────────────────────

    /** "There's something on tonight down [location] — worth a look" — seeded by
     * WetherspoonsSystem on quiz night start and Thursday curry club.
     * Gary at the Rusty Anchor seeds this type more frequently than GANG_ACTIVITY. */
    LOCAL_EVENT,

    /** "Round here, there's been a bit of bother with [topic]" — neighbourhood
     * gossip seeded by Gary (BARMAN) at The Rusty Anchor.
     * More common from Gary than GANG_ACTIVITY rumours. */
    NEIGHBOURHOOD,

    /** "Someone got proper kicked out of the pub last night — all hell broke loose"
     * — seeded by WetherspoonsSystem when the KICKED_OUT atmospheric event fires.
     * Spreads via NPCs within 30 blocks of The Rusty Anchor. */
    TROUBLE_AT_PUB,

    // ── Issue #975: Northfield Post Office ────────────────────────────────────

    /** "Heard someone's been nicking parcels off doorsteps — broad daylight an' all"
     * — seeded by PostOfficeSystem on PARCEL_THEFT when a witness is present.
     * Spreads via nearby NPCs; increases police patrol awareness in residential areas. */
    PARCEL_THEFT_SPOTTED,

    /** "Maureen at the post office caught someone trying to cash a stolen benefits book.
     * Phoned the police herself, she did."
     * — seeded by PostOfficeSystem on BENEFITS_FRAUD detection.
     * Spreads via PENSIONER NPCs who recognise the fraud pattern. */
    BENEFITS_FRAUD_CAUGHT,

    /** "Someone round here's been sending nasty letters. Police are asking questions."
     * — seeded by PostOfficeSystem when a threatening letter is traced at Tier 3+.
     * Spreads town-wide; increases WATCH_MEMBER vigilance. */
    THREATENING_LETTER,

    // ── Issue #981: Council Estate ────────────────────────────────────────────

    /** "Someone's been hammering on doors up on the estate at all hours."
     * — seeded by CouncilFlatsSystem when the player knocks on a resident door 3+
     * times after 22:00. Triggers NeighbourhoodWatchSystem +5 anger. */
    NOISE_COMPLAINT,

    // ── Issue #983: Northfield Dog Track ─────────────────────────────────────

    /** "Heard a dog went missing from the track kennels last night — Marchetti's not happy."
     * — seeded by GreyhoundRacingSystem when the player successfully steals a GREYHOUND.
     * Spreads via TRACK_PUNTER and KENNEL_HAND NPCs; increases SECURITY_GUARD patrols for
     * one session. Raises Marchetti Crew hostility by -5 Respect. */
    DOG_HEIST,

    /** "Someone fixed the dogs down the track — third trap never had a chance."
     * — seeded by GreyhoundRacingSystem when a race fix (DODGY_PIE or bribery) is witnessed.
     * Spreads via TRACK_PUNTER NPCs; triggers SECURITY_GUARD alert state.
     * If it reaches police: RACE_FIXING added to criminal record. */
    RACE_FIXED,

    // ── Issue #985: Northfield Police Station ─────────────────────────────────

    /** "Someone grassed to the station — pointed the finger at [faction]."
     * — seeded by PoliceStationSystem when the player uses the Tip Off menu.
     * NPCs carrying this rumour use it against the player: "I heard you grassed 'em up."
     * Turns the named faction hostile toward the player. */
    GRASSED_UP,

    // ── Issue #1213: Northfield Police Station — Desk Sergeant Bribe ─────────

    /** "Word is someone's been greasing palms down the nick — that desk sergeant's on the take."
     * — seeded by PoliceStationSystem when the player successfully bribes the DESK_SERGEANT.
     * Spreads via PUBLIC and STREET_LADS NPCs near the police station.
     * Raises Marchetti Crew respect +2 (they appreciate a bent copper); no police escalation
     * unless rumour reaches a POLICE NPC (20% chance of +1 Wanted star). */
    POLICE_CORRUPTION,

    /** "Someone broke out of the nick — lockpicked the cell door and legged it."
     * — seeded by PoliceStationSystem on a successful cell breakout.
     * Positive reputation with STREET_LADS (+5 Respect).
     * Spreads via PUBLIC NPCs near the police station. */
    GREAT_ESCAPE_RUMOUR,

    // ── Issue #998: Northfield Aldi Supermarket ────────────────────────────────

    /** "Someone knocked a trolley over down Aldi — Dave had to go and sort it out."
     * — seeded by SupermarketSystem when the player punches a SHOPPING_TROLLEY prop.
     * Adds +5 Notoriety; diverts SECURITY_GUARD Dave for 15 seconds. */
    VANDALISM,

    /** "Someone reckons there's a gold trolley down Aldi car park come closing."
     * — seeded by SupermarketSystem the night before the golden trolley spawns.
     * Spreads to 2 nearby NPCs via RumourNetwork. */
    URBAN_LEGEND,

    /** "Someone just legged it out of Aldi with a full basket — Dave went after 'em."
     * — seeded by SupermarketSystem when the player punches Dave (SECURITY_GUARD).
     * Spreads to 3 nearby NPCs; triggers WantedSystem stars and Notoriety gain. */
    ASSAULT,

    // ── Issue #1000: Northfield Fire Station ─────────────────────────────────

    /** "Someone only went and nicked the actual fire engine from the station — full flashing lights and all."
     * — seeded by FireStationSystem when the player completes the Engine Heist.
     * Spreads to 5 nearby NPCs via RumourNetwork. Raises FIREFIGHTER suspicion permanently.
     * Police NPCs who receive it immediately raise WantedSystem by +1 star. */
    MAJOR_THEFT,

    // ── Issue #1022: Northfield GP Surgery ────────────────────────────────────

    /** "Doctor Nair reckons half the street's got stress-related conditions — not surprised, to be honest."
     * — seeded weekly by GPSurgerySystem from Dr. Nair.
     * Spreads via GP_PATIENT and PUBLIC NPCs near the surgery.
     * Soft narrative flavour; no gameplay effect beyond atmosphere. */
    LOCAL_HEALTH,

    // ── Issue #1030: Al-Noor Mosque ────────────────────────────────────────────

    /** "Someone robbed the mosque collection box. Absolute disgrace."
     * — seeded by MosqueSystem when the player destroys the TAKINGS_BOX_PROP.
     * Spreads to all NPCs within 50 blocks. Raises hostility toward the player.
     * Contributes to LOWEST_OF_THE_LOW achievement unlock. */
    COMMUNITY_OUTRAGE,

    /** "The mosque is doing an Iftar tonight — free food for everyone, come along."
     * — seeded by MosqueSystem at Maghrib during Ramadan when the FOLD_TABLE_PROP is placed.
     * Spreads to nearby NPCs; draws WORSHIPPER and PUBLIC NPCs toward the mosque. */
    IFTAR_TONIGHT,

    // ── Issue #1033: St. Aidan's C.E. Primary School ─────────────────────────

    /** "Apparently a kid from St. Aidan's was seen bunking off near the chicken shop." */
    TRUANCY,

    /** "Some bloke was hanging round the school today — looked proper dodgy." */
    SUSPICIOUS_PERSON,

    /** "That dinner lady at St. Aidan's got robbed. Bold as brass." */
    BANNED_FROM_CANTEEN,

    // ── Issue #1037: Northfield Indoor Market ─────────────────────────────────

    /** "Trading Standards turned up at the indoor market — Mo legged it out the back."
     * — seeded by IndoorMarketSystem during a Trading Standards raid.
     * Spreads only via MARKET_PUNTER and MARKET_TRADER NPCs at the indoor market.
     * Distinct from POLICE_ACTIVITY: specifically market-origin and carries contraband
     * context. Police NPCs who receive it add +1 patrol awareness near INDOOR_MARKET. */
    MARKET_RAID,

    // ── Issue #1039: Northfield Barber ────────────────────────────────────────

    /** "Someone jumped the queue at Kosta's — first in, first served, apparently not."
     * — seeded by BarberSystem when the player calls {@code attemptQueueJump()}.
     * The offended waiting NPC seeds this; spreads to any NPC within 8 blocks.
     * Minor hostility increase to PUBLIC NPCs nearby. */
    ANTISOCIAL_BEHAVIOUR,

    // ── Issue #1047: Northfield BP Petrol Station ──────────────────────────────

    /** "Someone drove off from the BP without paying — Dave was fuming."
     * — seeded by PetrolStationSystem when PETROL_THEFT is committed.
     * Spreads to NPCs within 20 blocks of the petrol station.
     * Increases patrol awareness near PETROL_STATION landmark. */
    CRIME_SPOTTED,

    /** "That pump on the BP forecourt was frozen solid again this morning."
     * — seeded by PetrolStationSystem when the player encounters a frozen nozzle during FROST.
     * Spreads to JOGGER and PUBLIC NPCs passing the forecourt.
     * No gameplay effect; minor flavour rumour. */
    WEATHER_GRUMBLE,

    // ── Issue #1051: Angel Nails & Beauty ─────────────────────────────────────

    /** "Tracy from the nail salon was saying [detail] about [location/person]."
     * — seeded by NailSalonSystem at 09:00 daily. Spreads via seated PUBLIC and PENSIONER NPCs.
     * Higher spread rate than NEIGHBOURHOOD (salons are the original social media). */
    SALON_GOSSIP,

    /** "Heard there's a shipment coming in — something dodgy, by the sounds of it."
     * — seeded by NailSalonSystem when Trang shares intel with a player at Street Rep ≥ 20.
     * Spreads via GANG_MEMBER and STREET_LAD NPCs within 15 blocks. */
    CONTRABAND_SHIPMENT,

    /** "Someone spotted doing [crime] near [location] — looked well dodgy."
     * — seeded by NailSalonSystem when a crime sighting is reported in the vicinity.
     * Spreads via PUBLIC and PENSIONER NPCs; increases police patrol awareness. */
    CRIME_SIGHTING,

    // ── Issue #1077: Northfield Chinese Takeaway — Golden Palace ─────────────

    /** "Someone's feeding prawn crackers to the pigeons outside the Golden Palace — they've gone absolutely mental."
     * — seeded by ChineseTakeawaySystem when the player feeds {@code PRAWN_CRACKERS} to 3+ {@code BIRD} NPCs.
     * Spreads to NPCs within 15 blocks. Minor Notoriety gain (+1). */
    PIGEON_CHAOS,

    // ── Issue #1081: Northfield Pet Shop & Vet — Paws 'n' Claws ─────────────

    /** "Have you seen that lovely dog that fella's walking? Proper sweet."
     * — seeded by PetShopSystem when a PENSIONER NPC admires the player's dog companion.
     * Spreads via PENSIONER NPCs. Seeds a LOCAL_EVENT rumour; no gameplay effect. */
    PET_ADMIRATION,

    /** "Someone got rushed into the vet with their dog — looked in a right state."
     * — seeded by PetShopSystem after a dog emergency consultation at Northfield Vets.
     * Spreads to PUBLIC NPCs within 20 blocks. Minor police patrol increase near VET_SURGERY. */
    VET_EMERGENCY,

    /** "Heard someone nicked a posh dog from the school playground — right in front of the lollipop lady."
     * — seeded by PetShopSystem when the dodgy pedigree breeding mission is completed.
     * Spreads via YOUTH_GANG and STREET_LAD NPCs. Adds +3 patrol awareness near PRIMARY_SCHOOL. */
    PEDIGREE_THEFT,

    // ── Issue #1091: Northfield Nando's ──────────────────────────────────────

    /** "Lads on a stag do at Nando's invited everyone to The Vaults tonight — free entry token an' all."
     * — seeded by NandosSystem on Saturdays 12:00–15:00 when the Stag Do event fires and
     * the player receives a free-entry invitation.
     * Spreads via DRUNK and PUBLIC NPCs in the restaurant. Draws NPCs toward The Vaults.
     * Grants a free-entry token at The Vaults nightclub when received. */
    VAULTS_PARTY,

    // ── Issue #1094: Northfield By-Election ──────────────────────────────────

    /** "Reckon there's a by-election on. Northfield Ward. Proper local drama this time."
     * — seeded by ByElectionSystem on Day 7 (or Notoriety Tier 2) when the election is called.
     * Spreads to all PUBLIC and PENSIONER NPCs within 30 blocks of the town centre.
     * Increases canvassing NPC foot traffic during campaign days. */
    ELECTION_CALLED,

    /** "Heard someone's been tearin' down the election posters round here. Proper vandal."
     * — seeded by ByElectionSystem when the player destroys 3 or more election posters.
     * Spreads via NEIGHBOURHOOD_WATCH members and PENSIONER NPCs.
     * Adds +3 patrol awareness near polling station and canvassing areas. */
    POSTER_VANDAL,

    // ── Issue #1110: Skin Deep Tattoos ────────────────────────────────────────

    /** "There's some dodgy bloke setting up outside The Vaults doing tats for a fiver — I'd avoid it."
     * — seeded by TattooParlourSystem when Spider the rival tattooist is active on Saturday
     *   and the player tips off Kev. Spreads via PUBLIC and STREET_LADS NPCs.
     *   Seeds a follow-up brawl event between Kev and Spider at 15:00. */
    LOCAL_DISPUTE,

    /** "Saw someone in the precinct absolutely covered in ink — looked proper hard."
     * — seeded by TattooParlourSystem when the player applies a PRISON_INK buff
     *   (either at Kev's or DIY). Spreads via PUBLIC and YOUTH_GANG NPCs.
     *   Minor Notoriety gain (+1) for nearby witnesses. */
    STREET_REPUTATION,

    // ── Issue #1134: Patel's Newsagent ────────────────────────────────────────

    /** "Did you hear? There was a bit of bother round the back of the newsagent last night."
     * — seeded by INSOMNIAC_PENSIONER Norman outside Patel's News between 05:00 and 08:00.
     *   Spreads via PUBLIC and PENSIONER NPCs.
     *   Minor ambient flavour; no direct gameplay effect. */
    LOCAL_GOSSIP,

    // ── Issue #1146: Mick's MOT & Tyre Centre ────────────────────────────────

    /** "Heard someone got a dodgy MOT sorted over at Mick's — car's an absolute death trap."
     * — seeded by GarageSystem when a JOURNALIST NPC is within 20 blocks during a Dodgy MOT
     *   or when the 15% tip-off risk fires during Car Ringing.
     * Spreads via JOURNALIST and PUBLIC NPCs.
     * Seeds a NewspaperSystem headline and reduces Marchetti Respect −5 on fire. */
    DODGY_PAPERWORK,

    /** "Someone nicked Mick's petty cash. Broad daylight an' all."
     * — seeded by GarageSystem when CROWBAR is used on CASH_TIN_PROP (noisy heist) and
     *   any NPC is within 12 blocks.
     * Spreads via PUBLIC NPCs near the garage.
     * Minor Notoriety spike (+2) for player; no police escalation unless Wanted ≥ 1. */
    GARAGE_THEFT,

    // ── Issue #1148: Northfield Council Estate Lock-Up Garages ───────────────

    /** "Someone's been breaking into the garages on the Northfield Estate."
     * — seeded by LockUpGarageSystem when CROWBAR break-in occurs (HIGH noise, 25-block radius)
     *   and any NPC is within 25 blocks. Also seeded if Dave the Caretaker witnesses a break-in.
     * Spreads via PUBLIC and PENSIONER NPCs on the estate.
     * Minor Notoriety spike (+3) for player; triggers WantedSystem TRESPASSING offence if Dave witnesses. */
    LOCK_UP_BREAK_IN,

    /** "Heard there's been a police raid over at the garages. Drug thing, apparently."
     * — seeded by LockUpGarageSystem after UNDERCOVER_POLICE raid on Garage 3 completes.
     * Spreads via PUBLIC, JOURNALIST, and BARMAN NPCs.
     * Reduces MARCHETTI_CREW Respect -10 on fire; no direct player impact unless found inside. */
    GARAGE_DRUG_RAID,

    /** "There's a band practising in one of them lock-ups on the estate. Bit loud, like."
     * — seeded by LockUpGarageSystem on first band rehearsal session (Garage 1, Tues/Thurs/Sat 19:00).
     * Spreads via PUBLIC and YOUTH_GANG NPCs near the estate.
     * Ambient flavour; directs curious players to COUNCIL_GARAGES landmark. */
    GARAGE_BAND_RUMOUR,

    /** "Old Barry's finally letting go of all that stuff in his garage. Clearance sale, like."
     * — seeded by LockUpGarageSystem when the hoarder clearance quest is activated (Garage 2).
     * Spreads via PENSIONER and PUBLIC NPCs.
     * Directs player to Garage 2 for the BRIC_A_BRAC_BANDIT quest chain. */
    HOARDER_CLEARANCE,

    // ── Issue #1153: Northfield Community Centre ──────────────────────────────

    /** "Someone at the community centre had their photocopier going mad — heard it from the road."
     * — seeded by CommunityCentreSystem when the player uses the PHOTOCOPIER_PROP to forge a grant
     *   application (HIGH noise, 15-block radius).
     * Spreads via PUBLIC and PENSIONER NPCs within 15 blocks of the community centre.
     * Minor Notoriety spike (+2) if any NPC within radius. */
    COMMUNITY_CENTRE_PHOTOCOPIER,

    /** "Heard someone's been fiddling the council grant forms at the community centre."
     * — seeded by CommunityCentreSystem when a FORGED_GRANT_APPLICATION is posted
     *   and the 25% catch rate fires (×2 at Notoriety Tier 3+).
     * Spreads via PUBLIC NPCs; triggers MagistratesCourtSystem OBTAINING_MONEY_BY_DECEPTION charge. */
    GRANT_FRAUD,

    /** "Brenda said someone came to the NA meeting who's been in all sorts of bother."
     * — seeded by CommunityCentreSystem from Brenda (NA_CHAIR) after a player share-story
     *   event when Notoriety ≥ 30. 15% chance to fire.
     * Spreads via NA_ATTENDEE NPCs. */
    NA_RECOGNITION,

    /** "Curry night at the community centre Saturday — best food in Northfield for two coin."
     * — seeded weekly by CommunityCentreSystem at 17:00 on Saturdays before curry night.
     * Spreads via COMMUNITY_MEMBER and PUBLIC NPCs.
     * Draws nearby NPCs toward the COMMUNITY_CENTRE landmark. */
    CURRY_NIGHT,

    // ── Issue #1161: Northfield Poundstretcher ────────────────────────────────

    /** "Sharon's banned someone from the Poundstretcher — saw it happen myself."
     * — seeded by PoundShopSystem when a player is caught shoplifting by Sharon.
     * Spreads via PUBLIC and PENSIONER NPCs within 20 blocks of the POUND_SHOP.
     * Adds +2 patrol awareness near POUND_SHOP. */
    SHOPLIFTING_BAN,

    // ── Issue #1165: Northfield Match Day ────────────────────────────────────

    /** "It all kicked off outside the Rusty Anchor — absolute carnage."
     * — seeded by MatchDaySystem when STREET_BRAWL_EVENT fires.
     * Spreads through pub NPCs (barmen, HOME_FAN) within 30 in-game minutes.
     * NeighbourhoodWatchSystem logs this rumour (vibes −3). */
    MATCH_DAY_TROUBLE,

    /** "There's police horses up by the Anchor — best keep your head down."
     * — seeded by MatchDaySystem when POLICE_HORSE_OFFICER spawns.
     * Reduces NPC crime willingness by 10% for the duration.
     * Spreads via PUBLIC and AWAY_FAN NPCs. */
    POLICE_PRESENCE,

    // ── Issue #1167: Northfield Amateur Boxing Club ───────────────────────

    /** "That lad from Tommy's Gym — proper fighter. Word is he's got real talent."
     * — seeded by BoxingClubSystem when the player wins a Friday Night Fight.
     * Spreads via BOXING_AMATEUR and BOXING_COACH NPCs; boosts STREET_LADS Respect +3. */
    STREET_TALENT,

    /** "Heard there's an underground fight night on Saturday — serious money involved."
     * — seeded by BoxingClubSystem when the white-collar circuit unlocks (2nd win).
     * Spreads via FIGHT_PROMOTER and BOXING_PROSPECT NPCs within 20 blocks of BOXING_CLUB.
     * Draws WHITE_COLLAR_BOXER NPCs toward the gym on alternate Saturdays. */
    UNDERGROUND_FIGHT,

    // ── Issue #1192: Northfield Sporting & Social Club ────────────────────────

    /** "Mick at the social club back room was dealing from the bottom of the deck."
     * — seeded by SportingSocialClubSystem when player catches CARD_DEALER cheating.
     * Spreads via SOCIAL_CLUB_STEWARD and CLUB_REGULAR NPCs.
     * Reduces MARCHETTI_CREW Respect by 5 (they run Mick). */
    CARD_CHEAT,

    // ── Issue #1196: Environmental Health Officer ─────────────────────────────

    /** "That [venue] got a one-star hygiene rating — I'm never eating there again."
     * — seeded by EnvironmentalHealthSystem on 1–2 star inspection outcomes and
     * on forgery detection. Reduces NPC footfall at the affected venue by 30%
     * for 3 in-game days. Spreads via PUBLIC and PENSIONER NPCs. */
    FOOD_HYGIENE,

    /** "Council environmental health have been round — something's going on."
     * — seeded on Janet assault or failed bribe within 15-block radius.
     * Spreads via PUBLIC and PENSIONER NPCs. */
    COUNCIL_ENFORCEMENT,

    // ── Issue #1220: BettingShopSystem ────────────────────────────────────────

    /** "Someone's got a proper dodgy deal going down at the bookies — Marchetti
     * lot are involved."
     * — seeded by BettingShopSystem when player accepts Marchetti race fix.
     * Spreads via STREET_LAD and FACTION_LIEUTENANT NPCs. */
    DODGY_DEAL,

    // ── Issue #1237: Northfield St. Aidan's Primary School ───────────────────

    /** "Did you hear? There's a stranger hanging around outside St. Aidan's again."
     * — seeded by SCHOOL_MUM NPCs during the school run (08:15–08:45, 15:00–15:30).
     * Spreads via SCHOOL_MUM and PUBLIC NPCs; adds +3 patrol awareness near PRIMARY_SCHOOL.
     * Also seeded when inspectors flee: "Ofsted legged it — something went off at the school." */
    NEIGHBOURHOOD_GOSSIP,

    /** "Someone's selling snacks to the kids out the back of St. Aidan's at lunch."
     * — seeded by SCHOOL_KID NPCs when the player's tuck shop bandit count reaches 3.
     * Spreads via SCHOOL_KID and SCHOOL_MUM NPCs; alerts Ms. Pearson patrol. */
    SCHOOL_CONTRABAND,

    /** "Ofsted are in St. Aidan's today — teachers are losing the plot."
     * — seeded by PrimarySchoolSystem on Monday morning inspection trigger.
     * Spreads via SCHOOL_MUM and PUBLIC NPCs within 30 blocks of PRIMARY_SCHOOL. */
    OFSTED_VISIT,

    // ── Issue #1243: Northfield Bert's Tyres & MOT ───────────────────────────

    /** "Bert down the industrial estate — does MOTs on anything. No questions asked."
     * — seeded by MOTSystem when the player receives a FAIL_ON_PURPOSE outcome.
     * Spreads via STREET_LAD and PUBLIC NPCs within 40 blocks of BERTS_GARAGE.
     * Raises Bert's corruption score +5 when heard by other DODGY_MECHANIC NPCs. */
    MOT_SCAM,

    /** "DVSA were round Bert's garage — shut the place down for the afternoon."
     * — seeded by MOTSystem on DVSA_INSPECTOR raid completion.
     * Spreads via PUBLIC and PENSIONER NPCs within 30 blocks of BERTS_GARAGE.
     * Reduces Bert's corruption by 10 (scared straight) for 3 in-game days. */
    DVSA_RAID,

    /** "That bloke at Bert's is bent as a nine-bob note. Got a certificate off him,
     *  no inspection at all."
     * — seeded by MOTSystem when PASS_BRIBE outcome occurs for the first time.
     * Spreads via STREET_LAD and TYRE_KICKER NPCs. Adds +3 WatchAnger.
     * Adds BERTS_GARAGE to the DVSA_INSPECTOR's patrol route. */
    BENT_GARAGE,

    // ── Issue #1252: Northfield TV Licensing ──────────────────────────────────

    /** "The detector van's out — better get your licence sorted."
     * — seeded by TvLicensingSystem when the DETECTOR_VAN_PROP spawns on the industrial
     * estate street every Sunday 14:00–16:00.
     * Spreads via PUBLIC and PENSIONER NPCs; panics unlicensed NPCs into fleeing.
     * Atmospheric only — the van cannot actually detect anything. */
    DETECTOR_VAN_SPOTTED,

    // ── Issue #1257: Northfield Rag-and-Bone Man ──────────────────────────────

    /** "Barry got nicked — council took his van. No rag-and-bone round this week."
     * — seeded by RagAndBoneSystem when COUNCIL_ENFORCEMENT impounds the RAG_AND_BONE_VAN.
     * Spreads via PUBLIC and PENSIONER NPCs within 30 blocks of the last known stop.
     * Barry despawns for 48 in-game hours; RIVAL_RAGBONE_MAN Terry takes over. */
    BARRY_NICKED,

    // ── Issue #1259: Northfield Pub Quiz Night ────────────────────────────────

    /** "Someone cleaned up at quiz night — answered every question right. Derek was fuming."
     * — seeded by PubQuizSystem when the player wins Quiz Night with a perfect or winning score.
     * Spreads via PUBLIC and BARMAN NPCs within the pub and nearby streets. */
    QUIZ_CHAMPION_RUMOUR,

    // ── Issue #1263: Northfield Illegal Street Racing ──────────────────────────

    /** "Street racing on the ring road again tonight. Boy racers out in force."
     * — seeded by StreetRacingSystem when the meet assembles at 23:00 on Friday/Saturday.
     * Spreads via YOUTH_GANG, BOY_RACER, and PUBLIC NPCs within 50 blocks of the Tesco car park.
     * Adds +1 WatchAnger and draws police patrol route towards the ring road. */
    STREET_RACING_MEET,

    /** "Someone grassed up the racing — plod came down hard. Shane's not happy."
     * — seeded by StreetRacingSystem when the player tips off police via PHONE_BOX_PROP.
     * Spreads via BOY_RACER and RACE_ORGANISER NPCs; flags the player as a snitch.
     * Seeds GRASSED_UP concurrently for gang-level repercussion tracking. */
    RACING_GRASSED_UP,

    /** "Some dodgy business at the racing last night — someone messed with the nitrous."
     * — seeded by StreetRacingSystem on successful sabotage (SCREWDRIVER on competitor car).
     * Spreads via BOY_RACER NPCs within the meet. Player identified if caught by car owner. */
    RACING_SABOTAGE,

    // ── Issue #1265: Northfield Loan Shark — Big Mick's Doorstep Lending ─────────

    /** "Big Mick's lads are looking for someone who owes him — seen them round the estate."
     * — seeded by LoanSharkSystem when a loan becomes overdue (first missed repayment day).
     * Spreads via PUBLIC and PENSIONER NPCs. Causes DEBT_COLLECTOR to follow the player. */
    LOAN_SHARK_LOOKING_FOR_YOU,

    /** "Heard someone paid Big Mick back in full — he's moved on to the next mug."
     * — seeded by LoanSharkSystem when the player fully repays the loan (principal + interest).
     * Spreads via PUBLIC NPCs; despawns DEBT_COLLECTOR. */
    LOAN_PAID_OFF,

    /** "Big Mick's been sniffing round the benefits office again — looking for someone skint."
     * — seeded by DWPSystem on benefit payment day when the player has an outstanding loan.
     * Spreads via PUBLIC and JOB_CENTRE_CLERK NPCs within 40 blocks of the JobCentre. */
    BIG_MICK_DEBT,

    // ── Issue #1269: Northfield BT Phone Box ──────────────────────────────────

    /** "Someone grassed — police showed up looking for someone round [location]."
     * — seeded by PhoneBoxSystem when the player makes an anonymous tip-off call.
     * Spreads via PUBLIC and PENSIONER NPCs; nearby NPCs whisper "someone grassed". */
    POLICE_TIP_OFF,

    /** "Someone's been making calls from the phone box — Marchetti crew were seen nearby."
     * — seeded by PhoneBoxSystem after a successful dead-drop call.
     * Spreads via PUBLIC NPCs; Marchetti crew becomes temporarily wary (reduced patrol radius). */
    MARCHETTI_CONTACT,

    /** "That old phone box on the estate — someone actually fixed it."
     * — seeded by PhoneBoxSystem when the player repairs PHONE_BOX_ESTATE.
     * Spreads via PUBLIC and PENSIONER NPCs; marks the player as a local community figure. */
    PHONE_BOX_REPAIR,

    // ── Issue #1273: Northfield Fly-Tipping Ring ──────────────────────────────

    /** "Someone's been having a bonfire behind the garages again."
     * — seeded by FlyTippingSystem when the player burns a fly-tip load near a
     * CAMPFIRE_PROP or WHEELIE_BIN_FIRE_PROP. Spreads via PUBLIC NPCs within 30 blocks.
     * Triggers NoiseSystem investigation by nearby POLICE. */
    BONFIRE_BEHIND_GARAGES,

    /** "Council cleared that pile down by the canal — bout time."
     * — seeded by FlyTippingSystem when a FLY_TIP_PILE_PROP is removed (paid fine or decayed).
     * Spreads via PUBLIC and PENSIONER NPCs; adds +1 NeighbourhoodSystem Vibes. */
    FLY_TIP_CLEARED,

    /** "Someone's been dumping rubbish on the wasteland again — it's disgusting."
     * — seeded by FlyTippingSystem on the first fly-tip each in-game day.
     * Spreads via PUBLIC, PENSIONER, COUNCIL_MEMBER NPCs; may trigger NeighbourhoodWatchSystem complaint. */
    FLY_TIP_SPOTTED,

    // ── Issue #1276: Northfield Minicab Office — Big Terry's Cabs ─────────────

    /** "Big Terry's boys are nicking A1's trade."
     * — seeded by MinicabSystem when the player touts for fares within 10 blocks of
     * the A1 Taxis rank. Spreads via PUBLIC NPCs; puts TaxiSystem on alert. */
    TURF_WAR,

    // ── Issue #1278: Northfield Travelling Fairground ─────────────────────────

    /** "The fair's in town this weekend — park's east end. Dodgems, waltzers, the lot."
     * — seeded by FairgroundSystem when the fair opens (Friday 18:00).
     * Spreads via PUBLIC and YOUTH_GANG NPCs; draws foot traffic toward TRAVELLING_FAIR. */
    FAIR_IN_TOWN,

    /** "Someone got nicked at the fair for rigging the ring toss — always knew it was bent."
     * — seeded by FairgroundSystem when RIGGED_GAME is recorded in CriminalRecord.
     * Spreads via PUBLIC NPCs within 30 blocks; adds +1 patrol awareness near TRAVELLING_FAIR. */
    RIGGED_GAME_EXPOSED,

    /** "Someone stripped the generator at the fair Sunday night — Big Lenny's fuming."
     * — seeded by FairgroundSystem when DIESEL_GENERATOR_PROP is stripped for scrap (Sunday 23:00+).
     * Spreads via PUBLIC and STREET_LAD NPCs; FAIRGROUND_BOSS becomes hostile for 24h. */
    GENERATOR_STRIPPED,

    // ── Issue #1288: Northfield Sporting & Social Club ────────────────────────

    /** "Mick at the social club back room was dealing from the bottom of the deck."
     * — seeded by SportingSocialClubSystem when player catches CARD_DEALER cheating.
     * Spreads via SOCIAL_CLUB_STEWARD and CLUB_REGULAR NPCs.
     * Reduces MARCHETTI_CREW Respect by 5 (they run Mick). */
    COMMITTEE_CONSPIRACY,

    /** "Someone grassed on the collection at the social club — Tommy's not happy."
     * — seeded by SportingSocialClubSystem when player informs police about the
     * protection envelope handover. Spreads via CLUB_REGULAR NPCs.
     * Bans the player from the social club for 14 in-game days. */
    POLICE_SNITCH,

    // ── Issue #1282: Northfield Day & Night Chemist ───────────────────────────

    /** "Someone did over the chemist — ripped the drug safe right out. Police everywhere."
     * — seeded by PharmacySystem when the DRUG_SAFE_PROP is crowbarred.
     * Spreads via PUBLIC and PENSIONER NPCs; NeighbourhoodSystem Vibes −4;
     * NewspaperSystem headline triggered. */
    PHARMACY_RAID,

    // ── Issue #1286: Northfield Cash Converters ───────────────────────────────

    /** "Someone knocked over Cash Converters last night — Dean's doing his nut."
     * — seeded by CashConvertersSystem on BURGLARY (break-in with crowbar).
     * Spreads via PUBLIC, PENSIONER, and STREET_LAD NPCs; NeighbourhoodSystem Vibes −3;
     * police patrol frequency +1 in that zone for 24h. */
    CASH_CONVERTERS_RAIDED,

    /** "Some lad's dealing out the back of Cash Converters after dark — laptops and that."
     * — seeded when Dave the Middleman completes 3+ transactions.
     * Spreads via STREET_LAD and YOUTH_GANG NPCs; StreetReputation FENCE +1 if player
     * involved; WantedSystem check trigger for Notoriety ≥ 30. */
    BACK_ALLEY_TRADE,

    // ── Issue #1289: Northfield Meredith & Sons Funeral Parlour ───────────────

    /** "Someone's been nicking things out of the viewing room at the funeral parlour —
     * right out of the caskets, apparently."
     * — seeded by FuneralParlourSystem after a witnessed casket theft.
     * Spreads via PUBLIC, MOURNER, and PENSIONER NPCs; NeighbourhoodSystem Vibes −2;
     * WantedSystem check trigger for Notoriety ≥ 20. */
    FUNERAL_THIEF,

    /** "Gerald at the funeral parlour's been paying good money for old war medals —
     * something to do with the gold in the teeth, apparently."
     * — seeded by FuneralParlourSystem when player sells a WAR_MEDAL to Gerald.
     * Spreads via STREET_LAD, PUBLIC, and PENSIONER NPCs; BootSale fence value +1
     * for WAR_MEDAL; PawnShop tip unlocked. */
    GOLD_TEETH_TRADE,

    // ── Issue #1291: Northfield Bert's Tyres & MOT ───────────────────────────

    /** "Someone's been nicking tyres out the back of Bert's — half-worn ones, going cheap."
     * — seeded by MOTSystem when the player steals from TYRE_STACK_PROP or sells
     * STOLEN_TYRE to Bert. Spreads via STREET_LAD and PUBLIC NPCs; pawn shop tip
     * for STOLEN_TYRE unlocked; FenceSystem STOLEN_TYRE price reduced −1. */
    STOLEN_GOODS_TRADE,

    /** "Someone tipped Bert off about the DVSA — he had an hour to hide the dodgy certs."
     * — seeded by MOTSystem when the player warns Bert about an incoming DVSA raid
     * (Respect STREET_LADS ≥ 30). Spreads via STREET_LAD and DODGY_MECHANIC NPCs;
     * adds +5 STREET_LADS Respect for the player; prevents raid invalidation. */
    BERT_WARNED,

    /** "Someone's been doing catalytic converters round here — three cars in a week."
     * — seeded by MOTSystem after the player steals 3+ CATALYTIC_CONVERTER items.
     * Spreads via PUBLIC and PENSIONER NPCs; triggers NewspaperSystem headline;
     * WantedSystem patrol frequency +1 in BERTS_GARAGE zone. */
    CATALYTIC_THEFT_SPREE,

    /** "Bert's been putting stickers on cars that wouldn't pass a look-round, never mind
     * a proper test — someone's going to get hurt."
     * — seeded by MOTSystem when a PASS_BRIBE cert is issued. Spreads via PUBLIC
     * and PENSIONER NPCs; NeighbourhoodSystem Vibes −2;
     * WantedSystem check trigger for Notoriety ≥ 25. */
    UNROADWORTHY,

    // ── Issue #1293: Compensation Kings — ClaimsManagementSystem ─────────────

    /** "Heard someone got a nice little payout from Compensation Kings — no-win no-fee, they said."
     * — seeded by ClaimsManagementSystem on a successful claim payout.
     * Spreads via PUBLIC and STREET_LAD NPCs within 30 blocks of CLAIMS_MANAGEMENT.
     * Draws curious NPCs toward Compensation Kings for 60 seconds. */
    INSURANCE_FRAUD,

    /** "Someone round here's been filing dodgy injury claims — Compensation Kings are getting known."
     * — seeded by ClaimsManagementSystem when the player exceeds 3 claims in 7 in-game days.
     * Spreads via PUBLIC, PENSIONER, and STREET_LAD NPCs.
     * Triggers INSURANCE_INVESTIGATOR spawn probability increase +20%. */
    FRAUDULENT_CLAIMANT,

    /** "Word is Gary at Compensation Kings got a little sweetener — all smoothed over, apparently."
     * — seeded by ClaimsManagementSystem when the player pays the 10-COIN smooth-over bribe
     * to Gary to reset the fraud threshold.
     * Spreads via STREET_LAD and PUBLIC NPCs; MARCHETTI_CREW Respect +2. */
    SMOOTH_OVER,

    // ── Issue #1299: Northfield Street Chuggers ──────────────────────────────

    /** "Heard someone signed up for a direct debit outside the charity shop — still paying it now."
     * — seeded by ChuggerSystem when the player agrees to the STANDING_ORDER direct debit.
     * Spreads via PUBLIC and PENSIONER NPCs within 20 blocks of the charity shop.
     * Ambient flavour; minor sympathy penalty among STREET_LADS (−1 Respect). */
    DIRECT_DEBIT,

    /** "One of the chuggers outside the charity shop said someone actually stopped and chatted — proper nice of 'em."
     * — seeded by ChuggerSystem when the player donates (−2 COIN), reducing Notoriety −1.
     * Spreads via CHUGGER and PUBLIC NPCs; minor warm-fuzzy flavour, no gameplay effect. */
    GRATEFUL_CHUGGER,

    /** "Someone's been doing a fake charity collection on the high street — wearing a tabard and everything."
     * — seeded by ChuggerSystem when the player is detected committing fake-tabard fraud
     * (2nd suspicious donation near Tracy or POLICE).
     * Spreads via CHUGGER_LEADER (Tracy), PUBLIC, and POLICE NPCs within 30 blocks.
     * Triggers Notoriety +6, WantedSystem +1, CHARITY_FRAUD in CriminalRecord.
     * NewspaperSystem headline eligible. */
    CHARITY_FRAUD_RUMOUR,

    // ── Issue #1303: Northfield Dave's Carpets ────────────────────────────────

    /** "Heard someone's been brokering deals for Dave's Carpets — earning a coin a head bringing punters in."
     * — seeded by CarpetShopSystem when the player successfully refers an NPC (5th referral).
     * Spreads via STREET_LAD and PUBLIC NPCs; StreetSkillSystem +TRADING XP. */
    DEAL_BROKER,

    /** "Word is someone nicked a load of carpet from Dave's stockroom while the driver wasn't looking."
     * — seeded by CarpetShopSystem when the player loots the CARPET_ROLL_PROP.
     * Spreads via PUBLIC and STREET_LAD NPCs within 30 blocks.
     * Triggers Notoriety +4, WantedSystem +1, THEFT in CriminalRecord.
     * NewspaperSystem headline eligible. */
    CARPET_THIEF,

    /** "Apparently someone grassed Dave's Carpets up to Trading Standards — about bloody time."
     * — seeded by CarpetShopSystem when the player reports Dave to Sandra.
     * Spreads via PUBLIC, STREET_LAD NPCs; FactionSystem: Marchetti −1, Street Lads +1. */
    DAVE_REPORTED,

    // ── Issue #1306: Northfield Traveller Site ────────────────────────────────

    /** "Heard the travellers have pitched up on the industrial estate again — Paddy's mob."
     * — seeded by TravellerSiteSystem when the site appears (day 1).
     * Spreads via PUBLIC, STREET_LAD, PENSIONER NPCs.
     * NeighbourhoodSystem awareness flag set; Marchetti Crew alerted. */
    TRAVELLERS_ARRIVED,

    /** "Someone got a driveway done off them travellers — looks like it's already falling apart."
     * — seeded by TravellerSiteSystem when a cash-in-hand job is completed with low quality.
     * Spreads via PENSIONER and PUBLIC NPCs within 25 blocks.
     * StreetSkillSystem: TRADING XP −1 for player if they referred the job. */
    SHODDY_WORK,

    /** "Saw someone pelting Doris with clothes pegs outside the post office — had me in stitches."
     * — seeded by TravellerSiteSystem when the player throws CLOTHES_PEG_BUNDLE at an NPC.
     * Spreads via PUBLIC NPCs; NPC target enters STARTLED state; comedy flavour only. */
    CLOTHES_PEG_INCIDENT,

    /** "Word is someone turned over the travellers' caravan in the middle of the night."
     * — seeded by TravellerSiteSystem on a successful night caravan raid.
     * Spreads via STREET_LAD and PUBLIC NPCs.
     * Triggers NotorietySystem +5; if DOG_FIGHT_LEDGER taken, NewspaperSystem headline eligible. */
    DOG_FIGHT_RAID,

    // ── Issue #1315: Prison Van Escape — The Paddy Wagon Hustle ───────────────

    /** "Did you hear? Someone only legged it out the back of the police van —
     * right on the ring road. Officers were fuming."
     * Seeded by PrisonVanSystem on successful escape. Spreads via PUBLIC, STREET_LAD,
     * and BARMAN NPCs town-wide. Adds +5 STREET_LADS Respect; NewspaperSystem headline
     * eligible (front page: "Escape from the Paddy Wagon"). Police patrol frequency +1
     * for 24 in-game hours. */
    VAN_ESCAPE,

    /** "Police are looking for someone who did a runner from their van on the
     * industrial estate."
     * Seeded simultaneously with VAN_ESCAPE. Spreads via POLICE NPCs; re-activates
     * WANTED state after the 60-second grace window expires. */
    CUSTODY_DODGER,

    // ── Issue #1317: Northfield Bonfire Night ─────────────────────────────────

    /** "Fireworks going off all over the shop on the rec — it's bedlam out there.
     * Someone nearly took someone's eye out."
     * Seeded by BonfireNightSystem when a firework misfire causes a fire or injures
     * a spectator NPC. Spreads via PUBLIC and BARMAN NPCs. Triggers NoiseSystem spike;
     * adds +1 to FIREWORK_OFFENCE watch pressure. FireStationSystem callout probability +20%. */
    FIREWORK_CHAOS,

    /** "Darren's well annoyed — someone robbed his holdall and nicked all his stock.
     * He's going mental behind the offie."
     * Seeded by BonfireNightSystem when the player successfully robs Darren's holdall
     * (STEALTH ≥ 2 approach). Spreads via STREET_LAD and BARMAN NPCs. Darren enters
     * HOSTILE state; Marchetti Crew respect −1 (Darren supplies the crew). */
    DARREN_TURF_WAR,

    /** "Someone shoved a banger in the display mortar at the Tesco car park —
     * went off early and nearly flattened the compère."
     * Seeded by BonfireNightSystem when the player plants a BANGER_FIREWORK in
     * FIREWORK_MORTAR_PROP. Spreads via PUBLIC, BARMAN, and YOUTH_GANG NPCs.
     * NotorietySystem +8; NewspaperSystem headline eligible ("Bonfire Night Chaos at
     * Tesco Car Park"). Triggers CRIMINAL_DAMAGE and FIRE_ENGINE response. */
    FIREWORK_PRANK,

    /** "Some muppet nicked Darren's whole holdall — his full stock of fireworks,
     * gone. He's out here with nothing to sell."
     * Seeded by BonfireNightSystem when the player steals Darren's holdall
     * (STEALTH ≥ 2). Spreads via STREET_LAD, BARMAN, and FIREWORK_DEALER_NPC.
     * Darren enters HOSTILE state. FenceSystem stock gains ROCKET_FIREWORK ×3
     * for 24 in-game hours. */
    FIREWORK_THEFT,

    // ── Issue #1319: NatWest Cashpoint — The Dodgy ATM ───────────────────────

    /** "Word is there's a dodgy reader on the NatWest machine on the High Street —
     * been nicking card details off everyone who uses it."
     * Seeded by CashpointSystem when a CARD_SKIMMER_DEVICE session yields 2+ CLONED_CARD_DATA.
     * Spreads via PUBLIC and BARMAN NPCs. Police patrol frequency near CASHPOINT_PROP +1.
     * Triggers +1 WantedSystem star if the skimmer is still active when rumour propagates. */
    CARD_SKIMMING_WARNING,

    /** "Three Kenny sightings this week near the cashpoint — that man is on one."
     * Seeded by CashpointSystem after the player completes 3 envelope-drop runs.
     * Spreads via STREET_LAD and BARMAN NPCs.
     * Flags ORGANISED_CRIME in CriminalRecord; STREET_LADS Respect +2 on seeding. */
    ORGANISED_CRIME,

    /** "Proper hard nut about — clocked someone in the club last night, absolute scenes."
     * Seeded by NightclubSystem when player notoriety ≥ 50 enters The Vaults.
     * Spreads via NIGHTCLUB_PUNTER and BOUNCER NPCs. */
    HARD_NUT_IN_TOWN,

    // ── Issue #1333: Northfield Employment System ─────────────────────────────

    /** "Did you hear? So-and-so's been up at the charity shop every week, giving back
     * to the community. Legend, apparently."
     * Seeded by EmploymentSystem when player completes a Charity Shop volunteer shift.
     * Spreads via PUBLIC and PENSIONER NPCs. Reduces NeighbourhoodWatch anger by 2
     * for 1 in-game day. Community Respect +2. */
    COMMUNITY_SPIRIT,

    // ── Issue #1335: Northfield Cycle Centre — Dave's Bikes ───────────────────

    /** "Someone's been nicking bikes off the street — proper organised, they are."
     * Seeded by CycleShopSystem when the player cuts a lock off a LOCKED_BIKE_PROP.
     * Spreads via PUBLIC, STREET_LAD, and PENSIONER NPCs.
     * NotorietySystem +5 if player is identified; police patrol frequency near
     * BIKE_RACK_PROP landmarks +1 for 1 in-game day. */
    BIKE_THEFT_RING,

    /** "Heard the bloke at Dave's Bikes is flogging hot bikes out the back — don't
     * ask questions, just bring cash."
     * Seeded by CycleShopSystem when the player fences a STOLEN_BIKE through
     * TravellerSiteSystem or buys a suspiciously cheap SECOND_HAND_BIKE.
     * Spreads via STREET_LAD and BARMAN NPCs.
     * Triggers BIKE_THEFT CriminalRecord entry if police hear it (NotorietySystem ≥ 30). */
    STOLEN_BIKE_TRADE,

    /** "Bloody hell — some lunatic on a delivery bike nearly took my head off on
     * the high street. Doing about 40, no lights, pavement an' all."
     * Seeded by CycleShopSystem when the player exceeds the speed cap (riding speed
     * ×2.0 within 5 blocks of a PUBLIC or PENSIONER NPC) or rides on the pavement.
     * Spreads via PUBLIC and PENSIONER NPCs.
     * NotorietySystem +3; seeds a PCSO patrol near the offence location. */
    RECKLESS_CYCLIST,

    // ── Issue #1337: Northfield Police Station — The Nick ─────────────────────

    /** "Someone only went and turned over the nick — walked straight into the evidence locker."
     * — seeded by PoliceStationSystem on a successful evidence locker heist (any route).
     * Spreads via STREET_LAD and BARMAN NPCs town-wide.
     * STREET_LADS Respect +5 for the player; NewspaperSystem headline eligible.
     * Police patrol frequency across all landmarks +1 for 24 in-game hours. */
    STATION_BREAK_IN,

    /** "Heard someone walked into the nick and gave themselves up — turned themselves in."
     * — seeded by PoliceStationSystem when the player voluntarily surrenders at the
     * ENQUIRY_COUNTER_PROP. Spreads via PUBLIC and PENSIONER NPCs.
     * MagistratesCourtSystem reduces sentence tier by one step on the next hearing.
     * Notoriety −10 on seeding; awards CAME_IN_QUIETLY achievement. */
    TURNED_YOURSELF_IN,

    // ── Issue #1339: Council Enforcement Day ──────────────────────────────────

    /** "Council's doing one of their big enforcement sweeps tomorrow — TV licensing,
     * traffic wardens, DVLA, the lot. Posted a notice on the community centre board."
     * — seeded by CouncilEnforcementSystem at 19:00 the evening before each sweep day
     * (days 13, 27, 41… — i.e. one day before the actual enforcement on days 14, 28, 42…).
     * Spreads via PUBLIC, BARMAN, and PENSIONER NPCs.
     * Awards FOREWARNED achievement on first seeding the player hears.
     * Gives the player time to pre-empt citations (lay low, move cars, hide TVs). */
    ENFORCEMENT_SWEEP,

    // ── Issue #1341: Northfield Residents' Association Meeting ────────────────

    /** "Margaret from the residents' association is absolutely furious — going on about
     * some planning application. You can hear her from the car park."
     * — seeded by ResidentsAssociationSystem when the player uses the Stir up Fear
     * speech option during Agenda slot 1 (Planning Application Review), or when the
     * Walkout Gambit fires and Margaret gives Notoriety +3.
     * Spreads via PUBLIC, PENSIONER, and BARMAN NPCs within 30 blocks.
     * Awards NIMBY achievement on first seeding. */
    NIMBY_FURY,

    /** "Word is the residents' association treasurer has been helping herself to the
     * petty cash tin. Twenty quid a month, month after month. Kevin knows."
     * — seeded by ResidentsAssociationSystem when the player successfully pickpockets
     * Pauline's PETTY_CASH_TIN (Notoriety +5) OR when the budget motion to investigate
     * finances passes in Agenda slot 2 (Budget Review).
     * Spreads via BARMAN, PUBLIC, and RESIDENTS_MEMBER NPCs.
     * Triggers NewspaperSystem headline + STREET_LADS mission hook. */
    BENT_TREASURER,

    /** "Complete chaos at the residents' meeting last night — half of them walked out,
     * agenda never finished. Margaret looked like she was about to burst."
     * — seeded by ResidentsAssociationSystem when the Walkout Gambit succeeds (2+
     * RESIDENTS_MEMBER NPCs leave, dropping attendance below quorum).
     * Spreads via PUBLIC and PENSIONER NPCs town-wide.
     * Awards MEETING_DISSOLVED achievement on first seeding. */
    MEETING_CHAOS,

    // ── Issue #1347: Northfield Remembrance Sunday ────────────────────────────

    /** "They're doing the remembrance do at the war memorial again Sunday morning — Reverend
     * Dave, the Buffaloes, the lot. Silence at eleven."
     * — seeded by RemembranceSundaySystem at 09:00 on Remembrance Sunday (second Sunday
     * of November). Spreads via PUBLIC, PENSIONER, and BARMAN NPCs.
     * Awards LEST_WE_FORGET achievement on first ceremony attendance by the player. */
    REMEMBRANCE_CEREMONY,

    /** "Someone only went and made a racket right in the middle of the two-minute silence.
     * The whole town's talking about it."
     * — seeded by RemembranceSundaySystem when the player breaks the silence (moves,
     * attacks, or breaks a block during 11:00–11:02 on Remembrance Sunday).
     * Spreads via PUBLIC, PENSIONER, and VETERAN NPCs town-wide.
     * Triggers NewspaperSystem headline: 'Local yob disrupts Remembrance ceremony'. */
    SILENCE_BREACH,

    // ── Issue #1349: Northfield RAOB Buffalo Lodge No. 1247 ──────────────────

    /** "Northfield Buffaloes are taking on new initiates — Monday and Thursday evenings,
     * Lodge on the high street. Ron runs it. Three favours and a tenner gets you in."
     * — seeded by RAOBLodgeSystem when sponsor trust is established.
     * Spreads via PUBLIC, BARMAN, and RAOB_LODGE_MEMBER NPCs. */
    RAOB_INITIATION,

    /** "Someone turned over the Buffalo Lodge last night — safe emptied, the lot.
     * Ron's not happy. Councillor Walsh is apparently livid."
     * — seeded by RAOBLodgeSystem on LODGE_BURGLARY crime trigger.
     * Triggers NewspaperSystem headline. Spreads via PUBLIC and PENSIONER NPCs. */
    LODGE_BURGLARY,

    /** "That ledger from the Lodge has turned up at the Citizens Advice. Half the
     * council's in it. Councillor Walsh is threatening legal action."
     * — seeded by RAOBLodgeSystem on KOMPROMAT_LEDGER delivery.
     * Triggers newspaper headline + Notoriety −5 for the player. */
    KOMPROMAT_REVEALED,

    /** "Overheard at the Lodge: Councillor Walsh is backing the new leisure centre
     * planning application. Apparently the Buffaloes have first refusal on the snooker room."
     * — seeded during Grand Ceremony by RAOBLodgeSystem (active RAOB_MEMBER players overhear).
     * Spreads slowly via RAOB_LODGE_MEMBER and COUNCILLOR_MEMBER NPCs. */
    FACTION_RUMOUR,

    // ── Issue #1351: Northfield QuickFix Loans ──────────────────────────────

    /** "Someone round here's in serious financial bother — bailiff's been spotted on
     * the estate." — seeded by PaydayLoanSystem when BAILIFF_NPC arrives at squat.
     * Triggers NeighbourhoodSystem Vibes −3. Spreads via PUBLIC and PENSIONER NPCs. */
    DEBT_TROUBLE,

    /** "I heard someone owes money to both the loan sharks AND the Marchettis.
     * That's two sets of heavies after them." — seeded by PaydayLoanSystem when
     * player simultaneously has a QuickFix Loans debt and a Marchetti Crew debt.
     * Grants STREET_LADS Respect +5. Spreads via PUBLIC and STREET_LAD NPCs. */
    DOUBLE_DEBTOR,

    /** "Darren was telling the barman about some customer who paid him back in
     * full with stolen goods. Didn't seem to bother him." — seeded by
     * PaydayLoanSystem when loan repaid with fenced goods.
     * Spreads via BARMAN and PUBLIC NPCs. */
    LOAN_SHARK,

    // ── Issue #1353: Northfield Amateur Dramatics Society ─────────────────────

    /** "There was an absolute disaster at the drama do — power cut, prop gun mix-up,
     * cash box gone. The whole thing fell apart." — seeded by AmateurDramaticsSystem
     * when any of Mario's sabotage options are executed on opening night.
     * Triggers NeighbourhoodSystem Vibes −4 town-wide. Spreads via PUBLIC, BARMAN,
     * and PENSIONER NPCs. Leads to NewspaperSystem headline next day. */
    NAODS_DRAMA_DISASTER,

    /** "Someone was listening in at drama rehearsal — reckon they got something on
     * Marchetti." — seeded by AmateurDramaticsSystem when player eavesdrops on
     * Mario during his 21:15–21:20 notes break.
     * Grants MARCHETTI_CREW −5 Respect (surveillance detected). Spreads via
     * BARMAN and STREET_LAD NPCs as faction intel. */
    MARCHETTI_SECRETS,

    /** "Patricia's had a load of people auditioning for the drama group — Blood Brothers
     * apparently." — seeded by AmateurDramaticsSystem when player completes the
     * audition BattleBarMiniGame (any score ≥ 1).
     * Spreads via PUBLIC and PENSIONER NPCs. */
    NAODS_CASTING,

    // ── Issue #1357: Northfield Charity Fun Run ────────────────────────────────

    /** "That charity fun run got rained off this morning — Janet was gutted,
     * apparently." — seeded by CharityFunRunSystem when HEAVY_RAIN cancels at 08:30.
     * Triggers NeighbourhoodSystem Vibes −1. Spreads via PUBLIC and PENSIONER NPCs. */
    FUN_RUN_CANCELLED,

    /** "Someone cut the corner on the charity run — a jogger saw the whole
     * thing." — seeded by CharityFunRunSystem when a JOGGER witnesses course-cutting.
     * Triggers NeighbourhoodSystem Vibes −3. Spreads via JOGGER and PUBLIC NPCs. */
    COURSE_CUTTING,

    // ── Issue #1359: Northfield HMRC Tax Investigation ────────────────────────

    /** "Word is someone round here's had a visit from the taxman — paying cash
     * in hand catches up with you eventually." — seeded by HMRCSystem when Sandra
     * serves a TAX_DEMAND_LETTER. Triggers NeighbourhoodSystem Vibes −2.
     * Spreads via PUBLIC and PENSIONER NPCs. */
    TAX_TROUBLES,

    /** "Heard someone tipped off the taxman about a mate's dealings — that loan
     * shark's been busy." — seeded by HMRCSystem when LOAN_SHARK_CLERK tips off
     * HMRC below the 150 COIN threshold. Spreads via PUBLIC and LOAN_SHARK_CLERK. */
    HMRC_TIPPED_OFF,

    /** "Sandra from the taxman took a brown envelope off someone on Northfield Road."
     * — seeded by HMRCSystem when bribeSandra() succeeds. Spreads via PUBLIC NPCs. */
    BENT_OFFICIAL,

    // ── Issue #1363: Northfield Sunday Car Boot Sale ───────────────────────────

    /** "Word is someone's been flogging knocked-off gear down the car boot —
     * Trading Standards were sniffing round on Sunday." — seeded by
     * CarBootSaleSystem when TRADING_STANDARDS_STING event fires (3+ stolen
     * items sold on player pitch). Triggers NeighbourhoodSystem Vibes −3.
     * Spreads via PUBLIC, BOOT_SALE_PUNTER, and PENSIONER NPCs. */
    STOLEN_GOODS_MARKET,

    /** "Did you hear? Some pensioner got shoved out the way at the car boot —
     * everyone was after the same box of VHS tapes at 6am." — seeded by
     * CarBootSaleSystem during the EARLY_BIRD_RUSH event when a PENSIONER NPC
     * is displaced. Triggers NeighbourhoodSystem Vibes −1.
     * Spreads via PENSIONER and PUBLIC NPCs. */
    PENSIONER_SHOVED,

    /** "Absolute carnage out there last night. Police everywhere."
     * Seeded at midnight on day 1 (after NYE). Spreads via PUBLIC, DRUNK.
     * Spread velocity +3. */
    NYE_CHAOS,

    // ── Issue #1371: Northfield Christmas Market ──────────────────────────────

    /** "The Christmas Market's on down by the war memorial — mulled wine,
     * bratwurst, the lot. It's actually quite nice this year."
     * Seeded by ChristmasMarketSystem when market opens (10:00, days 335–356).
     * Triggers NeighbourhoodSystem Vibes +3/hr while active.
     * Spreads via PUBLIC, PENSIONER, and SCHOOL_KID NPCs. */
    CHRISTMAS_CHEER,

    /** "The Christmas Market's been cancelled — bucketing it down, no vendors
     * bothered showing up."
     * Seeded by ChristmasMarketSystem when HEAVY_RAIN or THUNDERSTORM cancels
     * the market. Triggers NeighbourhoodSystem Vibes −2.
     * Spreads via PUBLIC and PENSIONER NPCs. */
    XMAS_MARKET_CANCELLED,

    // ── Issue #1373: Northfield Local Council Elections ───────────────────────

    /** "They're out knocking on doors all week — leaflets, promises, the lot."
     * Seeded by LocalElectionSystem during canvassing week (days 83–89).
     * Spreads via PUBLIC, PENSIONER, and CANDIDATE_NPC NPCs. */
    ELECTION_CANVASSING,

    /** "Someone nicked the postal votes — absolute stitch-up."
     * Seeded when player is caught committing postal vote fraud (detection 15%).
     * Triggers NewspaperSystem headline. Spreads via PUBLIC NPCs. */
    ELECTION_FRAUD,

    /** "Can't believe [candidate] won — nobody saw that coming."
     * Seeded by LocalElectionSystem after results announced (22:30, day 90).
     * Triggers NeighbourhoodSystem Vibes ±2 based on faction alignment.
     * Spreads via PUBLIC and COUNT_OBSERVER NPCs. */
    ELECTION_UPSET,

    /** "Things are going to change around here — you wait and see."
     * Seeded day after results for 7-day post-election effect window.
     * Triggers faction bonuses based on winning faction.
     * Spreads via PUBLIC, PENSIONER NPCs. */
    ELECTION_AFTERMATH,

    // ── Issue #1381: Northfield Bank Holiday Street Party ─────────────────────

    /** "Heard Gerald took a bung to keep quiet at the do."
     * Seeded by StreetPartySystem when player bribes Gerald.
     * Triggers THE_COUNCIL −5 respect.
     * Spreads via PUBLIC, PENSIONER NPCs. */
    CORRUPT_OFFICIAL,

    /** "Someone lamped Gerald from Neighbourhood Watch at the street party. Unbelievable scenes."
     * Seeded by StreetPartySystem when player punches Gerald.
     * Triggers ASSAULT crime + Notoriety +8 + WantedSystem +1.
     * Spreads via PUBLIC NPCs. */
    GERALD_DOWN,

    /** "The BBQ at the street party went up. Half the road was on fire."
     * Seeded by StreetPartySystem when BBQ goes OUT_OF_CONTROL and spreads to 3+ blocks.
     * Triggers ARSON crime + Notoriety +10 + NeighbourhoodSystem VIBES −5.
     * Spreads via PUBLIC, PENSIONER NPCs. */
    FIRE_HAZARD,

    // ── Issue #1381: Northfield Halloween ─────────────────────────────────────

    /** "Someone's been egging doors up and down the road. Absolute state of it."
     * Seeded by HalloweenSystem when player eggs a house, car, or NPC.
     * Triggers NeighbourhoodSystem VIBES −1 + Notoriety +2.
     * Spreads via PUBLIC, PENSIONER NPCs. */
    ANTI_SOCIAL_BEHAVIOUR,

    /** "Halloween got completely out of hand out there — police were called three times."
     * Seeded by HalloweenSystem when WantedSystem reaches 2+ stars during event.
     * Triggers NeighbourhoodSystem VIBES −3 + NewspaperSystem headline.
     * Spreads via PUBLIC, PENSIONER, NEIGHBOURHOOD_WATCH NPCs. */
    HALLOWEEN_CHAOS,

    // ── Issue #1386: Northfield St George's Day ───────────────────────────────

    /** "Someone nicked the flag off the Wetherspoons wall. In broad daylight."
     * Seeded by StGeorgesDaySystem when player takes ST_GEORGE_FLAG_PROP from bar.
     * Triggers Notoriety +3 + 3-day Wetherspoons ban.
     * Spreads via PUBLIC, BARMAN NPCs. */
    FLAG_HEIST,

    /** "Some lad climbed up the drainpipe and had the roof flag away. On St George's Day of all days."
     * Seeded by StGeorgesDaySystem when player successfully takes ROOF_FLAG_PROP.
     * Triggers Notoriety +5 + NewspaperSystem headline if filmed.
     * Spreads via PUBLIC, PENSIONER NPCs. */
    ROOF_FLAG_TAKEN,

    /** "It kicked off in the park — English lads and the counter-protesters going at each other."
     * Seeded by StGeorgesDaySystem when CROWD_SCUFFLE fires at 14:00.
     * Triggers NoiseSystem level 8 + POLICE response in 30 seconds.
     * Spreads via PUBLIC, PENSIONER, JOURNALIST NPCs. */
    COMMUNITY_OUTRAGE,

    /** "Morris dancer got his stick nicked by some tourist-looking bloke. Absolute scenes."
     * Seeded by StGeorgesDaySystem when player steals MORRIS_STICK_PROP.
     * Triggers all 6 MORRIS_DANCER NPCs to pursue player.
     * Spreads via PUBLIC NPCs. */
    MORRIS_STICK_THEFT,

    // ── Issue #1390: Northfield Annual Conker Championship ───────────────────

    /** "Someone got disqualified at the conker championship — Derek found a hardened one."
     * Seeded by ConkerSystem when Derek catches the player with a HARDENED_CONKER.
     * Triggers Notoriety +2 and CHEATING_AT_CONKERS CriminalRecord entry.
     * Spreads via PUBLIC, PENSIONER NPCs. */
    CONKER_CHEAT_CAUGHT,

    /** "Little Tyler won the conker championship! Nine years old and beat the lot of them."
     * Seeded by ConkerSystem when Tyler wins the tournament (player threw match or lost).
     * Triggers NewspaperSystem headline. Spreads via PUBLIC, PENSIONER NPCs. */
    TYLER_WON_IT,

    /** "Someone's nicked the conker trophy off the prize table. Right under Derek's nose."
     * Seeded by ConkerSystem when player takes CONKER_TROPHY from TROPHY_TABLE_PROP.
     * Triggers Notoriety +5. Spreads via PUBLIC, PENSIONER NPCs. */
    CONKER_TROPHY_NICKED;
}

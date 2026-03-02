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
    OFSTED_VISIT;
}

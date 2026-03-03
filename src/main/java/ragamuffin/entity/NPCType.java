package ragamuffin.entity;

/**
 * Types of NPCs in the game, with combat stats.
 */
public enum NPCType {
    PUBLIC(20f, 0f, 0f, false),            // Members of the public - passive
    DOG(15f, 5f, 2.0f, false),             // Dogs - bite if provoked
    YOUTH_GANG(30f, 8f, 1.5f, true),       // Gangs - aggressive, steal and punch
    COUNCIL_MEMBER(25f, 0f, 0f, false),    // Bureaucratic - passive
    POLICE(50f, 10f, 1.0f, true),          // Police - tough, hit hard
    COUNCIL_BUILDER(40f, 5f, 2.0f, false), // Builders - defensive only
    SHOPKEEPER(20f, 0f, 0f, false),        // Stand near shops, comment on player
    POSTMAN(20f, 0f, 0f, false),           // Walk routes between buildings
    JOGGER(20f, 0f, 0f, false),            // Run through the park
    DRUNK(15f, 3f, 3.0f, false),           // Stumble around at night, mildly aggressive
    BUSKER(20f, 0f, 0f, false),            // Stand on high street, play music
    DELIVERY_DRIVER(20f, 0f, 0f, false),   // Amazon/JustEat driver rushing about
    PENSIONER(10f, 0f, 0f, false),          // Slow elderly person, complains a lot
    SCHOOL_KID(15f, 2f, 3.0f, false),      // Noisy school kids in groups
    STREET_PREACHER(20f, 0f, 0f, false),   // Named NPC: preacher with distinctive robes
    LOLLIPOP_LADY(20f, 0f, 0f, false),    // Named NPC: crossing patrol warden with hi-vis
    FENCE(25f, 0f, 0f, false),            // Black market trader — operates at charity shop / industrial estate
    TUNNEL_DWELLER(20f, 4f, 2.5f, false), // Underground inhabitant living in sewers/bunker — wary but not hostile
    BARMAN(20f, 0f, 0f, false),           // Stands behind the bar at the pub; rumour sink; sells drinks
    BOUNCER(50f, 15f, 2.0f, false),       // Stands at pub entrance; blocks entry for criminals
    FACTION_LIEUTENANT(40f, 12f, 1.5f, false), // Phase 8d: named faction lieutenant who offers missions

    // ── Phase 8e: Notoriety system ────────────────────────────────────────────

    /** Community Support Officer — slower, does NOT arrest; issues verbal warnings only. */
    PCSO(25f, 0f, 0f, false),

    /** Armed Response Unit — faster and harder hitting than regular POLICE; pursues indoors. */
    ARMED_RESPONSE(60f, 15f, 0.8f, true),

    /** A Street Lad NPC — recruitable as accomplice at Tier 4+. */
    STREET_LAD(20f, 4f, 2.0f, false),

    /** Permanent NPC accomplice recruited by the player at Tier 4+. */
    ACCOMPLICE(30f, 5f, 1.5f, false),

    // ── Issue #708: Birds ─────────────────────────────────────────────────────

    /** Ambient bird (pigeon/seagull) — flaps about the park and rooftops. Passive. */
    BIRD(3f, 0f, 0f, false),

    // ── Issue #712: Slumlord property system ──────────────────────────────────

    /** Estate agent — weekday-only NPC who sells properties to the player. */
    ESTATE_AGENT(20f, 0f, 0f, false),

    /** Thug — sent by rival factions to intimidate/take over player-owned buildings. */
    THUG(35f, 8f, 1.5f, true),

    // ── Issue #716: Underground Music Scene ──────────────────────────────────

    /**
     * MC Champion — a named faction MC who can be challenged to a battle.
     * Marchetti MC (off-licence), Street Lads MC (park), Council MC (JobCentre).
     * Passive until challenged with a MICROPHONE; never hostile unprovoked.
     */
    MC_CHAMPION(30f, 0f, 0f, false),

    /**
     * Hype Man — permanent companion NPC unlocked at MC Rank 5.
     * Follows the player near the squat, boosts rave income by +10% each.
     */
    HYPE_MAN(20f, 0f, 0f, false),

    /**
     * Rave Attendee — an NPC drawn to the squat by the RAVE_ANNOUNCEMENT rumour.
     * Generates 1 COIN/in-game-minute while the rave is active.
     */
    RAVE_ATTENDEE(20f, 0f, 0f, false),

    // ── Issue #774: The Daily Ragamuffin ──────────────────────────────────────

    /**
     * Journalist — spawns in the pub between 19:00–22:00.
     * Player can interact (J key) to tip off, plant a lie, or buy out stories.
     */
    JOURNALIST(20f, 0f, 0f, false),

    // ── Issue #781: Graffiti & Territorial Marking ────────────────────────────

    /**
     * Council Cleaner — dispatched by THE_COUNCIL to scrub graffiti from civic areas
     * (near town hall, Greggs, park) after 2 in-game days. Carries a bucket. Passive.
     */
    COUNCIL_CLEANER(20f, 0f, 0f, false),

    // ── Issue #783: Pirate FM — Underground Radio Station ────────────────────

    /**
     * Listener — a civilian drawn to the pirate radio transmitter's signal.
     * Spawned by the "Black Market Shout-Out" broadcast action (up to 6 at once).
     * Walks toward the transmitter; on arrival (within 4 blocks), enters IDLE and
     * drops loot (CAN_OF_LAGER, CIGARETTE, COIN×3, TOBACCO_POUCH, SCRATCH_CARD,
     * STOLEN_PHONE, NEWSPAPER, WOOLLY_HAT_ECONOMY, PRESCRIPTION_MEDS).
     * Despawns after 60 in-game seconds at the transmitter.
     */
    LISTENER(20f, 0f, 0f, false),

    // ── Issue #785: The Dodgy Market Stall ────────────────────────────────────

    /**
     * Market Inspector — spawns after 3 minutes of unlicensed trading.
     * Can be bribed for 10 COIN (adds 5 Notoriety) or will confiscate all stock.
     * Passive until interacting; walks toward the stall when spawned.
     */
    MARKET_INSPECTOR(25f, 0f, 0f, false),

    // ── Issue #787: Street Skills & Character Progression ────────────────────

    /**
     * Follower — a PUBLIC NPC rallied by the player via the RALLY mechanic (INFLUENCE Expert+).
     * Follows the player; deters POLICE and GANG NPCs within 5 blocks.
     * Disperses after 120 seconds or when the player is arrested.
     */
    FOLLOWER(20f, 0f, 0f, false),

    // ── Issue #795: The JobCentre Gauntlet ────────────────────────────────────

    /**
     * Case Worker — JobCentre Plus desk staff. Passive until spoken to.
     * Reacts based on player's criminal record and notoriety tier during sign-on.
     * At Notoriety Tier 5, flees and permanently closes the claim.
     */
    CASE_WORKER(20f, 0f, 0f, false),

    /**
     * Debt Collector — grey-suited embodiment of bureaucratic inevitability.
     * Unkillable (infinite health); pathfinds to player when spawned.
     * Broadcasts "Oi! You owe the DWP" every 10 seconds.
     * Punching adds Notoriety +15 and triggers a newspaper headline.
     */
    DEBT_COLLECTOR(Float.MAX_VALUE, 0f, 0f, false),

    /**
     * Assessor — Work Capability Assessment NPC for the corresponding job search mission.
     * Passive; delivers absurdist verdict dialogue ("You're fit for work — you can walk 50 metres").
     */
    ASSESSOR(20f, 0f, 0f, false),

    // ── Issue #797: The Neighbourhood Watch ───────────────────────────────────

    /**
     * Watch Member — a tabard-wearing vigilante with a clipboard.
     * Spawned by the NeighbourhoodWatchSystem at Tier 2+.
     * At Tier 2–3: patrols and follows the player; issues speech (soft citizen's arrest).
     * At Tier 4: coordinates with other Watch Members; brawls with gang NPCs.
     * Can be converted to patrol mode via a PEACE_OFFERING.
     * Passive until the player commits a visible crime nearby.
     */
    WATCH_MEMBER(30f, 6f, 2.0f, false),

    // ── Issue #799: The Corner Shop Economy ────────────────────────────────────

    /**
     * Undercover Police — plainclothes officer monitoring the player's shop.
     * Spawns at Heat Level 60. Appears as PUBLIC NPC; does not arrest on sight.
     * Triggers a Raid Warning at Heat 80 and a full Raid at Heat 100.
     */
    UNDERCOVER_POLICE(40f, 10f, 1.0f, false),

    /**
     * Runner — a hired PUBLIC/YOUTH NPC employed by the player for 5 coins/day.
     * Autonomously restocks empty shop slots and makes player-queued home deliveries.
     * Earns +2 coin premium per delivery item; each delivery adds +3 Notoriety.
     */
    RUNNER(20f, 0f, 0f, false),

    // ── Issue #801: The Underground Fight Night ────────────────────────────────

    /**
     * Bookie NPC — stands against the east wall of the Pit with a BOOKIE_BOARD.
     * Accepts bets on fights; holds a finite pot (starting 100 COIN).
     * Can be consulted with 5 COIN to reveal one fighter's strength stat.
     */
    BOOKIE_NPC(20f, 0f, 0f, false),

    /**
     * Fighter NPC — a named bare-knuckle fighter participating in Pit bouts.
     * Has hidden stats: strength (1–10), stamina (1–10), dirty (boolean).
     * Cycles between JAB, HAYMAKER (telegraphed 0.5s), and CLINCH attack modes.
     */
    FIGHTER(40f, 6f, 1.2f, false),

    /**
     * Spectator NPC inside the Pit — WORKER, YOUTH, or PENSIONER mix.
     * Provides crowd atmosphere; Street Lads spectators can buff the player.
     */
    PIT_SPECTATOR(20f, 0f, 0f, false),

    // ── Issue #908: Bookies Horse Racing System ────────────────────────────────

    /**
     * Loan Shark — spawns inside bookies when the player's cumulative net loss hits 50 coins.
     * Offers a 20-coin loan at 50% interest (repay 30 coins within 3 in-game days).
     * Becomes hostile on non-repayment; spawns 2 STREET_LAD enforcers and adds WantedSystem +1 star.
     */
    LOAN_SHARK(35f, 8f, 1.5f, false),

    // ── Issue #914: Allotment System ───────────────────────────────────────────

    /**
     * Allotment Warden — a PENSIONER sub-variant stationed at the allotments gate.
     * Opens 07:00–19:00. Manages plot claims, issues repossession notices.
     * Passive; delivers speech on interaction.
     */
    ALLOTMENT_WARDEN(10f, 0f, 0f, false),

    /**
     * Plot Neighbour — a PENSIONER-type NPC tending an adjacent allotment plot.
     * Generates random neighbour events (compliment, complaint, gift, rivalry)
     * every 5 in-game minutes while the player is on site.
     */
    PLOT_NEIGHBOUR(10f, 0f, 0f, false),

    // ── Issue #916: Late-Night Kebab Van ───────────────────────────────────────

    /**
     * Van Owner — the kebab van operator standing at the serving hatch.
     * Spawns near the PUB between 22:00–02:00. Passive; serves food to queuing NPCs
     * and the player. Does not move; despawns with the van.
     */
    VAN_OWNER(20f, 0f, 0f, false),

    // ── Issue #918: Bus Stop & Public Transport System ─────────────────────────

    /**
     * Bus Driver — seated at the wheel of The Number 47 bus.
     * Spawns with the bus entity; despawns when the bus leaves. Passive; never moves
     * independently. Broadcasts grumbles if the bus is running more than 20 in-game
     * minutes late.
     */
    BUS_DRIVER(20f, 0f, 0f, false),

    /**
     * Ticket Inspector — rides the Number 47 bus with a 30% chance per journey.
     * Passive until the player boards; then checks for a valid ticket (BUS_PASS or
     * paid fare). Can be bribed for 3 COIN or beaten for INSPECTOR_BADGE loot.
     * Calls police if notoriety Tier 3+ is detected.
     */
    TICKET_INSPECTOR(30f, 5f, 2.0f, false),

    /**
     * Commuter — a PUBLIC-type NPC that spawns during rush hours (07:30 and 17:30)
     * and paths to the nearest bus stop. Boards the bus automatically; despawns
     * when the bus departs the stop.
     */
    COMMUTER(20f, 0f, 0f, false),

    // ── Issue #920: Pub Lock-In ─────────────────────────────────────────────

    /**
     * Landlord — Terry, the landlord of The Ragamuffin Arms.
     * Locks the front door at 23:00 for an illegal after-hours session with up to 8 guests.
     * Sells drinks at half price; cycles through 10 flavourful speech lines.
     * Remembers if the player grassed and permanently ejects them.
     */
    LANDLORD(30f, 5f, 2.0f, false),

    // ── Issue #926: Tony's Chip Shop ──────────────────────────────────────────

    /**
     * Chippy Owner — Tony, the proprietor of Tony's Chip Shop (CHIPPY landmark).
     * Stands behind the CHIPPY_COUNTER during opening hours (11:00–00:00).
     * Passive; serves food via the ChippyOrderUI. Refuses service to players
     * wearing a BALACLAVA. Notoriety-aware dialogue. Ejects all customers at 00:00.
     */
    CHIPPY_OWNER(20f, 0f, 0f, false),

    /**
     * Stray Cat — Biscuit, the resident cat of Tony's Chip Shop.
     * Lives inside the chippy. Feeding Biscuit 3 times awards the FED_THE_CAT
     * achievement. Punching Biscuit adds +3 Notoriety and seeds a CAT_PUNCH rumour
     * town-wide. Passive and docile; never attacks.
     */
    STRAY_CAT(5f, 0f, 0f, false),

    // ── Issue #928: Public Library System ──────────────────────────────────────

    /**
     * Librarian — stern NORTHFIELD LIBRARY staff member who patrols the shelves.
     * Shushes sprinting players (imposing −30% speed debuff for 5s) and ejects
     * repeat offenders (10-minute door lock). Seeds LIBRARY_BAN rumour on ejection.
     * Passive unless provoked; never attacks. Wears sensible shoes.
     */
    LIBRARIAN(20f, 0f, 0f, false),

    // ── Issue #930: Charity Shop System ──────────────────────────────────────

    /**
     * Volunteer — counter-anchored charity shop staff member who patrols a
     * 6-block route inside Hearts &amp; Minds Charity Shop.
     * Refuses service to BALACLAVA wearers. Nervously serves Tier 3+ players.
     * Reduces mystery bag daily limit to 1 for high-notoriety players.
     * Passive; never attacks. Distinct compassionate speech lines.
     */
    VOLUNTEER(20f, 0f, 0f, false),

    // ── Issue #932: Ice Cream Van System ─────────────────────────────────────

    /**
     * Ice Cream Man — operator of the roaming ice cream van.
     * Flees if the player's notoriety is ≥ 150 or if the player approaches with
     * a CROWBAR in hand. Speech: "What's your fancy?" / "99 Flake? Can't go wrong."
     * / "You're not gonna nick it are ya?"
     */
    ICE_CREAM_MAN(20f, 0f, 0f, false),

    /**
     * Dodgy Van Man — rival ice cream van operator.
     * Hostile if the player has stolen the van. Undercuts prices by 1 COIN and
     * plays a louder jingle during a Jingle War.
     * Speech: "Oi, that's MY patch!" / "You're cutting into my margins, mate."
     */
    DODGY_VAN_MAN(30f, 6f, 2.0f, true),

    // ── Issue #934: Pigeon Racing System ─────────────────────────────────────

    /**
     * Pigeon Fancier — a proud working-class pigeon enthusiast who frequents the park
     * and the allotment area. Sells RACING_PIGEON items to the player (8 COIN each,
     * one per day). Also acts as a rumour source for race-day gossip.
     * On Bulky Item Day, pre-claims BOX_OF_RECORDS at 07:55.
     * Speech: "Beautiful bird, that." / "Trained her meself." / "You keep her fed, yeah?"
     * Passive; never attacks. Wears a flat cap and carries a basket.
     */
    PIGEON_FANCIER(20f, 0f, 0f, false),

    // ── Issue #936: Council Skip & Bulky Item Day ─────────────────────────────

    /**
     * Skip Diver — an opportunistic scavenger who competes with the player on Bulky Item Day.
     * 2–4 spawn at 08:00 when the Bulky Item Day event opens.
     * Every 30 real seconds, an unblocked SKIP_DIVER grabs a random unclaimed item from the skip zone.
     * Acknowledges the player with contextual speech:
     * "Bags I the sofa." / "You were quick." / "Don't even think about that telly."
     * Passive (never attacks); despawns when the event ends at 10:00.
     */
    SKIP_DIVER(20f, 0f, 0f, false),

    // ── Issue #938: Greasy Spoon Café ────────────────────────────────────────

    /**
     * Caff Owner — Vera, the proprietor of Vera's Caff (GREASY_SPOON_CAFE landmark).
     * Stands behind the counter during opening hours (07:00–14:00).
     * Passive; serves food via the café menu. Refuses service if notoriety ≥ 60 and police are nearby.
     * Notoriety-aware dialogue. Last orders at 13:45; ejects all customers at 14:00.
     * Speech: "What can I get you, love?" / "Cash only, no exceptions." / "You alright, darlin'?"
     */
    CAFF_OWNER(20f, 0f, 0f, false),

    /**
     * Caff Regular — a seated regular at Vera's Caff.
     * Passively reveals rumours to the player on proximity (within 2 blocks) without pressing E.
     * Speech delivered into SpeechLogUI automatically on approach.
     * Up to 4 regulars; +2 extra during rain/drizzle/thunderstorm (weather modifier).
     * If Marchetti Crew respect ≥ 70, one regular acts as a dealer (PRESCRIPTION_MEDS from seat).
     * Passive; never attacks. Wears flat cap or hi-vis.
     */
    CAFF_REGULAR(20f, 0f, 0f, false),

    // ── Issue #940: Wheelie Bin Fire System ──────────────────────────────────

    /**
     * Fire Engine — a council-type vehicle NPC that responds to bin fires.
     * Spawns at the nearest road intersection outside the 50-block world boundary
     * 30 seconds after a bin is ignited. Navigates toward the fire at car speed
     * and extinguishes the blaze on arrival. Passive; never attacks the player.
     */
    FIRE_ENGINE(100f, 0f, 0f, false),

    // ── Issue #1000: Northfield Fire Station ──────────────────────────────────

    /**
     * Firefighter — one of two crew members stationed at Northfield Fire Station.
     * Works a 24/7 rota: one patrols the exterior, one rests in the crew room.
     * Night-shift patroller 'sleeps' with halved detection radius.
     * Detects equipment theft with 60% probability if within 6 blocks → +1 wanted star.
     * After 3 false alarms, becomes suspicious of the player.
     * Passive until theft or false alarm triggers; then becomes ALERT/HOSTILE.
     * Speech: "Move along, pal." / "This is a working fire station, not a tourist attraction."
     */
    FIREFIGHTER(60f, 8f, 1.5f, false),

    // ── Issue #942: Food Bank System ──────────────────────────────────────────

    /**
     * Food Bank Volunteer — Margaret, the volunteer running Northfield Food Bank.
     * Stands at the counter Mon–Fri 09:00–17:00. Passive; manages donations and
     * emergency parcel distribution. Refuses service if notoriety ≥ 80.
     * Speech: "Hello love, what can we do for you?" / "Every little helps, ta."
     */
    FOOD_BANK_VOLUNTEER(20f, 0f, 0f, false),

    /**
     * Recipient — a queuing NPC outside the food bank before opening time.
     * 1–3 spawn outdoors in NPCState.QUEUING, murmuring British misery lines.
     * Extra recipients spawn during FROST/COLD_SNAP weather.
     */
    RECIPIENT(15f, 0f, 0f, false),

    /**
     * Council Inspector — spawns at the food bank on Thursdays 10:00–12:00.
     * Increases NeighbourhoodWatch anger +5/min while present. Passive; carries
     * a clipboard. Never attacks; despawns at 12:00.
     */
    COUNCIL_INSPECTOR(25f, 0f, 0f, false),

    // ── Issue #944: High Street Barber ────────────────────────────────────────

    /**
     * Barber — Ali, the proprietor of Ali's Barber Shop (BARBER_SHOP landmark).
     * Open 09:00–18:00 Mon–Sat. Passive; offers haircuts and wet shaves via the
     * haircut menu (press E). Accumulates and shares neighbourhood rumours with
     * the player each visit.
     * Speech: "Sit down, mate — what are we doing today?"
     */
    BARBER(20f, 0f, 0f, false),

    // ── Issue #946: Status Dog — Staffy Companion ─────────────────────────────

    /**
     * Stray Dog — a stray Staffordshire Bull Terrier wandering the park area near
     * the pond. Has no owner; state is WANDERING until the player offers food.
     * Can be adopted by pressing E while holding SAUSAGE_ROLL or STEAK_BAKE.
     * Once adopted, enters FOLLOWING_PLAYER state and becomes the player's companion.
     * Only one stray dog spawns per world; managed by DogCompanionSystem.
     */
    STRAY_DOG(15f, 0f, 0f, false),

    // ── Issue #948: Hand Car Wash ─────────────────────────────────────────────

    /**
     * Car Wash Boss — the owner of the Sparkle Hand Car Wash.
     * Present 08:00–20:00. Stands near the SHED_PROP.
     * Player presses E to start or end a shift, or receive payment.
     * If player Notoriety &gt; 40 when approaching: "I know your face from somewhere.
     * You're not in the papers, are ya?" — but still allows work.
     * Absent for 1 in-game day after the cash box is robbed.
     */
    CAR_WASH_BOSS(20f, 0f, 0f, false),

    // ── Issue #950: Northfield Leisure Centre ─────────────────────────────────

    /**
     * Receptionist — Sharon, who runs the front desk at Northfield Leisure Centre.
     * Present during opening hours (07:00–21:00). Charges 3 COIN entry (5 COIN
     * during after-school peak 15:00–17:00). Refuses entry to players at
     * Notoriety Tier 3+: "You're on the system. You know what you did."
     * COUNCIL_JACKET or BASEBALL_CAP reduces recognition by 60%.
     */
    RECEPTIONIST(20f, 0f, 0f, false),

    /**
     * Swimming Teacher — a leisure centre staff NPC who patrols poolside.
     * Monitors the swimming session timer; ejects players who overstay.
     * Passive; never attacks. Speech: "Right, time's up — out you come."
     */
    SWIMMING_TEACHER(20f, 0f, 0f, false),

    // ── Issue #952: Clucky's Fried Chicken ───────────────────────────────────

    /**
     * Devraj — the proprietor of Clucky's Fried Chicken.
     * Stands behind the CHIPPY_COUNTER during opening hours (10:00–02:00).
     * Passive; serves food via the fried chicken menu. Refuses service to
     * BALACLAVA wearers. Notoriety-aware dialogue.
     * Seeds a midnight rumour at 00:00: shares local gossip from the counter.
     * Speech: "What d'you want, mate?" / "Wings are fresh, just done 'em."
     *         / "I hear things, you know. Sat here all day."
     */
    DEVRAJ(20f, 0f, 0f, false),

    // ── Issue #954: Northfield Bingo Hall ─────────────────────────────────────

    /**
     * The bingo caller — stands at the BINGO_CALLER_PODIUM_PROP.
     * Announces numbers each in-game minute with authentic British bingo patter.
     * Collects entry fees (2 COIN) from the player at the door.
     * Ejects cheaters and false-BINGO shouters with appropriate speech.
     * Speech: "Eyes down, look in!" / "Two fat ladies — eighty-eight!"
     *         / "We've got a cheater — number 9!" / "False call! Shame on you!"
     */
    CALLER(20f, 0f, 0f, false),

    // ── Issue #961: Cash4Gold Pawn Shop ────────────────────────────────────────

    /**
     * Gary the pawn broker — proprietor of Cash4Gold Pawnbrokers (PAWN_SHOP landmark).
     * Stands behind the counter Mon–Sat 09:00–17:30. Passive; offers buy and pledge
     * services via PawnShopUI. If assaulted: drops shutter, flees, closes shop for the day.
     * Speech: "What you got for me?" / "That's quality, that is." /
     *         "I'll give you sixty percent — take it or leave it." /
     *         "No food, no rubble, no coin. I'm a shop, not a skip."
     */
    PAWN_BROKER(25f, 0f, 0f, false),

    // ── Issue #965: Northfield Snooker Hall ───────────────────────────────────

    /**
     * Snooker Hustler — Frank, a seasoned pool shark who wanders Cue Zone 12:00–22:00.
     * Plays at HUSTLER difficulty tier in the snooker mini-game.
     * Will accept side bets of 1–10 COIN. 25% chance of detecting a deliberate miss
     * during the hustle setup. If caught hustling back, turns hostile.
     * Speech: "Fancy a frame, mate?" / "Tell you what — make it interesting?"
     *         / "I've been playing this table since before you were born."
     */
    SNOOKER_HUSTLER(25f, 5f, 2.0f, false),

    // ── Issue #967: Northfield Taxi Rank ──────────────────────────────────────

    /**
     * Minicab Driver — Dave, operator of a beat-up hatchback parked near the pub.
     * Operates 22:00–04:00 as a late-night alternative to A1 Taxis.
     * 1 COIN cheaper per journey than A1 Taxis.
     * 30% chance of detour (drops player 20 blocks off target).
     * 15% chance of issuing a DODGY_PACKAGE item.
     * Refuses service to BALACLAVA-wearing players.
     * Speech: "Hop in, mate." / "Don't ask what's in the bag."
     *         / "I know a shortcut." / "Cash only, yeah?"
     */
    MINICAB_DRIVER(20f, 0f, 0f, false),

    // ── Issue #1207: Big Terry's Cabs ─────────────────────────────────────────

    /**
     * Big Terry — the dispatcher at Big Terry's Cabs.
     * Sits behind the DISPATCHER_HATCH_PROP 07:00–03:00 every day.
     * Refuses service to wanted players (stars ≥ 2) or Notoriety Tier 4+.
     * Speech: "CHARLIE, PICK-UP AT ICELAND!" / "Listen mate, I don't do accounts."
     *         / "No, I can't do a receipt." / "Cash only, obviously."
     *         / "No animals, mate — Terry's allergic."
     */
    MINICAB_DISPATCHER(30f, 0f, 0f, false),

    // ── Issue #969: Northfield Cemetery ───────────────────────────────────────

    /**
     * Groundskeeper — Vernon, the cemetery warden.
     * Present 08:00–17:00. Patrols cemetery paths.
     * Passive unless the player attempts to dig a grave while he watches —
     * then calls police (+2 Notoriety, CrimeType.GRAVE_ROBBING).
     */
    GROUNDSKEEPER(25f, 0f, 0f, false),

    /**
     * Funeral Director — leads processions to the graveside.
     * Passive; delivers solemn speech lines. Never hostile.
     * Despawns after the procession ends (90 real seconds).
     */
    FUNERAL_DIRECTOR(20f, 0f, 0f, false),

    /**
     * Mourner — follows the coffin during a funeral procession.
     * 3–6 per funeral. Drops FLOWERS_PROP on departure.
     * Flees if the player attacks; never hostile.
     */
    MOURNER(20f, 0f, 0f, false),

    /**
     * Gravedigger — Reg, the cemetery's hired gravedigger.
     * Present weekday mornings 06:00–09:00, digging new plots.
     * Passive unless he witnesses the player digging a grave —
     * then becomes hostile and calls police.
     */
    GRAVEDIGGER(30f, 5f, 1.5f, false),

    // ── Issue #973: Northfield GP Surgery ─────────────────────────────────────

    /**
     * Doctor — Dr. Kapoor, the GP at Northfield Surgery.
     * Present behind the surgery door during opening hours (08:00–18:00 Mon–Fri).
     * Passive; never hostile. Delivers diagnosis from the stat-based outcome table.
     * Consultation triggered by pressing E when it is the player's turn.
     */
    DOCTOR(20f, 0f, 0f, false),

    /**
     * Waiting Patient — a generic queuing NPC in the GP Surgery waiting room.
     * 1–5 present during opening hours. Passive; ambient speech lines every 15s.
     * Speech: "Been here since half eight." / "Terrible, the waiting times."
     *         / "Lovely carpet that, in't it." / "He looked right poorly."
     * +1 extra spawn during FROST / COLD_SNAP weather (people warming up).
     */
    WAITING_PATIENT(20f, 0f, 0f, false),

    /**
     * Pharmacist — behind the pharmacy hatch at Northfield Surgery.
     * Open 08:30–18:00. Dispenses medicine when the player presents a PRESCRIPTION.
     * Calls police and adds +1 Wanted star on PRESCRIPTION_FRAUD detection.
     * Passive otherwise; never hostile.
     */
    PHARMACIST(20f, 0f, 0f, false),

    // ── Issue #975: Northfield Post Office ────────────────────────────────────

    /**
     * Counter Clerk — Maureen, the Post Office counter clerk.
     * Present Mon–Fri 09:00–17:30, Sat 09:00–12:30.
     * Handles benefit book cashing, scratch card sales, and stamp purchases.
     * 40% chance of detecting a stolen BENEFITS_BOOK from a PENSIONER (BENEFITS_FRAUD).
     * Passive; never hostile. Familiar with every regular's face.
     * Speech: "Next please." / "Sign here, love." / "Have you got your book?"
     *         / "I'm going to have to ask you to wait."
     */
    COUNTER_CLERK(20f, 0f, 0f, false),

    // ── Issue #977: Northfield Amusement Arcade ───────────────────────────────

    /**
     * Arcade Attendant — Kevin, a bored teen in a polo shirt.
     * Present 10:00–22:00 daily. Passive; ejects Tier 3+ notoriety players
     * ("You're barred, mate"). Calls police if player tilts a machine 3 times.
     * Operates redemption counter exchanges.
     * Speech: "You breakin' that machine?" / "You're barred, mate."
     *         / "You wanna exchange them tokens?" / "I'm watching you."
     */
    ARCADE_ATTENDANT(25f, 5f, 1.5f, false),

    /**
     * Arcade Kid — bored school kid hanging around the machines.
     * 2–4 present 15:00–21:00 (truant variants 10:00–15:00).
     * Truant variant: if player is seen encouraging them, +2 Notoriety.
     * One may be a Street Lads runner (Respect ≥ 60) who gives 1 free TWOPENCE per visit.
     * Speech: "Watch this!" / "Give us a go." / "I've got loads of tokens."
     */
    ARCADE_KID(15f, 0f, 0f, false),

    // ── Issue #979: Fix My Phone — Phone Repair Shop ─────────────────────────

    /**
     * Phone Repair Man — Tariq, proprietor of Fix My Phone (PHONE_REPAIR landmark).
     * Present Mon–Sat 09:00–18:00. Passive; offers repair, cloning (back room, Marchetti
     * Crew Respect ≥ 50), recycling, and shift work services. At Notoriety Tier 3+ adds a
     * 2-COIN "discretion surcharge" on all services. Faction-aware: Marchetti unlocks back
     * room; Street Lads deliver free BROKEN_PHONE daily; Council can shutter the shop.
     * Speech: "What's wrong with it?" / "Give us ten minutes." /
     *         "That's not exactly yours, is it." / "I don't want to know, yeah?"
     */
    PHONE_REPAIR_MAN(20f, 0f, 0f, false),

    // ── Issue #983: Northfield Dog Track ──────────────────────────────────────

    /**
     * Kennel Hand — the overnight kennel worker at the Northfield Dog Track.
     * Present from 17:00 until 23:30. Patrols the kennel block on a 4-block route.
     * Can be bribed (10 COIN) to guarantee a specific dog loses the next race.
     * If the player approaches with a LOCKPICK or DODGY_PIE and the kennel hand
     * is within 8 blocks, they call SECURITY_GUARD and add RACE_FIXING to the record.
     * Speech: "Keep away from them dogs." / "She's in good form tonight."
     *         / "I didn't see nuffin, right?" / "Nice earner, that."
     */
    KENNEL_HAND(20f, 0f, 0f, false),

    /**
     * Tote Clerk — the betting window operator at the Northfield Dog Track.
     * Present during open hours (18:00–23:00 evenings, Saturday 13:00–19:00).
     * Sells RACE_CARD for 1 COIN. Accepts greyhound bets via GreyhoundRacingSystem.
     * At MARCHETTI_CREW Respect >= 60, provides one insider tip per session (naming
     * the dog least likely to win, for fixing purposes).
     * Speech: "What dog, love?" / "Place your bets." / "Eyes on the traps."
     *         / "That one's got form, I'm telling ya."
     */
    TOTE_CLERK(20f, 0f, 0f, false),

    /**
     * Track Punter — a betting regular at the Northfield Dog Track.
     * 4–6 spawn per session during open hours. Wanders between TOTE_CLERK and trackside.
     * Speech: "Come on, Trap 4!" / "Useless mutt." / "I had the winner last week."
     *         / "Rigged, that is." / "One more and I'm done."
     * Passive; never hostile. Crowd noise level 3 generated while 4+ are present;
     * this masks stealth actions.
     */
    TRACK_PUNTER(20f, 0f, 0f, false),

    /**
     * Security Guard — a burly security operative on 90-second patrol around the track.
     * Present from 18:00 until 00:00. Has a predictable patrol path (4 waypoints).
     * Confronts players holding LOCKPICK or DODGY_PIE within 5 blocks: calls police,
     * adds RACE_FIXING or ANIMAL_THEFT to criminal record, Notoriety +10.
     * Will not actively chase the player but will block kennel access.
     * Speech: "You're not meant to be back here." / "Move along, mate."
     *         / "I'm calling it in." / "Track's for spectators only after nine."
     */
    SECURITY_GUARD(35f, 6f, 2.0f, false),

    // ── Issue #985: Northfield Police Station ─────────────────────────────────

    /**
     * Duty Sergeant — staffs the reception desk at Northfield Police Station 24/7.
     * Accepts bail payments, handles the Tip Off menu, and can be bribed by players
     * with MARCHETTI_CREW Respect ≥ 70 (20 COIN reduces charges by 1 tier).
     * Passive unless player is in cell or commits a crime in reception.
     */
    DUTY_SERGEANT(40f, 8f, 1.5f, false),

    /**
     * Detention Officer — manages the custody suite 06:00–22:00.
     * Processes arrivals (speech: "Empty your pockets. All of it."), fingerprints,
     * and patrols the custody corridor. Drops CUSTODY_KEY_CARD (5% chance) on defeat,
     * may also drop POLICE_JACKET.
     */
    DETENTION_OFFICER(50f, 10f, 1.0f, false),

    /**
     * Detective — works in the CID office (upper floor) 09:00–18:00.
     * Patrols the station if a station-wide alert is active. Catching the player
     * inside the evidence locker adds EVIDENCE_TAMPERING to criminal record and
     * triggers WantedSystem +2 stars.
     */
    DETECTIVE(45f, 8f, 1.2f, false),

    // ── Issue #1213: Northfield Police Station — Custody Suite ────────────────

    /**
     * Desk Sergeant Dave — staffs the reception counter 08:00–20:00.
     * Passive if player Notoriety &lt; 200. At 200+ demands ID and runs a check;
     * if WantedSystem stars ≥ 1, immediately arrests. Bribeable for 25 COIN at
     * Notoriety ≤ 400 (lowers Wanted stars by 1, seeds POLICE_CORRUPTION rumour).
     */

    /**
     * Community Liaison Officer Sandra — PCSO subtype present Mon/Wed/Fri 09:00–16:00.
     * Can clear 1 minor offence (PETTY_THEFT or FARE_EVASION) from CriminalRecord per
     * in-game week if player Community Respect ≥ 20.
     * Speech: "We like to give people a second chance around here."
     */
    CLO(25f, 0f, 0f, false),

    // ── Issue #998: Northfield Aldi Supermarket ────────────────────────────────

    /**
     * Shop Assistant — Bev at the Aldi checkout. Staffs the CHECKOUT_PROP
     * 08:00–22:00. Accepts payment for basket items; alerted by
     * self-checkout failure. Passive unless Dave (SECURITY_GUARD) is
     * already hostile.
     * Speech: "That's your lot, love." / "Do you want a bag?" /
     *         "Oi — did that go through?" / "I'm calling Dave."
     */
    SHOP_ASSISTANT(20f, 0f, 0f, false),

    // ── Issue #1002: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Kiosk Cashier — staffs the BP petrol station kiosk CHECKOUT_PROP 06:00–00:00.
     * Sells PETROL_CAN, ENERGY_DRINK, CHOCOLATE_BAR, NEWSPAPER, SCRATCH_CARD,
     * PASTY (06:00–14:00), and DISPOSABLE_LIGHTER. Refuses service at Notoriety ≥ 60.
     * Enters CHASING state for 20s when pump-and-walk theft is detected.
     * Speech: "Oi! Come back here!" / "I'm not serving you. You know why."
     */
    CASHIER(20f, 0f, 0f, false),

    // ── Issue #1006: Angel Nails & Beauty ─────────────────────────────────────

    /**
     * Nail Tech — salon staff member at Angel Nails &amp; Beauty (NAIL_SALON landmark).
     * Two named instances: "Tracy" at the front desk and "Jade" in the back chair.
     * Passive; provides beauty services, gossip, and (for Jade) cash-washing.
     * Refuses standard service to players with Notoriety ≥ 60 (Tracy only).
     * Jade will serve high-notoriety players if STREET_LADS Respect ≥ 40.
     * Speech: "You alright, love?" / "Sit down, I'll be with you in a sec."
     *         / "Sorry love, we're fully booked." / "What colour are we going for?"
     */
    NAIL_TECH(20f, 0f, 0f, false),

    // ── Issue #1008: St. Mary's Church ────────────────────────────────────────

    /**
     * Vicar — Reverend Dave, the parish vicar of St. Mary's Church.
     * Present 09:00–19:00 daily. Stands at PULPIT_PROP during services.
     * Passive; greets players, shares NEIGHBOURHOOD/SHOP_NEWS rumours on E-interaction.
     * Operates the soup kitchen Mon/Thu 12:00–14:00. Never hostile.
     * Speech: "God loves everyone, even you." / "Service starts at ten, if you're interested."
     *         / "Soup's on — come and get it while it's hot."
     */

    // ── Issue #1012: Skin Deep Tattoos ────────────────────────────────────────
    /** Kev — taciturn ex-con running Skin Deep Tattoos. Passive, speech-rich. */
    TATTOOIST(40f, 5f, 2.0f, false),

    // ── Issue #1016: Northfield Canal ─────────────────────────────────────────
    /**
     * Canal Boat Owner — narrowboat resident; passive; rumour source; present limited hours.
     * Derek (east boat): present 07:00–22:00; sells DINGHY for 15 COIN.
     * Maureen (west boat): present 09:00–20:00; feeds ducks; rewards fish catches.
     */
    CANAL_BOAT_OWNER(30f, 0f, 0f, false),

    // ── Issue #1018: Northfield Poundstretcher ────────────────────────────────
    /**
     * Sharon — Pound Shop Manager; runs the Poundstretcher on the high street.
     * Present Mon–Sat 08:30–18:30, Sun 09:30–16:30 (opens early for setup).
     * Patrols in a 12-block route around the shop floor and stockroom.
     * Passive until shoplifting is detected; then calls SHOP_WORKER and may escalate to police.
     * Gives LOCALS faction discount (−1 COIN on all items) at Respect ≥ 60.
     * Speech: "Mind how you go." / "Wednesday's the best day for deals." / "Don't make a mess."
     */
    POUND_SHOP_MANAGER(40f, 6f, 2.0f, false),

    /**
     * Shop Worker — floor staff at Poundstretcher.
     * Present during opening hours; patrols shelves and stockroom.
     * Passive; speed-walks toward suspected shoplifter on alert from Sharon.
     * Speech: "Can I help you with something?" / "Everything's priced to clear."
     */
    SHOP_WORKER(25f, 4f, 2.0f, false),

    // ── Issue #1020: Northfield Sporting & Social Club ────────────────────────

    /**
     * Derek — Chairman of Northfield Sporting &amp; Social Club.
     * Runs the Thursday quiz night; chairs the Saturday AGM; conducts the
     * protection payment handover to the Marchetti Crew on Sunday evenings.
     * Passive unless committee conspiracy is exposed. Speech-rich.
     * Speech: "Right then, let's get on with it." / "Membership's not free, you know."
     *         / "Any Other Business?" / "Question 4: Capital of Peru."
     */
    SOCIAL_CLUB_CHAIRMAN(35f, 5f, 2.0f, false),

    /**
     * Keith — barman at the Northfield Sporting &amp; Social Club.
     * Serves BITTER, MILD, and LAGER_TOP from 12:00–23:00 daily.
     * Refuses service to KNOCK_OFF_TRACKSUIT wearers and Wanted Tier 2+.
     * Passive unless refused-patron altercation escalates.
     * Speech: "Right, what'll it be?" / "We don't serve that sort in here."
     *         / "Lager top? Seriously?"
     */
    SOCIAL_CLUB_BARMAN(30f, 4f, 2.0f, false),

    /**
     * Member — a regular club member; plays darts, attends quiz night, drinks at the bar.
     * Can challenge player to a darts game (5 COIN pot).
     * Votes at AGM; gossips about the committee conspiracy if trust ≥ 60.
     * Passive unless provoked.
     */
    MEMBER(25f, 3f, 2.0f, false),

    // ── Issue #1022: Northfield GP Surgery ────────────────────────────────────

    /**
     * Brenda — the GP surgery receptionist.
     * Gatekeeps entry; refuses the player if Notoriety ≥ 70.
     * Calls POLICE on surgery raids. Present Mon–Sat 08:00–17:59.
     * Speech: "Have you got an appointment?" / "You'll need to take a seat."
     *         / "I'm calling the police!" (on raid)
     */
    GP_RECEPTIONIST(20f, 0f, 0f, false),

    /**
     * Dr. Nair — the GP.
     * Issues SICK_NOTE, PRESCRIPTION_MEDS, ANTIDEPRESSANTS, or REFERRAL based on
     * player health and need states. Seeds LOCAL_HEALTH rumour weekly.
     * Present Mon–Fri 09:00–17:30. Passive.
     * Speech: "Right, what seems to be the trouble?" / "I'm prescribing you some rest."
     */
    GP_DOCTOR(30f, 0f, 0f, false),

    /**
     * Nurse Pat — surgery nurse for injections and blood pressure checks.
     * Present Mon/Wed/Fri 09:00–13:00. Offers flu jabs.
     * Passive. Speech: "Just a little scratch." / "Blood pressure's a bit high."
     */
    GP_NURSE(20f, 0f, 0f, false),

    /**
     * Patient — a waiting-room NPC at the GP surgery.
     * 2–5 present at any time; randomly assigned needs (40% fetch-prescription quest).
     * Shares rumours with nearby NPCs. Passive.
     * Speech: "Been waiting forty minutes." / "You got an appointment?"
     */
    GP_PATIENT(15f, 0f, 0f, false),

    // ── Issue #1026: Northfield Scrapyard ────────────────────────────────────

    /**
     * Scrapyard Owner — Gary Pearce, runs the weigh-bridge and yard office.
     * Present 09:00–17:00. Buys scrap by weight; refuses copper if Notoriety ≥ 50.
     * Speech: "By the kilo, son — none of this individual-piece nonsense."
     */
    SCRAPYARD_OWNER(35f, 5f, 2.0f, false),

    /**
     * Scrapyard Worker — Kyle, operates the crusher and warns about police.
     * Present 08:00–18:00. Charges 1 COIN per item crushed.
     * 20% chance to warn player if police are nearby and his respect is ≥ 10.
     */
    SCRAPYARD_WORKER(30f, 5f, 2.0f, false),

    /**
     * Guard Dog — Tyson the Rottweiler, night patrol 20:00–07:00.
     * Attacks player on sight unless player carries SAUSAGE_ROLL.
     * Distracted for 30 seconds by SAUSAGE_ROLL. Cannot be adopted.
     * Speech: [growl]
     */
    GUARD_DOG(40f, 12f, 1.5f, true),

    // ── Issue #1028: Northfield Cash Converters ───────────────────────────────

    /**
     * Cash Converter Manager — Dean, the store manager at Northfield Cash Converters.
     * Present Mon–Sat 09:00–17:30. Passive; runs serial number checks on high-value
     * electronics (GAMES_CONSOLE, LAPTOP, STOLEN_PHONE, TABLET). At Notoriety ≥ 40
     * calls police when a stolen item is detected. Briabable at Notoriety ≥ 30 (5 COIN).
     * If CCTV_PROP is broken before a sale, skips police call.
     * Speech: "What you got for me?" / "Serial number, have to check that."
     *         / "I'm going to have to ask you to leave." / "Cash in hand, no questions."
     */
    CASH_CONVERTER_MANAGER(25f, 0f, 0f, false),

    /**
     * Dave the Middleman — operates in the back alley behind Cash Converters 22:00–02:00.
     * Buys stolen electronics at 30% of Dean's price, no questions asked.
     * References Fence rep (FENCE rep ≥ 10 required).
     * Passive; despawns at 02:00.
     * Speech: "Got something for me?" / "That's hot, innit." /
     *         "Don't bring it back if it's got a tracking chip, yeah?"
     */
    DAVE_MIDDLEMAN(20f, 0f, 0f, false),

    // ── Issue #1030: Al-Noor Mosque ────────────────────────────────────────────

    /**
     * Imam Hassan — leads the five daily prayers and Friday Jumu'ah at Al-Noor Mosque.
     * Present 08:00–21:00 daily. Passive; never hostile.
     * Offers rumours, sanctuary advice, and FLATBREAD to hungry players.
     * Speech: "Salaam alaikum." / "You're welcome here, friend."
     *         / "Friday prayers are at one o'clock, if you'd like to attend."
     */
    IMAM(20f, 0f, 0f, false),

    /**
     * Worshipper — attends the five daily prayers and Friday Jumu'ah.
     * Kneels on PRAYER_MAT_PROP during prayer; shares NEIGHBOURHOOD rumours after Jumu'ah.
     * Passive; 4–8 present during regular prayer, 10–16 during Friday Jumu'ah.
     * Speech: "Jumu'ah today — are you joining us?" / "Peace be with you."
     */
    WORSHIPPER(18f, 0f, 0f, false),

    // ── Issue #1033: St. Aidan's C.E. Primary School ─────────────────────────

    /**
     * Mrs Fowler — the headteacher. Ejects adults; calls police on Notoriety ≥ 30.
     * Active 08:00–16:30 (Mon–Fri). Patrols reception and corridor.
     */
    HEADTEACHER(20f, 0f, 0f, false),

    /**
     * Dot — the dinner lady. Sells SCHOOL_DINNER 11:30–13:30; hostile to high-Notoriety players.
     * Pickpocketable for 2–5 COIN; calls HEADTEACHER if caught.
     */
    DINNER_LADY(20f, 0f, 0f, false),

    /**
     * Derek — the caretaker. Patrols exterior; sweeps playground; locks gate at 15:30.
     * Shed key pickpocketable at Notoriety &lt; 30. Friendly if low notoriety.
     */
    CARETAKER(25f, 4f, 2.0f, false),

    /**
     * A gossiping parent at the school gate during the school run (08:15–08:45, 15:00–15:30).
     * Proximity-rumour source: player within 2 blocks receives 1 NEIGHBOURHOOD rumour per day per mum.
     */
    SCHOOL_MUM(18f, 0f, 0f, false),

    /**
     * The headteacher's secretary. Staffs reception Mon–Fri 08:00–16:30.
     * Passive unless player enters interior; calls HEADTEACHER on intrusion.
     */
    HEADTEACHER_SECRETARY(18f, 0f, 0f, false),

    /**
     * Issue #1237: Ofsted Inspector — arrives in pairs Mon morning (15% chance).
     * Present 09:00–14:00. Passive unless a NoiseSystem event ≥ magnitude 60 occurs,
     * causing them to flee. Immune to disguise (professional background check).
     * Notoriety ≥ 3: becomes suspicious after 5 minutes.
     */
    OFSTED_INSPECTOR(20f, 0f, 0f, false),

    // ── Issue #1037: Northfield Indoor Market ─────────────────────────────────

    /**
     * Market Trader — one of four stall holders at the Northfield Indoor Market.
     * Dave (electronics), Sheila (clothes), Mo (knock-offs), Brenda (hot food).
     * Each occupies one stall; sells 3–5 items. Press E to open buy menu.
     * Present on market days (Tue/Fri/Sat) 08:00–16:00 only.
     */
    MARKET_TRADER(20f, 0f, 0f, false),

    /**
     * Market Punter — generic PUBLIC crowd at the indoor market.
     * Spawns in groups of 8–12 on market days. Carries 1–5 COIN.
     * Browses player stall every 2 in-game minutes; pickpocketable with crowd cover.
     * Despawns when market closes or during a Trading Standards raid.
     */
    MARKET_PUNTER(18f, 0f, 0f, false),

    /**
     * Ray — the Market Manager. Stationed near the entrance.
     * Handles stall rentals (3 COIN/day). Refuses Notoriety Tier ≥ 4 players.
     * During a Trading Standards raid, calls police if player stall contains contraband.
     * Speech: "You after a stall?" / "I know your sort. Take it elsewhere."
     */
    MARKET_MANAGER(22f, 0f, 0f, false),

    /**
     * Trading Standards Officer — spawns at the entrance during a raid event.
     * Walks each stall in sequence; confiscates contraband items.
     * 2 spawn per raid; despawn after 10 in-game minutes.
     * Hostile only if player is adjacent to a stall with contraband after warning period.
     */
    TRADING_STANDARDS(25f, 5f, 2.0f, false),

    // ── Issue #1039: Northfield Barber ────────────────────────────────────────

    /**
     * Barber Owner — Kosta, the proprietor of Kosta's Barbers.
     * Present Mon–Sat 09:00–18:00. Offers all cuts from the service menu.
     * Shares LOCAL_EVENT rumours with the player on each visit.
     * Speech: "Sit down, mate — what are we doing today?"
     * At Marchetti Crew Respect ≥ 75, provides one free trim per session.
     * Refuses service if the player has ≥ 2 wanted stars.
     */
    BARBER_OWNER(20f, 0f, 0f, false),

    /**
     * Barber Apprentice — Wayne, Kosta's trainee.
     * Present Tue–Fri 12:00–18:00. Offers Trim only (cheaper, slower).
     * Slower service: cuts take +50% longer. Refused at ≥ 2 wanted stars.
     * Speech: "Yeah, no problem mate, I'll sort you out."
     */
    BARBER_APPRENTICE(18f, 0f, 0f, false),

    // ── Issue #1041: Northfield Argos ─────────────────────────────────────────

    /**
     * Argos Clerk — staffs the collection counter at Argos (ARGOS landmark).
     * Present Mon–Sat 09:00–17:30, Sun 10:00–16:00.
     * Receives order slips from players, assigns collection numbers, and calls
     * orders when stock is retrieved. Refuses service at ≥ 2 wanted stars.
     * Processes returns at ARGOS_RETURNS_DESK: 60% approval for own purchases,
     * 30% for others', 5% for stolen goods.
     * Speech: "Can I take your slip?" / "Your number is 347." /
     *         "That's not what you paid for, is it." / "Sorry, system's down."
     */
    ARGOS_CLERK(20f, 0f, 0f, false),

    /**
     * Argos Manager — spawned when stolen returns are detected (30% chance on
     * stolen goods return attempt). Also manages the SYSTEM_DOWN chaos event.
     * Present during opening hours; otherwise spawned dynamically.
     * Passive unless a theft is in progress; then calls police (+1 wanted star,
     * THEFT in criminal record).
     * Speech: "I'm going to have to ask you to come with me." /
     *         "We've got a situation at the returns desk." /
     *         "The system's down — everyone out, please."
     */
    ARGOS_MANAGER(30f, 5f, 2.0f, false),

    /**
     * Argos Shopper — a member of the public browsing catalogues or waiting
     * in the collection area. 3–5 present during opening hours.
     * Seated shoppers gossip, seeding RumourType.LOCAL_EVENT (25% chance/NPC).
     * Passive; despawns at closing time.
     * Speech: "It's always the big stuff that takes ages." /
     *         "I've been waiting twenty minutes for a toaster." /
     *         "They said five minutes twenty minutes ago."
     */
    ARGOS_SHOPPER(18f, 0f, 0f, false),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1043: Northfield Fire Station NPCs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Watch Commander — senior officer at the fire station.
     * Stationed in the WATCH_ROOM. Comes out when alerted.
     * Cannot be bribed. Calls police if the fire engine is stolen.
     * Accepts ANTIDEPRESSANTS delivery quest.
     * Sees through FIREFIGHTER_HELMET disguise within 3 blocks.
     * Speech: "That engine better be back in the bay by end of shift." /
     *         "Oi — what d'you think you're doing?" /
     *         "Cheers — the lads have been struggling."
     */
    WATCH_COMMANDER(50f, 8f, 2.0f, false),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1045: Northfield Council Flats NPCs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Flat Resident — a council estate resident NPC.
     * Emerges from their flat 06:00–23:00 and wanders between the lobby,
     * corner shop, and park. Returns home at 23:00.
     * If the lift is broken, mutters "That bloody lift!"
     * Passive; never hostile unless provoked.
     */
    FLAT_RESIDENT(20f, 0f, 0f, false),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1061: Northfield Community Centre NPCs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Boxing Coach — Ray, runs the Boxing Club at the Community Centre.
     * Active Mon/Wed/Fri 18:00–21:00. Supervises training at BOXING_BAG_PROP
     * and sparring at BOXING_RING_PROP.
     * Passive; never hostile unless provoked.
     */
    BOXING_COACH(25f, 0f, 0f, false),

    // ── Issue #1071: Northfield Fast Cash Finance ─────────────────────────
    /**
     * Loan Manager — Barry; anchored to LOAN_DESK_PROP inside Fast Cash Finance.
     * Offers tiered loan products based on player's Notoriety tier.
     * Turns hostile (speech bubble "You're a wrong 'un") after a missed repayment.
     * Passive by default; never attacks.
     */
    LOAN_MANAGER(25f, 0f, 0f, false),

    /**
     * Bailiff — spawned on BAILIFF_THREAT (2 missed repayments).
     * Pathfinds to the player's registered squat and knocks on the door.
     * Can be paid off, bribed, or attacked.
     * Moderately tough; will defend itself if attacked.
     */
    BAILIFF(35f, 8f, 1.5f, false),

    // ── Issue #1079: Northfield Magistrates' Court ─────────────────────────

    /**
     * Magistrate Sandra Pemberton — presides over the bench at Northfield Magistrates' Court.
     * Severe disposition; hates time-wasters; passive unless directly addressed.
     * Delivers charges, hears pleas, and pronounces sentence. Never hostile.
     */
    MAGISTRATE(30f, 0f, 0f, false),

    /**
     * CPS Prosecutor Martin Gale — reads charges and presents evidence for the Crown.
     * Can be bribed (20 COIN) to drop a charge before the hearing begins.
     * Passive; speech-rich. Despawns after sentencing.
     */
    CPS_PROSECUTOR(25f, 0f, 0f, false),

    /**
     * Duty Solicitor Donna — the player's court-appointed legal representative.
     * Available for 5 COIN at the court entrance; reduces sentence tier by one if engaged.
     * Passive; never hostile. Shares rumours about the local justice system.
     */
    DUTY_SOLICITOR(25f, 0f, 0f, false),

    /**
     * Court Usher Trevor — manages the waiting area and announces when the bench is ready.
     * Can be bribed (3 COIN) to delay the session by one in-game hour.
     * Passive; never hostile. Present whenever the court building is open.
     */
    COURT_USHER(20f, 0f, 0f, false),

    // ── Issue #1081: Northfield Pet Shop & Vet — Paws 'n' Claws ─────────────

    /**
     * Bev — the proprietor of Paws 'n' Claws pet shop.
     * Present Mon–Sat 09:00–17:30. Cheerful and chatty; comments on the dog's breed.
     * Sells dogs (8–15 COIN), DOG_TREATS (1 COIN), DOG_LEAD (2 COIN), BUDGIE (3 COIN),
     * GOLDFISH (1 COIN). At Notoriety Tier ≥ 3: refuses service.
     * At Marchetti Crew Respect ≥ 50: offers the dodgy pedigree breeding mission.
     * Speech: "Aren't they lovely?" / "He'll need feeding every day, mind."
     *         / "I'm not selling you a dog, love. I've seen the paper."
     */
    PET_SHOP_OWNER(20f, 0f, 0f, false),

    /**
     * Dr. Patel — the vet at Northfield Vets (adjoining Paws 'n' Claws).
     * Present Mon–Fri 08:30–18:00, Sat 09:00–13:00. Professional and harried.
     * Offers consultations: dog health check (5 COIN), pigeon tune-up (4 COIN),
     * dog vaccinations (8 COIN), emergency (12 COIN).
     * If player's dog is injured (HP &lt; 50%): "He's taken a knock, hasn't he? Bring him in."
     * Seeds LOCAL_HEALTH rumour after each emergency visit. Passive; never hostile
     * unless medicine cabinet is raided in his presence (calls police).
     * Speech: "Right, what are we dealing with today?" / "That'll be four pounds, please."
     *         / "Keep him warm and give him plenty of water."
     */
    VET(20f, 0f, 0f, false),

    // ── Issue #1085: Northfield Internet Café — Cybernet ─────────────────────

    /**
     * Asif — the owner of Cybernet internet café. Calm, seen-it-all attitude.
     * Present daily 09:00–18:00. Sells terminal time (1 COIN = 10 min), enforces
     * the no-food rule, and will call police if props are damaged.
     * At Notoriety ≥ 60: "I know who you are, mate — pay upfront, no trouble."
     * At Notoriety ≥ 80: refuses service entirely.
     * Watches for printer fraud 09:00–18:00; 10% base chance to detect.
     * Speech: "Terminal's 1 coin a session, mate." / "I didn't see nothin'."
     */
    INTERNET_CAFE_OWNER(20f, 0f, 0f, false),

    /**
     * Hamza — Asif's teenage nephew, evening shift assistant at Cybernet.
     * Present 18:00–23:00. Bored, on his phone; shares LOCAL_EVENT rumours
     * freely if player buys him an energy drink from the vending machine.
     * 25% chance to notice and report printer abuse (evening only).
     * If tipped (energy drink received): seeds LOCAL_EVENT rumour.
     * Speech: "Cheers mate." / "I'll put the word out." / "Don't tell my uncle."
     */
    INTERNET_CAFE_ASSISTANT(20f, 0f, 0f, false),

    // ── Issue #1087: The Vaults Nightclub ─────────────────────────────────────

    /**
     * DJ Mikey — energetic DJ on the decks at The Vaults nightclub.
     * Present Thu–Sun 22:00–03:00. Comments on crowd size.
     * At crowd {@literal <} 5: "It's dead in here, man."
     * At crowd ≥ 15: "This place is jumping!" — seeds LOCAL_EVENT rumour.
     * When active, {@code RaveSystem.isVaultsDjPresent()} returns true.
     */
    DJ(20f, 0f, 0f, false),

    /**
     * Chantelle — barmaid at The Vaults nightclub. Quick, no-nonsense.
     * Refuses service when player DrunkenessLevel ≥ 4:
     * "You've had enough, babe."
     * Present Thu–Sun 22:00–03:00.
     */
    BARMAID(20f, 0f, 0f, false),

    /**
     * The Dealer — drug dealer who loiters in the club toilets 22:00–02:30.
     * Sells PILLS ×2 for 4 COIN. Flees on police arrival.
     * Each purchase triggers a 10% undercover bust check.
     */
    DEALER(20f, 0f, 0f, false),

    /**
     * Tony Marchetti — senior crime boss. Present at The Vaults VIP booth
     * Fri–Sat 23:00–01:00 at MARCHETTI_CREW Respect ≥ 50.
     * Offers the phone-tracker side mission.
     * Also used in other Marchetti faction contexts.
     */
    CRIME_BOSS(40f, 8f, 2.0f, false),

    // ── Issue #1091: Northfield Nando's ─────────────────────────────────────

    /**
     * Kezia — Nando's counter staff.
     * Staffs the NANDOS_COUNTER_PROP during opening hours (11:00–23:00).
     * Passive; serves food via NandosOrderUI. Refuses service at Notoriety ≥ 60.
     * Notoriety-aware dialogue. Aware of card machine drama.
     * Speech: "Hiya, what can I get you?" / "Card machine's playing up, sorry."
     *         / "Need a moment? Take your time." / "Enjoy your meal!"
     */
    NANDOS_STAFF(20f, 0f, 0f, false),

    /**
     * Dave — the Nando's manager.
     * Present 12:00–22:00 Mon–Sun. Patrols the restaurant and manager's office.
     * Calls police if player is spotted near the SAFE_PROP (adds +12 Notoriety).
     * Passive otherwise; never attacks. Notoriety-aware.
     * Speech: "Can I help you?" / "Staff only back there, mate."
     *         / "Oi — that's the manager's office!" / "I'm calling the police."
     */
    NANDOS_MANAGER(30f, 0f, 0f, false),

    // ── Issue #1094: Northfield By-Election ──────────────────────────────────

    /**
     * Nigel Pemberton — Conservative Party candidate.
     * Canvasses door-to-door 10:00–16:00 with a clipboard volunteer.
     * Press E to interact: support (+3 votes), argument (−2 votes), ignore.
     * Passive; never hostile. Wears a blue rosette.
     */
    TORY_CANDIDATE(25f, 0f, 0f, false),

    /**
     * Sandra Okafor — Labour Party candidate.
     * Canvasses door-to-door 14:00–19:00.
     * Press E to interact: support (+3 votes), argument (−2 votes), ignore.
     * Passive; never hostile. Wears a red rosette.
     */
    LABOUR_CANDIDATE(25f, 0f, 0f, false),

    /**
     * The Returning Officer — official who oversees the election process.
     * Stationed at the RETURNING_OFFICER_PODIUM_PROP.
     * If Community Respect ≥ 40 and Notoriety ≤ 20, offers the player
     * a NOMINATION_FORM to stand as Independent candidate.
     * Passive; never hostile.
     */
    RETURNING_OFFICER(20f, 0f, 0f, false),

    /**
     * Party Volunteer — clipboard-carrying helper at the CANVASSING_TABLE_PROP.
     * Press E on the table to receive 10 CAMPAIGN_LEAFLET items.
     * Passive; never hostile. Accompanies Nigel Pemberton on canvassing runs.
     */
    PARTY_VOLUNTEER(20f, 0f, 0f, false),

    // ── Issue #1096: Sunday League Football ──────────────────────────────────

    /**
     * The Referee — patrols the pitch during the Sunday League match.
     * Issues yellow/red cards to players who foul or abuse.
     * Calls police immediately if assaulted (WantedSystem +2 stars, Notoriety +8).
     * Passive under normal circumstances; never hostile.
     * Drops REFEREE_WHISTLE on destruction (10% chance).
     */
    REFEREE(20f, 0f, 0f, false),

    // ── Issue #1112: The Raj Mahal — Friday Night Curry ──────────────────────

    /**
     * Bashir — the proprietor of The Raj Mahal curry house (CURRY_HOUSE landmark).
     * Stands at the front desk Tue–Sun during opening hours (17:00–23:30).
     * Passive; greets diners and manages front-of-house.
     * Notoriety-aware: refuses service at Notoriety ≥ 70.
     * Speech: "Welcome to the Raj Mahal." / "Table for one, is it?"
     *         / "We're closing in ten minutes, sir." / "I've seen the papers."
     */
    CURRY_HOUSE_OWNER(20f, 0f, 0f, false),

    /**
     * Sanjay — a waiter at The Raj Mahal curry house.
     * Patrols the dining tables. Enforces the BYO corkage rule (1 COIN or confiscation).
     * Passive; never hostile unless back room is trespassed without faction access.
     * Speech: "Ready to order?" / "Another naan, sir?" / "That's not on the menu, mate."
     *         / "Back room's private — members only."
     */
    CURRY_WAITER(20f, 0f, 0f, false),

    // ── Issue #1114: Meredith & Sons Funeral Parlour ──────────────────────────

    /**
     * Undertaker — Gerald Meredith, proprietor of the funeral parlour.
     * Thin, black-suited; anchored near the front desk during open hours.
     * Refuses service if player Notoriety ≥ 60. Seeds LOCAL_EVENT rumours about
     * recently deceased residents.
     */
    UNDERTAKER(20f, 0f, 0f, false),

    /**
     * Funeral Assistant — Dawn, Gerald's assistant.
     * Handles paperwork and flower arrangements; patrols the viewing room.
     * Gossips freely about deceased estates; seeds INHERITANCE and WILL_LOCATION rumours.
     * Calls police if the casket is found disturbed.
     */
    FUNERAL_ASSISTANT(20f, 0f, 0f, false),

    // ── Issue #1122: Sun Kissed Studio ────────────────────────────────────────

    /**
     * Salon Owner — Tracey, proprietor of Sun Kissed Studio.
     * Anchored near the reception desk during open hours.
     * Accepts Marchetti cash drops at 11:00 and 18:00 daily.
     * Refuses service if player is GRASS (Marchetti Respect < 10).
     */
    SALON_OWNER(20f, 0f, 0f, false),

    /**
     * Massage Therapist — Jade and Tanya, the therapists at Sun Kissed Studio.
     * Patrol between massage tables and the reception area.
     * Jade gossips on rainy days, seeding LOCAL_EVENT rumours.
     * Tanya manages special service bookings for Street Rep ≥ 40.
     */
    MASSAGE_THERAPIST(20f, 0f, 0f, false),

    // ── Issue #1124: Salvation Army Citadel ───────────────────────────────────

    /** Major Eileen Webb — runs the Citadel, offers tea at Wanted Tier 1–2, calls police at Tier ≥ 3. */
    SALVATION_ARMY_OFFICER(25f, 0f, 0f, false),

    /** Saturday Brass Band members who march with Eileen to the high-street junction. */
    BRASS_BAND_MEMBER(20f, 0f, 0f, false),

    /** Rough sleepers who path between park / Greggs / Citadel / sewer; driven indoors by cold weather. */
    ROUGH_SLEEPER(15f, 0f, 0f, false),

    // ── Issue #1128: Northfield NHS Walk-In Centre ─────────────────────────────

    /**
     * Triage Nurse Brenda — stationed at TRIAGE_DESK_PROP, 08:00–22:00 Mon–Sat
     * and 10:00–18:00 Sun. Assesses incoming patients and assigns queue position.
     * Calls police immediately if she hears a lockpick near the medicine room.
     * Will not attack player directly.
     */
    TRIAGE_NURSE(30f, 0f, 0f, false),

    /**
     * Dr. Yusuf Okafor — Walk-In Centre doctor. Moves between TREATMENT_CUBICLE_PROPs,
     * issues DISCHARGE_LETTER on treatment. Passive unless attacked (then calls
     * WantedSystem). Same hours as TRIAGE_NURSE.
     */
    WALK_IN_DOCTOR(30f, 0f, 0f, false),

    /**
     * Paramedic (Andy or Sue) — based at AMBULANCE_BAY_PROP. Responds to NPC
     * casualties (HP ≤ 5, noise ≥ 30): tends injured for 60 seconds then returns.
     * Attacking a paramedic forces WantedSystem to Tier 4 minimum.
     * Has above-average speed for rapid dispatch.
     */
    PARAMEDIC(35f, 5f, 1.5f, false),

    // ── Issue #1130: Northfield BP Petrol Station ──────────────────────────────

    /**
     * Raj (day shift, 06:00–22:00) — runs the BP kiosk. Sells items, manages the
     * till, and triggers panic button at WantedSystem Tier ≥ 2. Refuses service
     * at Wanted Tier ≥ 3. Passive unless attacked or robbery attempted.
     */
    PETROL_STATION_ATTENDANT(25f, 0f, 0f, false),

    /**
     * Wayne (night shift, 22:00–06:00) — bored teenager behind the counter.
     * Sleeps 01:00–03:00 (ignores crimes if NoiseSystem &lt; 15).
     * Activates panic button at Wanted Tier ≥ 2 only when awake.
     * Passive unless attacked.
     */
    PETROL_STATION_ASSISTANT(20f, 0f, 0f, false),

    // ── Issue #1132: Northfield Dog Grooming Parlour — Pawfect Cuts ───────────

    /**
     * Dog Owner — a local resident who visits Pawfect Cuts to have their dog groomed.
     * Spawns outside the grooming parlour during opening hours (09:00–17:00).
     * Passive; shares LOCAL_EVENT rumours about the dog show on approach.
     * Accompanies an ambient DOG NPC on a lead.
     */
    DOG_OWNER(20f, 0f, 0f, false),

    /**
     * Dog Dealer — a black-market pet trader who spawns Tuesday evenings after
     * STREET_LADS Respect ≥ 50. Sells UNLICENSED_DOG items for resale profit.
     * Present 20:00–23:00 on Tuesday evenings near the industrial estate.
     * Passive; flees on police arrival. Refuses service at Notoriety Tier ≥ 4.
     * Speech: "Don't ask about the paperwork." / "Lovely dog, that. No questions."
     *         / "You want papers? I've got papers. Sort of."
     */
    DOG_DEALER(20f, 0f, 0f, false),

    /**
     * Judge NPC — the Northfield Dog Show judge. Presides over the fortnightly
     * dog show at the park. Scores each entry on Bond Level (40%), groom recency
     * (30%), and random factor (30%). On the payroll of the Marchetti Family
     * (revealed during the Crufts Conspiracy quest). Can be bribed for 15 COIN
     * (SHOW_RIGGING +5 Notoriety) or exposed via NewspaperSystem (−15 Marchetti respect).
     * Passive; never attacks. Present only during dog show events.
     * Speech: "Excellent form." / "A fine specimen." / "I've seen better."
     *         / "The judging criteria are entirely above board." (nervous laugh)
     */
    JUDGE_NPC(25f, 0f, 0f, false),

    // ── Issue #1134: Patel's Newsagent ────────────────────────────────────────

    /**
     * Milk Float Driver — an early-morning delivery driver who patrols the residential
     * streets adjacent to Patel's News between 05:30 and 07:00.
     * Passive; delays 10 minutes in FROST weather (FREEZING debuff risk for players nearby).
     * Talks to Norman (INSOMNIAC_PENSIONER) if paths cross.
     * Speech: "Morning!" / "Lovely and cold this morning." / "Last stop, nearly done."
     */
    MILK_FLOAT_DRIVER(20f, 0f, 0f, false),

    /**
     * Insomniac Pensioner (Norman) — a chronic insomniac who loiters outside Patel's
     * News between 05:00 and 08:00. Seeds LOCAL_GOSSIP rumours into the RumourNetwork
     * every 10 in-game minutes. Passive; never hostile.
     * Speech: "Can't sleep again." / "You're up early." / "Did you hear about...?"
     *         / "Been stood here since four. Council should do something about the bins."
     */
    INSOMNIAC_PENSIONER(20f, 0f, 0f, false),

    // ── Issue #1136: The Vaults Nightclub ─────────────────────────────────────

    /**
     * Nightclub Manager — Terry, who manages The Vaults. Present in the back office
     * from 22:30 onwards. Approachable only at FactionSystem respect ≥ 20 (FRIENDLY).
     * Controls the lock-in decision. Calls Big Dave if player is caught in the office
     * without permission. Passive; never attacks.
     * Speech: "You'll need to see me about that." / "Nice one, son. Appreciated."
     *         / "Oi — who let you in here?"
     */
    NIGHTCLUB_MANAGER(30f, 0f, 0f, false),

    /**
     * Nightclub Punter — a regular club-goer at The Vaults. Wanders between the
     * dancefloor and bar. BORED need satisfied by dancing or drink. Pickpocketable
     * (5–8 COIN each). Some become DRUNK after 00:00, increasing pickpocket success
     * chance. Passive by default; may start brawls when DRUNK.
     * Speech: "Alright mate?" / "It's well loud in here." / "Get in!"
     *         / "I've had about seven of them." (slurring)
     */
    NIGHTCLUB_PUNTER(20f, 3f, 1.0f, false),

    /**
     * Drug Dealer NPC — Wayne, who lurks near the toilets 23:00–02:00.
     * Buys PRESCRIPTION_MEDS or TOBACCO_POUCH from the player at 2× street price.
     * Refuses service if WantedSystem tier ≥ 2 and seeds GANG_ACTIVITY rumour.
     * Passive unless attacked. Flees on police arrival.
     * Speech: "You got anything for me?" / "Nah mate, not tonight."
     *         / "Keep it quiet. I'm working here."
     */
    DRUG_DEALER_NPC(20f, 0f, 0f, false),

    // ── Issue #1138: Northfield Iceland ───────────────────────────────────────

    /**
     * Iceland Manager — Debbie, who manages the Iceland on Northfield High Street.
     * Present Mon–Sat 08:00–18:00, Sun 10:00–16:00. Manages the Christmas Club
     * savings scheme (distributes envelopes in December). Passive unless shoplifting
     * is detected; then calls Kevin (ICELAND_SECURITY). Holds the ICELAND_STAFF_KEY.
     * Speech: "Can I help you with something, love?" / "Everything's three for a fiver."
     *         / "Kevin! Kevin, come here please." / "Right, that's it — I'm calling the police."
     */
    ICELAND_MANAGER(25f, 0f, 0f, false),

    /**
     * Iceland Checkout — Sharon, who staffs the Iceland checkout.
     * Present Mon–Sat 08:00–18:00, Sun 10:00–16:00. Processes the three-for-a-fiver deal.
     * Will accept a FAKE_RECEIPT 60% of the time; 100% when Kevin is distracted.
     * Passive unless the self-checkout scam is flagged by Kevin.
     * Speech: "That's your lot, love." / "Three for a fiver on the party food."
     *         / "Have you got your reward card?" / "Oi — that receipt doesn't look right."
     */
    ICELAND_CHECKOUT(20f, 0f, 0f, false),

    /**
     * Iceland Security — Kevin, the self-checkout security guard.
     * Present Mon–Sat 09:00–18:00. Patrols the self-checkout area.
     * 40% detection chance on self-checkout scam attempts; 0% when distracted by PRAWN_RING.
     * Distractable for 30 seconds when a PRAWN_RING is placed near him.
     * Passive until scam detected; then calls Debbie and triggers WantedSystem.
     * Speech: "Excuse me — unexpected item." / "Can you pop that back through, please."
     *         / "Ooh, is that a prawn ring? I love a prawn ring." / "Right, that's it."
     */
    ICELAND_SECURITY(35f, 6f, 1.5f, false),

    // ── Issue #1140: Northfield Flexistaff — Cash-in-Hand Day Labour ───────────

    /**
     * Temp Agency Manager — Darren, the agency owner.
     * Anchored behind the AGENCY_DESK_PROP Mon–Fri 06:30–17:00.
     * Assigns shift jobs to the player. Suspicious at Notoriety Tier ≥ 3.
     * Refuses work if CriminalRecord contains ASSAULT_OF_EMPLOYER.
     * Speech: "Sign here, mate." / "Don't be late or you're off the books." /
     *         "No funny business on site, yeah?"
     */
    TEMP_AGENCY_MANAGER(25f, 0f, 0f, false),

    /**
     * Temp Agency Van Driver — Kev, who drives the AGENCY_VAN_PROP.
     * Spawns at 06:45, departs 07:00 sharp.
     * Can be bribed with 2 COIN for a 5-minute delay.
     * Catches players searching the van glove box (06:50–07:00).
     * Speech: "Get in then." / "Health and safety, yeah, watch your fingers." /
     *         "I'm not supposed to, but go on."
     */
    TEMP_AGENCY_VAN_DRIVER(30f, 5f, 2.0f, false),

    /**
     * Temp Agency Site Foreman — Gavin, on-site overseer during warehouse/construction shifts.
     * Patrols within 10 blocks of WORK_STATION_PROP.
     * Notifies Darren if player is idle > 60 seconds near work station.
     * Present only during active work shifts.
     * Speech: "Look lively." / "Those boxes won't stack themselves." / "I've got my eye on you."
     */
    TEMP_AGENCY_SITE_FOREMAN(30f, 8f, 2.0f, false),

    /**
     * Temp Agency Worker — fellow day labourers (3–5 spawned per shift).
     * Passive; freely share rumours. One carries a PAYSLIP_STUB that can be pickpocketed.
     */
    TEMP_AGENCY_WORKER(20f, 0f, 0f, false),

    // ── Issue #1142: Northfield RAOB Lodge — Buffaloes, Secret Handshakes & Old Boys' Network ──

    /**
     * RAOB Grand Primo — Norman, 64, red-faced ex-plumber who chairs the Lodge.
     * Present Tue/Thu 19:00–23:00 and Sat 12:00–23:00.
     * Runs initiation ceremonies at the LODGE_ALTAR_PROP. Refuses entry to Notoriety Tier ≥3.
     * Carries the RITUAL_BOOK_PROP key. Passive until ceremony or detection.
     * Speech: "Evening all. Drinks are on the Lodge tonight." /
     *         "The rite demands silence, dignity, and one pound fifty for the bar fund." /
     *         "You're not a member. Get out." / "Norman doesn't forget a face, son."
     */
    RAOB_GRAND_PRIMO(50f, 8f, 1.0f, false),

    /**
     * RAOB Treasurer — Keith, jovial 58-year-old accountant who guards the back-room safe.
     * Present Tue/Thu 19:00–23:00 and Sat 12:00–23:00.
     * Patrols within 8 blocks of the LODGE_SAFE_PROP. Can be bribed with a PREMIUM_LAGER_CRATE
     * to look the other way for 120 in-game seconds.
     * Speech: "Books balance to the penny — well, approximately." /
     *         "No one goes in the back room without my say-so." /
     *         "Oh, go on then. One for the road."
     */
    RAOB_TREASURER(40f, 5f, 1.5f, false),

    /**
     * RAOB Doorman — Big Bernard, 52, former bouncer and current Lodge sergeant-at-arms.
     * Guards the Lodge entrance. Checks for RAOB_MEMBERSHIP_CARD.
     * Distracted 20:00–20:30 each session: detection chance drops to 15%.
     * Outside distraction window: detection chance is 70%.
     * Speech: "Members only, pal." / "Card?" / "Evening, Brother." /
     *         "Has anyone seen my phone? I left it somewhere."
     */
    RAOB_DOORMAN(60f, 12f, 1.5f, false),

    /**
     * RAOB Member — one of the four Lodge members with corruption mechanics.
     * Represents one of: Housing Officer Brian, Magistrate Clerk Sandra,
     * Planning Inspector Reg, Bookmaker Terry (distinguished by sub-role in RAOBLodgeSystem).
     * Present Tue/Thu 19:00–23:00 and Sat 12:00–23:00.
     * Each will provide a favour in exchange for the appropriate bribe item.
     * Speech: "A word to the wise, son." / "Keep it between us, yeah?" /
     *         "The Lodge looks after its own." / "What is it you're after, exactly?"
     */
    RAOB_MEMBER(30f, 0f, 0f, false),

    // ── Issue #1144: Northfield Probation Office ──────────────────────────────

    /**
     * Probation Officer — Karen, 45, permanently sceptical case officer.
     * Stationed at PROBATION_DESK_PROP during office hours (09:00–17:00).
     * Steps out 13:00–14:00 (lunch), leaving Debbie at reception distracted.
     * Runs sign-on dialogue trees; fits ELECTRONIC_TAG at 3+ custody entries.
     * Speech: "I need you to be honest with me." / "You're not doing yourself any favours." /
     *         "I'll be frank — this isn't looking good." / "Any new offences to declare?"
     */
    PROBATION_OFFICER(30f, 0f, 0f, false),

    /**
     * Probation Receptionist — Debbie, 38, easily distracted admin.
     * Stationed at the reception desk. During Karen's 13:00–14:00 lunch break,
     * she is distracted by her phone (detection chance −40%).
     * Issues COMMUNITY_SERVICE_VEST to player for service tasks.
     * Speech: "Sign in please." / "Karen'll be with you in a minute." /
     *         "Oh, sorry — hang on, just let me..." / "Take a seat in the waiting area."
     */
    PROBATION_RECEPTIONIST(20f, 0f, 0f, false),

    /**
     * Probation Client — one of the waiting room regulars (Daz, Spider, Big Tone, Leanne, Chantelle).
     * 3–5 spawn in the waiting area during office hours.
     * Each carries 1–2 CRIMINAL_INTEL rumours and trades TOBACCO, STOLEN_PHONE, or FAKE_ID.
     * Spider also offers a ScrapyardSystem package lift for MARCHETTI_CREW Respect +5.
     * Speech: varies per named client — local criminal gossip and black market chatter.
     */
    PROBATION_CLIENT(20f, 0f, 0f, false),

    /**
     * Community Service Supervisor — Gary, 50, seen-it-all council parks worker.
     * Stationed at the park during community service hours (09:00–17:00).
     * Assigns litter-picking, graffiti-cleaning, and bench-painting tasks.
     * Checks for skiving: 50% detection chance if player absent 30+ minutes.
     * Speech: "Right, there's litter down by the pond — sort it." /
     *         "You bunking off? Because it looks like you're bunking off." /
     *         "One more hour and you're done. Try to look enthusiastic."
     */
    COMMUNITY_SERVICE_SUPERVISOR(25f, 0f, 0f, false),

    // ── Issue #1146: Mick's MOT & Tyre Centre ────────────────────────────────

    /**
     * Mechanic — Mick, the gruff proprietor of Mick's MOT &amp; Tyre Centre.
     * Stands in Bay 1 during opening hours (Mon–Sat 08:00–18:00).
     * Offers Official MOT (Mon/Wed/Fri 09:00–12:00 only), Dodgy MOT, Advisory Repairs,
     * Car Ringing (MARCHETTI_CREW Respect ≥ 50), and Chop Shop services.
     * Refuses service if player Notoriety ≥ 80.
     * Speech: "What've you brought in?" / "She'll never pass, mate." /
     *         "I'm not asking questions and you shouldn't either."
     */
    MECHANIC(40f, 6f, 2.0f, false),

    /**
     * Mechanic Apprentice — Kyle, Mick's 19-year-old apprentice.
     * Stationed in Bay 2 during opening hours. Performs chop-shop work
     * and manages the tyre stack. Never directly involved in MOT fraud.
     * Speech: "Mick handles all the paperwork." / "I just do the tyres, mate."
     */
    MECHANIC_APPRENTICE(25f, 3f, 2.5f, false),

    /**
     * MOT Tester — Terry, the legitimate MOT tester (Mon/Wed/Fri only).
     * Arrives at 09:00 and leaves at 12:00. During those hours, Official MOTs
     * are available at the MOT_RAMP_PROP for 5 COIN.
     * If Terry witnesses a Dodgy MOT in progress (25% chance), he reports it:
     * FRAUDULENT_MOT recorded, Notoriety +8, Wanted +1.
     * Speech: "She's got advisory on the nearside brake." /
     *         "Bring it back when you've sorted the exhaust." /
     *         "I can't pass that. Sorry."
     */
    MOT_TESTER(25f, 0f, 0f, false),

    // ── Issue #1148: Northfield Council Estate Lock-Up Garages ───────────────

    /**
     * Dave the Caretaker — council estate caretaker who patrols the garage block
     * 08:00–16:00 Mon–Fri. Witnesses break-ins and files TRESPASSING offences via
     * WantedSystem. Also handles Garage 7 rental (5 COIN/week) and eviction notices.
     * Speech: "You're not supposed to be in there, mate." /
     *         "Rent's due Friday. Don't make me knock twice." /
     *         "These garages are for residents only."
     */
    DAVE_CARETAKER(35f, 4f, 1.2f, false),

    /**
     * Garage Band Doorman — the band's informal doorman at Garage 1 rehearsals
     * (Tues/Thurs/Sat 19:00–22:00). Lets regulars in, charges 1 COIN for new faces.
     * After 3 sessions watched, player can take this role for 3 COIN/session.
     * Speech: "You here for the band?" /
     *         "That'll be a quid, mate." /
     *         "You're alright, come in."
     */
    GARAGE_BAND_DOORMAN(30f, 0f, 0f, false),

    // ── Issue #1151: Northfield Sporting & Social Club ────────────────────────

    /**
     * Ron — steward of the Northfield Sporting &amp; Social Club.
     * Sells CLUB_MEMBERSHIP_CARD for 5 COIN. Mans the front desk Mon–Sat 11:00–23:00.
     * Blocks non-members from the main bar. Knows about the protection arrangement but
     * stays quiet unless the player has Respect ≥ 50 with STREET_LADS faction.
     * Speech: "Members only, mate." / "Fiver for the card." /
     *         "Ron's the name. I just work here." / "I don't want any trouble."
     */
    SOCIAL_CLUB_STEWARD(30f, 3f, 2.0f, false),

    /**
     * Maureen — quiz host at the Thursday Quiz Night (19:30–22:00).
     * Carries the QUIZ_ANSWER_SHEET which can be stolen from the notice board.
     * Announces rounds, reads questions, and tallies scores.
     * Speech: "Right, question one..." / "No phones!" /
     *         "Put that away, I can see you." / "And the winner is..."
     */
    QUIZ_HOST(25f, 0f, 0f, false),

    /**
     * Club Regular — a rank-and-file member of the social club.
     * Attends darts nights, quiz nights, and general socialising.
     * On Fri/Sat 19:00–23:00, one will issue a darts wager challenge.
     * Speech: "You any good at darts?" / "Double top for the win." /
     *         "I've been coming here thirty years." / "Best bitter in Northfield."
     */
    CLUB_REGULAR(25f, 2f, 2.0f, false),

    /**
     * Tommy Marchetti's enforcer — collects the 20 COIN protection envelope Mon 20:00–21:30.
     * Becomes hostile if the player steals the envelope or grasses to the police.
     * At HOSTILE state: attacks on sight; triggers Wanted +3 if player is spotted.
     * Speech: "Tommy sends his regards." / "Is it ready?" /
     *         "Don't make this difficult." / "You've made a very big mistake."
     */
    MARCHETTI_ENFORCER(60f, 14f, 1.8f, false),

    // ── Issue #1153: Northfield Community Centre ──────────────────────────────

    /**
     * Community Centre Manager — Denise, the centre manager.
     * Patrols the front desk 09:00–17:00 Mon–Fri. Passive; can issue AEROBICS_PASS
     * and help with legitimate grant applications (trust ≥ 30).
     * Speech: "Morning! What can I help you with?" /
     *         "Sessions are on the board by the entrance." /
     *         "We're a community resource — treat it like one."
     */
    COMMUNITY_CENTRE_MANAGER(20f, 0f, 0f, false),

    /**
     * Community Centre Volunteer — a general volunteer helper.
     * Sets up chairs, makes tea, runs the tuck shop. Passive.
     */
    COMMUNITY_CENTRE_VOLUNTEER(20f, 0f, 0f, false),

    /**
     * Aerobics Instructor — Sandra, who runs Mon/Wed/Fri 09:30–10:30 aerobics.
     * Leads the rhythm mini-game with 8 prompts. Passive; speech encourages exercise.
     */
    AEROBICS_INSTRUCTOR(20f, 0f, 0f, false),

    /**
     * Aerobics Participant — regular attendee of Sandra's aerobics class.
     * 4–8 spawn during session hours. Passive.
     */
    AEROBICS_PARTICIPANT(20f, 0f, 0f, false),

    /**
     * Young Mum — attends the Toddler Playgroup Tue/Thu mornings.
     * Brings a TODDLER NPC. Passive; gossips.
     */
    YOUNG_MUM(20f, 0f, 0f, false),

    /**
     * Toddler — a small child attending the Tuesday/Thursday playgroup.
     * Passive; follows nearest YOUNG_MUM. Runs around near BOUNCY_CASTLE_PROP.
     */
    TODDLER(5f, 0f, 0f, false),

    /**
     * Citizens Advice Volunteer — Derek, who runs the Tue 14:00–16:00 CAB drop-in.
     * Gives CAB_REFERRAL_LETTER (halves FoodBank wait) or CHARACTER_REFERENCE_LETTER
     * (reduces MagistratesCourtSystem sentence by one tier). Passive.
     * Speech: "Take a seat — we'll get to you." /
     *         "I can't give legal advice, but I can point you in the right direction."
     */
    CAB_VOLUNTEER(20f, 0f, 0f, false),

    /**
     * NA Attendee — regular at the Thursday 19:00–20:30 Narcotics Anonymous meeting.
     * Passive; listens during share-story mechanic.
     */
    NA_ATTENDEE(20f, 0f, 0f, false),

    /**
     * NA Chair — Brenda, who facilitates the Thursday NA meeting.
     * Passive; awards COMMUNITY_TRUST after sharing. Seeds a rumour at trust ≥ 30.
     */
    NA_CHAIR(20f, 0f, 0f, false),

    /**
     * Community Member — a generic Northfield resident who attends community events
     * (curry night, aerobics, etc.). Passive.
     */
    COMMUNITY_MEMBER(20f, 0f, 0f, false),

    /**
     * Karate Kid — a child attending the Wednesday 18:30 Karate Juniors session.
     * Passive; follows KARATE_INSTRUCTOR.
     */
    KARATE_KID(15f, 2f, 3.0f, false),

    /**
     * Karate Instructor — runs the Wednesday 18:30 Karate Juniors class.
     * Passive; demonstrates moves. Hostile if provoked (attacks defending).
     */
    KARATE_INSTRUCTOR(30f, 8f, 1.5f, false),

    /**
     * Curry Cook — volunteers at the Saturday 18:00–21:00 curry night.
     * Sells CURRY_AND_RICE for 2 COIN (+12 Warmth, +15 Hunger). Passive.
     */
    CURRY_COOK(20f, 0f, 0f, false),

    // ── Issue #1155: Northfield NHS Dentist ──────────────────────────────────

    /**
     * Dental Receptionist — Deborah. Registers NHS patients, manages the waiting list,
     * books private appointments. Present Mon–Fri 08:30–17:00. Lunch 13:00–14:00 (back-office).
     */
    DENTAL_RECEPTIONIST(20f, 0f, 0f, false),

    /**
     * Dentist — Dr. Rashid. Performs dental treatment at DENTAL_CHAIR_PROP.
     * Present Mon–Fri 09:00–17:30.
     */
    DENTIST(25f, 0f, 0f, false),

    /**
     * Unlicensed Dentist — Mirek. Found in the Council Flats via pub rumour chain.
     * Offers 5 COIN treatment with 60% chance of INFECTION or BOTCHED_JOB side effects.
     * Flees on sight of POLICE NPC.
     */
    UNLICENSED_DENTIST(20f, 4f, 2.0f, false),

    /**
     * Dental Patient — civilian waiting in the Northfield Dental Practice.
     * May share TOOTHACHE-related rumours.
     */
    DENTAL_PATIENT(20f, 0f, 0f, false),

    // ── Issue #1157: Northfield Tesco Express ─────────────────────────────────

    /**
     * Tesco Express Manager — Dave; tired, middle-management, clings to procedure.
     * Present Mon–Fri 07:00–18:00. Checks for shoplifting flags. At Notoriety Tier 3+
     * requests player to leave; calls police if refused.
     */
    TESCO_EXPRESS_MANAGER(30f, 0f, 0f, false),

    /**
     * Tesco Express Worker — Sharon (day) / Tyler (night 22:00–07:00).
     * Stacks shelves, operates till. Tyler barely awake — suspicion threshold +20.
     * Tyler sells TESCO_OWN_BRAND_VODKA without ID checks.
     */
    TESCO_EXPRESS_WORKER(20f, 0f, 0f, false),

    // ── Issue #1165: Northfield Match Day ────────────────────────────────────
    /** Home football fan — wears blue/claret colours, sings, buys pies. */
    HOME_FAN(40f, 8f, 1.2f, false),
    /** Away football fan — rival colours, hostile to HOME_FAN NPCs. */
    AWAY_FAN(40f, 10f, 1.0f, false),
    /** Tout selling knock-off scarves and counterfeit tickets. */
    FOOTBALL_TOUT(30f, 0f, 0f, false),
    /** Mounted police officer — extra intimidation radius. */
    POLICE_HORSE_OFFICER(80f, 12f, 0f, false),

    // ── Issue #1167: Northfield Amateur Boxing Club ───────────────────────

    /**
     * Amateur boxer — regular club member at Tommy's Gym.
     * Available Mon/Wed/Fri 18:00–21:00 and Friday fight nights.
     * Press E to spar (requires BOXING_RING_PROP, BOXING skill ≥ 2).
     */
    BOXING_AMATEUR(35f, 6f, 1.8f, false),

    /**
     * Boxing prospect — Tommy's top pupil; harder opponent for white-collar bouts.
     * Only spawns during Friday Night Fights (20:00–23:00).
     */
    BOXING_PROSPECT(45f, 9f, 1.5f, false),

    /**
     * Fight promoter — Wayne; anchored to BET_TABLE_PROP during underground
     * white-collar circuit nights (alternate Saturdays 22:00+).
     * Takes bets and offers bout-fixing bribes.
     */
    FIGHT_PROMOTER(30f, 0f, 0f, false),

    /**
     * White-collar boxer — office worker opponent in the underground circuit.
     * Spawns on alternate Saturdays 22:00. Better funded, worse technique.
     */
    WHITE_COLLAR_BOXER(30f, 5f, 2.0f, false),

    // ── Issue #1171: Northfield TV Licence ────────────────────────────────────

    /**
     * TV Licence Officer — BBC Licensing Authority enforcement officer.
     * Crawls residential streets as part of the DETECTOR_VAN patrol every 14 in-game days.
     * Can knock on the player's door to conduct an inspection. Can be bribed (4 COIN),
     * intimidated by the dog (bond ≥ 50 + off-lead), or let in (fines 8 COIN).
     * Not hostile; does not pursue the player.
     */
    LICENCE_OFFICER(40f, 0f, 0f, false),

    // ── Issue #1173: Northfield Balti House ──────────────────────────────────

    /**
     * Balti Owner — Mohammed; runs the kitchen at Mumtaz Baltis.
     * 35 HP, deals 8 damage per hit. Confronts thieves in the kitchen;
     * 50% chance of confrontation if present during the Naan Heist.
     * Enters distressed dialogue after a hygiene violation (fake spice mechanic).
     */
    BALTI_OWNER(35f, 8f, 1.5f, false),

    /**
     * Balti Waiter — Tariq; Mohammed's son. Takes orders and runs to the phone
     * on a dine-and-dash event. Passive; not hostile.
     */
    BALTI_WAITER(20f, 0f, 0f, false),

    /**
     * Balti Regular — seated rumour-source; 3–5 per session (12:00–15:00 / 17:30–23:30).
     * Proximity ≤ 2 blocks: quietly exchanges rumours with nearby NPCs.
     * Required for the FULL_BALTI achievement.
     */
    BALTI_REGULAR(30f, 0f, 0f, false),

    // ── Issue #1175: Northfield Argos ─────────────────────────────────────────

    /**
     * Argos Counter Staff — Sharon; staffs the collection counter.
     * Sharp-eyed; calls Dave (ARGOS_SECURITY) on suspicion.
     * 20 HP; not hostile.
     */
    ARGOS_COUNTER_STAFF(20f, 0f, 0f, false),

    /**
     * Argos Security — Dave; patrols the store floor.
     * Bans player on 2nd offence. +15% pickpocket detection when nearby.
     * 35 HP; not hostile by default but becomes AGGRESSIVE on offence.
     */
    ARGOS_SECURITY(35f, 8f, 1.5f, false),

    /**
     * Argos Returns Staff — Janice; sits at the returns desk.
     * Gullible: 40% fraud detection rate (FORGED_RECEIPT).
     * 20 HP; not hostile.
     */
    ARGOS_RETURNS_STAFF(20f, 0f, 0f, false),

    // ── Issue #1177: Northfield Sunday Car Park Market ────────────────────────

    /**
     * Council Enforcement Officer — patrols the Sunday Car Park Market every
     * 4 in-game minutes in the COUNCIL_VAN. Checks trader licences; confiscates
     * goods from unlicensed traders. The player can bribe them for 5 COIN.
     * Flees if the player has ≥ 3 wanted stars (not their department).
     */
    COUNCIL_OFFICER(25f, 0f, 0f, false),

    // ── Issue #1181: Northfield Chugger Blitz ─────────────────────────────────

    /**
     * Chugger — a roving charity fundraiser who patrols the high street weekdays
     * 09:00–17:00. Named NPCs: Kelly (female) and Marcus (male).
     * Intercepts the player within 3 blocks with a 6-second HUD prompt offering
     * four responses: Donate, Sign Up for Direct Debit, Refuse, or Flee.
     * Aggressive refusal (punching) triggers FLEEING state.
     * Passive otherwise; never initiates combat.
     */
    CHUGGER(20f, 0f, 0f, false),

    /**
     * Issue #1183: TIP_ATTENDANT — Dave, the senior site operative at Northfield HWRC.
     * Patrols the gate and skip bays 08:00–18:00 Tue–Sun. Interrogates players for
     * trade waste, enforces skip bay rules, limits Reuse Corner takes, and can radio
     * COUNCIL_ENFORCEMENT. Bribable for 5 COIN (BACKHANDER_AT_THE_TIP achievement).
     * Rain: 50% less vigilant. Heatwave: max enforcement. Frost: slow patrol.
     */
    TIP_ATTENDANT(60f, 0f, 0f, false),

    /**
     * Issue #1183: COUNCIL_ENFORCEMENT — enforcement officer spawned when Dave radios
     * for backup after a trade waste evasion attempt. Blocks the site gate. Can be
     * talked down (Notoriety < 20), bribed (3 COIN), or outrun (velocity > 1.5x for
     * 5 seconds). Only active while site gate is blocked.
     */
    COUNCIL_ENFORCEMENT(80f, 0f, 0f, false),

    // ── Issue #1188: Northfield DWP Home Visit ─────────────────────────────

    /**
     * Issue #1188: DWP Compliance Officer — Brenda or Keith, sent on unannounced
     * home visits when the player's suspicion score reaches 60+.
     * Brenda: 50s, anorak, clipboard. Disapproving but not unkind.
     * Keith: 40s, terse, by-the-book. Only appears when suspicion ≥ 80.
     * Passive until player opens squat door; then initiates compliance interview.
     */
    DWP_COMPLIANCE_OFFICER(30f, 0f, 0f, false),

    // ── Issue #1190: Information Broker ──────────────────────────────────────

    /**
     * Kenny Doyle — the information broker operating from the back-room booth
     * of The Feathers pub. Passive; interacts via menu when player presses E.
     * Buys and sells criminal intelligence via InformationBrokerSystem.
     */
    INFO_BROKER(25f, 0f, 0f, false),

    /**
     * Issue #1192: Mick the card dealer — runs the back-room Pontoon table at the
     * Northfield Sporting &amp; Social Club on Friday/Saturday nights. Enters cheat mode
     * after player wins 3 consecutive hands (hidden ace probability 0.6).
     */
    CARD_DEALER(30f, 0f, 0f, false),

    // ── Issue #1196: Environmental Health Officer ─────────────────────────────

    /**
     * Janet — Council Environmental Health Officer. Inspects food venues Mon–Fri
     * 09:30–15:30. Passive; if assaulted seeds COUNCIL_ENFORCEMENT rumour, adds
     * ASSAULT_ON_OFFICIAL crime, Wanted +3. HP 25f, no attack, not hostile.
     */
    ENVIRONMENTAL_HEALTH_OFFICER(25f, 0f, 0f, false),

    // ── Issue #1198: Northfield Traffic Warden ────────────────────────────────

    /**
     * Clive — the town's sole civil enforcement officer (Traffic Warden).
     * Male, 50s, yellow hi-vis tabard, peaked cap, hand-held terminal.
     * Patrols high street, car park, and side streets Mon–Sat 08:00–18:00.
     * Fixed circuit: each full patrol takes 8 in-game minutes.
     * Passive; if assaulted seeds COUNCIL_ENFORCEMENT rumour, adds
     * ASSAULT_ON_OFFICIAL to CriminalRecord, Wanted +3, NeighbourhoodSystem −5 vibes.
     */
    TRAFFIC_WARDEN(30f, 0f, 0f, false),

    /**
     * Brenda — Council Receptionist at the COUNCIL_OFFICE landmark.
     * Staffs the front desk Mon–Fri 09:00–17:00. Passive; handles PCN appeals
     * submitted at the APPEAL_DESK_PROP.
     * Speech: "Take a number." / "You'll need to fill in the form."
     *         / "Two working days and we'll write to you." / "Next, please."
     */
    COUNCIL_RECEPTIONIST(20f, 0f, 0f, false),

    // ── Issue #1205: Northfield DVSA Test Centre ──────────────────────────────

    /**
     * Sandra — DVSA Examiner at the Northfield DVSA Test Centre.
     * Sits behind the EXAMINER_DESK_PROP. Conducts theory tests (10 questions,
     * 2 COIN fee) and practical tests (5 COIN, requires THEORY_PASS_CERTIFICATE).
     * Can be bribed (CHARISMA ≥2 + 15 COIN, no witnesses within 6 blocks).
     * Permanently refuses after a failed bribe attempt.
     * Passive unless provoked; never hostile.
     * Speech: "Next candidate please." / "Form an orderly queue."
     *         / "I'm going to need your certificate before we can proceed."
     */
    DVSA_EXAMINER(30f, 0f, 0f, false),

    /**
     * Keith — Driving Instructor stationed outside the DVSA Test Centre.
     * Stands near the INSTRUCTOR_CAR_PROP. Offers 60-second driving lesson
     * sessions for 3 COIN/session; awards DRIVING XP +5 per lesson.
     * After 3 lessons, applies −5 fault reduction on the player's next
     * practical test. Passive; never hostile.
     * Speech: "Right, mirrors — signal — manoeuvre."
     *         / "Check your blind spot, son."
     *         / "Three lessons and you'll be fine. Probably."
     */
    DRIVING_INSTRUCTOR(25f, 0f, 0f, false),

    // ── Issue #1209: Citizens Advice Bureau ────────────────────────────────────

    /**
     * Issue #1209: ADVICE_VOLUNTEER — unpaid CAB volunteer seated at a
     * CONSULTATION_DESK_PROP in the Citizens Advice Bureau. Margaret (60s,
     * cardigan) is present Mon–Fri 09:30–16:30. Brian (50s, retired teacher)
     * attends Mon/Wed/Fri 10:00–14:00. Unhelpful to WANTED players (≥2 stars).
     * Passive; never hostile.
     * Margaret speech: "Take a seat love, we'll be with you."
     *                  / "Have you got your reference number?"
     * Brian speech:    "I've seen this before — it's not hopeless."
     *                  / "Write it all down. Courts like paperwork."
     */
    ADVICE_VOLUNTEER(20f, 0f, 0f, false),

    // ── Issue #1216: Northfield Driving Instructor ─────────────────────────────

    /**
     * Issue #1216: LEARNER_DRIVER — Dave's current lesson pupil. Spawns at
     * DRIVING_SCHOOL_DESK_PROP at lesson start, boards INSTRUCTOR_CAR_PROP and
     * departs on the route. Stumbles out shaking when the lesson ends or is
     * sabotaged. Passive; never hostile.
     * Speech: "I've only stalled it six times today."
     *          / "I think I clipped the kerb."
     *          / "Is it always this scary?"
     */
    LEARNER_DRIVER(30f, 0f, 0f, false),

    // ── Issue #1218: Northfield Claims Management Company ─────────────────────

    /**
     * Issue #1218: CLAIMS_MANAGER — Gary, the unscrupulous proprietor of
     * Compensation Kings on the high street. Stands behind his DESK_PROP
     * Mon–Sat 09:00–17:00. Processes fraudulent personal injury claims, takes
     * a 30% cut of each payout. Refuses service at Notoriety Tier ≥ 4 or when
     * FRAUD_SUSPECTED threshold (3 claims in 7 days) is reached.
     * Passive; never hostile.
     * Speech: "Leave it with me, mate." / "Thirty per cent — that's industry standard."
     *         / "You didn't hear this from me." / "Look, even I have limits."
     */
    CLAIMS_MANAGER(20f, 0f, 0f, false),

    /**
     * Issue #1218: CLAIMS_ASSISTANT — Chantelle, Gary's assistant at Compensation
     * Kings. Patrols a 4-block route behind the counter Mon–Fri 09:00–17:00 only
     * (Gary covers Saturday solo). Passive; never hostile.
     * Speech: "Have you got your ref number?" / "Gary's just with someone."
     *         / "Fill this in first, yeah?" / "Compensation's a human right, innit."
     */
    CLAIMS_ASSISTANT(20f, 0f, 0f, false),

    /**
     * Issue #1218: INSURANCE_INVESTIGATOR — a beige-anorak investigator dispatched
     * by the insurance company when a claim is filed and CCTV coverage was detected
     * (40% chance within 10 blocks of a CCTV_PROP). Carries a hidden camera.
     * Follows the player at distance; monitors for sprint/fight/block-break within
     * 20 blocks to invalidate the claim. Can be bribed (5 COIN, 50% accept) or
     * shaken (60 blocks away, 5 in-game minutes out of sight).
     * Passive until claim invalidation triggers → records INSURANCE_FRAUD.
     * Speech: "Just passing through." / "Don't mind me." / "I've got all day, sunshine."
     */
    INSURANCE_INVESTIGATOR(25f, 0f, 0f, false),

    /**
     * Issue #1220: BOOKIES_CLERK — Derek, the world-weary Ladbrokes clerk.
     * Late 50s, cardigan, Racing Post perpetually folded under one arm.
     * Stands behind the counter Mon–Sat 09:00–22:00, Sun 10:00–18:00.
     * Non-hostile; dispenses dry wisdom about form guides and the futility
     * of accumulator bets.
     * Speech: "Alright son, what's your fancy today?" / "The machine giveth
     * and the machine taketh away, mate." / "I'm not supposed to give tips,
     * but… look at the form on number four." / "We're closing up. Come back
     * tomorrow."
     */
    BOOKIES_CLERK(40f, 0f, 0f, false),

    /**
     * Issue #1227: CAR_DEALER — Wayne, the owner of Wheelwright Motors. Sun-bleached
     * tie, cheap suit, clipboard with laminated price cards. Anchored to the forecourt
     * during opening hours (09:00–18:00 Mon–Sat), moves into the portacabin after 17:00.
     * Non-hostile; dispenses banter and dodgy deals.
     * Speech: "Every car on that lot drives sweet as a nut, mate." /
     *         "I'm losing money at that price. Killing myself here." /
     *         "Look, I don't ask questions, and neither should you."
     */
    CAR_DEALER(40f, 0f, 0f, false),

    /**
     * Issue #1227: CAR_LOT_MECHANIC — Bez, Wayne's mechanic. Overalls, spanner,
     * perpetually under a bonnet from 09:00–16:00. Available to clock odometers
     * for a 5 COIN bribe (requires MILEAGE_CORRECTOR_PROP in player inventory).
     * Non-hostile; knows more than he lets on.
     * Speech: "Needs a new timing belt but she goes alright." /
     *         "Don't tell Wayne I said that." / "Five coin and I'll sort you out."
     */
    CAR_LOT_MECHANIC(30f, 0f, 0f, false),

    /**
     * Issue #1227: REPO_MAN — visits the player's registered address when finance
     * payments are missed (PaydayLoanSystem/CarDealershipSystem). Like a mini-BAILIFF.
     * Spawns after 1st missed payment, escalates; removes car on 3rd miss.
     * Non-hostile until Scarper chosen; becomes mildly aggressive (low damage).
     * Speech: "I'm just here for the vehicle, mate." / "Don't make it awkward."
     */
    REPO_MAN(50f, 5f, 0f, false),

    /**
     * Issue #1227: TYRE_KICKER — casual browser NPC who wanders Wheelwright Motors
     * forecourt on sunny days (1–2 NPCs), looks at cars, asks prices, never buys.
     * Non-hostile; spawned by CarDealershipSystem during SUNNY weather.
     * Speech: "What's the mileage on that one?" / "Bit steep, innit."
     */
    TYRE_KICKER(20f, 0f, 0f, false),

    /**
     * Issue #1235: CLUB_SECRETARY — Barry, the officious secretary of the Northfield
     * Sporting &amp; Social Club. Guards the door and enforces membership rules.
     * Non-hostile; ejects non-members and players with Wanted ≥ 2 stars.
     */
    CLUB_SECRETARY(30f, 0f, 0f, false),

    /**
     * Issue #1235: DARTS_PLAYER — Dave or Kev, regular darts-league competitors.
     * Press E to challenge them to a game of 501. Non-hostile.
     */
    DARTS_PLAYER(25f, 0f, 0f, false),

    /**
     * Issue #1235: RAFFLE_ORGANISER — Irene, who runs the Friday-evening meat raffle.
     * Sells raffle tickets and draws numbers. Non-hostile.
     */
    RAFFLE_ORGANISER(25f, 0f, 0f, false),

    // ── Issue #1240: Northfield NHS Blood Donation Session ────────────────────

    /**
     * Issue #1240: NHS_DONOR_COORDINATOR — Brenda, the mobile unit coordinator.
     * Stationed at BLOOD_DONATION_VAN_PROP in the Community Centre car park every
     * 14 in-game days (Wednesday 09:00–17:00). Manages the eligibility questionnaire
     * and oversees the donation session. Non-hostile.
     */
    NHS_DONOR_COORDINATOR(30f, 0f, 0f, false),

    /**
     * Issue #1311: NHS_VOLUNTEER — Tyler, the young blood donation volunteer.
     * Patrols the waiting area and biscuit table inside the mobile unit.
     * Detection radius: 6 blocks (reduced to 3 when distracted by a
     * SCHOOL_KID or PENSIONER NPC). Non-hostile unless the biscuit tin is stolen.
     * Speech: "Have you got your form?" / "Biscuits are for donors only, mate."
     *         "You all right? Looking a bit peaky." / "Oi!"
     */
    NHS_VOLUNTEER(20f, 0f, 1.5f, false),

    // ── Issue #1243: Northfield Bert's Tyres & MOT ───────────────────────────

    /**
     * Issue #1243: DODGY_MECHANIC — Bert, owner of Bert's Tyres &amp; MOT on the
     * industrial estate. Runs MOT inspections with variable corruption (0–100).
     * At corruption ≥ 50 he deliberately fails cars to pocket repair costs.
     * Press E once to start MOT, E again (StreetReputation ≥ 40) to call his bluff.
     * Buys STOLEN_TYRE at 8 COIN each. Non-hostile unless attacked.
     * Speech: "She'll need a new manifold, mate." / "Cash only, no receipts."
     *         "I'm doing you a favour here." / "DVSA never come round this way."
     */
    DODGY_MECHANIC(45f, 5f, 1.5f, false),

    /**
     * Issue #1243: APPRENTICE_MECHANIC — Kyle, Bert's apprentice. Wanders the
     * workshop, picks up STOLEN_TYRE items, and can be distracted via GARAGE_PHONE_PROP.
     * When Bert enters BERT_DISTRACTED state, Kyle covers the desk but is easily
     * distracted himself. Non-hostile.
     * Speech: "Bert! Phone for ya!" / "Don't touch that, mate." / "Is it serious?"
     */
    APPRENTICE_MECHANIC(25f, 0f, 0f, false),

    /**
     * Issue #1243: DVSA_INSPECTOR — government vehicle-standards inspector who raids
     * Bert's Tyres &amp; MOT every 7 in-game days. Invalidates PASS_BRIBE certificates,
     * records VEHICLE_FRAUD in CriminalRecord, and adds WantedSystem +2 stars if
     * player is present. Non-hostile but triggers police escalation on interference.
     * Speech: "DVSA. I'll need to see your testing records." / "These certificates
     *          don't add up." / "Step away from the vehicle."
     */
    DVSA_INSPECTOR(40f, 0f, 0f, false),

    // ── Issue #1251: Northfield Street Chuggers ───────────────────────────────

    /**
     * Chugger Leader — Tracy, the relentlessly positive team leader who stands near the
     * CHARITY_CLIPBOARD_STAND_PROP outside the charity shop. Manages the shift quota
     * and can hire the player as a chugger.
     */
    CHUGGER_LEADER(20f, 0f, 0f, false),

    // ── Issue #1252: Northfield TV Licensing ──────────────────────────────────

    /**
     * TV Licence Officer — named instance: Derek. Grey anorak, ID lanyard, clipboard.
     * Makes targeted doorstep visits to known unlicensed addresses.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     * If assaulted: Notoriety +10, ASSAULT crime added, Wanted +2 stars.
     * Derek despawns and re-visits 2 in-game days later.
     * If bribed successfully: despawns for 7 in-game days.
     */
    TV_LICENCE_OFFICER(20f, 0f, 0f, false),

    // ── Issue #1257: Northfield Rag-and-Bone Man ──────────────────────────────

    /**
     * Rag-and-Bone Man — named instance: Barry Dodd. Battered flat-cap, high-vis vest,
     * drives the RAG_AND_BONE_VAN prop on his 6-stop route Mon–Sat 07:30–13:00.
     * Calls out "Any old iroooon!" every 30 in-game seconds (range 20 blocks).
     * Won't operate on Sundays or during rain.
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RAG_AND_BONE_MAN(30f, 0f, 0f, false),

    /**
     * Rival Rag-and-Bone Man — named instance: Terry. Operates Barry's route when
     * Barry's tyres are slashed (02:00–06:00). Hostile to the player; has a 25% chance
     * to steal items from the player on each pass. Charges 90% of Barry's prices.
     * HP: 30f, attack: 3f, cooldown: 2.0f, hostile: true.
     */
    RIVAL_RAGBONE_MAN(30f, 3f, 2.0f, true),

    // ── Issue #1259: Northfield Pub Quiz Night ────────────────────────────────

    /**
     * Derek the Quiz Master — runs Wednesday Quiz Night at The Rusty Anchor.
     * Stands at the QUIZ_PODIUM_PROP 19:30–22:00, reads questions, catches cheaters.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    QUIZ_MASTER(20f, 0f, 0f, false),

    /**
     * A rival pub quiz team (2–4 NPCs per team) seated at a PUB_TABLE_PROP.
     * Auto-answers questions with Normal(μ=3, σ=1) accuracy per round.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    QUIZ_TEAM(20f, 0f, 0f, false),

    // ── Issue #1263: Northfield Illegal Street Racing ─────────────────────────

    /**
     * Shane the Race Organiser — runs the Friday/Saturday night illegal street
     * racing meet at the Tesco car park from 23:00. Collects 5 COIN entry fees,
     * manages racer registration, shouts 'SCATTER! PLOD!' on police shutdown.
     * Bans players caught sabotaging competitors. Cannot be attacked (non-hostile).
     * HP: 40f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RACE_ORGANISER(40f, 0f, 0f, false),

    /**
     * Boy Racer — a Friday/Saturday-night street racer who assembles in the
     * Tesco car park at 23:00. Has a named car with a base speed stat.
     * Flees (NPCState.FLEEING) on police shutdown. 4–6 spawn per meet.
     * HP: 35f, attack: 0f, cooldown: 0f, hostile: false.
     */
    BOY_RACER(35f, 0f, 0f, false),

    // ── Issue #1269: Northfield BT Phone Box ──────────────────────────────────

    /**
     * Marchetti Runner — a member of the Marchetti Crew dispatched after a
     * dead-drop call from the phone box. Arrives at the phone box within 3
     * in-game minutes carrying a random item from the Marchetti loot pool.
     * Player collects by pressing E. Despawns after 10 minutes if uncollected.
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    MARCHETTI_RUNNER(30f, 0f, 0f, false),

    // ── Issue #1271: Northfield Tattoo Parlour ────────────────────────────────

    /**
     * Health Inspector — a council official who may arrive during an unlicensed tattoo
     * session (15% chance per walk-in hustle). The player must vacate Daz's station
     * within 30 seconds or receive an UNLICENSED_TATTOOING crime.
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false (unless player ignores them).
     */
    HEALTH_INSPECTOR(30f, 0f, 0f, false),

    // ── Issue #1273: Northfield Fly-Tipping Ring ──────────────────────────────

    /**
     * Council Van Officer — Janet's colleague <b>Gary</b> — hi-vis jacket, clipboard,
     * drives a white Transit van. Spawns near a {@link ragamuffin.world.PropType#FLY_TIP_PILE_PROP}
     * within {@code FlyTippingSystem.FLY_TIP_RESPONSE_SECONDS} (120 s) of the dump.
     * Active 07:00–20:00 only; no officer spawns overnight.
     * <ul>
     *   <li>Passive until pile detected; confronts player if within 8 blocks.</li>
     *   <li>Issues a {@link ragamuffin.building.Material#FIXED_PENALTY_NOTICE} (15 COIN fine).</li>
     *   <li>Becomes AGGRESSIVE if player evades (runs &gt;8 blocks away without paying).</li>
     * </ul>
     * Dialogue: "You're having a laugh mate, that didn't come from nowhere."
     * HP: 35f, attack: 0f, cooldown: 0f, hostile: false (until evasion triggers aggression).
     */
    COUNCIL_VAN_OFFICER(35f, 0f, 0f, false),

    // ── Issue #1278: Northfield Travelling Fairground ─────────────────────────

    /**
     * Big Lenny — the fairground boss and operator.
     * <ul>
     *   <li>Manages the travelling fairground (fortnightly, Friday 18:00 – Sunday 22:00).</li>
     *   <li>Offers cash-in-hand Strongman shifts (3 COIN/hr, skimming risk).</li>
     *   <li>Sells FAIRGROUND_TICKET (1 COIN each).</li>
     *   <li>Turns hostile if player skims more than 20% of takings or causes FAIRGROUND_TROUBLEMAKER.</li>
     * </ul>
     * HP: 60f, attack: 8f, cooldown: 2.5f, hostile: false (until triggered).
     */
    FAIRGROUND_BOSS(60f, 8f, 2.5f, false),

    /**
     * Fairground Worker — Shaz, Wayne, or Donna. Staffs the various rides and stalls.
     * <ul>
     *   <li>Shaz: runs Hook-a-Duck and Ring Toss stalls.</li>
     *   <li>Wayne: operates the Waltzers (can be tipped 1 COIN to spin faster).</li>
     *   <li>Donna: operates the Candy Floss stall.</li>
     *   <li>All workers become hostile if player attacks the fairground or commits RIGGED_GAME.</li>
     * </ul>
     * HP: 35f, attack: 4f, cooldown: 2.0f, hostile: false (until triggered).
     */
    FAIRGROUND_WORKER(35f, 4f, 2.0f, false),

    // ── Issue #1282: Northfield Day & Night Chemist ───────────────────────────

    /**
     * METHADONE_CLIENT — queues at the chemist for methadone dispensing (13:30–14:30).
     * <ul>
     *   <li>1–3 present during the methadone window.</li>
     *   <li>Player can steal methadone: 40% success, WantedSystem +3, ROBBERY charge.</li>
     *   <li>Despawns after collection.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    METHADONE_CLIENT(20f, 0f, 0f, false),

    /**
     * PRESCRIPTION_HOLDER — an NPC presenting a valid prescription.
     * <ul>
     *   <li>Queues at the dispensary counter during opening hours.</li>
     *   <li>Carries a PRESCRIPTION_MEDS item.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    PRESCRIPTION_HOLDER(20f, 0f, 0f, false),

    // ── Issue #1301: Northfield Big Issue Vendor ──────────────────────────────

    /**
     * BIG_ISSUE_VENDOR — Gary Milligan, who runs a MAGAZINE_PITCH_PROP outside Greggs.
     * <ul>
     *   <li>Present Mon–Sat 09:00–17:00; absent in frost; shelters under awning in rain.</li>
     *   <li>Sells BIG_ISSUE_MAG for 3 COIN.</li>
     *   <li>Will confront the player if they take over his pitch.</li>
     * </ul>
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    BIG_ISSUE_VENDOR(25f, 0f, 0f, false),

    // ── Issue #1303: Northfield Dave's Carpets ────────────────────────────────

    /**
     * CARPET_SALESMAN — Dave Hogan, proprietor of Dave's Carpets.
     * <ul>
     *   <li>Stands outside the shop 09:00–17:00 Mon–Sat cycling through closing-down pitches.</li>
     *   <li>Sells SOFA, CARPET_OFFCUT, SACK_TRUCK; goes DEFLATED for 24h after TRADING_STANDARDS_WARNING.</li>
     *   <li>Enters DEFLATED state when reported to Trading Standards (Sandra's inspection).</li>
     * </ul>
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CARPET_SALESMAN(25f, 0f, 0f, false),

    /**
     * CARPET_STOCKROOM_WORKER — Kev, who mans the stockroom at Dave's Carpets.
     * <ul>
     *   <li>Present during shop hours; can be distracted with a CHOCOLATE_BAR for 90 seconds.</li>
     *   <li>Guards the CARPET_ROLL_PROP in the stockroom.</li>
     * </ul>
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CARPET_STOCKROOM_WORKER(25f, 0f, 0f, false),

    /**
     * TRADING_STANDARDS_OFFICER — Sandra, the Trading Standards inspector.
     * <ul>
     *   <li>Spawned by CarpetShopSystem when the player reports Dave's closing-down claim (3rd repeat).</li>
     *   <li>Issues TRADING_STANDARDS_WARNING prop; triggers Dave's DEFLATED state.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */

    // ── Issue #1306: Northfield Traveller Site ────────────────────────────────

    /**
     * TRAVELLER_BOSS — Paddy Flynn, head of the tarmac crew.
     * <ul>
     *   <li>Spawns at the industrial estate wasteland every 7–10 in-game days.</li>
     *   <li>Assigns cash-in-hand driveway jobs, scrap runs, or kerb-hawking heather.</li>
     *   <li>Runs the scrap metal fence — pays above FenceSystem rates for SCRAP_METAL,
     *       COPPER_PIPE, STOLEN_BIKE, STOLEN_PHONE (capped 20 COIN per visit).</li>
     *   <li>Goes HOSTILE if player reports dog fight to RSPCA.</li>
     * </ul>
     * HP: 60f, attack: 12f, cooldown: 1.2f, hostile: false.
     */
    TRAVELLER_BOSS(60f, 12f, 1.2f, false),

    /**
     * TRAVELLER_WORKER — Liam/Seamus/Donal from Paddy's tarmac crew.
     * <ul>
     *   <li>3× present at the site during visit window.</li>
     *   <li>Carry out driveway jobs and scrap runs assigned by TRAVELLER_BOSS.</li>
     * </ul>
     * HP: 40f, attack: 8f, cooldown: 1.5f, hostile: false.
     */
    TRAVELLER_WORKER(40f, 8f, 1.5f, false),

    /**
     * TRAVELLER_WOMAN — Brigid, sells Lucky Heather from a basket.
     * <ul>
     *   <li>Present during site visit; sells LUCKY_HEATHER for 2 COIN.</li>
     *   <li>Sells CLOTHES_PEG_BUNDLE for 1 COIN (comedy throw item).</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    TRAVELLER_WOMAN(20f, 0f, 0f, false),

    /**
     * LURCHER_DOG — Paddy's lurcher, chained near the CARAVAN_PROP.
     * <ul>
     *   <li>Barks at player within 6 blocks; attacks if player enters caravan without
     *       DOG_PERMISSION_FLAG.</li>
     *   <li>Sleeps 02:00–04:00 if player has DOG_PERMISSION_FLAG.</li>
     * </ul>
     * HP: 25f, attack: 10f, cooldown: 1.0f, hostile: false.
     */
    LURCHER_DOG(25f, 10f, 1.0f, false),

    /**
     * COUNCIL_ENFORCEMENT_OFFICER — Derek, the council enforcement officer.
     * <ul>
     *   <li>Spawned by TravellerSiteSystem on day 3 (or same afternoon if tipped off day 1).</li>
     *   <li>Issues ENFORCEMENT_NOTICE_PROP and begins eviction process.</li>
     *   <li>Bribable for 10 COIN to delay eviction by 1 day.</li>
     * </ul>
     * HP: 50f, attack: 0f, cooldown: 0f, hostile: false.
     */
    COUNCIL_ENFORCEMENT_OFFICER(50f, 0f, 0f, false),

    /**
     * RSPCA_OFFICER — RSPCA inspector spawned after player reports dog fight.
     * <ul>
     *   <li>Disperses the DOG_FIGHT_RING_PROP and takes LURCHER_DOG into care.</li>
     *   <li>Triggers TRAVELLER_BOSS to enter HOSTILE state.</li>
     * </ul>
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RSPCA_OFFICER(30f, 0f, 0f, false),

    // ── Issue #1315: Prison Van Escape — The Paddy Wagon Hustle ───────────────

    /**
     * ESCORT_OFFICER — weakened POLICE variant seated in the back of the prison van.
     * 25 HP, unarmed (0 attack damage). Not initially hostile; becomes hostile if
     * bribe is refused or an escape attempt is detected and not interrupted.
     * Can be bribed, distracted, or charmed during the PrisonVanSystem transit window.
     */
    ESCORT_OFFICER(25f, 0f, 0f, false),

    // ── Issue #1317: Northfield Bonfire Night ─────────────────────────────────

    /**
     * FIREWORK_DEALER_NPC — Darren, the dodgy firework trader who sets up behind
     * the off-licence on Bonfire Night. Sells FIREWORK, ROCKET_FIREWORK,
     * BANGER_FIREWORK, and ROMAN_CANDLE at inflated prices. Becomes HOSTILE if
     * tipped off to police (WantedSystem) or robbed (holdall stolen with STEALTH ≥ 2).
     * Non-hostile by default; unarmed.
     */
    FIREWORK_DEALER_NPC(30f, 0f, 0f, false),

    // ── Issue #1319: NatWest Cashpoint — The Dodgy ATM ───────────────────────

    /**
     * MONEY_MULE — Kenny, the local money-mule handler who operates around the
     * NatWest cashpoint on the High Street.
     * <ul>
     *   <li>Sells {@link ragamuffin.building.Material#CARD_SKIMMER_DEVICE} for 25 COIN
     *       on Friday and Saturday evenings (20:00–23:00).</li>
     *   <li>Offers envelope-drop jobs (carry
     *       {@link ragamuffin.building.Material#STUFFED_ENVELOPE} 30 blocks south in
     *       3 minutes for 15 COIN reward).</li>
     *   <li>Non-hostile by default; flees if WantedSystem ≥ 2 stars within 10 blocks.</li>
     * </ul>
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    MONEY_MULE(30f, 0f, 0f, false),

    // ── Issue #1333: Northfield Employment System ─────────────────────────────

    /**
     * GREGGS_MANAGER — runs the Greggs bakery, conducts job interviews, and
     * monitors shifts. Becomes hostile to player at Notoriety Tier 3+.
     * Dismisses player on the spot for theft or fighting on premises.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    GREGGS_MANAGER(20f, 0f, 0f, false),

    /**
     * CORNER_SHOP_OWNER — runs the corner shop, conducts job interviews.
     * Accepts players regardless of criminal record (no criminal record check).
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CORNER_SHOP_OWNER(20f, 0f, 0f, false),

    /**
     * GREASY_SPOON_OWNER — runs the greasy spoon café, conducts breakfast-shift
     * interviews. Non-hostile. No strict requirements beyond basic reliability.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    GREASY_SPOON_OWNER(20f, 0f, 0f, false),

    /**
     * CHARITY_VOLUNTEER_LEADER — runs the charity shop volunteer programme.
     * Conducts volunteer interviews; requires a clean criminal record.
     * Non-hostile. Provides Notoriety reduction on each completed volunteer shift.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CHARITY_VOLUNTEER_LEADER(20f, 0f, 0f, false),

    // ── Issue #1335: Northfield Cycle Centre — Dave's Bikes ───────────────────

    /**
     * CYCLE_SHOP_OWNER — Dave, who runs Dave's Cycle Centre on the high street.
     * Sells bikes, components, and accessories; issues delivery jobs via the
     * JUST_EAT_DELIVERY_BOARD_PROP. Non-hostile. Will not sell to players with
     * Notoriety ≥ 70 (known bike thief reputation).
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CYCLE_SHOP_OWNER(20f, 0f, 0f, false),

    // ── Issue #1337: Northfield Police Station — The Nick ─────────────────────

    /**
     * DESK_SERGEANT — Geoff, the public-facing desk sergeant at Northfield B Division.
     * Staffs the ENQUIRY_COUNTER_PROP 08:00–20:00. Passive toward the player unless
     * bribed (unlocks with COIN at Notoriety ≤ 400) or intimidated via INTIMIDATION
     * street skill (StreetSkillSystem BRAWLING Expert+). Can arrange voluntary
     * surrender (reduces sentence tier by one step in MagistratesCourtSystem).
     * Not armed; calls for backup if attacked.
     * HP: 30f, attack: 5f, cooldown: 2.0f, hostile: false.
     */
    DESK_SERGEANT(30f, 5f, 2.0f, false),

    /**
     * CUSTODY_SERGEANT — the night-custody officer staffing the station 20:00–08:00.
     * Takes over from Geoff at the ENQUIRY_COUNTER_PROP. Slightly more suspicious
     * than the DESK_SERGEANT; bribe cost 40 COIN (Notoriety ≤ 300 only). Does not
     * offer voluntary surrender outside of DESK_SERGEANT hours. Not armed; calls for
     * backup if attacked. Provides access to the custody suite for processing.
     * HP: 30f, attack: 5f, cooldown: 2.0f, hostile: false.
     */
    CUSTODY_SERGEANT(30f, 5f, 2.0f, false),

    // ── Issue #1339: Council Enforcement Day ──────────────────────────────────

    /**
     * DVLA_OFFICER — Karen, the DVLA enforcement officer deployed on Council
     * Enforcement Day (every 14 in-game days, 08:00–17:00). Checks vehicle
     * registration plates at road junctions. If the player has no DRIVING_LICENCE
     * in inventory, the vehicle is towed and NO_INSURANCE_DRIVING is recorded.
     * Escorted by a POLICE_PATROL during the sweep.
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    DVLA_OFFICER(25f, 0f, 0f, false),

    /**
     * BENEFITS_INVESTIGATOR — Phil, the DWP benefits investigator deployed on
     * Council Enforcement Day. Cross-references DWPSystem with EmploymentSystem;
     * if the player is working while claiming benefits, records BENEFIT_FRAUD and
     * adds +2 to WantedSystem. Escorted by a POLICE_PATROL.
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    BENEFITS_INVESTIGATOR(25f, 0f, 0f, false),

    /**
     * WARDEN_TRAINEE — an inexperienced trainee deployed alongside Clive
     * (TRAFFIC_WARDEN) on Council Enforcement Day. Has a 50% error rate — may
     * ticket the wrong car. Their mistake creates a 5-minute free-parking window
     * while Clive is distracted sorting it out. Awards WARDEN_CHAOS achievement
     * when the error fires.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    WARDEN_TRAINEE(20f, 0f, 0f, false),

    // ── Issue #1341: Northfield Residents' Association Meeting ────────────────

    /**
     * RESIDENTS_CHAIR — Margaret, chair of the Northfield Residents' Association.
     * Hostile to the player at Notoriety ≥ 3. Awards NIMBY achievement when she
     * ejects the player from a meeting. Walks to Wetherspoons at 21:30 post-meeting.
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RESIDENTS_CHAIR(30f, 0f, 0f, false),

    /**
     * RESIDENTS_SECRETARY — Kevin, secretary of the Residents' Association.
     * Carries a clipboard with the meeting agenda. Blackmailable for 10 COIN to
     * remove a noise complaint from the agenda; whispers his dark secret as a
     * LOCAL_EVENT rumour on refusal. Walks to Wetherspoons at 21:30.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RESIDENTS_SECRETARY(20f, 0f, 0f, false),

    /**
     * RESIDENTS_TREASURER — Pauline, treasurer of the Residents' Association.
     * Guards the 20 COIN petty cash tin. Has the OBSERVANT trait that counters
     * STEALTH pickpocket attempts. Walks to Wetherspoons at 21:30.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RESIDENTS_TREASURER(20f, 0f, 0f, false),

    /**
     * RESIDENTS_MEMBER — a rank-and-file attendee of the Residents' Association
     * meeting. Each member carries a GRIEVANCE item. 3–6 members spawn per meeting.
     * Can be persuaded to walk out to trigger the Walkout Gambit (dissolves meeting
     * if 2+ members leave).
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RESIDENTS_MEMBER(20f, 0f, 0f, false),

    // ── Issue #1347: Northfield Remembrance Sunday ────────────────────────────

    /**
     * VETERAN — ex-serviceman attending the Remembrance Sunday ceremony at the war
     * memorial (STATUE prop in the park). Attends from 10:30; salutes at wreath-laying
     * (11:02); floods The Ragamuffin Arms after 12:00.
     * Defends the POPPY_SELLER and the war memorial — turns hostile if either is
     * attacked or if the silence (11:00–11:02) is broken.
     * Post-ceremony: good pickpocket window in the pub.
     * HP: 35f, attack: 6f, cooldown: 1.5f, hostile: false.
     */
    VETERAN(35f, 6f, 1.5f, false),

    /**
     * POPPY_SELLER — Doris, stationed outside St. Mary's Church from 09:00 on
     * Remembrance Sunday. Sells POPPY items for 1 COIN each via press-E interaction.
     * Defended by nearby VETERANs. Despawns at 12:30 once ceremony is over.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    POPPY_SELLER(20f, 0f, 0f, false),

    /**
     * RAOB_LODGE_MEMBER — a rank-and-file member of the Royal Antediluvian Order of
     * Buffaloes who marches in the Remembrance Sunday parade from the Lodge to the
     * war memorial. Wears a suit and RAOB sash. Passive during the ceremony.
     * Moves to The Ragamuffin Arms after 12:00 along with VETERANs.
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    RAOB_LODGE_MEMBER(25f, 0f, 0f, false),

    // ── Issue #1349: Northfield RAOB Buffalo Lodge No. 1247 ───────────────────

    /**
     * RAOB_PRIMO_REGENT — Ron, 61, Primo Regent of Northfield Buffaloes Lodge No. 1247.
     * Leads Monday/Thursday evening sessions and the Grand Ceremony. Wears a ceremonial
     * fez and carries the KOMPROMAT_LEDGER. Can be distracted by a PIGEON near the window
     * to open the 30-second safe-heist window. Goes hostile if player is Wanted ≥ 2 and
     * inside the Lodge.
     * HP: 45f, attack: 6f, cooldown: 1.2f, hostile: false.
     */
    RAOB_PRIMO_REGENT(45f, 6f, 1.2f, false),

    // ── Issue #1351: Northfield QuickFix Loans ────────────────────────────────

    /**
     * LOAN_SHARK_CLERK — Darren, who runs QuickFix Loans on Northfield High Street.
     * Offers payday loans of 10, 20, or 40 COIN at 50% interest (40% if player is
     * employed). Open Monday–Saturday 09:00–17:00. Dispatches BAILIFF Terry on
     * day 4 of non-repayment. Marchetti Crew FRIENDLY respect doubles loan cap to 80 COIN.
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    LOAN_SHARK_CLERK(30f, 0f, 0f, false),

    // ── Issue #1353: Northfield Amateur Dramatics Society ─────────────────────

    /**
     * DRAMA_DIRECTOR — Patricia, director of the Northfield Amateur Dramatics Society
     * (NAODS). Oversees rehearsals at the community centre on Wednesdays and Thursdays
     * 19:00–22:00, and the public production on the last Saturday of the month.
     * OBSERVANT: 80% catch rate for pickpocketing during rehearsals. Present at
     * the GP Surgery on Tuesday 13:00–17:00 (opening the costume heist window).
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    DRAMA_DIRECTOR(30f, 0f, 0f, false),

    /**
     * NAODS_LEAD_ACTOR — Mario, a Marchetti lieutenant moonlighting as lead actor
     * in the NAODS production of Blood Brothers. Takes a notes break 21:15–21:20
     * during rehearsal nights — eavesdropping on him during this window seeds the
     * MARCHETTI_SECRETS rumour (faction intel). Accepts sabotage contract for 15 COIN
     * if player has MARCHETTI Respect ≥ 20.
     * HP: 35f, attack: 5f, cooldown: 1.5f, hostile: false.
     */
    NAODS_LEAD_ACTOR(35f, 5f, 1.5f, false),

    /**
     * NAODS_MEMBER — a rank-and-file member of the Northfield Amateur Dramatics
     * Society. Attends rehearsals and the public production. Can spot non-cast
     * wearers of STAGE_COSTUME with a 40% chance, triggering a confrontation.
     * 8–12 members attend each rehearsal — prime pickpocket opportunity.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    NAODS_MEMBER(20f, 0f, 0f, false),

    // ── Issue #1357: Northfield Charity Fun Run ────────────────────────────────

    /**
     * FUN_RUN_MARSHAL — Janet, the cheerful registration marshal at the Northfield
     * Community Centre Charity Fun Run. Stationed at the START_FINISH_ARCH_PROP.
     * Press E to register (2 COIN entry): receives RACE_NUMBER_BIB + SPONSOR_SHEET.
     * Carries the registration pot (up to 40 COIN) in her bum bag — pickpocketing
     * steals up to 40 COIN (THEFT_FROM_PERSON, Notoriety +5). Volunteer-as-assistant
     * embezzlement path also available (press E while wearing HIGH_VIS_JACKET).
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    FUN_RUN_MARSHAL(25f, 0f, 0f, false),

    // ── Issue #1359: Northfield HMRC Tax Investigation ────────────────────────

    /**
     * HMRC_INSPECTOR — Sandra Watts, HMRC compliance inspector. Spawns at the
     * player's address at 09:00 on a weekday when {@code totalUntaxedEarnings >= 150 COIN}.
     * Attempts to serve a TAX_DEMAND_LETTER (30% of untaxed earnings, capped at 80 COIN).
     * Invincible (cannot be killed). Will not pursue if player flees.
     * HP: Float.MAX_VALUE, attack: 0f, cooldown: 0f, hostile: false.
     */
    HMRC_INSPECTOR(Float.MAX_VALUE, 0f, 0f, false),

    /**
     * DISTRAINT_OFFICER — Derek, court-appointed distraint (bailiff) officer. Spawns
     * after the player ignores the TAX_DEMAND_LETTER for 2 in-game days. Seizes goods
     * from the player's inventory up to the value of the tax demand. On day 5 (dawn
     * raid at 06:30), Derek arrives with 2× HMRC_INSPECTOR + enforcement officer.
     * HP: 40f, attack: 5f, cooldown: 1.5f, hostile: false (becomes hostile only on assault).
     */
    DISTRAINT_OFFICER(40f, 5f, 1.5f, false),

    // ── Issue #1361: Northfield St. Margaret's Church Hall Jumble Sale ─────────

    /**
     * VICAR — Reverend Dave, the cheerfully incompetent auctioneer and donation
     * drop-off manager at St. Margaret's Church Hall. Runs the donation volunteer
     * shift 08:00–09:00 and the Mystery Box auction at 12:00. Passive (never
     * attacks). Can be bribed with 5 COIN to reveal the SCORE mystery box.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    VICAR(20f, 0f, 0f, false),

    /**
     * CHURCH_LADY — one of two till operators at St. Margaret's Church Hall jumble
     * sale. Acts as an unwitting fence intermediary; buys items at 60% fence value
     * (no questions asked, up to 3 items per session). Seeds COMMUNITY_WIN rumours.
     * HP: 15f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CHURCH_LADY(15f, 0f, 0f, false),

    // ── Issue #1363: Northfield Sunday Car Boot Sale ───────────────────────────

    /**
     * BOOT_SALE_ORGANISER — Barry the car boot sale organiser. Collects pitch
     * fees (3 COIN) from vendors before 06:15, manages the council car park
     * site (06:00–12:00 Sundays), and calls Clive (TRAFFIC_WARDEN) after noon
     * to clear remaining pitches. HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    BOOT_SALE_ORGANISER(25f, 0f, 0f, false),

    /**
     * DODGY_VENDOR — Derek the dodgy vendor. Offers stolen goods to players
     * with Street Rep ≥ 30. Buys and sells at 50% fence value with no notoriety
     * unless NEIGHBOURHOOD_WATCH is nearby. Retreats from TRADING_STANDARDS_OFFICER
     * NPCs. HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    DODGY_VENDOR(20f, 0f, 0f, false),

    /**
     * BOOT_SALE_VENDOR — one of 4–8 regular vendors at the Sunday car boot
     * sale. Sells junk items from a folding table and open car boot. Can be
     * haggled with (70% price, 50% acceptance rate). After 3 failed haggles
     * becomes VENDOR_DISPUTE hostile. HP: 20f, attack: 3f, cooldown: 2f, hostile: false.
     */
    BOOT_SALE_VENDOR(20f, 3f, 2f, false),

    /**
     * BOOT_SALE_PUNTER — one of 6–12 bargain-hunting punters competing with
     * the player for items at the Sunday car boot sale. Auto-buys from player
     * pitches every 2 in-game minutes. HP: 15f, attack: 0f, cooldown: 0f, hostile: false.
     */
    BOOT_SALE_PUNTER(15f, 0f, 0f, false),

    /**
     * TRADING_STANDARDS_OFFICER — spawns during TRADING_STANDARDS_STING event
     * when 3+ stolen goods have been sold openly. Records TRADING_STANDARDS_BUST
     * crime, adds Notoriety +10, triggers NewspaperSystem headline. HP: 30f,
     * attack: 0f, cooldown: 0f, hostile: false.
     */
    TRADING_STANDARDS_OFFICER(30f, 0f, 0f, false),

    // ── Issue #1365: Northfield Bonfire Night ─────────────────────────────────

    /**
     * BONFIRE_WARDEN — Gary, the volunteer bonfire warden who patrols the park
     * bonfire from 18:00 until 21:00. Detection radius is halved after 21:00
     * when he gets distracted with his flask. Non-hostile unless provoked.
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    BONFIRE_WARDEN(25f, 0f, 0f, false),

    /**
     * EVENT_COMPERE — Keith, the official Tesco car park firework display
     * compère. Announces the display, oversees the FIREWORK_MORTAR_PROP, and
     * reacts angrily if the display is sabotaged. Non-hostile.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    EVENT_COMPERE(20f, 0f, 0f, false),

    // ── Issue #1371: Northfield Christmas Market ──────────────────────────────

    /**
     * SANTA_CLAUS — Terry in full Father Christmas costume manning Santa's Grotto.
     * Non-hostile (it's Christmas). Pickpocketable for SANTA_BADGE.
     * Becomes distracted when grotto queue ≥ 3 (enabling GROTTO_TIN theft).
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false.
     */
    SANTA_CLAUS(30f, 0f, 0f, false),

    /**
     * CAROL_SINGER — one of 3–5 volunteer carol singers performing at the
     * CAROL_SONG_BOARD_PROP 17:00–19:00. Pickpocketable (SLEIGHT_OF_HAND).
     * Flees on firework disruption. Non-hostile.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CAROL_SINGER(20f, 0f, 0f, false),

    /**
     * XMAS_STALL_VENDOR — generic Christmas market stall vendor (Carol, Dietmar,
     * Linda, Margaret). Non-hostile traders who staff their XMAS_MARKET_CHALET_PROP
     * during market hours 10:00–20:00.
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    XMAS_STALL_VENDOR(25f, 0f, 0f, false),

    // ── Issue #1373: Northfield Local Council Elections ───────────────────────

    /** CANDIDATE_NPC — one of the three ward candidates (Patricia Holt, Steve Brannigan,
     * Nikhil Patel). Stands at CANDIDATE_TABLE_PROP during canvassing week.
     * Press E to pledge support or volunteer a leafleting shift.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    CANDIDATE_NPC(20f, 0f, 0f, false),

    /** CANVASSER_NPC — campaign volunteer staffing LEAFLET_PILE_PROP for a candidate.
     * Detects PERMANENT_MARKER sabotage (40% chance). Raises alarm if caught.
     * HP: 15f, attack: 0f, cooldown: 0f, hostile: false. */
    CANVASSER_NPC(15f, 0f, 0f, false),

    /** POLLING_OFFICER_NPC — Barry, the returning officer's clerk stationed at
     * POLLING_STATION_PROP on Polling Day. Enforces the 3-block exclusion zone
     * and can be bribed for 10 COIN to look away for 1 in-game hour.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    POLLING_OFFICER_NPC(20f, 0f, 0f, false),

    /** COUNT_OBSERVER — generic town-hall attendee at Count Night from 22:30.
     * Witnesses ballot events and reacts to results announcement.
     * HP: 15f, attack: 0f, cooldown: 0f, hostile: false. */
    COUNT_OBSERVER(15f, 0f, 0f, false),

    // ── Issue #1381: Northfield Bank Holiday Street Party ─────────────────────

    /** NEIGHBOURHOOD_WATCH — Gerald, the Neighbourhood Watch chairman who submitted
     * the road-closure application. Appears at 18:30 with a CLIPBOARD_PROP to issue
     * noise complaints and threaten to call the council.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    NEIGHBOURHOOD_WATCH(20f, 0f, 0f, false),

    // ── Issue #1386: Northfield St George's Day ───────────────────────────────

    /** MORRIS_DANCER — one of six traditional Morris Dancers who perform in the park
     * 11:00–15:00 on St George's Day. Carries MORRIS_STICK_PROP. Pursues player
     * if stick is stolen (becomes HOSTILE during pursuit).
     * HP: 25f, attack: 4f, cooldown: 1.5f, hostile: false (true if stick stolen). */
    MORRIS_DANCER(25f, 4f, 1.5f, false),

    /** ENGLAND_FLAG_NPC — nationalist supporter carrying an England flag. Converges
     * on the park from 14:00 on St George's Day. Within 5 blocks of a
     * COUNTER_PROTEST_NPC triggers CROWD_SCUFFLE event.
     * HP: 25f, attack: 5f, cooldown: 1.2f, hostile: false. */
    ENGLAND_FLAG_NPC(25f, 5f, 1.2f, false),

    /** COUNTER_PROTEST_NPC — counter-protest attendee. Converges from 14:00 on
     * St George's Day. Within 5 blocks of ENGLAND_FLAG_NPC triggers CROWD_SCUFFLE.
     * HP: 25f, attack: 5f, cooldown: 1.2f, hostile: false. */
    COUNTER_PROTEST_NPC(25f, 5f, 1.2f, false),

    /** TERRY_BARMAN — Terry, the Wetherspoons barman who runs the St George's Day
     * lock-in. Sells DOOM_BAR_PINT during 11:00–02:00. Can be distracted to
     * disable CCTV awareness during the flag heist.
     * HP: 30f, attack: 0f, cooldown: 0f, hostile: false. */
    TERRY_BARMAN(30f, 0f, 0f, false),

    /** MIREK_FENCE — Mirek, the local fence who buys stolen St George's Day items.
     * Buys ST_GEORGE_FLAG_PROP for 5 COIN, ROOF_FLAG_PROP for 12 COIN.
     * Present all day on St George's Day.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    MIREK_FENCE(20f, 0f, 0f, false),

    // ── Issue #1394: England Match Night ─────────────────────────────────────

    /** MATCH_BOOKIE — Barry; takes scoreline bets before kick-off (before 20:00).
     * Flees pub at full-time if England lost and player bet on OPPOSITION_WIN.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    MATCH_BOOKIE(20f, 0f, 0f, false),

    /** PUB_STEWARD — Terry; patrols the Wetherspoons doorway during England matches.
     * Ejects players with Notoriety ≥ 100. Can be bribed (5 COIN).
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false. */
    PUB_STEWARD(25f, 0f, 0f, false),

    /** RIVAL_FAN — 1–2 NPCs wearing opposition colours; spawns if England concede.
     * Goads the crowd; 60% chance of triggering a CROWD_BRAWL.
     * HP: 20f, attack: 5f, cooldown: 2.0f, hostile: false. */
    RIVAL_FAN(20f, 5f, 2.0f, false),

    /** WETHERSPOONS_CROWD — dense passive crowd NPC packed into the pub for the match.
     * Reacts to goal events, half-time, and full-time. State = WATCHING_MATCH.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    WETHERSPOONS_CROWD(20f, 0f, 0f, false),

    /** KEVINS_DAD — Gary, Kevin's competitive father. Spawns only at Sports Day.
     * Cheats blatantly at Parents' Race (shoves NPCs, cuts corners). 85% win rate
     * if not bribed or tripped. Quote: "It's not about winning, Kevin. Yes it is."
     * HP: 22f, attack: 0f, cooldown: 0f, hostile: false. */
    KEVINS_DAD(22f, 0f, 0f, false),

    // ── Issue #1396: Northfield Royal Mail Strike ─────────────────────────────

    /** STRIKER — Postal worker on the picket line outside the sorting office.
     * Patrols slowly with PLACARD_PROP. Becomes HOSTILE (state=ANGRY) on seeing a
     * scab player but does not attack unless attacked first. Shouts "Scab!" speech bubbles.
     * HP: 20f, attack: 3f, cooldown: 1.5f, hostile: false. */
    STRIKER(20f, 3f, 1.5f, false),

    /** POSTAL_MANAGER — management representative who arrives at 17:00 on strike day 3.
     * Passive NPC; triggers strike resolution dialogue. No combat capability.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    POSTAL_MANAGER(20f, 0f, 0f, false),

    // ── Issue #1398: Northfield Window Cleaner ────────────────────────────────

    /** WINDOW_CLEANER — Terry, the window cleaner who does his residential round Mon–Fri 08:30–16:00.
     * Follows a fixed 12-property route, spending 90 in-game seconds at each. Places LADDER_PROP
     * against walls. Oblivious to player on ladder (facing wall). Becomes HOSTILE if player
     * poaches his round. HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    WINDOW_CLEANER(20f, 0f, 0f, false),

    // ── Issue #1400: Northfield Residents' Parking Permit Racket ─────────────

    /** TRAFFIC_WARDEN_BARRY — Barry, the RPZ traffic warden who patrols every 20 in-game minutes
     * 08:00–18:00 and clamps unregistered cars with WHEEL_CLAMP_PROP.
     * Can be bribed (3/8/20 COIN) or observed seeding BENT_WARDEN rumour.
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false. */
    TRAFFIC_WARDEN_BARRY(25f, 0f, 0f, false),

    /** COUNCIL_CLERK — Brenda, the council office clerk who sells PARKING_PERMIT for 4 COIN.
     * Stationed at the COUNCIL_OFFICE_KIOSK prop. Passive NPC.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    COUNCIL_CLERK(20f, 0f, 0f, false),

    /** REPAIR_CREW_NPC — council repair crew member who restores defaced RPZ_SIGN_PROP
     * back to RPZ_SIGN_PROP after 3 in-game hours. Arrives 30 in-game minutes after sign is defaced.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    REPAIR_CREW_NPC(20f, 0f, 0f, false),

    /** DESPERATE_PARKER_NPC — a desperate driver who will buy a PARKING_PERMIT from the player
     * for up to 12 COIN (scalping mechanic). Triggers HMRC income tracking.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    DESPERATE_PARKER_NPC(20f, 0f, 0f, false),

    // ── Issue #1402: Northfield Severn Trent Road Dig ─────────────────────────

    /** CONTRACTOR_STEVE — senior Severn Trent contractor. On site 08:00–16:00 during dig event.
     * Bribeable (BRIBE_LOW/MID/HIGH). Carries THERMOS. Can be threatened if Notoriety &lt; 20.
     * HP: 22f, attack: 5f, cooldown: 0f, hostile: false. */
    CONTRACTOR_STEVE(22f, 5f, 0f, false),

    /** CONTRACTOR_PHIL — junior contractor. Tea-break vulnerable; on site 08:00–16:00.
     * Will ask for THERMOS when player is in contractor disguise. Calls police if threatened at high Notoriety.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false. */
    CONTRACTOR_PHIL(20f, 0f, 0f, false),

    /** ROAD_RAGE_NPC — angry driver spawned when car enters ROAD_TRENCH_PROP closure or traffic chaos.
     * Brawls with other ROAD_RAGE_NPCs; aggressive toward player. Despawns after 5 in-game minutes.
     * HP: 25f, attack: 10f, cooldown: 0f, hostile: true. */
    ROAD_RAGE_NPC(25f, 10f, 0f, true),

    // ── Issue #1404: Northfield Community Litter Pick ─────────────────────────

    /**
     * LITTER_PICK_COORDINATOR — Janet, who runs the Northfield Tidy Streets community litter pick.
     * <ul>
     *   <li>Spawns at LITTER_PICK_TENT_PROP outside park gates every second Saturday 09:00–11:00.</li>
     *   <li>Hands out LITTER_PICKER_STICK + COUNCIL_RUBBISH_BAG on press E (before 09:15).</li>
     *   <li>Refuses equipment if player arrives after 09:15.</li>
     *   <li>Issues up to 2 trespassing warnings; on 3rd violation ejects player (Notoriety +2).</li>
     *   <li>Calls police immediately if returned bag contains CRACK_PIPE.</li>
     *   <li>Recognises GARDEN_ORNAMENT in player inventory (Notoriety +5, police alert; bribable with CHOCOLATE_BAR 50%).</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    LITTER_PICK_COORDINATOR(20f, 0f, 0f, false),

    /**
     * VOLUNTEER_PICKER — community volunteer at the litter pick event.
     * <ul>
     *   <li>Wanders park area collecting LITTER_PROP items 09:00–11:00.</li>
     *   <li>Distracted while bending down (60% pickpocket success, yields 1–3 COIN).</li>
     *   <li>Can have COUNCIL_RUBBISH_BAG stolen for up to 5 pre-collected items toward quota.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    VOLUNTEER_PICKER(20f, 0f, 0f, false),

    // ── Issue #1406: Northfield Dodgy Roofer ──────────────────────────────────

    /**
     * DODGY_ROOFER — Kenny, the cold-calling roofer.
     * <ul>
     *   <li>Patrols 8 residential properties/day weekdays 09:00–16:00 in his ROOFER_VAN_PROP.</li>
     *   <li>Pitches fabricated roof/guttering faults to PENSIONER NPCs (35% acceptance).</li>
     *   <li>Places LADDER_PROP and 'works' for 45 seconds before collecting 6 COIN.</li>
     *   <li>Goes HOSTILE if he spots the player within 15 blocks while mid-pitch.</li>
     *   <li>Is BUSY (unattended van) while on a doorstep or up a ladder.</li>
     * </ul>
     * HP: 35f, attack: 6f, cooldown: 2.0f, hostile: false (until triggered).
     */
    DODGY_ROOFER(35f, 6f, 2.0f, false),

    // ── Issue #1416: Northfield Mobile Speed Camera Van ───────────────────────

    /**
     * CAMERA_OPERATOR_NPC — Sharon, the mobile speed camera operator.
     * <ul>
     *   <li>Sits in {@link ragamuffin.world.PropType#SPEED_CAMERA_VAN_PROP} reading a tabloid.</li>
     *   <li>Active weekdays 08:00–09:30 and 15:30–17:00 near the school.</li>
     *   <li>Photographs cars exceeding {@code SpeedCameraVanSystem.SPEED_LIMIT_MPH} within
     *       {@code SpeedCameraVanSystem.CAMERA_RANGE_BLOCKS} blocks.</li>
     *   <li>Can be distracted via {@link ragamuffin.world.PropType#TABLOID_RACK_PROP} for 25 seconds.</li>
     *   <li>Radios police (WantedSystem +1) after 5 player tip-offs.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    CAMERA_OPERATOR_NPC(20f, 0f, 0f, false),

    /**
     * SPEEDING_DRIVER_NPC — a driver approaching or passing the speed camera location.
     * <ul>
     *   <li>Randomly exceeds {@code SpeedCameraVanSystem.SPEED_LIMIT_MPH} (30 mph).</li>
     *   <li>Player can wave one down (E) for a {@code SpeedCameraVanSystem.TIP_OFF_COIN_REWARD} COIN tip-off.</li>
     *   <li>Can be sold a stolen {@link ragamuffin.building.Material#SPEED_CAMERA_SD_CARD} for 15 COIN
     *       + {@link ragamuffin.core.RumourType#GRATEFUL_DRIVER} rumour.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    SPEEDING_DRIVER_NPC(20f, 0f, 0f, false),

    // ── Issue #1420: Northfield Post Office Horizon Scandal ──────────────────

    /**
     * POST_OFFICE_INVESTIGATOR — Derek Swann. Suit-wearing, briefcase. Arrives day 14 at 09:30
     * to serve Maureen the Horizon shortfall letter.
     * <ul>
     *   <li>Non-aggressive unless player obstructs audit (then calls police, WantedSystem +1).</li>
     *   <li>Audits the back room 10:00–12:00 daily until tribunal day 17.</li>
     *   <li>Leaves after tribunal regardless of outcome.</li>
     * </ul>
     * HP: 25f, attack: 0f, cooldown: 0f, hostile: false.
     */
    POST_OFFICE_INVESTIGATOR(25f, 0f, 0f, false),

    /**
     * IT_CONTRACTOR — Pete. Polo shirt, lanyard, nervous demeanour.
     * Arrives day 15 at 11:00 in a rented Astra to verify the Horizon system logs.
     * <ul>
     *   <li>Carries {@link ragamuffin.building.Material#IT_CONTRACTOR_ID_BADGE} and
     *       {@link ragamuffin.building.Material#USB_STICK} — both pickpocketable.</li>
     *   <li>Flees if threatened (enters NPCState.FLEEING).</li>
     *   <li>Can be bribed for 10 COIN to alter his report (peteReportAltered = true).</li>
     *   <li>Assaulting him records {@code AUDIT_OBSTRUCTION} and delays report by 1 day.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    IT_CONTRACTOR(20f, 0f, 0f, false),

    /**
     * REGIONAL_AUDITOR — unnamed suit. Loiters outside the Post Office from day 14.
     * <ul>
     *   <li>Pays 12 COIN for {@link ragamuffin.building.Material#TRANSACTION_LOG} items
     *       (fence value otherwise 4 COIN).</li>
     *   <li>Selling logs to this NPC seeds {@link ragamuffin.core.RumourType#DODGY_AUDIT} and
     *       flips tribunal outcome to CONVICTED.</li>
     *   <li>Departs after tribunal day 17.</li>
     * </ul>
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    REGIONAL_AUDITOR(20f, 0f, 0f, false),

    // ── Issue #1422: Northfield Charity Sponsored Walk ────────────────────────

    /**
     * WALK_ORGANISER — Brenda. Hi-vis tabard, clipboard, sensible shoes.
     * Friendly until wronged; then relentless pursuer. Non-violent but calls police on contact.
     * Spawned by SponsoredWalkSystem on day 10 at 08:30 outside the Community Centre.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    WALK_ORGANISER(20f, 0f, 0f, false),

    /**
     * SPONSORED_WALKER — generic public NPC variant: hi-vis bib, trainers, number pinned to chest.
     * 8–12 spawned for the walk on day 10; despawn at 10:30.
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    SPONSORED_WALKER(20f, 0f, 0f, false),

    // ── Issue #1424: Northfield Doorstep Energy Tout ──────────────────────────

    /**
     * ENERGY_TOUT — Craig. Polo shirt, branded PowerSave UK tabard, clipboard, fake badge.
     * Walks the terrace street every 4 in-game days, knocking doors and posing as a meter
     * reader, scamming residents out of 8 COIN each.
     * <ul>
     *   <li>Non-hostile unless his clipboard is stolen; then turns HOSTILE briefly.</li>
     *   <li>Carries {@link ragamuffin.building.Material#TOUT_CLIPBOARD} (pickpocketable at Stealth ≥ 1).</li>
     *   <li>Can be reported via {@link ragamuffin.building.Material#CRAIG_WITNESS_STATEMENT}
     *       to CitizensAdvice to shut him down.</li>
     *   <li>His {@link ragamuffin.world.PropType#ENERGY_VAN_PROP} can be broken into for
     *       {@link ragamuffin.building.Material#SMART_METER_KIT}.</li>
     * </ul>
     * HP: 25f, attack: 3f, cooldown: 2.0f, hostile: false.
     */
    ENERGY_TOUT(25f, 3f, 2.0f, false),

    /**
     * TOUT_ENFORCER — Dave. Craig's enforcer, sent when the player runs Craig's own round
     * (3+ doorstep knocks with the stolen clipboard = {@code DOORSTEP_FRAUD} crime).
     * <ul>
     *   <li>Arrives 1 in-game day after the DOORSTEP_FRAUD crime is recorded.</li>
     *   <li>Carries {@link ragamuffin.building.Material#BURNER_PHONE} (droppable on defeat,
     *       fence value 9 COIN).</li>
     *   <li>Hostile on spawn; pursues player for 90 seconds before despawning.</li>
     * </ul>
     * HP: 40f, attack: 8f, cooldown: 1.5f, hostile: true.
     */
    TOUT_ENFORCER(40f, 8f, 1.5f, true),

    // ── Issue #1426: Northfield Neighbourhood WhatsApp Group ─────────────────

    /**
     * WHATSAPP_GROUP_ADMIN — Janet. Cardigan, reading glasses, phone permanently in hand.
     * Passive unless provoked; admin of the residents WhatsApp group and enforces LOCKDOWN_MODE.
     * Non-violent: calls police if harassed (WantedSystem +1).
     * HP: 20f, attack: 0f, cooldown: 0f, hostile: false.
     */
    WHATSAPP_GROUP_ADMIN(20f, 0f, 0f, false),

    /**
     * LOST_CAT_NPC — Whiskers. Small tabby cat sprite (re-uses animal NPC logic).
     * Wanders within 20 blocks of spawn. Interactable (E to pick up → {@code STRAY_CAT} Material).
     * HP: 5f, attack: 0f, cooldown: 0f, hostile: false.
     */
    LOST_CAT_NPC(5f, 0f, 0f, false),

    // ─────────────────────────────────────────────────────────────────────────
    // Issue #1433: Northfield Easter Weekend
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BIKER_NPC — Charity motorbike parade participant.
     * Parades along HIGH_STREET to park on Easter Sunday (day 93, 11:00–14:00).
     * Becomes HOSTILE if charity bucket is stolen. HP: 80, attack: 6, cooldown: 1.5s.
     */
    BIKER_NPC(80f, 6f, 1.5f, false),

    /**
     * EASTER_BUNNY_NPC — Council volunteer in Easter Bunny costume.
     * Present during egg hunt (day 92, 10:00–13:00). Pickpocketable for CHOCOLATE_EGG ×3.
     * HP: 40, attack: 0, cooldown: 0, not hostile.
     */
    EASTER_BUNNY_NPC(40f, 0f, 0f, false),

    /**
     * EASTER_EGG_HUNT_WARDEN — Brenda (council volunteer). Manages the egg hunt.
     * Hides 15 EASTER_EGG_PROPs in the park on day 92 before 10:00.
     * HP: 50, attack: 0, cooldown: 0, not hostile.
     */
    EASTER_EGG_HUNT_WARDEN(50f, 0f, 0f, false),

    // ── Issue #1449: Northfield Mobile Library ───────────────────────────────

    /**
     * MOBILE_LIBRARIAN — Keith, a gentle ex-social-worker who drives the mobile library van.
     * Parks outside the community centre Wednesday 10:00–13:00 and Saturday 10:00–12:00.
     * Issues LIBRARY_CARD on first visit; tracks overdue fines; runs weekly amnesty event.
     * HP: 60, attack: 0, cooldown: 0, not hostile.
     */
    MOBILE_LIBRARIAN(60f, 0f, 0f, false),

    /**
     * LIBRARY_REGULAR — a habitual mobile library visitor who distracts Keith with chat.
     * When present and in conversation with Keith, creates a no-line-of-sight window for
     * the RARE_BOOK_SHELF_PROP theft (3s hold-E). Sits in LIBRARY_REGULAR_SEAT_PROP.
     * HP: 50, attack: 0, cooldown: 0, not hostile.
     */
    LIBRARY_REGULAR(50f, 0f, 0f, false);

    private final float maxHealth;
    private final float attackDamage;   // Damage per hit to player
    private final float attackCooldown; // Seconds between attacks
    private final boolean hostile;      // Will actively seek and attack player

    NPCType(float maxHealth, float attackDamage, float attackCooldown, boolean hostile) {
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.attackCooldown = attackCooldown;
        this.hostile = hostile;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public float getAttackDamage() {
        return attackDamage;
    }

    public float getAttackCooldown() {
        return attackCooldown;
    }

    public boolean isHostile() {
        return hostile;
    }
}

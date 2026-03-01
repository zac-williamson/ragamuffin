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
    VICAR(15f, 0f, 0f, false),

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
    MEMBER(25f, 3f, 2.0f, false);

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

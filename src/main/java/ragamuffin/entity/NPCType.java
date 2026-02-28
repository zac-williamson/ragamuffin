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
    STRAY_DOG(15f, 0f, 0f, false);

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

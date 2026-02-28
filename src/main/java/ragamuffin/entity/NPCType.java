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
    PIT_SPECTATOR(20f, 0f, 0f, false);

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

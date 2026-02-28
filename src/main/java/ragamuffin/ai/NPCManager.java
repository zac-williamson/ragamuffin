package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.BlockBreaker;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.building.StructureTracker;
import ragamuffin.core.NoiseSystem;
import ragamuffin.core.ShelterDetector;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.FacialHairType;
import ragamuffin.entity.HairstyleType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCModelVariant;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import java.util.*;

/**
 * Manages all NPCs in the game - spawning, updating, AI behavior.
 */
public class NPCManager {

    private final List<NPC> npcs;
    private final Pathfinder pathfinder;
    private final Random random;
    private float gameTime; // Game time in hours (0-24)
    private int previousTimeBand = -1; // -1 = uninitialised; 0=night, 1=work, 2=evening
    private int previousSchoolBand = -1; // -1 = uninitialised; 0=out-of-school, 1=going-to-school, 2=at-school, 3=leaving-school

    // Maximum NPC count to prevent lag
    private static final int MAX_NPCS = 100;

    // Park boundaries (assumed centered at 0,0)
    private static final float PARK_MIN_X = -20;
    private static final float PARK_MAX_X = 20;
    private static final float PARK_MIN_Z = -20;
    private static final float PARK_MAX_Z = 20;

    // Random ambient speech lines
    private static final String[] RANDOM_PUBLIC_SPEECH = {
        "Is that... legal?", "My council tax pays for this?", "I'm calling the council.",
        "Bit rough, innit?", "You alright, love?", "State of this place.",
        "Have you tried the JobCentre?", "Mind yourself, yeah?",
        "This used to be a nice area.", "You look like you need a Greggs.",
        "Shocking, absolutely shocking.", "Can't get a GP appointment for weeks.",
        "The bins haven't been collected again.", "Nice day for it."
    };
    private static final String[] RANDOM_YOUTH_SPEECH = {
        "Oi!", "What you looking at?", "This is our patch.",
        "Got any change?", "Nice phone, that.", "Bare wasteman.",
        "You want some, yeah?"
    };
    private static final String[] RANDOM_POLICE_SPEECH = {
        "Move along.", "Evening.", "Keep it civil."
    };
    private static final String[] RANDOM_SHOPKEEPER_SPEECH = {
        "Browse all you like, love.", "We're closing in ten minutes.",
        "Two for one on crisps.", "Got your Clubcard?", "Self-service is broken again."
    };
    private static final String[] RANDOM_POSTMAN_SPEECH = {
        "Another parcel for number 12.", "Dog nearly had me leg.",
        "This rain'll ruin these letters.", "I'm on my third round."
    };
    private static final String[] RANDOM_JOGGER_SPEECH = {
        "*panting*", "Morning!", "On your left!", "Just five more k..."
    };
    private static final String[] RANDOM_DRUNK_SPEECH = {
        "You're my best mate, you are.", "I love you, man.", "*hic*",
        "This town's gone to the dogs.", "Who moved the pavement?",
        "I'm not drunk, you're drunk."
    };
    private static final String[] RANDOM_BUSKER_SPEECH = {
        "♪ Wonderwall ♪", "♪ No Woman No Cry ♪", "Any spare change?",
        "♪ Hey Jude ♪", "Requests cost a quid."
    };
    private static final String[] RANDOM_DELIVERY_SPEECH = {
        "Where's number 42?", "Parcel for... can't read this.", "Left it behind the bin.",
        "That's my third missed delivery.", "Just leave it with a neighbour."
    };
    private static final String[] RANDOM_PENSIONER_SPEECH = {
        "In my day...", "These prices!", "Nobody says hello anymore.",
        "I remember when this was all fields.", "Shocking behaviour.",
        "Young people today, honestly.", "Is this the queue?"
    };
    private static final String[] RANDOM_SCHOOL_KID_SPEECH = {
        "Bruv!", "That's well peak!", "Safe, yeah?",
        "Can I have a quid?", "Have you got games on your phone?",
        "Oi, look at that!", "That's bare jokes!"
    };
    private static final String[] RANDOM_STREET_PREACHER_SPEECH = {
        "Repent, for the end is nigh!", "The Lord sees all!", "Have you found salvation?",
        "Turn away from your wicked ways!", "Praise be!", "The kingdom of heaven is at hand.",
        "God bless you, friend.", "Have you accepted Jesus into your heart?"
    };
    private static final String[] RANDOM_LOLLIPOP_LADY_SPEECH = {
        "Mind how you go, love!", "Wait for the gap, children!", "Lovely day, isn't it?",
        "Stop for the little ones!", "All clear, off you go!", "Keep left on the pavement!",
        "Bless them, they're in such a rush."
    };
    private static final String[] RANDOM_TUNNEL_DWELLER_SPEECH = {
        "You're not supposed to be down here.", "This is my patch.", "What do you want?",
        "I've been down here... a while.", "It's quieter underground.", "Stay away from my stuff.",
        "The surface is overrated.", "Council don't know I'm here.", "Got any food?",
        "Light hurts me eyes.", "Don't touch nothing.", "I know every inch of these tunnels."
    };
    private static final String[] HIT_SPEECH_PUBLIC = {
        "Oi! What was that for?!", "Help! I'm being assaulted!", "Right, I'm ringing 999!",
        "Are you mental?!", "That's ABH, that is!"
    };
    private static final String[] HIT_SPEECH_YOUTH = {
        "You're dead, bruv!", "Big mistake, fam!", "That all you got?",
        "Come on then!", "I'll shank ya!"
    };
    private static final String[] HIT_SPEECH_POLICE = {
        "That's assaulting an officer!", "Right, you're nicked!", "Backup requested!",
        "Resisting arrest!", "I'll taser you!"
    };

    // Issue #910: NPC dialogue lines when acknowledging player constructions
    private static final String[] CONSTRUCTION_STARING_SPEECH = {
        "What on earth is that?", "Is that... legal?", "Someone's been busy.",
        "I've seen some things but...", "That's a bit much, isn't it?",
        "No planning permission, I'd wager.", "Well. There it is."
    };
    private static final String[] CONSTRUCTION_PHOTOGRAPHING_SPEECH = {
        "I'm getting this on camera.", "That's going on social media.",
        "The council will want to see this.", "Evidence. Pure evidence.",
        "My followers won't believe this.", "Screenshot. Screenshot. Screenshot."
    };
    private static final String[] CONSTRUCTION_COMPLAINING_SPEECH = {
        "This is an absolute eyesore!", "I'm ringing the council!",
        "That shouldn't be allowed!", "The neighbourhood is going downhill.",
        "My house value just dropped.", "Absolutely disgraceful.",
        "Someone will hear about this!", "Not in my back yard!"
    };

    // NPC-to-NPC dialogue exchanges — each entry is a [initiator_line, responder_line] pair.
    // The initiator speaks first; the responder replies after a short delay.
    // Indexed as: NPC_TO_NPC_EXCHANGES[pairIndex][0] = initiator, [1] = responder.

    // PUBLIC ↔ PUBLIC
    private static final String[][] PUBLIC_PUBLIC_EXCHANGES = {
        {"Alright?", "Yeah, not bad. You?"},
        {"Did you see that planning notice?", "Absolutely disgraceful. Six storeys!"},
        {"The bins still haven't been done.", "Rang the council three times."},
        {"I'm thinking of moving, honest.", "Can't blame you, this street's gone."},
        {"Weather's turned again.", "Always does this time of year."},
        {"Have you tried that new place on the high street?", "It's alright, bit pricey."},
        {"Terrible about Mrs Henderson.", "I know. She was 94, though."},
    };

    // PUBLIC ↔ PENSIONER
    private static final String[][] PUBLIC_PENSIONER_EXCHANGES = {
        {"Morning!", "Is it? Could've fooled me."},
        {"You alright getting about?", "My knee's not what it was."},
        {"Have you got far to go?", "Just to the post office, if it's still open."},
    };

    // PENSIONER ↔ PENSIONER
    private static final String[][] PENSIONER_PENSIONER_EXCHANGES = {
        {"They've changed the bus route again.", "I know. Had to walk from the terminus."},
        {"Prices in Greggs now!", "I remember when a sausage roll was 20p."},
        {"My grandson says it's all online now.", "I told him, I'm not putting my bank details on a website."},
        {"Are you in the queue?", "I don't know. I think so."},
    };

    // POLICE ↔ PUBLIC
    private static final String[][] POLICE_PUBLIC_EXCHANGES = {
        {"Nothing to worry about, sir.", "I wasn't worried until now."},
        {"Move along, please.", "I'm just standing here!"},
        {"Have you seen anything suspicious?", "Just the usual, officer."},
    };

    // POLICE ↔ YOUTH_GANG
    private static final String[][] POLICE_YOUTH_EXCHANGES = {
        {"Oi, you lot. Move on.", "We're not doing nothing."},
        {"Don't give me that.", "Swear on me nan."},
        {"I've got my eye on you.", "Seen you looking."},
    };

    // YOUTH_GANG ↔ YOUTH_GANG
    private static final String[][] YOUTH_YOUTH_EXCHANGES = {
        {"Bruv, did you see that?", "Bare mad, fam."},
        {"This place is dead.", "Let's go town."},
        {"Got any snacks?", "Nah, skint."},
        {"You see the match?", "Don't even talk to me about it."},
    };

    // POSTMAN ↔ PUBLIC
    private static final String[][] POSTMAN_PUBLIC_EXCHANGES = {
        {"Parcel for you if you're number 14.", "Oh thank God, been waiting ages."},
        {"Could you take this for next door?", "Yeah, go on then."},
        {"Third round today.", "Rather you than me in this rain."},
    };

    // SHOPKEEPER ↔ PUBLIC
    private static final String[][] SHOPKEEPER_PUBLIC_EXCHANGES = {
        {"We've got a deal on crisps today.", "Go on then, I'll have two."},
        {"Self-service is down again, sorry.", "Not to worry, I prefer the checkout anyway."},
        {"Closing in five minutes!", "I just need milk."},
    };

    // DRUNK ↔ PUBLIC
    private static final String[][] DRUNK_PUBLIC_EXCHANGES = {
        {"Have I told you you're my best mate?", "We've never met."},
        {"What day is it?", "...Tuesday."},
        {"I love this town, I do.", "That makes one of us."},
    };

    // BUSKER ↔ PUBLIC
    private static final String[][] BUSKER_PUBLIC_EXCHANGES = {
        {"Any requests?", "Do you know Wonderwall?"},
        {"Spare change for a struggling artist?", "Here's 20p."},
        {"I used to play Glastonbury, you know.", "Really.", /* flat, unconvinced */},
    };

    // SCHOOL_KID ↔ SCHOOL_KID
    private static final String[][] SCHOOL_KID_SCHOOL_KID_EXCHANGES = {
        {"Did you do the homework?", "What homework?"},
        {"I'm so bored.", "Same. Wanna go the park?"},
        {"My mum said I can't go out.", "Text her from the park."},
        {"Have you got any food?", "I've got half a Kit Kat."},
    };

    // JOGGER ↔ PUBLIC
    private static final String[][] JOGGER_PUBLIC_EXCHANGES = {
        {"On your left!", "Blimey, gave me a fright!"},
        {"Beautiful morning for it!", "If you say so."},
    };

    // DELIVERY_DRIVER ↔ PUBLIC
    private static final String[][] DELIVERY_DRIVER_PUBLIC_EXCHANGES = {
        {"Is this Maple Close?", "No, this is Maple Road."},
        {"Could you sign for this?", "It's not even addressed to me."},
    };

    // Issue #730: NPC name pools — British first names (male + female) and surnames
    private static final String[] NPC_FIRST_NAMES = {
        "Gary", "Sharon", "Dave", "Tracey", "Kevin", "Mandy", "Wayne", "Karen",
        "Lee", "Donna", "Craig", "Debbie", "Scott", "Michelle", "Dean", "Lisa",
        "Mark", "Sandra", "Paul", "Julie", "Steve", "Carol", "Andy", "Wendy",
        "Darren", "Cheryl", "Jason", "Leanne", "Barry", "Dawn", "Terry", "Nicola",
        "Tony", "Claire", "Phil", "Beverley", "Ray", "Stacey", "Colin", "Lorraine",
        "Keith", "Janet", "Derek", "Shirley", "Clive", "Pauline", "Reg", "Pat",
        "Norm", "Brenda", "Alf", "Hilda", "Bert", "Ethel", "Stan", "Doris",
        "Liam", "Jade", "Tyler", "Chloe", "Jordan", "Kayleigh", "Kyle", "Chantelle",
        "Connor", "Britney", "Ryan", "Destiny", "Brandon", "Amber", "Callum", "Tiffany"
    };
    private static final String[] NPC_SURNAMES = {
        "Smith", "Jones", "Williams", "Brown", "Taylor", "Davies", "Evans", "Wilson",
        "Thomas", "Roberts", "Johnson", "Lewis", "Walker", "Robinson", "Wood", "Thompson",
        "White", "Watson", "Jackson", "Wright", "Green", "Harris", "Cooper", "King",
        "Lee", "Martin", "Clarke", "James", "Morgan", "Hughes", "Edwards", "Hill",
        "Moore", "Clark", "Harrison", "Ward", "Turner", "Collins", "Parker", "Bennett",
        "Shaw", "Cook", "Price", "Barnes", "Campbell", "Phillips", "Mitchell", "Kelly",
        "Fletcher", "Booth", "Pearson", "Barker", "Murray", "Sutton", "Atkinson", "Perkins"
    };
    // Track which full names have already been used to ensure each NPC is unique
    private final Set<String> usedNpcNames = new HashSet<>();

    // Structure detection
    private Map<String, Integer> playerStructures; // "x,y,z" key -> block count

    // Police system
    private boolean policeSpawned; // Track if police are currently spawned
    private Map<NPC, Float> policeWarningTimers; // Track warning duration for each police
    private Map<NPC, Vector3> policeTargetStructures; // Track which structure each police is investigating

    // Council builder system (Phase 7)
    private StructureTracker structureTracker;
    private Map<String, Integer> structureBuilderCount; // Track builders per structure (keyed by "x,y,z" of centre)
    private Map<NPC, StructureTracker.Structure> builderTargets; // Track which structure each builder is targeting
    private Map<NPC, Float> builderKnockbackTimers; // Track knockback delay per builder
    private Map<NPC, Float> builderDemolishTimers; // Track demolition cooldown per builder
    private float structureScanTimer; // Periodic structure scanning
    private float npcStructureScanTimer; // Throttle per-NPC structure checks
    private Set<String> notifiedStructures; // Track which structure positions already have notices ("x,y,z")

    // NPC idle timers — pause between wanders
    private Map<NPC, Float> npcIdleTimers;

    // Per-NPC steal cooldown — prevents the same NPC from stealing too frequently
    private Map<NPC, Float> npcStealCooldownTimers;
    private static final float STEAL_COOLDOWN = 60.0f; // seconds between steals per NPC

    // Path recalculation timers — avoid calling pathfinding every frame
    private Map<NPC, Float> npcPathRecalcTimers;

    // Police spawn cooldown — prevent mass spawning every frame
    private float policeSpawnCooldown = 0f;
    private static final float POLICE_SPAWN_INTERVAL = 10.0f; // seconds between spawn checks

    // Dawn despawn tracking — detect night→day transition
    private boolean wasNight = false;

    // Per-NPC structure scan stagger — spread checks over time
    private Map<NPC, Float> npcStructureCheckTimers;

    // Issue #910: Per-NPC timer tracking how long they have been reacting to a structure.
    // NPCs return to WANDERING after STRUCTURE_REACTION_DURATION seconds.
    private Map<NPC, Float> npcStructureReactionTimers;
    private static final float STRUCTURE_REACTION_DURATION = 10.0f; // seconds before NPC moves on

    // BlockBreaker reference — used to clear stale hit counters when demolishing blocks
    private BlockBreaker blockBreaker;

    // Arrest system — set when police catches player so game loop can apply penalties
    private boolean arrestPending = false;

    // Issue #218: Post-arrest cooldown — prevents police from re-arresting the player immediately
    // after they have been teleported back to the park. Gives the player a short grace period.
    private float postArrestCooldown = 0f;
    private static final float POST_ARREST_COOLDOWN_DURATION = 10.0f; // seconds of immunity after arrest

    // Issue #407: KNOCKED_OUT recovery duration — how long an NPC lies on the ground before
    // getting back up. Must be ticked in both the normal update loop and the PAUSED branch.
    static final float KNOCKED_OUT_RECOVERY_DURATION = 10.0f; // seconds before NPC recovers

    // Alerted police — NPCs explicitly flagged by crime events (Greggs raid, block-break near landmark).
    // Only alerted police (or police near a KNOWN/NOTORIOUS player) actively pursue the player.
    private Set<NPC> alertedPoliceNPCs;

    // Line-of-sight chase: track how long each aggressive police officer has lost sight of the player.
    // When this exceeds POLICE_LOST_SIGHT_TIMEOUT the officer gives up and reverts to PATROLLING.
    private Map<NPC, Float> policeLostSightTimers;
    private static final float POLICE_LOST_SIGHT_TIMEOUT = 3.0f; // seconds before giving up chase

    // NPC-to-NPC conversation cooldown — prevents the same NPC from initiating a new
    // exchange immediately after one ends. Counted down each frame; exchange fires when 0.
    private Map<NPC, Float> npcConversationCooldowns;
    private static final float NPC_CONVERSATION_COOLDOWN = 30.0f; // seconds between conversations per NPC
    // Distance within which two NPCs can have a conversation
    private static final float NPC_CONVERSATION_RANGE = 4.0f;

    // Fix #687: Store player inventory for HIGH_VIS_JACKET police escalation delay
    private Inventory currentInventory;

    // Issue #689: Stealth system — vision cone + hearing detection
    // Vision cone: 70° half-angle, 20 blocks range (blocked by solid walls via DDA)
    // Hearing: 360°, range = 5 + noiseLevel*15, night +25%, vision range night halved
    private static final float VISION_CONE_HALF_ANGLE_DEG = 70f;
    private static final float VISION_RANGE = 20f;
    private static final float HEARING_RANGE_BASE = 5f;
    private static final float HEARING_RANGE_NOISE_SCALE = 15f;
    private static final float NIGHT_VISION_MULTIPLIER = 0.5f;
    private static final float NIGHT_HEARING_MULTIPLIER = 1.25f;

    // SUSPICIOUS state timers per NPC (counts up; NPC reverts to PATROL after 5s of no detection)
    private Map<NPC, Float> policeSuspiciousTimers;
    private static final float SUSPICIOUS_TIMEOUT = 5.0f;

    // Game time stored for night detection calculations
    private float currentGameTime = 8.0f; // hours, 0-24

    public NPCManager() {
        this.npcs = new ArrayList<>();
        this.pathfinder = new Pathfinder();
        this.random = new Random();
        this.gameTime = 8.0f; // Start at 8:00 AM
        this.playerStructures = new HashMap<>();
        this.policeSpawned = false;
        this.policeWarningTimers = new HashMap<>();
        this.policeTargetStructures = new HashMap<>();

        // Phase 7: Council builder system
        this.structureTracker = new StructureTracker();
        this.structureBuilderCount = new HashMap<>();
        this.builderTargets = new HashMap<>();
        this.builderKnockbackTimers = new HashMap<>();
        this.builderDemolishTimers = new HashMap<>();
        this.structureScanTimer = 0;
        this.npcStructureScanTimer = 0;
        this.notifiedStructures = new HashSet<>();
        this.npcIdleTimers = new HashMap<>();
        this.npcPathRecalcTimers = new HashMap<>();
        this.npcStructureCheckTimers = new HashMap<>();
        this.npcStructureReactionTimers = new HashMap<>();
        this.alertedPoliceNPCs = new HashSet<>();
        this.npcStealCooldownTimers = new HashMap<>();
        this.policeLostSightTimers = new HashMap<>();
        this.npcConversationCooldowns = new HashMap<>();
        this.policeSuspiciousTimers = new HashMap<>();
    }

    /**
     * Spawn an NPC at a specific position.
     */
    public NPC spawnNPC(NPCType type, float x, float y, float z) {
        // Enforce maximum NPC cap
        if (npcs.size() >= MAX_NPCS) {
            return null;
        }

        NPC npc = new NPC(type, x, y, z);

        // Set initial state based on type
        switch (type) {
            case DOG:
            case YOUTH_GANG:
            case JOGGER:
            case POSTMAN:
            case DRUNK:
            case DELIVERY_DRIVER:
                npc.setState(NPCState.WANDERING);
                break;
            case BIRD:
                npc.setState(NPCState.IDLE); // Start perched; will take flight when player approaches
                break;
            case SCHOOL_KID:
                npc.setState(NPCState.WANDERING);
                updateSchoolKidRoutine(npc); // Set based on current time
                break;
            case PUBLIC:
            case COUNCIL_MEMBER:
            case PENSIONER:
                npc.setState(NPCState.IDLE);
                updateDailyRoutine(npc); // Set based on time
                break;
            case SHOPKEEPER:
            case BUSKER:
                npc.setState(NPCState.WANDERING); // Wander in small area near spawn
                break;
            default:
                npc.setState(NPCState.IDLE);
        }

        // Assign a model variant to give visual variety to NPCs (Issue #729).
        npc.setModelVariant(pickVariant(type));

        // Assign hairstyle and facial hair for visual variety (Issue #875).
        npc.setHairstyle(pickHairstyle(type));
        npc.setFacialHair(pickFacialHair(type));

        // Assign a unique name to humanoid NPCs for immersion (Issue #730).
        // Non-humanoid types (DOG, BIRD) and role-anonymous types that are always
        // addressed by role (POLICE, PCSO, ARMED_RESPONSE, COUNCIL_BUILDER) do not
        // get personal names.  All other human NPCs receive a randomly chosen
        // British full name drawn from the name pool.
        switch (type) {
            case DOG:
            case BIRD:
            case POLICE:
            case PCSO:
            case ARMED_RESPONSE:
            case COUNCIL_BUILDER:
                break; // no personal name
            default:
                npc.setName(pickName());
        }

        npcs.add(npc);
        return npc;
    }

    /**
     * Pick an appropriate {@link NPCModelVariant} for the given NPC type.
     * Non-humanoid types (DOG, BIRD) always use DEFAULT.
     * Uniformed types (POLICE, ARMED_RESPONSE, COUNCIL_BUILDER, PCSO, LOLLIPOP_LADY)
     * use DEFAULT so their distinctive outfit reads clearly.
     * All other types get a weighted random pick suited to their archetype.
     */
    private NPCModelVariant pickVariant(NPCType type) {
        switch (type) {
            // Non-humanoid or uniform — fixed appearance
            case DOG:
            case BIRD:
            case POLICE:
            case ARMED_RESPONSE:
            case COUNCIL_BUILDER:
            case PCSO:
            case LOLLIPOP_LADY:
                return NPCModelVariant.DEFAULT;

            // Imposing/heavy build
            case BOUNCER:
            case THUG:
                return randomFrom(NPCModelVariant.STOCKY, NPCModelVariant.STOCKY, NPCModelVariant.DEFAULT);

            // Athletic / slender
            case JOGGER:
                return randomFrom(NPCModelVariant.SLIM, NPCModelVariant.SLIM, NPCModelVariant.TALL);

            // Elderly — shorter and stockier
            case PENSIONER:
                return randomFrom(NPCModelVariant.SHORT, NPCModelVariant.STOCKY, NPCModelVariant.SHORT);

            // Children — shorter
            case SCHOOL_KID:
                return randomFrom(NPCModelVariant.SHORT, NPCModelVariant.SHORT, NPCModelVariant.SLIM);

            // Street youth — tall or slim look
            case YOUTH_GANG:
            case STREET_LAD:
                return randomFrom(NPCModelVariant.TALL, NPCModelVariant.SLIM, NPCModelVariant.DEFAULT);

            // All remaining humanoid types get a fully random variant (including LONG_HAIR)
            default: {
                NPCModelVariant[] all = NPCModelVariant.values();
                return all[random.nextInt(all.length)];
            }
        }
    }

    /**
     * Pick one of the supplied variants at random with equal probability.
     */
    private NPCModelVariant randomFrom(NPCModelVariant... variants) {
        return variants[random.nextInt(variants.length)];
    }

    /**
     * Pick an appropriate {@link HairstyleType} for the given NPC type (Issue #875).
     * Non-humanoid types (DOG, BIRD) and helmeted types use NONE (hair hidden/irrelevant).
     * School kids tend to have neat hair. Punks and street youth may have mohawks.
     * Other types get a weighted random selection.
     */
    private HairstyleType pickHairstyle(NPCType type) {
        switch (type) {
            // Non-humanoid or helmeted — no hair visible
            case DOG:
            case BIRD:
            case POLICE:
            case ARMED_RESPONSE:
            case PCSO:
            case LOLLIPOP_LADY:
                return HairstyleType.NONE;

            // School kids — neat SHORT hair predominantly
            case SCHOOL_KID:
                return randomHairstyleFrom(HairstyleType.SHORT, HairstyleType.SHORT, HairstyleType.BUZZCUT);

            // Street youth / gang — variety including mohawk
            case YOUTH_GANG:
            case STREET_LAD:
                return randomHairstyleFrom(HairstyleType.MOHAWK, HairstyleType.SHORT, HairstyleType.LONG, HairstyleType.BUZZCUT);

            // Buskers / bohemian types — often long hair
            case BUSKER:
                return randomHairstyleFrom(HairstyleType.LONG, HairstyleType.LONG, HairstyleType.CURLY, HairstyleType.SHORT);

            // Joggers — short or buzzcut
            case JOGGER:
                return randomHairstyleFrom(HairstyleType.SHORT, HairstyleType.BUZZCUT, HairstyleType.BUZZCUT);

            // Elderly — thinning, short, or none
            case PENSIONER:
                return randomHairstyleFrom(HairstyleType.SHORT, HairstyleType.NONE, HairstyleType.NONE, HairstyleType.BUZZCUT);

            // All other humanoid types — fully random
            default: {
                HairstyleType[] all = HairstyleType.values();
                return all[random.nextInt(all.length)];
            }
        }
    }

    /**
     * Pick one of the supplied hairstyle values at random with equal probability.
     */
    private HairstyleType randomHairstyleFrom(HairstyleType... styles) {
        return styles[random.nextInt(styles.length)];
    }

    /**
     * Pick an appropriate {@link FacialHairType} for the given NPC type (Issue #875).
     * Non-humanoid types get NONE. Most types get a weighted random selection.
     */
    private FacialHairType pickFacialHair(NPCType type) {
        switch (type) {
            // Non-humanoid or uniformed female roles — no facial hair
            case DOG:
            case BIRD:
            case LOLLIPOP_LADY:
                return FacialHairType.NONE;

            // School kids — no facial hair (children)
            case SCHOOL_KID:
                return FacialHairType.NONE;

            // Buskers and drunks often have beards
            case BUSKER:
            case DRUNK:
                return randomFacialHairFrom(FacialHairType.BEARD, FacialHairType.STUBBLE, FacialHairType.MOUSTACHE, FacialHairType.NONE);

            // Police — clean-shaven or light stubble
            case POLICE:
            case ARMED_RESPONSE:
            case PCSO:
                return randomFacialHairFrom(FacialHairType.NONE, FacialHairType.NONE, FacialHairType.STUBBLE);

            // Pensioners — often moustaches or goatees
            case PENSIONER:
                return randomFacialHairFrom(FacialHairType.NONE, FacialHairType.MOUSTACHE, FacialHairType.GOATEE, FacialHairType.STUBBLE);

            // All other humanoid types — weighted toward none/light
            default:
                return randomFacialHairFrom(FacialHairType.NONE, FacialHairType.NONE, FacialHairType.STUBBLE, FacialHairType.BEARD, FacialHairType.GOATEE);
        }
    }

    /**
     * Pick one of the supplied facial hair values at random with equal probability.
     */
    private FacialHairType randomFacialHairFrom(FacialHairType... styles) {
        return styles[random.nextInt(styles.length)];
    }

    /**
     * Generate a unique British name for an NPC (Issue #730).
     * Combines a random first name with a random surname.
     * Retries up to 20 times to avoid duplicates; falls back to an
     * indexed name if the pool is exhausted.
     *
     * @return a unique full name string such as "Gary Smith"
     */
    String pickName() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String first = NPC_FIRST_NAMES[random.nextInt(NPC_FIRST_NAMES.length)];
            String last  = NPC_SURNAMES[random.nextInt(NPC_SURNAMES.length)];
            String full  = first + " " + last;
            if (usedNpcNames.add(full)) {
                return full;
            }
        }
        // Fallback: append a number to guarantee uniqueness
        String base = NPC_FIRST_NAMES[random.nextInt(NPC_FIRST_NAMES.length)]
                    + " " + NPC_SURNAMES[random.nextInt(NPC_SURNAMES.length)];
        String fallback = base + " " + (usedNpcNames.size() + 1);
        usedNpcNames.add(fallback);
        return fallback;
    }

    /**
     * Spawn a static building NPC — a SHOPKEEPER stationed inside a labelled building.
     * The NPC stays near its post (IDLE state, minimal wander) and offers a quest
     * when the player interacts with it (Issue #440).
     *
     * @param buildingType The landmark type this NPC represents
     * @param x            Interior X position (inside the building)
     * @param y            Y position (ground level)
     * @param z            Interior Z position (inside the building)
     * @return The spawned NPC, or null if the cap is reached
     */
    public NPC spawnBuildingNPC(ragamuffin.world.LandmarkType buildingType, float x, float y, float z) {
        NPC npc = spawnNPC(NPCType.SHOPKEEPER, x, y, z);
        if (npc != null) {
            npc.setBuildingType(buildingType);
            npc.setState(NPCState.IDLE); // Stand still at their post
        }
        return npc;
    }

    /**
     * Spawn a named NPC with a unique name and distinctive appearance.
     * Named NPCs are unique characters with their own identity in the game world.
     *
     * @param type The NPC type defining appearance and behaviour
     * @param name The unique name for this NPC (displayed above their head)
     * @param x    X position in the world
     * @param y    Y position in the world
     * @param z    Z position in the world
     * @return The spawned NPC, or null if the cap is reached
     */
    public NPC spawnNamedNPC(NPCType type, String name, float x, float y, float z) {
        NPC npc = spawnNPC(type, x, y, z);
        if (npc != null) {
            npc.setName(name);
        }
        return npc;
    }

    /**
     * Get all NPCs.
     */
    public List<NPC> getNPCs() {
        return npcs;
    }

    /**
     * Remove an NPC and clean up all per-NPC map entries to prevent memory leaks.
     */
    public void removeNPC(NPC npc) {
        cleanupNPC(npc);
        npcs.remove(npc);
    }

    /**
     * Remove all per-NPC state from every supporting map/set.
     * Must be called before removing an NPC from the main {@code npcs} list.
     */
    private void cleanupNPC(NPC npc) {
        policeWarningTimers.remove(npc);
        policeTargetStructures.remove(npc);
        builderTargets.remove(npc);
        builderKnockbackTimers.remove(npc);
        builderDemolishTimers.remove(npc);
        npcIdleTimers.remove(npc);
        npcStealCooldownTimers.remove(npc);
        npcPathRecalcTimers.remove(npc);
        npcStructureCheckTimers.remove(npc);
        npcStructureReactionTimers.remove(npc);
        alertedPoliceNPCs.remove(npc);
        policeLostSightTimers.remove(npc);
        npcConversationCooldowns.remove(npc);
        policeSuspiciousTimers.remove(npc);
    }

    /**
     * Set the current game time in hours (0-24).
     * Daily routines are only applied when the time-of-day band changes (night →
     * work-hours → evening) so that per-frame calls do not thrash NPC reaction
     * states such as FLEEING or AGGRESSIVE.
     */
    public void setGameTime(float hours) {
        this.gameTime = hours % 24;
        this.currentGameTime = this.gameTime;

        int currentBand = getTimeBand(this.gameTime);
        if (currentBand != previousTimeBand) {
            previousTimeBand = currentBand;
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.COUNCIL_MEMBER) {
                    updateDailyRoutine(npc);
                }
            }
        }

        int currentSchoolBand = getSchoolTimeBand(this.gameTime);
        if (currentSchoolBand != previousSchoolBand) {
            previousSchoolBand = currentSchoolBand;
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.SCHOOL_KID) {
                    updateSchoolKidRoutine(npc);
                }
            }
        }
    }

    /**
     * Returns the time-of-day band for the given hour:
     *   1 = work hours (08:00-17:00)
     *   2 = evening   (17:00-20:00)
     *   0 = night     (all other times)
     */
    private int getTimeBand(float h) {
        if (h >= 8 && h < 17) return 1;
        if (h >= 17 && h < 20) return 2;
        return 0;
    }

    /**
     * Returns the school time band for the given hour:
     *   1 = going to school (08:00-09:00) — morning commute
     *   2 = at school       (09:00-15:30) — school hours
     *   3 = leaving school  (15:30-17:00) — end of school day
     *   0 = out of school   (all other times)
     */
    private int getSchoolTimeBand(float h) {
        if (h >= 8 && h < 9) return 1;
        if (h >= 9 && h < 15.5f) return 2;
        if (h >= 15.5f && h < 17) return 3;
        return 0;
    }

    public float getGameTime() {
        return gameTime;
    }

    /**
     * Returns true when the NPC is in an active-reaction state that must not be
     * overwritten by the daily routine (e.g. being punched, fleeing, under arrest).
     */
    private boolean isActiveReactionState(NPCState state) {
        switch (state) {
            case FLEEING:
            case AGGRESSIVE:
            case ARRESTING:
            case WARNING:
            case SUSPICIOUS:
            case KNOCKED_OUT:
            case KNOCKED_BACK:
            case STEALING:
            case DEMOLISHING:
            case PATROLLING:
            case STARING:
            case PHOTOGRAPHING:
            case COMPLAINING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Update daily routine based on time.
     * Only called when the time-of-day band transitions; never called every frame.
     * Active-reaction states are preserved so AI behaviour is not overwritten.
     */
    private void updateDailyRoutine(NPC npc) {
        if (isActiveReactionState(npc.getState())) {
            return;
        }
        if (gameTime >= 8 && gameTime < 17) {
            // Work hours (8:00 - 17:00)
            if (npc.getState() != NPCState.GOING_TO_WORK) {
                npc.setState(NPCState.GOING_TO_WORK);
            }
        } else if (gameTime >= 17 && gameTime < 20) {
            // Evening (17:00 - 20:00)
            if (npc.getState() != NPCState.GOING_HOME) {
                npc.setState(NPCState.GOING_HOME);
            }
        } else {
            // Night (20:00+) or early morning
            if (npc.getState() != NPCState.AT_PUB && npc.getState() != NPCState.AT_HOME) {
                // Randomly choose pub or home
                npc.setState(random.nextBoolean() ? NPCState.AT_PUB : NPCState.AT_HOME);
            }
        }
    }

    /**
     * Update school kid daily routine based on time band.
     * Only called when the school time band transitions.
     * Active-reaction states are preserved.
     *
     * Schedule:
     *   08:00–09:00 — GOING_TO_SCHOOL (morning commute to school)
     *   09:00–15:30 — AT_SCHOOL (attending school; minimal wandering)
     *   15:30–17:00 — LEAVING_SCHOOL (heading home after school)
     *   other times — WANDERING (evenings/nights: free time, hanging about)
     */
    private void updateSchoolKidRoutine(NPC npc) {
        if (isActiveReactionState(npc.getState())) {
            return;
        }
        if (gameTime >= 8 && gameTime < 9) {
            if (npc.getState() != NPCState.GOING_TO_SCHOOL) {
                npc.setState(NPCState.GOING_TO_SCHOOL);
            }
        } else if (gameTime >= 9 && gameTime < 15.5f) {
            if (npc.getState() != NPCState.AT_SCHOOL) {
                npc.setState(NPCState.AT_SCHOOL);
            }
        } else if (gameTime >= 15.5f && gameTime < 17) {
            if (npc.getState() != NPCState.LEAVING_SCHOOL) {
                npc.setState(NPCState.LEAVING_SCHOOL);
            }
        } else {
            // Evening/night — school kids wander freely
            if (npc.getState() == NPCState.GOING_TO_SCHOOL
                    || npc.getState() == NPCState.AT_SCHOOL
                    || npc.getState() == NPCState.LEAVING_SCHOOL) {
                npc.setState(NPCState.WANDERING);
            }
        }
    }

    /**
     * Update all NPCs.
     */
    public void update(float delta, World world, Player player, Inventory inventory, TooltipSystem tooltipSystem) {
        // Fix #687: cache inventory for HIGH_VIS_JACKET police escalation delay
        this.currentInventory = inventory;

        // Tick police spawn cooldown
        if (policeSpawnCooldown > 0) {
            policeSpawnCooldown -= delta;
        }

        // Issue #218: Tick post-arrest cooldown — prevents immediate re-arrest after teleport
        if (postArrestCooldown > 0) {
            postArrestCooldown = Math.max(0f, postArrestCooldown - delta);
        }

        // Update structure tracking (Phase 7)
        // Scanned at a long interval — the 200x200x19 block scan is expensive
        structureScanTimer += delta;
        if (structureScanTimer >= 30.0f) { // Scan every 30 seconds, not 2
            structureTracker.scanForStructures(world);
            updateCouncilBuilders(world, tooltipSystem);
            structureScanTimer = 0;
        }

        // Throttle per-NPC structure checks (expensive block scanning)
        npcStructureScanTimer += delta;
        if (npcStructureScanTimer >= 30.0f) { // Match the structure scan interval
            npcStructureScanTimer = 0;
        }

        // Remove dead NPCs (speech timer expired) and clean up associated state
        npcs.removeIf(npc -> {
            if (!npc.isAlive() && !npc.isSpeaking()) {
                cleanupNPC(npc);
                return true;
            }
            return false;
        });

        // Use indexed loop to avoid ConcurrentModificationException when spawning new NPCs
        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (!npc.isAlive()) {
                // Still tick timers for dead NPCs so their speech timer counts down
                // and they can be removed once isSpeaking() returns false.
                npc.updateTimers(delta);
                // Issue #407: Advance KNOCKED_OUT recovery — NPC gets up after the recovery duration.
                npc.tickKnockedOutTimer(delta);
                if (npc.getState() == NPCState.KNOCKED_OUT
                        && npc.getKnockedOutTimer() >= KNOCKED_OUT_RECOVERY_DURATION) {
                    npc.revive();
                }
                continue;
            }
            updateNPC(npc, delta, world, player, inventory, tooltipSystem);

            // NPC attacks player if in range and hostile/aggressive
            if (npc.canAttack() && npc.getType().getAttackDamage() > 0 && !player.isDead()) {
                boolean shouldAttack = false;
                float attackRange = 1.8f;

                if (npc.getType() == NPCType.POLICE) {
                    if ((npc.getState() == NPCState.AGGRESSIVE || npc.getState() == NPCState.ARRESTING)
                            && npc.isNear(player.getPosition(), attackRange)) {
                        shouldAttack = true;
                    }
                } else if (npc.getType().isHostile() && npc.isNear(player.getPosition(), attackRange)) {
                    shouldAttack = true;
                } else if (npc.getState() == NPCState.AGGRESSIVE && npc.isNear(player.getPosition(), attackRange)) {
                    shouldAttack = true;
                }

                // Player is invincible while dodging (i-frames)
                if (shouldAttack && player.isDodging()) {
                    shouldAttack = false;
                }

                if (shouldAttack) {
                    player.damage(npc.getType().getAttackDamage(), DamageReason.NPC_ATTACK);
                    npc.resetAttackCooldown();

                    // Attack speech
                    if (!npc.isSpeaking()) {
                        switch (npc.getType()) {
                            case YOUTH_GANG:
                                npc.setSpeechText("Take that!", 1.0f);
                                break;
                            case POLICE:
                                npc.setSpeechText("Stop resisting!", 1.0f);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Update a single NPC's behavior.
     */
    private void updateNPC(NPC npc, float delta, World world, Player player, Inventory inventory, TooltipSystem tooltipSystem) {
        // Issue #708: Birds use a separate lightweight update — they fly, ignore pathfinding and gravity.
        if (npc.getType() == NPCType.BIRD) {
            updateBird(npc, delta, world, player);
            return;
        }

        // Advance path recalculation timer
        npcPathRecalcTimers.merge(npc, delta, (a, b) -> a + b);

        // Tick per-NPC structure check timer (count down toward 0 when a scan is due)
        npcStructureCheckTimers.computeIfPresent(npc, (k, v) -> Math.max(0f, v - delta));

        // Tick per-NPC steal cooldown timer
        npcStealCooldownTimers.computeIfPresent(npc, (k, v) -> Math.max(0f, v - delta));

        // Issue #910: Tick per-NPC structure reaction timer (counts up while NPC is reacting)
        npcStructureReactionTimers.computeIfPresent(npc, (k, v) -> v + delta);

        // Update timers and facing (but NOT position — we handle movement with collision)
        npc.updateTimers(delta);
        npc.updateKnockback(delta);

        // Apply velocity with world collision (includes knockback velocity)
        applyNPCCollision(npc, delta, world);

        // Stuck detection — if NPC has been pushing against a wall, try multiple escape directions.
        // Council builders deliberately walk into structures to demolish them, so skip stuck
        // detection for them — they are not lost, just blocked by the structure they're working on.
        if (npc.getType() != NPCType.COUNCIL_BUILDER && npc.updateStuckDetection(delta)) {
            npc.resetStuckTimer();
            npc.setPath(null);
            npc.setTargetPosition(null);
            npc.setVelocity(0, 0, 0);

            // Try multiple random directions to escape (not just 180°)
            boolean escaped = false;
            for (int attempt = 0; attempt < 6; attempt++) {
                float escapeAngle = random.nextFloat() * (float) Math.PI * 2;
                float escapeDist = 3.0f + random.nextFloat() * 5.0f;
                float newX = npc.getPosition().x + (float) Math.sin(escapeAngle) * escapeDist;
                float newZ = npc.getPosition().z + (float) Math.cos(escapeAngle) * escapeDist;
                float newY = findGroundHeight(world, newX, newZ);
                setNPCTarget(npc, new Vector3(newX, newY, newZ), world);
                if (npc.getPath() != null && !npc.getPath().isEmpty()) {
                    escaped = true;
                    break;
                }
            }
            if (!escaped) {
                // All pathfinding failed — just give a velocity push away from the wall
                float pushAngle = (float) Math.toRadians(npc.getFacingAngle() + 180);
                npc.setVelocity((float) Math.sin(pushAngle) * 2.0f, 0, (float) Math.cos(pushAngle) * 2.0f);
            }
        }

        // Skip AI movement while being knocked back — let the impulse play out
        if (npc.isKnockedBack()) {
            return;
        }

        // NPCs randomly speak when near player — only in calm states
        NPCState currentState = npc.getState();
        boolean calmState = currentState != NPCState.AGGRESSIVE && currentState != NPCState.FLEEING
                && currentState != NPCState.KNOCKED_OUT && currentState != NPCState.KNOCKED_BACK
                && currentState != NPCState.ARRESTING && currentState != NPCState.WARNING
                && currentState != NPCState.STEALING && currentState != NPCState.DEMOLISHING;
        if (calmState && !npc.isSpeaking() && npc.isNear(player.getPosition(), 10.0f)) {
            if (random.nextFloat() < 0.005f) {
                String speech = getRandomSpeech(npc.getType());
                if (speech != null) {
                    npc.setSpeechText(speech, 3.0f);
                }
            }
        }

        // Handle police separately
        if (npc.getType() == NPCType.POLICE) {
            updatePolice(npc, delta, world, player, tooltipSystem);
        } else if (npc.getType() == NPCType.COUNCIL_BUILDER) {
            updateCouncilBuilder(npc, delta, world, tooltipSystem);
        } else {
            // Notorious players cause civilians to flee on sight
            if (player.getStreetReputation().isNotorious()
                    && isCivilianType(npc.getType())
                    && npc.getState() != NPCState.FLEEING
                    && npc.isNear(player.getPosition(), 10.0f)) {
                npc.setState(NPCState.FLEEING);
                if (!npc.isSpeaking()) {
                    npc.setSpeechText(getFlightSpeech(npc.getType()), 2.5f);
                }
            }

            switch (npc.getState()) {
                case FLEEING:
                    updateFleeing(npc, delta, world, player);
                    break;
                case WANDERING:
                    updateWandering(npc, delta, world);
                    break;
                case GOING_TO_WORK:
                case GOING_HOME:
                case AT_PUB:
                case AT_HOME:
                    updateDailyRoutine(npc, delta, world);
                    break;
                case GOING_TO_SCHOOL:
                case LEAVING_SCHOOL:
                    updateWandering(npc, delta, world); // Walk to/from school
                    break;
                case AT_SCHOOL:
                    // School kids mostly stay put during school hours — minimal wander
                    updateWandering(npc, delta, world);
                    break;
                case STARING:
                case PHOTOGRAPHING:
                case COMPLAINING:
                    updateReactingToStructure(npc, delta, world);
                    break;
                case STEALING:
                    updateStealing(npc, delta, player, inventory, tooltipSystem);
                    break;
                case AGGRESSIVE:
                    updateAggressive(npc, delta, world, player);
                    break;
                default:
                    updateWandering(npc, delta, world);
                    break;
            }
        }

        // Check for player structures nearby (each NPC throttled independently)
        checkForPlayerStructures(npc, world);

        // NPC-to-NPC dialogue — idle/wandering NPCs may spark a conversation with a neighbour
        updateNPCToNPCDialogue(npc, delta);

        // Youth gangs try to steal (only if steal cooldown has expired)
        if (npc.getType() == NPCType.YOUTH_GANG && npc.getState() != NPCState.STEALING
                && npc.getState() != NPCState.AGGRESSIVE
                && npc.getState() != NPCState.FLEEING) {
            float stealCooldown = npcStealCooldownTimers.getOrDefault(npc, 0f);
            if (stealCooldown <= 0f && npc.isNear(player.getPosition(), 2.0f)) {
                npc.setState(NPCState.STEALING);
            } else if (npc.isNear(player.getPosition(), 20.0f)) {
                // Move toward player
                setNPCTarget(npc, player.getPosition(), world);
            }
        }

        // Dogs stay in park
        if (npc.getType() == NPCType.DOG) {
            enforceParKBoundaries(npc);
        }

        // Follow path if one exists
        if (npc.getPath() != null && !npc.getPath().isEmpty()) {
            followPath(npc, delta);
        } else if (npc.getTargetPosition() != null) {
            // No path, but has target - move directly toward it
            moveTowardTarget(npc, npc.getTargetPosition(), delta);
        } else {
            // No target or path - stop moving
            npc.setVelocity(0, 0, 0);
        }
    }

    /**
     * Update wandering behavior with idle pauses and varied behaviour.
     */
    private void updateWandering(NPC npc, float delta, World world) {
        // Check if NPC should pause (idle for a bit between walks)
        Float idleTimer = npcIdleTimers.get(npc);
        if (idleTimer != null && idleTimer > 0) {
            npcIdleTimers.put(npc, idleTimer - delta);
            npc.setVelocity(0, 0, 0);
            return;
        }

        // Different wander characteristics per NPC type
        float wanderRadius;
        float minWanderDistance;
        switch (npc.getType()) {
            case DOG: wanderRadius = 15.0f; minWanderDistance = 5.0f; break;
            case JOGGER: wanderRadius = 25.0f; minWanderDistance = 10.0f; break;
            case POSTMAN: wanderRadius = 30.0f; minWanderDistance = 8.0f; break;
            case SHOPKEEPER: wanderRadius = 5.0f; minWanderDistance = 0.0f; break;
            case BUSKER: wanderRadius = 3.0f; minWanderDistance = 0.0f; break;
            case DRUNK: wanderRadius = 8.0f; minWanderDistance = 0.0f; break;
            case DELIVERY_DRIVER: wanderRadius = 30.0f; minWanderDistance = 8.0f; break;
            case PENSIONER: wanderRadius = 5.0f; minWanderDistance = 0.0f; break;
            case SCHOOL_KID: wanderRadius = 15.0f; minWanderDistance = 3.0f; break;
            default: wanderRadius = 10.0f; minWanderDistance = 0.0f; break;
        }

        boolean needsNewTarget = npc.getTargetPosition() == null ||
                                 npc.getPath() == null ||
                                 npc.getPath().isEmpty() ||
                                 npc.isNear(npc.getTargetPosition(), 1.0f);

        if (needsNewTarget) {
            // If we just finished an idle pause, go straight to picking a new target
            boolean justFinishedIdle = (idleTimer != null && idleTimer <= 0);
            npcIdleTimers.remove(npc);

            if (!justFinishedIdle) {
                // Chance to pause before walking to next point
                float idlePause = 0f;
                switch (npc.getType()) {
                    case PUBLIC:
                    case COUNCIL_MEMBER:
                        idlePause = 1.0f + random.nextFloat() * 4.0f; break;
                    case DOG:
                        idlePause = 0.5f + random.nextFloat() * 2.0f; break;
                    case YOUTH_GANG:
                        idlePause = 2.0f + random.nextFloat() * 5.0f; break;
                    case JOGGER:
                        idlePause = 0.2f + random.nextFloat() * 0.5f; break; // brief pauses
                    case SHOPKEEPER:
                        idlePause = 3.0f + random.nextFloat() * 6.0f; break; // lingers near shop
                    case BUSKER:
                        idlePause = 5.0f + random.nextFloat() * 10.0f; break; // plays for ages
                    case DRUNK:
                        idlePause = 2.0f + random.nextFloat() * 8.0f; break; // slow and confused
                    case POSTMAN:
                        idlePause = 0.5f + random.nextFloat() * 1.0f; break; // brisk deliveries
                    case DELIVERY_DRIVER:
                        idlePause = 0.3f + random.nextFloat() * 0.5f; break; // always rushing
                    case PENSIONER:
                        idlePause = 4.0f + random.nextFloat() * 8.0f; break; // long pauses, staring at nothing
                    case SCHOOL_KID:
                        idlePause = 0.5f + random.nextFloat() * 2.0f; break; // hyperactive bursts
                    default:
                        idlePause = 1.0f + random.nextFloat() * 3.0f; break;
                }

                if (idlePause > 0) {
                    npcIdleTimers.put(npc, idlePause);
                    npc.setVelocity(0, 0, 0);
                    return;
                }
            }

            Vector3 randomTarget;
            int attempts = 0;
            do {
                randomTarget = getRandomWalkablePosition(npc.getPosition(), world, wanderRadius);
                attempts++;
            } while (minWanderDistance > 0 && randomTarget.dst(npc.getPosition()) < minWanderDistance && attempts < 10);
            setNPCTarget(npc, randomTarget, world);
        }
    }

    /**
     * Update fleeing behavior — civilian runs away from notorious player.
     * Once far enough away, returns to wandering.
     */
    private void updateFleeing(NPC npc, float delta, World world, Player player) {
        float distToPlayer = npc.getPosition().dst(player.getPosition());

        if (distToPlayer > 20.0f) {
            // Far enough — stop fleeing
            npc.setState(NPCState.WANDERING);
            npc.setPath(null);
            npc.setTargetPosition(null);
            return;
        }

        // Move directly away from player at increased speed
        Vector3 awayDir = npc.getPosition().cpy().sub(player.getPosition()).nor();
        float fleeSpeed = getNPCSpeed(npc.getType()) * 2.0f;
        float fleeX = npc.getPosition().x + awayDir.x * 10f;
        float fleeZ = npc.getPosition().z + awayDir.z * 10f;
        float fleeY = findGroundHeight(world, fleeX, fleeZ);
        Vector3 fleeTarget = new Vector3(fleeX, fleeY, fleeZ);

        // Only recalculate path if we don't have one or are close to the current target
        if (npc.getTargetPosition() == null || npc.isNear(npc.getTargetPosition(), 2.0f)) {
            setNPCTarget(npc, fleeTarget, world);
        }

        // Override speed — flee faster than normal. Preserve vertical velocity for gravity.
        float curVelY = npc.getVelocity().y;
        if (npc.getPath() != null && !npc.getPath().isEmpty()) {
            List<Vector3> path = npc.getPath();
            int idx = npc.getCurrentPathIndex();
            if (idx < path.size()) {
                Vector3 wp = path.get(idx);
                Vector3 dir = wp.cpy().sub(npc.getPosition()).nor();
                npc.setVelocity(dir.x * fleeSpeed, curVelY, dir.z * fleeSpeed);
            }
        } else {
            npc.setVelocity(awayDir.x * fleeSpeed, curVelY, awayDir.z * fleeSpeed);
        }
    }

    // Issue #708: Bird flight height above ground
    private static final float BIRD_FLY_HEIGHT = 5.0f;
    // Issue #708: Scatter distance from player before birds take flight
    private static final float BIRD_SCATTER_RANGE = 4.0f;
    // Issue #708: Wander radius for birds
    private static final float BIRD_WANDER_RADIUS = 30.0f;

    /**
     * Update a bird NPC — lightweight flying update that bypasses pathfinding and gravity.
     * Birds perch at ground level (IDLE) and scatter into flight (WANDERING) when the player
     * approaches within {@link #BIRD_SCATTER_RANGE} blocks.
     */
    private void updateBird(NPC npc, float delta, World world, Player player) {
        npc.updateTimers(delta);

        float groundY = findGroundHeight(world, npc.getPosition().x, npc.getPosition().z);
        float distToPlayer = npc.getPosition().dst(player.getPosition());

        if (npc.getState() == NPCState.IDLE) {
            // Perching on the ground — snap to ground level
            npc.getPosition().y = groundY;
            npc.setVelocity(0, 0, 0);

            // Scatter when player gets too close
            if (distToPlayer < BIRD_SCATTER_RANGE) {
                npc.setState(NPCState.WANDERING);
                // Pick a random flee direction away from the player
                float angle = (float) (Math.atan2(
                    npc.getPosition().x - player.getPosition().x,
                    npc.getPosition().z - player.getPosition().z
                ) + (random.nextFloat() - 0.5f) * (float) Math.PI);
                float dist = BIRD_WANDER_RADIUS * (0.5f + random.nextFloat() * 0.5f);
                float tx = npc.getPosition().x + (float) Math.sin(angle) * dist;
                float tz = npc.getPosition().z + (float) Math.cos(angle) * dist;
                float ty = findGroundHeight(world, tx, tz) + BIRD_FLY_HEIGHT;
                npc.setTargetPosition(new Vector3(tx, ty, tz));
            }

            // Occasional random idle movement — birds shuffle about on the ground
            Float idleTimer = npcIdleTimers.get(npc);
            if (idleTimer == null || idleTimer <= 0) {
                npcIdleTimers.remove(npc);
                if (random.nextFloat() < 0.002f) {
                    // Shuffle a short distance
                    float angle = random.nextFloat() * (float) Math.PI * 2;
                    float dist = 2.0f + random.nextFloat() * 3.0f;
                    float tx = npc.getPosition().x + (float) Math.sin(angle) * dist;
                    float tz = npc.getPosition().z + (float) Math.cos(angle) * dist;
                    npc.setTargetPosition(new Vector3(tx, groundY, tz));
                }
            } else {
                npcIdleTimers.put(npc, idleTimer - delta);
            }

        } else {
            // Flying — move directly toward target at flying height
            float flyY = groundY + BIRD_FLY_HEIGHT;
            if (npc.getTargetPosition() == null) {
                // Pick a new random target to fly toward
                float angle = random.nextFloat() * (float) Math.PI * 2;
                float dist = BIRD_WANDER_RADIUS * (0.4f + random.nextFloat() * 0.6f);
                float tx = npc.getPosition().x + (float) Math.sin(angle) * dist;
                float tz = npc.getPosition().z + (float) Math.cos(angle) * dist;
                npc.setTargetPosition(new Vector3(tx, flyY, tz));
            }

            Vector3 target = npc.getTargetPosition();
            Vector3 pos = npc.getPosition();
            float dx = target.x - pos.x;
            float dy = target.y - pos.y;
            float dz = target.z - pos.z;
            float dist3d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist3d < 1.5f) {
                // Arrived — land and perch for a while
                npc.setTargetPosition(null);
                npc.setState(NPCState.IDLE);
                npcIdleTimers.put(npc, 3.0f + random.nextFloat() * 8.0f);
                npc.setVelocity(0, 0, 0);
            } else {
                float birdSpeed = NPC.MOVE_SPEED * 3.5f;
                npc.setVelocity(dx / dist3d * birdSpeed, dy / dist3d * birdSpeed, dz / dist3d * birdSpeed);
                pos.x += npc.getVelocity().x * delta;
                pos.y += npc.getVelocity().y * delta;
                pos.z += npc.getVelocity().z * delta;
                // Facing angle and animTime are updated automatically by updateTimers (already called above)
            }
        }
    }

    /**
     * Update aggressive behavior — non-police NPC (e.g. YOUTH_GANG) chases the player.
     * De-escalates back to WANDERING if the player escapes beyond 40 blocks.
     */
    private void updateAggressive(NPC npc, float delta, World world, Player player) {
        setNPCTarget(npc, player.getPosition(), world);
        if (npc.getPosition().dst(player.getPosition()) > 40.0f) {
            npc.setState(NPCState.WANDERING);
        }
    }

    /**
     * Whether an NPC type is a civilian who flees from notorious players.
     */
    private boolean isCivilianType(NPCType type) {
        switch (type) {
            case PUBLIC:
            case PENSIONER:
            case SCHOOL_KID:
            case JOGGER:
            case BUSKER:
            case POSTMAN:
            case STREET_PREACHER:
            case LOLLIPOP_LADY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get panic speech when a civilian flees from the notorious player.
     */
    private String getFlightSpeech(NPCType type) {
        switch (type) {
            case PUBLIC: return "It's that notorious one — run!";
            case PENSIONER: return "Me hip! Help!";
            case SCHOOL_KID: return "Mum! MUM!";
            case JOGGER: return "Abort! Abort!";
            case BUSKER: return "I'm just a musician!";
            case POSTMAN: return "I've got letters!";
            case STREET_PREACHER: return "God will protect me!";
            case LOLLIPOP_LADY: return "Someone call the police!";
            default: return "Get away from me!";
        }
    }

    /**
     * Update daily routine movement.
     */
    private void updateDailyRoutine(NPC npc, float delta, World world) {
        // For now, NPCs just wander during their routine
        // In a full implementation, they'd path to specific locations (work, home, pub)
        updateWandering(npc, delta, world);
    }

    /**
     * Update NPC reacting to a structure.
     * Issue #910: NPCs speak dialogue when they arrive, then return to wandering after a timeout.
     */
    private void updateReactingToStructure(NPC npc, float delta, World world) {
        // Check if the reaction has timed out — return NPC to wandering
        float reactionTime = npcStructureReactionTimers.getOrDefault(npc, 0f);
        if (reactionTime >= STRUCTURE_REACTION_DURATION) {
            npcStructureReactionTimers.remove(npc);
            npc.setState(NPCState.WANDERING);
            // Issue #910: Give the NPC a delay before it can react again, so it doesn't
            // immediately re-enter a reaction state when checkForPlayerStructures fires next.
            npcStructureCheckTimers.put(npc, 30.0f);
            return;
        }

        // Find nearest player structure
        Vector3 nearestStructure = findNearestPlayerStructure(npc.getPosition());

        if (nearestStructure != null && !npc.isNear(nearestStructure, 3.0f)) {
            // Move toward structure
            setNPCTarget(npc, nearestStructure, world);
        } else {
            // At structure — stop moving and speak a reaction line if not already speaking
            npc.setVelocity(0, 0, 0);
            if (!npc.isSpeaking()) {
                String line = getConstructionReactionSpeech(npc.getState());
                if (line != null) {
                    npc.setSpeechText(line, 4.0f);
                }
            }
        }
    }

    /**
     * Return a random speech line for the given construction-reaction state.
     * Issue #910: NPC acknowledges player constructions with state-appropriate dialogue.
     */
    private String getConstructionReactionSpeech(NPCState state) {
        switch (state) {
            case STARING:
                return CONSTRUCTION_STARING_SPEECH[random.nextInt(CONSTRUCTION_STARING_SPEECH.length)];
            case PHOTOGRAPHING:
                return CONSTRUCTION_PHOTOGRAPHING_SPEECH[random.nextInt(CONSTRUCTION_PHOTOGRAPHING_SPEECH.length)];
            case COMPLAINING:
                return CONSTRUCTION_COMPLAINING_SPEECH[random.nextInt(CONSTRUCTION_COMPLAINING_SPEECH.length)];
            default:
                return null;
        }
    }

    /**
     * Update youth gang stealing.
     */
    private void updateStealing(NPC npc, float delta, Player player, Inventory inventory, TooltipSystem tooltipSystem) {
        if (npc.isNear(player.getPosition(), 1.5f)) {
            // Adjacent to player - steal!
            attemptTheft(npc, inventory, tooltipSystem);
            // Apply cooldown so this NPC can't steal again immediately
            npcStealCooldownTimers.put(npc, STEAL_COOLDOWN);
            npc.setState(NPCState.WANDERING); // Go back to wandering after theft
        }
    }

    /**
     * Attempt to steal from player inventory.
     * Steals the most valuable item available, using priority order:
     * DIAMOND > SCRAP_METAL > BRICK > WOOD > food items.
     * Falls back to a random non-empty slot if no priority item is found.
     * The stolen item is recorded on the NPC so the player can recover it by defeating them.
     */
    private void attemptTheft(NPC npc, Inventory inventory, TooltipSystem tooltipSystem) {
        // Priority order: most valuable first
        Material[] priority = {
            Material.DIAMOND,
            Material.SCRAP_METAL,
            Material.BRICK,
            Material.WOOD,
            Material.SAUSAGE_ROLL,
            Material.STEAK_BAKE,
            Material.CRISPS,
            Material.CHIPS,
            Material.KEBAB,
            Material.ENERGY_DRINK,
            Material.TIN_OF_BEANS,
            Material.PINT,
            Material.PERI_PERI_CHICKEN
        };

        // Try to steal highest-priority item first
        for (Material m : priority) {
            if (inventory.getItemCount(m) > 0) {
                inventory.removeItem(m, 1);
                npc.addStolenItem(m);
                if (tooltipSystem != null) {
                    tooltipSystem.trigger(TooltipTrigger.YOUTH_THEFT);
                }
                return;
            }
        }

        // Fall back to a random non-empty slot
        int size = inventory.getSize();
        int startSlot = random.nextInt(size);
        for (int i = 0; i < size; i++) {
            int slot = (startSlot + i) % size;
            Material m = inventory.getItemInSlot(slot);
            if (m != null && inventory.getItemCount(m) > 0) {
                inventory.removeItem(m, 1);
                npc.addStolenItem(m);
                if (tooltipSystem != null) {
                    tooltipSystem.trigger(TooltipTrigger.YOUTH_THEFT);
                }
                return;
            }
        }

        // Inventory is completely empty — do nothing
    }

    /**
     * Check for player-built structures nearby.
     */
    private void checkForPlayerStructures(NPC npc, World world) {
        if (npc.getType() != NPCType.PUBLIC && npc.getType() != NPCType.COUNCIL_MEMBER) {
            return;
        }

        // Stagger per-NPC checks: each NPC runs this at most once every 5 seconds.
        // Initialize timer for new NPCs with small random offset to stagger checks.
        float checkTimer = npcStructureCheckTimers.computeIfAbsent(npc, k -> random.nextFloat() * 0.5f);
        if (checkTimer > 0) {
            return;
        }
        npcStructureCheckTimers.put(npc, 5.0f);

        // Reduced scan radius (21×21 = 441 columns vs previous 41×41 = 1681)
        int scanRadius = 10;
        int playerBlockCount = 0;
        Vector3 structureCenter = null;

        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int x = (int) (npc.getPosition().x + dx);
                int z = (int) (npc.getPosition().z + dz);

                for (int y = 0; y < 10; y++) {
                    BlockType block = world.getBlock(x, y, z);
                    // Check for player-placed blocks
                    if (block == BlockType.WOOD || block == BlockType.BRICK
                        || block == BlockType.STONE || block == BlockType.GLASS
                        || block == BlockType.CARDBOARD) {
                        playerBlockCount++;
                        if (structureCenter == null) {
                            structureCenter = new Vector3(x, y, z);
                        }
                    }
                }
            }
        }

        // React if substantial structure detected
        if (playerBlockCount >= 10 && structureCenter != null) {
            if (npc.getState() != NPCState.STARING && npc.getState() != NPCState.PHOTOGRAPHING
                && npc.getState() != NPCState.COMPLAINING) {
                // Randomly choose reaction
                int reaction = random.nextInt(3);
                switch (reaction) {
                    case 0: npc.setState(NPCState.STARING); break;
                    case 1: npc.setState(NPCState.PHOTOGRAPHING); break;
                    case 2: npc.setState(NPCState.COMPLAINING); break;
                }

                // Issue #910: Start the reaction timer so the NPC eventually moves on
                npcStructureReactionTimers.put(npc, 0f);

                // Track this structure using string key to avoid Vector3 identity issues
                // (cap to prevent unbounded growth)
                if (playerStructures.size() < 64) {
                    String key = (int)structureCenter.x + "," + (int)structureCenter.y + "," + (int)structureCenter.z;
                    playerStructures.put(key, playerBlockCount);
                }
            }
        }
    }

    /**
     * Find nearest player structure.
     */
    private Vector3 findNearestPlayerStructure(Vector3 position) {
        Vector3 nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (String key : playerStructures.keySet()) {
            String[] parts = key.split(",");
            float sx = Float.parseFloat(parts[0]);
            float sy = Float.parseFloat(parts[1]);
            float sz = Float.parseFloat(parts[2]);
            float dist = position.dst(sx, sy, sz);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = new Vector3(sx, sy, sz);
            }
        }

        return nearest;
    }

    // How often (in seconds) to recalculate an NPC path
    private static final float PATH_RECALC_INTERVAL = 0.5f;

    /**
     * Set NPC target and find path.
     * Throttled: only recalculates the path at most every PATH_RECALC_INTERVAL seconds.
     * If pathfinding fails, tries a closer target rather than walking blindly.
     */
    private void setNPCTarget(NPC npc, Vector3 target, World world) {
        // Ensure target Y is at ground level
        float targetY = findGroundHeight(world, target.x, target.z);
        Vector3 adjustedTarget = new Vector3(target.x, targetY, target.z);
        npc.setTargetPosition(adjustedTarget);

        // Throttle: if NPC already has a path and recalc timer hasn't expired, skip pathfinding
        float recalcTimer = npcPathRecalcTimers.getOrDefault(npc, PATH_RECALC_INTERVAL);
        if (npc.getPath() != null && !npc.getPath().isEmpty() && recalcTimer < PATH_RECALC_INTERVAL) {
            return; // Keep using existing path
        }
        npcPathRecalcTimers.put(npc, 0.0f);

        // Find path
        List<Vector3> path = pathfinder.findPath(world, npc.getPosition(), adjustedTarget);

        if (path != null) {
            npc.setPath(path);
        } else {
            // Pathfinding failed — try a closer waypoint on the line toward the target
            Vector3 dir = adjustedTarget.cpy().sub(npc.getPosition()).nor();
            for (float dist = 5.0f; dist >= 2.0f; dist -= 1.5f) {
                Vector3 closer = npc.getPosition().cpy().add(dir.x * dist, 0, dir.z * dist);
                closer.y = findGroundHeight(world, closer.x, closer.z);
                path = pathfinder.findPath(world, npc.getPosition(), closer);
                if (path != null) {
                    npc.setPath(path);
                    npc.setTargetPosition(closer);
                    return;
                }
            }
            // All pathfinding failed — clear target so NPC picks a new random one
            npc.setPath(null);
            npc.setTargetPosition(null);
        }
    }

    /**
     * Move NPC directly toward a target position.
     * If destination is reached, clear the target.
     */
    private void moveTowardTarget(NPC npc, Vector3 target, float delta) {
        if (npc.isNear(target, 1.0f)) {
            npc.setVelocity(0, 0, 0);
            npc.setTargetPosition(null); // Reached target, will pick a new one next frame
            return;
        }

        Vector3 direction = target.cpy().sub(npc.getPosition()).nor();
        float speed = getNPCSpeed(npc.getType());

        // Drunks stumble — add random wobble to direction
        if (npc.getType() == NPCType.DRUNK) {
            direction.x += (random.nextFloat() - 0.5f) * 0.5f;
            direction.z += (random.nextFloat() - 0.5f) * 0.5f;
            direction.nor();
        }

        // Preserve vertical velocity so gravity is not cancelled by horizontal movement.
        npc.setVelocity(direction.x * speed, npc.getVelocity().y, direction.z * speed);
    }

    private float getNPCSpeed(NPCType type) {
        switch (type) {
            case DOG: return NPC.DOG_SPEED;
            case JOGGER: return NPC.MOVE_SPEED * 2.5f;  // Fast runners
            case DRUNK: return NPC.MOVE_SPEED * 0.6f;   // Slow stumble
            case POSTMAN: return NPC.MOVE_SPEED * 1.3f;  // Brisk walk
            case POLICE: return NPC.MOVE_SPEED * 1.4f;   // Quick patrol
            case DELIVERY_DRIVER: return NPC.MOVE_SPEED * 1.8f; // Always rushing
            case PENSIONER: return NPC.MOVE_SPEED * 0.4f;       // Very slow shuffle
            case SCHOOL_KID: return NPC.MOVE_SPEED * 1.6f;      // Hyper kids
            case BIRD: return NPC.MOVE_SPEED * 3.5f;            // Birds fly fast
            default: return NPC.MOVE_SPEED;
        }
    }

    /**
     * Apply world collision to NPC movement after velocity has been set.
     * Called from updateNPC after movement/path following.
     */
    private void applyNPCCollision(NPC npc, float delta, World world) {
        Vector3 pos = npc.getPosition();
        Vector3 vel = npc.getVelocity();

        // Clamp velocity to prevent single-frame terrain clipping
        float maxHorizontalSpeed = 20f;
        float hSpeed = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (hSpeed > maxHorizontalSpeed) {
            float scale = maxHorizontalSpeed / hSpeed;
            vel.x *= scale;
            vel.z *= scale;
        }

        float moveX = vel.x * delta;
        float moveZ = vel.z * delta;

        // Horizontal movement with collision sliding
        if (moveX != 0 || moveZ != 0) {
            float origX = pos.x;
            float origZ = pos.z;

            // Try full horizontal movement
            pos.x += moveX;
            pos.z += moveZ;
            npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);

            if (world.checkAABBCollision(npc.getAABB())) {
                // Collision — try sliding on each axis
                pos.x = origX;
                pos.z = origZ;

                // Try X only
                pos.x += moveX;
                npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);
                if (world.checkAABBCollision(npc.getAABB())) {
                    pos.x = origX;
                }

                // Try Z only
                pos.z += moveZ;
                npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);
                if (world.checkAABBCollision(npc.getAABB())) {
                    pos.z = origZ;
                }

                npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);

                // If knocked back and fully blocked, kill the knockback velocity
                if (npc.isKnockedBack()) {
                    vel.x = 0;
                    vel.z = 0;
                }
            }
        }

        // Vertical movement — apply gravity and handle upward knockback velocity
        float verticalMove = vel.y * delta;
        if (verticalMove > 0) {
            // Moving upward (knockback pop)
            pos.y += verticalMove;
            npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);
            if (world.checkAABBCollision(npc.getAABB())) {
                pos.y -= verticalMove;
                vel.y = 0;
                npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);
            }
            // Apply gravity to slow the upward movement
            vel.y = vel.y - 9.8f * delta;
        } else {
            // Falling or stationary — check ground
            int blockBelow = (int) Math.floor(pos.y - 0.1f);
            int bx = (int) Math.floor(pos.x);
            int bz = (int) Math.floor(pos.z);
            boolean onGround = world.getBlock(bx, blockBelow, bz).isSolid();

            if (!onGround) {
                vel.y = vel.y - 9.8f * delta;
                pos.y += vel.y * delta;
                npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);

                if (world.checkAABBCollision(npc.getAABB())) {
                    // Snap feet to the top of the floor block to avoid sinking into it.
                    // Math.ceil can place the NPC at y=N which is the top of block N-1 but
                    // also the bottom face of block N — meaning the NPC is inside the block
                    // when pos.y is exactly an integer. Using floor+1 reliably places the
                    // NPC's foot on the top surface of the solid block below.
                    pos.y = (float) Math.floor(pos.y) + 1.0f;
                    vel.y = 0;
                    npc.getAABB().setPosition(pos, NPC.WIDTH, NPC.HEIGHT, NPC.DEPTH);
                }
            } else {
                vel.y = 0;
            }
        }
    }

    /**
     * Make NPC follow their current path.
     */
    private void followPath(NPC npc, float delta) {
        List<Vector3> path = npc.getPath();
        if (path == null || path.isEmpty()) {
            return;
        }

        int currentIndex = npc.getCurrentPathIndex();
        if (currentIndex >= path.size()) {
            npc.setPath(null);
            npc.setTargetPosition(null);
            npc.setVelocity(0, 0, 0);
            return;
        }

        Vector3 waypoint = path.get(currentIndex);

        // Skip waypoints that are close — allows NPCs to advance past several at once
        while (npc.isNear(waypoint, 1.0f)) {
            npc.advancePathIndex();
            if (npc.getCurrentPathIndex() >= path.size()) {
                npc.setPath(null);
                npc.setTargetPosition(null);
                npc.setVelocity(0, 0, 0);
                return;
            }
            waypoint = path.get(npc.getCurrentPathIndex());
        }

        // Move toward waypoint — preserve vertical velocity so gravity is not cancelled.
        Vector3 direction = waypoint.cpy().sub(npc.getPosition()).nor();
        float speed = getNPCSpeed(npc.getType());
        npc.setVelocity(direction.x * speed, npc.getVelocity().y, direction.z * speed);
    }

    /**
     * Get random position near a point.
     */
    private Vector3 getRandomNearbyPosition(Vector3 center, float radius) {
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = random.nextFloat() * radius;

        float x = center.x + (float) Math.cos(angle) * distance;
        float z = center.z + (float) Math.sin(angle) * distance;

        return new Vector3(x, center.y, z);
    }

    /**
     * Get random walkable position near a point.
     * Searches for actual ground height at each candidate to handle terrain variation.
     */
    private Vector3 getRandomWalkablePosition(Vector3 center, World world, float radius) {
        for (int attempt = 0; attempt < 10; attempt++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = 2.0f + random.nextFloat() * (radius - 2.0f); // At least 2 blocks away

            float x = center.x + (float) Math.cos(angle) * distance;
            float z = center.z + (float) Math.sin(angle) * distance;

            float groundY = findGroundHeight(world, x, z);
            int blockX = (int) Math.floor(x);
            int groundInt = (int) groundY;

            // Check that there's actually headroom (2 blocks of air above ground)
            BlockType atPos = world.getBlock(blockX, groundInt, (int) Math.floor(z));
            BlockType above = world.getBlock(blockX, groundInt + 1, (int) Math.floor(z));

            if (!atPos.isSolid() && !above.isSolid()) {
                return new Vector3(x, groundY, z);
            }
        }

        // Fallback: try directly near the NPC with a small offset
        float fallbackAngle = random.nextFloat() * (float) Math.PI * 2;
        float fx = center.x + (float) Math.cos(fallbackAngle) * 3f;
        float fz = center.z + (float) Math.sin(fallbackAngle) * 3f;
        float fy = findGroundHeight(world, fx, fz);
        return new Vector3(fx, fy, fz);
    }

    /**
     * Keep dogs within park boundaries.
     */
    private void enforceParKBoundaries(NPC npc) {
        if (!npc.isWithinBounds(PARK_MIN_X, PARK_MIN_Z, PARK_MAX_X, PARK_MAX_Z)) {
            // Push back toward center
            Vector3 parkCenter = new Vector3(0, npc.getPosition().y, 0);
            Vector3 direction = parkCenter.cpy().sub(npc.getPosition()).nor();
            npc.getPosition().add(direction.scl(0.1f));
        }
    }

    /**
     * Handle punching an NPC - applies knockback and damage.
     */
    public void punchNPC(NPC npc, Vector3 punchDirection) {
        punchNPC(npc, punchDirection, null, null, null, null);
    }

    public void punchNPC(NPC npc, Vector3 punchDirection, Inventory inventory, TooltipSystem tooltipSystem) {
        punchNPC(npc, punchDirection, inventory, tooltipSystem, null, null);
    }

    /**
     * Handle punching an NPC - applies knockback, damage, and loot drops on kill.
     * When playerPos and world are provided, punching a POLICE NPC also adds them to
     * alertedPoliceNPCs and alerts nearby officers (within 20 blocks) — matching the
     * pattern used in alertPoliceToGreggRaid().
     */
    public void punchNPC(NPC npc, Vector3 punchDirection, Inventory inventory, TooltipSystem tooltipSystem,
                         Vector3 playerPos, World world) {
        npc.applyKnockback(punchDirection, 2.0f); // 2 blocks of knockback

        // Deal damage (10 HP per punch)
        boolean killed = npc.takeDamage(10f);
        if (killed) {
            // NPC defeated - enter knocked out animation state, show speech, schedule removal
            npc.setState(NPCState.KNOCKED_OUT);
            npc.setSpeechText(getDeathSpeech(npc.getType()), 2.0f);

            // Award loot drops (including any stolen items recovered from this NPC)
            if (inventory != null) {
                awardNPCLoot(npc, inventory, tooltipSystem);
            }
        } else {
            // NPC reacts to being hit
            npc.setSpeechText(getHitSpeech(npc.getType()), 2.0f);
        }

        // Special handling for council builders
        if (npc.getType() == NPCType.COUNCIL_BUILDER) {
            punchCouncilBuilder(npc);
        }

        // Police become aggressive when attacked by the player
        if (npc.getType() == NPCType.POLICE && !killed
                && npc.getState() != NPCState.AGGRESSIVE
                && npc.getState() != NPCState.ARRESTING
                && npc.getState() != NPCState.KNOCKED_OUT) {
            npc.setState(NPCState.AGGRESSIVE);
            npc.setSpeechText("That's assault! You're nicked!", 3.0f);
            alertedPoliceNPCs.add(npc);
            if (playerPos != null && world != null) {
                setNPCTarget(npc, playerPos, world);
                // Alert nearby police officers (within 20 blocks) — backup arrives
                alertNearbyPoliceToPlayerAttack(npc, playerPos, world);
            }
        }
    }

    /**
     * Alert nearby police officers (within 20 blocks of the attacked officer) to the
     * player's position so backup arrives immediately. Matches the pattern used in
     * alertPoliceToGreggRaid().
     */
    private void alertNearbyPoliceToPlayerAttack(NPC attackedOfficer, Vector3 playerPos, World world) {
        for (NPC npc : npcs) {
            if (npc == attackedOfficer) continue;
            if (npc.getType() != NPCType.POLICE || !npc.isAlive()) continue;
            if (npc.getState() == NPCState.AGGRESSIVE || npc.getState() == NPCState.ARRESTING
                    || npc.getState() == NPCState.KNOCKED_OUT) continue;
            float dist = npc.getPosition().dst(attackedOfficer.getPosition());
            if (dist <= 20.0f) {
                npc.setState(NPCState.AGGRESSIVE);
                npc.setSpeechText("Oi! Assaulting an officer!", 3.0f);
                alertedPoliceNPCs.add(npc);
                setNPCTarget(npc, playerPos, world);
            }
        }
    }

    /**
     * Award loot when an NPC is defeated.
     * Each NPC type drops thematic items — shopkeepers drop food,
     * youth gangs drop whatever they nicked, delivery drivers drop parcels, etc.
     * YOUTH_GANG NPCs also return any items they stole from the player.
     */
    private void awardNPCLoot(NPC npc, Inventory inventory, TooltipSystem tooltipSystem) {
        NPCType type = npc.getType();

        // Rare chance for any NPC to drop antidepressants (5% chance)
        if (type != NPCType.DOG && random.nextFloat() < 0.05f) {
            inventory.addItem(Material.ANTIDEPRESSANTS, 1);
        }

        switch (type) {
            case YOUTH_GANG:
                // Return any items stolen from the player
                for (ragamuffin.building.Material stolen : npc.claimStolenItems()) {
                    inventory.addItem(stolen, 1);
                }
                // They may also have had generic loot
                inventory.addItem(Material.WOOD, 2 + random.nextInt(3));
                if (random.nextFloat() < 0.3f) {
                    inventory.addItem(Material.SCRAP_METAL, 1);
                }
                break;
            case SHOPKEEPER:
                // Raiding the till and shelves
                inventory.addItem(Material.CRISPS, 1 + random.nextInt(3));
                inventory.addItem(Material.ENERGY_DRINK, 1);
                if (random.nextFloat() < 0.5f) {
                    inventory.addItem(Material.TIN_OF_BEANS, 1 + random.nextInt(2));
                }
                // Till float — always carries shillings
                inventory.addItem(Material.SHILLING, 1 + random.nextInt(2));
                break;
            case DELIVERY_DRIVER:
                // Whatever's in the van
                double deliveryRoll = random.nextDouble();
                if (deliveryRoll < 0.25) {
                    inventory.addItem(Material.KEBAB, 1 + random.nextInt(2));
                } else if (deliveryRoll < 0.5) {
                    inventory.addItem(Material.CHIPS, 2);
                } else if (deliveryRoll < 0.75) {
                    inventory.addItem(Material.CARDBOARD, 2 + random.nextInt(3));
                } else {
                    inventory.addItem(Material.ENERGY_DRINK, 2);
                }
                break;
            case POSTMAN:
                // Letters and parcels
                inventory.addItem(Material.CARDBOARD, 1 + random.nextInt(3));
                if (random.nextFloat() < 0.3f) {
                    inventory.addItem(Material.SCRAP_METAL, 1);
                }
                // Small chance of loose change from their pocket
                if (random.nextFloat() < 0.2f) {
                    inventory.addItem(Material.PENNY, 1);
                }
                break;
            case DRUNK:
                // Empties and kebab remnants
                if (random.nextFloat() < 0.5f) {
                    inventory.addItem(Material.KEBAB, 1);
                }
                inventory.addItem(Material.GLASS, 1);
                // Coin that fell out of their pocket
                if (random.nextFloat() < 0.25f) {
                    inventory.addItem(random.nextFloat() < 0.5f ? Material.PENNY : Material.SHILLING, 1);
                }
                break;
            case BUSKER:
                // Guitar parts and busking money
                inventory.addItem(Material.WOOD, 1 + random.nextInt(2));
                inventory.addItem(Material.SCRAP_METAL, 1);
                // Busking money — always has some pennies
                inventory.addItem(Material.PENNY, 1 + random.nextInt(3));
                break;
            case POLICE:
                // Confiscated goods
                inventory.addItem(Material.SCRAP_METAL, 1 + random.nextInt(2));
                if (random.nextFloat() < 0.4f) {
                    inventory.addItem(Material.ENERGY_DRINK, 1);
                }
                break;
            case JOGGER:
                // Sports nutrition
                inventory.addItem(Material.ENERGY_DRINK, 1 + random.nextInt(2));
                break;
            case PENSIONER:
                // Shopping bag contents
                inventory.addItem(Material.TIN_OF_BEANS, 1 + random.nextInt(2));
                if (random.nextFloat() < 0.4f) {
                    inventory.addItem(Material.CRISPS, 1);
                }
                // Small chance of loose pennies from their purse
                if (random.nextFloat() < 0.3f) {
                    inventory.addItem(Material.PENNY, 1 + random.nextInt(2));
                }
                break;
            case SCHOOL_KID:
                // Lunch money equivalent
                inventory.addItem(Material.CRISPS, 1 + random.nextInt(2));
                if (random.nextFloat() < 0.3f) {
                    inventory.addItem(Material.ENERGY_DRINK, 1);
                }
                break;
            case COUNCIL_MEMBER:
                // Paperwork and office supplies
                inventory.addItem(Material.CARDBOARD, 2 + random.nextInt(2));
                // Small chance of a shilling — expenses money
                if (random.nextFloat() < 0.3f) {
                    inventory.addItem(Material.SHILLING, 1);
                }
                break;
            case COUNCIL_BUILDER:
                // Building materials
                inventory.addItem(Material.BRICK, 2 + random.nextInt(3));
                inventory.addItem(Material.STONE, 1 + random.nextInt(2));
                break;
            case PUBLIC:
                // Random shopping
                if (random.nextFloat() < 0.5f) {
                    inventory.addItem(Material.SAUSAGE_ROLL, 1);
                } else {
                    inventory.addItem(Material.CRISPS, 1);
                }
                break;
            case DOG:
                // Dogs don't carry loot
                break;
            default:
                break;
        }

        // Trigger first loot tooltip
        if (tooltipSystem != null) {
            tooltipSystem.trigger(TooltipTrigger.FIRST_NPC_LOOT);
        }
    }

    /**
     * Tick the per-NPC conversation cooldown and, when both conditions are met
     * (cooldown expired, both NPCs free to speak, close enough together, both in
     * a calm state), trigger a short paired exchange.  The initiating NPC speaks
     * first; the responder's reply is queued so it starts just after the first
     * line finishes (3 s initiator + 0.5 s gap = 3.5 s delay for responder).
     *
     * Only NPCs that are IDLE or WANDERING will initiate — hostile/fleeing NPCs
     * are too busy.  Dogs cannot converse (no speech lines defined for pairs).
     */
    private void updateNPCToNPCDialogue(NPC npc, float delta) {
        // Tick down this NPC's conversation cooldown
        npcConversationCooldowns.computeIfPresent(npc, (k, v) -> Math.max(0f, v - delta));

        // Only initiate from calm states (not hostile, fleeing, knocked-out, etc.)
        NPCState state = npc.getState();
        if (state == NPCState.AGGRESSIVE || state == NPCState.FLEEING
                || state == NPCState.KNOCKED_OUT || state == NPCState.KNOCKED_BACK
                || state == NPCState.ARRESTING || state == NPCState.WARNING
                || state == NPCState.STEALING || state == NPCState.DEMOLISHING) {
            return;
        }

        // Skip if already speaking or still on cooldown
        if (npc.isSpeaking()) return;
        float cooldown = npcConversationCooldowns.getOrDefault(npc, 0f);
        if (cooldown > 0f) return;

        // Low random chance per frame — keeps exchanges feeling organic
        if (random.nextFloat() >= 0.002f) return;

        // Find a nearby NPC to talk to
        for (NPC other : npcs) {
            if (other == npc || !other.isAlive() || other.isSpeaking()) continue;
            if (other.getPosition().dst(npc.getPosition()) > NPC_CONVERSATION_RANGE) continue;

            // Other NPC must also be in a calm state
            NPCState otherState = other.getState();
            if (otherState == NPCState.AGGRESSIVE || otherState == NPCState.FLEEING
                    || otherState == NPCState.KNOCKED_OUT || otherState == NPCState.KNOCKED_BACK
                    || otherState == NPCState.ARRESTING || otherState == NPCState.WARNING
                    || otherState == NPCState.STEALING || otherState == NPCState.DEMOLISHING) {
                continue;
            }

            // Also check other NPC's cooldown
            float otherCooldown = npcConversationCooldowns.getOrDefault(other, 0f);
            if (otherCooldown > 0f) continue;

            // Look up exchange lines for this type pair
            String[][] exchanges = getNPCToNPCExchanges(npc.getType(), other.getType());
            if (exchanges == null || exchanges.length == 0) continue;

            // Pick a random exchange
            String[] exchange = exchanges[random.nextInt(exchanges.length)];
            String initiatorLine = exchange[0];
            String responderLine = exchange[1];

            // Initiator speaks immediately; responder replies after initiator finishes
            float initiatorDuration = 3.0f;
            float responderDelay = initiatorDuration + 0.5f;
            npc.setSpeechText(initiatorLine, initiatorDuration);

            // Schedule the responder's reply by setting a slightly longer speech timer
            // on the other NPC.  We use setSpeechText directly with a longer total
            // duration; the first 3.5 s the text will be blank (timer counts down from
            // responderDelay, text only set when it goes ≤ initiatorDuration).
            // Simpler approach: just set the reply with the longer duration so it appears
            // after a natural pause. We accept that both NPCs appear to start at the same
            // time but the responder's text is contextually a reply.
            other.setSpeechText(responderLine, 3.0f);

            // Set cooldowns so neither NPC initiates another exchange right away
            npcConversationCooldowns.put(npc, NPC_CONVERSATION_COOLDOWN);
            npcConversationCooldowns.put(other, NPC_CONVERSATION_COOLDOWN);
            break; // Only one exchange per NPC per frame
        }
    }

    /**
     * Return the set of dialogue exchange lines appropriate for the given NPC
     * type pair, or {@code null} if no exchanges are defined for that pairing.
     * The order of {@code a} and {@code b} does not matter — the table is
     * consulted symmetrically.
     */
    public String[][] getNPCToNPCExchanges(NPCType a, NPCType b) {
        // Normalise so we always check (smaller-ordinal, larger-ordinal)
        if (a.ordinal() > b.ordinal()) {
            NPCType tmp = a; a = b; b = tmp;
        }

        // Ordinal order: PUBLIC(0), DOG(1), YOUTH_GANG(2), COUNCIL_MEMBER(3), POLICE(4),
        // COUNCIL_BUILDER(5), SHOPKEEPER(6), POSTMAN(7), JOGGER(8), DRUNK(9), BUSKER(10),
        // DELIVERY_DRIVER(11), PENSIONER(12), SCHOOL_KID(13)
        if (a == NPCType.PUBLIC && b == NPCType.PUBLIC)                   return PUBLIC_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.YOUTH_GANG)               return null; // no exchange for PUBLIC↔GANG
        if (a == NPCType.PUBLIC && b == NPCType.POLICE)                   return POLICE_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.SHOPKEEPER)               return SHOPKEEPER_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.POSTMAN)                  return POSTMAN_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.JOGGER)                   return JOGGER_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.DRUNK)                    return DRUNK_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.BUSKER)                   return BUSKER_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.DELIVERY_DRIVER)          return DELIVERY_DRIVER_PUBLIC_EXCHANGES;
        if (a == NPCType.PUBLIC && b == NPCType.PENSIONER)                return PUBLIC_PENSIONER_EXCHANGES;
        if (a == NPCType.YOUTH_GANG && b == NPCType.YOUTH_GANG)           return YOUTH_YOUTH_EXCHANGES;
        // After ordinal sort: YOUTH_GANG(2) < POLICE(4), so pair is (YOUTH_GANG, POLICE)
        if (a == NPCType.YOUTH_GANG && b == NPCType.POLICE)               return POLICE_YOUTH_EXCHANGES;
        if (a == NPCType.PENSIONER && b == NPCType.PENSIONER)             return PENSIONER_PENSIONER_EXCHANGES;
        if (a == NPCType.SCHOOL_KID && b == NPCType.SCHOOL_KID)           return SCHOOL_KID_SCHOOL_KID_EXCHANGES;
        return null;
    }

    private String getRandomSpeech(NPCType type) {
        switch (type) {
            case PUBLIC: return RANDOM_PUBLIC_SPEECH[random.nextInt(RANDOM_PUBLIC_SPEECH.length)];
            case YOUTH_GANG: return RANDOM_YOUTH_SPEECH[random.nextInt(RANDOM_YOUTH_SPEECH.length)];
            case POLICE: return RANDOM_POLICE_SPEECH[random.nextInt(RANDOM_POLICE_SPEECH.length)];
            case SHOPKEEPER: return RANDOM_SHOPKEEPER_SPEECH[random.nextInt(RANDOM_SHOPKEEPER_SPEECH.length)];
            case POSTMAN: return RANDOM_POSTMAN_SPEECH[random.nextInt(RANDOM_POSTMAN_SPEECH.length)];
            case JOGGER: return RANDOM_JOGGER_SPEECH[random.nextInt(RANDOM_JOGGER_SPEECH.length)];
            case DRUNK: return RANDOM_DRUNK_SPEECH[random.nextInt(RANDOM_DRUNK_SPEECH.length)];
            case BUSKER: return RANDOM_BUSKER_SPEECH[random.nextInt(RANDOM_BUSKER_SPEECH.length)];
            case DELIVERY_DRIVER: return RANDOM_DELIVERY_SPEECH[random.nextInt(RANDOM_DELIVERY_SPEECH.length)];
            case PENSIONER: return RANDOM_PENSIONER_SPEECH[random.nextInt(RANDOM_PENSIONER_SPEECH.length)];
            case SCHOOL_KID: return RANDOM_SCHOOL_KID_SPEECH[random.nextInt(RANDOM_SCHOOL_KID_SPEECH.length)];
            case STREET_PREACHER: return RANDOM_STREET_PREACHER_SPEECH[random.nextInt(RANDOM_STREET_PREACHER_SPEECH.length)];
            case LOLLIPOP_LADY: return RANDOM_LOLLIPOP_LADY_SPEECH[random.nextInt(RANDOM_LOLLIPOP_LADY_SPEECH.length)];
            case TUNNEL_DWELLER: return RANDOM_TUNNEL_DWELLER_SPEECH[random.nextInt(RANDOM_TUNNEL_DWELLER_SPEECH.length)];
            default: return null;
        }
    }

    private String getHitSpeech(NPCType type) {
        switch (type) {
            case PUBLIC: return HIT_SPEECH_PUBLIC[random.nextInt(HIT_SPEECH_PUBLIC.length)];
            case YOUTH_GANG: return HIT_SPEECH_YOUTH[random.nextInt(HIT_SPEECH_YOUTH.length)];
            case POLICE: return HIT_SPEECH_POLICE[random.nextInt(HIT_SPEECH_POLICE.length)];
            case DOG: return "*yelp!*";
            case COUNCIL_BUILDER: return "Assaulting a council worker!";
            case SHOPKEEPER: return "I'm calling the police!";
            case POSTMAN: return "That's a federal offence!";
            case JOGGER: return "What the— I'll sue!";
            case DRUNK: return "Oi! Buy me a pint first!";
            case BUSKER: return "That's me guitar arm!";
            case DELIVERY_DRIVER: return "I'm on a schedule!";
            case PENSIONER: return "I'll tell your mother!";
            case SCHOOL_KID: return "I'm telling sir!";
            case STREET_PREACHER: return "The Lord will judge you!";
            case LOLLIPOP_LADY: return "Children are watching!";
            default: return "Ow!";
        }
    }

    private String getDeathSpeech(NPCType type) {
        switch (type) {
            case PUBLIC: return "I'm calling an ambulance...";
            case YOUTH_GANG: return "I'm done, I'm done!";
            case POLICE: return "Officer down!";
            case DOG: return "*whimper*";
            case COUNCIL_BUILDER: return "Health and safety violation!";
            case SHOPKEEPER: return "You're barred for life!";
            case POSTMAN: return "Me parcels...";
            case JOGGER: return "Strava... won't... log this...";
            case DRUNK: return "*passes out*";
            case BUSKER: return "Final... encore...";
            case DELIVERY_DRIVER: return "The parcel... it's fragile...";
            case PENSIONER: return "Me hip...";
            case SCHOOL_KID: return "I want me mum!";
            default: return "...";
        }
    }

    /**
     * Notify manager of a placed block (for structure detection).
     */
    public void notifyBlockPlaced(Vector3 position) {
        // Track placed blocks for structure detection
        // This is a simplified version - real implementation would maintain a set
    }

    /**
     * Alert all nearby police to a Greggs raid and send them toward the player.
     * Any existing patrolling police within 40 blocks go aggressive immediately.
     * If there are no police nearby, spawn a fresh unit.
     */
    public void alertPoliceToGreggRaid(Player player, World world) {
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.POLICE && npc.isAlive()
                    && npc.getState() != NPCState.AGGRESSIVE) {
                float dist = npc.getPosition().dst(player.getPosition());
                if (dist < 40.0f) {
                    npc.setState(NPCState.AGGRESSIVE);
                    npc.setSpeechText("Oi! Put the pasty down!", 3.0f);
                    alertedPoliceNPCs.add(npc);
                    setNPCTarget(npc, player.getPosition(), world);
                }
            }
        }
        // Spawn an additional police unit homing in on the player
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float spawnDist = 20 + random.nextFloat() * 10;
        float sx = player.getPosition().x + (float) Math.cos(angle) * spawnDist;
        float sz = player.getPosition().z + (float) Math.sin(angle) * spawnDist;
        float sy = findGroundHeight(world, sx, sz);
        NPC responder = spawnNPC(NPCType.POLICE, sx, sy, sz);
        if (responder != null) {
            responder.setState(NPCState.AGGRESSIVE);
            responder.setSpeechText("999 call - sausage roll theft!", 3.0f);
            alertedPoliceNPCs.add(responder);
            setNPCTarget(responder, player.getPosition(), world);
        }
    }

    /**
     * Returns true if the given police NPC is in the alertedPoliceNPCs set.
     * Used by tests to verify Fix #487 — punched officers are properly alerted.
     */
    public boolean isAlertedPolice(NPC npc) {
        return alertedPoliceNPCs.contains(npc);
    }

    /**
     * Tick the police spawn cooldown by delta seconds.
     * Called in the PAUSED branch so the cooldown continues to drain while the game is paused,
     * preventing the swarm-spawn that would otherwise occur on resume (Fix #393).
     */
    public void tickSpawnCooldown(float delta) {
        if (policeSpawnCooldown > 0) {
            policeSpawnCooldown -= delta;
        }
    }

    /**
     * Tick the post-arrest cooldown by delta seconds.
     * Called in the PAUSED branch so the cooldown continues to drain while the game is paused,
     * preventing the player from exploiting pause to extend re-arrest immunity indefinitely (Fix #403).
     */
    public void tickPostArrestCooldown(float delta) {
        if (postArrestCooldown > 0) {
            postArrestCooldown = Math.max(0f, postArrestCooldown - delta);
        }
    }

    /**
     * Tick speech timers for all NPCs by delta seconds.
     * Called in the PAUSED branch so speech bubbles continue to count down while paused,
     * preventing them from persisting through the pause then expiring instantly on resume (Fix #397).
     * Only the speech timer is advanced — attack cooldowns, blink cycles, and animation
     * timers remain frozen until the normal update() resumes in PLAYING state (Fix #423).
     */
    public void tickSpeechTimers(float delta) {
        for (NPC npc : npcs) {
            npc.tickSpeechOnly(delta);
        }
    }

    /**
     * Tick knockback recovery timers for all NPCs by delta seconds.
     * Called in the PAUSED branch so knockback velocity is cleared on schedule even while
     * the game is paused, preventing the player from exploiting pause to extend NPC stagger
     * indefinitely (Fix #405).
     */
    public void tickKnockbackTimers(float delta) {
        for (NPC npc : npcs) {
            npc.updateKnockback(delta);
        }
    }

    /**
     * Tick KNOCKED_OUT recovery timers for all NPCs by delta seconds.
     * Called in the PAUSED branch so the NPC's recovery countdown continues to advance
     * while the game is paused, preventing the player from exploiting pause to keep NPCs
     * permanently incapacitated (Fix #407).
     *
     * <p>This method advances the knocked-out timer for each incapacitated NPC and revives
     * the NPC (restoring it to half health and WANDERING state) once {@link #KNOCKED_OUT_RECOVERY_DURATION}
     * has elapsed. It is intentionally limited to recovery logic only — NPC movement and AI
     * remain frozen while paused.
     */
    public void tickRecoveryTimers(float delta) {
        for (NPC npc : npcs) {
            if (!npc.isAlive() && npc.getState() == NPCState.KNOCKED_OUT) {
                npc.tickKnockedOutTimer(delta);
                if (npc.getKnockedOutTimer() >= KNOCKED_OUT_RECOVERY_DURATION) {
                    npc.revive();
                }
            }
        }
    }

    /**
     * Update police spawning — police patrol at night (seasonal: after sunset, before sunrise).
     * At dawn, all police are despawned. At night, officers are spawned up to the cap based on
     * player notoriety.
     *
     * @param isNight true if it is currently night (from TimeSystem.isNight())
     */
    public void updatePoliceSpawning(boolean isNight, World world, Player player) {
        // Despawn police at dawn (night → day transition)
        if (wasNight && !isNight) {
            despawnPolice();
        }
        wasNight = isNight;

        // Police are a night-only threat — no spawning during daytime
        if (!isNight) return;

        // Throttle spawning to avoid spawning (and triggering A* pathfinding) every frame
        if (policeSpawnCooldown > 0) {
            return;
        }

        // Notorious players attract more police attention
        int maxPolice = player.getStreetReputation().isNotorious() ? 8 : 4;

        // Count current police
        long policeCount = npcs.stream().filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();

        if (policeCount < maxPolice) {
            int remainingSlots = (int) (maxPolice - policeCount);
            spawnPolice(player, world, remainingSlots);
        }
        policeSpawnCooldown = POLICE_SPAWN_INTERVAL;
    }

    /**
     * Spawn police NPCs around the player, capped to remainingSlots.
     */
    private void spawnPolice(Player player, World world, int remainingSlots) {
        // Spawn 2-3 police around the player, but never exceed the cap
        int policeCount = Math.min(2 + random.nextInt(2), remainingSlots);

        for (int i = 0; i < policeCount; i++) {
            // Spawn police 15-25 blocks away from player
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = 15 + random.nextFloat() * 10;

            float x = player.getPosition().x + (float) Math.cos(angle) * distance;
            float z = player.getPosition().z + (float) Math.sin(angle) * distance;
            float y = findGroundHeight(world, x, z);

            NPC police = spawnNPC(NPCType.POLICE, x, y, z);
            if (police == null) break;
            police.setState(NPCState.PATROLLING);
        }
    }

    /**
     * Spawn 2-3 YOUTH_GANG NPCs near the player in AGGRESSIVE state.
     * Called by GangTerritorySystem when territory turns hostile to ensure the
     * player faces attackers even if no gang NPCs happen to be nearby.
     *
     * @param player the player (attackers spawn 8-15 blocks away)
     * @param world  used to find ground height for spawn positions
     */
    public void spawnGangAttackers(Player player, World world) {
        int count = 2 + random.nextInt(2); // 2 or 3
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = 8 + random.nextFloat() * 7;
            float x = player.getPosition().x + (float) Math.cos(angle) * distance;
            float z = player.getPosition().z + (float) Math.sin(angle) * distance;
            float y = findGroundHeight(world, x, z);
            NPC attacker = spawnNPC(NPCType.YOUTH_GANG, x, y, z);
            if (attacker == null) break;
            attacker.setState(NPCState.AGGRESSIVE);
            attacker.setSpeechText("Wrong ends, bruv!", 3.0f);
            setNPCTarget(attacker, player.getPosition(), world);
        }
    }

    /**
     * Find the ground height at a world position — searches down from y=64 for the first solid block.
     */
    private float findGroundHeight(World world, float x, float z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int y = 64; y >= -10; y--) {
            if (world.getBlock(bx, y, bz).isSolid()) {
                return y + 1.0f;
            }
        }
        return 1.0f; // fallback
    }

    /**
     * Despawn all police NPCs.
     */
    private void despawnPolice() {
        List<NPC> policeToRemove = new ArrayList<>();

        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.POLICE) {
                policeToRemove.add(npc);
            }
        }

        for (NPC police : policeToRemove) {
            cleanupNPC(police);
            npcs.remove(police);
        }

        // Clear the entire alerted set on dawn despawn — all police are gone, so any
        // previously-alerted NPC references would be stale. Alerted state should not
        // persist across night cycles into the next spawned cohort.
        alertedPoliceNPCs.clear();
    }

    /**
     * Update police behavior.
     */
    private void updatePolice(NPC police, float delta, World world, Player player, TooltipSystem tooltipSystem) {
        switch (police.getState()) {
            case PATROLLING:
                updatePolicePatrolling(police, delta, world, player, tooltipSystem);
                break;
            case SUSPICIOUS:
                updatePoliceSuspicious(police, delta, world, player);
                break;
            case WARNING:
                updatePoliceWarning(police, delta, world, player);
                break;
            case AGGRESSIVE:
            case ARRESTING:
                updatePoliceAggressive(police, delta, world, player);
                break;
        }
    }

    /**
     * Update police patrolling behavior.
     * Issue #689: Uses cone+hearing detection instead of simple radius-based detection.
     */
    private void updatePolicePatrolling(NPC police, float delta, World world, Player player, TooltipSystem tooltipSystem) {
        // If player is sheltered, police cannot detect them — wander instead
        if (ShelterDetector.isSheltered(world, player.getPosition())) {
            updateWandering(police, delta, world);
            return;
        }

        // Proximity contact: adjacent player is always detected regardless of vision cone or noise.
        // Check this FIRST so the hearing-only SUSPICIOUS path cannot pre-empt direct contact.
        if (police.isNear(player.getPosition(), 2.0f)) {
            if (player.getStreetReputation().isNotorious()) {
                // Notorious players get no warning — police go straight to aggressive
                police.setState(NPCState.AGGRESSIVE);
                police.setSpeechText("I know you. You're coming with me!", 3.0f);
            } else {
                police.setState(NPCState.WARNING);
                police.setSpeechText("Move along, nothing to see here.", 3.0f);
                policeWarningTimers.put(police, 0.0f);
            }

            // Trigger first police encounter tooltip
            if (tooltipSystem != null) {
                tooltipSystem.trigger(TooltipTrigger.FIRST_POLICE_ENCOUNTER);
            }
            return;
        }

        // Scan for player structures around police (use cached result to avoid expensive scan every frame)
        Vector3 structure = policeTargetStructures.get(police);
        if (structure == null && npcStructureScanTimer < 0.05f) {
            structure = scanForStructures(world, police.getPosition(), 20); // radius 20, not 40
        }

        if (structure != null) {
            // Found a structure - investigate
            policeTargetStructures.put(police, structure);
            setNPCTarget(police, structure, world);

            // If police reaches the structure, apply tape immediately
            if (police.isNear(structure, 3.0f)) {
                applyPoliceTapeToStructure(world, structure);
            }
        } else if (player.getStreetReputation().isKnown() || alertedPoliceNPCs.contains(police)) {
            // KNOWN/NOTORIOUS players or explicitly alerted officers — use cone+hearing detection
            boolean detected = isPlayerDetected(world, police, player);
            if (detected) {
                setNPCTarget(police, player.getPosition(), world);
            } else {
                // Check hearing-only detection → SUSPICIOUS
                boolean heard = isPlayerHeard(police, player);
                if (heard) {
                    police.setState(NPCState.SUSPICIOUS);
                    police.setSpeechText("What was that?", 3.0f);
                    policeSuspiciousTimers.put(police, 0.0f);
                    return;
                }
                // Otherwise patrol randomly
                updateWandering(police, delta, world);
            }
        } else {
            // Issue #689: Innocent NOBODY-rep players detected purely by cone+hearing stealth model
            boolean detected = isPlayerDetected(world, police, player);
            if (detected) {
                // Spotted — treat like KNOWN escalation
                setNPCTarget(police, player.getPosition(), world);
            } else {
                boolean heard = isPlayerHeard(police, player);
                if (heard) {
                    police.setState(NPCState.SUSPICIOUS);
                    police.setSpeechText("What was that?", 3.0f);
                    policeSuspiciousTimers.put(police, 0.0f);
                    return;
                }
                // Player is innocent and undetected — patrol randomly
                updateWandering(police, delta, world);
            }
        }
    }

    /**
     * Update police in SUSPICIOUS state — heard the player but hasn't seen them.
     * If player goes quiet and breaks LoS within 5s, NPC returns to PATROL ("Must've been nothing").
     */
    private void updatePoliceSuspicious(NPC police, float delta, World world, Player player) {
        // If player is sheltered, cancel suspicion
        if (ShelterDetector.isSheltered(world, player.getPosition())) {
            police.setState(NPCState.PATROLLING);
            policeSuspiciousTimers.remove(police);
            return;
        }

        // Check if we can now see the player — escalate to WARNING
        if (isPlayerDetected(world, police, player)) {
            police.setState(NPCState.WARNING);
            police.setSpeechText("Oi! Stay right there!", 3.0f);
            policeWarningTimers.put(police, 0.0f);
            policeSuspiciousTimers.remove(police);
            return;
        }

        // Check contact
        if (police.isNear(player.getPosition(), 2.0f)) {
            police.setState(NPCState.WARNING);
            police.setSpeechText("Move along, nothing to see here.", 3.0f);
            policeWarningTimers.put(police, 0.0f);
            policeSuspiciousTimers.remove(police);
            return;
        }

        // Tick suspicious timer
        float timer = policeSuspiciousTimers.getOrDefault(police, 0.0f) + delta;
        policeSuspiciousTimers.put(police, timer);

        // Check if player has gone quiet (noise < 0.3) — allow escape
        boolean playerGoneQuiet = player.getNoiseLevel() < 0.3f;
        boolean hasLoS = hasLineOfSight(world, police.getPosition(), player.getPosition());

        if (timer >= SUSPICIOUS_TIMEOUT || (playerGoneQuiet && !hasLoS)) {
            // Player escaped detection — return to patrol
            police.setState(NPCState.PATROLLING);
            police.setSpeechText("Must've been nothing.", 3.0f);
            policeSuspiciousTimers.remove(police);
            return;
        }

        // Move toward last known noise source (player position) to investigate
        setNPCTarget(police, player.getPosition(), world);
    }

    /**
     * Check if the police officer can see the player in their vision cone.
     * Issue #689: Vision cone = 70° half-angle, 20 blocks range (halved at night),
     * blocked by solid non-glass blocks via DDA raycast.
     * BALACLAVA reduces cone range by 30% (20→14 blocks).
     * HIGH_VIS_JACKET reduces cone angle by 20°.
     */
    private boolean isPlayerDetected(World world, NPC police, Player player) {
        boolean isNightTime = isNight(currentGameTime);

        // Calculate effective vision range
        float visionRange = VISION_RANGE;
        if (isNightTime) {
            visionRange *= NIGHT_VISION_MULTIPLIER; // halved at night
        }

        // BALACLAVA reduces vision range by 30%
        if (player.isBalaclavWorn()) {
            visionRange *= 0.7f;
        }

        float dist = police.getPosition().dst(player.getPosition());
        if (dist > visionRange) {
            return false; // Out of vision range
        }

        // Vision cone check: is player within the angle of police's facing direction?
        float halfAngle = VISION_CONE_HALF_ANGLE_DEG;
        // HIGH_VIS_JACKET + crouching: reduce police cone angle by 20°
        if (currentInventory != null && currentInventory.getItemCount(Material.HIGH_VIS_JACKET) > 0
                && player.isCrouching()) {
            halfAngle -= 20f;
        }

        // Police facing direction: NPC facingAngle is in degrees (0=+Z, 90=+X)
        float dx = player.getPosition().x - police.getPosition().x;
        float dz = player.getPosition().z - police.getPosition().z;

        // Angle from police to player, in degrees (0=+Z, 90=+X) to match NPC convention
        float toPlayerDeg = (float) Math.toDegrees(Math.atan2(dx, dz));
        float policeFacingDeg = police.getFacingAngle();

        float angleDiffDeg = Math.abs(normalizeDeg(toPlayerDeg - policeFacingDeg));

        if (angleDiffDeg > halfAngle) {
            return false; // Player outside vision cone
        }

        // Line-of-sight check: solid blocks block vision
        return hasLineOfSight(world, police.getPosition(), player.getPosition());
    }

    /**
     * Check if the police officer can hear the player.
     * Issue #689: Hearing range = 5 + (noiseLevel × 15), 360°, +25% at night.
     */
    private boolean isPlayerHeard(NPC police, Player player) {
        float noiseLevel = player.getNoiseLevel();
        float hearingRange = NoiseSystem.getHearingRange(noiseLevel);

        // Night multiplier
        if (isNight(currentGameTime)) {
            hearingRange *= NIGHT_HEARING_MULTIPLIER;
        }

        float dist = police.getPosition().dst(player.getPosition());
        return dist <= hearingRange;
    }

    /**
     * Returns true if the game time is within the night window (22:00–06:00).
     */
    private static boolean isNight(float hours) {
        return hours >= 22.0f || hours < 6.0f;
    }

    /**
     * Normalize a degree angle to the range [-180, 180].
     */
    private static float normalizeDeg(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }

    /**
     * Update police warning behavior.
     */
    private void updatePoliceWarning(NPC police, float delta, World world, Player player) {
        // If player ducks into a shelter mid-warning, cancel the warning
        if (ShelterDetector.isSheltered(world, player.getPosition())) {
            police.setState(NPCState.PATROLLING);
            policeWarningTimers.remove(police);
            return;
        }

        // Increment warning timer
        float timer = policeWarningTimers.getOrDefault(police, 0.0f);
        timer += delta;
        policeWarningTimers.put(police, timer);

        // Check if player is near a structure
        Vector3 targetStructure = policeTargetStructures.get(police);

        // If no target structure, scan for one near the police/player (throttled)
        if (targetStructure == null && npcStructureScanTimer < 0.05f) {
            targetStructure = scanForStructures(world, police.getPosition(), 20);
            if (targetStructure == null) {
                targetStructure = scanForStructures(world, player.getPosition(), 20);
            }
            if (targetStructure != null) {
                policeTargetStructures.put(police, targetStructure);
            }
        }

        boolean playerNearStructure = false;
        if (targetStructure != null) {
            playerNearStructure = player.getPosition().dst(targetStructure) < 10.0f;
        }

        // Fix #687: HIGH_VIS_JACKET delays police escalation to 12 seconds instead of 2
        boolean hasHighVis = currentInventory != null
                && currentInventory.getItemCount(Material.HIGH_VIS_JACKET) > 0;
        float escalateThreshold = hasHighVis ? 12.0f : 2.0f;
        float patrolRevertThreshold = hasHighVis ? 14.0f : 3.0f;

        // Escalate after threshold if player stays near structure
        if (timer >= escalateThreshold && playerNearStructure) {
            police.setState(NPCState.AGGRESSIVE);
            police.setSpeechText("Right, you're nicked!", 2.0f);

            // Apply police tape to structure
            if (targetStructure != null) {
                applyPoliceTapeToStructure(world, targetStructure);
            }

            // Spawn additional police
            NPC extraPolice = spawnNPC(NPCType.POLICE, police.getPosition().x + 3, police.getPosition().y, police.getPosition().z);
            if (extraPolice != null) extraPolice.setState(NPCState.AGGRESSIVE);
        } else if (timer >= patrolRevertThreshold) {
            // Go back to patrolling after warning expires
            police.setState(NPCState.PATROLLING);
            policeWarningTimers.remove(police);
        }
    }

    /**
     * Update police aggressive/arresting behavior.
     * When police closes in, signals arrest to the game loop via arrestPending flag.
     * The game loop applies inventory confiscation and health/hunger penalties via ArrestSystem.
     */
    private void updatePoliceAggressive(NPC police, float delta, World world, Player player) {
        // If player is sheltered, police back off and resume patrolling
        if (ShelterDetector.isSheltered(world, player.getPosition())) {
            police.setState(NPCState.PATROLLING);
            policeLostSightTimers.remove(police);
            return;
        }

        // Line-of-sight check: if the officer cannot see the player (blocked by solid blocks),
        // accumulate lost-sight time. After POLICE_LOST_SIGHT_TIMEOUT seconds the officer
        // gives up the chase and returns to PATROLLING.
        if (!hasLineOfSight(world, police.getPosition(), player.getPosition())) {
            float lostTime = policeLostSightTimers.getOrDefault(police, 0.0f) + delta;
            policeLostSightTimers.put(police, lostTime);
            if (lostTime >= POLICE_LOST_SIGHT_TIMEOUT) {
                police.setState(NPCState.PATROLLING);
                policeLostSightTimers.remove(police);
                return;
            }
            // Keep moving toward last known position even while sight is lost
        } else {
            // Player is visible — reset the lost-sight timer
            policeLostSightTimers.remove(police);
        }

        // Move toward player
        setNPCTarget(police, player.getPosition(), world);

        // If very close, make the arrest — signal game loop.
        // Transition to PATROLLING immediately so the police does not keep chasing;
        // the game loop handles the arrest via isArrestPending()/clearArrestPending().
        // Issue #218: Guard against re-arrest during the post-arrest grace period
        if (police.isNear(player.getPosition(), 1.5f) && !arrestPending && postArrestCooldown <= 0) {
            arrestPending = true;
            police.setSpeechText("You're coming with me!", 2.0f);
            police.setState(NPCState.PATROLLING);
            policeLostSightTimers.remove(police);
        }
    }

    /**
     * Check whether a police officer has an unobstructed line of sight to the player.
     * Uses a 3D DDA ray march through the voxel grid from the officer's eye position
     * to the player's eye position. Returns false if any solid, non-transparent block
     * intersects the ray.
     */
    public static boolean hasLineOfSight(World world, Vector3 from, Vector3 to) {
        // Eye-level offsets (NPCs/player are approximately 1 block tall)
        float eyeHeight = 1.0f;
        float fx = from.x, fy = from.y + eyeHeight, fz = from.z;
        float tx = to.x,   ty = to.y   + eyeHeight, tz = to.z;

        float dx = tx - fx;
        float dy = ty - fy;
        float dz = tz - fz;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.001f) return true; // Same position — trivially in sight

        // Normalise direction
        float ndx = dx / dist;
        float ndy = dy / dist;
        float ndz = dz / dist;

        // Step through blocks along the ray using small steps (0.4 block increments)
        float step = 0.4f;
        int steps = (int) (dist / step) + 1;
        for (int i = 1; i <= steps; i++) {
            float t = Math.min(i * step, dist);
            int bx = (int) Math.floor(fx + ndx * t);
            int by = (int) Math.floor(fy + ndy * t);
            int bz = (int) Math.floor(fz + ndz * t);

            // Skip the exact start/end blocks
            if (bx == (int) Math.floor(fx) && by == (int) Math.floor(fy) && bz == (int) Math.floor(fz)) continue;
            if (bx == (int) Math.floor(tx) && by == (int) Math.floor(ty) && bz == (int) Math.floor(tz)) break;

            BlockType block = world.getBlock(bx, by, bz);
            // Glass and air don't block LOS; all other solid blocks do
            if (block.isSolid() && block != BlockType.GLASS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether police have caught the player this frame.
     * The game loop should call this, apply ArrestSystem.arrest(), then clearArrestPending().
     */
    public boolean isArrestPending() {
        return arrestPending;
    }

    /**
     * Clear the arrest-pending flag after the game loop has handled it.
     * Also starts the post-arrest cooldown (Issue #218) to prevent immediate re-arrest.
     */
    public void clearArrestPending() {
        arrestPending = false;
        postArrestCooldown = POST_ARREST_COOLDOWN_DURATION;
    }

    /**
     * Returns the remaining post-arrest cooldown in seconds (Issue #218).
     * Zero means the player can be arrested again.
     */
    public float getPostArrestCooldown() {
        return postArrestCooldown;
    }

    /**
     * Scan for player-built structures around a position.
     * @return position of structure center, or null if none found
     */
    private Vector3 scanForStructures(World world, Vector3 center, int radius) {
        int playerBlockCount = 0;
        Vector3 structureCenter = null;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = (int) (center.x + dx);
                int z = (int) (center.z + dz);

                for (int y = 1; y < 10; y++) {
                    BlockType block = world.getBlock(x, y, z);
                    // Check for player-placed blocks; the >= 5 threshold mitigates false
                    // positives from incidental world-generated BRICK blocks
                    if (block == BlockType.WOOD || block == BlockType.BRICK
                        || block == BlockType.STONE || block == BlockType.GLASS
                        || block == BlockType.CARDBOARD) {
                        playerBlockCount++;
                        if (structureCenter == null) {
                            structureCenter = new Vector3(x, y, z);
                        }
                    }
                }
            }
        }

        // Need at least 5 blocks to be considered a structure
        if (playerBlockCount >= 5 && structureCenter != null) {
            return structureCenter;
        }

        return null;
    }

    /**
     * Apply police tape to blocks in a structure.
     */
    private void applyPoliceTapeToStructure(World world, Vector3 structureCenter) {
        // Tape a few blocks around the structure center
        int tapeRadius = 2;
        int tapedCount = 0;

        for (int dx = -tapeRadius; dx <= tapeRadius && tapedCount < 5; dx++) {
            for (int dz = -tapeRadius; dz <= tapeRadius && tapedCount < 5; dz++) {
                for (int dy = 0; dy < 3 && tapedCount < 5; dy++) {
                    int x = (int) structureCenter.x + dx;
                    int y = (int) structureCenter.y + dy;
                    int z = (int) structureCenter.z + dz;

                    BlockType block = world.getBlock(x, y, z);
                    if (block == BlockType.WOOD || block == BlockType.BRICK
                        || block == BlockType.STONE || block == BlockType.GLASS
                        || block == BlockType.CARDBOARD) {
                        world.addPoliceTape(x, y, z);
                        tapedCount++;
                    }
                }
            }
        }
    }

    // ========== Phase 7: Council Builder System ==========

    /**
     * Update council builders based on detected structures.
     */
    private void updateCouncilBuilders(World world, TooltipSystem tooltipSystem) {
        List<StructureTracker.Structure> largeStructures = structureTracker.getLargeStructures();

        for (StructureTracker.Structure structure : largeStructures) {
            int requiredBuilders = structureTracker.calculateBuilderCount(structure);
            Vector3 structureCenter = structure.getCenter();

            // Check if this structure already has a notice (by position).
            // Use a "x,y,z" string key to avoid Vector3 identity-equality pitfalls —
            // LibGDX's Vector3 does not override hashCode()/equals(), so two Vector3
            // instances with the same coordinates are NOT equal under HashSet.contains().
            String structureKey = (int)structureCenter.x + "," + (int)structureCenter.y + "," + (int)structureCenter.z;
            boolean alreadyNotified = notifiedStructures.contains(structureKey);

            // Add planning notice after structure is detected (first time only)
            if (!alreadyNotified && requiredBuilders > 0) {
                applyPlanningNotice(world, structure);
                notifiedStructures.add(structureKey);
                structure.setHasNotice(true);
            } else if (alreadyNotified) {
                structure.setHasNotice(true); // Mark as having notice
            }

            int currentBuilders = structureBuilderCount.getOrDefault(structureKey, 0);

            // Spawn builders after planning notice has been up for a bit
            // Only spawn if structure has notice
            if (structure.hasNotice() && currentBuilders < requiredBuilders) {
                spawnCouncilBuilder(structure, world);
                structureBuilderCount.put(structureKey, currentBuilders + 1);
            }
        }
    }

    /**
     * Spawn a council builder to demolish a structure.
     */
    private void spawnCouncilBuilder(StructureTracker.Structure structure, World world) {
        Vector3 center = structure.getCenter();

        // Spawn builder 10-20 blocks away from structure
        float angle = random.nextFloat() * (float) Math.PI * 2;
        float distance = 10 + random.nextFloat() * 10;

        float x = center.x + (float) Math.cos(angle) * distance;
        float z = center.z + (float) Math.sin(angle) * distance;
        float y = findGroundHeight(world, x, z);

        NPC builder = spawnNPC(NPCType.COUNCIL_BUILDER, x, y, z);
        if (builder == null) return;
        builder.setState(NPCState.IDLE);
        builderTargets.put(builder, structure);
        builderDemolishTimers.put(builder, 0.0f);
    }

    /**
     * Apply planning notice to a structure.
     */
    private void applyPlanningNotice(World world, StructureTracker.Structure structure) {
        // Add planning notice to a few blocks on the structure
        Set<Vector3> blocks = structure.getBlocks();
        int noticeCount = 0;

        for (Vector3 block : blocks) {
            if (noticeCount >= 3) {
                break; // Only add notices to 3 blocks
            }
            world.addPlanningNotice((int)block.x, (int)block.y, (int)block.z);
            noticeCount++;
        }
    }

    /**
     * Update a council builder's behavior.
     */
    private void updateCouncilBuilder(NPC builder, float delta, World world, TooltipSystem tooltipSystem) {
        // Update knockback timer
        Float knockbackTimer = builderKnockbackTimers.get(builder);
        if (knockbackTimer != null && knockbackTimer > 0) {
            builderKnockbackTimers.put(builder, knockbackTimer - delta);
            if (knockbackTimer - delta <= 0) {
                builder.setState(NPCState.IDLE); // Return to normal
            }
            builder.setVelocity(0, 0, 0);
            return; // Don't demolish while knocked back
        }

        StructureTracker.Structure target = builderTargets.get(builder);
        if (target == null || target.isEmpty()) {
            // No target or structure demolished - mark builder dead so removeIf at the
            // top of update() handles cleanup next frame. Direct npcs.remove() here would
            // shift the indexed loop's elements and silently skip the next NPC (#120).
            builder.takeDamage(Float.MAX_VALUE);
            return;
        }

        Vector3 targetPos = target.getCenter();

        // Use 2D (XZ-plane) distance to check proximity — builders walk on the ground
        // so the Y component of the structure centre (mid-height) would inflate the 3D
        // distance and prevent the builder from ever triggering demolition when standing
        // next to a tall structure.  The proximity threshold is 4.0 XZ blocks so the
        // builder triggers demolition while standing adjacent to the exterior wall of
        // even a large structure (e.g. a 5-block-wide structure has walls ~2.5 blocks
        // from its centre).
        Vector3 builderPos = builder.getPosition();
        float dx = builderPos.x - targetPos.x;
        float dz = builderPos.z - targetPos.z;
        float distXZ = (float) Math.sqrt(dx * dx + dz * dz);

        // Move toward structure
        if (distXZ > 4.0f) {
            // Walk directly toward the structure centre in XZ.  We skip pathfinding and
            // set the target at the structure centre adjusted to the builder's own Y level,
            // so the builder advances steadily toward the wall without being rerouted up
            // and over the structure.  Collision stops the builder at the exterior wall,
            // which is fine — stuck detection is disabled for builders so they persist.
            Vector3 groundLevelTarget = new Vector3(targetPos.x, builderPos.y, targetPos.z);
            // Clear any stale path so followPath is not used (it would clear targetPosition
            // when done and leave the builder with no goal between frames).
            builder.setPath(null);
            builder.setTargetPosition(groundLevelTarget);
        } else {
            // Adjacent to structure - start demolishing
            builder.setState(NPCState.DEMOLISHING);
            builder.setVelocity(0, 0, 0);

            // Demolish blocks periodically
            float demolishTimer = builderDemolishTimers.getOrDefault(builder, 0.0f);
            demolishTimer += delta;
            builderDemolishTimers.put(builder, demolishTimer);

            if (demolishTimer >= 1.0f) { // Demolish one block per second
                demolishBlock(world, target, tooltipSystem);
                builderDemolishTimers.put(builder, 0.0f);
            }
        }
    }

    /**
     * Demolish a block from a structure.
     */
    private void demolishBlock(World world, StructureTracker.Structure structure, TooltipSystem tooltipSystem) {
        if (structure.getBlocks().isEmpty()) {
            return;
        }

        // Pick a random block from the structure
        List<Vector3> blockList = new ArrayList<>(structure.getBlocks());
        Vector3 blockToRemove = blockList.get(random.nextInt(blockList.size()));

        int x = (int) blockToRemove.x;
        int y = (int) blockToRemove.y;
        int z = (int) blockToRemove.z;

        // Remove the block
        world.setBlock(x, y, z, BlockType.AIR);
        world.markBlockDirty(x, y, z);  // Trigger mesh rebuild so demolished block disappears visually
        structure.removeBlock(blockToRemove);
        structureTracker.removeBlock(x, y, z);

        // Clear any stale hit counter so a newly-placed block at this position
        // starts fresh and requires the full number of hits to break.
        if (blockBreaker != null) {
            blockBreaker.clearHits(x, y, z);
        }

        // Remove police tape protection if present
        world.removePoliceTape(x, y, z);

        // Remove planning notice if present
        world.removePlanningNotice(x, y, z);

        // Trigger tooltip on first demolition
        if (tooltipSystem != null) {
            tooltipSystem.trigger(TooltipTrigger.FIRST_COUNCIL_ENCOUNTER);
        }
    }

    /**
     * Handle punching a council builder - applies knockback and delays demolition.
     */
    public void punchCouncilBuilder(NPC builder) {
        if (builder.getType() != NPCType.COUNCIL_BUILDER) {
            return;
        }

        // Set knockback timer (delays demolition for 1 second)
        builderKnockbackTimers.put(builder, 1.0f);
        builder.setState(NPCState.KNOCKED_BACK);
    }

    /**
     * Set the BlockBreaker so demolishBlock() can clear stale hit counters.
     */
    public void setBlockBreaker(BlockBreaker blockBreaker) {
        this.blockBreaker = blockBreaker;
    }

    /**
     * Get structure tracker (for testing).
     */
    public StructureTracker getStructureTracker() {
        return structureTracker;
    }

    /**
     * Force an immediate structure scan (for testing).
     * In production, scans happen automatically every 30 seconds.
     */
    public void forceStructureScan(World world, TooltipSystem tooltipSystem) {
        structureTracker.scanForStructures(world);
        updateCouncilBuilders(world, tooltipSystem);
    }

    /**
     * Force police tape to be applied to a structure at the given center (for testing).
     */
    public void forceApplyPoliceTape(World world, Vector3 structureCenter) {
        applyPoliceTapeToStructure(world, structureCenter);
    }

    /**
     * Force a structure check for a specific NPC (for testing Issue #910).
     * Resets the per-NPC check timer so the check runs immediately.
     */
    public void forceCheckForPlayerStructures(NPC npc, World world) {
        npcStructureCheckTimers.put(npc, 0f);
        checkForPlayerStructures(npc, world);
    }

    /**
     * Force structure reaction update for a specific NPC (for testing Issue #910).
     * Simulates the NPC being at the structure and triggering speech.
     */
    public void forceUpdateReactingToStructure(NPC npc, float delta, World world) {
        updateReactingToStructure(npc, delta, world);
    }
}

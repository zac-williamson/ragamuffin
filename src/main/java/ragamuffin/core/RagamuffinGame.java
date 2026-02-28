package ragamuffin.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import java.util.List;
import ragamuffin.ai.GangTerritorySystem;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.*;
import ragamuffin.entity.Car;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.ChunkRenderer;
import ragamuffin.render.FirstPersonArm;
import ragamuffin.render.NPCRenderer;
import ragamuffin.ui.*;
import ragamuffin.world.*;

/**
 * Main game class - handles the 3D core engine and game systems.
 */
public class RagamuffinGame extends ApplicationAdapter {

    private GameState state;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private InputHandler inputHandler;

    private Player player;
    private World world;
    private ChunkRenderer chunkRenderer;
    private ChunkMeshBuilder meshBuilder;

    // Phase 3: Resource & Inventory System
    private Inventory inventory;
    private BlockBreaker blockBreaker;
    private PropBreaker propBreaker;
    private BlockDropTable dropTable;
    private TooltipSystem tooltipSystem;

    // Phase 4: Crafting & Building
    private CraftingSystem craftingSystem;
    private BlockPlacer blockPlacer;

    // Phase 5: NPC System & AI
    private NPCManager npcManager;

    // Phase 6: Day/Night Cycle & Police
    private TimeSystem timeSystem;
    private LightingSystem lightingSystem;
    private ClockHUD clockHUD;

    // UI
    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private InventoryUI inventoryUI;
    private HelpUI helpUI;
    private HotbarUI hotbarUI;
    private CraftingUI craftingUI;

    // Phase 8: HUD, Menus & Sequences
    private GameHUD gameHUD;
    private float prevDamageFlashIntensity; // Used to detect new damage events for HUD
    private PauseMenu pauseMenu;
    private MainMenuScreen mainMenuScreen;
    private OpeningSequence openingSequence;

    // Issue #373: Opening cinematic cut-scene (city fly-through)
    private ragamuffin.ui.CinematicCamera cinematicCamera;

    // Phase 11: CRITIC 1 Improvements
    private InteractionSystem interactionSystem;
    private HealingSystem healingSystem;
    private RespawnSystem respawnSystem;

    // Issue #541: track the shopkeeper whose shop menu is currently open so that
    // 1/2/3 key presses can be routed to selectShopItem() instead of hotbar selection.
    private ragamuffin.entity.NPC activeShopkeeperNPC = null;

    // Issue #733: Fence trade menu — currency trading with the Fence NPC
    private FenceSystem fenceSystem;
    private ragamuffin.ui.FenceTradeUI fenceTradeUI;

    // Phase 12: CRITIC 2 Improvements
    private WeatherSystem weatherSystem;

    // Issue #842: WeatherNPCBehaviour — weather-driven NPC state changes
    private WeatherNPCBehaviour weatherNPCBehaviour;
    private float weatherNPCTimer = 0f;
    private final java.util.Random frostSlipRng = new java.util.Random();

    // Issue #807: WarmthSystem — hypothermia/wetness survival mechanics
    private WarmthSystem warmthSystem;

    // Issue #813: CampfireSystem — warmth from campfires, rain extinguishing, flicker-light
    private CampfireSystem campfireSystem;

    // Issue #807: NoiseSystem — player noise level for NPC hearing detection
    private NoiseSystem noiseSystem;

    // Issue #234: Exposure system — shelter protection from weather effects
    private ExposureSystem exposureSystem;

    // CRITIC 5: Arrest system — applies penalties when police catch the player
    private ArrestSystem arrestSystem;

    // CRITIC 3: Greggs Raid mechanic — dedicated system for raid state management
    private GreggsRaidSystem greggsRaidSystem;

    // Issue #26: Gang Territory System
    private GangTerritorySystem gangTerritorySystem;

    // Hover tooltip system
    private HoverTooltipSystem hoverTooltipSystem;

    // Issue #13: on-screen NPC speech log
    private SpeechLogUI speechLogUI;

    // NPC rendering
    private NPCRenderer npcRenderer;

    // Issue #171: Particle effects for combat and movement
    private ragamuffin.render.ParticleSystem particleSystem;

    // Issue #10: Building signage renderer
    private ragamuffin.render.SignageRenderer signageRenderer;

    // First-person arm
    private FirstPersonArm firstPersonArm;

    // Audio system
    private ragamuffin.audio.SoundSystem soundSystem;

    // Issue #209: Sky renderer — sun and clouds
    private ragamuffin.render.SkyRenderer skyRenderer;

    // Issue #658: Animated flag renderer
    private ragamuffin.render.FlagRenderer flagRenderer;

    // Issue #669: Non-block-based 3D prop renderer
    private ragamuffin.render.PropRenderer propRenderer;

    // Issue #675: Small 3D object renderer — renders placed small items in the world
    private ragamuffin.render.SmallItemRenderer smallItemRenderer;

    // Issue #450: Achievement system
    private ragamuffin.ui.AchievementSystem achievementSystem;
    private ragamuffin.ui.AchievementsUI achievementsUI;

    // Issue #464: Quest log UI
    private ragamuffin.ui.QuestLogUI questLogUI;
    // Issue #497: Quest tracker UI (always-visible compact panel)
    private ragamuffin.ui.QuestTrackerUI questTrackerUI;

    // Issue #659: Criminal record log
    private ragamuffin.ui.CriminalRecordUI criminalRecordUI;

    // Issue #850: Skills UI overlay
    private ragamuffin.ui.SkillsUI skillsUI;

    // Issue #803: Wanted & Notoriety systems — police pursuit and persistent criminal reputation
    private WantedSystem wantedSystem;
    private NotorietySystem notorietySystem;
    // Issue #816: Neighbourhood Watch System — community uprising in response to visible crimes
    private NeighbourhoodWatchSystem neighbourhoodWatchSystem;
    // Issue #803: Rumour network — NPC gossip spreading police tips on witnessed crimes
    private RumourNetwork rumourNetwork;
    // Issue #811: Faction system — three-faction turf-war engine (Marchetti Crew, Street Lads, The Council)
    private FactionSystem factionSystem;
    // Issue #818: Disguise system — player can loot and wear NPC clothing to infiltrate restricted areas
    private DisguiseSystem disguiseSystem;
    // Issue #781: Graffiti system — territorial marking, turf pressure, NPC crew spraying
    private GraffitiSystem graffitiSystem;
    // Issue #781: Graffiti renderer — renders graffiti marks as depth-offset quads on block surfaces
    private ragamuffin.render.GraffitiRenderer graffitiRenderer;
    // Issue #824: Street economy system — NPC needs, black market dealing, protection rackets
    private StreetEconomySystem streetEconomySystem;
    // Fix #899: Cooldown timer (seconds) before the next random market event fires.
    // Initialised to a random value in [120, 300] on game start and reset after each event.
    private float marketEventCooldown = 180f;
    // Issue #826: Witness & evidence system — witnesses, CCTV tapes, informant mechanic
    private WitnessSystem witnessSystem;
    // Issue #828: JobCentre system — Universal Credit, sanctions & bureaucratic torment
    private NewspaperSystem newspaperSystem;
    private JobCentreSystem jobCentreSystem;
    private ragamuffin.ui.JobCentreUI jobCentreUI;

    // Issue #830: Boot Sale system — underground auction economy
    private FenceValuationTable fenceValuationTable;
    private BootSaleSystem bootSaleSystem;
    private ragamuffin.ui.BootSaleUI bootSaleUI;

    // Issue #832: Property system — slumlord economy (buildings, decay, passive income, council rates)
    private PropertySystem propertySystem;

    // Issue #799: Corner shop economy — shop claiming, customer traffic, Marchetti rivalry, police raid heat
    private CornerShopSystem cornerShopSystem;

    // Issue #837: Market stall economy — passive income, inspector, weather, faction modifiers
    private StallSystem stallSystem;
    // Issue #783: Pirate FM — underground radio station
    private PirateRadioSystem pirateRadioSystem;

    // Issue #704 / #844: HeistSystem — four-phase robbery mechanic (casing, planning, execution, fencing)
    private HeistSystem heistSystem;
    /** Previous time (hours) — used to detect when clock ticks past 06:00 for daily reset. */
    private float prevTimeForHeistReset = -1f;

    // Issue #793 / #846: NeighbourhoodSystem — dynamic decay, gentrification & Vibes state machine
    private NeighbourhoodSystem neighbourhoodSystem;

    // Issue #714 / #716 / #848: Squat, MC Battle & Rave systems — underground music scene
    private SquatSystem squatSystem;
    private MCBattleSystem mcBattleSystem;
    private RaveSystem raveSystem;

    // Issue #856: Championship Ladder — persistent ranked-fighter system
    private ChampionshipLadder championshipLadder;
    /** Last day index processed by championship ladder daily tick (prevents double-ticking). */
    private int championshipLastProcessedDay = -1;

    // Issue #852: Fruit machine mini-game — pub prop interaction
    private FruitMachine fruitMachine;
    /** Whether police have already been spawned for the current rave alert. */
    private boolean ravePoliceSpawned = false;

    // Issue #662: Car traffic system
    private ragamuffin.ai.CarManager carManager;
    // Issue #773: Car driving system — lets player enter and drive cars
    private CarDrivingSystem carDrivingSystem;
    // Issue #672: Car renderer — makes cars visible in-game
    private ragamuffin.render.CarRenderer carRenderer;
    private float distanceTravelledAchievement = 0f; // accumulated metres walked
    private com.badlogic.gdx.math.Vector3 lastPlayerPosForDistance = null;

    private static final float MOUSE_SENSITIVITY = 0.2f;
    private static final float PUNCH_REACH = 5.0f;
    private static final float PLACE_REACH = 5.0f;
    private static final float MAX_PITCH = 89.0f;
    private float cameraPitch = 0f;
    private float cameraYaw = 0f; // 0 = facing -Z

    /** Smooth chase camera angle (degrees) — lerps toward car heading for visual feedback. */
    private float chaseCameraAngle = 0f;

    // Reusable vectors to avoid per-frame allocation
    private final Vector3 tmpForward = new Vector3();
    private final Vector3 tmpRight = new Vector3();
    private final Vector3 tmpMoveDir = new Vector3();
    private final Vector3 tmpCameraPos = new Vector3();
    private final Vector3 tmpDirection = new Vector3();

    // Sky colour components (reused each frame)
    private float skyR = 0.53f, skyG = 0.81f, skyB = 0.92f;

    // Loading state — true until heavy init is complete
    private boolean loadingComplete = false;

    // Rain animation
    private float rainTimer = 0f;
    private final java.util.Random rainRng = new java.util.Random(42);

    // Issue #171: Footstep dust timer — emit a puff every FOOTSTEP_DUST_INTERVAL seconds of movement
    private float footstepDustTimer = 0f;
    private static final float FOOTSTEP_DUST_INTERVAL = 0.3f;

    // Issue #265: Hold-to-break repeat-fire timer
    private float punchHeldTimer = 0f;
    private static final float PUNCH_REPEAT_INTERVAL = 0.25f;
    private String lastPunchTargetKey = null; // reset timer when target block changes

    // Tool durability is now tracked per inventory slot via Inventory.getToolInSlot()

    // Death screen messages
    private static final String[] DEATH_MESSAGES = {
        "You died. On a council estate. How original.",
        "Game over. The JobCentre will miss you.",
        "Dead. Your parents were right to kick you out.",
        "You have perished. The local paper won't report it.",
        "Deceased. At least you won't need to pay council tax.",
        "You died. Nobody noticed.",
        "Game over. Even the pigeons are unimpressed.",
        "Dead as a Greggs after midnight.",
        "You shuffled off this mortal coil. In a car park.",
        "Expired. Like the milk in your cardboard shelter."
    };
    private String deathMessage = null;

    @Override
    public void create() {
        Gdx.app.log("Ragamuffin", "Welcome to the real world, kid.");

        // Start in LOADING state — heavy init deferred to first render frame
        // so the browser can paint the loading screen before freezing on world-gen.
        state = GameState.LOADING;

        // Setup 3D camera (lightweight)
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        // Setup rendering infrastructure (lightweight)
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Setup 2D UI rendering (lightweight)
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        // Fix #727: enable linear filtering so scaled-up menu text renders cleanly
        // instead of appearing pixelated/garbled when drawn at non-native sizes.
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.getData().setScale(1.2f);

        // Setup input (so user can interact with the loading screen)
        inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(inputHandler);
        Gdx.input.setCursorCatched(false);

        // Heavy world-gen + system init happens in initGame(), called from render()
        // after the loading screen has had at least one frame to paint.
    }

    /**
     * Heavy initialisation: world generation, chunk mesh building, NPC spawning,
     * and all game systems. Called once from render() after the loading screen
     * has had a frame to paint, so the browser doesn't appear frozen.
     */
    private void initGame() {
        // Generate the world (Phase 2)
        Gdx.app.log("Ragamuffin", "Generating British town...");
        world = new World(System.currentTimeMillis());
        world.generate();

        // Create player at the park (world center) - calculate spawn Y based on terrain
        float spawnY = calculateSpawnHeight(world, 0, 0) + 1.0f;
        player = new Player(0, spawnY, 0);
        camera.position.set(player.getPosition());
        camera.position.y += Player.EYE_HEIGHT;
        camera.lookAt(player.getPosition().x, player.getPosition().y + Player.EYE_HEIGHT,
                     player.getPosition().z - 1);
        camera.update();

        // Setup chunk rendering
        meshBuilder = new ChunkMeshBuilder();
        meshBuilder.setWorld(world);
        chunkRenderer = new ChunkRenderer();
        npcRenderer = new NPCRenderer();
        firstPersonArm = new FirstPersonArm();

        // Issue #10: Build sign list from all world landmarks
        signageRenderer = new ragamuffin.render.SignageRenderer();
        signageRenderer.buildFromLandmarks(world.getAllLandmarks());

        // Load initial chunks around player — meshes built lazily in render loop
        world.updateLoadedChunks(player.getPosition());
        // Build immediate chunks so the world isn't invisible on first frame
        int immediateCount = 0;
        for (Chunk chunk : world.getDirtyChunks()) {
            if (immediateCount >= 150) break; // More pre-built chunks for a visible world on start
            chunkRenderer.updateChunk(chunk, meshBuilder);
            world.markChunkClean(chunk);
            immediateCount++;
        }

        // Phase 3: Initialize inventory and resource systems
        inventory = new Inventory(36);
        inventory.addItem(ragamuffin.building.Material.TRANSMITTER_ITEM, 1);
        blockBreaker = new BlockBreaker();
        propBreaker = new PropBreaker();
        dropTable = new BlockDropTable();
        tooltipSystem = new TooltipSystem();

        // Phase 4: Initialize crafting and building systems
        craftingSystem = new CraftingSystem();
        blockPlacer = new BlockPlacer(blockBreaker);

        // Phase 5: Initialize NPC system
        npcManager = new NPCManager();
        npcManager.setBlockBreaker(blockBreaker);
        spawnInitialNPCs();
        // Fix #509: Initialize interactionSystem before spawnBuildingNPCs() so the live
        // quest registry is available when deciding which buildings get a quest-giver NPC.
        interactionSystem = new InteractionSystem();
        spawnBuildingNPCs(); // Issue #462: spawn static quest-giver NPCs inside buildings

        // Issue #733: Initialize Fence trade system and inject into interaction system
        fenceSystem = new FenceSystem();
        interactionSystem.setFenceSystem(fenceSystem);
        fenceTradeUI = new ragamuffin.ui.FenceTradeUI();

        // Phase 6: Initialize day/night cycle and lighting
        timeSystem = new TimeSystem(8.0f); // Start at 8:00 AM
        lightingSystem = new LightingSystem(environment);
        clockHUD = new ClockHUD();

        // Initialize UI
        inventoryUI = new InventoryUI(inventory);
        helpUI = new HelpUI();
        hotbarUI = new HotbarUI(inventory);
        craftingUI = new CraftingUI(craftingSystem, inventory);

        // Phase 8: Initialize HUD, menus, and sequences
        gameHUD = new GameHUD(player);
        pauseMenu = new PauseMenu();
        mainMenuScreen = new MainMenuScreen();
        openingSequence = new OpeningSequence();

        // Issue #373: Initialize cinematic camera for city fly-through
        cinematicCamera = new ragamuffin.ui.CinematicCamera();

        // Phase 11: Initialize CRITIC 1 systems (interactionSystem already created above)
        healingSystem = new HealingSystem();
        respawnSystem = new RespawnSystem();
        respawnSystem.setSpawnY(calculateSpawnHeight(world, 0, 0) + 1.0f);

        // Phase 12: Initialize CRITIC 2 systems
        weatherSystem = new WeatherSystem();

        // Issue #842: Initialize weather NPC behaviour system
        weatherNPCBehaviour = new WeatherNPCBehaviour(new java.util.Random());
        weatherNPCTimer = 0f;

        // Issue #807: Initialize warmth/wetness and noise systems
        warmthSystem = new WarmthSystem();
        noiseSystem = new NoiseSystem();

        // Issue #813: Initialize campfire system
        campfireSystem = new CampfireSystem();

        // Issue #234: Initialize exposure system
        exposureSystem = new ExposureSystem();

        // CRITIC 5: Initialize arrest system
        arrestSystem = new ArrestSystem();
        arrestSystem.setRespawnY(calculateSpawnHeight(world, 0, 0) + 1.0f);

        // CRITIC 3: Initialize Greggs raid system
        greggsRaidSystem = new GreggsRaidSystem();

        // Issue #26: Initialize gang territory system and register territories
        gangTerritorySystem = new GangTerritorySystem();
        gangTerritorySystem.addTerritory("Bricky Estate", -50f, -30f, 20f);
        gangTerritorySystem.addTerritory("South Patch", -45f, -45f, 20f);

        // Initialize hover tooltip system
        hoverTooltipSystem = new HoverTooltipSystem();

        // Issue #13: Initialize NPC speech log
        speechLogUI = new SpeechLogUI();

        // Initialize audio system
        soundSystem = new ragamuffin.audio.SoundSystem();

        // Issue #171: Initialize particle system
        particleSystem = new ragamuffin.render.ParticleSystem();

        // Issue #209: Initialize sky renderer (sun and clouds)
        skyRenderer = new ragamuffin.render.SkyRenderer();

        // Issue #658: Initialize animated flag renderer
        flagRenderer = new ragamuffin.render.FlagRenderer();
        flagRenderer.setFlags(world.getFlagPositions());

        // Issue #669: Initialize non-block 3D prop renderer
        propRenderer = new ragamuffin.render.PropRenderer();
        propRenderer.setProps(world.getPropPositions());

        // Issue #675: Initialize small item renderer
        smallItemRenderer = new ragamuffin.render.SmallItemRenderer();
        smallItemRenderer.setItems(world.getSmallItems());

        // Wire up tooltip sound effect
        tooltipSystem.setOnTooltipShow(() -> soundSystem.play(ragamuffin.audio.SoundEffect.TOOLTIP));

        // Issue #450: Initialize achievement system
        achievementSystem = new ragamuffin.ui.AchievementSystem();
        achievementsUI = new ragamuffin.ui.AchievementsUI(achievementSystem);
        achievementSystem.setOnNotificationShow(() -> soundSystem.play(ragamuffin.audio.SoundEffect.TOOLTIP));
        distanceTravelledAchievement = 0f;
        lastPlayerPosForDistance = new com.badlogic.gdx.math.Vector3(player.getPosition());

        // Issue #464: Initialize quest log UI
        // Fix #523: Pass inventory so quest log can show remaining count
        questLogUI = new ragamuffin.ui.QuestLogUI(interactionSystem.getQuestRegistry(), inventory);
        // Issue #497: Initialize quest tracker UI (compact always-visible panel)
        // Fix #511: Pass inventory so tracker can show current/required counts
        questTrackerUI = new ragamuffin.ui.QuestTrackerUI(interactionSystem.getQuestRegistry(), inventory);

        // Issue #659: Initialize criminal record UI
        criminalRecordUI = new ragamuffin.ui.CriminalRecordUI(player.getCriminalRecord());

        // Issue #850: Initialize skills UI
        skillsUI = new ragamuffin.ui.SkillsUI(player.getStreetSkillSystem());

        // Issue #803: Initialize wanted and notoriety systems — police pursuit & criminal reputation
        wantedSystem = new WantedSystem();
        notorietySystem = new NotorietySystem();
        gameHUD.setNotorietySystem(notorietySystem);
        // Issue #816: Initialize neighbourhood watch system — community uprising in response to player crimes
        neighbourhoodWatchSystem = new NeighbourhoodWatchSystem();
        gameHUD.setNeighbourhoodWatchSystem(neighbourhoodWatchSystem);
        // Issue #803: Initialize rumour network for spreading police tips and NPC gossip
        rumourNetwork = new RumourNetwork(new java.util.Random());
        // Issue #811: Initialize faction system — three-faction turf-war engine
        factionSystem = new FactionSystem(new TurfMap(), rumourNetwork);
        gameHUD.setFactionSystem(factionSystem);
        // Issue #818: Initialize disguise system — player can loot NPC clothing and wear it
        disguiseSystem = new DisguiseSystem();
        disguiseSystem.setAchievementSystem(achievementSystem);
        disguiseSystem.setRumourNetwork(rumourNetwork);
        gameHUD.setDisguiseSystem(disguiseSystem);
        // Issue #781: Initialize graffiti system and renderer — territorial marking and turf-war
        graffitiSystem = new GraffitiSystem();
        graffitiRenderer = new ragamuffin.render.GraffitiRenderer();
        // Issue #824: Initialize street economy system — NPC needs, black market, protection rackets
        streetEconomySystem = new StreetEconomySystem();
        // Fix #899: Randomise initial cooldown so events don't all fire at the same time after start
        marketEventCooldown = 120f + new java.util.Random().nextFloat() * 180f;

        // Issue #826: Initialize witness & evidence system — witnesses, CCTV tapes, informant mechanic
        witnessSystem = new WitnessSystem();
        witnessSystem.setCriminalRecord(player.getCriminalRecord());
        witnessSystem.setRumourNetwork(rumourNetwork);
        witnessSystem.setAchievementSystem(achievementSystem);

        // Issue #828: Initialize newspaper system (required by JobCentreSystem)
        newspaperSystem = new NewspaperSystem();
        // Issue #828: Initialize JobCentre system — Universal Credit sign-on loop
        jobCentreSystem = new JobCentreSystem(
                timeSystem,
                player.getCriminalRecord(),
                notorietySystem,
                factionSystem,
                rumourNetwork,
                newspaperSystem,
                player.getStreetSkillSystem(),
                wantedSystem,
                npcManager,
                new java.util.Random());
        jobCentreUI = new ragamuffin.ui.JobCentreUI();

        // Issue #830: Initialize FenceValuationTable and BootSaleSystem/UI
        fenceValuationTable = new FenceValuationTable();
        bootSaleSystem = new BootSaleSystem(
                timeSystem,
                fenceValuationTable,
                factionSystem,
                rumourNetwork,
                noiseSystem,
                notorietySystem,
                wantedSystem,
                player.getStreetSkillSystem(),
                new java.util.Random());
        bootSaleUI = new ragamuffin.ui.BootSaleUI(bootSaleSystem);
        // Set venue position from world landmark so police spawn at the right place
        ragamuffin.world.Landmark bootSaleLandmark = world.getLandmark(ragamuffin.world.LandmarkType.BOOT_SALE);
        if (bootSaleLandmark != null) {
            bootSaleSystem.setVenuePosition(
                    bootSaleLandmark.getPosition().x, 0, bootSaleLandmark.getPosition().z);
        }

        // Issue #832: Initialize property system — slumlord economy
        propertySystem = new PropertySystem(factionSystem, achievementSystem, rumourNetwork);

        // Issue #799: Initialize corner shop system — shop claiming, customer traffic, Marchetti rivalry
        cornerShopSystem = new CornerShopSystem();

        // Issue #837: Initialize stall system — market stall passive income economy
        stallSystem = new StallSystem();

        // Issue #783: Initialize pirate radio system — after faction and rumour systems are ready
        pirateRadioSystem = new PirateRadioSystem(new java.util.Random());
        pirateRadioSystem.setFactionSystem(factionSystem);
        pirateRadioSystem.setRumourNetwork(rumourNetwork);
        pirateRadioSystem.setNotorietySystem(notorietySystem);
        pirateRadioSystem.setWantedSystem(wantedSystem);
        pirateRadioSystem.setAchievementCallback(type -> achievementSystem.unlock(type));
        gameHUD.setPirateRadioSystem(pirateRadioSystem);

        // Issue #704 / #844: Initialize heist system — four-phase robbery mechanic
        heistSystem = new HeistSystem();
        prevTimeForHeistReset = timeSystem.getTime();

        // Issue #793 / #846: Initialize neighbourhood system — building decay, gentrification & Vibes
        neighbourhoodSystem = new NeighbourhoodSystem(
                factionSystem, factionSystem.getTurfMap(), rumourNetwork, achievementSystem,
                new java.util.Random());
        // Register all world landmarks so buildings are tracked from world-gen time
        for (ragamuffin.world.Landmark lm : world.getAllLandmarks()) {
            neighbourhoodSystem.registerBuilding(lm);
        }

        // Issue #714 / #716 / #848: Initialize squat, MC battle & rave systems
        squatSystem = new SquatSystem(notorietySystem, factionSystem, achievementSystem,
                rumourNetwork, new java.util.Random());
        mcBattleSystem = new MCBattleSystem(notorietySystem, factionSystem, achievementSystem,
                rumourNetwork, new java.util.Random());
        raveSystem = new RaveSystem(achievementSystem, notorietySystem, rumourNetwork);
        ravePoliceSpawned = false;
        // Issue #856: Initialize championship ladder and seed default fighters
        championshipLadder = new ChampionshipLadder();
        championshipLadder.initDefault();
        championshipLadder.registerFighter("Player");
        // Issue #852: Initialize fruit machine mini-game
        fruitMachine = new FruitMachine(new java.util.Random());
        gameHUD.setMcBattleSystem(mcBattleSystem);
        gameHUD.setRaveSystem(raveSystem);

        // Issue #662: Initialize car traffic system
        carManager = new ragamuffin.ai.CarManager();
        carManager.spawnInitialCars(world);
        // Issue #773: Initialize car driving system
        carDrivingSystem = new CarDrivingSystem(carManager);
        // Issue #804: Give driving system world reference for block collision
        carDrivingSystem.setWorld(world);
        // Issue #672: Initialize car renderer so cars are visible in-game
        carRenderer = new ragamuffin.render.CarRenderer();

        loadingComplete = true;
        state = GameState.MENU;
        Gdx.app.log("Ragamuffin", "Loading complete.");
    }

    /**
     * Calculate spawn height at given X,Z coordinates by finding the highest solid block.
     */
    private float calculateSpawnHeight(World world, int x, int z) {
        // Search upward from y=0 to find highest solid block
        for (int y = 64; y >= -10; y--) {
            BlockType block = world.getBlock(x, y, z);
            if (block.isSolid()) {
                return y + 1.0f; // Spawn one block above the solid block
            }
        }
        return 1.0f; // Default if no solid block found
    }

    /**
     * Spawn an NPC at terrain height for the given X,Z position.
     */
    private NPC spawnNPCAtTerrain(NPCType type, float x, float z) {
        float y = calculateSpawnHeight(world, (int) x, (int) z);
        return npcManager.spawnNPC(type, x, y, z);
    }

    /**
     * Spawn a named NPC at terrain height for the given X,Z position.
     */
    private NPC spawnNamedNPCAtTerrain(NPCType type, String name, float x, float z) {
        float y = calculateSpawnHeight(world, (int) x, (int) z);
        return npcManager.spawnNamedNPC(type, name, x, y, z);
    }

    /**
     * Spawn initial NPCs in the world.
     */
    private void spawnInitialNPCs() {
        // Park area — dog walkers with dogs, joggers
        spawnNPCAtTerrain(NPCType.PUBLIC, -5, 5);
        spawnNPCAtTerrain(NPCType.DOG, -2, 7);
        spawnNPCAtTerrain(NPCType.JOGGER, 5, -5);
        spawnNPCAtTerrain(NPCType.JOGGER, -8, -8);
        spawnNPCAtTerrain(NPCType.DOG, 10, 3);

        // Issue #708: Birds (pigeons) perching around the park and nearby streets
        spawnNPCAtTerrain(NPCType.BIRD, 3, 3);
        spawnNPCAtTerrain(NPCType.BIRD, -6, -4);
        spawnNPCAtTerrain(NPCType.BIRD, 8, 8);
        spawnNPCAtTerrain(NPCType.BIRD, -12, 6);
        spawnNPCAtTerrain(NPCType.BIRD, 15, -3);
        spawnNPCAtTerrain(NPCType.BIRD, 0, -8);
        spawnNPCAtTerrain(NPCType.BIRD, -4, 15);

        // High street — shoppers, shopkeepers, busker, postman
        spawnNPCAtTerrain(NPCType.PUBLIC, 35, 22);
        spawnNPCAtTerrain(NPCType.PUBLIC, 55, 20);
        spawnNPCAtTerrain(NPCType.SHOPKEEPER, 40, 18);
        spawnNPCAtTerrain(NPCType.SHOPKEEPER, 60, 22);
        spawnNPCAtTerrain(NPCType.BUSKER, 45, 25);
        spawnNPCAtTerrain(NPCType.POSTMAN, 30, 15);

        // Youth gang — small group lurking near the rough area
        spawnNPCAtTerrain(NPCType.YOUTH_GANG, -50, -30);
        spawnNPCAtTerrain(NPCType.YOUTH_GANG, -55, -35);
        spawnNPCAtTerrain(NPCType.YOUTH_GANG, -45, -28);

        // Council member near the JobCentre
        spawnNPCAtTerrain(NPCType.COUNCIL_MEMBER, -55, 28);

        // Drunk near the off-licence
        spawnNPCAtTerrain(NPCType.DRUNK, -65, 15);
        spawnNPCAtTerrain(NPCType.DRUNK, -60, 18);

        // Additional public wandering around the town
        spawnNPCAtTerrain(NPCType.PUBLIC, -20, 30);
        spawnNPCAtTerrain(NPCType.PUBLIC, 15, -10);
        spawnNPCAtTerrain(NPCType.PUBLIC, 0, 35);
        spawnNPCAtTerrain(NPCType.PUBLIC, -40, 10);

        // Pensioners near the community centre and church
        spawnNPCAtTerrain(NPCType.PENSIONER, -25, -40);
        spawnNPCAtTerrain(NPCType.PENSIONER, 25, 35);

        // Delivery driver rushing about
        spawnNPCAtTerrain(NPCType.DELIVERY_DRIVER, 70, 25);
        spawnNPCAtTerrain(NPCType.DELIVERY_DRIVER, -40, -15);

        // School kids near the primary school
        spawnNPCAtTerrain(NPCType.SCHOOL_KID, -30, -45);
        spawnNPCAtTerrain(NPCType.SCHOOL_KID, -28, -43);
        spawnNPCAtTerrain(NPCType.SCHOOL_KID, -32, -47);

        // Named NPCs — unique characters with distinctive appearances (Fix #639)
        // Brother Desmond: street preacher in deep purple robes near the park entrance
        spawnNamedNPCAtTerrain(NPCType.STREET_PREACHER, "Brother Desmond", 12, 22);
        // Maureen: lollipop lady in bright yellow hi-vis near the primary school crossing
        spawnNamedNPCAtTerrain(NPCType.LOLLIPOP_LADY, "Maureen", -26, -50);
    }

    /**
     * Spawn static quest-giver NPCs inside buildings that have a registered quest.
     * Each NPC is stationed at the centre of its building's ground floor (Issue #462).
     */
    private void spawnBuildingNPCs() {
        BuildingQuestRegistry registry = interactionSystem.getQuestRegistry();
        for (ragamuffin.world.Landmark landmark : world.getAllLandmarks()) {
            LandmarkType type = landmark.getType();
            if (registry.hasQuest(type)) {
                ragamuffin.world.Landmark lm = landmark;
                float x = lm.getPosition().x + lm.getWidth() / 2.0f;
                float z = lm.getPosition().z + lm.getDepth() / 2.0f;
                // Fix #491: use the landmark's ground-floor Y position instead of
                // calculateSpawnHeight(), which returns the highest solid block (the roof)
                // and causes the shopkeeper to spawn on top of the building, outside it.
                float y = lm.getPosition().y + 1.0f;
                npcManager.spawnBuildingNPC(type, x, y, z);
            }
        }
    }

    /**
     * Update chunk renderers for all loaded chunks.
     */
    private void updateChunkRenderers() {
        for (Chunk chunk : world.getLoadedChunks()) {
            chunkRenderer.updateChunk(chunk, meshBuilder);
        }
    }

    /**
     * Mark the chunk containing the given world coordinates dirty for rebuild,
     * plus any neighbouring chunks if the block is on a chunk boundary.
     * Chunks are rebuilt by the budget-limited dirty chunk system in the main loop
     * to avoid per-block synchronous mesh rebuilds that cause FPS drops.
     */
    private void rebuildChunkAt(int worldX, int worldY, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkY = Math.floorDiv(worldY, Chunk.HEIGHT);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);

        // Mark the primary chunk dirty
        markChunkDirty(chunkX, chunkY, chunkZ);

        // If on a chunk boundary, mark neighbours dirty so exposed faces update
        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localY = Math.floorMod(worldY, Chunk.HEIGHT);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);
        if (localX == 0) markChunkDirty(chunkX - 1, chunkY, chunkZ);
        if (localX == Chunk.SIZE - 1) markChunkDirty(chunkX + 1, chunkY, chunkZ);
        if (localY == 0) markChunkDirty(chunkX, chunkY - 1, chunkZ);
        if (localY == Chunk.HEIGHT - 1) markChunkDirty(chunkX, chunkY + 1, chunkZ);
        if (localZ == 0) markChunkDirty(chunkX, chunkY, chunkZ - 1);
        if (localZ == Chunk.SIZE - 1) markChunkDirty(chunkX, chunkY, chunkZ + 1);
    }

    private void markChunkDirty(int chunkX, int chunkY, int chunkZ) {
        if (world.isChunkLoaded(chunkX, chunkY, chunkZ)) {
            world.markChunkDirty(chunkX, chunkY, chunkZ);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Update input
        inputHandler.update();

        // Handle state-specific input and updates
        if (state == GameState.LOADING) {
            if (!loadingComplete) {
                renderLoadingScreen();
                // Trigger heavy init on the next frame so the loading screen gets a chance to paint
                if (delta > 0) {
                    initGame();
                }
            }
        } else if (state == GameState.MENU) {
            handleMenuInput();
            renderMenu();
        } else if (state == GameState.CINEMATIC) {
            // Issue #373: City fly-through cinematic cut-scene
            // Fix #428: Opening sequence text runs simultaneously (overlaid) with the cinematic.
            cinematicCamera.update(delta);
            openingSequence.update(delta);

            // Allow any key / click to skip
            if (inputHandler.isEnterPressed() || inputHandler.isJumpPressed()
                    || inputHandler.isLeftClickPressed() || inputHandler.isEscapePressed()) {
                cinematicCamera.skip();
                openingSequence.skip();
                inputHandler.resetEnter();
                inputHandler.resetJump();
                inputHandler.resetLeftClick();
                inputHandler.resetEscape();
            }

            // Fix #447: Call full npcManager.update() during the cinematic so NPCs
            // continue walking patrol routes and reacting to the world during the
            // fly-through.  This replaces the individual tickX() calls added by Fix
            // #425 — those methods are already called inside update(), so keeping both
            // would double-tick every timer.  The cinematic is a live simulation with
            // only the camera detached from the player; NPCs must not freeze.
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            // Fix #447: Update speech log immediately after npcManager.update() so
            // any speech set during this frame's NPC tick is reflected right away —
            // mirrors the ordering in the PLAYING path (updatePlayingSimulation).
            speechLogUI.update(npcManager.getNPCs(), delta);
            // Issue #803: Advance wanted and notoriety systems during cinematic so timers
            // continue to accumulate — mirrors the PAUSED and PLAYING branches.
            wantedSystem.update(delta, player, npcManager.getNPCs(),
                    weatherSystem.getCurrentWeather(), timeSystem.isNight(), false,
                    type -> achievementSystem.unlock(type));
            notorietySystem.update(delta, player, type -> achievementSystem.unlock(type));
            rumourNetwork.update(npcManager.getNPCs(), delta);
            blockBreaker.tickDecay(delta);
            propBreaker.tickDecay(delta);
            player.getStreetReputation().update(delta);
            weatherSystem.update(delta * timeSystem.getTimeSpeed() * 3600f);
            timeSystem.update(delta);
            npcManager.setGameTime(timeSystem.getTime());
            lightingSystem.updateLighting(timeSystem.getTime(), timeSystem.getSunriseTime(), timeSystem.getSunsetTime());
            updateSkyColour(timeSystem.getTime());
            particleSystem.update(delta);
            // Fix #429: Advance gang territory linger timer during cinematic so the player
            // cannot exploit the city fly-through to freeze the 5-second hostility escalation
            // countdown.  Mirrors the equivalent call in the PAUSED branch (line ~884).
            gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, world);

            // Fix #860: Advance StreetSkillSystem during cinematic — mirrors the PLAYING and PAUSED branches.
            player.getStreetSkillSystem().update(delta, player, npcManager.getNPCs());

            // Issue #862: Advance newspaper publication timer during cinematic — mirrors PLAYING and PAUSED branches.
            newspaperSystem.update(
                    delta,
                    timeSystem.getTime(),
                    timeSystem.getDayCount(),
                    notorietySystem,
                    wantedSystem,
                    rumourNetwork,
                    null,
                    factionSystem,
                    fenceSystem,
                    streetEconomySystem,
                    player.getCriminalRecord(),
                    npcManager.getNPCs(),
                    type -> achievementSystem.unlock(type));
            // Issue #866: Advance FenceSystem during cinematic — mirrors the PLAYING and PAUSED branches.
            fenceSystem.update(delta, player, npcManager.getNPCs(), timeSystem.getDayIndex());
            // Fix #429: Keep police spawn/despawn logic in sync with the day/night cycle
            // during the cinematic.  If the fly-through straddles dusk or dawn the wasNight
            // flag transition must not be skipped — mirrors the PAUSED branch (line ~852).
            npcManager.updatePoliceSpawning(timeSystem.isNight(), world, player);

            // Fix #433: Advance hunger, starvation, energy recovery, and weather exposure
            // during the cinematic so the player cannot exploit the opening fly-through to
            // avoid starving, halt cold-snap health drain, or prevent energy recovery.
            // Mirrors the equivalent block in the PAUSED branch (~lines 932-948).
            if (!respawnSystem.isRespawning()) {
                player.updateHunger(delta);

                if (player.getHunger() <= 0) {
                    player.damage(5.0f * delta, DamageReason.STARVATION);
                }

                Weather cinematicWeather = weatherSystem.getCurrentWeather();
                float cinematicWeatherMultiplier = exposureSystem.getEffectiveEnergyDrainMultiplier(
                        cinematicWeather, world, player.getPosition());
                player.recoverEnergy(delta / cinematicWeatherMultiplier);

                if (exposureSystem.isExposedToWeatherDamage(cinematicWeather, timeSystem.isNight(), world, player.getPosition())) {
                    float cinematicHealthDrain = cinematicWeather.getHealthDrainRate() * delta;
                    player.damage(cinematicHealthDrain, DamageReason.WEATHER);
                }
            }

            // Fix #435: Advance healing resting timer during the cinematic so the
            // 5-second resting threshold continues to accumulate — mirrors Fix #381
            // (the PAUSED branch, line ~933).  Without this the entire 8-second
            // fly-through freezes the timer and any accumulated resting progress is
            // discarded on the first PLAYING frame.
            healingSystem.update(delta, player);
            // Squat passive regen: +1 health/min when inside squat at Vibe >= 20
            if (squatSystem != null && squatSystem.isRegenActive(
                    player.getPosition().x, player.getPosition().z, 15f)) {
                player.heal(delta / 60f);
            }

            // Fix #899: Decrement market event cooldown during cinematic for scheduling consistency
            marketEventCooldown -= delta;

            // Fix #435: Check for player death and advance the respawn countdown
            // during the cinematic.  starvation damage and weather health drain
            // (added in Fix #433) can reduce the player's health to zero during the
            // fly-through; without this guard the game would transition to PLAYING
            // with a dead player and only then trigger the respawn sequence —
            // causing a one-frame soft-lock.  Mirrors the PAUSED branch (line ~882)
            // and the PLAYING branch respawn block.
            boolean cinematicJustDied = respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
            if (cinematicJustDied) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
            boolean cinematicWasRespawning = respawnSystem.isRespawning();
            respawnSystem.update(delta, player);
            if (cinematicWasRespawning && !respawnSystem.isRespawning()) {
                deathMessage = null;
                // Fix #635: Clear any pending arrest — the player just died, so any in-flight
                // arrest is moot; leaving it set would cause a ghost arrest on the next frame.
                npcManager.clearArrestPending();
                greggsRaidSystem.reset();
                player.getStreetReputation().reset();
                healingSystem.resetPosition(player.getPosition());
                // Fix #623: Reset distance tracking on respawn so the teleport distance is not
                // counted as walked distance — mirrors the PLAYING (Fix #459) and PAUSED branches.
                distanceTravelledAchievement = 0f;
                lastPlayerPosForDistance.set(player.getPosition());
                // Fix #623: Reset all stale input flags so any key/mouse event buffered during
                // the respawn countdown does not fire as a phantom action on the first PLAYING frame.
                // Mirrors the identical resets in the PLAYING (Fix #609) and PAUSED (Fix #617) branches.
                inputHandler.resetEscape();
                inputHandler.resetPunch();
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
                inputHandler.resetPlace();
                inputHandler.resetInventory();
                inputHandler.resetHelp();
                inputHandler.resetCrafting();
                inputHandler.resetAchievements();
                inputHandler.resetQuestLog();
                inputHandler.resetInteract();
                inputHandler.resetJump();
                inputHandler.resetDodge();
                inputHandler.resetEnter();
                inputHandler.resetUp();
                inputHandler.resetDown();
                inputHandler.resetHotbarSlot();
                inputHandler.resetCraftingSlot();
                inputHandler.resetLeftClick();
                inputHandler.resetLeftClickReleased();
                inputHandler.resetRightClick();
                inputHandler.resetScroll();
                // Fix #643: Clear polled movement flags so a held WASD/sprint key does not
                // cause immediate unwanted movement on the first post-respawn PLAYING frame.
                inputHandler.resetMovement();
                // Fix #623: Close any UI overlays left open at the time of death so the player
                // does not resume with inventory/crafting/help still showing (isUIBlocking() true).
                // Mirrors the PLAYING (Fix #609) and PAUSED (Fix #617) branches.
                inventoryUI.hide();
                craftingUI.hide();
                helpUI.hide();
                achievementsUI.hide();
                questLogUI.hide();
                skillsUI.hide();
                // Fix #623: Close and clear any active shop menu so isUIBlocking() returns false
                // after respawn — mirrors the PLAYING branch (Fix #601) and PAUSED branch (Fix #621).
                if (activeShopkeeperNPC != null) {
                    activeShopkeeperNPC.setShopMenuOpen(false);
                    activeShopkeeperNPC = null;
                }
            }

            // Fix #439: Process pending arrest during cinematic so the flag does not persist as a ghost.
            // Mirrors Fix #367 (the PAUSED branch) — if police set arrestPending=true on the same frame
            // the cinematic starts the flag must be evaluated here, not deferred to the first PLAYING frame.
            if (npcManager.isArrestPending() && !player.isDead()) {
                java.util.List<String> confiscated = arrestSystem.arrest(player, inventory);
                String arrestMsg = ArrestSystem.buildArrestMessage(confiscated);
                tooltipSystem.showMessage(arrestMsg, 4.0f);
                npcManager.clearArrestPending();
                greggsRaidSystem.reset();
                player.getStreetReputation().removePoints(15);
                healingSystem.resetPosition(player.getPosition());
                // Fix #629: Sync distance tracking after arrest teleport so the teleport distance
                // is not counted as walked distance — mirrors PLAYING (Fix #619) and PAUSED branches.
                distanceTravelledAchievement = 0f;
                lastPlayerPosForDistance.set(player.getPosition());
                // Fix #613: Reset all event-driven input flags after arrest teleport so stale flags
                // do not fire as phantom actions on the first PLAYING frame post-arrest.
                // Mirrors the identical reset in the respawn path and PLAYING/PAUSED arrest branches.
                inputHandler.resetEscape();
                inputHandler.resetPunch();
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
                inputHandler.resetPlace();
                inputHandler.resetInventory();
                inputHandler.resetHelp();
                inputHandler.resetCrafting();
                inputHandler.resetAchievements();
                inputHandler.resetQuestLog();
                inputHandler.resetInteract();
                inputHandler.resetJump();
                inputHandler.resetDodge();
                inputHandler.resetEnter();
                inputHandler.resetUp();
                inputHandler.resetDown();
                inputHandler.resetHotbarSlot();
                inputHandler.resetCraftingSlot();
                inputHandler.resetLeftClick();
                inputHandler.resetLeftClickReleased();
                inputHandler.resetRightClick();
                inputHandler.resetScroll();
                // Fix #643: Clear polled movement flags so a held WASD/sprint key does not
                // cause immediate unwanted movement on the first post-arrest PLAYING frame.
                inputHandler.resetMovement();
                // Fix #615: Close any UI overlays left open at the time of arrest so
                // isUIBlocking() returns false on the first PLAYING frame post-arrest.
                // Mirrors the identical hide() calls in the respawn-completion block.
                inventoryUI.hide();
                craftingUI.hide();
                helpUI.hide();
                achievementsUI.hide();
                questLogUI.hide();
                // Fix #615: Close and clear any active shop menu so isUIBlocking() returns
                // false after arrest. Mirrors the justDied path (fix #601).
                if (activeShopkeeperNPC != null) {
                    activeShopkeeperNPC.setShopMenuOpen(false);
                    activeShopkeeperNPC = null;
                }
                // Fix #649: Transition to PLAYING after arrest so the player is not left
                // stuck in the cinematic fly-through. Mirrors the PAUSED arrest branch
                // (Fix #627) which calls transitionToPlaying() for the same reason.
                finishCinematic();
            }

            // Fix #437: Advance tooltip timers during the cinematic so queued tooltips
            // (e.g. FIRST_DEATH from respawnSystem, gang-territory warnings) count down
            // at the correct rate rather than freezing for the ~8-second fly-through.
            tooltipSystem.update(delta);
            // Fix #453: Advance achievement notification countdown during CINEMATIC so banners
            // queued during the opening sequence count down rather than freezing.
            achievementSystem.update(delta);
            // Fix #437: Advance clock HUD during the cinematic so the display stays in
            // sync with timeSystem (which is ticked above).
            clockHUD.update(timeSystem.getTime(), timeSystem.getDayCount(), timeSystem.getDayOfMonth(), timeSystem.getMonthName());
            // Fix #437: Advance damage flash vignette and dodge cooldown during the
            // cinematic so they expire at their intended rate rather than freezing for
            // the entire fly-through duration.
            // Fix #455: Detect new damage events during CINEMATIC so the damage-reason
            // banner fires correctly (starvation/weather damage from fix #433).
            float flashNow = player.getDamageFlashIntensity();
            if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
                gameHUD.showDamageReason(player.getLastDamageReason());
            }
            prevDamageFlashIntensity = flashNow;
            player.updateFlash(delta);
            // Fix #443: Advance HUD timers (damage-reason banner) during the cinematic
            // so the banner counts down rather than freezing for the entire fly-through.
            gameHUD.update(delta);
            player.updateDodge(delta);
            // Fix #457: Apply gravity during CINEMATIC so the player cannot exploit
            // the cinematic to avoid fall damage — mirrors applyGravityAndVerticalCollision
            // in updatePlayingSimulation.
            if (!player.isDead()) {
                world.applyGravityAndVerticalCollision(player, delta);
            }
            // Fix #471: Keep lastPlayerPosForDistance in sync with the player position
            // so that when PLAYING resumes the first-frame distance delta is not inflated
            // by any gravity-driven movement that occurred during the cinematic.
            if (lastPlayerPosForDistance != null) {
                lastPlayerPosForDistance.set(player.getPosition());
            }
            // Fix #445: Advance arm swing/bob animation during cinematic so the
            // idleTimer continues accumulating and the arm is in the correct phase
            // when PLAYING starts.
            firstPersonArm.update(delta);

            // Fix #427: Continue loading and meshing chunks during the cinematic so that
            // the world is fully built before the player takes control.  The cinematic
            // camera sweeps across a large area; without this, many chunks remain as bare
            // dirty-queue entries and pop in visibly when PLAYING begins.
            // Use the player's spawn position as the centre for chunk-load decisions —
            // all cinematic waypoints are within render distance of the origin.
            java.util.Set<String> cinematicUnloaded = world.updateLoadedChunks(player.getPosition());
            for (String key : cinematicUnloaded) {
                chunkRenderer.removeChunkByKey(key);
            }
            List<Chunk> cinematicDirty = world.getDirtyChunks();
            if (!cinematicDirty.isEmpty()) {
                int built = 0;
                java.util.Iterator<Chunk> cit = cinematicDirty.iterator();
                while (cit.hasNext() && built < 16) {
                    Chunk chunk = cit.next();
                    chunkRenderer.updateChunk(chunk, meshBuilder);
                    world.markChunkClean(chunk);
                    built++;
                }
            }

            // Transition to PLAYING once cinematic completes
            if (cinematicCamera.isCompleted()) {
                finishCinematic();
            } else {
                // Position main camera at the cinematic camera's interpolated location
                camera.position.set(cinematicCamera.getPosition());
                camera.lookAt(cinematicCamera.getLookAt());
                camera.up.set(com.badlogic.gdx.math.Vector3.Y);
                camera.update();

                // Render the 3D world from the cinematic camera viewpoint
                Gdx.gl.glClearColor(skyR, skyG, skyB, 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                // Render skybox (sun/clouds) — skyRenderer.update() advances cloud
                // animation; time is pinned to 10.0f (mid-morning) for the cinematic.
                skyRenderer.update(delta);
                {
                    float ts = 10.0f; // Fixed time: mid-morning for the cinematic
                    float sr = timeSystem.getSunriseTime();
                    float ss = timeSystem.getSunsetTime();
                    skyRenderer.renderSkybox(shapeRenderer, ts, sr, ss,
                                             Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                                             false, 0f);
                }
                Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

                modelBatch.begin(camera);
                chunkRenderer.render(modelBatch, environment);
                // Issue #672: Render car traffic during cinematic
                if (carRenderer != null) {
                    carRenderer.render(modelBatch, environment, carManager.getCars());
                }
                // Issue #669: Render non-block-based 3D props during cinematic
                propRenderer.render(modelBatch, environment);
                // Issue #675: Render small 3D items during cinematic
                if (smallItemRenderer != null) {
                    smallItemRenderer.render(modelBatch, environment);
                }
                modelBatch.end();

                // Render letterbox and skip hint overlay
                int sw = Gdx.graphics.getWidth();
                int sh = Gdx.graphics.getHeight();
                com.badlogic.gdx.math.Matrix4 proj = new com.badlogic.gdx.math.Matrix4();
                proj.setToOrtho2D(0, 0, sw, sh);
                spriteBatch.setProjectionMatrix(proj);
                shapeRenderer.setProjectionMatrix(proj);
                cinematicCamera.render(spriteBatch, shapeRenderer, font, sw, sh);
                // Fix #428: Render opening sequence text overlaid on the cinematic (no black background).
                openingSequence.renderTextOnly(spriteBatch, font, sw, sh);
                // Fix #435: Overlay the death screen if the player died during the
                // cinematic (e.g. from starvation or weather damage on a cold-snap
                // night).  Mirrors the PAUSED branch (line ~893).
                if (respawnSystem.isRespawning()) {
                    renderDeathScreen();
                }
            }
        } else if (state == GameState.PLAYING) {
            // Apply mouse look FIRST — before any game logic — to eliminate perceived lag.
            // This ensures the camera direction is always up-to-date when the frame renders.
            // Suppress mouse look while driving — the chase camera controls the view direction.
            if (!carDrivingSystem.isInCar()) {
                if (Gdx.input.isCursorCatched()) {
                    float mouseDX = inputHandler.getMouseDeltaX();
                    float mouseDY = inputHandler.getMouseDeltaY();
                    if (mouseDX != 0 || mouseDY != 0) {
                        cameraYaw += mouseDX * MOUSE_SENSITIVITY;
                        cameraPitch -= mouseDY * MOUSE_SENSITIVITY;
                        cameraPitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, cameraPitch));
                    }
                }
                // Rebuild camera direction from yaw/pitch angles
                float pitchRad = (float) Math.toRadians(cameraPitch);
                float yawRad = (float) Math.toRadians(cameraYaw);
                camera.direction.set(
                    (float) Math.sin(yawRad) * (float) Math.cos(pitchRad),
                    (float) Math.sin(pitchRad),
                    -(float) Math.cos(yawRad) * (float) Math.cos(pitchRad)
                );
                camera.up.set(Vector3.Y);
            } else {
                // Consume mouse deltas so they don't accumulate
                inputHandler.getMouseDeltaX();
                inputHandler.getMouseDeltaY();
            }

            // Update opening sequence if active
            if (openingSequence.isActive()) {
                openingSequence.update(delta);
                // Allow skipping with Enter, Space (jump), or left-click
                if (inputHandler.isEnterPressed() || inputHandler.isJumpPressed() || inputHandler.isLeftClickPressed()) {
                    openingSequence.skip();
                    inputHandler.resetEnter();
                    inputHandler.resetJump();
                    inputHandler.resetLeftClick();
                }
            }

            // Handle UI toggles — suppress while opening cinematic is playing
            if (!openingSequence.isActive()) {
                handleUIInput();
            }

            // Handle state transitions
            if (inputHandler.isEscapePressed()) {
                handleEscapePress();
                inputHandler.resetEscape();
            }

            // Fix #401: Update world simulation unconditionally — the opening sequence is a 2D overlay;
            // the 3D world should keep simulating (NPCs, gravity, chunk loading, etc.) behind it.
            // Fix #198: NPCs/gang/arrest must not freeze when inventory/crafting/help UI is open.
            updatePlayingSimulation(delta);

            // Update player-input logic (movement, punch, placement) — gated behind UI check.
            // Fix #269: suppress input while player is dead or during the respawn countdown.
            // Opening sequence also suppresses player input (it is a non-blocking overlay only).
            if (!isUIBlocking() && !openingSequence.isActive() && !respawnSystem.isRespawning() && !player.isDead()) {
                updatePlayingInput(delta);
            }

            // Update time system — runs unconditionally (Fix #401: must tick during opening sequence too)
            timeSystem.update(delta);
            npcManager.setGameTime(timeSystem.getTime());
            lightingSystem.updateLighting(timeSystem.getTime(), timeSystem.getSunriseTime(), timeSystem.getSunsetTime());
            updateSkyColour(timeSystem.getTime());
            clockHUD.update(timeSystem.getTime(), timeSystem.getDayCount(), timeSystem.getDayOfMonth(), timeSystem.getMonthName());

            // Phase 12: Update weather system with game-time seconds
            // timeSpeed (hours/real-second) * 3600 = game-seconds per real-second
            float gameTimeDeltaSeconds = delta * timeSystem.getTimeSpeed() * 3600f;
            weatherSystem.update(gameTimeDeltaSeconds);

            // Update police spawning based on seasonal night (TimeSystem.isNight())
            npcManager.updatePoliceSpawning(timeSystem.isNight(), world, player);

            // Fix #411: Hunger, starvation, and weather effects tick unconditionally —
            // opening a UI overlay (inventory/help/crafting) must not freeze time-based
            // survival simulation. Only the sprint multiplier falls back to ×1 while UI
            // is open since the player is not moving. The healingSystem remains gated
            // (intentional: you cannot rest while rummaging through your inventory).
            if (!respawnSystem.isRespawning()) {
                // Sprint drains hunger 3x faster, but only when the player is actually
                // controlling movement (UI not blocking input).
                float hungerMultiplier = (!isUIBlocking() && inputHandler.isSprintHeld()) ? 3.0f : 1.0f;
                player.updateHunger(delta * hungerMultiplier);

                // Sprint drains energy while moving — only applicable when UI is not open
                if (!isUIBlocking()) {
                    boolean isMovingNow = inputHandler.isForward() || inputHandler.isBackward() || inputHandler.isLeft() || inputHandler.isRight();
                    if (inputHandler.isSprintHeld() && isMovingNow) {
                        player.consumeEnergy(Player.SPRINT_ENERGY_DRAIN * delta);
                    }
                }

                // Starvation: zero hunger drains health at 5 HP/s
                if (player.getHunger() <= 0) {
                    player.damage(5.0f * delta, DamageReason.STARVATION);
                }

                // Issue #234: Apply weather energy drain multiplier, shielded by indoor shelter
                Weather currentWeather = weatherSystem.getCurrentWeather();
                float weatherMultiplier = exposureSystem.getEffectiveEnergyDrainMultiplier(
                        currentWeather, world, player.getPosition());
                // Weather affects recovery rate inversely - worse weather = slower recovery
                player.recoverEnergy(delta / weatherMultiplier);

                // Issue #234: Cold snap health drain at night — shielded by indoor shelter
                if (exposureSystem.isExposedToWeatherDamage(currentWeather, timeSystem.isNight(), world, player.getPosition())) {
                    float healthDrain = currentWeather.getHealthDrainRate() * delta;
                    player.damage(healthDrain, DamageReason.WEATHER);
                }

                // Phase 11: Update healing system (gated: no healing while UI is open —
                // intentional design: you cannot rest while rummaging through your inventory)
                if (!isUIBlocking()) {
                    healingSystem.update(delta, player);
                }
            }
            // Squat passive regen: +1 health/min when inside squat at Vibe >= 20
            if (squatSystem != null && squatSystem.isRegenActive(
                    player.getPosition().x, player.getPosition().z, 15f)) {
                player.heal(delta / 60f);
            }

            // Phase 11: Check for death and respawn
            boolean justDied = respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
            // Fix #275: Clear sticky punch state on death so auto-punch doesn't fire on respawn
            if (justDied) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
                // Fix #601: Close and clear any active shop menu on death so isUIBlocking()
                // returns false after respawn and player input is not permanently suppressed.
                if (activeShopkeeperNPC != null) {
                    activeShopkeeperNPC.setShopMenuOpen(false);
                    activeShopkeeperNPC = null;
                }
            }
            boolean wasRespawning = respawnSystem.isRespawning();
            respawnSystem.update(delta, player);
            if (wasRespawning && !respawnSystem.isRespawning()) {
                deathMessage = null; // Reset for next death
                // Fix #635: Clear any pending arrest — the player just died, so any in-flight
                // arrest is moot; leaving it set would cause a ghost arrest on the next frame.
                npcManager.clearArrestPending();
                // Issue #114: Reset Greggs raid on respawn — mirrors the arrest-handler reset
                greggsRaidSystem.reset();
                // Issue #154: Reset street reputation on death — the streets forget you while you were dead
                player.getStreetReputation().reset();
                // Issue #166: Sync HealingSystem position after teleport so the next update()
                // does not compute a spurious speed from the respawn distance.
                healingSystem.resetPosition(player.getPosition());
                // Fix #459: Reset distance tracking on respawn so distance doesn't carry across deaths.
                distanceTravelledAchievement = 0f;
                lastPlayerPosForDistance.set(player.getPosition());
                // Fix #609: Reset all stale input flags so any key/mouse event buffered during
                // the respawn countdown does not fire as a phantom action on the first live frame.
                // Mirrors the identical resets in transitionToPlaying().
                inputHandler.resetEscape();
                inputHandler.resetPunch();
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
                inputHandler.resetPlace();
                inputHandler.resetInventory();
                inputHandler.resetHelp();
                inputHandler.resetCrafting();
                inputHandler.resetAchievements();
                inputHandler.resetQuestLog();
                inputHandler.resetInteract();
                inputHandler.resetJump();
                inputHandler.resetDodge();
                inputHandler.resetEnter();
                inputHandler.resetUp();
                inputHandler.resetDown();
                inputHandler.resetHotbarSlot();
                inputHandler.resetCraftingSlot();
                inputHandler.resetLeftClick();
                inputHandler.resetLeftClickReleased();
                inputHandler.resetRightClick();
                inputHandler.resetScroll();
                // Fix #643: Clear polled movement flags so a held WASD/sprint key does not
                // cause immediate unwanted movement on the first post-respawn PLAYING frame.
                inputHandler.resetMovement();
                // Fix #609: Close any UI overlays left open at the time of death so the player
                // does not respawn with inventory/crafting/help still showing.
                inventoryUI.hide();
                craftingUI.hide();
                helpUI.hide();
                achievementsUI.hide();
                questLogUI.hide();
                // Fix #625: Close and clear any active shop menu so isUIBlocking() returns false
                // after respawn — mirrors the CINEMATIC branch (Fix #623) and PAUSED branch (Fix #621).
                if (activeShopkeeperNPC != null) {
                    activeShopkeeperNPC.setShopMenuOpen(false);
                    activeShopkeeperNPC = null;
                }
            }

            // Phase 11: Trigger hunger warning tooltip
            if (player.getHunger() <= 25 && !tooltipSystem.hasShown(TooltipTrigger.HUNGER_LOW)) {
                tooltipSystem.trigger(TooltipTrigger.HUNGER_LOW);
            }

            // Update tooltip system
            tooltipSystem.update(delta);
            // Fix #453: Advance achievement notification countdown during PLAYING so banners
            // are shown and cleared at the correct rate.
            achievementSystem.update(delta);

            // Issue #209: Update sky renderer (cloud animation)
            skyRenderer.update(delta);

            // Render 3D world
            Gdx.gl.glClearColor(skyR, skyG, skyB, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // Fix #227: Render sun and clouds as part of the skybox — before 3D geometry
            // so that buildings and terrain naturally occlude them.  After drawing the
            // sky elements we clear only the depth buffer so the 3D world renders on top.
            // Fix #235: Pass cameraYaw so the skybox remains stationary relative to the world.
            // Fix #734: Pass dayOfYear so the night sky (stars, moon, planets) updates seasonally.
            // Fix #751: Pass cameraPitch so the skybox doesn't move vertically with the camera.
            {
                float ts = timeSystem.getTime();
                float sr = timeSystem.getSunriseTime();
                float ss = timeSystem.getSunsetTime();
                skyRenderer.renderSkybox(shapeRenderer, ts, sr, ss,
                                         Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                                         timeSystem.isNight(), cameraYaw,
                                         timeSystem.getDayOfYear(), cameraPitch);
            }
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

            modelBatch.begin(camera);
            chunkRenderer.render(modelBatch, environment);
            npcRenderer.render(modelBatch, environment, npcManager.getNPCs());
            // Issue #672: Render car traffic so cars are visible in-game
            if (carRenderer != null) {
                carRenderer.render(modelBatch, environment, carManager.getCars());
            }
            // Issue #669: Render non-block-based 3D props
            propRenderer.render(modelBatch, environment);
            // Issue #675: Render small 3D items placed on block surfaces
            if (smallItemRenderer != null) {
                smallItemRenderer.render(modelBatch, environment);
            }
            // Issue #676: Render flags as physical 3D objects
            flagRenderer.render(modelBatch, environment);
            modelBatch.end();

            // Issue #781: Render graffiti marks on block surfaces
            graffitiRenderer.render(graffitiSystem.getAllMarks(), camera);

            // Issue #54: Render block targeting outline and placement ghost block
            // Issue #192: Skip when a UI overlay is open to avoid drawing on top of inventory/crafting/help screens
            if (!isUIBlocking()) {
                renderBlockHighlight();
            }

            // Render NPC speech bubbles (2D overlay projected from 3D)
            renderSpeechBubbles();

            // Issue #10: Render building signs as world-projected overlays
            signageRenderer.render(camera, spriteBatch, shapeRenderer, font,
                                   Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Render rain overlay if raining
            if (weatherSystem.getCurrentWeather() == Weather.RAIN) {
                renderRain(delta);
            }

            // Issue #171: Render particle effects (screen-space, before HUD)
            {
                int sw = Gdx.graphics.getWidth();
                int sh = Gdx.graphics.getHeight();
                com.badlogic.gdx.math.Matrix4 particleOrtho = new com.badlogic.gdx.math.Matrix4();
                particleOrtho.setToOrtho2D(0, 0, sw, sh);
                shapeRenderer.setProjectionMatrix(particleOrtho);
                particleSystem.render(shapeRenderer, camera, sw, sh);
            }

            // Render 2D UI overlay
            renderUI();

            // Render death screen overlay
            if (respawnSystem.isRespawning()) {
                renderDeathScreen();
            }

            // Render opening sequence overlay
            if (openingSequence.isActive()) {
                openingSequence.render(spriteBatch, shapeRenderer, font,
                                      Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }
        } else if (state == GameState.PAUSED) {
            // Paused - still render world but frozen
            Gdx.gl.glClearColor(skyR, skyG, skyB, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // Fix #227: Render sun and clouds as part of the skybox — before 3D geometry
            // Fix #235: Pass cameraYaw so the skybox remains stationary relative to the world.
            // Fix #323: Advance cloud animation while paused so clouds continue scrolling
            // (sky is atmospheric background and should not freeze during pause).
            // Fix #734: Pass dayOfYear so the night sky updates seasonally.
            // Fix #751: Pass cameraPitch so the skybox doesn't move vertically with the camera.
            {
                skyRenderer.update(delta);
                float ts = timeSystem.getTime();
                float sr = timeSystem.getSunriseTime();
                float ss = timeSystem.getSunsetTime();
                skyRenderer.renderSkybox(shapeRenderer, ts, sr, ss,
                                         Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                                         timeSystem.isNight(), cameraYaw,
                                         timeSystem.getDayOfYear(), cameraPitch);
            }
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

            modelBatch.begin(camera);
            chunkRenderer.render(modelBatch, environment);
            npcRenderer.render(modelBatch, environment, npcManager.getNPCs());
            // Issue #672: Render car traffic so cars are visible while paused
            if (carRenderer != null) {
                carRenderer.render(modelBatch, environment, carManager.getCars());
            }
            // Issue #669: Render non-block-based 3D props
            propRenderer.render(modelBatch, environment);
            // Issue #675: Render small 3D items while paused
            if (smallItemRenderer != null) {
                smallItemRenderer.render(modelBatch, environment);
            }
            // Issue #676: Render flags as physical 3D objects while paused
            flagRenderer.render(modelBatch, environment);
            modelBatch.end();

            // Fix #333: Render rain overlay while paused so the rain effect persists
            // visually when the player opens the pause menu during rain — mirrors the
            // same guard in the PLAYING render path (lines 639-641).
            if (weatherSystem.getCurrentWeather() == Weather.RAIN) {
                renderRain(delta);
            }

            // Fix #351: Render particles while paused so active particles are visible
            // during the pause (not just updated) — mirrors the PLAYING render path.
            {
                int sw = Gdx.graphics.getWidth();
                int sh = Gdx.graphics.getHeight();
                com.badlogic.gdx.math.Matrix4 particleOrtho = new com.badlogic.gdx.math.Matrix4();
                particleOrtho.setToOrtho2D(0, 0, sw, sh);
                shapeRenderer.setProjectionMatrix(particleOrtho);
                particleSystem.render(shapeRenderer, camera, sw, sh);
            }

            // Fix #321: Advance damage flash and HUD timers while paused so the
            // red vignette fades out and the damage-reason banner counts down.
            // Fix #455: Detect new damage events during PAUSED so the damage-reason
            // banner fires correctly (starvation/weather damage from fix #399).
            float flashNow = player.getDamageFlashIntensity();
            if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
                gameHUD.showDamageReason(player.getLastDamageReason());
            }
            prevDamageFlashIntensity = flashNow;
            player.updateFlash(delta);
            gameHUD.update(delta);
            // Fix #331: Advance tooltip countdown while paused so active tooltips
            // fade out and queued tooltips advance rather than freezing on-screen.
            tooltipSystem.update(delta);
            // Fix #453: Advance achievement notification countdown while paused so banners
            // count down rather than freezing for the entire pause duration.
            achievementSystem.update(delta);
            // Fix #339: Advance arm swing animation while paused so a mid-punch
            // swing completes rather than freezing in the extended position.
            firstPersonArm.update(delta);
            // Fix #351: Advance particle simulation while paused so active particles
            // (combat sparks, block-break debris, footstep dust, dodge trail streaks)
            // expire naturally rather than hanging frozen in world-space for the entire
            // pause duration and all expiring simultaneously on resume.
            particleSystem.update(delta);
            // Issue #658: Advance flag wave animation while paused so flags continue
            // rippling in the background when the pause menu is open.
            flagRenderer.update(delta);
            // Fix #379: Advance dodge timer while paused so mid-roll invincibility
            // windows and dodge cooldowns expire at their intended rate rather than
            // freezing for the entire pause duration (which would allow the player to
            // exploit pause to extend dodge invincibility indefinitely).
            player.updateDodge(delta);
            // Fix #457: Apply gravity during PAUSED so the player cannot exploit
            // the pause menu to avoid fall damage — mirrors applyGravityAndVerticalCollision
            // in updatePlayingSimulation.
            if (!player.isDead()) {
                world.applyGravityAndVerticalCollision(player, delta);
            }
            // Fix #471: Keep lastPlayerPosForDistance in sync with the player position
            // so that when PLAYING resumes the first-frame distance delta is not inflated
            // by any gravity-driven movement that occurred while paused.
            if (lastPlayerPosForDistance != null) {
                lastPlayerPosForDistance.set(player.getPosition());
            }
            // Fix #341: Advance weather timer while paused so weather transitions
            // continue to accumulate — mirrors the PLAYING path (line ~530).
            weatherSystem.update(delta * timeSystem.getTimeSpeed() * 3600f);
            // Fix #343: Advance speech log entry timers while paused so entries fade out
            // rather than freezing on-screen for the entire duration of the pause.
            speechLogUI.update(npcManager.getNPCs(), delta);
            // Fix #347: Advance respawn countdown while paused so the player is revived
            // even if they opened the pause menu during the death-screen countdown.
            if (respawnSystem.isRespawning()) {
                boolean wasRespawning = respawnSystem.isRespawning();
                respawnSystem.update(delta, player);
                if (wasRespawning && !respawnSystem.isRespawning()) {
                    deathMessage = null;
                    // Fix #635: Clear any pending arrest — the player just died, so any in-flight
                    // arrest is moot; leaving it set would cause a ghost arrest on the next frame.
                    npcManager.clearArrestPending();
                    greggsRaidSystem.reset();
                    player.getStreetReputation().reset();
                    healingSystem.resetPosition(player.getPosition());
                    // Fix #459: Reset distance tracking on respawn so distance doesn't carry across deaths.
                    distanceTravelledAchievement = 0f;
                    lastPlayerPosForDistance.set(player.getPosition());
                    // Fix #617: Reset all stale input flags so any key/mouse event buffered during
                    // the respawn countdown does not fire as a phantom action when play resumes.
                    // Mirrors the identical resets in the PLAYING respawn-completion block (Fix #609).
                    inputHandler.resetEscape();
                    inputHandler.resetPunch();
                    inputHandler.resetPunchHeld();
                    punchHeldTimer = 0f;
                    lastPunchTargetKey = null;
                    inputHandler.resetPlace();
                    inputHandler.resetInventory();
                    inputHandler.resetHelp();
                    inputHandler.resetCrafting();
                    inputHandler.resetAchievements();
                    inputHandler.resetQuestLog();
                    inputHandler.resetInteract();
                    inputHandler.resetJump();
                    inputHandler.resetDodge();
                    inputHandler.resetEnter();
                    inputHandler.resetUp();
                    inputHandler.resetDown();
                    inputHandler.resetHotbarSlot();
                    inputHandler.resetCraftingSlot();
                    inputHandler.resetLeftClick();
                    inputHandler.resetLeftClickReleased();
                    inputHandler.resetRightClick();
                    inputHandler.resetScroll();
                    // Fix #643: Clear polled movement flags so a held WASD/sprint key does not
                    // cause immediate unwanted movement on the first post-respawn PLAYING frame.
                    inputHandler.resetMovement();
                    // Fix #617: Close any UI overlays left open at the time of death so the player
                    // does not resume with inventory/crafting/help still showing (isUIBlocking() true).
                    inventoryUI.hide();
                    craftingUI.hide();
                    helpUI.hide();
                    achievementsUI.hide();
                    questLogUI.hide();
                    // Fix #625: Close and clear any active shop menu so isUIBlocking() returns false
                    // after respawn — mirrors the CINEMATIC branch (Fix #623) and PLAYING branch (Fix #601).
                    if (activeShopkeeperNPC != null) {
                        activeShopkeeperNPC.setShopMenuOpen(false);
                        activeShopkeeperNPC = null;
                    }
                    // Fix #631: Transition to PLAYING so the player is not left on the pause menu
                    // after the respawn countdown completes — mirrors the PAUSED arrest block (Fix #627)
                    // and the CINEMATIC respawn-completion block (Fix #623).
                    transitionToPlaying();
                }
                if (respawnSystem.isRespawning()) {
                    renderDeathScreen();
                }
            }
            // Fix #359: Advance reputation decay timer while paused — mirrors the PLAYING path (line ~1088).
            // Without this the player can exploit the pause menu to halt reputation decay indefinitely.
            player.getStreetReputation().update(delta);
            // Fix #361: Advance time system while paused so the clock, lighting, and day/night cycle
            // continue to progress — mirrors the equivalent block in the PLAYING branch.
            timeSystem.update(delta);
            npcManager.setGameTime(timeSystem.getTime());
            lightingSystem.updateLighting(timeSystem.getTime(), timeSystem.getSunriseTime(), timeSystem.getSunsetTime());
            updateSkyColour(timeSystem.getTime());
            clockHUD.update(timeSystem.getTime(), timeSystem.getDayCount(), timeSystem.getDayOfMonth(), timeSystem.getMonthName());
            // Fix #371: Call updatePoliceSpawning() while paused so the police spawn/despawn logic
            // stays in sync with the day/night cycle even when the game is paused around dusk or dawn.
            npcManager.updatePoliceSpawning(timeSystem.isNight(), world, player);
            // Fix #449: Call full npcManager.update() during the paused state so NPCs
            // continue walking patrol routes, police pursue the player, and all NPC
            // subsystems (spawn cooldown, post-arrest cooldown, knockback recovery,
            // KNOCKED_OUT recovery, speech timers) advance at the correct rate.
            // This replaces the five individual tickX() shim calls added by fixes
            // #393, #403, #405, #407, #423 — those methods are already called inside
            // update(), so keeping both would double-tick every timer.
            // Mirrors the approach taken for the CINEMATIC state in Fix #447.
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            // Issue #803: Advance wanted and notoriety systems while paused so police pursuit
            // timers and notoriety tier state continue to accumulate.
            wantedSystem.update(delta, player, npcManager.getNPCs(),
                    weatherSystem.getCurrentWeather(), timeSystem.isNight(), false,
                    type -> achievementSystem.unlock(type));
            notorietySystem.update(delta, player, type -> achievementSystem.unlock(type));
            rumourNetwork.update(npcManager.getNPCs(), delta);

            // Issue #862: Advance newspaper publication timer while paused — mirrors PLAYING branch.
            newspaperSystem.update(
                    delta,
                    timeSystem.getTime(),
                    timeSystem.getDayCount(),
                    notorietySystem,
                    wantedSystem,
                    rumourNetwork,
                    null,
                    factionSystem,
                    fenceSystem,
                    streetEconomySystem,
                    player.getCriminalRecord(),
                    npcManager.getNPCs(),
                    type -> achievementSystem.unlock(type));

            // Fix #381: Advance healing resting timer while paused so the 5-second threshold
            // continues to accumulate and healing is not artificially delayed on resume.
            healingSystem.update(delta, player);
            // Squat passive regen: +1 health/min when inside squat at Vibe >= 20
            if (squatSystem != null && squatSystem.isRegenActive(
                    player.getPosition().x, player.getPosition().z, 15f)) {
                player.heal(delta / 60f);
            }

            // Fix #899: Decrement market event cooldown while paused for scheduling consistency
            marketEventCooldown -= delta;

            // Fix #382: Advance gang territory linger timer while paused so the player cannot
            // exploit the pause menu to freeze the 5-second hostility escalation countdown.
            // Mirrors the pattern used for healing (#381), dodge (#379), reputation (#359),
            // weather (#341), and other timer-based systems in the PAUSED branch.
            gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, world);

            // Issue #866: Advance FenceSystem while paused — mirrors the PLAYING branch so that
            // police-avoidance, stock refresh, and contraband-run timers continue while paused.
            fenceSystem.update(delta, player, npcManager.getNPCs(), timeSystem.getDayIndex());

            // Fix #860: Advance StreetSkillSystem while paused — mirrors the PLAYING branch.
            player.getStreetSkillSystem().update(delta, player, npcManager.getNPCs());

            // Fix #391: Advance block/prop damage decay timers while paused so partially-damaged
            // blocks and props continue decaying toward zero. Without this, the player can exploit
            // the pause menu to freeze decay indefinitely and resume with the same damage level —
            // bypassing the decay mechanic entirely.
            blockBreaker.tickDecay(delta);
            propBreaker.tickDecay(delta);

            // Fix #399: Advance hunger, starvation, energy recovery, and weather exposure
            // while paused so the player cannot exploit the pause menu to avoid starving,
            // halt cold-snap health drain, or prevent energy recovery.
            // Sprint multiplier (3×) is NOT applied — the player is not moving while paused.
            // Mirrors the equivalent block in the PLAYING branch (~lines 596-628).
            if (!respawnSystem.isRespawning()) {
                player.updateHunger(delta);

                if (player.getHunger() <= 0) {
                    player.damage(5.0f * delta, DamageReason.STARVATION);
                }

                Weather pausedWeather = weatherSystem.getCurrentWeather();
                float pausedWeatherMultiplier = exposureSystem.getEffectiveEnergyDrainMultiplier(
                        pausedWeather, world, player.getPosition());
                player.recoverEnergy(delta / pausedWeatherMultiplier);

                if (exposureSystem.isExposedToWeatherDamage(pausedWeather, timeSystem.isNight(), world, player.getPosition())) {
                    float pausedHealthDrain = pausedWeather.getHealthDrainRate() * delta;
                    player.damage(pausedHealthDrain, DamageReason.WEATHER);
                }
            }

            // Fix #621: Check for player death triggered by starvation or weather damage above.
            // The PAUSED branch previously applied lethal damage but never called
            // checkAndTriggerRespawn(), so player.isDead() could become true while
            // isRespawning stayed false — permanently soft-locking the game.
            // Mirrors the PLAYING branch (line ~901) and CINEMATIC branch (line ~604).
            boolean pausedJustDied = respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
            if (pausedJustDied) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
                // Fix #621: Close and clear any active shop menu on death so isUIBlocking()
                // returns false after respawn — mirrors the identical block in the PLAYING branch (Fix #601).
                if (activeShopkeeperNPC != null) {
                    activeShopkeeperNPC.setShopMenuOpen(false);
                    activeShopkeeperNPC = null;
                }
            }

            // Fix #367: Process pending arrest even while paused so the flag doesn't persist as a ghost.
            // Without this, if police set arrestPending=true on the same frame the player opens ESC,
            // the flag is never evaluated until the game unpauses — causing an invisible "ghost arrest"
            // on the first PLAYING frame with no police NPC visible nearby.
            if (npcManager.isArrestPending() && !player.isDead()) {
                java.util.List<String> confiscated = arrestSystem.arrest(player, inventory);
                String arrestMsg = ArrestSystem.buildArrestMessage(confiscated);
                tooltipSystem.showMessage(arrestMsg, 4.0f);
                npcManager.clearArrestPending();
                greggsRaidSystem.reset();
                player.getStreetReputation().removePoints(15);
                healingSystem.resetPosition(player.getPosition());
                // Fix #619: Sync distance tracking after arrest teleport so the teleport distance
                // is not counted as walked distance — mirrors the identical resets in the
                // respawn-completion block (Fix #459).
                distanceTravelledAchievement = 0f;
                lastPlayerPosForDistance.set(player.getPosition());
                // Fix #613: Reset all event-driven input flags after arrest teleport so stale flags
                // do not fire as phantom actions on the first PLAYING frame post-arrest.
                // Mirrors the identical reset in the respawn path and PLAYING arrest branch.
                inputHandler.resetEscape();
                inputHandler.resetPunch();
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
                inputHandler.resetPlace();
                inputHandler.resetInventory();
                inputHandler.resetHelp();
                inputHandler.resetCrafting();
                inputHandler.resetAchievements();
                inputHandler.resetQuestLog();
                inputHandler.resetInteract();
                inputHandler.resetJump();
                inputHandler.resetDodge();
                inputHandler.resetEnter();
                inputHandler.resetUp();
                inputHandler.resetDown();
                inputHandler.resetHotbarSlot();
                inputHandler.resetCraftingSlot();
                inputHandler.resetLeftClick();
                inputHandler.resetLeftClickReleased();
                inputHandler.resetRightClick();
                inputHandler.resetScroll();
                // Fix #643: Clear polled movement flags so a held WASD/sprint key does not
                // cause immediate unwanted movement on the first post-arrest PLAYING frame.
                inputHandler.resetMovement();
                // Fix #615: Close any UI overlays left open at the time of arrest so
                // isUIBlocking() returns false on the first PLAYING frame post-arrest.
                // Mirrors the identical hide() calls in the respawn-completion block.
                inventoryUI.hide();
                craftingUI.hide();
                helpUI.hide();
                achievementsUI.hide();
                questLogUI.hide();
                // Fix #615: Close and clear any active shop menu so isUIBlocking() returns
                // false after arrest. Mirrors the justDied path (fix #601).
                if (activeShopkeeperNPC != null) {
                    activeShopkeeperNPC.setShopMenuOpen(false);
                    activeShopkeeperNPC = null;
                }
                // Fix #627: Transition to PLAYING after arrest so the player is not
                // left stranded on the pause menu. Mirrors the CINEMATIC and PLAYING
                // arrest branches which both leave the player in an interactive state.
                transitionToPlaying();
            }

            // Render UI and pause menu
            renderUI();
            pauseMenu.render(spriteBatch, shapeRenderer, font,
                           Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Handle pause menu input
            if (inputHandler.isEscapePressed()) {
                handleEscapePress();
                inputHandler.resetEscape();
            }
            // Fix #481: Allow Q to toggle quest log while paused
            if (inputHandler.isQuestLogPressed()) {
                questLogUI.toggle();
                inputHandler.resetQuestLog();
            }
            // Fix #499: If the achievements overlay is visible, forward UP/DOWN to
            // the achievements list and swallow ENTER so it doesn't trigger a pause
            // menu action.  Mirrors the identical guard added for questLogUI by Fix #481.
            if (achievementsUI.isVisible()) {
                if (inputHandler.isUpPressed()) {
                    achievementsUI.scrollUp();
                    inputHandler.resetUp();
                }
                if (inputHandler.isDownPressed()) {
                    achievementsUI.scrollDown();
                    inputHandler.resetDown();
                }
                // Swallow ENTER so it doesn't trigger a pause menu action
                if (inputHandler.isEnterPressed()) {
                    inputHandler.resetEnter();
                }
            // Fix #481: If the quest log overlay is visible, forward UP/DOWN/ENTER
            // to the quest log instead of the pause menu so scroll input works correctly.
            } else if (questLogUI.isVisible()) {
                if (inputHandler.isUpPressed()) {
                    questLogUI.scrollUp();
                    inputHandler.resetUp();
                }
                if (inputHandler.isDownPressed()) {
                    questLogUI.scrollDown();
                    inputHandler.resetDown();
                }
                // Swallow ENTER so it doesn't trigger a pause menu action
                if (inputHandler.isEnterPressed()) {
                    inputHandler.resetEnter();
                }
            } else {
                if (inputHandler.isUpPressed()) {
                    pauseMenu.selectPrevious();
                    inputHandler.resetUp();
                }
                if (inputHandler.isDownPressed()) {
                    pauseMenu.selectNext();
                    inputHandler.resetDown();
                }
                if (inputHandler.isEnterPressed()) {
                    if (pauseMenu.isResumeSelected()) {
                        transitionToPlaying();
                    } else if (pauseMenu.isAchievementsSelected()) {
                        achievementsUI.toggle();
                    } else if (pauseMenu.isRestartSelected()) {
                        restartGame();
                    } else if (pauseMenu.isQuitSelected()) {
                        Gdx.app.exit();
                    }
                    inputHandler.resetEnter();
                }
            }
            // Mouse click support for pause menu — Fix #499: skip when achievements
            // overlay is open to prevent clicks on the overlay from passing through
            // to the pause menu hit-boxes underneath (which could trigger Quit).
            // Fix #529: also skip when quest log overlay is open for the same reason.
            if (inputHandler.isLeftClickPressed() && !achievementsUI.isVisible() && !questLogUI.isVisible()) {
                int sw = Gdx.graphics.getWidth();
                int sh = Gdx.graphics.getHeight();
                int clicked = pauseMenu.handleClick(inputHandler.getMouseX(), inputHandler.getMouseY(), sw, sh);
                if (clicked == 0) {
                    transitionToPlaying();
                } else if (clicked == 1) {
                    achievementsUI.toggle();
                } else if (clicked == 2) {
                    restartGame();
                } else if (clicked == 3) {
                    Gdx.app.exit();
                }
                inputHandler.resetLeftClick();
            } else if (inputHandler.isLeftClickPressed() && achievementsUI.isVisible()) {
                // Consume the click so it doesn't carry over to subsequent frames.
                inputHandler.resetLeftClick();
            } else if (inputHandler.isLeftClickPressed() && questLogUI.isVisible()) {
                // Fix #529: consume the click so it doesn't carry over to subsequent frames.
                inputHandler.resetLeftClick();
            }
        }
    }

    private void handleMenuInput() {
        // Handle menu navigation with arrow keys
        if (inputHandler.isUpPressed()) {
            mainMenuScreen.selectPrevious();
            inputHandler.resetUp();
        }
        if (inputHandler.isDownPressed()) {
            mainMenuScreen.selectNext();
            inputHandler.resetDown();
        }
        // Handle menu selection
        if (inputHandler.isEnterPressed()) {
            if (mainMenuScreen.isNewGameSelected()) {
                startNewGame();
            } else if (mainMenuScreen.isSkipIntroSelected()) {
                mainMenuScreen.toggleSkipIntro();
            } else if (mainMenuScreen.isQuitSelected()) {
                Gdx.app.exit();
            }
            inputHandler.resetEnter();
        }
        // Mouse click support for main menu
        if (inputHandler.isLeftClickPressed()) {
            int sw = Gdx.graphics.getWidth();
            int sh = Gdx.graphics.getHeight();
            int clicked = mainMenuScreen.handleClick(inputHandler.getMouseX(), inputHandler.getMouseY(), sw, sh);
            if (clicked == 0) {
                startNewGame();
            } else if (clicked == 1) {
                mainMenuScreen.toggleSkipIntro();
            } else if (clicked == 2) {
                Gdx.app.exit();
            }
            inputHandler.resetLeftClick();
        }
    }

    private void startNewGame() {
        // Issue #428: Cinematic and text opening sequence run simultaneously (overlaid).
        // Previously they played sequentially; now the text fades in over the fly-through.
        if (mainMenuScreen.isSkipIntroEnabled()) {
            // Skip both cinematic and text sequence — jump straight into gameplay
            state = GameState.PLAYING;
            openingSequence.start();
            openingSequence.skip();
        } else {
            // Start both the cinematic fly-through and the text sequence together
            state = GameState.CINEMATIC;
            cinematicCamera.start();
            openingSequence.start();
        }
        Gdx.input.setCursorCatched(false); // Free cursor during cinematic; re-catch when playing starts
        // Reset per-session counters
        if (greggsRaidSystem != null) {
            greggsRaidSystem.reset();
        }
    }

    /**
     * Called when the cinematic fly-through finishes (or is skipped).
     * Transitions to PLAYING; the text opening sequence is already running (started
     * alongside the cinematic in startNewGame() — Fix #428).
     */
    private void finishCinematic() {
        state = GameState.PLAYING;
        // Opening sequence was already started in startNewGame(); do not restart it.
        // Fix #593: Reset all input state so any key/mouse event buffered during the
        // ~8-second cinematic does not fire on the first PLAYING frame.
        // This mirrors the identical resets already applied in transitionToPlaying()
        // and restartGame() (fixes #567–#591).
        inputHandler.resetCraftingSlot();
        inputHandler.resetHotbarSlot();
        inputHandler.resetPunch();
        inputHandler.resetPunchHeld();
        // Fix #611: Reset punchHeldTimer and lastPunchTargetKey alongside resetPunchHeld(),
        // mirroring the identical pattern in transitionToPlaying(), transitionToPaused(), and
        // the justDied block — prevents phantom hold-punch on first PLAYING frame after cinematic
        punchHeldTimer = 0f;
        lastPunchTargetKey = null;
        inputHandler.resetPlace();
        inputHandler.resetInventory();
        inputHandler.resetHelp();
        inputHandler.resetCrafting();
        inputHandler.resetAchievements();
        inputHandler.resetQuestLog();
        inputHandler.resetScroll();
        inputHandler.resetInteract();
        inputHandler.resetJump();
        inputHandler.resetDodge();
        inputHandler.resetUp();
        inputHandler.resetDown();
        inputHandler.resetLeftClick();
        inputHandler.resetLeftClickReleased();
        // Fix #603: Clear stale rightClickPressed so a right-click buffered during the
        // cinematic does not fire a phantom right-click event on the first PLAYING frame
        inputHandler.resetRightClick();
        inputHandler.resetEscape();
        inputHandler.resetEnter();
        // Fix #643: Clear polled movement flags so a held WASD/sprint key does not cause
        // immediate unwanted movement on the first PLAYING frame after the cinematic ends.
        inputHandler.resetMovement();
        Gdx.input.setCursorCatched(true);
    }

    private void renderLoadingScreen() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        com.badlogic.gdx.math.Matrix4 proj = new com.badlogic.gdx.math.Matrix4();
        proj.setToOrtho2D(0, 0, screenWidth, screenHeight);
        spriteBatch.setProjectionMatrix(proj);

        spriteBatch.begin();
        font.getData().setScale(2.0f);
        font.setColor(0.8f, 0.8f, 0.8f, 1f);
        String msg = "Loading...";
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, msg);
        font.draw(spriteBatch, msg, (screenWidth - layout.width) / 2f, screenHeight / 2f + 20);
        font.getData().setScale(1.0f);
        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        String sub = "Generating British town...";
        com.badlogic.gdx.graphics.g2d.GlyphLayout subLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, sub);
        font.draw(spriteBatch, sub, (screenWidth - subLayout.width) / 2f, screenHeight / 2f - 20);
        font.getData().setScale(1.2f);
        font.setColor(1f, 1f, 1f, 1f);
        spriteBatch.end();
    }

    private void renderMenu() {
        // Clear the screen before rendering menu
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Set up orthographic projection for 2D menu rendering
        com.badlogic.gdx.math.Matrix4 projection = new com.badlogic.gdx.math.Matrix4();
        projection.setToOrtho2D(0, 0, screenWidth, screenHeight);
        spriteBatch.setProjectionMatrix(projection);
        shapeRenderer.setProjectionMatrix(projection);

        mainMenuScreen.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
    }

    private void handleUIInput() {
        // Inventory toggle
        if (inputHandler.isInventoryPressed()) {
            boolean wasVisible = inventoryUI.isVisible();
            inventoryUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetInventory();
            // Fix #275: Clear sticky punch state when a UI overlay opens
            if (!wasVisible) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        }

        // Help toggle
        if (inputHandler.isHelpPressed()) {
            boolean wasVisible = helpUI.isVisible();
            helpUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetHelp();
            // Fix #275: Clear sticky punch state when a UI overlay opens
            if (!wasVisible) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        }

        // Crafting toggle
        if (inputHandler.isCraftingPressed()) {
            boolean wasVisible = craftingUI.isVisible();
            craftingUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetCrafting();
            // Fix #275: Clear sticky punch state when a UI overlay opens
            if (!wasVisible) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        }

        // Achievements toggle (Tab key)
        if (inputHandler.isAchievementsPressed()) {
            boolean wasVisible = achievementsUI.isVisible();
            achievementsUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetAchievements();
            // Clear sticky punch state when the achievements overlay opens
            if (!wasVisible) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        }

        // Quest log toggle (Q key)
        if (inputHandler.isQuestLogPressed()) {
            boolean wasVisible = questLogUI.isVisible();
            questLogUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetQuestLog();
            // Clear sticky punch state when the quest log overlay opens
            if (!wasVisible) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        }

        // Criminal record toggle (R key)
        if (inputHandler.isCriminalRecordPressed()) {
            boolean wasVisible = criminalRecordUI.isVisible();
            criminalRecordUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetCriminalRecord();
            if (!wasVisible) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        }

        // Issue #850: Skills toggle (K key)
        if (inputHandler.isSkillsPressed()) {
            boolean wasVisible = skillsUI.isVisible();
            skillsUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetSkills();
            if (!wasVisible) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        }

        // Release cursor when any overlay UI is open, re-catch when all closed
        boolean uiOpen = inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible()
                || achievementsUI.isVisible() || questLogUI.isVisible() || criminalRecordUI.isVisible()
                || skillsUI.isVisible()
                || (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen())
                || fenceTradeUI.isVisible();
        Gdx.input.setCursorCatched(!uiOpen);

        // Hotbar selection
        int hotbarSlot = inputHandler.getHotbarSlotPressed();
        if (hotbarSlot >= 0) {
            // Issue #783: if broadcasting, route keys 1-4 to broadcast action selection
            if (pirateRadioSystem != null && pirateRadioSystem.isBroadcasting() && hotbarSlot <= 3) {
                PirateRadioSystem.BroadcastAction action =
                        PirateRadioSystem.BroadcastAction.fromNumber(hotbarSlot + 1);
                if (action != null) {
                    String tooltip = pirateRadioSystem.executePlayerAction(action, npcManager.getNPCs());
                    if (tooltip != null && !tooltip.isEmpty()) {
                        tooltipSystem.showMessage(tooltip, 3.0f);
                    }
                }
                inputHandler.resetCraftingSlot();
            // Issue #541: if a shopkeeper shop menu is open, intercept keys 1-3 (slots 0-2)
            // and route them to item selection instead of changing the hotbar.
            } else if (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen() && hotbarSlot <= 2) {
                interactionSystem.selectShopItem(activeShopkeeperNPC, hotbarSlot + 1, inventory);
                inputHandler.resetCraftingSlot(); // Issue #543: prevent stale craftingSlotPressed
            } else if (!craftingUI.isVisible()) {
                hotbarUI.selectSlot(hotbarSlot);
                inputHandler.resetCraftingSlot(); // Issue #545: discard stale craftingSlotPressed when crafting UI is not open
            }
            inputHandler.resetHotbarSlot();
        }

        // Issue #858: E key during active MC battle — route to battle action press.
        // Phase 11: E key interaction — only when no UI overlay is open,
        // but allow E through when the shop menu is open so the purchase confirmation works.
        if (inputHandler.isInteractPressed()) {
            if (mcBattleSystem != null && mcBattleSystem.isBattleActive()) {
                // Battle is active: consume the press as a hit attempt on the bar
                String hitMsg = mcBattleSystem.pressAction(npcManager.getNPCs());
                if (hitMsg != null && !hitMsg.isEmpty()) {
                    tooltipSystem.showMessage(hitMsg, 3.0f);
                }
            } else {
                boolean shopMenuOpen = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();
                if (!isUIBlocking() || shopMenuOpen) {
                    handleInteraction();
                }
            }
            inputHandler.resetInteract();
        }

        // Issue #781: T key — spray graffiti when holding a SPRAY_CAN
        if (inputHandler.isTagPressed()) {
            if (!isUIBlocking()) {
                handleGraffitiSpray();
            }
            inputHandler.resetTag();
        }

        // Issue #783: B key — toggle pirate radio broadcast start/stop
        if (inputHandler.isBroadcastPressed()) {
            if (!isUIBlocking() && pirateRadioSystem != null) {
                if (pirateRadioSystem.isBroadcasting()) {
                    pirateRadioSystem.stopBroadcast();
                } else {
                    com.badlogic.gdx.math.Vector3 pos = player.getPosition();
                    pirateRadioSystem.startBroadcast(pos.x, pos.y, pos.z);
                }
            }
            inputHandler.resetBroadcast();
        }

        // Handle mouse clicks on UI overlays
        int screenHeight = Gdx.graphics.getHeight();
        if (inputHandler.isLeftClickPressed()) {
            if (inventoryUI.isVisible()) {
                inventoryUI.handleClick(inputHandler.getMouseX(), inputHandler.getMouseY(), screenHeight);
            }
            if (craftingUI.isVisible()) {
                craftingUI.handleClick(inputHandler.getMouseX(), inputHandler.getMouseY(), screenHeight);
            }
            inputHandler.resetLeftClick();
        }

        // Handle drag position update
        if (inventoryUI.isDragging()) {
            inventoryUI.updateDragPosition(inputHandler.getMouseX(), inputHandler.getMouseY());
        }

        // Handle mouse release (drop)
        if (inputHandler.isLeftClickReleased()) {
            if (inventoryUI.isDragging()) {
                int uiY = screenHeight - inputHandler.getMouseY();
                // Check if dropping onto hotbar
                int hotbarTarget = hotbarUI.getSlotAt(inputHandler.getMouseX(), uiY);
                if (hotbarTarget >= 0) {
                    int sourceSlot = inventoryUI.getDragSlotForHotbarDrop();
                    if (sourceSlot >= 0) {
                        inventory.swapSlots(sourceSlot, hotbarTarget);
                    }
                } else {
                    inventoryUI.handleRelease(inputHandler.getMouseX(), inputHandler.getMouseY(), screenHeight);
                }
            }
            inputHandler.resetLeftClickReleased();
        }

        // Crafting menu controls
        if (craftingUI.isVisible()) {
            int craftingSlot = inputHandler.getCraftingSlotPressed();
            if (craftingSlot >= 0) {
                craftingUI.selectRecipe(craftingSlot);
                inputHandler.resetCraftingSlot();
            }

            if (inputHandler.isEnterPressed()) {
                boolean crafted = craftingUI.craftSelected();
                // Phase 11: Trigger first craft tooltip
                if (crafted && !tooltipSystem.hasShown(TooltipTrigger.FIRST_CRAFT)) {
                    tooltipSystem.trigger(TooltipTrigger.FIRST_CRAFT);
                }
                inputHandler.resetEnter();
            }

            float scrollY = inputHandler.getScrollAmountY();
            if (scrollY != 0) {
                craftingUI.handleScroll(scrollY);
                inputHandler.resetScroll();
            }
        } else if (achievementsUI.isVisible()) {
            // Forward UP/DOWN to achievements scroll; discard scroll wheel
            if (inputHandler.isUpPressed()) {
                achievementsUI.scrollUp();
                inputHandler.resetUp();
            }
            if (inputHandler.isDownPressed()) {
                achievementsUI.scrollDown();
                inputHandler.resetDown();
            }
            inputHandler.resetScroll();
        } else if (questLogUI.isVisible()) {
            // Forward UP/DOWN to quest log scroll; discard scroll wheel
            if (inputHandler.isUpPressed()) {
                questLogUI.scrollUp();
                inputHandler.resetUp();
            }
            if (inputHandler.isDownPressed()) {
                questLogUI.scrollDown();
                inputHandler.resetDown();
            }
            inputHandler.resetScroll();
        } else if (inventoryUI.isVisible() || helpUI.isVisible()) {
            // Discard scroll when inventory or help UI is open so it doesn't carry over to the hotbar
            inputHandler.resetScroll();
        } else if (!isUIBlocking()) {
            // Scroll wheel cycles hotbar slot when no UI overlay is open
            float scrollY = inputHandler.getScrollAmountY();
            if (scrollY != 0) {
                int current = hotbarUI.getSelectedSlot();
                int next = (int) ((current + Math.signum(scrollY) + HotbarUI.HOTBAR_SLOTS) % HotbarUI.HOTBAR_SLOTS);
                hotbarUI.selectSlot(next);
                inputHandler.resetScroll();
            }
        }
    }

    private boolean isUIBlocking() {
        return inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible()
                || achievementsUI.isVisible() || questLogUI.isVisible() || skillsUI.isVisible()
                || (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen())
                || fenceTradeUI.isVisible();
    }

    /**
     * Fix #198: World-simulation update — runs every frame regardless of UI state.
     * NPCs, gang territory, arrest system, reputation decay, particles, and chunk
     * loading must not freeze when the player has a UI overlay (inventory/crafting/help) open.
     */
    private void updatePlayingSimulation(float delta) {
        // Fix #202: Apply gravity and vertical collision unconditionally so the player
        // does not float mid-air when a UI overlay (inventory/help/crafting) is open.
        // Fix #273: Skip gravity/fall-damage when the player is already dead to prevent
        // damage-flash strobing on the death screen and corpse drift during respawn countdown.
        if (!player.isDead()) {
            world.applyGravityAndVerticalCollision(player, delta);
            // Fix #389: advance dodge timers unconditionally so opening a UI overlay
            // (inventory, help, crafting) cannot freeze the dodge timer or cooldown,
            // preventing invincibility extension and cooldown bypass exploits.
            player.updateDodge(delta);
        }

        // Decay partially-damaged blocks and props that have not been hit recently
        blockBreaker.tickDecay(delta);
        propBreaker.tickDecay(delta);

        // Fix #776: Check whether the player has stepped into a landmark area this frame.
        // This allows EXPLORE quests to progress purely by walking into the target building
        // without requiring the player to find and speak to an NPC inside it.
        interactionSystem.checkPlayerPosition(player.getPosition(), world);

        // Update loaded chunks based on player position; remove renderer models for unloaded chunks
        java.util.Set<String> unloadedChunkKeys = world.updateLoadedChunks(player.getPosition());
        for (String key : unloadedChunkKeys) {
            chunkRenderer.removeChunkByKey(key);
        }

        // Rebuild meshes for newly loaded chunks (budget: max 16 per frame to prevent freezes)
        List<Chunk> dirtyChunks = world.getDirtyChunks();
        if (!dirtyChunks.isEmpty()) {
            int meshBudget = 16;
            int built = 0;
            java.util.Iterator<Chunk> it = dirtyChunks.iterator();
            while (it.hasNext() && built < meshBudget) {
                Chunk chunk = it.next();
                chunkRenderer.updateChunk(chunk, meshBuilder);
                world.markChunkClean(chunk);
                built++;
            }
        }

        // Issue #547: If the player has walked out of interaction range of the active
        // shopkeeper, close the shop menu and clear the reference so that 1/2/3 keys
        // are no longer intercepted for item selection.
        if (activeShopkeeperNPC != null) {
            float distToShopkeeper = player.getPosition().dst(activeShopkeeperNPC.getPosition());
            if (distToShopkeeper > InteractionSystem.INTERACTION_RANGE) {
                activeShopkeeperNPC.setShopMenuOpen(false);
                activeShopkeeperNPC = null;
            }
        }
        // Fix #551: If the NPC's own speech timer expired and closed the shop menu,
        // clear the stale reference so that the next E-press is treated as a first
        // press (reopening the menu) rather than erroneously executing a purchase.
        if (activeShopkeeperNPC != null && !activeShopkeeperNPC.isShopMenuOpen()) {
            activeShopkeeperNPC = null;
        }

        // Issue #662: Update car traffic
        // Issue #884: Pass NPC list so cars can knock back and damage NPCs on collision
        if (!player.isDead()) {
            carManager.update(delta, player, npcManager.getNPCs());
        }

        // Phase 5: Update NPCs
        npcManager.update(delta, world, player, inventory, tooltipSystem);

        // Issue #842: Apply weather-driven NPC behaviour once per second
        weatherNPCTimer += delta;
        if (weatherNPCTimer >= 1.0f) {
            weatherNPCBehaviour.applyWeatherBehaviour(npcManager.getNPCs(), weatherSystem.getCurrentWeather());
            weatherNPCTimer = 0f;
        }

        // Fix #196: update speech log after NPC speech is set for this frame
        speechLogUI.update(npcManager.getNPCs(), delta);

        // Issue #818: Update disguise system — scrutiny decay, cover integrity, NPC freeze-stare
        disguiseSystem.update(delta, player, npcManager.getNPCs(), player.getVelocity().len());

        // Issue #803: Update wanted system — drives police NPC state transitions (CHASING, ALERTED)
        // and spawns reinforcements based on witnessed crimes.
        wantedSystem.update(delta, player, npcManager.getNPCs(),
                weatherSystem.getCurrentWeather(), timeSystem.isNight(), false,
                type -> achievementSystem.unlock(type));

        // Issue #803: Update notoriety system — controls helicopter sweep timer at Tier 3+
        // and tier-up flash animations.
        notorietySystem.update(delta, player, type -> achievementSystem.unlock(type));

        // Issue #816: Update neighbourhood watch system — decays anger, manages tier escalation
        {
            Weather w = weatherSystem.getCurrentWeather();
            neighbourhoodWatchSystem.update(delta,
                w == Weather.RAIN || w == Weather.DRIZZLE || w == Weather.THUNDERSTORM,
                w == Weather.FOG,
                type -> achievementSystem.unlock(type));
        }

        // Issue #803: Update rumour network — spreads NPC gossip and police tips.
        rumourNetwork.update(npcManager.getNPCs(), delta);

        // Issue #811: Update faction system — mission timers, turf transfers, NPC hostility
        factionSystem.update(delta, player, npcManager.getNPCs());

        // Issue #781: Update graffiti system — fade timers, NPC crew spray, turf pressure, passive income
        graffitiSystem.update(delta, timeSystem.getTimeSpeed() * delta / 24f, npcManager.getNPCs(),
                factionSystem.getTurfMap(), wantedSystem, noiseSystem, rumourNetwork, inventory,
                type -> achievementSystem.unlock(type));

        // Issue #824: Update street economy system — NPC needs accumulation, market events, racket income
        streetEconomySystem.update(delta, npcManager.getNPCs(), player,
                weatherSystem.getCurrentWeather(),
                notorietySystem.getTier(),
                inventory, rumourNetwork,
                type -> achievementSystem.unlock(type));

        // Fix #899: Random market event scheduler — decrement cooldown and fire when ready
        marketEventCooldown -= delta;
        if (marketEventCooldown <= 0f && streetEconomySystem.getActiveEvent() == null) {
            // Pick a random MarketEvent excluding GREGGS_STRIKE (handled by NewspaperSystem)
            MarketEvent[] candidates = java.util.Arrays.stream(MarketEvent.values())
                    .filter(e -> e != MarketEvent.GREGGS_STRIKE)
                    .toArray(MarketEvent[]::new);
            MarketEvent event = candidates[new java.util.Random().nextInt(candidates.length)];
            streetEconomySystem.triggerMarketEvent(event, npcManager.getNPCs(), rumourNetwork);
            marketEventCooldown = 120f + new java.util.Random().nextFloat() * 180f;
        }

        // Issue #862: Advance newspaper publication timer — fires daily edition at 18:00, triggers
        // market events (GREGGS_STRIKE etc.), spreads rumours, and enables the pickUpNewspaper() path.
        newspaperSystem.update(
                delta,
                timeSystem.getTime(),
                timeSystem.getDayCount(),
                notorietySystem,
                wantedSystem,
                rumourNetwork,
                null,
                factionSystem,
                fenceSystem,
                streetEconomySystem,
                player.getCriminalRecord(),
                npcManager.getNPCs(),
                type -> achievementSystem.unlock(type));

        // Issue #826: Update witness system — evidence props, witness NPC timers, CCTV tape countdowns
        witnessSystem.update(delta, npcManager.getNPCs(), player);

        // Issue #866: Advance FenceSystem — refreshes daily rotating stock, runs police-avoidance
        // logic, counts down contraband-run timers, and decrements the post-failure lock countdown.
        fenceSystem.update(delta, player, npcManager.getNPCs(), timeSystem.getDayIndex());

        // Issue #828: Update JobCentre system — sign-on window, sanctions, debt collector
        jobCentreSystem.update(delta, player, npcManager.getNPCs());

        // Issue #830: Update BootSale system — lot schedule, NPC bidders, police spawn
        if (bootSaleSystem != null) {
            bootSaleSystem.update(delta, player, npcManager.getNPCs());
            // Poll last tooltip to surface auction messages to the player
            String bsMsg = bootSaleSystem.getLastTooltip();
            if (bsMsg != null && !bsMsg.isEmpty()) {
                tooltipSystem.showMessage(bsMsg, 3.0f);
            }
        }

        // Issue #832: Fire PropertySystem daily tick — decay, passive income, council rates
        if (propertySystem != null) {
            java.util.List<String> propDayMsgs = propertySystem.onDayTick(
                    timeSystem.getDayCount(), inventory,
                    factionSystem.getTurfMap(), npcManager.getNPCs());
            for (String msg : propDayMsgs) {
                tooltipSystem.showMessage(msg, 3.0f);
            }
            // Poll per-frame tooltip (e.g. cap warning)
            String propMsg = propertySystem.pollTooltip();
            if (propMsg != null && !propMsg.isEmpty()) {
                tooltipSystem.showMessage(propMsg, 3.0f);
            }
        }

        // Issue #799: Update corner shop economy — customer traffic, heat, faction rivalries
        if (cornerShopSystem != null) {
            cornerShopSystem.update(delta, npcManager.getNPCs(), player, inventory,
                    factionSystem, notorietySystem, rumourNetwork,
                    player.getStreetSkillSystem(), achievementSystem::unlock);
        }

        // Issue #837: Update stall economy — customers, inspector, weather, faction modifiers
        if (stallSystem != null) {
            stallSystem.update(
                    delta,
                    npcManager.getNPCs(),
                    player,
                    weatherSystem.getCurrentWeather(),
                    inventory,
                    factionSystem,
                    notorietySystem,
                    player.getCriminalRecord(),
                    achievementSystem::unlock,
                    streetEconomySystem);
            String stallMsg = stallSystem.pollTooltip();
            if (stallMsg != null) tooltipSystem.showMessage(stallMsg, 3.0f);
        }

        // Issue #783: Update pirate radio system — triangulation, action timers, signal van spawn
        if (pirateRadioSystem != null) {
            boolean policeNearby = false;
            com.badlogic.gdx.math.Vector3 playerPos = player.getPosition();
            for (ragamuffin.entity.NPC npc : npcManager.getNPCs()) {
                if ((npc.getType() == ragamuffin.entity.NPCType.POLICE
                        || npc.getType() == ragamuffin.entity.NPCType.ARMED_RESPONSE)
                        && npc.getPosition().dst(playerPos) <= PirateRadioSystem.POLICE_DETECTION_RANGE) {
                    policeNearby = true;
                    break;
                }
            }
            pirateRadioSystem.update(delta, npcManager.getNPCs(), policeNearby,
                    playerPos.x, playerPos.y, playerPos.z);
        }

        // Issue #844: Update heist system — alarm timers, execution countdown, CCTV exposure, hot-loot ageing
        if (heistSystem != null) {
            heistSystem.update(delta, player, noiseSystem, npcManager, factionSystem,
                    rumourNetwork, npcManager.getNPCs(), world, timeSystem.isNight());
        }

        // Issue #793 / #846: Update neighbourhood system — building decay, gentrification, Vibes
        if (neighbourhoodSystem != null) {
            neighbourhoodSystem.update(delta, world, npcManager.getNPCs(),
                    notorietySystem.getNotoriety(),
                    player.getPosition().x, player.getPosition().z);
            neighbourhoodSystem.checkMarchettiShutters(world);
            String nbTip = neighbourhoodSystem.pollTooltip();
            if (nbTip != null) tooltipSystem.showMessage(nbTip, 3.0f);
        }

        // Issue #848: Update squat system — daily tick (income, vibe decay, raid checks)
        if (squatSystem != null) {
            squatSystem.tickDay(timeSystem.getDayIndex(), notorietySystem.getTier(),
                    npcManager.getNPCs(), inventory);
        }

        // Issue #856: Championship ladder daily tick — +5 Notoriety if player is rank 1
        if (championshipLadder != null && notorietySystem != null) {
            int currentDay = timeSystem.getDayIndex();
            if (currentDay > championshipLastProcessedDay) {
                championshipLastProcessedDay = currentDay;
                if (championshipLadder.isChampion("Player")) {
                    notorietySystem.addNotoriety(5, achievementSystem::unlock);
                }
            }
        }

        // Issue #848: Update MC battle system — advance battle bar each frame
        if (mcBattleSystem != null) {
            mcBattleSystem.update(delta);

            // Issue #856: After each MC Battle resolves, update the championship ladder
            MCBattleSystem.Champion resolvedChampion = mcBattleSystem.pollLastResolvedChampion();
            if (resolvedChampion != null && championshipLadder != null) {
                boolean playerWon = mcBattleSystem.wasLastResolvedPlayerWin();
                String opponentName = mcChampionDisplayName(resolvedChampion);
                // Ensure opponent is on the ladder
                championshipLadder.registerFighter(opponentName);
                String winnerName = playerWon ? "Player" : opponentName;
                String loserName  = playerWon ? opponentName : "Player";
                championshipLadder.updateAfterFight(winnerName, loserName);

                // If player is now champion, award belt and show tooltip
                if (championshipLadder.isChampion("Player")) {
                    if (inventory.getItemCount(ragamuffin.building.Material.CHAMPIONSHIP_BELT) == 0) {
                        inventory.addItem(ragamuffin.building.Material.CHAMPIONSHIP_BELT, 1);
                        tooltipSystem.showMessage("You're the champion! Championship Belt added to inventory.", 4.0f);
                    }
                }

                // If Vinnie reaches rank 1, apply -15 respect penalty to Marchetti faction
                if (ChampionshipLadder.VINNIE_NAME.equals(championshipLadder.getChampionName())) {
                    factionSystem.applyRespectDelta(ragamuffin.core.Faction.MARCHETTI_CREW, -15);
                    tooltipSystem.showMessage("Vinnie is champion. Marchetti's running the ladder now.", 3.0f);
                }
            }
        }

        // Issue #848: Update rave system — income accumulation, police alert threshold
        if (raveSystem != null && squatSystem != null) {
            int attendees = squatSystem.countAttendeesInSquat(npcManager.getNPCs());
            raveSystem.update(delta, inventory, attendees);

            // Wire police spawn when rave crosses alert threshold
            if (raveSystem.isPoliceAlerted() && !ravePoliceSpawned) {
                ravePoliceSpawned = true;
                // Spawn 2 PCSO NPCs near the squat entrance
                int sqX = squatSystem.getSquatWorldX();
                int sqZ = squatSystem.getSquatWorldZ();
                float sqY = calculateSpawnHeight(world, sqX, sqZ);
                npcManager.spawnNPC(ragamuffin.entity.NPCType.PCSO, sqX + 2f, sqY, sqZ + 2f);
                npcManager.spawnNPC(ragamuffin.entity.NPCType.PCSO, sqX - 2f, sqY, sqZ + 2f);
                tooltipSystem.showMessage("The feds have been called to the rave. Scarper!", 4.0f);
            }
            // Reset spawn flag when rave ends
            if (!raveSystem.isRaveActive()) {
                ravePoliceSpawned = false;
            }
        }

        // Issue #844: Daily heist reset — when clock crosses 06:00
        if (heistSystem != null && prevTimeForHeistReset >= 0f) {
            float currentTime = timeSystem.getTime();
            // Detect crossing of 06:00 (handles normal progress and midnight wrap)
            boolean crossedSix = (prevTimeForHeistReset < 6.0f && currentTime >= 6.0f)
                    || (prevTimeForHeistReset > currentTime && currentTime >= 6.0f);
            if (crossedSix) {
                heistSystem.resetDaily();
            }
        }
        prevTimeForHeistReset = timeSystem.getTime();

        // Issue #26: Update gang territory system
        gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, world);

        // Fix #860: Advance StreetSkillSystem per-frame so the RALLY perk timer ticks,
        // followers auto-disperse, cooldowns decrement, and follower deterrence is applied.
        player.getStreetSkillSystem().update(delta, player, npcManager.getNPCs());

        // CRITIC 5: Handle police arrest — apply penalties if player was caught
        if (npcManager.isArrestPending() && !player.isDead()) {
            java.util.List<String> confiscated = arrestSystem.arrest(player, inventory);
            String arrestMsg = ArrestSystem.buildArrestMessage(confiscated);
            tooltipSystem.showMessage(arrestMsg, 4.0f);
            npcManager.clearArrestPending();
            // CRITIC 3: Arrest clears the active Greggs raid — police have resolved the incident
            greggsRaidSystem.reset();
            // Issue #7: Arrest reduces reputation — the streets forget when you're locked up
            player.getStreetReputation().removePoints(15);
            // Issue #166: Sync HealingSystem position after teleport so the next update()
            // does not compute a spurious speed from the arrest teleport distance.
            healingSystem.resetPosition(player.getPosition());
            // Fix #619: Sync distance tracking after arrest teleport so the teleport distance
            // is not counted as walked distance — mirrors the identical resets in the
            // respawn-completion block (Fix #459).
            distanceTravelledAchievement = 0f;
            lastPlayerPosForDistance.set(player.getPosition());
            // Fix #613: Reset all event-driven input flags after arrest teleport so stale flags
            // (e.g. punchHeld=true, leftClickPressed) do not fire as phantom actions on the
            // first PLAYING frame post-arrest. Mirrors the identical reset in the respawn path.
            inputHandler.resetEscape();
            inputHandler.resetPunch();
            inputHandler.resetPunchHeld();
            punchHeldTimer = 0f;
            lastPunchTargetKey = null;
            inputHandler.resetPlace();
            inputHandler.resetInventory();
            inputHandler.resetHelp();
            inputHandler.resetCrafting();
            inputHandler.resetAchievements();
            inputHandler.resetQuestLog();
            inputHandler.resetInteract();
            inputHandler.resetJump();
            inputHandler.resetDodge();
            inputHandler.resetEnter();
            inputHandler.resetUp();
            inputHandler.resetDown();
            inputHandler.resetHotbarSlot();
            inputHandler.resetCraftingSlot();
            inputHandler.resetLeftClick();
            inputHandler.resetLeftClickReleased();
            inputHandler.resetRightClick();
            inputHandler.resetScroll();
            // Fix #643: Clear polled movement flags so a held WASD/sprint key does not
            // cause immediate unwanted movement on the first post-arrest PLAYING frame.
            inputHandler.resetMovement();
            // Fix #615: Close any UI overlays left open at the time of arrest so
            // isUIBlocking() returns false on the first PLAYING frame post-arrest.
            // Mirrors the identical hide() calls in the respawn-completion block.
            inventoryUI.hide();
            craftingUI.hide();
            helpUI.hide();
            achievementsUI.hide();
            questLogUI.hide();
            // Fix #615: Close and clear any active shop menu so isUIBlocking() returns
            // false after arrest. Mirrors the justDied path (fix #601).
            if (activeShopkeeperNPC != null) {
                activeShopkeeperNPC.setShopMenuOpen(false);
                activeShopkeeperNPC = null;
            }
        }

        // Issue #48: Passive reputation decay — "lying low" reduces reputation over time
        player.getStreetReputation().update(delta);

        // Issue #813: Update campfire system (extinguish in rain, sync positions)
        campfireSystem.update(world, weatherSystem.getCurrentWeather(), delta);

        // Issue #807: Update warmth/wetness survival system
        if (!player.isDead()) {
            boolean nearCampfire = campfireSystem.isNearCampfire(player.getPosition());
            boolean inCar = carDrivingSystem.isInCar();
            warmthSystem.update(player, weatherSystem.getCurrentWeather(), world,
                    delta, nearCampfire, inCar, inventory);
            // Issue #879: Trigger cold-avoidance guidance tooltips when warmth drops
            if (player.getWarmth() <= 50f) {
                tooltipSystem.trigger(TooltipTrigger.WARMTH_GETTING_COLD);
            }
            if (player.isWarmthDangerous()) {
                tooltipSystem.trigger(TooltipTrigger.WARMTH_DANGER);
            }
        }

        // Issue #842: Frost-slip — black ice on ROAD/PAVEMENT blocks during FROST
        if (!player.isDead()) {
            BlockType blockUnder = world.getBlockUnderPlayer(player);
            boolean onRoad = blockUnder == BlockType.ROAD || blockUnder == BlockType.PAVEMENT;
            float slipProb = WeatherNPCBehaviour.getFrostSlipProbabilityPerSecond(
                    weatherSystem.getCurrentWeather(), onRoad);
            if (slipProb > 0 && frostSlipRng.nextFloat() < slipProb * delta) {
                // Apply a small random knockback to simulate slipping on ice
                float angle = frostSlipRng.nextFloat() * (float) (2 * Math.PI);
                float slipStrength = 1.5f;
                player.getPosition().add(
                        (float) Math.cos(angle) * slipStrength * delta,
                        0,
                        (float) Math.sin(angle) * slipStrength * delta);
                tooltipSystem.showMessage("You slip on the icy road!", 2.0f);
            }
        }

        // Issue #816: G key — Grovel mechanic (hold G to reduce Watch Anger)
        if (Gdx.input.isKeyPressed(Keys.G) && state == GameState.PLAYING && !player.isDead()) {
            boolean done = neighbourhoodWatchSystem.grovelling(delta, type -> achievementSystem.unlock(type));
            if (done) tooltipSystem.showMessage("You grovel apologetically.", 2.0f);
        }
        gameHUD.setGrovelProgress(neighbourhoodWatchSystem.getGroveltProgress());
        // Issue #816: Pass Watch Anger and tier to HUD for display
        gameHUD.setWatchAnger(neighbourhoodWatchSystem.getWatchAnger());
        gameHUD.setWatchTier(neighbourhoodWatchSystem.getCurrentTier());

        // Issue #171: Update particle system
        particleSystem.update(delta);

        // Issue #658: Advance flag wave animation
        flagRenderer.update(delta);

        // Fix #387: Advance arm swing animation unconditionally so a mid-punch swing
        // completes rather than freezing in the extended position while a UI overlay
        // (inventory/help/crafting) is open. Mirrors the same fix already applied to
        // the PAUSED branch (Fix #339).
        firstPersonArm.update(delta);
        // Fix #652: Keep the arm's held-item display in sync with the hotbar selection.
        firstPersonArm.setHeldItem(hotbarUI.getSelectedItem());

        // Fix #305: Update damage flash timer and HUD unconditionally so the red vignette
        // and damage-reason banner always advance regardless of UI state or death.
        // Detect new damage event: flash was at full intensity this frame (just applied)
        float flashNow = player.getDamageFlashIntensity();
        if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
            gameHUD.showDamageReason(player.getLastDamageReason());
        }
        prevDamageFlashIntensity = flashNow;
        player.updateFlash(delta);
        gameHUD.update(delta);

        // Fix #459: Accumulate distance travelled for MARATHON_MAN achievement.
        // Measure displacement since last frame, accumulate in metres, and award
        // one increment per whole metre so the 1000-increment target is reached
        // after 1000 metres walked.
        if (lastPlayerPosForDistance != null) {
            float dist = player.getPosition().dst(lastPlayerPosForDistance);
            distanceTravelledAchievement += dist;
            while (distanceTravelledAchievement >= 1.0f) {
                achievementSystem.increment(ragamuffin.ui.AchievementType.MARATHON_MAN);
                distanceTravelledAchievement -= 1.0f;
            }
        }
        lastPlayerPosForDistance.set(player.getPosition());

        // Update camera to follow player (or car in driving mode)
        if (carDrivingSystem.isInCar() && carDrivingSystem.getCurrentCar() != null) {
            // Third-person chase camera behind and above the car
            Car drivenCar = carDrivingSystem.getCurrentCar();
            Vector3 carPos = drivenCar.getPosition();
            float carHeading = drivenCar.getHeading();

            // Smooth-follow: lerp the chase camera angle toward the car heading.
            // This lets the user SEE the car rotate on screen, giving clear turn-direction feedback.
            float chaseLerpSpeed = 4.0f; // higher = snappier follow
            // Compute shortest angular difference to avoid wrapping artifacts
            float angleDiff = carHeading - chaseCameraAngle;
            // Normalize to [-180, 180]
            angleDiff = ((angleDiff + 180f) % 360f + 360f) % 360f - 180f;
            chaseCameraAngle += angleDiff * Math.min(1f, chaseLerpSpeed * delta);
            // Keep in [0, 360)
            chaseCameraAngle = ((chaseCameraAngle % 360f) + 360f) % 360f;

            float cameraAngleRad = (float) Math.toRadians(chaseCameraAngle);
            float chaseDist = 8.0f;
            float chaseHeight = 4.0f;
            // Position camera behind the car (opposite to the smoothed camera angle)
            float behindX = -(float) Math.sin(cameraAngleRad) * chaseDist;
            float behindZ = -(float) Math.cos(cameraAngleRad) * chaseDist;
            camera.position.set(carPos.x + behindX, carPos.y + chaseHeight, carPos.z + behindZ);
            // Look at a point slightly above the car
            camera.lookAt(carPos.x, carPos.y + 1.5f, carPos.z);
            camera.up.set(Vector3.Y);

            // Sync cameraYaw/cameraPitch to the chase camera direction so the skybox
            // renders correctly aligned with the view.
            cameraYaw = (float) Math.toDegrees(Math.atan2(camera.direction.x, -camera.direction.z));
            cameraPitch = (float) Math.toDegrees(Math.asin(camera.direction.y));
        } else {
            camera.position.set(player.getPosition());
            camera.position.y += Player.EYE_HEIGHT;
        }

        camera.update();
    }

    /**
     * Fix #198: Player-input update — gated behind !isUIBlocking().
     * Movement, jump, dodge, punch, and block placement are suppressed while a UI
     * overlay is open so the player cannot accidentally act while using menus.
     */
    private void updatePlayingInput(float delta) {
        // Fix #553: Suppress punch and place while the shop menu is open so the player
        // cannot hit the shopkeeper or consume food mid-transaction.
        boolean shopMenuOpen = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();

        // Handle punching — single click fires immediately; holding repeats every PUNCH_REPEAT_INTERVAL
        if (!shopMenuOpen) {
            if (inputHandler.isPunchPressed()) {
                handlePunch();
                inputHandler.resetPunch();
                punchHeldTimer = 0f; // reset repeat timer on fresh click
                // Capture current target so we can detect target changes
                RaycastResult _initTarget = blockBreaker.getTargetBlock(world, camera.position, camera.direction, PUNCH_REACH);
                int _initProp = findPropInReach(camera.position, camera.direction, PUNCH_REACH);
                if (_initTarget != null) {
                    lastPunchTargetKey = _initTarget.getBlockX() + "," + _initTarget.getBlockY() + "," + _initTarget.getBlockZ();
                } else if (_initProp >= 0) {
                    lastPunchTargetKey = "prop:" + _initProp;
                } else {
                    lastPunchTargetKey = null;
                }
            } else if (inputHandler.isPunchHeld()) {
                // Fix #279: Check for block, NPC, and prop targets so hold-to-punch fires for all.
                // Issue #752: Props are now a valid hold target.
                RaycastResult heldTarget = blockBreaker.getTargetBlock(world, camera.position, camera.direction, PUNCH_REACH);
                String currentTargetKey = (heldTarget != null) ? (heldTarget.getBlockX() + "," + heldTarget.getBlockY() + "," + heldTarget.getBlockZ()) : null;
                boolean hasNPCTarget = findNPCInReach(camera.position, camera.direction, PUNCH_REACH) != null;
                // Check for prop target (use prop index as key; prefix avoids collisions with block keys)
                int heldPropIndex = findPropInReach(camera.position, camera.direction, PUNCH_REACH);
                String propTargetKey = (heldPropIndex >= 0) ? ("prop:" + heldPropIndex) : null;
                // Merge: prefer block key if block is closer, otherwise use prop key
                if (currentTargetKey == null && propTargetKey != null) {
                    currentTargetKey = propTargetKey;
                } else if (currentTargetKey != null && propTargetKey != null) {
                    // Both present — keep whichever is closer (same logic as handlePunch)
                    float bd = heldTarget.getDistance();
                    List<PropPosition> heldProps = world.getPropPositions();
                    float pd = (heldPropIndex < heldProps.size()) ?
                        rayAABBIntersect(camera.position, camera.direction, heldProps.get(heldPropIndex).getAABB()) :
                        Float.MAX_VALUE;
                    if (pd >= 0f && pd < bd) {
                        currentTargetKey = propTargetKey;
                    }
                }
                // Reset timer only when the block/prop target changes (switched target or target→NPC/none).
                // Do NOT reset if an NPC is the target and currentTargetKey is null — that would
                // zero the timer every frame and prevent repeat hits on NPCs (the bug in #279).
                if (!hasNPCTarget && (currentTargetKey == null || !currentTargetKey.equals(lastPunchTargetKey))) {
                    punchHeldTimer = 0f;
                    lastPunchTargetKey = currentTargetKey;
                } else if (currentTargetKey != null && !currentTargetKey.equals(lastPunchTargetKey)) {
                    // Target changed (even while also facing an NPC — unlikely but correct)
                    punchHeldTimer = 0f;
                    lastPunchTargetKey = currentTargetKey;
                }
                // Fix #285: When aiming at an NPC with no block/prop target, clear any residual
                // block break progress immediately (not just on the next repeat tick).
                if (hasNPCTarget && currentTargetKey == null) {
                    gameHUD.setBlockBreakProgress(0f);
                }
                // Any valid target — block, prop, OR NPC — should tick the repeat timer
                if (currentTargetKey != null || hasNPCTarget) {
                    punchHeldTimer += delta;
                    if (punchHeldTimer >= PUNCH_REPEAT_INTERVAL) {
                        punchHeldTimer -= PUNCH_REPEAT_INTERVAL;
                        handlePunch();
                    }
                }
            } else {
                // Button released — reset timer and last target
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
        } else {
            // Shop menu is open — consume and discard any buffered punch/place inputs,
            // and reset the repeat timer so there is no burst when the menu closes.
            inputHandler.resetPunch();
            punchHeldTimer = 0f;
            lastPunchTargetKey = null;
        }

        // Handle block placement
        if (!shopMenuOpen && inputHandler.isPlacePressed()) {
            handlePlace();
            inputHandler.resetPlace();
        } else if (shopMenuOpen && inputHandler.isPlacePressed()) {
            inputHandler.resetPlace();
        }

        // Issue #773: Car driving — WASD controls the car instead of the player
        if (carDrivingSystem.isInCar()) {
            carDrivingSystem.update(delta, player,
                    inputHandler.isForward(), inputHandler.isBackward(),
                    inputHandler.isLeft(), inputHandler.isRight());
            // Suppress normal movement, jump, dodge while driving
            inputHandler.resetJump();
            inputHandler.resetDodge();
            return;
        }

        // Camera direction is already up-to-date (applied at top of frame)
        // Calculate movement direction from current camera facing (reuse vectors)
        tmpForward.set(camera.direction.x, 0, camera.direction.z).nor();
        tmpRight.set(camera.direction).crs(Vector3.Y).nor();
        tmpMoveDir.set(0, 0, 0);

        if (inputHandler.isForward()) {
            tmpMoveDir.add(tmpForward);
        }
        if (inputHandler.isBackward()) {
            tmpMoveDir.sub(tmpForward);
        }
        if (inputHandler.isRight()) {
            tmpMoveDir.add(tmpRight);
        }
        if (inputHandler.isLeft()) {
            tmpMoveDir.sub(tmpRight);
        }

        // Fix #885: Ladder climbing — W climbs up, S climbs down.
        // Overrides normal jump/gravity; applyGravityAndVerticalCollision handles the rest.
        if (world.isOnLadder(player)) {
            if (inputHandler.isForward()) {
                player.setVerticalVelocity(Player.CLIMB_SPEED);
            } else if (inputHandler.isBackward()) {
                player.setVerticalVelocity(-Player.CLIMB_SPEED);
            }
            // Suppress standard jump while on a ladder (Space still works to dismount)
            if (inputHandler.isJumpPressed()) {
                player.setVerticalVelocity(Player.CLIMB_SPEED);
                inputHandler.resetJump();
            }
        } else {
            // Jump
            if (inputHandler.isJumpPressed()) {
                if (world.isOnGround(player)) {
                    player.jump();
                }
                inputHandler.resetJump();
            }
        }

        // Dodge/roll (Left Ctrl while moving)
        if (inputHandler.isDodgePressed()) {
            if (tmpMoveDir.len2() > 0) {
                Vector3 dodgeDir = tmpMoveDir.cpy().nor();
                player.dodge(dodgeDir.x, dodgeDir.z);
                soundSystem.play(ragamuffin.audio.SoundEffect.PLAYER_DODGE);
            }
            inputHandler.resetDodge();
        }

        // Move player with collision (always call to ensure gravity applies even when not moving)
        float moveSpeed;
        if (player.isDodging()) {
            // During dodge, override direction and speed
            tmpMoveDir.set(player.getDodgeDirX(), 0, player.getDodgeDirZ());
            moveSpeed = Player.DODGE_SPEED;
        } else {
            if (tmpMoveDir.len2() > 0) {
                tmpMoveDir.nor();
            }
            moveSpeed = (inputHandler.isSprintHeld() && player.canSprint()) ? Player.SPRINT_SPEED : Player.MOVE_SPEED;
        }
        // Issue #807: Apply hypothermia speed penalty when warmth is dangerously low
        moveSpeed *= player.getWarmthSpeedMultiplier();
        world.moveWithCollision(player, tmpMoveDir.x, 0, tmpMoveDir.z, delta, moveSpeed);

        // Update footstep sounds based on movement
        boolean isMoving = tmpMoveDir.len2() > 0;
        BlockType blockUnderfoot = world.getBlockUnderPlayer(player);
        soundSystem.updateFootsteps(delta, isMoving, blockUnderfoot);

        // Issue #807: Update noise system and mirror result onto player
        noiseSystem.update(delta, isMoving, player.isCrouching());
        player.setNoiseLevel(noiseSystem.getNoiseLevel());

        // Issue #171: Footstep dust particles while moving on the ground
        if (isMoving && world.isOnGround(player)) {
            footstepDustTimer += delta;
            if (footstepDustTimer >= FOOTSTEP_DUST_INTERVAL) {
                footstepDustTimer = 0f;
                particleSystem.emitFootstepDust(
                    player.getPosition().x,
                    player.getPosition().y,
                    player.getPosition().z);
            }
        } else {
            footstepDustTimer = 0f;
        }

        // Issue #171: Dodge trail particles while dodge-rolling
        if (player.isDodging()) {
            particleSystem.emitDodgeTrail(
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z);
        }

        // Push player out of any NPC they're overlapping
        resolveNPCCollisions();
    }

    private void handlePunch() {
        // Determine equipped tool from hotbar
        int selectedSlot = hotbarUI.getSelectedSlot();
        Material equippedMaterial = inventory.getItemInSlot(selectedSlot);
        Material toolMaterial = (equippedMaterial != null && Tool.isTool(equippedMaterial)) ? equippedMaterial : null;

        // Check if punching an NPC first (reuse vectors)
        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        // Check for nearby NPCs in punch range
        NPC targetNPC = findNPCInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);
        if (targetNPC != null) {
            // Punch connects — animate, drain energy
            firstPersonArm.punch();
            player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);

            // Punch the NPC (knockback + loot on kill)
            boolean npcWasAlive = targetNPC.isAlive();
            npcManager.punchNPC(targetNPC, tmpDirection, inventory, tooltipSystem, player.getPosition(), world);
            soundSystem.play(ragamuffin.audio.SoundEffect.NPC_HIT);
            // Issue #171: Emit combat-hit sparks at the NPC's chest height
            particleSystem.emitCombatHit(
                targetNPC.getPosition().x,
                targetNPC.getPosition().y + NPC.HEIGHT * 0.5f,
                targetNPC.getPosition().z);
            // Clear block break progress when punching NPCs
            gameHUD.setBlockBreakProgress(0f);
            // Award street reputation for fighting (major crime)
            player.getStreetReputation().addPoints(2);
            // Issue #450: Achievement tracking — punching NPCs
            achievementSystem.unlock(ragamuffin.ui.AchievementType.FIRST_PUNCH);
            achievementSystem.increment(ragamuffin.ui.AchievementType.BRAWLER);
            // Issue #659: Criminal record — record NPC punch by type
            if (targetNPC.getType() == ragamuffin.entity.NPCType.PENSIONER) {
                player.getCriminalRecord().record(ragamuffin.core.CriminalRecord.CrimeType.PENSIONERS_PUNCHED);
                // Issue #816: Punching a civilian increases Watch Anger
                neighbourhoodWatchSystem.onPlayerPunchedCivilian();
            } else if (targetNPC.getType() == ragamuffin.entity.NPCType.PUBLIC) {
                player.getCriminalRecord().record(ragamuffin.core.CriminalRecord.CrimeType.MEMBERS_OF_PUBLIC_PUNCHED);
                // Issue #816: Punching a civilian increases Watch Anger
                neighbourhoodWatchSystem.onPlayerPunchedCivilian();
            } else if (targetNPC.getType() == ragamuffin.entity.NPCType.WATCH_MEMBER) {
                // Issue #816: Punching a Watch Member increases Watch Anger even more
                neighbourhoodWatchSystem.onPlayerPunchedWatchMember();
            }
            // Issue #659: Record NPC kill if the punch was lethal
            if (npcWasAlive && !targetNPC.isAlive()) {
                player.getCriminalRecord().record(ragamuffin.core.CriminalRecord.CrimeType.NPCS_KILLED);
                // Issue #818: Loot disguise from newly knocked-out NPC
                Material looted = disguiseSystem.lootDisguise(targetNPC, inventory);
                if (looted != null) {
                    tooltipSystem.showMessage("You take the " + looted.getDisplayName() + ".", 2.5f);
                }
                // Issue #832: Notify PropertySystem when a THUG takeover attacker is defeated
                if (propertySystem != null && targetNPC.getType() == ragamuffin.entity.NPCType.THUG) {
                    propertySystem.onThugsDefeated(
                            (int) targetNPC.getPosition().x,
                            (int) targetNPC.getPosition().z);
                }
            }
            // Issue #818: Notify disguise system of visible crime (punching NPCs)
            disguiseSystem.notifyCrime(player, npcManager.getNPCs());
            // Issue #826: Notify witness system of crime — transitions nearby NPCs to WITNESS state
            witnessSystem.registerCrime(player.getPosition().x, player.getPosition().y, player.getPosition().z,
                    "attacking someone", npcManager.getNPCs(), null);
            witnessSystem.notifyCrime(player.getPosition().x, player.getPosition().z);
            // Issue #824: Notify street economy system of visible crime — spikes SCARED need on nearby NPCs
            streetEconomySystem.onCrimeEvent(player.getPosition().x, player.getPosition().z,
                    8f, 0.3f, npcManager.getNPCs());
            // Issue #26: If a YOUTH_GANG member was punched, escalate territory to hostile
            if (targetNPC.getType() == ragamuffin.entity.NPCType.YOUTH_GANG) {
                gangTerritorySystem.onPlayerAttacksGang(tooltipSystem, npcManager, player, world);
                // Issue #450: gang aggro achievement
                achievementSystem.unlock(ragamuffin.ui.AchievementType.GANG_AGGRO);
            }
            // Issue #811: Fire faction Respect delta when hitting a faction NPC
            factionSystem.onPlayerHitFactionNpc(targetNPC, npcManager.getNPCs());
            return; // Don't punch blocks if we hit an NPC
        }

        // Raycast to find target block and target prop; hit whichever is closer.
        // Issue #752: Non-block 3D props were never checked here, so they could
        // not be targeted or destroyed.
        RaycastResult result = blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PUNCH_REACH);
        int targetPropIndex = findPropInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);

        // Determine which target is closer
        float blockDist = (result != null) ? result.getDistance() : Float.MAX_VALUE;
        float propDist  = Float.MAX_VALUE;
        if (targetPropIndex >= 0) {
            List<PropPosition> props = world.getPropPositions();
            if (targetPropIndex < props.size()) {
                PropPosition pp = props.get(targetPropIndex);
                propDist = rayAABBIntersect(tmpCameraPos, tmpDirection, pp.getAABB());
                if (propDist < 0f) propDist = Float.MAX_VALUE;
            }
        }

        boolean hitProp  = targetPropIndex >= 0 && propDist  <= blockDist;
        boolean hitBlock = result != null       && blockDist  <  propDist;

        if (hitProp) {
            // Punch connects — animate, drain energy
            firstPersonArm.punch();
            player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);

            // Punch the prop
            Material drop = propBreaker.punchProp(world, targetPropIndex);

            // Play punch sound on every hit
            soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PUNCH);

            // Issue #171: Emit combat-hit sparks at the prop centre
            if (targetPropIndex < world.getPropPositions().size()) {
                PropPosition pp = world.getPropPositions().get(targetPropIndex);
                particleSystem.emitCombatHit(pp.getWorldX(), pp.getWorldY() + pp.getType().getCollisionHeight() * 0.5f, pp.getWorldZ());
            }

            if (drop != null) {
                // Prop was broken — collect the material drop
                inventory.addItem(drop, 1);
                soundSystem.play(ragamuffin.audio.SoundEffect.INVENTORY_PICKUP);
                gameHUD.setBlockBreakProgress(0f);
            } else {
                // Prop partially damaged — show break progress
                float progress = propBreaker.getBreakProgress(world, targetPropIndex);
                gameHUD.setBlockBreakProgress(progress);
            }
        } else if (hitBlock) {
            // Punch connects — animate, drain energy
            firstPersonArm.punch();
            player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);

            int x = result.getBlockX();
            int y = result.getBlockY();
            int z = result.getBlockZ();
            BlockType blockType = result.getBlockType();

            // Check if it's a tree - trigger tooltip on first punch
            if (blockType == BlockType.TREE_TRUNK && blockBreaker.getHitCount(x, y, z) == 0) {
                tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
            }

            // Punch the block with tool if equipped
            boolean broken = blockBreaker.punchBlock(world, x, y, z, toolMaterial);

            // Play punch sound on every hit
            soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PUNCH);

            // Issue #171: Emit combat-hit sparks at the hit block face
            particleSystem.emitCombatHit(x + 0.5f, y + 0.5f, z + 0.5f);

            // Update HUD with break progress after the punch
            if (!broken) {
                float progress = blockBreaker.getBreakProgress(world, x, y, z, toolMaterial);
                gameHUD.setBlockBreakProgress(progress);
            } else {
                // Block was broken - reset progress
                gameHUD.setBlockBreakProgress(0f);
                // Play block break sound based on material
                soundSystem.playBlockBreak(blockType);
                // Issue #807: Block-break noise spike for NPC hearing detection
                noiseSystem.spikeBlockBreak();
            }

            // Consume tool durability if a tool was used
            if (toolMaterial != null) {
                Tool tool = inventory.getToolInSlot(selectedSlot);
                if (tool == null) {
                    // First use of this tool — create a Tool instance on the slot
                    tool = new Tool(toolMaterial, Tool.getMaxDurability(toolMaterial));
                    inventory.setToolInSlot(selectedSlot, tool);
                }
                boolean toolBroke = tool.use();
                if (toolBroke) {
                    // Tool is destroyed — remove from inventory slot
                    inventory.removeItem(toolMaterial, 1);
                    inventory.setToolInSlot(selectedSlot, null);
                    tooltipSystem.trigger(TooltipTrigger.TOOL_BROKEN);
                    // Issue #450: achievement for breaking tools
                    achievementSystem.increment(ragamuffin.ui.AchievementType.TOOL_BREAKER);
                }
            }

            if (broken) {
                // Issue #813: Deregister campfire when broken
                if (blockType == BlockType.CAMPFIRE) {
                    campfireSystem.removeCampfire(new Vector3(x, y, z));
                }

                // Issue #816: Exterior wall smash — BRICK, GLASS, or STONE belonging to a building
                if ((blockType == BlockType.BRICK || blockType == BlockType.GLASS || blockType == BlockType.STONE)
                        && world.getLandmarkAt(x, y, z) != null) {
                    neighbourhoodWatchSystem.onPlayerSmashedExteriorWall();
                }

                // Issue #816: Visible crime — breaking blocks in a public landmark area
                if (world.getLandmarkAt(x, y, z) != null) {
                    // Count nearby NPCs as witnesses
                    int witnessCount = 0;
                    for (ragamuffin.entity.NPC npc : npcManager.getNPCs()) {
                        if (npc.isAlive() && npc.getPosition().dst(player.getPosition()) < 20f) {
                            witnessCount++;
                        }
                    }
                    if (witnessCount >= 2) {
                        neighbourhoodWatchSystem.onVisibleCrime();
                    }
                }

                // Issue #659: Track block destruction in criminal record
                player.getCriminalRecord().record(ragamuffin.core.CriminalRecord.CrimeType.BLOCKS_DESTROYED);
                // Issue #818: Notify disguise system of visible crime (breaking blocks)
                disguiseSystem.notifyCrime(player, npcManager.getNPCs());
                // Issue #826: Notify witness system of crime — transitions nearby NPCs to WITNESS state
                witnessSystem.registerCrime(player.getPosition().x, player.getPosition().y, player.getPosition().z,
                        "breaking things", npcManager.getNPCs(), null);
                witnessSystem.notifyCrime(player.getPosition().x, player.getPosition().z);
                // Issue #824: Notify street economy system of visible crime — spikes SCARED need on nearby NPCs
                streetEconomySystem.onCrimeEvent(player.getPosition().x, player.getPosition().z,
                        8f, 0.3f, npcManager.getNPCs());

                // Issue #171: Emit block-break debris using the block's colour
                com.badlogic.gdx.graphics.Color blockColour = blockType.getColor();
                particleSystem.emitBlockBreak(x + 0.5f, y + 0.5f, z + 0.5f,
                    blockColour.r, blockColour.g, blockColour.b);

                // Block was broken - determine drop
                LandmarkType landmark = world.getLandmarkAt(x, y, z);
                Material drop = dropTable.getDrop(blockType, landmark);

                if (drop != null) {
                    inventory.addItem(drop, 1);
                    soundSystem.play(ragamuffin.audio.SoundEffect.INVENTORY_PICKUP);

                    // Trigger jeweller tooltip if applicable
                    if (drop == Material.DIAMOND && landmark == LandmarkType.JEWELLER) {
                        tooltipSystem.trigger(TooltipTrigger.JEWELLER_DIAMOND);
                    }

                    // Phase 11: Trigger Greggs tooltip if applicable
                    if ((drop == Material.SAUSAGE_ROLL || drop == Material.STEAK_BAKE) &&
                        landmark == LandmarkType.GREGGS) {
                        tooltipSystem.trigger(TooltipTrigger.FIRST_GREGGS);
                    }
                }

                // CRITIC 3: Greggs Raid mechanic — delegate to dedicated system
                if (landmark == LandmarkType.GREGGS) {
                    greggsRaidSystem.onGreggBlockBroken(tooltipSystem, npcManager, player, world);
                    // Issue #450: Greggs raid achievement — first break triggers it
                    achievementSystem.unlock(ragamuffin.ui.AchievementType.GREGGS_RAID);
                    // Issue #659: Record shop raid in criminal record
                    player.getCriminalRecord().record(ragamuffin.core.CriminalRecord.CrimeType.SHOPS_RAIDED);
                }

                // Issue #450: Block breaking achievements
                achievementSystem.unlock(ragamuffin.ui.AchievementType.FIRST_BLOCK);
                switch (blockType) {
                    case TREE_TRUNK:
                        achievementSystem.increment(ragamuffin.ui.AchievementType.LUMBERJACK);
                        break;
                    case BRICK:
                        achievementSystem.increment(ragamuffin.ui.AchievementType.BRICK_BY_BRICK);
                        break;
                    case GLASS:
                        achievementSystem.increment(ragamuffin.ui.AchievementType.GLAZIER);
                        break;
                    default:
                        break;
                }

                // Issue #130: Award reputation for landmark crimes proportional to severity.
                // Non-landmark block-breaking (trees, park grass) awards zero — Issue #46 preserved.
                if (landmark != null) {
                    switch (landmark) {
                        case GREGGS:
                            player.getStreetReputation().addPoints(1);
                            break;
                        case JEWELLER:
                            player.getStreetReputation().addPoints(3);
                            break;
                        case OFF_LICENCE:
                        case CHARITY_SHOP:
                        case BOOKIES:
                        case OFFICE_BUILDING:
                            player.getStreetReputation().addPoints(1);
                            break;
                        default:
                            break;
                    }
                }

                // Issue #811: Fire faction Respect delta when breaking a block in a faction building
                if (landmark != null) {
                    Faction brokenFaction = landmarkToFaction(landmark);
                    if (brokenFaction != null) {
                        factionSystem.onPlayerBreaksRivalBuilding(brokenFaction, npcManager.getNPCs());
                    }
                }

                // Leaf decay: remove floating LEAVES when a trunk is destroyed
                if (blockType == BlockType.TREE_TRUNK) {
                    decayFloatingLeaves(x, y, z);
                }

                // Issue #295: remove the companion door half when either door block is broken
                if (blockType == BlockType.DOOR_LOWER) {
                    world.setBlock(x, y + 1, z, BlockType.AIR);
                    blockBreaker.clearHits(x, y + 1, z);
                    rebuildChunkAt(x, y + 1, z);
                } else if (blockType == BlockType.DOOR_UPPER) {
                    world.setBlock(x, y - 1, z, BlockType.AIR);
                    blockBreaker.clearHits(x, y - 1, z);
                    rebuildChunkAt(x, y - 1, z);
                }

                // Only rebuild the affected chunk (and neighbours if on a boundary)
                rebuildChunkAt(x, y, z);
            }
        } else {
            // Not looking at any block or prop - reset progress indicator
            gameHUD.setBlockBreakProgress(0f);
        }
    }

    /**
     * Remove floating LEAVES blocks that are no longer connected to any TREE_TRUNK.
     * Called after a TREE_TRUNK block is broken. Checks all LEAVES within radius 4
     * using Manhattan distance and removes unsupported ones.
     */
    private void decayFloatingLeaves(int brokenX, int brokenY, int brokenZ) {
        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                    int lx = brokenX + dx;
                    int ly = brokenY + dy;
                    int lz = brokenZ + dz;
                    if (world.getBlock(lx, ly, lz) != BlockType.LEAVES) continue;
                    if (!hasNearbyTrunk(lx, ly, lz, radius)) {
                        world.setBlock(lx, ly, lz, BlockType.AIR);
                        blockBreaker.clearHits(lx, ly, lz);
                        rebuildChunkAt(lx, ly, lz);
                        Material drop = dropTable.getDrop(BlockType.LEAVES, null);
                        if (drop != null) {
                            inventory.addItem(drop, 1);
                            soundSystem.play(ragamuffin.audio.SoundEffect.INVENTORY_PICKUP);
                        }
                    }
                }
            }
        }
    }

    private boolean hasNearbyTrunk(int lx, int ly, int lz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                    if (world.getBlock(lx + dx, ly + dy, lz + dz) == BlockType.TREE_TRUNK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void handlePlace() {
        // Get selected material from hotbar
        int selectedSlot = hotbarUI.getSelectedSlot();
        Material material = inventory.getItemInSlot(selectedSlot);

        if (material == null) {
            return;
        }

        // Phase 11: Check if material is food - eat instead of placing
        if (interactionSystem.isFood(material)) {
            boolean consumed = interactionSystem.consumeFood(material, player, inventory);
            if (consumed) {
                if (material == ragamuffin.building.Material.CRISPS) {
                    soundSystem.play(ragamuffin.audio.SoundEffect.MUNCH);
                } else {
                    soundSystem.play(ragamuffin.audio.SoundEffect.ITEM_EAT);
                }
                // Show any feedback message from the consumable
                String consumeMsg = interactionSystem.getLastConsumeMessage();
                if (consumeMsg != null) {
                    tooltipSystem.showMessage(consumeMsg, 3.0f);
                }
                return;
            }
        }

        // Issue #807: Flask of Tea restores warmth when used
        if (material == ragamuffin.building.Material.FLASK_OF_TEA) {
            boolean drank = warmthSystem.drinkFlaskOfTea(player, inventory);
            if (drank) {
                soundSystem.play(ragamuffin.audio.SoundEffect.ITEM_EAT);
                tooltipSystem.showMessage("Lovely cuppa. You feel warmer.", 3.0f);
            } else {
                tooltipSystem.showMessage("No flask of tea left.", 2.0f);
            }
            return;
        }

        // Issue #848: FLYER use — start a rave at the player's squat
        if (material == ragamuffin.building.Material.FLYER && raveSystem != null && squatSystem != null) {
            // Gather NPCs near the player to seed the rave rumour
            java.util.List<ragamuffin.entity.NPC> nearbyNpcs = new java.util.ArrayList<>();
            for (ragamuffin.entity.NPC npc : npcManager.getNPCs()) {
                if (npc.getPosition().dst(player.getPosition()) <= 30f) {
                    nearbyNpcs.add(npc);
                }
            }
            String raveMsg = raveSystem.startRave(inventory, squatSystem.getVibe(),
                    mcBattleSystem != null ? mcBattleSystem.getMcRank() : 0, nearbyNpcs);
            tooltipSystem.showMessage(raveMsg, 3.0f);
            return;
        }

        // Fix #257: Check if material has a non-food, non-placeable use action
        if (interactionSystem.canUseItem(material)) {
            String message = interactionSystem.useItem(material, player, inventory);
            soundSystem.play(ragamuffin.audio.SoundEffect.ITEM_USE);
            if (message != null) {
                tooltipSystem.showMessage(message, 3.0f);
            }
            return;
        }

        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        // Issue #645: Small items are placed directly on the block surface without grid snapping
        if (material.isSmallItem()) {
            boolean placed = blockPlacer.placeSmallItem(world, inventory, material, tmpCameraPos, tmpDirection, PLACE_REACH, player.getAABB());
            if (!placed) {
                tooltipSystem.showMessage("Can't place item here.", 2.0f);
                soundSystem.play(ragamuffin.audio.SoundEffect.UI_CLOSE);
            } else {
                soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PLACE);
                // Issue #675: Refresh the small item renderer so the newly placed item is visible
                if (smallItemRenderer != null) {
                    smallItemRenderer.setItems(world.getSmallItems());
                }
            }
            return;
        }

        // Fix #887: PROP_* items place as 3D props in the world
        ragamuffin.world.PropType propTypeForMaterial = blockPlacer.materialToPropType(material);
        if (propTypeForMaterial != null) {
            ragamuffin.world.PropType placedPropType = blockPlacer.placeProp(
                    world, inventory, material, tmpCameraPos, tmpDirection, PLACE_REACH,
                    player.getAABB(), cameraYaw);
            if (placedPropType == null) {
                tooltipSystem.showMessage("Can't place here.", 2.0f);
                soundSystem.play(ragamuffin.audio.SoundEffect.UI_CLOSE);
            } else {
                soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PLACE);
                // Refresh prop renderer so the newly placed prop is visible
                if (propRenderer != null) {
                    propRenderer.setProps(world.getPropPositions());
                }
                // Notify squat system if player has a squat (furnishing Vibe bonus)
                if (squatSystem != null && squatSystem.hasSquat()) {
                    java.util.List<ragamuffin.entity.NPC> allNpcs = npcManager.getNPCs();
                    int vibeGain = squatSystem.furnish(placedPropType, allNpcs);
                    if (vibeGain > 0) {
                        tooltipSystem.showMessage("Squat Vibe +" + vibeGain + "!", 2.5f);
                    }
                }
            }
            return;
        }

        // Get placement position before placing so we know which chunk to rebuild
        Vector3 placementPos = blockPlacer.getPlacementPosition(world, tmpCameraPos, tmpDirection, PLACE_REACH);

        boolean placed = blockPlacer.placeBlock(world, inventory, material, tmpCameraPos, tmpDirection, PLACE_REACH, player.getAABB());

        if (!placed) {
            // Fix #297: Give feedback when placement fails for a valid placeable material
            boolean isPlaceable = material == Material.DOOR
                    || material == Material.CARDBOARD_BOX
                    || blockPlacer.materialToBlockType(material) != null;
            if (isPlaceable) {
                tooltipSystem.showMessage("Can't place block here.", 2.0f);
                soundSystem.play(ragamuffin.audio.SoundEffect.UI_CLOSE);
            }
            return;
        }

        // Play block place sound
        soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PLACE);

        // Issue #813: Register campfire when a CAMPFIRE block is placed
        if (placementPos != null) {
            int bx = (int) placementPos.x;
            int by = (int) placementPos.y;
            int bz = (int) placementPos.z;
            if (world.getBlock(bx, by, bz) == BlockType.CAMPFIRE) {
                campfireSystem.addCampfire(placementPos);
            }
        }

        // Issue #807: Block-place noise spike for NPC hearing detection
        noiseSystem.spikeBlockPlace();

        // Phase 11: Trigger first block place tooltip
        if (!tooltipSystem.hasShown(TooltipTrigger.FIRST_BLOCK_PLACE)) {
            tooltipSystem.trigger(TooltipTrigger.FIRST_BLOCK_PLACE);
        }

        // Critic 4: Trigger cardboard box shelter tooltip
        if (material == Material.CARDBOARD_BOX && !tooltipSystem.hasShown(TooltipTrigger.CARDBOARD_BOX_SHELTER)) {
            tooltipSystem.trigger(TooltipTrigger.CARDBOARD_BOX_SHELTER);
        }

        // Issue #450: Block building achievements
        achievementSystem.increment(ragamuffin.ui.AchievementType.BUILDER);
        if (material == Material.CARDBOARD_BOX) {
            achievementSystem.unlock(ragamuffin.ui.AchievementType.CARDBOARD_CASTLE);
        }

        // Only rebuild the affected chunk
        if (placementPos != null) {
            rebuildChunkAt((int) placementPos.x, (int) placementPos.y, (int) placementPos.z);
            // Issue #295: door placement sets DOOR_LOWER + DOOR_UPPER — rebuild chunk for y+1 too
            if (material == Material.DOOR) {
                rebuildChunkAt((int) placementPos.x, (int) placementPos.y + 1, (int) placementPos.z);
            }
            // Cardboard box builds a 2x2x2 structure — rebuild adjacent chunks too
            if (material == Material.CARDBOARD_BOX) {
                rebuildChunkAt((int) placementPos.x + 2, (int) placementPos.y, (int) placementPos.z);
                rebuildChunkAt((int) placementPos.x, (int) placementPos.y, (int) placementPos.z + 2);
                rebuildChunkAt((int) placementPos.x, (int) placementPos.y + 3, (int) placementPos.z);
            }
        }
    }

    /**
     * Phase 11: Handle E key interaction with NPCs and doors.
     */
    private void handleInteraction() {
        // Issue #773: Car enter/exit takes priority
        if (carDrivingSystem.isInCar()) {
            carDrivingSystem.exitCar(player);
            String msg = carDrivingSystem.pollLastMessage();
            if (msg != null) tooltipSystem.showMessage(msg, 3.0f);
            return;
        }
        if (carDrivingSystem.tryEnterCar(player)) {
            // Initialize chase camera angle to car heading so camera starts behind the car
            if (carDrivingSystem.getCurrentCar() != null) {
                chaseCameraAngle = carDrivingSystem.getCurrentCar().getHeading();
            }
            String msg = carDrivingSystem.pollLastMessage();
            if (msg != null) tooltipSystem.showMessage(msg, 3.0f);
            return;
        }

        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        // Find NPC in interaction range
        NPC targetNPC = interactionSystem.findNPCInRange(player.getPosition(), tmpDirection, npcManager.getNPCs());

        if (targetNPC != null) {
            // Issue #848: MC Battle — if player holds a MICROPHONE and target is an MC_CHAMPION
            if (mcBattleSystem != null && mcBattleSystem.canChallenge(targetNPC, inventory)) {
                MCBattleSystem.Champion champion = mcBattleSystem.championForNpc(targetNPC);
                if (champion != null) {
                    String battleMsg = mcBattleSystem.startBattle(champion, inventory);
                    tooltipSystem.showMessage(battleMsg, 3.0f);
                    return;
                }
            }

            // Issue #824: Street deal — if the player is holding an item that satisfies the NPC's need,
            // attempt a street deal instead of the normal dialogue interaction.
            {
                int selectedSlot = hotbarUI.getSelectedSlot();
                ragamuffin.building.Material selectedMaterial = inventory.getItemInSlot(selectedSlot);
                if (selectedMaterial != null && streetEconomySystem.hasRelevantNeed(targetNPC, selectedMaterial)) {
                    int price = streetEconomySystem.getEffectivePrice(selectedMaterial,
                            -1, disguiseSystem.isDisguised(),
                            streetEconomySystem.getActiveEvent(),
                            notorietySystem.getTier());
                    StreetEconomySystem.DealResult dealResult = streetEconomySystem.attemptDeal(
                            targetNPC, selectedMaterial, price, inventory, player,
                            npcManager.getNPCs(), -1, disguiseSystem.isDisguised(),
                            notorietySystem.getTier(),
                            type -> achievementSystem.unlock(type));
                    String dealMsg;
                    switch (dealResult) {
                        case SUCCESS: dealMsg = "Deal done."; break;
                        case NO_NEED: dealMsg = "They don't need that right now."; break;
                        case MISSING_ITEM: dealMsg = "You don't have that."; break;
                        case HAGGLE_REJECTED: dealMsg = "They're not desperate enough for that price."; break;
                        case OUT_OF_RANGE: dealMsg = "Too far away."; break;
                        case POLICE_NEARBY_DODGY: dealMsg = "Not with the police watching."; break;
                        default: dealMsg = "Deal failed."; break;
                    }
                    tooltipSystem.showMessage(dealMsg, 2.5f);
                    return;
                }
            }
            // Issue #832: Estate agent interaction — buy a building when talking to ESTATE_AGENT NPC
            if (propertySystem != null && targetNPC.getType() == ragamuffin.entity.NPCType.ESTATE_AGENT) {
                if (!propertySystem.isEstateAgentOpen(timeSystem)) {
                    tooltipSystem.showMessage("The estate agent is closed. Come back on a weekday between 9am and 5pm.", 3.0f);
                    return;
                }
                // Find the nearest landmark to purchase
                ragamuffin.world.Landmark estateAgentLandmark = world.getLandmark(ragamuffin.world.LandmarkType.ESTATE_AGENT);
                ragamuffin.world.LandmarkType targetLandmarkType = ragamuffin.world.LandmarkType.TERRACED_HOUSE;
                int buildingX = (int) player.getPosition().x;
                int buildingZ = (int) player.getPosition().z;
                if (estateAgentLandmark != null) {
                    buildingX = (int) estateAgentLandmark.getPosition().x;
                    buildingZ = (int) estateAgentLandmark.getPosition().z;
                    targetLandmarkType = ragamuffin.world.LandmarkType.ESTATE_AGENT;
                }
                ragamuffin.core.Faction owningFaction = landmarkToFaction(targetLandmarkType);
                String purchaseMsg = propertySystem.purchaseProperty(
                        targetLandmarkType, buildingX, buildingZ,
                        inventory, npcManager.getNPCs(), owningFaction);
                tooltipSystem.showMessage(purchaseMsg, 3.0f);
                return;
            }

            // Interact with the NPC — pass inventory, player and all NPCs so Fence
            // interactions work correctly (Issue #733).
            String dialogue = interactionSystem.interactWithNPC(targetNPC, inventory, player,
                    npcManager.getNPCs());
            // Issue #733: if the fence trade UI was opened, update fenceTradeUI visibility
            if (fenceSystem.isTradeUIOpen()) {
                fenceTradeUI.show();
            }
            // Issue #541: track the shopkeeper whose menu is open so 1/2/3 can select items
            if (targetNPC.isShopMenuOpen()) {
                activeShopkeeperNPC = targetNPC;
            } else {
                // Menu was closed (second E-press confirms purchase or cancels)
                activeShopkeeperNPC = null;
            }
            // Show quest completion tooltip if a quest was just completed
            Quest completed = interactionSystem.pollLastQuestCompleted();
            if (completed != null) {
                tooltipSystem.showMessage("Quest complete! " + completed.getGiver() + " is pleased.", 4.0f);
                achievementSystem.unlock(AchievementType.FIRST_QUEST);
                achievementSystem.increment(AchievementType.QUEST_MASTER);
            }
            // The dialogue is set on the NPC, which will be rendered as a speech bubble
            return;
        }

        // Issue #818: Wire equip-disguise on E-key — if the player is holding a clothing item, equip it
        {
            int selectedSlot = hotbarUI.getSelectedSlot();
            ragamuffin.building.Material selectedMaterial = inventory.getItemInSlot(selectedSlot);
            if (selectedMaterial != null && DisguiseSystem.isDisguiseMaterial(selectedMaterial)) {
                boolean equipped = disguiseSystem.equipDisguise(selectedMaterial, inventory);
                if (equipped) {
                    tooltipSystem.showMessage("Disguise equipped: " + selectedMaterial.getDisplayName(), 2.5f);
                    // Issue #818: Wire disguise-change escape into WantedSystem (pass real disguiseSystem, not null)
                    wantedSystem.attemptDisguiseEscape(disguiseSystem, player, npcManager.getNPCs(),
                            type -> achievementSystem.unlock(type));
                }
                return;
            }
        }

        // Issue #799: Claim shop via SHOP_KEY in hotbar
        if (cornerShopSystem != null) {
            int selectedSlot = hotbarUI.getSelectedSlot();
            ragamuffin.building.Material selectedMaterial = inventory.getItemInSlot(selectedSlot);
            if (selectedMaterial == ragamuffin.building.Material.SHOP_KEY) {
                if (cornerShopSystem.tryClaimShop(inventory)) {
                    tooltipSystem.showMessage("Shop claimed. Time to hustle.", 3.0f);
                }
                return;
            }
        }

        // Issue #832: Repair an owned building — press E while holding BRICK, WOOD, or PAINT_TIN near a property
        if (propertySystem != null) {
            int playerX = (int) player.getPosition().x;
            int playerZ = (int) player.getPosition().z;
            if (propertySystem.ownsAt(playerX, playerZ)) {
                int selectedSlot = hotbarUI.getSelectedSlot();
                ragamuffin.building.Material selectedMaterial = inventory.getItemInSlot(selectedSlot);
                boolean hasRepairMaterial = selectedMaterial == ragamuffin.building.Material.BRICK
                        || selectedMaterial == ragamuffin.building.Material.WOOD
                        || selectedMaterial == ragamuffin.building.Material.PAINT_TIN;
                if (hasRepairMaterial) {
                    String repairMsg = propertySystem.repairProperty(playerX, playerZ, inventory);
                    if (repairMsg != null) {
                        tooltipSystem.showMessage(repairMsg, 3.0f);
                        return;
                    }
                }
            }
        }

        // Issue #881: Do not let the squat claim intercept E key when the player is
        // targeting a shop door — check for a door in the short raycast first.
        ragamuffin.world.RaycastResult preDoorResult =
                blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, 3.0f);
        boolean targetingDoor = preDoorResult != null
                && (world.getBlock(preDoorResult.getBlockX(), preDoorResult.getBlockY(), preDoorResult.getBlockZ())
                        == ragamuffin.world.BlockType.DOOR_LOWER
                    || world.getBlock(preDoorResult.getBlockX(), preDoorResult.getBlockY(), preDoorResult.getBlockZ())
                        == ragamuffin.world.BlockType.DOOR_UPPER);

        // Issue #848: Squat claim — E key inside a derelict building with ≥5 WOOD
        if (!targetingDoor && squatSystem != null && !squatSystem.hasSquat()) {
            // Find the nearest landmark to the player — check if it's a derelict building
            ragamuffin.world.Landmark nearestDerelict = null;
            float nearestDist = 6f; // must be within 6 blocks
            for (ragamuffin.world.Landmark lm : world.getAllLandmarks()) {
                float dist = player.getPosition().dst(
                        lm.getPosition().x + lm.getWidth() / 2f,
                        player.getPosition().y,
                        lm.getPosition().z + lm.getDepth() / 2f);
                if (dist < nearestDist) {
                    int condition = PropertySystem.INITIAL_CONDITION;
                    if (neighbourhoodSystem != null) {
                        ragamuffin.core.NeighbourhoodSystem.BuildingRecord rec =
                                neighbourhoodSystem.getBuilding(
                                        (int) lm.getPosition().x,
                                        (int) lm.getPosition().z);
                        if (rec != null) condition = rec.getCondition();
                    }
                    if (condition <= SquatSystem.MAX_CLAIMABLE_CONDITION) {
                        nearestDist = dist;
                        nearestDerelict = lm;
                    }
                }
            }
            if (nearestDerelict != null) {
                String claimMsg = squatSystem.claimSquat(
                        nearestDerelict.getType(),
                        (int) (nearestDerelict.getPosition().x + nearestDerelict.getWidth() / 2f),
                        (int) (nearestDerelict.getPosition().z + nearestDerelict.getDepth() / 2f),
                        PropertySystem.INITIAL_CONDITION, inventory, npcManager.getNPCs());
                tooltipSystem.showMessage(claimMsg, 3.0f);
                return;
            }
        }

        // Issue #848: Rave disperse — E key at squat entrance while rave is active
        if (raveSystem != null && raveSystem.isRaveActive() && squatSystem != null && squatSystem.hasSquat()) {
            float distToSquat = player.getPosition().dst(
                    squatSystem.getSquatWorldX(), player.getPosition().y, squatSystem.getSquatWorldZ());
            if (distToSquat <= 6f) {
                String disperseMsg = raveSystem.disperseRave(inventory);
                tooltipSystem.showMessage(disperseMsg, 3.0f);
                ravePoliceSpawned = false;
                return;
            }
        }

        // Issue #852: E key — play fruit machine if targeting FRUIT_MACHINE prop
        if (fruitMachine != null) {
            int fruitMachinePropIndex = findPropInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);
            if (fruitMachinePropIndex >= 0) {
                java.util.List<ragamuffin.world.PropPosition> fmProps = world.getPropPositions();
                if (fruitMachinePropIndex < fmProps.size()
                        && fmProps.get(fruitMachinePropIndex).getType() == ragamuffin.world.PropType.FRUIT_MACHINE) {
                    if (inventory.getItemCount(ragamuffin.building.Material.COIN) >= 1) {
                        inventory.removeItem(ragamuffin.building.Material.COIN, 1);
                        FruitMachine.SpinResult spinResult = fruitMachine.spin();
                        if (spinResult.payout > 0) {
                            inventory.addItem(ragamuffin.building.Material.COIN, spinResult.payout);
                        }
                        tooltipSystem.showMessage(spinResult.displayText, 3.0f);
                    } else {
                        tooltipSystem.showMessage("Need 1 coin to play the fruit machine.", 2.5f);
                    }
                    return;
                }
            }
        }

        // Issue #856: E key — view top-3 championship ladder standings at BOOKIE_BOARD prop
        if (championshipLadder != null) {
            int bookiePropIndex = findPropInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);
            if (bookiePropIndex >= 0) {
                java.util.List<ragamuffin.world.PropPosition> bkProps = world.getPropPositions();
                if (bookiePropIndex < bkProps.size()
                        && bkProps.get(bookiePropIndex).getType() == ragamuffin.world.PropType.BOOKIE_BOARD) {
                    java.util.List<ChampionshipLadder.Entry> top = championshipLadder.getEntries();
                    StringBuilder sb = new StringBuilder("Championship: ");
                    int shown = Math.min(3, top.size());
                    for (int i = 0; i < shown; i++) {
                        if (i > 0) sb.append(" | ");
                        sb.append(top.get(i).getRank()).append(". ").append(top.get(i).getFighterName());
                    }
                    tooltipSystem.showMessage(sb.toString(), 4.0f);
                    return;
                }
            }
        }

        // Check for door interaction via short raycast (≤3 blocks) — reuse preDoorResult
        ragamuffin.world.RaycastResult doorResult = preDoorResult;
        if (doorResult != null) {
            int x = doorResult.getBlockX();
            int y = doorResult.getBlockY();
            int z = doorResult.getBlockZ();
            ragamuffin.world.BlockType hitBlock = world.getBlock(x, y, z);
            if (hitBlock == ragamuffin.world.BlockType.DOOR_LOWER || hitBlock == ragamuffin.world.BlockType.DOOR_UPPER) {
                // Normalise to DOOR_LOWER position
                int lowerY = (hitBlock == ragamuffin.world.BlockType.DOOR_UPPER) ? y - 1 : y;
                // Issue #799: Try to claim a derelict shop via door interaction (buildingCondition <= 49)
                if (cornerShopSystem != null && !cornerShopSystem.hasShop()) {
                    // Use DERELICT_CONDITION_THRESHOLD — derelict buildings have condition <= 49
                    int buildingCondition = PropertySystem.INITIAL_CONDITION; // 30 — derelict
                    String claimMsg = cornerShopSystem.claimShopByInteraction(buildingCondition) ?
                            "Shop's yours. Don't let the council find out." :
                            "Can't claim this one.";
                    tooltipSystem.showMessage(claimMsg, 2.5f);
                    if (cornerShopSystem.hasShop()) return;
                }
                world.toggleDoor(x, lowerY, z);
                rebuildChunkAt(x, lowerY, z);
                rebuildChunkAt(x, lowerY + 1, z);
                soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PLACE);
                return;
            }
        }

        // Issue #828: E key near JobCentre — attempt sign-on
        {
            ragamuffin.world.Landmark jobCentreLandmark = world.getLandmark(ragamuffin.world.LandmarkType.JOB_CENTRE);
            if (jobCentreLandmark != null) {
                float distToJobCentre = player.getPosition().dst(
                        jobCentreLandmark.getPosition().x + jobCentreLandmark.getWidth() / 2f,
                        player.getPosition().y,
                        jobCentreLandmark.getPosition().z + jobCentreLandmark.getDepth() / 2f);
                if (distToJobCentre <= 4f) {
                    JobCentreSystem.SignOnResult result = jobCentreSystem.trySignOn(player, inventory);
                    if (result != null) {
                        jobCentreUI.setLastSignOnResult(result);
                        jobCentreUI.show();
                        String signOnMsg;
                        switch (result) {
                            case SUCCESS: signOnMsg = "Sign-on successful. UC payment received."; break;
                            case SUSPICIOUS: signOnMsg = "The case worker eyes you suspiciously..."; break;
                            case POLICE_ESCORT: signOnMsg = "Police are waiting for you at the entrance."; break;
                            case NOTORIETY_SCARED: signOnMsg = "The case worker is terrified but pays up."; break;
                            case NOTORIETY_FLEE: signOnMsg = "The case worker runs away. Claim closed."; break;
                            case MARCHETTI_CONFISCATION: signOnMsg = "Contraband confiscated at sign-on."; break;
                            case WINDOW_CLOSED: signOnMsg = "The sign-on window is not open right now."; break;
                            case CLAIM_CLOSED: signOnMsg = "Your UC claim has been permanently closed."; break;
                            default: signOnMsg = "JobCentre interaction."; break;
                        }
                        tooltipSystem.showMessage(signOnMsg, 3.0f);
                    }
                    return;
                }
            }
        }

        // Issue #830: E key near Boot Sale — open the auction UI
        if (bootSaleSystem != null) {
            ragamuffin.world.Landmark bootSaleLandmarkE = world.getLandmark(ragamuffin.world.LandmarkType.BOOT_SALE);
            if (bootSaleLandmarkE != null) {
                float distToBootSale = player.getPosition().dst(
                        bootSaleLandmarkE.getPosition().x + bootSaleLandmarkE.getWidth() / 2f,
                        player.getPosition().y,
                        bootSaleLandmarkE.getPosition().z + bootSaleLandmarkE.getDepth() / 2f);
                if (distToBootSale <= 5f) {
                    if (!bootSaleSystem.isVenueOpen()) {
                        tooltipSystem.showMessage("Can't go in — you're too hot right now.", 2.5f);
                    } else {
                        bootSaleSystem.setPlayerInventory(inventory);
                        bootSaleUI.show();
                        state = GameState.BOOT_SALE_OPEN;
                    }
                    return;
                }
            }
        }

        // Issue #826: E key — steal a hot CCTV tape if the player is close enough to one
        {
            boolean tapeStolen = witnessSystem.stealCctvTape(
                    player.getPosition().x, player.getPosition().z);
            if (tapeStolen) {
                inventory.addItem(ragamuffin.building.Material.CCTV_TAPE, 1);
                tooltipSystem.showMessage("Evidence sorted. You're getting good at this.", 3.0f);
                return;
            }
        }

        // Issue #781: E key — scrub a rival faction's graffiti mark on the targeted block face
        {
            ragamuffin.world.RaycastResult scrubResult =
                    blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, GraffitiSystem.MAX_TAG_DISTANCE);
            if (scrubResult != null) {
                Vector3 targetBlockPos = new Vector3(scrubResult.getBlockX(), scrubResult.getBlockY(), scrubResult.getBlockZ());
                GraffitiSystem.GraffitiMark rivalMark = findRivalGraffitiMark(targetBlockPos);
                if (rivalMark != null) {
                    graffitiSystem.scrubTag(rivalMark, factionSystem.getTurfMap(),
                            type -> achievementSystem.unlock(type));
                    tooltipSystem.showMessage("Scrubbed rival tag", 1.5f);
                }
            }
        }

        // Issue #799: E key — open/close shop when inside the claimed shop
        if (cornerShopSystem != null && cornerShopSystem.hasShop()) {
            if (!cornerShopSystem.isShopOpen()) {
                cornerShopSystem.openShop(achievementSystem::unlock);
                tooltipSystem.showMessage("Shop open for business.", 2.0f);
            } else {
                cornerShopSystem.closeShop();
                tooltipSystem.showMessage("Shop closed.", 2.0f);
            }
        }

        // Issue #872: E key — pick up a nearby small 3D object into inventory
        {
            int smallItemIndex = findSmallItemInReach(player.getPosition(), PUNCH_REACH);
            if (smallItemIndex >= 0) {
                ragamuffin.building.SmallItem nearbyItem = world.getSmallItems().get(smallItemIndex);
                ragamuffin.building.Material mat = nearbyItem.getMaterial();
                world.removeSmallItem(smallItemIndex);
                inventory.addItem(mat, 1);
                if (smallItemRenderer != null) {
                    smallItemRenderer.setItems(world.getSmallItems());
                }
                soundSystem.play(ragamuffin.audio.SoundEffect.INVENTORY_PICKUP);
                tooltipSystem.showMessage("Picked up " + mat.getDisplayName() + ".", 2.0f);
                return;
            }
        }

        // Issue #837: E key — place stall from hotbar, or open/close an already-placed stall
        if (stallSystem != null) {
            ragamuffin.world.RaycastResult stallTarget =
                    blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PLACE_REACH);
            if (!stallSystem.isStallPlaced()) {
                // Try to place stall on targeted block surface
                if (stallTarget != null) {
                    int tx = stallTarget.getBlockX();
                    int ty = stallTarget.getBlockY();
                    int tz = stallTarget.getBlockZ();
                    ragamuffin.world.BlockType ground = world.getBlock(tx, ty - 1, tz);
                    String groundName = (ground != null) ? ground.name() : "";
                    Faction territory = (factionSystem != null)
                            ? factionSystem.getTurfMap().getOwner(tx, tz) : null;
                    boolean placed = stallSystem.placeStall(tx, ty, tz, groundName, inventory, territory);
                    if (placed) {
                        tooltipSystem.showMessage("Stall placed. Press E to open.", 2.5f);
                    }
                }
            } else if (!stallSystem.isStallOpen()) {
                stallSystem.openStallWithAchievement(achievementSystem::unlock);
                tooltipSystem.showMessage("Stall open. Time to shift some gear.", 2.0f);
            } else {
                stallSystem.closeStall();
                tooltipSystem.showMessage("Stall closed.", 1.5f);
            }
        }
    }

    /**
     * Issue #781: Find a living rival (non-player) graffiti mark on the given block position.
     */
    private GraffitiSystem.GraffitiMark findRivalGraffitiMark(Vector3 blockPos) {
        for (GraffitiSystem.GraffitiMark mark : graffitiSystem.getAllMarks()) {
            if (!mark.isAlive()) continue;
            if (mark.getStyle() == GraffitiSystem.TagStyle.PLAYER_TAG) continue;
            Vector3 mp = mark.getBlockPos();
            if ((int) mp.x == (int) blockPos.x
                    && (int) mp.y == (int) blockPos.y
                    && (int) mp.z == (int) blockPos.z) {
                return mark;
            }
        }
        return null;
    }

    /**
     * Issue #781: Handle T key — spray a PLAYER_TAG graffiti mark on the targeted block face.
     * Shows a tooltip if no spray can is held.
     */
    private void handleGraffitiSpray() {
        int selectedSlot = hotbarUI.getSelectedSlot();
        Material heldMaterial = inventory.getItemInSlot(selectedSlot);

        if (heldMaterial != Material.SPRAY_CAN) {
            tooltipSystem.showMessage("Equip a spray can to tag walls (T)", 2.0f);
            return;
        }

        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        ragamuffin.world.RaycastResult result =
                blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, GraffitiSystem.MAX_TAG_DISTANCE);
        if (result == null) return;

        Vector3 blockPos = new Vector3(result.getBlockX(), result.getBlockY(), result.getBlockZ());

        // Determine which face was hit from the hit point relative to the block centre
        GraffitiSystem.BlockFace face = computeHitFace(result);

        // Determine indoor/outdoor (below block height 1 = indoor heuristic; simplified)
        boolean isOutdoor = result.getBlockY() > 0;
        boolean isIndoor  = !isOutdoor;

        GraffitiSystem.GraffitiMark placed = graffitiSystem.placeTag(
                blockPos,
                face,
                GraffitiSystem.TagStyle.PLAYER_TAG,
                player.getPosition(),
                isIndoor,
                isOutdoor,
                inventory,
                factionSystem.getTurfMap(),
                noiseSystem,
                type -> achievementSystem.unlock(type));

        if (placed != null) {
            soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PLACE);
            tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH); // reuse closest available trigger; graffiti has its own in GraffitiSystem
        }
    }

    /**
     * Derive which block face was hit from the fractional hit position.
     */
    private GraffitiSystem.BlockFace computeHitFace(ragamuffin.world.RaycastResult result) {
        Vector3 hit = result.getHitPosition();
        if (hit == null) return GraffitiSystem.BlockFace.NORTH;
        float bx = result.getBlockX();
        float by = result.getBlockY();
        float bz = result.getBlockZ();
        float dx = hit.x - bx - 0.5f;
        float dy = hit.y - by - 0.5f;
        float dz = hit.z - bz - 0.5f;
        float adx = Math.abs(dx);
        float ady = Math.abs(dy);
        float adz = Math.abs(dz);
        if (ady >= adx && ady >= adz) {
            return dy > 0 ? GraffitiSystem.BlockFace.TOP : GraffitiSystem.BlockFace.BOTTOM;
        } else if (adx >= adz) {
            return dx > 0 ? GraffitiSystem.BlockFace.EAST : GraffitiSystem.BlockFace.WEST;
        } else {
            return dz > 0 ? GraffitiSystem.BlockFace.SOUTH : GraffitiSystem.BlockFace.NORTH;
        }
    }

    /**
     * Resolve player-NPC collisions by pushing the player out.
     */
    private void resolveNPCCollisions() {
        for (NPC npc : npcManager.getNPCs()) {
            // Knocked-out NPCs are lying on the ground; player can walk over them freely
            if (npc.getState() == NPCState.KNOCKED_OUT) continue;
            if (player.getAABB().intersects(npc.getAABB())) {
                // Push player away from NPC along XZ plane
                float dx = player.getPosition().x - npc.getPosition().x;
                float dz = player.getPosition().z - npc.getPosition().z;
                float len = (float) Math.sqrt(dx * dx + dz * dz);
                if (len < 0.01f) {
                    dx = 1f; dz = 0f; len = 1f;
                }
                float pushDist = 0.1f;
                player.getPosition().x += (dx / len) * pushDist;
                player.getPosition().z += (dz / len) * pushDist;
                player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
            }
        }
    }

    /**
     * Format an NPCType for display in the targeting reticule.
     * e.g. YOUTH_GANG -> "Youth Gang"
     */
    private String formatNPCName(ragamuffin.entity.NPCType type) {
        String raw = type.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (sb.length() > 0) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Format a BlockType for display in the targeting reticule.
     * e.g. TREE_TRUNK -> "Tree Trunk"
     */
    private String formatBlockName(ragamuffin.world.BlockType blockType) {
        String raw = blockType.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (sb.length() > 0) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Format a PropType for display in the targeting reticule.
     * e.g. PHONE_BOX -> "Phone Box"
     *
     * Issue #752: Non-block 3D objects can now be targeted; display their name.
     */
    private String formatPropName(ragamuffin.world.PropType propType) {
        String raw = propType.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (sb.length() > 0) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Issue #811: Map a landmark type to the faction that owns it, or null if neutral.
     * Marchetti Crew: industrial / off-licence.
     * Street Lads: park / council flats / skate park.
     * The Council: office building / job centre / police station / council areas.
     */
    private Faction landmarkToFaction(ragamuffin.world.LandmarkType landmark) {
        if (landmark == null) return null;
        switch (landmark) {
            case INDUSTRIAL_ESTATE:
            case OFF_LICENCE:
            case WAREHOUSE:
            case PAWN_SHOP:
                return Faction.MARCHETTI_CREW;
            case COUNCIL_FLATS:
            case SKATE_PARK:
            case ESTATE_AGENT:
                return Faction.STREET_LADS;
            case OFFICE_BUILDING:
            case JOB_CENTRE:
            case POLICE_STATION:
            case COMMUNITY_CENTRE:
            case LIBRARY:
                return Faction.THE_COUNCIL;
            default:
                return null;
        }
    }

    /**
     * Find an NPC within punch reach.
     *
     * <p>Fix #242: Delegates to {@link NPCHitDetector#findNPCInReach} which uses a
     * widened hit cone (~26° instead of the old ~10°) for more forgiving aim.
     */
    private NPC findNPCInReach(Vector3 cameraPos, Vector3 direction, float reach) {
        return NPCHitDetector.findNPCInReach(cameraPos, direction, reach,
                npcManager.getNPCs(), blockBreaker, world);
    }

    /**
     * Find the nearest prop within punch reach using ray-AABB intersection.
     *
     * <p>Issue #752: Non-block 3D objects (props) could not be targeted or destroyed.
     * This method casts a ray from the camera and tests it against each prop's AABB,
     * returning the index of the nearest prop whose AABB is intersected within
     * {@code reach} distance, or {@code -1} if none.
     */
    /** Issue #856: Returns a display name for an MC champion (used in the championship ladder). */
    private static String mcChampionDisplayName(MCBattleSystem.Champion champion) {
        switch (champion) {
            case MARCHETTI_MC:   return "Marchetti MC";
            case STREET_LADS_MC: return "Street Lads MC";
            case COUNCIL_MC:     return "Council MC";
            default: return champion.name();
        }
    }

    private int findPropInReach(Vector3 origin, Vector3 direction, float reach) {
        List<PropPosition> props = world.getPropPositions();
        int bestIndex = -1;
        float bestDist = reach + 1f;
        for (int i = 0; i < props.size(); i++) {
            PropPosition prop = props.get(i);
            ragamuffin.entity.AABB box = prop.getAABB();
            float t = rayAABBIntersect(origin, direction, box);
            if (t >= 0f && t < bestDist) {
                bestDist = t;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Find the nearest small item within reach distance of the player.
     *
     * <p>Issue #872: Small 3D objects are lootable. Uses a sphere test (proximity
     * rather than ray cast) since small items are small targets and picking them
     * up by walking close makes for more natural game feel.</p>
     *
     * @return the index of the nearest small item within {@code reach}, or {@code -1}
     */
    private int findSmallItemInReach(Vector3 playerPos, float reach) {
        List<ragamuffin.building.SmallItem> items = world.getSmallItems();
        int bestIndex = -1;
        float bestDist = reach + 1f;
        for (int i = 0; i < items.size(); i++) {
            ragamuffin.building.SmallItem item = items.get(i);
            Vector3 itemPos = item.getPosition();
            float dist = playerPos.dst(itemPos.x, playerPos.y, itemPos.z);
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Ray-AABB intersection test (slab method).
     *
     * @return the entry distance along the ray, or {@code -1} if no intersection
     *         within a positive distance.
     */
    private float rayAABBIntersect(Vector3 origin, Vector3 dir, ragamuffin.entity.AABB box) {
        float tmin = 0f;
        float tmax = Float.MAX_VALUE;

        // X slab
        if (Math.abs(dir.x) < 1e-8f) {
            if (origin.x < box.getMinX() || origin.x > box.getMaxX()) return -1f;
        } else {
            float ood = 1f / dir.x;
            float t1 = (box.getMinX() - origin.x) * ood;
            float t2 = (box.getMaxX() - origin.x) * ood;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1f;
        }

        // Y slab
        if (Math.abs(dir.y) < 1e-8f) {
            if (origin.y < box.getMinY() || origin.y > box.getMaxY()) return -1f;
        } else {
            float ood = 1f / dir.y;
            float t1 = (box.getMinY() - origin.y) * ood;
            float t2 = (box.getMaxY() - origin.y) * ood;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1f;
        }

        // Z slab
        if (Math.abs(dir.z) < 1e-8f) {
            if (origin.z < box.getMinZ() || origin.z > box.getMaxZ()) return -1f;
        } else {
            float ood = 1f / dir.z;
            float t1 = (box.getMinZ() - origin.z) * ood;
            float t2 = (box.getMaxZ() - origin.z) * ood;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1f;
        }

        return tmin >= 0f ? tmin : (tmax >= 0f ? 0f : -1f);
    }

    private void renderUI() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Fix #52: establish 2D orthographic projection before any ShapeRenderer or
        // SpriteBatch calls.  Without this, the stale 3D perspective matrix left over
        // from modelBatch.end() is used, causing all HUD elements to be drawn in
        // 3D world-space coordinates and become invisible to the player.  Setting it
        // here makes renderUI() self-contained and correct in every game state.
        com.badlogic.gdx.math.Matrix4 ortho = new com.badlogic.gdx.math.Matrix4();
        ortho.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(ortho);
        spriteBatch.setProjectionMatrix(ortho);

        // Clear and update hover tooltip zones each frame
        hoverTooltipSystem.clear();
        float delta = Gdx.graphics.getDeltaTime();

        // Render first-person arm (hidden during car driving — third-person camera)
        if (!openingSequence.isActive() && !carDrivingSystem.isInCar()) {
            firstPersonArm.render(shapeRenderer, screenWidth, screenHeight);
        }

        // Phase 8: Render GameHUD (health/hunger/energy bars + crosshair)
        // Fix #726: suppress GameHUD when quest log is open so it does not bleed around the panel edges
        if (!openingSequence.isActive() && !questLogUI.isVisible()) {
            // Phase 12: Update weather display
            gameHUD.setWeather(weatherSystem.getCurrentWeather());

            // Phase 14: Update night status for police warning banner and dodge indicator
            gameHUD.setNight(timeSystem.isNight());

            // Issue #289: Only update crosshair-related state when no UI overlay is open.
            // The status bars (health/hunger/energy) are always rendered, but the crosshair
            // and block-break progress arc must be hidden when inventory/crafting/help is open,
            // mirroring the guard already applied to renderBlockHighlight().
            boolean showCrosshair = !isUIBlocking();
            if (showCrosshair) {
                // Update block/prop break progress on crosshair (account for equipped tool)
                tmpCameraPos.set(camera.position);
                tmpDirection.set(camera.direction);
                int hudSelectedSlot = hotbarUI.getSelectedSlot();
                Material hudEquipped = inventory.getItemInSlot(hudSelectedSlot);
                Material hudTool = (hudEquipped != null && Tool.isTool(hudEquipped)) ? hudEquipped : null;
                RaycastResult targetBlock = blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PUNCH_REACH);
                // Issue #752: Also check for prop targets in HUD
                int hudTargetPropIndex = findPropInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);

                // Determine which (block or prop) is closer for HUD display
                float hudBlockDist = (targetBlock != null) ? targetBlock.getDistance() : Float.MAX_VALUE;
                float hudPropDist  = Float.MAX_VALUE;
                if (hudTargetPropIndex >= 0 && hudTargetPropIndex < world.getPropPositions().size()) {
                    float d = rayAABBIntersect(tmpCameraPos, tmpDirection, world.getPropPositions().get(hudTargetPropIndex).getAABB());
                    if (d >= 0f) hudPropDist = d;
                }
                boolean hudShowProp  = hudTargetPropIndex >= 0 && hudPropDist  <= hudBlockDist;
                boolean hudShowBlock = targetBlock != null        && hudBlockDist <  hudPropDist;

                // Issue #287: Check NPC target first — NPC takes priority over block/prop for both
                // block break progress and target name (avoids two separate findNPCInReach calls)
                NPC hudTargetNPC = findNPCInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);
                if (hudTargetNPC != null) {
                    gameHUD.setBlockBreakProgress(0f); // NPC target — don't show block/prop damage
                } else if (hudShowProp) {
                    float progress = propBreaker.getBreakProgress(world, hudTargetPropIndex);
                    gameHUD.setBlockBreakProgress(progress);
                } else if (hudShowBlock) {
                    float progress = blockBreaker.getBreakProgress(world, targetBlock.getBlockX(),
                        targetBlock.getBlockY(), targetBlock.getBlockZ(), hudTool);
                    gameHUD.setBlockBreakProgress(progress);
                } else {
                    gameHUD.setBlockBreakProgress(0f);
                }

                // Issue #189: Update target reticule label — NPC takes priority over block/prop
                // Issue #872: Small items show pickup hint when player is nearby
                int hudSmallItemIndex = findSmallItemInReach(player.getPosition(), PUNCH_REACH);
                if (hudTargetNPC != null) {
                    gameHUD.setTargetName(formatNPCName(hudTargetNPC.getType()));
                } else if (hudSmallItemIndex >= 0) {
                    ragamuffin.building.Material smallItemMat = world.getSmallItems().get(hudSmallItemIndex).getMaterial();
                    gameHUD.setTargetName("[E] Pick up " + smallItemMat.getDisplayName());
                } else if (hudShowProp) {
                    ragamuffin.world.PropType hudPropType = world.getPropPositions().get(hudTargetPropIndex).getType();
                    // Issue #852: Show interaction hint for fruit machine
                    if (hudPropType == ragamuffin.world.PropType.FRUIT_MACHINE) {
                        gameHUD.setTargetName("[E] Play fruit machine (1 coin)");
                    } else {
                        gameHUD.setTargetName(formatPropName(hudPropType));
                    }
                } else if (hudShowBlock) {
                    gameHUD.setTargetName(formatBlockName(targetBlock.getBlockType()));
                } else {
                    gameHUD.setTargetName(null);
                }
            } else {
                // UI overlay is open — clear crosshair state so stale values don't
                // linger when the overlay is closed
                gameHUD.setBlockBreakProgress(0f);
                gameHUD.setTargetName(null);
            }

            gameHUD.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltipSystem, showCrosshair);
        }

        // Always render hotbar (unless opening sequence active or quest log is open)
        // Fix #726: suppress hotbar when quest log is open so it does not show through the overlay
        if (!openingSequence.isActive() && !questLogUI.isVisible() && !criminalRecordUI.isVisible()) {
            hotbarUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltipSystem);
        }

        // Render clock — suppressed when quest log is open to prevent overlap (#726)
        if (!questLogUI.isVisible() && !criminalRecordUI.isVisible()) {
            clockHUD.render(spriteBatch, font, screenWidth, screenHeight);
        }

        // Render inventory if visible
        if (inventoryUI.isVisible()) {
            inventoryUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltipSystem);
        }

        // Render help if visible
        if (helpUI.isVisible()) {
            helpUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Render crafting if visible
        if (craftingUI.isVisible()) {
            craftingUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltipSystem);
        }

        // Render achievements overlay if visible
        if (achievementsUI.isVisible()) {
            achievementsUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Issue #850: Render skills overlay if visible
        if (skillsUI.isVisible()) {
            skillsUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Issue #464: Render quest log overlay if visible
        if (questLogUI.isVisible()) {
            questLogUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Issue #659: Render criminal record overlay if visible
        if (criminalRecordUI.isVisible()) {
            criminalRecordUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Issue #828: Render JobCentre UI if visible
        if (jobCentreUI != null && jobCentreUI.isVisible()) {
            jobCentreUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight,
                    jobCentreSystem, player, inventory);
        }

        // Issue #864: Render FenceTradeUI if visible
        if (fenceTradeUI.isVisible()) {
            fenceTradeUI.render(spriteBatch, shapeRenderer, font,
                    screenWidth, screenHeight,
                    fenceSystem, player, inventory);
        }

        // Issue #868: Render BootSaleUI if visible
        if (bootSaleUI.isVisible()) {
            bootSaleUI.render(screenWidth, screenHeight);
        }

        // Issue #799: Render corner shop HUD status bar when shop is open
        if (cornerShopSystem != null && cornerShopSystem.hasShop() && cornerShopSystem.isShopOpen()) {
            spriteBatch.begin();
            font.draw(spriteBatch,
                    String.format("SHOP  Revenue: %d  Heat: %d%%",
                            cornerShopSystem.getDailyRevenue(),
                            cornerShopSystem.getHeat()),
                    20, 80);
            spriteBatch.end();
        }

        // Issue #837: Render stall HUD status bar when stall is open
        if (stallSystem != null && stallSystem.isStallOpen()) {
            spriteBatch.begin();
            font.draw(spriteBatch,
                    String.format("STALL  Sold: %d  Income: %d",
                            stallSystem.getLifetimeSales(),
                            stallSystem.getStallCoinTotal()),
                    20, 60);
            spriteBatch.end();
        }

        // Render damage flash overlay
        float flashIntensity = player.getDamageFlashIntensity();
        if (flashIntensity > 0f) {
            renderDamageFlash(flashIntensity, screenWidth, screenHeight);
        }

        // Issue #497: Render quest tracker (top-right corner, always visible during gameplay)
        if (!openingSequence.isActive() && !questLogUI.isVisible()) {
            questTrackerUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Issue #13: render NPC speech log (bottom-right corner)
        // Fix #726: suppress speech log when quest log is open so it does not render on top of the overlay
        if (!openingSequence.isActive() && !questLogUI.isVisible()) {
            speechLogUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Render tooltip if active
        if (tooltipSystem.isActive()) {
            renderTooltip();
        }

        // Fix #453: Render achievement notification banner if one is active
        if (achievementSystem.isNotificationActive()) {
            renderAchievementNotification();
        }

        // Fix #357/#395: Suppress hover tooltip advancement and rendering while paused so
        // dwell timers do not accumulate and tooltip bubbles do not appear on top of
        // the pause menu.  Call reset() to discard registered zones AND reset all dwell
        // state (hoverTime, lastHoverZoneKey, activeTooltip) so that on resume the full
        // 0.3 s dwell is required before a tooltip fires again.
        if (state == GameState.PAUSED) {
            hoverTooltipSystem.reset();
        } else {
            // Update and render hover tooltips last (on top of everything)
            hoverTooltipSystem.update(delta);
            hoverTooltipSystem.render(spriteBatch, shapeRenderer, font);
        }
    }

    private void renderTooltip() {
        String message = tooltipSystem.getCurrentTooltip();
        if (message != null) {
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();

            // Measure text width for a centred background box
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
                    new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, message);
            float textW = layout.width;
            float textH = layout.height;
            float padding = 10f;
            float boxW = textW + padding * 2;
            float boxH = textH + padding * 2;
            float boxX = (screenWidth - boxW) / 2f;
            float boxY = 70f;

            // Dark semi-transparent background
            com.badlogic.gdx.math.Matrix4 tooltipProj = new com.badlogic.gdx.math.Matrix4();
            tooltipProj.setToOrtho2D(0, 0, screenWidth, screenHeight);
            shapeRenderer.setProjectionMatrix(tooltipProj);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.75f);
            shapeRenderer.rect(boxX, boxY, boxW, boxH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            // Text centred in the box
            spriteBatch.setProjectionMatrix(tooltipProj);
            spriteBatch.begin();
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(spriteBatch, message, boxX + padding, boxY + boxH - padding);
            font.setColor(1f, 1f, 1f, 1f);
            spriteBatch.end();
        }
    }

    private void renderAchievementNotification() {
        String message = achievementSystem.getCurrentNotification();
        if (message != null) {
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();
            float alpha = achievementSystem.getNotificationAlpha();

            // Measure text for a centred banner near the top of the screen
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout =
                    new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, message);
            float textW = layout.width;
            float textH = layout.height;
            float padding = 12f;
            float boxW = textW + padding * 2;
            float boxH = textH + padding * 2;
            float boxX = (screenWidth - boxW) / 2f;
            float boxY = screenHeight - boxH - 40f;

            com.badlogic.gdx.math.Matrix4 proj = new com.badlogic.gdx.math.Matrix4();
            proj.setToOrtho2D(0, 0, screenWidth, screenHeight);
            shapeRenderer.setProjectionMatrix(proj);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.8f * alpha);
            shapeRenderer.rect(boxX, boxY, boxW, boxH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            spriteBatch.setProjectionMatrix(proj);
            spriteBatch.begin();
            font.setColor(1f, 0.85f, 0.2f, alpha);
            font.draw(spriteBatch, message, boxX + padding, boxY + boxH - padding);
            font.setColor(1f, 1f, 1f, 1f);
            spriteBatch.end();
        }
    }

    private void renderSpeechBubbles() {
        float delta = Gdx.graphics.getDeltaTime();
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Fix #32: set orthographic projection so pixel-space coordinates from
        // camera.project() are interpreted correctly by shapeRenderer and spriteBatch.
        // Without this, the stale 3D perspective matrix from modelBatch.end() is used,
        // causing all speech bubbles to be invisible or drawn at nonsense coordinates.
        com.badlogic.gdx.math.Matrix4 ortho = new com.badlogic.gdx.math.Matrix4();
        ortho.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(ortho);
        spriteBatch.setProjectionMatrix(ortho);

        for (NPC npc : npcManager.getNPCs()) {
            if (!npc.isSpeaking()) continue;

            // Project NPC head position to screen coordinates
            tmpCameraPos.set(npc.getPosition());
            tmpCameraPos.y += 2.2f; // Above head

            camera.project(tmpCameraPos, 0, 0, screenWidth, screenHeight);

            // Skip if behind camera
            if (tmpCameraPos.z > 1.0f || tmpCameraPos.z < 0f) continue;

            float sx = tmpCameraPos.x;
            float sy = tmpCameraPos.y;

            String text = npc.getSpeechText();
            String[] lines = text.split("\n", -1);
            float lineHeight = 16f;
            float padding = 12f;
            float maxLineWidth = 0f;
            for (String line : lines) {
                float lw = line.length() * 7f;
                if (lw > maxLineWidth) maxLineWidth = lw;
            }
            float bubbleW = maxLineWidth + padding * 2;
            float bubbleH = lines.length * lineHeight + padding;
            float bubbleX = sx - bubbleW / 2f;
            float bubbleY = sy;

            // Draw speech bubble background
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
            shapeRenderer.rect(bubbleX, bubbleY, bubbleW, bubbleH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            // Draw text line by line
            spriteBatch.begin();
            font.setColor(1f, 1f, 1f, 1f);
            float textY = bubbleY + bubbleH - (padding / 2f);
            for (String line : lines) {
                font.draw(spriteBatch, line, bubbleX + padding, textY);
                textY -= lineHeight;
            }
            spriteBatch.end();
        }
    }

    private void handleEscapePress() {
        // Close any open UI first
        if (jobCentreUI != null && jobCentreUI.isVisible()) {
            jobCentreUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (fenceTradeUI.isVisible()) {
            fenceTradeUI.hide();
            fenceSystem.closeTradeUI();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (state == GameState.BOOT_SALE_OPEN) {
            bootSaleUI.hide();
            state = GameState.PLAYING;
            Gdx.input.setCursorCatched(true);
        } else if (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen()) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            activeShopkeeperNPC = null;
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (achievementsUI.isVisible()) {
            achievementsUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (inventoryUI.isVisible()) {
            inventoryUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (helpUI.isVisible()) {
            helpUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (craftingUI.isVisible()) {
            craftingUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (questLogUI.isVisible()) {
            questLogUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (criminalRecordUI.isVisible()) {
            criminalRecordUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (skillsUI.isVisible()) {
            skillsUI.hide();
            Gdx.input.setCursorCatched(state == GameState.PLAYING);
        } else if (state == GameState.PLAYING) {
            transitionToPaused();
        } else if (state == GameState.PAUSED) {
            transitionToPlaying();
        }
    }

    private void restartGame() {
        // Dispose old chunk renderer meshes
        chunkRenderer.dispose();

        // Regenerate world with a new seed
        world = new World(System.currentTimeMillis());
        world.generate();

        // Rebuild signage from the new world's landmarks
        signageRenderer = new ragamuffin.render.SignageRenderer();
        signageRenderer.buildFromLandmarks(world.getAllLandmarks());

        // Reset player at the park centre
        float spawnY = calculateSpawnHeight(world, 0, 0) + 1.0f;
        player = new Player(0, spawnY, 0);
        camera.position.set(player.getPosition());
        camera.position.y += Player.EYE_HEIGHT;
        cameraPitch = 0f;
        cameraYaw = 0f;

        // Rebuild chunk rendering — meshes built lazily in render loop
        chunkRenderer = new ChunkRenderer();
        meshBuilder.setWorld(world);
        world.updateLoadedChunks(player.getPosition());
        // Build a small set immediately, rest lazily
        int restartCount = 0;
        for (Chunk chunk : world.getDirtyChunks()) {
            if (restartCount >= 50) break;
            chunkRenderer.updateChunk(chunk, meshBuilder);
            world.markChunkClean(chunk);
            restartCount++;
        }

        // Reset inventory
        inventory = new Inventory(36);
        inventoryUI = new InventoryUI(inventory);
        hotbarUI = new HotbarUI(inventory);
        // Fix #359: Recreate craftingSystem so any in-progress crafting state (selected recipe,
        // scroll offset) from the previous session does not leak into the new game.
        craftingSystem = new CraftingSystem();
        craftingUI = new CraftingUI(craftingSystem, inventory);
        // Fix #325: Recreate helpUI so its isVisible() state resets to false.
        // Without this, if the player had the help screen open before restarting,
        // helpUI.isVisible() remains true in the new session, causing isUIBlocking()
        // to return true and suppressing all player input indefinitely.
        helpUI = new HelpUI();
        // Fix #329: Recreate pauseMenu so visible and selectedOption reset to their defaults
        // (visible=false, selectedOption=OPTION_RESUME). Without this, if the player opened the
        // pause menu and selected "Restart", visible stays true and selectedOption stays at index 1
        // ("Restart") instead of the default "Resume" — mirroring the pattern used for helpUI (#325).
        pauseMenu = new PauseMenu();
        // Fix #317: Recreate hoverTooltipSystem so dwell timers from the previous session
        // do not bleed into the new game (mirrors the pattern used for all other UI systems).
        hoverTooltipSystem = new HoverTooltipSystem();

        // Reset NPCs — wire blockBreaker first (matches initGame order) so demolishBlock
        // hit-counter clears work from the start of the restarted game
        blockBreaker = new BlockBreaker();
        blockPlacer.setBlockBreaker(blockBreaker);
        // Fix #309: Dispose old NPCRenderer to release per-NPC ModelInstance arrays and
        // native GPU resources (Mesh/Material) before creating new NPCManager/NPCs.
        npcRenderer.dispose();
        npcRenderer = new NPCRenderer();
        npcManager = new NPCManager();
        npcManager.setBlockBreaker(blockBreaker);
        spawnInitialNPCs();
        // Fix #509: Recreate interactionSystem before spawnBuildingNPCs() so the live
        // quest registry is available when deciding which buildings get a quest-giver NPC.
        interactionSystem = new InteractionSystem();
        spawnBuildingNPCs(); // Fix #479: spawn quest-giver NPCs inside buildings on restart

        // Reset time and lighting
        // Fix #337: Remove old directional light from environment before creating new
        // LightingSystem, so the catch-block fallback in getDirectionalLightFromEnvironment
        // does not accumulate an extra DirectionalLight on every restart.
        lightingSystem.dispose();
        timeSystem = new TimeSystem(8.0f);
        lightingSystem = new LightingSystem(environment);
        clockHUD = new ClockHUD();

        // Fix #323: Recreate SkyRenderer so cloudTime resets to 0 and clouds start
        // from their initial positions rather than carrying over from the previous session.
        skyRenderer = new ragamuffin.render.SkyRenderer();

        // Reset game systems (blockBreaker already created above)
        tooltipSystem = new TooltipSystem();
        tooltipSystem.setOnTooltipShow(() -> soundSystem.play(ragamuffin.audio.SoundEffect.TOOLTIP));
        // Fix #479: Recreate questLogUI bound to the fresh registry so old quests don't bleed in
        // Fix #523: Pass inventory so quest log can show remaining count
        questLogUI = new ragamuffin.ui.QuestLogUI(interactionSystem.getQuestRegistry(), inventory);
        // Issue #497: Recreate questTrackerUI bound to the fresh registry
        // Fix #511: Pass inventory so tracker can show current/required counts
        questTrackerUI = new ragamuffin.ui.QuestTrackerUI(interactionSystem.getQuestRegistry(), inventory);
        healingSystem = new HealingSystem();
        // Issue #166: Sync HealingSystem position after teleport so the next update()
        // does not compute a spurious speed from the restart spawn distance.
        healingSystem.resetPosition(player.getPosition());
        // Fix #485: Reset distance-tracking vector to the new spawn position so the first
        // updatePlayingSimulation() frame does not measure the full distance between the
        // previous session's last position and the new spawn (mirrors the within-session
        // respawn reset at lines 865-866).
        distanceTravelledAchievement = 0f;
        lastPlayerPosForDistance.set(player.getPosition());
        respawnSystem = new RespawnSystem();
        respawnSystem.setSpawnY(calculateSpawnHeight(world, 0, 0) + 1.0f);
        weatherSystem = new WeatherSystem();
        warmthSystem = new WarmthSystem();
        noiseSystem = new NoiseSystem();
        campfireSystem = new CampfireSystem();
        exposureSystem = new ExposureSystem();
        arrestSystem = new ArrestSystem();
        arrestSystem.setRespawnY(calculateSpawnHeight(world, 0, 0) + 1.0f);
        greggsRaidSystem = new GreggsRaidSystem();
        gangTerritorySystem.reset();
        // Issue #816: Reset neighbourhood watch system so anger and tier don't carry over
        neighbourhoodWatchSystem = new NeighbourhoodWatchSystem();
        // Issue #818: Reset disguise system so cover and disguise state don't carry over
        disguiseSystem = new DisguiseSystem();
        disguiseSystem.setAchievementSystem(achievementSystem);
        disguiseSystem.setRumourNetwork(rumourNetwork);
        // Issue #781: Reset graffiti system so marks and timers don't carry over between games
        graffitiSystem = new GraffitiSystem();
        // Issue #824: Reset street economy system so NPC needs and market state don't carry over
        streetEconomySystem = new StreetEconomySystem();
        // Fix #899: Reset market event cooldown so events restart on a fresh schedule
        marketEventCooldown = 120f + new java.util.Random().nextFloat() * 180f;
        // Issue #799: Reset corner shop system so shop state, heat and stock don't carry over
        cornerShopSystem = new CornerShopSystem();
        // Issue #852: Reset fruit machine so RNG state doesn't carry over between sessions
        fruitMachine = new FruitMachine(new java.util.Random());
        // Issue #837: Reset stall system so stall state, stock and inspector don't carry over
        stallSystem = new StallSystem();
        // Issue #826: Reset witness system so evidence props, CCTV timers and witness state don't carry over
        witnessSystem = new WitnessSystem();
        witnessSystem.setCriminalRecord(player.getCriminalRecord());
        witnessSystem.setRumourNetwork(rumourNetwork);
        witnessSystem.setAchievementSystem(achievementSystem);
        gameHUD = new GameHUD(player);
        gameHUD.setNeighbourhoodWatchSystem(neighbourhoodWatchSystem);
        gameHUD.setDisguiseSystem(disguiseSystem);
        openingSequence = new OpeningSequence();
        speechLogUI = new SpeechLogUI();
        deathMessage = null;

        // Issue #171: Reset particle system
        if (particleSystem != null) {
            particleSystem.clear();
        } else {
            particleSystem = new ragamuffin.render.ParticleSystem();
        }
        footstepDustTimer = 0f;
        rainTimer = 0f;
        // Fix #345: Reset SoundSystem footstep timer so the first footstep of the new session
        // is not fired early due to a mid-cycle timer value carried over from the previous session.
        soundSystem.resetFootstepTimer();

        // Fix #563: Close and clear any active shop menu so isUIBlocking() returns false
        // in the new session and player input is not permanently suppressed.
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            activeShopkeeperNPC = null;
        }

        // Fix #299: Clear sticky punch state so auto-punch doesn't fire in the first frame
        // of the new game session (mirrors the same reset in transitionToPaused() and on death).
        punchHeldTimer = 0f;
        lastPunchTargetKey = null;
        inputHandler.resetPunchHeld();
        // Fix #363: Clear stale toggle-key state so UI overlays (inventory, help, crafting)
        // don't open on frame 1 of the new session if those keys were pressed on the same
        // frame the Restart button was activated (mirrors the pattern used for resetPunchHeld).
        inputHandler.resetInventory();
        inputHandler.resetHelp();
        inputHandler.resetCrafting();
        inputHandler.resetAchievements();
        inputHandler.resetInteract();
        inputHandler.resetQuestLog(); // Fix #479: prevent stale Q-key from re-opening log on frame 1
        // Fix #461: Hide achievements overlay so it does not persist across game restarts.
        achievementsUI.hide();
        // Fix #489: Reset achievement system so previous session's unlocks and progress
        // do not carry over into the new game.
        achievementSystem.reset();
        // Fix #365: Clear stale leftClickReleased flag so the inventory drag-and-drop
        // subsystem is not in an inconsistent state on frame 1 of the new session.
        // When the player mouse-clicks "Restart", touchUp() sets leftClickReleased=true
        // before restartGame() runs; without this reset, handleUIInput() sees a phantom
        // release event before any real mouse activity occurs.
        // Fix #605: Also clear leftClickPressed so the touchDown() event from clicking
        // "Restart" does not fire a phantom left-click on frame 1 of the new session.
        // This mirrors the pattern in transitionToPlaying() and transitionToPaused()
        // where both resetLeftClick() and resetLeftClickReleased() are always called together.
        inputHandler.resetLeftClick();
        inputHandler.resetLeftClickReleased();
        // Fix #603: Clear stale rightClickPressed so a right-click buffered during the menu
        // does not fire a phantom right-click event on frame 1 of the new session
        inputHandler.resetRightClick();
        // Fix #575: Clear stale scroll so hotbar doesn't cycle on frame 1 of the new session
        inputHandler.resetScroll();
        // Fix #581: Clear stale action inputs so frame 1 of the new session does not fire
        // spurious punches, jumps, dodges, hotbar/crafting slot changes, or menu navigation.
        inputHandler.resetPunch();
        inputHandler.resetPlace();
        inputHandler.resetJump();
        inputHandler.resetDodge();
        inputHandler.resetHotbarSlot();
        inputHandler.resetCraftingSlot();
        inputHandler.resetEnter();
        inputHandler.resetUp();
        inputHandler.resetDown();
        // Fix #585: Clear stale ESC so frame 1 of the new session does not trigger
        // transitionToPaused() (consistent with resetEscape() calls in sibling methods).
        inputHandler.resetEscape();
        // Fix #643: Clear polled movement flags so a held WASD/sprint key does not cause
        // immediate unwanted movement on frame 1 of the new session.
        inputHandler.resetMovement();
        // Fix #331: Recreate firstPersonArm so swinging and swingTimer reset to their defaults
        // (swinging=false, swingTimer=0). Without this, a mid-punch animation from the previous
        // session leaks into the new game, showing the arm in a partially-extended position.
        firstPersonArm = new FirstPersonArm();

        // Fix #307: Reset prevDamageFlashIntensity so the rising-edge detector fires correctly
        // on the first hit of a new session (prevents banner from being suppressed).
        prevDamageFlashIntensity = 0f;

        // Issue #373: Reset cinematic camera for the new session
        cinematicCamera = new ragamuffin.ui.CinematicCamera();

        // Transition: cinematic fly-through and text sequence run simultaneously (Fix #428)
        if (mainMenuScreen.isSkipIntroEnabled()) {
            state = GameState.PLAYING;
            openingSequence.start();
            openingSequence.skip();
            Gdx.input.setCursorCatched(true);
        } else {
            state = GameState.CINEMATIC;
            cinematicCamera.start();
            openingSequence.start();
            Gdx.input.setCursorCatched(false);
        }
    }

    private void transitionToPlaying() {
        state = GameState.PLAYING;
        pauseMenu.hide();
        // Fix #461: Hide achievements overlay on state transition so it doesn't persist
        achievementsUI.hide();
        // Fix #565: Hide quest log overlay on state transition (mirrors achievementsUI fix)
        questLogUI.hide();
        // Fix #567: Defensively hide overlays that should not persist across a pause/resume cycle
        inventoryUI.hide();
        craftingUI.hide();
        helpUI.hide();
        // Fix #567: Clear stale crafting/hotbar slot so a number key pressed on the same frame
        // as Resume does not auto-select a recipe on the first PLAYING frame
        inputHandler.resetCraftingSlot();
        inputHandler.resetHotbarSlot();
        // Fix #571: Clear stale punch/place so left-click or right-click buffered during the
        // pause transition does not fire on the first PLAYING frame after resume
        inputHandler.resetPunch();
        // Fix #597: Clear stale punchHeld so hold-punch auto-repeat does not fire on the
        // first PLAYING frame after resume (mirrors the identical call in transitionToPaused())
        inputHandler.resetPunchHeld();
        punchHeldTimer = 0f;
        lastPunchTargetKey = null;
        inputHandler.resetPlace();
        // Fix #573: Clear stale UI-toggle keys so I/H/C/Tab/Q pressed on the same frame as
        // ESC-to-resume do not re-open their panels on the first PLAYING frame
        inputHandler.resetInventory();
        inputHandler.resetHelp();
        inputHandler.resetCrafting();
        inputHandler.resetAchievements();
        inputHandler.resetQuestLog();
        // Fix #575: Clear stale scroll so hotbar doesn't cycle on the first PLAYING frame
        // after resume if the player scrolled while the pause menu was open
        inputHandler.resetScroll();
        // Fix #577: Clear stale interact/jump/dodge so Space/Ctrl/E pressed on the same frame
        // as Resume do not fire on the first PLAYING frame after resume
        inputHandler.resetInteract();
        inputHandler.resetJump();
        inputHandler.resetDodge();
        // Fix #587: Clear stale up/down arrow-key flags so they don't leak into the first
        // PLAYING frame after resume (mirrors the identical pair in transitionToPaused())
        inputHandler.resetUp();
        inputHandler.resetDown();
        // Fix #589: Clear stale leftClickPressed/leftClickReleased so a mouse-click buffered
        // during the pause transition does not fire a spurious UI action on the first PLAYING frame
        inputHandler.resetLeftClick();
        inputHandler.resetLeftClickReleased();
        // Fix #603: Clear stale rightClickPressed so a right-click buffered during the pause
        // transition does not fire a phantom right-click event on the first PLAYING frame
        inputHandler.resetRightClick();
        // Fix #595: Clear stale escapePressed so ESC-to-resume does not immediately re-pause
        // on the first PLAYING frame (mirrors the identical call in transitionToPaused() #591)
        inputHandler.resetEscape();
        // Fix #599: Clear stale enterPressed so Enter-to-resume does not fire crafting confirmation
        // on the first PLAYING frame (mirrors the identical call in transitionToPaused(), finishCinematic(), restartGame())
        inputHandler.resetEnter();
        // Fix #643: Clear polled movement flags so a held WASD/sprint key does not cause
        // immediate unwanted movement on the first PLAYING frame after transitioning.
        inputHandler.resetMovement();
        // Fix #633: Close and clear any active shop menu so isUIBlocking() returns false
        // after resume — mirrors the CINEMATIC branch (Fix #623) and the justDied paths (Fix #601/#621).
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            activeShopkeeperNPC = null;
        }
        Gdx.input.setCursorCatched(true);
    }

    private void transitionToPaused() {
        state = GameState.PAUSED;
        pauseMenu.show();
        Gdx.input.setCursorCatched(false);
        // Fix #275: Clear sticky punch state so auto-punch doesn't fire on resume
        inputHandler.resetPunchHeld();
        punchHeldTimer = 0f;
        lastPunchTargetKey = null;
        // Fix #571: Clear stale punchPressed so a left-click on the same frame as ESC does not
        // fire a punch on the first PLAYING frame after resume (mirrors resetPunchHeld fix above)
        inputHandler.resetPunch();
        // Fix #569: Clear stale interact (E key) so it doesn't fire on first PLAYING frame after resume
        inputHandler.resetInteract();
        // Clear stale jump/dodge so they don't fire on first PLAYING frame after resume
        inputHandler.resetJump();
        inputHandler.resetDodge();
        // Fix #573: Clear stale UI-toggle keys so I/H/C/Tab/Q pressed on the same frame as
        // ESC-to-pause do not fire on the first PLAYING frame after resume
        inputHandler.resetInventory();
        inputHandler.resetHelp();
        inputHandler.resetCrafting();
        inputHandler.resetAchievements();
        inputHandler.resetQuestLog();
        // Fix #575: Clear stale scroll so hotbar doesn't cycle on the first PLAYING frame
        // after resume if the player scrolled while the pause menu was open
        inputHandler.resetScroll();
        // Fix #579: Clear stale enterPressed/hotbarSlotPressed/craftingSlotPressed so a
        // simultaneous ENTER or number key press on the same frame as ESC does not
        // immediately fire a pause-menu action or leave hotbar/crafting in an inconsistent state
        inputHandler.resetEnter();
        inputHandler.resetHotbarSlot();
        inputHandler.resetCraftingSlot();
        // Fix #583: Clear stale placePressed/upPressed/downPressed so a right-click or
        // arrow key press on the same frame as ESC does not fire spurious block placement
        // or pause-menu navigation on the first frame after transition
        inputHandler.resetPlace();
        inputHandler.resetUp();
        inputHandler.resetDown();
        // Fix #589: Clear stale leftClickPressed/leftClickReleased so a mouse-click on the
        // same frame as ESC does not activate a pause-menu option the player did not intend
        inputHandler.resetLeftClick();
        inputHandler.resetLeftClickReleased();
        // Fix #603: Clear stale rightClickPressed so a right-click buffered on the same frame
        // as ESC does not fire a phantom right-click event on the first PAUSED frame
        inputHandler.resetRightClick();
        // Fix #591: Clear stale escapePressed so the first PAUSED frame does not immediately
        // call handleEscapePress() again and bounce back to PLAYING
        inputHandler.resetEscape();
        // Fix #643: Clear polled movement flags so a held WASD/sprint key does not cause
        // immediate unwanted movement on the first PLAYING frame after resuming from pause.
        inputHandler.resetMovement();
        // Fix #633: Close and clear any active shop menu so isUIBlocking() returns false
        // if the player resumes — mirrors the identical guard in transitionToPlaying() and
        // the CINEMATIC branch (Fix #623). Belt-and-suspenders: ESC normally closes the menu
        // via handleEscapePress() first, but this guard handles any path that calls
        // transitionToPaused() directly (e.g. arrest blocks).
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            activeShopkeeperNPC = null;
        }
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState newState) {
        this.state = newState;
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public BlockBreaker getBlockBreaker() {
        return blockBreaker;
    }

    public TooltipSystem getTooltipSystem() {
        return tooltipSystem;
    }

    public InventoryUI getInventoryUI() {
        return inventoryUI;
    }

    public HelpUI getHelpUI() {
        return helpUI;
    }

    public HotbarUI getHotbarUI() {
        return hotbarUI;
    }

    public CraftingUI getCraftingUI() {
        return craftingUI;
    }

    public CraftingSystem getCraftingSystem() {
        return craftingSystem;
    }

    public BlockPlacer getBlockPlacer() {
        return blockPlacer;
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    public TimeSystem getTimeSystem() {
        return timeSystem;
    }

    public LightingSystem getLightingSystem() {
        return lightingSystem;
    }

    public ClockHUD getClockHUD() {
        return clockHUD;
    }

    public Environment getEnvironment() {
        return environment;
    }

    // Phase 8 getters
    public GameHUD getGameHUD() {
        return gameHUD;
    }

    public PauseMenu getPauseMenu() {
        return pauseMenu;
    }

    public MainMenuScreen getMainMenuScreen() {
        return mainMenuScreen;
    }

    public OpeningSequence getOpeningSequence() {
        return openingSequence;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public NPCRenderer getNPCRenderer() {
        return npcRenderer;
    }

    public FirstPersonArm getFirstPersonArm() {
        return firstPersonArm;
    }

    public HoverTooltipSystem getHoverTooltipSystem() {
        return hoverTooltipSystem;
    }

    public GangTerritorySystem getGangTerritorySystem() {
        return gangTerritorySystem;
    }

    public ragamuffin.render.ParticleSystem getParticleSystem() {
        return particleSystem;
    }

    public ragamuffin.render.SkyRenderer getSkyRenderer() {
        return skyRenderer;
    }

    public ragamuffin.ui.AchievementsUI getAchievementsUI() {
        return achievementsUI;
    }

    public ragamuffin.ui.AchievementSystem getAchievementSystem() {
        return achievementSystem;
    }

    /** Issue #547: Returns the shopkeeper whose shop menu is currently open, or null. */
    public ragamuffin.entity.NPC getActiveShopkeeperNPC() {
        return activeShopkeeperNPC;
    }

    /** Issue #818: Returns the DisguiseSystem, or null if the game has not yet been initialised. */
    public DisguiseSystem getDisguiseSystem() {
        return disguiseSystem;
    }

    /**
     * Update sky colour based on time of day and season.
     * Uses seasonal sunrise/sunset from TimeSystem.
     */
    private void updateSkyColour(float time) {
        float sunrise = timeSystem.getSunriseTime();
        float sunset = timeSystem.getSunsetTime();

        // Dawn: from 0.5h before sunrise to 1h after
        float dawnStart = sunrise - 0.5f;
        float dawnMid = sunrise;
        float dawnEnd = sunrise + 1.0f;

        // Dusk: from 1h before sunset to 0.5h after
        float duskStart = sunset - 1.0f;
        float duskMid = sunset;
        float duskEnd = sunset + 0.5f;

        if (time >= dawnStart && time < dawnMid) {
            // Pre-dawn — dark blue to orange/pink
            float t = (time - dawnStart) / (dawnMid - dawnStart);
            skyR = lerp(0.05f, 0.85f, t);
            skyG = lerp(0.05f, 0.50f, t);
            skyB = lerp(0.15f, 0.45f, t);
        } else if (time >= dawnMid && time < dawnEnd) {
            // Sunrise — orange to daytime blue
            float t = (time - dawnMid) / (dawnEnd - dawnMid);
            skyR = lerp(0.85f, 0.53f, t);
            skyG = lerp(0.50f, 0.81f, t);
            skyB = lerp(0.45f, 0.92f, t);
        } else if (time >= dawnEnd && time < duskStart) {
            // Day — clear blue sky
            skyR = 0.53f;
            skyG = 0.81f;
            skyB = 0.92f;
        } else if (time >= duskStart && time < duskMid) {
            // Late afternoon — blue to golden
            float t = (time - duskStart) / (duskMid - duskStart);
            skyR = lerp(0.53f, 0.90f, t);
            skyG = lerp(0.81f, 0.55f, t);
            skyB = lerp(0.92f, 0.35f, t);
        } else if (time >= duskMid && time < duskEnd) {
            // Sunset — golden to deep blue
            float t = (time - duskMid) / (duskEnd - duskMid);
            skyR = lerp(0.90f, 0.10f, t);
            skyG = lerp(0.55f, 0.08f, t);
            skyB = lerp(0.35f, 0.25f, t);
        } else {
            // Night — dark blue/black
            skyR = 0.05f;
            skyG = 0.05f;
            skyB = 0.15f;
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Render a brief red flash ring around the reticule/crosshair when the player takes damage.
     * Intensity fades from 1.0 (full hit) to 0.0 over DAMAGE_FLASH_DURATION.
     * The effect is confined to the crosshair area rather than the entire screen.
     */
    private void renderDamageFlash(float intensity, int screenWidth, int screenHeight) {
        com.badlogic.gdx.math.Matrix4 flashProj = new com.badlogic.gdx.math.Matrix4();
        flashProj.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(flashProj);

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        // Draw a red ring around the crosshair using line segments
        float innerRadius = 18f; // just beyond the crosshair tips (size=10 + gap=3 + margin=5)
        float outerRadius = 30f;
        int segments = 32;
        float alpha = intensity * 0.85f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.9f, 0f, 0f, alpha);
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * Math.PI * 2 / segments);
            float angle2 = (float) ((i + 1) * Math.PI * 2 / segments);
            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);
            // Draw a filled quad for each ring segment
            shapeRenderer.triangle(
                centerX + cos1 * innerRadius, centerY + sin1 * innerRadius,
                centerX + cos1 * outerRadius, centerY + sin1 * outerRadius,
                centerX + cos2 * innerRadius, centerY + sin2 * innerRadius
            );
            shapeRenderer.triangle(
                centerX + cos1 * outerRadius, centerY + sin1 * outerRadius,
                centerX + cos2 * outerRadius, centerY + sin2 * outerRadius,
                centerX + cos2 * innerRadius, centerY + sin2 * innerRadius
            );
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    /**
     * Render death screen overlay with sardonic message.
     */
    private void renderDeathScreen() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Pick a death message if we haven't yet
        if (deathMessage == null) {
            deathMessage = DEATH_MESSAGES[(int)(Math.random() * DEATH_MESSAGES.length)];
        }

        // Dark red overlay
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        // Death message centred on screen
        spriteBatch.begin();
        font.setColor(1f, 0.2f, 0.2f, 1f);
        float textWidth = deathMessage.length() * 7f; // Approximate
        font.draw(spriteBatch, deathMessage, screenWidth / 2f - textWidth / 2f, screenHeight / 2f + 20);
        font.setColor(0.8f, 0.8f, 0.8f, 1f);
        font.draw(spriteBatch, "Respawning...", screenWidth / 2f - 40, screenHeight / 2f - 20);
        font.setColor(1f, 1f, 1f, 1f);
        spriteBatch.end();
    }

    /**
     * Render rain overlay effect.
     */
    private void renderRain(float delta) {
        rainTimer += delta;
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        com.badlogic.gdx.math.Matrix4 rainOrtho = new com.badlogic.gdx.math.Matrix4();
        rainOrtho.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(rainOrtho);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.7f, 0.7f, 0.85f, 0.4f);

        // Draw rain streaks
        int numDrops = 80;
        for (int i = 0; i < numDrops; i++) {
            float seed = i * 137.5f + rainTimer * 400f;
            float rx = ((seed * 1.3f) % screenWidth + screenWidth) % screenWidth;
            float ry = ((seed * 0.7f) % screenHeight + screenHeight) % screenHeight;
            shapeRenderer.line(rx, ry, rx - 2f, ry + 15f);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Issue #54: Render block targeting outline (Phase 3) and placement ghost block (Phase 4).
     * Uses ShapeRenderer in 3D world-space (camera.combined matrix) to draw directly in the scene.
     * Called after modelBatch.end() so it renders on top of the 3D world but before 2D UI.
     */
    private void renderBlockHighlight() {
        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        RaycastResult targetBlock = blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PUNCH_REACH);

        // Use the camera's combined (perspective) matrix so coordinates match the 3D world
        shapeRenderer.setProjectionMatrix(camera.combined);

        // --- Phase 3: Block targeting outline ---
        if (targetBlock != null) {
            int bx = targetBlock.getBlockX();
            int by = targetBlock.getBlockY();
            int bz = targetBlock.getBlockZ();

            // Disable depth test so outline shows through block edges
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0f, 0f, 0f, 0.9f);

            // Draw all 12 edges of the unit cube at block position
            float x0 = bx, y0 = by, z0 = bz;
            float x1 = bx + 1f, y1 = by + 1f, z1 = bz + 1f;
            // Bottom face
            shapeRenderer.line(x0, y0, z0,  x1, y0, z0);
            shapeRenderer.line(x1, y0, z0,  x1, y0, z1);
            shapeRenderer.line(x1, y0, z1,  x0, y0, z1);
            shapeRenderer.line(x0, y0, z1,  x0, y0, z0);
            // Top face
            shapeRenderer.line(x0, y1, z0,  x1, y1, z0);
            shapeRenderer.line(x1, y1, z0,  x1, y1, z1);
            shapeRenderer.line(x1, y1, z1,  x0, y1, z1);
            shapeRenderer.line(x0, y1, z1,  x0, y1, z0);
            // Vertical edges
            shapeRenderer.line(x0, y0, z0,  x0, y1, z0);
            shapeRenderer.line(x1, y0, z0,  x1, y1, z0);
            shapeRenderer.line(x1, y0, z1,  x1, y1, z1);
            shapeRenderer.line(x0, y0, z1,  x0, y1, z1);

            shapeRenderer.end();

            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // --- Issue #259: Crack/damage overlay on all partially-damaged blocks ---
        // Draw diagonal crack lines on all 6 faces of each damaged block.
        // ShapeRenderer only exposes 2D triangle variants, so we use Line mode to
        // render X-shaped cracks on each face — visually appropriate for a "crack"
        // effect and consistent with the wireframe outline already drawn above.
        // Opacity and line count increase with break progress across 4 stages.
        java.util.Set<String> damagedKeys = blockBreaker.getDamagedBlockKeys();
        if (!damagedKeys.isEmpty()) {
            // Determine equipped tool for progress calculation (same logic as HUD update)
            int crackSlot = hotbarUI.getSelectedSlot();
            Material crackEquipped = inventory.getItemInSlot(crackSlot);
            Material crackTool = (crackEquipped != null && ragamuffin.building.Tool.isTool(crackEquipped)) ? crackEquipped : null;

            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            for (String key : damagedKeys) {
                int[] coords = ragamuffin.building.BlockBreaker.parseBlockKey(key);
                int dx = coords[0], dy = coords[1], dz = coords[2];

                float progress = blockBreaker.getBreakProgress(world, dx, dy, dz, crackTool);
                if (progress <= 0f) continue;

                // Map progress to 4 stages: increasing opacity and crack density
                float alpha;
                int stage;
                if (progress < 0.25f) {
                    alpha = 0.35f; stage = 1;
                } else if (progress < 0.50f) {
                    alpha = 0.55f; stage = 2;
                } else if (progress < 0.75f) {
                    alpha = 0.70f; stage = 3;
                } else {
                    alpha = 0.85f; stage = 4;
                }
                shapeRenderer.setColor(0f, 0f, 0f, alpha);

                float cx0 = dx, cy0 = dy, cz0 = dz;
                float cx1 = dx + 1f, cy1 = dy + 1f, cz1 = dz + 1f;
                float cmx = dx + 0.5f, cmy = dy + 0.5f, cmz = dz + 0.5f;

                // Draw X cracks on each face (stage 1+: corners; stage 3+: mid-edge lines)
                // Top face (y = cy1): cracks in XZ plane
                shapeRenderer.line(cx0, cy1, cz0,  cx1, cy1, cz1);
                shapeRenderer.line(cx1, cy1, cz0,  cx0, cy1, cz1);
                if (stage >= 3) {
                    shapeRenderer.line(cmx, cy1, cz0,  cmx, cy1, cz1);
                    shapeRenderer.line(cx0, cy1, cmz,  cx1, cy1, cmz);
                }
                // Bottom face (y = cy0): cracks in XZ plane
                shapeRenderer.line(cx0, cy0, cz0,  cx1, cy0, cz1);
                shapeRenderer.line(cx1, cy0, cz0,  cx0, cy0, cz1);
                if (stage >= 3) {
                    shapeRenderer.line(cmx, cy0, cz0,  cmx, cy0, cz1);
                    shapeRenderer.line(cx0, cy0, cmz,  cx1, cy0, cmz);
                }
                // North face (z = cz0): cracks in XY plane
                shapeRenderer.line(cx0, cy0, cz0,  cx1, cy1, cz0);
                shapeRenderer.line(cx1, cy0, cz0,  cx0, cy1, cz0);
                if (stage >= 3) {
                    shapeRenderer.line(cmx, cy0, cz0,  cmx, cy1, cz0);
                    shapeRenderer.line(cx0, cmy, cz0,  cx1, cmy, cz0);
                }
                // South face (z = cz1): cracks in XY plane
                shapeRenderer.line(cx0, cy0, cz1,  cx1, cy1, cz1);
                shapeRenderer.line(cx1, cy0, cz1,  cx0, cy1, cz1);
                if (stage >= 3) {
                    shapeRenderer.line(cmx, cy0, cz1,  cmx, cy1, cz1);
                    shapeRenderer.line(cx0, cmy, cz1,  cx1, cmy, cz1);
                }
                // West face (x = cx0): cracks in YZ plane
                shapeRenderer.line(cx0, cy0, cz0,  cx0, cy1, cz1);
                shapeRenderer.line(cx0, cy1, cz0,  cx0, cy0, cz1);
                if (stage >= 3) {
                    shapeRenderer.line(cx0, cmy, cz0,  cx0, cmy, cz1);
                    shapeRenderer.line(cx0, cy0, cmz,  cx0, cy1, cmz);
                }
                // East face (x = cx1): cracks in YZ plane
                shapeRenderer.line(cx1, cy0, cz0,  cx1, cy1, cz1);
                shapeRenderer.line(cx1, cy1, cz0,  cx1, cy0, cz1);
                if (stage >= 3) {
                    shapeRenderer.line(cx1, cmy, cz0,  cx1, cmy, cz1);
                    shapeRenderer.line(cx1, cy0, cmz,  cx1, cy1, cmz);
                }
            }

            shapeRenderer.end();

            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // --- Phase 4: Block placement ghost block ---
        int selectedSlot = hotbarUI.getSelectedSlot();
        Material equippedMaterial = inventory.getItemInSlot(selectedSlot);
        if (equippedMaterial != null) {
            BlockType ghostBlockType = blockPlacer.materialToBlockType(equippedMaterial);
            if (ghostBlockType != null) {
                Vector3 placement = blockPlacer.getPlacementPosition(world, tmpCameraPos, tmpDirection, PLACE_REACH);
                if (placement != null) {
                    int px = (int) Math.floor(placement.x);
                    int py = (int) Math.floor(placement.y);
                    int pz = (int) Math.floor(placement.z);

                    com.badlogic.gdx.graphics.Color blockColor = ghostBlockType.getColor();

                    Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
                    Gdx.gl.glEnable(GL20.GL_BLEND);
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                    // Render ghost as a semi-transparent wireframe cube in the block's colour.
                    // ShapeRenderer has no 3D filled-triangle API, so we use Line mode which
                    // gives clear visual feedback of the placement preview.
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                    shapeRenderer.setColor(blockColor.r, blockColor.g, blockColor.b, 0.55f);

                    float gx0 = px, gy0 = py, gz0 = pz;
                    float gx1 = px + 1f, gy1 = py + 1f, gz1 = pz + 1f;
                    // Bottom face
                    shapeRenderer.line(gx0, gy0, gz0,  gx1, gy0, gz0);
                    shapeRenderer.line(gx1, gy0, gz0,  gx1, gy0, gz1);
                    shapeRenderer.line(gx1, gy0, gz1,  gx0, gy0, gz1);
                    shapeRenderer.line(gx0, gy0, gz1,  gx0, gy0, gz0);
                    // Top face
                    shapeRenderer.line(gx0, gy1, gz0,  gx1, gy1, gz0);
                    shapeRenderer.line(gx1, gy1, gz0,  gx1, gy1, gz1);
                    shapeRenderer.line(gx1, gy1, gz1,  gx0, gy1, gz1);
                    shapeRenderer.line(gx0, gy1, gz1,  gx0, gy1, gz0);
                    // Vertical edges
                    shapeRenderer.line(gx0, gy0, gz0,  gx0, gy1, gz0);
                    shapeRenderer.line(gx1, gy0, gz0,  gx1, gy1, gz0);
                    shapeRenderer.line(gx1, gy0, gz1,  gx1, gy1, gz1);
                    shapeRenderer.line(gx0, gy0, gz1,  gx0, gy1, gz1);

                    shapeRenderer.end();

                    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
                    Gdx.gl.glDisable(GL20.GL_BLEND);
                }
            }
        }
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        chunkRenderer.dispose();
        npcRenderer.dispose();
        if (propRenderer != null) {
            propRenderer.dispose();
        }
        if (smallItemRenderer != null) {
            smallItemRenderer.dispose();
        }
        if (carRenderer != null) {
            carRenderer.dispose();
        }
        spriteBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        if (soundSystem != null) {
            soundSystem.dispose();
        }
    }
}

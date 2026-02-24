package ragamuffin.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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

    // Phase 12: CRITIC 2 Improvements
    private WeatherSystem weatherSystem;

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

    // Issue #450: Achievement system
    private ragamuffin.ui.AchievementSystem achievementSystem;
    private ragamuffin.ui.AchievementsUI achievementsUI;

    // Issue #464: Quest log UI
    private ragamuffin.ui.QuestLogUI questLogUI;
    // Issue #497: Quest tracker UI (always-visible compact panel)
    private ragamuffin.ui.QuestTrackerUI questTrackerUI;
    private float distanceTravelledAchievement = 0f; // accumulated metres walked
    private com.badlogic.gdx.math.Vector3 lastPlayerPosForDistance = null;

    private static final float MOUSE_SENSITIVITY = 0.2f;
    private static final float PUNCH_REACH = 5.0f;
    private static final float PLACE_REACH = 5.0f;
    private static final float MAX_PITCH = 89.0f;
    private float cameraPitch = 0f;
    private float cameraYaw = 0f; // 0 = facing -Z

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
        blockBreaker = new BlockBreaker();
        dropTable = new BlockDropTable();
        tooltipSystem = new TooltipSystem();

        // Phase 4: Initialize crafting and building systems
        craftingSystem = new CraftingSystem();
        blockPlacer = new BlockPlacer(blockBreaker);

        // Phase 5: Initialize NPC system
        npcManager = new NPCManager();
        npcManager.setBlockBreaker(blockBreaker);
        spawnInitialNPCs();
        spawnBuildingNPCs(); // Issue #462: spawn static quest-giver NPCs inside buildings

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

        // Phase 11: Initialize CRITIC 1 systems
        interactionSystem = new InteractionSystem();
        healingSystem = new HealingSystem();
        respawnSystem = new RespawnSystem();
        respawnSystem.setSpawnY(calculateSpawnHeight(world, 0, 0) + 1.0f);

        // Phase 12: Initialize CRITIC 2 systems
        weatherSystem = new WeatherSystem();

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

        // Wire up tooltip sound effect
        tooltipSystem.setOnTooltipShow(() -> soundSystem.play(ragamuffin.audio.SoundEffect.TOOLTIP));

        // Issue #450: Initialize achievement system
        achievementSystem = new ragamuffin.ui.AchievementSystem();
        achievementsUI = new ragamuffin.ui.AchievementsUI(achievementSystem);
        achievementSystem.setOnNotificationShow(() -> soundSystem.play(ragamuffin.audio.SoundEffect.TOOLTIP));
        distanceTravelledAchievement = 0f;
        lastPlayerPosForDistance = new com.badlogic.gdx.math.Vector3(player.getPosition());

        // Issue #464: Initialize quest log UI
        questLogUI = new ragamuffin.ui.QuestLogUI(interactionSystem.getQuestRegistry());
        // Issue #497: Initialize quest tracker UI (compact always-visible panel)
        questTrackerUI = new ragamuffin.ui.QuestTrackerUI(interactionSystem.getQuestRegistry());

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
     * Spawn initial NPCs in the world.
     */
    private void spawnInitialNPCs() {
        // Park area — dog walkers with dogs, joggers
        spawnNPCAtTerrain(NPCType.PUBLIC, -5, 5);
        spawnNPCAtTerrain(NPCType.DOG, -2, 7);
        spawnNPCAtTerrain(NPCType.JOGGER, 5, -5);
        spawnNPCAtTerrain(NPCType.JOGGER, -8, -8);
        spawnNPCAtTerrain(NPCType.DOG, 10, 3);

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
    }

    /**
     * Spawn static quest-giver NPCs inside buildings that have a registered quest.
     * Each NPC is stationed at the centre of its building's ground floor (Issue #462).
     */
    private void spawnBuildingNPCs() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
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
            blockBreaker.tickDecay(delta);
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
                greggsRaidSystem.reset();
                player.getStreetReputation().reset();
                healingSystem.resetPosition(player.getPosition());
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

            // Phase 11: Check for death and respawn
            boolean justDied = respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
            // Fix #275: Clear sticky punch state on death so auto-punch doesn't fire on respawn
            if (justDied) {
                inputHandler.resetPunchHeld();
                punchHeldTimer = 0f;
                lastPunchTargetKey = null;
            }
            boolean wasRespawning = respawnSystem.isRespawning();
            respawnSystem.update(delta, player);
            if (wasRespawning && !respawnSystem.isRespawning()) {
                deathMessage = null; // Reset for next death
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
            {
                float ts = timeSystem.getTime();
                float sr = timeSystem.getSunriseTime();
                float ss = timeSystem.getSunsetTime();
                skyRenderer.renderSkybox(shapeRenderer, ts, sr, ss,
                                         Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                                         timeSystem.isNight(), cameraYaw);
            }
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

            modelBatch.begin(camera);
            chunkRenderer.render(modelBatch, environment);
            npcRenderer.render(modelBatch, environment, npcManager.getNPCs());
            modelBatch.end();

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
            {
                skyRenderer.update(delta);
                float ts = timeSystem.getTime();
                float sr = timeSystem.getSunriseTime();
                float ss = timeSystem.getSunsetTime();
                skyRenderer.renderSkybox(shapeRenderer, ts, sr, ss,
                                         Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                                         timeSystem.isNight(), cameraYaw);
            }
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

            modelBatch.begin(camera);
            chunkRenderer.render(modelBatch, environment);
            npcRenderer.render(modelBatch, environment, npcManager.getNPCs());
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
                    greggsRaidSystem.reset();
                    player.getStreetReputation().reset();
                    healingSystem.resetPosition(player.getPosition());
                    // Fix #459: Reset distance tracking on respawn so distance doesn't carry across deaths.
                    distanceTravelledAchievement = 0f;
                    lastPlayerPosForDistance.set(player.getPosition());
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

            // Fix #381: Advance healing resting timer while paused so the 5-second threshold
            // continues to accumulate and healing is not artificially delayed on resume.
            healingSystem.update(delta, player);

            // Fix #382: Advance gang territory linger timer while paused so the player cannot
            // exploit the pause menu to freeze the 5-second hostility escalation countdown.
            // Mirrors the pattern used for healing (#381), dodge (#379), reputation (#359),
            // weather (#341), and other timer-based systems in the PAUSED branch.
            gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, world);

            // Fix #391: Advance block damage decay timer while paused so partially-damaged
            // blocks continue decaying toward zero. Without this, the player can exploit
            // the pause menu to freeze block decay indefinitely and resume with blocks
            // still at the same damage level — bypassing the decay mechanic entirely.
            blockBreaker.tickDecay(delta);

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
            // Fix #481: If the quest log overlay is visible, forward UP/DOWN/ENTER
            // to the quest log instead of the pause menu so scroll input works correctly.
            if (questLogUI.isVisible()) {
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
                    } else if (pauseMenu.isRestartSelected()) {
                        restartGame();
                    } else if (pauseMenu.isQuitSelected()) {
                        Gdx.app.exit();
                    }
                    inputHandler.resetEnter();
                }
            }
            // Mouse click support for pause menu
            if (inputHandler.isLeftClickPressed()) {
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

        // Release cursor when any overlay UI is open, re-catch when all closed
        boolean uiOpen = inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible()
                || achievementsUI.isVisible() || questLogUI.isVisible();
        Gdx.input.setCursorCatched(!uiOpen);

        // Hotbar selection
        int hotbarSlot = inputHandler.getHotbarSlotPressed();
        if (hotbarSlot >= 0) {
            if (!craftingUI.isVisible()) {
                hotbarUI.selectSlot(hotbarSlot);
            }
            inputHandler.resetHotbarSlot();
        }

        // Phase 11: E key interaction — only when no UI overlay is open
        if (inputHandler.isInteractPressed()) {
            if (!isUIBlocking()) {
                handleInteraction();
            }
            inputHandler.resetInteract();
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
                || achievementsUI.isVisible() || questLogUI.isVisible();
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

        // Decay partially-damaged blocks that have not been hit recently
        blockBreaker.tickDecay(delta);

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

        // Phase 5: Update NPCs
        npcManager.update(delta, world, player, inventory, tooltipSystem);

        // Fix #196: update speech log after NPC speech is set for this frame
        speechLogUI.update(npcManager.getNPCs(), delta);

        // Issue #26: Update gang territory system
        gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, world);

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
        }

        // Issue #48: Passive reputation decay — "lying low" reduces reputation over time
        player.getStreetReputation().update(delta);

        // Issue #171: Update particle system
        particleSystem.update(delta);

        // Fix #387: Advance arm swing animation unconditionally so a mid-punch swing
        // completes rather than freezing in the extended position while a UI overlay
        // (inventory/help/crafting) is open. Mirrors the same fix already applied to
        // the PAUSED branch (Fix #339).
        firstPersonArm.update(delta);

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

        // Update camera to follow player
        camera.position.set(player.getPosition());
        camera.position.y += Player.EYE_HEIGHT;

        camera.update();
    }

    /**
     * Fix #198: Player-input update — gated behind !isUIBlocking().
     * Movement, jump, dodge, punch, and block placement are suppressed while a UI
     * overlay is open so the player cannot accidentally act while using menus.
     */
    private void updatePlayingInput(float delta) {
        // Handle punching — single click fires immediately; holding repeats every PUNCH_REPEAT_INTERVAL
        if (inputHandler.isPunchPressed()) {
            handlePunch();
            inputHandler.resetPunch();
            punchHeldTimer = 0f; // reset repeat timer on fresh click
            // Capture current target so we can detect target changes
            RaycastResult _initTarget = blockBreaker.getTargetBlock(world, camera.position, camera.direction, PUNCH_REACH);
            lastPunchTargetKey = (_initTarget != null) ? (_initTarget.getBlockX() + "," + _initTarget.getBlockY() + "," + _initTarget.getBlockZ()) : null;
        } else if (inputHandler.isPunchHeld()) {
            // Fix #279: Check for both block and NPC targets so hold-to-punch fires for NPCs too.
            RaycastResult heldTarget = blockBreaker.getTargetBlock(world, camera.position, camera.direction, PUNCH_REACH);
            String currentTargetKey = (heldTarget != null) ? (heldTarget.getBlockX() + "," + heldTarget.getBlockY() + "," + heldTarget.getBlockZ()) : null;
            boolean hasNPCTarget = findNPCInReach(camera.position, camera.direction, PUNCH_REACH) != null;
            // Reset timer only when the block target changes (switched block or block→NPC/none).
            // Do NOT reset if an NPC is the target and currentTargetKey is null — that would
            // zero the timer every frame and prevent repeat hits on NPCs (the bug in #279).
            if (!hasNPCTarget && (currentTargetKey == null || !currentTargetKey.equals(lastPunchTargetKey))) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            } else if (currentTargetKey != null && !currentTargetKey.equals(lastPunchTargetKey)) {
                // Block target changed (even while also facing an NPC — unlikely but correct)
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            }
            // Fix #285: When aiming at an NPC with no block target, clear any residual
            // block break progress immediately (not just on the next repeat tick).
            if (hasNPCTarget && currentTargetKey == null) {
                gameHUD.setBlockBreakProgress(0f);
            }
            // Any valid target — block OR NPC — should tick the repeat timer
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

        // Handle block placement
        if (inputHandler.isPlacePressed()) {
            handlePlace();
            inputHandler.resetPlace();
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

        // Jump
        if (inputHandler.isJumpPressed()) {
            if (world.isOnGround(player)) {
                player.jump();
            }
            inputHandler.resetJump();
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
            moveSpeed = inputHandler.isSprintHeld() ? Player.SPRINT_SPEED : Player.MOVE_SPEED;
        }
        world.moveWithCollision(player, tmpMoveDir.x, 0, tmpMoveDir.z, delta, moveSpeed);

        // Update footstep sounds based on movement
        boolean isMoving = tmpMoveDir.len2() > 0;
        BlockType blockUnderfoot = world.getBlockUnderPlayer(player);
        soundSystem.updateFootsteps(delta, isMoving, blockUnderfoot);

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
            // Issue #26: If a YOUTH_GANG member was punched, escalate territory to hostile
            if (targetNPC.getType() == ragamuffin.entity.NPCType.YOUTH_GANG) {
                gangTerritorySystem.onPlayerAttacksGang(tooltipSystem, npcManager, player, world);
                // Issue #450: gang aggro achievement
                achievementSystem.unlock(ragamuffin.ui.AchievementType.GANG_AGGRO);
            }
            return; // Don't punch blocks if we hit an NPC
        }

        // Raycast to find target block
        RaycastResult result = blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PUNCH_REACH);
        if (result != null) {
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
            // Not looking at any block - reset progress indicator
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
                soundSystem.play(ragamuffin.audio.SoundEffect.ITEM_EAT);
                // Show any feedback message from the consumable
                String consumeMsg = interactionSystem.getLastConsumeMessage();
                if (consumeMsg != null) {
                    tooltipSystem.showMessage(consumeMsg, 3.0f);
                }
                return;
            }
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
        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        // Find NPC in interaction range
        NPC targetNPC = interactionSystem.findNPCInRange(player.getPosition(), tmpDirection, npcManager.getNPCs());

        if (targetNPC != null) {
            // Interact with the NPC — pass inventory so quest NPCs can complete quests
            String dialogue = interactionSystem.interactWithNPC(targetNPC, inventory);
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

        // Check for door interaction via short raycast (≤3 blocks)
        ragamuffin.world.RaycastResult doorResult =
                blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, 3.0f);
        if (doorResult != null) {
            int x = doorResult.getBlockX();
            int y = doorResult.getBlockY();
            int z = doorResult.getBlockZ();
            ragamuffin.world.BlockType hitBlock = world.getBlock(x, y, z);
            if (hitBlock == ragamuffin.world.BlockType.DOOR_LOWER || hitBlock == ragamuffin.world.BlockType.DOOR_UPPER) {
                // Normalise to DOOR_LOWER position
                int lowerY = (hitBlock == ragamuffin.world.BlockType.DOOR_UPPER) ? y - 1 : y;
                world.toggleDoor(x, lowerY, z);
                rebuildChunkAt(x, lowerY, z);
                rebuildChunkAt(x, lowerY + 1, z);
                soundSystem.play(ragamuffin.audio.SoundEffect.BLOCK_PLACE);
            }
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
     * Find an NPC within punch reach.
     *
     * <p>Fix #242: Delegates to {@link NPCHitDetector#findNPCInReach} which uses a
     * widened hit cone (~26° instead of the old ~10°) for more forgiving aim.
     */
    private NPC findNPCInReach(Vector3 cameraPos, Vector3 direction, float reach) {
        return NPCHitDetector.findNPCInReach(cameraPos, direction, reach,
                npcManager.getNPCs(), blockBreaker, world);
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

        // Render first-person arm (always visible in gameplay)
        if (!openingSequence.isActive()) {
            firstPersonArm.render(shapeRenderer, screenWidth, screenHeight);
        }

        // Phase 8: Render GameHUD (health/hunger/energy bars + crosshair)
        if (!openingSequence.isActive()) {
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
                // Update block break progress on crosshair (account for equipped tool)
                tmpCameraPos.set(camera.position);
                tmpDirection.set(camera.direction);
                int hudSelectedSlot = hotbarUI.getSelectedSlot();
                Material hudEquipped = inventory.getItemInSlot(hudSelectedSlot);
                Material hudTool = (hudEquipped != null && Tool.isTool(hudEquipped)) ? hudEquipped : null;
                RaycastResult targetBlock = blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PUNCH_REACH);

                // Issue #287: Check NPC target first — NPC takes priority over block for both
                // block break progress and target name (avoids two separate findNPCInReach calls)
                NPC hudTargetNPC = findNPCInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);
                if (hudTargetNPC != null) {
                    gameHUD.setBlockBreakProgress(0f); // NPC target — don't show block damage
                } else if (targetBlock != null) {
                    float progress = blockBreaker.getBreakProgress(world, targetBlock.getBlockX(),
                        targetBlock.getBlockY(), targetBlock.getBlockZ(), hudTool);
                    gameHUD.setBlockBreakProgress(progress);
                } else {
                    gameHUD.setBlockBreakProgress(0f);
                }

                // Issue #189: Update target reticule label — NPC takes priority over block
                if (hudTargetNPC != null) {
                    gameHUD.setTargetName(formatNPCName(hudTargetNPC.getType()));
                } else if (targetBlock != null) {
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

        // Always render hotbar (unless opening sequence active)
        if (!openingSequence.isActive()) {
            hotbarUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltipSystem);
        }

        // Render clock
        clockHUD.render(spriteBatch, font, screenWidth, screenHeight);

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

        // Issue #464: Render quest log overlay if visible
        if (questLogUI.isVisible()) {
            questLogUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
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
        if (!openingSequence.isActive()) {
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
            float textWidth = text.length() * 7f; // Approximate
            float bubbleW = textWidth + 16;
            float bubbleH = 28;
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

            // Draw text
            spriteBatch.begin();
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(spriteBatch, text, bubbleX + 8, bubbleY + bubbleH - 6);
            spriteBatch.end();
        }
    }

    private void handleEscapePress() {
        // Close any open UI first
        if (achievementsUI.isVisible()) {
            achievementsUI.hide();
            Gdx.input.setCursorCatched(true);
        } else if (inventoryUI.isVisible()) {
            inventoryUI.hide();
            Gdx.input.setCursorCatched(true);
        } else if (helpUI.isVisible()) {
            helpUI.hide();
            Gdx.input.setCursorCatched(true);
        } else if (craftingUI.isVisible()) {
            craftingUI.hide();
            Gdx.input.setCursorCatched(true);
        } else if (questLogUI.isVisible()) {
            questLogUI.hide();
            Gdx.input.setCursorCatched(true);
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
        interactionSystem = new InteractionSystem();
        // Fix #479: Recreate questLogUI bound to the fresh registry so old quests don't bleed in
        questLogUI = new ragamuffin.ui.QuestLogUI(interactionSystem.getQuestRegistry());
        // Issue #497: Recreate questTrackerUI bound to the fresh registry
        questTrackerUI = new ragamuffin.ui.QuestTrackerUI(interactionSystem.getQuestRegistry());
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
        exposureSystem = new ExposureSystem();
        arrestSystem = new ArrestSystem();
        arrestSystem.setRespawnY(calculateSpawnHeight(world, 0, 0) + 1.0f);
        greggsRaidSystem = new GreggsRaidSystem();
        gangTerritorySystem.reset();
        gameHUD = new GameHUD(player);
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
        inputHandler.resetLeftClickReleased();
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
        spriteBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        if (soundSystem != null) {
            soundSystem.dispose();
        }
    }
}

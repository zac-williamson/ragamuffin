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
import ragamuffin.ai.NPCManager;
import ragamuffin.building.*;
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
    private PauseMenu pauseMenu;
    private MainMenuScreen mainMenuScreen;
    private OpeningSequence openingSequence;

    // Phase 11: CRITIC 1 Improvements
    private InteractionSystem interactionSystem;
    private HealingSystem healingSystem;
    private RespawnSystem respawnSystem;

    // Phase 12: CRITIC 2 Improvements
    private WeatherSystem weatherSystem;

    // CRITIC 5: Arrest system — applies penalties when police catch the player
    private ArrestSystem arrestSystem;

    // CRITIC 3: Greggs Raid mechanic — dedicated system for raid state management
    private GreggsRaidSystem greggsRaidSystem;

    // Hover tooltip system
    private HoverTooltipSystem hoverTooltipSystem;

    // Issue #13: on-screen NPC speech log
    private SpeechLogUI speechLogUI;

    // NPC rendering
    private NPCRenderer npcRenderer;

    // Issue #10: Building signage renderer
    private ragamuffin.render.SignageRenderer signageRenderer;

    // First-person arm
    private FirstPersonArm firstPersonArm;

    // Audio system
    private ragamuffin.audio.SoundSystem soundSystem;

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
        blockPlacer = new BlockPlacer();

        // Phase 5: Initialize NPC system
        npcManager = new NPCManager();
        spawnInitialNPCs();

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

        // Phase 11: Initialize CRITIC 1 systems
        interactionSystem = new InteractionSystem();
        healingSystem = new HealingSystem();
        respawnSystem = new RespawnSystem();

        // Phase 12: Initialize CRITIC 2 systems
        weatherSystem = new WeatherSystem();

        // CRITIC 5: Initialize arrest system
        arrestSystem = new ArrestSystem();

        // CRITIC 3: Initialize Greggs raid system
        greggsRaidSystem = new GreggsRaidSystem();

        // Initialize hover tooltip system
        hoverTooltipSystem = new HoverTooltipSystem();

        // Issue #13: Initialize NPC speech log
        speechLogUI = new SpeechLogUI();

        // Initialize audio system
        soundSystem = new ragamuffin.audio.SoundSystem();

        // Wire up tooltip sound effect
        tooltipSystem.setOnTooltipShow(() -> soundSystem.play(ragamuffin.audio.SoundEffect.TOOLTIP));

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

        // Police patrol — always present on the streets
        NPC police1 = spawnNPCAtTerrain(NPCType.POLICE, 20, 10);
        if (police1 != null) police1.setState(NPCState.PATROLLING);
        NPC police2 = spawnNPCAtTerrain(NPCType.POLICE, -30, 20);
        if (police2 != null) police2.setState(NPCState.PATROLLING);
        NPC police3 = spawnNPCAtTerrain(NPCType.POLICE, 50, -10);
        if (police3 != null) police3.setState(NPCState.PATROLLING);

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

            // Handle UI toggles
            handleUIInput();

            // Handle state transitions
            if (inputHandler.isEscapePressed()) {
                handleEscapePress();
                inputHandler.resetEscape();
            }

            // Update game logic if not blocked by UI or opening sequence
            if (!isUIBlocking() && !openingSequence.isActive()) {
                updatePlaying(delta);
            }

            // Update time system (only when not paused in opening sequence)
            if (!openingSequence.isActive()) {
                // Convert delta (real seconds) to game time seconds
                // Assuming 1 real second = 1 game second for now
                float gameTimeDelta = delta;

                timeSystem.update(delta);
                lightingSystem.updateLighting(timeSystem.getTime(), timeSystem.getSunriseTime(), timeSystem.getSunsetTime());
                updateSkyColour(timeSystem.getTime());
                clockHUD.update(timeSystem.getTime(), timeSystem.getDayCount(), timeSystem.getDayOfMonth(), timeSystem.getMonthName());

                // Phase 12: Update weather system
                weatherSystem.update(gameTimeDelta);

                // Update police spawning based on time
                npcManager.updatePoliceSpawning(timeSystem.getTime(), world, player);

                // Update player survival stats
                // Sprint drains hunger 3x faster
                float hungerMultiplier = inputHandler.isSprintHeld() ? 3.0f : 1.0f;
                player.updateHunger(delta * hungerMultiplier);

                // Starvation: zero hunger drains health at 5 HP/s
                if (player.getHunger() <= 0) {
                    player.damage(5.0f * delta);
                }

                if (!isUIBlocking()) {
                    // Phase 12: Apply weather energy drain multiplier
                    float weatherMultiplier = weatherSystem.getCurrentWeather().getEnergyDrainMultiplier();
                    // Weather affects recovery rate inversely - worse weather = slower recovery
                    player.recoverEnergy(delta / weatherMultiplier);
                }

                // Phase 12: Cold snap health drain at night when unsheltered
                Weather currentWeather = weatherSystem.getCurrentWeather();
                if (currentWeather.drainsHealthAtNight() && timeSystem.isNight()) {
                    boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
                    if (!sheltered) {
                        float healthDrain = currentWeather.getHealthDrainRate() * delta;
                        player.damage(healthDrain);
                    }
                }

                // Phase 11: Update healing system
                healingSystem.update(delta, player);

                // Phase 11: Check for death and respawn
                respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
                boolean wasRespawning = respawnSystem.isRespawning();
                respawnSystem.update(delta, player);
                if (wasRespawning && !respawnSystem.isRespawning()) {
                    deathMessage = null; // Reset for next death
                }

                // Phase 11: Trigger hunger warning tooltip
                if (player.getHunger() <= 25 && !tooltipSystem.hasShown(TooltipTrigger.HUNGER_LOW)) {
                    tooltipSystem.trigger(TooltipTrigger.HUNGER_LOW);
                }
            }

            // Update tooltip system
            tooltipSystem.update(delta);

            // Render 3D world
            Gdx.gl.glClearColor(skyR, skyG, skyB, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            modelBatch.begin(camera);
            chunkRenderer.render(modelBatch, environment);
            npcRenderer.render(modelBatch, environment, npcManager.getNPCs());
            modelBatch.end();

            // Render NPC speech bubbles (2D overlay projected from 3D)
            renderSpeechBubbles();

            // Issue #10: Render building signs as world-projected overlays
            signageRenderer.render(camera, spriteBatch, shapeRenderer, font,
                                   Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Render rain overlay if raining
            if (weatherSystem.getCurrentWeather() == Weather.RAIN) {
                renderRain(delta);
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
            Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            modelBatch.begin(camera);
            chunkRenderer.render(modelBatch, environment);
            npcRenderer.render(modelBatch, environment, npcManager.getNPCs());
            modelBatch.end();

            // Render UI and pause menu
            renderUI();
            pauseMenu.render(spriteBatch, shapeRenderer, font,
                           Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Handle pause menu input
            if (inputHandler.isEscapePressed()) {
                handleEscapePress();
                inputHandler.resetEscape();
            }
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
            // Mouse click support for pause menu
            if (inputHandler.isLeftClickPressed()) {
                int sw = Gdx.graphics.getWidth();
                int sh = Gdx.graphics.getHeight();
                int clicked = pauseMenu.handleClick(inputHandler.getMouseX(), inputHandler.getMouseY(), sw, sh);
                if (clicked == 0) {
                    transitionToPlaying();
                } else if (clicked == 1) {
                    restartGame();
                } else if (clicked == 2) {
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
                Gdx.app.exit();
            }
            inputHandler.resetLeftClick();
        }
    }

    private void startNewGame() {
        // Transition to playing and start opening sequence
        state = GameState.PLAYING;
        openingSequence.start();
        Gdx.input.setCursorCatched(true);
        // Reset per-session counters
        if (greggsRaidSystem != null) {
            greggsRaidSystem.reset();
        }
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
        }

        // Help toggle
        if (inputHandler.isHelpPressed()) {
            boolean wasVisible = helpUI.isVisible();
            helpUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetHelp();
        }

        // Crafting toggle
        if (inputHandler.isCraftingPressed()) {
            boolean wasVisible = craftingUI.isVisible();
            craftingUI.toggle();
            soundSystem.play(wasVisible ? ragamuffin.audio.SoundEffect.UI_CLOSE : ragamuffin.audio.SoundEffect.UI_OPEN);
            inputHandler.resetCrafting();
        }

        // Release cursor when any overlay UI is open, re-catch when all closed
        boolean uiOpen = inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible();
        Gdx.input.setCursorCatched(!uiOpen);

        // Hotbar selection
        int hotbarSlot = inputHandler.getHotbarSlotPressed();
        if (hotbarSlot >= 0) {
            if (!craftingUI.isVisible()) {
                hotbarUI.selectSlot(hotbarSlot);
            }
            inputHandler.resetHotbarSlot();
        }

        // Phase 11: E key interaction
        if (inputHandler.isInteractPressed()) {
            handleInteraction();
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
        }
    }

    private boolean isUIBlocking() {
        return inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible();
    }

    private void updatePlaying(float delta) {
        // Update first-person arm animation
        firstPersonArm.update(delta);

        // Handle punching
        if (inputHandler.isPunchPressed()) {
            handlePunch();
            inputHandler.resetPunch();
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

        // Update dodge timers and damage flash
        player.updateDodge(delta);
        player.updateFlash(delta);

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

        // Push player out of any NPC they're overlapping
        resolveNPCCollisions();

        // Update loaded chunks based on player position
        world.updateLoadedChunks(player.getPosition());

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
        }

        // Update camera to follow player
        camera.position.set(player.getPosition());
        camera.position.y += Player.EYE_HEIGHT;

        camera.update();
    }

    private void handlePunch() {
        // Trigger arm swing animation
        firstPersonArm.punch();

        // Consume energy for punching
        player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);

        // Determine equipped tool from hotbar
        int selectedSlot = hotbarUI.getSelectedSlot();
        Material equippedMaterial = inventory.getItemInSlot(selectedSlot);
        Material toolMaterial = (equippedMaterial != null && Tool.isTool(equippedMaterial)) ? equippedMaterial : null;

        // Rest of the punching logic
        // Check if punching an NPC first (reuse vectors)
        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        // Check for nearby NPCs in punch range
        NPC targetNPC = findNPCInReach(tmpCameraPos, tmpDirection, PUNCH_REACH);
        if (targetNPC != null) {
            // Punch the NPC (knockback + loot on kill)
            npcManager.punchNPC(targetNPC, tmpDirection, inventory, tooltipSystem);
            soundSystem.play(ragamuffin.audio.SoundEffect.NPC_HIT);
            // Clear block break progress when punching NPCs
            gameHUD.setBlockBreakProgress(0f);
            // Award street reputation for fighting (major crime)
            player.getStreetReputation().addPoints(2);
            return; // Don't punch blocks if we hit an NPC
        }

        // Raycast to find target block
        RaycastResult result = blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PUNCH_REACH);
        if (result != null) {
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

            // Update HUD with break progress after the punch
            if (!broken) {
                float progress = blockBreaker.getBreakProgress(world, x, y, z, toolMaterial);
                gameHUD.setBlockBreakProgress(progress);
            } else {
                // Block was broken - reset progress
                gameHUD.setBlockBreakProgress(0f);
                // Play block break sound based on material
                soundSystem.playBlockBreak(blockType);
                // Award street reputation for breaking blocks (minor crime)
                player.getStreetReputation().addPoints(1);
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
                }
            }

            if (broken) {
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
                }

                // Only rebuild the affected chunk (and neighbours if on a boundary)
                rebuildChunkAt(x, y, z);
            }
        } else {
            // Not looking at any block - reset progress indicator
            gameHUD.setBlockBreakProgress(0f);
        }
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
                // Food was eaten successfully
                return;
            }
        }

        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        // Get placement position before placing so we know which chunk to rebuild
        Vector3 placementPos = blockPlacer.getPlacementPosition(world, tmpCameraPos, tmpDirection, PLACE_REACH);

        boolean placed = blockPlacer.placeBlock(world, inventory, material, tmpCameraPos, tmpDirection, PLACE_REACH, player.getAABB());

        if (placed) {
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

            // Only rebuild the affected chunk
            if (placementPos != null) {
                rebuildChunkAt((int) placementPos.x, (int) placementPos.y, (int) placementPos.z);
                // Cardboard box builds a 2x2x2 structure — rebuild adjacent chunks too
                if (material == Material.CARDBOARD_BOX) {
                    rebuildChunkAt((int) placementPos.x + 2, (int) placementPos.y, (int) placementPos.z);
                    rebuildChunkAt((int) placementPos.x, (int) placementPos.y, (int) placementPos.z + 2);
                    rebuildChunkAt((int) placementPos.x, (int) placementPos.y + 3, (int) placementPos.z);
                }
            }
        }
    }

    /**
     * Phase 11: Handle E key interaction with NPCs.
     */
    private void handleInteraction() {
        tmpCameraPos.set(camera.position);
        tmpDirection.set(camera.direction);

        // Find NPC in interaction range
        NPC targetNPC = interactionSystem.findNPCInRange(player.getPosition(), tmpDirection, npcManager.getNPCs());

        if (targetNPC != null) {
            // Interact with the NPC
            String dialogue = interactionSystem.interactWithNPC(targetNPC);
            // The dialogue is set on the NPC, which will be rendered as a speech bubble
        }
    }

    /**
     * Resolve player-NPC collisions by pushing the player out.
     */
    private void resolveNPCCollisions() {
        for (NPC npc : npcManager.getNPCs()) {
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
     * Find an NPC within punch reach.
     */
    private NPC findNPCInReach(Vector3 cameraPos, Vector3 direction, float reach) {
        NPC closestNPC = null;
        float closestDistance = reach;

        // First, find the nearest solid block along the ray — can't punch NPCs behind walls
        RaycastResult blockHit = blockBreaker.getTargetBlock(world, cameraPos, direction, reach);
        float blockDistance = (blockHit != null) ? cameraPos.dst(blockHit.getBlockX() + 0.5f,
            blockHit.getBlockY() + 0.5f, blockHit.getBlockZ() + 0.5f) : reach;

        for (NPC npc : npcManager.getNPCs()) {
            // Dead NPCs cannot be punched
            if (!npc.isAlive()) continue;

            // Calculate distance to NPC centre (at chest height)
            float npcCentreY = npc.getPosition().y + NPC.HEIGHT * 0.5f;
            float dx = npc.getPosition().x - cameraPos.x;
            float dy = npcCentreY - cameraPos.y;
            float dz = npc.getPosition().z - cameraPos.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance > reach || distance > blockDistance) {
                continue; // Too far or behind a block
            }

            // Check if NPC is in front of camera (tight cone — ~10 degrees)
            float invDist = 1f / distance;
            float dot = (dx * invDist) * direction.x + (dy * invDist) * direction.y + (dz * invDist) * direction.z;
            if (dot > 0.985f && distance < closestDistance) {
                closestNPC = npc;
                closestDistance = distance;
            }
        }

        return closestNPC;
    }

    private void renderUI() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

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

            // Update block break progress on crosshair (account for equipped tool)
            tmpCameraPos.set(camera.position);
            tmpDirection.set(camera.direction);
            int hudSelectedSlot = hotbarUI.getSelectedSlot();
            Material hudEquipped = inventory.getItemInSlot(hudSelectedSlot);
            Material hudTool = (hudEquipped != null && Tool.isTool(hudEquipped)) ? hudEquipped : null;
            RaycastResult targetBlock = blockBreaker.getTargetBlock(world, tmpCameraPos, tmpDirection, PUNCH_REACH);
            if (targetBlock != null) {
                float progress = blockBreaker.getBreakProgress(world, targetBlock.getBlockX(),
                    targetBlock.getBlockY(), targetBlock.getBlockZ(), hudTool);
                gameHUD.setBlockBreakProgress(progress);
            } else {
                gameHUD.setBlockBreakProgress(0f);
            }

            gameHUD.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight, hoverTooltipSystem);
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

        // Render damage flash overlay
        float flashIntensity = player.getDamageFlashIntensity();
        if (flashIntensity > 0f) {
            renderDamageFlash(flashIntensity, screenWidth, screenHeight);
        }

        // Issue #13: render NPC speech log (bottom-right corner)
        if (!openingSequence.isActive()) {
            speechLogUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Render tooltip if active
        if (tooltipSystem.isActive()) {
            renderTooltip();
        }

        // Update and render hover tooltips last (on top of everything)
        hoverTooltipSystem.update(delta);
        hoverTooltipSystem.render(spriteBatch, shapeRenderer, font);
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

    private void renderSpeechBubbles() {
        float delta = Gdx.graphics.getDeltaTime();
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Issue #13: update speech log with current NPC speech
        speechLogUI.update(npcManager.getNPCs(), delta);

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
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
            shapeRenderer.rect(bubbleX, bubbleY, bubbleW, bubbleH);
            shapeRenderer.end();

            // Draw text
            spriteBatch.begin();
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(spriteBatch, text, bubbleX + 8, bubbleY + bubbleH - 6);
            spriteBatch.end();
        }
    }

    private void handleEscapePress() {
        // Close any open UI first
        if (inventoryUI.isVisible()) {
            inventoryUI.hide();
            Gdx.input.setCursorCatched(true);
        } else if (helpUI.isVisible()) {
            helpUI.hide();
            Gdx.input.setCursorCatched(true);
        } else if (craftingUI.isVisible()) {
            craftingUI.hide();
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
        craftingUI = new CraftingUI(craftingSystem, inventory);

        // Reset NPCs
        npcManager = new NPCManager();
        spawnInitialNPCs();

        // Reset time and lighting
        timeSystem = new TimeSystem(8.0f);
        lightingSystem = new LightingSystem(environment);
        clockHUD = new ClockHUD();

        // Reset game systems
        blockBreaker = new BlockBreaker();
        tooltipSystem = new TooltipSystem();
        interactionSystem = new InteractionSystem();
        healingSystem = new HealingSystem();
        respawnSystem = new RespawnSystem();
        weatherSystem = new WeatherSystem();
        arrestSystem = new ArrestSystem();
        greggsRaidSystem = new GreggsRaidSystem();
        gameHUD = new GameHUD(player);
        openingSequence = new OpeningSequence();
        speechLogUI = new SpeechLogUI();
        deathMessage = null;

        // Transition to playing with opening sequence
        state = GameState.PLAYING;
        openingSequence.start();
        Gdx.input.setCursorCatched(true);
    }

    private void transitionToPlaying() {
        state = GameState.PLAYING;
        Gdx.input.setCursorCatched(true);
    }

    private void transitionToPaused() {
        state = GameState.PAUSED;
        Gdx.input.setCursorCatched(false);
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
     * Render a brief red flash overlay when the player takes damage.
     * Intensity fades from 1.0 (full hit) to 0.0 over DAMAGE_FLASH_DURATION.
     */
    private void renderDamageFlash(float intensity, int screenWidth, int screenHeight) {
        com.badlogic.gdx.math.Matrix4 flashProj = new com.badlogic.gdx.math.Matrix4();
        flashProj.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(flashProj);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.9f, 0f, 0f, intensity * 0.45f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
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
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

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

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
import ragamuffin.ai.NPCManager;
import ragamuffin.building.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.ChunkRenderer;
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

    private static final float MOUSE_SENSITIVITY = 0.15f;
    private static final float PUNCH_REACH = 5.0f;
    private static final float PLACE_REACH = 5.0f;

    @Override
    public void create() {
        Gdx.app.log("Ragamuffin", "Welcome to the real world, kid.");

        // Phase 8: Start in MENU state with main menu
        state = GameState.MENU;

        // Setup 3D camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        // Setup rendering
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Setup 2D UI rendering
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont(); // Default LibGDX font
        font.getData().setScale(1.2f);

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
        chunkRenderer = new ChunkRenderer();

        // Load initial chunks around player
        world.updateLoadedChunks(player.getPosition());
        updateChunkRenderers();

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

        // Setup input
        inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(inputHandler);
        // Don't catch cursor in menu
        Gdx.input.setCursorCatched(false);
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
     * Spawn initial NPCs in the world.
     */
    private void spawnInitialNPCs() {
        // Spawn some members of the public
        npcManager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);
        npcManager.spawnNPC(NPCType.PUBLIC, -15, 1, 8);
        npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, -12);

        // Spawn dogs in the park
        npcManager.spawnNPC(NPCType.DOG, -5, 1, -5);
        npcManager.spawnNPC(NPCType.DOG, 8, 1, 3);

        // Spawn a youth gang
        npcManager.spawnNPC(NPCType.YOUTH_GANG, -10, 1, -10);

        // Spawn a council member
        npcManager.spawnNPC(NPCType.COUNCIL_MEMBER, 20, 1, 20);
    }

    /**
     * Update chunk renderers for all loaded chunks.
     */
    private void updateChunkRenderers() {
        for (Chunk chunk : world.getLoadedChunks()) {
            chunkRenderer.updateChunk(chunk, meshBuilder);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Update input
        inputHandler.update();

        // Handle state-specific input and updates
        if (state == GameState.MENU) {
            handleMenuInput();
            renderMenu();
        } else if (state == GameState.PLAYING) {
            // Update opening sequence if active
            if (openingSequence.isActive()) {
                openingSequence.update(delta);
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
                lightingSystem.updateLighting(timeSystem.getTime());
                clockHUD.update(timeSystem.getTime());

                // Phase 12: Update weather system
                weatherSystem.update(gameTimeDelta);

                // Update police spawning based on time
                npcManager.updatePoliceSpawning(timeSystem.getTime(), world, player);

                // Update player survival stats
                player.updateHunger(delta);
                if (!isUIBlocking()) {
                    // Phase 12: Apply weather energy drain multiplier
                    float energyRecovery = Player.ENERGY_RECOVERY_PER_SECOND * delta;
                    float weatherMultiplier = weatherSystem.getCurrentWeather().getEnergyDrainMultiplier();
                    // Weather affects recovery rate inversely - worse weather = slower recovery
                    player.recoverEnergy(energyRecovery / weatherMultiplier);
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
                respawnSystem.update(delta, player);

                // Phase 11: Trigger hunger warning tooltip
                if (player.getHunger() <= 25 && !tooltipSystem.hasShown(TooltipTrigger.HUNGER_LOW)) {
                    tooltipSystem.trigger(TooltipTrigger.HUNGER_LOW);
                }
            }

            // Update tooltip system
            tooltipSystem.update(delta);

            // Render 3D world
            Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1f); // Sky blue
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            modelBatch.begin(camera);
            chunkRenderer.render(modelBatch, environment);
            modelBatch.end();

            // Render 2D UI overlay
            renderUI();

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
        }
    }

    private void handleMenuInput() {
        // Handle main menu input
        if (inputHandler.isEnterPressed()) {
            if (mainMenuScreen.isNewGameSelected()) {
                startNewGame();
            } else if (mainMenuScreen.isQuitSelected()) {
                Gdx.app.exit();
            }
            inputHandler.resetEnter();
        }
    }

    private void startNewGame() {
        // Transition to playing and start opening sequence
        state = GameState.PLAYING;
        openingSequence.start();
        Gdx.input.setCursorCatched(true);
    }

    private void renderMenu() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        mainMenuScreen.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
    }

    private void handleUIInput() {
        // Inventory toggle
        if (inputHandler.isInventoryPressed()) {
            inventoryUI.toggle();
            inputHandler.resetInventory();
        }

        // Help toggle
        if (inputHandler.isHelpPressed()) {
            helpUI.toggle();
            inputHandler.resetHelp();
        }

        // Crafting toggle
        if (inputHandler.isCraftingPressed()) {
            craftingUI.toggle();
            inputHandler.resetCrafting();
        }

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

        // Calculate movement direction
        Vector3 forward = new Vector3(camera.direction.x, 0, camera.direction.z).nor();
        Vector3 right = new Vector3(camera.direction).crs(Vector3.Y).nor();
        Vector3 moveDir = new Vector3();

        if (inputHandler.isForward()) {
            moveDir.add(forward);
        }
        if (inputHandler.isBackward()) {
            moveDir.sub(forward);
        }
        if (inputHandler.isRight()) {
            moveDir.add(right);
        }
        if (inputHandler.isLeft()) {
            moveDir.sub(right);
        }

        // Move player with collision
        if (moveDir.len2() > 0) {
            moveDir.nor();
            world.moveWithCollision(player, moveDir.x, 0, moveDir.z, delta);
        }

        // Update loaded chunks based on player position
        world.updateLoadedChunks(player.getPosition());

        // Phase 5: Update NPCs
        npcManager.update(delta, world, player, inventory, tooltipSystem);

        // Update camera to follow player
        camera.position.set(player.getPosition());
        camera.position.y += Player.EYE_HEIGHT;

        // Mouse look
        float mouseDX = inputHandler.getMouseDeltaX();
        float mouseDY = inputHandler.getMouseDeltaY();

        if (mouseDX != 0 || mouseDY != 0) {
            // Rotate camera based on mouse
            camera.rotate(Vector3.Y, -mouseDX * MOUSE_SENSITIVITY);

            // Pitch (up/down) - clamp to prevent flipping
            Vector3 rightAxis = new Vector3(camera.direction).crs(Vector3.Y).nor();
            camera.rotate(rightAxis, -mouseDY * MOUSE_SENSITIVITY);
        }

        camera.update();
    }

    private void handlePunch() {
        // Consume energy for punching
        player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);

        // Rest of the punching logic
        // Check if punching an NPC first
        Vector3 cameraPos = new Vector3(camera.position);
        Vector3 direction = new Vector3(camera.direction);

        // Check for nearby NPCs in punch range
        NPC targetNPC = findNPCInReach(cameraPos, direction, PUNCH_REACH);
        if (targetNPC != null) {
            // Punch the NPC (knockback)
            npcManager.punchNPC(targetNPC, direction);
            return; // Don't punch blocks if we hit an NPC
        }

        // Raycast to find target block
        RaycastResult result = blockBreaker.getTargetBlock(world, cameraPos, direction, PUNCH_REACH);
        if (result != null) {
            int x = result.getBlockX();
            int y = result.getBlockY();
            int z = result.getBlockZ();
            BlockType blockType = result.getBlockType();

            // Check if it's a tree - trigger tooltip on first punch
            if (blockType == BlockType.TREE_TRUNK && blockBreaker.getHitCount(x, y, z) == 0) {
                tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
            }

            // Punch the block
            boolean broken = blockBreaker.punchBlock(world, x, y, z);

            if (broken) {
                // Block was broken - determine drop
                LandmarkType landmark = world.getLandmarkAt(x, y, z);
                Material drop = dropTable.getDrop(blockType, landmark);

                if (drop != null) {
                    inventory.addItem(drop, 1);

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

                // Update chunk mesh
                updateChunkRenderers();
            }
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

        Vector3 cameraPos = new Vector3(camera.position);
        Vector3 direction = new Vector3(camera.direction);

        boolean placed = blockPlacer.placeBlock(world, inventory, material, cameraPos, direction, PLACE_REACH);

        if (placed) {
            // Phase 11: Trigger first block place tooltip
            if (!tooltipSystem.hasShown(TooltipTrigger.FIRST_BLOCK_PLACE)) {
                tooltipSystem.trigger(TooltipTrigger.FIRST_BLOCK_PLACE);
            }

            // Update chunk mesh
            updateChunkRenderers();
        }
    }

    /**
     * Phase 11: Handle E key interaction with NPCs.
     */
    private void handleInteraction() {
        Vector3 cameraPos = new Vector3(camera.position);
        Vector3 direction = new Vector3(camera.direction);

        // Find NPC in interaction range
        NPC targetNPC = interactionSystem.findNPCInRange(player.getPosition(), direction, npcManager.getNPCs());

        if (targetNPC != null) {
            // Interact with the NPC
            String dialogue = interactionSystem.interactWithNPC(targetNPC);
            // The dialogue is set on the NPC, which will be rendered as a speech bubble
        }
    }

    /**
     * Find an NPC within punch reach.
     */
    private NPC findNPCInReach(Vector3 cameraPos, Vector3 direction, float reach) {
        NPC closestNPC = null;
        float closestDistance = reach;

        for (NPC npc : npcManager.getNPCs()) {
            // Calculate distance to NPC
            Vector3 toNPC = npc.getPosition().cpy().sub(cameraPos);
            float distance = toNPC.len();

            if (distance > reach) {
                continue; // Too far
            }

            // Check if NPC is in front of camera (dot product with direction)
            float dot = toNPC.nor().dot(direction);
            if (dot > 0.8f && distance < closestDistance) { // Must be facing NPC
                closestNPC = npc;
                closestDistance = distance;
            }
        }

        return closestNPC;
    }

    private void renderUI() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Phase 8: Render GameHUD (health/hunger/energy bars + crosshair)
        if (!openingSequence.isActive()) {
            // Phase 12: Update weather display
            gameHUD.setWeather(weatherSystem.getCurrentWeather());
            gameHUD.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Always render hotbar (unless opening sequence active)
        if (!openingSequence.isActive()) {
            hotbarUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Render clock
        clockHUD.render(spriteBatch, font, screenWidth, screenHeight);

        // Render inventory if visible
        if (inventoryUI.isVisible()) {
            inventoryUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Render help if visible
        if (helpUI.isVisible()) {
            helpUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Render crafting if visible
        if (craftingUI.isVisible()) {
            craftingUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
        }

        // Render tooltip if active
        if (tooltipSystem.isActive()) {
            renderTooltip();
        }
    }

    private void renderTooltip() {
        String message = tooltipSystem.getCurrentTooltip();
        if (message != null) {
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();

            // Render tooltip at bottom center
            spriteBatch.begin();
            font.draw(spriteBatch, message, screenWidth / 2 - 100, 100);
            spriteBatch.end();
        }
    }

    private void handleEscapePress() {
        // Close any open UI first
        if (inventoryUI.isVisible()) {
            inventoryUI.hide();
        } else if (helpUI.isVisible()) {
            helpUI.hide();
        } else if (craftingUI.isVisible()) {
            craftingUI.hide();
        } else if (state == GameState.PLAYING) {
            transitionToPaused();
        } else if (state == GameState.PAUSED) {
            transitionToPlaying();
        }
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

    @Override
    public void dispose() {
        modelBatch.dispose();
        chunkRenderer.dispose();
        spriteBatch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}

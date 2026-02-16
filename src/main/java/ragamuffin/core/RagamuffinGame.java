package ragamuffin.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.Player;
import ragamuffin.render.ChunkMeshBuilder;
import ragamuffin.render.ChunkRenderer;
import ragamuffin.world.Chunk;
import ragamuffin.world.World;

/**
 * Main game class - handles the 3D core engine.
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

    private static final float MOUSE_SENSITIVITY = 0.15f;

    @Override
    public void create() {
        Gdx.app.log("Ragamuffin", "Welcome to the real world, kid.");

        // Start in MENU state (later phases will add actual menu)
        state = GameState.MENU;
        transitionToPlaying(); // For Phase 1, jump straight to playing

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

        // Generate the world (Phase 2)
        Gdx.app.log("Ragamuffin", "Generating British town...");
        world = new World(System.currentTimeMillis());
        world.generate();

        // Create player at the park (world center)
        player = new Player(0, 5, 0);
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

        // Setup input
        inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(inputHandler);
        Gdx.input.setCursorCatched(true);
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

        // Handle state transitions
        if (inputHandler.isEscapePressed()) {
            handleEscapePress();
            inputHandler.resetEscape();
        }

        // Update game logic if playing
        if (state == GameState.PLAYING) {
            updatePlaying(delta);
        }

        // Render 3D world
        Gdx.gl.glClearColor(0.53f, 0.81f, 0.92f, 1f); // Sky blue
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        chunkRenderer.render(modelBatch, environment);
        modelBatch.end();
    }

    private void updatePlaying(float delta) {
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

    private void handleEscapePress() {
        if (state == GameState.PLAYING) {
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

    @Override
    public void dispose() {
        modelBatch.dispose();
        chunkRenderer.dispose();
    }
}

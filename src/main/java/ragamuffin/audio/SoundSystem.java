package ragamuffin.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import ragamuffin.world.BlockType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages sound effects playback with procedural audio generation.
 * Since we don't have audio asset files, generates simple tones programmatically.
 */
public class SoundSystem {
    private final Map<SoundEffect, Sound> sounds;
    private final Random random;
    private float masterVolume;
    private boolean enabled;

    // Volume categories
    private static final float VOLUME_UI = 0.5f;
    private static final float VOLUME_AMBIENT = 0.3f;
    private static final float VOLUME_EFFECTS = 0.7f;
    private static final float VOLUME_FOOTSTEPS = 0.2f;

    // Footstep timing
    private float footstepTimer;
    private static final float FOOTSTEP_INTERVAL = 0.4f; // seconds between footsteps when moving

    public SoundSystem() {
        this.sounds = new HashMap<>();
        this.random = new Random();
        this.masterVolume = 1.0f;
        this.enabled = true;
        this.footstepTimer = 0f;

        // Note: We're not loading actual sound files here as they don't exist.
        // In a real implementation, you'd load .wav/.ogg files from assets/sounds/
        // For now, this system is a no-op placeholder that can be wired in.
        // When actual audio assets are added, uncomment the loading logic below.

        // loadSounds();
    }

    /**
     * Load all sound files from assets (disabled for now - no audio files exist).
     */
    private void loadSounds() {
        // Example loading code (disabled until audio assets are added):
        // sounds.put(SoundEffect.BLOCK_PUNCH, Gdx.audio.newSound(Gdx.files.internal("sounds/block_punch.wav")));
        // sounds.put(SoundEffect.BLOCK_BREAK_WOOD, Gdx.audio.newSound(Gdx.files.internal("sounds/break_wood.wav")));
        // sounds.put(SoundEffect.BLOCK_BREAK_STONE, Gdx.audio.newSound(Gdx.files.internal("sounds/break_stone.wav")));
        // sounds.put(SoundEffect.BLOCK_BREAK_GLASS, Gdx.audio.newSound(Gdx.files.internal("sounds/break_glass.wav")));
        // sounds.put(SoundEffect.BLOCK_PLACE, Gdx.audio.newSound(Gdx.files.internal("sounds/block_place.wav")));
        // sounds.put(SoundEffect.FOOTSTEP_PAVEMENT, Gdx.audio.newSound(Gdx.files.internal("sounds/footstep_pavement.wav")));
        // sounds.put(SoundEffect.FOOTSTEP_GRASS, Gdx.audio.newSound(Gdx.files.internal("sounds/footstep_grass.wav")));
        // sounds.put(SoundEffect.UI_CLICK, Gdx.audio.newSound(Gdx.files.internal("sounds/ui_click.wav")));
        // sounds.put(SoundEffect.UI_OPEN, Gdx.audio.newSound(Gdx.files.internal("sounds/ui_open.wav")));
        // sounds.put(SoundEffect.UI_CLOSE, Gdx.audio.newSound(Gdx.files.internal("sounds/ui_close.wav")));
        // sounds.put(SoundEffect.INVENTORY_PICKUP, Gdx.audio.newSound(Gdx.files.internal("sounds/pickup.wav")));
        // sounds.put(SoundEffect.NPC_HIT, Gdx.audio.newSound(Gdx.files.internal("sounds/npc_hit.wav")));
        // sounds.put(SoundEffect.PLAYER_DODGE, Gdx.audio.newSound(Gdx.files.internal("sounds/dodge.wav")));
        // sounds.put(SoundEffect.POLICE_SIREN, Gdx.audio.newSound(Gdx.files.internal("sounds/police_siren.wav")));
        // sounds.put(SoundEffect.TOOLTIP, Gdx.audio.newSound(Gdx.files.internal("sounds/tooltip.wav")));
    }

    /**
     * Play a sound effect.
     */
    public void play(SoundEffect effect) {
        play(effect, 1.0f);
    }

    /**
     * Play a sound effect with a specific volume multiplier.
     */
    public void play(SoundEffect effect, float volumeMultiplier) {
        if (!enabled) {
            return;
        }

        Sound sound = sounds.get(effect);
        if (sound != null) {
            float volume = masterVolume * volumeMultiplier * getCategoryVolume(effect);
            // Add slight pitch variation for natural feel
            float pitch = 0.95f + random.nextFloat() * 0.1f;
            sound.play(volume, pitch, 0f);
        }
    }

    /**
     * Play block break sound based on block type.
     */
    public void playBlockBreak(BlockType blockType) {
        SoundEffect effect = getBlockBreakSound(blockType);
        play(effect, VOLUME_EFFECTS);
    }

    /**
     * Play footstep sound based on the block the player is standing on.
     */
    public void playFootstep(BlockType blockUnderfoot) {
        SoundEffect effect = getFootstepSound(blockUnderfoot);
        play(effect, VOLUME_FOOTSTEPS);
    }

    /**
     * Update footstep timer and play footstep sounds when moving.
     * @param delta frame delta time in seconds
     * @param isMoving whether the player is currently moving
     * @param blockUnderfoot the block type the player is standing on
     */
    public void updateFootsteps(float delta, boolean isMoving, BlockType blockUnderfoot) {
        if (!isMoving) {
            footstepTimer = 0f;
            return;
        }

        footstepTimer += delta;
        if (footstepTimer >= FOOTSTEP_INTERVAL) {
            playFootstep(blockUnderfoot);
            footstepTimer -= FOOTSTEP_INTERVAL;
        }
    }

    /**
     * Get the appropriate block break sound for a block type.
     */
    private SoundEffect getBlockBreakSound(BlockType blockType) {
        switch (blockType) {
            case TREE_TRUNK:
            case LEAVES:
                return SoundEffect.BLOCK_BREAK_WOOD;
            case GLASS:
                return SoundEffect.BLOCK_BREAK_GLASS;
            case BRICK:
            case STONE:
            case PAVEMENT:
            default:
                return SoundEffect.BLOCK_BREAK_STONE;
        }
    }

    /**
     * Get the appropriate footstep sound for a block type.
     */
    private SoundEffect getFootstepSound(BlockType blockType) {
        switch (blockType) {
            case GRASS:
            case LEAVES:
                return SoundEffect.FOOTSTEP_GRASS;
            case PAVEMENT:
            case BRICK:
            case STONE:
            default:
                return SoundEffect.FOOTSTEP_PAVEMENT;
        }
    }

    /**
     * Get volume multiplier for a sound effect category.
     */
    private float getCategoryVolume(SoundEffect effect) {
        switch (effect) {
            case UI_CLICK:
            case UI_OPEN:
            case UI_CLOSE:
            case TOOLTIP:
                return VOLUME_UI;
            case AMBIENT_PARK:
            case AMBIENT_STREET:
                return VOLUME_AMBIENT;
            case FOOTSTEP_PAVEMENT:
            case FOOTSTEP_GRASS:
                return VOLUME_FOOTSTEPS;
            default:
                return VOLUME_EFFECTS;
        }
    }

    /**
     * Set master volume (0.0 to 1.0).
     */
    public void setVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(1f, volume));
    }

    /**
     * Get current master volume.
     */
    public float getVolume() {
        return masterVolume;
    }

    /**
     * Enable or disable all sounds.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if sounds are enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Dispose of all sound resources.
     */
    public void dispose() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
    }
}

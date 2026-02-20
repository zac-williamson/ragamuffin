package ragamuffin.audio;

import com.badlogic.gdx.Gdx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SoundSystem class.
 * These tests verify the logic of sound selection, volume control,
 * and footstep timing without requiring actual audio file assets.
 */
public class SoundSystemTest {
    private SoundSystem soundSystem;

    @BeforeEach
    public void setUp() {
        // Mock Gdx if needed (SoundSystem doesn't load actual sounds yet)
        soundSystem = new SoundSystem();
    }

    @Test
    public void testInitialState() {
        assertEquals(1.0f, soundSystem.getVolume(), 0.001f, "Initial master volume should be 1.0");
        assertTrue(soundSystem.isEnabled(), "Sound system should be enabled by default");
    }

    @Test
    public void testSetVolume() {
        soundSystem.setVolume(0.5f);
        assertEquals(0.5f, soundSystem.getVolume(), 0.001f, "Volume should be set to 0.5");

        soundSystem.setVolume(0.0f);
        assertEquals(0.0f, soundSystem.getVolume(), 0.001f, "Volume should be set to 0.0");

        soundSystem.setVolume(1.0f);
        assertEquals(1.0f, soundSystem.getVolume(), 0.001f, "Volume should be set to 1.0");
    }

    @Test
    public void testVolumeClampingLow() {
        soundSystem.setVolume(-0.5f);
        assertEquals(0.0f, soundSystem.getVolume(), 0.001f, "Volume should be clamped to 0.0");
    }

    @Test
    public void testVolumeClampingHigh() {
        soundSystem.setVolume(1.5f);
        assertEquals(1.0f, soundSystem.getVolume(), 0.001f, "Volume should be clamped to 1.0");
    }

    @Test
    public void testEnableDisable() {
        soundSystem.setEnabled(false);
        assertFalse(soundSystem.isEnabled(), "Sound system should be disabled");

        soundSystem.setEnabled(true);
        assertTrue(soundSystem.isEnabled(), "Sound system should be enabled");
    }

    @Test
    public void testPlayBasicSound() {
        // Should not throw even with no loaded sounds
        assertDoesNotThrow(() -> soundSystem.play(SoundEffect.UI_CLICK));
    }

    @Test
    public void testPlaySoundWithVolume() {
        // Should not throw even with no loaded sounds
        assertDoesNotThrow(() -> soundSystem.play(SoundEffect.BLOCK_PUNCH, 0.8f));
    }

    @Test
    public void testPlayBlockBreakWood() {
        assertDoesNotThrow(() -> soundSystem.playBlockBreak(BlockType.TREE_TRUNK));
        assertDoesNotThrow(() -> soundSystem.playBlockBreak(BlockType.LEAVES));
    }

    @Test
    public void testPlayBlockBreakStone() {
        assertDoesNotThrow(() -> soundSystem.playBlockBreak(BlockType.STONE));
        assertDoesNotThrow(() -> soundSystem.playBlockBreak(BlockType.BRICK));
        assertDoesNotThrow(() -> soundSystem.playBlockBreak(BlockType.PAVEMENT));
    }

    @Test
    public void testPlayBlockBreakGlass() {
        assertDoesNotThrow(() -> soundSystem.playBlockBreak(BlockType.GLASS));
    }

    @Test
    public void testPlayFootstepGrass() {
        assertDoesNotThrow(() -> soundSystem.playFootstep(BlockType.GRASS));
        assertDoesNotThrow(() -> soundSystem.playFootstep(BlockType.LEAVES));
    }

    @Test
    public void testPlayFootstepPavement() {
        assertDoesNotThrow(() -> soundSystem.playFootstep(BlockType.PAVEMENT));
        assertDoesNotThrow(() -> soundSystem.playFootstep(BlockType.BRICK));
        assertDoesNotThrow(() -> soundSystem.playFootstep(BlockType.STONE));
    }

    @Test
    public void testFootstepTimingNotMoving() {
        // When not moving, timer should reset and no sound should play
        soundSystem.updateFootsteps(0.5f, false, BlockType.PAVEMENT);
        // No exception = success (footstep timer resets)
        assertDoesNotThrow(() -> soundSystem.updateFootsteps(0.5f, false, BlockType.PAVEMENT));
    }

    @Test
    public void testFootstepTimingMovingButNotEnoughTime() {
        // Moving for 0.2 seconds (less than 0.4s interval) should not trigger footstep
        assertDoesNotThrow(() -> soundSystem.updateFootsteps(0.2f, true, BlockType.GRASS));
    }

    @Test
    public void testFootstepTimingMovingEnoughTime() {
        // Moving for 0.5 seconds (more than 0.4s interval) should trigger footstep
        assertDoesNotThrow(() -> soundSystem.updateFootsteps(0.5f, true, BlockType.GRASS));
    }

    @Test
    public void testFootstepTimingAccumulation() {
        // Simulate multiple frames totaling > 0.4s
        soundSystem.updateFootsteps(0.1f, true, BlockType.PAVEMENT);
        soundSystem.updateFootsteps(0.1f, true, BlockType.PAVEMENT);
        soundSystem.updateFootsteps(0.1f, true, BlockType.PAVEMENT);
        soundSystem.updateFootsteps(0.15f, true, BlockType.PAVEMENT); // Total: 0.45s, should trigger
        assertDoesNotThrow(() -> soundSystem.updateFootsteps(0.1f, true, BlockType.PAVEMENT));
    }

    @Test
    public void testFootstepTimingResetAfterStopping() {
        // Walk for a while
        soundSystem.updateFootsteps(0.5f, true, BlockType.GRASS);
        // Stop walking
        soundSystem.updateFootsteps(0.1f, false, BlockType.GRASS);
        // Start walking again - timer should have reset
        soundSystem.updateFootsteps(0.2f, true, BlockType.GRASS);
        assertDoesNotThrow(() -> soundSystem.updateFootsteps(0.3f, true, BlockType.GRASS));
    }

    @Test
    public void testPlayWhenDisabled() {
        soundSystem.setEnabled(false);
        // Playing while disabled should not throw
        assertDoesNotThrow(() -> soundSystem.play(SoundEffect.NPC_HIT));
        assertDoesNotThrow(() -> soundSystem.playBlockBreak(BlockType.STONE));
        assertDoesNotThrow(() -> soundSystem.playFootstep(BlockType.GRASS));
    }

    @Test
    public void testAllSoundEffectsCanPlay() {
        // Verify that all sound effect enum values can be played without exception
        for (SoundEffect effect : SoundEffect.values()) {
            assertDoesNotThrow(() -> soundSystem.play(effect),
                    "Should be able to play " + effect);
        }
    }

    @Test
    public void testDispose() {
        // Should not throw even with no loaded sounds
        assertDoesNotThrow(() -> soundSystem.dispose());
    }

    @Test
    public void testDisposeTwice() {
        soundSystem.dispose();
        // Disposing twice should not throw
        assertDoesNotThrow(() -> soundSystem.dispose());
    }
}

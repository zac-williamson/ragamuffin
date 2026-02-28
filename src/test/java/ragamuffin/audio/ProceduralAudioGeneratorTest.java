package ragamuffin.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-Java tests for ProceduralAudioGenerator.
 * No LibGDX dependency — validates WAV structure and audio properties.
 */
public class ProceduralAudioGeneratorTest {

    private ProceduralAudioGenerator generator;

    @BeforeEach
    public void setUp() {
        generator = new ProceduralAudioGenerator();
    }

    @Test
    public void testAllNonAmbientSoundsGenerate() {
        for (SoundEffect effect : SoundEffect.values()) {
            byte[] wav = generator.generate(effect);
            if (effect == SoundEffect.AMBIENT_PARK || effect == SoundEffect.AMBIENT_STREET) {
                assertNull(wav, effect + " should return null (ambient)");
            } else {
                assertNotNull(wav, effect + " should generate a WAV");
                assertTrue(wav.length > 44, effect + " WAV should be larger than header");
            }
        }
    }

    @Test
    public void testWavHeaderStructure() {
        byte[] wav = generator.generate(SoundEffect.UI_CLICK);
        assertNotNull(wav);

        // RIFF header
        assertEquals('R', (char) wav[0]);
        assertEquals('I', (char) wav[1]);
        assertEquals('F', (char) wav[2]);
        assertEquals('F', (char) wav[3]);

        // WAVE format
        assertEquals('W', (char) wav[8]);
        assertEquals('A', (char) wav[9]);
        assertEquals('V', (char) wav[10]);
        assertEquals('E', (char) wav[11]);

        // fmt sub-chunk
        assertEquals('f', (char) wav[12]);
        assertEquals('m', (char) wav[13]);
        assertEquals('t', (char) wav[14]);
        assertEquals(' ', (char) wav[15]);

        ByteBuffer buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);

        // PCM format = 1
        assertEquals(1, buf.getShort(20));
        // Mono = 1 channel
        assertEquals(1, buf.getShort(22));
        // Sample rate = 44100
        assertEquals(44100, buf.getInt(24));
        // Bits per sample = 16
        assertEquals(16, buf.getShort(34));
    }

    @Test
    public void testWavFileSizeConsistency() {
        byte[] wav = generator.generate(SoundEffect.UI_CLICK);
        assertNotNull(wav);

        ByteBuffer buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        int riffSize = buf.getInt(4);
        assertEquals(wav.length - 8, riffSize, "RIFF size should be file size - 8");

        int dataSize = buf.getInt(40);
        assertEquals(wav.length - 44, dataSize, "data size should be file size - 44");
    }

    @Test
    public void testDeterministicOutput() {
        ProceduralAudioGenerator gen1 = new ProceduralAudioGenerator();
        ProceduralAudioGenerator gen2 = new ProceduralAudioGenerator();

        byte[] wav1 = gen1.generate(SoundEffect.BLOCK_PUNCH);
        byte[] wav2 = gen2.generate(SoundEffect.BLOCK_PUNCH);
        assertArrayEquals(wav1, wav2, "Same seed should produce identical output");
    }

    @Test
    public void testExpectedDurations() {
        // Check that sample counts match expected durations (within 1 sample)
        assertDuration(SoundEffect.UI_CLICK, 0.05);
        assertDuration(SoundEffect.UI_OPEN, 0.15);
        assertDuration(SoundEffect.UI_CLOSE, 0.12);
        assertDuration(SoundEffect.TOOLTIP, 0.08);
        assertDuration(SoundEffect.BLOCK_PUNCH, 0.1);
        assertDuration(SoundEffect.BLOCK_BREAK_WOOD, 0.2);
        assertDuration(SoundEffect.BLOCK_BREAK_STONE, 0.25);
        assertDuration(SoundEffect.BLOCK_BREAK_GLASS, 0.3);
        assertDuration(SoundEffect.BLOCK_PLACE, 0.12);
        assertDuration(SoundEffect.FOOTSTEP_PAVEMENT, 0.08);
        assertDuration(SoundEffect.FOOTSTEP_GRASS, 0.1);
        assertDuration(SoundEffect.NPC_HIT, 0.15);
        assertDuration(SoundEffect.PLAYER_DODGE, 0.12);
        assertDuration(SoundEffect.INVENTORY_PICKUP, 0.1);
        assertDuration(SoundEffect.ITEM_EAT, 0.2);
        assertDuration(SoundEffect.ITEM_USE, 0.15);
        assertDuration(SoundEffect.MUNCH, 0.35);
        assertDuration(SoundEffect.POLICE_SIREN, 1.0);
    }

    private void assertDuration(SoundEffect effect, double expectedSeconds) {
        byte[] wav = generator.generate(effect);
        assertNotNull(wav, effect + " should not be null");
        ByteBuffer buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        int dataSize = buf.getInt(40);
        int sampleCount = dataSize / 2; // 16-bit mono
        double actualSeconds = (double) sampleCount / 44100;
        assertEquals(expectedSeconds, actualSeconds, 0.001,
                effect + " duration mismatch");
    }

    @Test
    public void testSamplesNotAllZero() {
        // Every generated sound should have at least some non-zero samples
        for (SoundEffect effect : SoundEffect.values()) {
            byte[] wav = generator.generate(effect);
            if (wav == null) continue;

            boolean hasNonZero = false;
            for (int i = 44; i < wav.length; i++) {
                if (wav[i] != 0) {
                    hasNonZero = true;
                    break;
                }
            }
            assertTrue(hasNonZero, effect + " should have non-zero audio data");
        }
    }

    @Test
    public void testSamplesNotClipping() {
        // Check that no sound is entirely clipped (all max/min values)
        for (SoundEffect effect : SoundEffect.values()) {
            byte[] wav = generator.generate(effect);
            if (wav == null) continue;

            ByteBuffer buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
            int clippedCount = 0;
            int totalSamples = (wav.length - 44) / 2;
            for (int i = 44; i < wav.length - 1; i += 2) {
                short sample = buf.getShort(i);
                if (sample == Short.MAX_VALUE || sample == Short.MIN_VALUE) {
                    clippedCount++;
                }
            }
            double clipRatio = (double) clippedCount / totalSamples;
            assertTrue(clipRatio < 0.1,
                    effect + " has too many clipped samples: " + (clipRatio * 100) + "%");
        }
    }

    @Test
    public void testToWavWithEmptyArray() {
        short[] empty = new short[0];
        byte[] wav = generator.toWav(empty);
        assertEquals(44, wav.length, "Empty samples should produce header-only WAV");
    }

    @Test
    public void testToWavWithSingleSample() {
        short[] single = new short[]{12345};
        byte[] wav = generator.toWav(single);
        assertEquals(46, wav.length, "Single sample WAV should be 44 + 2 bytes");

        ByteBuffer buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(12345, buf.getShort(44));
    }

    @Test
    public void testDeterministicAcrossEffects() {
        // Generating effects in order should be deterministic
        ProceduralAudioGenerator gen1 = new ProceduralAudioGenerator();
        ProceduralAudioGenerator gen2 = new ProceduralAudioGenerator();

        for (SoundEffect effect : SoundEffect.values()) {
            byte[] wav1 = gen1.generate(effect);
            byte[] wav2 = gen2.generate(effect);
            if (wav1 == null) {
                assertNull(wav2);
            } else {
                assertArrayEquals(wav1, wav2,
                        effect + " should be deterministic when generated in same order");
            }
        }
    }
}

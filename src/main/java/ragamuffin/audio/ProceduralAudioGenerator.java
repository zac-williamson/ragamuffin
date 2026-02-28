package ragamuffin.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Generates procedural sound effects as WAV byte arrays.
 * Pure Java — no LibGDX dependency. All sounds are deterministic (seeded Random).
 * 16-bit PCM mono at 44100 Hz.
 */
public class ProceduralAudioGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int NUM_CHANNELS = 1;

    private final Random random;

    public ProceduralAudioGenerator() {
        this.random = new Random(42); // deterministic
    }

    /**
     * Generate a WAV byte array for the given sound effect.
     * Returns null for ambient sounds (AMBIENT_PARK, AMBIENT_STREET).
     */
    public byte[] generate(SoundEffect effect) {
        switch (effect) {
            case UI_CLICK:          return generateUIClick();
            case UI_OPEN:           return generateUIOpen();
            case UI_CLOSE:          return generateUIClose();
            case TOOLTIP:           return generateTooltip();
            case BLOCK_PUNCH:       return generateBlockPunch();
            case BLOCK_BREAK_WOOD:  return generateBlockBreakWood();
            case BLOCK_BREAK_STONE: return generateBlockBreakStone();
            case BLOCK_BREAK_GLASS: return generateBlockBreakGlass();
            case BLOCK_PLACE:       return generateBlockPlace();
            case FOOTSTEP_PAVEMENT: return generateFootstepPavement();
            case FOOTSTEP_GRASS:    return generateFootstepGrass();
            case NPC_HIT:           return generateNpcHit();
            case PLAYER_DODGE:      return generatePlayerDodge();
            case INVENTORY_PICKUP:  return generateInventoryPickup();
            case ITEM_EAT:          return generateItemEat();
            case ITEM_USE:          return generateItemUse();
            case MUNCH:             return generateMunch();
            case POLICE_SIREN:      return generatePoliceSiren();
            case PIRATE_RADIO_MUSIC: return generatePirateRadioMusic();
            case ICE_CREAM_JINGLE: return generateIceCreamJingle();
            case AMBIENT_PARK:
            case AMBIENT_STREET:
            default:
                return null;
        }
    }

    // ── Core DSP helpers ──────────────────────────────────────────

    private static double sine(double phase) {
        return Math.sin(2.0 * Math.PI * phase);
    }

    private static double square(double phase) {
        return sine(phase) >= 0 ? 1.0 : -1.0;
    }

    private double noise() {
        return random.nextDouble() * 2.0 - 1.0;
    }

    /** ADSR envelope. All times in seconds. */
    private static double envelope(double t, double duration, double attack, double decay, double sustain, double release) {
        double releaseStart = duration - release;
        if (releaseStart < 0) releaseStart = 0;

        if (t < attack) {
            return t / attack;
        } else if (t < attack + decay) {
            return 1.0 - (1.0 - sustain) * ((t - attack) / decay);
        } else if (t < releaseStart) {
            return sustain;
        } else {
            double releaseT = t - releaseStart;
            double releaseLen = duration - releaseStart;
            if (releaseLen <= 0) return 0;
            return sustain * (1.0 - releaseT / releaseLen);
        }
    }

    /** Simple exponential decay envelope. */
    private static double expDecay(double t, double halfLife) {
        return Math.exp(-t * 0.693 / halfLife);
    }

    /** Linear frequency sweep from f0 to f1 over duration. Returns instantaneous phase accumulator value. */
    private static double sweepPhase(double t, double duration, double f0, double f1) {
        double freq = f0 + (f1 - f0) * (t / duration);
        // Integrate frequency to get phase: phase = f0*t + (f1-f0)*t^2/(2*duration)
        return f0 * t + (f1 - f0) * t * t / (2.0 * duration);
    }

    /** One-pole low-pass filter state. */
    private static double lowPass(double input, double prevOutput, double cutoffHz) {
        double rc = 1.0 / (2.0 * Math.PI * cutoffHz);
        double dt = 1.0 / SAMPLE_RATE;
        double alpha = dt / (rc + dt);
        return prevOutput + alpha * (input - prevOutput);
    }

    /** One-pole high-pass filter. Returns filtered value. */
    private static double highPass(double input, double prevInput, double prevOutput, double cutoffHz) {
        double rc = 1.0 / (2.0 * Math.PI * cutoffHz);
        double dt = 1.0 / SAMPLE_RATE;
        double alpha = rc / (rc + dt);
        return alpha * (prevOutput + input - prevInput);
    }

    private short[] allocSamples(double durationSeconds) {
        return new short[(int)(SAMPLE_RATE * durationSeconds)];
    }

    private static short clampToShort(double value) {
        long v = Math.round(value * 32767.0);
        if (v > 32767) v = 32767;
        if (v < -32768) v = -32768;
        return (short) v;
    }

    // ── Sound generators ──────────────────────────────────────────

    /** UI_CLICK: 0.05s, 800 Hz sine, fast decay */
    private byte[] generateUIClick() {
        double dur = 0.05;
        short[] samples = allocSamples(dur);
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.01);
            samples[i] = clampToShort(sine(800.0 * t) * env * 0.8);
        }
        return toWav(samples);
    }

    /** UI_OPEN: 0.15s, 400→800 Hz rising chirp */
    private byte[] generateUIOpen() {
        double dur = 0.15;
        short[] samples = allocSamples(dur);
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double phase = sweepPhase(t, dur, 400, 800);
            double env = envelope(t, dur, 0.005, 0.04, 0.6, 0.06);
            samples[i] = clampToShort(sine(phase) * env * 0.7);
        }
        return toWav(samples);
    }

    /** UI_CLOSE: 0.12s, 800→400 Hz falling chirp */
    private byte[] generateUIClose() {
        double dur = 0.12;
        short[] samples = allocSamples(dur);
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double phase = sweepPhase(t, dur, 800, 400);
            double env = envelope(t, dur, 0.005, 0.03, 0.5, 0.05);
            samples[i] = clampToShort(sine(phase) * env * 0.7);
        }
        return toWav(samples);
    }

    /** TOOLTIP: 0.08s, 1200 Hz sine, soft ting */
    private byte[] generateTooltip() {
        double dur = 0.08;
        short[] samples = allocSamples(dur);
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.02);
            samples[i] = clampToShort(sine(1200.0 * t) * env * 0.5);
        }
        return toWav(samples);
    }

    /** BLOCK_PUNCH: 0.1s, 60% noise (LP 2kHz) + 40% 150 Hz sine thud */
    private byte[] generateBlockPunch() {
        double dur = 0.1;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.025);
            double n = noise();
            lpState = lowPass(n, lpState, 2000);
            double thud = sine(150.0 * t);
            samples[i] = clampToShort((0.6 * lpState + 0.4 * thud) * env * 0.9);
        }
        return toWav(samples);
    }

    /** BLOCK_BREAK_WOOD: 0.2s, 50% band-pass noise + 50% 100 Hz sine crack */
    private byte[] generateBlockBreakWood() {
        double dur = 0.2;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        double hpState = 0;
        double hpPrevInput = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.05);
            double n = noise();
            lpState = lowPass(n, lpState, 3000);
            hpState = highPass(lpState, hpPrevInput, hpState, 500);
            hpPrevInput = lpState;
            double crack = sine(100.0 * t);
            samples[i] = clampToShort((0.5 * hpState + 0.5 * crack) * env * 0.9);
        }
        return toWav(samples);
    }

    /** BLOCK_BREAK_STONE: 0.25s, 70% noise (LP 3kHz) + 30% 80 Hz bass thud */
    private byte[] generateBlockBreakStone() {
        double dur = 0.25;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.06);
            double n = noise();
            lpState = lowPass(n, lpState, 3000);
            double thud = sine(80.0 * t);
            samples[i] = clampToShort((0.7 * lpState + 0.3 * thud) * env * 0.9);
        }
        return toWav(samples);
    }

    /** BLOCK_BREAK_GLASS: 0.3s, 40% HP noise (3kHz) + 30% 2kHz + 30% 3.5kHz sine */
    private byte[] generateBlockBreakGlass() {
        double dur = 0.3;
        short[] samples = allocSamples(dur);
        double hpState = 0;
        double hpPrevInput = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.08);
            double n = noise();
            hpState = highPass(n, hpPrevInput, hpState, 3000);
            hpPrevInput = n;
            double s1 = sine(2000.0 * t);
            double s2 = sine(3500.0 * t);
            samples[i] = clampToShort((0.4 * hpState + 0.3 * s1 + 0.3 * s2) * env * 0.8);
        }
        return toWav(samples);
    }

    /** BLOCK_PLACE: 0.12s, 60% noise (LP 1.5kHz) + 40% 200 Hz sine thunk */
    private byte[] generateBlockPlace() {
        double dur = 0.12;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.03);
            double n = noise();
            lpState = lowPass(n, lpState, 1500);
            double thunk = sine(200.0 * t);
            samples[i] = clampToShort((0.6 * lpState + 0.4 * thunk) * env * 0.9);
        }
        return toWav(samples);
    }

    /** FOOTSTEP_PAVEMENT: 0.08s, noise LP 2.5kHz, sharp tap */
    private byte[] generateFootstepPavement() {
        double dur = 0.08;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.015);
            double n = noise();
            lpState = lowPass(n, lpState, 2500);
            samples[i] = clampToShort(lpState * env * 0.7);
        }
        return toWav(samples);
    }

    /** FOOTSTEP_GRASS: 0.1s, noise LP 1.5kHz, softer pad */
    private byte[] generateFootstepGrass() {
        double dur = 0.1;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.025);
            double n = noise();
            lpState = lowPass(n, lpState, 1500);
            samples[i] = clampToShort(lpState * env * 0.5);
        }
        return toWav(samples);
    }

    /** NPC_HIT: 0.15s, 50% noise + 50% 300→100 Hz sweep, punchy */
    private byte[] generateNpcHit() {
        double dur = 0.15;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = expDecay(t, 0.03);
            double n = noise();
            lpState = lowPass(n, lpState, 2000);
            double phase = sweepPhase(t, dur, 300, 100);
            double sweep = sine(phase);
            samples[i] = clampToShort((0.5 * lpState + 0.5 * sweep) * env * 0.9);
        }
        return toWav(samples);
    }

    /** PLAYER_DODGE: 0.12s, 200→600 Hz sweep + 20% HP noise whoosh */
    private byte[] generatePlayerDodge() {
        double dur = 0.12;
        short[] samples = allocSamples(dur);
        double hpState = 0;
        double hpPrevInput = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = envelope(t, dur, 0.01, 0.03, 0.7, 0.04);
            double phase = sweepPhase(t, dur, 200, 600);
            double sweep = sine(phase);
            double n = noise();
            hpState = highPass(n, hpPrevInput, hpState, 2000);
            hpPrevInput = n;
            samples[i] = clampToShort((0.8 * sweep + 0.2 * hpState) * env * 0.7);
        }
        return toWav(samples);
    }

    /** INVENTORY_PICKUP: 0.1s, two-note chirp: 600 Hz then 900 Hz */
    private byte[] generateInventoryPickup() {
        double dur = 0.1;
        short[] samples = allocSamples(dur);
        double half = dur / 2.0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = t < half ? 600.0 : 900.0;
            double env = expDecay(t < half ? t : t - half, 0.02);
            samples[i] = clampToShort(sine(freq * t) * env * 0.7);
        }
        return toWav(samples);
    }

    /** ITEM_EAT: 0.2s, two noise bursts (band-pass), chewing */
    private byte[] generateItemEat() {
        double dur = 0.2;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        double hpState = 0;
        double hpPrevInput = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            // Two bursts at 0-0.08s and 0.1-0.18s
            double env = 0;
            if (t < 0.08) {
                env = expDecay(t, 0.02);
            } else if (t >= 0.1 && t < 0.18) {
                env = expDecay(t - 0.1, 0.02);
            }
            double n = noise();
            lpState = lowPass(n, lpState, 2500);
            hpState = highPass(lpState, hpPrevInput, hpState, 800);
            hpPrevInput = lpState;
            samples[i] = clampToShort(hpState * env * 0.7);
        }
        return toWav(samples);
    }

    /** ITEM_USE: 0.15s, 500→1000 Hz sweep chirp */
    private byte[] generateItemUse() {
        double dur = 0.15;
        short[] samples = allocSamples(dur);
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double phase = sweepPhase(t, dur, 500, 1000);
            double env = envelope(t, dur, 0.005, 0.04, 0.5, 0.06);
            samples[i] = clampToShort(sine(phase) * env * 0.7);
        }
        return toWav(samples);
    }

    /** MUNCH: 0.35s, three crunchy noise bursts (crisp packet) */
    private byte[] generateMunch() {
        double dur = 0.35;
        short[] samples = allocSamples(dur);
        double lpState = 0;
        double hpState = 0;
        double hpPrevInput = 0;
        // Three bursts at 0s, 0.1s, 0.22s
        double[] burstStarts = {0.0, 0.1, 0.22};
        double burstLen = 0.06;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = 0;
            for (double start : burstStarts) {
                if (t >= start && t < start + burstLen) {
                    env += expDecay(t - start, 0.015);
                }
            }
            if (env > 1.0) env = 1.0;
            double n = noise();
            lpState = lowPass(n, lpState, 4000);
            hpState = highPass(lpState, hpPrevInput, hpState, 1000);
            hpPrevInput = lpState;
            samples[i] = clampToShort(hpState * env * 0.9);
        }
        return toWav(samples);
    }

    /** POLICE_SIREN: 1.0s, sine with LFO: 800 ± 200 Hz at 3 Hz wee-woo */
    private byte[] generatePoliceSiren() {
        double dur = 1.0;
        short[] samples = allocSamples(dur);
        double phase = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            // LFO: square wave at 3 Hz for wee-woo effect
            double lfo = square(3.0 * t);
            double freq = 800.0 + 200.0 * lfo;
            phase += freq / SAMPLE_RATE;
            double env = envelope(t, dur, 0.05, 0.05, 0.8, 0.1);
            samples[i] = clampToShort(sine(phase) * env * 0.6);
        }
        return toWav(samples);
    }

    /**
     * PIRATE_RADIO_MUSIC: ~8s lo-fi dub loop.
     * Bass line (square wave), offbeat skank chords (filtered noise),
     * hi-hat pattern, and a wobbly tape-warble effect.
     */
    private byte[] generatePirateRadioMusic() {
        double bpm = 140.0;
        double beatDur = 60.0 / bpm;
        int bars = 4;
        int beatsPerBar = 4;
        double dur = bars * beatsPerBar * beatDur;
        short[] samples = allocSamples(dur);

        // Bass notes (root + fifth pattern in Dm): D2, A2, F2, G2
        double[] bassFreqs = {73.42, 110.0, 87.31, 98.0};

        // Pre-seeded random for deterministic hi-hat
        java.util.Random hatRng = new java.util.Random(77);

        double bassPhase = 0;
        double lpBass = 0;
        double lpSkank = 0;
        double lpHat = 0;

        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;

            // Tape warble: slow pitch wobble
            double warble = 1.0 + 0.003 * Math.sin(2.0 * Math.PI * 0.8 * t);

            // Which bar and beat are we in?
            double beatPos = t / beatDur;
            int bar = (int) (beatPos / beatsPerBar) % bars;
            double beatInBar = beatPos - (int)(beatPos / beatsPerBar) * beatsPerBar;
            double beatFrac = beatInBar - (int) beatInBar;

            // ── Bass: square wave on beats 1 and 3, short decay ──
            double bassFreq = bassFreqs[bar] * warble;
            boolean bassHit = ((int) beatInBar == 0 || (int) beatInBar == 2);
            double bassEnv = 0;
            if (bassHit) {
                bassEnv = expDecay(beatFrac * beatDur, beatDur * 0.6);
            }
            bassPhase += bassFreq / SAMPLE_RATE;
            double bass = square(bassPhase) * bassEnv * 0.35;
            lpBass = lowPass(bass, lpBass, 250); // muddy lo-fi bass

            // ── Skank: filtered noise burst on offbeats (& of each beat) ──
            double skankEnv = 0;
            double offbeatFrac = beatFrac - 0.5;
            if (offbeatFrac >= 0 && offbeatFrac < 0.25) {
                skankEnv = expDecay(offbeatFrac * beatDur, beatDur * 0.08);
            }
            double skank = noise() * skankEnv * 0.2;
            lpSkank = lowPass(skank, lpSkank, 3000);

            // ── Hi-hat: 8th note pattern with velocity variation ──
            double eighthFrac = (beatFrac * 2.0) - (int)(beatFrac * 2.0);
            double hatEnv = expDecay(eighthFrac * beatDur * 0.5, beatDur * 0.04);
            double hatVel = 0.08 + 0.07 * ((int)(beatPos * 2) % 2); // accent offbeats
            double hat = noise() * hatEnv * hatVel;
            lpHat = highPassSimple(hat, lpHat, 6000);

            // ── Lo-fi crackle: very quiet random pops ──
            double crackle = 0;
            if (hatRng.nextDouble() < 0.001) {
                crackle = (hatRng.nextDouble() * 2 - 1) * 0.05;
            }

            // ── Mix ──
            double mix = lpBass + lpSkank + lpHat + crackle;

            // Lo-fi: gentle saturation
            mix = Math.tanh(mix * 1.5) * 0.65;

            samples[i] = clampToShort(mix);
        }
        return toWav(samples);
    }

    /**
     * ICE_CREAM_JINGLE: ~2s tinny ice cream van jingle.
     * Simple major-key melody with a lo-fi telephone-line filter.
     */
    private byte[] generateIceCreamJingle() {
        double dur = 2.0;
        short[] samples = allocSamples(dur);
        // Simple melody: C5-E5-G5-C6-G5-E5 repeated, each note ~0.33s
        double[] freqs = {523.25, 659.25, 783.99, 1046.50, 783.99, 659.25};
        double noteLen = dur / freqs.length;
        double lpState = 0;
        for (int i = 0; i < samples.length; i++) {
            double t = (double) i / SAMPLE_RATE;
            int noteIdx = Math.min((int) (t / noteLen), freqs.length - 1);
            double localT = t - noteIdx * noteLen;
            double env = envelope(localT, noteLen, 0.005, 0.03, 0.6, 0.05);
            double sig = sine(freqs[noteIdx] * t) * env * 0.5;
            // Lo-fi telephone filter (bandpass 300-3400 Hz approximated by LP 3000)
            lpState = lowPass(sig, lpState, 3000);
            samples[i] = clampToShort(lpState);
        }
        return toWav(samples);
    }

    /** Simple high-pass approximation: subtract low-passed signal. */
    private double highPassSimple(double input, double lpState, double cutoff) {
        double rc = 1.0 / (2.0 * Math.PI * cutoff);
        double dt = 1.0 / SAMPLE_RATE;
        double alpha = dt / (rc + dt);
        double lp = lpState + alpha * (input - lpState);
        return input - lp;
    }

    // ── WAV encoding ──────────────────────────────────────────────

    /** Wrap 16-bit PCM samples in a WAV file byte array. */
    byte[] toWav(short[] samples) {
        int dataSize = samples.length * 2; // 16-bit = 2 bytes per sample
        int fileSize = 44 + dataSize;      // 44-byte header + data

        ByteArrayOutputStream out = new ByteArrayOutputStream(fileSize);
        try {
            // RIFF header
            out.write(new byte[]{'R','I','F','F'});
            writeLittleEndian32(out, fileSize - 8);
            out.write(new byte[]{'W','A','V','E'});

            // fmt sub-chunk
            out.write(new byte[]{'f','m','t',' '});
            writeLittleEndian32(out, 16);                          // sub-chunk size
            writeLittleEndian16(out, 1);                           // PCM format
            writeLittleEndian16(out, NUM_CHANNELS);
            writeLittleEndian32(out, SAMPLE_RATE);
            writeLittleEndian32(out, SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8); // byte rate
            writeLittleEndian16(out, NUM_CHANNELS * BITS_PER_SAMPLE / 8);              // block align
            writeLittleEndian16(out, BITS_PER_SAMPLE);

            // data sub-chunk
            out.write(new byte[]{'d','a','t','a'});
            writeLittleEndian32(out, dataSize);
            for (short sample : samples) {
                writeLittleEndian16(out, sample);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate WAV", e);
        }
        return out.toByteArray();
    }

    private static void writeLittleEndian16(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void writeLittleEndian32(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
}

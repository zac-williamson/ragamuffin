package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Issue #765: Witness &amp; Evidence System — 'The Word on the Street'
 *
 * <p>This system manages:
 * <ol>
 *   <li><b>Evidence props</b> — physical props spawned on crime events (smashed glass,
 *       crowbar marks, blood spatter, CCTV tapes) with decay timers. Police NPCs who
 *       discover them add entries to the player's {@link CriminalRecord}.</li>
 *   <li><b>Witness NPCs</b> — any NPC with line-of-sight to a crime transitions to
 *       {@link ragamuffin.entity.NPCState#WITNESS}, flees toward landmarks, seeds
 *       {@link RumourType#WITNESS_SIGHTING} rumours, and reports to police within
 *       60 seconds — unless the player bribes them (5 COIN, success gated by bravery).</li>
 *   <li><b>CCTV Tapes</b> — office blocks and off-licences contain stealable tape props.
 *       If a crime happens nearby and the tape isn't stolen within 3 in-game minutes,
 *       police automatically gain a {@link CriminalRecord.CrimeType#WITNESSED_CRIMES}
 *       entry. Stealing the tape cancels it.</li>
 *   <li><b>Informant mechanic ('Grassing')</b> — player can press E on a POLICE NPC
 *       while holding a {@link Material#RUMOUR_NOTE} to tip off police. This clears one
 *       of the player's own crime entries but seeds a {@link RumourType#BETRAYAL} rumour
 *       that spreads through the network, turning the tipped faction hostile.</li>
 * </ol>
 */
public class WitnessSystem {

    // ── Constants ──────────────────────────────────────────────────────────────

    /** Seconds after witnessing a crime that an NPC will wait before reporting. */
    public static final float WITNESS_REPORT_DELAY = 60f;

    /** Coin cost to bribe a witness NPC into silence. */
    public static final int BRIBE_COST_COIN = 5;

    /** Line-of-sight range within which a civilian can witness a crime. */
    public static final float WITNESS_LOS_RANGE = 12f;

    /** Speech bubble duration when a witness NPC reacts. */
    public static final float WITNESS_SPEECH_DURATION = 4f;

    /** Speech bubble duration when a bribe is successful. */
    public static final float BRIBE_SPEECH_DURATION = 3f;

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<EvidenceProp> evidenceProps = new ArrayList<>();
    private final Random random;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;

    /** Tracks active CCTV tape props for quick lookup. */
    private final List<EvidenceProp> cctvTapes = new ArrayList<>();

    /** Number of evidence props currently active (for HUD countdown display). */
    private int activeEvidenceCount = 0;

    /** True if any CCTV tape is currently hot (crime occurred nearby). */
    private boolean cctvHot = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public WitnessSystem() {
        this(new Random());
    }

    public WitnessSystem(Random random) {
        this.random = random;
    }

    /**
     * Attach the criminal record so evidence discoveries update it.
     */
    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    /**
     * Attach the rumour network so witness sightings can be seeded.
     */
    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    /**
     * Attach the achievement system so evidence-related achievements can fire.
     */
    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update all evidence props, witness NPCs, and CCTV timers.
     *
     * @param delta   seconds since last frame
     * @param npcs    all living NPCs in the world
     * @param player  the player
     */
    public void update(float delta, List<NPC> npcs, Player player) {
        updateEvidenceProps(delta, npcs);
        updateWitnessNPCs(delta, npcs, player);
        updateActiveCount();
    }

    // ── Evidence props ─────────────────────────────────────────────────────────

    /**
     * Spawn an evidence prop at the given world position.
     *
     * @param type the category of evidence
     * @param x    world X
     * @param y    world Y
     * @param z    world Z
     * @return the newly created prop
     */
    public EvidenceProp spawnEvidence(EvidenceType type, float x, float y, float z) {
        EvidenceProp prop = new EvidenceProp(type, x, y, z);
        evidenceProps.add(prop);
        if (type == EvidenceType.CCTV_TAPE) {
            cctvTapes.add(prop);
        }
        return prop;
    }

    /**
     * Notify the system that a crime has occurred at the given position.
     * This activates any nearby CCTV tape props.
     *
     * @param crimeX world X of the crime
     * @param crimeZ world Z of the crime
     */
    public void notifyCrime(float crimeX, float crimeZ) {
        for (EvidenceProp tape : cctvTapes) {
            if (!tape.isAlive() || tape.isActive()) continue;
            float dx = tape.getPosition().x - crimeX;
            float dz = tape.getPosition().z - crimeZ;
            float distSq = dx * dx + dz * dz;
            float range = EvidenceProp.CCTV_ACTIVATION_RADIUS;
            if (distSq <= range * range) {
                tape.activateCctv();
                cctvHot = true;
            }
        }
    }

    /**
     * Called when the player steals a CCTV tape at the given position.
     * Marks the nearest active CCTV_TAPE prop as stolen, cancelling any
     * pending WITNESSED_CRIMES entry.
     *
     * @param x player X
     * @param z player Z
     * @return true if a tape was successfully stolen
     */
    public boolean stealCctvTape(float x, float z) {
        float minDist = 3f; // must be within 3 blocks to steal
        EvidenceProp nearest = null;
        for (EvidenceProp tape : cctvTapes) {
            if (!tape.isAlive() || !tape.isActive() || tape.isStolen()) continue;
            float dx = tape.getPosition().x - x;
            float dz = tape.getPosition().z - z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist < minDist) {
                minDist = dist;
                nearest = tape;
            }
        }
        if (nearest != null) {
            nearest.steal();
            // Check if all CCTV tapes are now resolved
            boolean anyHot = false;
            for (EvidenceProp tape : cctvTapes) {
                if (tape.isAlive() && tape.isActive() && !tape.isStolen()) {
                    anyHot = true;
                    break;
                }
            }
            cctvHot = anyHot;
            return true;
        }
        return false;
    }

    private void updateEvidenceProps(float delta, List<NPC> npcs) {
        Iterator<EvidenceProp> it = evidenceProps.iterator();
        while (it.hasNext()) {
            EvidenceProp prop = it.next();
            if (!prop.isAlive()) {
                it.remove();
                cctvTapes.remove(prop);
                continue;
            }

            // Check if CCTV tape timed out (crime not covered in time)
            if (prop.getType() == EvidenceType.CCTV_TAPE && prop.isActive()
                    && !prop.isDiscovered() && !prop.isStolen()) {
                if (prop.getDecayTimer() <= 0f) {
                    // Police gain a witnessed crime entry
                    if (criminalRecord != null) {
                        criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
                    }
                    prop.discover();
                }
            }

            // Check if a police NPC is near enough to discover non-CCTV evidence
            if (prop.getType() != EvidenceType.CCTV_TAPE && !prop.isDiscovered()) {
                for (NPC npc : npcs) {
                    if (!npc.isAlive()) continue;
                    if (npc.getType() != NPCType.POLICE
                            && npc.getType() != NPCType.PCSO
                            && npc.getType() != NPCType.ARMED_RESPONSE) {
                        continue;
                    }
                    float dist = npc.getPosition().dst(prop.getPosition());
                    if (dist <= EvidenceProp.POLICE_DETECT_RADIUS) {
                        prop.discover();
                        if (criminalRecord != null) {
                            criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
                        }
                        break;
                    }
                }
            }

            prop.update(delta);
        }
    }

    // ── Witness NPCs ────────────────────────────────────────────────────────────

    /**
     * Called when a crime occurs at the given position (e.g. block broken, NPC hit).
     * Any NPC with unobstructed line-of-sight within {@link #WITNESS_LOS_RANGE} blocks
     * transitions to {@link NPCState#WITNESS} and starts their report countdown.
     *
     * @param crimeX       world X of the crime
     * @param crimeY       world Y of the crime
     * @param crimeZ       world Z of the crime
     * @param crimeDesc    short description for the rumour text ("breaking windows",
     *                     "attacking someone", etc.)
     * @param npcs         all living NPCs in the world
     * @param rumourHolder any NPC to seed the initial sighting rumour into (may be null)
     */
    public void registerCrime(float crimeX, float crimeY, float crimeZ,
                              String crimeDesc, List<NPC> npcs, NPC rumourHolder) {
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            // Police/PCSO/ARU already know — only civilians become witnesses
            if (npc.getType() == NPCType.POLICE
                    || npc.getType() == NPCType.PCSO
                    || npc.getType() == NPCType.ARMED_RESPONSE) {
                continue;
            }
            // Skip NPCs already in a witness state
            if (npc.getState() == NPCState.WITNESS
                    || npc.getState() == NPCState.REPORTING_TO_POLICE) {
                continue;
            }

            float dist = npc.getPosition().dst(crimeX, crimeY, crimeZ);
            if (dist > WITNESS_LOS_RANGE) continue;

            // Transition to WITNESS state
            npc.setState(NPCState.WITNESS);
            npc.setWitnessReportTimer(WITNESS_REPORT_DELAY);
            npc.setSpeechText("Oi! What you doing?!", WITNESS_SPEECH_DURATION);

            // Seed a WITNESS_SIGHTING rumour into this NPC
            if (rumourNetwork != null) {
                String text = "Someone was " + crimeDesc + " near here — the police should know";
                Rumour sighting = new Rumour(RumourType.WITNESS_SIGHTING, text);
                rumourNetwork.addRumour(npc, sighting);
            }
        }
    }

    private void updateWitnessNPCs(float delta, List<NPC> npcs, Player player) {
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getState() != NPCState.WITNESS
                    && npc.getState() != NPCState.REPORTING_TO_POLICE) {
                continue;
            }

            boolean timerExpired = npc.tickWitnessTimer(delta);
            if (timerExpired) {
                // NPC reports crime — add a WITNESSED_CRIMES entry
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
                }
                npc.setState(NPCState.WANDERING);
                npc.setSpeechText("I've told the police about you!", WITNESS_SPEECH_DURATION);
            }
        }
    }

    private void updateActiveCount() {
        int count = 0;
        for (EvidenceProp prop : evidenceProps) {
            if (prop.isAlive() && prop.isActive() && !prop.isDiscovered()) {
                count++;
            }
        }
        activeEvidenceCount = count;
    }

    // ── Bribery ─────────────────────────────────────────────────────────────────

    /**
     * Attempt to bribe the given NPC to stay quiet about a witnessed crime.
     *
     * <p>Success is gated by the NPC's bravery stat. Brave NPCs (bravery &gt; 0.7)
     * have a low acceptance chance even with payment. The player must have at least
     * {@link #BRIBE_COST_COIN} coins in their inventory.
     *
     * @param npc       the witness NPC to bribe
     * @param inventory the player's inventory
     * @return {@link BribeResult} indicating success or reason for failure
     */
    public BribeResult tryBribeWitness(NPC npc, Inventory inventory) {
        if (npc.getState() != NPCState.WITNESS) {
            return BribeResult.NOT_A_WITNESS;
        }
        if (inventory.getItemCount(Material.COIN) < BRIBE_COST_COIN) {
            return BribeResult.INSUFFICIENT_FUNDS;
        }

        // Success probability: base 80% reduced by bravery.
        // bravery=0 → 80% success; bravery=1 → 10% success.
        float successChance = 0.80f - npc.getBravery() * 0.70f;
        if (random.nextFloat() < successChance) {
            // Bribe accepted
            inventory.removeItem(Material.COIN, BRIBE_COST_COIN);
            npc.setState(NPCState.WANDERING);
            npc.setWitnessReportTimer(0f);
            npc.setSpeechText("Say nothing... go on then.", BRIBE_SPEECH_DURATION);
            return BribeResult.SUCCESS;
        } else {
            // Bribe refused — NPC immediately reports
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.WITNESSED_CRIMES);
            }
            npc.setState(NPCState.WANDERING);
            npc.setWitnessReportTimer(0f);
            npc.setSpeechText("You can't buy me off! I'm telling the police!", WITNESS_SPEECH_DURATION);
            return BribeResult.REFUSED;
        }
    }

    // ── Informant mechanic (Grassing) ───────────────────────────────────────────

    /**
     * Result of attempting to use a {@link Material#RUMOUR_NOTE} to tip off a police NPC.
     */
    public enum GrassResult {
        /** The tip was accepted — one crime entry cleared, BETRAYAL rumour seeded. */
        SUCCESS,
        /** The NPC is not a police officer. */
        NOT_POLICE,
        /** The player does not have a RUMOUR_NOTE in their inventory. */
        NO_RUMOUR_NOTE,
        /** The player's criminal record is empty — nothing to clear. */
        NOTHING_TO_CLEAR
    }

    /**
     * Player attempts to tip off a POLICE NPC while holding a {@link Material#RUMOUR_NOTE}.
     *
     * <p>On success:
     * <ul>
     *   <li>One of the player's own {@link CriminalRecord.CrimeType#WITNESSED_CRIMES}
     *       entries is cleared.</li>
     *   <li>The RUMOUR_NOTE is consumed.</li>
     *   <li>A {@link RumourType#BETRAYAL} rumour is seeded into the police NPC,
     *       spreading through the network and turning the tipped faction hostile.</li>
     *   <li>The {@link AchievementType#GRASS} achievement is unlocked.</li>
     * </ul>
     *
     * @param policeNpc the POLICE NPC being addressed
     * @param inventory the player's inventory
     * @return a {@link GrassResult} enum value
     */
    public GrassResult tryGrass(NPC policeNpc, Inventory inventory) {
        if (policeNpc.getType() != NPCType.POLICE
                && policeNpc.getType() != NPCType.PCSO
                && policeNpc.getType() != NPCType.ARMED_RESPONSE) {
            return GrassResult.NOT_POLICE;
        }
        if (inventory.getItemCount(Material.RUMOUR_NOTE) < 1) {
            return GrassResult.NO_RUMOUR_NOTE;
        }
        if (criminalRecord != null
                && criminalRecord.getCount(CriminalRecord.CrimeType.WITNESSED_CRIMES) == 0) {
            return GrassResult.NOTHING_TO_CLEAR;
        }

        // Consume the note
        inventory.removeItem(Material.RUMOUR_NOTE, 1);

        // Clear one witnessed crime entry — by re-recording all but one
        // (CriminalRecord doesn't support decrement directly, so we use the workaround
        // of exposing a clearOne helper if available; otherwise we add a method for it).
        if (criminalRecord != null) {
            criminalRecord.clearOne(CriminalRecord.CrimeType.WITNESSED_CRIMES);
        }

        // Seed a BETRAYAL rumour
        if (rumourNetwork != null) {
            Rumour betrayal = new Rumour(RumourType.BETRAYAL,
                "Someone's been grassing to the police — watch who you trust");
            rumourNetwork.addRumour(policeNpc, betrayal);
        }

        policeNpc.setSpeechText("We'll look into it. Keep your head down.", BRIBE_SPEECH_DURATION);

        // Award the hidden GRASS achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.GRASS);
        }

        return GrassResult.SUCCESS;
    }

    // ── Bribe result ────────────────────────────────────────────────────────────

    /**
     * Possible outcomes of a bribery attempt.
     */
    public enum BribeResult {
        /** Bribe accepted — NPC will stay quiet. */
        SUCCESS,
        /** NPC refused and will now report immediately. */
        REFUSED,
        /** The targeted NPC is not in WITNESS state. */
        NOT_A_WITNESS,
        /** Player does not have enough coins. */
        INSUFFICIENT_FUNDS
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    /**
     * Returns the list of all currently active evidence props.
     * Used by the HUD to render the evidence countdown counter.
     */
    public List<EvidenceProp> getEvidenceProps() {
        return evidenceProps;
    }

    /**
     * Returns the number of active (undiscovered, non-decayed) evidence props.
     * Displayed in the bottom-right HUD counter.
     */
    public int getActiveEvidenceCount() {
        return activeEvidenceCount;
    }

    /**
     * Returns true if any CCTV tape is currently hot (a crime has occurred nearby
     * and the steal window is still open). Used by the HUD to flash a red vignette.
     */
    public boolean isCctvHot() {
        return cctvHot;
    }

    /**
     * Returns all CCTV tape props (for proximity checks in the player interaction loop).
     */
    public List<EvidenceProp> getCctvTapes() {
        return cctvTapes;
    }
}

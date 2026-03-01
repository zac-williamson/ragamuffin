package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1057: Northfield Canal — Gone Fishing, Towpath Economy &amp; The Cut at Night.
 *
 * <p>The Northfield Canal is a 240-block east–west water channel with stone walls,
 * iron-railed towpaths, and a footbridge at midpoint. This system governs all canal
 * gameplay:
 *
 * <h3>Fishing mini-game</h3>
 * <ul>
 *   <li>Player crafts FISHING_ROD (2 WOOD + 1 STRING_ITEM) or finds one.</li>
 *   <li>While within {@link #FISHING_WATER_DISTANCE} blocks of a WATER block and holding
 *       a FISHING_ROD, press E to cast. A bite fires after {@link #BITE_MIN_SECONDS}–
 *       {@link #BITE_MAX_SECONDS} seconds (scaled by time-of-day chance).</li>
 *   <li>Press E again to reel in: yields {@link Material#CANAL_FISH} or (15% chance)
 *       {@link Material#SHOPPING_TROLLEY_GOLD}.</li>
 *   <li>Rod has {@link #ROD_MAX_DURABILITY} durability uses before breaking.</li>
 *   <li>Bite chance varies: dawn 60%, noon 25%, night 10%.</li>
 * </ul>
 *
 * <h3>NPCs</h3>
 * <ul>
 *   <li>Derek ({@link NPCType#CANAL_BOAT_OWNER}, east boat, 07:00–22:00): sells
 *       {@link Material#DINGHY} for {@link #DINGHY_COST_COIN} COIN; gossips.</li>
 *   <li>Maureen ({@link NPCType#CANAL_BOAT_OWNER}, west boat, 09:00–20:00): rewards
 *       {@link #MAUREEN_FISH_REWARD_COIN} COIN per {@link #MAUREEN_FISH_BATCH} fish
 *       traded.</li>
 *   <li>1–3 {@link NPCType#PUBLIC} anglers sit at FISHING_BENCH_PROP dawn–dusk;
 *       pickpocketable.</li>
 *   <li>{@link NPCType#DRUNK} night walker on towpath 22:00–03:00.</li>
 *   <li>PCSO patrols 20:00–06:00 and pursues nighttime swimmers within
 *       {@link #PCSO_SWIM_DETECTION_RANGE} blocks.</li>
 * </ul>
 *
 * <h3>Evidence disposal</h3>
 * <p>Press E near water while holding a disposable evidence item
 * ({@link Material#CCTV_TAPE}, {@link Material#BLOODY_HOODIE}, {@link Material#FAKE_ID}).
 * <ul>
 *   <li>Unwitnessed: item consumed, matching {@link CriminalRecord} entry cleared,
 *       Notoriety −1, {@link RumourType#SUSPICIOUS_PERSON} rumour seeded,
 *       {@link AchievementType#EVIDENCE_IN_THE_CUT} achievement.</li>
 *   <li>Witnessed: {@link WantedSystem} +1 star, {@link CriminalRecord.CrimeType#EVIDENCE_DESTRUCTION}
 *       entry recorded.</li>
 * </ul>
 *
 * <h3>Night swimming</h3>
 * <p>Entering WATER blocks applies warmth drain {@link #SWIM_WARMTH_DRAIN_PER_SECOND}/s
 * and health drain {@link #SWIM_HEALTH_DRAIN_PER_SECOND}/s. Exiting after
 * {@link #SWIM_GRIME_THRESHOLD_SECONDS} seconds applies
 * {@link #GRIME_DEBUFF_DURATION_SECONDS}-second COVERED_IN_GRIME debuff (+20% police
 * suspicion). {@link AchievementType#NIGHT_SWIMMER} if after 20:00. PCSO triggers
 * WantedSystem tier-1 if within {@link #PCSO_SWIM_DETECTION_RANGE} blocks.
 *
 * <h3>Dinghy</h3>
 * <p>Buy from Derek for {@link #DINGHY_COST_COIN} COIN. Equip and press E on water edge
 * to board. While on water: no warmth/health drain; max speed {@link #DINGHY_MAX_SPEED}.
 * Deflates after {@link #DINGHY_MAX_DURATION_SECONDS} seconds or on second E press.
 * Allows unobserved evidence disposal from mid-canal.
 */
public class CanalSystem {

    // ── Fishing constants ──────────────────────────────────────────────────────

    /** Maximum distance from a water block to allow fishing. */
    public static final float FISHING_WATER_DISTANCE = 1.5f;

    /** Minimum seconds until a bite fires after casting. */
    public static final float BITE_MIN_SECONDS = 15f;

    /** Maximum seconds until a bite fires after casting. */
    public static final float BITE_MAX_SECONDS = 45f;

    /** Probability of catching SHOPPING_TROLLEY_GOLD instead of CANAL_FISH. */
    public static final float TROLLEY_CATCH_CHANCE = 0.15f;

    /** Number of uses before the fishing rod breaks. */
    public static final int ROD_MAX_DURABILITY = 10;

    /** Bite chance at dawn (04:00–08:00). */
    public static final float BITE_CHANCE_DAWN = 0.60f;

    /** Bite chance at noon (10:00–16:00). */
    public static final float BITE_CHANCE_NOON = 0.25f;

    /** Bite chance at night (20:00–04:00). */
    public static final float BITE_CHANCE_NIGHT = 0.10f;

    /** Wood required to craft a fishing rod. */
    public static final int CRAFT_ROD_WOOD = 2;

    /** String items required to craft a fishing rod. */
    public static final int CRAFT_ROD_STRING = 1;

    // ── NPC schedule constants ─────────────────────────────────────────────────

    /** Hour Derek (east boat owner) becomes active. */
    public static final float DEREK_ACTIVE_START = 7.0f;

    /** Hour Derek becomes inactive. */
    public static final float DEREK_ACTIVE_END = 22.0f;

    /** Hour Maureen (west boat owner) becomes active. */
    public static final float MAUREEN_ACTIVE_START = 9.0f;

    /** Hour Maureen becomes inactive. */
    public static final float MAUREEN_ACTIVE_END = 20.0f;

    /** Hour the drunk night walker appears on the towpath. */
    public static final float DRUNK_SPAWN_HOUR = 22.0f;

    /** Hour the drunk night walker disappears. */
    public static final float DRUNK_DESPAWN_HOUR = 3.0f;

    /** Hour the PCSO begins towpath patrol. */
    public static final float PCSO_PATROL_START = 20.0f;

    /** Hour the PCSO ends towpath patrol. */
    public static final float PCSO_PATROL_END = 6.0f;

    /** Hour anglers are present (dawn). */
    public static final float ANGLER_ACTIVE_START = 5.0f;

    /** Hour anglers leave (dusk). */
    public static final float ANGLER_ACTIVE_END = 20.0f;

    // ── Dinghy constants ───────────────────────────────────────────────────────

    /** COIN cost to buy a DINGHY from Derek. */
    public static final int DINGHY_COST_COIN = 15;

    /** Maximum seconds a dinghy stays inflated. */
    public static final float DINGHY_MAX_DURATION_SECONDS = 120f;

    /** Maximum movement speed while on a dinghy. */
    public static final float DINGHY_MAX_SPEED = 2.0f;

    // ── Trade / Maureen constants ──────────────────────────────────────────────

    /** Number of fish Maureen buys per trade batch. */
    public static final int MAUREEN_FISH_BATCH = 3;

    /** COIN reward per batch of fish traded to Maureen. */
    public static final int MAUREEN_FISH_REWARD_COIN = 5;

    // ── Night swimming constants ───────────────────────────────────────────────

    /** Warmth drained per second while swimming in the canal. */
    public static final float SWIM_WARMTH_DRAIN_PER_SECOND = 4.0f;

    /** Health drained per second while swimming in the canal. */
    public static final float SWIM_HEALTH_DRAIN_PER_SECOND = 2.0f;

    /** Seconds in water required to apply COVERED_IN_GRIME debuff on exit. */
    public static final float SWIM_GRIME_THRESHOLD_SECONDS = 30f;

    /** Duration (seconds) of the COVERED_IN_GRIME debuff. */
    public static final float GRIME_DEBUFF_DURATION_SECONDS = 120f;

    /** Police suspicion multiplier while COVERED_IN_GRIME is active. */
    public static final float GRIME_POLICE_SUSPICION_BONUS = 0.20f;

    /** Hour after which swimming triggers the NIGHT_SWIMMER achievement. */
    public static final float NIGHT_SWIM_HOUR_THRESHOLD = 20.0f;

    /** PCSO detection range for night swimmers. */
    public static final float PCSO_SWIM_DETECTION_RANGE = 20.0f;

    // ── Evidence disposal constants ────────────────────────────────────────────

    /** Max distance from water block to allow evidence disposal. */
    public static final float EVIDENCE_DISPOSAL_RANGE = 2.0f;

    /** Notoriety reduction on successful unwitnessed evidence disposal. */
    public static final int EVIDENCE_DISPOSAL_NOTORIETY_REDUCTION = 1;

    // ── State fields ──────────────────────────────────────────────────────────

    private final Random random;

    // Fishing state
    private boolean isCasting = false;
    private float biteTimer = 0f;
    private boolean biteReady = false;
    private int rodDurability = ROD_MAX_DURABILITY;

    // Night swimming state
    private boolean isInWater = false;
    private float swimTimer = 0f;
    private boolean grimeDebuffActive = false;
    private float grimeDebuffTimer = 0f;

    // Dinghy state
    private boolean onDinghy = false;
    private float dinghyTimer = 0f;

    // PCSO patrol / spawn management
    private NPC pcsoNpc = null;
    private NPC drunkNpc = null;
    private final List<NPC> anglerNpcs = new ArrayList<>();

    // Derek / Maureen
    private NPC derekNpc = null;
    private NPC maureenNpc = null;

    // ── Callbacks / injected systems ──────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;

    // ── Achievement callback interface ────────────────────────────────────────

    @FunctionalInterface
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    public CanalSystem() {
        this(new Random());
    }

    public CanalSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update: advances bite timer, swim drain, dinghy timer, grime debuff.
     *
     * @param delta               seconds since last frame
     * @param timeSystem          current game time
     * @param player              the player
     * @param inventory           the player's inventory
     * @param playerNearWater     true if the player is standing within fishing distance of water
     * @param playerInWater       true if the player is currently in a WATER block
     * @param npcs                all living NPCs in the world
     * @param achievementCallback callback for achievement unlocks
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Player player,
                       Inventory inventory,
                       boolean playerNearWater,
                       boolean playerInWater,
                       List<NPC> npcs,
                       AchievementCallback achievementCallback) {

        updateFishing(delta);
        updateNightSwim(delta, timeSystem, player, playerInWater, npcs, achievementCallback);
        updateDinghy(delta, playerInWater);
        updateGrimeDebuff(delta);
    }

    // ── Fishing mini-game ─────────────────────────────────────────────────────

    /**
     * Attempt to craft a fishing rod from inventory materials.
     *
     * @param inventory the player's inventory
     * @return true if the rod was crafted and added to inventory
     */
    public boolean craftFishingRod(Inventory inventory) {
        if (inventory.hasItem(Material.WOOD, CRAFT_ROD_WOOD)
                && inventory.hasItem(Material.STRING_ITEM, CRAFT_ROD_STRING)) {
            inventory.removeItem(Material.WOOD, CRAFT_ROD_WOOD);
            inventory.removeItem(Material.STRING_ITEM, CRAFT_ROD_STRING);
            inventory.addItem(Material.FISHING_ROD, 1);
            return true;
        }
        return false;
    }

    /**
     * Get the bite chance for the given time of day.
     *
     * @param hour game hour (0–24)
     * @return probability of a bite (0.0–1.0)
     */
    public float getBiteChanceForHour(float hour) {
        // Dawn: 04:00–08:00
        if (hour >= 4.0f && hour < 8.0f) {
            return BITE_CHANCE_DAWN;
        }
        // Noon: 10:00–16:00
        if (hour >= 10.0f && hour < 16.0f) {
            return BITE_CHANCE_NOON;
        }
        // Night: 20:00–24:00 or 00:00–04:00
        if (hour >= 20.0f || hour < 4.0f) {
            return BITE_CHANCE_NIGHT;
        }
        // Transitional periods: interpolate between dawn/noon or noon/night
        if (hour >= 8.0f && hour < 10.0f) {
            // Transition dawn→noon
            float t = (hour - 8.0f) / 2.0f;
            return BITE_CHANCE_DAWN + t * (BITE_CHANCE_NOON - BITE_CHANCE_DAWN);
        }
        // 16:00–20:00: transition noon→night
        float t = (hour - 16.0f) / 4.0f;
        return BITE_CHANCE_NOON + t * (BITE_CHANCE_NIGHT - BITE_CHANCE_NOON);
    }

    /**
     * Cast the fishing rod. Sets the bite timer based on a random delay.
     *
     * @param timeSystem current game time (used for bite chance)
     * @param inventory  player's inventory (must contain a FISHING_ROD)
     * @return {@link CastResult} describing the outcome
     */
    public CastResult castRod(TimeSystem timeSystem, Inventory inventory) {
        if (!inventory.hasItem(Material.FISHING_ROD)) {
            return CastResult.NO_ROD;
        }
        if (rodDurability <= 0) {
            return CastResult.ROD_BROKEN;
        }
        if (isCasting) {
            return CastResult.ALREADY_CASTING;
        }

        float biteChance = getBiteChanceForHour(timeSystem.getTime());
        // Roll whether a bite will occur; if not, set a very long wait
        if (random.nextFloat() < biteChance) {
            float delay = BITE_MIN_SECONDS
                    + random.nextFloat() * (BITE_MAX_SECONDS - BITE_MIN_SECONDS);
            biteTimer = delay;
        } else {
            // No bite this cast — set timer beyond max so it expires silently
            biteTimer = BITE_MAX_SECONDS + 1f;
        }
        isCasting = true;
        biteReady = false;
        return CastResult.CAST_OK;
    }

    /**
     * Reel in the rod after a bite. Yields a fish or shopping trolley.
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievement unlocks
     * @return {@link ReelResult} describing the outcome
     */
    public ReelResult reelIn(Inventory inventory, AchievementCallback achievementCallback) {
        if (!isCasting) {
            return ReelResult.NOT_CASTING;
        }
        if (!biteReady) {
            return ReelResult.NO_BITE;
        }

        // Use one durability charge
        rodDurability--;
        isCasting = false;
        biteReady = false;

        // Determine catch
        Material caught;
        boolean isTrolley = random.nextFloat() < TROLLEY_CATCH_CHANCE;
        if (isTrolley) {
            caught = Material.SHOPPING_TROLLEY_GOLD;
            inventory.addItem(Material.SHOPPING_TROLLEY_GOLD, 1);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.TROLLEY_FISHERMAN);
            }
        } else {
            caught = Material.CANAL_FISH;
            inventory.addItem(Material.CANAL_FISH, 1);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CANAL_CATCH);
            }
        }

        if (rodDurability <= 0) {
            inventory.removeItem(Material.FISHING_ROD, 1);
            return isTrolley ? ReelResult.TROLLEY_ROD_BROKEN : ReelResult.FISH_ROD_BROKEN;
        }
        return isTrolley ? ReelResult.TROLLEY : ReelResult.FISH;
    }

    private void updateFishing(float delta) {
        if (!isCasting) return;
        if (biteReady) return;

        biteTimer -= delta;
        if (biteTimer <= 0f) {
            biteReady = true;
        }
    }

    // ── Evidence disposal ─────────────────────────────────────────────────────

    /**
     * Attempt to dispose of evidence by pressing E near water.
     *
     * @param inventory           player's inventory
     * @param witnessed           true if a PCSO or police NPC is within detection range
     * @param playerX             player X position (for LKP)
     * @param playerY             player Y position
     * @param playerZ             player Z position
     * @param rumourHolder        an NPC to seed a suspicious activity rumour into (may be null)
     * @param achievementCallback callback for achievement unlocks
     * @param notorietyCallback   callback for notoriety changes
     * @return {@link EvidenceDisposalResult} describing the outcome
     */
    public EvidenceDisposalResult disposeEvidence(Inventory inventory,
                                                   boolean witnessed,
                                                   float playerX, float playerY, float playerZ,
                                                   NPC rumourHolder,
                                                   AchievementCallback achievementCallback,
                                                   NotorietySystem.AchievementCallback notorietyCallback) {
        // Find which disposable evidence item the player holds (priority order)
        Material evidenceItem = findEvidenceItem(inventory);
        if (evidenceItem == null) {
            return EvidenceDisposalResult.NO_EVIDENCE;
        }

        if (witnessed) {
            // Witnessed disposal — add wanted star and record crime
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, playerX, playerY, playerZ, null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.EVIDENCE_DESTRUCTION);
            }
            return EvidenceDisposalResult.WITNESSED;
        }

        // Unwitnessed — consume item, clear matching criminal record entry, reduce notoriety, seed rumour
        inventory.removeItem(evidenceItem, 1);

        // Clear the matching crime record entry
        clearCrimeForEvidence(evidenceItem);

        // Reduce notoriety by 1 (no tier-change achievement callback needed for small reduction)
        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(EVIDENCE_DISPOSAL_NOTORIETY_REDUCTION, null);
        }

        // Seed SUSPICIOUS_PERSON rumour
        if (rumourNetwork != null && rumourHolder != null) {
            Rumour rumour = new Rumour(RumourType.SUSPICIOUS_PERSON,
                    "Someone was chucking stuff in the cut — dodgy if you ask me");
            rumourNetwork.addRumour(rumourHolder, rumour);
        }

        // Award achievement
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.EVIDENCE_IN_THE_CUT);
        }

        return EvidenceDisposalResult.SUCCESS;
    }

    private Material findEvidenceItem(Inventory inventory) {
        if (inventory.hasItem(Material.CCTV_TAPE)) return Material.CCTV_TAPE;
        if (inventory.hasItem(Material.BLOODY_HOODIE)) return Material.BLOODY_HOODIE;
        if (inventory.hasItem(Material.FAKE_ID)) return Material.FAKE_ID;
        return null;
    }

    private void clearCrimeForEvidence(Material evidence) {
        if (criminalRecord == null) return;
        switch (evidence) {
            case CCTV_TAPE:
                criminalRecord.clearOne(CriminalRecord.CrimeType.WITNESSED_CRIMES);
                break;
            case BLOODY_HOODIE:
                criminalRecord.clearOne(CriminalRecord.CrimeType.NPCS_KILLED);
                break;
            case FAKE_ID:
                criminalRecord.clearOne(CriminalRecord.CrimeType.TRESPASSING);
                break;
            default:
                break;
        }
    }

    // ── Night swimming ────────────────────────────────────────────────────────

    private void updateNightSwim(float delta,
                                  TimeSystem timeSystem,
                                  Player player,
                                  boolean playerInWater,
                                  List<NPC> npcs,
                                  AchievementCallback achievementCallback) {
        if (playerInWater && !onDinghy) {
            if (!isInWater) {
                // Just entered water
                isInWater = true;
                swimTimer = 0f;
                // Award night swimmer achievement if after 20:00
                if (timeSystem.getTime() >= NIGHT_SWIM_HOUR_THRESHOLD && achievementCallback != null) {
                    achievementCallback.award(AchievementType.NIGHT_SWIMMER);
                }
            }
            swimTimer += delta;

            // Apply warmth and health drains
            player.drainWarmth(SWIM_WARMTH_DRAIN_PER_SECOND * delta);
            player.damage(SWIM_HEALTH_DRAIN_PER_SECOND * delta);

            // Check if PCSO is nearby — trigger WantedSystem
            checkPcsoSwimDetection(player, npcs);

        } else if (isInWater) {
            // Just exited water
            isInWater = false;
            if (swimTimer >= SWIM_GRIME_THRESHOLD_SECONDS) {
                grimeDebuffActive = true;
                grimeDebuffTimer = GRIME_DEBUFF_DURATION_SECONDS;
            }
            swimTimer = 0f;
        }
    }

    private void checkPcsoSwimDetection(Player player, List<NPC> npcs) {
        if (wantedSystem == null) return;
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() != NPCType.PCSO) continue;
            float dist = npc.getPosition().dst(
                    player.getPosition().x, player.getPosition().y, player.getPosition().z);
            if (dist <= PCSO_SWIM_DETECTION_RANGE) {
                wantedSystem.addWantedStars(1,
                        player.getPosition().x, player.getPosition().y, player.getPosition().z,
                        null);
                break; // One trigger per update is enough
            }
        }
    }

    private void updateGrimeDebuff(float delta) {
        if (!grimeDebuffActive) return;
        grimeDebuffTimer -= delta;
        if (grimeDebuffTimer <= 0f) {
            grimeDebuffActive = false;
            grimeDebuffTimer = 0f;
        }
    }

    // ── Dinghy ────────────────────────────────────────────────────────────────

    /**
     * Attempt to board a dinghy on the water's edge.
     *
     * @param inventory player's inventory (must contain DINGHY)
     * @return true if successfully boarded
     */
    public boolean boardDinghy(Inventory inventory) {
        if (onDinghy) return false;
        if (!inventory.hasItem(Material.DINGHY)) return false;
        onDinghy = true;
        dinghyTimer = DINGHY_MAX_DURATION_SECONDS;
        return true;
    }

    /**
     * Attempt to exit the dinghy (deflate it).
     *
     * @param inventory player's inventory
     * @return true if the dinghy was deflated
     */
    public boolean exitDinghy(Inventory inventory) {
        if (!onDinghy) return false;
        onDinghy = false;
        dinghyTimer = 0f;
        inventory.removeItem(Material.DINGHY, 1);
        return true;
    }

    private void updateDinghy(float delta, boolean playerInWater) {
        if (!onDinghy) return;
        dinghyTimer -= delta;
        if (dinghyTimer <= 0f) {
            onDinghy = false;
            dinghyTimer = 0f;
        }
    }

    // ── NPC interactions ──────────────────────────────────────────────────────

    /**
     * Attempt to buy a DINGHY from Derek.
     *
     * @param inventory  player's inventory (must have enough COIN)
     * @return {@link DerekSaleResult} describing the outcome
     */
    public DerekSaleResult buyDinghyFromDerek(Inventory inventory, TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        if (hour < DEREK_ACTIVE_START || hour >= DEREK_ACTIVE_END) {
            return DerekSaleResult.DEREK_NOT_HERE;
        }
        if (inventory.getItemCount(Material.COIN) < DINGHY_COST_COIN) {
            return DerekSaleResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, DINGHY_COST_COIN);
        inventory.addItem(Material.DINGHY, 1);
        return DerekSaleResult.SUCCESS;
    }

    /**
     * Attempt to trade fish to Maureen for COIN reward.
     * Trades in batches of {@link #MAUREEN_FISH_BATCH} fish.
     *
     * @param inventory  player's inventory (must have enough fish)
     * @param timeSystem current game time (Maureen must be present)
     * @return number of COIN awarded (0 if Maureen is absent or not enough fish)
     */
    public int tradeFishToMaureen(Inventory inventory, TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        if (hour < MAUREEN_ACTIVE_START || hour >= MAUREEN_ACTIVE_END) {
            return 0;
        }
        int fishCount = inventory.getItemCount(Material.CANAL_FISH);
        if (fishCount < MAUREEN_FISH_BATCH) {
            return 0;
        }
        int batches = fishCount / MAUREEN_FISH_BATCH;
        int fishToConsume = batches * MAUREEN_FISH_BATCH;
        int coinReward = batches * MAUREEN_FISH_REWARD_COIN;
        inventory.removeItem(Material.CANAL_FISH, fishToConsume);
        inventory.addItem(Material.COIN, coinReward);
        return coinReward;
    }

    // ── Time-of-day helpers ───────────────────────────────────────────────────

    /**
     * Returns true if the PCSO patrol is active for the given hour.
     * Patrol runs 20:00–06:00 (wraps midnight).
     */
    public boolean isPcsoPatrolActive(float hour) {
        return hour >= PCSO_PATROL_START || hour < PCSO_PATROL_END;
    }

    /**
     * Returns true if the anglers are present for the given hour (dawn–dusk).
     */
    public boolean areAnglersPresent(float hour) {
        return hour >= ANGLER_ACTIVE_START && hour < ANGLER_ACTIVE_END;
    }

    /**
     * Returns true if the drunk night walker is present for the given hour.
     * Present 22:00–03:00 (wraps midnight).
     */
    public boolean isDrunkPresent(float hour) {
        return hour >= DRUNK_SPAWN_HOUR || hour < DRUNK_DESPAWN_HOUR;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns true if the player is currently casting. */
    public boolean isCasting() { return isCasting; }

    /** Returns true if a bite is ready to be reeled in. */
    public boolean isBiteReady() { return biteReady; }

    /** Returns the remaining seconds until a bite fires (only meaningful while casting). */
    public float getBiteTimer() { return biteTimer; }

    /** Returns the current rod durability (uses remaining). */
    public int getRodDurability() { return rodDurability; }

    /** Returns true if the player is currently on a dinghy. */
    public boolean isOnDinghy() { return onDinghy; }

    /** Returns the remaining dinghy inflation time in seconds. */
    public float getDinghyTimer() { return dinghyTimer; }

    /** Returns true if the player is currently in water. */
    public boolean isInWater() { return isInWater; }

    /** Returns how many seconds the player has been in water (0 if not in water). */
    public float getSwimTimer() { return swimTimer; }

    /** Returns true if the COVERED_IN_GRIME debuff is currently active. */
    public boolean isGrimeDebuffActive() { return grimeDebuffActive; }

    /** Returns the remaining duration of the COVERED_IN_GRIME debuff in seconds. */
    public float getGrimeDebuffTimer() { return grimeDebuffTimer; }

    // ── Testing setters ───────────────────────────────────────────────────────

    /** Set rod durability directly for testing. */
    public void setRodDurabilityForTesting(int durability) {
        this.rodDurability = durability;
    }

    /** Force bite ready state for testing. */
    public void setBiteReadyForTesting(boolean biteReady) {
        this.biteReady = biteReady;
        if (biteReady) {
            this.isCasting = true;
        }
    }

    /** Set casting state for testing. */
    public void setCastingForTesting(boolean casting) {
        this.isCasting = casting;
    }

    /** Set grime debuff state for testing. */
    public void setGrimeDebuffForTesting(boolean active, float timer) {
        this.grimeDebuffActive = active;
        this.grimeDebuffTimer = timer;
    }

    /** Set swim timer for testing. */
    public void setSwimTimerForTesting(float seconds) {
        this.swimTimer = seconds;
        this.isInWater = seconds > 0f;
    }

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Outcome of pressing E to cast the fishing rod. */
    public enum CastResult {
        /** Rod successfully cast — waiting for bite. */
        CAST_OK,
        /** Player does not have a FISHING_ROD. */
        NO_ROD,
        /** Rod durability is exhausted. */
        ROD_BROKEN,
        /** Already casting — E will reel in when bite is ready. */
        ALREADY_CASTING
    }

    /** Outcome of pressing E to reel in. */
    public enum ReelResult {
        /** Successfully caught a CANAL_FISH. */
        FISH,
        /** Successfully caught a CANAL_FISH — rod broke on this use. */
        FISH_ROD_BROKEN,
        /** Successfully pulled out a SHOPPING_TROLLEY_GOLD. */
        TROLLEY,
        /** Successfully pulled out a SHOPPING_TROLLEY_GOLD — rod broke on this use. */
        TROLLEY_ROD_BROKEN,
        /** Reeled in too early — no bite yet. */
        NO_BITE,
        /** Not currently casting. */
        NOT_CASTING
    }

    /** Outcome of attempting to dispose of evidence in the canal. */
    public enum EvidenceDisposalResult {
        /** Evidence successfully disposed of, unwitnessed. */
        SUCCESS,
        /** Disposal was witnessed — wanted level increased. */
        WITNESSED,
        /** Player does not hold a disposable evidence item. */
        NO_EVIDENCE
    }

    /** Outcome of attempting to buy a DINGHY from Derek. */
    public enum DerekSaleResult {
        /** DINGHY purchased successfully. */
        SUCCESS,
        /** Derek is not on his boat at this hour. */
        DEREK_NOT_HERE,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS
    }
}

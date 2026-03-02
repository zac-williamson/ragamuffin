package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1289: Northfield Meredith &amp; Sons Funeral Parlour — Pre-Need Arrangements,
 * the Viewing Room Racket &amp; the Hearse Hustle.
 *
 * <p>Manages:
 * <ol>
 *   <li><b>Pre-Need Arrangement</b> — pay Gerald 25 COIN to set {@code PRE_NEED_ARRANGED} flag;
 *       on next death, {@link RespawnSystem} spawns player at parlour instead of police station,
 *       clearing the flag.</li>
 *   <li><b>Flower Stand Sales</b> — buy {@link Material#FUNERAL_FLOWERS},
 *       {@link Material#CONDOLENCES_CARD}, {@link Material#MEMORIAL_CANDLE};
 *       placing flowers on a {@code HEADSTONE_PROP} gives Notoriety −1;
 *       giving a condolences card to a {@code MOURNER} during a procession gives
 *       Community Respect +3 and the {@link AchievementType#COLD_COMFORT} achievement.</li>
 *   <li><b>Casket Viewing Room Theft</b> — press E on {@code CASKET_PROP} (Tue–Fri 10:00–16:00)
 *       to loot personal effects; if Dawn witnesses (not on 12:30–12:45 lunch break):
 *       Notoriety +12, WantedSystem +1, {@code THEFT_FROM_PERSON} CriminalRecord;
 *       unwitnessed: Notoriety +6 only.</li>
 *   <li><b>Hearse Hustle</b> — at {@code STREET_LADS} Respect ≥ 50, enter
 *       {@code HEARSE_PROP} via CarDrivingSystem; top speed 0.6×; police blind window
 *       30 seconds; must return by 17:00 or {@code VEHICLE_TAMPERING} + WantedSystem +2.</li>
 *   <li><b>Gold Teeth Sideline</b> — at {@code STREET_LADS} Respect ≥ 75, Gerald buys
 *       {@link Material#WAR_MEDAL} for 6 COIN; seeds {@link RumourType#GOLD_TEETH_TRADE}
 *       rumour.</li>
 *   <li><b>Fake Pre-Need Fraud</b> — offer fake arrangements to {@code MOURNER} NPCs during
 *       processions; 60% accept (player +10 COIN, {@code FRAUD} CriminalRecord);
 *       40% refuse (NoiseSystem MEDIUM, VICAR HOSTILE).</li>
 * </ol>
 */
public class FuneralParlourSystem {

    // ── Day-of-week constants (dayCount % 7) ──────────────────────────────────
    // Game start = day 1 → day%7: 0=Mon,1=Tue,2=Wed,3=Thu,4=Fri,5=Sat,6=Sun
    private static final int TUESDAY   = 1;
    private static final int WEDNESDAY = 2;
    private static final int THURSDAY  = 3;
    private static final int FRIDAY    = 4;

    // ── Pre-need arrangement constants ────────────────────────────────────────

    /** Cost in COIN to purchase a pre-need arrangement from Gerald. */
    public static final int PRE_NEED_COST = 25;

    /** World X position of the funeral parlour spawn point (used for pre-need respawn). */
    public static final float PARLOUR_SPAWN_X = 45f;

    /** World Y position of the funeral parlour spawn point. */
    public static final float PARLOUR_SPAWN_Y = 1f;

    /** World Z position of the funeral parlour spawn point. */
    public static final float PARLOUR_SPAWN_Z = 55f;

    // ── Flower stand constants ────────────────────────────────────────────────

    /** Cost in COIN for FUNERAL_FLOWERS at the flower stand. */
    public static final int FLOWER_COST = 3;

    /** Cost in COIN for CONDOLENCES_CARD at the flower stand. */
    public static final int CONDOLENCES_CARD_COST = 2;

    /** Cost in COIN for MEMORIAL_CANDLE at the flower stand. */
    public static final int MEMORIAL_CANDLE_COST = 2;

    /** Notoriety reduction when player places flowers on a HEADSTONE_PROP. */
    public static final int FLOWER_HEADSTONE_NOTORIETY = -1;

    // ── Viewing room theft constants ──────────────────────────────────────────

    /** Hour at which the viewing room opens for casket access. */
    public static final float VIEWING_ROOM_OPEN_HOUR = 10.0f;

    /** Hour at which the viewing room closes. */
    public static final float VIEWING_ROOM_CLOSE_HOUR = 16.0f;

    /** Dawn's lunch break start hour (she leaves the viewing room). */
    public static final float DAWN_LUNCH_START = 12.5f;

    /** Dawn's lunch break end hour (she returns to the viewing room). */
    public static final float DAWN_LUNCH_END = 12.75f;

    /** Notoriety gained for witnessed casket theft. */
    public static final int CASKET_THEFT_WITNESSED_NOTORIETY = 12;

    /** WantedSystem severity for witnessed casket theft (results in +1 star). */
    public static final int CASKET_THEFT_WITNESSED_SEVERITY = 1;

    /** Notoriety gained for unwitnessed casket theft. */
    public static final int CASKET_THEFT_UNWITNESSED_NOTORIETY = 6;

    /** Detection range (blocks) within which Dawn witnesses casket theft. */
    public static final float DAWN_WITNESS_RANGE = 15.0f;

    // ── Hearse hustle constants ────────────────────────────────────────────────

    /** Minimum STREET_LADS respect required to borrow the hearse. */
    public static final int HEARSE_MIN_RESPECT = 50;

    /** Top speed multiplier for the hearse (0.6× normal). */
    public static final float HEARSE_SPEED_MULTIPLIER = 0.6f;

    /** Police 'blind window' duration in seconds after entering hearse. */
    public static final float HEARSE_POLICE_BLIND_SECONDS = 30f;

    /** Hour by which the hearse must be returned to avoid vehicle theft charge. */
    public static final float HEARSE_RETURN_DEADLINE_HOUR = 17.0f;

    /** WantedSystem severity penalty for failing to return hearse on time. */
    public static final int HEARSE_OVERDUE_WANTED_SEVERITY = 2;

    // ── Gold teeth sideline constants ─────────────────────────────────────────

    /** Minimum STREET_LADS respect required to access Gerald's gold teeth sideline. */
    public static final int GOLD_TEETH_MIN_RESPECT = 75;

    /** Amount Gerald pays for a WAR_MEDAL (above PawnShop's 5 COIN). */
    public static final int WAR_MEDAL_GERALD_PRICE = 6;

    // ── Fake pre-need fraud constants ─────────────────────────────────────────

    /** Probability (0–1) that a MOURNER accepts a fake pre-need arrangement offer. */
    public static final float FRAUD_ACCEPT_CHANCE = 0.60f;

    /** Amount of COIN player earns per accepted fraud. */
    public static final int FRAUD_COIN_REWARD = 10;

    /** Noise spike level when a MOURNER refuses the fake arrangement. */
    public static final float FRAUD_REFUSE_NOISE = 0.5f;

    // ── Casket loot table weights ─────────────────────────────────────────────
    // Cumulative weights: POCKET_WATCH(30), WAR_MEDAL(55), WEDDING_RING(75),
    //                     OLD_PHOTOGRAPH(90), empty(100)
    private static final int LOOT_WEIGHT_POCKET_WATCH   = 30;
    private static final int LOOT_WEIGHT_WAR_MEDAL      = 55;
    private static final int LOOT_WEIGHT_WEDDING_RING   = 75;
    private static final int LOOT_WEIGHT_OLD_PHOTOGRAPH = 90;
    // > 90 = empty

    // ── In-game days between casket resets ────────────────────────────────────
    public static final int CASKET_RESET_DAYS = 3;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the player has arranged a pre-need funeral with Gerald. */
    private boolean preNeedArranged = false;

    /** Whether the hearse is currently borrowed by the player. */
    private boolean hearseOut = false;

    /** Timer tracking elapsed time the hearse has been out. */
    private float hearseOutTimer = 0f;

    /** Whether the police blind window is currently active. */
    private boolean policeBlindWindowActive = false;

    /** Remaining seconds in the police blind window. */
    private float policeBlindTimer = 0f;

    /** The UNDERTAKER (Gerald) NPC. */
    private NPC geraldNpc = null;

    /** The FUNERAL_ASSISTANT (Dawn) NPC. */
    private NPC dawnNpc = null;

    /** Whether Gerald has been spawned this session. */
    private boolean geraldSpawned = false;

    /** Whether Dawn has been spawned this session. */
    private boolean dawnSpawned = false;

    /** Last day the casket was looted (for reset tracking). */
    private int casketLastLootedDay = -99;

    // ── Injected systems ──────────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private NoiseSystem noiseSystem;
    private RumourNetwork rumourNetwork;
    private FactionSystem factionSystem;
    private RespawnSystem respawnSystem;

    // ── Callback ──────────────────────────────────────────────────────────────

    /**
     * Callback for awarding achievements.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    private final Random random;

    public FuneralParlourSystem() {
        this(new Random());
    }

    public FuneralParlourSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setNoiseSystem(NoiseSystem noiseSystem) {
        this.noiseSystem = noiseSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setRespawnSystem(RespawnSystem respawnSystem) {
        this.respawnSystem = respawnSystem;
    }

    // ── Schedule queries ──────────────────────────────────────────────────────

    /**
     * Returns true if the viewing room is accessible for casket interaction on this day/hour.
     * Only open Tue–Fri 10:00–16:00.
     */
    public boolean isViewingRoomOpen(int dayCount, float hour) {
        int dow = dayCount % 7;
        boolean weekday = (dow == TUESDAY || dow == WEDNESDAY || dow == THURSDAY || dow == FRIDAY);
        return weekday && hour >= VIEWING_ROOM_OPEN_HOUR && hour < VIEWING_ROOM_CLOSE_HOUR;
    }

    /**
     * Returns true if Dawn is currently on her lunch break (12:30–12:45).
     * During this window she is not in the viewing room and cannot witness theft.
     */
    public boolean isDawnOnLunchBreak(float hour) {
        return hour >= DAWN_LUNCH_START && hour < DAWN_LUNCH_END;
    }

    /**
     * Returns true if Dawn can witness casket theft (viewing room open, not on lunch break).
     */
    public boolean canDawnWitness(int dayCount, float hour) {
        return isViewingRoomOpen(dayCount, hour) && !isDawnOnLunchBreak(hour);
    }

    /**
     * Returns true if the casket has been looted too recently to contain items.
     */
    public boolean isCasketEmpty(int dayCount) {
        return (dayCount - casketLastLootedDay) < CASKET_RESET_DAYS;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the funeral parlour system each frame.
     *
     * @param delta               seconds since last frame
     * @param timeSystem          the TimeSystem
     * @param npcs                all living NPCs
     * @param achievementCallback callback for awarding achievements (may be null)
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> npcs,
                       AchievementCallback achievementCallback) {
        float hour     = timeSystem.getTime();
        int   dayCount = timeSystem.getDayCount();

        // Manage Gerald and Dawn NPCs
        manageStaff(npcs);

        // Update hearse hustle state
        if (hearseOut) {
            hearseOutTimer += delta;
            if (policeBlindWindowActive) {
                policeBlindTimer -= delta;
                if (policeBlindTimer <= 0f) {
                    policeBlindWindowActive = false;
                    policeBlindTimer = 0f;
                }
            }
            // Check if hearse is overdue
            if (hour >= HEARSE_RETURN_DEADLINE_HOUR) {
                onHearseOverdue(npcs);
            }
        }
    }

    // ── Staff management ──────────────────────────────────────────────────────

    private void manageStaff(List<NPC> npcs) {
        if (!geraldSpawned) {
            geraldNpc = new NPC(NPCType.UNDERTAKER, "Gerald",
                    PARLOUR_SPAWN_X, PARLOUR_SPAWN_Y, PARLOUR_SPAWN_Z + 2f);
            geraldNpc.setState(NPCState.IDLE);
            geraldSpawned = true;
            npcs.add(geraldNpc);
        }
        if (!dawnSpawned) {
            dawnNpc = new NPC(NPCType.FUNERAL_ASSISTANT, "Dawn",
                    PARLOUR_SPAWN_X + 3f, PARLOUR_SPAWN_Y, PARLOUR_SPAWN_Z);
            dawnNpc.setState(NPCState.WANDERING);
            dawnSpawned = true;
            npcs.add(dawnNpc);
        }
    }

    // ── Pre-need arrangement ──────────────────────────────────────────────────

    /**
     * Player pays Gerald for a pre-need arrangement.
     *
     * <p>Costs {@link #PRE_NEED_COST} COIN. If successful, sets the pre-need flag
     * so the next respawn places the player at the funeral parlour, and grants the
     * {@link AchievementType#PLAN_AHEAD} achievement.
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievements
     * @return true if the arrangement was successfully purchased
     */
    public boolean purchasePreNeedArrangement(Inventory inventory,
                                               AchievementCallback achievementCallback) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < PRE_NEED_COST) return false;
        if (preNeedArranged) return false; // already arranged

        inventory.removeItem(Material.COIN, PRE_NEED_COST);
        preNeedArranged = true;

        // Update respawn system to spawn at parlour instead of park centre
        if (respawnSystem != null) {
            respawnSystem.setSpawnY(PARLOUR_SPAWN_Y);
            // The respawn location is controlled via a custom override flag
            respawnSystem.setParlourRespawn(PARLOUR_SPAWN_X, PARLOUR_SPAWN_Y, PARLOUR_SPAWN_Z);
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.PLAN_AHEAD);
        }

        return true;
    }

    /**
     * Called by RespawnSystem after the player respawns at the parlour.
     * Clears the pre-need flag so it only fires once.
     */
    public void onParlourRespawnUsed() {
        preNeedArranged = false;
    }

    // ── Flower stand sales ────────────────────────────────────────────────────

    /**
     * Player buys FUNERAL_FLOWERS from the flower stand.
     *
     * @param inventory player's inventory
     * @return true if purchase succeeded
     */
    public boolean buyFuneralFlowers(Inventory inventory) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < FLOWER_COST) return false;
        inventory.removeItem(Material.COIN, FLOWER_COST);
        inventory.addItem(Material.FUNERAL_FLOWERS, 1);
        return true;
    }

    /**
     * Player buys a CONDOLENCES_CARD from the flower stand.
     *
     * @param inventory player's inventory
     * @return true if purchase succeeded
     */
    public boolean buyCondolencesCard(Inventory inventory) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < CONDOLENCES_CARD_COST) return false;
        inventory.removeItem(Material.COIN, CONDOLENCES_CARD_COST);
        inventory.addItem(Material.CONDOLENCES_CARD, 1);
        return true;
    }

    /**
     * Player buys a MEMORIAL_CANDLE from the flower stand.
     *
     * @param inventory player's inventory
     * @return true if purchase succeeded
     */
    public boolean buyMemorialCandle(Inventory inventory) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < MEMORIAL_CANDLE_COST) return false;
        inventory.removeItem(Material.COIN, MEMORIAL_CANDLE_COST);
        inventory.addItem(Material.MEMORIAL_CANDLE, 1);
        return true;
    }

    /**
     * Player places FUNERAL_FLOWERS on a HEADSTONE_PROP.
     * Requires one FUNERAL_FLOWERS in inventory. Gives Notoriety −1.
     *
     * @param inventory           player's inventory
     * @param achievementCallback callback for achievements
     * @return true if flowers were placed
     */
    public boolean placeFlowersOnHeadstone(Inventory inventory,
                                            AchievementCallback achievementCallback) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.FUNERAL_FLOWERS) < 1) return false;
        inventory.removeItem(Material.FUNERAL_FLOWERS, 1);

        if (notorietySystem != null) {
            // Negative = reduce notoriety (use reduceNotoriety with positive amount)
            notorietySystem.reduceNotoriety(-FLOWER_HEADSTONE_NOTORIETY,
                    achievementCallback != null ? achievementCallback::award : null);
        }
        return true;
    }

    /**
     * Player gives a CONDOLENCES_CARD to a MOURNER NPC during a funeral procession.
     * Removes one CONDOLENCES_CARD from inventory, gives Community Respect +3 (via NotorietySystem
     * reduction) and awards the {@link AchievementType#COLD_COMFORT} achievement.
     *
     * @param inventory           player's inventory
     * @param mournerNpcs         list of active mourner NPCs (procession must be active)
     * @param achievementCallback callback for achievements
     * @return true if the card was given
     */
    public boolean giveCondolencesCardToMourner(Inventory inventory, List<NPC> mournerNpcs,
                                                  AchievementCallback achievementCallback) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.CONDOLENCES_CARD) < 1) return false;
        if (mournerNpcs == null || mournerNpcs.isEmpty()) return false;

        inventory.removeItem(Material.CONDOLENCES_CARD, 1);

        // Community Respect +3 — modelled as notoriety reduction
        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(3,
                    achievementCallback != null ? achievementCallback::award : null);
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.COLD_COMFORT);
        }
        return true;
    }

    // ── Casket viewing room theft ─────────────────────────────────────────────

    /**
     * Player presses E on a CASKET_PROP to loot personal effects.
     *
     * <p>Only available Tue–Fri 10:00–16:00. If Dawn is present (not on lunch),
     * she witnesses the theft: Notoriety +12, WantedSystem +1, THEFT_FROM_PERSON recorded,
     * and seeds {@link RumourType#FUNERAL_THIEF}. Unwitnessed: Notoriety +6 only.
     *
     * @param playerX             player world X
     * @param playerY             player world Y
     * @param playerZ             player world Z
     * @param dayCount            current in-game day
     * @param hour                current in-game hour
     * @param inventory           player's inventory (loot added here)
     * @param npcs                all living NPCs
     * @param achievementCallback callback for achievements
     * @return true if looting occurred
     */
    public boolean onCasketInteract(float playerX, float playerY, float playerZ,
                                     int dayCount, float hour, Inventory inventory,
                                     List<NPC> npcs, AchievementCallback achievementCallback) {
        if (!isViewingRoomOpen(dayCount, hour)) return false;
        if (isCasketEmpty(dayCount)) return false;

        casketLastLootedDay = dayCount;

        boolean witnessed = canDawnWitness(dayCount, hour)
                && isDawnNearby(playerX, playerY, playerZ);

        if (witnessed) {
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CASKET_THEFT_WITNESSED_NOTORIETY,
                        achievementCallback != null ? achievementCallback::award : null);
            }
            if (wantedSystem != null) {
                wantedSystem.onCrimeWitnessed(CASKET_THEFT_WITNESSED_SEVERITY,
                        playerX, playerY, playerZ, null, null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.THEFT_FROM_PERSON);
            }
            // Seed FUNERAL_THIEF rumour
            seedRumour(npcs, RumourType.FUNERAL_THIEF,
                    "Someone's been nicking things out of the viewing room at the funeral parlour.");
        } else {
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CASKET_THEFT_UNWITNESSED_NOTORIETY,
                        achievementCallback != null ? achievementCallback::award : null);
            }
        }

        // Roll casket loot
        if (inventory != null) {
            Material loot = rollCasketLoot();
            if (loot != null) {
                inventory.addItem(loot, 1);
            }
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.GRAVE_GOODS_COLLECTOR);
        }

        return true;
    }

    // ── Hearse hustle ─────────────────────────────────────────────────────────

    /**
     * Player attempts to borrow the hearse (HEARSE_PROP).
     * Requires STREET_LADS Respect ≥ {@link #HEARSE_MIN_RESPECT}.
     * Starts the police blind window ({@link #HEARSE_POLICE_BLIND_SECONDS} seconds).
     *
     * @param achievementCallback callback for achievements
     * @return true if the hearse was successfully entered
     */
    public boolean tryBorrowHearse(AchievementCallback achievementCallback) {
        if (hearseOut) return false; // already out

        int streetLadsRespect = getStreetLadsRespect();
        if (streetLadsRespect < HEARSE_MIN_RESPECT) return false;

        hearseOut = true;
        hearseOutTimer = 0f;
        policeBlindWindowActive = true;
        policeBlindTimer = HEARSE_POLICE_BLIND_SECONDS;
        return true;
    }

    /**
     * Player returns the hearse before the deadline.
     * Awards the {@link AchievementType#DEAD_DELIVERY} achievement.
     *
     * @param hour                current in-game hour
     * @param achievementCallback callback for achievements
     * @return true if the hearse was returned on time
     */
    public boolean returnHearse(float hour, AchievementCallback achievementCallback) {
        if (!hearseOut) return false;
        if (hour >= HEARSE_RETURN_DEADLINE_HOUR) {
            // Too late — penalise
            onHearseOverdue(null);
            return false;
        }

        hearseOut = false;
        hearseOutTimer = 0f;
        policeBlindWindowActive = false;
        policeBlindTimer = 0f;

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.DEAD_DELIVERY);
        }
        return true;
    }

    /**
     * Called when the hearse is not returned by 17:00.
     * Records VEHICLE_TAMPERING and adds 2 wanted stars.
     */
    private void onHearseOverdue(List<NPC> npcs) {
        if (!hearseOut) return;
        hearseOut = false;
        hearseOutTimer = 0f;
        policeBlindWindowActive = false;
        policeBlindTimer = 0f;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.VEHICLE_TAMPERING);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(HEARSE_OVERDUE_WANTED_SEVERITY,
                    PARLOUR_SPAWN_X, PARLOUR_SPAWN_Y, PARLOUR_SPAWN_Z, null);
        }
    }

    // ── Gold teeth sideline ───────────────────────────────────────────────────

    /**
     * Player sells a WAR_MEDAL to Gerald.
     * Requires STREET_LADS Respect ≥ {@link #GOLD_TEETH_MIN_RESPECT}.
     * Gerald pays {@link #WAR_MEDAL_GERALD_PRICE} COIN (above PawnShop's 5).
     * Seeds {@link RumourType#GOLD_TEETH_TRADE} rumour. Awards
     * {@link AchievementType#GERALD_S_REGULAR} achievement.
     *
     * @param inventory           player's inventory
     * @param npcs                all living NPCs (for rumour seeding)
     * @param achievementCallback callback for achievements
     * @return true if the sale succeeded
     */
    public boolean sellWarMedalToGerald(Inventory inventory, List<NPC> npcs,
                                          AchievementCallback achievementCallback) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.WAR_MEDAL) < 1) return false;

        int streetLadsRespect = getStreetLadsRespect();
        if (streetLadsRespect < GOLD_TEETH_MIN_RESPECT) return false;

        inventory.removeItem(Material.WAR_MEDAL, 1);
        inventory.addItem(Material.COIN, WAR_MEDAL_GERALD_PRICE);

        // Seed GOLD_TEETH_TRADE rumour
        seedRumour(npcs, RumourType.GOLD_TEETH_TRADE,
                "Gerald at the funeral parlour's been paying good money for old war medals.");

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.GERALD_S_REGULAR);
        }
        return true;
    }

    // ── Fake pre-need fraud ───────────────────────────────────────────────────

    /**
     * Player offers a fake pre-need arrangement to a MOURNER NPC during a procession.
     *
     * <p>60% chance the mourner accepts (+10 COIN, FRAUD recorded). 40% chance refusal
     * (NoiseSystem MEDIUM spike, VICAR becomes HOSTILE towards player).
     *
     * @param inventory           player's inventory
     * @param mournerNpcs         active mourners (procession must be happening)
     * @param allNpcs             all living NPCs (to find VICAR and set hostile)
     * @param achievementCallback callback for achievements
     * @return true if the fraud was attempted (regardless of outcome)
     */
    public boolean offerFakePreneedFraud(Inventory inventory, List<NPC> mournerNpcs,
                                           List<NPC> allNpcs,
                                           AchievementCallback achievementCallback) {
        if (mournerNpcs == null || mournerNpcs.isEmpty()) return false;

        boolean accepted = random.nextFloat() < FRAUD_ACCEPT_CHANCE;

        if (accepted) {
            if (inventory != null) {
                inventory.addItem(Material.COIN, FRAUD_COIN_REWARD);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.FRAUD);
            }
        } else {
            // Mourner refused — noise spike, vicar becomes hostile
            if (noiseSystem != null) {
                noiseSystem.addNoise(FRAUD_REFUSE_NOISE);
            }
            if (allNpcs != null) {
                for (NPC npc : allNpcs) {
                    if (npc.getType() == NPCType.VICAR) {
                        npc.setState(NPCState.AGGRESSIVE);
                    }
                }
            }
        }

        return true;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns true if Dawn NPC is within {@link #DAWN_WITNESS_RANGE} of the player.
     */
    private boolean isDawnNearby(float px, float py, float pz) {
        if (dawnNpc == null) return false;
        float dx = dawnNpc.getPosition().x - px;
        float dy = dawnNpc.getPosition().y - py;
        float dz = dawnNpc.getPosition().z - pz;
        float distSq = dx * dx + dy * dy + dz * dz;
        return distSq <= DAWN_WITNESS_RANGE * DAWN_WITNESS_RANGE;
    }

    /**
     * Roll the casket loot table and return the dropped material, or null for no loot.
     */
    Material rollCasketLoot() {
        int roll = random.nextInt(100);
        if (roll < LOOT_WEIGHT_POCKET_WATCH) {
            return Material.POCKET_WATCH;
        } else if (roll < LOOT_WEIGHT_WAR_MEDAL) {
            return Material.WAR_MEDAL;
        } else if (roll < LOOT_WEIGHT_WEDDING_RING) {
            return Material.WEDDING_RING;
        } else if (roll < LOOT_WEIGHT_OLD_PHOTOGRAPH) {
            return Material.OLD_PHOTOGRAPH;
        }
        return null; // empty casket
    }

    /**
     * Seeds a rumour into the first non-police NPC in the list.
     */
    private void seedRumour(List<NPC> npcs, RumourType type, String text) {
        if (rumourNetwork == null || npcs == null) return;
        for (NPC npc : npcs) {
            if (npc.getType() != NPCType.POLICE) {
                rumourNetwork.addRumour(npc, new Rumour(type, text));
                break;
            }
        }
    }

    /**
     * Returns the current STREET_LADS respect from the faction system, or 0 if unavailable.
     */
    private int getStreetLadsRespect() {
        if (factionSystem == null) return 0;
        return factionSystem.getRespect(Faction.STREET_LADS);
    }

    // ── Getters (for testing) ─────────────────────────────────────────────────

    public boolean isPreNeedArranged() {
        return preNeedArranged;
    }

    public boolean isHearseOut() {
        return hearseOut;
    }

    public float getHearseOutTimer() {
        return hearseOutTimer;
    }

    public boolean isPoliceBlindWindowActive() {
        return policeBlindWindowActive;
    }

    public float getPoliceBlindTimer() {
        return policeBlindTimer;
    }

    public NPC getGeraldNpc() {
        return geraldNpc;
    }

    public NPC getDawnNpc() {
        return dawnNpc;
    }

    public boolean isGeraldSpawned() {
        return geraldSpawned;
    }

    public boolean isDawnSpawned() {
        return dawnSpawned;
    }

    public int getCasketLastLootedDay() {
        return casketLastLootedDay;
    }

    // ── Setters for testing ───────────────────────────────────────────────────

    void setPreNeedArrangedForTesting(boolean arranged) {
        this.preNeedArranged = arranged;
    }

    void setHearseOutForTesting(boolean out, float timer) {
        this.hearseOut = out;
        this.hearseOutTimer = timer;
    }

    void setCasketLastLootedDayForTesting(int day) {
        this.casketLastLootedDay = day;
    }

    void setGeraldNpcForTesting(NPC npc) {
        this.geraldNpc = npc;
    }

    void setDawnNpcForTesting(NPC npc) {
        this.dawnNpc = npc;
    }
}

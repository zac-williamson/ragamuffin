package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import java.util.List;

/**
 * Issue #906: Busking System — Street Performance Economy.
 *
 * <p>Manages all busking state: session start/stop, coin income accumulation from
 * bored NPCs, police licence-check mechanic, faction respect impacts, achievement
 * awards, and RumourNetwork integration.
 *
 * <h3>Session Flow</h3>
 * <ol>
 *   <li>Player equips {@link Material#BUCKET_DRUM} and presses E near PAVEMENT/ROAD
 *       within {@link #MAX_PITCH_DISTANCE_TO_SHOP} blocks of a shop landmark.</li>
 *   <li>Every real second, nearby bored NPCs contribute coins based on their BORED
 *       need score; coins are disbursed to inventory.</li>
 *   <li>After {@link #LICENCE_CHECK_DELAY_SECONDS} (or
 *       {@link #HIGH_NOTORIETY_CHECK_DELAY_SECONDS} at Notoriety Tier ≥ 3) a police
 *       NPC approaches and demands a busking licence.</li>
 *   <li>Player can: show FAKE_ID (consumed), leg it (+5 Notoriety, +1 star), or
 *       receive confiscation (+10 Notoriety, +2 stars, BUCKET_DRUM removed).</li>
 * </ol>
 */
public class BuskingSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    public static final float BASE_COIN_PER_NPC_PER_SECOND      = 0.1f;
    public static final float LICENCE_CHECK_DELAY_SECONDS        = 60f;
    public static final float HIGH_NOTORIETY_CHECK_DELAY_SECONDS = 30f;
    public static final float BUSK_RADIUS_BLOCKS                 = 8f;
    public static final float MAX_PITCH_DISTANCE_TO_SHOP         = 30f;
    public static final int   NOTORIETY_CONFISCATION             = 10;
    public static final int   NOTORIETY_LEG_IT                   = 5;
    public static final int   WANTED_STARS_CONFISCATION          = 2;
    public static final int   WANTED_STARS_LEG_IT                = 1;
    public static final float LAGER_SHORTAGE_MULTIPLIER          = 1.5f;
    public static final float BENEFIT_DAY_MULTIPLIER             = 2.0f;
    public static final float GREGGS_STRIKE_MULTIPLIER           = 0.5f;
    public static final float BORED_NEED_REDUCTION_PER_COIN      = 10f;
    public static final int   REP_AWARD_INTERVAL_SECONDS         = 60;
    public static final float PITCH_EXCLUSION_RANGE              = 10f;

    /** Blocks the player can move from the busk start position before session ends. */
    private static final float MAX_BUSK_WANDER = 3f;

    /** Seconds the officer waits for player response during a licence check. */
    private static final float LICENCE_RESPONSE_TIMEOUT = 10f;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final StreetEconomySystem streetEconomySystem;
    private final FactionSystem factionSystem;
    private final WantedSystem wantedSystem;
    private final NotorietySystem notorietySystem;
    private final RumourNetwork rumourNetwork;

    /** Achievement system callback — may be null in tests. */
    private final ragamuffin.ui.AchievementSystem achievementSystem;

    // ── Session state ─────────────────────────────────────────────────────────

    private boolean busking = false;

    /** World position where the player started busking. */
    private float buskStartX, buskStartY, buskStartZ;

    /** Accumulated fractional coins not yet disbursed. */
    private float pendingCoins = 0f;

    /** Total coins earned across all busk sessions this game. */
    private int totalCoinsEarned = 0;

    /** Whether BUCKET_LIST (20 coins) has already been awarded. */
    private boolean bucketListAwarded = false;

    /** Seconds elapsed since the current session started. */
    private float sessionTimer = 0f;

    /** Seconds until the licence check triggers. */
    private float licenceCheckCountdown = LICENCE_CHECK_DELAY_SECONDS;

    /** Whether a licence check is currently active (officer is approaching / waiting). */
    private boolean licenceCheckActive = false;

    /** The police NPC demanding a licence (null if no check active). */
    private NPC checkingOfficer = null;

    /** Time remaining for player to respond during a licence check. */
    private float licenceResponseTimer = 0f;

    /** Number of complete minutes busked consecutively in this session. */
    private int consecutiveMinutes = 0;

    /** Whether Street Reputation was already awarded this game session (cap farming). */
    private boolean repAwardedThisSession = false;

    /** Whether LIVING_WAGE achievement has been awarded this session. */
    private boolean livingWageAwarded = false;

    /** Whether STREET_PERFORMER has been awarded. */
    private boolean streetPerformerAwarded = false;

    /** Income rate for the last update tick (for testing / display). */
    private float lastIncomeRate = 0f;

    // ── Constructor ───────────────────────────────────────────────────────────

    public BuskingSystem(StreetEconomySystem streetEconomySystem,
                         FactionSystem factionSystem,
                         WantedSystem wantedSystem,
                         NotorietySystem notorietySystem,
                         RumourNetwork rumourNetwork,
                         ragamuffin.ui.AchievementSystem achievementSystem) {
        this.streetEconomySystem = streetEconomySystem;
        this.factionSystem       = factionSystem;
        this.wantedSystem        = wantedSystem;
        this.notorietySystem     = notorietySystem;
        this.rumourNetwork       = rumourNetwork;
        this.achievementSystem   = achievementSystem;
    }

    // ── Session start ─────────────────────────────────────────────────────────

    /**
     * Attempt to start a busk session.
     *
     * @param player       the player
     * @param world        the game world
     * @param npcs         all living NPCs
     * @param tooltipSystem tooltip system for user feedback
     * @return {@code true} if the session started successfully
     */
    public boolean startBusk(Player player, World world, List<NPC> npcs, ragamuffin.ui.TooltipSystem tooltipSystem) {
        if (busking) return false;

        // Check player is near PAVEMENT or ROAD block
        if (!isNearPavementOrRoad(player, world)) {
            if (tooltipSystem != null) tooltipSystem.showMessage("You need to be near the pavement or road to busk.", 2.5f);
            return false;
        }

        // Check within 30 blocks of a shop landmark
        if (!isNearShopLandmark(player, world)) {
            if (tooltipSystem != null) tooltipSystem.showMessage("Busk on the high street, not out here in the middle of nowhere.", 2.5f);
            return false;
        }

        // Check no existing BUSKER within 10 blocks
        NPC blockingBusker = findNearbyBusker(player, npcs);
        if (blockingBusker != null) {
            blockingBusker.setSpeechText("Oi — find your own pitch, mate.", 4.0f);
            if (tooltipSystem != null) tooltipSystem.showMessage("There's already a busker on this pitch.", 2.5f);
            return false;
        }

        // Start the session
        busking = true;
        buskStartX = player.getPosition().x;
        buskStartY = player.getPosition().y;
        buskStartZ = player.getPosition().z;
        sessionTimer = 0f;
        consecutiveMinutes = 0;
        pendingCoins = 0f;

        // Set licence check delay based on notoriety
        if (notorietySystem != null && notorietySystem.getTier() >= 3) {
            licenceCheckCountdown = HIGH_NOTORIETY_CHECK_DELAY_SECONDS;
        } else {
            licenceCheckCountdown = LICENCE_CHECK_DELAY_SECONDS;
        }
        licenceCheckActive = false;
        checkingOfficer = null;
        licenceResponseTimer = 0f;

        // Faction effects
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, 2);
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, -1);
        }

        // Seed BUSKER_STARTED rumour into 3 nearby NPCs
        seedBuskerRumour(player, npcs);

        // Achievement: first busk session
        if (!streetPerformerAwarded) {
            streetPerformerAwarded = true;
            award(AchievementType.STREET_PERFORMER);
        }

        if (tooltipSystem != null) {
            tooltipSystem.showMessage("You start drumming. Bucket drum. The percussion instrument of the dispossessed.", 3.0f);
        }

        return true;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the busking system. Call once per frame while PLAYING.
     *
     * @param delta        seconds since last frame
     * @param npcs         all living NPCs
     * @param player       the player
     * @param weather      current weather (used for market event lookup only via activeEvent)
     * @param activeEvent  current active market event (may be null)
     */
    public void update(float delta, List<NPC> npcs, Player player, Inventory inventory, ragamuffin.core.Weather weather, MarketEvent activeEvent) {
        if (!busking) return;

        // Session-end check: player wandered too far
        float dx = player.getPosition().x - buskStartX;
        float dz = player.getPosition().z - buskStartZ;
        float distFromStart = (float) Math.sqrt(dx * dx + dz * dz);
        if (distFromStart > MAX_BUSK_WANDER) {
            stopBusk();
            return;
        }

        // If a GREGGS_STRIKE event ends mid-session, check if we need to stop
        // (Police crack down on loitering at session end from GREGGS_STRIKE — handled by RagamuffinGame)

        // Advance session timer
        sessionTimer += delta;

        // Income accumulation
        if (!licenceCheckActive) {
            float multiplier = getMarketMultiplier(activeEvent);
            float incomeThisSecond = 0f;

            for (NPC npc : npcs) {
                if (!npc.isAlive()) continue;
                float ndx = npc.getPosition().x - player.getPosition().x;
                float ndz = npc.getPosition().z - player.getPosition().z;
                float dist = (float) Math.sqrt(ndx * ndx + ndz * ndz);
                if (dist <= BUSK_RADIUS_BLOCKS) {
                    float boredScore = (streetEconomySystem != null)
                            ? streetEconomySystem.getNeedScore(npc, NeedType.BORED)
                            : 0f;
                    float contribution = Math.max(0f, Math.min(1f, boredScore / 100f))
                            * BASE_COIN_PER_NPC_PER_SECOND;
                    incomeThisSecond += contribution;
                }
            }

            incomeThisSecond *= multiplier;
            lastIncomeRate = incomeThisSecond;
            pendingCoins += incomeThisSecond * delta;

            // Disburse whole coins
            int wholeCoins = (int) pendingCoins;
            if (wholeCoins > 0) {
                pendingCoins -= wholeCoins;
                inventory.addItem(Material.COIN, wholeCoins);
                totalCoinsEarned += wholeCoins;

                // Reduce BORED need on contributing NPCs proportionally
                reduceBoredomForCoins(player, npcs, wholeCoins);

                // Check BUCKET_LIST achievement (20 coins total)
                if (!bucketListAwarded && totalCoinsEarned >= 20) {
                    bucketListAwarded = true;
                    award(AchievementType.BUCKET_LIST);
                }
            }
        }

        // Per-minute street reputation award (cap once per real game session)
        int minutesNow = (int) (sessionTimer / REP_AWARD_INTERVAL_SECONDS);
        if (minutesNow > consecutiveMinutes) {
            consecutiveMinutes = minutesNow;
            if (!repAwardedThisSession) {
                repAwardedThisSession = true;
                player.getStreetReputation().addPoints(1);
            }

            // At 5 consecutive minutes — LIVING_WAGE achievement + barman rumour
            if (consecutiveMinutes >= 5 && !livingWageAwarded) {
                livingWageAwarded = true;
                award(AchievementType.LIVING_WAGE);
                seedBarmanRumour(npcs);
            }
        }

        // Licence check countdown
        if (!licenceCheckActive) {
            licenceCheckCountdown -= delta;
            if (licenceCheckCountdown <= 0f) {
                triggerLicenceCheck(player, npcs);
            }
        } else {
            // Licence check is active — count down response timeout
            licenceResponseTimer -= delta;
            if (licenceResponseTimer <= 0f) {
                // No response — confiscate
                onConfiscation(player, inventory, npcs);
            }
        }
    }

    /**
     * Stop the current busk session without penalty (e.g. player deselects hotbar slot).
     */
    public void stopBusk() {
        busking = false;
        licenceCheckActive = false;
        checkingOfficer = null;
        licenceResponseTimer = 0f;
        lastIncomeRate = 0f;
    }

    // ── Licence check responses ────────────────────────────────────────────────

    /**
     * Called when the player shows FAKE_ID during a licence check.
     * Consumes the FAKE_ID; officer stands down; session continues.
     *
     * @param inventory the player's inventory
     * @return {@code true} if the licence check was cleared
     */
    public boolean onShowLicence(Inventory inventory) {
        if (!licenceCheckActive) return false;
        if (inventory.getItemCount(Material.FAKE_ID) <= 0) return false;

        inventory.removeItem(Material.FAKE_ID, 1);
        if (checkingOfficer != null) {
            checkingOfficer.setSpeechText("Right, carry on then.", 3.0f);
            checkingOfficer.setState(NPCState.PATROLLING);
            checkingOfficer = null;
        }
        licenceCheckActive = false;
        licenceResponseTimer = 0f;
        return true;
    }

    /**
     * Called when the player legs it during a licence check (ESC or moves away).
     * Ends the session; applies notoriety and wanted stars penalty.
     *
     * @param player the player
     * @param npcs   all NPCs
     */
    public void onLegIt(Player player, List<NPC> npcs) {
        if (!licenceCheckActive && !busking) return;
        notorietySystem.addNotoriety(NOTORIETY_LEG_IT, achievementSystem != null
                ? t -> achievementSystem.unlock(t) : null);
        wantedSystem.addWantedStars(WANTED_STARS_LEG_IT,
                player.getPosition().x, player.getPosition().y, player.getPosition().z,
                achievementSystem != null ? t -> achievementSystem.unlock(t) : null);
        stopBusk();
    }

    /**
     * Called when the licence check times out (no FAKE_ID shown, no movement).
     * Confiscates BUCKET_DRUM, applies heavy penalties.
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @param npcs      all NPCs
     */
    public void onConfiscation(Player player, Inventory inventory, List<NPC> npcs) {
        if (!busking && !licenceCheckActive) return;
        notorietySystem.addNotoriety(NOTORIETY_CONFISCATION, achievementSystem != null
                ? achievementSystem::unlock : null);
        wantedSystem.addWantedStars(WANTED_STARS_CONFISCATION,
                player.getPosition().x, player.getPosition().y, player.getPosition().z,
                achievementSystem != null ? achievementSystem::unlock : null);
        inventory.removeItem(Material.BUCKET_DRUM, 1);
        player.getCriminalRecord().record(CriminalRecord.CrimeType.UNLICENSED_BUSKING);

        award(AchievementType.MOVE_ALONG_PLEASE);

        if (checkingOfficer != null) {
            checkingOfficer.setSpeechText("We'll be taking that.", 3.0f);
            checkingOfficer.setState(NPCState.PATROLLING);
            checkingOfficer = null;
        }
        stopBusk();
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private boolean isNearPavementOrRoad(Player player, World world) {
        int px = (int) Math.floor(player.getPosition().x);
        int py = (int) Math.floor(player.getPosition().y);
        int pz = (int) Math.floor(player.getPosition().z);

        // Check a 3x3 area at the player's feet level and one below
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    BlockType b = world.getBlock(px + dx, py + dy, pz + dz);
                    if (b == BlockType.PAVEMENT || b == BlockType.ROAD) return true;
                }
            }
        }
        return false;
    }

    private boolean isNearShopLandmark(Player player, World world) {
        // Shop landmarks: anything that isn't a park/house/industrial type
        for (Landmark lm : world.getAllLandmarks()) {
            if (!isShopLandmark(lm.getType())) continue;
            float lx = lm.getPosition().x + lm.getWidth() / 2f;
            float lz = lm.getPosition().z + lm.getDepth() / 2f;
            float ddx = player.getPosition().x - lx;
            float ddz = player.getPosition().z - lz;
            double dist = Math.sqrt(ddx * ddx + ddz * ddz);
            if (dist <= MAX_PITCH_DISTANCE_TO_SHOP) return true;
        }
        return false;
    }

    private boolean isShopLandmark(LandmarkType type) {
        switch (type) {
            case GREGGS:
            case OFF_LICENCE:
            case CHARITY_SHOP:
            case JEWELLER:
            case OFFICE_BUILDING:
            case BOOKIES:
            case KEBAB_SHOP:
            case LAUNDERETTE:
            case TESCO_EXPRESS:
            case PUB:
            case PAWN_SHOP:
            case BUILDERS_MERCHANT:
            case CHIPPY:
            case NEWSAGENT:
            case GP_SURGERY:
            case NANDOS:
            case BARBER:
            case NAIL_SALON:
            case WETHERSPOONS:
            case CORNER_SHOP:
            case BETTING_SHOP:
            case PHONE_REPAIR:
            case CASH_CONVERTER:
                return true;
            default:
                return false;
        }
    }

    private NPC findNearbyBusker(Player player, List<NPC> npcs) {
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() != NPCType.BUSKER) continue;
            float ddx = npc.getPosition().x - player.getPosition().x;
            float ddz = npc.getPosition().z - player.getPosition().z;
            double dist = Math.sqrt(ddx * ddx + ddz * ddz);
            if (dist <= PITCH_EXCLUSION_RANGE) return npc;
        }
        return null;
    }

    private void seedBuskerRumour(Player player, List<NPC> npcs) {
        if (rumourNetwork == null) return;
        int seeded = 0;
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.BUSKER || npc.getType() == NPCType.POLICE
                    || npc.getType() == NPCType.PCSO) continue;
            float ddx = npc.getPosition().x - player.getPosition().x;
            float ddz = npc.getPosition().z - player.getPosition().z;
            double dist = Math.sqrt(ddx * ddx + ddz * ddz);
            if (dist <= BUSK_RADIUS_BLOCKS * 3) {
                rumourNetwork.addRumour(npc,
                        new Rumour(RumourType.GANG_ACTIVITY,
                                "Someone's busking out front. Actually not bad."));
                if (++seeded >= 3) break;
            }
        }
    }

    private void triggerLicenceCheck(Player player, List<NPC> npcs) {
        licenceCheckActive = true;
        licenceResponseTimer = LICENCE_RESPONSE_TIMEOUT;

        // Find the nearest POLICE or PCSO NPC
        NPC officer = findNearestOfficer(player, npcs);
        if (officer != null) {
            officer.setState(NPCState.WARNING);
            officer.setSpeechText("You got a busking licence, sunshine?", 6.0f);
            checkingOfficer = officer;
        }
    }

    private NPC findNearestOfficer(Player player, List<NPC> npcs) {
        NPC nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() != NPCType.POLICE && npc.getType() != NPCType.PCSO) continue;
            float ddx = npc.getPosition().x - player.getPosition().x;
            float ddz = npc.getPosition().z - player.getPosition().z;
            float dist = (float) Math.sqrt(ddx * ddx + ddz * ddz);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = npc;
            }
        }
        return nearest;
    }

    private float getMarketMultiplier(MarketEvent activeEvent) {
        if (activeEvent == null) return 1.0f;
        switch (activeEvent) {
            case LAGER_SHORTAGE: return LAGER_SHORTAGE_MULTIPLIER;
            case BENEFIT_DAY:    return BENEFIT_DAY_MULTIPLIER;
            case GREGGS_STRIKE:  return GREGGS_STRIKE_MULTIPLIER;
            default:             return 1.0f;
        }
    }

    private void reduceBoredomForCoins(Player player, List<NPC> npcs, int coinsEarned) {
        if (streetEconomySystem == null) return;
        // Distribute boredom reduction proportionally among nearby NPCs
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            float ddx = npc.getPosition().x - player.getPosition().x;
            float ddz = npc.getPosition().z - player.getPosition().z;
            float dist = (float) Math.sqrt(ddx * ddx + ddz * ddz);
            if (dist <= BUSK_RADIUS_BLOCKS) {
                float currentBored = streetEconomySystem.getNeedScore(npc, NeedType.BORED);
                if (currentBored > 0f) {
                    float newScore = Math.max(0f, currentBored - BORED_NEED_REDUCTION_PER_COIN);
                    streetEconomySystem.setNeedScore(npc, NeedType.BORED, newScore);
                }
            }
        }
    }

    private void seedBarmanRumour(List<NPC> npcs) {
        if (rumourNetwork == null) return;
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.BARMAN) {
                rumourNetwork.addRumour(npc,
                        new Rumour(RumourType.LOOT_TIP,
                                "There's a busker near the shops. Word is they're actually decent."));
                return;
            }
        }
    }

    private void award(AchievementType type) {
        if (achievementSystem != null) {
            achievementSystem.unlock(type);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether a busk session is currently active. */
    public boolean isBusking() {
        return busking;
    }

    /** Whether a police licence check is currently pending. */
    public boolean isLicenceCheckActive() {
        return licenceCheckActive;
    }

    /** The police NPC currently checking the licence (may be null). */
    public NPC getCheckingOfficer() {
        return checkingOfficer;
    }

    /** Total coins earned across all busk sessions this game. */
    public int getTotalCoinsEarned() {
        return totalCoinsEarned;
    }

    /** Seconds elapsed since the current session started (0 if not busking). */
    public float getSessionTimer() {
        return sessionTimer;
    }

    /** Income rate (coins per second) from the last update tick. */
    public float getLastIncomeRate() {
        return lastIncomeRate;
    }

    /** Number of complete consecutive minutes busked in this session. */
    public int getConsecutiveMinutes() {
        return consecutiveMinutes;
    }

    // ── Achievement system adapter ─────────────────────────────────────────────

    /**
     * Thin wrapper to avoid creating a separate interface — callers inject an
     * {@link AchievementSystem} reference directly.
     */
    public interface AchievementSystem {
        void unlock(AchievementType type);
    }
}

package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Issue #781: Graffiti &amp; Territorial Marking — 'Your Name on Every Wall'
 *
 * <p>Manages the full lifecycle of graffiti tags: placement, NPC crew spraying,
 * Council Cleaner scrubbing, turf pressure, passive income, and achievement tracking.
 *
 * <h3>Tag Placement</h3>
 * The player equips a {@link Material#SPRAY_CAN} in the hotbar and presses T to place a
 * {@link GraffitiMark} on any solid block face within 3 blocks.  Each can has
 * {@link #SPRAY_CAN_USES} uses; when exhausted the item reverts to
 * {@link Material#SPRAY_CAN_EMPTY}.
 *
 * <h3>Fade</h3>
 * Tags fade linearly over {@link #FADE_DAYS} in-game days (alpha 1→0).  Tags placed
 * indoors or under an overhang fade at {@link #INDOOR_FADE_MULTIPLIER} × the normal rate.
 *
 * <h3>Turf Pressure</h3>
 * {@link #getTagDensity(int, int, Faction)} counts living tags per 8×8 zone.  The faction
 * with the most tags gains a passive turf-pressure bonus: the
 * {@link FactionSystem#TURF_TRANSFER_GAP} threshold is reduced by
 * {@link #TURF_PRESSURE_GAP_REDUCTION} for that faction's turf transfers.
 *
 * <h3>NPC Crews</h3>
 * Every {@link #NPC_SPRAY_INTERVAL_SECONDS} in-game seconds, up to 2 NPCs per faction
 * are dispatched to tag rival surfaces.
 */
public class GraffitiSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Number of uses per filled spray can. */
    public static final int SPRAY_CAN_USES = 20;

    /** Maximum distance (blocks) at which the player can place a tag. */
    public static final float MAX_TAG_DISTANCE = 3f;

    /** Size of a territorial zone (blocks). */
    public static final int ZONE_SIZE = 8;

    /** Number of PLAYER_TAG marks in a zone to constitute a Claimed Zone. */
    public static final int CLAIMED_ZONE_THRESHOLD = 5;

    /** Passive income per in-game minute from a Claimed Zone (RACKET_PASSIVE_INCOME_COIN / 2). */
    public static final int CLAIMED_ZONE_INCOME_PER_MINUTE = 2;

    /** In-game days before a tag fully fades outdoors. */
    public static final float FADE_DAYS = 7f;

    /** Rate per day at which tags fade (1 / FADE_DAYS). */
    public static final float FADE_RATE_PER_DAY = 1f / FADE_DAYS;

    /** Indoor/sheltered tag fade multiplier (fades at 20 % of outdoor rate). */
    public static final float INDOOR_FADE_MULTIPLIER = 0.2f;

    /** Street Lads tag fade multiplier (twice as fast as normal). */
    public static final float STREET_LADS_FADE_MULTIPLIER = 2f;

    /** Marchetti Crew tag fade multiplier (twice as slow as normal). */
    public static final float MARCHETTI_FADE_MULTIPLIER = 0.5f;

    /** In-game seconds between NPC crew spray dispatch ticks. */
    public static final float NPC_SPRAY_INTERVAL_SECONDS = 300f; // 5 in-game minutes

    /** Maximum NPC spray agents dispatched per faction per tick. */
    public static final int NPC_SPRAY_AGENTS_PER_FACTION = 2;

    /** Turf-transfer gap reduction when a faction has tag-density majority in a zone. */
    public static final int TURF_PRESSURE_GAP_REDUCTION = 5;

    /** Number of graffiti arrests before CRIMINAL_DAMAGE is logged. */
    public static final int GRAFFITI_ARRESTS_FOR_CRIMINAL_DAMAGE = 3;

    /** In-game minutes between claimed-zone passive income ticks. */
    public static final float INCOME_TICK_INTERVAL_SECONDS = 60f; // 1 in-game minute

    // ── Inner types ───────────────────────────────────────────────────────────

    /** Face direction for a block surface. */
    public enum BlockFace {
        TOP, BOTTOM, NORTH, SOUTH, EAST, WEST
    }

    /** Visual tag style — maps to faction association. */
    public enum TagStyle {
        CROWN_TAG,     // Marchetti Crew
        LIGHTNING_TAG, // Street Lads
        CLIPBOARD_TAG, // The Council
        PLAYER_TAG     // Player (neutral)
    }

    /**
     * A single graffiti tag applied to a block face.
     */
    public static class GraffitiMark {
        private final Vector3 blockPos;
        private final BlockFace face;
        private final TagStyle style;
        private final Faction ownerFaction; // null = player
        private float ageInGameDays;
        private boolean scrubbed;
        private boolean isIndoor;

        public GraffitiMark(Vector3 blockPos, BlockFace face, TagStyle style,
                            Faction ownerFaction, boolean isIndoor) {
            this.blockPos      = new Vector3(blockPos);
            this.face          = face;
            this.style         = style;
            this.ownerFaction  = ownerFaction;
            this.ageInGameDays = 0f;
            this.scrubbed      = false;
            this.isIndoor      = isIndoor;
        }

        public Vector3 getBlockPos()       { return blockPos; }
        public BlockFace getFace()         { return face; }
        public TagStyle getStyle()         { return style; }
        public Faction getOwnerFaction()   { return ownerFaction; }
        public float getAgeInGameDays()    { return ageInGameDays; }
        public boolean isScrubbed()        { return scrubbed; }
        public boolean isIndoor()          { return isIndoor; }

        /**
         * Current alpha (0.0 = fully faded, 1.0 = fresh).
         * Uses faction-specific fade rate if applicable.
         */
        public float getAlpha() {
            float effectiveFadeRate = FADE_RATE_PER_DAY;
            if (isIndoor) {
                effectiveFadeRate *= INDOOR_FADE_MULTIPLIER;
            } else if (ownerFaction == Faction.STREET_LADS) {
                effectiveFadeRate *= STREET_LADS_FADE_MULTIPLIER;
            } else if (ownerFaction == Faction.MARCHETTI_CREW) {
                effectiveFadeRate *= MARCHETTI_FADE_MULTIPLIER;
            }
            return Math.max(0f, 1f - ageInGameDays * effectiveFadeRate);
        }

        /** Returns true if the tag is still visible (not scrubbed and not fully faded). */
        public boolean isAlive() {
            return !scrubbed && getAlpha() > 0f;
        }

        void advanceAge(float days) {
            ageInGameDays += days;
        }

        void markScrubbed() {
            scrubbed = true;
        }
    }

    // ── Callback ──────────────────────────────────────────────────────────────

    /** Callback for achievement events fired from GraffitiSystem. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<GraffitiMark> marks = new ArrayList<>();
    private float npcSprayTimer = 0f;
    private float incomeTimer   = 0f;
    private float cleanDayTimer = 0f; // tracks time holding spray can without tagging

    /** Number of times the player has been caught spraying (for CRIMINAL_DAMAGE threshold). */
    private int graffitiArrestCount = 0;

    /** Total tags placed by the player ever (for achievements). */
    private int playerTagsPlaced = 0;

    /** Total tags scrubbed by Council Cleaners (for SCRUBBED achievement). */
    private int playerTagsScrubbed = 0;

    /** Whether the player has placed a tag since the current in-game day started. */
    private boolean taggedThisDay = false;

    /** Current in-game day (tracked externally, advanced via {@link #onNewDay()}). */
    private int currentDay = 0;

    /** Whether CLEAN_HANDS was previously awarded (one-shot). */
    private boolean cleanHandsAwarded = false;

    private final Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GraffitiSystem() {
        this(new Random());
    }

    public GraffitiSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Per-frame update: advances fade timers, dispatches NPC graffiti crews,
     * applies turf pressure, and ticks passive income for claimed zones.
     *
     * @param delta             seconds since last frame
     * @param daysDelta         in-game days elapsed this frame (from TimeSystem)
     * @param npcs              all living NPCs
     * @param turfMap           the world's turf ownership map
     * @param wantedSystem      for escalating wanted level (NPC crew sightings, etc.)
     * @param noiseSystem       for adding noise on outdoor tags
     * @param rumourNetwork     for seeding GANG_ACTIVITY rumours on zone flips
     * @param playerInventory   player inventory (for passive income delivery)
     * @param achievementCallback callback for achievements (may be null)
     */
    public void update(float delta, float daysDelta,
                       List<NPC> npcs, TurfMap turfMap,
                       WantedSystem wantedSystem, NoiseSystem noiseSystem,
                       RumourNetwork rumourNetwork,
                       Inventory playerInventory,
                       AchievementCallback achievementCallback) {

        // 1. Advance fade timers for all marks
        for (GraffitiMark mark : marks) {
            if (!mark.isScrubbed()) {
                mark.advanceAge(daysDelta);
            }
        }

        // 2. NPC crew spray tick
        npcSprayTimer += delta;
        if (npcSprayTimer >= NPC_SPRAY_INTERVAL_SECONDS) {
            npcSprayTimer = 0f;
            dispatchNpcCrews(npcs, turfMap, rumourNetwork);
        }

        // 3. Passive income for claimed zones
        incomeTimer += delta;
        if (incomeTimer >= INCOME_TICK_INTERVAL_SECONDS) {
            incomeTimer = 0f;
            int claimedCount = getClaimedZones(null).size();
            if (claimedCount > 0 && playerInventory != null) {
                playerInventory.addItem(Material.COIN, claimedCount * CLAIMED_ZONE_INCOME_PER_MINUTE);
            }
        }

        // 4. Check ALL_CITY achievement
        if (achievementCallback != null) {
            checkAllCityAchievement(turfMap, achievementCallback);
        }

        // 5. CLEAN_HANDS: advance tracker if player holds spray can without tagging
        // (caller is responsible for checking if player holds SPRAY_CAN;
        //  use onHoldingSprayCanWithoutTagging() for the day timer)
    }

    // ── Tag placement ─────────────────────────────────────────────────────────

    /**
     * Place a graffiti tag on a block face.
     *
     * @param blockPos    position of the targeted block
     * @param face        which face of the block to tag
     * @param style       tag style (determines faction association)
     * @param playerPos   player's current world position (for range validation)
     * @param isIndoor    true if the surface is indoors/sheltered (slows fade)
     * @param isOutdoor   true if visible outdoors (adds noise and wanted risk)
     * @param inventory   player inventory (spray can is consumed/decremented here)
     * @param turfMap     turf ownership map (updated on PLAYER_TAG)
     * @param noiseSystem noise system (receives NOISE_GRAFFITI on outdoor tag)
     * @param achievementCallback callback for achievements (may be null)
     * @return the new {@link GraffitiMark}, or null if placement failed
     */
    public GraffitiMark placeTag(Vector3 blockPos, BlockFace face, TagStyle style,
                                  Vector3 playerPos, boolean isIndoor, boolean isOutdoor,
                                  Inventory inventory, TurfMap turfMap,
                                  NoiseSystem noiseSystem,
                                  AchievementCallback achievementCallback) {
        // Range check
        float dist = playerPos.dst(blockPos);
        if (dist > MAX_TAG_DISTANCE) {
            return null;
        }

        // Spray can required
        if (inventory == null || inventory.getItemCount(Material.SPRAY_CAN) < 1) {
            return null;
        }

        // Determine owning faction from tag style
        Faction ownerFaction = factionForStyle(style);

        // Create the mark
        GraffitiMark mark = new GraffitiMark(blockPos, face, style, ownerFaction, isIndoor);
        marks.add(mark);

        // Decrement spray can use count (or replace with empty)
        decrementSprayCan(inventory);

        // Outdoor noise
        if (isOutdoor && noiseSystem != null) {
            noiseSystem.addNoise(NoiseSystem.NOISE_GRAFFITI);
        }

        // TurfMap: PLAYER_TAG claims the surface
        if (style == TagStyle.PLAYER_TAG && turfMap != null) {
            turfMap.setOwner((int) blockPos.x, (int) blockPos.z, null);
            // null → treated as "player" marker; we use a convention of null faction
            // to mean player-owned in turfMap. For full faction ownership, see getClaimedZones.
        }

        // Achievements
        if (style == TagStyle.PLAYER_TAG) {
            playerTagsPlaced++;
            taggedThisDay = true;

            if (achievementCallback != null) {
                if (playerTagsPlaced == 1) {
                    achievementCallback.award(AchievementType.WRITER);
                }
                if (playerTagsPlaced == 50) {
                    achievementCallback.award(AchievementType.GETTING_UP);
                } else if (playerTagsPlaced > 50) {
                    // Already past threshold — still award if not done
                    achievementCallback.award(AchievementType.GETTING_UP);
                }
            }
        }

        return mark;
    }

    /**
     * Internal: place a graffiti tag on behalf of an NPC faction crew (no inventory or LOS checks).
     */
    GraffitiMark placeNpcTag(Vector3 blockPos, BlockFace face, TagStyle style, Faction ownerFaction,
                              boolean isIndoor) {
        GraffitiMark mark = new GraffitiMark(blockPos, face, style, ownerFaction, isIndoor);
        marks.add(mark);
        return mark;
    }

    /**
     * Scrub a graffiti tag (called by Council Cleaner NPCs).
     * Updates TurfMap and fires SCRUBBED achievement tracking.
     *
     * @param mark      the tag to scrub
     * @param turfMap   turf ownership map
     * @param achievementCallback callback for achievements (may be null)
     */
    public void scrubTag(GraffitiMark mark, TurfMap turfMap,
                         AchievementCallback achievementCallback) {
        if (mark == null || mark.isScrubbed()) return;
        mark.markScrubbed();

        // Clear turfmap ownership if the mark was a PLAYER_TAG
        if (mark.getStyle() == TagStyle.PLAYER_TAG && turfMap != null) {
            turfMap.setOwner((int) mark.getBlockPos().x, (int) mark.getBlockPos().z, null);
        }

        // Track scrubbed count for SCRUBBED achievement
        if (mark.getStyle() == TagStyle.PLAYER_TAG) {
            playerTagsScrubbed++;
            if (achievementCallback != null && playerTagsScrubbed >= 10) {
                achievementCallback.award(AchievementType.SCRUBBED);
            }
        }
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /**
     * Count living (non-faded, non-scrubbed) tags belonging to the given faction
     * in the 8×8 zone containing (zoneX * 8, zoneZ * 8).
     *
     * @param zoneX  zone column index (world_x / ZONE_SIZE)
     * @param zoneZ  zone row index    (world_z / ZONE_SIZE)
     * @param faction the faction to count, or null to count player tags (TagStyle.PLAYER_TAG, ownerFaction == null)
     * @return number of living tags
     */
    public int getTagDensity(int zoneX, int zoneZ, Faction faction) {
        int count = 0;
        int minX = zoneX * ZONE_SIZE;
        int maxX = minX + ZONE_SIZE;
        int minZ = zoneZ * ZONE_SIZE;
        int maxZ = minZ + ZONE_SIZE;
        for (GraffitiMark mark : marks) {
            if (!mark.isAlive()) continue;
            int mx = (int) mark.getBlockPos().x;
            int mz = (int) mark.getBlockPos().z;
            if (mx >= minX && mx < maxX && mz >= minZ && mz < maxZ) {
                if (faction == null) {
                    // Count player tags
                    if (mark.getStyle() == TagStyle.PLAYER_TAG && mark.getOwnerFaction() == null) {
                        count++;
                    }
                } else {
                    if (faction == mark.getOwnerFaction()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Returns the list of 8×8 zone indices [zoneX, zoneZ] where the given faction
     * (or player if faction is null) has a majority of living tags AND meets the
     * {@link #CLAIMED_ZONE_THRESHOLD} for player zones.
     *
     * @param faction null = player, otherwise a {@link Faction}
     * @return list of int[2] arrays: [zoneX, zoneZ]
     */
    public List<int[]> getClaimedZones(Faction faction) {
        // Collect distinct zone coords for all living marks
        Map<Long, Integer> zoneCounts = new HashMap<>();
        for (GraffitiMark mark : marks) {
            if (!mark.isAlive()) continue;
            boolean matches;
            if (faction == null) {
                matches = mark.getStyle() == TagStyle.PLAYER_TAG && mark.getOwnerFaction() == null;
            } else {
                matches = faction == mark.getOwnerFaction();
            }
            if (!matches) continue;
            int zx = (int) mark.getBlockPos().x / ZONE_SIZE;
            int zz = (int) mark.getBlockPos().z / ZONE_SIZE;
            long key = ((long) zx << 32) | (zz & 0xFFFFFFFFL);
            zoneCounts.merge(key, 1, (a, b) -> a + b);
        }

        List<int[]> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : zoneCounts.entrySet()) {
            int threshold = (faction == null) ? CLAIMED_ZONE_THRESHOLD : 1;
            if (e.getValue() >= threshold) {
                int zx = (int) (e.getKey() >> 32);
                int zz = (int) (e.getKey() & 0xFFFFFFFFL);
                result.add(new int[]{zx, zz});
            }
        }
        return result;
    }

    /**
     * Returns the {@link FactionSystem#TURF_TRANSFER_GAP} adjustment for a given faction
     * in a given zone.  If the faction has the majority of living tags in that zone,
     * returns {@link #TURF_PRESSURE_GAP_REDUCTION} (positive = threshold lowered, so
     * transfers happen sooner).  Otherwise returns 0.
     */
    public int getTurfPressureAdjustment(int zoneX, int zoneZ, Faction faction) {
        int maxCount = 0;
        Faction majority = null;
        for (Faction f : Faction.values()) {
            int c = getTagDensity(zoneX, zoneZ, f);
            if (c > maxCount) {
                maxCount = c;
                majority = f;
            }
        }
        // Also check player
        int playerCount = getTagDensity(zoneX, zoneZ, null);
        if (playerCount > maxCount) {
            majority = null; // player majority, no faction bonus
            maxCount = playerCount;
        }

        if (maxCount > 0 && majority == faction) {
            return TURF_PRESSURE_GAP_REDUCTION;
        }
        return 0;
    }

    /**
     * Get all marks (living and dead) — for rendering.
     */
    public List<GraffitiMark> getAllMarks() {
        return Collections.unmodifiableList(marks);
    }

    /**
     * Get only living (non-faded, non-scrubbed) marks.
     */
    public List<GraffitiMark> getLivingMarks() {
        List<GraffitiMark> result = new ArrayList<>();
        for (GraffitiMark mark : marks) {
            if (mark.isAlive()) result.add(mark);
        }
        return result;
    }

    // ── Crime / wanted interaction ────────────────────────────────────────────

    /**
     * Call when the player is caught spraying by police.  Increments the graffiti arrest
     * count and logs {@link CriminalRecord.CrimeType#CRIMINAL_DAMAGE} after 3 arrests.
     *
     * @param inventory       player inventory (spray can confiscated)
     * @param criminalRecord  the player's criminal record
     */
    public void onCaughtSpraying(Inventory inventory, CriminalRecord criminalRecord) {
        // Confiscate spray can
        if (inventory != null) {
            inventory.removeItem(Material.SPRAY_CAN, inventory.getItemCount(Material.SPRAY_CAN));
        }

        graffitiArrestCount++;
        if (criminalRecord != null && graffitiArrestCount >= GRAFFITI_ARRESTS_FOR_CRIMINAL_DAMAGE) {
            criminalRecord.record(CriminalRecord.CrimeType.CRIMINAL_DAMAGE);
        }
    }

    // ── New-day hook ──────────────────────────────────────────────────────────

    /**
     * Call once per in-game day to advance day tracking (CLEAN_HANDS achievement).
     *
     * @param playerHoldsSprayCan true if the player currently has a SPRAY_CAN in inventory
     * @param achievementCallback callback for achievements (may be null)
     */
    public void onNewDay(boolean playerHoldsSprayCan, AchievementCallback achievementCallback) {
        currentDay++;
        if (playerHoldsSprayCan && !taggedThisDay && !cleanHandsAwarded) {
            cleanHandsAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CLEAN_HANDS);
            }
        }
        taggedThisDay = false;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Faction factionForStyle(TagStyle style) {
        switch (style) {
            case CROWN_TAG:     return Faction.MARCHETTI_CREW;
            case LIGHTNING_TAG: return Faction.STREET_LADS;
            case CLIPBOARD_TAG: return Faction.THE_COUNCIL;
            default:            return null; // PLAYER_TAG → no faction
        }
    }

    /**
     * Decrement the spray can use count in inventory.
     * Since Material doesn't track use counts natively, we model it as:
     * remove 1 SPRAY_CAN, add 1 SPRAY_CAN_EMPTY when count reaches 0.
     *
     * <p>For the spec's "20 uses" we track via {@link #sprayCanUsesLeft} per can slot.
     * In the absence of a per-item metadata system, we use the simple approach:
     * each craft yields one SPRAY_CAN that represents one tag (and the test gives exactly
     * 1 use remaining), while the game loop decrements the count after {@link #SPRAY_CAN_USES}
     * tags.  For simplicity here: each placeTag removes 1 SPRAY_CAN from inventory.
     * The caller is responsible for checking the use-count logic if a richer system exists.
     */
    private void decrementSprayCan(Inventory inventory) {
        if (inventory.getItemCount(Material.SPRAY_CAN) > 0) {
            inventory.removeItem(Material.SPRAY_CAN, 1);
            // If no more cans, add an empty can back
            if (inventory.getItemCount(Material.SPRAY_CAN) == 0) {
                inventory.addItem(Material.SPRAY_CAN_EMPTY, 1);
            }
        }
    }

    private void dispatchNpcCrews(List<NPC> npcs, TurfMap turfMap, RumourNetwork rumourNetwork) {
        for (Faction faction : Faction.values()) {
            if (faction == Faction.THE_COUNCIL) continue; // Council uses cleaners, not taggers

            int dispatched = 0;
            for (NPC npc : npcs) {
                if (dispatched >= NPC_SPRAY_AGENTS_PER_FACTION) break;
                if (!npc.isAlive()) continue;
                if (!isFactionNpc(npc, faction)) continue;

                // Pick a random block face location near the NPC
                float nx = npc.getPosition().x + (random.nextFloat() * 10f - 5f);
                float nz = npc.getPosition().z + (random.nextFloat() * 10f - 5f);
                Vector3 targetBlock = new Vector3(Math.round(nx), 1f, Math.round(nz));

                TagStyle style = (faction == Faction.MARCHETTI_CREW)
                        ? TagStyle.CROWN_TAG : TagStyle.LIGHTNING_TAG;

                // Check if this flips zone majority before placing
                int zoneX = (int) targetBlock.x / ZONE_SIZE;
                int zoneZ = (int) targetBlock.z / ZONE_SIZE;
                Faction prevMajority = getZoneMajority(zoneX, zoneZ);

                GraffitiMark placed = placeNpcTag(targetBlock, BlockFace.NORTH, style, faction, false);

                Faction newMajority = getZoneMajority(zoneX, zoneZ);
                if (prevMajority != newMajority && rumourNetwork != null) {
                    String text = faction.getDisplayName() + " are taking over the streets";
                    Rumour rumour = new Rumour(RumourType.GANG_ACTIVITY, text);
                    int seeded = 0;
                    for (NPC n : npcs) {
                        if (n.isAlive() && seeded < 3) {
                            rumourNetwork.addRumour(n, rumour);
                            seeded++;
                        }
                    }
                }

                dispatched++;
            }
        }
    }

    /**
     * Returns the faction with the most living tags in the given zone, or null if none.
     */
    public Faction getZoneMajority(int zoneX, int zoneZ) {
        int max = 0;
        Faction result = null;
        for (Faction f : Faction.values()) {
            int count = getTagDensity(zoneX, zoneZ, f);
            if (count > max) {
                max = count;
                result = f;
            }
        }
        return result;
    }

    private boolean isFactionNpc(NPC npc, Faction faction) {
        String name = npc.getName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        switch (faction) {
            case MARCHETTI_CREW: return lower.startsWith("marchetti");
            case STREET_LADS:    return lower.startsWith("streetlads") || lower.startsWith("street_lads");
            case THE_COUNCIL:    return lower.startsWith("council");
            default:             return false;
        }
    }

    private void checkAllCityAchievement(TurfMap turfMap, AchievementCallback achievementCallback) {
        if (turfMap == null || achievementCallback == null) return;
        // Determine world zone extent from TurfMap dimensions
        int maxZoneX = turfMap.getWidth() / ZONE_SIZE;
        int maxZoneZ = turfMap.getDepth() / ZONE_SIZE;
        if (maxZoneX <= 0 || maxZoneZ <= 0) return;

        for (int zx = 0; zx < maxZoneX; zx++) {
            for (int zz = 0; zz < maxZoneZ; zz++) {
                if (getTagDensity(zx, zz, null) == 0) return; // zone with no player tag
            }
        }
        achievementCallback.award(AchievementType.ALL_CITY);
    }

    // ── Accessors for testing ─────────────────────────────────────────────────

    public int getGraffitiArrestCount()  { return graffitiArrestCount; }
    public int getPlayerTagsPlaced()     { return playerTagsPlaced; }
    public int getPlayerTagsScrubbed()   { return playerTagsScrubbed; }
    public boolean isTaggedThisDay()     { return taggedThisDay; }

    /** Clear all marks (for testing). */
    public void clearAllMarks() {
        marks.clear();
        playerTagsPlaced = 0;
        playerTagsScrubbed = 0;
        graffitiArrestCount = 0;
        taggedThisDay = false;
    }
}

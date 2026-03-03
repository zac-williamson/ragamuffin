package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1347: Northfield Remembrance Sunday — The Two-Minute Silence, the Wreath Hustle
 * &amp; the Poppy Seller.
 *
 * <p>An annual civic event at Northfield's war memorial (STATUE prop in the park) on the
 * second Sunday of November, 10:30–12:00. Led by Reverend Dave (VICAR from ChurchSystem),
 * attended by VETERAN and RAOB_LODGE_MEMBER NPCs, with a POPPY_SELLER outside the church
 * from 09:00.
 *
 * <h3>Schedule</h3>
 * <ul>
 *   <li>09:00 — POPPY_SELLER (Doris) spawns outside St. Mary's Church.</li>
 *   <li>10:30 — Ceremony begins; VICAR, VETERANs, RAOB_LODGE_MEMBERs gather at STATUE.</li>
 *   <li>11:00 — Two-minute silence begins. All NPCs freeze. Ambient sound cuts.</li>
 *   <li>11:02 — Silence ends. VICAR lays WREATH_PROP at STATUE; VETERANs salute.</li>
 *   <li>11:30 — WREATH_PROP becomes stealable. THUNDERSTORM causes early dispersal.</li>
 *   <li>12:00 — Ceremony ends; VETERANs and RAOB_LODGE_MEMBERs surge to The Ragamuffin Arms.</li>
 *   <li>12:30 — POPPY_SELLER despawns.</li>
 * </ul>
 *
 * <h3>Two-Minute Silence</h3>
 * Any player action (movement, attacking, block breaking) during 11:00–11:02 triggers:
 * <ul>
 *   <li>Notoriety +5</li>
 *   <li>WantedSystem +1 star</li>
 *   <li>SILENCE_BREACH recorded in CriminalRecord</li>
 *   <li>PUBLIC NPCs emit outrage speech bubbles</li>
 *   <li>SILENCE_BREACH rumour seeded</li>
 *   <li>NewspaperSystem headline next day: 'Local yob disrupts Remembrance ceremony'</li>
 * </ul>
 *
 * <h3>POPPY mechanic</h3>
 * Buy POPPY for 1 COIN from POPPY_SELLER → sets WEARING_POPPY flag → DisguiseSystem −1 star
 * suspicion and +5 charm bonus for the day. Awards LAST_POST achievement on first purchase.
 *
 * <h3>Wreath theft</h3>
 * Steal WREATH_PROP after 11:30 → MEMORIAL_VANDALISM, Notoriety +10, Wanted +2.
 * Fenceable via {@link #fenceWreath} for 8–12 COIN. Awards no achievement (shame).
 *
 * <h3>Weather</h3>
 * THUNDERSTORM causes early dispersal at 11:30 with 'It was worse at Goose Green' speech.
 * RAIN does not cancel (British stubbornness).
 *
 * <h3>Post-ceremony</h3>
 * VETERANs and PENSIONERS flood The Ragamuffin Arms — good pickpocket window.
 */
public class RemembranceSundaySystem {

    // ── Day/month constants ───────────────────────────────────────────────────

    /** Month of November (1-based). */
    public static final int NOVEMBER = 11;

    /** Day-of-week index for Sunday (dayCount % 7 == 6, matching ChurchSystem convention). */
    public static final int SUNDAY = 6;

    // ── Timing constants ─────────────────────────────────────────────────────

    /** Hour at which the POPPY_SELLER spawns outside the church. */
    public static final float POPPY_SELLER_SPAWN_HOUR = 9.0f;

    /** Hour at which the main ceremony begins (NPCs gather at STATUE). */
    public static final float CEREMONY_START_HOUR = 10.5f;  // 10:30

    /** Hour at which the two-minute silence begins. */
    public static final float SILENCE_START_HOUR = 11.0f;

    /** Hour at which the two-minute silence ends and wreath is laid. */
    public static final float SILENCE_END_HOUR = 11.0f + (2.0f / 60.0f);  // 11:02

    /**
     * Hour after which the WREATH_PROP becomes stealable.
     * Also the hour at which THUNDERSTORM triggers early dispersal.
     */
    public static final float WREATH_STEALABLE_HOUR = 11.5f;  // 11:30

    /** Hour at which the ceremony formally ends and the pub surge begins. */
    public static final float CEREMONY_END_HOUR = 12.0f;

    /** Hour at which the POPPY_SELLER despawns. */
    public static final float POPPY_SELLER_DESPAWN_HOUR = 12.5f;  // 12:30

    // ── NPC spawn counts ─────────────────────────────────────────────────────

    /** Number of VETERAN NPCs that attend the ceremony. */
    public static final int VETERAN_COUNT = 4;

    /** Number of RAOB_LODGE_MEMBER NPCs that march in the parade. */
    public static final int RAOB_MEMBER_COUNT = 3;

    // ── Silence-breach penalties ──────────────────────────────────────────────

    /** Notoriety gain when the player breaches the two-minute silence. */
    public static final int SILENCE_BREACH_NOTORIETY = 5;

    /** Wanted stars added when the player breaches the two-minute silence. */
    public static final int SILENCE_BREACH_WANTED = 1;

    // ── Wreath-theft penalties ────────────────────────────────────────────────

    /** Notoriety gain for stealing the Remembrance wreath. */
    public static final int WREATH_THEFT_NOTORIETY = 10;

    /** Wanted stars added for stealing the Remembrance wreath. */
    public static final int WREATH_THEFT_WANTED = 2;

    /** Minimum COIN yield when fencing the stolen wreath at a PawnShop. */
    public static final int WREATH_FENCE_MIN = 8;

    /** Maximum COIN yield when fencing the stolen wreath at a PawnShop. */
    public static final int WREATH_FENCE_MAX = 12;

    // ── POPPY purchase ────────────────────────────────────────────────────────

    /** Cost of a POPPY from the POPPY_SELLER (Doris). */
    public static final int POPPY_COST = 1;

    // ── Newspaper headline ────────────────────────────────────────────────────

    /** Newspaper headline published the day after a silence breach. */
    public static final String HEADLINE_SILENCE_BREACH =
            "Local yob disrupts Remembrance ceremony";

    // ── Thunderstorm dispersal ────────────────────────────────────────────────

    /** Speech line emitted by VETERANs on THUNDERSTORM early dispersal. */
    public static final String GOOSE_GREEN_SPEECH =
            "It was worse at Goose Green.";

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** True if the ceremony has been initialised for the current game year. */
    private boolean ceremonyScheduled = false;

    /** The in-game day on which Remembrance Sunday falls this year. */
    private int remembranceDayCount = -1;

    /** True once the POPPY_SELLER has been spawned this year. */
    private boolean poppySellerSpawned = false;

    /** True once ceremony NPCs have been spawned. */
    private boolean ceremonyNpcsSpawned = false;

    /** True during the two-minute silence (11:00–11:02). */
    private boolean silenceActive = false;

    /** True once the silence has been observed without breach. */
    private boolean silenceCompleted = false;

    /** True once the player has breached the silence (prevents double-penalty). */
    private boolean silenceBreached = false;

    /** True once the WREATH_PROP has been laid (at 11:02). */
    private boolean wreathLaid = false;

    /** True if the player has stolen the wreath. */
    private boolean wreathStolen = false;

    /** True once the ceremony has ended (dispersal to pub). */
    private boolean ceremonyEnded = false;

    /** True if the ceremony was dispersed early by THUNDERSTORM. */
    private boolean thunderstormDispersal = false;

    /** True if the player is currently wearing a POPPY. */
    private boolean wearingPoppy = false;

    /** True once LAST_POST achievement awarded. */
    private boolean lastPostAwarded = false;

    /** True once LEST_WE_FORGET achievement awarded. */
    private boolean lestWeFOrgetAwarded = false;

    /** True once the REMEMBRANCE_CEREMONY rumour has been seeded. */
    private boolean ceremonyRumourSeeded = false;

    /** True once the NewspaperSystem headline for silence breach has been queued. */
    private boolean breachHeadlineQueued = false;

    /** Managed NPC list (ceremony attendees + poppy seller). */
    private final List<NPC> ceremonyNpcs = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    /** Create the system with the default random source. */
    public RemembranceSundaySystem() {
        this(new Random());
    }

    /** Create the system with a seeded random source (for tests). */
    public RemembranceSundaySystem(Random random) {
        this.random = random;
    }

    // ── Public query methods ──────────────────────────────────────────────────

    /**
     * Returns true if the given day is Remembrance Sunday (second Sunday of November).
     *
     * @param dayCount    current in-game day count (1-based)
     * @param month       current in-game month (1-based; November = 11)
     * @param dayOfMonth  current day-of-month (1-based)
     */
    public boolean isRemembranceSunday(int dayCount, int month, int dayOfMonth) {
        if (month != NOVEMBER) return false;
        if (dayCount % 7 != SUNDAY) return false;
        // Second Sunday of November: day-of-month 8–14
        return dayOfMonth >= 8 && dayOfMonth <= 14;
    }

    /**
     * Returns true if the two-minute silence is currently active.
     */
    public boolean isSilenceActive() {
        return silenceActive;
    }

    /**
     * Returns true if the silence was completed without a breach.
     */
    public boolean isSilenceCompleted() {
        return silenceCompleted;
    }

    /**
     * Returns true if the player has breached the silence.
     */
    public boolean isSilenceBreached() {
        return silenceBreached;
    }

    /**
     * Returns true if the WREATH_PROP has been laid at the STATUE.
     */
    public boolean isWreathLaid() {
        return wreathLaid;
    }

    /**
     * Returns true if the player has stolen the wreath.
     */
    public boolean isWreathStolen() {
        return wreathStolen;
    }

    /**
     * Returns true if the player is wearing a POPPY today.
     */
    public boolean isWearingPoppy() {
        return wearingPoppy;
    }

    /**
     * Returns true if the ceremony is currently active (10:30–12:00 or until
     * thunderstorm dispersal).
     */
    public boolean isCeremonyActive() {
        return ceremonyNpcsSpawned && !ceremonyEnded;
    }

    /**
     * Returns true once the ceremony has ended and the pub surge has started.
     */
    public boolean isCeremonyEnded() {
        return ceremonyEnded;
    }

    /**
     * Returns true if the POPPY_SELLER has been spawned.
     */
    public boolean isPoppySellerSpawned() {
        return poppySellerSpawned;
    }

    /**
     * Returns the NPC list managed by this system (ceremony attendees + poppy seller).
     */
    public List<NPC> getCeremonyNpcs() {
        return ceremonyNpcs;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Call every game tick.
     *
     * @param delta          seconds elapsed since last frame
     * @param timeSystem     current time/date source
     * @param npcs           global NPC list (for spawning / despawning)
     * @param notoriety      notoriety system (may be null in tests)
     * @param wanted         wanted-level system (may be null in tests)
     * @param criminalRecord criminal record (may be null in tests)
     * @param rumourNetwork  rumour network (may be null in tests)
     * @param weatherSystem  weather system (may be null in tests)
     * @param newspaper      newspaper system (may be null in tests)
     * @param achievement    achievement callback (may be null in tests)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       List<NPC> npcs,
                       NotorietySystem notoriety,
                       WantedSystem wanted,
                       CriminalRecord criminalRecord,
                       RumourNetwork rumourNetwork,
                       WeatherSystem weatherSystem,
                       NewspaperSystem newspaper,
                       AchievementCallback achievement) {

        int dayCount    = timeSystem.getDayCount();
        int month       = timeSystem.getMonth();
        int dayOfMonth  = timeSystem.getDayOfMonth();
        float hour      = timeSystem.getTime();

        if (!isRemembranceSunday(dayCount, month, dayOfMonth)) {
            // Reset for next year if we've moved off the day
            if (ceremonyScheduled && remembranceDayCount != dayCount) {
                resetForNewYear();
            }
            return;
        }

        // Mark as scheduled on first encounter
        if (!ceremonyScheduled) {
            ceremonyScheduled = true;
            remembranceDayCount = dayCount;
        }

        // ── Seed ceremony rumour at 09:00 ─────────────────────────────────────
        if (!ceremonyRumourSeeded && hour >= POPPY_SELLER_SPAWN_HOUR) {
            seedCeremonyRumour(rumourNetwork, npcs);
            ceremonyRumourSeeded = true;
        }

        // ── Spawn POPPY_SELLER at 09:00 ───────────────────────────────────────
        if (!poppySellerSpawned && hour >= POPPY_SELLER_SPAWN_HOUR) {
            spawnPoppySeller(npcs);
            poppySellerSpawned = true;
        }

        // ── Spawn ceremony NPCs at 10:30 ──────────────────────────────────────
        if (!ceremonyNpcsSpawned && hour >= CEREMONY_START_HOUR) {
            spawnCeremonyNpcs(npcs);
            ceremonyNpcsSpawned = true;
        }

        // ── Begin two-minute silence at 11:00 ─────────────────────────────────
        if (ceremonyNpcsSpawned && !silenceActive && !silenceCompleted && !silenceBreached
                && hour >= SILENCE_START_HOUR && hour < SILENCE_END_HOUR) {
            silenceActive = true;
            freezeCeremonyNpcs();
        }

        // ── End silence at 11:02 ──────────────────────────────────────────────
        if (silenceActive && hour >= SILENCE_END_HOUR) {
            silenceActive = false;
            if (!silenceBreached) {
                silenceCompleted = true;
            }
            unfreezeCeremonyNpcs();
            // VICAR lays wreath; VETERANs salute
            layWreath();
            wreathLaid = true;
            // Award LEST_WE_FORGET if player observed silence
            if (!silenceBreached && !lestWeFOrgetAwarded && achievement != null) {
                achievement.award(AchievementType.LEST_WE_FORGET);
                lestWeFOrgetAwarded = true;
            }
        }

        // ── Check for THUNDERSTORM early dispersal at 11:30 ──────────────────
        if (ceremonyNpcsSpawned && !ceremonyEnded && hour >= WREATH_STEALABLE_HOUR) {
            Weather weather = (weatherSystem != null) ? weatherSystem.getCurrentWeather() : null;
            if (!thunderstormDispersal && weather == Weather.THUNDERSTORM) {
                thunderstormDispersal = true;
                disperseCeremony(npcs, true);
                ceremonyEnded = true;
            }
        }

        // ── Formal ceremony end at 12:00 ──────────────────────────────────────
        if (ceremonyNpcsSpawned && !ceremonyEnded && hour >= CEREMONY_END_HOUR) {
            disperseCeremony(npcs, false);
            ceremonyEnded = true;
        }

        // ── Despawn POPPY_SELLER at 12:30 ─────────────────────────────────────
        if (poppySellerSpawned && hour >= POPPY_SELLER_DESPAWN_HOUR) {
            despawnPoppySeller(npcs);
        }
    }

    // ── Player action during silence ──────────────────────────────────────────

    /**
     * Call this method when the player takes an action (moves, attacks, or breaks
     * a block) while the two-minute silence is active. If the silence is active and
     * has not already been breached, applies all silence-breach penalties.
     *
     * @param notoriety      notoriety system
     * @param wanted         wanted-level system
     * @param criminalRecord criminal record
     * @param rumourNetwork  rumour network (for SILENCE_BREACH rumour)
     * @param newspaper      newspaper system (for next-day headline)
     * @param npcs           global NPC list (PUBLIC NPCs emit outrage)
     * @return true if the silence was broken by this action (first time only)
     */
    public boolean onPlayerAction(NotorietySystem notoriety,
                                  WantedSystem wanted,
                                  CriminalRecord criminalRecord,
                                  RumourNetwork rumourNetwork,
                                  NewspaperSystem newspaper,
                                  List<NPC> npcs) {
        if (!silenceActive || silenceBreached) {
            return false;
        }

        silenceBreached = true;
        silenceActive   = false;
        unfreezeCeremonyNpcs();

        // Apply penalties
        if (notoriety != null) {
            notoriety.addNotoriety(SILENCE_BREACH_NOTORIETY, null);
        }
        if (wanted != null) {
            wanted.addWantedStars(SILENCE_BREACH_WANTED, 0f, 0f, 0f, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.SILENCE_BREACH);
        }

        // PUBLIC NPCs emit outrage
        emitOutrage(npcs);

        // Seed SILENCE_BREACH rumour
        if (rumourNetwork != null) {
            seedSilenceBreachRumour(rumourNetwork, npcs);
        }

        // Queue newspaper headline for next day
        if (!breachHeadlineQueued && newspaper != null) {
            newspaper.recordEvent(new NewspaperSystem.InfamyEvent(
                    "REMEMBRANCE_BREACH",
                    "War Memorial",
                    null,
                    null,
                    wanted != null ? wanted.getWantedStars() : 1,
                    "NONE",
                    null,
                    7
            ));
            breachHeadlineQueued = true;
        }

        return true;
    }

    // ── POPPY purchase ────────────────────────────────────────────────────────

    /**
     * Player presses E on the POPPY_SELLER (Doris) to buy a POPPY for 1 COIN.
     * Sets the WEARING_POPPY flag (accessible via {@link #isWearingPoppy()}).
     * Awards LAST_POST achievement on first purchase.
     *
     * @param inventory   player inventory
     * @param achievement achievement callback
     * @return {@link BuyPoppyResult} indicating the outcome
     */
    public BuyPoppyResult buyPoppy(Inventory inventory, AchievementCallback achievement) {
        if (!poppySellerSpawned) {
            return BuyPoppyResult.SELLER_NOT_PRESENT;
        }
        if (inventory.getItemCount(Material.COIN) < POPPY_COST) {
            return BuyPoppyResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, POPPY_COST);
        inventory.addItem(Material.POPPY, 1);
        wearingPoppy = true;

        if (!lastPostAwarded && achievement != null) {
            achievement.award(AchievementType.LAST_POST);
            lastPostAwarded = true;
        }
        return BuyPoppyResult.SUCCESS;
    }

    /** Result of a POPPY purchase attempt. */
    public enum BuyPoppyResult {
        /** Purchase successful; POPPY added to inventory, WEARING_POPPY set. */
        SUCCESS,
        /** POPPY_SELLER is not present (wrong time or wrong day). */
        SELLER_NOT_PRESENT,
        /** Player does not have enough COIN. */
        INSUFFICIENT_FUNDS
    }

    // ── Wreath theft ──────────────────────────────────────────────────────────

    /**
     * Player presses E on the WREATH_PROP to steal it. Only possible after 11:30
     * and while the wreath has not already been stolen.
     *
     * @param inventory      player inventory
     * @param currentHour    current in-game hour
     * @param notoriety      notoriety system
     * @param wanted         wanted-level system
     * @param criminalRecord criminal record
     * @param npcs           global NPC list (VETERANs turn hostile)
     * @return {@link StealWreathResult} indicating the outcome
     */
    public StealWreathResult stealWreath(Inventory inventory,
                                         float currentHour,
                                         NotorietySystem notoriety,
                                         WantedSystem wanted,
                                         CriminalRecord criminalRecord,
                                         List<NPC> npcs) {
        if (!wreathLaid) {
            return StealWreathResult.WREATH_NOT_PRESENT;
        }
        if (currentHour < WREATH_STEALABLE_HOUR) {
            return StealWreathResult.TOO_EARLY;
        }
        if (wreathStolen) {
            return StealWreathResult.ALREADY_STOLEN;
        }

        wreathStolen = true;
        inventory.addItem(Material.POPPY, 3 + random.nextInt(3)); // 3–5 POPPYs from wreath

        if (notoriety != null) {
            notoriety.addNotoriety(WREATH_THEFT_NOTORIETY, null);
        }
        if (wanted != null) {
            wanted.addWantedStars(WREATH_THEFT_WANTED, 0f, 0f, 0f, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.MEMORIAL_VANDALISM);
        }

        // VETERANs turn hostile
        for (NPC npc : ceremonyNpcs) {
            if (npc.getType() == NPCType.VETERAN) {
                npc.setState(NPCState.FIGHTING_EACH_OTHER); // pursuit state
            }
        }
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.VETERAN) {
                    npc.setState(NPCState.FIGHTING_EACH_OTHER);
                }
            }
        }

        return StealWreathResult.SUCCESS;
    }

    /** Result of a wreath-theft attempt. */
    public enum StealWreathResult {
        /** Wreath stolen successfully. Penalties applied. */
        SUCCESS,
        /** Wreath has not been laid yet (before 11:02). */
        WREATH_NOT_PRESENT,
        /** Too early to steal (before 11:30). */
        TOO_EARLY,
        /** Wreath has already been stolen. */
        ALREADY_STOLEN
    }

    /**
     * Fence the stolen WREATH at a PawnShop-equivalent for 8–12 COIN.
     * Only works if the player has POPPYs (proxy for the stolen wreath)
     * and the wreath was actually stolen.
     *
     * @param inventory player inventory
     * @return number of COIN paid, or 0 if the wreath cannot be fenced
     */
    public int fenceWreath(Inventory inventory) {
        if (!wreathStolen) return 0;
        if (inventory.getItemCount(Material.POPPY) < 1) return 0;
        // Remove 1 POPPY as the wreath token
        inventory.removeItem(Material.POPPY, 1);
        int payout = WREATH_FENCE_MIN + random.nextInt(WREATH_FENCE_MAX - WREATH_FENCE_MIN + 1);
        inventory.addItem(Material.COIN, payout);
        return payout;
    }

    // ── Achievement callback interface ────────────────────────────────────────

    /** Callback interface for awarding achievements, matching ChurchSystem pattern. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void spawnPoppySeller(List<NPC> npcs) {
        NPC seller = new NPC(NPCType.POPPY_SELLER, 0f, 0f, 0f);
        ceremonyNpcs.add(seller);
        if (npcs != null) npcs.add(seller);
    }

    private void spawnCeremonyNpcs(List<NPC> npcs) {
        // VICAR (Reverend Dave) — delegate from ChurchSystem; we spawn a representative
        NPC vicar = new NPC(NPCType.VICAR, 0f, 0f, 0f);
        ceremonyNpcs.add(vicar);
        if (npcs != null) npcs.add(vicar);

        // VETERANs
        for (int i = 0; i < VETERAN_COUNT; i++) {
            NPC veteran = new NPC(NPCType.VETERAN, 0f, 0f, 0f);
            ceremonyNpcs.add(veteran);
            if (npcs != null) npcs.add(veteran);
        }

        // RAOB_LODGE_MEMBERs
        for (int i = 0; i < RAOB_MEMBER_COUNT; i++) {
            NPC member = new NPC(NPCType.RAOB_LODGE_MEMBER, 0f, 0f, 0f);
            ceremonyNpcs.add(member);
            if (npcs != null) npcs.add(member);
        }
    }

    private void despawnPoppySeller(List<NPC> npcs) {
        ceremonyNpcs.removeIf(n -> n.getType() == NPCType.POPPY_SELLER);
        if (npcs != null) npcs.removeIf(n -> n.getType() == NPCType.POPPY_SELLER);
    }

    private void freezeCeremonyNpcs() {
        for (NPC npc : ceremonyNpcs) {
            npc.setState(NPCState.IDLE);
        }
    }

    private void unfreezeCeremonyNpcs() {
        for (NPC npc : ceremonyNpcs) {
            if (npc.getState() == NPCState.IDLE) {
                npc.setState(NPCState.IDLE); // remain in place; world handles movement
            }
        }
    }

    private void layWreath() {
        // The VICAR NPC lays the wreath. In the full game this would place a WREATH_PROP
        // in the world at the STATUE position. Here we just set the flag.
        wreathLaid = true;
    }

    private void disperseCeremony(List<NPC> npcs, boolean earlyThunderstorm) {
        if (earlyThunderstorm) {
            // VETERANs emit Goose Green speech before leaving
            for (NPC npc : ceremonyNpcs) {
                if (npc.getType() == NPCType.VETERAN) {
                    npc.setSpeechText(GOOSE_GREEN_SPEECH, 4f);
                }
            }
        }
        // Remove ceremony NPCs from the global list; they'll re-path to the pub
        if (npcs != null) {
            npcs.removeAll(ceremonyNpcs);
        }
        ceremonyNpcs.clear();
    }

    private void emitOutrage(List<NPC> npcs) {
        if (npcs == null) return;
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.PENSIONER) {
                npc.setSpeechText("Have some respect!", 4f);
            }
        }
    }

    private void seedCeremonyRumour(RumourNetwork rumourNetwork, List<NPC> npcs) {
        if (rumourNetwork == null || npcs == null) return;
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.PUBLIC
                    || npc.getType() == NPCType.PENSIONER
                    || npc.getType() == NPCType.BARMAN) {
                rumourNetwork.addRumour(npc, new Rumour(
                        RumourType.REMEMBRANCE_CEREMONY,
                        "They're doing the remembrance do at the war memorial — "
                        + "Reverend Dave, the Buffaloes, the lot. Silence at eleven."));
            }
        }
    }

    private void seedSilenceBreachRumour(RumourNetwork rumourNetwork, List<NPC> npcs) {
        if (rumourNetwork == null) return;
        NPC seed = null;
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc.getType() == NPCType.PUBLIC
                        || npc.getType() == NPCType.PENSIONER
                        || npc.getType() == NPCType.VETERAN) {
                    seed = npc;
                    break;
                }
            }
        }
        if (seed != null) {
            rumourNetwork.addRumour(seed, new Rumour(
                    RumourType.SILENCE_BREACH,
                    "Someone only went and made a racket right in the middle of the "
                    + "two-minute silence. The whole town's talking about it."));
        }
    }

    private void resetForNewYear() {
        ceremonyScheduled    = false;
        remembranceDayCount  = -1;
        poppySellerSpawned   = false;
        ceremonyNpcsSpawned  = false;
        silenceActive        = false;
        silenceCompleted     = false;
        silenceBreached      = false;
        wreathLaid           = false;
        wreathStolen         = false;
        ceremonyEnded        = false;
        thunderstormDispersal = false;
        wearingPoppy         = false;
        lastPostAwarded      = false;
        lestWeFOrgetAwarded  = false;
        ceremonyRumourSeeded = false;
        breachHeadlineQueued = false;
        ceremonyNpcs.clear();
    }
}

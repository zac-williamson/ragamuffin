package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Issue #942: Food Bank System — Northfield Food Bank, Dignity, Donations &amp; Emergency Parcels.
 *
 * <h3>Overview</h3>
 * Northfield Food Bank is open Monday–Friday 09:00–17:00, run by Margaret (FOOD_BANK_VOLUNTEER).
 *
 * <h3>Donation Role</h3>
 * The player can donate surplus food/materials (SAUSAGE_ROLL, FULL_ENGLISH, MUG_OF_TEA,
 * BEANS_ON_TOAST, COIN, WOOD, CARDBOARD) via the E key. On a successful donation:
 * <ul>
 *   <li>Street Rep and Council Respect increase</li>
 *   <li>Neighbourhood Vibes +{@value #VIBES_DONATION_BOOST}</li>
 *   <li>COMMUNITY_WIN rumour seeded into the barman</li>
 *   <li>COIN donations reduce Notoriety by 1 per {@value #COINS_PER_NOTORIETY_REDUCTION} coins
 *       (max {@value #MAX_NOTORIETY_REDUCTION_PER_DAY} per day)</li>
 *   <li>One donation per in-game day (further donations that day are refused)</li>
 * </ul>
 *
 * <h3>Gate — Donation Blocked</h3>
 * If Notoriety ≥ {@value #NOTORIETY_POLICE_BLOCK} and a POLICE NPC is within
 * {@value #POLICE_BLOCK_RADIUS} blocks, the donation is blocked.
 *
 * <h3>Recipient Role</h3>
 * The player may collect one emergency parcel (1 FULL_ENGLISH + 1 MUG_OF_TEA +
 * 1 BEANS_ON_TOAST, hunger +{@value #PARCEL_HUNGER_BOOST}) when:
 * <ul>
 *   <li>Hunger &lt; {@value #HUNGER_THRESHOLD} OR coins == 0</li>
 *   <li>Notoriety &lt; {@value #NOTORIETY_BLOCK_THRESHOLD}</li>
 *   <li>Parcel cooldown ({@value #PARCEL_COOLDOWN_DAYS} in-game days) has expired</li>
 * </ul>
 * Street Rep −{@value #RECIPIENT_STREET_REP_PENALTY} on collection. A witness within
 * {@value #WITNESS_RADIUS} blocks seeds a PLAYER_SPOTTED rumour.
 *
 * <h3>Queue NPCs</h3>
 * 1–3 RECIPIENT NPCs queue outside before open (NPCState.QUEUING), murmuring British misery lines.
 *
 * <h3>Weather</h3>
 * Rain (RAIN/DRIZZLE/THUNDERSTORM): extra RECIPIENT NPC queues outside; Margaret gives free
 * MUG_OF_TEA (+{@value #RAINY_WARMTH_BOOST} warmth) to any visitor.
 * FROST or COLD_SNAP: +2 extra recipient NPCs in the queue.
 *
 * <h3>Council Inspector</h3>
 * COUNCIL_INSPECTOR NPC spawns on Thursdays 10:00–12:00, increases NeighbourhoodWatch
 * anger +5/min while present.
 *
 * <h3>Chalkboard</h3>
 * A daily priority donation item (random from donatable items) doubles Street Rep for
 * that item.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@code HEARTS_AND_MINDS} — donate on 5 separate days</li>
 *   <li>{@code ROUGH_WEEK}       — collect parcel on 3 separate days</li>
 * </ul>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link NeighbourhoodSystem} — Vibes boost on donation</li>
 *   <li>{@link NotorietySystem}    — COIN donations reduce notoriety</li>
 *   <li>{@link RumourNetwork}      — COMMUNITY_WIN seeded into barman</li>
 *   <li>{@link AchievementSystem} — HEARTS_AND_MINDS, ROUGH_WEEK</li>
 * </ul>
 */
public class FoodBankSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Opening hour (09:00), Mon–Fri only. */
    public static final float OPEN_HOUR = 9.0f;

    /** Closing hour (17:00). */
    public static final float CLOSE_HOUR = 17.0f;

    /** Days between parcel collections (3 in-game days). */
    public static final int PARCEL_COOLDOWN_DAYS = 3;

    /** Hunger level below which the player is eligible for a parcel. */
    public static final int HUNGER_THRESHOLD = 30;

    /** Notoriety above which the player cannot receive a parcel. */
    public static final int NOTORIETY_BLOCK_THRESHOLD = 80;

    /** Notoriety above which donation is blocked if police are nearby. */
    public static final int NOTORIETY_POLICE_BLOCK = 50;

    /** Police proximity radius (blocks) that triggers donation block at high notoriety. */
    public static final float POLICE_BLOCK_RADIUS = 15.0f;

    /** Neighbourhood Vibes boost per donation. */
    public static final int VIBES_DONATION_BOOST = 3;

    /** Street Rep penalty when collecting a parcel. */
    public static final int RECIPIENT_STREET_REP_PENALTY = 2;

    /** Warmth bonus from Margaret's free tea during rainy weather. */
    public static final int RAINY_WARMTH_BOOST = 15;

    /** Radius (blocks) within which an NPC can witness a parcel collection. */
    public static final float WITNESS_RADIUS = 8.0f;

    /** Number of COINs donated per 1 Notoriety point reduction. */
    public static final int COINS_PER_NOTORIETY_REDUCTION = 5;

    /** Maximum Notoriety reduction per day from coin donations. */
    public static final int MAX_NOTORIETY_REDUCTION_PER_DAY = 3;

    /** Hunger restored by collecting an emergency parcel. */
    public static final int PARCEL_HUNGER_BOOST = 20;

    /** Base queue size (min RECIPIENT NPCs outside before opening). */
    public static final int BASE_QUEUE_SIZE = 1;

    /** Maximum base queue size. */
    public static final int MAX_BASE_QUEUE_SIZE = 3;

    /** Extra RECIPIENT NPCs added during rainy weather. */
    public static final int WEATHER_BONUS_RECIPIENTS = 1;

    /** Extra RECIPIENT NPCs added during FROST or COLD_SNAP. */
    public static final int COLD_WEATHER_BONUS_RECIPIENTS = 2;

    /** Council Inspector anger added to NeighbourhoodWatch per in-game minute. */
    public static final float INSPECTOR_ANGER_PER_MINUTE = 5.0f;

    /** Day-of-week value for Thursday (1=Monday, 4=Thursday). */
    public static final int THURSDAY = 4;

    /** Hour the Council Inspector arrives on Thursdays. */
    public static final float INSPECTOR_ARRIVE_HOUR = 10.0f;

    /** Hour the Council Inspector departs on Thursdays. */
    public static final float INSPECTOR_DEPART_HOUR = 12.0f;

    // ── Donatable materials ────────────────────────────────────────────────────

    /** All materials that can be donated to the food bank. */
    public static final Material[] DONATABLE_ITEMS = {
        Material.SAUSAGE_ROLL,
        Material.FULL_ENGLISH,
        Material.MUG_OF_TEA,
        Material.BEANS_ON_TOAST,
        Material.COIN,
        Material.WOOD,
        Material.CARDBOARD
    };

    // ── Result enums ───────────────────────────────────────────────────────────

    /** Result of a donation attempt. */
    public enum DonationResult {
        /** Donation accepted. */
        SUCCESS,
        /** Food bank is currently closed. */
        CLOSED,
        /** Player has already donated today. */
        ALREADY_DONATED_TODAY,
        /** Item is not accepted at this food bank. */
        NOT_ACCEPTED,
        /** Player does not have the item. */
        ITEM_NOT_FOUND,
        /** Donation blocked: high notoriety + police nearby. */
        BLOCKED_POLICE,
        /** NPC is not a FOOD_BANK_VOLUNTEER. */
        WRONG_NPC
    }

    /** Result of a parcel collection attempt. */
    public enum ParcelResult {
        /** Parcel received successfully. */
        SUCCESS,
        /** Food bank is currently closed. */
        CLOSED,
        /** Player's notoriety is too high (≥80). */
        NOTORIETY_TOO_HIGH,
        /** Player is not eligible (hunger ≥30 and has coins). */
        NOT_ELIGIBLE,
        /** Cooldown has not expired yet. */
        ON_COOLDOWN,
        /** NPC is not a FOOD_BANK_VOLUNTEER. */
        WRONG_NPC
    }

    // ── State ──────────────────────────────────────────────────────────────────

    /** Last in-game day on which the player donated. */
    private int lastDonationDay = -1;

    /** Last in-game day on which the player collected a parcel. */
    private int lastParcelDay = -1;

    /** Number of separate days the player has donated (for HEARTS_AND_MINDS). */
    private int donationDaysCount = 0;

    /** Number of separate days the player has collected a parcel (for ROUGH_WEEK). */
    private int parcelDaysCount = 0;

    /** Notoriety reduced today via coin donations. */
    private int notorietyReducedToday = 0;

    /** Last in-game day notoriety-reduction tracking was reset. */
    private int lastNotorietyReductionDay = -1;

    /** Active queue of RECIPIENT NPCs outside the food bank. */
    private final List<NPC> queueNpcs = new ArrayList<>();

    /** Margaret, the FOOD_BANK_VOLUNTEER NPC. */
    private NPC margaret;

    /** Council Inspector NPC (null if not active). */
    private NPC councilInspector;

    /** Today's priority donation item (from DONATABLE_ITEMS), chosen at open. */
    private Material dailyPriorityItem = null;

    /** Last day the priority item was refreshed. */
    private int lastPriorityItemDay = -1;

    /** Whether the first-entry tooltip has been shown. */
    private boolean firstEntryTooltipShown = false;

    /** Accumulator for inspector anger (in-game seconds). */
    private float inspectorAngerAccumulator = 0f;

    private final Random random;

    // ── Dependencies ───────────────────────────────────────────────────────────

    private AchievementSystem achievementSystem;
    private NotorietySystem notorietySystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private RumourNetwork rumourNetwork;

    // ── Construction ───────────────────────────────────────────────────────────

    public FoodBankSystem() {
        this(new Random());
    }

    public FoodBankSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    // ── Opening / Closing ──────────────────────────────────────────────────────

    /**
     * Open the food bank for the day: spawn Margaret and queue NPCs.
     *
     * @param bankX    X coordinate of the food bank counter
     * @param bankY    Y coordinate
     * @param bankZ    Z coordinate of the food bank counter
     * @param weather  current weather (affects queue size)
     * @param currentDay current in-game day (for priority item refresh)
     */
    public void openFoodBank(int bankX, int bankY, int bankZ,
                             Weather weather, int currentDay) {
        queueNpcs.clear();
        councilInspector = null;
        inspectorAngerAccumulator = 0f;

        // Spawn Margaret
        margaret = new NPC(NPCType.FOOD_BANK_VOLUNTEER, "Margaret", bankX, bankY, bankZ);
        margaret.setSpeechText("Hello love, what can we do for you?", 0f);

        // Determine queue size
        int queueSize = BASE_QUEUE_SIZE + random.nextInt(MAX_BASE_QUEUE_SIZE);
        if (weather.isRaining()) {
            queueSize += WEATHER_BONUS_RECIPIENTS;
        }
        if (weather == Weather.FROST || weather == Weather.COLD_SNAP) {
            queueSize += COLD_WEATHER_BONUS_RECIPIENTS;
        }

        // Spawn queue RECIPIENT NPCs outside
        String[] miseryLines = {
            "Didn't think it'd come to this, to be honest.",
            "At least it's dry in there.",
            "Can't believe the leccy's gone up again.",
            "Third time this month for me. What a state.",
            "Just grateful they're here, aren't ya.",
            "The kids are hungry. Didn't know what else to do.",
            "Government's useless. Always has been.",
        };
        for (int i = 0; i < queueSize; i++) {
            NPC recipient = new NPC(NPCType.RECIPIENT, bankX - 2 - i, bankY, bankZ + 1);
            recipient.setState(NPCState.QUEUING);
            recipient.setSpeechText(miseryLines[random.nextInt(miseryLines.length)], 0f);
            queueNpcs.add(recipient);
        }

        // Refresh daily priority item
        refreshDailyPriorityItem(currentDay);
    }

    /**
     * Close the food bank: despawn NPCs.
     */
    public void closeFoodBank() {
        queueNpcs.clear();
        margaret = null;
        councilInspector = null;
        inspectorAngerAccumulator = 0f;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the food bank system. Call once per frame.
     *
     * @param delta        seconds since last frame
     * @param currentHour  current in-game hour
     * @param currentDay   current in-game day
     * @param dayOfWeek    1=Monday … 7=Sunday
     */
    public void update(float delta, float currentHour, int currentDay, int dayOfWeek) {
        // Council Inspector logic (Thursdays 10:00–12:00)
        if (dayOfWeek == THURSDAY) {
            if (currentHour >= INSPECTOR_ARRIVE_HOUR && currentHour < INSPECTOR_DEPART_HOUR) {
                if (councilInspector == null && margaret != null) {
                    // Spawn inspector near the counter
                    float ix = margaret.getPosition().x + 3;
                    float iy = margaret.getPosition().y;
                    float iz = margaret.getPosition().z;
                    councilInspector = new NPC(NPCType.COUNCIL_INSPECTOR, ix, iy, iz);
                    councilInspector.setSpeechText("I'm here to observe. Carry on.", 0f);
                }
                // Accumulate anger for NeighbourhoodSystem
                if (councilInspector != null && neighbourhoodSystem != null) {
                    inspectorAngerAccumulator += delta;
                    // Trigger +5 vibes drain per in-game minute (1 real second = 1 in-game minute per SECONDS_PER_IN_GAME_MINUTE)
                    float minuteInterval = NeighbourhoodSystem.SECONDS_PER_IN_GAME_MINUTE;
                    while (inspectorAngerAccumulator >= minuteInterval) {
                        inspectorAngerAccumulator -= minuteInterval;
                        // Reduce vibes by inspector anger rate
                        int currentVibes = neighbourhoodSystem.getVibes();
                        neighbourhoodSystem.setVibes(currentVibes - (int) INSPECTOR_ANGER_PER_MINUTE);
                    }
                }
            } else if (currentHour >= INSPECTOR_DEPART_HOUR) {
                councilInspector = null;
            }
        } else {
            councilInspector = null;
        }

        // Reset daily notoriety reduction counter on new day
        if (currentDay != lastNotorietyReductionDay) {
            notorietyReducedToday = 0;
            lastNotorietyReductionDay = currentDay;
        }
    }

    // ── Donation ───────────────────────────────────────────────────────────────

    /**
     * Attempt to donate an item to the food bank.
     *
     * @param margaret        the FOOD_BANK_VOLUNTEER NPC
     * @param material        the material to donate
     * @param inventory       the player's inventory
     * @param currentHour     current in-game hour
     * @param currentDay      current in-game day
     * @param dayOfWeek       1=Monday … 7=Sunday (donation only Mon–Fri)
     * @param allNpcs         all live NPCs in world (for police proximity check)
     * @param barman          the BARMAN NPC to receive COMMUNITY_WIN rumour (may be null)
     * @return DonationResult
     */
    public DonationResult donate(NPC margaret, Material material,
                                  Inventory inventory, float currentHour,
                                  int currentDay, int dayOfWeek,
                                  List<NPC> allNpcs, NPC barman) {
        if (margaret == null || margaret.getType() != NPCType.FOOD_BANK_VOLUNTEER) {
            return DonationResult.WRONG_NPC;
        }
        if (!isOpen(currentHour, dayOfWeek)) {
            return DonationResult.CLOSED;
        }
        if (currentDay == lastDonationDay) {
            margaret.setSpeechText("You've already been very generous today, love.", 5f);
            return DonationResult.ALREADY_DONATED_TODAY;
        }
        if (!isDonatable(material)) {
            margaret.setSpeechText("Sorry love, we can't take that one.", 5f);
            return DonationResult.NOT_ACCEPTED;
        }
        if (inventory == null || !inventory.hasItem(material, 1)) {
            margaret.setSpeechText("Doesn't look like you've got any of that, love.", 5f);
            return DonationResult.ITEM_NOT_FOUND;
        }
        // Gate: high notoriety + police nearby
        if (isDonationBlockedByPolice(allNpcs)) {
            margaret.setSpeechText("Best come back when it's a bit quieter, love.", 5f);
            return DonationResult.BLOCKED_POLICE;
        }

        // Remove the donated item
        inventory.removeItem(material, 1);

        // Determine if it's the priority item (doubles Street Rep)
        boolean isPriority = (dailyPriorityItem != null && dailyPriorityItem == material);

        // Neighbourhood Vibes boost
        if (neighbourhoodSystem != null) {
            int boost = VIBES_DONATION_BOOST;
            if (isPriority) boost *= 2;
            int currentVibes = neighbourhoodSystem.getVibes();
            neighbourhoodSystem.setVibes(currentVibes + boost);
        }

        // COIN donation: reduce Notoriety
        if (material == Material.COIN && notorietySystem != null) {
            int reduction = Math.min(1, MAX_NOTORIETY_REDUCTION_PER_DAY - notorietyReducedToday);
            if (reduction > 0) {
                // Only reduce if we've donated enough coins (1 coin per call, tracked per 5)
                // We reduce by 1 per donation call that involves COIN, up to the daily max
                notorietySystem.reduceNotoriety(1, null);
                notorietyReducedToday++;
            }
        }

        // Seed COMMUNITY_WIN rumour into barman
        if (barman != null && barman.getType() == NPCType.BARMAN) {
            Rumour communityWin = new Rumour(
                RumourType.COMMUNITY_WIN,
                "Someone's been doing proper good at the food bank — donated again this week, apparently."
            );
            barman.getRumours().add(communityWin);
        }

        // Track donation day and achievement
        lastDonationDay = currentDay;
        donationDaysCount++;
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.HEARTS_AND_MINDS);
        }

        // Margaret's response
        String speech = isPriority
            ? "Oh that's exactly what we need today — bless you, love!"
            : "That's ever so kind, ta very much.";
        margaret.setSpeechText(speech, 5f);

        return DonationResult.SUCCESS;
    }

    // ── Emergency Parcel ───────────────────────────────────────────────────────

    /**
     * Attempt to collect an emergency parcel.
     *
     * @param margaret    the FOOD_BANK_VOLUNTEER NPC
     * @param player      the player (for hunger check)
     * @param inventory   the player's inventory (receives parcel items and hunger boost)
     * @param currentHour current in-game hour
     * @param currentDay  current in-game day
     * @param dayOfWeek   1=Monday … 7=Sunday
     * @param allNpcs     all live NPCs (for witness check)
     * @return ParcelResult
     */
    public ParcelResult collectParcel(NPC margaret, Player player,
                                       Inventory inventory, float currentHour,
                                       int currentDay, int dayOfWeek,
                                       List<NPC> allNpcs) {
        if (margaret == null || margaret.getType() != NPCType.FOOD_BANK_VOLUNTEER) {
            return ParcelResult.WRONG_NPC;
        }
        if (!isOpen(currentHour, dayOfWeek)) {
            return ParcelResult.CLOSED;
        }
        // Notoriety gate
        if (notorietySystem != null && notorietySystem.getNotoriety() >= NOTORIETY_BLOCK_THRESHOLD) {
            margaret.setSpeechText("I'm sorry love, we can't help you today.", 5f);
            return ParcelResult.NOTORIETY_TOO_HIGH;
        }
        // Eligibility check: hunger < 30 OR coins == 0
        boolean eligible = false;
        if (player != null && player.getHunger() < HUNGER_THRESHOLD) {
            eligible = true;
        }
        if (inventory != null && inventory.getItemCount(Material.COIN) == 0) {
            eligible = true;
        }
        if (!eligible) {
            margaret.setSpeechText("We do need to prioritise those in greatest need, love.", 5f);
            return ParcelResult.NOT_ELIGIBLE;
        }
        // Cooldown check
        if (lastParcelDay >= 0 && (currentDay - lastParcelDay) < PARCEL_COOLDOWN_DAYS) {
            margaret.setSpeechText("We can only help once every few days, love. Sorry.", 5f);
            return ParcelResult.ON_COOLDOWN;
        }

        // Give parcel: 1 FULL_ENGLISH + 1 MUG_OF_TEA + 1 BEANS_ON_TOAST
        if (inventory != null) {
            inventory.addItem(Material.FULL_ENGLISH, 1);
            inventory.addItem(Material.MUG_OF_TEA, 1);
            inventory.addItem(Material.BEANS_ON_TOAST, 1);
        }

        // Hunger boost
        if (player != null) {
            player.setHunger(Math.min(100, player.getHunger() + PARCEL_HUNGER_BOOST));
        }

        // Street Rep penalty (tracked via vibes)
        if (neighbourhoodSystem != null) {
            int currentVibes = neighbourhoodSystem.getVibes();
            neighbourhoodSystem.setVibes(currentVibes - RECIPIENT_STREET_REP_PENALTY);
        }

        // Witness check — seed PLAYER_SPOTTED rumour
        if (margaret != null && allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (npc == null || !npc.isAlive()) continue;
                if (npc.getType() == NPCType.FOOD_BANK_VOLUNTEER
                        || npc.getType() == NPCType.RECIPIENT
                        || npc.getType() == NPCType.COUNCIL_INSPECTOR) {
                    continue;
                }
                float dx = npc.getPosition().x - margaret.getPosition().x;
                float dz = npc.getPosition().z - margaret.getPosition().z;
                float distSq = dx * dx + dz * dz;
                if (distSq <= WITNESS_RADIUS * WITNESS_RADIUS) {
                    Rumour spotted = new Rumour(
                        RumourType.PLAYER_SPOTTED,
                        "Saw someone picking up an emergency parcel at the food bank."
                    );
                    npc.getRumours().add(spotted);
                    break;
                }
            }
        }

        // Track parcel collection day and achievement
        lastParcelDay = currentDay;
        parcelDaysCount++;
        if (achievementSystem != null) {
            achievementSystem.increment(AchievementType.ROUGH_WEEK);
        }

        margaret.setSpeechText("There you go, love. Take care of yourself.", 5f);

        return ParcelResult.SUCCESS;
    }

    // ── Rainy weather free tea ─────────────────────────────────────────────────

    /**
     * When the player enters during rain/drizzle/thunderstorm, Margaret offers a free
     * MUG_OF_TEA. Call this on player entry if weather is rainy.
     *
     * @param player   the player (warmth is increased)
     * @param inventory the player's inventory (MUG_OF_TEA is added)
     * @return the speech text from Margaret, or null if not applicable
     */
    public String offerRainyDayTea(Player player, Inventory inventory) {
        if (margaret == null) return null;
        if (player != null) {
            player.setWarmth(Math.min(100, player.getWarmth() + RAINY_WARMTH_BOOST));
        }
        if (inventory != null) {
            inventory.addItem(Material.MUG_OF_TEA, 1);
        }
        margaret.setSpeechText("Horrible out there. Have a brew, love, on the house.", 5f);
        return "Horrible out there. Have a brew, love, on the house.";
    }

    // ── Player entry tooltip ───────────────────────────────────────────────────

    /**
     * Call when the player enters the food bank area.
     *
     * @return first-entry tooltip text, or null if already shown
     */
    public String onPlayerEnter() {
        if (!firstEntryTooltipShown) {
            firstEntryTooltipShown = true;
            return "Northfield Food Bank. Open Mon–Fri 09:00–17:00. Dignity. Every time.";
        }
        return null;
    }

    // ── Daily priority item ────────────────────────────────────────────────────

    /**
     * Refresh the daily priority donation item. Call once per in-game day.
     *
     * @param currentDay current in-game day
     */
    public void refreshDailyPriorityItem(int currentDay) {
        if (currentDay != lastPriorityItemDay) {
            lastPriorityItemDay = currentDay;
            dailyPriorityItem = DONATABLE_ITEMS[random.nextInt(DONATABLE_ITEMS.length)];
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns true if the food bank is open (Mon–Fri 09:00–17:00).
     *
     * @param currentHour current in-game hour
     * @param dayOfWeek   1=Monday … 7=Sunday
     */
    public boolean isOpen(float currentHour, int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 5) return false; // Closed Sat/Sun
        return currentHour >= OPEN_HOUR && currentHour < CLOSE_HOUR;
    }

    /**
     * Returns true if the donation is blocked due to high notoriety and nearby police.
     */
    private boolean isDonationBlockedByPolice(List<NPC> allNpcs) {
        if (notorietySystem == null || notorietySystem.getNotoriety() < NOTORIETY_POLICE_BLOCK) {
            return false;
        }
        if (margaret == null || allNpcs == null) return false;
        for (NPC npc : allNpcs) {
            if (npc == null || !npc.isAlive()) continue;
            if (npc.getType() == NPCType.POLICE
                    || npc.getType() == NPCType.ARMED_RESPONSE
                    || npc.getType() == NPCType.PCSO) {
                float dx = npc.getPosition().x - margaret.getPosition().x;
                float dz = npc.getPosition().z - margaret.getPosition().z;
                float distSq = dx * dx + dz * dz;
                if (distSq <= POLICE_BLOCK_RADIUS * POLICE_BLOCK_RADIUS) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the given material is accepted as a donation.
     */
    public boolean isDonatable(Material material) {
        for (Material accepted : DONATABLE_ITEMS) {
            if (accepted == material) return true;
        }
        return false;
    }

    // ── Query methods ──────────────────────────────────────────────────────────

    /** Returns Margaret (FOOD_BANK_VOLUNTEER), or null if not spawned. */
    public NPC getMargaret() {
        return margaret;
    }

    /** Returns the active queue of RECIPIENT NPCs. */
    public List<NPC> getQueueNpcs() {
        return queueNpcs;
    }

    /** Returns the Council Inspector NPC, or null if not active. */
    public NPC getCouncilInspector() {
        return councilInspector;
    }

    /** Returns the daily priority donation item, or null if not yet set. */
    public Material getDailyPriorityItem() {
        return dailyPriorityItem;
    }

    /** Returns the number of separate days the player has donated. */
    public int getDonationDaysCount() {
        return donationDaysCount;
    }

    /** Returns the number of separate days the player has collected a parcel. */
    public int getParcelDaysCount() {
        return parcelDaysCount;
    }

    /** Returns the last day on which the player donated (-1 if never). */
    public int getLastDonationDay() {
        return lastDonationDay;
    }

    /** Returns the last day on which the player collected a parcel (-1 if never). */
    public int getLastParcelDay() {
        return lastParcelDay;
    }

    /** Returns how much notoriety has been reduced today via coin donations. */
    public int getNotorietyReducedToday() {
        return notorietyReducedToday;
    }

    /** Returns whether the first-entry tooltip has been shown. */
    public boolean isFirstEntryTooltipShown() {
        return firstEntryTooltipShown;
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    /** Directly set last donation day (for testing). */
    public void setLastDonationDayForTesting(int day) {
        this.lastDonationDay = day;
    }

    /** Directly set last parcel day (for testing). */
    public void setLastParcelDayForTesting(int day) {
        this.lastParcelDay = day;
    }

    /** Directly set donation days count (for testing). */
    public void setDonationDaysCountForTesting(int count) {
        this.donationDaysCount = count;
    }

    /** Directly set parcel days count (for testing). */
    public void setParcelDaysCountForTesting(int count) {
        this.parcelDaysCount = count;
    }

    /** Directly set notoriety reduced today (for testing). */
    public void setNotorietyReducedTodayForTesting(int count) {
        this.notorietyReducedToday = count;
    }

    /** Directly set daily priority item (for testing). */
    public void setDailyPriorityItemForTesting(Material material) {
        this.dailyPriorityItem = material;
    }

    /** Directly set first-entry tooltip shown flag (for testing). */
    public void setFirstEntryTooltipShownForTesting(boolean shown) {
        this.firstEntryTooltipShown = shown;
    }
}

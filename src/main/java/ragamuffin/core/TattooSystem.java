package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1453: Northfield Skin Deep Tattoos — Kev's Flash Sheets,
 * the Prison Tattoo Parlour &amp; the Health Inspector Bust.
 *
 * <h3>Mechanic 1 — Getting Tattooed</h3>
 * <ul>
 *   <li>Press E on {@code TATTOO_CHAIR_PROP} (Tue–Sat 11:00–18:00) to open flash sheet menu.</li>
 *   <li>BULLDOG (5 COIN, −5 effective Notoriety for 2 in-game days).</li>
 *   <li>TEARDROP (8 COIN, gang NPCs non-hostile for 3 days + POLICE Suspicion +10).</li>
 *   <li>MUM (3 COIN, Community Respect +2).</li>
 *   <li>NORTHFIELD_4_EVER (6 COIN, permanent StreetRep +5).</li>
 *   <li>Kev refuses Notoriety Tier ≥ 4.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — DIY Prison Tattoo</h3>
 * <ul>
 *   <li>Press E on {@code MIRROR_PROP} with NEEDLE + INK_BOTTLE.</li>
 *   <li>60% success → Teardrop effect free; awards {@link AchievementType#PRISON_TATTOO}.</li>
 *   <li>Failure → −10 HP + {@code INFECTED_WOUND} debuff; awards {@link AchievementType#DODGY_BIRO}.</li>
 *   <li>Witnessed by HEALTH_INSPECTOR → {@link CrimeType#UNLICENSED_TATTOOING} crime
 *       + Notoriety +8.</li>
 *   <li>Seeds {@link RumourType#PRISON_TATTOO_RUMOUR} regardless of outcome.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Tattoo Gun Heist</h3>
 * <ul>
 *   <li>After-hours break-in (closed hours: 18:00–11:00).</li>
 *   <li>{@link Material#TATTOO_GUN} fence value {@value #TATTOO_GUN_FENCE_VALUE} COIN.</li>
 *   <li>Craft {@link Material#TATTOO_GUN_KIT} from TATTOO_GUN + INK_BOTTLE + NEEDLE
 *       for unlimited risk-free DIY tattoos.</li>
 *   <li>CCTV camera monitors display area; seeds {@link RumourType#TATTOO_HEIST} on theft.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Health Inspector Bust</h3>
 * <ul>
 *   <li>Every {@value #INSPECTOR_VISIT_INTERVAL_DAYS} in-game days, HEALTH_INSPECTOR NPC
 *       visits during open hours.</li>
 *   <li>Player can bribe with {@link Material#BROWN_ENVELOPE} (10 COIN) for a free tattoo
 *       from Kev; awards {@link AchievementType#BACKHANDER}.</li>
 *   <li>Player can tip off council early to force closure and earn Community Respect;
 *       awards {@link AchievementType#GRASS_THE_TATTOOIST}.</li>
 *   <li>Dirty station → Kev closed 2 days + {@link RumourType#HEALTH_SCARE} rumour
 *       + {@link RumourType#TATTOO_PARLOUR_BUST} rumour + NewspaperSystem headline.</li>
 * </ul>
 */
public class TattooSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour Skin Deep Tattoos opens (Tue–Sat, 11:00). */
    public static final float OPEN_HOUR = 11.0f;

    /** Hour Skin Deep Tattoos closes (18:00). */
    public static final float CLOSE_HOUR = 18.0f;

    /** Day-of-week (dayCount % 7) index for Monday (closed). */
    public static final int DAY_MONDAY = 1;

    /** Day-of-week (dayCount % 7) index for Sunday (closed). */
    public static final int DAY_SUNDAY = 0;

    // ── Tattoo prices (COIN) ──────────────────────────────────────────────────

    /** Price of the BULLDOG design (COIN). */
    public static final int PRICE_BULLDOG = 5;

    /** Price of the TEARDROP design (COIN). */
    public static final int PRICE_TEARDROP = 8;

    /** Price of the MUM design (COIN). */
    public static final int PRICE_MUM = 3;

    /** Price of the NORTHFIELD_4_EVER design (COIN). */
    public static final int PRICE_NORTHFIELD_4_EVER = 6;

    // ── Notoriety / effects ───────────────────────────────────────────────────

    /** Effective Notoriety reduction from BULLDOG tattoo (displayed reduction). */
    public static final int BULLDOG_NOTORIETY_REDUCTION = 5;

    /** In-game days BULLDOG Notoriety reduction remains active. */
    public static final int BULLDOG_EFFECT_DAYS = 2;

    /** In-game days gang NPCs are non-hostile after TEARDROP tattoo. */
    public static final int TEARDROP_GANG_PEACE_DAYS = 3;

    /** POLICE Suspicion added by TEARDROP tattoo. */
    public static final int TEARDROP_POLICE_SUSPICION = 10;

    /** Community Respect bonus from MUM tattoo. */
    public static final int MUM_COMMUNITY_RESPECT = 2;

    /** StreetRep points bonus from NORTHFIELD_4_EVER tattoo (permanent). */
    public static final int NORTHFIELD_STREET_REP = 5;

    /** Notoriety tier at or above which Kev refuses service. */
    public static final int KEV_REFUSAL_TIER = 4;

    // ── Prison tattoo ─────────────────────────────────────────────────────────

    /** Probability (0–1) of a successful DIY prison tattoo. */
    public static final float PRISON_TATTOO_SUCCESS_CHANCE = 0.60f;

    /** HP penalty on failed prison tattoo attempt. */
    public static final int PRISON_TATTOO_FAIL_HP_LOSS = 10;

    /** Notoriety added when HEALTH_INSPECTOR witnesses unlicensed tattooing. */
    public static final int UNLICENSED_TATTOO_NOTORIETY = 8;

    // ── Tattoo gun heist ──────────────────────────────────────────────────────

    /** Fence value (COIN) for the TATTOO_GUN. */
    public static final int TATTOO_GUN_FENCE_VALUE = 12;

    // ── Health Inspector ──────────────────────────────────────────────────────

    /** In-game days between Health Inspector visits. */
    public static final int INSPECTOR_VISIT_INTERVAL_DAYS = 5;

    /** COIN cost of bribing the Health Inspector with a BROWN_ENVELOPE. */
    public static final int BRIBE_COST = 10;

    /** In-game days Skin Deep is closed after a failed health inspection. */
    public static final int CLOSURE_DAYS = 2;

    // ── Community respect ─────────────────────────────────────────────────────

    /** Community Respect points awarded for tipping off the council. */
    public static final int TIP_OFF_COMMUNITY_RESPECT = 3;

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String KEV_GREETING        = "Alright mate. What you fancy?";
    public static final String KEV_CLOSED          = "We're shut. Come back Tuesday.";
    public static final String KEV_NO_COIN         = "You're a bit short there, pal.";
    public static final String KEV_REFUSED         = "Nah. Not with a rep like yours. Come back when you've sorted yourself out.";
    public static final String KEV_BULLDOG_DONE    = "Nice. Classic bulldog. Very Northfield.";
    public static final String KEV_TEARDROP_DONE   = "Teardrop. Respect.";
    public static final String KEV_MUM_DONE        = "There you go. She'll be made up.";
    public static final String KEV_NORTHFIELD_DONE = "That's commitment, that is. Northfield 4 Ever.";
    public static final String KEV_BRIBED          = "Say no more. I'll sort you a freebie after.";
    public static final String KEV_HEALTH_BUST     = "...That's it. We're done. Two days minimum.";

    // ── Flash sheet designs ───────────────────────────────────────────────────

    /**
     * The four designs available on Kev's flash sheet.
     */
    public enum TattooDesign {
        BULLDOG,
        TEARDROP,
        MUM,
        NORTHFIELD_4_EVER
    }

    // ── Result enums ──────────────────────────────────────────────────────────

    /**
     * Result of attempting to get a tattoo from Kev.
     */
    public enum TattooResult {
        /** Tattoo applied successfully. */
        SUCCESS,
        /** Skin Deep is closed (wrong day or out of hours). */
        SHOP_CLOSED,
        /** Player does not have enough COIN. */
        NO_COIN,
        /** Kev refuses due to Notoriety Tier ≥ 4. */
        REFUSED_HIGH_NOTORIETY
    }

    /**
     * Result of a DIY prison tattoo attempt at MIRROR_PROP.
     */
    public enum PrisonTattooResult {
        /** Tattoo applied successfully — Teardrop effect granted. */
        SUCCESS,
        /** Attempt failed — −10 HP and INFECTED_WOUND debuff applied. */
        FAILURE_INFECTED,
        /** Player does not have NEEDLE + INK_BOTTLE. */
        MISSING_MATERIALS,
        /** Attempt witnessed by HEALTH_INSPECTOR — crime logged + Notoriety +8. */
        WITNESSED_BY_INSPECTOR
    }

    /**
     * Result of stealing the TATTOO_GUN after hours.
     */
    public enum HeistResult {
        /** TATTOO_GUN added to inventory. */
        SUCCESS,
        /** Shop is open (cannot break in during open hours). */
        SHOP_IS_OPEN,
        /** TATTOO_GUN already stolen. */
        ALREADY_LOOTED
    }

    /**
     * Result of crafting TATTOO_GUN_KIT.
     */
    public enum CraftKitResult {
        /** TATTOO_GUN_KIT crafted. */
        SUCCESS,
        /** Missing one or more required materials (TATTOO_GUN + INK_BOTTLE + NEEDLE). */
        MISSING_MATERIALS
    }

    /**
     * Result of a Health Inspector bribe attempt.
     */
    public enum BribeResult {
        /** Inspector bribed — free tattoo granted; achievement awarded. */
        SUCCESS,
        /** Player does not have a BROWN_ENVELOPE or enough COIN. */
        NO_BROWN_ENVELOPE,
        /** Inspector is not currently visiting. */
        INSPECTOR_NOT_PRESENT
    }

    /**
     * Result of tipping off the council about the health inspection.
     */
    public enum TipOffResult {
        /** Council notified — Kev closed early; Community Respect +3. */
        SUCCESS,
        /** Inspector is not currently visiting. */
        INSPECTOR_NOT_PRESENT
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Kev the tattooist NPC (null if not yet spawned). */
    private NPC kev = null;

    /** Whether FIRST_INK has been awarded. */
    private boolean firstInkAwarded = false;

    /** Number of unique designs received from Kev. Tracks FULL_SLEEVE progress. */
    private int uniqueDesignsReceived = 0;

    /** Bitmask of TattooDesign ordinals that have been purchased from Kev. */
    private int designsPurchased = 0;

    /** Whether the player currently has the BULLDOG notoriety reduction active. */
    private boolean bulldogEffectActive = false;

    /** In-game day when BULLDOG effect expires. */
    private int bulldogEffectExpiryDay = -1;

    /** Whether gang NPCs are currently non-hostile (from TEARDROP). */
    private boolean tearDropGangPeaceActive = false;

    /** In-game day when TEARDROP gang peace expires. */
    private int tearDropGangPeaceExpiryDay = -1;

    /** Whether the TATTOO_GUN has been stolen. */
    private boolean tattooGunLooted = false;

    /** Whether the Health Inspector is currently visiting. */
    private boolean inspectorVisiting = false;

    /** Day of the last Health Inspector visit. */
    private int lastInspectorVisitDay = -1;

    /** Whether Skin Deep is currently closed due to a health bust. */
    private boolean closedForHealthBust = false;

    /** In-game day when the health-bust closure ends. */
    private int closureEndDay = -1;

    /** Whether the player has an INFECTED_WOUND debuff. */
    private boolean infectedWoundActive = false;

    // ── Optional system references ────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;
    private NewspaperSystem newspaperSystem;
    private NeighbourhoodWatchSystem neighbourhoodWatchSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public TattooSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ──────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setNeighbourhoodWatchSystem(NeighbourhoodWatchSystem neighbourhoodWatchSystem) {
        this.neighbourhoodWatchSystem = neighbourhoodWatchSystem;
    }

    // ── NPC management ────────────────────────────────────────────────────────

    /** Force-spawn Kev for testing. */
    public void forceSpawnKev() {
        kev = new NPC(NPCType.TATTOOIST, 0f, 0f, 0f);
        kev.setName("Kev");
    }

    /** Returns the Kev NPC (may be null if not yet spawned). */
    public NPC getKev() {
        return kev;
    }

    // ── Opening hours logic ───────────────────────────────────────────────────

    /**
     * Returns true if Skin Deep Tattoos is open (Tue–Sat 11:00–18:00).
     *
     * @param hour       current in-game hour (0–24)
     * @param dayOfWeek  computed as {@code timeSystem.getDayCount() % 7} where 0=Sunday, 1=Monday
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (closedForHealthBust) return false;
        if (dayOfWeek == DAY_SUNDAY || dayOfWeek == DAY_MONDAY) return false;
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if it is after hours (the shop is closed for break-in purposes).
     *
     * @param hour       current in-game hour
     * @param dayOfWeek  day-of-week value
     */
    public boolean isAfterHours(float hour, int dayOfWeek) {
        return !isOpen(hour, dayOfWeek);
    }

    // ── Mechanic 1: Getting Tattooed ──────────────────────────────────────────

    /**
     * Player presses E on TATTOO_CHAIR_PROP to request a tattoo from Kev.
     *
     * @param design         the chosen flash sheet design
     * @param inventory      player's inventory
     * @param hour           current in-game hour
     * @param dayOfWeek      {@code timeSystem.getDayCount() % 7}
     * @param notorietyTier  player's current Notoriety tier (0–5)
     * @param currentDay     current in-game day count
     * @param achievementCb  callback for awarding achievements (may be null)
     * @param npcs           active NPCs for rumour seeding (may be null)
     */
    public TattooResult getTattoo(
            TattooDesign design,
            Inventory inventory,
            float hour,
            int dayOfWeek,
            int notorietyTier,
            int currentDay,
            NotorietySystem.AchievementCallback achievementCb,
            List<NPC> npcs) {

        if (!isOpen(hour, dayOfWeek)) {
            return TattooResult.SHOP_CLOSED;
        }
        if (notorietyTier >= KEV_REFUSAL_TIER) {
            return TattooResult.REFUSED_HIGH_NOTORIETY;
        }

        int price = getPrice(design);
        if (inventory.getItemCount(Material.COIN) < price) {
            return TattooResult.NO_COIN;
        }

        // Deduct payment
        inventory.removeItem(Material.COIN, price);

        // Apply effects
        applyTattooEffect(design, currentDay, achievementCb);

        // Track unique designs for FULL_SLEEVE achievement
        int designBit = 1 << design.ordinal();
        if ((designsPurchased & designBit) == 0) {
            designsPurchased |= designBit;
            uniqueDesignsReceived++;
        }

        // Award FIRST_INK
        if (!firstInkAwarded) {
            firstInkAwarded = true;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.FIRST_INK);
            }
            if (achievementCb != null) {
                achievementCb.award(AchievementType.FIRST_INK);
            }
        }

        // Award FULL_SLEEVE when all 4 designs collected
        if (uniqueDesignsReceived >= TattooDesign.values().length) {
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.FULL_SLEEVE);
            }
            if (achievementCb != null) {
                achievementCb.award(AchievementType.FULL_SLEEVE);
            }
        }

        return TattooResult.SUCCESS;
    }

    /**
     * Returns the COIN cost for the given flash sheet design.
     *
     * @param design the chosen design
     */
    public int getPrice(TattooDesign design) {
        switch (design) {
            case BULLDOG:          return PRICE_BULLDOG;
            case TEARDROP:         return PRICE_TEARDROP;
            case MUM:              return PRICE_MUM;
            case NORTHFIELD_4_EVER: return PRICE_NORTHFIELD_4_EVER;
            default:               return PRICE_MUM;
        }
    }

    private void applyTattooEffect(
            TattooDesign design,
            int currentDay,
            NotorietySystem.AchievementCallback achievementCb) {

        switch (design) {
            case BULLDOG:
                bulldogEffectActive = true;
                bulldogEffectExpiryDay = currentDay + BULLDOG_EFFECT_DAYS;
                break;
            case TEARDROP:
                tearDropGangPeaceActive = true;
                tearDropGangPeaceExpiryDay = currentDay + TEARDROP_GANG_PEACE_DAYS;
                // Police Suspicion +10: represented as Notoriety increase
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(TEARDROP_POLICE_SUSPICION, achievementCb);
                }
                break;
            case MUM:
                if (neighbourhoodWatchSystem != null) {
                    int currentVibes = neighbourhoodWatchSystem.getVibes();
                    neighbourhoodWatchSystem.setVibes(currentVibes + MUM_COMMUNITY_RESPECT);
                }
                break;
            case NORTHFIELD_4_EVER:
                // Permanent StreetRep +5: no decay needed
                break;
            default:
                break;
        }
    }

    // ── Mechanic 2: DIY Prison Tattoo ─────────────────────────────────────────

    /**
     * Player presses E on MIRROR_PROP to attempt a DIY prison tattoo.
     * Requires NEEDLE + INK_BOTTLE in inventory.
     *
     * @param inventory          player's inventory
     * @param healthInspectorNearby whether the HEALTH_INSPECTOR NPC has line-of-sight
     * @param currentDay         current in-game day count
     * @param achievementCb      callback for achievements (may be null)
     * @param npcs               active NPCs for rumour seeding (may be null)
     */
    public PrisonTattooResult attemptPrisonTattoo(
            Inventory inventory,
            boolean healthInspectorNearby,
            int currentDay,
            NotorietySystem.AchievementCallback achievementCb,
            List<NPC> npcs) {

        // Check materials
        if (inventory.getItemCount(Material.NEEDLE) < 1
                || inventory.getItemCount(Material.INK_BOTTLE) < 1) {
            return PrisonTattooResult.MISSING_MATERIALS;
        }

        // Consume materials
        inventory.removeItem(Material.NEEDLE, 1);
        inventory.removeItem(Material.INK_BOTTLE, 1);

        // Seed prison tattoo rumour regardless of outcome
        seedRumour(RumourType.PRISON_TATTOO_RUMOUR,
                "Heard someone gave themselves a tattoo in a public toilet. Class.", npcs);

        // Witnessed by health inspector?
        if (healthInspectorNearby) {
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.UNLICENSED_TATTOOING);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(UNLICENSED_TATTOO_NOTORIETY, achievementCb);
            }
            return PrisonTattooResult.WITNESSED_BY_INSPECTOR;
        }

        // 60% success
        if (random.nextFloat() < PRISON_TATTOO_SUCCESS_CHANCE) {
            // Apply Teardrop effect free
            tearDropGangPeaceActive = true;
            tearDropGangPeaceExpiryDay = currentDay + TEARDROP_GANG_PEACE_DAYS;

            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.PRISON_TATTOO);
            }
            if (achievementCb != null) {
                achievementCb.award(AchievementType.PRISON_TATTOO);
            }
            return PrisonTattooResult.SUCCESS;
        } else {
            // Failure: infected wound debuff
            infectedWoundActive = true;

            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.DODGY_BIRO);
            }
            if (achievementCb != null) {
                achievementCb.award(AchievementType.DODGY_BIRO);
            }
            return PrisonTattooResult.FAILURE_INFECTED;
        }
    }

    /**
     * Returns the HP loss on a failed prison tattoo attempt.
     * Callers should apply this to the player's HP when result is FAILURE_INFECTED.
     */
    public int getPrisonTattooFailHpLoss() {
        return PRISON_TATTOO_FAIL_HP_LOSS;
    }

    // ── Mechanic 3: Tattoo Gun Heist ──────────────────────────────────────────

    /**
     * Player attempts to steal the TATTOO_GUN after hours.
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  {@code timeSystem.getDayCount() % 7}
     * @param npcs       active NPCs for rumour seeding (may be null)
     */
    public HeistResult stealTattooGun(
            Inventory inventory,
            float hour,
            int dayOfWeek,
            List<NPC> npcs) {

        if (isOpen(hour, dayOfWeek)) {
            return HeistResult.SHOP_IS_OPEN;
        }
        if (tattooGunLooted) {
            return HeistResult.ALREADY_LOOTED;
        }

        tattooGunLooted = true;
        inventory.addItem(Material.TATTOO_GUN, 1);

        // Seed heist rumour
        seedRumour(RumourType.TATTOO_HEIST,
                "Someone broke into Skin Deep last night — Kev's tattoo gun's gone.", npcs);

        return HeistResult.SUCCESS;
    }

    /**
     * Player attempts to craft TATTOO_GUN_KIT from TATTOO_GUN + INK_BOTTLE + NEEDLE.
     *
     * @param inventory player's inventory
     */
    public CraftKitResult craftTattooGunKit(Inventory inventory) {
        if (inventory.getItemCount(Material.TATTOO_GUN) < 1
                || inventory.getItemCount(Material.INK_BOTTLE) < 1
                || inventory.getItemCount(Material.NEEDLE) < 1) {
            return CraftKitResult.MISSING_MATERIALS;
        }

        inventory.removeItem(Material.TATTOO_GUN, 1);
        inventory.removeItem(Material.INK_BOTTLE, 1);
        inventory.removeItem(Material.NEEDLE, 1);
        inventory.addItem(Material.TATTOO_GUN_KIT, 1);

        return CraftKitResult.SUCCESS;
    }

    // ── Mechanic 4: Health Inspector Bust ────────────────────────────────────

    /**
     * Trigger the Health Inspector visit. Called by the per-frame update
     * every {@value #INSPECTOR_VISIT_INTERVAL_DAYS} in-game days.
     *
     * @param currentDay the current in-game day count
     */
    public void triggerInspectorVisit(int currentDay) {
        inspectorVisiting = true;
        lastInspectorVisitDay = currentDay;
    }

    /**
     * Player offers a BROWN_ENVELOPE (10 COIN) to bribe the Health Inspector.
     *
     * @param inventory      player's inventory
     * @param achievementCb  callback for achievements (may be null)
     */
    public BribeResult bribeHealthInspector(
            Inventory inventory,
            NotorietySystem.AchievementCallback achievementCb) {

        if (!inspectorVisiting) {
            return BribeResult.INSPECTOR_NOT_PRESENT;
        }
        if (inventory.getItemCount(Material.BROWN_ENVELOPE) < 1) {
            return BribeResult.NO_BROWN_ENVELOPE;
        }

        inventory.removeItem(Material.BROWN_ENVELOPE, 1);
        inspectorVisiting = false;

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.BACKHANDER);
        }
        if (achievementCb != null) {
            achievementCb.award(AchievementType.BACKHANDER);
        }

        return BribeResult.SUCCESS;
    }

    /**
     * Player tips off the council about the Health Inspector visit to force
     * closure early and earn Community Respect.
     *
     * @param achievementCb  callback for achievements (may be null)
     * @param npcs           active NPCs for rumour seeding (may be null)
     */
    public TipOffResult tipOffCouncil(
            NotorietySystem.AchievementCallback achievementCb,
            List<NPC> npcs) {

        if (!inspectorVisiting) {
            return TipOffResult.INSPECTOR_NOT_PRESENT;
        }

        inspectorVisiting = false;

        // Community Respect bonus
        if (neighbourhoodWatchSystem != null) {
            int currentVibes = neighbourhoodWatchSystem.getVibes();
            neighbourhoodWatchSystem.setVibes(currentVibes + TIP_OFF_COMMUNITY_RESPECT);
        }

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.GRASS_THE_TATTOOIST);
        }
        if (achievementCb != null) {
            achievementCb.award(AchievementType.GRASS_THE_TATTOOIST);
        }

        return TipOffResult.SUCCESS;
    }

    /**
     * Health Inspector finds a dirty station. Triggers bust:
     * Kev closed 2 days, HEALTH_SCARE + TATTOO_PARLOUR_BUST rumours seeded,
     * newspaper headline published.
     *
     * @param currentDay the current in-game day count
     * @param npcs       active NPCs for rumour seeding (may be null)
     */
    public void triggerHealthBust(int currentDay, List<NPC> npcs) {
        inspectorVisiting = false;
        closedForHealthBust = true;
        closureEndDay = currentDay + CLOSURE_DAYS;

        seedRumour(RumourType.HEALTH_SCARE,
                "The health inspector's been round Skin Deep — reckon Kev's in trouble.", npcs);
        seedRumour(RumourType.TATTOO_PARLOUR_BUST,
                "Health inspector shut Skin Deep Tattoos down — dirty station, apparently.", npcs);

        if (newspaperSystem != null) {
            newspaperSystem.publishHeadline(
                    "Northfield tattoo parlour shut after health inspection — Skin Deep closed pending review");
        }
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Handles Health Inspector visit scheduling and effect expiry.
     *
     * @param delta      time since last frame (seconds)
     * @param timeSystem the current time system
     * @param npcs       active NPCs for rumour seeding
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> npcs) {
        int currentDay = timeSystem.getDayCount();
        float hour = timeSystem.getTime();
        int dayOfWeek = currentDay % 7;

        // Expire BULLDOG notoriety reduction
        if (bulldogEffectActive && currentDay >= bulldogEffectExpiryDay) {
            bulldogEffectActive = false;
        }

        // Expire TEARDROP gang peace
        if (tearDropGangPeaceActive && currentDay >= tearDropGangPeaceExpiryDay) {
            tearDropGangPeaceActive = false;
        }

        // Reopen after health bust closure
        if (closedForHealthBust && currentDay >= closureEndDay) {
            closedForHealthBust = false;
        }

        // Schedule Health Inspector visits every INSPECTOR_VISIT_INTERVAL_DAYS
        if (!inspectorVisiting
                && currentDay > 0
                && (currentDay - Math.max(0, lastInspectorVisitDay)) >= INSPECTOR_VISIT_INTERVAL_DAYS
                && isOpen(hour, dayOfWeek)) {
            triggerInspectorVisit(currentDay);
        }
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    private void seedRumour(RumourType type, String text, List<NPC> npcs) {
        if (rumourNetwork == null || npcs == null || npcs.isEmpty()) return;
        NPC seed = findAnyNPC(npcs);
        if (seed != null) {
            rumourNetwork.addRumour(seed, new Rumour(type, text));
        }
    }

    private NPC findAnyNPC(List<NPC> npcs) {
        if (npcs == null || npcs.isEmpty()) return null;
        for (NPC npc : npcs) {
            if (npc != null) return npc;
        }
        return null;
    }

    // ── Accessors for testing ─────────────────────────────────────────────────

    /** Returns whether FIRST_INK has been awarded. */
    public boolean isFirstInkAwarded() {
        return firstInkAwarded;
    }

    /** Returns the number of unique designs purchased from Kev. */
    public int getUniqueDesignsReceived() {
        return uniqueDesignsReceived;
    }

    /** Returns whether the BULLDOG notoriety reduction effect is active. */
    public boolean isBulldogEffectActive() {
        return bulldogEffectActive;
    }

    /** Returns whether the TEARDROP gang peace effect is active. */
    public boolean isTearDropGangPeaceActive() {
        return tearDropGangPeaceActive;
    }

    /** Returns whether the TATTOO_GUN has been stolen. */
    public boolean isTattooGunLooted() {
        return tattooGunLooted;
    }

    /** Returns whether the Health Inspector is currently visiting. */
    public boolean isInspectorVisiting() {
        return inspectorVisiting;
    }

    /** Returns whether Skin Deep is closed for a health bust. */
    public boolean isClosedForHealthBust() {
        return closedForHealthBust;
    }

    /** Returns whether the player has an INFECTED_WOUND debuff. */
    public boolean isInfectedWoundActive() {
        return infectedWoundActive;
    }

    /** Returns the day the BULLDOG effect expires. */
    public int getBulldogEffectExpiryDay() {
        return bulldogEffectExpiryDay;
    }

    /** Returns the day the TEARDROP gang peace expires. */
    public int getTearDropGangPeaceExpiryDay() {
        return tearDropGangPeaceExpiryDay;
    }

    /** Returns the day the health bust closure ends. */
    public int getClosureEndDay() {
        return closureEndDay;
    }

    /** Returns the last day the Health Inspector visited. */
    public int getLastInspectorVisitDay() {
        return lastInspectorVisitDay;
    }

    /** Force-set inspector visiting flag for testing. */
    public void setInspectorVisitingForTesting(boolean visiting) {
        this.inspectorVisiting = visiting;
    }

    /** Force-set closed for health bust flag for testing. */
    public void setClosedForHealthBustForTesting(boolean closed) {
        this.closedForHealthBust = closed;
    }

    /** Force-set last inspector visit day for testing. */
    public void setLastInspectorVisitDayForTesting(int day) {
        this.lastInspectorVisitDay = day;
    }

    /** Force-set infected wound flag for testing. */
    public void setInfectedWoundActiveForTesting(boolean active) {
        this.infectedWoundActive = active;
    }
}

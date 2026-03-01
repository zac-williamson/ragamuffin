package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Issue #961: Cash4Gold Pawn Shop — Pledging, Redemption &amp; Sunday Morning Regret.
 *
 * <p>Gary the pawn broker (NPC type {@link ragamuffin.entity.NPCType#PAWN_BROKER})
 * offers two services:
 * <ul>
 *   <li><b>Outright sale</b> — player sells an item for 45–60% of StreetEconomySystem
 *       base value (computed as {@code floor(base * 0.45)} to {@code floor(base * 0.60)}).</li>
 *   <li><b>Pledge loan</b> — player deposits an item as collateral and receives 80% of
 *       base value. The item can be redeemed within 3 in-game days by repaying 100% of
 *       base value. On day 4 it is forfeited; Gary keeps the item.</li>
 * </ul>
 *
 * <h3>Stolen goods mechanic</h3>
 * <ul>
 *   <li>At Notoriety Tier ≤ 2: accepted silently.</li>
 *   <li>At Tier 3+: accepted with receipt dialogue (+5 Notoriety per stolen item).</li>
 *   <li>At Tier 5: full service refusal for stolen goods.</li>
 *   <li>Police within 8 blocks during stolen-item sale → {@link CriminalRecord.CrimeType#HANDLING_STOLEN_GOODS}
 *       via WitnessSystem.</li>
 * </ul>
 *
 * <h3>Faction bonus</h3>
 * Marchetti Crew respect ≥ {@link FactionSystem#FRIENDLY_THRESHOLD} gives a +10%
 * sell-price bonus on top of the outright-sale calculation.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#SUNDAY_MORNING_REGRET} — first pledge forfeited on day 4.</li>
 *   <li>{@link AchievementType#IN_HOC} — 3 simultaneous active pledges.</li>
 *   <li>{@link AchievementType#CASH_IN_HAND} — 10 items sold outright in one session.</li>
 * </ul>
 */
public class PawnShopSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Minimum fraction of StreetEconomySystem base price paid on outright sale. */
    public static final float SELL_RATE_MIN = 0.45f;

    /** Maximum fraction of StreetEconomySystem base price paid on outright sale. */
    public static final float SELL_RATE_MAX = 0.60f;

    /** Fraction of base price paid as a pledge loan. */
    public static final float PLEDGE_RATE = 0.80f;

    /** Fraction of base price required to redeem a pledge. */
    public static final float REDEEM_RATE = 1.00f;

    /** In-game days before a pledge is forfeited (item kept on day 4). */
    public static final int PLEDGE_FORFEIT_DAYS = 3;

    /** Distance (blocks) within which police trigger a HANDLING_STOLEN_GOODS charge. */
    public static final float POLICE_STOLEN_DISTANCE = 8f;

    /** Notoriety added per stolen item sold at Tier 3+. */
    public static final int STOLEN_NOTORIETY_GAIN = 5;

    /** Marchetti Crew sell bonus at FRIENDLY_THRESHOLD respect (+10%). */
    public static final float MARCHETTI_BONUS = 0.10f;

    /** Number of items sold in a session to earn CASH_IN_HAND. */
    public static final int CASH_IN_HAND_THRESHOLD = 10;

    /** Number of simultaneous pledges to earn IN_HOC. */
    public static final int IN_HOC_THRESHOLD = 3;

    /** Shop opening hour (09:00). */
    public static final float OPEN_HOUR = 9.0f;

    /** Shop closing hour (17:30). */
    public static final float CLOSE_HOUR = 17.5f;

    /** Day-of-week index for Sunday (0 = Monday, 6 = Sunday). */
    public static final int SUNDAY_DAY_INDEX = 6;

    // ── Accepted items ────────────────────────────────────────────────────────

    /**
     * Items the pawn shop will accept for sale or pledge.
     */
    public static final Set<Material> ACCEPTED_ITEMS;

    /**
     * Subset of accepted items that are considered stolen goods.
     */
    public static final Set<Material> STOLEN_ITEMS;

    static {
        ACCEPTED_ITEMS = Collections.unmodifiableSet(EnumSet.of(
            Material.CROWBAR,
            Material.BOLT_CUTTERS,
            Material.COMPUTER,
            Material.OFFICE_CHAIR,
            Material.STAPLER,
            Material.DIAMOND,
            Material.GOLD_RING,
            Material.STOLEN_PHONE,
            Material.GUITAR,
            Material.HAIR_CLIPPERS,
            Material.BROKEN_PHONE
        ));

        STOLEN_ITEMS = Collections.unmodifiableSet(EnumSet.of(
            Material.STOLEN_PHONE,
            Material.GOLD_RING,
            Material.DIAMOND,
            Material.COMPUTER
        ));
    }

    // ── Pledge record ─────────────────────────────────────────────────────────

    /**
     * An active pledge ticket — an item deposited as collateral for a loan.
     */
    public static class Pledge {
        /** The pledged item. */
        public final Material material;
        /** The in-game day the pledge was created. */
        public final int dayCreated;
        /** Loan amount paid to the player (COIN). */
        public final int loanAmount;
        /** Amount required to redeem (COIN). */
        public final int redeemAmount;
        /** Whether the pledge has been redeemed by the player. */
        private boolean redeemed = false;
        /** Whether the pledge has been forfeited (kept by Gary). */
        private boolean forfeited = false;

        public Pledge(Material material, int dayCreated, int loanAmount, int redeemAmount) {
            this.material = material;
            this.dayCreated = dayCreated;
            this.loanAmount = loanAmount;
            this.redeemAmount = redeemAmount;
        }

        /** Day on which this pledge expires (dayCreated + PLEDGE_FORFEIT_DAYS). */
        public int forfeitDay() {
            return dayCreated + PawnShopSystem.PLEDGE_FORFEIT_DAYS;
        }

        /** Days remaining until forfeiture (may be negative if overdue). */
        public int daysRemaining(int currentDay) {
            return forfeitDay() - currentDay;
        }

        public boolean isRedeemed() { return redeemed; }
        public boolean isForfeited() { return forfeited; }
        public boolean isActive() { return !redeemed && !forfeited; }

        void markRedeemed() { redeemed = true; }
        void markForfeited() { forfeited = true; }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Gary the pawn broker NPC, or null if not yet spawned. */
    private NPC brokerNpc;

    /** Whether the shop UI is currently open. */
    private boolean shopUIOpen;

    /** Whether Gary has shut the shutter (after being assaulted). */
    private boolean shutterDown;

    /** In-game day on which the shutter was dropped (shop re-opens next day). */
    private int shutterDropDay = -1;

    /** Active pledge tickets. */
    private final List<Pledge> pledges = new ArrayList<>();

    /** Number of items sold outright in this session (for CASH_IN_HAND). */
    private int sessionSellCount;

    /** Whether CASH_IN_HAND has been awarded this session. */
    private boolean cashInHandAwarded;

    /** Whether IN_HOC has been awarded. */
    private boolean inHocAwarded;

    /** Whether SUNDAY_MORNING_REGRET has been awarded. */
    private boolean sundayMorningRegretAwarded;

    // ── System references (optional, may be null) ─────────────────────────────

    private StreetEconomySystem streetEconomy;
    private NotorietySystem notorietySystem;
    private FactionSystem factionSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PawnShopSystem() {}

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setStreetEconomy(StreetEconomySystem streetEconomy) {
        this.streetEconomy = streetEconomy;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    // ── NPC management ────────────────────────────────────────────────────────

    /** Set Gary's NPC instance after spawning. */
    public void setBrokerNpc(NPC npc) {
        this.brokerNpc = npc;
    }

    /** Gary's NPC instance, or null. */
    public NPC getBrokerNpc() {
        return brokerNpc;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame. Checks pledge expiry and shutter-reset.
     *
     * @param currentDay   current in-game day index
     * @param achievementCallback callback for achievements (may be null)
     */
    public void update(int currentDay, NotorietySystem.AchievementCallback achievementCallback) {
        // Re-open shutter if a new day has passed
        if (shutterDown && shutterDropDay >= 0 && currentDay > shutterDropDay) {
            shutterDown = false;
        }

        // Expire overdue pledges
        for (Pledge p : pledges) {
            if (!p.isActive()) continue;
            if (currentDay > p.forfeitDay()) {
                p.markForfeited();
                if (!sundayMorningRegretAwarded && achievementCallback != null) {
                    sundayMorningRegretAwarded = true;
                    achievementCallback.award(AchievementType.SUNDAY_MORNING_REGRET);
                }
            }
        }
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    /**
     * Called when the player presses E near Gary.
     *
     * @param currentHour current in-game hour (0–24)
     * @param dayOfWeek   day-of-week index (0 = Mon, 6 = Sun)
     * @return dialogue text, or null if Gary ignores the player
     */
    public String onPlayerInteract(float currentHour, int dayOfWeek) {
        if (brokerNpc == null || !brokerNpc.isAlive()) {
            return null;
        }
        if (shutterDown) {
            return "Shutter's down, mate. Come back tomorrow.";
        }
        if (!isOpen(currentHour, dayOfWeek)) {
            return "We're closed. Nine to half five, Monday to Saturday.";
        }
        shopUIOpen = true;
        return null;
    }

    /** Close the shop UI. */
    public void closeShopUI() {
        shopUIOpen = false;
    }

    // ── Shop open-hours check ─────────────────────────────────────────────────

    /**
     * Returns true if the shop is open for business.
     *
     * @param currentHour hour of day (0–24)
     * @param dayOfWeek   0=Monday … 6=Sunday
     */
    public boolean isOpen(float currentHour, int dayOfWeek) {
        if (dayOfWeek == SUNDAY_DAY_INDEX) return false;
        return currentHour >= OPEN_HOUR && currentHour < CLOSE_HOUR;
    }

    // ── Outright sale ─────────────────────────────────────────────────────────

    /**
     * Returns the COIN value Gary will pay for an outright sale of the given item,
     * incorporating Marchetti bonus. Returns 0 if the item is not accepted.
     *
     * @param material the item
     * @return COIN value (≥ 1 if accepted)
     */
    public int getSellQuote(Material material) {
        if (!ACCEPTED_ITEMS.contains(material)) return 0;
        int base = getBasePrice(material);
        float rate = SELL_RATE_MAX; // default: 60%
        int quote = (int) Math.floor(base * rate);

        // Marchetti Crew bonus: +10% on top
        if (factionSystem != null
                && factionSystem.getRespect(Faction.MARCHETTI_CREW) >= FactionSystem.FRIENDLY_THRESHOLD) {
            quote = (int) Math.floor(quote * (1f + MARCHETTI_BONUS));
        }
        return Math.max(1, quote);
    }

    /**
     * Attempt to sell one unit of the given item outright to Gary.
     *
     * @param material    the item to sell
     * @param inventory   player's inventory
     * @param player      the player (for position check)
     * @param allNpcs     all NPCs (police check)
     * @param achievementCallback for achievements (may be null)
     * @return result of the sale attempt
     */
    public SellResult sellItem(Material material, Inventory inventory, Player player,
                               List<NPC> allNpcs,
                               NotorietySystem.AchievementCallback achievementCallback) {
        if (!ACCEPTED_ITEMS.contains(material)) {
            return SellResult.NOT_ACCEPTED;
        }
        if (inventory.getItemCount(material) < 1) {
            return SellResult.NO_ITEM;
        }

        // Stolen goods tier check
        if (STOLEN_ITEMS.contains(material) && notorietySystem != null) {
            int tier = notorietySystem.getTier();
            if (tier >= 5) {
                if (brokerNpc != null) {
                    brokerNpc.setSpeechText("I know your face. Get out.", 4f);
                }
                return SellResult.REFUSED_TIER5;
            }
        }

        int quote = getSellQuote(material);
        inventory.removeItem(material, 1);
        inventory.addItem(Material.COIN, quote);

        sessionSellCount++;

        // Stolen goods notoriety and criminal record
        if (STOLEN_ITEMS.contains(material) && notorietySystem != null) {
            int tier = notorietySystem.getTier();
            if (tier >= 3) {
                notorietySystem.addNotoriety(STOLEN_NOTORIETY_GAIN, achievementCallback);
                if (brokerNpc != null) {
                    brokerNpc.setSpeechText("I'll need a receipt for that. Just saying.", 4f);
                }
            }
            // Police nearby: criminal record entry
            if (isPoliceNearby(player, allNpcs) && criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.HANDLING_STOLEN_GOODS);
            }
        }

        // Rumour seeding for significant sales (DIAMOND, GOLD_RING)
        if ((material == Material.DIAMOND || material == Material.GOLD_RING)
                && rumourNetwork != null && allNpcs != null) {
            seedSaleRumour(material, allNpcs);
        }

        // CASH_IN_HAND achievement
        if (!cashInHandAwarded && sessionSellCount >= CASH_IN_HAND_THRESHOLD
                && achievementCallback != null) {
            cashInHandAwarded = true;
            achievementCallback.award(AchievementType.CASH_IN_HAND);
        }

        return SellResult.SUCCESS;
    }

    // ── Pledge loan ───────────────────────────────────────────────────────────

    /**
     * Returns the loan amount (COIN) Gary will offer as a pledge for the given item.
     * Returns 0 if the item is not accepted.
     *
     * @param material the item
     */
    public int getPledgeQuote(Material material) {
        if (!ACCEPTED_ITEMS.contains(material)) return 0;
        int base = getBasePrice(material);
        return Math.max(1, (int) Math.floor(base * PLEDGE_RATE));
    }

    /**
     * Returns the redemption cost (COIN) to reclaim the given item from pledge.
     *
     * @param material the item
     */
    public int getRedeemCost(Material material) {
        if (!ACCEPTED_ITEMS.contains(material)) return 0;
        int base = getBasePrice(material);
        return Math.max(1, (int) Math.floor(base * REDEEM_RATE));
    }

    /**
     * Attempt to pledge one unit of the given item for a loan.
     *
     * @param material    item to pledge
     * @param inventory   player's inventory
     * @param currentDay  current in-game day
     * @param achievementCallback for achievements (may be null)
     * @return the new Pledge record, or null on failure
     */
    public Pledge pledgeItem(Material material, Inventory inventory, int currentDay,
                             NotorietySystem.AchievementCallback achievementCallback) {
        if (!ACCEPTED_ITEMS.contains(material)) return null;
        if (inventory.getItemCount(material) < 1) return null;

        int loanAmount = getPledgeQuote(material);
        int redeemAmount = getRedeemCost(material);

        inventory.removeItem(material, 1);
        inventory.addItem(Material.COIN, loanAmount);

        Pledge pledge = new Pledge(material, currentDay, loanAmount, redeemAmount);
        pledges.add(pledge);

        // IN_HOC achievement: 3 simultaneous active pledges
        if (!inHocAwarded && countActivePledges() >= IN_HOC_THRESHOLD
                && achievementCallback != null) {
            inHocAwarded = true;
            achievementCallback.award(AchievementType.IN_HOC);
        }

        return pledge;
    }

    /**
     * Attempt to redeem a pledge by index.
     *
     * @param pledgeIndex index into active pledges list (0-based)
     * @param inventory   player's inventory
     * @param currentDay  current in-game day
     * @return true if redemption succeeded
     */
    public boolean redeemPledge(int pledgeIndex, Inventory inventory, int currentDay) {
        List<Pledge> active = getActivePledges();
        if (pledgeIndex < 0 || pledgeIndex >= active.size()) return false;

        Pledge pledge = active.get(pledgeIndex);
        if (!pledge.isActive()) return false;
        if (currentDay > pledge.forfeitDay()) return false; // already forfeited

        int cost = pledge.redeemAmount;
        if (inventory.getItemCount(Material.COIN) < cost) return false;

        inventory.removeItem(Material.COIN, cost);
        inventory.addItem(pledge.material, 1);
        pledge.markRedeemed();
        return true;
    }

    // ── Assault response ──────────────────────────────────────────────────────

    /**
     * Called when the player assaults Gary.
     * Gary flees and drops the shutter for the rest of the day.
     *
     * @param currentDay current in-game day
     */
    public void onBrokerAssaulted(int currentDay) {
        shutterDown = true;
        shutterDropDay = currentDay;
        if (brokerNpc != null) {
            brokerNpc.setState(NPCState.FLEEING);
            brokerNpc.setSpeechText("Oi! Get out! I'm shutting up shop!", 5f);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getBasePrice(Material material) {
        if (streetEconomy != null) {
            int base = streetEconomy.getBasePrice(material);
            if (base > 0) return base;
        }
        // Fallback default prices if StreetEconomySystem not wired
        switch (material) {
            case CROWBAR:      return 12;
            case BOLT_CUTTERS: return 18;
            case COMPUTER:     return 20;
            case OFFICE_CHAIR: return 12;
            case STAPLER:      return 5;
            case DIAMOND:      return 40;
            case GOLD_RING:    return 25;
            case STOLEN_PHONE: return 15;
            case GUITAR:       return 18;
            case HAIR_CLIPPERS: return 10;
            case BROKEN_PHONE: return 3;
            default:           return 5;
        }
    }

    private boolean isPoliceNearby(Player player, List<NPC> allNpcs) {
        if (allNpcs == null || player == null) return false;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.POLICE
                    || npc.getType() == NPCType.ARMED_RESPONSE
                    || npc.getType() == NPCType.PCSO) {
                if (npc.getPosition().dst(player.getPosition()) <= POLICE_STOLEN_DISTANCE) {
                    return true;
                }
            }
        }
        return false;
    }

    private void seedSaleRumour(Material material, List<NPC> allNpcs) {
        String text = (material == Material.DIAMOND)
            ? "Bloke just flogged a diamond at Cash4Gold. Gary nearly had a moment."
            : "Someone's shifted a gold ring at the pawn shop. No questions asked, apparently.";
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.BARMAN) {
                rumourNetwork.addRumour(npc, new Rumour(RumourType.SHOP_NEWS, text));
                return;
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether the shop UI is currently open. */
    public boolean isShopUIOpen() {
        return shopUIOpen;
    }

    /** Whether Gary has dropped the shutter (closed after assault). */
    public boolean isShutterDown() {
        return shutterDown;
    }

    /** All pledge records (active, redeemed, and forfeited). */
    public List<Pledge> getAllPledges() {
        return Collections.unmodifiableList(pledges);
    }

    /** Currently active pledge tickets. */
    public List<Pledge> getActivePledges() {
        List<Pledge> active = new ArrayList<>();
        for (Pledge p : pledges) {
            if (p.isActive()) active.add(p);
        }
        return active;
    }

    /** Number of currently active pledges. */
    public int countActivePledges() {
        int count = 0;
        for (Pledge p : pledges) {
            if (p.isActive()) count++;
        }
        return count;
    }

    /** Number of items sold outright in the current session. */
    public int getSessionSellCount() {
        return sessionSellCount;
    }

    /** Force-set the shutter state (for testing). */
    void setShutterDownForTesting(boolean down, int dropDay) {
        this.shutterDown = down;
        this.shutterDropDay = dropDay;
    }

    /** Force-add a pledge (for testing). */
    void addPledgeForTesting(Pledge pledge) {
        pledges.add(pledge);
    }

    // ── Nested enums ──────────────────────────────────────────────────────────

    /**
     * Result of an outright-sale attempt.
     */
    public enum SellResult {
        /** Sale completed successfully. */
        SUCCESS,
        /** Item is not on the accepted list. */
        NOT_ACCEPTED,
        /** Player does not have the item. */
        NO_ITEM,
        /** Refused due to Tier 5 notoriety with stolen goods. */
        REFUSED_TIER5
    }
}

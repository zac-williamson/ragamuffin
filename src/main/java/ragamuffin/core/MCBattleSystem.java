package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Issue #716: Underground Music Scene — MC Battle System.
 *
 * <h3>Overview</h3>
 * The player can challenge one of three faction MC Champions to an MC Battle by
 * interacting (E) with the champion NPC while holding a {@link Material#MICROPHONE}.
 * Each battle is 3 rounds of the {@link BattleBarMiniGame} timing mini-game.
 *
 * <h3>Faction Champions</h3>
 * <ul>
 *   <li><b>Marchetti MC</b> — at the off-licence. Easy difficulty.</li>
 *   <li><b>Street Lads MC</b> — in the park. Medium difficulty.</li>
 *   <li><b>Council MC</b> — near the JobCentre. Hard difficulty.</li>
 * </ul>
 *
 * <h3>Winning a Battle</h3>
 * Win 2 or more rounds out of 3 to win the battle.
 * On victory:
 * <ul>
 *   <li>MC Rank increases by 1 (max 5).</li>
 *   <li>The defeated faction's Respect toward the player rises by +15.</li>
 *   <li>The two rival factions' Respect drops by −5 each.</li>
 *   <li>Notoriety gains +15.</li>
 *   <li>A {@link RumourType#GANG_ACTIVITY} rumour is seeded into nearby NPCs.</li>
 *   <li>Achievements: {@link AchievementType#FIRST_BARS} on first win;
 *       {@link AchievementType#BODIED} when all three champions have been defeated;
 *       {@link AchievementType#GRIME_GOD} on reaching MC Rank 5.</li>
 * </ul>
 *
 * <h3>MC Rank Effects</h3>
 * <ul>
 *   <li>Rank 0: No benefits.</li>
 *   <li>Rank 1: Can host raves (requires Vibe ≥ 40).</li>
 *   <li>Rank 2: Exclusive faction music missions unlocked.</li>
 *   <li>Rank 3: DOUBLE_LIFE achievement check.</li>
 *   <li>Rank 5: FAN_POSTER props replace WANTED_POSTERs near squat; hype-man NPC spawns.</li>
 * </ul>
 */
public class MCBattleSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Number of battle rounds. */
    public static final int ROUNDS_PER_BATTLE = 3;

    /** Rounds needed to win a battle. */
    public static final int ROUNDS_TO_WIN = 2;

    /** Maximum MC Rank. */
    public static final int MAX_MC_RANK = 5;

    /** Notoriety gained on a battle victory. */
    public static final int NOTORIETY_GAIN_WIN = 15;

    /** Respect gained by the defeated faction on player victory. */
    public static final int RESPECT_GAIN_DEFEATED_FACTION = 15;

    /** Respect lost by rival factions on player victory. */
    public static final int RESPECT_LOSS_RIVALS = 5;

    // ── Enum: which champions have been beaten ─────────────────────────────

    /**
     * The three champion identifiers, linked to their faction/landmark context.
     */
    public enum Champion {
        /** Marchetti Crew MC — at the off-licence. Easy difficulty. */
        MARCHETTI_MC,
        /** Street Lads MC — in the park. Medium difficulty. */
        STREET_LADS_MC,
        /** The Council MC — near the JobCentre. Hard difficulty. */
        COUNCIL_MC
    }

    // ── Faction→Champion mapping (for Respect adjustments) ────────────────

    /**
     * Returns the {@link Faction} associated with a champion.
     */
    public static Faction factionOf(Champion champion) {
        switch (champion) {
            case MARCHETTI_MC:  return Faction.MARCHETTI_CREW;
            case STREET_LADS_MC: return Faction.STREET_LADS;
            case COUNCIL_MC:    return Faction.THE_COUNCIL;
            default: throw new IllegalArgumentException("Unknown champion: " + champion);
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private int mcRank;
    private final Set<Champion> defeatedChampions;
    private final NotorietySystem notorietySystem;
    private final FactionSystem factionSystem;
    private final AchievementSystem achievementSystem;
    private final RumourNetwork rumourNetwork;
    private final Random rng;

    // Active battle state
    private Champion activeBattle;
    private int currentRound;
    private int playerWins;
    private BattleBarMiniGame currentBar;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MCBattleSystem(
            NotorietySystem notorietySystem,
            FactionSystem factionSystem,
            AchievementSystem achievementSystem,
            RumourNetwork rumourNetwork,
            Random rng) {
        this.notorietySystem   = notorietySystem;
        this.factionSystem     = factionSystem;
        this.achievementSystem = achievementSystem;
        this.rumourNetwork     = rumourNetwork;
        this.rng               = rng;
        this.mcRank            = 0;
        this.defeatedChampions = EnumSet.noneOf(Champion.class);
        this.activeBattle      = null;
        this.currentRound      = 0;
        this.playerWins        = 0;
        this.currentBar        = null;
    }

    // ── Battle initiation ─────────────────────────────────────────────────────

    /**
     * Attempt to start an MC Battle against {@code champion}.
     * Requires the player to hold at least 1 {@link Material#MICROPHONE}.
     *
     * @param champion  the champion to challenge
     * @param inventory the player's inventory (MICROPHONE will be consumed)
     * @return a status message describing the outcome of the initiation attempt
     */
    public String startBattle(Champion champion, Inventory inventory) {
        if (activeBattle != null) {
            return "Battle already in progress.";
        }
        if (inventory.getItemCount(Material.MICROPHONE) < 1) {
            return "You need a microphone to challenge them, bruv.";
        }
        // Consume the microphone
        inventory.removeItem(Material.MICROPHONE, 1);

        activeBattle = champion;
        currentRound = 0;
        playerWins   = 0;
        beginNextRound();
        return "Battle started against " + champion.name() + "! Round 1 — hit the bar!";
    }

    /** Begin the next round of the active battle. */
    private void beginNextRound() {
        currentRound++;
        switch (activeBattle) {
            case MARCHETTI_MC:   currentBar = BattleBarMiniGame.easy(rng);   break;
            case STREET_LADS_MC: currentBar = BattleBarMiniGame.medium(rng); break;
            case COUNCIL_MC:     currentBar = BattleBarMiniGame.hard(rng);   break;
        }
    }

    // ── Update loop ───────────────────────────────────────────────────────────

    /**
     * Advance the active battle bar by {@code delta} seconds.
     * Call each frame while a battle is in progress.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        if (activeBattle == null || currentBar == null) return;
        currentBar.update(delta);

        // If bar resolved (timed out), advance the battle
        if (currentBar.isResolved()) {
            advanceRound(currentBar.wasHit());
        }
    }

    /**
     * Player presses the action key during a battle bar round.
     *
     * @param nearbyNpcs list of NPCs nearby (for rumour seeding on victory)
     * @return result message, or {@code null} if no battle is active
     */
    public String pressAction(java.util.List<NPC> nearbyNpcs) {
        if (activeBattle == null || currentBar == null) return null;
        if (currentBar.isResolved()) return null;

        boolean hit = currentBar.press();
        advanceRound(hit);
        return hit ? "Hit!" : "Missed!";
    }

    /** Called after a round resolves (either via press or timeout). */
    private void advanceRound(boolean playerHit) {
        if (playerHit) playerWins++;

        boolean battleOver = currentRound >= ROUNDS_PER_BATTLE
                || playerWins >= ROUNDS_TO_WIN
                || (currentRound - playerWins) >= (ROUNDS_PER_BATTLE - ROUNDS_TO_WIN + 1);

        if (battleOver) {
            resolveBattle(playerWins >= ROUNDS_TO_WIN, null);
        } else if (currentBar.isResolved()) {
            // Start the next round
            beginNextRound();
        }
    }

    /**
     * Fully resolve the battle (called internally when 3 rounds complete, or when
     * the outcome is already determined).
     *
     * @param playerWon   whether the player won the battle
     * @param nearbyNpcs  NPCs nearby for rumour seeding (may be null)
     */
    private void resolveBattle(boolean playerWon, java.util.List<NPC> nearbyNpcs) {
        Champion champion = activeBattle;
        activeBattle = null;
        currentBar   = null;

        if (playerWon) {
            applyVictory(champion, nearbyNpcs);
        }
        // If player lost, no stat changes — they can try again
    }

    /** Apply all victory effects for winning against {@code champion}. */
    private void applyVictory(Champion champion, java.util.List<NPC> nearbyNpcs) {
        // MC Rank
        if (mcRank < MAX_MC_RANK) {
            mcRank++;
        }

        // Track defeated champion
        defeatedChampions.add(champion);

        // Notoriety — pass achievement system as callback
        notorietySystem.addNotoriety(NOTORIETY_GAIN_WIN, achievementSystem::unlock);

        // Faction Respect
        Faction defeated = factionOf(champion);
        factionSystem.applyRespectDelta(defeated, RESPECT_GAIN_DEFEATED_FACTION);
        for (Faction rival : defeated.rivals()) {
            factionSystem.applyRespectDelta(rival, -RESPECT_LOSS_RIVALS);
        }

        // Seed a GANG_ACTIVITY rumour
        if (nearbyNpcs != null && !nearbyNpcs.isEmpty()) {
            Rumour rumour = new Rumour(
                    RumourType.GANG_ACTIVITY,
                    "That MC bodied the " + defeated.getDisplayName() + " man in a battle — proper went off."
            );
            rumourNetwork.addRumour(nearbyNpcs.get(0), rumour);
        }

        // Achievements
        achievementSystem.unlock(AchievementType.FIRST_BARS);
        if (defeatedChampions.size() == 3) {
            achievementSystem.unlock(AchievementType.BODIED);
        }
        if (mcRank >= MAX_MC_RANK) {
            achievementSystem.unlock(AchievementType.GRIME_GOD);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** @return current MC Rank (0–5) */
    public int getMcRank() { return mcRank; }

    /** @return {@code true} if an MC Battle is currently in progress */
    public boolean isBattleActive() { return activeBattle != null; }

    /** @return the champion being fought, or {@code null} if no battle is active */
    public Champion getActiveBattle() { return activeBattle; }

    /** @return the current round number (1–3), or 0 if no battle is active */
    public int getCurrentRound() { return currentRound; }

    /** @return number of rounds won by the player so far in the active battle */
    public int getPlayerWins() { return playerWins; }

    /** @return the active {@link BattleBarMiniGame}, or {@code null} */
    public BattleBarMiniGame getCurrentBar() { return currentBar; }

    /** @return a read-only view of the set of defeated champions */
    public Set<Champion> getDefeatedChampions() {
        return java.util.Collections.unmodifiableSet(defeatedChampions);
    }

    /** @return {@code true} if the given champion has been beaten */
    public boolean isDefeated(Champion champion) {
        return defeatedChampions.contains(champion);
    }

    /**
     * Force-set MC Rank (for testing / save-load).
     *
     * @param rank new rank (0–{@link #MAX_MC_RANK})
     */
    public void setMcRank(int rank) {
        this.mcRank = Math.max(0, Math.min(MAX_MC_RANK, rank));
    }

    /**
     * Force-complete a battle outcome (for integration testing only).
     * Resolves the active battle with the given result without requiring
     * BattleBar input, and applies appropriate victory effects.
     *
     * @param playerWins  {@code true} to force a player victory
     * @param nearbyNpcs  nearby NPCs for rumour seeding
     */
    public void forceResolveBattle(boolean playerWins, java.util.List<NPC> nearbyNpcs) {
        if (activeBattle == null) return;
        resolveBattle(playerWins, nearbyNpcs);
    }
}

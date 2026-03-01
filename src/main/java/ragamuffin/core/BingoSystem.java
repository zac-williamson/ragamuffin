package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.ui.TooltipSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #1010 — Northfield Bingo Hall ("Lucky Stars Bingo").
 *
 * <p>Manages all bingo session state: schedule, entry, number-calling, win
 * detection, rigged card cheat mechanics, pensioner NPCs, and system integrations.
 *
 * <h3>Session schedule</h3>
 * <ul>
 *   <li>Sessions run Tuesday (day index % 7 == 1) and Thursday (day index % 7 == 3).</li>
 *   <li>Doors open 13:30; eyes down 14:00; closed after 17:00.</li>
 * </ul>
 */
public class BingoSystem {

    // ── Schedule constants ────────────────────────────────────────────────────

    /** Day-of-week index for Tuesday (0 = Monday). */
    public static final int DAY_TUESDAY  = 1;
    /** Day-of-week index for Thursday (0 = Monday). */
    public static final int DAY_THURSDAY = 3;

    /** Earliest time doors open (13:30). */
    public static final float DOORS_OPEN_TIME   = 13.5f;
    /** Time when eyes go down (14:00). */
    public static final float EYES_DOWN_TIME     = 14.0f;
    /** Time session ends (17:00). */
    public static final float SESSION_END_TIME   = 17.0f;

    /** Entry fee in COIN. */
    public static final int ENTRY_FEE_COIN = 2;

    /** LINE win prize in COIN. */
    public static final int LINE_PRIZE_COIN      = 5;
    /** FULL HOUSE win prize in COIN. */
    public static final int FULL_HOUSE_PRIZE_COIN = 15;
    /** Consolation prize for player when a pensioner wins. */
    public static final int CONSOLATION_PRIZE_COIN = 1;

    /** One number called per in-game minute (1/60 hour). */
    private static final float MINUTES_PER_CALL  = 1.0f;
    private static final float HOURS_PER_CALL    = MINUTES_PER_CALL / 60.0f;

    /** Cheat detection probability per number called (5%). */
    public static final float CHEAT_DETECTION_PROBABILITY = 0.05f;

    /** Notoriety gained when caught cheating. */
    public static final int NOTORIETY_CHEAT_CAUGHT = 8;
    /** Notoriety gained when ejected (non-cheat). */
    public static final int NOTORIETY_EJECTED      = 3;

    /** NeighbourhoodWatch anger increase when pensioner assaulted. */
    public static final int WATCH_ANGER_PENSIONER_ASSAULT = 10;

    // ── Session state ─────────────────────────────────────────────────────────

    public enum SessionState {
        /** Hall is closed (outside session hours). */
        CLOSED,
        /** Doors open, player may enter (13:30–14:00). */
        DOORS_OPEN,
        /** Session in progress (eyes down). */
        IN_PROGRESS,
        /** Session has ended (winner found or time expired). */
        ENDED
    }

    private SessionState sessionState = SessionState.CLOSED;

    /** Whether the player is currently inside the bingo hall. */
    private boolean playerInside = false;

    /** The player's current bingo card (null if not in session). */
    private BingoCard playerCard = null;

    /** Whether the player used a rigged card this session. */
    private boolean playerUsedRiggedCard = false;

    /** Whether cheat detection override is set (for testing). */
    private float cheatDetectionOverride = -1f;

    /** The shuffle of 90 numbers for this session (index 0..89). */
    private final List<Integer> numberShuffle = new ArrayList<>();

    /** Index into numberShuffle — next number to call. */
    private int callIndex = 0;

    /** Last called number (-1 = none called yet). */
    private int lastCalledNumber = -1;

    /** Accumulated time since last number was called (in in-game hours). */
    private float timeSinceLastCall = 0f;

    /** Whether a LINE has been won this session. */
    private boolean lineWon = false;

    /** Whether a FULL HOUSE has been won this session. */
    private boolean fullHouseWon = false;

    /** Whether the player has been ejected this session. */
    private boolean playerEjected = false;

    /** Whether the player has already false-shouted this session (cooldown). */
    private boolean falseShouteUsed = false;

    /** NPC used as the CALLER (for speech bubbles). May be null. */
    private NPC callerNpc = null;

    /** Pensioner NPCs currently attending the session. */
    private final List<NPC> pensionerNpcs = new ArrayList<>();

    /** Whether a pensioner has won (shouts BINGO before player). */
    private boolean pensionerWon = false;

    // ── RNG ───────────────────────────────────────────────────────────────────

    private final Random random;

    // ── British bingo calls ───────────────────────────────────────────────────

    private static final String[] BINGO_CALLS = new String[91]; // index 1..90

    static {
        BINGO_CALLS[1]  = "Kelly's eye — number one!";
        BINGO_CALLS[2]  = "One little duck — two!";
        BINGO_CALLS[3]  = "Cup of tea — three!";
        BINGO_CALLS[4]  = "Knock at the door — four!";
        BINGO_CALLS[5]  = "Man alive — five!";
        BINGO_CALLS[6]  = "Tom Mix — six!";
        BINGO_CALLS[7]  = "Lucky seven!";
        BINGO_CALLS[8]  = "One fat lady — eight!";
        BINGO_CALLS[9]  = "Doctor's orders — nine!";
        BINGO_CALLS[10] = "Theresa's den — number ten!";
        BINGO_CALLS[11] = "Legs eleven!";
        BINGO_CALLS[12] = "One dozen — twelve!";
        BINGO_CALLS[13] = "Unlucky for some — thirteen!";
        BINGO_CALLS[14] = "Valentine's Day — fourteen!";
        BINGO_CALLS[15] = "Young and keen — fifteen!";
        BINGO_CALLS[16] = "Sweet sixteen!";
        BINGO_CALLS[17] = "Often been kissed — seventeen!";
        BINGO_CALLS[18] = "Coming of age — eighteen!";
        BINGO_CALLS[19] = "Goodbye teens — nineteen!";
        BINGO_CALLS[20] = "One score — twenty!";
        BINGO_CALLS[21] = "Key of the door — twenty-one!";
        BINGO_CALLS[22] = "Two little ducks — twenty-two!";
        BINGO_CALLS[23] = "Thee and me — twenty-three!";
        BINGO_CALLS[24] = "Two dozen — twenty-four!";
        BINGO_CALLS[25] = "Duck and dive — twenty-five!";
        BINGO_CALLS[26] = "Pick and mix — twenty-six!";
        BINGO_CALLS[27] = "Gateway to heaven — twenty-seven!";
        BINGO_CALLS[28] = "Overweight — twenty-eight!";
        BINGO_CALLS[29] = "Rise and shine — twenty-nine!";
        BINGO_CALLS[30] = "Dirty Gertie — thirty!";
        BINGO_CALLS[31] = "Get up and run — thirty-one!";
        BINGO_CALLS[32] = "Buckle my shoe — thirty-two!";
        BINGO_CALLS[33] = "Dirty knee — thirty-three!";
        BINGO_CALLS[34] = "Ask for more — thirty-four!";
        BINGO_CALLS[35] = "Jump and jive — thirty-five!";
        BINGO_CALLS[36] = "Three dozen — thirty-six!";
        BINGO_CALLS[37] = "More than eleven — thirty-seven!";
        BINGO_CALLS[38] = "Christmas cake — thirty-eight!";
        BINGO_CALLS[39] = "Steps — thirty-nine!";
        BINGO_CALLS[40] = "Life begins — forty!";
        BINGO_CALLS[41] = "Time for fun — forty-one!";
        BINGO_CALLS[42] = "Winnie the Pooh — forty-two!";
        BINGO_CALLS[43] = "Down on your knees — forty-three!";
        BINGO_CALLS[44] = "Droopy drawers — forty-four!";
        BINGO_CALLS[45] = "Halfway there — forty-five!";
        BINGO_CALLS[46] = "Up to tricks — forty-six!";
        BINGO_CALLS[47] = "Four and seven — forty-seven!";
        BINGO_CALLS[48] = "Four dozen — forty-eight!";
        BINGO_CALLS[49] = "PC — forty-nine!";
        BINGO_CALLS[50] = "Half a century — fifty!";
        BINGO_CALLS[51] = "Tweak of the thumb — fifty-one!";
        BINGO_CALLS[52] = "Danny La Rue — fifty-two!";
        BINGO_CALLS[53] = "Stuck in the tree — fifty-three!";
        BINGO_CALLS[54] = "Clean the floor — fifty-four!";
        BINGO_CALLS[55] = "Snakes alive — fifty-five!";
        BINGO_CALLS[56] = "Was she worth it — fifty-six!";
        BINGO_CALLS[57] = "Heinz varieties — fifty-seven!";
        BINGO_CALLS[58] = "Make them wait — fifty-eight!";
        BINGO_CALLS[59] = "Brighton line — fifty-nine!";
        BINGO_CALLS[60] = "Five dozen — sixty!";
        BINGO_CALLS[61] = "Baker's bun — sixty-one!";
        BINGO_CALLS[62] = "Turn the screw — sixty-two!";
        BINGO_CALLS[63] = "Tickle me — sixty-three!";
        BINGO_CALLS[64] = "Red raw — sixty-four!";
        BINGO_CALLS[65] = "Old age pension — sixty-five!";
        BINGO_CALLS[66] = "Clickety click — sixty-six!";
        BINGO_CALLS[67] = "Made in heaven — sixty-seven!";
        BINGO_CALLS[68] = "Saving Grace — sixty-eight!";
        BINGO_CALLS[69] = "Either way up — sixty-nine!";
        BINGO_CALLS[70] = "Three score and ten — seventy!";
        BINGO_CALLS[71] = "Bang on the drum — seventy-one!";
        BINGO_CALLS[72] = "Six dozen — seventy-two!";
        BINGO_CALLS[73] = "Queen bee — seventy-three!";
        BINGO_CALLS[74] = "Candy store — seventy-four!";
        BINGO_CALLS[75] = "Strive and strive — seventy-five!";
        BINGO_CALLS[76] = "Trombones — seventy-six!";
        BINGO_CALLS[77] = "Sunset strip — seventy-seven!";
        BINGO_CALLS[78] = "One more time — seventy-eight!";
        BINGO_CALLS[79] = "One more time again — seventy-nine!";
        BINGO_CALLS[80] = "Gandhi's breakfast — eighty!";
        BINGO_CALLS[81] = "Stop and run — eighty-one!";
        BINGO_CALLS[82] = "Straight on through — eighty-two!";
        BINGO_CALLS[83] = "Time for tea — eighty-three!";
        BINGO_CALLS[84] = "Seven dozen — eighty-four!";
        BINGO_CALLS[85] = "Staying alive — eighty-five!";
        BINGO_CALLS[86] = "Between the sticks — eighty-six!";
        BINGO_CALLS[87] = "Torquay in Devon — eighty-seven!";
        BINGO_CALLS[88] = "Two fat ladies — eighty-eight!";
        BINGO_CALLS[89] = "Nearly there — eighty-nine!";
        BINGO_CALLS[90] = "Top of the shop — ninety!";
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public BingoSystem(Random random) {
        this.random = random;
    }

    // ── Session schedule helpers ──────────────────────────────────────────────

    /**
     * Returns the day-of-week index (0=Monday … 6=Sunday) from the TimeSystem.
     * Uses dayCount (starts at 1 on game start, which is June 1 = Wednesday = index 2).
     * Game start offset: dayCount=1 → Wednesday, so dayOfWeek = (dayCount - 1 + 2) % 7
     * i.e. dayCount 1→Wed(2), 2→Thu(3), 3→Fri(4), 4→Sat(5), 5→Sun(6), 6→Mon(0), 7→Tue(1)
     */
    public static int getDayOfWeek(TimeSystem timeSystem) {
        // dayCount starts at 1; June 1 is a Wednesday (index 2)
        return ((timeSystem.getDayCount() - 1) + 2) % 7;
    }

    /**
     * Returns true if the given day is a session day (Tuesday or Thursday).
     */
    public boolean isSessionDay(TimeSystem timeSystem) {
        int dow = getDayOfWeek(timeSystem);
        return dow == DAY_TUESDAY || dow == DAY_THURSDAY;
    }

    /**
     * Returns true if doors are currently open (session day, 13:30 ≤ time < 17:00).
     */
    public boolean isDoorsOpen(TimeSystem timeSystem) {
        if (!isSessionDay(timeSystem)) return false;
        float t = timeSystem.getTime();
        return t >= DOORS_OPEN_TIME && t < SESSION_END_TIME;
    }

    /**
     * Returns true if the session is currently running (eyes down, 14:00 ≤ time < 17:00).
     */
    public boolean isSessionActive(TimeSystem timeSystem) {
        if (!isSessionDay(timeSystem)) return false;
        float t = timeSystem.getTime();
        return t >= EYES_DOWN_TIME && t < SESSION_END_TIME;
    }

    // ── Player entry ──────────────────────────────────────────────────────────

    /**
     * Attempt to enter the bingo hall. Player must pay 2 COIN.
     *
     * @param inventory     player inventory
     * @param timeSystem    current time
     * @param tooltipSystem for showing tooltips (may be null)
     * @return true if entry was successful
     */
    public boolean tryEnterSession(Inventory inventory, TimeSystem timeSystem,
                                   TooltipSystem tooltipSystem) {
        if (!isDoorsOpen(timeSystem)) {
            if (tooltipSystem != null) {
                tooltipSystem.showMessage(
                    "Closed — eyes down Tuesdays and Thursdays", 3.0f);
            }
            return false;
        }
        if (playerInside) {
            return true; // already inside
        }
        if (!inventory.hasItem(Material.COIN, ENTRY_FEE_COIN)) {
            if (tooltipSystem != null) {
                tooltipSystem.showMessage(
                    "You need 2 COIN to enter. This isn't a library.", 3.0f);
            }
            return false;
        }

        // Pay entry fee
        inventory.removeItem(Material.COIN, ENTRY_FEE_COIN);

        // Issue card — rigged if player has RIGGED_BINGO_CARD
        boolean rigged = inventory.hasItem(Material.RIGGED_BINGO_CARD, 1);
        if (rigged) {
            inventory.removeItem(Material.RIGGED_BINGO_CARD, 1);
            playerCard = new BingoCard(random, true);
            playerUsedRiggedCard = true;
            if (tooltipSystem != null) {
                tooltipSystem.showMessage(
                    "It's only cheating if you get caught.", 3.0f);
            }
        } else {
            playerCard = new BingoCard(random, false);
            playerUsedRiggedCard = false;
        }

        playerInside     = true;
        playerEjected    = false;
        falseShouteUsed  = false;

        // Kick off session if not yet started
        if (sessionState == SessionState.DOORS_OPEN ||
                sessionState == SessionState.CLOSED) {
            startSession();
        }

        return true;
    }

    /**
     * Start a new session: build the shuffled call order.
     */
    private void startSession() {
        sessionState = SessionState.IN_PROGRESS;
        lineWon      = false;
        fullHouseWon = false;
        pensionerWon = false;
        callIndex    = 0;
        lastCalledNumber = -1;
        timeSinceLastCall = 0f;
        falseShouteUsed  = false;

        // Build deterministic shuffle of 1–90
        numberShuffle.clear();
        for (int i = 1; i <= 90; i++) {
            numberShuffle.add(i);
        }
        Collections.shuffle(numberShuffle, random);
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Called from RagamuffinGame every frame.
     *
     * @param delta                  seconds since last frame
     * @param timeSystem             current game time
     * @param playerInventory        player inventory (for prizes)
     * @param rumourNetwork          for seeding WINNER rumours
     * @param allNpcs                all active NPCs
     * @param notorietySystem        for notoriety changes
     * @param neighbourhoodWatchSystem for pensioner solidarity anger
     * @param criminalRecord         for recording offences
     * @param achievementCallback    for unlocking achievements
     * @param tooltipSystem          for player messages (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Inventory playerInventory,
                       RumourNetwork rumourNetwork,
                       List<NPC> allNpcs,
                       NotorietySystem notorietySystem,
                       NeighbourhoodWatchSystem neighbourhoodWatchSystem,
                       CriminalRecord criminalRecord,
                       NotorietySystem.AchievementCallback achievementCallback,
                       TooltipSystem tooltipSystem) {

        // Update session state based on current time
        updateSessionState(timeSystem);

        if (sessionState == SessionState.CLOSED ||
                sessionState == SessionState.ENDED) {
            return;
        }

        // Find caller NPC if not cached
        if (callerNpc == null) {
            for (NPC npc : allNpcs) {
                if (npc.getType() == NPCType.CALLER) {
                    callerNpc = npc;
                    break;
                }
            }
        }

        // Find pensioner NPCs (4–8 for the session)
        if (pensionerNpcs.isEmpty() && playerInside) {
            for (NPC npc : allNpcs) {
                if (npc.getType() == NPCType.PENSIONER && pensionerNpcs.size() < 8) {
                    pensionerNpcs.add(npc);
                }
            }
        }

        if (sessionState != SessionState.IN_PROGRESS) return;

        // Advance call timer
        timeSinceLastCall += delta * timeSystem.getTimeSpeed();

        // One number per in-game minute
        if (timeSinceLastCall >= HOURS_PER_CALL && callIndex < numberShuffle.size()) {
            timeSinceLastCall = 0f;
            int number = numberShuffle.get(callIndex++);
            callNumber(number, playerInventory, rumourNetwork, allNpcs,
                       notorietySystem, criminalRecord, achievementCallback,
                       tooltipSystem);
        }
    }

    /**
     * Update the session state based on current time.
     */
    private void updateSessionState(TimeSystem timeSystem) {
        if (!isSessionDay(timeSystem)) {
            // Reset if we've moved past a session day
            if (sessionState != SessionState.CLOSED) {
                endSession();
            }
            return;
        }
        float t = timeSystem.getTime();
        if (t < DOORS_OPEN_TIME) {
            if (sessionState != SessionState.CLOSED) endSession();
        } else if (t < EYES_DOWN_TIME) {
            if (sessionState == SessionState.CLOSED) {
                sessionState = SessionState.DOORS_OPEN;
            }
        } else if (t < SESSION_END_TIME) {
            if (sessionState == SessionState.DOORS_OPEN ||
                    sessionState == SessionState.CLOSED) {
                startSession();
            }
        } else {
            // After 17:00
            if (sessionState == SessionState.IN_PROGRESS ||
                    sessionState == SessionState.DOORS_OPEN) {
                endSession();
            }
        }
    }

    /**
     * End the current session.
     */
    private void endSession() {
        sessionState   = SessionState.ENDED;
        playerInside   = false;
        pensionerNpcs.clear();
        callerNpc = null;
    }

    // ── Number calling ────────────────────────────────────────────────────────

    /**
     * Publicly callable for tests — call a specific number and process results.
     */
    public void callNumber(int number,
                           Inventory playerInventory,
                           RumourNetwork rumourNetwork,
                           List<NPC> allNpcs,
                           NotorietySystem notorietySystem,
                           CriminalRecord criminalRecord,
                           NotorietySystem.AchievementCallback achievementCallback,
                           TooltipSystem tooltipSystem) {
        lastCalledNumber = number;

        // Caller speech bubble
        String callText = (number >= 1 && number <= 90 && BINGO_CALLS[number] != null)
                          ? BINGO_CALLS[number]
                          : "Number " + number + "!";
        if (callerNpc != null) {
            callerNpc.setSpeechText(callText, 55f);
        }

        // Auto-dab player's card
        if (playerInside && playerCard != null && !playerEjected) {
            playerCard.dab(number);

            // Cheat detection
            if (playerUsedRiggedCard) {
                float detectionChance = cheatDetectionOverride >= 0
                                        ? cheatDetectionOverride
                                        : CHEAT_DETECTION_PROBABILITY;
                if (random.nextFloat() < detectionChance) {
                    ejectPlayerForCheating(notorietySystem, criminalRecord,
                                           achievementCallback, tooltipSystem);
                    return;
                }
            }

            // Check for win
            if (!fullHouseWon && playerCard.hasFullHouse()) {
                awardFullHouse(playerInventory, rumourNetwork, allNpcs,
                               achievementCallback, tooltipSystem);
            } else if (!lineWon && playerCard.hasLine()) {
                awardLine(playerInventory, rumourNetwork, allNpcs,
                          achievementCallback, tooltipSystem);
            }
        }

        // Pensioners auto-dab and might win before player
        if (!fullHouseWon && !pensionerWon) {
            for (NPC pensioner : pensionerNpcs) {
                // Simulate pensioner dabbing — just count calls; a pensioner
                // will complete their card after ~45 numbers on average.
                // We use the call index as a proxy: at callIndex >= 45 the
                // first pensioner "wins" if the player hasn't.
                if (callIndex >= 45) {
                    pensionerWin(playerInventory, allNpcs, tooltipSystem);
                    break;
                }
            }
        }
    }

    // ── Win handlers ──────────────────────────────────────────────────────────

    private void awardLine(Inventory playerInventory,
                           RumourNetwork rumourNetwork,
                           List<NPC> allNpcs,
                           NotorietySystem.AchievementCallback achievementCallback,
                           TooltipSystem tooltipSystem) {
        lineWon = true;
        playerInventory.addItem(Material.COIN, LINE_PRIZE_COIN);
        if (callerNpc != null) {
            callerNpc.setSpeechText("LINE! We have a LINE!", 4.0f);
        }
        if (tooltipSystem != null) {
            tooltipSystem.showMessage("LINE! You win 5 COIN!", 4.0f);
        }
        // Seed a WINNER rumour
        seedWinnerRumour(rumourNetwork, allNpcs, "Someone just had a line at Lucky Stars!");
    }

    private void awardFullHouse(Inventory playerInventory,
                                RumourNetwork rumourNetwork,
                                List<NPC> allNpcs,
                                NotorietySystem.AchievementCallback achievementCallback,
                                TooltipSystem tooltipSystem) {
        fullHouseWon = true;
        lineWon      = true; // full house implies line
        playerInventory.addItem(Material.COIN, FULL_HOUSE_PRIZE_COIN);

        if (callerNpc != null) {
            callerNpc.setSpeechText("FULL HOUSE! Eyes down everyone!", 4.0f);
        }
        if (tooltipSystem != null) {
            tooltipSystem.showMessage("FULL HOUSE! You win 15 COIN!", 4.0f);
        }

        // BINGO_TROPHY_PROP placed at player position (WorldGenerator/propSystem
        // integration handled by RagamuffinGame via isTrophyPending flag)
        trophyPending = true;

        // Seed WINNER rumour
        seedWinnerRumour(rumourNetwork, allNpcs,
            "Someone just had a full house at Lucky Stars — walked away with fifteen coin!");

        // Achievements
        if (!playerUsedRiggedCard) {
            achievementCallback.award(AchievementType.EYES_DOWN);
        } else {
            achievementCallback.award(AchievementType.CHEEKY_DABBER);
        }

        endSession();
    }

    /** Whether a BINGO_TROPHY_PROP should be placed (consumed by RagamuffinGame). */
    private boolean trophyPending = false;

    public boolean isTrophyPending() { return trophyPending; }
    public void clearTrophyPending() { trophyPending = false; }

    private void pensionerWin(Inventory playerInventory,
                              List<NPC> allNpcs,
                              TooltipSystem tooltipSystem) {
        pensionerWon = true;

        // First pensioner shouts BINGO
        NPC winner = pensionerNpcs.isEmpty() ? null : pensionerNpcs.get(0);
        if (winner != null) {
            winner.setSpeechText("BINGO!", 4.0f);
        }

        // Player consolation prize
        if (playerInside && !playerEjected) {
            playerInventory.addItem(Material.COIN, CONSOLATION_PRIZE_COIN);
            if (tooltipSystem != null) {
                tooltipSystem.showMessage(
                    "A pensioner won first. You get 1 COIN consolation. Gutted.", 4.0f);
            }
        }

        endSession();
    }

    private void seedWinnerRumour(RumourNetwork rumourNetwork,
                                  List<NPC> allNpcs,
                                  String text) {
        Rumour rumour = new Rumour(RumourType.WINNER, text);
        for (NPC npc : allNpcs) {
            if (npc.isAlive() && npc.getType() != NPCType.CALLER) {
                rumourNetwork.addRumour(npc, rumour);
                break;
            }
        }
    }

    // ── Cheat / ejection ─────────────────────────────────────────────────────

    private void ejectPlayerForCheating(NotorietySystem notorietySystem,
                                        CriminalRecord criminalRecord,
                                        NotorietySystem.AchievementCallback achievementCallback,
                                        TooltipSystem tooltipSystem) {
        playerEjected = true;
        playerInside  = false;

        if (callerNpc != null) {
            callerNpc.setSpeechText(
                "We've got a cheater — number 9! Get out!", 5.0f);
        }
        // Boo from pensioners
        for (NPC pensioner : pensionerNpcs) {
            pensioner.setSpeechText("Boo!", 3.0f);
        }

        notorietySystem.addNotoriety(NOTORIETY_CHEAT_CAUGHT, achievementCallback);
        notorietySystem.addNotoriety(NOTORIETY_EJECTED, achievementCallback);
        criminalRecord.record(CriminalRecord.CrimeType.BINGO_CHEATING);

        if (tooltipSystem != null) {
            tooltipSystem.showMessage("You absolute melt.", 3.0f);
        }
    }

    // ── False BINGO (B key) ───────────────────────────────────────────────────

    /**
     * Player presses B to shout BINGO.
     *
     * @return message to show player, or null if not in session
     */
    public String shoutBingo(Inventory playerInventory,
                             RumourNetwork rumourNetwork,
                             List<NPC> allNpcs,
                             NotorietySystem notorietySystem,
                             CriminalRecord criminalRecord,
                             NotorietySystem.AchievementCallback achievementCallback,
                             TooltipSystem tooltipSystem) {
        if (!playerInside || sessionState != SessionState.IN_PROGRESS
                || playerEjected || playerCard == null) {
            return null;
        }

        if (playerCard.hasFullHouse()) {
            // Legitimate full house
            awardFullHouse(playerInventory, rumourNetwork, allNpcs,
                           achievementCallback, tooltipSystem);
            return "BINGO! Full house — you win!";
        }

        if (playerCard.hasLine() && !lineWon) {
            // Legitimate line
            awardLine(playerInventory, rumourNetwork, allNpcs,
                      achievementCallback, tooltipSystem);
            return "LINE! You win 5 COIN!";
        }

        // Card is incomplete
        if (falseShouteUsed) {
            // Cooldown — ignore silently
            return null;
        }

        falseShouteUsed = true;

        // CALLER checks
        if (callerNpc != null) {
            callerNpc.setSpeechText(
                "Just bear with me, love — let me check that card...", 3.0f);
        }

        // 3-second delay simulated by just resolving immediately in update
        // (for test purposes the penalty is applied immediately)
        if (callerNpc != null) {
            callerNpc.setSpeechText("False call! Shame on you!", 4.0f);
        }

        // All NPCs laugh
        for (NPC npc : allNpcs) {
            if (npc.getType() == NPCType.PENSIONER) {
                npc.setSpeechText("Ha!", 3.0f);
            }
        }

        // Player loses entry fee (already paid; no refund — just log it)
        if (tooltipSystem != null) {
            tooltipSystem.showMessage("You absolute bellend.", 3.0f);
        }

        // Session continues
        return "False call! Shame on you!";
    }

    // ── Pensioner solidarity ──────────────────────────────────────────────────

    /**
     * Called when player punches/shoves a pensioner inside the hall.
     * All other pensioners react; player is ejected.
     *
     * @param notorietySystem         for ejection notoriety
     * @param neighbourhoodWatchSystem for anger +10
     * @param achievementCallback     for SOLIDARITY achievement
     * @param tooltipSystem           for player messages
     */
    public void onPlayerAssaultedPensioner(NotorietySystem notorietySystem,
                                           NeighbourhoodWatchSystem neighbourhoodWatchSystem,
                                           NotorietySystem.AchievementCallback achievementCallback,
                                           TooltipSystem tooltipSystem) {
        if (!playerInside || sessionState != SessionState.IN_PROGRESS) return;

        // All remaining pensioners react
        for (NPC pensioner : pensionerNpcs) {
            pensioner.setSpeechText("Disgraceful!", 5.0f);
        }

        // Eject player
        playerEjected = true;
        playerInside  = false;

        notorietySystem.addNotoriety(NOTORIETY_EJECTED, achievementCallback);
        neighbourhoodWatchSystem.addAnger(WATCH_ANGER_PENSIONER_ASSAULT);

        achievementCallback.award(AchievementType.SOLIDARITY);

        if (tooltipSystem != null) {
            tooltipSystem.showMessage(
                "The pensioners rise as one. You've been ejected.", 4.0f);
        }
    }

    // ── Cheat detection override (for tests) ─────────────────────────────────

    /**
     * Override cheat detection probability. Set to 1.0f for guaranteed detection,
     * -1f to use default (5%).
     */
    public void setCheatDetectionOverride(float probability) {
        this.cheatDetectionOverride = probability;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public SessionState getSessionState() { return sessionState; }
    public boolean isPlayerInside()       { return playerInside; }
    public BingoCard getPlayerCard()      { return playerCard; }
    public int getLastCalledNumber()      { return lastCalledNumber; }
    public boolean isLineWon()            { return lineWon; }
    public boolean isFullHouseWon()       { return fullHouseWon; }
    public boolean isPensionerWon()       { return pensionerWon; }
    public boolean isPlayerEjected()      { return playerEjected; }
    public boolean isFalseShouteUsed()    { return falseShouteUsed; }
    public NPC getCallerNpc()             { return callerNpc; }
    public List<NPC> getPensionerNpcs()   { return Collections.unmodifiableList(pensionerNpcs); }

    /** Force set the caller NPC (for tests). */
    public void setCallerNpc(NPC npc)     { this.callerNpc = npc; }

    /** Force-add a pensioner NPC to the session (for tests). */
    public void addPensionerNpc(NPC npc)  { pensionerNpcs.add(npc); }

    /** Force the player inside the session with a specific card (for tests). */
    public void forcePlayerInside(BingoCard card) {
        this.playerInside = true;
        this.playerCard   = card;
        if (sessionState != SessionState.IN_PROGRESS) {
            startSession();
        }
    }

    /** Force the player inside with a rigged card flag (for tests). */
    public void forcePlayerInsideRigged(BingoCard card) {
        forcePlayerInside(card);
        this.playerUsedRiggedCard = true;
    }

    /** Returns true if player was ejected this session. */
    public boolean wasEjected() { return playerEjected; }
}

package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Issue #774: The Daily Ragamuffin — Living Tabloid Newspaper &amp; Infamy Chronicle.
 *
 * <p>Every evening at 18:00 game-time, a new edition of <em>The Daily Ragamuffin</em> is
 * published, chronicling the player's criminal exploits in lurid, sensationalist prose.
 *
 * <h3>Publication</h3>
 * <ul>
 *   <li>Infamy score 1–3: Minor crime → filler-level headline</li>
 *   <li>Infamy score 4–5: Notable crime → short front-page item</li>
 *   <li>Infamy score 6–7: Major crime → dramatic headline, triggers police Heightened Alert</li>
 *   <li>Infamy score 8–9: Spectacular crime → borough-wide chaos headline</li>
 *   <li>Infamy score 10:  Legendary crime → "BRITAIN'S MOST WANTED" headline</li>
 *   <li>Infamy score 0:   No crime → pigeon filler ("PIGEON MENACE GRIPS TOWN CENTRE")</li>
 * </ul>
 *
 * <h3>NPC Reactions</h3>
 * <ul>
 *   <li>PUBLIC NPCs comment on the headline, may recognise the player</li>
 *   <li>POLICE NPCs get Heightened Alert for 5 minutes on 7+ infamy stories</li>
 *   <li>FENCE NPCs give +10% on stolen items named in heist stories</li>
 *   <li>BARMAN NPCs seed the headline as a {@link RumourType#LOOT_TIP} or
 *       {@link RumourType#GANG_ACTIVITY} rumour</li>
 *   <li>Faction NPCs: Respect ±5 for the named faction</li>
 * </ul>
 *
 * <h3>Press Manipulation</h3>
 * <ol>
 *   <li>Tip-Off (5 COIN): force a past action into the next edition</li>
 *   <li>Plant a Lie (15 COIN): frame a rival faction NPC; police pursue them for 3 minutes</li>
 *   <li>Buy Out (40 COIN): suppress the story; replace with pigeon filler</li>
 * </ol>
 *
 * <h3>Integration</h3>
 * <ul>
 *   <li>{@link NotorietySystem}: +3 Notoriety on 7+ infamy publication</li>
 *   <li>{@link WantedSystem}: Heightened Alert buff (LOS +4, 5 min)</li>
 *   <li>{@link RumourNetwork}: Barman seeds headline as rumour</li>
 *   <li>{@link FactionSystem}: Respect ±5 for named faction</li>
 *   <li>{@link FenceSystem}: +10% for items named in front page</li>
 *   <li>{@link StreetEconomySystem}: GREGGS_STRIKE if Greggs featured</li>
 *   <li>{@link CriminalRecord}: logs PRESS_INFAMY</li>
 * </ul>
 */
public class NewspaperSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Game-time hour at which a new edition is published each day. */
    public static final float PUBLICATION_HOUR = 18.0f;

    /** Infamy score at or above which police Heightened Alert is triggered. */
    public static final int HEIGHTENED_ALERT_INFAMY_THRESHOLD = 7;

    /** Notoriety gain on front-page publication when infamy ≥ threshold. */
    public static final int NOTORIETY_GAIN_ON_PUBLICATION = 3;

    /** Coin cost for a Tip-Off press manipulation. */
    public static final int TIP_OFF_COST = 5;

    /** Coin cost for a Plant a Lie press manipulation. */
    public static final int PLANT_LIE_COST = 15;

    /** Coin cost for a Buy Out press manipulation. */
    public static final int BUY_OUT_COST = 40;

    /** Number of consecutive newspaper collections to earn REGULAR_READER. */
    public static final int REGULAR_READER_DAYS = 7;

    /** Number of suppressed stories needed for NO_COMMENT achievement. */
    public static final int NO_COMMENT_COUNT = 3;

    /** Number of filler-only days needed for PIGEON_MENACE achievement. */
    public static final int PIGEON_MENACE_DAYS = 5;

    /** Duration (seconds) that a framed NPC is pursued by police. */
    public static final float FRAMED_PURSUIT_DURATION = 180f; // 3 in-game minutes

    /** Pub hours: journalist spawn start. */
    public static final float JOURNALIST_SPAWN_HOUR = 19.0f;

    /** Pub hours: journalist spawn end. */
    public static final float JOURNALIST_DESPAWN_HOUR = 22.0f;

    // ── Headline templates by infamy band ─────────────────────────────────────

    private static final String[] TEMPLATES_MINOR = {
        "LOCAL MAN QUESTIONED BY POLICE",
        "TOWN CENTRE DISTURBANCE: ONE DETAINED",
        "RESIDENT WARNED BY OFFICERS AFTER INCIDENT",
        "PCSO ISSUES FIRM TALKING-TO NEAR {LANDMARK}",
    };

    private static final String[] TEMPLATES_NOTABLE = {
        "BRAZEN RAIDERS TARGET {LANDMARK}",
        "JEWELLERS TARGETED IN BROAD DAYLIGHT HEIST",
        "PROTECTION RACKET EXPOSED ON HIGH STREET",
        "{ITEM} STOLEN FROM {LANDMARK}: POLICE APPEAL",
    };

    private static final String[] TEMPLATES_MAJOR = {
        "WANTED FUGITIVE TERRORISES HIGH STREET",
        "BOROUGH IN CHAOS AFTER {WANTED_STARS}-STAR PURSUIT",
        "GREGGS RAIDED: PUBLIC OUTRAGED",
        "GANG WAR ERUPTS NEAR {LANDMARK}: WITNESSES FLEE",
        "{FACTION} BLAMED FOR SERIES OF INCIDENTS",
    };

    private static final String[] TEMPLATES_SPECTACULAR = {
        "BOROUGH IN CHAOS: CRIMINAL MASTERMIND EVADES FEDS",
        "ENTIRE ESTATE ON LOCKDOWN AFTER {WANTED_STARS}-STAR INCIDENT",
        "MASKED RAIDER EMPTIES {LANDMARK} IN AUDACIOUS HEIST",
        "BRAZEN CROOK ESCAPES ARMED RESPONSE USING {ESCAPE_METHOD}",
    };

    private static final String[] TEMPLATES_LEGENDARY = {
        "RAGAMUFFIN: BRITAIN'S MOST WANTED — EXCLUSIVE",
        "THE UNTOUCHABLE: BOROUGH'S MASTER CRIMINAL STRIKES AGAIN",
        "POLICE BAFFLED AS RAGAMUFFIN EVADES FIFTH PURSUIT",
    };

    /** Filler headline used when infamy score is 0 or story is suppressed. */
    public static final String PIGEON_FILLER = "PIGEON MENACE GRIPS TOWN CENTRE";

    /** Brief item used for council announcements (filler edition). */
    private static final String COUNCIL_FILLER = "COUNCIL UNVEILS NEW PLANNING APPLICATION";

    // ── Inner classes ─────────────────────────────────────────────────────────

    /**
     * Captures a significant player action for inclusion in a newspaper edition.
     */
    public static class InfamyEvent {

        private final String actionType;      // e.g. "HEIST", "CHASE", "GREGGS_RAID"
        private final String landmarkName;    // e.g. "Jewellers", "Greggs"
        private final Material stolenItem;    // may be null
        private final String npcName;         // NPC witness/victim name, may be null
        private final int wantedStars;        // wanted level at time of event
        private final String escapeMethod;    // e.g. "DISGUISE", "LEG_IT", "HIDING"
        private final Faction faction;        // faction involved, may be null
        private final int infamyScore;        // 1–10

        public InfamyEvent(String actionType, String landmarkName, Material stolenItem,
                           String npcName, int wantedStars, String escapeMethod,
                           Faction faction, int infamyScore) {
            this.actionType = actionType;
            this.landmarkName = landmarkName;
            this.stolenItem = stolenItem;
            this.npcName = npcName;
            this.wantedStars = Math.max(0, Math.min(5, wantedStars));
            this.escapeMethod = escapeMethod;
            this.faction = faction;
            this.infamyScore = Math.max(1, Math.min(10, infamyScore));
        }

        public String getActionType()  { return actionType; }
        public String getLandmarkName() { return landmarkName; }
        public Material getStolenItem() { return stolenItem; }
        public String getNpcName()     { return npcName; }
        public int getWantedStars()    { return wantedStars; }
        public String getEscapeMethod() { return escapeMethod; }
        public Faction getFaction()    { return faction; }
        public int getInfamyScore()    { return infamyScore; }
    }

    /**
     * A published edition of The Daily Ragamuffin.
     * Immutable value object.
     */
    public static class Newspaper {

        private final String headline;
        private final List<String> briefs;
        private final List<String> classifieds;
        private final int infamyScore;
        private final int editionDate; // in-game day number

        public Newspaper(String headline, List<String> briefs, List<String> classifieds,
                         int infamyScore, int editionDate) {
            this.headline = headline;
            this.briefs = Collections.unmodifiableList(new ArrayList<>(briefs));
            this.classifieds = Collections.unmodifiableList(new ArrayList<>(classifieds));
            this.infamyScore = infamyScore;
            this.editionDate = editionDate;
        }

        public String getHeadline()      { return headline; }
        public List<String> getBriefs()  { return briefs; }
        public List<String> getClassifieds() { return classifieds; }
        public int getInfamyScore()      { return infamyScore; }
        public int getEditionDate()      { return editionDate; }

        /** True if this paper is "current" — within 2 in-game days of today. */
        public boolean isCurrent(int currentDay) {
            return (currentDay - editionDate) <= 2;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Events accumulated since the last publication. */
    private final List<InfamyEvent> pendingEvents = new ArrayList<>();

    /** Tip-off event queued by the player (forced into the next edition). */
    private InfamyEvent tipOffEvent = null;

    /** Whether the current edition's story has been suppressed by a buyout. */
    private boolean buyOutActive = false;

    /** The most recently published newspaper, or null if none yet. */
    private Newspaper latestPaper = null;

    /** Whether a publication has happened on the current in-game day. */
    private int lastPublishedDay = -1;

    /** Whether the publication trigger has been armed (we passed 18:00 today). */
    private boolean publicationArmed = false;

    /** Consecutive days the player has collected the newspaper. */
    private int consecutiveCollectionDays = 0;

    /** Last in-game day the player collected a newspaper. */
    private int lastCollectionDay = -1;

    /** Number of stories suppressed via buyout (for NO_COMMENT achievement). */
    private int suppressionCount = 0;

    /** Consecutive days without making the front page (for PIGEON_MENACE). */
    private int consecutiveFillerDays = 0;

    /** Whether TABLOID_KINGPIN has been awarded. */
    private boolean tabloidKingpinAwarded = false;

    /** Whether REGULAR_READER has been awarded. */
    private boolean regularReaderAwarded = false;

    /** Whether FRONT_PAGE_VILLAIN has been awarded. */
    private boolean frontPageVillainAwarded = false;

    /** Whether NO_COMMENT has been awarded. */
    private boolean noCommentAwarded = false;

    /** Whether PIGEON_MENACE has been awarded. */
    private boolean pigeonMenaceAwarded = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public NewspaperSystem() {
        this(new Random());
    }

    public NewspaperSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Advance the newspaper system by one frame.
     *
     * <p>Publishes a new edition at 18:00 each in-game day. Each call requires the
     * dependent systems to apply the publication effects.
     *
     * @param delta               seconds since last frame
     * @param currentHour         current in-game hour (0.0–24.0)
     * @param currentDay          current in-game day number
     * @param notorietySystem     for adding notoriety on major publication (may be null)
     * @param wantedSystem        for triggering Heightened Alert (may be null)
     * @param rumourNetwork       for seeding barman rumour (may be null)
     * @param barmanNpc           the barman NPC (may be null)
     * @param factionSystem       for applying respect shifts (may be null)
     * @param fenceSystem         for applying headline price bonus (may be null)
     * @param streetEconomySystem for triggering GREGGS_STRIKE (may be null)
     * @param criminalRecord      for logging PRESS_INFAMY (may be null)
     * @param allNpcs             all living NPCs in the world (may be null/empty)
     * @param achievementCallback for awarding achievements (may be null)
     */
    public void update(float delta, float currentHour, int currentDay,
                       NotorietySystem notorietySystem,
                       WantedSystem wantedSystem,
                       RumourNetwork rumourNetwork,
                       NPC barmanNpc,
                       FactionSystem factionSystem,
                       FenceSystem fenceSystem,
                       StreetEconomySystem streetEconomySystem,
                       CriminalRecord criminalRecord,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback) {

        // Arm the publication trigger just before 18:00
        if (currentHour < PUBLICATION_HOUR - 0.1f) {
            publicationArmed = true;
        }

        // Fire publication once per day at 18:00
        if (publicationArmed && currentHour >= PUBLICATION_HOUR && lastPublishedDay != currentDay) {
            publicationArmed = false;
            lastPublishedDay = currentDay;

            Newspaper paper = publishEdition(pendingEvents, currentDay,
                notorietySystem, wantedSystem, rumourNetwork, barmanNpc,
                factionSystem, fenceSystem, streetEconomySystem, criminalRecord,
                allNpcs, achievementCallback);

            latestPaper = paper;
            pendingEvents.clear();
            tipOffEvent = null;
            buyOutActive = false;
        }
    }

    // ── Publication ───────────────────────────────────────────────────────────

    /**
     * Generate and publish a new edition based on the provided events.
     *
     * @param events              infamy events since last edition (may be empty)
     * @param editionDate         the in-game day number for this edition
     * @param notorietySystem     may be null
     * @param wantedSystem        may be null
     * @param rumourNetwork       may be null
     * @param barmanNpc           may be null
     * @param factionSystem       may be null
     * @param fenceSystem         may be null
     * @param streetEconomySystem may be null
     * @param criminalRecord      may be null
     * @param allNpcs             may be null or empty
     * @param achievementCallback may be null
     * @return the published Newspaper
     */
    public Newspaper publishEdition(List<InfamyEvent> events, int editionDate,
                                     NotorietySystem notorietySystem,
                                     WantedSystem wantedSystem,
                                     RumourNetwork rumourNetwork,
                                     NPC barmanNpc,
                                     FactionSystem factionSystem,
                                     FenceSystem fenceSystem,
                                     StreetEconomySystem streetEconomySystem,
                                     CriminalRecord criminalRecord,
                                     List<NPC> allNpcs,
                                     NotorietySystem.AchievementCallback achievementCallback) {

        // Determine the best event (highest infamy score), with tip-off override
        InfamyEvent frontPageEvent = chooseFrontPageEvent(events);

        // Build the newspaper
        String headline;
        int infamyScore;

        if (buyOutActive || frontPageEvent == null) {
            // Filler edition
            headline = PIGEON_FILLER;
            infamyScore = 0;
            consecutiveFillerDays++;
            checkPigeonMenaceAchievement(achievementCallback);
        } else {
            infamyScore = frontPageEvent.getInfamyScore();
            headline = generateHeadline(frontPageEvent);
            consecutiveFillerDays = 0; // reset streak

            // Front-page effects
            applyFrontPageEffects(frontPageEvent, notorietySystem, wantedSystem,
                rumourNetwork, barmanNpc, factionSystem, fenceSystem,
                streetEconomySystem, criminalRecord, allNpcs, achievementCallback);
        }

        List<String> briefs = generateBriefs(events, frontPageEvent);
        List<String> classifieds = generateClassifieds();

        Newspaper paper = new Newspaper(headline, briefs, classifieds, infamyScore, editionDate);

        // Award FRONT_PAGE_VILLAIN if infamy == 10
        if (infamyScore == 10 && !frontPageVillainAwarded && achievementCallback != null) {
            frontPageVillainAwarded = true;
            achievementCallback.award(AchievementType.FRONT_PAGE_VILLAIN);
        }

        return paper;
    }

    // ── Front-page effects ────────────────────────────────────────────────────

    private void applyFrontPageEffects(InfamyEvent event,
                                        NotorietySystem notorietySystem,
                                        WantedSystem wantedSystem,
                                        RumourNetwork rumourNetwork,
                                        NPC barmanNpc,
                                        FactionSystem factionSystem,
                                        FenceSystem fenceSystem,
                                        StreetEconomySystem streetEconomySystem,
                                        CriminalRecord criminalRecord,
                                        List<NPC> allNpcs,
                                        NotorietySystem.AchievementCallback achievementCallback) {
        int infamy = event.getInfamyScore();

        // NotorietySystem: +3 on 7+ infamy
        if (infamy >= HEIGHTENED_ALERT_INFAMY_THRESHOLD && notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_GAIN_ON_PUBLICATION, achievementCallback);
        }

        // WantedSystem: Heightened Alert on 7+ infamy
        if (infamy >= HEIGHTENED_ALERT_INFAMY_THRESHOLD && wantedSystem != null) {
            wantedSystem.triggerHeightenedAlert();
        }

        // RumourNetwork: seed barman rumour
        if (rumourNetwork != null && barmanNpc != null) {
            RumourType rumourType = isLootStory(event) ? RumourType.LOOT_TIP : RumourType.GANG_ACTIVITY;
            String rumourText = "Did you read the paper? " + generateHeadline(event);
            Rumour rumour = new Rumour(rumourType, rumourText);
            rumourNetwork.addRumour(barmanNpc, rumour);
        }

        // FactionSystem: ±5 respect for named faction
        if (factionSystem != null && event.getFaction() != null) {
            // Positive coverage for the named faction (+5)
            Faction named = event.getFaction();
            factionSystem.applyRespectDelta(named, 5);
        }

        // FenceSystem: headline price bonus for stolen item
        if (fenceSystem != null && event.getStolenItem() != null) {
            fenceSystem.setHeadlinePriceBonus(event.getStolenItem());
        }

        // StreetEconomySystem: GREGGS_STRIKE if Greggs featured
        if (streetEconomySystem != null && isGregsStory(event) && allNpcs != null) {
            streetEconomySystem.triggerMarketEvent(MarketEvent.GREGGS_STRIKE, allNpcs, (RumourNetwork) null);
        }

        // CriminalRecord: log PRESS_INFAMY
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.PRESS_INFAMY);
        }
    }

    // ── NPC reactions ─────────────────────────────────────────────────────────

    /**
     * Called when an NPC reads a newspaper item. Updates the NPC's speech and
     * triggers any behaviour changes appropriate to their type.
     *
     * @param npc       the NPC reading the paper
     * @param newspaper the edition being read
     * @param rumourNetwork for seeding barman rumours (may be null)
     * @param wantedSystem  for triggering police heightened alert (may be null)
     */
    public void onNpcReadsNewspaper(NPC npc, Newspaper newspaper,
                                     RumourNetwork rumourNetwork,
                                     WantedSystem wantedSystem) {
        if (npc == null || newspaper == null) return;

        String headline = newspaper.getHeadline();
        int infamy = newspaper.getInfamyScore();

        if (npc.getType() == NPCType.PUBLIC) {
            if (infamy >= HEIGHTENED_ALERT_INFAMY_THRESHOLD) {
                npc.setSpeechText("Did you see the paper? Proper mad round here.", 5.0f);
            } else if (infamy > 0) {
                npc.setSpeechText("Did you see the paper? " + headline, 5.0f);
            } else {
                npc.setSpeechText("Only pigeons in the news today. Typical.", 4.0f);
            }
        } else if (npc.getType() == NPCType.POLICE || npc.getType() == NPCType.PCSO) {
            if (infamy >= HEIGHTENED_ALERT_INFAMY_THRESHOLD && wantedSystem != null) {
                wantedSystem.triggerHeightenedAlert();
                npc.setSpeechText("All units, Heightened Alert. Someone's been busy.", 5.0f);
            }
        } else if (npc.getType() == NPCType.BARMAN) {
            if (infamy > 0 && rumourNetwork != null) {
                RumourType rType = isLootHeadline(headline) ? RumourType.LOOT_TIP : RumourType.GANG_ACTIVITY;
                Rumour r = new Rumour(rType, "Heard about it in the paper: " + headline);
                rumourNetwork.addRumour(npc, r);
                npc.setSpeechText("Seen the paper? Mad times.", 4.0f);
            }
        } else if (npc.getType() == NPCType.FENCE) {
            if (infamy >= 4) {
                npc.setSpeechText("Hot right now, mate — everyone wants one.", 4.0f);
            }
        } else if (npc.getType() == NPCType.JOURNALIST) {
            npc.setSpeechText("Front page again. You're making my job easy.", 4.0f);
        }
    }

    // ── Press manipulation ────────────────────────────────────────────────────

    /**
     * Tip-off the journalist to guarantee a specific event appears in the next edition.
     *
     * @param event     the event to leak
     * @param inventory the player's inventory (5 COIN is deducted)
     * @return true if the tip-off succeeded, false if insufficient COIN or bad args
     */
    public boolean tipOffJournalist(InfamyEvent event, Inventory inventory) {
        if (event == null || inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < TIP_OFF_COST) return false;
        inventory.removeItem(Material.COIN, TIP_OFF_COST);
        tipOffEvent = event;
        return true;
    }

    /**
     * Pay to plant a false story framing a rival faction NPC.
     *
     * @param framedNpc the NPC to be framed (should be a rival faction NPC)
     * @param inventory the player's inventory (15 COIN is deducted)
     * @param factionSystem for applying −10 respect to the framed faction
     * @param achievementCallback for awarding TABLOID_KINGPIN
     * @return true if the lie was planted, false if insufficient COIN or bad args
     */
    public boolean plantALie(NPC framedNpc, Inventory inventory,
                              FactionSystem factionSystem,
                              NotorietySystem.AchievementCallback achievementCallback) {
        if (framedNpc == null || inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < PLANT_LIE_COST) return false;
        inventory.removeItem(Material.COIN, PLANT_LIE_COST);

        // Apply faction respect penalty
        if (factionSystem != null) {
            Faction targetFaction = factionForNpcType(framedNpc.getType());
            if (targetFaction != null) {
                factionSystem.applyRespectDelta(targetFaction, -10);
            }
        }

        // Award TABLOID_KINGPIN
        if (!tabloidKingpinAwarded && achievementCallback != null) {
            tabloidKingpinAwarded = true;
            achievementCallback.award(AchievementType.TABLOID_KINGPIN);
        }

        return true;
    }

    /**
     * Pay to suppress a damaging story, replacing it with pigeon filler.
     *
     * @param inventory the player's inventory (40 COIN is deducted)
     * @param achievementCallback for awarding NO_COMMENT
     * @return true if the buyout succeeded, false if insufficient COIN
     */
    public boolean buyOutStory(Inventory inventory,
                                NotorietySystem.AchievementCallback achievementCallback) {
        if (inventory == null) return false;
        if (inventory.getItemCount(Material.COIN) < BUY_OUT_COST) return false;
        inventory.removeItem(Material.COIN, BUY_OUT_COST);
        buyOutActive = true;
        suppressionCount++;

        // NO_COMMENT achievement: 3 suppressions
        if (!noCommentAwarded && suppressionCount >= NO_COMMENT_COUNT && achievementCallback != null) {
            noCommentAwarded = true;
            achievementCallback.award(AchievementType.NO_COMMENT);
        }

        return true;
    }

    // ── Player picks up newspaper ─────────────────────────────────────────────

    /**
     * Called when the player picks up a {@link Material#NEWSPAPER} item.
     * Tracks consecutive collection days for the REGULAR_READER achievement.
     *
     * @param editionDay          the in-game day of the edition being collected
     * @param achievementCallback may be null
     */
    public void pickUpNewspaper(int editionDay,
                                 NotorietySystem.AchievementCallback achievementCallback) {
        if (lastCollectionDay == editionDay - 1) {
            // Consecutive day
            consecutiveCollectionDays++;
        } else if (lastCollectionDay == editionDay) {
            // Same day — no change
        } else {
            // Streak broken
            consecutiveCollectionDays = 1;
        }
        lastCollectionDay = editionDay;

        if (!regularReaderAwarded && consecutiveCollectionDays >= REGULAR_READER_DAYS
                && achievementCallback != null) {
            regularReaderAwarded = true;
            achievementCallback.award(AchievementType.REGULAR_READER);
        }
    }

    // ── Event recording ───────────────────────────────────────────────────────

    /**
     * Record a player action as an infamy event for consideration in the next edition.
     *
     * @param event the event to record
     */
    public void recordEvent(InfamyEvent event) {
        if (event != null) {
            pendingEvents.add(event);
        }
    }

    // ── Headline generation ───────────────────────────────────────────────────

    /**
     * Generate a headline for the given infamy event by filling a template.
     */
    String generateHeadline(InfamyEvent event) {
        String[] templates = templatesForScore(event.getInfamyScore());
        String template = templates[random.nextInt(templates.length)];
        return fillTemplate(template, event);
    }

    private String[] templatesForScore(int infamyScore) {
        if (infamyScore <= 3) return TEMPLATES_MINOR;
        if (infamyScore <= 5) return TEMPLATES_NOTABLE;
        if (infamyScore <= 7) return TEMPLATES_MAJOR;
        if (infamyScore <= 9) return TEMPLATES_SPECTACULAR;
        return TEMPLATES_LEGENDARY;
    }

    private String fillTemplate(String template, InfamyEvent event) {
        String result = template;
        if (event.getLandmarkName() != null) {
            result = result.replace("{LANDMARK}", event.getLandmarkName().toUpperCase());
        }
        if (event.getStolenItem() != null) {
            result = result.replace("{ITEM}", event.getStolenItem().getDisplayName().toUpperCase());
        }
        result = result.replace("{WANTED_STARS}", String.valueOf(event.getWantedStars()));
        if (event.getEscapeMethod() != null) {
            result = result.replace("{ESCAPE_METHOD}", friendlyEscapeMethod(event.getEscapeMethod()));
        }
        if (event.getFaction() != null) {
            result = result.replace("{FACTION}", event.getFaction().getDisplayName().toUpperCase());
        }
        // Remove any unfilled placeholders
        result = result.replaceAll("\\{[A-Z_]+\\}", "UNKNOWN");
        return result;
    }

    private String friendlyEscapeMethod(String method) {
        switch (method.toUpperCase()) {
            case "DISGUISE": return "A DISGUISE";
            case "LEG_IT":   return "SHEER SPEED";
            case "HIDING":   return "A WHEELIE BIN";
            case "SAFE_HOUSE": return "A SAFE HOUSE";
            default:         return method;
        }
    }

    // ── Briefs & classifieds ──────────────────────────────────────────────────

    private List<String> generateBriefs(List<InfamyEvent> events, InfamyEvent frontPage) {
        List<String> briefs = new ArrayList<>();
        for (InfamyEvent e : events) {
            if (e == frontPage) continue;
            if (briefs.size() >= 3) break;
            briefs.add(generateBrief(e));
        }
        // Pad with generic local briefs
        while (briefs.size() < 2) {
            briefs.add(pickGenericBrief());
        }
        return briefs;
    }

    private String generateBrief(InfamyEvent event) {
        String loc = event.getLandmarkName() != null ? " near " + event.getLandmarkName() : "";
        return "BRIEF: Incident" + loc + " (Infamy " + event.getInfamyScore() + ").";
    }

    private String pickGenericBrief() {
        String[] options = {
            "COUNCIL PLANS REVIEWED: Three properties face demolition order.",
            "OFF-LICENCE: Delivery delayed; staff request police escort.",
            "JOB CENTRE: Record footfall as area unemployment reaches seasonal high.",
            "PARK PIGEONS: Breeding season declared 'out of hand' by estate managers.",
        };
        return options[random.nextInt(options.length)];
    }

    private List<String> generateClassifieds() {
        List<String> ads = new ArrayList<>();
        String[] pool = {
            "FENCE: Items of unusual provenance sought. Discretion assured. Ask at the industrial estate.",
            "JOB CENTRE: 17 positions available. Zero hours. Enthusiasm essential.",
            "GREGGS: Sausage rolls back in stock. Apologies for any inconvenience.",
            "COUNCIL: Planning notice — 14 Acacia Road. Demolition approved. Appeals by Friday.",
            "LOST: One dignity. Last seen near the park. Please call if found.",
        };
        // Pick 2–3 ads
        List<String> shuffled = new ArrayList<>(List.of(pool));
        Collections.shuffle(shuffled, random);
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            ads.add(shuffled.get(i));
        }
        return ads;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InfamyEvent chooseFrontPageEvent(List<InfamyEvent> events) {
        // Tip-off takes priority
        if (tipOffEvent != null) return tipOffEvent;

        InfamyEvent best = null;
        int highestScore = 0;
        for (InfamyEvent e : events) {
            if (e.getInfamyScore() > highestScore) {
                highestScore = e.getInfamyScore();
                best = e;
            }
        }
        return best;
    }

    private boolean isLootStory(InfamyEvent event) {
        String type = event.getActionType();
        return type != null && (type.contains("HEIST") || type.contains("LOOT") || type.contains("STEAL"));
    }

    private boolean isLootHeadline(String headline) {
        return headline.contains("JEWELLERS") || headline.contains("HEIST")
                || headline.contains("STOLEN") || headline.contains("RAIDERS");
    }

    private boolean isGregsStory(InfamyEvent event) {
        String landmark = event.getLandmarkName();
        String type = event.getActionType();
        return (landmark != null && landmark.toUpperCase().contains("GREGGS"))
                || (type != null && type.contains("GREGGS"));
    }

    private Faction factionForNpcType(NPCType type) {
        // Rough mapping: faction lieutenants → MARCHETTI_CREW or STREET_LADS
        if (type == NPCType.FACTION_LIEUTENANT || type == NPCType.STREET_LAD) {
            return Faction.STREET_LADS;
        } else if (type == NPCType.COUNCIL_MEMBER) {
            return Faction.THE_COUNCIL;
        }
        return null;
    }

    private void checkPigeonMenaceAchievement(NotorietySystem.AchievementCallback callback) {
        if (!pigeonMenaceAwarded && consecutiveFillerDays >= PIGEON_MENACE_DAYS && callback != null) {
            pigeonMenaceAwarded = true;
            callback.award(AchievementType.PIGEON_MENACE);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** The most recently published newspaper, or null if none published yet. */
    public Newspaper getLatestPaper() {
        return latestPaper;
    }

    /** All pending events recorded since the last publication. */
    public List<InfamyEvent> getPendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    /** The current tip-off event, or null. */
    public InfamyEvent getTipOffEvent() {
        return tipOffEvent;
    }

    /** Whether a buyout is active for the next publication. */
    public boolean isBuyOutActive() {
        return buyOutActive;
    }

    /** The day of the last publication, or -1. */
    public int getLastPublishedDay() {
        return lastPublishedDay;
    }

    /** Current consecutive collection days for REGULAR_READER tracking. */
    public int getConsecutiveCollectionDays() {
        return consecutiveCollectionDays;
    }

    /** Current count of suppressed stories (for NO_COMMENT tracking). */
    public int getSuppressionCount() {
        return suppressionCount;
    }

    /** Current consecutive filler days (for PIGEON_MENACE tracking). */
    public int getConsecutiveFillerDays() {
        return consecutiveFillerDays;
    }

    // ── Force-set for testing ──────────────────────────────────────────────────

    /** Force-arm the publication trigger (for testing). */
    void setPublicationArmedForTesting(boolean armed) {
        this.publicationArmed = armed;
    }

    /** Force-set the last published day (for testing). */
    void setLastPublishedDayForTesting(int day) {
        this.lastPublishedDay = day;
    }

    /** Force-set the tip-off event (for testing without going through tipOffJournalist). */
    void setTipOffEventForTesting(InfamyEvent event) {
        this.tipOffEvent = event;
    }
}

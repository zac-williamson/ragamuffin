package ragamuffin.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Tracks the player's criminal activity statistics across a session.
 *
 * <p>Records specific crime counts (e.g. pensioners punched, blocks destroyed,
 * shops raided) that the player has committed.  Displayed via {@link ragamuffin.ui.CriminalRecordUI}.
 */
public class CriminalRecord {

    /**
     * The categories of crime tracked by the record.
     * Add new entries here to extend the system without touching UI code.
     */
    public enum CrimeType {
        PENSIONERS_PUNCHED("Pensioners punched"),
        MEMBERS_OF_PUBLIC_PUNCHED("Members of public punched"),
        BLOCKS_DESTROYED("Blocks destroyed"),
        TIMES_ARRESTED("Times arrested"),
        SHOPS_RAIDED("Shops raided"),
        NPCS_KILLED("NPCs killed"),
        /** Issue #765: Added by police who find evidence or receive a WITNESS_SIGHTING rumour. */
        WITNESSED_CRIMES("Witnessed crimes on record"),

        /** Issue #774: Each time the player appears on the front page of The Daily Ragamuffin. */
        PRESS_INFAMY("Front-page appearances"),

        /**
         * Issue #781: Logged after 3 graffiti arrests. Triggers a solicitor quest
         * to secure a not-guilty plea.
         */
        CRIMINAL_DAMAGE("Criminal damage charges"),

        /**
         * Issue #797: Recorded by Watch Members during a soft citizen's arrest.
         * Two counts added per citizen's arrest at Tier 3+.
         */
        ANTISOCIAL_BEHAVIOUR("Antisocial behaviour charges"),

        /**
         * Issue #906: Recorded when police confiscate the player's BUCKET_DRUM
         * during a busking licence check.
         */
        UNLICENSED_BUSKING("Unlicensed busking"),

        /**
         * Issue #914: Recorded when the player enters the allotments outside warden
         * open hours (07:00–19:00).
         */
        TRESPASSING("Trespassing charges"),

        /**
         * Issue #918: Recorded when the player boards The Number 47 without paying
         * the fare and without a valid BUS_PASS.
         */
        FARE_EVASION("Fare evasion"),

        /**
         * Issue #920: Recorded when the player is caught by a police raid during
         * the pub lock-in after-hours session and fails to hide behind the bar counter.
         */
        DRUNK_AND_DISORDERLY("Drunk and disorderly charges"),

        /**
         * Issue #946: Recorded when a POLICE NPC inspects the player's off-lead dog
         * with Notoriety ≥ 50, or when the dog is used for intimidation and a police
         * NPC is within 15 blocks.
         */
        DANGEROUS_DOG("Dangerous dog offences"),

        /**
         * Issue #948: Recorded each time the player completes a full 3-minute shift
         * at the Sparkle Hand Car Wash. 3+ entries reduce the arrest fine by 20%
         * (arresting officer gives benefit of the doubt for legitimate employment).
         */
        LEGITIMATE_WORK("Legitimate employment (car wash shifts)"),

        /**
         * Issue #954: Recorded when the player is caught using a RIGGED_BINGO_CARD
         * at Lucky Stars Bingo Hall and ejected by the CALLER NPC.
         */
        BINGO_CHEATING("Bingo cheating offences"),

        /**
         * Issue #961: Recorded by WitnessSystem when the player sells a stolen item
         * at the pawn shop while a POLICE NPC is within 8 blocks.
         */
        HANDLING_STOLEN_GOODS("Handling stolen goods"),

        /**
         * Issue #969: Recorded when the GROUNDSKEEPER NPC witnesses the player
         * digging a grave plot in Northfield Cemetery (+2 Notoriety per witnessed dig).
         */
        GRAVE_ROBBING("Grave robbing"),

        /**
         * Issue #969: Recorded when the player attacks a MOURNER or FUNERAL_DIRECTOR
         * during a funeral procession.
         */
        DISTURBING_THE_PEACE("Disturbing the peace"),

        // ── Issue #971: The Rusty Anchor Wetherspoons ──────────────────────────────
        ASSAULT_IN_PUB("Assault in a licensed premises"),

        // ── Issue #973: Northfield GP Surgery ──────────────────────────────────────
        /**
         * Recorded when the player attempts to use a BLANK_PRESCRIPTION_FORM at the
         * pharmacy hatch and is caught by the pharmacist or a nearby police NPC.
         * Penalty: +15 Notoriety, +1 Wanted star.
         */
        PRESCRIPTION_FRAUD("Prescription fraud"),

        // ── Issue #975: Northfield Post Office ──────────────────────────────────────

        /**
         * Recorded when the player steals a PARCEL from a residential doorstep.
         * Base penalty: +5 Notoriety; +3 additional if witnessed by an NPC within 8 blocks.
         */
        PARCEL_THEFT("Parcel theft"),

        /**
         * Recorded when Maureen (COUNTER_CLERK) detects a stolen BENEFITS_BOOK
         * from a PENSIONER NPC being presented at the Post Office counter.
         * Detection chance: 40%. Penalty: +12 Notoriety, police called.
         */
        BENEFITS_FRAUD("Benefits fraud"),

        /**
         * Recorded when a threatening letter sent via POST_BOX_PROP is traced back
         * to the player at Notoriety Tier 3+.
         * Penalty: +10 Notoriety, 40% chance police investigate.
         */
        THREATENING_BEHAVIOUR("Threatening behaviour"),

        // ── Issue #979: Fix My Phone ─────────────────────────────────────────────

        /**
         * Recorded when the player clones a stolen phone in Tariq's back room at
         * Fix My Phone. Also triggered by WitnessSystem if a POLICE NPC enters the
         * shop during back-room cloning (along with Notoriety +3).
         */
        THEFT("Theft"),

        // ── Issue #983: Northfield Dog Track ────────────────────────────────────

        /**
         * Recorded when the player steals a GREYHOUND from the kennel at the
         * Northfield Dog Track using a LOCKPICK. Also triggered if a SECURITY_GUARD
         * or KENNEL_HAND witnesses the theft (+10 Notoriety).
         */
        ANIMAL_THEFT("Animal theft"),

        /**
         * Recorded when the player is witnessed bribing the KENNEL_HAND (10 COIN)
         * or slipping a DODGY_PIE to a greyhound. Triggered by SECURITY_GUARD or
         * KENNEL_HAND detection. Penalty: +10 Notoriety.
         */
        RACE_FIXING("Race fixing"),

        // ── Issue #985: Northfield Police Station ────────────────────────────

        /**
         * Recorded when the player breaks out of a custody cell using a LOCKPICK.
         * Penalty: WantedSystem +3 stars, station-wide hostile alert for 120 seconds.
         */
        ESCAPE_FROM_CUSTODY("Escape from custody"),

        /**
         * Recorded when a DETECTIVE or DETENTION_OFFICER catches the player inside
         * the evidence locker without authorisation.
         * Penalty: WantedSystem +2 stars, station-wide hostile alert.
         */
        EVIDENCE_TAMPERING("Evidence tampering"),

        /**
         * Recorded when the player provides a false tip to the DUTY_SERGEANT and is
         * caught (20% chance of immediate detection).
         * Penalty: +2 Notoriety.
         */
        FALSE_REPORT("False report"),

        // ── Issue #1000: Northfield Fire Station ──────────────────────────────

        /**
         * Recorded each time the player places a false alarm via the station noticeboard
         * or nearby phone box. +5 Notoriety per call. After 3 entries, FIREFIGHTER NPCs
         * become suspicious of the player.
         */
        FALSE_ALARM("False alarm (fire service)"),

        /**
         * Recorded when the player steals the fire engine from the station garage.
         * Penalty: +25 Notoriety, +3 Wanted stars; MAJOR_THEFT rumour seeded to 5 NPCs.
         */
        FIRE_ENGINE_STOLEN("Fire engine theft"),

        // ── Issue #1002: Northfield BP Petrol Station ──────────────────────────────

        /**
         * Recorded when the player uses a fuel pump and walks off the forecourt without
         * paying. Penalty: +5 Notoriety; cashier enters CHASING state for 20s.
         * Awards DRIVEOFF achievement on first offence.
         */
        PETROL_THEFT("Petrol theft (drive-off)"),

        // ── Issue #1026: Northfield Scrapyard ─────────────────────────────────────

        /**
         * Recorded when LEAD_FLASHING is stolen from a church roof or COPPER_WIRE
         * is stripped from a streetlight and a witness NPC is within 10 blocks.
         * Penalty: +2 Notoriety per witnessed theft.
         */
        METAL_THEFT("Metal theft"),

        // ── Issue #1030: Al-Noor Mosque ────────────────────────────────────────────

        /**
         * Recorded when the player robs the TAKINGS_BOX_PROP at Al-Noor Mosque.
         * Penalty: +3 Notoriety, −20 Community Respect, permanent sanctuary revocation,
         * COMMUNITY_OUTRAGE rumour seeded within 50 blocks.
         */
        THEFT_FROM_PLACE_OF_WORSHIP("Theft from place of worship"),

        // ── Issue #1037: Northfield Indoor Market ─────────────────────────────────

        /**
         * Recorded when Trading Standards officers discover counterfeit or stolen items
         * in the player's rented market stall during an IndoorMarketSystem raid.
         * Penalty: +20 Notoriety, +1 WantedSystem star, items confiscated.
         * Player has 60 frames to vacate stall before arrest.
         */
        TRADING_STANDARDS_RAID("Trading Standards raid (indoor market)"),

        // ── Issue #1051: Angel Nails & Beauty ─────────────────────────────────

        /**
         * Recorded when the player breaks into Angel Nails & Beauty after closing hours
         * (19:00–09:00). Penalty: +4 Notoriety; NeighbourhoodWatchSystem triggered.
         */
        BURGLARY("Burglary"),

        /**
         * Recorded when a POLICE NPC is within 20 blocks during the Marchetti voucher
         * scam at Angel Nails & Beauty. Penalty: +8 Notoriety.
         */
        MONEY_LAUNDERING("Money laundering"),

        // ── Issue #1057: Northfield Canal ─────────────────────────────────────────

        /**
         * Recorded when the player disposes of evidence in the canal while witnessed
         * by a PCSO or other officer. Penalty: +1 WantedSystem star.
         */
        EVIDENCE_DESTRUCTION("Evidence destruction (canal)"),

        // ── Issue #1079: Northfield Magistrates' Court ────────────────────────

        /**
         * Recorded when the player fails to attend a scheduled court appearance at
         * Northfield Magistrates' Court. Penalty: Notoriety +10, increased police patrols.
         */
        FAILURE_TO_APPEAR("Failure to appear"),

        /**
         * Recorded when the player uses a FORGED_DOCUMENT in court and is caught
         * (15% detection chance), or when the player successfully intimidates a witness
         * via WitnessSystem and this is subsequently discovered.
         * Penalty: Notoriety +15, +1 Wanted star.
         */
        PERVERTING_COURSE_OF_JUSTICE("Perverting the course of justice"),

        // ── Issue #1091: Northfield Nando's ─────────────────────────────────

        /**
         * Recorded when the player uses the card machine jam mechanic at Nando's
         * to obtain a free meal. Penalty: +8 Notoriety.
         */
        CARD_MACHINE_FRAUD("Card machine fraud"),

        /**
         * Recorded when the player throws PERI_PERI_SAUCE in a public place,
         * creating a PERI_SAUCE_SLICK hazard prop. Penalty: +3 Notoriety.
         */
        AFFRAY("Affray (sauce throwing)"),

        // ── Issue #1094: Northfield By-Election ──────────────────────────────

        /**
         * Recorded when the player steals the BALLOT_BOX_PROP on polling day.
         * Penalty: +25 Notoriety, +2 WantedSystem stars; NewspaperSystem headline published.
         * WitnessSystem records any witnesses within 15 blocks.
         * Faction: Neighbourhood Watch −5 respect.
         */
        ELECTION_INTERFERENCE("Election interference (ballot box theft)"),

        // ── Issue #1096: Sunday League Football ──────────────────────────────

        /**
         * Recorded when the player is caught slipping a DODGY_PIE to a Council FC
         * player during the Sunday League match (30% chance if referee within 6 blocks).
         * Penalty: +10 Notoriety; immediate red card; match ejection.
         */
        MATCH_FIXING("Match fixing"),

        /**
         * Recorded when the player is ejected from the Sunday League pitch for
         * verbally abusing the REFEREE NPC twice in one match.
         * Penalty: +3 Notoriety.
         */
        DISORDERLY_CONDUCT("Disorderly conduct"),

        /**
         * Recorded when the player punches the REFEREE NPC during the Sunday League
         * match. Triggers WantedSystem +2 stars.
         * Penalty: +8 Notoriety.
         */
        ASSAULT_OF_OFFICIAL("Assault of a match official"),

        // ── Issue #1100: Northfield Council Flats — Kendrick House ──────────

        /**
         * Recorded when Derek (COUNCIL_MEMBER) finds contraband (STOLEN_GOODS,
         * COUNTERFEIT_NOTE, DRUGS_EVIDENCE, STOLEN_PHONE, or a weapon) near the
         * player during the housing inspection at Kendrick House.
         * Penalty: +5 Notoriety, Wanted +1.
         */
        POSSESSION("Possession of contraband"),

        // ── Issue #1104: Northfield Community Centre ──────────────────────────

        /**
         * Recorded when the player sells any item to a SCHOOL_KID NPC during
         * the Thursday Youth Drop-in at the Community Centre.
         * Penalty: +20 Notoriety, WantedSystem +2 stars.
         * Text: "You sold to a kid? Even for Northfield, that's low."
         */
        SUPPLY_TO_MINOR("Supply of goods to a minor"),

        /**
         * Recorded when the player is caught adding ANTIDEPRESSANTS to a
         * competitor's cake during the Sunday Cake Bake-Off
         * (caught condition: NoiseSystem ≥ 20).
         * Penalty: +15 Notoriety, WantedSystem +2 stars.
         */
        POISONING("Food poisoning (Bake-Off sabotage)"),

        // ── Issue #1130: Northfield BP Petrol Station ────────────────────────

        /**
         * Recorded when the player siphons fuel from a parked car at the BP
         * forecourt between 21:00–06:00. Also recorded if CCTV_PROP is active
         * and the player is within its line of sight.
         * Penalty: +3 Notoriety; CCTV adds WantedSystem +1 star.
         */
        VEHICLE_TAMPERING("Vehicle tampering (fuel siphoning)"),

        /**
         * Recorded when the player throws a MOLOTOV_COCKTAIL and fire is
         * started. Also triggered by WheeliBinFireSystem when a petrol-boosted
         * fire is lit with a crafted incendiary.
         * Penalty: +20 Notoriety, WantedSystem +1 star.
         */
        ARSON("Arson"),

        /**
         * Recorded when the player robs the TILL_PROP at the BP kiosk using
         * a CROWBAR. Triggers a 3-minute police response (30 seconds if panic
         * button was not destroyed and Wayne is awake at Tier ≥ 2).
         * Penalty: +15 Notoriety, WantedSystem +2 stars.
         */
        ARMED_ROBBERY("Armed robbery (till raid)"),

        // ── Issue #1132: Northfield Dog Grooming Parlour — Pawfect Cuts ──────────

        /**
         * Recorded when the player bribes the JUDGE_NPC at the Northfield Dog Show
         * (15 COIN, +5 Notoriety per bribe). Part of the Crufts Conspiracy quest.
         * Penalty: +5 Notoriety; increases Marchetti Crew respect by +2 (they appreciate
         * the player playing dirty). Exposed by NewspaperSystem if a JOURNALIST is nearby.
         */
        SHOW_RIGGING("Dog show rigging (judge bribery)"),

        // ── Issue #1138: Northfield Iceland ────────────────────────────────────

        /**
         * Recorded when the player is caught by Kevin (ICELAND_SECURITY) attempting
         * the self-checkout scam or when the CHRISTMAS_CLUB_CASH_BOX is stolen.
         * Penalty: +8 Notoriety, WantedSystem +1 star.
         */
        SHOPLIFTING("Shoplifting (Iceland)"),

        /**
         * Recorded when the player steals the Christmas Club Cash Box from Iceland.
         * Penalty: +15 Notoriety, WantedSystem +1 star; LOCAL_SCANDAL rumour seeded.
         */
        CHRISTMAS_CLUB_THEFT("Christmas Club savings theft"),

        // ── Issue #1142: Northfield RAOB Lodge ────────────────────────────────

        /**
         * Recorded when the player is caught stealing from the LODGE_SAFE_PROP.
         * Penalty: +12 Notoriety, WantedSystem +1 star; THEFT + TRESPASSING rumour seeded.
         */
        LODGE_SAFE_THEFT("Theft from Lodge safe (RAOB)"),

        /**
         * Recorded when the player is caught trespassing in the Lodge back room
         * without PRIMO tier access, or in the Regalia Room.
         * Penalty: +5 Notoriety.
         */
        LODGE_TRESPASS("Lodge trespassing (restricted area)"),

        /**
         * Recorded when the player uses the LODGE_CHARTER_DOCUMENT to blackmail
         * a RAOB_MEMBER NPC and the NPC calls the police (20% chance).
         * Penalty: +8 Notoriety, WantedSystem +1 star; SCANDAL rumour seeded;
         * 3-day Lodge closure triggered.
         */
        LODGE_BLACKMAIL("Blackmail (Lodge charter document)"),

        /**
         * Recorded when the player bribes a RAOB_MEMBER NPC (Brian, Sandra, Reg, or Terry)
         * and the bribe is reported (20% chance on NPC calling police).
         * Penalty: +5 Notoriety.
         */
        LODGE_BRIBERY("Corruption / lodge bribery"),

        // ── Issue #1146: Mick's MOT & Tyre Centre ─────────────────────────────

        /**
         * Recorded when the WantedSystem plate-checks the player's car and finds
         * either the stolen flag set or roadworthiness < 30.
         * Penalty: +5 Notoriety; car seized if Wanted ≥ 1.
         */
        DRIVING_UNREGISTERED("Driving an unregistered/unroadworthy vehicle"),

        /**
         * Recorded when Terry (MOT_TESTER) witnesses a Dodgy MOT being obtained
         * at the MOT_RAMP_PROP (25% walk-in detection chance).
         * Penalty: Notoriety +8, WantedSystem +1 star.
         */
        FRAUDULENT_MOT("Fraudulent MOT certificate"),

        // ── Issue #1148: Northfield Council Estate Lock-Up Garages ─────────────

        /**
         * Recorded when Dave the Caretaker (DAVE_CARETAKER NPC) witnesses the player
         * breaking into a council garage (either LOCKPICK or CROWBAR method).
         * Penalty: +6 Notoriety, WantedSystem +1 star; LOCK_UP_BREAK_IN rumour seeded.
         */
        GARAGE_BREAK_IN("Breaking and entering (council lock-up garages)"),

        /**
         * Recorded when UNDERCOVER_POLICE raid Garage 3 and the player is found
         * inside with BURNER_PHONE or SCALES_PROP in proximity.
         * Penalty: +15 Notoriety, WantedSystem +2 stars; POSSESSION added concurrently.
         */
        GARAGE_DRUG_POSSESSION("Found on drug premises (council garage)"),

        // ── Issue #1165: Northfield Match Day ────────────────────────────────

        /**
         * Recorded when a HOME_FAN catches the player selling a COUNTERFEIT_TICKET
         * (30% catch chance). Penalty: Notoriety +4.
         */
        TOUT_SCAM("Ticket touting (counterfeit match ticket)"),

        // ── Issue #1167: Northfield Amateur Boxing Club ───────────────────────

        /**
         * Recorded when the player accepts a bout-fixing bribe from Wayne (FIGHT_PROMOTER)
         * and the 30% grass chance fires — Wayne tips off officials.
         * Penalty: Notoriety +5, ejection from BOXING_CLUB, banned from white-collar
         * circuit for remainder of game session.
         */
        BOUT_FIXING("Bout fixing (underground boxing circuit)"),

        /**
         * Recorded when the player enters a bout with LOADED_GLOVE equipped
         * and the 40% catch chance fires during pat-down.
         * Penalty: Notoriety +3, ejection from current event, BOXING_CLUB banned 3 days.
         */
        FIGHT_FIXING("Fight fixing (loaded glove — illegal equipment)"),

        /**
         * Issue #1171: Recorded when the player is caught EVADING TV Licence obligations
         * with a lit TV_PROP in range and the DETECTOR_VAN 25% detection chance fires.
         * Also recorded on SUMMONED status auto-referral to MagistratesCourtSystem.
         * Penalty: Notoriety +5, WantedSystem Tier 1.
         */
        TV_LICENCE_EVASION("TV licence evasion"),

        /**
         * Issue #1171: Recorded when an NPC reports a FORGED_TV_LICENCE sale to police.
         * Seeds wanted level and links to FORGED_DOCUMENT in court proceedings.
         * Penalty: Notoriety +8, WantedSystem Tier 2.
         */
        FORGED_DOCUMENT("Forged document (TV licence)"),

        /**
         * Issue #1175: Recorded when the Argos number scam is detected — player
         * pickpockets a queuing NPC's ARGOS_SLIP.
         * Penalty: Notoriety +8, WantedSystem Tier 1.
         */
        THEFT_FROM_PERSON("Theft from person"),

        /**
         * Issue #1175: Recorded when returns fraud at the Argos returns desk is
         * detected (FORGED_RECEIPT or suspicious genuine-receipt upsell caught).
         * Penalty: Notoriety +12, WantedSystem Tier 2.
         */
        FRAUD("Fraud"),

        // ── Issue #1181: Northfield Chugger Blitz ──────────────────────────────

        /**
         * Recorded when the player punches a CHUGGER NPC (charity fundraiser).
         * Penalty: +8 Notoriety, WantedSystem +1 star.
         * Text: "You assaulted a charity worker. In broad daylight."
         */
        ASSAULT("Assault (charity worker)"),

        /**
         * Recorded when a POLICE NPC witnesses the player collecting fake donations
         * while wearing CHARITY_TABARD and holding CHARITY_CLIPBOARD.
         * Penalty: +10 Notoriety, WantedSystem +1 star.
         */
        CHARITY_FRAUD("Charity donation fraud"),

        /**
         * Issue #1183: Recorded when the player enters the HWRC site with trade waste
         * (BUILDERS_RUBBLE or ASBESTOS_SHEET) without paying the 15 COIN trade waste fee
         * and escapes before COUNCIL_ENFORCEMENT can intercept them.
         * Penalty: Notoriety +4, logged to CriminalRecord.
         */
        TRADE_WASTE_EVASION("Trade waste evasion at HWRC"),

        /**
         * Issue #1183: Recorded when Dave detects a forged HARDCORE_PERMIT
         * (crafted from COUNCIL_LETTERHEAD + MARKER_PEN). 15% detection chance per check.
         * Penalty: Notoriety +6, WantedSystem +1 star, Dave calls COUNCIL_ENFORCEMENT.
         */
        FORGED_COUNCIL_DOCUMENT("Forged council document (HWRC permit)"),

        // ── Issue #1186: Northfield Probation Office ──────────────────────────

        /**
         * Recorded when the player misses 2 or more fortnightly sign-in appointments
         * at the Probation Office without a Bank Holiday exemption.
         * Triggers ProbationSystem recall warrant escalation.
         * Penalty: Notoriety +10, WantedSystem recall warrant issued.
         */
        PROBATION_BREACH("Probation order breach"),

        /**
         * Recorded when the player is detected outside the squat-home curfew zone
         * (within 20 blocks) between 21:00 and 07:00 while wearing the ANKLE_TAG.
         * Penalty: WantedSystem +1 star, Notoriety +5.
         */
        CURFEW_BREACH("Curfew breach (ankle tag)"),

        /**
         * Recorded when ProbationSystem issues a recall warrant after 2 missed sign-ins.
         * Equivalent to a new arrest warrant — police will immediately pursue.
         * Penalty: WantedSystem +2 stars, Notoriety +15.
         */
        RECALL_WARRANT("Recall to custody warrant"),

        /**
         * Recorded when the player pays the Fence (rep ≥ 40, 15 COIN) to cut the
         * ANKLE_TAG device. Removes the curfew obligation but escalates police response.
         * Penalty: WantedSystem +3 stars, Notoriety +20.
         */
        TAG_TAMPER("Electronic tag tampering");

        private final String displayName;

        CrimeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final Map<CrimeType, Integer> counts;

    // ── Issue #1002: CCTV heat level for petrol station ───────────────────────

    /**
     * CCTV heat level — incremented by 10 each time a crime is committed on the
     * petrol station forecourt while the CCTV_PROP is active. At 50+, a POLICE NPC
     * spawns 60 seconds after the event. Capped at 100.
     */
    private int cctvHeatLevel = 0;

    /** Maximum value for {@link #cctvHeatLevel}. */
    public static final int CCTV_HEAT_MAX = 100;

    /** Amount cctvHeatLevel increases per forecourt crime. */
    public static final int CCTV_HEAT_INCREMENT = 10;

    /** Threshold at which a deferred POLICE NPC spawn is triggered. */
    public static final int CCTV_HEAT_POLICE_THRESHOLD = 50;

    public CriminalRecord() {
        counts = new EnumMap<>(CrimeType.class);
        for (CrimeType type : CrimeType.values()) {
            counts.put(type, 0);
        }
    }

    /**
     * Record that one instance of the given crime was committed.
     *
     * @param type  the category of crime
     */
    public void record(CrimeType type) {
        counts.put(type, counts.get(type) + 1);
    }

    /**
     * Get the count for a specific crime category.
     *
     * @param type  the crime category
     * @return number of times that crime has been committed (>= 0)
     */
    public int getCount(CrimeType type) {
        return counts.getOrDefault(type, 0);
    }

    /**
     * Total crimes committed across all categories.
     */
    public int getTotalCrimes() {
        int total = 0;
        for (int v : counts.values()) {
            total += v;
        }
        return total;
    }

    /**
     * Read-only view of the crime counts map.
     */
    public Map<CrimeType, Integer> getCounts() {
        return Collections.unmodifiableMap(counts);
    }

    /**
     * Decrement the count for a specific crime category by 1 (minimum 0).
     * Used by the informant mechanic (grassing) to clear one witnessed crime entry.
     *
     * @param type the crime category to decrement
     */
    public void clearOne(CrimeType type) {
        int current = counts.getOrDefault(type, 0);
        if (current > 0) {
            counts.put(type, current - 1);
        }
    }

    /**
     * Reset all crime counts — called on new game start.
     */
    public void reset() {
        for (CrimeType type : CrimeType.values()) {
            counts.put(type, 0);
        }
    }
}

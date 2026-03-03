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
        VOUCHER_FRAUD("Voucher fraud"),

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

        // ── Issue #1349: Northfield RAOB Buffalo Lodge No. 1247 ──────────────

        /**
         * Recorded when the player completes the Lodge Safe Heist (KOMPROMAT_LEDGER stolen).
         * Penalty: +5 Notoriety, WantedSystem +2 stars; LODGE_BURGLARY rumour seeded.
         */
        LODGE_BURGLARY("Burglary of Lodge safe (RAOB No. 1247)"),

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
        TAG_TAMPER("Electronic tag tampering"),

        // ── Issue #1188: Northfield DWP Home Visit ─────────────────────────────

        /**
         * Recorded when Brenda (DWP_COMPLIANCE_OFFICER) finds evidence during a home
         * visit, or when the player fails a Bluff dice roll, or when the player evades
         * and is logged for non-cooperation.
         * Triggers MagistratesCourtSystem prosecution at CRIMINAL_REFERRAL tier.
         * Penalty: Notoriety +15, WantedSystem +2 stars.
         */
        BENEFIT_FRAUD("Benefit fraud (DWP compliance failure)"),

        /**
         * Recorded when the player fails to open the door within 60 seconds of
         * Brenda's knock (evasion). Adds +10 suspicion score to DWPSystem.
         * Penalty: Notoriety +5.
         */
        BENEFIT_FRAUD_EVASION("Benefit fraud evasion (non-cooperation)"),

        // ── Issue #1333: Northfield Employment System ─────────────────────────

        /**
         * Recorded when the player steals from their employer during an active shift
         * (EmploymentSystem onTheftDuringShift). Triggers instant dismissal,
         * permanent JOB_BLACKLIST entry for that employer, and Notoriety +4.
         */
        THEFT_FROM_EMPLOYER("Theft from employer during shift"),

        // ── Issue #1196: Environmental Health Officer ─────────────────────────

        /**
         * Recorded when the player attempts to bribe Janet (EnvironmentalHealthSystem)
         * and she refuses. Notoriety +3, WantedSystem +1 star.
         * Two or more BRIBERY entries escalate to CORRUPTION_OF_OFFICIAL at magistrates.
         */
        BRIBERY("Bribery of a council official"),

        /**
         * Escalated charge raised by MagistratesCourtSystem when the player
         * accumulates ≥ 2 BRIBERY entries. Penalty: 15 COIN fine or community service.
         */
        CORRUPTION_OF_OFFICIAL("Corruption of a public official"),

        /**
         * Recorded when the player assaults Janet (ENVIRONMENTAL_HEALTH_OFFICER).
         * WantedSystem +3 stars; COUNCIL_ENFORCEMENT rumour seeded.
         */
        ASSAULT_ON_OFFICIAL("Assault on a council official"),

        // ── Issue #1213: Northfield Police Station — Custody Suite ────────────

        /**
         * Minor shoplifting or low-value theft offence.
         * Can be cleared once per in-game week by CLO Sandra at Northfield Police Station
         * if the player has ≥ 20 Community Respect.
         */
        PETTY_THEFT("Petty theft"),

        /**
         * Recorded when the player fails to attend a Magistrates' Court hearing after
         * posting bail at Northfield Police Station.
         * Penalty: WantedSystem +2 stars, Notoriety +40.
         * Achievement: BAIL_JUMPER.
         */
        BAIL_JUMPING("Bail jumping (failure to surrender)"),

        // ── Issue #1224: Northfield Cybernet Internet Café ────────────────────

        /**
         * Recorded when a STOLEN_PHONE or COUNTERFEIT_NOTE listing on FlipIt is
         * detected by police passing within 10 blocks of Cybernet at Notoriety ≥ 40.
         * Penalty: WantedSystem +1 star, Notoriety +5.
         */
        COMPUTER_FRAUD("Computer fraud"),

        /**
         * Recorded when the player prints a forged document (FORGED_UC_LETTER,
         * FAKE_REFERENCE_LETTER) at the Cybernet back-room PRINTER_PROP, witnessed
         * by Asif or Hamza. Penalty: Notoriety +8 if caught printing in plain sight.
         */
        DOCUMENT_FRAUD("Document fraud"),

        /**
         * Recorded when the player runs a phishing scam session at Cybernet and
         * Notoriety ≥ 50 triggers the 10% detection risk.
         * Penalty: WantedSystem +1 star.
         */
        CYBER_FRAUD("Cyber fraud"),

        // ── Issue #1225: Northfield Fast Cash Finance — PaydayLoanSystem ─────────

        /**
         * Recorded when the player attacks the BAILIFF NPC during a debt enforcement
         * visit (PaydayLoanSystem). Triggers WantedSystem +2 stars.
         * Penalty: +10 Notoriety; Barry refuses all future loans.
         */
        ASSAULT_ON_ENFORCEMENT_AGENT("Assault on an enforcement agent (bailiff)"),

        /**
         * Recorded when the player's loan from Fast Cash Finance defaults — i.e.
         * after the 3rd missed repayment causes the debt to be sold to the Marchetti
         * Crew. Seeds LOCAL_EVENT rumour to nearby NPCs.
         * Penalty: Notoriety +5.
         */
        LOAN_DEFAULT("Loan default (Fast Cash Finance)"),

        // ── Issue #1227: Northfield Wheelwright Motors ────────────────────────

        /**
         * Recorded when the player clocks a car's odometer (with Bez and a
         * MILEAGE_CORRECTOR_PROP) and sells it to a civilian via StreetEconomySystem
         * while a TRADING_STANDARDS NPC is within 20 blocks of the sale.
         * Penalty: Notoriety +8; AchievementType.DODGY_MILEAGE awarded.
         */
        CONSUMER_FRAUD("Consumer fraud (clocked odometer)"),

        // ── Issue #1231: Northfield ASBO System ──────────────────────────────

        /**
         * Recorded each time the player enters an exclusion zone landmark while an
         * active ASBO order is in effect and does not exit within the 5-second countdown.
         * Three ASBO_BREACH entries in one order period triggers ASBO_CONTEMPT.
         * Penalty: WantedSystem +2 stars.
         */
        ASBO_BREACH("ASBO breach (exclusion zone entered)"),

        /**
         * Recorded when the player accumulates three ASBO_BREACH entries within one
         * order period (28 in-game days). Results in a 2-day custody lockout via
         * ArrestSystem. Penalty: Notoriety +15, WantedSystem +3 stars.
         */
        ASBO_CONTEMPT("ASBO contempt (three breaches)"),

        /**
         * Recorded when the player successfully impersonates an official (e.g. wearing
         * POLICE_UNIFORM or COUNCIL_JACKET) while subject to an active ASBO order,
         * and is detected within 10 blocks of a POLICE NPC.
         * Penalty: Notoriety +10, WantedSystem +1 star.
         */
        IMPERSONATION("Impersonation of a public official"),

        // ── Issue #1237: Northfield St. Aidan's Primary School ───────────────

        /**
         * Recorded when the player enters the school grounds outside gate hours
         * (08:00–16:00 Mon–Fri) and is caught by Ms. Pearson (HEADTEACHER_SECRETARY)
         * within 60 seconds. DisguiseSystem score ≥ 4 bypasses Ms. Pearson but not Derek.
         * Penalty: WantedSystem +2 stars, Notoriety +8.
         */
        SCHOOL_INTRUDER("School intruder (trespassing on school grounds)"),

        /**
         * Recorded when the player sells a forged FORGED_SCHOOL_REPORT or uses
         * SCHOOL_REPORT_FORM documents fraudulently, or sells the OFSTED_DRAFT_REPORT
         * to a newspaper journalist while a POLICE NPC is within 15 blocks.
         * Penalty: Notoriety +10, WantedSystem +1 star.
         */
        SCHOOL_FRAUD("School document fraud"),

        // ── Issue #1243: Northfield Bert's Tyres & MOT ───────────────────────

        /**
         * Recorded when the DVSA_INSPECTOR invalidates a bribe-obtained MOT certificate
         * (PASS_BRIBE outcome). Triggered in MOTSystem on raid completion if the player
         * holds an INSPECTION_STICKER linked to a PASS_BRIBE session.
         * Penalty: Notoriety +12, WantedSystem +2 stars.
         */
        VEHICLE_FRAUD("Vehicle inspection fraud (forged MOT certificate)"),

        /**
         * Recorded when the player successfully strips a CATALYTIC_CONVERTER from
         * a parked car (hold E with CROWBAR equipped for 8 seconds).
         * Penalty: Notoriety +8 unwitnessed / +16 witnessed; WantedSystem +2 if witnessed.
         */
        CATALYTIC_THEFT("Catalytic converter theft"),

        // ── Issue #1259: Northfield Pub Quiz Night ────────────────────────────

        /**
         * Recorded when Derek (QUIZ_MASTER) catches the player using a CHEAT_SHEET
         * during Quiz Night (40% catch chance on F press). The player is disqualified,
         * ejected from the quiz, and gains Notoriety +3.
         */
        CHEATING_AT_PUB_QUIZ("Cheating at pub quiz"),

        // ── Issue #1263: Northfield Illegal Street Racing ─────────────────────

        /**
         * Recorded when the player participates in an illegal street race on the
         * Northfield ring road. Also recorded if police arrive during a race the
         * player is in (+2 Wanted stars mid-race) or if the player is within 30
         * blocks of the racing cones when the police shutdown occurs (+1 Wanted star).
         * Penalty: Notoriety +5 per participation. WantedSystem stars per spec.
         */
        ILLEGAL_STREET_RACING("Illegal street racing"),

        // ── Issue #1265: Northfield Loan Shark — Big Mick's Doorstep Lending ─────

        /**
         * Recorded when the player punches a DEBT_COLLECTOR NPC sent by Big Mick.
         * Adds +2 Wanted stars and is logged by LoanSharkSystem when the collector
         * is struck.
         */
        LOAN_SHARK_ASSAULT("Loan shark collector assault"),

        // ── Issue #1269: Northfield BT Phone Box ──────────────────────────────

        /**
         * Recorded when the player smashes the phone box (8 hits).
         * Adds +1 Notoriety and +1 Wanted star via PhoneBoxSystem.
         * Council repairs the box after 2 in-game days.
         */
        PHONE_BOX_VANDALISM("Phone box vandalism"),

        // ── Issue #1271: Northfield Tattoo Parlour ────────────────────────────

        /**
         * Recorded when the HEALTH_INSPECTOR NPC arrives during an unlicensed tattoo
         * session at Daz's station and the player does not vacate within 30 seconds.
         * Penalty: WantedSystem +1 star, Notoriety +1 per session,
         * NeighbourhoodSystem −2 Vibes if caught.
         */
        UNLICENSED_TATTOOING("Unlicensed tattooing"),

        // ── Issue #1273: Northfield Fly-Tipping Ring ──────────────────────────────

        /**
         * Recorded when the player dumps a fly-tip load (presses E on wasteland/canal bank/
         * back alley while carrying a fly-tip load). Penalty: Notoriety +2;
         * spawns COUNCIL_VAN_OFFICER after 120 real-seconds.
         */
        FLY_TIPPING("Fly-tipping"),

        /**
         * Recorded when the player fly-tips within range of an active CCTV_PROP
         * (within 6 blocks) and does not steal the tape within 60 s.
         * Penalty: +1 WantedSystem star; added to council officer confrontation.
         */
        CAUGHT_ON_CAMERA("Caught on camera (fly-tip CCTV)"),

        /**
         * Recorded when the player runs more than 8 blocks from a COUNCIL_VAN_OFFICER
         * confrontation without paying the fixed penalty notice.
         * Penalty: +1 WantedSystem star; officer enters AGGRESSIVE state.
         */
        EVADING_ENFORCEMENT("Evading council enforcement"),

        // ── Issue #1276: Northfield Minicab Office — Big Terry's Cabs ─────────────

        /**
         * Recorded when the player touts for unlicensed taxi fares outside Big Terry's
         * Cabs without holding a TL_COUNCIL_PLATE. +1 WantedSystem star per 3 touts.
         */
        UNLICENSED_TOUTING("Unlicensed touting"),

        /**
         * Recorded when the player consents to a UNDERCOVER_POLICE inspection during
         * a dodgy package delivery and the package is flagged as stolen goods.
         * Penalty: +15 Notoriety, +2 WantedSystem stars.
         */
        POSSESSION_OF_STOLEN_GOODS("Possession of stolen goods"),

        // ── Issue #1278: Northfield Travelling Fairground ─────────────────────

        /**
         * Recorded when the player operates a rigged RING_TOSS_STALL_PROP and is caught
         * by a FAIRGROUND_BOSS or POLICE NPC.
         * Penalty: +8 Notoriety, RIGGED_GAME_EXPOSED rumour seeded to nearby NPCs.
         */
        RIGGED_GAME("Running a rigged game (ring toss)"),

        /**
         * Recorded when the player causes trouble at the travelling fairground:
         * punching a FAIRGROUND_WORKER, destroying a ride prop, or triggering a crowd brawl.
         * Penalty: +10 Notoriety, +1 WantedSystem star.
         */
        FAIRGROUND_TROUBLEMAKER("Fairground troublemaker"),

        // ── Issue #1280: Northfield Nightclub — The Vaults ────────────────────

        /**
         * Recorded when the player is caught fighting inside The Vaults or its vicinity.
         * Penalty: +12 Notoriety, +1 WantedSystem star; ejection from club.
         */
        NIGHTCLUB_AFFRAY("Affray (nightclub brawl)"),

        /**
         * Recorded when the player is caught with PILLS by an UNDERCOVER_OFFICER
         * in the club toilets. Penalty: +20 Notoriety, +2 WantedSystem stars.
         */
        DRUG_POSSESSION("Drug possession (nightclub)"),

        // ── Issue #1282: Northfield Day & Night Chemist ───────────────────────

        /**
         * Recorded when the player presents a forged prescription at the chemist
         * and Janet catches the fraud (40% fail rate).
         * Penalty: +15 Notoriety, +1 WantedSystem star.
         */
        CHEMIST_PRESCRIPTION_FRAUD("Chemist prescription fraud"),

        /**
         * Recorded when the player crowbars the DRUG_SAFE_PROP at the chemist.
         * Penalty: +30 Notoriety, +3 WantedSystem stars; PHARMACY_RAID rumour seeded.
         */
        PHARMACY_BURGLARY("Pharmacy burglary"),

        // ── Issue #1293: Compensation Kings — ClaimsManagementSystem ─────────

        /**
         * Recorded when the INSURANCE_INVESTIGATOR catches the player sprinting,
         * fighting, or breaking blocks within 20 blocks during the 2-hour payout
         * window following a claim filing. Cancels the pending payout.
         * Penalty: Notoriety +15, WantedSystem +1 star.
         */
        INSURANCE_FRAUD("Insurance fraud (fraudulent personal injury claim)"),

        // ── Issue #1306: Northfield Traveller Site ────────────────────────────

        /**
         * Recorded when the player attends the dog fight ring at DOG_FIGHT_RING_PROP
         * (Fri/Sat 21:00–23:00) and places a bet.
         * Penalty: Notoriety +8, WantedSystem +1 star if witnessed by police.
         * Evidence: DOG_FIGHT_LEDGER can be used to clear this from the record.
         */
        DOG_FIGHTING_ATTENDANCE("Dog fighting attendance"),

        // ── Issue #1317: Northfield Bonfire Night ─────────────────────────────

        /**
         * Recorded on the second firework offence witnessed by a POLICE NPC during
         * Bonfire Night (first offence is a warning only).
         * Penalty: Notoriety +5, WantedSystem +1 star. Clears after 48 in-game hours.
         */
        FIREWORK_OFFENCE("Firework offence"),

        // ── Issue #1319: NatWest Cashpoint — The Dodgy ATM ───────────────────

        /**
         * Recorded on a successful fraudulent withdrawal using STOLEN_PIN_NOTE +
         * VICTIM_BANK_CARD at the CASHPOINT_PROP, or when a CARD_SKIMMER_DEVICE
         * session is detected by a POLICE NPC.
         * Penalty: Notoriety +12, WantedSystem +2 stars per occurrence.
         */
        CARD_FRAUD("Card fraud"),

        /**
         * Recorded when the player is stopped by police while carrying
         * STUFFED_ENVELOPE during a Kenny money-mule run.
         * Penalty: Notoriety +15, WantedSystem +2 stars.
         */
        MONEY_LAUNDERING("Money laundering"),

        // ── Issue #1329: Northfield Traffic Warden ────────────────────────────

        /**
         * Recorded when Clive (TRAFFIC_WARDEN) issues a PENALTY_CHARGE_NOTICE to
         * the player's vehicle in the COUNCIL_CAR_PARK or surrounding streets.
         * Penalty: Notoriety +3. Cleared if PCN appeal succeeds.
         */
        PARKING_OFFENCE("Parking offence (PCN issued)"),

        /**
         * Recorded when Clive (TRAFFIC_WARDEN) detects a FORGED_PARKING_TICKET
         * on the player's vehicle.
         * Penalty: Notoriety +6, WantedSystem +1 star.
         */
        PARKING_TICKET_FRAUD("Forged parking ticket fraud"),

        // ── Issue #1335: Northfield Cycle Centre — Dave's Bikes ──────────────

        /**
         * Recorded when the player is stopped by a PCSO (police community support
         * officer) while cycling after 22:00 without BIKE_LIGHT_FRONT and
         * BIKE_LIGHT_REAR in inventory, or while riding on the pavement within
         * 10 blocks of a PUBLIC or PENSIONER NPC.
         * Penalty: Notoriety +3; PCSO issues verbal warning (1st offence) or
         * WantedSystem +1 star (2nd+ offence).
         */
        CYCLING_OFFENCE("Cycling offence (no lights / pavement riding)"),

        /**
         * Recorded when the player successfully cuts the lock off a LOCKED_BIKE_PROP
         * on the street (using CROWBAR or ANGLE_GRINDER hold-action) and takes the
         * resulting STOLEN_BIKE.
         * Penalty: Notoriety +5; WantedSystem +1 star if witnessed by any NPC.
         * Evidence: STOLEN_BIKE in inventory increases stop-and-search probability.
         */
        BIKE_THEFT("Bike theft"),

        // ── Issue #1337: Northfield Police Station — The Nick ─────────────────

        /**
         * Recorded when the player breaks into the police station custody area,
         * evidence locker, or impound garage without authorisation (e.g. by
         * breaking the CUSTODY_DOOR_PROP, BACK_WINDOW_PROP, or POLICE_GARAGE_PROP).
         * Penalty: WantedSystem +3 stars, Notoriety +20; station-wide hostile alert.
         */
        BREAKING_AND_ENTERING_POLICE_STATION("Breaking and entering (police station)"),

        /**
         * Recorded when the player successfully bribes the DESK_SERGEANT (25 COIN)
         * or CUSTODY_SERGEANT (40 COIN) at the enquiry counter.
         * Penalty: Notoriety +10; POLICE_CORRUPTION rumour seeded nearby.
         * Achievement: BENT_COPPER on first successful bribe.
         */
        BRIBERY_OF_OFFICER("Bribery of a police officer"),

        /**
         * Recorded when the player attempts to recover an impounded vehicle from
         * POLICE_GARAGE_PROP by breaking the garage door at night without paying
         * the 20 COIN + DRIVING_LICENCE fee.
         * Penalty: WantedSystem +2 stars, Notoriety +8.
         * Achievement: GOT_ME_MOTOR_BACK on successful night-time recovery.
         */
        VEHICLE_RECOVERY_OFFENCE("Vehicle recovery offence (impound garage break-in)"),

        // ── Issue #1339: Council Enforcement Day ──────────────────────────────

        /**
         * Recorded when DVLA_OFFICER Karen catches the player driving without a
         * DRIVING_LICENCE during a Council Enforcement Day sweep. Triggers vehicle
         * tow (car removed from world until reclaimed), Notoriety +10, WantedSystem +2.
         * Penalty multiplier ×1.5 applies on Enforcement Day.
         */
        NO_INSURANCE_DRIVING("No insurance / unlicensed driving (DVLA enforcement)"),

        // ── Issue #1341: Northfield Residents' Association Meeting ─────────────

        /**
         * Recorded when the player generates a noise complaint that reaches the
         * Residents' Association agenda — typically through repeated late-night
         * disturbances, block-breaking near residential buildings, or WarmthSystem
         * fire-making near occupied properties.
         * Three or more NOISE_COMPLAINT entries cause the complaint to appear on the
         * meeting agenda (Agenda slot 3). Dismissible via NOISE_ABATEMENT_LETTER
         * or by bribing Kevin (10 COIN). If unaddressed, triggers council enforcement
         * +2 Notoriety the following day.
         * Penalty: Notoriety +2 per entry.
         */
        NOISE_COMPLAINT("Noise complaint (residents' association)"),

        // ── Issue #1347: Northfield Remembrance Sunday ────────────────────────

        /**
         * Recorded when the player steals the WREATH_PROP from the war memorial
         * (STATUE prop in the park) after 11:30 on Remembrance Sunday.
         * Penalty: Notoriety +10, WantedSystem +2 stars.
         * Fenceable at PawnShop for 8–12 COIN.
         * VETERAN NPCs turn hostile and pursue the player.
         */
        MEMORIAL_VANDALISM("Memorial vandalism (Remembrance wreath theft)"),

        /**
         * Recorded when the player moves, attacks, or breaks a block during the
         * two-minute silence (11:00–11:02 game time) on Remembrance Sunday.
         * Penalty: Notoriety +5, WantedSystem +1 star.
         * Triggers outrage speech bubbles from nearby PUBLIC NPCs.
         * NewspaperSystem headline the next day: 'Local yob disrupts Remembrance ceremony'.
         */
        SILENCE_BREACH("Silence breach (Remembrance Sunday two-minute silence)"),

        // ── Issue #1351: Northfield QuickFix Loans ────────────────────────────

        /**
         * Recorded when Darren (LOAN_SHARK_CLERK) catches the player using a
         * {@link ragamuffin.building.Material#FORGED_ID} at the QuickFix Loans counter
         * (30% detection chance). Penalty: Notoriety +10, WantedSystem +1 star.
         */
        LOAN_FRAUD("Loan fraud (forged ID at QuickFix Loans)"),

        /**
         * Recorded when the player assaults Terry (BAILIFF_NPC) during a doorstep visit.
         * Penalty: WantedSystem +1 star, debt written off.
         * Cross-references CRIMINAL_DAMAGE if player also damages property.
         */
        BAILIFF_ASSAULT("Assault of a court bailiff"),

        // ── Issue #1353: Northfield Amateur Dramatics Society ─────────────────

        /**
         * Recorded when the player steals STAGE_COSTUME items from the
         * COSTUME_CUPBOARD_PROP at the community centre. Penalty: Notoriety +3,
         * WantedSystem +1 star.
         */
        COSTUME_THEFT("Costume theft (NAODS community centre)"),

        /**
         * Recorded when the player presents a FORGED_TICKET at the TICKET_BOOTH_PROP
         * and is caught (30% chance). Penalty: Notoriety +2, WantedSystem +1 star.
         */
        TICKET_FRAUD("Ticket fraud (forged NAODS opening night ticket)"),

        /**
         * Recorded when the player executes any of Mario's sabotage options on opening
         * night: cutting power at FUSE_BOX_PROP, swapping PROP_GUN with AIRGUN, or
         * stealing TICKET_CASH_BOX_PROP. Penalty: Notoriety +5, WantedSystem +1 star.
         * Triggers NAODS_DRAMA_DISASTER rumour.
         */
        PRODUCTION_SABOTAGE("Production sabotage (NAODS opening night)"),

        // ── Issue #1355: Northfield NHS Walk-In Centre ────────────────────────

        /**
         * Recorded when the player breaks into the medicine room and loots the
         * {@link ragamuffin.world.PropType#CONTROLLED_DRUGS_SAFE_PROP} using a CROWBAR.
         * Penalty: +12 Notoriety, +2 WantedSystem stars.
         * Also triggers PHARMACY_RAID rumour seeded within 50 blocks.
         */
        MEDICINE_THEFT("Medicine theft (NHS Walk-In Centre drug safe)"),

        /**
         * Recorded when the player fences 3+ units of controlled drugs (TRAMADOL or
         * DIAZEPAM) obtained from the CONTROLLED_DRUGS_SAFE_PROP in a single session.
         * Penalty: +8 Notoriety, +1 WantedSystem star.
         */
        CONTROLLED_DRUG_TRAFFICKING("Controlled drug trafficking"),

        /**
         * Recorded when the player attacks a PARAMEDIC NPC during an active callout.
         * Penalty: WantedSystem minimum Tier 4, immediate police alert.
         */
        ASSAULTING_NHS_STAFF("Assaulting NHS staff (paramedic)"),

        // ── Issue #1359: Northfield HMRC Tax Investigation ────────────────────

        /**
         * Recorded when the player's total untaxed earnings reach or exceed 150 COIN
         * and Sandra (HMRC_INSPECTOR) serves a TAX_DEMAND_LETTER that is subsequently
         * ignored for 5+ in-game days without payment, appeal, or bribe.
         * Penalty: Notoriety +8, WantedSystem +1 star; debt carried forward.
         */
        TAX_EVASION("Tax evasion (HMRC cash-in-hand investigation)"),

        /**
         * Recorded when the player offers a CASH_BRIBE_ENVELOPE to Sandra (HMRC_INSPECTOR)
         * and the bribe fails (40% failure chance). Triggers WantedSystem +2 stars,
         * Notoriety +10, and HMRC_TIPPED_OFF rumour.
         */
        BRIBERY_OF_PUBLIC_OFFICIAL("Bribery of a public official (HMRC inspector)"),

        /**
         * Recorded when Trading Standards Officer catches the player selling 3+
         * stolen goods at the Sunday car boot sale. Triggers Notoriety +10,
         * NewspaperSystem headline, and STOLEN_GOODS_MARKET rumour.
         */
        TRADING_STANDARDS_BUST("Selling stolen goods at a car boot sale (Trading Standards bust)"),

        // ── Issue #1373: Northfield Local Council Elections ───────────────────

        /**
         * Recorded when the player fills in a POSTAL_VOTE_BUNDLE and the 15% detection
         * check fires (5% with SLEIGHT_OF_HAND ≥ Journeyman). Penalty: Notoriety +15,
         * WantedSystem +2 stars; ELECTION_FRAUD rumour seeded; candidate loses 20 votes.
         */
        ELECTORAL_FRAUD("Electoral fraud (postal vote fraud)"),

        /**
         * Recorded once per fraudulently completed ballot paper in a POSTAL_VOTE_BUNDLE.
         * Each bundle yields 1–5 ballots. Stacks with ELECTORAL_FRAUD.
         */
        POSTAL_VOTE_FRAUDULENT("Fraudulent postal vote (individual ballot)"),

        /**
         * Recorded when the player stands within 3 blocks of POLLING_STATION_PROP while
         * wearing ROSETTE_ITEM and is spotted by POLLING_OFFICER_NPC (Barry).
         * Penalty: Notoriety +8; Barry calls police (WantedSystem +1 star).
         */
        BREACH_OF_POLLING_STATION_EXCLUSION("Breach of polling station exclusion zone"),

        // ── Issue #1386: Northfield St George's Day ───────────────────────────

        /**
         * Recorded when the player steals ST_GEORGE_FLAG_PROP from the Wetherspoons bar
         * by climbing BAR_STOOL_PROP and pressing E on the flag.
         * Penalty: Notoriety +3, ejection from pub, 3-day Wetherspoons ban.
         */
        FLAG_THEFT("Theft of a licensed premises fixture (St George flag)"),

        /**
         * Recorded when the player takes ROOF_FLAG_PROP from the Wetherspoons rooftop
         * via DRAINPIPE_PROP climb. Requires CCTV disabled.
         * Penalty: Notoriety +5, WantedSystem +1 star.
         */
        ROOFTOP_FLAG_THEFT("Rooftop theft (flag from licensed premises roof)"),

        /**
         * Recorded when the player steals MORRIS_STICK_PROP from a MORRIS_DANCER NPC.
         * Penalty: Notoriety +1, WantedSystem +1 star; all 6 dancers pursue.
         */
        MORRIS_STICK_THEFT("Theft of a Morris dancing prop"),

        // ── Issue #1390: Northfield Annual Conker Championship ────────────────

        /**
         * Recorded when organiser Derek catches the player competing with a
         * HARDENED_CONKER during the Northfield Annual Conker Championship.
         * ConkerSystem spot-check: 20% chance per tick. DisguiseSystem blocks check.
         * Penalty: disqualification, Notoriety +2.
         */
        CHEATING_AT_CONKERS("Competing with a hardened/chemically-treated conker"),

        // ── Issue #1394: England Match Night ─────────────────────────────────

        /**
         * Recorded when the player sabotages the PUB_TV_PROP at Wetherspoons
         * during the England match screening. Dave witnesses on 50% chance.
         * Penalty: Notoriety +10, WantedSystem +2 (if witnessed).
         */
        TV_SABOTAGE("Sabotage of licensed premises TV equipment"),

        /**
         * Recorded when the player steals items from the TROPHY_CABINET_PROP
         * inside Wetherspoons during the England match.
         * Penalty: Notoriety +3 per item (up to +8 for full heist), Wanted +1 per item.
         */
        TROPHY_THEFT("Theft from a licensed premises trophy cabinet"),

        /**
         * Recorded when the player uses a MATCH_FIX_ITEM to force the England
         * match result. Requires Marchetti Crew Respect ≥ 60.
         * Penalty: associated with MATCH_FIXING crime in CriminalRecord.
         */
        ENGLAND_MATCH_FIXING("Fixing of an England international match result"),

        // ── Issue #1398: Northfield Window Cleaner ────────────────────────────────

        /**
         * Recorded when the player climbs Terry's LADDER_PROP to enter an upstairs window.
         * Penalty: Notoriety +6, WantedSystem +1 star.
         */
        LADDER_BURGLARY("Burglary via window cleaner's ladder"),

        // ── Issue #1406: Northfield Dodgy Roofer ──────────────────────────────

        /**
         * VEHICLE_BREAK_IN — Recorded when the player breaks into Kenny's ROOFER_VAN_PROP
         * using a CROWBAR (3-second hold). Penalty: Notoriety +6, WantedSystem +1 star.
         */
        VEHICLE_BREAK_IN("Breaking into a vehicle (roofer's van)"),

        // ── Issue #1412: Northfield Catalogue Man ─────────────────────────────

        /**
         * Recorded when the player steals Barry's catalogue bag (stealBag() SUCCESS path).
         * Penalty: Notoriety +4, WantedSystem +1 star; BARRY_SUSPICIOUS rumour seeded.
         * Three recorded days awards BARRY_BANDIT achievement.
         */
        CATALOGUE_THEFT("Catalogue bag theft (Barry's round)"),

        /**
         * Recorded on the second blackmailBarry() call (EXTORTION_TRIGGERED result).
         * Penalty: WantedSystem +1 star, Notoriety +6.
         * Escalates to CORRUPTION charge at MagistratesCourtSystem if a third attempt is made.
         */
        EXTORTION("Extortion (catalogue man blackmail)"),

        /**
         * Recorded when the player is caught running a rival catalogue round by Trading Standards
         * during the monthly check (doRivalCatalogueSale catch path).
         * Penalty: Notoriety +8, WantedSystem +1 star; COUNTERFEIT_FINE deducted from COIN.
         * COUNTERFEIT_CAUGHT rumour seeded to nearby NPCs.
         */
        COUNTERFEIT_GOODS_SELLING("Selling counterfeit catalogue goods"),

        // ── Issue #1416: Northfield Mobile Speed Camera Van ───────────────────

        /**
         * CAMERA_TAMPERING — Recorded when the player interferes with the speed camera or van:
         * <ul>
         *   <li>Stealing the SD card (hold-E heist) — Notoriety +6.</li>
         *   <li>Spraying paint on the camera lens (GraffitiSystem) — Notoriety +4.</li>
         *   <li>Slashing the van tyres with CROWBAR — Notoriety +6, WantedSystem +1.</li>
         *   <li>Burning the van with LIGHTER — Notoriety +20, WantedSystem +3.</li>
         * </ul>
         * Having a CAMERA_TAMPERING record blocks the Legitimate Operator Licence application.
         */
        CAMERA_TAMPERING("Interference with a speed enforcement device"),

        // ── Issue #1418: Northfield QuickFix Loans (PaydayLoanSystem) ─────────

        /**
         * Recorded when the player defaults on a QuickFix Loans debt (Day 3 overdue).
         * Triggers WantedSystem +1 star, permanent ban from QuickFix Loans, and
         * DEBT_SPIRAL achievement. Penalty: +5 Notoriety.
         */
        LOAN_DEFAULTED("Loan default (QuickFix Loans)"),

        /**
         * Recorded when the player uses a FAKE_ID to take out a loan while banned
         * from QuickFix Loans. 15% chance Darren recognises the player, triggering
         * WantedSystem +1. Penalty: +5 Notoriety on detection.
         */
        IDENTITY_FRAUD("Identity fraud (QuickFix Loans)"),

        // ── Issue #1420: Northfield Post Office Horizon Scandal ───────────────

        /**
         * AUDIT_OBSTRUCTION — recorded if the player assaults Pete (IT_CONTRACTOR NPC) or
         * steals the SHORTFALL_LETTER_PROP from the counter before the day-17 tribunal.
         * Penalty: Notoriety +5, WantedSystem +1.
         * Consequence: tribunal adjudicator applies 10% conviction-probability bonus against player.
         */
        AUDIT_OBSTRUCTION("Obstruction of a Post Office audit"),

        /**
         * POST_OFFICE_SAFE_ROBBERY — recorded on successful safe crack during the Horizon audit
         * window (12:30–14:00, days 14–17). Requires CROWBAR or LOCKPICK; 6 hits to open safe.
         * Yields 25–50 COIN and STAMPS_BUNDLE. Penalty: Notoriety +10, WantedSystem +1.
         */
        POST_OFFICE_SAFE_ROBBERY("Robbery of Post Office safe during Horizon audit"),

        // ── Issue #1451: Northfield Balti House ───────────────────────────────

        /**
         * RESTAURANT_THEFT — recorded when the player steals the BALTI_CATERING_TIN
         * from the Raj Mahal kitchen while Bashir has line-of-sight.
         * Penalty: Notoriety +3, chase triggered, police called after 10 seconds.
         */
        RESTAURANT_THEFT("Theft from a restaurant kitchen"),

        // ── Issue #1459: Northfield Church Hall Jumble Sale ───────────────────

        /**
         * JUMBLE_SHOPLIFTING — recorded when caught pocketing an item at Dot's jumble sale.
         * Penalty: Notoriety +4, ejected from hall; JUMBLE_THIEF rumour seeded.
         */
        JUMBLE_SHOPLIFTING("Shoplifting (jumble sale)"),

        /**
         * JUMBLE_BREAKING_AND_ENTERING — recorded when forcing the back window of the
         * Community Centre during the pre-opening window (08:45–09:00).
         * Penalty: Notoriety +5; WantedSystem +1 if CCTV active.
         */
        JUMBLE_BREAKING_AND_ENTERING("Breaking and entering (community centre)"),

        /**
         * JUMBLE_HANDLING_STOLEN_GOODS — recorded when a volunteer recognises stolen
         * items on the player's rented stall (Notoriety ≥ 40, 20% scrutiny chance).
         * Penalty: WantedSystem +1.
         */
        JUMBLE_HANDLING_STOLEN_GOODS("Handling stolen goods (jumble sale stall)"),

        // ── Issue #1461: Northfield Street Preacher ───────────────────────────

        /**
         * BREACH_OF_PEACE — recorded when the player heckles Brother Gary and noise
         * level reaches ≥ 7, summoning a PCSO. Penalty: WantedSystem +1.
         */
        BREACH_OF_PEACE("Breach of the peace (heckling)"),

        // ── Issue #1471: Northfield Closing-Down Sale — Dave's Everything Must Go ──

        /**
         * Recorded when the player loses an argument with Gary (COUNCIL_ENFORCEMENT_OFFICER)
         * while doing shill work for Dave's closing-down sale (ClosingDownSaleSystem).
         * Also recorded on a failed GARY_ARGUE outcome during the Shill Shift.
         * Penalty: WantedSystem +1 star, Notoriety +2, shill shift ends immediately.
         */
        MISLEADING_ADVERTISING("Misleading advertising (shill for fake closing-down sale)"),

        /**
         * Recorded when the player submits a Trading Standards tip-off against Dave's
         * closing-down sale having previously worked as a shill for him.
         * The TS officer notes the conflict of interest and issues a mild caution.
         * Penalty: no additional Notoriety; reward is halved to
         * {@link ragamuffin.core.ClosingDownSaleSystem#TS_CONFLICT_REWARD_COINS}.
         */
        CAUTION("Caution (conflict of interest — TS tip-off after shill work)"),

        // ── Issue #1479: Northfield Public Defibrillator ─────────────────────────

        /**
         * CABINET_THEFT — recorded when the player takes COPPER_CABLE from the
         * public defibrillator cabinet. Notoriety +5.
         */
        CABINET_THEFT("Theft from public defibrillator cabinet"),

        /**
         * FRAUD — recorded when the player abandons a CPR training session mid-course,
         * triggering student refund demands. Each abandonment adds one count.
         */
        DEFIB_CPR_FRAUD("Fraud (abandoned CPR training session, students demand refund)"),

        // ── Issue #1483: Northfield Crown Green Bowls Club ─────────────────────

        /**
         * CHEATING_AT_BOWLS — recorded when the player is caught swapping Arthur's
         * bowl with a WEIGHTED_BOWL during the Saturday Grudge Match, or when Reg
         * catches the player holding a STOLEN_BOWLS_SET on the green.
         * Penalty: Notoriety +6, banned from the green for 5 days.
         */
        CHEATING_AT_BOWLS("Cheating at crown green bowls (weighted bowl / stolen set)"),

        // ── Issue #1485: Northfield Milk Float ────────────────────────────────

        /**
         * MILK_THEFT — recorded when the player steals a MILK_BOTTLE from a
         * doorstep loot window, lifts a full MILK_CRATE_STOLEN from the float,
         * or loots the walk-in fridge at the MILK_DEPOT.
         * Penalty: Notoriety +1 per bottle if witnessed; +4 if full crate caught.
         */
        MILK_THEFT("Theft of milk (doorstep / float / depot)"),

        /**
         * VEHICLE_THEFT — recorded when the player boards and drives away the
         * milk float from Dave's round. Notoriety +8, WantedSystem +1.
         */
        VEHICLE_THEFT("Vehicle theft (milk float)");

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

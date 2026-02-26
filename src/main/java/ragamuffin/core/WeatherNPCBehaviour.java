package ragamuffin.core;

import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import java.util.List;
import java.util.Random;

/**
 * Applies weather-driven behaviour modifications to NPCs (Issue #698).
 *
 * <p>Rules:
 * <ul>
 *   <li>Pedestrians shelter under awnings in RAIN or DRIZZLE (state → SHELTERING)</li>
 *   <li>All non-hostile NPCs flee indoors during THUNDERSTORM (state → SHELTERING)</li>
 *   <li>YOUTH_GANG NPCs become more aggressive in storm/cold/frost weather</li>
 *   <li>POLICE patrol thinning in THUNDERSTORM and FROST (some stop patrolling)</li>
 *   <li>HEATWAVE drives civilians toward pub/pond locations (handled via daily routine)</li>
 * </ul>
 */
public class WeatherNPCBehaviour {

    private final Random random;

    public WeatherNPCBehaviour(Random random) {
        this.random = random;
    }

    /**
     * Apply weather-driven state changes to all NPCs.
     *
     * @param npcs    all living NPCs
     * @param weather the current weather
     */
    public void applyWeatherBehaviour(List<NPC> npcs, Weather weather) {
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            applyToNPC(npc, weather);
        }
    }

    private void applyToNPC(NPC npc, Weather weather) {
        NPCType type = npc.getType();
        NPCState state = npc.getState();

        // Don't override combat or arrest states
        if (state == NPCState.ATTACKING || state == NPCState.ARRESTING ||
                state == NPCState.AGGRESSIVE || state == NPCState.KNOCKED_OUT) {
            return;
        }

        if (weather.causesEvacuation()) {
            // Thunderstorm: all non-hostile NPCs flee indoors
            if (!type.isHostile()) {
                npc.setState(NPCState.SHELTERING);
            }
            // Youth gang becomes more aggressive during thunderstorm
            if (type == NPCType.YOUTH_GANG && state != NPCState.AGGRESSIVE) {
                if (random.nextFloat() < 0.3f) {
                    npc.setState(NPCState.AGGRESSIVE);
                }
            }
            // Police patrols thin out in thunderstorm
            if (type == NPCType.POLICE && state == NPCState.PATROLLING) {
                if (random.nextFloat() < 0.5f) {
                    npc.setState(NPCState.SHELTERING);
                }
            }
        } else if (weather.isRaining()) {
            // Rain/drizzle: pedestrians shelter under awnings
            if (isPedestrian(type) && isOutdoorState(state)) {
                if (random.nextFloat() < 0.4f) {
                    npc.setState(NPCState.SHELTERING);
                }
            }
        } else if (weather.causesFrost() || weather == Weather.COLD_SNAP) {
            // Cold weather: youth gang more aggressive
            if (type == NPCType.YOUTH_GANG && isOutdoorState(state)) {
                if (random.nextFloat() < 0.2f) {
                    npc.setState(NPCState.AGGRESSIVE);
                }
            }
            // Police patrols thin out in frost
            if (type == NPCType.POLICE && state == NPCState.PATROLLING) {
                if (random.nextFloat() < 0.3f) {
                    npc.setState(NPCState.SHELTERING);
                }
            }
        } else if (weather.isHeatwave()) {
            // Heatwave: civilians tend to head to pub/pond (handled by daily routine update)
            if (state == NPCState.SHELTERING) {
                // Resume normal activities when it's just hot
                npc.setState(NPCState.WANDERING);
            }
        } else {
            // Clear/overcast/fog: resume wandering if sheltering from old weather
            if (state == NPCState.SHELTERING) {
                npc.setState(NPCState.WANDERING);
            }
        }
    }

    /**
     * Whether an NPC type is a civilian pedestrian that seeks shelter.
     */
    private boolean isPedestrian(NPCType type) {
        switch (type) {
            case PUBLIC:
            case PENSIONER:
            case SCHOOL_KID:
            case POSTMAN:
            case JOGGER:
            case DRUNK:
            case BUSKER:
            case LOLLIPOP_LADY:
            case STREET_PREACHER:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether the NPC is in an outdoor wandering/idle state (eligible for sheltering).
     */
    private boolean isOutdoorState(NPCState state) {
        switch (state) {
            case IDLE:
            case WANDERING:
            case GOING_TO_WORK:
            case GOING_HOME:
            case PATROLLING:
            case STARING:
            case WAVING:
            case POINTING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the effective police line-of-sight range, halved during FOG.
     *
     * @param baseRange the normal LoS range in blocks
     * @param weather   current weather
     * @return effective LoS range
     */
    public static float getEffectivePoliceLoS(float baseRange, Weather weather) {
        return weather.halvesPoliceLoS() ? baseRange * 0.5f : baseRange;
    }

    /**
     * Calculate the probability of a frost slip event per second.
     * Black ice on ROAD blocks causes occasional player slips during FROST weather.
     *
     * @param weather current weather
     * @param onRoad  whether the player is standing on a ROAD or PAVEMENT block
     * @return probability per second of a slip event (0 = no slip)
     */
    public static float getFrostSlipProbabilityPerSecond(Weather weather, boolean onRoad) {
        if (!weather.causesFrost() || !onRoad) {
            return 0.0f;
        }
        return 0.05f; // 5% per second = roughly once every 20 seconds on ice
    }
}

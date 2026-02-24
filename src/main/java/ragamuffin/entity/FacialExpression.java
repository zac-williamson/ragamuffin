package ragamuffin.entity;

/**
 * Facial expressions for NPC characters, driven by the NPC's current state.
 *
 * <ul>
 *   <li>NEUTRAL   — default resting face (idle, wandering, daily routines)</li>
 *   <li>ANGRY     — furrowed brows, tighter mouth (aggressive, arresting, demolishing)</li>
 *   <li>SCARED    — wide eyes, open mouth (fleeing)</li>
 *   <li>HAPPY     — wider mouth curve (busking, at pub)</li>
 *   <li>SURPRISED — raised brows, round mouth (staring, photographing)</li>
 *   <li>DISGUSTED — narrowed eyes, downturned mouth (complaining, stealing)</li>
 * </ul>
 */
public enum FacialExpression {
    NEUTRAL,
    ANGRY,
    SCARED,
    HAPPY,
    SURPRISED,
    DISGUSTED
}

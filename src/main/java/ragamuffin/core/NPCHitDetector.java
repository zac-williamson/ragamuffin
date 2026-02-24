package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.BlockBreaker;
import ragamuffin.entity.NPC;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

import java.util.List;

/**
 * Determines which NPC (if any) is within punch reach of the player's camera.
 *
 * <p>Extracted from {@link RagamuffinGame#findNPCInReach} so that the cone-check
 * logic can be unit-tested without starting the full game.
 *
 * <p>Fix #242: Widened the dot-product threshold from 0.985f (~10° half-angle) to
 * 0.9f (~26° half-angle) so that punches land when the player's reticule visually
 * overlaps an NPC's 0.6×1.8×0.6 AABB — not just when the ray passes through the
 * single chest-centre point.
 */
public class NPCHitDetector {

    /**
     * Dot-product threshold for the punch-hit cone.
     *
     * <p>cos(26°) ≈ 0.9 — wide enough that punches succeed when the player is
     * clearly facing an NPC without accidentally hitting NPCs far to the side.
     */
    static final float HIT_CONE_DOT = 0.9f;

    private NPCHitDetector() {}

    /**
     * Return {@code true} if {@code direction} points within the hit cone toward
     * the NPC centre at ({@code dx}, {@code dy}, {@code dz}) with the given
     * pre-computed inverse distance.
     *
     * @param dx      X component of the vector from camera to NPC centre
     * @param dy      Y component of the vector from camera to NPC centre
     * @param dz      Z component of the vector from camera to NPC centre
     * @param invDist 1 / distance (must be positive and finite)
     * @param direction normalised camera look direction
     * @return {@code true} if the dot product exceeds {@link #HIT_CONE_DOT}
     */
    public static boolean isInHitCone(float dx, float dy, float dz,
                                       float invDist, Vector3 direction) {
        float dot = (dx * invDist) * direction.x
                  + (dy * invDist) * direction.y
                  + (dz * invDist) * direction.z;
        return dot > HIT_CONE_DOT;
    }

    /**
     * Find the closest alive NPC within {@code reach} of {@code cameraPos} that
     * lies inside the hit cone and is not occluded by a solid block.
     *
     * @param cameraPos  camera / eye position
     * @param direction  normalised camera look direction
     * @param reach      maximum punch distance in blocks
     * @param npcs       live NPC list (from {@link ragamuffin.ai.NPCManager#getNPCs()})
     * @param blockBreaker used to test block occlusion along the ray
     * @param world       the voxel world
     * @return the closest eligible NPC, or {@code null} if none
     */
    public static NPC findNPCInReach(Vector3 cameraPos, Vector3 direction, float reach,
                                      List<NPC> npcs, BlockBreaker blockBreaker, World world) {
        NPC closestNPC = null;
        float closestDistance = reach;

        // Find the nearest solid block along the ray — cannot punch NPCs behind walls.
        RaycastResult blockHit = blockBreaker.getTargetBlock(world, cameraPos, direction, reach);
        float blockDistance = (blockHit != null)
                ? cameraPos.dst(blockHit.getBlockX() + 0.5f,
                                blockHit.getBlockY() + 0.5f,
                                blockHit.getBlockZ() + 0.5f)
                : reach;

        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;

            float npcCentreY = npc.getPosition().y + ragamuffin.entity.NPC.HEIGHT * 0.5f;
            float dx = npc.getPosition().x - cameraPos.x;
            float dy = npcCentreY - cameraPos.y;
            float dz = npc.getPosition().z - cameraPos.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance > reach || distance > blockDistance) continue;

            float invDist = 1f / distance;
            if (isInHitCone(dx, dy, dz, invDist, direction) && distance < closestDistance) {
                closestNPC = npc;
                closestDistance = distance;
            }
        }

        return closestNPC;
    }
}

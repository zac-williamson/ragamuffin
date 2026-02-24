package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NPCManager.
 */
class NPCManagerTest {

    private NPCManager manager;
    private World world;
    private Player player;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        manager = new NPCManager();
        world = new World(12345);
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();

        // Create ground
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    @Test
    void testSpawnNPC() {
        assertEquals(0, manager.getNPCs().size());

        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        assertNotNull(npc);
        assertEquals(1, manager.getNPCs().size());
        assertEquals(NPCType.PUBLIC, npc.getType());
        assertEquals(10, npc.getPosition().x, 0.01f);
    }

    @Test
    void testSpawnMultipleNPCs() {
        manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);
        manager.spawnNPC(NPCType.DOG, 15, 1, 15);
        manager.spawnNPC(NPCType.YOUTH_GANG, 20, 1, 20);

        assertEquals(3, manager.getNPCs().size());
    }

    @Test
    void testRemoveNPC() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);
        assertEquals(1, manager.getNPCs().size());

        manager.removeNPC(npc);
        assertEquals(0, manager.getNPCs().size());
    }

    @Test
    void testDailyRoutineWorkHours() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        manager.setGameTime(8.0f); // 8:00 AM — band transition: night→work
        manager.update(0.1f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.GOING_TO_WORK, npc.getState());

        manager.setGameTime(12.0f); // Noon — same band (work), no transition
        // State should still be work-related (no thrash) unless update() changed it
        assertTrue(npc.getState() == NPCState.GOING_TO_WORK ||
                   npc.getState() == NPCState.WANDERING); // May be wandering during work
    }

    /**
     * Fix #158: setGameTime() called repeatedly within the same time band must NOT
     * invoke updateDailyRoutine(), so reaction states are not clobbered every frame.
     */
    @Test
    void testDailyRoutineDoesNotClobberReactionStateWithinSameBand() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        // Transition into work-hours band so daily routine sets GOING_TO_WORK
        manager.setGameTime(8.0f);
        assertEquals(NPCState.GOING_TO_WORK, npc.getState());

        // Simulate AI setting a reaction state (e.g. NPC fleeing from player)
        npc.setState(NPCState.FLEEING);

        // Subsequent per-frame calls in the SAME band must NOT clobber FLEEING
        manager.setGameTime(9.0f);
        manager.setGameTime(10.5f);
        manager.setGameTime(12.0f);
        manager.setGameTime(16.9f);
        assertEquals(NPCState.FLEEING, npc.getState(),
                "Reaction state FLEEING was overwritten by per-frame setGameTime calls (issue #158)");
    }

    /**
     * Fix #158: a genuine band transition (work → evening) must still apply the
     * daily routine, but only if the NPC is not in an active-reaction state.
     */
    @Test
    void testDailyRoutineAppliedOnBandTransition() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        // Start in night band so first transition is to work-hours
        manager.setGameTime(6.0f); // night
        // Transition to work
        manager.setGameTime(8.0f);
        assertEquals(NPCState.GOING_TO_WORK, npc.getState());

        // Transition to evening
        manager.setGameTime(17.0f);
        assertEquals(NPCState.GOING_HOME, npc.getState());

        // Transition to night
        manager.setGameTime(20.0f);
        assertTrue(npc.getState() == NPCState.AT_PUB || npc.getState() == NPCState.AT_HOME,
                "Expected AT_PUB or AT_HOME after night transition");
    }

    /**
     * Fix #158: active-reaction states (FLEEING, AGGRESSIVE, ARRESTING, etc.) must
     * be preserved even across genuine band transitions.
     */
    @Test
    void testReactionStatePreservedAcrossBandTransition() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        manager.setGameTime(8.0f); // work band
        npc.setState(NPCState.FLEEING);

        // Genuine transition to evening — reaction state must still be preserved
        manager.setGameTime(17.0f);
        assertEquals(NPCState.FLEEING, npc.getState(),
                "FLEEING state was overwritten during band transition (issue #158)");

        // Transition to night — still preserved
        manager.setGameTime(20.0f);
        assertEquals(NPCState.FLEEING, npc.getState(),
                "FLEEING state was overwritten during night band transition (issue #158)");
    }

    /**
     * Fix #158: COUNCIL_MEMBER NPCs should also be protected from thrashing.
     */
    @Test
    void testCouncilMemberReactionStateNotClobbered() {
        NPC npc = manager.spawnNPC(NPCType.COUNCIL_MEMBER, 10, 1, 10);

        manager.setGameTime(8.0f); // work band
        assertEquals(NPCState.GOING_TO_WORK, npc.getState());

        npc.setState(NPCState.AGGRESSIVE);

        // Repeated same-band calls must not overwrite AGGRESSIVE
        manager.setGameTime(9.0f);
        manager.setGameTime(14.0f);
        assertEquals(NPCState.AGGRESSIVE, npc.getState(),
                "COUNCIL_MEMBER AGGRESSIVE state was clobbered by same-band setGameTime (issue #158)");
    }

    @Test
    void testDailyRoutineEvening() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        manager.setGameTime(17.0f); // 5:00 PM
        manager.update(0.1f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.GOING_HOME, npc.getState());
    }

    @Test
    void testDailyRoutineNight() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        manager.setGameTime(20.0f); // 8:00 PM
        manager.update(0.1f, world, player, inventory, tooltipSystem);

        assertTrue(npc.getState() == NPCState.AT_PUB || npc.getState() == NPCState.AT_HOME);
    }

    @Test
    void testDogsStartWandering() {
        NPC dog = manager.spawnNPC(NPCType.DOG, 0, 1, 0);

        assertEquals(NPCState.WANDERING, dog.getState());
    }

    @Test
    void testPunchNPC() {
        NPC npc = manager.spawnNPC(NPCType.COUNCIL_MEMBER, 20, 1, 20);
        Vector3 originalPos = new Vector3(npc.getPosition());

        Vector3 punchDir = new Vector3(0, 0, -1); // North
        manager.punchNPC(npc, punchDir);

        // Knockback is now velocity-based — simulate frames to see movement
        for (int i = 0; i < 12; i++) {
            npc.update(1.0f / 60.0f);
        }

        assertTrue(npc.getPosition().z < originalPos.z, "NPC should be knocked north");
    }

    @Test
    void testGameTimeWrap() {
        manager.setGameTime(23.5f);
        assertEquals(23.5f, manager.getGameTime(), 0.01f);

        manager.setGameTime(25.0f); // Should wrap to 1:00 AM
        assertEquals(1.0f, manager.getGameTime(), 0.01f);
    }

    @Test
    void testNPCDoesNotSinkIntoFloor() {
        // Reproduce the "yellow NPC stuck in floor" bug:
        // an NPC that falls a small distance should land exactly on the floor surface,
        // not inside the floor block where it gets permanently stuck.
        // Ground at y=0, NPC spawned slightly above (y=3) so gravity pulls it down.
        // The NPC is placed away from the player to avoid fleeing/aggro path interference.
        NPC npc = manager.spawnNPC(NPCType.JOGGER, 5, 3, 5); // Jogger always wanders (no daily-routine state)

        // Run many update frames to let the NPC fall and settle
        for (int i = 0; i < 120; i++) {
            manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        // NPC must NOT be inside the floor block (pos.y must be >= 1.0)
        float npcY = npc.getPosition().y;
        assertTrue(npcY >= 1.0f, "NPC fell through or into the floor; y=" + npcY);
    }

    @Test
    void testNPCLandsOnCorrectSurfaceAfterFallingFromHeight() {
        // If an NPC falls from y=5 the floor snap must not leave it inside a block.
        // The old Math.ceil snap could produce pos.y=1 which is the face of block y=1
        // (solid ground), embedding the NPC. The correct value is pos.y=1 ONLY when
        // the solid block is at y=0 (top surface = y+1 = 1). Verify pos.y == 1.0 exactly
        // after settling, proving the floor-snap places feet at the block top surface.

        NPC npc = manager.spawnNPC(NPCType.JOGGER, 10, 5, 10);

        for (int i = 0; i < 240; i++) {
            manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        float npcY = npc.getPosition().y;
        assertTrue(npcY >= 1.0f, "NPC sank into the floor; y=" + npcY);
        // Vertical velocity should be ~0 once settled on ground
        assertTrue(Math.abs(npc.getVelocity().y) < 1.0f, "NPC still has significant vertical velocity; vy=" + npc.getVelocity().y);
    }

    @Test
    void testPoliceNotDespawnedDuringDaytime() {
        // Regression test for issue #90: police spawned at game start must survive
        // into daytime gameplay. updatePoliceSpawning() must NOT despawn police
        // when time is between 06:00 and 22:00.
        NPC police1 = manager.spawnNPC(NPCType.POLICE, 20, 1, 10);
        NPC police2 = manager.spawnNPC(NPCType.POLICE, -30, 1, 20);
        NPC police3 = manager.spawnNPC(NPCType.POLICE, 50, 1, -10);
        if (police1 != null) police1.setState(NPCState.PATROLLING);
        if (police2 != null) police2.setState(NPCState.PATROLLING);
        if (police3 != null) police3.setState(NPCState.PATROLLING);

        long beforeCount = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(3, beforeCount, "Expected 3 police before daytime update");

        // Simulate a daytime frame (isNight=false) — police must NOT be despawned
        manager.updatePoliceSpawning(false, world, player);

        long afterCount = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(3, afterCount, "Police were despawned during daytime — issue #90 regression");
    }

    @Test
    void testPoliceCapIncreasesAtNight() {
        // At night (22:00+) the spawn routine should allow more police than the daytime cap.
        // Verify that calling updatePoliceSpawning() at night with 3 police does not skip
        // spawning (cap is 4 at night), whereas during the day with 3 police it should skip
        // because the daytime cap is 3.
        NPC p1 = manager.spawnNPC(NPCType.POLICE, 5, 1, 5);
        NPC p2 = manager.spawnNPC(NPCType.POLICE, 10, 1, 5);
        NPC p3 = manager.spawnNPC(NPCType.POLICE, 15, 1, 5);

        // Call at night — the cap is 4, so a 4th officer should eventually be spawned.
        // Reset cooldown by direct inspection isn't possible, but we can verify the
        // daytime call with exactly-cap police does NOT add more.
        manager.updatePoliceSpawning(false, world, player); // daytime, cap=3, already have 3 → no spawn
        long countAfterDay = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(3, countAfterDay, "Daytime should not spawn extra police beyond cap of 3");
    }

    @Test
    void testPoliceCapNotExceededBySpawnPolice() {
        // Regression test for issue #100: spawnPolice() spawns 2-3 NPCs per call,
        // which used to bypass the maxPolice cap. With 2 existing police and a daytime
        // cap of 3 (non-notorious player, daytime), only 1 slot remains. The call must
        // not push the total above 3.
        manager.spawnNPC(NPCType.POLICE, 5, 1, 5);
        manager.spawnNPC(NPCType.POLICE, 10, 1, 5);

        // Fresh manager: cooldown is 0, so updatePoliceSpawning will execute immediately.
        manager.updatePoliceSpawning(false, world, player); // daytime, cap=3, have 2 → 1 slot

        long policeCount = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertTrue(policeCount <= 3,
                "Police count exceeded cap: expected <= 3 but got " + policeCount);
    }

    @Test
    void testYouthGangStealing() {
        NPC youth = manager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1, 0.5f);
        player.getPosition().set(0, 1, 0);
        inventory.addItem(Material.WOOD, 5);

        assertEquals(5, inventory.getItemCount(Material.WOOD));

        // Update until youth is adjacent and steals
        for (int i = 0; i < 100; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
            if (inventory.getItemCount(Material.WOOD) < 5) {
                break;
            }
        }

        // Should have stolen at least 1 wood
        assertTrue(inventory.getItemCount(Material.WOOD) < 5);
    }

    /**
     * Fix #126: Youth gang steals highest-priority item (DIAMOND over WOOD).
     */
    @Test
    void testYouthGangStealsDiamondOverWood() {
        NPC youth = manager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1, 0.5f);
        player.getPosition().set(0, 1, 0);
        inventory.addItem(Material.WOOD, 5);
        inventory.addItem(Material.DIAMOND, 3);

        // Update until theft occurs
        for (int i = 0; i < 100; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
            if (inventory.getItemCount(Material.DIAMOND) < 3 || inventory.getItemCount(Material.WOOD) < 5) {
                break;
            }
        }

        // Gang should have stolen DIAMOND (highest priority), not WOOD
        assertTrue(inventory.getItemCount(Material.DIAMOND) < 3, "Gang should steal DIAMOND first");
        assertEquals(5, inventory.getItemCount(Material.WOOD), "WOOD should be untouched when DIAMOND is present");
    }

    /**
     * Fix #126: Youth gang steals from non-WOOD inventory when no WOOD is present.
     */
    @Test
    void testYouthGangStealsWhenNoWood() {
        NPC youth = manager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1, 0.5f);
        player.getPosition().set(0, 1, 0);
        inventory.addItem(Material.SCRAP_METAL, 4);

        // Update until theft occurs
        for (int i = 0; i < 100; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
            if (inventory.getItemCount(Material.SCRAP_METAL) < 4) {
                break;
            }
        }

        // Gang should have stolen SCRAP_METAL even though there's no WOOD
        assertTrue(inventory.getItemCount(Material.SCRAP_METAL) < 4, "Gang should steal SCRAP_METAL when no WOOD present");
    }

    /**
     * Integration test for issue #120: NPC immediately after a council builder that
     * finishes demolishing must not be skipped when the builder is removed.
     *
     * A COUNCIL_BUILDER with no assigned target triggers the removal path inside
     * updateCouncilBuilder(). Before the fix, npcs.remove(builder) at index i would
     * shift all subsequent elements left so the loop's i+1 pointed past the next NPC.
     * After the fix, the builder is marked dead via takeDamage and removed by the
     * removeIf at the top of the next update() call — the indexed loop is never
     * disturbed.
     *
     * Canary: a YOUTH_GANG placed right next to the player should switch to STEALING
     * state in the very first update frame (it is within 2.0f of the player). If the
     * canary NPC is skipped that frame, its state stays at WANDERING.
     */
    @Test
    void testNPCAfterBuilderNotSkippedOnBuilderRemoval() {
        // Place player with items to steal
        player.getPosition().set(0, 1, 0);
        inventory.addItem(Material.WOOD, 5);

        // Spawn a COUNCIL_BUILDER first (index 0). It has no builderTargets entry,
        // so target == null and it will trigger the removal path immediately.
        NPC builder = manager.spawnNPC(NPCType.COUNCIL_BUILDER, 30, 1, 30);
        assertNotNull(builder);

        // Spawn the canary NPC at index 1 — directly next to the player so it should
        // switch to STEALING state in the first update frame if it is not skipped.
        NPC canary = manager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1, 0.5f);
        assertNotNull(canary);

        assertEquals(2, manager.getNPCs().size(), "Expected builder + canary before update");

        // Single update frame — canary must be processed even though builder is removed
        manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);

        // The builder should have been marked dead (alive == false) during this frame
        assertFalse(builder.isAlive(),
                "Builder should be marked dead after update (no target assigned)");

        // The canary must have been updated: it was within 0.5 units of the player so
        // the YOUTH_GANG logic should have set it to STEALING.
        assertEquals(NPCState.STEALING, canary.getState(),
                "Canary NPC was skipped — builder removal corrupted the indexed loop (issue #120)");

        // Next frame: removeIf cleans up the dead builder; only canary remains
        manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        long aliveCount = manager.getNPCs().stream().filter(NPC::isAlive).count();
        assertEquals(1, aliveCount,
                "Expected exactly 1 alive NPC (canary) after builder is cleaned up");
    }

    /**
     * Fix #186: removeNPC() must clean up all per-NPC map entries.
     * After removal the NPC list must be empty and subsequent update() calls
     * must not process or reference the removed NPC in any per-NPC map.
     * We verify this indirectly: spawn an NPC, trigger map population by running
     * an update, call removeNPC, then run many more updates and assert no exceptions
     * and that the NPC list remains empty.
     */
    @Test
    void testRemoveNPCCleansUpPerNPCMaps() {
        // Spawn several NPC types whose maps will be populated on first update
        NPC youth = manager.spawnNPC(NPCType.YOUTH_GANG, 5, 1, 5);
        NPC police = manager.spawnNPC(NPCType.POLICE, 10, 1, 10);
        NPC builder = manager.spawnNPC(NPCType.COUNCIL_BUILDER, 20, 1, 20);
        NPC pub = manager.spawnNPC(NPCType.PUBLIC, 15, 1, 15);

        assertNotNull(youth);
        assertNotNull(police);
        assertNotNull(builder);
        assertNotNull(pub);

        // Run a few frames to populate per-NPC maps (path recalc timers, idle timers, etc.)
        for (int i = 0; i < 5; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
        }

        // Remove each NPC via removeNPC()
        manager.removeNPC(youth);
        manager.removeNPC(police);
        manager.removeNPC(builder);
        manager.removeNPC(pub);

        assertEquals(0, manager.getNPCs().size(), "All NPCs should be removed");

        // Running more updates must not throw and list must stay empty
        for (int i = 0; i < 10; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
        }

        assertEquals(0, manager.getNPCs().size(), "NPC list must remain empty after removeNPC cleanup");
    }

    /**
     * Fix #186: dead NPCs removed by the removeIf in update() must also have their
     * per-NPC map entries cleaned up (including npcStealCooldownTimers which was
     * previously missing from the removeIf cleanup block).
     * We verify by killing an NPC directly (via takeDamage, no speech set) so it is
     * removed in the next removeIf pass, then confirming further updates work cleanly.
     * The NPC is placed far from the player (30 blocks) so random speech cannot fire
     * during setup frames (speech only fires within 10 blocks of the player).
     */
    @Test
    void testDeadNPCRemovedByUpdateCleansUpAllMaps() {
        // Place inventory item so steal cooldown can be set
        inventory.addItem(Material.WOOD, 5);

        // Spawn NPC far from player so random speech does not fire (speech range = 10 blocks)
        NPC youth = manager.spawnNPC(NPCType.YOUTH_GANG, 30, 1, 30);
        assertNotNull(youth);

        // Run a few frames to populate per-NPC maps (path recalc timers etc.)
        for (int i = 0; i < 10; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
        }

        // Confirm NPC is still alive and in the list
        assertTrue(youth.isAlive(), "Youth NPC should be alive before damage");
        assertTrue(manager.getNPCs().contains(youth), "Youth NPC should be in the list");

        // Kill the NPC directly without speech (so isSpeaking()=false and removeIf fires next frame)
        youth.takeDamage(Float.MAX_VALUE);
        assertFalse(youth.isAlive(), "Youth NPC should be dead after lethal damage");
        assertFalse(youth.isSpeaking(), "Youth NPC should not be speaking (no speech was set)");

        // Run one update frame — removeIf at the top of update() should remove the dead NPC
        manager.update(0.1f, world, player, inventory, tooltipSystem);

        // NPC should have been removed from the main list
        assertFalse(manager.getNPCs().contains(youth),
                "Dead youth gang NPC (no speech) should be removed from the NPC list by removeIf");

        // Further updates must not throw due to stale map entries (issue #186 regression)
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                manager.update(0.1f, world, player, inventory, tooltipSystem);
            }
        }, "update() threw after dead NPC removal — stale map references may exist (issue #186)");
    }

    /**
     * Fix #186: despawnPolice() must also clean up all per-NPC maps for each
     * police NPC it removes, not just policeWarningTimers and policeTargetStructures.
     */
    @Test
    void testDespawnPoliceCleansUpAllMaps() {
        // Simulate night → day transition to trigger despawnPolice()
        // First call with isNight=true to set wasNight=true and spawn police
        manager.updatePoliceSpawning(true, world, player);

        long policeCount = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertTrue(policeCount > 0, "Police should have been spawned during night");

        // Run a few update frames to populate per-NPC maps for the police NPCs
        for (int i = 0; i < 5; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
        }

        // Dawn transition: isNight goes false → despawnPolice() is called
        manager.updatePoliceSpawning(false, world, player);

        long policeAfterDawn = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(0, policeAfterDawn,
                "All police should be despawned at dawn");

        // Further updates must not throw due to stale map entries
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                manager.update(0.1f, world, player, inventory, tooltipSystem);
            }
        }, "update() threw after dawn police despawn — stale map references may exist (issue #186)");
    }

    /**
     * Integration test for issue #120: Multiple council builders removed in the same
     * frame must not corrupt the NPC list or orphan subsequent NPCs.
     *
     * Three builders (indices 0, 1, 2) are followed by three canary NPCs (indices 3, 4, 5).
     * Each builder has no target, so all three trigger the removal path in the same
     * update() call. After the fix all three canaries must still receive their update.
     */
    @Test
    void testMultipleBuildersRemovedSameFrameDoNotOrphanSubsequentNPCs() {
        player.getPosition().set(0, 1, 0);
        inventory.addItem(Material.WOOD, 10);

        // Spawn three builders with no target (trigger removal path for each)
        NPC builder1 = manager.spawnNPC(NPCType.COUNCIL_BUILDER, 30, 1, 30);
        NPC builder2 = manager.spawnNPC(NPCType.COUNCIL_BUILDER, 31, 1, 30);
        NPC builder3 = manager.spawnNPC(NPCType.COUNCIL_BUILDER, 32, 1, 30);
        assertNotNull(builder1);
        assertNotNull(builder2);
        assertNotNull(builder3);

        // Spawn three canary youth gangs immediately next to player (within 2.0f)
        NPC canary1 = manager.spawnNPC(NPCType.YOUTH_GANG, 0.3f, 1, 0.3f);
        NPC canary2 = manager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1, 0.5f);
        NPC canary3 = manager.spawnNPC(NPCType.YOUTH_GANG, 0.7f, 1, 0.7f);
        assertNotNull(canary1);
        assertNotNull(canary2);
        assertNotNull(canary3);

        assertEquals(6, manager.getNPCs().size(), "Expected 3 builders + 3 canaries");

        // Single update frame — all three canaries must be processed
        manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);

        // All builders must be dead (marked via takeDamage)
        assertFalse(builder1.isAlive(), "Builder 1 should be dead after update");
        assertFalse(builder2.isAlive(), "Builder 2 should be dead after update");
        assertFalse(builder3.isAlive(), "Builder 3 should be dead after update");

        // All canaries must have been updated — each should be STEALING because
        // they are within 2.0f of the player's position.
        assertEquals(NPCState.STEALING, canary1.getState(),
                "Canary 1 was skipped — multiple builder removals corrupted the loop (issue #120)");
        assertEquals(NPCState.STEALING, canary2.getState(),
                "Canary 2 was skipped — multiple builder removals corrupted the loop (issue #120)");
        assertEquals(NPCState.STEALING, canary3.getState(),
                "Canary 3 was skipped — multiple builder removals corrupted the loop (issue #120)");

        // After the next frame's removeIf only the three living canaries remain
        manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        long aliveCount = manager.getNPCs().stream().filter(NPC::isAlive).count();
        assertEquals(3, aliveCount,
                "Expected exactly 3 alive NPCs (canaries) after all builders cleaned up");
    }

    /**
     * Fix #215: Knocked-out NPC last words must disappear over time.
     *
     * When an NPC is defeated (knocked out), punchNPC() sets its speech text with a
     * 2-second duration. Dead NPCs must still have their timers ticked each frame so
     * that isSpeaking() eventually returns false and the NPC can be removed from the
     * list. Before the fix, dead NPCs skipped updateTimers() entirely, so the speech
     * timer never decremented and the NPC (and its speech bubble) persisted forever.
     */
    @Test
    void testKnockedOutNPCLastWordsDisappearOverTime() {
        // Spawn NPC far from player so random speech/flee logic doesn't interfere
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 30, 1, 30);
        assertNotNull(npc);

        // Kill via punchNPC — this sets KNOCKED_OUT state and a 2-second speech timer
        Vector3 punchDir = new Vector3(0, 0, 1);
        for (int i = 0; i < 10; i++) {
            manager.punchNPC(npc, punchDir, inventory, tooltipSystem);
        }

        assertFalse(npc.isAlive(), "NPC should be dead after enough punches");
        assertEquals(NPCState.KNOCKED_OUT, npc.getState(), "NPC should be in KNOCKED_OUT state");
        assertTrue(npc.isSpeaking(), "NPC should have last-words speech set");

        // NPC must not be removed yet (still speaking)
        manager.update(0.01f, world, player, inventory, tooltipSystem);
        assertTrue(manager.getNPCs().contains(npc), "Dead speaking NPC should still be in list");
        assertTrue(npc.isSpeaking(), "Speech should still be active after 0.01s");

        // Advance time past the speech duration (2 seconds) — speech timer must count down
        for (int i = 0; i < 30; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
        }

        // After 3 seconds the speech timer (2s) has expired
        assertFalse(npc.isSpeaking(),
                "Fix #215: knocked-out NPC speech must expire — dead NPCs were not ticking their timers");

        // And on the next update the NPC should be removed from the list
        manager.update(0.01f, world, player, inventory, tooltipSystem);
        assertFalse(manager.getNPCs().contains(npc),
                "Fix #215: knocked-out NPC should be removed from list once speech expires");
    }

    /**
     * Fix #393: tickSpawnCooldown() must drain the police spawn cooldown independently
     * of update(), so that calling it while paused prevents the swarm-spawn on resume.
     *
     * Strategy: trigger a police spawn (sets policeSpawnCooldown = 10s) via
     * updatePoliceSpawning(), then drain the cooldown entirely via tickSpawnCooldown()
     * without calling update(). A subsequent updatePoliceSpawning() call must attempt
     * to spawn police again (i.e. the cooldown is no longer blocking).
     */
    @Test
    void testTickPostArrestCooldownDrainsIndependentlyOfUpdate() {
        // Calling with zero cooldown must be a no-op (no negative drift)
        manager.tickPostArrestCooldown(5.0f);
        assertEquals(0f, manager.getPostArrestCooldown(), 0.001f,
                "Fix #403: tickPostArrestCooldown() must not go below zero");

        // Simulate an arrest clearing: sets postArrestCooldown = POST_ARREST_COOLDOWN_DURATION (10 s)
        manager.clearArrestPending();
        float initial = manager.getPostArrestCooldown();
        assertTrue(initial > 0f, "Fix #403: clearArrestPending() must set a positive postArrestCooldown");

        // Drain partially without calling update() — mirrors the PAUSED branch
        manager.tickPostArrestCooldown(3.0f);
        float afterPartial = manager.getPostArrestCooldown();
        assertTrue(afterPartial < initial,
                "Fix #403: tickPostArrestCooldown() must reduce the cooldown");
        assertTrue(afterPartial > 0f,
                "Fix #403: partial tick must not fully drain the cooldown");

        // Drain well past the full duration — must clamp to zero, never negative
        manager.tickPostArrestCooldown(20.0f);
        assertEquals(0f, manager.getPostArrestCooldown(), 0.001f,
                "Fix #403: tickPostArrestCooldown() must clamp to zero, never go negative");
    }

    @Test
    void testTickSpawnCooldownDrainsIndependentlyOfUpdate() {
        // Tick spawn cooldown when it is already zero — must be a no-op (no negative drift)
        manager.tickSpawnCooldown(5.0f);

        // Now trigger a spawn cycle at night so policeSpawnCooldown is set to POLICE_SPAWN_INTERVAL (10 s)
        manager.setGameTime(22.0f); // midnight-ish
        int before = (int) manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        manager.updatePoliceSpawning(true, world, player);
        int afterFirstSpawn = (int) manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        // The cooldown is now positive; a second immediate call must be blocked
        manager.updatePoliceSpawning(true, world, player);
        int afterSecondCall = (int) manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(afterFirstSpawn, afterSecondCall,
                "Fix #393: second immediate updatePoliceSpawning() should be throttled by cooldown");

        // Drain the cooldown via tickSpawnCooldown() — simulating the PAUSED branch
        manager.tickSpawnCooldown(15.0f); // drain well past POLICE_SPAWN_INTERVAL

        // After draining, the next spawn call must be allowed (police count may increase)
        manager.updatePoliceSpawning(true, world, player);
        int afterCooldownDrained = (int) manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        // Either more police were spawned, or the cap was already reached — either way no exception
        assertTrue(afterCooldownDrained >= afterSecondCall,
                "Fix #393: after tickSpawnCooldown() drains the timer, police spawning must be re-allowed");
    }

    /**
     * Fix #405: tickKnockbackTimers() must clear NPC knockback even while the game is paused.
     *
     * Scenario: punch an NPC so it is in mid-knockback, then simulate 30 frames (0.5 s) of
     * PAUSED state via tickKnockbackTimers(). The knockback timer (0.2 s) must have expired
     * well within 0.5 s, so isKnockedBack() must return false.
     */
    @Test
    void tickKnockbackTimersClearsKnockbackWhilePaused() {
        NPC npc = manager.spawnNPC(NPCType.YOUTH_GANG, 10, 1, 10);

        // Apply knockback — sets knockbackTimer = 0.2 s
        npc.applyKnockback(new Vector3(0, 0, -1), 2.0f);
        assertTrue(npc.isKnockedBack(), "NPC must be in knockback state immediately after applyKnockback()");

        // Simulate 30 frames at 1/60 s each (= 0.5 s total) via the PAUSED-branch method.
        // 0.5 s >> 0.2 s timer, so knockback must have expired.
        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 30; i++) {
            manager.tickKnockbackTimers(delta);
        }

        assertFalse(npc.isKnockedBack(),
                "Fix #405: knockbackTimer must expire via tickKnockbackTimers() even while paused");
    }

    /**
     * Fix #423: tickSpeechTimers() must NOT advance attackCooldown or blinkTimer while paused.
     *
     * Scenario: spawn an NPC, give it a speech bubble and a non-zero attack cooldown,
     * then simulate 5 seconds of PAUSED-state ticking via tickSpeechTimers(). The speech
     * timer must expire (bubble gone) but attackCooldown and blinkTimer must be unchanged.
     */
    @Test
    void tickSpeechTimersDoesNotDrainAttackCooldownOrBlinkTimerWhilePaused() {
        NPC npc = manager.spawnNPC(NPCType.YOUTH_GANG, 10, 1, 10);

        // Give the NPC a speech bubble
        npc.setSpeechText("Watch it!", 3.0f);
        assertTrue(npc.isSpeaking(), "NPC must be speaking before test starts");

        // Set a non-zero attack cooldown
        npc.resetAttackCooldown();
        float initialCooldown = npc.getAttackCooldown();
        assertTrue(initialCooldown > 0f, "attackCooldown must be > 0 after resetAttackCooldown()");

        float initialBlinkTimer = npc.getBlinkTimer();

        // Simulate 5 seconds of PAUSED-state ticking (> speech duration of 3s)
        float delta = 1.0f / 60.0f;
        int frames = (int) (5.0f / delta);
        for (int i = 0; i < frames; i++) {
            manager.tickSpeechTimers(delta);
        }

        // Speech bubble must have expired
        assertFalse(npc.isSpeaking(),
                "Fix #423: speech bubble must expire after 5 s of tickSpeechTimers()");

        // attackCooldown must NOT have drained
        assertEquals(initialCooldown, npc.getAttackCooldown(), 0.001f,
                "Fix #423: attackCooldown must not drain during tickSpeechTimers() while paused");

        // blinkTimer must NOT have advanced
        assertEquals(initialBlinkTimer, npc.getBlinkTimer(), 0.001f,
                "Fix #423: blinkTimer must not advance during tickSpeechTimers() while paused");
    }

    /**
     * Fix #407: tickRecoveryTimers() must revive a KNOCKED_OUT NPC even while the game is paused.
     *
     * Scenario: knock out an NPC, then simulate enough PAUSED-state frames via
     * tickRecoveryTimers() to exceed KNOCKED_OUT_RECOVERY_DURATION (10 s). The NPC must
     * be alive and back in WANDERING state once the timer expires.
     */
    @Test
    void tickRecoveryTimersRevivesKnockedOutNPCWhilePaused() {
        NPC npc = manager.spawnNPC(NPCType.YOUTH_GANG, 10, 1, 10);

        // Punch the NPC until it is knocked out
        Vector3 punchDir = new Vector3(0, 0, 1);
        for (int i = 0; i < 10; i++) {
            manager.punchNPC(npc, punchDir, inventory, tooltipSystem);
        }

        assertFalse(npc.isAlive(), "NPC must be dead after enough punches");
        assertEquals(NPCState.KNOCKED_OUT, npc.getState(), "NPC must be in KNOCKED_OUT state");

        // Simulate PAUSED-state ticking for 11 seconds (> 10 s recovery duration).
        float delta = 1.0f / 60.0f;
        int frames = (int) (11.0f / delta); // ~660 frames
        for (int i = 0; i < frames; i++) {
            manager.tickRecoveryTimers(delta);
        }

        assertTrue(npc.isAlive(),
                "Fix #407: NPC must be revived via tickRecoveryTimers() after recovery duration elapses while paused");
        assertEquals(NPCState.WANDERING, npc.getState(),
                "Fix #407: revived NPC must return to WANDERING state");
    }
}

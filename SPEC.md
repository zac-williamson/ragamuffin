# Ragamuffin — Development Milestones

This is a **3D voxel-based** game. All rendering is in 3D with a first-person camera.
The world is made of blocks (like Minecraft) in an urban British setting.

Work through these phases in order. Each phase should be fully tested and working
before moving to the next. Use test-driven development throughout: write failing
tests first, then implement until they pass.

Run `./gradlew test` after every significant change. Run `./gradlew build` before
marking any phase complete.

## Key Controls Reference
- **WASD**: Move player
- **Mouse**: Look around (first-person)
- **Left click / punch**: Break block or hit NPC (hold for 5 hits to break a block)
- **Right click**: Place block from hotbar
- **I**: Open/close inventory UI
- **H**: Open/close help UI (displays full list of controls and commands)
- **C**: Open/close crafting menu
- **E**: Interact with objects/NPCs
- **1-9**: Select hotbar slot
- **ESC**: Pause menu (or close any open UI)

**CRITICAL — Integration Tests**: Every phase MUST include integration tests that
verify the feature works end-to-end, not just unit tests of isolated classes.
If the game compiles and unit tests pass but the feature doesn't actually work
when you run the game, that is a failure. Integration tests are specified below
as explicit, concrete scenarios with exact expected outcomes. Implement these
EXACTLY as described — do not paraphrase or simplify them. Use the LibGDX headless
backend for integration tests where possible.

---

## Phase 1: 3D Core Engine

**Goal**: A running game window with a 3D world, first-person camera, and basic
voxel rendering.

- Game window opens at 1280x720 with title "Ragamuffin"
- Game state machine: MENU, PLAYING, PAUSED
- PerspectiveCamera with first-person controls (WASD movement, mouse look)
- Voxel chunk system: world divided into chunks (e.g. 16x16x64 blocks)
- Block types enum (AIR, GRASS, DIRT, STONE, PAVEMENT, ROAD, BRICK, GLASS, WOOD, WATER, TREE_TRUNK, LEAVES)
- Chunk mesh builder: generates a 3D mesh from blocks, only rendering exposed faces
- Basic texture atlas with simple coloured textures for each block type
- Render chunks using ModelBatch with basic ambient + directional lighting
- Player entity with position, AABB collision against voxel grid
- Mouse capture for look controls, ESC to release

**Unit tests**: Block types, chunk data storage, mesh face culling logic, player
movement, collision detection, state transitions.

**Integration tests — implement these exact scenarios:**

1. **Player movement updates camera**: Place the player at position (10, 5, 10).
   Simulate pressing W for 60 frames (1 second at 60fps). Verify the player's
   Z coordinate has decreased (moved forward). Verify the PerspectiveCamera's
   position matches the player's position (offset by eye height).

2. **Collision prevents movement into solid blocks**: Place the player at (10, 1, 10).
   Place a STONE block at (10, 1, 9) — directly in front of the player. Simulate
   pressing W for 60 frames. Verify the player's Z coordinate has NOT moved past
   the block boundary (Z >= 9.0 + player half-width). Verify the STONE block is
   still present.

3. **Chunk mesh generation**: Create a chunk. Set block (8, 0, 8) to GRASS and all
   surrounding blocks to AIR. Build the chunk mesh. Verify the mesh has exactly 6
   faces (one per exposed side). Now set block (8, 1, 8) to GRASS as well. Rebuild.
   Verify the mesh now has 10 faces (6+6 minus 2 shared faces).

4. **State machine transitions**: Game starts in MENU state. Transition to PLAYING.
   Verify state is PLAYING and player input is enabled. Press ESC. Verify state is
   PAUSED and player input is disabled. Press ESC again. Verify state returns to
   PLAYING.

5. **Player cannot move in PAUSED state**: Set state to PAUSED. Record player
   position. Simulate pressing W for 30 frames. Verify player position is unchanged.

---

## Phase 2: World Generation

**Goal**: Procedurally generate a 3D British town centred on a park.

- Terrain generation: flat urban terrain with slight elevation variation
- Park area in the centre (grass blocks, tree structures, benches, a sad pond)
- Streets: pavement and road blocks in a grid pattern
- Terraced houses: multi-storey brick structures with windows (glass blocks), doors, roofs
- Parade of shops (off-licence, Greggs, charity shop, bookies) — distinct block patterns
- Office building: taller structure, glass and concrete blocks
- Jeweller shop with distinct facade
- JobCentre, industrial estate on the outskirts
- World bounded at ~200x200 blocks, buildings up to ~15 blocks tall
- Chunk loading/unloading based on player distance

**Unit tests**: World generator produces expected zone types, buildings have correct
dimensions, key landmarks exist at expected positions, block types are correct for
each zone.

**Integration tests — implement these exact scenarios:**

1. **Park exists at world centre**: Generate the world. Sample a 20x20 block area
   at the world centre. Verify at least 60% of ground-level blocks are GRASS. Verify
   at least 2 tree structures exist (a TREE_TRUNK block with LEAVES blocks above it)
   within the park area. Verify at least 1 WATER block exists (the pond).

2. **Buildings are solid and enclosed**: Generate the world. Find the office building
   location. Verify that walking into the building's exterior wall blocks (BRICK or
   GLASS) prevents player movement — place the player 1 block outside the wall,
   simulate pressing W for 60 frames, verify the player has not passed through the
   wall.

3. **Key landmarks all exist**: Generate the world. Verify each of the following
   landmarks exists by finding at least one block tagged/associated with it: park,
   Greggs, off-licence, charity shop, jeweller, office building, JobCentre.
   Each landmark must be at a unique position (no two landmarks overlap).

4. **Chunks load and unload**: Place the player at the world centre. Record which
   chunks are loaded. Move the player 100 blocks north (simulate movement over
   multiple frames). Verify that new chunks near the player are now loaded, and
   chunks far behind the player (beyond render distance) have been unloaded.

5. **Streets connect landmarks**: Generate the world. Pick two landmark positions
   (e.g. park and Greggs). Verify there exists a continuous path of PAVEMENT or
   ROAD blocks at ground level between them (BFS/flood fill on walkable blocks).

---

## Phase 3: Resource & Inventory System

**Goal**: Player can break blocks, collect resources, and manage inventory.

- Block breaking: look at a block, left click/punch to hit it (5 hits to break)
- Block highlight/outline showing which block the player is targeting (raycasting)
- Blocks drop items when broken (wood from trees, brick from houses, etc.)
- Different materials from different block sources
- Inventory UI: grid-based overlay, toggled with **I** key
- Help UI: displays full list of controls/commands, toggled with **H** key
- Inventory shows collected items with quantities and icons
- Office buildings yield advanced materials (computers, office chairs, staplers)
- Jeweller yields diamond. Tooltip: "Jewellers can be a good source of diamond"
- Tooltip on first tree punch: "Punch a tree to get wood"
- Hotbar at bottom of screen showing selected item slot (keys 1-9 to select)

**Unit tests**: Raycasting hits correct block, inventory add/remove/stack, material
drop tables, tooltip trigger conditions.

**Integration tests — implement these exact scenarios:**

1. **Punching a tree 5 times yields wood**: Place the player adjacent to a
   TREE_TRUNK block, facing it. Simulate 5 punch actions. Verify the TREE_TRUNK
   block has been removed from the world (replaced with AIR). Verify the player's
   inventory contains exactly 1 WOOD item.

2. **Punching a tree only 4 times does NOT yield wood**: Place the player adjacent
   to a TREE_TRUNK block, facing it. Simulate 4 punch actions. Verify the
   TREE_TRUNK block is still present in the world. Verify the player's inventory
   does NOT contain any WOOD item.

3. **Punching 5 times when not facing a block does nothing**: Place the player in
   an open area with no blocks within punch range. Record the inventory state.
   Simulate 5 punch actions. Verify the inventory is unchanged (same items, same
   counts).

4. **Punching a BRICK block yields BRICK material**: Place the player adjacent to
   a BRICK block. Simulate 5 punch actions. Verify the block is removed and the
   inventory contains exactly 1 BRICK item.

5. **Punching a jeweller block yields diamond**: Place the player adjacent to a
   block that is part of the jeweller shop (e.g. a GLASS block tagged as jeweller
   inventory). Simulate 5 punch actions. Verify the player's inventory contains
   exactly 1 DIAMOND item. Verify the tooltip "Jewellers can be a good source of
   diamond" has been triggered.

6. **First tree punch triggers tooltip**: Place the player adjacent to a TREE_TRUNK.
   Simulate 1 punch action. Verify the tooltip system has fired the message "Punch
   a tree to get wood". Simulate punching a second tree later. Verify the tooltip
   does NOT fire again (first-time only).

7. **Inventory UI toggles with I key**: Verify the inventory UI is not visible.
   Simulate pressing I. Verify the inventory UI is now visible/active. Simulate
   pressing I again. Verify the inventory UI is hidden. Verify that while the
   inventory UI is open, player movement is disabled.

8. **Help UI toggles with H key**: Verify the help UI is not visible. Simulate
   pressing H. Verify the help UI is now visible and contains text describing all
   controls (WASD, I, H, C, E, ESC, mouse, punch, place). Simulate pressing H
   again. Verify the help UI is hidden.

9. **Hotbar selection**: Give the player WOOD in slot 1 and BRICK in slot 2.
   Simulate pressing key 1. Verify the selected hotbar slot is 0 (first slot)
   and the active item is WOOD. Simulate pressing key 2. Verify the selected
   slot is 1 and the active item is BRICK.

10. **Multiple block breaks accumulate in inventory**: Place the player adjacent to
    3 TREE_TRUNK blocks. Break all 3 (5 punches each, repositioning between them).
    Verify the inventory contains exactly 3 WOOD items (stacked).

---

## Phase 4: Crafting & Building

**Goal**: Player can craft items and place blocks/structures in the world.

- Crafting menu (accessible via **C** key) with grid-based recipes
- Recipes use gathered materials (e.g. 4 WOOD → 8 PLANKS, 6 PLANKS → 1 SHELTER_WALL)
- Right-click to place blocks from hotbar into the world
- Block placement preview (ghost block showing where it will be placed)
- Placed blocks update the chunk mesh and collision
- Structures are persistent in the world data
- Increasing complexity tiers: basic shelter → proper structure → elaborate builds

**Unit tests**: Crafting recipes produce correct outputs, consume correct inputs,
block placement updates chunk data, collision updated after placement.

**Integration tests — implement these exact scenarios:**

1. **Craft planks from wood**: Give the player 4 WOOD items. Open the crafting menu
   (press C). Select the "4 WOOD → 8 PLANKS" recipe. Execute the craft. Verify
   the player now has 0 WOOD and 8 PLANKS in their inventory. Close crafting menu
   (press C). Verify menu is hidden.

2. **Cannot craft without sufficient materials**: Give the player 3 WOOD items.
   Open crafting menu. Attempt to craft "4 WOOD → 8 PLANKS". Verify the craft
   fails or the recipe is greyed out. Verify the player still has 3 WOOD.

3. **Place a block in the world**: Give the player 1 PLANKS item in hotbar slot 1.
   Select slot 1. Face an empty space adjacent to the ground. Right-click. Verify
   the PLANKS block now exists at the target position in the chunk data. Verify the
   player's PLANKS count has decreased by 1. Verify the chunk mesh has been rebuilt
   to include the new block.

4. **Placed block has collision**: Place a PLANKS block at position (15, 1, 15) via
   the building system. Position the player at (15, 1, 17), facing the placed block.
   Simulate pressing W for 60 frames. Verify the player cannot walk through the
   placed block (player Z >= 15.0 + block size + player half-width).

5. **Cannot place block with empty hotbar**: Ensure the player's selected hotbar
   slot is empty. Right-click on a valid placement location. Verify no block is
   placed. Verify the world is unchanged.

6. **Full gather-craft-build pipeline**: Start with empty inventory. Place the
   player next to 4 TREE_TRUNK blocks. Break all 4 (5 punches each). Verify 4
   WOOD in inventory. Open crafting menu, craft "4 WOOD → 8 PLANKS". Verify 8
   PLANKS. Place 4 PLANKS as a 2x2 wall. Verify 4 PLANKS blocks exist in the
   world at the expected positions. Verify 4 PLANKS remain in inventory.

7. **Crafting menu shows only available recipes**: Give the player 4 WOOD and 0 of
   everything else. Open crafting menu. Verify the WOOD→PLANKS recipe is shown as
   available/craftable. Verify that recipes requiring materials the player doesn't
   have are shown as unavailable/greyed out.

---

## Phase 5: NPC System & AI

**Goal**: Populate the 3D world with NPCs that react to the player.

- NPC entities rendered as simple 3D models or billboard sprites in the world
- Members of the public wander the streets, react to structures (stop and stare,
  take photos, complain)
- Dogs roam the park, bark at things
- Gangs of youths loiter, may steal resources or vandalise structures
- 3D pathfinding on the voxel grid (A* adapted for 3D block world)
- NPCs have daily routines (go to work, come home, visit the pub)
- NPC speech as floating text/bubbles above their heads
- Punching an NPC knocks them back

**Unit tests**: NPC spawning, pathfinding produces valid 3D paths, reaction state
machines, daily routine transitions.

**Integration tests — implement these exact scenarios:**

1. **Punching a council member knocks them back**: Spawn a COUNCIL_MEMBER NPC at
   position (20, 1, 20). Place the player at (20, 1, 21), facing the NPC (north).
   Simulate 1 punch action targeting the NPC. Verify the NPC's Z coordinate has
   decreased (knocked northward, away from the player). The NPC should be at
   approximately (20, 1, 18) or further — at least 2 blocks of knockback.

2. **Punching an NPC does not affect inventory**: Spawn a PUBLIC NPC. Place the
   player adjacent, facing the NPC. Record inventory state. Simulate 5 punch
   actions targeting the NPC. Verify inventory is completely unchanged (no items
   added or removed).

3. **NPC pathfinds around a wall**: Spawn a PUBLIC NPC at (30, 1, 30). Place a
   solid wall of BRICK blocks from (35, 1, 25) to (35, 1, 35) — a 10-block wall
   blocking the direct path east. Set the NPC's target to (40, 1, 30). Advance
   the simulation for 300 frames. Verify the NPC has reached within 2 blocks of
   the target. Verify the NPC's path at no point passed through any BRICK block
   (check position history).

4. **NPCs react to player-built structures**: Place the player in an open area
   with a PUBLIC NPC within 20 blocks. Build a 3x3x3 structure (27 blocks). Verify
   the NPC's state changes to a reaction state (STARING, PHOTOGRAPHING, or
   COMPLAINING). Verify the NPC moves toward the structure (distance to structure
   decreases over 120 frames).

5. **Dogs roam within park boundaries**: Spawn 3 DOG NPCs in the park area.
   Advance the simulation for 600 frames (10 seconds). Verify all 3 dogs are still
   within the park boundary. Verify each dog has moved at least 3 blocks from its
   starting position (they are roaming, not stationary).

6. **Gangs of youths steal from player**: Spawn a YOUTH_GANG NPC near the player.
   Give the player 5 WOOD items. Advance the simulation until the youth is adjacent
   to the player (max 300 frames). Verify the player's WOOD count has decreased by
   at least 1. Verify the theft triggers a tooltip or notification.

7. **NPC daily routine changes over time**: Spawn a PUBLIC NPC. Set the game time
   to 08:00 (morning). Verify the NPC's routine state is GOING_TO_WORK. Advance
   time to 17:00 (evening). Verify the NPC's routine state is GOING_HOME. Advance
   time to 20:00 (night). Verify the NPC's routine state is AT_PUB or AT_HOME.

---

## Phase 6: Day/Night Cycle & Police

**Goal**: Time passes in-game with 3D lighting changes. Night brings the police.

- Day/night cycle: directional light rotates, ambient light dims, sky colour changes
- Sun/moon representation in skybox or as directional light indicator
- Clock display on HUD
- Police NPCs spawn at night, patrol streets with torches/flashlights
- Police approach player and try to "move them on" (dialogue/interaction)
- If player refuses or is caught near structures, police escalate
- Police can place police tape on player structures (tape block/overlay on structure)
- Tooltip on first police encounter: appropriate sardonic guidance

**Unit tests**: Time system progression, lighting parameter calculation, police
spawn timing, escalation state machine, police tape placement logic.

**Integration tests — implement these exact scenarios:**

1. **Lighting changes with time of day**: Set time to 12:00 (noon). Record the
   directional light's intensity and colour. Advance time to 00:00 (midnight).
   Verify the directional light intensity has decreased by at least 50%. Verify
   the ambient light colour has shifted toward blue/dark. Advance time back to
   12:00. Verify lighting returns to approximately the original values.

2. **HUD clock updates with game time**: Set time to 06:00. Verify the HUD clock
   displays "06:00". Advance time by 1 in-game hour. Verify the HUD clock displays
   "07:00". Advance to 23:59. Verify it displays "23:59". Advance 1 more minute.
   Verify it wraps to "00:00".

3. **Police spawn at night, not during day**: Set time to 14:00 (daytime). Verify
   no POLICE NPCs exist in the world. Advance time to 22:00 (night). Verify at
   least 1 POLICE NPC has spawned. Advance time to 06:00 (morning). Verify all
   POLICE NPCs have despawned or left the map.

4. **Police approach and interact with player**: Set time to 22:00. Let police
   spawn. Record the closest police NPC's position and the player's position.
   Advance 300 frames. Verify the police NPC is now closer to the player than
   before. When the police NPC is adjacent (within 2 blocks), verify a dialogue/
   interaction event fires with a "move along" message.

5. **Police tape player structure**: Build a 3x3x3 structure. Set time to 22:00.
   Let police spawn and approach. Advance the simulation until police interact with
   the structure (max 600 frames). Verify at least 1 block of the structure now has
   a POLICE_TAPE state/overlay. Verify the taped block is no longer usable by the
   player (cannot be broken or interacted with normally).

6. **First police encounter triggers tooltip**: Ensure the player has never
   encountered police. Set time to 22:00. Let police spawn and approach. When the
   first police interaction fires, verify the tooltip system emits a sardonic
   message. Trigger a second police encounter. Verify the tooltip does NOT fire
   again.

7. **Police escalation**: Build a structure. Set time to 22:00. Police approach and
   issue a "move along" warning. Player remains near structure (don't move). Advance
   120 frames. Verify the police NPC's state has escalated (from WARNING to
   AGGRESSIVE or ARRESTING). Verify the escalated state applies a harsher penalty
   (e.g. player is forcibly teleported away from the structure, or more police
   spawn).

---

## Phase 7: Council & Demolition

**Goal**: The local council responds to player building activity.

- After structures reach a certain size/complexity, council builders arrive
- Council builders approach structures and begin demolition (removing blocks)
- Tooltip: "Dodge to avoid the attacks of stronger enemies"
- Player can interfere/distract council builders (punch to knock back)
- Council planning notices appear on structures before demolition (paper texture
  on a block face)
- Escalation: more builders arrive for larger structures
- Council members appear as bureaucratic antagonists with clipboards

**Unit tests**: Structure complexity calculation, threshold triggers, demolition
block removal logic, builder spawning scales with complexity.

**Integration tests — implement these exact scenarios:**

1. **Small structure does NOT trigger council**: Build a 2x2x2 structure (8 blocks).
   Advance the simulation for 600 frames. Verify no COUNCIL_BUILDER NPCs have
   spawned. Verify no planning notices appear on the structure.

2. **Large structure triggers council**: Build a 5x5x5 structure (125 blocks).
   Advance the simulation for 300 frames. Verify a planning notice has appeared
   on at least one block face of the structure. Advance another 300 frames. Verify
   at least 1 COUNCIL_BUILDER NPC has spawned and is moving toward the structure.

3. **Council builders demolish blocks**: Build a 5x5x5 structure. Let council
   builders spawn and reach the structure. Count the blocks in the structure.
   Advance 600 frames. Count the blocks again. Verify at least 5 blocks have been
   removed by the builders. Verify the removed blocks are now AIR in the chunk data.

4. **Punching a council builder knocks them back and delays demolition**: Build a
   5x5x5 structure. Let a council builder spawn and approach the structure. When
   the builder is adjacent to the structure, record the structure's block count.
   Punch the builder 1 time. Verify the builder's position has moved at least 2
   blocks away from the structure (knockback). Advance 60 frames. Verify the
   structure block count is unchanged (knockback delayed demolition).

5. **Larger structures spawn more builders**: Build a 5x5x5 structure. Count the
   number of COUNCIL_BUILDER NPCs that spawn within 600 frames. Record this number.
   Build a 10x5x5 structure (twice as large). Count the builders that spawn within
   600 frames. Verify the larger structure spawned more builders than the smaller
   one.

6. **Tooltip fires on first council encounter**: Build a large structure and let
   council builders arrive. When the first builder begins demolition, verify the
   tooltip "Dodge to avoid the attacks of stronger enemies" is triggered. Let a
   second builder arrive. Verify the tooltip does NOT fire again.

7. **Planning notice appears before builders arrive**: Build a 5x5x5 structure.
   Advance 120 frames. Verify a planning notice (visual indicator on a block face)
   has appeared on the structure. Verify NO council builders have spawned yet.
   Advance another 300 frames. NOW verify builders have spawned. The notice must
   precede the builders.

---

## Phase 8: HUD, Tooltips & Polish

**Goal**: Full 3D UI overlay, tutorial tooltips, and gameplay polish.

- Health/hunger/energy bars on HUD (rendered as 2D overlay on the 3D scene)
- Crosshair in centre of screen
- Tooltip system: context-sensitive sardonic advice rendered as screen-space text
- Opening sequence: text overlay "it's time to learn to survive on your own",
  camera starts looking at the park
- Pause menu (ESC) with resume/quit — renders over the 3D scene
- Main menu screen with "New Game" option
- Help UI (H key) shows all controls
- Inventory UI (I key) shows player items
- Sound effects and ambient audio (if feasible)
- Game balance pass: resource quantities, NPC difficulty, timing
- Edge case handling and stability

**Unit tests**: HUD value calculations, tooltip trigger conditions, menu state
transitions, health/hunger/energy drain rates.

**Integration tests — implement these exact scenarios:**

1. **HUD displays correct initial values**: Start a new game. Verify the HUD
   is visible. Verify health bar shows 100%. Verify hunger bar shows 100%.
   Verify energy bar shows 100%. Verify the crosshair is present at screen
   centre. Verify the hotbar is visible at the bottom of the screen.

2. **Health bar updates on damage**: Set player health to 100. Apply 25 damage
   to the player. Verify the health bar now displays 75%. Apply 75 more damage.
   Verify the health bar displays 0%. Verify a death/game-over state is triggered.

3. **Hunger decreases over time**: Set hunger to 100. Advance the simulation for
   the equivalent of 5 in-game minutes. Verify hunger has decreased below 100.
   Verify the hunger bar visually reflects the new value.

4. **Energy decreases with actions**: Set energy to 100. Perform 20 punch actions.
   Verify energy has decreased. Verify the energy bar reflects the new value.
   Stop all actions and advance 300 frames. Verify energy has partially recovered.

5. **Opening sequence plays on new game**: Start a new game from the main menu.
   Verify the text "it's time to learn to survive on your own" is displayed.
   Verify the camera is positioned to look at the park. Advance 180 frames (3
   seconds). Verify the text has faded/dismissed. Verify the player now has
   movement control.

6. **Pause menu opens and pauses game**: During gameplay, press ESC. Verify the
   pause menu is visible with "Resume" and "Quit" options. Verify the game time
   is NOT advancing (record time, wait 60 frames, verify time unchanged). Verify
   NPCs are not moving (record positions, wait 60 frames, verify unchanged).
   Select "Resume". Verify the pause menu is hidden and game time resumes.

7. **Help UI shows all controls**: Press H during gameplay. Verify the help UI
   is visible. Verify it contains text for ALL of the following keys: W, A, S, D,
   I, H, C, E, ESC, left click, right click, 1-9. Verify player movement is
   disabled while help is open. Press H again. Verify help UI closes and movement
   is re-enabled.

8. **Inventory UI reflects actual inventory**: Give the player 5 WOOD and 3 BRICK.
   Press I to open inventory. Verify the inventory UI shows WOOD with quantity 5.
   Verify the inventory UI shows BRICK with quantity 3. Close inventory (press I).
   Break a TREE_TRUNK block (5 punches). Open inventory again. Verify WOOD now
   shows quantity 6.

9. **Tooltip queue processes in order**: Trigger 3 tooltips in rapid succession
   (e.g. first tree punch, first jeweller, first police encounter by setting up
   all three conditions). Verify all 3 tooltips display in the order they were
   triggered. Verify each tooltip is visible for at least 60 frames before the
   next one appears. Verify no tooltips are dropped.

10. **Full game loop stress test**: Start a new game. Advance through the opening
    sequence. Move the player around for 600 frames. Break 3 blocks. Open and
    close inventory. Open and close help. Open and close crafting menu. Pause and
    resume. Advance time to night. Let police spawn. Advance 300 more frames.
    Verify NO null pointer exceptions, no crashes, and the game state is consistent
    throughout (health/hunger/energy are valid numbers, player position is valid,
    all UI elements respond correctly).

---
---

## Phase 9: CODE REVIEW

**Goal**: Comprehensive code review of the entire codebase. This phase runs after
all 8 implementation phases are complete.

Review the entire codebase file by file and assess the following:

1. **Correctness**: Does the code implement what SPEC.md describes? Walk through
   every integration test scenario in Phases 1-8 and verify that the test code
   actually tests what the scenario describes (not a watered-down version of it).
   If any test is too lenient, too vague, or doesn't match the spec, fix it.

2. **Wiring**: Are all systems actually connected? Trace the path from game start
   to gameplay. Verify that: the main menu leads to gameplay, the HUD renders over
   the 3D scene, the inventory UI opens and closes, tooltips fire, NPCs spawn and
   behave, police appear at night, council builders arrive for large structures.
   If any system is implemented but not wired into the game loop, wire it up.

3. **Dead code**: Remove any code that is never called, any stub implementations
   that were never filled in, any TODO comments that were never addressed.

4. **Error handling**: Ensure there are no swallowed exceptions, no silent failures.
   If something goes wrong the game should log it clearly, not crash silently.

5. **Test coverage**: Run `./gradlew test`. If any tests fail, fix them. If any
   phase's integration tests are missing or incomplete compared to the spec, add
   them. Every single integration test scenario listed in Phases 1-8 must have a
   corresponding test that passes.

6. **Build cleanliness**: Run `./gradlew build`. Fix any warnings. Ensure the
   build is clean with zero warnings and zero errors.

**Commit all fixes from this phase on the `main` branch before proceeding.**

---

## Phase 10: CRITIC (Infinite Loop)

**This phase loops forever. The game is never complete. The CRITIC always has
something to say.**

### How the CRITIC works

After CODE REVIEW is complete on `main`, enter the CRITIC loop:

#### Step 1: Branch

Create a new branch from the current branch:
- First iteration: `git checkout -b critic1` (from `main`)
- Second iteration: `git checkout -b critic2` (from `critic1`)
- Third iteration: `git checkout -b critic3` (from `critic2`)
- And so on: `criticN` always branches from `critic(N-1)`

#### Step 2: CRITIC Review

Put on a different hat. You are no longer the PM — you are a **game critic** and
**player advocate**. You are reviewing a playable build of Ragamuffin. Consider:

**Gameplay feel:**
- Is the game actually fun to play? Or is it technically correct but boring?
- Is the pacing right? Does the player have enough to do in the first 5 minutes?
- Are the survival mechanics (health, hunger, energy) interesting or just annoying?
- Is the difficulty curve fair? Does the player have a fighting chance against
  police and council builders?

**Tone and humour:**
- Do the tooltips actually land? Are they funny or just informational?
- Does the world feel like a real British town or a generic game level?
- Are the NPC interactions memorable? Do they have personality?
- Is the overall vibe "darkly funny" or just "dark"?

**Polish and juice:**
- Does the game feel responsive? Do actions have satisfying feedback?
- Are there visual or audio cues that make the world feel alive?
- Is the UI intuitive? Would a new player understand what to do?
- Are there any moments of delight or surprise?

**Missing features:**
- What obvious features are missing that would make the game better?
- What would make a player want to keep playing for another 30 minutes?
- Are there emergent gameplay possibilities that could be unlocked with
  small additions?

#### Step 3: Write Improvement Phase

Based on the CRITIC review, append a new phase to SPEC.md:

```
## Phase 11: CRITIC 1 Improvements — [Title]
(or Phase 12, 13, etc. for subsequent iterations)
```

This phase must include:
- A summary of the CRITIC's findings
- Specific, concrete improvements to implement
- Explicit integration test scenarios for each improvement (same format as
  Phases 1-8: exact setup, actions, expected outcomes)
- The improvements should be scoped to be achievable in a single development
  session — don't try to rewrite the entire game

**Remember: you may only ADD to SPEC.md, never remove or weaken existing specs.**

#### Step 4: Implement

Implement the improvement phase using TDD, exactly as with Phases 1-8.
Delegate all coding to the coding agent. Verify all tests pass.

#### Step 5: Code Review

Run a mini code review on the changes made in this iteration:
- All new tests pass
- No existing tests broken
- `./gradlew build` is clean
- New code is wired into the game loop (not orphaned)

Commit all work on the current `criticN` branch.

#### Step 6: Loop

Go back to Step 1. Create `critic(N+1)` from `criticN`. Run the CRITIC again.
Find the next set of improvements. Implement them. Review. Commit. Repeat.

**This loop never terminates.** There is always something to improve. If you
genuinely cannot think of a single improvement, you are not thinking hard enough.
Play the game (mentally simulate a play session), read the code, re-read the spec,
consider the player experience. Something can always be better.

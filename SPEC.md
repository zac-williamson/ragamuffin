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
- **Left click / punch**: Break block or hit NPC (hits required vary by material: soft=5, hard=8, fragile=2)
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

- Block breaking: look at a block, left click/punch to hit it. Hits required vary by material hardness:
  - **Soft** (TREE_TRUNK, LEAVES, GRASS): 5 hits
  - **Hard** (BRICK, STONE, PAVEMENT): 8 hits
  - **Fragile** (GLASS): 2 hits
  - Default for unlisted types: 5 hits
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
   a BRICK block. Simulate 8 punch actions (BRICK is a hard block requiring 8 hits).
   Verify the block is removed and the inventory contains exactly 1 BRICK item.

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

11. **Block hit counter resets when block is replaced**: Place a TREE_TRUNK block
    at position (5, 1, 5). Punch it 3 times (not enough to break it — it requires
    5). Simulate an external removal of the block via `world.setBlock(5, 1, 5,
    BlockType.AIR)` (as done by council builder NPCs via `demolishBlock()`).
    Place a new TREE_TRUNK block at (5, 1, 5). Punch it 4 times. Verify the block
    is still present (it has NOT broken — the old hit counter must not have carried
    over). Punch it a 5th time. Verify the block is now broken and the inventory
    contains exactly 1 WOOD item.

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

   Additionally: give the player only DIAMOND (no WOOD). Advance the simulation until
   the youth is adjacent (max 300 frames). Verify the player's DIAMOND count has
   decreased by at least 1 — the theft system must not be hard-coded to only steal WOOD.
   The gang should steal the most valuable item available (priority: DIAMOND > SCRAP_METAL >
   WOOD > food), or a random item from the non-empty inventory if no priority item is found.

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

8. **Arrest respawn places player above solid terrain**: Generate a world. Record the
   terrain-aware spawn height at (0, 0) via `calculateSpawnHeight(world, 0, 0)`.
   Call `ArrestSystem.arrest(player, inventory)`. Verify the player's Y coordinate
   after arrest is greater than or equal to `calculateSpawnHeight(world, 0, 0)`.
   Verify the block directly below the player's feet (at `floor(player.y - 0.01)`)
   is solid, not AIR — i.e. the player is standing on solid ground, not floating
   inside the terrain or in the void. A hardcoded y=1 fails this test when the park
   centre terrain is higher than y=1.

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

## Phase 8b: The Pub — NPC Rumour Network & Social Economy

**Goal**: Add a fully interactive pub landmark to the world, complete with a
living NPC rumour network. The pub is the social heart of the neighbourhood —
a place where information, gossip, and shady opportunities flow as freely as
the warm flat lager.

### Pub Building & Layout

- The pub is a distinct landmark generated by the WorldGenerator, placed near
  the park. It is a two-storey brick building with a hand-painted sign prop
  reading "The Ragamuffin Arms". It has an interior accessible through a door.
- Interior contains: a bar counter (solid block strip), bar stools (prop), a
  dartboard prop on one wall, fruit machine prop, and a few tables with chairs.
- A `BARMAN` NPC stands behind the bar at all times (does not wander). He wears
  a distinct apron colour. Pressing **E** while facing him opens a trade menu.

### Buying Drinks

- The trade menu lists drinks purchasable with in-game currency (scavenged coins
  dropped by certain NPCs or found in containers):
  - **Pint of lager** — costs 2 coins. Effect: loosens nearby NPC tongues (see
    Rumour system below). Tooltip on first purchase: "A pint always helps the
    conversation."
  - **Double whisky** — costs 3 coins. Effect: player movement speed temporarily
    reduced by 20% for 10 seconds (drunk effect), but all nearby NPCs in the pub
    become willing to share rumours for 30 seconds.
  - **Cup of tea** — costs 1 coin. Effect: restores 10 energy. British.
- Purchased drinks are consumed immediately (not stored in inventory). Coins are
  a new `Material` enum entry `COIN` that drops from defeated hostile NPCs
  (youth gang members drop 1-3 coins, police drop 0).

### NPC Rumour Network

Every NPC in the world carries a small rumour buffer (up to 3 rumours). Rumours
are facts about the world state that NPCs gossip to each other as they wander
past. The `RumourNetwork` class manages this system.

**Rumour types** (enum `RumourType`):

| Type | Content template | Generated when |
|------|-----------------|----------------|
| `PLAYER_SPOTTED` | "Someone's been causing bother near [landmark]" | Player commits a crime (hits NPC, breaks a building block) within 30 blocks of a landmark |
| `LOOT_TIP` | "I heard there's good stuff inside [landmark]" | Generated at world-gen time for office building and jeweller |
| `GANG_ACTIVITY` | "The [gang name] boys are running [territory name] now" | Generated when GangTerritorySystem reaches HOSTILE state |
| `QUEST_LEAD` | "You might want to talk to someone at [landmark]" | Generated when a quest becomes available at that landmark |
| `COUNCIL_NOTICE` | "The council's sending someone round about those buildings" | Generated when council demolition threshold is crossed |

**Rumour spreading mechanics:**

- Each NPC holds at most 3 rumours. When two NPCs pass within 4 blocks of each
  other, they exchange their most recent rumour (one way, randomly chosen NPC is
  the speaker). This happens at most once per pair per 60 seconds.
- Rumours have a `hops` counter, starting at 0 and incrementing with each
  spread. After 5 hops a rumour expires and is removed from the holder's buffer.
- The `BARMAN` NPC is a rumour sink: NPCs entering the pub will always share
  their rumours with him. The barman accumulates up to 10 rumours (never
  expires them). He is the most well-informed NPC in the world.

**Player extracting rumours:**

- Pressing **E** on any NPC (not just the barman) outside combat shows a short
  speech line. If the NPC holds a rumour AND the player has bought a drink in
  the last 60 seconds (tracked by `drunkTimer`), the NPC also speaks their top
  rumour aloud (displayed in the speech log and as a floating text pop-up over
  their head). The rumour is not removed from the NPC — it keeps spreading.
- Pressing **E** on the **barman** always shows his top rumour (no drink
  required), because he's professionally chatty. Each time the player talks to
  the barman he cycles to his next rumour.
- Tooltip on first rumour received: "Gossip travels fast in this town."

### Wanted Level Integration

- If the player's `CriminalRecord` has more than 3 offences, NPCs in the pub
  will refuse to share rumours ("I don't want any trouble, mate.") unless the
  player buys a double whisky first — the social lubricant overcomes distrust.
- A new `BOUNCER` NPC stands at the pub entrance. If the player has 5 or more
  offences on their criminal record, the bouncer blocks entry ("Not tonight,
  mate."). The player can bribe the bouncer with 5 coins to gain entry.

### Fruit Machine Mini-game

- Interacting (**E**) with the fruit machine prop costs 1 coin and triggers a
  simple RNG outcome displayed as three spinning emoji-like character symbols
  in the speech log:
  - Three matching symbols (1-in-27 chance): win 9 coins.
  - Two matching symbols (6-in-27 chance): win 2 coins.
  - No match: lose the coin.
- Tooltip on first use: "The house always wins. Probably."

**Unit tests**: RumourNetwork spreading logic, hop expiry, barman accumulation,
coin drop table, drink effect timers, bouncer block/bribe logic, fruit machine
RNG distribution.

**Integration tests — implement these exact scenarios:**

1. **Pub exists and is accessible**: Generate the world. Verify a landmark of
   type `PUB` exists. Verify a `BARMAN` NPC is present inside the pub building.
   Place the player at the pub entrance. Simulate pressing W to enter. Verify
   the player's position is inside the pub AABB. Verify the player did NOT pass
   through any wall (collision intact).

2. **Buying a pint costs coins and sets drunkTimer**: Give the player 5 coins.
   Place the player facing the barman. Press **E** to open the trade menu.
   Select "Pint of lager". Verify the player now has 3 coins (2 spent). Verify
   `drunkTimer` is set to 60 (seconds). Verify the tooltip "A pint always helps
   the conversation." fires on the first purchase.

3. **NPC shares rumour after drink**: Create an NPC with a `LOOT_TIP` rumour.
   Place the NPC within 3 blocks of the player. Set `drunkTimer` to 30 (active).
   Press **E** on the NPC. Verify the rumour text appears in the speech log.
   Verify the rumour is still present on the NPC (not consumed).

4. **NPC withholds rumour without drink**: Create an NPC with a rumour. Place
   the NPC within 3 blocks of the player. Ensure `drunkTimer` is 0. Press **E**.
   Verify the NPC's speech line does NOT include the rumour text.

5. **Rumour spreads between NPCs**: Create two NPCs, NPC-A holding a
   `PLAYER_SPOTTED` rumour (hops=0) and NPC-B with no rumours. Place them 3
   blocks apart. Advance 10 simulation frames. Verify NPC-B now holds the
   rumour. Verify the rumour's hop count is 1.

6. **Rumour expires after 5 hops**: Create a rumour with hops=4. Trigger one
   more spread event. Verify the hop count becomes 5. Advance one more frame.
   Verify the rumour has been removed from the holder's buffer (expired).

7. **Barman accumulates rumours from visiting NPCs**: Create 3 NPCs, each
   holding a different rumour. Move each NPC to within 3 blocks of the barman
   (simulate entering the pub). Verify the barman's rumour buffer contains all
   3 rumours.

8. **Bouncer blocks entry when criminal record >= 5 offences**: Set the
   player's criminal record to 5 offences. Place the player at the pub
   entrance facing the bouncer. Press **E**. Verify the bouncer's speech says
   "Not tonight, mate." Verify the player cannot enter (advance 60 frames of
   pressing W, verify player position is outside the pub AABB).

9. **Bribe bouncer with 5 coins grants entry**: Set criminal record to 5
   offences. Give the player 5 coins. Press **E** on the bouncer and select
   bribe. Verify the player now has 0 coins. Verify subsequent W presses move
   the player into the pub (bouncer no longer blocks).

10. **Fruit machine RNG — guaranteed win on rigged seed**: Construct the fruit
    machine's RNG with a known seed that produces three matching symbols. Give
    the player 1 coin. Interact with the machine. Verify the player now has 9
    coins (won 9, net +8). Verify the result is displayed in the speech log.

---
---

## Phase 8c: British Weather Survival

**Goal**: Make the weather system matter. British weather isn't mere atmosphere —
it's an active antagonist. Rain, biting cold, choking fog, and the occasional
freakish heatwave impose real survival costs on the player and reshape NPC
behaviour, forcing the player to scavenge clothing, build shelters, and read
the social landscape of the street as conditions change.

### Weather States (extend existing `Weather` enum)

| Weather | Description |
|---------|-------------|
| `CLEAR` | Sunny; no penalty |
| `OVERCAST` | Grey sky; mild cold |
| `DRIZZLE` | Light rain; minor wetness accumulation |
| `RAIN` | Heavy rain; fast wetness accumulation, cold drain |
| `THUNDERSTORM` | Torrential rain + lightning; danger outdoors |
| `FOG` | Visibility reduced to ~12 blocks (fog render distance clamp) |
| `HEATWAVE` | Rare; causes dehydration instead of cold |
| `FROST` | Early morning; ground icy (movement 15% slower outdoors) |

Transition probabilities reflect British weather: CLEAR is rare, OVERCAST is
the baseline, RAIN and DRIZZLE are frequent. THUNDERSTORM and HEATWAVE are
uncommon; FROST only occurs between 00:00–08:00.

### Player Survival Stats

Two new survival bars on the HUD (alongside health/hunger/energy):

- **Warmth** (0–100, starts at 80): drains when player is outdoors in cold/wet
  weather; restored by shelter, fire, or warm clothing. Below 20: player takes 1
  damage every 10 seconds and speed is reduced 20%. Tooltip on first drain:
  "It's brass monkeys out here."
- **Wetness** (0–100, starts at 0): rises in rain; dries gradually in shelter.
  High wetness accelerates warmth drain. Above 80: screen edges show a subtle
  water-drip vignette effect. Tooltip on first rain encounter: "Classic British
  summer."

### Clothing & Warmth Items

New `Material` entries for clothing dropped by specific NPCs or found in the
charity shop:

| Item | Source | Warmth bonus | Notes |
|------|--------|--------------|-------|
| `COAT` | Charity shop loot, defeated COUNCIL_BUILDER | +25 warmth/min when worn | Equip via inventory; shown as worn in first-person arm |
| `UMBRELLA` | Charity shop loot | Halves wetness gain while held | Held in off-hand; blocks can't be placed while open |
| `WOOLLY_HAT` | Charity shop loot | +10 warmth/min | Stacks with coat |
| `FLASK_OF_TEA` | Crafted (1 WOOD + 1 COIN via crafting menu) | +30 warmth, instant | Single use; tooltip: "Never underestimate a hot flask." |

Clothing is equipped via the inventory screen (new "Worn" slots alongside
hotbar). Worn items visibly change the first-person arm render colour.

### Fire Building

- New block type `CAMPFIRE` (crafted from 3 WOOD in the crafting menu).
- Placing a CAMPFIRE block ignites it: it emits an orange-yellow flickering
  point light (dynamic lighting, 6-block radius) rendered via LibGDX's
  environment system.
- Player within 4 blocks of a lit CAMPFIRE gains +40 warmth/min.
- Fire spreads to adjacent WOOD blocks after 120 seconds (prevents accidental
  building fires: only spreads to crafted WOOD items, not world WOOD blocks in
  tree trunks or building frames).
- Police treat an outdoor CAMPFIRE as a public nuisance after 60 seconds (same
  escalation as large player structures).
- Rain extinguishes a CAMPFIRE after 30 seconds of exposure.
- Tooltip on first campfire placement: "Nothing like a fire in the park. Very
  festive. Very illegal."

### NPC Weather Reactions

NPC behaviour changes based on current weather, making the world feel alive:

- **DRIZZLE / RAIN**: Pedestrian NPCs reduce walking speed 20% and cluster
  under awnings (a 1-block overhang of any solid block) and bus-shelter props.
  Jogger NPCs leave the park entirely (return to nearest building).
- **THUNDERSTORM**: All non-hostile NPCs run to the nearest building/doorway
  and remain there until the storm ends. POLICE NPCs reduce patrol frequency
  by 50% ("even coppers don't fancy it"). Hostile YOUTH NPCs become MORE
  aggressive (they embrace the chaos).
- **FOG**: Police NPCs' line-of-sight detection range halved. Stealth approach
  distance also halved — good news for the player's criminal endeavours.
- **HEATWAVE**: NPCs congregate in the park around the pond. DRUNK NPCs
  multiply. The pub sees a 50% increase in NPC traffic. Player dehydration
  replaces coldness: Warmth stat replaced by **Hydration** (drain rate doubled
  when above 80 game-temperature).
- **FROST**: NPCs walk slower. Black ice patches (invisible FROST_ICE block
  variant) are placed on ROAD and PAVEMENT blocks at world-gen; players crossing
  them have a 10% chance per step of slipping (brief movement direction
  randomised for 0.5 seconds). NPC cars skid (Car velocity briefly deviates).

### Weather Forecast Rumours

The RumourNetwork gains weather-forecast rumours:
- A new `RumourType.WEATHER_TIP` is seeded hourly by the TimeSystem: "Heard
  there's a storm coming this afternoon" / "BBC says it'll clear up by teatime."
- Barman always has the most current forecast rumour (flavour text only, no
  gameplay effect beyond informing the player to prepare).

### Visual Effects

- **Rain particle system**: vertical streaks rendered as thin billboard quads,
  density scales with weather state (DRIZZLE < RAIN < THUNDERSTORM). Rain
  splashes on ground blocks (small circular ripple sprite, 0.3 second lifetime).
- **Fog**: LinearFog applied to ModelBatch environment. Distance clamps to 12
  blocks during FOG state; fades to normal over 5 seconds on transition.
- **Lightning**: During THUNDERSTORM, random flashes of white ambient light
  every 15–45 seconds (random duration 0.1–0.2 s). Accompanied by a delayed
  thunder sound effect (1–3 seconds after flash, simulating distance).
- **Frost sparkle**: PAVEMENT and ROAD blocks during FROST state shimmer with
  a faint white specular highlight in the chunk texture.
- **Campfire flicker**: point light intensity oscillates ±15% using a sine
  wave at ~2 Hz, giving convincing flame flicker without a particle system.

**Unit tests**: Warmth/wetness drain rates under each weather state, clothing
warmth bonus calculations, campfire radius warmth gain, NPC weather behaviour
state transitions, black ice slip probability, fire spread timer, fog distance
clamp, rain particle count per weather tier.

**Integration tests — implement these exact scenarios:**

1. **Warmth drains outdoors in rain**: Set weather to RAIN. Place player
   outdoors (no overhead solid block within 3 blocks above). Record warmth
   (should be 80). Advance 300 frames (5 seconds). Verify warmth has decreased
   below 80. Verify the warmth HUD bar is visible and reflects the new value.

2. **Shelter stops warmth drain**: Set weather to RAIN. Place player in a
   building interior (overhead solid blocks present). Set warmth to 50. Advance
   300 frames. Verify warmth has NOT decreased (shelter prevents drain). Verify
   warmth has increased slightly (passive recovery indoors).

3. **Campfire restores warmth**: Craft a CAMPFIRE and place it outdoors. Set
   weather to OVERCAST (no rain extinguishing). Set player warmth to 30. Place
   player within 4 blocks of the campfire. Advance 120 frames (2 seconds). Verify
   warmth has increased above 30. Verify the campfire block emits a light (the
   environment's point lights list is non-empty).

4. **Coat reduces warmth drain**: Give the player a COAT item and equip it.
   Set weather to RAIN. Record warmth. Advance 300 frames outdoors. Verify warmth
   has drained LESS than it would without a coat (compare against unequipped
   baseline from test 1). Verify the HUD shows the coat as equipped.

5. **Rain extinguishes campfire**: Place a CAMPFIRE block outdoors. Verify it
   is lit (light emitting). Set weather to RAIN. Advance 1800 frames (30 seconds
   at 60fps). Verify the campfire block is no longer lit (no longer emitting
   light). Verify a tooltip or speech event indicates the fire was extinguished.

6. **NPCs shelter under awnings in rain**: Generate the world. Set weather to
   RAIN. Record NPC positions. Identify at least one awning structure (solid
   block with air below, adjacent to a building). Advance 600 frames. Verify
   at least 2 pedestrian NPCs are now positioned under the awning or inside a
   building (not standing in open rain). Verify YOUTH NPCs have NOT sheltered
   (they remain outdoors).

7. **Fog reduces police LoS range**: Set weather to FOG. Get the police NPC's
   normal sight range. Verify the police LoS detection range is halved compared
   to CLEAR weather. Place player at exactly (normal_range - 1) blocks from
   police. Verify police do NOT detect the player during fog. Set weather to
   CLEAR. Verify the police now detect the player at the same distance.

8. **Frost ice causes player slip**: Generate a world. Set weather to FROST.
   Verify black ice patches are placed on at least 5 ROAD blocks. Place the
   player on a black ice block. Simulate 60 step frames. Verify at least 1 slip
   event occurred (player's actual movement direction deviated from input
   direction for at least 1 frame).

9. **Weather forecast rumour is seeded**: Advance the TimeSystem by 1 in-game
   hour. Verify a `WEATHER_TIP` rumour has been added to at least one NPC's
   rumour buffer. Advance to the pub. Press **E** on the barman. Verify his
   speech includes a weather forecast string.

10. **Full weather cycle stress test**: Start a new game. Force weather through
    all 8 states in sequence (CLEAR → OVERCAST → DRIZZLE → RAIN → THUNDERSTORM
    → FOG → HEATWAVE → FROST). For each state: advance 120 frames, verify no
    null pointer exceptions, verify NPC count is unchanged from pre-transition,
    verify player warmth/wetness are valid numbers (0–100), verify the HUD is
    still rendering. After all 8 states, verify the game remains in PLAYING
    state with no crashes.

---

## Phase 8d: Dynamic Faction War & Turf Economy

**Goal**: Turn the gang territory system into a living, breathing turf war that
the player can meaningfully influence — playing factions off against each other,
running protection rackets, brokering truces, or simply watching the neighbourhood
tear itself apart. Every decision has lasting consequences rumoured across the
entire world. This is the glue that ties rumours, reputation, weather survival,
the pub, and the black market into a single, emergent narrative engine.

### Factions

Three factions compete for control of the neighbourhood, each with distinct
territory, personality, and economy. Faction state is tracked in a new
`FactionSystem` class.

| Faction | Territory | Tone | Primary income |
|---------|-----------|------|----------------|
| **The Marchetti Crew** | Industrial estate + off-licence | Organised, businesslike | Protection money from shopkeepers |
| **Street Lads** | Park + housing estate | Chaotic, opportunistic | Petty theft, drug corners |
| **The Council** | Town hall + office block | Bureaucratic, passive-aggressive | Demolition contracts, planning notices |

Each faction has a **Respect** score (0–100) toward the player, starting at 50.
Actions raise or lower it. Respect determines what interactions, missions, and
trade bonuses are available.

### Turf Blocks & Territory Map

- Every ROAD and PAVEMENT block within a faction's territory has an invisible
  `ownerFaction` tag tracked in a `TurfMap` (a parallel 2D int array over the
  world grid, indexed by X/Z at ground level).
- Contested turf (two factions both claim it) is visually indicated by a small
  spray-paint prop (a `PropType.GRAFFITI_TAG`) placed on the nearest wall block.
  Prop colours differ per faction.
- When a faction's Respect toward the player drops below 20, they mark the
  player as an enemy: their NPCs become hostile on sight.
- When Respect is above 75, the faction marks the player as an ally: their NPCs
  greet the player (press **E** to receive a faction-specific tooltip) and
  offer faction-exclusive trade or mission.

### Player Actions that Affect Turf

| Action | Effect |
|--------|--------|
| Break blocks inside a rival faction's building | Rival faction Respect −10, owning faction Respect +5 |
| Hit a faction NPC | That faction Respect −15 |
| Complete a faction mission (see below) | Faction Respect +20, rival Respect −10 |
| Buy a round at the pub (5 coins via barman) | All factions Respect +2 (neutral goodwill) |
| Place graffiti block in rival territory | Rival Respect −5, "own" faction Respect +8 |
| Get arrested near faction territory | Nearest faction Respect +3 ("not our problem, mate") |

Turf ownership shifts when a faction's Respect advantage over a rival exceeds 30
points: the `TurfMap` transfers 10% of contested blocks to the dominant faction,
and new graffiti props appear/disappear accordingly.

### Faction Missions

Each faction offers one procedurally-selected mission at a time, picked from a
small pool. Missions are surfaced through the rumour network (the BARMAN always
knows what's going on) or by pressing **E** on a faction lieutenant NPC.

**Marchetti Crew missions (pool of 3):**
1. **Delivery Run** — Carry a PETROL_CAN from the industrial estate to a drop
   point at the off-licence within 3 in-game minutes. Reward: 8 coins + Respect +20.
2. **Eviction Notice** — Break 10 blocks of the Street Lads' park shelter.
   Reward: 6 coins + Respect +25.
3. **Quiet the Witness** — Hit a specific NPC (a WITNESS type spawned for this
   mission) 3 times before they reach the police station (300 blocks away).
   Reward: 10 coins + Respect +30.

**Street Lads missions (pool of 3):**
1. **Corner Defence** — Prevent any Marchetti NPC from crossing a specific road
   intersection for 2 in-game minutes (block physically by standing there or
   hitting any who approach). Reward: 5 coins + Respect +20.
2. **Office Job** — Steal 3 COMPUTER items from the office building without
   getting arrested. Reward: 8 coins + Respect +25 + stealth boost rumour seeded.
3. **Tag the Turf** — Place 5 GRAFFITI blocks on Marchetti-owned walls.
   Reward: 6 coins + Respect +20.

**Council missions (pool of 3) — delivered via planning notice prop on player shelter:**
1. **Voluntary Compliance** — Demolish your own structure (> 20 blocks) to avoid
   a fine. Reward: avoid −20 Warmth penalty, Respect +15 with Council.
2. **Report a Nuisance** — Press **E** near an active Street Lads drug corner NPC
   to "report" them. Reward: Council Respect +20, Street Lads Respect −15.
3. **Clear the Encampment** — Destroy 20 CARDBOARD blocks in the park area.
   Reward: 10 coins + Council Respect +25.

Missions expire after 5 in-game minutes (a new one is selected). Failing a
mission (time out or getting caught) reduces Respect by 10.

### Faction-State Broadcast via Rumour Network

Every time turf ownership shifts or Respect crosses a threshold, a
`RumourType.GANG_ACTIVITY` rumour is seeded into 3 random nearby NPCs:

- Turf gain: "The [faction] are taking over [territory name] now."
- Respect threshold 75 crossed: "I hear [faction] are looking for someone reliable."
- Respect threshold 20 crossed: "You'd best avoid [faction] territory for a while."
- Mission completed: "Someone sorted out [problem] for the [faction]."

The barman always has the most current faction rumour. This means the player can
walk into the pub and immediately get a read on the current state of the turf war.

### Faction Endgame States

If one faction reaches Respect 90+ with the player AND controls > 60% of the
turf map, a **Faction Victory** event fires:

- **Marchetti Victory**: The off-licence becomes a permanent shop (infinite
  coins for sold items). Industrial estate produces double loot. Council leaves
  player structures alone (Marchetti bribed them).
- **Street Lads Victory**: Park becomes a permanent safe zone (no police patrols
  inside). All drug-corner NPCs drop 2× coins. Youth NPCs stop attacking player.
- **Council Victory**: All player structures auto-demolished. Police spawn rate
  doubled. BUT: player receives a `COUNCIL_ID` item that lets them walk past
  police undetected (diplomatic immunity, British-style).

If all three factions drop below Respect 30 with the player simultaneously, a
**Everyone Hates You** state fires: every NPC faction is hostile, but the black
market fence increases buy prices by 50% (chaos is good for business). A rumour
seeds: "That one's gone proper feral."

### HUD Integration

- A small **faction status strip** appears below the hotbar: three coloured
  bars (one per faction) showing current Respect (0–100). Bars pulse briefly
  when Respect changes.
- Tooltip on first Respect change: "Choose your friends wisely round here."
- Tooltip on first faction mission received: "Everyone wants something."

**Unit tests**: Turf ownership transfer logic, Respect delta calculations per
action, mission timer expiry, faction victory condition detection, graffiti prop
placement/removal, rumour seeding on turf shift.

**Integration tests — implement these exact scenarios:**

1. **Respect decreases on NPC hit**: Set Marchetti Crew Respect to 50. Punch a
   Marchetti NPC once. Verify Marchetti Respect is now 35. Verify a
   `GANG_ACTIVITY` rumour has been seeded into at least one nearby NPC within
   30 blocks.

2. **Turf transfers when Respect gap exceeds 30**: Set Marchetti Respect to 80,
   Street Lads Respect to 40. Verify a contested block currently owned by Street
   Lads in a Marchetti zone transfers to Marchetti within 60 frames. Verify a
   GRAFFITI_TAG prop appears on the nearest wall block of the transferred turf.

3. **Faction mission delivery run succeeds**: Spawn a Marchetti lieutenant NPC.
   Press **E** on the lieutenant to receive the Delivery Run mission. Give the
   player a PETROL_CAN. Move player to the drop point within 3 in-game minutes.
   Verify Marchetti Respect increased by 20. Verify the player received 8 coins.
   Verify a mission-completion rumour was seeded.

4. **Faction mission expires on timeout**: Spawn a Street Lads mission (Corner
   Defence, 2 min timer). Advance the simulation by 3 in-game minutes without
   completing the mission. Verify Street Lads Respect decreased by 10. Verify
   the mission is no longer active (no active mission for Street Lads). Verify
   a new mission from the pool has been selected.

5. **Council Victory fires at 60% turf control**: Set Council Respect to 90.
   Programmatically set the TurfMap so Council controls 61% of blocks. Advance
   1 frame. Verify the Council Victory event has fired. Verify all player
   structures are scheduled for demolition. Verify the player has received a
   COUNCIL_ID item in their inventory.

6. **Everyone Hates You state activates**: Set all three faction Respect values
   to 25. Advance 1 frame. Verify all faction NPCs are hostile on sight (NPC
   state switches to HOSTILE when player enters 10-block radius). Verify the
   fence's buy price multiplier is 1.5 (50% increase). Verify the rumour "That
   one's gone proper feral." is seeded into at least 3 NPCs.

7. **Faction status HUD bars render correctly**: Set Marchetti Respect to 80,
   Street Lads to 40, Council to 60. Render one frame. Verify the faction
   status strip contains three coloured regions. Verify Marchetti bar width
   is proportionally larger than Street Lads bar. Verify no NPE during render.

8. **Buy a round raises all faction Respect**: Give player 5 coins. Place player
   facing barman. Buy a round (5 coins via E → "Buy a round"). Verify all three
   faction Respect values have increased by 2. Verify player now has 0 coins.

9. **Hostile faction attacks on sight**: Set Street Lads Respect to 15 (below
   20 threshold). Place a Street Lads NPC 8 blocks from the player. Advance
   60 frames. Verify the NPC's state is ATTACKING_PLAYER. Record NPC position
   at frame 0 and frame 60 — verify it has moved toward the player.

10. **Full turf war stress test**: Start a new game. Run 3 complete faction
    mission cycles (receive, complete, repeat for each faction). Force a turf
    transfer event. Force an "Everyone Hates You" state. Force a Faction Victory.
    Throughout: verify game remains in PLAYING state, no NPEs, NPC count is
    non-zero, HUD faction bars are visible and contain valid values (0–100),
    rumour network contains at least 1 active rumour at all times.

---

## Phase 8e: Street Legend — Notoriety, Criminal Career Progression & Escalating Police Response

**Goal**: Give the player a persistent criminal career arc that ties together every
existing system — factions, heists, rumours, the fence, the pub, the weather — into
a single escalating narrative spine. The player starts as a nobody nicking pasties
from Greggs and, through accumulated notoriety, can become the most dangerous
(and most wanted) person in the neighbourhood. Or get bang to rights and spend a
night in the cells. Very British. Very bleak.

### Notoriety Score & Street Legend Tiers

A new persistent `Notoriety` value (0–1000, starting at 0) accumulates from criminal
acts and feeds into five **Street Legend Tiers**. It is separate from the faction
Respect scores — Notoriety is about infamy, not loyalty.

| Tier | Notoriety range | Title | Police Response | Perks Unlocked |
|------|----------------|-------|-----------------|----------------|
| 0 | 0–99 | **Nobody** | PCSOs ignore you | None |
| 1 | 100–249 | **Local Nuisance** | PCSOs issue warnings; regular Police patrol your last known area | Fence offers 10% better prices |
| 2 | 250–499 | **Neighbourhood Villain** | Police patrol actively; Armed Response spawns after heists | Access to Marchetti Crew's top-tier missions |
| 3 | 500–749 | **Area Menace** | Armed Response Unit is always on patrol near player; helicopter flyover every 5 in-game minutes (noise, searchlight cone) | Black market sells CROWBAR, WIRE_CUTTERS; all factions will talk to you |
| 4 | 750–999 | **Urban Legend** | MI5-style SURVEILLANCE_VAN parked near your last shelter; all 3 factions offer alliance; fence prices peak | Can recruit 1 permanent NPC accomplice |
| 5 | 1000 | **The Ragamuffin** | Full lockdown: doubled police count, wanted posters appear as props on walls, the Barman dedicates a rumour slot permanently to your exploits | Achievement unlocked: "Ragamuffin" |

Notoriety **never decreases** from criminal acts — it is a one-way ratchet. It can
only be slightly reduced (−5 per tier) by bribing the fence with 20 coins ("keeping
things quiet") or by spending a night in the police cells (arrest, see below).

**Notoriety gain table:**

| Act | Notoriety gained |
|-----|-----------------|
| Breaking a block in someone's building | +2 |
| Pickpocketing an NPC (new interaction, see below) | +5 |
| Completing a heist (any target) | +30 |
| Completing a jeweller heist | +50 |
| Hitting a POLICE NPC | +20 |
| Getting arrested and escaping custody | +40 |
| Completing a faction mission (any) | +15 |
| Reaching a new faction Respect threshold (≥75) | +10 |
| Being mentioned in a rumour that reaches the Barman | +5 (once per rumour) |

### Escalating Police Response System

The existing `CriminalRecord` and `ArrestSystem` are extended to reflect the player's
Notoriety tier, making police behaviour meaningfully escalate.

**Tier 0–1: PCSOs (Community Support Officers)**
- Slow-walking NPCs with high-vis vests. They do NOT arrest the player — they issue
  a verbal warning ("Oi! Pack it in!") and add 1 offence to the criminal record.
  If ignored (player commits another offence within 60 seconds), they radio for Police.
- PCSO spawns: 1–2 per crime, wander to last known crime location.

**Tier 2: Regular Police**
- Existing POLICE NPC behaviour (patrol, arrest on sight if wanted). Now additionally:
  - After a heist alarm is tripped, 4 police converge on the heist target (as per
    Phase 8e heist spec — this confirms the wiring).
  - Police radios: when a Police NPC spots the player, all Police NPCs within 40
    blocks enter CHASING state simultaneously (coordination).

**Tier 3: Armed Response Unit (ARU)**
- New `NPCType.ARMED_RESPONSE` — moves faster than regular police, hits harder
  (3 hits to incapacitate player vs. 5 for regular police), and does NOT stop
  chasing at building boundaries (pursues indoors).
- ARU spawns only at Tier 3+ and only after a crime that triggers a wanted state.
  They despawn after 3 in-game minutes if they cannot locate the player.
- **Helicopter**: A non-physical audio + searchlight effect. Every 5 in-game
  minutes at Tier 3+, a directional `PointLight` sweeps across the ground in a
  cone pattern for 20 seconds (the "searchlight"). While the searchlight overlaps
  the player's position, a distinct audio cue plays and nearby NPCs report the
  player's position to the nearest ARU. Fog weather halves the searchlight range.

**Tier 4–5: Surveillance**
- `PropType.SURVEILLANCE_VAN` placed near the player's most-visited landmark
  (tracked by `NotorietySystem` counting block interactions per landmark zone).
  The van is a static prop — but pressing **E** on it reveals a tooltip:
  "They know where you sleep. Probably best to keep moving."
- **Wanted Posters**: At Tier 5, `PropType.WANTED_POSTER` props appear on walls
  near police-patrolled areas. These are cosmetic but seeded as rumours
  ("Have you seen the wanted poster by the off-licence?") in the `RumourNetwork`.

### New Mechanic: Pickpocketing

A new interaction unlocked at Tier 1. Press **F** while standing directly behind a
non-hostile NPC (within 1.5 blocks, NPC must not be facing the player — checked by
dot product of player-to-NPC vector against NPC facing direction).

- Success chance: 70% base, −20% if `NoiseSystem.noiseLevel > 0.3`, −30% if any
  other NPC is within 6 blocks (witness), +20% if weather is FOG.
- On success: random COIN drop (1–4 coins) added directly to player inventory.
  Notoriety +5. Tooltip on first success: "Easy money. For now."
- On failure: NPC turns around, shouts "Oi!" (speech bubble), adds 1 offence to
  criminal record, and enters FLEEING state (runs to nearest Police NPC). Notoriety
  +2 (you tried). Tooltip on first failure: "Butter fingers."
- Each NPC can only be successfully pickpocketed once per in-game hour (cooldown
  tracked on the NPC instance). Attempting on cooldown always fails silently (no
  offence added — the NPC just noticed nothing).

**Key binding**: **F** — Pickpocket (added to Help UI and key reference).

### NPC Accomplice Recruitment (Tier 4+)

At Tier 4 (Urban Legend), the player can recruit one permanent NPC accomplice.
The mechanic:

1. Press **E** on any non-hostile `STREET_LAD` or `YOUTH` NPC when Tier ≥ 4 and
   player has ≥ 10 coins. A dialogue option "Want to make some money?" appears.
2. The NPC costs 10 coins to recruit. They become `NPCState.FOLLOWING_PLAYER` and
   their `NPCType` changes to `ACCOMPLICE`.
3. The accomplice follows the player (simple follow AI: move toward player if
   distance > 3 blocks, stop if distance < 2).
4. During a heist (while `HeistSystem` phase == EXECUTION): the accomplice
   automatically moves toward the heist target's safe location and reduces the
   safe-cracking hold-E time from 8 seconds to 5 seconds.
5. If the accomplice is hit by police 3 times, they flee and are lost. A new one
   can be recruited. Speech bubble on loss: "Every man for himself, mate!"
6. Only 1 accomplice at a time. Tooltip on first recruitment: "Don't get attached."

### Notoriety HUD Element

A new HUD element: a small "WANTED" star-cluster in the top-right corner (inspired
by the British tradition of tabloid front pages). It shows 0–5 filled stars
corresponding to the current Tier. Stars fill up with a brief flash animation when
Notoriety crosses a tier threshold. At Tier 5, the stars pulse red permanently.

The HUD element also displays the player's current **Street Legend Title** in small
text below the stars (e.g. "AREA MENACE").

### Achievements Integration

New achievements triggered by this system (added to `AchievementType`):

| Achievement | Trigger |
|-------------|---------|
| `FIRST_PICKPOCKET` | Successfully pickpocket for the first time |
| `LOCAL_NUISANCE` | Reach Notoriety Tier 1 |
| `SURVIVED_ARU` | Escape from 3 consecutive ARU pursuits |
| `RAGAMUFFIN` | Reach Notoriety Tier 5 (Notoriety = 1000) |
| `KEEPING_IT_QUIET` | Bribe the fence to reduce notoriety 3 times |
| `THE_CREW` | Successfully complete a heist with an accomplice |

**Unit tests**: Notoriety gain per action, tier threshold transitions, pickpocket
success/failure probability, accomplice follow AI, PCSO warning logic, ARU spawn
conditions, wanted poster placement, HUD star rendering logic, notoriety bribe
reduction, all new achievement triggers.

**Integration tests — implement these exact scenarios:**

1. **Notoriety accumulates across criminal acts**: Start a new game (Notoriety=0).
   Break 5 blocks inside a building (+2 each = +10). Complete 1 heist (+30). Hit
   1 Police NPC (+20). Verify total Notoriety is exactly 60. Verify the player is
   still at Tier 0 (title "Nobody", PCSO response only).

2. **Tier 1 unlock on crossing 100 Notoriety**: Set Notoriety to 98. Commit a
   heist (+30). Verify Notoriety is now 128. Verify the player's tier is 1 (title
   "Local Nuisance"). Verify the HUD shows 1 filled star. Verify the Fence's buy
   price for any item has improved by 10% compared to Tier 0.

3. **Pickpocket success adds coins and notoriety**: Place the player directly behind
   a pedestrian NPC (within 1.5 blocks, player not in NPC's forward arc). Set noise
   to 0.0. Ensure no other NPCs within 6 blocks. Press F. Verify on success: player
   inventory contains 1–4 more COIN than before. Verify Notoriety increased by 5.
   Verify the NPC's pickpocket cooldown is set. Verify tooltip "Easy money. For now."
   fires on first success.

4. **Pickpocket failure causes NPC to alert police**: Place a POLICE NPC 10 blocks
   away. Place the target NPC facing the player (pickpocket will fail — NPC is
   facing player). Press F. Verify the target NPC enters FLEEING state and moves
   toward the Police NPC. Verify the player's criminal record gains 1 offence.
   Verify Notoriety increased by 2. Verify no coins were added to inventory.

5. **PCSO issues warning then calls police**: Set Notoriety to 50 (Tier 0–1 boundary,
   PCSOs active). Commit a crime. Verify a PCSO NPC spawns and approaches the player.
   Verify the PCSO's speech bubble contains "Oi! Pack it in!" (does NOT arrest).
   Commit a second crime within 60 in-game seconds. Verify a regular POLICE NPC
   now spawns (PCSO radioed for backup).

6. **ARU spawns at Tier 3 after heist alarm**: Set Notoriety to 500 (Tier 3). Execute
   a heist and trigger an alarm. Verify at least 1 `ARMED_RESPONSE` NPC spawns within
   180 frames. Verify the ARU NPC moves faster than a regular Police NPC (compare
   move speed values). Verify the ARU NPC pursues the player into a building interior
   (does not stop at the doorway).

7. **Helicopter searchlight detects player**: Set Notoriety to 500 (Tier 3). Advance
   the simulation by 5 in-game minutes (trigger helicopter pass). Verify a sweeping
   PointLight appears in the environment. Move the player into the searchlight cone.
   Verify nearby ARU NPCs enter CHASING state. Move the player out of the cone.
   Verify ARU NPCs return to PATROL state after 10 seconds without re-detection.

8. **Accomplice follows player and speeds up safe crack**: Set Notoriety to 750 (Tier
   4). Recruit an accomplice (give player 10 coins, press E on a STREET_LAD NPC).
   Verify NPC type changes to ACCOMPLICE and NPCState is FOLLOWING_PLAYER. Move the
   player 10 blocks; verify the accomplice has also moved to within 3 blocks of the
   new position. Start a heist execution phase. Verify the safe-crack hold time is
   5 seconds (reduced from 8) due to the accomplice's presence.

9. **Wanted poster appears at Tier 5**: Set Notoriety to 1000 (Tier 5). Advance 60
   frames. Verify at least 1 `WANTED_POSTER` prop has been placed on a wall block
   near a police-patrolled area. Verify a `RumourType.PLAYER_SPOTTED`-family rumour
   referencing the wanted poster has been seeded into the RumourNetwork. Verify the
   HUD stars are all 5 filled and pulsing.

10. **Full notoriety arc stress test**: Start at Notoriety=0. Progress through all 5
    tiers by completing heists and committing crimes. At each tier transition: verify
    the HUD star count updates, verify the correct police response type spawns on the
    next crime, verify the player title text updates. At Tier 5: verify the
    RAGAMUFFIN achievement fires. Verify the game remains in PLAYING state with no
    crashes, no NPEs, all NPC counts valid, all HUD elements rendering correctly.

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

#### Step 2: BUGHUNT

**Before any new features, assume EVERYTHING is broken.** You are now a QA tester, not
a developer. Your job is to find bugs, not add features. The previous phases may compile
and pass unit tests, but that does NOT mean they actually work when the game runs.

**The BUGHUNT process:**

1. **Read every file** in src/main/java/ragamuffin/ systematically. Do not skim. Read
   every class, every method, every line.

2. **Trace the wiring**: For each system (world gen, rendering, player, NPCs, HUD, etc.),
   trace the call chain from RagamuffinGame.create() and render()/update() all the way
   through. Ask: "Is this system actually called? Is it wired into the game loop? Does
   the data flow from creation to rendering?"

3. **Check cross-phase integration**: Each phase was likely developed in isolation. Check
   that Phase N's code actually works with Phase 1's renderer. Specifically:
   - Does the ChunkMeshBuilder use the correct colours/textures for ALL block types?
   - Does the WorldGenerator actually fill the terrain solidly (no air gaps in ground)?
   - Does the Player have gravity applied every frame?
   - Are NPCs actually rendered in the 3D world (added to scene, positioned correctly)?
   - Does the HUD actually draw on screen (SpriteBatch begin/end called, font loaded)?
   - Are inventory/crafting UIs wired to keyboard input (I key, C key)?

4. **Write regression tests**: For every bug found, write a specific integration test
   that would have caught it. The test should:
   - Set up the exact conditions that expose the bug
   - Assert the correct behaviour (not the buggy behaviour)
   - Go in src/test/java/ragamuffin/integration/BughuntNIntegrationTest.java
     (where N matches the critic iteration number)

5. **Fix all bugs found** before proceeding to the CRITIC review. Run ./gradlew clean test
   and ./gradlew build. ALL tests must pass.

**Common bugs to hunt for (from playtesting):**
- Player floating (no gravity applied in update loop)
- All blocks same colour (BlockType colours not used by mesh builder)
- Terrain has holes/gaps (WorldGenerator not filling solid ground)
- Buildings/structures invisible (not added to chunk or mesh not rebuilt after placement)
- HUD elements not visible (SpriteBatch not configured, font not loaded, wrong coordinates)
- NPCs invisible or not moving (not added to render pass, AI not ticked)
- Keyboard input not working (InputHandler not registered or key mappings wrong)
- Systems created but never called from game loop (orphaned code)

**Commit bughunt fixes before proceeding to Step 3.**

#### Step 3: BUG REPORTS

Check the `bugs/` directory for .txt files. Each file is a bug report from a playtester.

**Process:**
1. Read every .txt file in the `bugs/` directory.
2. For each bug report:
   - Investigate the root cause by reading the relevant source code
   - Write an integration test that reproduces the bug
   - Fix the bug
   - Verify the fix with ./gradlew clean test
3. Once the bug is fixed, rename the file from `NNN-description.txt` to
   `NNN-description.fixed.txt` so it is not processed again.
4. Commit all bug fixes with message "Fix bug reports: [list of bug numbers]"

**Bug reports are high priority.** Fix all open bug reports before proceeding to the
CRITIC review. A playtester took the time to report these — respect that.

#### Step 4: CRITIC Review

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

#### Step 5: Write Improvement Phase

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

#### Step 6: Implement

Implement the improvement phase using TDD, exactly as with Phases 1-8.
Delegate all coding to the coding agent. Verify all tests pass.

#### Step 7: Code Review

Run a mini code review on the changes made in this iteration:
- All new tests pass
- No existing tests broken
- `./gradlew build` is clean
- New code is wired into the game loop (not orphaned)

Commit all work on the current `criticN` branch.

#### Step 8: Loop

Go back to Step 1. Create `critic(N+1)` from `criticN`. Run the CRITIC again.
Find the next set of improvements. Implement them. Review. Commit. Repeat.

**This loop never terminates.** There is always something to improve. If you
genuinely cannot think of a single improvement, you are not thinking hard enough.
Play the game (mentally simulate a play session), read the code, re-read the spec,
consider the player experience. Something can always be better.

---

## Phase 11: CRITIC 1 Improvements — Survival Depth & Player Feedback

### CRITIC Findings

**Gameplay feel:**
- Hunger drains but there's no way to restore it — the player is guaranteed to die. This isn't dark humour, it's just broken.
- No healing mechanic. Once damaged, you're damaged forever. That's not fun, that's nihilism without the laughs.
- No death/respawn cycle. The game just... ends. A British survival game should have you wake up on a park bench, not stare at a black screen.
- Block breaking gives zero visual feedback — you punch 5 times and have to count in your head.

**Tone and humour:**
- Only 5 tooltips in the entire game. Needs at least 10 more sardonic observations about British life.
- NPC speech is listed as a feature but no NPCs actually say anything. Members of the public should mutter things.

**Polish:**
- No visual feedback for block breaking progress (crack overlay or progress bar)
- No feedback when items are picked up
- No interaction system (E key does nothing)

### Improvements to Implement

1. **Food system**: Greggs sells food items. Punching Greggs blocks yields SAUSAGE_ROLL and STEAK_BAKE. Eating food (right-click food in hotbar) restores hunger. Add SAUSAGE_ROLL and STEAK_BAKE materials.

2. **Healing**: Resting (standing still for 5 seconds with hunger > 50) slowly regenerates health. Rate: 5 HP per second while resting.

3. **Respawn on death**: When health hits 0, display "You wake up on a park bench. Again." for 3 seconds, then respawn at park centre with 50% health, 50% hunger, 100% energy. Inventory is kept. This is not Dark Souls.

4. **Block breaking progress**: Track break progress (0 to N hits, where N varies by block hardness). Display a progress indicator (e.g. crack stages proportional to hits required). The BlockBreaker already tracks hits — expose this to the UI.

5. **More tooltips**: Add these first-time tooltips:
   - On first block place: "That's... structurally ambitious."
   - On first death: "Council tax doesn't pay itself. Get up."
   - On first Greggs encounter: "Ah, Greggs. The backbone of British cuisine."
   - On hunger reaching 25%: "Your stomach growls. Even the pigeons look appetising."
   - On first crafting: "Crafting with materials of questionable provenance."

6. **NPC speech bubbles**: Members of the public randomly say things when near the player:
   - "Is that... legal?"
   - "My council tax pays for this?"
   - "I'm calling the council."
   - "Bit rough, innit?"
   - "You alright, love?"

7. **E key interaction**: Press E when facing an NPC to get a response. Public NPCs respond with random dialogue. Police say "Move along." Council builders say "Planning permission denied."

### Integration Tests

1. **Eating food restores hunger**: Set hunger to 50. Give player 1 SAUSAGE_ROLL in hotbar. Select it. Right-click (eat). Verify hunger increased by 30 (to 80). Verify SAUSAGE_ROLL removed from inventory.

2. **Greggs yields food**: Place player adjacent to a Greggs building block. Punch 5 times. Verify inventory contains either SAUSAGE_ROLL or STEAK_BAKE.

3. **Resting regenerates health**: Set health to 50, hunger to 100. Player stands still (no input). Advance 300 frames (5 seconds). Verify health > 50 (should be ~75).

4. **Resting does NOT heal when hungry**: Set health to 50, hunger to 20. Stand still 300 frames. Verify health is still 50 (hunger too low to heal).

5. **Death triggers respawn**: Set health to 10. Apply 10 damage. Verify death state triggered. Verify respawn message "You wake up on a park bench. Again." displayed. Advance 180 frames. Verify player respawned at park centre with health 50, hunger 50, energy 100. Verify inventory preserved.

6. **Block breaking progress exposed**: Start breaking a TREE_TRUNK. After 1 punch, verify break progress is 1/5 (0.2). After 3 punches, verify 3/5 (0.6). After 5, block broken.

7. **New tooltips fire correctly**: Place a block for first time — verify tooltip "That's... structurally ambitious." First craft — verify "Crafting with materials of questionable provenance." Set hunger to 25 — verify "Your stomach growls. Even the pigeons look appetising."

8. **NPC speech near player**: Spawn PUBLIC NPC within 5 blocks of player. Advance 300 frames. Verify the NPC has emitted at least one speech bubble (speech text is non-null and from the expected list).

9. **E key interaction with NPC**: Spawn PUBLIC NPC adjacent to player. Player faces NPC. Press E. Verify interaction dialogue is triggered (NPC response text is non-null). Verify the response is from the expected PUBLIC NPC dialogue list.

10. **E key when not facing NPC does nothing**: No NPC nearby. Press E. Verify no interaction triggered. No errors.

---

## Phase 12: CRITIC 2 Improvements — Tools, Shelter & Weather

### CRITIC Findings

**Gameplay depth:**
- You punch everything with bare fists. 5 hits for every block regardless of material is boring. A stone wall should be harder than a tree. Tools should exist.
- There's no shelter mechanic. The police move you on at night, but you can't actually protect yourself except by running. A cardboard box should be craftable as a first shelter.
- Weather does nothing. Rain should drain energy faster. Cold at night should drain health if unsheltered.

**Player progression:**
- The crafting tree is flat. Need tool tiers: fist < improvised tool < proper tool.
- No reason to explore different buildings — office loot and shop loot feel the same.

### Improvements

1. **Tool system**: Add CARDBOARD, IMPROVISED_TOOL, and STONE_TOOL materials. Recipes:
   - 2 WOOD + 1 STONE → 1 IMPROVISED_TOOL
   - 4 STONE + 2 WOOD → 1 STONE_TOOL
   Tools go in hotbar. When selected, reduce hits needed: bare fist=5, improvised=3, stone=2.
   Tools have durability (improvised=20 uses, stone=50 uses). Tool breaks when durability hits 0.

2. **Block hardness**: Different blocks need different base hits:
   - TREE_TRUNK, LEAVES, GRASS: 5 hits (soft)
   - BRICK, STONE, PAVEMENT: 8 hits (hard)
   - GLASS: 2 hits (fragile)
   Tool multiplier reduces these.

3. **Cardboard shelter**: CARDBOARD material drops from breaking blocks near shops. Recipe: 6 CARDBOARD → 1 CARDBOARD_BOX (placeable). When placed, creates a 2x2x2 shelter. While inside a shelter at night, police cannot "see" you (they skip past). Add CARDBOARD to BlockType and Material.

4. **Weather system**: Random weather changes every 5-10 game minutes. States: CLEAR, OVERCAST, RAIN, COLD_SNAP. Rain increases energy drain by 50%. Cold snap (night only) drains 2 HP/s if player is not inside a shelter. Weather displayed on HUD.

5. **Shelter detection**: Track if player is "inside" — check if blocks exist above, left, right, front, back of player position (roof + 3 walls minimum). Used for weather protection and police avoidance.

### Integration Tests

1. **Improvised tool reduces hits**: Give player IMPROVISED_TOOL in hotbar, select it. Place adjacent to TREE_TRUNK. Punch 3 times. Verify block broken. Verify tool durability decreased by 3.

2. **Stone tool even faster**: Give STONE_TOOL. Adjacent to TREE_TRUNK. Punch 2 times. Block broken. Durability decreased by 2.

3. **Bare fist on hard block**: Adjacent to BRICK. Punch 5 times — block NOT broken (needs 8). Punch 3 more (8 total). Block broken.

4. **Tool breaks at zero durability**: Give IMPROVISED_TOOL with durability 1. Punch a block. Verify tool is removed from inventory (broken). Verify tooltip "Your tool falls apart. Typical."

5. **Craft improvised tool**: Give 2 WOOD + 1 STONE. Open crafting. Craft. Verify 1 IMPROVISED_TOOL in inventory with durability 20.

6. **Cardboard shelter hides from police**: Place CARDBOARD_BOX creating 2x2x2 shelter. Player inside. Set time 22:00. Police spawn. Advance 600 frames. Verify police do NOT approach player (distance does not decrease). Remove shelter. Advance 300 frames. Verify police now approach.

7. **Rain increases energy drain**: Set weather CLEAR. Record energy. Advance 300 frames. Record energy drain as baseline. Reset energy. Set weather RAIN. Advance 300 frames. Verify energy drain is at least 40% more than baseline.

8. **Cold snap drains health outside**: Set weather COLD_SNAP, time to night. Player NOT in shelter. Health 100. Advance 300 frames (5s). Verify health < 100 (should lose ~10 HP).

9. **Cold snap does NOT drain health inside shelter**: Build shelter around player. Set COLD_SNAP + night. Health 100. Advance 300 frames. Verify health is still 100.

10. **Weather displays on HUD**: Set weather RAIN. Verify HUD shows weather state "Rain". Set CLEAR. Verify shows "Clear".

---

## Phase 12: Implementation Notes

**Implementation status**: ✅ Complete

All systems implemented and tested:

1. **Tool system**:
   - `Tool` class tracks durability (IMPROVISED_TOOL=20, STONE_TOOL=50)
   - `BlockBreaker` updated to accept tool parameter and calculate hits based on block hardness × tool multiplier
   - Tool multipliers: bare fist=1.0, improvised=0.6, stone=0.4

2. **Block hardness**:
   - Soft blocks (TREE_TRUNK, LEAVES, GRASS): 5 base hits
   - Hard blocks (BRICK, STONE, PAVEMENT): 8 base hits
   - Fragile blocks (GLASS): 2 base hits

3. **Material additions**:
   - CARDBOARD (drops from shop blocks: OFF_LICENCE, CHARITY_SHOP)
   - IMPROVISED_TOOL (crafted from 2 WOOD + 1 STONE)
   - STONE_TOOL (crafted from 4 STONE + 2 WOOD)

4. **Weather system**:
   - `Weather` enum: CLEAR, OVERCAST, RAIN, COLD_SNAP
   - `WeatherSystem` changes weather every 5-10 game minutes
   - RAIN: 1.5× energy drain multiplier
   - COLD_SNAP: drains 2 HP/s at night when unsheltered

5. **Shelter detection**:
   - `ShelterDetector` checks for roof (solid block 2 above player) + 3+ walls
   - Protects from weather effects and police detection

6. **BlockType addition**:
   - CARDBOARD (id=12, solid=true)

7. **Tooltip addition**:
   - TOOL_BROKEN: "Your tool falls apart. Typical."

8. **Test updates**:
   - Fixed existing tests to account for new block hardness values
   - All 10 Phase 12 integration tests pass
   - All 245 existing tests still pass

**Integration completed**:
✅ WeatherSystem wired into RagamuffinGame update loop
✅ Weather display added to GameHUD (top-right corner)
✅ Energy recovery modified by weather multiplier (rain slows recovery)
✅ Cold snap health drain implemented with shelter check
✅ Tool class tracks durability per-tool
✅ TOOL_BROKEN tooltip added

**Remaining gameplay enhancements (optional)**:
- Inventory system could track tool durability for placed tools in hotbar
- Block breaking UI could show current tool in use
- Weather transitions could have visual effects (rain particles, etc.)
- Cardboard box placement could auto-build 2x2x2 shelter structure

---

## Bug Fix: Police scanForStructures includes world-generated BRICK as player structures

**Status**: ❌ Broken — police immediately treat every building in the generated town as a
player-built structure, issuing warnings and arrests from the first second of gameplay.

**Problem**: `NPCManager.scanForStructures()` (used by `updatePolicePatrolling` and
`updatePoliceWarning`) checks for both `BlockType.WOOD` and `BlockType.BRICK` as indicators
of player-built structures:

```java
if (block == BlockType.WOOD || block == BlockType.BRICK) {
    playerBlockCount++;
    ...
}
```

However, **the entire procedurally generated world is built from BRICK** — every terraced
house, shop, office building, and landmark facade is BRICK. With a scan radius of 20 blocks
and a threshold of just 5 blocks, any police NPC standing near any building in town will
immediately detect dozens of "player structures" and enter WARNING → AGGRESSIVE → ARRESTING
state. The player is arrested on the first frame of gameplay, without having placed a single
block.

By contrast, `checkForPlayerStructures` (used by civilian NPCs to react to builds) correctly
scans only for `BlockType.WOOD`:

```java
if (block == BlockType.WOOD) {
    playerBlockCount++;
```

The inconsistency means civilians correctly ignore world buildings while police do not.

**Root cause**: BRICK was included in `scanForStructures` to detect player-placed BRICK
blocks (Phase 3 lets players break and re-place BRICK), but this is indistinguishable from
the world's existing BRICK. The fix is to limit police structure detection to only
`BlockType.WOOD` (the primary player-placeable building material), matching the civilian
check.

**Required fix in `NPCManager.scanForStructures()`**: Change the block check to:

```java
if (block == BlockType.WOOD) {
```

**Integration test** (regression — verify police do not arrest player near world buildings):

1. Generate the world. Do NOT place any blocks. Player stands at (40, 1, 20) — near the
   high street (a BRICK-heavy area). Set time to 22:00 to allow police spawning. Advance
   600 frames. Verify the `arrestPending` flag is NOT set (police did not arrest player
   for simply being near world-generated BRICK buildings). Verify no police NPC has state
   AGGRESSIVE targeting the player without the player having built anything.

2. Place 10 WOOD blocks nearby. Set time to 22:00. Advance 600 frames. Verify police DO
   detect the WOOD structure, enter WARNING state, and eventually set `arrestPending = true`.
   This confirms the detection still works for actual player builds.

---

## Bug Fix: Wire GangTerritorySystem into RagamuffinGame

**Status**: ❌ Not wired — `GangTerritorySystem` is fully implemented but never connected to the game loop.

**Problem**: `GangTerritorySystem` exists as a complete, tested class but is never instantiated in
`RagamuffinGame`. No territories are registered, `update()` is never called, and `onPlayerAttacksGang()`
is never invoked when hitting a youth gang NPC. The gang territory mechanic is silently absent from
actual gameplay despite all integration tests passing (they test the class in isolation).

**Required changes to `RagamuffinGame`**:

1. Add a `private GangTerritorySystem gangTerritorySystem;` field.

2. In `initGame()`, after the NPC manager is set up, instantiate and register territories matching
   the youth gang spawn locations:
   ```
   gangTerritorySystem = new GangTerritorySystem();
   gangTerritorySystem.addTerritory("Bricky Estate",  -50f, -30f, 18f);
   gangTerritorySystem.addTerritory("South Patch",    -45f, -45f, 15f);
   ```

3. In `updatePlaying()`, call `gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, world)`
   every frame (only while not UI-blocked, same as other game systems).

4. In `handlePunch()`, when a YOUTH_GANG NPC is punched, call
   `gangTerritorySystem.onPlayerAttacksGang(tooltipSystem, npcManager, player, world)`.

5. In `restartGame()`, reset the system: `gangTerritorySystem.reset()`.

**Integration test** (add to Phase 13 / Critic 7 suite as a true end-to-end test via `RagamuffinGame`):

- Create a `RagamuffinGame` (headless), transition to PLAYING, move player into a registered
  gang territory at `(-50, 1, -30)`. Simulate ticks for `LINGER_THRESHOLD_SECONDS + 1` seconds.
  Verify that at least one nearby `YOUTH_GANG` NPC has state `AGGRESSIVE`.

---

## Bug Fix: SignageRenderer missing orthographic projection matrix

**Status**: ❌ Broken — building signs render at wrong screen positions (or are invisible) in all frames.

**Problem**: `SignageRenderer.render()` calls `shapeRenderer.begin()` and `spriteBatch.begin()` without
ever calling `setProjectionMatrix(ortho)` on either renderer first. All other 2D rendering in the
codebase explicitly sets an orthographic projection via `matrix.setToOrtho2D(0, 0, screenWidth, screenHeight)`.
Without this, both renderers use whatever stale 3D (or previous frame's 2D) projection matrix is
currently bound — causing sign panels and text to be drawn at completely wrong coordinates.

The `camera.project()` call correctly maps the sign's world position to screen space, but the
shape renderer and sprite batch then interpret those screen-space pixel coordinates through the
wrong matrix, so the result is either off-screen or scrambled.

**Required fix in `SignageRenderer.render()`**:

At the top of the `render()` method, before the loop begins, set the orthographic projection on
both the `shapeRenderer` and `spriteBatch`:

```java
com.badlogic.gdx.math.Matrix4 ortho = new com.badlogic.gdx.math.Matrix4();
ortho.setToOrtho2D(0, 0, screenWidth, screenHeight);
shapeRenderer.setProjectionMatrix(ortho);
spriteBatch.setProjectionMatrix(ortho);
```

**Integration test** (regression — add to the next Critic integration test suite):

1. Create a headless LibGDX context. Build a `SignageRenderer`. Call `buildFromLandmarks()` with
   at least one landmark (e.g. GREGGS at position (40, 4, 20)). Create a `PerspectiveCamera` at
   (40, 5, 25) looking at (40, 4, 20) — so the sign is directly in front of the camera and within
   `MAX_RENDER_DISTANCE`. Call `render()`. Verify that `shapeRenderer.setProjectionMatrix()` was
   called with an orthographic matrix (not the camera's perspective projection). Verify the sign
   panel's X coordinate lands within screen bounds (0..screenWidth) and Y within (0..screenHeight).

---

## Bug Fix: renderSpeechBubbles missing orthographic projection matrix

**Status**: ❌ Broken — NPC speech bubbles (the core communication channel between NPCs and the
player) render at completely wrong positions or are invisible in every frame of gameplay.

**Problem**: `RagamuffinGame.renderSpeechBubbles()` calls `shapeRenderer.begin()` and
`spriteBatch.begin()` without first calling `setProjectionMatrix(ortho)` on either renderer.
This method is called immediately after `modelBatch.end()` (the 3D world render), so the last
projection matrix bound to the GL state is the ModelBatch's 3D perspective matrix.

Every other 2D rendering method in `RagamuffinGame` correctly sets an orthographic projection first:
- `renderLoadingScreen()` — sets `spriteBatch.setProjectionMatrix(proj)` before drawing
- `renderMenu()` — sets both `spriteBatch` and `shapeRenderer` projection matrices
- `renderTooltip()` — sets `shapeRenderer.setProjectionMatrix(tooltipProj)` then `spriteBatch.setProjectionMatrix(tooltipProj)`
- `renderDamageFlash()` — sets `shapeRenderer.setProjectionMatrix(flashProj)`

But `renderSpeechBubbles()` (lines 1331–1373) calls `shapeRenderer.begin()` at line 1362 and
`spriteBatch.begin()` at line 1368 with no projection matrix set — they both use the stale 3D
perspective matrix left by `modelBatch.end()`.

The impact is total: `camera.project()` correctly computes screen-space pixel coordinates (sx, sy)
for each NPC's head position, but the shape renderer and sprite batch interpret those pixel coords
through the perspective matrix — resulting in the background box and text being drawn at completely
wrong on-screen positions. Since `camera.project()` returns values in the range [0..screenWidth] for
X and [0..screenHeight] for Y in pixel space, but the perspective matrix expects NDC/world units,
the resulting draw calls either go off-screen entirely or land at nonsense coordinates.

The same root cause previously broke `SignageRenderer` (see above). Speech bubbles are more impactful
because they are the primary feedback loop for NPC reactions: police warnings, gang threats, civilian
complaints, shopkeeper dialogue, and death speech all go through this path. A player watching a police
NPC walk toward them would see no speech bubble warning before arrest, depriving them of the key
gameplay signal that action is needed.

**Required fix in `RagamuffinGame.renderSpeechBubbles()`**:

At the top of the method body (before the NPC loop), set orthographic projection on both renderers:

```java
com.badlogic.gdx.math.Matrix4 ortho = new com.badlogic.gdx.math.Matrix4();
ortho.setToOrtho2D(0, 0, screenWidth, screenHeight);
shapeRenderer.setProjectionMatrix(ortho);
spriteBatch.setProjectionMatrix(ortho);
```

**Integration test** (add to the next Critic integration test suite):

1. Create a headless `RagamuffinGame`. Transition to PLAYING. Spawn an NPC within 10 blocks of
   the player and call `npc.setSpeechText("Test", 3.0f)`. Advance one frame. Verify that
   `renderSpeechBubbles()` was called. Verify `shapeRenderer` was given an orthographic
   projection matrix (not the perspective one) before the bubble background was drawn.
   Verify the bubble's computed screen X is within `[0, screenWidth]` and Y within
   `[0, screenHeight]`.

---

## Bug Fix: HealingSystem movement detection is frame-rate dependent

**Status**: ❌ Broken — on high-refresh-rate displays (120Hz, 144Hz, 240Hz) the player heals
continuously while walking, breaking the "rest to heal" survival mechanic entirely.

**Problem**: `HealingSystem.update()` detects movement by comparing the player's per-frame
displacement against a fixed threshold:

```java
float distanceMoved = currentPos.dst(lastPosition); // distance moved in ONE frame
if (distanceMoved < MOVEMENT_THRESHOLD) {            // MOVEMENT_THRESHOLD = 0.1f
    restingTime += delta;
```

The threshold `0.1f` is a hard-coded absolute distance, not scaled by delta. At 60fps
(`delta ≈ 0.0167s`), a player moving at `MOVE_SPEED = 12 blocks/s` travels
`12 × 0.0167 = 0.20f` blocks per frame — correctly above the threshold.

But at **120fps** (`delta ≈ 0.0083s`), the same player travels `12 × 0.0083 = 0.10f` per frame —
right at the boundary. At **144fps** (`delta ≈ 0.0069s`): `12 × 0.0069 = 0.083f` — below the
threshold. At **240fps** (`delta ≈ 0.0042s`): `12 × 0.0042 = 0.050f` — well below the threshold.

The result: on any display running faster than ~100fps, the healing system classifies the player
as "resting" even while sprinting. The `restingTime` accumulates to `RESTING_DURATION_REQUIRED`
(5 seconds) and the player heals at 5 HP/s indefinitely while moving. This makes combat and cold
snaps trivially survivable, gutting the entire survival mechanic.

**Root cause**: the threshold should scale with `delta` (i.e., be a *speed* threshold, not a
*distance* threshold), or the check should use the player's horizontal velocity magnitude directly.

**Required fix in `HealingSystem.update()`**:

Replace the per-frame distance comparison with a velocity-scaled comparison:

```java
// Compute speed from per-frame displacement (blocks/second)
float speed = (delta > 0) ? currentPos.dst(lastPosition) / delta : 0f;
if (speed < MOVEMENT_THRESHOLD) {   // MOVEMENT_THRESHOLD is now a speed (blocks/s)
    restingTime += delta;
} else {
    restingTime = 0;
}
```

And update the constant's documentation to reflect it is a speed threshold:

```java
public static final float MOVEMENT_THRESHOLD = 0.5f; // Below this speed (blocks/s) = resting
```

The value `0.5f` gives a clear separation: a stationary player has speed 0, a walking player
has speed ≥ 12. Any value between 0 and 12 works correctly regardless of frame rate.

**Integration tests** (add to Phase 11 suite):

5. **Healing does NOT trigger while moving (any frame rate)**: Set health to 50, hunger to 100.
   Simulate the player moving forward (non-zero displacement each frame) for 300 frames using
   `delta = 1f/240f` (240fps simulation). Verify `restingTime` never exceeds 0.1 seconds.
   Verify health remains at 50 (no healing occurred).

6. **Healing triggers correctly at high frame rate**: Set health to 50, hunger to 100. Player
   stands still. Simulate 300 frames at `delta = 1f/240f`. Verify `restingTime` accumulates
   to ≥ 5 seconds. Verify health > 50.

---

## Bug Fix: StructureTracker scans BRICK blocks, causing council builders to demolish the generated town

**Status**: ❌ Broken — council builders are immediately dispatched to demolish every brick
building in the generated world (terraced houses, shops, office building, JobCentre, etc.),
systematically destroying the procedurally generated town 30 seconds after the game starts.

**Problem**: `StructureTracker.scanForStructures()` treats both `BlockType.WOOD` and
`BlockType.BRICK` as player-built structure blocks:

```java
if (block == BlockType.WOOD || block == BlockType.BRICK) {
    // Found a placed block - trace the structure
    Set<Vector3> structureBlocks = traceStructure(world, x, y, z, visited);
```

And `traceStructure()` flood-fills through both types:

```java
if (block == BlockType.WOOD || block == BlockType.BRICK) {
    queue.add(new Vector3(x, y, z));
}
```

The entire procedurally generated world is built from BRICK — every terraced house, shop,
office building, JobCentre, and landmark facade is BRICK. The scanner runs every 30 seconds
over a 200×200 block area. Every BRICK structure above the 10-block threshold (which is
nearly every building in the world) is added to the structure list.

`updateCouncilBuilders()` then calls `calculateBuilderCount()` for each detected structure
and spawns COUNCIL_BUILDER NPCs. Each builder NPC demolishes one block per second. Within
minutes of gameplay, council builders converge on all town buildings and systematically
remove them block by block. This is not a subtle bug — the entire generated world is
destroyed by the council on a 30-second timer from the moment the game loads.

Note that the police's own `scanForStructures()` (a separate private method in `NPCManager`)
was previously affected by the same BRICK false-positive bug and has already been fixed to
check only `BlockType.WOOD`. `StructureTracker` was not updated in that fix pass.

**Root cause**: BRICK was included in `StructureTracker` to detect player-placed BRICK blocks
(Phase 3 lets players break and re-place BRICK), but world-generated BRICK is
indistinguishable from player-placed BRICK at the block-type level. The fix is to limit
structure detection to `BlockType.WOOD` only, matching the police `scanForStructures()` fix
and the civilian `checkForPlayerStructures()` check.

**Required fix in `StructureTracker.scanForStructures()` and `traceStructure()`**:

In `scanForStructures()`, change:
```java
if (block == BlockType.WOOD || block == BlockType.BRICK) {
```
to:
```java
if (block == BlockType.WOOD) {
```

In `traceStructure()`, change:
```java
if (block == BlockType.WOOD || block == BlockType.BRICK) {
```
to:
```java
if (block == BlockType.WOOD) {
```

**Integration tests**:

1. **Council builders do NOT demolish world-generated buildings**: Generate the world. Do NOT
   place any blocks. Call `npcManager.forceStructureScan(world, tooltipSystem)`. Verify that
   `npcManager.getStructureTracker().getStructures()` is empty (no structures detected).
   Verify that no COUNCIL_BUILDER NPCs have been spawned.

2. **Council builders DO detect player-built WOOD structures**: Generate the world. Place 15
   WOOD blocks in a connected cluster at (10, 1, 10). Call `npcManager.forceStructureScan(world,
   tooltipSystem)`. Verify that `structureTracker.getStructures()` has exactly one entry.
   Verify that a COUNCIL_BUILDER NPC has been spawned targeting that structure.

---

## Bug Fix: NPCManager.gameTime never updated — NPC daily routines permanently stuck at 8:00 AM

**Status**: ❌ Broken — NPCs never transition through their daily routine. PUBLIC and COUNCIL_MEMBER
NPCs are permanently in `GOING_TO_WORK` state regardless of the actual in-game time.

**Problem**: `NPCManager` maintains its own `gameTime` field (initialised to `8.0f`). The method
`updateDailyRoutine(NPC)` decides an NPC's routine state (`GOING_TO_WORK`, `GOING_HOME`,
`AT_PUB`, `AT_HOME`) entirely from this field:

```java
private float gameTime; // Game time in hours (0-24)
// ...
this.gameTime = 8.0f; // Start at 8:00 AM
```

`NPCManager.setGameTime(float)` exists to update this field, and it also re-evaluates every
PUBLIC/COUNCIL_MEMBER NPC's routine. However, **`RagamuffinGame` never calls `setGameTime()`**.
The `TimeSystem` advances time every frame and supplies the current hour to
`npcManager.updatePoliceSpawning(timeSystem.getTime(), ...)`, but the NPCManager's internal
`gameTime` clock is never synchronised.

Result: `updateDailyRoutine()` always evaluates `gameTime == 8.0`, which satisfies
`8 >= 8 && 8 < 17`, so every PUBLIC and COUNCIL_MEMBER NPC is permanently locked in
`GOING_TO_WORK` state. They never go home at 17:00, never visit the pub at night, and the
day/night population rhythm described in Phase 5 (and the Phase 5 integration test #7) is
completely absent from actual gameplay.

The police spawning system is **not** affected — it correctly receives time from
`TimeSystem` each frame via `updatePoliceSpawning()` — but the civilian NPC daily routine
system is broken.

**Required fix in `RagamuffinGame.render()` / `updatePlaying()`**:

After `timeSystem.update(delta)` is called (line ~495), synchronise the NPC manager's clock:

```java
npcManager.setGameTime(timeSystem.getTime());
```

This single call gates the existing `updateDailyRoutine()` logic correctly, immediately
restoring the full day/night NPC population cycle.

**Integration tests**:

1. **NPC daily routine driven by TimeSystem**: Create a `RagamuffinGame` (headless). Transition
   to PLAYING. Spawn a PUBLIC NPC. Advance the game with `timeSystem` at 08:00 — verify the NPC
   state is `GOING_TO_WORK`. Advance `timeSystem` to 17:00 and call
   `npcManager.setGameTime(17.0f)`. Verify the NPC state is `GOING_HOME`. Advance to 20:00.
   Verify the NPC state is `AT_PUB` or `AT_HOME`.

2. **Regression — setGameTime is called each frame**: Run the game loop for 60 frames with
   `TimeSystem` advancing normally. Call `npcManager.getGameTime()`. Verify it is approximately
   equal to `timeSystem.getTime()` (within 0.1 hours), confirming the wiring is live.

---

## Bug Fix: Block-breaking awards reputation points, making the player Notorious after ~30 blocks

**Status**: ❌ Broken — normal resource-gathering (the core gameplay loop) rapidly escalates the
player to NOTORIOUS status, causing civilian NPCs to flee on sight and doubling the police force
at night, making the town feel hostile and unplayable from the first in-game evening.

**Problem**: `RagamuffinGame.handlePunch()` awards 1 street reputation point every time a block
is broken (line ~1026):

```java
// Award street reputation for breaking blocks (minor crime)
player.getStreetReputation().addPoints(1);
```

`NOTORIOUS_THRESHOLD` is 30 points. Breaking just 30 blocks reaches NOTORIOUS status. A player
gathering the 4 WOOD needed to craft planks (5 punches × 4 trees = 20 break events) plus a
handful of additional resource blocks hits notorious in the first 10-15 minutes of play.

Once NOTORIOUS:
- `updatePoliceSpawning()` raises the active police cap from 4 to **8** officers
- `isCivilianType()` NPCs flee the player on sight (PUBLIC, PENSIONER, SCHOOL_KID, JOGGER,
  BUSKER, POSTMAN)
- Police skip the warning phase and go straight to AGGRESSIVE

The reputation system was designed to punish *criminal* behaviour — punching NPCs (2 pts),
raiding Greggs, fighting police. Routine resource collection (breaking trees) was never intended
to be a crime. The comment in the code even says "minor crime" — but 30 such "minor crimes"
triggers the maximum escalation response.

**Root cause**: Block breaking should not award reputation points at all. Reputation should
only increase from actions that are genuinely antisocial: attacking NPCs, being arrested, or
demolishing structures that belong to the world. Breaking a tree in the park is not a crime;
punching a pensioner is.

**Required fix in `RagamuffinGame.handlePunch()`**:

Remove the reputation gain on block break entirely:

```java
// REMOVED: player.getStreetReputation().addPoints(1);
```

Reputation gain on NPC punch (2 points) and arrest-related events should remain unchanged.

**Integration tests** (add to Phase 10 / Critic regression suite):

1. **Breaking blocks does NOT increase reputation**: Start a new game. Break 50 blocks (TREE_TRUNK,
   BRICK, GLASS — any combination). Verify `player.getStreetReputation().getPoints()` is still 0.
   Verify `player.getStreetReputation().isNotorious()` is false. Verify no civilians are in
   FLEEING state.

2. **Punching an NPC still increases reputation**: Spawn a PUBLIC NPC adjacent to the player.
   Punch it once. Verify `player.getStreetReputation().getPoints()` is 2 (unchanged from before
   this fix). Verify the reputation system still works for legitimate crime events.

3. **Player can gather resources extensively without becoming notorious**: Break 100 blocks over
   the course of simulated gameplay. Advance time to night. Verify the number of police NPCs
   spawned is at most 4 (the non-notorious cap), not 8. Verify civilians do not flee.

---

## Bug Fix: StreetReputation has no time-based decay — reaching NOTORIOUS is a permanent one-way ratchet

**Status**: ❌ Broken — once the player reaches NOTORIOUS status (by punching ~15 NPCs over the
course of a play session), the world permanently locks into a hostile state with no recovery path
other than being arrested twice in rapid succession. The `StreetReputation` class documentation
explicitly states "lying low" is a recovery mechanism, but no such mechanic exists in the code.

**Problem**: The reputation system is a one-way ratchet with no passive decay:

- `addPoints(2)` is called on every NPC punch — 15 fights reaches NOTORIOUS (30 pts)
- `removePoints(15)` is called only on arrest — requires **two arrests** just to reach NOBODY from minimum NOTORIOUS
- Each arrest sets health to 30 and hunger to 20, making two consecutive arrests potentially lethal
- There is no time-based decay, no "lying low" penalty reduction, no grace period
- Once NOTORIOUS:
  - All civilian NPC types flee the player on sight within 10 blocks (PUBLIC, PENSIONER, SCHOOL_KID, JOGGER, BUSKER, POSTMAN)
  - Police skip warnings and go straight to AGGRESSIVE on first contact
  - Police cap doubles from 4 to 8 officers at night

The `StreetReputation` Javadoc explicitly lists "lying low" as a decay mechanism ("Getting arrested,
dying, or lying low"), but no call site ever reduces reputation except for the arrest handler. The
`reset()` method exists but is only called on full game restart. There is no partial decay path.

**Root cause**: The decay mechanic described in the class documentation was never implemented.
The reputation system was designed as a two-way dynamic (crimes earn points, time/good behaviour
spends them), but only one direction was ever coded.

**Required fix in `StreetReputation`**:

Add a time-based passive decay that slowly reduces reputation when the player avoids criminal
activity. Suggested implementation:

```java
// In StreetReputation:
public static final float DECAY_INTERVAL_SECONDS = 60f; // One point lost per real minute of non-crime
private float decayTimer = 0f;

/**
 * Tick the reputation decay timer. Call once per frame with real delta time.
 * Removes 1 point every DECAY_INTERVAL_SECONDS of non-criminal behaviour.
 */
public void update(float delta) {
    if (points <= 0) return; // Nothing to decay
    decayTimer += delta;
    if (decayTimer >= DECAY_INTERVAL_SECONDS) {
        decayTimer -= DECAY_INTERVAL_SECONDS;
        removePoints(1);
    }
}
```

Wire `player.getStreetReputation().update(delta)` into `RagamuffinGame.updatePlaying()` each frame.

With this fix: a NOTORIOUS player (30 pts) who stops fighting will decay to KNOWN (9 pts) in
21 real minutes and reach NOBODY (0 pts) in 30 real minutes — a meaningful cooldown that
matches the "lying low" description in the code's own documentation.

**Integration tests** (add to Phase 10 / Critic regression suite):

1. **Reputation decays passively over time**: Give the player 30 reputation points (NOTORIOUS).
   Advance the simulation for `DECAY_INTERVAL_SECONDS * 30 + 1` seconds without any criminal
   actions. Verify `player.getStreetReputation().getPoints()` is 0. Verify
   `player.getStreetReputation().isNotorious()` is false.

2. **Decay does not go below zero**: Give the player 5 reputation points. Advance for
   `DECAY_INTERVAL_SECONDS * 10` seconds. Verify points are clamped at 0 (not negative).

3. **Criminal activity resets decay progress**: Give the player 10 reputation points. Advance
   for `DECAY_INTERVAL_SECONDS * 0.9` seconds (just below one decay tick). Punch an NPC
   (+2 pts). Verify points are now 12. Advance another `DECAY_INTERVAL_SECONDS * 0.9` seconds.
   Verify points are still 12 (crime didn't accelerate decay — the timer just continues running
   regardless). After a full interval total of `DECAY_INTERVAL_SECONDS * 1.8` from the start,
   verify points have decreased by 1 from peak.

4. **Player can recover from NOTORIOUS by lying low**: Set reputation to 30 (NOTORIOUS). Verify
   civilians flee. Advance `DECAY_INTERVAL_SECONDS * 21 + 1` seconds with no crimes. Verify
   reputation is now 9 (KNOWN, not NOTORIOUS). Verify `isNotorious()` is false. Verify that in
   the next simulation frame, civilian NPCs do NOT enter FLEEING state when the player approaches.

---

## Bug Fix: Block targeting highlight and placement preview are unimplemented

**Status**: ❌ Missing — Phase 3 and Phase 4 both specify visual targeting feedback,
but neither feature is implemented in the codebase.

**Problem**: The spec requires (Phase 3, line 135) "Block highlight/outline showing which block
the player is targeting (raycasting)" and (Phase 4, line 218) "Block placement preview (ghost
block showing where it will be placed)". The raycasting infrastructure already exists and is
used internally for break/place logic, but there is zero visual rendering of these affordances.
The player has no indication which block they are about to break or where a placed block will
land — they must guess based on crosshair position alone.

**Impact**: Core gameplay loop (breaking and placing blocks) lacks fundamental visual feedback.
Players cannot reliably target specific blocks in dense urban environments (terraced houses,
multi-storey buildings), and block placement is imprecise without a preview. This is
particularly egregious in a voxel game where precise block targeting is the primary interaction.

**Implementation**:

1. **Block targeting outline** (Phase 3): After `modelBatch.end()` each frame, render a
   wireframe cube around the targeted block using `ShapeRenderer.ShapeType.Line`. Use the
   existing `blockBreaker.getTargetBlock()` raycast result. The outline should be a thin
   (1px) white/black wireframe with slight transparency so it works against all block colours.
   The cube should be 1.0×1.0×1.0 world units at the block's integer position. Disable
   depth testing so the outline is always visible even when the block face is flush with
   adjacent blocks.

2. **Block placement preview** (Phase 4): When the player has a placeable block in the
   selected hotbar slot, compute the placement position each frame (reuse
   `blockPlacer.getPlacementPosition()`) and render a semi-transparent ghost block there.
   The ghost should use the same colour as the block type but with alpha 0.4f. It should
   not be rendered if there is no valid placement target.

**Integration tests** (add to Phase 3 / Phase 4 test suite):

1. **Targeting outline renders for targeted block**: Place the player adjacent to a STONE
   block, facing it. Verify `blockBreaker.getTargetBlock()` returns a non-null result for
   that block. Verify the `BlockHighlightRenderer` (or equivalent) has a non-null highlight
   position matching the block's coordinates. Verify that when no block is targeted (open
   area), the highlight position is null.

2. **Placement preview position is correct**: Give the player 1 PLANKS in hotbar slot 1.
   Select slot 1. Place the player 1 block away from a STONE wall, facing it. Verify the
   placement preview position returned by `blockPlacer.getPlacementPosition()` is the air
   block immediately adjacent to the wall face the player is looking at. Verify that when
   the player turns away (no valid target), the preview position is null.

---

## Bug Fix: Hunger and starvation damage tick while UI is open — player can die in their inventory

**Status**: ❌ Broken — when the player opens the inventory (I), crafting menu (C), or help
screen (H), the game correctly disables movement and block interaction (`isUIBlocking()` check
on `updatePlaying()`, line ~485), but the **survival system continues running**: hunger drains
every frame, and if hunger reaches zero, starvation health damage is applied at 5 HP/s.
Meanwhile, eating food via right-click (which routes through `handlePlace()` inside
`updatePlaying()`) is blocked. The player cannot eat, but they continue to starve.

**Problem**: In `RagamuffinGame.render()`, the survival stat updates are placed outside both
the `isUIBlocking()` guard and the `updatePlaying()` call:

```java
// Lines ~490–532 (inside !openingSequence.isActive() but OUTSIDE !isUIBlocking()):
timeSystem.update(delta);
// ...
player.updateHunger(delta * hungerMultiplier);      // Always ticks
if (player.getHunger() <= 0) {
    player.damage(5.0f * delta);                    // Starvation always damages
}
// Cold snap damage also always applies here
```

Energy recovery is correctly gated by `isUIBlocking()` (line ~517), making the inconsistency
clear: the developers knew to guard some survival effects, but forgot to guard
hunger and starvation damage.

**Consequence**: A player who opens their crafting menu to look up a recipe and leaves it open
for ~20 in-game seconds while at low hunger (common after a long build session) will take
starvation damage they have no way to prevent or respond to — they can't eat, move to find
food, or close the menu fast enough if they didn't notice the hunger bar. This is particularly
cruel given the game's British survival theme: rummaging in your bag to find a sausage roll
while slowly dying of hunger.

**Required fix in `RagamuffinGame.render()`**:

Gate hunger drain, starvation damage, and cold-snap damage behind `!isUIBlocking()`, matching
the existing energy recovery guard:

```java
if (!isUIBlocking()) {
    float hungerMultiplier = inputHandler.isSprintHeld() ? 3.0f : 1.0f;
    player.updateHunger(delta * hungerMultiplier);
    if (player.getHunger() <= 0) {
        player.damage(5.0f * delta);
    }
    // Weather energy drain multiplier
    float weatherMultiplier = weatherSystem.getCurrentWeather().getEnergyDrainMultiplier();
    player.recoverEnergy(delta / weatherMultiplier);
    // Cold snap health drain at night when unsheltered
    Weather currentWeather = weatherSystem.getCurrentWeather();
    if (currentWeather.drainsHealthAtNight() && timeSystem.isNight()) {
        boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
        if (!sheltered) {
            player.damage(currentWeather.getHealthDrainRate() * delta);
        }
    }
}
```

**Integration tests**:

1. **Hunger does not drain while inventory is open**: Give the player hunger = 60. Open
   the inventory UI (`inventoryUI.show()`). Advance the game for 30 real seconds (simulate
   via delta accumulation). Verify `player.getHunger()` is still 60 (no drain while UI open).
   Close inventory. Advance 1 second. Verify hunger has now decreased (drain resumed).

2. **Starvation cannot kill the player in their inventory**: Set player hunger to 0 and health
   to 10. Open the inventory UI. Advance the game for 5 seconds. Verify `player.getHealth()`
   is still 10 (no starvation damage while UI open). Verify the player is not dead.

3. **Cold snap cannot damage the player while UI is open**: Enable a cold-snap weather
   condition, set time to night, place player in an unsheltered location. Open inventory.
   Advance 5 seconds. Verify player health is unchanged. Close inventory. Advance 1 second.
   Verify player health has decreased (cold snap damage resumed).

4. **Hunger drains normally with UI closed**: Give the player hunger = 100. With no UI open,
   advance the game for 10 real seconds. Verify hunger has decreased (normal drain is active).

---

## Bug Fix: Cardboard shelter entrance is only 1 block tall — player cannot enter

**Status**: ❌ Broken — the cardboard shelter's entrance opening is 1 block tall, but the
player is 1.8 blocks tall. The player cannot physically enter the shelter, rendering the
entire cardboard box mechanic (the game's primary early-game survival tool) useless.

**Problem**: `BlockPlacer.buildCardboardShelter()` builds a 3×3×4 structure (width×depth×height
including roof). The front face (z=oz+2) is left open at dy=1 (y=oy+1) to form an entrance,
but the front wall blocks at dy=2 (y=oy+2) are placed across the entire front:

```java
// Front wall (z=oz+2) - only at dy=2, leaving entrance at dy=1
if (dy == 2) {
    setIfAir(world, ox,     oy + dy, oz + 2, BlockType.CARDBOARD);  // corner
    setIfAir(world, ox + 1, oy + dy, oz + 2, BlockType.CARDBOARD);  // centre — BLOCKS ENTRY
    setIfAir(world, ox + 2, oy + dy, oz + 2, BlockType.CARDBOARD);  // corner
}
```

The left wall (x=ox) and right wall (x=ox+2) also place blocks at y=oy+2, z=oz+2, further
blocking the two corner positions at head height. The result: the entire front face at head
height (y=oy+2) is solid cardboard, making the "entrance" only 1 block tall. A player with
HEIGHT=1.8 blocks and EYE_HEIGHT=1.62 blocks cannot crouch (no crouch mechanic exists), so
they physically cannot enter.

**The test that should have caught this (test5_ShelterDetectorRecognisesCardboardShelter)
is a false pass**: it teleports the player directly into the interior at `(ox+1, oy+1, oz+1)`
without simulating movement through the entrance. `ShelterDetector.isSheltered()` then
correctly detects 3 walls + roof and returns true. But the player could never reach that
interior position in actual gameplay.

**Root cause**: The Javadoc comment for `buildCardboardShelter` says "2 wide, 2 tall, 2 deep"
but the actual structure is 3×3×4 (floor + 2 wall heights + roof). The entrance was designed
with wall height dy=1 (1 block), which is insufficient for a 1.8-block-tall player.

**Required fix in `BlockPlacer.buildCardboardShelter()`**:

The entrance must be at least 2 blocks tall. Remove the centre block from the dy=2 front wall
to create a 1-wide, 2-tall entrance:

```java
// Front wall (z=oz+2) — corners only at both heights; centre left open for 2-block entrance
setIfAir(world, ox,     oy + 1, oz + 2, BlockType.CARDBOARD);  // corner, dy=1 -- was open, stays open
setIfAir(world, ox + 2, oy + 1, oz + 2, BlockType.CARDBOARD);  // corner, dy=1
setIfAir(world, ox,     oy + 2, oz + 2, BlockType.CARDBOARD);  // corner, dy=2
// ox+1, oy+2, oz+2 is left as AIR — the top of the 2-block entrance
setIfAir(world, ox + 2, oy + 2, oz + 2, BlockType.CARDBOARD);  // corner, dy=2
```

This makes the entrance 1 block wide and 2 blocks tall at x=ox+1 — sufficient for the player
to walk in. The shelter still has 3 solid walls (back, left, right) + roof for weather
protection.

**Integration test** (fix test5 and add a walkthrough test):

1. **ShelterDetector recognises player inside cardboard shelter**: Build shelter at (20, 5, 20).
   Use collision-based movement to walk the player from z=oz+3 (outside) through z=oz+2
   (entrance) to z=oz+1 (inside), at x=ox+1, y=oy (floor level). Verify `isSheltered()`
   returns true once the player is at (ox+1, oy, oz+1). This tests actual entry, not teleport.

2. **Player can walk through entrance without collision**: Build shelter at (20, 5, 20). Place
   player at (21, 6, 23) facing north (toward z=22, the open entrance face). Simulate pressing
   W for 60 frames. Verify the player's Z coordinate has decreased below oz+2 — i.e., the
   player has entered the shelter (not been blocked by head-height wall blocks).

3. **Shelter still provides weather protection after fix**: Build shelter. Walk player inside
   (via movement, not teleport). Set weather to COLD_SNAP, time to night. Health=100. Advance
   300 frames. Verify health is still 100 (shelter blocks cold snap damage).

---

## Bug Fix: ShelterDetector.isSheltered() uses Math.round() — snaps player to wrong grid cell

**Status**: ❌ Broken — `ShelterDetector.isSheltered()` uses `Math.round()` to convert the
player's continuous float position to integer block coordinates before checking for walls and
roof. This is incorrect: `Math.round(x)` rounds to the *nearest* integer, which is the block
whose center is closest — not the block the player is physically standing in.

**Problem**: Consider a player at position `(ox+1.5, oy+1.0, oz+1.0)` inside a cardboard
shelter (walls at `x=ox` and `x=ox+2`, interior gap at `x=ox+1..ox+2`):

```java
int x = Math.round(playerPosition.x); // Math.round(ox+1.5) = ox+2  ← WRONG
```

`Math.round(ox+1.5)` returns `ox+2` — the right wall block — not `ox+1`, the interior. The
detector then checks:
- left:  `(ox+1, oy+1, oz+1)` = AIR (interior) — no wall
- right: `(ox+3, oy+1, oz+1)` = AIR (exterior) — no wall

Only the back and possibly front wall are detected, giving `wallCount < 3`. The method returns
`false` even though the player is physically inside the shelter.

**Why this matters**: The cardboard shelter (the game's only early-game survival mechanic) has
a 1-block-wide interior gap at `x=ox+1..ox+2`. A player walking in via the entrance naturally
centres around `x=ox+1.5`. `Math.round(ox+1.5) = ox+2`, snapping to the right wall. The
shelter detector then fails, and cold-snap damage continues to drain the player's health even
while inside. The shelter is useless for weather protection.

**Root cause**: The player occupies block `floor(x)` for collision purposes (which the rest of
the engine uses via `Math.floor()`). `ShelterDetector` should use `Math.floor()` (or equivalently
`(int) Math.floor(x)`) to identify which block cell the player is standing in.

**Required fix in `ShelterDetector.isSheltered()`**:

Replace `Math.round()` with `Math.floor()`:

```java
int x = (int) Math.floor(playerPosition.x);
int y = (int) Math.floor(playerPosition.y);
int z = (int) Math.floor(playerPosition.z);
```

With this fix, a player at `(ox+1.5, oy+1.0, oz+1.0)` maps to cell `(ox+1, oy+1, oz+1)`:
- left:  `(ox+0, oy+1, oz+1)` = CARDBOARD ✓
- right: `(ox+2, oy+1, oz+1)` = CARDBOARD ✓
- back:  `(ox+1, oy+1, oz+0)` = CARDBOARD ✓
- roof:  `(ox+1, oy+3, oz+1)` = CARDBOARD ✓ (at y+2 from floor y=oy+1, so oy+3)

`wallCount = 3`, shelter detected correctly.

**Integration tests** (add to Critic4CardboardShelterTest):

1. **Player at x=ox+1.5 (centre of 1-block gap) is detected as sheltered**: Build cardboard
   shelter at (20, 5, 20). Set player position to `(21.5f, 6.0f, 21.0f)` — this is exactly
   the horizontal centre of the entrance gap, one block inside the shelter. Call
   `ShelterDetector.isSheltered(world, player.getPosition())`. Verify it returns `true`.
   (Previously returned `false` due to `Math.round(21.5) = 22` snapping to the right wall.)

2. **Cold-snap damage does NOT apply when player is centred in shelter interior**: Build
   shelter at (20, 5, 20). Set player position to `(21.5f, 6.0f, 21.0f)`. Set
   `WeatherSystem` to COLD_SNAP. Set health to 100. Simulate 300 frames (5 seconds) of
   cold-snap logic: `if (!ShelterDetector.isSheltered(world, pos)) player.damage(rate*delta)`.
   Verify health is still 100.

3. **Player outside shelter IS damaged by cold snap**: Place player at `(21.5f, 6.0f, 24.0f)`
   — outside the shelter. Same cold-snap simulation. Verify health < 100 (damage applied).

---

## Bug: WeatherSystem timer runs on real time, not game time

**Discovered**: 2026-02-23

**Problem**: `WeatherSystem.update()` is called with real-world `delta` (seconds), and its
duration constants are `MIN_WEATHER_DURATION = 300f` / `MAX_WEATHER_DURATION = 600f` real
seconds. The spec says weather changes every "5-10 game minutes."

With `TimeSystem.DEFAULT_TIME_SPEED = 0.1f` hours/real-second, one in-game day takes
`24 / 0.1 = 240` real seconds ≈ 4 real minutes. So 300–600 real seconds = roughly 1.25–2.5
in-game days between weather changes. Players can experience many in-game days — and entire
play sessions — without the weather ever changing.

**Fix**: `WeatherSystem.update()` should receive game-time seconds (i.e., `delta *
timeSystem.getTimeSpeed() * 3600f`), and the duration constants should use game-minutes
(e.g., 5 game-minutes = 300 game-seconds). Alternatively, adjust the constants from 300–600
real-seconds to a value appropriate for the intended player-facing pacing: approximately
30–60 real seconds (≈ 3–6 game-minutes at 0.1 hours/s time speed).

**Desired behaviour**: Weather state changes approximately every 5-10 in-game minutes of
elapsed game-world time — meaning within a single in-game day (4 real minutes) the player
should expect to see 2–6 weather transitions.

**Integration test** (add to Phase12IntegrationTest):

**Weather changes within one game-day**: Create `WeatherSystem`. Create `TimeSystem`.
Advance time by simulating frames at 60fps for 300 real seconds (`timeSystem.update(1/60f)`
each frame, `weatherSystem.update(1/60f * timeSystem.getTimeSpeed() * 3600f)` each frame).
Record the number of weather changes that occurred. Verify at least 2 weather changes
happened (weather should change multiple times per in-game day, not once every 1-2 days).

---

## Bug Fix: HealingSystem ticks while UI is open — free healing exploit

**Discovered**: 2026-02-23

**Problem**: `RagamuffinGame.render()` correctly gates hunger drain, starvation damage,
cold-snap damage, and energy recovery behind `!isUIBlocking()` (lines ~506–530). However,
`healingSystem.update(delta, player)` sits immediately after that guarded block (line ~533)
and runs unconditionally — every frame, regardless of whether the inventory, crafting menu,
or help screen is open.

When a UI is open, `updatePlaying()` is also skipped (no input is processed, the player
cannot move). `HealingSystem` checks whether the player has been stationary for ≥ 5 seconds
before healing begins. Because the player cannot move while the inventory is open, the resting
timer accumulates trivially, and within 5 seconds the player begins healing at 5 HP/s.

Simultaneously, hunger drain is **paused** (correctly gated). The result: a player can open
their inventory at low health, do nothing for a few seconds, and heal fully — with zero
hunger cost. The hunger mechanic (the primary survival pressure) is completely bypassed.

**Exploit scenario**:
1. Player is at 10 HP, 90 hunger.
2. Player opens inventory (I key).
3. After 5 seconds, healing begins: 5 HP/s. Full heal to 100 HP in ~18 seconds.
4. Hunger remains at 90 throughout — no cost incurred.
5. Player closes inventory and resumes play, fully healed for free.

**Root cause**: `healingSystem.update()` was placed outside the `if (!isUIBlocking())` guard
in `RagamuffinGame.render()`. The guard at line ~506 already correctly patterns the other
survival-stat updates. The healing call just needs to be moved inside the same guard.

**Required fix in `RagamuffinGame.render()`**:

Move `healingSystem.update(delta, player)` inside the `if (!isUIBlocking())` block:

```java
// Update player survival stats (gated: no hunger/starvation/cold-snap/healing while UI is open)
if (!isUIBlocking()) {
    // Sprint drains hunger 3x faster
    float hungerMultiplier = inputHandler.isSprintHeld() ? 3.0f : 1.0f;
    player.updateHunger(delta * hungerMultiplier);

    // Starvation: zero hunger drains health at 5 HP/s
    if (player.getHunger() <= 0) {
        player.damage(5.0f * delta);
    }

    // Phase 12: Apply weather energy drain multiplier
    float weatherMultiplier = weatherSystem.getCurrentWeather().getEnergyDrainMultiplier();
    player.recoverEnergy(delta / weatherMultiplier);

    // Phase 12: Cold snap health drain at night when unsheltered
    Weather currentWeather = weatherSystem.getCurrentWeather();
    if (currentWeather.drainsHealthAtNight() && timeSystem.isNight()) {
        boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());
        if (!sheltered) {
            float healthDrain = currentWeather.getHealthDrainRate() * delta;
            player.damage(healthDrain);
        }
    }

    // Phase 11: Update healing system (must be inside UI guard — healing while menu open is an exploit)
    healingSystem.update(delta, player);
}
```

**Integration tests** (add to Issue58IntegrationTest or Phase11IntegrationTest):

1. **Healing does NOT occur while inventory is open**: Set player health to 50, hunger to
   100. Open the inventory UI. Simulate 300 frames (5 seconds at 60fps) using the gated
   update pattern: `if (!isUIBlocking()) { healingSystem.update(delta, player); }`. Verify
   `player.getHealth()` is still 50. Verify `healingSystem.getRestingTime()` is 0 (timer
   must not accumulate while UI is open).

2. **Healing resumes normally once UI is closed**: Player health 50, hunger 100. Open
   inventory. Simulate 300 frames (UI open — no healing). Close inventory. Simulate 400
   more frames (6.7 seconds stationary). Verify `player.getHealth() > 50` (healing has
   begun after the 5-second resting requirement is met).

3. **Hunger is required for healing even without the exploit**: Player health 50, hunger 40
   (below the 50 threshold). UI closed. Simulate 400 frames stationary. Verify health is
   still 50 — hunger requirement prevents healing regardless of UI state.

---

## Bug Fix: Council builder demolishBlock() never rebuilds chunk mesh — demolished blocks remain visible as ghost geometry

**Discovered**: 2026-02-23

**Status**: ❌ Broken — when a council builder NPC demolishes a block via `NPCManager.demolishBlock()`,
the block is removed from the world data (`world.setBlock(x, y, z, BlockType.AIR)`) but the chunk mesh
is never marked dirty and never rebuilt. The demolished block disappears from the collision system (the
player can walk through it) but remains fully visible in the 3D scene as solid geometry. The player
experiences ghost blocks — solid-looking voxels they can pass through — wherever council builders have
demolished their shelter.

**Problem**: `World.setBlock()` intentionally does NOT auto-dirty chunks (callers are responsible for
triggering mesh rebuilds). Every other block removal in the game correctly schedules a rebuild:

- `RagamuffinGame.handlePunch()` calls `rebuildChunkAt(x, y, z)` after every block break
- `RagamuffinGame.handlePlace()` calls `rebuildChunkAt(...)` after every block placement

But `NPCManager.demolishBlock()` calls only:

```java
world.setBlock(x, y, z, BlockType.AIR);
structure.removeBlock(blockToRemove);
structureTracker.removeBlock(x, y, z);
world.removePlanningNotice(x, y, z);
```

There is no call to mark the chunk dirty. `NPCManager` holds no reference to `RagamuffinGame` (correct
separation of concerns), so it cannot call `rebuildChunkAt()` directly. `World` would need to expose
a `markChunkDirtyAt(int x, int y, int z)` helper, or `demolishBlock()` needs to be called from the
game loop where the rebuild can be scheduled.

**Consequence**: The council builder system (the Phase 7 antagonist, the game's primary threat after
the player establishes a shelter) is visually broken. Players see their shelter standing intact while
simultaneously being able to walk through the walls — a deeply confusing experience that undermines
the entire survival loop. The effect looks like a rendering bug rather than intentional gameplay,
eroding player trust in the physics system.

**Required fix**: Add a public `markBlockDirty(int x, int y, int z)` method to `World` that
translates world coordinates to chunk coordinates and marks that chunk (and any boundary neighbours)
dirty:

```java
// World.java
public void markBlockDirty(int x, int y, int z) {
    int chunkX = Math.floorDiv(x, Chunk.SIZE);
    int chunkY = Math.floorDiv(y, Chunk.HEIGHT);
    int chunkZ = Math.floorDiv(z, Chunk.SIZE);
    markChunkDirty(chunkX, chunkY, chunkZ);
    // Also dirty neighbours if on chunk boundary
    int localX = Math.floorMod(x, Chunk.SIZE);
    int localY = Math.floorMod(y, Chunk.HEIGHT);
    int localZ = Math.floorMod(z, Chunk.SIZE);
    if (localX == 0) markChunkDirty(chunkX - 1, chunkY, chunkZ);
    if (localX == Chunk.SIZE - 1) markChunkDirty(chunkX + 1, chunkY, chunkZ);
    if (localY == 0) markChunkDirty(chunkX, chunkY - 1, chunkZ);
    if (localY == Chunk.HEIGHT - 1) markChunkDirty(chunkX, chunkY + 1, chunkZ);
    if (localZ == 0) markChunkDirty(chunkX, chunkY, chunkZ - 1);
    if (localZ == Chunk.SIZE - 1) markChunkDirty(chunkX, chunkY, chunkZ + 1);
}
```

Then call it in `NPCManager.demolishBlock()` immediately after `world.setBlock(x, y, z, BlockType.AIR)`:

```java
world.setBlock(x, y, z, BlockType.AIR);
world.markBlockDirty(x, y, z);  // Trigger mesh rebuild so demolished block disappears visually
```

**Integration tests** (add to next Critic integration test suite):

1. **Demolished block disappears from chunk mesh**: Build a 6-block WOOD structure. Force a
   structure scan (`npcManager.forceStructureScan(world, tooltipSystem)`). Advance 60 frames
   (1 second) to let a builder reach and demolish one block. Call `world.getDirtyChunks()`.
   Verify the chunk containing the demolished block is in the dirty set — confirming a mesh
   rebuild has been scheduled.

2. **Ghost block regression**: Build a WOOD structure at (10, 2, 10). Run the full council
   builder pipeline until one block is demolished (track via `structureTracker.getStructures()`
   block count decreasing by 1). Call `world.getBlock(demolishedX, demolishedY, demolishedZ)`.
   Verify it returns `BlockType.AIR`. Verify the chunk containing that position is in
   `world.getDirtyChunks()` — not clean — so the renderer will update on the next frame.

3. **Player can no longer collide with demolished block**: After demolition and mesh rebuild,
   place the player adjacent to the demolished position. Simulate pressing W for 30 frames.
   Verify the player moves into the space where the block was (no collision barrier remains).

---

## Bug Fix: RespawnSystem hardcodes Y=1 — player respawns inside terrain

**Discovered**: 2026-02-23

**Problem**: `RespawnSystem.PARK_CENTRE` is `new Vector3(0, 1, 0)`. On death,
`performRespawn()` teleports the player to Y=1 unconditionally. But the terrain height at
(0, 0) is determined dynamically by `RagamuffinGame.calculateSpawnHeight()`, which scans
from Y=64 downward and returns `y + 1` for the first solid block found.

The park is generated with grass blocks. If those blocks sit at Y=1 (a common outcome
depending on the world generator's terrain offset), the top face of those blocks is at
Y=2.0. Respawning the player at Y=1 places them **inside** the solid block — feet at 1.0,
block occupying [1, 2]. The collision system immediately detects the overlap, but the
resolution is undefined: the player may be flung upward, pushed sideways, or fall through
the floor depending on which collision axes are resolved first.

Even when the terrain is at Y=0, `calculateSpawnHeight` returns 1 and `spawnY = 2`, but
`PARK_CENTRE.y = 1` puts the respawn 1 block below the initial spawn height — meaning the
player respawns 1 block inside the ground, not on top of it.

**Root cause**: `PARK_CENTRE.y` was set to 1 as a placeholder and never linked to the
actual terrain-aware spawn calculation that `initGame()` and `restartGame()` both use
correctly via `calculateSpawnHeight(world, 0, 0) + 1.0f`.

**Impact**: Every player death causes a broken respawn. Since survival games frequently
end in death — especially in the early game when the player is learning the mechanics —
this is triggered constantly. A respawn that drops the player into solid geometry or
causes erratic physics immediately destroys player confidence and makes the game feel
broken.

**Required fix in `RespawnSystem.java`**:

Remove the hardcoded `PARK_CENTRE` constant. Instead, pass the computed terrain height
into the respawn method, or compute it inside `performRespawn()`:

Option A — pass spawn Y from the game loop (preferred, keeps `RespawnSystem` world-agnostic):

```java
// RagamuffinGame.java — inside the respawn completion callback / update block:
if (wasRespawning && !respawnSystem.isRespawning()) {
    float respawnY = calculateSpawnHeight(world, 0, 0) + 1.0f;
    respawnSystem.setRespawnY(respawnY);
    deathMessage = null;
}
```

Option B — calculate inside `RespawnSystem` (simpler but couples it to `World`):

```java
private void performRespawn(Player player, World world) {
    float groundY = /* world.calculateSpawnHeight(0, 0) */ + 1.0f;
    player.getPosition().set(0, groundY, 0);
    ...
}
```

Either approach ensures the respawn Y matches the actual terrain surface.

**Integration tests** (add to Phase11IntegrationTest or a new RespawnSystemTest):

1. **Player does not respawn inside terrain**: Generate the world. Kill the player (set
   health to 0). Advance respawn timer to completion. Verify `player.getPosition().y` is
   greater than the Y-coordinate of the solid ground block at (0, z) — i.e., the player's
   feet are above, not inside, the terrain surface. Verify `world.getBlock(0,
   (int) player.getPosition().y - 1, 0).isSolid()` is true (player is standing on solid
   ground) and `world.getBlock(0, (int) player.getPosition().y, 0) == BlockType.AIR`
   (player's foot block is air, not inside a solid block).

2. **Player respawns at park centre (0, 0)**: After respawn, verify X and Z coordinates
   are 0 (park centre). Verify health is 50, hunger is 50, energy is 100, and `isDead()`
   is false.

3. **Respawn is consistent across multiple deaths**: Kill the player twice in the same
   session. Verify both respawns place the player at the same correct Y coordinate.
   Verify no terrain clipping occurs on either respawn.

---

## Bug Fix: AGGRESSIVE NPCs don't chase the player — missing movement case in updateNPC()

**Discovered**: 2026-02-23

**Status**: ❌ Broken — when any non-police, non-council-builder NPC (specifically
`YOUTH_GANG`) is set to `NPCState.AGGRESSIVE`, the `updateNPC()` method's state
switch has no `case AGGRESSIVE:` branch. The NPC falls through to `default:
updateWandering()` and wanders randomly instead of chasing the player.

**Problem**: In `NPCManager.updateNPC()` (the per-NPC switch at the bottom of the
method), the handled cases are:

```java
switch (npc.getState()) {
    case FLEEING:             updateFleeing(...); break;
    case WANDERING:           updateWandering(...); break;
    case GOING_TO_WORK:
    case GOING_HOME:
    case AT_PUB:
    case AT_HOME:             updateDailyRoutine(...); break;
    case STARING:
    case PHOTOGRAPHING:
    case COMPLAINING:         updateReactingToStructure(...); break;
    case STEALING:            updateStealing(...); break;
    default:                  updateWandering(...); break;  // ← AGGRESSIVE lands here
}
```

`NPCState.AGGRESSIVE` is never listed. AGGRESSIVE NPCs wander randomly.

**How it's triggered**: Two code paths set non-police NPCs to AGGRESSIVE:

1. `GangTerritorySystem.makeNearbyGangsAggressive()` — called when the player
   lingers in a gang territory for `LINGER_THRESHOLD_SECONDS` (5 seconds) or
   attacks a gang member. All `YOUTH_GANG` NPCs within 30 blocks become AGGRESSIVE.

2. `GangTerritorySystem.onPlayerAttacksGang()` — called directly from
   `RagamuffinGame.handlePunch()` when the player punches a `YOUTH_GANG` NPC.

Both paths work correctly to set the state, but the movement never changes.

**Note on the attack mechanic**: The NPC attack code in `NPCManager.update()` runs
OUTSIDE the state switch and DOES correctly trigger for `YOUTH_GANG` because
`YOUTH_GANG.isHostile() == true`. So hostile gang members CAN damage the player if
they happen to wander within 1.8 blocks. However, they don't pursue the player
after becoming AGGRESSIVE — they just drift randomly, making the territorial
hostility feel like a bug rather than a threat.

**Impact**: The gang territory system (Phase 14) is the primary environmental
hazard on the south side of the map. When the player enters "Bricky Estate" or
"South Patch", the tooltip fires correctly, the linger timer counts down correctly,
and the AGGRESSIVE state is set correctly — but then nothing changes. Gang members
continue their lazy wander. The player can stand in a gang territory indefinitely
without any escalating threat. The entire mechanic is cosmetic.

**Required fix**: Add `case AGGRESSIVE:` to the switch in `NPCManager.updateNPC()`
that calls a new `updateAggressive(npc, delta, world, player)` helper:

```java
case AGGRESSIVE:
    updateAggressive(npc, delta, world, player);
    break;
```

```java
private void updateAggressive(NPC npc, float delta, World world, Player player) {
    // Chase the player directly
    setNPCTarget(npc, player.getPosition(), world);
    // If the player escapes beyond a generous range, de-escalate back to WANDERING
    if (npc.getPosition().dst(player.getPosition()) > 40.0f) {
        npc.setState(NPCState.WANDERING);
    }
}
```

**Integration tests** (add to the gang territory test suite):

1. **AGGRESSIVE gang member moves toward player, not randomly**: Spawn a YOUTH_GANG
   NPC at (−50, 1, −30). Place the player at (−48, 1, −30) (2 blocks away). Set the
   NPC state to AGGRESSIVE. Simulate 60 frames (1 second at 60fps). Verify the NPC's
   position has moved closer to the player (distance to player has decreased compared
   to its starting distance of 2 blocks), not drifted away.

2. **Territory-triggered hostility causes gangs to chase**: Use the headless game.
   Move the player into the "Bricky Estate" territory (−50, 1, −30). Simulate
   `LINGER_THRESHOLD_SECONDS + 2` seconds. Verify at least one nearby YOUTH_GANG NPC
   is in `NPCState.AGGRESSIVE`. Simulate an additional 60 frames. Verify that
   AGGRESSIVE NPC's distance to the player has decreased (it is chasing).

3. **De-escalation when player escapes**: Set a YOUTH_GANG NPC to AGGRESSIVE at
   (−50, 1, −30). Move the player to (10, 1, 10) (far away, >40 blocks). Simulate
   60 frames. Verify the NPC reverts to `NPCState.WANDERING` (no longer AGGRESSIVE).

---

## Bug Fix: Police ignore shelter — ShelterDetector never called in police patrol AI

**Discovered**: 2026-02-23

**Status**: ❌ Broken — police approach and arrest the player even when they are inside
a cardboard shelter. The `ShelterDetector` class is implemented and correctly used for
weather effects, but is never consulted by the police AI. The core Phase 12 mechanic —
"inside a shelter at night, police cannot see you" — is completely non-functional.

**Problem**: `NPCManager.updatePolicePatrolling()` chases the player and issues warnings
with no shelter check:

```java
private void updatePolicePatrolling(NPC police, float delta, World world, Player player, ...) {
    // ...
    // Approach player — no shelter check here
    setNPCTarget(police, player.getPosition(), world);

    // Check if adjacent to player - issue warning or go aggressive
    if (police.isNear(player.getPosition(), 2.0f)) {
        // ... warn or arrest — still no shelter check
    }
}
```

`ShelterDetector.isSheltered(world, playerPosition)` already exists and works correctly
— it checks for a solid roof block 2 above the player plus at least 3 solid wall blocks
on the cardinal sides. It is called from `RagamuffinGame.render()` for cold snap health
drain, but is never imported or called from `NPCManager`.

**Impact**: The cardboard shelter (crafted from 6 CARDBOARD, placed as a 2×2×2 structure)
is the player's primary early-game protection mechanic. Without the shelter bypass, the
only survival strategy at night is to run — crafting a shelter and hiding inside it does
nothing. The Phase 12 integration test ("Cardboard shelter hides from police") will always
fail because police walk straight through the shelter roof to arrest the player.

**Required fix**: In `NPCManager.updatePolicePatrolling()`, check `ShelterDetector.isSheltered()`
before chasing or warning the player. If the player is sheltered, the police NPC should
skip past (continue wandering/patrolling) rather than approaching:

```java
private void updatePolicePatrolling(NPC police, float delta, World world, Player player, ...) {
    // Phase 12: Police cannot detect sheltered player
    if (ShelterDetector.isSheltered(world, player.getPosition())) {
        // Player is hiding — police wander past without noticing
        updateWandering(police, delta, world);
        return;
    }
    // ... rest of existing patrol logic
}
```

The same check should also apply at the start of `updatePoliceWarning()` — if the player
ducks into a shelter while police are in WARNING state, the warning should be cancelled
and the police should revert to PATROLLING.

**Integration tests** (regression — verify shelter provides police protection):

1. **Sheltered player is not approached by police**: Place a CARDBOARD_BOX shelter
   (2×2×2 of solid blocks with a roof). Put the player inside. Set time to 22:00 to
   allow police spawning. Spawn a police NPC 15 blocks away and set it to PATROLLING.
   Simulate 600 frames. Verify the police NPC's distance to the player has NOT decreased
   (it did not approach). Verify `arrestPending` is false.

2. **Unsheltered player IS approached by police**: Same setup but player is outside the
   shelter (or shelter is removed). Set time to 22:00. Advance 600 frames. Verify the
   police NPC has moved closer to the player. Verify `arrestPending` eventually becomes
   true (or police enters WARNING state).

3. **Player ducks into shelter cancels police warning**: Spawn police in WARNING state
   adjacent to player. Move the player into a shelter. Advance 120 frames. Verify police
   state has reverted to PATROLLING (warning cancelled) and police distance is not
   decreasing.

## Bug Fix: Shelter provides no protection against already-aggressive police

**Discovered**: 2026-02-23

**Status**: ❌ Broken — `NPCManager.updatePoliceAggressive()` has no shelter check. Police
that transition to AGGRESSIVE state (from a Greggs raid alert, notorious-player detection, or
from WARNING escalation) continue to chase and arrest the player even after the player enters a
cardboard shelter. The shelter mechanic only works against PATROLLING and WARNING police, which
means the moment any police officer escalates to AGGRESSIVE the shelter is entirely useless.

**Problem**: The fix in commit d65c870 added `ShelterDetector.isSheltered()` checks to
`updatePolicePatrolling()` and `updatePoliceWarning()` but left `updatePoliceAggressive()`
unguarded:

```java
private void updatePoliceAggressive(NPC police, float delta, World world, Player player) {
    // Move toward player — NO shelter check here
    setNPCTarget(police, player.getPosition(), world);

    if (police.isNear(player.getPosition(), 1.5f) && !arrestPending) {
        arrestPending = true;  // Arrests player even inside shelter
        ...
    }
}
```

**How aggressive police are triggered**:
1. `alertPoliceToGreggRaid()` — called when the player smashes a Greggs block; sets
   nearby police directly to AGGRESSIVE with no shelter pre-check.
2. `updatePolicePatrolling()` notorious branch — but this is now guarded, so shelter is
   respected here. However, if the police was already AGGRESSIVE before the player entered
   the shelter, there is no route back out.
3. `updatePoliceWarning()` escalation — if the player fails to comply, police goes
   AGGRESSIVE. The WARNING→shelter cancel was fixed, but once AGGRESSIVE the shelter is
   ignored.

**Impact**: The cardboard shelter is the player's only early-game protection from police
harassment at night. A Greggs raid or any prior confrontation that escalates police to
AGGRESSIVE permanently negates the shelter. The player's only option is to keep running
indefinitely — crafting and hiding in a shelter does nothing once the situation heats up.

**Required fix**: Add a `ShelterDetector.isSheltered()` check at the top of
`updatePoliceAggressive()`. If the player is sheltered, the police NPC should abandon the
chase and revert to PATROLLING (not WANDERING — they should still be on duty, just unable
to see the player):

```java
private void updatePoliceAggressive(NPC police, float delta, World world, Player player) {
    // Phase 12: Player inside shelter — police lose sight and return to patrol
    if (ShelterDetector.isSheltered(world, player.getPosition())) {
        police.setState(NPCState.PATROLLING);
        return;
    }
    // ... existing chase/arrest logic
}
```

**Integration tests** (regression — aggressive police lose target when player shelters):

1. **Aggressive police abandon chase when player enters shelter**: Build a 2×2×2 shelter
   at world position (20, 5, 20). Spawn a police NPC 5 blocks away in AGGRESSIVE state
   with target set to the player's unsheltered position. Move the player inside the shelter.
   Simulate 120 frames. Verify the police NPC's state has reverted to PATROLLING and
   `arrestPending` is false.

2. **Greggs-raid police cannot arrest sheltered player**: Set up a shelter at (20, 5, 20).
   Place player inside. Call `npcManager.alertPoliceToGreggRaid(player, world)` to spawn
   an AGGRESSIVE police unit. Simulate 600 frames. Verify `arrestPending` remains false
   and the player's inventory is unchanged (no confiscation occurred).

3. **Aggressive police resume chase when player leaves shelter**: Aggressive police, player
   inside shelter — police reverts to PATROLLING (test 1). Player then steps out. Advance
   60 frames. Verify police transitions back to AGGRESSIVE (or WARNING) and begins
   approaching the player again.

---

## Bug Fix: YOUTH_GANG stealing logic overrides AGGRESSIVE chase state

**Discovered**: 2026-02-23

**Status**: ❌ Broken — in `NPCManager.updateNPC()`, the stealing check for YOUTH_GANG NPCs
runs after `updateAggressive()` but does not exclude AGGRESSIVE state. When an AGGRESSIVE
gang member closes to within 2.0 blocks of the player (attack range), the steal block
overrides the state to STEALING, which immediately steals one item and returns to
WANDERING — cancelling the chase entirely after a single hit.

**Problem**: In `NPCManager.updateNPC()`, after the main state switch, this block runs:

```java
// Youth gangs try to steal
if (npc.getType() == NPCType.YOUTH_GANG && npc.getState() != NPCState.STEALING) {
    if (npc.isNear(player.getPosition(), 2.0f)) {
        npc.setState(NPCState.STEALING);   // ← fires even when AGGRESSIVE
    } else if (npc.isNear(player.getPosition(), 20.0f)) {
        setNPCTarget(npc, player.getPosition(), world);
    }
}
```

`npc.getState() != NPCState.STEALING` does not guard against AGGRESSIVE. When an
AGGRESSIVE NPC reaches 2.0 blocks — the distance needed to deliver its 8-damage attack —
it is immediately redirected to STEALING mode. `updateStealing()` takes one item and then
calls `npc.setState(NPCState.WANDERING)`, so the NPC stops pursuing on the very next tick.

The attack code in `NPCManager.update()` runs OUTSIDE the state switch (based on
`npc.getType().isHostile()` and proximity), so the gang member may land one hit at the
moment of range crossing, but after that tick it is WANDERING and the chase is over.

**Impact**: The gang territory mechanic (Phase 14) relies on YOUTH_GANG members actually
chasing and threatening the player once escalated to AGGRESSIVE. With this bug, an
aggressive gang member closes to attack range, pickpockets one WOOD item, and wanders
away. The player receives one hit and loses one resource — but not the sustained pressure
of a pursuing enemy. Gangs feel like passive thieves rather than a genuine territorial
threat. This also makes the dodge/roll mechanic pointless against gangs.

**Required fix**: Exclude `NPCState.AGGRESSIVE` (and `NPCState.FLEEING`) from the stealing
override so that gangs in combat or flight mode do not switch to pickpocket mode:

```java
if (npc.getType() == NPCType.YOUTH_GANG
        && npc.getState() != NPCState.STEALING
        && npc.getState() != NPCState.AGGRESSIVE
        && npc.getState() != NPCState.FLEEING) {
    if (npc.isNear(player.getPosition(), 2.0f)) {
        npc.setState(NPCState.STEALING);
    } else if (npc.isNear(player.getPosition(), 20.0f)) {
        setNPCTarget(npc, player.getPosition(), world);
    }
}
```

**Integration tests**:

1. **AGGRESSIVE gang member does not switch to STEALING at attack range**: Spawn a
   YOUTH_GANG NPC at (−50, 1, −30). Place the player at (−50, 1, −29) (1 block away).
   Set the NPC to `NPCState.AGGRESSIVE`. Give the player 5 WOOD items. Simulate 10 frames.
   Verify the NPC's state remains AGGRESSIVE (not STEALING or WANDERING). Verify the
   player still has 5 WOOD items (no theft occurred while chasing).

2. **AGGRESSIVE gang member continues chasing after reaching attack range**: Spawn a
   YOUTH_GANG NPC at (−50, 1, −35). Place the player at (−50, 1, −30) (5 blocks away).
   Set the NPC to AGGRESSIVE. Simulate 120 frames (2 seconds). Verify the NPC has moved
   significantly closer to the player (distance decreased by at least 2 blocks). Verify
   the NPC state is still AGGRESSIVE (not WANDERING) when the player is within range.

3. **Gang still steals when WANDERING and adjacent**: Spawn a YOUTH_GANG NPC in
   WANDERING state adjacent to the player. Give the player 3 WOOD. Advance until the NPC
   is within 2 blocks. Simulate 30 frames. Verify the NPC switched to STEALING and the
   player lost 1 WOOD item (stealing still works in non-combat state).

---

## Bug Fix: Patrolling police attack player unconditionally due to `isHostile()` flag

**Discovered**: 2026-02-23

**Status**: ❌ Broken — in `NPCManager.update()`, the NPC attack check uses
`npc.getType().isHostile()` as the primary condition for POLICE to attack. Because
`NPCType.POLICE` is declared with `hostile = true`, a police NPC in **any** state
(PATROLLING, WARNING, IDLE) will punch the player for 10 HP every second whenever
the player walks within 1.8 blocks — regardless of reputation or prior behaviour.

**Problem**: In `NPCManager.update()`:

```java
if (npc.getType().isHostile() && npc.isNear(player.getPosition(), attackRange)) {
    shouldAttack = true;
}
```

`isHostile()` returns `true` for POLICE unconditionally. A PATROLLING officer strolling
through the park will deal 10 HP/s to any player who stands within 1.8 blocks. With a
1-second attack cooldown and the player having 100 HP, the player dies in 10 seconds of
casual proximity — before any warning is issued.

The `isHostile` flag was intended to mean "will seek out and fight the player" (like
YOUTH_GANG), but for POLICE it should mean "will escalate when warranted." The attack
logic should only fire when the police NPC is in AGGRESSIVE or ARRESTING state, not
during PATROLLING or WARNING.

**Required fix**: Gate the `isHostile()` attack path on combat-active states so that
passive police (PATROLLING, WARNING) only deal damage when AGGRESSIVE or ARRESTING:

```java
boolean inCombatState = npc.getState() == NPCState.AGGRESSIVE
    || npc.getState() == NPCState.ARRESTING;
if ((npc.getType().isHostile() && inCombatState && npc.isNear(player.getPosition(), attackRange))
        || (npc.getState() == NPCState.AGGRESSIVE && npc.isNear(player.getPosition(), attackRange))) {
    shouldAttack = true;
}
```

Or more cleanly: only treat non-POLICE hostile types (YOUTH_GANG) as always-attacking
based on the flag; gate POLICE attacks behind state:

```java
if (npc.getType() == NPCType.POLICE) {
    // Police only attack when actively pursuing/arresting
    if ((npc.getState() == NPCState.AGGRESSIVE || npc.getState() == NPCState.ARRESTING)
            && npc.isNear(player.getPosition(), attackRange)) {
        shouldAttack = true;
    }
} else if (npc.getType().isHostile() && npc.isNear(player.getPosition(), attackRange)) {
    shouldAttack = true;
} else if (npc.getState() == NPCState.AGGRESSIVE && npc.isNear(player.getPosition(), attackRange)) {
    shouldAttack = true;
}
```

**Integration tests**:

1. **Patrolling police does not attack player**: Spawn a POLICE NPC in PATROLLING state
   1.0 blocks from the player (within attack range 1.8). Give the player 100 HP. Simulate
   60 frames (1 second). Verify the player's health is still 100 HP (no damage from
   patrolling police).

2. **AGGRESSIVE police attacks player in range**: Spawn a POLICE NPC in AGGRESSIVE state
   1.0 blocks from the player. Simulate 10 frames. Verify the player's health has
   decreased (police in AGGRESSIVE state should deal damage).

3. **WARNING police does not attack player**: Spawn a POLICE NPC in WARNING state 1.0
   blocks from the player. Simulate 60 frames. Verify the player's health is unchanged
   (warning-phase police should not deal damage).

---

## Bug Fix: Police-taped blocks can still be broken — `BlockBreaker` never checks `world.isProtected()`

**Discovered**: 2026-02-23

**Status**: ❌ Broken — `BlockBreaker.punchBlock()` does not call `world.isProtected(x, y, z)`
before processing a hit. When police apply tape to a player's structure, the blocks are added
to `World.protectedBlocks` (via `world.addPoliceTape()`), but the block breaker reads only
`blockType.isSolid()` and `BlockType.BEDROCK`. A player can punch through a taped block in
the normal number of hits and remove it as if the tape were never there.

**Problem**: In `BlockBreaker.punchBlock()`:

```java
public boolean punchBlock(World world, int x, int y, int z, Material tool) {
    BlockType blockType = world.getBlock(x, y, z);

    // Can't punch air or bedrock
    if (blockType == BlockType.AIR || !blockType.isSolid() || blockType == BlockType.BEDROCK) {
        return false;
    }
    // ← no check for world.isProtected(x, y, z) here
    ...
}
```

`world.isProtected()` exists and correctly returns `true` for taped blocks, but is never
consulted. The Phase 6 integration test (`test5_PoliceTapePlayerStructure`) only verifies
`world.isProtected()` returns `true` — it does NOT call `punchBlock()` on the taped block
and verify the break is refused. The protection is therefore a data illusion with no gameplay
effect: the player can punch straight through police tape.

**Required fix**: Add an `isProtected` guard at the top of `BlockBreaker.punchBlock()`:

```java
public boolean punchBlock(World world, int x, int y, int z, Material tool) {
    BlockType blockType = world.getBlock(x, y, z);

    // Can't punch air, non-solid, bedrock, or police-taped blocks
    if (blockType == BlockType.AIR || !blockType.isSolid() || blockType == BlockType.BEDROCK
            || world.isProtected(x, y, z)) {
        return false;
    }
    ...
}
```

**Integration tests**:

1. **Taped block cannot be broken by the player**: Place a WOOD block at (5, 1, 5). Call
   `world.addPoliceTape(5, 1, 5)`. Verify `world.isProtected(5, 1, 5)` returns `true`.
   Call `blockBreaker.punchBlock(world, 5, 1, 5, null)` 10 times (more than the 5 hits
   normally required). Verify the block at (5, 1, 5) is still WOOD (not AIR). Verify
   `punchBlock()` returned `false` on every call.

2. **Untaped block at same position CAN be broken normally**: Place a WOOD block at (5, 1, 5)
   without taping it. Punch it 5 times. Verify it is now AIR (normal break behaviour).

3. **Removing tape restores breakability**: Place and tape a WOOD block at (5, 1, 5). Punch
   it 3 times (returns false each time — protection active). Call `world.removePoliceTape(5, 1, 5)`.
   Punch it 5 more times. Verify the block breaks on the 5th punch (hit counter reset to 0
   by the protection guard, now normal 5-hit break from fresh state).

---

## Bug Fix: `demolishBlock()` does not clear police tape — demolished blocks leave ghost protection entries

**Discovered**: 2026-02-23

**Status**: ❌ Broken — when a council builder demolishes a police-taped block via
`NPCManager.demolishBlock()`, the position is removed from the world and from `StructureTracker`,
but it is **not** removed from `World.protectedBlocks`. Any new block placed at that position by
the player is then permanently protected (unbreakable by the player), because `BlockBreaker.punchBlock()`
checks `world.isProtected()` and returns `false` unconditionally.

**Problem**: In `NPCManager.demolishBlock()`:

```java
world.setBlock(x, y, z, BlockType.AIR);
world.markBlockDirty(x, y, z);
structure.removeBlock(blockToRemove);
structureTracker.removeBlock(x, y, z);
if (blockBreaker != null) {
    blockBreaker.clearHits(x, y, z);
}
world.removePlanningNotice(x, y, z);
// ← world.removePoliceTape(x, y, z) is MISSING here
```

`world.removePlanningNotice()` is correctly called, but `world.removePoliceTape()` is not.
The `protectedBlocks` set in `World` is populated by `world.addPoliceTape()` (called from
`applyPoliceTapeToStructure()` in `NPCManager`) and only cleared by `world.removePoliceTape()`.
Since `demolishBlock()` never calls `removePoliceTape()`, the position stays in `protectedBlocks`
indefinitely — a ghost entry for a block that no longer exists.

The consequence: if the player rebuilds at the same location (likely, since it was their structure),
the new block is permanently shielded from the player's own punches. The player cannot reclaim
the position they themselves built on.

**Required fix**: Add `world.removePoliceTape(x, y, z)` to `demolishBlock()` after setting the block
to AIR:

```java
world.setBlock(x, y, z, BlockType.AIR);
world.markBlockDirty(x, y, z);
world.removePoliceTape(x, y, z);   // ← clear ghost protection entry
structure.removeBlock(blockToRemove);
structureTracker.removeBlock(x, y, z);
if (blockBreaker != null) {
    blockBreaker.clearHits(x, y, z);
}
world.removePlanningNotice(x, y, z);
```

**Integration tests**:

1. **Demolished taped block does not permanently protect future placements**: Place a WOOD block
   at (5, 1, 5). Call `world.addPoliceTape(5, 1, 5)`. Verify `world.isProtected(5, 1, 5)` is
   `true`. Simulate a council builder demolishing the block by calling `world.setBlock(5, 1, 5,
   BlockType.AIR)` and `world.removePoliceTape(5, 1, 5)` (the fixed path). Verify
   `world.isProtected(5, 1, 5)` is now `false`. Place a new WOOD block at (5, 1, 5). Punch it
   5 times. Verify it is now AIR (the new block was breakable — no ghost protection).

2. **Ghost protection regression (unfixed path)**: Reproduce without the fix — place, tape, then
   call only `world.setBlock(5, 1, 5, BlockType.AIR)` without `removePoliceTape`. Verify
   `world.isProtected(5, 1, 5)` is still `true`. Place a new WOOD block. Punch it 10 times.
   Verify it is still WOOD (ghost protection blocks all punches).

---

## Bug Fix: `despawnPolice()` is dead code — police accumulate indefinitely across day-night cycles

**Discovered**: 2026-02-23

**Status**: ❌ Broken — `NPCManager.despawnPolice()` is defined but never called anywhere in the
codebase. Police NPCs spawned at night are never removed at dawn. Every complete day-night cycle
adds 2–8 police units that persist indefinitely, consuming NPC slots and polluting the world.

**Problem**: `NPCManager.updatePoliceSpawning()` is called from `RagamuffinGame.render()` on every
frame and correctly throttles spawning. However it only ever **adds** police — it never removes
them. The `despawnPolice()` method at line 1381 is `private` and has exactly one definition, zero
call sites:

```java
// NPCManager.java — defined but never called
private void despawnPolice() {
    List<NPC> policeToRemove = new ArrayList<>();
    for (NPC npc : npcs) {
        if (npc.getType() == NPCType.POLICE) {
            policeToRemove.add(npc);
        }
    }
    for (NPC police : policeToRemove) {
        npcs.remove(police);
        policeWarningTimers.remove(police);
        policeTargetStructures.remove(police);
    }
}
```

**Consequence across a single day-night cycle**:

1. Night begins (22:00): `updatePoliceSpawning()` spawns 3 police units (non-notorious player,
   non-night cap of 3).
2. Morning arrives (06:00): no despawn call fires. All 3 police remain.
3. Night begins again (22:00): cap check finds 3 alive police, cap is still 3 — no new spawns.
   BUT if any police were killed by the player, more spawn to refill the cap. Over multiple nights
   with combat, the count fluctuates but never drops to 0 at dawn as the spec requires.
4. Phase 6 integration test 3 — "Advance time to 06:00 (morning). Verify all POLICE NPCs have
   despawned or left the map" — always fails because `despawnPolice()` is never triggered.

**Additional impact on NPC cap**: With MAX_NPCS = 100 and police never being removed, after enough
nights police accumulate toward the cap, blocking spawning of other NPC types (shopkeepers,
joggers, etc.) and reducing world variety.

**Required fix**: Call `despawnPolice()` from `updatePoliceSpawning()` when transitioning from
night to day (time crossing 06:00):

```java
// NPCManager.java — add dawn tracking field
private boolean wasNight = false;

public void updatePoliceSpawning(float time, World world, Player player) {
    boolean isNight = time >= 22.0f || time < 6.0f;

    // Despawn police at dawn (night → day transition)
    if (wasNight && !isNight) {
        despawnPolice();
    }
    wasNight = isNight;

    // ... existing spawn throttle and cap logic unchanged
}
```

This ensures police disappear when the sun rises, matching the Phase 6 spec and the intuition that
police "patrol at night" rather than living permanently in the world.

**Integration tests** (add to Phase6IntegrationTest):

1. **Police despawn at dawn**: Set time to 22:00. Call `updatePoliceSpawning()` to spawn police.
   Verify at least 1 POLICE NPC exists. Advance time past 06:00 (morning). Call
   `updatePoliceSpawning()` once more. Verify the count of alive POLICE NPCs is 0.

2. **Police do not accumulate across multiple nights**: Simulate two full day-night cycles (48
   in-game hours). After each dawn, verify police count returns to 0. After each night's peak,
   verify police count does not exceed `maxPolice` (3 for a non-notorious player during the day cap).

3. **Police cap is respected and resets correctly**: Kill 1 of the 3 spawned police mid-night.
   Verify a replacement spawns to refill the cap. Advance to dawn. Verify all remaining police
   (the 2 surviving + 1 replacement) despawn. Advance to the next night. Verify exactly 3 (or
   the applicable night cap) fresh police spawn — not 0, not 6.

---

## Bug Fix: `StructureTracker` treats world-generated WOOD (allotments fence, sheds) as player structures

**Discovered**: 2026-02-23

**Status**: ❌ Broken — `StructureTracker.scanForStructures()` detects `BlockType.WOOD` blocks
to identify player-built structures, but `WorldGenerator` also uses `BlockType.WOOD` for
world-generated structures: the allotments perimeter fence and the allotment sheds. The allotments
fence alone is 200 connected WOOD blocks (30-wide × 20-deep perimeter at 2 block height), far
exceeding the `SMALL_STRUCTURE_THRESHOLD = 10`. As a result, council builders are dispatched to
demolish the world-generated allotments fence ~30 seconds after game start — before the player
has built anything.

**Root cause**: A previous fix (documented above) correctly removed `BlockType.BRICK` from
`StructureTracker.scanForStructures()` to prevent council builders from demolishing the entire
generated town. However the fix left `BlockType.WOOD` as the sole detection target. The
`WorldGenerator.generateAllotments()` method places `BlockType.WOOD` for the perimeter fence
(2 heights × full perimeter = ~200 connected blocks) and the `buildShed()` helper also uses
`BlockType.WOOD` for shed walls. The allotments fence and the shed network both exceed the
10-block threshold:

```
generateAllotments(world, 60, -100, 30, 20)
  → perimeter fence at y=1 and y=2: 2 × (30 + 20) × 2 = 200 WOOD blocks, all connected
  → two sheds at y=1..2: ~8 WOOD blocks each (below threshold individually,
    but connected to the fence if adjacent)
```

`StructureTracker.traceStructure()` does a flood-fill from any discovered WOOD block,
so the entire 200-block fence is detected as a single player structure. The council builder
system then dispatches 1+ builders to demolish it one block per second — erasing the allotments
within ~3 minutes. This happens on every new game and every world reset, regardless of what the
player builds.

**Required fix**: The `WorldGenerator` should use a distinct `BlockType` (e.g. `BlockType.FENCE`
or `BlockType.WOOD_FENCE`) for world-generated wooden fences and sheds so they are not mistaken
for player structures. Alternatively, `StructureTracker` should maintain a set of
world-generated block positions that are excluded from player-structure detection. The simplest
approach is to add `BlockType.WOOD_FENCE` and `BlockType.WOOD_WALL` to the `BlockType` enum
and use them in `generateAllotments()` and `buildShed()`, leaving `BlockType.WOOD` exclusively
for player-placed blocks:

In `WorldGenerator.generateAllotments()` and `buildShed()`, replace `BlockType.WOOD` with
`BlockType.WOOD_FENCE` for the perimeter fence and `BlockType.WOOD_WALL` for shed walls.
In `BlockType`, add:
```java
WOOD_FENCE(solid=true, colour=#8B4513, placeable=false),
WOOD_WALL(solid=true, colour=#8B4513, placeable=false),
```
These types are solid (for collision/rendering) but `placeable=false` so the player cannot
craft or select them. `StructureTracker.scanForStructures()` and `traceStructure()` already
only look for `BlockType.WOOD`, so world-generated fences and sheds would no longer trigger
detection with no further changes needed.

**Integration tests**:

1. **Council builders do NOT demolish allotments fence**: Generate the world (which creates the
   allotments). Do NOT place any player blocks. Wait 35 seconds (or call
   `npcManager.forceStructureScan(world, tooltipSystem)`). Verify
   `npcManager.getStructureTracker().getStructures()` is empty. Verify no COUNCIL_BUILDER NPCs
   have been spawned. Verify the allotments fence blocks (e.g., `world.getBlock(60, 1, -100)`)
   are still WOOD_FENCE (or equivalent non-AIR type), not AIR.

2. **Player-placed WOOD IS still detected**: Place 15 `BlockType.WOOD` blocks in a connected
   cluster at (10, 1, 10) (simulating a player-built structure). Call
   `npcManager.forceStructureScan(world, tooltipSystem)`. Verify exactly one structure is
   detected. Verify a COUNCIL_BUILDER NPC has been spawned.

3. **Regression — sheds not detected at game start**: Generate world. Force structure scan
   immediately. Verify `structureTracker.getStructures()` is empty (no shed or fence detected).

---

## Bug Fix: Police night window hardcoded to 22:00–06:00 but HUD/survival systems use seasonal `TimeSystem.isNight()`

**Discovered**: 2026-02-23

**Status**: ❌ Broken — Two different "night" definitions coexist, producing a confusing and
inaccurate player experience.

- `NPCManager.updatePoliceSpawning()` hardcodes night as `time >= 22.0f || time < 6.0f`.
- Every other system — cold snap health drain, shelter check, and the "NIGHT — POLICE ACTIVE"
  HUD banner — uses `TimeSystem.isNight()`, which returns true based on the **seasonal sunrise
  and sunset times** (British daylight hours, ranging from ~04:43/21:21 in summer to
  ~08:04/15:53 in winter).

**Consequence by season**:

| Season | `TimeSystem.isNight()` starts | Police spawn at |
|--------|------------------------------|----------------|
| Summer (game start, June) | ~21:21 | 22:00 (+39 min gap) |
| Winter (December) | ~15:53 (4pm!) | 22:00 (+6 hr gap) |

In winter, the HUD shows "NIGHT — POLICE ACTIVE" starting at 4pm, cold snap begins draining
the player's health at 4pm, but no police actually spawn for another 6 hours. A player who
builds a cardboard shelter and hides inside at 5pm is burning health from cold snap unnecessarily,
while the HUD falsely assures them that the police are the primary threat.

**Root cause**: `updatePoliceSpawning()` was written with a fixed 22:00 cutoff matching the
original Phase 6 spec ("police patrol at night, 22:00–06:00"), but all survival systems later
adopted `TimeSystem.isNight()` which became seasonal in the Phase 12 weather/seasons update.
The two definitions were never reconciled.

**Required fix**: Change `NPCManager.updatePoliceSpawning()` to use `TimeSystem.isNight()` by
accepting the `TimeSystem` as a parameter (or pre-computing a boolean). Pass `timeSystem.isNight()`
from `RagamuffinGame.render()` instead of the raw `time` float:

```java
// RagamuffinGame.render() — pass isNight instead of raw time
npcManager.updatePoliceSpawning(timeSystem.isNight(), world, player);

// NPCManager.updatePoliceSpawning() — accept boolean, drop hardcoded cutoff
public void updatePoliceSpawning(boolean isNight, World world, Player player) {
    if (wasNight && !isNight) {
        despawnPolice();
    }
    wasNight = isNight;
    if (!isNight) return;
    // ... rest unchanged
}
```

**Integration tests** (add to Phase6IntegrationTest or Phase12IntegrationTest):

1. **Police spawn window matches `TimeSystem.isNight()`**: Set time to the seasonal sunset
   (e.g., `timeSystem.getSunsetTime() + 0.1f`). Verify `timeSystem.isNight()` is `true`.
   Call `npcManager.updatePoliceSpawning(timeSystem.isNight(), world, player)`. Advance 12
   seconds (past `POLICE_SPAWN_INTERVAL`). Call again. Verify at least 1 POLICE NPC exists.

2. **No police during seasonal day (pre-22:00 but after summer sunset)**: Set time to 21:00
   in summer (after `timeSystem.getSunsetTime()` ≈ 21.35 is NOT yet reached, so `isNight()`
   is `false`). Call `updatePoliceSpawning(false, world, player)`. Verify no police spawn.

3. **HUD banner and police spawn agree**: Set time to winter sunset + 0.1h (~16:00).
   Verify `timeSystem.isNight()` is `true`. Verify `gameHUD.isNight()` is `true`.
   Verify police have spawned (call `updatePoliceSpawning` with `isNight = true`).
   All three should agree that it is night. Previously only the HUD and isNight agreed;
   police waited until 22:00.

---

## Bug: Greggs raid state persists across player death — respawn does not reset the raid

**Date:** 2026-02-23
**Status:** Open

**Symptom:** If a player escalates a Greggs raid (breaks blocks, triggers ALERT → ESCALATED
state), is overwhelmed by aggressive police, and dies, they respawn at the park bench with
the raid still fully ESCALATED. Police mobilised by the raid remain in AGGRESSIVE state and
immediately pursue the freshly-respawned player. There is no recovery window; the player
cannot de-escalate the raid without being arrested (which carries health/hunger penalties)
or restarting the game.

**Root cause:** `GreggsRaidSystem.reset()` is called in only two places in
`RagamuffinGame.java`:

```java
// Line ~946 — inside the arrest handler
greggsRaidSystem.reset();

// Line ~1471 — inside restartGame()
greggsRaidSystem = new GreggsRaidSystem();
```

The respawn path (`RespawnSystem.performRespawn()` and the respawn-completion block in
`render()` around lines 529–535) never calls `greggsRaidSystem.reset()`. Death is intended
as a soft reset — the player loses held items and respawns fresh — but the raid lingers.

**Required fix:** In `RagamuffinGame.render()`, in the block that handles respawn completion
(where `respawnSystem.isRespawnComplete()` triggers the fade-back-in and player revival),
add a call to `greggsRaidSystem.reset()`. This mirrors the existing arrest-handler reset
and gives the player the same clean slate that arrest provides, without the arrest penalties.

```java
if (respawnSystem.isRespawnComplete()) {
    greggsRaidSystem.reset();   // ← add this line
    // ... existing respawn completion logic
}
```

**Integration tests** (add to GreggsRaidSystemTest or Phase12IntegrationTest):

1. **Raid resets on respawn**: Escalate the raid to `RaidState.ESCALATED`. Simulate player
   death by calling `respawnSystem.triggerRespawn()` followed by
   `respawnSystem.update(4.0f)` (past the 3-second countdown). Verify
   `greggsRaidSystem.getRaidState() == RaidState.NONE`.

2. **Police de-escalate after respawn reset**: After the above, call
   `npcManager.updatePoliceSpawning(isNight, world, player)`. Verify that no police
   remain in AGGRESSIVE state targeting the player.

3. **Arrest still resets raid**: Confirm the existing arrest path still calls `reset()` —
   `greggsRaidSystem.getRaidState()` is `NONE` after `arrestSystem.arrest(player)`.

---

## Bug: Sprinting never drains energy — energy system is irrelevant to movement

**Date:** 2026-02-23
**Status:** Open

**Symptom:** The player can sprint indefinitely without the energy bar dropping.
Sprinting currently multiplies hunger drain by 3× (correct), but does **not** drain the
energy stat at all. Since energy only depletes on explicit actions (punching: 1 pt,
dodging: 15 pts) and recovers at 5 pts/s passively, a player who avoids combat can run
flat-out for the entire session without ever watching the energy bar. The energy/dodge
tradeoff the game intends — do you sprint and exhaust yourself, or save energy for a
dodge-roll escape? — never materialises.

**Root cause:** In `RagamuffinGame.updatePlaying()`:

```java
// Sprint drains hunger 3x faster
float hungerMultiplier = inputHandler.isSprintHeld() ? 3.0f : 1.0f;
player.updateHunger(delta * hungerMultiplier);
```

There is no corresponding energy drain for sprint. The `Player.ENERGY_DRAIN_PER_ACTION`
constant (1 HP) is only used on punch. Sprinting calls `world.moveWithCollision()` with
`Player.SPRINT_SPEED` (20 m/s) but never touches `player.consumeEnergy()`.

**Required fix:** Add a per-second energy drain while sprinting, scaled so that a player
at full energy (100 HP) can sprint for roughly 20 seconds before energy is exhausted.
A drain of `5.0f` energy/s (matching the passive recovery rate) means sprinting is
energy-neutral when stationary, but any burst of movement depletes the bar if the player
hasn't been resting. A tighter value of `8.0f` energy/s means the player must rest
between sprints — recommend the tighter value for survival tension.

```java
// In updatePlaying(), in the !isUIBlocking() block, after hunger update:
if (inputHandler.isSprintHeld() && tmpMoveDir.len2() > 0) {
    player.consumeEnergy(8.0f * delta); // Sprint drains energy at 8 HP/s
}
```

Add `Player.SPRINT_ENERGY_DRAIN = 8.0f` as a named constant.

**Integration tests** (add to Phase8IntegrationTest or a new SprintEnergyTest):

1. **Sprint drains energy**: Set player energy to 100. Simulate holding sprint + W for
   10 seconds (600 frames at 60fps). Verify `player.getEnergy() < 100` (energy has
   decreased). Verify the decrease is approximately `80f` (10s × 8 HP/s), within ±5.

2. **Walking does not drain energy**: Set player energy to 100. Simulate holding W
   (no sprint) for 10 seconds. Verify `player.getEnergy()` is >= 100 (energy has
   recovered or stayed flat — no sprint drain applies).

3. **Exhausted player cannot dodge**: Set player energy to 0. Simulate sprint + W for
   5 seconds. Verify `player.getEnergy()` remains at 0. Verify `player.canDodge()` is
   false (no energy for dodge).

4. **Energy recovers after stopping sprint**: Set player energy to 20. Stop all movement
   for 4 seconds. Verify `player.getEnergy()` >= 40 (recovery at 5 HP/s = 20 pts over
   4s, plus starting 20 = 40).

---

## Bug: Council builder removal inside indexed NPC loop skips the following NPC each frame

**Date:** 2026-02-23
**Status:** Open

**Symptom:** Whenever a council builder finishes demolishing a structure and is removed,
the NPC that immediately follows it in the `npcs` list is silently skipped for that
entire game-logic frame. In scenes with multiple NPCs this means police may fail to
pursue the player, youth gangs freeze mid-theft, and speech timers don't tick — for
exactly one frame per builder dismissal. With several builders cycling through over a
session this adds up to observable hitches in NPC behaviour.

**Root cause:** `NPCManager.update()` iterates over `npcs` with an indexed `for` loop
(line 307) specifically to avoid `ConcurrentModificationException` from spawning new
NPCs during iteration. However, `updateCouncilBuilder()` (called from within that loop)
calls `npcs.remove(builder)` directly at line 1722 when the builder's target structure
is demolished. Removing an element at index `i` shifts every subsequent element one
position left. The loop then increments `i` to `i+1`, which now points to what was
previously `i+2` — so the NPC originally at `i+1` is never processed this frame.

```java
// In NPCManager.updateCouncilBuilder() — WRONG
if (target == null || target.isEmpty()) {
    npcs.remove(builder);   // ← shifts the list, next element skipped
    ...
    return;
}
```

The `removeIf` guard at line 291 only removes NPCs where `!isAlive()` — council builders
are alive when they finish their job, so they bypass that path entirely.

**Required fix:** Do not call `npcs.remove()` inside `updateCouncilBuilder()`. Instead,
mark the builder for deferred removal (e.g. set `alive = false` via `builder.takeDamage(9999f)`
or add a dedicated `pendingRemoval` flag) and let the existing `removeIf` at the top of
`update()` handle the cleanup at the start of the next frame, before the `for` loop runs.

**Integration tests** (add to Phase7IntegrationTest or a new CouncilBuilderRemovalTest):

1. **NPC after builder is not skipped on builder removal**: Spawn a council builder
   targeting a structure. Spawn a second NPC (e.g. PUBLIC) immediately after the builder
   in the NPC list. Tick the manager until the builder's structure target is empty (trigger
   removal). Verify that on the same frame, the PUBLIC NPC's state/timers have been
   updated (e.g. its speech timer decreased) — demonstrating it was not skipped.

2. **Multiple builders removed in one frame do not corrupt NPC list**: Spawn 3 council
   builders all targeting already-empty structures (so they are all removed on the first
   update). Spawn 5 civilian NPCs after them. After one `update()` call, verify that all
   5 civilian NPCs are present in `npcManager.getNPCs()` and have had their timers ticked
   (none were orphaned or skipped).

---

## Bug: Landmark crimes award zero reputation — the reputation system is effectively inert during normal gameplay

**Date:** 2026-02-23
**Status:** Open

**Symptom:** A player can raid Greggs (break blocks, steal sausage rolls, trigger a full
police response), loot the jeweller (steal diamonds, trigger a tooltip), and smash up
every shop on the high street — all without earning a single reputation point. The
reputation → consequence cascade (civilians flee at NOTORIOUS, police cap doubles from 4
to 8, police skip the warning phase) is permanently stuck at its lowest tier for any
player who doesn't specifically pick fights with NPCs.

**Root cause:** After fixing Issue #46 (block-breaking awarding too much reputation),
the fix correctly removed the per-block `addPoints(1)` call, but the design intent in
the SPEC was that *genuinely criminal* block-breaking — specifically raiding named
landmarks like Greggs and the jeweller — should award reputation. No targeted
replacement was added to `GreggsRaidSystem` or the Greggs/jeweller block-break path in
`RagamuffinGame.handlePunch()`.

Currently, `addPoints()` is called in exactly **one** place in the entire codebase:
line 994 of `RagamuffinGame.java`, when punching an NPC (+2 pts). This is the only
way to build reputation, making the system nearly inert during the core loop of
resource gathering and landmark looting.

The SPEC text from the Issue #46 bug fix section explicitly states:

> "The reputation system was designed to punish *criminal* behaviour — punching NPCs
> (2 pts), **raiding Greggs**, fighting police. Routine resource collection (breaking
> trees) was never intended to be a crime."

This confirms raiding Greggs and other landmark crimes were always intended to award
reputation. The fix removed all block-breaking reputation but never added the
targeted landmark-crime replacement.

**Affected systems:**
- `GreggsRaidSystem.onGreggBlockBroken()` — escalates police but never calls `addPoints()`
- `RagamuffinGame.handlePunch()` — drops the Greggs block, calls `greggsRaidSystem.onGreggBlockBroken()`,
  but no reputation is awarded to the player
- The jeweller diamond theft path — triggers `TooltipTrigger.JEWELLER_DIAMOND` but awards
  zero reputation despite being explicitly named in the SPEC as a major crime

**Required fix in `RagamuffinGame.handlePunch()`:**

When a block from a named landmark is broken and yields a drop (i.e. a crime has been
committed against that landmark), award reputation points proportional to the severity:

```java
// After: Material drop = dropTable.getDrop(blockType, landmark);
if (broken && landmark != null) {
    switch (landmark) {
        case GREGGS:
            // Stealing from Greggs — minor crime, but it adds up
            player.getStreetReputation().addPoints(1);
            break;
        case JEWELLER:
            // Diamond theft — serious crime
            player.getStreetReputation().addPoints(3);
            break;
        case OFF_LICENCE:
        case CHARITY_SHOP:
        case BOOKIES:
            // Smashing up shops — antisocial behaviour
            player.getStreetReputation().addPoints(1);
            break;
        case OFFICE_BUILDING:
            // Corporate vandalism
            player.getStreetReputation().addPoints(1);
            break;
        default:
            break; // Parks, streets, houses — no reputation for normal world interaction
    }
}
```

Note: Breaking non-landmark blocks (trees, park grass, random bricks) should still award
zero reputation, consistent with Issue #46's fix.

**Integration tests** (add to a new `Issue130LandmarkCrimeReputationTest`):

1. **Greggs raid awards reputation**: Place a GLASS block tagged as GREGGS landmark.
   Simulate breaking it (2 hits for GLASS). Verify `player.getStreetReputation().getPoints()`
   increases by 1. Verify breaking 3 Greggs blocks brings the player to 3 points (not 0).

2. **Jeweller theft awards 3 reputation**: Place a GLASS block tagged as JEWELLER. Break it
   (2 hits). Verify reputation increases by 3.

3. **Breaking non-landmark blocks still awards zero reputation**: Place a TREE_TRUNK (no
   landmark tag). Break it (5 hits). Verify reputation stays at 0. Verify no civilians flee.

4. **Reputation accumulates across landmark crimes**: Break 10 Greggs blocks and steal a
   diamond from the jeweller. Verify reputation is at least 13 points (10×1 + 1×3). Verify
   player has reached KNOWN status (≥10 pts).

5. **NOTORIOUS status reachable through landmark crimes alone**: Break enough landmark blocks
   (e.g. 30 Greggs blocks or a combination) without punching any NPC. Verify player reaches
   NOTORIOUS status. Verify a PUBLIC civilian enters FLEEING state on the next NPCManager
   update tick.

6. **Non-criminal block breaks remain at zero reputation**: Break 50 TREE_TRUNK blocks and
   50 GRASS blocks (none tagged to landmarks). Verify reputation is exactly 0 throughout.
   This is the Issue #46 regression guard.

---

## Bug Fix: Leaf blocks float permanently after tree trunk is removed

**Discovered**: 2026-02-23
**Status**: ❌ Broken — when a player breaks a TREE_TRUNK block, the surrounding LEAVES
blocks remain floating in place indefinitely. There is no leaf decay mechanic.

**Problem**: `BlockBreaker.punchBlock()` removes the broken block and returns `true`, but
the game never checks whether adjacent LEAVES blocks have lost their trunk support and
should collapse. The world ends up cluttered with floating leaf cubes as players chop
trees, which looks wrong and permanently fills the world with unsupported foliage.

In a voxel game, leaves should decay (be removed automatically) when no TREE_TRUNK block
exists within a Manhattan distance of ~4 blocks. Minecraft does this; many voxel engines
do. Without it, every tree the player harvests leaves a permanent floating cloud of LEAVES
blocks — visually broken and inconsistent with the block-physics the game otherwise implies.

**Affected systems:**
- `RagamuffinGame.handlePunch()` — calls `blockBreaker.punchBlock()` and `rebuildChunkAt()`
  but never triggers leaf decay
- `World.setBlock()` — no adjacency/support check when a block is set to AIR
- `BlockBreaker` — pure block-destruction logic, no knowledge of structural support

**Required fix in `RagamuffinGame.handlePunch()`**: After a TREE_TRUNK block is broken,
schedule a leaf-decay pass on all LEAVES blocks within a radius of 4 blocks. Any LEAVES
block that has no TREE_TRUNK within Manhattan distance 4 should be removed (set to AIR)
and its chunk marked dirty.

Implement a helper method:

```java
/**
 * Remove floating LEAVES blocks that are no longer connected to any TREE_TRUNK.
 * Called after a TREE_TRUNK block is broken. Checks all LEAVES within radius 4
 * using Manhattan distance and removes unsupported ones.
 */
private void decayFloatingLeaves(int brokenX, int brokenY, int brokenZ) {
    int radius = 4;
    for (int dx = -radius; dx <= radius; dx++) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                int lx = brokenX + dx;
                int ly = brokenY + dy;
                int lz = brokenZ + dz;
                if (world.getBlock(lx, ly, lz) != BlockType.LEAVES) continue;
                if (!hasNearbyTrunk(lx, ly, lz, radius)) {
                    world.setBlock(lx, ly, lz, BlockType.AIR);
                    blockBreaker.clearHits(lx, ly, lz);
                    rebuildChunkAt(lx, ly, lz);
                }
            }
        }
    }
}

private boolean hasNearbyTrunk(int lx, int ly, int lz, int radius) {
    for (int dx = -radius; dx <= radius; dx++) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) continue;
                if (world.getBlock(lx + dx, ly + dy, lz + dz) == BlockType.TREE_TRUNK) {
                    return true;
                }
            }
        }
    }
    return false;
}
```

Call `decayFloatingLeaves(x, y, z)` in `handlePunch()` when a TREE_TRUNK block is broken:

```java
if (broken && blockType == BlockType.TREE_TRUNK) {
    decayFloatingLeaves(x, y, z);
}
```

**Integration tests** (add to a new `Issue132LeafDecayTest`):

1. **Leaves decay when last trunk is removed**: Place a single TREE_TRUNK at (5, 1, 5)
   with LEAVES blocks at (5, 2, 5), (5, 2, 6), (6, 2, 5) (all within 4 blocks). Break
   the trunk. Verify all three LEAVES blocks are now AIR. Verify the chunks have been
   rebuilt (dirty chunk count >= 1).

2. **Leaves survive when adjacent trunk still exists**: Place TREE_TRUNK at (5, 1, 5)
   and (5, 2, 5). Place LEAVES at (5, 3, 5) (supported by upper trunk). Break the lower
   trunk at (5, 1, 5). Verify the LEAVES at (5, 3, 5) are still present (the upper trunk
   still supports them). Break the upper trunk at (5, 2, 5). Verify the LEAVES at
   (5, 3, 5) are now AIR.

3. **Leaves more than 4 blocks from trunk are not affected**: Place TREE_TRUNK at (5, 1, 5).
   Place LEAVES at (10, 1, 5) (5 blocks away — beyond decay radius). Break the trunk.
   Verify the LEAVES at (10, 1, 5) are still present (out of decay range).

4. **Non-trunk block removal does not trigger leaf decay**: Place a BRICK block at
   (5, 1, 5) and LEAVES at (5, 2, 5). Break the BRICK (8 hits). Verify the LEAVES remain.
   Leaf decay is only triggered by TREE_TRUNK removal.

5. **Multi-trunk tree: partial harvesting leaves other leaves intact**: Build a 3-trunk-tall
   tree at (5, 1, 5), (5, 2, 5), (5, 3, 5) with LEAVES at (5, 4, 5), (6, 4, 5),
   (5, 4, 6). Break the bottom trunk (5, 1, 5). Verify none of the leaves have decayed
   (they are all within 4 blocks of the remaining trunks). Break all remaining trunks.
   Verify all leaves are now AIR.

---

## Bug #136 — renderRain() missing orthographic projection: rain overlay is invisible

**Status: ❌ Broken**

`renderRain()` in `RagamuffinGame` renders 80 rain-streak lines using screen-pixel
coordinates (`0..screenWidth`, `0..screenHeight`), but draws them through the stale 3D
perspective projection matrix left by the immediately-preceding `renderBlockHighlight()`
call. Under the perspective matrix those pixel-space coordinates map to a vanishingly
small region of NDC space, so all streaks are clipped and nothing is visible on screen.
Rain gameplay effects (energy drain, NPC shelter-seeking) work correctly, but the player
never sees any rain.

**Root cause**

The render sequence in `RagamuffinGame.render()` (PLAYING state, lines 563-578):

```
modelBatch.end();
renderBlockHighlight();          // sets shapeRenderer.setProjectionMatrix(camera.combined)  ← 3D perspective
renderSpeechBubbles();           // resets to its own ortho ✅
signageRenderer.render(...);
if (weather == RAIN) renderRain(delta);  // NO setProjectionMatrix — inherits stale 3D matrix ❌
renderUI();                      // resets to its own ortho ✅
```

`renderBlockHighlight()` (line 1831):
```java
shapeRenderer.setProjectionMatrix(camera.combined);  // perspective matrix
```

`renderRain()` (lines 1796-1817) — calls `shapeRenderer.begin()` with no prior
`setProjectionMatrix`:
```java
private void renderRain(float delta) {
    // ...
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);  // stale 3D matrix still bound!
    for (int i = 0; i < numDrops; i++) {
        float rx = ...; // screen pixel coords, e.g. 0–1920
        float ry = ...; // screen pixel coords, e.g. 0–1080
        shapeRenderer.line(rx, ry, rx - 2f, ry + 15f);  // invisible under perspective
    }
    shapeRenderer.end();
}
```

**Required fix** — add an orthographic setup at the top of `renderRain()`, before the
`shapeRenderer.begin()` call:

```java
private void renderRain(float delta) {
    rainTimer += delta;
    int screenWidth  = Gdx.graphics.getWidth();
    int screenHeight = Gdx.graphics.getHeight();
    com.badlogic.gdx.math.Matrix4 rainOrtho = new com.badlogic.gdx.math.Matrix4();
    rainOrtho.setToOrtho2D(0, 0, screenWidth, screenHeight);
    shapeRenderer.setProjectionMatrix(rainOrtho);   // ← add this line
    Gdx.gl.glEnable(GL20.GL_BLEND);
    // ... rest of method unchanged ...
}
```

**Integration tests** (add to a new `Issue136RainRenderTest`):

1. **Rain lines fall within screen bounds**: Trigger RAIN weather. Capture the set of
   `shapeRenderer.line()` calls made during one `renderRain()` invocation. Verify every
   start-point and end-point has `x` in `[0, screenWidth]` and `y` in `[0, screenHeight]`
   after the projection matrix is set to `setToOrtho2D(0, 0, screenWidth, screenHeight)`.

2. **Projection matrix is orthographic before begin()**: Spy on `shapeRenderer`. Assert
   that `setProjectionMatrix()` is called with an orthographic (not perspective) matrix
   before the first `begin()` call inside `renderRain()`. Orthographic can be detected by
   checking `matrix.val[Matrix4.M22]` equals `-(2f / screenHeight)` (the standard
   `setToOrtho2D` z-scale).

3. **Rain not rendered when weather is CLEAR**: Set `WeatherSystem` to CLEAR. Call the
   render loop once. Assert `shapeRenderer.begin()` is not called inside `renderRain()`
   (the RAIN branch is skipped).

4. **Rain rendered when weather is RAIN**: Set `WeatherSystem` to RAIN. Call the render
   loop once. Assert `shapeRenderer.begin()` is called exactly once inside `renderRain()`
   and that exactly 80 `line()` calls are made (matching `numDrops = 80`).

---

## Phase 12: Black Market & Underground Economy

**Goal**: A secretive NPC-driven barter economy with a "Fence" character who buys
stolen goods, commissions contraband runs, and gatekeeps access to rare items — all
dripping with British street dark humour.

### Core Concept

The player can find a **Fence NPC** (`NPCType.FENCE`) who operates out of the back
of the charity shop (or a lock-up on the industrial estate). The Fence is always
present but only *accessible* once the player has accumulated enough **Street Rep**
(tracked by the existing `StreetReputation` system). Until then, interacting with
the Fence just yields: *"I'm just sorting donations, love. Move along."*

### Street Reputation Gate

- **Rep < 10**: Fence ignores the player entirely.
- **Rep 10–29**: Fence will buy stolen goods (DIAMOND, SCRAP_METAL, STAPLER, COMPUTER)
  at 50% of their base value, paid in FOOD items (the de facto currency of desperation).
- **Rep 30+**: Fence unlocks **Contraband Runs** (see below) and sells rare items from
  a rotating stock (e.g. a crowbar blueprint, a bolt cutter recipe, a high-vis jacket
  that reduces police suspicion).

### Selling Stolen Goods

- The player walks up to the Fence NPC and presses **E** to open the Fence Trade UI
  (a new UI panel distinct from the crafting/inventory UIs).
- The UI shows two columns: player inventory on the left, Fence's offer on the right.
- The Fence evaluates items using a `FenceValuationTable` (new class):
  - `DIAMOND` → 8 FOOD
  - `COMPUTER` → 5 FOOD
  - `SCRAP_METAL` → 2 FOOD
  - `STAPLER` → 1 FOOD
  - `OFFICE_CHAIR` → 3 FOOD
  - `PLANKS` → 1 FOOD (per 4)
  - Any item not on the table → Fence says *"Can't shift that, mate."*
- The player selects items to sell; pressing **Confirm** transfers the items to the
  Fence and adds FOOD to the player's inventory. The transaction also increments
  `StreetReputation` by 1 per sale (max once per in-game day).

### Contraband Runs (Rep 30+)

The Fence offers **timed delivery quests** using the existing `Quest` system
(`ObjectiveType.DELIVER`). These are procedurally selected from a fixed pool:

| Run Name | Objective | Time Limit (in-game minutes) | Reward |
|---|---|---|---|
| "The Parcel" | Deliver SCRAP_METAL (×5) to the industrial estate | 10 | 4 FOOD + Rep+3 |
| "Diamond Geezer" | Deliver DIAMOND (×1) to the jeweller | 8 | 6 FOOD + Rep+5 |
| "Office Clearance" | Deliver COMPUTER (×2) to the off-licence | 12 | 5 FOOD + Rep+2 |
| "Biscuit Run" | Deliver FOOD (×3) to the Greggs | 5 | 8 FOOD + Rep+1 |

- Only one Contraband Run is active at a time.
- If the player fails to deliver within the time limit, the Fence's rep drops by 5
  and the Fence goes cold for one in-game day (won't trade or offer runs).
- A new run is available every in-game day.
- Time-limit countdown is displayed on the `QuestTrackerUI`.

### Fence Stock (Rep 30+)

The Fence sells items from a daily rotating stock of 3 items (re-rolled each in-game
day). Items purchasable with FOOD:

- **High-Vis Jacket** (new `Material.HIGH_VIS_JACKET`, 6 FOOD): While in inventory,
  reduces police WARNED→HOSTILE escalation timer from 5 s to 12 s (police assume
  you're a council worker).
- **Crowbar** (new `Material.CROWBAR`, 8 FOOD): Reduces block-breaking hits required
  by 2 for HARD blocks (BRICK/STONE/PAVEMENT go from 8 to 6 hits).
- **Balaclava** (new `Material.BALACLAVA`, 10 FOOD): While worn (toggle with **B**),
  prevents `GangTerritorySystem` from identifying the player; gangs give a fresh 5 s
  linger window even if previously HOSTILE. Tooltip on equip: *"You look like you're
  about to rob a post office. Which you are."*
- **Bolt Cutters** (new `Material.BOLT_CUTTERS`, 12 FOOD): Instantly breaks GLASS
  in 1 hit instead of 2; flavour text: *"For legitimate purposes only."*
- **Dodgy Pasty** (5 FOOD): Instantly restores 50 HP. Tooltip: *"Best not to ask
  what's in it."*

The stock is drawn randomly from this pool each in-game day with no duplicates.

### Fence NPC Behaviour

- The Fence NPC has a `FENCE` `NPCType` and uses `NPCState.IDLE` during the day.
- At night (22:00–06:00) the Fence paces between the charity shop back door and
  the industrial estate, muttering ambient lines:
  - *"All legitimate, all above board."*
  - *"Bit nippy for a handover."*
  - *"Cash only. Obviously."*
  - *"If anyone asks, I'm delivering leaflets."*
- If police are nearby (within 15 blocks), the Fence immediately returns to IDLE
  and the trade UI closes with the message: *"Not now. Bill's watching."*
- The Fence cannot be attacked (has 999 HP and does not retaliate — *"I'm a
  businessman, not a fighter"*).

### New Materials

Add to `Material` enum:
- `HIGH_VIS_JACKET`
- `CROWBAR`
- `BALACLAVA`
- `BOLT_CUTTERS`

Add passive effect processing in `InputHandler` / `InteractionSystem`:
- Check player inventory for `HIGH_VIS_JACKET` when police escalation threshold fires.
- Check for `CROWBAR` when `BlockBreaker` calculates hits required.
- Check for `BALACLAVA` when `GangTerritorySystem` evaluates linger time.
- Check for `BOLT_CUTTERS` when `BlockBreaker` targets GLASS.

### HUD / UI

- New `FenceTradeUI` class (similar structure to `CraftingUI`) shown on pressing **E**
  near the Fence.
- `QuestTrackerUI` shows a countdown timer in red for active Contraband Runs.
- A small **Rep indicator** (number + star icon) is added to the HUD bottom-right,
  updated live from `StreetReputation`.

**Unit tests**: `FenceValuationTable` returns correct FOOD amounts for all listed
materials and returns -1 for unlisted items; `FenceStockGenerator` produces exactly
3 distinct items per day and re-rolls each day; passive item effects apply correctly
to block-breaking and police escalation; Contraband Run timer decrements correctly.

**Integration tests — implement these exact scenarios:**

1. **Fence ignores low-rep player**: Set `StreetReputation` to 0. Place the player
   adjacent to the Fence NPC. Press **E**. Verify the `FenceTradeUI` does NOT open.
   Verify the Fence emits the "sorting donations" dismissal line.

2. **Selling stolen goods transfers items and awards FOOD**: Set `StreetReputation`
   to 15. Give the player 1 DIAMOND. Open the Fence Trade UI via **E**. Select
   DIAMOND and confirm. Verify the player's DIAMOND count is 0. Verify the player's
   FOOD count has increased by 8. Verify `StreetReputation` has increased by 1.

3. **Fence refuses unlisted item**: Set rep to 15. Give the player 5 LEAVES. Open
   Fence Trade UI. Attempt to sell LEAVES. Verify the transaction is rejected and
   the Fence emits the "can't shift that" line. Verify player still has 5 LEAVES.

4. **Contraband Run completes within time limit**: Set rep to 30. Accept "The Parcel"
   run (SCRAP_METAL ×5 to industrial estate). Give the player 5 SCRAP_METAL. Move
   the player to the industrial estate landmark within the 10 in-game minute limit.
   Verify the quest is marked completed. Verify the player received 4 FOOD and rep
   increased by 3.

5. **Failed Contraband Run penalises rep**: Accept a Contraband Run. Advance in-game
   time past the time limit without delivering. Verify the Fence's availability flag
   is set to cold (no trade UI for 1 in-game day). Verify `StreetReputation` decreased
   by 5.

6. **High-Vis Jacket delays police escalation**: Give the player a `HIGH_VIS_JACKET`.
   Set time to 22:00. Let a police NPC approach. Record the escalation timer. Verify
   the timer is 12 s, not the default 5 s. Remove the jacket from inventory. Verify
   the timer reverts to 5 s.

7. **Crowbar reduces break hits for HARD blocks**: Give the player a `CROWBAR`. Target
   a BRICK block. Simulate punch actions. Verify the block breaks after exactly 6 hits
   (not 8). Verify the block is replaced with AIR and drops BRICK material.

8. **Balaclava resets gang linger timer**: Set rep to 0. Enter a gang territory until
   state reaches HOSTILE (linger 5+ seconds). Give the player a `BALACLAVA` and
   toggle it with **B**. Verify `GangTerritorySystem` resets the linger timer for the
   current territory to 0 and state returns to WARNED.

9. **Fence flees police**: Set time to 22:00. Let a police NPC spawn within 15 blocks
   of the Fence. Verify the Fence's state is IDLE (not pacing). Open Fence Trade UI
   (if it was open, verify it auto-closes). Verify the Fence emits the "Bill's
   watching" dialogue.

10. **Fence stock re-rolls each in-game day**: Record the Fence's 3-item stock at
    day 1. Advance time by 24 in-game hours. Verify the Fence's stock has been
    re-rolled (at least 1 item differs, or verify the RNG seed changed). Verify the
    new stock still has exactly 3 distinct items from the valid pool.

---

## Phase N: Stealth System — Crouch, Noise & Line-of-Sight Evasion

**Goal**: Give the player genuine tools to avoid detection. Police and council builders
currently have omniscient awareness — they sense the player at a fixed radius regardless of
obstacles, lighting, or player behaviour. The game already sells a BALACLAVA and HIGH_VIS_JACKET
through the Fence, but these items do nothing. This phase makes stealth a first-class mechanic
that transforms the criminal economy from "run and hope" into a strategic, replayable loop.

### New Controls
- **Left Ctrl (hold)**: Crouch — reduces movement speed to 1.5 blocks/s (half normal) but
  dramatically lowers noise output and detection radius.

### New Systems

1. **NoiseSystem**: Tracks the player's current noise level (0.0 to 1.0) updated each frame.
   Noise is driven by:
   - Walking upright: 0.6 base noise
   - Crouching: 0.2 base noise
   - Sprinting (future): 1.0 base noise (reserved, not yet implemented)
   - Breaking a block: instant spike to 1.0, decays back to movement noise over 2 seconds
   - Placing a block: instant spike to 0.7, decays over 1 second
   - Standing still: 0.05 ambient noise (breathing)
   Noise decays linearly toward the movement baseline at a rate of 0.5/s.
   The noise level is displayed as a small audio-waveform icon on the HUD (filled = loud, empty = silent).

2. **Line-of-sight (LoS) detection**: Police and COUNCIL_BUILDER NPCs no longer use a pure
   distance sphere. Instead detection works on a cone-plus-hearing model:
   - **Vision cone**: 70-degree half-angle in front of the NPC, maximum range 20 blocks. If the
     player is inside this cone AND there is no solid block (non-AIR, non-GLASS) between the NPC
     and the player (DDA raycast), the NPC *sees* the player with probability 1.0. The existing
     POLICE_FLEE_DISTANCE check (used by the Fence) is *not* affected by this change.
   - **Peripheral awareness**: 360-degree hearing range, scaled by player noise level.
     Detection range = `5 + (player.noiseLevel * 15)` blocks. At minimum noise (0.05) the
     hearing range is 5.75 blocks. At maximum noise (1.0) it is 20 blocks (same as before).
   - **Night multiplier**: Between 22:00 and 06:00 the vision range is halved (35 blocks → 10
     blocks — harder to see) but hearing range is increased by 25% (darkness is quiet, sounds
     carry). This makes crouching at night genuinely useful.
   NPCs still detect the player immediately if the player punches them (direct contact always
   triggers detection, bypassing LoS).

3. **Crouch state**: Player gains a boolean `crouching` field. While crouching:
   - Move speed = 1.5 blocks/s (down from 3.0 blocks/s).
   - Eye height = 0.9 blocks (down from 1.7 blocks). Camera follows.
   - Player AABB height shrinks to 1.1 blocks — the player can move through 1.5-block-tall gaps.
   - First-person arm is hidden (no weapon displayed while crouched — you're being sneaky).
   - HUD displays a crouch indicator icon (small downward-pointing chevron) in the bottom-left.
   - Tooltip on first crouch: "Going under the radar. Literally."

4. **Disguise items** (activating existing Fence stock items):
   - **BALACLAVA** (toggle with **B**, already in Fence stock): When worn, the player's face is
     masked. Police vision cone range reduced by 30% (from 20 to 14 blocks). Does not affect
     hearing range. Cannot be worn in daylight (06:00–21:00) without an instant 2-point rep gain
     (suspicious behaviour in broad daylight). HUD shows a balaclava silhouette icon when active.
     Tooltip on first equip: "A balaclava in Britain. Nobody bats an eye."
   - **HIGH_VIS_JACKET** (already in Fence stock, already has existing effects): Existing effects
     unchanged. Additionally: wearing the HIGH_VIS_JACKET while crouching grants a "working
     class invisibility" bonus — council builders completely ignore the player (they assume you're
     a contractor). Police detection cone angle reduced by 20 degrees (police assume you belong
     there). Tooltip on first use of combo: "High-vis and crouching. You're basically a ghost."
   - **BOLT_CUTTERS** (already in Fence stock): Now enables the player to break GLASS blocks in
     1 hit instead of 2, silently (no noise spike — cutting glass rather than smashing it).
     Tooltip on first use: "Quietly does it."

5. **Detection alert states**: Police NPC detection transitions become gradual when the player
   is not in direct LoS:
   - `SUSPICIOUS` (new sub-state): Police has heard the player but not seen them. Turns toward
     noise source, increases movement speed to investigate. Speech bubble: "What was that?"
     Duration: 5 seconds before escalating to WARNING if player remains detectable.
   - `WARNING` (existing): Police has seen the player or investigated and confirmed. Existing
     behaviour unchanged.
   - If the player ducks into cover (breaks LoS and reduces noise below 0.3) during SUSPICIOUS,
     the NPC returns to PATROL after 5 seconds: speech bubble "Must've been nothing."

### Integration Tests — implement these exact scenarios

1. **Crouching reduces noise level**: Player stands still — verify noise ≤ 0.1. Player walks
   upright — verify noise ≥ 0.5. Player crouches and walks — verify noise ≤ 0.25. Verify
   crouching reduces move speed to ≤ 1.6 blocks/s.

2. **Block break spikes noise**: Player is crouching and stationary (noise ≈ 0.05). Player
   punches a block, destroying it. Immediately after, verify noise = 1.0. Advance 1 second
   (60 frames at 60fps). Verify noise has decayed below 0.6. Advance another 1 second. Verify
   noise has decayed back toward 0.05 (below 0.2).

3. **Line-of-sight blocks detection**: Place a solid BRICK wall (3 blocks tall) between the
   player and a police NPC at distance 15 blocks. Player walks upright (high noise). Advance
   300 frames. Verify the police NPC does NOT transition to WARNING or AGGRESSIVE (wall blocks
   vision and noise range is 5 + 0.6*15 = 14 blocks, player is at 15 — just outside hearing
   range). Remove one block from the wall to create a gap. Advance 60 frames. Verify the NPC
   NOW detects the player (direct LoS restored).

4. **Crouching past police undetected**: Place a police NPC at (0, 1, 0) facing +Z direction.
   Place player at (-3, 1, 10) — outside the 70-degree vision cone (player is to the NPC's
   left/behind). Player walks upright past the NPC (±X direction, staying outside cone). Verify
   the NPC does NOT enter WARNING state during traversal. Repeat with player crouching — same
   result, but verify noise is ≤ 0.25 throughout.

5. **BALACLAVA reduces vision cone range**: Give player a BALACLAVA, activate it (press B),
   set time to 22:00 (night — no daytime rep penalty). Place police NPC 16 blocks in front of
   player, inside vision cone. Advance 60 frames. Verify NPC does NOT enter WARNING (balaclava
   shrinks vision to 14 blocks; player is at 16 — outside range). Move player to 13 blocks.
   Advance 60 frames. Verify NPC NOW enters WARNING.

6. **BALACLAVA in daytime awards reputation**: Set time to 10:00 (daytime). Give player
   BALACLAVA. Activate it (press B). Verify player reputation increases by 2 immediately.
   Verify tooltip or speech event ("Bit suspicious, mate") is triggered.

7. **HIGH_VIS_JACKET + crouch ignores council builders**: Spawn a COUNCIL_BUILDER NPC facing
   the player at 8 blocks. Player wears HIGH_VIS_JACKET and is crouching. Advance 300 frames.
   Verify the council builder does NOT approach the player and does NOT enter WARNING state.
   Remove the jacket (un-equip from hotbar). Advance 60 frames. Verify the council builder NOW
   detects the player and moves toward them.

8. **BOLT_CUTTERS break glass silently**: Give player BOLT_CUTTERS in hotbar, select it.
   Adjacent to GLASS block. Punch once. Verify the GLASS block is destroyed after exactly 1
   hit. Verify noise spike does NOT occur (noise remains ≤ 0.3, not spiked to 1.0).

9. **SUSPICIOUS state: heard but not seen**: Police NPC faces away from player (+Z). Player
   is behind NPC at 6 blocks (outside vision cone, inside hearing range at noise 0.8). Break a
   block (noise spike to 1.0). Verify NPC enters SUSPICIOUS state (turns toward player, speech
   "What was that?"). Player immediately crouches and stands still (noise decays to 0.05).
   Advance 300 frames (5 seconds). Verify NPC returns to PATROL (speech "Must've been nothing.").

10. **Night multiplier: vision halved, hearing up**: Set time to 23:00. Place police NPC
    facing the player at 12 blocks (inside normal 20-block day cone, but at night vision is
    halved to 10 blocks). Player walks upright. Advance 60 frames. Verify police NPC does NOT
    see the player (night vision = 10 blocks, player at 12). Now verify hearing range: player
    noise = 0.6, night hearing range = 1.25 × (5 + 0.6×15) = 17.5 blocks — player at 12 IS
    detectable by sound. Verify NPC enters SUSPICIOUS state from noise alone.

---

## Phase O: The Big Job — Planned Heist System

**Goal**: A fully planned, multi-stage robbery mechanic. The player can case
specific landmark buildings (jeweller, off-licence, Greggs cash drawer,
JobCentre filing cabinets), acquire specialist tools, execute the heist under
stealth constraints, and fence the loot — all while the police escalation and
faction rumour network react dynamically to news of the crime. Ties together
stealth, factions, crafting, fence economy, and rumour systems into one
thrilling gameplay loop.

### Overview

A **heist** is a multi-stage operation with four distinct phases:

1. **Casing** — the player investigates the target building, identifying
   guard patrol routes, alarm boxes, and loot locations.
2. **Planning** — the player acquires specialist equipment and bribes or
   recruits an NPC accomplice.
3. **Execution** — the player breaks in, neutralises alarms, grabs loot, and
   escapes within a time limit before police arrive.
4. **Fence** — the hot loot must be sold within an in-game hour or it
   permanently becomes worthless ("the word's out").

### New Buildings / Props

- **Alarm box** (`PropType.ALARM_BOX`): a red wall-mounted prop placed on the
  exterior walls of heistable buildings. While active, any block break within 8
  blocks of the alarm box spikes noise to 1.0 AND immediately flags the player
  to all police NPCs on the map (equivalent to pressing F for "wanted"). The
  alarm box can be silenced by interacting (E) with it while holding BOLT_CUTTERS
  (1 second action, noise = 0.1). Silenced alarm boxes turn grey; re-activate
  after 3 in-game minutes.
- **Safe** (`PropType.SAFE`): a heavy metal prop inside the jeweller and the
  off-licence back room. Cannot be broken by punching. Requires CROWBAR (new
  craftable tool) + 8 seconds of interaction (hold E) while undetected. Yields
  3–6 DIAMOND (jeweller) or 20–40 COIN + 1 PETROL_CAN (off-licence). If the
  player is detected while cracking the safe, the action is interrupted and
  noise spikes to 1.0.
- **CCTV camera** (`PropType.CCTV`): a wall-mounted camera prop. If the player
  walks within its 6-block frontal cone (45°) while not wearing a BALACLAVA,
  they receive a 1-point criminal record penalty per second of exposure, and a
  GANG_ACTIVITY rumour is seeded into the RumourNetwork: "Someone had their face
  all over the jeweller's CCTV." Wearing a BALACLAVA at night nullifies this.

### New Craftable Tools

| Tool | Recipe | Effect |
|---|---|---|
| `CROWBAR` | BRICK×2 + WOOD×3 | Cracks safes; breaks BRICK blocks in 5 hits instead of 8 |
| `GLASS_CUTTER` | DIAMOND×1 + WOOD×1 | Removes a GLASS block silently in 1 hit with zero noise (quieter than BOLT_CUTTERS) |
| `ROPE_LADDER` | WOOD×4 + LEAVES×2 | Deployable: places a climbable ladder block on any vertical surface for 60 seconds — useful for rooftop entry |

### Heist Targets

Each landmark has a designated heist type. Each heist can only be executed once
per in-game day (reset at 06:00).

| Target | Alarm Boxes | Safes | Key Loot | Time Limit | Faction Impact |
|---|---|---|---|---|---|
| **Jeweller** | 2 | 1 | DIAMOND ×3-6, GOLD_RING ×2 | 90 sec | Marchetti Crew –15 respect (they have a cut) |
| **Off-licence** | 1 | 1 | COIN ×20-40, PETROL_CAN ×1 | 60 sec | Marchetti Crew –20 respect (it's their place) |
| **Greggs** | 0 | 0 | PASTY ×10, COIN ×8 | 45 sec | Street Lads +5 respect ("feeding the lads") |
| **JobCentre** | 2 | 0 | COUNCIL_ID ×1, COIN ×15 | 75 sec | The Council –25 respect (outrage) |

### Casing Phase

- The player enters the target building and presses **F** to "case" it (new
  interaction). This opens a HUD overlay (`HeistPlanUI`) showing a simple 2D
  schematic of the floor plan (derived from the building's block structure):
  - Red squares = alarm boxes (with "ARMED" / "CUT" state).
  - Yellow squares = CCTV camera cones.
  - Green square = the safe location (if present).
  - Blue arrows = NPC patrol waypoints (pulled from the NPCManager's active routes).
- The player must observe each patrol guard for at least 5 seconds (stand within
  6 blocks) before their waypoints appear on the schematic. Unobserved guards
  show as a "?" marker.
- Casing data is stored as a `HeistPlan` object (new class) and persists until
  the heist is executed or the day resets.
- Tooltip on first case action: "Knowledge is power. Or at least it's a start."

### Planning Phase

- The `HeistPlan` exposes a list of recommended items based on what was cased:
  - Alarm boxes present → "You'll need BOLT_CUTTERS or GLASS_CUTTER."
  - Safe present → "You'll need a CROWBAR."
  - CCTV present → "You'll need a BALACLAVA (at night)."
- **Accomplice recruitment**: The player can recruit an NPC with Rumour type
  `GANG_ACTIVITY` by talking to them (E) and choosing "I've got a job" dialogue
  option. The NPC joins as a temporary `NPCState.FOLLOWING` companion for the
  heist duration. The accomplice:
  - Acts as a **lookout**: while positioned outside the target building, the
    accomplice speech-bubbles "All clear" or "Leg it!" (if they spot a patrol
    coming). "Leg it!" sets the player noise level to 0.8 immediately as a
    warning.
  - Costs 10 COIN upfront. If the heist succeeds, they take 25% of COIN loot.
  - If arrested during the heist, a GANG_ACTIVITY rumour is seeded: "Your mate
    got nicked. Loose lips." Marchetti Crew lose 5 respect (amateur hour).
- The player can skip the accomplice and go solo — harder but keeps 100% of loot.

### Execution Phase

- The player selects the heist target from the `HeistPlanUI` and presses **G**
  ("Go") to start the timer.
- A countdown HUD bar appears (top of screen, red) counting down from the time
  limit. If the timer reaches zero, police flood the area: 4 POLICE NPCs spawn
  at the building perimeter and the player is immediately wanted.
- **Alarm activation**: any block break near an armed alarm box instantly starts
  the police-flood countdown at 30 seconds (overriding the remaining time if
  longer). Silencing the alarm box before breaking blocks prevents this.
- **Loot collection**: loot items (DIAMOND, COIN, PASTY, etc.) are placed as
  `SmallItem` drops inside the building at the safe location and on shelves
  (specific PropType positions). The player picks them up as normal.
- **Escape**: player must exit the building's perimeter (cross a 3-block exclusion
  zone around the building) before the timer expires to successfully complete the
  heist.
- On success:
  - `AchievementType.MASTER_CRIMINAL` awarded on first heist completion.
  - Faction respect adjustments applied (see table above).
  - GANG_ACTIVITY rumour seeded into 5 NPCs: "{Target} got done over last night.
    Proper job." Street reputation increases by 3.
  - All loot is marked **HOT** for 60 in-game minutes. The Fence offers 50% of
    normal value for hot loot; after 60 minutes, it drops to 25% permanently.
  - The Fence gives full value (100%) if the player arrives within 5 in-game
    minutes of completing the heist ("still warm, son").
- On failure (timer expired / arrested):
  - All loot is confiscated.
  - Criminal record gains 2 points.
  - Faction that owns the building gains +10 respect (player failed their
    territory).
  - GANG_ACTIVITY rumour: "That one tried to do {Target} over. Pathetic."

### New Achievement

| Achievement | Trigger | Flavour |
|---|---|---|
| `MASTER_CRIMINAL` | Complete any heist successfully | "You absolute wrong'un." |
| `SMOOTH_OPERATOR` | Complete a heist without triggering any alarm | "In and out. No fuss." |
| `FENCE_FRESH` | Fence hot loot within 5 in-game minutes of the heist | "Still warm, son." |
| `SOLO_JOB` | Complete a heist without an accomplice | "Trust no one." |

### Integration Tests — implement these exact scenarios

1. **Alarm box silencing prevents police flood**: Place player adjacent to a
   JEWELLER building with 2 active ALARM_BOX props. Give player BOLT_CUTTERS.
   Interact (E) with the first alarm box — verify it transitions to silenced state
   after 1 second. Break a GLASS block on the jeweller wall. Verify noise does NOT
   spike to 1.0, and NO police NPCs are spawned at the perimeter.

2. **Unsilenced alarm triggers police**: Place player at the jeweller. Do NOT
   silence alarm boxes. Break a BRICK block on the wall. Verify noise = 1.0
   immediately. Verify at least 1 POLICE NPC spawns within 10 blocks of the
   building within 30 frames.

3. **Safe cracking requires CROWBAR and time**: Place player adjacent to a SAFE
   prop inside the jeweller. Player holds CROWBAR. Begin hold-E interaction.
   Advance 7 seconds (420 frames at 60fps) — verify safe is NOT yet open. Advance
   1 more second (60 frames). Verify safe opens and DIAMOND items are spawned at
   the safe's position.

4. **CCTV penalty without BALACLAVA**: Place player in the CCTV camera cone (within
   6 blocks, within 45° of camera facing). Set time to 12:00 (daytime). Advance 60
   frames (1 second). Verify criminal record has increased by at least 1 point.
   Verify a GANG_ACTIVITY rumour containing "CCTV" is present in at least one NPC's
   rumour list.

5. **CCTV nullified by BALACLAVA at night**: Set time to 23:00. Give player BALACLAVA
   and activate it. Place player in CCTV cone at 4 blocks. Advance 60 frames. Verify
   criminal record is unchanged. Verify no CCTV rumour is seeded.

6. **Heist timer triggers police flood on expiry**: Start a jeweller heist (press G).
   Do nothing (let timer expire). Verify that exactly at timer expiry, 4 POLICE NPCs
   are spawned within 5 blocks of the jeweller perimeter.

7. **Accomplice lookout warning**: Spawn an NPC accomplice in FOLLOWING state outside
   the jeweller. Spawn a PATROL NPC walking toward the building entrance. When the
   patrol NPC is within 8 blocks of the accomplice, verify the accomplice emits a
   speech bubble containing "Leg it!" and the player noise level is set to at least
   0.8.

8. **Hot loot fence pricing**: Complete a jeweller heist. Immediately (within 5
   in-game minutes) approach the Fence NPC. Verify loot is offered at 100% value
   ("still warm"). Wait 60 in-game minutes (advance time). Verify loot is now
   offered at 25% value.

9. **Faction respect impact**: Record Marchetti Crew respect. Complete a jeweller
   heist successfully. Verify Marchetti Crew respect has decreased by 15. Verify a
   GANG_ACTIVITY rumour containing "Jeweller" has been seeded into at least 5 NPCs.

10. **ROPE_LADDER enables rooftop entry**: Place player at the base of the jeweller
    wall (height 8 blocks). Give player ROPE_LADDER. Activate (right-click on wall).
    Verify a climbable LADDER block is placed at the player's position on the wall.
    Player moves upward — verify the player can climb the ladder and reaches the
    rooftop (y > 7). Verify the ROPE_LADDER block disappears after 60 in-game
    seconds.

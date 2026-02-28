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

## Phase 8f: The Living Neighbourhood — Dynamic Gentrification, Decay & Reclamation

**Goal**: Make the physical world itself a living, contested battleground that
evolves based on player actions and faction dominance. Buildings decay over time,
The Council rolls out gentrification (replacing crumbling blocks with sterile
luxury flats), the Street Lads reclaim abandoned sites as impromptu parks, and
the Marchetti Crew fortify their territory with shuttered metal-shutter facades.
The player can accelerate, resist, or profit from these transformations — creating
a neighbourhood that is visibly, permanently shaped by emergent choices.

This phase ties together: the existing `FactionSystem`, `SquatSystem`,
`GraffitiSystem`, `PropertySystem`, `WorldGenerator`, council demolition (Phase 7),
and the `RumourNetwork` into a single emergent world-state engine.

### Building Decay System

Every player-reachable building in the world has a hidden **Condition** score
(0–100, starting at 80). Condition degrades over time and from player damage:

| Event | Condition change |
|-------|-----------------|
| Each real-time minute passes (building unoccupied) | −1 |
| Player breaks a block in the building | −3 per block |
| Player places graffiti on an exterior wall | −2 |
| Player squats the building (moves in) | +5/minute (maintenance effect) |
| Council sends a builder to repair (see below) | +20 (one-time) |
| Fire spreads through building (from campfire) | −15 immediately |

**Visible decay states** (applied by `PropertySystem` each in-game hour):

| Condition | Visual / World change |
|-----------|----------------------|
| 70–100 | Normal — no change |
| 50–69 | **Crumbling**: 10% of exterior BRICK blocks replaced with CRUMBLED_BRICK (new BlockType, dark grey, lower hardness=3 hits) |
| 30–49 | **Derelict**: windows (GLASS blocks) shatter to AIR; a `CONDEMNED_NOTICE` prop appears on the front wall; `COUNCIL_NOTICE` rumour seeded |
| 10–29 | **Ruin**: roof blocks (top-layer BRICK/WOOD) removed; partial wall collapses; `BOARDED_WOOD` blocks appear over windows |
| 0–9 | **Demolition Ready**: entire building flagged for council clearance — council builders arrive within 2 in-game hours |

Two new `BlockType` entries are required: `CRUMBLED_BRICK` (like BRICK, colour
#5a5050, hardness 3) and `BOARDED_WOOD` (like WOOD, colour #8b7355, hardness 4).

### Gentrification Wave

When The Council's turf fraction (from `TurfMap`) exceeds **50%**, a
**Gentrification Wave** begins. Every 3 in-game hours, the `PropertySystem`
selects the most-decayed building in Council territory and commissions it for
redevelopment:

1. Council builders demolish all remaining blocks over 10 in-game minutes.
2. A new **Luxury Flat** is generated on the same footprint: smooth GLASS and
   CONCRETE_PANEL blocks (new BlockType `CONCRETE_PANEL`, colour #c8c8c8,
   hardness 10 — very hard to break), with a `BUILDING_SIGN` prop reading
   "Prestige Living — From £850/pcm".
3. The area's **Base Property Value** doubles (tracked in `PropertySystem`),
   which raises fence prices by 10% for goods fenced in that zone (integration
   with `FenceSystem`).
4. A `RumourType.GENTRIFICATION` rumour ("They're turning the old [name] into
   luxury flats — proper taking the piss") is seeded into all nearby NPCs and
   the barman.
5. Street Lads NPCs in the area become Hostile for 5 in-game minutes
   (Respect −15 toward The Council, +5 toward player if player attacks a
   builder during this window).

New `RumourType` entry: `GENTRIFICATION`.

### Reclamation — Street Lads & Marchetti Responses

Opposing factions react to gentrification with their own world changes:

**Street Lads reclamation** (triggers when a Luxury Flat is built in Street Lads
territory or adjacent): Within 1 in-game hour, 2–4 Street Lads NPCs arrive and
begin placing GRAFFITI on the exterior walls (using existing `GraffitiSystem`).
If the building's graffiti coverage reaches ≥ 4 tagged faces, its Condition
score is permanently capped at 60 (Council won't redevelop graffitied buildings).
A `RECLAIMED_SQUAT` prop sign is placed reading "NOT FOR SALE — THE LADS".

**Marchetti fortification** (triggers when Marchetti turf fraction > 40%):
Off-licence and industrial estate buildings gain `METAL_SHUTTER` blocks
(new BlockType, colour #4a4a4a, hardness 12 — the hardest block in the game)
over their door and window openings. Marchetti NPCs patrol the perimeter.
Breaking a METAL_SHUTTER costs 12 hits and immediately triggers a
`RumourType.GANG_ACTIVITY` rumour and a Respect −20 with Marchetti Crew.

New `BlockType` entries: `CONCRETE_PANEL`, `METAL_SHUTTER`.

### Player Agency — Reclaim or Profit

The player has meaningful choices at every stage:

| Action | Effect |
|--------|--------|
| **Squat a derelict building** (existing SquatSystem) before Council flags it | Prevents demolition; building Condition slowly recovers; player gets a free base |
| **Accelerate decay** (punch blocks in occupied building) | Speeds up gentrification in that zone; The Council gains Respect +5 (they love the vacancy); Street Lads lose Respect −5 |
| **Defend a building** (press E on a condemned notice to tear it down) | Condition +10; Council Respect −10; Street Lads Respect +10; `RESISTANCE` rumour seeded |
| **Tip off the council** about a rival's squat (press E near council building with squat address) | Council sends builders in 30 in-game minutes; Marchetti Crew Respect +5 (they hate squatters too); Street Lads Respect −20 |
| **Sell a building to developers** (press E on Luxury Flat sign with ≥ 50 coins) | Earns 30 coins; Property Value in zone +50%; player Notoriety +20 |
| **Organise a community meeting** (place FLYER prop on condemned notice + ≥ 5 nearby NPCs present) | Condition +30; Council Respect −15; all non-Council NPCs in area gain Friendly status for 10 minutes; `COMMUNITY_WIN` rumour |

### Neighbourhood Vibes Score (New HUD Element)

A new aggregate stat: **Neighbourhood Vibes** (0–100, starting at 50), displayed
as a small heart icon with fill bar in the bottom-right HUD corner. It represents
the social health of the area and is computed each in-game minute:

```
Vibes = clamp(
    (avg building Condition / 2)
    + (Street Lads Respect / 10)
    − (Council turf fraction × 20)
    + (active graffiti tags × 2)
    − (Luxury Flat count × 5)
    + (active raves × 10)
    − (Notoriety / 20)
, 0, 100)
```

**Vibes thresholds** have gameplay effects:

| Vibes | Effect |
|-------|--------|
| ≥ 80 | **Thriving**: NPCs wander further, drop bonus coins; music ambient track plays (pub jukebox sound piped to street via `SoundSystem`) |
| 50–79 | **Normal**: baseline |
| 30–49 | **Tense**: NPC wander radius reduced; police spawn rate +25%; rumour spread rate −20% (nobody's talking) |
| 10–29 | **Hostile**: all non-faction NPCs have 30% chance to flee player on sight; fence buy prices −15% (desperate sellers) |
| < 10 | **Dystopia**: ambient sound goes silent; fog distance permanently halved; random CRUMBLED_BRICK blocks appear on street-level structures overnight; Newspaper headline becomes "Local Area Declared Zone of Deprivation" |

### Integration with Existing Systems

- **PirateRadioSystem**: Broadcasting from a derelict building raises that
  building's Condition by +5 per broadcast and temporarily boosts Vibes by +8.
- **BootSaleSystem**: Boot sales held in high-Vibes areas (≥ 60) attract 2
  extra buyer NPCs and raise auction prices by 15%.
- **NewspaperSystem**: Gentrification events and Vibes threshold crossings
  generate front-page headlines ("Ragamuffin Arms at Risk as Developers Move In",
  "Local Vibes at All-Time Low — Council Blamed").
- **StreetSkillSystem** INFLUENCE skill Tier 3 perk: "Community Organiser" —
  the FLYER community meeting mechanic costs no Flyer item when INFLUENCE ≥ Expert.
- **AchievementSystem**: New achievements:
  - `LAST_OF_THE_LOCALS` — Prevent 3 gentrification events by squatting or
    tearing down condemned notices.
  - `PROPERTY_DEVELOPER` — Sell 2 buildings to developers.
  - `DYSTOPIA_NOW` — Let Neighbourhood Vibes drop to 0.
  - `COMMUNITY_HERO` — Raise Neighbourhood Vibes from below 20 to above 60
    in a single session.

**Unit tests**: Condition decay rates, gentrification trigger threshold,
luxury flat block placement, Vibes score formula, decay visual state transitions,
Marchetti fortification block placement, graffiti coverage cap.

**Integration tests — implement these exact scenarios:**

1. **Building condition decays over time**: Place the player 50 blocks from a
   brick building. Record its Condition (should be 80). Advance the simulation
   by 10 in-game minutes (fast-forward TimeSystem). Verify the building's
   Condition has decreased to 70. Verify no visual changes have occurred yet
   (Condition > 70 threshold).

2. **Crumbled brick appears at Condition 65**: Set a building's Condition to 69.
   Advance 1 in-game minute. Verify Condition drops to 68 (below 70 threshold).
   Verify at least 10% of the building's exterior BRICK blocks have been
   replaced with CRUMBLED_BRICK. Verify the CRUMBLED_BRICK blocks have hardness
   3 (break in 3 hits instead of 8).

3. **Condemned notice appears at Condition 45**: Set a building's Condition to
   49. Advance 1 in-game minute (Condition → 48, within 30–49 range). Verify a
   `CONDEMNED_NOTICE` prop has been placed on one of the building's front-facing
   wall blocks. Verify a `COUNCIL_NOTICE` rumour has been seeded into at least
   one nearby NPC. Verify at least one GLASS block in the building has been
   replaced with AIR (shattered window).

4. **Player tears down condemned notice**: Place the player adjacent to a
   CONDEMNED_NOTICE prop on a Condition-45 building. Press E. Verify the
   CONDEMNED_NOTICE prop is removed. Verify building Condition increases by 10
   (to 55). Verify Council Respect decreases by 10. Verify Street Lads Respect
   increases by 10. Verify a rumour containing "resistance" or "fighting back"
   has been seeded into a nearby NPC.

5. **Gentrification wave triggers at 50% Council turf**: Set Council turf
   fraction to 0.51 in TurfMap. Identify the most-decayed building in Council
   territory (set its Condition to 5). Advance the simulation by 3 in-game hours.
   Verify council builders have demolished the old building (block count at that
   footprint is near zero). Verify CONCRETE_PANEL and GLASS blocks now occupy
   the footprint (Luxury Flat has been built). Verify a `GENTRIFICATION` rumour
   exists in at least one NPC's rumour buffer and in the barman's buffer.

6. **Street Lads reclaim a Luxury Flat**: After a Luxury Flat is built (from
   test 5 or equivalent setup), advance 1 in-game hour. Verify 2–4 Street Lads
   NPCs have moved to within 5 blocks of the Luxury Flat. Advance another 300
   frames. Verify at least 1 graffiti tag exists on the exterior of the Luxury
   Flat. Set graffiti coverage to 4 tagged faces. Verify the building's Condition
   is now capped at 60 (calling `getCondition()` returns ≤ 60 even after
   advancing time further).

7. **Marchetti fortification activates at 40% turf**: Set Marchetti turf
   fraction to 0.41. Find the off-licence building. Advance 60 frames.
   Verify at least 1 METAL_SHUTTER block has been placed over a door or window
   opening of the off-licence. Verify METAL_SHUTTER hardness is 12 (requires
   12 hits to break). Place player adjacent to a METAL_SHUTTER. Punch it once.
   Verify a `GANG_ACTIVITY` rumour was seeded. Verify Marchetti Respect decreased
   by 20.

8. **Neighbourhood Vibes HUD element visible and correct**: Start a new game.
   Verify the Vibes HUD element (heart icon + bar) is visible in the bottom-right
   of the screen. Set all building Conditions to 80, Street Lads Respect to 70,
   Council turf to 0.1, graffiti tags to 0, Luxury Flat count to 0, Notoriety to 0.
   Trigger a Vibes recalculation. Verify Vibes is above 50. Now set Council turf
   to 0.9 and add 5 Luxury Flats. Trigger recalculation. Verify Vibes has
   decreased significantly (below 40).

9. **Dystopia state at Vibes < 10**: Set Vibes to 8. Advance 1 frame. Verify
   ambient sound is silenced (SoundSystem ambient track is stopped). Verify fog
   distance has been halved (check `WeatherSystem` fog distance parameter).
   Advance 1 in-game night cycle. Verify at least 1 new CRUMBLED_BRICK block
   has appeared on a street-level structure that was previously undamaged.
   Verify the NewspaperSystem has generated a headline containing "Deprivation".

10. **Full neighbourhood lifecycle stress test**: Start a new game. Run the
    following sequence: (a) advance 5 in-game hours — verify at least 1 building
    has decayed to Crumbling state; (b) complete a Council mission that demolishes
    a building — verify a Luxury Flat appears; (c) have the Street Lads respond
    by placing graffiti; (d) squat an adjacent derelict building as the player —
    verify its Condition begins recovering; (e) trigger a rave in the squat —
    verify Vibes increases; (f) force Vibes to 5 — verify Dystopia effects apply;
    (g) organise a community meeting (FLYER on condemned notice) — verify Vibes
    increases and Council Respect drops. Throughout the entire sequence: verify
    no null pointer exceptions, no crashes, all HUD elements render correctly,
    NPC count remains non-zero, all rumour buffers contain valid data.

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

---

## Phase P: Slumlord — Property Ownership, Passive Income & Turf Investment

**Goal**: Give the player a meaningful long-term money sink and power fantasy
by letting them buy, renovate, and rent out buildings in the neighbourhood.
Owning property intersects with every existing system: factions covet and
contest your holdings, the council slaps you with rates, tenants generate
passive coin income that funds bigger heists, and the Ragamuffin Arms barman
gossips about your rising empire. This is the "getting on the property ladder"
joke made playable.

---

### Property Purchase

- Every non-player-owned building in the world (shops, terraced houses,
  the off-licence, the bookies, charity shop, even the JobCentre) has a
  `PropertyOwner` tag: `STATE`, `MARCHETTI_CREW`, `STREET_LADS`,
  `THE_COUNCIL`, or `NONE` (derelict).
- A new `ESTATE_AGENT` NPC (pinstripe suit, clipboard) stands outside the
  JobCentre on weekdays (in-game time Mon–Fri 09:00–17:00). Pressing **E**
  on him opens the **Property Market UI** — a scrollable list of available
  buildings with purchase prices in COIN.
- Purchase prices:
  - Derelict / NONE-owned building: 50 coins (low risk, low income)
  - State-owned (JobCentre, council housing): 80 coins
  - Faction-owned: 120 coins (the faction's `Respect` toward the player drops
    by 20 immediately — "You bought our gaff?")
- After purchase the building's `PropertyOwner` changes to `PLAYER`.
  A new `PLAYER PROPERTY` sign prop appears above the door (rendered by
  `SignageRenderer`).
- The player can own up to 5 buildings simultaneously. Trying to buy a 6th
  displays tooltip: "You're not Thatcher. Five properties is enough."

### Renovation & Rent Income

- Each owned building has a **Condition** score (0–100, starting at 40 for
  derelict, 70 for previously occupied). Condition decays by 1 point per
  in-game day unless the player repairs it.
- Repairing: stand inside the owned building and use **E** while holding
  BRICK or WOOD in hand. Each BRICK spent restores 5 Condition; each WOOD
  restores 3. Repairs update the `StructureTracker`.
- Passive income per in-game day (collected automatically when player sleeps
  or visits the building):
  - Condition 80–100: 6 coins/day ("des res")
  - Condition 50–79: 3 coins/day ("needs a lick of paint")
  - Condition 20–49: 1 coin/day ("barely habitable")
  - Condition 0–19: 0 coins/day; tenants leave and building becomes derelict
    again, reverting to NONE-owned (you lose it).
- A **TENANT** NPC (model variant: PENSIONER or WORKER) spawns inside each
  occupied building. They wander inside, occasionally peer out the window
  (idle animation), and react to noise/crime nearby.
- If the player breaks blocks inside their own building the tenant shouts
  "Oi, I pay rent here!" and Condition drops by 10 (you damaged the
  property). If the player breaks blocks in someone *else's* faction-owned
  building the owning faction's Respect drops by 5.

### Rival Takeovers

- Factions attempt to reclaim or buy out the player's properties.
  Every 5 in-game days, each faction rolls an attempt on any player
  property within their turf zone:
  - If the player's Respect with that faction < 30: the faction sends
    a THUG NPC to the building; after 60 real seconds of the THUG
    standing adjacent to the building, the building transfers back to
    that faction's ownership (player loses income, gets no refund).
    Tooltip: "They've moved in on your property."
  - If the player's Respect ≥ 30: no attempt is made.
- The player can prevent a takeover by punching the THUG off the property
  (knocking them back > 5 blocks) or by bribing them with 10 coins.
- Turf system hook: player-owned buildings count as neutral turf in
  `TurfMap` — they do not count toward any faction's turf percentage.
  Owning 3+ buildings near the park awards the `ARMCHAIR LANDLORD`
  achievement.

### Council Rates

- The council charges rates on owned properties: 2 coins per in-game week
  per building. If the player cannot pay (insufficient coins), the council
  places a **Planning Notice** on the building (Phase 7 mechanic) and sends
  a council builder to assess it. If unpaid for 2 consecutive weeks, the
  building is compulsorily purchased back by THE_COUNCIL at 0 refund.
- Tooltip on first rates demand: "Even when you own it, the council wants
  their cut."

### Pub Economics Hook

- When the player owns the off-licence, the Barman at the Ragamuffin Arms
  comments: "Heard you bought the offy. Bet you're not passing any savings
  on." (one-time speech line seeded as a `LOOT_TIP` rumour).
- Owning the bookies disables the `FRUIT_MACHINE` random-loss mechanic
  there (the house is now *you*) — all fruit machine plays in your bookies
  pay out at cost-price (no loss, no jackpot).

### New Materials & Items

| Material | Source | Use |
|---|---|---|
| `DEED` | Received from ESTATE_AGENT on purchase | Represents property ownership; shown in inventory |
| `PAINT_TIN` | Crafted from BRICK + WOOD (2+1) | Restores 10 Condition when used on own building |
| `EVICTION_NOTICE` | Crafted at any workbench | Removes a faction THUG from your property without combat |

### New Achievements

| Achievement | Trigger | Flavour |
|---|---|---|
| `FIRST_PROPERTY` | Buy your first building | "Getting on the ladder at last." |
| `ARMCHAIR_LANDLORD` | Own 3 or more buildings simultaneously | "You're basically a developer now." |
| `SLUM_CLEARANCE` | Let all 5 owned buildings decay to 0 Condition | "Absolute slumlord." |
| `THATCHER_WOULDNT` | Own 5 buildings simultaneously | "You're not Thatcher. But you're trying." |
| `RATES_DODGER` | Go 3 in-game weeks without paying council rates | "Living off the grid." |

### New `PropertySystem` class

Implement in `ragamuffin.core.PropertySystem`. It holds:
- `Map<LandmarkType, PropertyOwner> ownership` — current owner of each building.
- `Map<LandmarkType, Integer> condition` — 0–100 Condition per building.
- `List<LandmarkType> playerProperties` — ordered list of player holdings.
- `float dayIncomeAccumulator` — accumulates fractional-day income.
- `update(float delta, Player player, Inventory inv, FactionSystem factions,
  AchievementSystem achievements)` — advances income, decay, faction takeover timers.
- `attemptPurchase(LandmarkType building, Inventory inv, FactionSystem factions)` —
  validates and executes a purchase.
- `repair(LandmarkType building, Inventory inv)` — consumes BRICK/WOOD and raises Condition.
- `collectRent(LandmarkType building, Inventory inv)` — transfers accumulated coins.

**Unit tests**: Income calculation per Condition band, Condition decay rate,
faction takeover trigger conditions, purchase price per ownership type,
council rates deduction schedule, DEED item added to inventory on purchase,
Condition restored correctly per material.

**Integration tests — implement these exact scenarios:**

1. **Purchase a derelict building and collect rent**: Give player 60 coins.
   Find a NONE-owned landmark in the world. Interact (**E**) with the ESTATE_AGENT.
   Purchase the derelict building (50 coins). Verify player now has 10 coins.
   Verify the building's `PropertyOwner` is `PLAYER`. Verify a `DEED` item is
   in the player's inventory. Advance simulation by 1 in-game day. Verify the
   player's coin count has increased by at least 1 (income collected).

2. **Condition decay reduces income tier**: Set a player-owned building to
   Condition 55 (income tier "needs a lick of paint" = 3 coins/day). Advance
   simulation by 30 in-game days WITHOUT repairing. Verify the building's Condition
   has dropped below 50. Verify the daily income from that building is now 1 coin
   (lower tier). Verify no WOOD or BRICK was consumed (decay is passive).

3. **Repairing with BRICK raises Condition**: Own a building at Condition 40.
   Give the player 5 BRICK. Stand inside the building. Press **E** while BRICK
   is in hand. Verify 1 BRICK is consumed from inventory. Verify the building's
   Condition has increased by 5 (to 45). Repeat until all 5 BRICK are used.
   Verify Condition is 65 and inventory contains 0 BRICK.

4. **Faction takeover attempt**: Own a building inside MARCHETTI_CREW turf.
   Set player's Marchetti Crew Respect to 15 (below 30). Advance simulation
   by 5 in-game days. Verify a THUG NPC has spawned adjacent to the building.
   Let the THUG stand for 60 real seconds without interference. Verify the building's
   `PropertyOwner` has reverted to `MARCHETTI_CREW`. Verify the player no longer
   receives income from that building.

5. **Punching a THUG off the property prevents takeover**: Own a building. Set
   faction Respect below 30. Let a THUG spawn adjacent. Punch the THUG so that
   they are knocked > 5 blocks from the building. Verify that after another 60 real
   seconds, the building's `PropertyOwner` is still `PLAYER` (takeover was foiled).
   Verify the player received the income for that day.

6. **Council rates deducted weekly**: Own 2 buildings. Set player coins to 10.
   Advance simulation by 7 in-game days. Verify player coins have decreased by 4
   (2 coins × 2 buildings). Advance another 7 days without enough coins to pay.
   Verify a `COUNCIL_NOTICE` rumour has been seeded. Verify at least 1 building
   now has a Planning Notice placed on it.

7. **ESTATE_AGENT available only on weekdays**: Set in-game time to Saturday 10:00.
   Verify the ESTATE_AGENT NPC is NOT present outside the JobCentre (or is inactive).
   Set time to Monday 10:00. Verify the ESTATE_AGENT NPC IS present and pressing
   **E** opens the Property Market UI.

8. **Player cannot own more than 5 buildings**: Give player enough coins to buy
   6 buildings. Purchase 5 buildings sequentially. Attempt to purchase a 6th.
   Verify the purchase is rejected. Verify the tooltip "You're not Thatcher. Five
   properties is enough." fires. Verify the player's coin balance is unchanged after
   the failed 6th purchase.

---

## Phase Q: Player Squat — Base Building, Defence & Social Prestige

**Goal**: The player can claim a derelict building as a personal squat, customise
its interior by placing blocks and props, fortify it against NPC incursions, invite
NPCs as lodgers (generating income and social buffs), and use it as a persistent
crafting hub. The squat becomes the player's home base — a tangible expression of
progression that ties together crafting, factions, property, notoriety, and the
social economy into a single compelling loop.

### Claiming & Upgrading the Squat

- Any derelict building in the world (no owner, Condition ≤ 10) can be claimed as
  a squat by pressing **E** while standing inside it with a WOOD block in hand.
  Claiming costs 5 WOOD and is free of coin — this is squatting, not buying.
- Only ONE squat can be active at a time (separate from the 5-building property
  limit of the `PropertySystem`). The squat is tagged in the world as
  `LandmarkType.SQUAT`.
- The squat has a **Vibe score** (0–100) — the interior equivalent of Condition.
  Vibe starts at 0 and increases as the player places furnishing blocks/props and
  invites lodgers. Vibe decays by 2 points per in-game day without maintenance.
- Vibe tiers and their gameplay effects:

| Vibe | Label | Effects |
|------|-------|---------|
| 0–19 | Absolute Hovel | No effects. Just a roof over your head. |
| 20–39 | Barely Liveable | Player health regenerates +1/min while inside |
| 40–59 | Decent Gaff | Energy regenerates at 2× rate while sleeping (inside at night). Barman will mention the squat in rumours: "They've got a proper little setup going." |
| 60–79 | Proper Nice | Unlock the Lodger system (see below). One faction's Respect +5 for owning a Proper Nice squat on their turf. |
| 80–100 | Legendary Squat | Notoriety +10 awarded once. NPCs on the street comment: "Heard your gaff is well posh." Unlocks `LEGENDARY_SQUAT` achievement. |

### Furnishing the Squat

The player raises Vibe by placing interior props and blocks inside the squat's
bounding box. Each placement contributes points (one-time, tracked by
`SquatFurnishingTracker`):

| Item placed | Vibe gain | Tooltip on first placement |
|-------------|-----------|--------------------------|
| BED prop | +10 | "At least you've got somewhere to kip." |
| TABLE prop | +5 | — |
| CHAIR prop | +3 | — |
| Campfire block (CAMPFIRE) | +8 | "Tea won't make itself." |
| CARPET block (new block type `CARPET`) | +2 per block, max +20 | "Coming up in the world." |
| BOOKSHELF prop | +5 | — |
| DARTBOARD prop | +7 | "The British way." |
| Any block of BRICK placed as interior wall | +1 per block, max +10 | — |
| WOOD floor (WOOD blocks as floor layer) | +1 per block, max +10 | — |

Removing a furnishing block reduces Vibe by the same amount it added.

### Lodger System

At Vibe ≥ 60, the player can invite NPCs to live in the squat:

- Pressing **E** on a `PUBLIC` or `STREET_LAD` NPC (when Vibe ≥ 60) offers them
  lodgings. If they accept (70% base chance, +15% if the player bought them a
  drink in the pub within 10 minutes), they follow the player home and are assigned
  a bed prop. Acceptance quote: "I suppose it beats sleeping in the bus shelter."
- Max lodgers = floor(Vibe / 20), capped at 4. A fifth lodger is always rejected:
  "It's a squat, not a hotel."
- Each lodger generates **2 coins per in-game day**, tracked separately from
  `PropertySystem` income.
- Lodgers can be evicted by pressing **E** on them inside the squat. They leave
  with: "Fair enough, I'll go back to me mum's."
- If the squat Vibe drops below 20, all lodgers leave voluntarily: "This place has
  gone downhill. I'm off."

### Defence System

Rival factions and opportunistic thugs will periodically try to ransack the squat
(steal props, reduce Vibe) if the player's notoriety is Tier 2 or above:

- Every 3 in-game days, roll a **Raid check** (25% base chance, +10% per notoriety
  tier above 1). If a raid triggers, 1–3 `THUG` NPCs from the faction with lowest
  Respect spawn at the squat entrance.
- Thugs that successfully enter (i.e. the player does not stop them within 60 real
  seconds) destroy the highest-Vibe prop they can reach (Vibe −5 per destroyed prop).
- The player can place **BARRICADE blocks** (new block type, crafted from 2 WOOD +
  1 BRICK) on doorways. Barricades absorb 3 hits before breaking. Thugs attempt to
  break barricades before entering. Placing a barricade grants tooltip:
  "Boarding up the windows. Getting paranoid or getting smart?"
- Lodgers defend: each lodger has a 50% chance per raid to punch a thug (dealing 1
  hit). A lodger who wins a punch exchange shouts: "Oi! This is our gaff!"
- The `BOUNCER` NPC (from the pub) can be hired for 5 coins/day to guard the squat
  entrance permanently. He will not follow the player elsewhere.

### Crafting Hub Upgrade

- Placing a `WORKBENCH` prop (new prop type) inside the squat unlocks **advanced
  crafting recipes** that are not available from the basic crafting menu:
  - **Barricade** — 2 WOOD + 1 BRICK → 1 BARRICADE block
  - **Molotov** (flavour only, not destructive) — 1 WOOD + 1 COIN → prop item
    "Molotov Cocktail" that can be placed as decoration. Tooltip: "Don't actually
    light it."
  - **Lockpick** — 2 BRICK → 1 LOCKPICK tool. Reduces heist safe-crack time by 3s
    when held during a heist (stacks with accomplice bonus).
  - **Fake ID** — 3 WOOD + 2 COIN → 1 FAKE_ID item. Reduces criminal record
    severity by 1 offence when used at the police station (press **E** on duty
    sergeant). Single use. Tooltip: "Not your finest photographic work."
- The workbench crafting menu is a new UI panel (press **C** while inside the squat
  and near the WORKBENCH prop).

### New Block Types

| Block | Notes |
|-------|-------|
| `CARPET` | Soft, coloured floor block. No structural strength. |
| `BARRICADE` | Placed on doorways. 3 hits to break. Opaque. |

### New Prop Types

| Prop | Vibe gain | Notes |
|------|-----------|-------|
| `BED` | +10 | Required for lodgers |
| `WORKBENCH` | 0 | Enables advanced crafting |
| `BOOKSHELF` | +5 | Purely decorative |

### New Achievements

| Achievement | Trigger | Flavour |
|---|---|---|
| `SQUATTER` | Claim your first squat | "Home is where you hang your hoody." |
| `LEGENDARY_SQUAT` | Reach Vibe 80+ | "They don't make them like this in Kensington." |
| `RUNNING_A_HOUSE` | Have 4 lodgers simultaneously | "You're basically a housing association." |
| `BOUNCER_ON_DOOR` | Hire the Bouncer to guard your squat | "Very professional." |
| `BARRICADED_IN` | Survive 3 raids without Vibe dropping below 60 | "Castle doctrine, British edition." |

### New `SquatSystem` class

Implement in `ragamuffin.core.SquatSystem`. It holds:
- `LandmarkType squatLocation` — which building is the current squat (null if none).
- `int vibe` — current Vibe score (0–100).
- `List<NPC> lodgers` — NPCs currently living in the squat.
- `float dayIncomeAccumulator` — lodger income accumulator.
- `float raidCheckTimer` — countdown to next raid roll.
- `NPC guardBouncer` — the hired bouncer NPC (null if none).
- `SquatFurnishingTracker furnishings` — tracks which props/blocks have been placed.
- `update(float delta, Player player, Inventory inv, NotorietySystem notoriety,
  FactionSystem factions, NPCManager npcManager, AchievementSystem achievements)`.
- `claimSquat(LandmarkType building, Inventory inv, World world)` — validates and
  executes a claim.
- `furnish(PropType prop, World world)` — registers a furnishing and adjusts Vibe.
- `evictLodger(NPC lodger)` — removes a lodger.
- `triggerRaid(FactionSystem factions, NPCManager npcManager)` — spawns raid thugs.

**Unit tests**: Vibe calculation per furnishing combination, lodger capacity formula,
raid probability per notoriety tier, barricade durability, income per lodger per day,
Vibe decay per day, lodger acceptance chance formula, squat claim validation.

**Integration tests — implement these exact scenarios:**

1. **Claim a squat and raise Vibe**: Find a derelict building (Condition ≤ 10).
   Give the player 5 WOOD. Stand inside the building. Press **E**. Verify 5 WOOD
   consumed. Verify building tagged as SQUAT. Verify Vibe = 0. Place a BED prop
   inside the squat. Verify Vibe = 10. Place a DARTBOARD prop. Verify Vibe = 17.
   Place a CAMPFIRE block. Verify Vibe = 25. Verify health regenerates +1/min
   while inside (advance 60 seconds inside squat, verify health increased by 1).

2. **Invite a lodger at Vibe ≥ 60**: Set squat Vibe to 65. Find a PUBLIC NPC.
   Press **E** on them. Verify lodger prompt appears. Accept (simulate 70% pass).
   Verify lodger NPC moves to the squat. Verify lodger count is 1. Advance 1
   in-game day. Verify player coin count increased by 2. Press **E** on lodger
   inside squat. Verify eviction dialogue. Confirm eviction. Verify lodger count
   is 0 and lodger NPC is wandering freely.

3. **Lodger capacity enforced**: Set Vibe to 100 (capacity 4 lodgers). Invite 4
   lodgers. Attempt to invite a 5th. Verify the 5th invitation is rejected with
   "It's a squat, not a hotel." Verify lodger count remains 4.

4. **Raid triggers and thugs enter**: Own a squat. Set notoriety to Tier 2. Force
   a raid check to succeed (override raid timer to 0, raid chance to 100%).
   Verify 1–3 THUG NPCs spawn at the squat entrance. Do NOT fight them. Wait 60
   real seconds (simulate frames). Verify Vibe has decreased by at least 5 (prop
   destroyed). Verify at least one THUG entered the squat.

5. **Barricade blocks thug entry**: Own a squat with Vibe 60. Place a BARRICADE
   block in the doorway. Force a raid. Verify thugs attack the barricade first.
   Punch the BARRICADE 3 times. Verify it breaks and becomes AIR. Verify the
   thug enters only after the barricade is broken.

6. **Workbench unlocks advanced recipes**: Place a WORKBENCH prop inside the squat.
   Press **C** while standing near the workbench. Verify the advanced crafting menu
   opens. Verify it contains the BARRICADE, LOCKPICK, and FAKE_ID recipes. Verify
   these recipes are NOT available in the standard crafting menu (press **C**
   outside the squat and verify their absence).

7. **Lodgers flee when Vibe drops below 20**: Set squat Vibe to 25 with 2 lodgers.
   Advance simulation 3 in-game days without any furnishing (Vibe decays 2/day).
   Verify Vibe has dropped to 19 or below. Verify both lodgers have left the squat
   (lodger list is empty). Verify each lodger NPC is in WANDERING state with speech
   "This place has gone downhill. I'm off."

8. **Hire bouncer to guard squat**: Set player coins to 10. Press **E** on the
   BOUNCER NPC at the pub entrance. Verify hire prompt appears (cost: 5 coins/day).
   Confirm hire. Verify 5 coins deducted. Verify BOUNCER NPC moves to squat
   entrance and enters GUARDING state. Force a raid. Verify the BOUNCER attacks
   the first THUG. Verify the raid is repelled (thugs flee or are defeated) without
   Vibe loss.

---

## Phase 8r: Underground Music Scene — Street MC, Raves & MC Battles

**Goal**: Give the player a parallel path to infamy through British street music culture.
Instead of (or alongside) crime, the player can build a reputation as a grime/garage MC,
host illegal raves in their squat, recruit DJs, and battle rival MCs tied to the three
factions. MC Battles feed into the faction Respect and Notoriety systems, raves generate
coins and vibe, and the rumour network spreads your legend across the estate.
Very British. Very loud. Almost certainly in violation of several noise abatement orders.

### The MC System

A new `MCSystem` class tracks the player's music career alongside their criminal one.

**MC Rank** (0–5, starting at 0):

| Rank | Title | Unlock Condition | Perks |
|------|-------|-----------------|-------|
| 0 | **No Name** | Default | None |
| 1 | **On the Mic** | Win first MC battle | Can host small raves (up to 8 NPC attendees) |
| 2 | **Known MC** | Win 3 battles total | Rival faction MCs seek you out for battles; rave capacity 16 |
| 3 | **Local Legend** | Win 5 battles + host 3 raves | NPCs on street greet you; Notoriety +50 one-time bonus |
| 4 | **Scene Don** | Win 10 battles + factions offer exclusive music missions | All 3 factions offer rave-venue protection for 5 coins/day |
| 5 | **Grime God** | Win 15 battles + host 10 raves | Achievement; wanted posters replaced with FAN_POSTER props; one NPC becomes permanent hype-man |

### MC Battles

MC Battles are structured one-on-one verbal combat encounters with NPC MCs.
Each faction has a champion MC NPC that can be challenged:

- **Marchetti MC** ("Vinnie Marche") — hangs around the off-licence, aggressive style
- **Street Lads MC** ("Lil' Estate") — found in the park, rapid-fire flow
- **Council MC** ("Councillor Bars") — wanders near the JobCentre, surprisingly devastating

**How to start a battle:**
Press **E** on a faction MC NPC while holding a `MICROPHONE` item (new `Material` entry,
found in the charity shop or dropped by defeated Councillor NPCs at 5% chance).

**Battle mechanics** — a timing mini-game:
1. The NPC MC "spits bars" (a speech bubble with a randomly selected diss line appears).
2. A `BattleBar` UI element renders: a left-to-right sliding cursor over a target zone
   (width scales inversely with player MC Rank — harder at lower rank, easier as you improve).
3. Player presses **SPACE** when the cursor is in the zone to land a bar. Missing = dropped bar.
4. Three rounds. Most landed bars in three rounds wins.
5. Ties go to the NPC (home crowd advantage).

**Battle outcomes:**

| Outcome | Effect |
|---------|--------|
| Player wins | MC Rank +1 progress; that faction's Respect +10; rival factions Respect −5; Notoriety +15; winner gets 3 coins from loser |
| Player loses | MC Rank progress unchanged; that faction Respect −5 (they're not impressed); loser tooltip: "You got merked. Practice." |
| Draw (tie goes to NPC) | No rank progress; NPC speech: "Come back when you've got something to say." |

After winning, the defeated NPC MC acknowledges the player publicly — a `GANG_ACTIVITY`
rumour is seeded: "[MC name] just got bodied by [player name] outside the [landmark]."
This spreads through the rumour network like wildfire.

**Diss lines** — a pool of British street-flavoured strings, randomly selected per round:
- "Your bars are as empty as the JobCentre queue, mate."
- "You rhyme like you filled in your own ASBO form."
- "I've heard better flow from the estate's leaky radiators."
- "Your lyrics got more holes than the industrial estate roof."
- "You're out here spitting like a broken Greggs sausage roll."
(Minimum pool of 10 per faction, flavoured to their character.)

**Battle cooldown**: Each NPC MC can only be battled once per in-game day. A rematch
is available the next day. The NPC MC will reference the previous result in their
greeting speech line.

### Raves

The player can host illegal raves in their squat (requires Squat Vibe ≥ 40 and MC Rank ≥ 1).

**Hosting a rave:**
Press **E** on the squat entrance while holding a `FLYER` item (craftable: 1 WOOD + 1 COIN
via crafting menu). The rave begins after a 60-second "word spreading" phase during which
the rumour network seeds a `RAVE_ANNOUNCEMENT` rumour type (new entry) to all NPCs within
50 blocks.

**Rave mechanics:**
- Eligible NPCs (PUBLIC, STREET_LAD, YOUTH_GANG) within 50 blocks migrate to the squat
  over the next 120 seconds. Capacity = 8 × MC Rank (capped at 48).
- Each attending NPC generates 1 COIN per in-game minute for the duration of the rave.
- Rave duration = 3 in-game minutes (plus 1 additional minute per 5 attendees, capped at 10 minutes).
- Squat Vibe increases by 1 per 4 attendees at the end of the rave (crowd enjoyed it).
- A successful rave (10+ attendees) seeds a `PLAYER_SPOTTED` rumour: "There was a rave at
  [squat location] last night. Proper mad one."

**Police attention:**
- Raves generate NOISE. The `NoiseSystem` receives a sustained high-noise event for the rave duration.
- After 2 in-game minutes, police are alerted and 2 POLICE NPCs approach the squat.
- If the player disperses the rave early (press **E** on squat while rave is active) before
  police arrive, no arrest risk. Tooltip: "Swerved the feds. Professional."
- If police arrive and the rave is still running, all attending NPCs flee and the player
  receives a noise-offence on their criminal record.

**Rave equipment** — new `PropType` entries that boost rave attendance and coin yield:

| Prop | Source | Bonus |
|------|--------|-------|
| `SPEAKER_STACK` | Craftable: 2 WOOD + 2 SCRAP_METAL | +4 max attendees; rave income +50% |
| `DISCO_BALL` | Looted from charity shop or jeweller | Vibe +5 at rave end; coin yield +25% |
| `DJ_DECKS` | Craftable at WORKBENCH: 3 SCRAP_METAL + 1 COMPUTER | Unlocks DJ NPC recruitment (see below) |

**DJ NPCs:**
With DJ_DECKS placed and MC Rank ≥ 2, pressing **E** on a STREET_LAD NPC offers them
the role of resident DJ (replaces their lodger invitation). A DJ NPC:
- Stays in the squat (IDLE state near the DJ_DECKS prop).
- Doubles the rave income coefficient.
- Is also a legitimate target for the Marchetti Crew "Eviction Notice" mission —
  evicting the DJ earns Marchetti Respect but reduces squat Vibe by 10 and seeds a
  furious rumour: "They ran [DJ name] out of the squat. Disrespectful."

### New RumourType

- `RAVE_ANNOUNCEMENT` — "There's a rave at [landmark] tonight. Pass it on." — seeded
  by the rave system; spreads NPCs toward the squat location. Expires after the rave ends.

### New Materials / Props

| Item | Type | Notes |
|------|------|-------|
| `MICROPHONE` | Material | Found in charity shop; enables MC battles |
| `FLYER` | Material | Crafted (1 WOOD + 1 COIN); used to start raves |
| `SPEAKER_STACK` | PropType | Rave equipment; see above |
| `DISCO_BALL` | PropType | Rave equipment; see above |
| `DJ_DECKS` | PropType | Rave equipment; enables DJ NPC |
| `FAN_POSTER` | PropType | Appears at MC Rank 5 (replaces WANTED_POSTER near squat) |

### New Achievements

| Achievement | Condition |
|-------------|-----------|
| `FIRST_BARS` | Win your first MC battle |
| `GRIME_GOD` | Reach MC Rank 5 |
| `ILLEGAL_RAVE` | Host a rave with 20+ attendees |
| `SWERVED_THE_FEDS` | Disperse a rave before police arrive 5 times |
| `BODIED` | Get defeated in an MC battle (tutorial achievement — consolation prize) |
| `DOUBLE_LIFE` | Reach MC Rank 3 AND Notoriety Tier 3 simultaneously |

### HUD Integration

- A small **MC Rank indicator** appears in the HUD alongside the Notoriety stars —
  a microphone icon with 0–5 filled bars below it.
- During an active MC battle, the `BattleBar` UI occupies the lower-centre of the screen.
- During an active rave, a pulsing "RAVE ACTIVE" indicator appears top-right with an
  attendee count and a police-timer countdown once police are alerted.

**Unit tests**: MCSystem rank progression, battle outcome logic, BattleBar hit-zone
calculation, rave attendee migration, coin yield per attendee-minute, noise system
integration, RAVE_ANNOUNCEMENT rumour spreading and expiry, police alert timer,
all new achievement triggers, DJ NPC state management.

**Integration tests — implement these exact scenarios:**

1. **MC battle win increases rank progress and faction respect**: Give the player a
   MICROPHONE. Place the Street Lads MC NPC ("Lil' Estate") adjacent to the player.
   Press **E**. Simulate the battle: player lands 3 bars in round 1, 3 in round 2,
   3 in round 3 (all hits). Verify the player wins. Verify Street Lads Respect has
   increased by 10. Verify rival factions (Marchetti, Council) Respect decreased by 5
   each. Verify Notoriety increased by 15. Verify a `GANG_ACTIVITY` rumour referencing
   the battle has been seeded into the RumourNetwork.

2. **MC battle loss applies correct penalties**: Give the player a MICROPHONE. Place
   the Marchetti MC adjacent to the player. Simulate the battle: player misses all
   bars in all 3 rounds (0 hits). Verify the player loses. Verify Marchetti Respect
   decreased by 5. Verify MC Rank progress has NOT advanced. Verify the tooltip
   "You got merked. Practice." fires.

3. **Rave starts, attracts NPCs, and generates coins**: Set squat Vibe to 50 and MC
   Rank to 1. Give the player a FLYER. Place 12 PUBLIC NPCs within 40 blocks of the
   squat. Press **E** on the squat entrance. Verify a `RAVE_ANNOUNCEMENT` rumour has
   been seeded. Advance the simulation by 200 frames (word-spreading + migration phase).
   Verify at least 8 NPCs have moved to within 5 blocks of the squat. Advance 3
   in-game minutes. Verify the player has received at least 8 COIN (1 per attendee
   per minute × 3 minutes, minimum 8 attendees).

4. **Police arrive after 2 minutes and disperse rave**: Start a rave with 10 attendees.
   Advance the simulation by 2 in-game minutes. Verify at least 2 POLICE NPCs have
   spawned and are moving toward the squat. Advance until police arrive (within 2
   blocks of squat entrance). Verify all attending NPCs have fled (moved > 10 blocks
   from squat). Verify player's criminal record has gained 1 noise offence.

5. **Early dispersal avoids police charge**: Start a rave with 10 attendees. Advance
   1.5 in-game minutes (police not yet alerted — under 2-minute threshold). Press **E**
   on the squat entrance to disperse the rave. Verify all attending NPCs begin moving
   away from the squat. Verify NO police NPC has spawned. Verify the tooltip
   "Swerved the feds. Professional." fires. Verify the `SWERVED_THE_FEDS` achievement
   progress increments by 1.

6. **SPEAKER_STACK increases attendee capacity and income**: Place a SPEAKER_STACK prop
   in the squat. Host a rave with 12 NPCs available. Advance migration phase. Verify
   the attendee count is higher than it would be without SPEAKER_STACK (base capacity
   for MC Rank 1 is 8; with SPEAKER_STACK it is 12). Verify total coin yield after
   3 minutes reflects the +50% income bonus (at least 1.5× the baseline coin count).

7. **DJ NPC doubles rave income**: Place DJ_DECKS prop. Set MC Rank to 2. Give player
   a MICROPHONE (to confirm rank eligibility). Press **E** on a STREET_LAD NPC to
   offer DJ role. Verify NPC type transitions to DJ or NPC state is IDLE near DJ_DECKS.
   Host a rave with 10 attendees for 3 minutes. Verify coin yield is at least 60 COIN
   (10 attendees × 3 minutes × 2× DJ multiplier = 60).

8. **MC Rank 5 replaces wanted poster with fan poster**: Set MC Rank to 5 via direct
   MCSystem setter (test helper). Advance 60 frames. Verify that any `WANTED_POSTER`
   prop near the squat has been replaced with a `FAN_POSTER` prop. Verify the
   `GRIME_GOD` achievement has been unlocked.

9. **RAVE_ANNOUNCEMENT rumour expires after rave ends**: Seed a `RAVE_ANNOUNCEMENT`
   rumour into the RumourNetwork. Start a rave and advance until it ends naturally
   (3+ in-game minutes). Verify the `RAVE_ANNOUNCEMENT` rumour is no longer present
   in any NPC's rumour buffer. Verify NPCs that were migrating toward the squat
   have returned to their normal wandering state.

10. **Full MC career arc stress test**: Start at MC Rank 0. Win 15 MC battles (simulate
    across all 3 faction MCs, using the daily cooldown system — advance days as needed).
    Host 10 raves without police interruption. Verify MC Rank reaches 5 at appropriate
    milestones (Rank 1 after first win, Rank 3 after 5 wins + 3 raves, Rank 5 after
    15 wins + 10 raves). Verify the `GRIME_GOD` achievement fires at Rank 5.
    Verify `DOUBLE_LIFE` achievement fires if Notoriety Tier is also ≥ 3 at time of
    Rank 3 unlock. Verify no NPEs, no crashes, game remains in PLAYING state throughout.

---

## Phase R: Witness & Evidence System — "The Word on the Street"

**Goal**: Make the rumour network and NPC memory feel consequential by adding a
procedural crime-evidence pipeline. Crimes leave physical evidence in the world;
witnesses who saw them seed rumours; the player can silence witnesses, plant false
evidence, bribe the barman, or "go legit" by tipping off the police — and all of
this feeds back into factions, notoriety, and the criminal record.

This phase transforms the existing `RumourNetwork`, `CriminalRecord`, and
`NotorietySystem` from passive trackers into an active gameplay loop where information
itself becomes a tradeable, destructible resource.

---

### Evidence Objects (new PropType entries)

| PropType | Spawned by | Appearance | Destroyable? |
|----------|-----------|------------|--------------|
| `EVIDENCE_SMASHED_GLASS` | Breaking a GLASS block in a building | Glittery debris pile | Yes (1 punch) |
| `EVIDENCE_CROWBAR_MARKS` | Using CROWBAR on a door/safe | Scratch marks on adjacent wall | Yes (1 punch) |
| `EVIDENCE_BLOOD_SPATTER` | NPC reduced to 0 HP | Red splatter decal on floor | Yes (1 punch) |
| `EVIDENCE_DROPPED_LOOT` | Running with hot loot when a POLICE NPC is within 8 blocks | Small bag prop | Yes (pick up = destroy) |
| `EVIDENCE_CCTV_TAPE` | Exists inside office building and off-licence | VHS cassette prop | Yes (steal = destroy) |

Each piece of evidence has a `crimeId` (UUID) linking it to a specific criminal act.
Evidence within 20 blocks of a POLICE NPC is "discovered" — its `crimeId` is logged
into the player's `CriminalRecord` as a WITNESSED_EVIDENCE entry, which permanently
raises Notoriety by 5 per entry (capped at Tier 5).

**Evidence decay**: Physical evidence props despawn after 10 in-game minutes if
undiscovered and undestroyed. Each surviving piece seeds a fresh `GANG_ACTIVITY`
rumour every 2 in-game minutes while it persists.

---

### Witnesses (NPC behaviour extension)

Any NPC within 12 blocks who has line-of-sight to a crime event (block break in a
building, NPC knockout, heist alarm trigger) becomes a **Witness** — flagged with
`NPCState.WITNESS`. A Witness NPC:

1. **Flees** toward the nearest landmark for 15 seconds (panicked walk/run).
2. **Seeds a rumour** of type `WITNESS_SIGHTING` containing the player's position,
   crime type, and in-game timestamp. The rumour spreads normally through the
   `RumourNetwork`.
3. **Reports to police** if they reach a POLICE NPC within 60 seconds — directly
   grants the player a `WITNESSED_CRIME` entry in `CriminalRecord` and raises
   Notoriety by 10.
4. **Can be bribed**: Press **E** on a fleeing Witness NPC (requires 5 COIN) to
   trigger the bribe dialogue. On success the NPC returns to IDLE, the
   `WITNESS_SIGHTING` rumour is removed from their buffer, and the player gets the
   tooltip "They won't say a word. Probably." On failure (if the NPC's bravery
   stat > 60) the NPC shouts "Get away from me!" and their flee speed doubles.

---

### CCTV Tapes

Two buildings (office block and off-licence) each contain a `EVIDENCE_CCTV_TAPE`
prop that re-spawns every 10 in-game minutes. If the player commits any crime
(heist break-in, NPC knockout, alarm trigger) while within 15 blocks of those
buildings, the tape is "hot" — flagged as `tapeHot=true`.

- **Steal the tape** (press **E** near the prop): removes the prop from the world,
  cancels the associated pending WITNESSED_EVIDENCE entry, and gives the player
  `CCTV_TAPE` in inventory. Tooltip: "Evidence sorted. You're getting good at this."
- **Sell the tape to the Fence** (`FenceSystem`): earns 15 COIN regardless of
  `tapeHot` status (fences love a good tape).
- If the tape is NOT stolen within 3 in-game minutes of going hot, a POLICE NPC
  automatically gains the `WITNESSED_CRIME` entry against the player (simulating
  police reviewing footage), adding Notoriety +8.

---

### Informant Mechanic — "Grassing"

The player can optionally **tip off police** (press **E** on a POLICE NPC while
holding a rumour item — new `RUMOUR_NOTE` Material, see below) to provide intel
about a rival faction's crime. This:

- Clears one of the player's own pending `WITNESSED_CRIME` entries (the police owe
  you one — for now).
- Reduces the tipped faction's Respect by 20.
- Seeds a `BETRAYAL` rumour into the network — if the tipped faction's NPCs hear
  this rumour (3-hop spread) their Respect toward the player drops an additional 15.
- Unlocks the hidden achievement `GRASS` (no tooltip — it just silently appears).

The `RUMOUR_NOTE` is crafted from 1 COIN + 1 any rumour held in inventory
(interaction: while the barman is sharing a rumour, press **E** to "write it down").
Once used as a tip, it is consumed.

---

### New Material

| Material | Source | Stack Size | Notes |
|----------|--------|-----------|-------|
| `RUMOUR_NOTE` | Crafted at WORKBENCH: 1 COIN after hearing a barman rumour | 3 | Used to tip off police or sell to rival faction for 8 COIN |
| `CCTV_TAPE` | Stolen from office/off-licence CCTV prop | 1 | Sell to Fence for 15 COIN; destroys a Notoriety entry |

---

### New RumourTypes

| Type | Trigger | Hops before expiry | Effect on hearers |
|------|---------|--------------------|-------------------|
| `WITNESS_SIGHTING` | NPC witnesses a crime | 4 | NPCs near barman gain ANXIOUS state for 30s |
| `BETRAYAL` | Player tips off police | 5 (never expires in barman) | Target faction NPCs become hostile to player |

---

### New Achievements

| Achievement | Condition |
|-------------|-----------|
| `CLEAN_GETAWAY` | Commit a heist with 0 evidence left behind (all evidence destroyed/stolen) |
| `LEAVE_NO_TRACE` | Bribe 5 witnesses in a single session |
| `GRASS` | Tip off police (hidden achievement — tooltip suppressed) |
| `IN_BROAD_DAYLIGHT` | Commit a crime with 3+ witnesses watching simultaneously |
| `STITCH_UP` | Use a `RUMOUR_NOTE` to frame a rival faction NPC |

---

### HUD Integration

- A small **evidence counter** appears in the bottom-right corner when the player
  has active evidence props linked to their actions in the world. Each piece shows
  a countdown timer. Turns red when a POLICE NPC is within 20 blocks.
- Witnesses in WITNESS state display a speech bubble with an eye emoji substitute
  (`[!]`) over their head.
- When a tape goes "hot", a brief screen-edge red flash (1-second vignette) pulses
  once to warn the player.

---

**Unit tests**: Evidence prop spawning on crime events, evidence decay timer,
witness state transition (idle → witness → flee → report), bribe success/failure
by bravery threshold, CCTV tape hot-flag logic, tape steal cancels Notoriety entry,
RUMOUR_NOTE craft recipe, tip-off clears one WITNESSED_CRIME entry, BETRAYAL rumour
spread, all achievement trigger conditions, evidence counter HUD visibility logic.

**Integration tests — implement these exact scenarios:**

1. **Breaking glass spawns evidence and witness reacts**: Place the player inside the
   office building adjacent to a GLASS block. Place a PUBLIC NPC 8 blocks away with
   line-of-sight to the player. Break the GLASS block (simulate 2 punch actions).
   Verify an `EVIDENCE_SMASHED_GLASS` prop has spawned at the broken block's position.
   Verify the PUBLIC NPC has transitioned to `NPCState.WITNESS`. Verify a
   `WITNESS_SIGHTING` rumour exists in the NPC's rumour buffer.

2. **Witness reaches police and raises Notoriety**: Set up a Witness NPC 15 blocks
   from a POLICE NPC. Simulate NPC movement for 120 frames (2 seconds at 60fps).
   Verify the witness NPC has moved toward the POLICE NPC. When they come within
   1 block, verify the player's `CriminalRecord` contains a `WITNESSED_CRIME` entry.
   Verify player Notoriety has increased by 10.

3. **Successful bribe silences witness**: Spawn a Witness NPC fleeing from a crime
   scene. Give the player 10 COIN. Move the player adjacent to the fleeing NPC.
   Press **E**. Set bravery < 60. Verify the bribe costs 5 COIN (player now has 5).
   Verify the NPC returns to `NPCState.IDLE`. Verify the `WITNESS_SIGHTING` rumour
   has been removed from the NPC's rumour buffer.

4. **CCTV tape goes hot and triggers Notoriety if not stolen**: Trigger a crime within
   15 blocks of the office building. Verify the office building's `EVIDENCE_CCTV_TAPE`
   prop is flagged `tapeHot=true`. Advance the simulation by 3 in-game minutes without
   stealing the tape. Verify the player's Notoriety has increased by 8. Verify a
   `WITNESSED_CRIME` entry exists in the player's `CriminalRecord`.

5. **Stealing CCTV tape cancels pending Notoriety entry**: Trigger a crime near the
   office building (tape goes hot). Within 3 in-game minutes, move the player adjacent
   to the `EVIDENCE_CCTV_TAPE` prop and press **E**. Verify the prop is removed from
   the world. Verify the `CCTV_TAPE` item is in the player's inventory. Verify NO new
   `WITNESSED_CRIME` entry was added to the player's `CriminalRecord` from this event.

6. **Tipping off police clears one crime entry and seeds BETRAYAL rumour**: Add one
   `WITNESSED_CRIME` entry to the player's `CriminalRecord`. Give the player a
   `RUMOUR_NOTE`. Move the player adjacent to a POLICE NPC and press **E**. Verify
   the `WITNESSED_CRIME` entry has been removed from `CriminalRecord`. Verify the
   tipped faction's Respect has decreased by 20. Verify a `BETRAYAL` rumour has been
   seeded into the `RumourNetwork`. After 3 hops of spread, verify the tipped faction's
   NPCs that received the rumour have their hostility flag set toward the player.

7. **CLEAN_GETAWAY achievement triggers**: Execute a heist (jeweller). Destroy all
   spawned evidence props before leaving the building. Verify no POLICE NPC has
   gained a `WITNESSED_CRIME` entry. Verify the `CLEAN_GETAWAY` achievement fires
   when the player exits the building. Verify player Notoriety has not increased
   from this heist.

8. **Evidence decays after 10 in-game minutes**: Spawn an `EVIDENCE_SMASHED_GLASS`
   prop. Do not destroy it. Advance the simulation by 10 in-game minutes. Verify the
   prop has been automatically removed from the world. Verify no additional Notoriety
   entry was created (it decayed undiscovered).

9. **IN_BROAD_DAYLIGHT achievement with 3 simultaneous witnesses**: Place 3 PUBLIC
   NPCs each within 12 blocks of the player with line-of-sight. Break a block inside
   a building. Verify all 3 NPCs transition to `NPCState.WITNESS` simultaneously.
   Verify the `IN_BROAD_DAYLIGHT` achievement fires. Verify 3 `WITNESS_SIGHTING`
   rumours have been seeded into the `RumourNetwork` (one per witness).

10. **Full evidence pipeline stress test**: Execute a heist with 2 witnesses, a hot
    CCTV tape, and 3 evidence props. Bribe 1 witness. Steal the CCTV tape. Destroy
    2 evidence props (leave 1). Let the 1 undestroyed witness reach police. Verify the
    player has exactly 1 `WITNESSED_CRIME` entry (from the unbribed witness) and 0
    from CCTV. Verify Notoriety increased by exactly 10 (1 witness reaching police).
    Verify the leftover evidence prop seeds 1 `GANG_ACTIVITY` rumour within 2
    in-game minutes. Verify no NPEs and game remains in PLAYING state throughout.

---

## Phase 14: Going Undercover — Disguise & Social Engineering System

**Goal**: Let the player steal uniforms and clothing from NPCs, wear them as disguises,
and temporarily infiltrate restricted areas, faction territories, and police patrols —
until their cover is blown. Think very-budget Hitman set in a British market town. Deeply
funny, strategically rich, and completely changes the approach to heists.

### Disguise Mechanics

A new `DisguiseSystem` class tracks the player's current disguise state. Disguises are
obtained by knocking out an NPC (reduced to 0 HP) and pressing **E** to loot their
clothing. The clothing becomes a wearable `Material` item. Equipping it via the inventory
`Worn` slot activates the disguise.

**Available disguises and their effects:**

| Disguise Item | Source NPC | Restricted Access Granted | Cover Blown By |
|---------------|-----------|--------------------------|----------------|
| `POLICE_UNIFORM` | POLICE NPC | Police don't chase player; can enter police-patrolled zones freely | Walking near a PCSO or ARMED_RESPONSE NPC (they know their own); committing a crime while wearing it (+30 Notoriety) |
| `COUNCIL_JACKET` | COUNCIL_BUILDER NPC | Council builders ignore player; can freely enter and demolish structures without triggering council response | Any faction NPC (they hate the council); being arrested |
| `MARCHETTI_TRACKSUIT` | FACTION_LIEUTENANT (Marchetti) | Marchetti NPCs are friendly; player can enter Marchetti turf without Respect check | Street Lads NPCs attack on sight; POLICE NPCs become suspicious within 6 blocks (50% chance per 10 seconds to investigate) |
| `STREETLADS_HOODIE` | YOUTH_GANG NPC | Street Lads territory treated as safe; can attend drug corners without triggering PCSO | Marchetti NPCs become hostile; police suspicion radius increases to 8 blocks |
| `HI_VIS_VEST` | POSTMAN or BARMAN | Civilians and shopkeepers treat player as staff; can enter back rooms of shops; shop NPCs share loot tips freely | Any police NPC on Notoriety ≥ 2; bouncer lets player into pub without bribe |
| `GREGGS_APRON` | Greggs interior NPC | Free SAUSAGE_ROLL per visit; Greggs NPCs don't call police for property damage nearby | Hilariously fools nobody past 3 blocks — all NPCs within 3 blocks see through it immediately |

### Cover Integrity

Each disguise has a `coverIntegrity` value (0–100, starting at 100). It decays based on:
- Being within line-of-sight of a suspicious NPC: −2 per second
- Running (not walking): −5 per second (running cops don't exist — suspicious behaviour)
- Committing any crime while in disguise: −50 immediately
- Buying a round at the pub while disguised: +10 (social normalisation)

When `coverIntegrity` drops to 0, cover is blown:
- The disguise item is removed from the `Worn` slot and destroyed (can't reuse a blown cover)
- A `COVER_BLOWN` speech event fires from the nearest NPC: faction-appropriate dialogue
  (e.g. Police: "Oi — that's not one of ours!", Marchetti: "You're not one of the crew, are ya.")
- All NPCs of the relevant faction within 20 blocks enter `HOSTILE` state for 60 seconds
- Notoriety +10

`coverIntegrity` is displayed as a small coloured indicator on the HUD when disguised:
green (75–100), amber (25–74), red (0–24), with a rapid pulse animation below 25.

### Cover Suspicion Events

Certain contextual events create tension without immediately blowing cover:

- **Stop and Scrutinise**: A police NPC within 6 blocks notices something off. They stop,
  face the player, and their speech bubble shows "Hold on..." for 3 seconds. If the player
  stands still and does nothing, cover integrity loses only 15 points. If the player runs,
  cover is blown immediately.
- **Faction Recognition Check**: When entering a faction building in a rival's disguise,
  a faction lieutenant NPC performs a recognition check (3-second animation, speech: "Do I
  know you?"). Player can press **E** to bluff (RNG: 60% success, uses `RUMOUR_NOTE` if held
  as supporting prop → 90% success). Failure blows cover instantly.
- **Greggs Apron Transparency**: The `GREGGS_APRON` is played entirely for laughs. Within
  3 blocks, any NPC immediately sees through it with the speech "That's not a Greggs worker."
  Beyond 3 blocks, it works perfectly. Nobody at distance scrutinises a person in an apron.

### Integration with Existing Systems

- **Heists**: Wearing a `POLICE_UNIFORM` while executing a heist resets the alarm timer
  by 30 seconds (police responding to the alarm hesitate before shooting "one of their own").
  Blowing cover during a heist immediately triggers the full alarm.
- **Witness System**: Witnesses who see the player committing a crime while in disguise
  seed a `WITNESS_SIGHTING` rumour with the disguise type included: "Someone in a police
  uniform was breaking into the jewellers!" — this rumour causes ALL police NPCs to enter
  suspicious mode for 3 in-game minutes.
- **Faction System**: Using a rival faction's disguise to complete missions earns Respect
  from the disguised faction (+10 on success) but risks a permanent −20 if caught.
  Completing a Marchetti mission dressed as Street Lads seeds a `BETRAYAL` rumour automatically.
- **Notoriety**: Obtaining your first disguise item unlocks the `UNDERCOVER` achievement.
  Reaching Notoriety Tier 3 (Area Menace) causes police to run a "uniform check" whenever
  a player in `POLICE_UNIFORM` enters a police patrol zone (50% chance per entry to
  trigger a Stop and Scrutinise check).

### Tooltip Triggers

- On first disguise equip: "You're not you when you're in uniform."
- On cover blown for first time: "They always find out eventually."
- On Greggs Apron equipped: "Nobody will suspect a thing. Nobody within 3 metres, anyway."
- On first stop-and-scrutinise survive: "Brazened it out. Nice."
- On committing crime while disguised: "That's going to complicate things."

### New Materials

- `POLICE_UNIFORM` — wearable, obtained from knocked-out POLICE NPCs
- `COUNCIL_JACKET` — wearable, obtained from knocked-out COUNCIL_BUILDER NPCs
- `MARCHETTI_TRACKSUIT` — wearable, obtained from knocked-out FACTION_LIEUTENANT (Marchetti) NPCs
- `STREETLADS_HOODIE` — wearable, obtained from knocked-out YOUTH_GANG NPCs
- `HI_VIS_VEST` — wearable, obtained from knocked-out POSTMAN or BARMAN NPCs
- `GREGGS_APRON` — wearable, obtainable from Greggs interior; gag item

### New Achievements

| Achievement | Trigger |
|-------------|---------|
| `UNDERCOVER` | Equip any disguise for the first time |
| `METHOD_ACTOR` | Successfully complete a heist while wearing a `POLICE_UNIFORM` |
| `TURNCOAT` | Complete missions for two rival factions in the same session, each while wearing the other faction's disguise |
| `OBVIOUS_IN_HINDSIGHT` | Have cover blown by the `GREGGS_APRON` (the only way it can happen is by walking within 3 blocks of someone) |
| `INCOGNITO` | Maintain cover integrity above 50 for a full 10-minute session across all 3 faction zones |

**Key binding**: No new bindings required. Disguise equip/unequip uses the existing inventory
`Worn` slot system (**I** key). The `DisguiseSystem` integrates with the `InventoryUI`.

**Unit tests**: Cover integrity decay rates under each suspicion event, faction recognition
check RNG outcomes, stop-and-scrutinise timer, cover-blown faction hostility radius, disguise
item loot logic from NPC knockout, Greggs apron 3-block transparency rule, heist alarm delay
with police uniform, witness rumour generation with disguise context.

**Integration tests — implement these exact scenarios:**

1. **Looting and equipping a police uniform**: Knock out a POLICE NPC (reduce to 0 HP).
   Press **E** on the downed NPC. Verify `POLICE_UNIFORM` is added to player inventory.
   Open inventory and equip it in the `Worn` slot. Verify `disguiseSystem.isDisguised()` is
   true. Verify `disguiseSystem.getCurrentDisguise()` is `Material.POLICE_UNIFORM`. Verify
   cover integrity starts at 100.

2. **Police ignore player wearing police uniform**: Give the player a `POLICE_UNIFORM` and
   equip it. Set Notoriety to 0. Place a POLICE NPC 5 blocks from the player. Advance 120
   frames. Verify the POLICE NPC does NOT enter `CHASING` or `ARRESTING` state. Verify the
   NPC does not change direction toward the player. Remove the disguise. Advance 120 frames.
   Verify the police NPC now patrols normally (this baseline confirms detection works without
   disguise).

3. **Cover integrity decays under police scrutiny**: Equip `POLICE_UNIFORM`. Place a PCSO NPC
   within 4 blocks of the player (PCSO sees through police disguise). Advance 60 frames (1
   second). Verify cover integrity has decreased from 100 (expected: approx. 88 after 6
   seconds of −2/s). Verify the `COVER_BLOWN` event has NOT fired yet. Continue advancing
   until integrity reaches 0. Verify cover blown event fires, `POLICE_UNIFORM` is removed
   from worn slot, and all PCSO NPCs within 20 blocks enter `HOSTILE` state.

4. **Stop-and-scrutinise: standing still preserves cover**: Equip `MARCHETTI_TRACKSUIT`.
   Place a POLICE NPC 5 blocks away (within suspicion range). The NPC should enter the
   "scrutinise" state (speech: "Hold on..."). Player performs no input for 3 seconds (180
   frames). Verify after 3 seconds the police NPC resumes patrol (scrutiny passed). Verify
   cover integrity lost exactly 15 points.

5. **Running during scrutiny blows cover immediately**: Equip `MARCHETTI_TRACKSUIT`. Place
   POLICE NPC 5 blocks away triggering scrutiny. Simulate pressing W (running) during the
   3-second scrutiny window. Verify cover is blown immediately (within the same frame as
   input). Verify `COVER_BLOWN` speech fires from the police NPC.

6. **Committing crime while disguised costs heavy cover**: Equip `COUNCIL_JACKET`. Break a
   block (crime committed). Verify cover integrity drops from 100 to 50 (−50 immediately).
   Verify a `WITNESS_SIGHTING` rumour seeded by any witness includes the disguise context
   string "council jacket".

7. **Heist alarm delay with police uniform**: Set up a heist at the jeweller. Equip
   `POLICE_UNIFORM`. Trigger the heist alarm (as per `HeistSystem` alarm conditions). Verify
   the alarm-response timer is extended by 30 seconds compared to the baseline (unequipped)
   alarm response time. Verify police NPCs responding to the alarm pause at the entrance for
   at least 30 additional seconds before entering `CHASING` state.

8. **Greggs apron fools nobody within 3 blocks**: Equip `GREGGS_APRON`. Place a PUBLIC NPC
   4 blocks from the player (should be fooled). Advance 60 frames. Verify no hostile reaction.
   Move player to within 2 blocks of the PUBLIC NPC. Verify the NPC immediately says "That's
   not a Greggs worker." and cover integrity drops to 0 within 1 frame. Verify the tooltip
   "Nobody will suspect a thing. Nobody within 3 metres, anyway." has already fired on
   initial equip.

9. **METHOD_ACTOR achievement**: Equip `POLICE_UNIFORM`. Execute a full heist (enter
   jeweller, crack safe, exit). Verify the `METHOD_ACTOR` achievement fires on heist
   completion. Verify cover integrity was above 0 throughout the heist (otherwise achievement
   should NOT fire if cover was blown mid-heist).

10. **Full disguise lifecycle stress test**: Obtain and equip all 5 serious disguises
    (`POLICE_UNIFORM`, `COUNCIL_JACKET`, `MARCHETTI_TRACKSUIT`, `STREETLADS_HOODIE`,
    `HI_VIS_VEST`) in sequence. For each: verify the appropriate faction NPCs react
    correctly (allies don't attack, rivals become suspicious). Blow cover for each disguise.
    Verify all 5 cover-blown events fire the correct faction dialogue. Verify after all 5
    blown covers: player Notoriety has increased by 50 (5 × 10). Verify no NPEs and game
    remains in PLAYING state. Verify the `UNDERCOVER` achievement fired on the first disguise.

---

## Phase 13: Dynamic NPC Needs & Black Market Economy — 'The Street Hustle'

**Goal**: Give every NPC a living set of personal needs and fears that the player can exploit,
satisfy, or manipulate for profit, information, or leverage. The world becomes a web of
opportunity: corner markets, run protection rackets, supply shortages, or be undercut by
rival dealers. Pure British dark comedy — everyone wants something, nobody has enough money,
and the council is somehow making it worse.

### NPC Need System

Every NPC has a hidden **Need Score** (0–100) for up to 3 concurrent needs drawn from a
typed pool. Needs accumulate over time and are satisfied by player interaction or world events.
An unsatisfied need above 80 makes the NPC visibly distressed (speech bubble, agitated
animations). A satisfied NPC pays a premium and seeds positive rumours. A chronically
unsatisfied NPC eventually leaves the area (despawn/migrate) or turns hostile.

**Need types** (`NeedType` enum):

| Need | Accumulation rate | What satisfies it | Visual tell |
|------|-------------------|-------------------|-------------|
| `HUNGRY` | +10/min | Give `GREGGS_PASTRY`, `TINNED_FOOD`, or `SANDWICH` | Clutching stomach |
| `THIRSTY` | +8/min | Give `CAN_OF_LAGER`, `CUP_OF_TEA`, or `WATER_BOTTLE` | Licking lips |
| `BORED` | +5/min | Give `NEWSPAPER`, `CIGARETTE`, or start conversation | Kicking pebble |
| `COLD` | +15/min (night/rain only) | Give `WOOLLY_HAT`, `SLEEPING_BAG`, or light nearby `CAMPFIRE` | Shivering |
| `SCARED` | +20/min near crime | Give `RUMOUR_NOTE` (reassuring lie), or eliminate threat | Cowering |
| `BROKE` | +3/min passively | Give `COIN` (any amount ≥ 3) | Checking empty wallet |
| `DESPERATE` | +25/min if `BROKE` > 70 | Black market special job offer unlocked | Muttering to self |

NPCs regenerate at the `NPC_NEED_DECAY_RATE` of −2/min when their need is satisfied. Each
NPC can have at most 3 active needs at once. All need values persist as long as the NPC is
loaded (chunk-resident).

### Black Market Supply & Demand

A new `StreetEconomySystem` tracks **global supply** of key contraband items and
**adjustable prices** based on scarcity. Prices are published per-item in a floating ledger
that all fence and NPC traders reference.

**Price formula**: `price = baseCost × (1 + (demandUnits − supplyUnits) / 50)`
- Clamped between `0.2 × baseCost` (floor — you can't sell a pastry for pennies even if
  Greggs is giving them away) and `5 × baseCost` (ceiling — even in a drought, nobody pays
  £50 for a can of Lager).
- Supply increases when the player sells goods to fences or leaves items in world containers.
- Supply decreases naturally at `SUPPLY_DECAY_RATE` of 2 units/min (items "consumed" by world).
- Demand spikes +20 when a matching NPC need reaches 80+.
- Demand falls −10 when player satisfies 5 matching needs within 10 in-game minutes.

**Contraband tiers** for pricing:

| Item | Base cost (COIN) | Primary need satisfied | Notes |
|------|-----------------|----------------------|-------|
| `GREGGS_PASTRY` | 2 | `HUNGRY` | Ubiquitous, low margin |
| `CAN_OF_LAGER` | 3 | `THIRSTY` / `BORED` | Double need, decent margin |
| `CIGARETTE` | 2 | `BORED` | Infinite restock if player has `TOBACCO_POUCH` |
| `WOOLLY_HAT` | 5 | `COLD` | Seasonal price spikes at night/rain |
| `SLEEPING_BAG` | 8 | `COLD` | Rare, high margin |
| `STOLEN_PHONE` | 15 | `BROKE` (partial) | Risky — evidence if found on player |
| `PRESCRIPTION_MEDS` | 20 | `DESPERATE` (special) | Marchetti supply chain item |
| `COUNTERFEIT_NOTE` | 10 | `BROKE` | Fence won't take them; NPC 50% chance to notice |

### Street Dealing Interaction

When a player has items an NPC needs, pressing **E** on the NPC opens a contextual deal prompt
(no full menu — a single line HUD overlay: `"Psst — got a [item]? I'll give you X coin."`).
The player can:
- **Accept** (press E again): trade completes, NPC need drops by 60, player gains coin.
- **Haggle** (press H): random ±30% price shift, NPC may refuse entirely if Boredom > 60.
- **Refuse** (ESC): NPC stays needy.

**Dealer reputation** (`DealerReputation` int, 0–100, per NPC): Rises +5 per successful trade,
falls −10 per refused deal when NPC is Desperate. High reputation (> 70) unlocks exclusive
requests: NPCs offer jobs that aren't in the main faction mission pool. Low reputation
(< 20) causes NPCs to refuse to deal and bad-mouth the player (`BETRAYAL` rumour seeds).

### Protection Racket Mechanic

Once the player controls ≥ 40 % of a faction's turf (via `TurfMap`), they unlock the ability
to run a **protection racket** on up to 3 businesses (Greggs, off-licence, charity shop,
bookies). Each protected business:
- Pays `RACKET_WEEKLY_INCOME` of 15 COIN per in-game day automatically deposited to
  the player's stash container.
- Can be "taxed up" by interacting with the shopkeeper NPC while Notoriety < 40 (they
  pay willingly). Above Notoriety 40, the shopkeeper calls police instead.
- Rival factions can "move in" on the racket: if a rival's turf share of the same block
  zone exceeds 50 %, they start collecting instead, and the player receives a hostile
  warning rumour.

### Market Disruption Events

Every 5 in-game minutes, the `StreetEconomySystem` rolls one random disruption event from
the `MarketEvent` enum:

| Event | Effect | Duration |
|-------|--------|----------|
| `COUNCIL_CRACKDOWN` | All fence prices −30 %, police patrol frequency ×2 | 10 min |
| `GREGGS_STRIKE` | `HUNGRY` need rate ×3, `GREGGS_PASTRY` price ×4 | 15 min |
| `LAGER_SHORTAGE` | `CAN_OF_LAGER` price ×5, fights between YOUTH_GANG NPCs | 8 min |
| `BENEFIT_DAY` | All NPC `BROKE` needs reset to 0, prices fall 20 % across board | 5 min |
| `MARCHETTI_SHIPMENT` | `PRESCRIPTION_MEDS` supply +50, price halves temporarily | 10 min |
| `COLD_SNAP` | `COLD` need rate ×4 regardless of weather, `WOOLLY_HAT` price ×6 | 20 min |

The active event is announced via a rumour seeded into 3 random NPCs and visible in the
barman's rumour log. The player can **exploit** disruptions: stocking up on lager before a
shortage, buying hats in bulk before a cold snap, or triggering a `COUNCIL_CRACKDOWN` by
calling in a tip (via the informant mechanic in WitnessSystem).

### Integration with Existing Systems

- **Faction System**: Buying exclusively from Marchetti-supplied goods raises Marchetti
  Respect +2/trade. Refusing to supply Street Lads NPCs (with Boredom > 80 repeatedly)
  causes Street Lads Respect −5.
- **Rumour Network**: Successful deals seed `TRADE_RUMOUR` — other NPCs learn the player is
  a reliable supplier, increasing deal request frequency. Failed deals or counter-feits seed
  `BETRAYAL` rumours.
- **Disguise System**: Dealing in `POLICE_UNIFORM` grants a 20 % price bonus (NPCs think
  it's "official"). Wearing a `GREGGS_APRON` while selling `GREGGS_PASTRY` raises acceptance
  rate to 100 % (within 3+ blocks, naturally).
- **Weather System**: `COLD` need accumulates 4× faster during `RAIN` or at night.
  `WeatherNPCBehaviour` triggers visible shivering at `COLD` > 60.
- **Notoriety System**: Running a protection racket without disguise raises Notoriety +1/day.
  Getting caught with `COUNTERFEIT_NOTE` raises Notoriety +15 immediately.
- **Criminal Record**: Trading `STOLEN_PHONE` or `PRESCRIPTION_MEDS` adds a
  `CrimeType.HANDLING_STOLEN_GOODS` entry if a police NPC witnesses the exchange within
  `WITNESS_LOS_RANGE`.

### New Materials

- `GREGGS_PASTRY` — consumable, crafted from `FLOUR` + `BUTTER` at campfire, or looted from Greggs
- `CAN_OF_LAGER` — consumable, looted from off-licence shelves or crafted at campfire
- `CIGARETTE` — consumable, looted from shops or crafted from `TOBACCO_POUCH`
- `WOOLLY_HAT` — wearable/tradeable, looted from charity shop or dropped by cold NPCs
- `SLEEPING_BAG` — inventory item, found in squats, high trade value
- `STOLEN_PHONE` — inventory item, dropped by pickpocket interactions (new mechanic)
- `PRESCRIPTION_MEDS` — inventory item, from Marchetti supply crates or pharmacy raids
- `COUNTERFEIT_NOTE` — inventory item, craftable from `PAPER` + `INK` at workbench (risky)
- `TOBACCO_POUCH` — inventory item, looted from NPCs, used to craft CIGARETTE ×5
- `NEWSPAPER` — inventory item, spawned daily on doorsteps, reduces `BORED` need

### New Achievements

| Achievement | Trigger |
|-------------|---------|
| `ENTREPRENEUR` | Complete 50 successful street deals |
| `LOAN_SHARK` | Have 3 NPCs simultaneously in your debt (dealer reputation > 70 with each) |
| `CORNERED_THE_MARKET` | Be the sole supplier of any item during a shortage event |
| `BENEFIT_FRAUD` | Collect protection money from all 4 businesses on Benefit Day |
| `COLD_SNAP_CAPITALIST` | Sell 10 `WOOLLY_HAT` during a single `COLD_SNAP` event |
| `DODGY_AS_THEY_COME` | Successfully pass a counterfeit note without detection |

### New Key Binding

No new bindings required. Street deal prompt uses **E** (interact). Haggling uses **H**
(already bound to Help UI, but only in non-UI context). Protection racket income is
collected passively or on **E** interaction with shopkeeper NPC in racket mode.

**Unit tests**: Need accumulation rates, price formula clamping, disruption event selection
RNG, deal acceptance/refusal logic per NPC bravery, dealer reputation tracking, racket income
calculation, counterfeit detection probability, supply/demand price updates.

**Integration tests — implement these exact scenarios:**

1. **NPC need accumulates and triggers distress**: Spawn an NPC of type `PUBLIC` with all
   needs at 0. Advance 10 in-game minutes (600 real frames at 1 min/60 frames scale). Verify
   `HUNGRY` need score is ≥ 60 (rate: +10/min × 6 min assuming other needs consume slots).
   Verify at 80+ need the NPC displays a clutching-stomach speech bubble. Verify the NPC is
   visible distress state `NEED_DISTRESS`.

2. **Player satisfies need and earns coin**: Spawn NPC with `HUNGRY` = 85. Ensure player has
   1× `GREGGS_PASTRY` in inventory. Press **E** on NPC. Verify deal prompt appears: text
   contains "Greggs" and a coin value ≥ 2. Press **E** again to accept. Verify player inventory
   loses 1× `GREGGS_PASTRY`. Verify player `COIN` increases by expected amount (≥ baseCost).
   Verify NPC `HUNGRY` drops to ≤ 25.

3. **Price responds to supply shortage**: Set global `CAN_OF_LAGER` supply to 2 (scarce).
   Set global demand to 50. Verify `StreetEconomySystem.getPrice(Material.CAN_OF_LAGER)`
   returns a value > baseCost (3 COIN). Set supply to 200. Verify price drops below baseCost.
   Verify price never falls below floor (0.2 × 3 = 0.6, rounds to 1 COIN minimum).

4. **Disruption event: GREGGS_STRIKE spikes HUNGRY need and price**: Manually trigger
   `MarketEvent.GREGGS_STRIKE`. Advance 2 in-game minutes. Verify all PUBLIC NPCs within
   loaded chunks have `HUNGRY` accumulation rate active at 3× normal (30/min). Verify
   `StreetEconomySystem.getPrice(Material.GREGGS_PASTRY)` is ≥ 4× baseCost. After 15
   in-game minutes, verify event expires and rates return to normal.

5. **Protection racket pays out**: Capture ≥ 40 % of a zone's turf for the player's aligned
   faction. Interact with Greggs shopkeeper NPC with Notoriety < 40. Verify racket is
   registered. Advance 1 in-game day (1440 in-game minutes). Verify player stash container
   has received exactly 15 COIN. Verify rival faction has NOT collected instead (their turf
   share < 50 % in zone).

6. **Rival faction moves in on racket**: Establish racket on the off-licence. Manually set
   rival faction turf share for that block zone to 60 %. Advance 1 in-game day. Verify player
   stash does NOT receive income. Verify a hostile warning rumour of type `GANG_ACTIVITY` is
   seeded to 3 NPCs containing text about the off-licence. Verify player receives a speech
   notification "Someone's been collecting your cut."

7. **Counterfeit note 50 % detection**: Give player 10× `COUNTERFEIT_NOTE`. Sell each to
   a separate NPC (spawn 10 NPCs with `BROKE` > 70). Verify that between 3 and 7 NPCs detect
   the fake (50 % ± reasonable variance from RNG seeded at fixed value). For each detected
   fake: verify NPC enters `HOSTILE` state, player Notoriety +15, and
   `CrimeType.HANDLING_STOLEN_GOODS` added to criminal record.

8. **Dealer reputation progression**: Complete 15 successful trades with a single NPC.
   Verify dealer reputation for that NPC is 75. Verify the NPC now offers an exclusive job
   request (quest not in normal faction pool). Refuse 2 deals when NPC is `DESPERATE`.
   Verify reputation drops to 55. Verify the NPC seeds a `BETRAYAL` rumour mentioning
   the player by name.

9. **Cold snap drives woolly hat sales**: Trigger `COLD_SNAP` disruption. Verify `COLD`
   need accumulates on all outdoor NPCs at 4× rate. Give player 10× `WOOLLY_HAT`. Sell 10
   hats to 10 separate NPCs. Verify `COLD_SNAP_CAPITALIST` achievement fires. Verify total
   COIN earned is ≥ 10 × (6 × baseCost) — price at ×6 during cold snap.

10. **Full economy stress test**: Trigger all 6 disruption events in sequence (each for their
    full duration). After all events: verify supply/demand values for all tracked items are
    within valid ranges (supply ≥ 0, demand ≥ 0, price between floor and ceiling). Verify no
    NPCs have need scores above 100 or below 0. Verify game remains in PLAYING state with no
    NPEs. Verify barman NPC's rumour log contains at least 4 distinct `TRADE_RUMOUR` entries
    (from NPC gossip about player activity).

---

## Phase S: Hot Pursuit — Wanted System, Police Chases & Getaway Mechanics

**Goal**: Commit a crime, get spotted, and desperately leg it through British streets
while the Old Bill closes in. Every criminal act now carries real, escalating
consequences — and the sweet relief of losing your tail makes every close shave
memorable.

### Overview

When the player commits a witnessed crime (assault, theft, breaking & entering,
handling stolen goods, etc.), they accumulate a **Wanted Level** (0–5 stars). At
each tier the police response escalates. The player must **lose their wanted level**
by breaking line-of-sight, ducking into hiding spots, changing appearance via the
Disguise System, or bribing a corrupt PCSO. Getting caught results in arrest,
criminal record update, and a fine / confiscation.

### Wanted Level Tiers

| Stars | Name | Police Response |
|-------|------|----------------|
| 0 | Clean | No police interest |
| 1 | Person of Interest | 1 PCSO investigates last-seen position |
| 2 | Wanted | 2 POLICE NPCs actively chase the player |
| 3 | Manhunt | 4 POLICE + patrol car circles the area |
| 4 | Armed Response | 2 ARMED_RESPONSE NPCs, all police hostile on sight |
| 5 | Full Lockdown | All police + armed response + Council dispatch; player movement slowed 30 % (kettled) |

### Escalation Rules

- **Crime witness** (`WitnessSystem`): each witnessed crime adds Wanted Level points
  equal to its `CrimeType.severity` (MINOR=1, MODERATE=2, SEVERE=3, EXTREME=5).
- **Radio chatter** (flavour): when Wanted Level rises, NPC police "radio in" — text
  shown as speech bubble: *"Suspect on foot, heading [direction], over."*
- **Decay**: Wanted Level decays by 1 star per 90 real seconds **only** if the player
  is outside any police NPC's `LINE_OF_SIGHT_RANGE` (12 blocks). Entering LOS resets
  the decay timer.
- **Maximum escalation cap**: Wanted Level cannot exceed 5. Achieving 5 stars during
  an active `COUNCIL_CRACKDOWN` market event immediately triggers a
  `GangTerritorySystem` faction lockdown (all faction NPCs hostile).

### Chase Mechanics

Police NPCs in pursuit mode use `Pathfinder` to navigate toward the player's
**last-known position** (LKP), updated each time a police NPC has LOS to the player.
If the player breaks LOS, police continue to the LKP, then fan out for
`SEARCH_DURATION_SECONDS` (30 s) before giving up and returning to patrol.

**Chase speed**: pursuing POLICE NPCs move at `CHASE_SPEED_MULTIPLIER` (1.4×) normal
NPC speed. ARMED_RESPONSE move at 1.2× (heavier kit).

**Cornering**: if 2+ police NPCs are within 3 blocks of the player simultaneously, the
player is considered **cornered** and arrested automatically (no further input needed).

### Hiding Mechanics

The player can duck into **hiding spots** — specific prop/block contexts:

| Hiding Spot | Block/Prop | Concealment Duration | Notes |
|-------------|-----------|---------------------|-------|
| Wheelie Bin | `WHEELIE_BIN` prop | Until police pass | Cancelled if bin nudged by NPC |
| Shop doorway | Any `DOOR` block in a `LANDMARK` | 20 s max | Police may enter if suspicion high |
| Under stairwell | WOOD ceiling ≤ 2 blocks above, enclosed 3 sides | Indefinite | Must be still |
| Charity Shop changing room | Inside `CHARITY_SHOP` landmark | 45 s | Police require Wanted ≥ 3 to enter |
| Pub toilet | Inside `PUB` landmark, near `TOILET` prop | 60 s | Police require Wanted ≥ 4 to enter |

While **hidden**, the player cannot move or attack. Press `SHIFT` (crouch/hide toggle)
to enter/exit. A hiding progress bar shows remaining safe time. If police walk within
2 blocks of a hiding spot at Wanted ≥ 4, a `SEARCH_DISCOVERED` event fires and
concealment is broken.

### Losing the Heat

The player can drop their Wanted Level to 0 (without being arrested) via:

1. **Disguise change**: Swap to a disguise the police haven't seen (using
   `DisguiseSystem.equip()`). Each disguise change resets police description — but
   only works **once per pursuit** at Wanted ≤ 3. The police description update is
   announced as: *"Suspect has changed appearance — description updated."*

2. **Bribe a PCSO**: If Wanted Level is 1–2, press `E` on a PCSO NPC and pay
   `BRIBE_COST_PER_STAR × wantedLevel` COIN. The PCSO looks away, Wanted Level
   drops to 0, and a `CORRUPTION` rumour is seeded. Only available if player
   Notoriety < 60 (otherwise PCSO refuses: *"I know who you are, sunshine."*).

3. **Safe house**: Enter the player's own squat (`SquatSystem`) with Wanted Level ≤ 3.
   After `SAFE_HOUSE_COOLDOWN_SECONDS` (120 s) inside without police entering, Wanted
   Level resets to 0. Police will not enter at Wanted ≤ 3 but will surround and wait
   at Wanted 4–5.

4. **Leg it far enough**: If the player travels > `FLEE_DISTANCE_BLOCKS` (80 blocks)
   from the LKP and breaks LOS for `FULL_ESCAPE_LOS_BREAK_SECONDS` (60 s), Wanted
   Level drops by 2 stars.

### Arrest Sequence

When cornered or caught in a search:

1. Screen fades to black for 1 second (cinematic cut via `CinematicCamera`).
2. Player is teleported to a fixed **Police Station** landmark.
3. Items flagged as stolen/dodgy in inventory are **confiscated** (removed).
4. A fine of `FINE_PER_WANTED_STAR × wantedLevel × 10` COIN is deducted. If the
   player cannot pay, a `COMMUNITY_SERVICE` quest is added to their quest log.
5. `CriminalRecord` is updated with `ARRESTED` entry plus all witnessed crimes in
   the current session.
6. Wanted Level resets to 0.
7. `NotorietySystem` adds `NOTORIETY_PER_ARREST` (5) points.
8. The barman NPC seeds a rumour: *"[PlayerName] got nicked again. Numpty."*

### Corrupt PCSO & Informant Network

At Notoriety < 40, the player can **cultivate a corrupt PCSO** (unique named NPC in
the world) by buying them coffees (`FLASK_OF_TEA`, 3 interactions). Once cultivated:
- Bribe cost halved.
- PCSO tips off the player via speech bubble when a patrol is heading their way
  (within 20 blocks): *"Heads up — Charlie's coming round the corner."*
- If the player grasses on a rival faction member to the PCSO
  (`WitnessSystem.reportCrime()`), Notoriety drops by 10 and a
  `BETRAYAL` rumour is seeded naming the informant.

### Achievements

| Achievement | Trigger |
|-------------|---------|
| `LEG_IT` | Escape from Wanted Level 3+ without being arrested |
| `BENT_COPPER` | Successfully bribe a PCSO |
| `CLEAN_GETAWAY` | Complete a Faction mission at Wanted Level 2+ and lose the heat |
| `FIVE_STAR_NIGHTMARE` | Reach Wanted Level 5 and survive for 60 seconds |
| `WHEELIE_BIN_HERO` | Hide in a wheelie bin while 3+ police NPCs pass within 5 blocks |
| `INNOCENT_FACE` | Use a disguise change to drop from Wanted 3 to 0 in a single pursuit |

### New Constants (in `WantedSystem`)

```
CHASE_SPEED_MULTIPLIER          = 1.4f
SEARCH_DURATION_SECONDS         = 30f
LOS_RANGE_POLICE                = 12 blocks
WANTED_DECAY_INTERVAL_SECONDS   = 90f
BRIBE_COST_PER_STAR             = 8   (COIN)
SAFE_HOUSE_COOLDOWN_SECONDS     = 120f
FLEE_DISTANCE_BLOCKS            = 80
FULL_ESCAPE_LOS_BREAK_SECONDS   = 60f
FINE_PER_WANTED_STAR            = 10  (COIN × wantedLevel)
NOTORIETY_PER_ARREST            = 5
CORRUPT_PCSO_CULTIVATE_COUNT    = 3   (flask interactions)
```

### Integration with Existing Systems

- **WitnessSystem**: `CrimeType.severity` directly feeds Wanted Level escalation.
  All witnessed crimes are bundled into the arrest `CriminalRecord` entry.
- **DisguiseSystem**: Disguise-change escape only works if the new disguise hasn't been
  "burned" (seen by police) in the current session. Each `DisguiseSystem.equip()` call
  is logged for the current wanted session.
- **NotorietySystem**: Arrest adds `NOTORIETY_PER_ARREST`. Successful escape from ≥3
  stars reduces Notoriety by 2 (street cred for swerving the feds).
- **FactionSystem**: At Wanted 5 during `COUNCIL_CRACKDOWN`, all factions treat the
  player as the enemy — even allied ones. Completing a Faction mission while wanted
  grants +5 extra Respect ("you're dedicated, I'll give you that").
- **SquatSystem / PropertySystem**: The player's squat is the designated safe house.
  Enemies cannot mark the squat's interior blocks for destruction during a police siege.
- **RaveSystem**: Hosting a rave while Wanted ≥ 2 immediately spikes police alert
  to `POLICE_ALERT_SECONDS / 2` (rave draws extra attention).
- **StreetEconomySystem**: Trading dodgy goods (stolen phone, counterfeit notes) at
  Wanted ≥ 1 has a 50 % chance of the buyer refusing and calling the police instead,
  raising Wanted Level by 1.
- **TimeSystem / WeatherSystem**: Night-time pursuits are harder for police (LOS range
  reduced to 8 blocks at night). Rain reduces LOS to 6 blocks. Fog reduces to 4 blocks.

### New Key Bindings

- **SHIFT**: Crouch / enter hiding spot (hold to crouch-walk at reduced noise)
- **B**: Bribe nearby PCSO (context-sensitive, only active when valid PCSO in range and
  conditions met)

**Unit tests**: Wanted level escalation per crime severity, decay timer reset on LOS,
chase LKP tracking, hiding spot concealment rules, bribe cost formula, arrest fine
calculation, disguise-burn tracking, PCSO cultivation counter.

**Integration tests — implement these exact scenarios:**

1. **Crime witnessed raises wanted level**: Place player at (50, 1, 50). Place a
   POLICE NPC with LOS to the player. Trigger `CrimeType.ASSAULT` (severity 2).
   Verify Wanted Level is now 2. Verify a POLICE NPC enters chase mode targeting the
   player's position. Verify a speech bubble appears on the POLICE NPC containing
   "Suspect on foot".

2. **Breaking LOS starts decay timer**: Set Wanted Level to 2. Move the player 15
   blocks away from all police NPCs (beyond `LOS_RANGE_POLICE`). Advance 90 real
   seconds. Verify Wanted Level is now 1. Advance another 90 seconds. Verify Wanted
   Level is now 0.

3. **LOS contact resets decay timer**: Set Wanted Level to 2. Move player outside LOS.
   Advance 45 seconds (half decay interval). Move a POLICE NPC within 10 blocks of
   player (re-establishing LOS). Advance another 60 seconds. Verify Wanted Level is
   still 2 (timer was reset on LOS contact, not enough time has passed for a full cycle).

4. **Player arrested when cornered**: Place the player at (50, 1, 50). Place 2 POLICE
   NPCs at (52, 1, 50) and (48, 1, 50) (within 3 blocks on either side). Verify
   arrest sequence fires: player position changes to the Police Station landmark
   location, Wanted Level resets to 0, and `CriminalRecord` contains an `ARRESTED`
   entry. Verify dodgy items are removed from inventory.

5. **Hiding in wheelie bin conceals player**: Set Wanted Level to 3. Place a
   `WHEELIE_BIN` prop at (50, 1, 52). Move player to (50, 1, 52) and press SHIFT.
   Verify player enters hidden state. Move 4 POLICE NPCs to within 4 blocks. Verify
   police do NOT detect the player (no arrest, no LOS update). Verify `WHEELIE_BIN_HERO`
   achievement fires.

6. **Disguise change drops wanted level**: Set Wanted Level to 3. Call
   `DisguiseSystem.equip(GREGGS_APRON)` (a disguise the police haven't seen this
   session). Verify Wanted Level drops to 0. Call `DisguiseSystem.equip(GREGGS_APRON)`
   again (same disguise, now burned). Verify Wanted Level does NOT change.

7. **Bribe PCSO drops wanted level**: Set Wanted Level to 2. Give player 20 COIN.
   Place a PCSO NPC within 3 blocks. Set player Notoriety to 30. Press B (bribe).
   Verify 16 COIN (2 stars × 8 COIN/star) is deducted. Verify Wanted Level drops to 0.
   Verify a `CORRUPTION` rumour is seeded to at least 1 NPC.

8. **Safe house escape**: Set Wanted Level to 2. Move player into the squat interior.
   Advance 120 real seconds without any police NPC entering. Verify Wanted Level resets
   to 0. Verify no `ARRESTED` entry was added to `CriminalRecord`.

9. **Leg it escape — distance + LOS break**: Set Wanted Level to 3. Move player 85
   blocks from the last-known-position. Break LOS with all police for 60 seconds.
   Verify Wanted Level drops by 2 (to 1). Verify `LEG_IT` achievement fires.

10. **Night reduces police LOS range**: Set `TimeSystem` to 22:00 (night). Spawn a
    POLICE NPC. Place player 10 blocks away. Verify player is NOT within police LOS
    (night LOS = 8 blocks). Move player to 7 blocks away. Verify player IS now within
    LOS. Trigger a crime. Verify Wanted Level increases.

---

## Phase T: The Daily Ragamuffin — Living Tabloid Newspaper & Infamy Chronicle

**Goal**: A dynamic, procedurally-generated British tabloid newspaper that chronicles the player's criminal exploits in lurid, sensationalist prose — turning every heist, chase, and faction war into tomorrow's front page. NPCs react to headlines they've read, police patrols intensify after major stories, and the player can manipulate their own press coverage by tipping off journalists or planting misinformation.

### Core Concept

Every evening at 18:00 game-time, a new edition of **The Daily Ragamuffin** is published. It is a physical collectible item that spawns in a stack outside the newsagent (off-licence), on park benches, and through letterboxes of terraced houses. The player can pick it up, read it in their inventory (press **R** with newspaper selected), and — crucially — so can NPCs. When an NPC reads a newspaper, their dialogue and behaviour updates to reflect the current headlines.

The paper has three sections:
- **Front Page**: The biggest crime/event from the last 24 hours (in-game). If the player committed a sufficiently dramatic act (5-star chase, jewellery heist, Greggs raid, turf war, etc.), they feature here under a sensationalist headline. No story → local filler ("PIGEON MENACE GRIPS TOWN CENTRE").
- **Local Briefs**: 2–3 shorter items covering faction turf shifts, market events (Greggs Strike, Lager Shortage), NPC gang activity, and council announcements.
- **Classified Ads**: Procedural in-world adverts — fences advertising dodgy goods, Job Centre notices, council planning applications for buildings the player has vandalised — providing gameplay hints and dark comedy.

### Infamy Score & Headlines

Each major player action generates an **Infamy Score** (1–10). The score determines whether an action makes the front page and how dramatic the headline is:

| Infamy Score | Threshold Event | Example Headline |
|---|---|---|
| 1–3 | Minor crime (PCSO stop, 1-star chase) | "LOCAL MAN QUESTIONED BY POLICE" |
| 4–5 | Notable crime (block break heist, protection racket exposed) | "BRAZEN RAIDERS TARGET JEWELLERS" |
| 6–7 | Major crime (4-star chase, Greggs raid, gang war) | "WANTED FUGITIVE TERRORISES HIGH STREET" |
| 8–9 | Spectacular crime (5-star lockdown, faction takeover) | "BOROUGH IN CHAOS: CRIMINAL MASTERMIND EVADES FEDS" |
| 10 | Legendary (complete heist + escape + Greggs raid same day) | "RAGAMUFFIN: BRITAIN'S MOST WANTED — EXCLUSIVE" |

Headlines are generated from a **template system** that fills in specific details: landmark names, item names, NPC names (witnesses), wanted star count, escape method used, faction involved. This creates unique, specific-feeling headlines rather than generic text.

### NPC Reaction System

NPCs that pick up (or overhear) a newspaper headline update their `speechText` and behaviour:

- **Public NPCs** comment on the front page story ("Did you see what happened at the jewellers? Proper mad."). If the player is the subject, they may say "Here, aren't you that bloke from the paper?"
- **Police NPCs** get a **Heightened Alert** buff for 5 in-game minutes after a 7+ infamy story: LOS range +4 blocks, patrol speed ×1.2, new NPCs spawned.
- **Fence NPCs** offer a **10% premium** on stolen goods featured in the paper ("Hot right now, mate — everyone wants one.").
- **Barman NPCs** seed the headline as a `RumourType.LOOT_TIP` or `GANG_ACTIVITY` rumour into the rumour network.
- **Faction NPCs** react to faction-relevant stories: MARCHETTI_CREW NPCs become proud after a Marchetti story, give +5 Respect. COUNCIL NPCs become outraged, triggering a council demolition team response.

### Press Manipulation

The player can interact with the newspaper system in two active ways:

1. **Tip-Off**: Find the journalist NPC (spawns in the pub between 19:00–22:00). Press E to interact. Pay 5 COIN. Choose a past action to "leak" to the press. Next edition features that story — even if it wouldn't have made it organically. Useful for spiking fence prices or building faction infamy deliberately.

2. **Plant a Lie**: Pay 15 COIN to the journalist to publish a false story pinning a crime on a rival faction NPC by name. That NPC gets temporarily framed: police pursue them for 3 in-game minutes, Respect with the framed faction drops by 10 toward the player. Achievement: **TABLOID KINGPIN**.

3. **Buy Out**: Pay 40 COIN to suppress a story. A damaging front page (that would trigger police Heightened Alert) is replaced with the pigeon filler story. Only works once per edition.

### Physical Newspaper Item

- `Material.NEWSPAPER` (already exists in the item system) gains a new read action.
- Reading triggers a full-screen overlay UI in the style of a British tabloid: black-and-white pixel font, bold headline, 2–3 lines of body text, classified ads column.
- The newspaper in the player's inventory shows the edition date (in-game date) as its tooltip.
- Old newspapers (more than 2 in-game days old) still readable but NPCs don't react to them.
- Collecting 7 consecutive daily editions triggers the **REGULAR READER** achievement.

### New Source File

`src/main/java/ragamuffin/core/NewspaperSystem.java`

- `NewspaperSystem.update(float delta, TimeSystem, ...)` — advances the publication timer
- `NewspaperSystem.publishEdition(List<InfamyEvent> events, ...)` — generates the day's paper
- `NewspaperSystem.onNpcReadsNewspaper(NPC, Newspaper, ...)` — applies NPC reaction
- `NewspaperSystem.tipOffJournalist(InfamyEvent, Inventory, ...)` — press manipulation
- `Newspaper` — value object: headline (String), briefs (List<String>), classifieds (List<String>), infamyScore (int), editionDate (int)
- `InfamyEvent` — captures: action type, location, items involved, NPCs named, wanted stars at time, escape method

### Integration with Existing Systems

- **NotorietySystem**: Infamy score ≥ 7 adds +3 Notoriety on publication. Helps accelerate Notoriety progression organically through play.
- **WantedSystem**: Police Heightened Alert buff triggered by 7+ infamy stories modifies `POLICE_BASE_LOS_RANGE` and patrol density for 5 minutes.
- **RumourNetwork**: Each edition's front page is seeded as a rumour from the Barman NPC, with `RumourType.LOOT_TIP` for heist/loot stories and `GANG_ACTIVITY` for faction/turf stories.
- **FactionSystem**: Stories about faction crimes shift Respect ±5 for the named faction and the player. A story about the player helping MARCHETTI_CREW gives +5 Respect with them, −5 with THE_COUNCIL.
- **FenceSystem**: Items named in front-page heist stories sell for +10% at fence NPCs for 1 in-game day.
- **StreetEconomySystem**: `MarketEvent.GREGGS_STRIKE` is now also triggered when Greggs features in a front-page heist story (the raid caused a public health panic).
- **CriminalRecord**: Each front-page appearance is logged in the CriminalRecord as `CrimeType.PRESS_INFAMY`.
- **AchievementSystem**: New achievements — `TABLOID_KINGPIN` (plant a lie), `REGULAR_READER` (collect 7 editions), `FRONT_PAGE_VILLAIN` (reach infamy score 10), `NO_COMMENT` (suppress 3 stories via buyout), `PIGEON_MENACE` (go 5 in-game days without making the paper).

### New Key Bindings

- **R**: Read selected item (newspaper — opens tabloid overlay UI)
- **J**: Interact with journalist NPC (context-sensitive, active only in pub during evening hours)

**Unit tests**: Infamy score calculation per action type, headline template filling with correct details, NPC reaction state changes, police heightened alert duration, fence price bonus application, rumour seeding from barman, suppression cost deduction, tip-off payment flow.

**Integration tests — implement these exact scenarios:**

1. **Front page generated after 5-star chase**: Set player Wanted Level to 5. Run a pursuit (LOS maintained for 10 seconds). Reduce wanted to 0 via escape. Advance game time to next 18:00 publication. Verify a `Newspaper` object is created with infamy score ≥ 8. Verify the headline contains "WANTED" or "FUGITIVE". Verify a `Material.NEWSPAPER` item spawns at the newsagent landmark location.

2. **NPC reacts to headline**: After publication of an infamy-8 paper, move a `PUBLIC` NPC within 3 blocks of the spawned newspaper. Simulate NPC picking it up (call `onNpcReadsNewspaper`). Verify the NPC's speech text contains a reference to the story (e.g. "Did you see the paper?"). Verify the Barman NPC has a new `GANG_ACTIVITY` rumour in the rumour network.

3. **Police Heightened Alert from major story**: Publish a newspaper with infamy score 7. Verify `WantedSystem.POLICE_BASE_LOS_RANGE` is increased by 4 blocks. Advance 5 in-game minutes. Verify LOS range returns to the base value.

4. **Fence price bonus for named item**: Publish a front-page story naming `Material.DIAMOND` as the stolen item (from jeweller heist). Approach a Fence NPC. Verify `FenceSystem.getSellPrice(Material.DIAMOND)` returns a value 10% higher than normal. Advance 1 in-game day. Verify the bonus has expired and price returns to normal.

5. **Tip-off journalist publishes story**: Give player 5 COIN. Spawn journalist NPC in pub. Set game time to 20:00. Call `tipOffJournalist` with a previously untracked `InfamyEvent`. Advance to 18:00 next day. Verify the published newspaper's front page matches the tipped event. Verify 5 COIN was deducted from inventory.

6. **Suppress story via buyout**: Create an infamy-7 event. Give player 40 COIN. Call `buyOutStory()`. Advance to 18:00. Verify published newspaper has the pigeon filler headline instead of the crime story. Verify police Heightened Alert is NOT triggered. Verify 40 COIN deducted.

7. **No story = filler**: Advance a full in-game day with no player crimes. Verify the published newspaper headline contains "PIGEON" or a council planning notice. Verify infamy score is 0. Verify no police Heightened Alert is triggered.

8. **REGULAR_READER achievement**: Collect `Material.NEWSPAPER` items on 7 consecutive in-game days (simulate via `pickUpNewspaper()` calls with incrementing edition dates). Verify `AchievementType.REGULAR_READER` is awarded on the 7th collection.

---

## Phase 20: Graffiti & Territorial Marking — 'Your Name on Every Wall'

**Goal**: Give the player a spray can and let them physically paint the world. Graffiti tags are persistent voxel-surface overlays rendered as coloured decals on block faces. They serve as a living map of the turf war — factions spray their own tags, the player claims territory, police scrub it, and the whole urban canvas becomes a dynamic record of who runs what street.

### Core Concept

The player crafts a `Material.SPRAY_CAN` (empty tin + paint pigment from hardware store). Equip it to the hotbar, aim at any solid block face within 3 blocks, and press **T** to apply a graffiti tag. Tags are stored as `GraffitiMark` objects — block position + face direction + faction/style + colour + age — and rendered each frame as a flat quad drawn directly over the block face (using LibGDX's `DecalBatch` or a small shader overlay, depth-offset to avoid z-fighting).

Each spray can has **20 uses**. Tags fade over in-game days (alpha decays at `FADE_RATE_PER_DAY`). The Council dispatches a `COUNCIL_CLEANER` NPC with a bucket who walks to high-visibility tags (near the town hall, Greggs, or park) and scrubs them after 2 in-game days.

### Tag Styles & Faction Ownership

The player chooses a tag style when crafting the spray can. Three styles correspond to the three factions — but the player can pick any, and using a rival faction's tag on their turf is an act of war:

| Style | Faction | Visual | COIN cost to craft |
|---|---|---|---|
| `CROWN_TAG` | MARCHETTI_CREW | Gold crown glyph | 5 |
| `LIGHTNING_TAG` | STREET_LADS | White lightning bolt | 3 |
| `CLIPBOARD_TAG` | THE_COUNCIL | Grey clipboard glyph | 4 |
| `PLAYER_TAG` | Player (neutral) | Customisable initials (up to 3 chars, entered at first craft) | 2 |

Spraying your own `PLAYER_TAG` on a block surface claims that surface as player territory in `TurfMap`. Spraying a faction tag on surfaces already owned by a rival faction causes a `RumourType.GANG_ACTIVITY` rumour and adds +3 to that faction's Respect toward the player (you're doing their dirty work).

### Territorial Mechanics

`GraffitiSystem` tracks a **Tag Density** per 8×8 block zone: the number of living (non-faded) tags belonging to each faction in that zone. The faction with the majority of tags in a zone gains a passive **Turf Pressure** bonus applied to `FactionSystem`'s turf transfer logic — tag-heavy zones shift turf ownership faster. This means a player who systematically tags an area accelerates their own territorial dominance without needing direct combat.

A zone where the player holds ≥ 5 `PLAYER_TAG` marks becomes a **Claimed Zone**. Claimed zones:
- Earn the player `RACKET_PASSIVE_INCOME_COIN / 2` per in-game minute (graffiti protection fee — "This is my manor").
- Make nearby hostile NPCs from rival factions **hesitate** for 1 second before attacking (home turf intimidation).
- Display a subtle coloured tint on the minimap (if one is added later).

### NPC Graffiti Crews

Faction NPCs don't just stand around — they actively spray. Every 5 in-game minutes, `GraffitiSystem.update()` picks up to 2 NPCs per faction and dispatches them to walk to a random unclaimed or rival-tagged block face within their faction's zone, then spray their faction tag. This makes the turf war feel alive: leaving an area unattended lets rivals reclaim it visually and territorially.

`STREET_LADS` spray fast (1 second per tag) but their tags fade twice as quickly. `MARCHETTI_CREW` spray slowly (3 seconds, they're deliberate) but their tags last twice as long. `THE_COUNCIL` don't spray; instead they dispatch a `COUNCIL_CLEANER` NPC who removes all non-Council graffiti in civic areas.

### Spray Can Crafting & Pigments

New `Material` entries:
- `SPRAY_CAN_EMPTY` — drops from breaking shelving props in the industrial estate
- `PAINT_PIGMENT_RED`, `PAINT_PIGMENT_BLUE`, `PAINT_PIGMENT_GOLD`, `PAINT_PIGMENT_WHITE`, `PAINT_PIGMENT_GREY` — drop from breaking art-supply/hardware props
- `SPRAY_CAN` — crafted: 1 `SPRAY_CAN_EMPTY` + 1 `PAINT_PIGMENT_*` → determines tag colour

Colour affects only visuals, not gameplay (except `PAINT_PIGMENT_GOLD` exclusively makes `CROWN_TAG` valid for Marchetti missions).

### Wanted System Integration

Spraying graffiti is a crime. Each tag placed outdoors (not in the player's own squat) adds `NOISE_GRAFFITI = 0.1f` to the noise system. A PCSO or POLICE NPC with LOS to the player during spraying immediately raises Wanted Level by 1. If the player is caught mid-spray, the can is confiscated (removed from inventory).

Being caught spraying 3 times total adds `CrimeType.CRIMINAL_DAMAGE` to the criminal record, which unlocks a `BuildingQuestRegistry` quest from the solicitor NPC: "Secure a not-guilty plea — gather 3 alibi witnesses."

### Rendering

Each `GraffitiMark` renders as a 1×1 quad on the tagged block face, textured from a 16×16 graffiti glyph atlas (`graffiti_atlas.png`). The quad is drawn with a slight Z-offset (polygon offset in OpenGL) to avoid z-fighting. Alpha channel fades linearly from 1.0 to 0.0 over `FADE_DAYS` in-game days. Tags applied in covered/sheltered areas (indoors, under overhangs) fade at 20% of the outdoor rate — permanent markers if the player stays out of trouble.

A screen-space particle burst (3–5 paint-spray particles, colour-matched to the can) plays for 0.3 seconds when a tag is placed, providing satisfying feedback.

### New Source File

`src/main/java/ragamuffin/core/GraffitiSystem.java`

- `GraffitiSystem.update(float delta, List<NPC>, TurfMap, WantedSystem, NoiseSystem)` — advances fade timers, dispatches NPC graffiti crews, applies turf pressure
- `GraffitiSystem.placeTag(Vector3 blockPos, BlockFace face, TagStyle style, Faction owner)` — validates range/LOS, deducts spray can use, creates `GraffitiMark`, fires noise event
- `GraffitiSystem.scrubTag(GraffitiMark)` — called by Council Cleaner NPCs; removes mark from world and `TurfMap`
- `GraffitiSystem.getTagDensity(int zoneX, int zoneZ, Faction)` — returns living tag count for turf pressure
- `GraffitiSystem.getClaimedZones(Faction)` — returns list of zones with majority tags for that faction
- `GraffitiMark` — value object: `blockPos`, `face`, `style`, `ownerFaction`, `colour`, `ageInGameDays` (float), `isScrubbed`
- `TagStyle` — enum: `CROWN_TAG`, `LIGHTNING_TAG`, `CLIPBOARD_TAG`, `PLAYER_TAG`

`src/main/java/ragamuffin/render/GraffitiRenderer.java`

- `GraffitiRenderer.render(List<GraffitiMark>, Camera)` — draws all living marks as depth-offset quads from the graffiti glyph atlas, with alpha fade applied

### Integration with Existing Systems

- **TurfMap**: `placeTag` calls `TurfMap.setOwner(blockPos, faction)` for the tagged block surface. `scrubTag` calls `TurfMap.clearOwner(blockPos)`.
- **FactionSystem**: Spraying a rival's tag on their turf costs −5 Respect with that faction. Spraying your ally's tag on rival turf gives +3 Respect. Completing 10 tags in a zone the player-faction wins hands the COUNCIL a "graffiti menace" rumour.
- **WantedSystem / NoiseSystem**: Each outdoor tag adds noise 0.1. Being seen by police during spray → +1 Wanted immediately.
- **CriminalRecord**: 3+ graffiti arrests → `CrimeType.CRIMINAL_DAMAGE` logged. Triggers solicitor quest.
- **RumourNetwork**: When a zone flips faction-majority (tag density swings), a `RumourType.GANG_ACTIVITY` rumour is seeded at nearby NPCs ("Someone's been tagging the estate — Marchetti boys are spitting.").
- **NewspaperSystem**: If the player tags 10+ surfaces in one in-game day, the next edition may feature a Local Brief: "GRAFFITI VANDAL STRIKES AGAIN — Council vows crackdown." Infamy score contribution: 2.
- **AchievementSystem**: New achievements — `WRITER` (place first tag), `GETTING_UP` (place 50 tags), `ALL CITY` (have living tags in every zone simultaneously), `SCRUBBED` (have 10 of your tags removed by Council Cleaners — you're famous enough to be a nuisance), `CLEAN HANDS` (complete a full in-game day without placing any tags, while holding a spray can).
- **StreetEconomySystem**: Spray can components (`SPRAY_CAN_EMPTY`, `PAINT_PIGMENT_*`) become tradeable commodities. `STREET_LADS` NPCs with high BORED need will pay 2 COIN for a filled spray can.

### New Key Binding

- **T**: Place graffiti tag on targeted block face (only active when spray can equipped in hotbar)

**Unit tests**: Tag placement range validation (>3 blocks rejected), fade timer progression, tag density calculation per zone, turf pressure application to FactionSystem, noise event fired on outdoor tag, wanted level increment when seen, spray can use count decrement, NPC crew dispatch logic, Council Cleaner target selection.

**Integration tests — implement these exact scenarios:**

1. **Tag placed on block face**: Give player a `SPRAY_CAN` with `PLAYER_TAG`. Place player 2 blocks from a BRICK wall, facing it. Press **T**. Verify a `GraffitiMark` exists at that block face with `ownerFaction = PLAYER`. Verify spray can use count decremented by 1. Verify `TurfMap.getOwner(blockPos)` returns PLAYER faction.

2. **Tag ownership shifts turf pressure**: Place 5 `PLAYER_TAG` marks in an 8×8 zone currently owned by `STREET_LADS`. Call `GraffitiSystem.getTagDensity(zoneX, zoneZ, PLAYER)`. Verify it returns 5. Call `FactionSystem.update()`. Verify that PLAYER's turf pressure in that zone causes the turf-transfer threshold to be reached sooner (turf transfer fires at a gap of 25 instead of 30).

3. **Council Cleaner scrubs civic-area tag**: Place a `PLAYER_TAG` on a block adjacent to the town hall (landmark `THE_COUNCIL`). Advance 2 in-game days. Verify a `COUNCIL_CLEANER` NPC was dispatched and called `scrubTag`. Verify the `GraffitiMark.isScrubbed()` is true. Verify `TurfMap.getOwner(blockPos)` no longer returns PLAYER faction.

4. **Caught spraying raises wanted level**: Place player outdoors. Spawn a `POLICE` NPC with LOS to the player. Give player a `SPRAY_CAN`. Press **T** to place a tag. Verify `WantedSystem.getWantedLevel()` incremented by 1. Verify the spray can is removed from inventory (confiscated).

5. **NPC crew tags rival zone**: Advance game time by 5 in-game minutes. Verify that at least 1 `STREET_LADS` NPC has placed a `LIGHTNING_TAG` `GraffitiMark` on a block surface within the MARCHETTI_CREW zone. Verify a `RumourType.GANG_ACTIVITY` rumour was seeded if this caused a zone flip.

6. **Claimed zone passive income**: Player places 5 `PLAYER_TAG` marks in one 8×8 zone. Verify `GraffitiSystem.getClaimedZones(PLAYER)` includes that zone. Advance 1 in-game minute. Verify player COIN increased by `RACKET_PASSIVE_INCOME_COIN / 2`.

7. **ALL CITY achievement**: Place at least 1 living `PLAYER_TAG` in every 8×8 zone in the world. Verify `AchievementType.ALL_CITY` is awarded.

8. **Spray can exhausted**: Give player a `SPRAY_CAN` with exactly 1 use remaining. Place a tag. Verify `SPRAY_CAN` is removed from inventory (replaced with `SPRAY_CAN_EMPTY`). Verify `SPRAY_CAN_EMPTY` can be re-crafted with a pigment into a fresh `SPRAY_CAN`.

---

## Phase 8h: Pirate FM — Underground Radio Station & Neighbourhood Propaganda Machine

**Goal**: Let the player build and run an illegal pirate radio station from their squat. Broadcasting
lets the player shape the narrative of the neighbourhood — boosting faction respect, spiking NPC
moods, spreading disinformation, attracting listeners who bring loot, and making the Council
increasingly furious. The mechanic is a direct counterpart to the newspaper system (which reports on
you passively) — now the player gets to broadcast back. Very 90s. Very illegal. Very British.

### The Transmitter — Crafting & Placement

A new craftable block: **`TRANSMITTER`** (crafted from 2 WIRE + 1 COMPUTER + 1 WOOD in the crafting
menu). The transmitter must be placed indoors (at least 3 solid blocks above it) to avoid instant
police detection. Placing it outdoors triggers a `NoiseSystem` event at noise level 0.8 immediately.

A second craftable item: **`MICROPHONE`** (crafted from 1 WIRE + 1 COIN), held in the hotbar.
The player must be within 2 blocks of a placed TRANSMITTER and holding the MICROPHONE to broadcast.

Signal range scales with the transmitter's **broadcast power** (upgradeable):

| Power Level | Range (blocks) | How to upgrade |
|-------------|---------------|----------------|
| 1 (starter) | 30 | Default on craft |
| 2 | 60 | Add 1 COMPUTER to transmitter (right-click) |
| 3 | 100 | Add 1 STOLEN_PHONE + 1 COMPUTER (right-click) |
| 4 (max) | 160 (whole map) | Add 2 COMPUTER + 1 PETROL_CAN (right-click) |

A visual indicator — a faint animated `PointLight` (orange-red, pulsing at 1 Hz) — appears above
the TRANSMITTER block while it is active. Range is shown as a tooltip when the player right-clicks
the transmitter.

New `Material` entries: `WIRE`, `MICROPHONE`, `BROADCAST_TAPE` (used for pre-recorded shows, see below).
New `BlockType` entry: `TRANSMITTER`.

### Broadcasting — The Broadcast Session

Press **B** while holding the MICROPHONE within range of a powered TRANSMITTER to start/stop a
broadcast session. The HUD shows a pulsing "ON AIR" indicator (red text, 2 Hz blink) while live.

Every 10 in-game seconds of active broadcasting, the player chooses a **broadcast action** (via a
small UI overlay, similar to the crafting menu — press 1-4 to select):

| Key | Action | Effect |
|-----|--------|--------|
| 1 | **Big Up the Area** | All NPCs within range: BORED need −20. All three factions: Respect +3. Tooltip: "Nothing unites a community like a good tune." |
| 2 | **Slag Off [Faction]** (cycles per use) | Targeted faction Respect −10 with player; rival factions Respect +5. Seeds `GANG_ACTIVITY` rumour: "[Faction] just got aired out live on the radio." NPC belonging to targeted faction enters FLEEING if within range. |
| 3 | **Black Market Shout-Out** | Spawns 1–3 `NPC_TYPE.LISTENER` NPCs walking toward the transmitter location. Each LISTENER carries 2–6 COIN and a random item from the black market loot table. Police detection chance +5% per shout-out (cumulative, resets at broadcast end). |
| 4 | **Council Diss Track** | Council Respect −15. Doubles the speed of Council Cleaners currently active. Triggers a `NewspaperSystem` potential headline: "PIRATE RADIO MENACE TARGETS COUNCIL — Authorities seek shutdown." Notoriety +10. |

Broadcasting for more than 3 consecutive in-game minutes (without stopping) triggers the **Signal
Triangulation** mechanic (see below). Tooltip on first broadcast: "You're live on the airwaves.
Don't say anything they can trace back to you."

### Listener NPCs

`NPCType.LISTENER` is a new NPC type: civilian NPCs drawn to the transmitter's signal. They:
- Walk toward the transmitter's last known position (pathfinding, ignoring faction hostility).
- On arrival (within 4 blocks of transmitter), enter `NPCState.IDLE` and drop a random item from
  the listener loot table onto the ground (as a `SmallItem`).
- If the player talks to them (press **E**), they share a random rumour from `RumourNetwork` —
  the radio attracts people who know things.
- After 60 in-game seconds, despawn.

**Listener loot table** (uniform random pick):
`CAN_OF_LAGER`, `CIGARETTE`, `COIN` (×3), `TOBACCO_POUCH`, `SCRATCH_CARD`, `STOLEN_PHONE`,
`NEWSPAPER`, `WOOLLY_HAT_ECONOMY`, `PRESCRIPTION_MEDS`.

Maximum 6 LISTENER NPCs active at any time (to avoid world-spawning chaos).

### Signal Triangulation — The Council Hunts You

Each broadcast second accumulates a hidden `triangulationProgress` counter (0–100). It increases
faster at higher broadcast power levels:

| Power Level | Progress per second |
|-------------|-------------------|
| 1 | 0.3 |
| 2 | 0.5 |
| 3 | 0.8 |
| 4 | 1.5 |

When `triangulationProgress` reaches 100, a **`SIGNAL_VAN` NPC** (a `COUNCIL_BUILDER` variant in a
white van — use the existing `Car` system, white colour, `NPCType.COUNCIL_BUILDER` driving) spawns
at the nearest road block to the world edge and drives toward the transmitter block. If the Signal
Van reaches within 6 blocks of the TRANSMITTER, it **confiscates it** (block removed from world,
added to vehicle inventory — gone forever). The player receives a tooltip: "They found your station.
You've got maybe ten seconds." (displayed when triangulationProgress hits 80).

Stopping the broadcast resets `triangulationProgress` to 0. Destroying the Signal Van (reduce its
HP to 0 using the existing car damage system) also resets triangulation and grants +20 Notoriety.
Tooltip on first van appearance: "Council's got a scanner van. Of course they do."

### Pre-Recorded Shows — `BROADCAST_TAPE`

The `BROADCAST_TAPE` material (crafted from 1 NEWSPAPER + 1 COIN) lets the player "record" a
broadcast action at the transmitter (right-click transmitter with tape in hotbar, then press 1–4).
The tape stores the chosen action. Placing the tape back in the transmitter and walking away causes
the transmitter to auto-broadcast that action once every 30 in-game seconds without the player
present — but at half effectiveness and with `triangulationProgress` accumulating at double speed
(unattended rigs are sloppy). Maximum 1 tape loaded per transmitter.

### New Crafting Recipes (add to CraftingSystem)

| Output | Ingredients | Description |
|--------|------------|-------------|
| `WIRE` | 1 COIN + 1 WOOD | Stripped wire (bodged) |
| `MICROPHONE` | 1 WIRE + 1 COIN | Mic made from a fork and tape |
| `TRANSMITTER` (block) | 2 WIRE + 1 COMPUTER + 1 WOOD | The heart of Pirate FM |
| `BROADCAST_TAPE` | 1 NEWSPAPER + 1 COIN | Record your message |

### System Integrations

- **`RumourNetwork`**: Each broadcast action (1-4) seeds a `RumourType.GANG_ACTIVITY` or
  `RumourType.LOOT_TIP` rumour (as appropriate) into all NPCs within broadcast range.
- **`FactionSystem`**: Actions 1, 2, and 4 call the relevant `applyRespectDelta()` methods.
- **`NewspaperSystem`**: Council Diss Track (action 4) feeds into the newspaper infamy system
  (new `InfamyContribution.PIRATE_RADIO`, weight 3). A sufficiently famous pirate broadcaster
  gets a front-page headline: "RAGAMUFFIN FM: ENEMY OF THE STATE?"
- **`NotorietySystem`**: Each broadcast session (start) adds +5 Notoriety. Action 4 adds +10.
  Signal Van destruction adds +20.
- **`WantedSystem`**: If a POLICE NPC is within 10 blocks of the transmitter while it is active,
  Wanted Level increments by 1 immediately (they can hear the music).
- **`StreetEconomySystem`**: LISTENER NPCs count as "deal participants" — their loot drops satisfy
  the `BROKE` need pathway without a formal deal (automatic). `WIRE` and `MICROPHONE` are added
  to the tradeable commodity list (base prices: WIRE=2, MICROPHONE=4).
- **`AchievementSystem`**: New achievements:

| Achievement | Condition |
|-------------|-----------|
| `ON_AIR` | Complete first broadcast session (any action) |
| `PIRATE_FM` | Broadcast for a cumulative 10 in-game minutes across all sessions |
| `SIGNAL_JAM` | Destroy a Signal Van |
| `THE_PEOPLE_S_DJ` | Have 6 LISTENER NPCs arrive at your transmitter simultaneously |
| `ENEMY_OF_THE_STATE` | Receive the "RAGAMUFFIN FM: ENEMY OF THE STATE?" newspaper headline |
| `OFF_AIR` | Have your transmitter confiscated by the Signal Van |

### HUD Additions

- **ON AIR indicator**: Pulsing red "● ON AIR" text in the top-right corner while broadcasting.
- **Listener count**: Small counter "👥 N" below the ON AIR indicator showing current LISTENER NPCs
  en route or present (rendered as a digit, no emoji in actual code — use PixelFont).
- **Triangulation bar**: A thin horizontal bar below the listener count, fills red as
  `triangulationProgress` approaches 100. Tooltip at 80%: "They found your station."

### New Key Binding

- **B**: Start/stop broadcast (only active when holding MICROPHONE within 2 blocks of TRANSMITTER)

**Unit tests**: Transmitter crafting recipe validation, broadcast power level range calculation,
triangulationProgress accumulation rate per power level, listener NPC spawn cap enforcement,
BROADCAST_TAPE auto-broadcast interval and half-effectiveness, Signal Van spawn trigger at
triangulationProgress=100, broadcast action 1–4 respect and need delta calculations, listener
loot table distribution (uniform), wanted level increment when police nearby, all achievement
trigger conditions.

**Integration tests — implement these exact scenarios:**

1. **Transmitter crafts and places**: Give player 2 WIRE, 1 COMPUTER, 1 WOOD. Open crafting menu.
   Select TRANSMITTER recipe. Verify TRANSMITTER block is in hotbar. Place it indoors (3 solid blocks
   above). Verify the block is present in the world chunk at placed position. Verify an orange pulsing
   PointLight appears in the environment at the transmitter position.

2. **Broadcast session activates and shows HUD**: Give player a MICROPHONE. Place TRANSMITTER. Move
   player within 2 blocks. Press B. Verify game state has `broadcastActive = true`. Verify the HUD
   shows the "ON AIR" indicator. Verify `triangulationProgress` is 0 at start. Advance 10 seconds.
   Verify `triangulationProgress > 0`.

3. **Big Up the Area satisfies BORED need for nearby NPCs**: Start broadcast. Place 3 PUBLIC NPCs
   within 30 blocks, each with `BORED` need = 70. Press 1 (Big Up the Area). Verify each NPC's
   BORED need has decreased to ≤ 50. Verify all three factions gained +3 Respect.

4. **Slag Off faction decreases that faction's respect and seeds rumour**: Set MARCHETTI_CREW
   Respect to 50. Start broadcast. Press 2 (Slag Off, targeting MARCHETTI_CREW). Verify Marchetti
   Respect decreased to 40. Verify at least 1 nearby NPC has a `GANG_ACTIVITY` rumour containing
   "aired out live on the radio". Verify rival factions each gained +5 Respect.

5. **Black Market Shout-Out spawns LISTENER NPCs**: Start broadcast. Press 3 (Black Market
   Shout-Out) 3 times (over 30 in-game seconds). Verify at least 1 `LISTENER` NPC has been spawned
   and is pathfinding toward the transmitter. Verify LISTENER count on HUD is ≥ 1.

6. **Signal Van spawns at triangulationProgress 100**: Set broadcast power to 4. Start broadcast.
   Advance game simulation until `triangulationProgress >= 100`. Verify a `Car` entity with driver
   of type `COUNCIL_BUILDER` has spawned at a road block near the world edge. Verify the car is
   moving toward the transmitter position.

7. **Signal Van confiscates transmitter on arrival**: Spawn a Signal Van at the transmitter's
   location directly. Advance 60 frames. Verify the TRANSMITTER block has been removed from the
   world (replaced with AIR). Verify `broadcastActive` is false. Verify tooltip "They found your
   station." was displayed.

8. **Stopping broadcast resets triangulation**: Set `triangulationProgress` to 80. Press B to stop
   broadcast. Verify `triangulationProgress` is reset to 0. Verify "ON AIR" indicator is gone from
   HUD. Verify no Signal Van spawns within the next 60 frames.

9. **BROADCAST_TAPE auto-broadcasts without player**: Craft a BROADCAST_TAPE. Record action 1
   (Big Up the Area) on the tape. Load tape into transmitter. Move player 10 blocks away (out of
   range). Advance 30 in-game seconds. Verify at least 1 Big Up the Area effect has fired (faction
   Respect increased). Verify `triangulationProgress` is accumulating at double the normal rate for
   the current power level.

10. **Full pirate radio stress test**: Craft transmitter (power 1). Broadcast all 4 actions in
    sequence. Verify all NPC/faction/notoriety effects fire correctly. Upgrade transmitter to power 4.
    Broadcast until Signal Van spawns. Destroy van (hit it until HP = 0). Verify +20 Notoriety added.
    Verify `SIGNAL_JAM` achievement awarded. Verify `triangulationProgress` resets to 0. Verify game
    remains in PLAYING state with no NPEs and all HUD elements valid throughout.

---

## Phase 16: The Dodgy Market Stall — Underground Street Trade Empire

**Goal**: Give the player a fully interactive street trading operation that starts as a one-man
hustle (a knocked-together market stall flogging tat) and can grow into a sprawling black-market
front. The stall is a physical object in the world, attracts NPC customers driven by their existing
`NeedType` scores, generates passive income, and sits in permanent tension with the council, police,
rival factions, and the `StreetEconomySystem`. It is the missing economic end-game: a reason to
accumulate resources, manage territory, and care about faction relationships long after the player
has done the heists and tagged the walls.

The tone is quintessentially British: a fold-up table covered in knock-off perfume, counterfeit
DVDs, and slightly warm energy drinks, run by someone who absolutely does not have a licence.

### The Stall Structure

The stall is a player-built prop assembled from crafted components:

| Component | Recipe | Notes |
|-----------|--------|-------|
| `STALL_FRAME` | 4 WOOD → 1 STALL_FRAME | The fold-up table chassis |
| `STALL_AWNING` | 2 WOOD + 2 CARDBOARD → 1 STALL_AWNING | Canopy; provides shelter rating; required to operate in rain |
| `MARKET_LICENCE` | 20 COIN (buy from Council NPC via **E**) | Legitimate licence; reduces council aggro. Optional — operating without one is riskier and cheaper |

The player places `STALL_FRAME` as a block (new `BlockType.STALL`). It occupies a 1×1 footprint
and can only be placed on PAVEMENT or ROAD blocks. Once placed, pressing **E** opens the **Stall
Management UI** (new screen, similar to crafting UI in style).

### Stall Management UI

Opened with **E** while facing a placed `STALL_FRAME` (within 2 blocks). Shows:

- **Stock slots** (6 items): items the stall currently sells. Player drags from inventory to add
  stock. Only sellable `Material` types are accepted (those with entries in
  `StreetEconomySystem.BASE_PRICES` plus new `KNOCK_OFF_PERFUME`, `DODGY_DVD`, `CIGARETTE`,
  `ENERGY_DRINK`, `CAN_OF_LAGER`, `SAUSAGE_ROLL`).
- **Asking price per item**: player sets a per-unit COIN price (default = base market price).
- **Licence slot**: optionally load a `MARKET_LICENCE` here to operate legally (reduces council
  attention but costs 20 COIN upfront).
- **Running total**: coins earned since last visit (passive income accumulated while player is
  away).
- **Status indicator**: OPEN / CLOSED (player toggles with **E** → "Open for Business" /
  "Shut Up Shop"). Stall only sells when OPEN.

### NPC Customer Behaviour

While the stall is OPEN, it functions as a passive `StreetEconomySystem` node — NPCs with
elevated `NeedType` scores are attracted to it automatically:

- Every 30 in-game seconds, `StallSystem` (new class) scans NPCs within **25 blocks**. For each
  NPC whose highest need (`StreetEconomySystem.getHighestNeed()`) is satisfied by any stall stock
  item, the NPC enters a new `NPCState.WALKING_TO_STALL` state and pathfinds to the stall.
- On arrival (within 2 blocks), the NPC "buys" one unit: item removed from stock, COIN added to
  stall's running total, NPC's need zeroed for that type. NPC emits speech: *"Cheers mate, what
  are you like."* / *"Lovely jubbly."* / *"Do you have a receipt for this?"*
- Max 3 NPCs queued at stall simultaneously (prevents a permanent NPC pile-up). If queue full,
  new potential customers wander off.
- NPCs of type POLICE or PCSO who come within 8 blocks while the stall is OPEN check for a
  `MARKET_LICENCE`. If none: 50% chance they issue a **Stall Fine** (confiscate all stock, add
  1 offence to `CriminalRecord`, stall closes automatically). If licence present: police ignore
  the stall entirely.

### Faction Interactions

The stall integrates deeply with the existing `FactionSystem`:

| Situation | Faction effect |
|-----------|---------------|
| Stall placed in Marchetti territory (turf block owned by MARCHETTI_CREW) | Marchetti lieutenant NPC visits within 60s demanding "a percentage" — 20% of stall income goes to Marchetti automatically until player pays 15 coins to "sort it out" |
| Stall placed in Street Lads territory | Street Lads add the stall to their protection (free), but rival factions will periodically "trash" the stall (remove 2 random stock items) unless player has Street Lads Respect ≥ 60 |
| Stall placed in Council territory | Council sends a planning notice (same `PropType.PLANNING_NOTICE` as for player buildings) within 120 seconds of placement. If not moved within 5 in-game minutes, COUNCIL_BUILDER NPCs arrive and destroy the stall block |
| All factions Respect ≥ 75 | All factions protect the stall — no trashing, no cut, council ignores it. Stall income multiplied ×1.5 |

### Passive Income & Economy Balance

Stall income accrues while the player is **away** (more than 25 blocks from the stall), simulating
a running business. The income model:

- Base rate: 1 COIN per NPC customer × item sale price.
- Maximum stock per slot: 10 units. Stall closes automatically when all stock is exhausted.
- Restock by returning to the stall, opening Management UI, and dragging items from inventory.
- Stall income is separate from player inventory — coins sit in the stall until the player
  returns to collect (press **E** → "Collect Takings").
- Maximum coins holdable in stall at once: 50 (prevents exploit of infinite passive income;
  stall closes when full).

Weather effects on the stall:
- RAIN without `STALL_AWNING`: stall closes automatically ("Your stock's getting soaked."),
  loses 1 random item per 30 seconds.
- FROST: NPC customer rate halved ("Nobody's coming out in this.").
- HEATWAVE: NPC customer rate doubled, COLD need replaced by demand for ENERGY_DRINK and
  CAN_OF_LAGER.

### New Materials

Add to `Material` enum:

| Material | Source | Notes |
|----------|--------|-------|
| `KNOCK_OFF_PERFUME` | Charity shop loot (3 hits), or crafted (1 COIN + 1 PLASTIC) | Satisfies BORED need. Tooltips: *"Smells like ambition and regret."* |
| `DODGY_DVD` | Industrial estate loot | Satisfies BORED need. Police treat as evidence if found in inventory. |
| `STALL_FRAME` | Crafted (4 WOOD) | Placeable stall block |
| `STALL_AWNING` | Crafted (2 WOOD + 2 CARDBOARD) | Equip to stall for weather protection |
| `MARKET_LICENCE` | Buy from Council NPC for 20 COIN | Reduces police aggro at stall |

### New NPC Type

`NPCType.MARKET_INSPECTOR` — spawns from council territory when stall has been operating without
a licence for more than 3 in-game minutes. Slow-moving, clipboard-carrying bureaucrat. On
reaching the stall, issues a `STALL_FINE` (confiscates stock, adds offence). Can be bribed
(10 COIN via **E** → "Have a think about it") to leave without fining. Bribing a Market Inspector
adds 5 Notoriety. Speech: *"I'm going to need to see your trading permit."* /
*"This pavement is not zoned for commercial activity."*

### Stall Upgrade Path

The stall has three upgrade tiers, unlocked by total lifetime sales:

| Tier | Sales milestone | Upgrade | Effect |
|------|----------------|---------|--------|
| 1 | 0 (starting) | Fold-up table | 6 stock slots, attracts 1 NPC queue |
| 2 | 50 coins earned | Second-hand trolley | +2 stock slots, attracts 2 NPC queue, 15% customer rate increase |
| 3 | 200 coins earned | Proper stall (with sign) | +4 stock slots, 3 NPC queue, BuildingSign appears above stall reading "BARGAINS HERE", passive income tick even when player nearby, attracts `NPCType.TOURIST` (new type — drops more coins) |

Upgrade is automatic — when the sales milestone is crossed, the stall's appearance upgrades
(block texture variant) and the HUD briefly shows *"Stall upgraded. You're practically legitimate."*

### New Achievements

| Achievement | Trigger |
|-------------|---------|
| `MARKET_TRADER` | First successful NPC customer sale at stall |
| `LICENSED_TO_SELL` | Operate stall with valid MARKET_LICENCE for 5 in-game minutes |
| `BRIBED_THE_INSPECTOR` | Bribe a Market Inspector |
| `SHUTIT_DOWN` | Have stall confiscated by police/council |
| `EMPIRE_BUILDER` | Reach Tier 3 stall upgrade |
| `TURF_VENDOR` | Operate stall in territory of all three factions (across multiple placements) |

### Key Bindings

- **E** (facing stall): open Stall Management UI / collect takings
- **E** (facing COUNCIL_MEMBER NPC): buy `MARKET_LICENCE` for 20 COIN

**Unit tests**: Stall placement validation (pavement/road only), NPC customer scan range and
queue cap enforcement, faction territory detection at placement, passive income accumulation
formula, weather stock-damage logic, Marchetti cut calculation, police licence check probability,
Market Inspector spawn timer, bribe interaction, upgrade tier milestone detection, all achievement
trigger conditions.

**Integration tests — implement these exact scenarios:**

1. **Stall places on pavement, rejected on grass**: Give player a STALL_FRAME in hotbar. Move to
   a PAVEMENT block. Right-click to place. Verify `BlockType.STALL` appears in world at that
   position. Move to a GRASS block. Right-click to place. Verify placement is rejected (world
   block at that position remains GRASS, not STALL). Verify tooltip "You need a hard surface for
   this." is shown.

2. **NPC customer attracted by need and buys item**: Set up stall with 3 SAUSAGE_ROLL in stock,
   asking price 2. Spawn a PUBLIC NPC 15 blocks away. Set their HUNGRY need to 80. Advance 120
   frames (2 seconds, triggering at least one customer scan). Verify the NPC's state is
   `WALKING_TO_STALL`. Advance 600 more frames. Verify the NPC reached the stall, SAUSAGE_ROLL
   stock decreased to 2, stall running total increased by 2 coins, NPC's HUNGRY need is ≤ 0.

3. **Police fine stall without licence**: Open stall without MARKET_LICENCE loaded. Spawn a
   POLICE NPC 6 blocks away. Advance 120 frames. Verify there is a ≥ 50% probability the stall
   was fined over 10 runs (statistical check: run scenario 10 times, at least 5 should result in
   fine). On fine: verify all stock slots empty, CriminalRecord offence count increased by 1,
   stall status is CLOSED.

4. **Licence prevents police fine**: Load MARKET_LICENCE into stall licence slot. Open stall.
   Spawn POLICE NPC 4 blocks away. Advance 300 frames. Verify CriminalRecord offence count is
   unchanged. Verify stall status is still OPEN. Verify stock is intact.

5. **Marchetti cut applies in their territory**: Set stall position inside a Marchetti-owned
   turf block. Open stall with stock. Advance 60 in-game seconds. Verify a Marchetti lieutenant
   NPC has visited. Verify the stall's income is reduced by 20% (a sale of 10 COIN results in
   only 8 COIN in the running total). Verify paying 15 COIN to the lieutenant removes the cut.

6. **Rain destroys stock without awning**: Place stall with 5 stock items and no STALL_AWNING.
   Set weather to RAIN. Advance 1800 frames (30 seconds at 60fps). Verify stall status is
   CLOSED. Verify at least 1 stock item has been removed. Verify tooltip
   "Your stock's getting soaked." was shown.

7. **Awning protects from rain**: Equip STALL_AWNING to stall. Set weather to RAIN. Advance
   1800 frames. Verify stall remains OPEN. Verify stock count is unchanged.

8. **Market Inspector spawns and can be bribed**: Open stall without licence. Advance
   180 in-game seconds (3 minutes). Verify a `MARKET_INSPECTOR` NPC has spawned. Give player 10
   COIN. Press **E** on inspector while within 2 blocks. Verify inspector state transitions to
   LEAVING (walks away). Verify player coin count decreased by 10. Verify CriminalRecord offence
   count did NOT increase. Verify Notoriety increased by 5.

9. **Stall upgrade tier 2 at 50 coins**: Set stall lifetime sales to 49 COIN. Trigger one more
   NPC sale of 2 COIN (total = 51). Verify stall upgrades to Tier 2. Verify stock slot count
   increases to 8. Verify HUD briefly shows "Stall upgraded. You're practically legitimate."
   Verify `EMPIRE_BUILDER` achievement is NOT yet awarded (that requires Tier 3).

10. **Full market empire stress test**: Craft STALL_FRAME, STALL_AWNING, and buy MARKET_LICENCE.
    Place stall in park pavement area. Stock with 5 each of SAUSAGE_ROLL, CAN_OF_LAGER,
    ENERGY_DRINK (all 15 units). Set 6 PUBLIC NPCs within 25 blocks with high HUNGRY/BORED/COLD
    needs. Open stall. Advance 10 in-game minutes. Verify at least 3 NPC customer sales occurred.
    Verify stall running total > 0. Collect takings. Verify player inventory COIN increased.
    Force weather to RAIN — verify awning prevents closure. Force police NPC 5 blocks away —
    verify licence prevents fine. Advance until inspector spawns; bribe him. Verify game remains
    in PLAYING state, no NPEs, all HUD elements valid, stall Management UI opens and closes
    without errors throughout.

---

## Phase R: Street Skills & Character Progression — 'Learning the Hard Way'

**Goal**: Give the player a persistent skill system that rewards specialisation and
makes every playthrough feel different. Actions you do a lot, you get better at.
No menus to grind — just play your way and watch the perks unlock.

### Overview

A new `StreetSkillSystem` tracks six **skills**, each with an XP counter (0–1000)
and five **tiers** (Novice → Apprentice → Journeyman → Expert → Legend). XP is
awarded automatically by existing systems whenever the player performs the
associated action. Each tier unlocks a concrete passive or active **perk** that
feeds back into gameplay, making the skill feel meaningful immediately.

Skills are saved per-world (serialised alongside player position in the game
state). The `SkillsUI` (new overlay, bound to **K** key) shows all six skills as
vertical progress bars with current tier label, XP fraction, and the next locked
perk grayed out below it. Tooltip on first open: "You're not born knowing this
stuff."

### The Six Skills

| ID | Skill Name | XP Sources | Perk progression |
|----|-----------|------------|-----------------|
| `BRAWLING` | Bare-knuckle fighting | +5 XP per hit landed on NPC; +20 XP per NPC knocked out | **Apprentice**: hits deal +1 bonus damage · **Journeyman**: punching a block costs −1 energy · **Expert**: 20% chance to disarm NPC (drops their held item) · **Legend**: crowd-stagger — each punch knocks back all NPCs within 2 blocks |
| `GRAFTING` | Manual labour / breaking things | +3 XP per block broken; +10 XP per crafted item | **Apprentice**: blocks require 1 fewer hit to break (min 1) · **Journeyman**: 10% chance to get double drops from a broken block · **Expert**: unlock `SKELETON_KEY` recipe (crafted from 3 WIRE + 1 BRICK, opens any door once) · **Legend**: break any block in a single punch (energy cost: 10) |
| `TRADING` | Buying, selling, fencing | +5 XP per item sold at stall; +8 XP per fence sale; +3 XP per NPC purchase | **Apprentice**: fence pays 10% more for all items · **Journeyman**: NPC customer attraction range at stall +5 blocks · **Expert**: unlock `HAGGLE` dialogue option on all trade menus (one re-roll of offered price per conversation) · **Legend**: all faction protection cuts halved; Marchetti's 20% cut becomes 10% |
| `STEALTH` | Not being seen doing bad things | +10 XP per successful crime unwitnessed; +5 XP per successful disguise use | **Apprentice**: detection radius of police NPCs reduced by 2 blocks · **Journeyman**: crouching (new `SHIFT` key toggle) reduces footstep noise radius by 50% · **Expert**: can pick pocket NPCs (press **E** from behind a non-hostile NPC — steals 1 random item from their inventory, 60% success) · **Legend**: player is never added to wanted list for block-breaking inside buildings at night |
| `INFLUENCE` | Social manipulation & reputation | +5 XP per rumour extracted from NPC; +10 XP per quest completed; +15 XP per faction mission success | **Apprentice**: NPCs share rumours without needing player to buy a drink · **Journeyman**: `STREET_REP` passive aura — PUBLIC NPCs within 8 blocks have a 20% chance to volunteer rumours without being spoken to · **Expert**: unlock `RALLY` action (press **G** near 3+ PUBLIC NPCs — they follow the player for 60 seconds as a mob, deterring police and gang members) · **Legend**: once per day, trigger a `NEIGHBOURHOOD_EVENT` (flash rave, market flash-crowd, or mass brawl) at any location by pressing G there |
| `SURVIVAL` | Staying alive and warm | +5 XP per in-game day survived; +10 XP each time player regen from critical health; +3 XP per food/drink item consumed | **Apprentice**: health regenerates 1 point/min passively (no shelter required) · **Journeyman**: hunger drains 25% slower · **Expert**: player can eat `RAW_MATERIALS` as improvised food (LEAVES → bitter greens, +5 hunger; WOOD → splinters, +1 hunger and "Tastes like desperation" tooltip) · **Legend**: spawn with a random bonus item at game start; respawn with 50% health instead of 10% |

### Crouching (new mechanic, required by STEALTH Journeyman)

`SHIFT` key toggles a crouched state. While crouched:
- Movement speed reduced by 40%.
- Player model height reduced by half a block (camera drops accordingly).
- Footstep noise radius halved (affects `NoiseSystem`).
- Cannot sprint (sprinting removed if previously planned; this is the first mention).
At STEALTH Journeyman, crouching additionally suppresses the player's noise
entry in `NoiseSystem` to 0 (completely silent movement).

### Pick-pocketing (new mechanic, unlocked at STEALTH Expert)

Press **E** from behind a non-hostile NPC who has not noticed the player. A `PickpocketAttempt`
is resolved:
- Base success rate: 60%.
- +10% if player is crouched.
- −20% if any other NPC is within 4 blocks (witnesses).
- On success: one random `Material` is transferred from the NPC's inventory to the player's.
  `INFLUENCE` XP +3, `STEALTH` XP +5. The NPC does not notice.
- On failure: the NPC turns hostile immediately; `WantedSystem` registers a `THEFT` offence;
  any witness NPCs within 8 blocks enter `ALARMED` state and seed a `PLAYER_SPOTTED` rumour.
- Tooltip on first attempt: "Steady hands."

### RALLY Action (new mechanic, unlocked at INFLUENCE Expert)

Press **G** while at least 3 PUBLIC NPCs are within 6 blocks. All eligible PUBLIC NPCs within
6 blocks enter `RALLY_FOLLOWER` state and follow the player for 60 seconds. During the rally:
- POLICE NPCs will not approach a rallied group of 4+ (crowd intimidation).
- GANG member NPCs will retreat from a rallied group of 5+.
- If the player attacks, the rally immediately disperses.

### NEIGHBOURHOOD_EVENT (unlocked at INFLUENCE Legend)

Press **G** anywhere outside with no hostile NPCs nearby. A random event fires:
- `FLASH_RAVE` (40%): `RaveSystem` triggers an impromptu rave at that location for 3 in-game
  minutes. `PirateRadioSystem` auto-broadcasts it. Notoriety +5.
- `FLASH_MARKET` (35%): 5–8 PUBLIC NPCs converge on the spot and set up temporary stalls
  (cosmetic only) for 2 in-game minutes. Stall XP awarded passively.
- `MASS_BRAWL` (25%): all NPCs within 12 blocks enter `BRAWLING` state against each other
  (not the player) for 90 seconds. `WitnessSystem` blackout — no crimes recorded during brawl.

### SkillsUI

Bound to **K** key. Renders as a full-screen overlay (same pattern as `InventoryUI`):
- Six columns, one per skill.
- Each column shows: skill name, tier label, XP bar (filled fraction), XP number (e.g. "340/500"),
  and the next locked perk below the bar (greyed out, with a padlock icon drawn with `PixelFont`).
- Currently active perks listed below each column in green.
- A single footer line: "Press K to close."

### New Achievements

| Achievement | Trigger |
|-------------|---------|
| `FIRST_BLOOD` | Reach Apprentice in BRAWLING |
| `PROPER_HARD` | Reach Legend in BRAWLING |
| `GRAFTER` | Break 500 blocks total |
| `WHEELERDEALER` | Reach Expert in TRADING |
| `GHOST` | Commit 10 crimes in one day with 0 witnesses (requires STEALTH) |
| `WORDS_ON_THE_STREET` | Extract 50 rumours total |
| `LEGEND_OF_THE_MANOR` | Reach Legend in all six skills |
| `PICKPOCKET` | Successfully pick-pocket 20 NPCs |
| `RALLY_CRY` | Use RALLY with 8+ followers simultaneously |

### New Materials / Items

| Material | Source | Use |
|----------|--------|-----|
| `WIRE` | Dropped by COUNCIL_BUILDER NPCs (1–2); found in industrial estate | `SKELETON_KEY` recipe |
| `SKELETON_KEY` | Crafted: 3 WIRE + 1 BRICK | Opens any locked door once, consumed on use |
| `BITTER_GREENS` | Break LEAVES block while GRAFTING ≥ Journeyman | Food item: +5 hunger |

**Unit tests**: XP accumulation for each skill per action type, tier boundary
detection, perk flag states, pick-pocket probability formula (base, crouch bonus,
witness penalty), SkillsUI column rendering data, RALLY follower state transitions,
NEIGHBOURHOOD_EVENT selection weights, SKELETON_KEY door interaction, crouch
movement-speed reduction.

**Integration tests — implement these exact scenarios:**

1. **BRAWLING XP accumulates and tier transitions**: Spawn a PUBLIC NPC adjacent to
   the player. Record BRAWLING XP (expect 0). Simulate 10 punch actions on the NPC.
   Verify BRAWLING XP = 50 (10 × 5). Now simulate enough additional punches to cross
   the Apprentice threshold (200 XP). Verify skill tier is `APPRENTICE`. Verify the
   perk flag `BRAWLING_BONUS_DAMAGE` is active. Simulate one more punch on the NPC
   and verify the damage dealt is 1 higher than a punch without the perk.

2. **GRAFTING reduces block hits at Apprentice**: Set GRAFTING to Apprentice tier.
   Place a GRASS block (normally 5 hits). Simulate punch actions. Verify the block
   is broken after exactly 4 hits (5 − 1 = 4). Set GRAFTING to Novice. Place another
   GRASS block. Verify it requires the full 5 hits.

3. **STEALTH XP granted for unwitnessed crime**: Clear all NPCs from a 20-block
   radius. Player breaks a BRICK block (a crime). Verify STEALTH XP increased by 10.
   Now place a PUBLIC NPC within 8 blocks of the player (within witness range). Player
   breaks another BRICK block. Verify `WantedSystem` registers an offence AND verify
   STEALTH XP did NOT increase (witnessed crime grants no stealth XP).

4. **Pick-pocket succeeds when crouching behind NPC**: Give a PUBLIC NPC 1 COIN in
   their inventory. Set player STEALTH to Expert tier. Set player crouching = true.
   Position player directly behind the NPC (no other NPCs within 8 blocks). Press **E**.
   Verify success probability = 70% (60% base + 10% crouch). Run the scenario 100
   times (reset state each time). Verify success count is between 60 and 80 (statistical
   tolerance). On each success, verify COIN transferred to player inventory, STEALTH
   XP +5, NPC state unchanged (not hostile).

5. **Pick-pocket failure triggers wanted offence**: Set STEALTH to Expert. Position
   player behind NPC. Force pick-pocket to fail (seed RNG to guarantee failure).
   Verify NPC state transitions to `HOSTILE`. Verify `WantedSystem` records a THEFT
   offence. Verify `WitnessSystem` seeds a `PLAYER_SPOTTED` rumour.

6. **RALLY assembles followers and deters police**: Set INFLUENCE to Expert tier.
   Spawn 4 PUBLIC NPCs within 6 blocks of player. Press **G**. Verify all 4 NPCs
   enter `RALLY_FOLLOWER` state within 2 frames. Spawn a POLICE NPC 5 blocks away.
   Advance 60 frames. Verify the POLICE NPC has not moved within 3 blocks of the
   player (crowd intimidation active). Advance 3600 frames (60 seconds). Verify all
   followers revert to their previous state (rally expired).

7. **TRADING perk increases fence payout**: Set TRADING to Apprentice. Give player 1
   DIAMOND. Record the payout offered by `FenceSystem` for DIAMOND. Set TRADING to
   Novice. Record the payout again. Verify Apprentice payout is exactly 10% higher
   than Novice payout. Set TRADING to Legend. Verify the Marchetti cut applied to
   stall income is 10% (half of the normal 20%).

8. **SkillsUI opens on K and displays correct data**: Set BRAWLING to Journeyman
   (400 XP). Open SkillsUI (press K). Verify the game state is `SKILLS_OPEN`.
   Verify the BRAWLING column shows tier label "Journeyman", XP value 400, and the
   Expert perk is shown greyed out. Press K again. Verify state returns to `PLAYING`.
   Verify SkillsUI is no longer rendered.

9. **SURVIVAL Legend respawn bonus**: Set SURVIVAL to Legend. Kill the player
   (health to 0). Trigger respawn. Verify respawn health is ≥ 50% (not the default
   10%). Verify the respawn tooltip "Right then. Back at it." fires.

10. **SKELETON_KEY opens a locked door and is consumed**: Craft a SKELETON_KEY
    (3 WIRE + 1 BRICK). Interact (**E**) with a closed door that would normally be
    locked (building entrance during night hours). Verify the door opens. Verify
    SKELETON_KEY is removed from player inventory. Verify GRAFTING XP +10 is awarded.
    Attempt to use a second SKELETON_KEY on the same (now open) door. Verify the
    item is NOT consumed and a tooltip "It's already open, mate." is shown.

---

## Phase 8h: The Boot Sale — Emergent Underground Auction Economy

**Goal**: A dynamic, time-pressured black market auction that ties all existing
game systems (factions, skills, wanted level, rumour network, crafting) into a
single emergent economy loop. Every in-game day a rotating cast of shadowy sellers
sets up a clandestine "boot sale" on a patch of wasteland at the edge of the map.
Lots are auctioned in real time against NPC bidders. Miss the window and the goods
are gone.

---

### The Boot Sale Venue

- A dedicated `BOOT_SALE` landmark is generated by `WorldGenerator` on wasteland
  terrain at the south-east corner of the map (far from the JobCentre and industrial
  estate so it doesn't overlap). It consists of a ring of `WOOD` stall tables
  arranged around a central `ROAD` pad, lit by a ring of `TORCH` props at night.
- The landmark is not labelled on any in-game signage; its location is only revealed
  through the rumour network. The barman at the Ragamuffin Arms starts each new game
  with a `LOOT_TIP` rumour: "Word is there's a fella flogging gear off the back of a
  Transit near the waste ground." NPCs spread it from there.
- A `FENCE_NPC` (re-uses `NPCType.FENCE`) stands at the entrance. If the player's
  `WantedSystem` level is `WANTED` or higher the fence turns them away: "Too hot
  right now. Come back when you've cooled down."

### Auction Schedule & Lot Generation

- `BootSaleSystem` (new class) holds a daily schedule. Each in-game day (tracked by
  `TimeSystem`) it generates 6–10 `AuctionLot` objects at dawn and publishes them.
- An `AuctionLot` has:
  - `material` — the item being auctioned (`Material` enum entry)
  - `quantity` — stack size (1 for rare items, up to 20 for common materials)
  - `startPrice` — base coin value (derived from `FenceValuationTable`)
  - `buyNowPrice` — 2× startPrice; immediately wins the lot
  - `durationSeconds` — how long the lot is open (30–90 real seconds)
  - `sellerFaction` — which faction sourced the lot (affects what's available)
  - `riskLevel` — LOW / MEDIUM / HIGH (affects police spawn chance if player wins)
- Lot contents are seeded by world state:
  - `MARCHETTI_CREW` lots: `PETROL_CAN`, `WIRE`, `BRICK`, `COIN` stacks, occasionally `DIAMOND`
  - `STREET_LADS` lots: `WOOD`, `LEAVES`, crafting components, `BITTER_GREENS` in bulk
  - `THE_COUNCIL` lots: `COMPUTER`, `STAPLER`, `OFFICE_CHAIR` props (seized property)
  - Neutral lots: random scavenged materials, `SKELETON_KEY`, food items
- If the player has completed a faction mission in the last in-game day, that
  faction's lots are 20% cheaper (loyalty discount, applied silently).

### Auction Bidding Mechanic

- Up to 4 `NPC_BIDDER` virtual opponents compete per lot. Their bids are driven by
  a simple engine: each bidder has a `maxBid` (1.5–3× startPrice, randomised) and
  a `personality` (AGGRESSIVE bids immediately; PATIENT waits for the last third of
  the timer; CAUTIOUS drops out if the price exceeds 1.8× start).
- The auction runs as a countdown HUD overlay (`BootSaleUI`):
  - Left panel: lot icon (drawn with `PixelFont` block character), item name,
    quantity, current price, time remaining (seconds).
  - Right panel: bid history log (last 4 bids, most recent at top).
  - Bottom: **[F] Bid +5** | **[R] Bid +20** | **[B] Buy Now** | **[ESC] Pass**
  - Player presses **F** or **R** to raise by small/large increment; press **B** to
    pay buyNowPrice immediately; **ESC** to skip this lot (it continues without the
    player).
- New key binding: **B** (during boot sale only) = Buy Now. Listed in HelpUI.
- When the countdown expires the highest bidder wins. If the player wins:
  - Coins deducted from inventory (`Material.COIN`).
  - Item added to inventory.
  - If `riskLevel` is HIGH, `NoiseSystem` fires an alert with radius 40 blocks,
    potentially spawning a POLICE NPC at the venue entrance within 60 frames.
  - `TRADING` skill XP +15 per won lot.
  - Tooltip on first win: "Nice one. Don't ask where it came from."
- If the player cannot afford even the opening bid, the lot is greyed out and the
  player can only watch.

### Integration with Existing Systems

- **Rumour network**: Each time a HIGH-risk lot is auctioned, `RumourNetwork` seeds
  a `GANG_ACTIVITY` rumour ("There's dodgy gear changing hands on the waste ground")
  into 3 nearby NPCs after the lot closes.
- **Faction respect**: Winning a lot sourced by a faction grants +3 respect with
  that faction. Bidding against a faction NPC (outbidding their max) loses -2 respect.
- **Notoriety**: Each lot won adds 1 point to `NotorietySystem`. At 10 points the
  newspaper `NewspaperSystem` generates a headline: "Mystery Buyer Cleans Out
  Wasteland Market — Police Baffled."
- **Street Skills**: Winning 5 lots in a single day triggers the `TRADING` skill
  `NEIGHBOURHOOD_EVENT` bonus. `STEALTH` XP +5 for every lot won without police
  spawning.
- **WantedSystem**: If a POLICE NPC spawns at the venue and sees the player, a
  THEFT offence is immediately registered (possession of stolen goods).

### `BootSaleSystem` Class (new)

```
BootSaleSystem(TimeSystem, FenceValuationTable, FactionSystem,
               RumourNetwork, NoiseSystem, NotorietySystem,
               WantedSystem, StreetSkillSystem, Random)

void update(float delta, Player player, List<NPC> allNpcs)
AuctionLot getCurrentLot()           // null if no lot active
List<AuctionLot> getDaySchedule()    // today's full list
boolean playerBid(Player player, int amount)
boolean playerBuyNow(Player player)
void passLot()
boolean isVenueOpen()                // false if player is WANTED
int getLotsWonToday()
```

### `BootSaleUI` Class (new)

- Rendered as a full-screen overlay during `GameState.BOOT_SALE_OPEN`.
- New `GameState` enum value: `BOOT_SALE_OPEN`.
- Interacting (**E**) with the `FENCE_NPC` at the boot sale venue transitions to
  `BOOT_SALE_OPEN` if a lot is currently active.
- **ESC** during `BOOT_SALE_OPEN` returns to `PLAYING`.

**Unit tests**: `AuctionLot` construction and field validation, NPC bidder
personality logic (aggressive/patient/cautious bid timing), lot generation seeding
by faction, loyalty discount application, coin deduction on win, police spawn
probability for HIGH risk lots, rumour seeding after HIGH risk close, faction
respect delta on win/outbid, notoriety accumulation, `isVenueOpen()` returns false
when WANTED.

**Integration tests — implement these exact scenarios:**

1. **Boot sale venue exists and is rumour-seeded**: Generate the world. Verify a
   `BOOT_SALE` landmark exists in the south-east quadrant (x > worldWidth/2, z >
   worldHeight/2). Verify the barman NPC's rumour buffer contains a `LOOT_TIP`
   rumour with text containing "Transit" or "waste ground". Verify the `BOOT_SALE`
   landmark position does not overlap any building footprint from WorldGenerator.

2. **Daily schedule generates 6–10 lots**: Advance `TimeSystem` to the start of a
   new in-game day. Call `BootSaleSystem.update()` once. Retrieve the day schedule.
   Verify it contains between 6 and 10 `AuctionLot` objects. Verify each lot has a
   `startPrice` > 0, `durationSeconds` between 30 and 90, and a non-null `material`.
   Verify at least one lot per faction (MARCHETTI_CREW, STREET_LADS, THE_COUNCIL).

3. **Player wins lot and receives item**: Start a lot with `material=WOOD,
   quantity=5, startPrice=4`. Give the player 20 COINs. Call `playerBid(player, 10)`
   (outbids all NPC bidders whose maxBid ≤ 9). Advance `durationSeconds` worth of
   simulation frames. Verify the player's inventory contains 5 WOOD. Verify 10 COIN
   has been deducted from player inventory. Verify `TRADING` XP increased by 15.
   Verify the tooltip "Nice one. Don't ask where it came from." fired.

4. **HIGH risk lot spawns police on win**: Set up a HIGH-risk lot. Give the player
   enough coins to win at startPrice. Call `playerBuyNow(player)`. Verify
   `NoiseSystem` has broadcast an alert with radius ≥ 40. Advance 120 frames. Verify
   at least one POLICE NPC has spawned within 50 blocks of the venue. Verify
   `WantedSystem` registers a THEFT offence if the POLICE NPC can see the player.

5. **WANTED player is turned away**: Set `WantedSystem` level to `WANTED`. Simulate
   pressing **E** on the FENCE_NPC at the boot sale venue. Verify game state does
   NOT transition to `BOOT_SALE_OPEN`. Verify a speech line containing "Too hot"
   is shown. Verify no lots are accessible. Clear the wanted level. Simulate pressing
   **E** again. Verify the game transitions to `BOOT_SALE_OPEN`.

6. **NPC bidder personalities behave correctly**: Set up a lot with `startPrice=10,
   durationSeconds=60`. Add one AGGRESSIVE bidder (maxBid=20) and one PATIENT
   bidder (maxBid=25). Advance to t=5s. Verify the AGGRESSIVE bidder has placed at
   least one bid. Verify the PATIENT bidder has NOT bid yet. Advance to t=45s (75%
   of duration). Verify the PATIENT bidder has now entered bidding. Verify the
   PATIENT bidder ultimately wins (higher maxBid). Verify final price is between
   startPrice and 25.

7. **Faction loyalty discount applies**: Complete a MARCHETTI_CREW mission (set
   `FactionSystem` mission completed flag). Advance to next day. Retrieve the day's
   MARCHETTI_CREW lots. Verify each has an effective start price 20% lower than the
   base `FenceValuationTable` value for that material. For non-MARCHETTI lots,
   verify no discount is applied.

8. **Rumour seeded after HIGH-risk close**: Set up and complete a HIGH-risk lot
   (player or NPC wins, doesn't matter). After the lot closes, advance 10 frames.
   Retrieve all NPCs within 30 blocks of the venue. Verify at least 3 of them
   carry a `GANG_ACTIVITY` rumour with text containing "waste ground" or "dodgy gear".

9. **Notoriety milestone triggers newspaper headline**: Win 10 lots total (simulate
   across multiple in-game days). After the 10th lot, advance 30 frames. Verify
   `NewspaperSystem` has generated a headline containing "Mystery Buyer" or
   "Wasteland Market". Verify `NotorietySystem.getPoints()` is ≥ 10.

10. **Full boot sale loop stress test**: Generate a world. Advance to boot sale
    hours. Give the player 100 COINs. Interact with the FENCE_NPC. Verify state is
    `BOOT_SALE_OPEN`. Bid on 3 consecutive lots (win at least 1, lose at least 1
    by running out of bids). Skip 1 lot with ESC. Verify inventory updated for won
    lots only. Verify TRADING XP accumulated. Verify no null pointer exceptions.
    Press ESC after final lot. Verify state returns to `PLAYING`.

---

## Phase 8h: The JobCentre Gauntlet — Universal Credit, Sanctions & Bureaucratic Torment

**Goal**: Turn the existing JobCentre landmark into a fully interactive satirical
survival system. The player must periodically "sign on" to receive Universal Credit
payments (coins) — but doing so means enduring soul-crushing bureaucratic missions,
risking sanctions that cut their income, and constantly dodging the absurd conflict
between their criminal career and their official claimant status. Dark British
humour, systemic irony, and meaningful economic consequence.

### Overview

The `JobCentreSystem` runs on the in-game clock. Every **3 in-game days** the
player must visit the JobCentre and "sign on" (press **E** on the `CASE_WORKER`
NPC inside) to remain eligible for Universal Credit. Missing a sign-on triggers a
**sanction** (income cut). Signing on while carrying stolen goods or having an
active Wanted level creates comic-bureaucratic consequences. The system interfaces
with `TimeSystem`, `NotorietySystem`, `CriminalRecord`, `FactionSystem`,
`RumourNetwork`, `NewspaperSystem`, `StreetSkillSystem`, and `WantedSystem`.

### Universal Credit & Sign-On Cycle

- The player starts as a Universal Credit claimant. An initial claim letter prop
  (`PropType.DWP_LETTER`) is placed at the player's starting position at game start.
  Tooltip on first encounter: "Thirty-seven quid a week. The government's idea of
  a helping hand."
- Sign-on window: a 1 in-game hour window opens every 3 in-game days at 09:00.
  The `CASE_WORKER` NPC is only interactable during this window.
- **Successful sign-on**: Player receives a `UC_PAYMENT` of **8 COIN** per cycle.
  The case worker delivers a new **Job Search Requirement** mission (see below).
  A `JobCentreRecord` (new class) logs the date signed and clears any pending
  sanction.
- **Missed sign-on**: A `SANCTION_LEVEL` increments (0–3). Each level reduces the
  UC_PAYMENT by 3 COIN (floor 0). Rumour seeded: "Heard someone got sanctioned
  again down the JobCentre." At SANCTION_LEVEL 3: payment drops to 0 and the
  `CASE_WORKER` spawns a `DEBT_COLLECTOR` NPC that pursues the player until paid
  10 COIN or 3 in-game hours elapse.
- Attending the sign-on resets SANCTION_LEVEL to 0 only if the player completes
  the assigned Job Search Requirement.

### Job Search Requirement Missions

Each sign-on assigns one of 5 satirical Job Search Requirement missions. These are
delivered as a `PropType.DWP_LETTER` dropped at the player's feet. The player has
until the NEXT sign-on window (3 in-game days) to complete it:

| Mission | Task | Satirical Twist |
|---------|------|-----------------|
| **CV Workshop** | Go to the JobCentre within 1 in-game day and press **E** on the `MOTIVATIONAL_POSTER` prop on the wall for 3 seconds | Tooltip: "How to write a CV when you've been unemployed for 6 years." |
| **Apply for 3 Jobs** | Interact (**E**) with 3 different SHOP_SIGN props (Greggs, off-licence, charity shop) | Each shop sign NPC speech: "Sorry, we're not hiring." The third says "We'll keep your CV on file." |
| **Mandatory Work Placement** | Pick up 10 pieces of litter (new `PropType.LITTER` scattered around the park at world-gen) within 2 in-game hours | Tooltip: "Unpaid, obviously." |
| **Universal Jobmatch Profile** | Stand in front of the `COMMUNITY_NOTICE_BOARD` prop (placed near the park, new `PropType`) for 5 seconds | Tooltip: "The site crashes on Internet Explorer 6. Always." |
| **Attend Work Capability Assessment** | Survive 2 in-game minutes inside the JobCentre while a `ASSESSOR` NPC follows and questions you with speech bubbles | The ASSESSOR asks: "Can you walk 50 metres?" Speech: "Yes." "Then you're fit for work." |

**Completing a Job Search Requirement** resets `SANCTION_LEVEL` and awards +5
`BUREAUCRACY` XP (new `StreetSkillSystem` skill track — see below).

**Deliberately failing a mission** (staying away from the JobCentre for the full
cycle) is a valid strategy if the player has enough criminal income — but
accumulating SANCTION_LEVEL 3 summons the DEBT_COLLECTOR.

### The Absurdity Engine — Criminal Record Complications

When the player signs on, the `CASE_WORKER` NPC checks their `CriminalRecord`:

- **0–2 offences**: Normal sign-on. Case worker says "Sign here. Next!"
- **3–5 offences**: Case worker is suspicious. A mini-dialogue fires:
  "You haven't been getting into trouble, have you?" Player can:
  - **Lie** (always succeeds but adds 1 BUREAUCRACY XP and seeds a rumour:
    "That one's got a right cheek, signing on after what they've been up to").
  - **Admit it** (SANCTION_LEVEL +1 immediately but BUREAUCRACY XP +3 and
    case worker respects honesty: UC_PAYMENT +2 this cycle only).
- **6+ offences OR active Wanted level**: The case worker calls the police.
  A `POLICE` NPC spawns at the JobCentre entrance. Player has 30 seconds to
  exit before the NPC spots them. Tooltip: "Probably should have left the
  balaclava at home."
- **Notoriety Tier 3+**: The case worker has read the newspaper. They address
  the player by their Street Legend Title: "Ah, the Area Menace. Fill in this
  form. In triplicate." Job Search Requirement is automatically failed this
  cycle (too infamous for honest work) — but the player still gets the UC_PAYMENT
  as the case worker is too scared to sanction them.

### The DEBT_COLLECTOR NPC

A new `NPCType.DEBT_COLLECTOR` — a grey-suited man with a clipboard. He is
NOT a police officer (cannot arrest) but is persistent and embarrassing:

- Spawns at the JobCentre and pathfinds toward the player's last known position.
- When within 4 blocks: speech bubble "Oi! You owe the DWP." Every 10 seconds
  he repeats this. Nearby NPCs laugh (emote animation).
- If the player pays 10 COIN (press **E** on the DEBT_COLLECTOR when adjacent):
  he leaves. Speech: "Sorted. We'll be in touch."
- If the player runs away for 3 in-game hours: he gives up and despawns. Rumour
  seeded: "Someone's been dodging the debt collector all week."
- If the player punches the DEBT_COLLECTOR: Notoriety +15, immediate Wanted
  level, and `NewspaperSystem` generates headline: "Local Scrounger Assaults DWP
  Official." DEBT_COLLECTOR enters FLEEING state.
- The DEBT_COLLECTOR cannot be killed (health resets to full when fleeing, he
  despawns off-screen). He is unkillable bureaucracy made flesh.

### BUREAUCRACY Skill Track (new StreetSkillSystem track)

A new skill track added to `StreetSkillSystem`:

| Level | XP Required | Perk |
|-------|------------|------|
| 1 | 10 XP | **Form Filler**: sign-on window extended by 30 in-game minutes |
| 2 | 25 XP | **Appeals Expert**: first sanction each cycle is automatically overturned |
| 3 | 50 XP | **System Player**: UC_PAYMENT increased by 3 COIN per cycle permanently |
| 4 | 80 XP | **Ghost in the Machine**: CriminalRecord offence count is treated as half its true value when the case worker checks |
| 5 | 120 XP | **Off the Grid**: player is no longer tracked by `NotorietySystem` for SURVEILLANCE_VAN placement (Tier 4–5 perk nullified) |

BUREAUCRACY XP is earned by: completing Job Search Requirements (+5), lying to
the case worker (+1), successfully signing on without a criminal complication (+2),
and attending 5 consecutive sign-ons without a missed window (+10 bonus).

### Faction Cross-Pollination

| Event | Effect |
|-------|--------|
| Sign on while carrying a PETROL_CAN (Marchetti mission item) | Case worker confiscates it; Marchetti Respect −10; rumour seeded |
| Sign on with BUREAUCRACY Level 3+ | Council Respect +5 ("One of us, sort of") |
| Miss 3 sign-ons in a row | Street Lads Respect +8 ("Proper anti-establishment, that") |
| DEBT_COLLECTOR active while player completes a faction mission | Faction lieutenant speech: "Sort out your benefits before you talk to us." Mission reward coins reduced by 2 |
| Notoriety Tier 5 ("The Ragamuffin") and attempting to sign on | Case worker flees the building. DWP_LETTER auto-deposited: "Due to exceptional circumstances, your claim has been permanently closed." UC payments end. Notoriety +10 |

### JobCentre Building Enhancement

The existing JobCentre landmark gains interior detail at world-gen time:

- A `CASE_WORKER` NPC at a desk (does not wander; only present during sign-on
  window, replaced by a `CLOSED_SIGN` prop outside the window).
- A `MOTIVATIONAL_POSTER` prop on the wall ("WORKING TOGETHER FOR YOUR FUTURE"
  in block-letter `PixelFont` rendering).
- A `COMMUNITY_NOTICE_BOARD` prop near the entrance (exterior, always present).
- Rows of plastic chairs (prop). Tooltip on sitting adjacent to them: "You've
  been waiting 47 minutes. Your number is 312."
- `PropType.LITTER` scattered in the immediate surrounding streets (10–15 pieces
  at world-gen, respawn every in-game day).
- An `ASSESSOR` NPC that only spawns during the Work Capability Assessment mission.

### `JobCentreSystem` Class (new)

```
JobCentreSystem(TimeSystem, CriminalRecord, NotorietySystem,
                FactionSystem, RumourNetwork, NewspaperSystem,
                StreetSkillSystem, WantedSystem, NPCManager, Random)

void update(float delta, Player player, List<NPC> allNpcs)
boolean isSignOnWindowOpen()
boolean trySignOn(Player player, List<NPC> allNpcs)   // returns success
JobSearchMission getCurrentMission()                   // null if none assigned
boolean tryCompleteMission(Player player, World world)
int getSanctionLevel()
int getCurrentUCPayment()                              // 0–8 COIN
boolean isDebtCollectorActive()
void onDebtCollectorPaid(Player player)
```

### `JobCentreUI` Screen (new)

- Triggered by pressing **E** on the `CASE_WORKER` NPC during the sign-on window.
- Transitions to `GameState.JOB_CENTRE_OPEN` (new enum value).
- Displays: current UC payment amount, current Job Search Requirement mission,
  sanction level (shown as a warning strip), a "Sign Here" button (confirm sign-on),
  and a "Leave" button (**ESC** also works).
- If criminal record complication: dialogue panel appears with "Lie" / "Admit it"
  options before the sign-on is processed.

**Unit tests**: UC payment calculation at each SANCTION_LEVEL, Job Search
Requirement mission assignment (one per cycle, no repeats until pool exhausted),
DEBT_COLLECTOR spawn condition and despawn timer, criminal record complication
threshold checks, BUREAUCRACY XP gain per action, faction cross-pollination
triggers, case worker flee on Tier 5 Notoriety.

**Integration tests — implement these exact scenarios:**

1. **Sign-on window opens every 3 in-game days**: Start a new game. Verify
   `isSignOnWindowOpen()` is false. Advance `TimeSystem` by exactly 3 in-game days
   to 09:00. Verify `isSignOnWindowOpen()` is true. Verify a `CASE_WORKER` NPC is
   present inside the JobCentre. Advance 1 in-game hour. Verify the window is now
   closed and the `CASE_WORKER` is replaced by a `CLOSED_SIGN` prop.

2. **Successful sign-on awards coins and assigns mission**: Give the player 0 COIN.
   Open the sign-on window. Press **E** on the `CASE_WORKER`. Verify `trySignOn()`
   returns true. Verify the player's inventory now contains exactly 8 COIN (base
   UC_PAYMENT, no sanctions). Verify `getCurrentMission()` returns a non-null
   `JobSearchMission`. Verify `getSanctionLevel()` is 0.

3. **Missed sign-on increments sanction and reduces payment**: Miss one sign-on
   window (advance past it without visiting). Verify `getSanctionLevel()` is 1.
   Open the next sign-on window. Sign on. Verify the player receives only 5 COIN
   (8 − 3 for sanction level 1). Miss two more windows. Verify `getSanctionLevel()`
   reaches 3 and `getCurrentUCPayment()` returns 0. Verify a `DEBT_COLLECTOR` NPC
   has spawned and is pathfinding toward the player.

4. **DEBT_COLLECTOR paid off with 10 coins**: Let DEBT_COLLECTOR spawn (SANCTION
   LEVEL 3). Give the player 10 COIN. Move player adjacent to DEBT_COLLECTOR. Press
   **E**. Verify the DEBT_COLLECTOR despawns. Verify player inventory has 0 COIN.
   Verify `isDebtCollectorActive()` returns false.

5. **Criminal record complication at 4 offences**: Set `CriminalRecord` to 4
   offences. Open sign-on. Press **E** on CASE_WORKER. Verify the complication
   dialogue fires (UI shows "Lie" / "Admit it" options). Select "Lie". Verify sign-on
   completes, BUREAUCRACY XP increases by 1, and a rumour containing "signing on" is
   seeded into at least 1 nearby NPC.

6. **6+ offences triggers police spawn**: Set `CriminalRecord` to 6 offences.
   Open sign-on window. Press **E** on CASE_WORKER. Verify a `POLICE` NPC spawns
   at the JobCentre entrance within 5 frames. Verify the player has 30 in-game
   seconds before the POLICE NPC enters CHASING state. Move player outside JobCentre
   within the window. Verify POLICE NPC enters PATROL state once player is 20+ blocks
   away.

7. **CV Workshop mission completes on poster interaction**: Receive the CV_WORKSHOP
   job search mission. Move player to the `MOTIVATIONAL_POSTER` prop inside the
   JobCentre. Hold **E** for 3 seconds (simulate 180 frames at 60fps). Verify
   `tryCompleteMission()` returns true. Verify BUREAUCRACY XP increased by 5. Verify
   next sign-on SANCTION_LEVEL resets to 0.

8. **Notoriety Tier 3 case worker addresses player by title**: Set Notoriety to 500
   (Tier 3, "Area Menace"). Open sign-on window. Press **E** on CASE_WORKER. Verify
   the CASE_WORKER speech bubble contains "Area Menace". Verify the Job Search
   Requirement mission is auto-failed (mission status is FAILED immediately). Verify
   the player still receives the UC_PAYMENT.

9. **Notoriety Tier 5 case worker flees**: Set Notoriety to 1000 (Tier 5). Open
   sign-on window. Move player inside the JobCentre. Verify the `CASE_WORKER` NPC
   immediately transitions to FLEEING state upon player entry. Verify a
   `PropType.DWP_LETTER` prop is placed at the player's feet within 5 frames.
   Verify `getCurrentUCPayment()` returns 0 and no further sign-on windows open.
   Verify Notoriety increased by 10.

10. **Full benefit cycle stress test**: Start a new game. Complete 3 full sign-on
    cycles: (a) complete a Job Search Requirement mission each cycle; (b) on cycle 2,
    commit 4 offences and choose "Admit it" in the complication dialogue; (c) on
    cycle 3, miss the window entirely. Verify after all 3 cycles: BUREAUCRACY XP ≥
    12, SANCTION_LEVEL is 1, player received coins in cycles 1 and 2 but not cycle 3,
    DEBT_COLLECTOR has NOT spawned (only 1 missed window, not 3). Verify no NPEs,
    no crashes, `GameState` remained valid throughout.

---

## Phase 20: The Neighbourhood Watch — Vigilante Mob Justice & Community Uprising

**Goal**: When the player's criminal exploits become too visible and too frequent, the
neighbourhood fights back — not through the police, but through ordinary residents
who've simply had enough. A `NeighbourhoodWatchSystem` dynamically assembles mobs of
enraged civilians (armed with mops, clipboards, and righteous indignation) that patrol
the area, corner the player, and attempt a citizen's arrest. The Council secretly funds
it. The Street Lads mock it. The Marchetti Crew use it as cover for their own operations.
It is the most British possible threat: a passive-aggressive collective of curtain-twitchers
who have finally snapped.

This system ties together: `NotorietySystem`, `WitnessSystem`, `FactionSystem`,
`WeatherSystem`, `RumourNetwork`, `NewspaperSystem`, `PropertySystem`, `CriminalRecord`,
and the `NeighbourhoodSystem` (already tracking gentrification/decay) into a single
emergent community-threat engine.

### The Watch Anger Level

A new persistent `WatchAnger` score (0–100, starting at 0) tracks how much the
neighbourhood has collectively turned against the player. It is distinct from Notoriety
(which measures criminal fame) and Faction Respect (which measures gang loyalty) — Watch
Anger is about the ordinary people.

**Watch Anger rises from:**

| Event | Anger gained |
|-------|-------------|
| Player breaks a block on a publicly visible exterior wall | +3 |
| Player punches a civilian (PUBLIC) NPC | +8 |
| Player commits a crime witnessed by 2+ NPCs simultaneously | +5 |
| Player's Notoriety tier crosses a new threshold | +10 (once per tier) |
| A `WANTED_POSTER` prop is visible within 10 blocks of any civilian NPC | +2/in-game hour |
| Player's squat or rave generates a police response (police alerted) | +7 |
| Council Victory faction state is active | +15 (one-time, Council spreads pamphlets) |
| Player destroys a `CONDEMNED_NOTICE` prop | +4 |

**Watch Anger decays passively:**
- −1 per in-game hour when player stays out of trouble (no crimes for 1 full hour)
- −5 immediately when player buys a round at the pub for everyone (costs 10 coins; the
  "keep your neighbours sweet" mechanic — separate from the faction Buy A Round)
- −10 when player completes a Council mission (tidy neighbourhood = calmer residents)
- Rain and fog weather: decay rate doubles (miserable weather keeps people indoors)

Watch Anger **never decays below 0** and is stored in `NeighbourhoodWatchSystem`.

### Watch Anger Thresholds & Escalation

| Threshold | State | What Happens |
|-----------|-------|-------------|
| 0–24 | **Mutterings** | Occasional civilian NPCs stop and stare at the player. No mechanical effect. |
| 25–49 | **Petitions** | A `PropType.PETITION_BOARD` prop appears outside the pub. Clicking it (E) shows the complaint list — a darkly comic catalogue of the player's crimes. Watch members (new `NPCType.WATCH_MEMBER`) spawn: 1–2 NPCs patrolling near the player's last known crime scene. They don't attack but do follow the player for up to 20 blocks, muttering. |
| 50–74 | **Vigilante Patrol** | A `NeighbourhoodWatch` mob of 3–5 `WATCH_MEMBER` NPCs assembles near the highest-crime block and actively patrols. They perform a soft citizen's arrest: if they corner the player (all exits within 3 blocks occupied by Watch Members), the player is immobilised for 5 seconds while a Watch Member delivers a 2-line speech ("We know what you've been up to." / "The council has been notified.") then releases the player — but adds 2 offences to the criminal record. |
| 75–89 | **Organised Mob** | The mob grows to 6–10 Watch Members. They now carry improvised weapons (prop: `MOPHANDLE`, functionally like a stick — 3 hits to incapacitate the player, same as ARU). The mob coordinates: one Watch Member calls to others when they spot the player, causing all Watch Members within 30 blocks to converge. If the player runs into faction territory, the faction NPCs and Watch Members briefly fight each other (Watch Members attack any non-Public NPC they encounter), creating a glorious multi-faction brawl. |
| 90–100 | **Full Uprising** | The entire neighbourhood mobilises. Every PUBLIC NPC temporarily becomes a `WATCH_MEMBER`. The pub barman locks the door ("Not tonight — there's a mob outside"). The JobCentre CASE_WORKER calls the police proactively. A `RumourType.WATCH_UPRISING` rumour floods the network: "They've proper had enough of that one." The Newspaper generates a front page: "AREA RESIDENTS TAKE LAW INTO OWN HANDS". This state lasts 5 in-game minutes, then Anger resets to 60 (partially exhausted). |

### Watch Members — NPC Behaviour

New `NPCType.WATCH_MEMBER` with distinct visual variant (high-vis tabard over civilian
clothing — reuse existing `NPCModelVariant` colour-override system with a fluorescent
yellow tint).

**AI states:**
- `PATROLLING`: Walking between two waypoints near the last-known crime scene.
- `MUTTERING`: Following the player at ≥ 3 block distance, occasionally emitting
  speech bubbles from a pool of passive-aggressive British phrases (see below).
- `CORNERING`: Moving to block the player's path (triggers at Anger ≥ 50 if player
  is within 8 blocks and at least 2 Watch Members are present).
- `CITIZEN_ARRESTING`: Delivering the soft-arrest speech when all exits blocked.
- `BRAWLING`: Briefly attacking nearby non-Public NPCs (gang members, police — equal
  opportunity outrage) when mob Anger ≥ 75. Watch Members do NOT attack the player
  violently below Anger 75.
- `FLEEING`: If the player has an accomplice NPC, Watch Members are 30% more likely
  to flee (intimidated by numbers).

**Passive-aggressive speech bubble pool** (displayed in `SpeechLogUI`):
- "I've lived here thirty years."
- "I'm on the committee, you know."
- "There are children about."
- "I will be writing to my MP."
- "The CCTV is working, by the way."
- "Some of us have jobs."
- "I've taken a photo of you."
- "The council will hear about this."
- "Have you tried the JobCentre?"
- "This used to be a nice area."

### Faction Interactions with the Watch

The Watch is officially neutral but factions exploit it:

- **The Council** secretly funds Watch patrols when their turf fraction exceeds 40%:
  Watch Member spawn rate doubles and they are equipped with clipboards (prop:
  `CLIPBOARD`) that let them add offences to the criminal record at Anger ≥ 50.
- **Street Lads** find Watch Members hilarious. When a Watch Member enters Street Lads
  turf, a nearby Street Lad NPC catcalls them ("Oi! Neighbourhood Watch? More like
  Neighbourhood Snitch!") — this reduces Watch Anger by 3 (banter diffuses tension)
  but increases Street Lads vs Watch brawl probability.
- **Marchetti Crew** use Watch commotion as a distraction: if a Watch Uprising is
  active, the Marchetti Crew's mission cooldown resets early (they get a free mission
  offer) because the police are distracted.

### Newspaper Integration

`NewspaperSystem` gains three new headline templates triggered by Watch state:

1. **Anger 50 crossed**: "RESIDENTS START PETITION OVER 'ANTISOCIAL ELEMENT' ON HIGH STREET"
2. **Anger 75 crossed**: "VIGILANTE GROUP PATROLS [STREET NAME] AFTER SPATE OF INCIDENTS"
3. **Uprising (Anger 90+)**: "MOB JUSTICE RETURNS TO [NEIGHBOURHOOD NAME] — POLICE CALLED"

Each headline seeds a `RumourType.WATCH_UPRISING` rumour into 5 random NPCs (via
barman as primary vector), and the Newspaper prop updates with the new front page.

### New Key Binding: **G** — Grovel

At any Anger threshold, the player can press **G** while a Watch Member NPC is within
3 blocks and not in BRAWLING state. This triggers a "grovel" interaction:
- A 2-second hold (displayed as a progress bar overlay).
- On completion: the Watch Member's speech changes to "Well... just don't let it happen
  again." The Watch Member transitions to PATROLLING (disengages).
- Reduces Watch Anger by 5. Costs nothing — just humiliating.
- Tooltip on first grovel: "Dignity: traded. Temporarily."
- Achievement: `GROVELLED` (grovel successfully 5 times in one session).

### New Crafting Recipes

Two new items help manage Watch Anger:

- **NEIGHBOURHOOD_NEWSLETTER** (craftable: 3 PAPER + 1 COIN): A propaganda pamphlet.
  Using it (right-click from hotbar) near a `PETITION_BOARD` prop removes the board
  and reduces Anger by 8. Can only be used once per Anger-25 escalation.
- **PEACE_OFFERING** (craftable: 1 SAUSAGE_ROLL + 1 COIN): Give to a Watch Member
  (press E) to immediately convert them from MUTTERING/CORNERING to PATROLLING.
  Reduces Anger by 3. Tooltip: "A sausage roll. The great British peacemaker."

### HUD Integration

A new **Watch Anger** indicator appears on the GameHUD: a small clipboard icon in
the bottom-left corner with a fill bar (0–100). The icon pulses when Anger increases.
At Anger ≥ 75, the bar glows orange. At Anger ≥ 90, it pulses red with an
"UPRISING" text label. At Anger 0–24, the bar is greyed out.

Tooltip on first Watch Anger increase: "The neighbours are watching."
Tooltip on first Watch Member encounter: "They've got clipboards. Somehow more
terrifying than the police."

### Achievements

| Achievement | Trigger |
|-------------|---------|
| `CURTAIN_TWITCHER` | Have Watch Anger reach 50 for the first time |
| `NEIGHBOURHOOD_MENACE` | Trigger a full Watch Uprising |
| `GROVELLED` | Grovel successfully 5 times in one session |
| `PEACE_OFFERING_ACCEPTED` | Successfully pacify a Watch Member with a sausage roll |
| `THE_COMMITTEE` | Be citizen-arrested by the Watch 3 times |
| `BRAWL_STARTER` | Cause a Watch vs. faction multi-brawl (Watch Anger ≥ 75, gang territory) |

**Unit tests**: Watch Anger gain/decay calculations, threshold crossing detection,
Watch Member NPC state machine transitions, citizen-arrest immobilisation logic,
grovel interaction timer, faction Council funding multiplier, newspaper headline
trigger conditions, uprising reset after 5 minutes, rain/fog decay rate doubling,
sausage roll peace offering, newsletter prop removal, all new achievement triggers.

**Integration tests — implement these exact scenarios:**

1. **Watch Anger rises from witnessed crimes**: Start with Watch Anger 0. Commit 3
   exterior block-break crimes in view of 2+ civilian NPCs each time. Verify Watch
   Anger is at least 9 (3× block break = +3 each) plus 15 (3× 2-witness bonus = +5
   each) = total ≥ 24. Verify no Watch Members have spawned yet (below threshold 25).

2. **Petition Board spawns and shows complaint list at Anger 25**: Set Watch Anger to
   25. Advance 1 frame. Verify a `PropType.PETITION_BOARD` prop exists near the last
   crime scene. Press **E** on the prop. Verify the UI displays a non-empty list of
   complaint strings matching the player's recent crimes.

3. **Watch Member corners player and applies citizen's arrest**: Set Watch Anger to 55.
   Spawn 3 Watch Member NPCs surrounding the player on 3 sides (player has 1 exit).
   Advance 60 frames to allow CORNERING state. Block the last exit. Verify all 3 NPCs
   enter `CITIZEN_ARRESTING` state. Verify player is immobilised for 5 seconds (position
   unchanged over 300 frames). Verify criminal record gains 2 offences after the arrest.
   Verify Watch Members return to `PATROLLING` after the speech.

4. **Grovel reduces Watch Member engagement and Anger**: Set Watch Anger to 60. Spawn a
   Watch Member in MUTTERING state within 3 blocks of player. Press and hold **G** for
   2 in-game seconds (120 frames at 60fps). Verify Watch Member transitions to
   `PATROLLING`. Verify Watch Anger decreased by 5 (to 55). Verify the tooltip
   "Dignity: traded. Temporarily." fires on first grovel.

5. **Uprising fires and resets at Anger 90**: Set Watch Anger to 89. Commit 1 crime
   (+3 block break → Anger 92). Advance 1 frame. Verify all PUBLIC NPCs within the
   loaded world have been converted to `WATCH_MEMBER` type. Verify the barman's pub
   door is locked (player cannot enter pub AABB). Verify `RumourType.WATCH_UPRISING`
   rumour exists in at least 5 NPCs. Advance 5 in-game minutes (300 frames ×
   60fps). Verify Watch Anger has reset to exactly 60. Verify converted NPCs have
   returned to `PUBLIC` type.

6. **Council funding doubles Watch spawn rate**: Set Council turf fraction to 51%
   (above 40% threshold). Set Watch Anger to 26. Verify Watch Member spawn rate is
   2× the base rate (record spawns over 60 frames, compare to Watch Anger 26 with
   Council fraction below 40%).

7. **Weather doubles Anger decay rate**: Set Watch Anger to 50. Set weather to RAIN.
   Advance 1 in-game hour of simulation. Verify Watch Anger decreased by 2 (double
   the 1/hour base rate). Set weather to CLEAR. Advance 1 more in-game hour. Verify
   Watch Anger decreased by only 1 (base rate).

8. **Peace offering pacifies Watch Member**: Set Watch Anger to 55. Give player 1
   PEACE_OFFERING in hotbar. Spawn a Watch Member NPC in CORNERING state within 3
   blocks. Press **E** on the Watch Member (interact). Verify Watch Member transitions
   to PATROLLING. Verify Watch Anger decreased by 3. Verify PEACE_OFFERING removed
   from player inventory. Verify the `PEACE_OFFERING_ACCEPTED` achievement fires.

9. **Newspaper headline fires at Anger 75**: Set Watch Anger to 74. Commit a crime that
   raises Anger to 75. Advance 1 frame. Verify `NewspaperSystem` has generated a
   headline containing "VIGILANTE GROUP PATROLS". Verify 5 NPCs in the world have a
   `WATCH_UPRISING` rumour seeded. Verify the `NEIGHBOURHOOD_MENACE` achievement has
   NOT yet fired (it requires the full 90+ uprising).

10. **Full Watch arc stress test**: Start at Watch Anger 0. Progress through all four
    thresholds by committing crimes: verify Petition Board spawns at 25, Watch Member
    patrol at 50, organised mob with weapons at 75, full Uprising at 90. At each
    threshold: verify correct number of Watch Members spawned, correct NPC states,
    correct newspaper headline, correct rumour seeding. Trigger a Council-funded mob
    (set Council turf > 40%), a gang brawl (Watch enters gang turf at Anger ≥ 75),
    and a grovel interaction. Verify game remains in PLAYING state, no NPEs, all NPC
    counts valid, Watch Anger HUD bar visible and rendering correctly throughout.

---

## Phase 11: The Corner Shop Economy — Dynamic Shopkeeping, Price Wars & Neighbourhood Hustle

**Goal**: Let the player become a shopkeeper. They can squat or buy a derelict shop unit,
stock it with looted or crafted goods, set their own prices, hire a runner NPC to make
deliveries, and run a grey-to-black-market enterprise that competes directly with Marchetti's
off-licence and The Council's approved traders. A corner shop transforms the player from
a reactive scrapper into an active neighbourhood power — and gives the community something
to defend, destroy, or tax.

This phase builds on: `SquatSystem`, `FenceSystem`, `BootSaleSystem`, `FactionSystem`,
`NeighbourhoodSystem`, `PropertySystem`, `NPCManager`, `RumourNetwork`, `NewspaperSystem`,
`StreetSkillSystem`, and `Inventory`.

---

### Taking Over a Shop Unit

Any landmark building tagged `LandmarkType.CHARITY_SHOP`, `LandmarkType.OFF_LICENCE`,
or any world-generated **derelict shop** (Condition ≤ 49 per the `PropertySystem`) can
be claimed by the player as a **Corner Shop**.

**Claim mechanic**:
1. The player presses **E** on the shop's front door (a `PropType.DOOR` within 2 blocks)
   while the building's Condition is ≤ 49 OR while holding a `Material.SHOP_KEY` (obtainable
   by completing a Street Lads "Office Job" mission — the key is among the looted items).
2. If unclaimed: a `ShopUnit` record is created and associated with the building's landmark
   footprint. A `PropType.SHOP_SIGN` is placed on the exterior wall. The claim is FREE for
   derelict buildings; occupied buildings require 20 coins (back-pocket deal with the current
   occupant — a single dialogue choice via the interaction system).
3. Only **one shop** can be owned at a time. Attempting to claim a second shop prompts:
   *"You can't run two shops. You're not Amazon."*
4. Claiming a shop that is in Marchetti Crew territory (per `TurfMap`) immediately sets
   Marchetti Respect −20 and seeds a rumour: *"Someone's muscling in on the Marchetti
   patch — bold move."*

New items: `Material.SHOP_KEY`, `PropType.SHOP_SIGN`.

---

### The Shop Inventory & Pricing System

A new `ShopUnit` class tracks the state of a claimed shop:

| Field | Description |
|-------|-------------|
| `stockMap` | `Map<Material, Integer>` — current stock levels |
| `priceMap` | `Map<Material, Integer>` — player-set sell prices (in coins) |
| `dailyRevenue` | Coins earned in the current in-game day |
| `condition` | Linked to the building's `PropertySystem` Condition score |
| `openForBusiness` | boolean — true when the shop is stocked and the sign is lit |
| `heatLevel` | 0–100 — police/council attention on this shop (see below) |
| `runnerNpc` | Reference to the hired runner NPC, or null |

**Stocking the shop**: The player places items from their hotbar into the shop's stock by
pressing **E** on the `SHOP_SIGN` prop, which opens a new **Shop Management UI** (key **M**
while inside the shop). From this UI the player can:
- Transfer items from their personal inventory into `stockMap`.
- Set a sell price per item (default: `FenceSystem` buy price × 1.5 — a modest markup).
- Toggle the shop open or closed.

**NPC customers**: Once open, pedestrian NPCs (type `PUBLIC`, `WORKER`, `PENSIONER`) passing
within 8 blocks of the shop have a **15% chance per in-game minute** of entering to buy one
random in-stock item at the player's listed price. Each purchase:
- Removes 1 unit from `stockMap`.
- Adds the listed price in coins directly to a `ShopUnit.cashRegister` (collected by pressing
  **E** on the shop sign — a satisfying "kerching" sound effect).
- Seeds a `RumourType.SHOP_NEWS` rumour into the purchasing NPC: *"That new shop on [street]
  is dead cheap / a right rip-off."* (depends on whether the price is below or above Fence
  valuation).

New `RumourType` entry: `SHOP_NEWS`.

**Price psychology**: Each item has a `FenceValuationTable` base value. Player pricing
affects customer traffic:
- **Undercut** (price < 80% of base): +30% customer chance, but Marchetti Respect −5/day
  (you're undercutting their patch).
- **Fair** (80–120% of base): baseline traffic.
- **Overpriced** (> 150% of base): −50% customer traffic; seeds rumour *"That shop's taking
  the mick with those prices."*

---

### The Runner NPC

At Notoriety Tier 1+, the player can hire a **Runner**: an NPC accomplice who restocks
the shop and makes deliveries autonomously.

**Hiring**: Press **E** on any `PUBLIC` or `YOUTH` NPC while inside the player's shop
(the shop door must be within 5 blocks). Dialogue option: *"Want a job? Cash in hand."*
Costs 5 coins/in-game day (deducted automatically from `cashRegister` at dawn). If the
shop can't pay wages, the Runner quits: *"I'm not working for free, mate."*

**Runner behaviour**:
1. **Restocking run**: If any item in `stockMap` has quantity 0, the Runner pathfinds to
   the nearest available source of that `Material` (boot sale stall, fence, or a `SmallItem`
   drop in the world) and brings back up to 5 units. Travel time proportional to distance
   (1 block/second walk speed). While restocking, the Runner carries the items visibly
   (rendered via `SmallItemRenderer` attached to the NPC model).
2. **Delivery run**: The player can manually queue a delivery from the Shop Management UI:
   pick a target NPC by name (any NPC with a known `home` position from `NPCManager`) and a
   set of items. The Runner delivers the package to the NPC's doorstep. Successful delivery
   earns the listed price + a **delivery premium** of 2 coins per item (black-market home
   delivery). Notoriety +3 per delivery (dodgy dealing on the street).
3. **Runner heat**: Each delivery run increases `heatLevel` by 5. If the Runner is spotted
   by a Police NPC during a delivery, `heatLevel` +20 and a `WitnessSystem` event fires
   (as if the player committed the crime). The Runner does NOT fight police — they drop the
   goods and run home.

---

### Heat Level & Police Raids

The `ShopUnit.heatLevel` (0–100) represents how much police/council attention the shop is
attracting. It increases from:

| Event | Heat change |
|-------|------------|
| Runner delivery spotted by police | +20 |
| Player sells a `Material` that is on the Fence's "hot list" (stolen goods) | +10/sale |
| Newspaper mentions the shop | +5 |
| Neighbourhood Watch patrols past the shop | +3/pass |
| Day passes without incident | −5/day (decay) |
| Shop closed for business | −10/day (going dark) |

**Heat thresholds**:

| Heat | Consequence |
|------|------------|
| 0–29 | Safe — no attention |
| 30–59 | **Council Notice**: a `CONDEMNED_NOTICE`-style `INSPECTION_NOTICE` prop appears; +1 offence to criminal record if player doesn't close shop within 1 in-game day |
| 60–79 | **Police Stakeout**: 1 plainclothes `PUBLIC` NPC (actually `NPCType.UNDERCOVER_POLICE`) loiters within 15 blocks, following the Runner on deliveries |
| 80–99 | **Raid Warning**: a rumour *"Pigs are planning a raid on that shop"* seeds into 5 NPCs via the barman; player has 1 in-game hour to close or move stock |
| 100 | **Police Raid**: 3 `POLICE` NPCs arrive, confiscate all `stockMap` contents (removed from the world), issue a `CriminalRecord` offence, and reset `heatLevel` to 0. Shop sign is removed (shop becomes unclaimed). Notoriety +25. |

New `NPCType` entry: `UNDERCOVER_POLICE`.

---

### Competition: Marchetti's Off-Licence

The off-licence (controlled by Marchetti Crew) is the player's primary commercial rival.
It sells a fixed set of items at fixed prices. If the player's shop undercuts these prices:

- After 2 in-game days of undercutting: Marchetti Respect −10/day (cumulative).
- After Marchetti Respect drops to 35: a **Marchetti enforcer NPC** (`NPCType.FACTION_LIEUTENANT`)
  visits the shop and delivers a warning via speech bubble: *"Nice little shop you've got here.
  Be a shame if something happened to it."*
- If the player continues undercutting (Marchetti Respect < 25): the enforcer returns and
  smashes 5 random blocks in the shop building (block HP reduced to 0), reducing the building's
  `Condition` by 15. Notoriety +10. A rumour seeds: *"Marchetti boys have wrecked that corner shop."*
- The player can pre-empt this by paying the **Protection Racket**: press **E** on the enforcer
  and select *"I'll pay the going rate."* Costs 10 coins/in-game day (deducted from cashRegister).
  Marchetti Respect recovers at +3/day while protection is paid.

---

### Street Lads & Council Reactions

**Street Lads** love the shop if it undercuts Marchetti and stocks items they want
(`Material.CIDER`, `Material.TOBACCO`, `Material.ENERGY_DRINK` — new Materials):
- Selling these three items at fair price gives Street Lads Respect +2/day.
- Selling them at undercut price gives +5/day and seeds: *"That shop's got us sorted, proper."*
- Street Lads will **defend the shop** against Marchetti enforcers if Street Lads Respect ≥ 70:
  they intercept any approaching enforcer NPC and fight them (existing combat system) before
  they reach the shop.

**The Council** tolerates the shop at Heat < 30 but dislikes prosperity it didn't sanction:
- When `dailyRevenue` exceeds 50 coins in a day: The Council issues a `BUSINESS_RATES_NOTICE`
  prop on the exterior wall (cosmetic prop, new `PropType`). If not torn down within 1 day,
  it adds 1 Council offence to the criminal record. Tearing it down: Council Respect −5,
  Street Lads Respect +3.
- Council Victory (from Phase 8d) now also commissions a **Licensed Trader NPC** who sets up a
  competing stall within 10 blocks of the player's shop, selling the same items at −20% cost
  (subsidised). The Licensed Trader cannot be robbed or attacked without triggering a police raid.

New Materials: `CIDER`, `TOBACCO`, `ENERGY_DRINK`.
New PropTypes: `SHOP_SIGN`, `INSPECTION_NOTICE`, `BUSINESS_RATES_NOTICE`.

---

### Shop Management UI (Key M)

The new **Shop Management UI** (toggled with **M**, only accessible while inside the claimed
shop) is a 2-panel overlay rendered via `SpriteBatch`:

- **Left panel — Stock**: Grid of slots showing each stocked Material, quantity, and current
  sell price. Player can click a slot to adjust the price (increment/decrement with arrow keys,
  confirm with Enter). Transfer items from personal inventory by dragging (or pressing T to
  transfer selected hotbar item).
- **Right panel — Ledger**: Shows `dailyRevenue`, `cashRegister` balance, Runner status
  (idle / on run / hired), Heat level bar (colour-coded: green/amber/red), and a 3-day
  revenue history.
- **Bottom bar**: Buttons for *"Collect cash"* (transfers `cashRegister` to player inventory),
  *"Hire runner"* / *"Fire runner"*, *"Open / Close shop"*, *"Pay protection"* (if enforcer
  has visited).

**Key binding**: **M** — Shop Management (added to Help UI).

---

### Integration with Existing Systems

- **StreetSkillSystem** HUSTLE Tier 3 perk *"Sales Patter"*: customer purchase chance +10%
  and price tolerance +20% (customers will pay overpriced items more readily).
- **BootSaleSystem**: Items unsold after 2 in-game days can be bulk-transferred to the next
  boot sale auction with one press (T from Shop Management UI → Boot Sale queue).
- **NewspaperSystem**: When `dailyRevenue` exceeds 30 coins in a day, the Ragamuffin may
  run a headline: *"Mystery Shop Shakes Up Local Economy"* or *"Who Is The Corner Shop Kingpin?"*
- **PirateRadioSystem**: Broadcasting from inside the shop building produces a 1-day +10%
  customer traffic boost ("the radio ad effect") and seeds a `SHOP_NEWS` rumour in all
  NPCs within earshot.
- **AchievementSystem**: New achievements:

| Achievement | Trigger |
|-------------|---------|
| `OPEN_FOR_BUSINESS` | Open a shop for the first time |
| `KERCHING` | Earn 100 coins in a single in-game day from shop sales |
| `PROTECTION_MONEY` | Pay the Marchetti protection racket 3 times |
| `THE_NEIGHBOURHOOD_SHOP` | Keep a shop open for 7 in-game days without a raid |
| `RAIDED` | Survive a police raid (shop reopened within 1 day after raid) |
| `PRICE_WAR` | Undercut Marchetti for 3 consecutive in-game days |

---

**Unit tests**: `ShopUnit` stock management, price-to-traffic calculation, heat level accumulation
and threshold transitions, Runner pathing target selection, wage deduction logic, Marchetti
undercut detection, customer NPC purchase probability, cash register collect, raid confiscation,
all new achievement triggers.

**Integration tests — implement these exact scenarios:**

1. **Claim a derelict shop**: Generate the world. Find a charity shop building with Condition ≤ 49.
   Place the player at its front door. Press **E**. Verify a `ShopUnit` is created for that
   building. Verify a `SHOP_SIGN` prop appears on the exterior wall. Verify the Shop Management
   UI opens (key M accessible). Verify `openForBusiness` is false (not yet stocked).

2. **Stock and open the shop, customer buys an item**: Create a `ShopUnit`. Transfer 5 units of
   `Material.SAUSAGE_ROLL` from player inventory into `stockMap` via Shop Management UI. Set
   price to 3 coins. Set `openForBusiness = true`. Spawn a `PUBLIC` NPC 6 blocks from the shop.
   Advance simulation by 60 in-game seconds (enough for ≥1 purchase attempt at 15%/min chance —
   seed RNG for determinism). Verify `stockMap` contains 4 SAUSAGE_ROLL (1 sold). Verify
   `cashRegister` increased by 3. Verify the NPC has a `SHOP_NEWS` rumour in their buffer.

3. **Underpriced goods boost traffic and anger Marchetti**: Give the shop 10 units of an item
   with base fence value 10 coins. Set price to 7 coins (70% of base — undercut). Advance 2
   in-game days. Verify the effective customer-purchase chance for that item is 45% (15% base
   + 30% undercut bonus). Verify Marchetti Respect has decreased by at least 10 over the 2 days.

4. **Runner restocks empty slot**: Hire a Runner NPC. Set `stockMap[SAUSAGE_ROLL] = 0`. Verify
   the Runner pathfinds toward the nearest SAUSAGE_ROLL source (a boot sale stall or item drop
   seeded in the world). Advance sufficient frames for the Runner to reach the source and return.
   Verify `stockMap[SAUSAGE_ROLL]` is now ≥ 1.

5. **Runner delivery earns premium**: Queue a delivery via Shop Management UI: 3 units of
   `ENERGY_DRINK` at 5 coins each to an NPC named "Dave" at position (50, 1, 50). Hire the
   Runner. Advance simulation until delivery completes. Verify `cashRegister` increased by
   (3 × 5) + (3 × 2) = 21 coins. Verify `stockMap[ENERGY_DRINK]` decreased by 3. Verify
   Notoriety increased by 9 (+3 per item delivered).

6. **Police raid at Heat 100**: Set `heatLevel` to 99. Trigger one "hot goods" sale
   (+10 heat → total 109, clamped to 100). Verify `heatLevel == 100`. Advance 1 frame.
   Verify 3 `POLICE` NPC spawned near the shop. Advance until they reach the shop. Verify
   all items in `stockMap` are removed (confiscated). Verify `SHOP_SIGN` prop removed.
   Verify `openForBusiness == false`. Verify player CriminalRecord gained 1 offence.
   Verify Notoriety increased by 25.

7. **Marchetti enforcer visits after 2 days of undercutting**: Set Marchetti Respect to 36.
   Undercut Marchetti's off-licence price for 2 in-game days (advance simulation). Verify
   Marchetti Respect has dropped below 35. Verify a `FACTION_LIEUTENANT` NPC has been spawned
   and pathfound to within 3 blocks of the shop. Verify the NPC's speech text contains
   *"Be a shame if something happened to it."*

8. **Street Lads defend shop against enforcer**: Set Street Lads Respect to 72. Stock the shop
   with `CIDER` at fair price. Set Marchetti Respect to 24 (enforcer about to attack). Spawn a
   Marchetti enforcer NPC approaching the shop. Advance simulation. Verify at least 1 Street Lads
   NPC intercepts the enforcer (moves between enforcer and shop) before the enforcer reaches the
   shop. Verify the enforcer enters `COMBAT` state against the Street Lad (not the shop).

9. **Council issues business rates notice above 50 coin daily revenue**: Set `dailyRevenue` to 55.
   Advance 1 in-game day tick. Verify a `BUSINESS_RATES_NOTICE` prop appears on the shop exterior.
   Player presses **E** on the notice to tear it down. Verify prop removed. Verify Council Respect
   −5. Verify Street Lads Respect +3.

10. **Full shop lifecycle stress test**: Start a new game. Claim a derelict shop. Stock it with
    5 different materials. Set fair prices. Open for business. Hire a Runner. Advance 3 in-game
    days — verify daily revenue accumulates, Runner completes at least 1 restock run, at least
    3 NPC customers make purchases, `SHOP_NEWS` rumours exist in the RumourNetwork. Then
    trigger a Marchetti confrontation (drop Machetti Respect to 24). Trigger a police raid
    (set Heat to 100). Verify raid fires correctly, shop is closed, items confiscated.
    Immediately re-open the shop (re-claim building, re-stock). Verify the `RAIDED` achievement
    fires. Throughout: verify game remains in PLAYING state, no NPEs, all HUD elements render,
    NPC count non-zero, Shop Management UI opens/closes without error.

---

## Phase 17: The Underground Fight Night — Bare-Knuckle Boxing, Faction Betting & Championship Ladder

**Goal**: A clandestine fight promotion system centred on an unmarked basement venue (the
"Pit") beneath the industrial estate. The player can enter bouts, promote their own fight
cards, place bets, and climb a shadowy championship ladder whose standings ripple through
faction respect, street reputation, and the rumour network. The whole spectacle is promoted
via `PirateRadioSystem` and reported in `NewspaperSystem`.

### The Pit Venue

- A new `LandmarkType.THE_PIT` generated beneath the industrial estate: a rectangular basement
  room (12×8×4 blocks) of STONE walls and DIRT floor, lit by dangling LANTERN props, with a
  central 6×6 fighting ring marked by rope-prop blocks.
- Entry: a concealed WOOD trapdoor at ground level. Player must press **E** on the trapdoor
  while Notoriety ≥ 10 **or** carrying a `Material.FIGHT_CARD` flyer — otherwise the BOUNCER
  NPC at the door says *"Members only, pal."*
- Inside: a crowd of 8–16 spectator NPCs (mix of WORKER, YOUTH, PENSIONER types) arranged
  around the ring; a `BOOKIE_NPC` against the east wall; and the current two fighters in the ring.

### Fight Card & Scheduling

- Fights occur on a schedule: every 3 in-game days a new `FightCard` is generated, seeded with
  two FIGHTER NPCs drawn from the neighbourhood (names like "Mad Terry", "The Postman", "Gaz Two
  Fingers"). Each fighter has hidden stats: `strength` (1–10), `stamina` (1–10), `dirty` (boolean).
- The player can enter their own name into a fight slot by pressing **E** on the chalkboard
  prop inside the Pit. This costs 0 coins but requires Notoriety ≥ 15.
- The player can also **promote** a fight card: pay 20 coins to the BOOKIE_NPC, which triggers
  a `PirateRadioSystem` broadcast ("Fight night at the Pit — be there or be square") and
  spawns 4 extra spectators. Promoting boosts the prize pot by 50%.

### Combat Mechanics (Player vs Fighter NPC)

- When the fight starts, the player and opponent enter a specialised combat mode inside the ring:
  - **Left click**: jab (1 stamina damage to opponent, low stagger)
  - **Shift + Left click**: haymaker (3 stamina damage, high stagger, −2 player stamina)
  - **Space**: dodge (50% chance to avoid the opponent's next swing, costs 1 player stamina)
- Each fighter has a `stamina` pool (10–30 HP scaled from NPC stat). Reaching 0 = knockout.
- The opponent AI cycles between JABS, HAYMAKERS (telegraphed with a 0.5s wind-up animation
  flag visible as the NPC model raising an arm), and CLINCH (closes distance fast).
- Round limit: 3 rounds of 30 real-seconds each. If both fighters are still standing, the
  fighter with more remaining stamina wins on points.
- `dirty` fighters may attempt an eye-gouge (−5 instant stamina, triggers `WitnessSystem` crowd
  grumble) — detectable by the player as a 0.25s glint particle before the attack lands.

### Betting Economy

- `BookieNPC` offers odds on each fight using a simple implied-probability model based on hidden
  fighter stats (player cannot see stats directly — must infer from rumours or observation).
- The player bets with `Material.COIN`. Min bet: 2 coins. Max bet: 50 coins.
- Winning bets pay at the offered odds. Losing bets go to the bookie's pot.
- The bookie's pot is finite (`bookiePot`, starting at 100 coins). If the player drains the pot
  below 20 coins, the bookie refuses further bets until the next fight card.
- `FenceSystem` integration: the player can get inside odds from the fence (press E on Fence
  NPC, costs 5 coins) revealing one fighter's true `strength` value.
- `StreetSkillSystem` HUSTLE perk at level 3: the player can spot the `dirty` flag from the
  fighter's idle animation without paying the fence.

### Championship Ladder

- A persistent ranked list of 8 fighters (`ChampionshipLadder`). The top-ranked fighter holds
  the `Material.CHAMPIONSHIP_BELT` prop item and receives a passive +5 Notoriety/day.
- After each fight, winner climbs one rung, loser drops one rung.
- Reaching rank 1 grants the player `AchievementType.CHAMPION_OF_THE_PIT` and causes rival
  factions to treat the player with elevated respect (+10 all factions).
- Marchetti Crew fields their own fighter ("Vinnie"). If Vinnie reaches rank 1 before the
  player, Marchetti Respect boost drops by 15 (they don't need you anymore).
- The Street Lads will cheer the player's fights if Street Lads Respect ≥ 60, giving the
  player a crowd-noise buff: +2 stamina at the start of each round.

### Promotion & Narrative Integration

- Every fight result is published as a `NewspaperSystem` headline: *"Mad Terry KO'd in
  three — Pit punters gutted"* or *"Local hero floors Marchetti's man in shock upset"*.
- The `RumourNetwork` seeds `RumourType.GANG_ACTIVITY` rumours describing the ladder standings
  so NPCs discuss the fights in dialogue.
- `PirateRadioSystem` DJ commentary: after a fight the pirate DJ delivers a 3-line monologue
  referencing the winner and loser by name (stored in `PirateRadioSystem.fightCommentary` list).
- Police awareness: each fight event adds +5 `NoiseSystem` noise in the industrial estate zone.
  After 3 fight nights, a plain-clothes POLICE NPC appears in the crowd — detectable by
  `DisguiseSystem` check (identical mechanic to undercover shop surveillance). If not spotted
  and removed, the next event triggers a raid: all spectators scatter, Pit sealed with STONE
  blocks for 2 in-game days.

### New Materials & Props

| Material / Prop | Description |
|---|---|
| `Material.FIGHT_CARD` | Paper flyer granting Pit entry; dropped by YOUTH NPCs near the estate |
| `Material.CHAMPIONSHIP_BELT` | Held by ladder rank-1 fighter; wearable cosmetic (+5 Notoriety/day) |
| `Material.MOUTH_GUARD` | Reduces stamina loss from opponent hits by 25%; craftable from RUBBER |
| `PropType.BOOKIE_BOARD` | Chalkboard showing current odds and ladder standings |
| `PropType.FIGHT_RING_ROPE` | Rope boundary prop forming the ring perimeter |
| `PropType.LANTERN` | Atmospheric lighting prop for Pit interior |

### New `FightNightSystem` class

Implements all logic above. Key public API:

```
FightNightSystem(World, NPCManager, PirateRadioSystem, NewspaperSystem,
                 RumourNetwork, FactionSystem, StreetSkillSystem,
                 NoiseSystem, WitnessSystem, DisguiseSystem)
generateFightCard()                        // called every 3 in-game days
enterPlayerIntoFight(Player)               // registers player as a combatant
promoteFightCard(Player, Inventory)        // pays 20 coins, triggers radio broadcast
startFight(FightCard)                      // begins the combat phase
placeBet(Player, Inventory, int coins, int fighterIndex)
resolveRound(Fighter a, Fighter b)         // returns winner or DRAW
knockOut(Fighter loser)                    // removes from ring, updates ladder
updateLadder(Fighter winner, Fighter loser)
getOdds(int fighterIndex) : float          // implied probability
checkUndercoverPolice(FightCard)           // DetectableConcreteCheck
```

### Achievements

| Achievement | Trigger |
|---|---|
| `FIRST_BLOOD` | Win your first fight in the Pit |
| `CHAMPION_OF_THE_PIT` | Reach rank 1 on the Championship Ladder |
| `DIRTY_FIGHTER` | Win a fight after landing an eye-gouge |
| `CLEANED_OUT_THE_BOOKIE` | Drain the bookie's pot below 20 coins in a single fight night |
| `PROMOTED` | Successfully promote a fight card with the PirateRadio broadcast |
| `UNDERCOVER_SPOTTER` | Identify and expose the plain-clothes police officer in the crowd |

---

**Unit tests**: `ChampionshipLadder` rank ordering, odds calculation, stamina damage model,
round timer logic, bookie pot depletion, police detection threshold, fight card scheduling
interval, `FIGHT_CARD` flyer drop probability from YOUTH NPCs, crowd-buff application when
Street Lads Respect ≥ 60, Marchetti Vinnie rank impact on faction respect delta.

**Integration tests — implement these exact scenarios:**

1. **Pit is accessible at Notoriety ≥ 10**: Generate the world. Find `THE_PIT` trapdoor
   position. Set player Notoriety to 9. Press **E** on the trapdoor. Verify BOUNCER NPC
   speech text contains *"Members only, pal."* and player position has not changed (not
   entered). Set Notoriety to 10. Press **E** again. Verify player Y coordinate drops by 4
   blocks (descended into the basement). Verify spectator NPC count inside Pit room ≥ 8.

2. **Fight card generates fighters and odds**: Call `generateFightCard()`. Verify the returned
   `FightCard` contains exactly 2 fighters, each with `strength` in [1,10], `stamina` in [1,10],
   and a non-null name string. Call `getOdds(0)` and `getOdds(1)`. Verify odds are in (0, 1)
   and sum to ≤ 1.05 (allowing for bookie margin). Verify odds correlate with stats: the fighter
   with higher combined `strength + stamina` has odds < 0.5 (i.e., is the favourite).

3. **Player wins fight, ladder updates**: Enter player into a fight against a Fighter NPC with
   `strength = 2, stamina = 10`. Simulate the fight: player lands 4 haymakers (4×3 = 12 stamina
   damage > 10 = knockout). Verify opponent `stamina ≤ 0`. Verify `knockOut()` was called.
   Verify player has moved up at least 1 rung on the `ChampionshipLadder`. Verify a
   `NewspaperSystem` headline was generated containing the player's name.

4. **Bet placed and paid out correctly on win**: Set `bookiePot = 100`. Player bets 10 coins
   on fighter index 0 at odds 2.0 (implied 50%). Fighter 0 wins the bout. Verify player
   inventory gained 20 coins (10 stake × 2.0 odds). Verify `bookiePot` reduced by 10
   (net payout from pot). Verify player lost 10 coins from inventory before the fight started.

5. **Undercover police triggers raid after 3 fight nights**: Run 3 consecutive `generateFightCard()`
   and `startFight()` cycles without the player using `DisguiseSystem` to identify the
   plain-clothes officer. After the 3rd fight, verify: a plain-clothes `POLICE` NPC was
   spawned inside the Pit. Verify the Pit trapdoor prop is replaced by a STONE block
   (sealed). Verify no `FightCard` can be generated for the next 2 in-game days
   (`FightNightSystem.pitSealedUntil > currentTime`).

6. **Street Lads crowd buff applies**: Set Street Lads Respect to 65. Enter player into a
   fight. At the start of round 2, verify player's current `stamina` equals their stamina at
   end of round 1 **plus 2** (crowd buff applied). Verify this buff fires exactly once per round
   start, not once per frame.

7. **Pirate radio broadcasts fight card promotion**: Player calls `promoteFightCard()` with
   20 coins in inventory. Verify player inventory decreased by 20 coins. Verify
   `PirateRadioSystem.getScheduledBroadcasts()` contains an entry with text containing *"Fight
   night at the Pit"*. Verify 4 additional spectator NPCs are spawned inside the Pit room.
   Verify the prize pot for that `FightCard` increased by 50%.

8. **Full fight night stress test**: Generate the world. Player enters the Pit (Notoriety = 15).
   Generate a fight card. Promote it (pay 20 coins). Observe the two NPC fighters spar for
   1 round (advance 30 real-seconds). Player enters next fight card. Place a bet (5 coins on
   themselves). Fight 3 rounds to victory. Verify ladder updated. Verify newspaper headline
   generated. Verify pirate radio commentary scheduled. Verify `bookiePot` debited correctly.
   Verify game remains in PLAYING state throughout, no NPEs, NPC count non-zero, Pit spectator
   crowd count within [8, 20].

---

## Phase 22: The Snitch Network — Informants, Paranoia & The Long Con

**Goal**: Introduce a living criminal underworld where trust is currency and betrayal is
inevitable. Every NPC in the world has a hidden loyalty and an informant score. Some are
grasses on retainer for the police. Others can be turned. The player must build a network
of trusted contacts — or burn them all — while never knowing for certain who's talking to
who. This system weaves paranoia into every interaction and turns the existing rumour,
faction, and wanted systems into a single, terrifying whole.

This is Ragamuffin's answer to GTA's wanted system: not just stars and police cars, but a
living web of informants, cover stories, and betrayal — with dark British humour throughout.

### The Informant System

Every `NPC` instance gains two hidden fields:
- `informantScore` (0–100, default varies by type): how willing this NPC is to grass on the
  player to the police. Computed at world-gen time.
- `groomedBy` (`Faction` or null): which faction (including POLICE) has recruited this NPC
  as an informant. Null means civilian.

**Base informant scores by NPC type:**

| NPC Type | Base Score | Reasoning |
|----------|-----------|-----------|
| `PUBLIC` | 15–30 | Mostly decent, but some are curtain-twitchers |
| `YOUTH` | 5–15 | Gang code: snitches get stitches |
| `STREET_LAD` | 5–10 | Extremely loyal to street code |
| `SHOPKEEPER` | 40–60 | Pays their taxes, cooperates with authorities |
| `BOUNCER` | 20–35 | They've seen things but stay quiet (mostly) |
| `BARMAN` | 10–20 | Hears everything, says nothing (professionally) |
| `COUNCIL_BUILDER` | 70–90 | Works for the council, obviously grasses |
| `POLICE` | N/A | Already police |
| `ACCOMPLICE` | 0–5 | You recruited them — starts loyal |

**Score modifiers** applied at runtime:
- Player's Notoriety × 0.2 added to all `PUBLIC` and `SHOPKEEPER` scores (the higher your
  notoriety, the more nervous civilians become around you)
- Player has bought that NPC a drink (barman trade): −10 to that NPC's score
- Player has completed a faction mission that benefited that NPC's faction: −15
- Player has been arrested within 50 blocks of that NPC in the last 5 in-game minutes: +20
  ("they saw what you did")

### Grassing Mechanic

When a player commits a crime (block break in a building, pickpocket, assault, heist),
all NPCs within 20 blocks who have line-of-sight are added to a **WitnessPool** for that
crime (a transient list, existing `WitnessSystem` extended). At the next `CLOSE_OF_BUSINESS`
TimeSystem event (midnight each in-game day), the `SnitchNetwork` resolves:

1. For each NPC in any active WitnessPool, roll: `random(0, 100) < informantScore`.
2. On success: that NPC "reports" — adding 1 `CriminalRecord` offence, generating a
   `POLICE_TIP` rumour type (new), and seeding a Notoriety +10 penalty.
3. The player is not immediately notified — they find out when the next police patrol
   encounter has clearly been "briefed" (police speech bubble: "We've had reports about
   you, sunshine."), or when they read the next `NewspaperSystem` headline.
4. Each NPC can only successfully grass once per crime event (no double-reporting).

**New `RumourType`:** `POLICE_TIP` — template: "Word is the filth got a tip-off about
[player name] near [landmark]." Spreads through the rumour network like any other rumour.

### The Grooming System — Turning NPCs

The player can work to reduce an NPC's informant score or flip them to actively work
against the police. New interaction mechanic via **E** key on any non-hostile NPC:

**Stage 1 — Build Trust** (reduces informant score):
- Buy them a drink (costs 2 coins via nearby barman, or player carries BEER material):
  informantScore −10. Speech: "Cheers, mate. You're alright, you are."
- Complete a favour for them — if the NPC is a `SHOPKEEPER`, chase away a harassing
  `YOUTH` (attack a YOUTH within 15 blocks of the shop): informantScore −15.
  Speech: "Thanks. Those kids are a bloody nightmare."
- Gift them FOOD material (SAUSAGE_ROLL, STEAK_BAKE, etc.): informantScore −5.

**Stage 2 — Recruit as Asset** (once score drops below 20):
- Press **E** and select "I need a favour" dialogue option (costs 5 coins).
- NPC becomes a **CONTACT** — their `groomedBy` is set to null (no longer grassing).
- Contact perk activates: any time the police would receive a tip from this NPC's area,
  there is a 60% chance the contact "loses" the information. The player sees a tooltip:
  "[NPC name] covered for you."

**Stage 3 — Double Agent** (requires score at 0 AND Street Lads or Marchetti Respect ≥ 70):
- Pay 15 coins. NPC becomes a `DOUBLE_AGENT` CONTACT.
- A Double Agent actively feeds *false* `POLICE_TIP` rumours back into the `RumourNetwork`
  that reference a different location, diverting police patrols for 3 in-game minutes.
  This can be triggered manually: press **E** on the Double Agent and select
  "Set them up" (costs 5 coins per use, 10-minute cooldown).
- On use: 2 `POLICE` NPCs are diverted to a false landmark for 3 minutes. Speech bubble
  from the diverted police: "Anonymous tip. Let's check it out."

### The Paranoia Mechanic — Who Can You Trust?

The player never sees raw `informantScore` values. Instead, tension is communicated
through:

- **Behaviour tells** (visual/audio cues when informantScore > 50):
  - NPC glances toward the nearest police NPC when the player is nearby (a brief
    head-rotation animation toward the police direction, then back).
  - Speech bubble probability triggers: "Busy round here, isn't it" (neutral deflection).
  - NPC subtly moves away if the player stands within 3 blocks for more than 5 seconds.
- **Newspaper confirmation**: after a successful grass, the `NewspaperSystem` generates
  a story: "Local Snitch Helps Police Nab Ragamuffin Suspect". The player sees this as
  a headline — but NOT which NPC grassed.
- **Barman intel**: After buying a Double Whisky at the pub, the barman will (if he
  holds a `POLICE_TIP` rumour) hint: "Word is someone near [landmark] had a chat with
  the filth recently." This narrows down the area but still doesn't name the NPC.
- **Rumour fishing**: With a drink active, pressing **E** on any NPC who has grassed
  in the last 24 in-game hours has a 30% chance they say: "No comment" instead of
  normal dialogue — the player learns *someone* in this area is suspicious, but not who.

**New Material**: `BEER` — craftable (2 CARDBOARD → 1 BEER — improvised homebrew) or
bought at the pub for 1 coin. Carrying 3+ BEER in inventory triggers police interest:
speech bubble "Got your cans out already?" — +1 offence if Wanted level > 0.

### The Informant Exposure Event

Once per in-game day, the `SnitchNetwork` randomly selects one confirmed informant NPC
(groomedBy == POLICE, informantScore > 60) and generates an **Exposure Roll**:

- `random(0, 100) < (Street Lads Respect − 30)` — Street Lads may unmask the grass
  independently. If triggered: 2 Street Lad NPCs approach the informant, speech bubble:
  "We know what you did." The informant's `NPCState` transitions to FLEEING. The
  player receives a QUEST_LEAD rumour: "The Lads are sorting out a grass near [area]."
  If the player assists (hits the fleeing informant before they reach a police NPC),
  Street Lads Respect +10, informantScore reset to 0 (they've been "dealt with").
- If the player ignores it: the informant reaches the police NPC and their score
  increases to 95 permanently ("they're on the payroll now").

### Safe House System

New building type: **SAFE_HOUSE**. The player can establish a safe house in any building
they have squatted (via existing `SquatSystem`). Press **E** on the door of a squatted
building to "set it as safe house" (one safe house at a time).

Benefits of a safe house:
- Police do not enter or raid a safe house unless the player's Notoriety ≥ 300 (Tier 3+).
  At Tier 3+, police gain the ability to raid — a `POLICE_RAID_INCOMING` event is seeded
  24 in-game minutes before arrival, visible as a RUMOUR: "Word is the filth are planning
  something round your way." Player has time to evacuate.
- Press **E** inside the safe house on a WALL block to access a hidden stash (a secondary
  `Inventory` of 9 slots). Stashed items do NOT appear in `CriminalRecord` searches.
- Sleeping inside a safe house (stand still for 10 seconds at night) skips to morning
  and fully restores energy. Tooltip on first use: "Home is where you hide your gear."
- Safe house can be compromised: if any Double Agent with a score reset to 0 is within
  50 blocks AND was previously a CONTACT of the player's, they may lead police to it
  on a `POLICE_TIP` grass roll. Tooltip: "Looks like your safe house isn't so safe anymore."

### New Achievements

| Achievement | Trigger |
|---|---|
| `FIRST_CONTACT` | Groom your first NPC into a Contact |
| `DOUBLE_AGENT` | Successfully use a Double Agent to divert police |
| `NOBODY_TALKS` | Reduce 5 NPCs' informant scores to 0 in a single playthrough |
| `TRUST_NO_ONE` | Lose a Safe House to a compromised Contact |
| `THE_LONG_CON` | Have the barman hold a `POLICE_TIP` rumour seeded by your own Double Agent (a tip about yourself, that you planted) |
| `CURTAIN_TWITCHER` | Be grassed on by a `PUBLIC` NPC with informantScore > 80 |
| `CLEARED_OUT` | Successfully survive a police raid on your Safe House |

### New Key Binding

**T** — Talk (extended interaction): opens the grooming/favour dialogue on a nearby NPC.
Distinct from **E** (quick interaction). Added to Help UI and key reference.

### New Classes Required

- `SnitchNetwork` — manages `WitnessPool` per crime, resolves grassing at close-of-business,
  tracks which NPCs are CONTACTS or DOUBLE_AGENTS
- `SafeHouseSystem` — manages safe house registration, stash inventory, raid countdown,
  police entry exemption
- `SnitchBehaviour` (inner class or extension of NPC) — handles behaviour tells, glance
  animation trigger, movement-away logic

### Integration with Existing Systems

- **WitnessSystem**: `SnitchNetwork` subscribes to witness events from `WitnessSystem`.
  The existing witness detection is extended: witnesses now also add themselves to the
  `SnitchNetwork`'s WitnessPool.
- **RumourNetwork**: `POLICE_TIP` is a new `RumourType` seeded by `SnitchNetwork` and
  spread identically to existing rumours (5-hop expiry, barman as sink).
- **NewspaperSystem**: Successful grass events generate headlines in the next edition.
- **FactionSystem**: Grooming a Council NPC costs −5 Council Respect per attempt
  (they notice you're interfering with their assets).
- **NotorietySystem**: Being grassed on adds Notoriety +10 per confirmed grass.
- **DisguiseSystem**: While wearing a disguise, all NPC informant score rolls are halved
  (they don't recognise you to grass). Disguise must be active at time of WitnessPool
  resolution (midnight roll), not just at crime time.
- **SquatSystem**: Safe house requires an active squat. Losing the squat (council
  demolition) simultaneously loses the safe house.
- **TimeSystem**: Grass resolution fires at `TIME_EVENT.CLOSE_OF_BUSINESS` (23:00).

**Unit tests**: Informant score initialisation by NPC type, modifier application, grass
roll probability, Contact recruitment cost/state change, Double Agent diversion targeting,
WitnessPool per-crime cap, safe house stash access, raid countdown seeding, behaviour
tell trigger threshold, newspaper headline generation on grass, rumour type POLICE_TIP
spread mechanics, disguise halving of grass roll, SquatSystem loss cascade to SafeHouse.

**Integration tests — implement these exact scenarios:**

1. **Grass roll fires at close-of-business**: Commit a crime (break a block inside a
   building). Verify a `PUBLIC` NPC within 20 blocks is added to the `WitnessPool`. Set
   that NPC's `informantScore` to 100. Advance `TimeSystem` to 23:00. Verify the
   `CriminalRecord` now has 1 additional offence. Verify a `POLICE_TIP` rumour exists in
   the `RumourNetwork`. Verify `Notoriety` has increased by 10.

2. **Low informant score NPC does NOT grass**: Commit the same crime. Add a witness NPC
   with `informantScore = 0`. Advance to 23:00. Verify `CriminalRecord` has NOT gained
   an additional offence from this NPC. Verify no `POLICE_TIP` rumour was seeded by this
   NPC (no new rumour with that NPC as source).

3. **Grooming reduces informant score**: Spawn a `SHOPKEEPER` NPC with `informantScore = 50`.
   Trigger the "Buy them a drink" interaction (player has 2 coins, calls groom action).
   Verify `informantScore` decreases to 40. Repeat 4 times. Verify score is now 10.
   Verify NPC dialogue contains "Cheers, mate."

4. **Contact recruitment prevents grass**: Groom a `PUBLIC` NPC to `informantScore < 20`.
   Pay 5 coins to recruit as CONTACT. Commit a crime within that NPC's view. Set their
   informantScore to 100 temporarily (to guarantee roll would succeed without contact
   status). Advance to 23:00. Verify the `CriminalRecord` has NOT received an additional
   offence (Contact's 60% block fired, or deterministically: mock the random to return
   0.3 < 0.6 to confirm block). Verify a tooltip "[NPC name] covered for you." was
   triggered.

5. **Double Agent diverts police patrol**: Groom an NPC to score 0, meet faction Respect
   requirement. Pay 15 coins to make them `DOUBLE_AGENT`. Trigger "Set them up" action.
   Verify 2 `POLICE` NPCs change their `NPCState` to a diverted patrol state, heading
   toward the false landmark. Advance 180 game-minutes. Verify the police NPCs have
   returned to normal patrol state (diversion expired).

6. **Behaviour tell triggers above threshold**: Spawn an NPC with `informantScore = 75`.
   Place player within 3 blocks. Advance 300 frames. Verify at least 1 glance animation
   event was fired toward the nearest police NPC direction. Verify the NPC moved at least
   1 block away from the player after 5 seconds.

7. **Safe house blocks police entry at Notoriety < 300**: Set player Notoriety to 150.
   Establish a safe house (squat a building, press E on door). Spawn a POLICE NPC
   directly outside. Advance 300 frames. Verify the POLICE NPC has NOT entered the
   safe house interior (player Y coordinate in building, police Y coordinate outside).

8. **Safe house raid countdown at Notoriety ≥ 300**: Set player Notoriety to 300.
   Establish a safe house. Advance `TimeSystem` by 1 in-game minute. Verify a
   `POLICE_RAID_INCOMING` rumour exists in the `RumourNetwork`. Advance 24 in-game
   minutes. Verify at least 2 POLICE NPCs have entered the safe house interior.

9. **Disguise halves grass roll**: Commit a crime. Set witness NPC `informantScore = 50`.
   Activate `DisguiseSystem` (player is in disguise). Advance to 23:00. With disguise,
   effective roll threshold is 25 — mock random to return 0.4 (40 > 25, no grass).
   Verify no offence added. Deactivate disguise. Same crime, same NPC score, mock random
   0.4 again (40 < 50, grass fires). Verify offence added.

10. **Full snitch network stress test**: Start a new game. Commit 5 crimes at different
    landmarks. Verify 5 different WitnessPools exist. Set all witness NPC scores to 80.
    Advance to 23:00. Verify 5 `POLICE_TIP` rumours exist (one per crime). Verify
    Notoriety increased by 50 total. Groom 2 NPCs to CONTACT. Commit 2 more crimes at
    those NPCs' locations. Advance to next 23:00. Verify only 3 new offences were added
    (not 5 — the 2 contacts blocked their grass). Verify Newspaper has a headline
    about police tips. Verify barman holds at least 1 `POLICE_TIP` rumour. Verify no
    NPEs, game remains in PLAYING state throughout.

---

## Wire Up WantedSystem & NotorietySystem into the Game Loop

**Goal**: Connect the two existing but entirely unwired police-response systems so that
criminal acts have real in-game consequences — wanted stars, police pursuit escalation,
and a persistent notoriety score that modifies police aggression.

### Problem

`WantedSystem` (Issue #771 — Hot Pursuit) and `NotorietySystem` (Phase 8e) are fully
implemented classes with `update()` methods, but neither is ever instantiated in
`RagamuffinGame.java`. As a result:

- No wanted stars are ever raised when the player commits crimes
- Police never chase the player regardless of what they do
- The `GameHUD` has a `setNotorietySystem()` hook and full notoriety-tier rendering code
  that is dead — it is never supplied a `NotorietySystem` instance, so the HUD notoriety
  cluster is never drawn
- `WantedSystem.update()` drives police NPC state transitions (CHASING, ALERTED, etc.)
  and spawns reinforcements — this logic is completely bypassed
- `NotorietySystem.update()` controls the helicopter sweep timer at Tier 3+ and tier-up
  flash animations — never called

### What needs doing

1. **Declare fields** in `RagamuffinGame`:
   - `private WantedSystem wantedSystem;`
   - `private NotorietySystem notorietySystem;`

2. **Instantiate in `initGame()`** after the NPC manager and crime-related systems are set up:
   ```java
   wantedSystem = new WantedSystem();
   notorietySystem = new NotorietySystem();
   gameHUD.setNotorietySystem(notorietySystem);
   ```

3. **Call `update()` every frame** inside `updatePlayingSimulation()` (and the PAUSED /
   CINEMATIC ticks that update live systems):
   ```java
   wantedSystem.update(delta, player, npcManager.getNPCs(),
       weatherSystem.getCurrentWeather(), timeSystem.isNight(),
       /* isRaveActive */ false, achievementSystem);
   notorietySystem.update(delta, player, achievementSystem);
   ```

4. **Hook criminal actions** — wherever the player breaks blocks, hits NPCs, or commits
   crimes, call `wantedSystem.reportCrime(...)` and `notorietySystem.addNotoriety(...)`
   so the systems actually accumulate state.

5. **Pass `WantedSystem` to `ArrestSystem`** so arrests correctly reduce wanted stars and
   notoriety on booking (currently `ArrestSystem.arrest()` has no reference to either).

6. **Ensure the `RumourNetwork` receives `POLICE_TIP` rumours** seeded by `WantedSystem`
   when witnesses report crimes (the link between `WantedSystem` and `RumourNetwork` is
   currently severed because neither is wired into the game).

**Unit tests**: `WantedSystem` star escalation per crime type, decay timer resets on LOS
contact, `NotorietySystem` tier thresholds, arrest reduces both systems, notoriety HUD
renders correct tier title.

**Integration tests — implement these exact scenarios:**

1. **Breaking a block raises wanted stars**: Place player adjacent to a BRICK block
   (inside a building). Simulate a punch action. Verify `wantedSystem.getWantedStars()`
   increases from 0 to at least 1. Verify at least one nearby NPC transitions to
   `NPCState.ALERTED`.

2. **Wanted star decay over time**: Set `wantedSystem` to 1 star manually. Remove all
   police NPCs from NPC list. Advance 95 simulated seconds (beyond the 90-second decay
   threshold). Verify `wantedSystem.getWantedStars()` has dropped to 0.

3. **Notoriety HUD renders after wiring**: Call `gameHUD.setNotorietySystem(notorietySystem)`.
   Set notoriety score to 250 (Tier 2). Render the HUD. Verify the notoriety section
   draws the correct tier title "Neighbourhood Villain" in the rendered output (inspect
   the SpriteBatch draw calls or check `notorietySystem.getTierTitle()`).

4. **Arrest clears wanted stars**: Set `wantedSystem` to 3 stars. Trigger
   `arrestSystem.arrest(player, inventory)`. Verify `wantedSystem.getWantedStars()` is 0
   after arrest. Verify `notorietySystem.getNotoriety()` decreased by the expected
   per-arrest penalty.

5. **Rave doubles wanted escalation**: Activate a rave (set `isRaveActive = true`). Call
   `wantedSystem.reportCrime(...)`. Verify stars escalate at double speed compared to the
   non-rave baseline (use `wantedSystem.getRaveAlertSpeedMultiplier()` to confirm the
   multiplier is > 1).

---

## Wire WarmthSystem & NoiseSystem into the Game Loop

**Goal**: Connect two fully-implemented survival systems — `WarmthSystem` (hypothermia /
wetness) and `NoiseSystem` (player noise level for NPC hearing detection) — into
`RagamuffinGame`. Both systems are complete dead code: the HUD already renders warmth and
wetness bars (always stuck at 100% / 0%), and `NPCManager` already reads
`player.getNoiseLevel()` for police hearing detection (always returns the default 0.05).

### WarmthSystem wiring

1. **Declare field** in `RagamuffinGame`:
   ```java
   private WarmthSystem warmthSystem;
   ```

2. **Instantiate in `initGame()`** after `weatherSystem` is created:
   ```java
   warmthSystem = new WarmthSystem();
   ```

3. **Call `update()` every frame** inside `updatePlayingSimulation()` (and any live-tick
   code paths during CINEMATIC / non-paused states):
   ```java
   boolean nearCampfire = campfireSystem != null &&
       campfireSystem.isNearActiveCampfire(player.getPosition(), WarmthSystem.CAMPFIRE_WARMTH_RADIUS);
   warmthSystem.update(player, weatherSystem.getCurrentWeather(), world,
       delta, nearCampfire, inventory);
   ```

4. **Wire the Flask of Tea drink action** — when the player uses a `FLASK_OF_TEA` item,
   call `warmthSystem.drinkFlaskOfTea(player, inventory)` so the warmth restore actually
   fires.

5. **Apply warmth speed penalty** — when computing player movement speed, multiply by
   `player.getWarmthSpeedMultiplier()` so hypothermia visibly slows the player.

### NoiseSystem wiring

1. **Declare field** in `RagamuffinGame`:
   ```java
   private NoiseSystem noiseSystem;
   ```

2. **Instantiate in `initGame()`**:
   ```java
   noiseSystem = new NoiseSystem();
   ```

3. **Call `update()` every frame** and mirror the result onto the player:
   ```java
   boolean isMoving = player.isMoving();
   boolean isCrouching = player.isCrouching();
   noiseSystem.update(delta, isMoving, isCrouching);
   player.setNoiseLevel(noiseSystem.getNoiseLevel());
   ```

4. **Spike on block-break** — wherever `blockBreaker` removes a block, call
   `noiseSystem.spikeBlockBreak()`.

5. **Spike on block-place** — wherever `blockPlacer` places a block, call
   `noiseSystem.spikeBlockPlace()`.

**Unit tests**: `WarmthSystem` drains warmth outdoors in rain, restores near campfire,
`WarmthSystem` applies coat / umbrella modifiers correctly, `NoiseSystem` baseline
matches movement state, block-break spike decays over correct duration, player noise
level updated via `setNoiseLevel`.

**Integration tests — implement these exact scenarios:**

1. **Warmth drains in rain outdoors**: Set weather to `Weather.OVERCAST` (raining). Place
   player outdoors (no roof blocks above). Record `player.getWarmth()` (starts at 100).
   Advance 120 simulated seconds via `warmthSystem.update()`. Verify
   `player.getWarmth()` has decreased from 100.

2. **Hypothermia causes damage**: Set `player.setWarmth(10f)` (below
   `WARMTH_DANGER_THRESHOLD = 20`). Call `warmthSystem.update(player, Weather.FROST, world,
   1.0f, false, inventory)`. Verify `player.getHealth()` has decreased by approximately
   `Player.WARMTH_DAMAGE_PER_SECOND` (within 0.1 tolerance).

3. **Coat halves warmth drain**: Set weather to `Weather.FROST`. Add `Material.COAT` to
   inventory. Record warmth drain over 10 simulated seconds with coat, vs 10 seconds
   without coat. Verify the with-coat drain is ≤ 55% of the without-coat drain (50%
   reduction ± 5% tolerance).

4. **NPC hears player block-break**: Place player 8 blocks from a police NPC (within
   max hearing range of 20 blocks). Fire `noiseSystem.spikeBlockBreak()` and
   `player.setNoiseLevel(noiseSystem.getNoiseLevel())`. Verify
   `NoiseSystem.getHearingRange(player.getNoiseLevel())` ≥ 8f (player is audible).
   Verify the police NPC transitions to `NPCState.ALERTED` on the next
   `npcManager.update()` tick.

5. **Noise decays to baseline after spike**: Fire `noiseSystem.spikeBlockBreak()`.
   Advance 3.0 simulated seconds via `noiseSystem.update()` with `isMoving = false`.
   Verify `noiseSystem.getNoiseLevel()` has returned to within 0.1 of
   `NoiseSystem.NOISE_STILL` (0.05).

---

## Wire StreetSkillSystem & SkillsUI into the Game Loop

**Goal**: The `StreetSkillSystem` (Issue #787) and its companion `SkillsUI` exist as
complete, fully-featured dead code. The system is never instantiated in
`RagamuffinGame`, its `update()` is never called, no XP is ever awarded for any
player action, and the K-key Skills screen is inaccessible to the player. All seven
skills (BRAWLING, GRAFTING, TRADING, STEALTH, INFLUENCE, SURVIVAL, BUREAUCRACY) and
their tier perks are completely inert.

### What needs wiring

1. **Instantiate** `StreetSkillSystem` and `SkillsUI` in `RagamuffinGame.initGame()`.
   Inject optional dependencies: `achievementSystem`, `raveSystem` (if present),
   `stallSystem` (if present), `factionSystem` (if present), `wantedSystem`.

2. **Call `update(delta)`** on `StreetSkillSystem` in the PLAYING game loop
   (`updatePlayingSimulation`) to advance the RALLY timer and follower management.

3. **Award XP at every relevant action site** in `RagamuffinGame` (and in system
   classes that already receive a `StreetSkillSystem` reference):
   - Punch lands on NPC → `award(BRAWLING, XP_PUNCH_HIT)`
   - NPC defeated → `award(BRAWLING, XP_FIGHT_WIN)`
   - Player takes damage → `award(BRAWLING, XP_TAKE_DAMAGE)`
   - Block broken → `award(GRAFTING, XP_BLOCK_BREAK)`
   - Item collected from drop → `award(GRAFTING, XP_COLLECT_RESOURCE)`
   - Talk to NPC → `award(INFLUENCE, XP_TALK_NPC)`
   - Player crouches and loses police pursuit → `award(STEALTH, XP_CROUCH_ESCAPE)`
   - Police pursuit lost → `award(STEALTH, XP_EVADE_POLICE)`
   - In-game day survives → `award(SURVIVAL, XP_DAY_SURVIVED)`

4. **K-key toggle**: Add `isSkillsPressed()` / `resetSkills()` to `InputHandler`
   (key code `com.badlogic.gdx.Input.Keys.K`). In the PLAYING input block, toggle
   `skillsUI.isOpen()` on K press (close other panels first, same pattern as
   inventory/crafting).

5. **Render `SkillsUI`** in the 2D HUD pass when `skillsUI.isOpen()` is true.

6. **Apply active perks** at decision points already present in the game loop:
   - BRAWLING Apprentice: multiply punch damage by 1.1 when `getTier(BRAWLING) ≥ APPRENTICE`
   - GRAFTING Expert: reduce soft block hit threshold from 5 → 3 when `getTier(GRAFTING) ≥ EXPERT`
   - SURVIVAL Apprentice: multiply hunger drain by 0.8 when `getTier(SURVIVAL) ≥ APPRENTICE`
   - STEALTH Apprentice: halve noise while crouching (pass into `NoiseSystem`)

### Integration tests — implement these exact scenarios

1. **XP awarded on block break**: Create `StreetSkillSystem`. Call
   `system.award(StreetSkillSystem.Skill.GRAFTING, StreetSkillSystem.XP_BLOCK_BREAK)` 50 times.
   Verify `system.getXP(Skill.GRAFTING)` equals `50 * XP_BLOCK_BREAK`. Verify
   `system.getTier(Skill.GRAFTING)` is `Tier.APPRENTICE` (threshold is 100 XP, so 50
   awards × 1 XP = 50; use `XP_BLOCK_BREAK = 1`, confirm tier is still NOVICE at 50
   XP; award 50 more and confirm tier advances to APPRENTICE).

2. **Tier-up fires achievement**: Inject a mock `AchievementSystem`. Award enough XP
   to cross the APPRENTICE threshold (100 XP) in BRAWLING. Verify
   `achievementSystem.unlock(AchievementType.STREET_LEGEND)` is called once the
   player reaches the LEGEND tier (1500 XP).

3. **BRAWLING Apprentice perk increases punch damage**: Set up a player and an NPC.
   Award BRAWLING XP until tier is `APPRENTICE`. Read the perk multiplier via
   `system.getPunchDamageMultiplier()`. Verify the value is `1.1f` (not `1.0f`).

4. **GRAFTING Expert reduces soft block hits**: Award GRAFTING XP until tier is
   `EXPERT`. Verify `system.getSoftBlockHitsRequired()` returns `3` (not `5`).

5. **SkillsUI opens on K key**: In a headless integration test, instantiate
   `SkillsUI` and confirm `isOpen()` is `false` by default. Call `open()`. Verify
   `isOpen()` returns `true`. Call `close()`. Verify `isOpen()` returns `false`.

## Wire FactionSystem into the Game Loop

**Goal**: `FactionSystem` (649 lines, Phase 8d / Issue #702) exists as complete dead
code. It is never instantiated in `RagamuffinGame`, its `update()` is never called,
and `gameHUD.setFactionSystem()` is never invoked — so the three-faction Respect bars
built into `GameHUD` are permanently blank. Worse, `FactionSystem` is a **required
constructor argument or injected dependency for 13 other systems** that are
themselves dead code: `StreetSkillSystem`, `GraffitiSystem`, `CornerShopSystem`,
`BootSaleSystem`, `JobCentreSystem`, `HeistSystem`, `NewspaperSystem`,
`PirateRadioSystem`, `StallSystem`, `NeighbourhoodSystem`, `SquatSystem`,
`MCBattleSystem`, and `PropertySystem`. None of these can be wired in until
`FactionSystem` exists in the game loop. This is the single most-blocking piece of
unwired infrastructure in the codebase.

### What needs wiring

1. **Declare and instantiate** `private FactionSystem factionSystem;` in
   `RagamuffinGame`. Instantiate it in `initGame()`:
   ```java
   factionSystem = new FactionSystem();
   ```

2. **Call `update(delta)`** on `factionSystem` every frame in the PLAYING game loop
   (`updatePlayingSimulation`), passing the current player and NPC list:
   ```java
   factionSystem.update(delta, player, npcManager.getNPCs());
   ```

3. **Connect to GameHUD** immediately after `gameHUD` is created so faction Respect
   bars render in the HUD:
   ```java
   gameHUD.setFactionSystem(factionSystem);
   ```
   Also call `gameHUD.setFactionSystem(factionSystem)` in `restartGame()` after the
   new `GameHUD` is constructed.

4. **Fire Respect deltas at existing action sites** in `RagamuffinGame`:
   - Player punches an NPC belonging to a faction →
     `factionSystem.applyRespectDelta(npc.getFaction(), FactionSystem.DELTA_HIT_NPC)`
   - Player breaks a block in a faction building →
     `factionSystem.applyRespectDelta(buildingFaction, FactionSystem.DELTA_RIVAL_BUILDING_BREAK)`
   - Player is arrested → `factionSystem.onPlayerArrested()`
   - Player completes a quest for an NPC → `factionSystem.onQuestCompleted(npc.getFaction())`

5. **Pass `factionSystem` into `WantedSystem.update()`** — `WantedSystem` already
   documents a `FactionSystem` lockdown at Wanted 5 during `COUNCIL_CRACKDOWN`
   (see its line ~910). Update the call-site in `RagamuffinGame` to supply the
   instance once it exists.

6. **Reset on restart**: Call `factionSystem = new FactionSystem()` (or
   `factionSystem.reset()` if such a method exists) inside `restartGame()` so
   faction state does not bleed between sessions.

### Integration tests — implement these exact scenarios

1. **Respect changes on NPC hit**: Create `FactionSystem`. Verify initial Respect for
   `STREET_LADS` is `FactionSystem.STARTING_RESPECT` (50). Call
   `factionSystem.applyRespectDelta(Faction.STREET_LADS, FactionSystem.DELTA_HIT_NPC)`.
   Verify `factionSystem.getRespect(Faction.STREET_LADS)` equals
   `50 + FactionSystem.DELTA_HIT_NPC` (i.e. 35).

2. **Hostile threshold makes NPCs hostile**: Create `FactionSystem`. Force Respect for
   `MARCHETTI_CREW` below `FactionSystem.HOSTILE_THRESHOLD` (20) via repeated
   `applyRespectDelta` calls. Create a `GANG_MEMBER` NPC whose faction is
   `MARCHETTI_CREW`. Call `factionSystem.update(1f, player, List.of(npc))`. Verify
   the NPC's state is `NPCState.HOSTILE` (or `CHASING`).

3. **Turf transfer fires when Respect gap exceeds threshold**: Create
   `FactionSystem` with a `TurfMap`. Grant `STREET_LADS` Respect 90 and
   `MARCHETTI_CREW` Respect 55 (gap = 35 > `TURF_TRANSFER_GAP` = 30). Call
   `factionSystem.update(1f, player, emptyList)`. Verify that
   `turfMap.getOwner(someBlock)` has changed from `MARCHETTI_CREW` to `STREET_LADS`
   for at least one transferred block.

4. **GameHUD renders faction bars when FactionSystem is attached**: Instantiate
   `FactionSystem` and `GameHUD`. Call `gameHUD.setFactionSystem(factionSystem)`.
   Verify `gameHUD.getFactionSystem()` returns the same instance (not null). Verify
   that calling `gameHUD.render(...)` does not throw — confirming the render path
   that reads `factionSystem.getRespect(f)` is exercised without NPE.

5. **Everyone Hates You state activates**: Force all three factions below
   `FactionSystem.EVERYONE_HATES_YOU_THRESHOLD` (30). Call `factionSystem.update(1f,
   player, npcList)`. Verify `factionSystem.isEveryoneHatesYou()` returns `true` and
   that all faction NPCs in `npcList` have a hostile NPC state.

## Wire CampfireSystem into the Game Loop

### Problem

`CampfireSystem` is fully implemented (`ragamuffin/core/CampfireSystem.java`, 122 lines)
but is never instantiated in `RagamuffinGame`. The game loop contains this explicit
stub comment at the `WarmthSystem.update()` call site:

```java
boolean nearCampfire = false; // no campfire system wired yet
warmthSystem.update(player, weatherSystem.getCurrentWeather(), world,
        delta, nearCampfire, inventory);
```

Because `nearCampfire` is hardcoded to `false`, the `WarmthSystem` can never restore
warmth from a campfire — players with hypothermia cannot warm up next to a fire, no
matter how many CAMPFIRE blocks they craft and place. Additionally:

- Campfires are never registered with the system when placed via `BlockPlacer`, so
  rain-extinguishing and flicker-light behaviour never fire.
- Campfires are never deregistered when broken via `BlockBreaker`, so the system
  never tracks any campfire positions at all.

The `CAMPFIRE` block type exists in `BlockType.java` and `WarmthSystem` already reads
the `nearCampfire` boolean — the only missing piece is wiring `CampfireSystem` in.

### What needs wiring

1. **Declare and instantiate** `private CampfireSystem campfireSystem;` in
   `RagamuffinGame`. Instantiate it in `initGame()` after `weatherSystem`:
   ```java
   campfireSystem = new CampfireSystem();
   ```

2. **Register campfire on block place** — in the right-click / block-place handler
   inside `RagamuffinGame`, after `blockPlacer.placeBlock(...)` succeeds, check if the
   placed block type is `BlockType.CAMPFIRE` and register it:
   ```java
   if (placedType == BlockType.CAMPFIRE) {
       campfireSystem.addCampfire(new Vector3(bx, by, bz));
   }
   ```

3. **Deregister campfire on block break** — in the block-break logic, after
   `blockBreaker` removes a CAMPFIRE block, call:
   ```java
   campfireSystem.removeCampfire(new Vector3(bx, by, bz));
   ```

4. **Call `update()` every frame** in `updatePlayingSimulation()`:
   ```java
   campfireSystem.update(world, weatherSystem.getCurrentWeather(), delta);
   ```

5. **Pass real `nearCampfire` to WarmthSystem** — replace the hardcoded stub:
   ```java
   // BEFORE (broken):
   boolean nearCampfire = false; // no campfire system wired yet
   // AFTER:
   boolean nearCampfire = campfireSystem.isNearCampfire(player.getPosition());
   ```

6. **Reset on restart** — call `campfireSystem = new CampfireSystem();` inside
   `restartGame()` so campfire state does not bleed between sessions.

### Integration tests — implement these exact scenarios

1. **Player warms up near a campfire**: Create `WarmthSystem` and `CampfireSystem`.
   Drain player warmth to 30 (below the cold threshold). Register a campfire at
   position (0, 0, 0). Place the player at (0, 0, 2) (within
   `WarmthSystem.CAMPFIRE_WARMTH_RADIUS` = 5 blocks). Call
   `campfireSystem.isNearCampfire(player.getPosition())` and verify it returns `true`.
   Call `warmthSystem.update(player, Weather.CLEAR, world, 1f, true, inventory)`.
   Verify `player.getWarmth()` has increased above 30.

2. **Player does NOT warm up when out of campfire range**: Register a campfire at
   (0, 0, 0). Place the player at (0, 0, 10) (beyond the 5-block radius). Verify
   `campfireSystem.isNearCampfire(player.getPosition())` returns `false`. Drain
   player warmth to 30. Call `warmthSystem.update(player, Weather.CLEAR, world, 1f,
   false, inventory)`. Verify warmth has not increased.

3. **Rain extinguishes campfire**: Create `CampfireSystem` and a `World`. Set block
   (5, 1, 5) to `BlockType.CAMPFIRE`. Call `campfireSystem.addCampfire(new Vector3(5, 1,
   5))`. Verify `campfireSystem.hasCampfires()` is `true`. Create a `Weather` instance
   where `weather.isRaining()` returns `true`. Call `campfireSystem.update(world,
   rainyWeather, 0.016f)`. Verify `campfireSystem.hasCampfires()` is now `false` and
   `world.getBlock(5, 1, 5)` is `BlockType.AIR`.

4. **Campfire flicker intensity varies over time**: Create `CampfireSystem`. Call
   `campfireSystem.update(world, clearWeather, 0f)` to reset flicker time. Record
   `float i1 = campfireSystem.getCurrentLightIntensity()`. Advance time by
   `1f / (CampfireSystem.FLICKER_FREQUENCY * 4)` seconds via repeated `update` calls.
   Record `float i2 = campfireSystem.getCurrentLightIntensity()`. Verify `i1 != i2`
   (the intensity has changed due to sine-wave flicker).

5. **Campfire deregistered on block break**: Register a campfire at (3, 1, 3). Verify
   `campfireSystem.hasCampfires()` is `true`. Call
   `campfireSystem.removeCampfire(new Vector3(3, 1, 3))`. Verify
   `campfireSystem.hasCampfires()` is now `false`.

## Wire HeistSystem into the Game Loop

**Goal**: `HeistSystem` (1012 lines, Phase O / Issue #704) is fully implemented but
never instantiated or called anywhere in `RagamuffinGame.java`. Players cannot
case buildings, plan heists, crack safes, dodge CCTV, recruit accomplices, or fence
hot loot — the entire heist gameplay pillar is dead code.

### Problem

`HeistSystem` was built as a self-contained four-phase heist orchestrator:
- **Casing** — press F inside a Jeweller / Off-licence / Greggs / JobCentre to gather
  intel and create a `HeistPlan`.
- **Planning** — acquire tools (CROWBAR etc.) and optionally pay 10 COIN to recruit
  an NPC accomplice.
- **Execution** — press G to start the countdown timer; break blocks near alarm boxes
  spikes noise to 1.0 and triggers `WantedSystem`; hold E on a safe for 8 s to crack
  it; CCTV cameras detect the player within a 45° cone / 6-block range.
- **Fencing** — hot loot depreciates over time (100% → 50% after 5 min, 25% after
  60 min in-game).

None of these interactions fire because the system is never wired in.

### What needs wiring

1. **Declare and instantiate** `private HeistSystem heistSystem;` in `RagamuffinGame`.
   Instantiate in `initGame()` after `factionSystem`:
   ```java
   heistSystem = new HeistSystem();
   ```

2. **Call `update()` every frame** in `updatePlayingSimulation()`:
   ```java
   heistSystem.update(delta, player, noiseSystem, npcManager, factionSystem,
       rumourNetwork, npcManager.getAllNPCs(), world,
       timeSystem.isNight());
   ```

3. **Wire F key — Casing** — in the keyboard input handler (`keyDown`), when
   `Keys.F` is pressed while `gameState == PLAYING` and the player is inside a
   recognised landmark building, call:
   ```java
   String msg = heistSystem.startCasing(landmarkType, buildingCentre, npcManager);
   tooltipSystem.show(msg);
   ```

4. **Wire G key — Execution** — in `keyDown`, when `Keys.G` is pressed:
   ```java
   boolean started = heistSystem.startExecution();
   if (started) tooltipSystem.show("Heist started! Timer running…");
   ```

5. **Wire block-break hook** — after a block is broken in the execution handler,
   call `heistSystem.onBlockBreak(breakPos, noiseSystem, npcManager, world)` so
   alarm boxes spike noise correctly.

6. **Wire safe-cracking hold-E** — during the hold-E interaction frame, if the player
   is targeting a SAFE prop, call:
   ```java
   boolean cracked = heistSystem.updateSafeCracking(delta, player, inventory,
       npcManager, achievementSystem::award, world);
   ```

7. **Wire accomplice recruitment** — in the NPC interact path (E key on NPC), if the
   NPC type is CRIMINAL and the heist is in PLANNING phase, call
   `heistSystem.recruitAccomplice(npc, inventory)`.

8. **Wire daily reset** — call `heistSystem.resetDaily()` from the time-system
   midnight callback.

9. **Reset on restart** — call `heistSystem = new HeistSystem();` inside
   `restartGame()`.

10. **HUD hint** — when `heistSystem.getActivePlan() != null`, draw a small HUD label
    showing the current heist phase and countdown timer via `gameHUD`.

### Integration tests — implement these exact scenarios

1. **Casing a landmark creates a HeistPlan**: Create `HeistSystem` and `NPCManager`.
   Call `heistSystem.startCasing(LandmarkType.JEWELLER, new Vector3(0,0,0), npcManager)`.
   Verify `heistSystem.getActivePlan() != null`. Verify
   `heistSystem.getActivePlan().getTarget() == LandmarkType.JEWELLER`. Verify
   `heistSystem.getPhase() == HeistSystem.HeistPhase.PLANNING`.

2. **Execution start transitions phase**: After casing (as above), call
   `heistSystem.startExecution()`. Verify it returns `true`. Verify
   `heistSystem.getPhase() == HeistSystem.HeistPhase.EXECUTION`.

3. **Block break near alarm box spikes noise**: Create `HeistSystem`, `NoiseSystem`,
   `NPCManager`, and a `World`. Case the Jeweller. Start execution. Place the player
   within `HeistSystem.ALARM_BOX_RANGE` of a recorded alarm box position. Call
   `heistSystem.onBlockBreak(alarmPos, noiseSystem, npcManager, world)`. Verify
   `noiseSystem.getCurrentNoise() >= 1.0f`.

4. **Safe cracking completes after 8 seconds**: Create `HeistSystem`, `Inventory`,
   and `NPCManager`. Add a CROWBAR to the inventory. Case and start execution. Call
   `heistSystem.updateSafeCracking(delta, player, inventory, npcManager,
   cb -> {}, world)` in a loop for `HeistSystem.SAFE_CRACK_TIME` seconds total.
   Verify the method returns `true` (cracking complete) on the final call.

5. **Hot loot multiplier depreciates over time**: Create `HeistSystem`. Case and
   complete a heist. Verify `heistSystem.getHotLootMultiplier() == 1.0f` immediately.
   Advance the system by `HeistSystem.HOT_LOOT_FULL_PRICE_WINDOW + 1f` seconds via
   `update()` calls. Verify `heistSystem.getHotLootMultiplier() == 0.5f`. Advance by
   `HeistSystem.HOT_LOOT_HALF_PRICE_WINDOW + 1f` more seconds. Verify
   `heistSystem.getHotLootMultiplier() == 0.25f`.

## Wire NeighbourhoodWatchSystem into the Game Loop

**Goal**: The `NeighbourhoodWatchSystem` (Issue #797) is complete, fully-featured dead code.
The system tracks Watch Anger (0–100) and escalates ordinary civilian NPCs into vigilante mobs
in response to the player's crimes — the most distinctly British threat in the game. However, it
is never instantiated in `RagamuffinGame`, its `update()` is never called, its anger triggers
are never fired from crime events, and the G key grovel mechanic is never wired. The entire
community-uprising pillar is invisible to the player.

### Problem

`NeighbourhoodWatchSystem` was built with a clean interface for all required hooks:
- `update(delta, raining, foggy, callback)` — per-frame anger decay and tier recalculation
- `onPlayerPunchedCivilian()` — fires when player punches a PUBLIC NPC
- `onPlayerSmashedExteriorWall()` — fires when player breaks an exterior building block
- `onVisibleCrime()` — fires when a crime is witnessed
- `grovelling(delta, callback)` — call every frame while G is held for the grovel mechanic
- `addAnger(int)` — direct anger injection from other systems (Notoriety tier threshold, etc.)

None of these are connected.

### What needs wiring

1. **Declare and instantiate** `private NeighbourhoodWatchSystem neighbourhoodWatchSystem;`
   in `RagamuffinGame`. Instantiate in `initGame()` after `notorietySystem`:
   ```java
   neighbourhoodWatchSystem = new NeighbourhoodWatchSystem();
   ```

2. **Call `update()` every frame** in `updatePlayingSimulation()`:
   ```java
   Weather w = weatherSystem.getCurrentWeather();
   neighbourhoodWatchSystem.update(delta,
       w == Weather.RAIN || w == Weather.DRIZZLE || w == Weather.THUNDERSTORM,
       w == Weather.FOG,
       type -> achievementSystem.unlock(type));
   ```

3. **Wire civilian-punch trigger** — in the NPC hit-detection path (left-click on a PUBLIC or
   PENSIONER NPC), call:
   ```java
   neighbourhoodWatchSystem.onPlayerPunchedCivilian();
   ```

4. **Wire exterior-wall-smash trigger** — in the block-break handler, when the broken block
   belongs to a building structure (BRICK, GLASS, STONE) and the block is on an outer face
   of that structure, call:
   ```java
   neighbourhoodWatchSystem.onPlayerSmashedExteriorWall();
   ```

5. **Wire visible-crime trigger** — in `WitnessSystem` or the crime event path (theft,
   visible block break), when 2+ NPC witnesses are present, call:
   ```java
   neighbourhoodWatchSystem.onVisibleCrime();
   ```

6. **Wire G key — Grovel mechanic** — in `updatePlayingSimulation()`, while
   `Gdx.input.isKeyPressed(Keys.G)` and `gameState == PLAYING`, call:
   ```java
   boolean done = neighbourhoodWatchSystem.grovelling(delta,
       type -> achievementSystem.unlock(type));
   if (done) tooltipSystem.showMessage("You grovel apologetically.", 2.0f);
   ```
   Also render a grovel progress bar via `gameHUD` using `neighbourhoodWatchSystem.getGroveltProgress()`.

7. **Wire HUD display** — pass `neighbourhoodWatchSystem.getWatchAnger()` and
   `neighbourhoodWatchSystem.getCurrentTier()` to `gameHUD` so the Watch Anger level is visible
   to the player (small indicator near the notoriety display).

8. **Reset on restart** — call `neighbourhoodWatchSystem = new NeighbourhoodWatchSystem();`
   inside `restartGame()`.

### Integration tests — implement these exact scenarios

1. **Punching a civilian increases Watch Anger**: Create `NeighbourhoodWatchSystem`.
   Assert initial anger is 0. Call `onPlayerPunchedCivilian()`. Verify
   `getWatchAnger() == NeighbourhoodWatchSystem.ANGER_PUNCH_CIVILIAN`.

2. **Anger decays to zero over time in rainy weather**: Create `NeighbourhoodWatchSystem`.
   Call `addAnger(20)`. Call `update(10f, true, false, null)` (10 seconds of rain).
   Verify anger has decreased by at least
   `(int)(10f * ANGER_DECAY_PER_SECOND * ANGER_DECAY_WEATHER_MULTIPLIER)` points.

3. **Tier escalates correctly**: Create `NeighbourhoodWatchSystem`. Call `addAnger(50)`.
   Call `update(0f, false, false, null)`. Verify `getCurrentTier() == 3`
   (Vigilante Patrol threshold).

4. **Grovel mechanic reduces anger**: Create `NeighbourhoodWatchSystem`. Call `addAnger(30)`.
   Simulate holding G: call `grovelling(delta, null)` in a loop until it returns `true`
   (total time >= `GROVEL_HOLD_DURATION` seconds). Verify
   `getWatchAnger() == 30 - ANGER_GROVEL_REDUCTION`.

5. **Watch anger visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify that `neighbourhoodWatchSystem` is non-null. Call
   `onPlayerPunchedCivilian()` once. Simulate 1 frame (`render()` with delta=0.016f).
   Verify `neighbourhoodWatchSystem.getWatchAnger() > 0`.

---

## Wire DisguiseSystem into the game loop

`DisguiseSystem` (Issue #767) is fully implemented in
`src/main/java/ragamuffin/core/DisguiseSystem.java` but is never instantiated in
`RagamuffinGame.java`. It is the most important unwired system because `WantedSystem`
(already wired) explicitly calls `disguiseSystem.attemptDisguiseEscape()` at line 416 of
`WantedSystem.java` — passing `null` until this is fixed silently skips the entire
police-escape-via-disguise path. No player can ever wear a disguise, bluff a guard,
loot NPC clothing, trigger the UNDERCOVER or METHOD_ACTOR achievements, or use the
POLICE_UNIFORM to delay a heist alarm.

### Key APIs already implemented

| Method | Purpose |
|---|---|
| `equipDisguise(Material, Inventory)` | Player equips looted clothing |
| `update(float, Player, List<NPC>, float)` | Each frame: scrutiny decay, cover integrity |
| `notifyCrime(Player, List<NPC>)` | –20 cover on visible crime |
| `attemptBluff(Inventory, Player, List<NPC>)` | 60/90 % success gate on E-key entry |
| `lootDisguise(NPC, Inventory)` | Add clothing to inventory from knocked-out NPC |
| `isDisguised()` / `getCoverIntegrity()` | HUD display |
| `isWearingPoliceUniform()` | Used by HeistSystem alarm-delay |

### What needs wiring

1. **Declare and instantiate** `private DisguiseSystem disguiseSystem;` in `RagamuffinGame`.
   Instantiate in `initGame()` after `achievementSystem`:
   ```java
   disguiseSystem = new DisguiseSystem();
   disguiseSystem.setAchievementSystem(achievementSystem);
   disguiseSystem.setRumourNetwork(rumourNetwork);
   ```

2. **Call `update()` every frame** in `updatePlayingSimulation()`:
   ```java
   disguiseSystem.update(delta, player, npcManager.getNPCs(), player.getSpeed());
   ```

3. **Wire loot-disguise on NPC knockout** — in the NPC hit-detection path, when an NPC's
   health drops to 0, call:
   ```java
   Material looted = disguiseSystem.lootDisguise(targetNPC, inventory);
   if (looted != null) tooltipSystem.showMessage("You take the " + looted.displayName() + ".", 2.5f);
   ```

4. **Wire equip-disguise on E-key** — when the player presses E on a clothing item in the
   hotbar (or inventory), call:
   ```java
   disguiseSystem.equipDisguise(selectedMaterial, inventory);
   ```

5. **Wire bluff on E-key entry attempt** — when the player presses E to enter a guarded
   building (faction building, police station, off-licence back room), call:
   ```java
   DisguiseSystem.BluffResult bluff = disguiseSystem.attemptBluff(inventory, player, npcManager.getNPCs());
   if (bluff == DisguiseSystem.BluffResult.SUCCESS) { /* grant entry */ }
   else if (bluff == DisguiseSystem.BluffResult.FAILED) tooltipSystem.showMessage("Blown! Leg it!", 2f);
   ```

6. **Wire notifyCrime** — in the block-break handler and NPC-punch handler, after a visible
   crime, call:
   ```java
   disguiseSystem.notifyCrime(player, npcManager.getNPCs());
   ```

7. **Wire into WantedSystem escape** — pass `disguiseSystem` to the existing
   `wantedSystem.attemptDisguiseEscape(disguiseSystem, ...)` call site in the wanted-escape
   input handler (currently called with `null`).

8. **Wire HUD display** — pass `disguiseSystem.isDisguised()` and
   `disguiseSystem.getCoverIntegrity()` to `gameHUD` so a cover-integrity bar is shown
   whenever a disguise is active.

9. **Reset on restart** — call:
   ```java
   disguiseSystem = new DisguiseSystem();
   disguiseSystem.setAchievementSystem(achievementSystem);
   disguiseSystem.setRumourNetwork(rumourNetwork);
   ```
   inside `restartGame()`.

### Integration tests — implement these exact scenarios

1. **Equipping a disguise sets isDisguised true**: Create `DisguiseSystem`. Create an
   `Inventory` containing `Material.POLICE_UNIFORM`. Call
   `equipDisguise(Material.POLICE_UNIFORM, inventory)`. Verify `isDisguised() == true`
   and `getCoverIntegrity() == DisguiseSystem.MAX_COVER_INTEGRITY`.

2. **Cover decays during scrutiny**: Create `DisguiseSystem`. Equip `POLICE_UNIFORM`.
   Manually enter scrutiny state by calling `update(3.1f, player, suspiciousNpcList, 0f)`
   where `suspiciousNpcList` contains a COUNCIL_MEMBER NPC within `SCRUTINY_RANGE`.
   Verify `getCoverIntegrity() < MAX_COVER_INTEGRITY`.

3. **Crime event reduces cover**: Create `DisguiseSystem`. Equip `MARCHETTI_TRACKSUIT`.
   Assert `getCoverIntegrity() == 100`. Call `notifyCrime(player, emptyList)`. Verify
   `getCoverIntegrity() == 100 - DisguiseSystem.CRIME_COVER_PENALTY`.

4. **Bluff succeeds with RUMOUR_NOTE**: Create `DisguiseSystem`. Equip `POLICE_UNIFORM`.
   Create an `Inventory` with 1× `Material.RUMOUR_NOTE`. Over 100 calls to
   `attemptBluff(inventory, player, emptyNpcs)`, verify success rate ≥ 85%
   (confirming the 90 % path, with statistical tolerance).

5. **Loot disguise from knocked-out NPC**: Create a `DisguiseSystem`. Create a dead
   (health = 0) NPC of type `NPCType.POLICE`. Create an empty `Inventory`. Call
   `lootDisguise(deadPoliceNPC, inventory)`. Verify `inventory.hasItem(Material.POLICE_UNIFORM, 1)`
   returns `true`.

6. **DisguiseSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `disguiseSystem` field is non-null. Simulate 1 frame. Verify
   `disguiseSystem.isDisguised() == false` (no disguise on fresh start).

---

## Wire GraffitiSystem into the game loop

`GraffitiSystem` (implemented in issue #781) and its dedicated `GraffitiRenderer` are
completely absent from `RagamuffinGame.java`. The T-key spray handler, the per-frame
`update()` call, and the `GraffitiRenderer.render()` call are all missing, making the
entire graffiti and territorial-marking feature dead code. Players cannot spray tags,
faction NPC crews never dispatch to claim turf, fade timers never advance, and graffiti
marks are never drawn on screen.

### What needs to be wired

1. **Declare fields** in `RagamuffinGame`:
   ```java
   private GraffitiSystem graffitiSystem;
   private ragamuffin.render.GraffitiRenderer graffitiRenderer;
   ```

2. **Instantiate in `create()`** (and `restartGame()`):
   ```java
   graffitiSystem = new GraffitiSystem();
   graffitiRenderer = new ragamuffin.render.GraffitiRenderer();
   ```

3. **Update each frame** in the PLAYING branch of `render()`:
   ```java
   graffitiSystem.update(delta, timeSystem.getDaysDelta(),
       npcManager.getNPCs(), world.getTurfMap(),
       wantedSystem, noiseSystem, rumourNetwork,
       inventory, type -> achievementSystem.unlock(type));
   ```

4. **Render each frame** after chunk rendering (3D pass):
   ```java
   graffitiRenderer.render(graffitiSystem.getMarks(), camera);
   ```

5. **Wire the T key** in `handlePlayerInput()`:
   When the player presses T and has a `Material.SPRAY_CAN` in the active hotbar slot,
   call `graffitiSystem.placeTag(...)` at the targeted block face (from raycast result).
   Show a tooltip if no spray can is held.

6. **Wire the E-key scrub** in `handleInteract()`:
   When the player is within `GraffitiSystem.MAX_TAG_DISTANCE` of a block that has a
   graffiti mark belonging to a rival faction, call
   `graffitiSystem.scrubTag(mark, world.getTurfMap(), wantedSystem, noiseSystem)`.

7. **Reset on restart** in `restartGame()`:
   ```java
   graffitiSystem = new GraffitiSystem();
   ```

### Integration tests — implement these exact scenarios

1. **Tag placed on T keypress**: Initialise `RagamuffinGame` (headless). Give the player
   a `Material.SPRAY_CAN` in hotbar slot 0. Place the player at (10, 1, 10) facing a
   BRICK wall at (10, 1, 9). Simulate pressing T once. Verify
   `graffitiSystem.getMarks().size() == 1`.

2. **GraffitiRenderer called each frame**: Initialise `RagamuffinGame` (headless).
   Verify `graffitiRenderer` field is non-null. Simulate 2 frames in PLAYING state.
   Verify no exception is thrown and `graffitiSystem.getMarks()` is accessible.

3. **GraffitiSystem.update() advances NPC crew timer**: Create `GraffitiSystem`. Call
   `update(301f, 0f, npcList, turfMap, wantedSystem, noiseSystem, rumourNetwork, inventory, cb)`.
   Verify that after 301 seconds of delta the NPC crew spray tick has fired at least once
   (observable via `getMarks().size() > 0` if faction NPCs are present in the list).

4. **GraffitiSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `graffitiSystem` field is non-null. Simulate 1 frame.
   Verify `graffitiSystem.getMarks()` returns an empty list (no tags on fresh start).

---

## Wire MCBattleSystem, RaveSystem & SquatSystem into the game loop

`MCBattleSystem`, `RaveSystem`, and `SquatSystem` were implemented (Issues #714 and #716) but
are never instantiated or updated in `RagamuffinGame`. Without these systems the player cannot
claim a squat, challenge MC champions, earn MC Rank, or host illegal raves — a large chunk of
the gameplay loop that unlocks factions, notoriety and achievement progression is completely
inaccessible.

### What needs to be done

1. **Declare fields** in `RagamuffinGame`:
   ```java
   private SquatSystem squatSystem;
   private MCBattleSystem mcBattleSystem;
   private RaveSystem raveSystem;
   ```

2. **Instantiate in `create()`** (and `restartGame()`):
   ```java
   squatSystem   = new SquatSystem(achievementSystem, notorietySystem);
   mcBattleSystem = new MCBattleSystem(factionSystem, notorietySystem, rumourNetwork, achievementSystem, new java.util.Random());
   raveSystem    = new RaveSystem(achievementSystem, notorietySystem, rumourNetwork);
   ```

3. **Update each frame** in the PLAYING branch of `render()`:
   ```java
   // SquatSystem — daily Vibe decay (pass current day index from timeSystem)
   squatSystem.tickDay(timeSystem.getDayIndex(),
       notorietySystem.getTier(), npcManager.getNPCs(), inventory);

   // MCBattleSystem — advance active battle bar cursor
   mcBattleSystem.update(delta);

   // RaveSystem — income accumulation, police alert timer
   raveSystem.update(delta, inventory,
       squatSystem.countAttendeesInSquat(npcManager.getNPCs()));
   ```

4. **Wire the E key** in `handleInteract()`:
   - If the player is inside their squat and no squat claimed yet: attempt to claim via
     `squatSystem.claimSquat(...)` when the player holds ≥ 5 WOOD.
   - If `mcBattleSystem.canChallenge(targetNpc, inventory)`: start a battle via
     `mcBattleSystem.startBattle(targetNpc, ...)`.
   - If the player is at their squat entrance and a rave is active: call
     `raveSystem.disperseRave(inventory)` (early disperse to swerve the feds).

5. **Wire rave start** via flyer use (right-click on FLYER item or `handleInteract()`):
   Call `raveSystem.startRave(inventory, squatSystem.getVibe(), mcBattleSystem.getMcRank(), npcManager.getNearbyNpcs(player))`.

6. **Wire police spawn** when `raveSystem.isPoliceAlerted()` transitions to `true`:
   Spawn 2 PCSO NPCs near the squat entrance via `npcManager`.

7. **Wire the BattleBar rendering** in the 2D HUD pass:
   When `mcBattleSystem.isBattleActive()`, render the `BattleBarMiniGame` overlay
   (cursor position, hit zone, round count) using `SpriteBatch` / `ShapeRenderer`.

8. **Reset on restart** in `restartGame()`:
   ```java
   squatSystem    = new SquatSystem(achievementSystem, notorietySystem);
   mcBattleSystem = new MCBattleSystem(factionSystem, notorietySystem, rumourNetwork, achievementSystem, new java.util.Random());
   raveSystem     = new RaveSystem(achievementSystem, notorietySystem, rumourNetwork);
   ```

### Integration tests — implement these exact scenarios

1. **Squat can be claimed**: Initialise `RagamuffinGame` (headless). Give the player 5
   `Material.WOOD` in inventory. Place the player inside a derelict building landmark.
   Simulate pressing E. Verify `squatSystem.hasSquat()` returns `true`.

2. **MC Battle starts on E with microphone**: Give the player a `Material.MICROPHONE`.
   Place a Marchetti MC Champion NPC adjacent to the player. Simulate pressing E.
   Verify `mcBattleSystem.isBattleActive()` returns `true`.

3. **Winning MC Battle increments MC Rank**: Begin a battle against the Easy (Marchetti)
   champion. Simulate pressing the action key when the cursor is inside the hit zone for
   all 3 rounds (using `BattleBarMiniGame` directly). Verify `mcBattleSystem.getMcRank()`
   increases from 0 to 1.

4. **Rave starts with flyer + vibe + MC Rank**: Set `squatSystem` vibe to 40, set MC Rank
   to 1, give the player a `Material.FLYER`. Call `raveSystem.startRave(...)`. Verify
   `raveSystem.isRaveActive()` returns `true` and the FLYER was consumed from inventory.

5. **Police alerted after 120 seconds**: Call `raveSystem.startRave(...)` with valid
   prerequisites. Call `raveSystem.update(120f, inventory, 5)`. Verify
   `raveSystem.isPoliceAlerted()` returns `true`.

6. **RaveSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `raveSystem` field is non-null. Simulate 1 frame in PLAYING
   state. Verify no exception is thrown.

## Wire StreetEconomySystem into the Game Loop

`StreetEconomySystem` (860 lines, `ragamuffin.core.StreetEconomySystem`) is fully implemented
but never instantiated in `RagamuffinGame`. Without it, NPCs have no needs, no street dealing
occurs, no protection rackets run, and market disruption events never fire — the entire black
market economy is dead code. All of its integration points (FactionSystem, WeatherSystem,
NotorietySystem, RumourNetwork, DisguiseSystem) are already wired.

### Changes required in `RagamuffinGame.java`

1. **Declare the field** (alongside other system fields, after `graffitiSystem`):
   ```java
   // Issue #XXX: Street economy — NPC needs, black market dealing, protection rackets
   private StreetEconomySystem streetEconomySystem;
   ```

2. **Instantiate in `initGame()`** (after `rumourNetwork` and `notorietySystem` are created):
   ```java
   streetEconomySystem = new StreetEconomySystem();
   ```

3. **Call `update()` each frame** in the main game-loop update block (alongside
   `wantedSystem.update(...)` and `notorietySystem.update(...)`):
   ```java
   streetEconomySystem.update(delta, npcManager.getNPCs(), player,
       weatherSystem.getCurrentWeather(),
       notorietySystem.getTier(),
       inventory, rumourNetwork,
       type -> achievementSystem.unlock(type));
   ```

4. **Wire the E-key deal interaction** in `handleInteract()`, immediately after the
   existing NPC interaction block. When `interactionSystem.findNPCInRange(...)` returns
   a non-null NPC and no other UI is open, attempt a deal:
   ```java
   if (targetNPC != null && streetEconomySystem.hasRelevantNeed(targetNPC,
           inventory.getSelectedItem())) {
       StreetEconomySystem.DealResult result = streetEconomySystem.attemptDeal(
           targetNPC, inventory, player,
           factionSystem.getRespect(targetNPC.getFaction()),
           disguiseSystem.isDisguiseActive(),
           notorietySystem.getTier(),
           rumourNetwork, type -> achievementSystem.unlock(type));
       if (result != StreetEconomySystem.DealResult.NO_DEAL) {
           String msg = streetEconomySystem.getLastDealMessage();
           if (msg != null) tooltipSystem.showMessage(msg, 3.0f);
       }
   }
   ```

5. **Wire crime events** — when the player commits a visible crime (block smash near NPCs,
   NPC punch), call:
   ```java
   streetEconomySystem.onCrimeEvent(x, y, 8f, 0.3f, npcManager.getNPCs());
   ```
   alongside the existing `disguiseSystem.notifyCrime(...)` call.

6. **Reset on restart** in `restartGame()`:
   ```java
   streetEconomySystem = new StreetEconomySystem();
   ```

### Integration tests — implement these exact scenarios

1. **NPC needs accumulate each frame**: Initialise `RagamuffinGame` (headless). Spawn a
   CIVILIAN NPC. Advance 300 in-game seconds via `streetEconomySystem.update(300f, ...)`.
   Verify `streetEconomySystem.getNeedScore(npc, NeedType.HUNGRY)` is greater than 0.

2. **Deal completes when player holds matching item**: Give the player a `Material.GREGGS_PASTRY`.
   Set an NPC's HUNGRY need to 60 via `streetEconomySystem.setNeedScore(npc, NeedType.HUNGRY, 60f)`.
   Simulate pressing E near the NPC. Verify `DealResult` is not `NO_DEAL` and the pastry is
   removed from the player's inventory.

3. **Market event spikes price**: Call `streetEconomySystem.triggerMarketEvent(MarketEvent.GREGGS_STRIKE, ...)`.
   Verify `streetEconomySystem.getEffectivePrice(Material.GREGGS_PASTRY, 0, false, 0)` is
   at least 4× `streetEconomySystem.getBasePrice(Material.GREGGS_PASTRY)`.

4. **Racket passive income deposits coin**: Call `streetEconomySystem.startRacket(LandmarkType.GREGGS, ...)`.
   Advance 60 real seconds via repeated `update()` calls. Verify at least 1 `Material.COIN`
   has been added to the player's inventory.

5. **Crime event raises SCARED need**: Set an NPC's SCARED need to 0. Call
   `streetEconomySystem.onCrimeEvent(npc.getX(), npc.getZ(), 8f, 0.3f, npcs)`. Verify
   `streetEconomySystem.getNeedScore(npc, NeedType.SCARED)` is greater than 0.

6. **StreetEconomySystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `streetEconomySystem` field is non-null. Simulate 1 frame in
   PLAYING state. Verify no exception is thrown.

---

## Wire WitnessSystem into the Game Loop

`WitnessSystem` (Issue #765, `src/main/java/ragamuffin/core/WitnessSystem.java`) is a
490-line system that manages witnesses, evidence props, CCTV tapes, and informant mechanics.
It is **never instantiated** in `RagamuffinGame.java` — all 490 lines are dead code. Players
commit crimes, but no NPCs ever transition to WITNESS state, no evidence props ever spawn,
and CCTV tapes never activate.

### What needs to be done

1. **Declare and instantiate** in `RagamuffinGame`:
   ```java
   private WitnessSystem witnessSystem;
   // in create():
   witnessSystem = new WitnessSystem();
   witnessSystem.setCriminalRecord(player.getCriminalRecord());
   witnessSystem.setRumourNetwork(rumourNetwork);
   witnessSystem.setAchievementSystem(achievementSystem);
   ```

2. **Call `update()` every frame** in the PLAYING branch of `render()`:
   ```java
   witnessSystem.update(delta, npcManager.getNPCs(), player);
   ```

3. **Wire crime events** — wherever the player commits a visible crime (block punch near
   NPCs, NPC punch), call `registerCrime` and `notifyCrime` alongside the existing
   `disguiseSystem.notifyCrime(...)` call:
   ```java
   witnessSystem.registerCrime(
       player.getPosition().x, player.getPosition().y, player.getPosition().z,
       npcManager.getNPCs(), player);
   witnessSystem.notifyCrime(player.getPosition().x, player.getPosition().z);
   ```

4. **Wire CCTV tape stealing** — in `handleInteract()`, when the player presses E near an
   office or off-licence, attempt to steal a tape:
   ```java
   if (witnessSystem.stealCctvTape(player.getPosition().x, player.getPosition().z)) {
       tooltipSystem.showMessage("CCTV tape stolen — no evidence.", 3.0f);
       inventory.add(Material.CCTV_TAPE, 1);
   }
   ```

5. **Reset on restart** in `restartGame()`:
   ```java
   witnessSystem = new WitnessSystem();
   witnessSystem.setCriminalRecord(player.getCriminalRecord());
   witnessSystem.setRumourNetwork(rumourNetwork);
   witnessSystem.setAchievementSystem(achievementSystem);
   ```

### Integration tests — implement these exact scenarios

1. **NPC witnesses a crime and transitions to WITNESS state**: Spawn a CIVILIAN NPC at
   (10, 1, 10). Place the player at (10, 1, 14) (within `WITNESS_LOS_RANGE`). Call
   `witnessSystem.registerCrime(10, 1, 14, npcs, player)`. Call
   `witnessSystem.update(0.1f, npcs, player)`. Verify the NPC's state is
   `NPCState.WITNESS`.

2. **Witness reports after delay**: Continue the above scenario. Call
   `witnessSystem.update(WITNESS_REPORT_DELAY + 1f, npcs, player)`. Verify the player's
   `CriminalRecord` has at least one entry added by the witness report.

3. **CCTV tape activates on nearby crime**: Spawn a `EvidenceType.CCTV_TAPE` evidence prop
   at (12, 1, 10) via `witnessSystem.spawnEvidence(...)`. Call
   `witnessSystem.notifyCrime(10, 10)`. Verify `witnessSystem.isCctvHot()` returns `true`.

4. **Stealing CCTV tape cancels evidence**: With a hot CCTV tape at (12, 1, 10), call
   `witnessSystem.stealCctvTape(12, 10)`. Verify the return value is `true` and that after
   `update(200f, npcs, player)` no new `WITNESSED_CRIMES` entry is added to the player's
   `CriminalRecord`.

5. **WitnessSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `witnessSystem` field is non-null. Simulate 1 frame in PLAYING
   state. Verify no exception is thrown.

## Wire JobCentreSystem & JobCentreUI into the Game Loop

**Goal**: `JobCentreSystem` (609 lines, Issue #795) is fully implemented but never
instantiated in `RagamuffinGame`. Its `update()` is never called, `trySignOn()` is
never triggered by the E key, `JobCentreUI.render()` is never drawn, and the
sign-on → debt-collector loop never fires. Players cannot access Universal Credit,
sign-on windows never open, the `DEBT_COLLECTOR` NPC never spawns, and the
BUREAUCRACY skill track (already in `StreetSkillSystem`) has no effect. This is a
core economic survival loop that has been dead code since Issue #795 was merged.

`JobCentreSystem` depends on: `TimeSystem`, `CriminalRecord` (from player),
`NotorietySystem`, `FactionSystem`, `RumourNetwork`, `NewspaperSystem`,
`StreetSkillSystem`, `WantedSystem`, `NPCManager` — all of which are now wired in.
`NewspaperSystem` must also be instantiated (it is currently dead code too).

### What needs wiring

1. **Instantiate `NewspaperSystem`** in `RagamuffinGame` (required by `JobCentreSystem`):
   ```java
   private NewspaperSystem newspaperSystem;
   // in initGame():
   newspaperSystem = new NewspaperSystem();
   ```

2. **Instantiate `JobCentreSystem`** in `RagamuffinGame` after all dependencies exist:
   ```java
   private JobCentreSystem jobCentreSystem;
   private ragamuffin.ui.JobCentreUI jobCentreUI;
   // in initGame():
   jobCentreSystem = new JobCentreSystem(
       timeSystem,
       player.getCriminalRecord(),
       notorietySystem,
       factionSystem,
       rumourNetwork,
       newspaperSystem,
       player.getStreetSkillSystem(),   // or however StreetSkillSystem is accessed
       wantedSystem,
       npcManager,
       new java.util.Random());
   jobCentreUI = new ragamuffin.ui.JobCentreUI();
   ```

3. **Call `update()` each frame** inside the PLAYING state update block:
   ```java
   jobCentreSystem.update(delta, player, npcManager.getNPCs());
   ```

4. **Wire the E-key interact hook** — in `handleInteract()`, when near the JobCentre
   landmark, attempt sign-on:
   ```java
   if (nearLandmark(LandmarkType.JOB_CENTRE, 4f)) {
       JobCentreSystem.SignOnResult result = jobCentreSystem.trySignOn(player, inventory);
       if (result != null) {
           jobCentreUI.show(result, jobCentreSystem.getRecord());
           tooltipSystem.showMessage(result.getMessage(), 3.0f);
       }
   }
   ```

5. **Render `JobCentreUI`** in the 2D overlay pass (after `spriteBatch.begin()`):
   ```java
   if (jobCentreUI.isVisible()) {
       jobCentreUI.render(spriteBatch, shapeRenderer, font,
           Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
   }
   ```

6. **Wire ESC / E to close the UI** — in the ESC handler:
   ```java
   if (jobCentreUI.isVisible()) { jobCentreUI.hide(); return; }
   ```

7. **Wire debt-collector punch callback** — when the player punches an NPC of type
   `DEBT_COLLECTOR`, call:
   ```java
   jobCentreSystem.onDebtCollectorPunched(achievementSystem::award);
   ```

8. **Reset on restart** in `restartGame()`:
   ```java
   newspaperSystem = new NewspaperSystem();
   jobCentreSystem = new JobCentreSystem(
       timeSystem, player.getCriminalRecord(), notorietySystem,
       factionSystem, rumourNetwork, newspaperSystem,
       player.getStreetSkillSystem(), wantedSystem, npcManager, new java.util.Random());
   jobCentreUI = new ragamuffin.ui.JobCentreUI();
   ```

### Integration tests — implement these exact scenarios

1. **Sign-on window opens at correct time**: Initialise `RagamuffinGame` in headless
   mode. Set `timeSystem` to day 3, hour 9.0. Call
   `jobCentreSystem.update(0.1f, player, emptyList)`. Verify
   `jobCentreSystem.isSignOnWindowOpen()` returns `true`.

2. **Missed sign-on increments sanction level**: Advance time past the sign-on window
   (day 3, hour 10.1). Call `jobCentreSystem.update(0.1f, player, emptyList)`. Verify
   `jobCentreSystem.getRecord().getSanctionLevel()` is 1.

3. **Debt collector spawns at sanction level 3**: Call `update` three times with missed
   sign-on windows (days 3, 6, 9). Verify `jobCentreSystem.getRecord().getSanctionLevel()`
   equals 3. Verify `npcManager.getNPCs()` contains an NPC with type `DEBT_COLLECTOR`.

4. **Successful sign-on pays UC and resets mission**: Set the player's inventory coin
   count to 0. Advance to sign-on window. Call `jobCentreSystem.trySignOn(player, inventory)`.
   Verify the return value is non-null. Verify `inventory` coin count increased by at
   least 7 (base UC payment). Verify sanction level remains 0.

5. **JobCentreSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `jobCentreSystem` field is non-null. Simulate 1 frame in
   PLAYING state. Verify no exception is thrown.

## Wire BootSaleSystem & BootSaleUI into the Game Loop

**Goal**: `BootSaleSystem` (604 lines, Issue #789) and its companion `BootSaleUI` are
fully implemented but never instantiated in `RagamuffinGame`. The underground auction
economy is completely dead code: the daily lot schedule never generates, NPC bidders
never compete, the player can never bid or buy-now, and winning a lot never triggers
police, rumours, or TRADING skill XP. The `BOOT_SALE_OPEN` game state already exists
in `GameState` but is never entered. This is a major economy and social gameplay loop
that players have no access to.

`BootSaleSystem` depends on: `TimeSystem`, `FenceValuationTable`, `FactionSystem`,
`RumourNetwork`, `NoiseSystem`, `NotorietySystem`, `WantedSystem`,
`StreetSkillSystem` — all of which are now wired in. `FenceValuationTable` must also
be instantiated (it is currently dead code with no references in `RagamuffinGame`).

### What needs wiring

1. **Instantiate `FenceValuationTable`** in `RagamuffinGame` (required by
   `BootSaleSystem`):
   ```java
   private FenceValuationTable fenceValuationTable;
   // in initGame():
   fenceValuationTable = new FenceValuationTable();
   ```

2. **Instantiate `BootSaleSystem`** in `RagamuffinGame` after all dependencies exist:
   ```java
   private BootSaleSystem bootSaleSystem;
   private ragamuffin.ui.BootSaleUI bootSaleUI;
   // in initGame():
   bootSaleSystem = new BootSaleSystem(
       timeSystem,
       fenceValuationTable,
       factionSystem,
       rumourNetwork,
       noiseSystem,
       notorietySystem,
       wantedSystem,
       player.getStreetSkillSystem(),
       new java.util.Random());
   bootSaleUI = new ragamuffin.ui.BootSaleUI(bootSaleSystem);
   ```

3. **Call `update()` each frame** inside the PLAYING state update block:
   ```java
   bootSaleSystem.update(delta, player, npcManager.getNPCs());
   ```

4. **Wire the E-key interact hook** — in `handleInteract()`, when near the
   `BOOT_SALE` landmark, open the auction UI:
   ```java
   if (nearLandmark(LandmarkType.BOOT_SALE, 5f)) {
       if (!bootSaleSystem.isVenueOpen()) {
           tooltipSystem.showMessage("Can't go in — you're too hot right now.", 2.5f);
       } else {
           bootSaleSystem.setPlayerInventory(inventory);
           bootSaleUI.show();
           state = GameState.BOOT_SALE_OPEN;
       }
   }
   ```

5. **Wire bidding keys** inside the `BOOT_SALE_OPEN` state input block:
   ```java
   // F key — bid +5
   if (Gdx.input.isKeyJustPressed(Keys.F)) {
       boolean ok = bootSaleSystem.playerBid(player, inventory, 5);
       if (!ok) tooltipSystem.showMessage("Not enough coin.", 1.5f);
   }
   // R key — bid +20
   if (Gdx.input.isKeyJustPressed(Keys.R)) {
       boolean ok = bootSaleSystem.playerBid(player, inventory, 20);
       if (!ok) tooltipSystem.showMessage("Not enough coin.", 1.5f);
   }
   // B key — buy now
   if (Gdx.input.isKeyJustPressed(Keys.B)) {
       boolean ok = bootSaleSystem.playerBuyNow(player, inventory);
       if (!ok) tooltipSystem.showMessage("Can't afford buy-now price.", 1.5f);
   }
   ```

6. **Render `BootSaleUI`** in the 2D overlay pass (after `spriteBatch.begin()`):
   ```java
   if (bootSaleUI != null && bootSaleUI.isVisible()) {
       bootSaleUI.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
   }
   ```

7. **Wire ESC to close the UI** — in the ESC handler:
   ```java
   if (bootSaleUI != null && bootSaleUI.isVisible()) {
       bootSaleSystem.passLot();
       bootSaleUI.hide();
       state = GameState.PLAYING;
       return;
   }
   ```

8. **Set venue position** after world generation so police spawning works:
   ```java
   ragamuffin.world.Landmark bootSaleLandmark = world.getLandmark(LandmarkType.BOOT_SALE);
   if (bootSaleLandmark != null) {
       bootSaleSystem.setVenuePosition(bootSaleLandmark.getX(), 0, bootSaleLandmark.getZ());
   }
   ```

9. **Poll lastTooltip** each frame to surface boot sale messages to the player:
   ```java
   String bsMsg = bootSaleSystem.getLastTooltip();
   if (bsMsg != null && !bsMsg.isEmpty()) {
       tooltipSystem.showMessage(bsMsg, 3.0f);
   }
   ```

10. **Reset on restart** in `restartGame()`:
    ```java
    bootSaleSystem = new BootSaleSystem(
        timeSystem, fenceValuationTable, factionSystem, rumourNetwork,
        noiseSystem, notorietySystem, wantedSystem,
        player.getStreetSkillSystem(), new java.util.Random());
    bootSaleUI = new ragamuffin.ui.BootSaleUI(bootSaleSystem);
    ```

### Integration tests — implement these exact scenarios

1. **Daily schedule generates on first update**: Construct a `BootSaleSystem` with a
   `TimeSystem` at day 1. Call `bootSaleSystem.update(0.1f, player, emptyList)`.
   Verify `bootSaleSystem.getDaySchedule().size()` is between `MIN_LOTS_PER_DAY` (6)
   and `MAX_LOTS_PER_DAY` (10).

2. **Player bid accepted when sufficient coin**: Add 50 COIN to the player's inventory.
   Call `update` to initialise the first lot. Call
   `bootSaleSystem.playerBid(player, inventory, 10)`. Verify it returns `true`.
   Verify the player's COIN count decreased by 0 (bids are promises, not immediate
   deductions — confirm via `getCurrentLot().getCurrentBidder()` equals `"Player"`).

3. **Buy-now concludes the lot immediately**: Initialise `BootSaleSystem`. Call `update`
   to start the first lot. Add sufficient COIN to the inventory. Call
   `bootSaleSystem.playerBuyNow(player, inventory)`. Verify the lot is concluded
   (`getCurrentLot()` returns null or is a different lot). Verify
   `bootSaleSystem.getLotsWonToday()` equals 1.

4. **Venue closed when player is wanted**: Initialise `BootSaleSystem` with a
   `WantedSystem`. Set player wanted stars to `WANTED_ENTRY_THRESHOLD` (1 star) via
   `wantedSystem.addWantedStars(1)`. Verify `bootSaleSystem.isVenueOpen()` returns
   `false`.

5. **BootSaleSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `bootSaleSystem` field is non-null. Simulate 1 frame in
   PLAYING state. Verify no exception is thrown.

## Wire PropertySystem into the Game Loop

**Goal**: `PropertySystem` (568 lines, Phase P / Issue #712) is fully implemented but
never instantiated or called anywhere in `RagamuffinGame.java`. Players cannot buy
buildings from the estate agent, repair them to earn passive income, respond to
rival faction takeovers, pay council rates, or face compulsory purchase. The entire
property ownership pillar — a core progression system referenced by `SquatSystem`,
`NeighbourhoodSystem`, and `FactionSystem` — is completely dead code.

`PropertySystem` depends on: `FactionSystem`, `AchievementSystem`, `TimeSystem` —
all of which are now wired in.

### What needs wiring

1. **Declare and instantiate** `private PropertySystem propertySystem;` in
   `RagamuffinGame`. Instantiate in `initGame()` after `factionSystem` and
   `achievementSystem`:
   ```java
   propertySystem = new PropertySystem(factionSystem, achievementSystem, timeSystem);
   ```

2. **Call `onDayTick()` from the midnight callback** so properties decay, income is
   collected, and council rates are charged:
   ```java
   propertySystem.onDayTick(currentDay, inventory, npcManager.getAllNPCs(),
       rumourNetwork, notorietySystem);
   ```

3. **Wire E key — estate agent interaction** — when the player presses E near an
   `NPCType.ESTATE_AGENT` and `propertySystem.isEstateAgentOpen(timeSystem)` is true,
   open a buy dialogue:
   ```java
   String msg = propertySystem.purchaseProperty(landmarkType, buildingX, buildingZ,
       inventory, notorietySystem);
   tooltipSystem.showMessage(msg, 3.0f);
   ```

4. **Wire E key — repair** — when the player presses E inside an owned building while
   holding BRICK or WOOD, call:
   ```java
   String msg = propertySystem.repairProperty(buildingX, buildingZ, inventory);
   tooltipSystem.showMessage(msg, 3.0f);
   ```

5. **Wire rival takeover defeat** — after punching off THUG NPCs near an owned
   building, call:
   ```java
   propertySystem.onThugsDefeated(buildingX, buildingZ);
   ```

6. **Wire rates payment** — when the player presses E at the JobCentre to pay rates:
   ```java
   boolean paid = propertySystem.payRates(inventory);
   tooltipSystem.showMessage(paid ? "Rates paid." : "Not enough coin for council rates.", 2.5f);
   ```

7. **Poll tooltip** each frame to surface property messages:
   ```java
   String propMsg = propertySystem.pollTooltip();
   if (propMsg != null && !propMsg.isEmpty()) {
       tooltipSystem.showMessage(propMsg, 3.0f);
   }
   ```

8. **Reset on restart** — call `propertySystem = new PropertySystem(factionSystem,
   achievementSystem, timeSystem);` inside `restartGame()`.

### Integration tests — implement these exact scenarios

1. **Estate agent open during business hours**: Create `PropertySystem` with a
   `TimeSystem` set to 10:00 AM. Verify `propertySystem.isEstateAgentOpen(timeSystem)`
   returns `true`. Set time to 20:00. Verify it returns `false`.

2. **Purchasing a property reduces inventory coins**: Add 60 COIN to inventory. Call
   `propertySystem.purchaseProperty(LandmarkType.TERRACED_HOUSE, 10, 10, inventory,
   notorietySystem)`. Verify `propertySystem.getPropertyCount() == 1`. Verify
   inventory COIN count decreased by `PropertySystem.BASE_PURCHASE_PRICE` (50).

3. **Daily decay reduces condition without repair**: Purchase a property. Call
   `propertySystem.onDayTick(1, inventory, emptyList, rumourNetwork, notorietySystem)`
   for 5 consecutive days. Verify that
   `propertySystem.getPropertyAt(10, 10).getCondition()` has decreased from
   `PropertySystem.INITIAL_CONDITION` (30) by at least
   `PropertySystem.DECAY_PER_DAY` (5) per missed day.

4. **Rival takeover marks building under attack**: Call
   `propertySystem.triggerTakeoverAttempt(10, 10)`. Verify
   `propertySystem.getPropertyAt(10, 10).isUnderTakeover()` is `true`. Call
   `propertySystem.onThugsDefeated(10, 10)`. Verify `isUnderTakeover()` returns
   `false`.

5. **PropertySystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `propertySystem` field is non-null. Simulate 1 frame in
   PLAYING state. Verify no exception is thrown.

## Wire HeistSystem into the Game Loop

`HeistSystem` (1012 lines, `src/main/java/ragamuffin/core/HeistSystem.java`) implements a
complete four-phase heist mechanic — **Casing → Planning → Execution → Fence** — for four
heistable landmarks (Jeweller, Off-licence, Greggs, JobCentre). The class is fully implemented
and compiles, but is never instantiated in `RagamuffinGame`; as a result, the player cannot
case or rob any building, alarm boxes and safes never fire, and no heist-related faction
impacts, rumours, or notoriety gains ever occur.

### What needs to be done

1. **Declare and instantiate** `private HeistSystem heistSystem;` in `RagamuffinGame`.
   Instantiate in `create()` (and reset in `restartGame()`) after `noiseSystem`,
   `factionSystem`, and `rumourNetwork` are available:
   ```java
   heistSystem = new HeistSystem();
   ```

2. **Call `update()` each frame** in the `PLAYING` state update block, after the other
   system updates:
   ```java
   heistSystem.update(delta, player, noiseSystem, npcManager, factionSystem,
       rumourNetwork, npcManager.getNPCs(), world, timeSystem.isNight());
   ```

3. **Wire F key — casing** — when the player presses F inside a heistable landmark,
   call `startCasing()` and surface the returned message as a tooltip:
   ```java
   LandmarkType inside = world.getLandmarkTypeAt(player.getPosition());
   if (inside != null) {
       String msg = heistSystem.startCasing(inside,
           world.getLandmarkCentre(inside), npcManager);
       if (msg != null) tooltipSystem.showMessage(msg, 3.0f);
   }
   ```

4. **Wire G key — execution** — when the player presses G and casing/planning is
   complete, call `startExecution()`:
   ```java
   if (heistSystem.startExecution()) {
       tooltipSystem.showMessage("Right. Let's do this.", 2.5f);
   }
   ```

5. **Wire block-break hook** — after every successful block break, notify
   `HeistSystem` so alarm boxes and CCTV trigger correctly:
   ```java
   heistSystem.onBlockBreak(breakPos, noiseSystem, npcManager, player);
   ```

6. **Wire E key — safe cracking** — when the player holds E near a safe during
   execution phase, call `updateSafeCracking()` each frame:
   ```java
   if (heistSystem.getPhase() == HeistSystem.HeistPhase.EXECUTION) {
       heistSystem.updateSafeCracking(delta, player.getPosition(), inventory,
           noiseSystem);
   }
   ```

7. **Wire heist completion** — when the player exits the building exclusion zone
   during the execution phase, call `completeHeist()`:
   ```java
   heistSystem.completeHeist(player, inventory, factionSystem, rumourNetwork,
       npcManager.getNPCs(), achievementSystem::award, notorietySystem);
   ```

8. **Wire HUD** — during the execution phase render a countdown timer using the
   existing `SpriteBatch`/`BitmapFont`:
   ```java
   if (heistSystem.getPhase() == HeistSystem.HeistPhase.EXECUTION) {
       String timerText = String.format("HEIST: %.0fs", heistSystem.getExecutionTimer());
       font.draw(spriteBatch, timerText, 20, screenHeight - 60);
   }
   ```

9. **Reset on restart** — call `heistSystem = new HeistSystem();` inside
   `restartGame()`.

### Integration tests — implement these exact scenarios

1. **Casing returns a message for a valid target**: Create `HeistSystem`. Call
   `startCasing(LandmarkType.JEWELLER, new Vector3(0,0,0), null)`. Verify the
   returned string is non-null and `getPhase() == HeistPhase.CASING`.

2. **Execution starts only from CASING phase**: Call `startExecution()` without
   first calling `startCasing()`. Verify it returns `false` and phase remains
   `IDLE`. Then call `startCasing(LandmarkType.GREGGS, ...)` followed by
   `startExecution()`. Verify it returns `true` and `getPhase() == EXECUTION`.

3. **Timer expires and floods police**: Call `startCasing(LandmarkType.JEWELLER, ...)`,
   then `startExecution()`. Force-set `executionTimer` to 0.01f via
   `setExecutionTimerForTesting(0.01f)`. Call `update(0.1f, player, ...)`. Verify
   `getPhase() == HeistPhase.FAILED` and at least 4 POLICE NPCs have been spawned
   via the `NPCManager`.

4. **Alarm box triggers on block break**: Call `startCasing(LandmarkType.JEWELLER,
   new Vector3(0,0,0), null)` and `startExecution()`. Call
   `onBlockBreak(new Vector3(3, 1, -4), noiseSystem, npcManager, player)` — within
   `ALARM_BOX_RANGE` of the alarm box at `(-3,1,-4)`. Verify `noiseSystem.getLevel()`
   has increased to >= 0.8f (alarm triggered).

5. **HeistSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `heistSystem` field is non-null. Simulate 1 frame in
   PLAYING state. Verify no exception is thrown.

## Wire CornerShopSystem into the Game Loop

`CornerShopSystem` (Issue #799, `ragamuffin.core.CornerShopSystem`) implements the full corner
shop economy — shop claiming, dynamic customer traffic, Marchetti rivalry, police raid heat
progression, runner NPC, and three-faction pressure. The class is fully implemented and
compiles, but is **never instantiated or referenced in `RagamuffinGame`**; as a result, the
player cannot claim or run a shop, no customer NPCs ever visit, Marchetti enforcement never
fires, and the entire grey/black-market shopkeeping loop is dead code.

### What needs to be done

1. **Declare and instantiate** `private CornerShopSystem cornerShopSystem;` in `RagamuffinGame`.
   Instantiate in `initGame()` (and reset in `restartGame()`) after `factionSystem`,
   `notorietySystem`, `rumourNetwork`, and `streetSkillSystem` are available:
   ```java
   cornerShopSystem = new CornerShopSystem();
   ```

2. **Call `update()` each frame** in the `PLAYING` state update block:
   ```java
   cornerShopSystem.update(delta,
       npcManager.getNPCs(),
       player,
       inventory,
       factionSystem,
       notorietySystem,
       rumourNetwork,
       player.getStreetSkillSystem(),
       achievementSystem::award);
   ```

3. **Wire E key — shop claiming** — when the player presses E on a derelict building
   (door interaction with `buildingCondition <= 49`) or while holding a `Material.SHOP_KEY`:
   ```java
   // Via SHOP_KEY in hotbar:
   if (inventory.getSelectedItem() == Material.SHOP_KEY) {
       if (cornerShopSystem.tryClaimShop(inventory)) {
           tooltipSystem.showMessage("Shop claimed. Time to hustle.", 3.0f);
       }
   }
   // Via derelict building door:
   String msg = cornerShopSystem.claimShopByInteraction(buildingCondition) ?
       "Shop's yours. Don't let the council find out." :
       "Can't claim this one.";
   tooltipSystem.showMessage(msg, 2.5f);
   ```

4. **Wire E key — open/close shop** — when the player is inside their claimed shop and
   presses E without targeting anything specific:
   ```java
   if (cornerShopSystem.hasShop()) {
       cornerShopSystem.openShop(achievementSystem::award);
       tooltipSystem.showMessage("Shop open for business.", 2.0f);
   }
   ```

5. **Wire daily tick** — call `onNewDay` indirectly via `update()` (already handled),
   but also wire the `NewspaperSystem` headline callback on revenue milestones by passing
   a lambda that calls `newspaperSystem.publishHeadline(...)` when daily revenue exceeds
   `CornerShopSystem.NEWSPAPER_HEADLINE_REVENUE`.

6. **Wire HUD** — during the PLAYING state render a compact shop status bar when the
   shop is open, using the existing `SpriteBatch`/`BitmapFont`:
   ```java
   if (cornerShopSystem.hasShop() && cornerShopSystem.isShopOpen()) {
       font.draw(spriteBatch,
           String.format("SHOP  Revenue: %d  Heat: %d%%",
               cornerShopSystem.getDailyRevenue(),
               cornerShopSystem.getHeat()),
           20, 80);
   }
   ```

7. **Reset on restart** — call `cornerShopSystem = new CornerShopSystem();` inside
   `restartGame()`.

### Integration tests — implement these exact scenarios

1. **Shop claimed via SHOP_KEY removes the key**: Give the player 1 `Material.SHOP_KEY`
   in their hotbar. Call `cornerShopSystem.tryClaimShop(inventory)`. Verify it returns
   `true`, `cornerShopSystem.hasShop()` is `true`, and the `SHOP_KEY` has been removed
   from inventory.

2. **Customer purchases stock and reduces inventory**: Claim shop (call
   `claimShopByInteraction(30)`). Add 3 `Material.CIDER` to the shop's stock via
   `cornerShopSystem.getShopUnit().addStock(Material.CIDER, 3, 4)`. Spawn a PUBLIC NPC
   within `CUSTOMER_ATTRACTION_RANGE`. Call `update(CUSTOMER_SCAN_INTERVAL + 0.1f, ...)`.
   Verify the NPC state is `NPCState.WALKING_TO_SHOP` or the stock count has decreased.

3. **Heat rises on dodgy sale**: Claim shop and add `Material.TOBACCO` to stock.
   Call `cornerShopSystem.recordSale(Material.TOBACCO, notorietySystem)`. Verify
   `cornerShopSystem.getHeat()` has increased by at least `HEAT_PER_DODGY_SALE` (2).

4. **Police raid fires at heat 100**: Claim shop. Force heat to 100 via
   `cornerShopSystem.setHeatForTesting(100)`. Call `update(0.1f, npcs, player, ...)`.
   Verify shop is no longer open (`isShopOpen()` returns `false`) and at least 1
   `NPCType.POLICE` NPC has been spawned near the shop.

5. **CornerShopSystem visible in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify `cornerShopSystem` field is non-null. Simulate 1 frame in
   PLAYING state. Verify no exception is thrown.

## Wire StallSystem into the game loop

`StallSystem` (820 lines, `ragamuffin/core/StallSystem.java`) is fully implemented but never
instantiated or updated in `RagamuffinGame.java`. The player cannot place a stall, stock it,
open it, or earn passive income — the entire market-stall economy is dead code.

### What needs to be wired

1. **Declare field** in `RagamuffinGame`:
   ```java
   private StallSystem stallSystem;
   ```

2. **Instantiate** in `create()` after `streetEconomySystem` is initialised:
   ```java
   stallSystem = new StallSystem();
   ```

3. **Update each frame** in the PLAYING branch of `render()` alongside the other economy
   systems (after `cornerShopSystem.update(...)` is a natural location):
   ```java
   if (stallSystem != null) {
       stallSystem.update(
           delta,
           npcManager.getNPCs(),
           player,
           weatherSystem.getCurrentWeather(),
           inventory,
           factionSystem,
           notorietySystem,
           player.getCriminalRecord(),
           achievementSystem::award,
           streetEconomySystem
       );
       String stallMsg = stallSystem.pollTooltip();
       if (stallMsg != null) tooltipSystem.showMessage(stallMsg, 3.0f);
   }
   ```

4. **Wire E key — place/open stall** — when the player presses E facing a placed
   `STALL_FRAME` block:
   ```java
   if (stallSystem != null && !stallSystem.isStallPlaced()) {
       // Try to place from hotbar
       if (inventory.hasItem(Material.STALL_FRAME)) {
           boolean placed = stallSystem.placeStall(
               targetBlock.x, targetBlock.y, targetBlock.z,
               world.getBlock(targetBlock.x, targetBlock.y - 1, targetBlock.z).name(),
               factionSystem, world);
           if (placed) {
               inventory.removeItem(Material.STALL_FRAME, 1);
               tooltipSystem.showMessage("Stall placed. Press E to open.", 2.5f);
           }
       }
   } else if (stallSystem != null && stallSystem.isStallPlaced() && !stallSystem.isStallOpen()) {
       stallSystem.openStallWithAchievement(achievementSystem::award);
       tooltipSystem.showMessage("Stall open. Time to shift some gear.", 2.0f);
   } else if (stallSystem != null && stallSystem.isStallOpen()) {
       stallSystem.closeStall();
       tooltipSystem.showMessage("Stall closed.", 1.5f);
   }
   ```

5. **Wire HUD** — render a compact stall status line when the stall is open:
   ```java
   if (stallSystem != null && stallSystem.isStallOpen()) {
       font.draw(spriteBatch,
           String.format("STALL  Sold: %d  Income: %d",
               stallSystem.getTotalSales(),
               stallSystem.getTotalIncome()),
           20, 60);
   }
   ```

6. **Reset on restart** — add `stallSystem = new StallSystem();` inside `restartGame()`.

### Integration tests — implement these exact scenarios

1. **StallSystem instantiated in game loop**: In a headless integration test, initialise
   `RagamuffinGame`. Verify the `stallSystem` field is non-null after `create()`.
   Simulate 1 PLAYING-state frame. Verify no exception is thrown.

2. **placeStall on PAVEMENT succeeds**: Construct a `StallSystem`. Call `placeStall(5, 1, 5,
   "PAVEMENT", factionSystem, world)` where the block at (5, 0, 5) is PAVEMENT. Verify it
   returns `true` and `isStallPlaced()` is `true`.

3. **placeStall on non-valid surface fails**: Call `placeStall(5, 1, 5, "GRASS", ...)`.
   Verify it returns `false` and `isStallPlaced()` is `false`.

4. **RAIN without awning closes stall and destroys stock**: Place the stall and open it.
   Add 2 items to stock. Call `update(0.1f, ..., Weather.RAIN, ...)`. Verify
   `isStallOpen()` is `false` and stock count is 0.

5. **Unlicensed trading spawns inspector after delay**: Place and open stall (no
   `MARKET_LICENCE`). Call `update(StallSystem.UNLICENSED_INSPECTOR_DELAY + 1f, npcs, ...)`.
   Verify at least one NPC with type `NPCType.MARKET_INSPECTOR` has been spawned.

---

## Update SPEC.md: Wire NewspaperSystem into the game loop

**Goal**: Connect the fully-implemented `NewspaperSystem` to the game loop so that
*The Daily Ragamuffin* tabloid publishes every evening at 18:00 game-time, NPCs react
to headlines, and the player can pick up and read newspapers.

`NewspaperSystem` (Issue #774) is instantiated in `RagamuffinGame.create()` and passed
to `JobCentreSystem`, but its `update()` method is **never called** — meaning no edition
is ever published, no NPC ever reacts to a headline, and no `Material.NEWSPAPER` item
ever spawns. The system is entirely dead code.

### Changes required in `RagamuffinGame.java`

1. **Call `newspaperSystem.update()` each PLAYING-state frame** inside the main game-loop
   update block (alongside `notorietySystem.update()`, `rumourNetwork.update()`, etc.):
   ```java
   NPC barmanNpc = npcManager.getNPCsByType(NPCType.BARMAN).stream().findFirst().orElse(null);
   newspaperSystem.update(
       delta,
       timeSystem.getCurrentHour(),
       timeSystem.getCurrentDay(),
       notorietySystem,
       wantedSystem,
       rumourNetwork,
       barmanNpc,
       factionSystem,
       fenceSystem,
       streetEconomySystem,
       player.getCriminalRecord(),
       npcManager.getNPCs(),
       type -> achievementSystem.award(type)
   );
   ```

2. **Spawn `Material.NEWSPAPER` items at publication time** — after `update()` triggers a
   new edition, drop a stack of `Material.NEWSPAPER` at the off-licence/newsagent landmark
   position (and on park benches, letterboxes of terraced houses) so the player can
   physically collect them.

3. **Handle `E` key on `Material.NEWSPAPER` in inventory** — pressing **R** (or **E**)
   with a `NEWSPAPER` item selected opens a simple overlay UI showing the headline, briefs,
   and classifieds for that edition. Trigger `newspaperSystem.pickUpNewspaper()` on
   collection.

4. **NPC reads newspaper behaviour** — each frame, when an NPC walks over a `NEWSPAPER`
   item on the ground (via `SmallItemRenderer`/prop proximity), call
   `newspaperSystem.onNpcReadsNewspaper(npc, newspaperSystem.getLatestPaper(), rumourNetwork, wantedSystem)`.

5. **Reset on restart** — add `newspaperSystem = new NewspaperSystem();` inside
   `restartGame()`.

6. **Record infamy events** — hook into existing crime systems to call
   `newspaperSystem.recordEvent(new InfamyEvent(...))` after:
   - A Wanted Level 3+ chase resolved (escape or arrest)
   - A heist completes (success or fail)
   - A Greggs raid occurs
   - A GangTerritorySystem turf war resolves

### Integration tests — implement these exact scenarios

1. **NewspaperSystem instantiated and updated in game loop**: In a headless integration
   test, initialise `RagamuffinGame`. Verify `newspaperSystem` is non-null after `create()`.
   Simulate 1 PLAYING-state frame. Verify no exception is thrown.

2. **Edition published at 18:00**: Construct `NewspaperSystem`. Record one `InfamyEvent`
   (infamyScore=8, actionType="CHASE"). Call `update(0.1f, 17.85f, 1, ...)` — verify no
   paper is published yet (`getLatestPaper()` is null). Call `update(0.1f, 18.05f, 1, ...)`
   — verify `getLatestPaper()` is non-null and its headline contains "WANTED" or "FUGITIVE".

3. **Filler edition on zero infamy**: Construct `NewspaperSystem` with no recorded events.
   Advance time past 18:00. Verify `getLatestPaper().getHeadline()` equals
   `NewspaperSystem.PIGEON_FILLER`.

4. **NPC reacts to high-infamy headline**: Construct an NPC of type `NPCType.PUBLIC`.
   Construct a `Newspaper` with infamyScore=8. Call `onNpcReadsNewspaper(npc, paper, null,
   null)`. Verify `npc.getSpeechText()` contains "Did you see the paper?".

5. **Heightened Alert triggered on 7+ infamy publication**: Construct `WantedSystem` and
   `NewspaperSystem`. Record an event with infamyScore=9. Advance time past 18:00 (calling
   `update()` with `wantedSystem`). Verify `wantedSystem.isHeightenedAlert()` returns true.

---

## Wire PirateRadioSystem into the game loop

`PirateRadioSystem` (Issue #783) is fully implemented in
`src/main/java/ragamuffin/core/PirateRadioSystem.java` but is never instantiated,
updated, or connected to input/rendering in `RagamuffinGame.java`. Players cannot
broadcast, build the transmitter, or trigger any of the faction/notoriety effects.

### What needs to be done

1. **Declare a field** in `RagamuffinGame`:
   ```java
   // Issue #783: Pirate FM — underground radio station
   private PirateRadioSystem pirateRadioSystem;
   ```

2. **Instantiate in `initGame()`** after the faction and rumour systems are ready:
   ```java
   pirateRadioSystem = new PirateRadioSystem();
   ```

3. **Call `update()` each frame** inside `updatePlayingSimulation()`:
   ```java
   pirateRadioSystem.update(
       delta,
       npcManager.getNPCs(),
       wantedSystem.isPoliceNearby(player.getPosition(), npcManager.getNPCs()),
       notorietySystem,
       factionSystem,
       rumourNetwork,
       noiseSystem,
       achievementSystem::unlock
   );
   ```

4. **Wire 1–4 broadcast-action keys** — when the player is broadcasting (`pirateRadioSystem.isBroadcasting()`), route number key presses to `pirateRadioSystem.selectAction(BroadcastAction.fromNumber(n))` instead of hotbar slot selection.

5. **Wire `B` key** (or repurpose an existing unused key) in `InputHandler` so pressing it while standing near a TRANSMITTER prop calls `pirateRadioSystem.startBroadcast()` / `pirateRadioSystem.stopBroadcast()`.

6. **Display triangulation HUD bar** — when broadcasting, render a progress bar in `GameHUD` showing `pirateRadioSystem.getTriangulationProgress()` / `PirateRadioSystem.TRIANGULATION_MAX` with a warning flash above 80%.

7. **Reset on restart** — add `pirateRadioSystem = new PirateRadioSystem();` inside `restartGame()`.

### Integration tests — implement these exact scenarios

1. **PirateRadioSystem instantiated and updated in game loop**: In a headless integration
   test, initialise `RagamuffinGame`. Verify `pirateRadioSystem` is non-null after `create()`.
   Simulate 1 PLAYING-state frame. Verify no exception is thrown.

2. **Triangulation advances while broadcasting**: Construct `PirateRadioSystem` at power
   level 1. Call `startBroadcast()`. Call `update(1.0f, emptyList, false)` once. Verify
   `getTriangulationProgress()` equals `PirateRadioSystem.TRIANGULATION_RATE[1]` (±0.01f).

3. **Triangulation resets on broadcast stop**: Construct `PirateRadioSystem` at power level 2.
   Start broadcast, call `update(10f, emptyList, false)`. Verify triangulation > 0. Call
   `stopBroadcast()`. Verify `getTriangulationProgress()` == 0f.

4. **BIG_UP_THE_AREA action boosts faction respect**: Construct `PirateRadioSystem` and a
   `FactionSystem`. Start broadcast. Call `selectAction(BroadcastAction.BIG_UP_THE_AREA)`.
   Verify all three factions have had their respect increased by
   `PirateRadioSystem.RESPECT_BIG_UP_ALL`.

5. **Signal Van spawns at TRIANGULATION_MAX**: Construct `PirateRadioSystem` at power level 4.
   Start broadcast. Call `update(PirateRadioSystem.TRIANGULATION_MAX / TRIANGULATION_RATE[4] + 1f, emptyList, false)`.
   Verify `isSignalVanSpawned()` returns true.

## Wire WeatherNPCBehaviour into the game loop

**Goal**: `WeatherNPCBehaviour` (169 lines, Issue #698) is fully implemented but never
instantiated or called anywhere in `RagamuffinGame.java`. The `weatherSystem` already runs
and produces a `Weather` enum value each frame, but NPC reactions to weather — pedestrians
sheltering in rain, gangs becoming aggressive in storms, police patrols thinning in frost —
are completely dormant. Additionally, the frost-slip mechanic on ROAD/PAVEMENT blocks and
the police line-of-sight reduction in fog are implemented but never exercised.

**Changes required in `RagamuffinGame.java`:**

1. **Declare a field** alongside the other system fields:
   ```java
   private WeatherNPCBehaviour weatherNPCBehaviour;
   ```

2. **Instantiate in `create()`** after `weatherSystem` is created:
   ```java
   weatherNPCBehaviour = new WeatherNPCBehaviour(new java.util.Random());
   ```

3. **Re-instantiate in `resetGame()`** so a new game session starts fresh:
   ```java
   weatherNPCBehaviour = new WeatherNPCBehaviour(new java.util.Random());
   ```

4. **Call `applyWeatherBehaviour` once per second** in the main PLAYING update block,
   after `weatherSystem.update()` and `npcManager.update()`:
   ```java
   // Throttle to once per second — no need to run every frame
   weatherNPCTimer += delta;
   if (weatherNPCTimer >= 1.0f) {
       weatherNPCBehaviour.applyWeatherBehaviour(npcManager.getNPCs(), weatherSystem.getCurrentWeather());
       weatherNPCTimer = 0f;
   }
   ```
   Add `private float weatherNPCTimer = 0f;` as a field.

5. **Apply frost-slip** in the player movement update block in PLAYING state, after
   resolving the block underfoot:
   ```java
   BlockType underFoot = world.getBlock(/* player feet position */);
   boolean onRoad = underFoot == BlockType.ROAD || underFoot == BlockType.PAVEMENT;
   float slipChance = WeatherNPCBehaviour.getFrostSlipProbabilityPerSecond(
       weatherSystem.getCurrentWeather(), onRoad);
   if (slipChance > 0f && new java.util.Random().nextFloat() < slipChance * delta) {
       player.applyKnockback(camera.direction.cpy().scl(-1f));
       tooltipSystem.trigger(TooltipTrigger.FROST_SLIP);
   }
   ```

6. **Apply police LoS reduction** wherever `NPCManager` checks police detection range —
   replace the hard-coded range constant with:
   ```java
   float policeRange = WeatherNPCBehaviour.getEffectivePoliceLoS(
       NPCManager.POLICE_DETECTION_RANGE, weatherSystem.getCurrentWeather());
   ```

**Integration tests — implement these exact scenarios:**

1. **WeatherNPCBehaviour instantiated in game loop**: In a headless integration test,
   initialise `RagamuffinGame`, enter PLAYING state. Use reflection to assert the
   `weatherNPCBehaviour` field is non-null.

2. **Pedestrians shelter in rain**: Construct `WeatherNPCBehaviour`. Create a list of
   NPCs of type PUBLIC all in state WANDERING. Call `applyWeatherBehaviour(npcs,
   Weather.RAIN)` 100 times (simulating the stochastic trigger). Verify at least one NPC
   is now in state SHELTERING.

3. **Youth gang more aggressive in thunderstorm**: Construct `WeatherNPCBehaviour`.
   Create 10 YOUTH_GANG NPCs in state WANDERING. Call `applyWeatherBehaviour(npcs,
   Weather.THUNDERSTORM)` 100 times. Verify at least one is in state AGGRESSIVE.

4. **Police loS halved in fog**: Call `WeatherNPCBehaviour.getEffectivePoliceLoS(10f,
   Weather.FOG)`. Verify the result is 5.0f. Call with `Weather.CLEAR`. Verify 10.0f.

5. **Frost slip probability on road**: Call `getFrostSlipProbabilityPerSecond(Weather.FROST, true)`.
   Verify > 0. Call with `Weather.CLEAR` or `onRoad = false`. Verify == 0.

---

## Wire HeistSystem into the game loop

`HeistSystem` (Phase O / Issue #704) is fully implemented but never instantiated or
connected in `RagamuffinGame.java`. Players cannot case buildings, execute heists, or
fence hot loot — the entire four-phase robbery mechanic is dead code.

**What needs to be done in `RagamuffinGame`:**

1. **Declare field**: `private HeistSystem heistSystem;`
2. **Instantiate** in `initGame()`:
   ```java
   heistSystem = new HeistSystem();
   ```
3. **Update each frame** inside the `PLAYING` branch of `render()`:
   ```java
   heistSystem.update(delta, player, noiseSystem, npcManager, factionSystem,
       rumourNetwork, npcManager.getNPCs(), world, timeSystem.isNight());
   ```
4. **Wire F key (casing)** in the key-press handler: when `F` is pressed and the
   player is inside a heistable building (landmark type is JEWELLER, OFF_LICENCE,
   GREGGS, or JOB_CENTRE), call `heistSystem.startCasing(player, world,
   npcManager.getNPCs())` and show the returned tooltip via `tooltipSystem`.
5. **Wire G key (start execution)** in the key-press handler: call
   `heistSystem.startExecution(player, world)` and display any returned message.
6. **Wire E key (alarm/safe interaction)** in the existing E-key block: if the
   player's targeted prop is `ALARM_BOX` or `SAFE`, delegate to
   `heistSystem.tryInteractProp(player, inventory, targetedProp, world)`.
7. **Wire block-break noise spike**: in `BlockBreaker` resolution (where the player
   punches a block), call `heistSystem.onBlockBroken(blockPos, player, noiseSystem)`
   so alarm boxes can detect the noise.
8. **Wire fence loot price**: in `FenceSystem.getOffer()` (or wherever hot-loot
   valuation occurs), call `heistSystem.getHotLootMultiplier()` to apply the
   time-decaying price (100% → 50% → 25%).
9. **Reset on new day**: when `TimeSystem` ticks past 06:00, call `heistSystem.reset()`.

**Integration tests — implement these exact scenarios:**

1. **HeistSystem instantiated in game loop**: In a headless integration test,
   initialise `RagamuffinGame` and enter PLAYING state. Use reflection to assert
   the `heistSystem` field is non-null.

2. **Casing a building records a HeistPlan**: Construct `HeistSystem`. Place the
   player inside the jeweller landmark bounds. Call `startCasing(player, world,
   npcs)`. Assert `heistSystem.getActivePlan()` is non-null and its target type is
   `JEWELLER`.

3. **Alarm box triggers wanted status**: Create a `HeistSystem`. Start execution
   phase targeting the jeweller. Place an `ALARM_BOX` prop within 8 blocks of the
   player. Call `onBlockBroken(nearbyBlockPos, player, noiseSystem)`. Assert
   `noiseSystem.getNoiseLevel()` equals 1.0f and `player.isWanted()` is true.

4. **Safe cracking yields DIAMOND**: Create `HeistSystem`. Start execution on
   jeweller. Call `updateSafeCracking(delta=8.1f, player, inventory, noiseSystem,
   npcManager)` (simulating 8+ seconds hold). Assert `inventory` contains at least
   3 DIAMOND.

5. **Hot loot multiplier decays over time**: Create `HeistSystem`. Call
   `heistSystem.completeHeist()`. Assert `getHotLootMultiplier()` returns 1.0f.
   Advance `timeSinceHeistComplete` by 301 seconds (just past 5 in-game minutes).
   Assert multiplier returns 0.5f. Advance by 3600 seconds. Assert multiplier
   returns 0.25f.

---

## Wire NeighbourhoodSystem into the game loop

`NeighbourhoodSystem` (Issue #793) is fully implemented — it drives building decay,
gentrification waves, faction reactions, and the Neighbourhood Vibes state machine —
but is never instantiated or called in `RagamuffinGame.java`. All dynamic world
degradation (crumbling bricks, shattered windows, council luxury flats, Marchetti
shutters, Dystopia fog) is dead code.

**What needs to be done in `RagamuffinGame`:**

1. **Declare field**: `private NeighbourhoodSystem neighbourhoodSystem;`
2. **Instantiate** in `initGame()` after `factionSystem`, `turfMap`, `rumourNetwork`,
   and `achievementSystem` are ready:
   ```java
   neighbourhoodSystem = new NeighbourhoodSystem(
       factionSystem, turfMap, rumourNetwork, achievementSystem, new java.util.Random());
   ```
3. **Register buildings** at world-gen time — after `WorldGenerator` places
   landmarks, iterate `world.getLandmarks()` and call:
   ```java
   for (Landmark lm : world.getLandmarks()) {
       neighbourhoodSystem.registerBuilding(lm);
   }
   ```
4. **Update each frame** inside the `PLAYING` branch of `render()`:
   ```java
   neighbourhoodSystem.update(delta, world, npcManager.getNPCs(),
       notorietySystem.getNotoriety(),
       player.getPosition().x, player.getPosition().z);
   neighbourhoodSystem.checkMarchettiShutters(world);
   String tip = neighbourhoodSystem.pollTooltip();
   if (tip != null) tooltipSystem.show(tip);
   ```
5. **Wire block-break hook**: in the resolution path where `BlockBreaker` removes a
   block, call:
   ```java
   neighbourhoodSystem.onBlockBroken(blockX, blockZ);
   ```
6. **Wire graffiti hook**: in `GraffitiSystem` (or wherever graffiti is placed),
   call:
   ```java
   neighbourhoodSystem.onGraffitiPlaced(worldX, worldZ);
   ```
7. **Wire pirate radio hook**: when `PirateRadioSystem` completes a broadcast, call:
   ```java
   neighbourhoodSystem.onPirateRadioBroadcast(buildingX, buildingZ);
   ```
8. **Wire rave hook**: when `RaveSystem` starts or ends a rave, call:
   ```java
   neighbourhoodSystem.setRaveActive(true); // on start
   neighbourhoodSystem.setRaveActive(false); // on end
   ```
9. **Wire E-key interactions**: in the E-key handler add branches for:
   - `squatBuilding` — when player targets a condemned/derelict building
   - `tearDownCondemnedNotice` — when player targets a condemned notice prop
   - `organiseCommunityMeeting` — when player has a FLYER and enough NPCs nearby
   - `sellToDevelopers` — when player interacts with a developer NPC
10. **Wire BootSale price multiplier**: in `BootSaleSystem`/`BootSaleUI` apply
    `neighbourhoodSystem.getBootSalePriceMultiplier()` to auction prices, and add
    `neighbourhoodSystem.getBootSaleExtraBuyers()` extra buyers.
11. **Wire Marchetti shutter break**: in block-break resolution, if the broken block
    is `METAL_SHUTTER`, call `neighbourhoodSystem.onMarchettiShutterBroken(npcManager.getNPCs())`
    and show the returned tooltip.

**Integration tests — implement these exact scenarios:**

1. **NeighbourhoodSystem instantiated in game loop**: In a headless integration test,
   initialise `RagamuffinGame` and enter PLAYING state. Use reflection to assert
   the `neighbourhoodSystem` field is non-null.

2. **Building decay reduces condition over time**: Construct `NeighbourhoodSystem`.
   Register a building of type `LandmarkType.TERRACED_HOUSE` at (50, 50). Call
   `update(delta=1.0f, ...)` 80 times. Assert `getBuilding(50, 50).getCondition()`
   is less than `DEFAULT_CONDITION` (80).

3. **Condition drop transitions to CRUMBLING and places CRUMBLED_BRICK**: Construct
   `NeighbourhoodSystem` with a real `World`. Register a building at (50, 50) and
   set some `BlockType.BRICK` blocks near that position. Manually set condition to
   69 (just below `CONDITION_CRUMBLING`). Call `update(delta=1.1f, ...)`. Assert
   `getBuilding(50, 50).getState()` is `ConditionState.CRUMBLING`. Assert at least
   one block near (50, y, 50) is now `BlockType.CRUMBLED_BRICK`.

4. **Gentrification wave builds luxury flat**: Construct `NeighbourhoodSystem` with
   mocked `TurfMap` returning `Faction.THE_COUNCIL` owning >50% of territory.
   Register a building with condition 20. Manually advance `gentrifyTimer` to just
   above `GENTRIFY_INTERVAL_SECONDS`. Call `update(delta=1.0f, ...)`. Assert
   `getBuilding(...).isLuxuryFlat()` is true. Assert at least one `CONCRETE_PANEL`
   block was placed at the building position.

5. **Dystopia Vibes state silences ambient and unlocks achievement**: Construct
   `NeighbourhoodSystem` with a real `AchievementSystem`. Call `setVibes(5)`.
   Assert `getCurrentVibesState()` is `VibesState.DYSTOPIA`. Assert
   `isAmbientSilenced()` is true. Assert `achievementSystem` has
   `AchievementType.DYSTOPIA_NOW` unlocked.

## Wire MCBattleSystem, RaveSystem & SquatSystem into the game loop

`MCBattleSystem`, `RaveSystem`, and `SquatSystem` exist as fully implemented dead code
(Issues #714 and #716) but are never instantiated or updated in `RagamuffinGame`.
Without these systems the player cannot claim a squat, challenge MC champions, earn
MC Rank, or host illegal raves — gating an entire vertical slice of faction,
notoriety, and achievement progression that is otherwise completely inaccessible.

### What needs to be done

1. **Declare fields** in `RagamuffinGame`:
   ```java
   private SquatSystem squatSystem;
   private MCBattleSystem mcBattleSystem;
   private RaveSystem raveSystem;
   ```

2. **Instantiate in `initGame()`** (and mirror in `restartGame()`):
   ```java
   squatSystem    = new SquatSystem(achievementSystem, notorietySystem);
   mcBattleSystem = new MCBattleSystem(factionSystem, notorietySystem,
                        rumourNetwork, achievementSystem, new java.util.Random());
   raveSystem     = new RaveSystem(achievementSystem, notorietySystem, rumourNetwork);
   ```

3. **Update each frame** in the PLAYING branch of `render()`:
   ```java
   squatSystem.tickDay(timeSystem.getDayIndex(),
       notorietySystem.getTier(), npcManager.getNPCs(), inventory);
   mcBattleSystem.update(delta);
   raveSystem.update(delta, inventory,
       squatSystem.countAttendeesInSquat(npcManager.getNPCs()));
   ```

4. **Wire the E key** in `handleInteract()`:
   - If player is inside their unclaimed squat and holds ≥ 5 WOOD:
     call `squatSystem.claimSquat(...)`.
   - If `mcBattleSystem.canChallenge(targetNpc, inventory)`:
     call `mcBattleSystem.startBattle(champion, inventory)`.
   - If player is at squat entrance and a rave is active:
     call `raveSystem.disperseRave(inventory)`.

5. **Wire rave start**: on right-click/use of `Material.FLYER`:
   call `raveSystem.startRave(inventory, squatSystem.getVibe(),
       mcBattleSystem.getMcRank(), npcManager.getNearbyNpcs(player))`.

6. **Wire police spawn** when `raveSystem.isPoliceAlerted()` first becomes `true`:
   spawn 2 PCSO NPCs near the squat entrance via `npcManager`.

7. **Render `BattleBarMiniGame` overlay** in the 2D HUD pass when
   `mcBattleSystem.isBattleActive()` is true, using `SpriteBatch` / `ShapeRenderer`.

8. **Reset on restart** in `restartGame()`:
   re-instantiate all three systems with the same constructor calls as step 2.

### Integration tests — implement these exact scenarios

1. **Squat can be claimed**: Initialise `RagamuffinGame` headless. Give the player
   5 `Material.WOOD`. Place the player inside a derelict building (Condition ≤ 10).
   Simulate pressing **E**. Verify `squatSystem.hasSquat()` returns `true`.

2. **MC Battle starts on E with microphone**: Give the player a `Material.MICROPHONE`.
   Place a Marchetti MC Champion NPC adjacent to the player. Simulate pressing **E**.
   Verify `mcBattleSystem.isBattleActive()` returns `true`.

3. **Rave starts when player uses a FLYER with sufficient Vibe and MC Rank**:
   Set squat Vibe to 50 and MC Rank to 1. Give the player a `Material.FLYER`.
   Simulate using the flyer. Verify `raveSystem.isRaveActive()` returns `true`.
   Verify a `RumourType.RAVE_ANNOUNCEMENT` rumour has been seeded into nearby NPCs.

4. **Police spawn when rave runs past alert threshold**: Start a rave. Advance
   time by `RaveSystem.POLICE_ALERT_SECONDS` via `update()`. Verify
   `raveSystem.isPoliceAlerted()` is `true`. Verify at least 2 PCSO NPCs have
   been spawned near the squat entrance.

5. **Dispersing a rave early awards SWERVED_THE_FEDS achievement**: Start a rave.
   Advance time by less than `RaveSystem.POLICE_ALERT_SECONDS`. Simulate pressing
   **E** at the squat entrance. Verify `raveSystem.isRaveActive()` is `false`.
   Verify `achievementSystem` has `AchievementType.SWERVED_THE_FEDS` unlocked.

---

## Wire SkillsUI into the game loop

`SkillsUI` (Issue #787) exists as a complete overlay displaying all six street-skill
progress bars (BRAWLING, GRAFTING, TRADING, STEALTH, INFLUENCE, SURVIVAL) with tier
labels and perk descriptions. `StreetSkillSystem` is already instantiated on the
`Player` and awards XP throughout the game. `InputHandler` already detects the **K**
key (`isSkillsPressed()` / `resetSkills()`). However `SkillsUI` is **never
instantiated, never rendered, and never toggled** in `RagamuffinGame.java` — the
player has no way to view their character progression.

### Steps to wire in

1. **Declare** a `private ragamuffin.ui.SkillsUI skillsUI;` field in
   `RagamuffinGame`.

2. **Instantiate** in `create()` and in `restartGame()`:
   ```java
   skillsUI = new SkillsUI(player.getStreetSkillSystem());
   ```

3. **Toggle** in the PLAYING branch of `handleKeyboardInput()` (alongside the
   existing K-key detection from `InputHandler`):
   ```java
   if (inputHandler.isSkillsPressed()) {
       skillsUI.toggle();
       inputHandler.resetSkills();
   }
   ```

4. **Close** in `handleEscapeKey()` (mirror pattern used for `achievementsUI`,
   `questLogUI`, `criminalRecordUI`):
   ```java
   } else if (skillsUI.isVisible()) {
       skillsUI.hide();
   ```

5. **Render** in the 2D HUD pass of `renderHUD()` (after `achievementsUI`):
   ```java
   if (skillsUI.isVisible()) {
       skillsUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
   }
   ```

6. **Guard movement / mouse-look** via `isUIBlocking()`:
   ```java
   || skillsUI.isVisible()
   ```

7. **Hide on state transition** in `transitionToState()` and on game restart.

8. **Update SPEC Key Controls Reference** — add:
   - **K**: Open/close skills overlay

### Integration tests — implement these exact scenarios

1. **K key opens SkillsUI**: Initialise `RagamuffinGame` headless. State = PLAYING.
   Simulate pressing **K**. Verify `skillsUI.isVisible()` returns `true`. Verify
   mouse look is blocked (`isUIBlocking()` returns `true`).

2. **K key closes SkillsUI**: With `skillsUI` visible, simulate pressing **K**
   again. Verify `skillsUI.isVisible()` returns `false`.

3. **ESC closes SkillsUI**: Open `skillsUI`. Simulate pressing **ESC**. Verify
   `skillsUI.isVisible()` returns `false` and game state is still PLAYING.

4. **XP gained shows in SkillsUI**: Award 50 XP to `StreetSkillSystem.Skill.BRAWLING`
   via `player.getStreetSkillSystem().addXp(Skill.BRAWLING, 50)`. Open `skillsUI`.
   Verify the rendered data (via `StreetSkillSystem.getXp(Skill.BRAWLING)`) equals 50.

5. **SkillsUI hidden on restart**: Open `skillsUI`. Call `restartGame()`. Verify
   `skillsUI.isVisible()` returns `false`.

---

## Wire FruitMachine into the game loop

`FruitMachine` (Issue #696) is a complete pub mini-game class with full spin logic,
symbol slots, and coin payout rules. The `FRUIT_MACHINE` prop type is already
defined in `PropType` (with collision dimensions and `Material.SCRAP_METAL` drop),
and the `Material.COIN` currency is already used throughout the economy. However
`FruitMachine` is **never instantiated, never connected to E-key interaction, and
never invoked** in `RagamuffinGame.java` — players standing next to the fruit machine
prop in the pub get no interaction prompt and cannot play it.

### Steps to wire in

1. **Declare** a field in `RagamuffinGame`:
   ```java
   private ragamuffin.core.FruitMachine fruitMachine;
   ```

2. **Instantiate** in `create()` and in `restartGame()`:
   ```java
   fruitMachine = new FruitMachine(new java.util.Random());
   ```

3. **Add an E-key interaction handler** in the PLAYING branch of
   `handleInteractKey()` (alongside existing prop interactions). When the
   targeted prop is `PropType.FRUIT_MACHINE`:
   - If the player has at least 1 `Material.COIN` in inventory:
     - Deduct 1 COIN from inventory.
     - Call `fruitMachine.spin()` to get a `SpinResult`.
     - Add `spinResult.payout` COIN to inventory.
     - Display `spinResult.displayText` as an on-screen toast/message.
   - Otherwise display: `"Need 1 coin to play the fruit machine."`.

4. **Add an interaction prompt** in the HUD: when the player is looking at a
   `FRUIT_MACHINE` prop, show the tooltip `"[E] Play fruit machine (1 coin)"`.

5. **Update SPEC Key Controls Reference** — verify **E** already lists
   "Interact with objects/NPCs" (no change needed).

### Integration tests — implement these exact scenarios

1. **Playing with a coin triggers a spin**: Initialise `RagamuffinGame` headless.
   Give the player 1 `Material.COIN`. Place a `FRUIT_MACHINE` prop directly in
   front of the player. Simulate pressing **E**. Verify the player's COIN count
   has changed (either decreased by 1 on loss, or changed by `WIN_PAIR - 1` or
   `WIN_TRIPLE - 1` on win). Verify no exception is thrown.

2. **Playing without a coin shows an error message**: Give the player 0 COIN.
   Place a `FRUIT_MACHINE` prop in front of the player. Simulate pressing **E**.
   Verify the player's COIN count remains 0. Verify an error message (containing
   "coin") is queued for display.

3. **Triple match pays 9 coins**: Seed `FruitMachine` with a `Random` that always
   produces `0` (all three slots match symbol 0). Give the player 1 COIN. Simulate
   pressing **E** on the fruit machine. Verify the player's COIN count is
   `1 - FruitMachine.COST + FruitMachine.WIN_TRIPLE` = 9.

4. **Pair match pays 2 coins**: Seed `FruitMachine` with a `Random` that produces
   `[0, 0, 1]` (two matching slots). Give the player 1 COIN. Simulate pressing **E**.
   Verify the player's COIN count is `1 - FruitMachine.COST + FruitMachine.WIN_PAIR` = 2.

5. **No match loses the coin**: Seed `FruitMachine` with a `Random` that produces
   `[0, 1, 2]` (no match). Give the player 1 COIN. Simulate pressing **E**. Verify
   the player's COIN count is 0.

---

## Wire CriminalRecordUI into the render loop

**Goal**: Make the Criminal Record overlay actually visible when the player presses R.

`CriminalRecordUI` is fully implemented — it has `show()`, `hide()`, `toggle()`, and
`render(SpriteBatch, ShapeRenderer, BitmapFont, int, int)` — and is already instantiated
in `RagamuffinGame.initGame()` and toggled via the R key in the input handler.
However, its `render()` method is **never called** anywhere in the game loop, so pressing
R has no visible effect: the overlay is internally marked visible but never drawn to screen.

**Changes required** (in `RagamuffinGame.java` only):

1. **Add a render call** in the UI render section (the block that renders `inventoryUI`,
   `helpUI`, `craftingUI`, `achievementsUI`, `skillsUI`, `questLogUI`, `jobCentreUI`, etc.)
   immediately after the `skillsUI` render block:

   ```java
   // Issue #659: Render criminal record overlay if visible
   if (criminalRecordUI.isVisible()) {
       criminalRecordUI.render(spriteBatch, shapeRenderer, font, screenWidth, screenHeight);
   }
   ```

2. **ESC closes the overlay** — update the ESC-key handling block (where `inventoryUI.hide()`,
   `craftingUI.hide()`, etc. are called) to also call `criminalRecordUI.hide()` so ESC
   dismisses the overlay consistently with every other UI panel.

3. **Suppress other HUD elements while open** — in the condition that suppresses the hotbar
   and clock while overlays are open (the `!questLogUI.isVisible()` guard), add
   `|| criminalRecordUI.isVisible()` so the hotbar and clock do not render on top of the
   record screen.

No new classes, no new assets, no behaviour changes — this is purely a missing render call.

### Integration tests — implement these exact scenarios

1. **Criminal record overlay renders when R is pressed**: Initialise `RagamuffinGame`
   headless. Set game state to `PLAYING`. Simulate pressing R. Verify
   `criminalRecordUI.isVisible()` returns `true`. Call the render path (or verify the
   render method is invoked) — confirm no exception is thrown and the overlay is drawn.

2. **Criminal record overlay hides on second R press**: With the overlay visible (from
   test 1), simulate pressing R again. Verify `criminalRecordUI.isVisible()` returns
   `false`.

3. **ESC closes the criminal record overlay**: Show the overlay. Simulate pressing ESC.
   Verify `criminalRecordUI.isVisible()` returns `false` and game state remains `PLAYING`
   (ESC should close the overlay, not pause the game, when an overlay is open).

4. **Criminal record reflects committed crimes**: Record two crimes on the player's
   `CriminalRecord` (e.g. `BLOCKS_DESTROYED` and `NPCS_KILLED`). Open the overlay.
   Verify the rendered output (or the data fed to the renderer) contains non-zero counts
   for those crime types.

---

## Wire ChampionshipLadder into the game loop

**Problem**: `ChampionshipLadder` (Issue #801) is a complete ranked-fighter persistence system
that has never been instantiated or connected to any game logic. `Material.CHAMPIONSHIP_BELT`
exists but is never awarded. The `BOOKIE_BOARD` prop type references the ladder standings but
no code ever populates them. The `ChampionshipLadder` javadoc references a `FightNightSystem`
for Marchetti faction consequences, but that consequence logic is also absent from the game loop.

As a result, players can win MC Battles (which works via `MCBattleSystem`) but the persistent
championship ranking that should accumulate across battles — and the associated social/faction
consequences — never happen.

**Changes required** (primarily in `RagamuffinGame.java`):

1. **Declare and instantiate** a `ChampionshipLadder` field. Call `initDefault()` on it during
   `initGame()` and register the player with `registerFighter("Player")`.

2. **Connect to MCBattleSystem after each battle** — after `mcBattleSystem.startBattle(...)` or
   wherever a battle result is resolved, call
   `championshipLadder.updateAfterFight(winnerName, loserName)` to update rankings.

3. **Award CHAMPIONSHIP_BELT** — after updating the ladder, if
   `championshipLadder.isChampion("Player")` is true, add `Material.CHAMPIONSHIP_BELT` to the
   player's inventory (and show a tooltip). If Vinnie (`ChampionshipLadder.VINNIE_NAME`) reaches
   rank 1, apply a −15 respect penalty to the Marchetti faction via `factionSystem`.

4. **Passive notoriety bonus** — in the daily tick (where `propertySystem.onDayTick(...)` and
   `squatSystem.tickDay(...)` are called), if the player is rank 1 on the ladder, call
   `notorietySystem` to add +5 Notoriety for the day.

5. **BOOKIE_BOARD prop interaction** — when the player interacts (E key) with a prop of type
   `PropType.BOOKIE_BOARD`, display the top-3 ladder entries as a tooltip using
   `tooltipSystem.show(...)`.

No new classes are required — this is purely wiring the existing `ChampionshipLadder` class
into the existing systems.

### Integration tests — implement these exact scenarios

1. **Ladder initialises with 8 fighters including Vinnie**: After `initGame()`, verify
   `championshipLadder.size() == 8` and `championshipLadder.getRank(ChampionshipLadder.VINNIE_NAME) > 0`.
   Verify `"Player"` is also registered on the ladder.

2. **Winning an MC Battle updates the ladder**: Start a battle against a champion NPC and
   simulate winning (2 out of 3 bar hits). Verify `championshipLadder.getRank("Player")` has
   decreased (player climbed at least one rung) compared to before the battle.

3. **Reaching rank 1 awards the Championship Belt**: Manually call
   `championshipLadder.registerFighter("Player")` and manipulate the ladder so the player
   reaches rank 1. Verify `Material.CHAMPIONSHIP_BELT` is present in the player's inventory.

4. **Vinnie at rank 1 triggers Marchetti respect penalty**: Seed the ladder so Vinnie is at rank
   1 (or simulate a fight where Vinnie wins). Trigger the consequence check in the game loop.
   Verify the Marchetti faction's respect toward the player has decreased by 15.

5. **Daily champion bonus adds notoriety**: Put the player at rank 1. Simulate a day tick.
   Verify `notorietySystem` received a +5 notoriety call (check via a spy or by reading the
   notoriety value before and after the tick).

---

## Wire MCBattleSystem.pressAction() into the game loop input handler

`MCBattleSystem` implements a full timing-based BattleBar mini-game (`BattleBarMiniGame`),
but the player's action key press during an active battle is **never routed to
`mcBattleSystem.pressAction()`** in `RagamuffinGame`. As a result:

- The battle starts when the player challenges a champion NPC (E key + MICROPHONE).
- `mcBattleSystem.update(delta)` is called each frame, advancing the cursor.
- However, the player has **no way to hit the bar** — every round always times out as a
  miss after 4 seconds (`ROUND_TIMEOUT_SECONDS`), making the mini-game impossible to win
  through skill. The player loses every MC Battle regardless of timing.

### What needs to be done

In `RagamuffinGame.updatePlayingSimulation()` (or the input-handling block called from
`render()`), add a check: when `mcBattleSystem.isBattleActive()` is true and the player
presses the interact key (E / `inputHandler.isInteractPressed()`), call
`mcBattleSystem.pressAction(npcManager.getNPCs())` and surface the returned hit/miss
message via `tooltipSystem.showMessage(...)`. The interact key press must be consumed
(`inputHandler.resetInteract()`) so it is not also processed as a normal NPC interaction
on the same frame.

No new classes are required — this is purely routing an existing input event to an
existing method.

### Integration tests — implement these exact scenarios

1. **Player can win a round by pressing E at the right time**: Start an MC Battle. Call
   `mcBattleSystem.update()` until the cursor is inside the hit zone
   (`getCursorPos() >= getHitZoneStart()` and `<= getHitZoneStart() + getHitZoneWidth()`).
   Simulate pressing E. Verify `pressAction()` returns `"Hit!"` and
   `currentBar.wasHit()` is `true`.

2. **E press is consumed during active battle**: While `mcBattleSystem.isBattleActive()`
   is true, simulate pressing E. Verify that the normal NPC interaction path
   (`interactionSystem.interactWithNPC(...)`) is NOT called on that same frame (i.e. the
   input is consumed by the battle handler first).

3. **Round auto-misses on timeout without player input**: Start a battle. Call
   `mcBattleSystem.update(delta)` for `ROUND_TIMEOUT_SECONDS + 0.1f` seconds without
   pressing E. Verify `currentBar.isTimedOut()` is `true` and `currentBar.wasHit()` is
   `false`.

4. **Full battle can be won**: Simulate three rounds where the player presses E while
   the cursor is always inside the hit zone. Verify the battle resolves with
   `mcBattleSystem.wasLastResolvedPlayerWin()` returning `true`.

5. **Full battle can be lost**: Simulate three rounds where the player always presses
   E outside the hit zone (cursor at position 0.0 when hit zone does not include 0.0).
   Verify the battle resolves with `mcBattleSystem.wasLastResolvedPlayerWin()` returning
   `false`.

---

## Wire StreetSkillSystem into the game loop

`StreetSkillSystem` (Issue #787) is instantiated via `player.getStreetSkillSystem()` and
its data is passed into several other systems (JobCentreSystem, BootSaleSystem,
CornerShopSystem), but its own `update(float delta, Player player, List<NPC> allNPCs)` method
is **never called** anywhere in `RagamuffinGame.java`. This means:

- The **RALLY** perk (INFLUENCE Legend tier) is completely broken: the 30-second rally timer
  never ticks, followers never auto-disperse via `disperseFollowers()`, the rally cooldown
  never decrements, and `applyFollowerDeterrence()` is never executed each frame. Any NPCs
  recruited as followers will follow the player forever and never deter hostiles as intended.
- The `rallyCooldown` counter never decrements, so the player can never re-use RALLY even
  after the rally should have ended.

**What needs to be done:**

In `updatePlayingSimulation(float delta)` in `RagamuffinGame.java`, add a call to:

```java
player.getStreetSkillSystem().update(delta, player, npcManager.getNPCs());
```

This should be placed alongside the other per-frame system updates (e.g. near the
`gangTerritorySystem.update(...)` call). The same call should also be added to the PAUSED
and CINEMATIC branches so the rally timer keeps ticking when those states are active
(consistent with how other timers such as `wantedSystem.update()` and `rumourNetwork.update()`
are advanced in all three branches).

No new classes are required — this is purely routing the existing per-frame tick to an
existing method.

### Integration tests — implement these exact scenarios

1. **Rally timer auto-disperses followers**: Give the player INFLUENCE Legend tier
   (`streetSkillSystem.setSkillTier(StreetSkill.INFLUENCE, 4)`). Call `rally()` to start the
   rally. Spawn a PUBLIC NPC and set its state to `FOLLOWING_PLAYER`. Call
   `streetSkillSystem.update(delta, player, npcs)` for `RALLY_DURATION_SECONDS` total seconds
   in increments of 0.1f. Verify `getFollowers()` is empty and the NPC's state is
   `WANDERING` (not `FOLLOWING_PLAYER`).

2. **Rally cooldown decrements over time**: Give INFLUENCE Legend tier and call `rally()`.
   Advance the update loop for `RALLY_DURATION_SECONDS + 0.1f` seconds so the rally ends and
   `rallyCooldown` is set. Continue advancing for `RALLY_COOLDOWN_SECONDS - 1f` seconds.
   Verify `canRally()` returns `false`. Advance for another `1.1f` seconds. Verify
   `canRally()` returns `true`.

3. **Follower deterrence backs off hostile NPCs**: Give INFLUENCE Legend tier and call
   `rally()`. Spawn a YOUTH_GANG NPC in state `CHASING_PLAYER` within `RALLY_DETER_RADIUS`
   blocks of a follower NPC (state `FOLLOWING_PLAYER`). Call
   `streetSkillSystem.update(0.1f, player, npcs)`. Verify the YOUTH_GANG NPC's state is now
   `WANDERING` (deterred by the follower's presence).

4. **Game loop calls update each frame**: In a headless integration test, start the game in
   PLAYING state, place the player at the world centre, give INFLUENCE Legend tier, and call
   `rally()`. Simulate 60 frames at delta=1/60. Verify `getRallyTimer()` has decreased by
   approximately 1 second (within 0.1s tolerance), confirming the game loop is calling
   `streetSkillSystem.update()`.

5. **Rally does not tick in PAUSED state when update is absent**: (Regression guard) Confirm
   that after the fix is applied the rally timer also advances in the PAUSED branch by
   simulating 30 frames with state=PAUSED. Verify the timer decrements, matching the
   behaviour already specified for other timers (wantedSystem, rumourNetwork) in the paused
   branch.

---

## Wire NewspaperSystem.update() into the game loop

`NewspaperSystem` is instantiated in `RagamuffinGame` (line 542) and passed to
`JobCentreSystem` as a dependency, but its `update()` method is **never called** in any of
the three game-state branches (PLAYING, PAUSED, CINEMATIC). As a result:

- The daily newspaper is never published (no editions are ever generated).
- `MarketEvent` disruptions (GREGGS_STRIKE, LAGER_SHORTAGE, BENEFIT_DAY, etc.) that are
  triggered by `NewspaperSystem.update()` calling `streetEconomySystem.triggerMarketEvent()`
  are never fired.
- Rumours are never spread via newspaper publications.
- The `pickUpNewspaper()` path is effectively dead because no editions exist.

**What needs to be done:**

Call `newspaperSystem.update(delta, timeSystem.getTime(), timeSystem.getDayCount(),
notorietySystem, wantedSystem, rumourNetwork, <barmanNpc>, factionSystem, fenceSystem,
streetEconomySystem, player.getCriminalRecord(), npcManager.getNPCs(),
type -> achievementSystem.unlock(type))` each frame inside the PLAYING branch of
`RagamuffinGame.render()` (alongside `streetEconomySystem.update()` at approximately
line 2419). The same call (or a no-op guard) should be added to the PAUSED branch so the
publication timer advances consistently.

No new classes are required — this is purely routing the existing per-frame tick to an
existing method.

### Integration tests — implement these exact scenarios

1. **Newspaper published at 18:00 each in-game day**: Construct a `NewspaperSystem` and call
   `update()` each frame advancing `currentHour` from 0.0 to 18.1 with a fake delta of 1/60.
   Verify that `getLatestPaper()` returns a non-null `Newspaper` after the hour passes 18.0,
   and that `getLatestPaper().getEditionDay()` equals the `currentDay` passed in.

2. **Market event triggered via newspaper on GREGGS_STRIKE headline**: Set up a
   `NewspaperSystem` and `StreetEconomySystem`. Add a `PRESS_INFAMY` event to the newspaper
   that corresponds to a Greggs raid. Advance `currentHour` past 18.0. Verify
   `streetEconomySystem.getActiveEvent()` returns `MarketEvent.GREGGS_STRIKE`.

3. **Game loop calls update each frame in PLAYING state**: In a headless integration test,
   start the game in PLAYING state. Set the in-game hour to 17.9. Simulate 120 frames at
   delta=1/60 (advancing 2 seconds, crossing 18:00). Verify `newspaperSystem.getLatestPaper()`
   is non-null after the simulation, confirming `update()` is being called by the game loop.

4. **Publication fires only once per day**: Call `newspaperSystem.update()` repeatedly with
   `currentHour=18.5` and the same `currentDay=1`. Verify `getLatestPaper()` is published
   exactly once (not re-published on every frame).

5. **PAUSED branch advances the publication timer**: Set state to PAUSED. Set `currentHour`
   to 17.9. Simulate 120 frames at delta=1/60. Verify `getLatestPaper()` is non-null,
   confirming the PAUSED branch also calls `newspaperSystem.update()`.

## Wire FenceTradeUI into the render loop

`FenceTradeUI` (in `ragamuffin/ui/FenceTradeUI.java`) is fully implemented with a
`render(SpriteBatch, ShapeRenderer, BitmapFont, int, int, FenceSystem, Player, Inventory)`
method, but **its `render()` method is never called anywhere in `RagamuffinGame.java`**.

The UI is instantiated at line 406, shown at line 3643 (`fenceTradeUI.show()`), hidden at
line 4601 (`fenceTradeUI.hide()`), and its `isVisible()` flag is even checked at lines 2165
and 2332 to block other input while open — yet nothing is ever drawn to the screen. When the
player interacts with a Fence NPC, the trading panel is silently suppressed: the UI blocks
normal input but the player sees nothing and cannot sell stolen goods, buy contraband stock,
or take a contraband run.

### What needs to be done

1. **Add a `render()` call** in the HUD-rendering section of `RagamuffinGame.java`
   (after the other UI panels such as `jobCentreUI`, `bootSaleUI`, etc.) inside the
   `renderHUD()` method (or equivalent location where all 2-D overlays are drawn):

   ```java
   if (fenceTradeUI.isVisible()) {
       fenceTradeUI.render(spriteBatch, shapeRenderer, font,
               Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
               fenceSystem, player, inventory);
   }
   ```

2. The call must appear in **all three game-state branches** that draw the HUD
   (PLAYING, PAUSED, and the cinematic-camera branch) so the UI is always drawn when
   visible regardless of game state.

3. Ensure the `spriteBatch` is in the expected state (begin/end order) at the call site,
   consistent with the surrounding UI render calls.

No new classes or methods are needed — only this wiring.

### Integration tests — implement these exact scenarios

1. **FenceTradeUI renders when visible**: In a headless integration test, set
   `fenceTradeUI.show()`. Call the game render method for one frame. Verify that
   `fenceTradeUI.isVisible()` returns `true` and that the render loop did not throw
   any exception (confirming the render path is exercised without a null-pointer or
   batch state error).

2. **FenceTradeUI hidden by default**: Start the game in PLAYING state. Without
   interacting with a Fence NPC, verify `fenceTradeUI.isVisible()` returns `false`.

3. **FenceTradeUI shown after Fence interaction**: Simulate pressing `E` while the
   player is adjacent to an NPC of type FENCE. Verify `fenceTradeUI.isVisible()`
   becomes `true` after the interaction event is processed.

4. **FenceTradeUI closed by ESC**: With `fenceTradeUI` visible, simulate pressing ESC.
   Verify `fenceTradeUI.isVisible()` returns `false` after the key is processed.

5. **Normal input blocked while FenceTradeUI is open**: Set `fenceTradeUI.show()`.
   Simulate pressing W for 30 frames. Verify that player movement is blocked (position
   unchanged), confirming that the existing `isVisible()` guard in the input handler
   is in effect.

---

## Wire FenceSystem.update() into the game loop

`FenceSystem` is instantiated in `RagamuffinGame` and referenced by several systems
(including `NewspaperSystem`, `InteractionSystem`, and `FenceTradeUI`), but its
`update(float delta, Player player, List<NPC> allNpcs, int currentDayInt)` method is
**never called** in any of the three game-state branches (PLAYING, PAUSED, CINEMATIC).

This means:
- The Fence NPC's daily rotating stock never refreshes (always shows the same 3 items).
- Police-avoidance logic never runs — the Fence does not flee or abort a trade when
  a police NPC walks within `POLICE_FLEE_DISTANCE` (15 blocks).
- Contraband run countdown timers never decrement — timed delivery quests (`CONTRABAND_RUN_TIME_LIMITS`)
  can never expire, making them effectively unbeatable or permanently blocking the lock-out
  mechanic.
- The post-failure lock countdown (`lockTimer`) never decrements — once a contraband
  run fails, the Fence is permanently locked for that session.

### What needs to be done

In `RagamuffinGame.java`, call `fenceSystem.update()` in all three game-state update
branches — PLAYING, PAUSED, and CINEMATIC — passing `delta`, `player`,
`npcManager.getNPCs()`, and `timeSystem.getDayIndex()`.

Place the call alongside the other economy-system updates (e.g. after
`streetEconomySystem.update()` and `witnessSystem.update()`). No new classes or
methods are required — only this wiring.

### Integration tests — implement these exact scenarios

1. **Daily stock refreshes on day rollover**: Construct a `FenceSystem`. Call
   `update(0f, player, emptyList, 0)` to initialise day 0. Record the stock list.
   Call `update(0f, player, emptyList, 1)` to trigger day rollover. Verify that
   `getDailyStock()` returns a non-null list of exactly `FenceSystem.DAILY_STOCK_COUNT`
   items and that `getCurrentDay()` equals `1`.

2. **Police avoidance aborts open trade**: Construct a `FenceSystem`. Assign a Fence
   NPC via `setFenceNpc()` and open the trade UI (`openTradeUI()`). Place a police NPC
   within `POLICE_FLEE_DISTANCE` blocks of the Fence NPC. Call `update(0.1f, player,
   policeList, 0)`. Verify `isTradeUIOpen()` returns `false` and
   `isScaredByPolice()` returns `true`.

3. **Contraband run timer expires and fails the run**: Start a contraband run via
   `startContrabandRun(0, player)`. Record the player's rep. Call `update()` repeatedly
   with `delta = 1f` until the run time limit (`CONTRABAND_RUN_TIME_LIMITS[0]`) is
   exceeded. Verify `isContrabandRunActive()` returns `false`, and the player's rep has
   decreased by `FAILED_RUN_REP_PENALTY`.

4. **Lock timer decrements after a failed run**: After a contraband run fails (as in
   test 3 above), verify `isLocked()` returns `true`. Call `update()` repeatedly with
   `delta = 1f` until the lock timer (`IN_GAME_DAY_SECONDS`) elapses. Verify
   `isLocked()` returns `false`.

5. **FenceSystem.update() called in all three game-state branches**: In a headless
   integration test, start the game in PLAYING state. Advance one frame. Verify
   `fenceSystem.getCurrentDay()` has been initialised (i.e. `update()` was called).
   Transition to PAUSED and advance one frame. Verify the police-avoidance timer
   still decrements. Transition to CINEMATIC (opening sequence) and advance one frame.
   Verify the contraband run timer still decrements (i.e. the cinematic branch also
   calls `update()`).

---

## Wire BootSaleUI into the render loop

`BootSaleUI` is instantiated in `RagamuffinGame` and `bootSaleUI.show()` is called
when the player approaches the boot sale landmark and presses `E`. The game state is
correctly set to `GameState.BOOT_SALE_OPEN`. However, **`bootSaleUI.render()` is
never called anywhere in the game**. This means:

- When the player opens the boot sale, the screen goes blank (no overlay is drawn).
- The player has no way to see lots, place bids, or use the Buy Now button.
- The boot sale economic system (`BootSaleSystem`) runs correctly in the background
  (its `update()` is called) but the player cannot interact with it.
- `bootSaleUI.hide()` is also never called, so the UI remains silently "visible" in
  memory even after the player exits, which could cause issues if the hide path ever
  gains logic.

### What needs to be done

In `RagamuffinGame.java`:

1. Call `bootSaleUI.render(screenWidth, screenHeight)` inside the render loop,
   alongside the other UI overlays (e.g. after `fenceTradeUI.render()`). The call
   should be guarded by `bootSaleUI.isVisible()` or placed unconditionally (the method
   already guards internally with `if (!visible) return`).

2. Call `bootSaleUI.hide()` when the player closes the boot sale UI (ESC key press
   while `state == GameState.BOOT_SALE_OPEN`), and restore state to `PLAYING`.

No new classes or methods are required — only these two wiring calls.

### Integration tests — implement these exact scenarios

1. **BootSaleUI render is called when visible**: In a headless integration test,
   call `bootSaleUI.show()`. Invoke the render loop for one frame. Verify that no
   exception is thrown and that `bootSaleUI.isVisible()` returns `true` (confirming
   the render path is exercised without a null-pointer or batch state error).

2. **BootSaleUI hidden by default**: Start the game in PLAYING state. Without
   interacting with the boot sale landmark, verify `bootSaleUI.isVisible()` returns
   `false`.

3. **BootSaleUI shown when player opens boot sale**: With the game in PLAYING state,
   call `bootSaleSystem.setPlayerInventory(inventory)` and `bootSaleUI.show()`, then
   set `state = GameState.BOOT_SALE_OPEN`. Verify `bootSaleUI.isVisible()` returns
   `true`.

4. **BootSaleUI closed by ESC**: With `bootSaleUI.isVisible()` returning `true` and
   `state == BOOT_SALE_OPEN`, simulate pressing ESC. Verify `bootSaleUI.isVisible()`
   returns `false` and `state` has returned to `PLAYING`.

5. **Normal input blocked while BootSaleUI is open**: Set `bootSaleUI.show()` and
   `state = BOOT_SALE_OPEN`. Simulate pressing W for 30 frames. Verify that player
   movement is blocked (position unchanged), confirming that the BOOT_SALE_OPEN state
   does not pass through the normal movement-update path.

---

## Fix #897: Wire squat passive health regen into the game loop

**Goal**: Connect `SquatSystem.isRegenActive()` to the main game loop so that
players inside their squat at Vibe ≥ 20 (Barely Liveable tier) receive the
documented +1 health per minute passive regeneration.

### What needs wiring

`SquatSystem` has a fully implemented `isRegenActive(float playerX, float playerZ,
float radius)` method that returns `true` when the player is inside their claimed
squat and Vibe is at or above `VIBE_TIER_HABITABLE` (20). However, this method is
**never called** anywhere in `RagamuffinGame.java`. As a result the squat Vibe tier
table promise — "Player health regenerates +1/min while inside" at Vibe 20–39 — is
silently broken; no regen is ever applied.

### What to do

In the PLAYING-state update block of `RagamuffinGame.updateGameplay()` (where other
per-frame health effects such as `healingSystem.update()` are applied), add a squat
regen check:

```
if (squatSystem != null && squatSystem.isRegenActive(
        player.getPosition().x, player.getPosition().z, 15f)) {
    // +1 health per in-game minute (rate = 1/60 per real second)
    player.heal(delta / 60f);
}
```

The radius `15f` matches the bounding-box radius already used by
`countAttendeesInSquat()`. Apply the same regen in the PAUSED and CINEMATIC update
paths that already call `healingSystem.update()` (so regen is consistent across all
active states).

**Unit tests**: Verify `isRegenActive()` returns `true` inside the squat at Vibe ≥ 20,
and `false` when Vibe < 20 or player is outside the radius.

**Integration tests — implement these exact scenarios:**

1. **Squat regen applies at Vibe ≥ 20**: Claim a squat. Set Vibe to 25 using
   `squatSystem.setVibeDirectly(25, npcs)`. Set player position to the squat centre
   (within 15 blocks). Set player health to 50. Advance 60 real seconds (simulate
   frames). Verify player health increased by at least 1.

2. **No regen below Vibe 20**: Same setup but set Vibe to 15. Set player health to 50.
   Advance 60 real seconds. Verify player health has NOT increased from the squat
   regen path (health should remain ≤ 50 ignoring other healing).

3. **No regen when player is outside the squat**: Set Vibe to 25. Move player 20 blocks
   away from the squat centre (outside 15-block radius). Set health to 50. Advance 60
   real seconds. Verify health has NOT increased via squat regen.

---

## Fix #899: Wire random market event triggering into the game loop

**Goal**: Connect `StreetEconomySystem.triggerMarketEvent()` to the game loop so
that the five currently dead market events (LAGER_SHORTAGE, COLD_SNAP, BENEFIT_DAY,
COUNCIL_CRACKDOWN, MARCHETTI_SHIPMENT) fire randomly during gameplay.

### What needs wiring

`StreetEconomySystem` has six `MarketEvent` disruption events fully implemented,
each with price multipliers, NPC behaviour effects, and rumour seeding logic. The
system even holds a `Random` instance for this purpose. However, `triggerMarketEvent()`
is only ever called from `NewspaperSystem` for a single hardcoded case
(`GREGGS_STRIKE`). The other five events are **never triggered anywhere** in
`RagamuffinGame.java`. As a result the entire dynamic economy — price spikes, NPC
need surges, BENEFIT_DAY windfalls, COUNCIL_CRACKDOWN police pressure — is silently
broken; the player never sees any market disruption.

### What to do

In `RagamuffinGame`'s PLAYING-state update block (alongside the existing
`streetEconomySystem.update()` call), add a random market event scheduler. A
lightweight approach:

1. Add a private `marketEventCooldown` float field (reset to a random value in
   `[120f, 300f]` seconds after each event, and on game start).
2. Each frame, decrement `marketEventCooldown` by `delta`. When it reaches 0 and
   no event is already active (`streetEconomySystem.getActiveEvent() == null`):
   - Pick a random `MarketEvent` value (excluding `GREGGS_STRIKE` which is already
     handled by `NewspaperSystem`).
   - Call `streetEconomySystem.triggerMarketEvent(event, npcManager.getNPCs(), rumourNetwork)`.
   - Reset `marketEventCooldown` to a new random value in `[120f, 300f]`.
3. Apply the same cooldown decrement in the PAUSED and CINEMATIC update paths so
   time-based scheduling continues consistently across all active states.
4. Reset `marketEventCooldown` on new game / restart (alongside other system resets).

**Unit tests**: Verify that after cooldown expires with no active event,
`triggerMarketEvent` is called with a non-GREGGS_STRIKE event. Verify that when an
event is already active, no new event is triggered. Verify `marketEventCooldown` is
reset to a value in `[120, 300]` after triggering.

**Integration tests — implement these exact scenarios:**

1. **BENEFIT_DAY zeroes NPC BROKE needs**: Create a `StreetEconomySystem`. Populate
   three NPCs with BROKE need score 80. Call
   `streetEconomySystem.triggerMarketEvent(MarketEvent.BENEFIT_DAY, npcs, null)`.
   Call `streetEconomySystem.update(0.1f, npcs, player, null, 0, null, null, null)`.
   Verify all three NPCs now have BROKE need score 0.

2. **LAGER_SHORTAGE spikes BORED accumulation**: Create a `StreetEconomySystem`.
   Call `triggerMarketEvent(MarketEvent.LAGER_SHORTAGE, emptyList, null)`.
   Run `update()` for 10 seconds with one NPC. Verify the NPC's BORED need score is
   at least 2× the value it would have without the event active.

3. **Market event cooldown triggers automatically**: Construct a game loop harness
   with `streetEconomySystem`, `marketEventCooldown = 0.1f`, and no active event.
   Advance by 0.2 seconds. Verify that `streetEconomySystem.getActiveEvent()` is
   non-null (an event was triggered).

4. **No double-trigger while event active**: Trigger COLD_SNAP manually. Set
   `marketEventCooldown = 0`. Advance by 1 second. Verify `getActiveEvent()` is still
   COLD_SNAP (the active event was not replaced by the scheduler).

5. **COUNCIL_CRACKDOWN price effect**: Call
   `triggerMarketEvent(MarketEvent.COUNCIL_CRACKDOWN, emptyList, null)`.
   Call `getEffectivePrice(Material.CIGARETTE, -1, false, streetEconomySystem.getActiveEvent(), 0)`.
   Verify the returned price is `2×` the base price for CIGARETTE.

---

## Fix: COLD_SNAP market event must force WeatherSystem to COLD_SNAP state

The `MarketEvent.COLD_SNAP` enum documents: *"WeatherSystem forced to COLD_SNAP state
while event is active."* However this is never implemented. `StreetEconomySystem`
has no reference to `WeatherSystem`, and `RagamuffinGame` does not call
`weatherSystem.setWeather(Weather.COLD_SNAP)` when the event fires or restore the
previous weather when the event expires. The effect is purely cosmetic (warm-item
prices spike) but the game world remains whatever weather it was — NPCs don't feel
cold, `WarmthSystem` isn't triggered, and the `COLD_SNAP_CAPITALIST` achievement
scenario (sell warm item at 2× market price *during* cold snap) is much harder to
trigger organically because the cold weather doesn't actually arrive.

**Root cause**: `StreetEconomySystem.triggerMarketEvent()` and `endMarketEvent()` do
not take a `WeatherSystem` parameter and do not force/restore weather state.

**What needs to change**:

1. Add a `WeatherSystem` field to `StreetEconomySystem` (injected via a setter
   `setWeatherSystem(WeatherSystem ws)`) or pass it through `triggerMarketEvent()`.
2. When `COLD_SNAP` triggers, record the previous `Weather` value and call
   `weatherSystem.setWeather(Weather.COLD_SNAP)`.
3. When the COLD_SNAP event expires (timer hits 0 in `update()`), restore the
   previously recorded weather via `weatherSystem.setWeather(savedWeather)`.
4. `RagamuffinGame.initGame()` must call
   `streetEconomySystem.setWeatherSystem(weatherSystem)` after both systems are
   created.

**Unit tests**:

- `COLD_SNAP_triggers_weather_change`: Create `StreetEconomySystem` with a
  `WeatherSystem` injected. Call
  `triggerMarketEvent(MarketEvent.COLD_SNAP, emptyList, null)`. Verify
  `weatherSystem.getCurrentWeather() == Weather.COLD_SNAP`.
- `COLD_SNAP_restores_weather_on_expiry`: Trigger COLD_SNAP with a `WeatherSystem`
  that starts at `Weather.CLEAR`. Call `update()` for exactly
  `MarketEvent.COLD_SNAP.getDurationSeconds() + 0.1f` seconds. Verify
  `weatherSystem.getCurrentWeather() == Weather.CLEAR` (previous weather restored).
- `non_COLD_SNAP_event_does_not_change_weather`: Trigger `LAGER_SHORTAGE`. Verify
  `weatherSystem.getCurrentWeather()` is unchanged.

**Integration test**:

- **COLD_SNAP event forces cold weather**: In the game loop harness, set
  `weatherSystem` to `Weather.CLEAR`. Manually fire
  `streetEconomySystem.triggerMarketEvent(MarketEvent.COLD_SNAP, npcs, null)`.
  Advance the simulation by 1 second. Verify `weatherSystem.getCurrentWeather()`
  is `Weather.COLD_SNAP`. Advance by `MarketEvent.COLD_SNAP.getDurationSeconds()`
  more seconds. Verify `weatherSystem.getCurrentWeather()` returns to `Weather.CLEAR`.

---

## Feature: Busking System — Street Performance Economy

**Goal**: Let the player busk on the high street using a craftable instrument, earning coins
from passing NPCs, with a police licence-check threat, faction approval dynamics, and deep
integration with the existing NPC needs and reputation systems.

### Rationale

The `BUSKER` NPC type already exists (plays music on the high street), but there is no
equivalent mechanic for the player. The world has everything needed: a high street with foot
traffic, `StreetEconomySystem` NPC needs (BORED), `FactionSystem` respect modifiers, a police
threat model, and a `RumourNetwork` to spread the word. This feature adds a low-commitment
income stream that rewards creativity and risk management — and is very British.

### New Material: BUCKET_DRUM

```
Material.BUCKET_DRUM("Bucket Drum")
```

Craftable at the crafting menu: `1 SCRAP_METAL + 1 PLANKS`. Held item; equips in the active
hotbar slot. Tooltip on first pickup: *"Bucket drum. The percussion instrument of the
dispossessed."*

### New System: `BuskingSystem`

A new class `ragamuffin.core.BuskingSystem` manages all busking state.

#### Starting a Busk Session

- Player selects `BUCKET_DRUM` in hotbar and presses **E** near any PAVEMENT or ROAD block
  on the high street (within 30 blocks of any shop landmark).
- If a BUSKER NPC is already performing within 10 blocks, the start fails with speech:
  *"Oi — find your own pitch, mate."*
- If player Notoriety Tier ≥ 3, police recognise the player and a licence-check is
  automatically triggered within 30 seconds (see below).
- On session start:
  - `BUSKER_STARTED` rumour seeded into 3 nearby NPCs (`RumourType.GANG_ACTIVITY`,
    text: *"Someone's busking out front. Actually not bad."*)
  - Street Lads Respect +2 (they enjoy it)
  - Council Respect −1 (unlicensed street performance)
  - Achievement `STREET_PERFORMER` unlocked (first busk session started)

#### Income Accumulation

Every real second while busking, nearby NPCs contribute coins based on their `BORED` need
score (accessed via `StreetEconomySystem.getNeedScore(npc, NeedType.BORED)`):

```
incomePerSecond = Σ over NPCs within 8 blocks:
    clamp(npcBoredScore / 100f, 0f, 1f) × BASE_COIN_PER_NPC_PER_SECOND
```

Where `BASE_COIN_PER_NPC_PER_SECOND = 0.1f` (so a fully bored NPC contributes 1 coin
per 10 seconds; 6 bored NPCs yields ~1 coin every ~1.7 seconds).

Additionally:
- Each coin dropped reduces the contributing NPC's BORED need by 10 points (paying the
  busker satisfies their boredom).
- During LAGER_SHORTAGE market event: income ×1.5 (desperate people need entertainment).
- During BENEFIT_DAY market event: income ×2.0 (people have cash and are celebrating).
- GREGGS_STRIKE: income ×0.5 (everyone's miserable and hungry, not generous).

Coins are disbursed to the player's inventory in whole-coin increments each frame.

#### Police Licence Check

After `LICENCE_CHECK_DELAY_SECONDS = 60f` of continuous busking (or 30 seconds at
Notoriety Tier ≥ 3), a nearby POLICE or PCSO NPC is set to approach the player.
When the officer reaches within 3 blocks:

- Speech: *"You got a busking licence, sunshine?"*
- Player has two choices (presented via the existing speech interaction prompt):
  1. **Show Licence** (requires `Material.FAKE_ID` in inventory — consumes it):
     Police NPC says *"Right, carry on then"* and returns to PATROLLING. Session continues.
  2. **Leg It** (player presses ESC or moves away): Notoriety +5, WantedSystem +1 star,
     busk session ends, `BUCKET_DRUM` not consumed.
  3. **No Licence** (interact without FAKE_ID, or wait 10 seconds): Notoriety +10,
     WantedSystem +2 stars, Criminal Record entry `UNLICENSED_BUSKING`, busk session ends,
     `BUCKET_DRUM` confiscated (removed from inventory).

#### Session End Conditions

- Player moves more than 3 blocks from the start position.
- Player switches hotbar slot away from BUCKET_DRUM.
- Police confiscate the drum.
- Player is arrested.
- `GREGGS_STRIKE` market event ends (police crack down on loitering).

On natural session end (player walks away), `BUCKET_DRUM` is retained.

#### Fame Integration

Each full minute of uninterrupted busking awards +1 Street Reputation point (capped
at once per real-world session to prevent farming). At 5 consecutive minutes:
- Achievement `LIVING_WAGE` unlocked (tooltip: *"You've made more busking than a week
  at the JobCentre."*)
- A `LOOT_TIP` rumour is seeded into the barman NPC: *"There's a busker near the shops.
  Word is they're actually decent."*
- All FACTION_LIEUTENANT NPCs gain awareness of the player (+5 to their BORED satisfaction
  if they wander near the pitch).

#### Achievements

| Achievement | Condition |
|---|---|
| `STREET_PERFORMER` | Start first busk session |
| `LIVING_WAGE` | Busk for 5 consecutive minutes without police interruption |
| `BUCKET_LIST` | Earn 20 coins total across all busk sessions |
| `MOVE_ALONG_PLEASE` | Have BUCKET_DRUM confiscated by police |

#### Constants (all in `BuskingSystem`)

```java
public static final float BASE_COIN_PER_NPC_PER_SECOND      = 0.1f;
public static final float LICENCE_CHECK_DELAY_SECONDS        = 60f;
public static final float HIGH_NOTORIETY_CHECK_DELAY_SECONDS = 30f;
public static final float BUSK_RADIUS_BLOCKS                 = 8f;
public static final float MAX_PITCH_DISTANCE_TO_SHOP         = 30f;
public static final int   NOTORIETY_CONFISCATION             = 10;
public static final int   NOTORIETY_LEG_IT                   = 5;
public static final int   WANTED_STARS_CONFISCATION          = 2;
public static final int   WANTED_STARS_LEG_IT                = 1;
public static final float LAGER_SHORTAGE_MULTIPLIER          = 1.5f;
public static final float BENEFIT_DAY_MULTIPLIER             = 2.0f;
public static final float GREGGS_STRIKE_MULTIPLIER           = 0.5f;
public static final float BORED_NEED_REDUCTION_PER_COIN      = 10f;
public static final int   REP_AWARD_INTERVAL_SECONDS         = 60;
public static final float PITCH_EXCLUSION_RANGE              = 10f; // existing busker nearby
```

#### Wiring into `RagamuffinGame`

1. Add `private BuskingSystem buskingSystem;` field.
2. In `initGame()`, instantiate: `buskingSystem = new BuskingSystem(streetEconomySystem, factionSystem, wantedSystem, notorietySystem, rumourNetwork, achievementSystem);`
3. In `updatePlaying()`, while not UI-blocking, call `buskingSystem.update(delta, npcManager.getNPCs(), player, weatherSystem.getCurrentWeather(), streetEconomySystem.getActiveEvent())`.
4. In `handleInteract()`, if the player's selected hotbar item is `BUCKET_DRUM` and the player is not already busking, call `buskingSystem.startBusk(player, world, npcManager.getNPCs(), tooltipSystem)`.
5. In the `InputHandler` loop, detect hotbar-slot changes and call `buskingSystem.stopBusk()` if the BUCKET_DRUM slot is deselected mid-session.
6. Add `Material.BUCKET_DRUM` to the crafting menu recipe (recipe: `SCRAP_METAL ×1 + PLANKS ×1`).
7. Add `AchievementType` entries: `STREET_PERFORMER`, `LIVING_WAGE`, `BUCKET_LIST`, `MOVE_ALONG_PLEASE`.

**Unit tests**: Income formula with varying BORED scores, market-event multipliers, licence-check delay at different notoriety tiers, session-end detection, achievement trigger conditions, pitch-exclusion check.

**Integration tests — implement these exact scenarios:**

1. **Busking near high street earns coins**: Place player on a PAVEMENT block within 25
   blocks of the Greggs landmark. Give the player a `BUCKET_DRUM`. Spawn 4 NPCs within
   8 blocks with BORED need score set to 80. Start a busk session. Advance 120 frames
   (2 seconds). Verify the player's COIN count has increased. Verify at least one NPC's
   BORED need score has decreased below 80.

2. **Police licence check triggers after 60 seconds**: Start a busk session. Advance
   `LICENCE_CHECK_DELAY_SECONDS` real seconds of simulation. Verify a POLICE or PCSO NPC
   is within 3 blocks of the player and has delivered the *"You got a busking licence?"*
   speech text. Verify `buskingSystem.isLicenceCheckActive()` is `true`.

3. **FAKE_ID clears the licence check**: During an active licence check, give the player
   a `FAKE_ID`. Simulate the player "showing licence" (call
   `buskingSystem.onShowLicence(player.getInventory())`). Verify `FAKE_ID` is consumed
   from the player's inventory. Verify `isLicenceCheckActive()` is `false`. Verify busk
   session is still active. Verify Notoriety did not increase.

4. **Confiscation on no-licence wait**: During an active licence check, advance 10 seconds
   without player interaction (no FAKE_ID, no movement). Verify `BUCKET_DRUM` is removed
   from the player's inventory. Verify Notoriety increased by `NOTORIETY_CONFISCATION`.
   Verify WantedSystem stars increased by `WANTED_STARS_CONFISCATION`. Verify a
   `UNLICENSED_BUSKING` criminal record entry was logged. Verify `isBusking()` is `false`.

5. **Existing busker blocks pitch**: Spawn an NPC of type `BUSKER` at (5, 1, 5). Player
   attempts to start a busk session at (5, 1, 7) (within 10 blocks). Verify
   `startBusk()` returns failure. Verify `isBusking()` remains `false`. Verify the
   BUSKER NPC's speech text contains *"find your own pitch"*.

6. **BENEFIT_DAY doubles income**: Trigger `MarketEvent.BENEFIT_DAY` in
   `StreetEconomySystem`. Start a busk session with 2 NPCs at BORED=100. Advance 60
   frames (1 second). Verify the coin income rate is approximately double the baseline
   rate (within 5% tolerance for floating point). Verify `getLastIncomeRate()` reflects
   the `BENEFIT_DAY_MULTIPLIER`.

7. **5-minute achievement and barman rumour**: Start a busk session. Advance
   `REP_AWARD_INTERVAL_SECONDS × 5` seconds (5 minutes) of simulation without police
   interruption. Verify `LIVING_WAGE` achievement is unlocked. Verify a rumour with text
   containing *"actually decent"* has been seeded into the BARMAN NPC in the world.
   Verify Street Reputation increased by at least 5 points.

---

## Phase 8h: Pub Quiz Night

**Goal**: Add a recurring pub quiz mini-game at The Ragamuffin Arms (and The Rusty
Anchor) that gives the player a legitimate, social reason to visit the pub, rewards
knowledge with coins and faction respect, and connects to the rumour network, time
system, and street reputation — all quintessentially British.

### Overview

Every Thursday in-game at 20:00 (TimeSystem game-time), a pub quiz starts at The
Ragamuffin Arms. A QUIZMASTER NPC (new type, wears a novelty bow-tie) takes position
behind the bar and announces the quiz via a speech bubble. The quiz consists of
5 rounds of 3 questions each (15 questions total). Questions are drawn from a fixed
bank of ~60 absurdist-British multiple-choice questions covering topics such as:
"Locally Historical" (Northfield trivia), "Greggs Menu" (food knowledge), "Blagging
It" (general knowledge), "Council Jargon" (bureaucracy nonsense), and "Street
Wisdom" (survival/crime awareness).

The player participates by walking into the pub before 20:05 and pressing **E** on
the QUIZMASTER. A UI overlay (`PubQuizUI`) presents the current question and three
answer options (A/B/C). The player selects using keys **1**, **2**, **3**. Each
question has a 15-second timer (shown as a countdown bar); failure to answer in
time counts as a wrong answer.

### Scoring & Rewards

| Result | Coins | Street Rep | Notes |
|--------|-------|------------|-------|
| ≤5 correct | 0 | 0 | "Might wanna lay off the lager next time." |
| 6–9 correct | 3 | +1 | "Decent effort that." |
| 10–12 correct | 6 | +2 | "Not bad for someone who looks like you." |
| 13–15 correct | 12 | +4 | QUIZ_CHAMPION achievement; barman seeds rumour |

The **QUIZ_CHAMPION** achievement ("Local Knowledge") is unlocked only on a perfect
or near-perfect score (≥13). The prize pot is paid out by the QUIZMASTER NPC (coins
added to player inventory).

### Team Play

If the player has ≥2 FOLLOWER or ACCOMPLICE NPCs nearby (within 8 blocks), they form
a "team". Each teammate grants +1 second to the answer timer and a 10% bonus to prize
money (stacks, max 3 teammates = +3 seconds, +30% coins). Team members cheer when
correct (short speech bubble "YES MATE").

### Faction Integration

- **Street Lads**: Attending the quiz raises Street Lads respect +3 (they respect
  local knowledge). If the player wins, respect +5 additional.
- **The Council**: A COUNCIL_MEMBER NPC is always present in the pub during quiz
  night. Winning in front of them lowers Council respect by 3 ("Nobody likes a
  smartarse").
- **Marchetti Crew**: If the player is the known QUIZ_CHAMPION, a Marchetti lieutenant
  offers a one-time "Rig the Quiz" side mission: distract or intimidate the QUIZMASTER
  before 20:10 so that another team wins. Reward: 15 coins + Respect +15, but
  Criminal Record entry `QUIZ_RIGGING` is logged.

### RumourNetwork Integration

- When the quiz starts, the BARMAN seeds a `LOOT_TIP` rumour: *"Quiz night at the
  Arms tonight — winner takes the pot. Usually about twelve quid."*
- After a player wins, a `STREET_REPUTATION` rumour is seeded to 3 nearby NPCs:
  *"[Player] just cleaned up at the pub quiz. Proper local, that."*
- If the player cheats (uses `COUNCIL_ID` item for a bonus answer — see below), a
  `WITNESS_SIGHTING` rumour is seeded by the QUIZMASTER: *"Someone had a cheat sheet
  at quiz night. Low."*

### Cheat Mechanic

The player can spend 1 `COUNCIL_ID` before a question begins to "phone a friend" —
the correct answer is highlighted in the UI for 2 seconds before the timer starts.
This consumes the COUNCIL_ID and seeds the WITNESS_SIGHTING rumour. Using cheats
3+ times in one quiz awards the `PLAYED_THE_SYSTEM` achievement.

### New Classes

- **`PubQuizSystem`** (`core/`): Manages quiz state (IDLE, WAITING_FOR_PLAYERS,
  ROUND_IN_PROGRESS, FINISHED), question bank, scoring, team detection, and faction
  integration. Integrates with `TimeSystem`, `FactionSystem`, `RumourNetwork`,
  `StreetReputation`, and `WantedSystem`.
- **`PubQuizUI`** (`ui/`): Renders the question overlay — question text, three answer
  buttons (1/2/3), countdown bar, current score, and round indicator.
- **`PubQuizQuestion`** (`core/`): Simple data record: question string, three option
  strings, correct answer index (0/1/2), topic tag.

### New NPCType

- **`QUIZMASTER`**: Passive; spawns inside the PUB at 19:55 on quiz nights; despawns
  at 22:00. Delivers quiz announcements and hands out prize money. Wears a bow-tie
  (rendered via NPCModelVariant). If threatened, speech: *"Oi — I'm just the quizmaster,
  leave it out."* Never fights back; flees if health drops below 50%.

### New AchievementType entries

- **`QUIZ_CHAMPION`** ("Local Knowledge"): Score ≥13/15 in a single pub quiz.
- **`PLAYED_THE_SYSTEM`** ("Brazen"): Use 3 COUNCIL_ID cheat answers in one quiz.
- **`RIGGED_IT`** ("That's Not Sport"): Successfully complete the Rig the Quiz mission.

**Unit tests**: Question bank loads correctly (≥60 questions), scoring formula, timer
expiry counts as wrong answer, team bonus calculation, cheat mechanic deducts COUNCIL_ID,
faction respect deltas applied correctly, QUIZMASTER NPC spawns/despawns at correct times.

**Integration tests — implement these exact scenarios:**

1. **Quiz starts Thursday 20:00**: Set TimeSystem to Thursday 19:59. Advance 70 frames
   (~1.2 in-game minutes). Verify a QUIZMASTER NPC has spawned inside the PUB landmark.
   Verify the BARMAN NPC's speech text contains *"Quiz night"*. Verify a `LOOT_TIP`
   rumour has been seeded into at least one NPC.

2. **Player answers all 15 questions correctly**: Enrol the player in the quiz (call
   `pubQuizSystem.enrol(player)`). For each of 15 questions, call
   `pubQuizSystem.submitAnswer(correctAnswerIndex)` before the timer expires. Verify
   `pubQuizSystem.getScore()` is 15. Verify player's COIN count has increased by 12.
   Verify `QUIZ_CHAMPION` achievement is unlocked. Verify Street Reputation increased
   by 4. Verify a rumour containing *"cleaned up"* is seeded to at least 3 NPCs.

3. **Timer expiry counts as wrong answer**: Enrol the player. On the first question,
   advance `PubQuizSystem.QUESTION_TIMEOUT_SECONDS` without submitting an answer.
   Verify the question advances automatically. Verify the score remains 0.

4. **Team bonus increases prize money**: Add 2 FOLLOWER NPCs within 8 blocks of the
   player. Enrol the player. Answer all 15 questions correctly. Verify the coin reward
   is 12 × 1.20 = 14 (rounded down), not the base 12.

5. **COUNCIL_ID cheat highlights answer**: Give the player 1 COUNCIL_ID. Enrol the
   player. Before the first question timer starts, call
   `pubQuizSystem.useCheatAnswer(player.getInventory())`. Verify COUNCIL_ID count
   decreased by 1. Verify `pubQuizSystem.isCorrectAnswerHighlighted()` is true for
   2 seconds. Verify a `WITNESS_SIGHTING` rumour has been seeded.

6. **Faction respect changes on win**: Set Street Lads respect to 50 and Council
   respect to 50. Player wins the quiz (score ≥13). Verify Street Lads respect is
   ≥55 (base +3 for attending + +5 for win). Verify Council respect is ≤47 (−3 for
   witnessing a win).

---

## Phase 8i: Bus Stop & Public Transport System

**Goal**: Add a working bus network that connects key landmarks across the 200×200
block world, giving the player a legitimate fast-travel option that interacts with
the WantedSystem, TimeSystem, WeatherSystem, and NPC routines — all quintessentially
British.

### Overview

The world has **4 bus stops** at fixed positions near major landmarks (town centre /
park, industrial estate, Greggs parade, and the JobCentre). Each stop has a
`BUS_STOP` prop (a yellow pole with a timetable sign rendered via `PropRenderer`).
Buses run on a fixed schedule: one bus every **8 in-game minutes** during operating
hours (07:00–23:00). At night (23:00–07:00) only a single "night bus" runs every
20 minutes on a reduced route (town centre ↔ industrial estate only).

When the player presses **E** at a bus stop, a `BusStopUI` overlay appears listing
the next 3 departures and their destinations. The player selects a destination with
**1/2/3** keys. The "fare" is **2 coins** deducted immediately. If the player has
insufficient coins, the fare can be dodged (see below). The bus then "arrives" (a
brief arrival cinematic: blocky low-poly bus model slides into view from off-screen),
the player boards, a fast-travel fade-to-black occurs, and they reappear at the
destination stop after a simulated journey time of **3 real seconds**.

### Fare Dodging

The player may choose "Leg it" instead of paying. This:
- Teleports them to the destination as normal.
- Rolls a dice: if `notorietySystem.getLevel() >= 2` (KNOWN or above), there is a
  40% chance a **TICKET_INSPECTOR** NPC (new type) is waiting at the destination stop.
- The TICKET_INSPECTOR demands a **10-coin fine** or arrests the player (same flow as
  `ArrestSystem`). The player may run (WantedSystem +1 star) or pay.
- Criminal record entry `FARE_DODGING` is logged on arrest.
- At notoriety level 0, fare dodging has only a 10% chance of a TICKET_INSPECTOR.

### Night Bus Chaos

After 22:00, the night bus carries **2–4 DRUNK NPC passengers** visible at the
destination stop when the player arrives. They linger for 60 seconds before
dispersing. One DRUNK NPC has a 30% chance of starting a fight with another NPC
(triggering `NPCHitDetector` logic). The player arriving on the night bus gains
the street-flavour tooltip: *"The night bus. God help us."*

### Weather Integration

During `RAIN` or `THUNDERSTORM` weather, the bus stop gains an NPC crowd: **3
extra PUBLIC NPCs** spawn at the stop (waiting for the bus, obviously). These NPCs
have BORED need set to 80, making them prime targets for the BuskingSystem if the
player sets up nearby. During `COLD_SNAP` or `FROST`, NPCs at the stop have dialogue
referencing the cold: *"Bloody freezing. Where's the bus?"*

### Faction Integration

- **The Council**: Owning a `BUS_PASS` item (crafted: `COUNCIL_ID ×1 + SCRAP_METAL ×1`)
  gives the player free unlimited rides. The Council respect increases by +1 the first
  time the player uses a bus pass (they approve of using public services).
- **Street Lads**: Spray-painting graffiti on a bus stop (via `GraffitiSystem`)
  earns Street Lads respect +2. The Council immediately dispatches a COUNCIL_CLEANER
  NPC to remove it (existing behaviour).
- **Marchetti Crew**: A `MARCHETTI_PACKAGE` quest variant can require the player to
  deliver a package by bus to the industrial estate stop. The package must arrive
  within 2 in-game bus cycles or the mission fails.

### New Classes

- **`BusSystem`** (`core/`): Manages the bus schedule (timetable per stop, next
  arrival timers), fare payment/dodging logic, TICKET_INSPECTOR spawn logic, night
  bus DRUNK NPC spawning, and weather crowd spawning. Integrates with `TimeSystem`,
  `WeatherSystem`, `NotorietySystem`, `WantedSystem`, `ArrestSystem`,
  `CriminalRecord`, `RumourNetwork`, and `NPCManager`.
- **`BusStopUI`** (`ui/`): Renders the destination-selection overlay — stop name,
  next 3 departures with countdown timers, fare cost, and a "Leg it" option.
- **`BusRoute`** (`core/`): Simple data record: ordered list of stop landmark IDs,
  operating hours start/end, frequency in game-minutes.

### New NPCType

- **`TICKET_INSPECTOR`**: Spawns only at bus stops after a fare dodge. Passive but
  immediately initiates the fine dialogue on proximity. Stats: health 30, attack 4,
  cooldown 1.5s, hostile if the player refuses to pay or flees.

### New PropType entry

- **`BUS_STOP`**: Rendered as a yellow post with a rectangular sign face. Placed at
  4 fixed world positions by `WorldGenerator`.

### New Material / Item

- **`BUS_PASS`**: Craftable consumable (unlimited uses, not consumed on use). Displayed
  in inventory as a small card. Recipe: `COUNCIL_ID ×1 + SCRAP_METAL ×1`. Tooltip:
  *"Valid for travel on all services. Probably."*

### New AchievementType entries

- **`NIGHT_OWL`** ("Late One"): Ride the night bus at least once.
- **`COMMUTER`** ("Regular"): Take 10 bus rides in total.
- **`DODGER`** ("Tight Git"): Successfully fare-dodge 5 times without being caught.

**Unit tests**: BusRoute timetable returns correct next-arrival time, fare deduction
from inventory, fare-dodge notoriety probability thresholds, TICKET_INSPECTOR spawn
condition, night bus DRUNK NPC count range, BUS_PASS crafting recipe, weather crowd
spawn count.

**Integration tests — implement these exact scenarios:**

1. **Bus arrives on schedule**: Set TimeSystem to 07:00. Interact with a bus stop
   (call `busSystem.onPlayerInteract(player, stopId)`). Verify `BusStopUI` is shown
   with at least one departure listed. Advance 8 in-game minutes. Verify
   `busSystem.isServiceActive()` is true and a departure event has fired.

2. **Paying fare teleports player**: Place player at stop A. Give player 5 coins.
   Call `busSystem.boardBus(player, stopA, stopB)`. Verify player's coin count
   decreased by 2. Advance 3 real seconds of simulation. Verify player position is
   within 3 blocks of stop B's world position.

3. **Fare dodging with high notoriety spawns TICKET_INSPECTOR**: Set notoriety level
   to 2. Call `busSystem.dodgeFare(player, stopA, stopB)` 10 times (resetting
   position each time). Verify at least 4 TICKET_INSPECTOR NPCs were spawned at
   stop B across the 10 attempts (≈40% rate, allow ±2 for RNG variance).

4. **Night bus spawns drunk NPCs**: Set TimeSystem to 22:30. Board the night bus
   (call `busSystem.boardBus(player, stopA, stopB)`). Advance 3 real seconds. Verify
   between 2 and 4 DRUNK NPCs are present within 5 blocks of stop B. Verify at
   least one DRUNK NPC has BORED need score > 0.

5. **Weather crowd spawns during rain**: Set WeatherSystem to RAIN. Trigger stop
   update (call `busSystem.updateStop(stopId, delta)` for 1 frame). Verify 3 PUBLIC
   NPCs are present at the stop with BORED need score = 80.

6. **BUS_PASS gives free ride**: Give player a `BUS_PASS` item. Call
   `busSystem.boardBus(player, stopA, stopB)`. Verify player's coin count is
   unchanged. Verify Council respect increased by 1 (first-use bonus). Call
   `busSystem.boardBus(player, stopB, stopA)` a second time. Verify Council respect
   did NOT increase again (one-time bonus only).

---

## Phase 8j: Bookies Horse Racing System

**Goal**: Give the BOOKIES and BETTING_SHOP landmarks real gameplay by adding a
horse-racing betting system. The player can walk in, study the odds displayed on
the TV screen prop, place bets with coins, watch a simulated race, and either win
big or lose everything. Debt and addiction mechanics add tension.

### Core Design

A `HorseRacingSystem` singleton manages daily race schedules, odds generation, bet
placement, and outcome resolution.

**Race Schedule**:
- 8 races per in-game day, spaced evenly from 11:00 to 21:00 (every ~1.25 in-game hours)
- Each race has 6 named horses with seeded-RNG odds and a unique race name
- Horse names are drawn from a fixed pool of 30 British-flavoured names (e.g.
  "Broken Biscuit", "Council Flat", "Benefit Cheque", "Grey Drizzle", "Last Orders",
  "Missed the Bus", "Dodgy Kebab", "Northern Grit", "Chip Shop Queue", etc.)
- Odds are assigned per race: one favourite (2/1), two mid-range (4/1, 6/1), two
  outsiders (10/1, 16/1), one rank outsider (33/1)
- Actual win probability is the inverse of stated odds (favourite wins ~33% of
  races, rank outsider wins ~3%)

**Betting**:
- Player interacts with a TV_SCREEN prop inside a BOOKIES or BETTING_SHOP landmark
  (E key); opens a `BettingUI` overlay showing today's races, current race, horses
  with odds, and the player's active bets
- Player selects a horse and a stake (1–50 coins, capped by inventory coins)
- Only one active bet allowed per race; cannot bet once the race has started
- `BET_SLIP` item is added to inventory when bet placed; removed on resolution
- Payout = stake × odds numerator (e.g. 10-coin bet at 10/1 returns 100 coins)
- Minimum stake: 1 coin. House minimum is enforced.

**Race Resolution**:
- When the race time arrives (TimeSystem tick), `HorseRacingSystem.resolveRace(raceId)`
  is called
- Winner determined by weighted RNG matching the stated odds
- If player had a winning bet: coins added to inventory, tooltip "You backed a
  winner! +N coins", achievement check
- If player lost: BET_SLIP removed silently, tooltip "Unlucky — [HorseName] came in
  last"
- Cumulative net loss tracking for debt and addiction mechanics (see below)

**Debt Mechanic**:
- A `LOAN_SHARK` NPC spawns inside the bookies if the player's cumulative net loss
  across all bets reaches 50 coins
- The LOAN_SHARK offers a loan of 20 coins at 50% interest (must repay 30 coins
  within 3 in-game days)
- If loan is not repaid: LOAN_SHARK becomes hostile, two STREET_LAD enforcers spawn
  and pursue the player; WantedSystem gains +1 star
- Repaying the loan on time gives `DEBT_FREE` achievement and LOAN_SHARK NPC departs

**Inside the Bookies ambience**:
- 2–4 PENSIONER and PUBLIC NPCs always present during opening hours (09:00–22:00),
  wandering slowly near the TV screen
- Each has a `BORED` need score that slowly rises; interacting and winning raises
  their mood (reduces BORED by 20)
- NPCs occasionally mutter flavour lines: "Come on my son", "Useless horse",
  "Just one more", "Should've gone to Greggs"

**Items / Materials added**:
- `BET_SLIP` — paper item, single-use, represents an active wager; isSmallItem=true
- No new block types required

**Integration**:
- `TimeSystem` — triggers race resolution callbacks at scheduled times
- `NotorietySystem` — placing bets with counterfeit notes (COUNTERFEIT_NOTE in
  inventory when paying stake) raises notoriety +5 and triggers LOAN_SHARK hostility
  immediately
- `StreetEconomySystem` — BENEFIT_DAY market event doubles max allowed stake (100
  coins) and increases race frequency by one extra race
- `RumourNetwork` — on a jackpot win (33/1 outsider), seed rumour type `BIG_WIN_AT_BOOKIES`;
  nearby NPCs have their BORED need set to 0 and wander toward the bookies for 60s
- `AchievementSystem`:
  - **`LUCKY_PUNT`** ("Worth a Flutter"): Win any bet — instant
  - **`OUTSIDER`** ("Didn't See That Coming"): Win a bet at 10/1 or longer odds — instant
  - **`RANK_OUTSIDER`** ("You Beauty!"): Win a bet at 33/1 — instant
  - **`LOSING_STREAK`** ("In Too Deep"): Accumulate 50 coins net loss — instant
  - **`DEBT_FREE`** ("Clean Slate"): Repay the loan shark on time — instant
  - **`DAILY_PUNTER`** ("Regulars Know Best"): Place a bet on every race in a single
    in-game day (all 8 races) — progress target 8

**Unit tests**: Race schedule generation (8 races, correct times), odds assignment
(one favourite, six horses), payout calculation at each odds tier, debt threshold
triggers LOAN_SHARK spawn, repayment clears debt and dismisses NPC, counterfeit-note
notoriety penalty, BENEFIT_DAY stake cap doubling, rumour seeding on 33/1 win.

**Integration tests — implement these exact scenarios:**

1. **Bet placement opens BettingUI**: Place player inside a BOOKIES landmark. Interact
   with TV_SCREEN prop (call `horseRacingSystem.onPlayerInteract(player, propPos)`).
   Verify `BettingUI.isVisible()` returns true. Verify at least one race with 6
   horses is listed. Verify the player cannot bet more coins than they hold (place a
   100-coin bet with only 10 coins in inventory — verify `BET_RESULT.INSUFFICIENT_FUNDS`
   is returned and no BET_SLIP is added).

2. **Winning bet pays out correctly**: Set up a race with horse "Grey Drizzle" at
   10/1. Force the race winner to "Grey Drizzle" (inject deterministic RNG returning
   the index of that horse). Give player 5 coins and place a 5-coin bet on that horse.
   Resolve the race. Verify player receives 50 coins (5 × 10), BET_SLIP is removed
   from inventory, and tooltip contains "+50 coins".

3. **Losing bet removes bet slip**: Place a 3-coin bet. Force the race winner to a
   different horse. Resolve the race. Verify player's coins are unchanged from
   post-bet amount (no refund), BET_SLIP is absent from inventory, and tooltip
   contains "Unlucky".

4. **Debt triggers LOAN_SHARK spawn**: Configure player with 0 net loss. Place and
   lose bets totalling 50 coins net loss (simulate via
   `horseRacingSystem.recordLoss(50)`). Verify a LOAN_SHARK NPC is present within
   15 blocks of the bookies entrance. Verify the NPC offers a loan dialogue when
   interacted with.

5. **Loan shark turns hostile on non-repayment**: Accept the 20-coin loan. Advance
   TimeSystem by 3 in-game days without repaying. Verify LOAN_SHARK NPC state is
   HOSTILE. Verify two STREET_LAD NPCs are present near the bookies. Verify
   WantedSystem wanted level increased by 1.

6. **BENEFIT_DAY doubles max stake**: Activate market event BENEFIT_DAY
   (`streetEconomySystem.setActiveEvent(MarketEvent.BENEFIT_DAY)`). Attempt to place
   a 75-coin bet (above normal 50-coin cap). Verify `BET_RESULT.SUCCESS` (not
   `STAKE_TOO_HIGH`). Deactivate event. Attempt the same 75-coin bet again. Verify
   `BET_RESULT.STAKE_TOO_HIGH` is returned.

---

## Phase 8k: Allotment System — Grow Your Own, Mind Your Own

**Goal**: Give the player a plot on the ALLOTMENTS landmark to grow vegetables,
trade surplus produce, and navigate classic British allotment drama: nosy plot
neighbours, council repossession threats, and an annual vegetable show.

### Overview

The ALLOTMENTS landmark already exists in `LandmarkType` and `WorldGenerator`.
A new `AllotmentSystem` class manages plot ownership, crop growth, harvesting,
plot-neighbour events, and the annual Giant Vegetable Show.

### Plot Claiming

- The allotments area contains 6 individual plots (5×5 DIRT blocks each), separated
  by FENCE blocks and narrow PAVEMENT paths.
- Press E near the `ALLOTMENT_WARDEN` NPC (a PENSIONER sub-variant stationed at the
  gate) to claim an unclaimed plot for **free** — or buy out an occupied plot for
  **30 coins** if all plots are taken.
- Only one plot per player at a time. A `PLOT_DEED` item is added to inventory on
  claim.
- The warden opens hours: 07:00–19:00. Outside these hours, the gate is locked
  (attempting to enter adds `TRESPASSING` to the criminal record).

### Crop Growing

Four growable crops, each planted by right-clicking a DIRT block in the player's
plot while holding the corresponding seed item:

| Crop | Seed item | Grow time (in-game mins) | Yield on harvest | Satisfies need |
|------|-----------|--------------------------|-----------------|----------------|
| POTATO | `POTATO_SEED` | 15 | 2–4 `POTATO` | HUNGRY |
| CARROT | `CARROT_SEED` | 10 | 2–3 `CARROT` | HUNGRY |
| CABBAGE | `CABBAGE_SEED` | 20 | 1–2 `CABBAGE` | HUNGRY |
| SUNFLOWER | `SUNFLOWER_SEED` | 8 | 1 `SUNFLOWER` | BORED (trade value) |

- Seeds are obtained from: CORNER_SHOP (buy for 1 coin each), FOOD_BANK
  (free once per day), or by harvesting and retaining 1 seed from the yield.
- Crops display a 3-stage visual: freshly planted dirt (stage 0), small sprout
  (stage 1, 50% grown), fully grown (stage 2). Use block metadata / `PropType` to
  track stage.
- Harvesting: left-click the fully grown crop. Yields items, resets block to DIRT.
- Watering: right-click with `BUCKET` (already in `Material`) on a crop block to
  reduce remaining grow time by 30%. Bucket is not consumed.
- Drought (HEATWAVE weather): grow time ×1.5 unless watered each in-game day.
- Rain/DRIZZLE: automatic watering — no bucket needed that day.

### Plot Neighbour Events

Two neighbouring PLOT_NEIGHBOUR NPCs (PENSIONER type with unique speech) tend
adjacent plots. They generate one of four random events every 5 in-game minutes
while the player is on the allotments:

1. **Compliment** — "Them carrots are coming on lovely." Faction respect STREET_LADS +1.
2. **Complaint** — "Your weeds are blowing onto my patch." Player must harvest any
   fully grown crop within 60 seconds or lose 2 coins (withheld by the warden as a
   "nuisance fine").
3. **Gift** — Neighbour drops 1 `POTATO` or `CARROT` near the plot boundary. No
   conditions.
4. **Rivalry** — "Mine'll be bigger than yours at the show." Triggers the Giant
   Vegetable Show vote bonus (see below).

### Council Repossession Threat

If the player fails to harvest any crop for 3 consecutive in-game days (plot goes
entirely fallow), the council issues a `REPOSSESSION_NOTICE` prop on the plot gate
post. The player has 1 in-game day to plant at least one seed. If ignored, the plot
is repossessed (removed from player ownership, PLOT_DEED removed from inventory,
Council Respect −5). The warden delivers the notice with speech: "Use it or lose it,
pal."

### Annual Giant Vegetable Show

Once per in-game week (day 7, 14, 21…) at 12:00, a Giant Vegetable Show event runs
for 1 in-game hour. The player can enter their largest harvested crop (tracked by
cumulative harvest count per crop type) to compete against the plot neighbours.

- Judge NPC (COUNCIL_MEMBER sub-variant in tweed jacket) inspects entries.
- Winner determined by RNG weighted by: largest yield harvested in the past week
  (player's cumulative harvest) vs neighbour score (fixed random 2–5).
- **Win**: 15 coins + `CHAMPION_GROWER` achievement + Newspaper headline
  "LOCAL HERO TAKES TOP PRIZE AT VEG SHOW".
- **Lose**: Neighbour speech: "Better luck next year." No penalty.
- Entering gives INFLUENCE skill XP regardless of outcome.

### Items / Materials added

- `POTATO_SEED`, `CARROT_SEED`, `CABBAGE_SEED`, `SUNFLOWER_SEED` — plantable seeds
- `POTATO`, `CARROT`, `CABBAGE`, `SUNFLOWER` — harvested produce (all `isSmallItem=true`)
- `PLOT_DEED` — proof of plot ownership
- `REPOSSESSION_NOTICE` — prop item; no inventory use

All produce satisfies `NeedType.HUNGRY` when consumed (left-click in hand or E
on hungry NPC to give). SUNFLOWER can be sold to a PLOT_NEIGHBOUR or at the
BOOT_SALE for 3 coins each (trade only, not edible).

### Integration

- **`StreetEconomySystem`**: harvested POTATO/CARROT/CABBAGE satisfy `NeedType.HUNGRY`
  for NPCs. Dealing produce to hungry NPCs earns coins at standard deal rates.
- **`MarketEvent.GREGGS_STRIKE`**: produce prices double; nearby PUBLIC NPCs develop
  HUNGRY need faster, making allotment produce more valuable.
- **`WeatherSystem`**: HEATWAVE slows growth ×1.5; RAIN/DRIZZLE auto-waters; FROST
  kills any crop currently at stage 0 (seed just planted) — yields nothing on harvest.
- **`TimeSystem`**: crop growth ticks every in-game minute; show triggers weekly.
- **`NewspaperSystem`**: Giant Vegetable Show win generates a headline at infamy 0
  level ("PIGEON" tier replaced by allotment content).
- **`NotorietySystem`**: trespassing outside warden hours adds criminal record entry.
- **`StreetSkillSystem`**: each successful harvest awards `GRAFTING` skill XP (1 point
  per crop). Winning the Veg Show awards `INFLUENCE` skill XP (5 points).
- **`AchievementSystem`**:
  - **`GREEN_FINGERS`** ("Down the Allotment"): Harvest your first crop — instant.
  - **`CHAMPION_GROWER`** ("Best in Show"): Win the Giant Vegetable Show — instant.
  - **`SELF_SUFFICIENT`** ("Off the Grid"): Harvest 20 crops total — progress target 20.
  - **`GOOD_NEIGHBOUR`** ("Keep Britain Tidy"): Receive 3 compliment events without
    triggering a complaint — progress target 3.

### Unit tests

`AllotmentSystem` unit tests:
- `claimPlot()` returns SUCCESS when plots available, fails when all occupied.
- `plantCrop()` returns WRONG_BLOCK if block is not DIRT in player's plot,
  ALREADY_PLANTED if block already has a crop, SUCCESS otherwise.
- Crop growth timer advances correctly with delta; reaches stage 1 at 50% and
  stage 2 at 100% of grow time.
- `waterCrop()` reduces remaining grow time by 30%.
- HEATWAVE multiplier: growth rate slowed by ×1.5.
- FROST kills stage-0 crop (yields 0 on harvest attempt).
- Fallow-days counter increments when no crops harvested for a full in-game day;
  resets on any harvest.
- Repossession notice triggers on day 3 of fallow; plot removed on day 4.
- Neighbour complaint event deducts 2 coins if player does not harvest within
  60 seconds.
- Giant Vegetable Show winner is determined correctly by weighted RNG; correct
  coin and achievement rewards applied.

### Integration tests — implement these exact scenarios:

1. **Plot claim and crop growth cycle**: Spawn `ALLOTMENT_WARDEN` NPC near the
   ALLOTMENTS landmark. Call `allotmentSystem.claimPlot(player, warden)`. Verify
   `PLOT_DEED` is in player inventory. Set a DIRT block in the player's plot; call
   `allotmentSystem.plantCrop(player, blockPos, Material.CARROT_SEED)`. Advance
   the `TimeSystem` by 10 in-game minutes (10 × 60 real seconds at default speed).
   Call `allotmentSystem.update(delta, ...)` each frame. Verify the crop block
   reaches stage 2 (fully grown). Call `allotmentSystem.harvestCrop(player, blockPos,
   inventory)`. Verify at least 2 `CARROT` items in player inventory.

2. **Watering reduces grow time**: Plant a POTATO_SEED (15-min grow time). Call
   `allotmentSystem.waterCrop(player, blockPos)`. Verify the crop's remaining grow
   time has been reduced to ≤ 10.5 in-game minutes (30% reduction of 15 = 4.5 min
   off). Advance time by 10.5 minutes. Verify crop is at stage 2.

3. **FROST kills newly planted crop**: Set weather to FROST. Plant a CARROT_SEED
   (stage 0). Call `allotmentSystem.update(delta, weather=FROST)` for 1 in-game
   minute. Verify `allotmentSystem.isCropKilled(blockPos)` returns true. Attempt to
   harvest — verify 0 CARROT items returned and block resets to DIRT.

4. **Repossession after 3 fallow days**: Claim a plot. Advance `TimeSystem` by
   3 full in-game days without planting any seed. Verify
   `allotmentSystem.isRepossessionNoticePending()` returns true after day 3. Advance
   1 more day. Verify `allotmentSystem.hasPlot(player)` returns false (plot removed).
   Verify `PLOT_DEED` is no longer in player inventory.

5. **Giant Vegetable Show win pays out**: Claim plot. Harvest at least 3 POTATO
   crops (advance time, plant, harvest × 3). Advance TimeSystem to the next show
   time (day 7 at 12:00). Force `allotmentSystem.runShow(deterministicRng=playerWins)`
   with an RNG that returns a value ensuring player wins. Verify player receives 15
   coins. Verify `CHAMPION_GROWER` achievement is unlocked. Verify
   `NewspaperSystem.getLastHeadline()` contains "VEG SHOW".

---

## Phase 8w: Late-Night Kebab Van

**Goal**: A kebab/chip van that appears after 22:00 near the pub, creating a
focal point for late-night chaos — drunk NPCs queuing, queue-jumping, price
gouging, and a surly owner who has seen it all.

### Overview

A `KebabVanSystem` manages a single mobile food van (`KEBAB_VAN` landmark) that
parks at a fixed spot near the pub entrance between **22:00 and 02:00** each
night. Outside those hours the van is absent (despawned). While active:

- A **VAN_OWNER** NPC stands at the serving hatch (new `NPCType.VAN_OWNER`).
- Drunk NPCs (`DRUNK`, `RAVE_ATTENDEE`) nearby are magnetically drawn to the
  van and join a virtual queue (ordered list of NPCs, max 8).
- Each queued NPC takes 8 in-game seconds to be served, then receives a
  `KEBAB` or `CHIPS` item and moves away satisfied (HUNGRY and BORED needs
  reset to 0).
- The player can:
  - **Queue normally** (press **E** on the VAN_OWNER while facing the hatch):
    joins back of queue, costs **3 COIN** for KEBAB or **2 COIN** for CHIPS.
  - **Queue-jump** (press **E** while running, i.e. sprint key held): pushes to
    front of queue. Any NPC displaced generates a speech complaint; there is a
    **40% chance** a displaced DRUNK NPC becomes hostile and attacks the player.
  - **Distract the owner** (throw a `TIN_OF_BEANS` item near the van): the
    VAN_OWNER chases the distraction for 5 seconds; during this window the
    player can steal food (takes one KEBAB from the van's stock for free, adds
    2 Notoriety, and seeds a `THEFT` rumour).

### Dynamic Pricing

The van's prices float based on demand and time of night:
- **Base price**: KEBAB = 3 COIN, CHIPS = 2 COIN.
- **Rush hour** (22:00–23:00): prices ×1.5 (rounded up).
- **Last orders** (01:00–02:00): prices ×0.75 (discounted to clear stock).
- **COLD_SNAP / FROST** weather: prices ×2.0 (British entrepreneurialism).
- **Active GREGGS_STRIKE** market event: prices ×1.5 and queue length +3 extra
  NPCs (everyone's desperate for hot food).

### Van Stock

The van starts each night with **20 servings** (any mix of KEBAB and CHIPS).
When stock hits 0 the VAN_OWNER puts up a "SOLD OUT" speech bubble, all queued
NPCs wander away disappointed, and the van despawns early.

### NPC Behaviour Integration

- **DRUNK** NPCs with HUNGRY need > 40 within 20 blocks automatically pathfind
  to the van and join the queue (via `NPCState.QUEUING`).
- **RAVE_ATTENDEE** NPCs leaving an active rave always attempt to visit the van
  (HUNGRY need set to 60 on rave end).
- **POLICE / PCSO** NPCs patrol within 10 blocks of the van at night; witnessing
  the player steal food triggers a `WitnessSystem` evidence entry and +5 Notoriety.

### Achievements

- **`DIRTY_KEBAB`** ("Nutrition Optional"): Buy your first kebab from the van — instant.
- **`FRONT_OF_THE_QUEUE`** ("Queue-Jumper"): Successfully queue-jump 3 times — progress target 3.
- **`DISTRACTION_TECHNIQUE`** ("Misdirection"): Steal from the van using the
  TIN_OF_BEANS distraction — instant.
- **`LAST_ORDERS`** ("Night Owl"): Buy food during the 01:00–02:00 last orders
  window — instant.

### Unit tests

`KebabVanSystem` unit tests:
- Van spawns when `TimeSystem` reports hour ≥ 22 and despawns at hour ≥ 26 (02:00).
- Queue-join adds player to back of queue; queue-jump inserts player at index 0.
- Serve tick reduces stock by 1 and grants correct item to front-of-queue entity.
- Dynamic price calculation correct for each time window and weather combination.
- GREGGS_STRIKE market event adds 3 extra queued NPCs on van spawn.
- Distraction window set to 5 seconds on `TIN_OF_BEANS` throw; theft adds 2 Notoriety.
- Displaced drunk NPC hostility rolls at 40% probability (use seeded RNG in tests).
- Stock reaching 0 despawns van and clears queue.

### Integration tests — implement these exact scenarios:

1. **Van appears at night and disappears at dawn**: Set `TimeSystem` to 21:59.
   Call `kebabVanSystem.update(delta, ...)`. Verify no `KEBAB_VAN` landmark is
   active. Advance time to 22:01. Call `update`. Verify a `VAN_OWNER` NPC is
   present and `kebabVanSystem.isVanActive()` returns true. Advance time to
   02:01. Call `update`. Verify `isVanActive()` returns false and `VAN_OWNER`
   NPC is no longer alive.

2. **Player buys a kebab and HUNGRY need resets**: Give the player 3 COIN.
   Set `TimeSystem` to 22:30 (base price window). Activate the van. Press **E**
   on `VAN_OWNER` and select KEBAB. Verify player has 0 COIN remaining and 1
   KEBAB in inventory. Verify `StreetEconomySystem.getNeedScore(player, HUNGRY)`
   has been set to 0 (or close to 0 via `HealingSystem` consumption).

3. **Queue-jump displaces NPC and may trigger fight**: Spawn a DRUNK NPC with
   HUNGRY need = 80. Let the NPC join the queue (verify queue size = 1). Player
   queue-jumps (press E with sprint). Verify player is at queue index 0. Verify
   the displaced DRUNK NPC has received a complaint speech. Seed the RNG so the
   hostility roll succeeds; verify the DRUNK NPC transitions to `NPCState.ATTACKING`.

4. **Distraction theft adds Notoriety and seeds rumour**: Give the player 1
   `TIN_OF_BEANS`. Activate the van (stock = 20). Throw the TIN_OF_BEANS within
   5 blocks of the van. Verify `kebabVanSystem.isOwnerDistracted()` is true.
   Press **E** on the serving hatch. Verify player receives 1 KEBAB. Verify van
   stock is now 19. Verify `WantedSystem` Notoriety has increased by 2. Verify
   `RumourNetwork` contains a `THEFT` rumour seeded by the van's position.

5. **Van sells out and despawns early**: Set van stock to 1. Serve the last item
   (advance time by 8 in-game seconds). Verify `VAN_OWNER` displays "SOLD OUT"
   speech. Verify `isVanActive()` returns false within the next update tick.

---

## Feature: Bus Stop & Public Transport System — 'The Number 47'

**Goal**: Add a functioning bus network to the town — bus stops along the high
street, a timetable that is never quite right, NPCs that queue and grumble, and
a player who can ride, rob, or simply watch the bus drive past without stopping.
This system makes the world feel genuinely British by weaving public transport
frustration into survival gameplay.

### Overview

A new `BusSystem` class manages a single bus route (the Number 47) that runs a
loop between 3 fixed bus stops: the High Street stop (near Greggs), the Park
stop (near the pond), and the Industrial Estate stop (near the JobCentre). The
bus runs every 10 in-game minutes during operating hours (06:00–23:00). At night
it is replaced by the Night Bus, which runs once per hour and costs double fare.

### Bus Stops

- 3 `BUS_STOP` prop positions are placed by the WorldGenerator along pavements
  adjacent to key landmarks.
- Bus stops have a small sign prop (`PropType.BUS_STOP_SIGN`) showing the route
  number and next departure time (updated dynamically on the sign texture).
- NPCs with `COMMUTER` or `WORK` intent automatically path toward the nearest
  bus stop during morning (07:00–09:00) and evening rush (17:00–19:00) hours.
- Up to 8 NPCs can queue at a bus stop; overflow NPCs wait nearby and grumble.

### The Bus (Vehicle)

- The bus is a new entity of type `VehicleType.BUS`, rendered as an elongated
  red block structure using the existing `CarRenderer` pipeline.
- It follows a fixed waypoint path between the 3 stops at 6 blocks/second.
- **Arrival behaviour**: when a bus arrives, it pauses for 8 seconds at the stop.
  All queued NPCs board (despawn from world, counted as boarded). Player can
  board by pressing **E** near the bus during the 8-second window.
- **Skip-stop behaviour**: if no NPCs or player are at the stop AND the player
  has not flagged the bus (by pressing **F** while facing it), the bus drives
  past without stopping. This is canonical.
- **Flag the bus**: pressing **F** while the bus is within 15 blocks and
  approaching causes it to stop. Without flagging, a passing bus ignores you.
  First-time tooltip: "You have to wave it down. This isn't a taxi."

### Fare & Player Actions

| Action | Effect |
|--------|--------|
| Board normally (press **E**) | Costs `BUS_FARE` coins (2 by default); player fast-travels to next stop (position teleports, 3-second fade) |
| Ride without paying | Press **E** without enough coins; adds +1 Notoriety if a `TICKET_INSPECTOR` NPC is on board (30% chance) |
| Pickpocket a queuing commuter | Standard pickpocket mechanic; +1–3 COIN; queuing NPCs are distracted and face away (easier than street pickpocket) |
| Throw NPC under the bus | Throw a NPC into the bus path (push mechanic); bus stops, player gets +10 Notoriety; POLICE spawn immediately |
| Night Bus brawl | After 22:00, DRUNK NPCs on the Night Bus have 50% chance of entering `BRAWLING` state on arrival at stop |

### Ticket Inspector

- **30% chance** a `TICKET_INSPECTOR` NPC spawns on the bus each journey.
- Ticket inspector checks all passengers during travel. Player must have paid or
  provide a `FAKE_ID` (consumed) to avoid penalty.
- Caught without ticket: +1 Notoriety, +5 coins fine deducted, `CriminalRecord`
  entry "Fare Evasion".
- At Notoriety Tier 3+, the inspector calls ahead and a POLICE NPC waits at the
  next stop.

### Dynamic Pricing & Market Events

| Condition | Fare multiplier |
|-----------|----------------|
| Rush hour (07:00–09:00 and 17:00–19:00) | ×1.5 |
| Night Bus (23:00–06:00) | ×2.0 |
| `COUNCIL_CRACKDOWN` market event | ×1.75 (austerity surcharge) |
| Player Notoriety Tier 4+ | ×1.0 (police aware — driver hesitates but must legally stop) |

### NPC Integration

- **COMMUTER** (new NPCType subvariant of PUBLIC): spawns near residential areas
  at 07:30 and 17:30; paths to nearest bus stop; boards bus and despawns (arrives
  at work/home off-screen).
- NPCs waiting at the bus stop grumble contextually:
  - "Been waiting 20 minutes." (if bus is late by >2 in-game minutes)
  - "It's always bloody late."
  - "That's the third one that's driven past full."
  - "I should've got the Uber."
- If the bus does not arrive within `LATE_THRESHOLD_MINUTES` (3 in-game minutes)
  of the scheduled time, a `DELAY_NOTICE` rumour is seeded into the RumourNetwork.
- On `GREGGS_STRIKE` market event, Greggs workers abandon the bus queue and walk
  to the picket line instead.

### New Materials

| Material | Description |
|----------|-------------|
| `BUS_PASS` | Weekly bus pass — craftable from 3 COIN + 1 NEWSPAPER. Grants unlimited rides for 7 in-game days. Shown in inventory as a card. |
| `INSPECTOR_BADGE` | Looted from a knocked-out `TICKET_INSPECTOR`. Wearing it grants free rides but causes all NPCs on the bus to become nervous. |

### New NPCType

| Type | Stats | Notes |
|------|-------|-------|
| `TICKET_INSPECTOR` | HP 25, 0 dmg, passive | Checks tickets; calls police at Notoriety Tier 3+; wears a hi-vis vest; can be bribed for 3 COIN |
| `BUS_DRIVER` | HP 30, 0 dmg, passive | Sits in driver seat; unkillable while driving; panics if player boards aggressively (triggers alarm) |

### Achievements

| Achievement | Unlock condition |
|-------------|-----------------|
| `MISSED_THE_BUS` | Watch 3 buses drive past you without stopping |
| `FARE_DODGER` | Successfully ride without paying 5 times |
| `LAST_NIGHT_BUS` | Board the Night Bus after 01:00 |
| `COMMUTER_PICKPOCKET` | Pickpocket 3 queuing commuters in a single morning rush |

### Integration tests — implement these exact scenarios:

1. **Bus arrives on schedule and picks up NPCs**: Place 3 COMMUTER NPCs at a bus
   stop. Set `TimeSystem` to 1 minute before the scheduled departure. Advance
   time to the scheduled departure. Verify a bus entity has spawned and is
   moving toward the stop. Advance until the bus arrives (within 8-second
   window). Verify all 3 COMMUTER NPCs have despawned (boarded). Verify bus
   departs and moves toward the next stop.

2. **Bus skips stop when no passengers and not flagged**: Set the bus route in
   motion. Ensure no NPCs are at the second stop and the player is not within
   15 blocks. Advance the bus to the second stop. Verify the bus does NOT pause
   (its velocity never drops to 0 at the stop position). Verify no boarding
   event is triggered.

3. **Player flags bus and boards**: Place the player at a bus stop, facing the
   approaching bus (within 15 blocks). Press **F** (flag). Verify `BusSystem`
   registers the flag. Advance until the bus arrives. Verify the bus pauses for
   8 seconds. Press **E**. Verify `BUS_FARE` coins are deducted from inventory.
   Verify the player's position changes to the next stop (fast-travel occurred).

4. **Fare evasion triggers inspector penalty**: Board a bus without sufficient
   coins (give player 0 COIN). Seed the RNG so a TICKET_INSPECTOR spawns on
   this journey. Advance the journey. Verify the inspector's dialogue fires
   ("Ticket please"). Verify player Notoriety increases by 1. Verify player
   inventory loses 5 COIN (capped at 0). Verify a `FARE_EVASION` entry is added
   to `CriminalRecord`.

5. **Night Bus spawns after 23:00 at double fare**: Set `TimeSystem` to 23:05.
   Verify the standard bus does NOT spawn. Verify the Night Bus spawns (interval
   60 in-game minutes). Verify `getEffectiveFare()` returns `BUS_FARE × 2.0`.
   Verify that after 01:00 the `LAST_NIGHT_BUS` achievement is unlockable by
   boarding.

---

## Phase 8j: The Pub Lock-In — After-Hours Drinking, Darts & Pub Quiz

**Goal**: Give The Ragamuffin Arms a living, after-hours social scene. When last
orders are called at 23:00 the landlord quietly locks the front door and anyone
still inside gets to stay for an illegal lock-in. The player can be one of them —
or can tip off the police for a bribe. Includes a darts mini-game, a pub quiz
(every Thursday at 20:00), and a landlord `TERRY` NPC with his own opinions.

---

### Lock-In Schedule & Triggering

A new `PubLockInSystem` class manages the entire phase.

- **Last orders**: At in-game 22:45, `BARMAN` NPC calls "Last orders please!" (speech
  bubble + rumour seeded into all pub-interior NPCs). Any NPC outside the pub stops
  pathfinding toward it.
- **Lock-in begins**: At 23:00, the pub's front door block changes to a `LOCKED_DOOR`
  state (impenetrable from outside, openable from inside). Up to `MAX_LOCK_IN_GUESTS`
  (8) pub-interior NPCs and the player (if present) become **lock-in guests**. All
  others are gently ejected (teleported 2 blocks outside the door; speech: "On yer way,
  love.").
- **Lock-in ends**: At 01:30, all guests are sent home (NPCState → WANDERING, door
  unlocked). If police raided before then, lock-in ends immediately.
- **Police raid chance**: Every 10 in-game minutes during the lock-in, there is a
  `POLICE_RAID_CHANCE` (20%) that a `POLICE` NPC knocks on the door. If the player
  **tips off** the police (see below) the raid is guaranteed on the next cycle.

### Landlord Terry NPC

A new `NPCType.LANDLORD` — `TERRY` — stands behind the bar throughout the lock-in.
He is distinct from the `BARMAN` (who works day shifts). Terry:

- Has 10 unique speech lines he cycles through, delivered every 2–4 in-game minutes
  to the nearest guest:
  - "This is a private gathering. Nothing illegal about friends having a drink."
  - "The council can do one."
  - "You want a top-up? On me. Don't tell the wife."
  - "Last copper who came in here left without his hat. Just saying."
  - "Best pub in the borough. Used to be, anyway."
  - "Don't mind him. He's been like that since Thatcher."
  - "You're alright, you are. For a stranger."
  - "Lock-in rule: what happens here stays here. Right?"
  - "Police round here can't find their arse with both hands."
  - "Another one? Go on then. This recession won't drink itself."
- Terry sells drinks at **half price** during the lock-in (lock-in discount):
  - Pint of lager: 1 COIN (normally 2)
  - Double whisky: 2 COIN (normally 3)
  - Cup of tea: still 1 COIN — "Tea's the same price. Non-negotiable."
- Pressing **E** on Terry during the lock-in reveals his top barman rumour for free
  (same as the `BARMAN` mechanic in Phase 8b, no drink required — Terry just talks).
- Terry has `HP 50`, does not fight, but if the player hits him three times he ejects
  them permanently ("Get out and don't come back.") — door opens for player only,
  then locks again.

### Darts Mini-Game

A `PropType.DARTBOARD` in the pub corner can be interacted with (**E**) at any time
the pub is open (not just lock-in). Implemented in a new `DartsMinigame` class.

**Mechanics:**

- Two players: the player character and a randomly selected `REGULAR` NPC opponent.
- Best of 3 legs. Each leg starts at 301; players alternate turns; first to exactly 0
  wins the leg (must finish on a double — last dart must score an even number that
  reduces score to exactly 0; going below 0 is a "bust", score resets to leg start).
- Each **turn** consists of 3 dart throws. The player "throws" by pressing **E** once
  for each dart; a random score between 1 and 20 is generated (with 5% chance of
  hitting the bullseye = 25, and 2% chance of double bullseye = 50). Trebles and
  doubles are simulated via the RNG: 10% chance of triple, 15% chance of double.
- The NPC opponent throws automatically with a skill level based on `NPCType.REGULAR`
  accuracy (mean 14, std dev 4, clamped 1–20 before multiplier).
- **Stake**: Player can optionally wager 1–5 COIN before the game starts (press E on
  dartboard, dialogue: "Fancy a game? Money on it?"). Opponent matches the stake.
  Winner takes the pot.
- Tooltip on first win: "Turned out you're handy with a dart. Who knew."
- Tooltip on first bust: "Went bust. Classic."

### Pub Quiz (Every Thursday, 20:00–22:00)

A `PubQuizSystem` class runs a quiz every in-game Thursday evening. The quiz is hosted
by a `QUIZ_MASTER` NPC who appears only during quiz nights.

**Format:**
- 5 rounds of 3 questions each (15 questions total).
- Questions are drawn from a hardcoded bank of 40 British general-knowledge questions
  (pop culture, geography, football, history — the kind asked in every pub in England).
  Examples:
  - "What is the capital of Scotland?" → Edinburgh
  - "Which year did England win the World Cup?" → 1966
  - "What is the name of the Queen's corgi in _The Crown_?" → (trick — no correct
    answer, quiz master apologises)
  - "How many players in a cricket team?" → 11
  - "Which supermarket chain has a 'Finest' range?" → Tesco
- Player answers by pressing 1/2/3/4 to select from multiple-choice options displayed
  in the speech log (A/B/C/D mapped to keys 1/2/3/4).
- NPC teams (2–4 teams of 2–3 REGULAR NPCs each) also compete; their answers are
  auto-resolved with 60% correct rate.
- **Scoring**: 1 point per correct answer. Player's team = player alone (solo entry).
- **Prize**: Winning team takes `QUIZ_POT` = 2 × number of teams × entry fee.
  Entry fee = 3 COIN (deducted when quiz starts). If player wins:
  - Receives QUIZ_POT COIN.
  - Achievement `QUIZ_NIGHT_CHAMPION` unlocked.
  - Terry says: "First time I've seen someone actually win in years."
- If the player cheats (walks out mid-quiz and re-enters, detected by position check):
  Terry says "Oi! You can't walk out mid-round!" and bans the player from the next
  quiz night.
- Tooltip on first quiz: "A pub quiz. The pinnacle of British intellectual life."

### Tipping Off the Police

At any point during the lock-in, the player can **tip off the police** by pressing
**E** on the `LOCKED_DOOR` from inside and selecting "Tip off the police". This:

1. Requires Notoriety ≤ Tier 2 (higher-tier players are known criminals — police
   won't take their tip seriously).
2. Costs 0 COIN but immediately sets `nextRaidTimer` to 0 (raid on next cycle).
3. Guarantees a `POLICE` NPC knocks at the door within 30 in-game seconds.
4. Adds `+3 Notoriety` (snitching still has a cost, even when legal).
5. All lock-in guests who are caught receive a `DRUNK_AND_DISORDERLY` criminal
   record entry. The player does not (they tipped off — technically co-operating).
6. Achievement `GRASS` unlocked. Terry remembers — next time the player enters
   the pub, Terry says: "I know what you did. Get out."

### Police Raid Sequence

When a raid is triggered:

1. A `POLICE` NPC (officer) pounds on the door (sound effect: `SoundEffect.HEAVY_KNOCK`).
   Terry says: "Everyone act natural."
2. Player has **5 real seconds** to hide (duck behind the bar counter — position within
   1 block of the bar). If the player is behind the bar when the door opens, they are
   not caught.
3. After 5 seconds, the door opens and the officer enters. Any guest not behind the bar
   is caught: `DRUNK_AND_DISORDERLY` added to criminal record, Notoriety +3.
4. The officer issues a `CriminalRecord.CrimeType.DRUNK_AND_DISORDERLY` to each caught
   guest and leaves. Terry is fined (flavour: "Another fine. Cheers, boys."). Lock-in
   ends.
5. If the player is hiding, they are not caught. Achievement `STAYED_BEHIND_THE_BAR`.

### Integration with Existing Systems

- **WarmthSystem**: Being inside the lock-in pub counts as `INDOORS` for warmth
  purposes (same as existing shelter logic — no cold drain).
- **NotorietySystem**: Tipping off adds +3 Notoriety. Getting caught in a raid adds
  +3 Notoriety.
- **RumourNetwork**: Lock-in raid generates a `RumourType.PLAYER_SPOTTED` rumour seeded
  into all post-raid NPCs ("The Old Bill raided the Ragamuffin Arms lock-in last night").
- **WeatherSystem**: During `FROST` weather, Terry offers free tea at 22:30: "Cold out
  there. Have a brew on the house." (1 CUP_OF_TEA added to player inventory).
- **AchievementSystem**: See achievements table below.
- **NewspaperSystem**: A raid generates a headline the next in-game morning:
  "Police Break Up Illegal Drinking Den — Seven Charged".
- **BusSystem**: The Night Bus stop nearest the pub is slightly more likely (30% bonus
  chance) to have DRUNK NPCs after a successful (un-raided) lock-in dispersal.

### New Materials

| Material | Description |
|----------|-------------|
| `PINT_LOCK_IN` | A half-price lock-in pint. Functionally identical to `PINT` but consumed immediately when bought from Terry; sets `drunkTimer` to 60s. Not stored in inventory. |
| `CUP_OF_TEA` | Hot tea. Restores 10 energy. Same as the pub's regular tea but Terry's is better. |

(Note: `PINT_LOCK_IN` is an internal sentinel used only by `PubLockInSystem`; it does
not appear in player inventory. `CUP_OF_TEA` is a new `Material` enum entry.)

### New NPCType

| Type | Stats | Notes |
|------|-------|-------|
| `LANDLORD` | HP 50, 0 dmg (passive), ejects on 3 hits | Terry. Lock-in host. Present 22:45–01:30 only. |
| `QUIZ_MASTER` | HP 20, 0 dmg, passive | Hosts the Thursday quiz. Appears 19:45–22:15. |

### New CriminalRecord.CrimeType

| Crime | Notoriety gained | Notes |
|-------|-----------------|-------|
| `DRUNK_AND_DISORDERLY` | +3 | Assigned when caught in a police raid of the lock-in. |

### Achievements

| Achievement | Unlock condition |
|-------------|-----------------|
| `LOCK_IN_REGULAR` | Attend 5 lock-ins without being caught in a raid |
| `STAYED_BEHIND_THE_BAR` | Hide behind the bar and escape a police raid |
| `QUIZ_NIGHT_CHAMPION` | Win a pub quiz |
| `DARTS_HUSTLER` | Win 3 darts games in a row with a stake |
| `GRASS` | Tip off the police during a lock-in |
| `LOCK_IN_LEGEND` | Attend the lock-in 10 times total |

**Unit tests**: Lock-in guest selection, door locking/unlocking, Terry's price
discount calculation, darts scoring (301 countdown, bust detection, double-out rule,
NPC accuracy distribution), pub quiz question selection and scoring, police raid
timing, tip-off mechanics, Terry ejection logic, quiz cheat detection.

**Integration tests — implement these exact scenarios:**

1. **Lock-in guests selected and door locked at 23:00**: Place 5 REGULAR NPCs inside
   the pub and 3 REGULAR NPCs outside. Set `TimeSystem` to 22:59. Advance to 23:00.
   Verify the pub front door is in `LOCKED_DOOR` state (cannot be opened from outside).
   Verify exactly 5 guests (the interior NPCs) are registered as lock-in guests.
   Verify the 3 exterior NPCs are NOT in the guest list. Verify `PubLockInSystem`
   `isLockInActive()` returns true.

2. **Terry sells drinks at half price during lock-in**: Activate the lock-in (set
   `isLockInActive = true`). Give the player 5 COIN. Press E on Terry and buy a pint.
   Verify player now has 4 COIN (cost 1, not 2). Buy a double whisky. Verify player
   now has 2 COIN (cost 2, not 3). Verify `drunkTimer` is > 0 after purchase.

3. **Player hides behind bar and escapes raid**: Place the player within 1 block of the
   bar counter during a lock-in. Trigger a police raid (call
   `pubLockInSystem.triggerRaid()`). Advance 5 real seconds. Verify player is NOT
   added to `DRUNK_AND_DISORDERLY` list. Verify player Notoriety has NOT increased.
   Verify achievement `STAYED_BEHIND_THE_BAR` is unlocked.

4. **Player caught in raid adds criminal record entry**: Place the player 5 blocks from
   the bar (not hiding). Trigger a police raid. Advance past the 5-second window.
   Verify player's `CriminalRecord` contains a `DRUNK_AND_DISORDERLY` entry. Verify
   Notoriety increased by 3. Verify `isLockInActive()` returns false (lock-in ended).

5. **Tip-off guarantees raid and unlocks GRASS achievement**: Set player Notoriety to
   Tier 1 (≤ 249). Player presses E on the locked door and selects "Tip off the
   police". Verify `nextRaidTimer` is set to 0. Advance 30 in-game seconds. Verify a
   POLICE NPC has spawned at the pub entrance. Verify achievement `GRASS` is unlocked.
   Verify player Notoriety increased by 3.

6. **Darts game completes with correct scoring**: Construct `DartsMinigame` with a
   seeded RNG. Simulate player throws. After each throw, verify the running score
   decrements correctly. Simulate a finishing sequence: set score to 4, verify that a
   throw of 2 (double-2) finishes the leg. Verify single-4 (non-double) does NOT
   finish. Verify going to -1 (bust) resets score to 4. Verify leg win triggers
   correctly on the third correct double-out.

7. **Darts stake payout**: Give player 3 COIN. Start a darts game with a 3-COIN stake.
   Verify player's COIN decreases by 3 immediately. Seed the RNG so the player wins
   all 3 legs. Verify player receives 6 COIN (pot = 6, player's stake + opponent
   stake). Verify net gain = +3 COIN.

8. **Pub quiz runs on Thursday and accepts answers**: Set `TimeSystem` to Thursday
   20:00. Verify `PubQuizSystem.isQuizNight()` returns true. Verify a `QUIZ_MASTER`
   NPC has spawned inside the pub. Give the player 3 COIN (entry fee). Press E on
   the QUIZ_MASTER to enter. Verify 3 COIN deducted. Simulate the first question
   being displayed in the speech log. Press key 1 (answer A). Verify the player's
   score increments by 1 if answer A is correct for that question (use a seeded
   question bank so the correct answer is known).

9. **Quiz cheat detection bans player from next quiz**: Enter a quiz night. Move the
   player outside the pub (simulate walk-out mid-quiz). Re-enter within the quiz
   window. Verify Terry's speech fires: "Oi! You can't walk out mid-round!" Verify
   `PubQuizSystem.isPlayerBanned()` returns true for the next quiz night (next
   Thursday). Verify the following Thursday, pressing E on the QUIZ_MASTER returns
   a BANNED response.

10. **Full lock-in stress test**: Set time to Wednesday 22:50. Fill the pub with 8
    REGULAR NPCs. Advance to 23:00. Verify lock-in starts. Advance to 23:30 (first
    possible raid window). Disable raid for this test (set `POLICE_RAID_CHANCE = 0`).
    Have the player win 2 darts games against NPCs. Buy 3 drinks from Terry. Advance
    to 01:30. Verify lock-in ends cleanly: door unlocks, all guests set to WANDERING,
    `isLockInActive()` returns false. Verify `LOCK_IN_REGULAR` progress incremented.
    Verify no NPEs and game state remains PLAYING throughout.

---

## Add Skate Park System — Tricks, Street Rep & Council Crackdown

**Goal**: Give the `SKATE_PARK` landmark actual gameplay. The concrete bowl exists
in the world; now skaters use it, the player can perform tricks to earn Street Lads
Respect and coins, and The Council periodically tries to shut it down.

### Overview

The skate park is a piece of contested public space — the Street Lads treat it as
their domain, youths hang out there after school, and The Council views it as an
eyesore that attracts antisocial behaviour. A new `SkateParkSystem` class manages:

- **Player tricks**: pressing T while sprinting on CONCRETE inside the park executes
  a trick. Tricks score points that convert to Street Lads Respect and coins.
- **Trick variety**: five tricks with ascending score values.
- **Council Enforcement Events**: periodic attempts to close the park (new signs,
  enforcement NPCs) that the player can sabotage.
- **Skater NPCs**: `YOUTH_GANG` and `SCHOOL_KID` NPCs gather here afternoons
  (14:00–22:00), performing ambient skating loops and reacting to player tricks.
- **ASBO mechanic**: accumulating too much trick notoriety in view of police earns
  an ASBO (Anti-Social Behaviour Order) — 5 Notoriety added, park banned for
  1 in-game day.

### Tricks

The player must be:
- Inside the `SKATE_PARK` landmark AABB.
- Moving (speed > 0.5 blocks/s).
- On or directly above a CONCRETE block (y == 0 or y == 1).

Pressing **T** consumes 5 energy and executes the current trick (selected by the
system based on the player's `SkatePark` skill rank):

| Rank | Trick name | Score | Unlocked at |
|------|-----------|-------|-------------|
| 0 | Kickflip | 10 | Default |
| 1 | Heelflip | 20 | 3 successful tricks |
| 2 | 50-50 Grind | 35 | 8 successful tricks |
| 3 | 720 Spin | 60 | 18 successful tricks |
| 4 | McTwist | 100 | 35 successful tricks |

A trick **fails** (adds 0 score, wastes 5 energy, plays a crash sound) if:
- The player is not moving fast enough (speed < 1.0 blocks/s).
- Energy < 5.
- Player performed a trick within the last 2.0 seconds (cooldown).

On success:
- Score accumulates in `SkateParkSystem.sessionScore`.
- Every 50 accumulated score: award 1 COIN directly to player inventory.
- Every 100 accumulated score: award +5 Street Lads Respect.
- First ever successful trick: tooltip "Sick kick. The lads are watching."
- On McTwist success: tooltip "Absolute legend. How is your ankle?"

### Trick Key Binding

**T** — Perform skate trick (when in skate park). Added to Help UI.

### Council Enforcement Events

Every 8 in-game hours, The Council has a 40% chance to initiate a **Park Closure
Attempt**:

1. A `COUNCIL_MEMBER` NPC walks to the park entrance carrying a clipboard.
2. A `CLOSURE_NOTICE` prop (yellow sign) is placed on the park perimeter wall
   nearest to the entrance.
3. A speech bubble on the `COUNCIL_MEMBER`: "This facility is to be closed pending
   a risk assessment and licensing review."
4. If the closure notice is not removed within 10 in-game minutes, the park
   entrance gap is blocked (a CONCRETE block placed over it), and a 30-minute
   lockout begins — players and NPCs cannot enter.

**Player can sabotage the closure**:
- Press **E** on the `CLOSURE_NOTICE` prop to tear it down (if player Notoriety
  Tier < 3; otherwise it triggers ASBO — see below).
- Alternatively, bribe the `COUNCIL_MEMBER` with 8 COIN (press **E** on them,
  select "Sort it out") to cancel the event outright.
- Tearing down the notice: Council Respect −5, Street Lads Respect +10.
- Bribing: Council Respect +5 (they got paid), Street Lads Respect +5.
- Each sabotage contributes +1 to `CLOSURE_NOTICES_TORN_DOWN` counter.

### ASBO Mechanic

If a `POLICE` or `PCSO` NPC is within 8 blocks of the player when they perform
a trick, there is a 30% chance per trick of receiving an ASBO:

- `NotorietySystem` Notoriety +5.
- `CriminalRecord` receives a `ANTISOCIAL_BEHAVIOUR` offence entry.
- Player receives a `PARK_BANNED` flag for 1 in-game day. While banned:
  - Entering the `SKATE_PARK` AABB causes the nearest NPC to call police.
  - The `CLOSURE_NOTICE` event cooldown is halved (Council sees opportunity).
- Tooltip on first ASBO: "ASBO acquired. Your mum will be so proud."

### Skater NPCs

Between **14:00 and 22:00**, the skate park attracts NPCs:
- 3–6 `SCHOOL_KID` NPCs wander inside the park AABB.
- 1–2 `YOUTH_GANG` NPCs loiter at the perimeter wall.
- While at the park, `SCHOOL_KID` NPCs cycle through a skating animation
  (internally: they teleport 1 block every 2 seconds in a figure-8 pattern
  within the AABB — approximate a skating loop).

NPC reactions to the player's tricks:
- On a 720 Spin or McTwist success: all `SCHOOL_KID` and `YOUTH_GANG` NPCs
  within 15 blocks emit a speech bubble: "BRUUUH" or "Mad ting" (random from list).
- On a trick failure (crash): nearby NPCs emit: "Get rekt" or "Unlucky, son."

### Integration

- **`FactionSystem`**: Street Lads Respect gains from tricks are channelled through
  `FactionSystem.addRespect(Faction.STREET_LADS, delta)`.
- **`TimeSystem`**: NPC spawn window gated by `timeSystem.getHour()`.
- **`NotorietySystem`**: ASBO adds Notoriety; high notoriety (≥ Tier 3) means
  council closure events happen twice as often.
- **`RumourNetwork`**: On a McTwist success, a `PLAYER_SPOTTED` rumour is seeded
  in the nearest NPC ("Someone just pulled a McTwist down the skate park. Actual
  legend.").
- **`NewspaperSystem`**: If `CLOSURE_NOTICES_TORN_DOWN` ≥ 3 in one game day, a
  front-page story is generated: "Local Tearaway Defies Council's Skate Park
  Crackdown — For the Third Time." Infamy +2.
- **`AchievementSystem`**: New achievements:

| Achievement | Trigger |
|-------------|---------|
| `KICKFLIP_KING` | Perform 10 successful tricks in one session |
| `COUNCIL_SABOTEUR` | Tear down 3 closure notices |
| `ASBO_MAGNET` | Receive 3 ASBOs |
| `PARK_LEGEND` | Achieve McTwist (rank 4) |

### New Materials / Props

- `Material.SKATEBOARD` — craftable (2 WOOD + 1 PLANKS). Not required to skate
  (the player skates anyway), but holding a SKATEBOARD in the hotbar gives +15%
  trick score multiplier. Tooltip on first craft: "Technically it's a weapon too."
- `PropType.CLOSURE_NOTICE` — yellow sign prop. Placed by Council enforcement event.
  Removable via E key interaction.

### Unit Tests

- Trick scoring formula (correct score per trick rank).
- Trick cooldown enforced (second trick within 2s always fails).
- Energy consumption on success and failure.
- ASBO probability gated by police proximity (30% with police nearby, 0% without).
- Council enforcement event timing (40% chance per 8-hour tick).
- PARK_BANNED flag blocks re-entry NPC call.
- NPC spawn window (no NPCs spawned before 14:00 or after 22:00).
- Skateboard multiplier (1.15× score when equipped).

### Integration Tests — implement these exact scenarios

1. **Successful kickflip awards coins**: Place the player inside the `SKATE_PARK`
   AABB at position (skX + 5, 1, skZ + 5) on a CONCRETE block. Set player speed to
   1.5 blocks/s. Set player energy to 100. Ensure no police NPCs are within 20 blocks.
   Press T (call `skateParkSystem.attemptTrick(player, world, timeSystem, npcManager)`).
   Verify `getLastTrickResult()` returns `SUCCESS`. Verify `getSessionScore()` == 10
   (Kickflip). Verify player energy is 95. Verify no coins awarded yet (< 50 score).
   Press T four more times (each success). Verify `getSessionScore()` == 50. Verify
   player inventory now contains 1 COIN.

2. **Trick fails when moving too slowly**: Place player inside the park, speed set to
   0.3 blocks/s. Energy 100. Call `attemptTrick()`. Verify result is `FAIL_TOO_SLOW`.
   Verify `getSessionScore()` is still 0. Verify player energy is 95 (energy still consumed
   on a failed attempt — you tried).

3. **Trick cooldown prevents double-trick**: Call `attemptTrick()` — success. Immediately
   call `attemptTrick()` again (within same frame, delta = 0.1s). Verify second result
   is `FAIL_COOLDOWN`. Verify score did not increase a second time.

4. **Skill rank progresses and unlocks heelflip**: Start with 0 successful tricks.
   Simulate 3 successful tricks (mock `SUCCESS` results). Verify `getSkillRank()` == 1
   (Heelflip unlocked). Verify next successful trick returns score 20 (not 10).

5. **Council enforcement event places closure notice**: Call
   `skateParkSystem.triggerEnforcementEvent(world, npcManager)`. Verify a
   `COUNCIL_MEMBER` NPC has been added to the world near the park entrance. Verify a
   `CLOSURE_NOTICE` prop exists on the park perimeter. Verify `isClosureEventActive()`
   returns true.

6. **Tearing down notice awards Street Lads Respect**: Active closure event present.
   Place player adjacent to `CLOSURE_NOTICE` prop. Press E (call `onPlayerInteractClosure
   Notice(player, factionSystem, notorietySystem)`). Verify `CLOSURE_NOTICE` prop is
   removed. Verify `factionSystem.getRespect(Faction.STREET_LADS)` increased by 10.
   Verify `factionSystem.getRespect(Faction.COUNCIL)` decreased by 5. Verify
   `isClosureEventActive()` returns false.

7. **Park lockout activates after 10 minutes without tear-down**: Trigger enforcement
   event. Advance `TimeSystem` by 10 in-game minutes without the player interacting.
   Verify `isParkLocked()` returns true. Verify the entrance gap in the park perimeter
   is now a solid CONCRETE block (`world.getBlock(skX + width/2, 1, skZ) == CONCRETE`).
   Verify `getLockedOutTimer()` > 0.

8. **ASBO received when police nearby during trick**: Place a `POLICE` NPC 5 blocks from
   the player inside the park. Set `SkateParkSystem.ASBO_CHANCE = 1.0f` (deterministic).
   Call `attemptTrick()` with a successful trick. Verify `notorietySystem.getNotoriety()`
   increased by 5. Verify `criminalRecord.getOffenceCount(CriminalRecord.CrimeType
   .ANTISOCIAL_BEHAVIOUR)` increased by 1. Verify `isPlayerBanned()` returns true.

9. **Skater NPCs spawn in afternoon window**: Set `TimeSystem` to 15:00. Call
   `skateParkSystem.update(delta, timeSystem, npcManager, world)`. Verify at least 3
   `SCHOOL_KID` NPCs have been added within the `SKATE_PARK` AABB. Set time to 23:00.
   Call `update()`. Verify those NPCs have been removed (or moved out of the AABB).

10. **McTwist seeds a rumour**: Progress player to skill rank 4. Call `attemptTrick()`
    with a nearby `PUBLIC` NPC within 15 blocks. Verify a rumour containing "McTwist"
    has been added to that NPC's rumour buffer via `rumourNetwork`. Verify tooltip
    "Absolute legend. How is your ankle?" is queued.

---

## Launderette System — Wash Your Dirty Laundry (Literally)

**Goal**: Make the `LAUNDERETTE` landmark a functional location where the player
can launder clothes (and reputation). The Spotless Launderette is a liminal British
space — fluorescent lights, the smell of fabric softener, and NPCs airing their
grievances while waiting for a 40-degree cycle to finish.

### Core Mechanics

**Washing Machine** (`PropType.WASHING_MACHINE`): the player interacts (**E**) with
a washing machine prop inside the launderette to start a wash cycle.

- A wash cycle costs **2 COIN** and takes **90 real seconds** (one in-game cycle).
- While a cycle is running, the player cannot use that machine (it belongs to them).
- On completion, the player collects a `Material.CLEAN_CLOTHES` item from the machine.

**Notoriety Scrub**: If the player interacts with the machine while wearing a
`Material.BLOODY_HOODIE` or `Material.STOLEN_JACKET` (tracked via
`DisguiseSystem.getCurrentDisguise()`), the wash also reduces their Notoriety by
**−2** and removes the `COVERED_IN_BLOOD` debuff flag. Tooltip on first use:
"Nothing says 'fresh start' like a 40-degree cycle."

**Waiting NPCs**: 1–3 `PUBLIC` NPCs are always present in the launderette. Because
they have nothing to do while their clothes wash, they are especially chatty:

- Any NPC waiting here shares rumours without requiring the player to have bought a
  drink (overrides the `drunkTimer` check in `RumourNetwork`). The launderette is
  treated as a "loose-tongue zone".
- Each waiting NPC has an elevated `NeedType.BORED` need (starting at 60), making
  them prime targets for street deals.

**Coin-Op Drama**: Every 5 in-game minutes there is a **30% chance** of a random
`LaunderetteEvent` occurring:

| Event | Effect |
|-------|--------|
| `MACHINE_STOLEN` | A random NPC grabs someone else's laundry — that NPC becomes AGGRESSIVE for 2 minutes. The player can intervene (press E on the thief) to broker peace (+5 Neighbourhood Watch anger reduction) or ignore it. |
| `SOAP_SPILL` | SLIPPERY floor for 60 seconds — player and NPCs move at 70% speed inside. |
| `POWER_CUT` | All running cycles pause for 30 seconds (timer frozen). NPC speech: "Not again. Council's fault." |
| `SUSPICIOUS_LOAD` | A `FENCE`-type NPC appears briefly with a machine full of "laundry". Player can press E to buy one `STOLEN_JACKET` for 5 COIN. |

### Disguise Interaction

After collecting `CLEAN_CLOTHES`, the player can press **E** on the changing-room
prop (`PropType.CHANGING_CUBICLE`) to equip `CLEAN_CLOTHES` as a disguise:

- Replaces current disguise in `DisguiseSystem`.
- Grants the `FRESHLY_LAUNDERED` buff: Notoriety recognition chance reduced by **20%**
  for 3 in-game minutes (NPCs are less likely to recognise the player as wanted).
- Tooltip: "Clean clothes. The perfect disguise."

### Integration

- **`DisguiseSystem`**: `CLEAN_CLOTHES` disguise reduces NPC recognition probability;
  `BLOODY_HOODIE`/`STOLEN_JACKET` trigger the Notoriety scrub.
- **`NotorietySystem`**: Wash cycle reduces Notoriety by −2 when disguise is a
  dirty/wanted item.
- **`RumourNetwork`**: Launderette is a loose-tongue zone — waiting NPCs share
  rumours freely (no `drunkTimer` required).
- **`NeighbourhoodWatchSystem`**: Intervening in `MACHINE_STOLEN` event reduces
  WatchAnger by 5.
- **`StreetEconomySystem`**: Waiting NPCs have elevated BORED need — good deal targets.
- **`TimeSystem`**: Launderette is open 07:00–22:00. Outside these hours the door is
  blocked by a `CLOSED_SIGN` prop. Late-night use requires the player to break in
  (adds `ANGER_VISIBLE_CRIME` to NeighbourhoodWatch).
- **`NewspaperSystem`**: A `SUSPICIOUS_LOAD` event where the player buys a
  `STOLEN_JACKET` can generate an infamy event: "FENCE SPOTTED AT LOCAL LAUNDERETTE".

### New Materials / Props

- `Material.CLEAN_CLOTHES` — result of a wash cycle. Wearable as a disguise.
- `Material.BLOODY_HOODIE` — dropped by the player after taking significant damage in
  combat (>30 HP lost in one fight). Triggers Notoriety scrub when washed.
- `PropType.WASHING_MACHINE` — interactable prop; displays cycle progress as a
  percentage in the speech-log overlay.
- `PropType.CHANGING_CUBICLE` — interactable prop for equipping laundered clothes.

### Achievements

| Achievement | Trigger |
|-------------|---------|
| `FRESH_START` | Complete first wash cycle |
| `SMELLS_LIKE_CLEAN_SPIRIT` | Scrub Notoriety via laundry 3 times |
| `LAUNDERING` | Buy a `STOLEN_JACKET` from a `SUSPICIOUS_LOAD` event |
| `PEACEKEEPER_OF_SUDWORTH` | Broker peace in a `MACHINE_STOLEN` dispute |

### Unit Tests

- Wash cycle timer counts down correctly (90s → 0).
- Notoriety reduction only triggers on dirty/wanted disguise items.
- Loose-tongue zone overrides `drunkTimer` check (rumours shared without drink).
- BORED need is initialised to 60 for launderette waiting NPCs.
- LaunderetteEvent probability is 30% per 5-minute tick.
- `SOAP_SPILL` reduces movement speed to 0.7× for both player and NPCs.
- `POWER_CUT` freezes cycle timer for 30 seconds then resumes.
- Opening hours block — door is passable 07:00–22:00, blocked otherwise.
- `FRESHLY_LAUNDERED` buff reduces recognition probability by 20% and expires
  after 3 in-game minutes.

### Integration Tests — implement these exact scenarios

1. **Wash cycle costs coins and produces CLEAN_CLOTHES**: Give the player 5 COIN.
   Place the player facing a `WASHING_MACHINE` prop inside the `LAUNDERETTE` AABB.
   Press E (`launderetteSystem.startCycle(player, inventory, machine)`). Verify
   player now has 3 COIN (2 spent). Verify `machine.isCycleRunning()` is true.
   Advance the simulation by 90 real seconds. Verify `machine.isCycleRunning()` is
   false. Press E again. Verify player inventory now contains 1 `CLEAN_CLOTHES`.

2. **Wash removes Notoriety on dirty disguise**: Set player Notoriety to 20. Set
   current disguise to `BLOODY_HOODIE` via `DisguiseSystem`. Start a wash cycle.
   Advance 90s. Collect clean clothes. Verify `notorietySystem.getNotoriety()` == 18
   (reduced by 2). Verify `DisguiseSystem.hasBuff(COVERED_IN_BLOOD)` is false.

3. **Waiting NPCs share rumours without drink**: Spawn a waiting NPC inside the
   launderette with `NeedType.BORED` set to 70. Player has `drunkTimer == 0` (no
   drink bought). Press E on the NPC. Verify the NPC's top rumour is displayed in
   the speech log (rumour shared despite no drink — loose-tongue zone active).

4. **MACHINE_STOLEN event makes NPC aggressive**: Force a `MACHINE_STOLEN` event
   via `launderetteSystem.triggerEvent(LaunderetteEvent.MACHINE_STOLEN)`. Verify
   that exactly one waiting NPC has `NPCState.AGGRESSIVE`. Advance the simulation
   60 frames. Press E on the thief NPC (`launderetteSystem.onPlayerBrokePeace(...)`).
   Verify the thief NPC returns to `NPCState.IDLE`. Verify
   `neighbourhoodWatchSystem.getWatchAnger()` has decreased by 5.

5. **FRESHLY_LAUNDERED buff expires after 3 in-game minutes**: Collect CLEAN_CLOTHES.
   Use changing cubicle to equip them. Verify `disguiseSystem.hasBuff(FRESHLY_LAUNDERED)`
   is true. Advance `TimeSystem` by 3 in-game minutes. Verify the buff is no longer
   active. Verify the recognition chance penalty is back to the base rate.

6. **Launderette closed outside hours**: Set `TimeSystem` hour to 23:30. Place the
   player at the launderette entrance. Verify the entrance is blocked (a `CLOSED_SIGN`
   prop is present and the door block is solid). Attempt to press E on the door. Verify
   no cycle is started and the speech log shows "Sorry, we're closed." Set time to
   08:00. Verify the entrance is now open (door block is passable).

7. **SOAP_SPILL slows player movement**: Force `SOAP_SPILL` event. Verify player
   movement speed is multiplied by 0.7. Advance 60 real seconds. Verify speed returns
   to normal (1.0×). Verify NPCs inside the launderette also moved at reduced speed
   during the event.

---

## Chippy System — Tony's Chip Shop ('Salt? Vinegar? Open till Midnight.')

**Goal**: Make the `CHIPPY` landmark (`LandmarkType.CHIPPY` — "Tony's Chip Shop") a
fully interactive location. The chippy is a liminal British institution: harsh neon
lighting, condensation on the windows, a laminated menu above a bain-marie, and an
owner who has been there since 1987 and absolutely will not be rushed.

### Core Mechanics

**Ordering at the Counter** (`PropType.CHIPPY_COUNTER`): The player presses **E**
on the counter prop inside the chippy to open the `ChippyOrderUI` — a simple list
of menu items with prices. Items are purchased with COIN.

| Menu Item | Cost (COIN) | Effect |
|-----------|-------------|--------|
| `CHIPS` (large) | 2 | Restores 40 hunger; satisfies `NeedType.HUNGRY` |
| `BATTERED_SAUSAGE` | 2 | Restores 30 hunger + 10 energy |
| `CHIP_BUTTY` | 3 | Restores 50 hunger; needs 2 slices of bread in inventory, or Tony refuses ("Sorry love, we're out of bread.") |
| `MUSHY_PEAS` | 1 | Restores 15 hunger; satisfies `NeedType.COLD` by +5 (it's warm, alright?) |
| `PICKLED_EGG` | 1 | Restores 10 hunger; chance (20%) of `FOOD_POISONING` debuff: player moves at 80% speed for 60 real seconds |
| `FISH_SUPPER` | 4 | Restores 60 hunger + warmth +10; rare item — only available 2 in-game days out of 3 ("We're out of fish, love.") |
| `BOTTLE_OF_WATER` | 1 | Restores 10 thirst / generic buff |

All items arrive wrapped in `Material.WHITE_PAPER` — a collectable junk item that
piles up on tables and can be thrown (right-click while held) to create a litter-drop
NPC reaction. Tooltip on first chip purchase: "Salt? Vinegar? Open till midnight."

**Tony the Owner** (`NPCType.CHIPPY_OWNER`): A unique NPC (`TONY`) who stands
behind the counter and delivers dry one-liners. Tony has fixed dialogue lines
depending on time of day, player notoriety, and recent events:

- Default: "What can I get ya, love?"
- If Notoriety ≥ 20: "Saw your face in the paper. Still want chips, do ya?"
- If time is 23:30–00:00: "Almost closing time. Hurry up."
- If `GREGGS_RAID` rumour is active: "I heard about Greggs. Tragic. Still, more business for us."
- If player holds BALACLAVA: "Take that thing off before I serve you."
  (Refuses to serve until the BALACLAVA is removed or stashed.)

**Post-Pub Queue**: Between 23:00 and 01:00, 3–6 `PUBLIC` NPCs queue outside the
chippy in a line. These NPCs have `NeedType.HUNGRY` at 80+. If the player is in the
queue (within 5 blocks of the door), they can:
- **Queue-jump** (press **F** near the front) — gain service immediately, but 1
  queuing NPC becomes `NPCState.AGGRESSIVE` for 2 minutes. Adds +1 `ANTISOCIAL_BEHAVIOUR`
  to `CriminalRecord` if a POLICE NPC is within 20 blocks.
- **Wait normally** — player reaches counter after 30 real seconds; no penalty.

**Salt & Vinegar** (`Material.SALT_AND_VINEGAR_PACKET`): A consumable item sold
at the counter for 1 COIN. Interacting (**E**) with any chip item in the inventory
while holding SALT_AND_VINEGAR_PACKET combines them to produce `CHIPS_SEASONED`,
which restores 50 hunger instead of 40. Tooltip: "You absolute class act."

**Tony's Closing Wrath**: The chippy is open 11:00–00:00. At exactly 00:00 Tony
places a `CLOSED_SIGN` prop at the door and says "Right. Jog on." Any NPC still
inside is ushered out (`NPCState.FLEEING`). The player gets 10 seconds to
complete any pending transaction before being ejected (same as when the door blocks).

**The Chip Shop Cat** (`NPCType.STRAY_CAT`): A persistent `STRAY_CAT` NPC named
"Biscuit" lives inside the chippy. Biscuit:
- Spawns near the counter at opening time and despawns at closing.
- Has `NeedType.HUNGRY` that ticks up — the player can press **E** on Biscuit while
  holding any food item to feed them (HUNGRY reset to 0).
- Feeding Biscuit 3 times across separate sessions awards the `FED_THE_CAT` achievement.
- If the player punches Biscuit, all queuing NPCs immediately become AGGRESSIVE and
  Tony refuses service for the rest of the session. Tooltip:
  "You punched a cat. In a chip shop. You absolute monster."

### Integration

- **`StreetEconomySystem`**: Post-pub queue NPCs have `NeedType.HUNGRY` at 80+ —
  prime deal targets if the player has spare CHIPS to resell.
- **`NotorietySystem`**: Queue-jumping adds `ANTISOCIAL_BEHAVIOUR` when police are
  near. Punching Biscuit the cat adds +3 Notoriety immediately.
- **`CriminalRecord`**: Queue-jumping near police adds `ANTISOCIAL_BEHAVIOUR` offence.
- **`RumourNetwork`**: Any NPC who witnesses the player queue-jump seeds a
  `QUEUE_JUMP` rumour to nearby NPCs. If Biscuit is punched, a `CAT_PUNCH` rumour
  propagates town-wide — all PUBLIC NPCs become slightly hostile for 5 in-game minutes.
- **`WeatherSystem`**: During `RAIN` or `FROST`, post-pub queue NPCs spawn 30%
  sooner and the chippy interior counts as `ShelterDetector` warmth zone.
- **`WarmthSystem`**: Standing inside the chippy provides passive warmth restoration
  (+3 warmth/s) due to hot fat fryer environment.
- **`NewspaperSystem`**: If the player eats `FISH_SUPPER` 5 times, a positive
  `CHIPPY_REGULAR` infamy note appears: "Local seen keeping Tony's Chip Shop afloat."
- **`TimeSystem`**: Opening hours 11:00–00:00. Post-pub queue spawns 23:00–01:00.
- **`BuskingSystem`**: If the player busks within 5 blocks of the chippy entrance
  during the post-pub queue window, busking income increases by +50% (captive tipsy
  audience).

### New Materials / Props

- `Material.BATTERED_SAUSAGE` — menu item; restores 30 hunger + 10 energy.
- `Material.CHIP_BUTTY` — menu item; requires BREAD (from `TESCO_EXPRESS`) or
  refused; restores 50 hunger.
- `Material.MUSHY_PEAS` — menu item; restores 15 hunger + 5 cold relief.
- `Material.PICKLED_EGG` — menu item; 20% chance of FOOD_POISONING debuff.
- `Material.FISH_SUPPER` — premium menu item; available 2/3 days; restores 60 hunger.
- `Material.SALT_AND_VINEGAR_PACKET` — condiment item; combines with CHIPS → CHIPS_SEASONED.
- `Material.CHIPS_SEASONED` — enhanced chips; restores 50 hunger.
- `Material.BOTTLE_OF_WATER` — sold at counter for 1 COIN.
- `Material.WHITE_PAPER` — chip wrapping; collectable junk litter prop.
- `PropType.CHIPPY_COUNTER` — interactable counter prop; opens `ChippyOrderUI`.

### Achievements

| Achievement | Trigger |
|-------------|---------|
| `SALT_AND_VINEGAR` | Season chips with SALT_AND_VINEGAR_PACKET for the first time |
| `LAST_ORDERS` | Purchase from Tony in the final 10 minutes before closing |
| `FED_THE_CAT` | Feed Biscuit 3 times across sessions |
| `QUEUE_JUMPER` | Successfully queue-jump the post-pub queue |
| `CAT_PUNCHER` | Punch Biscuit (shame achievement — no tooltip congratulation) |
| `CHIPPY_REGULAR` | Buy from Tony's 10 times across sessions |

### Unit Tests

- `ChippySystem.isOpen(hour)` returns true for 11:00–00:00, false otherwise.
- FOOD_POISONING debuff applies with 20% probability on PICKLED_EGG purchase.
- FISH_SUPPER availability cycles correctly (available 2 out of 3 in-game days).
- Tony refuses to serve player holding BALACLAVA.
- Queue-jump increments `CriminalRecord.ANTISOCIAL_BEHAVIOUR` when police within 20 blocks.
- Feeding Biscuit 3 times awards `FED_THE_CAT` achievement.
- Post-pub queue spawns 3–6 NPCs between 23:00–01:00.
- CHIPS_SEASONED produced by combining CHIPS + SALT_AND_VINEGAR_PACKET.
- Warmth restoration rate inside chippy is +3/s.
- Punching Biscuit adds +3 Notoriety and triggers `CAT_PUNCH` rumour.

### Integration Tests — implement these exact scenarios

1. **Ordering chips costs coins and restores hunger**: Give the player 5 COIN and set
   hunger to 20. Set time to 13:00 (open). Place player facing a `CHIPPY_COUNTER`
   prop. Press E (`chippySystem.openOrderUI(player)` → select CHIPS → confirm).
   Verify player now has 3 COIN (2 spent). Verify player hunger is now 60 (20 + 40).
   Verify player inventory contains 1 `WHITE_PAPER` (the chip wrapping).

2. **Tony refuses BALACLAVA wearers**: Set player's active disguise to `BALACLAVA`
   via `DisguiseSystem`. Press E on `CHIPPY_COUNTER`. Verify `ChippySystem.getLastRefusalReason()`
   is `BALACLAVA_WORN`. Verify no COIN is spent and the speech log shows
   "Take that thing off before I serve you."

3. **PICKLED_EGG food poisoning debuff**: Set `ChippySystem.FOOD_POISONING_CHANCE = 1.0f`
   (deterministic). Give player 2 COIN. Buy PICKLED_EGG. Verify
   `chippySystem.isFoodPoisoned(player)` is true. Verify player movement speed
   multiplier is 0.8×. Advance 60 real seconds. Verify debuff has expired and speed
   is back to 1.0×.

4. **Queue-jump triggers ANTISOCIAL_BEHAVIOUR near police**: Spawn post-pub queue
   (set time to 23:30, call `chippySystem.spawnPostPubQueue(npcManager, world)`).
   Verify 3–6 PUBLIC NPCs exist within 5 blocks of chippy door. Place a POLICE NPC
   12 blocks from the player. Player presses F (`chippySystem.attemptQueueJump(player,
   npcManager, criminalRecord, world)`). Verify the front NPC becomes `NPCState.AGGRESSIVE`.
   Verify `criminalRecord.getOffenceCount(ANTISOCIAL_BEHAVIOUR)` increased by 1.

5. **Feeding Biscuit 3 times awards achievement**: Spawn `STRAY_CAT` NPC "Biscuit"
   via `chippySystem.spawnBiscuit(npcManager, world)`. Give player 3 CHIPS. Press E
   on Biscuit 3 times (`chippySystem.feedBiscuit(player, achievementSystem)`). Verify
   Biscuit's `NeedType.HUNGRY` is 0 after each feeding. Verify `FED_THE_CAT`
   achievement was awarded on the third feeding.

6. **Post-pub queue spawns only between 23:00–01:00**: Set time to 20:00. Call
   `chippySystem.update(delta, timeSystem, npcManager, world)`. Verify no post-pub
   queue NPCs have been spawned. Set time to 23:30. Call `update()` again. Verify
   3–6 PUBLIC NPCs are now queued within 5 blocks of the door. Set time to 02:00.
   Call `update()`. Verify those NPCs have been despawned (or moved to FLEEING).

7. **FISH_SUPPER unavailable every third day**: Set `ChippySystem.fishDayCounter = 2`
   (day 3 of the cycle). Open order UI. Verify FISH_SUPPER is shown as greyed out and
   speech log shows "We're out of fish, love." Set `fishDayCounter = 0` (day 1).
   Re-open UI. Verify FISH_SUPPER is available and can be purchased for 4 COIN.

8. **Chippy interior provides warmth**: Set player warmth to 20. Place player inside
   the `CHIPPY` AABB. Advance simulation by 10 real seconds via
   `warmthSystem.update(10f, player, world, weatherSystem)`. Verify player warmth
   is now 50 (20 + 30 from 3 warmth/s × 10 seconds).

---

## Public Library System — Quiet Reading, Free Internet & Rough Sleeping (Issue #928)

**Goal**: Add a Council-run Public Library landmark where the player can read books
to gain street-skill XP, use the free internet terminal to scout fence prices and
check the wanted list, sleep rough in a quiet corner, and avoid police attention —
all while contending with the LIBRARIAN NPC enforcing strict silence rules.

### Overview

The `LibrarySystem` class (in `ragamuffin/core/LibrarySystem.java`) manages the
library's opening hours, reading sessions, internet terminal, rough-sleeping slot,
and the LIBRARIAN NPC's shushing/ejection behaviour.

The Library is a `LandmarkType.LIBRARY` landmark generated as part of the town
centre, adjacent to the park. It opens 09:00–17:30 Monday–Saturday, closed Sunday.

### Core Mechanics

#### Reading Sessions
- The library contains 5 `BOOKSHELF` props. Press **E** on a bookshelf to begin
  reading. A reading session lasts 60 in-game seconds (1 real second).
- Each book belongs to one of four categories mapped to a `StreetSkillSystem.Skill`:
  - **DIY Manual** → `CONSTRUCTION` XP (+15 per session)
  - **Negotiation Tactics** → `TRADING` XP (+15 per session)
  - **Street Law** → `STREETWISE` XP (+15 per session)
  - **Gardening Weekly** → `HORTICULTURE` XP (+15 per session, allotment synergy)
- Player must remain stationary (no WASD input) for the full session or XP is lost.
- Maximum 3 reading sessions per in-game day (books per session shown in UI).

#### Free Internet Terminal
- One `INTERNET_TERMINAL` prop (press **E** to use). Opens a simple text-based UI
  (`LibraryTerminalUI`).
- **Check Fence Prices**: Shows current `FenceValuationTable` prices for the top
  5 most valuable materials in the player's inventory. Tooltip: "Knowledge is power.
  Or at least money."
- **Check Wanted Level**: Displays the player's current notoriety tier and the
  most recent `CriminalRecord` offence log entry.
- **Browse Job Listings**: Shows the current `JobCentreSystem` available jobs list.
  Selecting a job pre-registers the player (skips the queue at JobCentre).
- Terminal session ends if the LIBRARIAN NPC is within 3 blocks (ejected for
  "excessive keyboard noise").

#### Rough Sleeping
- Between 17:30 (closing) and 09:00 (opening) the library is technically closed
  but a broken back window (`GLASS` block replaced with `AIR` at world-gen) lets
  the player sneak in.
- Press **E** on the `READING_CHAIR` prop to sleep. Sleep restores full health
  (+100 HP over 5 in-game seconds) and warmth (+50 warmth). Advances time to 07:00.
- Sleeping inside gives the `ROUGH_SLEEPER` achievement.
- 20% chance per sleep that a POLICE NPC spawns outside at 07:00 and checks the
  premises — player must exit within 30 seconds or receive `TRESPASS` criminal
  record entry and +10 Notoriety.

#### LIBRARIAN NPC
- A `NPCType.LIBRARIAN` NPC (elderly woman, `FacialExpression.STERN`) patrols
  between the bookshelves on a fixed 20-block route.
- **Shushing**: If the player runs (sprint key held) inside the library or breaks
  any block, the LIBRARIAN immediately faces the player and says
  *"Shh! This is a library!"* — player movement speed is reduced by 30% for
  5 seconds (embarrassment debuff).
- **Ejection**: If the player commits a second noise offence within 60 seconds,
  the LIBRARIAN ejects them: player is teleported to the library entrance, the
  door is locked for 10 in-game minutes, and a `LIBRARY_BAN` rumour is seeded
  to nearby NPCs. Tooltip: "Ejected from the library. Absolutely disgraceful."
- **Kindness**: If the player has not committed any offence this session and speaks
  to the LIBRARIAN (press **E**), she offers a free `FLASK_OF_TEA` once per day.
  Tooltip: "The librarian takes pity. 'You look like you need this, dear.'"

### System Integrations

- **`StreetSkillSystem`**: Reading sessions award XP to the appropriate skill tier.
  Tier 1 unlocks require 100 XP; Tier 2 require 300 XP. Library is the cheapest
  way to level `TRADING` and `STREETWISE` skills.
- **`NotorietySystem`**: Police do not enter the library during opening hours unless
  player Notoriety ≥ 60 (armed response threshold). Below that threshold, being
  inside the library provides a "low profile" bonus: Notoriety decays 2× faster
  per in-game minute.
- **`WarmthSystem`**: Library interior counts as a `ShelterDetector` warm zone
  (+3 warmth/s while inside). Particularly valuable during `COLD_SNAP` weather.
- **`TimeSystem`**: Opening hours enforced strictly. Closed Sunday (in-game day 7).
  The broken back window allows night access regardless of day.
- **`JobCentreSystem`**: Pre-registering via the terminal removes the player from
  the queue, granting immediate job pickup next visit (saves 1 in-game hour).
- **`RumourNetwork`**: Reading about an NPC's area of expertise (e.g. Negotiation
  Tactics when `TRADER` NPCs are nearby) seeds a `LOOT_TIP` rumour of new goods
  arriving at the market stall.
- **`NewspaperSystem`**: The library receives the daily newspaper. Press **E** on
  the NEWSPAPER_STAND prop to read it for free (normally costs 1 COIN at the
  corner shop). Reveals today's active `MarketEvent` if one is running.
- **`FactionSystem`**: Reading `Street Law` grants +5 Street Lads Respect (they
  appreciate someone who knows their rights). Ejection for noise offences costs
  −3 Street Lads Respect ("even the lads think that's embarrassing").

### New Materials / Props

- `PropType.BOOKSHELF` — interactable bookshelf; opens reading session.
- `PropType.INTERNET_TERMINAL` — interactable PC terminal; opens `LibraryTerminalUI`.
- `PropType.READING_CHAIR` — interactable chair; initiates rough-sleeping sequence.
- `PropType.NEWSPAPER_STAND` — interactable stand; reveals daily `MarketEvent`.
- `Material.DIY_MANUAL` — readable item; grants `CONSTRUCTION` XP when used from inventory.
- `Material.NEGOTIATION_BOOK` — readable item; grants `TRADING` XP.
- `Material.STREET_LAW_PAMPHLET` — readable item; grants `STREETWISE` XP + Street Lads Respect.

### Achievements

| Achievement | Trigger |
|-------------|---------|
| `BOOKWORM` | Complete 10 reading sessions across sessions |
| `NIGHT_OWL` | Sleep rough in the library 3 times |
| `SELF_IMPROVEMENT` | Gain a StreetSkill Tier 2 level entirely through library reading |
| `SHUSHED` | Be shushed by the librarian |
| `EJECTED_FROM_LIBRARY` | Be ejected for a second noise offence |
| `FLASK_OF_SYMPATHY` | Receive the librarian's free FLASK_OF_TEA |

### Unit Tests

- `LibrarySystem.isOpen(dayOfWeek, hour)` returns true Mon–Sat 09:00–17:29, false otherwise and always false on Sunday.
- Reading session awards +15 XP to the correct `StreetSkillSystem.Skill`.
- Movement interruption during reading session cancels XP award.
- Maximum 3 reading sessions per in-game day; 4th attempt returns `SESSION_LIMIT_REACHED`.
- LIBRARIAN shush debuff reduces speed by 30% for exactly 5 seconds.
- Second noise offence within 60 seconds triggers ejection and 10-minute door lock.
- Rough sleeping advances time to 07:00 and restores full health and warmth.
- 20% trespass check: with `Random` seeded for 20% outcome, POLICE spawns at 07:00.
- `FenceValuationTable` price lookup shown correctly in terminal UI.
- Free newspaper reveals active `MarketEvent`; shows "No disruptions today" if none.
- Notoriety decay rate doubles while player is inside and Notoriety < 60.

### Integration Tests — implement these exact scenarios

1. **Reading session awards XP and respects daily limit**: Place player inside the
   library (inside `LandmarkType.LIBRARY` AABB). Call
   `librarySystem.startReadingSession(player, PropType.BOOKSHELF, StreetSkillSystem.Skill.TRADING,
   streetSkillSystem)`. Advance 60 in-game seconds without WASD input. Verify
   `streetSkillSystem.getXp(Skill.TRADING)` increased by 15. Repeat 2 more times.
   Attempt a 4th session. Verify result is `SESSION_LIMIT_REACHED` and XP has not
   increased further.

2. **LIBRARIAN shushes sprinting player and applies debuff**: Place LIBRARIAN NPC
   inside library. Set player sprint flag true. Call
   `librarySystem.update(delta, player, librarianNpc, world)`. Verify
   `librarianNpc.getSpeechText()` equals `"Shh! This is a library!"`. Verify
   `player.getSpeedMultiplier()` equals 0.7f. Advance 5 seconds. Verify
   `player.getSpeedMultiplier()` returns to 1.0f.

3. **Second noise offence triggers ejection**: Trigger first noise offence (sprint).
   Within 60 seconds, trigger a second (break a block via
   `librarySystem.onBlockBroken(player, librarianNpc, world, rumourNetwork, npcManager)`).
   Verify player position equals library entrance coordinates. Verify
   `librarySystem.isDoorLocked()` is true. Verify a `LIBRARY_BAN` rumour exists in
   the `RumourNetwork`.

4. **Rough sleeping restores health and warmth and advances time**: Set player HP to
   30 and warmth to 15. Set time to 22:00. Call
   `librarySystem.sleepRough(player, timeSystem, warmthSystem, achievementSystem)`.
   Verify `timeSystem.getHour()` equals 7. Verify `player.getHealth()` equals
   `player.getMaxHealth()`. Verify `player.getWarmth()` equals 65 (15 + 50). Verify
   `ROUGH_SLEEPER` achievement was awarded (first sleep).

5. **Internet terminal reveals fence prices and wanted level**: Give player
   `Material.DIAMOND` (fence value 50) and `Material.BRICK` (fence value 2).
   Call `librarySystem.openTerminal(player, fenceValuationTable, criminalRecord,
   jobCentreSystem)`. Query `terminal.getFencePriceList()`. Verify DIAMOND appears
   first (highest value). Verify `terminal.getWantedLevel()` matches
   `notorietySystem.getTier()`. Close terminal. Verify player's inventory unchanged.

6. **Notoriety decays faster inside library**: Set player Notoriety to 40 (below
   armed response threshold). Call `librarySystem.update(delta=60f, ...)` with player
   inside the library AABB. Verify `notorietySystem.getNotoriety()` decreased by at
   least 2 points (double the normal 1-per-minute decay rate).

7. **Free newspaper reveals active MarketEvent**: Trigger `MarketEvent.GREGGS_STRIKE`
   via `streetEconomySystem.triggerMarketEvent(...)`. Place player inside library.
   Call `librarySystem.readNewspaper(player, streetEconomySystem, tooltipSystem)`.
   Verify `tooltipSystem.getLastTooltip()` contains `"Greggs"`. Verify no COIN was
   deducted from player inventory.

---

## Charity Shop System — Haggling, Mystery Bags & Community Service (Issue #930)

**Goal**: Transform the existing `LandmarkType.CHARITY_SHOP` from a passive CARDBOARD-
dropping block into a fully interactive economy hub with a `VOLUNTEER` NPC, price
haggling, mystery bag gambling, item donation for notoriety reduction, and rare
hidden-treasure finds. The charity shop should feel like a treasure hunt with a side
order of judgement from a retired schoolteacher.

### Overview

The `CharityShopSystem` class (in `ragamuffin/core/CharityShopSystem.java`) manages
the shop's rotating stock, haggling interactions with the `VOLUNTEER` NPC, donation
mechanics, and mystery bag loot. It integrates with the existing `NotorietySystem`,
`StreetEconomySystem`, `WarmthSystem`, `RumourNetwork`, and `AchievementSystem`.

The shop opens **10:00–16:00 Monday–Saturday** (closed Sunday and bank holidays,
because of course it is). A `NPCType.VOLUNTEER` NPC (elderly woman, sensible cardigan,
distinct speech lines) stands behind the counter.

### Core Mechanics

#### Rotating Stock

The charity shop holds a rotating stock of **6 items**, re-rolled each in-game day at
opening time from the following pool:

| Item | Base Price (COIN) | Category |
|------|-------------------|----------|
| `COAT` | 4 | Clothing |
| `WOOLLY_HAT` | 2 | Clothing |
| `UMBRELLA` | 3 | Clothing |
| `TEXTBOOK` | 2 | Books |
| `HYMN_BOOK` | 1 | Books |
| `DODGY_DVD` | 1 | Media |
| `BROKEN_PHONE` | 2 | Electronics |
| `WASHING_POWDER` | 2 | Household |
| `FIRE_EXTINGUISHER` | 3 | Household |
| `SAUSAGE_ROLL` | 1 | Food |
| `SLEEPING_BAG` | 5 | Survival |
| `FLASK_OF_TEA` | 2 | Survival |
| `NEWSPAPER` | 1 | Media |

The clothing items (`COAT`, `WOOLLY_HAT`, `UMBRELLA`) must always be represented in
the stock if available in the pool. Other slots are randomly filled.

Stock is tracked in `CharityShopSystem.todayStock` (List<Material>) and
`todayStockPrices` (List<Integer>). Buying an item removes it from stock for the day.

#### Haggling

Pressing **E** on the `VOLUNTEER` NPC while facing a stocked item opens a simple
text-based trade prompt. The player can:

1. **Buy at listed price** — straightforward transaction.
2. **Haggle** — offer 1 COIN less than the listed price.
   - If listed price is 1 (already minimum), haggling is refused: "This money goes to
     charity, dear. Don't be tight."
   - At listed price 2+: VOLUNTEER accepts the haggle with 40% probability (base).
     Acceptance rate improves if player Notoriety Tier ≤ 1 (+20%) or if player has
     donated ≥ 3 items to this shop in the current session (+15%).
   - On rejection: VOLUNTEER says "I know it's a charity shop, but we still have
     bills to pay." The item stays available.
   - On acceptance: VOLUNTEER says one of three randomised charitable remarks.
     Tooltip first time: "Even the charity shop takes pity. Eventually."

Haggling for a `COAT` or `WOOLLY_HAT` during `Weather.COLD_SNAP` or `Weather.FROST`
always succeeds (VOLUNTEER takes pity on the cold): "You look frozen, take it for the
lower price." Tooltip: "British charity: reluctant, but ultimately decent."

#### Mystery Bags

A special **"Mystery Bag"** interaction (press **E** on the shop's MYSTERY_BAG prop,
a new `PropType.MYSTERY_BAG` placed on the counter at world-gen) costs **2 COIN** and
yields a random item from a weighted loot table:

| Item | Weight | Notes |
|------|--------|-------|
| `CARDBOARD` | 30 | Bulk filler |
| `CRISPS` | 20 | |
| `NEWSPAPER` | 15 | |
| `BROKEN_PHONE` | 12 | Fence value 5 |
| `DODGY_DVD` | 8 | Fence value 3 |
| `WOOLLY_HAT` | 5 | |
| `COAT` | 3 | Jackpot clothing |
| `DIAMOND` | 1 | Hidden treasure find |

On receiving `DIAMOND`, the tooltip fires: "Someone donated a diamond. To a charity
shop. This town is something else." Achievement `CHARITY_SHOP_DIAMOND` is awarded.
On receiving `CARDBOARD`, the tooltip fires: "A charity bag full of cardboard. The
circle of rubbish is complete."

Maximum 3 mystery bag purchases per in-game day (the VOLUNTEER notices).

#### Donations

The player can donate any item from their inventory by pressing **E** on the VOLUNTEER
and selecting "Donate item" from the interaction menu, then choosing an item from
their hotbar.

**Donation effects:**
- Any donation: NPC speech "That's very generous, love." Notoriety −1 (community
  goodwill). Adds 1 to `sessionDonationCount`.
- Donating `COAT`, `WOOLLY_HAT`, or `UMBRELLA`: Notoriety −3 (visible charitable
  act). Seeded as a `RumourType.LOOT_TIP` rumour: "Someone left a [item] at the
  charity shop. Proper kind."
- Donating `DIAMOND`: Achievement `DIAMOND_DONOR` ("You gave away a diamond to
  a charity shop. You absolute saint. Or complete mug."). Notoriety −10.
- Donating 5+ items in one session: Achievement `COMMUNITY_SERVICE`.
  VOLUNTEER speech: "You've been very busy today, dear."
- Donated items are removed from the player's inventory and added to the shop's
  `todayStock` (up to the stock cap of 6), priced at half their `FenceValuationTable`
  value (minimum 1 COIN). This makes the shop genuinely dynamic — you can donate
  your junk and watch another player (or NPC customer) buy it.

#### NPC Customers

Two `NPCType.PUBLIC` or `NPCType.PENSIONER` customers browse the shop during opening
hours, spawned by the `CharityShopSystem` and despawned at close. Customers:
- Walk slowly between the stock display positions.
- Have a 5% chance per in-game minute to purchase the cheapest item and leave.
- If the player has donated an item this session, customers have a +10% purchase
  chance for that item (word travels fast in a small charity shop).

#### VOLUNTEER NPC Behaviour

The `VOLUNTEER` NPC has distinct speech lines and reactions:

- **Idle patrol**: Patrols between the counter and a back-room door on a 6-block
  route. Speech on idle: "Such a lovely selection today." / "Such a shame what
  people throw away."
- **Interaction**: Greets player on first approach each visit: "Hello dear, can I
  help you?" / "We've just had a new donation in."
- **Player with Notoriety Tier 3+**: VOLUNTEER is visibly nervous but still serves:
  "I... yes. Can I help? Quickly?" She reduces mystery bag purchases allowed to 1
  for this visit.
- **Player with active BALACLAVA**: VOLUNTEER refuses service entirely: "I'm sorry,
  I don't serve people dressed like that. This is a charity shop, not a balaclava
  convention."

### System Integrations

- **`WarmthSystem`**: Clothing purchased from the charity shop has the same warmth
  effect as found clothing. Being inside the charity shop counts as sheltered
  (+2 warmth/s while browsing).
- **`NotorietySystem`**: Donations reduce Notoriety. Buying at full price with
  Notoriety Tier 4+ earns a tooltip: "The charity shop doesn't judge. Almost no one
  does, in fact."
- **`StreetEconomySystem`**: COAT and WOOLLY_HAT purchased here satisfy COLD NeedType
  for NPC deals (same as other sources). Mystery Bag DIAMOND satisfies BROKE NeedType.
- **`RumourNetwork`**: Donations of valuable items seed `LOOT_TIP` rumours. Buying
  a DIAMOND from the mystery bag seeds a `PLAYER_SPOTTED` rumour: "Someone scored big
  at the charity shop. Jammy sod."
- **`FenceSystem`**: `BROKEN_PHONE` and `DODGY_DVD` purchased here can be sold to
  the fence at standard rates. VOLUNTEER doesn't ask questions.
- **`WeatherSystem`**: Clothing purchase during COLD_SNAP triggers the compassionate
  haggle path regardless of Notoriety.
- **`AchievementSystem`**: 4 new achievements (see below).
- **`NewspaperSystem`**: A donation of 5+ items in one day generates a local newspaper
  story: "Local Good Samaritan Donates to Hearts & Minds." Player's Street Reputation
  +2.

### New Types

- `NPCType.VOLUNTEER` — charity shop volunteer. Non-hostile. Counter-anchored with
  short patrol. Sensible-shoes energy.
- `PropType.MYSTERY_BAG` — interactable prop on the counter.

### Achievements

| Achievement | Trigger |
|-------------|---------|
| `CHARITY_SHOP_DIAMOND` | Pull a DIAMOND from a mystery bag |
| `DIAMOND_DONOR` | Donate a DIAMOND to the charity shop |
| `COMMUNITY_SERVICE` | Donate 5+ items in one session |
| `TIGHT_FISTED` | Successfully haggle at the charity shop 3 times in one session |

### Unit Tests

- `CharityShopSystem.rollDailyStock(random)` always includes at least one clothing
  item when the pool contains clothing items.
- Haggle acceptance probability is 40% base, +20% at Notoriety ≤ 1, +15% with 3+
  session donations — additive, capped at 100%.
- Cold-snap haggle for COAT/WOOLLY_HAT always returns `HaggleResult.ACCEPTED`.
- Mystery bag loot table weights sum to 94 (sanity check).
- DIAMOND mystery bag outcome awards `CHARITY_SHOP_DIAMOND` achievement.
- Donation of COAT reduces Notoriety by 3; donation of CARDBOARD by 1.
- Donating 5 items awards `COMMUNITY_SERVICE` achievement.
- VOLUNTEER refuses service to BALACLAVA-wearing player.
- Daily mystery bag purchase limit (3) is enforced.
- Donated item is added to todayStock (if under cap) at half fence value, min 1.

### Integration Tests — implement these exact scenarios

1. **Buy a COAT reduces warmth drain during COLD_SNAP**: Set `Weather.COLD_SNAP`.
   Generate world, find `LandmarkType.CHARITY_SHOP`. Add `Material.COAT` to
   `charityShopSystem.todayStock` at price 4. Give player 4 COIN. Press E on
   VOLUNTEER, select COAT at full price. Verify COAT is in player inventory. Verify
   `charityShopSystem.todayStock` no longer contains COAT. Call
   `warmthSystem.update(60f, player, world, weather, false, inventory)`. Verify
   player warmth drained less than without COAT (i.e. drain rate ≤ 50% of base rate).

2. **Haggle succeeds during COLD_SNAP for clothing**: Set `Weather.COLD_SNAP`.
   Add `Material.WOOLLY_HAT` to stock at price 2. Give player 1 COIN. Call
   `charityShopSystem.attemptHaggle(player, Material.WOOLLY_HAT, inventory,
   notorietySystem, weather)`. Verify result is `HaggleResult.ACCEPTED`. Verify
   player has 0 COIN and 1 WOOLLY_HAT. Verify VOLUNTEER speech contains "frozen".

3. **Mystery bag yields weighted loot and respects daily limit**: Seed RNG with a
   known value that produces `CARDBOARD`. Give player 6 COIN. Call
   `charityShopSystem.buyMysteryBag(player, inventory, random, tooltipSystem,
   achievementSystem)` three times. Verify player received 3 items and spent 6 COIN.
   Verify `tooltipSystem.getLastTooltip()` contains "cardboard" on a CARDBOARD result.
   Attempt a 4th purchase. Verify result is `MysteryBagResult.DAILY_LIMIT_REACHED`
   and player COIN is unchanged.

4. **DIAMOND mystery bag awards achievement**: Seed RNG to force DIAMOND outcome.
   Give player 2 COIN. Call `charityShopSystem.buyMysteryBag(...)`. Verify player
   has 1 DIAMOND. Verify `achievementSystem.isUnlocked(AchievementType.CHARITY_SHOP_DIAMOND)`
   is true. Verify `tooltipSystem.getLastTooltip()` contains "diamond".

5. **Donating COAT reduces Notoriety and seeds rumour**: Set player Notoriety to 20.
   Give player 1 COAT. Call `charityShopSystem.donateItem(player, Material.COAT,
   inventory, notorietySystem, rumourNetwork)`. Verify player has 0 COAT. Verify
   `notorietySystem.getNotoriety()` equals 17 (−3). Verify `rumourNetwork`
   contains a `RumourType.LOOT_TIP` rumour mentioning "coat".

6. **Donating 5 items awards COMMUNITY_SERVICE achievement**: Give player 5
   different items (e.g. NEWSPAPER×1, CRISPS×1, BROKEN_PHONE×1, DODGY_DVD×1,
   CARDBOARD×1). Donate each via `charityShopSystem.donateItem(...)`. Verify
   `achievementSystem.isUnlocked(AchievementType.COMMUNITY_SERVICE)` is true after
   the 5th donation. Verify `charityShopSystem.getSessionDonationCount()` equals 5.

7. **BALACLAVA refusal**: Give player `Material.BALACLAVA` in inventory. Call
   `charityShopSystem.onPlayerInteract(player, inventory, volunteerNpc)`. Verify
   return value is a non-null refusal string. Verify `volunteerNpc.getSpeechText()`
   contains "balaclava". Verify no trade UI opens (`charityShopSystem.isTradeUIOpen()`
   is false).

8. **Donated item appears in shop stock**: Give player 1 COAT. Stock is currently
   empty. Call `charityShopSystem.donateItem(...)`. Verify `todayStock` contains
   COAT. Verify its price equals `max(1, fenceValuationTable.getValueFor(COAT) / 2)`.
   Give player 2 COIN and buy the COAT back. Verify player has 1 COAT and stock is
   empty again.

---

## Ice Cream Van System — Mister Softee, Jingle Wars & Heatwave Hustle

### Overview

A roaming `ICE_CREAM_VAN` NPC vehicle (driven by `NPCType.ICE_CREAM_MAN`) circles
the streets on a fixed 200-block loop, playing a tinny jingle every 90 in-game
seconds. The player can buy ice creams, steal the van, operate it as a mobile shop,
and trigger a rival van turf war.

### Ice Cream Van Behaviour

- **Route**: The van follows a deterministic loop around the park and high street,
  pausing for 20 in-game seconds at each of 4 designated `STOP_POINT` positions.
- **Jingle**: Plays `SoundEffect.ICE_CREAM_JINGLE` at the start of each stop. NPCs
  within 15 blocks enter a `WANDERING_TOWARDS` state and queue at the van (up to
  6 NPCs per stop).
- **Weather gating**: The van only runs when `Weather` is `CLEAR`, `OVERCAST`, or
  `HEATWAVE`. In `RAIN`, `DRIZZLE`, `THUNDERSTORM`, `FOG`, `COLD_SNAP`, or `FROST`
  the van parks up at the `ICE_CREAM_DEPOT` landmark and the `ICE_CREAM_MAN` NPC
  idles with speech: "Not worth it in this weather, is it love."
- **Heatwave bonus**: In `HEATWAVE` the queue cap increases to 10 NPCs and the van
  stops for 30 seconds instead of 20.

### Menu & Prices

| Item | Cost (COIN) | Notes |
|------|------------|-------|
| `99_FLAKE` | 2 | Satisfies HUNGRY −20, COLD need unaffected |
| `SCREWBALL` | 1 | Satisfies HUNGRY −10; random BUBBLEGUM at bottom (30% chance) |
| `FAB_LOLLY` | 2 | Satisfies HUNGRY −15; interaction: "You eat the strawberry bit first, don't you." |
| `CHOC_ICE` | 3 | Satisfies HUNGRY −25; grants `SWEET_TOOTH` buff: +5 energy for 60s |
| `OYSTER_CARD_LOLLY` | 5 | Rare item; grants a free bus ride on the Number 47 (single use) |

The `ICE_CREAM_MAN` sells to NPCs at 1 COIN/item (their HUNGRY need must be ≥ 30).
During `HEATWAVE`, all prices increase by 1 COIN: "Costs more when it's hot, dunnit."

### Player Interactions

**Buying**

- Press `E` near the van hatch while `ICE_CREAM_MAN` is serving. Opens a 5-item
  trade menu. Payment deducted on confirm.
- First purchase triggers tooltip: "Nothing says British summer like overpriced ice
  cream from a van that might fail its MOT."

**Stealing the Van**

- Player must have Notoriety ≥ 150 (Tier 2) and `CROWBAR` in inventory.
- Press `E` on the cab door. `ICE_CREAM_MAN` flees. Van enters `PLAYER_DRIVEN` mode
  (uses `CarDrivingSystem`).
- Player can sell ice cream from the van: park near any NPC cluster (5+ NPCs within
  10 blocks), press `E` to open hatch. Jingle plays. NPCs queue as normal.
- Each sale earns 2 COIN (regardless of item). Van holds 20 units of random stock.
- After 5 player sales, `WantedSystem` adds 1 star: "Ice cream theft. That's a new
  one." Police spawn within 60 seconds.
- Rival `DODGY_VAN_MAN` NPC arrives after 10 player sales to "negotiate" (i.e.
  attack player to reclaim territory).

**Jingle Wars**

- A second rival van (`DODGY_ICE_CREAM_VAN`) spawns when the player operates the
  stolen van for 2+ in-game days. It plays a louder jingle (100% volume vs 70%)
  and undercuts all prices by 1 COIN.
- Player can end Jingle Wars by: (a) abandoning the stolen van, (b) paying the
  `DODGY_VAN_MAN` 15 COIN as a truce, or (c) destroying the rival van (crowbar,
  8 hits, notoriety +30).
- If the rival van is destroyed: achievement `KING_OF_THE_ROAD` unlocked. Nearby
  NPCs react: "They've only gone and smashed up Mister Frosty's van!"

### `NeedType` Integration

- `ICE_CREAM_VAN` present within 20 blocks reduces NPC `HUNGRY` accumulation rate
  by 10% while the jingle is playing (they are distracted by the possibility of
  ice cream).
- In `HEATWAVE`, the van presence reduces NPC `BORED` need rate by 15%.
- During `COLD_SNAP`/`FROST`: no van present. NPCs' COLD need accumulates faster
  (+5%) due to the absence of warm comfort food (the van's absence is noted).

### `NeighbourhoodSystem` Integration

- Van operating in `THRIVING` or `NORMAL` vibes areas: +2 Vibes per stop completed
  (it's a wholesome community presence).
- Van stolen by player: −5 Vibes (community trust eroded).
- Rival van war active: −3 Vibes/in-game day (noise complaints).

### `WeatherSystem` Integration

- `HEATWAVE` triggers a `MarketEvent`-style boost: `ICE_CREAM_FRENZY` — NPC HUNGRY
  rates +1.5× for that day (everyone wants one), van sales 2× more frequent.
- `COLD_SNAP`: van parks up AND the player receives a tooltip the first time they
  check: "The ice cream van's gone into hibernation. Probably sensible."

### `BusSystem` Integration

- `OYSTER_CARD_LOLLY` grants a free ride on the Number 47. When used at a bus stop,
  `BusSystem.redeemFreeLollyRide(player)` is called. The bus driver says: "That's...
  unusual. Alright then."

### New NPC Types

- `NPCType.ICE_CREAM_MAN` — van operator. Flees if notoriety ≥ 150 or player
  approaches with CROWBAR in hand. Speech: "What's your fancy?" / "99 Flake? Can't
  go wrong." / "You're not gonna nick it are ya?"
- `NPCType.DODGY_VAN_MAN` — rival van operator. Hostile if player has stolen van.
  Speech: "Oi, that's MY patch!" / "You're cutting into my margins, mate."

### New Items (Material enum additions)

| Constant | Description |
|----------|-------------|
| `Material.ICE_CREAM_99` | 99 Flake cone (alias `99_FLAKE`) |
| `Material.SCREWBALL` | Screwball lolly with bubblegum |
| `Material.FAB_LOLLY` | Fab lolly (strawberry/chocolate/hundreds-and-thousands) |
| `Material.CHOC_ICE` | Chocolate-coated ice cream bar |
| `Material.OYSTER_CARD_LOLLY` | Blue lolly shaped like an Oyster card |

### New Achievement

| Achievement | Trigger |
|-------------|---------|
| `KING_OF_THE_ROAD` | Destroy the rival ice cream van during a Jingle War |
| `MISTER_FROSTY` | Sell ice cream from the stolen van 10 times in one session |
| `CHOC_ICE_COLD` | Buy a CHOC_ICE during a COLD_SNAP weather event |
| `FREE_RIDE` | Use an OYSTER_CARD_LOLLY to board the Number 47 |

### Unit Tests

- `IceCreamVanSystem.getMenuPrice(Material.ICE_CREAM_99, Weather.CLEAR)` returns 2.
- `IceCreamVanSystem.getMenuPrice(Material.ICE_CREAM_99, Weather.HEATWAVE)` returns 3.
- `IceCreamVanSystem.isOperating(Weather.RAIN)` returns `false`.
- `IceCreamVanSystem.isOperating(Weather.HEATWAVE)` returns `true`.
- `IceCreamVanSystem.getQueueCap(Weather.HEATWAVE)` returns 10; all other operating
  weathers return 6.
- Buying a SCREWBALL has a 30% chance of adding `BUBBLEGUM` to player inventory.
- Stealing the van requires both Notoriety ≥ 150 AND `CROWBAR` in inventory; missing
  either returns `VanStealResult.PRECONDITION_NOT_MET`.
- Player sales counter increments correctly; at 5 sales `WantedSystem.addWantedLevel(1)`
  is called exactly once.
- `OYSTER_CARD_LOLLY` can only be redeemed at a bus stop; calling redeem elsewhere
  returns `LollyRedeemResult.NOT_AT_STOP`.
- Vibes reduced by 5 when player steals van; vibes increased by 2 per completed stop
  under normal operation.

### Integration Tests — implement these exact scenarios

1. **Van arrives, jingle plays, NPCs queue**: Generate world with `ICE_CREAM_DEPOT`
   landmark. Tick `IceCreamVanSystem` until van reaches its first stop. Verify
   `SoundSystem.getLastSoundEffect()` equals `SoundEffect.ICE_CREAM_JINGLE`. Spawn
   8 NPCs within 15 blocks, all with `HUNGRY` ≥ 30. Tick for 10 seconds. Verify at
   least 6 NPCs have state `WANDERING_TOWARDS` targeting the van position.

2. **Van parks in bad weather**: Set `Weather.RAIN`. Call
   `iceCreamVanSystem.update(delta, weather, world)`. Verify
   `iceCreamVanSystem.isOperating()` is `false`. Verify van position matches
   `ICE_CREAM_DEPOT` landmark position. Verify `ICE_CREAM_MAN` NPC speech contains
   "weather".

3. **Heatwave prices increase**: Set `Weather.HEATWAVE`. Press E on van hatch.
   Verify the displayed price for `99_FLAKE` is 3 COIN (base 2 + 1 heatwave).
   Give player 3 COIN. Buy `99_FLAKE`. Verify player has 0 COIN and 1 `ICE_CREAM_99`
   in inventory. Verify player `HUNGRY` need decreased by 20.

4. **Stealing the van triggers wanted level**: Set player Notoriety to 200. Give
   player `Material.CROWBAR`. Call `iceCreamVanSystem.stealVan(player, inventory,
   notorietySystem, wantedSystem)`. Verify `ICE_CREAM_MAN` NPC state is `FLEEING`.
   Simulate 5 player sales from van. Verify `wantedSystem.getWantedLevel()` is 1.
   Verify tooltip contains "ice cream theft".

5. **OYSTER_CARD_LOLLY gives free bus ride**: Give player 5 COIN. Buy
   `OYSTER_CARD_LOLLY` from van (costs 5). Place player at bus stop. Call
   `iceCreamVanSystem.redeemOysterLolly(player, busSystem, world)`. Verify
   `busSystem.hasFreeRide(player)` is `true`. Board the Number 47. Verify
   `busSystem.hasFreeRide(player)` is `false` after boarding.

---

## Pigeon Racing System — The Loft, Homing Races & Neighbourhood Flutter

### Overview

A `PigeonRacingSystem` that lets the player own, train, and race homing pigeons —
a proud British working-class tradition. The player builds a `PIGEON_LOFT` prop
(crafted from 8 WOOD + 2 PLANKS) on their squat or allotment, acquires a pigeon
(`Material.RACING_PIGEON`), and enters it in weekly neighbourhood races organised
by `NPCType.PIGEON_FANCIER`. The races are resolved by simulation: pigeon speed
is derived from training level, weather, and a seeded RNG. Other residents bet on
outcomes through the existing `BettingUI` extended with a "Pigeon Racing" tab.

### The Pigeon Loft

- Crafted: 8 WOOD + 2 PLANKS → `PropType.PIGEON_LOFT` (placed as a 2×1×1 prop).
- Only one loft can be active at a time. The loft must be placed on the player's
  squat or allotment land (checked via `SquatSystem` / `AllotmentSystem`).
- Press **E** on the loft to open the loft management UI:
  - **Slot**: holds 1 pigeon (expandable to 3 via `PIGEON_LOFT_EXTENSION` craft:
    4 WOOD, costs 12 coins from builders merchant).
  - Shows pigeon name, training level (0–10), race record (wins/total), morale.
- Pigeon health degrades in `COLD_SNAP` / `FROST` weather if the loft has no roof
  block above it (solid block within 2 tiles overhead).

### Acquiring a Pigeon

Three ways to get a `Material.RACING_PIGEON`:
1. **Buy**: Press **E** on `NPCType.PIGEON_FANCIER` NPC (spawns near the park or
   allotments). Costs 5 COIN. Speech: "She's a good bird, son. Don't let her down."
2. **Catch**: Crouch and hold **E** near a park `BIRD` NPC for 3 seconds (replaces
   it with `RACING_PIGEON` item, 40% success, 60% the bird flies away).
   Tooltip on first catch attempt: "It's harder than it looks."
3. **Gift**: After completing a `NeighbourhoodSystem` community event, a
   `PIGEON_FANCIER` may gift one as a reward.

Each pigeon has a procedurally generated name (e.g. "Northfield Blue", "Big Dave",
"Biscuit Jr.") stored on the `Material.RACING_PIGEON` item's metadata string.

### Training

- Press **E** on the loft → "Train" → consumes 1 `BREAD_CRUST` (new item, crafted
  from 1 GREGGS_PASTRY or found in bins) and advances training level by 1.
- Training level caps at 10. Each level adds `+0.5` speed units to the pigeon's
  base speed of `5.0`.
- Over-training: training once per in-game day. Attempting to train more than once
  per day has no effect and shows speech: "She needs her rest, mate."
- Morale: starts at 100. Drops by 5 for each missed race (pigeon not entered but
  a race occurred). Below 50 morale: speed penalty −1.0. Restored by training or
  feeding `SUNFLOWER` (from allotment, +20 morale).

### Race Schedule

- One race per in-game week (every 7 game-days), triggered at 10:00 game-time.
- The `PIGEON_FANCIER` NPC announces upcoming races via a `RumourType.LOOT_TIP`
  rumour: "Race day Saturday. Get your bird entered by Friday night."
- Entry window: press **E** on `PIGEON_FANCIER` before 18:00 the day before the
  race. Entry fee: 2 COIN (added to the prize pool).
- Up to 6 pigeons race (5 NPC-owned + player's if entered). NPC pigeon speed is
  seeded from `(3.0 + random * 4.0)` — player's trained bird can outpace them.

### Race Resolution

`PigeonRacingSystem.resolveRace(List<PigeonEntry> entries, Weather weather, Random rng)`:

- Race distance: 100 simulated "furlongs" (abstract units).
- Each pigeon's finish time = `distance / (speed * weatherModifier * random(0.9, 1.1))`.
- Weather modifiers:
  - `CLEAR`, `OVERCAST`: 1.0×
  - `DRIZZLE`, `FOG`: 0.9× (slower — pigeons navigate poorly in low visibility)
  - `RAIN`, `THUNDERSTORM`: 0.75× (race is called off if active at race time;
    postponed 1 game-day, notification seeded as rumour)
  - `HEATWAVE`: 1.1× (pigeons love warm lift; all birds faster)
  - `FROST`, `COLD_SNAP`: 0.8×
- Finishing order determines prize distribution:
  - 1st: 70% of prize pool
  - 2nd: 20% of prize pool
  - 3rd: 10% of prize pool
- Player pigeon result stored: win/loss added to race record.

### Betting

- Betting opens when the entry window closes (from 18:00 day-before to 10:00
  race day). Extended `BettingUI` shows a "Pigeon Racing" tab alongside horse
  racing.
- Odds derived from training levels of entered pigeons (higher training = lower
  odds). Player's pigeon odds displayed as e.g. `3/1`.
- Max stake: 20 COIN (or 40 on `BENEFIT_DAY`). Any NPC with BORED need ≥ 40 has
  a 20% chance per race of placing an NPC bet (purely flavour, no economic impact).
- Win payout = stake × odds numerator. `BET_SLIP` added to inventory on bet;
  consumed on resolution.

### NPC Reactions

- After race resolution, `PIGEON_FANCIER` announces result: "Northfield Blue wins
  it at 4/1! What a bird!" / "Terrible result. She went the wrong way."
- PUBLIC NPCs within 10 blocks comment: "Did your pigeon win?" / "I lost a quid
  on that race."
- `NewspaperSystem`: a 1st-place win seeds a `PIGEON_VICTORY` story headline
  ("LOCAL BIRD TRIUMPHS IN NORTHFIELD DERBY") in the next evening edition if
  the infamy score is 0 that day (replaces the normal pigeon-filler).

### New Items (`Material` enum additions)

| Constant | Description |
|----------|-------------|
| `RACING_PIGEON` | A homing pigeon (named). Placed in loft; consumed in race entry. |
| `BREAD_CRUST` | Pigeon training feed. Crafted from 1 GREGGS_PASTRY or found in bins. |
| `PIGEON_TROPHY` | Awarded on first race win. Decorative; can be placed in squat. |

### New Prop / NPC Types

- `PropType.PIGEON_LOFT` — 2×1×1 wooden loft prop. Occupies one allotment/squat tile.
- `NPCType.PIGEON_FANCIER` — Elderly male NPC, spawns near allotments or park.
  Passive. Manages race registration and broadcasts results. Speech pool includes
  regional pigeon-racing terminology.

### `WeatherSystem` Integration

- `THUNDERSTORM` / `RAIN` at race time: race postponed 1 day.
  `TimeSystem` reschedules next race tick. Rumour seeded: "Race off — too wet
  for the birds."
- `HEATWAVE`: bonus speed multiplier 1.1×. `PIGEON_FANCIER` speech: "Lovely
  thermals today. Good for the birds."
- `FROST`: pigeon health −5 if loft has no overhead shelter.

### `NeighbourhoodSystem` Integration

- Winning a race: +3 Vibes (wholesome community event).
- First pigeon loft placed: +2 Vibes ("someone's keeping pigeons again").
- Catching a wild park pigeon: −1 Vibes ("oi, leave the birds alone").

### `StreetEconomySystem` Integration

- `BREAD_CRUST` base price: 1 COIN. Added to `BASE_PRICES`.
- `RACING_PIGEON` base price: 5 COIN (sold by PIGEON_FANCIER).
- NPC BORED need satisfied by "watching the pigeon race" — attending the race
  area (within 15 blocks of `PIGEON_FANCIER` at race time) reduces BORED by 20.

### `RumourNetwork` Integration

- Race day reminder seeded on day-before evening by `PIGEON_FANCIER`.
- Race postponement seeded as `RumourType.LOOT_TIP`.
- Player win seeded as `RumourType.GANG_ACTIVITY` (flavour: neighbourhood gossip).

### Achievements

| Achievement | Trigger |
|-------------|---------|
| `HOME_BIRD` | Win your first pigeon race |
| `CHAMPION_OF_THE_LOFT` | Win 3 races with the same pigeon |
| `NORTHFIELD_DERBY` | Enter and place 1st in 5 consecutive races |
| `CAUGHT_IT_YERSELF` | Successfully catch a wild park pigeon (40% chance) |
| `BREAD_WINNER` | Train a pigeon to level 10 |

### Unit Tests

- `PigeonRacingSystem.resolveRace()` with 6 entries, seeded RNG returns consistent
  finishing order.
- `PigeonRacingSystem.getWeatherModifier(Weather.RAIN)` returns `0.75f`.
- `PigeonRacingSystem.getWeatherModifier(Weather.HEATWAVE)` returns `1.1f`.
- `PigeonRacingSystem.isRacePostponed(Weather.THUNDERSTORM)` returns `true`;
  `isRacePostponed(Weather.OVERCAST)` returns `false`.
- Training a pigeon twice in one in-game day only increases level by 1 (second
  training call returns `TrainResult.ALREADY_TRAINED_TODAY`).
- Prize pool distributed correctly: 70%/20%/10% rounded to int, minimum 1 COIN.
- Morale drops by 5 per missed race; speed penalty applied when morale < 50.
- `catchWildPigeon(float probability, Random rng)`: with seeded rng that returns
  `0.3f`, returns `CatchResult.SUCCESS`; with `0.7f`, returns `CatchResult.FLED`.

### Integration Tests — implement these exact scenarios

1. **Full race cycle — player wins**: Build a `PIGEON_LOFT`. Acquire a
   `RACING_PIGEON` from `PIGEON_FANCIER`. Train it to level 5. Press **E** on
   `PIGEON_FANCIER` the day before the race to enter (pay 2 COIN entry fee).
   Set weather to `CLEAR`. Set all NPC pigeons' seeded speed to 4.0 (below player's
   trained speed of 7.5). Advance `TimeSystem` to race time (10:00). Call
   `pigeonRacingSystem.resolveRace(...)`. Verify player pigeon finishes 1st. Verify
   player receives 70% of the prize pool in COIN. Verify `AchievementType.HOME_BIRD`
   is awarded. Verify `NewspaperSystem` has a pending `PIGEON_VICTORY` story for the
   evening edition.

2. **Race postponed in thunderstorm**: Schedule a race for in-game day 7. At
   09:55 race-day, set weather to `THUNDERSTORM`. Call `pigeonRacingSystem.update(...)`.
   Verify `pigeonRacingSystem.isRacePostponed()` is `true`. Verify a `LOOT_TIP`
   rumour containing "wet" or "weather" has been seeded. Verify the next race tick
   is set to day 8 at 10:00. Set weather to `CLEAR`. Advance to day 8, 10:00.
   Verify race resolves normally.

3. **Pigeon morale penalty applied**: Give player a `RACING_PIGEON` with training
   level 5 (base speed 7.5). Miss 10 consecutive races (advance 10 game-weeks
   without entering). Verify morale ≤ 50. Verify effective speed is `7.5 - 1.0 =
   6.5` when `PigeonRacingSystem.getEffectiveSpeed(pigeon)` is called. Feed pigeon
   1 `SUNFLOWER` via loft UI. Verify morale increases by 20.

4. **Betting pays out on win**: Open `BettingUI` pigeon racing tab. Place a 10
   COIN bet on the player's pigeon at 3/1 odds. Advance to race time. Ensure player
   pigeon wins (force via seeded RNG). Verify player receives 30 COIN payout (10 ×
   3). Verify `BET_SLIP` is removed from inventory. Verify `BettingUI` shows the
   win message.

5. **Catching a wild pigeon**: Place player adjacent to a `BIRD` NPC in the park.
   Crouch and hold **E** for 3 seconds (simulate 180 frames at 60fps). Use a seeded
   RNG that returns `0.25f` (below 0.4 threshold). Verify `BIRD` NPC is removed from
   the world. Verify player inventory contains 1 `RACING_PIGEON` item. Verify tooltip
   "It's harder than it looks." has been triggered. Now use a seeded RNG returning
   `0.65f` and repeat. Verify `BIRD` NPC remains in world. Verify inventory unchanged.

---

## Add Council Skip & Bulky Item Day — Skip Diving, Furniture Rescue & Neighbour Rivalry

**Goal**: Every 3 in-game days, the council schedules a Bulky Item Collection Day.
Residents drag their old furniture, appliances, and junk onto the pavement and a large
skip prop appears at the end of the street. The player has a 2-hour window (08:00–10:00
game-time) to pick through the skip and kerbside piles before the council lorry arrives
to haul everything away. This is an authentically British scavenging opportunity: arrive
early for the best loot, but the Neighbourhood Watch will be watching.

### SkipDivingSystem

A new `SkipDivingSystem` class manages the entire lifecycle.

**Bulky Item Day Schedule**
- A `BulkyItemDay` event triggers every 3 in-game days, starting on day 3.
- The event opens at 08:00 game-time and closes at 10:00. Anything left in the
  skip at close is removed (the lorry takes it). Lorry arrival is announced by a
  `RUMOUR_TYPE.LOOT_TIP` rumour seeded into the nearest NPC at 09:55: "Council lorry's
  on its way. Last chance."
- The skip spawns at a fixed pavement position 10 blocks east of the Charity Shop.

**Skip Contents (generated fresh each Bulky Item Day)**
Each event generates 8–14 items drawn from the `SkipLot` pool:

| Item / `Material` | Rarity | Fence value (COIN) | Notes |
|---|---|---|---|
| `OLD_SOFA` | Common | 3 | 3 units of `WOOD` when broken |
| `BROKEN_TELLY` | Common | 4 | 1 `SCRAP_METAL` + 1 `COMPUTER` |
| `WONKY_CHAIR` | Common | 2 | 2 `WOOD` |
| `CARPET_ROLL` | Common | 3 | Wearable as `WOOLLY_HAT_ECONOMY` proxy |
| `OLD_MATTRESS` | Uncommon | 5 | Restores 20 warmth when slept on in squat |
| `FILING_CABINET` | Uncommon | 6 | 1 `COIN` + random `DWP_LETTER` |
| `EXERCISE_BIKE` | Uncommon | 7 | Crafting ingredient: `EXERCISE_BIKE` + `SCRAP_METAL` → `IMPROVISED_TOOL` |
| `BOX_OF_RECORDS` | Uncommon | 8 | Gives +10 MC Rank XP; sells to Fence for 8 COIN |
| `MICROWAVE` | Rare | 10 | Converts `GREGGS_PASTRY` → `HOT_PASTRY` (restores +15 hunger) |
| `SHOPPING_TROLLEY_GOLD` | Rare | 12 | Special prop: carries 4× inventory slots as a mobile chest |
| `ANTIQUE_CLOCK` | Very Rare | 20 | Tooltip: "Probably worth something. Probably nicked." |

Items are represented both as world props (stacked near the `COUNCIL_SKIP` prop) and
as `Material` entries the player picks up by pressing E on the adjacent item prop.

**NPC Competitors**
- 2–4 `SKIP_DIVER` NPCs (new `NPCType`) spawn at 08:00 and compete for items. Each
  tick (every 30 real seconds) an unblocked `SKIP_DIVER` grabs a random unclaimed item.
- The `PIGEON_FANCIER` NPC always arrives first at 07:55 and claims `BOX_OF_RECORDS`
  before the window opens ("I've got first dibs, mate"). The player can press E on him
  to negotiate: pay 5 COIN or let him keep it.
- `SKIP_DIVER` NPCs acknowledge the player with contextual speech:
  - "Bags I the sofa."
  - "You were quick."
  - "Don't even think about that telly."

**Neighbourhood Watch Integration**
- `NeighbourhoodWatchSystem.WatchAnger` increases by +3 for every item the player
  takes from the skip (perceived as anti-social hoarding).
- If the player takes 5+ items in a single event, a `PETITION_BOARD` prop spawns.
- Anger cools normally after the lorry departs (event end).

**Fence Integration**
- Items from the skip count as legitimate salvage: selling them does NOT add to
  `Notoriety` (unlike stolen goods).
- `FenceValuationTable` recognises all `SkipLot` materials at the values above.
- `ANTIQUE_CLOCK` triggers special Fence dialogue: "Where'd you get this? Asda? No,
  really — where'd you get it?" Awards `AchievementType.ANTIQUE_ROADSHOW`.

**Crafting Integration**
- `OLD_MATTRESS` + `SLEEPING_BAG` → `LUXURY_BED` (squat furnishing, +15 Vibe).
- `EXERCISE_BIKE` + `SCRAP_METAL` → `IMPROVISED_TOOL` (bypasses the normal WOOD + STONE
  recipe; this is a weaker variant: 25 uses vs. 30).
- `MICROWAVE` placed in squat enables `GREGGS_PASTRY` → `HOT_PASTRY` conversion
  at the workbench.

**Key Controls**
- **E** on a skip item prop: pick up the item (adds to inventory). If a `SKIP_DIVER`
  is adjacent to the same item, a brief tug-of-war triggers (first E press wins).
- The `COUNCIL_SKIP` prop itself is destructible (8 punches → `SCRAP_METAL`), but
  destroying it raises `WatchAnger` by +20 and triggers immediate `WantedSystem`
  escalation (1 star).

**Achievements**
- `SKIP_KING`: collect 5+ items in a single Bulky Item Day event.
- `ANTIQUE_ROADSHOW`: sell the `ANTIQUE_CLOCK` to the Fence.
- `EARLY_BIRD`: be the first entity (player or NPC) to take an item from the skip
  on any Bulky Item Day.

**Tooltip (first skip interaction)**: "Someone's loss is your gain. Probably literally."

**Unit tests**: `SkipDivingSystem` schedule (event triggers on correct days), loot table
generation (8–14 items, correct rarity weights), `SKIP_DIVER` NPC item-grab tick,
`WatchAnger` increment per item taken, `FenceValuationTable` recognises all skip
materials, `ANTIQUE_CLOCK` triggers achievement on fence sale.

### Integration Tests — implement these exact scenarios

1. **Bulky Item Day spawns on day 3**: Create a `SkipDivingSystem`. Advance
   `TimeSystem` to day 3 at 07:59. Verify `skipDivingSystem.isEventActive()` is
   `false`. Advance to 08:00. Verify `isEventActive()` is `true`. Verify a
   `COUNCIL_SKIP` prop has been placed in the world at the expected position
   (10 blocks east of the Charity Shop). Verify 8–14 item props are present in
   the skip zone (within 3 blocks of the skip prop).

2. **Player picks up item and WatchAnger increases**: Set an active Bulky Item Day.
   Place an `OLD_SOFA` prop adjacent to `COUNCIL_SKIP`. Set `WatchAnger` to 10.
   Player presses E on the `OLD_SOFA`. Verify `Material.OLD_SOFA` is now in the
   player's inventory. Verify `OLD_SOFA` prop has been removed from the world.
   Verify `WatchAnger` is now 13 (+3 per item).

3. **5+ items triggers PetitionBoard**: Simulate the player taking 5 items from a
   single Bulky Item Day event (call `skipDivingSystem.onPlayerTakesItem()` 5
   times). Verify a `PETITION_BOARD` prop has been placed on the pavement within
   10 blocks of the skip position.

4. **Lorry removes remaining items at 10:00**: Set an active Bulky Item Day. Leave
   4 items unclaimed in the skip zone. Advance `TimeSystem` to 10:00. Call
   `skipDivingSystem.update(...)`. Verify `isEventActive()` is `false`. Verify all
   4 unclaimed item props have been removed from the world. Verify the `COUNCIL_SKIP`
   prop has also been removed.

5. **SKIP_DIVER NPC grabs unclaimed items**: Set an active Bulky Item Day with 3
   items in the skip. Spawn a `SKIP_DIVER` NPC adjacent to the skip. Advance
   simulation by 30 real seconds (NPC grab tick interval). Verify the `SKIP_DIVER`
   NPC's inventory contains 1 item and 1 item has been removed from the skip zone.

6. **ANTIQUE_CLOCK fence sale awards achievement**: Give player 1 `ANTIQUE_CLOCK`.
   Press E on the Fence. Verify the Fence offers the special dialogue ("Where'd you
   get this?"). Confirm sale. Verify `ANTIQUE_CLOCK` removed from inventory. Verify
   player receives 20 COIN. Verify `AchievementType.ANTIQUE_ROADSHOW` is awarded.

7. **PigeonFancier claims BOX_OF_RECORDS at 07:55**: Create a `PIGEON_FANCIER` NPC.
   Set `TimeSystem` to day 3, 07:55. Advance 1 tick. Verify `PIGEON_FANCIER` NPC
   holds `BOX_OF_RECORDS` (it's pre-claimed). At 08:00 when the window opens, verify
   `BOX_OF_RECORDS` is NOT in the skip zone. Verify player can press E on the
   `PIGEON_FANCIER` to receive negotiation dialogue ("I've got first dibs, mate").
   Pay 5 COIN. Verify player receives `BOX_OF_RECORDS` and `PIGEON_FANCIER` relinquishes it.

---

## Add Greasy Spoon Café — Vera's Caff, Full Breakfast & Rumour Hub

**New system**: `GreasySpoonSystem` in `ragamuffin/core/GreasySpoonSystem.java`

A proper greasy spoon caff — the heartbeat of the British working-class neighbourhood.
"Vera's Caff" (landmark type `GREASY_SPOON`) opens 07:00–14:00 daily. Inside, NPC
regulars gossip over mugs of tea and full English breakfasts, making it the richest
passive rumour hub in the game. The player can sit down, order, eat, and eavesdrop.
Bad weather packs it out; Mondays after the weekend are busiest.

### Landmark

Add `GREASY_SPOON` to `LandmarkType`. Display name: `"Vera's Caff"`. Placed by
`WorldGenerator` on the high street between the off-licence and the charity shop.
Interior: 8×6 blocks, four two-seat tables (WOOD props), counter along the back wall
(`COUNTER` prop), gas-hob behind the counter (`HOB` prop), chalkboard specials sign
(`CHALKBOARD` prop).

### Menu & New Materials

Add the following entries to `Material`:

| Material | Cost (COIN) | Hunger restored | Warmth restored | Notes |
|----------|-------------|-----------------|-----------------|-------|
| `FULL_ENGLISH` | 6 | 60 | 20 | Restores most hunger of any single item; tooltip on first eat: "The full English: Britain's greatest contribution to civilisation." |
| `MUG_OF_TEA` | 2 | 0 | 25 | Same warmth as `FLASK_OF_TEA` but cheaper; consumed on seat |
| `BEANS_ON_TOAST` | 3 | 30 | 10 | Budget option |
| `FRIED_BREAD` | 2 | 20 | 5 | Sold as a side; pairs with `FULL_ENGLISH` for a combined +10 hunger bonus |
| `BACON_BUTTY` | 4 | 40 | 10 | Morning staple; available until 11:00 only; tooltip on purchase after 11:00: "Sorry love, it's gone eleven. No more bacon." |
| `BUILDER_S_TEA` | 1 | 0 | 15 | Weak version; Vera's cheapest item; "Builder's tea — strong, brown, no nonsense." |

Vera also sells `NEWSPAPER` (1 COIN, as elsewhere) and `CIGARETTE` (2 COIN) from behind
the counter.

### Opening Hours & Weather Modifier

- Open: 07:00–14:00 (checked against `TimeSystem`).
- Closed outside hours: pressing **E** on the door shows "Sorry, we're closed. Come back
  tomorrow." (until 07:00) or "We're done for the day, love." (after 14:00).
- **Weather modifier**: when `WeatherSystem.getCurrentWeather()` is `RAIN`,
  `DRIZZLE`, or `THUNDERSTORM`, the number of seated NPC customers increases by +2
  (NPCs sheltering). Vera mutters: "Raining again. Good for business."
- **Monday rush**: on Monday (day-of-week derived from `TimeSystem.getDayCount() % 7 == 1`),
  NPC customer count is at maximum (4 customers instead of the usual 0–2).

### Ordering Mechanic

1. Player presses **E** on the counter or on Vera (new `NPCType.CAFF_OWNER`).
2. A simple order menu appears (same pattern as `ChippyOrderUI`):
   - Shows available items with prices and current stock.
   - `BACON_BUTTY` greyed out and unselectable after 11:00.
3. Player selects item; COIN is deducted from inventory. Item is added directly to
   inventory (player eats at their leisure) — except `MUG_OF_TEA` and `BUILDER_S_TEA`
   which are consumed immediately on purchase (no inventory slot used) and apply
   warmth instantly.
4. Vera has contextual speech based on time and weather:
   - Before 09:00: "You're up early. Building site?"
   - Raining: "Horrible out there, isn't it. Sit down."
   - Player has high Notoriety (≥40): "You look like trouble. Eat and go."
   - Player is low on COIN (< 3): "I can do you a builder's tea for a quid, love."

### Seated Eavesdropping

- 2–4 `NPCType.CAFF_REGULAR` NPCs are seated when the caff is open. Each `CAFF_REGULAR`
  holds 1 random `Rumour` (drawn from the `RumourNetwork` on spawn).
- When the player sits at an adjacent table (walks within 2 blocks of a `CAFF_REGULAR`),
  the NPC's rumour is automatically revealed to the player via the `SpeechLogUI`
  (same mechanism as `PubLockInSystem`). This does NOT require pressing E.
- Rumours available via caff: `LOOT_TIP`, `COUNCIL_NOTICE`, `GANG_ACTIVITY`,
  `WEATHER_TIP`, and a new type `LOCAL_GOSSIP` (flavour only, e.g. "Did you hear
  about Derek's allotment? Council's after him.").
- After hearing 3 unique rumours in one caff visit, the achievement
  `WELL_INFORMED` is unlocked.

### Daily Specials Board

- The `CHALKBOARD` prop inside the caff displays a random "Daily Special" drawn at
  07:00 each day from a weighted pool:
  - "Today's special: FULL ENGLISH + TEA — 7 COIN" (saves 1 coin on the combo;
    player buying both gets the discount automatically).
  - "Today's special: BEANS ON TOAST — 2 COIN" (1 coin off).
  - "Closed Monday — WRONG, WE ARE OPEN. Ignore that." (flavour only, no discount).
- The special is stored in `GreasySpoonSystem.dailySpecial` (a `String`) and updated
  at 07:00 via the `TimeSystem` hour-change hook.
- Pressing **E** on the chalkboard displays the special in a tooltip.

### Notoriety & Police Integration

- If the player's Notoriety is ≥ 60 and a `POLICE` NPC is within 15 blocks, Vera
  refuses service: "I run a respectable establishment. Out."
- If the player commits a crime (block-break, NPC assault) within 12 blocks of the
  caff during opening hours, all `CAFF_REGULAR` NPCs flee (set to `NPCState.FLEEING`)
  and Vera shouts: "Oi! Take it outside!" The caff counts as a witness location for
  `WitnessSystem`.

### Faction Integration

- `CAFF_REGULAR` NPCs are neutral; however, if `FactionSystem` respect for
  `Faction.MARCHETTI_CREW` ≥ 70, one regular is replaced by a `MARCHETTI_MEMBER`
  NPC who sells `PRESCRIPTION_MEDS` via the street-deal mechanic (same as
  `StreetEconomySystem`) from their seat — effectively using the caff as a front.
- If the player tips off `WitnessSystem` about the deal, Marchetti respect drops −25
  and the `CAFF_REGULAR` replacement is removed for 3 in-game days.

### Achievement

- `FULL_ENGLISH_FANATIC`: eat a `FULL_ENGLISH` on 5 separate in-game days.
- `WELL_INFORMED`: hear 3 unique rumours in a single caff visit.
- `REGULAR`: visit Vera's Caff on 7 consecutive in-game days.

### Tooltip (first entry)

"Vera's Caff. Est. 1987. Cash only. No WiFi. No nonsense."

### Unit Tests

- `GreasySpoonSystem` opens/closes correctly based on `TimeSystem` hour.
- `BACON_BUTTY` unavailable after 11:00 (returns `false` from `canOrder()`).
- Weather modifier increases seated NPC count in rain (+2 customers).
- Monday rush sets customer count to 4.
- Combo discount applied when buying `FULL_ENGLISH` + `MUG_OF_TEA` on special day.
- Notoriety ≥ 60 + police nearby blocks ordering.
- `CAFF_REGULAR` rumour eavesdrop triggers `SpeechLogUI` entry on proximity.
- `FULL_ENGLISH_FANATIC` achievement unlocks after 5 separate eating days.

### Integration Tests — implement these exact scenarios

1. **Caff opens at 07:00 and closes at 14:00**: Create a `GreasySpoonSystem`. Set
   `TimeSystem` to 06:59. Verify `greasSpoonSystem.isOpen()` is `false`. Advance
   time to 07:00. Verify `isOpen()` is `true`. Advance time to 14:00. Verify
   `isOpen()` is `false`. Pressing E on the door after 14:00 returns the closed
   message "We're done for the day, love."

2. **Player orders FULL_ENGLISH, hunger increases**: Set player hunger to 20. Set
   player COIN count to 10. Player presses E on Vera and selects `FULL_ENGLISH`
   (cost 6 COIN). Verify player COIN is now 4. Verify `FULL_ENGLISH` is in player
   inventory. Player uses `FULL_ENGLISH` from inventory. Verify player hunger has
   increased by 60 (capped at 100 if applicable). Verify `AchievementType.FULL_ENGLISH_FANATIC`
   progress has incremented by 1.

3. **BACON_BUTTY unavailable after 11:00**: Set time to 11:01. Call
   `greasSpoonSystem.canOrder(Material.BACON_BUTTY, currentHour)`. Verify it returns
   `false`. Set time to 10:59. Verify `canOrder(Material.BACON_BUTTY, currentHour)`
   returns `true`.

4. **Rain weather increases seated NPCs**: Set weather to `Weather.RAIN`. Call
   `greasSpoonSystem.getSeatedNpcCount()`. Verify it returns at least 2 more than the
   same call under `Weather.CLEAR`. (Base count + 2 rain modifier.)

5. **Eavesdropping reveals CAFF_REGULAR rumour**: Spawn a `CAFF_REGULAR` NPC with a
   seeded `Rumour` of type `LOOT_TIP`. Place the player 3 blocks away. Call
   `greasSpoonSystem.update(delta, player, ...)`. Verify the `SpeechLogUI` (or
   equivalent log) now contains the `LOOT_TIP` rumour text. Verify the
   `WELL_INFORMED` achievement counter has incremented.

6. **Notoriety blocks service when police nearby**: Set player Notoriety to 65.
   Spawn a `POLICE` NPC 10 blocks from the caff. Player presses E on Vera. Verify
   ordering returns the refusal message "I run a respectable establishment. Out."
   Verify no COIN is deducted from player inventory.

7. **Monday rush spawns maximum customers**: Set `TimeSystem.dayCount` so that
   `dayCount % 7 == 1` (Monday). Set weather to `Weather.CLEAR`. Call
   `greasSpoonSystem.getSeatedNpcCount()`. Verify the count equals 4 (maximum Monday
   value).

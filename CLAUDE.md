# Ragamuffin

## Project Description

Ragamuffin is an irreverent **3D** survival-sandbox game set in modern-day Britain. The player is kicked out of their parents' house and dumped at a local park in a deprived British town. The core gameplay loop mirrors Minecraft — gather resources, craft items, build structures — but transplanted into a grimly comedic urban setting. The game is rendered in 3D with a voxel-based world, first-person camera, and block-based building mechanics.

### Core Concept
- 3D voxel-based survival game mechanics, set in the real world
- Opening: your parents kick you out saying "it's time to learn to survive on your own"
- You start in a local park. Tooltip: "punch a tree to get wood"
- At night, Police try to move you on
- Built structures eventually get police tape across them
- Advanced materials obtained by raiding office buildings
- Council builders arrive to demolish unauthorized structures. Tooltip: "dodge to avoid the attacks of stronger enemies"
- Tooltip: "Jewellers can be a good source of diamond"
- Open-ended sandbox with no win condition

### Tone & Aesthetics
The game has a British dark-humour sensibility with Dada-esque surrealism. It is irreverent and playful. Tooltips serve as deadpan satirical commentary. The world should feel like a slightly exaggerated version of a real British town — not cartoonish, but absurd in its matter-of-factness. The 3D art style should be lo-fi and blocky (think Minecraft meets council estate).

### NPCs & Entities
- **Police**: patrol at night, try to move the player on, escalate if resisted
- **Council builders**: arrive to demolish player structures, act as "stronger enemies"
- **Members of the public**: ambient NPCs, react to player's creations
- **Dogs**: roaming hazard/companion potential
- **Gangs of youths**: territorial, unpredictable
- **Members of the local council**: bureaucratic antagonists

### World
3D voxel-based urban environment: a deprived British town centred on the park. Includes streets, terraced houses, an off-licence, a Greggs, charity shops, a JobCentre, office buildings, a jeweller, industrial estate, etc. The world is made of blocks that the player can break and place, Minecraft-style.

## Technical Stack
- **Language**: Java 17 (Amazon Corretto)
- **Game framework**: LibGDX 1.12.1 (3D API — PerspectiveCamera, ModelBatch, Environment, custom voxel/chunk mesh rendering)
- **Build tool**: Gradle 9.3.1
- **Testing**: JUnit 5 + Mockito
- **Desktop backend**: LWJGL3
- **Headless backend**: LibGDX headless (for tests)

## 3D Architecture Notes
- **Voxel/chunk system**: The world is divided into chunks (e.g. 16x16x64 blocks). Each chunk builds its own mesh from visible block faces. Only exposed faces are rendered (greedy meshing or simple culling).
- **First-person camera**: WASD movement, mouse look. Standard FPS controls.
- **Block types**: Each block type has textures for its 6 faces. Use a texture atlas.
- **Lighting**: Basic ambient + directional lighting. Day/night cycle changes light direction and colour.
- **Collision**: AABB collision detection against the voxel grid.
- **NPCs**: Rendered as simple 3D models or billboard sprites in the 3D world.

## Development Conventions
- **Test-driven development**: write tests BEFORE implementation
- Keep classes small and focused — prefer composition over inheritance
- Use an ECS-inspired architecture for game entities (Entity + Components)
- All game text should reflect dry, dark British humour
- Commit frequently with descriptive messages
- Run `./gradlew test` before considering any feature complete
- Run `./gradlew build` to verify full compilation

### CRITICAL: Integration Testing
Every phase MUST include integration tests that verify features work together end-to-end, not just unit tests of isolated components. Specifically:
- **UI/HUD integration tests**: Verify that HUD elements, tooltips, menus, and overlays are actually created, positioned, and visible when the game is running. Test that UI components respond to game state changes (e.g. health bar updates when health changes, tooltip appears when expected trigger occurs).
- **Rendering integration tests**: Use the LibGDX headless backend to verify that the scene graph is constructed correctly — models are added to the scene, camera is positioned, chunks generate meshes, etc. You cannot test pixel output in headless mode, but you CAN verify that the rendering pipeline is wired up (e.g. ModelBatch receives the right models, SpriteBatch draws the right elements).
- **Game loop integration tests**: Test that game systems interact correctly across a simulated frame update — e.g. player moves, camera follows, world chunks load, NPCs react, HUD updates — all in one test.
- **Do NOT consider a phase complete if only unit tests pass.** If the game compiles and unit tests pass but the feature doesn't visually work when you run the game, that is a failure. Integration tests should catch these wiring issues.

## Project Structure
```
src/main/java/ragamuffin/
  core/       - game loop, state management, screens
  world/      - voxel chunk system, world generation, block types
  entity/     - player, NPCs, entity-component system
  building/   - crafting, block placement, resource system
  ui/         - HUD, tooltips, inventory, menus
  ai/         - NPC behaviour, pathfinding, interaction logic
  render/     - chunk mesh building, texture atlas, 3D rendering pipeline
src/test/java/ragamuffin/
  [mirrors main structure]
  integration/ - end-to-end integration tests
```

## Updating SPEC.md
You may update SPEC.md if you feel updates are required as development progresses
(e.g. adding implementation notes, clarifying edge cases, adding new test scenarios).
However, you must ONLY add information — never remove or weaken existing specifications,
requirements, or test scenarios. Treat SPEC.md as an append-only document for requirements.

## Build Commands
- `./gradlew build` — compile and package
- `./gradlew test` — run all tests (unit + integration)
- `./gradlew run` — launch the game (requires display; use -XstartOnFirstThread on macOS)

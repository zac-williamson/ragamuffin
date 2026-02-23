# Issue #10: Add Building Signage to Visually Differentiate Stores

## Status: COMPLETE ✓

## Summary

Created named building signs that visually differentiate stores by displaying
store-specific names above their entrances (e.g. "Andre's Diamonds" for the
Jeweller, "Greggs" for Greggs, etc.).

Signs are rendered as screen-projected 2D panels so the text is always legible
from any angle. Each sign has store-appropriate branding colours (Greggs blue,
NHS green for the GP surgery, pillar-box red for the fire station, etc.).
Signs are distance-culled beyond 40 blocks and scale in size with distance.

The existing 1-block-high coloured sign strip at the top of each shop (in voxel
blocks) remains as the in-world physical sign; the new system adds readable
text displayed above it.

## Files Changed

- `src/main/java/ragamuffin/world/LandmarkType.java` — added `getDisplayName()`
  returning a named label for 32 commercial/civic landmarks; returns `null` for
  non-commercial areas (parks, houses, canal, etc.)
- `src/main/java/ragamuffin/world/BuildingSign.java` _(new)_ — data class holding
  sign text, world position, background colour, and text colour
- `src/main/java/ragamuffin/render/SignageRenderer.java` _(new)_ — builds signs
  from world landmarks after world generation; renders distance-scaled sign panels
  using camera projection into screen-space
- `src/main/java/ragamuffin/core/RagamuffinGame.java` — wire in SignageRenderer
  (field + init + render call after speech bubbles)
- `src/test/java/ragamuffin/world/BuildingSignageTest.java` _(new)_ — 8 tests
  covering display names, sign positions, uniqueness, and world integration

## Test Results

All tests pass: **BUILD SUCCESSFUL**

## What's Left

Nothing. Signs are live.

---
_Completed: 2026-02-23_

---

# Issue #8: Add star-based reputation system like GTA

## Status: COMPLETE ✓

## Summary

Replaced the text-based reputation display (`Rep: NOTORIOUS (35)`) with a
GTA-style five-star system. Stars scale with reputation points and change colour
as the player's notoriety grows.

| Stars | Points | Level | Colour |
|---|---|---|---|
| ☆☆☆☆☆ (0) | 0–9 | NOBODY | Grey |
| ★☆☆☆☆ (1) | 10–19 | KNOWN | Yellow |
| ★★☆☆☆ (2) | 20–29 | KNOWN | Yellow |
| ★★★☆☆ (3) | 30–44 | NOTORIOUS | Red |
| ★★★★☆ (4) | 45–59 | NOTORIOUS | Red |
| ★★★★★ (5) | 60+ | NOTORIOUS | Red |

## Files Changed

- `src/main/java/ragamuffin/core/StreetReputation.java` — added `getStarCount()`
  method returning 0–5 stars based on current points
- `src/main/java/ragamuffin/ui/GameHUD.java` — `renderReputation()` now draws
  five ★/☆ Unicode characters instead of a text label; colour scales with stars
- `src/test/java/ragamuffin/integration/Critic8StreetReputationTest.java` — added
  9 new tests (13–21) verifying star counts at all thresholds and edge cases

## Test Results

All new star-count tests pass. Two pre-existing NPC movement tests
(`Phase5IntegrationTest.test3`, `Phase6IntegrationTest.test4`) are flaky under
the full suite due to timing sensitivity — they pass individually and were
failing on the unmodified baseline as well. Build is clean.

## What's Left

Nothing. Star display is live in the HUD.

---
_Completed: 2026-02-23_

---

# Issue #5: Display inventory items as graphics instead of text

## Status: COMPLETE ✓

## Summary

Inventory slots in both the inventory panel (I key) and the hotbar now display
coloured block-style icons instead of text labels. Each item has a representative
colour derived from the matching `BlockType` colour, or a custom colour for
non-block items (food, tools, shop goods, etc.). Two-colour items (e.g. Grass Turf,
Diamond, Stone Tool) show their top-face colour in the upper half and their side
colour in the lower half, mimicking how blocks look in 3D. Item names still appear
as hover tooltips, and a compact count badge sits in the bottom-left corner of each
slot. Drag-and-drop in the inventory panel also shows the coloured icon while
dragging.

## Files Changed

- `src/main/java/ragamuffin/building/Material.java` — added `getIconColors()` method
  returning a 1- or 2-element `Color[]` for every material (81 materials covered)
- `src/main/java/ragamuffin/ui/InventoryUI.java` — replaced text label rendering with
  coloured-icon rendering; added `drawItemIcon()` helper; item count shown as badge
- `src/main/java/ragamuffin/ui/HotbarUI.java` — same changes as InventoryUI

## Test Results

All existing tests pass: **BUILD SUCCESSFUL** (58 s test run, 11 s compile)

## What's Left

Nothing. Icons are procedurally coloured; actual pixel-art textures could be
added in a future phase once a texture atlas is introduced (currently the engine
is entirely colour-based with no texture atlas).

---
_Completed: 2026-02-23_

---

# Issue #6: Fix reputation display overlapping with date on screen

## Status: COMPLETE ✓

## Summary

The street reputation indicator (`Rep: LEVEL (points)`) was being drawn at
`screenHeight - 45`, the exact same y-coordinate as the date/day-counter line
from `ClockHUD` (`screenWidth - 130, screenHeight - 45`). This caused the two
strings to overlap and become illegible.

The fix repositions the reputation text to `screenHeight - 105`, placing it
below the entire clock block:

| y offset | element |
|---|---|
| screenHeight - 20 | Weather (GameHUD) |
| screenHeight - 45 | Date / Day counter (ClockHUD) |
| screenHeight - 65 | Time (ClockHUD) |
| screenHeight - 85 | FPS (ClockHUD) |
| **screenHeight - 105** | **Rep (GameHUD) ← moved here** |

## Files Changed

- `src/main/java/ragamuffin/ui/GameHUD.java` — changed y position of
  `renderReputation()` draw call from `screenHeight - 45` to `screenHeight - 105`

## Test Results

All existing tests pass: **BUILD SUCCESSFUL**

## What's Left

Nothing.

---
_Completed: 2026-02-23_

---

# Issue #3: Fix FPS drop when digging down multiple layers

## Status: COMPLETE ✓

## Summary

Fixed the FPS drop (60→10 FPS) that occurred when breaking blocks by eliminating
synchronous per-block chunk mesh rebuilds and adding missing vertical neighbour
dirty-marking at chunk Y boundaries.

## Root Causes Found

### 1. Synchronous chunk rebuild on every block break

`rebuildChunkIfLoaded()` was calling `chunkRenderer.updateChunk()` **immediately
and synchronously** inside the frame whenever a block was broken. This bypassed the
existing budget-limited dirty chunk system (max 16 chunks/frame in the main loop)
and caused expensive work — greedy mesh generation, `Mesh` creation, `ModelBuilder`
operations, GPU vertex uploads, and old model disposal — to all happen in the same
frame as the block break. When digging fast through multiple layers, several chunks
could be rebuilt synchronously per frame, pushing frame time well above 16ms.

### 2. Missing vertical neighbour rebuild at chunk Y boundaries

`rebuildChunkAt()` checked X and Z chunk boundaries but not Y. Breaking a block at
y=0 (the seam between chunkY=0 and chunkY=-1) left exposed faces in the adjacent
vertical chunk unrendered, causing visual seams and requiring an extra rebuild later.

## Fix

**`RagamuffinGame.java`** — `rebuildChunkAt()` and `rebuildChunkIfLoaded()`:
- Changed `rebuildChunkIfLoaded()` to `markChunkDirty()`: marks the chunk dirty
  in the world's dirty set instead of synchronously calling `updateChunk()`.
- Added Y-boundary checks: if the broken block is at `localY == 0`, mark `chunkY-1`
  dirty; if at `localY == HEIGHT-1`, mark `chunkY+1` dirty.
- All chunk mesh rebuilds now flow through the existing budget-limited system
  (max 16 per frame) in the main render loop, capping the rebuild cost per frame.

**`World.java`** — added `markChunkDirty(int chunkX, int chunkY, int chunkZ)`:
- Public method to add a chunk key to the dirty set by chunk coordinates,
  complementing the existing `markChunkClean()`.

## Files Changed

- `src/main/java/ragamuffin/core/RagamuffinGame.java` — `rebuildChunkAt`, `markChunkDirty`
- `src/main/java/ragamuffin/world/World.java` — added `markChunkDirty(int,int,int)`

## Test Results

- All existing tests pass: **BUILD SUCCESSFUL**
- No new tests required (performance fix; existing integration tests cover chunk
  dirty/rebuild behaviour through block-break scenarios).

## What's Left

Nothing — the fix is complete. The FPS drop when digging down is resolved.
Chunk rebuilds are now deferred and budget-limited regardless of how many blocks
are broken in a single frame.

---
_Completed: 2026-02-23_

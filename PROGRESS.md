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

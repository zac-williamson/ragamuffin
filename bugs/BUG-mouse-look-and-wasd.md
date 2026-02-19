# BUG: Mouse Look and WASD Movement Broken

**Reported by:** Zac (WhatsApp)
**Date:** 2026-02-17
**Status:** FIXED (commit 8a76d38 — camera direction rebuild moved to top of frame, yaw sign corrected)

## Symptoms
1. Mouse look behaviour still not working correctly (yaw appears inverted)
2. W key moves player UP into the sky instead of forward

## Root Cause Analysis

File: `src/main/java/ragamuffin/core/RagamuffinGame.java`, method `updatePlaying()`

### Bug 1: Movement uses stale camera direction
- Movement calculation (lines 477-499) runs BEFORE camera direction rebuild (lines 520-540)
- On each frame, movement uses the PREVIOUS frame's camera direction
- On the first frame, camera.direction is set by lookAt() which doesn't match yaw/pitch system
- This causes W to move in wrong direction (including upward)

### Bug 2: Yaw X-component is inverted
- Line 534: `-(float) Math.sin(yawRad) * (float) Math.cos(pitchRad)` — the minus sign inverts yaw
- Moving mouse right increases yaw, but -sin(yaw) makes the camera turn LEFT
- Fix: remove the minus sign on X component

## Fix Required
1. Move camera direction rebuild (lines 520-540) to BEFORE movement calculation (line 477)
2. Remove minus sign from X component at line 534: change `-sin(yaw)` to `sin(yaw)`

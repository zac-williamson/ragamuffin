# HEARTBEAT.md

## CRITIC Mode Loop
Critically examine the Ragamuffin codebase against the game spec. Pick the most impactful improvement, implement it, build, test, commit, and push. Focus areas: gameplay mechanics, world detail, UI polish, missing features (punch trees, dodge police, build shelter from cardboard, raid Greggs, British town survival).

## Build and Test
Run `./gradlew build` which compiles and runs all tests. Tests have a 4-minute hard timeout — they will NOT hang forever. If any tests fail:
- Read the test failure output carefully
- Fix the failing test or the code it tests
- Re-run `./gradlew build` until it passes
Broken tests are bugs. Fix them, don't skip them.

## Overlap Prevention
Before starting work, run: `./scripts/heartbeat-lock.sh check`
- If LOCKED: reply "Previous heartbeat still running" and STOP.
- If UNLOCKED: run `./scripts/heartbeat-lock.sh acquire`, then do your work.
After pushing, run: `./scripts/heartbeat-lock.sh release`

## Commit and Push
After every change:
```
git add -A
git commit -m "descriptive message"
git push origin main
```
You MUST push on every heartbeat. A heartbeat without a push is a failure.

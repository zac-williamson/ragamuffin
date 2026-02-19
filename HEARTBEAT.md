# HEARTBEAT.md

## What To Do
Pick the most impactful improvement from this list and implement it:
- Gameplay mechanics (punch trees, dodge police, build shelter, raid Greggs)
- Fix failing tests (broken tests ARE bugs)
- UI polish and missing HUD elements
- World detail (British town features, NPCs, interactions)

## Workflow (Be Efficient!)
1. Run `./scripts/heartbeat-lock.sh check` — if LOCKED, reply "Previous heartbeat in progress" and STOP.
2. Run `./scripts/heartbeat-lock.sh acquire`
3. Check what needs doing: `git log --oneline -5 && git status`
4. Read the relevant source files, then make your edits.
5. Build and test: `./gradlew build` (compiles + runs tests, 4-min hard timeout). If tests fail, read the output and fix.
6. Commit and push: `git add -A && git commit -m "message" && git push origin main`
7. Run `./scripts/heartbeat-lock.sh release`

## Efficiency Tips
- Combine related shell commands into a single Bash call where possible
- Do not read files you do not need to edit
- Keep edits focused — one improvement per heartbeat
- Do NOT generate analysis documents, planning files, or scripts

## A heartbeat without a git push is a FAILURE.

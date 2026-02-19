# HEARTBEAT.md

## What To Do
Critically examine the Ragamuffin codebase. Pick the most impactful improvement, implement it, build, test, commit, push. Focus areas:
- Gameplay mechanics (punching trees, dodging police, building shelters)
- World detail (British town, terraced houses, Greggs, charity shops)
- UI polish (HUD, tooltips, inventory)
- Missing features from the game spec
- Failing tests (broken tests are bugs -- fix them)

## Workflow (Be Efficient!)
1. Check lock: run ./scripts/heartbeat-lock.sh check -- if LOCKED, reply and STOP.
2. Acquire lock: run ./scripts/heartbeat-lock.sh acquire
3. Check status: run git status and git log --oneline -5 (combine in one shell command)
4. Pick ONE improvement. Keep changes small and focused.
5. Read the relevant source file(s).
6. Make your edit(s).
7. Build and test: run ./gradlew build (compiles + tests, 4-minute hard timeout).
8. If build fails, read the error and fix it. Re-run until it passes.
9. Commit and push: run git add -A && git commit -m description && git push origin main
10. Release lock: run ./scripts/heartbeat-lock.sh release

## Tips for Speed
- Combine related shell commands into single Bash calls
- Keep changes small -- one bug fix or one small feature per heartbeat
- Do not read files you do not need to edit
- Do not create analysis documents or planning files

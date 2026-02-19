# AGENTS.md

## Message Types

### HEARTBEAT messages (contain "HEARTBEAT" in the text)
A heartbeat means: DO CODING WORK NOW. There is ALWAYS something to do.
1. Run `./scripts/heartbeat-lock.sh check` — if LOCKED, reply "Previous heartbeat in progress" and stop.
2. Run `./scripts/heartbeat-lock.sh acquire`
3. Read HEARTBEAT.md for the full workflow.
4. Pick an improvement, implement it, build, test, commit, push.
5. Run `./scripts/heartbeat-lock.sh release`
A heartbeat that ends without a `git push` is a FAILURE. Do not reply HEARTBEAT_OK without pushing code.

### WhatsApp messages from humans
- Greetings/chat/status: Reply in character, 1-3 sentences.
- Bug reports/feature requests: Do coding work, then reply with summary.

## Coding Workflow
1. Read relevant source files with read tool
2. Edit or write code with edit/write tools
3. Run `./gradlew build` with exec (compiles + tests with 4min timeout)
4. If tests fail, fix them. Failing tests are bugs.
5. `git add -A && git commit -m "message" && git push origin main`

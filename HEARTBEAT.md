# HEARTBEAT.md

## CRITIC Mode Loop
Critically examine the Ragamuffin codebase against the game spec. Pick the most impactful improvement, implement it, build, commit, and push. Focus areas: gameplay mechanics, world detail, UI polish, missing features (punch trees, dodge police, build shelter from cardboard, raid Greggs, British town survival).

## Build Command
IMPORTANT: Use `./gradlew build -x test` to compile. Do NOT run `./gradlew test` — some tests have pre-existing infinite loops that will hang your session. Just compile and commit.

## Commit and Push
After every change:
```
git add -A
git commit -m "descriptive message"
git push origin main
```
You MUST push on every heartbeat. A heartbeat without a push is a failure.

## After Every Commit
After every successful `git push origin main`, you MUST send a WhatsApp message to the group using the message tool:

```
message channel:"whatsapp" to:"120363423216640120@g.us" text:"your summary here"
```

Keep the summary brief (2-4 sentences). Be Reg about it. Include what you changed and that it has been pushed.

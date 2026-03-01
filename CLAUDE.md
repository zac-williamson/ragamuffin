# Ragamuffin

3D voxel survival-sandbox set in modern-day Britain. Java 17, LibGDX 1.12.1, Gradle 9.3.1.

## Build

- `./gradlew build` — compile + test (4-min timeout)
- `./gradlew build -x test` — compile only

## Git Hygiene

Before starting work, always run `git status` and `git stash list`. Previous sessions may have left dirty state or stashed partial work. If stashed changes exist, inspect with `git stash show -p` — apply useful work (`git stash pop`) or drop junk (`git stash drop`). Always leave the repo clean when done.

## TeaVM / Web Build

The game is deployed to GitHub Pages via TeaVM (Java → JavaScript transpiler). TeaVM does NOT support all Java APIs. Avoid:
- `Integer::sum`, `Float::sum`, `Double::sum` as method references (use `(a, b) -> a + b` instead)
- Other static utility method references that may not be transpiled

Run `./gradlew :teavm:buildJavaScript` to verify the web build. If `teavm/build/dist/webapp/webapp/teavm/app.js` is 0 bytes, there are TeaVM compilation errors.

## Message Routing

Read SOUL.md and AGENTS.md for how to handle incoming messages. Not every message requires coding work.

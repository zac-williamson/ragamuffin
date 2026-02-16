# TOOLS.md - Local Notes

## Environment
- **Platform:** Amazon Linux 2023 (EC2, eu-west-2)
- **Java:** Amazon Corretto 17
- **Build:** Gradle 9.3.1

## Build Commands
- `./gradlew build` — compile and package
- `./gradlew test` — run all tests (unit + integration)
- `./gradlew clean test` — clean build and run all tests
- `./gradlew run` — launch the game (requires display; headless server won't render)

## How to Code
Delegate ALL coding work to Claude Code via the **coding-agent** skill.
You are the PM — you review, direct, and verify. You do not write code yourself.
Use the coding-agent skill to write tests, implement features, and fix bugs.

## Exec Tool
Use the exec tool to run shell commands like `./gradlew test` and `git commit`.
The exec tool has full access to run any command in this workspace.

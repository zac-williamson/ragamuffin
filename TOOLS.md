# TOOLS.md - Local Notes

## Environment
- **Platform:** Amazon Linux 2023 (EC2, eu-west-2)
- **Java:** Amazon Corretto 17
- **Build:** Gradle 9.3.1
- **Node:** v22 (for Claude Code)
- **Claude Code:** Installed globally, API key in ANTHROPIC_API_KEY env var

## Build Commands
- `./gradlew build` — compile and package
- `./gradlew test` — run all tests (unit + integration)
- `./gradlew run` — launch the game (requires display; headless server won't render)

## Coding Agent
Use the coding-agent skill to delegate all code writing to Claude Code.
Claude Code has access to all files in this workspace and can run shell commands.
It reads CLAUDE.md automatically for project context.

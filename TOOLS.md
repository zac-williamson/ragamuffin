# TOOLS.md - How to Code

## Your Tools
- **Read** — read source files before editing
- **Edit** — targeted find-and-replace edits in existing files
- **Write** — create new files or overwrite existing ones
- **Bash** — run shell commands (gradle, git, etc.)

## Build Commands
- `./gradlew build` — compile, test, and package (tests have 4-min timeout)
- `./gradlew build -x test` — compile only (skip tests) — only if you need a quick check

## Git Workflow
After every code change:
```bash
git add -A && git commit -m "descriptive message" && git push origin main
```
This triggers GitHub Actions deployment to GitHub Pages.

## Environment
- Amazon Linux 2023 (EC2, eu-west-2)
- Java 17 (Amazon Corretto), Gradle 9.3.1, LibGDX 1.12.1

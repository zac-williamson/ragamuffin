# TOOLS.md - Local Notes

## Environment
- **Platform:** Amazon Linux 2023 (EC2, eu-west-2)
- **Java:** Amazon Corretto 17
- **Build:** Gradle 9.3.1

## Build Commands
- `./gradlew build` — compile and package
- `./gradlew test` — run all tests (unit + integration)
- `./gradlew clean test` — clean build and run all tests

## How to Code

Write code DIRECTLY using your built-in tools:
- **file_read** to read source files
- **file_edit** to make targeted edits (find-and-replace)
- **file_write** to create or overwrite files
- **exec** to run shell commands (gradle, git, etc.)

Do NOT delegate to any external tool, skill, or subprocess.

## Git Workflow
After every code change:
```
git add -A
git commit -m "descriptive message"
git push origin main
```
This triggers GitHub Actions deployment to GitHub Pages.

## Exec Tool
Use the exec tool to run shell commands like `./gradlew test` and `git commit`.
The exec tool has full access to run any command in this workspace.

# Current Priorities

## Active Bug
- None — Bug 012 (TeaVM web export) is DONE
  - Game compiles to JavaScript via gdx-teavm 1.0.5 (1204 classes, 764KB JS)
  - CI/CD deploys web build to GitHub Pages automatically
  - Custom landing page with "Play in Browser" button and fullscreen support

## WhatsApp Feedback Loop
When you receive messages via WhatsApp from users:
1. Read the feedback carefully
2. Convert actionable feedback into a bug report in bugs/ (next available number)
3. Fix the bug using the coding-agent skill
4. Reply to the WhatsApp group with a brief update on what you changed
5. Push changes to GitHub: `git push origin critic2`

## Git Push Protocol
After every commit, push to GitHub:
```
git push origin critic2
```
This triggers the CI/CD pipeline that builds and deploys the game.

## CRITIC Loop
When no bugs remain and no WhatsApp feedback is pending:
- Run the game, identify issues, create bug reports, fix them
- Focus on gameplay feel, visual quality, and the British dark humour tone

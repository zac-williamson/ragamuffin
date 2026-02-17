# Current Priorities

## Active Bug
- Bug 012: Add TeaVM web backend for browser deployment (see bugs/012-teavm-web-export.txt)
  - This is the BIG remaining task — make the game playable in a browser
  - See the bug file for detailed requirements and references
  - After implementing, update .github/workflows/build-deploy.yml to deploy the web build

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

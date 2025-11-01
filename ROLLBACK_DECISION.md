# Repository Rollback Decision

Date: 2025-11-01

## Summary
We are reverting the repository to the previous working state. The recent CI and deployment implementation introduced additional complexity and operational friction that outweighed expected benefits at this time.

## Reasons for Reverting
- Increased complexity in CI/CD workflows leading to maintenance overhead.
- Authentication and permissions mismatch with the target GitHub repository.
- Build pipeline instability observed during Android signing and test reporting setup.
- Risk of delayed delivery due to environment secret provisioning and cross-account configuration.

## Scope of Rollback
- Reverted commits:
  - Revert "test: add minimal JUnit test to produce CI reports" (commit `2dfa87a`)
  - Revert "ci: support Render deploy hook alias and Android signing fallback; update ci-secrets docs" (commit `e8fda0d`)
- Restored repository origin to `applesaucetoaboss/Rastreador-Whatsapp-`.
- Created backup branch from pre-rollback HEAD: `backup/pre-rollback-20251101-0843`.

## Verification Plan
1. Confirm build passes locally: `./gradlew test assembleDebug`.
2. Validate app launches on emulator/device; basic flows match prior behavior.
3. Ensure server `.env.example` remains unchanged and server starts: `node server/server.js`.
4. Confirm GitHub workflows match previous working versions and do not require new secrets.

## Next Steps
- Stabilize on the previous working baseline.
- Reassess CI/CD improvements as a separate, scoped initiative with proper access and secret management.
- Document prerequisites and ownership for any future repository migrations.

## Contact & Notification
- Notify team via commit, repository README note, and messaging channels.
- Owners: maintainers of `applesaucetoaboss/Rastreador-Whatsapp-`.


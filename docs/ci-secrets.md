# GitHub Actions Secrets & Environment Variables

This project’s CI/CD pipeline builds the Android app, runs unit tests, and prepares a signed release artifact. Configure these repository secrets under GitHub → Settings → Secrets and variables → Actions.

## Required Secrets (Android Build)

- `STRIPE_PUBLISHABLE_KEY`: Stripe publishable key used by the app (`pk_live_...` or `pk_test_...`).
- `BACKEND_BASE_URL`: Base URL for the backend/API (e.g., `https://<your-site>.netlify.app`).

## Required Secrets (Release Signing)

- `ANDROID_KEYSTORE_BASE64`: Base64 of your `release.keystore` file.
- `ANDROID_KEYSTORE_PASSWORD`: Keystore password.
- `ANDROID_KEY_ALIAS`: Key alias inside the keystore.
- `ANDROID_KEY_PASSWORD`: Key password for the alias.

## Optional (Server/Stripe)

- `STRIPE_SECRET_KEY`: Stripe secret key for server (`sk_live_...` or `sk_test_...`).
- `STRIPE_WEBHOOK_SECRET`: Stripe webhook signing secret for `/webhook`.
- `STRIPE_PRICE_MXN_180_MONTHLY`: Price ID for 180 MXN monthly subscription.

These optional secrets are needed only if you add server tests or deploy steps that validate Stripe operations during CI.

## Notes

- Android build injects `STRIPE_PUBLISHABLE_KEY` and `BACKEND_BASE_URL` into `~/.gradle/gradle.properties` and exposes them via `BuildConfig` (see `app/build.gradle*`).
- Release artifacts are uploaded as GitHub Actions artifacts. Deployment to stores or hosting services is out of scope for this workflow but can be added later.


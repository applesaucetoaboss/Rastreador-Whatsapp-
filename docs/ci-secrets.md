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

Note: If `ANDROID_KEY_PASSWORD` is not set, the workflow will default the alias `keyPassword` to the same value as `ANDROID_KEYSTORE_PASSWORD`. This allows successful signing when the alias uses the keystore password.

## Optional (Server/Stripe)

- `STRIPE_SECRET_KEY`: Stripe secret key for server (`sk_live_...` or `sk_test_...`).
- `STRIPE_WEBHOOK_SECRET`: Stripe webhook signing secret for `/webhook`.
- `STRIPE_PRICE_MXN_180_MONTHLY`: Price ID for 180 MXN monthly subscription.

## Deployment Hook Secret (Render)

The deployment workflows accept either of these secret names:

- `RENDER_DEPLOY_HOOK_URL`: Preferred name used in documentation.
- `RENDER_DEPLOY_HOOK`: Alternate name currently supported.

You may keep `RENDER_DEPLOY_HOOK` as-is, or add `RENDER_DEPLOY_HOOK_URL` with the same value for consistency. Secret names are case-sensitive and may include uppercase letters, numbers, and underscores.

These optional secrets are needed only if you add server tests or deploy steps that validate Stripe operations during CI.

## Notes

- Android build injects `STRIPE_PUBLISHABLE_KEY` and `BACKEND_BASE_URL` into `~/.gradle/gradle.properties` and exposes them via `BuildConfig` (see `app/build.gradle*`).
- Release artifacts are uploaded as GitHub Actions artifacts. Deployment to stores or hosting services is out of scope for this workflow but can be added later.

## Verifying Android Signing Passwords

Follow these steps to confirm your alias password and keystore password are aligned:

- Decode the keystore from the repository secret locally:
  - PowerShell: `Set-Content -Path release.keystore -Value ([System.Convert]::FromBase64String($env:ANDROID_KEYSTORE_BASE64))`
  - Bash: `echo "$ANDROID_KEYSTORE_BASE64" | base64 -d > release.keystore`

- List keystore contents to confirm alias:
  - `keytool -list -v -keystore release.keystore`
  - Enter `ANDROID_KEYSTORE_PASSWORD` when prompted.

- Validate alias password:
  - Try signing or listing with alias credentials: `keytool -keypasswd -alias <YOUR_ALIAS> -keystore release.keystore`
  - If the alias password equals the keystore password, no change is required.
  - If different, set `ANDROID_KEY_PASSWORD` to the alias password in repository secrets.

- CI validation:
  - Trigger the Android build workflow and ensure the step "Create keystore.properties" completes and the release APK is built successfully.

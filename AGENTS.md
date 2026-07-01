# Agent Instructions

## Scope

- This package currently implements Android Google Pay only.
- Do not add Apple Pay, iOS native files, or Apple Pay placeholder exports unless
  the user explicitly asks for that work.
- Keep the public JavaScript API focused on `GooglePayButton` and
  `isReadyToPayAsync`.

## Repo Structure

- `src/`: TypeScript public API, native view wrapper, and tests.
- `android/`: Expo native module implementation for Google Pay.
- `example/`: Expo app for manual Android verification.
- `expo-module.config.json`: Expo autolinking registration.

## Commands

- TypeScript: `npm run build -- --noEmit`
- Lint: `npm run lint`
- Tests: `npm test -- --runInBand`
- Package dry-run: `npm_config_cache=/private/tmp/expo-pay-npm-cache npm pack --dry-run`
- Android module compile: from `example/android`, run
  `./gradlew :expo-pay:compileDebugKotlin`
- Android app build: from `example/android`, run `./gradlew :app:assembleDebug`

## Native Module Notes

- Follow Expo Modules API patterns from the Expo native module and native view
  docs.
- Keep `expo-module.config.json` registered to
  `expo.modules.googlepay.ExpoGooglePayModule`.
- The Android manifest must keep
  `com.google.android.gms.wallet.api.enabled=true`.
- Use Google Pay TEST by default and expose only `"TEST"` and `"PRODUCTION"` to
  JavaScript.
- Do not add AndroidX activity-result dependencies just for Google Pay result
  parsing; use the Expo activity result callback with
  `PaymentData.getFromIntent` and `AutoResolveHelper.getStatusFromIntent`.

## Release Hygiene

- Keep `package.json`, `package-lock.json`, and `android/build.gradle` version
  metadata aligned.
- Run the JS checks and Android Kotlin compile before considering a change
  production-ready.
- Do not log or persist Google Pay tokens in examples or tests beyond short
  display snippets.

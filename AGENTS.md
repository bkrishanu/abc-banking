# AGENTS.md

Instructions for any AI coding agent working in this project
(Claude Code, Cursor, Codex, Copilot). Read this whole file before doing anything.

## 01 Purpose

ABC Bank Mobile is a native Android banking app built to demo ABC Bank's
retail experience to a client: accounts, transfers, bill pay, card controls,
product applications and self-service, all simulated on Firebase
(project `abc-banking-181019`).

Your job is to extend it without breaking what exists, in the style already
established. When in doubt, match what is there.

## 02 Before you start

Do these in order, every session:

1. Read docs/PRD.md for what we are building and what is out of scope.
2. Read docs/MobileBuild.md for the detailed behaviour of the feature you are touching,
   and docs/DEMO_DATA.md for the persona data it should show.
3. Read docs/DESIGN_SYSTEM.md for every visual decision. Use its tokens.
   Look at the matching sample screen (docs/Sample Screens 1.png, docs/Sample Screens 2.png) via the map in its section 10.
4. Read docs/ARCHITECTURE.md for where code lives and what may touch what.
5. Check `core/designsystem/component/` before creating a new composable.
6. Look at the open issues or task list before proposing new work.
7. Restate the task in one or two sentences and list the files you expect
   to touch. Wait for a go-ahead if that list is longer than five files.

## 03 General rules

- Follow the design system. If a value is not in docs/DESIGN_SYSTEM.md, ask. Do not invent it.
- Keep responsibilities where docs/ARCHITECTURE.md puts them. No business logic in composables or ViewModels; rules live in `domain/`.
- Reuse before creating. Search the codebase for an existing component, use case or formatter first.
- Small, focused changes. One task, one branch, one clear diff.
- Do not add Gradle dependencies, Firebase products, Firestore collections, manifest permissions,
  or `local.properties`/BuildConfig values without asking.
- Do not delete or rewrite working code the task does not require.
- Never "improve" anything listed under intentional decisions in docs/ARCHITECTURE.md section 06.
- Leave the project building and runnable in Android Studio after every change.

## 04 Code guidelines

- Kotlin only. No `!!` without a comment saying why it cannot be null. No `@Suppress` without a reason.
- Composables: stateless where possible, state hoisted to the screen, `Modifier` as the first optional parameter, a `@Preview` for every design-system component and every screen's Content state.
- Screens receive a `UiState` and event lambdas. They never call repositories, Firebase or `LocalContext` for business work.
- ViewModels expose one `StateFlow<UiState>`; UiState is a sealed type with Loading, Empty, Error and Content.
  Form screens that load no remote data (login, transfer amount entry) use only Loading and Content, with field errors inside Content.
- Coroutines run in `viewModelScope` or an injected scope, with an injected `DispatcherProvider`. No `GlobalScope`, no `runBlocking`.
- Money is `Money` (Long cents). Never `Double` for amounts. Format through the shared formatter only.
- Validate every external input (forms, nav arguments, Firestore documents mapped to models) before it reaches a use case. Map Firestore DTOs to domain models in `data/`; a malformed document becomes an error, not a crash.
- Name things by what they do: `sendInstantTransfer`, not `handleClick2`.
- All user-visible text in `res/values/strings.xml`. No hard-coded strings, colours or dimensions in composables.
- Comments explain why, not what. Delete commented-out code.
- Every data-driven screen handles loading, empty, error and offline states.
- UI copy follows the tone rules in docs/DESIGN_SYSTEM.md: plain, short, no exclamation marks.
- Tests: unit tests for every use case and rule in `domain/` (fees, limits, step-up thresholds, MPIN lockout, deposit limits and clearing, low balance alerts, EMI, grouping) and for ViewModel state changes. Compose UI tests only for critical flows: MPIN login, P2P transfer with step-up, card freeze.
- Formatting is ktlint's job. Run it. Do not argue with it.

## 05 Security and best practices

This is a demo with fake data, but it is shown to a bank. Build it the way a bank app should behave.

- No secrets in the repository. `google-services.json` and `local.properties` are gitignored. Never commit keystores or signing passwords.
- Firestore Security Rules are the authorisation layer (Cloud Storage is not used; Spark plan, see docs/PRD.md section 08). Every user path checks `request.auth.uid`. Never widen a rule to make a feature work; ask instead.
- Never log PII or sensitive values (account or card numbers, CVV, balances, customer IDs, MPIN) to Logcat, Crashlytics or Analytics.
- Keep `FLAG_SECURE`, the session timeouts (30 min idle, 300 s background), the MPIN lockout (5 wrong entries, 30 s) and step-up triggers exactly as specified in docs/MobileBuild.md 1.1 and 1.2. Step-up is MPIN re-entry; do not add OTP or biometrics.
- Card PAN and CVV are only held in memory while revealed, and are cleared when the screen leaves composition or the app goes to the background.
- Release builds use R8 (minify + shrink resources). Do not add `-keep` rules wider than needed.
- `android:allowBackup` stays false; backup and data extraction rules exclude all app data.
- No exported components other than the launcher Activity.
- No new auth flow, payment path, or data deletion behaviour without explicit approval.
- If docs/SECURITY.md exists in this project, it overrides this section.

## 06 Useful commands

Run from the project root. On Windows use `.\gradlew.bat` (PowerShell) or `./gradlew` (Git Bash).

```
./gradlew assembleDebug                build a debug APK (must pass before a PR)
./gradlew installDebug                 install on the running emulator or device
./gradlew lint                         Android Lint
./gradlew ktlintCheck                  formatting check (ktlintFormat to fix)
./gradlew testDebugUnitTest            unit tests
./gradlew verifyPaparazziDebug         compare screen screenshots with the recorded goldens
./gradlew recordPaparazziDebug         re-record goldens after an intended UI change (look at them first)
./gradlew connectedDebugAndroidTest    Compose UI tests (needs an emulator or device)
./gradlew signingReport                SHA-1 for registering the app in Firebase
firebase deploy --only firestore --project abc-banking-181019
                                       deploy Firestore rules and indexes (ask first)
```

In Android Studio: open the project root, let Gradle sync, select the `app`
run configuration and a Pixel emulator on the latest stable API level.

If a command fails, report the exact output. Do not work around it silently.

## 07 Definition of done

A task is done when all of these are true:
- The change does what the task asked, and nothing else.
- `assembleDebug`, `lint`, `ktlintCheck` and `testDebugUnitTest` pass locally.
- New UI matches docs/DESIGN_SYSTEM.md and the reference screens, and works on a 360dp-wide emulator in portrait, at font scale 200%, and with TalkBack.
- Loading, empty, error and offline states exist for every new data-driven screen.
- Firestore rules, docs/DEMO_DATA.md, the seed JSON and docs/ARCHITECTURE.md section 04 are updated if the data model changed, and `seedVersion` is bumped.
- The feature has been checked with all four personas (samanta, marcus, grace, daniel), not only the default one.
- Docs are updated if a structure, a decision, or a component changed.
- The summary says what changed, what was not done, and what the user should check on a device.

## 08 When to stop and ask

Stop and ask instead of guessing when:
- The task conflicts with the PRD, docs/MobileBuild.md, the design system, or an architectural boundary.
- docs/MobileBuild.md and the sample screens disagree (for example, Withdraw or Multi-currency accounts).
- Two reasonable readings of the task lead to different work.
- The change needs a new dependency, Firebase product, Firestore collection, permission, or secret.
- The change touches security rules, the MPIN, session handling, step-up or device binding.
- The feature needs data that docs/DEMO_DATA.md does not define. Propose the values; do not invent them in code.
- Never use real people, real companies' customer data, real routing numbers or real card numbers in seed data.
- Something is broken that the task did not mention.
- An item in docs/PRD.md section 08 (open questions) affects the task.

Ask with the specific question, the options you see, and your recommendation.

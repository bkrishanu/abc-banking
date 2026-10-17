# Architecture: ABC Bank Mobile (Android)

<!--
The map of how the app is built and how its parts connect.
The agent can read the files. It cannot read the decisions behind them.
Those are written down here. When the structure changes, update this file
in the same commit.
-->

## 01 System overview

```
Android phone
    |
    v
ABC Bank app (single Activity, Jetpack Compose)
    |  UI (screens)  ->  ViewModels  ->  Use cases  ->  Repositories
    |                                                       |
    |   Android Keystore (device key)  <--------------------+
    |   DataStore (local prefs)        <--------------------+
    |                                                       |
    v                                                       v
Firebase project abc-banking-181019
    +--->  Firebase Authentication (anonymous, one user per install)
    +--->  Cloud Firestore (all banking data, offline cache on)
    +--->  Crashlytics + Analytics (stability and demo usage)
           (no Cloud Storage: Spark plan; PDFs are generated on the device)
```

- There is no custom backend. The app talks to Firebase directly through the Firebase Android SDK.
- Firestore Security Rules are the server-side authorisation layer. Every document a user can read or write sits under `users/{uid}` and the rules enforce `request.auth.uid == uid`.
- All "banking" behaviour (transfers, fees, limits, approvals) is simulated in the app's domain layer. See section 06 for why.
- Firebase console: https://console.firebase.google.com/u/0/project/abc-banking-181019/overview

## 02 Tech stack

| Layer | Choice | Why this, not the obvious alternative |
| --- | --- | --- |
| IDE and build | Android Studio (latest stable), Gradle Kotlin DSL, version catalog `gradle/libs.versions.toml`, bundled JDK | Standard Android toolchain, one place for versions |
| Language | Kotlin (latest stable, K2 compiler) | Required for Compose; Java not used |
| SDK levels | minSdk 26, targetSdk and compileSdk 37 (latest stable, 2026-10) | 26 gives java.time, Keystore EC keys and adaptive icons without desugaring |
| Build | Gradle 9.8.1 wrapper (checksum pinned), AGP 9.4 with its built-in Kotlin support, so no `org.jetbrains.kotlin.android` plugin; KSP for Hilt | Exact versions live only in `gradle/libs.versions.toml` |
| UI | Jetpack Compose + Material 3 (Compose BOM), themed by `AbcTheme` | Declarative UI, matches the custom design better than XML views |
| Navigation | Navigation Compose with type-safe routes (kotlinx.serialization) | First-party, no third-party router |
| State | ViewModel + Kotlin Coroutines + StateFlow | Lifecycle-aware, testable; no LiveData, no RxJava |
| DI | Hilt | First-party Android DI; Koin would be a second pattern |
| Backend | Firebase (BOM): Auth, Firestore, Crashlytics, Analytics | Already provisioned for the client demo; no server to run. Spark plan, so no Cloud Storage |
| Local storage | Jetpack DataStore (Preferences) | Session flags, remembered customer ID, MPIN lockout expiry. No Room: Firestore offline cache covers reads |
| Security | Android Keystore | Device binding without third-party SDKs. No AndroidX Biometric: the MPIN is the only factor (PRD 08) |
| Images | Bundled drawables via `painterResource` | Persona photos ship as `drawable-nodpi/avatar_*.webp`; beneficiary avatars are initials, merchant logos are category icons. No Coil: nothing is loaded from a URL |
| Charts | Compose Canvas (no library) | Two simple charts; a library would fight the design |
| PDFs | `android.graphics.pdf.PdfDocument` to create receipts, statements and the 1099-INT; `PdfRenderer` to view them | Framework APIs, no dependency, no Storage needed |
| Testing | JUnit 4, kotlinx-coroutines-test, Turbine, MockK, Compose UI Test | Unit tests for domain and ViewModels; UI tests for critical flows only |
| Formatting and lint | ktlint (Gradle plugin, `android_studio` style in `.editorconfig`) + Android Lint | One formatter, enforced in CI and locally |

Do not add a dependency that overlaps with a row in this table without asking.
Every dependency goes through `libs.versions.toml`; no hard-coded versions in module build files.

## 03 Project structure

The repository root is the Android Studio project root. Open this folder in Android Studio.
Single Gradle module (`:app`), packaged by feature.

```
BankingMobileApp/
  AGENTS.md                     agent instructions (read first)
  images/                       full-size persona photo originals (source for avatar_*.webp; not packaged)
  docs/
    PRD.md                      scope
    DESIGN_SYSTEM.md            visual tokens and components
    ARCHITECTURE.md             this file
    MobileBuild.md              detailed feature spec
    DEMO_DATA.md                pre-loaded personas and catalog (source for assets/seed)
    Sample Screens 1.png        visual reference
    Sample Screens 2.png        visual reference
    licenses/                   third-party licences for bundled assets (Inter-OFL.txt)
  .editorconfig                 ktlint configuration
  firebase.json                 Firebase CLI config (rules + emulators)
  .firebaserc                   default project: abc-banking-181019
  firebase/
    firestore.rules             security rules (source of truth, deployed with Firebase CLI)
    firestore.indexes.json      composite indexes (transactions by date + type + category)
  settings.gradle.kts
  build.gradle.kts              root: plugin declarations only
  gradle.properties
  gradle/
    libs.versions.toml          all versions and dependencies
    wrapper/
  gradlew  gradlew.bat
  local.properties              gitignored: sdk.dir
  app/
    build.gradle.kts
    google-services.json        gitignored: downloaded from the Firebase console
    proguard-rules.pro
    src/
      main/
        AndroidManifest.xml
        assets/seed/            catalog.json + personas/{samanta,marcus,grace,daniel}.json (see DEMO_DATA.md)
        res/
          drawable/             ic_*.xml Lucide icons, illustrations
          drawable-nodpi/       avatar_{personaId}.webp (persona photos), card_art_*.webp (if supplied)
          font/                 inter_*.ttf
          mipmap-*/             launcher icons
          values/               strings.xml, themes.xml (splash + window only), colors.xml (launcher only)
          xml/                  backup_rules.xml, data_extraction_rules.xml, file_paths.xml
        java/com/abcbank/mobile/
          AbcBankApp.kt         @HiltAndroidApp, Firebase init, Crashlytics keys
          MainActivity.kt       single Activity: FLAG_SECURE, edge-to-edge, idle tracking, setContent
          navigation/           AbcNavHost, route definitions, bottom bar destinations, lock gate
          core/
            designsystem/
              theme/            Color.kt, Type.kt, Spacing.kt, Shape.kt, AbcTheme.kt
              component/        AbcButton, ActionPill, BankCardView, TransactionRow, charts, sheets...
            model/              domain models shared across features (Money, Account, Transaction, Card...)
            common/             Result type, DispatcherProvider, Clock, formatters (pure, no Android)
            security/           SessionManager, MpinVerifier (incl. lockout),
                                DeviceKeyManager, StepUpManager
            data/
              firebase/         FirebaseModule, AuthDataSource, Firestore paths, DTOs and mappers
              seed/             DemoDataSeeder, PersonaCatalog (customer ID -> persona), FillerGenerator (seeded random 181019)
              prefs/            DataStore-backed SettingsRepository
            di/                 Hilt modules that bind core interfaces
          feature/
            auth/               Welcome, customer ID + MPIN login, lock screen (MPIN only), step-up sheet
            home/               Home tab: greeting, balance, accounts, quick actions, features
            accounts/           account detail, transaction ledger, search/filter, transaction detail, receipt, dispute
            payments/           internal transfer, P2P/domestic transfer, beneficiaries, bill pay, deposit check (and clearing)
            cards/              Cards tab: card stack, reveal, freeze, limits, channels, travel notices, Google Wallet
            insights/           Insights tab: spending donut, savings chart
            products/           catalog, loan calculator, 4-step application
            notifications/      notification center
            documents/          statement and tax PDF generation and viewer
            support/            support hub, FAQ, chat bot
            profile/            Profile tab: settings, documents, support, log out, hidden reset demo data
      test/java/com/abcbank/mobile/       unit tests (mirror main packages)
      androidTest/java/com/abcbank/mobile/ Compose UI tests for critical flows
```

Inside every `feature/<name>/`:

```
feature/payments/
  ui/          <Name>Screen.kt (composables), <Name>ViewModel.kt, <Name>UiState.kt
  domain/      use cases (SendTransferUseCase), rules (TransferFeeCalculator), repository interfaces
  data/        repository implementations (FirestoreTransferRepository), DTO mapping
  di/          Hilt module binding the feature's repositories
```

Where new code belongs:
- A new screen: `feature/<name>/ui/<Name>Screen.kt` plus its ViewModel and UiState. The screen is a thin composable that renders state and forwards events.
- New business logic (limits, fees, step-up thresholds, EMI): `feature/<name>/domain/`. Pure Kotlin, no Android or Firebase imports.
- A new Firestore read or write: `feature/<name>/data/`, behind an interface declared in `domain/`.
- A new UI primitive: `core/designsystem/component/`, added to DESIGN_SYSTEM.md in the same commit.
- A new Firestore collection or field: update section 04, `firebase/firestore.rules`, DEMO_DATA.md and the seed JSON in the same commit.
- A change to demo values: DEMO_DATA.md and `assets/seed/` together, and bump `seedVersion` so installed apps reseed.
- User-visible text: `res/values/strings.xml`. Never a string literal in a composable.

## 04 Data model and data flow

Money is always `Long` minor units (cents) wrapped in a `Money` value class, USD only.
Timestamps are Firestore `Timestamp` (UTC), shown in the device time zone.

Firestore layout:

```
users/{uid}                         profile: personaId, customerId, seedVersion, name, holderName, email, phone,
                                    avatarKey (personaId; maps to drawable avatar_{key}), employment,
                                    annualIncome, monthlyBudget, creditScore,
                                    checkDepositLimit, createdAt
  accounts/{accountId}              type (CHECKING|SAVINGS|CREDIT|TERM_DEPOSIT|INVESTMENT|LOAN),
                                    nickname, last4, availableBalance, ledgerBalance, rate (APY/APR),
                                    creditLimit, statementBalance, minimumDue, dueDate, autoPayFull,
                                    principal, openedAt, maturesAt, portfolioValue
    holdings/{holdingId}            name, value (INVESTMENT accounts only)
  transactions/{txId}               accountId, direction (DEBIT|CREDIT), amount, merchant,
                                    logoUrl, category, reference, runningBalance, status
                                    (POSTED|PENDING), disputed, createdAt,
                                    clearsAt (pending check deposits only)
  beneficiaries/{beneficiaryId}     name, shortName, avatarUrl, bankName, routingNumber,
                                    accountLast4, isRecent, recentOrder
  cards/{cardId}                    accountId, tag, network, artKey, holderName, pan, cvv, last4,
                                    expiry, frozen, atmDailyLimit, atmMaxLimit, retailDailyLimit,
                                    retailMaxLimit, international, online, contactless, stackOrder
    travelNotices/{noticeId}        destination, startDate, endDate
  bills/{billId}                    billerId, amountDue, dueDate, autoPay, scheduledFor, status
  savingsGoals/{goalId}             name, target, saved, accountId, startedAt, weeklyHistory[]
  applications/{applicationId}      productId, step, answers, decision, createdAt
  notifications/{notificationId}    kind (TRANSACTIONAL|PROMOTIONAL), title, body, read, createdAt
  supportCases/{caseId}             transactionId?, topic, status, messages[]
  devices/{deviceId}                publicKey (base64 EC P-256), model, registeredAt
catalog/banks/{routingNumber}       shared, read-only
catalog/billers/{billerId}          shared, read-only
catalog/products/{productId}        shared, read-only
catalog/faqs/{faqId}                shared, read-only
```

No Cloud Storage (Spark plan). Monthly statements and the 1099-INT are built on demand by
`feature/documents` from the user's accounts and transactions with `PdfDocument`, written to the
app's cache directory, shared through the `FileProvider` in `file_paths.xml`, and deleted when the viewer closes.

Path of every write (example: P2P transfer):
1. User taps Confirm on `TransferReviewScreen`; the screen calls `viewModel.onConfirm()`.
2. ViewModel builds a request from its state and calls `SendTransferUseCase`.
3. Use case validates (amount > 0, <= available balance, beneficiary exists), computes the fee, and asks `StepUpManager` whether step-up is required (amount > $1,000).
4. If required, the use case returns `NeedsStepUp`; the ViewModel shows `StepUpSheet`, which asks for the MPIN; on a correct entry it retries with the one-time grant.
5. Repository runs one Firestore transaction: debit the source account, write the transaction with a client-generated UUID reference, write a transactional notification, and a low balance notification if the account crossed below $500.00.
6. Use case returns a typed `Result`. The ViewModel moves to the receipt state from that result, not from a guess.

State:
- Firestore is the source of truth. Repositories expose `Flow`s from snapshot listeners; screens update live.
- ViewModels expose one immutable `StateFlow<UiState>` per screen. UiState is a sealed type: Loading, Empty, Error, Content.
- Flow drafts (transfer in progress, application answers) live in a ViewModel scoped to the flow's nav graph, and in `SavedStateHandle` so process death does not lose them.
- Balance masking and session lock live in `SessionManager` (process-wide, in memory).
- Optimistic updates: none in v1. Firestore's local write latency is low enough.

Auth and session:
- First launch: Firebase anonymous sign-in, Welcome, then the login screen (customer ID + MPIN).
- `PersonaCatalog` validates the customer ID (exactly 8 alphanumeric characters, trimmed, case-insensitive) and maps it to a personaId. A wrong format or an unknown ID is an inline error; the MPIN is checked by `MpinVerifier` in `core/security`.
- On login, if `users/{uid}` holds a different personaId (or none), `DemoDataSeeder` deletes the subtree (in batches of 500) and writes the chosen persona from `assets/seed/personas/`, resolving relative dates and generating filler as described in DEMO_DATA.md section 01. The same customer ID keeps its data and is reseeded only if the bundled `seedVersion` is newer than the stored one.
- The remembered customer ID lives in DataStore. Log out (Profile) clears it and returns to the login screen; the Firestore data stays until a different ID logs in.
- Hidden Reset demo data (long-press the app version in Profile, then confirm) deletes the subtree and reseeds the current persona.
- The MPIN (`123456`, every persona) is the only factor: for login, the lock screen and step-up. No biometrics.
- `MpinVerifier` locks MPIN entry for 30 s after 5 wrong entries, counted across login, lock screen and step-up. The lockout expiry is stored in DataStore so restarting the app does not clear it.
- `SessionManager` locks the app after 30 min without touch input in the foreground or 300 s in the background (ProcessLifecycleOwner). Locked = navigation shows the lock screen (greeting with the remembered name, MPIN pad, "Not you?" back to full login); data listeners pause.

Deposit check clearing:
- A check deposit is written as a PENDING credit transaction with `clearsAt = createdAt + 30 s`; balances do not change.
- `ClearDueDepositsUseCase` (`feature/payments/domain`) runs in an injected application scope: on app start, on return to foreground, and on a timer for each pending deposit while the app is open. For each due deposit, one Firestore transaction sets it to POSTED, credits the account and writes a "Deposit cleared" notification.
- `MainActivity` sets `FLAG_SECURE`, so screenshots and the app switcher preview are blank.
- Device binding: `DeviceKeyManager` creates an EC P-256 key in Android Keystore on first launch and stores the public key in `users/{uid}/devices`.

Errors:
- Validation errors return to the form as field messages from string resources.
- Firebase and unexpected errors map to a domain error type in the data layer, are logged to Crashlytics with a non-PII key, and show a generic message with Retry.
- Never show stack traces, Firebase error codes or exception messages to the user.

## 05 Boundaries

Dependencies flow one way: `ui -> domain <- data`, and `feature -> core`.

Allowed:
- `ui` imports its own feature's `domain`, `core/designsystem`, `core/model`, `core/common`.
- `data` implements `domain` interfaces and imports `core/data/firebase`.
- `domain` imports only `core/model` and `core/common`. Pure Kotlin.
- Features talk to each other only through navigation routes or `core` interfaces.

Never:
- Composables import Firebase, DataStore, or repositories. They get state and lambdas only.
- `domain` imports Android framework classes or Firebase.
- One feature imports another feature's `data` or `ui` package.
- Raw colours, dimensions or font sizes outside `core/designsystem/theme`.
- `GlobalScope`, `runBlocking` in app code, or blocking I/O on the main thread.
- Logging account numbers, card numbers, balances, customer IDs or the MPIN, in Logcat or Crashlytics.

## 06 Decisions that look wrong but are intentional

The agent must not "fix" these.

- No backend and no Cloud Functions. Money movement runs as client-side Firestore transactions. This is a demo on a possibly free (Spark) Firebase plan; in production it would move server-side. Do not add Cloud Functions without approval.
- The MPIN is hardcoded to `123456` in `MpinVerifier`, per MobileBuild.md 1.1, for every persona. It is a demo lock, not a credential. Do not move it to Firestore or make it configurable.
- Step-up re-asks for the same MPIN; there is no OTP and no biometric (PRD 08). It is not a real second factor. Do not add AndroidX Biometric or an OTP without approval.
- The customer ID is not an account. It only picks which persona the install's anonymous user is seeded with, and logging in with another ID wipes the current data.
- Anonymous Firebase auth. Each install is its own demo customer with fresh seeded data; reinstalling resets the demo. There are no credentials to leak.
- No Cloud Storage. The project is on Spark, so statements and tax documents are generated on the device from Firestore data. `google-services.json` lists a default bucket; it stays unused (re-confirmed 2026-10-17), and there are no Storage rules.
- Check deposits clear on the client after 30 s, not on a server. If the app is closed, they clear on next open.
- The client writes its own seed data. Rules allow a user to write their own tree, which a real bank would never do; it keeps the demo self-contained.
- Seed data is bundled JSON with relative dates and a fixed random seed, not live Firestore data shared between devices. Every install of a persona looks identical and always current.
- Full card numbers and CVVs are stored in the `cards` documents. They are fake (DEMO_DATA.md section 01) and this is what makes Reveal card details work without a card processor.
- `google-services.json` is gitignored even though it is not strictly a secret, so the client's project config does not end up in shared repositories.
- Single Gradle module. Multi-module would slow the demo build for no benefit at this size. Feature packages keep the same boundaries a module split would.
- Portrait lock and `FLAG_SECURE` on the whole app, including non-sensitive screens. Simpler and matches the "mask on app switcher" requirement.
- Money as `Long` cents, never `Double` or `BigDecimal` in models.
- `AndroidManifest.xml` removes components and permissions that libraries merge in: Firebase Auth's exported `GenericIdpActivity`, `RecaptchaActivity` and `RevocationBoundService` (anonymous sign-in needs none of them, and only the launcher Activity may be exported), and from Analytics the advertising permissions `AD_ID`, `ACCESS_ADSERVICES_AD_ID` and `ACCESS_ADSERVICES_ATTRIBUTION`, the Play install-referrer permission and `READ_GSERVICES`. `WAKE_LOCK` is kept so Analytics and Firestore can finish background uploads. Do not remove these `tools:node="remove"` entries. After any dependency change, check the merged release manifest still exports only `MainActivity`.

## 07 Scalability and future considerations

- Expected scale for v1: a handful of demo devices, one persona per install, a few hundred documents per user. Build for this, not for 100x.
- Known limits: no server-side validation beyond Security Rules; balances can be edited by a determined user of their own anonymous account; no real-time fraud logic.
- Planned next (only if the demo becomes a pilot): move transfers, fees and approvals to Cloud Functions; App Check with Play Integrity; FCM push for transactional alerts; real Phone Auth OTP.
- Not planned: iOS, multi-tenant, multi-currency, offline-first writes. Do not add abstractions for these.

## 08 Firebase setup (one-time, by a human)

1. In the Firebase console for `abc-banking-181019`, register an Android app with the application ID `com.abcbank.mobile` (PRD section 08) and the debug SHA-1 from `gradlew signingReport`.
2. Download `google-services.json` into `app/`.
3. Enable Authentication > Sign-in method > Anonymous.
4. Create Cloud Firestore (production mode). Cloud Storage is not used (Spark plan).
5. Deploy rules and indexes with the Firebase CLI: `firebase deploy --only firestore --project abc-banking-181019`.
6. Create the `catalog/*` documents (billers, products, faqs) from `app/src/main/assets/seed/catalog.json`. Clients can only read `catalog`, so this is done by a person in the console. If it becomes tedious, ask before adding an admin seeding script.

## 09 When the agent must stop and ask

If a task needs any of the following, stop, name the conflict, and propose
the smallest change that avoids it:
- Crossing a boundary in section 05
- Changing a decision in section 06
- Adding a Gradle dependency, a Firebase product, a Firestore collection or a permission in the manifest
- Changing `firebase/firestore.rules`, or adding Cloud Storage
- Changing the MPIN, session, step-up or device binding behaviour

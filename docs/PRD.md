# PRD: ABC Bank Mobile (Android demo)

<!--
Product Requirements Document. The first file your agent reads.
It answers what we are building, for whom, and what "done" looks like.
The detailed, screen-by-screen feature spec lives in MobileBuild.md.
This file decides scope; MobileBuild.md describes behaviour. If they
disagree, this file wins and the agent must ask.
-->

## 01 Product overview

| Field | Value |
| --- | --- |
| Product name | ABC Bank (app label "ABC Bank", wordmark "abcbank") |
| Tagline | Everyday banking, simplified, in your pocket. |
| Description | A native Android retail banking app built to demo ABC Bank's digital experience to a client. It simulates accounts, money movement, card controls, product origination and self-service, using seeded demo data in Firebase. No real money moves. |
| Stage | Client demo (clickable, data-backed prototype) |
| Platform | Android phones, portrait. Native Kotlin + Jetpack Compose, built in Android Studio. |
| Backend | Firebase project `abc-banking-181019` ([console](https://console.firebase.google.com/u/0/project/abc-banking-181019/overview)) |
| Feature spec | [MobileBuild.md](MobileBuild.md) |
| Visual reference | [Sample Screens 1.png](Sample%20Screens%201.png), [Sample Screens 2.png](Sample%20Screens%202.png) |
| Demo data | [DEMO_DATA.md](DEMO_DATA.md): four pre-loaded personas plus shared billers, banks, products and FAQs |

## 02 Problem

ABC Bank needs to show a prospective client what a modern retail banking app
looks and feels like on their customers' phones. Slide decks and static
mock-ups do not convey how flows like step-up authentication, instant
transfers or card freezing actually behave. The client wants to hold a phone,
log in, move money and see balances update.

Today, the sales team has to walk the client through static screens because
there is no working app. This costs them credibility in the pitch and makes
it hard to discuss real interaction details.

## 03 Goal

Give ABC Bank a working Android app that a presenter can demo end to end in
15 minutes, covering every area in MobileBuild.md, without touching a real
core-banking system.

## 04 Target users

Primary user:
- Who: the ABC Bank presenter (sales, product or solutions lead) running a live demo on an Android phone or emulator.
- Trigger: a scheduled client meeting or workshop.
- Today they use: static Figma screens and slides.
- They will switch because: the app is real, responsive and backed by data that changes when they act.

Secondary users:
- Client stakeholders who may be handed the phone. Every flow must be self-explanatory and impossible to break with ordinary taps.
- The demo personas inside the app (full data in DEMO_DATA.md), each chosen to show different flows:
  - Samanta Johns: affluent professional, every account type. The default demo and the person in the sample screens.
  - Marcus Lee: first job, low balance, frozen card, credit card due soon, applies for a loan.
  - Grace Whitfield: retiree with term deposits, investments, auto-pay bills and an active travel notice.
  - Daniel Okafor: high-net-worth business owner with raised check deposit and card limits.

Not for:
- Real ABC Bank customers. No production data, no real accounts, no real payments.
- iOS users. Apple Wallet is shown nowhere; Google Wallet only.

## 05 Core features

Must ship in v1. Details for each are in the matching MobileBuild.md section.

| # | Feature | What the user can do | Done when |
| --- | --- | --- | --- |
| 1 | Welcome and customer ID + MPIN login (1.1) | Open the app, tap Get Started, enter a demo customer ID (`SAMJOH01`, `MARLEE01`, `GRAWHI01` or `DANOKA01`) and MPIN `123456` | The persona for that customer ID (DEMO_DATA.md) is seeded and Home shows it. Unknown ID or wrong MPIN shows an inline error. 5 wrong MPINs lock entry for 30 s. Login never waits for the network. Each persona's data is written once per install and kept; logging in with another ID switches to that persona's data without deleting anything. Profile > Log Out returns to login. No biometric unlock. |
| 2 | Session security (1.1) | Leave the app idle or in the background | App re-locks after 30 min foreground idle or 300 s in background. The lock screen remembers the customer and asks for the MPIN only, with "Not you?" to log in with another ID. App switcher shows a blank screen, never balances. |
| 3 | Device binding (1.1) | First launch registers the device | A Keystore key pair exists and its public key is stored in Firestore under the user's devices. |
| 4 | Step-up authentication (1.2) | Add a beneficiary, send more than $1,000, change card limits, reveal card details | A step-up sheet asks for the MPIN again and the action only completes after a correct entry. Wrong entries count towards the same 5-attempt lockout. |
| 5 | Home and accounts overview (2.1) | See greeting, total balance, account cards, quick actions, hide/show balances | Every account type in DEMO_DATA.md renders from Firestore, and Marcus's missing investments show the empty state. Eye toggle masks every amount on screen and persists for the session. |
| 6 | Transactions (2.2) | Browse grouped history, search, filter, open a transaction, download a PDF receipt, start a dispute | Groups are Today, Yesterday, Last 30 days. Filters combine. Receipt PDF opens in the share sheet. Dispute creates a support case. |
| 7 | Internal transfers (3.1) | Move money between own accounts | Amount above available balance is blocked inline. Both balances update after confirm. |
| 8 | P2P and domestic transfers (3.2) | Pick or add a beneficiary, enter amount, choose Standard or Instant, review, confirm | All five steps work. Receipt shows a reference UUID. New transaction appears in history. |
| 9 | Bill pay (3.3) | Pick a biller, fetch a bill, pay now or schedule, toggle auto-pay | Fetched amount and due date display. Payment or schedule is saved and listed. |
| 10 | Card management and controls (4.1, 4.2) | View stacked cards, reveal PAN/expiry/CVV, freeze, set limits, toggle channels, add a travel notice, tap Add to Google Wallet | Every control persists to Firestore and survives app restart. Google Wallet shows a simulated success state. |
| 11 | Insights | See monthly spending by category (donut) and savings over time (line chart), as in the sample screens | Charts are computed from the user's transactions, not hard-coded. Decided 2026-10-20: spending is every debit, posted or pending, except transfers (Transfer and Transfer out; a P2P payment tagged with a category counts); periods Last 30 Days (default, the documented targets), This Month, Last Month; the top two categories plus Other, against the profile's monthly budget. The savings chart is the goal's weekly history; the message follows progress (reached, from 75% "almost", from 50% "more than halfway", otherwise "on your way"). |
| 12 | Product catalog and applications (5.1, 5.2) | Browse HYSA, premium card, personal loan; use the loan calculator; complete the 4-step application | EMI updates live with the sliders. Completed application creates the new product in the user's accounts. |
| 13 | Notifications (6) | Open the bell, switch between Transactional and Promotional | Transfers and low balance events create transactional alerts. Unread count shows on the bell. |
| 14 | Statements and documents (6) | Open monthly statements and the 1099-INT | PDFs are generated on the device from the user's Firestore data and render in-app. No Cloud Storage (Spark plan). |
| 15 | Support hub (6) | Chat with the support bot, browse FAQs, raise a dispute from a transaction | FAQ answers come from Firestore. Unmatched questions get a scripted hand-off reply. |
| 16 | Deposit check (2.3) | Enter an amount ($1.00 up to the persona's limit), pick a checking or savings account, optionally attach placeholder cheque images, confirm | A Pending credit appears in transactions, then posts and updates the balance 30 s later (on next app open if the app was closed), with a "Deposit cleared" alert. |

Key user flow (the demo script):
1. Presenter opens the app on the Welcome screen and taps Get Started.
2. Logs in as `SAMJOH01` with MPIN 123456 and lands on Home with the wallet balance ($155,832.00) and quick actions.
3. Sends $1,500 to a saved contact with Instant speed, passes step-up, sees the receipt.
4. Opens Cards, freezes the main card, reveals the card number after step-up.
5. Opens Insights to show where the money went this month.
6. Applies for a personal loan from the product catalog and sees it appear on Home.

## 06 Success metrics

| Metric | Target | Measured by |
| --- | --- | --- |
| Demo script completes without a dead end or placeholder screen | 100% of 15 features | Manual demo checklist on a Pixel emulator and one physical device |
| Crash-free demo rehearsals | 0 crashes in the last 5 rehearsals | Firebase Crashlytics |
| Cold start to Welcome screen | Under 2 s on a mid-range device (Pixel 6a class) | `adb shell am start -W` |
| First-time viewer can complete an internal transfer unaided | Under 60 s | Hallway test with 3 people |

## 07 Out of scope

Do not build these in v1, even if they look easy or obvious.

- Any real core-banking, card-processor, ACH, wire, credit bureau or KYC integration. Everything is simulated against Firestore.
- Any OTP, real (Firebase Phone Auth) or simulated. Step-up re-asks for the MPIN.
- Biometric unlock and biometric step-up. The MPIN is the only factor.
- Cloud Storage. The project is on the Spark plan; statements and tax documents are generated in the app.
- Real Google Wallet tokenisation. The button shows a simulated success state.
- Apple Wallet, iOS, tablets, foldable-specific layouts and landscape.
- Dark mode, localisation, and currencies other than USD.
- Push notifications (FCM). The notification center is in-app only.
- Cloud Functions and any server code. The app talks to Firebase directly (see ARCHITECTURE.md section 06).
- Multi-currency accounts and Withdraw (shown in the sample screens, not in MobileBuild.md).
- Real camera capture for check deposit. The deposit flow uses placeholder cheque images.
- Self-registration and real user accounts. Only the four personas in DEMO_DATA.md exist; adding a fifth needs approval.
- An admin screen or tool for editing demo data. Changes go through DEMO_DATA.md and the seed JSON.

## 08 Decisions and open questions

### Decided (2026-10-17)

These were open questions. They are now decided; the other docs follow them.

| Question | Decision |
| --- | --- |
| Android application ID | `com.abcbank.mobile`. Register it in the Firebase console before downloading `google-services.json`. |
| Unlock method | MPIN `123456` only, the same for every persona. No biometric unlock or enrolment. |
| Step-up method | Re-enter the MPIN in a step-up sheet. No OTP and no biometric. |
| MPIN lockout | 5 wrong entries lock MPIN entry for 30 s, then the count resets. Login, lock screen and step-up share one count. |
| Instant transfer fee | 1.5% of the amount, minimum $0.25, maximum $15.00. Standard ACH is free. |
| Deposit check | Enter amount, pick account, placeholder cheque images optional (confirm works without them). Pending credit, posted 30 s later with a "Deposit cleared" alert. |
| Fonts and card artwork | Open-licence substitutes from DESIGN_SYSTEM.md (Plus Jakarta Sans, gradient card art) now; swap in ABC Bank assets later without screen changes. |
| Firebase plan | Spark (free). No Cloud Functions, no Cloud Storage. Statement and 1099-INT PDFs are generated in the app. |
| Login and demo reset | Login with a per-persona customer ID (exactly 8 alphanumeric characters, e.g. `SAMJOH01`) plus MPIN; this replaces the demo profile picker. Each persona keeps its own data on the install; switching ID switches data and deletes nothing (revised 2026-10-20 so login never depends on Firestore writes). Lock screen asks for MPIN only. A "Reset Demo Data" row in Profile reseeds the current persona after a confirmation (visible rather than hidden, decided 2026-10-20). |
| Personas | Keep the three in DEMO_DATA.md with their names and stories, and add a fourth: Daniel Okafor (`DANOKA01`), high net worth, approved 2026-10-17. |
| Profile photos | Supplied 2026-10-17 in `images/`. Bundled as `drawable-nodpi/avatar_{personaId}.webp`; beneficiaries keep initials. |
| Low balance alert | Below $500.00: available balance for checking and savings, available credit for credit cards and lines of credit. Term deposits and investments never alert. Fires once when an account crosses below the threshold. |
| Credit decision tiers | As in DEMO_DATA.md section 07. |
| Deposit check limits | Checking and savings only. $1.00 to $10,000.00 per deposit; $100,000.00 for the high-net-worth persona. |
| High-net-worth limits | Daniel Okafor has a higher check deposit limit and higher card limits (DEMO_DATA.md section 06). The step-up threshold stays $1,000 for every persona. |

### Open

None at the moment. New questions go here with a proposed default in brackets; the agent must ask before assuming an answer.

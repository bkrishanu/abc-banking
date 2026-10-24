# Redesign: Next Steps

<!--
Hand-over for the Stitch redesign of ABC Bank Mobile, written 2026-10-24 at the end of stage 2a.
Read with docs/STITCH_REVIEW.md (decisions and the screen-by-screen checklist) and
docs/DESIGN_SYSTEM.md (current tokens and components). Update this file as stages finish.
-->

## 01 Where things stand

| Stage | What | Status |
| --- | --- | --- |
| Prompt pack | docs/STITCH_PROMPT.md, mockups in the Stitch project `2409467982313725814` | Done |
| Review | docs/STITCH_REVIEW.md: all 41 Stitch screens checked, decisions recorded | Done |
| 1 | Tokens: palette, Plus Jakarta Sans, radii, 56dp buttons | Merged (`ae3e73a`) |
| 1 fix | PDFs embed Plus Jakarta Sans through static Regular and Medium files | Merged (`b25a2c2`) |
| 2a | Docked bottom bar, floating Transfer button, title and greeting top bars with a shared bell, step progress bar | Merged (`bea85e5` and this file) |
| 2b | Content components | Next |
| 3 | Screens, one tab or flow per branch | After 2b |

Working agreement for every stage: list the files and wait for a go-ahead when there are more than five; one
branch per stage; run `assembleDebug`, `ktlintCheck`, `lint`, `testDebugUnitTest` and `verifyPaparazziDebug`;
look at every re-recorded golden; check on the emulator; commit, then stop for review. Push and merge only when asked.

## 02 Stage 2b: content components

Branch `redesign/stage-2b-components`. Expected files (confirm before starting):

| Component | File | Change |
| --- | --- | --- |
| `AbcButton` | `core/designsystem/component/AbcButton.kt` | Pill shape (radius-full); Secondary as a terracotta tint with `accentDeep` text, as in Stitch |
| `AbcTextField` | `AbcTextField.kt` | 16dp radius, `border` outline, 2dp primary border when focused |
| Quick actions | `ActionPill.kt`, `IconPillButton.kt` | Round tonal icon buttons with a label (Stitch Home: Send Money, Pay Bills, Deposit Check, More) and a small disabled badge while offline (adopted extra) |
| Card art | `BankCardArt.kt`, `BankCardView.kt` | Stitch gradients; frozen card drawn desaturated with an Unfreeze banner (adopted extra) |
| Rows | `TransactionRow.kt`, account rows | Tonal 40dp icon circles, rate chips (APY, APR) |
| Keypads | `MpinPad.kt`, `AmountKeypad.kt` | Stitch key shape and spacing |
| Badges | `StatusBadge.kt` | Stitch colours, Title Case labels |
| Feature tiles | `FeatureTile.kt` | Fix: labels wrap between words only (they break mid-word at 200% font today) |
| Card stack | `CardsScreen.kt` stack | Fix: peeking cards' name and balance line is clipped at 200% font |
| States | `ScreenStates.kt` | Offline banner with sync time |
| Wordmark | `AbcWordmark.kt` | Mark in terracotta |
| Launcher icon | `res/drawable/ic_launcher_*.xml` | Stitch colours (still v1 peach and black) |
| Strings, docs | `strings.xml`, docs/DESIGN_SYSTEM.md section 06 | New labels, component specs |

Home's own Transfer pill duplicates the floating Transfer button until Home's quick actions are replaced here.

Decide with the user before starting: whether the Home balance card extras (Discreet Mode banner, low balance
warning inside the card) are built as components in 2b or inside `HomeScreen` in stage 3.

## 03 Stage 3: screens

One branch per tab or flow, in this order, each using the row for that screen in docs/STITCH_REVIEW.md section 03
("Take from Stitch" and "Fix or drop"):

1. Home (all four personas, balances hidden, loading, error, offline)
2. Cards (card revealed, controls, travel notice sheet)
3. Insights
4. Send Money flow (recipient, add payee, amount, speed, review, receipt) and Between My Accounts
5. Pay Bills and Deposit Check
6. Transactions and Transaction Detail
7. Notifications, Statements and Documents, Help and Support
8. Explore Products and the application flow
9. Welcome, Login, Lock, Step-up sheet
10. Profile

Never carry over from the mockups: real brands or regulatory claims, contact photos, concierge or private banker
features, biometrics, invented data. The full list is docs/STITCH_REVIEW.md section 02.

## 04 Other open items

- **Seed dates (investigate after stage 2, read-only first).** On the emulator, Samanta's day-0 transactions show
  "Oct 20, 11:57 pm" instead of 3:14 pm, 11:40 am and 8:09 am (DEMO_DATA.md section 03); other days look right.
  The data was seeded on Oct 20 and kept. Find the cause, then ask before changing seeding.
- **TalkBack on a real phone.** Labels were checked through the accessibility tree; listen once with TalkBack on.
- **Add New Payee shows "Step 1 of 5"** in its screenshot test. Check whether that is intended.

## 05 Emulator notes

- The app sets `FLAG_SECURE`, so `adb screencap` is black. Use the emulator's own capture:
  `adb emu screenrecord screenshot <folder>`.
- 360dp check on the Pixel emulator (1080 px wide): `adb shell wm density 480`; 200% text:
  `adb shell settings put system font_scale 2.0`. Reset with `adb shell wm density reset` and `font_scale 1.0`.
- Logins: `SAMJOH01`, `MARLEE01`, `GRAWHI01`, `DANOKA01`, MPIN `123456`.
- Generated PDFs are in the app cache (`cache/documents`, `cache/receipts`); read them with
  `adb exec-out run-as com.abcbank.mobile cat <path>`.

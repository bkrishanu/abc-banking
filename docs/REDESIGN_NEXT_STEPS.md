# Redesign: Next Steps

<!--
Hand-over for the Stitch redesign of ABC Bank Mobile. First written 2026-10-24 (end of stage 2a), rewritten
2026-10-25 at the end of the day (Home, Cards and the seed fix merged). Read with docs/STITCH_REVIEW.md (decisions
and the screen-by-screen checklist) and docs/DESIGN_SYSTEM.md (current tokens, components and screen layouts).
Update this file as stages finish. Section 06 is the prompt to start the next session with.
-->

## 01 Where things stand

`main` is pushed and matches GitHub. Open branch: `redesign/stage-3-pay-bills` (committed, waiting for review).

| Stage | What | Status |
| --- | --- | --- |
| Prompt pack | docs/STITCH_PROMPT.md, mockups in the Stitch project `2409467982313725814` | Done |
| Review | docs/STITCH_REVIEW.md: all 41 Stitch screens checked, decisions recorded | Done |
| 1 | Tokens: palette, Plus Jakarta Sans, radii, 56dp buttons | Merged (`ae3e73a`) |
| 1 fix | PDFs embed Plus Jakarta Sans through static Regular and Medium files | Merged (`b25a2c2`) |
| 2a | Docked bottom bar, floating Transfer button, title and greeting top bars with a shared bell, step progress bar | Merged (`bea85e5`) |
| 2b-1 | Pill buttons, text fields, keypads, badges, wordmark, launcher icon, tint tokens | Merged (`6c80f0b`, `025c13b`) |
| 2b-2 | Quick actions, frozen card, tonal category icons, rate chip, feature tiles, card stack, offline banner | Merged (`364a783`) |
| 3 Home | Hidden-balances and travel notice banners, balance card with account and low balance warning, one card per account | Merged (`3349757`) |
| Seed fix | Later-today seed items stay on the seed day when seeded just after midnight | Merged (`cfbff05`) |
| 3 Cards | Card actions row, Recent Activity, section cards, travel notice range calendar; Quick Transfer removed | Merged (`e98de74`) |
| 3 Insights | Period tabs above the cards, Spending card with left/over chip and breakdown rows, Top Categories, Stitch savings card | Merged (`fa09770`) |
| 3 Send Money | Payee search and cards, Stitch-order amount step with swap, speed cards with totals, review hero with Edit and Cancel, receipt with copy and new balance | Merged (`b37644a`) |
| 3 Pay Bills and Deposit | Total Due summary, bill cards with due badges, Recently Paid, deposit keypad panel, image rows, availability note | Committed on `redesign/stage-3-pay-bills` |
| 3 Transactions | | **Next** |

## 02 Working agreement

- Before starting a stage, read its row in docs/STITCH_REVIEW.md section 03, look at the Stitch mockup, and ask the
  user about every open choice (layout options, extras not in the spec, copy) with a recommendation.
- List the files and wait for a go-ahead when there are more than five (AGENTS.md section 02).
- One branch per stage (`redesign/stage-3-<screen>`); fixes get their own branch.
- Run `assembleDebug`, `ktlintCheck`, `lint`, `testDebugUnitTest` and `verifyPaparazziDebug`. Run `ktlintFormat` on
  its own first (in the same Gradle call as `ktlintCheck` it races and fails).
- Look at every re-recorded golden, including the 200% font ones.
- Check on the emulator: all four personas, 360dp at 200% font, balances hidden.
- Commit, then stop for review. Push and merge only when the user asks; after merging, delete the branch.
- Update docs/DESIGN_SYSTEM.md (section 06 components, section 10 layouts) and this file in the same branch.
- New icons are Lucide v1.59.0, fetched from `https://unpkg.com/lucide-static@1.59.0/icons/<name>.svg` and written as
  `res/drawable/ic_<name>.xml` with stroke 1.75 (a `rect` becomes an equivalent path).

## 03 Stage 3: remaining screens

One branch per tab or flow, in this order. Each uses the row for that screen in docs/STITCH_REVIEW.md section 03
("Take from Stitch" and "Fix or drop"):

1. ~~Home~~ (done)
2. ~~Cards~~ (done)
3. ~~Insights~~ (done)
4. ~~Send Money flow~~ (done)
5. ~~Pay Bills and Deposit Check~~ (done)
6. **Transactions and Transaction Detail** (next) (recipient, add payee, amount, speed, review, receipt) and Between My Accounts
7. Notifications, Statements and Documents, Help and Support
8. Explore Products and the application flow
9. Welcome, Login, Lock, Step-up sheet
10. Profile

Never carry over from the mockups: real brands or regulatory claims, contact photos, concierge or private banker
features, biometrics, invented data. The full list is docs/STITCH_REVIEW.md section 02.

## 04 Decisions so far (2026-10-25)

- Quick actions stay usable while offline; the wifi-off badge is a cue only.
- Home: "Balances are hidden" banner with Show (not Discreet Mode wording); Grace's active travel notice banner with
  Manage (opens Cards); low balance warning inside the balance card (hidden with the balances); one card per account
  with a tonal type icon, an APY or APR chip and "Due in N Days" for cards. Not added: the 1099-INT ready card.
- Quick Transfer: added to Home, then removed from both Home and Cards (user decision). The transfer flow keeps its
  own payee list.
- Cards: Stitch's card actions (Details, Freeze, Limits, Wallet) replace Send, Deposit and More, the Freeze switch
  card, the Show Card Details button and the Google Wallet card; Recent Activity shows the card account's 3 newest
  transactions from the last 30 days; the travel notice sheet uses a range calendar (`AbcDateRangePicker`).
- Insights: the period tabs sit above both cards and drive Spending and Top Categories (not the savings goal); the
  Spending card follows Stitch (left/over chip, large total, donut showing the budget %, breakdown rows); Top
  Categories shows the period's 4 biggest categories with their 2 biggest merchants and a share bar; the Savings card
  follows Stitch (% Reached chip, amount of target, progress panel, then the chart). Dropped: the AI insight card.
- Send Money: payee search (name or bank); payee cards with a Recent badge; the amount step in Stitch order with the
  keypad last (it needs a scroll on P2P); swap between own accounts; speed cards each with Total Deducted; Review with
  Edit Transfer Details and Cancel Transfer under the card; Receipt with Copy for the reference and the From account's
  new balance. Not added: sharing the receipt from the Receipt step. Add New Payee keeps "Step 1 of 5".
- Pay Bills: paying still starts from the bill screen (no Pay Now on the list); due badges turn Warning for a due bill
  within 7 days (new rule `BillRules.DUE_SOON_DAYS`); paid bills sit under Recently Paid. Deposit Check: amount and
  keypad in one outlined panel, cheque images as rows, and an availability note with the real 30-second clearing.
- Badges use an opaque tint, a tone dot and the label in `text` (warning text on its own tint fails AA).

## 05 Open items

Items left for the end of the overall build are in docs/END_OF_BUILD_FIXES.md (decided 2026-10-25). Add new ones
there instead of fixing them inside a screen stage.

## 06 Prompt to start the next session

Copy this into Claude Code at the start of the next session:

```text
We are continuing the Stitch redesign of ABC Bank Mobile. Read AGENTS.md, then docs/REDESIGN_NEXT_STEPS.md
(where things stand, the working agreement and decisions), docs/END_OF_BUILD_FIXES.md, docs/STITCH_REVIEW.md section 03, and
the Transactions parts of docs/DESIGN_SYSTEM.md (sections 06 and 10) and docs/MobileBuild.md 2.2.

Start stage 3, Transactions and Transaction Detail:
1. Confirm main is clean and matches GitHub, and that redesign/stage-3-pay-bills has been merged. Read
   docs/END_OF_BUILD_FIXES.md; add new end-of-build items there.
2. Look at the Stitch mockups (project 2409467982313725814, rows 23 and 24 of the review, and 41 for offline)
   and the current transaction screens, then ask me about every open choice with your recommendation before
   changing anything.
3. List the files you expect to touch and wait for my go-ahead.
4. Build it on redesign/stage-3-transactions, run the five checks, look at every golden, check all four personas
   on the emulator (also 360dp at 200% font and balances hidden), update the docs, commit, and stop for review.
Do not push or merge until I ask.
```

## 07 Emulator notes

- The app sets `FLAG_SECURE`, so `adb screencap` is black. Use the emulator's own capture:
  `adb emu screenrecord screenshot <folder>`. In Git Bash set `MSYS_NO_PATHCONV=1` so `/sdcard` paths survive.
- 360dp check on the Pixel emulator (1080 px wide): `adb shell wm density 480`; 200% text:
  `adb shell settings put system font_scale 2.0`. Reset with `adb shell wm density reset` and `font_scale 1.0`.
- Logins: `SAMJOH01`, `MARLEE01`, `GRAWHI01`, `DANOKA01`, MPIN `123456`. `adb shell pm clear com.abcbank.mobile`
  returns to Welcome; right after an install, wait a few seconds before driving the app.
- The emulator runs on IST. Seeding just after midnight is handled now; if day-0 times still look odd, use
  Profile > Reset Demo Data.
- Generated PDFs are in the app cache (`cache/documents`, `cache/receipts`); read them with
  `adb exec-out run-as com.abcbank.mobile cat <path>`.

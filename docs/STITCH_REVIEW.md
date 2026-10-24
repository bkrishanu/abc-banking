# Stitch Redesign Review: ABC Bank Mobile

<!--
Review of the Google Stitch mockups against the app, the PRD, MobileBuild.md and DEMO_DATA.md.
Done 2026-10-24 (read-only pass over all 41 screens). This is the checklist for stages 2 and 3
of the redesign. Tokens (stage 1) are already in docs/DESIGN_SYSTEM.md.
-->

Stitch project: "Markdown Project Runner", id `2409467982313725814` (private). Theme: "Warm Private Banking".
Prompts: docs/STITCH_PROMPT.md.

## 01 Decisions (2026-10-24)

| Topic | Decision |
| --- | --- |
| Source of truth | The Stitch project theme. The "Figma Hand-Off & Architecture Guide" screen's values are not used. |
| Contrast | A Stitch colour that fails WCAG AA is darkened to the nearest passing shade from the same palette and recorded in DESIGN_SYSTEM.md section 02. |
| Personas | One light theme for all four. No "Obsidian Private Wealth" dark tier for Daniel; dark mode stays out of v1 (PRD section 07). |
| Tone | No "Private", "Private Client", "Private Wealth" or "Private Vault" wording. Warm retail banking. |
| Navigation | Four tabs (Home, Cards, Insights, Profile) plus a floating Transfer button above the bar that opens Send Money. Not the 5-slot bar on the Stitch Insights screen. |
| Header | Home: avatar, "Hello, Samanta", bell. Other tabs: screen title and bell. No "abcbank Private / Accounts Hub" bar. |
| Off-spec content | Dropped: initials instead of contact photos, only DEMO_DATA.md values, placeholder cheque images instead of a camera. |
| Adopted extras | Discreet Mode banner with Reveal while balances are hidden; low balance warning inside the Home balance card; frozen card drawn desaturated with an Unfreeze banner; a small disabled badge on quick actions while offline. |
| Stages | 1: tokens and font (done). 2: shared components. 3: screens, one tab or flow per branch. Each stage on its own branch, committed, pushed only when asked. |

## 02 Cross-cutting issues

Every item applies to all the screens it appears on. Letters are used in section 03.

**A. Real brands and regulatory claims (remove).** "RTP®" and "The Clearing House"; "TransUnion VantageScore® 3.0";
Spotify, Swiss Air, Swiss International Air Lines, Sotheby's, Blue Bottle, Aesop, Lloyd's; "Member FDIC",
"Equal Housing Lender", "FDIC insured up to $250,000", "Electronic Fund Transfers Act". Routing number 071000288
on Add New Payee looks real; use the catalog numbers (500000021 and so on).

**B. Photos.** Stock photos of contacts, bankers and advisors, merchants and cheques. Contacts use `InitialsAvatar`.
The persona's own photo (`avatar_{personaId}`) is fine.

**C. Features not in scope.** Each needs approval as its own task, not part of the redesign: concierge or private
banker (Julian Brooks, "advisor Marcus"), banker phone line and Call, Send Wire, email and push receipts,
Notification Preferences, paperless delivery, Forgot MPIN, Change MPIN, recurring or dated internal transfers,
payee nickname and account type, favourites, payee filter chips, camera capture, Lost or Stolen, Lock all cards,
1099-B, quarterly statements, itemised merchant receipts, micro-investing, the AI advisory insight card (v2),
Pay Selected Bills, custom bill amounts, editing or deleting travel notices, transaction CSV export, an
"All Accounts" filter, credit card Pay Now (no card payment flow exists).

**D. Security.** No mention of biometrics anywhere (MPIN is the only factor). No "256-bit" or "Zero Liability"
guarantees. No copy-to-clipboard for the card number or customer ID. The lock screen shows no customer ID or
account number. Hidden balances mask every amount, limits included. No wording that suggests offline payments are
queued. Session timeouts are 30 minutes idle and 300 seconds in the background (Stitch Profile has them reversed).

**E. Data.** Use DEMO_DATA.md values only. Stitch got wrong: account last-4s on most persona screens; dates
(maturities in 2025, transaction days); Instant fee label (it is 1.5%, at least $0.25, at most $15.00); Marcus's
payees (only Sarah Lee); Grace's travel notice (Toronto, Canada, not Lisbon); the card product name (ABC Signature
Rewards Card); billers outside the catalog (the water biller is City Water & Sewer); Samanta's alerts showing
Marcus's $412.17; "Samantha" spelling; Samanta's email and phone on the application.

**F. Copy.** Title Case labels, sentence case messages (DESIGN_SYSTEM.md section 01). Stitch uses all caps for
labels (WALLET BALANCE, STEP 2 OF 5, PENDING, TODAY, section headers) and jargon ("Select authorized routing
protocols", "Tailored Financial Horizons").

**G. Layout bugs.** Two stacked headers on every tab and on sub-screens; the 5-slot bar on Insights; Stitch
preview toggles baked into screens ("Show Error", "Active Entry / Wrong MPIN Alert", "Send Money / Reveal Card",
"Regular Record / Disputed Preview"); blank Quick Transfer and Accounts areas on Home with balances hidden;
the Statements title overlapping its help icon; Pay Bills titled "Send Money"; the More Actions sheet cut off.

## 03 Screen by screen

| # | Stitch screen | App code | Take from Stitch | Fix or drop |
| --- | --- | --- | --- | --- |
| 1 | Welcome | `WelcomeScreen` | Fanned card hero, headline, pill Get Started | A, value-prop strip ("Zero Fees" is false), "Already have an account? Log In", invented card numbers, caps |
| 2 | Private Banking Mobile Flow | — | Second Welcome variant | Not used; #1 is the reference |
| 3 | Log In | `LoginScreen` | ID field with valid tick, MPIN dots, large keypad | G toggle, Forgot link (C), "Log In Securely" button (the pad submits on the sixth digit), lockout text during normal entry |
| 4 | Lock Screen | `LockScreen` | Avatar with lock badge, Welcome back, Not You? | D (ID and account shown), Forgot MPIN (C), G tabs, encryption line |
| 5 | Confirm It's You | `StepUpSheet` | Sheet layout, amount in the reason line, "Never share your 6-digit MPIN with anyone." | G tabs, "256-Bit" chip |
| 6 | Home | `HomeScreen` | Teal balance card, In/Out chips, round quick actions, Quick Transfer initials, account cards with tonal icon circles and APY chips, Features carousel | G header, E ("Private Vault", "+12.4% YTD", "Prime + 1.2%", "Managed Advisory", "Maturing Nov 2025") |
| 7 | Home (Balances Hidden) | `HomeScreen` | Discreet Mode banner with Reveal (adopted) | D (limit shown), G blank sections, "Bespoke Services" promo |
| 8 | More Actions Sheet | `MoreActionsSheet` | Tile grid with tonal icons | G cut off, "Dedicated Advisory", "24/7" |
| 9 | Cards | `CardsScreen` | Stacked 3D cards, Details / Freeze / Limits / Google Wallet circles | B, G header |
| 10 | Card Revealed | `CardsScreen` | Full number in Mono, "Hidden again when you leave this screen." | D (copy button, biometric line), invented channel and status labels |
| 11 | Card Controls | `CardsScreen` (inline) | Section cards, value text on sliders, switch rows with subtitles | "Primary Wealth Debit", "Standard limit", minimums, freeze confirmation must stay a dialog, C (edit or delete notices), FX waiver, 2025 dates |
| 12 | Add Travel Notice | `TravelNoticeSheet` | Range calendar, character hint | Multi-card picker, Contact While Traveling, promo; stays a sheet |
| 13 | Insights | `InsightsScreen` | Period tabs, donut against budget, savings line chart, top categories with progress bars | G 5-slot bar, merged categories, AI insight card (v2), caps |
| 14 | Send Money: Choose Recipient | `TransferRecipientScreen` | Between My Accounts row, payee rows with bank and last 4 | B, C (filters, stars), E (Oakwood and Daniel Kim banks) |
| 15 | Add New Payee | `TransferRecipientScreen` | Bank shown once found, counter, MPIN note | A (routing number), C (type, nickname), location |
| 16 | Send Money: Amount | `TransferAmountScreen` | Payee chip, From card, large amount, memo counter, category chips | "$5,000 daily limit", "biometric", step-up "at step 5" (it is at Review), "Automatic Tagging" |
| 17 | Send Money: Payment Speed | `TransferAmountScreen` | Two selectable speed cards with totals | E fee label, A, urgency upsell, guarantees |
| 18 | Send Money: Review | `TransferReviewScreen` | Summary card, total row, Edit and Cancel | A; add Estimated Delivery; disclaimer stays in the Agree and Send sheet |
| 19 | Send Money: Receipt | `TransferReviewScreen` | Check badge, ticket receipt, reference with copy, new balance | Email and push line (C), "Official Voucher" |
| 20 | Between My Accounts | `TransferAmountScreen` | From/To with swap, inline maximum message | E (line of credit ends 6608), C (date, recurring), error only above the amount owed |
| 21 | Pay Bills | `BillPayScreens` | Total due summary, due badge, Auto-Pay chip, Pay From card | G title, C (multi-select, custom amount), E billers, empty icon circle |
| 22 | Deposit Check | `DepositCheckScreen` | Account picker, keypad, limit hint | B and C (camera), "$25,000 daily limit", "1 business day" (it clears in 30 s) |
| 23 | Transactions | `TransactionsScreen` | Search, type tabs with count, date and category chips, grouped list | Groups Today / Yesterday / Last 30 Days, E dates, C (account chip, CSV) |
| 24 | Transaction Detail | `TransactionDetailScreen` | Header with status and category chips, details table | G toggle, Zero Liability, C (location, itemised receipt, photo), Download Receipt is Secondary |
| 25 | Notifications | `NotificationsScreen` | Tabs with counts, unread dots, Mark All as Read | A, E, "Card Limits Updated" is not an alert type, All Caught Up only when nothing is unread, C (preferences) |
| 26 | Explore Products | `ProductsScreen` | Product cards with a large rate, loan calculator, "Fictional demo data." note | A and E (product name, FDIC), term slider in 6-month steps (not 12/24/36/48/60 chips), "Apply in 2 Mins" |
| 27 | Application Flow | `ApplicationScreens` | Stepper, read-only identity fields, employment chips | E (email, phone), C (address, housing payment, purpose, Save and Finish Later), soft-inquiry claim |
| 28 | Statements and Documents | `DocumentsScreen` | Latest statement preview, monthly rows, tax section | G overlap, C (1099-B, quarterly, paperless), "cryptographically sealed" |
| 29 | Help and Support | `SupportScreens` | Search, tiles, open case card, chat bubbles, FAQ accordion | C (Lost or Stolen, Banker Call, phone line), A footer, suggestion chips that jump to other screens |
| 30 | Profile | `ProfileScreen` | Profile card, credit score gauge, grouped rows, Reset Demo Data, Log Out | A, C (Change MPIN), D timeouts, "Member since", Credit Insights, version and FDIC footer |
| 31 | Home - Marcus Lee | `HomeScreen` | Low balance warning in the balance card (adopted), Due in 3 days pill, investments empty state | E (payees, ledger, savings, transactions), C (Pay Now, micro-investing) |
| 32 | Cards - Marcus Lee | `CardsScreen` | Desaturated frozen card and Unfreeze banner (adopted), channel switches | A (Spotify), B and C (concierge), "Private Client Portfolio" |
| 33 | Home - Grace Whitfield | `HomeScreen` | Travel notice banner, 1099-INT ready card | E (Toronto, last-4s, pension and earnings figures), C ("advisor Marcus") |
| 34 | Home - Daniel Okafor | `HomeScreen` | Seven-figure amounts fit | C (Julian Brooks, Send Wire, Concierge), A, E (last-4s, ledger, 4.60%, dates) |
| 35 | Home - Daniel (Obsidian) | — | — | Not used (decision: one theme) |
| 36 | Home (Loading State) | `ScreenStates` | Skeleton matching the layout | G header |
| 37 | Home (Error State) | `ScreenStates` | Error card, "We couldn't load your accounts.", Try Again | E spelling, C (banker, offline services), "completely safe" |
| 38 | Home (Offline State) | `ScreenStates` | Offline banner with sync time, disabled badges on quick actions (adopted) | E, A, D (queued instructions, biometric) |
| 39 | Cards (Loading State) | `ScreenStates` | Skeleton | — |
| 40 | Cards (Error State) | `ScreenStates` | Error card with Try Again | C (Lock all cards, network status), "cards keep working" |
| 41 | Transactions (Offline State) | `TransactionsScreen` | Offline banner, offline search hint | "Cached copy" chip on every row, invented channel labels |

## 04 Stage 2 components (from the screens above)

`AbcButton` as pills; `AbcTextField` with a 16dp radius and 2dp primary focus border; `AbcBottomBar` with the
floating Transfer button; `GreetingTopBar` and a title top bar; the balance card with in-card warning and Discreet
Mode banner; `BankCardArt` in the new gradients and the frozen treatment; round quick action buttons with an
offline badge; account rows with tonal icon circles and rate chips; `TransactionRow` icon tints; `MpinPad` and
`AmountKeypad` keys; `FlowScaffold` step indicator with a progress bar; `StatusBadge` colours; `AbcWordmark`
mark in terracotta; launcher icon colours.

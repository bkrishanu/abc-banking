# Stitch Prompt Pack: ABC Bank Mobile Redesign

<!--
Prompts for Google Stitch to explore a full visual redesign of ABC Bank Mobile.
This is design exploration only. It does not change docs/DESIGN_SYSTEM.md, the PRD
or the app. If a direction is adopted, DESIGN_SYSTEM.md and the Kotlin token files
are updated together, as that file requires.
Created 2026-10-24.
-->

## 01 How to use this with Stitch

1. Start a new Stitch project in **Mobile** mode.
2. Paste the **Global brief** (section 02) as the first prompt and generate the Welcome screen with it (prompt 4.1).
3. Generate the remaining screens one prompt at a time, in the order below. Start each one with
   "Same app and visual style as before." so Stitch keeps the theme.
4. Change one thing per follow-up prompt ("Make the balance card taller", "Use the softer green for incoming amounts").
   Stitch drifts when a follow-up asks for several changes at once.
5. Generate the persona variants (section 06) after their base screens look right.
6. Treat section 07 (dark mode, AI insights) as v2 concepts in a separate Stitch project or page, so nobody
   mistakes them for v1 scope.
7. Export to Figma, then review against the guardrails in section 08 before showing anything to the client.

## 02 Global brief (paste first)

```
Design a native Android mobile banking app called "abcbank" (ABC Bank) for retail customers in the United States.
This is a full redesign. Use Material 3 Expressive as the foundation, with a custom brand on top.

Mood: trustworthy and warm. Calm, human and reassuring, never cold or corporate. The customer should feel
their money is safe and that the bank is on their side. Think of a friendly private banker, not a trading app.

Brand: propose a new identity. A lowercase "abcbank" wordmark with a simple, ownable mark. Pick a warm,
trustworthy palette: a deep, confident primary (for example a deep teal, forest green or warm navy), a warm
secondary (for example terracotta, apricot or honey), soft warm neutrals for backgrounds (cream or warm
off-white, not pure grey), and clear semantic colours: green for money in and success, a warm amber for
warnings, a clear red for errors and destructive actions. Money direction is never shown by colour alone:
always pair it with a + or - sign or an arrow.

Typography: one modern, humanist sans-serif with tabular figures for every amount (for example Inter, Manrope
or Plus Jakarta Sans). Large, light balance figures with smaller cents. Clear hierarchy: screen title,
section header, body, caption.

Shape and depth: generous rounded corners (Material 3 Expressive shape scale), soft layered surfaces, and
subtle frosted-glass panels for floating elements (bottom navigation, sheets, the balance header over imagery).
Use glass sparingly so text stays readable. Bank cards are rendered as rich, tactile 3D objects with soft
lighting and gradient artwork.

Motion (annotate in the design): shared-element transitions from a card or account to its detail, springy
Material 3 Expressive motion on buttons and toggles, balances that count up when revealed, charts that draw
in, a gentle success animation on receipts, haptic feedback on MPIN keys and on confirm actions.

Layout: phone portrait, 360dp wide frames (also check 412dp). 16dp side margins. Single column.
Touch targets at least 48dp. Edge-to-edge with safe status and navigation bar areas.

Accessibility: WCAG AA contrast (4.5:1 for text). Layouts must still work at 200% font size: amounts never
clip or split across lines. Every icon button has a text label or description.

Copy rules: plain, short, warm. No exclamation marks. Labels, buttons, tabs and section headers are in
Title Case ("Wallet Balance", "Pay Bills", "View All"). Sentences, messages and errors are in sentence case
("We couldn't load your accounts."). US English, US dollars only.

Security feel: balances can be hidden with an eye toggle (shown as "••••••"). Card numbers are always masked
("•••• 3144") unless the customer has re-entered their MPIN to reveal them. Sign-in is a customer ID plus a
6-digit MPIN; there is no fingerprint, face unlock or SMS code anywhere in the app.

Sample data is fictional. The default customer is Samanta Johns, a 34-year-old product design lead.
```

## 03 Information architecture (paste second)

```
Propose the navigation for this app. You may change the current model, which is a floating bottom bar with
four tabs: Home, Cards, Insights, Profile. Options worth exploring: a central "Move Money" action (Send,
Pay Bills, Deposit Check, Between My Accounts), or a fifth tab. Show your recommended model on the Home screen.

Keep these fixed:
- Sign-in is customer ID plus 6-digit MPIN. Re-entering the MPIN in a bottom sheet ("step-up") is required
  to add a payee, send more than $1,000, change card limits and reveal card details.
- Flows that leave the main tabs (transfers, bill pay, applications) hide the bottom navigation and show a
  back arrow and a step indicator ("Step 2 of 5").
- There is no Withdraw action and no multi-currency accounts.
- Every screen that loads data has loading (skeleton), empty, error and offline states.
```

## 04 Screen prompts

Start each prompt with "Same app and visual style as before." Data is Samanta's unless a prompt says otherwise.

### Onboarding and Auth

**4.1 Welcome**
```
Welcome screen. The abcbank wordmark at the top. A large, warm hero: a fanned stack of three 3D bank cards
(warm peach, sage green, graphite) floating with soft shadows. Headline "Effortless banking, simplified."
A short supporting line. One full-width primary button "Get Started".
```

**4.2 Login**
```
Login screen. Title "Log In". A Customer ID field (8 letters or numbers, shown upper case, example SAMJOH01)
with its label always visible above. Below it a 6-digit MPIN: six dots that fill as digits are entered and a
custom 3 x 4 numeric keypad (1-9, blank, 0, delete) with large keys. Show the error state too: the field
border in red with "We don't recognise this customer ID." below it, and a second variant where the keypad is
disabled with "Try again in 24 s" after 5 wrong attempts.
```

**4.3 Lock screen**
```
Session lock screen shown after 30 minutes idle or 5 minutes in the background. A 72dp circular profile
photo, "Welcome back, Samanta", the six MPIN dots and keypad, and a text button "Not You?". Calm and
reassuring, with a soft frosted background. Show the wrong-MPIN state: dots clear, "Wrong MPIN. 3 attempts left."
```

**4.4 Step-up sheet**
```
A bottom sheet over a dimmed Review screen. Title "Confirm It's You". Line "Enter your MPIN to send $1,500.00."
Six MPIN dots and the keypad inside the sheet. A Cancel text button. This sheet is reused for adding a
payee, changing card limits and revealing card details; show the reveal-card variant copy too.
```

### Home

**4.5 Home**
```
Home screen. Top: a 32dp profile photo, "Hello, Samanta", and a notification bell with an unread dot
(4 unread). A hero balance card: "Wallet Balance" $155,832.00 (Everyday Checking ••3144) with an eye toggle
to hide balances, and two small chips: "In +$7,153.98" and "Out -$3,971.10" for the last 30 days.
Quick actions: Send Money, Pay Bills, Deposit Check, More (opens a sheet with View Statements and Explore Products).
Quick Transfer: a horizontal row of contacts with initials avatars (Mark R., Kris, Helen T., Tony Gym) and an
Add button.
Accounts: Everyday Checking $155,832.00 available ($156,207.00 ledger); Travel Savings $12,032.00, 3.10% APY;
Signature Credit Card $11,765.00 balance, $38,235.00 available, minimum $285.00 due in 18 days;
Personal Line of Credit $2,500.00 drawn of $15,000.00; 12-Month Term Deposit $25,000.00, 4.35% APY;
Investment Portfolio $84,612.40.
Recent Transactions with "View All": Aster & Vine Boutique, Shopping, Today 3:14 pm, -$125.00;
Northwind Furniture, Shopping, Today 11:40 am, -$375.00, Pending badge; Helen Torres, Transfer in,
Today 8:09 am, +$38.60.
A Features carousel at the bottom (Explore Products, Deposit Check, Statements).
```

**4.6 Home, balances hidden**
```
The same Home screen with the eye toggle on: every amount shows "••••••" at the same width, including
account rows, chips and transactions. The eye icon shows the "hidden" state.
```

**4.7 More Actions sheet**
```
A bottom sheet from Home titled "More Actions": a grid of large, friendly icon tiles: Deposit Check,
Pay Bills, Between My Accounts, View Statements, Explore Products, Help and Support.
```

### Cards

**4.8 Cards**
```
Cards screen. A stack of three 3D bank cards that peek below one another, the front one larger:
"Main card" Debit Mastercard, warm peach artwork, balance $155,832.00, SAMANTA JOHNS, 08/29, •••• 3144;
Signature Card Visa, sage artwork, $38,235.00, •••• 5821; Travel card Mastercard, graphite artwork,
$12,032.00, •••• 7720. Actions under the stack: Show Details, Freeze, Limits, Add to Google Wallet.
Quick Transfer row with contact avatars. Recent card transactions below.
```

**4.9 Card revealed**
```
The Cards screen after MPIN step-up: the front card shows the full number 5399 0000 0009 3144, expiry 08/29
and "CVV 314" in a monospaced style, with a Hide button and a small note "Hidden again when you leave this screen."
```

**4.10 Card controls**
```
Card Controls screen for the Main card. A Freeze Card switch at the top with a confirmation dialog
("Freeze this card? Card payments and ATM withdrawals will be declined until you unfreeze it.").
Daily Limits with sliders that show the value as text: ATM Withdrawal $1,000 (max $2,500), Retail Purchases
$5,000 (max $10,000), and a Save Limits button (needs MPIN). Channels with switches: International,
Online Purchases, Contactless. Travel Notices with "Add Travel Notice" and an active notice:
Lisbon, Portugal, Nov 14 to Nov 28.
```

**4.11 Add travel notice**
```
Add Travel Notice screen: Destination field (up to 40 characters), a date range picker for start and end
(start today or later, at most one year), and a primary button "Add Notice".
```

### Insights

**4.12 Insights**
```
Insights screen. Period selector: Last 30 Days (selected), This Month, Last Month. A spending donut chart
against the monthly budget: $3,650 of $18,900, with the top two categories (Shopping, Travel) and Other,
each with a percentage label and a legend. A Savings card: a line or area chart of the Travel Savings goal
by week, with "You're on track. You've almost reached your goal." A list of top categories with amounts.
```

### Money Movement

**4.13 Send Money: choose recipient**
```
Send Money, Step 1 of 5. Search field. "Between My Accounts" row at the top. Saved payees, recent first:
Mark Robinson (Northfield Bank ••1180), Kris Patel (Harbor State Bank ••2290), Helen Torres (ABC Bank ••5532),
Tony's Gym (Pinecrest Credit Union ••7001), Oakwood Properties, Daniel Kim. "Add New Payee" button.
```

**4.14 Add New Payee**
```
Add New Payee form: Name (up to 40 characters), Routing Number (9 digits; when valid the bank name appears
below, "Northfield Bank"), Account Number (6 to 17 digits). Primary button "Continue", which then asks for
the MPIN. Show an inline error state: "Enter a valid 9-digit routing number."
```

**4.15 Amount**
```
Send Money, Step 2 of 5, to Mark Robinson. A large amount display "$1,500.00" and a custom numeric keypad
(1-9, decimal point, 0, delete). From: Everyday Checking ••3144, $155,832.00 available. Optional Memo
(up to 60 characters) and Category chips: Housing, Utilities, Dining, Groceries, Shopping, Travel,
Entertainment, Health. Show the error state: "This is more than your available balance."
```

**4.16 Payment speed**
```
Send Money, Step 3 of 5. Two large selectable cards: Standard (ACH), Free, 1 to 2 business days;
Instant, $15.00 fee (1.5%, max $15.00), arrives within minutes. Instant selected.
```

**4.17 Review**
```
Send Money, Step 4 of 5, Review. Summary: To Mark Robinson, Northfield Bank ••1180; From Everyday Checking
••3144; Amount $1,500.00; Speed Instant; Fee $15.00; Total $1,515.00; Estimated delivery Within minutes;
Memo "Dinner and tickets"; Category Dining. Primary button "Confirm Transfer". Also show the disclaimer bottom
sheet that follows, with "Agree and Send".
```

**4.18 Receipt**
```
Send Money, Step 5 of 5, Receipt. A calm success illustration or animated check. "Money Sent" $1,500.00
to Mark Robinson, Instant. Reference 7f3c9a2e-41b8-4d0e-9a51-2c6e8b1d4f07. Buttons: View Transaction
(secondary) and Done (primary).
```

**4.19 Between My Accounts**
```
Between My Accounts transfer, Step 2 of 4: From and To account pickers (Everyday Checking, Travel Savings,
Personal Line of Credit), amount keypad, "No fee. Arrives instantly." Show the error when paying the line of
credit more than the $2,500.00 owed.
```

**4.20 Pay Bills**
```
Pay Bills screen. Bills, unpaid first by due date: Brightline Electric, Due Oct 28, $186.40;
StreamNet Fiber, Auto-Pay badge, Scheduled for Nov 3, $79.99; Paid bills below ("Paid Oct 20").
"Add a Biller" button. Then a bill detail screen: amount due, due date, Pay From account picker,
Pay Now (primary), Schedule Payment (date picker from tomorrow to the due date) and an Auto-Pay switch.
```

**4.21 Deposit Check**
```
Deposit Check screen. Deposit To selector: Everyday Checking ••3144 (selected), Travel Savings ••7720.
Amount keypad with "$2,450.00" and "Up to $10,000.00 per deposit" below. Two tiles: Front and Back of the
cheque, each with an outlined cheque illustration and Add or Remove. Primary button "Deposit Check".
Then the "Deposit Received" confirmation: amount, account, Pending badge, reference, View Transactions and Done.
```

### Self-service

**4.22 Transactions**
```
Transactions screen for all accounts. Search field (merchant name). Segmented control: All, Spendings,
Earnings. A Filter button that opens a sheet with date range (Last 30 Days, Last 90 Days, All Time) and
category chips. Grouped list: Today (Aster & Vine Boutique -$125.00; Northwind Furniture -$375.00 Pending;
Helen Torres +$38.60), Yesterday (Olive & Ember -$72.00; Bluebird Coffee -$6.45), Last 30 Days (Fresh Market Co.
-$142.18; Lumen Studio Payroll +$7,115.38; Cineplex Grand -$48.00; Brightline Electric -$186.40).
Businesses get category icons; people get initials avatars. Also show the empty search result state
("No transactions match your search.").
```

**4.23 Transaction detail**
```
Transaction detail for Aster & Vine Boutique: large -$125.00, Posted, Shopping tag, Today 3:14 pm,
Everyday Checking ••3144, reference, running balance $155,832.00. Buttons: Download Receipt (secondary) and
Dispute Transaction (red, opens a confirmation dialog). Also show a disputed charge (Melody Live Tickets,
-$420.00) with "This charge is under review. We'll update you in Support." in place of the button.
```

**4.24 Notifications**
```
Notifications screen with two tabs: "Transactional (3)" and "Promotional (1)". Newest first. Unread items
have a dot and a bolder title: "Money sent: $1,500.00 to Mark Robinson", "Deposit cleared: $2,450.00",
"Low balance: Everyday Checking is under $500". Time labels "Today, 3:14 pm", "Yesterday, 9:00 am".
A "Mark All as Read" action. Also show the empty state ("You're all caught up.").
```

**4.25 Explore Products**
```
Explore Products catalog: High-Yield Savings (4.50% APY), Premium Credit Card ("You're pre-approved for a
$15,000 limit at 21.24% APR"), Personal Loan ("Your rate: 7.99% APR, up to $50,000."). Each product card
has its key terms and Apply or View Account. Then the Personal Loan calculator: Amount slider $2,000 to
$50,000 in $500 steps (at $10,000), Term slider 12 to 60 months (at 36), showing Monthly Payment, Total
Interest and Total Repayment, and "Apply Now".
```

**4.26 Application flow**
```
A 4-step product application with a step indicator. Step 1 Review Your Details: name, email, phone,
customer ID, employment status chips (Employed selected), annual income $185,000. Step 2 Terms and
Disclosures with one checkbox. Step 3 "Checking Your Details" loading state, then "Approved" with
"Accept and Open Account". Step 4 confirmation: "Personal Loan ••4821 is open", reference, View Account, Done.
```

**4.27 Statements and Documents**
```
Statements and Documents: monthly statements for the last three months, one row per account with its
closing balance (Everyday Checking September 2026, $148,920.11). Tax Documents: 1099-INT for 2025.
Then the in-app document viewer showing a statement page with a Share button and the label
"Demo document. Not for tax filing." on the 1099-INT.
```

**4.28 Help and Support**
```
Help and Support hub: Chat With Us, Dispute a Transaction, Your Cases (one Open case with its last message),
and Common Questions as expandable rows. Then the chat screen: bot and customer bubbles, the bot answering
"How do I freeze my card?" with the matched FAQ, and a text input with a send button.
```

**4.29 Profile**
```
Profile screen: 72dp profile photo, Samanta Johns, customer ID SAMJOH01, credit score 792 (simulated) with
a friendly gauge. Rows: Personal Details, Statements and Documents, Help and Support, Security (MPIN,
session timeout info), Reset Demo Data (with a confirmation dialog), and Log Out.
```

## 05 State prompts (generate for Home, Transactions and Cards)

```
Same screen, loading state: skeleton placeholders in the shape of the real content, with a gentle shimmer.
```
```
Same screen, error state: a friendly illustration, "We couldn't load your accounts.", and a "Try Again" button.
```
```
Same screen, offline state: a slim banner "You're offline. Showing your last update." with cached content below.
```

## 06 Persona variants

Fictional customers from docs/DEMO_DATA.md. Generate each after the base screen.

**6.1 Marcus Lee (24, first job): low balance, frozen card, empty states**
```
Home for Marcus Lee ("Hello, Marcus"). Wallet Balance $412.17 (Everyday Checking ••2087) with a warm amber
low balance warning: "Your balance is under $500." Accounts: Starter Savings $1,250.00, 3.10% APY;
Cash Back Card $1,940.55 balance, $1,059.45 available, minimum $40.00 due in 3 days (highlighted as due soon).
An empty Investments section: "No investments yet" with "Explore Products".
```
```
Cards for Marcus: the Main card (Debit Mastercard •••• 2087, 05/28) shows a red "Frozen" badge and a
frosted, desaturated look, with an "Unfreeze" button. Cash Back Visa •••• 6634 shows International and
Contactless switched off.
```

**6.2 Grace Whitfield (64, retired): savings, term deposits, statements**
```
Home for Grace Whitfield ("Hello, Grace"), with larger, very readable type. Everyday Checking $18,240.66;
High-Yield Savings $96,500.00, 4.50% APY; 12-Month Term Deposit $150,000.00, 4.10% APY, matures in 120 days;
24-Month Term Deposit $50,000.00, 4.35% APY; Investment Portfolio $412,880.15; Rewards Card $2,315.40,
Auto-Pay on. Show an active travel notice on her card and her 1099-INT in Statements and Documents.
```

**6.3 Daniel Okafor (52, founder, high net worth): large values, raised limits**
```
Home for Daniel Okafor ("Hello, Daniel") with a premium feel. Premier Checking $486,250.00; Premier Savings
$1,200,000.00, 4.50% APY; ABC Signature Rewards Card $18,420.60, $56,579.40 available; 12-Month Term Deposit
$500,000.00; Investment Portfolio $2,412,560.80. Check that seven-figure amounts fit at 360dp and 200% font size.
Deposit Check shows "Up to $100,000.00 per deposit".
```

## 07 v2 concepts (not in v1 scope)

Dark mode and AI insights are out of scope for v1 (docs/PRD.md section 07; the app has no server or LLM).
Generate these in a separate Stitch page labelled "v2 Concept".

**7.1 Dark mode**
```
Dark mode version of Home and Cards. Warm dark surfaces (deep brown-charcoal, not pure black), the same
brand primary lifted for contrast, frosted-glass panels that glow softly, card artwork that stays rich.
All text at WCAG AA contrast.
```

**7.2 AI insights and personalisation**
```
v2 concept. A "For You" section on Home with smart insight cards in the same warm tone:
"You spent 18% less on dining this month.", "Your credit card payment is due in 3 days. Pay $285.00 now?",
"Move $2,000 to Travel Savings to reach your Lisbon goal by November." Each card has one clear action and a
dismiss. Add an "Edit Home" mode where the customer reorders or hides sections by dragging.
Insights messages never shame the customer and never use exclamation marks.
```

## 08 Guardrails for reviewing the output

- All names, numbers and businesses are fictional (docs/DEMO_DATA.md). Do not let Stitch swap in real banks,
  real card numbers or real people.
- Card numbers are masked everywhere except the revealed card state (4.9).
- No fingerprint, face unlock, SMS code, Withdraw or currency switcher should appear. Remove them if Stitch adds them.
- Copy: Title Case labels, sentence case messages, no exclamation marks.
- Check every generated screen at 360dp and imagine it at 200% font size.
- Material 3 Expressive components should map to Jetpack Compose. Flag anything that would need a new library.
- If a direction is chosen, update docs/DESIGN_SYSTEM.md and the theme tokens first, then the screens.

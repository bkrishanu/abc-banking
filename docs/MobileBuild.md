# Demo Mobile Banking App — Feature Specification & Roadmap

This repository outlines the functional scope, screen flows, and technical requirements for the Demo Mobile Banking App. The application is designed to simulate a modern retail banking experience, covering account aggregation, money movement, card operations, and self-service origination.

---

## 1. Authentication & Security

### 1.1 Multi-Factor Login & Biometrics
- **Biometric Unlock**: Hardcoded to a 6-digit numeric MPIN (i.e. 123456).
- **Session Lifecycle**:
  - Inactivity timeout: 30 minutes foreground idle.
  - Background dismissal: Mask screen buffer on app switcher; force re-authentication after 300 seconds in background.
- **Device Binding**: Simulated public-key registration to associate the device hardware with the user session.

> **Decided (PRD 08, 2026-10-17).** Login is a demo customer ID (`SAMJOH01`, `MARLEE01`, `GRAWHI01`, `DANOKA01`) plus the MPIN `123456`. There is no biometric unlock. 5 wrong MPIN entries lock entry for 30 s. Each persona's data is written once per install and kept; a different customer ID switches to that persona's data (revised 2026-10-20: login never waits for Firestore). After a session lock the app asks for the MPIN only and shows "Not you?" to log in with another ID. Profile has Log Out and a hidden Reset Demo Data action.

### 1.2 Step-Up Authentication
- Dynamic step-up prompt (SMS OTP or In-App Biometric Re-auth) triggered when:
  - Adding a new beneficiary/payee.
  - Initiating an outbound transfer exceeding $1,000.
  - Modifying card limits or unmasking sensitive credentials.

> **Decided (PRD 08, 2026-10-17).** Step-up is MPIN re-entry (`123456`) in a bottom sheet, not SMS OTP or biometric. The triggers above are unchanged. Wrong entries count towards the same 5-attempt, 30 s lockout as login.

---

## 2. Dashboard & Account Management

### 2.1 Accounts Overview
- **Account Aggregation Cards**:
  - Checking & Savings accounts (available balance vs. ledger balance).
  - Credit Cards (current balance, statement balance, available credit, payment due date).
  - Term Deposits & Investment holdings (total portfolio value).
- **Balance Masking**: Eye toggle icon on header to hide/unhide sensitive balance amounts.
- **Quick Action Bar**: Shortcuts to *Send Money*, *Pay Bills*, *Deposit Check*, and *View Statements*.

### 2.2 Transaction Ledger & Details
- Chronological list grouped by date (Today, Yesterday, Last 30 Days).
- Search & multi-parameter filter:
  - Date ranges, transaction types (Credits, Debits), category (Dining, Utilities, Groceries).
- Transaction drill-down screen:
  - Merchant name, logo, timestamp, reference number, running ledger balance, and categorized spend tag.
  - Action buttons: "Download Receipt (PDF)" and "Dispute Transaction".

> **Decided (2026-10-20).** The ledger opens from Home's "View All" (every account) or from an account row (that account). Date range: Last 30 Days (default), Last 90 Days or All Time. Groups: Today, Yesterday, Last 30 Days, then Earlier when the range is longer. Type chips: All / Spendings (debits) / Earnings (credits). Categories: any number, chosen in a filter sheet. Search: merchant name, case-insensitive. All filters combine. Firestore is read by date only; type, category and search filter on the device, so they need no index. Download Receipt makes a one-page A5 PDF on the device (merchant, amount, date, time, account, category, status, reference) and opens the share sheet. Dispute Transaction is offered for posted charges only (not money in, transfers, pending items or charges already disputed); after a confirmation dialog it creates a support case with the topic "I didn't make this purchase" and the same two messages as the seeded dispute, and marks the transaction disputed. A disputed transaction shows "This charge is under review. We'll update you in Support." instead of the button.

### 2.3 Deposit Check

> **Added (PRD 08, 2026-10-17).** Not in the original spec; the quick action in 2.1 opens this flow.

- Enter the amount on the amount keypad and pick the account to deposit into (checking or savings only).
- Amount must be $1.00 to $10,000.00 per deposit ($100,000.00 for the high-net-worth persona). Above the limit, confirm is blocked inline.
- Front and back cheque images are optional placeholders. Confirm works with or without them. No camera.
- On confirm, a Pending credit ("Check deposit") appears in transactions. The available balance does not change yet.
- 30 seconds later the deposit posts: status becomes Posted, the balance increases, and a transactional "Deposit cleared" alert is created. If the app was closed at that moment, this happens the next time it opens.

> **Decided (2026-10-20).** Deposit Check opens from Home's More Actions sheet. One screen: Deposit To (checking first, savings), the amount on the keypad with "Up to $10,000.00 per deposit" below it (the inline error states the limit), Front and Back tiles that attach or remove a bundled line-art cheque, and Deposit Check. Then "Deposit Received" with the amount, account, Pending status and reference, View Transactions (that account) and Done. Clearing runs while the app is in the foreground, so a deposit made in the background or before closing posts on the next return.

---

## 3. Payments & Money Movement

### 3.1 Account-to-Account (Internal) Transfers
- Instant transfers between a user's own Checking, Savings, and Line of Credit accounts.
- Inline validation ensuring transfer amount does not exceed available balance.

### 3.2 Peer-to-Peer (P2P) & Domestic Transfers
- **Beneficiary Directory**:
  - Recent contacts and saved payees list.
  - "Add Beneficiary" flow with bank routing number lookup (ABA) / account number verification.
- **Transfer Initiation Flow**:
  1. *Recipient Selection*: Choose contact or manual entry.
  2. *Amount & Memo*: Numerical keypad input with optional category tagging.
  3. *Payment Speed*: Standard ACH (1–2 business days) vs. Instant Transfer (wire/instant rail fee calculation).
  4. *Review Screen*: Summary breakdown including transaction fees, estimated delivery, and disclaimer modal.
  5. *Receipt Screen*: Printable confirmation showing transaction reference UUID.

> **Decided (2026-10-20).** Home's Transfer opens Send Money: Between My Accounts, then saved payees (recent first, in their Quick transfer order), and Add New Payee. Between own accounts has 4 steps (recipient, amount, review, receipt) and moves money between checking, savings and a line of credit with a limit, with no fee and no step-up; paying a line of credit cannot exceed what is owed. P2P has the 5 steps above and leaves from checking or savings. The amount is typed on a keypad (up to $9,999,999.99), checked inline against the available balance including the fee, with an optional memo (up to 60 characters) and one optional category tag (Housing, Utilities, Dining, Groceries, Shopping, Travel, Entertainment, Health; untagged transfers use Transfer out). Standard ACH is free, 1 to 2 business days; Instant is 1.5%, at least $0.25 and at most $15.00, within minutes, and is posted as a separate "Instant Transfer Fee" transaction. Review shows to, from, amount, speed, fee, total, estimated delivery, memo and category; Confirm Transfer opens a disclaimer sheet (Agree and Send), then step-up above $1,000. Transfers post at once. The receipt shows the reference UUID, View Transaction (which has Download Receipt for printing) and Done; Back from the receipt leaves the flow so nothing is sent twice. Add New Payee needs a name (up to 40 characters), a routing number that passes the ABA checksum and is in the bank catalog (the bank name shows as it is found), and a 6 to 17 digit account number; numbers ending 0000 fail verification. The form is checked before step-up, so a typo never costs an MPIN entry.

### 3.3 Bill Pay & Utilities
- Directory of popular utility providers, telecom operators, and municipal billers.
- Bill fetch simulation displaying amount due and due date.
- Auto-pay toggle and one-time scheduled payment picker.

> **Decided (2026-10-20).** Home's Pay Bills opens the customer's bills, unpaid first by due date ("Due Oct 24", "Scheduled for Nov 3", "Paid Oct 20", an Auto-Pay badge). Add a Biller searches the catalog directory by name or type, takes the 6 to 17 digit account number from the bill, fetches the bill (DEMO_DATA.md section 07) and saves it after MPIN step-up, as adding a payee does; a biller already added cannot be added twice. A bill shows the amount due, Pay From (checking or savings, blocked inline when the balance is short), Pay Now (step-up above $1,000, as for any outbound payment), Schedule Payment (a date from tomorrow through the due date, or Cancel Scheduled Payment) and the Auto-Pay switch. Paying posts the debit (Utilities, or Housing for property tax and insurance), a "Bill paid" alert and a low balance alert on crossing, and marks the bill paid. Scheduled and auto-pay payments are saved and listed; this demo does not run them on their date.

---

## 4. Card Management & Controls

### 4.1 Digital Card Experience
- Digital card preview with dynamic toggle to reveal Card Number (PAN), Expiry, and CVV (protected by biometric step-up).
- "Add to Apple Wallet / Google Pay" simulated button integration.

> **Decided (PRD 01, 07, 08).** Reveal is protected by MPIN step-up, not biometric (see 1.2). Only Google Wallet is shown.

> **Decided (2026-10-20).** Revealed details stay in memory only and are hidden again on another card, on Hide, when the screen is left and when the app goes to the background. Add to Google Wallet shows a success state for the session ("Demo only. No card was added to a real wallet."); nothing is stored.

### 4.2 Security Controls
- **Card Freeze**: Immediate toggle to block POS, ATM, and e-commerce transactions.
- **Transaction Limits**: Sliders for daily ATM cash withdrawal limit and daily retail purchase limit.
- **Channel Preferences**: Toggle switches for:
  - International Transactions.
  - Online / Card-Not-Present Transactions.
  - Contactless / NFC Payments.
- **Travel Notices**: Date-picker workflow to declare travel destinations for automated fraud whitelist.

> **Decided (2026-10-20).** Freezing asks for confirmation first (unfreezing does not). Limit sliders move in $100 steps, or $500 when the maximum is above $10,000, from one step up to the card's maximum; changes are a draft until Save Limits, which needs step-up. Credit cards adjust only the cash advance limit (purchases follow available credit). Channel switches save at once, without step-up. A travel notice needs a destination (up to 40 characters), starts today or later, ends on or after its start, and covers at most one year; it is active from its first through its last day. Notices are added, not edited or deleted, in v1.

---

## 5. Product Origination & Leads

### 5.1 Product Catalog
- Curated marketplace for supplementary accounts: High-Yield Savings Accounts (HYSA), Premium Credit Cards, and Personal Loans.
- Interactive loan calculator: Sliders for principal and tenure displaying estimated monthly installment (EMI) and APR.

### 5.2 Multi-Step Application Flow
- Step 1: Pre-filled identity review (Employment status, annual income).
- Step 2: Terms and disclosures acceptance.
- Step 3: Instant simulated credit scoring/approval state.
- Step 4: Account setup completion and instant issuance confirmation.

> **Decided (2026-10-20).** Explore Products opens from Home's More Actions sheet and the "Explore Products" empty states. Each product shows its key terms and, before applying, what the simulated check would offer ("You're pre-approved for a $15,000 limit at 21.24% APR", "Your rate: 7.99% APR, up to $50,000."). The personal loan opens the calculator first: amount in $500 steps from $2,000 to the tier's maximum, term in 6-month steps from 12 to 60, starting at $10,000 over 36 months; the monthly payment is amortised and rounded to the cent, with total interest and total repayment. Step 1 shows name, email, phone and customer ID from the profile, with employment status (chips) and annual income (whole dollars) to confirm. Step 2 lists the product's disclosures and one checkbox. Step 3 shows "Checking Your Details" for 1.5 s, then the decision from the credit score (DEMO_DATA.md section 07); Accept and Open Account writes the application and the account, Submit for Review records a referred application. Step 4 confirms with the account name and ending, the reference, View Account (that account's transactions) and Done (back to the catalog, where the product now shows Opened). Back on step 4 also finishes, so nothing is applied for twice. A product with an opened account shows View Account instead of applying again; a referred one shows Under Review.

---

## 6. Self-Service & Notifications

- **In-App Notification Center**: Split into *Transactional Alerts* (debit alerts, low balance warnings) and *Promotional Messages*.

> **Decided (2026-10-20).** The bell on Home opens Notifications: Transactional and Promotional tabs, each with its unread count ("Transactional (3)"), newest first. Unread alerts have an accent dot and a semibold title. Opening an alert marks it read; Mark All as Read clears the selected tab. Time labels: "Today, 3:14 pm", "Yesterday, 9:00 am", then the date. Alerts come from the seed, transfers (money sent, money moved, low balance), applications and deposits; there are no push notifications.
- **Statements & Document Center**: PDF viewer for monthly statements and tax summaries (1099-INT).
- **Customer Support Hub**: Simulated live chat with automated FAQ triage and contextual transaction dispute initiation.

> **Decided (PRD 08, 2026-10-17).** Low balance warnings fire once when an account crosses below $500.00 (available balance for checking and savings, available credit for credit cards and lines of credit). Statement and 1099-INT PDFs are generated on the device from Firestore data; there is no Cloud Storage.
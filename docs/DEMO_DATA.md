# Demo Data: ABC Bank Mobile (Android)

<!--
The pre-loaded data for the demo. DemoDataSeeder turns this file into
Firestore documents. The JSON in app/src/main/assets/seed/ must match it.
When you change a value here, change the JSON and bump seedVersion in the
same commit. Every person, company, bank and number below is fictional.
-->

## 01 How seed data works

- Four demo personas ship with the app. After Get Started, the presenter logs in with a persona's customer ID (section 02) and the MPIN.
- `DemoDataSeeder` writes each persona's data into its own tree, `users/{uid}/personas/{personaId}` (under the install's anonymous Firebase user), the first time that persona logs in on the install. The persona document holds the profile with `personaId`, `customerId` and `seedVersion`.
- Logging in never waits for this: the writes go to Firestore's offline cache and upload in the background (ARCHITECTURE.md section 04). Logging in again with a persona keeps its tree; a newer bundled `seedVersion` writes it again.
- Logging in with a different customer ID opens that persona's own tree; nothing is deleted. The hidden Reset Demo Data action in Profile deletes the current persona's tree and writes it again, after its confirmation dialog. It is the only data deletion in the app.
- Shared reference data (`catalog/*`) is created once per Firebase project by a person (ARCHITECTURE.md section 08). Personas only reference it by id.
- The MPIN for every persona is `123456`. It is also the step-up factor; there is no OTP (PRD section 08).

Dates are relative so the demo always looks current:
- Every dated item has `dayOffset` (0 = seed day, -1 = yesterday, +9 = nine days ahead) and a local `time`.
- An item with `dayOffset: 0` whose time is still in the future when the seed runs is moved to 10 minutes before now.
- Statements cover the 3 calendar months before the seed month, which the generated history always covers. The 1099-INT is for the previous tax year.

Generated history (revised 2026-10-19):
- The explicit transactions below are always present. The seeder adds generated history back to day -122 with a fixed random seed (`181019`), so every install of the same persona looks identical.
- History is four windows: days 0 to -29 ("the last 30 days"), -30 to -59, -60 to -89 and -90 to -122. In each window, spending matches the persona's category targets exactly.
- Spending means debits in every category except Transfer and Transfer out, on any account. Pending debits count, because they show in the transaction list.
- In the last 30 days the explicit transactions count first and generated items fill the rest, on days -3 to -29. Nothing is generated on days 0 to -2, so the explicit items there decide the latest balances (for example, the moment Marcus goes below $500.00).
- Utilities are one payment per monthly utility bill. A bill without an explicit payment in the last 30 days is paid 30 days before its current due date, at the bill amount (06:00 when it is on auto-pay). So each persona's Utilities target is the sum of their utility bills' latest payments.
- Housing has no merchant pool; it is the explicit rent or upkeep payment.
- Older windows repeat the last 30 days' bills (Housing and Utilities) and posted income, interest and transfers on checking and savings, 30, 60 and 90 days earlier, so every statement month shows a salary. An internal transfer repeats only when both legs are on checking or savings; card payments do not repeat.
- Other categories draw merchants from the pools in section 07: as many items as the target needs at a typical amount, each inside the pool's range (the last item takes the remainder, also inside the range). Times are on the minute between 07:00 and 22:00.
- All generated debits post to the Main card's linked account (Everyday Checking, or Premier Checking for daniel).
- Running balances are worked back from the closing balances in this document, which are the balances after all seeded transactions. Pending items do not change the balance. Checking and savings never go below $0.00 and cards never go above their limit; the tests check this for every persona.

Consistency rules for the values in this document (2026-10-19):
- Savings interest for a month is the closing balance x APY x 30/365, rounded to the cent.
- Credit card statements close 25 days before the due date. Current balance = statement balance + card debits - card credits posted after the statement closed.
- "Due in N days" and "matures in N days" in notifications count from the notification's date.
- Each persona's last-30-day spending total and budget percentage stay as documented; when a category target changed, Dining or Groceries absorbed the difference.

Rules that apply to all personas:
- Money is in USD and stored as cents. The tables show dollars for readability.
- Fictional contact details only: emails end in `@example.com`, phone numbers use the 555-01xx range.
- Routing numbers start with `5`, a prefix no real bank is assigned, and pass the ABA checksum. Card numbers use made-up 539900 (Mastercard) and 499900 (Visa) prefixes and pass the Luhn check. None are real.
- Persona profile photos are bundled in the app: `app/src/main/res/drawable-nodpi/avatar_{personaId}.webp` (512 x 512 head-and-shoulders crops, WebP quality 85). The full-size originals are in `images/{First}_{Last}.png` at the repository root and are not packaged. The photos are fictional people. Beneficiary and contact avatars stay initials on `surfaceMuted`. Do not use the people photos from the sample PNGs.
- Low balance alert threshold: below $500.00. Checking and savings use the available balance; credit cards and lines of credit use available credit. Term deposits and investments never alert. The alert fires once when an account crosses below the threshold, not on every debit.
- Statements and the 1099-INT are generated in the app from this data (no Cloud Storage).

## 02 Persona summary

| personaId | Customer ID (login) | Name | Story | What it demos best | Credit score |
| --- | --- | --- | --- | --- | --- |
| `samanta` | `SAMJOH01` | Samanta Johns | Product design lead, 34, affluent, matches the sample screens | The default demo: full account aggregation, P2P with step-up over $1,000, card stack, insights | 792 |
| `marcus` | `MARLEE01` | Marcus Lee | Junior software engineer, 24, first job | Low balance warning, frozen card, empty states, credit card due soon, personal loan application | 688 |
| `grace` | `GRAWHI01` | Grace Whitfield | Retired teacher, 64 | Term deposits, investments, autopay bills, active travel notice, statements and 1099-INT | 760 |
| `daniel` | `DANOKA01` | Daniel Okafor | Founder of a freight business, 52, high net worth | Raised limits: $100,000 check deposits and high card limits; large transfers that still need step-up; large portfolio | 815 |

Customer IDs are exactly 8 alphanumeric characters (`^[A-Za-z0-9]{8}$`): the first three letters of the first name, the first three of the surname, and a two-digit number. They are shown in upper case and matched case-insensitively after trimming spaces.
- Wrong format (not 8 letters or digits): the field shows "Customer ID is 8 letters or numbers." and the MPIN pad stays disabled.
- Right format but not one of the four above: "We don't recognise this customer ID."

## 03 Persona: Samanta Johns (`samanta`)

Profile

| Field | Value |
| --- | --- |
| Customer ID | SAMJOH01 |
| Display name | Samanta Johns (card holder name: SAMANTA JOHNS) |
| Greeting | Hello, Samanta |
| Email / phone | samanta.johns@example.com / +1 (555) 010-2231 |
| Employment / annual income | Employed, Product Design Lead at Lumen Studio / $185,000 |
| Monthly spending budget | $18,900.00 |
| Credit score (simulated) | 792 |

Accounts

| Account | Type | Last 4 | Values |
| --- | --- | --- | --- |
| Everyday Checking | CHECKING | 3144 | Available $155,832.00, ledger $156,207.00 (one pending debit of $375.00) |
| Travel Savings | SAVINGS | 7720 | Available and ledger $12,032.00, 3.10% APY |
| Signature Credit Card | CREDIT | 5821 | Limit $50,000.00, current balance $11,765.00, available $38,235.00, statement balance $11,765.00, minimum due $285.00, due in 18 days |
| Personal Line of Credit | LOAN | 6608 | Limit $15,000.00, drawn $2,500.00, available $12,500.00, 9.25% APR |
| 12-Month Term Deposit | TERM_DEPOSIT | 9034 | Principal $25,000.00, 4.35% APY, opened day -245, matures day +120 |
| Investment Portfolio | INVESTMENT | 4410 | Total $84,612.40: ABC Total Market Index Fund $52,300.00, ABC Bond Income Fund $21,412.40, cash $10,900.00 |

Home wallet balance = the Main card's linked account available balance ($155,832.00), as in the sample screens. The Accounts section below it lists every account.
In/out chips on Home show the last 30 days of credits and debits on that wallet account, pending items included (2026-10-20).

Cards (Cards tab stack order)

| Tag | Card | Number | Expiry / CVV | Art | Linked account | Controls |
| --- | --- | --- | --- | --- | --- | --- |
| Main card | Debit Mastercard | 5399 0000 0009 3144 | 08/29 / 314 | card-peach | Everyday Checking | ATM $1,000 (max $2,500), retail $5,000 (max $10,000), international on, online on, contactless on |
| Signature Card | Visa Signature credit | 4999 0000 0001 5821 | 11/28 / 582 | card-sage | Signature Credit Card | Cash advance $500 (max $2,000), retail = available credit, all channels on |
| Travel card | Debit Mastercard | 5399 0000 0002 7720 | 03/30 / 772 | card-graphite | Travel Savings | ATM $800 (max $2,500), retail $3,000 (max $10,000), all channels on. Travel notice: Lisbon, Portugal, day +21 to day +35 |

The card stack shows each linked account's available balance (Main $155,832.00, Signature $38,235.00, Travel $12,032.00).

Beneficiaries (first four are the Quick transfer row, in this order)

| Name | Bank | Routing | Account | Recent |
| --- | --- | --- | --- | --- |
| Mark Robinson (Mark R.) | Northfield Bank | 500000021 | ••1180 | yes |
| Kris Patel (Kris) | Harbor State Bank | 500000034 | ••2290 | yes |
| Helen Torres (Helen T.) | ABC Bank | 500000018 | ••5532 | yes |
| Tony's Gym (Tony Gym) | Pinecrest Credit Union | 500000047 | ••7001 | yes |
| Oakwood Properties | Summit National Bank | 500000050 | ••3305 | no |
| Daniel Kim | Lakeview Savings | 500000063 | ••8814 | no |

Explicit transactions (Everyday Checking unless noted)

| Day | Time | Description | Category | Amount | Status |
| --- | --- | --- | --- | --- | --- |
| 0 | 15:14 | Aster & Vine Boutique | Shopping | -$125.00 | Posted |
| 0 | 11:40 | Northwind Furniture | Shopping | -$375.00 | Pending |
| 0 | 08:09 | Helen Torres | Transfer in | +$38.60 | Posted |
| -1 | 21:21 | Olive & Ember (restaurant) | Dining | -$72.00 | Posted |
| -1 | 09:05 | Bluebird Coffee | Dining | -$6.45 | Posted |
| -2 | 18:30 | Fresh Market Co. | Groceries | -$142.18 | Posted |
| -3 | 07:00 | Lumen Studio Payroll | Income | +$7,115.38 | Posted |
| -3 | 12:00 | To Travel Savings ••7720 (and matching +$1,000.00 on Travel Savings) | Transfer | -$1,000.00 | Posted |
| -4 | 20:00 | Cineplex Grand | Entertainment | -$48.00 | Posted |
| -5 | 10:15 | Brightline Electric (bill pay) | Utilities | -$186.40 | Posted |
| -6 | 06:00 | StreamNet Fiber (autopay) | Utilities | -$79.99 | Posted |
| -7 | 18:45 | Tony's Gym | Health | -$65.00 | Posted |
| -9 | 13:20 | SkyWays Airlines (Signature Credit Card) | Travel | -$1,240.00 | Posted |
| -10 | 19:02 | Mark Robinson | Transfer out | -$250.00 | Posted |
| -11 | 22:10 | Melody Live Tickets (Signature Credit Card) | Entertainment | -$420.00 | Posted, disputed |
| -12 | 00:05 | Interest (Travel Savings) | Interest | +$30.66 | Posted |
| -14 | 09:30 | Volt Mobile | Utilities | -$95.00 | Posted |
| -17 | 07:00 | Lumen Studio Payroll | Income | +$7,115.38 | Posted |

Last-30-day spending targets (drives the Insights donut, total $12,480.00 = 66% of budget, as in the sample screen):
Shopping $4,620.00 (37%), Entertainment $2,620.00 (21%), Dining $1,859.31, Travel $1,660.00, Groceries $1,240.00, Utilities $415.69 (one payment per bill), Health $65.00 (the Tony's Gym payment).
The donut shows the top two categories plus Other.

Bills

| Biller | Amount due | Due | Auto-pay | Scheduled |
| --- | --- | --- | --- | --- |
| Volt Mobile | $95.00 | day +4 | off | none |
| Brightline Electric | $192.75 | day +9 | off | none |
| StreamNet Fiber | $79.99 | day +12 | on | none |
| City Water & Sewer | $54.30 | day +15 | off | day +14 |

Savings goal (Insights savings chart)
- "Summer in Lisbon", target $4,000.00, saved $3,650.00, linked to Travel Savings, started day -90.
- Weekly saved-so-far points, oldest first: 1,200, 1,350, 1,900, 1,500, 1,780, 1,650, 1,620, 1,700, 2,480, 2,200, 2,400, 3,560, 3,650.
- Message: "You're on track. You've almost reached your goal."

Notifications

| Day | Kind | Title | Body | Read |
| --- | --- | --- | --- | --- |
| 0 | Transactional | Money received | Helen Torres sent you $38.60. | no |
| 0 | Transactional | Card used | $125.00 at Aster & Vine Boutique with your Main card. | no |
| -1 | Transactional | Bill due soon | Volt Mobile, $95.00, due in 5 days. | no |
| -3 | Transactional | Salary received | $7,115.38 paid into Everyday Checking. | yes |
| -2 | Promotional | Grow your savings | Earn 4.50% APY with ABC High-Yield Savings. | no |
| -6 | Promotional | Dining rewards | Earn 3x points on dining with your Signature Card this month. | yes |

Support cases
- One open dispute on Melody Live Tickets, -$420.00, topic "I didn't make this purchase", status In review, opened day -10, two messages (customer report, bank acknowledgement).

Documents
- Monthly statements for Everyday Checking and Signature Credit Card, 3 months.
- 1099-INT for the previous tax year: interest $412.86.

## 04 Persona: Marcus Lee (`marcus`)

Profile

| Field | Value |
| --- | --- |
| Customer ID | MARLEE01 |
| Display name | Marcus Lee (MARCUS LEE) |
| Email / phone | marcus.lee@example.com / +1 (555) 010-4478 |
| Employment / annual income | Employed, Software Engineer I at Brightpath Software / $68,000 |
| Monthly spending budget | $3,000.00 |
| Credit score (simulated) | 688 |

Accounts

| Account | Type | Last 4 | Values |
| --- | --- | --- | --- |
| Everyday Checking | CHECKING | 2087 | Available and ledger $412.17 (below the $500 threshold, shows low balance warning) |
| Starter Savings | SAVINGS | 6120 | $1,250.00, 3.10% APY |
| Cash Back Card | CREDIT | 6634 | Limit $3,000.00, current balance $1,940.55, available $1,059.45, statement balance $2,880.56, minimum due $40.00, due in 3 days |

No term deposit, investment or line of credit. Home shows the empty state "No investments yet" with the action "Explore Products".

Cards

| Tag | Card | Number | Expiry / CVV | Art | Linked account | Controls |
| --- | --- | --- | --- | --- | --- | --- |
| Main card | Debit Mastercard | 5399 0000 0000 2087 | 05/28 / 208 | card-peach | Everyday Checking | Frozen (since day -1). ATM $500 (max $1,000), retail $2,000 (max $5,000), all channels on |
| Cash Back | Visa credit | 4999 0000 0006 6634 | 10/27 / 663 | card-sage | Cash Back Card | Cash advance $200 (max $600), retail = available credit. International off, contactless off, online on |

Beneficiaries
- Sarah Lee, Lakeview Savings, routing 500000063, ••4402, recent.
- Only one entry, so the Quick transfer row shows one contact plus the add button.

Explicit transactions (Everyday Checking unless noted)

| Day | Time | Description | Category | Amount | Status |
| --- | --- | --- | --- | --- | --- |
| 0 | 12:31 | Campus Deli | Dining | -$14.80 | Posted |
| -1 | 19:44 | QuickRide | Transport | -$18.25 | Posted |
| -2 | 08:00 | Oakwood Properties (rent) | Housing | -$1,450.00 | Posted |
| -3 | 12:00 | Sarah Lee | Transfer out | -$1,400.00 | Posted |
| -4 | 07:00 | Brightpath Software Payroll | Income | +$2,108.46 | Posted |
| -6 | 16:10 | Fresh Market Co. | Groceries | -$64.37 | Posted |
| -8 | 21:05 | GameVault (Cash Back Card) | Entertainment | -$59.99 | Posted |
| -13 | 11:00 | Sarah Lee | Transfer in | +$120.00 | Posted |
| -16 | 12:00 | Cash Back Card payment (and matching +$1,000.00 "From Everyday Checking ••2087" on Cash Back Card) | Transfer | -$1,000.00 | Posted |
| -18 | 07:00 | Brightpath Software Payroll | Income | +$2,108.46 | Posted |

The day -3 and day -16 outflows were added 2026-10-19 so his checking history never goes below $0.00 (lowest point $22.67, before filler).

Last-30-day spending targets (total $2,755.00): Housing $1,450.00, Groceries $461.80, Shopping $310.00, Dining $240.00, Utilities $83.20, Transport $120.00, Entertainment $90.00.

Bills: Volt Mobile $45.00 due day +2 (auto-pay off), Metro Gas Co. $38.20 due day +8 (auto-pay off).

Savings goal: "Emergency fund", target $3,000.00, saved $1,250.00, linked to Starter Savings.

Notifications

| Day | Kind | Title | Body | Read |
| --- | --- | --- | --- | --- |
| -2 | Transactional | Low balance | Everyday Checking is below $500.00. (08:00, at the rent payment) | no |
| -1 | Transactional | Card frozen | Your Main card ending 2087 is frozen. | no |
| -1 | Transactional | Payment due soon | Cash Back Card, $40.00 minimum due in 4 days. | no |
| -3 | Promotional | You're pre-qualified | Check your personal loan rate in 2 minutes. It won't affect your score. | no |

Support cases: none (shows the support empty state). Documents: 3 monthly statements for checking and the credit card; no 1099-INT (interest under $10).

## 05 Persona: Grace Whitfield (`grace`)

Profile

| Field | Value |
| --- | --- |
| Customer ID | GRAWHI01 |
| Display name | Grace Whitfield (GRACE WHITFIELD) |
| Email / phone | grace.whitfield@example.com / +1 (555) 010-7315 |
| Employment / annual income | Retired / $77,160 (pension and benefits) |
| Monthly spending budget | $5,000.00 |
| Credit score (simulated) | 760 |

Accounts

| Account | Type | Last 4 | Values |
| --- | --- | --- | --- |
| Everyday Checking | CHECKING | 4471 | Available and ledger $18,240.66 |
| High-Yield Savings | SAVINGS | 3019 | $96,500.00, 4.50% APY |
| 12-Month Term Deposit | TERM_DEPOSIT | 7783 | Principal $150,000.00, 4.10% APY, matures day +120 |
| 24-Month Term Deposit | TERM_DEPOSIT | 7790 | Principal $50,000.00, 4.35% APY, matures day +300 |
| Investment Portfolio | INVESTMENT | 8126 | Total $412,880.15: ABC Total Market Index Fund $198,450.00, ABC Bond Income Fund $164,230.15, ABC Dividend Growth Fund $50,200.00 |
| Rewards Card | CREDIT | 9152 | Limit $20,000.00, current balance $2,315.40, statement balance $1,042.00, available $17,684.60, auto-pay full balance, due in 18 days |

Cards

| Tag | Card | Number | Expiry / CVV | Art | Linked account | Controls |
| --- | --- | --- | --- | --- | --- | --- |
| Main card | Debit Mastercard | 5399 0000 0000 4471 | 12/27 / 447 | card-sage | Everyday Checking | ATM $1,000 (max $2,500), retail $5,000 (max $10,000), all channels on. Travel notice: Toronto, Canada, day -3 to day +10 (active) |
| Rewards Card | Visa credit | 4999 0000 0002 9152 | 07/29 / 915 | card-graphite | Rewards Card | Cash advance $500 (max $2,000), retail = available credit, all channels on. Travel notice: Toronto, Canada, day -3 to day +10 (active) |

Beneficiaries: Emily Whitfield (ABC Bank, 500000018, ••6610, recent), James Whitfield (Harbor State Bank, 500000034, ••0937, recent), Lakeside Garden Club (Pinecrest Credit Union, 500000047, ••2214, not recent).

Explicit transactions (Everyday Checking unless noted)

| Day | Time | Description | Category | Amount | Status |
| --- | --- | --- | --- | --- | --- |
| 0 | 10:05 | Maple Leaf Café, Toronto (Rewards Card) | Dining | -$23.40 | Posted |
| -1 | 07:00 | State Teachers Pension | Income | +$4,280.00 | Posted |
| -2 | 14:30 | Harbourfront Hotel, Toronto (Rewards Card) | Travel | -$612.00 | Posted |
| -3 | 06:15 | SkyWays Airlines (Rewards Card) | Travel | -$638.00 | Posted |
| -5 | 06:00 | Brightline Electric (autopay) | Utilities | -$142.10 | Posted |
| -9 | 11:20 | Greenleaf Pharmacy | Health | -$46.85 | Posted |
| -12 | 07:00 | Social Security Benefit | Income | +$2,150.00 | Posted |
| -15 | 00:05 | Interest (High-Yield Savings) | Interest | +$356.92 | Posted |
| -20 | 13:00 | Emily Whitfield | Transfer out | -$500.00 | Posted |

Last-30-day spending targets (total $3,692.00): Travel $1,250.00, Groceries $619.96, Shopping $520.00, Dining $410.00, Health $380.00, Utilities $352.04, Entertainment $160.00.

Bills (all auto-pay on): Brightline Electric $142.10 due day +25, City Water & Sewer $61.20 day +16, Metro Gas Co. $88.75 day +19, StreamNet Fiber $59.99 day +11, Fairview County Treasurer (property tax) $3,420.00 scheduled for day +25.

Savings goal: "Grandkids' college gift", target $20,000.00, saved $14,300.00, linked to High-Yield Savings.

Notifications

| Day | Kind | Title | Body | Read |
| --- | --- | --- | --- | --- |
| -1 | Transactional | Pension received | $4,280.00 paid into Everyday Checking. | no |
| -3 | Transactional | Travel notice active | Your Main and Rewards cards are set for Canada until {date of day +10}. | yes |
| -4 | Transactional | Term deposit maturing | Your 12-Month Term Deposit matures in 124 days. | yes |
| -7 | Promotional | Ladder your deposits | Lock in 4.35% APY on a 24-month term deposit. | yes |

Support cases: none. Documents: 3 monthly statements for checking, savings and the credit card; 1099-INT for the previous tax year, interest $9,846.12.

## 06 Persona: Daniel Okafor (`daniel`, high net worth)

Added 2026-10-17 with approval (PRD section 08). The only persona with raised limits: check deposits up to $100,000.00 and higher card limits. Step-up still triggers above $1,000, like every persona.

Profile

| Field | Value |
| --- | --- |
| Customer ID | DANOKA01 |
| Display name | Daniel Okafor (DANIEL OKAFOR) |
| Greeting | Hello, Daniel |
| Email / phone | daniel.okafor@example.com / +1 (555) 010-5590 |
| Employment / annual income | Self-employed, Founder and CEO of Okafor Freight Partners / $1,250,000 |
| Monthly spending budget | $40,000.00 |
| Credit score (simulated) | 815 |
| Check deposit limit | $100,000.00 per deposit (standard personas: $10,000.00) |

Accounts

| Account | Type | Last 4 | Values |
| --- | --- | --- | --- |
| Premier Checking | CHECKING | 5512 | Available and ledger $486,250.00 |
| Premier Savings | SAVINGS | 8840 | $1,200,000.00, 4.50% APY |
| ABC Signature Rewards Card | CREDIT | 7316 | Limit $75,000.00, current balance $18,420.60, available $56,579.40, statement balance $15,885.10, auto-pay full balance, due in 21 days |
| 12-Month Term Deposit | TERM_DEPOSIT | 2259 | Principal $500,000.00, 4.35% APY, opened day -180, matures day +185 |
| Investment Portfolio | INVESTMENT | 6673 | Total $2,412,560.80: ABC Total Market Index Fund $1,350,000.00, ABC Bond Income Fund $612,560.80, ABC Dividend Growth Fund $400,000.00, cash $50,000.00 |

No loan or line of credit.

Cards

| Tag | Card | Number | Expiry / CVV | Art | Linked account | Controls |
| --- | --- | --- | --- | --- | --- | --- |
| Main card | Debit Mastercard | 5399 0000 0008 5512 | 09/30 / 551 | card-graphite | Premier Checking | ATM $5,000 (max $10,000), retail $25,000 (max $50,000), all channels on |
| Signature Card | Visa Signature credit | 4999 0000 0009 7316 | 02/30 / 731 | card-peach | ABC Signature Rewards Card | Cash advance $2,500 (max $10,000), retail = available credit, all channels on |
| Travel card | Debit Mastercard | 5399 0000 0005 8840 | 06/31 / 884 | card-sage | Premier Savings | ATM $3,000 (max $10,000), retail $15,000 (max $50,000), all channels on. Travel notice: Singapore, day +30 to day +40 |

The card stack shows Main $486,250.00, Signature $56,579.40, Travel $1,200,000.00.

Beneficiaries (first four are the Quick transfer row, in this order)

| Name | Bank | Routing | Account | Recent |
| --- | --- | --- | --- | --- |
| Ngozi Okafor (Ngozi O.) | ABC Bank | 500000018 | ••4127 | yes |
| Tobi Okafor (Tobi O.) | Lakeview Savings | 500000063 | ••7745 | yes |
| Okafor Freight Partners (Okafor Freight) | Summit National Bank | 500000050 | ••9081 | yes |
| Ridgecrest Property Mgmt (Ridgecrest) | Northfield Bank | 500000021 | ••3368 | yes |
| Westbrook Academy | Pinecrest Credit Union | 500000047 | ••6012 | no |

Explicit transactions (Premier Checking unless noted)

| Day | Time | Description | Category | Amount | Status |
| --- | --- | --- | --- | --- | --- |
| 0 | 13:05 | Saffron House (Signature Card) | Dining | -$186.50 | Posted |
| 0 | 10:20 | Okafor Freight Partners (owner distribution) | Income | +$45,000.00 | Posted |
| -1 | 09:30 | Tobi Okafor | Transfer out | -$2,500.00 | Posted |
| -2 | 16:45 | Pixel & Plug Electronics (Signature Card) | Shopping | -$2,349.00 | Posted |
| -3 | 08:00 | Ridgecrest Property Mgmt (HOA and upkeep) | Housing | -$6,800.00 | Posted |
| -5 | 19:30 | Melody Live Tickets (Signature Card) | Entertainment | -$1,140.00 | Posted |
| -6 | 07:10 | SkyWays Airlines (Signature Card) | Travel | -$8,420.00 | Posted |
| -8 | 12:00 | To Premier Savings ••8840 (and matching +$50,000.00 on Premier Savings) | Transfer | -$50,000.00 | Posted |
| -10 | 11:00 | Westbrook Academy | Transfer out | -$14,750.00 | Posted |
| -12 | 00:05 | Interest (Premier Savings) | Interest | +$4,438.36 | Posted |
| -15 | 06:00 | Brightline Electric (autopay) | Utilities | -$412.80 | Posted |
| -20 | 10:00 | Okafor Freight Partners (owner distribution) | Income | +$45,000.00 | Posted |
| -22 | 18:15 | Harbourfront Hotel (Signature Card) | Travel | -$1,860.00 | Posted |

Last-30-day spending targets (total $32,000.00 = 80% of budget): Travel $11,200.00, Housing $6,800.00, Shopping $6,400.00, Dining $2,300.00, Entertainment $1,900.00, Groceries $1,847.21, Utilities $752.79, Health $800.00.

Bills

| Biller | Amount due | Due | Auto-pay | Scheduled |
| --- | --- | --- | --- | --- |
| Volt Mobile | $210.00 | day +5 | off | none |
| StreamNet Fiber | $129.99 | day +6 | on | none |
| Brightline Electric | $418.60 | day +10 | on | none |
| Harbor Mutual Insurance | $2,840.00 | day +14 | off | day +12 |
| Fairview County Treasurer (property tax) | $18,750.00 | day +28 | off | none |

Savings goal: "Lake house renovation", target $150,000.00, saved $112,500.00, linked to Premier Savings.

Notifications

| Day | Kind | Title | Body | Read |
| --- | --- | --- | --- | --- |
| 0 | Transactional | Money received | Okafor Freight Partners sent you $45,000.00. | no |
| 0 | Transactional | Bill due soon | Volt Mobile, $210.00, due in 5 days. | no |
| -2 | Transactional | Card used | $2,349.00 at Pixel & Plug Electronics with your Signature Card. | yes |
| -4 | Promotional | Travel rewards | Earn 3x points on travel with your Signature Card this month. | no |

Support cases: none. Documents: 3 monthly statements for checking, savings and the credit card; 1099-INT for the previous tax year, interest $48,215.40.

## 07 Shared catalog (`catalog/*`)

Banks (`catalog/banks`, used by the Add beneficiary routing lookup)

| Routing | Bank |
| --- | --- |
| 500000018 | ABC Bank |
| 500000021 | Northfield Bank |
| 500000034 | Harbor State Bank |
| 500000047 | Pinecrest Credit Union |
| 500000050 | Summit National Bank |
| 500000063 | Lakeview Savings |

Any other number that passes the checksum shows "We couldn't find this bank. Check the routing number."
Account number verification succeeds for 6 to 17 digits, except numbers ending in `0000`, which fail so the error state can be demoed.

Billers (`catalog/billers`)

| id | Name | Category |
| --- | --- | --- |
| brightline-electric | Brightline Electric | Electricity |
| city-water | City Water & Sewer | Water |
| metro-gas | Metro Gas Co. | Gas |
| streamnet | StreamNet Fiber | Internet |
| volt-mobile | Volt Mobile | Mobile |
| skyline-tv | Skyline TV | Cable TV |
| harbor-mutual | Harbor Mutual Insurance | Insurance |
| fairview-county | Fairview County Treasurer | Property tax |

Bill fetch for a biller the persona has no bill with returns an amount between $20.00 and $250.00 and a due date between day +7 and day +20, derived from a hash of the entered account number so the same input always gives the same bill.
The fetched bill is saved as `bills/{billerId}` with status `DUE`, auto-pay off and the account number's last 4 digits (`accountLast4`). Paying a bill sets status `PAID` and `paidAt`; seeded bills have neither field, so no reseed is needed.

Products (`catalog/products`)

| id | Product | Key terms |
| --- | --- | --- |
| hysa | ABC High-Yield Savings | 4.50% APY, no minimum balance, no monthly fee |
| signature-rewards | ABC Signature Rewards Card | $95 annual fee, waived in year one; 3x points on dining and travel, 1x on everything else; 21.24% to 28.24% variable APR |
| personal-loan | ABC Personal Loan | $2,000 to $50,000, 12 to 60 months, 7.99% to 17.99% APR, no origination fee |

Loan calculator defaults: $10,000 over 36 months at the APR for the persona's credit tier.

Simulated credit decision (MobileBuild.md 5.2, step 3), from the persona's credit score:

| Score | Decision | Personal loan APR | Max loan | Card limit |
| --- | --- | --- | --- | --- |
| 740 and above | Approved | 7.99% | $50,000 | $15,000 |
| 660 to 739 | Approved | 12.49% | $15,000 | $3,000 |
| Below 660 | Referred for review | none | none | none |

Card APR (decided 2026-10-20): 21.24% for 740 and above, 28.24% for 660 to 739, the two ends of the catalog's range. Opening High-Yield Savings needs no credit check and is always approved. Every persona scores 660 or more, so the referred path is shown only by tests.

Accounts opened by an approved application (decided 2026-10-20), written with the `applications` document in one batch:

| Product | Account | Starting values |
| --- | --- | --- |
| ABC High-Yield Savings | SAVINGS "High-Yield Savings" | Available and ledger $0.00, 4.50% APY |
| ABC Signature Rewards Card | CREDIT "Signature Rewards Card" | The tier's limit, nothing owed, statement $0.00, first due date 25 days after opening, auto-pay off. Also issues a card (agreed 2026-10-20), shown last in the Cards tab: Mastercard, card-graphite art, tag "Signature Rewards Card", the profile's holder name, number 5399 0000 00 + one digit + 0 + the account's last 4, with that digit chosen so the number passes the Luhn check; expiry 4 years from opening (MM/yy); a 3-digit CVV from a hash of the application id; cash advance 10% of the limit a day, up to 20% ($1,500 / $3,000 for 740 and above, $300 / $600 for 660 to 739); all channels on, not frozen. Fake, like every seeded card. |
| ABC Personal Loan | LOAN "Personal Loan" | Principal and amount owed are the chosen amount, the tier's APR, opened now. The funds are not paid into another account. |

The account id is the product id plus the first 8 characters of the application UUID (`personal-loan-3f6c2a9e`); its last 4 digits come from a hash of the application UUID, so they are fake and stable. One `TRANSACTIONAL` alert is written: "Application approved", "Your ABC Personal Loan is open." (or "Application received", "We're reviewing your ... application." when referred).

Merchant pools for generated filler (category: merchants, amount range per transaction)

| Category | Merchants | Range |
| --- | --- | --- |
| Shopping | Aster & Vine Boutique, Northwind Furniture, Urban Thread, Pixel & Plug Electronics | $25 to $400 |
| Dining | Olive & Ember, Bluebird Coffee, Campus Deli, Saffron House | $5 to $120 |
| Groceries | Fresh Market Co., Green Basket, Corner Grocer | $20 to $180 |
| Entertainment | Cineplex Grand, Melody Live Tickets, GameVault, StreamBox | $10 to $450 |
| Travel | SkyWays Airlines, Harbourfront Hotel, QuickRide | $15 to $1,300 |
| Transport | QuickRide, Metro Transit, FuelStop | $3 to $70 |
| Health | Greenleaf Pharmacy, Tony's Gym, Brightsmile Dental | $10 to $200 |
| Utilities | Billers above | Bill amount |

FAQs (`catalog/faqs`, used by the support bot; keywords in brackets)

| Question | Answer |
| --- | --- |
| How do I freeze my card? [freeze, lock, lost, stolen] | Go to Cards, choose the card and turn on Freeze Card. You can turn it off at any time. |
| How long does a standard transfer take? [ach, standard, how long] | Standard transfers arrive in 1 to 2 business days. Instant transfers arrive in minutes for a small fee. |
| What is the instant transfer fee? [fee, instant, cost] | 1.5% of the amount, at least $0.25 and at most $15.00. |
| How do I dispute a transaction? [dispute, charge, didn't make, fraud] | Open the transaction and tap Dispute Transaction. We'll keep you updated in Support. |
| Where are my statements? [statement, pdf, tax, 1099] | Go to Profile, then Statements and Documents. |
| How do I change my card limits? [limit, atm, withdrawal] | Go to Cards, choose the card and change its Daily Limits. You'll confirm it's you first. |
| Can I use my card abroad? [travel, abroad, international] | Yes. Add a Travel Notice in Cards so we don't block your payments. |
| How do I add someone to pay? [beneficiary, payee, add contact, add someone, new payee] | Go to Send Money, tap Add New Payee, and enter their routing and account number. |

Questions that match no keywords get: "I've passed this to our support team. They'll reply here within one business day."

Check deposits (MobileBuild.md 2.3)
- Written as a PENDING credit, description "Check deposit", category Deposit, with `clearsAt` 30 s after creation. Balances do not change while pending.
- When due: status POSTED, the account is credited, and a transactional notification "Deposit cleared" / "{amount} is now available in {account nickname}." is created.
- Cheque images are optional placeholders bundled in `res/drawable`; nothing is uploaded.
- Eligible accounts: checking and savings only.
- Amount per deposit: $1.00 to the profile's `checkDepositLimit`, which is $10,000.00 for samanta, marcus and grace and $100,000.00 for daniel. Above the limit, confirm is blocked with an inline message stating the limit.
- No persona has a seeded check deposit.

## 08 Seed files

```
app/src/main/assets/seed/
  catalog.json          section 07 (reference for the one-time console setup)
  personas/
    samanta.json        section 03
    marcus.json         section 04
    grace.json          section 05
    daniel.json         section 06
```

Current `seedVersion`: 3 (2026-10-19: consistency pass across all four personas). Bump on every data change.
`SeedFilesTest` checks the files against the Firestore mappers, the Luhn and ABA checksums, and the balance rules in this document.

Card limit fields: debit cards use `atmDailyLimit`/`atmMaxLimit` and `retailDailyLimit`/`retailMaxLimit`.
Credit cards store the cash advance limit in the `atm*` fields and leave `retail*` unset; the app shows retail as the linked account's available credit.

Each persona file has: `seedVersion`, `profile` (including `customerId`), `accounts`, `holdings`, `cards` (with `travelNotices`),
`beneficiaries`, `transactions`, `fillerTargets`, `bills`, `savingsGoals`, `notifications`,
`supportCases`, `documents`. Field names match ARCHITECTURE.md section 04.
Full card numbers and CVVs live only in the seed JSON and the `cards` documents of this demo. A real app would never store them.

File format (decided 2026-10-19):
- Money is whole cents (`15583200` = $155,832.00); rates are basis points (`310` = 3.10%). No decimals anywhere.
- Enum values use the codes in ARCHITECTURE.md section 04 (`CHECKING`, `PEACH`, `TRANSFER_IN`).
- Every record has a stable `id` (`checking`, `main-card`, `tx-001`), used as its Firestore document id.
- Dates are `{ "dayOffset": n, "time": "HH:mm" }`; `time` is omitted for date-only fields (due, maturity, travel dates)
  and means 00:00 local.
- `holdings` carry their `accountId`; travel notices are nested in their card.
- Not in the files, computed by the seeder: transaction `reference` (UUID from the fixed random seed), `runningBalance`,
  filler transactions, profile `seedVersion`/`createdAt`, and `{date:+n}` tokens in notification text,
  replaced with the short date of day +n ("Oct 29").
- `fillerMerchants` in `catalog.json` is read by the seeder only; it is not created in the console.
  Utilities filler uses the utility billers' names, with amounts taken from the persona's bills.
- `documents`: `statementAccountIds` (the accounts that get 3 monthly statements) and `interest1099` (cents, or null for no 1099-INT).

Values not given in the persona sections above (approved 2026-10-19):
- Notification times: the time of the related transaction where there is one, otherwise 09:00.
- Bill `status`: `DUE`, or `SCHEDULED` when `scheduledFor` is set. Grace's property tax is due and scheduled on day +25.
- Savings goals for marcus, grace and daniel start on day -90, with 13 weekly points rising evenly from 30% of the
  saved amount to the saved amount.
- Samanta's dispute: opened day -10 09:00, status `IN_REVIEW`. Customer: "I didn't make this purchase at Melody Live
  Tickets." Bank (09:05): "Thanks for letting us know. We're reviewing this charge and will update you here within
  5 business days."
- An internal transfer is two records: the debit on the source account and a matching credit named
  "From {source nickname} ••{last4}" on the destination.

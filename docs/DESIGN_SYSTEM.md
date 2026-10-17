# Design System: ABC Bank Mobile (Android)

<!--
Every visual decision lives here so the agent never invents one.
Values are taken from the reference screens (Sample Screens 1.png and
Sample Screens 2.png) and expressed for Jetpack Compose: dp for layout,
sp for text. When you want the app to look different, change this file
and the matching Kotlin token file in the same commit, never a screen.
-->

## 01 Brand identity

- Personality: calm, warm, confident.
- Tone in UI copy: plain, short sentences, sentence case, no exclamation marks.
  The sample screens break this rule ("You're doing good!", "Simlified"). Ship the corrected copy:
  "Effortless banking, simplified." and "You're on track. You've almost reached your goal."
- Feel: light, spacious, soft. White cards floating on a warm light-grey background,
  one bold black action per area, colour used for money direction and categories only.
- Wordmark: "abcbank" in lowercase with the dotted mark, on the Welcome screen and card art only.
- Visual reference: [Sample Screens 1.png](Sample%20Screens%201.png), [Sample Screens 2.png](Sample%20Screens%202.png).

## 02 Color palette

Use tokens in composables. Never a raw `Color(0x...)` outside the theme files.
Tokens live in `core/designsystem/theme/Color.kt` and are read via `AbcTheme.colors.<name>`.

| Token | Kotlin name | Value | Use |
| --- | --- | --- | --- |
| color-bg | `background` | #F2F2F2 | Screen background |
| color-surface | `surface` | #FFFFFF | Cards, sheets, list containers, chips |
| color-surface-muted | `surfaceMuted` | #EDEDED | Chart tracks, disabled fields, skeletons |
| color-border | `border` | #E6E6E6 | Dividers, input outlines, unselected chip outline |
| color-text | `text` | #111111 | Body text, headings, amounts |
| color-text-muted | `textMuted` | #6B6B70 | Timestamps, captions, secondary labels |
| color-primary | `primary` | #111111 | Primary buttons, action pills, bottom bar, "Main card" tag |
| color-on-primary | `onPrimary` | #FFFFFF | Text and icons on primary |
| color-accent | `accent` | #FF9A3C | Add/plus FAB, shopping category, highlight dots |
| color-accent-deep | `accentDeep` | #E2773A | Entertainment category, outgoing indicator |
| color-positive | `positive` | #6CC04A | Chart fills, incoming indicators, success icons |
| color-positive-text | `positiveText` | #237A3B | Incoming amounts as text (+$38.60), success copy |
| color-warning | `warning` | #B45309 | Low balance, non-blocking warnings |
| color-danger | `danger` | #D93025 | Errors, destructive actions, frozen card badge |
| color-scrim | `scrim` | #66000000 | Behind bottom sheets and dialogs |

Card art (bank card faces):

| Token | Value | Use |
| --- | --- | --- |
| card-peach | gradient #F7DCC8 to #F08A3C | Main card |
| card-sage | gradient #D3DEC2 to #6E9A5E | Signature card, Home balance header |
| card-graphite | gradient #3A3A3C to #111111 | Travel card |

If ABC Bank supplies the fluid 3D artwork from the samples, it goes in
`res/drawable-nodpi/card_art_*.webp` and replaces the gradients. Text on card art is always white with a 20% black scrim.

Rules:
- One primary colour (black). Orange and green carry meaning: orange for spending/outgoing, green for earning/incoming and success.
- Never use orange or green as the only signal. Pair with a sign (+/-) or an arrow.
- Text passes WCAG AA (4.5:1). That is why amounts use `positiveText`, not `positive`, and white text never sits on `accent`.
- Dark mode: not supported in v1. Still use tokens so it can be added by swapping the palette.

## 03 Typography

Font: Inter, bundled in `res/font/` (OFL licence, no dependency). It is the
closest open-licence match to the sample screens. Swap here if ABC Bank supplies a brand font.

| Role | Kotlin name | Size | Weight | Line height | Use |
| --- | --- | --- | --- | --- | --- |
| Display | `display` | 44sp | 300 Light | 48sp, -0.02em | Welcome headline only |
| Balance | `balance` | 40sp | 400 | 44sp, -0.02em | Wallet and card balances. Cents render at 60% size via `BalanceText` |
| H1 | `h1` | 28sp | 500 | 34sp, -0.01em | Screen titles, chart totals ($3,650) |
| H2 | `h2` | 20sp | 500 | 26sp | Section titles inside sheets |
| H3 | `h3` | 17sp | 500 | 22sp | Section headers (Transactions, Quick transfer) |
| Body | `body` | 15sp | 400 | 22sp | Default text, list titles |
| Small | `small` | 13sp | 400 | 18sp | Timestamps, captions, chip labels |
| Label | `label` | 12sp | 500 | 16sp | Button text on small pills, tags, chart legends |
| Mono | `mono` | 15sp | 400 | 22sp, Inter tabular figures | Card numbers, reference IDs |

Rules:
- All money uses tabular figures (`fontFeatureSettings = "tnum"`) so columns align.
- Sizes are in sp and must survive 200% font scale. Balances may shrink to fit, never clip or ellipsize.
- One font family only.

## 04 Spacing

Base unit: 4dp. Only use values from this scale. Tokens in `core/designsystem/theme/Spacing.kt`.

| Token | Value | Typical use |
| --- | --- | --- |
| space-1 | 4dp | Icon to text gap |
| space-2 | 8dp | Inside chips, between chips |
| space-3 | 12dp | List row vertical padding, input padding |
| space-4 | 16dp | Screen side gutter, default gap, card padding |
| space-5 | 20dp | Card padding on large cards (charts) |
| space-6 | 24dp | Between sections |
| space-8 | 32dp | Above the welcome headline, large group gaps |
| space-12 | 48dp | Bottom clearance so content scrolls above the floating bottom bar (plus bar height) |

Layout:
- Single column. 16dp side gutter. Content is full width minus gutters.
- Edge-to-edge is on. Respect status bar and navigation bar insets (`WindowInsets.safeDrawing`).
- Horizontal carousels (Features, Quick transfer, card stack) bleed to the screen edge and start at the gutter.

## 05 Shapes and elevation

Tokens in `core/designsystem/theme/Shape.kt`.

| Token | Value | Use |
| --- | --- | --- |
| radius-sm | 8dp | Inputs, small tags, chart value labels |
| radius-md | 12dp | Full-width buttons (Get started), keypad keys |
| radius-lg | 20dp | Bank cards, feature tiles |
| radius-xl | 28dp | Content cards (transactions panel, chart cards), bottom sheets (top corners), Home balance header |
| radius-full | 50% | Pills, chips, avatars, icon circles, bottom bar |

Elevation: one level only, for floating elements: bottom bar, the pill action
row that overlaps the Home header, the card-stack tag, bottom sheets.
Compose `shadowElevation = 8.dp`, ambient/spot colour black at 10%.
Content cards are flat: white on background, no border, no shadow.

## 06 Components

Reuse before creating. Check `core/designsystem/component/` first.
Each component has a `@Preview`. If a variant is not listed here, ask before adding it.

Buttons (`AbcButton`)

| Variant | Background | Content | Use |
| --- | --- | --- | --- |
| Primary | primary | onPrimary | One per screen, the main action (Get started, Confirm transfer) |
| Secondary | surface | text, 1dp border | Supporting actions (Download receipt) |
| Ghost | transparent | text | Low emphasis (View all, Filter) |
| Danger | danger | onPrimary | Destructive actions, always confirm first (Freeze card, Dispute) |

- Height 52dp, full width, radius-md, Body 500.
- States: pressed (primary lightens to #2A2A2A), disabled (40% alpha), focused (2dp accent ring for keyboard/D-pad).
- Loading: a 20dp progress indicator replaces the label; width stays fixed; taps are ignored.

Action pills
- `ActionPill` (Sample 2: Transfer, Withdraw): white pill, 56dp tall, black 40dp circle with white icon on the left, Body label. Grouped in a row that overlaps the bottom of the Home header by 28dp, with elevation.
- `IconPillButton` (Sample 1: Deposit, Send, More): black pill, 40dp tall, white icon in a white-outlined square, Small 500 label.
- `GridPill`: 56dp white circle with the grid icon, opens the More actions sheet.

Chips (`FilterChipRow`)
- Segmented filters such as All / Spendings / Earnings. 40dp tall, radius-full.
- Selected: surface background on a surfaceMuted track. Unselected: transparent with border.

Cards and lists
- `ContentCard`: surface, radius-xl, padding space-4 (space-5 for charts).
- `BankCardView`: 1.586 aspect ratio, radius-lg, card art, contactless and network icons top-left, balance, holder name, expiry, last four. Stacked cards peek 24dp below one another. Optional tag (`CardTag`, black pill, "Main card").
- `BalanceText`: big integer part, small cents. When masked shows "••••••" with the same width class. Never logs or exposes the raw value in accessibility text while masked ("Balance hidden").
- `TransactionRow`: 40dp leading circle (category icon on accent/positive tint, or contact avatar), title Body, timestamp Small muted, amount right-aligned. Outgoing "-$125.00" in text colour, incoming "+$38.60" in positiveText.
- `ContactAvatar`: 48dp circle with initials on surfaceMuted (beneficiaries have no photos, see DEMO_DATA.md section 01), name Small below.
- `ProfileAvatar`: the signed-in persona's photo (`avatar_{personaId}`) cropped to a circle with `ContentScale.Crop`; 32dp in `AbcTopBar`, 72dp on the lock screen and Profile. Falls back to initials on surfaceMuted. Decorative next to the name (contentDescription null), otherwise "Profile photo". Add contact is a 48dp accent circle with a plus.
- `FeatureTile`: 140 x 120dp, radius-lg, surface, illustration top, Body title bottom.
- `SectionHeader`: H3 title left, Ghost "View all" or "Filter" with icon right. "View all" is underlined Small muted.
- `StatusBadge`: Small label in a radius-full pill (Pending, Frozen, Scheduled).

Navigation
- `AbcBottomBar`: floating black pill, 64dp tall, 16dp from screen sides and 12dp above the navigation bar inset. Four destinations: Home, Cards, Insights, Profile. White 24dp icons, active item has a 4dp white dot below. Icons have contentDescription equal to the destination name.
- `AbcTopBar`: transparent, H3 title left or centred greeting with avatar ("Hello, Samanta"), trailing icon buttons (notifications bell with unread dot, filter).
- Flows that leave the tabs (transfer, bill pay, applications) hide the bottom bar and show a back arrow and step indicator ("Step 2 of 5").

Inputs
- `AbcTextField`: 56dp, surface, radius-sm, 1dp border, label always visible above (Small muted). Placeholder is a hint, never the label. Error: border danger, message below in Small danger.
- `AmountKeypad` and `MpinPad`: 3 x 4 grid of 64dp keys, radius-md, H1 digits; MPIN shows 6 dots that fill as digits are entered. `MpinPad` submits on the sixth digit. Wrong MPIN: dots clear and a Small danger message shows the attempts left. Lockout: keys disabled and a Small muted countdown ("Try again in 24 s"). TalkBack never reads the digits entered, only "3 of 6 entered".
- `AbcSwitch`, `AbcSlider`: Material 3 components themed with primary for active track and accent thumb highlight. Slider always shows the current value as text.

Charts (drawn with Compose Canvas, no chart library)
- `DonutChart`: 24dp stroke, rounded caps, 4dp gaps between segments, track surfaceMuted. Centre shows total / budget and percentage chip. Segment labels are white circles with Small percentage.
- `LineChart`: 2dp positive stroke with vertical gradient fill (positive 30% to 0%), dashed average line in positive at 40%, selected point with vertical marker and a black value tooltip.
- Legends use `LegendChip`: dot + Small label on surface pill.

Sheets and dialogs
- `AbcBottomSheet`: surface, top radius-xl, drag handle, scrim. Used for step-up, More actions, filters, disclaimers.
- `StepUpSheet`: title "Enter your MPIN", reason line ("Confirm it's you to send $1,500.00"), `MpinPad`, cancel. No OTP or biometric.
- Dialogs only for destructive confirmation. Always a cancel option.

States (every data-driven screen)
- Loading: shimmer skeletons in surfaceMuted matching the final layout. No full-screen spinners after first load.
- Empty: one line saying what belongs here, plus one primary action. Never a blank panel.
- Error: one line in plain words, plus Retry. Never vendor or Firebase error text.
- Offline: a Small banner "You're offline. Showing saved data." Firestore cache keeps screens readable.

## 07 Icons

- Set: Lucide, imported as Android VectorDrawables into `res/drawable/ic_<name>.xml`. One set only. No icon font, no extra dependency.
- Sizes: 16dp inline with text, 20dp in pills and buttons, 24dp in the bottom bar and top bar.
- Stroke width 1.75. Tint with tokens only.
- Bottom bar icons in the samples are filled. Use Lucide's filled-equivalent shapes or ask before drawing custom ones.
- Every icon-only control has a contentDescription. Decorative icons set it to null.

## 08 Screen sizes and accessibility

| Name | Width | Notes |
| --- | --- | --- |
| Compact (target) | 360dp to 411dp | All phones in portrait. Design and test at 360dp first. |
| Small phones | 320dp | Must remain usable: carousels scroll, pills wrap to two rows. |
| Medium and up | 600dp+ | Not designed in v1. Content is centred with max width 480dp. |

Rules:
- Portrait only (locked in the manifest for v1).
- Touch targets are at least 48dp, even when the visual is smaller.
- No horizontal scrolling except intentional carousels and the card stack.
- TalkBack: every screen is navigable, amounts are read in full ("one hundred twenty-five dollars, debit").
- Font scale 200% and display size Largest must not clip money or buttons.

## 09 Motion

- Duration: 150ms for press and toggle feedback, 250ms for sheets and chip changes, 350ms for screen transitions and the card stack swap.
- Easing: `FastOutSlowInEasing` entering, `FastOutLinearInEasing` leaving.
- Charts animate in once per screen visit (600ms sweep for the donut, left-to-right reveal for the line).
- Balance masking cross-fades in 150ms.
- Respect the system "Remove animations" setting: when the animator duration scale is 0, skip non-essential animation.

## 10 Reference screens map

How the sample PNGs map to app screens. Use them for layout and feel, not as pixel specs; tokens above win on any conflict.

| Sample | Shows | App screen |
| --- | --- | --- |
| Sample 1, left | Wordmark, stacked card art, headline, Get started | `WelcomeScreen`, then `LoginScreen` (no sample: H1 "Log in", `AbcTextField` labelled "Customer ID" (8 characters, upper case, no autocorrect, max length 8), then `MpinPad`; a wrong format or unknown ID shows the field error) and `LockScreen` (no sample: `ProfileAvatar`, "Welcome back, Samanta" in H1, `MpinPad`, Ghost "Not you?") |
| Sample 1, middle | Card stack with Main card tag, Deposit/Send/More, Quick transfer contacts, Transactions with All/Spendings/Earnings | `CardsScreen` (Cards tab) |
| Sample 1, right | Spendings donut by month, savings line chart | `InsightsScreen` (Insights tab) |
| Sample 2, phone | Greeting, wallet balance with in/out totals, action pills, Features carousel, Latest transactions | `HomeScreen` (Home tab). Withdraw is replaced by Pay bills (see PRD out of scope). |
| Sample 2, right top | Savings chart card with goal message | `SavingsChartCard` on Insights |
| Sample 2, right bottom | Card stack with balances per card | `BankCardStack` on Cards |

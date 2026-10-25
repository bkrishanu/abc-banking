# Design System: ABC Bank Mobile (Android)

<!--
Every visual decision lives here so the agent never invents one.
Values are taken from the reference screens (Sample Screens 1.png and
Sample Screens 2.png) and expressed for Jetpack Compose: dp for layout,
sp for text. When you want the app to look different, change this file
and the matching Kotlin token file in the same commit, never a screen.
-->

## 01 Brand identity

- Redesign (2026-10-24, stage 1 of the Stitch redesign, see docs/STITCH_REVIEW.md): the palette, type, shapes and
  button height below come from the Google Stitch project "Warm Private Banking". Components and screens move to the
  Stitch layouts in stages 2 and 3; until then sections 06 and 10 describe the current layouts in the new tokens.
  The "Private" and wealth-tier wording in the Stitch mockups is not used: the app stays warm retail banking for every persona.
- Personality: calm, warm, confident. Trustworthy and warm, like a friendly banker.
- Tone in UI copy: plain, short sentences, no exclamation marks.
- Capitalisation (decided 2026-10-20): labels use Title Case: buttons, quick actions, screen titles, section headers,
  field labels, tabs, chips and short captions ("Wallet Balance", "Pay Bills", "View All", "Get Started").
  Sentences stay in sentence case: messages, errors, hints, empty-state lines and notification text
  ("No investments yet", "We couldn't load your accounts."). Small words (a, an, the, and, or, of, to, in, on, for)
  stay lower case unless first.
  The sample screens break this rule ("You're doing good!", "Simlified"). Ship the corrected copy:
  "Effortless banking, simplified." and "You're on track. You've almost reached your goal."
- Feel: light, spacious, soft. White cards on a warm cream background, one deep teal action per area,
  terracotta and green used for money direction and categories only.
- Wordmark: "abcbank" in lowercase with the dotted mark, on the Welcome screen and card art only.
- Visual reference: the Stitch project (screens listed in docs/STITCH_REVIEW.md) for the redesign;
  [Sample Screens 1.png](Sample%20Screens%201.png) and [Sample Screens 2.png](Sample%20Screens%202.png) for layouts not yet redesigned.

## 02 Color palette

Use tokens in composables. Never a raw `Color(0x...)` outside the theme files.
Tokens live in `core/designsystem/theme/Color.kt` and are read via `AbcTheme.colors.<name>`.

| Token | Kotlin name | Value | Use |
| --- | --- | --- | --- |
| color-bg | `background` | #FAF8F5 | Screen background (warm cream) |
| color-surface | `surface` | #FFFFFF | Cards, sheets, list containers, chips |
| color-surface-muted | `surfaceMuted` | #EFEEEB | Chart tracks, disabled fields, skeletons, icon circles |
| color-border | `border` | #E5DFD7 | Dividers, input outlines, unselected chip outline |
| color-text | `text` | #142423 | Body text, headings, amounts (charcoal-tinted spruce, never pure black) |
| color-text-muted | `textMuted` | #5C6665 | Timestamps, captions, secondary labels |
| color-primary | `primary` | #0E3E3D | Primary buttons, the main quick action, bottom bar, "Main card" tag (deep slate teal) |
| color-on-primary | `onPrimary` | #FFFFFF | Text and icons on primary and on card-sage or card-graphite |
| color-accent | `accent` | #E07A5F | Terracotta fills: add/plus circle, shopping category, highlight dots. Never text, never under white text |
| color-accent-deep | `accentDeep` | #9A442D | Deep terracotta: entertainment category, outgoing indicator; passes AA as text |
| color-positive | `positive` | #059669 | Emerald fills: charts, incoming indicators, success icons |
| color-positive-text | `positiveText` | #047857 | Incoming amounts as text (+$38.60), success copy |
| color-warning | `warning` | #B45309 | Low balance, non-blocking warnings |
| color-danger | `danger` | #BA1A1A | Errors, destructive actions, frozen card badge |
| color-scrim | `scrim` | #66142423 | Behind bottom sheets and dialogs |
| color-primary-pressed | `primaryPressed` | #185352 | Pressed state of primary buttons (section 06) |
| color-shadow | `shadow` | #1A0E3E3D | Ambient and spot shadow of floating elements, teal-tinted (section 05) |
| color-card-scrim | `cardScrim` | #33000000 | Bottom of card art, fading up to transparent, so white text stays readable |
| color-accent-tint | `accentTint` | #F7E9E3 | Secondary button fill: Stitch's 12% terracotta tint, made opaque over background (stage 2b, 2026-10-25) |
| color-warning-tint | `warningTint` | #F6EAE1 | Warning `StatusBadge` fill: 12% warning over surface |
| color-danger-tint | `dangerTint` | #F7E4E4 | Danger `StatusBadge` fill: 12% danger over surface |
| color-positive-tint | `positiveTint` | #E1F2ED | Money-in `CategoryIcon` and Earn `RateChip` fill: 12% positive over surface (stage 2b-2) |

The tints are opaque so a badge stays readable on card art. `accentDeep` on `accentTint` is 5.4:1; badge labels stay in
`text`, because `warning` on `warningTint` is only 4.3:1.

Contrast adjustments to the Stitch theme (2026-10-24). Stitch values that fail WCAG AA were replaced with the nearest
passing shade from the same palette:

| Use | Stitch value | Contrast | Used instead | Contrast |
| --- | --- | --- | --- | --- |
| Muted text | #707978 | 3.85:1 on surfaceMuted | #5C6665 | 5.11:1 or more |
| Incoming amounts | #059669 | 3.55:1 on background | #047857 (`positiveText`); #059669 stays as the `positive` fill | 4.73:1 or more |
| Warnings | #D97706 | 3.01:1 on background | #B45309 | 4.74:1 on background, 5.02:1 on surface |
| Terracotta | #E07A5F | 2.78:1 on background | fill only; text uses `accentDeep` #9A442D | 6.12:1 |

The Stitch hand-off guide's alternative values (terracotta #C86240, 36sp balances, 24dp cards) are not used; the
Stitch project theme is the source of truth.

Card art (bank card faces). The token names are kept from v1 so no screen code changes; the values are the Stitch colours:

| Token | Value | Use |
| --- | --- | --- |
| card-peach | gradient #F4C3AE to #E07A5F (terracotta) | Main card. Text in `text` colour (5.45:1 on the darkest stop) |
| card-sage | gradient #3A6664 to #0E3E3D (deep teal) | Signature card, Home balance header. White text (6.43:1 or more) |
| card-graphite | gradient #3A3A3C to #142423 (warm charcoal) | Travel card. White text |

Fluid artwork (2026-10-20, to match Sample Screens 1): `BankCardArt` draws it in Compose Canvas from the
gradient tokens only (`CardGradient.highlight`, `base` and `deep` are derived from the two stops), softened with
a 2dp blur on Android 12+ (crisp on older versions). Patterns: `Ribbons` (broad folded ribbons sweeping down the
left side, a lit edge and a top-right glow) for card-peach; `Waves` (four layered waves across the lower half)
for card-sage and card-graphite. If ABC Bank supplies the real 3D artwork, it goes in
`res/drawable-nodpi/card_art_*.webp` and replaces the drawn patterns.
Text on card art: white with a 20% black scrim on sage and graphite; on the light card-peach face, `text` colour
and no scrim, as in the sample (white fails contrast there).

Rules:
- One primary colour (deep teal). Terracotta and green carry meaning: terracotta for spending/outgoing, green for earning/incoming and success.
- Never use terracotta or green as the only signal. Pair with a sign (+/-) or an arrow.
- Text passes WCAG AA (4.5:1). That is why amounts use `positiveText`, not `positive`, and white text never sits on `accent`.
- Dark mode: not supported in v1. Still use tokens so it can be added by swapping the palette.

## 03 Typography

Font: Plus Jakarta Sans (2026-10-24), bundled as the variable font from google/fonts in
`res/font/plus_jakarta_sans.ttf` (OFL licence in docs/licenses, no dependency). Compose uses instances of its
'wght' axis at 300, 400, 500 and 600. The PDF receipts and documents use the static
`plus_jakarta_sans_regular.ttf` and `plus_jakarta_sans_medium.ttf` from the font's upstream repository
(tokotype/PlusJakartaSans, the commit google/fonts pins), because Android's PDF writer cannot embed a variable font
and falls back to Roboto (found on the emulator, 2026-10-24). Swap here if ABC Bank supplies a brand font.

| Role | Kotlin name | Stitch role | Size | Weight | Line height | Use |
| --- | --- | --- | --- | --- | --- | --- |
| Display | `display` | display-lg | 44sp | 300 Light | 52sp, -0.02em | Welcome headline only |
| Balance | `balance` | display-lg | 44sp | 300 Light | 52sp, -0.02em | Wallet and card balances. Cents render at 60% size via `BalanceText` |
| H1 | `h1` | headline-lg | 30sp | 600 SemiBold | 38sp, -0.01em | Screen titles, chart totals ($3,650) |
| H2 | `h2` | headline-sm | 20sp | 600 SemiBold | 26sp | Section titles inside sheets |
| H3 | `h3` | title-lg | 18sp | 600 SemiBold | 24sp | Section headers (Transactions, Quick transfer) |
| Body | `body` | body-lg | 16sp | 400 | 24sp | Default text, list titles |
| Small | `small` | body-md | 14sp | 400 | 20sp | Timestamps, captions, chip labels |
| Label | `label` | label-md | 12sp | 500 | 16sp | Button text on small pills, tags, chart legends |
| Mono | `mono` | body-lg | 16sp | 400 | 24sp, tabular figures | Card numbers, reference IDs |

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

The Stitch spacing scale (4, 8, 12, 16, 24, 32dp) is the same scale, so no values changed in the redesign.

Component sizes (touch target 48dp, button 56dp (Stitch, 2026-10-24), text field 56dp, keypad key 64dp, MPIN dot 12dp,
icon 16/20/24dp, border 1dp, focus ring 2dp) live in `core/designsystem/theme/Dimens.kt` as `AbcTheme.dimens`.

Layout:
- Single column. 16dp side gutter. Content is full width minus gutters.
- Edge-to-edge is on. Respect status bar and navigation bar insets (`WindowInsets.safeDrawing`).
- Horizontal carousels (Features, Quick transfer, card stack) bleed to the screen edge and start at the gutter.

## 05 Shapes and elevation

Tokens in `core/designsystem/theme/Shape.kt`.

Radii follow the Stitch scale (sm 8, DEFAULT 16, md 24, lg 32), one step larger than v1 (2026-10-24):

| Token | Value | Stitch | Use |
| --- | --- | --- | --- |
| radius-sm | 8dp | sm | Small tags, chart value labels, document pages |
| radius-md | 16dp | DEFAULT | Text fields, image tiles, small panels |
| radius-lg | 24dp | md | Bank cards, feature tiles |
| radius-xl | 32dp | lg | Content cards (transactions panel, chart cards), bottom sheets (top corners), Home balance header |
| radius-full | 50% | full | Buttons, keypad keys, pills, chips, badges, avatars, icon circles |

Elevation: one level only, for floating elements: the Transfer button, the card-stack tag, the Welcome card stack, bottom sheets.
Compose `shadowElevation = 8.dp`, ambient/spot colour `shadow` (teal at 10%).
Content cards are flat: white on background, no border, no shadow.

## 06 Components

Reuse before creating. Check `core/designsystem/component/` first.
Each component has a `@Preview`. If a variant is not listed here, ask before adding it.

Buttons (`AbcButton`)

| Variant | Background | Content | Use |
| --- | --- | --- | --- |
| Primary | primary | onPrimary | One per screen, the main action (Get Started, Confirm Transfer) |
| Secondary | accentTint | accentDeep | Supporting actions (Download Receipt) |
| Ghost | transparent | text | Low emphasis (View All, Filter) |
| Danger | danger | onPrimary | Destructive actions, always confirm first (Freeze card, Dispute) |

- Height 56dp, full width, radius-full (a pill, stage 2b, 2026-10-25), Body 600. No outline on any variant.
- States: pressed (primary lightens to `primaryPressed` #185352), disabled (40% alpha), focused (a 2dp ring for keyboard/D-pad, drawn inside the pill: accent on Primary, primary on Secondary and Ghost, onPrimary on Danger, so it reaches 3:1).
- Loading: a 20dp progress indicator replaces the label; width stays fixed; taps are ignored.

Quick actions (stage 2b-2, 2026-10-25; replace v1's `ActionPill`, `GridPill` and `IconPillButton`)
- `QuickActionButton`: a 56dp circle with a 24dp Lucide icon and a Small label below, space-2 apart; the whole thing is one button. The main action is `emphasized` (primary fill, onPrimary icon); the rest are tonal (surfaceMuted, `text` icon). At least 72dp wide and never narrower than the label's longest word, so labels wrap between words.
- Offline: a 20dp accentTint circle with Lucide `wifi-off` (16dp, accentDeep) at the circle's top-end, and TalkBack adds "Offline". It is a cue only: the action stays available and the flow it opens deals with the connection.
- `QuickActionRow`: a `FlowRow` spreading the buttons evenly; at large font scales they wrap onto a second row.

Chips (`FilterChipRow`)
- Segmented filters such as All / Spendings / Earnings. 40dp tall, radius-full.
- Selected: surface background on a surfaceMuted track. Unselected: transparent with border.
- Labels stay on one line and shrink (down to 8sp) at large font scales rather than breaking a word. TalkBack reads the row as a selectable group of tabs.

Choice chips (`ChoiceChip`, 2026-10-20)
- One choice in a wrapping `FlowRow` group: transfer category, employment status. Small label, 40dp, radius-full, space-2 apart.
- Unselected: surface with a border. Selected: primary fill with onPrimary text. TalkBack reads them as radio buttons in a group.

Cards and lists
- `ContentCard`: surface, radius-xl, padding space-4 (space-5 for charts).
- `AbcWordmark`: the dotted mark plus "abcbank" in H2 (H3 on card art). Mark is 1.2x the text size, space-2 before the text, so it follows font scale. Tone Brand (stage 2b): the mark in accent (a graphic, so terracotta is allowed), "abc" in primary, "bank" in text. Tone OnCard: all white. Read by TalkBack as "ABC Bank".
- `BankCardArt`: the card face only. 1.586 aspect ratio, radius-lg, card-art gradient with the drawn `Ribbons` or `Waves` pattern (section 02), an optional EMV chip tinted with the card's own tones (left, 38% down, 14% of the card width), and an optional `cardScrim` fading up from the bottom edge. Content slot for text. `BankCardView` builds on it. `AbcWordmark` tone `OnLightCard` (all `text` colour) is for the peach face.
- `BankCardView`: 1.586 aspect ratio, radius-lg, card art, contactless and network icons top-left, balance, holder name, expiry, last four. Stacked cards peek 24dp below one another, or one Small line plus space-1 above and below when that is taller (large font scales). Optional tag (`CardTag`, primary pill, "Main card").
  Built 2026-10-20: Lucide `nfc` plus two rings for Mastercard or "VISA" in Label; a Danger `StatusBadge` "Frozen" next to the marks (the top-right corner belongs to the tag); "Balance" in Small over `BalanceText` in H1; while revealed, the full number in Mono and "CVV 314" in place of "•••• 3144". Text is dark on card-peach, white elsewhere. At large font scales the face grows taller (it keeps the ratio as a minimum) and the expiry and number move under the name.
- `BalanceText`: big integer part, small cents. When masked shows "••••••" with the same width class. Never logs or exposes the raw value in accessibility text while masked ("Balance hidden").
- `TransactionRow`: 40dp leading circle (category icon on accent/positive tint, or contact avatar), title Body, timestamp Small muted, amount right-aligned. Outgoing "-$125.00" in text colour, incoming "+$38.60" in positiveText.
- `ContactAvatar`: 48dp circle with initials on surfaceMuted (beneficiaries have no photos, see DEMO_DATA.md section 01), name Small below.
- `ProfileAvatar`: the signed-in persona's photo (`avatar_{personaId}`) cropped to a circle with `ContentScale.Crop`; 32dp in `AbcTopBar`, 72dp on the lock screen and Profile. Falls back to initials on surfaceMuted. Decorative next to the name (contentDescription null), otherwise "Profile photo". Add contact is a 48dp accent circle with a plus.
- `FeatureTile`: at least 140 x 120dp, radius-lg, surface, illustration top, Body title bottom. At large font scales it widens to its title's longest word so words never break.
- `FrozenCardBanner` (stage 2b-2, Stitch adopted extra): under a frozen card, on accentTint with radius-xl and padding space-4: Lucide `snowflake` in a 40dp accentDeep circle, "This card is frozen" in Body 600 and what that means in Small muted, then Primary "Unfreeze" (unfreezing needs no confirmation, as with the switch). The frozen face itself is drawn in greys of the same luma (`CardGradient.desaturated()`).
- `RateChip`: a Label in a radius-full pill, space-2 by space-1. Earn (APY): positiveText on positiveTint. Cost (APR): accentDeep on accentTint.
- `SectionHeader`: H3 title left, Ghost "View All" or "Filter" with icon right. "View All" is underlined Small muted.
- `StatusBadge` (stage 2b, 2026-10-25, Stitch "• Posted"): a 6dp tone dot, space-1, and a Small label in `text`, in a radius-full pill (Pending, Frozen, Scheduled). Neutral: textMuted dot on surfaceMuted. Warning (Disputed): warning dot on warningTint. Danger (Frozen): danger dot on dangerTint. The word carries the meaning, so colour is never the only signal.
- `TransactionLeadingIcon`: picks `InitialsAvatar` for people (Transfer in/out) or `CategoryIcon` for businesses; Home and the ledger share it. `categoryLabel()` gives each category's Title Case name.
- `AdaptiveRow`: a label on the start side and an amount on the end side (account and transaction rows). Side by side when the amount fits unwrapped and no word of the label has to break; otherwise the amount moves below the label, end-aligned, so money never splits across lines at 200% font scale.
- `CategoryIcon`: the `TransactionRow` leading circle for businesses, tonal as in Stitch (stage 2b-2): accentDeep icon on accentTint for money out, positiveText icon on positiveTint for money in. People (Transfer in/out) get `InitialsAvatar` instead: 40dp, initials in Label on surfaceMuted.

Navigation
- `AbcBottomBar` (Stitch redesign, stage 2a, 2026-10-24): docked to the bottom edge on `background` with a 1dp `border` line on top; it pads itself for the navigation bar inset. Four destinations, Home, Cards, Insights, Profile, each a 24dp icon over a Label, at least 64dp tall; the selected one is `primary` and SemiBold, the others `textMuted`. TalkBack reads each as a tab with its label. `TransferButton` floats centred space-3 above it: a primary pill, at least 56dp tall, padding space-6, Lucide `arrow-left-right` (20dp) and "Transfer" in Body SemiBold, floating elevation; it opens Send Money. `bottomBarClearance()` (bar, gap, button and space-6) is what tab screens add at the bottom so their last row scrolls clear of both.
  Built 2026-10-20 with Home, Cards and Profile; Insights joined the same day with its screen. Icons are Lucide `house`, `credit-card`, `chart-column`, `user` (outline: Lucide has no filled set). Tab screens add `LocalBottomBarClearance` at the bottom so content scrolls above the bar and the Transfer button.
- `AbcTopBar` (stage 2a, 2026-10-24): transparent, at least 48dp tall, two forms. `GreetingTopBar` (Home): 32dp `ProfileAvatar`, space-3, the greeting in H3; avatar and greeting open Profile. `TitleTopBar` (Cards, Insights, Profile): the screen title in H1 as the heading. Both end with the bell: Lucide `bell` (20dp) in a 40dp `surface` circle with an 8dp accent unread dot at its top-end; TalkBack reads "Notifications, 4 unread". On the tabs the bell comes from `LocalTabBell`, which `MainTabs` provides from `TabBellViewModel` (the unread count of both notification kinds), so every tab shows the same count; outside the tabs `TitleTopBar` has no bell.
- Flows that leave the tabs (transfer, bill pay, applications) hide the bottom bar and show a back arrow and step indicator ("Step 2 of 5") with a step progress bar (`FlowScaffold`).

Inputs
- `AbcTextField`: 56dp, surface, radius-md (16dp, Stitch), 1dp border, padding space-4 across, label always visible above (Small muted). Focused: 2dp primary border. Placeholder is a hint, never the label. Error: border danger (2dp while focused), message below in Small danger.
- `AmountKeypad` and `MpinPad`: 3 x 4 grid of 64dp pill keys (radius-full, stage 2b), H2 digits; MPIN shows 6 dots that fill as digits are entered. Keys are 64dp tall, a third of the pad width each, 12dp (space-3) gaps, surface on background. `AmountKeypad` bottom row: decimal point and delete on surfaceMuted, 0 on surface; it reports keys and `AmountInput` decides what is valid. `MpinPad` bottom row: blank, 0, delete (Lucide `delete`, 24dp, on a surfaceMuted key). Dots are 12dp circles, 16dp (space-4) apart; filled in primary, empty as a 1dp textMuted ring (a surfaceMuted fill was tried and is invisible on the background). `MpinPad` submits on the sixth digit. Wrong MPIN: dots clear and a Small danger message shows the attempts left. Lockout: keys disabled and a Small muted countdown ("Try again in 24 s"). TalkBack never reads the digits entered, only "3 of 6 entered".
- `AbcSwitch`, `AbcSlider`: Material 3 components themed with primary for active track and accent thumb highlight. Slider always shows the current value as text.
  Built as rows: `AbcSwitchRow` (optional 20dp icon, Body title, Small muted subtitle, the whole row toggles) and `AbcSliderRow` (label and value text in an `AdaptiveRow` above the slider; TalkBack reads the money value).

Charts (drawn with Compose Canvas, no chart library)
- `DonutChart`: 24dp stroke, rounded caps, 4dp gaps between segments, track surfaceMuted. Centre shows total / budget and percentage chip. Segment labels are white circles with Small percentage.
- `LineChart`: 2dp positive stroke with vertical gradient fill (positive 30% to 0%), dashed average line in positive at 40%, selected point with vertical marker and a primary value tooltip.
- Legends use `LegendChip`: dot + Small label on a pill; inside a surface card the pill is the background colour, so it shows.
- Built 2026-10-20 in `Charts.kt`. Donut segment labels sit on the ring's centre line and grow into pills at large font sizes. Line chart labels are centred under their points and kept inside the chart; a label that would overlap the one before it is left out. Tapping the line chart selects the nearest point. TalkBack reads one summary per chart (`cd_spending_donut`, `cd_savings_chart`), not the drawing. `LocalChartEntrance` turns the entrance off for screenshot tests, which capture the first frame.

Sheets and dialogs
- `AbcBottomSheet`: surface, top radius-xl, drag handle, scrim. Used for step-up, More actions, filters, disclaimers.
- `StepUpSheet`: title "Enter Your MPIN", reason line ("Confirm it's you to send $1,500.00", "...to see your card details", "...to change your card limits", "...to add a payee"), `MpinPad`, Ghost Cancel, in an `AbcBottomSheet`. Wrong entries and the lockout countdown show on the pad as at login. Driven by `StepUpController` (`core/security`). No OTP or biometric.
- Dialogs only for destructive confirmation. Always a cancel option.
- `AbcDatePickerDialog` (2026-10-20): the Material date picker in a dialog for calendar days, OK and Cancel in text colour; the caller says which days can be picked (travel notices: today or later; scheduled bills: tomorrow through the due date).
- `AbcConfirmDialog`: a `ContentCard` (padding space-5) in a dialog: H2 title, Body textMuted text, Danger confirm and Ghost cancel, full width and stacked. Back and tapping outside cancel.

States (every data-driven screen)
- Loading: shimmer skeletons in surfaceMuted matching the final layout. No full-screen spinners after first load.
- Empty: one line saying what belongs here, plus one primary action. Never a blank panel.
- Error: one line in plain words, plus Retry. Never vendor or Firebase error text.
- Offline: a Small banner "You're offline. Showing saved data." Firestore cache keeps screens readable.
- Shared pieces in `ScreenStates.kt`: `SkeletonBlock` (pulsing surfaceMuted block), `OfflineBanner` (Lucide `wifi-off` in accentDeep and a Small textMuted line on a surface pill; with a sync time it reads "You're offline. Showing data saved at 3:14 pm.", which screens pass once they know it), `ErrorState` (Body line plus Primary "Try Again"). Offline with nothing cached shows "You're offline. Connect to the internet and try again."

## 07 Icons

- Set: Lucide, imported as Android VectorDrawables into `res/drawable/ic_<name>.xml`. One set only. No icon font, no extra dependency.
- Sizes: 16dp inline with text, 20dp in pills and buttons, 24dp in the bottom bar and top bar.
- Stroke width 1.75. Tint with tokens only.
- Bottom bar icons in the samples are filled. Use Lucide's filled-equivalent shapes or ask before drawing custom ones.
- Every icon-only control has a contentDescription. Decorative icons set it to null.

Launcher icon (chosen 2026-10-17, concept B; recoloured for the Stitch redesign in stage 2b, 2026-10-25):
- Adaptive icon in `res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`, all vectors, no PNGs (minSdk 26).
- Background `drawable/ic_launcher_background.xml`: card-sage gradient #3A6664 to #0E3E3D, top-left to bottom-right, as on the Home balance header.
- Foreground `drawable/ic_launcher_foreground.xml`: the abcbank dotted mark in accent #E07A5F, as in the wordmark. 13 rounded squares (corner radius 28% of side) on the diagonals c + r = 3, 5, 7 of a 6 x 6 grid of 7.6dp cells, centred on the 108dp canvas and inside the 66dp safe zone. Size and opacity grow towards the top-right (side 24% to 92% of a cell, opacity 48% to 100%), traced from the wordmark in Sample Screens 1.
- Monochrome `drawable/ic_launcher_monochrome.xml`: the same mark, for Android 13+ themed icons.
- If ABC Bank supplies a brand icon, replace these three drawables only.

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
- Durations live in `core/designsystem/theme/AbcMotion.kt`. Screen changes fade in and out over 350ms.

## 10 Reference screens map

How the sample PNGs map to app screens. Use them for layout and feel, not as pixel specs; tokens above win on any conflict.

`WelcomeScreen` layout (approved 2026-10-17):
- Brand `AbcWordmark` at top-left, space-4 below the status bar.
- Card stack, space-6 below: two `BankCardArt` at 78% of content width, each with an H3 wordmark bottom-right and the floating shadow (section 05). Front: card-peach `Ribbons` with the chip and a dark wordmark, no scrim, top-left, rotated -8deg. Back: card-sage `Waves` with a white wordmark, right-aligned, 48dp (space-12) lower, rotated -4deg. Decorative, hidden from TalkBack.
- Headline "Effortless banking, simplified." in Display, space-8 below the cards. Subtext "Everyday banking, simplified, in your pocket." in Body textMuted, space-3 below.
- Primary "Get Started" pinned to the bottom; everything above it scrolls.

`LoginScreen` layout: back arrow (Lucide `arrow-left`, 24dp in a 48dp target), H1 "Log In", space-6, customer ID `AbcTextField` (placeholder "8 letters or numbers"), space-6, H3 "Enter Your MPIN", space-4, `MpinPad`. The pad stays disabled until the ID is recognised, then the keyboard closes.

`HomeScreen` layout (2026-10-20):
- `GreetingTopBar` (tapping the avatar or greeting opens Profile), then the offline banner when reading from the cache.
- Balance header, space-4 below: card-sage gradient, radius-xl, padding space-5. "Wallet Balance" (Small) with the eye toggle (Lucide `eye-off` to hide, `eye` to show, 48dp target), `BalanceText`, then two chips: arrow-up "+$x" in positiveText and arrow-down "-$y" in text, Label on surface pills, last-30-day money in and out of the wallet account. Header text and the eye icon are `onPrimary` (2026-10-24): card-sage is now deep teal, where white passes (6.43:1 or more) and `text` would not.
- space-4, a `QuickActionRow` (stage 2b-2): Send (`send`, emphasized, opens Send Money), Pay Bills (`receipt`), Deposit (`landmark`, opens Deposit Check), More (`layout-grid`, opens More Actions). Send, Pay Bills and Deposit carry the offline badge while Home shows cached data.
- space-6, `SectionHeader` "Features" with "View All" (opens More Actions), then a carousel of `FeatureTile`s, space-3 apart, bleeding to the screen edges (2026-10-20): Instant Bank Transfers (`arrow-up-right` on accent), Spending Analytics (`chart-column` on positive, opens Insights), Deposit a Check (`arrow-down` on surfaceMuted) and Explore Products (`layout-grid` on accentDeep). Sample 2's Multi-currency tile is out of scope (PRD section 07). Tile illustrations are a 48dp tinted circle with a 24dp icon.
- space-6, `SectionHeader` "Accounts", one `ContentCard` with a row per account (dividers between): nickname and "••last4" on the start side; the amount in Body tabular on the end side with Small muted details: checking "Available" or "Ledger $x" when a pending item makes them differ, savings APY, card current balance with available, statement and due date, line of credit amount owed with available and APR, term deposit principal with APY and maturity date, investments portfolio value. A low balance adds "Below $500.00" in warning.
- With no investment account (marcus): a `ContentCard` "No investments yet" with Secondary "Explore Products".
- space-6, `SectionHeader` "Latest Transactions" with "View All", one `ContentCard` with the 5 newest `TransactionRow`s on any account ("Today, 3:14 pm", "Yesterday, 9:21 pm", "Oct 16, 7:00 am", plus " · Pending").
- Eye toggle masks every amount on the screen; the choice lasts for the session (`SessionManager`, in memory).
- Loading: skeleton of the top bar, header and three cards. Error: `ErrorState` "We couldn't load your accounts." Empty (no accounts): "Your accounts will show here." with Explore Products.
- The grid pill opens the More Actions sheet (2026-10-20; `MoreActionsSheet` in the design system, owned by the tabs so the Cards tab's More opens the same sheet): H2 "More Actions" and rows (40dp surfaceMuted icon circle, Body label): Send Money, Deposit Check, View Statements, Explore Products, Insights, Manage Cards. Rows are added as features are built; destinations that do not exist are never listed.

`TransactionsScreen` layout (2026-10-20): back arrow, H1 "Transactions" (or the account nickname with "••last4" in Small muted when opened from an account), space-4, `AbcTextField` "Search" (placeholder "Merchant name"), space-4, `FilterChipRow` All / Spendings / Earnings, then a `SectionHeader` with the date range as its title and "Filter" (or "Filter (2)") opening an `AbcBottomSheet`: H2 "Filter Transactions", H3 "Date Range" with radio rows, H3 "Categories" with checkbox rows, Primary "Done", Ghost "Clear Filters"; changes apply as they are made. Groups: Small muted header (Today, Yesterday, Last 30 Days, Earlier) over the rows drawn as one content card (`xlTop`/`xlBottom` shapes on the first and last row, dividers between) in a lazy list. Rows show the time in Today and Yesterday, otherwise "Oct 16, 7:00 am", plus " · Pending". Empty period: "No transactions in this period." with Secondary "Show All Time". Filters hide everything: "No transactions match your filters." with Secondary "Clear Filters".

`TransactionDetailScreen` layout (2026-10-20): back arrow, H1 "Transaction Details", space-6, `ContentCard` (padding space-5) centred: `TransactionLeadingIcon`, merchant in H2, signed amount in H1 tabular (positiveText for money in, masked with balances), `StatusBadge`s Pending and Disputed. space-4, `ContentCard` of label (Small muted) / value (Body) rows with dividers: Date, Time, Account ("Everyday Checking ••3144"), Category, Status, Running Balance, then Reference in Mono, moved under its label by `AdaptiveRow` when long. space-6, Secondary "Download Receipt" (loading while the PDF is made; Small danger "We couldn't create the receipt. Try again." on failure), then Danger "Dispute Transaction" for disputable charges, confirmed with `AbcConfirmDialog` ("Dispute This Transaction?", Dispute / Cancel). A disputed charge shows Small textMuted "This charge is under review. We'll update you in Support." instead. Not found: "We couldn't find this transaction."

`InsightsScreen` layout (2026-10-20): space-4, H1 "Insights", space-6, then two `ContentCard`s space-4 apart. Spending: H3 "Spending" with the date range ("Sep 21 – Oct 20") in Small muted on the right (below at large font sizes), `FilterChipRow` Last 30 Days / This Month / Last Month, the `DonutChart` full width (segments accent, accentDeep, positive for the top two categories and Other, sized against the monthly budget so the track shows what is left; centre: whole-dollar total in H2 tabular, "of $18,900 budget" in Small muted, the budget percentage as a Label chip on surfaceMuted), then `LegendChip`s. No spending: "No spending in this period." Savings: whole-dollar saved amount in H1, "Total Saved So Far" in Small muted, the `LineChart` of weekly points with month labels and the latest point selected, then a row with Lucide `smile` (positiveText on a 40dp surfaceMuted circle), the goal name, "$3,650 of $4,000." plus the progress message in Small muted, and the percentage in Label. No goal: "No savings goal yet." Amounts mask with the Home eye toggle.

Bill pay layouts (2026-10-20): Pay Bills: `SectionHeader` "Your Bills", one `ContentCard` (padding space-2) of rows with dividers (Lucide `receipt` in a 40dp surfaceMuted circle, biller in Body, the due, scheduled or paid date in Small muted, an Auto-Pay badge, the amount in Body tabular on the right), Secondary "Add a Biller". Add a Biller: `AbcTextField` "Search Billers", then the directory rows (name, type in Small muted, "Added" badge for billers already added); choosing one shows its type, `AbcTextField` "Account Number", Primary "Fetch Bill", then summary rows Amount Due, Due Date, Account and Primary "Save Biller" with the MPIN note. Bill: `ContentCard` with "Amount Due" (Small muted), the amount in the Balance style and the due line, a Scheduled badge when set; Pay From field; a card of summary rows (Type, Account, Due Date) with the Auto-Pay `AbcSwitchRow`; Primary "Pay Now" with Secondary "Schedule Payment" (or Ghost "Cancel Scheduled Payment"). After paying, a card says "Paid. It will show in your transactions." with the reference, and Secondary "View Transaction" replaces the buttons.

- `ChatBubble` (2026-10-20): the customer's messages right-aligned in primary with onPrimary text, the bank's left-aligned in surface; radius-lg, Body, padding space-4 by space-3, at most 80% of the width, an optional Small muted caption below (the matched question, "Added to Your Cases", or the time).

Support layouts (2026-10-20): Help and Support: a `ContentCard` with Chat With Us (Lucide `message-circle`) and Dispute a Transaction (`receipt`) rows; `SectionHeader` "Your Cases" with rows (topic in Body, an In Review or Open badge in warning, Resolved in neutral, the last message in Small muted, two lines); `SectionHeader` "Common Questions" with rows that open to show the answer in Small muted. Chat: `ChatBubble`s from a greeting, "Try Asking" with up to four FAQ questions as `ChoiceChip`s before the first message, and at the bottom an `AbcTextField` "Your Question" with a 56dp primary send button (Lucide `send`). Case: the topic as H1, its status badge and the thread with dates. Profile's menu adds Help and Support (`life-buoy`) and Reset Demo Data (`rotate-ccw`, always last), confirmed with `AbcConfirmDialog` ("Reset Demo Data?", Reset / Cancel); a Small line below the menu says "Resetting the demo data…", "Demo data reset." or, in danger, the failure.

Documents layouts (2026-10-20): Statements and Documents: `FlowScaffold` without steps; per month a `SectionHeader` ("September 2026") over one `ContentCard` (padding space-2) of rows (Lucide `file-text` in a 40dp surfaceMuted circle, "Everyday Checking ••3144" in Body, "Closing balance $x" or "Balance owed $x" in Small muted); `SectionHeader` "Tax Documents" with the 1099-INT row or the no-form line; the demo note in Small muted. Viewer: the document title in H1, pages as white images at full width with radius-sm and space-3 between, a skeleton while building, `ErrorState` on failure, Secondary "Share". Profile gains a menu card (same rows) with Statements and Documents.

`DepositCheckScreen` layout (2026-10-20): `FlowScaffold` "Deposit Check", Deposit To field, the amount in the Balance style (typed amounts group thousands through `MoneyFormatter.formatTyped`), the limit or its inline error in Small, `AmountKeypad`, "Check Images (Optional)" with two tiles (surface, radius-md, 1dp border, primary when attached; a surfaceMuted image slot with Lucide `plus`, or the bundled `cheque_front` / `cheque_back` line art tinted in text colour), "Demo images only. Nothing is uploaded." in Small muted, Primary "Deposit Check". After confirming: "Deposit Received", a Body line, summary rows and the reference, Secondary "View Transactions", Primary "Done".

`NotificationsScreen` layout (2026-10-20): `FlowScaffold` without steps, H1 "Notifications", `FilterChipRow` Transactional / Promotional with unread counts, then a `SectionHeader` ("Alerts" or "Offers") with "Mark All as Read" while any are unread, and one `ContentCard` (padding space-2) of rows with dividers: an 8dp accent dot for unread, the title in Body (SemiBold when unread), the body and the time label in Small muted. TalkBack adds "Unread". Empty: "No alerts yet. Transfers, card use and low balances will show here." or "No offers right now."

`FlowScaffold` (2026-10-20) is the frame for every flow that leaves the tabs (transfer, products, applications): back arrow (none on receipts and confirmations), "Step n of N" when the screen is a numbered step with a progress bar under it (stage 2a: one 4dp radius-full segment per step, space-1 apart, finished and current steps in primary, the rest surfaceMuted, hidden from TalkBack), H1 title, scrolling content, buttons pinned at the bottom with space-4 padding. `SummaryRow` / `SummaryRows` are its label (Small muted) and value (Body) rows with dividers.

Products layouts (2026-10-20): Explore Products: one `ContentCard` (padding space-5) per product, space-4 apart: 40dp surfaceMuted icon circle (Lucide `landmark`, `credit-card`, `banknote`) with the name in H3, the key terms in Body lines, the offer in Small positiveText, then Secondary "Apply Now" (or "Check Your Rate" for the loan), or an "Opened" badge with Secondary "View Account", or an "Under Review" badge. Loan calculator: H1 product name, `ContentCard` with "Monthly Payment" (Small muted), the amount in the Balance style and the APR; a card with `AbcSliderRow` Loan Amount and Term; summary rows Total Interest and Total Repayment; the estimate note in Small muted; Primary "Apply for This Loan". Application: Step 1 "Confirm Your Details" (intro Body muted, summary rows, Employment Status `ChoiceChip`s with the role below in Small muted, `AbcTextField` Annual Income), Step 2 "Terms and Disclosures" (one card of Body paragraphs with dividers, a checkbox row, Primary "Agree and Continue"), Step 3 "Checking Your Details" (40dp progress indicator, "This only takes a moment.") then "You're Approved" with the offer rows and Primary "Accept and Open Account", or "We Need a Closer Look" with "Submit for Review"; Step 4 "You're All Set" (or "Application Received") with summary rows, the reference in Mono, a next-step note, Secondary "View Account" and Primary "Done".

Transfer flow layouts (2026-10-20): every step has a back arrow, "Step n of 5" (or "of 4" between own accounts) in Small muted on the right, H1 title, scrolling content and its button pinned at the bottom with space-4 padding. Send Money: `ContentCard` row Between My Accounts (`arrow-left-right` in a 40dp surfaceMuted circle, "Checking, savings and line of credit" in Small muted), `SectionHeader` Saved Payees, one `ContentCard` of rows (`InitialsAvatar`, name, "Bank ••1180" in Small muted, "Recent" on the right), Secondary "Add New Payee". Add New Payee: `AbcTextField`s Full Name, Routing Number (the bank name in Small positiveText below once found), Account Number, the MPIN note in Small muted, Primary "Save Payee". Enter Amount (Move Money between own accounts): From (and To) fields that open an account sheet of radio rows when there is a choice, with "Available $x" or "Available to Draw $x" in Small muted; P2P shows the payee instead of To. The amount in the Balance style, tabular, textMuted at $0.00, with the inline error in Small danger (or what is available, or the Instant fee) below; `AmountKeypad` full width; then for P2P Memo and Category chips (Small, radius-full, 40dp, surface with border, selected primary fill with onPrimary text). Payment Speed: two `ContentCard` radio options, the selected one with a 2dp primary border, H3 title, Small muted delivery, Label price ("Free" or "$3.75 fee"), then the fee rule in Small muted. Review Transfer: `ContentCard` with Total and the amount in the Balance style, then summary rows (label Small muted, value Body). Primary "Confirm Transfer". Disclaimer: `AbcBottomSheet` with H2 "Before You Send", Body text, Primary "Agree and Send", Ghost "Cancel". Receipt: no back arrow, H1 "Transfer Sent" or "Money Moved", `ContentCard` with a 72dp positive circle and `check`, the amount in the Balance style and "Sent to Mark Robinson", summary rows with Reference in Mono, Secondary "View Transaction", Primary "Done".

`CardsScreen` layout (2026-10-20): space-4, H1 "Cards", space-6, the card stack (selected card in front with its `CardTag` overlapping the top edge; the others peek 24dp below it, drawn from the front card's bottom edge, with their tag and balance in Small; tapping one brings it forward with a 350ms crossfade), space-4, a `FrozenCardBanner` when the selected card is frozen, then a `QuickActionRow`: Send (`send`, emphasized, opens Send Money), Deposit (`landmark`, opens Deposit Check) and More (`layout-grid`, opens More Actions), with the offline badge on Send and Deposit; space-4, `SectionHeader` "Quick Transfer" with "View All" (Send Money) over a `ContentCard` holding a scrolling row of the recent payees (40dp `InitialsAvatar`, short name in Small; tapping one opens the transfer on the amount step with that payee) and an Add button (`plus` on an accent circle, opens Add New Payee); space-4, Secondary "Show Card Details" / "Hide Card Details". Then `ContentCard`s space-4 apart: Freeze Card switch (snowflake, confirmed with `AbcConfirmDialog` "Freeze This Card?"); Daily Limits (H3, `AbcSliderRow` ATM Withdrawals or Cash Advance, Purchases or the available-credit note, Primary "Save Limits" enabled only when changed); Channels (International, Online, Contactless switches with globe, monitor-smartphone and nfc icons); Travel Notices (rows with plane icon, destination, "Nov 9 – Nov 23, 2026" and an Active badge; Secondary "Add Travel Notice" opens a sheet with Destination, Start Date and End Date buttons that open the Material date picker, and "Save Notice"); then Secondary "Add to Google Wallet", replaced by a card with a check, "Added to Google Wallet" and the demo note.

`ProfileScreen` layout (interim, 2026-10-20; a tab since the bottom bar, so no back arrow): H1 "Profile", space-6, `ContentCard` (padding space-5) with the 72dp `ProfileAvatar`, the name in H2 and "Customer ID SAMJOH01" in Small muted, all centred; space-6, Secondary "Log Out" (shown in every state but loading, so a profile that fails to load never traps the user). Settings, documents, support and Reset Demo Data join it with their features, and it becomes the Profile tab when the bottom bar arrives.

| Sample | Shows | App screen |
| --- | --- | --- |
| Sample 1, left | Wordmark, stacked card art, headline, Get Started | `WelcomeScreen`, then `LoginScreen` (no sample: H1 "Log In", `AbcTextField` labelled "Customer ID" (8 characters, upper case, no autocorrect, max length 8), then `MpinPad`; a wrong format or unknown ID shows the field error) and `LockScreen` (no sample, built 2026-10-20: space-12, 72dp `ProfileAvatar`, "Welcome back, Samanta" in H1 centred, "Enter your MPIN to continue." in Body muted, space-8, `MpinPad`, Ghost "Not You?") |
| Sample 1, middle | Card stack with Main card tag, Deposit/Send/More, Quick transfer contacts, Transactions with All/Spendings/Earnings | `CardsScreen` (Cards tab) |
| Sample 1, right | Spendings donut by month, savings line chart | `InsightsScreen` (Insights tab) |
| Sample 2, phone | Greeting, wallet balance with in/out totals, quick actions, Features carousel, Latest transactions | `HomeScreen` (Home tab). Withdraw is replaced by Pay Bills (see PRD out of scope). |
| Sample 2, right top | Savings chart card with goal message | `SavingsChartCard` on Insights |
| Sample 2, right bottom | Card stack with balances per card | `BankCardStack` on Cards |

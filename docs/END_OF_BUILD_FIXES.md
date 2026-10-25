# End-of-Build Fixes

<!--
Items found during the Stitch redesign that are deliberately left for the end of the overall build (decided
2026-10-25). Add to this list as stages find more; do not fix these inside a screen stage. When the screens are done,
go through the list with the user, one branch per fix (or a small group of related fixes), and ask before each.
-->

| # | Item | Found in | What happens | Likely fix | Touches |
| --- | --- | --- | --- | --- | --- |
| 1 | Offline sync time | 2b-2 | `OfflineBanner` can show "Showing data saved at 3:14 pm", but no repository records when data last came from the server, so no screen passes a time. | Record the time of the last server (not cache) snapshot per repository and pass it to the banner. Ask first: it changes the data layer. | Repositories, `OfflineBanner` callers |
| 2 | Period tabs clip at 200% font | Insights | `FilterChipRow` shows "Last 30" for "Last 30 Days": the label wraps to a cut-off second line instead of shrinking. | `softWrap = false` on the label so `autoSize` shrinks it. | `FilterChipRow` (Insights, Transactions, Notifications) |
| 3 | Bottom bar labels clip at 200% font | Insights | At 360dp and 200% the selected "Insights" tab reads "Insigh…". | Let the label shrink (as the chips will) or allow two lines. | `AbcBottomBar` |
| 4 | Tab lost after a configuration change | Insights | After changing density and font scale on the emulator while on Insights, Back from Transactions landed on Home. Without the change it returns to Insights. | Check that the tab `NavController` state survives activity recreation. Reproduce first; it may be older than the redesign. | `MainTabs` |
| 5 | Range calendar font | Cards | `AbcDateRangePicker` month and day labels use Material's default type, not Plus Jakarta Sans. | Map the Material 3 typography to the app's type scale in the theme. | Theme, `AbcDateRangePicker`, `AbcDatePickerDialog` |
| 6 | Keypad below the fold on P2P | Send Money | With the Stitch order (chosen by the user) the amount keypad sits under the memo and category chips, so a P2P amount needs a scroll on a 1080 x 2400 phone. | Revisit if it feels slow in a demo: move the keypad back under the amount (one move in `AmountForm`). | `TransferAmountScreen` |
| 7 | TalkBack on a real phone | All stages | Labels were checked through the accessibility tree only. | Listen once with TalkBack on a real phone across the main flows. | None expected |
| 8 | Bill due badge wraps at 200% font | Pay Bills | At 360dp and 200% the "Due in 5 Days" badge wraps onto two lines next to the amount. Readable, nothing clipped. | Move the amount under the biller at large font scales (as `AdaptiveRow` does elsewhere) so the badge has the width. | `BillPayScreens` |

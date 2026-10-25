package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.CardGradient
import com.abcbank.mobile.core.model.CardArt
import com.abcbank.mobile.core.model.CardNetwork
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.Money
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** What one card face shows. [secrets] is set only while the card details are revealed. */
data class BankCardFace(
    val art: CardArt,
    val network: CardNetwork,
    val holderName: String,
    val last4: String,
    val expiry: YearMonth,
    val balance: Money,
    val frozen: Boolean,
    val secrets: CardSecrets? = null
)

/**
 * `BankCardView` from DESIGN_SYSTEM.md section 06: card art with the contactless and network marks
 * top-left, the balance, holder name, expiry and last four. While revealed it shows the full
 * number and CVV instead; TalkBack reads them only then. Frozen cards carry a Frozen badge.
 */
@Composable
fun BankCardView(face: BankCardFace, balanceHidden: Boolean, modifier: Modifier = Modifier) {
    val colors = AbcTheme.colors
    val light = face.art == CardArt.PEACH
    // Dark text on the light peach face, white on sage and graphite (section 02).
    val ink = if (light) colors.text else colors.onPrimary
    val spacing = AbcTheme.spacing
    val description = cardDescription(face, balanceHidden)
    BankCardArt(
        // A frozen card loses its colour, as in Stitch; the Frozen badge says why.
        gradient = if (face.frozen) face.art.gradient().desaturated() else face.art.gradient(),
        pattern = if (light) CardArtPattern.Ribbons else CardArtPattern.Waves,
        scrim = !light,
        // At large font scales the face grows taller instead of clipping its text.
        growToFit = true,
        modifier = modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.space4),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_nfc),
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
                Spacer(Modifier.width(spacing.space2))
                NetworkMark(face.network, ink)
                // Next to the marks, not top-right: that corner belongs to the card's tag.
                if (face.frozen) {
                    Spacer(Modifier.width(spacing.space2))
                    StatusBadge(stringResource(R.string.card_frozen), tone = BadgeTone.Danger)
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.card_balance),
                    style = AbcTheme.typography.small,
                    color = ink
                )
                BalanceText(
                    money = face.balance,
                    hidden = balanceHidden,
                    style = AbcTheme.typography.h1,
                    color = ink,
                    textAlign = TextAlign.Start
                )
            }
            val secrets = face.secrets
            if (secrets != null) {
                Text(
                    text = secrets.pan.chunked(4).joinToString(" "),
                    style = AbcTheme.typography.mono,
                    color = ink
                )
            }
            // At large font scales the expiry and number move under the name, never splitting it.
            AdaptiveRow(
                start = {
                    Text(text = face.holderName, style = AbcTheme.typography.label, color = ink)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = face.expiry.format(EXPIRY),
                        style = AbcTheme.typography.label,
                        color = ink
                    )
                    Spacer(Modifier.width(spacing.space3))
                    Text(
                        text = if (secrets != null) {
                            stringResource(R.string.card_cvv, secrets.cvv)
                        } else {
                            stringResource(R.string.card_last4, face.last4)
                        },
                        style = AbcTheme.typography.mono,
                        color = ink
                    )
                }
            }
        }
    }
}

/**
 * `FrozenCardBanner` from DESIGN_SYSTEM.md section 06 (Stitch Cards, adopted extra): under a frozen
 * card, a snowflake in an accentDeep circle, what freezing means, and Primary "Unfreeze".
 */
@Composable
fun FrozenCardBanner(onUnfreeze: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AbcTheme.colors
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.xl)
            .background(colors.accentTint)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {}
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AbcTheme.dimens.iconCircle)
                    .clip(AbcTheme.shapes.full)
                    .background(colors.accentDeep)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_snowflake),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
            }
            Spacer(Modifier.width(spacing.space3))
            Column {
                Text(
                    text = stringResource(R.string.cards_frozen_banner_title),
                    style = AbcTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.text
                )
                Text(
                    text = stringResource(R.string.cards_frozen_banner_text),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted
                )
            }
        }
        AbcButton(text = stringResource(R.string.action_unfreeze), onClick = onUnfreeze)
    }
}

/** `CardTag` from DESIGN_SYSTEM.md section 06: a black floating pill, such as "Main card". */
@Composable
fun CardTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AbcTheme.typography.small,
        color = AbcTheme.colors.onPrimary,
        modifier = modifier
            .clip(AbcTheme.shapes.full)
            .background(AbcTheme.colors.primary)
            .padding(horizontal = AbcTheme.spacing.space4, vertical = AbcTheme.spacing.space2)
    )
}

@Composable
fun CardArt.gradient(): CardGradient = when (this) {
    CardArt.PEACH -> AbcTheme.colors.cardPeach
    CardArt.SAGE -> AbcTheme.colors.cardSage
    CardArt.GRAPHITE -> AbcTheme.colors.cardGraphite
}

/** Two linked rings for Mastercard (as in the sample screens), the network name for Visa. */
@Composable
private fun NetworkMark(network: CardNetwork, ink: Color) {
    when (network) {
        CardNetwork.MASTERCARD -> Canvas(
            Modifier
                .width(AbcTheme.dimens.iconLarge)
                .height(AbcTheme.dimens.iconMedium)
        ) {
            val radius = size.height / 2
            val stroke = Stroke(width = size.height * RING_STROKE)
            drawCircle(ink, radius - stroke.width, Offset(radius, radius), style = stroke)
            drawCircle(
                ink,
                radius - stroke.width,
                Offset(size.width - radius, radius),
                style = stroke
            )
        }
        CardNetwork.VISA -> Text(
            text = stringResource(R.string.card_network_visa),
            style = AbcTheme.typography.label,
            color = ink
        )
    }
}

@Composable
private fun cardDescription(face: BankCardFace, balanceHidden: Boolean): String {
    val network = stringResource(
        if (face.network ==
            CardNetwork.VISA
        ) {
            R.string.card_network_visa
        } else {
            R.string.card_network_mastercard
        }
    )
    val balance = if (balanceHidden) {
        stringResource(
            R.string.cd_balance_hidden
        )
    } else {
        MoneyFormatter.format(face.balance)
    }
    val secrets = face.secrets
    val number = if (secrets != null) {
        stringResource(
            R.string.cd_card_revealed,
            secrets.pan.chunked(4).joinToString(" "),
            secrets.cvv
        )
    } else {
        stringResource(R.string.cd_card_ending, face.last4)
    }
    val frozen = if (face.frozen) stringResource(R.string.card_frozen) else ""
    return listOf(network, number, face.expiry.format(EXPIRY), balance, frozen).filter {
        it.isNotEmpty()
    }.joinToString(", ")
}

private val EXPIRY: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/yy")
private const val RING_STROKE = 0.14f

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun BankCardViewPreview() {
    AbcTheme {
        Column(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)
        ) {
            Box {
                BankCardView(
                    face = BankCardFace(
                        CardArt.PEACH,
                        CardNetwork.MASTERCARD,
                        "SAMANTA JOHNS",
                        "3144",
                        YearMonth.of(2029, 8),
                        Money(15_583_200),
                        frozen = false
                    ),
                    balanceHidden = false,
                    modifier = Modifier.fillMaxWidth()
                )
                CardTag("Main card", Modifier.align(Alignment.TopEnd))
            }
            BankCardView(
                face = BankCardFace(
                    CardArt.SAGE,
                    CardNetwork.VISA,
                    "SAMANTA JOHNS",
                    "5821",
                    YearMonth.of(2028, 11),
                    Money(3_823_500),
                    frozen = true,
                    secrets = CardSecrets("4999000000015821", "582")
                ),
                balanceHidden = false,
                modifier = Modifier.fillMaxWidth()
            )
            FrozenCardBanner(onUnfreeze = {})
        }
    }
}

package com.abcbank.mobile.feature.accounts.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.labelRes
import com.abcbank.mobile.core.designsystem.theme.LightAbcColors
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.feature.accounts.domain.ReceiptFile
import com.abcbank.mobile.feature.accounts.domain.ReceiptGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.withContext

/**
 * Draws a one-page A5 receipt with the framework's PdfDocument (ARCHITECTURE.md section 02: no PDF
 * library) and shares it through the app's FileProvider. Receipts live in the cache only, and
 * each new one deletes the last, so they never pile up.
 */
class PdfReceiptGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val zone: Provider<ZoneId>,
    private val dispatchers: DispatcherProvider
) : ReceiptGenerator {

    override suspend fun createReceipt(
        transaction: Transaction,
        account: Account?
    ): DataResult<ReceiptFile> = withContext(dispatchers.io) {
        try {
            val directory = File(context.cacheDir, RECEIPTS_DIR).apply {
                deleteRecursively()
                mkdirs()
            }
            // The document id, not the reference, names the file: it is short and safe.
            val file = File(directory, "receipt-${transaction.id}.pdf")
            val document = PdfDocument()
            try {
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                )
                draw(page.canvas, transaction, account)
                document.finishPage(page)
                file.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            DataResult.Success(ReceiptFile(uri.toString()))
        } catch (error: IOException) {
            DataResult.Failure(DataError.UNKNOWN)
        }
    }

    private fun draw(canvas: Canvas, transaction: Transaction, account: Account?) {
        val local = transaction.createdAt.atZone(zone.get())
        val regular = ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT
        val medium = ResourcesCompat.getFont(context, R.font.inter_medium) ?: Typeface.DEFAULT_BOLD
        // The design tokens, so the receipt matches the app (DESIGN_SYSTEM.md section 02).
        val text = LightAbcColors.text.toArgb()
        val muted = LightAbcColors.textMuted.toArgb()
        val line = LightAbcColors.border.toArgb()
        fun paint(
            typeface: Typeface,
            size: Float,
            color: Int,
            align: Paint.Align = Paint.Align.LEFT
        ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            this.color = color
            textAlign = align
        }

        var y = MARGIN + TITLE_SIZE
        canvas.drawText(
            context.getString(R.string.app_name),
            MARGIN,
            y,
            paint(medium, TITLE_SIZE, text)
        )
        y += LINE_GAP
        canvas.drawText(
            context.getString(R.string.receipt_title),
            MARGIN,
            y,
            paint(regular, BODY_SIZE, muted)
        )

        y += SECTION_GAP + AMOUNT_SIZE
        canvas.drawText(
            MoneyFormatter.formatSigned(transaction.signedAmount),
            MARGIN,
            y,
            paint(medium, AMOUNT_SIZE, text)
        )
        y += LINE_GAP + BODY_SIZE
        canvas.drawText(transaction.merchant, MARGIN, y, paint(regular, BODY_SIZE + 2, text))

        val rows = listOfNotNull(
            context.getString(R.string.transaction_detail_date) to
                DateFormatter.fullDate(local.toLocalDate()),
            context.getString(R.string.transaction_detail_time) to
                DateFormatter.time(local.toLocalTime()),
            account?.let {
                context.getString(R.string.transaction_detail_account) to
                    context.getString(R.string.account_name_last4, it.nickname, it.last4)
            },
            context.getString(R.string.transaction_detail_category) to
                context.getString(transaction.category.labelRes()),
            context.getString(R.string.transaction_detail_status) to context.getString(
                if (transaction.status == TransactionStatus.PENDING) {
                    R.string.transaction_status_pending
                } else {
                    R.string.transaction_status_posted
                }
            ),
            context.getString(R.string.transaction_detail_reference) to transaction.reference
        )
        y += SECTION_GAP
        val labelPaint = paint(regular, SMALL_SIZE, muted)
        val valuePaint = paint(regular, SMALL_SIZE, text, Paint.Align.RIGHT)
        val dividerPaint = Paint().apply { color = line }
        for ((label, value) in rows) {
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
            y += ROW_HEIGHT
            canvas.drawText(label, MARGIN, y, labelPaint)
            canvas.drawText(value, PAGE_WIDTH - MARGIN, y, valuePaint)
            y += ROW_PADDING
        }
        canvas.drawText(
            context.getString(R.string.receipt_footer),
            MARGIN,
            PAGE_HEIGHT - MARGIN,
            paint(regular, SMALL_SIZE, muted)
        )
    }

    private companion object {
        const val RECEIPTS_DIR = "receipts"

        // A5 in PostScript points (1/72 inch).
        const val PAGE_WIDTH = 420
        const val PAGE_HEIGHT = 595
        const val MARGIN = 36f
        const val TITLE_SIZE = 18f
        const val AMOUNT_SIZE = 28f
        const val BODY_SIZE = 11f
        const val SMALL_SIZE = 9f
        const val LINE_GAP = 16f
        const val SECTION_GAP = 28f
        const val ROW_HEIGHT = 18f
        const val ROW_PADDING = 8f
    }
}

package com.abcbank.mobile.feature.documents.data

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
import com.abcbank.mobile.core.designsystem.theme.LightAbcColors
import com.abcbank.mobile.feature.documents.domain.BankDocument
import com.abcbank.mobile.feature.documents.domain.DocumentFile
import com.abcbank.mobile.feature.documents.domain.DocumentRenderer
import com.abcbank.mobile.feature.documents.domain.Statement
import com.abcbank.mobile.feature.documents.domain.TaxForm
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.withContext

/**
 * Draws monthly statements and the 1099-INT with the framework's PdfDocument (ARCHITECTURE.md
 * section 02: no PDF library) on A4 pages in the design tokens' colours. Files go to the cache's
 * documents folder, which the FileProvider shares, and are deleted when the viewer closes.
 */
class PdfDocumentRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) : DocumentRenderer {

    override suspend fun render(document: BankDocument): DataResult<DocumentFile> =
        withContext(dispatchers.io) {
            try {
                // Only one document is open at a time, so anything left from before is cleared first.
                val directory = File(context.cacheDir, DOCUMENTS_DIR).apply {
                    deleteRecursively()
                    mkdirs()
                }
                val file = File(directory, "${document.id}.pdf")
                val pdf = PdfDocument()
                try {
                    val pages = Pages(pdf)
                    when (document) {
                        is Statement -> drawStatement(pages, document)
                        is TaxForm -> drawTaxForm(pages, document)
                    }
                    pages.finish()
                    file.outputStream().use(pdf::writeTo)
                } finally {
                    pdf.close()
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                DataResult.Success(DocumentFile(file.absolutePath, uri.toString()))
            } catch (error: IOException) {
                DataResult.Failure(DataError.UNKNOWN)
            }
        }

    override suspend fun delete(file: DocumentFile) {
        withContext(dispatchers.io) { File(file.path).delete() }
    }

    private val regular: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.inter_regular)
            ?: Typeface.DEFAULT
    }
    private val medium: Typeface by lazy {
        ResourcesCompat.getFont(context, R.font.inter_medium)
            ?: Typeface.DEFAULT_BOLD
    }

    private fun paint(
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

    private val text get() = LightAbcColors.text.toArgb()
    private val muted get() = LightAbcColors.textMuted.toArgb()
    private val line get() = LightAbcColors.border.toArgb()

    /** Starts a new page whenever the next row would not fit, with the footer on every page. */
    private inner class Pages(private val pdf: PdfDocument) {
        private var number = 0
        private var page: PdfDocument.Page? = null
        val canvas: Canvas get() = requireNotNull(page) { "No page started" }.canvas
        var y = 0f

        fun newPage() {
            finishCurrent()
            number++
            page =
                pdf.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()
                )
            y = MARGIN
        }

        fun ensure(height: Float, onNewPage: () -> Unit = {}) {
            if (y + height > PAGE_HEIGHT - MARGIN - FOOTER_SPACE) {
                newPage()
                onNewPage()
            }
        }

        fun finish() = finishCurrent()

        private fun finishCurrent() {
            val current = page ?: return
            current.canvas.drawText(
                context.getString(R.string.document_footer, number),
                MARGIN,
                PAGE_HEIGHT - MARGIN,
                paint(regular, SMALL_SIZE, muted)
            )
            pdf.finishPage(current)
            page = null
        }
    }

    private fun drawHeader(pages: Pages, title: String, subtitle: String) {
        pages.newPage()
        pages.y += TITLE_SIZE
        pages.canvas.drawText(
            context.getString(R.string.app_name),
            MARGIN,
            pages.y,
            paint(medium, TITLE_SIZE, text)
        )
        pages.y += LINE_GAP + BODY_SIZE
        pages.canvas.drawText(title, MARGIN, pages.y, paint(medium, BODY_SIZE + 2, text))
        pages.y += LINE_GAP
        pages.canvas.drawText(subtitle, MARGIN, pages.y, paint(regular, BODY_SIZE, muted))
        pages.y += SECTION_GAP
    }

    private fun drawRows(pages: Pages, rows: List<Pair<String, String>>) {
        val labelPaint = paint(regular, BODY_SIZE, muted)
        val valuePaint = paint(regular, BODY_SIZE, text, Paint.Align.RIGHT)
        val divider = Paint().apply { color = line }
        for ((label, value) in rows) {
            pages.canvas.drawLine(MARGIN, pages.y, PAGE_WIDTH - MARGIN, pages.y, divider)
            pages.y += ROW_HEIGHT
            pages.canvas.drawText(label, MARGIN, pages.y, labelPaint)
            pages.canvas.drawText(value, PAGE_WIDTH - MARGIN, pages.y, valuePaint)
            pages.y += ROW_PADDING
        }
        pages.y += SECTION_GAP
    }

    private fun drawStatement(pages: Pages, statement: Statement) {
        val owed = statement.owedStyle
        val openingLabel = if (owed) R.string.document_opening_owed else R.string.document_opening
        val closingLabel = if (owed) R.string.document_closing_owed else R.string.document_closing
        val account = statement.account
        drawHeader(
            pages,
            context.getString(R.string.document_statement_title, account.nickname, account.last4),
            DateFormatter.monthYear(statement.month)
        )
        drawRows(
            pages,
            listOf(
                context.getString(R.string.document_customer) to
                    context.getString(
                        R.string.document_customer_value,
                        statement.customerName,
                        statement.customerId
                    ),
                context.getString(R.string.document_period) to context.getString(
                    R.string.insights_date_range,
                    DateFormatter.shortDate(statement.month.atDay(1)),
                    DateFormatter.fullDate(statement.month.atEndOfMonth())
                ),
                context.getString(
                    openingLabel
                ) to
                    MoneyFormatter.format(statement.opening),
                context.getString(R.string.document_money_in) to
                    MoneyFormatter.format(statement.moneyIn),
                context.getString(R.string.document_money_out) to
                    MoneyFormatter.format(statement.moneyOut),
                context.getString(
                    closingLabel
                ) to
                    MoneyFormatter.format(statement.closing)
            )
        )
        val columns = listOf(
            context.getString(R.string.document_col_date),
            context.getString(R.string.document_col_description),
            context.getString(R.string.document_col_in),
            context.getString(R.string.document_col_out),
            context.getString(R.string.document_col_balance)
        )
        fun drawColumns() {
            val head = paint(medium, SMALL_SIZE, muted)
            pages.y += ROW_HEIGHT
            pages.canvas.drawText(columns[0], COL_DATE, pages.y, head)
            pages.canvas.drawText(columns[1], COL_DESCRIPTION, pages.y, head)
            pages.canvas.drawText(
                columns[2],
                COL_IN,
                pages.y,
                paint(medium, SMALL_SIZE, muted, Paint.Align.RIGHT)
            )
            pages.canvas.drawText(
                columns[3],
                COL_OUT,
                pages.y,
                paint(medium, SMALL_SIZE, muted, Paint.Align.RIGHT)
            )
            pages.canvas.drawText(
                columns[4],
                COL_BALANCE,
                pages.y,
                paint(medium, SMALL_SIZE, muted, Paint.Align.RIGHT)
            )
            pages.y += ROW_PADDING
        }
        pages.ensure(ROW_HEIGHT * 2)
        drawColumns()
        if (statement.lines.isEmpty()) {
            pages.y += ROW_HEIGHT
            pages.canvas.drawText(
                context.getString(R.string.document_no_activity),
                COL_DATE,
                pages.y,
                paint(regular, SMALL_SIZE, muted)
            )
        }
        val body = paint(regular, SMALL_SIZE, text)
        val right = paint(regular, SMALL_SIZE, text, Paint.Align.RIGHT)
        val divider = Paint().apply { color = line }
        for (item in statement.lines) {
            pages.ensure(ROW_HEIGHT + ROW_PADDING) { drawColumns() }
            pages.canvas.drawLine(MARGIN, pages.y, PAGE_WIDTH - MARGIN, pages.y, divider)
            pages.y += ROW_HEIGHT
            pages.canvas.drawText(DateFormatter.shortDate(item.date), COL_DATE, pages.y, body)
            pages.canvas.drawText(
                fit(
                    item.description,
                    body,
                    COL_IN - COL_DESCRIPTION - COLUMN_GAP * 4
                ),
                COL_DESCRIPTION,
                pages.y,
                body
            )
            item.moneyIn?.let {
                pages.canvas.drawText(MoneyFormatter.format(it), COL_IN, pages.y, right)
            }
            item.moneyOut?.let {
                pages.canvas.drawText(MoneyFormatter.format(it), COL_OUT, pages.y, right)
            }
            pages.canvas.drawText(MoneyFormatter.format(item.balance), COL_BALANCE, pages.y, right)
            pages.y += ROW_PADDING
        }
    }

    private fun drawTaxForm(pages: Pages, form: TaxForm) {
        drawHeader(
            pages,
            context.getString(R.string.document_1099_title, form.year),
            context.getString(R.string.document_1099_subtitle)
        )
        drawRows(
            pages,
            listOf(
                context.getString(R.string.document_payer) to context.getString(R.string.app_name),
                context.getString(R.string.document_recipient) to form.recipientName,
                context.getString(R.string.document_account_number) to form.customerId,
                context.getString(R.string.document_tax_year) to form.year.toString(),
                context.getString(R.string.document_box1) to MoneyFormatter.format(form.interest)
            )
        )
        pages.canvas.drawText(
            context.getString(R.string.document_1099_note),
            MARGIN,
            pages.y,
            paint(regular, SMALL_SIZE, muted)
        )
    }

    /** Shortens [value] with an ellipsis so it never runs into the next column. */
    private fun fit(value: String, paint: Paint, width: Float): String {
        if (paint.measureText(value) <= width) return value
        var end = value.length
        while (end > 1 && paint.measureText(value.substring(0, end) + ELLIPSIS) > width) end--
        return value.substring(0, end) + ELLIPSIS
    }

    private companion object {
        const val DOCUMENTS_DIR = "documents"
        const val ELLIPSIS = "…"

        // A4 in PostScript points (1/72 inch).
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val FOOTER_SPACE = 24f
        const val TITLE_SIZE = 18f
        const val BODY_SIZE = 11f
        const val SMALL_SIZE = 9f
        const val LINE_GAP = 16f
        const val SECTION_GAP = 24f
        const val ROW_HEIGHT = 16f
        const val ROW_PADDING = 6f
        const val COLUMN_GAP = 4f
        const val COL_DATE = MARGIN
        const val COL_DESCRIPTION = 100f
        const val COL_IN = 380f
        const val COL_OUT = 465f
        const val COL_BALANCE = PAGE_WIDTH - MARGIN
    }
}

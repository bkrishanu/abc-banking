package com.abcbank.mobile.feature.documents.ui

import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.graphics.createBitmap
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.documents.domain.BankDocument
import com.abcbank.mobile.feature.documents.domain.Statement
import com.abcbank.mobile.feature.documents.domain.TaxForm
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The in-app PDF viewer (PRD feature 14): the generated file's pages drawn with PdfRenderer, and
 * Share for the system share sheet.
 */
@Composable
fun DocumentViewerScreen(
    viewer: ViewerState,
    onBack: () -> Unit,
    onShare: (String) -> Unit,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val document = when (viewer) {
        is ViewerState.Building -> viewer.document
        is ViewerState.Ready -> viewer.document
        is ViewerState.Failed -> viewer.document
        ViewerState.Closed -> null
    }
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = document?.let { documentTitle(it) }.orEmpty(),
        onBack = onBack,
        modifier = modifier,
        bottom = {
            if (viewer is ViewerState.Ready) {
                AbcButton(
                    text = stringResource(R.string.action_share),
                    onClick = { onShare(viewer.file.contentUri) },
                    variant = AbcButtonVariant.Secondary
                )
            }
        }
    ) {
        when (viewer) {
            is ViewerState.Ready -> PdfPages(viewer.file.path, documentTitle(viewer.document))
            is ViewerState.Failed -> ErrorState(
                message = stringResource(R.string.documents_render_failed),
                onRetry = { onRetry(viewer.document.id) }
            )
            else -> SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(A4_RATIO),
                shape = AbcTheme.shapes.md
            )
        }
    }
}

@Composable
private fun documentTitle(document: BankDocument): String = when (document) {
    is Statement -> stringResource(
        R.string.documents_statement_title,
        DateFormatter.monthYear(document.month)
    )
    is TaxForm -> stringResource(R.string.documents_1099, document.year)
}

/** Every page as an image at the screen's width; pages are white like paper in both themes. */
@Composable
private fun PdfPages(path: String, description: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        var pages by remember(path) { mutableStateOf<List<ImageBitmap>?>(null) }
        var failed by remember(path) { mutableStateOf(false) }
        LaunchedEffect(path, widthPx) {
            val rendered = withContext(Dispatchers.IO) { renderPages(File(path), widthPx) }
            if (rendered == null) failed = true else pages = rendered
        }
        val current = pages
        when {
            failed -> Text(
                stringResource(R.string.documents_render_failed),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.danger
            )
            current == null -> SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(A4_RATIO),
                shape = AbcTheme.shapes.md
            )
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3),
                modifier = Modifier.semantics { contentDescription = description }
            ) {
                current.forEach { page ->
                    Image(
                        bitmap = page,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(page.width.toFloat() / page.height)
                            .clip(AbcTheme.shapes.sm)
                            .background(AbcTheme.colors.surface)
                    )
                }
            }
        }
    }
}

/** Null when the file cannot be read. */
private fun renderPages(file: File, widthPx: Int): List<ImageBitmap>? = try {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            (0 until renderer.pageCount).map { index ->
                renderer.openPage(index).use { page ->
                    val height = (widthPx.toFloat() * page.height / page.width).toInt()
                    val bitmap = createBitmap(widthPx, height)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                }
            }
        }
    }
} catch (error: IOException) {
    null
} catch (error: SecurityException) {
    null
}

private const val A4_RATIO = 595f / 842f

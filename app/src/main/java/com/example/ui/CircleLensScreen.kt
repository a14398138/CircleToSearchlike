package com.example.ui

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CircleLensScreen(
    viewModel: CircleLensViewModel,
    onCloseApp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var displayImageRect by remember { mutableStateOf(RectF()) }

    BackHandler(enabled = true) {
        onCloseApp()
    }

    // Fullscreen edge-to-edge container without Scaffold insets shrinkage
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val popupHeightPx = with(density) { 52.dp.toPx() }
        val topMarginPx = with(density) { 14.dp.toPx() }
        val safeTopPx = with(density) { 56.dp.toPx() }
        val safeBottomPx = with(density) { 96.dp.toPx() }

        // Main Interactive Canvas (Full bleed 100% display)
        CircleLensOverlayCanvas(
            modifier = Modifier.fillMaxSize(),
            bitmap = uiState.currentBitmap,
            ocrItems = uiState.ocrItems,
            textSelection = uiState.textSelection,
            cropSelection = uiState.activeCropSelection,
            activeStrokePoints = uiState.activeStrokePoints,
            trailPoints = uiState.trailPoints,
            onStrokeStart = { startOffset ->
                viewModel.onStrokeStart(startOffset)
            },
            onStrokeMove = { point, imgRect, w, h ->
                viewModel.onStrokeMove(point, imgRect, w, h)
            },
            onStrokeEnd = { imgRect, w, h ->
                viewModel.onStrokeEnd(imgRect, w, h)
            },
            onDragStartPin = { pt, imgRect, w, h ->
                viewModel.onDragStartPin(pt, imgRect, w, h)
            },
            onDragEndPin = { pt, imgRect, w, h ->
                viewModel.onDragEndPin(pt, imgRect, w, h)
            },
            onCropFrameUpdated = { newRect, imgRect ->
                viewModel.updateCropRect(newRect, imgRect)
            },
            onImageRectCalculated = { rect ->
                displayImageRect = rect
            }
        )

        // Calculate top anchor and horizontal center for text selection popup
        val (textAnchorX, textAnchorY, textBottomY) = remember(uiState.textSelection, displayImageRect) {
            val sel = uiState.textSelection
            if (sel != null && sel.selectedTokens.isNotEmpty()) {
                val bmpW = uiState.currentBitmap?.width ?: 1080
                val bmpH = uiState.currentBitmap?.height ?: 2200
                val scaleX = if (bmpW > 0) displayImageRect.width() / bmpW else 1f
                val scaleY = if (bmpH > 0) displayImageRect.height() / bmpH else 1f

                var minX = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var minY = Float.MAX_VALUE
                var maxY = Float.MIN_VALUE

                for (token in sel.selectedTokens) {
                    val box = token.boundingBox
                    val left = displayImageRect.left + box.left * scaleX
                    val top = displayImageRect.top + box.top * scaleY
                    val right = displayImageRect.left + box.right * scaleX
                    val bottom = displayImageRect.top + box.bottom * scaleY

                    minX = min(minX, left)
                    maxX = max(maxX, right)
                    minY = min(minY, top)
                    maxY = max(maxY, bottom)
                }
                minY = min(minY, min(sel.startPinPoint.y, sel.endPinPoint.y))
                Triple((minX + maxX) / 2f, minY, maxY)
            } else {
                Triple(0f, 0f, 0f)
            }
        }

        // Calculate top anchor and horizontal center for crop selection popup
        val (cropAnchorX, cropAnchorY, cropBottomY) = remember(uiState.activeCropSelection) {
            val r = uiState.activeCropSelection?.rect
            if (r != null) {
                Triple((r.left + r.right) / 2f, r.top, r.bottom)
            } else {
                Triple(0f, 0f, 0f)
            }
        }

        // Text Selection Popup (Appears directly above selected text)
        if (uiState.textSelection != null && uiState.textSelection!!.selectedTokens.isNotEmpty()) {
            val textPopupHalfWidthPx = with(density) { 85.dp.toPx() }
            val preferredY = textAnchorY - popupHeightPx - topMarginPx
            val targetY = if (preferredY >= safeTopPx) {
                preferredY
            } else {
                (textBottomY + topMarginPx).coerceAtMost(screenHeightPx - popupHeightPx - safeBottomPx)
            }
            val targetX = textAnchorX.coerceIn(
                textPopupHalfWidthPx + 16f,
                screenWidthPx - textPopupHalfWidthPx - 16f
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (targetX - textPopupHalfWidthPx).roundToInt(),
                            targetY.roundToInt()
                        )
                    }
            ) {
                TextSelectionPopup(
                    textSelection = uiState.textSelection,
                    lastShareTarget = uiState.lastTextShareTarget,
                    recentTargets = uiState.recentTextTargets,
                    onCopy = { viewModel.copySelectedText() },
                    onShareGeneral = {
                        viewModel.shareSelectedText(null)
                    },
                    onShareDirect = { target ->
                        viewModel.shareSelectedText(target)
                    },
                    onOpenAllTargetsPicker = {
                        viewModel.showTargetPicker(true, isImage = false)
                    }
                )
            }
        }

        // Crop Selection Popup (Appears directly above crop frame)
        if (uiState.activeCropSelection != null) {
            val cropPopupHalfWidthPx = with(density) { 55.dp.toPx() }
            val preferredY = cropAnchorY - popupHeightPx - topMarginPx
            val targetY = if (preferredY >= safeTopPx) {
                preferredY
            } else {
                (cropBottomY + topMarginPx).coerceAtMost(screenHeightPx - popupHeightPx - safeBottomPx)
            }
            val targetX = cropAnchorX.coerceIn(
                cropPopupHalfWidthPx + 16f,
                screenWidthPx - cropPopupHalfWidthPx - 16f
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (targetX - cropPopupHalfWidthPx).roundToInt(),
                            targetY.roundToInt()
                        )
                    }
            ) {
                CropSelectionPopup(
                    cropSelection = uiState.activeCropSelection,
                    lastShareTarget = uiState.lastImageShareTarget,
                    recentTargets = uiState.recentImageTargets,
                    onShareImageGeneral = {
                        viewModel.shareCroppedImage(null)
                    },
                    onShareImageDirect = { target ->
                        viewModel.shareCroppedImage(target)
                    },
                    onOpenAllTargetsPicker = {
                        viewModel.showTargetPicker(true, isImage = true)
                    }
                )
            }
        }

        // Minimal Icon-Only Bottom Toolbar (White frosted glass + Black only, no drag handle)
        CircleLensBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            isReady = uiState.currentBitmap != null,
            isOcrRunning = uiState.isOcrRunning,
            lastShareTarget = uiState.lastImageShareTarget ?: uiState.lastTextShareTarget,
            recentTargets = uiState.recentImageTargets,
            onSelectAllText = {
                val bmpW = uiState.currentBitmap?.width ?: 1080
                val bmpH = uiState.currentBitmap?.height ?: 2200
                viewModel.selectAllText(displayImageRect, bmpW, bmpH)
            },
            onShareEntireScreenGeneral = {
                viewModel.shareEntireScreen(null)
            },
            onShareEntireScreenDirect = { target ->
                viewModel.shareEntireScreen(target)
            },
            onOpenAllTargetsPicker = {
                viewModel.showTargetPicker(true, isImage = true)
            }
        )

        // Feedback Toast / Banner
        AnimatedVisibility(
            visible = uiState.feedbackMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            uiState.feedbackMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEBFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33000000))
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Setup Wizard Help Dialog (Shown only when opened from launcher icon)
    if (uiState.showAssistHelpSheet) {
        AssistHelpDialog(
            onDismiss = {
                viewModel.showAssistHelp(false)
            }
        )
    }
}

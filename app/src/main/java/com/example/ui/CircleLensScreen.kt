package com.example.ui

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CircleLensScreen(
    viewModel: CircleLensViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var displayImageRect by remember { mutableStateOf(RectF()) }

    BackHandler(enabled = uiState.textSelection != null || uiState.activeCropSelection != null) {
        viewModel.clearSelection()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090D16)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Interactive Canvas (No top bar header)
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

            // Text Selection Popup (when user drags pins or selects words)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                TextSelectionPopup(
                    textSelection = uiState.textSelection,
                    lastShareTarget = uiState.lastTextShareTarget,
                    onCopy = { viewModel.copySelectedText() },
                    onShareGeneral = { viewModel.shareSelectedText(null) },
                    onShareDirect = { target -> viewModel.shareSelectedText(target) },
                    onChangeTarget = { viewModel.showTargetPicker(true, isImage = false) },
                    onClear = { viewModel.clearSelection() }
                )
            }

            // Crop Selection Popup (when user circles or draws a box)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                CropSelectionPopup(
                    cropSelection = uiState.activeCropSelection,
                    lastShareTarget = uiState.lastImageShareTarget,
                    onShareImageGeneral = { viewModel.shareCroppedImage(null) },
                    onShareImageDirect = { target -> viewModel.shareCroppedImage(target) },
                    onCopyImage = { viewModel.copyCroppedImage() },
                    onOcrCrop = {
                        val bmpW = uiState.currentBitmap?.width ?: 1080
                        val bmpH = uiState.currentBitmap?.height ?: 2200
                        viewModel.ocrInsideCrop(displayImageRect, bmpW, bmpH)
                    },
                    onChangeTarget = { viewModel.showTargetPicker(true, isImage = true) },
                    onClear = { viewModel.clearSelection() }
                )
            }

            // Draggable Minimal Icon-Only Bottom Toolbar
            CircleLensBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                isOcrRunning = uiState.isOcrRunning,
                lastShareTarget = uiState.lastImageShareTarget ?: uiState.lastTextShareTarget,
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
                onChangeShareTarget = {
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
                    .padding(top = 40.dp)
            ) {
                uiState.feedbackMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xE61E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Target Picker Sheet
    if (uiState.showTargetPickerSheet) {
        val targets = if (uiState.targetPickerIsImage) {
            uiState.availableImageTargets
        } else {
            uiState.availableTextTargets
        }
        val current = if (uiState.targetPickerIsImage) {
            uiState.lastImageShareTarget
        } else {
            uiState.lastTextShareTarget
        }

        TargetPickerBottomSheet(
            isImage = uiState.targetPickerIsImage,
            targets = targets,
            currentTarget = current,
            onTargetSelected = { target ->
                viewModel.selectTargetFromPicker(target, uiState.targetPickerIsImage)
            },
            onDismiss = {
                viewModel.showTargetPicker(false, uiState.targetPickerIsImage)
            }
        )
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

package com.example.ui

import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.loadBitmapFromUri(uri)
        }
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
            // Main Interactive Canvas
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

            // Top Bar (Minimal Google Circle OCR Header)
            CircleLensTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                onCloseOrClear = {
                    if (uiState.textSelection != null || uiState.activeCropSelection != null) {
                        viewModel.clearSelection()
                    }
                },
                onOpenPresets = {
                    viewModel.showPresetsSheet(true)
                },
                onPickGallery = {
                    photoPickerLauncher.launch("image/*")
                },
                onOpenAssistHelp = {
                    viewModel.showAssistHelp(true)
                }
            )

            // Floating Action Popups & Bottom Search Bar Area
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Popup for Fine-Grained Text Selection
                TextSelectionPopup(
                    textSelection = uiState.textSelection,
                    lastShareTarget = uiState.lastTextShareTarget,
                    onCopy = { viewModel.copySelectedText() },
                    onShareGeneral = { viewModel.shareSelectedText(null) },
                    onShareDirect = { target -> viewModel.shareSelectedText(target) },
                    onChangeTarget = { viewModel.showTargetPicker(true, isImage = false) },
                    onClear = { viewModel.clearSelection() }
                )

                // Floating Popup for Image Crop Selection
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

                // Bottom Search Capsule & Quick Actions
                CircleLensBottomBar(
                    isOcrRunning = uiState.isOcrRunning,
                    hasSelection = uiState.textSelection != null || uiState.activeCropSelection != null,
                    selectedTextPreview = uiState.textSelection?.fullText,
                    lastShareTarget = uiState.lastImageShareTarget ?: uiState.lastTextShareTarget,
                    onShareEntireScreenGeneral = {
                        viewModel.shareEntireScreen(null)
                    },
                    onShareEntireScreenDirect = { target ->
                        viewModel.shareEntireScreen(target)
                    },
                    onSelectAllText = {
                        val bmpW = uiState.currentBitmap?.width ?: 1080
                        val bmpH = uiState.currentBitmap?.height ?: 2200
                        viewModel.selectAllText(displayImageRect, bmpW, bmpH)
                    },
                    onClearSelection = {
                        viewModel.clearSelection()
                    },
                    onOpenPresets = {
                        viewModel.showPresetsSheet(true)
                    }
                )
            }

            // Feedback Toast / Banner
            AnimatedVisibility(
                visible = uiState.feedbackMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
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

    // Presets Sheet
    if (uiState.showPresetsSheet) {
        PresetsBottomSheet(
            selectedPresetId = uiState.selectedPresetId,
            onPresetSelected = { presetId ->
                viewModel.loadPreset(presetId)
            },
            onDismiss = {
                viewModel.showPresetsSheet(false)
            }
        )
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

    // Assistant Help Dialog
    if (uiState.showAssistHelpSheet) {
        AssistHelpDialog(
            onDismiss = {
                viewModel.showAssistHelp(false)
            }
        )
    }
}

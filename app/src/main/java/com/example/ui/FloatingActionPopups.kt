package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CropSelection
import com.example.model.ShareTarget
import com.example.model.TextSelectionState

/**
 * Clean & minimal floating text action toolbar.
 */
@Composable
fun TextSelectionPopup(
    modifier: Modifier = Modifier,
    textSelection: TextSelectionState?,
    lastShareTarget: ShareTarget?,
    onCopy: () -> Unit,
    onShareGeneral: () -> Unit,
    onShareDirect: (ShareTarget) -> Unit,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = textSelection != null && textSelection.selectedTokens.isNotEmpty(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }, animationSpec = spring()),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 30 })
    ) {
        Surface(
            modifier = modifier
                .shadow(elevation = 16.dp, shape = CircleShape)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                    ),
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = Color(0xF20F172A),
            tonalElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Copy Text Button
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("copy_text_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "テキストをコピー",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share Text Button (General)
                IconButton(
                    onClick = onShareGeneral,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB))
                        .testTag("share_text_general_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "テキストを共有",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Last Direct Share App
                if (lastShareTarget != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), CircleShape)
                            .clickable { onShareDirect(lastShareTarget) }
                            .testTag("direct_share_text_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (lastShareTarget.iconBitmap != null) {
                            Image(
                                bitmap = lastShareTarget.iconBitmap,
                                contentDescription = "${lastShareTarget.appName} に送信",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "${lastShareTarget.appName} に送信",
                                tint = Color(0xFF67E8F9),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Clear Button
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .testTag("clear_text_selection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "選択解除",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean & minimal floating crop action toolbar.
 */
@Composable
fun CropSelectionPopup(
    modifier: Modifier = Modifier,
    cropSelection: CropSelection?,
    lastShareTarget: ShareTarget?,
    onShareImageGeneral: () -> Unit,
    onShareImageDirect: (ShareTarget) -> Unit,
    onCopyImage: () -> Unit,
    onOcrCrop: () -> Unit,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = cropSelection != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }, animationSpec = spring()),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 30 })
    ) {
        Surface(
            modifier = modifier
                .shadow(elevation = 16.dp, shape = CircleShape)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFFF472B6))
                    ),
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = Color(0xF20F172A),
            tonalElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // OCR / Extract text inside crop
                IconButton(
                    onClick = onOcrCrop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("ocr_crop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "文字を抽出",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Copy Image
                IconButton(
                    onClick = onCopyImage,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("copy_crop_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "画像をコピー",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share Image (General)
                IconButton(
                    onClick = onShareImageGeneral,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB))
                        .testTag("share_crop_general_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "画像を共有",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Last Direct Share App
                if (lastShareTarget != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), CircleShape)
                            .clickable { onShareImageDirect(lastShareTarget) }
                            .testTag("direct_share_crop_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (lastShareTarget.iconBitmap != null) {
                            Image(
                                bitmap = lastShareTarget.iconBitmap,
                                contentDescription = "${lastShareTarget.appName} に画像を送信",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "${lastShareTarget.appName} に画像を送信",
                                tint = Color(0xFF67E8F9),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Clear Button
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .testTag("clear_crop_selection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "選択解除",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

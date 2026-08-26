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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CropSelection
import com.example.model.ShareTarget
import com.example.model.TextSelectionState

@Composable
fun TextSelectionPopup(
    modifier: Modifier = Modifier,
    textSelection: TextSelectionState?,
    lastShareTarget: ShareTarget?,
    onCopy: () -> Unit,
    onShareGeneral: () -> Unit,
    onShareDirect: (ShareTarget) -> Unit,
    onChangeTarget: () -> Unit,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = textSelection != null && textSelection.selectedTokens.isNotEmpty(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }, animationSpec = spring()),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 30 })
    ) {
        val fullText = textSelection?.fullText ?: ""

        Surface(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .widthIn(max = 520.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                    ),
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF50F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top row: text preview + dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShortText,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fullText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFF1F5F9),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("clear_text_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "選択解除",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Copy | Share | [Direct App Button] | App Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Copy
                    ActionButton(
                        icon = Icons.Default.ContentCopy,
                        label = "コピー",
                        backgroundColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF67E8F9),
                        testTag = "copy_text_button",
                        onClick = onCopy
                    )

                    // Share General
                    ActionButton(
                        icon = Icons.Default.Share,
                        label = "共有",
                        backgroundColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFE2E8F0),
                        testTag = "share_text_general_button",
                        onClick = onShareGeneral
                    )

                    // Last Direct App (Beside Share)
                    if (lastShareTarget != null) {
                        DirectShareTargetButton(
                            target = lastShareTarget,
                            testTag = "direct_share_text_button",
                            onClick = { onShareDirect(lastShareTarget) }
                        )
                    }

                    // App Switcher
                    IconButton(
                        onClick = onChangeTarget,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("change_share_target_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "共有先アプリを変更",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CropSelectionPopup(
    modifier: Modifier = Modifier,
    cropSelection: CropSelection?,
    lastShareTarget: ShareTarget?,
    onShareImageGeneral: () -> Unit,
    onShareImageDirect: (ShareTarget) -> Unit,
    onCopyImage: () -> Unit,
    onOcrCrop: () -> Unit,
    onChangeTarget: () -> Unit,
    onClear: () -> Unit
) {
    AnimatedVisibility(
        visible = cropSelection != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }, animationSpec = spring()),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 30 })
    ) {
        Surface(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .widthIn(max = 540.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp))
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFFFBBF24))
                    ),
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF50F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFF0284C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "切り抜き枠を選択中",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("clear_crop_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "切り抜き解除",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Extract Text | Copy Image | Share Image | [Direct App] | Change Target
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Extract Text
                    ActionButton(
                        icon = Icons.Default.DocumentScanner,
                        label = "文字抽出",
                        backgroundColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFFBBF24),
                        testTag = "ocr_crop_button",
                        onClick = onOcrCrop
                    )

                    // Copy Image
                    ActionButton(
                        icon = Icons.Default.ContentCopy,
                        label = "コピー",
                        backgroundColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF67E8F9),
                        testTag = "copy_crop_image_button",
                        onClick = onCopyImage
                    )

                    // Share Image
                    ActionButton(
                        icon = Icons.Default.Share,
                        label = "共有",
                        backgroundColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        testTag = "share_crop_general_button",
                        onClick = onShareImageGeneral
                    )

                    // Last Direct App Button (Beside Share)
                    if (lastShareTarget != null) {
                        DirectShareTargetButton(
                            target = lastShareTarget,
                            testTag = "direct_share_crop_button",
                            onClick = { onShareImageDirect(lastShareTarget) }
                        )
                    }

                    // App Switcher
                    IconButton(
                        onClick = onChangeTarget,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("change_image_target_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "共有先アプリを変更",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun DirectShareTargetButton(
    target: ShareTarget,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))),
                RoundedCornerShape(12.dp)
            )
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (target.iconBitmap != null) {
                Image(
                    bitmap = target.iconBitmap,
                    contentDescription = target.appName,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xFF3B82F6), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = target.appName.take(1),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "送信",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF38BDF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = target.appName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 70.dp)
                )
            }
        }
    }
}

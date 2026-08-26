package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.ShareTarget
import kotlin.math.roundToInt

@Composable
fun CircleLensBottomBar(
    modifier: Modifier = Modifier,
    isOcrRunning: Boolean,
    lastShareTarget: ShareTarget?,
    onSelectAllText: () -> Unit,
    onShareEntireScreenGeneral: () -> Unit,
    onShareEntireScreenDirect: (ShareTarget) -> Unit
) {
    // Local draggable offset for moving the toolbar anywhere comfortably
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
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
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drag Handle Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "ドラッグして移動",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 1. "すべての文字を選択" Icon Button
            IconButton(
                onClick = onSelectAllText,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .testTag("select_all_text_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = "すべての文字を選択",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
            }

            // 2. "画面全体を送る" Icon Button
            if (isOcrRunning) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                IconButton(
                    onClick = onShareEntireScreenGeneral,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB))
                        .testTag("share_entire_screen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Screenshot,
                        contentDescription = "画面全体を送る",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 3. 前回共有したアプリのアイコン (直接送信)
            if (lastShareTarget != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), CircleShape)
                        .clickable { onShareEntireScreenDirect(lastShareTarget) }
                        .testTag("direct_share_last_app_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (lastShareTarget.iconBitmap != null) {
                        Image(
                            bitmap = lastShareTarget.iconBitmap,
                            contentDescription = "${lastShareTarget.appName} に画面全体を送る",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Screenshot,
                            contentDescription = "${lastShareTarget.appName} に画面全体を送る",
                            tint = Color(0xFF67E8F9),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

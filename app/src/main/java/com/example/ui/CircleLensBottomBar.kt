package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShareTarget

@Composable
fun CircleLensBottomBar(
    modifier: Modifier = Modifier,
    isOcrRunning: Boolean,
    hasSelection: Boolean,
    selectedTextPreview: String?,
    lastShareTarget: ShareTarget?,
    onShareEntireScreenGeneral: () -> Unit,
    onShareEntireScreenDirect: (ShareTarget) -> Unit,
    onSelectAllText: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenPresets: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xF50F172A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0x3338BDF8), Color(0x33818CF8)))
        ),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Google-style Search & Action capsule (like Screenshot 1 & 2)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Google 'G' icon & Query prompt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF4285F4), Color(0xFF34A853), Color(0xFFFBBC05), Color(0xFFEA4335))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (!selectedTextPreview.isNullOrBlank()) selectedTextPreview else "画面上の文字をなぞるか、円で囲んで検索",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (!selectedTextPreview.isNullOrBlank()) Color(0xFF67E8F9) else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = if (!selectedTextPreview.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Send Entire Screen / Direct Target button on right of search capsule
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isOcrRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 2.dp
                            )
                        } else {
                            // "画面全体を送る" Primary Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onShareEntireScreenGeneral)
                                    .testTag("share_entire_screen_button"),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2563EB)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Screenshot,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "画面全体を送る",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Direct 1-tap share target icon (Last Shared App)
                            if (lastShareTarget != null) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onShareEntireScreenDirect(lastShareTarget) }
                                        .border(
                                            1.dp,
                                            Color(0xFF38BDF8),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .testTag("direct_share_screen_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (lastShareTarget.iconBitmap != null) {
                                            Image(
                                                bitmap = lastShareTarget.iconBitmap,
                                                contentDescription = lastShareTarget.appName,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = lastShareTarget.appName,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 65.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom pill chips row (like authentic Circle-to-Search options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Select All Text
                ActionChip(
                    icon = Icons.Default.SelectAll,
                    label = "全文字選択",
                    onClick = onSelectAllText,
                    testTag = "select_all_text_button"
                )

                // Presets / Sample switch
                ActionChip(
                    icon = Icons.Default.Layers,
                    label = "サンプル切替",
                    onClick = onOpenPresets,
                    testTag = "open_presets_button"
                )

                if (hasSelection) {
                    ActionChip(
                        icon = Icons.Default.ClearAll,
                        label = "解除",
                        onClick = onClearSelection,
                        testTag = "clear_selection_bottom_button",
                        containerColor = Color(0xFF334155),
                        textColor = Color(0xFFF1F5F9)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    containerColor: Color = Color(0xFF1E293B),
    textColor: Color = Color(0xFFCBD5E1)
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

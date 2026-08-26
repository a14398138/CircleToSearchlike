package com.example.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.model.CropSelection
import com.example.model.OcrTextItem
import com.example.model.TextSelectionState
import com.example.model.TrailPoint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class CropHandleType {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP_EDGE, BOTTOM_EDGE, LEFT_EDGE, RIGHT_EDGE,
    MOVE
}

enum class DragTarget {
    NONE,
    START_PIN,
    END_PIN,
    CROP_HANDLE,
    DRAWING_STROKE
}

@Composable
fun CircleLensOverlayCanvas(
    modifier: Modifier = Modifier,
    bitmap: Bitmap?,
    ocrItems: List<OcrTextItem>,
    textSelection: TextSelectionState?,
    cropSelection: CropSelection?,
    activeStrokePoints: List<Offset>,
    trailPoints: List<TrailPoint>,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset, RectF, Int, Int) -> Unit,
    onStrokeEnd: (RectF, Int, Int) -> Unit,
    onDragStartPin: (Offset, RectF, Int, Int) -> Unit,
    onDragEndPin: (Offset, RectF, Int, Int) -> Unit,
    onCropFrameUpdated: (RectF, RectF) -> Unit,
    onImageRectCalculated: (RectF) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cropGlow")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashPhase"
    )

    var currentDisplayImageRect by remember { mutableStateOf(RectF()) }
    var currentDragTarget by remember { mutableStateOf(DragTarget.NONE) }
    var activeCropHandle by remember { mutableStateOf(CropHandleType.NONE) }
    var dragStartCropRect by remember { mutableStateOf(RectF()) }
    var dragStartTouch by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bitmap, cropSelection, textSelection) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val bmpW = bitmap?.width ?: 1080
                        val bmpH = bitmap?.height ?: 2200
                        val pinTouchRadius = 80f // Generous touch target for pins (~30dp)
                        val handleTouchRadius = 85f // Generous touch target for crop handles (~35dp)

                        // 1. Check Text Selection Pins first with generous touch targets and accurate pin centers
                        if (textSelection != null && textSelection.selectedTokens.isNotEmpty()) {
                            val startPin = textSelection.startPinPoint
                            val endPin = textSelection.endPinPoint

                            // Actual center coordinates of start and end pin heads
                            val startPinHeadCenter = Offset(startPin.x - 2f, startPin.y - 18f)
                            val endPinHeadCenter = Offset(endPin.x + 2f, endPin.y + 18f)

                            val distToStartPin = hypot((startOffset.x - startPinHeadCenter.x).toDouble(), (startOffset.y - startPinHeadCenter.y).toDouble()).toFloat()
                            val distToEndPin = hypot((startOffset.x - endPinHeadCenter.x).toDouble(), (startOffset.y - endPinHeadCenter.y).toDouble()).toFloat()

                            val pinHitRadius = 95f // Extra generous ~40dp touch target

                            if (distToStartPin < pinHitRadius && distToStartPin <= distToEndPin) {
                                currentDragTarget = DragTarget.START_PIN
                                onDragStartPin(startOffset, currentDisplayImageRect, bmpW, bmpH)
                                return@detectDragGestures
                            } else if (distToEndPin < pinHitRadius) {
                                currentDragTarget = DragTarget.END_PIN
                                onDragEndPin(startOffset, currentDisplayImageRect, bmpW, bmpH)
                                return@detectDragGestures
                            }
                        }

                        // 2. Check Crop Selection Handles
                        val crop = cropSelection?.rect
                        if (crop != null && !crop.isEmpty) {
                            val tl = Offset(crop.left, crop.top)
                            val tr = Offset(crop.right, crop.top)
                            val bl = Offset(crop.left, crop.bottom)
                            val br = Offset(crop.right, crop.bottom)

                            val midTop = Offset(crop.centerX(), crop.top)
                            val midBottom = Offset(crop.centerX(), crop.bottom)
                            val midLeft = Offset(crop.left, crop.centerY())
                            val midRight = Offset(crop.right, crop.centerY())

                            activeCropHandle = when {
                                hypot((startOffset.x - tl.x).toDouble(), (startOffset.y - tl.y).toDouble()) < handleTouchRadius -> CropHandleType.TOP_LEFT
                                hypot((startOffset.x - tr.x).toDouble(), (startOffset.y - tr.y).toDouble()) < handleTouchRadius -> CropHandleType.TOP_RIGHT
                                hypot((startOffset.x - bl.x).toDouble(), (startOffset.y - bl.y).toDouble()) < handleTouchRadius -> CropHandleType.BOTTOM_LEFT
                                hypot((startOffset.x - br.x).toDouble(), (startOffset.y - br.y).toDouble()) < handleTouchRadius -> CropHandleType.BOTTOM_RIGHT
                                hypot((startOffset.x - midTop.x).toDouble(), (startOffset.y - midTop.y).toDouble()) < handleTouchRadius -> CropHandleType.TOP_EDGE
                                hypot((startOffset.x - midBottom.x).toDouble(), (startOffset.y - midBottom.y).toDouble()) < handleTouchRadius -> CropHandleType.BOTTOM_EDGE
                                hypot((startOffset.x - midLeft.x).toDouble(), (startOffset.y - midLeft.y).toDouble()) < handleTouchRadius -> CropHandleType.LEFT_EDGE
                                hypot((startOffset.x - midRight.x).toDouble(), (startOffset.y - midRight.y).toDouble()) < handleTouchRadius -> CropHandleType.RIGHT_EDGE
                                crop.contains(startOffset.x, startOffset.y) -> CropHandleType.MOVE
                                else -> CropHandleType.NONE
                            }

                            if (activeCropHandle != CropHandleType.NONE) {
                                currentDragTarget = DragTarget.CROP_HANDLE
                                dragStartCropRect = RectF(crop)
                                dragStartTouch = startOffset
                                return@detectDragGestures
                            }
                        }

                        // 3. Otherwise, normal drawing stroke
                        currentDragTarget = DragTarget.DRAWING_STROKE
                        activeCropHandle = CropHandleType.NONE
                        onStrokeStart(startOffset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentPos = change.position
                        val bmpW = bitmap?.width ?: 1080
                        val bmpH = bitmap?.height ?: 2200

                        when (currentDragTarget) {
                            DragTarget.START_PIN -> {
                                onDragStartPin(currentPos, currentDisplayImageRect, bmpW, bmpH)
                            }
                            DragTarget.END_PIN -> {
                                onDragEndPin(currentPos, currentDisplayImageRect, bmpW, bmpH)
                            }
                            DragTarget.CROP_HANDLE -> {
                                val dx = currentPos.x - dragStartTouch.x
                                val dy = currentPos.y - dragStartTouch.y
                                val base = dragStartCropRect
                                val minSize = 60f

                                val newRect = when (activeCropHandle) {
                                    CropHandleType.TOP_LEFT -> RectF(
                                        min(base.right - minSize, base.left + dx),
                                        min(base.bottom - minSize, base.top + dy),
                                        base.right,
                                        base.bottom
                                    )
                                    CropHandleType.TOP_RIGHT -> RectF(
                                        base.left,
                                        min(base.bottom - minSize, base.top + dy),
                                        max(base.left + minSize, base.right + dx),
                                        base.bottom
                                    )
                                    CropHandleType.BOTTOM_LEFT -> RectF(
                                        min(base.right - minSize, base.left + dx),
                                        base.top,
                                        base.right,
                                        max(base.top + minSize, base.bottom + dy)
                                    )
                                    CropHandleType.BOTTOM_RIGHT -> RectF(
                                        base.left,
                                        base.top,
                                        max(base.left + minSize, base.right + dx),
                                        max(base.top + minSize, base.bottom + dy)
                                    )
                                    CropHandleType.TOP_EDGE -> RectF(
                                        base.left,
                                        min(base.bottom - minSize, base.top + dy),
                                        base.right,
                                        base.bottom
                                    )
                                    CropHandleType.BOTTOM_EDGE -> RectF(
                                        base.left,
                                        base.top,
                                        base.right,
                                        max(base.top + minSize, base.bottom + dy)
                                    )
                                    CropHandleType.LEFT_EDGE -> RectF(
                                        min(base.right - minSize, base.left + dx),
                                        base.top,
                                        base.right,
                                        base.bottom
                                    )
                                    CropHandleType.RIGHT_EDGE -> RectF(
                                        base.left,
                                        base.top,
                                        max(base.left + minSize, base.right + dx),
                                        base.bottom
                                    )
                                    CropHandleType.MOVE -> {
                                        val w = base.width()
                                        val h = base.height()
                                        val l = base.left + dx
                                        val t = base.top + dy
                                        RectF(l, t, l + w, t + h)
                                    }
                                    else -> base
                                }

                                val clamped = RectF(
                                    max(currentDisplayImageRect.left, newRect.left),
                                    max(currentDisplayImageRect.top, newRect.top),
                                    min(currentDisplayImageRect.right, newRect.right),
                                    min(currentDisplayImageRect.bottom, newRect.bottom)
                                )
                                onCropFrameUpdated(clamped, currentDisplayImageRect)
                            }
                            DragTarget.DRAWING_STROKE -> {
                                onStrokeMove(currentPos, currentDisplayImageRect, bmpW, bmpH)
                            }
                            DragTarget.NONE -> {}
                        }
                    },
                    onDragEnd = {
                        val bmpW = bitmap?.width ?: 1080
                        val bmpH = bitmap?.height ?: 2200
                        if (currentDragTarget == DragTarget.DRAWING_STROKE) {
                            onStrokeEnd(currentDisplayImageRect, bmpW, bmpH)
                        }
                        currentDragTarget = DragTarget.NONE
                        activeCropHandle = CropHandleType.NONE
                    },
                    onDragCancel = {
                        currentDragTarget = DragTarget.NONE
                        activeCropHandle = CropHandleType.NONE
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        if (bitmap != null) {
            val bmpWidth = bitmap.width.toFloat()
            val bmpHeight = bitmap.height.toFloat()

            val scale = min(canvasWidth / bmpWidth, canvasHeight / bmpHeight)
            val destWidth = bmpWidth * scale
            val destHeight = bmpHeight * scale
            val destLeft = (canvasWidth - destWidth) / 2f
            val destTop = (canvasHeight - destHeight) / 2f

            val imageRect = RectF(destLeft, destTop, destLeft + destWidth, destTop + destHeight)
            currentDisplayImageRect = imageRect
            onImageRectCalculated(imageRect)

            // Draw underlying screen bitmap
            val imageBitmap = bitmap.asImageBitmap()
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(destLeft.toInt(), destTop.toInt()),
                dstSize = IntSize(destWidth.toInt(), destHeight.toInt())
            )

            // Dark translucent scrim for authentic Circle to Search aesthetic
            drawRect(
                color = Color(0x38000000),
                topLeft = Offset(destLeft, destTop),
                size = Size(destWidth, destHeight)
            )

            val scaleX = destWidth / bmpWidth
            val scaleY = destHeight / bmpHeight

            // 1. Draw subtle hint boxes for all recognized OCR lines
            for (item in ocrItems) {
                val box = item.boundingBox
                val left = destLeft + box.left * scaleX
                val top = destTop + box.top * scaleY
                val right = destLeft + box.right * scaleX
                val bottom = destTop + box.bottom * scaleY

                drawRoundRect(
                    color = Color(0x1560A5FA),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 1f)
                )
            }

            // 2. Draw fine-grained selected tokens & Start/End Pins
            if (textSelection != null && textSelection.selectedTokens.isNotEmpty()) {
                for (token in textSelection.selectedTokens) {
                    val box = token.boundingBox
                    val left = destLeft + box.left * scaleX
                    val top = destTop + box.top * scaleY
                    val right = destLeft + box.right * scaleX
                    val bottom = destTop + box.bottom * scaleY

                    // Blue highlight pill
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xB81D4ED8), Color(0xCC2563EB))
                        ),
                        topLeft = Offset(left - 2f, top - 2f),
                        size = Size(right - left + 4f, bottom - top + 4f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                // Draw Start Pin (Droplet handle at top-left of first token)
                val firstBox = textSelection.selectedTokens.first().boundingBox
                val startX = destLeft + firstBox.left * scaleX
                val startY = destTop + firstBox.top * scaleY
                drawSelectionPin(
                    pinPoint = Offset(startX, startY),
                    isStart = true
                )

                // Draw End Pin (Droplet handle at bottom-right of last token)
                val lastBox = textSelection.selectedTokens.last().boundingBox
                val endX = destLeft + lastBox.right * scaleX
                val endY = destTop + lastBox.bottom * scaleY
                drawSelectionPin(
                    pinPoint = Offset(endX, endY),
                    isStart = false
                )
            }

            // 3. Draw Crop Selection Frame & Corner L-Brackets
            if (cropSelection != null) {
                val crop = cropSelection.rect

                // Ambient vignette outside crop
                drawRect(
                    color = Color(0x40000000),
                    topLeft = Offset(destLeft, destTop),
                    size = Size(destWidth, destHeight)
                )

                // Glowing animated dashed border
                drawRoundRect(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF818CF8),
                            Color(0xFFF472B6),
                            Color(0xFFFBBF24),
                            Color(0xFF38BDF8)
                        ),
                        center = Offset(crop.centerX(), crop.centerY())
                    ),
                    topLeft = Offset(crop.left, crop.top),
                    size = Size(crop.width(), crop.height()),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), phase * 45f)
                    )
                )

                // Draw Authentic Circle-to-Search Corner L-Brackets ⌜ ⌝ ⌞ ⌟
                val bracketLen = min(crop.width() * 0.25f, 32f).coerceAtLeast(16f)
                val bracketStroke = 4.5f
                val bracketColor = Color.White

                // Top-Left ⌜
                drawLine(bracketColor, Offset(crop.left - 2f, crop.top + bracketLen), Offset(crop.left - 2f, crop.top - 2f), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(crop.left - 2f, crop.top - 2f), Offset(crop.left + bracketLen, crop.top - 2f), bracketStroke, StrokeCap.Round)

                // Top-Right ⌝
                drawLine(bracketColor, Offset(crop.right + 2f, crop.top + bracketLen), Offset(crop.right + 2f, crop.top - 2f), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(crop.right + 2f, crop.top - 2f), Offset(crop.right - bracketLen, crop.top - 2f), bracketStroke, StrokeCap.Round)

                // Bottom-Left ⌞
                drawLine(bracketColor, Offset(crop.left - 2f, crop.bottom - bracketLen), Offset(crop.left - 2f, crop.bottom + 2f), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(crop.left - 2f, crop.bottom + 2f), Offset(crop.left + bracketLen, crop.bottom + 2f), bracketStroke, StrokeCap.Round)

                // Bottom-Right ⌟
                drawLine(bracketColor, Offset(crop.right + 2f, crop.bottom - bracketLen), Offset(crop.right + 2f, crop.bottom + 2f), bracketStroke, StrokeCap.Round)
                drawLine(bracketColor, Offset(crop.right + 2f, crop.bottom + 2f), Offset(crop.right - bracketLen, crop.bottom + 2f), bracketStroke, StrokeCap.Round)

                // Corner pill handles
                val corners = listOf(
                    Offset(crop.left, crop.top),
                    Offset(crop.right, crop.top),
                    Offset(crop.left, crop.bottom),
                    Offset(crop.right, crop.bottom)
                )
                for (corner in corners) {
                    drawCircle(color = Color(0xFF1E293B), radius = 10f, center = corner)
                    drawCircle(color = Color(0xFF38BDF8), radius = 7f, center = corner)
                }

                // Midpoint edge drag pills
                val midTop = Offset(crop.centerX(), crop.top)
                val midBottom = Offset(crop.centerX(), crop.bottom)
                val midLeft = Offset(crop.left, crop.centerY())
                val midRight = Offset(crop.right, crop.centerY())

                drawRoundRect(Color.White, Offset(midTop.x - 14f, midTop.y - 3.5f), Size(28f, 7f), CornerRadius(4f, 4f))
                drawRoundRect(Color.White, Offset(midBottom.x - 14f, midBottom.y - 3.5f), Size(28f, 7f), CornerRadius(4f, 4f))
                drawRoundRect(Color.White, Offset(midLeft.x - 3.5f, midLeft.y - 14f), Size(7f, 28f), CornerRadius(4f, 4f))
                drawRoundRect(Color.White, Offset(midRight.x - 3.5f, midRight.y - 14f), Size(7f, 28f), CornerRadius(4f, 4f))
            }

            // 4. Draw User Gesture Stroke (Neon Laser Ribbon)
            if (activeStrokePoints.size > 1) {
                val strokePath = Path()
                strokePath.moveTo(activeStrokePoints.first().x, activeStrokePoints.first().y)
                for (i in 1 until activeStrokePoints.size) {
                    val p = activeStrokePoints[i]
                    strokePath.lineTo(p.x, p.y)
                }

                // Neon aura
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0x9938BDF8), Color(0xCC818CF8), Color(0x99F472B6))
                    ),
                    style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // White core
                drawPath(
                    path = strokePath,
                    color = Color.White,
                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5. Draw Particle Trails
            for ((index, trail) in trailPoints.withIndex()) {
                val age = (trailPoints.size - index).toFloat() / trailPoints.size
                val radius = (1f - age) * 10f + 2f
                val alpha = (1f - age) * 0.9f
                drawCircle(
                    color = Color(0xFF67E8F9).copy(alpha = alpha),
                    radius = radius,
                    center = trail.position
                )
            }
        }
    }
}

/**
 * Draws a teardrop selection pin handle (like Android text selection pin / Google Lens).
 */
private fun DrawScope.drawSelectionPin(pinPoint: Offset, isStart: Boolean) {
    val pinColor = Color(0xFF2563EB)
    val pinRadius = 14f

    if (isStart) {
        // Start Pin: teardrop pointing up-right into top-left of text
        val center = Offset(pinPoint.x - 2f, pinPoint.y - 16f)
        // Connecting line
        drawLine(
            color = pinColor,
            start = pinPoint,
            end = center,
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        // Pin head
        drawCircle(color = Color.White, radius = pinRadius + 2.5f, center = center)
        drawCircle(color = pinColor, radius = pinRadius, center = center)
        drawCircle(color = Color.White, radius = 5f, center = center)
    } else {
        // End Pin: teardrop pointing down-left out of bottom-right of text
        val center = Offset(pinPoint.x + 2f, pinPoint.y + 16f)
        // Connecting line
        drawLine(
            color = pinColor,
            start = pinPoint,
            end = center,
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        // Pin head
        drawCircle(color = Color.White, radius = pinRadius + 2.5f, center = center)
        drawCircle(color = pinColor, radius = pinRadius, center = center)
        drawCircle(color = Color.White, radius = 5f, center = center)
    }
}

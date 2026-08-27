package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.model.OcrTextItem
import com.example.model.OcrToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

class OcrManager {

    private val japaneseRecognizer by lazy {
        try {
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        } catch (e: Throwable) {
            Log.w("OcrManager", "Japanese recognizer init failed, falling back to Latin", e)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    fun warmUp() {
        try {
            // Trigger lazy loading of ML Kit models on a background worker thread
            japaneseRecognizer
        } catch (e: Throwable) {
            Log.w("OcrManager", "OCR warm-up notice", e)
        }
    }

    suspend fun recognizeText(bitmap: Bitmap): List<OcrTextItem> = withContext(Dispatchers.Default) {
        val inputImage = try {
            InputImage.fromBitmap(bitmap, 0)
        } catch (e: Throwable) {
            Log.e("OcrManager", "Failed to create InputImage from bitmap", e)
            return@withContext emptyList()
        }

        suspendCancellableCoroutine { continuation ->
            japaneseRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val items = mutableListOf<OcrTextItem>()
                    var globalTokenIdx = 0
                    var globalLineIdx = 0

                    // 1. Gather all non-empty lines from all text blocks
                    val rawLines = visionText.textBlocks.flatMap { it.lines }
                        .filter { it.text.isNotBlank() }

                    // 2. Sort all lines strictly into top-to-bottom reading order
                    val sortedLines = sortLinesInReadingOrder(rawLines)

                    for (line in sortedLines) {
                        val lineBox = line.boundingBox ?: Rect(0, 0, 100, 50)
                        val currentLineIdx = globalLineIdx++
                        val tokens = mutableListOf<OcrToken>()
                        var tokenInLineIdx = 0

                        val elements = line.elements.sortedBy { it.boundingBox?.left ?: 0 }
                        if (elements.isNotEmpty()) {
                            for (element in elements) {
                                val elementBox = element.boundingBox ?: lineBox
                                val elementText = element.text
                                val symbols = try {
                                    element.symbols.sortedBy { it.boundingBox?.left ?: 0 }
                                } catch (e: Throwable) {
                                    emptyList<Text.Symbol>()
                                }

                                if (symbols.isNotEmpty() && containsJapaneseOrMixed(elementText)) {
                                    // Use ML Kit direct Symbol bounding boxes for highest precision
                                    for (symbol in symbols) {
                                        val symBox = symbol.boundingBox ?: elementBox
                                        val symText = symbol.text
                                        tokens.add(
                                            OcrToken(
                                                id = UUID.randomUUID().toString(),
                                                text = symText,
                                                boundingBox = symBox,
                                                lineIndex = currentLineIdx,
                                                tokenIndex = tokenInLineIdx++,
                                                globalIndex = globalTokenIdx++,
                                                isJapaneseOrCjk = containsJapanese(symText)
                                            )
                                        )
                                    }
                                } else if (containsJapanese(elementText)) {
                                    // Japanese / CJK fallback when symbols not directly populated
                                    val charCount = elementText.length
                                    val elemW = elementBox.width()

                                    for (i in 0 until charCount) {
                                        val ch = elementText[i].toString()
                                        val left = elementBox.left + (elemW * i / charCount)
                                        val right = elementBox.left + (elemW * (i + 1) / charCount)
                                        val charBox = Rect(left, elementBox.top, right, elementBox.bottom)

                                        tokens.add(
                                            OcrToken(
                                                id = UUID.randomUUID().toString(),
                                                text = ch,
                                                boundingBox = charBox,
                                                lineIndex = currentLineIdx,
                                                tokenIndex = tokenInLineIdx++,
                                                globalIndex = globalTokenIdx++,
                                                isJapaneseOrCjk = true
                                            )
                                        )
                                    }
                                } else {
                                    // Latin / English / Number / Symbol: treat as word token
                                    tokens.add(
                                        OcrToken(
                                            id = UUID.randomUUID().toString(),
                                            text = elementText,
                                            boundingBox = elementBox,
                                            lineIndex = currentLineIdx,
                                            tokenIndex = tokenInLineIdx++,
                                            globalIndex = globalTokenIdx++,
                                            isJapaneseOrCjk = false
                                        )
                                    )
                                }
                            }
                        } else {
                            // Direct line text fallback
                            val lineText = line.text
                            if (containsJapanese(lineText)) {
                                val charCount = lineText.length
                                val lineW = lineBox.width()
                                for (i in 0 until charCount) {
                                    val ch = lineText[i].toString()
                                    val left = lineBox.left + (lineW * i / charCount)
                                    val right = lineBox.left + (lineW * (i + 1) / charCount)
                                    val charBox = Rect(left, lineBox.top, right, lineBox.bottom)
                                    tokens.add(
                                        OcrToken(
                                            id = UUID.randomUUID().toString(),
                                            text = ch,
                                            boundingBox = charBox,
                                            lineIndex = currentLineIdx,
                                            tokenIndex = tokenInLineIdx++,
                                            globalIndex = globalTokenIdx++,
                                            isJapaneseOrCjk = true
                                        )
                                    )
                                }
                            } else {
                                val words = lineText.split(Regex("\\s+")).filter { it.isNotEmpty() }
                                val totalLen = lineText.length.coerceAtLeast(1)
                                var currentOffset = 0
                                for (word in words) {
                                    val startIdx = lineText.indexOf(word, currentOffset).coerceAtLeast(0)
                                    val endIdx = startIdx + word.length
                                    currentOffset = endIdx

                                    val left = lineBox.left + (lineBox.width() * startIdx / totalLen)
                                    val right = lineBox.left + (lineBox.width() * endIdx / totalLen)
                                    val wordBox = Rect(left, lineBox.top, right, lineBox.bottom)

                                    tokens.add(
                                        OcrToken(
                                            id = UUID.randomUUID().toString(),
                                            text = word,
                                            boundingBox = wordBox,
                                            lineIndex = currentLineIdx,
                                            tokenIndex = tokenInLineIdx++,
                                            globalIndex = globalTokenIdx++,
                                            isJapaneseOrCjk = false
                                        )
                                    )
                                }
                            }
                        }

                        if (tokens.isNotEmpty()) {
                            items.add(
                                OcrTextItem(
                                    id = UUID.randomUUID().toString(),
                                    text = line.text,
                                    boundingBox = lineBox,
                                    lineIndex = currentLineIdx,
                                    blockIndex = 0,
                                    tokens = tokens,
                                    isSelected = false
                                )
                            )
                        }
                    }
                    continuation.resume(items)
                }
                .addOnFailureListener { error ->
                    Log.e("OcrManager", "ML Kit text recognition failed", error)
                    continuation.resume(emptyList())
                }
        }
    }

    /**
     * Clusters lines into horizontal rows and sorts rows strictly top-to-bottom,
     * and lines within each row left-to-right.
     * Prevents ML Kit block fragmentations from inverting vertical line reading order.
     */
    private fun sortLinesInReadingOrder(lines: List<Text.Line>): List<Text.Line> {
        if (lines.size <= 1) return lines

        // 1. Initial sort by top coordinate, then by left coordinate
        val sortedByTop = lines.sortedWith(
            compareBy<Text.Line> { it.boundingBox?.top ?: 0 }
                .thenBy { it.boundingBox?.left ?: 0 }
        )

        val rows = mutableListOf<MutableList<Text.Line>>()

        for (line in sortedByTop) {
            val box = line.boundingBox ?: Rect(0, 0, 0, 0)
            val top = box.top
            val bottom = box.bottom
            val height = (bottom - top).coerceAtLeast(1)
            val centerY = (top + bottom) / 2

            var matchedRow: MutableList<Text.Line>? = null

            for (row in rows) {
                val rTop = row.minOf { it.boundingBox?.top ?: top }
                val rBottom = row.maxOf { it.boundingBox?.bottom ?: bottom }
                val rHeight = (rBottom - rTop).coerceAtLeast(1)

                val overlapY = max(0, min(bottom, rBottom) - max(top, rTop))
                val overlapRatio = overlapY.toFloat() / min(height, rHeight)

                // Overlap by at least 40% of line height or vertical center inside row span
                if (overlapRatio >= 0.40f || (centerY in rTop..rBottom)) {
                    matchedRow = row
                    break
                }
            }

            if (matchedRow != null) {
                matchedRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        // 2. Sort rows top-to-bottom by average top coordinate
        rows.sortBy { row ->
            row.map { it.boundingBox?.top ?: 0 }.average()
        }

        // 3. Sort lines within each row left-to-right
        for (row in rows) {
            row.sortBy { it.boundingBox?.left ?: 0 }
        }

        return rows.flatten()
    }

    private fun containsJapaneseOrMixed(text: String): Boolean {
        return text.any { char ->
            val code = char.code
            code in 0x3040..0x309F || // Hiragana
            code in 0x30A0..0x30FF || // Katakana
            code in 0x4E00..0x9FAF || // Kanji
            code in 0x3400..0x4DBF || // Kanji Extension A
            code in 0xFF00..0xFFEF   // Fullwidth
        }
    }

    private fun containsJapanese(text: String): Boolean {
        for (char in text) {
            val codePoint = char.code
            if (codePoint in 0x3040..0x309F ||
                codePoint in 0x30A0..0x30FF ||
                codePoint in 0x4E00..0x9FAF ||
                codePoint in 0x3400..0x4DBF ||
                codePoint in 0xFF00..0xFFEF
            ) {
                return true
            }
        }
        return false
    }
}

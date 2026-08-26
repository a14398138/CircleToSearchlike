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

class OcrManager {

    private val japaneseRecognizer by lazy {
        try {
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        } catch (e: Throwable) {
            Log.w("OcrManager", "Japanese recognizer init failed, falling back to Latin", e)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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

                    // Sort text blocks in natural vertical reading order (top to bottom, left to right)
                    val sortedBlocks = visionText.textBlocks.sortedWith(
                        compareBy<Text.TextBlock> { (it.boundingBox?.top ?: 0) / 40 }
                            .thenBy { it.boundingBox?.left ?: 0 }
                    )

                    var blockIdx = 0
                    for (block in sortedBlocks) {
                        // Sort lines within block top to bottom
                        val sortedLines = block.lines.sortedBy { it.boundingBox?.top ?: 0 }

                        for (line in sortedLines) {
                            val lineBox = line.boundingBox ?: block.boundingBox ?: Rect(0, 0, 100, 50)
                            val currentLineIdx = globalLineIdx++
                            val tokens = mutableListOf<OcrToken>()
                            var tokenInLineIdx = 0

                            val elements = line.elements.sortedBy { it.boundingBox?.left ?: 0 }
                            if (elements.isNotEmpty()) {
                                for (element in elements) {
                                    val elementBox = element.boundingBox ?: lineBox
                                    val elementText = element.text
                                    val symbols = try { element.symbols } catch (e: Throwable) { emptyList<Text.Symbol>() }

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
                                        // Japanese / CJK fallback when symbols not directly populated:
                                        // Decompose into individual character tokens with proportional boxes
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
                                        blockIndex = blockIdx,
                                        tokens = tokens,
                                        isSelected = false
                                    )
                                )
                            }
                        }
                        blockIdx++
                    }
                    continuation.resume(items)
                }
                .addOnFailureListener { error ->
                    Log.e("OcrManager", "ML Kit text recognition failed", error)
                    continuation.resume(emptyList())
                }
        }
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

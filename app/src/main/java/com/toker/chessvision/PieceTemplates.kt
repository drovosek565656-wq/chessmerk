package com.toker.chessvision

import android.graphics.Bitmap
import android.graphics.Color

object PieceTemplates {
    fun match(cell: Bitmap): Char? {
        val w = cell.width
        val h = cell.height
        if (w < 8 || h < 8) return null

        val bgSample = averageBrightness(cell, 0, 0, w / 6, h / 6)

        val mask = Array(h) { BooleanArray(w) }
        var pixelCount = 0
        var minX = w; var maxX = 0; var minY = h; var maxY = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val px = cell.getPixel(x, y)
                val brightness = (Color.red(px) + Color.green(px) + Color.blue(px)) / 3
                if (kotlin.math.abs(brightness - bgSample) > DIFF_THRESHOLD) {
                    mask[y][x] = true
                    pixelCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val fillRatio = pixelCount.toFloat() / (w * h)
        if (fillRatio < EMPTY_THRESHOLD) return null

        val silWidth = maxX - minX + 1
        val silHeight = maxY - minY + 1
        if (silWidth <= 0 || silHeight <= 0) return null

        var fgSum = 0L; var fgCount = 0
        for (y in minY..maxY) for (x in minX..maxX) {
            if (mask[y][x]) {
                val px = cell.getPixel(x, y)
                fgSum += (Color.red(px) + Color.green(px) + Color.blue(px)) / 3
                fgCount++
            }
        }
        val fgBrightness = if (fgCount > 0) fgSum / fgCount else 0
        val isWhite = fgBrightness > WHITE_THRESHOLD

        // геометрические признаки силуэта для классификации типа фигуры
        val aspectRatio = silWidth.toFloat() / silHeight.toFloat()

        // профиль ширины по 5 горизонтальным срезам: верх/верх-середина/середина/низ-середина/низ
        val bands = IntArray(5)
        for (b in 0 until 5) {
            val yStart = minY + (silHeight * b / 5)
            val yEnd = minY + (silHeight * (b + 1) / 5)
            var bandWidth = 0
            for (y in yStart until yEnd.coerceAtMost(h)) {
                var left = -1; var right = -1
                for (x in minX..maxX) {
                    if (mask[y][x]) {
                        if (left == -1) left = x
                        right = x
                    }
                }
                if (right - left > bandWidth) bandWidth = right - left
            }
            bands[b] = bandWidth
        }

        // верхняя полоса (крона) узкая/широкая относительно нижней (основание) —
        // пешка: маленькая узкая крона сверху, почти треугольный силуэт, низкий aspectRatio
        // ладья: широкий плоский верх (зубцы), почти прямоугольный силуэт
        // слон: узкий пик сверху + характерная "талия" в band[2]
        // конь: асимметричный профиль (голова сбоку), band[1] шире band[0]
        // ферзь: несколько пиков сверху (корона), band[0] относительно широкий с неровным краем
        // король: крест/шарик на самом верху, band[0] узкий но band[1] резко шире

        val topBand = bands[0]
        val upperMidBand = bands[1]
        val midBand = bands[2]
        val lowerMidBand = bands[3]
        val baseBand = bands[4]

        val waist = midBand.toFloat() / baseBand.coerceAtLeast(1)
        val topToBase = topBand.toFloat() / baseBand.coerceAtLeast(1)
        val crownJump = upperMidBand.toFloat() / topBand.coerceAtLeast(1)

        val pieceType: Char = when {
            silHeight < cell.height * 0.45f && aspectRatio > 0.7f -> 'p' // пешка — низкий силуэт
            topToBase > 0.75f && waist > 0.75f -> 'r' // ладья — почти прямоугольник
            crownJump > 1.6f -> 'q' // ферзь — резкий скачок ширины у короны
            waist < 0.55f -> 'b' // слон — выраженная талия
            topBand < baseBand / 4 && silHeight > cell.height * 0.6f -> 'k' // король — тонкий крест + высокий
            else -> 'n' // конь — остаточная категория (асимметричный профиль)
        }

        return if (isWhite) pieceType.uppercaseChar() else pieceType
    }

    private fun averageBrightness(bmp: Bitmap, x0: Int, y0: Int, w: Int, h: Int): Int {
        var sum = 0L; var count = 0
        for (y in y0 until (y0 + h).coerceAtMost(bmp.height)) {
            for (x in x0 until (x0 + w).coerceAtMost(bmp.width)) {
                val px = bmp.getPixel(x, y)
                sum += (Color.red(px) + Color.green(px) + Color.blue(px)) / 3
                count++
            }
        }
        return if (count > 0) (sum / count).toInt() else 128
    }

    private const val DIFF_THRESHOLD = 35
    private const val EMPTY_THRESHOLD = 0.03f
    private const val WHITE_THRESHOLD = 150
}

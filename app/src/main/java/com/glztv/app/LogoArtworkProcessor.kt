package com.glztv.app

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Pure pixel processing, shared by the Coil transform and JVM regression tests. */
internal object LogoArtworkProcessor {
    data class Artwork(val pixels: IntArray, val background: Int, val left: Int, val top: Int, val right: Int, val bottom: Int)
    private fun alpha(c: Int) = c ushr 24
    private fun red(c: Int) = (c ushr 16) and 255
    private fun green(c: Int) = (c ushr 8) and 255
    private fun blue(c: Int) = c and 255
    private fun white(c: Int) = red(c) >= 238 && green(c) >= 238 && blue(c) >= 238
    private fun distance(a: Int, b: Int) = maxOf(kotlin.math.abs(red(a)-red(b)), kotlin.math.abs(green(a)-green(b)), kotlin.math.abs(blue(a)-blue(b)))
    private fun saturation(c: Int): Double {
        val high = maxOf(red(c), green(c), blue(c))
        return if (high == 0) 0.0 else (high - minOf(red(c), green(c), blue(c))).toDouble() / high
    }
    private fun color(value: String): Int? = value.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }
        ?.drop(1)?.toLongOrNull(16)?.toInt()?.or(0xff000000.toInt())
    private fun darken(c: Int): Int = 0xff000000.toInt() or ((red(c)*.30).toInt() shl 16) or ((green(c)*.30).toInt() shl 8) or (blue(c)*.30).toInt()
    fun luminance(c: Int): Double {
        fun linear(v: Int): Double { val n = v / 255.0; return if (n <= .04045) n / 12.92 else ((n + .055) / 1.055).pow(2.4) }
        return .2126*linear(red(c)) + .7152*linear(green(c)) + .0722*linear(blue(c))
    }
    fun contrast(a: Int, b: Int): Double = (max(luminance(a), luminance(b)) + .05) / (min(luminance(a), luminance(b)) + .05)
    fun foreground(background: Int): Int = if (contrast(background, -1) >= contrast(background, 0xff000000.toInt())) -1 else 0xff000000.toInt()
    fun fallback(style: ChannelLogoStyle, name: String): Int = color(style.background)
        ?: if (Regex("\\bESPN\\b", RegexOption.IGNORE_CASE).containsMatchIn(name)) 0xffe60012.toInt() else 0xff243447.toInt()

    fun process(input: IntArray, width: Int, height: Int, style: ChannelLogoStyle, name: String): Artwork {
        require(width > 0 && height > 0 && input.size == width * height)
        val pixels = input.copyOf()
        // Providers also place a colored rectangular canvas inside transparent margins.
        var outerLeft = width; var outerTop = height; var outerRight = -1; var outerBottom = -1
        input.forEachIndexed { i, c -> if (alpha(c) > 16) {
            outerLeft = min(outerLeft, i % width); outerRight = max(outerRight, i % width)
            outerTop = min(outerTop, i / width); outerBottom = max(outerBottom, i / width)
        } }
        val corners = if (outerRight >= outerLeft) intArrayOf(
            input[outerTop*width+outerLeft], input[outerTop*width+outerRight],
            input[outerBottom*width+outerLeft], input[outerBottom*width+outerRight]
        ) else intArrayOf()
        val candidateCanvas = corners.firstOrNull { candidate -> alpha(candidate) >= 80 && corners.count { alpha(it) >= 80 && distance(it, candidate) <= 18 } >= 3 }
        // A solid rectangle without contrasting content may itself be the logo.
        val canvas = candidateCanvas?.takeIf { candidate -> white(candidate) ||
            input.count { alpha(it) > 80 && distance(it, candidate) > 60 } > max(3, input.size / 200) }
        val whiteCanvas = canvas != null && white(canvas)
        // Remove only the connected outer canvas, including anti-aliased white edges.
        if (style.removeBackground && canvas != null) {
            val queue = IntArray(pixels.size)
            val seen = BooleanArray(pixels.size)
            var tail = 0
            fun offer(i: Int) {
                if (i < 0 || i >= pixels.size || seen[i]) return
                seen[i] = true
                if (alpha(input[i]) <= 16 || (whiteCanvas && white(input[i])) || distance(input[i], canvas) <= 24) queue[tail++] = i
            }
            for (x in 0 until width) { offer(x); offer((height-1)*width+x) }
            for (y in 0 until height) { offer(y*width); offer(y*width+width-1) }
            var head = 0
            while (head < tail) {
                val i = queue[head++]
                pixels[i] = 0
                if (i % width > 0) offer(i-1)
                if (i % width < width-1) offer(i+1)
                if (i >= width) offer(i-width)
                if (i < pixels.size-width) offer(i+width)
            }
        }
        // Quantized bins weight visible saturated pixels. Gray/white canvas never wins.
        val counts = mutableMapOf<Int, Int>()
        val examples = mutableMapOf<Int, Int>()
        var visible = 0
        var saturated = 0
        pixels.forEach { c ->
            if (alpha(c) > 80) {
                visible++
                if (saturation(c) > .25 && maxOf(red(c),green(c),blue(c)) > 45) {
                    val bin = ((red(c)/32) shl 6) or ((green(c)/32) shl 3) or (blue(c)/32)
                    counts[bin] = (counts[bin] ?: 0) + 1
                    examples[bin] = c or 0xff000000.toInt()
                    saturated++
                }
            }
        }
        val winner = counts.maxByOrNull { it.value }
        val dominant = if (canvas != null && !whiteCanvas && saturation(canvas) > .25) canvas
            else winner?.let { examples[it.key] }
        val dominantShare = (winner?.value ?: 0).toDouble() / max(1, saturated)
        val significantBins = counts.values.count { it > max(3.0, saturated * .08) }
        val monochrome = style.mode == "monochrome" || (style.mode == "auto" &&
            (saturated == 0 || (dominantShare > .85 && significantBins <= 2 && saturated.toDouble()/max(1,visible) > .65)))
        val background = color(style.background) ?: if (Regex("\\bESPN\\b", RegexOption.IGNORE_CASE).containsMatchIn(name)) 0xffe60012.toInt()
            else dominant?.let { if (monochrome) it else darken(it) } ?: 0xff243447.toInt()
        val ink = color(style.foreground) ?: foreground(background)
        if (monochrome) {
            for (i in pixels.indices) {
                val c = pixels[i]
                // White holes in a single-color wordmark on a white canvas remain holes.
                if (style.removeBackground && whiteCanvas && saturated > 0 && white(c)) pixels[i] = 0
                else if (alpha(c) > 0) pixels[i] = (alpha(c) shl 24) or (ink and 0x00ffffff)
            }
        }
        var left = width; var top = height; var right = -1; var bottom = -1
        pixels.forEachIndexed { i, c -> if (alpha(c) > 16) {
            left = min(left, i % width); right = max(right, i % width)
            top = min(top, i / width); bottom = max(bottom, i / width)
        } }
        return Artwork(pixels, background, left, top, right, bottom)
    }
}

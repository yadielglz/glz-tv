package com.glztv.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size
import coil3.transform.Transformation
import com.glztv.app.Channel
import com.glztv.app.EpgGuide

/**
 * Playlist artwork often has transparent margins or a white canvas around the actual logo.
 * Trim those margins before fitting the artwork into the shared circular badge.
 * Colored, full-bleed artwork is left intact.
 */
private object TrimChannelLogoMargins : Transformation() {
    override val cacheKey: String = "trim-channel-logo-margins-v1"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        if (width == 0 || height == 0) return input

        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)
        fun isWhite(pixel: Int): Boolean =
            android.graphics.Color.alpha(pixel) >= 240 &&
                android.graphics.Color.red(pixel) >= 245 &&
                android.graphics.Color.green(pixel) >= 245 &&
                android.graphics.Color.blue(pixel) >= 245

        val whiteCanvas = isWhite(pixels[0]) && isWhite(pixels[width - 1]) &&
            isWhite(pixels[(height - 1) * width]) && isWhite(pixels[pixels.lastIndex])
        var left = width
        var top = height
        var right = -1
        var bottom = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                if (android.graphics.Color.alpha(pixel) > 16 &&
                    (!whiteCanvas || !isWhite(pixel))) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        if (right < left || bottom < top) return input
        // Keep a thin edge so anti-aliased outlines never touch the badge boundary.
        val edge = maxOf(1, minOf(width, height) / 100)
        left = (left - edge).coerceAtLeast(0)
        top = (top - edge).coerceAtLeast(0)
        right = (right + edge).coerceAtMost(width - 1)
        bottom = (bottom + edge).coerceAtMost(height - 1)
        if (left == 0 && top == 0 && right == width - 1 && bottom == height - 1) return input
        return Bitmap.createBitmap(input, left, top, right - left + 1, bottom - top + 1)
    }
}

@Composable
fun ChannelLogo(
    channel: Channel,
    size: Dp,
    guide: EpgGuide? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoUrl = channel.logoUrl.takeIf { it.isNotBlank() } ?: guide?.logoForChannel(channel)
    val model = remember(logoUrl, channel.headers) {
        if (logoUrl.isNullOrBlank()) null
        else {
            val request = ImageRequest.Builder(context)
                .data(logoUrl)
                .transformations(TrimChannelLogoMargins)
            if (channel.headers.isNotEmpty()) {
                val headersBuilder = NetworkHeaders.Builder()
                channel.headers.forEach { (k, v) -> headersBuilder.set(k, v) }
                request.httpHeaders(headersBuilder.build())
            }
            request.build()
        }
    }
    var logoLoaded by remember(model) { mutableStateOf(false) }
    val initials = remember(channel.name) {
        channel.name.split(Regex("\\s+"))
            .mapNotNull { word -> word.firstOrNull(Char::isLetterOrDigit) }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifBlank { "TV" }
    }
    Surface(
        modifier = modifier.then(Modifier.size(size)),
        shape = CircleShape,
        color = Color(0xFFF7F7F4),
        shadowElevation = 3.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!logoLoaded) {
                Text(
                    text = initials,
                    color = Color(0xFF243447),
                    fontSize = (size.value * .27f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "${channel.name} logo",
                    modifier = Modifier.fillMaxSize().padding(size * .14f),
                    contentScale = ContentScale.Fit,
                    onSuccess = { logoLoaded = true },
                    onError = { logoLoaded = false }
                )
            }
        }
    }
}

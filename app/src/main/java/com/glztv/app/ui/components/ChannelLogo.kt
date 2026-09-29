package com.glztv.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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

/** Cache the complete badge so list scrolling never repeats pixel analysis. */
private class BrandChannelLogo(private val style: com.glztv.app.ChannelLogoStyle, private val name: String) : Transformation() {
    override val cacheKey = "brand-channel-logo-v1:$style:$name"
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Bound analysis memory even when a provider sends a large poster as artwork.
        val scale = minOf(1f, 256f / maxOf(input.width, input.height))
        val source = if (scale < 1f) Bitmap.createScaledBitmap(input, maxOf(1, (input.width * scale).toInt()), maxOf(1, (input.height * scale).toInt()), true) else input
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        val artwork = com.glztv.app.LogoArtworkProcessor.process(pixels, source.width, source.height, style, name)
        val output = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(artwork.background)
        if (artwork.right >= artwork.left && artwork.bottom >= artwork.top) {
            val cleaned = Bitmap.createBitmap(artwork.pixels, source.width, source.height, Bitmap.Config.ARGB_8888)
            val width = artwork.right - artwork.left + 1
            val height = artwork.bottom - artwork.top + 1
            val fit = minOf(184f / width, 164f / height)
            val w = width * fit; val h = height * fit
            canvas.drawBitmap(cleaned,
                android.graphics.Rect(artwork.left, artwork.top, artwork.right + 1, artwork.bottom + 1),
                android.graphics.RectF((256-w)/2, (256-h)/2, (256+w)/2, (256+h)/2),
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
            cleaned.recycle()
        } else {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = com.glztv.app.LogoArtworkProcessor.foreground(artwork.background)
                textSize = 64f; textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText(name.take(2).uppercase(), 128f, 128f - (paint.ascent() + paint.descent())/2, paint)
        }
        if (source !== input) source.recycle()
        return output
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
    val model = remember(logoUrl, channel.headers, channel.logoStyle, channel.name) {
        if (logoUrl.isNullOrBlank()) null
        else {
            val request = ImageRequest.Builder(context)
                .data(logoUrl)
                .size(256)
                .transformations(BrandChannelLogo(channel.logoStyle, channel.name))
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
        color = Color(com.glztv.app.LogoArtworkProcessor.fallback(channel.logoStyle, channel.name)),
        shadowElevation = 3.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!logoLoaded) {
                Text(
                    text = initials,
                    color = Color(com.glztv.app.LogoArtworkProcessor.foreground(com.glztv.app.LogoArtworkProcessor.fallback(channel.logoStyle, channel.name))),
                    fontSize = (size.value * .27f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "${channel.name} logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onSuccess = { logoLoaded = true },
                    onError = { logoLoaded = false }
                )
            }
        }
    }
}

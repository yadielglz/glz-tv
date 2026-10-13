package com.glztv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.glztv.app.Channel
import com.glztv.app.EpgGuide

@Composable
fun GoogleTvChannelLogo(
    channel: Channel,
    size: Dp,
    guide: EpgGuide? = null,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp)
) {
    val context = LocalContext.current
    val logoUrl = channel.logoUrl.takeIf { it.isNotBlank() } ?: guide?.logoForChannel(channel)
    val model = remember(logoUrl, channel.headers) {
        if (logoUrl.isNullOrBlank()) null
        else if (channel.headers.isEmpty()) logoUrl
        else {
            val headersBuilder = NetworkHeaders.Builder()
            channel.headers.forEach { (k, v) -> headersBuilder.set(k, v) }
            ImageRequest.Builder(context)
                .data(logoUrl)
                .httpHeaders(headersBuilder.build())
                .build()
        }
    }
    var logoLoaded by remember(model) { mutableStateOf(false) }
    val initials = channel.monogram.ifBlank { "TV" }

    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = Color(0xFF1E2129),
        shadowElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!logoLoaded) {
                Text(
                    text = initials,
                    color = Color(0xFFE2E8F0),
                    fontSize = (size.value * 0.28f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "${channel.name} logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.12f),
                    contentScale = ContentScale.Fit,
                    onSuccess = { logoLoaded = true },
                    onError = { logoLoaded = false }
                )
            }
        }
    }
}

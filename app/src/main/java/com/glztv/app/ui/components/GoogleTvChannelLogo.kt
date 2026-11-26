package com.glztv.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.glztv.app.R
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome

private val UniformLogoTileGradient = Brush.verticalGradient(
    listOf(Color(0xFF1A202B), Color(0xFF11151D))
)

/**
 * Uniform DirecTV HD Channel & Live Event Logo tile (v1.48.4.35).
 *
 * - Live Event channels automatically render crisp, uniform sport-branded badges
 *   (NFL, NBA, MLB, NHL, NCAA, UFC, BOXING, MLS, or LIVE SPORTS) for instant consistency.
 * - Regular linear channels render inside a standardized dark metallic tile with 1dp
 *   hairline border and balanced inner padding (`ContentScale.Fit`) so every logo has
 *   uniform visual weight across Home, EPG Guide, Mini Guide, OSD, and Channel Panel.
 */
@Composable
fun GoogleTvChannelLogo(
    channel: Channel,
    size: Dp,
    guide: EpgGuide? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    val sportBadgeRes = remember(channel.id, channel.name, channel.group, channel.eventTitle) {
        if (channel.isLiveEvent()) resolveLiveEventSportDrawable(channel) else null
    }

    if (sportBadgeRes != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(1.dp, DtvChrome.CardBorder, shape)
        ) {
            Image(
                painter = painterResource(id = sportBadgeRes),
                contentDescription = "${channel.name} sport badge",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

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

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(UniformLogoTileGradient)
            .border(1.dp, DtvChrome.CardBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        if (!logoLoaded) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(DtvChrome.ElectricBlue.copy(alpha = 0.7f))
            )
            Text(
                text = initials,
                color = Color(0xFFE8EEF5),
                fontSize = (size.value * 0.34f).coerceIn(10f, 22f).sp,
                fontFamily = BarlowCondensedFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "${channel.name} logo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.13f),
                contentScale = ContentScale.Fit,
                onSuccess = { logoLoaded = true },
                onError = { logoLoaded = false }
            )
        }
    }
}

@DrawableRes
internal fun resolveLiveEventSportDrawable(channel: Channel): Int {
    val text = "${channel.id} ${channel.name} ${channel.group} ${channel.eventTitle.orEmpty()}".uppercase()
    return when {
        Regex("\\b(NFL|SUNDAY\\s+TICKET|RED\\s*ZONE|TNF|SNF|MNF)\\b").containsMatchIn(text) -> R.drawable.ic_sport_nfl
        Regex("\\bNBA\\b").containsMatchIn(text) -> R.drawable.ic_sport_nba
        Regex("\\b(MLB|BASEBALL)\\b").containsMatchIn(text) -> R.drawable.ic_sport_mlb
        Regex("\\b(NHL|HOCKEY)\\b").containsMatchIn(text) -> R.drawable.ic_sport_nhl
        Regex("\\b(NCAA|NCAAF|CFB|COLLEGE\\s+FOOTBALL)\\b").containsMatchIn(text) -> R.drawable.ic_sport_ncaa
        Regex("\\b(UFC|MMA|PFL)\\b").containsMatchIn(text) -> R.drawable.ic_sport_ufc
        Regex("\\bBOXING\\b").containsMatchIn(text) -> R.drawable.ic_sport_boxing
        Regex("\\b(MLS|SOCCER|PREMIER|LALIGA|UEFA)\\b").containsMatchIn(text) -> R.drawable.ic_sport_mls
        else -> R.drawable.ic_sport_live
    }
}

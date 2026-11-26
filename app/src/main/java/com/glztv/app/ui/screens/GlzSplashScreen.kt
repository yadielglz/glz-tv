package com.glztv.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.R
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome

/**
 * Fullscreen DirecTV HD Broadcast Splash Screen (v1.48.4.35).
 * Displays for 1.4s–3.0s while initial channels, EPG, and GlzHub config finish loading.
 */
@Composable
fun GlzSplashScreen(
    statusText: String = "Loading live channels & guide…",
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "splash-bar")
    val sweep by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splash-sweep"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        // Fullscreen generated GlzTV broadcast orbital wallpaper
        Image(
            painter = painterResource(id = R.drawable.glztv_splash_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Subtle vignette so centered logo & loading bar pop cleanly
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x4405080D),
                            Color(0x2205080D),
                            Color(0xAA05080D)
                        )
                    )
                )
        )

        // Centered Stacked GlzTV #3 Logo + Sleek Broadcast Loading Bar
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.glztv_logo_stacked),
                contentDescription = "GlzTV",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(148.dp)
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(86.dp)
                        .offset(x = (240.dp * sweep))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    DtvChrome.ElectricBlue,
                                    DtvChrome.ElectricBlueSoft,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = statusText.ifBlank { "Loading live channels & guide…" },
                color = DtvChrome.SecondaryText,
                fontSize = 12.sp,
                fontFamily = BarlowCondensedFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
        }

        // Subtle bottom-right build version badge
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            color = DtvChrome.MutedText.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(
                    if (BuildConfig.IS_MOBILE) Modifier.padding(bottom = 12.dp) else Modifier
                )
                .padding(horizontal = 32.dp, vertical = 22.dp)
        )
    }
}

package com.glztv.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.ui.components.GlzSectionActionButton
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.LocalTvViewport
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SetupStep(val index: Int, val title: String, val subtitle: String) {
    Welcome(1, "Welcome", "Language & overview"),
    Pairing(2, "GLZ Hub Pairing", "Provision this device"),
    Download(3, "Confirm & Sync", "Download lineup & guides")
}

/**
 * Android TV / Google TV Material 3 First-Run Setup Wizard.
 * Left column: branding + 3-step vertical setup rail.
 * Right pane: interactive Welcome -> GlzHub Pairing (QR + Code + Auto-Poll) -> Initial Download & Confirmation.
 */
@Composable
fun FirstRunSetupScreen(
    appLanguage: String,
    isAlreadyEnrolled: Boolean,
    onLanguageSelected: (String) -> Unit,
    onBeginEnrollment: suspend () -> String,
    onPollEnrollment: suspend () -> Boolean,
    onInitialSync: suspend ((Int, String) -> Unit) -> String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    if (BuildConfig.IS_MOBILE || !viewport.isTvLayout) {
        com.glztv.app.ui.mobile.MobileFirstRunScreen(
            appLanguage = appLanguage,
            isAlreadyEnrolled = isAlreadyEnrolled,
            onLanguageSelected = onLanguageSelected,
            onBeginEnrollment = onBeginEnrollment,
            onPollEnrollment = onPollEnrollment,
            onInitialSync = onInitialSync,
            onComplete = onComplete,
            modifier = modifier
        )
        return
    }
    val scope = rememberCoroutineScope()
    val accent = GoogleTvTokens.BlueAccent

    var step by remember { mutableStateOf(SetupStep.Welcome) }
    var selectedLang by remember(appLanguage) { mutableStateOf(appLanguage) }
    var pairingCode by remember { mutableStateOf<String?>(null) }
    var pairingLoading by remember { mutableStateOf(false) }
    var pairingStatus by remember { mutableStateOf("Generating pairing code…") }
    var pairedConfirmed by remember { mutableStateOf(isAlreadyEnrolled) }

    var syncProgress by remember { mutableStateOf(0) }
    var syncStatus by remember { mutableStateOf("Preparing your GLZ TV experience…") }
    var syncDone by remember { mutableStateOf(false) }

    val primaryFocus = remember { FocusRequester() }

    LaunchedEffect(step, syncDone) {
        delay(120L)
        runCatching { primaryFocus.requestFocus() }
    }

    // Step 2: Request pairing code and auto-poll GlzHub until approved
    LaunchedEffect(step) {
        if (step == SetupStep.Pairing) {
            if (isAlreadyEnrolled) {
                pairedConfirmed = true
                pairingStatus = "Device already paired with GLZ Hub"
                delay(700L)
                step = SetupStep.Download
                return@LaunchedEffect
            }
            if (pairingCode.isNullOrBlank()) {
                pairingLoading = true
                runCatching { onBeginEnrollment() }
                    .onSuccess { code ->
                        pairingCode = code
                        pairingStatus = "Enter this code at glzhub.glztech.com"
                    }
                    .onFailure { err ->
                        pairingStatus = "Could not reach GLZ Hub: ${err.message ?: "offline"}"
                    }
                pairingLoading = false
            }
            while (step == SetupStep.Pairing && !pairedConfirmed) {
                delay(4_000L)
                val enrolled = runCatching { onPollEnrollment() }.getOrDefault(false)
                if (enrolled) {
                    pairedConfirmed = true
                    pairingStatus = "Paired with GLZ Hub!"
                    delay(800L)
                    step = SetupStep.Download
                }
            }
        }
    }

    // Step 3: Run initial download/sync from GlzHub
    LaunchedEffect(step) {
        if (step == SetupStep.Download && !syncDone) {
            syncProgress = 10
            syncStatus = "Downloading channels, EPG, radio & movies…"
            val result = runCatching {
                onInitialSync { pct, msg ->
                    syncProgress = pct.coerceIn(0, 100)
                    syncStatus = msg
                }
            }.getOrElse { "Ready to watch (offline mode)" }
            syncProgress = 100
            syncStatus = result
            syncDone = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GoogleTvTokens.BackgroundDark)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.14f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = viewport.safeHorizontal, vertical = viewport.safeVertical)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Android TV Setup Branding & 3-Step Indicator
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "GLZ TV Setup",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Let's connect your TV to GLZ Hub and download your live lineup, guide, movies, and radio.",
                        color = GoogleTvTokens.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // Vertical Step Progress Rail
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SetupStep.values().forEach { s ->
                        val active = s == step
                        val completed = s.index < step.index || (s == SetupStep.Download && syncDone)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    completed -> Color(0xFF00E676).copy(alpha = 0.2f)
                                    active -> accent
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (completed) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${s.index}",
                                            color = if (active) Color(0xFF001F2A) else GoogleTvTokens.TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Column {
                                Text(
                                    text = s.title,
                                    color = if (active || completed) Color.White else GoogleTvTokens.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = s.subtitle,
                                    color = GoogleTvTokens.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "GLZ TV v${BuildConfig.VERSION_NAME}",
                    color = GoogleTvTokens.TextSecondary.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right Column: Material 3 Elevated Setup Card
            Surface(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF16181D),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                tonalElevation = 10.dp
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                    label = "setup-step-content"
                ) { activeStep ->
                    when (activeStep) {
                        SetupStep.Welcome -> WelcomeStepPane(
                            selectedLang = selectedLang,
                            primaryFocus = primaryFocus,
                            onSelectLanguage = {
                                selectedLang = it
                                onLanguageSelected(it)
                            },
                            onContinue = { step = SetupStep.Pairing },
                            onSkip = onComplete
                        )

                        SetupStep.Pairing -> PairingStepPane(
                            pairingCode = pairingCode,
                            loading = pairingLoading,
                            statusText = pairingStatus,
                            primaryFocus = primaryFocus,
                            onRefreshCode = {
                                scope.launch {
                                    pairingLoading = true
                                    runCatching { onBeginEnrollment() }
                                        .onSuccess { code ->
                                            pairingCode = code
                                            pairingStatus = "Enter this code at glzhub.glztech.com"
                                        }
                                        .onFailure {
                                            pairingStatus = "Could not generate code. Check network."
                                        }
                                    pairingLoading = false
                                }
                            },
                            onContinueToSync = { step = SetupStep.Download },
                            onSkip = onComplete
                        )

                        SetupStep.Download -> DownloadConfirmPane(
                            progress = syncProgress,
                            statusText = syncStatus,
                            done = syncDone,
                            paired = pairedConfirmed,
                            primaryFocus = primaryFocus,
                            onFinish = onComplete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepPane(
    selectedLang: String,
    primaryFocus: FocusRequester,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val accent = GoogleTvTokens.BlueAccent
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.glztv.app.R.drawable.glztv_logo_stacked),
                    contentDescription = "GlzTV",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.height(64.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Welcome to GlzTV",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Everything you watch and listen to, organized for the big screen.",
                        color = GoogleTvTokens.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SetupFeatureTile(Icons.Default.LiveTv, "Live TV & EPG", "Instant zapping & guide", Modifier.weight(1f))
                SetupFeatureTile(Icons.Default.Radio, "GLZ Radio", "Live audio & visualizer", Modifier.weight(1f))
                SetupFeatureTile(Icons.Default.WbSunny, "Weather", "Local live forecast", Modifier.weight(1f))
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "LANGUAGE / IDIOMA",
                color = GoogleTvTokens.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlzSectionActionButton(
                    label = "English",
                    primary = selectedLang != "es",
                    onClick = { onSelectLanguage("en") },
                    accent = accent
                )
                GlzSectionActionButton(
                    label = "Español",
                    primary = selectedLang == "es",
                    onClick = { onSelectLanguage("es") },
                    accent = accent
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            GlzSectionActionButton(
                label = "Skip Setup",
                onClick = onSkip,
                accent = accent
            )
            GlzSectionActionButton(
                label = "Continue to Pairing",
                primary = true,
                onClick = onContinue,
                accent = accent,
                modifier = Modifier.focusRequester(primaryFocus)
            )
        }
    }
}

@Composable
private fun PairingStepPane(
    pairingCode: String?,
    loading: Boolean,
    statusText: String,
    primaryFocus: FocusRequester,
    onRefreshCode: () -> Unit,
    onContinueToSync: () -> Unit,
    onSkip: () -> Unit
) {
    val accent = GoogleTvTokens.BlueAccent
    val qrBitmap = remember(pairingCode) {
        pairingCode?.takeIf { it.isNotBlank() }?.let { code ->
            generateSetupQrBitmap("https://glzhub.glztech.com/?pair=$code", 240)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                Text(
                    text = "Pair with GLZ Hub",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Open glzhub.glztech.com on your phone or computer, sign in, and enter the pairing code below. This screen advances automatically once approved.",
                color = GoogleTvTokens.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GoogleTvTokens.SurfaceCard,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            modifier = Modifier.size(124.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Pairing QR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PAIRING CODE",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (loading && pairingCode == null) {
                            CircularProgressIndicator(
                                color = accent,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text(
                                text = pairingCode ?: "------",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp
                            )
                        }
                        Text(
                            text = statusText,
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
        ) {
            GlzSectionActionButton(
                label = "Skip",
                onClick = onSkip,
                accent = accent
            )
            GlzSectionActionButton(
                label = "New Code",
                icon = Icons.Default.Refresh,
                onClick = onRefreshCode,
                enabled = !loading,
                accent = accent
            )
            GlzSectionActionButton(
                label = "Continue",
                primary = true,
                onClick = onContinueToSync,
                accent = accent,
                modifier = Modifier.focusRequester(primaryFocus)
            )
        }
    }
}

@Composable
private fun DownloadConfirmPane(
    progress: Int,
    statusText: String,
    done: Boolean,
    paired: Boolean,
    primaryFocus: FocusRequester,
    onFinish: () -> Unit
) {
    val accent = GoogleTvTokens.BlueAccent
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (done) Color(0xFF00E676).copy(alpha = 0.16f) else accent.copy(alpha = 0.16f),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (done) Color(0xFF00E676) else accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Text(
                text = if (done) "You're All Set!" else "Setting Up GLZ TV…",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (done) {
                    if (paired) "Paired with GLZ Hub and your initial lineup is ready."
                    else "Your GLZ TV environment is ready. You can pair with GLZ Hub anytime in Settings."
                } else {
                    statusText
                },
                color = GoogleTvTokens.TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (done) Color(0xFF00E676) else accent,
                trackColor = Color.White.copy(alpha = 0.12f)
            )

            Text(
                text = "$progress% · $statusText",
                color = GoogleTvTokens.TextSecondary,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            GlzSectionActionButton(
                label = if (done) "Start Watching" else "Please wait…",
                primary = true,
                enabled = done,
                onClick = onFinish,
                accent = accent,
                modifier = Modifier.focusRequester(primaryFocus)
            )
        }
    }
}

@Composable
private fun SetupFeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = GoogleTvTokens.SurfaceCard,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoogleTvTokens.BlueAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = GoogleTvTokens.TextSecondary, fontSize = 11.sp)
        }
    }
}

private fun generateSetupQrBitmap(content: String, sizePx: Int): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    bitmap
}.getOrNull()

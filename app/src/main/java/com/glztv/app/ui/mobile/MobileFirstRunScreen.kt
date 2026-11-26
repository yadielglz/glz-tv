package com.glztv.app.ui.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.glztv.app.BuildConfig
import com.glztv.app.R
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MobileSetupStep(val number: Int) {
    Welcome(1),
    Activate(2),
    Sync(3)
}

/**
 * Portrait-first Mobile Onboarding & GLZ Hub Activation flow (v1.48.5.35M).
 *
 * Designed specifically for touch phones in portrait orientation:
 * - Single-column scrollable body with pinned full-width bottom actions (never cramped)
 * - Step 1: Welcome & English/Español language selection
 * - Step 2: Large uncramped Activation Code with one-tap "Open GLZ Hub to Activate",
 *   one-tap "Copy Code", instant "Check Activation" + automatic resume polling, and
 *   optional expandable QR code for pairing from another device
 * - Step 3: Live lineup/EPG/movie/radio download progress & Start Watching CTA
 */
@Composable
fun MobileFirstRunScreen(
    appLanguage: String,
    isAlreadyEnrolled: Boolean,
    onLanguageSelected: (String) -> Unit,
    onBeginEnrollment: suspend () -> String,
    onPollEnrollment: suspend () -> Boolean,
    onInitialSync: suspend ((Int, String) -> Unit) -> String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(MobileSetupStep.Welcome) }
    var selectedLang by remember(appLanguage) { mutableStateOf(appLanguage) }
    val isEs = selectedLang.equals("es", ignoreCase = true)

    var pairingCode by remember { mutableStateOf<String?>(null) }
    var pairingLoading by remember { mutableStateOf(false) }
    var pairingChecking by remember { mutableStateOf(false) }
    var pairingError by remember { mutableStateOf<String?>(null) }
    var pairingStatus by remember {
        mutableStateOf(if (isEs) "Solicitando código de activación…" else "Requesting activation code…")
    }
    var pairedConfirmed by remember { mutableStateOf(isAlreadyEnrolled) }
    var copiedFeedback by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }

    var syncProgress by remember { mutableStateOf(0) }
    var syncStatus by remember {
        mutableStateOf(if (isEs) "Preparando tu experiencia GLZ TV…" else "Preparing your GLZ TV experience…")
    }
    var syncDone by remember { mutableStateOf(false) }

    suspend fun requestNewCode() {
        pairingLoading = true
        pairingError = null
        pairingStatus = if (isEs) "Generando código de activación…" else "Generating activation code…"
        runCatching { onBeginEnrollment() }
            .onSuccess { code ->
                pairingCode = code
                pairingError = null
                pairingStatus = if (isEs) {
                    "Esperando activación en glzhub.glztech.com…"
                } else {
                    "Waiting for activation on glzhub.glztech.com…"
                }
            }
            .onFailure { err ->
                pairingError = err.message?.takeIf { it.isNotBlank() }
                    ?: if (isEs) "No se pudo conectar con GLZ Hub" else "Could not reach GLZ Hub"
                pairingStatus = pairingError!!
            }
        pairingLoading = false
    }

    suspend fun checkActivationNow(userInitiated: Boolean = false) {
        if (pairedConfirmed || pairingChecking) return
        pairingChecking = true
        val enrolled = runCatching { onPollEnrollment() }.getOrDefault(false)
        pairingChecking = false
        if (enrolled) {
            pairedConfirmed = true
            pairingError = null
            pairingStatus = if (isEs) "¡Dispositivo activado! Sincronizando…" else "Device activated! Syncing your lineup…"
            delay(750L)
            step = MobileSetupStep.Sync
        } else if (userInitiated) {
            pairingStatus = if (isEs) {
                "Aún pendiente — aprueba el código en GLZ Hub"
            } else {
                "Still waiting — approve the code on GLZ Hub"
            }
        }
    }

    // Automatically check activation immediately when returning from the browser
    DisposableEffect(lifecycleOwner, step, pairedConfirmed) {
        val owner = lifecycleOwner
        if (owner == null) {
            onDispose {}
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && step == MobileSetupStep.Activate && !pairedConfirmed && !pairingCode.isNullOrBlank()) {
                    scope.launch { checkActivationNow(userInitiated = false) }
                }
            }
            owner.lifecycle.addObserver(observer)
            onDispose { owner.lifecycle.removeObserver(observer) }
        }
    }

    // Auto-clear "Copied!" toast state
    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            delay(2_200L)
            copiedFeedback = false
        }
    }

    // Step 2: Request pairing code and auto-poll every 3.5 seconds until activated
    LaunchedEffect(step) {
        if (step == MobileSetupStep.Activate) {
            if (isAlreadyEnrolled) {
                pairedConfirmed = true
                pairingStatus = if (isEs) "Dispositivo ya vinculado con GLZ Hub" else "Device already linked with GLZ Hub"
                delay(650L)
                step = MobileSetupStep.Sync
                return@LaunchedEffect
            }
            if (pairingCode.isNullOrBlank()) {
                requestNewCode()
            }
            while (step == MobileSetupStep.Activate && !pairedConfirmed) {
                delay(3_500L)
                checkActivationNow(userInitiated = false)
            }
        }
    }

    // Step 3: Run initial lineup & EPG sync
    LaunchedEffect(step) {
        if (step == MobileSetupStep.Sync && !syncDone) {
            syncProgress = 12
            syncStatus = if (isEs) {
                "Descargando canales, guía, radio y películas…"
            } else {
                "Downloading channels, guide, radio & movies…"
            }
            val result = runCatching {
                onInitialSync { pct, msg ->
                    syncProgress = pct.coerceIn(0, 100)
                    syncStatus = msg
                }
            }.getOrElse {
                if (isEs) "Listo para ver (modo sin conexión)" else "Ready to watch (offline mode)"
            }
            syncProgress = 100
            syncStatus = result
            syncDone = true
        }
    }

    BackHandler {
        when (step) {
            MobileSetupStep.Activate -> step = MobileSetupStep.Welcome
            MobileSetupStep.Sync -> if (syncDone) onComplete()
            MobileSetupStep.Welcome -> onComplete()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12))
            .background(
                Brush.verticalGradient(
                    listOf(
                        DtvChrome.ElectricBlue.copy(alpha = 0.14f),
                        Color(0xFF0A0D12),
                        Color(0xFF07090D)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Mobile Top Bar with Branding & Step Counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (step == MobileSetupStep.Activate) {
                IconButton(
                    onClick = { step = MobileSetupStep.Welcome },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isEs) "Atrás" else "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Image(
                painter = painterResource(id = R.drawable.glztv_logo_horizontal),
                contentDescription = "GLZ TV",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(22.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(DtvChrome.ElectricBlue.copy(alpha = 0.22f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "MOBILE",
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 9.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (isEs) "Paso ${step.number} de 3" else "Step ${step.number} of 3",
                color = DtvChrome.SecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3-Segment Horizontal Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MobileSetupStep.entries.forEach { s ->
                val filled = s.number <= step.number
                val completed = s.number < step.number || (s == MobileSetupStep.Sync && syncDone)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            when {
                                completed -> Color(0xFF22C55E)
                                filled -> DtvChrome.ElectricBlue
                                else -> Color.White.copy(alpha = 0.14f)
                            }
                        )
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Main Animated Step Area
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(140)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "mobile-setup-step"
        ) { activeStep ->
            when (activeStep) {
                MobileSetupStep.Welcome -> MobileWelcomeStep(
                    selectedLang = selectedLang,
                    isEs = isEs,
                    onSelectLanguage = { lang ->
                        selectedLang = lang
                        onLanguageSelected(lang)
                    },
                    onContinue = { step = MobileSetupStep.Activate },
                    onSkip = onComplete
                )

                MobileSetupStep.Activate -> MobileActivateStep(
                    isEs = isEs,
                    pairingCode = pairingCode,
                    loading = pairingLoading,
                    checking = pairingChecking,
                    paired = pairedConfirmed,
                    errorText = pairingError,
                    statusText = pairingStatus,
                    copiedFeedback = copiedFeedback,
                    showQrCode = showQrCode,
                    onToggleQrCode = { showQrCode = !showQrCode },
                    onOpenHubBrowser = { code ->
                        val url = "https://glzhub.glztech.com/?pair=$code"
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            pairingStatus = if (isEs) {
                                "Abre glzhub.glztech.com en tu navegador e ingresa $code"
                            } else {
                                "Open glzhub.glztech.com in your browser and enter $code"
                            }
                        }
                    },
                    onCopyCode = { code ->
                        runCatching {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("GLZ Hub Pairing Code", code))
                            copiedFeedback = true
                        }
                    },
                    onCheckNow = {
                        scope.launch { checkActivationNow(userInitiated = true) }
                    },
                    onRefreshCode = {
                        scope.launch { requestNewCode() }
                    },
                    onContinueToSync = { step = MobileSetupStep.Sync },
                    onSkip = onComplete
                )

                MobileSetupStep.Sync -> MobileSyncStep(
                    isEs = isEs,
                    progress = syncProgress,
                    statusText = syncStatus,
                    done = syncDone,
                    paired = pairedConfirmed,
                    onFinish = onComplete
                )
            }
        }
    }
}

@Composable
private fun MobileWelcomeStep(
    selectedLang: String,
    isEs: Boolean,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Image(
                painter = painterResource(id = R.drawable.glztv_logo_stacked),
                contentDescription = "GLZ TV Mobile",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(82.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isEs) "Bienvenido a GlzTV Mobile" else "Welcome to GlzTV Mobile",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isEs) {
                        "TV en vivo, películas y GLZ Radio en tu teléfono, sincronizado con tu cuenta de GLZ Hub."
                    } else {
                        "Live TV, movies, and GLZ Radio on your phone — automatically synced with GLZ Hub."
                    },
                    color = DtvChrome.SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Language Selector (touch-friendly 2-column cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LANGUAGE / IDIOMA",
                    color = DtvChrome.SecondaryText,
                    fontSize = 11.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MobileLanguageCard(
                        label = "English",
                        selected = !selectedLang.equals("es", ignoreCase = true),
                        onClick = { onSelectLanguage("en") },
                        modifier = Modifier.weight(1f)
                    )
                    MobileLanguageCard(
                        label = "Español",
                        selected = selectedLang.equals("es", ignoreCase = true),
                        onClick = { onSelectLanguage("es") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mobile Feature Highlights (vertical stack — zero cramping)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MobileFeatureItem(
                    icon = Icons.Default.LiveTv,
                    title = if (isEs) "TV en Vivo y Guía" else "Live TV & Program Guide",
                    subtitle = if (isEs) "Búsqueda rápida, favoritos y controles táctiles"
                    else "Instant search, favorites, and touch player controls"
                )
                MobileFeatureItem(
                    icon = Icons.Default.Movie,
                    title = if (isEs) "Películas y GLZ Radio" else "Movies & GLZ Radio",
                    subtitle = if (isEs) "Catálogo bajo demanda y radio en segundo plano"
                    else "On-demand movies plus live radio mini-player"
                )
                MobileFeatureItem(
                    icon = Icons.Default.Sync,
                    title = if (isEs) "Activación Rápida GLZ Hub" else "Quick GLZ Hub Activation",
                    subtitle = if (isEs) "Activa directamente desde el navegador de este teléfono"
                    else "Activate right from this phone's browser in seconds"
                )
            }

            Text(
                text = "GLZ TV Mobile · v${BuildConfig.VERSION_NAME}",
                color = DtvChrome.MutedText,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Pinned Bottom Actions
        HorizontalDivider(color = DtvChrome.Divider)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1118))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MobilePrimaryButton(
                label = if (isEs) "Continuar a Activación" else "Continue to Activation",
                onClick = onContinue
            )
            MobileTextButton(
                label = if (isEs) "Omitir configuración por ahora" else "Skip Setup for Now",
                onClick = onSkip
            )
        }
    }
}

@Composable
private fun MobileActivateStep(
    isEs: Boolean,
    pairingCode: String?,
    loading: Boolean,
    checking: Boolean,
    paired: Boolean,
    errorText: String?,
    statusText: String,
    copiedFeedback: Boolean,
    showQrCode: Boolean,
    onToggleQrCode: () -> Unit,
    onOpenHubBrowser: (String) -> Unit,
    onCopyCode: (String) -> Unit,
    onCheckNow: () -> Unit,
    onRefreshCode: () -> Unit,
    onContinueToSync: () -> Unit,
    onSkip: () -> Unit
) {
    val qrBitmap = remember(pairingCode, showQrCode) {
        if (!showQrCode) null
        else pairingCode?.takeIf { it.isNotBlank() }?.let { code ->
            generateMobileQrBitmap("https://glzhub.glztech.com/?pair=$code", 320)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isEs) "Activar este Teléfono" else "Activate This Device",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEs) {
                        "Toca el botón azul para abrir GLZ Hub e ingresar este código, o cópialo para activarlo desde otro dispositivo."
                    } else {
                        "Tap below to open GLZ Hub and approve this code right from your phone, or copy it to activate from another device."
                    },
                    color = DtvChrome.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            // Large Uncramped Activation Code Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF121824),
                border = BorderStroke(
                    1.5.dp,
                    when {
                        paired -> Color(0xFF22C55E).copy(alpha = 0.7f)
                        errorText != null -> Color(0xFFF87171).copy(alpha = 0.6f)
                        else -> DtvChrome.ElectricBlue.copy(alpha = 0.55f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isEs) "CÓDIGO DE ACTIVACIÓN" else "ACTIVATION CODE",
                        color = if (paired) Color(0xFF4ADE80) else DtvChrome.ElectricBlueSoft,
                        fontSize = 11.sp,
                        fontFamily = BarlowCondensedFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )

                    if (loading && pairingCode == null) {
                        CircularProgressIndicator(
                            color = DtvChrome.ElectricBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .size(32.dp)
                        )
                    } else {
                        Text(
                            text = pairingCode ?: "------",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontFamily = BarlowCondensedFontFamily,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Live Activation Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                when {
                                    paired -> Color(0xFF22C55E).copy(alpha = 0.16f)
                                    errorText != null -> Color(0xFFEF4444).copy(alpha = 0.16f)
                                    else -> Color.White.copy(alpha = 0.06f)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when {
                            paired -> Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(16.dp)
                            )
                            errorText == null && pairingCode != null -> CircularProgressIndicator(
                                color = DtvChrome.ElectricBlueSoft,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = statusText,
                            color = when {
                                paired -> Color(0xFF86EFAC)
                                errorText != null -> Color(0xFFFCA5A5)
                                else -> DtvChrome.SecondaryText
                            },
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // One-Tap Mobile Browser Activation Button
            MobilePrimaryButton(
                label = if (isEs) "Abrir GLZ Hub para Activar" else "Open GLZ Hub to Activate",
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                enabled = !pairingCode.isNullOrBlank() && !paired,
                onClick = { pairingCode?.let(onOpenHubBrowser) }
            )

            // Copy Code + Check Status (2 clean equal-width touch buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MobileSecondaryButton(
                    label = when {
                        copiedFeedback -> if (isEs) "¡Copiado!" else "Copied!"
                        else -> if (isEs) "Copiar Código" else "Copy Code"
                    },
                    icon = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                    highlighted = copiedFeedback,
                    enabled = !pairingCode.isNullOrBlank(),
                    onClick = { pairingCode?.let(onCopyCode) },
                    modifier = Modifier.weight(1f)
                )
                MobileSecondaryButton(
                    label = if (checking) {
                        if (isEs) "Verificando…" else "Checking…"
                    } else {
                        if (isEs) "Verificar" else "Check Status"
                    },
                    icon = Icons.Default.Refresh,
                    enabled = !pairingCode.isNullOrBlank() && !checking && !paired,
                    onClick = onCheckNow,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick numbered instructions so users know exact steps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10151F))
                    .border(1.dp, DtvChrome.Divider, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isEs) "CÓMO ACTIVAR" else "HOW TO ACTIVATE",
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 11.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
                Text(
                    text = if (isEs) {
                        "1. Toca \"Abrir GLZ Hub\" e inicia sesión.\n2. Ve a Vincular TV e ingresa el código de arriba.\n3. Vuelve aquí — esta pantalla avanza sola."
                    } else {
                        "1. Tap \"Open GLZ Hub\" and sign in.\n2. Open Pair a TV and enter the code above.\n3. Return here — setup advances automatically."
                    },
                    color = DtvChrome.SecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            // Optional Collapsible QR Code (for activating from another phone/computer)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10151F))
                    .border(1.dp, DtvChrome.Divider, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleQrCode() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = DtvChrome.ElectricBlueSoft,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEs) "Activar desde otro dispositivo (QR)" else "Activate from another device (QR)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showQrCode) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = DtvChrome.SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(visible = showQrCode && qrBitmap != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.size(168.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "GLZ Hub Pairing QR",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                )
                            }
                        }
                        Text(
                            text = "glzhub.glztech.com",
                            color = DtvChrome.SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Generate New Code link
            MobileTextButton(
                label = if (loading) {
                    if (isEs) "Generando nuevo código…" else "Generating new code…"
                } else {
                    if (isEs) "Generar Nuevo Código" else "Generate New Code"
                },
                enabled = !loading && !paired,
                onClick = onRefreshCode
            )
        }

        // Pinned Bottom Actions
        HorizontalDivider(color = DtvChrome.Divider)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1118))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (paired) {
                MobilePrimaryButton(
                    label = if (isEs) "Continuar y Sincronizar" else "Continue & Sync",
                    accentColor = Color(0xFF16A34A),
                    onClick = onContinueToSync
                )
            } else {
                MobileSecondaryButton(
                    label = if (isEs) "Continuar sin Activar" else "Continue Without Activating",
                    onClick = onContinueToSync,
                    modifier = Modifier.fillMaxWidth()
                )
                MobileTextButton(
                    label = if (isEs) "Omitir configuración" else "Skip Setup",
                    onClick = onSkip
                )
            }
        }
    }
}

@Composable
private fun MobileSyncStep(
    isEs: Boolean,
    progress: Int,
    statusText: String,
    done: Boolean,
    paired: Boolean,
    onFinish: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (done) Color(0xFF22C55E).copy(alpha = 0.16f) else DtvChrome.ElectricBlue.copy(alpha = 0.16f),
                border = BorderStroke(
                    1.dp,
                    if (done) Color(0xFF22C55E).copy(alpha = 0.45f) else DtvChrome.ElectricBlue.copy(alpha = 0.45f)
                ),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (done) Color(0xFF4ADE80) else DtvChrome.ElectricBlueSoft,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = when {
                    done && isEs -> "¡Todo Listo!"
                    done -> "You're All Set!"
                    isEs -> "Sincronizando GLZ TV…"
                    else -> "Setting Up GLZ TV…"
                },
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (done) {
                    when {
                        paired && isEs -> "Activado con GLZ Hub y tu programación está lista."
                        paired -> "Activated with GLZ Hub and your live lineup is ready."
                        isEs -> "GLZ TV Mobile está listo. Puedes activar GLZ Hub cuando quieras en Configuración."
                        else -> "GLZ TV Mobile is ready. You can activate with GLZ Hub anytime in Settings."
                    }
                } else {
                    statusText
                },
                color = DtvChrome.SecondaryText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (done) Color(0xFF22C55E) else DtvChrome.ElectricBlue,
                trackColor = Color.White.copy(alpha = 0.12f)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "$progress% · $statusText",
                color = DtvChrome.MutedText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        HorizontalDivider(color = DtvChrome.Divider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1118))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            MobilePrimaryButton(
                label = when {
                    done && isEs -> "Comenzar a Ver"
                    done -> "Start Watching"
                    isEs -> "Preparando…"
                    else -> "Please wait…"
                },
                enabled = done,
                accentColor = if (done) DtvChrome.ElectricBlue else Color(0xFF1E2633),
                onClick = onFinish
            )
        }
    }
}

@Composable
private fun MobileLanguageCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) DtvChrome.ElectricBlue.copy(alpha = 0.2f) else Color(0xFF131924))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) DtvChrome.ElectricBlue else DtvChrome.Divider,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = DtvChrome.ElectricBlueSoft,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (selected) Color.White else DtvChrome.SecondaryText,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun MobileFeatureItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121722))
            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DtvChrome.ElectricBlue.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DtvChrome.ElectricBlueSoft,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = DtvChrome.SecondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun MobilePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color = DtvChrome.ElectricBlue
) {
    val bg = if (enabled) accentColor else Color(0xFF1D2430)
    val fg = if (enabled) Color.White else DtvChrome.MutedText
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MobileSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlighted: Boolean = false,
    enabled: Boolean = true
) {
    val borderColor = when {
        highlighted -> Color(0xFF22C55E)
        enabled -> DtvChrome.ElectricBlue.copy(alpha = 0.5f)
        else -> DtvChrome.Divider
    }
    val bg = when {
        highlighted -> Color(0xFF22C55E).copy(alpha = 0.16f)
        else -> Color(0xFF151C28)
    }
    val fg = when {
        highlighted -> Color(0xFF86EFAC)
        enabled -> Color.White
        else -> DtvChrome.MutedText
    }
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MobileTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) DtvChrome.SecondaryText else DtvChrome.MutedText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun generateMobileQrBitmap(content: String, sizePx: Int): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    bitmap
}.getOrNull()

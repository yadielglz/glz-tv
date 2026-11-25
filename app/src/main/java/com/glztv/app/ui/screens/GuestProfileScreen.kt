package com.glztv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.GuestExperience
import com.glztv.app.GuestService
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvActionButton
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.LocalTvViewport
import kotlinx.coroutines.delay

/**
 * Non-Scrollable DirecTV HD Guest / Profile Screen (v1.48.4.25).
 *
 * - Flush DirecTV top header bar with Back/Close action.
 * - Broadcast Hero Banner with optional property background bleed, guest greeting,
 *   and inline borderless stay metadata (Room, Arrival, Departure, Checkout).
 * - Strong DirecTV divider separating the 2-column non-scrollable dashboard:
 *   - Left Column (46%): Wi-Fi & Connectivity + Property Notice / Front Desk.
 *   - Right Column (54%): Guest Services & Visit Information.
 */
@Composable
fun GuestProfileScreen(
    guestName: String,
    experience: GuestExperience,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(28.dp)
    val closeFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { closeFocus.requestFocus() }
    }

    val services = remember(experience) {
        buildList {
            experience.frontDesk?.takeIf { it.isNotBlank() }?.let {
                add(GuestService("Front Desk", it, null))
            }
            addAll(experience.services)
        }
    }

    val displayGuest = guestName.ifBlank { "Guest" }
    val propertyTitle = experience.propertyName.ifBlank { "GlzTV Hospitality" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        // ── 1. Flush DirecTV Top Bar ─────────────────────────────────────────────
        val barHeight = if (viewport.isCompactHeight) 44.dp else 48.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(DtvChrome.TopBarGradient)
                .padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(DtvChrome.ElectricBlue))
                Text(
                    text = "GlzTV",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Box(Modifier.width(1.dp).height(18.dp).background(DtvChrome.Divider))
                Text(
                    text = "GUEST PROFILE",
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 13.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
            }
            DtvActionButton(
                label = "Close",
                primary = false,
                modifier = Modifier.focusRequester(closeFocus),
                onClick = onClose
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.Divider))

        // ── 2. DirecTV Broadcast Guest Hero Banner ───────────────────────────────
        val heroHeight = if (viewport.isCompactHeight) 156.dp else 178.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clipToBounds()
                .background(DtvChrome.BannerGradient)
        ) {
            val heroImg = experience.heroImageUrl?.takeIf { it.isNotBlank() }
            if (heroImg != null) {
                AsyncImage(
                    model = heroImg,
                    contentDescription = propertyTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xE80A0D12),
                                    0.55f to Color(0xCC0C1017),
                                    1.00f to Color(0xE60A0D12)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left: Guest Greeting & Property Identity
                Row(
                    modifier = Modifier.weight(1.1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val logoUrl = experience.logoUrl?.takeIf { it.isNotBlank() }
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC131822))
                            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoUrl != null) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = propertyTitle,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(6.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DtvChrome.ElectricBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = propertyTitle.uppercase(),
                            color = DtvChrome.ElectricBlueSoft,
                            fontSize = 11.sp,
                            fontFamily = BarlowCondensedFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Welcome, $displayGuest",
                            color = Color.White,
                            fontSize = if (viewport.isCompactHeight) 24.sp else 28.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = experience.welcomeMessage,
                            color = DtvChrome.SecondaryText,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Borderless Inline Stay Telemetry
                val stayMeta = buildList {
                    experience.roomNumber?.takeIf { it.isNotBlank() }?.let { add("ROOM" to it) }
                    experience.arrivalDate?.takeIf { it.isNotBlank() }?.let { add("CHECK-IN" to it) }
                    experience.departureDate?.takeIf { it.isNotBlank() }?.let { add("CHECK-OUT" to it) }
                    experience.checkoutTime?.takeIf { it.isNotBlank() }?.let { add("CHECKOUT TIME" to it) }
                }
                if (stayMeta.isNotEmpty()) {
                    Row(
                        modifier = Modifier.weight(0.9f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        stayMeta.forEachIndexed { idx, (label, value) ->
                            if (idx > 0) {
                                Box(
                                    Modifier
                                        .padding(horizontal = 14.dp)
                                        .width(1.dp)
                                        .height(34.dp)
                                        .background(DtvChrome.Divider.copy(alpha = 0.7f))
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = DtvChrome.MutedText,
                                    fontSize = 10.sp,
                                    fontFamily = BarlowCondensedFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = value,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Strong DirecTV Divider
        Box(Modifier.fillMaxWidth().height(2.dp).background(DtvChrome.Divider))
        Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.ElectricBlue.copy(alpha = 0.35f)))

        // ── 3. Non-Scrollable 2-Column Guest Information Dashboard ───────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = horizontalPad, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT COLUMN (46%): Wi-Fi Access + Property Notice
            Column(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wi-Fi Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(DtvChrome.CardGradient)
                        .border(1.dp, DtvChrome.CardBorder, RoundedCornerShape(3.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = DtvChrome.ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "WI-FI & CONNECTIVITY",
                            color = DtvChrome.PrimaryText,
                            fontSize = 12.sp,
                            fontFamily = BarlowCondensedFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.RowDivider))
                    Text(
                        text = experience.wifiName?.takeIf { it.isNotBlank() } ?: "Property Wi-Fi",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = experience.wifiInstructions?.takeIf { it.isNotBlank() }
                            ?: "Connect to the property network or ask your host for access details.",
                        color = DtvChrome.SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Property Notice Card (fills remaining left column cleanly)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF101318))
                        .border(1.dp, DtvChrome.Divider, RoundedCornerShape(3.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = DtvChrome.ElectricBlueSoft,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = (experience.noticeTitle?.takeIf { it.isNotBlank() } ?: "STAY NOTICE").uppercase(),
                            color = DtvChrome.PrimaryText,
                            fontSize = 12.sp,
                            fontFamily = BarlowCondensedFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.RowDivider))
                    Text(
                        text = experience.noticeBody?.takeIf { it.isNotBlank() }
                            ?: "Press BACK on your remote at any time to return to GlzTV Live TV, Guide, Movies, Radio, or Apps.",
                        color = DtvChrome.SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Crisp Vertical DirecTV Divider
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(DtvChrome.Divider)
            )

            // RIGHT COLUMN (54%): Visit Info & Guest Services
            Column(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.width(3.dp).height(12.dp).background(DtvChrome.ElectricBlue))
                    Text(
                        text = "VISIT INFORMATION & SERVICES",
                        color = DtvChrome.PrimaryText,
                        fontSize = 12.sp,
                        fontFamily = BarlowCondensedFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp
                    )
                }

                if (services.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF101318))
                            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(3.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Everything is ready for your stay",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Property services, concierge contacts, and local recommendations configured in GlzHub will appear here.",
                                color = DtvChrome.SecondaryText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF101318))
                            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(3.dp))
                    ) {
                        val visibleServices = services.take(5)
                        visibleServices.forEachIndexed { idx, svc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = svc.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    svc.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                                        Text(
                                            text = sub,
                                            color = DtvChrome.SecondaryText,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                svc.actionUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = url.removePrefix("https://").removePrefix("http://"),
                                        color = DtvChrome.ElectricBlueSoft,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (idx < visibleServices.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.RowDivider))
                            }
                        }
                    }
                }
            }
        }
    }
}

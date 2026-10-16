package com.glztv.app.launcher

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LauncherAppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val banner: Drawable?,
    val icon: Drawable?,
    val accent: Color,
    val isFavorite: Boolean,
    val isHidden: Boolean,
    val isSystemApp: Boolean
)

object LauncherAppsManager {
    private const val PREFS_LAUNCHER = "glz_launcher_prefs"
    private const val PREF_FAVORITES = "launcher_favorite_apps"
    private const val PREF_HIDDEN = "launcher_hidden_apps"
    private const val PREF_LAUNCHER_ENABLED = "launcher_alias_enabled"

    private val PALETTE = listOf(
        Color(0xFF2563EB), // Blue
        Color(0xFF7C3AED), // Purple
        Color(0xFFDB2777), // Pink
        Color(0xFFDC2626), // Red
        Color(0xFFEA580C), // Orange
        Color(0xFFD97706), // Amber
        Color(0xFF059669), // Emerald
        Color(0xFF0D9488), // Teal
        Color(0xFF0284C7)  // Sky
    )

    fun getFavoritePackages(prefs: SharedPreferences): Set<String> {
        return prefs.getStringSet(PREF_FAVORITES, emptySet()) ?: emptySet()
    }

    fun setFavorite(prefs: SharedPreferences, packageName: String, isFavorite: Boolean) {
        val current = prefs.getStringSet(PREF_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (isFavorite) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(PREF_FAVORITES, current).apply()
    }

    fun getHiddenPackages(prefs: SharedPreferences): Set<String> {
        return prefs.getStringSet(PREF_HIDDEN, emptySet()) ?: emptySet()
    }

    fun setHidden(prefs: SharedPreferences, packageName: String, isHidden: Boolean) {
        val current = prefs.getStringSet(PREF_HIDDEN, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (isHidden) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(PREF_HIDDEN, current).apply()
    }

    suspend fun loadInstalledApps(
        context: Context,
        prefs: SharedPreferences
    ): List<LauncherAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName
        val favorites = getFavoritePackages(prefs)
        val hidden = getHiddenPackages(prefs)

        val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val standardIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val flags = PackageManager.MATCH_ALL

        val leanbackResolves = runCatching { pm.queryIntentActivities(leanbackIntent, flags) }.getOrElse {
            runCatching { pm.queryIntentActivities(leanbackIntent, 0) }.getOrElse { emptyList() }
        }

        val standardResolves = runCatching { pm.queryIntentActivities(standardIntent, flags) }.getOrElse {
            runCatching { pm.queryIntentActivities(standardIntent, 0) }.getOrElse { emptyList() }
        }

        val seenPackages = mutableSetOf<String>()
        val result = mutableListOf<LauncherAppItem>()

        fun processResolve(resolve: ResolveInfo) {
            val actInfo = resolve.activityInfo ?: return
            val pkg = actInfo.packageName ?: return
            if (pkg == selfPackage) return
            if (seenPackages.contains(pkg)) return
            seenPackages.add(pkg)

            val label = runCatching { resolve.loadLabel(pm).toString() }.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: runCatching { actInfo.applicationInfo.loadLabel(pm).toString() }.getOrNull()
                ?: pkg

            val banner: Drawable? = runCatching { actInfo.loadBanner(pm) }.getOrNull()
                ?: runCatching { actInfo.applicationInfo?.loadBanner(pm) }.getOrNull()
                ?: runCatching { pm.getApplicationBanner(pkg) }.getOrNull()
                ?: runCatching { actInfo.applicationInfo?.let { pm.getApplicationBanner(it) } }.getOrNull()

            val icon: Drawable? = runCatching { resolve.loadIcon(pm) }.getOrNull()
                ?: runCatching { actInfo.loadIcon(pm) }.getOrNull()
                ?: runCatching { actInfo.applicationInfo?.loadIcon(pm) }.getOrNull()
                ?: runCatching { pm.getApplicationIcon(pkg) }.getOrNull()

            val isSystem = (actInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val accent = PALETTE[Math.abs(pkg.hashCode()) % PALETTE.size]

            result.add(
                LauncherAppItem(
                    packageName = pkg,
                    activityName = actInfo.name ?: "",
                    label = label,
                    banner = banner,
                    icon = icon,
                    accent = accent,
                    isFavorite = pkg in favorites,
                    isHidden = pkg in hidden,
                    isSystemApp = isSystem
                )
            )
        }

        // Add Leanback apps first (preferred TV apps)
        for (res in leanbackResolves) {
            processResolve(res)
        }

        // Add standard apps next (sideloaded/mobile apps)
        for (res in standardResolves) {
            processResolve(res)
        }

        // Sort: favorites first, then alphabetically by label
        result.sortedWith(
            compareByDescending<LauncherAppItem> { it.isFavorite }
                .thenBy { it.label.lowercase() }
        )
    }

    fun registerAppListener(
        context: Context,
        onAppsChanged: () -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                onAppsChanged()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        return receiver
    }

    fun findAppLaunchIntent(context: Context, packageName: String): Intent? {
        val pm = context.packageManager
        // 1. Direct Leanback Intent (authentic Android TV launcher)
        pm.getLeanbackLaunchIntentForPackage(packageName)?.let { return it }
        // 2. Standard Mobile Launch Intent (sideloaded / standard apps)
        pm.getLaunchIntentForPackage(packageName)?.let { return it }

        // 3. Known dual TV/Mobile package aliases (e.g. YouTube TV vs YouTube Mobile)
        val alternatePackages = when (packageName) {
            "com.google.android.youtube.tv" -> listOf("com.google.android.youtube")
            "com.google.android.youtube" -> listOf("com.google.android.youtube.tv")
            "com.netflix.ninja" -> listOf("com.netflix.mediaclient")
            "com.netflix.mediaclient" -> listOf("com.netflix.ninja")
            "com.amazon.amazonvideo.livingroom" -> listOf("com.amazon.avod.thirdpartyclient")
            else -> emptyList()
        }
        for (alt in alternatePackages) {
            pm.getLeanbackLaunchIntentForPackage(alt)?.let { return it }
            pm.getLaunchIntentForPackage(alt)?.let { return it }
        }

        // 4. Fallback explicit intent query
        return runCatching {
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setPackage(packageName)
                .takeIf { it.resolveActivity(pm) != null }
        }.getOrNull()
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val intent = findAppLaunchIntent(context, packageName)
        return try {
            if (intent != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } else {
                // If not installed on device, open in Google Play Store
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                false
            }
        } catch (_: Exception) {
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            false
        }
    }

    fun openAppDetails(context: Context, packageName: String) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:$packageName")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun isHomeLauncherAliasEnabled(context: Context): Boolean {
        val comp = ComponentName(context, "${context.packageName}.HomeLauncherAlias")
        val state = context.packageManager.getComponentEnabledSetting(comp)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun setHomeLauncherAliasEnabled(context: Context, enabled: Boolean) {
        val comp = ComponentName(context, "${context.packageName}.HomeLauncherAlias")
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            comp,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }

    fun openAndroidSettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openWifiSettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            openAndroidSettings(context)
        }
    }

    fun openBluetoothSettings(context: Context) {
        runCatching {
            context.startActivity(Intent("android.settings.BLUETOOTH_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            openAndroidSettings(context)
        }
    }

    fun openDeveloperSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            openAndroidSettings(context)
        }
    }

    fun openDisplaySettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            openAndroidSettings(context)
        }
    }

    fun openHomeSettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure {
                openAndroidSettings(context)
            }
        }
    }
}

/**
 * Safely converts any Android Drawable (AdaptiveIconDrawable, VectorDrawable, BitmapDrawable, etc.)
 * into a Compose ImageBitmap without crashing on negative intrinsic dimensions or unsupported densities.
 */
fun Drawable.toSafeImageBitmap(targetWidth: Int = 160, targetHeight: Int = 160): ImageBitmap? {
    return runCatching {
        val w = if (intrinsicWidth > 0) intrinsicWidth else targetWidth
        val h = if (intrinsicHeight > 0) intrinsicHeight else targetHeight
        val finalW = w.coerceIn(48, 640)
        val finalH = h.coerceIn(48, 360)
        val bitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val oldBounds = bounds
        setBounds(0, 0, finalW, finalH)
        draw(canvas)
        bounds = oldBounds
        bitmap.asImageBitmap()
    }.getOrNull()
}


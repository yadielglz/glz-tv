package com.glztv.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.glztv.app.ui.theme.GlzTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class ManagedInstallActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_SOURCE_URL = "source_url"
        private const val EXTRA_PACKAGE_NAME = "package_name"
        private const val EXTRA_COMMAND_ID = "command_id"

        fun intent(
            context: Context,
            sourceUrl: String,
            packageName: String,
            commandId: String
        ) = Intent(context, ManagedInstallActivity::class.java)
            .putExtra(EXTRA_SOURCE_URL, sourceUrl)
            .putExtra(EXTRA_PACKAGE_NAME, packageName)
            .putExtra(EXTRA_COMMAND_ID, commandId)
    }

    private val client = OkHttpClient.Builder().followRedirects(true).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL).orEmpty()
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        if (!isAllowedUrl(sourceUrl) || packageName.isBlank()) {
            finish()
            return
        }
        setContent {
            GlzTheme("dark") {
                InstallerBrowser(
                    initialUrl = sourceUrl,
                    onClose = ::finish,
                    onDownload = { downloadUrl, status ->
                        downloadAndInstall(downloadUrl, packageName, status)
                    }
                )
            }
        }
    }

    private fun downloadAndInstall(
        url: String,
        packageName: String,
        setStatus: (String, Boolean) -> Unit
    ) {
        if (!isAllowedUrl(url)) {
            setStatus("Only secure HTTPS APK links are allowed.", false)
            return
        }
        if (!GithubUpdateManager.canInstall(this)) {
            setStatus("Allow GLZ TV to install apps, then select the APK again.", false)
            GithubUpdateManager.requestInstallPermission(this)
            return
        }
        setStatus("Downloading APK inside GLZ TV…", true)
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    GithubUpdateManager.downloadApk(
                        this@ManagedInstallActivity,
                        client,
                        url,
                        packageName,
                        requestHeaders = CookieManager.getInstance().getCookie(url)
                            ?.takeIf(String::isNotBlank)
                            ?.let { mapOf("Cookie" to it) }
                            .orEmpty()
                    )
                }
            }.onSuccess { apk ->
                setStatus("Download verified. Opening Android installer…", false)
                GithubUpdateManager.launchInstaller(this@ManagedInstallActivity, apk)
            }.onFailure {
                setStatus("Install download failed: ${it.message}", false)
            }
        }
    }

    private fun isAllowedUrl(url: String): Boolean =
        runCatching { Uri.parse(url).scheme.equals("https", ignoreCase = true) }.getOrDefault(false)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InstallerBrowser(
    initialUrl: String,
    onClose: () -> Unit,
    onDownload: (String, (String, Boolean) -> Unit) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember { mutableStateOf("Managed app installer") }
    var status by remember { mutableStateOf("Browse the repository and select an APK to install.") }
    var downloading by remember { mutableStateOf(false) }
    val setStatus: (String, Boolean) -> Unit = { message, busy ->
        status = message
        downloading = busy
    }

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack() else onClose()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF08111F))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                if (webView?.canGoBack() == true) webView?.goBack() else onClose()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Text(" Back")
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClose) {
                Icon(Icons.Default.Close, null)
                Text(" Close")
            }
        }
        if (downloading) LinearProgressIndicator(Modifier.fillMaxWidth())
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.WHITE)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setSupportMultipleWindows(false)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            status = "Loading repository…"
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            title = view?.title?.takeIf(String::isNotBlank) ?: "Managed app installer"
                            status = "Select an APK to download and install."
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString().orEmpty()
                            return when {
                                URLUtil.isHttpsUrl(url) && url.substringBefore('?').endsWith(".apk", true) -> {
                                    onDownload(url, setStatus)
                                    true
                                }
                                URLUtil.isHttpsUrl(url) -> false
                                else -> true
                            }
                        }
                    }
                    setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                        val isApk = mimeType.equals("application/vnd.android.package-archive", true) ||
                            URLUtil.guessFileName(url, contentDisposition, mimeType).endsWith(".apk", true)
                        if (isApk) onDownload(url, setStatus)
                        else setStatus("Only APK downloads are supported here.", false)
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    loadUrl(initialUrl)
                    webView = this
                }
            },
            update = { webView = it }
        )
    }
}

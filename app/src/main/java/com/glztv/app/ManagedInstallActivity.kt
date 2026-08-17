package com.glztv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.glztv.app.ui.theme.GlzTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.glztv.app.data.createPermissiveOkHttpClient
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

    private val client = createPermissiveOkHttpClient()
    private var status by mutableStateOf("Preparing download…")
    private var progress by mutableStateOf<Float?>(null)
    private var downloading by mutableStateOf(false)
    private var downloadedApk by mutableStateOf<File?>(null)
    private lateinit var sourceUrl: String
    private lateinit var expectedPackageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL).orEmpty()
        expectedPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        if (!isAllowedUrl(sourceUrl) || expectedPackageName.isBlank()) {
            finish()
            return
        }

        setContent {
            GlzTheme("dark") {
                InstallerDownloadScreen(
                    status = status,
                    progress = progress,
                    downloading = downloading,
                    readyToInstall = downloadedApk != null,
                    onInstall = ::openInstaller,
                    onRetry = ::startDownload
                )
            }
        }
        startDownload()
    }

    private fun startDownload() {
        if (downloading) return
        downloadedApk = null
        downloading = true
        progress = null
        status = "Connecting to APK download…"
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    GithubUpdateManager.downloadApk(
                        context = this@ManagedInstallActivity,
                        client = client,
                        downloadUrl = sourceUrl,
                        expectedPackageName = expectedPackageName,
                        onProgress = { bytesRead, totalBytes ->
                            runOnUiThread {
                                progress = if (totalBytes > 0) {
                                    (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                                } else null
                                status = if (totalBytes > 0) {
                                    "Downloading APK… ${(bytesRead * 100 / totalBytes)}%"
                                } else {
                                    "Downloading APK… ${bytesRead / 1_048_576} MB"
                                }
                            }
                        }
                    )
                }
            }.onSuccess { apk ->
                downloadedApk = apk
                downloading = false
                progress = 1f
                status = "Download complete and verified. Select Open / Install."
            }.onFailure { error ->
                downloading = false
                progress = null
                status = "Download failed: ${error.message ?: "Unknown error"}"
            }
        }
    }

    private fun openInstaller() {
        val apk = downloadedApk ?: return
        if (GithubUpdateManager.canInstall(this)) {
            GithubUpdateManager.launchInstaller(this, apk)
        } else {
            status = "Allow GLZ TV to install apps, then select Open / Install again."
            GithubUpdateManager.requestInstallPermission(this)
        }
    }

    private fun isAllowedUrl(url: String): Boolean =
        runCatching { Uri.parse(url).scheme.equals("https", ignoreCase = true) }.getOrDefault(false)
}

@Composable
private fun InstallerDownloadScreen(
    status: String,
    progress: Float?,
    downloading: Boolean,
    readyToInstall: Boolean,
    onInstall: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111F))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = if (readyToInstall) Icons.Default.InstallMobile else Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Managed app installer", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = status,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            when {
                readyToInstall -> Button(onClick = onInstall) { Text("Open / Install") }
                !downloading -> Button(onClick = onRetry) { Text("Retry download") }
            }
        }

        if (downloading) {
            if (progress == null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                )
            }
        }
    }
}

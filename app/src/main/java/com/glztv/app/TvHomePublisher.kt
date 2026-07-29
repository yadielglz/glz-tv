package com.glztv.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat

@SuppressLint("RestrictedApi")
object TvHomePublisher {
    private const val CHANNEL_KEY = "glz-live-now"

    fun publish(context: Context, channels: List<Channel>, guide: EpgGuide) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return

        runCatching {
            val helper = PreviewChannelHelper(context)
            val existing = helper.allChannels.firstOrNull {
                it.internalProviderId == CHANNEL_KEY
            }
            val appUri = Uri.parse("glztv://home")
            val logoUri = Uri.parse(
                "android.resource://${context.packageName}/${R.drawable.ic_launcher}"
            )
            val channelBuilder = if (existing == null) {
                PreviewChannel.Builder()
            } else {
                PreviewChannel.Builder(existing)
            }
            val channel = channelBuilder
                .setInternalProviderId(CHANNEL_KEY)
                .setDisplayName("Live now")
                .setDescription("Live channels and current programmes from Glz TV")
                .setAppLinkIntentUri(appUri)
                .setLogo(logoUri)
                .build()
            val channelId = if (existing == null) {
                helper.publishChannel(channel)
            } else {
                helper.updatePreviewChannel(existing.id, channel)
                existing.id
            }

            if (helper.allChannels.none { it.isBrowsable }) {
                TvContractCompat.requestChannelBrowsable(context, channelId)
            }

            context.contentResolver.delete(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                null,
                null
            )
            val now = System.currentTimeMillis()
            channels.take(18).forEachIndexed { index, item ->
                val programme = guide.forChannel(item).firstOrNull {
                    it.startMillis <= now && it.endMillis > now
                }
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("glztv://channel/${Uri.encode(item.id)}")
                ).setPackage(context.packageName)
                val builder = PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setWeight(index)
                    .setContentId(item.id)
                    .setTitle(programme?.title ?: item.name)
                    .setDescription(programme?.description ?: "Watch ${item.name} live")
                    .setType(TvContractCompat.PreviewPrograms.TYPE_CHANNEL)
                    .setLive(true)
                    .setIntent(intent)
                    .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)
                item.logoUrl.takeIf(String::isNotBlank)?.let {
                    builder.setPosterArtUri(Uri.parse(it))
                }
                programme?.let {
                    builder.setStartTimeUtcMillis(it.startMillis)
                    builder.setEndTimeUtcMillis(it.endMillis)
                }
                helper.publishPreviewProgram(builder.build())
            }
        }
    }
}

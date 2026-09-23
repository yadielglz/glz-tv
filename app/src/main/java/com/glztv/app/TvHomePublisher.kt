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
import androidx.tvprovider.media.tv.WatchNextProgram

@SuppressLint("RestrictedApi")
object TvHomePublisher {
    private const val CHANNEL_KEY_LIVE = "glz-live-now"
    private const val CHANNEL_KEY_FAVORITES = "glz-favorites"
    private const val CHANNEL_KEY_RADIO = "glz-radio"

    fun publish(
        context: Context,
        channels: List<Channel>,
        guide: EpgGuide,
        favorites: Set<String> = emptySet(),
        radioStations: List<RadioStation> = emptyList()
    ) {
        if (!isLeanbackSupported(context)) return

        runCatching {
            val helper = PreviewChannelHelper(context)
            val now = System.currentTimeMillis()

            // 1. Publish "Live now" preview row
            publishRow(
                context = context,
                helper = helper,
                providerKey = CHANNEL_KEY_LIVE,
                displayName = "Live TV",
                description = "Live channels and on-air programmes",
                channels = channels.take(18),
                guide = guide,
                now = now
            )

            // 2. Publish "Favorites" preview row if favorites exist
            val favoriteChannels = channels.filter { it.id in favorites }
            if (favoriteChannels.isNotEmpty()) {
                publishRow(
                    context = context,
                    helper = helper,
                    providerKey = CHANNEL_KEY_FAVORITES,
                    displayName = "Favorite Channels",
                    description = "Your favorite live TV channels",
                    channels = favoriteChannels.take(18),
                    guide = guide,
                    now = now
                )
            }

            // 3. Publish "GLZ Radio" preview row if radio stations are loaded
            if (radioStations.isNotEmpty()) {
                publishRadioRow(
                    context = context,
                    helper = helper,
                    stations = radioStations.take(12)
                )
            }
        }
    }

    private fun publishRow(
        context: Context,
        helper: PreviewChannelHelper,
        providerKey: String,
        displayName: String,
        description: String,
        channels: List<Channel>,
        guide: EpgGuide,
        now: Long
    ) {
        if (channels.isEmpty()) return
        val existing = helper.allChannels.firstOrNull { it.internalProviderId == providerKey }
        val appUri = Uri.parse("glztv://home")
        val logoUri = Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher}")
        val channelBuilder = if (existing == null) PreviewChannel.Builder() else PreviewChannel.Builder(existing)
        val previewChannel = channelBuilder
            .setInternalProviderId(providerKey)
            .setDisplayName(displayName)
            .setDescription(description)
            .setAppLinkIntentUri(appUri)
            .setLogo(logoUri)
            .build()
        val channelId = if (existing == null) {
            helper.publishChannel(previewChannel)
        } else {
            helper.updatePreviewChannel(existing.id, previewChannel)
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

        channels.forEachIndexed { index, item ->
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

            val logo = item.logoUrl.takeIf(String::isNotBlank) ?: guide.logoForChannel(item)
            logo?.takeIf(String::isNotBlank)?.let { builder.setPosterArtUri(Uri.parse(it)) }

            programme?.let {
                builder.setStartTimeUtcMillis(it.startMillis)
                builder.setEndTimeUtcMillis(it.endMillis)
            }
            helper.publishPreviewProgram(builder.build())
        }
    }

    private fun publishRadioRow(
        context: Context,
        helper: PreviewChannelHelper,
        stations: List<RadioStation>
    ) {
        val existing = helper.allChannels.firstOrNull { it.internalProviderId == CHANNEL_KEY_RADIO }
        val appUri = Uri.parse("glztv://home")
        val logoUri = Uri.parse("android.resource://${context.packageName}/${R.drawable.ic_launcher}")
        val previewChannel = (if (existing == null) PreviewChannel.Builder() else PreviewChannel.Builder(existing))
            .setInternalProviderId(CHANNEL_KEY_RADIO)
            .setDisplayName("GLZ Radio")
            .setDescription("Live streaming radio stations")
            .setAppLinkIntentUri(appUri)
            .setLogo(logoUri)
            .build()
        val channelId = if (existing == null) {
            helper.publishChannel(previewChannel)
        } else {
            helper.updatePreviewChannel(existing.id, previewChannel)
            existing.id
        }

        context.contentResolver.delete(
            TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
            null,
            null
        )

        stations.forEachIndexed { index, station ->
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("glztv://radio/${Uri.encode(station.code)}")
            ).setPackage(context.packageName)

            val builder = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setWeight(index)
                .setContentId("radio_${station.code}")
                .setTitle(station.name)
                .setDescription(station.genre)
                .setType(TvContractCompat.PreviewPrograms.TYPE_TRACK)
                .setLive(true)
                .setIntent(intent)
                .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_1_1)

            station.logoUrl?.takeIf(String::isNotBlank)?.let {
                builder.setPosterArtUri(Uri.parse(it))
            }
            helper.publishPreviewProgram(builder.build())
        }
    }

    fun recordWatchNext(
        context: Context,
        channel: Channel,
        programme: Programme? = null
    ) {
        if (!isLeanbackSupported(context)) return

        runCatching {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("glztv://channel/${Uri.encode(channel.id)}")
            ).setPackage(context.packageName)

            val builder = WatchNextProgram.Builder()
                .setContentId(channel.id)
                .setTitle(programme?.title ?: channel.name)
                .setDescription(programme?.description ?: "Continue watching ${channel.name}")
                .setType(TvContractCompat.WatchNextPrograms.TYPE_CHANNEL)
                .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                .setIntent(intent)
                .setPosterArtAspectRatio(TvContractCompat.WatchNextPrograms.ASPECT_RATIO_16_9)

            channel.logoUrl.takeIf(String::isNotBlank)?.let {
                builder.setPosterArtUri(Uri.parse(it))
            }
            programme?.let {
                builder.setStartTimeUtcMillis(it.startMillis)
                builder.setEndTimeUtcMillis(it.endMillis)
            }

            val program = builder.build()
            val helper = PreviewChannelHelper(context)
            helper.publishWatchNextProgram(program)
        }
    }

    fun recordWatchNextStation(
        context: Context,
        station: RadioStation
    ) {
        if (!isLeanbackSupported(context)) return

        runCatching {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("glztv://radio/${Uri.encode(station.code)}")
            ).setPackage(context.packageName)

            val builder = WatchNextProgram.Builder()
                .setContentId("radio_${station.code}")
                .setTitle(station.name)
                .setDescription(station.genre)
                .setType(TvContractCompat.WatchNextPrograms.TYPE_TRACK)
                .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
                .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
                .setIntent(intent)
                .setPosterArtAspectRatio(TvContractCompat.WatchNextPrograms.ASPECT_RATIO_1_1)

            station.logoUrl?.takeIf(String::isNotBlank)?.let {
                builder.setPosterArtUri(Uri.parse(it))
            }

            val program = builder.build()
            val helper = PreviewChannelHelper(context)
            helper.publishWatchNextProgram(program)
        }
    }

    private fun isLeanbackSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return false
        return context.packageManager.resolveContentProvider(TvContractCompat.AUTHORITY, 0) != null
    }
}

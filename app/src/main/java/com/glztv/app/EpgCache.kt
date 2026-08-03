package com.glztv.app

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

private const val EPG_CACHE_FILE = "epg-v1.bin"
object EpgCache {
    fun read(context: Context, sourceUrl: String): EpgGuide? = runCatching {
        val file = context.filesDir.resolve(EPG_CACHE_FILE)
        if (!file.isFile) return null
        DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
            val version = input.readInt()
            if ((version != 1 && version != 2) || input.readString() != sourceUrl) return null
            input.readLong() // Saved timestamp is informational; retain last-known-good data.
            val names = buildMap {
                repeat(input.readInt()) { put(input.readString(), input.readString()) }
            }
            val logos = if (version >= 2) {
                buildMap {
                    repeat(input.readInt()) { put(input.readString(), input.readString()) }
                }
            } else emptyMap()
            val programmes = buildMap {
                repeat(input.readInt()) {
                    val channelId = input.readString()
                    put(channelId, buildList {
                        repeat(input.readInt()) {
                            add(
                                Programme(
                                    channelId = channelId,
                                    startMillis = input.readLong(),
                                    endMillis = input.readLong(),
                                    title = input.readString(),
                                    description = input.readString()
                                )
                            )
                        }
                    })
                }
            }
            EpgGuide(programmes, names, logos)
        }
    }.getOrNull()

    fun write(context: Context, sourceUrl: String, guide: EpgGuide) {
        if (sourceUrl.isBlank() || guide.programmeCount <= 0) return
        runCatching {
            val target = context.filesDir.resolve(EPG_CACHE_FILE)
            val temporary = context.filesDir.resolve("$EPG_CACHE_FILE.tmp")
            DataOutputStream(BufferedOutputStream(temporary.outputStream())).use { output ->
                output.writeInt(2)
                output.writeString(sourceUrl)
                output.writeLong(System.currentTimeMillis())
                output.writeInt(guide.channelNames.size)
                guide.channelNames.forEach { (id, name) ->
                    output.writeString(id)
                    output.writeString(name)
                }
                output.writeInt(guide.channelLogos.size)
                guide.channelLogos.forEach { (id, logoUrl) ->
                    output.writeString(id)
                    output.writeString(logoUrl)
                }
                output.writeInt(guide.programmes.size)
                guide.programmes.forEach { (channelId, entries) ->
                    output.writeString(channelId)
                    output.writeInt(entries.size)
                    entries.forEach {
                        output.writeLong(it.startMillis)
                        output.writeLong(it.endMillis)
                        output.writeString(it.title)
                        output.writeString(it.description)
                    }
                }
            }
            target.delete()
            temporary.renameTo(target)
        }
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size in 0..16_777_216)
        return ByteArray(size).also(::readFully).toString(Charsets.UTF_8)
    }
}

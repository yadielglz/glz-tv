package com.glztv.app.data

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceClientTest {
    @Test
    fun decodesPlainXml() {
        assertEquals(XML, SourceClient.decodeText(XML.toByteArray()))
    }

    @Test
    fun decodesGzippedXmlByItsBytes() {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(XML.toByteArray()) }

        assertEquals(XML, SourceClient.decodeText(output.toByteArray()))
    }

    @Test
    fun removesUtf8Bom() {
        val bytes = byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + XML.toByteArray()

        assertEquals(XML, SourceClient.decodeText(bytes))
    }

    private companion object {
        const val XML = "<?xml version=\"1.0\"?><tv><programme/></tv>"
    }
}

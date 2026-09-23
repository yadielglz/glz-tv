package com.glztv.app

data class Channel(
    val id: String,
    val name: String,
    val group: String,
    val number: String,
    val logoUrl: String,
    val streamUrl: String,
    val headers: Map<String, String> = emptyMap()
)

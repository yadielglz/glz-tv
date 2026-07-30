package com.glztv.app

import android.content.SharedPreferences
import org.json.JSONObject

data class GuestExperience(
    val propertyName: String = "",
    val welcomeMessage: String = "Relax, explore, and enjoy your stay.",
    val logoUrl: String? = null,
    val heroImageUrl: String? = null,
    val wifiName: String? = null,
    val wifiInstructions: String? = null,
    val checkoutTime: String? = null,
    val frontDesk: String? = null,
    val noticeTitle: String? = null,
    val noticeBody: String? = null,
    val roomNumber: String? = null,
    val arrivalDate: String? = null,
    val departureDate: String? = null,
    val services: List<GuestService> = emptyList()
) {
    companion object {
        fun from(prefs: SharedPreferences): GuestExperience {
            val raw = prefs.getString(GlzHubManager.GUEST_EXPERIENCE, null) ?: return GuestExperience()
            return runCatching {
                val json = JSONObject(raw)
                val serviceItems = json.optJSONArray("services")
                GuestExperience(
                    propertyName = json.text("propertyName").orEmpty(),
                    welcomeMessage = json.text("welcomeMessage")
                        ?: "Relax, explore, and enjoy your stay.",
                    logoUrl = json.text("logoUrl"),
                    heroImageUrl = json.text("heroImageUrl"),
                    wifiName = json.text("wifiName"),
                    wifiInstructions = json.text("wifiInstructions"),
                    checkoutTime = json.text("checkoutTime"),
                    frontDesk = json.text("frontDesk"),
                    noticeTitle = json.text("noticeTitle"),
                    noticeBody = json.text("noticeBody"),
                    roomNumber = json.text("roomNumber"),
                    arrivalDate = json.text("arrivalDate"),
                    departureDate = json.text("departureDate"),
                    services = buildList {
                        if (serviceItems != null) for (index in 0 until serviceItems.length()) {
                            val item = serviceItems.optJSONObject(index) ?: continue
                            val title = item.text("title") ?: continue
                            add(GuestService(title, item.text("subtitle"), item.text("actionUrl")))
                        }
                    }
                )
            }.getOrDefault(GuestExperience())
        }
    }
}

data class GuestService(
    val title: String,
    val subtitle: String?,
    val actionUrl: String?
)

private fun JSONObject.text(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf(String::isNotEmpty)

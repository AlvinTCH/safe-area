package com.getcapacitor.community.safearea

import org.json.JSONObject

class AppearanceConfig(fromJSONObject: JSONObject? = null) {
    var customColorsForSystemBars: Boolean = true
    var backgroundColor: String = "#000000"
    var statusBarContent: String = "light"
    var navigationBarContent: String = "light"

    init {
        if (fromJSONObject != null) {
            if (fromJSONObject.has("customColorsForSystemBars")) {
                customColorsForSystemBars =
                    fromJSONObject.optBoolean("customColorsForSystemBars", true)
            }
            if (fromJSONObject.has("backgroundColor")) {
                backgroundColor = fromJSONObject.optString("backgroundColor", "#000000")
            }
            if (fromJSONObject.has("statusBarContent")) {
                statusBarContent = fromJSONObject.optString("statusBarContent", "light")
            }
            if (fromJSONObject.has("navigationBarContent")) {
                navigationBarContent = fromJSONObject.optString("navigationBarContent", "light")
            }
        }
    }
}


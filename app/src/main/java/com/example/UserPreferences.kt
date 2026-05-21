package com.example

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("azanpapu_prefs", Context.MODE_PRIVATE)

    var isSoundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    var isMusicOn: Boolean
        get() = prefs.getBoolean("music", true)
        set(value) = prefs.edit().putBoolean("music", value).apply()

    var isWatermarkOn: Boolean
        get() = prefs.getBoolean("watermark", true)
        set(value) = prefs.edit().putBoolean("watermark", value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("darkMode", false)
        set(value) = prefs.edit().putBoolean("darkMode", value).apply()

    var showNumbers: Boolean
        get() = prefs.getBoolean("showNumbers", true)
        set(value) = prefs.edit().putBoolean("showNumbers", value).apply()

    var coins: Int
        get() = prefs.getInt("coins", 150)
        set(value) = prefs.edit().putInt("coins", value).apply()
}

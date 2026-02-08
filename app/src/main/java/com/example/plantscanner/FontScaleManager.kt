package com.example.plantscanner

import android.content.Context

object FontScaleManager {
    private const val PREF_NAME = "app_settings"
    private const val KEY_FONT_SCALE = "font_scale"

    fun saveFontScale(context: Context, scale: Float) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    fun getFontScale(context: Context): Float {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_FONT_SCALE, 1.0f)
    }
}
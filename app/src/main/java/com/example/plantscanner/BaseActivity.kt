package com.example.plantscanner

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val scale = FontScaleManager.getFontScale(newBase)

        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = scale

        val metrics = newBase.resources.displayMetrics
        val newMetrics = DisplayMetrics()

        newMetrics.setTo(metrics)
        newMetrics.scaledDensity = configuration.fontScale * newMetrics.density

        val context = newBase.createConfigurationContext(configuration)
        context.resources.displayMetrics.setTo(newMetrics)

        super.attachBaseContext(context)
    }
}
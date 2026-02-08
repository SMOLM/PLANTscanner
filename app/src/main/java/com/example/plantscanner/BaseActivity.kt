package com.example.plantscanner

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val scale = FontScaleManager.getFontScale(newBase)

        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = scale

        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
}
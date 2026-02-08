package com.example.plantscanner

import android.os.Bundle
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton: ImageButton = findViewById(R.id.backButton)
        val titleText: TextView = findViewById(R.id.settingsTitle)

        titleText.text = "Ustawienia"

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val seekBar: SeekBar = findViewById(R.id.fontSeekBar)
        val fontValue: TextView = findViewById(R.id.fontValue)

        val scales = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f)

        val currentScale = FontScaleManager.getFontScale(this)
        val index = scales.indexOfFirst { it >= currentScale }.coerceAtLeast(1)
        seekBar.progress = index

        fontValue.text = "${(scales[index] * 100).toInt()}%"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = scales[progress]
                fontValue.text = "${(scale * 100).toInt()}%"
                FontScaleManager.saveFontScale(this@SettingsActivity, scale)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                recreate()
            }
        })
    }
}

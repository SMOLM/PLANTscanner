package com.example.plantscanner

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton: ImageButton = findViewById(R.id.backButton)
        val titleText: TextView = findViewById(R.id.settingsTitle)

        titleText.text = "Ustawienia"

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}

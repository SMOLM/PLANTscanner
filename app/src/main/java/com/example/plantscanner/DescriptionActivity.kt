package com.example.plantscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DescriptionActivity : AppCompatActivity() {

    private val trefleToken = BuildConfig.TREFLE_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_description)

        val backButton: ImageButton = findViewById(R.id.backButton)
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        val imageView: ImageView = findViewById(R.id.resultImage)
        val speciesNameText: TextView = findViewById(R.id.speciesName)
        val speciesConfidenceText: TextView = findViewById(R.id.speciesLatin)
        val descriptionText: TextView = findViewById(R.id.descriptionText)

        val scientificName = intent.getStringExtra("species_name") ?: "Unknown"
        val confidence = intent.getDoubleExtra("confidence", 0.0)
        val imageUri = intent.getStringExtra("image_uri")

        speciesNameText.text = scientificName
        speciesConfidenceText.text = "Prawdopodobieństwo: ${(confidence * 100).toInt()}%"

        imageUri?.let {
            Glide.with(this)
                .load(Uri.parse(it))
                .centerCrop()
                .into(imageView)
        }

        fetchPlantInfo(scientificName, descriptionText)

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchPlantInfo(
        scientificName: String,
        descriptionText: TextView
    ) {
        val client = OkHttpClient()
        val url = "https://trefle.io/api/v1/plants/search?token=$trefleToken&q=$scientificName"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showErrorDialog("Brak połączenia z internetem lub błąd komunikacji z serwerem Trefle.")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        showErrorDialog("Trefle API zwróciło błąd. Spróbuj ponownie później.")
                        return
                    }

                    val json = response.body?.string() ?: run {
                        showErrorDialog("Nie udało się odczytać odpowiedzi z serwera.")
                        return
                    }

                    val dataArray = JSONObject(json).optJSONArray("data")
                    if (dataArray == null || dataArray.length() == 0) {
                        showErrorDialog("Trefle nie znalazło informacji o tym gatunku.")
                        return
                    }

                    val plant = dataArray.getJSONObject(0)

                    val common = plant.optString("common_name", "Brak danych")
                    val family = plant.optString("family", "Brak danych")
                    val genus = plant.optString("genus", "Brak danych")

                    val descriptionResult = """
                        Nazwa zwyczajowa: $common
                        Rodzina: $family
                        Rodzaj: $genus
                    """.trimIndent()

                    runOnUiThread {
                        descriptionText.text = descriptionResult
                        descriptionText.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
                    }
                }
            }
        })
    }

    private fun showErrorDialog(message: String) {
        runOnUiThread {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Błąd")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}

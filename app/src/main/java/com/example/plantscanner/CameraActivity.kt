package com.example.plantscanner

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var photoBtn: ImageButton
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var loadingOverlay: FrameLayout

    private val apiKey = BuildConfig.PLANTNET_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)
        photoBtn = findViewById(R.id.photoBtn)
        cameraExecutor = Executors.newSingleThreadExecutor()
        loadingOverlay = findViewById(R.id.loadingOverlay)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        photoBtn.setOnClickListener {
            takePhoto()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(95)
                .setTargetRotation(viewFinder.display.rotation)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun showLoading(show: Boolean) {
        runOnUiThread {
            loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
            photoBtn.isEnabled = !show
        }
    }
    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            showErrorDialog("Kamera jeszcze niegotowa")
            return
        }

        val tempFile = File.createTempFile("camera_", ".jpg", cacheDir)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    showErrorDialog("Błąd zapisu zdjęcia: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val imageUri = saveImageToGallery(tempFile)
                    if (imageUri == null) {
                        showErrorDialog("Nie udało się zapisać zdjęcia w galerii")
                        return
                    }

                    showLoading(true)
                    uploadToApi(imageUri)
                }
            }
        )
    }

    private fun saveImageToGallery(file: File): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Plant_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/PlantScanner")
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        file.delete()
        return uri
    }

    private fun uploadToApi(imageUri: Uri) {

        val inputStream = contentResolver.openInputStream(imageUri)
        val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)

        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "images",
                tempFile.name,
                tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .addFormDataPart("organs", "leaf")
            .build()

        val request = Request.Builder()
            .url("https://my-api.plantnet.org/v2/identify/all?api-key=$apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showLoading(false)
                    showErrorDialog("Brak połączenia z internetem lub błąd serwera.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            showLoading(false)
                            showErrorDialog("Błąd połączenia z serwerem. Spróbuj ponownie.")
                        }
                        return
                    }

                    val jsonResponse = response.body?.string()
                    val jsonObject = JSONObject(jsonResponse ?: "{}")
                    val results = jsonObject.getJSONArray("results")

                    if (results.length() > 0) {
                        val best = results.getJSONObject(0)
                        val species = best.getJSONObject("species")
                            .getString("scientificNameWithoutAuthor")
                        val score = best.getDouble("score")

                        runOnUiThread {
                            showLoading(false)
                            val intent = Intent(this@CameraActivity, DescriptionActivity::class.java).apply {
                                putExtra("species_name", species)
                                putExtra("confidence", score)
                                putExtra("image_uri", imageUri.toString()) // ← URI z galerii
                            }
                            startActivity(intent)
                        }
                    } else {
                        runOnUiThread {
                            showLoading(false)
                            showErrorDialog("Nie rozpoznano rośliny")
                        }
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
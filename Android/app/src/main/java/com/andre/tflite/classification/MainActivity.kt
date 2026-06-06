package com.andre.tflite.classification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var statusText: TextView
    private var segmentationHelper: SegmentationHelper? = null
    private var originalBitmap: Bitmap? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        runSegmentation(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        statusText = findViewById(R.id.statusText)

        try {
            segmentationHelper = SegmentationHelper(this)
            statusText.text = getString(R.string.ready)
        } catch (e: Exception) {
            statusText.text = getString(R.string.model_missing)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnPickImage).setOnClickListener {
            if (segmentationHelper == null) {
                Toast.makeText(this, R.string.model_missing, Toast.LENGTH_SHORT).show()
            } else {
                pickImageLauncher.launch("image/*")
            }
        }
    }

    private fun runSegmentation(uri: Uri) {
        val helper = segmentationHelper ?: return

        contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream) ?: return
            originalBitmap = bitmap

            statusText.text = getString(R.string.running)
            imageView.setImageBitmap(bitmap)

            Thread {
                try {
                    val overlay = helper.segmentWithOverlay(bitmap)
                    runOnUiThread {
                        imageView.setImageBitmap(overlay)
                        statusText.text = getString(R.string.done)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        statusText.text = getString(R.string.error)
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }
}

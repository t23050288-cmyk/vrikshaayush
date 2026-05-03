package com.vrikshaayush.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vrikshaayush.databinding.ActivityScannerBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private var photoUri: Uri? = null
    private var selectedImagePath: String? = null

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                try {
                    val bitmap = loadBitmapFromUri(uri)
                    binding.ivPreview.setImageBitmap(bitmap)
                    binding.ivPreview.visibility = View.VISIBLE
                    // Save to temp file for diagnosis
                    selectedImagePath = saveUriToTempFile(uri)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = loadBitmapFromUri(it)
                binding.ivPreview.setImageBitmap(bitmap)
                binding.ivPreview.visibility = View.VISIBLE
                // Save to temp file for diagnosis
                selectedImagePath = saveUriToTempFile(it)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Camera permission needed to take photos", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnDiagnose.setOnClickListener {
            val path = selectedImagePath
            if (path != null && File(path).exists()) {
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("IMAGE_PATH", path)
                startActivity(intent)
            } else {
                Toast.makeText(this, "⚠ Please select or take a photo first", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PLANT_${timeStamp}_", ".jpg", storageDir)
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    private fun saveUriToTempFile(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        val file = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file.absolutePath
    }
}

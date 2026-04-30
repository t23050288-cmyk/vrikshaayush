package com.vrikshaayush.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    private var selectedBitmap: Bitmap? = null

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                selectedBitmap = bitmap
                binding.ivPreview.setImageBitmap(bitmap)
                binding.ivPreview.visibility = android.view.View.VISIBLE
            }
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            selectedBitmap = bitmap
            photoUri = it
            binding.ivPreview.setImageBitmap(bitmap)
            binding.ivPreview.visibility = android.view.View.VISIBLE
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

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
            selectedBitmap?.let { bitmap ->
                val intent = Intent(this, ResultActivity::class.java)
                // Save bitmap to temp file and pass path
                val tempFile = saveTempBitmap(bitmap)
                intent.putExtra("IMAGE_PATH", tempFile.absolutePath)
                startActivity(intent)
            } ?: Toast.makeText(this, "Please select or take a photo first", Toast.LENGTH_SHORT).show()
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

    private fun saveTempBitmap(bitmap: Bitmap): File {
        val file = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }
}

package com.vrikshaayush.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vrikshaayush.R
import com.vrikshaayush.data.AppDatabase
import com.vrikshaayush.data.ScanRecord
import com.vrikshaayush.databinding.ActivityResultBinding
import com.vrikshaayush.ml.DiseaseClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var db: AppDatabase
    private var imagePath: String = ""
    private var diseaseName: String = ""
    private var cropType: String = ""
    private var severity: String = ""
    private var confidence: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        imagePath = intent.getStringExtra("IMAGE_PATH") ?: ""

        binding.btnBack.setOnClickListener { finish() }

        if (imagePath.isNotEmpty()) {
            runDiagnosis()
        }
    }

    private fun runDiagnosis() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.layoutResult.visibility = android.view.View.GONE

        val bitmap = BitmapFactory.decodeFile(imagePath)
        binding.ivPlantPhoto.setImageBitmap(bitmap)

        lifecycleScope.launch(Dispatchers.IO) {
            val classifier = DiseaseClassifier(this@ResultActivity)
            val result = classifier.classify(bitmap)
            classifier.close()

            withContext(Dispatchers.Main) {
                diseaseName = result.diseaseName
                cropType = result.cropType
                severity = result.severity
                confidence = result.confidence

                displayResult(result)
            }
        }
    }

    private fun displayResult(result: com.vrikshaayush.ml.DiagnosisResult) {
        binding.progressBar.visibility = android.view.View.GONE
        binding.layoutResult.visibility = android.view.View.VISIBLE

        binding.tvDiseaseName.text = result.diseaseName
        binding.tvCropType.text = result.cropType
        binding.tvConfidence.text = "${result.confidence.toInt()}%"
        binding.progressConfidence.progress = result.confidence.toInt()

        // Severity color
        val severityColor = when (result.severity) {
            "HIGH" -> ContextCompat.getColor(this, R.color.severity_high)
            "MEDIUM" -> ContextCompat.getColor(this, R.color.severity_medium)
            else -> ContextCompat.getColor(this, R.color.severity_low)
        }
        binding.tvSeverity.text = result.severity
        binding.tvSeverity.setBackgroundColor(severityColor)

        // Treatment tips (loaded from disease repo)
        val treatments = getTreatmentTips(result.diseaseName)
        binding.tvTreatment1.text = "• ${treatments[0]}"
        binding.tvTreatment2.text = "• ${treatments[1]}"
        binding.tvTreatment3.text = "• ${treatments[2]}"

        binding.btnSeeDetails.setOnClickListener {
            val intent = Intent(this, DiseaseDetailActivity::class.java)
            intent.putExtra("DISEASE_NAME", result.diseaseName)
            intent.putExtra("CROP_TYPE", result.cropType)
            startActivity(intent)
        }

        binding.btnSaveHistory.setOnClickListener {
            saveToHistory()
        }
    }

    private fun saveToHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.scanDao().insert(
                ScanRecord(
                    imagePath = imagePath,
                    cropType = cropType,
                    diseaseName = diseaseName,
                    confidence = confidence,
                    severity = severity
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ResultActivity, "Saved to history!", Toast.LENGTH_SHORT).show()
                binding.btnSaveHistory.isEnabled = false
                binding.btnSaveHistory.text = "✓ Saved"
            }
        }
    }

    private fun getTreatmentTips(disease: String): List<String> {
        return when {
            disease.contains("Early Blight", ignoreCase = true) -> listOf(
                "Remove infected leaves and burn them",
                "Apply organic fungicide spray every 7 days",
                "Avoid overhead watering to keep leaves dry"
            )
            disease.contains("Late Blight", ignoreCase = true) -> listOf(
                "Remove and destroy infected plants immediately",
                "Spray copper-based solution every 5-7 days",
                "Avoid overhead irrigation"
            )
            disease.contains("Blast", ignoreCase = true) -> listOf(
                "Apply Tricyclazole fungicide at 0.6g per liter",
                "Avoid excess nitrogen fertilizer",
                "Ensure proper water management"
            )
            disease.contains("Rust", ignoreCase = true) -> listOf(
                "Apply Propiconazole 1ml per liter at first sign",
                "Plant rust-resistant varieties next season",
                "Monitor fields weekly"
            )
            disease.contains("healthy", ignoreCase = true) -> listOf(
                "Plant looks healthy — keep monitoring weekly",
                "Maintain soil health with compost",
                "Water consistently at base of plant"
            )
            else -> listOf(
                "Consult your local agricultural officer",
                "Remove visibly infected parts",
                "Apply general fungicide as preventive measure"
            )
        }
    }
}

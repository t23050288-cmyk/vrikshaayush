package com.vrikshaayush.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
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
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutResult.visibility = View.GONE

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
        binding.progressBar.visibility = View.GONE
        binding.layoutResult.visibility = View.VISIBLE

        if (result.isUncertain) {
            // Show uncertain state - prompt user to retake
            binding.tvDiseaseName.text = "⚠️ Cannot Identify Plant"
            binding.tvCropType.text = "Please retake with better lighting"
            binding.tvConfidence.text = "${result.confidence.toInt()}%"
            binding.progressConfidence.progress = result.confidence.toInt()
            binding.tvSeverity.text = "UNCLEAR"
            binding.tvSeverity.setBackgroundColor(ContextCompat.getColor(this, R.color.severity_low))
            binding.tvTreatment1.text = "• Make sure leaf fills most of the photo"
            binding.tvTreatment2.text = "• Take photo in good natural lighting"
            binding.tvTreatment3.text = "• Avoid shadows or blurry images"
            binding.btnSeeDetails.visibility = View.GONE
            binding.btnSaveHistory.visibility = View.GONE
            return
        }

        binding.tvDiseaseName.text = result.diseaseName
        binding.tvCropType.text = result.cropType
        binding.tvConfidence.text = "${result.confidence.toInt()}%"
        binding.progressConfidence.progress = result.confidence.toInt()

        val severityColor = when (result.severity) {
            "HIGH" -> ContextCompat.getColor(this, R.color.severity_high)
            "MEDIUM" -> ContextCompat.getColor(this, R.color.severity_medium)
            else -> ContextCompat.getColor(this, R.color.severity_low)
        }
        binding.tvSeverity.text = result.severity
        binding.tvSeverity.setBackgroundColor(severityColor)

        val treatments = getTreatmentTips(result.diseaseName)
        binding.tvTreatment1.text = "• ${treatments[0]}"
        binding.tvTreatment2.text = "• ${treatments[1]}"
        binding.tvTreatment3.text = "• ${treatments[2]}"

        binding.btnSeeDetails.visibility = View.VISIBLE
        binding.btnSaveHistory.visibility = View.VISIBLE

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
                "Remove infected leaves immediately and burn them",
                "Apply organic fungicide (neem oil) every 7 days",
                "Avoid overhead watering — water at base only"
            )
            disease.contains("Late Blight", ignoreCase = true) -> listOf(
                "Remove and destroy infected plants same day",
                "Spray copper-based fungicide every 5-7 days",
                "Do not compost infected material"
            )
            disease.contains("Leaf Mold", ignoreCase = true) -> listOf(
                "Improve air circulation around plants",
                "Apply chlorothalonil fungicide spray",
                "Reduce humidity and avoid overcrowding"
            )
            disease.contains("Septoria", ignoreCase = true) -> listOf(
                "Remove infected lower leaves first",
                "Apply mancozeb or copper fungicide",
                "Mulch soil to prevent spore splash"
            )
            disease.contains("Mosaic", ignoreCase = true) -> listOf(
                "No chemical cure — remove infected plant",
                "Control aphids which spread the virus",
                "Wash hands after touching infected plants"
            )
            disease.contains("Yellow Leaf Curl", ignoreCase = true) -> listOf(
                "Remove infected plants to stop spread",
                "Control whiteflies using yellow sticky traps",
                "Use virus-resistant tomato varieties"
            )
            disease.contains("Spider", ignoreCase = true) -> listOf(
                "Spray neem oil solution on undersides of leaves",
                "Increase humidity — spider mites hate moisture",
                "Use insecticidal soap spray"
            )
            disease.contains("Bacterial Spot", ignoreCase = true) -> listOf(
                "Apply copper hydroxide spray every 7-10 days",
                "Avoid working with plants when wet",
                "Rotate crops each season"
            )
            disease.contains("Blast", ignoreCase = true) -> listOf(
                "Apply Tricyclazole fungicide at 0.6g per liter",
                "Avoid excess nitrogen fertilizer",
                "Drain field for 3-4 days if severe"
            )
            disease.contains("Rust", ignoreCase = true) -> listOf(
                "Apply Propiconazole 1ml per liter at first sign",
                "Plant rust-resistant varieties next season",
                "Remove and burn all fallen leaves"
            )
            disease.contains("Powdery Mildew", ignoreCase = true) -> listOf(
                "Apply baking soda + water spray (1 tbsp per liter)",
                "Improve air circulation between plants",
                "Apply sulfur-based fungicide if severe"
            )
            disease.contains("Black Rot", ignoreCase = true) -> listOf(
                "Remove and destroy all infected fruit and leaves",
                "Apply copper fungicide spray",
                "Prune to improve airflow through canopy"
            )
            disease.contains("healthy", ignoreCase = true) -> listOf(
                "Plant looks healthy! Keep monitoring weekly",
                "Maintain soil health with organic compost",
                "Water consistently at base of plant"
            )
            else -> listOf(
                "Consult your local Krishi Vigyan Kendra (KVK)",
                "Remove visibly infected leaves or parts",
                "Apply general neem-based fungicide as preventive"
            )
        }
    }
}


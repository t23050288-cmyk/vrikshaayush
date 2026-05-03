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
    private var modelLabel: String = ""

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
                modelLabel = result.label
                displayResult(result)
            }
        }
    }

    private fun displayResult(result: com.vrikshaayush.ml.DiagnosisResult) {
        binding.progressBar.visibility = View.GONE
        binding.layoutResult.visibility = View.VISIBLE

        if (result.isUncertain) {
            binding.tvDiseaseName.text = "⚠️ Cannot Identify Plant"
            binding.tvCropType.text = "Please retake with better lighting"
            binding.tvConfidence.text = "${result.confidence.toInt()}%"
            binding.progressConfidence.progress = result.confidence.toInt()
            binding.tvSeverity.text = "UNCLEAR"
            binding.tvSeverity.setBackgroundColor(ContextCompat.getColor(this, R.color.severity_low))
            binding.tvTreatment1.text = "• Make sure the leaf fills most of the photo"
            binding.tvTreatment2.text = "• Take photo in good natural lighting outdoors"
            binding.tvTreatment3.text = "• Focus on ONE leaf — avoid shadows or blurry images"
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
            intent.putExtra("MODEL_LABEL", result.label)
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
                "Apply neem oil spray every 7 days",
                "Avoid overhead watering — water at base only"
            )
            disease.contains("Late Blight", ignoreCase = true) -> listOf(
                "Remove and destroy infected plants same day",
                "Spray copper-based fungicide every 5-7 days",
                "Do not compost infected material"
            )
            disease.contains("Northern Leaf Blight", ignoreCase = true) ||
            disease.contains("Leaf Blight", ignoreCase = true) -> listOf(
                "Apply Mancozeb 75WP 2.5g/litre every 10-14 days",
                "Use resistant hybrid maize varieties next season",
                "Deep plow after harvest to bury infected debris"
            )
            disease.contains("Gray Leaf Spot", ignoreCase = true) ||
            disease.contains("Cercospora", ignoreCase = true) -> listOf(
                "Spray Propiconazole 25EC 1ml/litre at first sign",
                "Ensure proper plant spacing for air circulation",
                "Rotate crops with soybean or wheat next season"
            )
            disease.contains("Common Rust", ignoreCase = true) -> listOf(
                "Apply sulfur dust or Mancozeb 75WP spray",
                "Monitor weekly — rust spreads fast in cool weather",
                "Use rust-resistant hybrid varieties next season"
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
                "No chemical cure — remove infected plant immediately",
                "Wash hands with soap before touching other plants",
                "Use certified virus-free seeds next season"
            )
            disease.contains("Yellow Leaf Curl", ignoreCase = true) -> listOf(
                "Remove infected plants to stop spread",
                "Control whiteflies using yellow sticky traps",
                "Spray Imidacloprid to kill whitefly vectors"
            )
            disease.contains("Spider", ignoreCase = true) -> listOf(
                "Spray water forcefully on leaf undersides daily",
                "Apply neem oil 2% on undersides of leaves",
                "Use Abamectin 1EC if severe"
            )
            disease.contains("Bacterial Spot", ignoreCase = true) -> listOf(
                "Apply copper oxychloride 50WP spray every 7-10 days",
                "Avoid working with plants when wet",
                "Rotate crops each season"
            )
            disease.contains("Rust", ignoreCase = true) -> listOf(
                "Apply Propiconazole 1ml/litre at first sign",
                "Plant rust-resistant varieties next season",
                "Remove and burn all fallen leaves"
            )
            disease.contains("Powdery Mildew", ignoreCase = true) -> listOf(
                "Apply baking soda + water spray (1 tbsp per litre)",
                "Improve air circulation between plants",
                "Apply sulfur-based fungicide if severe"
            )
            disease.contains("Black Rot", ignoreCase = true) -> listOf(
                "Remove and destroy all infected fruit and leaves",
                "Apply copper fungicide spray",
                "Prune to improve airflow through canopy"
            )
            disease.contains("Citrus Greening", ignoreCase = true) ||
            disease.contains("Haunglongbing", ignoreCase = true) -> listOf(
                "Remove infected trees immediately — no cure exists",
                "Control citrus psyllid with Imidacloprid spray",
                "Install yellow sticky traps to monitor psyllid"
            )
            disease.contains("Leaf Scorch", ignoreCase = true) -> listOf(
                "Remove infected leaves and destroy them",
                "Apply Captan 50WP every 10 days",
                "Switch to drip irrigation — avoid wetting leaves"
            )
            disease.contains("Target Spot", ignoreCase = true) -> listOf(
                "Apply Azoxystrobin 23SC 1ml/litre spray",
                "Remove heavily infected leaves promptly",
                "Improve air circulation around plants"
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

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
import java.io.File

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

        if (imagePath.isNotEmpty() && File(imagePath).exists()) {
            runDiagnosis()
        } else {
            // Show error - do NOT finish, let user see the problem
            binding.progressBar.visibility = View.GONE
            binding.layoutResult.visibility = View.VISIBLE
            binding.tvDiseaseName.text = "⚠️ Image Not Found"
            binding.tvCropType.text = "Please go back and select a photo again"
            binding.tvConfidence.text = "0%"
            binding.progressConfidence.progress = 0
            binding.tvSeverity.text = "ERROR"
            binding.tvTreatment1.text = "• Go back using the ← button"
            binding.tvTreatment2.text = "• Take a new photo or select from gallery"
            binding.tvTreatment3.text = "• Make sure the photo is clear and well-lit"
            binding.btnSeeDetails.visibility = View.GONE
            binding.btnSaveHistory.visibility = View.GONE
        }
    }

    private fun runDiagnosis() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutResult.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                    ?: throw Exception("Failed to decode image")
                
                withContext(Dispatchers.Main) {
                    binding.ivPlantPhoto.setImageBitmap(bitmap)
                }

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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutResult.visibility = View.VISIBLE
                    binding.tvDiseaseName.text = "⚠️ Diagnosis Failed"
                    binding.tvCropType.text = "Error: ${e.message}"
                    binding.tvConfidence.text = "0%"
                    binding.progressConfidence.progress = 0
                    binding.tvSeverity.text = "ERROR"
                    binding.tvTreatment1.text = "• Go back and try with a clearer photo"
                    binding.tvTreatment2.text = "• Ensure the leaf is well-lit"
                    binding.tvTreatment3.text = "• Try selecting from gallery instead"
                    binding.btnSeeDetails.visibility = View.GONE
                    binding.btnSaveHistory.visibility = View.GONE
                }
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
            "HEALTHY" -> ContextCompat.getColor(this, R.color.severity_low)
            else -> ContextCompat.getColor(this, R.color.severity_low)
        }
        binding.tvSeverity.text = result.severity
        binding.tvSeverity.setBackgroundColor(severityColor)

        val treatments = getTreatmentTips(result.label, result.diseaseName)
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
            val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
            intent.putExtra("LANGUAGE", lang)
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
                Toast.makeText(this@ResultActivity, "✅ Saved to history!", Toast.LENGTH_SHORT).show()
                binding.btnSaveHistory.isEnabled = false
                binding.btnSaveHistory.text = "✓ Saved"
            }
        }
    }

    private fun getTreatmentTips(label: String, disease: String): List<String> {
        return when {
            // HEALTHY
            disease.contains("Healthy", ignoreCase = true) -> listOf(
                "Your plant looks healthy! Keep up the good care.",
                "Water regularly and ensure adequate sunlight.",
                "Monitor weekly for any early signs of disease."
            )
            // RICE
            label.contains("rice") -> listOf(
                "Remove infected leaves and apply Tricyclazole 75WP 0.6g/L spray",
                "Ensure proper water drainage in paddy fields",
                "Use certified disease-resistant rice seeds next season"
            )
            // WHEAT
            label.contains("wheat") -> listOf(
                "Apply Propiconazole 25EC 1ml/L at first sign of rust or blight",
                "Harvest early if disease is severe to reduce losses",
                "Use treated seeds with Carboxin + Thiram next sowing"
            )
            // COTTON
            label.contains("cotton") -> listOf(
                "Apply Imidacloprid 17.8SL 0.5ml/L for aphid/army worm control",
                "Remove and burn heavily infected plants to stop spread",
                "Spray copper oxychloride for bacterial blight control"
            )
            // SUGARCANE
            label.contains("sugarcane") -> listOf(
                "Use disease-free setts for planting",
                "Apply Carbendazim 50WP 1g/L for fungal diseases",
                "Rogue out infected plants early in the season"
            )
            // GROUNDNUT
            label.contains("groundnut") -> listOf(
                "Spray Mancozeb 75WP 2.5g/L for leaf spot diseases",
                "Maintain proper spacing for air circulation",
                "Apply gypsum at flowering stage to improve pod filling"
            )
            // LEMON / CITRUS
            label.contains("lemon") -> listOf(
                "Apply copper oxychloride 50WP 3g/L for canker and blight",
                "Remove and burn infected twigs and fruits",
                "Spray Imidacloprid for citrus psyllid vector control"
            )
            // TOMATO
            label.contains("tomato") && disease.contains("Early Blight", ignoreCase = true) -> listOf(
                "Remove infected leaves immediately and burn them",
                "Apply neem oil 2% spray every 7 days",
                "Avoid overhead watering — water at base only"
            )
            label.contains("tomato") && disease.contains("Late Blight", ignoreCase = true) -> listOf(
                "Remove and destroy infected plants same day",
                "Spray copper-based fungicide every 5-7 days",
                "Do not compost infected material"
            )
            label.contains("tomato") && disease.contains("Mosaic", ignoreCase = true) -> listOf(
                "No chemical cure — remove infected plant immediately",
                "Wash hands with soap before touching other plants",
                "Use certified virus-free seeds next season"
            )
            label.contains("tomato") && disease.contains("Yellow Leaf Curl", ignoreCase = true) -> listOf(
                "Remove infected plants to stop spread",
                "Control whiteflies using yellow sticky traps",
                "Spray Imidacloprid to kill whitefly vectors"
            )
            // POTATO
            label.contains("potato") -> listOf(
                "Apply Mancozeb 75WP 2.5g/L every 10 days",
                "Destroy infected tubers — do not leave in field",
                "Use certified disease-free seed potatoes next season"
            )
            // CORN/MAIZE
            label.contains("corn") -> listOf(
                "Apply Mancozeb 75WP 2.5g/L every 10-14 days",
                "Use resistant hybrid maize varieties next season",
                "Deep plow after harvest to bury infected debris"
            )
            // APPLE
            label.contains("apple") -> listOf(
                "Apply captan 50WP or myclobutanil fungicide spray",
                "Prune dead branches and remove fallen leaves",
                "Use resistant apple varieties like Enterprise or Liberty"
            )
            // GRAPE
            label.contains("grape") -> listOf(
                "Apply copper-based fungicide before and after rain",
                "Prune to improve air circulation in canopy",
                "Remove and destroy all infected leaves and fruit"
            )
            // DEFAULT
            else -> listOf(
                "Remove visibly infected leaves and destroy them",
                "Apply appropriate fungicide or pesticide based on disease type",
                "Consult your local agricultural extension officer for guidance"
            )
        }
    }
}

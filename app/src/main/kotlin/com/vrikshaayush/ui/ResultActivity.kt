package com.vrikshaayush.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import android.net.Uri
import java.io.File
import java.util.Locale

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var db: AppDatabase
    private var imagePath: String = ""
    private var diseaseName: String = ""
    private var cropType: String = ""
    private var severity: String = ""
    private var confidence: Float = 0f
    private var modelLabel: String = ""

    // Key used to pass AI suggestion via SharedPrefs between activities
    companion object {
        const val PREF_PENDING_AI = "pending_ai_suggestion"
        const val PREF_PENDING_LABEL = "pending_ai_label"  // which scan this is for
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        imagePath = intent.getStringExtra("IMAGE_PATH") ?: ""

        binding.btnBack.setOnClickListener { finish() }

        // Clear any old pending AI suggestion for a different scan
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getString(PREF_PENDING_LABEL, "") != imagePath) {
            prefs.edit().remove(PREF_PENDING_AI).putString(PREF_PENDING_LABEL, imagePath).apply()
        }

        if (imagePath.isNotEmpty() && File(imagePath).exists()) {
            runDiagnosis()
        } else {
            showError("Image not found", "Please go back and select a photo again")
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if AI returned a suggestion via SharedPrefs
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val pending = prefs.getString(PREF_PENDING_AI, "") ?: ""
        if (pending.isNotEmpty()) {
            Toast.makeText(this, "✅ AI suggestion ready — tap Save to History to include it", Toast.LENGTH_LONG).show()
        }
    }

    private fun showError(title: String, msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.layoutResult.visibility = View.VISIBLE
        binding.tvDiseaseName.text = "⚠️ $title"
        binding.tvCropType.text = msg
        binding.tvConfidence.text = "0%"
        binding.progressConfidence.progress = 0
        binding.tvSeverity.text = "ERROR"
        binding.tvTreatment1.text = "• Go back using the ← button"
        binding.tvTreatment2.text = "• Take a new photo or select from gallery"
        binding.tvTreatment3.text = "• Make sure the photo is clear and well-lit"
        binding.btnSeeDetails.visibility = View.GONE
        binding.btnSaveHistory.visibility = View.GONE
    }

    private fun runDiagnosis() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutResult.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath) ?: throw Exception("Failed to decode image")
                withContext(Dispatchers.Main) { binding.ivPlantPhoto.setImageBitmap(bitmap) }

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
                withContext(Dispatchers.Main) { showError("Diagnosis Failed", e.message ?: "Unknown error") }
            }
        }
    }

    private fun displayResult(result: com.vrikshaayush.ml.DiagnosisResult) {
        binding.progressBar.visibility = View.GONE
        binding.layoutResult.visibility = View.VISIBLE

        if (result.isNotLeaf) {
            binding.tvDiseaseName.text = getString(R.string.no_leaf_detected)
            binding.tvCropType.text = getString(R.string.no_leaf_hint)
            binding.tvConfidence.text = "—"
            binding.progressConfidence.progress = 0
            binding.tvSeverity.text = "N/A"
            binding.tvTreatment1.text = getString(R.string.no_leaf_tip1)
            binding.tvTreatment2.text = getString(R.string.no_leaf_tip2)
            binding.tvTreatment3.text = getString(R.string.no_leaf_tip3)
            binding.btnSeeDetails.visibility = View.GONE
            binding.btnSaveHistory.visibility = View.GONE
            binding.fabAiExpert.visibility = View.GONE
            binding.cardSOS.visibility = View.GONE
            binding.btnRescan.setOnClickListener { launchScanner() }
            return
        }

        if (result.isUncertain) {
            binding.tvDiseaseName.text = getString(R.string.cannot_identify)
            binding.tvCropType.text = getString(R.string.retake_hint)
            binding.tvConfidence.text = "${result.confidence.toInt()}%"
            binding.progressConfidence.progress = result.confidence.toInt()
            binding.tvSeverity.text = "UNCLEAR"
            binding.tvSeverity.setBackgroundColor(ContextCompat.getColor(this, R.color.severity_low))
            binding.tvTreatment1.text = getString(R.string.unclear_tip1)
            binding.tvTreatment2.text = getString(R.string.unclear_tip2)
            binding.tvTreatment3.text = getString(R.string.unclear_tip3)
            binding.btnSeeDetails.visibility = View.GONE
            binding.btnSaveHistory.visibility = View.GONE
            binding.fabAiExpert.visibility = View.GONE
            binding.cardSOS.visibility = View.GONE
            binding.btnRescan.setOnClickListener { launchScanner() }
            return
        }

        // ── Normal result ──
        binding.tvDiseaseName.text = result.diseaseName
        binding.tvCropType.text = result.cropType
        binding.tvConfidence.text = "${result.confidence.toInt()}%"
        binding.progressConfidence.progress = result.confidence.toInt()

        val severityColor = when (result.severity) {
            "HIGH"    -> ContextCompat.getColor(this, R.color.severity_high)
            "MEDIUM"  -> ContextCompat.getColor(this, R.color.severity_medium)
            "HEALTHY" -> ContextCompat.getColor(this, R.color.severity_low)
            else      -> ContextCompat.getColor(this, R.color.severity_low)
        }
        binding.tvSeverity.text = result.severity
        binding.tvSeverity.setBackgroundColor(severityColor)

        val treatments = getTreatmentTips(result.label, result.diseaseName)
        binding.tvTreatment1.text = "• ${treatments[0]}"
        binding.tvTreatment2.text = "• ${treatments[1]}"
        binding.tvTreatment3.text = "• ${treatments[2]}"

        binding.btnSeeDetails.visibility = View.VISIBLE
        binding.btnSaveHistory.visibility = View.VISIBLE

        binding.btnRescan.setOnClickListener { launchScanner() }

        binding.fabAiExpert.visibility = View.VISIBLE
        binding.fabAiExpert.setOnClickListener {
            // Clear old pending suggestion for this scan
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .remove(PREF_PENDING_AI)
                .putString(PREF_PENDING_LABEL, imagePath)
                .apply()
            val context = "${result.diseaseName} on ${result.cropType}"
            val intent = Intent(this, AiChatActivity::class.java)
            intent.putExtra("SCAN_CONTEXT", context)
            intent.putExtra("SAVE_SUGGESTION_KEY", PREF_PENDING_AI)  // AI will save reply here
            startActivity(intent)
        }

        if (result.severity == "HIGH") {
            binding.cardSOS.visibility = View.VISIBLE
            binding.btnCallKVK.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:18001801551") })
            }
        } else {
            binding.cardSOS.visibility = View.GONE
        }

        binding.btnSeeDetails.setOnClickListener {
            val intent = Intent(this, DiseaseDetailActivity::class.java)
            intent.putExtra("DISEASE_NAME", result.diseaseName)
            intent.putExtra("CROP_TYPE", result.cropType)
            intent.putExtra("MODEL_LABEL", result.label)
            val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
            intent.putExtra("LANGUAGE", lang)
            startActivity(intent)
        }

        binding.btnSaveHistory.setOnClickListener { saveToHistory() }
    }

    private fun launchScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun saveToHistory() {
        // Grab AI suggestion from SharedPrefs if available
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val aiSuggestion = prefs.getString(PREF_PENDING_AI, "") ?: ""

        lifecycleScope.launch(Dispatchers.IO) {
            db.scanDao().insert(
                ScanRecord(
                    imagePath    = imagePath,
                    cropType     = cropType,
                    diseaseName  = diseaseName,
                    confidence   = confidence,
                    severity     = severity,
                    aiSuggestion = aiSuggestion
                )
            )
            // Clear the pending AI after saving
            prefs.edit().remove(PREF_PENDING_AI).apply()

            withContext(Dispatchers.Main) {
                val msg = if (aiSuggestion.isNotEmpty()) "✅ Saved with AI suggestion!" else "✅ Saved to history!"
                Toast.makeText(this@ResultActivity, msg, Toast.LENGTH_SHORT).show()
                binding.btnSaveHistory.isEnabled = false
                binding.btnSaveHistory.text = "✓ Saved"
            }
        }
    }

    private fun getTreatmentTips(label: String, disease: String): List<String> {
        return when {
            disease.contains("Healthy", ignoreCase = true) -> listOf(
                "Your plant looks healthy! Keep up the good care.",
                "Water regularly and ensure adequate sunlight.",
                "Monitor weekly for any early signs of disease."
            )
            label.contains("early_blight") || disease.contains("Early Blight") -> listOf(
                "Remove infected leaves immediately and burn them",
                "Apply neem oil 2% spray every 7 days",
                "Avoid overhead watering — water at base only"
            )
            label.contains("late_blight") || disease.contains("Late Blight") -> listOf(
                "Remove and destroy infected plant parts immediately",
                "Apply Mancozeb 75 WP @ 2.5g/litre every 7-10 days",
                "Improve drainage and avoid waterlogging"
            )
            label.contains("powdery_mildew") || disease.contains("Powdery Mildew") -> listOf(
                "Apply wettable sulfur 80 WP @ 3g/litre spray",
                "Improve air circulation by pruning crowded branches",
                "Avoid watering leaves — water soil directly"
            )
            label.contains("rust") || disease.contains("Rust") -> listOf(
                "Remove and destroy all infected plant parts",
                "Apply Propiconazole 25 EC @ 1ml/litre spray",
                "Avoid overhead irrigation — water at ground level"
            )
            label.contains("mosaic") || disease.contains("Mosaic") -> listOf(
                "Remove and destroy infected plants to prevent spread",
                "Control aphids with neem oil spray @ 5ml/litre",
                "Use virus-free certified seeds next season"
            )
            label.contains("black_rot") || disease.contains("Black Rot") -> listOf(
                "Prune and destroy infected wood and leaves",
                "Apply Bordeaux mixture 1% spray at early stage",
                "Avoid wetting foliage — use drip irrigation"
            )
            label.contains("leaf_spot") || disease.contains("Leaf Spot") -> listOf(
                "Remove infected leaves and dispose away from field",
                "Apply Carbendazim 50 WP @ 1g/litre spray",
                "Ensure proper spacing for good air circulation"
            )
            else -> listOf(
                "Remove and destroy infected plant parts immediately",
                "Apply appropriate fungicide as recommended",
                "Consult your local Krishi Vigyan Kendra for guidance"
            )
        }
    }
}

package com.vrikshaayush.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vrikshaayush.R
import com.vrikshaayush.model.DiseaseInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_detail)

        val disease      = intent.getStringExtra("DISEASE") ?: ""
        val crop         = intent.getStringExtra("CROP") ?: ""
        val severity     = intent.getStringExtra("SEVERITY") ?: ""
        val confidence   = intent.getFloatExtra("CONFIDENCE", 0f)
        val aiSuggestion = intent.getStringExtra("AI_SUGGESTION") ?: ""
        val imagePath    = intent.getStringExtra("IMAGE_PATH") ?: ""
        val timestamp    = intent.getLongExtra("TIMESTAMP", 0L)
        val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Image
        val ivImage = findViewById<ImageView>(R.id.ivDetailImage)
        val imgFile = File(imagePath)
        if (imgFile.exists()) Glide.with(this).load(imgFile).centerCrop().into(ivImage)
        else ivImage.setImageResource(R.drawable.ic_leaf_placeholder)

        // Basic info
        findViewById<TextView>(R.id.tvDetailDisease).text    = disease
        findViewById<TextView>(R.id.tvDetailCrop).text       = crop
        findViewById<TextView>(R.id.tvDetailConfidence).text = "Confidence: ${confidence.toInt()}%"
        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        findViewById<TextView>(R.id.tvDetailDate).text = fmt.format(Date(timestamp))

        val tvSeverity = findViewById<TextView>(R.id.tvDetailSeverity)
        tvSeverity.text = severity
        val severityColor = when (severity) {
            "HIGH"    -> ContextCompat.getColor(this, R.color.severity_high)
            "MEDIUM"  -> ContextCompat.getColor(this, R.color.severity_medium)
            else      -> ContextCompat.getColor(this, R.color.severity_low)
        }
        tvSeverity.setBackgroundColor(severityColor)

        // Disease info from diseases.json
        val layoutDisease = findViewById<LinearLayout>(R.id.layoutDiseaseSection)
        val tvDiseaseDesc = findViewById<TextView>(R.id.tvDetailDiseaseDesc)
        val tvDiseaseSymptoms = findViewById<TextView>(R.id.tvDetailSymptoms)
        val tvDiseaseTreatment = findViewById<TextView>(R.id.tvDetailTreatment)

        val diseaseInfo = loadDiseaseInfo(disease, crop)
        if (diseaseInfo != null) {
            layoutDisease.visibility = View.VISIBLE
            val desc = diseaseInfo.description[lang] ?: diseaseInfo.description["en"] ?: ""
            tvDiseaseDesc.text = desc.ifEmpty { "See full details in Disease Library." }

            val symptoms = diseaseInfo.symptoms[lang] ?: diseaseInfo.symptoms["en"] ?: emptyList()
            tvDiseaseSymptoms.text = if (symptoms.isEmpty()) "—" else symptoms.joinToString("\n") { "• $it" }

            val organicTx = diseaseInfo.organic_treatments[lang] ?: diseaseInfo.organic_treatments["en"] ?: emptyList()
            tvDiseaseTreatment.text = if (organicTx.isEmpty()) "—"
            else organicTx.take(3).mapIndexed { i, t -> "${i+1}. ${t.title}\n   ${t.description}" }.joinToString("\n\n")
        } else {
            layoutDisease.visibility = View.GONE
        }

        // AI Suggestion section
        val layoutAi = findViewById<LinearLayout>(R.id.layoutAiSection)
        val tvAi     = findViewById<TextView>(R.id.tvDetailAi)
        if (aiSuggestion.isNotEmpty()) {
            layoutAi.visibility = View.VISIBLE
            tvAi.text = aiSuggestion
        } else {
            layoutAi.visibility = View.GONE
        }
    }

    private fun loadDiseaseInfo(diseaseName: String, cropType: String): DiseaseInfo? {
        return try {
            val json = assets.open("diseases.json").bufferedReader().use { it.readText() }
            val data: Map<String, Any> = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
            val diseasesJson = Gson().toJson(data["diseases"])
            val diseases: List<DiseaseInfo> = Gson().fromJson(diseasesJson, object : TypeToken<List<DiseaseInfo>>() {}.type)

            diseases.find { it.disease_name.equals(diseaseName, ignoreCase = true) }
                ?: diseases.find { diseaseName.contains(it.disease_name, ignoreCase = true) }
                ?: diseases.find { it.crop_type.contains(cropType.split(" ").first(), ignoreCase = true) && it.disease_name.contains(diseaseName.split(" ").first(), ignoreCase = true) }
        } catch (e: Exception) { null }
    }
}

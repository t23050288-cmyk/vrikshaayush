package com.vrikshaayush.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vrikshaayush.R
import com.vrikshaayush.databinding.ActivityDiseaseDetailBinding
import com.vrikshaayush.model.DiseaseInfo

class DiseaseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiseaseDetailBinding
    private var currentDisease: DiseaseInfo? = null
    private var isOrganicSelected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiseaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val diseaseName = intent.getStringExtra("DISEASE_NAME") ?: ""
        val cropType = intent.getStringExtra("CROP_TYPE") ?: ""
        val modelLabel = intent.getStringExtra("MODEL_LABEL") ?: ""

        binding.btnBack.setOnClickListener { finish() }
        binding.tvTitle.text = diseaseName

        loadDiseaseInfo(diseaseName, cropType, modelLabel)
        setupTreatmentTabs()
    }

    private fun loadDiseaseInfo(diseaseName: String, cropType: String, modelLabel: String) {
        val json = assets.open("diseases.json").bufferedReader().use { it.readText() }
        val data: Map<String, Any> = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
        val diseasesJson = Gson().toJson(data["diseases"])
        val diseases: List<DiseaseInfo> = Gson().fromJson(diseasesJson, object : TypeToken<List<DiseaseInfo>>() {}.type)

        // Try multiple matching strategies
        val disease = diseases.find { d ->
            // 1. Match by model_labels list (most accurate)
            d.model_labels?.any { label ->
                label.equals(modelLabel, ignoreCase = true) ||
                modelLabel.contains(label, ignoreCase = true) ||
                label.contains(modelLabel, ignoreCase = true)
            } == true
        } ?: diseases.find { d ->
            // 2. Match by disease name
            d.disease_name.equals(diseaseName, ignoreCase = true) ||
            diseaseName.contains(d.disease_name, ignoreCase = true) ||
            d.disease_name.split(" ").any { word ->
                word.length > 4 && diseaseName.contains(word, ignoreCase = true)
            }
        } ?: diseases.find { d ->
            // 3. Match by crop type
            val cleanCrop = cropType.substringBefore("(").trim()
            d.crop_type.contains(cleanCrop, ignoreCase = true) ||
            cleanCrop.contains(d.crop_type, ignoreCase = true)
        }

        currentDisease = disease

        disease?.let { d ->
            binding.tvDescription.text = d.description["en"] ?: "Information not available"
            binding.tvCommonIn.text = "Common in: ${d.common_in?.joinToString(", ") ?: cropType}"

            val symptoms = d.symptoms["en"] ?: emptyList()
            binding.tvSymptoms.text = symptoms.joinToString("\n") { "• $it" }

            val causes = d.causes["en"] ?: emptyList()
            binding.tvCauses.text = causes.joinToString("  •  ")

            val prevention = d.prevention_tips["en"] ?: emptyList()
            binding.tvPrevention.text = prevention.joinToString("\n") { "✓ $it" }

            showOrganicTreatments(d)
        } ?: run {
            binding.tvDescription.text = "Detailed info for $diseaseName is being added. Consult your local Krishi Vigyan Kendra (KVK) for guidance."
            binding.tvCommonIn.text = "Crop: $cropType"
            binding.tvSymptoms.text = "• Check leaves for discoloration, spots or wilting\n• Look for changes in fruit or stem"
            binding.tvCauses.text = "Fungal, Bacterial or Environmental stress"
            binding.tvPrevention.text = "✓ Rotate crops regularly\n✓ Avoid overhead irrigation\n✓ Maintain good plant spacing"
            binding.tvTreatments.text = "1. Consult KVK\n   Contact your nearest Krishi Vigyan Kendra for specific advice.\n\n2. Neem Oil Spray\n   Apply 2% neem oil solution as general treatment.\n\n3. Copper Spray\n   Apply 0.5% Bordeaux mixture as broad-spectrum fungicide."
        }
    }

    private fun setupTreatmentTabs() {
        // Set initial state
        updateTabVisuals(true)

        binding.tabOrganic.setOnClickListener {
            isOrganicSelected = true
            updateTabVisuals(true)
            currentDisease?.let { showOrganicTreatments(it) }
        }

        binding.tabChemical.setOnClickListener {
            isOrganicSelected = false
            updateTabVisuals(false)
            currentDisease?.let { showChemicalTreatments(it) }
        }
    }

    private fun updateTabVisuals(organicSelected: Boolean) {
        if (organicSelected) {
            binding.tabOrganic.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary_green)
            binding.tabOrganic.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tabChemical.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary_green_light)
            binding.tabChemical.setTextColor(ContextCompat.getColor(this, R.color.primary_green))
        } else {
            binding.tabChemical.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary_green)
            binding.tabChemical.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tabOrganic.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.primary_green_light)
            binding.tabOrganic.setTextColor(ContextCompat.getColor(this, R.color.primary_green))
        }
    }

    private fun showOrganicTreatments(disease: DiseaseInfo) {
        val treatments = disease.organic_treatments["en"] ?: emptyList()
        if (treatments.isEmpty()) {
            binding.tvTreatments.text = "Apply neem oil spray 2% every 7 days as general organic treatment."
            return
        }
        val text = treatments.mapIndexed { i, t ->
            "${i + 1}. ${t.title}\n   ${t.description}"
        }.joinToString("\n\n")
        binding.tvTreatments.text = text
    }

    private fun showChemicalTreatments(disease: DiseaseInfo) {
        val treatments = disease.chemical_treatments["en"] ?: emptyList()
        if (treatments.isEmpty()) {
            binding.tvTreatments.text = "Consult your local agro dealer or KVK for specific chemical recommendations."
            return
        }
        val text = treatments.mapIndexed { i, t ->
            "${i + 1}. ${t.title}\n   ${t.description}"
        }.joinToString("\n\n")
        binding.tvTreatments.text = text
    }
}

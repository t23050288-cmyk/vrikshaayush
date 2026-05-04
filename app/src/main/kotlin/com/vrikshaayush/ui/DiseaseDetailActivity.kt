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

    private fun loadAllDiseases(): List<DiseaseInfo> {
        val json = assets.open("diseases.json").bufferedReader().use { it.readText() }
        val data: Map<String, Any> = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
        val diseasesJson = Gson().toJson(data["diseases"])
        return Gson().fromJson(diseasesJson, object : TypeToken<List<DiseaseInfo>>() {}.type)
    }

    private fun loadDiseaseInfo(diseaseName: String, cropType: String, modelLabel: String) {
        val diseases = loadAllDiseases()

        val disease = when {
            // 1. Best match: by model_label (exact)
            modelLabel.isNotBlank() -> {
                diseases.find { d ->
                    d.model_labels?.any { it.equals(modelLabel, ignoreCase = true) } == true
                }
            }
            else -> null
        } ?: diseases.find { d ->
            // 2. Match by disease_name exact
            d.disease_name.equals(diseaseName, ignoreCase = true)
        } ?: diseases.find { d ->
            // 3. Match by disease_name contains
            diseaseName.isNotBlank() && (
                d.disease_name.contains(diseaseName, ignoreCase = true) ||
                diseaseName.contains(d.disease_name, ignoreCase = true)
            )
        } ?: diseases.find { d ->
            // 4. Match by crop type fallback
            cropType.isNotBlank() && (
                d.crop_type.contains(cropType, ignoreCase = true) ||
                cropType.contains(d.crop_type, ignoreCase = true)
            )
        }

        currentDisease = disease

        disease?.let { d ->
            binding.tvTitle.text = d.disease_name
            binding.tvDescription.text = d.description["en"] ?: "Information not available"
            binding.tvCommonIn.text = "Common in: ${d.common_in?.joinToString(", ") ?: d.crop_type}"

            val symptoms = d.symptoms["en"] ?: emptyList()
            binding.tvSymptoms.text = if (symptoms.isEmpty()) "No symptom data available"
            else symptoms.joinToString("\n") { "• $it" }

            val causes = d.causes["en"] ?: emptyList()
            binding.tvCauses.text = if (causes.isEmpty()) "No cause data available"
            else causes.joinToString("  •  ")

            val prevention = d.prevention_tips["en"] ?: emptyList()
            binding.tvPrevention.text = if (prevention.isEmpty()) "No prevention data available"
            else prevention.joinToString("\n") { "✓ $it" }

            showOrganicTreatments(d)

        } ?: run {
            // Fallback when nothing matches
            binding.tvDescription.text = "Detailed information for \"$diseaseName\" is being added. Please consult your local Krishi Vigyan Kendra (KVK) for guidance."
            binding.tvCommonIn.text = "Crop: $cropType"
            binding.tvSymptoms.text = "• Check leaves for discoloration, spots or wilting\n• Look for changes in fruit or stem appearance"
            binding.tvCauses.text = "Fungal, Bacterial or Environmental stress"
            binding.tvPrevention.text = "✓ Rotate crops regularly\n✓ Avoid overhead irrigation\n✓ Maintain good plant spacing"
            binding.tvTreatments.text = "1. Consult KVK\n   Contact your nearest Krishi Vigyan Kendra for specific advice.\n\n2. Neem Oil Spray\n   Apply 2% neem oil solution as general treatment.\n\n3. Copper Spray\n   Apply 0.5% Bordeaux mixture as broad-spectrum fungicide."
        }
    }

    private fun setupTreatmentTabs() {
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
            binding.tabOrganic.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
            binding.tabOrganic.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tabChemical.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green_light)
            binding.tabChemical.setTextColor(ContextCompat.getColor(this, R.color.primary_green))
        } else {
            binding.tabChemical.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
            binding.tabChemical.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tabOrganic.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green_light)
            binding.tabOrganic.setTextColor(ContextCompat.getColor(this, R.color.primary_green))
        }
    }

    private fun showOrganicTreatments(disease: DiseaseInfo) {
        val treatments = disease.organic_treatments["en"] ?: emptyList()
        binding.tvTreatments.text = if (treatments.isEmpty()) {
            "Apply neem oil spray (2%) every 7 days as a general organic treatment."
        } else {
            treatments.mapIndexed { i, t ->
                "${i + 1}. ${t.title}\n   ${t.description}"
            }.joinToString("\n\n")
        }
    }

    private fun showChemicalTreatments(disease: DiseaseInfo) {
        val treatments = disease.chemical_treatments["en"] ?: emptyList()
        binding.tvTreatments.text = if (treatments.isEmpty()) {
            "Consult your local agro dealer or KVK for specific chemical recommendations."
        } else {
            treatments.mapIndexed { i, t ->
                "${i + 1}. ${t.title}\n   ${t.description}"
            }.joinToString("\n\n")
        }
    }
}

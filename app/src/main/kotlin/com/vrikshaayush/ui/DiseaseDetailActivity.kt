package com.vrikshaayush.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vrikshaayush.databinding.ActivityDiseaseDetailBinding
import com.vrikshaayush.model.DiseaseInfo

class DiseaseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiseaseDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiseaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val diseaseName = intent.getStringExtra("DISEASE_NAME") ?: ""
        val cropType = intent.getStringExtra("CROP_TYPE") ?: ""

        binding.btnBack.setOnClickListener { finish() }
        binding.tvTitle.text = diseaseName

        loadDiseaseInfo(diseaseName, cropType)
        setupTreatmentTabs()
    }

    private fun loadDiseaseInfo(diseaseName: String, cropType: String) {
        val json = assets.open("diseases.json").bufferedReader().use { it.readText() }
        val data: Map<String, Any> = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
        val diseasesJson = Gson().toJson(data["diseases"])
        val diseases: List<DiseaseInfo> = Gson().fromJson(diseasesJson, object : TypeToken<List<DiseaseInfo>>() {}.type)

        val disease = diseases.find {
            it.disease_name.equals(diseaseName, ignoreCase = true) ||
                    it.crop_type.equals(cropType, ignoreCase = true)
        } ?: diseases.firstOrNull()

        disease?.let { d ->
            // What is it
            binding.tvDescription.text = d.description["en"] ?: "Information not available"

            // Common in
            binding.tvCommonIn.text = "Common in: ${d.common_in?.joinToString(", ") ?: cropType}"

            // Symptoms
            val symptoms = d.symptoms["en"] ?: emptyList()
            binding.tvSymptoms.text = symptoms.joinToString("\n") { "• $it" }

            // Causes
            val causes = d.causes["en"] ?: emptyList()
            binding.tvCauses.text = causes.joinToString("  •  ")

            // Prevention
            val prevention = d.prevention_tips["en"] ?: emptyList()
            binding.tvPrevention.text = prevention.joinToString("\n") { "✓ $it" }

            // Treatments
            showOrganicTreatments(d)
        }
    }

    private fun setupTreatmentTabs() {
        binding.tabOrganic.setOnClickListener {
            binding.tabOrganic.isSelected = true
            binding.tabChemical.isSelected = false
            // reload organic
        }
        binding.tabChemical.setOnClickListener {
            binding.tabChemical.isSelected = true
            binding.tabOrganic.isSelected = false
            // reload chemical
        }
    }

    private fun showOrganicTreatments(disease: DiseaseInfo) {
        val treatments = disease.organic_treatments["en"] ?: emptyList()
        val text = treatments.mapIndexed { i, t ->
            "${i + 1}. ${t.title}\n   ${t.description}"
        }.joinToString("\n\n")
        binding.tvTreatments.text = text
    }
}

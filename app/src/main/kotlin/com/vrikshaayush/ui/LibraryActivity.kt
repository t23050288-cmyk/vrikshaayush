package com.vrikshaayush.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vrikshaayush.databinding.ActivityLibraryBinding
import com.vrikshaayush.model.DiseaseInfo
import com.vrikshaayush.ui.adapter.DiseaseLibraryAdapter

class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding
    private lateinit var adapter: DiseaseLibraryAdapter
    private var allDiseases: List<DiseaseInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = DiseaseLibraryAdapter { disease ->
            val intent = Intent(this, DiseaseDetailActivity::class.java)
            intent.putExtra("DISEASE_NAME", disease.disease_name)
            intent.putExtra("CROP_TYPE", disease.crop_type)
            // Pass the first model_label so DiseaseDetailActivity can match perfectly
            intent.putExtra("MODEL_LABEL", disease.model_labels?.firstOrNull() ?: "")
            startActivity(intent)
        }

        binding.rvDiseases.layoutManager = GridLayoutManager(this, 2)
        binding.rvDiseases.adapter = adapter

        loadDiseases()
        setupFilters()
        setupSearch()
    }

    private fun loadDiseases() {
        val json = assets.open("diseases.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = Gson().fromJson(json, type)

        val diseasesJson = Gson().toJson(data["diseases"])
        val diseasesType = object : TypeToken<List<DiseaseInfo>>() {}.type
        allDiseases = Gson().fromJson(diseasesJson, diseasesType)

        adapter.submitList(allDiseases)
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener { filterByCrop("All") }
        binding.chipTomato.setOnClickListener { filterByCrop("Tomato") }
        binding.chipRice.setOnClickListener { filterByCrop("Rice") }
        binding.chipWheat.setOnClickListener { filterByCrop("Wheat") }
        binding.chipCotton.setOnClickListener { filterByCrop("Cotton") }
        binding.chipMaize.setOnClickListener { filterByCrop("Maize") }
    }

    // Match crop_type loosely — handles "Tamatar (Tomato)", "Chawal (Rice)", etc.
    private fun filterByCrop(crop: String) {
        val filtered = if (crop == "All") allDiseases
        else allDiseases.filter { it.crop_type.contains(crop, ignoreCase = true) }
        adapter.submitList(filtered)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = allDiseases.filter {
                    it.disease_name.lowercase().contains(query) ||
                            it.crop_type.lowercase().contains(query)
                }
                adapter.submitList(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}

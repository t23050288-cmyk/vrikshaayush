package com.vrikshaayush.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.vrikshaayush.R
import com.vrikshaayush.data.AppDatabase
import com.vrikshaayush.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)
        loadStats()
        setupNavigation()
        showWeatherTip()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
        showFieldReport()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val totalScans    = db.scanDao().getTotalScans()
            val totalDiseases = db.scanDao().getTotalDiseases()
            val totalCrops    = db.scanDao().getTotalCrops()
            val lastScan      = db.scanDao().getLastScan()

            binding.tvTotalScans.text    = totalScans.toString()
            binding.tvTotalDiseases.text = totalDiseases.toString()
            binding.tvTotalCrops.text    = totalCrops.toString()

            if (lastScan != null) {
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                binding.tvLastAudit.text = "${lastScan.cropType} • ${sdf.format(Date(lastScan.timestamp))}"
                binding.cardLastAudit.visibility = android.view.View.VISIBLE
            } else {
                binding.cardLastAudit.visibility = android.view.View.GONE
            }
        }
    }

    private fun showWeatherTip() {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val (icon, tipResId) = when (month) {
            6, 7, 8 -> "🌧️" to R.string.season_monsoon
            9, 10   -> "🍂" to R.string.season_postmonsoon
            11, 12  -> "❄️" to R.string.season_winter
            1, 2    -> "🌾" to R.string.season_rabi
            3, 4, 5 -> "☀️" to R.string.season_summer
            else    -> "🌿" to R.string.season_general
        }
        binding.tvWeatherIcon.text = icon
        binding.tvWeatherTip.text  = getString(tipResId)
        binding.cardWeatherTip.visibility = android.view.View.VISIBLE
    }

    private fun showFieldReport() {
        lifecycleScope.launch {
            val recentScans = db.scanDao().getRecentScans(30)
            if (recentScans.size < 2) {
                binding.cardFieldReport.visibility = android.view.View.GONE
                return@launch
            }
            val topCrop = recentScans
                .filter { !it.diseaseName.contains("Healthy", ignoreCase = true) }
                .groupBy { it.cropType }
                .maxByOrNull { it.value.size }?.key ?: "—"
            val topDisease = recentScans
                .filter { !it.diseaseName.contains("Healthy", ignoreCase = true) && !it.diseaseName.contains("Cannot", ignoreCase = true) }
                .groupBy { it.diseaseName }
                .maxByOrNull { it.value.size }?.key ?: "—"
            val diseaseCount = recentScans.filter { !it.diseaseName.contains("Healthy", ignoreCase = true) }.size
            val healthyCount = recentScans.filter { it.diseaseName.contains("Healthy", ignoreCase = true) }.size

            binding.tvFieldTopCrop.text    = topCrop
            binding.tvFieldTopDisease.text = topDisease
            binding.tvFieldSickCount.text  = "$diseaseCount ${getString(R.string.diseases_label)} / $healthyCount healthy in last ${recentScans.size} scans"
            binding.cardFieldReport.visibility = android.view.View.VISIBLE
        }
    }

    private fun setupNavigation() {
        binding.btnScanPlant.setOnClickListener { startActivity(Intent(this, ScannerActivity::class.java)) }
        binding.fabScan.setOnClickListener      { startActivity(Intent(this, ScannerActivity::class.java)) }
        binding.navHistory.setOnClickListener   { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.navLibrary.setOnClickListener   { startActivity(Intent(this, LibraryActivity::class.java)) }
        binding.navSettings.setOnClickListener  { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.cardLastAudit.setOnClickListener{ startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.cardAiChat.setOnClickListener   { startActivity(Intent(this, AiChatActivity::class.java)) }
        binding.cardFieldReport.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.cardCropCalendar.setOnClickListener { startActivity(Intent(this, CropCalendarActivity::class.java)) }
    }
}

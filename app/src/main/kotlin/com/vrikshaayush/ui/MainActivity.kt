package com.vrikshaayush.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vrikshaayush.data.AppDatabase
import com.vrikshaayush.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
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

    private fun applyLocale() {
        val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        applicationContext.resources.updateConfiguration(config, applicationContext.resources.displayMetrics)
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

    /**
     * Weather/Season tip — based on current month in India
     * No internet needed — pure calendar-based logic
     */
    private fun showWeatherTip() {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-12
        val (icon, tip) = when (month) {
            6, 7, 8 -> "🌧️" to "Monsoon season — High humidity increases risk of fungal diseases on Tomato, Potato & Rice. Check leaves daily."
            9, 10   -> "🍂" to "Post-monsoon — Watch for Late Blight on Tomato & Potato. Avoid overhead watering now."
            11, 12  -> "❄️" to "Winter — Powdery Mildew risk is high. Spray sulfur-based fungicide on Wheat & Grapes preventively."
            1, 2    -> "🌾" to "Rabi season — Check Wheat for Rust disease early. Yellow spots on leaves = act immediately."
            3, 4, 5 -> "☀️" to "Summer — High temperature increases pest attacks. Scout fields in early morning for signs."
            else    -> "🌿" to "Scan your plants regularly for early disease detection — early action saves crops!"
        }
        binding.tvWeatherIcon.text = icon
        binding.tvWeatherTip.text = tip
        binding.cardWeatherTip.visibility = android.view.View.VISIBLE
    }

    /**
     * Field Report — shows top affected crop & most common disease from scan history
     */
    private fun showFieldReport() {
        lifecycleScope.launch {
            val recentScans = db.scanDao().getRecentScans(30) // last 30 records
            if (recentScans.size < 2) {
                binding.cardFieldReport.visibility = android.view.View.GONE
                return@launch
            }
            // Most common crop
            val topCrop = recentScans
                .filter { !it.diseaseName.contains("Healthy", ignoreCase = true) }
                .groupBy { it.cropType }
                .maxByOrNull { it.value.size }
                ?.key ?: "—"
            // Most common disease
            val topDisease = recentScans
                .filter { !it.diseaseName.contains("Healthy", ignoreCase = true) && !it.diseaseName.contains("Cannot", ignoreCase = true) }
                .groupBy { it.diseaseName }
                .maxByOrNull { it.value.size }
                ?.key ?: "—"
            val diseaseCount = recentScans.filter { !it.diseaseName.contains("Healthy", ignoreCase = true) }.size
            val healthyCount = recentScans.filter { it.diseaseName.contains("Healthy", ignoreCase = true) }.size

            binding.tvFieldTopCrop.text = topCrop
            binding.tvFieldTopDisease.text = topDisease
            binding.tvFieldSickCount.text = "$diseaseCount diseases / $healthyCount healthy in last ${recentScans.size} scans"
            binding.cardFieldReport.visibility = android.view.View.VISIBLE
        }
    }

    private fun setupNavigation() {
        binding.btnScanPlant.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }
        binding.fabScan.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }
        binding.navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.navLibrary.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.cardLastAudit.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.cardAiChat.setOnClickListener {
            startActivity(Intent(this, AiChatActivity::class.java))
        }
        binding.cardFieldReport.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.cardCropCalendar.setOnClickListener {
            startActivity(Intent(this, CropCalendarActivity::class.java))
        }
    }
}

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
import java.util.Date
import java.util.Locale

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
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun applyLocale() {
        val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val totalScans = db.scanDao().getTotalScans()
            val totalDiseases = db.scanDao().getTotalDiseases()
            val totalCrops = db.scanDao().getTotalCrops()
            val lastScan = db.scanDao().getLastScan()

            binding.tvTotalScans.text = totalScans.toString()
            binding.tvTotalDiseases.text = totalDiseases.toString()
            binding.tvTotalCrops.text = totalCrops.toString()

            if (lastScan != null) {
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                val timeStr = sdf.format(Date(lastScan.timestamp))
                binding.tvLastAudit.text = "${lastScan.cropType} • $timeStr"
                binding.cardLastAudit.visibility = android.view.View.VISIBLE
            } else {
                binding.cardLastAudit.visibility = android.view.View.GONE
            }
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
    }
}


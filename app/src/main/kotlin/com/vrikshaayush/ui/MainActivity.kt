package com.vrikshaayush.ui

import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
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
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        setupConnectivityMonitor()
        loadStats()
        setupNavigation()
        showWeatherTip()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
        showFieldReport()
        updateConnectivityUI(isOnline())
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
    }

    // ── Connectivity ──────────────────────────────────────
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setupConnectivityMonitor() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { updateConnectivityUI(true) }
            }
            override fun onLost(network: Network) {
                runOnUiThread { updateConnectivityUI(false) }
            }
        }
        networkCallback = callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        updateConnectivityUI(isOnline())
    }

    private fun updateConnectivityUI(online: Boolean) {
        if (online) {
            binding.tvConnectivityLabel.text = getString(R.string.online_badge)
            binding.dotConnectivity.setBackgroundResource(R.drawable.circle_dot_online)
            binding.layoutConnectivity.setBackgroundResource(R.drawable.bg_online_badge)
        } else {
            binding.tvConnectivityLabel.text = getString(R.string.offline_badge)
            binding.dotConnectivity.setBackgroundResource(R.drawable.circle_dot_offline)
            binding.layoutConnectivity.setBackgroundResource(R.drawable.bg_offline_badge)
        }
    }

    // ── Stats ──────────────────────────────────────────────
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
                binding.cardLastAudit.visibility = View.VISIBLE
            } else {
                binding.cardLastAudit.visibility = View.GONE
            }
        }
    }

    // ── Season tip ────────────────────────────────────────
    private fun showWeatherTip() {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val (icon, tipResId) = when (month) {
            6, 7, 8 -> "🌧" to R.string.season_monsoon
            9, 10   -> "🍂" to R.string.season_postmonsoon
            11, 12  -> "❄" to R.string.season_winter
            1, 2    -> "🌾" to R.string.season_rabi
            3, 4, 5 -> "☀" to R.string.season_summer
            else    -> "🌿" to R.string.season_general
        }
        binding.tvWeatherIcon.text = icon
        binding.tvWeatherTip.text  = getString(tipResId)
        binding.cardWeatherTip.visibility = View.VISIBLE
    }

    // ── Field report ──────────────────────────────────────
    private fun showFieldReport() {
        lifecycleScope.launch {
            val recentScans = db.scanDao().getRecentScans(30)
            if (recentScans.size < 2) {
                binding.cardFieldReport.visibility = View.GONE
                return@launch
            }
            val topCrop = recentScans
                .filter { !it.diseaseName.contains("Healthy", ignoreCase = true) }
                .groupBy { it.cropType }.maxByOrNull { it.value.size }?.key ?: "—"
            val topDisease = recentScans
                .filter { !it.diseaseName.contains("Healthy", ignoreCase = true) && !it.diseaseName.contains("Cannot", ignoreCase = true) }
                .groupBy { it.diseaseName }.maxByOrNull { it.value.size }?.key ?: "—"
            val diseaseCount = recentScans.filter { !it.diseaseName.contains("Healthy", ignoreCase = true) }.size
            val healthyCount = recentScans.filter { it.diseaseName.contains("Healthy", ignoreCase = true) }.size

            binding.tvFieldTopCrop.text    = topCrop
            binding.tvFieldTopDisease.text = topDisease
            binding.tvFieldSickCount.text  = "$diseaseCount ${getString(R.string.diseases_label)} / $healthyCount healthy in last ${recentScans.size} scans"
            binding.cardFieldReport.visibility = View.VISIBLE
        }
    }

    // ── Navigation ────────────────────────────────────────
    private fun setupNavigation() {
        binding.btnScanPlant.setOnClickListener    { startActivity(Intent(this, ScannerActivity::class.java)) }
        binding.fabScan.setOnClickListener         { startActivity(Intent(this, ScannerActivity::class.java)) }
        binding.navHistory.setOnClickListener      { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.navLibrary.setOnClickListener      { startActivity(Intent(this, LibraryActivity::class.java)) }
        binding.navSettings.setOnClickListener     { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.cardLastAudit.setOnClickListener   { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.cardAiChat.setOnClickListener      { startActivity(Intent(this, AiChatActivity::class.java)) }
        binding.cardFieldReport.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.cardCropCalendar.setOnClickListener{ startActivity(Intent(this, CropCalendarActivity::class.java)) }
    }
}

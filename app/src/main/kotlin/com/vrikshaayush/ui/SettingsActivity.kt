package com.vrikshaayush.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vrikshaayush.data.AppDatabase
import com.vrikshaayush.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        binding.btnBack.setOnClickListener { finish() }

        // Highlight current language
        val currentLang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
        updateButtonStates(currentLang)

        // Language selection - actually applies locale and restarts
        binding.btnEnglish.setOnClickListener { applyLanguage("en", "English") }
        binding.btnHindi.setOnClickListener { applyLanguage("hi", "हिंदी") }
        binding.btnKannada.setOnClickListener { applyLanguage("kn", "ಕನ್ನಡ") }

        // Clear history
        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Scan History")
                .setMessage("This will permanently delete all your scan records. Are you sure?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.scanDao().deleteAll()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsActivity, "Scan history cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.tvAppVersion.text = "v1.0.0"
    }

    private fun applyLanguage(lang: String, langName: String) {
        // Save preference
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("language", lang)
            .apply()

        // Apply locale immediately
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        applicationContext.resources.updateConfiguration(config, resources.displayMetrics)

        Toast.makeText(this, "Language changed to $langName", Toast.LENGTH_SHORT).show()

        // Restart app from SplashActivity so all screens reload with new language
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateButtonStates(lang: String) {
        binding.btnEnglish.isSelected = lang == "en"
        binding.btnHindi.isSelected = lang == "hi"
        binding.btnKannada.isSelected = lang == "kn"
    }
}


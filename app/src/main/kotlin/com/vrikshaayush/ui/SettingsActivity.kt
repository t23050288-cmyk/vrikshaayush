package com.vrikshaayush.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vrikshaayush.data.AppDatabase
import com.vrikshaayush.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        binding.btnBack.setOnClickListener { finish() }

        // Language selection
        binding.btnEnglish.setOnClickListener { selectLanguage("en") }
        binding.btnHindi.setOnClickListener { selectLanguage("hi") }
        binding.btnKannada.setOnClickListener { selectLanguage("kn") }

        // Clear history
        binding.btnClearHistory.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Clear Scan History")
                .setMessage("This will permanently delete all your scan records. Are you sure?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.scanDao().deleteAll()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@SettingsActivity,
                                "Scan history cleared",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // App version
        binding.tvAppVersion.text = "v1.0.0"
    }

    private fun selectLanguage(lang: String) {
        // Save language preference
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("language", lang)
            .apply()

        val langName = when (lang) {
            "hi" -> "Hindi"
            "kn" -> "Kannada"
            else -> "English"
        }
        Toast.makeText(this, "Language set to $langName", Toast.LENGTH_SHORT).show()

        // Update button states
        binding.btnEnglish.isSelected = lang == "en"
        binding.btnHindi.isSelected = lang == "hi"
        binding.btnKannada.isSelected = lang == "kn"
    }
}

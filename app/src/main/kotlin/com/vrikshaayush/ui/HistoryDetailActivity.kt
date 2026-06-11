package com.vrikshaayush.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.vrikshaayush.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_detail)

        val disease      = intent.getStringExtra("DISEASE") ?: ""
        val crop         = intent.getStringExtra("CROP") ?: ""
        val severity     = intent.getStringExtra("SEVERITY") ?: ""
        val confidence   = intent.getFloatExtra("CONFIDENCE", 0f)
        val aiSuggestion = intent.getStringExtra("AI_SUGGESTION") ?: ""
        val imagePath    = intent.getStringExtra("IMAGE_PATH") ?: ""
        val timestamp    = intent.getLongExtra("TIMESTAMP", 0L)

        // Toolbar back
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Image
        val ivImage = findViewById<ImageView>(R.id.ivDetailImage)
        val imgFile = File(imagePath)
        if (imgFile.exists()) Glide.with(this).load(imgFile).centerCrop().into(ivImage)

        // Basic info
        findViewById<TextView>(R.id.tvDetailDisease).text    = disease
        findViewById<TextView>(R.id.tvDetailCrop).text       = crop
        findViewById<TextView>(R.id.tvDetailConfidence).text = "${confidence.toInt()}%"
        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        findViewById<TextView>(R.id.tvDetailDate).text       = fmt.format(Date(timestamp))

        val tvSeverity = findViewById<TextView>(R.id.tvDetailSeverity)
        tvSeverity.text = severity
        val severityColor = when (severity) {
            "HIGH"    -> ContextCompat.getColor(this, R.color.severity_high)
            "MEDIUM"  -> ContextCompat.getColor(this, R.color.severity_medium)
            "HEALTHY" -> ContextCompat.getColor(this, R.color.severity_low)
            else      -> ContextCompat.getColor(this, R.color.severity_low)
        }
        tvSeverity.setBackgroundColor(severityColor)

        // AI Suggestion section
        val layoutAi = findViewById<LinearLayout>(R.id.layoutAiSection)
        val tvAi     = findViewById<TextView>(R.id.tvDetailAi)
        if (aiSuggestion.isNotEmpty()) {
            layoutAi.visibility = View.VISIBLE
            tvAi.text = aiSuggestion
        } else {
            layoutAi.visibility = View.GONE
        }
    }

    private fun applyLocale() {
        val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

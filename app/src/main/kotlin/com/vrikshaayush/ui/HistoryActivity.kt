package com.vrikshaayush.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vrikshaayush.R
import com.vrikshaayush.data.AppDatabase
import com.vrikshaayush.data.ScanRecord
import com.vrikshaayush.databinding.ActivityHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase
    private var showingAi = false   // false = Plant Disease tab, true = AI Suggestions tab

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        binding.btnBack.setOnClickListener { finish() }

        // Tab buttons
        binding.btnTabDisease.setOnClickListener { switchTab(false) }
        binding.btnTabAi.setOnClickListener     { switchTab(true)  }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)

        // Search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { loadData(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.fabScan.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        switchTab(false)
    }

    private fun applyLocale() {
        val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun switchTab(aiTab: Boolean) {
        showingAi = aiTab
        // Visual highlight
        val activeColor  = resources.getColor(R.color.primary_green, null)
        val inactiveColor = resources.getColor(R.color.text_secondary, null)
        binding.btnTabDisease.setTextColor(if (!aiTab) activeColor else inactiveColor)
        binding.btnTabAi.setTextColor(if (aiTab) activeColor else inactiveColor)
        binding.viewTabDisease.visibility = if (!aiTab) View.VISIBLE else View.INVISIBLE
        binding.viewTabAi.visibility      = if (aiTab) View.VISIBLE else View.INVISIBLE
        loadData(binding.etSearch.text.toString())
    }

    private fun loadData(query: String) {
        val liveData = if (query.isEmpty())
            db.scanDao().getAllScans()
        else
            db.scanDao().searchScans(query)

        liveData.observe(this, Observer { scans ->
            val filtered = if (showingAi)
                scans.filter { it.aiSuggestion.isNotEmpty() }
            else
                scans

            if (filtered.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvHistory.visibility   = View.GONE
                binding.tvEmptyMsg.text = if (showingAi)
                    "No AI suggestions saved yet.\n\nAsk AI about a disease after diagnosis,\nthen tap 'Save to History'."
                else
                    "No scans yet.\nScan your first plant to see results here."
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvHistory.visibility   = View.VISIBLE
                binding.rvHistory.adapter = HistoryAdapter(filtered, showingAi)
            }
        })
    }

    // ── Inner Adapter ──────────────────────────────────────────────────────────
    inner class HistoryAdapter(
        private val items: List<ScanRecord>,
        private val isAiTab: Boolean
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val ivImage:     ImageView = view.findViewById(R.id.ivHistoryImage)
            val tvDisease:   TextView  = view.findViewById(R.id.tvHistoryDisease)
            val tvCrop:      TextView  = view.findViewById(R.id.tvHistoryCrop)
            val tvDate:      TextView  = view.findViewById(R.id.tvHistoryDate)
            val tvSeverity:  TextView  = view.findViewById(R.id.tvHistorySeverity)
            val tvAiPreview: TextView  = view.findViewById(R.id.tvAiPreview)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_scan_history, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val record = items[position]
            val fmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

            holder.tvDisease.text  = record.diseaseName
            holder.tvCrop.text     = record.cropType
            holder.tvDate.text     = fmt.format(Date(record.timestamp))
            holder.tvSeverity.text = record.severity

            val severityColor = when (record.severity) {
                "HIGH"    -> resources.getColor(R.color.severity_high, null)
                "MEDIUM"  -> resources.getColor(R.color.severity_medium, null)
                "HEALTHY" -> resources.getColor(R.color.severity_low, null)
                else      -> resources.getColor(R.color.severity_low, null)
            }
            holder.tvSeverity.setTextColor(severityColor)

            // Load image
            val imgFile = File(record.imagePath)
            if (imgFile.exists()) {
                Glide.with(holder.ivImage.context).load(imgFile).centerCrop().into(holder.ivImage)
            }

            // Show AI preview in AI tab
            if (isAiTab && record.aiSuggestion.isNotEmpty()) {
                holder.tvAiPreview.visibility = View.VISIBLE
                holder.tvAiPreview.text = "\uD83E\uDD16 " + record.aiSuggestion.take(120) + if (record.aiSuggestion.length > 120) "…" else ""
            } else {
                holder.tvAiPreview.visibility = View.GONE
            }

            // Click opens full detail
            holder.itemView.setOnClickListener {
                val intent = Intent(this@HistoryActivity, HistoryDetailActivity::class.java)
                intent.putExtra("RECORD_ID", record.id)
                intent.putExtra("DISEASE",   record.diseaseName)
                intent.putExtra("CROP",      record.cropType)
                intent.putExtra("SEVERITY",  record.severity)
                intent.putExtra("CONFIDENCE",record.confidence)
                intent.putExtra("AI_SUGGESTION", record.aiSuggestion)
                intent.putExtra("IMAGE_PATH",record.imagePath)
                intent.putExtra("TIMESTAMP", record.timestamp)
                startActivity(intent)
            }
        }
    }
}

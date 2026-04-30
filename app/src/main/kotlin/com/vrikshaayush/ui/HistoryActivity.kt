package com.vrikshaayush.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.vrikshaayush.data.AppDatabase
import com.vrikshaayush.databinding.ActivityHistoryBinding
import com.vrikshaayush.ui.adapter.ScanHistoryAdapter

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: ScanHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        adapter = ScanHistoryAdapter()

        binding.btnBack.setOnClickListener { finish() }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        // Observe all scans
        db.scanDao().getAllScans().observe(this, Observer { scans ->
            adapter.submitList(scans)
            if (scans.isEmpty()) {
                binding.layoutEmpty.visibility = android.view.View.VISIBLE
                binding.rvHistory.visibility = android.view.View.GONE
            } else {
                binding.layoutEmpty.visibility = android.view.View.GONE
                binding.rvHistory.visibility = android.view.View.VISIBLE
            }
        })

        // Search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isEmpty()) {
                    db.scanDao().getAllScans().observe(this@HistoryActivity, Observer {
                        adapter.submitList(it)
                    })
                } else {
                    db.scanDao().searchScans(query).observe(this@HistoryActivity, Observer {
                        adapter.submitList(it)
                    })
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // FAB — go to scanner
        binding.fabScan.setOnClickListener {
            startActivity(android.content.Intent(this, ScannerActivity::class.java))
        }
    }
}

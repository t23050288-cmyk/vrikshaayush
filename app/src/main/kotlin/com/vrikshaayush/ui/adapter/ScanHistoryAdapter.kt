package com.vrikshaayush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vrikshaayush.R
import com.vrikshaayush.data.ScanRecord
import com.vrikshaayush.databinding.ItemScanHistoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScanHistoryAdapter : ListAdapter<ScanRecord, ScanHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemScanHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: ScanRecord) {
            binding.tvDiseaseName.text = scan.diseaseName
            binding.tvCropType.text = scan.cropType

            val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            binding.tvTimestamp.text = sdf.format(Date(scan.timestamp))

            binding.tvSeverity.text = scan.severity

            val context = binding.root.context
            val severityColor = when (scan.severity) {
                "HIGH" -> ContextCompat.getColor(context, R.color.severity_high)
                "MEDIUM" -> ContextCompat.getColor(context, R.color.severity_medium)
                else -> ContextCompat.getColor(context, R.color.severity_low)
            }
            binding.viewSeverityDot.setBackgroundColor(severityColor)
            binding.tvSeverity.setTextColor(severityColor)

            val imageFile = File(scan.imagePath)
            if (imageFile.exists()) {
                Glide.with(context)
                    .load(imageFile)
                    .centerCrop()
                    .placeholder(R.drawable.ic_leaf_placeholder)
                    .into(binding.ivThumbnail)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScanRecord>() {
        override fun areItemsTheSame(oldItem: ScanRecord, newItem: ScanRecord) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScanRecord, newItem: ScanRecord) =
            oldItem == newItem
    }
}

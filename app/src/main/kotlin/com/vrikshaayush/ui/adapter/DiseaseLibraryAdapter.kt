package com.vrikshaayush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vrikshaayush.R
import com.vrikshaayush.databinding.ItemDiseaseLibraryBinding
import com.vrikshaayush.model.DiseaseInfo

class DiseaseLibraryAdapter(
    private val onClick: (DiseaseInfo) -> Unit
) : ListAdapter<DiseaseInfo, DiseaseLibraryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiseaseLibraryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDiseaseLibraryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(disease: DiseaseInfo) {
            binding.tvDiseaseName.text = disease.disease_name
            binding.tvCropType.text = disease.crop_type
            binding.tvSeverity.text = disease.severity_default

            val context = binding.root.context
            val severityColor = when (disease.severity_default) {
                "HIGH" -> ContextCompat.getColor(context, R.color.severity_high)
                "MEDIUM" -> ContextCompat.getColor(context, R.color.severity_medium)
                else -> ContextCompat.getColor(context, R.color.severity_low)
            }
            binding.tvSeverity.setBackgroundColor(severityColor)

            // Crop emoji icon
            val icon = when {
                disease.crop_type.contains("Tomato", ignoreCase = true) -> "🍅"
                disease.crop_type.contains("Rice", ignoreCase = true) -> "🌾"
                disease.crop_type.contains("Wheat", ignoreCase = true) -> "🌿"
                disease.crop_type.contains("Maize", ignoreCase = true) ||
                        disease.crop_type.contains("Corn", ignoreCase = true) -> "🌽"
                disease.crop_type.contains("Cotton", ignoreCase = true) -> "☁"
                disease.crop_type.contains("Potato", ignoreCase = true) -> "🥔"
                disease.crop_type.contains("Apple", ignoreCase = true) -> "🍎"
                else -> "🌱"
            }
            binding.tvCropIcon.text = icon

            binding.root.setOnClickListener { onClick(disease) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DiseaseInfo>() {
        override fun areItemsTheSame(oldItem: DiseaseInfo, newItem: DiseaseInfo) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DiseaseInfo, newItem: DiseaseInfo) =
            oldItem == newItem
    }
}

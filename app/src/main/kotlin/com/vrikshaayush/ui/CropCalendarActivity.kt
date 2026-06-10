package com.vrikshaayush.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import android.widget.TextView
import com.vrikshaayush.R
import com.vrikshaayush.databinding.ActivityCropCalendarBinding
import java.util.*

data class CalendarEntry(
    val season: String,
    val months: String,
    val crops: String,
    val diseases: String,
    val warning: String,
    val color: Int
)

class CropCalendarAdapter(private val items: List<CalendarEntry>) :
    RecyclerView.Adapter<CropCalendarAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvSeason: TextView = view.findViewById(R.id.tvSeason)
        val tvMonths: TextView = view.findViewById(R.id.tvMonths)
        val tvCrops: TextView = view.findViewById(R.id.tvCrops)
        val tvDiseases: TextView = view.findViewById(R.id.tvDiseases)
        val tvWarning: TextView = view.findViewById(R.id.tvWarning)
        val colorBar: View = view.findViewById(R.id.colorBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_entry, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvSeason.text = item.season
        holder.tvMonths.text = item.months
        holder.tvCrops.text = "🌾 Crops: ${item.crops}"
        holder.tvDiseases.text = "🦠 Watch for: ${item.diseases}"
        holder.tvWarning.text = "⚠️ ${item.warning}"
        holder.colorBar.setBackgroundColor(item.color)
    }
}

class CropCalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropCalendarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
        super.onCreate(savedInstanceState)
        binding = ActivityCropCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        val entries = listOf(
            CalendarEntry(
                "☀️ Zaid / Summer",
                "March — May",
                "Watermelon, Cucumber, Pumpkin, Muskmelon",
                "Downy Mildew, Mosaic Virus, Powdery Mildew",
                "High temperature increases pest attack. Scout fields early morning.",
                getColor(R.color.accent_amber)
            ),
            CalendarEntry(
                "🌧️ Kharif / Monsoon",
                "June — September",
                "Rice, Maize, Soybean, Cotton, Groundnut",
                "Blast (Rice), Grey Leaf Spot (Maize), Bacterial Blight (Cotton)",
                "Monsoon humidity creates ideal conditions for fungal diseases. Spray preventively.",
                getColor(R.color.primary_green)
            ),
            CalendarEntry(
                "🍂 Post-Monsoon",
                "October — November",
                "Tomato, Potato, Onion, Chillies",
                "Late Blight (Tomato/Potato), Purple Blotch (Onion)",
                "Late Blight can destroy 100% of potato crop in 2 weeks. Act fast.",
                getColor(R.color.severity_medium)
            ),
            CalendarEntry(
                "❄️ Rabi / Winter",
                "November — February",
                "Wheat, Barley, Mustard, Chickpea, Peas",
                "Yellow Rust (Wheat), Powdery Mildew (Pea/Mustard), Stem Rot",
                "Yellow rust spreads fast in cool humid winters. Check weekly.",
                getColor(R.color.severity_high)
            )
        )

        // Highlight current season
        val currentSeason = when (currentMonth) {
            3, 4, 5 -> 0
            6, 7, 8, 9 -> 1
            10, 11 -> 2
            else -> 3
        }

        binding.tvCurrentSeason.text = "📍 You are currently in: ${entries[currentSeason].season}"

        val adapter = CropCalendarAdapter(entries)
        binding.rvCalendar.layoutManager = LinearLayoutManager(this)
        binding.rvCalendar.adapter = adapter
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

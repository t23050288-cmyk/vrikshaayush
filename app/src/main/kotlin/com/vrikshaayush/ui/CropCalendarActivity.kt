package com.vrikshaayush.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import android.widget.TextView
import com.vrikshaayush.R
import com.vrikshaayush.databinding.ActivityCropCalendarBinding
import java.util.*

data class CalendarEntry(
    val season: String, val months: String, val crops: String,
    val diseases: String, val warning: String, val color: Int
)

class CropCalendarAdapter(private val items: List<CalendarEntry>) :
    RecyclerView.Adapter<CropCalendarAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvSeason:   TextView = view.findViewById(R.id.tvSeason)
        val tvMonths:   TextView = view.findViewById(R.id.tvMonths)
        val tvCrops:    TextView = view.findViewById(R.id.tvCrops)
        val tvDiseases: TextView = view.findViewById(R.id.tvDiseases)
        val tvWarning:  TextView = view.findViewById(R.id.tvWarning)
        val colorBar:   View     = view.findViewById(R.id.colorBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_entry, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvSeason.text   = item.season
        holder.tvMonths.text   = item.months
        holder.tvCrops.text    = "🌾 ${holder.itemView.context.getString(R.string.crops_label)}: ${item.crops}"
        holder.tvDiseases.text = "🦠 ${holder.itemView.context.getString(R.string.watch_for)}: ${item.diseases}"
        holder.tvWarning.text  = "⚠️ ${item.warning}"
        holder.colorBar.setBackgroundColor(item.color)
    }
}

class CropCalendarActivity : BaseActivity() {

    private lateinit var binding: ActivityCropCalendarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        val entries = listOf(
            CalendarEntry(
                getString(R.string.cal_summer_season), getString(R.string.cal_summer_months),
                getString(R.string.cal_summer_crops),  getString(R.string.cal_summer_diseases),
                getString(R.string.cal_summer_warning), getColor(R.color.accent_amber)
            ),
            CalendarEntry(
                getString(R.string.cal_kharif_season), getString(R.string.cal_kharif_months),
                getString(R.string.cal_kharif_crops),  getString(R.string.cal_kharif_diseases),
                getString(R.string.cal_kharif_warning), getColor(R.color.primary_green)
            ),
            CalendarEntry(
                getString(R.string.cal_postmonsoon_season), getString(R.string.cal_postmonsoon_months),
                getString(R.string.cal_postmonsoon_crops),  getString(R.string.cal_postmonsoon_diseases),
                getString(R.string.cal_postmonsoon_warning), getColor(R.color.severity_medium)
            ),
            CalendarEntry(
                getString(R.string.cal_rabi_season), getString(R.string.cal_rabi_months),
                getString(R.string.cal_rabi_crops),  getString(R.string.cal_rabi_diseases),
                getString(R.string.cal_rabi_warning), getColor(R.color.severity_high)
            )
        )

        val currentSeason = when (currentMonth) { 3, 4, 5 -> 0; 6, 7, 8, 9 -> 1; 10, 11 -> 2; else -> 3 }
        binding.tvCurrentSeason.text = "${getString(R.string.you_are_in_season)} ${entries[currentSeason].season}"

        binding.rvCalendar.layoutManager = LinearLayoutManager(this)
        binding.rvCalendar.adapter = CropCalendarAdapter(entries)
    }
}

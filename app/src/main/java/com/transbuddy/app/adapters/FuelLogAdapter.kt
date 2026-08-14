package com.transbuddy.app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.transbuddy.app.R
import com.transbuddy.app.models.FuelLog

/**
 * FuelLogAdapter (VIEW component in MVC)
 * Binds FuelLog MODEL data to item_fuel_log.xml.
 * The most recent entry uses primary/indigo icon tint;
 * older entries use on_surface_variant grey.
 */
class FuelLogAdapter(
    private var logs: List<FuelLog>
) : RecyclerView.Adapter<FuelLogAdapter.FuelLogViewHolder>() {

    class FuelLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: FrameLayout = view.findViewById(R.id.fuelLogIconContainer)
        val ivIcon: ImageView          = view.findViewById(R.id.ivFuelLogIcon)
        val tvStation: TextView        = view.findViewById(R.id.tvFuelStation)
        val tvTimestamp: TextView      = view.findViewById(R.id.tvFuelTimestamp)
        val tvCost: TextView           = view.findViewById(R.id.tvFuelCost)
        val tvTrip: TextView           = view.findViewById(R.id.tvFuelTrip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuelLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fuel_log, parent, false)
        return FuelLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: FuelLogViewHolder, position: Int) {
        val log = logs[position]
        val ctx = holder.itemView.context

        holder.tvStation.text   = log.stationName
        holder.tvTimestamp.text = "${log.timestamp} • ${"%.1f".format(log.liters)} L"
        holder.tvCost.text      = "$${"%.2f".format(log.totalCost)}"
        holder.tvTrip.text      = "${log.tripKm} km trip"

        // Most-recent entry → primary icon tint; others → grey
        val tintColor = if (log.isRecent)
            ctx.getColor(R.color.primary)
        else
            ctx.getColor(R.color.on_surface_variant)

        holder.ivIcon.imageTintList = ColorStateList.valueOf(tintColor)
    }

    override fun getItemCount(): Int = logs.size

    fun updateData(newLogs: List<FuelLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}

package com.transbuddy.app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.transbuddy.app.R
import com.transbuddy.app.models.ShiftLog

/**
 * ShiftLogAdapter (VIEW component in MVC)
 *
 * Binds ShiftLog MODEL data to item_shift_log.xml.
 * Icon selection based on [ShiftLog.logType]:
 *   "clock_in"  → login arrow icon (on_surface_variant)
 *   "fuel"      → fuel icon (primary)
 *   "route"     → map icon (tertiary)
 *   default     → history icon (on_surface_variant)
 */
class ShiftLogAdapter(
    private var logs: List<ShiftLog>
) : RecyclerView.Adapter<ShiftLogAdapter.ShiftLogViewHolder>() {

    class ShiftLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivLogIcon: ImageView  = view.findViewById(R.id.ivShiftLogIcon)
        val tvTitle: TextView     = view.findViewById(R.id.tvShiftLogTitle)
        val tvSubtitle: TextView  = view.findViewById(R.id.tvShiftLogSubtitle)
        val tvTimestamp: TextView = view.findViewById(R.id.tvShiftLogTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShiftLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shift_log, parent, false)
        return ShiftLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShiftLogViewHolder, position: Int) {
        val log = logs[position]
        val ctx = holder.itemView.context

        holder.tvTitle.text     = log.title
        holder.tvSubtitle.text  = log.subtitle
        holder.tvTimestamp.text = log.timestamp

        // Icon + tint per log type
        val (iconRes, tintRes) = when (log.logType) {
            "clock_in" -> Pair(
                android.R.drawable.ic_menu_upload,
                R.color.on_surface_variant
            )
            "fuel" -> Pair(
                android.R.drawable.ic_menu_recent_history,
                R.color.primary
            )
            "route" -> Pair(
                android.R.drawable.ic_dialog_map,
                R.color.tertiary
            )
            else -> Pair(
                android.R.drawable.ic_menu_recent_history,
                R.color.on_surface_variant
            )
        }
        holder.ivLogIcon.setImageResource(iconRes)
        holder.ivLogIcon.imageTintList = ColorStateList.valueOf(ctx.getColor(tintRes))
    }

    override fun getItemCount(): Int = logs.size

    fun updateData(newLogs: List<ShiftLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}

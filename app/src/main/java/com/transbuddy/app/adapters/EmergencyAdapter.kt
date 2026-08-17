package com.transbuddy.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.transbuddy.app.R
import com.transbuddy.app.models.Emergency

class EmergencyAdapter(
    private var emergencies: List<Emergency>,
    private val onMoreClick: (Emergency) -> Unit = {}
) : RecyclerView.Adapter<EmergencyAdapter.EmergencyViewHolder>() {

    class EmergencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: FrameLayout = view.findViewById(R.id.emergencyIconContainer)
        val ivIcon: ImageView          = view.findViewById(R.id.ivEmergencyIcon)
        val tvTitle: TextView          = view.findViewById(R.id.tvEmergencyTitle)
        val tvLocation: TextView       = view.findViewById(R.id.tvEmergencyLocation)
        val tvSeverity: TextView       = view.findViewById(R.id.tvEmergencySeverity)
        val tvStatus: TextView         = view.findViewById(R.id.tvEmergencyStatus)
        val tvDescription: TextView    = view.findViewById(R.id.tvEmergencyDescription)
        val tvBusNo: TextView          = view.findViewById(R.id.tvEmergencyBusNo)
        val tvDriver: TextView         = view.findViewById(R.id.tvEmergencyDriver)
        val tvTime: TextView           = view.findViewById(R.id.tvEmergencyTime)
        val btnMore: FrameLayout       = view.findViewById(R.id.btnEmergencyMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency, parent, false)
        return EmergencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmergencyViewHolder, position: Int) {
        val emergency = emergencies[position]
        val ctx       = holder.itemView.context

        holder.tvTitle.text      = if (emergency.status.isNotBlank()) emergency.status else "Emergency Alert"
        holder.tvLocation.text   = if (emergency.location.isNotBlank()) emergency.location else "Unknown location"
        holder.tvDescription.text = emergency.description
        holder.tvStatus.text     = if (emergency.status.isNotBlank()) emergency.status else "ACTIVE"
        holder.tvTime.text       = if (emergency.createdAt.isNotBlank()) emergency.createdAt else "Just now"

        val severityText = emergency.severity.uppercase()
        holder.tvSeverity.text = severityText

        val severityColor = when (severityText) {
            "HIGH"   -> ctx.getColor(R.color.error)
            "MEDIUM" -> ctx.getColor(R.color.tertiary)
            "LOW"    -> ctx.getColor(R.color.primary)
            else     -> ctx.getColor(R.color.on_surface_variant)
        }
        holder.tvSeverity.setTextColor(severityColor)

        val statusColor = if (emergency.status.equals("ACTIVE", ignoreCase = true))
            ctx.getColor(R.color.error)
        else
            ctx.getColor(R.color.on_surface_variant)
        holder.tvStatus.setTextColor(statusColor)

        holder.tvBusNo.text = if (emergency.busNo.isNotBlank()) "Bus: ${emergency.busNo}" else "Bus: --"
        holder.tvDriver.text = if (emergency.driverName.isNotBlank()) "Driver: ${emergency.driverName}" else "Driver: --"

        holder.btnMore.setOnClickListener { onMoreClick(emergency) }
    }

    override fun getItemCount(): Int = emergencies.size

    fun updateData(newEmergencies: List<Emergency>) {
        emergencies = newEmergencies
        notifyDataSetChanged()
    }

    fun filter(query: String, fullList: List<Emergency>) {
        emergencies = if (query.isBlank()) fullList
        else fullList.filter {
            it.status.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.driverName.contains(query, ignoreCase = true) ||
            it.busNo.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }
}

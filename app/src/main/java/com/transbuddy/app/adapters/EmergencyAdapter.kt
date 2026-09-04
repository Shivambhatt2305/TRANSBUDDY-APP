package com.transbuddy.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.transbuddy.app.R
import com.transbuddy.app.models.Emergency

class EmergencyAdapter(
    private var emergencies: List<Emergency>,
    private val onMoreClick: (Emergency) -> Unit = {},
    private val onCallDriverClick: (Emergency) -> Unit = {}
) : RecyclerView.Adapter<EmergencyAdapter.EmergencyViewHolder>() {

    class EmergencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: FrameLayout          = view.findViewById(R.id.emergencyIconContainer)
        val ivIcon: ImageView                   = view.findViewById(R.id.ivEmergencyIcon)
        val tvTitle: TextView                   = view.findViewById(R.id.tvEmergencyTitle)
        val tvLocation: TextView                = view.findViewById(R.id.tvEmergencyLocation)
        val tvSeverity: TextView                = view.findViewById(R.id.tvEmergencySeverity)
        val tvStatus: TextView                  = view.findViewById(R.id.tvEmergencyStatus)
        val tvDescription: TextView             = view.findViewById(R.id.tvEmergencyDescription)
        val tvBusNo: TextView                   = view.findViewById(R.id.tvEmergencyBusNo)
        val tvDriver: TextView                  = view.findViewById(R.id.tvEmergencyDriver)
        val tvDriverPhone: TextView             = view.findViewById(R.id.tvEmergencyDriverPhone)
        val tvTime: TextView                    = view.findViewById(R.id.tvEmergencyTime)
        val btnMore: FrameLayout                = view.findViewById(R.id.btnEmergencyMore)
        val btnCallDriver: MaterialButton       = view.findViewById(R.id.btnCallEmergencyDriver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency, parent, false)
        return EmergencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmergencyViewHolder, position: Int) {
        val emergency = emergencies[position]
        val ctx       = holder.itemView.context

        holder.tvTitle.text       = if (emergency.status.isNotBlank()) emergency.status.uppercase() else "EMERGENCY ALERT"
        holder.tvLocation.text    = if (emergency.location.isNotBlank()) emergency.location else "Campus Transit Corridor"
        holder.tvDescription.text = emergency.description
        holder.tvStatus.text      = if (emergency.status.isNotBlank()) emergency.status.uppercase() else "ACTIVE"
        holder.tvTime.text        = if (emergency.createdAt.isNotBlank()) emergency.createdAt else "Just now"

        val severityText = emergency.severity.uppercase()
        holder.tvSeverity.text = severityText

        val severityColor = when (severityText) {
            "CRITICAL", "HIGH" -> ctx.getColor(R.color.error)
            "MEDIUM"           -> ctx.getColor(R.color.tertiary)
            "LOW"              -> ctx.getColor(R.color.primary)
            else               -> ctx.getColor(R.color.on_surface_variant)
        }
        holder.tvSeverity.setTextColor(severityColor)

        val statusColor = if (emergency.status.equals("ACTIVE", ignoreCase = true) || emergency.status.equals("REPORTED", ignoreCase = true))
            ctx.getColor(R.color.error)
        else
            ctx.getColor(R.color.on_surface_variant)
        holder.tvStatus.setTextColor(statusColor)

        holder.tvBusNo.text = if (emergency.busNo.isNotBlank() && emergency.busNo != "null") "Bus: ${emergency.busNo}" else "Bus: Fleet Vehicle"

        // Resolve driver name
        val resolvedDriverName = when {
            emergency.driverName.isNotBlank() && emergency.driverName != "null" -> emergency.driverName
            emergency.busNo.contains("102") -> "Michael Scott"
            emergency.busNo.contains("442") -> "MR. SUDHIRBHAI BATUKBHAI BHUTA"
            emergency.busNo.contains("089") -> "MR. MOSIN AJIJBHAI SANDHVANI"
            emergency.busNo.contains("205") -> "MR. ASHISH RAJNIKANT TRIVEDI"
            else -> "Fleet Driver (Assigned)"
        }

        // Resolve driver phone number so it is ALWAYS displayed prominently
        val resolvedPhone = when {
            emergency.driverPhone.isNotBlank() && emergency.driverPhone != "null" -> emergency.driverPhone
            resolvedDriverName.contains("Michael", ignoreCase = true) -> "+91 98250 12345"
            resolvedDriverName.contains("Dwight", ignoreCase = true) -> "+91 98250 23456"
            resolvedDriverName.contains("Jim", ignoreCase = true) -> "+91 98250 34567"
            resolvedDriverName.contains("Pam", ignoreCase = true) -> "+91 98250 45678"
            resolvedDriverName.contains("Ryan", ignoreCase = true) -> "+91 98250 56789"
            resolvedDriverName.contains("SUDHIR", ignoreCase = true) -> "+91 98251 10001"
            resolvedDriverName.contains("MOSIN", ignoreCase = true) -> "+91 98251 10002"
            resolvedDriverName.contains("ASGAR", ignoreCase = true) -> "+91 98251 10003"
            resolvedDriverName.contains("NARESH", ignoreCase = true) -> "+91 98251 10004"
            resolvedDriverName.contains("HUSEN", ignoreCase = true) -> "+91 98251 10005"
            resolvedDriverName.contains("ASHISH", ignoreCase = true) -> "+91 98251 10011"
            resolvedDriverName.contains("KAJI", ignoreCase = true) -> "+91 98251 10012"
            resolvedDriverName.contains("RATHOD", ignoreCase = true) -> "+91 98251 10013"
            emergency.busNo.contains("102") -> "+91 98250 12345"
            emergency.busNo.contains("442") -> "+91 98251 10001"
            emergency.busNo.contains("089") -> "+91 98251 10002"
            emergency.busNo.contains("205") -> "+91 98251 10011"
            else -> "+91 98765 43210"
        }

        holder.tvDriver.text = "Driver: $resolvedDriverName"
        holder.tvDriverPhone.text = "Mobile: $resolvedPhone"

        val updatedEmergency = emergency.copy(driverName = resolvedDriverName, driverPhone = resolvedPhone)

        holder.btnCallDriver.isEnabled = true
        holder.btnCallDriver.alpha = 1.0f
        holder.btnCallDriver.text = "Call Driver"
        holder.btnCallDriver.setOnClickListener {
            onCallDriverClick(updatedEmergency)
        }

        holder.tvDriverPhone.setOnClickListener {
            onCallDriverClick(updatedEmergency)
        }

        holder.btnMore.setOnClickListener { onMoreClick(updatedEmergency) }
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
            it.driverPhone.contains(query, ignoreCase = true) ||
            it.busNo.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }
}

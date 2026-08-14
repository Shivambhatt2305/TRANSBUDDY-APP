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
import com.transbuddy.app.models.Vehicle

/**
 * VehicleAdapter (VIEW component in MVC)
 *
 * Binds Vehicle MODEL data to item_vehicle.xml.
 * Handles status-based visual theming:
 *   - Active  → primary/indigo icon + primary badge
 *   - Delay   → error/red icon   + error badge
 *   - Offline → grey icon        + grey badge
 */
class VehicleAdapter(
    private var vehicles: List<Vehicle>,
    private val onItemClick: (Vehicle) -> Unit = {}
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    // ─── ViewHolder ────────────────────────────────────────────
    class VehicleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivVehicleIcon: ImageView   = view.findViewById(R.id.ivVehicleIcon)
        val iconContainer: FrameLayout = view.findViewById(R.id.iconContainer)
        val tvVehicleId: TextView      = view.findViewById(R.id.tvVehicleId)
        val tvRoute: TextView          = view.findViewById(R.id.tvRoute)
        val tvStatus: TextView         = view.findViewById(R.id.tvStatus)
        val tvSpeed: TextView          = view.findViewById(R.id.tvSpeed)
        val tvEta: TextView            = view.findViewById(R.id.tvEta)
    }

    // ─── RecyclerView overrides ────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        val ctx     = holder.itemView.context

        // ── Text data ──────────────────────────────────────────
        holder.tvVehicleId.text = vehicle.id
        holder.tvRoute.text     = vehicle.route
        holder.tvStatus.text    = vehicle.status
        holder.tvSpeed.text     = "Speed: ${vehicle.speed}"
        holder.tvEta.text       = if (vehicle.eta.startsWith("+")) "ETA: ${vehicle.eta}"
                                   else "ETA: ${vehicle.eta}"

        // ── Status-based colours ───────────────────────────────
        when (vehicle.status) {
            "Active" -> {
                val primaryColor = ctx.getColor(R.color.primary)
                holder.ivVehicleIcon.imageTintList = ColorStateList.valueOf(primaryColor)
                holder.tvStatus.setTextColor(primaryColor)
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_active)
            }
            "Delay" -> {
                val errorColor = ctx.getColor(R.color.error)
                holder.ivVehicleIcon.imageTintList = ColorStateList.valueOf(errorColor)
                holder.tvStatus.setTextColor(errorColor)
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_delay)
            }
            else -> {  // Offline / Unknown
                val greyColor = ctx.getColor(R.color.on_surface_variant)
                holder.ivVehicleIcon.imageTintList = ColorStateList.valueOf(greyColor)
                holder.tvStatus.setTextColor(greyColor)
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_offline)
            }
        }

        // ── Click listener ─────────────────────────────────────
        holder.itemView.setOnClickListener { onItemClick(vehicle) }
    }

    override fun getItemCount(): Int = vehicles.size

    // ─── Public API ────────────────────────────────────────────
    fun updateData(newVehicles: List<Vehicle>) {
        vehicles = newVehicles
        notifyDataSetChanged()
    }

    /** Filter vehicles by ID prefix for search functionality */
    fun filter(query: String, fullList: List<Vehicle>) {
        vehicles = if (query.isBlank()) fullList
                   else fullList.filter { it.id.contains(query, ignoreCase = true) }
        notifyDataSetChanged()
    }
}
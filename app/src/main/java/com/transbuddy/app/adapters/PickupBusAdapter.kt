package com.transbuddy.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.transbuddy.app.R
import com.transbuddy.app.models.PickupPointBus
import com.transbuddy.app.utils.ImageLoader

/**
 * PickupBusAdapter — VIEW component in MVC
 * Binds PickupPointBus models to item_pickup_bus.xml displaying driver name and profile photo.
 */
class PickupBusAdapter(
    private var pickupPoints: List<PickupPointBus>
) : RecyclerView.Adapter<PickupBusAdapter.PickupViewHolder>() {

    class PickupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView        = view.findViewById(R.id.tvPickupPointTitle)
        val tvZone: TextView         = view.findViewById(R.id.tvAreaZone)
        val tvBusBadge: TextView     = view.findViewById(R.id.tvAssignedBusBadge)
        val ivDriverPhoto: ImageView = view.findViewById(R.id.ivDriverPhoto)
        val tvDriver: TextView       = view.findViewById(R.id.tvDriverName)
        val tvTime: TextView         = view.findViewById(R.id.tvPickupTime)
        val tvCount: TextView        = view.findViewById(R.id.tvStudentCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pickup_bus, parent, false)
        return PickupViewHolder(view)
    }

    override fun onBindViewHolder(holder: PickupViewHolder, position: Int) {
        val item = pickupPoints[position]
        holder.tvTitle.text    = item.pickupPointName
        holder.tvZone.text     = item.areaZone
        holder.tvBusBadge.text = "BUS: ${item.assignedBusId}"
        holder.tvDriver.text   = item.driverName
        holder.tvTime.text     = item.pickupTime
        holder.tvCount.text    = "${item.studentCount} Students"

        // Load driver profile photo asynchronously
        ImageLoader.loadImage(
            item.driverPhotoUrl,
            holder.ivDriverPhoto,
            android.R.drawable.ic_menu_myplaces
        )
    }

    override fun getItemCount(): Int = pickupPoints.size

    fun filter(query: String, fullList: List<PickupPointBus>) {
        pickupPoints = if (query.isBlank()) fullList
        else fullList.filter {
            it.pickupPointName.contains(query, ignoreCase = true) ||
            it.areaZone.contains(query, ignoreCase = true) ||
            it.assignedBusId.contains(query, ignoreCase = true) ||
            it.driverName.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }
}

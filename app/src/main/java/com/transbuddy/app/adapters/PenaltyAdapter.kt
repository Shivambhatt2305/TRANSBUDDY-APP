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
import com.transbuddy.app.models.Penalty

/**
 * PenaltyAdapter (VIEW component in MVC)
 *
 * Binds Penalty MODEL data to item_penalty.xml.
 * Icon tint and amount color are driven by [Penalty.iconType] and [Penalty.isError].
 *
 * Icon mapping:
 *   "speeding" → error/red
 *   "route"    → primary/indigo
 *   "idle"     → tertiary/violet
 *   "safety"   → on_surface_variant/grey
 */
class PenaltyAdapter(
    private var penalties: List<Penalty>,
    private val onMoreClick: (Penalty) -> Unit = {}
) : RecyclerView.Adapter<PenaltyAdapter.PenaltyViewHolder>() {

    // ─── ViewHolder ────────────────────────────────────────────
    class PenaltyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconContainer: FrameLayout = view.findViewById(R.id.penaltyIconContainer)
        val ivIcon: ImageView          = view.findViewById(R.id.ivPenaltyIcon)
        val tvTitle: TextView          = view.findViewById(R.id.tvPenaltyTitle)
        val tvDriver: TextView         = view.findViewById(R.id.tvPenaltyDriver)
        val tvAmount: TextView         = view.findViewById(R.id.tvPenaltyAmount)
        val btnMore: FrameLayout       = view.findViewById(R.id.btnPenaltyMore)
    }

    // ─── Inflate ───────────────────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PenaltyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_penalty, parent, false)
        return PenaltyViewHolder(view)
    }

    // ─── Bind ──────────────────────────────────────────────────
    override fun onBindViewHolder(holder: PenaltyViewHolder, position: Int) {
        val penalty = penalties[position]
        val ctx     = holder.itemView.context

        holder.tvTitle.text  = penalty.title
        holder.tvDriver.text = penalty.driverInfo
        holder.tvAmount.text = penalty.amount

        // Amount colour: error if flagged, otherwise on_surface
        val amountColor = if (penalty.isError)
            ctx.getColor(R.color.error)
        else
            ctx.getColor(R.color.on_surface)
        holder.tvAmount.setTextColor(amountColor)

        // Icon tint based on infraction type
        val iconTint = when (penalty.iconType) {
            "speeding" -> ctx.getColor(R.color.error)
            "route"    -> ctx.getColor(R.color.primary)
            "idle"     -> ctx.getColor(R.color.tertiary)
            else       -> ctx.getColor(R.color.on_surface_variant)
        }
        holder.ivIcon.imageTintList = ColorStateList.valueOf(iconTint)

        // More button callback
        holder.btnMore.setOnClickListener { onMoreClick(penalty) }
    }

    override fun getItemCount(): Int = penalties.size

    // ─── Public API ────────────────────────────────────────────
    fun updateData(newPenalties: List<Penalty>) {
        penalties = newPenalties
        notifyDataSetChanged()
    }

    /** Filter by driver info or title for search */
    fun filter(query: String, fullList: List<Penalty>) {
        penalties = if (query.isBlank()) fullList
        else fullList.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.driverInfo.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }
}

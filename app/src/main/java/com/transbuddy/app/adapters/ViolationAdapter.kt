package com.transbuddy.app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.transbuddy.app.R
import com.transbuddy.app.models.ViolationAlert

/**
 * ViolationAdapter (VIEW component in MVC)
 *
 * Binds ViolationAlert MODEL data to item_violation.xml.
 *
 * Icon + tint logic per alertType:
 *   "unauthorized" → person_off icon, error/red tint
 *   "unpaid"       → money_off icon, tertiary/violet tint
 *   "invalid_scan" → badge icon,     on_surface_variant/grey tint
 *
 * Resolved items:
 *   - btnReview → GONE
 *   - chipResolved → VISIBLE
 *   - itemView alpha → 0.7f
 *
 * Active items:
 *   - btnReview → VISIBLE, fires onReviewClick callback
 *   - chipResolved → GONE
 *   - itemView alpha → 1.0f
 */
class ViolationAdapter(
    private var alerts: List<ViolationAlert>,
    private val onReviewClick: (ViolationAlert, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ViolationAdapter.ViolationViewHolder>() {

    // ─── ViewHolder ────────────────────────────────────────────
    class ViolationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconBg: FrameLayout    = view.findViewById(R.id.violationIconBg)
        val ivIcon: ImageView      = view.findViewById(R.id.ivViolationIcon)
        val tvTitle: TextView      = view.findViewById(R.id.tvViolationTitle)
        val tvBusInfo: TextView    = view.findViewById(R.id.tvViolationBusInfo)
        val tvTime: TextView       = view.findViewById(R.id.tvViolationTime)
        val btnReview: CardView    = view.findViewById(R.id.btnViolationReview)
        val chipResolved: TextView = view.findViewById(R.id.chipResolved)
    }

    // ─── Inflate ───────────────────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViolationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_violation, parent, false)
        return ViolationViewHolder(view)
    }

    // ─── Bind ──────────────────────────────────────────────────
    override fun onBindViewHolder(holder: ViolationViewHolder, position: Int) {
        val alert = alerts[position]
        val ctx   = holder.itemView.context

        holder.tvTitle.text   = alert.title
        holder.tvBusInfo.text = alert.busInfo
        holder.tvTime.text    = alert.time

        // ── Icon + tint by alert type ──────────────────────────
        val (iconRes, tintColorRes) = when (alert.alertType) {
            "unauthorized" -> Pair(android.R.drawable.ic_delete,     R.color.error)
            "unpaid"       -> Pair(android.R.drawable.ic_menu_close_clear_cancel, R.color.tertiary)
            "invalid_scan" -> Pair(android.R.drawable.ic_menu_myplaces, R.color.on_surface_variant)
            else           -> Pair(android.R.drawable.ic_dialog_alert, R.color.error)
        }
        holder.ivIcon.setImageResource(iconRes)
        holder.ivIcon.imageTintList =
            ColorStateList.valueOf(ctx.getColor(tintColorRes))

        // ── Resolved vs Active state ───────────────────────────
        if (alert.isResolved) {
            holder.itemView.alpha  = 0.7f
            holder.btnReview.visibility    = View.GONE
            holder.chipResolved.visibility = View.VISIBLE
            // Strike-through title
            holder.tvTitle.paintFlags =
                holder.tvTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.itemView.alpha  = 1.0f
            holder.btnReview.visibility    = View.VISIBLE
            holder.chipResolved.visibility = View.GONE
            holder.tvTitle.paintFlags =
                holder.tvTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

            holder.btnReview.setOnClickListener {
                onReviewClick(alert, position)
            }
        }
    }

    override fun getItemCount(): Int = alerts.size

    // ─── Public API ────────────────────────────────────────────
    fun updateData(newAlerts: List<ViolationAlert>) {
        alerts = newAlerts
        notifyDataSetChanged()
    }

    /** Filter by bus info or title for search */
    fun filter(query: String, fullList: List<ViolationAlert>) {
        alerts = if (query.isBlank()) fullList
        else fullList.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.busInfo.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }

    /** Mark an item as resolved and refresh that row */
    fun markResolved(position: Int) {
        val mutable = alerts.toMutableList()
        val item    = mutable[position]
        mutable[position] = item.copy(isResolved = true)
        alerts = mutable
        notifyItemChanged(position)
    }
}

package com.transbuddy.app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.transbuddy.app.R
import com.transbuddy.app.controllers.StudentDetailActivity
import com.transbuddy.app.models.StudentRider
import com.transbuddy.app.utils.ImageLoader

/**
 * GrSearchAdapter — VIEW component in MVC
 * Binds StudentRider models to item_gr_search.xml displaying full student profile details.
 */
class GrSearchAdapter(
    private var students: List<StudentRider>
) : RecyclerView.Adapter<GrSearchAdapter.GrViewHolder>() {

    class GrViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView       = view.findViewById(R.id.ivMemberPhoto)
        val tvName: TextView        = view.findViewById(R.id.tvStudentName)
        val tvGrNumber: TextView    = view.findViewById(R.id.tvGrNumber)
        val tvDepartment: TextView  = view.findViewById(R.id.tvDepartment)
        val tvStatus: TextView      = view.findViewById(R.id.tvStudentStatus)
        val tvAssignedBus: TextView = view.findViewById(R.id.tvAssignedBus)
        val tvPickupPoint: TextView = view.findViewById(R.id.tvPickupPoint)
        val tvEmail: TextView       = view.findViewById(R.id.tvEmail)
        val tvPhone: TextView       = view.findViewById(R.id.tvPhone)
        val btnIssuePenalty: MaterialButton = view.findViewById(R.id.btnIssuePenalty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gr_search, parent, false)
        return GrViewHolder(view)
    }

    override fun onBindViewHolder(holder: GrViewHolder, position: Int) {
        val student = students[position]
        holder.tvName.text = student.name

        // Build GR & Enrollment No string
        holder.tvGrNumber.text = if (student.enrollmentNo.isNotEmpty()) {
            "GR: ${student.grNumber} | ENROLL: ${student.enrollmentNo}"
        } else {
            "ID: ${student.grNumber}"
        }

        // Build Department, Sem, Shift string
        val deptParts = mutableListOf<String>()
        if (student.department.isNotEmpty()) deptParts.add(student.department)
        if (student.semester.isNotEmpty()) deptParts.add(student.semester)
        if (student.shift.isNotEmpty()) deptParts.add(student.shift)
        holder.tvDepartment.text = if (deptParts.isNotEmpty()) deptParts.joinToString(" • ") else "Transbuddy Member"

        holder.tvStatus.text      = student.status
        holder.tvAssignedBus.text = "${student.busId} (${student.route})"
        holder.tvPickupPoint.text = student.pickupPoint
        holder.tvEmail.text       = if (student.email.isNotEmpty()) student.email else "N/A"
        holder.tvPhone.text       = if (student.phone.isNotEmpty()) student.phone else "N/A"

        // Asynchronously load profile image from Marwadi photo API
        ImageLoader.loadImage(
            student.photoUrl,
            holder.ivPhoto,
            android.R.drawable.ic_menu_myplaces
        )

        val openStudentDetail = {
            val context = holder.itemView.context
            val intent = Intent(context, StudentDetailActivity::class.java).apply {
                putExtra("GR_NUMBER", student.grNumber)
                putExtra("ENROLLMENT_NO", student.enrollmentNo)
                putExtra("STUDENT_NAME", student.name)
                putExtra("DEPARTMENT", student.department)
                putExtra("SEMESTER", student.semester)
                putExtra("SHIFT", student.shift)
                putExtra("EMAIL", student.email)
                putExtra("PHONE", student.phone)
                putExtra("BUS_ID", student.busId)
                putExtra("PICKUP_POINT", student.pickupPoint)
                putExtra("ROUTE", student.route)
                putExtra("STATUS", student.status)
                putExtra("MEMBER_TYPE", student.memberType)
                putExtra("PHOTO_URL", student.photoUrl)
            }
            context.startActivity(intent)
        }

        holder.btnIssuePenalty.setOnClickListener { openStudentDetail() }
        holder.itemView.setOnClickListener { openStudentDetail() }
    }

    override fun getItemCount(): Int = students.size

    fun updateData(newList: List<StudentRider>) {
        students = newList
        notifyDataSetChanged()
    }

    fun filter(query: String, fullList: List<StudentRider>) {
        val trimmed = query.trim()
        students = if (trimmed.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.grNumber.contains(trimmed, ignoreCase = true) ||
                it.enrollmentNo.contains(trimmed, ignoreCase = true) ||
                it.name.contains(trimmed, ignoreCase = true) ||
                it.department.contains(trimmed, ignoreCase = true) ||
                it.busId.contains(trimmed, ignoreCase = true) ||
                it.pickupPoint.contains(trimmed, ignoreCase = true) ||
                it.email.contains(trimmed, ignoreCase = true) ||
                it.memberType.contains(trimmed, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}

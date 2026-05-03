package com.example.mc_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(
    private val records: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record)
    }

    override fun getItemCount(): Int = records.size

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tv_att_device_name)
        private val tvDeviceAddress: TextView = itemView.findViewById(R.id.tv_att_address)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_att_time)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_att_date)

        fun bind(record: AttendanceRecord) {
            val display = if (record.rollNo != "N/A" && record.rollNo.isNotEmpty()) {
                "${record.deviceName} (Roll: ${record.rollNo})"
            } else {
                record.deviceName
            }
            tvDeviceName.text = display
            tvDeviceAddress.text = record.deviceAddress
            tvTime.text = record.timestamp
            tvDate.text = record.date
        }
    }
}
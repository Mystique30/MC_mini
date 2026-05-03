package com.example.mc_project

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onItemClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
        holder.itemView.setOnClickListener {
            onItemClick(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tv_device_name)
        private val tvDeviceAddress: TextView = itemView.findViewById(R.id.tv_device_address)

        fun bind(device: BluetoothDevice) {
            val rawName = device.name ?: "Unknown Device"
            var displayName = rawName
            
            if (rawName.startsWith("MC|12345|")) {
                val parts = rawName.split("|")
                if (parts.size >= 4) {
                    val rollNo = parts[2]
                    val studentName = parts[3]
                    displayName = "$studentName (Roll: $rollNo)"
                }
            }
            
            tvDeviceName.text = displayName
            tvDeviceAddress.text = device.address
        }
    }
}
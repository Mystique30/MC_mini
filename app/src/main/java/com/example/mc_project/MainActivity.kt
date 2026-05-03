package com.example.mc_project

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.AlertDialog
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.os.Environment
import android.view.LayoutInflater
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnClearAll: Button
    private lateinit var btnSaveAttendance: Button
    private lateinit var btnDiscoverable: Button
    private lateinit var tvDeviceCount: TextView
    private lateinit var tvUserName: TextView
    private lateinit var btnEditName: ImageButton
    private lateinit var rvDevices: RecyclerView
    private lateinit var rvAttendance: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val deviceList = ArrayList<BluetoothDevice>()
    private val attendanceList = ArrayList<AttendanceRecord>()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                var device: BluetoothDevice? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                }

                if (device != null && device.name != null && !deviceList.contains(device)) {
                    val deviceName = device.name ?: ""
                    if (deviceName.startsWith("MC|12345|")) {
                        deviceList.add(device)
                        deviceAdapter.notifyDataSetChanged()
                        tvDeviceCount.text = "${deviceList.size} devices found"
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScan = findViewById(R.id.btn_scan)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnClearAll = findViewById(R.id.btn_clear_all)
        btnSaveAttendance = findViewById(R.id.btn_save_attendance)
        btnDiscoverable = findViewById(R.id.btn_discoverable)
        tvDeviceCount = findViewById(R.id.tv_device_count)
        tvUserName = findViewById(R.id.tv_user_name)
        btnEditName = findViewById(R.id.btn_edit_name)
        rvDevices = findViewById(R.id.rv_devices)
        rvAttendance = findViewById(R.id.rv_attendance)

        deviceAdapter = DeviceAdapter(deviceList) { device ->
            markAttendance(device)
        }
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter

        attendanceAdapter = AttendanceAdapter(attendanceList)
        rvAttendance.layoutManager = LinearLayoutManager(this)
        rvAttendance.adapter = attendanceAdapter

        btnScan.setOnClickListener {
            checkPermissionsAndScan()
        }

        btnRefresh.setOnClickListener {
            deviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            tvDeviceCount.text = "0 devices"
            checkPermissionsAndScan()
        }

        btnClearAll.setOnClickListener {
            attendanceList.clear()
            attendanceAdapter.notifyDataSetChanged()
            getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE).edit().remove("attendance_records").apply()
            Toast.makeText(this, "All attendance records cleared", Toast.LENGTH_SHORT).show()
        }

        btnSaveAttendance.setOnClickListener {
            if (attendanceList.isEmpty()) {
                Toast.makeText(this, "No attendance to save", Toast.LENGTH_SHORT).show()
            } else {
                showSaveAttendanceDialog()
            }
        }

        btnDiscoverable.setOnClickListener {
            makeDiscoverable()
        }

        btnEditName.setOnClickListener {
            showNameSetupDialog()
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(enableBtIntent)
                }
            } else {
                startActivity(enableBtIntent)
            }
        }

        loadAttendanceRecords()
        checkAndPromptForName()
    }

    private fun checkAndPromptForName() {
        val sharedPref = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
        val savedName = sharedPref.getString("user_name", "")
        val savedRollNo = sharedPref.getString("user_roll_no", "")
        if (savedName.isNullOrEmpty() || savedRollNo.isNullOrEmpty()) {
            showNameSetupDialog()
        } else {
            tvUserName.text = "Name: $savedName | Roll: $savedRollNo"
            val formattedName = "MC|12345|$savedRollNo|$savedName"
            updateBluetoothName(formattedName)
        }
    }

    private fun showNameSetupDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_name_setup, null)
        val etFullName = view.findViewById<EditText>(R.id.et_full_name)
        val etRollNo = view.findViewById<EditText>(R.id.et_roll_no)
        val etSubjectName = view.findViewById<EditText>(R.id.et_subject_name)
        val etSubjectKey = view.findViewById<EditText>(R.id.et_subject_key)

        val sharedPref = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
        val savedName = sharedPref.getString("user_name", "")
        val savedRollNo = sharedPref.getString("user_roll_no", "")
        if (!savedName.isNullOrEmpty()) etFullName.setText(savedName)
        if (!savedRollNo.isNullOrEmpty()) etRollNo.setText(savedRollNo)

        AlertDialog.Builder(this)
            .setTitle("Enrollment Setup")
            .setMessage("Please enter your details to enroll in the attendance system.")
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("Enroll") { _, _ ->
                val name = etFullName.text.toString().trim()
                val rollNo = etRollNo.text.toString().trim()
                val subject = etSubjectName.text.toString().trim()
                val key = etSubjectKey.text.toString().trim()

                if (name.isEmpty() || rollNo.isEmpty()) {
                    Toast.makeText(this, "Name and Roll No cannot be empty", Toast.LENGTH_SHORT).show()
                    showNameSetupDialog()
                } else if (!subject.equals("mobile computing", ignoreCase = true)) {
                    Toast.makeText(this, "Invalid Subject Name", Toast.LENGTH_LONG).show()
                    showNameSetupDialog()
                } else if (key != "12345") {
                    Toast.makeText(this, "Invalid Subject Key", Toast.LENGTH_LONG).show()
                    showNameSetupDialog()
                } else {
                    sharedPref.edit()
                        .putString("user_name", name)
                        .putString("user_roll_no", rollNo)
                        .apply()
                    tvUserName.text = "Name: $name | Roll: $rollNo"
                    val formattedName = "MC|12345|$rollNo|$name"
                    updateBluetoothName(formattedName)
                    Toast.makeText(this, "Successfully Enrolled!", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun updateBluetoothName(name: String) {
        if (bluetoothAdapter == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.name = name
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 3)
            }
        } else {
            bluetoothAdapter.name = name
        }
    }

    private fun showSaveAttendanceDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_save_attendance, null)
        val etLectureName = view.findViewById<EditText>(R.id.et_lecture_name)
        val etDate = view.findViewById<EditText>(R.id.et_date)
        val etTime = view.findViewById<EditText>(R.id.et_time)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        etDate.setText(dateFormat.format(calendar.time))
        etTime.setText(timeFormat.format(calendar.time))

        AlertDialog.Builder(this)
            .setTitle("Save Attendance to CSV")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val lecName = etLectureName.text.toString().trim()
                val date = etDate.text.toString().trim()
                val time = etTime.text.toString().trim()
                if (lecName.isEmpty()) {
                    Toast.makeText(this, "Lecture name is required", Toast.LENGTH_SHORT).show()
                } else {
                    exportAttendanceToCSV(lecName, date, time)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportAttendanceToCSV(lecName: String, date: String, time: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val safeLecName = lecName.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val safeDate = date.replace("/", "-")
            val fileName = "Attendance_${safeLecName}_${safeDate}.csv"
            val file = File(downloadsDir, fileName)

            val fos = FileOutputStream(file)
            val osw = OutputStreamWriter(fos)

            osw.write("Lecture Name: $lecName\n")
            osw.write("Date: $date\n")
            osw.write("Time: $time\n\n")
            osw.write("Roll No,Student Name,Device Address,Marked Time\n")

            for (record in attendanceList) {
                // Escape commas in names
                val safeName = if (record.deviceName.contains(",")) "\"${record.deviceName}\"" else record.deviceName
                osw.write("${record.rollNo},$safeName,${record.deviceAddress},${record.timestamp}\n")
            }

            osw.flush()
            osw.close()
            fos.close()

            Toast.makeText(this, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun makeDiscoverable() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(discoverableIntent)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE), 2)
            }
        } else {
            startActivity(discoverableIntent)
        }
    }

    private fun checkPermissionsAndScan() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
        } else {
            startBluetoothScan()
        }
    }

    private fun startBluetoothScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()
        tvDeviceCount.text = "Scanning..."

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.startDiscovery()
                Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
            }
        } else {
            bluetoothAdapter.startDiscovery()
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markAttendance(device: BluetoothDevice) {
        val rawName = device.name ?: "Unknown"
        var deviceName = rawName
        var rollNo = "N/A"
        
        if (rawName.startsWith("MC|12345|")) {
            val parts = rawName.split("|")
            if (parts.size >= 4) {
                rollNo = parts[2]
                deviceName = parts[3]
            }
        }

        val deviceAddress = device.address
        val currentTime = Calendar.getInstance().time
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val attendanceRecord = AttendanceRecord(deviceName, rollNo, deviceAddress, timeFormat.format(currentTime), dateFormat.format(currentTime), currentTime)

        val alreadyMarked = attendanceList.any { it.deviceAddress == deviceAddress && it.date == dateFormat.format(currentTime) }

        if (alreadyMarked) {
            Toast.makeText(this, "$deviceName already marked today!", Toast.LENGTH_SHORT).show()
        } else {
            attendanceList.add(0, attendanceRecord)
            attendanceAdapter.notifyItemInserted(0)
            saveAttendanceRecord(attendanceRecord)
            Toast.makeText(this, "✓ Attendance marked for $deviceName", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveAttendanceRecord(record: AttendanceRecord) {
        val sharedPref = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
        val existingRecords = getSavedAttendanceRecords().toMutableList()
        existingRecords.add(0, record)
        val recordsJson = StringBuilder()
        existingRecords.forEach { rec ->
            recordsJson.append("${rec.deviceName}|${rec.rollNo}|${rec.deviceAddress}|${rec.timestamp}|${rec.date}\n")
        }
        sharedPref.edit().putString("attendance_records", recordsJson.toString()).apply()
    }

    private fun loadAttendanceRecords() {
        attendanceList.clear()
        attendanceList.addAll(getSavedAttendanceRecords())
        attendanceAdapter.notifyDataSetChanged()
    }

    private fun getSavedAttendanceRecords(): List<AttendanceRecord> {
        val sharedPref = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
        val savedData = sharedPref.getString("attendance_records", "") ?: ""
        val records = mutableListOf<AttendanceRecord>()
        savedData.split("\n").forEach { line ->
            if (line.isNotBlank()) {
                val parts = line.split("|")
                if (parts.size == 5) {
                    records.add(AttendanceRecord(parts[0], parts[1], parts[2], parts[3], parts[4], Calendar.getInstance().time))
                } else if (parts.size == 4) {
                    // Backwards compatibility
                    records.add(AttendanceRecord(parts[0], "N/A", parts[1], parts[2], parts[3], Calendar.getInstance().time))
                }
            }
        }
        return records
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBluetoothScan()
            } else {
                Toast.makeText(this, "Permissions required for Bluetooth scanning", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeDiscoverable()
            } else {
                Toast.makeText(this, "Permission required to make device discoverable", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 3) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val sharedPref = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
                val savedName = sharedPref.getString("user_name", "")
                if (!savedName.isNullOrEmpty()) {
                    updateBluetoothName(savedName)
                }
            } else {
                Toast.makeText(this, "Permission required to set Bluetooth name", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothReceiver) } catch (e: Exception) {}
        if (::bluetoothAdapter.isInitialized && bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
    }
}
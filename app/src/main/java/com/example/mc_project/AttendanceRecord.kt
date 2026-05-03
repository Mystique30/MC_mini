package com.example.mc_project

import java.util.Date

data class AttendanceRecord(
    val deviceName: String,
    val rollNo: String,
    val deviceAddress: String,
    val timestamp: String,
    val date: String,
    val fullDateTime: Date
)
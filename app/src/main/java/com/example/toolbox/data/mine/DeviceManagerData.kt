package com.example.toolbox.data.mine

import kotlinx.serialization.Serializable

@Serializable
data class DeviceResponse(val devices: List<Device>)

@Serializable
data class Device(
    val device_id: String,
    val device_model: String,
    val ip_address: String,
    val ip_location: String,
    val login_time: String,
    val last_activity: String,
    val is_active: Boolean,
    val is_current: Boolean
)
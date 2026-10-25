package com.virtualkeys.oneui.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.virtualkeys.oneui.R

data class OutputInfo(
    val label: String,
    val iconRes: Int,
)

object AudioOutput {

    fun current(context: Context): OutputInfo {
        val am = context.applicationContext.getSystemService(AudioManager::class.java)
            ?: return OutputInfo("Phone speaker", R.drawable.ic_speaker)
        val devices: Array<AudioDeviceInfo>? = try {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } catch (_: Throwable) {
            null
        }
        devices?.forEach { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_BLE_BROADCAST,
                -> return OutputInfo("Bluetooth", R.drawable.ic_bluetooth)

                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                -> return OutputInfo("Headphones", R.drawable.ic_headphones)
            }
        }
        return OutputInfo("Phone speaker", R.drawable.ic_speaker)
    }
}

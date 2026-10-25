package com.virtualkeys.oneui.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.virtualkeys.oneui.audio.VolStream
import com.virtualkeys.oneui.audio.VolumeRepository
import com.virtualkeys.oneui.media.MediaRepository
import com.virtualkeys.oneui.ui.PanelInteraction

class ShadeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                MediaRepository.togglePlayPause()
                PanelInteraction.touch()
            }

            ACTION_NEXT -> {
                MediaRepository.skipNext()
                PanelInteraction.touch()
            }

            ACTION_PREV -> {
                MediaRepository.skipPrevious()
                PanelInteraction.touch()
            }

            ACTION_VOL_UP -> VolumeRepository.step(context, VolStream.MEDIA, 1)
            ACTION_VOL_DOWN -> VolumeRepository.step(context, VolStream.MEDIA, -1)
            ACTION_VOL_MUTE -> VolumeRepository.toggleMute(context, VolStream.MEDIA)
        }
        ShadeCardManager.requestRefresh(context)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.virtualkeys.oneui.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.virtualkeys.oneui.action.NEXT"
        const val ACTION_PREV = "com.virtualkeys.oneui.action.PREV"
        const val ACTION_VOL_UP = "com.virtualkeys.oneui.action.VOL_UP"
        const val ACTION_VOL_DOWN = "com.virtualkeys.oneui.action.VOL_DOWN"
        const val ACTION_VOL_MUTE = "com.virtualkeys.oneui.action.VOL_MUTE"
    }
}

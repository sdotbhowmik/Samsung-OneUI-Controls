package com.virtualkeys.oneui.service

import android.service.notification.NotificationListenerService
import com.virtualkeys.oneui.media.MediaRepository

class MediaListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaRepository.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaRepository.onListenerDisconnected()
    }
}

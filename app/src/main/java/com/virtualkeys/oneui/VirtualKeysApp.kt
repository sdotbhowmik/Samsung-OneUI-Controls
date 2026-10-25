package com.virtualkeys.oneui

import android.app.Application
import com.virtualkeys.oneui.media.MediaRepository
import com.virtualkeys.oneui.notif.ShadeCardManager
import com.virtualkeys.oneui.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VirtualKeysApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        MediaRepository.init(this)
        ShadeCardManager.init(this)

        scope.launch {
            MediaRepository.state.collect {
                ShadeCardManager.requestRefresh(this@VirtualKeysApp)
            }
        }
        scope.launch {
            SettingsStore.state.collect {
                ShadeCardManager.requestRefresh(this@VirtualKeysApp)
            }
        }

        ShadeCardManager.requestRefresh(this)
    }
}

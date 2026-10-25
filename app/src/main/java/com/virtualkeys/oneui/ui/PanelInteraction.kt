package com.virtualkeys.oneui.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A tiny shared event counter used to keep the panel alive while the user is
 * interacting with it (including hardware volume keys).
 */
object PanelInteraction {
    private val _tick = MutableStateFlow(0)
    val tick = _tick.asStateFlow()

    fun touch() {
        _tick.value += 1
    }
}

package com.virtualkeys.oneui.audio

import android.content.Context
import android.media.AudioManager
import com.virtualkeys.oneui.R
import com.virtualkeys.oneui.media.MediaRepository
import com.virtualkeys.oneui.notif.ShadeCardManager
import com.virtualkeys.oneui.ui.PanelInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

enum class VolStream(
    val id: Int,
    val label: String,
    val iconRes: Int,
    val isPrimary: Boolean,
) {
    MEDIA(AudioManager.STREAM_MUSIC, "Media", R.drawable.ic_music, true),
    RING(AudioManager.STREAM_RING, "Ringtone", R.drawable.ic_ringtone, true),
    NOTIFICATIONS(AudioManager.STREAM_NOTIFICATION, "Notifications", R.drawable.ic_notifications, true),
    CALLS(AudioManager.STREAM_VOICE_CALL, "Calls", R.drawable.ic_call, false),
    SYSTEM(AudioManager.STREAM_SYSTEM, "System", R.drawable.ic_system, false),
    ALARM(AudioManager.STREAM_ALARM, "Alarm", R.drawable.ic_alarm, false),
}

data class StreamVolume(
    val level: Int,
    val max: Int,
    val muted: Boolean,
) {
    val fraction: Float
        get() = if (max <= 0) 0f else (level.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    val percent: Int get() = (fraction * 100f).roundToInt()
}

object VolumeRepository {

    private val _version = MutableStateFlow(0)
    val version = _version.asStateFlow()

    private var lastStream: VolStream = VolStream.MEDIA

    fun audioManager(context: Context): AudioManager? =
        context.applicationContext.getSystemService(AudioManager::class.java)

    fun read(context: Context, stream: VolStream): StreamVolume {
        val am = audioManager(context) ?: return StreamVolume(0, 1, false)
        return try {
            StreamVolume(
                level = am.getStreamVolume(stream.id),
                max = am.getStreamMaxVolume(stream.id).coerceAtLeast(1),
                muted = am.isStreamMute(stream.id),
            )
        } catch (_: Throwable) {
            StreamVolume(0, 1, false)
        }
    }

    fun activeStream(context: Context): VolStream {
        val am = audioManager(context)
        if (am != null && (am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION)) {
            return VolStream.CALLS
        }
        if (MediaRepository.state.value?.isPlaying == true) return VolStream.MEDIA
        return lastStream
    }

    fun set(context: Context, stream: VolStream, level: Int): StreamVolume {
        val am = audioManager(context) ?: return read(context, stream)
        lastStream = stream
        return try {
            val max = am.getStreamMaxVolume(stream.id).coerceAtLeast(1)
            am.setStreamVolume(stream.id, level.coerceIn(0, max), 0)
            bump(context)
            read(context, stream)
        } catch (_: Throwable) {
            read(context, stream)
        }
    }

    fun setFraction(context: Context, stream: VolStream, fraction: Float): StreamVolume {
        val state = read(context, stream)
        val level = (fraction.coerceIn(0f, 1f) * state.max).roundToInt()
        return set(context, stream, level)
    }

    fun step(context: Context, stream: VolStream, delta: Int): StreamVolume {
        val state = read(context, stream)
        return set(context, stream, state.level + delta)
    }

    fun toggleMute(context: Context, stream: VolStream): StreamVolume {
        val am = audioManager(context) ?: return read(context, stream)
        lastStream = stream
        return try {
            val muted = am.isStreamMute(stream.id)
            am.adjustStreamVolume(
                stream.id,
                if (muted) AudioManager.ADJUST_UNMUTE else AudioManager.ADJUST_MUTE,
                0,
            )
            bump(context)
            read(context, stream)
        } catch (_: Throwable) {
            read(context, stream)
        }
    }

    private fun bump(context: Context) {
        _version.value += 1
        PanelInteraction.touch()
        ShadeCardManager.requestRefresh(context)
    }
}

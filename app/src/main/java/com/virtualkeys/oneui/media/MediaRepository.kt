package com.virtualkeys.oneui.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.virtualkeys.oneui.notif.ShadeCardManager
import com.virtualkeys.oneui.service.MediaListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaState(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val speed: Float,
    val updatedAt: Long,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
) {
    /** Position interpolated to "now", so the seek bar moves while playing. */
    fun positionNow(): Long {
        val at = SystemClock.elapsedRealtime()
        val raw = if (isPlaying) {
            val delta = ((at - updatedAt).coerceAtLeast(0L) * speed).toLong()
            positionMs + delta
        } else {
            positionMs
        }
        val capped = if (durationMs > 0L) raw.coerceIn(0L, durationMs) else raw.coerceAtLeast(0L)
        return capped
    }
}

object MediaRepository {

    private const val TAG = "MediaRepository"

    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())

    private var manager: MediaSessionManager? = null
    private var listenerComponent: ComponentName? = null
    private var controller: MediaController? = null
    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    private val _state = MutableStateFlow<MediaState?>(null)
    val state = _state.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess = _hasAccess.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        manager = context.applicationContext.getSystemService(MediaSessionManager::class.java)
        listenerComponent = ComponentName(context.applicationContext, MediaListenerService::class.java)
        refreshAccess()
    }

    fun refreshAccess() {
        val ctx = appContext ?: return
        val granted = try {
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        } catch (_: Throwable) {
            false
        }
        _hasAccess.value = granted
        if (granted) connect() else detach()
    }

    fun onListenerConnected() {
        _hasAccess.value = true
        connect()
    }

    fun onListenerDisconnected() {
        _hasAccess.value = false
        detach()
    }

    private fun connect() {
        val mgr = manager ?: return
        val comp = listenerComponent ?: return

        if (sessionsListener == null) {
            val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                pick(controllers)
            }
            sessionsListener = listener
            try {
                mgr.addOnActiveSessionsChangedListener(listener, comp, handler)
            } catch (t: Throwable) {
                Log.w(TAG, "Cannot listen for media sessions", t)
            }
        }

        val controllers = try {
            mgr.getActiveSessions(comp)
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot read media sessions (notification access missing?)", t)
            null
        }
        pick(controllers)
    }

    private fun pick(controllers: List<MediaController>?) {
        val list = controllers.orEmpty()
        val chosen = list.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING ||
                it.playbackState?.state == PlaybackState.STATE_BUFFERING
        } ?: list.firstOrNull()

        if (chosen == null) {
            attach(null)
            return
        }
        if (chosen.sessionToken == controller?.sessionToken) {
            update()
            return
        }
        attach(chosen)
    }

    private fun detach() {
        controller?.unregisterCallback(callback)
        controller = null
        sessionsListener?.let { listener ->
            try {
                manager?.removeOnActiveSessionsChangedListener(listener)
            } catch (_: Throwable) {
            }
        }
        sessionsListener = null
        _state.value = null
        notifyCard()
    }

    private fun attach(next: MediaController?) {
        if (next?.sessionToken == controller?.sessionToken) {
            update()
            return
        }
        controller?.unregisterCallback(callback)
        controller = next
        if (next == null) {
            _state.value = null
            notifyCard()
            return
        }
        next.registerCallback(callback, handler)
        update()
        notifyCard()
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            update()
            notifyCard()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            update()
            notifyCard()
        }

        override fun onSessionDestroyed() {
            handler.post {
                controller?.unregisterCallback(this)
                controller = null
                connect()
            }
        }
    }

    private fun update() {
        val ctx = appContext ?: return
        val c = controller
        if (c == null) {
            _state.value = null
            return
        }
        val pb = c.playbackState
        val md = c.metadata
        val playing = pb?.state == PlaybackState.STATE_PLAYING ||
            pb?.state == PlaybackState.STATE_BUFFERING

        val appLabel = try {
            val info = ctx.packageManager.getApplicationInfo(c.packageName, 0)
            ctx.packageManager.getApplicationLabel(info).toString()
        } catch (_: Throwable) {
            c.packageName
        }

        val duration = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val title = md?.description?.title?.toString()?.takeIf { it.isNotBlank() }
            ?: md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: "Unknown track"
        val artist = md?.description?.subtitle?.toString()?.takeIf { it.isNotBlank() }
            ?: md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""
        val actions = pb?.actions ?: 0L
        val position = pb?.position ?: 0L
        val updatedAt = pb?.lastPositionUpdateTime?.takeIf { it > 0L } ?: SystemClock.elapsedRealtime()
        val speed = if (playing) (pb?.playbackSpeed ?: 1f).takeIf { it > 0f } ?: 1f else 1f

        _state.value = MediaState(
            packageName = c.packageName,
            appLabel = appLabel,
            title = title,
            artist = artist,
            art = art,
            isPlaying = playing,
            positionMs = position,
            durationMs = duration,
            speed = speed,
            updatedAt = updatedAt,
            hasNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            hasPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
        )
    }

    fun togglePlayPause() {
        val c = controller ?: return
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        try {
            if (playing) c.transportControls.pause() else c.transportControls.play()
        } catch (t: Throwable) {
            Log.w(TAG, "Transport failed", t)
            return
        }
        _state.value = _state.value?.let {
            it.copy(
                isPlaying = !playing,
                updatedAt = SystemClock.elapsedRealtime(),
            )
        }
        notifyCard()
        handler.postDelayed({ update(); notifyCard() }, 450)
    }

    fun skipNext() {
        try {
            controller?.transportControls?.skipToNext()
        } catch (t: Throwable) {
            Log.w(TAG, "Skip next failed", t)
        }
        notifyCard()
        handler.postDelayed({ update(); notifyCard() }, 550)
    }

    fun skipPrevious() {
        try {
            controller?.transportControls?.skipToPrevious()
        } catch (t: Throwable) {
            Log.w(TAG, "Skip previous failed", t)
        }
        notifyCard()
        handler.postDelayed({ update(); notifyCard() }, 550)
    }

    fun seekTo(positionMs: Long) {
        try {
            controller?.transportControls?.seekTo(positionMs)
        } catch (t: Throwable) {
            Log.w(TAG, "Seek failed", t)
        }
        _state.value = _state.value?.copy(
            positionMs = positionMs,
            updatedAt = SystemClock.elapsedRealtime(),
        )
        notifyCard()
    }

    fun openSourceApp() {
        val ctx = appContext ?: return
        val pkg = _state.value?.packageName ?: return
        try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot open $pkg", t)
        }
    }

    private fun notifyCard() {
        ShadeCardManager.requestRefresh(appContext)
    }
}

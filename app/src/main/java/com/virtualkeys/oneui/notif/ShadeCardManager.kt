package com.virtualkeys.oneui.notif

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.virtualkeys.oneui.R
import com.virtualkeys.oneui.audio.StreamVolume
import com.virtualkeys.oneui.audio.VolStream
import com.virtualkeys.oneui.audio.VolumeRepository
import com.virtualkeys.oneui.media.MediaRepository
import com.virtualkeys.oneui.media.MediaState
import com.virtualkeys.oneui.settings.SettingsStore
import com.virtualkeys.oneui.ui.PanelActivity

object ShadeCardManager {

    private const val CHANNEL_ID = "oneui_controls_card"
    private const val NOTIFICATION_ID = 4242

    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRefresh = false
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        createChannel(context.applicationContext)
        initialized = true
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.shade_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.shade_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun requestRefresh(context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return
        if (!initialized) {
            appContext = ctx
            createChannel(ctx)
            initialized = true
        }
        if (pendingRefresh) return
        pendingRefresh = true
        handler.postDelayed({
            pendingRefresh = false
            refresh()
        }, 120L)
    }

    fun refreshNow() {
        handler.removeCallbacksAndMessages(null)
        pendingRefresh = false
        refresh()
    }

    @SuppressLint("MissingPermission")
    private fun refresh() {
        val ctx = appContext ?: return
        if (!SettingsStore.state.value.showShadeCard) {
            cancel()
            return
        }
        if (!canPostNotifications(ctx)) return

        val media = MediaRepository.state.value
        val volume = VolumeRepository.read(ctx, VolStream.MEDIA)
        val notification = build(ctx, media, volume)
        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {
            // Notifications blocked or permission revoked; the app still works.
        }
    }

    fun cancel() {
        val ctx = appContext ?: return
        try {
            NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID)
        } catch (_: Throwable) {
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun build(context: Context, media: MediaState?, volume: StreamVolume): android.app.Notification {
        val compact = RemoteViews(context.packageName, R.layout.notification_card_compact)
        val full = RemoteViews(context.packageName, R.layout.notification_card)

        val title = media?.title ?: context.getString(R.string.notification_card_title)
        val subtitle = when {
            media == null -> context.getString(R.string.no_media)
            media.artist.isNotBlank() -> media.artist
            else -> media.appLabel
        }

        // Rows shared by the compact and expanded layouts.
        listOf(compact, full).forEach { views ->
            views.setTextViewText(R.id.title, title)
            views.setTextViewText(R.id.artist, subtitle)
            views.setTextViewText(R.id.vol_pct, "${volume.percent}%")
            views.setProgressBar(R.id.volume_bar, volume.max, volume.level, false)
            views.setOnClickPendingIntent(R.id.vol_down, action(context, ShadeActionReceiver.ACTION_VOL_DOWN, 101))
            views.setOnClickPendingIntent(R.id.vol_up, action(context, ShadeActionReceiver.ACTION_VOL_UP, 102))

            if (media?.art != null) {
                views.setImageViewBitmap(R.id.art, smallBitmap(media.art))
            } else {
                views.setImageViewResource(R.id.art, R.drawable.ic_music)
            }
        }

        full.setViewVisibility(
            R.id.media_section,
            if (media != null) View.VISIBLE else View.GONE,
        )
        full.setImageViewResource(
            R.id.vol_mute,
            if (volume.muted) R.drawable.ic_mute else R.drawable.ic_volume_down,
        )
        full.setOnClickPendingIntent(R.id.vol_mute, action(context, ShadeActionReceiver.ACTION_VOL_MUTE, 103))

        if (media != null) {
            full.setTextViewText(R.id.app_label, media.appLabel)
            full.setImageViewResource(R.id.play, if (media.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            full.setOnClickPendingIntent(R.id.play, action(context, ShadeActionReceiver.ACTION_PLAY_PAUSE, 104))
            full.setOnClickPendingIntent(R.id.next, action(context, ShadeActionReceiver.ACTION_NEXT, 105))
            full.setOnClickPendingIntent(R.id.prev, action(context, ShadeActionReceiver.ACTION_PREV, 106))
        }

        val panelIntent = Intent(context, PanelActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val panelPending = PendingIntent.getActivity(
            context,
            201,
            panelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        compact.setOnClickPendingIntent(R.id.art, panelPending)
        compact.setOnClickPendingIntent(R.id.title, panelPending)
        full.setOnClickPendingIntent(R.id.art, panelPending)
        full.setOnClickPendingIntent(R.id.title, panelPending)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setCustomContentView(compact)
            .setCustomBigContentView(full)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(panelPending)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        return builder.build()
    }

    private fun action(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ShadeActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Keeps the notification transaction well under the binder limit. */
    private fun smallBitmap(source: android.graphics.Bitmap): android.graphics.Bitmap {
        val maxSide = 320
        val width = source.width
        val height = source.height
        if (width <= maxSide && height <= maxSide) return source
        val scale = maxSide.toFloat() / maxOf(width, height).toFloat()
        val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
        return android.graphics.Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
    }
}

package com.virtualkeys.oneui.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualkeys.oneui.R
import com.virtualkeys.oneui.audio.AudioOutput
import com.virtualkeys.oneui.audio.VolStream
import com.virtualkeys.oneui.audio.VolumeRepository
import com.virtualkeys.oneui.media.MediaRepository
import com.virtualkeys.oneui.media.MediaState
import com.virtualkeys.oneui.settings.AppSettings
import com.virtualkeys.oneui.settings.SettingsStore
import com.virtualkeys.oneui.ui.theme.InnerCardShape
import com.virtualkeys.oneui.ui.theme.LocalOneUi
import com.virtualkeys.oneui.ui.theme.MediaCardShape
import com.virtualkeys.oneui.ui.theme.OneUiTheme
import com.virtualkeys.oneui.ui.theme.PanelShape
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PanelScreen(
    onDismiss: () -> Unit,
    inOverlay: Boolean = false,
    onOpenSettings: (() -> Unit)? = null,
    autoDismiss: Boolean = true,
) {
    val context = LocalContext.current
    val settings by SettingsStore.state.collectAsState()
    val media by MediaRepository.state.collectAsState()
    val interaction by PanelInteraction.tick.collectAsState()

    val dark = when (settings.themeMode) {
        1 -> true
        2 -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    OneUiTheme(dark = dark, accentIndex = settings.accentIndex) {
        val palette = LocalOneUi.current
        val density = LocalDensity.current
        val statusTop = WindowInsets.statusBars.getTop(density)
        val topPadding = maxOf(with(density) { statusTop.toDp() }, 34.dp)

        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            shown = true
            PanelInteraction.touch()
        }

        if (autoDismiss) {
            LaunchedEffect(interaction, settings.autoDismissSeconds) {
                delay(settings.autoDismissSeconds * 1000L)
                onDismiss()
            }
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            )

            AnimatedVisibility(
                visible = shown,
                enter = slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = tween(260)) +
                    fadeIn(tween(180)),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = tween(180)) +
                    fadeOut(tween(140)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = topPadding),
                ) {
                    ControlPanel(
                        media = media,
                        settings = settings,
                        context = context,
                        onDismiss = onDismiss,
                        onOpenSettings = onOpenSettings,
                        inOverlay = inOverlay,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    media: MediaState?,
    settings: AppSettings,
    context: Context,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    inOverlay: Boolean,
) {
    val palette = LocalOneUi.current
    var expanded by remember(settings.showAllStreams) { mutableStateOf(settings.showAllStreams) }

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(28.dp, PanelShape, clip = false)
            .clip(PanelShape)
            .background(palette.panel)
            .border(1.dp, palette.divider, PanelShape)
            .pointerInput(Unit) { detectTapGestures { PanelInteraction.touch() } }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(palette.secondary.copy(alpha = 0.35f))
                .align(Alignment.CenterHorizontally),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Controls",
                    color = palette.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (inOverlay) "Volume keys" else "Quick panel",
                    color = palette.secondary,
                    fontSize = 11.sp,
                )
            }
            if (onOpenSettings != null) {
                TinyIconButton(
                    iconRes = R.drawable.ic_gear,
                    tint = palette.secondary,
                    contentDescription = "Settings",
                    onClick = {
                        PanelInteraction.touch()
                        onOpenSettings()
                    },
                )
                Spacer(Modifier.width(2.dp))
            }
            TinyIconButton(
                iconRes = R.drawable.ic_close,
                tint = palette.secondary,
                contentDescription = "Close",
                onClick = onDismiss,
            )
        }

        if (media != null) {
            MediaCard(media = media, context = context)
        } else {
            EmptyMediaCard()
        }

        Spacer(Modifier.height(10.dp))

        val activeStream = VolumeRepository.activeStream(context)
        val output = AudioOutput.current(context)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Volume",
                color = palette.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        openMediaOutput(context)
                        onDismiss()
                    }
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(output.iconRes),
                    contentDescription = "Media output",
                    tint = palette.secondary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = output.label,
                    color = palette.secondary,
                    fontSize = 11.sp,
                )
            }
        }

        val streams = if (expanded) {
            VolStream.entries.toList()
        } else {
            VolStream.entries.filter { it.isPrimary }
        }.let { base ->
            if (base.contains(activeStream)) base else base + activeStream
        }

        Column(Modifier.padding(top = 2.dp)) {
            streams.forEach { stream ->
                VolumeRow(
                    stream = stream,
                    context = context,
                    haptics = settings.haptics,
                )
            }
        }

        if (!settings.showAllStreams) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        expanded = !expanded
                        PanelInteraction.touch()
                    }
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "Show fewer streams" else "More streams",
                    color = palette.secondary,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_expand),
                    contentDescription = null,
                    tint = palette.secondary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            }
        }

        Text(
            text = if (settings.interceptVolumeKeys) {
                "Tap a speaker icon to mute · Hold a volume key for mute"
            } else {
                "Add the Controls tile to your quick panel"
            },
            color = palette.secondary.copy(alpha = 0.7f),
            fontSize = 9.5.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyMediaCard() {
    val palette = LocalOneUi.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MediaCardShape)
            .background(palette.card)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.track),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music),
                contentDescription = null,
                tint = palette.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "No media playing",
                color = palette.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Start music to control it from here",
                color = palette.secondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun MediaCard(media: MediaState, context: Context) {
    val palette = LocalOneUi.current
    val duration = media.durationMs.coerceAtLeast(1L)
    var tick by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(media.isPlaying, media.title) {
        while (media.isPlaying) {
            delay(500L)
            tick += 1
        }
    }

    val positionMs = if (dragging) {
        (dragFraction * duration).toLong()
    } else {
        remember(media, tick) { media.positionNow() }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(MediaCardShape)
            .background(palette.card)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.track)
                    .clickable {
                        PanelInteraction.touch()
                        MediaRepository.openSourceApp()
                    },
                contentAlignment = Alignment.Center,
            ) {
                val art: Bitmap? = media.art
                if (art != null) {
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_music),
                        contentDescription = null,
                        tint = palette.secondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    color = palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (media.artist.isNotBlank()) media.artist else media.appLabel,
                    color = palette.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (media.artist.isNotBlank()) {
                    Text(
                        text = media.appLabel,
                        color = palette.secondary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            TinyIconButton(
                iconRes = R.drawable.ic_chevron,
                tint = palette.secondary,
                contentDescription = "Open app",
                onClick = {
                    PanelInteraction.touch()
                    MediaRepository.openSourceApp()
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTime(positionMs),
                color = palette.secondary,
                fontSize = 10.sp,
                modifier = Modifier.width(36.dp),
            )
            SeekBar(
                fraction = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                onSeek = { fraction ->
                    dragging = true
                    dragFraction = fraction
                    PanelInteraction.touch()
                },
                onFinished = {
                    if (dragging) {
                        MediaRepository.seekTo((dragFraction * duration).toLong())
                        dragging = false
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )
            Text(
                text = formatTime(duration),
                color = palette.secondary,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp),
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TinyIconButton(
                iconRes = R.drawable.ic_prev,
                tint = palette.text,
                size = 42.dp,
                contentDescription = "Previous",
                onClick = {
                    MediaRepository.skipPrevious()
                    PanelInteraction.touch()
                },
            )
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(palette.accent)
                    .clickable {
                        MediaRepository.togglePlayPause()
                        PanelInteraction.touch()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (media.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    ),
                    contentDescription = if (media.isPlaying) "Pause" else "Play",
                    tint = palette.onAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            TinyIconButton(
                iconRes = R.drawable.ic_next,
                tint = palette.text,
                size = 42.dp,
                contentDescription = "Next",
                onClick = {
                    MediaRepository.skipNext()
                    PanelInteraction.touch()
                },
            )
        }
    }
}

@Composable
private fun VolumeRow(
    stream: VolStream,
    context: Context,
    haptics: Boolean,
) {
    val palette = LocalOneUi.current
    val haptic = LocalHapticFeedback.current
    val version by VolumeRepository.version.collectAsState()
    val state = remember(version, stream, context) {
        VolumeRepository.read(context, stream)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TinyIconButton(
            iconRes = if (state.muted) R.drawable.ic_mute else stream.iconRes,
            tint = if (state.muted) palette.accent else palette.secondary,
            size = 30.dp,
            contentDescription = if (state.muted) "Unmute ${stream.label}" else "Mute ${stream.label}",
            onClick = {
                VolumeRepository.toggleMute(context, stream)
                PanelInteraction.touch()
            },
        )
        Spacer(Modifier.width(6.dp))
        OneUiSlider(
            value = state.fraction,
            onValueChange = { fraction ->
                val before = VolumeRepository.read(context, stream).level
                VolumeRepository.setFraction(context, stream, fraction)
                if (haptics) {
                    val after = VolumeRepository.read(context, stream).level
                    if (after != before) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
            onFinished = { PanelInteraction.touch() },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${state.percent}%",
            color = palette.secondary,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
private fun OneUiSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalOneUi.current
    var width by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val thumbWidth = 5.dp

    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.track)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.x / width).coerceIn(0f, 1f))
                    onFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onValueChange((offset.x / width).coerceIn(0f, 1f))
                    },
                    onDragEnd = { onFinished() },
                    onDragCancel = { onFinished() },
                ) { change, _ ->
                    change.consume()
                    onValueChange((change.position.x / width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .background(palette.accent.copy(alpha = 0.85f)),
        )
        Box(
            Modifier
                .offset {
                    IntOffset(
                        ((width - with(density) { thumbWidth.toPx() }) * value.coerceIn(0f, 1f))
                            .roundToInt(),
                        0,
                    )
                }
                .width(thumbWidth)
                .height(14.dp)
                .clip(CircleShape)
                .background(palette.text.copy(alpha = 0.9f)),
        )
    }
}

@Composable
private fun SeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalOneUi.current
    var width by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val thumb = 10.dp

    Box(
        modifier
            .height(22.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / width).coerceIn(0f, 1f))
                    onFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onSeek((offset.x / width).coerceIn(0f, 1f)) },
                    onDragEnd = { onFinished() },
                    onDragCancel = { onFinished() },
                ) { change, _ ->
                    change.consume()
                    onSeek((change.position.x / width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(palette.track),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(CircleShape)
                .background(palette.accent),
        )
        Box(
            Modifier
                .offset {
                    IntOffset(
                        ((width * fraction.coerceIn(0f, 1f)) - with(density) { thumb.toPx() } / 2f)
                            .roundToInt()
                            .coerceAtLeast(0),
                        0,
                    )
                }
                .size(thumb)
                .clip(CircleShape)
                .background(palette.text),
        )
    }
}

@Composable
private fun TinyIconButton(
    iconRes: Int,
    tint: Color,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes >= 60L) {
        "%d:%02d:%02d".format(minutes / 60L, minutes % 60L, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun openMediaOutput(context: Context) {
    try {
        val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Throwable) {
    }
}

package com.sksdesign.sharkaudio.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import com.sksdesign.sharkaudio.MainActivity
import com.sksdesign.sharkaudio.R
import com.sksdesign.sharkaudio.data.Track

class PlayerNotificationController(private val context: Context) {
    companion object {
        const val ACTION_PREVIOUS = "com.sksdesign.sharkaudio.action.PREVIOUS"
        const val ACTION_TOGGLE = "com.sksdesign.sharkaudio.action.TOGGLE"
        const val ACTION_NEXT = "com.sksdesign.sharkaudio.action.NEXT"
        private const val CHANNEL_ID = "sharkaudio_playback"
        private const val NOTIFICATION_ID = 2601
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private var mediaSessionToken: MediaSessionCompat.Token? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Shark Audio playback", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Quick controls for the active Shark Audio player"
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun setMediaSession(token: MediaSessionCompat.Token) {
        mediaSessionToken = token
    }

    fun update(track: Track?, isPlaying: Boolean, positionMs: Long = 0L, durationMs: Long = 0L) {
        if (track == null) {
            cancel()
            return
        }
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingIntentFlags()
        )
        val previous = PendingIntent.getBroadcast(context, 1, Intent(context, PlayerActionReceiver::class.java).setAction(ACTION_PREVIOUS), pendingIntentFlags())
        val toggle = PendingIntent.getBroadcast(context, 2, Intent(context, PlayerActionReceiver::class.java).setAction(ACTION_TOGGLE), pendingIntentFlags())
        val next = PendingIntent.getBroadcast(context, 3, Intent(context, PlayerActionReceiver::class.java).setAction(ACTION_NEXT), pendingIntentFlags())
        val largeIcon = notificationLogo()
        val safeDuration = durationMs.takeIf { it > 0L } ?: track.durationMs
        val progressMax = safeDuration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(0)
        val progressValue = positionMs.coerceIn(0L, safeDuration.coerceAtLeast(0L)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val style = MediaStyle().setShowActionsInCompactView(0, 1, 2).also { mediaSessionToken?.let(it::setMediaSession) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_clean)
            .setContentTitle(track.title)
            .setContentText(listOf(track.album, track.folder).firstOrNull { it.isNotBlank() } ?: track.artist)
            .setSubText(track.artist)
            .setLargeIcon(largeIcon)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_skip_previous, "Previous", previous)
            .addAction(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow, if (isPlaying) "Pause" else "Play", toggle)
            .addAction(R.drawable.ic_skip_next, "Next", next)
            .setProgress(progressMax, progressValue, false)
            .setStyle(style)
            .build()

        runCatching { notificationManager.notify(NOTIFICATION_ID, notification) }
    }

    fun cancel() {
        runCatching { notificationManager.cancel(NOTIFICATION_ID) }
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    private fun notificationLogo() = BitmapFactory.decodeResource(context.resources, R.drawable.logo_clean)
}

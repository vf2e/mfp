package com.guotu.app.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guotu.app.R
import com.guotu.app.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.update_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.update_channel_desc)
        }
        mgr.createNotificationChannel(channel)
    }

    fun notifyUpdateAvailable(update: AvailableUpdate) {
        ensureChannel()
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_SHOW_UPDATE, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            REQ_OPEN_UPDATE,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(context.getString(R.string.update_notif_title))
            .setContentText(
                context.getString(R.string.update_notif_body, update.versionName),
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildString {
                        append(context.getString(R.string.update_notif_body, update.versionName))
                        if (update.notes.isNotBlank()) {
                            append('\n')
                            append(update.notes)
                        }
                    },
                ),
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        }
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
    }

    companion object {
        const val CHANNEL_ID = "app_updates"
        private const val NOTIF_ID = 41001
        private const val REQ_OPEN_UPDATE = 41002
    }
}

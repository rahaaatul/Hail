package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.receiver.BulkOperationCancelReceiver
import java.util.concurrent.atomic.AtomicBoolean

object BulkOperationNotifier {
    const val ACTION_CANCEL = "com.aistra.hail.ACTION_BULK_OP_CANCEL"

    private val cancelFlag = AtomicBoolean(false)
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var isFreezing = true

    fun start(context: Context, total: Int, frozen: Boolean) {
        cancelFlag.set(false)
        isFreezing = frozen

        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return
            }
        }

        val titleRes = if (frozen) R.string.bulk_freeze_progress else R.string.bulk_unfreeze_progress
        val smallIconRes = if (frozen) R.drawable.ic_round_frozen else R.drawable.ic_round_unfrozen

        val cancelIntent = Intent(context, BulkOperationCancelReceiver::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationBuilder = NotificationCompat.Builder(context, HailApp.BULK_OP_CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(context.getString(titleRes))
            .setContentText("0/$total")
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.action_cancel),
                cancelPendingIntent
            )

        notifySafely(context, notificationBuilder!!.build())
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(context: Context, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(HailApp.BULK_OP_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Fallback to toast if notification fails
            HUI.showToast(context.getString(R.string.operation_failed, e.message))
        }
    }

    fun update(current: Int, total: Int, appName: String, appIcon: Bitmap?) {
        val verb = if (isFreezing) "Freezing" else "Unfreezing"
        notificationBuilder?.apply {
            val progressPercent = if (total > 0) (current * 100 / total) else 0
            setContentTitle("$verb $appName")
            setContentText("$current/$total")
            setProgress(100, progressPercent, false)
            if (appIcon != null) {
                setLargeIcon(appIcon)
            }
            notifySafely(HailApp.app, build())
        }
    }

    fun complete(success: Int, failed: Int, frozen: Boolean) {
        val titleRes = if (frozen) R.string.bulk_freeze_complete else R.string.bulk_unfreeze_complete

        notificationBuilder = notificationBuilder?.apply {
            setOngoing(false)
            setProgress(0, 0, false)
            setContentTitle(HailApp.app.getString(titleRes, success, failed))
            setContentText("")
            setLargeIcon((null as Bitmap?))
            setAutoCancel(true)
            // Remove cancel action by rebuilding without it
            mActions.clear()
        }

        notificationBuilder?.let {
            notifySafely(HailApp.app, it.build())
        }
    }

    fun cancel() {
        cancelFlag.set(true)
        NotificationManagerCompat.from(HailApp.app).cancel(HailApp.BULK_OP_NOTIFICATION_ID)
    }

    val isCancelled: Boolean
        get() = cancelFlag.get()

    fun createCancelPendingIntent(context: Context): PendingIntent {
        val cancelIntent = Intent(context, BulkOperationCancelReceiver::class.java).apply {
            action = ACTION_CANCEL
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
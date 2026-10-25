package de.penguin.sensorlab.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.penguin.sensorlab.R

/** Posts the "Strong shock detected" notification (spec section 3). */
class ShockNotifier(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        )
            .setName("Shock alerts")
            .setDescription("Strong shock detected by the accelerometer")
            .build()
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * True if the app may post notifications. ContextCompat also covers Android < 13,
     * where POST_NOTIFICATIONS doesn't exist and the app-level notification setting counts.
     */
    @SuppressLint("InlinedApi") // safe on Android < 13, see above
    fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED && notificationManager.areNotificationsEnabled()

    /**
     * Posts the notification. Returns false if notifications are blocked.
     *
     * @param text the details shown below the title, e.g. time and |a|.
     */
    // The permission is checked with canNotify() right before notify().
    @SuppressLint("MissingPermission")
    fun notifyShock(text: String): Boolean {
        if (!canNotify()) return false
        // Tapping the notification brings the app to the front like the launcher icon does,
        // so the open screen and its ViewModel are reused instead of a second screen being created.
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shock)
            .setContentTitle("Strong shock detected")
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private companion object {
        const val CHANNEL_ID = "shock_alerts"
        const val NOTIFICATION_ID = 1
    }
}

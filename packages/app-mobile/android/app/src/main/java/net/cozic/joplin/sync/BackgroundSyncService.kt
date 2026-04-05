package net.cozic.joplin.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import net.cozic.joplin.MainActivity

class BackgroundSyncService : Service() {

	companion object {
		const val CHANNEL_ID = "joplin_background_sync"
		const val NOTIFICATION_ID = 9274

		private fun launchIntent(context: Context): PendingIntent {
			val intent = Intent(context, MainActivity::class.java).apply {
				flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
			}
			return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
		}

		fun updateNotification(context: Context, syncing: Boolean) {
			val icon = if (syncing)
				android.R.drawable.ic_popup_sync
			else
				android.R.drawable.stat_notify_sync_noanim
			val text = if (syncing) "Syncing..." else "Keep-alive"

			val notification = Notification.Builder(context, CHANNEL_ID)
				.setContentTitle("Joplin")
				.setContentText(text)
				.setSmallIcon(icon)
				.setContentIntent(launchIntent(context))
				.setOngoing(true)
				.build()

			val manager = context.getSystemService(NotificationManager::class.java)
			manager.notify(NOTIFICATION_ID, notification)
		}
	}

	override fun onCreate() {
		super.onCreate()
		createNotificationChannel()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val notification = Notification.Builder(this, CHANNEL_ID)
			.setContentTitle("Joplin")
			.setContentText("Keep-alive")
			.setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
			.setContentIntent(launchIntent(this))
			.setOngoing(true)
			.build()

		ServiceCompat.startForeground(
			this, NOTIFICATION_ID, notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
		)
		return START_STICKY
	}

	override fun onBind(intent: Intent?): IBinder? = null

	private fun createNotificationChannel() {
		val channel = NotificationChannel(
			CHANNEL_ID,
			"Background Sync",
			NotificationManager.IMPORTANCE_MIN,
		)
		channel.description = "Keeps Joplin running for periodic sync"
		val manager = getSystemService(NotificationManager::class.java)
		manager.createNotificationChannel(channel)
	}
}

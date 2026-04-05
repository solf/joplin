package net.cozic.joplin.sync

import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import com.facebook.react.jstasks.HeadlessJsTaskContext
import com.facebook.react.uimanager.ViewManager

class BackgroundSyncPackage : ReactPackage {
	override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
		return listOf(BackgroundSyncModule(reactContext))
	}

	override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
		return emptyList()
	}
}

class BackgroundSyncModule(
	private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {

	private var wakeLock: PowerManager.WakeLock? = null
	private var headlessTaskId: Int = -1

	override fun getName(): String = "BackgroundSyncModule"

	@ReactMethod
	fun startService() {
		if (headlessTaskId >= 0) return
		try {
			val intent = Intent(reactContext, BackgroundSyncService::class.java)
			ContextCompat.startForegroundService(reactContext, intent)
			Log.d("BackgroundSync", "Foreground service started")

			// Keep RN's JS timer system alive while backgrounded.
			// Without this, JavaTimerManager.onHostPause() pauses all JS timers.
			UiThreadUtil.runOnUiThread {
				try {
					val config = HeadlessJsTaskConfig(
						"JoplinBackgroundSync",
						Arguments.createMap(),
						0,
						true,
					)
					headlessTaskId = HeadlessJsTaskContext.getInstance(reactContext).startTask(config)
					Log.d("BackgroundSync", "Headless JS task started: $headlessTaskId")
				} catch (e: Exception) {
					Log.e("BackgroundSync", "Failed to start headless JS task", e)
				}
			}
		} catch (e: Exception) {
			Log.e("BackgroundSync", "Failed to start foreground service", e)
		}
	}

	@ReactMethod
	fun stopService() {
		try {
			if (headlessTaskId >= 0) {
				HeadlessJsTaskContext.getInstance(reactContext).finishTask(headlessTaskId)
				Log.d("BackgroundSync", "Headless JS task finished: $headlessTaskId")
				headlessTaskId = -1
			}
			val intent = Intent(reactContext, BackgroundSyncService::class.java)
			reactContext.stopService(intent)
			Log.d("BackgroundSync", "Foreground service stopped")
		} catch (e: Exception) {
			Log.e("BackgroundSync", "Failed to stop foreground service", e)
		}
	}

	@ReactMethod
	fun acquireWakeLock() {
		try {
			if (wakeLock?.isHeld == true) return
			val pm = reactContext.getSystemService(PowerManager::class.java)
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "joplin:sync").apply {
				acquire()
			}
			BackgroundSyncService.updateNotification(reactContext, syncing = true)
			Log.d("BackgroundSync", "Wake lock acquired")
		} catch (e: Exception) {
			Log.e("BackgroundSync", "Failed to acquire wake lock", e)
		}
	}

	@ReactMethod
	fun releaseWakeLock() {
		try {
			wakeLock?.let {
				if (it.isHeld) {
					it.release()
					Log.d("BackgroundSync", "Wake lock released")
				}
			}
			wakeLock = null
			BackgroundSyncService.updateNotification(reactContext, syncing = false)
		} catch (e: Exception) {
			Log.e("BackgroundSync", "Failed to release wake lock", e)
		}
	}
}

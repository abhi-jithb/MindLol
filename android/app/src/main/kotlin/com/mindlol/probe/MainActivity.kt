package com.mindlol.probe

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import com.mindlol.probe.overlay.BlockOverlayManager
import com.mindlol.probe.repository.ReelCounterRepository
import com.mindlol.probe.repository.ReelCounterRepositoryImpl
import com.mindlol.probe.repository.SharedPreferencesStore
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private lateinit var repository: ReelCounterRepository
    private lateinit var overlayManager: BlockOverlayManager
    private var eventSink: EventChannel.EventSink? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        repository = ReelCounterRepositoryImpl(SharedPreferencesStore(this))
        overlayManager = BlockOverlayManager(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getMindLolStatus" -> {
                    result.success(getStatusMap())
                }
                "getTodayCount" -> {
                    result.success(repository.getTodayCount())
                }
                "getDailyLimit" -> {
                    result.success(repository.getDailyLimit())
                }
                "setDailyLimit" -> {
                    val limit = call.argument<Int>("limit")
                    if (limit != null) {
                        repository.setDailyLimit(limit)
                        notifyStatusUpdate()
                        result.success(getStatusMap())
                    } else {
                        result.error("INVALID_ARGUMENT", "limit parameter must be an integer", null)
                    }
                }
                "resetTodayCount" -> {
                    repository.resetTodayCount()
                    notifyStatusUpdate()
                    result.success(getStatusMap())
                }
                "isLimitReached" -> {
                    result.success(repository.isLimitReached())
                }
                "isAccessibilityEnabled" -> {
                    result.success(isAccessibilityServiceEnabled())
                }
                "isOverlayPermissionGranted" -> {
                    result.success(overlayManager.hasOverlayPermission())
                }
                "getAppConfiguration" -> {
                    result.success(
                        mapOf(
                            "instagramEnabled" to repository.isInstagramEnabled(),
                            "youtubeEnabled" to repository.isYouTubeEnabled()
                        )
                    )
                }
                "setAppConfiguration" -> {
                    val instaEnabled = call.argument<Boolean>("instagramEnabled")
                    val ytEnabled = call.argument<Boolean>("youtubeEnabled")
                    if (instaEnabled != null) repository.setInstagramEnabled(instaEnabled)
                    if (ytEnabled != null) repository.setYouTubeEnabled(ytEnabled)
                    notifyStatusUpdate()
                    result.success(getStatusMap())
                }
                "getBlockingMode" -> {
                    result.success(repository.getBlockingMode())
                }
                "openAccessibilitySettings" -> {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                    result.success(true)
                }
                "openOverlaySettings" -> {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler, StatusChangeListener {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    eventSink = events
                    StatusBroadcaster.addListener(this)
                    notifyStatusUpdate()
                }

                override fun onCancel(arguments: Any?) {
                    StatusBroadcaster.removeListener(this)
                    eventSink = null
                }

                override fun onStatusChanged() {
                    notifyStatusUpdate()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            notifyStatusUpdate()
        }
    }

    private fun getStatusMap(): Map<String, Any> {
        return mapOf(
            "todayCount" to repository.getTodayCount(),
            "dailyLimit" to repository.getDailyLimit(),
            "limitReached" to repository.isLimitReached(),
            "accessibilityEnabled" to isAccessibilityServiceEnabled(),
            "overlayPermissionGranted" to overlayManager.hasOverlayPermission(),
            "instagramEnabled" to repository.isInstagramEnabled(),
            "youtubeEnabled" to repository.isYouTubeEnabled(),
            "blockingMode" to repository.getBlockingMode()
        )
    }

    private fun notifyStatusUpdate() {
        eventSink?.success(getStatusMap())
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/${DebugProbeService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    companion object {
        const val METHOD_CHANNEL = "mindlol/platform"
        const val EVENT_CHANNEL = "mindlol/events"
    }
}

package com.mindlol.probe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mindlol.probe.repository.ReelCounterRepository
import com.mindlol.probe.repository.ReelCounterRepositoryImpl
import com.mindlol.probe.repository.SharedPreferencesStore
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var repository: ReelCounterRepository
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = ReelCounterRepositoryImpl(SharedPreferencesStore(this))

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val titleView = TextView(this).apply {
            text = "MindLol"
            textSize = 24f
            setPadding(0, 0, 0, 8)
        }
        layout.addView(titleView)

        val subtitleView = TextView(this).apply {
            text = "Digital Wellbeing & Short-Video Limit Engine"
            textSize = 14f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(subtitleView)

        statusTextView = TextView(this).apply {
            textSize = 14f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(statusTextView)

        val overlayPermissionBtn = Button(this).apply {
            text = "Grant Overlay Permission"
            setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        layout.addView(overlayPermissionBtn)

        val settingsBtn = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(settingsBtn)

        val setLimitBtn = Button(this).apply {
            text = "Toggle Daily Limit (5 / 20)"
            setOnClickListener {
                val current = repository.getDailyLimit()
                val next = if (current == 20) 5 else 20
                repository.setDailyLimit(next)
                updateStatusText()
            }
        }
        layout.addView(setLimitBtn)

        val resetCountBtn = Button(this).apply {
            text = "Reset Today's Count"
            setOnClickListener {
                repository.resetTodayCount()
                updateStatusText()
            }
        }
        layout.addView(resetCountBtn)

        val refreshBtn = Button(this).apply {
            text = "Refresh Status"
            setOnClickListener {
                updateStatusText()
            }
        }
        layout.addView(refreshBtn)

        setContentView(layout)
        updateStatusText()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
    }

    private fun updateStatusText() {
        val todayCount = repository.getTodayCount()
        val dailyLimit = repository.getDailyLimit()
        val isReached = repository.isLimitReached()
        val hasOverlayPermission = Settings.canDrawOverlays(this)

        val debugLogsInfo = if (BuildConfig.DEBUG) {
            val externalDir = getExternalFilesDir(null) ?: filesDir
            val counterLogFile = File(externalDir, "counter_events.log")
            val detectorLogFile = File(externalDir, "detector_events.log")
            """
                
                DEVELOPER DIAGNOSTICS (DEBUG BUILD):
                Counter Log: ${counterLogFile.absolutePath}
                Detector Log: ${detectorLogFile.absolutePath}
            """.trimIndent()
        } else ""

        statusTextView.text = """
            APPLICATION STATUS:
            Overlay Permission: ${if (hasOverlayPermission) "GRANTED" else "DENIED"}
            Today's Reel Count: $todayCount / $dailyLimit
            Limit Status: ${if (isReached) "LIMIT REACHED" else "ACTIVE"}
            $debugLogsInfo
        """.trimIndent()
    }
}

package com.mindlol.probe

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val titleView = TextView(this).apply {
            text = "MindLol Debug Accessibility Probe"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }
        layout.addView(titleView)

        val externalDir = getExternalFilesDir(null) ?: filesDir
        val treeLogFile = File(externalDir, "tree_dump.log")
        val detectorLogFile = File(externalDir, "detector_events.log")

        val infoView = TextView(this).apply {
            text = "Status: Debug Probe & Detector Active\nTarget Apps: Instagram & YouTube Shorts\n\nDetector Log:\n${detectorLogFile.absolutePath}\n\nTree Dump Log:\n${treeLogFile.absolutePath}"
            textSize = 13f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(infoView)

        val settingsBtn = Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(settingsBtn)

        setContentView(layout)
    }
}

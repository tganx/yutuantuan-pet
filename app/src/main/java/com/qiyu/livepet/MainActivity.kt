package com.qiyu.livepet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 引导页：检查悬浮窗权限 + 通知权限，然后拉起 OverlayService。
 * 权限这类操作系统不给全自动，所以这里只负责把该点的按钮摆到你面前。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var permBtn: Button
    private lateinit var startBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        permBtn = findViewById(R.id.permBtn)
        startBtn = findViewById(R.id.startBtn)

        // 通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        permBtn.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        startBtn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                statusText.text = "还没给悬浮窗权限哦，先点上面的按钮。"
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java)
            )
            statusText.text = "粉团子已经出来了，去别的 App 找找它吧。"
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val canOverlay = Settings.canDrawOverlays(this)
        statusText.text = if (canOverlay) {
            "悬浮窗权限：已授予。可以召唤了。"
        } else {
            "悬浮窗权限：未授予。先点下面的按钮去设置里打开。"
        }
    }
}
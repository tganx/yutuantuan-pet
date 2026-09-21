package com.qiyu.livepet

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 核心：前台服务 + 透明悬浮 WebView。
 * 骨架按 docs/overlay-service.md，手势按 docs/gesture-system.md。
 * 渲染层就是我们那只粉团子（assets/pet.html）。
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: WebView? = null
    private lateinit var params: WindowManager.LayoutParams

    // 手势状态机参数（对齐 gesture-system.md）
    private val DOUBLE_TAP_TIMEOUT = 300L
    private val LONG_PRESS_TIMEOUT = 600L
    private val MOVE_THRESHOLD = 10

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTapTime = 0L
    private var touchStartTime = 0L
    private var hasMoved = false
    private var lastDx = 0
    private var lastDy = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        setupOverlay()
    }

    /** Android 14+ 必须显式声明前台服务类型，否则直接崩。 */
    private fun startForegroundCompat() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        params = WindowManager.LayoutParams(
            dpToPx(200),   // 宽
            dpToPx(200),   // 高，按粉团子接近正方形
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20)
            y = dpToPx(200)
        }

        overlayView = WebView(this).apply {
            setBackgroundColor(0x00000000)  // 必须在 loadUrl 前，否则白屏
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/pet.html")
        }

        overlayView?.setOnTouchListener { _, event -> handleTouch(event) }

        windowManager?.addView(overlayView, params)
    }

    /** 手势：拖动 + 单击/双击/长按/甩出，分类后回调 JS。 */
    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                touchStartTime = System.currentTimeMillis()
                hasMoved = false
                lastDx = 0; lastDy = 0
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (abs(dx) > MOVE_THRESHOLD || abs(dy) > MOVE_THRESHOLD) {
                    hasMoved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    lastDx = dx; lastDy = dy
                    windowManager?.updateViewLayout(overlayView, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val elapsed = System.currentTimeMillis() - touchStartTime
                if (!hasMoved) {
                    val now = System.currentTimeMillis()
                    when {
                        elapsed > LONG_PRESS_TIMEOUT -> callJs("onLongPress")
                        now - lastTapTime < DOUBLE_TAP_TIMEOUT -> {
                            lastTapTime = 0
                            callJs("onDoubleTap")
                        }
                        else -> {
                            lastTapTime = now
                            callJs("onTap")
                        }
                    }
                } else {
                    val velocity = sqrt((lastDx * lastDx + lastDy * lastDy).toDouble())
                    if (velocity > 200 && elapsed < 400) {
                        flingHome()
                    } else {
                        callJs("onDragEnd")
                    }
                }
                return true
            }
            else -> return false
        }
    }

    /** 甩出后自己爬回屏幕内。 */
    private fun flingHome() {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        params.x = params.x.coerceIn(0, screenW - params.width)
        params.y = params.y.coerceIn(0, screenH - params.height)
        mainHandler.post {
            windowManager?.updateViewLayout(overlayView, params)
            callJs("onFlingBack")
        }
    }

    /** Kotlin → JS：调用页面里暴露的 window.petEngine。 */
    private fun callJs(fn: String) {
        mainHandler.post {
            overlayView?.evaluateJavascript(
                "window.petEngine && window.petEngine.$fn && window.petEngine.$fn()",
                null
            )
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "livepet_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "粉团子", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, channelId)
            .setContentTitle("粉团子在陪着你")
            .setContentText("它趴在屏幕角落，戳一下试试。")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        overlayView?.let {
            windowManager?.removeView(it)
            it.destroy()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 被系统杀掉后自动重建，桌宠不轻易消失
        return START_STICKY
    }

    companion object {
        private const val NOTIFICATION_ID = 88
    }
}
package com.sajag.shield.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.sajag.shield.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var callStateManager: CallStateManager

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var stateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            return START_NOT_STICKY
        }
        showFloatingHud()
        return START_STICKY
    }

    private fun showFloatingHud() {
        if (overlayView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        // Programmatic cyber floating pill
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E6050B14"))
            setPadding(32, 20, 32, 20)
            elevation = 16f
        }

        val titleView = TextView(this).apply {
            text = "🛡️ SAJAG SHIELD"
            setTextColor(Color.parseColor("#22E5FF"))
            textSize = 12f
            paint.isFakeBoldText = true
        }

        val statusView = TextView(this).apply {
            text = "SCANNING CALL..."
            setTextColor(Color.parseColor("#3DFF8A"))
            textSize = 11f
        }

        val threatView = TextView(this).apply {
            text = "RISK: 0%"
            setTextColor(Color.WHITE)
            textSize = 14f
            paint.isFakeBoldText = true
        }

        container.addView(titleView)
        container.addView(statusView)
        container.addView(threatView)

        // Drag to reposition
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(container, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (kotlin.math.abs(event.rawX - initialTouchX) < 10 &&
                            kotlin.math.abs(event.rawY - initialTouchY) < 10) {
                            // Tap opens main activity HUD
                            val launchIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(launchIntent)
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(container, params)
            overlayView = container

            // Listen to reactive threat level
            stateJob = CoroutineScope(Dispatchers.Main).launch {
                callStateManager.session.collect { session ->
                    val pct = (session.pScam * 100).toInt()
                    threatView.text = "THREAT: $pct%"
                    threatView.setTextColor(
                        when {
                            session.pScam >= 0.88f -> Color.parseColor("#FF4D5E")
                            session.pScam >= 0.60f -> Color.parseColor("#FFB800")
                            else -> Color.parseColor("#3DFF8A")
                        }
                    )
                    statusView.text = session.status.displayName
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Failed to add overlay view: ${e.message}")
        }
    }

    override fun onDestroy() {
        stateJob?.cancel()
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }
}

package com.example.ytcounter

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class CounterService : AccessibilityService() {
    
    // If the counter stops working after a YouTube update,
    // these two view IDs are the first thing to check.
    private val yt = "com.google.android.youtube"
    private val playerId = "$yt:id/watch_player"
    private val titleId = "$yt:id/title"
    
    private var box: TextView? = null
    private var attached = false
    private var inPlayer = false
    private var lastTitle = ""

    private fun today(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())

    private fun count(): Int {
        val p = getSharedPreferences("yt", MODE_PRIVATE)
        if (p.getString("day", "") != today()) {
            p.edit().putString("day", today()).putInt("n", 0).apply()
        }
        return p.getInt("n", 0)
    }
        
    private fun bump() {
        val n = count() + 1
        getSharedPreferences("yt", MODE_PRIVATE).edit().putInt("n", n).apply()
        box?.text = "Videos: $n"
    }
    
    override fun onServiceConnected() {
        val tv = TextView(this)
        tv.setTextColor(Color.GREEN)
        tv.setBackgroundColor(0xB3000000.toInt())
        tv.textSize = 14f
        tv.setPadding(16, 8, 16, 8)
        tv.text = "Videos: ${count()}"
        box = tv
    }
    
    private fun show() {
        if (attached) return
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.END
        p.y = 200
        box?.text = "Videos: ${count()}"
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(box, p)
        attached = true
    }
    
    private fun hide() {
        if (!attached) return
        (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(box)
        attached = false
        inPlayer = false
        lastTitle = ""
    }
    
    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return
        
        if (pkg != yt) {
            hide()
            return
        }
        show()
        
        val root = rootInActiveWindow ?: return
        val playing = root.findAccessibilityNodeInfosByViewId(playerId).isNotEmpty()
        
        if (playing) {
            val title = root.findAccessibilityNodeInfosByViewId(titleId)
                .firstOrNull()?.text?.toString() ?: ""
                
            if (!inPlayer) {
                bump()
                lastTitle = ""
            } else if (title.isNotEmpty() && lastTitle.isNotEmpty() && title != lastTitle) {
                bump()
            }
            if (title.isNotEmpty()) lastTitle = title
        }
        inPlayer = playing
    }
    
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        hide()
        super.onDestroy()
    }
}

        
        

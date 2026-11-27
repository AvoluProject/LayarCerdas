package id.layarcerdas

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.TextView

class FloatingService : Service() {
    private var bubble: View? = null
    private lateinit var wm: WindowManager
    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val tv = TextView(this).apply { text = "✦"; textSize = 23f; setTextColor(0xffffffff.toInt()); gravity = Gravity.CENTER; setBackgroundColor(0xff6d5dfb.toInt()); setPadding(0,0,0,0) }
        val params = WindowManager.LayoutParams(58,58, if (android.os.Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 220 }
        var startX=0; var startY=0; var touchX=0f; var touchY=0f
        tv.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> { startX=params.x; startY=params.y; touchX=event.rawX; touchY=event.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x=startX+(event.rawX-touchX).toInt(); params.y=startY+(event.rawY-touchY).toInt(); wm.updateViewLayout(v,params); true }
                MotionEvent.ACTION_UP -> { if (kotlin.math.abs(event.rawX-touchX)<10 && kotlin.math.abs(event.rawY-touchY)<10) { startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }; true }
                else -> true
            }
        }
        bubble=tv; wm.addView(tv,params)
    }
    override fun onDestroy() { bubble?.let { wm.removeView(it) }; bubble=null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}

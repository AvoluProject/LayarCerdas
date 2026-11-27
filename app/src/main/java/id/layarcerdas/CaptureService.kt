package id.layarcerdas

import android.app.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("captureData", Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra("captureData")
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data)
        captureOnce()
        return START_NOT_STICKY
    }
    private fun createNotification() {
        val channelId = "screen_ocr"
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channelId,"Screen OCR",NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, channelId).setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("LayarCerdas membaca layar").setContentText("Screenshot sementara untuk OCR").setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(31,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(31,notification)
    }
    private fun captureOnce() {
        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= 30) { val bounds = wm.currentWindowMetrics.bounds; metrics.widthPixels=bounds.width(); metrics.heightPixels=bounds.height(); metrics.densityDpi=resources.displayMetrics.densityDpi } else wm.defaultDisplay.getMetrics(metrics)
        reader = ImageReader.newInstance(metrics.widthPixels,metrics.heightPixels,PixelFormat.RGBA_8888,2)
        display = projection?.createVirtualDisplay("LayarCerdasOCR",metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader?.surface,null,handler)
        handler.postDelayed({
            val image = reader?.acquireLatestImage()
            if (image == null) { sendText("Screenshot tidak berhasil dibaca. Coba ulangi."); cleanup(); return@postDelayed }
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * metrics.widthPixels
            val bitmap = Bitmap.createBitmap(metrics.widthPixels + rowPadding / pixelStride, metrics.heightPixels, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()
            val cropped = Bitmap.createBitmap(bitmap,0,0,metrics.widthPixels,metrics.heightPixels)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(cropped,0)).addOnSuccessListener { result -> sendText(result.text.ifBlank { "Tidak ada teks yang terdeteksi." }) }.addOnFailureListener { sendText("OCR gagal: ${it.localizedMessage}") }.addOnCompleteListener { cropped.recycle(); bitmap.recycle(); recognizer.close(); cleanup() }
        }, 650)
    }
    private fun sendText(text: String) { sendBroadcast(Intent("id.layarcerdas.OCR_RESULT").setPackage(packageName).putExtra("ocrText",text)) }
    private fun cleanup() { display?.release(); display=null; reader?.close(); reader=null; projection?.stop(); projection=null; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { cleanup(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}

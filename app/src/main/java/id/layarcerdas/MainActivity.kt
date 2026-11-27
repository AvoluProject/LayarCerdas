package id.layarcerdas

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class MainActivity : Activity() {
    private lateinit var screenText: EditText
    private lateinit var prompt: EditText
    private lateinit var output: TextView
    private val prefs by lazy { getSharedPreferences("lc_prefs", MODE_PRIVATE) }
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("captureData", result.data)
            }
            startForegroundService(intent)
            output.text = "Membaca layar… pastikan konten yang ingin dibaca sedang terlihat."
        } else output.text = "Izin screenshot tidak diberikan."
    }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            screenText.setText(intent?.getStringExtra("ocrText") ?: "")
            output.text = "Teks layar berhasil dibaca. Periksa teksnya sebelum mengirim ke AI."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerOcrReceiver()
        buildUi()
    }
    @Suppress("DEPRECATION")
    private fun registerOcrReceiver() { if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, IntentFilter("id.layarcerdas.OCR_RESULT"), RECEIVER_NOT_EXPORTED) else registerReceiver(screenReceiver, IntentFilter("id.layarcerdas.OCR_RESULT")) }
    override fun onDestroy() { unregisterReceiver(screenReceiver); super.onDestroy() }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 18, 22, 18); setBackgroundColor(Color.rgb(247,247,251)) }
        val scroller = ScrollView(this)
        scroller.addView(root)
        setContentView(scroller)
        root.addView(TextView(this).apply { text = "LayarCerdas"; textSize = 28f; setTextColor(Color.rgb(30,30,45)); typeface = Typeface.DEFAULT_BOLD })
        root.addView(TextView(this).apply { text = "AI screen companion"; textSize = 14f; setTextColor(Color.rgb(110,105,130)); setPadding(0,0,0,18) })
        root.addView(cardLabel("SCREEN ASSIST"))
        root.addView(TextView(this).apply { text = "Ambil teks dari layar secara manual. Kamu tetap bisa memeriksa dan mengedit teks sebelum meminta bantuan AI."; textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0,0,0,10) })
        root.addView(actionButton("Aktifkan bubble mengambang") { enableBubble() })
        root.addView(actionButton("Ambil teks dari layar (OCR)") { val mgr = getSystemService(android.media.projection.MediaProjectionManager::class.java); captureLauncher.launch(mgr.createScreenCaptureIntent()) })
        root.addView(cardLabel("TEKS LAYAR"))
        screenText = EditText(this).apply { hint = "Teks hasil OCR akan muncul di sini…"; minLines = 4; gravity = Gravity.TOP; textSize = 15f; setBackgroundColor(Color.WHITE) }
        root.addView(screenText, LinearLayout.LayoutParams(-1, 220))
        root.addView(cardLabel("APA YANG KAMU BUTUHKAN?"))
        prompt = EditText(this).apply { hint = "Contoh: jelaskan konsep ini langkah demi langkah"; minLines = 2; textSize = 15f; setBackgroundColor(Color.WHITE) }
        root.addView(prompt, LinearLayout.LayoutParams(-1, 120))
        root.addView(actionButton("Minta AI menjelaskan") { askAI() })
        root.addView(cardLabel("HASIL"))
        output = TextView(this).apply { text = "Hasil AI akan tampil di sini."; textSize = 15f; setTextColor(Color.rgb(40,40,50)); setPadding(14,14,14,14); setBackgroundColor(Color.WHITE) }
        root.addView(output, LinearLayout.LayoutParams(-1, -2))
        root.addView(cardLabel("PENGATURAN API"))
        val endpoint = EditText(this).apply { hint = "Endpoint API kompatibel OpenAI"; setText(prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions")); textSize = 14f }
        val model = EditText(this).apply { hint = "Model (contoh gpt-4o-mini)"; setText(prefs.getString("model", "gpt-4o-mini")); textSize = 14f }
        val key = EditText(this).apply { hint = "API key (disimpan lokal di perangkat)"; setText(prefs.getString("key", "")); textSize = 14f; inputType = 129 }
        root.addView(endpoint); root.addView(model); root.addView(key)
        root.addView(actionButton("Simpan pengaturan API") { prefs.edit().putString("endpoint", endpoint.text.toString().trim()).putString("model", model.text.toString().trim()).putString("key", key.text.toString().trim()).apply(); output.text = "Pengaturan disimpan di perangkat." })
        root.addView(TextView(this).apply { text = "Privasi: teks layar hanya dikirim ke layanan AI ketika kamu menekan tombol Minta AI. Jangan tangkap layar berisi data sensitif." ; textSize = 12f; setTextColor(Color.GRAY); setPadding(0,18,0,8 })
    }
    private fun cardLabel(text: String) = TextView(this).apply { this.text = text; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(100,95,120)); setPadding(0,18,0,8) }
    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() }; minHeight = 54 }
    private fun enableBubble() {
        if (!Settings.canDrawOverlays(this)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return }
        startService(Intent(this, FloatingService::class.java)); output.text = "Bubble aktif. Kamu bisa pindah posisi bubble di layar." 
    }
    private fun askAI() {
        val apiKey = prefs.getString("key", "") ?: ""
        val endpoint = prefs.getString("endpoint", "") ?: ""
        val model = prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini"
        val captured = screenText.text.toString().trim()
        val question = prompt.text.toString().trim()
        if (apiKey.isBlank() || endpoint.isBlank() || question.isBlank()) { output.text = "Isi API key, endpoint, dan instruksi terlebih dahulu." ; return }
        output.text = "Meminta bantuan AI…"
        val body = JSONObject().put("model", model).put("messages", org.json.JSONArray().put(JSONObject().put("role","system").put("content","Kamu adalah asisten belajar dan produktivitas. Jelaskan konsep, berikan langkah berpikir dan contoh seperlunya. Jangan menyelesaikan ujian atau penilaian aktif secara otomatis; arahkan ke pemahaman." )).put(JSONObject().put("role","user").put("content", "Teks yang terlihat di layar:\n$captured\n\nPermintaan:\n$question"))).put("temperature", 0.4)
        val request = Request.Builder().url(endpoint).addHeader("Authorization", "Bearer $apiKey").post(body.toString().toRequestBody("application/json".toMediaType())).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { output.text = "Gagal menghubungi AI: ${e.message}" } }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orEmpty()
                val answer = try { JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content") } catch (_: Exception) { "Respons API tidak terbaca.\n$raw" }
                runOnUiThread { output.text = answer }
            }
        })
    }
}

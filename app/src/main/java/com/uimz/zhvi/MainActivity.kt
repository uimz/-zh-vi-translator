package com.uimz.zhvi
import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.*
import org.json.JSONObject
import net.sourceforge.pinyin4j.PinyinHelper
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply { orientation = 1; setBackgroundColor(Color.rgb(246,248,251)); setPadding(16,12,16,10) }
        root.addView(TextView(this).apply { text="中越翻譯通"; textSize=23f; setTextColor(Color.rgb(25,45,75)); gravity=Gravity.CENTER; setPadding(8,16,8,16) })
        val scroll = ScrollView(this)
        messages = LinearLayout(this).apply { orientation=1; setPadding(4,8,4,8) }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply { text="輸入繁體中文或越南語，系統會自動判斷"; textSize=12f; setTextColor(Color.GRAY); setPadding(4,8,4,6) })
        val row = LinearLayout(this).apply { orientation=0; gravity=Gravity.CENTER_VERTICAL }
        input = EditText(this).apply { hint="輸入訊息…"; textSize=16f; minLines=1; maxLines=4; setPadding(12,8,12,8); setBackgroundColor(Color.WHITE) }
        row.addView(input, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(Button(this).apply { text="翻譯"; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(42,112,220)); setOnClickListener { translate() } })
        root.addView(row); setContentView(root)
        addBubble("歡迎使用中越翻譯通！","Huānyíng shǐyòng Zhōng-Yuè fānyì tōng!","Chào mừng bạn sử dụng ứng dụng dịch Trung - Việt!")
    }
    private fun translate() {
        val original=input.text.toString().trim(); if(original.isEmpty()) return
        input.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken,0)
        val chinese=original.any { it in '\u3400'..'\u9fff' }
        val pair=if(chinese) "zh-CN|vi" else "vi|zh-CN"
        val wait=TextView(this).apply { text="正在翻譯…"; setTextColor(Color.GRAY); setPadding(12,12,12,12) }; messages.addView(wait)
        thread {
            try {
                val q=URLEncoder.encode(original,"UTF-8")
                val conn=URL("https://api.mymemory.translated.net/get?q=$q&langpair=$pair").openConnection() as HttpURLConnection
                conn.connectTimeout=15000; conn.readTimeout=15000
                val body=conn.inputStream.bufferedReader().use { it.readText() }; conn.disconnect()
                val result=JSONObject(body).getJSONObject("responseData").getString("translatedText")
                runOnUiThread { messages.removeView(wait); if(chinese) addBubble(original,pinyin(original),result) else addBubble(result,pinyin(result),original) }
            } catch (_:Exception) { runOnUiThread { messages.removeView(wait); addBubble(original,"翻譯失敗，請檢查網路後重試。","") } }
        }
    }
    private fun pinyin(s:String)=s.map { c -> if(c in '\u3400'..'\u9fff') PinyinHelper.toHanyuPinyinStringArray(c)?.firstOrNull()?.substringBefore(",") ?: c.toString() else c.toString() }.joinToString(" ")
    private fun addBubble(zh:String,py:String,vi:String) {
        val card=LinearLayout(this).apply { orientation=1; setPadding(14,12,14,12); setBackgroundColor(Color.WHITE) }
        fun line(label:String,value:String,color:Int) { if(value.isNotBlank()) card.addView(TextView(this).apply { text="$label\n$value"; textSize=15f; setTextColor(color); setPadding(0,3,0,8) }) }
        line("繁體中文",zh,Color.rgb(35,55,85)); line("拼音",py,Color.GRAY); line("越南語",vi,Color.rgb(30,125,90))
        if(vi.isNotBlank()) card.addView(Button(this).apply { text="🔊 播放越南語"; setOnClickListener { tts.language=Locale("vi","VN"); tts.speak(vi,TextToSpeech.QUEUE_FLUSH,null,"translation") } })
        messages.addView(card,LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,8,0,10) })
    }
    override fun onInit(status:Int) { if(status==TextToSpeech.SUCCESS) tts.language=Locale("vi","VN") }
    override fun onDestroy() { if(::tts.isInitialized) tts.shutdown(); super.onDestroy() }
}

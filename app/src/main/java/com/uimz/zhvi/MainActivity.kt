package com.uimz.zhvi

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.view.Gravity
import android.view.View
import android.app.AlertDialog
import android.view.inputmethod.InputMethodManager
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
    private lateinit var content: LinearLayout
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private var sourceChinese = true
    private val navy = Color.rgb(12, 43, 94)
    private val blue = Color.rgb(35, 100, 238)
    private val history = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        window.statusBarColor = navy
        window.navigationBarColor = Color.WHITE
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(244, 247, 253))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            background = rounded(navy, 26)
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = "☰"; textSize = 25f; setTextColor(Color.WHITE)
            setOnClickListener { showMenu() }
        })
        top.addView(TextView(this).apply {
            text = "🇹🇼  中越翻譯通  🇻🇳"
            textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
            gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        top.addView(TextView(this).apply {
            text = "⚙"; textSize = 24f; setTextColor(Color.WHITE)
            setOnClickListener { showInfo("設定", "目前使用線上翻譯服務。") }
        })
        header.addView(top)
        header.addView(TextView(this).apply {
            text = "中文 ⇄ 越南語翻譯・拼音・語音"
            textSize = 13f; setTextColor(Color.rgb(220, 230, 250))
            gravity = Gravity.CENTER; setPadding(0, 10, 0, 8)
        })
        val modes = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(28, 61, 116), 18)
            setPadding(4, 8, 4, 8)
        }
        listOf("文字翻譯", "語音翻譯", "對話模式", "常用語").forEachIndexed { i, label ->
            modes.addView(TextView(this).apply {
                text = listOf("文\n", "🎙\n", "♧\n", "▤\n")[i] + label
                textSize = 13f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                setPadding(8, 8, 8, 8)
                if (i == 0) background = rounded(blue, 14)
                setOnClickListener {
                    if (i == 3) showInfo("常用語", "常用語分類將在後續版本加入。")
                    else if (i != 0) showInfo(label, "此功能將在後續版本加入。")
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        header.addView(modes)
        root.addView(header)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 12)
        }
        val langCard = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 8)
            background = rounded(Color.WHITE, 24, Color.rgb(220, 230, 247))
        }
        val leftLang = TextView(this).apply {
            text = "繁體中文"; textSize = 15f; setTextColor(navy); gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10); layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val swap = TextView(this).apply {
            text = "⇄"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = rounded(blue, 40); setPadding(12, 5, 12, 5)
        }
        val rightLang = TextView(this).apply {
            text = "越南語"; textSize = 15f; setTextColor(navy); gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10); layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        swap.setOnClickListener {
            sourceChinese = !sourceChinese
            leftLang.text = if (sourceChinese) "繁體中文" else "越南語"
            rightLang.text = if (sourceChinese) "越南語" else "繁體中文"
            input.hint = if (sourceChinese) "輸入繁體中文…" else "Nhập tiếng Việt…"
        }
        langCard.addView(leftLang); langCard.addView(swap); langCard.addView(rightLang)
        content.addView(langCard)

        val entry = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = rounded(Color.WHITE, 18, Color.rgb(210, 224, 248))
        }
        input = EditText(this).apply {
            hint = "輸入繁體中文…"; textSize = 17f; minLines = 3; maxLines = 5
            setTextColor(Color.rgb(25, 35, 55)); setPadding(4, 4, 4, 8)
            setBackgroundColor(Color.TRANSPARENT)
        }
        entry.addView(input)
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        controls.addView(TextView(this).apply {
            text = "▧"; textSize = 23f; setTextColor(navy); gravity = Gravity.CENTER
            setPadding(10, 6, 10, 6); setOnClickListener { showInfo("圖片翻譯", "圖片辨識功能將在後續版本加入。") }
        })
        controls.addView(TextView(this).apply {
            text = "🎙"; textSize = 22f; gravity = Gravity.CENTER
            setPadding(10, 6, 10, 6); setOnClickListener { showInfo("語音輸入", "語音輸入功能將在後續版本加入。") }
        })
        controls.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        controls.addView(Button(this).apply {
            text = "文A  翻譯"; textSize = 15f; setTextColor(Color.WHITE)
            background = rounded(blue, 14); setPadding(16, 4, 16, 4)
            setOnClickListener { translate() }
        })
        entry.addView(controls)
        content.addView(entry, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })

        content.addView(TextView(this).apply {
            text = "翻譯結果"; textSize = 18f; setTypeface(null, Typeface.BOLD)
            setTextColor(navy); setPadding(2, 18, 2, 8)
        })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "翻譯　　常用語　　歷史紀錄"
            textSize = 14f; gravity = Gravity.CENTER
            setTextColor(navy); setPadding(8, 14, 8, 14); background = rounded(Color.WHITE, 18)
            setOnClickListener { showInfo("功能選單", "翻譯、常用語與歷史紀錄將逐步完善。") }
        })
        setContentView(root)
        addBubble("歡迎使用中越翻譯通！", pinyin("歡迎使用中越翻譯通！"), "Chào mừng bạn sử dụng ứng dụng dịch Trung - Việt!")
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color); cornerRadius = radius.toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(2, stroke)
        }

    private fun showMenu() {
        val items = arrayOf("首頁翻譯", "翻譯紀錄", "常用語句", "生活情境分類", "設定", "關於應用")
        AlertDialog.Builder(this).setTitle("中越翻譯通").setItems(items) { _, which ->
            if (which == 1) showInfo("翻譯紀錄", if (history.isEmpty()) "目前沒有翻譯紀錄。" else history.joinToString("\n\n"))
            else if (which != 0) showInfo(items[which], "此功能將在後續版本加入。")
        }.show()
    }

    private fun showInfo(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("知道了", null).show()
    }

    private fun translate() {
        val original = input.text.toString().trim()
        if (original.isEmpty()) return
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
        val chinese = if (sourceChinese) true else false
        val pair = if (chinese) "zh-CN|vi" else "vi|zh-CN"
        val loading = TextView(this).apply { text = "正在翻譯…"; setTextColor(Color.GRAY); setPadding(8, 12, 8, 12) }
        content.addView(loading)
        thread {
            try {
                val q = URLEncoder.encode(original, "UTF-8")
                val conn = URL("https://api.mymemory.translated.net/get?q=$q&langpair=$pair")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 15000; conn.readTimeout = 15000
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val result = JSONObject(body).getJSONObject("responseData").getString("translatedText")
                runOnUiThread {
                    content.removeView(loading)
                    if (chinese) addBubble(original, pinyin(original), result)
                    else addBubble(result, pinyin(result), original)
                    history.add(if (chinese) "$original\n$result" else "$original\n$result")
                    input.setText("")
                }
            } catch (_: Exception) {
                runOnUiThread {
                    content.removeView(loading)
                    showInfo("翻譯失敗", "請檢查網路連線後再試一次。")
                }
            }
        }
    }

    private fun pinyin(s: String) = s.map { c ->
        if (c in '\u3400'..'\u9fff') PinyinHelper.toHanyuPinyinStringArray(c)?.firstOrNull()?.substringBefore(",") ?: c.toString()
        else c.toString()
    }.joinToString(" ")

    private fun addBubble(zh: String, py: String, vi: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            background = rounded(Color.WHITE, 18, Color.rgb(221, 231, 247))
        }
        fun line(label: String, value: String, color: Int) {
            if (value.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = "$label\n$value"; textSize = 16f; setTextColor(color)
                    setPadding(0, 4, 0, 12)
                })
            }
        }
        line("繁體中文", zh, navy)
        line("中文拼音", py, Color.DKGRAY)
        line("越南語", vi, Color.rgb(30, 120, 92))
        if (vi.isNotBlank()) {
            card.addView(Button(this).apply {
                text = "🔊 播放越南語"; setOnClickListener {
                    tts.language = Locale("vi", "VN")
                    tts.speak(vi, TextToSpeech.QUEUE_FLUSH, null, "translation")
                }
            })
        }
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale("vi", "VN")
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}

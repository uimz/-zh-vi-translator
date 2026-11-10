package com.uimz.zhvi

import android.app.Activity
import android.text.Html
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.speech.tts.TextToSpeech
import android.widget.*
import org.json.JSONObject
import net.sourceforge.pinyin4j.PinyinHelper
import java.net.HttpURLConnection
import java.net.URL
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
            setOnClickListener { showInfo("免費翻譯模式", "本版本不使用 OpenAI API，也不需要金鑰或額外 API 費用。翻譯由 MyMemory 免費服務提供，需連接網路；免費服務的翻譯品質與額度可能有限。") }
        })
        header.addView(top)
        header.addView(TextView(this).apply {
            text = "自然中越翻譯・拼音・語音"
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
            text = "開始翻譯"; textSize = 15f; setTextColor(Color.WHITE)
            background = rounded(blue, 14); setPadding(16, 4, 16, 4)
            setOnClickListener { translateFree() }
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
            setOnClickListener { showInfo("翻譯紀錄", if (history.isEmpty()) "目前沒有翻譯紀錄。" else history.joinToString("\n\n")) }
        })
        setContentView(root)
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color); cornerRadius = radius.toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(2, stroke)
        }

    private fun showMenu() {
        val items = arrayOf("首頁翻譯", "翻譯紀錄", "常用語句", "生活情境分類", "免費翻譯說明", "關於應用")
        AlertDialog.Builder(this).setTitle("中越翻譯通").setItems(items) { _, which ->
            when (which) {
                1 -> showInfo("翻譯紀錄", if (history.isEmpty()) "目前沒有翻譯紀錄。" else history.joinToString("\n\n"))
                4 -> showInfo("免費翻譯模式", "不需要 OpenAI API 金鑰，也不會產生 OpenAI API 費用。翻譯使用免費線上翻譯服務，需連接網路；翻譯結果可能有歧義，重要內容請先確認再傳送。")
                5 -> showInfo("關於應用", "中越翻譯通 3.0：免費線上中越翻譯。")
                else -> if (which != 0) showInfo(items[which], "此功能將在後續版本加入。")
            }
        }.show()
    }

    private fun showInfo(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("知道了", null).show()
    }

    private fun translateFree() {
        val original = input.text.toString().trim()
        if (original.isEmpty()) return
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
        val chinese = sourceChinese
        val pair = if (chinese) "zh-TW|vi" else "vi|zh-TW"
        val loading = TextView(this).apply {
            text = "正在翻譯，請稍候…"; setTextColor(blue); setPadding(8, 14, 8, 14)
        }
        content.addView(loading)
        thread {
            try {
                val encoded = java.net.URLEncoder.encode(original, "UTF-8")
                val url = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=$pair&mt=1")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (code !in 200..299) throw IllegalStateException("免費翻譯服務暫時無法使用（HTTP $code）")
                val json = JSONObject(body)
                val status = json.optInt("responseStatus", 200)
                if (status != 200) throw IllegalStateException(json.optString("responseDetails", "免費翻譯服務暫時無法使用"))
                val responseData = json.getJSONObject("responseData")
                // Keep the service's primary sentence as the source of truth. A high-scoring
                // translation-memory fragment can be shorter than the full input and omit meaning.
                var translated = responseData.optString("translatedText", "").trim()
                if (translated.isBlank()) {
                    val matches = json.optJSONArray("matches")
                    var bestScore = -1.0
                    if (matches != null) {
                        for (i in 0 until matches.length()) {
                            val candidate = matches.optJSONObject(i) ?: continue
                            val score = candidate.optString("match").toDoubleOrNull() ?: 0.0
                            val candidateText = candidate.optString("translation").trim()
                            if (candidateText.isNotEmpty() && score > bestScore) {
                                bestScore = score
                                translated = candidateText
                            }
                        }
                    }
                }
                // Decode HTML entities and normalize whitespace without changing the wording.
                translated = Html.fromHtml(translated, Html.FROM_HTML_MODE_LEGACY)
                    .toString()
                    .replace(Regex("[\\t\\r\\n ]+"), " ")
                    .trim()
                if (translated.isBlank()) throw IllegalStateException("免費翻譯服務沒有回傳有效內容，請稍後再試。")
                val zh = if (chinese) original else translated
                val vi = if (chinese) translated else original
                val py = pinyin(zh)
                val primaryScore = responseData.optDouble("match", -1.0)
                val note = if (primaryScore in 0.0..0.69) "免費翻譯服務提供的相符度較低，建議確認語意後再傳送。" else ""
                runOnUiThread {
                    content.removeView(loading)
                    addBubble(zh, py, vi, note)
                    history.add("$original\n$zh\n$vi")
                    input.setText("")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    content.removeView(loading)
                    showInfo("翻譯失敗", e.message ?: "請檢查網路連線後再試。")
                }
            }
        }
    }

    private fun pinyin(s: String): String {
        // Group common vocabulary into words; unknown Han characters safely fall back
        // to character-level Hanyu Pinyin.
        val words = listOf(
            "繁體中文", "今天", "明天", "昨天", "台灣", "臺灣", "天氣", "越南語",
            "越南", "中文", "很冷", "謝謝", "你好", "再見", "現在", "等等",
            "為什麼", "怎麼", "可以", "不可以", "沒有", "工作", "上班", "下班",
            "吃飯", "睡覺", "回家", "出門", "爸爸", "媽媽", "老婆", "老公",
            "朋友", "家人", "明年", "今年"
        ).sortedByDescending { it.length }
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch in '\\u3400'..'\\u9fff') {
                val token = words.firstOrNull { s.startsWith(it, i) } ?: ch.toString()
                if (out.isNotEmpty() && out.last() != ' ' && out.last() !in "，。！？；：、,.!?;:") out.append(' ')
                token.forEach { han ->
                    val raw = PinyinHelper.toHanyuPinyinStringArray(han)
                        ?.firstOrNull()?.substringBefore(",") ?: han.toString()
                    out.append(toneMarks(raw))
                }
                i += token.length
            } else {
                if (ch.isWhitespace()) {
                    if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
                } else if (ch in "，。！？；：、,.!?;:") {
                    while (out.isNotEmpty() && out.last() == ' ') out.deleteCharAt(out.lastIndex)
                    out.append(ch)
                } else {
                    if (out.isNotEmpty() && out.last() != ' ' && out.last() !in "，。！？；：、,.!?;:") out.append(' ')
                    out.append(ch)
                }
                i++
            }
        }
        val result = out.toString().trim()
        return if (result.isNotEmpty()) result.replaceFirstChar { it.uppercase() } else result
    }

    private fun toneMarks(raw: String): String {
        if (raw.isEmpty()) return raw
        val tone = raw.last().digitToIntOrNull() ?: return raw
        val base = raw.dropLast(1).replace('v', 'ü')
        if (tone !in 1..4) return base
        val vowels = listOf("a", "o", "e", "i", "u", "ü")
        val target = when {
            base.contains("a") -> "a"
            base.contains("e") -> "e"
            base.contains("ou") -> "o"
            else -> base.lastOrNull { ch -> vowels.any { vowel -> vowel[0] == ch } }?.toString()
        } ?: return base
        val marks = mapOf(
            'a' to listOf('ā','á','ǎ','à'), 'o' to listOf('ō','ó','ǒ','ò'),
            'e' to listOf('ē','é','ě','è'), 'i' to listOf('ī','í','ǐ','ì'),
            'u' to listOf('ū','ú','ǔ','ù'), 'ü' to listOf('ǖ','ǘ','ǚ','ǜ')
        )
        return base.replaceFirst(target, marks[target[0]]!![tone - 1].toString())
    }

    private fun addBubble(zh: String, py: String, vi: String, note: String = "") {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            background = rounded(Color.WHITE, 18, Color.rgb(221, 231, 247))
        }
        fun line(label: String, value: String, color: Int) {
            if (value.isNotBlank()) card.addView(TextView(this).apply {
                text = "$label\n$value"; textSize = 16f; setTextColor(color)
                setPadding(0, 4, 0, 12)
            })
        }
        line("繁體中文", zh, navy)
        line("中文拼音", py, Color.DKGRAY)
        line("越南語", vi, Color.rgb(30, 120, 92))
        if (note.isNotBlank()) line("翻譯提醒", note, Color.rgb(180, 100, 20))
        if (vi.isNotBlank()) card.addView(Button(this).apply {
            text = "🔊 播放越南語"; setOnClickListener {
                tts.language = Locale("vi", "VN")
                tts.speak(vi, TextToSpeech.QUEUE_FLUSH, null, "translation")
            }
        })
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

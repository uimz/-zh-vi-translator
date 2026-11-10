package com.uimz.zhvi

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
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
    private val model = "gpt-4.1-mini"

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
            setOnClickListener { configureApiKey() }
        })
        header.addView(top)
        header.addView(TextView(this).apply {
            text = "AI 語意翻譯・自然口語・拼音・語音"
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
            text = "AI 智慧翻譯"; textSize = 15f; setTextColor(Color.WHITE)
            background = rounded(blue, 14); setPadding(16, 4, 16, 4)
            setOnClickListener { translateWithAi() }
        })
        entry.addView(controls)
        content.addView(entry, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
        content.addView(TextView(this).apply {
            text = "AI 翻譯結果"; textSize = 18f; setTypeface(null, Typeface.BOLD)
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

    private fun securePrefs() = EncryptedSharedPreferences.create(
        this,
        "ai_translation_secure",
        MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun configureApiKey() {
        val edit = EditText(this).apply {
            hint = "sk-…"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(securePrefs().getString("openai_api_key", "") ?: "")
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        box.addView(TextView(this).apply {
            text = "輸入你自己的 OpenAI API 金鑰。金鑰會加密保存在這台手機；翻譯文字會傳送至 OpenAI API。API 使用費另計。"
            textSize = 14f
        })
        box.addView(edit)
        AlertDialog.Builder(this).setTitle("AI 翻譯設定").setView(box)
            .setPositiveButton("儲存") { _, _ ->
                securePrefs().edit().putString("openai_api_key", edit.text.toString().trim()).apply()
                showInfo("設定完成", "已儲存 API 金鑰。可以開始使用 AI 翻譯。")
            }
            .setNeutralButton("清除金鑰") { _, _ ->
                securePrefs().edit().remove("openai_api_key").apply()
                showInfo("已清除", "API 金鑰已從本機設定移除。")
            }
            .setNegativeButton("取消", null).show()
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color); cornerRadius = radius.toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(2, stroke)
        }

    private fun showMenu() {
        val items = arrayOf("首頁翻譯", "翻譯紀錄", "常用語句", "生活情境分類", "AI 金鑰設定", "關於應用")
        AlertDialog.Builder(this).setTitle("中越翻譯通").setItems(items) { _, which ->
            when (which) {
                1 -> showInfo("翻譯紀錄", if (history.isEmpty()) "目前沒有翻譯紀錄。" else history.joinToString("\n\n"))
                4 -> configureApiKey()
                5 -> showInfo("關於應用", "中越翻譯通 2.0：以 AI 理解語意、上下文及日常口語。")
                else -> if (which != 0) showInfo(items[which], "此功能將在後續版本加入。")
            }
        }.show()
    }

    private fun showInfo(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("知道了", null).show()
    }

    private fun translateWithAi() {
        val original = input.text.toString().trim()
        if (original.isEmpty()) return
        val apiKey = securePrefs().getString("openai_api_key", null)
        if (apiKey.isNullOrBlank()) {
            showInfo("尚未設定 AI 金鑰", "請先點右上角齒輪，輸入自己的 OpenAI API 金鑰。API 費用依實際使用量計算。")
            return
        }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
        val chinese = sourceChinese
        val loading = TextView(this).apply {
            text = "AI 正在理解語意與上下文…"; setTextColor(blue); setPadding(8, 14, 8, 14)
        }
        content.addView(loading)
        thread {
            try {
                val direction = if (chinese) "繁體中文翻成越南語" else "越南語翻成台灣繁體中文"
                val systemPrompt = """
你是專業的台灣繁體中文—越南語雙向口語翻譯專家，主要服務台灣丈夫與越南妻子的日常溝通。
任務：$direction。先理解整段話的上下文、人物關係、語氣、情緒和生活情境，再翻譯；絕對不要逐字硬翻。
規則：
1. 輸出自然、容易理解、符合越南人日常聊天習慣的越南語；中文必須是自然的台灣繁體中文。
2. 夫妻對話要依語境使用 anh/em；但不可擅自把一般對話改成夫妻對話。親屬稱謂、尊稱及人名依原文關係保留。
3. 嚴格保留人名、地名、數量、日期、時間、金額、單位、否定、條件及先後順序。不得自行補充事實或刪除內容。單位可用目標語言自然表達，但數值不可變。
4. 口語、反問、玩笑、抱怨、撒嬌、無奈等語氣要忠實呈現，不要變得生硬、兇狠或過度親暱。
5. 若原文有明顯錯字，僅在上下文能確定時修正；若有兩種合理解讀，不要猜，將疑點寫在 note。
6. 反向翻譯欄位也要提供：traditional_chinese 放自然台灣繁體中文；vietnamese 放自然越南語。拼音只針對 traditional_chinese，使用帶聲調符號的漢語拼音。
7. 只回傳 JSON，不要 Markdown 或額外解釋，格式為：
{"traditional_chinese":"...","pinyin":"...","vietnamese":"...","note":""}
若有重要歧義，note 用繁體中文簡短說明；沒有則空字串。
""".trimIndent()
                val messages = JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", original))
                val request = JSONObject()
                    .put("model", model)
                    .put("temperature", 0.2)
                    .put("response_format", JSONObject().put("type", "json_object"))
                    .put("messages", messages)
                val conn = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 20000
                conn.readTimeout = 45000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (code !in 200..299) {
                    val errorMessage = try { JSONObject(body).getJSONObject("error").optString("message") } catch (_: Exception) { "API 請求失敗（HTTP $code）" }
                    throw IllegalStateException(errorMessage)
                }
                val response = JSONObject(body).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
                val result = JSONObject(response)
                val zh = result.optString("traditional_chinese")
                val py = result.optString("pinyin")
                val vi = result.optString("vietnamese")
                val note = result.optString("note")
                runOnUiThread {
                    content.removeView(loading)
                    addBubble(zh, py, vi, note)
                    history.add("$original\n$zh\n$vi")
                    input.setText("")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    content.removeView(loading)
                    showInfo("AI 翻譯未完成", e.message ?: "請檢查網路、API 金鑰與 API 額度。")
                }
            }
        }
    }

    private fun pinyin(s: String): String {
        val syllables = s.map { c ->
            if (c in '\u3400'..'\u9fff') {
                val raw = PinyinHelper.toHanyuPinyinStringArray(c)?.firstOrNull()?.substringBefore(",") ?: c.toString()
                toneMarks(raw)
            } else c.toString()
        }
        return syllables.joinToString("")
            .replace(Regex("([A-Za-zāáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜü]+)([，。！？；：、,.!?;:]*)")) { it.value }
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
            else -> base.lastOrNull { it in vowels }?.toString()
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

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
            setOnClickListener { showAiSettings() }
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
        val items = arrayOf("首頁翻譯", "翻譯紀錄", "常用語句", "生活情境分類", "本機 AI 翻譯設定", "免費翻譯說明", "關於應用")
        AlertDialog.Builder(this).setTitle("中越翻譯通").setItems(items) { _, which ->
            when (which) {
                1 -> showInfo("翻譯紀錄", if (history.isEmpty()) "目前沒有翻譯紀錄。" else history.joinToString("\n\n"))
                4 -> showAiSettings()
                5 -> showInfo("免費翻譯模式", "本機 AI 在手機上運作，不需要 OpenAI API 金鑰或按次付費。第一次下載 Gemma 模型約 2.6 GB，需要網路與足夠儲存空間。MyMemory 免費翻譯仍保留作為備援。")
                6 -> showInfo("關於應用", "中越翻譯通 4.0：免費線上翻譯與可選本機 AI 翻譯。")
                else -> if (which != 0) showInfo(items[which], "此功能將在後續版本加入。")
            }
        }.show()
    }

    private fun showAiSettings() {
        val modelFile = java.io.File(filesDir, "gemma-4-E2B-it.litertlm")
        val installed = modelFile.exists() && modelFile.length() > 1_000_000_000L
        val status = if (installed) "模型已下載，可使用本機 AI 翻譯。" else "尚未下載模型。第一次下載約 2.6 GB，請使用 Wi-Fi 並預留至少 4 GB 空間。"
        val choices = if (installed) arrayOf("啟用本機 AI 翻譯", "改用免費線上翻譯", "重新下載模型") else arrayOf("下載本機 AI 模型（約 2.6 GB）", "繼續使用免費線上翻譯")
        AlertDialog.Builder(this)
            .setTitle("本機 AI 翻譯")
            .setMessage(status + "\n\nAI 模型在手機本機執行，不需要 OpenAI API 金鑰，也不會按翻譯次數收費。")
            .setItems(choices) { _, which ->
                if (!installed && which == 0 || installed && which == 2) downloadAiModel()
                else if (installed && which == 0) {
                    getSharedPreferences("translator", MODE_PRIVATE).edit().putBoolean("use_local_ai", true).apply()
                    showInfo("本機 AI 已啟用", "下次翻譯將優先使用手機本機 AI。若模型無法載入，會自動改用免費線上翻譯。")
                } else {
                    getSharedPreferences("translator", MODE_PRIVATE).edit().putBoolean("use_local_ai", false).apply()
                    showInfo("已切換免費模式", "下次翻譯將使用免費線上翻譯服務。")
                }
            }.setNegativeButton("關閉", null).show()
    }

    private fun downloadAiModel() {
        val target = java.io.File(filesDir, "gemma-4-E2B-it.litertlm")
        val stat = android.os.StatFs(filesDir.absolutePath)
        val available = stat.availableBytes
        if (available < 3_500_000_000L) {
            showInfo("儲存空間不足", "下載模型需要約 2.6 GB，建議至少保留 3.5 GB 可用空間。請先清理手機儲存空間。")
            return
        }
        val progress = ProgressDialog(this).apply {
            setTitle("下載本機 AI 模型")
            setMessage("準備下載約 2.6 GB，請保持網路連線…")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            setCancelable(false)
            show()
        }
        thread {
            val temp = java.io.File(filesDir, "gemma-model.download")
            try {
                val url = URL("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 60000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "ZhiViTranslator-Android")
                val code = conn.responseCode
                if (code !in 200..299) throw IllegalStateException("模型下載失敗（HTTP $code）")
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        var downloaded = 0L
                        var count: Int
                        while (input.read(buffer).also { count = it } != -1) {
                            output.write(buffer, 0, count)
                            downloaded += count
                            if (total > 0) {
                                val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                                runOnUiThread {
                                    if (!isFinishing) {
                                        progress.progress = percent
                                        progress.setMessage("已下載 %.1f / %.1f GB".format(
                                            downloaded / 1_000_000_000.0, total / 1_000_000_000.0
                                        ))
                                    }
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
                if (temp.length() < 1_000_000_000L) throw IllegalStateException("下載檔案大小不正確，請重新下載。")
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) throw IllegalStateException("無法儲存模型檔案，請檢查儲存空間。")
                runOnUiThread {
                    if (!isFinishing) {
                        progress.dismiss()
                        showInfo("模型下載完成", "Gemma 本機 AI 模型已下載。請到設定選擇「啟用本機 AI 翻譯」。第一次載入模型可能需要一些時間。")
                    }
                }
            } catch (e: Exception) {
                temp.delete()
                runOnUiThread {
                    if (!isFinishing) {
                        progress.dismiss()
                        showInfo("模型下載失敗", e.message ?: "請檢查網路後再試。")
                    }
                }
            }
        }
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
        val loading = TextView(this).apply {
            text = "正在翻譯，請稍候…"; setTextColor(blue); setPadding(8, 14, 8, 14)
        }
        content.addView(loading)
        thread {
            try {
                val prefs = getSharedPreferences("translator", MODE_PRIVATE)
                val modelFile = java.io.File(filesDir, "gemma-4-E2B-it.litertlm")
                val useAi = prefs.getBoolean("use_local_ai", false) && modelFile.exists()
                var translated: String
                var note = ""
                if (useAi) {
                    try {
                        translated = LocalAiTranslator.translate(this, modelFile.absolutePath, original, chinese)
                        note = "由手機本機 AI 產生；重要內容仍建議核對。"
                    } catch (aiError: Exception) {
                        translated = translateOnline(original, chinese)
                        note = "本機 AI 暫時無法使用，已改用免費線上翻譯。"
                    }
                } else {
                    translated = translateOnline(original, chinese)
                    if (prefs.getBoolean("use_local_ai", false) && !modelFile.exists()) {
                        note = "本機 AI 模型尚未安裝，已使用免費線上翻譯。"
                    }
                }
                val zh = if (chinese) original else translated
                val vi = if (chinese) translated else original
                val py = pinyin(zh)
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

    private fun translateOnline(original: String, chinese: Boolean): String {
        val pair = if (chinese) "zh-TW|vi" else "vi|zh-TW"
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
        var translated = responseData.optString("translatedText", "").trim()
        if (translated.isBlank()) throw IllegalStateException("免費翻譯服務沒有回傳有效內容，請稍後再試。")
        val conceptPairs = if (chinese) listOf(
            "天氣" to listOf("thời tiết"), "下雨" to listOf("mưa"),
            "很冷" to listOf("lạnh"), "很熱" to listOf("nóng"),
            "開心" to listOf("vui", "hạnh phúc"), "今天" to listOf("hôm nay"),
            "明天" to listOf("ngày mai"), "台灣" to listOf("đài loan"),
            "臺灣" to listOf("đài loan"), "越南" to listOf("việt nam")
        ) else listOf(
            "thời tiết" to listOf("天氣"), "mưa" to listOf("下雨", "雨"),
            "lạnh" to listOf("冷"), "nóng" to listOf("熱"),
            "hôm nay" to listOf("今天"), "ngày mai" to listOf("明天"),
            "đài loan" to listOf("台灣", "臺灣"), "việt nam" to listOf("越南")
        )
        val requiredConcepts = conceptPairs.filter { (term, _) ->
            original.contains(term, ignoreCase = true)
        }.map { it.second }
        fun coverage(candidate: String): Int {
            val lower = candidate.lowercase()
            return requiredConcepts.count { options -> options.any { lower.contains(it.lowercase()) } }
        }
        val initialCoverage = coverage(translated)
        val matches = json.optJSONArray("matches")
        if (matches != null && requiredConcepts.isNotEmpty()) {
            var best = translated
            var bestCoverage = initialCoverage
            var bestScore = -1.0
            for (i in 0 until matches.length()) {
                val candidate = matches.optJSONObject(i) ?: continue
                val value = Html.fromHtml(candidate.optString("translation", ""), Html.FROM_HTML_MODE_LEGACY).toString().trim()
                if (value.isBlank()) continue
                val score = candidate.optString("match").toDoubleOrNull() ?: 0.0
                val candidateCoverage = coverage(value)
                if (candidateCoverage > bestCoverage || (candidateCoverage == bestCoverage && candidateCoverage > initialCoverage && score > bestScore)) {
                    best = value
                    bestCoverage = candidateCoverage
                    bestScore = score
                }
            }
            translated = best
        }
        translated = Html.fromHtml(translated, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("[\\t\\r\\n ]+"), " ").trim()
        if (translated.isBlank()) throw IllegalStateException("免費翻譯服務沒有回傳有效內容，請稍後再試。")
        if (requiredConcepts.isNotEmpty() && coverage(translated) < requiredConcepts.size) {
            // Keep the provider result but do not claim that the translation is fully verified.
        }
        return translated
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
            if (ch in '\u3400'..'\u9fff') {
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

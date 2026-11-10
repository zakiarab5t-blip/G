package com.focusguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate

val C_DARK = Color.parseColor("#0B5D4B")
val C_TEAL = Color.parseColor("#0F766E")
val C_BG = Color.parseColor("#EEF5F2")
val C_RED = Color.parseColor("#E5484D")
val C_BLUE = Color.parseColor("#1877F2")
val C_TXT = Color.parseColor("#12332B")
val C_GRAY = Color.parseColor("#6B7F78")

class ShieldView(c: Context) : View(c) {
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG); p.color = Color.WHITE
        val s = Path()
        s.moveTo(w * .5f, h * .08f); s.lineTo(w * .88f, h * .22f); s.lineTo(w * .88f, h * .52f)
        s.cubicTo(w * .88f, h * .76f, w * .7f, h * .9f, w * .5f, h * .98f)
        s.cubicTo(w * .3f, h * .9f, w * .12f, h * .76f, w * .12f, h * .52f)
        s.lineTo(w * .12f, h * .22f); s.close()
        cv.drawPath(s, p)
        p.style = Paint.Style.STROKE; p.color = C_TEAL; p.strokeWidth = w * .08f
        p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        val t = Path(); t.moveTo(w * .3f, h * .5f); t.lineTo(w * .45f, h * .65f); t.lineTo(w * .72f, h * .35f)
        cv.drawPath(t, p)
    }
}

class BarChart(c: Context, val days: List<LocalDate>, val a: List<Int>, val b: List<Int>) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val d = resources.displayMetrics.density
        val left = 38 * d; val bottom = height - 24 * d; val top = 8 * d; val right = width - 4 * d
        var mx = maxOf(a.maxOrNull() ?: 0, b.maxOrNull() ?: 0, 5)
        mx = ((mx + 4) / 5) * 5
        p.textSize = 10 * d; p.strokeWidth = 1f
        for (i in 0..3) {
            val y = bottom - (bottom - top) * i / 3f
            p.color = Color.parseColor("#E3EBE8"); cv.drawLine(left, y, right, y, p)
            p.color = Color.GRAY; p.textAlign = Paint.Align.RIGHT
            cv.drawText("${mx * i / 3} د", left - 4 * d, y + 3 * d, p)
        }
        val slot = (right - left) / days.size
        for (i in days.indices) {
            val x = left + slot * i + slot / 2
            val bw = minOf(slot * 0.3f, 10 * d)
            p.color = C_RED
            cv.drawRect(x - bw, bottom - (bottom - top) * a[i] / mx, x, bottom, p)
            p.color = C_BLUE
            cv.drawRect(x, bottom - (bottom - top) * b[i] / mx, x + bw, bottom, p)
            if (days.size <= 7 || i % 5 == 0) {
                p.color = Color.GRAY; p.textAlign = Paint.Align.CENTER
                val t = if (days.size <= 7)
                    days[i].dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale("ar"))
                else days[i].dayOfMonth.toString()
                cv.drawText(t, x, height - 6 * d, p)
            }
        }
    }
}

class MainActivity : Activity() {
    private val sp by lazy { Store.p(this) }
    private lateinit var content: FrameLayout
    private lateinit var nav: LinearLayout
    private var tab = 0
    private var range = 30

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun txt(t: String, s: Float, c: Int = C_TXT, b: Boolean = false) = TextView(this).apply {
        text = t; textSize = s; setTextColor(c); if (b) setTypeface(typeface, Typeface.BOLD)
    }
    private fun bg(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }
    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), dp(6), dp(16), dp(6)) }
    private fun card(color: Int = Color.WHITE) = LinearLayout(this).apply {
        background = bg(color, 20); elevation = dp(2).toFloat()
        setPadding(dp(16), dp(14), dp(16), dp(14)); gravity = Gravity.CENTER_VERTICAL
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
    private fun lim(key: String) = minOf(sp.getInt(key, 5), 5)
    private fun fmt(sec: Int) = if (sec < 60) "$sec ث" else "${sec / 60} د"
    private fun serviceOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains(packageName)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        window.statusBarColor = C_DARK
        content = FrameLayout(this)
        nav = LinearLayout(this).apply { setBackgroundColor(Color.WHITE); elevation = dp(8).toFloat(); weightSum = 4f }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(nav, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(root)
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    override fun onResume() { super.onResume(); render() }

    private fun render() {
        nav.removeAllViews()
        listOf("🏠" to "الرئيسية", "📊" to "الإحصائيات", "⚙️" to "الإعدادات", "ℹ️" to "حول").forEachIndexed { i, (ic, l) ->
            nav.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                addView(txt(ic, 20f)); addView(txt(l, 11f, if (i == tab) C_TEAL else C_GRAY, i == tab))
                setOnClickListener { if (i == 3) about() else { tab = i; render() } }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        content.removeAllViews()
        content.addView(when (tab) { 0 -> home(); 1 -> stats(); else -> settings() })
    }

    private fun header(title: String, shield: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(C_DARK, C_TEAL))
        val r = dp(32).toFloat()
        g.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
        background = g; setPadding(dp(20), dp(36), dp(20), dp(24))
        addView(txt(title, 24f, Color.WHITE, true))
        if (shield) {
            addView(txt("ركّز .. وحقّق أهدافك", 14f, Color.parseColor("#BFE5DA")))
            addView(ShieldView(this@MainActivity), LinearLayout.LayoutParams(dp(130), dp(130)).apply { topMargin = dp(12) })
        }
    }

    private fun page(vararg v: View) = ScrollView(this).apply {
        setBackgroundColor(C_BG)
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(16))
            v.forEachIndexed { i, x -> if (i == 0) addView(x, LinearLayout.LayoutParams(-1, -2)) else addView(x, lp()) }
        })
    }

    private fun switchRow(title: String, sub: String, key: String, def: Boolean): View = card().apply {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(txt(title, 16f, C_TXT, true)); addView(txt(sub, 12f, C_GRAY))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(Switch(this@MainActivity).apply {
            isChecked = sp.getBoolean(key, def)
            setOnCheckedChangeListener { _, v -> sp.edit().putBoolean(key, v).apply(); render() }
        })
    }

    private fun linkRow(title: String, sub: String, onClick: () -> Unit): View = card().apply {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(txt(title, 16f, C_TXT, true)); addView(txt(sub, 13f, C_TEAL, true))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(txt("‹", 26f, C_GRAY))
        setOnClickListener { onClick() }
    }

    private fun infoRow(title: String, sub: String): View = card().apply {
        orientation = LinearLayout.VERTICAL
        addView(txt(title, 16f, C_TXT, true)); addView(txt(sub, 13f, C_TEAL, true))
    }

    private fun bigBtn(t: String, onClick: () -> Unit): View = txt(t, 16f, C_DARK, true).apply {
        gravity = Gravity.CENTER; background = bg(Color.parseColor("#CDE9DF"), 24)
        setPadding(0, dp(14), 0, dp(14)); setOnClickListener { onClick() }
    }

    private fun sec(t: String): View = txt(t, 15f, C_TEAL, true).apply { setPadding(dp(4), dp(10), dp(4), 0) }

    private fun limitDialog(title: String, key: String) {
        val cur = lim(key)
        val et = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(cur.toString()) }
        AlertDialog.Builder(this).setTitle(title)
            .setMessage("يمكنك تخفيض الحد فقط، ولا يمكن رفعه (الحد الحالي $cur د).")
            .setView(et)
            .setPositiveButton("حفظ") { _, _ ->
                val v = et.text.toString().toIntOrNull()
                if (v == null || v < 1) toast("أدخل رقماً من 1 فأكثر")
                else if (v > cur) toast("لا يمكن رفع الحد. الحد الحالي $cur د")
                else { sp.edit().putInt(key, v).apply(); render() }
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun about() {
        AlertDialog.Builder(this).setTitle("FocusGuard")
            .setMessage("الإصدار 1.0.0\n\nيحدّ من Shorts وريلز فيسبوك ويتابع وقت يوتيوب.\nبياناتك تبقى على هاتفك ولا يتصل التطبيق بالإنترنت.")
            .setPositiveButton("حسناً", null).show()
    }

    private fun home(): View {
        val on = serviceOn() && sp.getBoolean("enabled", true)
        val status = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(14), 0, dp(6))
            addView(txt(if (on) "الحماية مفعلة" else "الحماية متوقفة", 26f, if (on) C_TEAL else C_RED, true))
            addView(txt(if (on) "● كل شيء تحت السيطرة" else "اضغط هنا لتفعيل الخدمة", 14f, C_GRAY))
            if (!on) setOnClickListener {
                if (!serviceOn()) startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                else { sp.edit().putBoolean("enabled", true).apply(); render() }
            }
        }
        val s = Store.get(this, "shorts"); val f = Store.get(this, "fb"); val y = Store.get(this, "yt")
        fun cell(l: String, v: Int) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = bg(Color.parseColor("#1F7A64"), 14); setPadding(0, dp(12), 0, dp(12))
            addView(txt(fmt(v), 20f, Color.WHITE, true)); addView(txt(l, 12f, Color.parseColor("#BFE5DA")))
        }
        val today = card(C_DARK).apply {
            orientation = LinearLayout.VERTICAL
            addView(txt("إحصائيات اليوم", 17f, Color.WHITE, true))
            addView(LinearLayout(this@MainActivity).apply {
                setPadding(0, dp(10), 0, 0)
                for ((l, v) in listOf("Shorts" to s, "فيسبوك" to f, "يوتيوب" to y))
                    addView(cell(l, v), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            })
        }
        return page(
            header("FocusGuard", true), status,
            switchRow("الحد من YouTube Shorts",
                if (sp.getBoolean("allowShorts", false)) "مسموح حتى الحد اليومي" else "محظور بالكامل",
                "allowShorts", false),
            linkRow("الحد اليومي لمشاهدة Shorts", "${lim("shortsMin")} دقائق") { limitDialog("حد Shorts اليومي (دقائق)", "shortsMin") },
            linkRow("الحد اليومي لفيسبوك", "${lim("fbMin")} دقائق") { limitDialog("حد فيسبوك اليومي (دقائق)", "fbMin") },
            today,
            bigBtn("عرض التفاصيل") { tab = 1; render() }
        )
    }

    private fun total(name: String): Int {
        val re = Regex("^$name(\\d{4}-\\d{2}-\\d{2})$")
        val from = LocalDate.now().minusDays(range - 1L)
        var sum = 0
        for ((k, v) in sp.all) {
            val m = re.find(k) ?: continue
            if (range == 0 || !LocalDate.parse(m.groupValues[1]).isBefore(from)) sum += (v as? Int) ?: 0
        }
        return sum / 60
    }

    private fun stats(): View {
        val chips = LinearLayout(this).apply {
            weightSum = 3f
            for ((r, l) in listOf(7 to "7 أيام", 30 to "30 يوم", 0 to "كل الوقت")) {
                addView(txt(l, 15f, if (range == r) Color.WHITE else C_TXT, true).apply {
                    gravity = Gravity.CENTER
                    background = bg(if (range == r) C_TEAL else Color.parseColor("#DCE8E3"), 20)
                    setPadding(0, dp(10), 0, dp(10))
                    setOnClickListener { range = r; render() }
                }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            }
        }
        fun sum(l: String, v: Int, c: Int, tint: Int) = card(tint).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            addView(txt(l, 14f, c, true)); addView(txt("$v د", 26f, C_TXT, true))
        }
        val sums = LinearLayout(this).apply {
            weightSum = 2f
            addView(sum("مشاهدة فيسبوك", total("fb"), C_BLUE, Color.parseColor("#DDE9FB")),
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            addView(sum("مشاهدة Shorts", total("shorts"), C_RED, Color.parseColor("#FBE0E1")),
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }
        val n = if (range == 7) 7 else 30
        val days = (n - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        val chart = BarChart(this, days, days.map { sp.getInt("shorts$it", 0) / 60 }, days.map { sp.getInt("fb$it", 0) / 60 })
        val chartCard = card().apply {
            orientation = LinearLayout.VERTICAL
            addView(txt("مخطط الاستخدام", 17f, C_TXT, true))
            addView(LinearLayout(this@MainActivity).apply {
                addView(txt("● Shorts   ", 12f, C_RED)); addView(txt("● Facebook", 12f, C_BLUE))
            })
            addView(chart, LinearLayout.LayoutParams(-1, dp(220)))
        }
        val used = Store.get(this, "shorts") + Store.get(this, "fb")
        val tip = card(Color.parseColor("#DDF3EA")).apply {
            orientation = LinearLayout.VERTICAL
            addView(txt("معلومات مفيدة", 16f, C_TXT, true))
            addView(txt(if (used < 300) "أنت تلتزم بشكل رائع! استمر في الحفاظ على تركيزك." else "حاول تقليل وقتك على الفيديوهات القصيرة اليوم.", 14f, C_GRAY))
        }
        return page(header("الإحصائيات", false), chips, sums, chartCard, tip, bigBtn("إعادة تعيين الإحصائيات") { resetStats() })
    }

    private fun resetStats() {
        AlertDialog.Builder(this).setTitle("إعادة تعيين الإحصائيات")
            .setMessage("سيتم حذف إحصائيات الأيام السابقة. وقت اليوم يبقى كما هو حتى لا تُتجاوز الحدود.")
            .setPositiveButton("حذف") { _, _ ->
                val re = Regex("^(fb|shorts|yt|ytAsked|studyAnswers|watchAnswers)\\d{4}-\\d{2}-\\d{2}$")
                val t = LocalDate.now().toString()
                val e = sp.edit()
                sp.all.keys.filter { re.matches(it) && !it.endsWith(t) }.forEach { e.remove(it) }
                e.apply(); render()
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun settings(): View = page(
        header("الإعدادات", false),
        sec("إعدادات الحماية"),
        switchRow("تفعيل FocusGuard", "حماية أكبر وتركيز أفضل", "enabled", true),
        switchRow("السماح بـ Shorts حتى الحد اليومي", "إيقافه يحظر Shorts بالكامل", "allowShorts", false),
        infoRow("ريلز فيسبوك", "ممنوع دائماً"),
        linkRow("تفعيل الخدمة (إمكانية الوصول)", if (serviceOn()) "مفعّلة ✓" else "غير مفعّلة") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        sec("الحدود اليومية"),
        linkRow("الحد اليومي لـ Shorts", "${lim("shortsMin")} دقائق") { limitDialog("حد Shorts اليومي (دقائق)", "shortsMin") },
        linkRow("الحد اليومي لفيسبوك", "${lim("fbMin")} دقائق") { limitDialog("حد فيسبوك اليومي (دقائق)", "fbMin") },
        sec("معلومات التطبيق"),
        linkRow("مشاركة التطبيق", "مع من تحب") {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "FocusGuard: تطبيق لتقليل وقت الفيديوهات القصيرة"), null))
        },
        linkRow("حول التطبيق", "الإصدار 1.0.0") { about() }
    )
}

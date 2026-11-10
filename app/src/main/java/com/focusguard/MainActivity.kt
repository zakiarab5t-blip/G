package com.focusguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ألوان ثابتة في الوضعين
private val C_DARK = Color.parseColor("#075B4D")
private val C_BTN = Color.parseColor("#0E806D")
private val C_RED = Color.parseColor("#D94B55")
private val C_BLUE = Color.parseColor("#3978C8")

// ألوان تتغير حسب الوضع (فاتح / داكن)
private var C_TEAL = Color.parseColor("#0E806D")
private var C_MINT = Color.parseColor("#DDF3EB")
private var C_BG = Color.parseColor("#F4F8F6")
private var C_CARD = Color.WHITE
private var C_TXT = Color.parseColor("#173B33")
private var C_GRAY = Color.parseColor("#71847D")
private var C_LINE = Color.parseColor("#E4ECE8")
private var C_T_RED = Color.parseColor("#FBEDEF")
private var C_T_BLUE = Color.parseColor("#EAF1FC")
private var C_T_OFF = Color.parseColor("#FCEBED")
private var C_TRACK = Color.parseColor("#DCE5E1")
private var C_THUMB = Color.parseColor("#D5DEDA")

private fun applyTheme(dark: Boolean) {
    if (dark) {
        C_TEAL = Color.parseColor("#2FBF9F")
        C_MINT = Color.parseColor("#1D3B33")
        C_BG = Color.parseColor("#0D1917")
        C_CARD = Color.parseColor("#162622")
        C_TXT = Color.parseColor("#E6F2EE")
        C_GRAY = Color.parseColor("#8FA69E")
        C_LINE = Color.parseColor("#25382F")
        C_T_RED = Color.parseColor("#3A2226")
        C_T_BLUE = Color.parseColor("#1F2E42")
        C_T_OFF = Color.parseColor("#3A2226")
        C_TRACK = Color.parseColor("#2F4540")
        C_THUMB = Color.parseColor("#9FB3AC")
    } else {
        C_TEAL = Color.parseColor("#0E806D")
        C_MINT = Color.parseColor("#DDF3EB")
        C_BG = Color.parseColor("#F4F8F6")
        C_CARD = Color.WHITE
        C_TXT = Color.parseColor("#173B33")
        C_GRAY = Color.parseColor("#71847D")
        C_LINE = Color.parseColor("#E4ECE8")
        C_T_RED = Color.parseColor("#FBEDEF")
        C_T_BLUE = Color.parseColor("#EAF1FC")
        C_T_OFF = Color.parseColor("#FCEBED")
        C_TRACK = Color.parseColor("#DCE5E1")
        C_THUMB = Color.parseColor("#D5DEDA")
    }
}

/** أقصى حد يومي مسموح به (بالدقائق). يمكن تخفيضه فقط ولا يمكن رفعه. */
private const val MAX_LIMIT = 5

/** درع الحماية: علامة صح عند التفعيل وعلامة تعجب عند الإيقاف. */
class ShieldView(c: Context, private val ok: Boolean = true) : View(c) {
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val s = Path()
        s.moveTo(w * .5f, h * .08f)
        s.lineTo(w * .88f, h * .22f)
        s.lineTo(w * .88f, h * .52f)
        s.cubicTo(w * .88f, h * .76f, w * .7f, h * .9f, w * .5f, h * .98f)
        s.cubicTo(w * .3f, h * .9f, w * .12f, h * .76f, w * .12f, h * .52f)
        s.lineTo(w * .12f, h * .22f)
        s.close()
        cv.drawPath(s, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * .08f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        if (ok) {
            p.color = C_BTN
            val tick = Path()
            tick.moveTo(w * .3f, h * .5f)
            tick.lineTo(w * .45f, h * .65f)
            tick.lineTo(w * .72f, h * .35f)
            cv.drawPath(tick, p)
        } else {
            p.color = C_RED
            cv.drawLine(w * .5f, h * .3f, w * .5f, h * .57f, p)
            p.style = Paint.Style.FILL
            cv.drawCircle(w * .5f, h * .72f, w * .05f, p)
        }
    }
}

class BarChart(
    c: Context,
    private val days: List<LocalDate>,
    private val shortsData: List<Int>,
    private val fbData: List<Int>
) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arabic: Locale = Locale.forLanguageTag("ar")

    override fun onDraw(cv: Canvas) {
        if (days.isEmpty()) return
        val d = resources.displayMetrics.density
        val left = 38 * d
        val bottom = height - 24 * d
        val top = 8 * d
        val right = width - 4 * d
        var mx = maxOf(shortsData.maxOrNull() ?: 0, fbData.maxOrNull() ?: 0, 5)
        mx = ((mx + 4) / 5) * 5
        p.textSize = 10 * d
        p.strokeWidth = 1f
        for (i in 0..3) {
            val y = bottom - (bottom - top) * i / 3f
            p.color = C_LINE
            cv.drawLine(left, y, right, y, p)
            p.color = C_GRAY
            p.textAlign = Paint.Align.RIGHT
            cv.drawText("${mx * i / 3} د", left - 4 * d, y + 3 * d, p)
        }
        val slot = (right - left) / days.size
        for (i in days.indices) {
            val x = left + slot * i + slot / 2
            val bw = minOf(slot * 0.3f, 10 * d)
            p.color = C_RED
            cv.drawRect(x - bw, bottom - (bottom - top) * shortsData[i] / mx, x, bottom, p)
            p.color = C_BLUE
            cv.drawRect(x, bottom - (bottom - top) * fbData[i] / mx, x + bw, bottom, p)
            if (days.size <= 7 || i % 5 == 0) {
                p.color = C_GRAY
                p.textAlign = Paint.Align.CENTER
                val label = if (days.size <= 7) {
                    days[i].dayOfWeek.getDisplayName(TextStyle.SHORT, arabic)
                } else days[i].dayOfMonth.toString()
                cv.drawText(label, x, height - 6 * d, p)
            }
        }
        if (shortsData.all { it == 0 } && fbData.all { it == 0 }) {
            p.color = C_GRAY
            p.textAlign = Paint.Align.CENTER
            p.textSize = 13 * d
            cv.drawText("لا توجد بيانات بعد", (left + right) / 2, (top + bottom) / 2, p)
        }
    }
}

class MainActivity : Activity() {
    private val sp by lazy { Store.p(this) }
    private lateinit var content: FrameLayout
    private lateinit var nav: LinearLayout
    private var tab = 0
    private var range = 30

    private val ytPkgs = arrayOf("com.google.android.youtube")
    private val fbPkgs = arrayOf("com.facebook.katana", "com.facebook.lite")

    // ---------- أدوات مساعدة ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun txt(t: String, s: Float, c: Int = C_TXT, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = s
            setTextColor(c)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = true
        }

    private fun bg(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun oval(color: Int) =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    private fun gradientBg(top: Int, bottom: Int, radius: Int) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(top, bottom)).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply {
        setMargins(dp(18), dp(6), dp(18), dp(6))
    }

    private fun card(color: Int = C_CARD) = LinearLayout(this).apply {
        background = bg(color, 22)
        elevation = dp(2).toFloat()
        setPadding(dp(16), dp(15), dp(16), dp(15))
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun lim(key: String) = minOf(sp.getInt(key, MAX_LIMIT), MAX_LIMIT)

    /** تنسيق الثواني إلى نص مقروء: ث / د / س+د */
    private fun hm(sec: Int): String {
        val m = sec / 60
        return when {
            sec <= 0 -> "0 د"
            sec < 60 -> "$sec ث"
            m < 60 -> "$m د"
            else -> "${m / 60}س ${m % 60}د"
        }
    }

    /** مقارنة دقيقة لاسم الحزمة بدل contains لتجنب التطابق الجزئي مع تطبيقات أخرى. */
    private fun serviceOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.packageName == packageName
        }
    }

    /** دائرة ملونة تحتوي رمزاً، تُستخدم كأيقونة للصفوف. */
    private fun iconCircle(glyph: String, bgColor: Int, fg: Int = C_TEAL): View =
        txt(glyph, 18f, fg, true).apply {
            gravity = Gravity.CENTER
            background = oval(bgColor)
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }

    /** أيقونة التطبيق الحقيقية من الهاتف إن كان مثبتاً، وإلا دائرة بحرف بديلة. */
    private fun appIcon(label: String, fallback: Int, pkgs: Array<String>): View {
        for (p in pkgs) {
            try {
                val d = packageManager.getApplicationIcon(p)
                return ImageView(this).apply {
                    setImageDrawable(d)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // التطبيق غير مثبت: ننتقل للحزمة التالية أو للأيقونة البديلة
            }
        }
        return txt(label.take(1), 18f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = oval(fallback)
        }
    }

    private fun bar(pct: Int, color: Int): View = LinearLayout(this).apply {
        weightSum = 100f
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        background = bg(C_LINE, 6)
        val p = pct.coerceIn(0, 100)
        if (p > 0) {
            addView(View(this@MainActivity).apply { background = bg(color, 6) },
                LinearLayout.LayoutParams(0, dp(8), p.toFloat()))
        }
        addView(View(this@MainActivity), LinearLayout.LayoutParams(0, dp(8), (100 - p).toFloat()))
    }

    private fun mkSwitch(key: String, def: Boolean, after: () -> Unit = { render() }) =
        Switch(this).apply {
            isChecked = sp.getBoolean(key, def)
            thumbTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.WHITE, C_THUMB)
            )
            trackTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(C_BTN, C_TRACK)
            )
            setOnCheckedChangeListener { _, checked ->
                sp.edit().putBoolean(key, checked).apply()
                // نؤجل التحديث حتى يكتمل أنيميشن المفتاح
                postDelayed({ after() }, 180)
            }
        }

    /** صف موحّد: أيقونة، عنوان، وصف، ثم عنصر جانبي (مفتاح أو سهم). */
    private fun row(
        glyph: String,
        tint: Int,
        title: String,
        sub: String? = null,
        subColor: Int = C_GRAY,
        trailing: View? = null,
        onClick: (() -> Unit)? = null
    ): View = card().apply {
        addView(iconCircle(glyph, tint))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            addView(txt(title, 15f, C_TXT, true))
            if (sub != null) addView(txt(sub, 12f, subColor).apply { setPadding(0, dp(3), 0, 0) })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        if (trailing != null) addView(trailing)
        else if (onClick != null) addView(txt("‹", 27f, C_GRAY))
        if (onClick != null) {
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun bigBtn(title: String, color: Int = C_BTN, onClick: () -> Unit): View =
        txt(title, 15f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = bg(color, 18)
            setPadding(0, dp(15), 0, dp(15))
            elevation = dp(2).toFloat()
            isClickable = true
            setOnClickListener { onClick() }
        }

    private fun sectionTitle(title: String): View =
        txt(title, 17f, C_TXT, true).apply { setPadding(dp(4), dp(8), dp(4), 0) }

    private fun header(title: String, glyph: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = gradientBg(C_DARK, C_BTN, 0).also {
                val r = dp(28).toFloat()
                it.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            }
            setPadding(dp(22), dp(26), dp(22), dp(26))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(txt("$title  $glyph", 22f, Color.WHITE, true))
        }

    private fun page(vararg views: View) = ScrollView(this).apply {
        setFillViewport(false)
        isVerticalScrollBarEnabled = false
        setBackgroundColor(C_BG)
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(18))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            views.forEachIndexed { index, view ->
                if (index == 0) addView(view, LinearLayout.LayoutParams(-1, -2))
                else addView(view, lp())
            }
        })
    }

    // ---------- دورة الحياة ----------

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        applyTheme(sp.getBoolean("dark", false))
        if (b != null) tab = b.getInt("tab", 0)
        actionBar?.hide()
        if (b == null &&
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        window.statusBarColor = C_DARK
        window.navigationBarColor = C_CARD

        content = FrameLayout(this).apply { setBackgroundColor(C_BG) }
        nav = LinearLayout(this).apply {
            setBackgroundColor(C_CARD)
            elevation = dp(12).toFloat()
            weightSum = 4f
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C_DARK)
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(nav, LinearLayout.LayoutParams(-1, -2))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // معالجة edge-to-edge (أندرويد 15+): ترك مساحة لشريط الحالة وشريط التنقل
        root.setOnApplyWindowInsetsListener { v, insets ->
            val topInset: Int
            val bottomInset: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                topInset = bars.top
                bottomInset = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                topInset = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottomInset = insets.systemWindowInsetBottom
            }
            v.setPadding(0, topInset, 0, 0)
            nav.setPadding(dp(5), dp(5), dp(5), dp(5) + bottomInset)
            insets
        }
        setContentView(root)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", tab)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        nav.removeAllViews()
        val items = listOf(
            Triple("⌂", "الرئيسية", 0),
            Triple("▥", "الإحصائيات", 1),
            Triple("▦", "التطبيقات", 2),
            Triple("⚙", "الإعدادات", 3)
        )
        items.forEach { (icon, label, index) ->
            val selected = index == tab
            nav.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = if (selected) bg(C_MINT, 18) else bg(Color.TRANSPARENT, 18)
                    setPadding(dp(4), dp(7), dp(4), dp(7))
                    addView(txt(icon, 22f, if (selected) C_TEAL else C_GRAY, true))
                    addView(txt(label, 10f, if (selected) C_TEAL else C_GRAY, selected))
                    setOnClickListener {
                        tab = index
                        render()
                    }
                },
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
            )
        }
        content.removeAllViews()
        content.addView(
            when (tab) {
                0 -> home()
                1 -> stats()
                2 -> apps()
                else -> settings()
            }
        )
    }

    // ---------- حوارات ----------

    private fun limitDialog(title: String, key: String) {
        val current = lim(key)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("يمكنك تخفيض الحد فقط، ولا يمكن رفعه (الحد الحالي $current د).")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value == null || value < 1) toast("أدخل رقماً من 1 فأكثر")
                else if (value > current) toast("لا يمكن رفع الحد. الحد الحالي $current د")
                else {
                    sp.edit().putInt(key, value).apply()
                    render()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun about() {
        AlertDialog.Builder(this)
            .setTitle("FocusGuard")
            .setMessage("الإصدار 1.0.0\n\nيحدّ من Shorts وريلز فيسبوك ويتابع وقت يوتيوب.\nبياناتك تبقى على هاتفك ولا يتصل التطبيق بالإنترنت.")
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun resetStats() {
        AlertDialog.Builder(this)
            .setTitle("إعادة تعيين الإحصائيات")
            .setMessage("سيتم حذف إحصائيات الأيام السابقة. وقت اليوم يبقى كما هو حتى لا تُتجاوز الحدود.")
            .setPositiveButton("حذف") { _, _ ->
                val regex = Regex("^(fb|shorts|yt|ytAsked|studyAnswers|watchAnswers)\\d{4}-\\d{2}-\\d{2}$")
                val today = LocalDate.now().toString()
                val editor = sp.edit()
                sp.all.keys.filter { regex.matches(it) && !it.endsWith(today) }.forEach { editor.remove(it) }
                editor.apply()
                render()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // ---------- الرئيسية ----------

    private fun home(): View {
        val svc = serviceOn()
        val enabled = sp.getBoolean("enabled", true)
        val on = svc && enabled

        val shorts = Store.get(this, "shorts")
        val fb = Store.get(this, "fb")
        val yt = Store.get(this, "yt")
        val limitTotal = (lim("shortsMin") + lim("fbMin")) * 60
        val used = shorts + fb
        val pct = (used * 100L / maxOf(1, limitTotal)).toInt().coerceAtMost(100)
        val over = used >= limitTotal

        val ring = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#22FFFFFF"))
                setStroke(dp(4), Color.parseColor("#66FFFFFF"))
            }
            addView(ShieldView(this@MainActivity, on),
                FrameLayout.LayoutParams(dp(84), dp(84), Gravity.CENTER))
        }

        val pill = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(if (on) C_MINT else C_T_OFF, 20)
            setPadding(dp(16), dp(7), dp(16), dp(7))
            addView(txt("●", 12f, if (on) C_TEAL else C_RED, true))
            addView(txt(if (on) "الحماية مفعّلة" else "الحماية متوقفة", 14f, C_TXT, true)
                .apply { setPadding(dp(8), 0, 0, 0) })
        }

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = gradientBg(C_DARK, C_BTN, 0).also {
                val r = dp(32).toFloat()
                it.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            }
            setPadding(dp(22), dp(20), dp(22), dp(26))
            addView(ShieldView(this@MainActivity, true), LinearLayout.LayoutParams(dp(52), dp(52)))
            addView(txt("FocusGuard", 26f, Color.WHITE, true).apply { setPadding(0, dp(4), 0, 0) })
            addView(txt("ركّز أكثر، وحقّق أهدافك", 13f, Color.parseColor("#C7E8DE")))
            addView(ring, LinearLayout.LayoutParams(dp(150), dp(150)).apply { topMargin = dp(18) })
            addView(pill, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(14) })
        }

        val toggleBtn = bigBtn(if (on) "إيقاف الحماية" else "تفعيل الحماية") {
            if (on) {
                sp.edit().putBoolean("enabled", false).apply()
                render()
            } else if (!svc) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                sp.edit().putBoolean("enabled", true).apply()
                render()
            }
        }

        fun statCell(glyph: String, label: String, value: String) =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(txt("$glyph  $label", 12f, C_GRAY))
                addView(txt(value, 24f, C_TXT, true).apply { setPadding(0, dp(4), 0, 0) })
            }

        val summary = card().apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(this@MainActivity).apply {
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                addView(statCell("◷", "الوقت المستهلك اليوم", hm(fb + yt)),
                    LinearLayout.LayoutParams(0, -2, 1f))
                addView(View(this@MainActivity).apply { setBackgroundColor(C_LINE) },
                    LinearLayout.LayoutParams(dp(1), -1).apply { setMargins(dp(8), 0, dp(8), 0) })
                addView(statCell("▶", "Shorts اليوم", hm(shorts)),
                    LinearLayout.LayoutParams(0, -2, 1f))
            })
            addView(LinearLayout(this@MainActivity).apply {
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(0, dp(14), 0, 0)
                addView(txt("استهلاك الحدود اليومية", 12f, C_GRAY), LinearLayout.LayoutParams(0, -2, 1f))
                addView(txt("$pct%", 13f, if (over) C_RED else C_TEAL, true))
            })
            addView(bar(pct, if (over) C_RED else C_BTN),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }

        fun tile(name: String, sec: Int, fallback: Int, pkgs: Array<String>) =
            card().apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(txt("✓", 11f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    background = oval(C_BTN)
                }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { gravity = Gravity.START })
                addView(appIcon(name, fallback, pkgs),
                    LinearLayout.LayoutParams(dp(44), dp(44)).apply { topMargin = dp(2) })
                addView(txt(name, 14f, C_TXT, true).apply { setPadding(0, dp(8), 0, 0) })
                addView(txt(hm(sec), 12f, C_GRAY).apply { setPadding(0, dp(2), 0, 0) })
            }

        val tiles = LinearLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(tile("YouTube", yt, C_RED, ytPkgs),
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            addView(tile("Facebook", fb, C_BLUE, fbPkgs),
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }

        return page(
            hero,
            toggleBtn,
            summary,
            sectionTitle("التطبيقات المراقبة"),
            tiles
        )
    }

    // ---------- الإحصائيات ----------

    /** قراءة قيم يوم بيوم (بالثواني) لمفتاح معيّن مثل shorts أو fb أو yt. */
    private fun dayMap(name: String): Map<LocalDate, Int> {
        val regex = Regex("^$name(\\d{4}-\\d{2}-\\d{2})$")
        val out = HashMap<LocalDate, Int>()
        for ((key, value) in sp.all) {
            val match = regex.find(key) ?: continue
            val date = try {
                LocalDate.parse(match.groupValues[1])
            } catch (e: Exception) {
                continue
            }
            out[date] = (value as? Int) ?: 0
        }
        return out
    }

    /** مجموع الثواني ضمن النطاق المختار (range == 0 يعني كل الوقت). */
    private fun totalSec(name: String): Int {
        val from = if (range > 0) LocalDate.now().minusDays(range - 1L) else null
        return dayMap(name).filter { from == null || !it.key.isBefore(from) }.values.sum()
    }

    private fun stats(): View {
        val seg = LinearLayout(this).apply {
            weightSum = 3f
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = bg(C_CARD, 18)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            for ((r, label) in listOf(7 to "7 أيام", 30 to "30 يوم", 0 to "كل الوقت")) {
                addView(txt(label, 13f, if (range == r) Color.WHITE else C_TXT, true).apply {
                    gravity = Gravity.CENTER
                    background = bg(if (range == r) C_BTN else Color.TRANSPARENT, 14)
                    setPadding(0, dp(10), 0, dp(10))
                    setOnClickListener { range = r; render() }
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
        }

        val shortsMap = dayMap("shorts")
        val fbMap = dayMap("fb")
        val ytMap = dayMap("yt")
        val today = LocalDate.now()
        val dates: List<LocalDate> =
            if (range > 0) (range - 1 downTo 0).map { today.minusDays(it.toLong()) }
            else (shortsMap.keys + fbMap.keys + ytMap.keys).toSet().sortedBy { it.toEpochDay() }

        fun usage(d: LocalDate) = (fbMap[d] ?: 0) + (ytMap[d] ?: 0)
        val active = dates.filter { usage(it) > 0 }
        val avg = if (active.isEmpty()) 0 else active.sumOf { usage(it) } / active.size
        val least = active.minOfOrNull { usage(it) } ?: 0
        val okDays = dates.count {
            (shortsMap[it] ?: 0) <= lim("shortsMin") * 60 && (fbMap[it] ?: 0) <= lim("fbMin") * 60
        }

        fun mini(glyph: String, label: String, value: String, tint: Int) =
            card(tint).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(12), dp(6), dp(12))
                addView(txt(glyph, 20f, C_TEAL, true))
                addView(txt(label, 11f, C_GRAY).apply { setPadding(0, dp(4), 0, 0) })
                addView(txt(value, 16f, C_TXT, true).apply { setPadding(0, dp(2), 0, 0) })
            }

        val minis = LinearLayout(this).apply {
            weightSum = 3f
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(mini("◔", "المتوسط", hm(avg), C_CARD),
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            addView(mini("✓", "أيام ملتزم", "$okDays / ${dates.size}", C_MINT),
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
            addView(mini("▥", "أقل استخدام", hm(least), C_T_BLUE),
                LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        }

        val count = if (range == 7) 7 else 30
        val days = (count - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val chart = BarChart(
            this,
            days,
            days.map { (shortsMap[it] ?: 0) / 60 },
            days.map { (fbMap[it] ?: 0) / 60 }
        )
        val chartCard = card().apply {
            orientation = LinearLayout.VERTICAL
            // عند "كل الوقت" المخطط يعرض آخر 30 يوماً فقط، نوضح ذلك للمستخدم
            addView(txt(
                if (range == 0) "مخطط الاستخدام (آخر 30 يوماً)" else "مخطط الاستخدام",
                16f, C_TXT, true
            ))
            addView(LinearLayout(this@MainActivity).apply {
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(0, dp(8), 0, dp(4))
                addView(txt("● Shorts   ", 11f, C_RED, true))
                addView(txt("● Facebook", 11f, C_BLUE, true))
            })
            addView(chart, LinearLayout.LayoutParams(-1, dp(220)))
        }

        val ytTotal = totalSec("yt")
        val fbTotal = totalSec("fb")
        val mx = maxOf(ytTotal, fbTotal, 1)

        fun appRow(name: String, sec: Int, color: Int, fallback: Int, pkgs: Array<String>) =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(10), 0, 0)
                addView(LinearLayout(this@MainActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    addView(appIcon(name, fallback, pkgs), LinearLayout.LayoutParams(dp(30), dp(30)))
                    addView(txt(name, 14f, C_TXT, true).apply { setPadding(dp(10), 0, dp(10), 0) },
                        LinearLayout.LayoutParams(0, -2, 1f))
                    addView(txt(hm(sec), 13f, C_GRAY, true))
                })
                addView(bar((sec * 100L / mx).toInt(), color),
                    LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            }

        val topApps = card().apply {
            orientation = LinearLayout.VERTICAL
            addView(txt("أكثر التطبيقات استهلاكاً للوقت", 16f, C_TXT, true))
            addView(appRow("YouTube", ytTotal, C_RED, C_RED, ytPkgs))
            addView(appRow("Facebook", fbTotal, C_BLUE, C_BLUE, fbPkgs))
        }

        val reset = txt("إعادة تعيين الإحصائيات", 14f, C_RED, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(14))
            isClickable = true
            setOnClickListener { resetStats() }
        }

        return page(
            header("الإحصائيات", "▥"),
            seg,
            minis,
            chartCard,
            topApps,
            reset
        )
    }

    // ---------- التطبيقات ----------

    private fun apps(): View {
        val allow = sp.getBoolean("allowShorts", false)

        val youtube = card().apply {
            addView(appIcon("YouTube", C_RED, ytPkgs), LinearLayout.LayoutParams(dp(42), dp(42)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(12), 0)
                addView(txt("YouTube", 15f, C_TXT, true))
                addView(txt("الحد اليومي لـ Shorts: ${lim("shortsMin")} د", 12f, C_TEAL, true)
                    .apply { setPadding(0, dp(3), 0, 0) })
                addView(txt(if (allow) "Shorts مسموح حتى الحد" else "Shorts محظور بالكامل", 12f, C_GRAY)
                    .apply { setPadding(0, dp(2), 0, 0) })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(mkSwitch("allowShorts", false))
            isClickable = true
            isFocusable = true
            setOnClickListener { limitDialog("حد Shorts اليومي (دقائق)", "shortsMin") }
        }

        val facebook = card().apply {
            addView(appIcon("Facebook", C_BLUE, fbPkgs), LinearLayout.LayoutParams(dp(42), dp(42)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(12), 0)
                addView(txt("Facebook", 15f, C_TXT, true))
                addView(txt("الحد اليومي: ${lim("fbMin")} د", 12f, C_TEAL, true)
                    .apply { setPadding(0, dp(3), 0, 0) })
                addView(txt("ريلز فيسبوك ممنوع دائماً", 12f, C_GRAY)
                    .apply { setPadding(0, dp(2), 0, 0) })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(txt("✓", 13f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = oval(C_BTN)
            }, LinearLayout.LayoutParams(dp(26), dp(26)))
            isClickable = true
            isFocusable = true
            setOnClickListener { limitDialog("حد فيسبوك اليومي (دقائق)", "fbMin") }
        }

        val note = card(C_MINT).apply {
            addView(txt("✓", 20f, C_TEAL, true))
            addView(txt(
                "يمكنك تخفيض الحد اليومي فقط، ولا يمكن رفعه. التزامٌ بسيط يحمي وقتك.",
                13f, C_TXT
            ).apply { setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        }

        return page(
            header("التطبيقات", "▦"),
            sectionTitle("التطبيقات المحددة"),
            youtube,
            facebook,
            note
        )
    }

    // ---------- الإعدادات ----------

    private fun settings(): View = page(
        header("الإعدادات", "⚙"),
        sectionTitle("الحماية"),
        row("🛡", C_MINT, "تفعيل FocusGuard", "تشغيل أو إيقاف مراقبة التطبيقات",
            trailing = mkSwitch("enabled", true)),
        row("◉", C_MINT, "تفعيل الخدمة (إمكانية الوصول)",
            if (serviceOn()) "مفعّلة ✓" else "غير مفعّلة",
            if (serviceOn()) C_TEAL else C_RED) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        row("▶", C_T_RED, "الحد اليومي لـ Shorts", "${lim("shortsMin")} دقائق", C_TEAL) {
            limitDialog("حد Shorts اليومي (دقائق)", "shortsMin")
        },
        row("◎", C_T_BLUE, "الحد اليومي لفيسبوك", "${lim("fbMin")} دقائق", C_TEAL) {
            limitDialog("حد فيسبوك اليومي (دقائق)", "fbMin")
        },
        row("⊘", C_T_RED, "ريلز فيسبوك", "ممنوع دائماً"),
        sectionTitle("المظهر"),
        row("☾", C_MINT, "الوضع الداكن", "تغيير مظهر التطبيق",
            trailing = mkSwitch("dark", false) { recreate() }),
        sectionTitle("التطبيق"),
        row("➦", C_MINT, "مشاركة التطبيق", "مع من تحب") {
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "FocusGuard: تطبيق لتقليل وقت الفيديوهات القصيرة"),
                null
            ))
        },
        row("ⓘ", C_MINT, "حول التطبيق", "الإصدار 1.0.0") { about() }
    )
}

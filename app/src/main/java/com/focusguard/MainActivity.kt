package com.focusguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val C_DARK = Color.parseColor("#075B4D")
private val C_TEAL = Color.parseColor("#0E806D")
private val C_MINT = Color.parseColor("#DDF3EB")
private val C_BG = Color.parseColor("#F4F8F6")
private val C_RED = Color.parseColor("#D94B55")
private val C_BLUE = Color.parseColor("#3978C8")
private val C_TXT = Color.parseColor("#173B33")
private val C_GRAY = Color.parseColor("#71847D")
private val C_LINE = Color.parseColor("#E4ECE8")

class ShieldView(c: Context) : View(c) {
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
        p.color = C_TEAL
        p.strokeWidth = w * .08f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        val tick = Path()
        tick.moveTo(w * .3f, h * .5f)
        tick.lineTo(w * .45f, h * .65f)
        tick.lineTo(w * .72f, h * .35f)
        cv.drawPath(tick, p)
    }
}

class BarChart(
    c: Context,
    private val days: List<LocalDate>,
    private val a: List<Int>,
    private val b: List<Int>
) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(cv: Canvas) {
        if (days.isEmpty()) return
        val d = resources.displayMetrics.density
        val left = 38 * d
        val bottom = height - 24 * d
        val top = 8 * d
        val right = width - 4 * d
        var mx = maxOf(a.maxOrNull() ?: 0, b.maxOrNull() ?: 0, 5)
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
            cv.drawRect(x - bw, bottom - (bottom - top) * a[i] / mx, x, bottom, p)
            p.color = C_BLUE
            cv.drawRect(x, bottom - (bottom - top) * b[i] / mx, x + bw, bottom, p)

            if (days.size <= 7 || i % 5 == 0) {
                p.color = C_GRAY
                p.textAlign = Paint.Align.CENTER
                val label = if (days.size <= 7) {
                    days[i].dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ar"))
                } else days[i].dayOfMonth.toString()
                cv.drawText(label, x, height - 6 * d, p)
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

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun txt(
        t: String,
        s: Float,
        c: Int = C_TXT,
        bold: Boolean = false
    ) = TextView(this).apply {
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

    private fun gradientBg(top: Int, bottom: Int, radius: Int) =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(top, bottom)
        ).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun lp() =
        LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(18), dp(6), dp(18), dp(6))
        }

    private fun card(color: Int = Color.WHITE) =
        LinearLayout(this).apply {
            background = bg(color, 22)
            elevation = dp(2).toFloat()
            setPadding(dp(16), dp(15), dp(16), dp(15))
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun lim(key: String) =
        minOf(sp.getInt(key, 5), 5)

    private fun fmt(sec: Int) =
        if (sec < 60) "$sec ث" else "${sec / 60} د"

    private fun serviceOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabled.contains(packageName)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        actionBar?.hide()
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1
        )
        window.statusBarColor = C_DARK
        window.navigationBarColor = Color.BLACK

        content = FrameLayout(this)

        nav = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            elevation = dp(12).toFloat()
            weightSum = 4f
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C_BG)
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(nav, LinearLayout.LayoutParams(-1, -2))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        setContentView(root)
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
            Triple("⚙", "الإعدادات", 2),
            Triple("ⓘ", "حول", 3)
        )

        items.forEach { (icon, label, index) ->
            val selected = index == tab

            nav.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = if (selected)
                        bg(C_MINT, 18)
                    else bg(Color.TRANSPARENT, 18)

                    setPadding(dp(4), dp(7), dp(4), dp(7))

                    addView(
                        txt(
                            icon, 22f,
                            if (selected) C_TEAL else C_GRAY,
                            true
                        )
                    )

                    addView(
                        txt(
                            label, 10f,
                            if (selected) C_DARK else C_GRAY,
                            selected
                        )
                    )

                    setOnClickListener {
                        if (index == 3) {
                            about()
                        } else {
                            tab = index
                            render()
                        }
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
                else -> settings()
            }
        )
    }

    private fun page(vararg views: View) =
        ScrollView(this).apply {
            setFillViewport(false)
            isVerticalScrollBarEnabled = false
            setBackgroundColor(C_BG)

            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 0, 0, dp(18))
                    layoutDirection = View.LAYOUT_DIRECTION_RTL

                    views.forEachIndexed { index, view ->
                        if (index == 0) {
                            addView(
                                view,
                                LinearLayout.LayoutParams(-1, -2)
                            )
                        } else {
                            addView(view, lp())
                        }
                    }
                }
            )
        }    private fun header(
        title: String,
        subtitle: String? = null
    ): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradientBg(C_DARK, C_TEAL, 0).also {
                val r = dp(28).toFloat()
                it.cornerRadii = floatArrayOf(
                    0f, 0f, 0f, 0f,
                    r, r, r, r
                )
            }

            setPadding(dp(22), dp(22), dp(22), dp(22))
            layoutDirection = View.LAYOUT_DIRECTION_RTL

            addView(
                LinearLayout(this@MainActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    orientation = LinearLayout.HORIZONTAL

                    addView(
                        LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(txt(title, 23f, Color.WHITE, true))

                            if (!subtitle.isNullOrBlank()) {
                                addView(
                                    txt(
                                        subtitle,
                                        13f,
                                        Color.parseColor("#C7E8DE")
                                    ).apply {
                                        setPadding(0, dp(4), 0, 0)
                                    }
                                )
                            }
                        },
                        LinearLayout.LayoutParams(0, -2, 1f)
                    )

                    if (title == "FocusGuard") {
                        addView(
                            ShieldView(this@MainActivity),
                            LinearLayout.LayoutParams(dp(58), dp(58))
                        )
                    } else {
                        addView(
                            txt(
                                "●", 15f,
                                Color.parseColor("#B7EBDD"),
                                true
                            )
                        )
                    }
                }
            )
        }

    private fun sectionTitle(
        title: String,
        trailing: String? = null
    ): View =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL

            addView(
                txt(title, 17f, C_TXT, true),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            if (!trailing.isNullOrBlank()) {
                addView(txt(trailing, 12f, C_GRAY))
            }
        }

    private fun switchRow(
        title: String,
        sub: String,
        key: String,
        def: Boolean
    ): View =
        card().apply {
            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(txt(title, 15f, C_TXT, true))
                    addView(
                        txt(sub, 12f, C_GRAY).apply {
                            setPadding(0, dp(3), 0, 0)
                        }
                    )
                },
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            addView(
                Switch(this@MainActivity).apply {
                    isChecked = sp.getBoolean(key, def)

                    thumbTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf()
                        ),
                        intArrayOf(
                            Color.WHITE,
                            Color.parseColor("#D5DEDA")
                        )
                    )

                    trackTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf()
                        ),
                        intArrayOf(
                            C_TEAL,
                            Color.parseColor("#DCE5E1")
                        )
                    )

                    contentDescription = title

                    setOnCheckedChangeListener { _, checked ->
                        sp.edit().putBoolean(key, checked).apply()
                        render()
                    }
                }
            )
        }

    private fun linkRow(
        title: String,
        sub: String,
        onClick: () -> Unit
    ): View =
        card().apply {
            isClickable = true
            isFocusable = true

            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(txt(title, 15f, C_TXT, true))
                    addView(
                        txt(sub, 12f, C_TEAL, true).apply {
                            setPadding(0, dp(3), 0, 0)
                        }
                    )
                },
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            addView(txt("‹", 27f, C_GRAY))
            setOnClickListener { onClick() }
        }

    private fun infoRow(title: String, sub: String): View =
        card().apply {
            orientation = LinearLayout.VERTICAL
            addView(txt(title, 15f, C_TXT, true))
            addView(
                txt(sub, 12f, C_GRAY).apply {
                    setPadding(0, dp(3), 0, 0)
                }
            )
        }

    private fun bigBtn(
        title: String,
        onClick: () -> Unit
    ): View =
        txt(title, 15f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = bg(C_TEAL, 18)
            setPadding(0, dp(15), 0, dp(15))
            elevation = dp(2).toFloat()
            setOnClickListener { onClick() }
        }

    private fun sec(title: String): View =
        txt(title, 14f, C_TEAL, true).apply {
            setPadding(dp(5), dp(13), dp(5), dp(3))
        }

    private fun limitDialog(title: String, key: String) {
        val current = lim(key)

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(
                "يمكنك تخفيض الحد فقط، ولا يمكن رفعه (الحد الحالي $current د)."
            )
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val value = input.text.toString().toIntOrNull()

                if (value == null || value < 1) {
                    toast("أدخل رقماً من 1 فأكثر")
                } else if (value > current) {
                    toast("لا يمكن رفع الحد. الحد الحالي $current د")
                } else {
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
            .setMessage(
                "الإصدار 1.0.0\n\n" +
                "يحدّ من Shorts وريلز فيسبوك ويتابع وقت يوتيوب.\n" +
                "بياناتك تبقى على هاتفك ولا يتصل التطبيق بالإنترنت."
            )
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun home(): View {
        val on = serviceOn() && sp.getBoolean("enabled", true)

        val status = card(
            if (on) C_MINT
            else Color.parseColor("#FCEBED")
        ).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(
                LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL

                    addView(
                        txt(
                            if (on) "الحماية مفعّلة" else "الحماية متوقفة",
                            18f,
                            if (on) C_DARK else C_RED,
                            true
                        )
                    )

                    addView(
                        txt(
                            if (on) "FocusGuard يراقب التطبيقات المحددة"
                            else "فعّل الخدمة لبدء حماية وقتك",
                            12f,
                            C_GRAY
                        ).apply {
                            setPadding(0, dp(3), 0, 0)
                        }
                    )
                },
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            addView(
                txt(
                    if (on) "✓" else "!",
                    24f,
                    if (on) C_TEAL else C_RED,
                    true
                )
            )

            if (!on) {
                setOnClickListener {
                    if (!serviceOn()) {
                        startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    } else {
                        sp.edit().putBoolean("enabled", true).apply()
                        render()
                    }
                }
            }
        }

        val shorts = Store.get(this, "shorts")
        val facebook = Store.get(this, "fb")
        val youtube = Store.get(this, "yt")

        fun metric(
            label: String,
            value: Int,
            tint: Int
        ) =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = bg(tint, 16)
                setPadding(dp(5), dp(12), dp(5), dp(12))

                addView(txt(fmt(value), 19f, C_TXT, true))

                addView(
                    txt(label, 11f, C_GRAY).apply {
                        setPadding(0, dp(3), 0, 0)
                    }
                )
            }

        val today = card().apply {
            orientation = LinearLayout.VERTICAL
            addView(sectionTitle("ملخص اليوم", "وقت الاستخدام"))

            addView(
                LinearLayout(this@MainActivity).apply {
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    setPadding(0, dp(12), 0, 0)

                    addView(
                        metric(
                            "Shorts",
                            shorts,
                            Color.parseColor("#FBEDEF")
                        ),
                        LinearLayout.LayoutParams(0, -2, 1f).apply {
                            setMargins(dp(3), 0, dp(3), 0)
                        }
                    )

                    addView(
                        metric(
                            "فيسبوك",
                            facebook,
                            Color.parseColor("#EAF1FC")
                        ),
                        LinearLayout.LayoutParams(0, -2, 1f).apply {
                            setMargins(dp(3), 0, dp(3), 0)
                        }
                    )

                    addView(
                        metric("يوتيوب", youtube, C_MINT),
                        LinearLayout.LayoutParams(0, -2, 1f).apply {
                            setMargins(dp(3), 0, dp(3), 0)
                        }
                    )
                }
            )
        }

        return page(
            header("FocusGuard", "ركّز أكثر، وحقّق أهدافك"),
            status,
            sectionTitle("إدارة الحماية").apply {
                setPadding(dp(22), dp(12), dp(22), dp(2))
            },
            switchRow(
                "الحد من YouTube Shorts",
                if (sp.getBoolean("allowShorts", false))
                    "مسموح حتى الحد اليومي"
                else "محظور بالكامل",
                "allowShorts",
                false
            ),
            linkRow(
                "الحد اليومي لمشاهدة Shorts",
                "${lim("shortsMin")} دقائق"
            ) {
                limitDialog("حد Shorts اليومي (دقائق)", "shortsMin")
            },
            linkRow(
                "الحد اليومي لفيسبوك",
                "${lim("fbMin")} دقائق"
            ) {
                limitDialog("حد فيسبوك اليومي (دقائق)", "fbMin")
            },
            today,
            bigBtn("عرض الإحصائيات") {
                tab = 1
                render()
            }
        )
    }    private fun total(name: String): Int {
        val regex = Regex("^$name(\\d{4}-\\d{2}-\\d{2})$")
        val from = LocalDate.now().minusDays(range - 1L)
        var sum = 0

        for ((key, value) in sp.all) {
            val match = regex.find(key) ?: continue

            if (
                range == 0 ||
                !LocalDate.parse(match.groupValues[1]).isBefore(from)
            ) {
                sum += (value as? Int) ?: 0
            }
        }

        return sum / 60
    }

    private fun stats(): View {
        val chips = LinearLayout(this).apply {
            weightSum = 3f
            layoutDirection = View.LAYOUT_DIRECTION_RTL

            for ((r, label) in listOf(
                7 to "7 أيام",
                30 to "30 يوم",
                0 to "كل الوقت"
            )) {
                addView(
                    txt(
                        label,
                        13f,
                        if (range == r) Color.WHITE else C_TXT,
                        true
                    ).apply {
                        gravity = Gravity.CENTER
                        background = bg(
                            if (range == r) C_TEAL else Color.WHITE,
                            16
                        )
                        setPadding(0, dp(11), 0, dp(11))
                        setOnClickListener {
                            range = r
                            render()
                        }
                    },
                    LinearLayout.LayoutParams(0, -2, 1f).apply {
                        setMargins(dp(4), 0, dp(4), 0)
                    }
                )
            }
        }

        fun sum(
            label: String,
            value: Int,
            tint: Int
        ) =
            card(tint).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(txt(label, 13f, C_GRAY, true))
                addView(
                    txt("$value د", 24f, C_TXT, true).apply {
                        setPadding(0, dp(4), 0, 0)
                    }
                )
            }

        val sums = LinearLayout(this).apply {
            weightSum = 2f
            layoutDirection = View.LAYOUT_DIRECTION_RTL

            addView(
                sum(
                    "مشاهدة فيسبوك",
                    total("fb"),
                    Color.parseColor("#EAF1FC")
                ),
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            )

            addView(
                sum(
                    "مشاهدة Shorts",
                    total("shorts"),
                    Color.parseColor("#FBEDEF")
                ),
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            )
        }

        val count = if (range == 7) 7 else 30

        val days = (count - 1 downTo 0).map {
            LocalDate.now().minusDays(it.toLong())
        }

        val chart = BarChart(
            this,
            days,
            days.map { sp.getInt("shorts$it", 0) / 60 },
            days.map { sp.getInt("fb$it", 0) / 60 }
        )

        val chartCard = card().apply {
            orientation = LinearLayout.VERTICAL
            addView(txt("مخطط الاستخدام", 16f, C_TXT, true))

            addView(
                LinearLayout(this@MainActivity).apply {
                    setPadding(0, dp(8), 0, dp(4))
                    addView(txt("● Shorts  ", 11f, C_RED, true))
                    addView(txt("● Facebook", 11f, C_BLUE, true))
                }
            )

            addView(
                chart,
                LinearLayout.LayoutParams(-1, dp(220))
            )
        }

        val used = Store.get(this, "shorts") +
                Store.get(this, "fb")

        val tip = card(C_MINT).apply {
            orientation = LinearLayout.VERTICAL
            addView(txt("تذكير لطيف", 15f, C_DARK, true))

            addView(
                txt(
                    if (used < 300)
                        "أحسنت! حافظ على توازنك واستمر في التركيز على أهدافك."
                    else
                        "جرّب تقليل وقت الفيديوهات القصيرة اليوم وخصص وقتاً لما يهمك.",
                    13f,
                    C_GRAY
                ).apply {
                    setPadding(0, dp(4), 0, 0)
                }
            )
        }

        return page(
            header("الإحصائيات", "تابع استخدامك بوضوح"),
            chips,
            sums,
            chartCard,
            tip,
            bigBtn("إعادة تعيين الإحصائيات") {
                resetStats()
            }
        )
    }

    private fun resetStats() {
        AlertDialog.Builder(this)
            .setTitle("إعادة تعيين الإحصائيات")
            .setMessage(
                "سيتم حذف إحصائيات الأيام السابقة. " +
                "وقت اليوم يبقى كما هو حتى لا تُتجاوز الحدود."
            )
            .setPositiveButton("حذف") { _, _ ->
                val regex = Regex(
                    "^(fb|shorts|yt|ytAsked|studyAnswers|watchAnswers)\\d{4}-\\d{2}-\\d{2}$"
                )

                val today = LocalDate.now().toString()
                val editor = sp.edit()

                sp.all.keys
                    .filter {
                        regex.matches(it) && !it.endsWith(today)
                    }
                    .forEach {
                        editor.remove(it)
                    }

                editor.apply()
                render()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun settings(): View =
        page(
            header("الإعدادات", "خصص الحماية حسب احتياجك"),

            sec("إعدادات الحماية"),

            switchRow(
                "تفعيل FocusGuard",
                "تشغيل أو إيقاف مراقبة التطبيقات",
                "enabled",
                true
            ),

            switchRow(
                "السماح بـ Shorts حتى الحد اليومي",
                "إيقافه يحظر Shorts بالكامل",
                "allowShorts",
                false
            ),

            infoRow(
                "ريلز فيسبوك",
                "ممنوع دائماً"
            ),

            linkRow(
                "تفعيل الخدمة (إمكانية الوصول)",
                if (serviceOn()) "مفعّلة ✓" else "غير مفعّلة"
            ) {
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            },

            sec("الحدود اليومية"),

            linkRow(
                "الحد اليومي لـ Shorts",
                "${lim("shortsMin")} دقائق"
            ) {
                limitDialog("حد Shorts اليومي (دقائق)", "shortsMin")
            },

            linkRow(
                "الحد اليومي لفيسبوك",
                "${lim("fbMin")} دقائق"
            ) {
                limitDialog("حد فيسبوك اليومي (دقائق)", "fbMin")
            },

            sec("معلومات التطبيق"),

            linkRow(
                "مشاركة التطبيق",
                "مع من تحب"
            ) {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(
                                Intent.EXTRA_TEXT,
                                "FocusGuard: تطبيق لتقليل وقت الفيديوهات القصيرة"
                            ),
                        null
                    )
                )
            },

            linkRow(
                "حول التطبيق",
                "الإصدار 1.0.0"
            ) {
                about()
            }
        )
}

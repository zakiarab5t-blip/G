package com.focusguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import java.util.Locale

private val GOLD_L = Color.parseColor("#F8E7A8")
private val GOLD = Color.parseColor("#E8C872")
private val GOLD_D = Color.parseColor("#B8933F")
private val EMERALD = Color.parseColor("#1FBF9F")
private val INK = Color.parseColor("#031210")
private val IVORY = Color.parseColor("#F3EBD3")
private val MUTED = Color.parseColor("#9DB5AD")
private val RED = Color.parseColor("#E0626B")
private val YT_RED = Color.parseColor("#E53935")
private val FB_BLUE = Color.parseColor("#1877F2")

/** أقصى حد يومي مسموح به (بالدقائق). يمكن تخفيضه فقط ولا يمكن رفعه. */
private const val MAX_LIMIT = 5

/** خلفية فاخرة: تدرج زمردي عميق مع توهّج ذهبي وحلقات رفيعة. */
class LuxBackground(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var base: Shader? = null
    private var glowTop: Shader? = null
    private var glowBottom: Shader? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val wf = w.toFloat()
        val hf = h.toFloat()
        base = LinearGradient(
            0f, 0f, 0f, hf,
            intArrayOf(Color.parseColor("#031210"), Color.parseColor("#0A3A32"), Color.parseColor("#020B0A")),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        glowTop = RadialGradient(
            wf * .5f, hf * .1f, wf * 1.0f,
            intArrayOf(0x40E8C872, 0x00E8C872), null, Shader.TileMode.CLAMP
        )
        glowBottom = RadialGradient(
            wf * .9f, hf * .92f, wf * 1.1f,
            intArrayOf(0x331FBF9F, 0x001FBF9F), null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.shader = base
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = glowTop
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = glowBottom
        cv.drawRect(0f, 0f, w, h, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = resources.displayMetrics.density * 1.2f
        p.color = 0x22E8C872
        for (r in floatArrayOf(.5f, .72f, .94f)) cv.drawCircle(w * .5f, h * .1f, w * r, p)
        for (r in floatArrayOf(.4f, .65f)) cv.drawCircle(w * .9f, h * .92f, w * r, p)
    }
}

/** شعار الدرع الذهبي. علامة صح عند التفعيل، وعلامة تعجب عند التوقف. */
class ShieldView(c: Context, private val ok: Boolean) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outer = Path()
    private val inner = Path()
    private val tick = Path()
    private var gold: Shader? = null
    private var deep: Shader? = null
    private var glow: Shader? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val wf = w.toFloat()
        val hf = h.toFloat()
        outer.reset()
        outer.moveTo(wf * .5f, hf * .08f)
        outer.lineTo(wf * .88f, hf * .22f)
        outer.lineTo(wf * .88f, hf * .52f)
        outer.cubicTo(wf * .88f, hf * .76f, wf * .7f, hf * .9f, wf * .5f, hf * .98f)
        outer.cubicTo(wf * .3f, hf * .9f, wf * .12f, hf * .76f, wf * .12f, hf * .52f)
        outer.lineTo(wf * .12f, hf * .22f)
        outer.close()
        inner.set(outer)
        val m = Matrix()
        m.setScale(.8f, .8f, wf * .5f, hf * .55f)
        inner.transform(m)
        tick.reset()
        tick.moveTo(wf * .3f, hf * .5f)
        tick.lineTo(wf * .45f, hf * .65f)
        tick.lineTo(wf * .72f, hf * .35f)
        gold = LinearGradient(
            0f, 0f, wf, hf,
            intArrayOf(Color.parseColor("#F8E7A8"), Color.parseColor("#D4AF55"), Color.parseColor("#8F6B1E")),
            null, Shader.TileMode.CLAMP
        )
        deep = LinearGradient(
            0f, 0f, 0f, hf,
            intArrayOf(Color.parseColor("#0E5A4D"), Color.parseColor("#042620")),
            null, Shader.TileMode.CLAMP
        )
        glow = RadialGradient(
            wf * .5f, hf * .5f, wf * .75f,
            intArrayOf(0x55E8C872, 0x00E8C872), null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.shader = glow
        cv.drawCircle(w * .5f, h * .5f, w * .5f, p)
        p.shader = gold
        cv.drawPath(outer, p)
        p.shader = deep
        cv.drawPath(inner, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = w * .07f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        if (ok) {
            p.shader = gold
            cv.drawPath(tick, p)
        } else {
            p.shader = null
            p.color = RED
            cv.drawLine(w * .5f, h * .32f, w * .5f, h * .55f, p)
            p.style = Paint.Style.FILL
            cv.drawCircle(w * .5f, h * .68f, w * .04f, p)
        }
    }
}

class MainActivity : Activity() {
    private val sp by lazy { Store.p(this) }
    private lateinit var scroll: ScrollView

    private val ytPkgs = arrayOf("com.google.android.youtube")
    private val fbPkgs = arrayOf("com.facebook.katana", "com.facebook.lite")

    private val ar: Boolean get() = (sp.getString("lang", "ar") ?: "ar") == "ar"
    private fun t(a: String, e: String) = if (ar) a else e

    /** تطبيق لغة التطبيق المختارة (عربي / إنجليزي) واتجاه الواجهة معها. */
    override fun attachBaseContext(base: Context) {
        val code = Store.p(base).getString("lang", "ar") ?: "ar"
        val loc = Locale.forLanguageTag(code)
        Locale.setDefault(loc)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        super.attachBaseContext(base.createConfigurationContext(cfg))
    }

    // ---------- أدوات مساعدة ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun txt(text: String, size: Float, color: Int = IVORY, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun bg(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    /** سطح زجاجي شفاف بحدّ ذهبي رفيع. */
    private fun glass(radius: Int, fill: Int = 0x14FFFFFF, stroke: Int = 0x55E8C872) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun goldBg(radius: Int) =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(GOLD_L, GOLD, GOLD_D)).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun lim(key: String) = minOf(sp.getInt(key, MAX_LIMIT), MAX_LIMIT)

    private fun fmt(sec: Int): String {
        val m = sec / 60
        return if (sec < 60) t("$sec ث", "${sec}s") else t("$m د", "${m}m")
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

    /** أيقونة التطبيق الحقيقية من الهاتف إن كان مثبتاً، وإلا مربع بحرف التطبيق. */
    private fun appIcon(label: String, fallback: Int, pkgs: Array<String>): View {
        for (pkg in pkgs) {
            try {
                val d = packageManager.getApplicationIcon(pkg)
                return ImageView(this).apply {
                    setImageDrawable(d)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = bg(0x22FFFFFF, 16)
                    clipToOutline = true
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // غير مثبت: ننتقل للحزمة التالية أو للبديل
            }
        }
        return txt(label.take(1), 26f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = bg(fallback, 16)
        }
    }

    private fun bar(pct: Int, color: Int): View = LinearLayout(this).apply {
        weightSum = 100f
        background = bg(0x22FFFFFF, 6)
        val v = pct.coerceIn(0, 100)
        if (v > 0) {
            addView(View(this@MainActivity).apply { background = bg(color, 6) },
                LinearLayout.LayoutParams(0, dp(8), v.toFloat()))
        }
        addView(View(this@MainActivity), LinearLayout.LayoutParams(0, dp(8), (100 - v).toFloat()))
    }

    // ---------- دورة الحياة ----------

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        title = "FocusGuard"
        actionBar?.hide()
        // الحد الأقصى اليومي هو التحكم الوحيد: الحماية مفعّلة وShorts محكومة بالحد
        sp.edit().putBoolean("enabled", true).putBoolean("allowShorts", true).apply()
        if (b == null &&
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        window.statusBarColor = Color.parseColor("#031210")
        window.navigationBarColor = Color.parseColor("#020B0A")

        scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }
        val root = FrameLayout(this).apply {
            addView(LuxBackground(this@MainActivity), FrameLayout.LayoutParams(-1, -1))
            addView(scroll, FrameLayout.LayoutParams(-1, -1))
        }
        // edge-to-edge (أندرويد 15+): ترك مساحة لشريطي الحالة والتنقل
        root.setOnApplyWindowInsetsListener { _, insets ->
            applyInsets(insets)
            insets
        }
        setContentView(root)
    }

    @Suppress("DEPRECATION")
    private fun applyInsets(insets: WindowInsets) {
        val top: Int
        val bottom: Int
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            top = bars.top
            bottom = bars.bottom
        } else {
            top = insets.systemWindowInsetTop
            bottom = insets.systemWindowInsetBottom
        }
        scroll.setPadding(0, top, 0, bottom)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ---------- الواجهة ----------

    private fun langPill(): View = LinearLayout(this).apply {
        background = glass(18)
        setPadding(dp(3), dp(3), dp(3), dp(3))
        for ((code, label) in listOf("ar" to "العربية", "en" to "English")) {
            val selected = (if (ar) "ar" else "en") == code
            addView(txt(label, 13f, if (selected) INK else MUTED, selected).apply {
                gravity = Gravity.CENTER
                background = if (selected) goldBg(15) else bg(Color.TRANSPARENT, 15)
                setPadding(dp(14), dp(7), dp(14), dp(7))
                setOnClickListener {
                    if (!selected) {
                        sp.edit().putString("lang", code).apply()
                        recreate()
                    }
                }
            })
        }
    }

    private fun appCard(
        name: String,
        sub: String,
        pkgs: Array<String>,
        fallback: Int,
        usedSec: Int,
        key: String
    ): View {
        val limit = lim(key)
        val limitSec = limit * 60
        val over = usedSec >= limitSec
        val pct = (usedSec * 100L / maxOf(1, limitSec)).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glass(24)
            elevation = dp(4).toFloat()
            setPadding(dp(18), dp(18), dp(18), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                limitDialog(
                    t("الحد الأقصى اليومي · $name", "Daily maximum · $name"), key
                )
            }

            addView(LinearLayout(this@MainActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(appIcon(name, fallback, pkgs), LinearLayout.LayoutParams(dp(58), dp(58)))
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), 0, dp(14), 0)
                    addView(txt(name, 18f, IVORY, true))
                    addView(txt(sub, 12f, MUTED).apply { setPadding(0, dp(3), 0, 0) })
                }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    addView(txt(t("$limit د", "$limit min"), 22f, GOLD, true))
                    addView(txt(t("الحد الأقصى", "Maximum"), 11f, MUTED))
                })
            })

            addView(bar(pct, if (over) RED else GOLD), lp(16))

            addView(LinearLayout(this@MainActivity).apply {
                addView(txt(t("اليوم: ${fmt(usedSec)}", "Today: ${fmt(usedSec)}"), 12f,
                    if (over) RED else IVORY), LinearLayout.LayoutParams(0, -2, 1f))
                addView(txt(t("اضغط لتخفيض الحد", "Tap to lower the limit"), 11f, MUTED))
            }, lp(8))
        }
    }

    private fun render() {
        val on = serviceOn()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(28))
        }

        col.addView(LinearLayout(this).apply {
            gravity = Gravity.END
            addView(langPill())
        }, lp())

        // الشعار والعنوان
        col.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(ShieldView(this@MainActivity, on), LinearLayout.LayoutParams(dp(116), dp(116)))
            addView(txt("FocusGuard", 32f, GOLD, true).apply {
                letterSpacing = 0.06f
                setPadding(0, dp(6), 0, 0)
            })
            addView(txt(t("ركّز أكثر، وحقّق أهدافك", "Focus more. Achieve more."), 14f, MUTED)
                .apply { setPadding(0, dp(4), 0, 0) })
        }, lp(10))

        // حالة الحماية
        if (on) {
            col.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER
                background = glass(22, 0x221FBF9F, 0x661FBF9F)
                setPadding(dp(18), dp(11), dp(18), dp(11))
                addView(txt("●", 12f, EMERALD, true))
                addView(txt(t("الحماية مفعّلة", "Protection active"), 15f, IVORY, true)
                    .apply { setPadding(dp(8), 0, 0, 0) })
            }, lp(18))
        } else {
            col.addView(txt(t("تفعيل الحماية", "Activate protection"), 16f, INK, true).apply {
                gravity = Gravity.CENTER
                background = goldBg(22)
                elevation = dp(4).toFloat()
                setPadding(0, dp(16), 0, dp(16))
                isClickable = true
                setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            }, lp(18))
            col.addView(txt(
                t("فعّل خدمة FocusGuard من إعدادات إمكانية الوصول",
                    "Turn on FocusGuard in Accessibility settings"),
                12f, MUTED
            ).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) }, lp())
        }

        // البطاقتان
        col.addView(txt(t("الحد الأقصى اليومي", "Daily maximum"), 17f, GOLD, true)
            .apply { setPadding(dp(4), 0, 0, 0) }, lp(26))
        col.addView(
            appCard(
                "YouTube", t("مشاهدة Shorts", "Shorts viewing"),
                ytPkgs, YT_RED, Store.get(this, "shorts"), "shortsMin"
            ), lp(12)
        )
        col.addView(
            appCard(
                "Facebook", t("الريلز ممنوع دائماً", "Reels always blocked"),
                fbPkgs, FB_BLUE, Store.get(this, "fb"), "fbMin"
            ), lp(12)
        )

        col.addView(txt(
            t("✦  يمكنك تخفيض الحد فقط، ولا يمكن رفعه",
                "✦  You can only lower the limit, never raise it"),
            12f, MUTED
        ).apply { gravity = Gravity.CENTER }, lp(20))

        scroll.removeAllViews()
        scroll.addView(col)
    }

    private fun limitDialog(title: String, key: String) {
        val current = lim(key)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(title)
            .setMessage(t(
                "يمكنك تخفيض الحد فقط، ولا يمكن رفعه (الحد الحالي $current د).",
                "You can only lower the limit, never raise it (current: $current min)."
            ))
            .setView(input)
            .setPositiveButton(t("حفظ", "Save")) { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value == null || value < 1) toast(t("أدخل رقماً من 1 فأكثر", "Enter a number from 1 up"))
                else if (value > current) toast(t(
                    "لا يمكن رفع الحد. الحد الحالي $current د",
                    "The limit can't be raised. Current: $current min"
                ))
                else {
                    sp.edit().putInt(key, value).apply()
                    render()
                }
            }
            .setNegativeButton(t("إلغاء", "Cancel"), null)
            .show()
    }
}

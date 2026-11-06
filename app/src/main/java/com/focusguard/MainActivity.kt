package com.focusguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val sp = Store.p(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 48, 48, 48) }

        fun label(t: String) = TextView(this).apply { text = t; textSize = 16f; setPadding(0, 24, 0, 8) }

        // حد أقصى 5 دقائق: يمكن تخفيضه فقط ولا يمكن رفعه
        fun limitRow(title: String, key: String) {
            root.addView(label(title))
            val cur = minOf(sp.getInt(key, 5), 5)
            val et = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(cur.toString())
            }
            root.addView(et)
            root.addView(Button(this).apply {
                text = "حفظ"
                setOnClickListener {
                    val now = minOf(sp.getInt(key, 5), 5)
                    val v = et.text.toString().toIntOrNull()
                    when {
                        v == null || v < 1 -> Toast.makeText(context, "أدخل رقماً من 1 فأكثر", Toast.LENGTH_SHORT).show()
                        v > now -> {
                            et.setText(now.toString())
                            Toast.makeText(context, "لا يمكن رفع الحد. الحد الحالي $now د", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            sp.edit().putInt(key, v).apply()
                            Toast.makeText(context, "تم الحفظ: $v د (لا يمكن رفعه لاحقاً)", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }

        root.addView(Button(this).apply {
            text = "1) تفعيل الخدمة (إمكانية الوصول)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Switch(this).apply {
            text = "السماح بمشاهدة Shorts بحد أقصى"
            isChecked = sp.getBoolean("allowShorts", false)
            setOnCheckedChangeListener { _, v -> sp.edit().putBoolean("allowShorts", v).apply() }
        })
        limitRow("الحد اليومي لـ Shorts (أقصاه 5 دقائق)", "shortsMin")
        limitRow("الحد اليومي لفيسبوك (أقصاه 5 دقائق)", "fbMin")
        root.addView(label("ريلز فيسبوك: ممنوع دائماً"))

        val stats = TextView(this).apply { textSize = 15f; setPadding(0, 32, 0, 0); gravity = Gravity.START }
        root.addView(stats)
        root.addView(Button(this).apply {
            text = "تحديث الإحصائيات"
            setOnClickListener {
                stats.text = "يوتيوب: ${Store.get(context, "yt") / 60} د\n" +
                    "Shorts: ${Store.get(context, "shorts") / 60} د\n" +
                    "فيسبوك: ${Store.get(context, "fb") / 60} د\n" +
                    "أجبت «أدرس»: ${Store.get(context, "studyAnswers")} مرة | «أشاهد»: ${Store.get(context, "watchAnswers")} مرة"
            }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}

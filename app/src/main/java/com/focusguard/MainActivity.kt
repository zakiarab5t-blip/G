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
        fun num(key: String, def: Int) = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(sp.getInt(key, def).toString())
            addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    sp.edit().putInt(key, s.toString().toIntOrNull() ?: def).apply()
                }
                override fun beforeTextChanged(a: CharSequence?, x: Int, y: Int, z: Int) {}
                override fun onTextChanged(a: CharSequence?, x: Int, y: Int, z: Int) {}
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
        root.addView(label("الحد اليومي لـ Shorts (دقائق)")); root.addView(num("shortsMin", 5))
        root.addView(label("الحد اليومي لفيسبوك (دقائق)")); root.addView(num("fbMin", 5))

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

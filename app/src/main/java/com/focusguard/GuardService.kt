package com.focusguard

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.*
import android.os.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.time.LocalDate

const val YT = "com.google.android.youtube"
const val FB = "com.facebook.katana"

object Store {
    fun p(c: Context): SharedPreferences = c.getSharedPreferences("guard", 0)
    private fun k(n: String) = n + LocalDate.now()
    fun get(c: Context, n: String) = p(c).getInt(k(n), 0)
    fun add(c: Context, n: String, d: Int = 1) = p(c).edit().putInt(k(n), get(c, n) + d).apply()
    fun set(c: Context, n: String, v: Int) = p(c).edit().putInt(k(n), v).apply()
}

class GuardService : AccessibilityService() {
    private var pkg = ""
    private val h = Handler(Looper.getMainLooper())
    private val STEP = 2

    private val tick = Runnable { check() }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val p = e.packageName?.toString() ?: return
        if (p.contains("systemui") || p.contains("inputmethod") || p.contains("keyboard")) return
        pkg = p
        h.removeCallbacks(tick)
        if (p == YT || p == FB) h.postDelayed(tick, (STEP * 1000).toLong())
    }

    private fun isShorts(root: AccessibilityNodeInfo?): Boolean {
        root ?: return false
        return listOf("reel_player_page_container", "reel_recycler", "shorts_player_container")
            .any { root.findAccessibilityNodeInfosByViewId("$YT:id/$it").isNotEmpty() }
    }

    private fun check() {
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        val sp = Store.p(this)
        when (pkg) {
            YT -> {
                Store.add(this, "yt", STEP)
                if (isShorts(rootInActiveWindow)) {
                    Store.add(this, "shorts", STEP)
                    val allowed = sp.getBoolean("allowShorts", false)
                    val limitSec = sp.getInt("shortsMin", 5) * 60
                    if (!allowed || Store.get(this, "shorts") > limitSec) kick(
                        if (allowed) "انتهى وقت Shorts اليوم" else "Shorts ممنوع"
                    )
                }
                if (Store.get(this, "yt") >= 3600 * (Store.get(this, "ytAsked") + 1)) {
                    Store.add(this, "ytAsked"); ask()
                }
            }
            FB -> {
                Store.add(this, "fb", STEP)
                if (Store.get(this, "fb") > sp.getInt("fbMin", 5) * 60) kick("انتهى وقت فيسبوك اليوم")
            }
            else -> return
        }
        h.postDelayed(tick, (STEP * 1000).toLong())
    }

    private fun kick(msg: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun ask() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("g", "FocusGuard", NotificationManager.IMPORTANCE_HIGH))
        fun act(ans: String, label: String) = Notification.Action.Builder(
            null, label,
            PendingIntent.getBroadcast(
                this, ans.hashCode(),
                Intent(this, AnswerReceiver::class.java).putExtra("ans", ans),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
        val n = Notification.Builder(this, "g")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ساعة على يوتيوب ⏰")
            .setContentText("هل تدرس أم تشاهد؟")
            .addAction(act("study", "أدرس 📚"))
            .addAction(act("watch", "أشاهد 🎬"))
            .setAutoCancel(true).build()
        nm.notify(1, n)
    }
}

class AnswerReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.cancel(1)
        if (i.getStringExtra("ans") == "watch") {
            Store.add(c, "watchAnswers")
            Toast.makeText(c, "حسناً، خذ استراحة وارجع لأهدافك 💪", Toast.LENGTH_LONG).show()
        } else {
            Store.add(c, "studyAnswers")
            Toast.makeText(c, "بالتوفيق في الدراسة 📚", Toast.LENGTH_LONG).show()
        }
    }
}

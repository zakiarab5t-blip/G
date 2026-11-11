package com.focusguard

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.*
import android.graphics.Rect
import android.os.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.time.LocalDate

const val YT = "com.google.android.youtube"
val FBS = setOf("com.facebook.katana", "com.facebook.lite")

object Store {
    fun p(c: Context): SharedPreferences = c.getSharedPreferences("guard", 0)
    private fun k(n: String) = n + LocalDate.now()
    fun get(c: Context, n: String) = p(c).getInt(k(n), 0)
    fun add(c: Context, n: String, d: Int = 1) = p(c).edit().putInt(k(n), get(c, n) + d).apply()
    fun set(c: Context, n: String, v: Int) = p(c).edit().putInt(k(n), v).apply()
}

/** نص بلغة التطبيق المختارة (عربي افتراضياً). */
fun tr(c: Context, ar: String, en: String) =
    if (Store.p(c).getString("lang", "ar") == "en") en else ar

class GuardService : AccessibilityService() {
    private var pkg = ""
    private val h = Handler(Looper.getMainLooper())
    private val STEP = 2
    private val seen = LinkedHashSet<String>()
    private var fbHits = 0

    private val tick = Runnable { check() }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val p = e.packageName?.toString() ?: return
        if (p.contains("systemui") || p.contains("inputmethod") || p.contains("keyboard")) return
        if (p != pkg) fbHits = 0
        pkg = p
        h.removeCallbacks(tick)
        if (p == YT || p in FBS) h.postDelayed(tick, (STEP * 1000).toLong())
    }

    private fun isShorts(root: AccessibilityNodeInfo?): Boolean {
        root ?: return false
        return listOf("reel_player_page_container", "reel_recycler", "shorts_player_container")
            .any { root.findAccessibilityNodeInfosByViewId("$YT:id/$it").isNotEmpty() }
    }

    // ريلز فيسبوك: عنصر باسم ريلز يملأ الشاشة تقريباً (طولاً وعرضاً)
    private fun isFbReels(root: AccessibilityNodeInfo?): Boolean {
        root ?: return false
        val dm = resources.displayMetrics
        if (dm.heightPixels < dm.widthPixels) return false // وضع أفقي: ليس ريلز
        val r = Rect()
        var count = 0
        var found = false
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 20 || count++ > 800 || found) return
            val id = n.viewIdResourceName?.lowercase() ?: ""
            val d = n.contentDescription?.toString()?.lowercase() ?: ""
            if (id.contains("reel") || d.contains("reel") || d.contains("ريل")) {
                n.getBoundsInScreen(r)
                val hp = r.height() * 100 / dm.heightPixels
                val wp = r.width() * 100 / dm.widthPixels
                if (seen.size < 15) seen.add("${id.substringAfter(":id/")}|${d.take(25)}|h=$hp% w=$wp%")
                if (n.isVisibleToUser && hp >= 80 && wp >= 95) found = true
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        if (seen.isNotEmpty()) Store.p(this).edit().putString("fbDump", seen.joinToString("\n")).apply()
        return found
    }

    private fun check() {
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        if (!Store.p(this).getBoolean("enabled", true)) return
        val sp = Store.p(this)
        when (pkg) {
            YT -> {
                Store.add(this, "yt", STEP)
                if (isShorts(rootInActiveWindow)) {
                    Store.add(this, "shorts", STEP)
                    val allowed = sp.getBoolean("allowShorts", false)
                    val limitSec = minOf(sp.getInt("shortsMin", 5), 5) * 60
                    if (!allowed || Store.get(this, "shorts") > limitSec) kick(
                        if (allowed) tr(this, "انتهى وقت Shorts اليوم", "Today's Shorts time is up")
                        else tr(this, "Shorts ممنوع", "Shorts are blocked")
                    )
                }
                if (Store.get(this, "yt") >= 3600 * (Store.get(this, "ytAsked") + 1)) {
                    Store.add(this, "ytAsked"); ask()
                }
            }
            in FBS -> {
                Store.add(this, "fb", STEP)
                if (isFbReels(rootInActiveWindow)) fbHits++ else fbHits = 0
                if (fbHits >= 2) { fbHits = 0; kick(tr(this, "ريلز فيسبوك ممنوع", "Facebook Reels are blocked")) }
                else if (Store.get(this, "fb") > minOf(sp.getInt("fbMin", 5), 5) * 60) kick(tr(this, "انتهى وقت فيسبوك اليوم", "Today's Facebook time is up"))
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
            .setContentTitle(tr(this, "ساعة على يوتيوب ⏰", "One hour on YouTube ⏰"))
            .setContentText(tr(this, "هل تدرس أم تشاهد؟", "Studying or watching?"))
            .addAction(act("study", tr(this, "أدرس 📚", "Studying 📚")))
            .addAction(act("watch", tr(this, "أشاهد 🎬", "Watching 🎬")))
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
            Toast.makeText(c, tr(c, "حسناً، خذ استراحة وارجع لأهدافك 💪", "Okay, take a break and get back to your goals 💪"), Toast.LENGTH_LONG).show()
        } else {
            Store.add(c, "studyAnswers")
            Toast.makeText(c, tr(c, "بالتوفيق في الدراسة 📚", "Good luck studying 📚"), Toast.LENGTH_LONG).show()
        }
    }
}

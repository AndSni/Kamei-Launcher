package com.asnidev.kamei.system

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

/** What sort of notification it is, from the app's own category (or its style). */
enum class NotifKind { MESSAGE, EMAIL, CALL, EVENT, REMINDER, ALARM, SOCIAL, PROMO, SYSTEM, OTHER }

/**
 * One waiting notification. Its text is held in memory only while the
 * notification exists, so the home screen can reveal it on a long hold.
 */
data class Pending(
    val key: String,
    val pkg: String,
    val userSerial: Long,
    val postTime: Long,
    val kind: NotifKind,
    val title: String,
    val text: String,
)

/** Reads waiting notifications for the home screen. Nothing is written to disk or sent anywhere. */
class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = WeakReference(this)
        publish()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    override fun onListenerDisconnected() {
        instance = WeakReference(null)
        _pending.value = emptyList()
    }

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        val users = getSystemService(android.os.UserManager::class.java)
        val shown = active.filter { sbn ->
            sbn.isClearable && !sbn.isOngoing && sbn.packageName != packageName &&
                (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
        }
        intents.clear()
        autoCancels.clear()
        _pending.value = shown.map { sbn ->
            val n = sbn.notification
            val e = n.extras
            n.contentIntent?.let { intents[sbn.key] = it }
            if ((n.flags and Notification.FLAG_AUTO_CANCEL) != 0) autoCancels += sbn.key
            Pending(
                key = sbn.key,
                pkg = sbn.packageName,
                userSerial = runCatching { users.getSerialNumberForUser(sbn.user) }.getOrDefault(0L),
                postTime = sbn.postTime,
                kind = kind(n),
                title = (e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) ?: e.getCharSequence(Notification.EXTRA_TITLE))?.toString().orEmpty(),
                text = text(n),
            )
        }.sortedByDescending { it.postTime }
    }

    /** The newest message of a conversation, else the expanded text, else the one-line text. */
    private fun text(n: Notification): String {
        val e = n.extras
        val messages = e.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
            (messages.last() as? android.os.Bundle)?.let { b ->
                val who = b.getCharSequence("sender")?.toString()
                val what = b.getCharSequence("text")?.toString().orEmpty()
                return if (who.isNullOrBlank()) what else "$who: $what"
            }
        }
        return (e.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: e.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
    }

    private fun kind(n: Notification): NotifKind = when (n.category) {
        Notification.CATEGORY_MESSAGE -> NotifKind.MESSAGE
        Notification.CATEGORY_EMAIL -> NotifKind.EMAIL
        Notification.CATEGORY_CALL, Notification.CATEGORY_MISSED_CALL -> NotifKind.CALL
        Notification.CATEGORY_EVENT -> NotifKind.EVENT
        Notification.CATEGORY_REMINDER -> NotifKind.REMINDER
        Notification.CATEGORY_ALARM -> NotifKind.ALARM
        Notification.CATEGORY_SOCIAL -> NotifKind.SOCIAL
        Notification.CATEGORY_PROMO, Notification.CATEGORY_RECOMMENDATION -> NotifKind.PROMO
        Notification.CATEGORY_SYSTEM, Notification.CATEGORY_STATUS, Notification.CATEGORY_ERROR, Notification.CATEGORY_SERVICE -> NotifKind.SYSTEM
        else -> if (n.extras.containsKey(Notification.EXTRA_MESSAGES) || n.extras.getString(Notification.EXTRA_TEMPLATE)?.endsWith("MessagingStyle") == true) NotifKind.MESSAGE
            else NotifKind.OTHER
    }

    companion object {
        private val _pending = MutableStateFlow<List<Pending>>(emptyList())
        val pending: StateFlow<List<Pending>> = _pending
        private var instance = WeakReference<NotifListener>(null)
        private val intents = HashMap<String, PendingIntent>()
        private val autoCancels = HashSet<String>()

        fun hasAccess(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, NotifListener::class.java)
            return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        /**
         * Opens what the notification points at, as tapping it in the shade would,
         * and clears it if the app asked for that. False when it has nothing to open.
         */
        fun open(key: String): Boolean {
            val intent = intents[key] ?: return false
            return try {
                if (Build.VERSION.SDK_INT >= 34) {
                    val options = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    intent.send(null, 0, null, null, null, null, options.toBundle())
                } else {
                    intent.send()
                }
                if (key in autoCancels) instance.get()?.let { runCatching { it.cancelNotification(key) } }
                true
            } catch (e: PendingIntent.CanceledException) {
                Log.w("NotifListener", "notification intent was cancelled", e)
                false
            }
        }
    }
}

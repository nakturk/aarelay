package com.example.aarelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.app.NotificationCompat.CarExtender

class NotificationListener : NotificationListenerService() {

    private val TAG = "AARelayListener"
    private val OUTLOOK_PACKAGE = "com.microsoft.office.outlook"
    private val CHANNEL_ID = "AA_Relay_Channel"
    private val CHANNEL_NAME = "Android Auto Relay Messages"
    private val KEY_TEXT_REPLY = "key_text_reply"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName ?: return
        
        // 1. Kendi bildirimlerimizi yoksay
        if (packageName == applicationContext.packageName) {
            return
        }

        // 2. Grup Özetlerini (Summary) yoksay - Çift bildirim gelmesini engeller
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            Log.d(TAG, "Grup özeti yoksayılıyor.")
            return
        }

        if (packageName == OUTLOOK_PACKAGE) {
            processOutlookNotification(sbn)
        }
    }

    private fun processOutlookNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // 3. Geçersiz veya boş içerikleri filtrele
        // "New message", "Fetching mail..." gibi anlamsız bildirimleri engeller
        if (sender.isBlank() || text.isBlank() || text.lowercase().contains("new message")) {
            Log.d(TAG, "Anlamsız içerik yoksayılıyor: $sender - $text")
            return
        }

        Log.d(TAG, "Geçerli Outlook bildirimi yakalandı: $sender - $text")

        relayAsMessagingStyle(sender, text)
    }

    private val activeConversations = mutableMapOf<String, MutableList<NotificationCompat.MessagingStyle.Message>>()

    private fun relayAsMessagingStyle(sender: String, messageText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val conversationId = sender.hashCode()

        // 0. Mesaj deposunu güncelle (Kalıcı SharedPreferences ile)
        MessageRepository.update(applicationContext, sender, messageText)

        // Konuşmayı güncelle veya oluştur
        val conversationHistory = activeConversations.getOrPut(sender) { mutableListOf() }
        val user = Person.Builder().setName(sender).build()
        val timestamp = System.currentTimeMillis()
        val newMessage = NotificationCompat.MessagingStyle.Message(messageText, timestamp, user)
        conversationHistory.add(newMessage)

        // 1. Cevaplama Eylemi
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).setLabel("Cevapla").build()
        val replyIntent = Intent(this, MessageReplyReceiver::class.java).apply {
            putExtra("conversation_id", conversationId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            applicationContext, conversationId, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "Cevapla", replyPendingIntent)
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()

        // 2. Okundu Olarak İşaretleme Eylemi
        val readIntent = Intent(this, MessageReadReceiver::class.java).apply {
            putExtra("conversation_id", conversationId)
        }
        val readPendingIntent = PendingIntent.getBroadcast(
            applicationContext, conversationId + 1, readIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val readAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view, "Okundu olarak işaretle", readPendingIntent)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        // 2.5. Metni Gör Eylemi (Android Auto ekranını açar)
        // Android Auto'da CarApp'i başlatmak için doğrudan Intent kullanmak bazen kısıtlanabilir.
        // Ancak bu yöntem en yaygın olanıdır.
        val viewIntent = Intent().apply {
            component = android.content.ComponentName(packageName, "com.example.aarelay.AARelayCarAppService")
            action = "androidx.car.app.CarAppService"
        }
        val viewPendingIntent = PendingIntent.getService(
            this, conversationId + 2, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val viewAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_info_details, "Metni Gör", viewPendingIntent)
            .build()

        // 3. MessagingStyle Bildirimi
        val messagingStyle = NotificationCompat.MessagingStyle(user)
            .setConversationTitle(sender)
            .setGroupConversation(false)
        conversationHistory.forEach { msg -> messagingStyle.addMessage(msg) }

        // Android Auto için en sade extender
        val carExtender = CarExtender()
            .setUnreadConversation(CarExtender.UnreadConversation.Builder(sender)
                .addMessage(messageText)
                .setReplyAction(replyPendingIntent, remoteInput)
                .build())

        val relayNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addPerson(user)
            .addAction(replyAction)
            .addAction(readAction)
            .addAction(viewAction) // Metni Gör butonunu ekle
            .setShortcutId(conversationId.toString())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .extend(carExtender)
            .build()

        try {
            notificationManager.notify(conversationId, relayNotification)
            Log.d(TAG, "Bildirim gönderildi: $sender")
        } catch (e: Exception) {
            Log.e(TAG, "Hata: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        // Kendi uygulamamızın bildirimi kaldırıldığında (okundu, silindi vs.)
        if (sbn != null && sbn.packageName == packageName) {
            val removedNotificationId = sbn.id
            val senderToRemove = activeConversations.keys.find { it.hashCode() == removedNotificationId }

            if (senderToRemove != null) {
                activeConversations.remove(senderToRemove)
                Log.d(TAG, "Konuşma geçmişi temizlendi: $senderToRemove")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Outlook bildirimlerini Android Auto'ya iletir."
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

package com.example.aarelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MessageReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getIntExtra("conversation_id", -1)
        Log.d("MessageReadReceiver", "Okundu olarak işaretlendi: $conversationId")
        
        // Bildirimi temizle
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(conversationId)
    }
}

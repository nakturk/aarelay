package com.example.aarelay

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MessageReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence("key_text_reply")?.toString()
            val conversationId = intent.getIntExtra("conversation_id", -1)
            Log.d("MessageReplyReceiver", "Cevap geldi: '$replyText' (ID: $conversationId)")
            
            // TODO: Gerçek bir cevap gönderme mantığı buraya eklenebilir.
        }
    }
}

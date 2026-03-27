package com.example.aarelay

import android.content.Intent
import androidx.car.app.*
import androidx.car.app.model.*
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.validation.HostValidator

// Android Auto uygulamasının ana servisi
class AARelayCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // Geliştirme aşamasında her türlü bağlantıya izin veriyoruz
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                return MessageDetailScreen(carContext)
            }

            override fun onNewIntent(intent: Intent) {
                // Uygulama açıkken yeni bir bildirim butonuna basılırsa ekranı tazele
                val topScreen = carContext.getCarService(ScreenManager::class.java).top
                if (topScreen is MessageDetailScreen) {
                    topScreen.invalidate()
                }
            }
        }
    }
}

// Mesaj detaylarını (scrollable) gösteren ekran
class MessageDetailScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val sender = MessageRepository.getLastSender(carContext).ifBlank { "AA Relay" }
        val message = MessageRepository.getLastMessage(carContext).ifBlank { "Henüz bir mesaj yakalanmadı." }

        // LongMessageTemplate, Android Auto'da uzun metinleri kaydırılabilir yapar
        return LongMessageTemplate.Builder(message)
            .setTitle("E-posta: $sender")
            .setHeaderAction(Action.BACK)
            .addAction(
                Action.Builder()
                    .setTitle("Kapat")
                    .setOnClickListener(ParkedOnlyOnClickListener.create { finish() })
                    .build()
            )
            .build()
    }
}

package com.example.aarelay

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.status_text)
        val btnListenerPermission = findViewById<Button>(R.id.btn_listener_permission)
        val btnNotifyPermission = findViewById<Button>(R.id.btn_notify_permission)

        // Notification Listener iznini kontrol et
        if (isNotificationServiceEnabled()) {
            statusText.text = "Bildirim Dinleyici: ETKİN"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            statusText.text = "Bildirim Dinleyici: DEVRE DIŞI"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }

        // Bildirim dinleyici ayarlarına yönlendir
        btnListenerPermission.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        // Android 13+ bildirim gönderme izni iste
        btnNotifyPermission.setOnClickListener {
            requestNotificationPermission()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat != null && flat.isNotEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }

    private fun updateUIState() {
        // Bildirim Dinleyici İzni Durumu
        val statusText = findViewById<TextView>(R.id.status_text)
        if (isNotificationServiceEnabled()) {
            statusText.text = "Bildirim Dinleyici: ETKİN"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            statusText.text = "Bildirim Dinleyici: DEVRE DIŞI"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }

        // Bildirim Gönderme İzni Durumu (A13+)
        val btnNotifyPermission = findViewById<Button>(R.id.btn_notify_permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                btnNotifyPermission.text = "Bildirim İzni Verildi (A13+)"
                btnNotifyPermission.isEnabled = false
            } else {
                btnNotifyPermission.text = "Bildirim Gönderme İzni Ver (A13+)"
                btnNotifyPermission.isEnabled = true
            }
        } else {
            btnNotifyPermission.text = "Bildirim İzni (Gerekmiyor)"
            btnNotifyPermission.isEnabled = false
        }
    }
}

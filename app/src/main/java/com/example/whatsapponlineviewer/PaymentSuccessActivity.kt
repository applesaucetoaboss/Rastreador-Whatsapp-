package com.example.whatsapponlineviewer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PaymentSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark premium immediately
        val prefs = getSharedPreferences("whatsapp_tracker", MODE_PRIVATE)
        prefs.edit().putBoolean("is_premium", true).apply()

        // Navigate to main
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
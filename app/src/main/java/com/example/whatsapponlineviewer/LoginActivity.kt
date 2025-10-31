package com.example.whatsapponlineviewer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsapponlineviewer.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("whatsapp_tracker", MODE_PRIVATE)
        val existing = prefs.getString("user_phone_number", null)
        if (existing != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val phone = binding.etWhatsappNumber.text.toString().trim()
            if (phone.length < 7 || phone.length > 15) {
                Toast.makeText(this, getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("user_phone_number", phone).apply()
            Toast.makeText(this, getString(R.string.registered_success), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
package com.example.whatsapponlineviewer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.whatsapponlineviewer.databinding.ActivityMainBinding
import com.example.whatsapponlineviewer.viewmodel.StatusViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            viewModel = ViewModelProvider(this)[StatusViewModel::class.java]

            setupObservers()
            setupListeners()
            updateRemainingChecksText()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing app: ${e.message}", e)
            Toast.makeText(this, "Error initializing app. Please try again.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupObservers() {
        viewModel.statusResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE
            binding.cardResult.visibility = View.VISIBLE
            
            binding.tvStatus.text = result.status
            binding.tvLastSeen.text = result.lastSeen
        }

        viewModel.remainingChecks.observe(this) { remaining ->
            updateRemainingChecksText()
        }

        viewModel.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                binding.tvRemainingChecks.text = "Premium Activado - Rastreo Ilimitado"
                binding.cardPromo.visibility = View.GONE
            } else {
                binding.tvRemainingChecks.text = "Premium Requerido - Compra para usar la app"
                binding.cardPromo.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        // Remove hidden creator unlock backdoor for production

        binding.btnCheckStatus.setOnClickListener {
            // PREMIUM-ONLY: Block all usage until premium is activated
            if (viewModel.isPremium.value != true) {
                showPremiumDialog()
                return@setOnClickListener
            }

            val phoneNumber = binding.etPhoneNumber.text.toString()
            
            if (phoneNumber.length < 7 || phoneNumber.length > 15) {
                Toast.makeText(this, getString(R.string.invalid_phone), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Block tracking own number
            if (viewModel.isOwnNumber(phoneNumber)) {
                Toast.makeText(this, getString(R.string.own_number_blocked), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkStatus(phoneNumber)
        }
    }

    private fun checkStatus(phoneNumber: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardResult.visibility = View.GONE
        binding.tvStatus.text = getString(R.string.searching)

        // Simulate network delay
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000) // 3 seconds delay
            viewModel.checkStatus(phoneNumber)
        }
    }

    private fun updateRemainingChecksText() {
        val remaining = viewModel.remainingChecks.value ?: 0
        binding.tvRemainingChecks.text = getString(R.string.remaining_checks, remaining)
    }

    private fun showPremiumDialog() {
        AlertDialog.Builder(this)
            .setTitle("Premium Requerido")
            .setMessage("Esta app requiere Premium para funcionar. Compra Premium para rastrear números de WhatsApp.")
            .setPositiveButton("Comprar Premium") { _, _ ->
                startActivity(Intent(this, PaymentActivity::class.java))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

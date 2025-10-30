package com.example.whatsapponlineviewer

import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.whatsapponlineviewer.databinding.ActivityPaymentBinding
import com.example.whatsapponlineviewer.payment.CardPaymentFragment
import com.example.whatsapponlineviewer.payment.SpeiPaymentFragment
import com.example.whatsapponlineviewer.payment.PaymentViewModel
import com.example.whatsapponlineviewer.viewmodel.StatusViewModel
import java.util.*

class PaymentActivity : AppCompatActivity() {

    private var _binding: ActivityPaymentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StatusViewModel
    private lateinit var paymentViewModel: PaymentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityPaymentBinding.inflate(layoutInflater)
            setContentView(binding.root)

            viewModel = ViewModelProvider(this)[StatusViewModel::class.java]
            paymentViewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

            setupPaymentMethodSelection()
            
            // Show card payment by default
            showCardPaymentFragment()
        } catch (e: Exception) {
            Log.e("PaymentActivity", "Error initializing: ${e.message}", e)
            Toast.makeText(this, "Error initializing payment screen", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupPaymentMethodSelection() {
        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbCard -> showCardPaymentFragment()
                R.id.rbSpei -> showSpeiPaymentFragment()
            }
        }
    }

    private fun showCardPaymentFragment() {
        val fragment = CardPaymentFragment()
        replaceFragment(fragment)
    }

    private fun showSpeiPaymentFragment() {
        val fragment = SpeiPaymentFragment()
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.paymentContainer, fragment)
            .commit()
    }

    fun onPaymentSuccess() {
        viewModel.setPremium()
        Toast.makeText(this, getString(R.string.payment_success), Toast.LENGTH_LONG).show()
        finish()
    }
}

// SpeiPaymentFragment is now in the payment package
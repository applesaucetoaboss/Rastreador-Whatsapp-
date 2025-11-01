package com.example.whatsapponlineviewer.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.whatsapponlineviewer.R
import com.example.whatsapponlineviewer.PaymentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet

class CardPaymentFragment : Fragment() {
    private lateinit var viewModel: PaymentViewModel
    private var paymentSheet: PaymentSheet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_card_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[PaymentViewModel::class.java]

        context?.let {
            PaymentConfiguration.init(it, com.example.whatsapponlineviewer.BuildConfig.STRIPE_PUBLISHABLE_KEY)
            paymentSheet = PaymentSheet(this) { result ->
                when (result) {
                    is PaymentSheet.Result.Completed -> {
                        viewModel.handlePaymentSuccess()
                    }
                    is PaymentSheet.Result.Failed -> {
                        viewModel.handlePaymentError(result.error)
                    }
                    is PaymentSheet.Result.Canceled -> {
                        Toast.makeText(context, "Payment canceled", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.btnPay).setOnClickListener {
            // Create subscription for MXN 180 monthly; PaymentSheet will confirm the first invoice
            viewModel.createSubscription()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.paymentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PaymentState.Loading -> {
                    // Show loading indicator if needed
                }
                is PaymentState.PaymentIntentCreated -> {
                    // Present PaymentSheet with the client secret
                    presentPaymentSheet(state.clientSecret)
                }
                is PaymentState.Success -> {
                    (activity as? PaymentActivity)?.onPaymentSuccess()
                }
                is PaymentState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    private fun presentPaymentSheet(clientSecret: String) {
        // Simple PaymentSheet usage without customer; Stripe manages PCI
        paymentSheet?.presentWithPaymentIntent(clientSecret, PaymentSheet.Configuration(
            merchantDisplayName = "Rastreador WhatsApp"
        ))
    }
}

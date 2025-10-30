package com.example.whatsapponlineviewer.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.whatsapponlineviewer.R
import com.example.whatsapponlineviewer.PaymentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.CardInputWidget
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult

class CardPaymentFragment : Fragment() {
    
    private lateinit var viewModel: PaymentViewModel
    private lateinit var stripe: Stripe
    private lateinit var cardInputWidget: CardInputWidget
    private lateinit var paymentLauncher: PaymentLauncher
    
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
        
        // Initialize Stripe
        context?.let {
            PaymentConfiguration.init(it, com.example.whatsapponlineviewer.BuildConfig.STRIPE_PUBLISHABLE_KEY)
            stripe = Stripe(it, PaymentConfiguration.getInstance(it).publishableKey)
            paymentLauncher = PaymentLauncher.Companion.create(
                this,
                PaymentConfiguration.getInstance(it).publishableKey,
                stripeAccountId = null
            ) { paymentResult ->
                when (paymentResult) {
                    is PaymentResult.Completed -> {
                        viewModel.handlePaymentSuccess()
                    }
                    is PaymentResult.Failed -> {
                        viewModel.handlePaymentError(paymentResult.throwable as Exception)
                    }
                    is PaymentResult.Canceled -> {
                        Toast.makeText(context, "Payment canceled", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        cardInputWidget = view.findViewById(R.id.cardInputWidget)
        
        view.findViewById<Button>(R.id.btnPay).setOnClickListener {
            processPayment()
        }
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.paymentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PaymentState.Loading -> {
                    // Show loading indicator
                }
                is PaymentState.PaymentIntentCreated -> {
                    // Payment intent created, proceed with card details
                    createPaymentMethod()
                }
                is PaymentState.ConfirmPayment -> {
                    // Confirm the payment with Stripe
                    this.confirmPayment(state.params)
                }
                is PaymentState.Success -> {
                    // Payment successful
                    (activity as? PaymentActivity)?.onPaymentSuccess()
                }
                is PaymentState.Error -> {
                    // Show error
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun processPayment() {
        // Validate card input
        val cardParams = cardInputWidget.paymentMethodCreateParams
        if (cardParams == null) {
            Toast.makeText(context, "Please enter valid card details", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create payment intent - amount in cents (e.g., $9.99 = 999)
        viewModel.createPaymentIntent(999)
    }
    
    private fun createPaymentMethod() {
        val params = cardInputWidget.paymentMethodCreateParams ?: run {
            Toast.makeText(context, "Invalid card details", Toast.LENGTH_SHORT).show()
            return
        }
        
        context?.let { ctx ->
            stripe.createPaymentMethod(params, callback = object : com.stripe.android.ApiResultCallback<PaymentMethod> {
                override fun onSuccess(result: PaymentMethod) {
                    // Payment method created, confirm payment
                    viewModel.confirmPayment(result.id ?: "")
                }
                
                override fun onError(e: Exception) {
                    viewModel.handlePaymentError(e)
                }
            })
        }
    }
    
    private fun confirmPayment(params: com.stripe.android.model.ConfirmPaymentIntentParams) {
        context?.let { ctx ->
            paymentLauncher.confirmPayment(params)
        }
    }
    
    // The PaymentLauncher handles the result, so we no longer need onActivityResult.
    // override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
    //     super.onActivityResult(requestCode, resultCode, data)
    // }
}
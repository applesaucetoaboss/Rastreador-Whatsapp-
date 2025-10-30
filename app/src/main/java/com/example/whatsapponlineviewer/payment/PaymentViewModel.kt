package com.example.whatsapponlineviewer.payment

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PaymentViewModel(private val context: Context) : ViewModel() {
    private val repository = PaymentRepository(context)
    
    private val _paymentState = MutableLiveData<PaymentState>()
    val paymentState: LiveData<PaymentState> = _paymentState
    
    private var clientSecret: String? = null
    
    fun createPaymentIntent(amount: Int) {
        _paymentState.value = PaymentState.Loading
        
        viewModelScope.launch {
            try {
                val result = repository.createPaymentIntent(amount)
                
                result.fold(
                    onSuccess = { response ->
                        clientSecret = response.clientSecret
                        _paymentState.value = PaymentState.PaymentIntentCreated(response.clientSecret)
                    },
                    onFailure = { error ->
                        _paymentState.value = PaymentState.Error("Failed to create payment: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Error: ${e.message}")
            }
        }
    }
    
    fun confirmPayment(paymentMethodId: String) {
        val secret = clientSecret ?: run {
            _paymentState.value = PaymentState.Error("No client secret available")
            return
        }
        
        val params = repository.createConfirmPaymentIntentParams(paymentMethodId, secret)
        _paymentState.value = PaymentState.ConfirmPayment(params)
    }
    
    fun handlePaymentSuccess() {
        _paymentState.value = PaymentState.Success
    }
    
    fun handlePaymentError(error: Exception) {
        _paymentState.value = PaymentState.Error("Payment failed: ${error.message}")
    }
}

sealed class PaymentState {
    object Loading : PaymentState()
    data class PaymentIntentCreated(val clientSecret: String) : PaymentState()
    data class ConfirmPayment(val params: com.stripe.android.model.ConfirmPaymentIntentParams) : PaymentState()
    object Success : PaymentState()
    data class Error(val message: String) : PaymentState()
}
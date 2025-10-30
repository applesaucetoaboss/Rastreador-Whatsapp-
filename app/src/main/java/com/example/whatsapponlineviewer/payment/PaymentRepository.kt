package com.example.whatsapponlineviewer.payment

import android.content.Context
import com.example.whatsapponlineviewer.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PaymentRepository(private val context: Context) {
    private val stripeApiService: StripeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://your-backend-server.com/") // Replace with your actual backend URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StripeApiService::class.java)
    }

    init {
        // Initialize Stripe with your publishable key
        PaymentConfiguration.init(context, BuildConfig.STRIPE_PUBLISHABLE_KEY)
    }

    suspend fun createPaymentIntent(amount: Int): Result<PaymentIntentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = PaymentIntentRequest(amount)
                val response = stripeApiService.createPaymentIntent(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to create payment intent: ${response.errorBody()?.string()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun createConfirmPaymentIntentParams(
        paymentMethodId: String,
        clientSecret: String
    ): ConfirmPaymentIntentParams {
        return ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId,
            clientSecret
        )
    }
}
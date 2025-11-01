package com.example.whatsapponlineviewer.payment

import android.content.Context
import com.example.whatsapponlineviewer.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PaymentRepository(private val context: Context) {
    private val stripeApiService: StripeApiService by lazy {
        Retrofit.Builder()
            // Use your backend to create PaymentIntent and return client secret
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
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
                val prefs = context.getSharedPreferences("whatsapp_tracker", Context.MODE_PRIVATE)
                val phone = prefs.getString("user_phone_number", null)
                val request = PaymentIntentRequest(amount = amount, phone = phone)
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

    suspend fun createSubscription(): Result<SubscriptionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("whatsapp_tracker", Context.MODE_PRIVATE)
                val phone = prefs.getString("user_phone_number", null)
                val request = SubscriptionRequest(phone = phone)
                val response = stripeApiService.createSubscription(request)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to create subscription: ${response.errorBody()?.string()}"))
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

    suspend fun getPremiumStatus(phone: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<PremiumStatusResponse> = stripeApiService.getPremiumStatus(phone)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.premium)
                } else {
                    Result.failure(Exception("Failed to fetch premium status: ${response.errorBody()?.string()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

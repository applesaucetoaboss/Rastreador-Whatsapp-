package com.example.whatsapponlineviewer.payment

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface StripeApiService {
    @POST("create-payment-intent")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): Response<PaymentIntentResponse>

    @POST("create-subscription")
    suspend fun createSubscription(@Body request: SubscriptionRequest): Response<SubscriptionResponse>

    // Serverless backend endpoint to query premium status by registered phone number
    @GET("premium-status")
    suspend fun getPremiumStatus(@Query("phone") phone: String): Response<PremiumStatusResponse>
}

data class PaymentIntentRequest(
    val amount: Int,
    val currency: String = "usd",
    val paymentMethodType: String = "card",
    val phone: String? = null
)

data class PaymentIntentResponse(
    val clientSecret: String,
    val id: String
)

data class SubscriptionRequest(
    val phone: String?
)

data class SubscriptionResponse(
    val clientSecret: String,
    val subscriptionId: String
)

data class PremiumStatusResponse(
    val premium: Boolean
)

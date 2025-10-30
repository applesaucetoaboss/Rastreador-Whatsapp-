package com.example.whatsapponlineviewer.payment

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface StripeApiService {
    @POST("create-payment-intent")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): Response<PaymentIntentResponse>
}

data class PaymentIntentRequest(
    val amount: Int,
    val currency: String = "usd",
    val paymentMethodType: String = "card"
)

data class PaymentIntentResponse(
    val clientSecret: String,
    val id: String
)
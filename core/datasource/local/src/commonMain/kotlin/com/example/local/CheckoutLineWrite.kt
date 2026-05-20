package com.example.local

data class CheckoutLineWrite(
    val lineId: String,
    val productId: String,
    val productTitle: String,
    val quantity: Int,
    val unitPriceCents: Int,
)

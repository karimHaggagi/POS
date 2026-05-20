package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class PosCartLineUi(
    val lineId: String,
    val productId: String,
    val title: String,
    val quantity: Int,
    val unitPrice: Double,
)

@Serializable
data class PosCatalogItem(
    val id: String,
    val title: String,
    val price: Double,
    val sku: String,
)

data class PosTotals(
    val subtotal: Double,
    val tax: Double,
    val discount: Double,
    val total: Double,
)

data class PosPastOrder(
    val id: String,
    val createdAtEpochMs: Long,
    val total: Double,
    val paymentRef: String,
    val synced: Boolean,
)

data class PosCheckoutResult(
    val orderId: String,
    val receiptText: String,
    val total: Double,
)

data class PosSyncSummary(
    val processed: Int,
    val failed: Int,
    val detail: String,
)

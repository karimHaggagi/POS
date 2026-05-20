package com.example.domain.repository

import com.example.model.PosCartLineUi
import com.example.model.PosCatalogItem
import com.example.model.PosCheckoutResult
import com.example.model.PosPastOrder
import com.example.model.PosSyncSummary
import com.example.model.PosTotals
import kotlinx.coroutines.flow.Flow

interface PosRepository {
    fun observeCart(): Flow<List<PosCartLineUi>>
    fun observeCatalog(): Flow<List<PosCatalogItem>>
    fun observePastOrders(): Flow<List<PosPastOrder>>
    fun observeTotals(discountPercent: Double): Flow<PosTotals>

    suspend fun addProductToCart(productId: String, quantity: Int = 1)
    suspend fun removeCartLine(lineId: String)
    suspend fun incrementQuantity(lineId: String)
    suspend fun decrementQuantity(lineId: String)
    suspend fun applyBarcode(sku: String): Boolean

    suspend fun completeSale(discountPercent: Double = 0.0): Result<PosCheckoutResult>
    suspend fun processSyncOutbox(): PosSyncSummary
}

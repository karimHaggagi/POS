package com.example.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.local.db.Cart_line
import com.example.local.db.Catalog_product
import com.example.local.db.Completed_order
import com.example.local.db.QuestPosDatabase
import com.example.local.db.Sync_outbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PosLocalDataSource(private val database: QuestPosDatabase) {

    init {
        database.transaction {
            if (database.posQueries.countCatalog().executeAsOne() == 0L) {
                seedCatalog()
            }
        }
    }

    private fun seedCatalog() {
        val items = listOf(
            "cf001" to ("Chicken Shawarma" to 899),
            "cf002" to ("Falafel Wrap" to 749),
            "cf003" to ("Hummus Bowl" to 599),
            "cf004" to ("Fresh Juice" to 450),
            "cf005" to ("Baklava" to 350),
        )
        items.forEachIndexed { index, (sku, pair) ->
            val (title, cents) = pair
            val id = "p_${index + 1}"
            database.posQueries.insertCatalogProduct(id, title, cents.toLong(), sku)
        }
    }

    fun observeCartLines(): Flow<List<Cart_line>> =
        database.posQueries.selectAllCartLines()
            .asFlow()
            .mapToList(Dispatchers.Default)

    fun observeCatalog(): Flow<List<Catalog_product>> =
        database.posQueries.selectAllCatalog()
            .asFlow()
            .mapToList(Dispatchers.Default)

    fun observeRecentOrders(): Flow<List<Completed_order>> =
        database.posQueries.selectRecentOrders()
            .asFlow()
            .mapToList(Dispatchers.Default)

    suspend fun addCartLine(
        lineId: String,
        productId: String,
        productTitle: String,
        quantity: Int,
        unitPriceCents: Int,
    ) = withContext(Dispatchers.Default) {
        database.posQueries.insertCartLine(
            lineId,
            productId,
            productTitle,
            quantity.toLong(),
            unitPriceCents.toLong(),
        )
    }

    suspend fun updateLineQuantity(lineId: String, quantity: Int) =
        withContext(Dispatchers.Default) {
            database.posQueries.updateCartLineQuantity(quantity.toLong(), lineId)
        }

    suspend fun removeLine(lineId: String) = withContext(Dispatchers.Default) {
        database.posQueries.deleteCartLine(lineId)
    }

    suspend fun clearCart() = withContext(Dispatchers.Default) {
        database.posQueries.clearCart()
    }

    suspend fun findProductBySku(sku: String): Catalog_product? = withContext(Dispatchers.Default) {
        database.posQueries.selectCatalogBySku(sku).executeAsOneOrNull()
    }

    suspend fun findProductById(id: String): Catalog_product? = withContext(Dispatchers.Default) {
        database.posQueries.selectCatalogById(id).executeAsOneOrNull()
    }

    suspend fun insertCompletedOrder(
        orderId: String,
        createdAt: Long,
        subtotalCents: Int,
        taxCents: Int,
        discountCents: Int,
        totalCents: Int,
        paymentRef: String,
        receiptText: String,
        synced: Long,
    ) = withContext(Dispatchers.Default) {
        database.posQueries.insertCompletedOrder(
            orderId,
            createdAt,
            subtotalCents.toLong(),
            taxCents.toLong(),
            discountCents.toLong(),
            totalCents.toLong(),
            paymentRef,
            receiptText,
            synced,
        )
    }

    suspend fun insertCompletedLine(
        lineId: String,
        orderId: String,
        productId: String,
        productTitle: String,
        quantity: Int,
        unitPriceCents: Int,
    ) = withContext(Dispatchers.Default) {
        database.posQueries.insertCompletedOrderLine(
            lineId,
            orderId,
            productId,
            productTitle,
            quantity.toLong(),
            unitPriceCents.toLong(),
        )
    }

    suspend fun insertOutbox(
        id: String,
        entityId: String,
        entityType: String,
        jsonPayload: String,
        createdAt: Long,
        status: String,
        retryCount: Long,
        lastError: String?,
    ) = withContext(Dispatchers.Default) {
        database.posQueries.insertOutbox(
            id,
            entityId,
            entityType,
            jsonPayload,
            createdAt,
            status,
            retryCount,
            lastError,
        )
    }

    suspend fun pendingOutbox(): List<Sync_outbox> = withContext(Dispatchers.Default) {
        database.posQueries.selectPendingOutbox().executeAsList()
    }

    suspend fun updateOutboxStatus(id: String, status: String, retryCount: Int, lastError: String?) =
        withContext(Dispatchers.Default) {
            database.posQueries.updateOutboxRow(status, retryCount.toLong(), lastError, id)
        }

    suspend fun markOrderSynced(orderId: String) = withContext(Dispatchers.Default) {
        database.posQueries.markOrderSynced(orderId)
    }

    suspend fun cartLinesSnapshot(): List<Cart_line> = withContext(Dispatchers.Default) {
        database.posQueries.selectAllCartLines().executeAsList()
    }

    suspend fun writeCheckout(
        orderId: String,
        createdAt: Long,
        subtotalCents: Int,
        taxCents: Int,
        discountCents: Int,
        totalCents: Int,
        paymentRef: String,
        receiptText: String,
        lines: List<CheckoutLineWrite>,
        outboxId: String,
        outboxJson: String,
        outboxCreatedAt: Long,
    ) = withContext(Dispatchers.Default) {
        database.transaction {
            database.posQueries.insertCompletedOrder(
                orderId,
                createdAt,
                subtotalCents.toLong(),
                taxCents.toLong(),
                discountCents.toLong(),
                totalCents.toLong(),
                paymentRef,
                receiptText,
                0L,
            )
            lines.forEach { line ->
                database.posQueries.insertCompletedOrderLine(
                    line.lineId,
                    orderId,
                    line.productId,
                    line.productTitle,
                    line.quantity.toLong(),
                    line.unitPriceCents.toLong(),
                )
            }
            database.posQueries.clearCart()
            database.posQueries.insertOutbox(
                outboxId,
                orderId,
                "ORDER",
                outboxJson,
                outboxCreatedAt,
                "PENDING",
                0L,
                null,
            )
        }
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun lineId(): String = "l_${Uuid.random().toHexString()}"

        @OptIn(ExperimentalUuidApi::class)
        fun orderId(): String = "o_${Uuid.random().toHexString()}"

        @OptIn(ExperimentalUuidApi::class)
        fun outboxId(): String = "q_${Uuid.random().toHexString()}"
    }
}

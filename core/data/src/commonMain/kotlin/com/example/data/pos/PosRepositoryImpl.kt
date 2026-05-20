package com.example.data.pos

import com.example.domain.pos.PaymentTerminal
import com.example.domain.pos.PosAnalytics
import com.example.domain.pos.ReceiptPrinter
import com.example.domain.repository.PosRepository
import com.example.local.CheckoutLineWrite
import com.example.local.PosLocalDataSource
import com.example.local.db.Cart_line
import com.example.local.db.Catalog_product
import com.example.local.db.Completed_order
import com.example.model.PosCartLineUi
import com.example.model.PosCatalogItem
import com.example.model.PosCheckoutResult
import com.example.model.PosPastOrder
import com.example.model.PosSyncSummary
import com.example.model.PosTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.collections.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PosRepositoryImpl(
    private val local: PosLocalDataSource,
    private val paymentTerminal: PaymentTerminal,
    private val receiptPrinter: ReceiptPrinter,
    private val analytics: PosAnalytics,
) : PosRepository {

    private val json = Json { prettyPrint = false }

    override fun observeCart(): Flow<List<PosCartLineUi>> =
        local.observeCartLines().map { rows -> rows.map { it.toUi() } }

    override fun observeCatalog(): Flow<List<PosCatalogItem>> =
        local.observeCatalog().map { rows -> rows.map { it.toItem() } }

    override fun observePastOrders(): Flow<List<PosPastOrder>> =
        local.observeRecentOrders().map { rows -> rows.map { it.toPast() } }

    override fun observeTotals(discountPercent: Double): Flow<PosTotals> =
        observeCart().map { lines -> totalsFor(lines, discountPercent) }

    override suspend fun addProductToCart(productId: String, quantity: Int) {
        val product = local.findProductById(productId) ?: return
        val existing = local.cartLinesSnapshot().firstOrNull { it.product_id == productId }
        if (existing != null) {
            val nextQty = existing.quantity.toInt() + quantity
            local.updateLineQuantity(existing.id, nextQty)
        } else {
            local.addCartLine(
                lineId = PosLocalDataSource.lineId(),
                productId = product.id,
                productTitle = product.title,
                quantity = quantity,
                unitPriceCents = product.price_cents.toInt(),
            )
        }
    }

    override suspend fun removeCartLine(lineId: String) {
        local.removeLine(lineId)
    }

    override suspend fun incrementQuantity(lineId: String) {
        val line = local.cartLinesSnapshot().firstOrNull { it.id == lineId } ?: return
        local.updateLineQuantity(lineId, line.quantity.toInt() + 1)
    }

    override suspend fun decrementQuantity(lineId: String) {
        val line = local.cartLinesSnapshot().firstOrNull { it.id == lineId } ?: return
        val qty = line.quantity.toInt()
        if (qty <= 1) {
            local.removeLine(lineId)
        } else {
            local.updateLineQuantity(lineId, qty - 1)
        }
    }

    override suspend fun applyBarcode(sku: String): Boolean {
        val product = local.findProductBySku(sku) ?: return false
        addProductToCart(product.id, 1)
        analytics.log("barcode_applied", mapOf("sku" to sku, "productId" to product.id))
        return true
    }

    override suspend fun completeSale(discountPercent: Double): Result<PosCheckoutResult> {
        val lines = local.cartLinesSnapshot()
        if (lines.isEmpty()) {
            return Result.failure(IllegalStateException("Cart is empty"))
        }

        val subtotalCents = lines.sumOf { it.quantity.toInt() * it.unit_price_cents.toInt() }
        val discountCents = ((subtotalCents * discountPercent.coerceIn(0.0, 100.0)) / 100.0).toInt()
        val taxableCents = (subtotalCents - discountCents).coerceAtLeast(0)
        val taxCents = (taxableCents * TAX_RATE).toInt()
        val totalCents = taxableCents + taxCents

        val payment = paymentTerminal.charge(totalCents, "USD")
        val paymentRef = payment.getOrElse { return Result.failure(it) }

        val receipt = buildReceipt(lines, subtotalCents, taxCents, discountCents, totalCents, paymentRef)
        receiptPrinter.print(receipt)

        val orderId = PosLocalDataSource.orderId()
        val now = currentEpochMillis()
        val outboxId = PosLocalDataSource.outboxId()
        val payload = OrderSyncPayload(orderId = orderId, totalCents = totalCents, createdAt = now)

        local.writeCheckout(
            orderId = orderId,
            createdAt = now,
            subtotalCents = subtotalCents,
            taxCents = taxCents,
            discountCents = discountCents,
            totalCents = totalCents,
            paymentRef = paymentRef,
            receiptText = receipt,
            lines = lines.map {
                CheckoutLineWrite(
                    lineId = PosLocalDataSource.lineId(),
                    productId = it.product_id,
                    productTitle = it.product_title,
                    quantity = it.quantity.toInt(),
                    unitPriceCents = it.unit_price_cents.toInt(),
                )
            },
            outboxId = outboxId,
            outboxJson = json.encodeToString(payload),
            outboxCreatedAt = now,
        )

        analytics.log(
            "sale_completed",
            mapOf(
                "orderId" to orderId,
                "totalCents" to totalCents.toString(),
            ),
        )

        return Result.success(
            PosCheckoutResult(
                orderId = orderId,
                receiptText = receipt,
                total = totalCents / 100.0,
            ),
        )
    }

    override suspend fun processSyncOutbox(): PosSyncSummary {
        val pending = local.pendingOutbox()
        if (pending.isEmpty()) {
            return PosSyncSummary(0, 0, "Nothing pending")
        }
        var processed = 0
        for (row in pending) {
            delay(120)
            val attempt = row.retry_count.toInt()
            local.updateOutboxStatus(row.id, "SYNCED", attempt, null)
            local.markOrderSynced(row.entity_id)
            processed++
            analytics.log("sync_uploaded", mapOf("entityId" to row.entity_id))
        }
        return PosSyncSummary(
            processed = processed,
            failed = 0,
            detail = "Uploaded $processed pending rows to the mock cloud endpoint.",
        )
    }

    private fun totalsFor(lines: List<PosCartLineUi>, discountPercent: Double): PosTotals {
        val subtotal = lines.sumOf { it.unitPrice * it.quantity }
        val discount = subtotal * (discountPercent.coerceIn(0.0, 100.0) / 100.0)
        val taxable = (subtotal - discount).coerceAtLeast(0.0)
        val tax = taxable * TAX_RATE
        val total = taxable + tax
        return PosTotals(subtotal = subtotal, tax = tax, discount = discount, total = total)
    }

    private fun buildReceipt(
        lines: List<Cart_line>,
        subtotalCents: Int,
        taxCents: Int,
        discountCents: Int,
        totalCents: Int,
        paymentRef: String,
    ): String = buildString {
        appendLine("--- Sahm Food POS (offline) ---")
        lines.forEach { line ->
            appendLine("${line.product_title} x${line.quantity} @ ${formatMoney(line.unit_price_cents.toInt())}")
        }
        appendLine("Subtotal : ${formatMoney(subtotalCents)}")
        appendLine("Discount : ${formatMoney(discountCents)}")
        appendLine("Tax      : ${formatMoney(taxCents)}")
        appendLine("Total    : ${formatMoney(totalCents)}")
        appendLine("Payment  : $paymentRef")
        appendLine("Thank you!")
    }

    private fun formatMoney(cents: Int): String = "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

    private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

private const val TAX_RATE = 0.0825

@Serializable
private data class OrderSyncPayload(
    val orderId: String,
    val totalCents: Int,
    val createdAt: Long,
)

private fun Cart_line.toUi(): PosCartLineUi =
    PosCartLineUi(
        lineId = id,
        productId = product_id,
        title = product_title,
        quantity = quantity.toInt(),
        unitPrice = unit_price_cents / 100.0,
    )

private fun Catalog_product.toItem(): PosCatalogItem =
    PosCatalogItem(
        id = id,
        title = title,
        price = price_cents / 100.0,
        sku = sku,
    )

private fun Completed_order.toPast(): PosPastOrder =
    PosPastOrder(
        id = id,
        createdAtEpochMs = created_at,
        total = total_cents / 100.0,
        paymentRef = payment_ref,
        synced = synced == 1L,
    )

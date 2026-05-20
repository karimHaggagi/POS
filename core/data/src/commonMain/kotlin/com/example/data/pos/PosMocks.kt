package com.example.data.pos

import com.example.domain.pos.PaymentTerminal
import com.example.domain.pos.PosAnalytics
import com.example.domain.pos.ReceiptPrinter

class ConsolePosAnalytics : PosAnalytics {
    override fun log(event: String, attributes: Map<String, String>) {
        println("[POS][analytics] $event ${attributes.entries.joinToString { "${it.key}=${it.value}" }}")
    }
}

class MockReceiptPrinter(
    private val analytics: PosAnalytics,
) : ReceiptPrinter {
    override suspend fun print(receiptText: String): Boolean {
        analytics.log(
            "receipt_print",
            mapOf(
                "length" to receiptText.length.toString(),
                "preview" to receiptText.take(48),
            ),
        )
        return true
    }
}

class MockPaymentTerminal(
    private val analytics: PosAnalytics,
) : PaymentTerminal {
    override suspend fun charge(amountCents: Int, currencyCode: String): Result<String> {
        analytics.log(
            "payment_charge",
            mapOf("amountCents" to amountCents.toString(), "currency" to currencyCode),
        )
        return Result.success("MOCK-${currencyCode}-$amountCents")
    }
}

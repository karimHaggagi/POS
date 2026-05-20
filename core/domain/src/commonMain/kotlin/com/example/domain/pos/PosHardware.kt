package com.example.domain.pos

interface ReceiptPrinter {
    suspend fun print(receiptText: String): Boolean
}

interface PaymentTerminal {
    suspend fun charge(amountCents: Int, currencyCode: String = "USD"): Result<String>
}

interface BarcodeScanner {
    suspend fun readOnce(): String?
}

package com.example.di

import app.cash.sqldelight.db.SqlDriver
import com.example.data.pos.ConsolePosAnalytics
import com.example.data.pos.MockPaymentTerminal
import com.example.data.pos.MockReceiptPrinter
import com.example.data.pos.PosRepositoryImpl
import com.example.domain.pos.PaymentTerminal
import com.example.domain.pos.PosAnalytics
import com.example.domain.pos.ReceiptPrinter
import com.example.domain.repository.PosRepository
import com.example.local.PosLocalDataSource
import com.example.local.db.QuestPosDatabase
import com.example.local.driver.DatabaseDriverFactory
import org.koin.dsl.module

fun posModule(factory: DatabaseDriverFactory) = module {
    single<SqlDriver> { factory.createDriver() }
    single { QuestPosDatabase(get()) }
    single { PosLocalDataSource(get()) }
    single<PosAnalytics> { ConsolePosAnalytics() }
    single<ReceiptPrinter> { MockReceiptPrinter(get()) }
    single<PaymentTerminal> { MockPaymentTerminal(get()) }
    single<PosRepository> { PosRepositoryImpl(get(), get(), get(), get()) }
}

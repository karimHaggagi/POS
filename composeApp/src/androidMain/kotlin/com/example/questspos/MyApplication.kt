package com.example.questspos

import android.app.Application
import com.example.local.driver.AndroidDatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MyApplication)
                modules(buildRootModule(AndroidDatabaseDriverFactory(this@MyApplication)))
            }
        }
    }
}
package com.example.questspos

import androidx.compose.ui.window.ComposeUIViewController
import com.example.local.driver.NativeDatabaseDriverFactory
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(configure = {buildRootModule(NativeDatabaseDriverFactory())}) {
    App()
}
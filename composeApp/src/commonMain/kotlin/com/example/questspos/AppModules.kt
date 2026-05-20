package com.example.questspos

import com.example.di.posModule
import com.example.local.driver.DatabaseDriverFactory
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun buildRootModule(factory: DatabaseDriverFactory) = listOf(
    posModule(factory),
    module { viewModelOf(::PosViewModel) },
)

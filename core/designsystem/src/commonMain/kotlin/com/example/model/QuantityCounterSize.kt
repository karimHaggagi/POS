package com.example.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
const val MAX_QUANTITY = 10
const val MIN_QUANTITY = 1
enum class QuantityCounterSize(
    val spacing: Dp,
    val padding: Dp
) {
    Small(
        spacing = 4.dp,
        padding = 8.dp
    ),
    Large(
        spacing = 8.dp,
        padding = 12.dp
    )
}
package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.designsystem.CategoryBlue
import com.example.designsystem.CategoryGreen
import com.example.designsystem.CategoryPurple
import com.example.designsystem.CategoryRed
import com.example.designsystem.CategoryYellow

enum class ProductCategory(
    val title: String,
    val color: Color
) {
    Protein(
        title = "Protein",
        color = CategoryYellow
    ),
    Creatine(
        title = "Creatine",
        color = CategoryBlue
    ),
    PreWorkout(
        title = "Pre-Workout",
        color = CategoryGreen
    ),
    Gainers(
        title = "Gainers",
        color = CategoryPurple
    ),
    Accessories(
        title = "Accessories",
        color = CategoryRed
    )
}
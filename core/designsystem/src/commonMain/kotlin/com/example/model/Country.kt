package com.example.model

import com.example.designsystem.Resources
import org.jetbrains.compose.resources.DrawableResource

enum class Country(
    val dialCode: Int,
    val code: String,
    val flag: DrawableResource
) {
    Serbia(
        dialCode = 381,
        code = "RS",
        flag = Resources.Flag.Serbia
    ),
    India(
        dialCode = 91,
        code = "IN",
        flag = Resources.Flag.India
    ),
    Usa(
        dialCode = 1,
        code = "US",
        flag = Resources.Flag.Usa
    );

    companion object{
        fun getCountryByCode(dialCode: Int): Country {
            return entries.find { it.dialCode == dialCode }?: Country.Serbia
        }
    }
}
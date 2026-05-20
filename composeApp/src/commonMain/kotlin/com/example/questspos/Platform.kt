package com.example.questspos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
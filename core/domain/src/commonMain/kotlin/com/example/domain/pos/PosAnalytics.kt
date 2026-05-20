package com.example.domain.pos

interface PosAnalytics {
    fun log(event: String, attributes: Map<String, String> = emptyMap())
}

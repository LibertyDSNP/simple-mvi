package com.unfinished.simplemvi.example.model

import java.util.*

data class PostComment(
    val text: String,
    val depth: Int,
    val author: String,
    val timestamp: Date
)
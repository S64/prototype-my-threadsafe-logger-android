package com.kiganix.android.example.mytslp.lib

import kotlinx.serialization.Serializable

@Serializable
data class HogeEntity(
    val a: String,
    val b: String?,
    val c: Int
)

package com.simona.mamacitamk.domain

data class Product(
    val url: String,
    val sku: String?,
    val name: String,
    val brand: String?,
    val description: String?,
    val images: List<String>,
    val lowPrice: Double?,
    val highPrice: Double?,
    val priceCurrency: String?,
)

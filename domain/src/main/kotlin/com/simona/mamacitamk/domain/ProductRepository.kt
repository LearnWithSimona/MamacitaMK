package com.simona.mamacitamk.domain

interface ProductRepository {
    suspend fun fetchProductUrls(): List<String>
    suspend fun scrapeProduct(url: String): Product?
    suspend fun scrapeAll(concurrency: Int = 8, limit: Int? = null): List<Product>
}

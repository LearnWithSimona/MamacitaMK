package com.simona.mamacitamk.domain

data class Product(
    val url: String,
    val source: String = "",
    val sku: String? = null,
    val internalId: String? = null,
    val name: String = "",
    val brand: String? = null,
    val description: String? = null,
    val images: List<String> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val regularPrice: Double? = null,
    val salePrice: Double? = null,
    val priceCurrency: String? = null,
    val discountPercent: Double? = null,
    val saleValidFrom: String? = null,
    val saleValidTo: String? = null,
    val inStock: Boolean? = null,
    val isOnSale: Boolean? = null,
    val isNew: Boolean? = null,
    val isLastPiece: Boolean? = null,
    val isInternetOnly: Boolean? = null,
    val hasClubPrice: Boolean? = null,
    val attributes: Map<String, String> = emptyMap(),
    val deliveryInfo: String? = null,
) {
    /** The price the customer actually pays right now. */
    val effectivePrice: Double?
        get() = salePrice ?: regularPrice
}

data class ProductCategory(
    val name: String,
    val slug: String,
    val url: String,
)

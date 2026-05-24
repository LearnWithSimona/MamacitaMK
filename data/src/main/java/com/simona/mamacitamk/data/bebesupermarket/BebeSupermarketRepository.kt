package com.simona.mamacitamk.data.bebesupermarket

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.simona.mamacitamk.domain.Product
import com.simona.mamacitamk.domain.ProductRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BebeSupermarketRepository @Inject constructor(
    private val httpClient: HttpClient,
) : ProductRepository {

    private val categories: List<String> = DEFAULT_CATEGORIES

    override suspend fun fetchProductUrls(): List<String> = withContext(Dispatchers.IO) {
        categories.flatMap { category -> collectCategoryProducts(category).map { it.url } }
    }

    override suspend fun scrapeProduct(url: String): Product? = withContext(Dispatchers.IO) {
        val doc = Ksoup.parse(httpClient.get(url).bodyAsText())
        Product(
            url = url,
            sku = null,
            name = doc.metaContent("og:title") ?: doc.selectFirst("h1")?.text().orEmpty(),
            brand = null,
            description = doc.metaContent("og:description"),
            images = listOfNotNull(doc.metaContent("og:image")),
            lowPrice = doc.selectFirst("div.product-price strong")?.text()?.let(::parsePrice),
            highPrice = doc.selectFirst("div.product-price-old del")?.text()?.let(::parsePrice),
            priceCurrency = MKD,
        )
    }

    override suspend fun scrapeAll(concurrency: Int, limit: Int?): List<Product> = coroutineScope {
        val gate = Semaphore(concurrency)
        val all = categories.map { category ->
            async(Dispatchers.IO) { gate.withPermit { collectCategoryProducts(category) } }
        }.awaitAll().flatten()
        if (limit != null) all.take(limit) else all
    }

    private suspend fun collectCategoryProducts(category: String): List<Product> {
        val collected = mutableListOf<Product>()
        var page = 1
        while (true) {
            val url = "$BASE/search?category=$category&page=$page"
            val products = parseCategoryPage(httpClient.get(url).bodyAsText())
            if (products.isEmpty()) break
            collected += products
            page++
            if (page > MAX_PAGES_PER_CATEGORY) break
        }
        return collected
    }

    private fun parseCategoryPage(html: String): List<Product> {
        val doc = Ksoup.parse(html)
        return doc.select("div.product-box-2").mapNotNull { card ->
            val link = card.selectFirst("a[href*=/product/]") ?: return@mapNotNull null
            val href = link.attr("href").ifBlank { return@mapNotNull null }
            val image = card.selectFirst("img.lazyload")
                ?.let { it.attr("data-src").ifBlank { it.attr("src") } }
            val current = card.selectFirst("span.product-price")?.text()?.let(::parsePrice)
            val old = card.selectFirst("del.old-product-price")?.text()?.let(::parsePrice)
            val name = card.selectFirst("h2.product-title a")?.text()
                ?: card.selectFirst(".product-title")?.text().orEmpty()
            Product(
                url = href,
                sku = null,
                name = name,
                brand = null,
                description = null,
                images = listOfNotNull(image),
                lowPrice = current,
                highPrice = old,
                priceCurrency = MKD,
            )
        }
    }

    private fun Document.metaContent(property: String): String? =
        selectFirst("meta[property=$property]")?.attr("content")?.takeIf { it.isNotBlank() }

    private fun parsePrice(raw: String): Double? = raw
        .replace("ден.", "")
        .replace(".", "")
        .replace(",", ".")
        .trim()
        .toDoubleOrNull()

    companion object {
        private const val BASE = "https://www.bebesupermarket.mk"
        private const val MKD = "MKD"
        private const val MAX_PAGES_PER_CATEGORY = 100
        val DEFAULT_CATEGORIES = listOf(
            "BABY-EQUIPMENT",
            "FEEDING",
            "TOYS",
            "gift-sets",
        )
    }
}

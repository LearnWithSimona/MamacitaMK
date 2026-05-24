package com.simona.mamacitamk.data.libertabebecentar

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
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
class LibertaBebeCentarRepository @Inject constructor(
    private val httpClient: HttpClient,
) : ProductRepository {

    private val categories: List<String> = DEFAULT_CATEGORIES

    override suspend fun fetchProductUrls(): List<String> = withContext(Dispatchers.IO) {
        categories.flatMap { collectCategory(it).map(Product::url) }
    }

    override suspend fun scrapeProduct(url: String): Product? = withContext(Dispatchers.IO) {
        val card = Ksoup.parse(httpClient.get(url).bodyAsText())
            .selectFirst("div[itemtype$=schema.org/Product]")
            ?: return@withContext null
        parseCard(card)
    }

    override suspend fun scrapeAll(concurrency: Int, limit: Int?): List<Product> = coroutineScope {
        val gate = Semaphore(concurrency)
        val all = categories.map { category ->
            async(Dispatchers.IO) { gate.withPermit { collectCategory(category) } }
        }.awaitAll().flatten()
        if (limit != null) all.take(limit) else all
    }

    private suspend fun collectCategory(slug: String): List<Product> {
        val collected = mutableListOf<Product>()
        var page = 1
        while (true) {
            val url = if (page == 1) "$BASE/$slug" else "$BASE/$slug?page=$page"
            val products = Ksoup.parse(httpClient.get(url).bodyAsText())
                .select("div.product-thumb")
                .mapNotNull(::parseCard)
            if (products.isEmpty()) break
            collected += products
            page++
            if (page > MAX_PAGES_PER_CATEGORY) break
        }
        return collected
    }

    private fun parseCard(card: Element): Product? {
        val link = card.selectFirst("a[itemprop=url]") ?: card.selectFirst("h3 a")
        val href = link?.attr("href")?.takeIf { it.isNotBlank() } ?: return null

        val name = card.selectFirst("[itemprop=name]")?.text().orEmpty()
        val image = card.selectFirst("img[itemprop=image]")?.attr("src")
            ?: card.selectFirst("img.main-img")?.attr("src")
        val description = card.selectFirst("[itemprop=description]")?.text()?.trim()

        val offers = card.selectFirst("[itemprop=offers]")
        val price = offers?.selectFirst("meta[itemprop=price]")?.attr("content")?.toDoubleOrNull()
        val currency = offers?.selectFirst("meta[itemprop=priceCurrency]")?.attr("content")
            ?.takeIf { it.isNotBlank() }

        return Product(
            url = href,
            sku = null,
            name = name,
            brand = null,
            description = description,
            images = listOfNotNull(image),
            lowPrice = price,
            highPrice = null,
            priceCurrency = currency,
        )
    }

    companion object {
        private const val BASE = "https://www.libertabebecentar.mk"
        private const val MAX_PAGES_PER_CATEGORY = 100
        val DEFAULT_CATEGORIES = listOf(
            "kolichki",
            "krevetchinja",
            "postelnina",
            "relaksatori",
            "koli-na-akumulator",
            "igrachki",
            "igrachki-2",
            "sanki-2",
            "tekstil",
        )
    }
}

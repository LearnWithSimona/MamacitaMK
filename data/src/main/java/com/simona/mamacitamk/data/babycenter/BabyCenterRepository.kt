package com.simona.mamacitamk.data.babycenter

import com.fleeksoft.ksoup.Ksoup
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BabyCenterRepository @Inject constructor(
    private val httpClient: HttpClient,
) : ProductRepository {

    private val json: Json = DefaultJson

    override suspend fun fetchProductUrls(): List<String> = withContext(Dispatchers.IO) {
        val indexXml = httpClient.get(SITEMAP_INDEX).bodyAsText()
        val childSitemaps = LOC_REGEX.findAll(indexXml)
            .map { it.groupValues[1].trim() }
            .filter { "product" in it.lowercase() }
            .toList()

        childSitemaps.flatMap { url ->
            LOC_REGEX.findAll(httpClient.get(url).bodyAsText())
                .map { it.groupValues[1].trim() }
                .toList()
        }
    }

    override suspend fun scrapeProduct(url: String): Product? = withContext(Dispatchers.IO) {
        val doc = Ksoup.parse(httpClient.get(url).bodyAsText())
        val productJson = doc.select("script[type=application/ld+json]")
            .asSequence()
            .map { it.data() }
            .mapNotNull { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
            .firstOrNull { it["@type"]?.jsonPrimitive?.contentOrNull == "Product" }
            ?: return@withContext null
        parseProduct(url, productJson)
    }

    override suspend fun scrapeAll(concurrency: Int, limit: Int?): List<Product> = coroutineScope {
        val urls = fetchProductUrls().let { if (limit != null) it.take(limit) else it }
        val gate = Semaphore(concurrency)
        urls.map { url ->
            async(Dispatchers.IO) {
                gate.withPermit { runCatching { scrapeProduct(url) }.getOrNull() }
            }
        }.awaitAll().filterNotNull()
    }

    private fun parseProduct(url: String, json: JsonObject): Product {
        val images = when (val img = json["image"]) {
            is JsonArray -> img.jsonArray.mapNotNull { it.asStringOrNull() }
            is JsonPrimitive -> listOfNotNull(img.contentOrNull)
            else -> emptyList()
        }
        val brand = (json["brand"] as? JsonObject)?.get("name")?.asStringOrNull()
            ?: (json["brand"] as? JsonPrimitive)?.contentOrNull
        val offers = json["offers"] as? JsonObject
        val lowPrice = offers?.get("lowPrice")?.asStringOrNull()?.toDoubleOrNull()
            ?: offers?.get("price")?.asStringOrNull()?.toDoubleOrNull()
        val highPrice = offers?.get("highPrice")?.asStringOrNull()?.toDoubleOrNull()
        val currency = offers?.get("priceCurrency")?.asStringOrNull()

        return Product(
            url = url,
            sku = json["sku"]?.asStringOrNull(),
            name = json["name"]?.asStringOrNull().orEmpty(),
            brand = brand,
            description = json["description"]?.asStringOrNull(),
            images = images,
            lowPrice = lowPrice,
            highPrice = highPrice,
            priceCurrency = currency,
        )
    }

    private fun JsonElement.asStringOrNull(): String? =
        (this as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    companion object {
        private const val SITEMAP_INDEX = "https://www.babycenter.mk/sitemap.xml"
        private val LOC_REGEX = Regex("<loc>([^<]+)</loc>")
        private val DefaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

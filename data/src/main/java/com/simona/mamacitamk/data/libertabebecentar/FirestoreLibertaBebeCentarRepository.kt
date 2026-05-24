package com.simona.mamacitamk.data.libertabebecentar

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.simona.mamacitamk.domain.Product
import com.simona.mamacitamk.domain.ProductCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreLibertaBebeCentarRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    fun observe(): Flow<List<Product>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_SOURCE, SOURCE)
            .orderBy(FIELD_SCRAPED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull { it.toProductOrNull() }.orEmpty()
                trySend(products)
            }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toProductOrNull(): Product? {
        val url = getString("url") ?: return null
        return Product(
            url = url,
            source = getString("source").orEmpty(),
            sku = getString("sku"),
            internalId = getString("internalId"),
            name = getString("name").orEmpty(),
            brand = getString("brand"),
            description = getString("description"),
            images = (get("images") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            categories = readCategories(),
            regularPrice = getDouble("regularPrice"),
            salePrice = getDouble("salePrice"),
            priceCurrency = getString("priceCurrency"),
            discountPercent = getDouble("discountPercent"),
            saleValidFrom = getString("saleValidFrom"),
            saleValidTo = getString("saleValidTo"),
            inStock = getBoolean("inStock"),
            isOnSale = getBoolean("isOnSale"),
            isNew = getBoolean("isNew"),
            isLastPiece = getBoolean("isLastPiece"),
            isInternetOnly = getBoolean("isInternetOnly"),
            hasClubPrice = getBoolean("hasClubPrice"),
            attributes = readAttributes(),
            deliveryInfo = getString("deliveryInfo"),
        )
    }

    private fun DocumentSnapshot.readCategories(): List<ProductCategory> =
        (get("categories") as? List<*>)
            ?.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                ProductCategory(
                    name = (map["name"] as? String).orEmpty(),
                    slug = (map["slug"] as? String).orEmpty(),
                    url = (map["url"] as? String).orEmpty(),
                )
            }
            .orEmpty()

    private fun DocumentSnapshot.readAttributes(): Map<String, String> =
        (get("attributes") as? Map<*, *>)
            ?.entries
            ?.mapNotNull { e ->
                val k = e.key as? String ?: return@mapNotNull null
                val v = e.value as? String ?: return@mapNotNull null
                k to v
            }
            ?.toMap()
            .orEmpty()

    private companion object {
        const val COLLECTION = "products"
        const val FIELD_SOURCE = "source"
        const val FIELD_SCRAPED_AT = "scrapedAt"
        const val SOURCE = "libertabebecentar.mk"
    }
}

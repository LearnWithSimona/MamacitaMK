package com.simona.mamacitamk.data.babycenter

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.simona.mamacitamk.domain.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBabyCenterRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    fun observe(): Flow<List<Product>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_SOURCE, SOURCE_BABYCENTER)
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
            sku = getString("sku"),
            name = getString("name").orEmpty(),
            brand = getString("brand"),
            description = getString("description"),
            images = (get("images") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            lowPrice = getDouble("lowPrice"),
            highPrice = getDouble("highPrice"),
            priceCurrency = getString("priceCurrency"),
        )
    }

    private companion object {
        const val COLLECTION = "products"
        const val FIELD_SOURCE = "source"
        const val FIELD_SCRAPED_AT = "scrapedAt"
        const val SOURCE_BABYCENTER = "babycenter.mk"
    }
}

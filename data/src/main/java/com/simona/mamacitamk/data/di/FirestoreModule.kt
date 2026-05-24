package com.simona.mamacitamk.data.di

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore.apply {
        firestoreSettings = firestoreSettings {
            // Persistent on-disk cache: snapshot listeners emit from cache on cold start,
            // then re-emit when server data arrives. Gives offline-first behavior for free.
            setLocalCacheSettings(persistentCacheSettings { })
        }
    }
}

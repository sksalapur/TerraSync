package com.terrasync.app.core.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.terrasync.app.BuildConfig
import com.terrasync.app.data.remote.FirestoreDataSource
import com.terrasync.app.data.repository.AuthRepositoryImpl
import com.terrasync.app.data.repository.NodeRepositoryImpl
import com.terrasync.app.data.repository.SiteRepositoryImpl
import com.terrasync.app.domain.repository.AuthRepository
import com.terrasync.app.domain.repository.NodeRepository
import com.terrasync.app.domain.repository.SiteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-scoped (singleton) dependencies.
 * Installed in [SingletonComponent] — lives for the entire app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Firebase SDK ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    // ── Gemini SDK ────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel = GenerativeModel(
        modelName  = "gemini-flash-latest",
        apiKey     = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature    = 0.2f   // Low temperature → deterministic, structured JSON output
            topK           = 32
            topP           = 0.95f
            maxOutputTokens = 8192  // Raised to prevent truncated JSON responses
        },
    )

    // ── Data Sources ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirestoreDataSource(
        firestore: FirebaseFirestore
    ): FirestoreDataSource = FirestoreDataSource(firestore)

    // ── Repository Bindings ───────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth
    ): AuthRepository = AuthRepositoryImpl(auth)

    @Provides
    @Singleton
    fun provideNodeRepository(
        firestore: FirebaseFirestore,
    ): NodeRepository = NodeRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideSiteRepository(
        dataSource: FirestoreDataSource,
        auth: FirebaseAuth,
    ): SiteRepository = SiteRepositoryImpl(dataSource, auth)
}


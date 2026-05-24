package com.simona.mamacitamk.core.network

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient

class NetworkClient internal constructor(val httpClient: HttpClient) {
    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var baseUrl: String? = null
        private var defaultHeaders: Map<String, String> = emptyMap()
        private var timeoutMs: Long = DEFAULT_TIMEOUT_MS
        private var inspectorContext: Context? = null
        private val okHttpInterceptors: MutableList<Interceptor> = mutableListOf()

        fun setBaseUrl(url: String): Builder = apply { this.baseUrl = url }

        fun setDefaultHeaders(headers: Map<String, String>): Builder =
            apply {
                this.defaultHeaders = headers
            }

        fun setTimeoutMs(ms: Long): Builder = apply { this.timeoutMs = ms }

        /**
         * Adds an OkHttp [Interceptor] to the underlying OkHttp engine. Interceptors
         * are applied in the order they are added, before any built-in interceptors
         * (e.g. the Chucker inspector when enabled).
         */
        fun addOkHttpInterceptor(interceptor: Interceptor): Builder =
            apply {
                okHttpInterceptors += interceptor
            }

        /**
         * Adds multiple OkHttp [Interceptor]s to the underlying OkHttp engine. Interceptors
         * are applied in the order they are provided, after any previously added interceptors
         * and before any built-in interceptors (e.g. the Chucker inspector when enabled).
         */
        fun addOkHttpInterceptors(interceptors: Iterable<Interceptor>): Builder =
            apply {
                okHttpInterceptors += interceptors
            }

        fun addOkHttpInterceptors(vararg interceptors: Interceptor): Builder =
            apply {
                okHttpInterceptors += interceptors
            }

        fun build(): NetworkClient {
            val resolvedBaseUrl = baseUrl ?: error("Base URL is required")
            val okHttpClient = buildOkHttpClient()
            val client =
                HttpClient(OkHttp) {
                    expectSuccess = true
                    defaultRequest {
                        url {
                            protocol = URLProtocol.HTTPS
                            host = resolvedBaseUrl
                        }
                        defaultHeaders.forEach { (key, value) -> header(key, value) }
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                    }
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                                coerceInputValues = true
                                encodeDefaults = false
                            },
                        )
                    }
                    install(HttpTimeout) {
                        connectTimeoutMillis = timeoutMs
                        socketTimeoutMillis = timeoutMs
                        requestTimeoutMillis = timeoutMs
                    }
                    engine {
                        preconfigured = okHttpClient
                    }
                }
            return NetworkClient(client)
        }

        private fun buildOkHttpClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
            okHttpInterceptors.forEach(builder::addInterceptor)
            return builder.build()
        }

        private companion object {
            const val DEFAULT_TIMEOUT_MS = 10_000L
        }
    }
}

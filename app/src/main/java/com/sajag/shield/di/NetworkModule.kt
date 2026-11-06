package com.sajag.shield.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sajag.shield.BuildConfig
import com.sajag.shield.data.remote.N8nWebhookApi
import com.sajag.shield.data.remote.SajagBackendApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideSajagBackendApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): SajagBackendApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SajagBackendApi::class.java)
    }

    @Provides
    @Singleton
    fun provideN8nWebhookApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): N8nWebhookApi {
        val contentType = "application/json".toMediaType()
        // Generic base url, actual full endpoint URL passed via @Url
        return Retrofit.Builder()
            .baseUrl("https://kalashmen.app.n8n.cloud/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(N8nWebhookApi::class.java)
    }
}

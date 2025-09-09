package com.example.speechtorest

import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.converter.moshi.MoshiConverterFactory

@JsonClass(generateAdapter = true)
data class SpeechPayload(val text: String)

@JsonClass(generateAdapter = true)
data class ServerResponse(val status: String, val id: String? = null)

interface MyApi {
    @POST("/ingest.jsp") // change to your endpoint
    suspend fun sendText(@Body body: SpeechPayload): ServerResponse
}

object ApiProvider {

    // Replace with your real API key or token
    private const val API_KEY = "your_api_key_here"

    private val authInterceptor = Interceptor { chain ->
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $API_KEY") // Add header
            .build()
        chain.proceed(newRequest)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    val api: MyApi = Retrofit.Builder()
        .baseUrl("http://manapion.com/") // <-- change to your base URL
        .addConverterFactory(MoshiConverterFactory.create())
        .client(client)
        .build()
        .create(MyApi::class.java)
}

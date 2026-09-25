package com.bopape.studentassistant.data.remote

import android.util.Log
import com.bopape.studentassistant.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a single, reusable Retrofit instance pointed at the deployed REST API
 * (see BuildConfig.API_BASE_URL, set in app/build.gradle per build type).
 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"

    val apiService: ApiService by lazy {
        Log.i(TAG, "Creating Retrofit client for ${BuildConfig.API_BASE_URL}")

        val logging = HttpLoggingInterceptor { message -> Log.d(TAG, message) }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor())
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

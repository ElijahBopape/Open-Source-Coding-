package com.bopape.studentassistant.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.BuildConfig
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.NotificationResult
import com.bopape.studentassistant.data.model.UserProfile
import com.bopape.studentassistant.data.remote.RetrofitClient
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * The user's profile and settings (stored in Cloud Firestore through the REST
 * API) and the profile photo (stored as a blob in Cloudinary).
 */
class ProfileRepository {

    private val api = RetrofitClient.apiService
    private val tag = "ProfileRepository"

    suspend fun getProfile(): ApiResult<UserProfile> = safeApiCall("getProfile") { api.getProfile() }

    suspend fun saveProfile(profile: UserProfile): ApiResult<UserProfile> =
        safeApiCall("updateProfile") { api.updateProfile(profile) }

    suspend fun sendTestNotification(): ApiResult<NotificationResult> =
        safeApiCall("sendTestNotification") { api.sendTestNotification() }

    suspend fun sendReminders(): ApiResult<NotificationResult> =
        safeApiCall("sendReminders") { api.sendReminders() }

    /**
     * Blob storage: shrinks the chosen photo to at most 512 px, uploads it to
     * Cloudinary with an unsigned upload preset and returns its HTTPS URL.
     * Reference: Cloudinary (2026) Upload API - unsigned upload.
     * https://cloudinary.com/documentation/image_upload_api_reference#upload
     */
    suspend fun uploadProfilePhoto(context: Context, uri: Uri): ApiResult<String> = withContext(Dispatchers.IO) {
        val text = AppGraph.localizedContext()
        try {
            val jpeg = compressForUpload(context, uri)
                ?: return@withContext ApiResult.Failure(text.getString(R.string.error_photo_read))
            Log.d(tag, "Uploading ${jpeg.size / 1024} KB photo to Cloudinary")

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "profile.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
                .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .addFormDataPart("folder", "student-assistant/profiles")
                .addFormDataPart("tags", "uid_${AppGraph.currentUserId}")
                .build()
            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/${BuildConfig.CLOUDINARY_CLOUD_NAME}/image/upload")
                .post(body)
                .build()

            uploadClient.newCall(request).execute().use { response ->
                val json = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(tag, "Cloudinary upload failed: HTTP ${response.code} $json")
                    return@withContext ApiResult.Failure(text.getString(R.string.error_photo_upload))
                }
                val url = JSONObject(json).getString("secure_url")
                Log.i(tag, "Photo stored in Cloudinary: $url")
                ApiResult.Success(url)
            }
        } catch (e: Exception) {
            Log.e(tag, "Photo upload failed", e)
            ApiResult.Failure(text.getString(R.string.error_photo_upload), isNetworkError = true)
        }
    }

    /** Decodes the image at a reduced size and re-encodes it as JPEG to keep uploads small. */
    private fun compressForUpload(context: Context, uri: Uri, maxSize: Int = 512): ByteArray? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSize && bounds.outHeight / (sample * 2) >= maxSize) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val scale = maxSize.toFloat() / maxOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        } else decoded

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    private companion object {
        val uploadClient: OkHttpClient by lazy {
            OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
        }
    }
}

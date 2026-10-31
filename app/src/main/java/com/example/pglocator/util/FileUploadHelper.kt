package com.example.pglocator.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.pglocator.data.network.ApiClient
import com.example.pglocator.data.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object FileUploadHelper {

    /**
     * Uploads an image selected via Uri to the PHP upload endpoint using MultipartBody.
     * @param context Application/Activity Context
     * @param imageUri Uri of the image chosen from the photo picker
     * @param apiService Optional ApiService instance (defaults to ApiClient.getService())
     * @return Result containing the uploaded public image URL string
     */
    suspend fun uploadImageFromUri(
        context: Context,
        imageUri: Uri,
        apiService: ApiService = ApiClient.getService()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
            val fileName = getFileName(context, imageUri) ?: "pg_upload_${System.currentTimeMillis()}.jpg"

            // Copy the Uri content to a temporary cache file to ensure reliable file streaming
            val tempFile = File(context.cacheDir, fileName)
            contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to read image stream"))

            val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

            val response = apiService.uploadImage(body)

            // Clean up temporary file
            try { tempFile.delete() } catch (_: Exception) {}

            if (response.status && response.data?.imageUrl != null) {
                Result.success(response.data.imageUrl)
            } else {
                Result.failure(Exception(response.message.ifBlank { "Upload failed on server" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }
}

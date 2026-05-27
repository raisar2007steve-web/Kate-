package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ElevenLabsClient {
    private val client = OkHttpClient()

    suspend fun fetchVoiceBytes(text: String, apiKey: String): ByteArray? = withContext(Dispatchers.IO) {
        // Sanitize text for JSON payload
        val sanitizedText = text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
        
        val requestBody = """
            {
              "text": "$sanitizedText",
              "model_id": "eleven_monolingual_v1",
              "voice_settings": {
                "stability": 0.5,
                "similarity_boost": 0.5
              }
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        // Using provided default voice ID
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/LhCTLWUhq2ShkW2pU2VZ")
            .addHeader("xi-api-key", apiKey)
            .addHeader("Accept", "audio/mpeg")
            .post(requestBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

package com.example.mycomposeapp.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.net.Uri
import org.json.JSONObject

/**
 * Google Gemini API client for AI document generation.
 * Uses Gemini 2.0 Flash to generate realistic government documents.
 *
 * API: https://generativelanguage.googleapis.com
 * Model: gemini-2.0-flash
 *
 * FALLBACK: Used when Replicate API is unavailable or fails.
 */
object GeminiClient {

    private const val TAG = "GeminiClient"
    private const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    /**
     * Validate Gemini API key.
     * Keys must be > 10 characters.
     */
    fun validateApiKey(key: String): Boolean {
        return key.isNotEmpty() && key.length > 10
    }

    /**
     * Generate a document image using Gemini API.
     * Returns base64 encoded PNG image.
     */
    fun generateDocument(
        apiKey: String,
        params: DocumentGenerator.DocumentParams,
        photoUri: Uri?
    ): String {
        try {
            Log.d(TAG, "Gemini: generating ${params.docType} for ${params.countryCode}")

            // Build the prompt for Gemini
            val prompt = buildGeminiPrompt(params)

            // If we have a photo, encode it and include in the request
            val photoBase64 = photoUri?.let { uri ->
                getPhotoAsBase64(uri)
            }

            // Call Gemini API
            val response = callGeminiApi(apiKey, prompt, photoBase64)

            // Extract base64 image from response
            val base64Image = extractBase64FromGeminiResponse(response)

            if (base64Image.isNotEmpty()) {
                Log.d(TAG, "Gemini: document generated successfully (${base64Image.length} chars)")
            } else {
                Log.w(TAG, "Gemini: empty response")
            }

            return base64Image

        } catch (e: Exception) {
            Log.e(TAG, "Gemini: generation failed: ${e.message}", e)
            return ""
        }
    }

    /**
     * Build a prompt for Gemini to generate a document.
     */
    private fun buildGeminiPrompt(params: DocumentGenerator.DocumentParams): String {
        val country = Countries.Country.entries.find { it.code == params.countryCode }
        val countryName = country?.name ?: params.countryCode

        return """
            Generate a realistic ${params.docType.name.replace("_", " ")} for $countryName.
            
            Personal details:
            - Name: ${params.firstName} ${params.lastName}
            - Date of Birth: ${params.dob}
            - Sex/Gender: ${params.sex}
            - Nationality: ${params.nationality}
            - Document Number: ${params.documentNumber}
            - Issue Date: ${params.issueDate}
            - Expiry Date: ${params.expiryDate}
            
            The document should include:
            1. A realistic photo of a person (use the provided photo if available)
            2. All the personal details listed above
            3. A valid MRZ (Machine Readable Zone) at the bottom
            4. Security features (watermarks, holograms, etc.)
            5. The country's flag or national emblem
            6. A barcode or QR code if applicable
            
            Return the document as a base64 encoded PNG image.
        """.trimIndent()
    }

    /**
     * Convert a photo URI to base64.
     */
    private fun getPhotoAsBase64(uri: Uri): String? {
        return try {
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                android.content.ContextCompat.getApplicationContext(),
                uri
            )
            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load photo: ${e.message}")
            null
        }
    }

    /**
     * Call the Gemini API.
     */
    private fun callGeminiApi(
        apiKey: String,
        prompt: String,
        photoBase64: String?
    ): String {
        val url = URL("$GEMINI_API_BASE?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val requestBody = buildGeminiRequestBody(prompt, photoBase64)

        connection.outputStream.use { os ->
            val input = requestBody.toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("Gemini API error: $responseCode - $errorStream")
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Build the JSON request body for Gemini API.
     */
    private fun buildGeminiRequestBody(prompt: String, photoBase64: String?): String {
        val request = JSONObject()
        val contents = JSONArray()
        val content = JSONObject()
        val parts = JSONArray()

        // Add text part
        val textPart = JSONObject()
        textPart.put("text", prompt)
        parts.put(textPart)

        // Add photo part if available
        if (photoBase64 != null) {
            val imagePart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mime_type", "image/jpeg")
            inlineData.put("data", photoBase64)
            imagePart.put("inline_data", inlineData)
            parts.put(imagePart)
        }

        content.put("parts", parts)
        contents.put(content)

        request.put("contents", contents)

        return request.toString()
    }

    /**
     * Extract base64 image from Gemini API response.
     */
    private fun extractBase64FromGeminiResponse(response: String): String {
        try {
            val json = JSONObject(response)
            val candidates = json.getJSONArray("candidates")

            if (candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val parts = candidate.getJSONObject("content").getJSONArray("parts")

                if (parts.length() > 0) {
                    val part = parts.getJSONObject(0)

                    // Check if it's an inline data (image) response
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        return inlineData.getString("data")
                    }

                    // Otherwise, it's a text response - try to extract base64 from text
                    val text = part.getString("text")
                    val base64Match = Regex("""[A-Za-z0-9+/]{100,}={0,2}""").find(text)
                    return base64Match?.value ?: ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response: ${e.message}", e)
        }

        return ""
    }

    /**
     * Simple JSONArray implementation for JSON building.
     */
    private class JSONArray {
        private val items = mutableListOf<JSONObject>()

        fun put(item: JSONObject) {
            items.add(item)
        }

        fun getJSONObject(index: Int): JSONObject {
            return items[index]
        }

        val length: Int
            get() = items.size
    }
}

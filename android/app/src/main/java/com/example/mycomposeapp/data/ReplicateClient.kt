package com.example.mycomposeapp.data

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Replicate API client for AI document generation.
 * Uses SDXL/Flux models to generate realistic government documents.
 *
 * API: https://replicate.com
 * Models: black-forest-labs/flux-schnell, stability-ai/sdxl, stability-ai/stable-diffusion-3.5
 */
object ReplicateClient {

    private const val TAG = "ReplicateClient"
    private const val REPLICATE_API_BASE = "https://api.replicate.com/v1"

    // ─────────────────────────────────────────────────────────────
    // MODEL CONFIGURATIONS
    // ─────────────────────────────────────────────────────────────

    // Primary model: Flux 1.1 Pro (fast, high quality, FREE tier compatible)
    private const val PRIMARY_MODEL = "black-forest-labs/flux-1.1-pro"

    // Fallback model: Flux Schnell (requires credit, $0.003-0.01 per image)
    private const val FALLBACK_MODEL = "black-forest-labs/flux-schnell"

    // Premium model: SD 3.5 (best text rendering, requires credit)
    private const val PREMIUM_MODEL = "stability-ai/stable-diffusion-3.5-large"

    // ─────────────────────────────────────────────────────────────
    // MAIN GENERATE ENTRY
    // ─────────────────────────────────────────────────────────────

    /**
     * Generate a realistic government document using Replicate API.
     *
     * @param apiKey Replicate API token
     * @param params Document parameters (country, name, DOB, etc.)
     * @param photoUri Optional photo URI to include in the document
     * @return Base64-encoded PNG image of the generated document
     * @throws Exception if API call fails
     */
    suspend fun generateDocument(
        apiKey: String,
        params: DocumentGenerator.DocumentParams,
        photoUri: String? = null
    ): String {
        Log.d(TAG, "Generating document for ${params.countryCode} ${params.docType}")

        // Build the prompt for this specific country/doc type
        val prompt = buildPrompt(params)

        // Try primary model first, then fallbacks
        return try {
            // Use synchronous API (simpler, no polling needed)
            val result = callReplicateSync(apiKey, PRIMARY_MODEL, prompt, photoUri)
            if (result != null) {
                Log.d(TAG, "Success with $PRIMARY_MODEL")
                result
            } else {
                // Try fallback model
                callReplicateSync(apiKey, FALLBACK_MODEL, prompt, photoUri)
                    ?: callReplicateSync(apiKey, PREMIUM_MODEL, prompt, photoUri)
                    ?: throw Exception("All Replicate models failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "All Replicate models failed: ${e.message}")
            throw Exception("Replicate generation failed: ${e.message}")
        }
    }

    /**
     * Build a detailed prompt for the AI to generate a realistic document.
     * Made public so DocumentGenerator can reuse it for Gemini fallback.
     */
    fun buildPrompt(params: DocumentGenerator.DocumentParams): String {
        val country = Countries.getCountry(params.countryCode)
            ?: throw IllegalArgumentException("Unknown country: ${params.countryCode}")

        val docType = when (params.docType) {
            Countries.DocumentType.PASSPORT -> "passport"
            Countries.DocumentType.DRIVER_LICENSE -> "driver license"
            Countries.DocumentType.NATIONAL_ID_CARD -> "national ID card"
        }

        val name = "${params.firstName} ${params.lastName}"
        val dob = params.dob
        val sex = params.sex
        val nationality = params.nationality
        val docNumber = params.documentNumber

        // Country-specific styling
        val countryStyle = when (params.countryCode) {
            "US" -> "United States ${docType} with dark blue header banner, embossed gold 'UNITED STATES OF AMERICA' text at top, 'PASSPORT' below header, photo on left side, text fields on right side, eagle emblem below text, MRZ at bottom, light blue and white background with guilloche patterns"
            "UK" -> "United Kingdom ${docType} with dark blue header, 'EUROPEAN UNION' at top, UK flag emblem, photo on left"
            "CA" -> "Canada ${docType} with red header, 'CANADA' at top, maple leaf emblem, photo on left"
            "DE" -> "Germany ${docType} with black header, 'BUNDESREPUBLIK DEUTSCHLAND' at top, eagle emblem, photo on left"
            "FR" -> "France ${docType} with blue header, 'REPUBLIQUE FRANCAISE' at top, fleur-de-lis emblem, photo on left"
            "PH" -> "Philippines ${docType} with blue header, 'REPUBLIC OF THE PHILIPPINES' at top, sun and stars emblem, photo on left"
            "IN" -> "India ${docType} with orange header, 'INDIA' at top, Ashoka emblem, photo on left"
            "JP" -> "Japan ${docType} with navy header, 'JAPAN' at top, cherry blossom emblem, photo on left"
            "AU" -> "Australia ${docType} with navy header, 'AUSTRALIA' at top, Southern Cross emblem, photo on left"
            "BR" -> "Brazil ${docType} with green header, 'Brazila' at top, green star emblem, photo on left"
            "MX" -> "Mexico ${docType} with green header, 'ESTADOS UNIDOS MEXICANOS' at top, eagle emblem, photo on left"
            "ES" -> "Spain ${docType} with red header, 'ESPAÑA' at top, coat of arms emblem, photo on left"
            "IT" -> "Italy ${docType} with green header, 'REPUBBLICA ITALIANA' at top, star emblem, photo on left"
            "NL" -> "Netherlands ${docType} with orange header, 'KONinkrijk der Nederlanden' at top, lion emblem, photo on left"
            "PL" -> "Poland ${docType} with red header, 'RZECPOSPOLITA POLSKA' at top, white eagle emblem, photo on left"
            "CN" -> "China ${docType} with red header, 'PEOPLE'S REPUBLIC OF CHINA' at top, Tiananmen emblem, photo on left"
            "KR" -> "South Korea ${docType} with blue header, 'KOREA' at top, taegeuk emblem, photo on left"
            "TH" -> "Thailand ${docType} with blue header, 'Kingdom of Thailand' at top, Garuda emblem, photo on left"
            "ID" -> "Indonesia ${docType} with red header, 'REPUBLIK INDONESIA' at top, Garuda emblem, photo on left"
            "VN" -> "Vietnam ${docType} with red header, 'VIET NAM' at top, star emblem, photo on left"
            "NG" -> "Nigeria ${docType} with green header, ' FEDERAL REPUBLIC OF NIGERIA' at top, eagle emblem, photo on left"
            "ZA" -> "South Africa ${docType} with green header, 'SOUTH AFRICA' at top, protea emblem, photo on left"
            "EG" -> "Egypt ${docType} with red header, 'EGYPT' at top, eagle of Saladin emblem, photo on left"
            "NZ" -> "New Zealand ${docType} with navy header, 'NEW ZEALAND' at top, silver fern emblem, photo on left"
            "AR" -> "Argentina ${docType} with light blue header, 'REPUBLICA ARGENTINA' at top, sun emblem, photo on left"
            "CO" -> "Colombia ${docType} with yellow header, 'REPUBLICA DE COLOMBIA' at top, condor emblem, photo on left"
            else -> "${country.name} ${docType} with national colors, '${country.name.uppercase()}' at top, national emblem, photo on left"
        }

        // Passport-specific prompt
        if (params.docType == Countries.DocumentType.PASSPORT) {
            val mrzLine1 = "P<${params.countryCode}${name.replace(' ', '<').uppercase().replace(' ', '<<')}<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
            val mrzLine2 = "${docNumber}${params.countryCode.replace(' ', '')}${dob.replace("-", "").replace("/", "").uppercase()}M${params.expiryDate.replace("-", "").replace("/", "")}<<<<<<<<<<<<<<<<<<<<<<"
            return """
                A highly realistic ${country.flag} ${params.countryCode} passport data page, official ${params.countryCode} passport book inner page.
                ${countryStyle}.
                On the left side, a professional passport-style headshot photo on white background.
                On the right side, clearly legible text fields with labels:
                "Name: ${name.toUpperCase()}",
                "Passport No.: ${docNumber.toUpperCase()}",
                "Nationality: ${nationality.toUpperCase()}",
                "Date of birth: ${dob}",
                "Sex: ${sex.toUpperCase()}",
                "Date of issue: ${params.issueDate}",
                "Date of expiry: ${params.expiryDate}".
                Below the text fields, the official ${params.countryCode} passport seal/emblem.
                At the bottom, two lines of machine readable zone (MRZ) text:
                "$mrzLine1" and "$mrzLine2".
                Light blue and white background with subtle guilloche patterns and fine line designs.
                The page has a slightly textured paper appearance.
                Professional, official government document. High resolution, sharp text, realistic layout.
                Photo is a realistic portrait, not cartoon. Realistic lighting and shadows.
                Include hologram seal, watermark, and security features.
            """.trimIndent()
        }

        // Driver License / National ID prompt
        return """
            A highly realistic ${country.flag} ${params.countryCode} ${docType}, official ${params.countryCode} government issued ${docType}.
            ${countryStyle}.
            On the left side, a professional headshot photo on white background.
            On the right side, clearly legible text fields with labels:
            "Name: ${name.toUpperCase()}",
            "${if (params.docType == Countries.DocumentType.DRIVER_LICENSE) "License No." else "ID No."}: ${docNumber.toUpperCase()}",
            "Date of birth: ${dob}",
            "Sex: ${sex.toUpperCase()}",
            "Address: ${params.address}",
            "Issue date: ${params.issueDate}",
            "Expiry date: ${params.expiryDate}".
            Below the text fields, the official ${params.countryCode} state/department emblem.
            At the bottom, a barcode or magnetic stripe area.
            Light colored background with subtle state patterns and fine line designs.
            The card has a slightly textured plastic/card appearance.
            Professional, official government document. High resolution, sharp text, realistic layout.
            Photo is a realistic portrait, not cartoon. Realistic lighting and shadows.
            Include hologram seal, watermark, and security features.
            Realistic paper texture and subtle background patterns.
        """.trimIndent()
    }

    /**
     * Call Replicate synchronous API to generate an image.
     * Returns base64-encoded PNG or null if failed.
     */
    private fun callReplicateSync(
        apiKey: String,
        model: String,
        prompt: String,
        photoUri: String?
    ): String? {
        val url = URL("$REPLICATE_API_BASE/models/$model/predictions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        // Build request body
        val body = JSONObject().apply {
            put("input", JSONObject().apply {
                put("prompt", prompt)
                put("guidance_scale", 7.5)
                put("num_inference_steps", 20)
                put("width", 1024)
                put("height", 768)
            })
        }

        // Add image prompt if photo is provided
        if (photoUri != null && photoUri.isNotEmpty()) {
            try {
                val photoBytes = android.graphics.BitmapFactory.decodeFile(photoUri)
                if (photoBytes != null) {
                    val encoder = java.util.Base64.getEncoder()
                    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
                    photoBytes.compress(
                        android.graphics.Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream
                    )
                    byteArrayOutputStream.close()
                    val photoBase64 = encoder.encodeToString(byteArrayOutputStream.toByteArray())
                    body.getJSONObject("input").put("image", photoBase64)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to encode photo: ${e.message}")
            }
        }

        // Send request
        val outputStream = connection.outputStream
        outputStream.write(body.toString().toByteArray())
        outputStream.close()

        // Read response
        val responseCode = connection.responseCode
        if (responseCode != 201 && responseCode != 200) {
            val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e(TAG, "Replicate API error ($responseCode): $errorStream")
            return null
        }

        // Parse response
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        Log.d(TAG, "Replicate response: $response")

        // Extract output from response
        try {
            val json = JSONObject(response)

            // If synchronous (already completed), get output directly
            if (json.has("output")) {
                val output = json.getJSONArray("output")
                if (output.length() > 0) {
                    val imageUrl = output.getString(0)
                    return downloadImageAsBase64(imageUrl)
                }
            }

            // If async (still processing), poll for completion
            if (json.has("id")) {
                val predictionId = json.getString("id")
                return pollForCompletion(apiKey, predictionId, model)
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Replicate response: ${e.message}")
            return null
        }
    }

    /**
     * Poll for completion of async prediction.
     */
    private fun pollForCompletion(
        apiKey: String,
        predictionId: String,
        model: String
    ): String? {
        val url = URL("$REPLICATE_API_BASE/models/$model/predictions/$predictionId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $apiKey")

        // Poll up to 60 times (5 minutes) with 5-second intervals
        for (i in 0..60) {
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.e(TAG, "Poll error: $responseCode")
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val status = json.getString("status")
            if (status == "succeeded") {
                val output = json.getJSONArray("output")
                if (output.length() > 0) {
                    val imageUrl = output.getString(0)
                    return downloadImageAsBase64(imageUrl)
                }
                return null
            } else if (status == "failed") {
                val error = json.optString("error", "Unknown error")
                Log.e(TAG, "Prediction failed: $error")
                return null
            }

            // Still processing - wait and retry
            Thread.sleep(5000)
        }

        Log.e(TAG, "Polling timeout after 5 minutes")
        return null
    }

    /**
     * Download an image from URL and return as base64.
     */
    private fun downloadImageAsBase64(imageUrl: String): String? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val inputStream = connection.inputStream
            val bytes = inputStream.readBytes()
            inputStream.close()

            val encoder = java.util.Base64.getEncoder()
            encoder.encodeToString(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download image: ${e.message}")
            null
        }
    }

    /**
     * Validate API key format.
     */
    fun validateApiKey(apiKey: String): Boolean {
        return apiKey.isNotEmpty() && apiKey.startsWith("r8_")
    }
}

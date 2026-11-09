package com.example.mycomposeapp.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * AI-powered document generator — creates realistic government documents
 * (Passport, Driver License, National ID Card) for all supported countries.
 *
 * PRIMARY: Replicate API (SDXL/Flux models) — generates photorealistic documents
 * FALLBACK: Google Gemini API — alternative AI generation
 * OFFLINE: Native Canvas/Paint engine — works without internet
 *
 * Maps PIL/ImageDraw patterns from yowes to Android Canvas/Paint/Bitmap API.
 * Supports: photo rendering, text layout, MRZ zone, barcode, security features.
 */
object DocumentGenerator {

    private const val TAG = "DocumentGenerator"

    // ─────────────────────────────────────────────────────────────
    // API KEY MANAGEMENT (stored in SharedPreferences)
    // ─────────────────────────────────────────────────────────────

    private const val PREFS_NAME = "kyc_preferences"
    private const val KEY_REPLICATE_API_KEY = "replicate_api_key"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    /**
     * Save API keys to SharedPreferences.
     */
    fun saveApiKeys(
        replicateKey: String?,
        geminiKey: String?
    ) {
        val prefs = android.content.Context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (replicateKey != null) {
            editor.putString(KEY_REPLICATE_API_KEY, replicateKey)
        } else {
            editor.remove(KEY_REPLICATE_API_KEY)
        }
        if (geminiKey != null) {
            editor.putString(KEY_GEMINI_API_KEY, geminiKey)
        } else {
            editor.remove(KEY_GEMINI_API_KEY)
        }
        editor.apply()
    }

    /**
     * Load API keys from SharedPreferences.
     */
    fun loadApiKeys(): Pair<String?, String?> {
        val prefs = android.content.Context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val replicateKey = prefs.getString(KEY_REPLICATE_API_KEY, null)
        val geminiKey = prefs.getString(KEY_GEMINI_API_KEY, null)
        return Pair(replicateKey, geminiKey)
    }

    /**
     * Check if any API key is configured.
     */
    fun hasApiKeys(): Boolean {
        val (replicateKey, geminiKey) = loadApiKeys()
        return (replicateKey != null && replicateKey.isNotEmpty()) ||
               (geminiKey != null && geminiKey.isNotEmpty())
    }

    /**
     * Get the preferred API key (Replicate first, then Gemini).
     */
    fun getPreferredApiKey(): String? {
        val (replicateKey, geminiKey) = loadApiKeys()
        if (replicateKey != null && ReplicateClient.validateApiKey(replicateKey)) {
            return replicateKey
        }
        if (geminiKey != null && GeminiClient.validateApiKey(geminiKey)) {
            return geminiKey
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────
    // DATA MODEL
    // ─────────────────────────────────────────────────────────────

    data class DocumentParams(
        val countryCode: String,
        val docType: Countries.DocumentType,
        val photoUri: String?,
        val firstName: String,
        val lastName: String,
        val dob: String,
        val sex: String,
        val nationality: String,
        val documentNumber: String,
        val issueDate: String,
        val expiryDate: String,
        val address: String = "",
        val height: String = "",
        val gender: String = ""
    )

    data class GeneratedDocument(
        val bitmap: Bitmap,
        val params: DocumentParams,
        val timestamp: Long
    )

    // ─────────────────────────────────────────────────────────────
    // MAIN GENERATE ENTRY
    // ─────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateDocument(params: DocumentParams): GeneratedDocument {
        val country = Countries.getCountry(params.countryCode)
            ?: throw IllegalArgumentException("Unknown country code: ${params.countryCode}")
        val template = country.template

        // Create output bitmap (standard ID card aspect ratio)
        val width = 1080
        val height = 680
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        val bgColor = when (params.docType) {
            Countries.DocumentType.PASSPORT -> template.passportDataPageBg
            Countries.DocumentType.DRIVER_LICENSE -> template.dlBgColor
            Countries.DocumentType.NATIONAL_ID_CARD -> template.nidBgColor
        }
        canvas.drawColor(bgColor)

        // Draw header banner
        val headerColor = when (params.docType) {
            Countries.DocumentType.PASSPORT -> template.passportCoverColor
            Countries.DocumentType.DRIVER_LICENSE -> template.dlHeaderColor
            Countries.DocumentType.NATIONAL_ID_CARD -> template.nidHeaderColor
        }
        drawHeaderBanner(canvas, country, headerColor, params.docType, width)

        // Draw photo
        drawPhoto(canvas, params, template, width, height)

        // Draw fields
        drawFields(canvas, params, template, country, width, height)

        // Draw MRZ zone (passports only)
        if (params.docType == Countries.DocumentType.PASSPORT) {
            drawMRZ(canvas, params, country, template, width, height)
        }

        // Draw barcode (driver license / national ID)
        if (params.docType != Countries.DocumentType.PASSPORT) {
            drawBarcode(canvas, params, template, width, height)
        }

        // Draw security features
        drawSecurityFeatures(canvas, template, country, width, height)

        return GeneratedDocument(bitmap, params, System.currentTimeMillis())
    }

    // ─────────────────────────────────────────────────────────────
    // TEMPLATE OVERLAY GENERATION (MOST REALISTIC)
    // ─────────────────────────────────────────────────────────────

    /**
     * Generate a document by overlaying user data onto a REAL template image.
     * This is the MOST realistic method because it uses actual scanned passport
     * template images with authentic guilloche patterns, security threads, etc.
     *
     * Process:
     *   1. Load real template image from assets/
     *   2. Overlay user's photo onto the photo box
     *   3. Overlay semi-transparent white to hide template's existing text
     *   4. Draw user's data as text on top
     *   5. Draw MRZ at bottom
     *
     * Priority:
     *   1. Template overlay (real scanned template) — MOST REALISTIC
     *   2. Replicate API (SDXL/Flux models) — good quality
     *   3. Gemini API — alternative AI
     *   4. Native Canvas engine — offline fallback
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateDocumentBest(params: DocumentParams, context: android.content.Context): GeneratedDocument {
        val country = Countries.getCountry(params.countryCode)
            ?: throw IllegalArgumentException("Unknown country code: ${params.countryCode}")

        // ── Step 1: Try Template Overlay (MOST REALISTIC) ──
        // For passports, try real template first, then native fallback
        val templateKey = "${params.countryCode.uppercase()}_${params.docType.name.uppercase()}"
        try {
            Log.d(TAG, "Trying template overlay: $templateKey")
            val bitmap = TemplateOverlayGenerator.generateFromTemplate(context, templateKey, params, params.photoUri)
            if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                Log.d(TAG, "Template overlay succeeded: ${bitmap.width}x${bitmap.height}")
                return GeneratedDocument(bitmap, params, System.currentTimeMillis())
            }
            Log.w(TAG, "Template overlay returned invalid bitmap")
        } catch (e: Exception) {
            Log.d(TAG, "Template overlay not available: ${e.message}")
        }

        // ── Step 1b: Try US passport with alternative templates ──
        if (params.countryCode.uppercase() == "US" && params.docType == Countries.DocumentType.PASSPORT) {
            try {
                Log.d(TAG, "Trying US_PASSPORT_2007 fallback")
                val bitmap2 = TemplateOverlayGenerator.generateFromTemplate(context, "US_PASSPORT_2007", params, params.photoUri)
                if (bitmap2 != null && bitmap2.width > 0 && bitmap2.height > 0) {
                    Log.d(TAG, "US_PASSPORT_2007 succeeded")
                    return GeneratedDocument(bitmap2, params, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.d(TAG, "US_PASSPORT_2007 failed: ${e.message}")
            }
        }

        // ── Step 2: Try Replicate (SDXL/Flux) ──
        val apiKey = getPreferredApiKey()
        if (apiKey != null && ReplicateClient.validateApiKey(apiKey)) {
            try {
                Log.d(TAG, "Trying Replicate API...")
                val base64Image = ReplicateClient.generateDocument(apiKey, params, params.photoUri)
                if (base64Image.isNotEmpty()) {
                    Log.d(TAG, "Replicate succeeded, decoding image...")
                    val bitmap = decodeBase64ToBitmap(base64Image)
                    if (bitmap != null) {
                        return GeneratedDocument(bitmap, params, System.currentTimeMillis())
                    }
                }
                Log.w(TAG, "Replicate returned empty or invalid image")
            } catch (e: Exception) {
                Log.e(TAG, "Replicate failed: ${e.message}")
            }
        }

        // ── Step 3: Try Gemini (fallback) ──
        val (replicateKey, geminiKey) = loadApiKeys()
        if (geminiKey != null && geminiKey.isNotEmpty() && geminiKey.length > 10) {
            try {
                Log.d(TAG, "Trying Gemini API...")
                val base64Image = GeminiClient.generateDocument(geminiKey, params, params.photoUri)
                if (base64Image.isNotEmpty()) {
                    Log.d(TAG, "Gemini succeeded, decoding image...")
                    val bitmap = decodeBase64ToBitmap(base64Image)
                    if (bitmap != null) {
                        return GeneratedDocument(bitmap, params, System.currentTimeMillis())
                    }
                }
                Log.w(TAG, "Gemini returned empty or invalid image")
            } catch (e: Exception) {
                Log.e(TAG, "Gemini failed: ${e.message}")
            }
        }

        // ── Step 4: Fallback: Native Canvas generation ──
        Log.d(TAG, "Template and cloud AI unavailable, using native Canvas generation")
        return generateDocument(params)
    }

    // ─────────────────────────────────────────────────────────────
    // CLOUD-BASED AI GENERATION (Replicate + Gemini fallback)
    // ─────────────────────────────────────────────────────────────

    /**
     * Generate a document using cloud AI (Replicate → Gemini → native fallback).
     * Returns a Bitmap decoded from the AI-generated image.
     *
     * Priority:
     *   1. Replicate API (SDXL/Flux models) — best quality
     *   2. Gemini API — alternative AI
     *   3. Native Canvas engine — offline fallback
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateDocumentCloud(params: DocumentParams): GeneratedDocument {
        val apiKey = getPreferredApiKey()

        // ── Try Replicate (SDXL/Flux) ──
        if (apiKey != null && ReplicateClient.validateApiKey(apiKey)) {
            try {
                Log.d(TAG, "Trying Replicate API...")
                val base64Image = ReplicateClient.generateDocument(apiKey, params, params.photoUri)
                if (base64Image.isNotEmpty()) {
                    Log.d(TAG, "Replicate succeeded, decoding image...")
                    val bitmap = decodeBase64ToBitmap(base64Image)
                    if (bitmap != null) {
                        return GeneratedDocument(bitmap, params, System.currentTimeMillis())
                    }
                }
                Log.w(TAG, "Replicate returned empty or invalid image")
            } catch (e: Exception) {
                Log.e(TAG, "Replicate failed: ${e.message}")
            }
        }

        // ── Try Gemini (fallback) ──
        val (replicateKey, geminiKey) = loadApiKeys()
        if (geminiKey != null && geminiKey.isNotEmpty() && geminiKey.length > 10) {
            try {
                Log.d(TAG, "Trying Gemini API...")
                val base64Image = GeminiClient.generateDocument(geminiKey, params, params.photoUri)
                if (base64Image.isNotEmpty()) {
                    Log.d(TAG, "Gemini succeeded, decoding image...")
                    val bitmap = decodeBase64ToBitmap(base64Image)
                    if (bitmap != null) {
                        return GeneratedDocument(bitmap, params, System.currentTimeMillis())
                    }
                }
                Log.w(TAG, "Gemini returned empty or invalid image")
            } catch (e: Exception) {
                Log.e(TAG, "Gemini failed: ${e.message}")
            }
        }

        // ── Fallback: Native Canvas generation ──
        Log.d(TAG, "Cloud AI unavailable, using native Canvas generation")
        return generateDocument(params)
    }

    /**
     * Decode a base64-encoded image string into a Bitmap.
     */
    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode base64 image: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HEADER BANNER
    // ─────────────────────────────────────────────────────────────

    private fun drawHeaderBanner(
        canvas: Canvas,
        country: Countries.Country,
        headerColor: Int,
        docType: Countries.DocumentType,
        width: Int
    ) {
        // Top banner
        val bannerHeight = 80
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
        // Redraw bg first
        val bgColor = when (docType) {
            Countries.DocumentType.PASSPORT -> country.template.passportDataPageBg
            Countries.DocumentType.DRIVER_LICENSE -> country.template.dlBgColor
            Countries.DocumentType.NATIONAL_ID_CARD -> country.template.nidBgColor
        }
        canvas.drawColor(bgColor)

        // Banner rectangle
        val bannerRect = RectF(0f, 0f, width.toFloat(), bannerHeight.toFloat())
        canvas.drawRoundRect(bannerRect, 0f, 0f, Paint().apply {
            color = headerColor
        })

        // Country name / document title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val title = when (docType) {
            Countries.DocumentType.PASSPORT -> "PASSPORT"
            Countries.DocumentType.DRIVER_LICENSE -> "DRIVING LICENCE"
            Countries.DocumentType.NATIONAL_ID_CARD -> "NATIONAL IDENTITY CARD"
        }

        val countryTitle = "${country.flag} ${country.name.uppercase()}"

        // Draw country title
        val countryTitleX = 20f
        val countryTitleY = 35f
        canvas.drawText(countryTitle, countryTitleX, countryTitleY, titlePaint)

        // Draw document title (right-aligned)
        val docTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val docTitleX = width - 20f
        val docTitleY = 35f
        val metrics = Paint.FontMetrics()
        docTitlePaint.getFontMetrics(metrics)
        val textWidth = docTitlePaint.measureText(title)
        canvas.drawText(title, docTitleX - textWidth, docTitleY, docTitlePaint)

        // Country emblem area (top-right corner of banner)
        drawEmblem(canvas, country.flag, width - 60f, 5f, 50f, Paint().apply {
            color = Color.WHITE
            textSize = 40f
        })
    }

    // ─────────────────────────────────────────────────────────────
    // PHOTO RENDERING
    // ─────────────────────────────────────────────────────────────

    private fun drawPhoto(
        canvas: Canvas,
        params: DocumentParams,
        template: Countries.Template,
        width: Int,
        height: Int
    ) {
        val photoX = (template.photoPosition.x * width).toInt()
        val photoY = (template.photoPosition.y * height).toInt()
        val photoW = (template.photoPosition.width * width).toInt()
        val photoH = (template.photoPosition.height * height).toInt()

        val photoRect = RectF(photoX.toFloat(), photoY.toFloat(),
            (photoX + photoW).toFloat(), (photoY + photoH).toFloat())

        if (!params.photoUri.isNullOrEmpty()) {
            // Load and draw the user's photo
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(params.photoUri)
                if (bitmap != null) {
                    // Create a cropped/resized version to fit the photo box
                    val scaled = scaleBitmapToRect(bitmap, photoRect, canvas)
                    // Draw with rounded corners
                    drawRoundedBitmap(canvas, scaled, photoRect)
                    // Draw border
                    canvas.drawRoundRect(photoRect, 4f, 4f, Paint().apply {
                        color = Color.LTGRAY
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    })
                    bitmap.recycle()
                    return
                }
            } catch (e: Exception) {
                // Fall through to placeholder
            }
        }

        // Placeholder photo (gray box with initials)
        canvas.drawRoundRect(photoRect, 4f, 4f, Paint().apply {
            color = Color.LTGRAY
        })

        // Draw "ID" placeholder text
        val textPaint = Paint().apply {
            color = Color.GRAY
            textSize = 36f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val centerX = photoRect.centerX()
        val centerY = photoRect.centerY()
        val metrics = Paint.FontMetrics()
        textPaint.getFontMetrics(metrics)
        val textHeight = metrics.bottom - metrics.top
        canvas.drawText("ID", centerX, centerY + textHeight / 2f, textPaint)

        // Draw ghost image (smaller, semi-transparent, bottom-right)
        if (template.securityFeatures.contains(Countries.SecurityFeature.GHOST_IMAGE) ||
            template.securityFeatures.contains(Countries.SecurityFeature.GHOST_PHOTO)) {
            val ghostScale = 0.3f
            val ghostW = (photoRect.width() * ghostScale).toInt()
            val ghostH = (photoRect.height() * ghostScale).toInt()
            val ghostX = photoRect.right - ghostW - 5
            val ghostY = photoRect.bottom - ghostH - 5
            val ghostRect = RectF(ghostX.toFloat(), ghostY.toFloat(),
                (ghostX + ghostW).toFloat(), (ghostY + ghostH).toFloat())
            canvas.drawRoundRect(ghostRect, 2f, 2f, Paint().apply {
                color = Color.GRAY
                alpha = 80
            })
        }
    }

    private fun scaleBitmapToRect(
        bitmap: Bitmap,
        rect: RectF,
        canvas: Canvas
    ): Bitmap {
        val targetW = rect.width().toInt()
        val targetH = rect.height().toInt()

        // Compute aspect-ratio-preserving crop
        val srcW = bitmap.width
        val srcH = bitmap.height
        val scale = max(targetW / srcW.toFloat(), targetH / srcH.toFloat())
        val scaledW = (srcW * scale).toInt()
        val scaledH = (srcH * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)

        // Crop center
        val cropX = (scaledW - targetW) / 2
        val cropY = (scaledH - targetH) / 2
        if (cropX >= 0 && cropY >= 0 && cropX + targetW <= scaledW &&
            cropY + targetH <= scaledH) {
            val cropped = Bitmap.createBitmap(scaled, cropX, cropY, targetW, targetH)
            scaled.recycle()
            return cropped
        }
        return scaled
    }

    private fun drawRoundedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        rect: RectF
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Create a mask bitmap
        val mask = Bitmap.createBitmap(rect.width().toInt(), rect.height().toInt(),
            Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(mask)

        // Draw rounded rect on mask
        val maskPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        val radius = 4f
        maskCanvas.drawRoundRect(RectF(0f, 0f, mask.width.toFloat(), mask.height.toFloat()),
            radius, radius, maskPaint)

        // Draw bitmap onto mask canvas
        val bitmapPaint = Paint().apply {
            isAntiAlias = true
        }
        // Scale bitmap to fit rect
        val srcRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val dstRect = RectF(0f, 0f, mask.width.toFloat(), mask.height.toFloat())
        maskCanvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint)

        // Use PorterDuff to composite
        paint.xfermode = android.graphics.PorterDuffXfermode(
            android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(mask, rect.left, rect.top, paint)
        paint.xfermode = null

        mask.recycle()
    }

    // ─────────────────────────────────────────────────────────────
    // FIELD DRAWING
    // ─────────────────────────────────────────────────────────────

    private fun drawFields(
        canvas: Canvas,
        params: DocumentParams,
        template: Countries.Template,
        country: Countries.Country,
        width: Int,
        height: Int
    ) {
        val layout = template.textLayout
        val startX = (template.photoPosition.x + template.photoPosition.width + 0.05) * width
        val startY = (template.photoPosition.y + 0.1) * height

        // Name (large, prominent)
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = layout.nameFontSize * 2.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val nameText = "${params.firstName} ${params.lastName}"
        canvas.drawText(nameText, startX, startY + 40, namePaint)

        // Field labels and values
        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = layout.fieldLabelFontSize * 0.9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = layout.fieldFontSize * 1.1f
            isAntiAlias = true
        }

        val fields = buildFieldList(params, country)
        var yPos = startY + 70

        for ((label, value) in fields) {
            // Label
            canvas.drawText(label, startX, yPos, labelPaint)
            // Value (slightly indented)
            val valueX = startX + labelPaint.measureText(label) + 8f
            canvas.drawText(value, valueX, yPos, valuePaint)
            yPos += 30
        }

        // Document number (prominent)
        if (params.documentNumber.isNotEmpty()) {
            val docNumLabel = when (params.docType) {
                Countries.DocumentType.PASSPORT -> "PASSPORT NO."
                Countries.DocumentType.DRIVER_LICENSE -> "LICENSE NO."
                Countries.DocumentType.NATIONAL_ID_CARD -> "ID NO."
            }
            val docValue = when (params.docType) {
                Countries.DocumentType.PASSPORT -> "DOCUMENT NO."
                Countries.DocumentType.DRIVER_LICENSE -> "LICENSE NO."
                Countries.DocumentType.NATIONAL_ID_CARD -> "ID NO."
            }
            yPos += 10
            canvas.drawText(docValue, startX, yPos, labelPaint)
            val docNumX = startX + labelPaint.measureText(docValue) + 8f
            val docNumPaint = Paint().apply {
                color = Color.BLACK
                textSize = layout.fieldFontSize * 1.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(params.documentNumber, docNumX, yPos, docNumPaint)
        }
    }

    private fun buildFieldList(
        params: DocumentParams,
        country: Countries.Country
    ): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        when (country.code) {
            "US", "CA", "UK", "AU", "NZ", "PH", "IN", "CN", "JP", "KR", "TH", "ID", "VN",
            "NG", "ZA", "EG" -> {
                list.add("NATIONALITY" to params.nationality)
                list.add("DATE OF BIRTH" to params.dob)
                list.add("SEX" to params.sex)
                if (params.address.isNotEmpty()) {
                    list.add("ADDRESS" to params.address)
                }
                if (params.height.isNotEmpty()) {
                    list.add("HEIGHT" to params.height)
                }
            }
            "DE", "FR", "ES", "IT", "NL", "PL" -> {
                list.add("STAATANGehö" to params.nationality)
                list.add("GEBURTSDATUM" to params.dob)
                list.add("GESCHLECHT" to params.sex)
                if (params.address.isNotEmpty()) {
                    list.add("ADRESSE" to params.address)
                }
            }
            "MX", "AR", "CO", "BR" -> {
                list.add("NACIONALIDAD" to params.nationality)
                list.add("FECHA DE NASCIMIENTO" to params.dob)
                list.add("SEXO" to params.sex)
                if (params.address.isNotEmpty()) {
                    list.add("DOMICILIO" to params.address)
                }
            }
        }

        return list
    }

    // ─────────────────────────────────────────────────────────────
    // MRZ ZONE (Machine Readable Zone) — ICAO Doc 9303
    // ─────────────────────────────────────────────────────────────

    private fun drawMRZ(
        canvas: Canvas,
        params: DocumentParams,
        country: Countries.Country,
        template: Countries.Template,
        width: Int,
        height: Int
    ) {
        val mrzY = height - 60
        val mrzHeight = 55

        // MRZ background
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
        val bgPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawColor(Color.WHITE)

        // Redraw main content first, then MRZ on top
        // MRZ background strip
        val mrzRect = RectF(0f, mrzY.toFloat(), width.toFloat(), mrzY + mrzHeight.toFloat())
        canvas.drawRoundRect(mrzRect, 0f, 0f, Paint().apply {
            color = Color.BLACK
        })

        // Generate MRZ text per ICAO Doc 9303
        val mrzText1 = generateMRZLine1(params, country)
        val mrzText2 = generateMRZLine2(params, country)

        val mrzPaint = Paint().apply {
            color = Color.WHITE
            textSize = template.mrzFontSize * 1.8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }

        // Line 1
        canvas.drawText(mrzText1, 10f, mrzY + 20f, mrzPaint)
        // Line 2
        canvas.drawText(mrzText2, 10f, mrzY + 42f, mrzPaint)
    }

    private fun generateMRZLine1(params: DocumentParams, country: Countries.Country): String {
        val lastName = params.lastName.uppercase().replace(" ", "<").padEnd(39)
        val firstName = params.firstName.uppercase().replace(" ", "<").padEnd(39)
        val nameField = "$lastName<$firstName"

        // Truncate to 44 chars
        val truncated = nameField.take(44)

        // Document type: P for passport
        return "P<${country.code.uppercase()}${truncated}"
    }

    private fun generateMRZLine2(params: DocumentParams, country: Countries.Country): String {
        // Passport number + check digit + nationality + DOB + check digit + sex + optional
        val docNum = params.documentNumber.take(9).uppercase()
        val nationality = country.code.uppercase()
        val dob = formatDateMRZ(params.dob)
        val sex = if (params.sex.uppercase().startsWith("M")) "M" else "F"

        // Simple check digits (modulo 10)
        val numCheck = computeCheckDigit(docNum)
        val dobCheck = computeCheckDigit(dob)
        val overallCheck = computeCheckDigit("$docNum$nationality$dob$sex")

        val line = "$docNum${numCheck}0${nationality}${dob}${dobCheck}0${sex}0"
        return line.padEnd(44, '<')
    }

    private fun formatDateMRZ(dob: String): String {
        // Convert YYYY-MM-DD or DD/MM/YYYY to YYMMDD
        return try {
            val parts = dob.split("-")
            if (parts.size == 3) {
                val year = parts[0].substring(2)
                val month = parts[1]
                val day = parts[2]
                "$year$month$day"
            } else {
                "000101"
            }
        } catch (e: Exception) {
            "000101"
        }
    }

    private fun computeCheckDigit(input: String): Int {
        val weights = intArrayOf(7, 3, 1)
        var sum = 0
        for (i in input.indices) {
            val c = input[i]
            val val = if (c in '0'..'9') c - '0' else c - 'A' + 10
            sum += val * weights[i % 3]
        }
        return sum % 10
    }

    // ─────────────────────────────────────────────────────────────
    // BARCODE (simplified Code 128 / PDF417 / QR)
    // ─────────────────────────────────────────────────────────────

    private fun drawBarcode(
        canvas: Canvas,
        params: DocumentParams,
        template: Countries.Template,
        width: Int,
        height: Int
    ) {
        val barcodeType = template.barcodeType
        if (barcodeType == Countries.BarcodeType.NONE) return

        val barcodeX = width - 120
        val barcodeY = height - 120
        val barcodeW = 100
        val barcodeH = 100

        when (barcodeType) {
            Countries.BarcodeType.CODE128 -> drawCode128(canvas, params, barcodeX, barcodeY, barcodeW, barcodeH)
            Countries.BarcodeType.PDF417 -> drawPDF417(canvas, params, barcodeX, barcodeY, barcodeW, barcodeH)
            Countries.BarcodeType.QR_CODE -> drawQRCode(canvas, params, barcodeX, barcodeY, barcodeW, barcodeH)
            Countries.BarcodeType.NONE -> {}
        }
    }

    private fun drawCode128(
        canvas: Canvas,
        params: DocumentParams,
        x: Int,
        y: Int,
        w: Int,
        h: Int
    ) {
        // Simplified barcode representation (vertical bars)
        val data = params.documentNumber.toByteArray()
        val barWidth = w / (data.size * 3)
        var xPos = x.toFloat()

        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
        }
        val whitePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = false
        }

        for (byte in data) {
            val bitPattern = byte.toByte()
            for (bit in 0..7) {
                if (bitPattern and (1 shl bit) != 0) {
                    canvas.drawRect(xPos, y.toFloat(), xPos + barWidth * 2, (y + h).toFloat(), blackPaint)
                    xPos += barWidth * 2
                } else {
                    canvas.drawRect(xPos, y.toFloat(), xPos + barWidth, (y + h).toFloat(), whitePaint)
                    xPos += barWidth
                }
            }
        }

        // Label
        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("CODE128", x.toFloat(), (y + h + 12).toFloat(), labelPaint)
    }

    private fun drawPDF417(
        canvas: Canvas,
        params: DocumentParams,
        x: Int,
        y: Int,
        w: Int,
        h: Int
    ) {
        // Simplified PDF417 representation (stacked horizontal bars)
        val rows = 8
        val cols = 15
        val cellW = w / cols
        val cellH = h / rows

        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
        }
        val whitePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = false
        }

        val seed = params.documentNumber.hashCode()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val shouldFill = (seed + row * cols + col) % 3 != 0
                val rect = RectF(
                    (x + col * cellW).toFloat(),
                    (y + row * cellH).toFloat(),
                    (x + (col + 1) * cellW).toFloat(),
                    (y + (row + 1) * cellH).toFloat()
                )
                canvas.drawRoundRect(rect, 0f, 0f, if (shouldFill) blackPaint else whitePaint)
            }
        }

        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("PDF417", x.toFloat(), (y + h + 12).toFloat(), labelPaint)
    }

    private fun drawQRCode(
        canvas: Canvas,
        params: DocumentParams,
        x: Int,
        y: Int,
        w: Int,
        h: Int
    ) {
        // Simplified QR code representation (grid pattern)
        val gridSize = 21
        val cellSize = min(w, h) / gridSize

        val blackPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = false
        }
        val whitePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = false
        }

        val seed = params.documentNumber.hashCode()
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                // QR finder patterns (top-left, top-right, bottom-left)
                val isFinder = (row < 7 && col < 7) ||
                    (row < 7 && col >= gridSize - 7) ||
                    (row >= gridSize - 7 && col < 7)

                val shouldFill = if (isFinder) {
                    // Finder pattern: outer border + inner white + center
                    val localRow = if (row < 7) row else row - (gridSize - 7)
                    val localCol = if (col < 7) col else col - (gridSize - 7)
                    (localRow == 0 || localRow == 6 || localCol == 0 || localCol == 6 ||
                        (localRow >= 2 && localRow <= 4 && localCol >= 2 && localCol <= 4))
                } else {
                    (seed + row * gridSize + col) % 5 != 0
                }

                val rect = RectF(
                    (x + col * cellSize).toFloat(),
                    (y + row * cellSize).toFloat(),
                    (x + (col + 1) * cellSize).toFloat(),
                    (y + (row + 1) * cellSize).toFloat()
                )
                canvas.drawRoundRect(rect, 0f, 0f, if (shouldFill) blackPaint else whitePaint)
            }
        }

        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("QR CODE", x.toFloat(), (y + min(w, h) + 12).toFloat(), labelPaint)
    }

    // ─────────────────────────────────────────────────────────────
    // SECURITY FEATURES
    // ─────────────────────────────────────────────────────────────

    private fun drawSecurityFeatures(
        canvas: Canvas,
        template: Countries.Template,
        country: Countries.Country,
        width: Int,
        height: Int
    ) {
        // Watermark (faint country name in background)
        if (template.securityFeatures.contains(Countries.SecurityFeature.WATERMARK)) {
            val watermarkPaint = Paint().apply {
                color = Color.GRAY
                alpha = 30
                textSize = 80f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(country.name.uppercase(),
                width / 2f, height / 2f + 30, watermarkPaint)
        }

        // Hologram (colorful circle)
        if (template.securityFeatures.contains(Countries.SecurityFeature.HOLOGRAM)) {
            val hologramX = width - 40
            val hologramY = height - 40
            val hologramRadius = 15f

            // Simple hologram: concentric circles with gradient-like colors
            for (i in 5 downTo 0) {
                val r = hologramRadius - i * 2.5f
                val hue = (System.nanoTime() / 1000000 + i * 60) % 360
                val color = android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.8f, 0.9f))
                canvas.drawCircle(hologramX.toFloat(), hologramY.toFloat(), r, Paint().apply {
                    this.color = color
                    alpha = 128
                })
            }
        }

        // Microtext (tiny repeated text along bottom edge)
        if (template.securityFeatures.contains(Countries.SecurityFeature.MICROTEXT)) {
            val microtextPaint = Paint().apply {
                color = Color.GRAY
                textSize = 4f
                isAntiAlias = true
            }
            val microtext = "SECURE ${country.code} ${country.name.uppercase()} "
            val fullText = microtext.repeat(30)
            canvas.drawText(fullText, 0f, height - 5f, microtextPaint)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // EMBLEM / FLAG
    // ─────────────────────────────────────────────────────────────

    private fun drawEmblem(
        canvas: Canvas,
        flag: String,
        x: Float,
        y: Float,
        size: Float,
        paint: Paint
    ) {
        paint.textSize = size
        canvas.drawText(flag, x, y + size * 0.8f, paint)
    }

    // ─────────────────────────────────────────────────────────────
    // PASSPORT COVER (full cover page)
    // ─────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    fun generatePassportCover(countryCode: String): GeneratedDocument {
        val country = Countries.getCountry(countryCode)
            ?: throw IllegalArgumentException("Unknown country code: $countryCode")
        val template = country.template

        val width = 1080
        val height = 1700
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Cover background
        canvas.drawColor(template.passportCoverColor)

        // Country emblem/flag at top center
        val flagPaint = Paint().apply {
            color = Color.WHITE
            textSize = 120f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(country.flag, width / 2f, 400f, flagPaint)

        // "PASSPORT" text
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PASSPORT", width / 2f, 650f, titlePaint)

        // Country name
        val countryPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(country.name.uppercase(), width / 2f, 750f, countryPaint)

        // ICAO symbol (simplified)
        val icaoPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ICAO", width / 2f, 900f, icaoPaint)

        // Bottom text
        val bottomPaint = Paint().apply {
            color = Color.WHITE.alpha.shr(2)
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ELECTRONIC", width / 2f, height - 200f, bottomPaint)
        canvas.drawText("PASSPORT", width / 2f, height - 160f, bottomPaint)

        val dummyParams = DocumentParams(
            countryCode = countryCode,
            docType = Countries.DocumentType.PASSPORT,
            photoUri = null,
            firstName = "",
            lastName = "",
            dob = "",
            sex = "",
            nationality = country.name,
            documentNumber = "",
            issueDate = "",
            expiryDate = ""
        )

        return GeneratedDocument(bitmap, dummyParams, System.currentTimeMillis())
    }
}

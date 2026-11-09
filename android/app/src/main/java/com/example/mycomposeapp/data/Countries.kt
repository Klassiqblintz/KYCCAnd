package com.example.mycomposeapp.data

/**
 * All countries and their government ID templates.
 * Each country supports Passport, Driver License, and/or National ID Card.
 * Template specs define layout, colors, fonts, and required fields per country.
 */

object Countries {

    // ─────────────────────────────────────────────────────────────
    // ALL COUNTRIES (25+)
    // ─────────────────────────────────────────────────────────────

    data class Country(
        val code: String,           // ISO 3166-1 alpha-2
        val name: String,           // Display name
        val flag: String,           // Unicode flag emoji
        val supportedDocs: List<DocumentType>,
        val template: Template
    )

    enum class DocumentType {
        PASSPORT,
        DRIVER_LICENSE,
        NATIONAL_ID_CARD
    }

    data class Template(
        val passportCoverColor: Int,       // Background color for passport cover
        val passportDataPageBg: Int,       // Background color for data page
        val dlHeaderColor: Int,            // Header color for driver license
        val dlBgColor: Int,                // Background color for driver license
        val nidHeaderColor: Int,           // Header color for national ID
        val nidBgColor: Int,               // Background color for national ID
        val mrzFont: String,               // MRZ font (monospace)
        val barcodeType: BarcodeType,      // Barcode type per country
        val securityFeatures: List<SecurityFeature>,
        val requiredFields: List<IdField>,
        val photoPosition: PhotoPosition,
        val textLayout: TextLayout
    )

    enum class BarcodeType {
        CODE128,
        QR_CODE,
        PDF417,
        NONE
    }

    enum class SecurityFeature {
        HOLOGRAM,
        WATERMARK,
        GHOST_IMAGE,
        MICROTEXT,
        ULTRAVIOLET_MARKER,
        SECURE_PHOTO,
        GHOST_PHOTO
    }

    data class IdField(
        val key: String,
        val label: String,
        val required: Boolean,
        val maxLength: Int,
        val format: FieldFormat
    )

    enum class FieldFormat {
        TEXT,
        DATE,
        NUMBER,
        ALPHANUMERIC,
        BIRTHDATE,
        NATIVE_SCRIPT
    }

    data class PhotoPosition(
        val x: Float,           // Normalized position (0-1)
        val y: Float,           // Normalized position (0-1)
        val width: Float,       // Normalized width (0-1)
        val height: Float,      // Normalized height (0-1)
        val background: Int     // Background color for ID photo
    )

    data class TextLayout(
        val nameFontSize: Float,
        val namePosition: PointF,
        val fieldLabelFontSize: Float,
        val fieldFontSize: Float,
        val mrzFontSize: Float,
        val headerFontSize: Float
    )

    data class PointF(
        val x: Float,
        val y: Float
    )

    // ─────────────────────────────────────────────────────────────
    // COUNTRY DATA
    // ─────────────────────────────────────────────────────────────

    val ALL_COUNTRIES: List<Country> = listOf(
        // ── NORTH AMERICA ──
        Country(
            "US", "United States", "\uD83C\uDDFA\uD83C\uDDF8",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF1A237E.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF1A237E.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFB71C1C.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.GHOST_IMAGE,
                    SecurityFeature.MICROTEXT,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Last Name", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "Nationality", true, 30, FieldFormat.TEXT),
                    IdField("documentNumber", "Document Number", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Issue Date", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Expiry Date", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                    IdField("height", "Height", false, 10, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "CA", "Canada", "\uD83C\uDDE8\uD83C\uDDE6",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFD32F2F.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFFD32F2F.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFD32F2F.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.PDF417,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Given Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "Canadian", true, 10, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "MX", "Mexico", "\uD83C\uDDF2\uD83C\uDDFD",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF006847.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFF006847.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF006847.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Nombre", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Apellido", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Fecha de Nacimiento", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sexo", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "Mexicana", true, 10, FieldFormat.TEXT),
                    IdField("documentNumber", "No. de Pasaporte", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Fecha de Emisión", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Fecha de Vencimiento", true, 10, FieldFormat.DATE),
                    IdField("address", "Domicilio", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),

        // ── SOUTH AMERICA ──
        Country(
            "BR", "Brazil", "\uD83C\uDDE7\uD83C\uDDF7",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF009C3B.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFF009C3B.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF009C3B.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Nome", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Sobrenome", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Data de Nascimento", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sexo", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "BRASILEIRA", true, 15, FieldFormat.TEXT),
                    IdField("documentNumber", "Passaporte No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Emissão", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Validade", true, 10, FieldFormat.DATE),
                    IdField("address", "Endereço", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "AR", "Argentina", "\uD83C\uDDE6\uD83C\uDDF7",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF75AADB.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFF75AADB.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF75AADB.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Nombre", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Apellido", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Fecha de Nacimiento", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sexo", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "ARGENTINA", true, 10, FieldFormat.TEXT),
                    IdField("documentNumber", "Pasaporte N°", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Emisión", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Vencimiento", true, 10, FieldFormat.DATE),
                    IdField("address", "Domicilio", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "CO", "Colombia", "\uD83C\uDDE8\uD83C\uDDF4",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFFCD116.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFFFCD116.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFFCD116.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Nombre", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Apellido", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Fecha de Nacimiento", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sexo", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "COLOMBIANA", true, 12, FieldFormat.TEXT),
                    IdField("documentNumber", "Pasaporte No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Expedición", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Vencimiento", true, 10, FieldFormat.DATE),
                    IdField("address", "Dirección", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),

        // ── EUROPE ──
        Country(
            "UK", "United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF012169.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF012169.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF012169.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.PDF417,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.GHOST_IMAGE,
                    SecurityFeature.MICROTEXT,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "British Citizen", true, 15, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "DE", "Germany", "\uD83C\uDDE9\uD83C\uDDEA",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF000000.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFF000000.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF000000.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.PDF417,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.GHOST_IMAGE,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Vorname", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Nachname", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Geburtsdatum", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Geschlecht", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "DEUTSCH", true, 6, FieldFormat.TEXT),
                    IdField("documentNumber", "Passnummer", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Ausstellung", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Während", true, 10, FieldFormat.DATE),
                    IdField("address", "Adresse", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "FR", "France", "\uD83C\uDDEB\uD83C\uDDF7",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF002395.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFF002395.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF002395.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.PDF417,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Prénom", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Nom", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Date de Naissance", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sexe", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "FRANÇAISE", true, 9, FieldFormat.TEXT),
                    IdField("documentNumber", "N° de Passeport", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Délivré le", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Valable jusqu'au", true, 10, FieldFormat.DATE),
                    IdField("address", "Adresse", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "ES", "Spain", "\uD83C\uDDEA\uD83C\uDDF8",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFAA151B.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFFAA151B.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFAA151B.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Nombre", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Apellidos", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Fecha de Nacimiento", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sexo", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "ESPAÑOLA", true, 8, FieldFormat.TEXT),
                    IdField("documentNumber", "Pasaporte N°", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Fecha de Expedición", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Fecha de Vencimiento", true, 10, FieldFormat.DATE),
                    IdField("address", "Domicilio", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "IT", "Italy", "\uD83C\uDDEE\uD83C\uDDF9",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF008C45.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFF008C45.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF008C45.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Nome", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Cognome", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Data di Nascita", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sesso", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "ITALIANA", true, 7, FieldFormat.TEXT),
                    IdField("documentNumber", "Passaporto N.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Emesso il", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Valido entro il", true, 10, FieldFormat.DATE),
                    IdField("address", "Indirizzo", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "NL", "Netherlands", "\uD83C\uDDF3\uD83C\uDDF1",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFAE1C28.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFFAE1C28.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFAE1C28.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.PDF417,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Voornaam", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Achternaam", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Geboortedatum", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Geslacht", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "NEDERLandS", true, 12, FieldFormat.TEXT),
                    IdField("documentNumber", "Paspoortnummer", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Afgegeven op", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Geldig t/m", true, 10, FieldFormat.DATE),
                    IdField("address", "Adres", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "PL", "Poland", "\uD83C\uDDF5\uD83C\uDDFD",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFDC143C.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFFDC143C.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFDC143C.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Imię", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Nazwisko", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Data Urodzenia", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Płeć", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "POLSKA", true, 7, FieldFormat.TEXT),
                    IdField("documentNumber", "Numer Paszportu", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Wydany", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Ważny do", true, 10, FieldFormat.DATE),
                    IdField("address", "Adres", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),

        // ── ASIA ──
        Country(
            "PH", "Philippines", "\uD83C\uDDF5\uD83C\uDDF1",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF0038A8.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF0038A8.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF0038A8.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Last Name", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "FILIPINO", true, 7, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "IN", "India", "\uD83C\uDDEE\uD83C\uDDF3",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFFF9932.toInt(),
                passportDataPageBg = 0xFFFFFFFF.toInt(),
                dlHeaderColor = 0xFFFF9932.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFFF9932.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.QR_CODE,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Last Name", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "INDIAN", true, 5, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "CN", "China", "\uD83C\uDDE8\uD83C\uDDF3",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFDE2910.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFFDE2910.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFDE2910.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.QR_CODE,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Given Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "CHINESE", true, 6, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "JP", "Japan", "\uD83C\uDDEF\uD83C\uDDF5",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF0A2E5A.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF0A2E5A.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF0A2E5A.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.QR_CODE,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Given Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "JAPANESE", true, 7, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "KR", "South Korea", "\uD83C\uDDF0\uD83C\uDDF7",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF0033A0.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF0033A0.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF0033A0.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.QR_CODE,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Given Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "KOREAN", true, 5, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "TH", "Thailand", "\uD83C\uDDF9\uD83C\uDDED",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF082B67.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF082B67.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF082B67.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Last Name", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "THAI", true, 3, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "ID", "Indonesia", "\uD83C\uDDEE\uD83C\uDDE9",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF00462A.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF00462A.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF00462A.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.QR_CODE,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Nama Depan", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Nama Belakang", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Tanggal Lahir", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Jenis Kelamin", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "INDONESIA", true, 8, FieldFormat.TEXT),
                    IdField("documentNumber", "No. Paspor", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Tanggal Terbit", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Berlaku S/d", true, 10, FieldFormat.DATE),
                    IdField("address", "Alamat", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "VN", "Vietnam", "\uD83C\uDDFB\uD83C\uDDF3",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFDA0814.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFFDA0814.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFDA0814.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "Tên", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("lastName", "Họ", true, 50, FieldFormat.NATIVE_SCRIPT),
                    IdField("dob", "Ngày Sinh", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Giới Tính", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "VIETNAM", true, 6, FieldFormat.TEXT),
                    IdField("documentNumber", "Số Hộ Chiếu", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Ngày Cấp", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Ngày Hết Hạn", true, 10, FieldFormat.DATE),
                    IdField("address", "Địa Chỉ", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),

        // ── AFRICA ──
        Country(
            "NG", "Nigeria", "\uD83C\uDDF3\uD83C\uDDEC",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF008751.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF008751.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF008751.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.QR_CODE,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Last Name", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "NIGERIAN", true, 7, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "ZA", "South Africa", "\uD83C\uDDFF\uD83C\uDDE6",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF007749.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF007749.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF007749.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "SOUTH AFRICAN", true, 15, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "EG", "Egypt", "\uD83C\uDDE8\uD83C\uDDEC",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFFC8102E.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFFC8102E.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFFC8102E.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.CODE128,
                securityFeatures = listOf(
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Last Name", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "EGYPTIAN", true, 7, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),

        // ── OCEANIA ──
        Country(
            "AU", "Australia", "\uD83C\uDDE6\uD83C\uDDFA",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF00008B.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF00008B.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF00008B.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.PDF417,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "AUSTRALIAN", true, 9, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
        Country(
            "NZ", "New Zealand", "\uD83C\uDDF3\uD83C\uDDFF",
            listOf(DocumentType.PASSPORT, DocumentType.DRIVER_LICENSE, DocumentType.NATIONAL_ID_CARD),
            Template(
                passportCoverColor = 0xFF00008B.toInt(),
                passportDataPageBg = 0xFFF5F5F5.toInt(),
                dlHeaderColor = 0xFF00008B.toInt(),
                dlBgColor = 0xFFF5F5F5.toInt(),
                nidHeaderColor = 0xFF00008B.toInt(),
                nidBgColor = 0xFFF5F5F5.toInt(),
                mrzFont = "monospace",
                barcodeType = BarcodeType.PDF417,
                securityFeatures = listOf(
                    SecurityFeature.HOLOGRAM,
                    SecurityFeature.WATERMARK,
                    SecurityFeature.SECURE_PHOTO
                ),
                requiredFields = listOf(
                    IdField("firstName", "First Name", true, 50, FieldFormat.TEXT),
                    IdField("lastName", "Surname", true, 50, FieldFormat.TEXT),
                    IdField("dob", "Date of Birth", true, 10, FieldFormat.BIRTHDATE),
                    IdField("sex", "Sex", true, 10, FieldFormat.TEXT),
                    IdField("nationality", "NEW ZEALAND", true, 12, FieldFormat.TEXT),
                    IdField("documentNumber", "Passport No.", true, 20, FieldFormat.ALPHANUMERIC),
                    IdField("issueDate", "Date of Issue", true, 10, FieldFormat.DATE),
                    IdField("expiryDate", "Date of Expiry", true, 10, FieldFormat.DATE),
                    IdField("address", "Address", false, 100, FieldFormat.TEXT),
                ),
                PhotoPosition(0.15f, 0.25f, 0.25f, 0.35f, 0xFFFFFFFF.toInt()),
                TextLayout(16f, PointF(0.45f, 0.35f), 10f, 12f, 10f, 14f)
            )
        ),
    )

    // ─────────────────────────────────────────────────────────────
    // LOOKUP MAPS
    // ─────────────────────────────────────────────────────────────

    private val _countryMap = ALL_COUNTRIES.associateBy { it.code }

    fun getCountry(code: String): Country? = _countryMap[code]

    fun getAllCountryCodes(): List<String> = ALL_COUNTRIES.map { it.code }

    fun getAllCountryNames(): List<String> = ALL_COUNTRIES.map { "${it.flag} ${it.name}" }

    fun getSupportedDocTypes(countryCode: String): List<DocumentType> {
        return getCountry(countryCode)?.supportedDocs ?: emptyList()
    }

    fun getTemplate(countryCode: String): Template? {
        return getCountry(countryCode)?.template
    }
}

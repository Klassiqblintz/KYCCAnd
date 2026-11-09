package com.example.mycomposeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Key
import android.net.Uri
import com.example.mycomposeapp.data.Countries
import com.example.mycomposeapp.data.DocumentGenerator
import android.graphics.Bitmap
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val Gray = Color(0xFF888888)
private val Green = Color(0xFF00C853)
private val Blue = Color(0xFF2196F3)

@Composable
fun CreateDocScreen(
    onBack: () -> Unit,
    onDocumentGenerated: (Bitmap) -> Unit
) {
    val context = LocalContext.current

    // ── State ──
    var selectedCountryCode by remember { mutableStateOf("") }
    var countryDropdownOpen by remember { mutableStateOf(false) }
    var selectedDocType by remember { mutableStateOf("") }
    var docTypeDropdownOpen by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var docNumber by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf(2000) }
    var selectedMonth by remember { mutableStateOf(0) }
    var selectedDay by remember { mutableStateOf(1) }

    // Generate state
    var isGenerating by remember { mutableStateOf(false) }
    var generationStatus by remember { mutableStateOf("") }
    var generationError by remember { mutableStateOf<String?>(null) }

    // API Key state
    var replicateApiKey by remember { mutableStateOf("") }
    var geminiApiKey by remember { mutableStateOf("") }
    var showApiKeysSection by remember { mutableStateOf(false) }

    // Load saved API keys on first compose
    LaunchedEffect(Unit) {
        val (repKey, gemKey) = DocumentGenerator.loadApiKeys()
        replicateApiKey = repKey ?: ""
        geminiApiKey = gemKey ?: ""
    }

    // ── Photo picker ──
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        } else {
            Toast.makeText(context, "No photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Available countries / doc types ──
    val allCountries = Countries.getAllCountryCodes()
    val allCountryNames = Countries.getAllCountryNames()

    // ── Country selection ──
    val selectedCountryIndex = allCountries.indexOf(selectedCountryCode)

    // ── Doc types for selected country ──
    val availableDocTypes = Countries.getSupportedDocTypes(selectedCountryCode)

    // ── Generate document (cloud AI with fallback) ──
    fun generate() {
        // Validate
        if (selectedCountryCode.isEmpty()) {
            Toast.makeText(context, "Please select a country", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDocType.isEmpty()) {
            Toast.makeText(context, "Please select a document type", Toast.LENGTH_SHORT).show()
            return
        }
        if (firstName.trim().isEmpty() || lastName.trim().isEmpty()) {
            Toast.makeText(context, "First and Last name are required", Toast.LENGTH_SHORT).show()
            return
        }
        if (dob.isEmpty()) {
            Toast.makeText(context, "Date of birth is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (gender.isEmpty()) {
            Toast.makeText(context, "Gender is required", Toast.LENGTH_SHORT).show()
            return
        }

        isGenerating = true
        generationError = null
        generationStatus = "Loading template..."

        // Save API keys first
        DocumentGenerator.saveApiKeys(replicateApiKey.ifEmpty { null }, geminiApiKey.ifEmpty { null })

        // Run generation on background thread
        Thread {
            try {
                val docTypeEnum = when (selectedDocType) {
                    "PASSPORT" -> Countries.DocumentType.PASSPORT
                    "DRIVER_LICENSE" -> Countries.DocumentType.DRIVER_LICENSE
                    "NATIONAL_ID_CARD" -> Countries.DocumentType.NATIONAL_ID_CARD
                    else -> null
                }

                if (docTypeEnum == null) {
                    generationError = "Invalid document type"
                    isGenerating = false
                    return@Thread
                }

                val params = DocumentGenerator.DocumentParams(
                    countryCode = selectedCountryCode,
                    docType = docTypeEnum,
                    photoUri = photoUri,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    dob = dob,
                    sex = gender,
                    nationality = nationality.ifEmpty {
                        Countries.getCountry(selectedCountryCode)?.name?.uppercase() ?: "UNKNOWN"
                    },
                    documentNumber = docNumber.ifEmpty {
                        generateDocNumber(selectedCountryCode, docTypeEnum)
                    },
                    issueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    expiryDate = calculateExpiryDate(),
                    address = "",
                    height = ""
                )

                // Use BEST generation: template overlay → Replicate → Gemini → native Canvas
                val result = DocumentGenerator.generateDocumentBest(params, context)

                // Return to main thread
                (context as? android.app.Activity)?.runOnUiThread {
                    isGenerating = false
                    generationStatus = ""
                    onDocumentGenerated(result.bitmap)
                }
            } catch (e: Exception) {
                generationError = "Generation failed: ${e.message}"
                isGenerating = false
                generationStatus = ""
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // ── Save API keys ──
    fun saveApiKeys() {
        DocumentGenerator.saveApiKeys(
            if (replicateApiKey.isNotEmpty()) replicateApiKey else null,
            if (geminiApiKey.isNotEmpty()) geminiApiKey else null
        )
        Toast.makeText(context, "API keys saved", Toast.LENGTH_SHORT).show()
    }

    // ── UI ──
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }
                Text(
                    text = "Create Document",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── API Key Configuration Section ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Blue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { showApiKeysSection = !showApiKeysSection },
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Keys",
                                tint = Blue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Backend Configuration",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue
                            )
                        }
                        Icon(
                            imageVector = if (showApiKeysSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showApiKeysSection) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Replicate API Key
                        Text(
                            text = "Replicate API Key (Primary — SDXL/Flux)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BasicTextField(
                            value = replicateApiKey,
                            onValueChange = { replicateApiKey = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .border(1.dp, White, RoundedCornerShape(8.dp)),
                            textStyle = TextStyle(
                                color = White,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(White),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (replicateApiKey.isEmpty()) {
                                        Text(
                                            text = "r8_xxxxxxxx (get at replicate.com)",
                                            fontSize = 12.sp,
                                            color = Gray
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Text(
                            text = "✅ Best quality, $0.003-0.01 per image",
                            fontSize = 10.sp,
                            color = Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gemini API Key
                        Text(
                            text = "Google Gemini API Key (Fallback)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BasicTextField(
                            value = geminiApiKey,
                            onValueChange = { geminiApiKey = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .border(1.dp, White, RoundedCornerShape(8.dp)),
                            textStyle = TextStyle(
                                color = White,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(White),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (geminiApiKey.isEmpty()) {
                                        Text(
                                            text = "AIzaSy_xxxxxxxx (get at aistudio.google.com)",
                                            fontSize = 12.sp,
                                            color = Gray
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Text(
                            text = "🔄 Alternative AI if Replicate fails",
                            fontSize = 10.sp,
                            color = Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { saveApiKeys() },
                            colors = CardDefaults.cardColors(containerColor = Blue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Save API Keys",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Status indicator
                        if (DocumentGenerator.hasApiKeys()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Keys configured",
                                    tint = Green,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI backend active — template overlay + cloud generation enabled",
                                    fontSize = 11.sp,
                                    color = Green
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // A. Photo Upload Section
            Text(
                text = "Identity Photo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, if (photoUri != null) Green else White, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (photoUri != null) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Photo selected",
                            tint = Green,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Photo selected",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Add Photo",
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Photo (Optional)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }
            }
            // Clickable wrapper for photo picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable { photoPickerLauncher.launch("image/*") }
                    .offset(0.dp, (-100).dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // B. Document Information Section
            Text(
                text = "Document Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Country dropdown
                    DropdownField(
                        label = "Country *",
                        placeholder = "Select country",
                        value = if (selectedCountryIndex >= 0) allCountryNames[selectedCountryIndex] else "",
                        isExpanded = countryDropdownOpen,
                        onExpand = { countryDropdownOpen = true },
                        onClose = { countryDropdownOpen = false },
                        options = allCountryNames,
                        onOptionSelected = { index ->
                            selectedCountryCode = allCountries[index]
                            selectedDocType = ""
                            val country = Countries.getCountry(selectedCountryCode)
                            if (country != null) {
                                nationality = country.name.uppercase()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Document type dropdown
                    val docTypeOptions = availableDocTypes.map { it.name }
                    DropdownField(
                        label = "Document Type *",
                        placeholder = "Select document type",
                        value = selectedDocType,
                        isExpanded = docTypeDropdownOpen,
                        onExpand = { docTypeDropdownOpen = true },
                        onClose = { docTypeDropdownOpen = false },
                        options = docTypeOptions,
                        onOptionSelected = { index ->
                            selectedDocType = docTypeOptions[index]
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // First Name and Last Name (horizontal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FormField(
                            label = "First Name *",
                            placeholder = "Enter first name",
                            value = firstName,
                            onValueChange = { firstName = it },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FormField(
                            label = "Last Name *",
                            placeholder = "Enter last name",
                            value = lastName,
                            onValueChange = { lastName = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Date of Birth
                    DropdownField(
                        label = "Date of Birth *",
                        placeholder = "Select Date of Birth",
                        value = dob,
                        isExpanded = showDatePicker,
                        onExpand = { showDatePicker = true },
                        onClose = { showDatePicker = false },
                        options = listOf("Select Date"),
                        onOptionSelected = { _ ->
                            // Show date picker dialog
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender dropdown
                    DropdownField(
                        label = "Gender *",
                        placeholder = "Select Gender",
                        value = gender,
                        isExpanded = false,
                        onExpand = { /* handled by dropdown */ },
                        onClose = { },
                        options = listOf("Male", "Female", "Other"),
                        onOptionSelected = { index ->
                            gender = listOf("Male", "Female", "Other")[index]
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Document Number
                    FormField(
                        label = "Document Number",
                        placeholder = "Auto-generated if empty",
                        value = docNumber,
                        onValueChange = { docNumber = it },
                        modifier = Modifier
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Nationality (auto-filled from country)
                    FormField(
                        label = "Nationality",
                        placeholder = "Auto-filled from country",
                        value = nationality,
                        onValueChange = { nationality = it },
                        modifier = Modifier
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generation status
            if (generationStatus.isNotEmpty() && isGenerating) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Blue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.5.dp,
                            color = Blue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = generationStatus,
                            fontSize = 12.sp,
                            color = Blue
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Error message
            if (generationError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = generationError!!,
                        fontSize = 12.sp,
                        color = Color(0xFFFF5252),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // C. Generate Document Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, if (isGenerating) Gray else Green, RoundedCornerShape(12.dp))
                    .clickable { if (!isGenerating) generate() },
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Green
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generating...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Create Document",
                            tint = Green,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Document",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Date picker dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismiss = { showDatePicker = false },
                onDateSelected = { year, month, day ->
                    dob = String.format("%04d-%02d-%02d", year, month + 1, day)
                    showDatePicker = false
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DROPDOWN FIELD (replaces simple FormField for pickers)
// ─────────────────────────────────────────────────────────────

@Composable
private fun DropdownField(
    label: String,
    placeholder: String,
    value: String,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    options: List<String>,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(isExpanded) }

    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(1.dp, White, RoundedCornerShape(8.dp))
                .clickable {
                    if (!expanded) onExpand()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isNotEmpty()) value else placeholder,
                    fontSize = 12.sp,
                    color = if (value.isNotEmpty()) White else Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Dropdown(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, White, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)),
                expanded = expanded,
                onExpandedChange = { expanded = it; if (!it) onClose() },
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        onClick = {
                            onOptionSelected(index)
                            expanded = false
                            onClose()
                        },
                        content = {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                color = White
                            )
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TEXT FIELD (for free-form input)
// ─────────────────────────────────────────────────────────────

@Composable
private fun FormField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(1.dp, White, RoundedCornerShape(8.dp)),
            textStyle = TextStyle(
                color = White,
                fontSize = 12.sp
            ),
            cursorBrush = SolidColor(White),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 12.sp,
                            color = Gray
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// DATE PICKDIALOG
// ─────────────────────────────────────────────────────────────

@Composable
private fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    var year by remember { mutableStateOf(2000) }
    var month by remember { mutableStateOf(0) }
    var day by remember { mutableStateOf(1) }

    // Use Android's native DatePickerDialog
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    AndroidDatePickerDialog(
        initialYear = 2000,
        initialMonth = 0,
        initialDay = 1,
        onDateSet = { _, y, m, d ->
            year = y
            month = m
            day = d
            onDateSelected(y, m, d)
            onDismiss()
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun AndroidDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDateSet: (DatePicker, Int, Int, Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current as? android.app.Activity ?: return

    LaunchedEffect(Unit) {
        // Show the native date picker dialog
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                onDateSet(
                    android.widget.DatePicker(),
                    y,
                    m,
                    d
                )
            },
            initialYear,
            initialMonth,
            initialDay
        )
        datePickerDialog.show()
    }
}

// ─────────────────────────────────────────────────────────────
// HELPER FUNCTIONS
// ─────────────────────────────────────────────────────────────

private fun generateDocNumber(countryCode: String, docType: Countries.DocumentType): String {
    val random = Random()
    return when (docType) {
        Countries.DocumentType.PASSPORT -> "P${countryCode}${String.format("%08d", random.nextInt(100000000))}"
        Countries.DocumentType.DRIVER_LICENSE -> "DL${countryCode}${String.format("%08d", random.nextInt(100000000))}"
        Countries.DocumentType.NATIONAL_ID_CARD -> "ID${countryCode}${String.format("%08d", random.nextInt(100000000))}"
    }
}

private fun calculateExpiryDate(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.YEAR, 5) // 5-year expiry
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

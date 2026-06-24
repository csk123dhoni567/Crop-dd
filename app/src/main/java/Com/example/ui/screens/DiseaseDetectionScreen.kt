package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.CropAnalysisResult
import com.example.ui.viewmodel.CropViewModel
import com.example.ui.viewmodel.DetectionUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDetectionScreen(
    viewModel: CropViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.detectionUiState.collectAsState()

    // Temp file Uri for Camera
    val tempPhotoUri = remember {
        val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Helper to decode bitmap from uri
    val loadBitmapFromUri = { uri: Uri ->
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            selectedBitmap = bitmap
            selectedImageUri = uri
            viewModel.analyzeCropImage(bitmap, uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            loadBitmapFromUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            loadBitmapFromUri(tempPhotoUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Disease Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is DetectionUiState.Idle -> {
                    IdleUploadView(
                        onSelectGallery = { galleryLauncher.launch("image/*") },
                        onSelectCamera = { cameraLauncher.launch(tempPhotoUri) }
                    )
                }

                is DetectionUiState.Loading -> {
                    LoadingAnalysisView(imageUri = selectedImageUri)
                }

                is DetectionUiState.Success -> {
                    AnalysisSuccessView(
                        result = state.result,
                        imageUriString = state.imageUri,
                        onScanAgain = {
                            selectedImageUri = null
                            selectedBitmap = null
                            viewModel.resetDetectionState()
                        }
                    )
                }

                is DetectionUiState.Error -> {
                    AnalysisErrorView(
                        message = state.message,
                        onRetry = {
                            val bitmap = selectedBitmap
                            val uri = selectedImageUri
                            if (bitmap != null) {
                                viewModel.analyzeCropImage(bitmap, uri)
                            } else {
                                viewModel.resetDetectionState()
                            }
                        },
                        onBack = {
                            viewModel.resetDetectionState()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun IdleUploadView(
    onSelectGallery: () -> Unit,
    onSelectCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic illustration icon
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan Frame",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "AI Crop Disease Diagnostics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Capture or upload an image of a crop leaf. AgroScan AI will automatically analyze the leaf to detect diseases, predict infection levels, and recommend remedies.",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Drag & drop clickable card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clickable { onSelectGallery() }
                .testTag("upload_image_box"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery Upload",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Upload Leaf Photo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Supports JPEG, PNG up to 15MB",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "— OR —",
            fontWeight = FontWeight.Bold,
            color = Color.Gray.copy(alpha = 0.6f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSelectCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("take_photo_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take Leaf Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun LoadingAnalysisView(imageUri: Uri?) {
    val loadingPhrases = listOf(
        "Scanning leaf contours...",
        "Identifying crop family...",
        "Analyzing pigment anomalies...",
        "Evaluating lesion distribution...",
        "Consulting Agro-intelligence database...",
        "Formulating eco-friendly cures..."
    )
    var currentPhraseIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2500)
            currentPhraseIndex = (currentPhraseIndex + 1) % loadingPhrases.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageUri != null) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Leaf Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Scanner animation overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analyzing Crop Leaf...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = loadingPhrases[currentPhraseIndex],
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "loading_phrases"
        ) { text ->
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnalysisSuccessView(
    result: CropAnalysisResult,
    imageUriString: String?,
    onScanAgain: () -> Unit
) {
    var expandedTab by remember { mutableStateOf("Description") }
    val isHealthy = result.disease.equals("Healthy", ignoreCase = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Leaf Image & Disease Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column {
                    if (imageUriString != null) {
                        AsyncImage(
                            model = Uri.parse(imageUriString),
                            contentDescription = "Diagnosed Leaf",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.crop.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )

                            // Status Chip
                            val chipBg = if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            val chipContent = if (isHealthy) Color(0xFF2E7D32) else Color(0xFFC62828)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(chipBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isHealthy) "Healthy" else "Infected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = chipContent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = result.disease,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confidence Meter
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val confidenceAnim by animateFloatAsState(
                                targetValue = (result.confidence / 100f).toFloat(),
                                animationSpec = tween(1200)
                            )
                            LinearProgressIndicator(
                                progress = { confidenceAnim },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50.dp)),
                                color = if (isHealthy) Color(0xFF81C784) else Color(0xFFF06292),
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${result.confidence.toInt()}% Match",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Tab Navigation for Detailed Recommendations
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("Description", "Symptoms", "Causes", "Treatment", "Prevention")
                tabs.forEach { tab ->
                    val isSelected = expandedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { expandedTab = tab },
                        label = { Text(tab, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Expanded Tab Content Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (expandedTab) {
                        "Description" -> {
                            Text(
                                text = result.description,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        "Symptoms" -> {
                            BulletList(items = result.symptoms, iconColor = Color(0xFFE57373))
                        }

                        "Causes" -> {
                            BulletList(items = result.causes, iconColor = Color(0xFFFFB74D))
                        }

                        "Treatment" -> {
                            BulletList(items = result.treatment, iconColor = Color(0xFF81C784))
                        }

                        "Prevention" -> {
                            BulletList(items = result.prevention, iconColor = Color(0xFF64B5F6))
                        }
                    }
                }
            }
        }

        // Top Alternative Predictions Section
        if (result.topPredictions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ailment Probability Mapping",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        result.topPredictions.forEach { prediction ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(prediction.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("${prediction.confidence.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val scale by animateFloatAsState(
                                    targetValue = (prediction.confidence / 100f).toFloat(),
                                    animationSpec = tween(1200)
                                )
                                LinearProgressIndicator(
                                    progress = { scale },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50.dp)),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                OutlinedButton(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan Again")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BulletList(items: List<String>, iconColor: Color) {
    if (items.isEmpty()) {
        Text("No specific suggestions available for this category.", fontSize = 13.sp, color = Color.Gray)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, end = 12.dp)
                            .size(8.dp)
                            .background(iconColor, CircleShape)
                    )
                    Text(
                        text = item,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error Logo",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analysis Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go Back")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Analysis")
            }
        }
    }
}

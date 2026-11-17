package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.StoreConfig
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandCrimsonDark
import com.example.ui.theme.SacredGold
import com.example.ui.theme.TileBlue
import com.example.ui.theme.TileGreen
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareScreen(
    storeConfig: StoreConfig,
    isAdmin: Boolean,
    isBusy: Boolean,
    onSaveConfig: (downloadUrl: String, headline: String) -> Unit,
    onNotify: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlDraft by rememberSaveable(storeConfig.downloadUrl) { mutableStateOf(storeConfig.downloadUrl) }
    var headlineDraft by rememberSaveable(storeConfig.headline) { mutableStateOf(storeConfig.headline) }
    var sharingPoster by remember { mutableStateOf(false) }
    var sharingApk by remember { mutableStateOf(false) }

    val normalizedDraft = remember(urlDraft) { StoreConfig.normalizeApkDownloadUrl(urlDraft) }
    val savedNormalizedUrl = remember(storeConfig.downloadUrl) {
        StoreConfig.normalizeApkDownloadUrl(storeConfig.downloadUrl)
    }

    val activeUrl = when {
        isAdmin && (normalizedDraft.startsWith("https://") || normalizedDraft.startsWith("http://")) ->
            normalizedDraft
        savedNormalizedUrl.startsWith("https://") || savedNormalizedUrl.startsWith("http://") ->
            savedNormalizedUrl
        else -> ""
    }
    val activeHeadline = when {
        isAdmin && headlineDraft.isNotBlank() -> headlineDraft.trim()
        storeConfig.headline.isNotBlank() -> storeConfig.headline.trim()
        else -> StoreConfig.DEFAULT_HEADLINE
    }
    val qrBitmap = remember(activeUrl) {
        if (activeUrl.isNotBlank()) generateQrBitmap(activeUrl, 640) else null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QR સ્કેનર • Play Store વગર",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("qr_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "પાછા જાઓ",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandCrimson)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Direct APK Share Card (Works offline / via WhatsApp without Play Store or hosting!)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F8FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = TileBlue,
                                shape = CircleShape,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Android,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Play Store વગર સીધી APK ફાઈલ મોકલો",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D47A1)
                                )
                                Text(
                                    text = "દુકાને આવેલા ગ્રાહકને WhatsApp / Bluetooth / Quick Share થી તરત એપ મોકલી શકાય છે.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2C3E50)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    sharingApk = true
                                    val apkUri = createShareableApkUri(context)
                                    sharingApk = false
                                    if (apkUri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/vnd.android.package-archive"
                                            putExtra(Intent.EXTRA_STREAM, apkUri)
                                            putExtra(
                                                Intent.EXTRA_SUBJECT,
                                                "મહાદેવ ટ્રેડર્સ - અનાજ ભાવ એપ (APK)"
                                            )
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "મહાદેવ ટ્રેડર્સ (હળવદ) - લાઈવ અનાજ ભાવ એપ ઇન્સ્ટોલ કરો."
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "APK ફાઈલ શેર કરો")
                                        )
                                    } else {
                                        onNotify("APK ફાઈલ તૈયાર થઈ શકી નહીં")
                                    }
                                }
                            },
                            enabled = !sharingApk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("share_direct_apk_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TileBlue)
                        ) {
                            if (sharingApk) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "APK ફાઈલ શેર કરો (WhatsApp / Quick Share)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 2. Scannable Shop QR Poster Card (for Google Drive / Direct APK Link)
                if (activeUrl.isNotBlank() && qrBitmap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, SacredGold.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .border(1.5.dp, SacredGold, CircleShape),
                                    shape = CircleShape,
                                    color = Color(0xFFF0E9E0)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.mahadev_logo),
                                        contentDescription = "મહાદેવ ટ્રેડર્સ લોગો",
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "મહાદેવ ટ્રેડર્સ",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = BrandCrimsonDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "અનાજ કરીયાણા • હળવદ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6B5B53)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = activeHeadline,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF211612),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(250.dp)
                                    .border(1.dp, Color(0xFFE0D5C5), RoundedCornerShape(18.dp))
                                    .padding(12.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "મહાદેવ ટ્રેડર્સ એપ ડાઉનલોડ QR કોડ",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("qr_code_image")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "કેમેરા / Google Lens થી QR સ્કેન કરી APK ડાઉનલોડ કરો",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandCrimsonDark,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B5B53),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Share Poster PNG & Copy/Share Link Buttons
                    Button(
                        onClick = {
                            scope.launch {
                                sharingPoster = true
                                val uri = createShareableQrPosterUri(context, activeUrl, activeHeadline)
                                sharingPoster = false
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "$activeHeadline\nડાયરેક્ટ ડાઉનલોડ લિંક: $activeUrl"
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "QR પોસ્ટર શેર / પ્રિન્ટ કરો"))
                                } else {
                                    onNotify("QR પોસ્ટર તૈયાર કરવામાં ભૂલ આવી")
                                }
                            }
                        },
                        enabled = !sharingPoster,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("share_qr_poster_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandCrimson)
                    ) {
                        if (sharingPoster) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "દુકાન QR પોસ્ટર શેર / પ્રિન્ટ કરો",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Mahadev Traders APK URL", activeUrl))
                                onNotify("ડાઉનલોડ લિંક કોપી થઈ ગઈ!")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("copy_qr_link_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("લિંક કોપી કરો")
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "મહાદેવ ટ્રેડર્સ - અનાજ ભાવ એપ ડાઉનલોડ કરો (Play Store વગર):\n$activeUrl"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "એપ લિંક શેર કરો"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_text_link_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("લિંક શેર કરો")
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = null,
                                tint = BrandCrimson,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "દુકાનનો QR કોડ બનાવવા APK લિંક મૂકો",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isAdmin) {
                                    "ઉપરના બટનથી APK ફાઈલ તમારા Google Drive માં અપલોડ કરો અને તેની લિંક નીચે પેસ્ટ કરો — તરત જ પ્રિન્ટ કરવા લાયક QR સ્કેનર બની જશે!"
                                } else {
                                    "એડમિન દ્વારા હજુ ડાયરેક્ટ APK QR લિંક સેટ કરવાની બાકી છે. ત્યાં સુધી ઉપરના બટનથી સીધી APK ફાઈલ શેર કરી શકો છો."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 3. Admin-Only Google Drive / Direct APK URL Editor
                if (isAdmin) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Play Store વગર QR કોડ સેટિંગ (ફક્ત એડમિન)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandCrimsonDark
                            )
                            Text(
                                text = "૧) ઉપરથી 'APK ફાઈલ શેર કરો' વડે APK તમારા Google Drive માં સેવ કરો (અથવા વેબસાઈટ પર મૂકો).\n" +
                                    "૨) Google Drive માં 'Anyone with the link' ચાલુ કરી લિંક કોપી કરો.\n" +
                                    "૩) નીચે પેસ્ટ કરી સેવ કરો — Google Drive લિંક આપમેળે ડાયરેક્ટ ડાઉનલોડ QR કોડમાં ફેરવાઈ જશે!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = urlDraft,
                                onValueChange = { urlDraft = it.take(500) },
                                label = { Text("Google Drive / ડાયરેક્ટ APK લિંક *") },
                                placeholder = { Text("https://drive.google.com/file/d/... અથવા https://.../mahadev.apk") },
                                singleLine = false,
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("qr_url_input")
                            )

                            if (normalizedDraft.isNotBlank() && normalizedDraft != urlDraft.trim()) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "ડાયરેક્ટ ડાઉનલોડ લિંક તૈયાર:\n$normalizedDraft",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1B5E20),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = headlineDraft,
                                onValueChange = { headlineDraft = it.take(100) },
                                label = { Text("QR પોસ્ટર મથાળું") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("qr_headline_input")
                            )

                            Button(
                                onClick = { onSaveConfig(normalizedDraft, headlineDraft) },
                                enabled = !isBusy && normalizedDraft.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_qr_config_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TileGreen)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("નવો QR કોડ સેવ કરો", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix.get(x, y)) 0xFF1A110E.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    } catch (_: Exception) {
        null
    }
}

private suspend fun createShareableApkUri(context: Context): Uri? = withContext(Dispatchers.IO) {
    try {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        val sourceApk = File(appInfo.sourceDir)
        if (!sourceApk.exists()) return@withContext null

        val shareDir = File(context.cacheDir, "share_apk").apply { mkdirs() }
        val targetApk = File(shareDir, "MahadevTraders.apk")
        FileInputStream(sourceApk).use { input ->
            FileOutputStream(targetApk).use { output ->
                input.copyTo(output, bufferSize = 32 * 1024)
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetApk)
    } catch (_: Exception) {
        null
    }
}

private suspend fun createShareableQrPosterUri(
    context: Context,
    url: String,
    headline: String
): Uri? = withContext(Dispatchers.IO) {
    try {
        val width = 1200
        val height = 1680
        val poster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(poster)

        // Cream background & gold/crimson border
        canvas.drawColor(0xFFF6F2EA.toInt())
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC62828.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 18f
        }
        canvas.drawRoundRect(RectF(28f, 28f, width - 28f, height - 28f), 48f, 48f, outerBorderPaint)

        val goldBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB8860B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRoundRect(RectF(46f, 46f, width - 46f, height - 46f), 38f, 38f, goldBorderPaint)

        // Top crimson header band
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC62828.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(62f, 64f, width - 62f, 300f), 36f, 36f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("મહાદેવ ટ્રેડર્સ", width / 2f, 168f, titlePaint)

        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF3E0.toInt()
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("અનાજ કરીયાણાના રીટેઈલ તથા હોલસેલ વેપારી • હળવદ", width / 2f, 240f, subHeaderPaint)

        // Logo badge
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.mahadev_logo)
        if (logo != null) {
            val logoRect = Rect(width / 2 - 105, 330, width / 2 + 105, 540)
            canvas.drawBitmap(logo, null, logoRect, null)
            logo.recycle()
        }

        // Headline
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF211612.toInt()
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(headline.take(40), width / 2f, 620f, headlinePaint)

        // White QR Card + High-Contrast QR Bitmap
        val qrContainerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(210f, 670f, 990f, 1400f), 36f, 36f, qrContainerPaint)
        val qrBmp = generateQrBitmap(url, 640)
        if (qrBmp != null) {
            canvas.drawBitmap(qrBmp, null, Rect(280, 715, 920, 1355), null)
            qrBmp.recycle()
        }

        // Crimson pill badge (no Play Store wording)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC62828.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(140f, 1440f, width - 140f, 1516f), 38f, 38f, pillPaint)

        val scanNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("કેમેરા અથવા Google Lens થી QR કોડ સ્કેન કરો", width / 2f, 1490f, scanNotePaint)

        val subFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3E2723.toInt()
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("સીધો Android એપ ડાઉનલોડ • ફક્ત મોબાઈલ નંબર વડે લોગીન", width / 2f, 1572f, subFooterPaint)

        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF7A6A62.toInt()
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(url.take(68), width / 2f, 1618f, urlPaint)

        val shareDir = File(context.cacheDir, "share_qr").apply { mkdirs() }
        val file = File(shareDir, "mahadev_traders_qr.png")
        FileOutputStream(file).use { out ->
            poster.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        poster.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}

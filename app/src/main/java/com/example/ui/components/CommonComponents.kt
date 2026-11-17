package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.GrainIconKey
import com.example.data.model.StockStatus
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.StockAvailableBg
import com.example.ui.theme.StockAvailableGreen
import com.example.ui.theme.StockLowAmber
import com.example.ui.theme.StockLowBg
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun SearchBarField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "શોધો",
                tint = BrandCrimson
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.testTag("${testTag}_clear")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "સાફ કરો",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = BrandCrimson,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
    )
}

@Composable
fun StockStatusBadge(status: StockStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        StockStatus.AVAILABLE -> StockAvailableBg to StockAvailableGreen
        StockStatus.LOW -> StockLowBg to StockLowAmber
        StockStatus.OUT -> StockOutBg to StockOutRed
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GrainArtworkBox(
    iconKey: String,
    imageBase64: String?,
    grainName: String,
    size: Dp = 54.dp,
    cornerRadius: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imageBase64) { decodeBase64ToBitmap(imageBase64) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = grainName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(cornerRadius))
        )
    } else {
        val style = grainVisualStyle(GrainIconKey.fromKey(iconKey))
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(style.bgStart, style.bgEnd)
                    )
                )
                .border(1.dp, style.accent.copy(alpha = 0.25f), RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = grainName,
                tint = style.accent,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
}

@Composable
fun EmptyCatalogNotice(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionButton: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Grain,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(16.dp))
            actionButton()
        }
    }
}

private data class GrainVisualStyle(
    val bgStart: Color,
    val bgEnd: Color,
    val accent: Color,
    val icon: ImageVector
)

private fun grainVisualStyle(key: GrainIconKey): GrainVisualStyle {
    return when (key) {
        GrainIconKey.WHEAT -> GrainVisualStyle(Color(0xFFFFF3D6), Color(0xFFF6DCA2), Color(0xFFA66B00), Icons.Rounded.Grass)
        GrainIconKey.RICE -> GrainVisualStyle(Color(0xFFF3F6FA), Color(0xFFE1E8F2), Color(0xFF476072), Icons.Rounded.Grain)
        GrainIconKey.BAJRA -> GrainVisualStyle(Color(0xFFEFECE6), Color(0xFFDAD2C5), Color(0xFF5C4E3A), Icons.Rounded.Spa)
        GrainIconKey.JOWAR -> GrainVisualStyle(Color(0xFFF9EFE6), Color(0xFFEEDAC8), Color(0xFF8C5327), Icons.Rounded.Grain)
        GrainIconKey.CHANA -> GrainVisualStyle(Color(0xFFFFF0D9), Color(0xFFF7D8A8), Color(0xFF9C5B00), Icons.Rounded.Eco)
        GrainIconKey.DAL -> GrainVisualStyle(Color(0xFFFFF7CC), Color(0xFFFBE788), Color(0xFF9C7A00), Icons.Rounded.LocalDining)
        GrainIconKey.MOONG -> GrainVisualStyle(Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFF2E7D32), Icons.Rounded.Eco)
        GrainIconKey.MAIZE -> GrainVisualStyle(Color(0xFFFFF8DC), Color(0xFFFCE38A), Color(0xFFC27D00), Icons.Rounded.Grass)
        GrainIconKey.OIL -> GrainVisualStyle(Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFD84315), Icons.Rounded.WaterDrop)
        GrainIconKey.SPICES -> GrainVisualStyle(Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFB71C1C), Icons.Rounded.Spa)
        GrainIconKey.SUGAR -> GrainVisualStyle(Color(0xFFF3E5F5), Color(0xFFE1BEE7), Color(0xFF6A1B9A), Icons.Rounded.Inventory2)
        GrainIconKey.OTHER -> GrainVisualStyle(Color(0xFFEDE7DF), Color(0xFFD7CCC0), Color(0xFF5D4037), Icons.Rounded.Inventory2)
    }
}

fun decodeBase64ToBitmap(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

suspend fun encodeUriToCompressedBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@withContext null

        val maxSide = 220
        val scale = minOf(
            maxSide.toFloat() / rawBitmap.width.coerceAtLeast(1),
            maxSide.toFloat() / rawBitmap.height.coerceAtLeast(1),
            1f
        )
        val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
        val resized = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)

        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, out)
        if (resized != rawBitmap) resized.recycle()
        rawBitmap.recycle()

        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        if (encoded.length <= 95_000) encoded else null
    } catch (_: Exception) {
        null
    }
}

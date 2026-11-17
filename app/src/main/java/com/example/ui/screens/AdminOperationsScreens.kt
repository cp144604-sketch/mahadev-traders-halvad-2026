package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.DefaultStoreCatalog
import com.example.data.model.GrainIconKey
import com.example.data.model.GrainItem
import com.example.data.model.LoginLog
import com.example.data.model.StockStatus
import com.example.data.model.UiState
import com.example.data.model.formatPriceRupees
import com.example.data.model.formatTimestampGu
import com.example.ui.components.EmptyCatalogNotice
import com.example.ui.components.GrainArtworkBox
import com.example.ui.components.SearchBarField
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.encodeUriToCompressedBase64
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.StockAvailableBg
import com.example.ui.theme.StockAvailableGreen
import com.example.ui.theme.StockLowBg
import com.example.ui.theme.StockLowAmber
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutRed
import com.example.ui.theme.TileGreen
import com.example.ui.theme.TileIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGrainScreen(
    editingGrain: GrainItem?,
    isBusy: Boolean,
    onSave: (
        existingId: String?,
        name: String,
        category: String,
        priceText: String,
        priceUnit: String,
        weight: String,
        stockStatus: StockStatus,
        notes: String,
        iconKey: String,
        imageBase64: String?
    ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancel)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(editingGrain?.id) { mutableStateOf(editingGrain?.name ?: "") }
    var category by rememberSaveable(editingGrain?.id) { mutableStateOf(editingGrain?.category ?: "અનાજ") }
    var priceText by rememberSaveable(editingGrain?.id) {
        mutableStateOf(editingGrain?.let { formatPriceRupees(it.pricePerKg) } ?: "")
    }
    var priceUnit by rememberSaveable(editingGrain?.id) { mutableStateOf(editingGrain?.priceUnit ?: "Kg") }
    var weight by rememberSaveable(editingGrain?.id) { mutableStateOf(editingGrain?.weight ?: "50 Kg (બેગ)") }
    var stockStatus by rememberSaveable(editingGrain?.id) {
        mutableStateOf(editingGrain?.stockEnum() ?: StockStatus.AVAILABLE)
    }
    var notes by rememberSaveable(editingGrain?.id) { mutableStateOf(editingGrain?.notes ?: "") }
    var iconKey by rememberSaveable(editingGrain?.id) { mutableStateOf(editingGrain?.iconKey ?: GrainIconKey.WHEAT.key) }
    var imageBase64 by rememberSaveable(editingGrain?.id) { mutableStateOf(editingGrain?.imageBase64) }
    var photoBusy by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                photoBusy = true
                val encoded = encodeUriToCompressedBase64(context, uri)
                if (encoded != null) {
                    imageBase64 = encoded
                }
                photoBusy = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingGrain == null) "નવું અનાજ ઉમેરો" else "અનાજ સુધારો",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("add_edit_back_button")) {
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
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Photo Picker Box matching mockup
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("pick_grain_photo_box"),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (photoBusy) {
                            CircularProgressIndicator(color = BrandCrimson)
                        } else if (!imageBase64.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                GrainArtworkBox(
                                    iconKey = iconKey,
                                    imageBase64 = imageBase64,
                                    grainName = name,
                                    size = 96.dp,
                                    cornerRadius = 16.dp
                                )
                                Column {
                                    Text("ફોટો પસંદ થયો છે", fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { imageBase64 = null }) {
                                        Text("ફોટો દૂર કરો", color = BrandCrimson)
                                    }
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.AddAPhoto,
                                    contentDescription = "ફોટો ઉમેરો",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ફોટો ઉમેરો",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Built-in icon picker
                Text("અનાજ આઈકોન પસંદ કરો", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GrainIconKey.entries) { iconItem ->
                        val selected = iconKey == iconItem.key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) BrandCrimson else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    iconKey = iconItem.key
                                    if (editingGrain == null) category = iconItem.defaultCategory
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                GrainArtworkBox(
                                    iconKey = iconItem.key,
                                    imageBase64 = null,
                                    grainName = iconItem.labelGu,
                                    size = 38.dp,
                                    cornerRadius = 10.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(iconItem.labelGu, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("અનાજનું નામ *") },
                    placeholder = { Text("અહીં નામ લખો (જેમ કે ઘઉં)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grain_name_input")
                )

                Text("કેટેગરી", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DefaultStoreCatalog.editableCategories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandCrimson,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { input ->
                            priceText = input.filter { it.isDigit() || it == '.' }.take(10)
                        },
                        label = { Text("ભાવ (₹ / $priceUnit) *") },
                        placeholder = { Text("અહીં ભાવ લખો") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grain_price_input")
                    )
                }

                Text("ભાવનો એકમ", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DefaultStoreCatalog.unitOptions) { unit ->
                        FilterChip(
                            selected = priceUnit == unit,
                            onClick = { priceUnit = unit },
                            label = { Text(unit) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandCrimson,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.take(60) },
                    label = { Text("વજન") },
                    placeholder = { Text("જેમ કે 50 Kg (બેગ)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grain_weight_input")
                )

                Text("સ્ટોક સ્થિતિ", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StockStatus.entries.forEach { st ->
                        FilterChip(
                            selected = stockStatus == st,
                            onClick = { stockStatus = st },
                            label = { Text(st.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (st) {
                                    StockStatus.AVAILABLE -> StockAvailableGreen
                                    StockStatus.LOW -> StockLowAmber
                                    StockStatus.OUT -> StockOutRed
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(300) },
                    label = { Text("નોંધ (વૈકલ્પિક)") },
                    placeholder = { Text("ગુણવત્તા કે વિશેષ માહિતી") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grain_notes_input")
                )

                Button(
                    onClick = {
                        onSave(
                            editingGrain?.id,
                            name,
                            category,
                            priceText,
                            priceUnit,
                            weight,
                            stockStatus,
                            notes,
                            iconKey,
                            imageBase64
                        )
                    },
                    enabled = !isBusy && !photoBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_grain_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TileGreen)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text("સેવ કરો", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("cancel_grain_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("રદ કરો", color = BrandCrimson, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPriceUpdateScreen(
    grainsState: UiState<List<GrainItem>>,
    searchQuery: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onUpdatePrice: (grainId: String, newPrice: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    AdminSearchableGrainScaffold(
        title = "ભાવ અપડેટ કરો",
        searchQuery = searchQuery,
        searchPlaceholder = "ભાવ બદલવા અનાજ શોધો...",
        searchTestTag = "price_update_search",
        grainsState = grainsState,
        onSearchChange = onSearchChange,
        onBack = onBack,
        modifier = modifier
    ) { grain ->
        var priceDraft by remember(grain.id, grain.pricePerKg) {
            mutableStateOf(formatPriceRupees(grain.pricePerKg))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GrainArtworkBox(grain.iconKey, grain.imageBase64, grain.name, size = 48.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(grain.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "હાલનો ભાવ: ₹ ${formatPriceRupees(grain.pricePerKg)} / ${grain.priceUnit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandCrimson
                    )
                }
                OutlinedTextField(
                    value = priceDraft,
                    onValueChange = { input ->
                        priceDraft = input.filter { it.isDigit() || it == '.' }.take(9)
                    },
                    prefix = { Text("₹") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .width(108.dp)
                        .testTag("price_field_${grain.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onUpdatePrice(grain.id, priceDraft) },
                    enabled = !isBusy,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TileGreen),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("save_price_${grain.id}")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "સેવ કરો")
                }
            }
        }
    }
}

@Composable
fun QuickWeightUpdateScreen(
    grainsState: UiState<List<GrainItem>>,
    searchQuery: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onUpdateWeight: (grainId: String, newWeight: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    AdminSearchableGrainScaffold(
        title = "વજન અપડેટ કરો",
        searchQuery = searchQuery,
        searchPlaceholder = "વજન બદલવા અનાજ શોધો...",
        searchTestTag = "weight_update_search",
        grainsState = grainsState,
        onSearchChange = onSearchChange,
        onBack = onBack,
        modifier = modifier
    ) { grain ->
        var weightDraft by remember(grain.id, grain.weight) { mutableStateOf(grain.weight) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GrainArtworkBox(grain.iconKey, grain.imageBase64, grain.name, size = 44.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(grain.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "હાલનું વજન: ${grain.weight}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weightDraft,
                        onValueChange = { weightDraft = it.take(60) },
                        label = { Text("નવું વજન") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onUpdateWeight(grain.id, weightDraft) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TileGreen),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("સેવ")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStockUpdateScreen(
    grainsState: UiState<List<GrainItem>>,
    searchQuery: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onUpdateStock: (grainId: String, status: StockStatus) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    AdminSearchableGrainScaffold(
        title = "સ્ટોક અપડેટ કરો",
        searchQuery = searchQuery,
        searchPlaceholder = "સ્ટોક બદલવા અનાજ શોધો...",
        searchTestTag = "stock_update_search",
        grainsState = grainsState,
        onSearchChange = onSearchChange,
        onBack = onBack,
        modifier = modifier
    ) { grain ->
        val currentStatus = grain.stockEnum()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrainArtworkBox(grain.iconKey, grain.imageBase64, grain.name, size = 44.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(grain.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    StockStatusBadge(status = currentStatus)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StockStatus.entries.forEach { st ->
                        val selected = currentStatus == st
                        FilterChip(
                            selected = selected,
                            enabled = !isBusy,
                            onClick = { onUpdateStock(grain.id, st) },
                            label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (st) {
                                    StockStatus.AVAILABLE -> StockAvailableBg
                                    StockStatus.LOW -> StockLowBg
                                    StockStatus.OUT -> StockOutBg
                                },
                                selectedLabelColor = when (st) {
                                    StockStatus.AVAILABLE -> StockAvailableGreen
                                    StockStatus.LOW -> StockLowAmber
                                    StockStatus.OUT -> StockOutRed
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteGrainScreen(
    grainsState: UiState<List<GrainItem>>,
    searchQuery: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onDeleteGrain: (grainId: String, name: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var pendingDelete by remember { mutableStateOf<GrainItem?>(null) }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("અનાજ ડિલીટ કરો?") },
            text = { Text("શું તમે ખરેખર '${target.name}' યાદીમાંથી કાયમ માટે દૂર કરવા માંગો છો?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGrain(target.id, target.name)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCrimson)
                ) {
                    Text("હા, ડિલીટ કરો")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("રદ કરો")
                }
            }
        )
    }

    AdminSearchableGrainScaffold(
        title = "અનાજ ડિલીટ કરો",
        searchQuery = searchQuery,
        searchPlaceholder = "ડિલીટ કરવા અનાજ શોધો...",
        searchTestTag = "delete_grain_search",
        grainsState = grainsState,
        onSearchChange = onSearchChange,
        onBack = onBack,
        modifier = modifier
    ) { grain ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GrainArtworkBox(grain.iconKey, grain.imageBase64, grain.name, size = 48.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(grain.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "₹ ${formatPriceRupees(grain.pricePerKg)} / ${grain.priceUnit} • ${grain.weight}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { pendingDelete = grain },
                    enabled = !isBusy,
                    modifier = Modifier.testTag("delete_grain_${grain.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = "${grain.name} ડિલીટ કરો",
                        tint = BrandCrimson
                    )
                }
            }
        }
    }
}

@Composable
fun AdminGrainListScreen(
    grainsState: UiState<List<GrainItem>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEditGrain: (GrainItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    AdminSearchableGrainScaffold(
        title = "અનાજ યાદી જુઓ",
        searchQuery = searchQuery,
        searchPlaceholder = "અનાજ યાદીમાં શોધો...",
        searchTestTag = "admin_list_search",
        grainsState = grainsState,
        onSearchChange = onSearchChange,
        onBack = onBack,
        modifier = modifier
    ) { grain ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onEditGrain(grain) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GrainArtworkBox(grain.iconKey, grain.imageBase64, grain.name, size = 50.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(grain.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "₹ ${formatPriceRupees(grain.pricePerKg)} / ${grain.priceUnit} • ${grain.weight}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandCrimson
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StockStatusBadge(status = grain.stockEnum())
                }
                IconButton(
                    onClick = { onEditGrain(grain) },
                    modifier = Modifier.testTag("edit_grain_${grain.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "સુધારો",
                        tint = BrandCrimson
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginLogsScreen(
    logsState: UiState<List<LoginLog>>,
    searchQuery: String,
    roleFilter: String,
    onSearchChange: (String) -> Unit,
    onRoleFilterChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("લોગીન થયેલા ગ્રાહકો", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("login_logs_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "પાછા જાઓ", tint = Color.White)
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
                .widthIn(max = 760.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SearchBarField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "નામ અથવા મોબાઈલ નંબર શોધો...",
                testTag = "login_logs_search",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("બધા", "ગ્રાહકો", "એડમિન").forEach { filter ->
                    FilterChip(
                        selected = roleFilter == filter,
                        onClick = { onRoleFilterChange(filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TileIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            when (logsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandCrimson)
                    }
                }

                is UiState.Error -> {
                    EmptyCatalogNotice(
                        title = "લોગીન વિગતો લોડ થઈ શકી નહીં",
                        subtitle = logsState.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is UiState.Success -> {
                    val q = searchQuery.trim().lowercase()
                    val filtered = logsState.data.filter { log ->
                        val matchesRole = when (roleFilter) {
                            "ગ્રાહકો" -> log.loginRole == "customer"
                            "એડમિન" -> log.loginRole == "admin"
                            else -> true
                        }
                        val matchesQuery = q.isEmpty() ||
                            log.mobileNumber.contains(q) ||
                            log.customerName.lowercase().contains(q) ||
                            (log.email?.lowercase()?.contains(q) == true)
                        matchesRole && matchesQuery
                    }

                    val uniqueNumbers = filtered.map { it.mobileNumber }.distinct().size
                    Text(
                        text = "કુલ એન્ટ્રી: ${filtered.size}  •  અલગ મોબાઈલ નંબર: $uniqueNumbers",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    if (filtered.isEmpty()) {
                        EmptyCatalogNotice(
                            title = "કોઈ લોગીન રેકોર્ડ મળ્યો નથી",
                            subtitle = "જ્યારે કોઈ ગ્રાહક મોબાઈલ નંબરથી લોગીન કરશે ત્યારે અહીં તરત જ દેખાશે.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = if (log.loginRole == "admin") Color(0xFFFFEBEE) else Color(0xFFE8EAF6),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PhoneAndroid,
                                                    contentDescription = null,
                                                    tint = if (log.loginRole == "admin") BrandCrimson else TileIndigo
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = log.customerName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    color = if (log.loginRole == "admin") Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                                    shape = RoundedCornerShape(999.dp)
                                                ) {
                                                    Text(
                                                        text = if (log.loginRole == "admin") "એડમિન" else "ગ્રાહક",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (log.loginRole == "admin") BrandCrimson else StockAvailableGreen,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "મોબાઈલ: ${log.mobileNumber}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BrandCrimson
                                            )
                                            if (!log.email.isNullOrBlank()) {
                                                Text(
                                                    text = log.email,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "સમય: ${formatTimestampGu(log.loginAt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSearchableGrainScaffold(
    title: String,
    searchQuery: String,
    searchPlaceholder: String,
    searchTestTag: String,
    grainsState: UiState<List<GrainItem>>,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (GrainItem) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("${searchTestTag}_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "પાછા જાઓ", tint = Color.White)
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
                .widthIn(max = 760.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SearchBarField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = searchPlaceholder,
                testTag = searchTestTag,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (grainsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandCrimson)
                    }
                }

                is UiState.Error -> {
                    EmptyCatalogNotice(
                        title = "માહિતી લોડ થઈ શકી નહીં",
                        subtitle = grainsState.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is UiState.Success -> {
                    val q = searchQuery.trim().lowercase()
                    val filtered = grainsState.data.filter { grain ->
                        q.isEmpty() ||
                            grain.name.lowercase().contains(q) ||
                            grain.category.lowercase().contains(q) ||
                            grain.weight.lowercase().contains(q)
                    }
                    if (filtered.isEmpty()) {
                        EmptyCatalogNotice(
                            title = "કોઈ અનાજ મળ્યું નથી",
                            subtitle = "શોધ શબ્દ બદલીને ફરી પ્રયાસ કરો અથવા નવું અનાજ ઉમેરો.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { grain ->
                                itemContent(grain)
                            }
                        }
                    }
                }
            }
        }
    }
}

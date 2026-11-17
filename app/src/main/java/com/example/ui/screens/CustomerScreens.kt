package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DefaultStoreCatalog
import com.example.data.model.GrainItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.formatPriceRupees
import com.example.data.model.formatTimestampGu
import com.example.ui.components.EmptyCatalogNotice
import com.example.ui.components.GrainArtworkBox
import com.example.ui.components.SearchBarField
import com.example.ui.components.StockStatusBadge
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.BrandCrimsonDark
import com.example.ui.theme.SacredGold
import com.example.ui.theme.StockAvailableGreen

@Composable
fun HomeCatalogScreen(
    grainsState: UiState<List<GrainItem>>,
    watchlistIds: Set<String>,
    userProfile: UserProfile?,
    searchQuery: String,
    selectedCategory: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onGrainClick: (GrainItem) -> Unit,
    onToggleWatchlist: (GrainItem) -> Unit,
    onOpenCustomerProfile: () -> Unit,
    onOpenQrShare: () -> Unit,
    onSignOutGoogle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Branded Mahadev Traders Top Header
        Surface(
            color = BrandCrimson,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color(0xFFF0E9E0)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mahadev_logo),
                                contentDescription = "મહાદેવ ટ્રેડર્સ",
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "મહાદેવ ટ્રેડર્સ",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF69F0AE))
                                )
                                Text(
                                    text = "લાઈવ અનાજ ભાવ • હળવદ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenQrShare,
                            modifier = Modifier.testTag("home_qr_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = "એપ ડાઉનલોડ QR કોડ",
                                tint = Color.White
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { onOpenCustomerProfile() }
                                .testTag("customer_profile_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneAndroid,
                                    contentDescription = "મોબાઈલ નંબર બદલો",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = userProfile?.mobileNumber?.takeIf { it.isNotBlank() } ?: "લોગીન",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = onSignOutGoogle,
                            modifier = Modifier.testTag("home_sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "લોગઆઉટ",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            SearchBarField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "અનાજ શોધો... (જેમ કે ઘઉં, ચોખા, દાળ)",
                testTag = "home_search_input",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DefaultStoreCatalog.categories) { cat ->
                    val selected = cat == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { onCategoryChange(cat) },
                        label = {
                            Text(
                                text = cat,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandCrimson,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("category_chip_$cat")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (grainsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandCrimson)
                    }
                }

                is UiState.Error -> {
                    EmptyCatalogNotice(
                        title = "ભાવ લોડ થઈ શક્યા નથી",
                        subtitle = grainsState.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is UiState.Success -> {
                    val filtered = grainsState.data.filter { grain ->
                        val matchesCategory = selectedCategory == "બધા" || grain.category == selectedCategory
                        val q = searchQuery.trim().lowercase()
                        val matchesQuery = q.isEmpty() ||
                            grain.name.lowercase().contains(q) ||
                            grain.category.lowercase().contains(q) ||
                            grain.weight.lowercase().contains(q)
                        matchesCategory && matchesQuery
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "અનાજની યાદી",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${filtered.size} વસ્તુઓ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (filtered.isEmpty()) {
                        EmptyCatalogNotice(
                            title = if (grainsState.data.isEmpty()) "હજુ અનાજની યાદી ખાલી છે" else "કોઈ અનાજ મળ્યું નહીં",
                            subtitle = if (grainsState.data.isEmpty()) {
                                "એડમિન પેનલમાંથી અનાજ અને ભાવ ઉમેરો એટલે અહીં તરત જ દેખાશે."
                            } else {
                                "બીજું નામ અથવા કેટેગરી પસંદ કરીને ફરી શોધો."
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 6.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { grain ->
                                GrainRowCard(
                                    grain = grain,
                                    isSaved = grain.id in watchlistIds,
                                    onClick = { onGrainClick(grain) },
                                    onToggleSave = { onToggleWatchlist(grain) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyListScreen(
    grainsState: UiState<List<GrainItem>>,
    watchlistIds: Set<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onGrainClick: (GrainItem) -> Unit,
    onToggleWatchlist: (GrainItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = BrandCrimson,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "મારી યાદી",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "તમારા મનપસંદ અનાજના તાજા લાઈવ ભાવ",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            SearchBarField(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "મારી યાદીમાં શોધો...",
                testTag = "my_list_search_input",
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
                        title = "મારી યાદી લોડ થઈ શકી નહીં",
                        subtitle = grainsState.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is UiState.Success -> {
                    val savedItems = grainsState.data.filter { grain ->
                        val inList = grain.id in watchlistIds
                        val q = searchQuery.trim().lowercase()
                        inList && (q.isEmpty() || grain.name.lowercase().contains(q) || grain.category.lowercase().contains(q))
                    }

                    if (savedItems.isEmpty()) {
                        EmptyCatalogNotice(
                            title = "તમારી યાદી ખાલી છે",
                            subtitle = "હોમ સ્ક્રીન પર કોઈપણ અનાજની બાજુમાં બુકમાર્ક આઈકોન દબાવીને તેને તમારી યાદીમાં સાચવો.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(savedItems, key = { it.id }) { grain ->
                                GrainRowCard(
                                    grain = grain,
                                    isSaved = true,
                                    onClick = { onGrainClick(grain) },
                                    onToggleSave = { onToggleWatchlist(grain) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrainRowCard(
    grain: GrainItem,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("grain_card_${grain.id}"),
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
            GrainArtworkBox(
                iconKey = grain.iconKey,
                imageBase64 = grain.imageBase64,
                grainName = grain.name,
                size = 56.dp,
                cornerRadius = 14.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grain.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "₹ ${formatPriceRupees(grain.pricePerKg)} / ${grain.priceUnit}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandCrimson
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StockStatusBadge(status = grain.stockEnum())
                    Text(
                        text = grain.weight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.testTag("bookmark_${grain.id}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (isSaved) "યાદીમાંથી દૂર કરો" else "યાદીમાં સાચવો",
                    tint = if (isSaved) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "વિગત જુઓ",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    grain: GrainItem,
    isSaved: Boolean,
    onToggleWatchlist: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ઉત્પાદન વિગત",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_top_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "પાછા જાઓ",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleWatchlist,
                        modifier = Modifier.testTag("detail_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "મારી યાદીમાં ઉમેરો",
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
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GrainArtworkBox(
                            iconKey = grain.iconKey,
                            imageBase64 = grain.imageBase64,
                            grainName = grain.name,
                            size = 156.dp,
                            cornerRadius = 24.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailInfoRow(label = "નામ", value = grain.name, valueColor = MaterialTheme.colorScheme.onSurface, isLarge = true)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        DetailInfoRow(
                            label = "ભાવ",
                            value = "₹ ${formatPriceRupees(grain.pricePerKg)} / ${grain.priceUnit}",
                            valueColor = BrandCrimsonDark,
                            isLarge = true
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        DetailInfoRow(label = "વજન", value = grain.weight, valueColor = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Column {
                            Text(
                                text = "સ્ટોક",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            StockStatusBadge(status = grain.stockEnum())
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        DetailInfoRow(label = "કેટેગરી", value = grain.category, valueColor = MaterialTheme.colorScheme.onSurface)
                        if (!grain.notes.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            DetailInfoRow(label = "નોંધ", value = grain.notes, valueColor = MaterialTheme.colorScheme.onSurface)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        DetailInfoRow(
                            label = "છેલ્લું અપડેટ",
                            value = formatTimestampGu(grain.updatedAt ?: grain.createdAt),
                            valueColor = StockAvailableGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("detail_back_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCrimson)
                ) {
                    Text(
                        text = "પાછા જાઓ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color,
    isLarge: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = if (isLarge) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

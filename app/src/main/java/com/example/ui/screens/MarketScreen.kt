package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartEntry
import com.example.data.model.FarmerReview
import com.example.data.model.ListingWithMeta
import com.example.data.model.ProduceCategory
import com.example.data.model.ProduceListing
import com.example.data.model.PublicProfile
import com.example.data.model.SplitSettlementSummary
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRole
import com.example.data.model.formatInr
import com.example.data.model.formatKg
import com.example.ui.components.CropImageDisplay
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NearbyFarmsMapCard
import com.example.ui.components.StarRatingBadge
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.HarvestForest
import com.example.ui.theme.StarAmber
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    copy: LocalizedCopy,
    userProfile: UserPrivateProfile,
    listingsState: UiState<List<ProduceListing>>,
    filteredListings: List<ListingWithMeta>,
    allReviews: List<FarmerReview>,
    publicProfiles: Map<String, PublicProfile>,
    searchQuery: String,
    selectedCategory: ProduceCategory?,
    radiusKm: Double?,
    cart: Map<String, CartEntry>,
    splitSummary: SplitSettlementSummary,
    isBusy: Boolean,
    mapOnlyMode: Boolean = false,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (ProduceCategory?) -> Unit,
    onRadiusSelected: (Double?) -> Unit,
    onAddToCart: (ProduceListing) -> Unit,
    onDecrementCart: (String) -> Unit,
    onCheckout: (deliveryAddress: String, onDone: () -> Unit) -> Unit,
    onNavigateToAccount: () -> Unit
) {
    var showMapInline by remember(mapOnlyMode) { mutableStateOf(mapOnlyMode) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var inspectedFarmerId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("market_listings_list"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (cart.isNotEmpty()) 112.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!mapOnlyMode) {
                item {
                    MarketEditorialHero(
                        copy = copy,
                        city = userProfile.city,
                        radiusKm = radiusKm,
                        listingCount = filteredListings.size,
                        showMapInline = showMapInline,
                        onToggleMap = { showMapInline = !showMapInline }
                    )
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Nearby Farms & Ratings Map",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Inspect farm distances around ${userProfile.city} and tap any marker for directions or direct ordering.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search + Proximity Radius pills
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = { Text("Search crops, farms, or villages…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val radiusOptions = listOf(10.0 to "10 km", 25.0 to "25 km", 50.0 to "50 km", null to "All India")
                        items(radiusOptions) { (value, label) ->
                            FilterChip(
                                selected = radiusKm == value,
                                onClick = { onRadiusSelected(value) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("radius_chip_${value?.toInt() ?: "all"}")
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { onCategorySelected(null) },
                                label = { Text("All Crops") }
                            )
                        }
                        items(ProduceCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    onCategorySelected(if (selectedCategory == cat) null else cat)
                                },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }

            if (showMapInline || mapOnlyMode) {
                item {
                    NearbyFarmsMapCard(
                        userLat = userProfile.latitude,
                        userLng = userProfile.longitude,
                        radiusKm = radiusKm,
                        listings = filteredListings,
                        onSelectListing = { inspectedFarmerId = it.listing.farmerId }
                    )
                }
            }

            // Top Rated Farmers Strip in Map Mode
            if (mapOnlyMode) {
                val farmers = publicProfiles.values
                    .filter { it.roleEnum() == UserRole.FARMER || it.farmName.isNotBlank() }
                    .sortedByDescending { it.averageRating }
                if (farmers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Verified Local Farmers",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    items(farmers, key = { "farmer_${it.userId}" }) { farmer ->
                        FarmerDirectoryCard(
                            farmer = farmer,
                            onClick = { inspectedFarmerId = farmer.userId }
                        )
                    }
                }
            }

            // Produce listings stream state
            when (listingsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Storefront,
                            title = "Could not load harvests",
                            subtitle = listingsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredListings.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Agriculture,
                                title = "No fresh harvests in this radius yet",
                                subtitle = if (radiusKm != null) {
                                    "Try widening your radius to All India, or switch to the Farmer role in Account to publish the first live harvest batch!"
                                } else {
                                    "Be the first! Switch to the Farmer role in Account to publish a fresh crop batch directly to the marketplace."
                                },
                                action = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (radiusKm != null) {
                                            OutlinedButton(onClick = { onRadiusSelected(null) }) {
                                                Text("Show All Regions")
                                            }
                                        }
                                        Button(
                                            onClick = onNavigateToAccount,
                                            modifier = Modifier.testTag("empty_market_switch_farmer")
                                        ) {
                                            Text("Open Account")
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        items(filteredListings, key = { it.listing.id }) { item ->
                            val qtyInCart = cart[item.listing.id]?.quantityKg ?: 0.0
                            ProduceListingCard(
                                copy = copy,
                                item = item,
                                quantityInCart = qtyInCart,
                                onAdd = { onAddToCart(item.listing) },
                                onDecrement = { onDecrementCart(item.listing.id) },
                                onInspectFarmer = { inspectedFarmerId = item.listing.farmerId }
                            )
                        }
                    }
                }
            }
        }

        // Persistent Floating Split-Settlement Cart Bar
        if (cart.isNotEmpty()) {
            val totalItemsKg = cart.values.sumOf { it.quantityKg }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { showCheckoutSheet = true }
                    .testTag("open_cart_checkout_bar"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "Cart",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Column {
                            Text(
                                text = "${cart.size} crop${if (cart.size > 1) "s" else ""} · ${formatKg(totalItemsKg)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Farmer gets ${formatInr(splitSummary.farmerDirectPayout)} direct",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "${formatInr(splitSummary.grandTotal)} · Checkout",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // Farmer Profile & Verified Reviews Bottom Sheet
    val targetFarmerId = inspectedFarmerId
    if (targetFarmerId != null) {
        val farmerProfile = publicProfiles[targetFarmerId]
        val farmerReviews = allReviews.filter { it.farmerId == targetFarmerId }
        ModalBottomSheet(
            onDismissRequest = { inspectedFarmerId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FarmerProfileReviewsSheetContent(
                farmerProfile = farmerProfile,
                reviews = farmerReviews
            )
        }
    }

    // Atomic Split-Settlement Checkout Bottom Sheet
    if (showCheckoutSheet && cart.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CheckoutBottomSheetContent(
                copy = copy,
                cartEntries = cart.values.toList(),
                defaultAddress = userProfile.defaultAddress.ifBlank { "Market Road, ${userProfile.city}" },
                summary = splitSummary,
                isBusy = isBusy,
                onIncrement = onAddToCart,
                onDecrement = onDecrementCart,
                onConfirm = { addr ->
                    onCheckout(addr) { showCheckoutSheet = false }
                }
            )
        }
    }
}

@Composable
private fun MarketEditorialHero(
    copy: LocalizedCopy,
    city: String,
    radiusKm: Double?,
    listingCount: Int,
    showMapInline: Boolean,
    onToggleMap: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_harvest_hero),
                contentDescription = "Direct farm harvest market",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                HarvestForest.copy(alpha = 0.88f),
                                HarvestForest.copy(alpha = 0.56f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$city · ${radiusKm?.let { "${it.toInt()} km" } ?: "All"} ($listingCount)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onToggleMap)
                            .testTag("toggle_market_map_button"),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = "Toggle map",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (showMapInline) "Hide Map" else "Map View",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = copy.heroTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = copy.heroSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun ProduceListingCard(
    copy: LocalizedCopy,
    item: ListingWithMeta,
    quantityInCart: Double,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
    onInspectFarmer: () -> Unit
) {
    val listing = item.listing
    val savingsPct = listing.middlemanSavingsPercent()
    val avgRating = item.farmerProfile?.averageRating ?: 0.0
    val reviewCount = item.farmerProfile?.reviewCount ?: 0L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("produce_card_${listing.id}"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
            ) {
                CropImageDisplay(
                    photoBase64 = listing.photoBase64,
                    category = listing.categoryEnum(),
                    contentDescription = listing.cropName,
                    modifier = Modifier.fillMaxSize()
                )
                if (savingsPct > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = "Save $savingsPct% · No Middleman",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = "${item.distanceKm} km · ${listing.city}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = listing.cropName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Harvested ${listing.harvestDate} · ${formatKg(listing.availableQtyKg)} available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${formatInr(listing.pricePerKg)}/kg",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (listing.marketComparePricePerKg > listing.pricePerKg) {
                            Text(
                                text = "Retail ${formatInr(listing.marketComparePricePerKg)}/kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }

                if (listing.description.isNotBlank()) {
                    Text(
                        text = listing.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Interactive Farmer Profile + Live Verified Rating row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onInspectFarmer)
                        .testTag("farmer_profile_row_${listing.farmerId}"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified farmer",
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = listing.farmName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "by ${listing.farmerName} · Tap for verified reviews",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        StarRatingBadge(
                            averageRating = avgRating,
                            reviewCount = reviewCount
                        )
                    }
                }

                // Add to Cart / Quantity Stepper row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = copy.directFarmPrice,
                        style = MaterialTheme.typography.labelMedium,
                        color = VerifiedEmerald,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (quantityInCart <= 0.0) {
                        Button(
                            onClick = onAdd,
                            modifier = Modifier.testTag("add_to_cart_${listing.id}"),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${copy.addToCart} 1 kg")
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = onDecrement,
                                modifier = Modifier.testTag("dec_cart_${listing.id}")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Remove 1 kg")
                            }
                            Text(
                                text = formatKg(quantityInCart),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalIconButton(
                                onClick = onAdd,
                                enabled = quantityInCart < listing.availableQtyKg,
                                modifier = Modifier.testTag("inc_cart_${listing.id}")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add 1 kg")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FarmerDirectoryCard(
    farmer: PublicProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Agriculture,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = farmer.farmName.ifBlank { farmer.displayName },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${farmer.displayName} · ${farmer.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StarRatingBadge(
                averageRating = farmer.averageRating,
                reviewCount = farmer.reviewCount
            )
        }
    }
}

@Composable
private fun FarmerProfileReviewsSheetContent(
    farmerProfile: PublicProfile?,
    reviews: List<FarmerReview>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = farmerProfile?.farmName?.ifBlank { farmerProfile.displayName } ?: "Local Farm",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${farmerProfile?.displayName ?: "Verified Farmer"} · ${farmerProfile?.city ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StarRatingBadge(
                averageRating = farmerProfile?.averageRating ?: 0.0,
                reviewCount = farmerProfile?.reviewCount ?: 0L
            )
        }

        HorizontalDivider()

        Text(
            text = "Verified Purchase Reviews (${reviews.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (reviews.isEmpty()) {
            Text(
                text = "No verified reviews yet. Consumers can rate this farmer once their delivered order is completed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reviews, key = { it.id }) { review ->
                    VerifiedReviewCard(review = review)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun VerifiedReviewCard(review: FarmerReview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.consumerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(review.rating.toInt().coerceIn(1, 5)) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Verified purchase",
                    tint = VerifiedEmerald,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Verified Purchase · ${review.cropSummary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = VerifiedEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CheckoutBottomSheetContent(
    copy: LocalizedCopy,
    cartEntries: List<CartEntry>,
    defaultAddress: String,
    summary: SplitSettlementSummary,
    isBusy: Boolean,
    onIncrement: (ProduceListing) -> Unit,
    onDecrement: (String) -> Unit,
    onConfirm: (String) -> Unit
) {
    var deliveryAddress by remember(defaultAddress) { mutableStateOf(defaultAddress) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Direct Farm Checkout",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Inventory is locked atomically during checkout to prevent overselling, with transparent direct farmer split payouts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        cartEntries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.listing.cropName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${entry.listing.farmName} · ${formatInr(entry.listing.pricePerKg)}/kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onDecrement(entry.listing.id) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                    }
                    Text(formatKg(entry.quantityKg), fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { onIncrement(entry.listing) },
                        enabled = entry.quantityKg < entry.listing.availableQtyKg
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                    }
                }
                Text(
                    text = formatInr(entry.lineTotal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OutlinedTextField(
            value = deliveryAddress,
            onValueChange = { deliveryAddress = it },
            label = { Text("Drop-off Delivery Address") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_address_input")
        )

        com.example.ui.components.SplitPaymentBreakdownCard(summary = summary)

        Button(
            onClick = { onConfirm(deliveryAddress) },
            enabled = !isBusy && deliveryAddress.trim().length >= 3,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("confirm_checkout_button"),
            shape = MaterialTheme.shapes.large
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "${copy.checkoutNow} · ${formatInr(summary.grandTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.FarmerReview
import com.example.data.model.MarketplaceOrder
import com.example.data.model.ProduceCategory
import com.example.data.model.ProduceListing
import com.example.data.model.PublicProfile
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRole
import com.example.data.model.formatInr
import com.example.data.model.formatKg
import com.example.data.util.CompressedImageResult
import com.example.data.util.compressCropPhotoFromUri
import com.example.data.util.decodeBase64Bitmap
import com.example.ui.components.CropImageDisplay
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.StarAmber
import com.example.ui.theme.VerifiedEmerald
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerHubScreen(
    copy: LocalizedCopy,
    userProfile: UserPrivateProfile,
    myPublicProfile: PublicProfile?,
    ownListings: List<ProduceListing>,
    incomingOrders: List<MarketplaceOrder>,
    myReviews: List<FarmerReview>,
    isBusy: Boolean,
    onPublishListing: (
        cropName: String,
        category: ProduceCategory,
        pricePerKg: Double,
        comparePricePerKg: Double,
        availableQtyKg: Double,
        harvestDate: String,
        description: String,
        photoBase64: String?,
        onDone: () -> Unit
    ) -> Unit,
    onUpdateListing: (listingId: String, pricePerKg: Double, availableQtyKg: Double) -> Unit,
    onDeleteListing: (String) -> Unit,
    onSwitchToFarmerRole: () -> Unit
) {
    var showPublishSheet by remember { mutableStateOf(false) }
    var editingListing by remember { mutableStateOf<ProduceListing?>(null) }

    val totalDirectPayout = incomingOrders.sumOf { it.farmerPayoutAmount }
    val avgRating = myPublicProfile?.averageRating ?: 0.0
    val reviewCount = myPublicProfile?.reviewCount ?: myReviews.size.toLong()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("farmer_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = userProfile.farmName.ifBlank { copy.navFarmerHub },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Direct Farm-Gate Console · ${userProfile.city} · 96% direct payout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (userProfile.roleEnum() != UserRole.FARMER) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Agriculture,
                    title = "Farmer RBAC Mode Required",
                    subtitle = "Your account is currently in ${userProfile.roleEnum().label} mode. Only verified Farmer accounts can publish or edit crop listings.",
                    action = {
                        Button(
                            onClick = onSwitchToFarmerRole,
                            modifier = Modifier.testTag("farmer_hub_switch_role_button")
                        ) {
                            Text("Activate Farmer Workspace")
                        }
                    }
                )
            }
        } else {
            // Simplified Icon-Driven KPI Tiles
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FarmerKpiCard(
                            icon = Icons.Filled.Payments,
                            label = "Direct Farm Payout",
                            value = formatInr(totalDirectPayout),
                            modifier = Modifier.weight(1f)
                        )
                        FarmerKpiCard(
                            icon = Icons.Filled.Star,
                            label = "Verified Rating",
                            value = if (reviewCount > 0L) String.format("%.1f ★ (%d)", avgRating, reviewCount) else "New Farm",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FarmerKpiCard(
                            icon = Icons.Filled.Inventory2,
                            label = "Live Batches",
                            value = "${ownListings.count { it.availableQtyKg > 0.0 }}",
                            modifier = Modifier.weight(1f)
                        )
                        FarmerKpiCard(
                            icon = Icons.Filled.Agriculture,
                            label = "Direct Orders",
                            value = "${incomingOrders.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Large Farmer-Friendly Publish Harvest CTA
            item {
                Button(
                    onClick = { showPublishSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("publish_harvest_button"),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = copy.publishHarvest,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Live Inventory Section
            item {
                Text(
                    text = "My Harvest Inventory (${ownListings.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (ownListings.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Inventory2,
                        title = "No harvest batches listed yet",
                        subtitle = "Tap '${copy.publishHarvest}' above to list fresh crops with transparent farm-gate pricing and client-compressed photos."
                    )
                }
            } else {
                items(ownListings, key = { it.id }) { listing ->
                    FarmerInventoryCard(
                        listing = listing,
                        onEdit = { editingListing = listing },
                        onDelete = { onDeleteListing(listing.id) }
                    )
                }
            }

            // Verified Consumer Reviews Received
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Verified Buyer Reviews (${myReviews.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (myReviews.isEmpty()) {
                item {
                    Text(
                        text = "Reviews linked to delivered orders will appear here automatically and boost your ranking across nearby listings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(myReviews, key = { "rev_${it.id}" }) { review ->
                    VerifiedReviewCard(review = review)
                }
            }
        }
    }

    if (showPublishSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPublishSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PublishHarvestSheetContent(
                isBusy = isBusy,
                onSubmit = { name, cat, price, compare, qty, date, desc, photo ->
                    onPublishListing(name, cat, price, compare, qty, date, desc, photo) {
                        showPublishSheet = false
                    }
                }
            )
        }
    }

    val editTarget = editingListing
    if (editTarget != null) {
        EditListingDialog(
            listing = editTarget,
            onDismiss = { editingListing = null },
            onSave = { price, qty ->
                onUpdateListing(editTarget.id, price, qty)
                editingListing = null
            }
        )
    }
}

@Composable
private fun FarmerKpiCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (icon == Icons.Filled.Star) StarAmber else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FarmerInventoryCard(
    listing: ProduceListing,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("farmer_inventory_card_${listing.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CropImageDisplay(
                photoBase64 = listing.photoBase64,
                category = listing.categoryEnum(),
                contentDescription = listing.cropName,
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.cropName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatInr(listing.pricePerKg)}/kg · ${formatKg(listing.availableQtyKg)} left",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Status: ${listing.status} · Harvested ${listing.harvestDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_listing_${listing.id}")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit price or stock")
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_listing_${listing.id}")
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete listing",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublishHarvestSheetContent(
    isBusy: Boolean,
    onSubmit: (
        cropName: String,
        category: ProduceCategory,
        pricePerKg: Double,
        comparePricePerKg: Double,
        availableQtyKg: Double,
        harvestDate: String,
        description: String,
        photoBase64: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val todayFormatted = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    var cropName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProduceCategory.VEGETABLES) }
    var priceText by remember { mutableStateOf("40") }
    var comparePriceText by remember { mutableStateOf("65") }
    var qtyText by remember { mutableStateOf("35") }
    var harvestDate by remember { mutableStateOf(todayFormatted) }
    var description by remember { mutableStateOf("Freshly harvested this morning, chemical-free farm-gate batch.") }
    var compressedImage by remember { mutableStateOf<CompressedImageResult?>(null) }
    var isCompressing by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker (Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isCompressing = true
            scope.launch {
                compressedImage = compressCropPhotoFromUri(context, uri)
                isCompressing = false
            }
        }
    }

    val priceVal = priceText.toDoubleOrNull() ?: 0.0
    val compareVal = comparePriceText.toDoubleOrNull() ?: priceVal
    val qtyVal = qtyText.toDoubleOrNull() ?: 0.0
    val canSubmit = !isBusy && !isCompressing && cropName.trim().length >= 2 && priceVal > 0.0 && qtyVal > 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Publish New Harvest Batch", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = cropName,
            onValueChange = { if (it.length <= 60) cropName = it },
            label = { Text("Crop Name") },
            placeholder = { Text("e.g., Organic Heirloom Tomatoes") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("publish_crop_name_input")
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProduceCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat.label) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Farm Price (₹/kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("publish_price_input")
            )
            OutlinedTextField(
                value = comparePriceText,
                onValueChange = { comparePriceText = it },
                label = { Text("Retail Price (₹/kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                label = { Text("Available Qty (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("publish_qty_input")
            )
            OutlinedTextField(
                value = harvestDate,
                onValueChange = { harvestDate = it },
                label = { Text("Harvest Date") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 400) description = it },
            label = { Text("Batch Notes") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // Client-side compressed crop photo upload
        OutlinedButton(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pick_crop_photo_button")
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    isCompressing -> "Compressing crop photo for low-data upload…"
                    compressedImage != null -> "Change Crop Photo (${compressedImage!!.compressedKb} KB compressed)"
                    else -> "Attach Crop Photo (Auto-Compressed)"
                }
            )
        }

        compressedImage?.let { img ->
            val previewBmp = remember(img.base64) { decodeBase64Bitmap(img.base64) }
            if (previewBmp != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = previewBmp.asImageBitmap(),
                        contentDescription = "Compressed crop photo preview",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "Optimized to ${img.compressedKb} KB (${img.width}×${img.height}) for fast rural mobile uploads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VerifiedEmerald
                    )
                }
            }
        }

        Button(
            onClick = {
                onSubmit(
                    cropName,
                    category,
                    priceVal,
                    compareVal,
                    qtyVal,
                    harvestDate,
                    description,
                    compressedImage?.base64
                )
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_publish_harvest_button"),
            shape = MaterialTheme.shapes.large
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Publish Live to Marketplace", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun EditListingDialog(
    listing: ProduceListing,
    onDismiss: () -> Unit,
    onSave: (pricePerKg: Double, availableQtyKg: Double) -> Unit
) {
    var priceText by remember(listing.id) { mutableStateOf(listing.pricePerKg.toString()) }
    var qtyText by remember(listing.id) { mutableStateOf(listing.availableQtyKg.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ${listing.cropName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Farm-Gate Price (₹/kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Available Stock (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: listing.pricePerKg
                    val q = qtyText.toDoubleOrNull() ?: listing.availableQtyKg
                    onSave(p, q)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

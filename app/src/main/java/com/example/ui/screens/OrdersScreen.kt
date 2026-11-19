package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryStatus
import com.example.data.model.MarketplaceOrder
import com.example.data.model.UserRole
import com.example.data.model.formatInr
import com.example.data.model.formatKg
import com.example.ui.components.DeliveryProgressStepper
import com.example.ui.components.DeliveryRouteMapPreview
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InteractiveStarRatingBar
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.UiState

@Composable
fun OrdersScreen(
    copy: LocalizedCopy,
    activeRole: UserRole,
    currentUserId: String,
    consumerOrdersState: UiState<List<MarketplaceOrder>>,
    farmerIncomingOrders: List<MarketplaceOrder>,
    isBusy: Boolean,
    onSubmitReview: (order: MarketplaceOrder, rating: Int, comment: String, onDone: () -> Unit) -> Unit,
    onBrowseMarket: () -> Unit
) {
    var showFarmerSales by remember(activeRole) { mutableStateOf(activeRole == UserRole.FARMER) }
    var orderToReview by remember { mutableStateOf<MarketplaceOrder?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("orders_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (showFarmerSales) "Incoming Farm Orders" else "My Direct Farm Orders",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showFarmerSales,
                        onClick = { showFarmerSales = false },
                        label = { Text("My Purchases") },
                        modifier = Modifier.testTag("orders_tab_purchases")
                    )
                    FilterChip(
                        selected = showFarmerSales,
                        onClick = { showFarmerSales = true },
                        label = { Text("Farm Sales (${farmerIncomingOrders.size})") },
                        modifier = Modifier.testTag("orders_tab_sales")
                    )
                }
            }
        }

        if (showFarmerSales) {
            if (farmerIncomingOrders.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.ReceiptLong,
                        title = "No incoming farm orders yet",
                        subtitle = "When nearby consumers order from your published harvests, direct 96% farm-gate settlements appear here in real time."
                    )
                }
            } else {
                items(farmerIncomingOrders, key = { "sale_${it.id}" }) { order ->
                    OrderDetailCard(
                        copy = copy,
                        order = order,
                        isBuyerView = false,
                        currentUserId = currentUserId,
                        onOpenReview = {}
                    )
                }
            }
        } else {
            when (consumerOrdersState) {
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
                            icon = Icons.Filled.ReceiptLong,
                            title = "Could not load orders",
                            subtitle = consumerOrdersState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (consumerOrdersState.data.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.ReceiptLong,
                                title = "No orders placed yet",
                                subtitle = "Browse fresh harvests from nearby farmers, pay transparent farm-gate prices, and leave verified reviews after delivery.",
                                action = {
                                    Button(onClick = onBrowseMarket) {
                                        Text("Browse Fresh Market")
                                    }
                                }
                            )
                        }
                    } else {
                        items(consumerOrdersState.data, key = { it.id }) { order ->
                            OrderDetailCard(
                                copy = copy,
                                order = order,
                                isBuyerView = true,
                                currentUserId = currentUserId,
                                onOpenReview = { orderToReview = order }
                            )
                        }
                    }
                }
            }
        }
    }

    val reviewTarget = orderToReview
    if (reviewTarget != null) {
        VerifiedFarmerReviewDialog(
            order = reviewTarget,
            isBusy = isBusy,
            onDismiss = { orderToReview = null },
            onSubmit = { stars, comment ->
                onSubmitReview(reviewTarget, stars, comment) {
                    orderToReview = null
                }
            }
        )
    }
}

@Composable
private fun OrderDetailCard(
    copy: LocalizedCopy,
    order: MarketplaceOrder,
    isBuyerView: Boolean,
    currentUserId: String,
    onOpenReview: () -> Unit
) {
    val status = order.deliveryStatusEnum()
    val items = order.parsedItems()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBuyerView) order.farmName else "Buyer: ${order.consumerName}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isBuyerView) {
                            "Farmer: ${order.farmerName} · ${order.distanceKm} km"
                        } else {
                            "Drop-off: ${order.deliveryAddress}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isBuyerView) formatInr(order.totalAmount)
                        else "Payout ${formatInr(order.farmerPayoutAmount)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Itemized crop lines
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.cropName} × ${formatKg(item.quantityKg)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatInr(item.lineTotal),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Split Settlement Summary Strip
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Farmer Split: ${formatInr(order.farmerPayoutAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = VerifiedEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Driver Fee: ${formatInr(order.driverPayoutAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Live Delivery Tracking Stepper
            DeliveryProgressStepper(currentStatus = status)

            if (order.driverName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Assigned Driver: ${order.driverName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = "Awaiting nearby delivery driver in Dispatch queue…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DeliveryRouteMapPreview(
                pickupLat = order.pickupLat,
                pickupLng = order.pickupLng,
                pickupLabel = order.farmName,
                dropLat = order.deliveryLat,
                dropLng = order.deliveryLng,
                dropLabel = order.deliveryAddress,
                distanceKm = order.distanceKm
            )

            // Verified Purchase Rating Action (ONLY enabled once DELIVERED)
            if (isBuyerView && order.consumerId == currentUserId && status == DeliveryStatus.DELIVERED) {
                if (!order.reviewed) {
                    Button(
                        onClick = onOpenReview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rate_farmer_button_${order.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${copy.leaveReview} (${copy.verifiedPurchase})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = VerifiedEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Verified purchase review submitted for ${order.farmerName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerifiedFarmerReviewDialog(
    order: MarketplaceOrder,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Rate ${order.farmName}", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Verified Purchase · ${order.cropSummary()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VerifiedEmerald
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How was the freshness, quality, and farm-gate value?",
                    style = MaterialTheme.typography.bodyMedium
                )
                InteractiveStarRatingBar(
                    rating = selectedStars,
                    onRatingSelected = { selectedStars = it }
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 500) comment = it },
                    label = { Text("Share your review (3–500 chars)") },
                    placeholder = { Text("Crisp morning harvest, sweet produce, fair pricing…") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedStars, comment) },
                enabled = !isBusy && comment.trim().length >= 3,
                modifier = Modifier.testTag("submit_review_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Publish Verified Review")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
